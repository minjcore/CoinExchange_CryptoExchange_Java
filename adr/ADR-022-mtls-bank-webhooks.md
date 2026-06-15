# ADR-022: mTLS for bank and partner deposit webhooks

| Field | Value |
|-------|-------|
| Status | Accepted |
| Date | 2026-06-08 |
| Source | [`core.business-processes.md`](../core.business-processes.md) §17, [`integration-surfaces.md`](../integration-surfaces.md) §4.1 |
| Related | [ADR-011](ADR-011-auth-identity-jwt-subject.md), [ADR-006](ADR-006-two-phase-deposit.md) |

---

## Decision

1. `notifyDeposit` / `bankWebhook` (S1) authenticated via **mTLS** — not end-user JWT.
2. Invalid/missing client cert → **401** — no PENDING journal.
3. VA on webhook maps to `memberId` via orchestration lookup table — not trusted from unauthenticated body alone.
4. Mobile user flows (payment, withdraw) use JWT ([ADR-011](ADR-011-auth-identity-jwt-subject.md)).
5. Webhook `businessRef` = bank transaction id ([ADR-005](ADR-005-idempotency-key-strategy.md)).

---

## Acceptance criteria (AC-022)

| ID | Criterion |
|----|-----------|
| AC-022-01 | notifyDeposit requires valid mTLS |
| AC-022-02 | No cert → 401, no enqueue |
| AC-022-03 | VA → memberId mapping validated |
| AC-022-04 | 202 ack with businessRef on success |

---

## Test cases (TC-022)

| ID | Expected | Maps to |
|----|----------|---------|
| TC-022-01 | Invalid mTLS → 401 | X-E10 |
| TC-022-02 | Valid mTLS → 202 + enqueue | Deposit webhook |
| TC-022-03 | Duplicate webhook idempotent | Deposit duplicate |
| TC-022-04 | Unmapped VA → hold/manual | orchestration deposit preconditions |

---

## References

- [`openapi/gtelpay-public.yaml`](../openapi/gtelpay-public.yaml)
- [`design-v2/acceptance.md`](../design-v2/acceptance.md) — X-E10
