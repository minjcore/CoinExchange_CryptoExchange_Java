# Data Model: Async Deposit

**Feature**: `002-async-deposit` | **Date**: 2026-06-18

---

## 1. Entities

### 1.1 `coa_trans` — Journal Header (PostgreSQL, `accounting` schema)

Unchanged schema — new rows for deposit flow.

| Column | Type | Notes |
|--------|------|-------|
| `id` | UUID PK | Journal identifier |
| `reference_id` | VARCHAR(64) | = `businessRef` — idempotency key |
| `use_case` | VARCHAR(32) | `DEPOSIT` for this flow |
| `status` | VARCHAR(16) | `PENDING` → `POSTED` (no edit after POSTED) |
| `created_at` | TIMESTAMPTZ | Immutable |
| `updated_at` | TIMESTAMPTZ | Updated on phase B |

**Unique constraint**: `(reference_id, use_case)` — duplicate `createJournal` → return existing (idempotent).

> **Note**: `coa_trans_data` (line-by-line DR/CR rows) is the PostgreSQL fallback schema. With TigerBeetle as backing store, the actual posting lines live in TB transfers — `coa_trans_data` is replaced by the TB read-model projector.

### 1.2 TigerBeetle Accounts (TB cluster, mapped from COA)

| TB Account ID | Mapping | COA code |
|--------------|---------|----------|
| `hash("1111")` | Receivable / nostro account | 1111 |
| `hash("3100")` | Deposit transit | 3100 |
| `hash("2110")` | USER wallet liability aggregate | 2110 |
| `hash("4110")` | Deposit fee revenue | 4110 |

TB accounts are seeded once at startup from `coa_account`. Account ID = deterministic u128 from COA code string.

### 1.3 TigerBeetle Transfers (hot postings, source of truth)

**Phase A — PENDING**:

| Field | Value |
|-------|-------|
| `id` | `hash(businessRef + ":phaseA")` |
| `debit_account_id` | 1111 |
| `credit_account_id` | 3100 |
| `amount` | `gross × 10⁴` (u128) |
| `flags.pending` | `true` |
| `user_data_128` | `coa_trans.id` (correlation) |

**Phase B — POST + liability transfers**:

| # | Transfer ID | Debit | Credit | Amount | Notes |
|---|-------------|-------|--------|--------|-------|
| 1 | `hash(businessRef + ":phaseA")` | — | — | — | `post_pending_transfer` closes phase A |
| 2 | `hash(businessRef + ":2110")` | 3100 | 2110 | `net × 10⁴` | Member liability |
| 3 | `hash(businessRef + ":4110")` | 3100 | 4110 | `fee × 10⁴` | Fee revenue (0 if fee=0) |

After transfers 1–3: `account[3100].balance = 0` ✓ (transit net-zero per ADR-010).

### 1.4 `wallet_tx` — Wallet Credit (PostgreSQL, `wallet` schema)

One row inserted after POSTED, never updated.

| Column | Type | Value |
|--------|------|-------|
| `wallet_id` | UUID | Member's wallet (pocket resolved by orchestration) |
| `business_ref` | VARCHAR(64) | = `businessRef` |
| `tx_type` | VARCHAR(32) | `DEPOSIT_CREDIT` |
| `direction` | VARCHAR(8) | `CREDIT` |
| `amount` | NUMERIC(18,4) | Net amount (gross − fee) |
| `available_after` | NUMERIC(18,4) | Snapshot of `wallet_balance.available` after mutation |
| `frozen_after` | NUMERIC(18,4) | Snapshot of `wallet_balance.frozen` after mutation |

**Unique constraint**: `(wallet_id, business_ref, tx_type)` — idempotent replay returns existing row.

### 1.5 `wallet_balance` — Spendable Snapshot (PostgreSQL, `wallet` schema)

Updated in the same transaction as `wallet_tx` insert.

