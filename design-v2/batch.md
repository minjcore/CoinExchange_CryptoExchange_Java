# Batch processing — payroll, disbursement, EOD settlement

**Status:** Design · **Scope:** `10_core/` · **Owner of execution:** orchestration (BFF), not a domain.

Consolidates the batch design that is otherwise split across
[`orchestration.md` §17](./orchestration.md), [`wallet.md` §20](./wallet.md),
[`accounting/vol-06-qr-payroll-disburse.md`](./accounting/vol-06-qr-payroll-disburse.md).
**ADR:** [ADR-017](../adr/ADR-017-partial-batch-payroll-disbursement.md) (partial success),
[ADR-021](../adr/ADR-021-aging-jobs-async-pending.md) (aging),
[ADR-015](../adr/ADR-015-eod-settlement-independent-batch.md) (EOD),
[ADR-005](../adr/ADR-005-idempotency-key-strategy.md) (keys),
[ADR-008](../adr/ADR-008-saga-compensation-no-2pc.md) (no 2PC).

---

## 1. Core decision — batch lives in orchestration, not in a domain

A batch is a **fan-out of independent single legs**. `core.wallet` and `core.accounting`
never see "a batch": each sees one ordinary leg. All batch-ness — fan-out, partial-success
summary, per-slice transit-zero, retry — is orchestration state. This keeps the domains on
their existing single-leg, single-transaction contracts ([`wallet.md` §21](./wallet.md),
[ADR-008](../adr/ADR-008-saga-compensation-no-2pc.md)).

Consequence: **no new wallet service, no new lock.** Batch reuses `credit()` / `debit()` and
the idempotency-triple + under-lock recheck already in `WalletCommandServiceImpl`
([`spec/trd/wallet.md` §9](../spec/trd/wallet.md)).

---

## 2. Two shapes — do not conflate

| | **Payroll** (MERCHANT) | **Disbursement** (PARTNER) |
|---|---|---|
| Wallet debit | **One** gross debit for the whole batch (`PAYROLL_DEBIT`) | **Per recipient** (`DISBURSEMENT_DEBIT`) |
| Idempotency key | batch `businessRef` | `{businessRef}:{recipientId}` |
| Transit | 3600 | 3700 |
| Insufficient lane balance | the single gross debit fails → whole batch rejected at submit | the individual leg is rejected |

The payroll merchant wallet is debited **exactly once** (gross = salaries + fee); the N
employee payouts then flow from transit. The partner wallet is debited **per beneficiary**.

---

## 3. Reconciling "reject whole batch" with "one failure ≠ rollback"

These look contradictory ([AC-017-04] vs [ADR-017] rule 3) but operate at different layers:

- **Submit-time, wallet layer:** payroll debits gross once. Insufficient `available` → reject
  the batch before any payout is enqueued. Disbursement validates `available` per leg.
- **Payout-time, bank layer:** a recipient whose *bank payout* fails is resolved by
  release/retry on **that recipient's** sub-key. It never re-touches the wallet — the money
  left the wallet exactly once at submit. Succeeded recipients are never rolled back.

So "reject whole batch" is a wallet/submit guard; "no rollback" is a payout/bank property.
They never fire on the same leg.

---

## 4. Idempotency (ADR-005)

- Batch `businessRef` = client batch id.
- Per-recipient legs: `{businessRef}:{recipientId}` — distinct rows under
  `UNIQUE (wallet_id, business_ref, tx_type)`.
- Replay of any leg (transport redelivery, client retry, aging re-enqueue) → idempotent
  no-op returning the prior `walletTxId`. Already guaranteed by the wallet recheck path;
  batch adds **no** new idempotency mechanism.

---

## 5. EOD settlement (ADR-015) — independent batch, not request-driven

Step order: [`orchestration.md` §17.3](./orchestration.md). Reconcile acquirer file vs the
2120 snapshot first; mismatch → **stop, no locking**. Then per merchant lock 2120→3800, MDR
3820, settle 3810; bank-out failure keeps 3810 for retry. Optional `MERCHANT_SETTLE_CREDIT`
per product flag. Idempotency: `(merchantId, settlementDate)` on the settling steps.

---

## 6. Liveness — aging jobs (ADR-021)

No batch slice stays non-terminal forever: frozen-without-payout re-enqueues, payout-sent-
no-callback polls the bank, over-SLA pendings alert ops. Metrics per
[ADR-021](../adr/ADR-021-aging-jobs-async-pending.md) AC.

---

## 7. Wallet impact (deferred — not v1)

Batch needs four tx types **not** in the v1 locked enum
([`spec/implementation.md` §2.1](../spec/implementation.md) — "implement exactly"):

| tx_type | Lane | Direction |
|---------|------|-----------|
| `PAYROLL_DEBIT` | MERCHANT | DEBIT (debit available) |
| `DISBURSEMENT_DEBIT` | PARTNER | DEBIT (debit available) |
| `PARTNER_PREFUND_CREDIT` | PARTNER | CREDIT |
| `MERCHANT_SETTLE_CREDIT` | MERCHANT | CREDIT |

Adding them = a deliberate change to the §2.1 lock, classified under the existing
credit/debit sets in `WalletTxType`. **No other wallet change.** Until §2.1 is reopened,
batch stays design-only.

---

## 8. Build order (when scheduled)

1. Reopen §2.1 → add the 4 tx types + classification + tests (smallest, wallet-local).
2. Accounting payroll/disbursement/EOD templates (3600 / 3700 / 3800–3820 transit-zero).
3. Orchestration fan-out + partial-success summary `{ succeeded[], failed[], retrying[] }`.
4. Aging jobs + metrics (ADR-021).
