# 004-payment — Quickstart

## What This Is

Wallet Payment (UC-3): a user pays a merchant directly from their on-platform wallet. Synchronous, 3-step, settles in under 1 second.

---

## Flow in 30 Seconds

```
POST /payments  →  PAYMENT_DEBIT (USER)  →  createJournal  →  PAYMENT_CREDIT (MERCHANT)  →  200 OK
```

1. `app-orchestration` receives the request (JWT auth required)
2. Calls `app-wallet` to debit USER wallet (`available -= gross`)
3. Calls `app-accounting` to post a TigerBeetle batch (immediately POSTED — no pending flag)
4. Calls `app-wallet` to credit MERCHANT wallet (`available += net`)
5. Returns 200

v1: net = gross (zero fee).

---

## Try It

### Happy Path

```bash
curl -X POST http://localhost:3000/payments \
  -H "Authorization: Bearer $JWT" \
  -H "Content-Type: application/json" \
  -d '{
    "businessRef": "pay_demo_001",
    "userWalletId": "<user-wallet-uuid>",
    "merchantWalletId": "<merchant-wallet-uuid>",
    "grossAmount": "100.00",
    "currency": "VND"
  }'
```

Expected response (200):
```json
{
  "businessRef": "pay_demo_001",
  "status": "SETTLED",
  "grossAmount": "100.00",
  "netAmount": "100.00"
}
```

### Idempotent Retry

Resubmit the exact same request body. You get back the same 200 response. No duplicate wallet rows, no duplicate TB transfers.

```bash
# Second call — same businessRef, same params
curl -X POST http://localhost:3000/payments \
  -H "Authorization: Bearer $JWT" \
  -H "Content-Type: application/json" \
  -d '{
    "businessRef": "pay_demo_001",
    ...
  }'
# → 200, no side effects
```

### Insufficient Funds

```bash
curl -X POST http://localhost:3000/payments \
  -H "Authorization: Bearer $JWT" \
  -H "Content-Type: application/json" \
  -d '{
    "businessRef": "pay_demo_002",
    "userWalletId": "<user-wallet-uuid>",
    "merchantWalletId": "<merchant-wallet-uuid>",
    "grossAmount": "999999.00",
    "currency": "VND"
  }'
# → 422 Unprocessable Entity
```

---

## Compensation — What Happens When the Ledger Fails

If `createJournal` returns an error after `PAYMENT_DEBIT` has already committed:

1. Orchestration issues `ADJUSTMENT_CREDIT` to `app-wallet` with `businessRef = pay_demo_001:comp`
2. USER available balance is fully restored
3. Merchant is never credited
4. The original payment returns 500 to the caller

No ledger entry exists (TigerBeetle write never committed). The compensation is wallet-only.

---

## Forward Retry — What Happens When Merchant Credit Fails

If `PAYMENT_CREDIT` fails after `createJournal` has already POSTED:

1. The ledger entry is immutable — do NOT attempt to reverse it
2. Orchestration retries `PAYMENT_CREDIT` with the same `businessRef`
3. The wallet idempotency constraint (`unique(wallet_id, business_ref, PAYMENT_CREDIT)`) prevents double-credit
4. Once the retry succeeds, 200 is returned

---

## Key Constraints

| Constraint | Value |
|-----------|-------|
| Idempotency key | `(wallet_id, business_ref, tx_type)` — unique in `wallet_tx` |
| Amount encoding (TigerBeetle) | `amount × 10⁴` (integer minor units) |
| Transit account post-assert | `balance(3500) == 0` after every batch |
| SLA | 1 000 ms (SC-001) |
| Fee in v1 | None (net = gross) |
| Ledger entry status | `POSTED` immediately (no pending flag) |

---

## Accounts Reference

| Code | Name | Movement per payment |
|------|------|----------------------|
| 2110 | USER liability | Debited (user balance goes down) |
| 3500 | Transit — payment | Debited then credited (nets to zero) |
| 2120 | MERCHANT liability | Credited (merchant balance goes up) |

---

## Error Reference

| HTTP Status | Meaning |
|-------------|---------|
| 200 | Payment settled |
| 409 | Duplicate `businessRef` with different parameters |
| 422 | Insufficient USER funds |
| 500 | Internal error (ledger failure, compensation issued) |

---

## ADR References

| ADR | What it governs |
|-----|----------------|
| ADR-001 | Immutable ledger — no reversal after POSTED |
| ADR-005 | businessRef idempotency semantics |
| ADR-009 | Fee computation at orchestration layer (zero in v1) |
| ADR-027 | Synchronous 3-commit payment flow |

---

## Testing Checklist

- [ ] TC-PAY-01: Happy path — USER deducted, MERCHANT credited, balance(3500)=0
- [ ] TC-PAY-02: Insufficient funds — 422, no side effects
- [ ] TC-PAY-03: Zero amount rejected — 422
- [ ] TC-PAY-04: Ledger failure — USER balance restored via ADJUSTMENT_CREDIT
- [ ] TC-PAY-05: Compensation is idempotent on retry
- [ ] TC-PAY-06: PAYMENT_CREDIT forward-retry after ledger POSTED
- [ ] TC-PAY-07: Duplicate businessRef (same params) — idempotent 200
- [ ] TC-PAY-08: Duplicate businessRef (different amount) — 409
