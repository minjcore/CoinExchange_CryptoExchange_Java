# Orchestration (Application / BFF)

**Not a domain.** Sequences accounting + wallet, computes fees, enforces auth, exposes public HTTP, publishes commands, consumes events.

Domains: [`accounting.md`](./accounting.md) · [`wallet.md`](./wallet.md)

**ADR:** [ADR-002](../adr/ADR-002-core-foundation-shared-library.md) · [ADR-003](../adr/ADR-003-dual-schema-single-postgres.md) · [ADR-005](../adr/ADR-005-idempotency-key-strategy.md) · [ADR-006](../adr/ADR-006-two-phase-deposit.md) (§11) · [ADR-007](../adr/ADR-007-freeze-settle-async-outflow.md) (§14–15, §23) · [ADR-008](../adr/ADR-008-saga-compensation-no-2pc.md) · [ADR-009](../adr/ADR-009-fee-ownership-orchestration.md) (§5, §20). Index: [`adr/README.md`](../adr/README.md).

---

## 1. Role

```
paymentorches ──► app-orchestration pod
                    ├── WalletGateway ──HTTP──► core.wallet pod
                    ├── LedgerGateway ──S2 HTTP──► core.accounting pod
                    ├── RabbitMQ commands (S6)
                    └── Kafka events (S3)
```

Orchestration **must not** INSERT/UPDATE `wallet_*` or `coa_*` directly ([`integration-surfaces.md`](../integration-surfaces.md) §9).

### 1.1 Why this layer is mandatory (not optional glue)

The split is the reason orchestration exists. `core.wallet` is the **hot path** (fast, read-your-write, single-row, high RPS); `core.accounting` is **back-office / system-of-record** (strong consistency, immutable, period close). Their consistency / latency / availability profiles are **opposite** ([ADR-004](../adr/ADR-004-wallet-balance-snapshot.md) vs [ADR-001](../adr/ADR-001-immutable-ledger.md); `trd/accounting.md` §4 positioning). Because they never share storage and never call each other ([`design/platform/boundaries.md`](../design/platform/boundaries.md), [ADR-003](../adr/ADR-003-dual-schema-single-postgres.md), [ADR-026](../adr/ADR-026-wallet-never-reverses-accounting.md)), **something must sequence a use case across both without coupling them** — that is orchestration, as a **separate service** ([ADR-038](../adr/ADR-038-orchestrator-separate-service-gateway-seam.md)) using **saga + compensation, not 2PC** ([ADR-008](../adr/ADR-008-saga-compensation-no-2pc.md)). Remove it and the only alternatives are a cross-domain transaction (breaks the boundary) or a domain calling another (forbidden, [ADR-012](../adr/ADR-012-orchestration-integration-forbidden-rules.md)).

### 1.2 Wallet addressing — resolve pocket → `wallet_id` (ADR-040)

A USER may hold multiple pockets ([ADR-040](../adr/ADR-040-user-multi-pocket-wallets.md)). **Orchestration resolves `memberId` + pocket selector → a concrete `wallet_id` before any wallet leg.** The wallet domain never guesses a pocket. Every step below that says "Wallet … USER / A / B" means a **resolved `wallet_id`**, not a member. Resolution rules:

| Input from S1 | Resolution |
|---------------|------------|
| explicit `walletId` | use as-is (validate owner = principal `memberId`) |
| `memberId` + `pocketCode` / `label` | look up the member's wallet for that pocket |
| `memberId` only | the member's `'default'` pocket |
| MERCHANT / PARTNER leg | the single wallet for `(memberId, type, currency)` (no pocket choice) |

Idempotency triple `(wallet_id, business_ref, tx_type)` is therefore naturally pocket-scoped ([ADR-005](../adr/ADR-005-idempotency-key-strategy.md)).

---

## 2. Surface map (app-orchestration only)

| ID | Protocol | Spec | Orch role |
|----|----------|------|-----------|
| S1 | HTTPS inbound | `spec/contracts/open-api/gtelpay-core-internal.yaml` | Implement — paymentorches → orch |
| *(public)* | HTTPS | `spec/contracts/open-api/gtelpay-public.yaml` | **paymentorches** |
| — | HTTPS outbound | `spec/contracts/open-api/wallet-internal.yaml` | `WalletGateway` → wallet pod |
| S2 | HTTPS outbound | `spec/contracts/open-api/accounting-internal.yaml` | `LedgerGateway` → accounting pod |
| S3 | Kafka | `spec/contracts/async-api/core-events.yaml` | Publish / consume |
| S6 | RabbitMQ | `spec/contracts/async-api/core-commands.yaml` | Publish commands |
| S4 | Gateway config (ref) | `spec/contracts/gateway/routes.example.yaml` | Platform — không orch layer |
| S5 | Library envelope | `foundation.md` §4 | Wire shape S1/S2/S6 |

Chi tiết: [`integration-surfaces.md`](../spec/integration-surfaces.md) §1.1.

---

## 3. Step order by use case

### 3.1 Deposit (async)

| # | Action |
|---|--------|
| 1 | S1 webhook/notify → validate → **202** + `businessRef` |
| 2 | S6 `BANK_DEPOSIT` → accounting worker → journal **PENDING** (3100) |
| 3 | Map VA → `memberId`; phase B → **POSTED**, 3100 = 0 |
| 4 | S3 `JournalPosted` and/or `WalletCreditCommand` / S6 `WALLET_CREDIT` |
| 5 | Wallet `DEPOSIT_CREDIT` USER net amount |

