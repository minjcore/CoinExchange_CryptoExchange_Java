# ADR-008: Saga orchestration with compensation — no two-phase commit across domains

| Field | Value |
|-------|-------|
| Status | Accepted |
| Date | 2026-06-08 |
| Deciders | Engineering |
| Related | [`core.business-processes.md`](../core.business-processes.md) §13–15, [`IMPLEMENTATION.md`](../IMPLEMENTATION.md) §8, [ADR-003](ADR-003-dual-schema-single-postgres.md), [ADR-005](ADR-005-idempotency-key-strategy.md) |

---

## Context

A single business action (payment, transfer, deposit) touches **wallet** and **accounting** in separate schemas ([ADR-003](ADR-003-dual-schema-single-postgres.md)). A distributed two-phase commit (2PC) across domains is **not used**.

[`core.business-processes.md`](../core.business-processes.md) §13 defines per-flow **saga** steps with **forward-retry** or **compensation** — never cross-service rollback of committed work.

[`IMPLEMENTATION.md`](../IMPLEMENTATION.md) §8: payment = **3 separate local commits** (debit → post → credit).

---

## Decision

1. **No 2PC** — Wallet TX and accounting TX commit independently; orchestration sequences steps.
2. **Forward-retry preferred** — After ledger POSTED, retry downstream wallet leg (idempotent) rather than reverse ledger.
3. **Compensation when safe** — If post fails after wallet debit, credit user with `{businessRef}:comp` (orchestration policy).
4. **POSTED ledger not auto-reversed** — Wallet drift fixed by retry credit or ops reversal journal — not wallet writing COA.
5. **Outbox** — Domain state + outbox in one local commit; relay at-least-once ([`core.business-processes.md`](../core.business-processes.md) §15).
6. **Saga record** — Orchestration stores `business_ref`, `step`, `status`, copied ids — no cross-schema JOIN for recovery ([`design-v2/orchestration.md`](../design-v2/orchestration.md) §18.1).
7. **DLQ + CommandFailed** — Poison messages to ops; not infinite retry without budget.

---

## Consequences

### Positive

- Matches microservices saga pattern; domains stay deployable independently.
- Idempotency ([ADR-005](ADR-005-idempotency-key-strategy.md)) makes retries safe after partial failure.

### Negative / trade-offs

- Temporary inconsistent states (user debited, merchant not credited) require monitoring and forward-retry.
- Compensation paths must be explicitly designed per flow.

---

## Relationship to other ADRs

| ADR | Topic |
|-----|--------|
| ADR-003 | Separate schemas enforce separate commits |
| ADR-005 | Retry-safe legs |
| ADR-007 | Withdraw saga — poll not compensate on timeout |

---

## Acceptance criteria (AC-008)

| ID | Criterion |
|----|-----------|
| AC-008-01 | Payment: three separate commits (debit, post, credit) — no XA |
| AC-008-02 | Post fails after debit → compensating credit user per policy |
| AC-008-03 | Post succeeds, merchant credit fails → forward-retry credit only |
| AC-008-04 | POSTED journal not undone by wallet module |
| AC-008-05 | Outbox written in same TX as domain mutation |
| AC-008-06 | Consumer ACK after commit or idempotent no-op |
| AC-008-07 | Orchestration saga record tracks last completed step |

---

## Test cases (TC-008)

| ID | Title | Expected | Maps to |
|----|-------|----------|---------|
| TC-008-01 | Payment post fails after debit | User credited `:comp` | Payment compensate |
| TC-008-02 | Merchant credit retry | Ledger stands; credit succeeds on retry | Payment forward-retry |
| TC-008-03 | POSTED + wallet lag | Retry credit idempotent | Deposit POSTED wallet fail |
| TC-008-04 | No cross-schema TX | Integration test / review | TC-003-06 |
| TC-008-05 | Outbox relay | X-E07 single publish effect | X-E07 |
| TC-008-06 | DLQ poison | X-E08 CommandFailed | X-E08 |
| TC-008-07 | Transfer compensate A | Credit A if post never POSTED | Transfer scenarios |

---

## References

- [`core.business-processes.md`](../core.business-processes.md) — §13–15
- [`IMPLEMENTATION.md`](../IMPLEMENTATION.md) — §8 Orchestration transaction boundaries
- [`design-v2/orchestration.md`](../design-v2/orchestration.md) — §4, §11–18
- [`design-v2/acceptance.md`](../design-v2/acceptance.md) — Payment, Transfer, X-E
