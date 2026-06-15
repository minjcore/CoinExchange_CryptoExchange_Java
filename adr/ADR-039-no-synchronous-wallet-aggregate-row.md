# ADR-039: No synchronous wallet-side aggregate row; aggregate = COA control + async rollup

| Field | Value |
|-------|-------|
| Status | Accepted |
| Date | 2026-06-12 |
| Deciders | Engineering |
| Source | Design review 2026-06-12 (proposal: write an aggregate "ví tổng" row on every member balance write) |
| Related | [ADR-003](ADR-003-dual-schema-single-postgres.md), [ADR-004](ADR-004-wallet-balance-snapshot.md), [ADR-014](ADR-014-reconciliation-w5-report-only.md), [ADR-020](ADR-020-wallet-lanes-coa-control-mapping.md), [ADR-026](ADR-026-wallet-never-reverses-accounting.md), [ADR-032](ADR-032-wallet-balance-monitoring.md) |

---

## Context

A proposal was raised: **every time a member's `wallet_balance` is mutated, also append/update one record holding the aggregate ("total") wallet balance** — so the sum of all member balances is maintained in one place inside the wallet domain.

The intent (a fast, in-domain total without a cross-schema JOIN) is reasonable, but the aggregate it describes **already exists** and the synchronous-row implementation conflicts with two locked decisions:

- **The accounting aggregate already is the total.** Each member credit/debit posts a journal line to a COA control account — **2110** (USER) / **2120** (MERCHANT) / **2130** (PARTNER) ([ADR-020](ADR-020-wallet-lanes-coa-control-mapping.md), [`spec/foundation.md`](../spec/foundation.md) §6.2). That control balance *is* the aggregate wallet liability. A wallet-side total would be a duplicate of it living in the wrong bounded context ([ADR-003](ADR-003-dual-schema-single-postgres.md), [ADR-026](ADR-026-wallet-never-reverses-accounting.md)).
- **A single shared row is a write hotspot.** [ADR-004](ADR-004-wallet-balance-snapshot.md) deliberately keeps mutation concurrency *per-wallet* (one row lock per `wallet_id`; different members never contend). Updating one shared aggregate row inside the same DB transaction would serialize **every** wallet mutation system-wide through that row — defeating the per-wallet concurrency model and the wallet read/write latency target.

This ADR locks the decision so the proposal is not re-implemented.

---

## Decision

1. **No synchronous wallet-side aggregate row.** A wallet mutation MUST NOT update or append a shared "total wallet balance" record in the same transaction as the per-member `wallet_balance` / `wallet_tx` write. Per-wallet concurrency from [ADR-004](ADR-004-wallet-balance-snapshot.md) is preserved.
2. **The authoritative aggregate is the COA control account** — 2110 / 2120 / 2130 in `core.accounting`, written via orchestration journals ([ADR-020](ADR-020-wallet-lanes-coa-control-mapping.md)). There is no second authoritative total.
3. **In-domain aggregates are derived asynchronously** when needed (monitoring, dashboards, MON-04 lane spike — [ADR-032](ADR-032-wallet-balance-monitoring.md)). Allowed mechanisms:

   | Mechanism | Use | Note |
   |-----------|-----|------|
   | **Rollup job** → `wallet_lane_rollup` (SUM per lane every N s/min) | default for monitor/dashboard | non-authoritative, staleness ≤ interval |
   | **Sharded/bucketed counter** (total = SUM of K buckets) | only if near-real-time aggregate is required at high QPS | reconcile to control; added complexity |

4. **Reconciliation is unchanged and report-only.** `SUM(wallet_balance)` per lane vs COA control stays the **W5** drift check ([ADR-014](ADR-014-reconciliation-w5-report-only.md)); rollup/bucket figures are derived views, never reconciled as a third source of truth, never auto-adjusted.
5. **Boundary intact.** The rollup query reads only `wallet_*`; it does not JOIN `coa_*`. Comparison against control happens in W5/monitor app code via two queries ([ADR-003](ADR-003-dual-schema-single-postgres.md), [ADR-032](ADR-032-wallet-balance-monitoring.md)).