### 3.2 Payment (sync)

| # | Action |
|---|--------|
| 1 | Wallet `PAYMENT_DEBIT` USER |
| 2 | Accounting POSTED (3500 = 0) |
| 3 | Wallet `PAYMENT_CREDIT` MERCHANT |
| 4 | S1 **200** + ids |

### 3.3 Transfer (sync)

| # | Action |
|---|--------|
| 1 | Wallet `TRANSFER_DEBIT` A (gross) |
| 2 | Accounting POSTED (3300 = 0) |
| 3 | Wallet `TRANSFER_CREDIT` B (net) |
| 4 | S1 **200** |

### 3.4 Withdraw (sync accept + async bank)

| # | Action |
|---|--------|
| 1 | Wallet `WITHDRAW_FREEZE` gross |
| 2 | Accounting POSTED (3200 = 0) when policy requires |
| 3 | S1 **200** accept |
| 4 | S6 `WITHDRAW_PAYOUT` → bank |
| 5a | Bank OK → `WITHDRAW_SETTLE` |
| 5b | Bank fail → `WITHDRAW_RELEASE` |

### 3.5 IBFT (sync accept + async Napas/bank)

| # | Action |
|---|--------|
| 1 | Wallet `IBFT_FREEZE` gross (principal + fee) |
| 2 | Accounting POSTED (3400 = 0, **1112** Napas leg, **5100** cost) |
| 3 | S1 **200** accept |
| 4 | S6 `IBFT_PAYOUT` → Napas/bank adapter |
| 5a | Terminal SUCCESS → `IBFT_SETTLE` `{ref}:settle` |
| 5b | Terminal FAIL → `IBFT_RELEASE` `{ref}:release` |
| — | Timeout UNKNOWN → **poll**; no RELEASE until terminal |

Detail: Part II §15. Wire product index: [`napas-api-portal-intro.md`](../references/napas-api-portal-intro.md) (gated spec on portal).

### 3.6 QR/POS, payroll, disbursement, EOD

| Flow | Wallet default | Orchestration owner |
|------|----------------|---------------------|
| QR/POS capture | No per-txn `wallet_tx` | Webhook → accounting POSTED ([`accounting.md`](./accounting.md) §19) |
| QR optional credit | `PAYMENT_CREDIT`-class if product flag | Same capture ref idempotent |
| EOD settlement | Optional `MERCHANT_SETTLE_CREDIT` (W-O2) | Batch job ([`accounting.md`](./accounting.md) §22, [`wallet.md`](./wallet.md) §25) |
| Payroll / disbursement | `PAYROLL_DEBIT` / `DISBURSEMENT_DEBIT` | Batch + per-recipient sub-keys |

### 3.7 Pocket operations (USER, ADR-040)

| Flow | Wallet | Accounting | Notes |
|------|--------|------------|-------|
| Create pocket | insert `wallet` + `wallet_balance(0,0)`; no `wallet_tx` | **none** (no money moves) | Orchestration thin pass-through; resolve def + `multi_allowed` before create ([`processes.md`](../spec/processes.md) §11A.1) |
| Close pocket | set `status=CLOSED` (require `available=0 && frozen=0`) | **none** | `'default'` pocket not closable (§11A.2) |
| Pocket→pocket (same member) | `TRANSFER_DEBIT` A → `TRANSFER_CREDIT` B | POSTED, 3300=0, **2110 net 0** | Reuses TRANSFER saga (§13); both legs resolve to that member's pockets; fee 0 default (§11A.3) |

> Create/close touch **no ledger** — pure account lifecycle on the wallet hot path. Only pocket→pocket reaches accounting, via the normal transfer path.

---

## 4. Saga / compensation matrix

| Flow | Risk | Detection | Recovery |
|------|------|-----------|----------|
| Deposit | POSTED, wallet credit fails | Consumer lag / alert | Retry credit (idempotent) |
| Deposit | Double webhook | UNIQUE `business_ref` | No-op return |
| Payment | User debited, post fails | TX boundary / error | Compensating credit user OR forward-fix per policy |
| Payment | Posted, merchant credit fails | Partial completion | Retry merchant credit |
| Transfer | Debit A OK, credit B fails | Partial | Retry B or credit A back |
| Withdraw | Timeout | Bank poll | **Do not release** until terminal |
| Withdraw | Bank reject | Payout callback | `WITHDRAW_RELEASE` |
| IBFT | Double settle | Idempotent sub-keys | Block second settle |
| EOD | File mismatch | Recon report | Hold settlement |

**Principle:** compensation > blind rollback. Ledger POSTED is not undone by wallet; use reversal journal when accounting compensation is required.

---

## 5. Fee policy (orchestration-owned)

| Rule | Owner |
|------|-------|
| Fee **rates** and product rules | Orchestration config |
| Fee **amount** on each request | Orchestration computes before domain calls |
| Fee **posting** to 41xx / 51xx | Accounting records lines |
| Net vs gross split for wallet | Orchestration passes amounts; wallet does not recompute |

Example (deposit): bank 100,000, fee 1,000 → wallet credit **99,000**; accounting posts **4110** +1,000.

