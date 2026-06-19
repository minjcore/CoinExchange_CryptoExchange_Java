# Data Model: Withdraw

**Feature**: `003-withdraw` | **Date**: 2026-06-19

---

## 0. Service Entry Point Map

| Service | Role | Entry protocol | Notes |
|---------|------|----------------|-------|
| `app-orchestration` | Receive `createWithdrawal`, freeze wallet, create PENDING journal, return 200, write outbox | **HTTP** `POST /withdrawals` (S1 inbound) | Sync accept before 200; outbox dispatches payout |
| `app-wallet` | Execute WITHDRAW_FREEZE / WITHDRAW_SETTLE / WITHDRAW_RELEASE | **HTTP** `wallet-internal.yaml` | Called sync by orchestration (accept) and payout worker (settle/release) |
| `app-accounting` | Execute createJournal (PENDING) / confirmWithdraw (POSTED) / voidWithdraw (FAILED) | **HTTP** `accounting-internal.yaml` | Called sync by orchestration (accept) and payout worker (settle/release) |
| `app-payout-worker` | Consume `WITHDRAW_PAYOUT` → call bank API → settle or release | **RabbitMQ** `core.commands.withdraw-payout` | No sync return; polls bank until terminal |

> **Rule (ADR-007, ADR-010):** `app-orchestration` calls `app-wallet` and `app-accounting` via **sync HTTP** for the accept phase (before 200). The payout and its outcome are async via RabbitMQ worker. This is the inverse of deposit: here the member blocks until funds are reserved.

> **`confirmWithdraw` and `voidWithdraw` are not separate HTTP endpoints in `accounting-internal.yaml`** beyond the existing `postJournal` / `reverseJournal` surfaces. They are **in-process methods** on `JournalService` (`confirmWithdraw(coaTransId, principal, fee)`, `voidWithdraw(coaTransId)`) called directly by the payout worker's in-process accounting adapter.

---

## 1. Entities

### 1.1 `coa_trans` — Journal Header (PostgreSQL, `accounting` schema)

| Column | Type | Notes |
|--------|------|-------|
| `id` | BIGINT PK | Journal identifier |
| `reference_id` | VARCHAR(64) | `businessRef` — idempotency key |
| `use_case` | VARCHAR(32) | `WITHDRAW` |
| `status` | VARCHAR(16) | `PENDING` → `POSTED` (settle) or `FAILED` (release) |
| `created_at` | TIMESTAMPTZ | Immutable |
| `updated_at` | TIMESTAMPTZ | Updated on settle/release |

**Unique constraint**: `(reference_id, use_case)` — duplicate `createJournal` → return existing.

### 1.2 TigerBeetle Accounts (TB cluster, mapped from COA)

| TB Account ID | Mapping | COA code |
|--------------|---------|----------|
| `hash("2110")` | USER wallet liability aggregate | 2110 |
| `hash("3200")` | Withdraw transit | 3200 |
| `hash("1111")` | Vietinbank Nostro | 1111 |
| `hash("4120")` | Withdraw fee revenue | 4120 |

### 1.3 TigerBeetle Transfers

**Phase A — PENDING (on accept, sync):**

| Field | Value |
|-------|-------|
| `id` | `hash(businessRef + ":withdrawA")` |
| `debit_account_id` | 2110 |
| `credit_account_id` | 3200 |
| `amount` | `gross × 10⁴` (u128) |
| `flags.pending` | `true` |
| `user_data_128` | `coa_trans.id` BIGINT |

**Phase B — POST + closing legs (on settle, async):**

| # | Transfer ID | Debit | Credit | Amount | Notes |
|---|-------------|-------|--------|--------|-------|
| 1 | `hash(businessRef + ":withdrawA")` | — | — | — | `post_pending_transfer` closes phase A |
| 2 | `hash(businessRef + ":1111")` | 3200 | 1111 | `principal × 10⁴` | Bank nostro |
| 3 | `hash(businessRef + ":4120")` | 3200 | 4120 | `fee × 10⁴` | Fee revenue |

After transfers 1–3: `account[3200].balance = 0` ✓ (ADR-010)