| Column | Changed? | Notes |
|--------|---------|-------|
| `available` | +net amount | Increases after DEPOSIT_CREDIT |
| `frozen` | no change | Deposit does not freeze |
| `version` | +1 | Optimistic lock |

---

## 2. State Transitions

```
[Deposit notification arrives]
        │
        ▼
  orchestration → 202 (async ack)
        │
        │ publish BANK_DEPOSIT (outbox → RabbitMQ)
        ▼
  accounting worker
        │
        ├─ VA lookup → unknown VA → hold PENDING for ops (no journal)
        │
        └─ VA resolved → memberId
              │
              ▼
        Phase A: createJournal(DEPOSIT, PENDING)
              │  TB: pending Transfer (1111←3100)
              │  Postgres: coa_trans status=PENDING
              ▼
        Phase B: confirmDeposit(coaTransId, fee)
              │  TB: post_pending + transfers (3100←2110, 3100←4110)
              │  Postgres: coa_trans status=POSTED
              │  Validate: TB account[3100].balance = 0
              ▼
        wallet credit path (Path B: publish WALLET_CREDIT command)
              │
              ▼
        wallet worker
              │  gate: coa_trans.status = POSTED
              │  INSERT wallet_tx (DEPOSIT_CREDIT, net amount)
              │  UPDATE wallet_balance (+net available)
              ▼
        DONE — member sees updated balance
```

---

## 3. Blnk PoC Interfaces (core.wallet)

These interfaces are spec-level only — not shipped in v1 production path, explored as PoC alongside the deposit feature.

### 3.1 `WalletBalanceMonitor` (balance monitor pattern from Blnk)

```
Interface: WalletBalanceMonitor
  - register(walletId, threshold, direction: ABOVE | BELOW)
  - fire: WalletBalanceEvent(walletId, currentAvailable, threshold, direction)
```

Trigger: fired inside `WalletCommandService` after each successful mutation.
Inspired by Blnk `balance.monitor` webhook — implemented as Spring `ApplicationEvent`, no Blnk binary dependency.

### 3.2 `getBalanceAt(walletId, asOf)` (historical balance from Blnk)

```
WalletQueryService.getBalanceAt(walletId: UUID, asOf: Instant): WalletBalance
```

Implementation: query `wallet_tx WHERE wallet_id = ? AND created_at <= ? ORDER BY created_at DESC LIMIT 1` → return `available_after` + `frozen_after` snapshots.
Blnk does this with a snapshot job; GtelPay `wallet_tx` already stores after-balance snapshots — same result, no extra tables.

---

## 4. Inbound & Outbound Entities (internal boundaries only)

These are the entities that cross service boundaries **within** the GtelPay system.
External inbound schemas (from outside the system) are out of scope here.

### Inbound entities

Entities that enter a service from another internal service.

#### `BankDepositCommand` — enters `accounting worker` from `app-orchestration` via RabbitMQ

```
BankDepositCommand {
  commandType : "BANK_DEPOSIT"          // discriminator
  businessRef : string                  // idempotency key, end-to-end
  memberId    : string                  // resolved by orchestration
  walletId    : string                  // resolved pocket wallet_id
  grossAmount : string (decimal, s4)    // e.g. "100000.0000"
  fee         : string (decimal, s4)    // computed by orchestration; "0.0000" if none
  currency    : string                  // "VND"
}
```

Source: `spec/contracts/async-api/core-commands.yaml`
Published by: `app-orchestration` (via outbox → RabbitMQ exchange `core.commands`)
Consumed by: `accounting worker`

---

#### `WalletCreditCommand` — enters `wallet worker` from `accounting worker` via RabbitMQ

```
WalletCreditCommand {
  commandType : "WALLET_CREDIT"
  businessRef : string                  // same businessRef as BankDepositCommand
  walletId    : string
  netAmount   : string (decimal, s4)    // grossAmount − fee
  coaTransId  : string (UUID, optional) // correlation to coa_trans
  currency    : string                  // "VND"
}
```

