# Data Model: IBFT

**Feature**: `006-ibft` | **Date**: 2026-06-19

---

## 0. Service Entry Point Map

| Service | Role | Entry protocol | Notes |
|---------|------|----------------|-------|
| `app-orchestration` | Receive `createIbft`, freeze wallet, create PENDING journal, return 200, write outbox | **HTTP** `POST /transfers` (S1 inbound, JWT auth) | Sync accept before 200; outbox dispatches payout |
| `app-wallet` | Execute IBFT_FREEZE / IBFT_SETTLE / IBFT_RELEASE | **HTTP** `wallet-internal.yaml` | Called sync by orchestration (accept) and payout worker (settle/release) |
| `app-accounting` | Execute createJournal (PENDING) / confirmIbft (POSTED) / voidIbft (FAILED) | **HTTP** `accounting-internal.yaml` | Called sync by orchestration (accept) and payout worker (settle/release) |
| `app-payout-worker` | Consume `IBFT_PAYOUT` → call Napas/bank API → settle or release | **RabbitMQ** `core.commands.ibft-payout` | No sync return; polls Napas until terminal |

> **Rule (ADR-007, ADR-010):** `app-orchestration` calls `app-wallet` and `app-accounting` via **sync HTTP** for the accept phase (before 200). The payout and its outcome are async via RabbitMQ worker. Same pattern as withdraw.

> **`confirmIbft` and `voidIbft` are not separate HTTP endpoints in `accounting-internal.yaml`** beyond the existing `postJournal` / `reverseJournal` surfaces. They are **in-process methods** on `JournalService` (`confirmIbft(coaTransId, principal, platformFee, napasCost)`, `voidIbft(coaTransId)`) called directly by the payout worker's in-process accounting adapter.

---

## 1. Entities

### 1.1 `coa_trans` — Journal Header (PostgreSQL, `accounting` schema)

| Column | Type | Notes |
|--------|------|-------|
| `id` | BIGINT PK | Journal identifier |
| `reference_id` | VARCHAR(64) | `businessRef` — idempotency key |
| `use_case` | VARCHAR(32) | `IBFT` |
| `status` | VARCHAR(16) | `PENDING` → `POSTED` (settle) or `FAILED` (release) |
| `created_at` | TIMESTAMPTZ | Immutable |
| `updated_at` | TIMESTAMPTZ | Updated on settle/release |

**Unique constraint**: `(reference_id, use_case)` — duplicate `createJournal` → return existing.

### 1.2 TigerBeetle Accounts (TB cluster, mapped from COA)

| TB Account ID | Mapping | COA code | Notes |
|--------------|---------|----------|-------|
| `hash("2110")` | USER wallet liability aggregate | 2110 | Same as withdraw/deposit |
| `hash("3400")` | IBFT transit | 3400 | New; must net to 0 per txn |
| `hash("1112")` | Napas Clearing account | 1112 | ADR-025: not 1111 |
| `hash("4130")` | Transfer fee revenue | 4130 | Platform income |
| `hash("5100")` | Napas cost expense | 5100 | Accumulates DR; not transit |

### 1.3 TigerBeetle Transfers

**Phase A — PENDING (on accept, sync):**

| Field | Value |
|-------|-------|
| `id` | `hash(businessRef + ":ibftA")` |
| `debit_account_id` | 2110 |
| `credit_account_id` | 3400 |
| `amount` | `gross × 10⁴` (u128) |
| `flags.pending` | `true` |
| `user_data_128` | `coa_trans.id` BIGINT |

**Phase B — POST + closing legs (on settle, async):**

| # | Transfer ID | Debit | Credit | Amount | Notes |
|---|-------------|-------|--------|--------|-------|
| 1 | `hash(businessRef + ":ibftA")` | — | — | — | `post_pending_transfer` closes phase A |
| 2 | `hash(businessRef + ":4130")` | 3400 | 4130 | `platformFee × 10⁴` | Fee revenue |
| 3 | `hash(businessRef + ":1112")` | 3400 | 1112 | `principal × 10⁴` | Napas clearing |
| 4 | `hash(businessRef + ":5100")` | 5100 | 1112 | `napasCost × 10⁴` | Platform expense leg |

After transfers 1–4: `account[3400].balance = 0` ✓ (ADR-010). Transfer 4 does not touch 3400 — it is a separate platform expense leg.

