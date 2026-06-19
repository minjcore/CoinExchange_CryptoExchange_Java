# ADR-021: Aging jobs for async pending states

| Field | Value |
|-------|-------|
| Status | Accepted |
| Date | 2026-06-08 |
| Source | [`core.business-processes.md`](../core.business-processes.md) §14, §15, [`design-v2/orchestration.md`](../design-v2/orchestration.md) §14.3, §21 |
| Related | [ADR-006](ADR-006-two-phase-deposit.md), [ADR-007](ADR-007-freeze-settle-async-outflow.md) |

---

## Decision

Every async non-terminal state needs an **aging job** with terminal resolution:

| Pending state | Job action |
|---------------|------------|
| Deposit `PENDING` (3100) | Retry `confirmDeposit` or ops reverse A |
| POSTED, wallet not credited | Retry `WALLET_CREDIT` / consumer |
| Frozen, no payout enqueued | Re-enqueue or ops |
| Payout sent, no callback | **Poll bank** ([ADR-007](ADR-007-freeze-settle-async-outflow.md)) |
| Frozen > Tmax | Severity alert — manual |

Nothing stays pending **forever** without escalation.

---

## Acceptance criteria (AC-021)

| ID | Criterion |
|----|-----------|
| AC-021-01 | Metric `deposit_pending_3100_age` monitored |
| AC-021-02 | Metric `wallet_credit_lag_after_posted` |
| AC-021-03 | Metric `frozen_without_payout` |
| AC-021-04 | PENDING > SLA alerts ops |
| AC-021-05 | Frozen > Tmax high severity |

---

## Test cases (TC-021)

| ID | Expected | Maps to |
|----|----------|---------|
| TC-021-01 | PENDING aging retry confirm | business-processes §13.1 |
| TC-021-02 | POSTED 2h no credit alert | X-E06 |
| TC-021-03 | Frozen poll not release | X-E13, TC-007-03 |
| TC-021-04 | IBFT frozen 72h then FAIL release | IBFT-E02 |

---

## References

- [`design-v2/orchestration.md`](../design-v2/orchestration.md) — §21 observability