Source: `spec/contracts/async-api/core-commands.yaml`
Published by: `accounting worker` after confirmDeposit → POSTED
Consumed by: `wallet worker`

---

### Outbound entities

Entities that leave a service toward downstream consumers.

#### `JournalPostedEvent` — emitted by `accounting worker` to Kafka (optional)

```
JournalPostedEvent {
  eventType   : "JournalPosted"
  useCase     : "DEPOSIT"
  businessRef : string
  coaTransId  : string (UUID)
  status      : "POSTED"
  netAmount   : string (decimal, s4)
  currency    : string
}
```

Source: `spec/contracts/async-api/core-events.yaml`
Topic: `core.accounting.journal-posted`
Consumed by: reporting, audit, reconciliation (async, latency-tolerant)

---

#### `WalletCreditedEvent` — emitted by `wallet worker` to Kafka (optional)

```
WalletCreditedEvent {
  eventType        : "WalletCredited"
  businessRef      : string
  walletId         : string
  netAmount        : string (decimal, s4)
  availableAfter   : string (decimal, s4)  // wallet_balance.available after credit
  currency         : string
}
```

Source: `spec/contracts/async-api/core-events.yaml`
Topic: `core.wallet.wallet-credited`
Consumed by: notification service, reporting (async)

---

#### `CommandFailedEvent` — emitted by any worker on unrecoverable failure

```
CommandFailedEvent {
  eventType   : "core.operations.command-failed"
  businessRef : string
  commandType : string                   // "BANK_DEPOSIT" | "WALLET_CREDIT"
  reason      : string
  failedAt    : string (ISO-8601)
}
```

Source: `spec/contracts/async-api/core-events.yaml`
Topic: `core.operations.command-failed`
Consumed by: ops alerting, saga manager

---

### Boundary summary

```
                  ┌─────────────────────────────────────────────────────┐
  [internal]      │                                                     │
                  │   app-orchestration                                 │
                  │        │                                            │
                  │        │ ── BankDepositCommand ──►                  │
                  │        ▼                                            │
                  │   accounting worker                                 │
                  │        │                                            │
                  │        ├ ── JournalPostedEvent ──► Kafka (out)      │
                  │        │                                            │
                  │        │ ── WalletCreditCommand ──►                 │
                  │        ▼                                            │
                  │   wallet worker                                     │
                  │        │                                            │
                  │        └ ── WalletCreditedEvent ──► Kafka (out)     │
                  │                                                     │
                  │   any worker ── CommandFailedEvent ──► Kafka (out)  │
                  └─────────────────────────────────────────────────────┘
```

---

## 5. Outbox Schema (PostgreSQL, `accounting` schema)

| Column | Type | Notes |
|--------|------|-------|
| `id` | UUID PK | |
| `command_type` | VARCHAR(32) | `BANK_DEPOSIT`, `WALLET_CREDIT` |
| `business_ref` | VARCHAR(64) | Idempotency key |
| `payload` | JSONB | Full command envelope per `core-commands.yaml` |
| `status` | VARCHAR(16) | `PENDING` → `PUBLISHED` → `FAILED` |
| `created_at` | TIMESTAMPTZ | |
| `published_at` | TIMESTAMPTZ | Nullable |

---

## 5. Migration Notes

| Migration | Contents |
|-----------|---------|
| `V_n__outbox_table.sql` | Create outbox table in `accounting` schema |
| `V_n+1__coa_trans_deposit_index.sql` | Add index `(reference_id, use_case)` if not exists |
| TB account seeding | On `core.accounting` startup: `createAccounts` for all COA codes in `coa_account`; idempotent (account id collision = no-op in TB) |
| `coa_trans_data` | Retained as read-model projection target — TB projector writes to it for reporting; not the posting source of truth |
