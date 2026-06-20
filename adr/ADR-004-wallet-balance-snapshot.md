# ADR-004: Wallet balance as a maintained snapshot over append-only `wallet_tx`

| Field | Value |
|-------|-------|
| Status | Accepted |
| Date | 2026-06-08 (rev. 2026-06-12 — as-of historical balance query) |
| Deciders | Engineering |
| Related | [`core.wallet.md`](../core.wallet.md) §3.3, §6, §9, [`IMPLEMENTATION.md`](../IMPLEMENTATION.md) §6, [ADR-001](ADR-001-immutable-ledger.md), [ADR-039](ADR-039-no-synchronous-wallet-aggregate-row.md) |

---

## Context

[ADR-001](ADR-001-immutable-ledger.md) makes the **accounting** ledger derive balances from immutable postings (no authoritative balance row). `core.wallet` answers a different question — *how much can this member spend right now?* — on the hot path of every payment, transfer, and withdrawal (TRD NFR target: balance reads at high QPS, P95 < 100ms).

If wallet followed ADR-001 literally, each balance read and each debit's sufficient-funds check would aggregate the member's entire `wallet_tx` history. That is correct but slow and lock-heavy at the rates wallet must serve.

We need a model that keeps a fast, transactionally-correct spendable balance while preserving a complete, immutable audit trail — and that does not contradict ADR-001 (which governs the accounting ledger, a separate bounded context, [ADR-002](ADR-002-core-sharedlib.md)).

---

## Decision

`core.wallet` keeps a **maintained balance snapshot (`wallet_balance`) as the authoritative spendable balance**, updated **in the same transaction** as every append to the immutable `wallet_tx` log.

1. **`wallet_balance` is authoritative for spendable funds** — `available` and `frozen` are read directly for balance queries and sufficient-funds checks; they are not re-derived per read ([`core.wallet.md`](../core.wallet.md) §3.3, FR-2).
2. **`wallet_tx` is the immutable audit trail** — append-only; `amount`/`direction` are never UPDATEd after insert. Each row snapshots `available_after` / `frozen_after`, so the balance is **reconstructible** from history at any point ([`core.wallet.md`](../core.wallet.md) §3.4).
3. **Snapshot and log move together** — invariant W2: every `wallet_balance` change has exactly one new `wallet_tx` in the same DB transaction. There is no path that mutates the balance without a movement row ([`core.wallet.md`](../core.wallet.md) §6).
4. **Corrections are compensating movements** — wrong balances are fixed by a new `wallet_tx` with a new `business_ref`, never by editing a historical row or hand-patching `wallet_balance` (invariant W4, F5).
5. **Concurrency on `wallet_balance`** — each mutation serializes per `wallet_id` via pessimistic row lock (`SELECT … FOR UPDATE`) **or** optimistic `version` check with retry ([`IMPLEMENTATION.md`](../IMPLEMENTATION.md) §6.4; [`design-v2/wallet.md`](../design-v2/wallet.md) §21 — v1 default optimistic).
6. **Cache is non-authoritative** — any Redis read-through cache is invalidated on write; the DB row remains the source of truth ([`core.wallet.md`](../core.wallet.md) §9).
7. **As-of (historical) balance is served from the log, not the live row** — a "balance at time T" query reads the latest `wallet_tx` for the wallet with timestamp ≤ T and returns its `available_after` / `frozen_after`; it never reconstructs from the live `wallet_balance`. This is enabled by point 2 (per-row after-snapshots) and mirrors Blnk's historical-balance retrieval ([`references/blnk-historical-balances.md`](../references/blnk-historical-balances.md), [`references/blnk-vs-gtelpay-comparison.md`](../references/blnk-vs-gtelpay-comparison.md) §4). It is a **read-only** wallet-domain query — no cross-schema JOIN, no COA access.

This is the **inverse default** of ADR-001 (snapshot-authoritative vs derive-authoritative). It is justified because the two contexts have different consistency/latency needs and never share storage ([ADR-003](ADR-003-dual-schema-single-postgres.md)).

---

## Consequences

### Positive

- Balance reads and sufficient-funds checks are single-row lookups — meets the wallet read latency target.
- Full audit trail is preserved (`wallet_tx` append-only with after-snapshots), so reconciliation and support can replay history.
- Snapshot is rebuildable from `wallet_tx`, so a corrupted `wallet_balance` is recoverable, not authoritative-by-faith.
- Clear, simple correctness story under concurrency via `FOR UPDATE`.

### Negative / trade-offs

- The snapshot can in principle drift from the log if a write bypasses the W2 invariant — guarded by code review, tests, and a periodic rebuild/verify job ([`operations.md`](../operations.md)).
- Row locking on `wallet_balance` serializes concurrent mutations of the **same** wallet (acceptable: per-wallet mutation is rarely highly concurrent; cross-wallet is unaffected).
- A second, **cross-domain** reconciliation is still required because the wallet snapshot mirrors but is not the accounting control balance (invariant W5).

### Implementation notes