Example (transfer): debit A **101,000** gross, credit B **100,000** net, **4130** +1,000 on ledger.

---

## 6. Auth & identity (design-level)

| Concern | Rule |
|---------|------|
| Public S1 | JWT or mTLS per Gateway; orchestration extracts `memberId` |
| `memberId` | Platform mid — never infer from opaque partner id without lookup |
| S2 / S6 | Internal auth (service token); not exposed to partners |
| RBAC | Orchestration enforces role (user pay vs merchant admin vs ops) |
| Idempotency header | Client supplies stable `X-Idempotency-Key` → `businessRef` |

Detail of claims and roles is product config — not stored in domain modules.

---

## 7. Reliability patterns

| Pattern | Application |
|---------|-------------|
| Outbox | Publish S3/S6 after local TX commit |
| At-least-once | Consumers idempotent on `businessRef` |
| DLQ | Poison messages → `core.commands.dlq` |
| Aging | Pending PENDING deposit / stuck frozen → alert |
| Reconciliation | W5 + A3 nightly; report-only |
| CommandFailed | Kafka fan-out for ops ([`spec/contracts/async-api/core-events.yaml`](../spec/contracts/async-api/core-events.yaml)) |

---

## 8. Idempotency propagation

```
S1 X-Idempotency-Key
  → businessRef (orchestration)
  → S2 reference_id
  → S6 envelope.businessRef
  → S3 payload.businessRef
  → wallet_tx.business_ref
```

Per [ADR-005](../adr/ADR-005-idempotency-key-strategy.md). `messageId` per physical publish only.

---

## 9. Forbidden

| # | Rule |
|---|------|
| F1 | Gateway → SQL on `wallet_*` / `coa_*` |
| F2 | Cross-schema JOIN in application queries |
| F3 | Wallet module imports accounting repo |
| F4 | Accounting module imports wallet repo |
| F5 | Edit POSTED `coa_trans_data` because wallet drifted |
| F6 | Use Kafka partition key as idempotency key |

Full list: [`integration-surfaces.md`](../integration-surfaces.md) §9.

---

## 10. References

- Wire index: [`integration-surfaces.md`](../integration-surfaces.md)
- Conformance: [`acceptance.md`](./acceptance.md)
- Shared types: [`core.foundation.md`](../core.foundation.md) Part I

---

# Part II — Per use case (orchestration depth)

Orchestration owns **step order, fees, auth, saga recovery, and wire mapping**. Domains own local invariants. DR/CR templates: [`core.foundation.md`](../core.foundation.md) §8–16.

---

## 11. DEPOSIT saga (async)

### 11.1 Preconditions

| Check | On fail |
|-------|---------|
| Webhook signature / mTLS valid | 401, no enqueue |
| `amount > 0`, scale ≤ 4 | 400 |
| Currency = VND (v1) | 400 |
| `businessRef` present (bank txn id) | 400 |

### 11.2 Steps (local commits)

| Step | Component | Commit | Rollback |
|------|-----------|--------|----------|
| S0 | `app-orchestration` | Ack **202** + write outbox row (atomic) | N/A — safe to retry |
| S1 | `app-accounting-worker` | Journal PENDING: TB pending Transfer (1111←3100) + `coa_trans` | TX rollback; requeue |
| S2 | `app-accounting-worker` | Map VA → `memberId` + `walletId` | Hold PENDING, no journal |
| S3 | `app-accounting-worker` | Phase B `confirmDeposit`: TB post_pending + 2110/4110 transfers + `coa_trans` POSTED; assert 3100=0 | Stay PENDING or reverse A |
| S4 | `app-accounting-worker` | Publish **`WALLET_CREDIT`** to `core.commands.wallet-credit` (S6 RabbitMQ); optional `JournalPosted` to Kafka | Relay retry |
| S5 | `app-wallet-worker` | `DEPOSIT_CREDIT` net — `creditByWalletId(walletId, businessRef, netAmount)` | Idempotent retry |

> **`app-orchestration` NEVER calls `app-accounting` or `app-wallet` via HTTP in the deposit flow.** S0 writes only to the outbox; S1–S5 are worker-owned commits.

### 11.3 Fee computation (orchestration)

```
gross = webhook.amount
fee   = feeSchedule.deposit(memberId)   // config, not wallet
net   = gross - fee
wallet command amount = net
accounting lines = foundation §8.1 with fee line 4110
```

Single computed `fee` used for both S2 lines and S5 amount.

### 11.4 HTTP / async responses

| Client action | Success | Idempotent replay |
|---------------|---------|-------------------|
| `notifyDeposit` / webhook | **202** + `businessRef` | Same body → same 202 |
| `getDepositStatus` | `PENDING` / `POSTED` / `FAILED` | Read model from `coa_trans` via S2 |

### 11.5 Compensation

| Stuck after | Orchestration action |
|-------------|---------------------|
| S3 POSTED, S5 not done | Retry `WALLET_CREDIT` / consume `JournalPosted` |
| S1 never ran (202 lost) | Client re-notify same ref → enqueue |
| Bank recall after POSTED | Ops playbook: reversal journal (S2) + wallet debit/adjustment |
| PENDING > SLA | Alert; ops complete S3 or reverse A |

### 11.6 Forbidden

