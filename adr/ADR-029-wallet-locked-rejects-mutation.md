# ADR-029: LOCKED wallet rejects debit and freeze — credit policy W-O1

| Field | Value |
|-------|-------|
| Status | Accepted |
| Date | 2026-06-08 |
| Source | [`core.wallet.md`](../core.wallet.md) FR-6, §8, [`design-v2/wallet.md`](../design-v2/wallet.md) §12 |
| Related | [ADR-004](ADR-004-wallet-balance-snapshot.md) |

---

## Decision

1. `wallet.status = LOCKED` → reject **debit** and **freeze** (`WALLET_LOCKED`).
2. **Settle/release** on existing frozen hold — allowed if already in-flight (or ops-gated) per product.
3. **Deposit credit** on LOCKED wallet — **W-O1 closed v1**: **reject** ([ADR-034](ADR-034-locked-wallet-deposit-credit-reject.md)).
4. Admin unlock restores ACTIVE — out of band ops.

---

## Acceptance criteria (AC-029)

| ID | Criterion |
|----|-----------|
| AC-029-01 | LOCKED debit → WALLET_LOCKED |
| AC-029-02 | LOCKED freeze on withdraw → reject before freeze |
| AC-029-03 | LOCKED deposit credit → reject — see ADR-034 |
| AC-029-04 | No wallet_tx on rejected LOCKED ops |

---

## Test cases (TC-029)

| ID | Expected | Maps to |
|----|----------|---------|
| TC-029-01 | LOCKED debit rejected | Wallet lock feature |
| TC-029-02 | LOCKED withdraw reject | Wallet lock freeze |
| TC-029-03 | LOCKED deposit reject | ADR-034, Wallet lock |
| TC-029-04 | ACTIVE after unlock works | ops path |

---

## References

- [`design-v2/acceptance.md`](../design-v2/acceptance.md) — Wallet lock feature
