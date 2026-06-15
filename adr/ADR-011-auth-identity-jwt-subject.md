# ADR-011: Authorization — `memberId` from JWT subject, not request body

| Field | Value |
|-------|-------|
| Status | Accepted |
| Date | 2026-06-08 |
| Source | [`core.business-processes.md`](../core.business-processes.md) §17, [`design-v2/orchestration.md`](../design-v2/orchestration.md) §19 |
| Related | [`openapi/gtelpay-public.yaml`](../openapi/gtelpay-public.yaml), [ADR-005](ADR-005-idempotency-key-strategy.md) |

---

## Decision

1. Public S1: Bearer JWT (OIDC) validated at Gateway/orchestration.
2. **`memberId` for authorization** = token `sub` (platform mid) — **never** trust body alone.
3. Body `memberId` / `payerId` must **match** principal or request rejected **403**.
4. Idempotency key scoped per principal — member A's key cannot replay member B's action.
5. Merchant payroll / partner disbursement: JWT scope = MERCHANT or PARTNER lane owner.
6. Bank `notifyDeposit`: **mTLS** channel — VA lookup maps to `memberId` (see [ADR-022](ADR-022-mtls-bank-webhooks.md)).

---

## Acceptance criteria (AC-011)

| ID | Criterion |
|----|-----------|
| AC-011-01 | Payment: token sub must equal payer |
| AC-011-02 | Body payerId ≠ sub → 403 |
| AC-011-03 | Withdraw: only sub's USER wallet |
| AC-011-04 | Idempotency key not portable across members |
| AC-011-05 | S2/RabbitMQ use service token — not end-user JWT |

---

## Test cases (TC-011)

| ID | Expected | Maps to |
|----|----------|---------|
| TC-011-01 | sub=100234 pay OK | Payment auth matrix |
| TC-011-02 | body payer 100235 → 403 | `acceptance.md` X-E09 |
| TC-011-03 | Merchant payroll wrong JWT → 403 | orchestration §19 |
| TC-011-04 | Same idempotency key different member → reject | AC-011-04 |

---

## References

- [`core.business-processes.md`](../core.business-processes.md) — §17
- [`design-v2/acceptance.md`](../design-v2/acceptance.md) — X-E09