- Credit wallet before S3 `POSTED`
- Second S1 enqueue with new `messageId` but same `businessRef` causing duplicate PENDING (accounting idempotency must block)
- Edit fee after S3 without new journal
- **HTTP call from `app-orchestration` to `app-accounting` or `app-wallet` for deposit** — orchestration owns only S0 (outbox write); S1–S5 are worker commits via RabbitMQ, never sync HTTP (ADR-038, ADR-041)

---

## 12. PAYMENT saga (sync)

### 12.1 Preconditions

| Check | On fail |
|-------|---------|
| JWT `memberId` = payer | 403 |
| Payer ≠ payee | 422 |
| `X-Idempotency-Key` present | 400 |
| Merchant wallet exists or auto-provision policy | 422 / provision |

### 12.2 Steps

```
1. wallet.debit(USER, gross, businessRef, PAYMENT_DEBIT)
2. accounting.create + lines + post (3500=0)
3. wallet.credit(MERCHANT, netToMerchant, businessRef, PAYMENT_CREDIT)
4. return 200 { walletTxId, coaTransId, businessRef }
```

All in one orchestration **request scope**; steps 1–3 are separate local TXs (no 2PC).

### 12.3 Failure policy

| Fails after step | Policy | HTTP |
|------------------|--------|------|
| 1 | Nothing persisted | 422 insufficient |
| 2 (post) | **Compensate** step 1: `credit(USER, gross, {ref}:comp)` | 503 / 422 per product |
| 3 (merchant) | **Forward-retry** 3; ledger stands | 200 if 3 succeeds on retry; else 503 + ops |
| 4 lost | Client retry same key → idempotent 200 | 200 |

### 12.4 Response contract

| Field | Source |
|-------|--------|
| `businessRef` | `X-Idempotency-Key` |
| `coaTransId` | S2 post response |
| `walletTxId` | debit leg id (or array if API exposes both) |

---

## 13. TRANSFER saga (sync)

Same 3-step shape as payment; both legs `USER`.

| Step | Amount |
|------|--------|
| Debit A | `gross = net + fee` |
| Post | foundation §10 |
| Credit B | `net` |

Failure policy identical to §12.3 with A/B instead of user/merchant.

---

## 14. WITHDRAW saga (sync accept + async bank)

### 14.1 Steps

| Step | When | Notes |
|------|------|-------|
| W1 | `WITHDRAW_FREEZE` gross | Before **200** |
| W2 | Accounting POSTED (3200=0) | Per O1 — typically on accept |
| W3 | **200** accept to client | User sees frozen |
| W4 | S6 `WITHDRAW_PAYOUT` | Async worker |
| W5a | Bank SUCCESS → `WITHDRAW_SETTLE` `{ref}:settle` | |
| W5b | Bank FAIL → `WITHDRAW_RELEASE` `{ref}:release` | |

### 14.2 Double-spend rule (orchestration)

```
IF bank_status == UNKNOWN (timeout):
  DO NOT call WITHDRAW_RELEASE
  POLL bank until SUCCESS | FAILED | REJECTED
ONLY on terminal failure → RELEASE
```

### 14.3 Aging jobs

| Condition | Action |
|-----------|--------|
| Frozen, no payout enqueued > T1 | Re-enqueue W4 or RELEASE per ops |
| Payout sent, no callback > T2 | Poll bank |
| Frozen > Tmax | Severity alert — manual |

### 14.4 Idempotency

| Leg | Key |
|-----|-----|
| Accept | `businessRef` |
| Settle | `{businessRef}:settle` |
| Release | `{businessRef}:release` |
| Payout command | `businessRef` in S6 envelope |

---

## 15. IBFT saga (sync accept + async Napas)

**Wallet reference:** [`wallet.md`](./wallet.md) §19 · **Accounting:** [`accounting.md`](./accounting.md) §18 · **Foundation:** §11 (transit **3400**, **1112**, **5100**, **4130**).

### 15.1 Preconditions

| Check | On fail |
|-------|---------|
| JWT `memberId` = payer | 403 |
| Beneficiary account / bank code valid (schema) | 422 |
| `X-Idempotency-Key` present | 400 |
| `available >= gross` (principal + fee) | 422 before freeze |
| Currency VND v1 | 400 |
| Napas/bank route available | 503 / 422 per adapter |

Beneficiary identity is validated and stored in **orchestration saga record** — wallet has no beneficiary columns.

### 15.2 Steps

| Step | When | Component | Notes |
|------|------|-----------|-------|
| I1 | Before **200** | Wallet `IBFT_FREEZE` gross | Same hold semantics as §14 W1 |
| I2 | On accept | Accounting POSTED IBFT template | 3400 = 0; **1112** not **1111** on bank leg |
| I3 | After I1+I2 | S1 **200** accept | Client sees `frozen` increased |
| I4 | Async | S6 `IBFT_PAYOUT` | Envelope `businessRef`; Napas message id in adapter |
| I5a | Terminal SUCCESS | Wallet `IBFT_SETTLE` `{ref}:settle` | Debit frozen only |
| I5b | Terminal FAIL/REJECT | Wallet `IBFT_RELEASE` `{ref}:release` | Restore available |

Fee revenue **4130** and Napas cost **5100** are accounting lines on I2 — orchestration passes single computed `fee` into line builder; wallet holds **gross** only.

### 15.3 Fee computation

