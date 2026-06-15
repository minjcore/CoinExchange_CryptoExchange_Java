# ADR-010: Transit accounts must net to zero at terminal POSTED

| Field | Value |
|-------|-------|
| Status | Accepted |
| Date | 2026-06-08 |
| Deciders | Engineering |
| Related | [`core.foundation.md`](../core.foundation.md) §4–5, §6.3, [`core.accounting.trd.md`](../core.accounting.trd.md), [ADR-001](ADR-001-immutable-ledger.md), [ADR-006](ADR-006-two-phase-deposit.md) |

---

## Context

Use cases route funds through **transit accounts** (3100–3820) so in-flight money is visible and trial balance stays interpretable ([`core.foundation.md`](../core.foundation.md) §6.3).

Foundation §5 invariant 3: each transit returns to **0** after the use case completes. Leaving transit non-zero indicates a stuck or partial journal.

---

## Decision

1. **Per use case transit** — 3100 deposit · 3200 withdraw · 3300 transfer · 3400 IBFT · 3500 payment/QR · 3600 payroll · 3700 disbursement · 3800/3810/3820 EOD.
2. **Terminal POSTED** — For sync flows, transit nets to **zero** in the same journal that reaches POSTED. For deposit, **3100 = 0** only at POSTED (after PENDING hold) — [ADR-006](ADR-006-two-phase-deposit.md).
3. **Post validation** — `postJournal` / `confirmDeposit` rejects if transit for that use case ≠ 0 at POSTED boundary.
4. **EOD batch** — All three settlement transits (3800, 3810, 3820) net zero per successful merchant settlement slice.
5. **Period close** — Non-zero transit blocks close ([`design-v2/accounting.md`](../design-v2/accounting.md) §23.2).
6. **Monitoring** — Aging alerts on PENDING deposit (3100) or stuck batch transits.

---

## Consequences

### Positive

- Clear signal for stuck flows in COA and ops dashboards.
- Aligns double-entry with business completion semantics.

### Negative / trade-offs

- Two-phase deposit temporarily shows 3100 ≠ 0 at PENDING — expected until POSTED.
- EOD partial failure may leave 3810 non-zero until bank retry — documented recovery.

---

## Relationship to other ADRs

| ADR | Topic |
|-----|--------|
| ADR-001 | POSTED journals immutable — transit cleared at post time |
| ADR-006 | Deposit 3100 lifecycle |
| ADR-008 | Stuck transit detected by aging / saga |

---

## Acceptance criteria (AC-010)

| ID | Criterion |
|----|-----------|
| AC-010-01 | Payment POSTED → transit 3500 net zero |
| AC-010-02 | Withdraw POSTED → transit 3200 net zero |
| AC-010-03 | Transfer POSTED → transit 3300 net zero |
| AC-010-04 | IBFT POSTED → transit 3400 net zero |
| AC-010-05 | Deposit POSTED → transit 3100 net zero |
| AC-010-06 | Payroll POSTED → transit 3600 net zero |
| AC-010-07 | Disbursement POSTED → transit 3700 net zero |
| AC-010-08 | EOD success → 3800/3810/3820 net zero per merchant slice |
| AC-010-09 | Unbalanced or non-zero transit → post rejected |

---

## Test cases (TC-010)

| ID | Title | Expected | Maps to |
|----|-------|----------|---------|
| TC-010-01 | Deposit POSTED 3100 | net zero | Deposit happy, DEP-E |
| TC-010-02 | Payment 3500 | net zero | Payment scenarios |
| TC-010-03 | Withdraw 3200 | net zero | Withdraw ledger POSTED |
| TC-010-04 | Transfer 3300 | net zero | Transfer scenarios |
| TC-010-05 | IBFT 3400 | net zero | IBFT-E01, IBFT ledger |
| TC-010-06 | Payroll 3600 | net zero | Payroll feature |
| TC-010-07 | EOD transits | 3800/3810/3820 zero | EOD happy |
| TC-010-08 | PENDING deposit 3100 | non-zero allowed until POSTED | Phase A only |
| TC-010-09 | Period close block | non-zero transit blocks | accounting §23.2 |

---

## References

- [`core.foundation.md`](../core.foundation.md) — §4 Rules, §5 invariants, §6.3 transit COA
- [`design-v2/accounting.md`](../design-v2/accounting.md) — Part II per use case
- [`design-v2/acceptance.md`](../design-v2/acceptance.md) — All flow features assert transit zero at POSTED
