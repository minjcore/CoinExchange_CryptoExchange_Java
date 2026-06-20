# ADR-012: Orchestration-only domain access (forbidden F1–F6)

| Field | Value |
|-------|-------|
| Status | Accepted |
| Date | 2026-06-08 |
| Source | [`integration-surfaces.md`](../integration-surfaces.md) §10, [`IMPLEMENTATION.md`](../IMPLEMENTATION.md) §8 |
| Related | [ADR-002](ADR-002-core-sharedlib.md), [ADR-003](ADR-003-dual-schema-single-postgres.md) |

---

## Decision

| Rule | Decision |
|------|----------|
| F1 | Gateway/partners **never** call S2 or INSERT `wallet_*`/`coa_*` |
| F2 | `core.wallet` must not import accounting repository |
| F3 | `core.accounting` must not mutate `wallet_balance` |
| F4 | Same command uses same field names S1/S3/S6 |
| F5 | No UPDATE finalized `wallet_tx` / posted `coa_trans_data` |
| F6 | `businessRef` in S6 **full body** — not header-only |

Only **orchestration** sequences domain services and workers.

---

## Acceptance criteria (AC-012)

| ID | Criterion |
|----|-----------|
| AC-012-01 | No Gateway route to S2 |
| AC-012-02 | Module graph: wallet ↛ accounting |
| AC-012-03 | Accounting service has no wallet repo |
| AC-012-04 | S6 envelope contains `businessRef` in JSON body |
| AC-012-05 | Orchestration is sole writer to domains via service APIs |

---

## Test cases (TC-012)

| ID | Expected | Maps to |
|----|----------|---------|
| TC-012-01 | Gateway config audit — no S2 upstream | F1 |
| TC-012-02 | Static analysis no cross-import | TC-002-01 |
| TC-012-03 | Accounting post does not touch wallet tables | F3 |
| TC-012-04 | AsyncAPI envelope schema has businessRef | F6 |

---

## References

- [`integration-surfaces.md`](../integration-surfaces.md) — §9–10
- [`design-v2/orchestration.md`](../design-v2/orchestration.md) — §9