```
gross     = principal + feeSchedule.ibft(memberId)
fee       = feeSchedule.ibft(memberId)
principal = request.amount   // beneficiary receives principal at bank; net/fee split is ledger
wallet freeze amount = gross
accounting lines = foundation §11 with 4130 + 5100
```

### 15.4 Double-spend / timeout rule (orchestration — locked)

```
IF napas_status IN (UNKNOWN, PENDING, TIMEOUT):
  DO NOT call IBFT_RELEASE
  POLL adapter until SUCCESS | FAILED | REJECTED | EXPIRED
ONLY on terminal failure → RELEASE
ON terminal SUCCESS → SETTLE (retry idempotent)
```

Same policy class as withdraw §14.2. **Timeout ≠ RELEASE** ([`wallet.md`](./wallet.md) §4.1, I-W3).

### 15.5 Napas / bank adapter contract (design)

| Concern | Rule |
|---------|------|
| Submit idempotency | Adapter dedupes on `businessRef` (I-F3) |
| Status poll | Orchestration or worker polls until terminal |
| Duplicate Napas response | Map to idempotent SETTLE or no-op |
| Partial response | Treat as UNKNOWN → poll |
| Ledger vs bank timing | Wallet SETTLE only after terminal SUCCESS — not on Napas ACK alone if product requires settlement confirm |

Full message formats: Napas DPP portal (not in repo). Product list reference: [`napas-api-portal-intro.md`](../references/napas-api-portal-intro.md).

### 15.6 Aging jobs

| Condition | Action |
|-----------|--------|
| Frozen, no I4 enqueued > T1 | Re-enqueue payout or ops |
| Payout sent, no callback > T2 | Poll Napas/bank |
| Frozen > Tmax | Severity alert — manual |
| SUCCESS but SETTLE fails | Retry `{ref}:settle` — **no** second payout |

### 15.7 Idempotency

| Leg | Key |
|-----|-----|
| Accept / freeze | `businessRef` |
| Settle | `{businessRef}:settle` |
| Release | `{businessRef}:release` |
| S6 payout | `businessRef` in envelope |

### 15.8 Failure matrix (orchestration)

| ID | Condition | Action | Forbidden |
|----|-----------|--------|-----------|
| I-O1 | I1 fails insufficient | 422, no I2 | POSTED without freeze |
| I-O2 | I2 fails after I1 | Compensate RELEASE or forward-fix per policy | Leave frozen without saga |
| I-O3 | I4 duplicate enqueue | Adapter no-op | Double bank debit |
| I-O4 | UNKNOWN timeout | Poll only | RELEASE |
| I-O5 | SUCCESS, SETTLE fails | Retry `:settle` | Second I4 |
| I-O6 | RELEASE after SETTLE | State machine block | Double available restore |
| I-O7 | Wrong gross on replay | 409 | Apply |
| I-O8 | 1111 used on bank leg | Reject at line build | Wrong clearing acct |

### 15.9 Forbidden

- RELEASE on timeout alone
- SETTLE before terminal bank SUCCESS (unless product explicitly defines ACK=success)
- Wallet credit beneficiary — IBFT is outflow hold only; beneficiary paid via bank rail
- Infer fee inside wallet module

---

## 16. QR/POS + EOD (batch orchestration)

### 16.1 QR capture (per txn)

| Step | Owner | Wallet |
|------|-------|--------|
| Q1 | Validate acquirer webhook / mTLS | — |
| Q2 | Fee/MDR compute (orchestration) | — |
| Q3 | Accounting POSTED capture (3500 = 0) | **Default: none** ([`wallet.md`](./wallet.md) §24) |
| Q4 (optional) | Product flag `creditMerchantOnCapture` | `PAYMENT_CREDIT` or dedicated type, ref = capture id |

| Q4 precondition | Rule |
|-----------------|------|
| Accounting Q3 POSTED | Gate before wallet |
| Duplicate capture ref | Idempotent wallet row |
| Amount mismatch | 409 |

### 16.2 EOD job (per merchant / settlement date)

| Step | Idempotency key | Wallet |
|------|-----------------|--------|
| E1 | `(merchantId, settlementDate)` | — |
| E2 | Load file + snapshot **2120** | — |
| E3 | Validate totals — mismatch → **stop** | No wallet command |
| E4 | Lock 2120 → 3800, MDR 3820, bank 3810 | — |
| E5 | Bank out retry on fail | Amount stays 3810 |
| E6 (optional) | `{merchantId}:{settlementDate}` | `MERCHANT_SETTLE_CREDIT` net ([`wallet.md`](./wallet.md) §25) |

On file mismatch: **stop** — no partial settle ([`accounting.md`](./accounting.md) §22). E6 runs only after E4 success and product flag W-O2 enabled.

### 16.3 EOD ↔ wallet reconciliation

If E6 enabled: orchestration stores `netSettlement` on saga record; W5 job may compare merchant wallet delta vs **2120** movement — report-only, no auto COA write from wallet.

---

## 17. PAYROLL / DISBURSEMENT batch

| Rule | Detail |
|------|--------|
| Batch ref | `businessRef` = client batch id |
| Per recipient | `{businessRef}:{recipientId}` |
| Partial success | Return summary `{ succeeded[], failed[], retrying[] }` |
| Fee | Orchestration computes per batch; `PAYROLL_DEBIT` / `DISBURSEMENT_DEBIT` gross |

### 17.1 Payroll step order

