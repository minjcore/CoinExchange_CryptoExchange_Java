# ADR-017: Partial success in payroll and disbursement batches

| Field | Value |
|-------|-------|
| Status | Accepted |
| Date | 2026-06-08 (rev. 2026-06-12 — Blnk partial-commit distinction) |
| Source | [`core.business-processes.md`](../core.business-processes.md) §13.6, [`IMPLEMENTATION.md`](../IMPLEMENTATION.md) D1 per-recipient keys |
| Related | [ADR-005](ADR-005-idempotency-key-strategy.md), [ADR-007](ADR-007-freeze-settle-async-outflow.md), [ADR-010](ADR-010-transit-accounts-net-zero.md) |

---

## Context

A payroll or disbursement batch fans one merchant/partner debit out to N recipients. Recipients fail independently (bank reject, missing beneficiary, timeout). The question is the **failure unit**: roll back the whole batch on any failure, or let succeeded legs stand.

Blnk models a related but **distinct** primitive — *partial commit of a single inflight transaction* ([`references/blnk-vs-gtelpay-comparison.md`](../references/blnk-vs-gtelpay-comparison.md) §3d, [`references/blnk-transaction-lifecycle.md`](../references/blnk-transaction-lifecycle.md)): one INFLIGHT amount is committed in slices over time, the remainder stays held. That is *intra-leg* partial settlement. ADR-017 is *inter-leg*: each recipient is its own independent leg, fully succeeded or fully failed. The two must not be conflated — see Alternatives.

---

## Decision

1. Batch `businessRef` = client batch id.
2. Per recipient: `{businessRef}:{recipientId}` for wallet and payout idempotency.
3. **One failed recipient does not roll back** succeeded legs.
4. Response: `{ succeeded[], failed[], retrying[] }` summary.
5. Completed batch **slice** must clear transit **3600** or **3700** — no stranded non-zero transit for finished slice.
6. `PAYROLL_DEBIT` / `DISBURSEMENT_DEBIT` gross from orchestration ([ADR-009](ADR-009-fee-ownership-orchestration.md)).
7. **Failure unit is the recipient leg, not a fraction of a leg** — a recipient is `succeeded` or `failed` as a whole. Blnk-style *partial commit of one inflight amount* is **out of scope v1**: there is no "settle 40% of recipient X now, 60% later". A recipient whose payout is uncertain follows [ADR-007](ADR-007-freeze-settle-async-outflow.md) freeze→settle/release in full (poll, no timeout-release).

---

## Consequences

### Positive
- A single bank/beneficiary failure does not strand the whole payroll run; succeeded employees keep their funds.
- Per-recipient sub-key makes retry of only the failed legs safe and idempotent ([ADR-005](ADR-005-idempotency-key-strategy.md)).

### Negative / trade-offs
- Caller must handle a tri-state summary, not a single success/fail — heavier client contract.
- A batch can sit in a mixed state (some POSTED, some retrying) until all legs reach terminal; ops must monitor stuck legs (pairs with [ADR-021](ADR-021-aging-jobs-async-pending.md), [ADR-033](ADR-033-bank-poll-t2-frozen-tmax.md)).

---

## Alternatives considered

| Alternative | Why rejected |
|-------------|--------------|
| All-or-nothing batch rollback | One bad beneficiary reverses hundreds of good payouts; bank legs may already be irreversible — cannot truly roll back. |
| **Blnk partial-commit of one inflight** (settle a fraction of a leg, hold the rest) | Adds intra-leg state (`committed_amount` < `inflight_amount`) with no v1 use case; VND fiat payout is whole-or-nothing per recipient. Revisit only if a product needs staged release of a *single* obligation. |
| Aggregate transit per batch, no per-recipient key | Loses idempotent retry granularity; a replay re-pays succeeded recipients. |

---

## Acceptance criteria (AC-017)

| ID | Criterion |
|----|-----------|
| AC-017-01 | Partial failure returns summary not full rollback |
| AC-017-02 | Succeeded recipient wallet_tx retained |
| AC-017-03 | Per-recipient sub-key idempotent |
| AC-017-04 | Insufficient lane balance rejects whole batch submit |
| AC-017-05 | Transit zero for completed slice |
| AC-017-06 | A recipient leg is whole succeeded/failed — no fractional commit of a single leg (v1) |

---

## Test cases (TC-017)

| ID | Expected | Maps to |
|----|----------|---------|
| TC-017-01 | Payroll POSTED + PAYROLL_DEBIT | Payroll happy |
| TC-017-02 | Insufficient merchant reject | Payroll insufficient |
| TC-017-03 | Partial PR-E scenarios | PR-E extended |
| TC-017-04 | Disburse prefund + partial | DIS-E, Disbursement feature |
| TC-017-05 | Partner insufficient reject | Disburse exceeds available |

---

## References

- [`design-v2/wallet.md`](../design-v2/wallet.md) — §20
- [`design-v2/orchestration.md`](../design-v2/orchestration.md) — §17
- [`references/blnk-vs-gtelpay-comparison.md`](../references/blnk-vs-gtelpay-comparison.md) — §3d, §4 partial-commit distinction
