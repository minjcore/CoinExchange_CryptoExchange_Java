# ADR-004: Wallet balance as a maintained snapshot over append-only `wallet_tx`

| Field | Value |
|-------|-------|
| Status | Accepted |
| Date | 2026-06-08 |
| Deciders | Engineering |
| Related | [`core.wallet.md`](../core.wallet.md) §3.3, §6, §9, [`IMPLEMENTATION.md`](../IMPLEMENTATION.md) §6, [ADR-001](ADR-001-immutable-ledger.md) |

---

## Context

[ADR-001](ADR-001-immutable-ledger.md) makes the **accounting** ledger derive balances from immutable postings (no authoritative balance row). `core.wallet` answers a different question — *how much can this member spend right now?* — on the hot path of every payment, transfer, and withdrawal (TRD NFR target: balance reads at high QPS, P95 < 100ms).

If wallet followed ADR-001 literally, each balance read and each debit's sufficient-funds check would aggregate the member's entire `wallet_tx` history. That is correct but slow and lock-heavy at the rates wallet must serve.

We need a model that keeps a fast, transactionally-correct spendable balance while preserving a complete, immutable audit trail — and that does not contradict ADR-001 (which governs the accounting ledger, a separate bounded context, [ADR-002](ADR-002-core-foundation-shared-library.md)).

---

## Decision

`core.wallet` keeps a **maintained balance snapshot (`wallet_balance`) as the authoritative spendable balance**, updated **in the same transaction** as every append to the immutable `wallet_tx` log.

1. **`wallet_balance` is authoritative for spendable funds** — `available` and `frozen` are read directly for balance queries and sufficient-funds checks; they are not re-derived per read ([`core.wallet.md`](../core.wallet.md) §3.3, FR-2).
2. **`wallet_tx` is the immutable audit trail** — append-only; `amount`/`direction` are never UPDATEd after insert. Each row snapshots `available_after` / `frozen_after`, so the balance is **reconstructible** from history at any point ([`core.wallet.md`](../core.wallet.md) §3.4).
3. **Snapshot and log move together** — invariant W2: every `wallet_balance` change has exactly one new `wallet_tx` in the same DB transaction. There is no path that mutates the balance without a movement row ([`core.wallet.md`](../core.wallet.md) §6).
4. **Corrections are compensating movements** — wrong balances are fixed by a new `wallet_tx` with a new `business_ref`, never by editing a historical row or hand-patching `wallet_balance` (invariant W4, F5).
5. **Concurrency by row lock** — `SELECT … FOR UPDATE` on the `wallet_balance` row per mutation (with `version` available for optional optimistic retry) keeps the snapshot consistent under concurrent debits ([`IMPLEMENTATION.md`](../IMPLEMENTATION.md) §6.4).
6. **Cache is non-authoritative** — any Redis read-through cache is invalidated on write; the DB row remains the source of truth ([`core.wallet.md`](../core.wallet.md) §9).

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

## References

- [`core.wallet.md`](../core.wallet.md) — §3.3 `wallet_balance`, §3.4 `wallet_tx`, §6 invariants W1–W5, §9 concurrency- [`IMPLEMENTATION.md`](../IMPLEMENTATION.md) — §6.4 command algorithm (`FOR UPDATE`)
- [ADR-001](ADR-001-immutable-ledger.md) — immutable ledger (accounting)