| Concern | Approach |
|---------|----------|
| Rebuild / verify | Job recomputes `available`/`frozen` from `wallet_tx` and compares to `wallet_balance`; alert on mismatch ([`operations.md`](../operations.md)) |
| Cross-domain drift | Invariant W5 reconciliation vs COA 2110/2120/2130 — log drift, never auto-adjust COA from wallet |
| Hot wallets | If a single wallet becomes a contention point, shard movements or batch; do not drop the W2 invariant |
| As-of balance query | `SELECT available_after, frozen_after FROM wallet_tx WHERE wallet_id=? AND created_at <= ? ORDER BY created_at DESC, id DESC LIMIT 1`; empty result ⇒ zero balance before first movement. Index `(wallet_id, created_at, id)` |

---

## Alternatives considered

| Alternative | Why rejected |
|-------------|--------------|
| Derive balance from `wallet_tx` on every read (ADR-001 style) | Too slow / lock-heavy for wallet read QPS and per-debit checks |
| Balance row only, no movement log | No audit trail, no rebuild, violates wallet immutability goal |
| Event-sourced wallet with async projections | Eventual-consistency on spendable balance risks overspend; v1 needs read-your-write on debits |

---

## Relationship to other ADRs

| ADR | Topic |
|-----|--------|
| [ADR-001](ADR-001-immutable-ledger.md) | Accounting ledger derives balances — **opposite default**, different context |
| [ADR-003](ADR-003-dual-schema-single-postgres.md) | Wallet snapshot lives in schema `wallet`, isolated from `coa_*` |
| [ADR-005](ADR-005-idempotency-key-strategy.md) | The idempotency key that makes the W2 single-transaction write replay-safe |

---

## Acceptance criteria (AC-004)

| ID | Criterion | Owner |
|----|-----------|-------|
| AC-004-01 | `getBalance` reads `wallet_balance.available` / `frozen` directly — not `SUM(wallet_tx)` on hot path | `core.wallet` |
| AC-004-02 | Every `wallet_balance` change inserts exactly one `wallet_tx` in the **same** DB transaction (W2) | `core.wallet` |
| AC-004-03 | No UPDATE on historical `wallet_tx` rows (`amount`, `direction`, `business_ref`) | `core.wallet` |
| AC-004-04 | Each `wallet_tx` records `available_after` and `frozen_after` snapshots | `core.wallet` |
| AC-004-05 | Corrections use new `wallet_tx` + new `business_ref` — never patch `wallet_balance` without a row | `core.wallet` |
| AC-004-06 | `available >= 0` and `frozen >= 0` always (W1) | `core.wallet` |
| AC-004-07 | Optional cache invalidated on write; DB row remains authoritative | Application |
| AC-004-08 | Periodic rebuild job can recompute snapshot from `wallet_tx` and detect drift | Ops job |
| AC-004-09 | As-of balance query returns the `available_after`/`frozen_after` of the latest `wallet_tx` ≤ T (zero if none); read-only, no COA JOIN | `core.wallet` |

---

## Test cases (TC-004)

| ID | Title | Procedure (summary) | Expected | Maps to |
|----|-------|---------------------|----------|---------|
| TC-004-01 | Credit updates snapshot + log | `DEPOSIT_CREDIT` 99000 | `available` += 99000; one `wallet_tx`; matching `available_after` | `acceptance.md` Deposit happy |
| TC-004-02 | Debit insufficient | Debit > `available` | `WALLET_INSUFFICIENT_BALANCE`; no row; balance unchanged | `acceptance.md` Payment insufficient |
| TC-004-03 | Freeze constant total | `WITHDRAW_FREEZE` 101000 | `available`↓ `frozen`↑; sum unchanged | `acceptance.md` Balance semantics freeze |
| TC-004-04 | Settle debits frozen only | After freeze, `WITHDRAW_SETTLE` | `frozen`↓; `available` unchanged | `acceptance.md` Balance semantics settle |
| TC-004-05 | No orphan balance update | Code path review / integration | No `wallet_balance` UPDATE without `wallet_tx` insert | AC-004-02 |
| TC-004-06 | Concurrent debits last cent | Two parallel debits on `available=1` | One succeeds; one `WALLET_INSUFFICIENT_BALANCE` | `acceptance.md` X-E11 |
| TC-004-07 | Rebuild verify | Recompute from `wallet_tx` for test wallet | Matches `wallet_balance` | AC-004-08 |
| TC-004-08 | Compensating movement | Wrong credit → `ADJUSTMENT_DEBIT` new ref | New row; history intact | `acceptance.md` ADJ-E02 |
| TC-004-09 | As-of balance | After 3 movements, query balance at T between mvt 2 and 3 | Returns mvt-2 `available_after`/`frozen_after`; T before mvt 1 ⇒ 0 | AC-004-09 |

---

## References

- [`core.wallet.md`](../core.wallet.md) — §3.3 `wallet_balance`, §3.4 `wallet_tx`, §6 invariants W1–W5, §9 concurrency
- [`IMPLEMENTATION.md`](../IMPLEMENTATION.md) — §6.4 command algorithm
- [`design-v2/wallet.md`](../design-v2/wallet.md) — §8 invariants, §21 concurrency, §26 cross-cutting
- [`design-v2/acceptance.md`](../design-v2/acceptance.md) — Balance semantics, Withdraw, X-E11
- [ADR-001](ADR-001-immutable-ledger.md) — immutable ledger (accounting; opposite default)
- [`references/blnk-historical-balances.md`](../references/blnk-historical-balances.md), [`references/blnk-vs-gtelpay-comparison.md`](../references/blnk-vs-gtelpay-comparison.md) — §4 as-of balance pattern
