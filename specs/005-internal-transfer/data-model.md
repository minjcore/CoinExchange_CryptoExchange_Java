# Data Model: Internal Transfer

**Feature**: `005-internal-transfer` | **Date**: 2026-06-19

---

## 0. Service Entry Point Map

| Service | Role | Entry protocol | Notes |
|---------|------|----------------|-------|
| `app-orchestration` | Receive `POST /transfers`, validate sender ≠ receiver, compute fee, sequence debit → journal → credit, return 200 | **HTTP** `POST /transfers` (S1 inbound, JWT auth) | Fully sync; no outbox; no async worker |
| `app-wallet` | Execute TRANSFER_DEBIT (sender) / TRANSFER_CREDIT (receiver) / ADJUSTMENT_CREDIT (compensation) | **HTTP** `wallet-internal.yaml` | Called sync by orchestration, twice on happy path |
| `app-accounting` | Execute createJournal(TRANSFER, POSTED) — 3 TB transfers, immediately POSTED | **HTTP** `accounting-internal.yaml` | Called sync by orchestration; no pending phase |

> **Rule (ADR-027, ADR-010):** `app-orchestration` calls `app-wallet` (TRANSFER_DEBIT), then `app-accounting` (createJournal POSTED), then `app-wallet` (TRANSFER_CREDIT) — all synchronous HTTP before returning 200. No RabbitMQ, no outbox, no async worker.

> **`createJournal(TRANSFER, POSTED)`** submits all 3 TigerBeetle non-pending transfers as a single batch. The journal is POSTED immediately on success — there is no PENDING intermediate state for this use case.

---

## 1. Entities

### 1.1 `coa_trans` — Journal Header (PostgreSQL, `accounting` schema)

| Column | Type | Notes |
|--------|------|-------|
| `id` | BIGINT PK | Journal identifier |
| `reference_id` | VARCHAR(64) | `businessRef` — idempotency key |
| `use_case` | VARCHAR(32) | `TRANSFER` |
| `status` | VARCHAR(16) | `POSTED` immediately on success (no PENDING state) |
| `created_at` | TIMESTAMPTZ | Immutable |
| `updated_at` | TIMESTAMPTZ | Set on creation (same as created_at for transfers) |

**Unique constraint**: `(reference_id, use_case)` — duplicate `createJournal` → return existing.

### 1.2 TigerBeetle Accounts (TB cluster, mapped from COA)

| TB Account ID | Mapping | COA code |
|--------------|---------|----------|
| `hash("2110")` | USER wallet liability aggregate | 2110 |
| `hash("3300")` | Internal Transfer transit | 3300 |
| `hash("4130")` | Transfer fee revenue | 4130 |

> Both sender and receiver map to COA 2110 in TigerBeetle (aggregate liability). Per-member balances are tracked in PostgreSQL `wallet_balance`, not in TB accounts.

### 1.3 TigerBeetle Transfers

**Batch (3 non-pending transfers, submitted together in `createJournal`):**

| # | Transfer ID | Debit | Credit | Amount | Leg |
|---|-------------|-------|--------|--------|-----|
| 1 | `hash(businessRef + ":debit")` | 2110 (sender) | 3300 | `gross × 10⁴` | Sender debit |
| 2 | `hash(businessRef + ":credit")` | 3300 | 2110 (receiver) | `net × 10⁴` | Receiver credit |
| 3 | `hash(businessRef + ":4130")` | 3300 | 4130 | `fee × 10⁴` | Fee revenue |

After batch: `account[3300].balance = 0` ✓ (gross = net + fee → ADR-010)

All 3 transfers have `flags.pending = false`. They are committed immediately; no post/void phase.

**Transfer fields (common):**

| Field | Value |
|-------|-------|
| `flags.pending` | `false` |
| `user_data_128` | `coa_trans.id` BIGINT |
| `ledger` | `1` (VND) |
| `code` | use-case specific (TRANSFER) |

### 1.4 `wallet_tx` — Wallet Transactions (PostgreSQL, `wallet` schema)