| # | Action |
|---|--------|
| 1 | Validate MERCHANT JWT + sufficient `available` for gross |
| 2 | `PAYROLL_DEBIT` MERCHANT gross |
| 3 | Accounting POSTED payroll template (3600=0) |
| 4 | S6 per-recipient payout commands |
| 5 | Emit batch summary event |

Failed recipient: retry forward or release hold per recipient — **do not** roll back succeeded legs.

### 17.2 Disbursement step order

| # | Action |
|---|--------|
| 1 | Optional: prefund POSTED + `PARTNER_PREFUND_CREDIT` |
| 2 | Validate PARTNER `available` ≥ disburse gross |
| 3 | `DISBURSEMENT_DEBIT` PARTNER |
| 4 | Accounting POSTED (3700=0) |
| 5 | Per-beneficiary bank out idempotent sub-keys |

### 17.3 EOD settlement job

| # | Action |
|---|--------|
| 1 | Load acquirer file + internal 2120 snapshot |
| 2 | Validate totals — mismatch → stop (no lock) |
| 3 | Per merchant: lock 2120→3800, MDR 3820, settle 3810 |
| 4 | Bank out — fail keeps 3810 for retry |
| 5 | Optional `MERCHANT_SETTLE_CREDIT` per product flag |

Idempotency: `(merchantId, settlementDate)` on steps 3–5.

---

## 18. Cross-cutting orchestration state

### 18.1 Saga record (orchestration DB — design)

| Field | Purpose |
|-------|---------|
| `business_ref` | Primary key |
| `use_case` | DEPOSIT, PAYMENT, … |
| `step` | Last completed step |
| `status` | IN_PROGRESS, COMPLETED, COMPENSATING, FAILED |
| `coa_trans_id` | After post |
| `wallet_tx_ids` | JSON array of legs |

Enables ops dashboard and aging without JOIN to `coa_*` / `wallet_*` in domain queries (orchestration may store copies of ids only).

### 18.2 Outbox relay → RabbitMQ → accounting backend

