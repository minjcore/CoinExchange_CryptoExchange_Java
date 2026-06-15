# ADR-034: LOCKED wallet rejects deposit credit (W-O1 closed)

| Field | Value |
|-------|-------|
| Status | Accepted |
| Date | 2026-06-08 |
| Source | [`design-v2/wallet.md`](../design-v2/wallet.md) §12, W-O1, [ADR-029](ADR-029-wallet-locked-rejects-mutation.md) |
| Related | [ADR-029](ADR-029-wallet-locked-rejects-mutation.md), [ADR-024](ADR-024-deposit-wallet-credit-dual-path.md), [ADR-006](ADR-006-two-phase-deposit.md) |

---

## Decision

1. **W-O1 closed for v1:** `wallet.status = LOCKED` → **reject** `DEPOSIT_CREDIT` / `WALLET_CREDIT` with `WALLET_LOCKED`.
2. POSTED deposit journal may still exist in accounting — wallet credit is **held** until wallet unlocked by ops, then **retry** credit idempotently ([ADR-024](ADR-024-deposit-wallet-credit-dual-path.md)).
3. Orchestration must not silently skip credit — saga stays `AWAITING_WALLET` or equivalent until ACTIVE.
4. Ops path: unlock wallet → aging job retries `WALLET_CREDIT` → single `wallet_tx`.
5. Supersedes open wording in [ADR-029](ADR-029-wallet-locked-rejects-mutation.md) AC-029-03 for v1.

---

## Acceptance criteria (AC-034)

| ID | Criterion |
|----|-----------|
| AC-034-01 | LOCKED + POSTED deposit → credit command rejected |
| AC-034-02 | No `wallet_tx` on rejected credit |
| AC-034-03 | Unlock + retry → exactly one DEPOSIT_CREDIT |
| AC-034-04 | W5 may show timing drift until unlock — expected |
| AC-034-05 | Saga documents AWAITING_WALLET state |

---

## Test cases (TC-034)

| ID | Expected | Maps to |
|----|----------|---------|
| TC-034-01 | LOCKED deposit credit reject | Wallet lock, DEP-E11 |
| TC-034-02 | Unlock retry idempotent | ADR-024 path |
| TC-034-03 | POSTED exists wallet uncredited | W5 timing drift |
| TC-034-04 | ACTIVE wallet same ref credits normally | Deposit happy |

---

## References

- [`design-v2/wallet.md`](../design-v2/wallet.md) — W-O1 table
- [`design-v2/acceptance.md`](../design-v2/acceptance.md) — LOCKED deposit feature