**Release — void (on bank fail, async):**

| Operation | Transfer ID |
|-----------|-------------|
| `void_pending_transfer` | `hash(businessRef + ":ibftA")` |

After void: `account[3400].balance = 0` ✓ (2110/3400 pending never posted)

### 1.4 `wallet_tx` — Wallet Transactions (PostgreSQL, `wallet` schema)

Three transaction types for the IBFT lifecycle:

| `tx_type` | Direction | When | `wallet_balance` effect |
|-----------|-----------|------|------------------------|
| `IBFT_FREEZE` | FREEZE | Accept (sync) | `available -= gross`, `frozen += gross` |
| `IBFT_SETTLE` | DEBIT | Napas success (async) | `frozen -= gross` |
| `IBFT_RELEASE` | UNFREEZE | Napas fail (async) | `frozen -= gross`, `available += gross` |

Common columns: `wallet_id`, `business_ref`, `tx_type`, `direction`, `amount`, `available_after`, `frozen_after`.

**Unique constraint**: `(wallet_id, business_ref, tx_type)` — idempotent replay for each leg.

> No `IBFT_DEBIT` type — wallet never debits `available` twice. Freeze is the debit commitment; settle closes it from `frozen`. (ADR-007)

> `gross = principal + platformFee`. napasCost is NOT included in gross — the platform absorbs it separately.

### 1.5 `wallet_balance` — Spendable Snapshot (PostgreSQL, `wallet` schema)

| Event | `available` | `frozen` |
|-------|-------------|---------|
| Accept (FREEZE) | − gross | + gross |
| Settle (DEBIT) | no change | − gross |
| Release (UNFREEZE) | + gross | − gross |

### 1.6 Outbox — `IBFT_PAYOUT` command (PostgreSQL, `accounting` schema)

| Column | Value |
|--------|-------|
| `command_type` | `IBFT_PAYOUT` |
| `business_ref` | `businessRef` |
| `payload` | Full command envelope (see §3) |
| `status` | `PENDING` → `PUBLISHED` → `FAILED` |

Written in the same transaction as the 200 response (ADR-013 outbox guarantee).

---

## 2. State Transitions

```
[Member submits POST /transfers (external bank destination)]
        │
        ▼
  orchestration (sync)
        │  Validate (balance, currency, idempotency)
        │
        ├─ available < gross → 422 WALLET_INSUFFICIENT_BALANCE
        │
        └─ OK
              │
              │  1. app-wallet: IBFT_FREEZE
              │     available -= gross, frozen += gross
              │
              │  2. app-accounting: createJournal(IBFT, PENDING)
              │     coa_trans status=PENDING
              │     TB: pending Transfer (2110←3400, gross)
              │
              │  3. INSERT outbox(IBFT_PAYOUT)   ─┐ same tx
              │  4. Return 200                    ─┘
              │
              │  outbox relay → RabbitMQ: IBFT_PAYOUT
              ▼
  app-payout-worker (async)
        │  Call Napas/bank API
        │
        ├─ Napas SUCCESS
        │       │
        │       ├─ app-accounting: confirmIbft(coaTransId, principal, platformFee, napasCost)
        │       │    TB: post_pending(ibftA)
        │       │    TB: Transfer 3400→4130 (platformFee)
        │       │    TB: Transfer 3400→1112 (principal)
        │       │    TB: Transfer 5100→1112 (napasCost)   ← separate expense leg
        │       │    assert account[3400].balance = 0
        │       │    coa_trans.status = POSTED
        │       │
        │       └─ app-wallet: IBFT_SETTLE
        │            frozen -= gross
        │
        └─ Napas FAIL (terminal)
                │
                ├─ app-accounting: voidIbft(coaTransId)
                │    TB: void_pending_transfer(ibftA)
                │    coa_trans.status = FAILED
                │    (5100/1112 NOT posted — no Napas call completed)
                │
                └─ app-wallet: IBFT_RELEASE
                     frozen -= gross, available += gross

  Napas UNKNOWN / TIMEOUT → POLL (ADR-033) — never auto-release
```

---

## 3. Inbound & Outbound Entities

### app-orchestration — inbound & outbound

#### Inbound — `IbftRequest` from member via HTTP (JWT auth, ADR-011)