The back-office accounting backend is fed **asynchronously** via an **outbox + RabbitMQ (S6)** — not a synchronous in-line write from the hot path. This is how the split is bridged without 2PC ([§1.1](#11-why-this-layer-is-mandatory-not-optional-glue), [ADR-013](../adr/ADR-013-outbox-at-least-once-messaging.md)).

```
saga step (local TX) ──writes──► outbox row   (same DB TX, atomic)
        │
   relay (poll/CDC) ──publish──► RabbitMQ S6 command  (core.commands.*)
                                       │
                          accounting backend listener ──► post coa_trans / coa_trans_data
                                       │
                          emit JournalPosted (S3 Kafka) ──► drives next wallet leg
```

| Rule | Detail |
|------|--------|
| Atomic capture | Outbox row written in the **same local TX** as the saga step advance — no lost message, no dual-write |
| Delivery | Relay is **at-least-once** ([ADR-013](../adr/ADR-013-outbox-at-least-once-messaging.md)); duplicates expected |
| Accounting idempotency | Listener dedups on `reference_id` (= `business_ref`) UNIQUE per `use_case` — replay posts once ([`spec/foundation.md`](../spec/foundation.md) §8.5) |
| Worker | RabbitMQ workers, not Temporal v1 ([ADR-035](../adr/ADR-035-rabbitmq-workers-not-temporal-v1.md)); e.g. `BANK_DEPOSIT`, `WALLET_CREDIT`, `WITHDRAW_PAYOUT` commands |
| Ordering | Per-`business_ref` correlation in the saga record (§18.1); accounting does not rely on global order |
| Failure | Poison message → DLQ (§18.3); accounting never blocks the wallet hot path |

> Accounting is therefore **eventually** consistent with the triggering event — acceptable because it is the back-office system-of-record, not the spendable balance. The authoritative spendable number stays on the wallet hot path ([ADR-004](../adr/ADR-004-wallet-balance-snapshot.md)).

**Two access paths to accounting (not a contradiction):**

| Path | When | Mechanism |
|------|------|-----------|
| **Async** outbox → RabbitMQ (S6) | deposit, batch (payroll/disburse/EOD), any post that can lag the hot path | this section; at-least-once command, idempotent listener |
| **Sync** S2 HTTP `accounting-internal` | payment / transfer where the journal must POST **before** returning 200 ([§3.2](#32-payment-sync)/[§3.3](#33-transfer-sync)) | [ADR-038](../adr/ADR-038-orchestrator-separate-service-gateway-seam.md) gateway seam, over the network |

The orchestrator chooses per use case; both go through the gateway seam, neither lets a domain call another.

### 18.3 DLQ handling

| Event | Action |
|-------|--------|
| Poison S6 message | `core.commands.dlq` + `CommandFailed` |
| Retry budget exceeded | Ops queue + saga `FAILED` |

### 18.4 HTTP error mapping (S1)

| Domain code | HTTP |
|-------------|------|
| `WALLET_INSUFFICIENT_BALANCE` | 422 |
| `WALLET_DUPLICATE_CONFLICT` | 409 |
| `WALLET_LOCKED` | 403 |
| `ACCOUNTING_*` validation | 422 |
| Transient downstream | 503 + retry-safe idempotency |

---

## 19. Auth enforcement matrix

| Endpoint family | Principal | Body `memberId` |
|-----------------|-----------|-----------------|
| `createPayment` | USER JWT | Must match sub |
| `createTransfer` | USER JWT | Payer = sub |
| `createWithdrawal` | USER JWT | sub only |
| `notifyDeposit` | mTLS bank | VA lookup, not body member |
| Merchant payroll | MERCHANT JWT | Merchant scope |
| S2 / S6 | Service token | N/A |

`X-Idempotency-Key` scoped to principal — key from member A cannot replay member B's action.

---

## 20. Fee schedule (orchestration config — v1 examples)

| Use case | Bearer | Revenue acct | v1 example |
|----------|--------|--------------|------------|
| Deposit | User (netted) | 4110 | 1,000 flat |
| Withdraw | User (added to gross) | 4120 | 1,000 |
| Transfer / IBFT | User | 4130 | 1,000 |
| QR/POS MDR | Merchant | 4140 | % of amount |
| Payroll / disbursement | Merchant / Partner | 4150 | per recipient |

Rounding: scale 4, HALF_UP at boundary ([`core.foundation.md`](../core.foundation.md) §6). Orchestration logs computed fee on saga record for audit.

---

## 21. Observability (orchestration)

| Metric | Alert |
|--------|-------|
| `deposit_pending_3100_age` | > SLA |
| `wallet_credit_lag_after_posted` | > 5 min |
| `frozen_without_payout` | > T1 |
| `saga_compensation_total` | spike |
| `idempotency_conflict_rate` | spike |

---

## 22. Part II traceability

| Use case | Accounting | Wallet | Acceptance |
|----------|------------|--------|------------|
| Deposit | accounting §14 | wallet §15 | acceptance Deposit |
| Payment | §16 | wallet §16 | acceptance Payment |
| Transfer | §17 | wallet §17 | acceptance Transfer |
| Withdraw | §15 | wallet §18 | acceptance Withdraw |
| IBFT | §18 | wallet §19 | acceptance IBFT |
| QR/POS | §19 | wallet §24 | acceptance QR/POS + QR-E |
| EOD | §22 | wallet §25 | acceptance EOD + EOD-E |
| Payroll | §20 | wallet §20.1 | acceptance Payroll + PR-E |
| Disbursement | §21 | wallet §20.2 | acceptance Disbursement + DIS-E |
| Cross-cutting | §23 | wallet §26 | acceptance X-E |

---

## 23. Bank poll policy (withdraw + IBFT unified)

| Status class | Examples | Orchestration action |
|--------------|----------|----------------------|
| **Terminal success** | `SUCCESS`, `SETTLED`, `COMPLETED` | `*_SETTLE` `{ref}:settle` |
| **Terminal failure** | `FAILED`, `REJECTED`, `RETURNED`, `EXPIRED` | `*_RELEASE` `{ref}:release` |
| **Non-terminal** | `PENDING`, `UNKNOWN`, `TIMEOUT`, adapter 504 | Poll; **frozen unchanged** |
| **Ambiguous duplicate** | Two SUCCESS callbacks | Idempotent SETTLE once |

| Parameter | Owner | v1 default |
|-----------|-------|------------|
| Poll interval T2 | Ops config | Product — **not in repo** |
| Max frozen age Tmax | Ops config | Alert only |
| Adapter retry budget | Worker | 3 with backoff → DLQ |

Withdraw §14 and IBFT §15 share the same worker abstraction (`PAYOUT_STATUS_POLL`) with `use_case` discriminator.

---

## 24. Balance visibility (orchestration read model)

Wallet stores only `available` + `frozen` ([`wallet.md`](./wallet.md) §2.1). Orchestration may expose richer **read APIs** without new wallet columns:

| UI label | Source | When |
|----------|--------|------|
| Spendable | `wallet.available` | Always |
| On hold | `wallet.frozen` | Withdraw/IBFT in flight |
| Deposit processing | Saga / `coa_trans` status PENDING | No wallet credit yet |
| Total exposure | `available + frozen` | Optional display |

`getDepositStatus`, `getWithdrawalStatus`, `getTransferStatus` aggregate saga + domain ids — no JOIN across `wallet_*` and `coa_*` in one SQL; orchestration stores copied ids on saga record (§18.1).

---

## 25. Reference synthesis — orchestration & integration (`references/`)

### 25.1 Saga & compensation

| Reference | Lesson | Project |
|-----------|--------|---------|
| [`microsoft-saga-pattern.md`](../references/microsoft-saga-pattern.md) | Choreography vs orchestration | **Orchestration** saga (ADR-008) |
| [`aws-saga-orchestration.md`](../references/aws-saga-orchestration.md) | AWS saga orchestration pattern | §4 matrix, §11–17 |
| [`microservices-io-saga.md`](../references/microservices-io-saga.md) | Saga definitions | business-processes §13 |
| [`zhorifiandi-saga-stuck-systems.md`](../references/zhorifiandi-saga-stuck-systems.md) | Unstick async flows | ADR-007 poll; ADR-021 aging |
| [`temporal-saga-patterns.md`](../references/temporal-saga-patterns.md) | Durable workflow saga | Informative for worker implementation |
| [`infoq-saga-orchestration-outbox.md`](../references/infoq-saga-orchestration-outbox.md) | Saga + outbox together | ADR-013 |
| [`confluent-chris-richardson-saga.md`](../references/confluent-chris-richardson-saga.md) | Richardson saga narrative | ADR-008 |

### 25.2 Outbox & messaging

| Reference | Lesson | Project |
|-----------|--------|---------|
| [`microservices-io-transactional-outbox.md`](../references/microservices-io-transactional-outbox.md) | Transactional outbox | §18.2; ADR-013 |
| [`decodable-outbox-pattern.md`](../references/decodable-outbox-pattern.md) | Outbox revisit | Same |
| [`adyen-webhooks.md`](../references/adyen-webhooks.md) | Webhook idempotency + HMAC | Bank/acquirer webhooks pattern |

### 25.3 Idempotency & API design

| Reference | Lesson | Project |
|-----------|--------|---------|
| [`stripe-idempotency-blog.md`](../references/stripe-idempotency-blog.md), [`stripe-api-idempotent-requests.md`](../references/stripe-api-idempotent-requests.md) | Idempotency keys | ADR-005; §8 |
| [`adyen-api-idempotency.md`](../references/adyen-api-idempotency.md) | PSP idempotency | Same key semantics |
| [`levelup-idempotency-payments.md`](../references/levelup-idempotency-payments.md) | Payment idempotency architecture | D1–D5 |
| [`stripe-dev-payment-api-design.md`](../references/stripe-dev-payment-api-design.md) | Payment API ergonomics | S1 OpenAPI; fee at boundary |
| [`pragmatic-engineer-designing-payment-system.md`](../references/pragmatic-engineer-designing-payment-system.md) | End-to-end payment system | Read order with business-processes |

### 25.4 Settlement, VA, Napas

| Reference | Lesson | Project |
|-----------|--------|---------|
| [`stripe-payment-settlement-explained.md`](../references/stripe-payment-settlement-explained.md) | Settlement timing vs auth | Withdraw/IBFT settle after bank terminal |
| [`moderntreasury-virtual-accounts.md`](../references/moderntreasury-virtual-accounts.md) | VA for deposit attribution | ADR-030 |
| [`increase-api-accounts.md`](../references/increase-api-accounts.md) | FBO / sub-account API | VA mapping pattern |
| [`napas-api-portal-intro.md`](../references/napas-api-portal-intro.md) | Napas DPP products | §15.5 IBFT |

### 25.5 DDD & composable orchestration

| Reference | Lesson | Project |
|-----------|--------|---------|
| [`martinfowler-bounded-context.md`](../references/martinfowler-bounded-context.md), [`devto-ddd-bounded-contexts.md`](../references/devto-ddd-bounded-contexts.md) | Shared entity across contexts | No JOIN; saga record stores ids |
| [`airwallex-ddd-payments.md`](../references/airwallex-ddd-payments.md) | Payments bounded contexts | wallet vs accounting |
| [`crossmint-wallet-architecture-fintech.md`](../references/crossmint-wallet-architecture-fintech.md) | Wallet product architecture | wallet.md boundary |

### 25.6 P2P / transfer patterns (orchestration order)

| Reference | Lesson | Project |
|-----------|--------|---------|
| [`tianpan-designing-paypal-transfer.md`](../references/tianpan-designing-paypal-transfer.md) | Deposit, P2P, dedup | Transfer §13; ADR-005 |
| [`medium-codefarm-digital-wallet-system.md`](../references/medium-codefarm-digital-wallet-system.md) | Top-up, P2P, withdraw interview | Step order §3 |
| [`moderntreasury-how-to-build-digital-wallet.md`](../references/moderntreasury-how-to-build-digital-wallet.md) | Wallet from ledger | Orchestration sequences wallet after POSTED |

### 25.7 Gaps from references (not in repo spec)

| Topic | Reference suggests | Status |
|-------|-------------------|--------|
| Bank SLA / poll interval T2 | Stripe settlement, MT scale | [ADR-033](../adr/ADR-033-bank-poll-t2-frozen-tmax.md) — values ops-owned |
| Webhook signature algorithm detail | Adyen webhooks | In `spec/contracts/open-api/` — verify per integration |
| Temporal / durable workflow mandate | temporal-saga | [ADR-035](../adr/ADR-035-rabbitmq-workers-not-temporal-v1.md) — RabbitMQ v1 |

---

## 26. Related docs (Part II)

| Need | Read |
|------|------|
| Wallet hold semantics | [`wallet.md`](./wallet.md) §4.1, §18–19, §24–25 |
| Ledger per flow | [`accounting.md`](./accounting.md) Part II |
| Wire surfaces | [`integration-surfaces.md`](../integration-surfaces.md) |
| End-to-end narrative | [`core.business-processes.md`](../core.business-processes.md) §13–17 |
| Conformance | [`acceptance.md`](./acceptance.md) |
| External corpus | [`references/README.md`](../references/README.md) |

---

## 27. Corpus note

| File | Lines (approx) | Role |
|------|----------------|------|
| `accounting.md` | 720+ | Ledger + §26–27 ref synthesis |
| `wallet.md` | 750+ | Wallet + §27–28 ref synthesis |
| `orchestration.md` | 760+ | This file — §25 ref synthesis |
| `acceptance.md` | 1700+ | ~177 Gherkin scenarios |
| `references/` | 32.726 | 108 scraped files — index in `references/README.md` |
| **Total design-v2** | **~3900+** | DR/CR stays in `core.foundation.md` |
