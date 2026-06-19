# ADR-023: Accounting period close blocks mutation of closed periods

| Field | Value |
|-------|-------|
| Status | Accepted |
| Date | 2026-06-08 |
| Source | [`core.accounting.trd.md`](../core.accounting.trd.md) FR-8, [`design-v2/accounting.md`](../design-v2/accounting.md) §23.2 |
| Related | [ADR-001](ADR-001-immutable-ledger.md) |

---

## Decision

1. Period states: Open → Closed (Locked).
2. **Closed/Locked:** no new postings with `posting_date` in closed period.
3. Corrections → **reversal journal** in **open** period ([ADR-001](ADR-001-immutable-ledger.md)).
4. Period close blocked if any transit account non-zero ([ADR-010](ADR-010-transit-accounts-net-zero.md)).
5. Period close blocked if open PENDING deposits (policy) or waiver list.
6. Trial balance must balance at close.

---

## Acceptance criteria (AC-023)

| ID | Criterion |
|----|-----------|
| AC-023-01 | Post to closed period → `ACCOUNTING_PERIOD_CLOSED` |
| AC-023-02 | Reversal in open period links `reverses_id` |
| AC-023-03 | Close with non-zero transit → reject |
| AC-023-04 | No UPDATE on closed `coa_trans_data` |

---

## Test cases (TC-023)

| ID | Expected | Maps to |
|----|----------|---------|
| TC-023-01 | Post closed period rejected | X-E03 |
| TC-023-02 | Reversal chain | X-E04, ADJ-E01 |
| TC-023-03 | Close blocked transit | accounting §23.2 |
| TC-023-04 | PENDING deposit blocks close | ops policy |

---

## References

- [`core.accounting.trd.md`](../core.accounting.trd.md) — FR-8
- [`design-v2/accounting.md`](../design-v2/accounting.md) — §23.2
