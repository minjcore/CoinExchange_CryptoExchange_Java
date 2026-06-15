# ADR-031: SQL ledger invariant checks in CI / nightly

| Field | Value |
|-------|-------|
| Status | Accepted |
| Date | 2026-06-08 |
| Source | [`core.foundation.md`](../core.foundation.md) §5, [`design-v2/accounting.md`](../design-v2/accounting.md) §27, [`references/medium-slope-payments-ledger-pitfalls.md`](../references/medium-slope-payments-ledger-pitfalls.md) |
| Related | [ADR-001](ADR-001-immutable-ledger.md), [ADR-010](ADR-010-transit-accounts-net-zero.md), [ADR-014](ADR-014-reconciliation-w5-report-only.md) |

---

## Decision

1. **Automated SQL invariant suite** runs after integration tests in CI and on a **nightly** schedule against a DB snapshot (or ephemeral test DB seeded from fixtures).
2. Failed invariant → **build / job fails** — no silent drift (Slope P2).
3. Checks are **read-only** — never auto-correct COA or wallet.
4. W5 cross-domain drift remains report-only ([ADR-014](ADR-014-reconciliation-w5-report-only.md)); invariant job may **include** W5 as informational with separate severity.

### Required invariants (v1)

| ID | Check | Source |
|----|-------|--------|
| INV-01 | Every `POSTED` `coa_trans`: `SUM(DR) = SUM(CR)` per `coa_trans_id` | FR-4, ADR-001 |
| INV-02 | No duplicate `(business_ref, use_case)` on `POSTED` journals | ADR-005 |
| INV-03 | Transit accounts **3100–3820** net **0** for each completed `POSTED` use case (per journal or per business_ref policy in `IMPLEMENTATION.md`) | ADR-010, foundation §5.3 |
| INV-04 | No `UPDATE`/`DELETE` on `coa_trans_data` rows linked to `POSTED` parent | ADR-001 |
| INV-05 | `wallet_tx`: every balance change has matching snapshot delta (W2 spot-check query) | ADR-004 |

### Optional / nightly-only

| ID | Check | Notes |
|----|-------|-------|
| INV-06 | Foundation §5.1: `(1111+1112+1113)` vs `(2110+2120+2130)` | Timing lag for async wallet credit — use tolerance window |
| INV-07 | W5 two-query drift beyond tolerance | Same thresholds as ADR-014 |

5. SQL lives in repo under `sql/invariants/` (implementation binding) — **not** duplicated in design-v2.

---

## Acceptance criteria (AC-031)

| ID | Criterion |
|----|-----------|
| AC-031-01 | CI pipeline runs INV-01…INV-05 on green integration test DB |
| AC-031-02 | INV-01 failure blocks merge |
| AC-031-03 | INV-03 covers all transit codes 3100–3820 |
| AC-031-04 | Nightly job runs INV-06/07 with documented tolerance |
| AC-031-05 | Invariant failure emits structured alert (table, row count, sample ids) |

---

## Test cases (TC-031)

| ID | Expected | Maps to |
|----|----------|---------|
| TC-031-01 | Balanced POSTED journal passes INV-01 | Deposit happy |
| TC-031-02 | Deliberate DR≠CR fixture fails INV-01 | CI gate |
| TC-031-03 | Stuck 3100 on POSTED deposit fails INV-03 | ADR-006/010 |
| TC-031-04 | Duplicate business_ref POSTED fails INV-02 | Idempotency conflicts |
| TC-031-05 | W2 bypass fixture fails INV-05 | ADR-004 |
| TC-031-06 | Nightly W5 drift over tolerance logs INV-07 | X-E06 |

---

## References

- [`design-v2/accounting.md`](../design-v2/accounting.md) — §27 pitfalls P2
- [`design-v2/acceptance.md`](../design-v2/acceptance.md) — Ledger invariant CI feature
