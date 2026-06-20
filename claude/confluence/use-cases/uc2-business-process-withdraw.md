# Business Process: Withdraw (UC-2)

> **CF page ID:** 51872199 | **Parent:** UC-2 Withdraw (50332049)
> **Source of truth:** this file → push to CF
> **See also:** `specs/003-withdraw/data-model.md`, `specs/003-withdraw/plan.md`

---

## Overview

| Attribute | Value |
|-----------|-------|
| Pattern | Freeze-settle-release — sync accept + async payout |
| SLA (SC-001) | Accept < 500 ms; payout async (bank-dependent) |
| Latency estimate (accept) | 2 sync hops × 35-75 ms ≈ 70-150 ms (within SLA) |
| Async component | Payout worker → bank dispatch via RabbitMQ |
| Fee (v1) | gross = principal + fee; fee computed by orchestration before freeze |
| Transit account | **3200** — must = 0 after POSTED or FAILED |

---

## Accept Flow (Sync, before 200)

```
Member → POST /withdrawals (businessRef, memberId, amount, currency, useFreeze=true)
    │
    ▼
app-orchestration
    │  1. Validate (balance, idempotency, useFreeze=true)
    │  2. app-wallet: WITHDRAW_FREEZE — available -= gross, frozen += gross
    │  3. app-accounting: createPendingWithdraw(businessRef, gross)
    │       Phase A: 2110 DR gross / 3200 CR gross  [PENDING]
    │  4. Return 200 { status: ACCEPTED, coaTransId, frozen }
    │
    ▼
Outbox → RabbitMQ WITHDRAW_PAYOUT (async, not yet implemented)
```

---

## Settle Flow (Async, on bank SUCCESS)

```
app-payout-worker
    │  1. Bank confirms transfer OK
    │  2. app-accounting: confirmWithdraw(coaTransId, principal, fee)
    │       Phase B: 3200 DR gross / 1111 CR principal / 4120 CR fee
    │       Assert: account[3200].balance = 0
    │       coa_trans.status = POSTED
    │  3. app-wallet: WITHDRAW_SETTLE — frozen -= gross
    │  4. Publish WithdrawSettledEvent (optional)
```

---

## Release Flow (Async, on bank FAIL / timeout)

```
app-payout-worker
    │  1. Bank confirms terminal failure
    │  2. app-accounting: voidWithdraw(coaTransId)
    │       coa_trans.status = FAILED  (no Phase B lines)
    │  3. app-wallet: WITHDRAW_RELEASE — frozen -= gross, available += gross
    │  4. Publish WithdrawReleasedEvent (optional)
```

---

## Ledger Mapping

### Phase A — Accept (PENDING)

| # | Account | Side | Amount | Note |
|---|---------|------|--------|------|
| 1 | 2110 (Wallet balance — User) | DEBIT | gross | USER liability reduced |
| 2 | 3200 (Transit — withdraw) | CREDIT | gross | Funds held in transit |

### Phase B — Settle (POSTED, added on bank success)

| # | Account | Side | Amount | Note |
|---|---------|------|--------|------|
| 3 | 3200 (Transit — withdraw) | DEBIT | gross | Clear transit |
| 4 | 1111 (Vietinbank — dedicated) | CREDIT | principal | Bank asset +principal |
| 5 | 4120 (Fee revenue — withdraw) | CREDIT | fee | Revenue recognised |

**Invariant INV-W:** `account[3200].balance = 0` after every POSTED or FAILED withdraw. Non-zero = fatal — page on-call immediately.

---

## Wallet TX Types

| tx_type | Direction | wallet_balance effect |
|---------|-----------|-----------------------|
| WITHDRAW_FREEZE | DEBIT | available −= gross, frozen += gross |
| WITHDRAW_SETTLE | DEBIT | frozen −= gross |
| WITHDRAW_RELEASE | CREDIT | frozen −= gross, available += gross |

Unique constraint: `(wallet_id, business_ref, tx_type)` — idempotent retry for each leg.

---

## Error Responses

| Scenario | HTTP | Error code |
|----------|------|------------|
| Insufficient available balance | 422 | WALLET_INSUFFICIENT_BALANCE |
| Duplicate businessRef, different amount | 409 | IDEMPOTENCY_CONFLICT |
| useFreeze = false | 422 | VALIDATION_ERROR |
| Wallet LOCKED | 422 | WALLET_LOCKED |

---

## Idempotency Keys

- Accept: `businessRef` → `coa_trans(reference_id, use_case=WITHDRAW)` unique constraint
- WITHDRAW_FREEZE: `(walletId, businessRef, WITHDRAW_FREEZE)`
- WITHDRAW_SETTLE: `(walletId, businessRef:settle, WITHDRAW_SETTLE)`
- WITHDRAW_RELEASE: `(walletId, businessRef:release, WITHDRAW_RELEASE)`

---

## ADR References

| ADR | Title | Status |
|-----|-------|--------|
| ADR-005 | Idempotency key strategy | Published |
| ADR-007 | Freeze-settle-release async outflow | Pending review |
| ADR-008 | Orchestration as sole sequencer | Pending review |
| ADR-009 | Fee computation owned by orchestration | Published |
| ADR-010 | Transit account invariant | Pending review |
| ADR-013 | Transactional outbox + at-least-once delivery | Published |
| ADR-027 | Sync HTTP for accept path | Pending review |
| ADR-029 | Wallet LOCKED rejects freeze | Pending review |
| ADR-033 | Bank poll T1/T2/Tmax; timeout ≠ release | Pending review |
