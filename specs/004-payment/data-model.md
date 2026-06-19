# 004-payment — Data Model

## Wallet Transaction Types

Three new `tx_type` values are introduced for this feature.

| tx_type | Direction | Description |
|---------|-----------|-------------|
| `PAYMENT_DEBIT` | DEBIT | Deducts gross amount from USER available balance |
| `PAYMENT_CREDIT` | CREDIT | Adds net amount to MERCHANT available balance |
| `ADJUSTMENT_CREDIT` | CREDIT | Compensation only — restores USER available when ledger fails |

---

## wallet_tx Row Schema

Each wallet transaction produces one row in `wallet_tx`.

| Column | Type | Notes |
|--------|------|-------|
| `id` | UUID | PK, generated |
| `wallet_id` | UUID | FK → wallets.id |
| `tx_type` | enum | `PAYMENT_DEBIT` / `PAYMENT_CREDIT` / `ADJUSTMENT_CREDIT` |
| `direction` | enum | `DEBIT` \| `CREDIT` |
| `amount` | numeric(18,4) | Stored in currency units (not scaled) |
| `business_ref` | varchar | Idempotency key supplied by caller |
| `status` | enum | `SETTLED` (sync flow — no pending state) |
| `created_at` | timestamptz | Set at insert |

### Unique Constraint

```sql
UNIQUE (wallet_id, business_ref, tx_type)
```

Enforces idempotency: the same `(wallet, ref, type)` triple can only be created once. A retry of `POST /payments` that reaches `PAYMENT_DEBIT` a second time hits this constraint and returns the existing row rather than inserting a duplicate.

---

## Compensation Row

When the ledger write fails after `PAYMENT_DEBIT`, a compensation row is created:

| Column | Value |
|--------|-------|
| `tx_type` | `ADJUSTMENT_CREDIT` |
| `direction` | `CREDIT` |
| `business_ref` | `{original_business_ref}:comp` |
| `wallet_id` | USER wallet |
| `amount` | gross (same as the original PAYMENT_DEBIT) |

The `:comp` suffix ensures this row satisfies the unique constraint independently from the original debit.

---

## TigerBeetle Account Codes

| Code | Account Name | Normal Balance |
|------|-------------|----------------|
| 2110 | USER liability | Credit (platform owes USER) |
| 3500 | Transit — payment | Zero after every completed batch |
| 2120 | MERCHANT liability | Credit (platform owes MERCHANT) |

No fee account is allocated in v1 (net = gross).

---

## TigerBeetle Transfers

A single payment produces a two-transfer atomic batch.

### Transfer 1 — USER → Transit

| Field | Value |
|-------|-------|
| `id` | `hash(businessRef + ":2110")` |
| `debit_account_id` | 2110 (USER liability) |
| `credit_account_id` | 3500 (transit) |
| `amount` | `gross × 10⁴` |
| `flags` | 0 (non-pending, immediately POSTED) |

### Transfer 2 — Transit → MERCHANT

| Field | Value |
|-------|-------|
| `id` | `hash(businessRef + ":2120")` |
| `debit_account_id` | 3500 (transit) |
| `credit_account_id` | 2120 (MERCHANT liability) |
| `amount` | `net × 10⁴` (= gross × 10⁴ in v1) |
| `flags` | 0 (non-pending, immediately POSTED) |

### Amount Encoding

Amounts are stored as integer minor units scaled by 10⁴:

```
100.00 VND  →  1_000_000
  0.01 VND  →        100
```

### Post-Batch Assertion

After the batch commits, orchestration asserts:

```
account[3500].debits_posted == account[3500].credits_posted
⟹ account[3500].balance == 0
```

A non-zero transit balance indicates a partially applied batch and must be treated as a fatal error.

---

## businessRef Patterns

| Event | businessRef |
|-------|-------------|
| Payment intent | `{caller-supplied ref}` (e.g., `pay_abc123`) |
| USER debit | same as payment intent |
| MERCHANT credit | same as payment intent |
| Compensation credit | `{payment intent ref}:comp` |

---

## HTTP Payload Shapes (abbreviated)

### POST /payments (inbound to app-orchestration)

```json
{
  "businessRef": "pay_abc123",
  "userWalletId": "uuid-user",
  "merchantWalletId": "uuid-merchant",
  "grossAmount": "100.00",
  "currency": "VND"
}
```

### PAYMENT_DEBIT (orchestration → app-wallet)

```json
{
  "walletId": "uuid-user",
  "txType": "PAYMENT_DEBIT",
  "amount": "100.00",
  "businessRef": "pay_abc123"
}
```

### createJournal (orchestration → app-accounting)

```json
{
  "journalType": "PAYMENT",
  "status": "POSTED",
  "businessRef": "pay_abc123",
  "grossAmount": "100.00",
  "netAmount": "100.00",
  "userAccountCode": 2110,
  "merchantAccountCode": 2120
}
```

### PAYMENT_CREDIT (orchestration → app-wallet)

```json
{
  "walletId": "uuid-merchant",
  "txType": "PAYMENT_CREDIT",
  "amount": "100.00",
  "businessRef": "pay_abc123"
}
```

### ADJUSTMENT_CREDIT (orchestration → app-wallet, compensation only)

```json
{
  "walletId": "uuid-user",
  "txType": "ADJUSTMENT_CREDIT",
  "amount": "100.00",
  "businessRef": "pay_abc123:comp"
}
```

---

## State Transitions

### Payment (happy path)

```
START
  │
  ▼
[USER_DEBITED]     — PAYMENT_DEBIT committed
  │
  ▼
[LEDGER_POSTED]    — TigerBeetle batch committed (POSTED)
  │
  ▼
[MERCHANT_CREDITED] — PAYMENT_CREDIT committed
  │
  ▼
COMPLETE (200 OK)
```

### Compensation path (ledger failure)

```
[USER_DEBITED]
  │
  ✗ ledger fails
  │
  ▼
[COMPENSATING]     — issue ADJUSTMENT_CREDIT
  │
  ▼
[COMPENSATED]      — 500 / error returned to caller
```

### Forward-retry path (merchant credit failure)

```
[LEDGER_POSTED]
  │
  ✗ PAYMENT_CREDIT fails
  │
  ▼
[RETRYING_CREDIT]  — retry PAYMENT_CREDIT (idempotent)
  │
  ▼
[MERCHANT_CREDITED]
  │
  ▼
COMPLETE (200 OK)
```