Two transaction types for the transfer (three on compensation path):

| `tx_type` | Direction | Who | When | `wallet_balance` effect |
|-----------|-----------|-----|------|------------------------|
| `TRANSFER_DEBIT` | DEBIT | Sender | Step 1 (sync) | `available -= gross` |
| `TRANSFER_CREDIT` | CREDIT | Receiver | Step 3 (sync) | `available += net` |
| `ADJUSTMENT_CREDIT` | CREDIT | Sender | Compensation only (step 2 fails) | `available += gross` |

Common columns: `wallet_id`, `business_ref`, `tx_type`, `direction`, `amount`, `available_after`.

**Unique constraint**: `(wallet_id, business_ref, tx_type)` — idempotent replay for each leg.

> No wallet leg for fee. The fee is captured in the TB journal (3300 DR / 4130 CR) only. The sender's wallet sees `TRANSFER_DEBIT amount=gross`; the split between principal and fee is internal to the accounting batch.

### 1.5 `wallet_balance` — Spendable Snapshot (PostgreSQL, `wallet` schema)

| Event | Sender `available` | Receiver `available` |
|-------|--------------------|----------------------|
| TRANSFER_DEBIT (step 1) | − gross | no change |
| TRANSFER_CREDIT (step 3) | no change | + net (= principal) |
| ADJUSTMENT_CREDIT (compensation) | + gross (restored) | no change |

### 1.6 Compensation — no outbox

Unlike withdraw, there is no outbox for internal transfer. Compensation (`ADJUSTMENT_CREDIT`) is issued inline by orchestration when step 2 fails after step 1. It is a direct synchronous call to `app-wallet`. No async worker is involved.

---

## 2. State Transitions

```
[Member submits POST /transfers]
        │
        ▼
  app-orchestration (sync)
        │  Validate (sender ≠ receiver, balance, currency, idempotency)
        │
        ├─ sender == receiver → 422 TRANSFER_SAME_MEMBER
        │
        ├─ available < gross → 422 WALLET_INSUFFICIENT_BALANCE
        │
        └─ OK
              │
              │  Step 1: app-wallet: TRANSFER_DEBIT (sender)
              │          available -= gross
              │
              │  Step 2: app-accounting: createJournal(TRANSFER, POSTED)
              │          TB batch (3 non-pending transfers):
              │            2110_sender DR / 3300 CR  gross
              │            3300 DR / 2110_receiver CR  net
              │            3300 DR / 4130 CR  fee
              │          assert account[3300].balance = 0
              │          coa_trans.status = POSTED
              │
              │  [Step 2 fails] ─────────────────────────────────────────┐
              │                                                           │
              │  Step 3: app-wallet: TRANSFER_CREDIT (receiver)           │
              │          available += net                                 │
              │                                                           │
              │  [Step 3 fails] → forward-retry TRANSFER_CREDIT          │
              │                   idempotently (do not reverse ledger)    │
              │                                                           │
              │  Return 200                                               │
              │                                          ▼               │
              │                             Compensation path:            │
              │                             ADJUSTMENT_CREDIT (sender)    │
              │                             available += gross            │
              │                             return 500 / retry            │
              └───────────────────────────────────────────────────────────┘
```

---

## 3. Inbound & Outbound Entities

### app-orchestration — inbound & outbound

#### Inbound — `TransferRequest` from member via HTTP (JWT auth, ADR-011)

```
POST /transfers   (spec/contracts/http/gtelpay-public.yaml)

TransferRequest {
  receiverMemberId  : BIGINT                  // must differ from sender (JWT sub)
  principalAmount   : string (decimal, s4)    // e.g. "100000.0000"
  businessRef       : string                  // = X-Idempotency-Key
  currency          : string                  // "VND"
  note              : string (optional)       // memo / description
}
```

Sender `memberId` is resolved from JWT. Fee is computed by orchestration from feeSchedule. `gross = principal + fee`, `net = principal`.

