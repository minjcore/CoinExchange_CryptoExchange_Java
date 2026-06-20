# Business Process: Wallet Payment (UC-3)

> **CF page ID:** 51806772 | **Parent:** UC-3 Wallet Payment (51609624)
> **Source of truth:** this file → push to CF
> **See also:** `specs/004-payment/data-model.md`, `specs/004-payment/plan.md`

---

## Overview

| Attribute | Value |
|-----------|-------|
| Pattern | Fully synchronous — 3-commit saga |
| SLA (SC-001) | 1 second |
| Latency estimate | 3 sync hops × 35-75 ms ≈ 105-225 ms (within SLA) |
| Async component | None |
| Fee (v1) | net = gross (no fee in v1) |
| Transit account | **3500** — must = 0 after every batch commit |

---

## Happy Path Flow

```
Member → POST /payments (businessRef, userWalletId, merchantWalletId, grossAmount)
    │
    ▼
app-orchestration
    │  1. Validate (balance, idempotency)
    │  2. app-wallet: PAYMENT_DEBIT — USER available -= gross
    │  3. app-accounting: createJournal(PAYMENT, POSTED)
    │       TB Transfer 1: hash(ref+":2110") — 2110 DR / 3500 CR (gross)
    │       TB Transfer 2: hash(ref+":2120") — 3500 DR / 2120 CR (net=gross)
    │       Assert: account[3500].balance = 0
    │       coa_trans.status = POSTED
    │  4. app-wallet: PAYMENT_CREDIT — MERCHANT available += net
    │  5. Return 200
```

---

## Ledger Mapping (TigerBeetle)

| # | Transfer ID | Debit | Credit | Amount | Leg |
|---|-------------|-------|--------|--------|-----|
| 1 | hash(ref + ":2110") | 2110 (USER) | 3500 (transit) | gross × 10⁴ | USER debit |
| 2 | hash(ref + ":2120") | 3500 (transit) | 2120 (MERCHANT) | net × 10⁴ | MERCHANT credit |

**Invariant INV-P:** account[3500].balance = 0 after every completed batch. Non-zero = fatal — page on-call immediately.

Both transfers are non-pending (flags.pending=false) — POSTED immediately. No two-phase needed.

---

## Compensation Path (Ledger Fail after USER Debit)

If createJournal fails after PAYMENT_DEBIT has committed:

```
[USER_DEBITED]
    │
    ✗ createJournal fails
    │
    ▼ ADJUSTMENT_CREDIT (businessRef:comp) — USER available += gross
    └─ Return 500 (no MERCHANT credit; no TB entry)
```

The `:comp` suffix keeps the compensation row idempotent under `(wallet_id, business_ref, tx_type)`.

---

## Forward-Retry Path (Merchant Credit Fail after POSTED)

```
[LEDGER_POSTED]
    │
    ✗ PAYMENT_CREDIT fails
    │
    ▼ Retry PAYMENT_CREDIT idempotently (unique constraint prevents double-credit)
    └─ Return 200
```

Do NOT reverse the TB journal (ADR-001: corrections are new journals).

---

## Wallet TX Types

| tx_type | Direction | Who | wallet_balance effect |
|---------|-----------|-----|-----------------------|
| PAYMENT_DEBIT | DEBIT | USER wallet | available −= gross |
| PAYMENT_CREDIT | CREDIT | MERCHANT wallet | available += net |
| ADJUSTMENT_CREDIT | CREDIT | USER wallet (compensation) | available += gross (restore) |

Unique constraint: `(wallet_id, business_ref, tx_type)` — idempotent retry for each leg.

---

## Error Responses

| Scenario | HTTP Code | Error Code |
|----------|-----------|------------|
| USER insufficient balance | 422 | WALLET_INSUFFICIENT_BALANCE |
| Duplicate businessRef, different amount | 409 | IDEMPOTENCY_CONFLICT |
| Ledger fail (after compensation) | 500 | INTERNAL_ERROR |

---

## Idempotency Keys

- Accept: `businessRef`
- PAYMENT_DEBIT: `(userWalletId, businessRef, PAYMENT_DEBIT)`
- PAYMENT_CREDIT: `(merchantWalletId, businessRef, PAYMENT_CREDIT)`
- Compensation: `(userWalletId, businessRef:comp, ADJUSTMENT_CREDIT)`
- TB Transfer IDs: `hash(ref+":2110")`, `hash(ref+":2120")` — deterministic

---

## ADR References

- ADR-001: corrections are new journals (not reversals)
- ADR-005: idempotency end-to-end
- ADR-010: transit account 3500 = 0 at terminal
- ADR-027: synchronous HTTP for all inter-service calls (payment flow)
- ADR-031: SQL invariant CI