**Release — void (on bank fail, async):**

| Operation | Transfer ID |
|-----------|-------------|
| `void_pending_transfer` | `hash(businessRef + ":withdrawA")` |

After void: `account[3200].balance = 0` ✓ (2110/3200 pending never posted)

### 1.4 `wallet_tx` — Wallet Transactions (PostgreSQL, `wallet` schema)

Three transaction types for the withdraw lifecycle:

| `tx_type` | Direction | When | `wallet_balance` effect |
|-----------|-----------|------|------------------------|
| `WITHDRAW_FREEZE` | FREEZE | Accept (sync) | `available -= gross`, `frozen += gross` |
| `WITHDRAW_SETTLE` | DEBIT | Bank success (async) | `frozen -= gross` |
| `WITHDRAW_RELEASE` | UNFREEZE | Bank fail (async) | `frozen -= gross`, `available += gross` |

Common columns: `wallet_id`, `business_ref`, `tx_type`, `direction`, `amount`, `available_after`, `frozen_after`.

**Unique constraint**: `(wallet_id, business_ref, tx_type)` — idempotent replay for each leg.

> No `WITHDRAW_DEBIT` type — wallet never debits `available` twice. Freeze is the debit commitment; settle closes it from `frozen`. (ADR-007)

### 1.5 `wallet_balance` — Spendable Snapshot (PostgreSQL, `wallet` schema)

| Event | `available` | `frozen` |
|-------|-------------|---------|
| Accept (FREEZE) | − gross | + gross |
| Settle (DEBIT) | no change | − gross |
| Release (UNFREEZE) | + gross | − gross |

### 1.6 Outbox — `WITHDRAW_PAYOUT` command (PostgreSQL, `accounting` schema)

Same outbox table as deposit (`command_type = 'WITHDRAW_PAYOUT'`).

| Column | Value |
|--------|-------|
| `command_type` | `WITHDRAW_PAYOUT` |
| `business_ref` | `businessRef` |
| `payload` | Full command envelope (see §4) |
| `status` | `PENDING` → `PUBLISHED` → `FAILED` |

Written in the same transaction as the 200 response (ADR-013 outbox guarantee).

---

## 2. State Transitions

```
[Member submits createWithdrawal]
        │
        ▼
  orchestration (sync)
        │  Validate (balance, currency, idempotency)
        │
        ├─ available < gross → 422 WALLET_INSUFFICIENT_BALANCE
        │
        └─ OK
              │
              │  1. app-wallet: WITHDRAW_FREEZE
              │     available -= gross, frozen += gross
              │
              │  2. app-accounting: createJournal(WITHDRAW, PENDING)
              │     coa_trans status=PENDING
              │     TB: pending Transfer (2110←3200, gross)
              │
              │  3. INSERT outbox(WITHDRAW_PAYOUT)   ─┐ same tx
              │  4. Return 200                        ─┘
              │
              │  outbox relay → RabbitMQ: WITHDRAW_PAYOUT
              ▼
  app-payout-worker (async)
        │  Call bank API (NAPAS / bank transfer)
        │
        ├─ Bank SUCCESS
        │       │
        │       ├─ app-accounting: confirmWithdraw(coaTransId, principal, fee)
        │       │    TB: post_pending(withdrawA)
        │       │    TB: Transfer 3200→1111 (principal)
        │       │    TB: Transfer 3200→4120 (fee)
        │       │    assert account[3200].balance = 0
        │       │    coa_trans.status = POSTED
        │       │
        │       └─ app-wallet: WITHDRAW_SETTLE
        │            frozen -= gross
        │
        └─ Bank FAIL (terminal)
                │
                ├─ app-accounting: voidWithdraw(coaTransId)
                │    TB: void_pending_transfer(withdrawA)
                │    coa_trans.status = FAILED
                │
                └─ app-wallet: WITHDRAW_RELEASE
                     frozen -= gross, available += gross

  Bank UNKNOWN / TIMEOUT → POLL (ADR-033) — never auto-release
```

---

## 3. Inbound & Outbound Entities

### app-orchestration — inbound & outbound

#### Inbound — `WithdrawRequest` from member via HTTP (JWT auth, ADR-011)

