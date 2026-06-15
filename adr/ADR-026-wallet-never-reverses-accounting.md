# ADR-026: Wallet module never reverses or posts accounting journals

| Field | Value |
|-------|-------|
| Status | Accepted |
| Date | 2026-06-08 |
| Source | [`design-v2/wallet.md`](../design-v2/wallet.md) §7, [`core.wallet.md`](../core.wallet.md) §8 |
| Related | [ADR-001](ADR-001-immutable-ledger.md), [ADR-008](ADR-008-saga-compensation-no-2pc.md) |

---

## Decision

1. Wallet **never** calls `reverseJournal` or creates `coa_trans`.
2. POSTED but wallet credit failed → **retry credit** — not ledger reversal from wallet.
3. Wrong wallet credit → ops `ADJUSTMENT_*` wallet row + accounting reversal via S2 (orchestration).
4. Wallet compensation = new `wallet_tx` only — orchestration-owned policy.

---

## Acceptance criteria (AC-026)

| ID | Criterion |
|----|-----------|
| AC-026-01 | Wallet module no accounting imports |
| AC-026-02 | No reverseJournal in wallet package |
| AC-026-03 | Ledger POSTED + wallet fail → retry path documented |
| AC-026-04 | ADJUSTMENT uses new business_ref |

---

## Test cases (TC-026)

| ID | Expected | Maps to |
|----|----------|---------|
| TC-026-01 | POSTED wallet consumer fail retry | Deposit stuck recovery |
| TC-026-02 | ADJUSTMENT_DEBIT after wrong credit | ADJ-E02 |
| TC-026-03 | Wallet cannot invoke S2 | ADR-012 F2 |

---

## References

- [`design-v2/wallet.md`](../design-v2/wallet.md) — §7.1
