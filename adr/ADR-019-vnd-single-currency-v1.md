# ADR-019: VND single currency v1 — no implicit FX

| Field | Value |
|-------|-------|
| Status | Pending review |
| Date | 2026-06-08 |
| Source | [`core.wallet.md`](../core.wallet.md) §3.2, [`core.business-processes.md`](../core.business-processes.md) §14, [`core.sharedlib.md`](../core.sharedlib.md) §6 |
| Related | [ADR-009](ADR-009-fee-ownership-orchestration.md) |

---

## Decision

1. Wallet `currency` = **VND** only in v1.
2. All amounts **scale 4**, `HALF_UP` at orchestration boundary.
3. Requests with non-VND currency → **400** reject before mutation.
4. Multi-currency COA ([`core.accounting.trd.md`](../core.accounting.trd.md) FR-9) deferred — rates immutable after post when enabled later.
5. No FX wallet lanes in v1 ([`references/moderntreasury-fx-wallets-tutorial.md`](../references/moderntreasury-fx-wallets-tutorial.md) out of scope).

---

## Acceptance criteria (AC-019)

| ID | Criterion |
|----|-----------|
| AC-019-01 | wallet.currency CHAR(3) = VND for v1 rows |
| AC-019-02 | Non-VND deposit/payment rejected at boundary |
| AC-019-03 | Amount scale ≤ 4 enforced |
| AC-019-04 | Zero/negative amount rejected before TX |

---

## Test cases (TC-019)

| ID | Expected | Maps to |
|----|----------|---------|
| TC-019-01 | USD payment → 400 | Edge validation (orchestration preconditions) |
| TC-019-02 | DEP-E01 minimum 1 unit | Deposit extended |
| TC-019-03 | scale 5 amount → 400 | Amount validation |
| TC-019-04 | fee rounding HALF_UP deposit 99k net | TC-009-01 |

---

## References

- [`design-v2/orchestration.md`](../design-v2/orchestration.md) — preconditions §11.1
- [`TERMINOLOGY.md`](../TERMINOLOGY.md)
