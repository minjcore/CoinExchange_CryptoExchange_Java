# ADR-014: Wallet–COA reconciliation (W5) is report-only

| Field | Value |
|-------|-------|
| Status | Accepted |
| Date | 2026-06-08 |
| Source | [`core.wallet.md`](../core.wallet.md) §6 W5, [`core.business-processes.md`](../core.business-processes.md) §14–15 |
| Related | [ADR-003](ADR-003-dual-schema-single-postgres.md), [ADR-004](ADR-004-wallet-balance-snapshot.md) |

---

## Decision

1. Periodic job: `SUM(available+frozen)` by `wallet_type` vs control **2110/2120/2130**.
2. **Timing tolerance** for async lag (deposit POSTED → wallet credit).
3. Drift beyond SLA → **alert** — ops investigates.
4. **Wallet never writes COA** to fix drift.
5. **Accounting never writes wallet_balance** to fix drift.
6. Corrections: ops reversal journal + wallet `ADJUSTMENT_*` with new refs.

---

## Acceptance criteria (AC-014)

| ID | Criterion |
|----|-----------|
| AC-014-01 | Recon uses two queries — no cross-schema JOIN |
| AC-014-02 | Within tolerance → log only |
| AC-014-03 | Beyond tolerance → high severity alert |
| AC-014-04 | No auto-adjust COA from wallet job |
| AC-014-05 | Report lists candidate missing credits by businessRef |

---

## Test cases (TC-014)

| ID | Expected | Maps to |
|----|----------|---------|
| TC-014-01 | POSTED 1min lag — no high alert | X-E05, W5 timing |
| TC-014-02 | POSTED 2h no credit — high alert | X-E06 |
| TC-014-03 | Persistent drift — no COA auto-write | Reconciliation feature |
| TC-014-04 | Two-query recon implementation | TC-003-07 |

---

## References

- [`design-v2/wallet.md`](../design-v2/wallet.md) — §11, §26.3
- [`design-v2/acceptance.md`](../design-v2/acceptance.md) — Reconciliation W5
