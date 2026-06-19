# ADR-032: Wallet balance monitoring and threshold alerts

| Field | Value |
|-------|-------|
| Status | Accepted |
| Date | 2026-06-08 (rev. 2026-06-12 — Blnk webhook alternative made explicit) |
| Source | [`references/blnk-balance-monitoring.md`](../references/blnk-balance-monitoring.md), [`design-v2/wallet.md`](../design-v2/wallet.md) §28, [`core.wallet.md`](../core.wallet.md) §6 W5 |
| Related | [ADR-004](ADR-004-wallet-balance-snapshot.md), [ADR-014](ADR-014-reconciliation-w5-report-only.md), [ADR-021](ADR-021-aging-jobs-async-pending.md), [ADR-039](ADR-039-no-synchronous-wallet-aggregate-row.md) |

---

## Context

Blnk emits a `balance.monitor` webhook **synchronously**, fired off the transaction path when a balance crosses a configured condition ([`references/blnk-balance-monitoring.md`](../references/blnk-balance-monitoring.md), [`references/blnk-vs-gtelpay-comparison.md`](../references/blnk-vs-gtelpay-comparison.md) §4). That model couples alert evaluation to the write path.

GtelPay wants the same operational signal (negative balance, stuck frozen, lane spike, W5 drift precursor) **without** putting monitor logic on the wallet hot path that [ADR-004](ADR-004-wallet-balance-snapshot.md) protects, and without a third balance store. This ADR fixes that the monitor is an **out-of-band job/metrics layer**, not an inline webhook — and records the webhook as a deliberate non-choice for v1.

---

## Decision

1. **Balance monitors** are ops-facing jobs/metrics — inspired by Blnk threshold webhooks, implemented as internal metrics + alert router (not a third ledger).
2. Monitors **never mutate** `wallet_balance` or COA ([ADR-014](ADR-014-reconciliation-w5-report-only.md)).
3. v1 monitors (minimum set):

| Monitor | Condition | Severity |
|---------|-----------|----------|
| MON-01 | `available < 0` for any wallet | critical |
| MON-02 | `frozen > 0` and wallet `LOCKED` > 24h | high |
| MON-03 | Single wallet `frozen` sum > configured cap | high |
| MON-04 | `SUM(available+frozen)` lane spike > N% vs 7d baseline | medium |
| MON-05 | W5 drift precursor: POSTED deposit > 15m no matching `wallet_tx` | medium → pairs with ADR-021 |

4. Alert payload includes `wallet_id`, `memberId`, `wallet_type`, `business_ref` candidates from saga store — **no cross-schema JOIN** in alert query path (two queries + correlate in app).
5. Thresholds are **ops config** (env/ConfigMap) — default values documented in runbook when product supplies them.
6. **Evaluation is out-of-band** — monitors read from snapshot/rollup ([ADR-039](ADR-039-no-synchronous-wallet-aggregate-row.md) lane rollup feeds MON-04), never inline on the wallet write transaction. Detection latency is bounded (AC-032-01), not instantaneous.

---

## Consequences

### Positive
- Monitor failure or slowness never blocks payment/transfer/withdraw — the hot path stays clean ([ADR-004](ADR-004-wallet-balance-snapshot.md), AC-032-04).
- No third balance store; monitors are derived views over `wallet_*` + saga store, so no new drift surface ([ADR-014](ADR-014-reconciliation-w5-report-only.md)).
- Thresholds tunable per environment without code change.

### Negative / trade-offs
- Alerts are **near-real-time, not instant** — a condition is caught within the job interval / detection window, not at the committing transaction (acceptable for ops signals; not a control to *prevent* a bad write).
- Lane-spike monitor (MON-04) inherits rollup staleness ([ADR-039](ADR-039-no-synchronous-wallet-aggregate-row.md)).

---

## Alternatives considered

| Alternative | Why rejected (v1) |
|-------------|-------------------|
| **Blnk-style synchronous `balance.monitor` webhook** off the write path | Couples alert evaluation to the wallet hot path ([ADR-004](ADR-004-wallet-balance-snapshot.md)); a slow/failing webhook endpoint would add latency or block commits. Revisit as an *additive* outbox-driven event ([ADR-013](ADR-013-outbox-at-least-once-messaging.md)) if a real-time consumer appears — still not inline. |
| Monitor as a control that **blocks** the offending transaction | A monitor is an ops signal, not an authorization rule; spend control lives in the wallet command (sufficient-funds, LOCKED — [ADR-029](ADR-029-wallet-locked-rejects-mutation.md)), not here. |
| Cross-schema JOIN wallet↔COA for richer alert context | Violates [ADR-003](ADR-003-dual-schema-single-postgres.md); use two queries + correlate in app (point 4). |

---

## Acceptance criteria (AC-032)

| ID | Criterion |
|----|-----------|
| AC-032-01 | MON-01 fires within 5m of negative available detection |
| AC-032-02 | MON-05 correlates without wallet↔COA SQL JOIN |
| AC-032-03 | Alerts are deduplicated per wallet per 1h window |
| AC-032-04 | Monitor job failure does not block payment paths |
| AC-032-05 | No auto-debit/credit from monitor |
| AC-032-06 | Evaluation is out-of-band — no monitor logic runs inside the wallet write transaction |

---

## Test cases (TC-032)

| ID | Expected | Maps to |
|----|----------|---------|
| TC-032-01 | Negative available triggers critical | MON-01 feature |
| TC-032-02 | Long frozen LOCKED wallet alerts | MON-02 |
| TC-032-03 | POSTED lag 20m triggers MON-05 | DEP-E07, X-E06 |
| TC-032-04 | Dedup second alert suppressed | Balance monitoring |
| TC-032-05 | Alert payload has wallet_id only | ADR-003 |
| TC-032-06 | Wallet write TX contains no monitor query/call | AC-032-06, ADR-004 |

---

## References

- [`design-v2/wallet.md`](../design-v2/wallet.md) — §28 gap closed
- [`design-v2/acceptance.md`](../design-v2/acceptance.md) — Balance monitoring feature
- [`references/blnk-vs-gtelpay-comparison.md`](../references/blnk-vs-gtelpay-comparison.md) — §4 monitor (webhook vs job), [`references/blnk-balance-monitoring.md`](../references/blnk-balance-monitoring.md)