---

## Consequences

### Positive
- Wallet write path keeps per-`wallet_id` concurrency — no system-wide serialization point, latency target ([ADR-004](ADR-004-wallet-balance-snapshot.md)) preserved.
- One authoritative aggregate (COA control), so no new drift surface and no extra reconciliation triangle.
- Monitoring still gets a fast in-domain total via rollup, without a cross-schema JOIN.

### Negative / trade-offs
- The in-domain aggregate (rollup) is **eventually consistent** — not exact at the instant of read. Acceptable for monitoring; anything needing the exact total reads the COA control or recomputes.
- A rollup table/job is extra operational surface (scheduling, staleness alerting).

---

## Alternatives considered

| Alternative | Why rejected |
|-------------|--------------|
| **Synchronous shared aggregate row** (the proposal) | Write hotspot — serializes every wallet mutation through one row lock; and duplicates the COA control balance in the wrong context (drift + boundary break). |
| Append-only aggregate delta log inside wallet (insert, no UPDATE) | Avoids the lock, but is functionally a re-implementation of the COA control account 2110/2120/2130 — second source of truth to reconcile. |
| Compute total on demand via `SUM(wallet_balance)` each time | Fine for occasional/admin reads; too heavy as a hot-path or high-frequency monitor query — that is what the rollup serves. |

---

## Relationship to other ADRs

| ADR | Topic |
|-----|-------|
| [ADR-004](ADR-004-wallet-balance-snapshot.md) | Per-wallet snapshot concurrency this ADR protects |
| [ADR-020](ADR-020-wallet-lanes-coa-control-mapping.md) | USER/MERCHANT/PARTNER → 2110/2120/2130 — the authoritative aggregate |
| [ADR-014](ADR-014-reconciliation-w5-report-only.md) | W5 drift check (sum vs control) — unchanged |
| [ADR-026](ADR-026-wallet-never-reverses-accounting.md) | Wallet does not own/duplicate accounting truth |
| [ADR-032](ADR-032-wallet-balance-monitoring.md) | Consumer of the async rollup (MON-04) |

---

## Acceptance criteria (AC-039)

| ID | Criterion |
|----|-----------|
| AC-039-01 | No code path updates a shared aggregate row in the same TX as a `wallet_balance` / `wallet_tx` write |
| AC-039-02 | Lane aggregate exposed to monitors comes from an async rollup (or sharded counter), not a synchronous per-write row |
| AC-039-03 | Rollup/bucket figures are non-authoritative — never used to adjust `wallet_balance` or COA |
| AC-039-04 | Authoritative aggregate liability remains COA 2110/2120/2130 |
| AC-039-05 | Rollup query touches only `wallet_*` — no cross-schema JOIN to `coa_*` |

---

## Test cases (TC-039)

| ID | Expected | Maps to |
|----|----------|---------|
| TC-039-01 | Two members mutated concurrently — no shared-row lock contention between them | AC-039-01, ADR-004 |
| TC-039-02 | MON-04 lane spike reads rollup; rollup lags ≤ interval, not exact | AC-039-02, ADR-032 |
| TC-039-03 | Rollup drift vs COA control surfaces in W5 report, no auto-adjust | AC-039-03, ADR-014 |
| TC-039-04 | Aggregate query plan shows no `coa_*` access | AC-039-05, ADR-003 |

---

## References

- [`spec/foundation.md`](../spec/foundation.md) — §5 invariants, §6.2 control accounts
- [`design-v2/wallet.md`](../design-v2/wallet.md) — §2.1 balance semantics, aggregate lives in accounting
- [`references/blnk-vs-gtelpay-comparison.md`](../references/blnk-vs-gtelpay-comparison.md) — §1, §5 (single aggregate; two-domain split)
- [ADR-004](ADR-004-wallet-balance-snapshot.md), [ADR-020](ADR-020-wallet-lanes-coa-control-mapping.md), [ADR-014](ADR-014-reconciliation-w5-report-only.md)
