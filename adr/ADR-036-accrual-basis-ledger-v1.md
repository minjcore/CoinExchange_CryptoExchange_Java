# ADR-036: Accrual-like recognition basis for platform ledger v1

| Field | Value |
|-------|-------|
| Status | Accepted |
| Date | 2026-06-08 |
| Source | [`design-v2/accounting.md`](../design-v2/accounting.md) §29.2, [`references/investopedia-accrual-accounting.md`](../references/investopedia-accrual-accounting.md), [`references/vn-luat-ke-toan-2015-dieu-5-7.md`](../references/vn-luat-ke-toan-2015-dieu-5-7.md) Điều 6 |
| Related | [ADR-006](ADR-006-two-phase-deposit.md), [ADR-007](ADR-007-freeze-settle-async-outflow.md), [ADR-019](ADR-019-vnd-single-currency-v1.md), [ADR-034](ADR-034-locked-wallet-deposit-credit-reject.md) |

---

## Decision

1. Platform ledger v1 uses **accrual-like** recognition — **not cash basis** — for user-facing balances and COA liability postings.
2. **Recognition triggers** (when economic obligation/right is established in domain model, not when bank cash moves):

| Flow | Accrual event | Cash event (async) |
|------|---------------|-------------------|
| Deposit | Phase B **POSTED** → 2110 CR, 3100 cleared | Bank webhook phase A; settlement T+N |
| Withdraw / IBFT | **POSTED** on accept + wallet **freeze** | Bank payout terminal → SETTLE |
| Payment / transfer | Sync **POSTED** + wallet debit/credit | N/A (internal) |
| Fee | Same journal as movement ([ADR-009](ADR-009-fee-ownership-orchestration.md)) | N/A |

3. User **`available`** reflects **spendable right after POSTED** — not "cash in bank account" ([`wallet.md`](../design-v2/wallet.md) §2.1).
4. **PENDING** deposit (3100) and **`frozen`** outflow are accrual **in-flight** states — not separate cash-basis columns.
5. **Cash basis** would credit wallet on bank receipt alone — **explicitly rejected** v1 (double-count risk vs POSTED path).
6. Aligns with VN **substance over form** (Luật KT 2015 Điều 6.6) and GAAP **revenue recognition / matching** (accounting.md §29.2). Full VAS/IFRS statutory BCTC is out of orchestration scope.

---

## Acceptance criteria (AC-036)

| ID | Criterion |
|----|-----------|
| AC-036-01 | Bank cash received, deposit PENDING → wallet `available` unchanged |
| AC-036-02 | Deposit POSTED → 2110 liability recognized before bank settlement completes |
| AC-036-03 | Withdraw accept POSTED + freeze before bank debit confirms |
| AC-036-04 | No code path credits wallet on webhook alone without POSTED (except idempotent replay of POSTED path) |
| AC-036-05 | Fee 4110 on same POSTED journal as movement (matching) |
| AC-036-06 | UI "deposit processing" = PENDING saga, not wallet column |

---

## Test cases (TC-036)

| ID | Expected | Maps to |
|----|----------|---------|
| TC-036-01 | Webhook only → PENDING, wallet 0 change | Deposit PENDING, Balance semantics |
| TC-036-02 | POSTED → 2110 + wallet credit order | Deposit happy, ADR-006 |
| TC-036-03 | Accept withdraw → freeze before bank OK | Withdraw happy, ADR-007 |
| TC-036-04 | POSTED payment → both wallets same TX | Payment happy |
| TC-036-05 | Cash-basis shortcut rejected in design | ACC-E01 |
| TC-036-06 | Fee on POSTED journal not later orphan | TC-009-01 |

---

## References

- [`design-v2/accounting.md`](../design-v2/accounting.md) — §29.2 accrual, §29 VN map
- [`design-v2/acceptance.md`](../design-v2/acceptance.md) — Accrual basis (ADR-036) feature
- [`references/netsuite-cash-vs-accrual.md`](../references/netsuite-cash-vs-accrual.md)
