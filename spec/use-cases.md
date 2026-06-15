# Use-case definitions (orchestration)

Implementable contract for each business use case the orchestrator runs: trigger, input,
validation, ordered gateway calls, wallet legs, transit, success output, failure/compensation,
idempotency. Business/design narrative → [`processes.md`](./processes.md); exact DR/CR lines →
[`foundation.md`](./foundation.md) §8–16; tx types → [`design-v2/wallet.md`](../design-v2/wallet.md).

Conventions: `ref` = `businessRef` (= S1 `X-Idempotency-Key`). Gateways: `W` = WalletGateway,
`L` = LedgerGateway. "POSTED" = ledger journal posted (transit nets to 0). All legs idempotent on
their key; replay returns the prior result.

---

## UC-1 Deposit (async)

| | |
|---|---|
| Trigger | bank webhook → `notifyDeposit`; client polls `getDepositStatus` (202) |
| Input | memberId, amount (gross), fee, currency, ref (bank ref) |
| Validate | ref present; amount > 0; currency=VND |
| Steps | 1) `L.createJournal(ref, DEPOSIT)` PENDING (3100) → 2) on funds confirmed `L.confirmDeposit(coaTransId, fee)` → POSTED → 3) `W.credit(USER, net, ref, coaTransId, DEPOSIT_CREDIT)` |
| Wallet | `DEPOSIT_CREDIT` only **after** POSTED |
| Failure | POSTED but wallet credit fails → forward-retry credit (idempotent); never reverse ledger |
| Idempotency | `ref` on journal + wallet_tx |

## UC-2 Withdraw (wallet sync + bank async)

| | |
|---|---|
| Trigger | `createWithdrawal` (200 on accept) |
| Input | memberId, amount, fee, currency, bank dest, ref |
| Validate | ref/idem; amount>0; sufficient available for gross |
| Steps | 1) `W.freeze(USER, gross, ref, WITHDRAW_FREEZE)` → 2) `L` POSTED accept (3200) → 3) enqueue bank payout (S6). On bank OK: `W.debit(USER, gross, {ref}:settle, WITHDRAW_SETTLE)`. On fail/cancel: `W.unfreeze(USER, gross, {ref}:release, WITHDRAW_RELEASE)` |
| Wallet | freeze → settle **or** release (never debit twice) |
| Failure | bank timeout → poll (ADR-033), not auto-release; aging job (ADR-021) |
| Idempotency | `ref` freeze; `{ref}:settle` / `{ref}:release` follow-ups |

## UC-3 Payment (sync 3-commit) — implemented

| | |
|---|---|
| Trigger | `POST /v1/payments` (JSON / x-www-form-urlencoded), 200 |
| Input | ref, memberId (USER), merchantId (MERCHANT), amount (gross), currency, netToMerchant |
| Validate | ref non-empty; idem-key==ref; member≠merchant, both>0; currency 3-letter; v1 net==gross |
| Steps | 1) `W.provision` USER+MERCHANT → 2) `W.debit(USER, gross, ref, PAYMENT_DEBIT)` → 3) `L.createJournal(ref,PAYMENT)` + lines (2110 DR, 3500 CR, 3500 DR, 2120 CR) + `post` (3500→0) → 4) `W.credit(MERCHANT, net, ref, coaTransId, PAYMENT_CREDIT)` |
| Success | `{ ref, walletTxId(debit), coaTransId, status:SUCCESS }` |
| Failure | post fails after debit → compensate `W.credit(USER, gross, {ref}:comp, ADJUSTMENT_CREDIT)`; credit fails after POSTED → forward-retry, ledger stands (ADR-008) |
| Idempotency | one `ref`, distinct tx_type per leg |

## UC-4 Internal transfer A→B (sync 3-commit)

| | |
|---|---|
| Trigger | `createTransfer` (internal dest), 200 |
| Input | ref, fromMemberId, toMemberId, amount, fee, currency |
| Validate | as payment; from≠to; sufficient available for gross |
| Steps | 1) `W.debit(A USER, gross, ref, TRANSFER_DEBIT)` → 2) `L` POSTED (3300→0) → 3) `W.credit(B USER, net, ref, coaTransId, TRANSFER_CREDIT)` |
| Failure | same as payment (compensate A after post-fail; forward-retry credit B) |
| Idempotency | `ref`, distinct tx_type per leg |

## UC-5 IBFT (interbank — wallet sync + bank async)

| | |
|---|---|
| Trigger | `createTransfer` (external dest) |
| Steps | 1) `W.freeze(USER, gross, ref)` → 2) `L` POSTED accept (3400) → 3) Napas payout (S6); OK → settle (debit frozen), fail → release |
| Failure | poll bank, aging (ADR-021/033); never double-debit |
| Idempotency | `ref` + `{ref}:settle` / `{ref}:release` |

## UC-6 QR/POS (acquirer + EOD)

| | |
|---|---|
| Trigger | acquirer settlement file |
| Wallet | none v1 (optional `MERCHANT_SETTLE_CREDIT` at EOD) |
| Ledger | acquirer 3500 → EOD 3800 (see UC-9) |

## UC-7 Payroll (batch)

| | |
|---|---|
| Trigger | merchant batch submit |
| Steps | validate MERCHANT available ≥ gross → `W.debit(MERCHANT, gross, ref, PAYROLL_DEBIT)` **once** → `L` POSTED (3600→0) → per-recipient payout (S6) |
| Partial | one recipient payout fails → does NOT roll back; summary `{succeeded[],failed[],retrying[]}` (ADR-017). v1 tx_type deferred (impl §2.1) |
| Idempotency | batch `ref`; payout `{ref}:{recipientId}` |

## UC-8 Disbursement (batch)

| | |
|---|---|
| Trigger | partner batch |
| Steps | optional prefund (`PARTNER_PREFUND_CREDIT`, 2130) → validate PARTNER available → `W.debit(PARTNER, gross, {ref}:{recipientId}, DISBURSEMENT_DEBIT)` per recipient → `L` POSTED (3700→0) → bank out |
| Partial | per-recipient, no rollback (ADR-017) |
| Idempotency | `{ref}:{recipientId}` |

## UC-9 EOD settlement (batch job)

| | |
|---|---|
| Trigger | scheduled |
| Steps | reconcile acquirer file vs 2120 snapshot (mismatch → stop, no lock) → per merchant lock 2120→3800, MDR 3820, settle 3810 → bank out (fail keeps 3810 for retry) → optional `MERCHANT_SETTLE_CREDIT` |
| Idempotency | `(merchantId, settlementDate)` |

---

## Cross-cutting (all)

- **Idempotency:** `ref` end-to-end; replay → prior result, no second effect (ADR-005).
- **Saga:** forward-retry preferred; compensate only when safe; POSTED ledger never reversed by wallet (ADR-008/026).
- **Liveness:** every async pending state has an aging job to a terminal state (ADR-021).
- **Invariants:** transit nets to 0 per completed use case; `available,frozen ≥ 0`; `sum(DR)=sum(CR)`.
