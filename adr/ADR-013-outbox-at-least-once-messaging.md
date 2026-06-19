# ADR-013: Transactional outbox and at-least-once delivery

| Field | Value |
|-------|-------|
| Status | Accepted |
| Date | 2026-06-08 |
| Source | [`core.business-processes.md`](../core.business-processes.md) §15, [`design-v2/orchestration.md`](../design-v2/orchestration.md) §18.2 |
| Related | [ADR-005](ADR-005-idempotency-key-strategy.md), [ADR-008](ADR-008-saga-compensation-no-2pc.md) |

---

## Decision

1. Domain mutation + **outbox row** in **one local DB commit**.
2. Relay publishes S3/S6 **at-least-once**.
3. Consumers idempotent on `(commandType, businessRef)` — ACK after commit or safe no-op.
4. `messageId` dedups transport redelivery only ([ADR-005](ADR-005-idempotency-key-strategy.md)).
5. Poison messages → DLQ `core.commands.dlq` + `core.operations.command-failed` Kafka.

---

## Acceptance criteria (AC-013)

| ID | Criterion |
|----|-----------|
| AC-013-01 | Outbox insert same TX as saga step advance |
| AC-013-02 | Relay retry does not double-apply business effect |
| AC-013-03 | Consumer replay same businessRef → no-op |
| AC-013-04 | Permanent failure emits CommandFailed |
| AC-013-05 | DLQ retains message for ops replay |

---

## Test cases (TC-013)

| ID | Expected | Maps to |
|----|----------|---------|
| TC-013-01 | Commit then relay failure → eventual one publish | X-E07 |
| TC-013-02 | Redelivery same businessRef → idempotent | TC-005-09, X-E07 |
| TC-013-03 | Poison → DLQ + CommandFailed | X-E08 |
| TC-013-04 | Outbox pending after crash → relay completes | AC-013-01 |

---

## References

- [`references/microservices-io-transactional-outbox.md`](../references/microservices-io-transactional-outbox.md)
- [`design-v2/acceptance.md`](../design-v2/acceptance.md) — X-E07, X-E08
