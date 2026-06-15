# ADR-007: Freeze–settle–release for async outflows (withdraw / IBFT)

| Field | Value |
|-------|-------|
| Status | Accepted |
| Date | 2026-06-08 |
| Deciders | Engineering |
| Related | [`IMPLEMENTATION.md`](../IMPLEMENTATION.md) §2 D5, [`core.business-processes.md`](../core.business-processes.md) §13.4–13.5, [`core.wallet.md`](../core.wallet.md) §5.4–5.5, [ADR-004](ADR-004-wallet-balance-snapshot.md), [ADR-005](ADR-005-idempotency-key-strategy.md) |

---

## Context

Withdraw and IBFT have an **async bank/Napas leg** after API accept. Debiting `available` immediately on accept risks **double-spend** if the bank later succeeds while the app also releases funds on timeout.

[`IMPLEMENTATION.md`](../IMPLEMENTATION.md) §2 D5 and §2.1 define **freeze on accept**, then **settle** (debit frozen) or **release** (unfreeze). There is **no** `WITHDRAW_DEBIT` that debits `available` directly on accept.

[`core.business-processes.md`](../core.business-processes.md) §13.4: **timeout ≠ failure** — poll until terminal bank status before RELEASE.

---

## Decision

1. **Accept** — `WITHDRAW_FREEZE` / `IBFT_FREEZE` moves gross (principal + fee) from `available` → `frozen` **before** S1 **200**.
2. **No immediate debit** — v1 does not debit `available` on accept; settle debits **frozen** only.
3. **Terminal SUCCESS** — `WITHDRAW_SETTLE` / `IBFT_SETTLE` with sub-key `{businessRef}:settle`.
4. **Terminal FAIL** — `WITHDRAW_RELEASE` / `IBFT_RELEASE` with sub-key `{businessRef}:release`.
5. **UNKNOWN / timeout** — **Do not RELEASE**; poll bank/Napas until SUCCESS | FAILED | REJECTED | EXPIRED.
6. **Accounting** — POSTED on accept (transit **3200** / **3400** = 0) per orchestration policy; wallet hold is independent of bank poll timing.
7. **Idempotency** — D5 sub-keys; adapter dedupes payout on `businessRef`.

---

## Consequences

### Positive

- Prevents spend of funds already earmarked for payout.
- `available + frozen` constant during hold — clear member exposure.
- Aligns with banking-core hold semantics ([`references/mambu-transaction-holds.md`](../references/mambu-transaction-holds.md)).

### Negative / trade-offs

- Frozen funds unavailable until terminal resolution — UX must show `frozen`.
- Requires payout status poll + aging jobs ([`design-v2/orchestration.md`](../design-v2/orchestration.md) §23).

---

## Relationship to other ADRs

| ADR | Topic |
|-----|--------|
| ADR-004 | Freeze/settle in same TX as `wallet_tx` |
| ADR-005 | Sub-keys `:settle` / `:release` |
| ADR-008 | Saga recovery when settle fails after bank success |

---

## Acceptance criteria (AC-007)

| ID | Criterion |
|----|-----------|
| AC-007-01 | Accept requires successful FREEZE before HTTP 200 |
| AC-007-02 | No `WITHDRAW_DEBIT` / direct available debit on accept |
| AC-007-03 | SETTLE reduces `frozen` only; `available` unchanged |
| AC-007-04 | RELEASE restores `available` from `frozen` |
| AC-007-05 | Bank UNKNOWN → frozen unchanged; no RELEASE |
| AC-007-06 | SETTLE after RELEASE forbidden (state machine) |
| AC-007-07 | IBFT uses `IBFT_*` tx_types; same rules as withdraw |
| AC-007-08 | Payout adapter idempotent on `businessRef` |

---

## Test cases (TC-007)

| ID | Title | Expected | Maps to |
|----|-------|----------|---------|
| TC-007-01 | Happy freeze → settle | frozen→0 after bank OK | Withdraw happy, IBFT happy |
| TC-007-02 | Bank fail → release | available restored | Withdraw/IBFT bank fail |
| TC-007-03 | Timeout no release | frozen unchanged | Withdraw timeout, IBFT timeout, X-E13 |
| TC-007-04 | SETTLE idempotent | `:settle` replay no-op | Withdraw/IBFT SETTLE idempotent |
| TC-007-05 | Insufficient no freeze | reject before 200 | Insufficient available |
| TC-007-06 | available+frozen constant | sum unchanged during hold | Balance semantics freeze |
| TC-007-07 | RELEASE after SETTLE blocked | X-E12 | IBFT/WITHDRAW state |

---

## References

- [`IMPLEMENTATION.md`](../IMPLEMENTATION.md) — §2 D5, §2.1 enum
- [`core.business-processes.md`](../core.business-processes.md) — §13.4, §13.5
- [`design-v2/wallet.md`](../design-v2/wallet.md) — §4.1, §18–19
- [`design-v2/orchestration.md`](../design-v2/orchestration.md) — §14–15, §23
- [`design-v2/acceptance.md`](../design-v2/acceptance.md) — Withdraw, IBFT, Balance semantics