---

#### Outbound — `TransferAck` (HTTP 200) to member

```
HTTP 200 OK

TransferAck {
  businessRef       : string
  status            : "COMPLETED"
  grossDebited      : string (decimal, s4)    // sender's wallet debit (principal + fee)
  netCredited       : string (decimal, s4)    // receiver's wallet credit (principal)
  fee               : string (decimal, s4)
}
```

Guarantee: returned only after TRANSFER_DEBIT, createJournal POSTED, and TRANSFER_CREDIT all succeed.

---

### Internal service calls (orchestration → app-wallet, synchronous)

#### Step 1 — `WalletTxRequest` (TRANSFER_DEBIT, sender)

```
POST /wallet-tx   (wallet-internal.yaml)

WalletTxRequest {
  walletId    : BIGINT
  txType      : "TRANSFER_DEBIT"
  direction   : "DEBIT"
  amount      : string (decimal, s4)    // gross
  businessRef : string
  currency    : string
}
```

#### Step 3 — `WalletTxRequest` (TRANSFER_CREDIT, receiver)

```
POST /wallet-tx   (wallet-internal.yaml)

WalletTxRequest {
  walletId    : BIGINT
  txType      : "TRANSFER_CREDIT"
  direction   : "CREDIT"
  amount      : string (decimal, s4)    // net (= principal)
  businessRef : string
  currency    : string
}
```

#### Compensation — `WalletTxRequest` (ADJUSTMENT_CREDIT, sender)

```
POST /wallet-tx   (wallet-internal.yaml)

WalletTxRequest {
  walletId    : BIGINT
  txType      : "ADJUSTMENT_CREDIT"
  direction   : "CREDIT"
  amount      : string (decimal, s4)    // gross (full restore)
  businessRef : string + ":comp"
  currency    : string
}
```

---

### Internal service call (orchestration → app-accounting, synchronous)

#### Step 2 — `CreateJournalRequest` (TRANSFER, POSTED)

```
POST /journals   (accounting-internal.yaml)

CreateJournalRequest {
  referenceId     : string          // businessRef
  useCase         : "TRANSFER"
  status          : "POSTED"        // immediately posted, no PENDING
  senderWalletId  : BIGINT
  receiverWalletId: BIGINT
  principalAmount : string (decimal, s4)
  fee             : string (decimal, s4)
  grossAmount     : string (decimal, s4)
  currency        : string
}
```

`app-accounting` / `core.accounting` submits the 3-transfer TB batch internally and returns `coaTransId`.

---

### Boundary summary

```
              ┌──────────────────────────────────────────────────────────┐
[member]      │ TransferRequest ──► app-orchestration                    │
              │                         │                                │
              │    Step 1 (sync) ───────┤                                │
              │          ▼              │                                │
              │    app-wallet           │                                │
              │    TRANSFER_DEBIT       │                                │
              │    (sender)             │                                │
              │                         │                                │
              │    Step 2 (sync) ───────┤                                │
              │          ▼              │                                │
              │    app-accounting                                        │
              │    createJournal(TRANSFER, POSTED)                       │
              │    [TB batch: 2110→3300→2110+4130]                       │
              │    assert account[3300].balance = 0                      │
              │                         │                                │
              │    Step 3 (sync) ───────┤                                │
              │          ▼              │                                │
              │    app-wallet           │                                │
              │    TRANSFER_CREDIT                                       │
              │    (receiver)           │                                │
              │                         │                                │
              │    app-orchestration ── 200 ──► member                  │
              │                                                          │
              │    [Step 2 fails after Step 1]                           │
              │          ▼                                               │
              │    app-wallet: ADJUSTMENT_CREDIT (sender)                │
              │    return error                                          │
              │                                                          │
              │    [Step 3 fails after Step 2]                           │
              │          ▼                                               │
              │    forward-retry TRANSFER_CREDIT idempotently            │
              │    do NOT reverse TB journal                             │
              └──────────────────────────────────────────────────────────┘
```
