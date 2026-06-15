# ADR-035: v1 async execution uses RabbitMQ workers, not Temporal

| Field | Value |
|-------|-------|
| Status | Accepted |
| Date | 2026-06-08 |
| Source | [`design-v2/orchestration.md`](../design-v2/orchestration.md) §25.7, [`references/temporal-saga-patterns.md`](../references/temporal-saga-patterns.md), [ADR-013](ADR-013-outbox-at-least-once-messaging.md), [ADR-008](ADR-008-saga-compensation-no-2pc.md) |
| Related | [ADR-008](ADR-008-saga-compensation-no-2pc.md), [ADR-013](ADR-013-outbox-at-least-once-messaging.md), [ADR-021](ADR-021-aging-jobs-async-pending.md) |

---

## Decision

1. **v1** saga steps, outbox relay, bank poll, and aging jobs run on **RabbitMQ consumers** + DB saga state — not a durable workflow engine (Temporal/Cadence).
2. Saga state table (or equivalent) is the **source of truth** for step progression; messages are **commands**, not the ledger of record.
3. At-least-once delivery handled by idempotency ([ADR-005](ADR-005-idempotency-key-strategy.md)) + outbox ([ADR-013](ADR-013-outbox-at-least-once-messaging.md)).
4. **Temporal** (or similar) is an **optional future** migration if Tmax/T2 complexity or human-in-the-loop steps exceed RabbitMQ ergonomics — not a v1 blocker.
5. Workers must be **horizontally scalable**; partition by `business_ref` hash where ordering per ref is required.

---

## Acceptance criteria (AC-035)

| ID | Criterion |
|----|-----------|
| AC-035-01 | No Temporal dependency in v1 deployment graph |
| AC-035-02 | Saga resume after worker crash uses DB state + redelivered message |
| AC-035-03 | Duplicate command delivery is idempotent |
| AC-035-04 | Aging jobs are scheduled workers, not inline HTTP |
| AC-035-05 | DLQ path documented for poison messages |

---

## Test cases (TC-035)

| ID | Expected | Maps to |
|----|----------|---------|
| TC-035-01 | Worker crash mid-saga — resume from DB | DEP-E06, X-E07 |
| TC-035-02 | Duplicate outbox relay — single effect | DEP-E08 |
| TC-035-03 | Poll worker independent of API pods | Bank poll ADR-033 |
| TC-035-04 | Poison message → DLQ after retry budget | X-E07 |

---

## References

- [`integration-surfaces.md`](../integration-surfaces.md) — RabbitMQ commands
- [`design-v2/orchestration.md`](../design-v2/orchestration.md) — §18.2, §25.7