```
POST /transfers   (spec/contracts/http/gtelpay-public.yaml)

IbftRequest {
  destinationBankAccountNumber : string                // beneficiary bank account
  destinationBankCode          : string                // bank BIC/code
  principalAmount              : string (decimal, s4)  // e.g. "100000.0000"
  businessRef                  : string                // = X-Idempotency-Key
  currency                     : string                // "VND"
}
```

`platformFee` and `napasCost` are computed by orchestration from feeSchedule before calling wallet/accounting. `gross = principal + platformFee` (napasCost not in gross — platform bears it separately).

---

#### Outbound — `IbftAck` (HTTP 200) to member

```
HTTP 200 OK

IbftAck {
  businessRef : string
  status      : "ACCEPTED"
  gross       : string (decimal, s4)   // principal + platformFee
  frozen      : string (decimal, s4)   // wallet_balance.frozen after freeze
}
```

Guarantee: returned only after IBFT_FREEZE and `coa_trans` PENDING both succeed.

---

#### Outbound — `IbftPayoutCommand` to payout worker via RabbitMQ (outbox relay)

```
Exchange: core.commands   Routing key: core.commands.ibft-payout

IbftPayoutCommand {
  commandType                  : "IBFT_PAYOUT"
  businessRef                  : string
  memberId                     : BIGINT
  walletId                     : BIGINT
  coaTransId                   : BIGINT               // correlation to coa_trans.id
  principalAmount              : string (decimal, s4)
  platformFee                  : string (decimal, s4)
  napasCost                    : string (decimal, s4)
  grossAmount                  : string (decimal, s4)
  destinationBankAccountNumber : string
  destinationBankCode          : string
  currency                     : string
}
```

Source: `spec/contracts/async-api/core-commands.yaml`
Published by: orchestration outbox relay after 200

---

### Internal worker — inbound & outbound (payout worker)

#### Inbound — `IbftPayoutCommand` (from orchestration via RabbitMQ)

See above.

#### Outbound — `IbftSettledEvent` to Kafka (optional)

```
IbftSettledEvent {
  eventType   : "IbftSettled"
  businessRef : string
  coaTransId  : BIGINT
  principal   : string (decimal, s4)
  platformFee : string (decimal, s4)
  napasCost   : string (decimal, s4)
  currency    : string
}
```

Topic: `core.accounting.ibft-settled`

#### Outbound — `IbftReleasedEvent` to Kafka (optional)

```
IbftReleasedEvent {
  eventType   : "IbftReleased"
  businessRef : string
  coaTransId  : BIGINT
  grossAmount : string (decimal, s4)
  reason      : string                  // "NAPAS_FAILED" | "NAPAS_REJECTED" | "OPS_VOID"
  currency    : string
}
```

Topic: `core.accounting.ibft-released`

#### Outbound — `CommandFailedEvent` (on unrecoverable error)

Reuses existing schema from `core-events.yaml` (`commandType = "IBFT_PAYOUT"`).

---

### Boundary summary

```
              ┌──────────────────────────────────────────────────────────┐
[member]      │ IbftRequest ──► app-orchestration                        │
              │                         │                                │
              │          ┌──────────────┤ sync (before 200)              │
              │          │              │                                │
              │          ▼              ▼                                │
              │    app-wallet     app-accounting                         │
              │    FREEZE         createJournal(PENDING)                 │
              │                                                          │
              │    app-orchestration ── 200 ──► member                  │
              │          │                                               │
              │          │ ── IbftPayoutCommand ──► RabbitMQ             │
              │          ▼                                               │
              │    app-payout-worker                                     │
              │          │── (Napas/bank call) ────────────►  Napas      │
              │          │                                               │
              │          ├── SUCCESS                                     │
              │          │    ├─ app-accounting: confirmIbft             │
              │          │    │    (post_pending + 3 TB transfers)       │
              │          │    └─ app-wallet: IBFT_SETTLE                 │
              │          │    └─ IbftSettledEvent ──► Kafka              │
              │          │                                               │
              │          └── FAIL                                        │
              │               ├─ app-accounting: voidIbft                │
              │               └─ app-wallet: IBFT_RELEASE                │
              │               └─ IbftReleasedEvent ──► Kafka             │
              └──────────────────────────────────────────────────────────┘
```