```
POST /withdrawals   (spec/contracts/http/gtelpay-public.yaml)

WithdrawRequest {
  bankAccountNumber  : string                // beneficiary bank account
  bankCode           : string                // bank BIC/code
  principalAmount    : string (decimal, s4)  // e.g. "100000.0000"
  businessRef        : string                // = X-Idempotency-Key
  currency           : string                // "VND"
}
```

Fee is computed by orchestration from feeSchedule before calling wallet/accounting. `gross = principal + fee`.

---

#### Outbound — `WithdrawAck` (HTTP 200) to member

```
HTTP 200 OK

WithdrawAck {
  businessRef : string
  status      : "ACCEPTED"
  gross       : string (decimal, s4)   // principal + fee
  frozen      : string (decimal, s4)   // wallet_balance.frozen after freeze
}
```

Guarantee: returned only after WITHDRAW_FREEZE and `coa_trans` PENDING both succeed.

---

#### Outbound — `WithdrawPayoutCommand` to payout worker via RabbitMQ (outbox relay)

```
Exchange: core.commands   Routing key: core.commands.withdraw-payout

WithdrawPayoutCommand {
  commandType       : "WITHDRAW_PAYOUT"
  businessRef       : string
  memberId          : BIGINT
  walletId          : BIGINT
  coaTransId        : BIGINT               // correlation to coa_trans.id
  principalAmount   : string (decimal, s4)
  fee               : string (decimal, s4)
  grossAmount       : string (decimal, s4)
  bankAccountNumber : string
  bankCode          : string
  currency          : string
}
```

Source: `spec/contracts/async-api/core-commands.yaml`
Published by: orchestration outbox relay after 200

---

### Internal worker — inbound & outbound (payout worker)

#### Inbound — `WithdrawPayoutCommand` (from orchestration via RabbitMQ)

See above.

#### Outbound — `WithdrawSettledEvent` to Kafka (optional)

```
WithdrawSettledEvent {
  eventType   : "WithdrawSettled"
  businessRef : string
  coaTransId  : BIGINT
  principal   : string (decimal, s4)
  fee         : string (decimal, s4)
  currency    : string
}
```

Topic: `core.accounting.withdraw-settled`

#### Outbound — `WithdrawReleasedEvent` to Kafka (optional)

```
WithdrawReleasedEvent {
  eventType   : "WithdrawReleased"
  businessRef : string
  coaTransId  : BIGINT
  grossAmount : string (decimal, s4)
  reason      : string                  // "BANK_FAILED" | "BANK_REJECTED" | "OPS_VOID"
  currency    : string
}
```

Topic: `core.accounting.withdraw-released`

#### Outbound — `CommandFailedEvent` (on unrecoverable error)

Reuses existing schema from `core-events.yaml` (`commandType = "WITHDRAW_PAYOUT"`).

---

### Boundary summary

```
              ┌──────────────────────────────────────────────────────────┐
[member]      │ WithdrawRequest ──► app-orchestration                    │
              │                         │                                │
              │          ┌──────────────┤ sync (before 200)              │
              │          │              │                                │
              │          ▼              ▼                                │
              │    app-wallet     app-accounting                         │
              │    FREEZE         createJournal(PENDING)                 │
              │                                                          │
              │    app-orchestration ── 200 ──► member                  │
              │          │                                               │
              │          │ ── WithdrawPayoutCommand ──► RabbitMQ         │
              │          ▼                                               │
              │    app-payout-worker                                     │
              │          │── (bank call) ──────────────────►  bank       │
              │          │                                               │
              │          ├── SUCCESS                                     │
              │          │    ├─ app-accounting: confirmWithdraw         │
              │          │    └─ app-wallet: SETTLE                      │
              │          │    └─ WithdrawSettledEvent ──► Kafka          │
              │          │                                               │
              │          └── FAIL                                        │
              │               ├─ app-accounting: voidWithdraw            │
              │               └─ app-wallet: RELEASE                     │
              │               └─ WithdrawReleasedEvent ──► Kafka         │
              └──────────────────────────────────────────────────────────┘
```
