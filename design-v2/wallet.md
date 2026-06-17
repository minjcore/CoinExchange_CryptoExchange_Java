# core.wallet — Design

**Status:** Draft · **Scope:** per-member balance (`wallet_*`) · **Not:** ledger, COA, fee rates, public HTTP

Ledger postings: [`core.foundation.md`](../core.foundation.md) Part II §8–16 · Accounting behavior: [`accounting.md`](./accounting.md)

This document defines **wallet obligations, operations, failures, and boundaries** — not a second copy of [`core.wallet.md`](../core.wallet.md) DDL sections.

---

## 1. Domain contract

Wallet answers: *how much can this member spend right now?*

| Guarantees | Never does |
|------------|------------|
| `available` / `frozen` reflect every mutation via new `wallet_tx` | Post journals or touch `coa_*` |
| `UNIQUE(wallet_id, business_ref, tx_type)` idempotency | Recompute fees (orchestration passes amounts) |
| Append-only `wallet_tx` — no UPDATE history | Expose HTTP or parse Kafka in domain module |
| Deposit credit only when orchestration says ledger POSTED | JOIN accounting tables |
| Row-level consistency per wallet command | Auto-fix COA 2110/2120/2130 |

Aggregate liabilities **2110 / 2120 / 2130** live in accounting. Wallet holds **per-member** slices reconciled in batch (W5).

---

## 2. Core concepts

| Term | Meaning |
|------|---------|
| **Wallet instance (pocket)** | `member_id` + `wallet_type` + `currency` + `label`. A USER may hold **multiple pockets** (label-distinguished, instantiated from `wallet_pocket_def`); MERCHANT/PARTNER single ([ADR-040](../adr/ADR-040-user-multi-pocket-wallets.md)). All USER pockets → control 2110 |
| **available** | Spendable now |
| **frozen** | Held (withdraw / IBFT in flight) |
| **wallet_tx** | Append-only movement; `direction` + `tx_type` |
| **business_ref** | Idempotency key = S1 `X-Idempotency-Key` (ADR-005) |
| **coa_trans_id** | Correlation only — not idempotency, no FK |

Lanes: `USER` → control 2110 · `MERCHANT` → 2120 · `PARTNER` → 2130 (reconciliation mapping only).

### 2.1 Balance semantics (industry → this project)

Industry wallets often expose three computed views ([`moderntreasury-balance-types-part-i.md`](../references/moderntreasury-balance-types-part-i.md)). This project **stores only two columns** on `wallet_balance`; everything else is derived or owned by another domain.

| Industry term | Formula (typical) | Where it lives here | Notes |
|---------------|-------------------|---------------------|-------|
| **Posted balance** | Σ posted in − Σ posted out | Accounting control **2110 / 2120 / 2130** (aggregate) | Per-member posted slice ≈ `SUM(wallet_tx)` for settled legs; W5 reconciles aggregate |
| **Available balance** | Posted in − (posted out + pending out) | **`wallet_balance.available`** | Authoritative spendable; debits/freeze check this column |
| **Pending balance** | Includes unsettled inflows and outflows | **Not a wallet column** | Deposit before POSTED: wallet unchanged; UI may show "processing" from orchestration/accounting state |
| **Hold / pending outflow** | Amount reserved, not yet settled | **`wallet_balance.frozen`** | Withdraw, IBFT, any in-flight outflow hold |

**Public API default (v1):** return `available` as spendable. Optionally expose `frozen` so the member sees held funds. Do **not** imply `available + frozen` equals bank balance — bank leg and COA aggregate can lag (async deposit, EOD).

**Deposit visibility:** Until orchestration credits after POSTED, `available` is **unchanged** even if the member initiated a bank transfer. Inflow "pending" is not modeled inside `wallet_*` (transit **3100** is accounting-only — see [`accounting.md`](./accounting.md) §14).

**Payment / transfer:** Sync legs update `available` immediately on debit/credit. No separate pending column — orchestration must complete both wallet legs in the designed order before returning success to the client.

---

## 3. Locked decisions (D1–D5)

From [`IMPLEMENTATION.md`](../IMPLEMENTATION.md) §2 — wallet-facing summary. **ADR:** [ADR-003](../adr/ADR-003-dual-schema-single-postgres.md) · [ADR-004](../adr/ADR-004-wallet-balance-snapshot.md) · [ADR-005](../adr/ADR-005-idempotency-key-strategy.md) · [ADR-006](../adr/ADR-006-two-phase-deposit.md) (credit after POSTED) · [ADR-007](../adr/ADR-007-freeze-settle-async-outflow.md) (§18–19) · [ADR-009](../adr/ADR-009-fee-ownership-orchestration.md) (no fee math).

| ADR | Wallet rule |
|-----|-------------|
| ADR-003 | Schema `wallet`; `coa_trans_id` correlation only |
| ADR-004 | `wallet_balance` authoritative; `wallet_tx` append-only same TX |
| ADR-005 | `UNIQUE(wallet_id, business_ref, tx_type)` + sub-keys D5 |


| ID | Rule |
|----|------|
| D1 | Idempotency triple: `(wallet_id, business_ref, tx_type)`; conflict amount → 409 |
| D2 | Payment: same `businessRef`, `PAYMENT_DEBIT` + `PAYMENT_CREDIT` on different wallets |
| D3 | Transfer: same ref, `TRANSFER_DEBIT` / `TRANSFER_CREDIT`; debit **gross**, credit **net** |
| D4 | Deposit: `DEPOSIT_CREDIT` after POSTED; amount = **net** from command |
| D5 | Withdraw/IBFT: sub-keys `{businessRef}:settle`, `{businessRef}:release` for follow-up legs |

---

## 4. Operations (design-level)

| Operation | Balance effect | `wallet_tx.direction` |
|-----------|----------------|----------------------|
| credit | `available += amount` | CREDIT |
| debit | `available -= amount` if sufficient | DEBIT |
| freeze | `available -= amount`, `frozen += amount` | FREEZE |
| unfreeze | reverse freeze | UNFREEZE |
| settle (withdraw) | `frozen -= amount` (debit from frozen) | DEBIT |
| lock | `status = LOCKED` | — |

Every balance change = **one** new `wallet_tx` in the **same** DB transaction as `wallet_balance` update (W2).

### 4.1 Hold / freeze lifecycle (industry mapping)

Withdraw and IBFT use the same authorization pattern as banking-core **transaction holds** ([`mambu-transaction-holds.md`](../references/mambu-transaction-holds.md)) and ledger **inflight** states ([`blnk-transaction-lifecycle.md`](../references/blnk-transaction-lifecycle.md)).

| Industry action | Mambu / Blnk | Wallet operation | `tx_type` (withdraw) | `tx_type` (IBFT) |
|-----------------|--------------|------------------|----------------------|------------------|
| Create hold | Hold posted; available ↓ | `freeze` | `WITHDRAW_FREEZE` | `IBFT_FREEZE` |
| Settle hold | Hold cleared; funds leave | `settle` (debit frozen) | `WITHDRAW_SETTLE` | `IBFT_SETTLE` |
| Cancel / reverse hold | Hold reversed; available restored | `unfreeze` | `WITHDRAW_RELEASE` | `IBFT_RELEASE` |

**Rules (locked):**

| Rule | Rationale |
|------|-----------|
| Freeze amount ≤ `available` at accept | Same as Mambu: hold cannot exceed available ([double-spend patterns](../references/medium-double-spend-fintech.md)) |
| `available + frozen` constant until SETTLE or RELEASE | Economic exposure unchanged during hold |
| **Timeout ≠ RELEASE** | Bank/Napas status unknown → poll; frozen stays until terminal bank outcome ([`orchestration.md`](./orchestration.md)) |
| SETTLE after RELEASE forbidden | Sub-keys `:settle` / `:release` + orchestration state machine |
| One hold per `business_ref` freeze leg | Idempotent replay returns same `wallet_tx_id` |

Wallet does **not** implement Mambu card **authorization holds** ([`mambu-authorization-holds.md`](../references/mambu-authorization-holds.md)) — card networks are out of scope v1. The `FREEZE`/`UNFREEZE` model covers fiat withdraw/IBFT only.

---

## 5. Use cases — wallet obligation

| Use case | Pattern | Wallet steps | Amount source |
|----------|---------|--------------|---------------|
| Deposit | async | `DEPOSIT_CREDIT` after POSTED | net from orchestration |
| Payment | sync | DEBIT user → CREDIT merchant | gross / net from orchestration |
| Transfer | sync | DEBIT A gross → CREDIT B net | orchestration |
| Withdraw | freeze + async bank | FREEZE → SETTLE or RELEASE | gross (principal + fee) |
| IBFT | same as withdraw | `IBFT_*` tx_types | gross |
| Partner / disbursement | batch | `PARTNER_PREFUND_CREDIT`, `DISBURSEMENT_DEBIT` | orchestration |
| Payroll | batch | `PAYROLL_DEBIT` MERCHANT | gross batch |
| QR/POS | often none per txn | optional `MERCHANT_SETTLE_CREDIT` at EOD | product |
| EOD | optional bulk | `MERCHANT_SETTLE_CREDIT` if product opts in | orchestration |

Accounting step order: [`accounting.md`](./accounting.md) §4 · Orchestration: [`orchestration.md`](./orchestration.md).

---

## 6. `tx_type` catalog

| `tx_type` | Direction | Lane |
|-----------|-----------|------|
| `DEPOSIT_CREDIT` | CREDIT | USER |
| `PAYMENT_DEBIT` | DEBIT | USER |
| `PAYMENT_CREDIT` | CREDIT | MERCHANT |
| `TRANSFER_DEBIT` | DEBIT | USER |
| `TRANSFER_CREDIT` | CREDIT | USER |
| `WITHDRAW_FREEZE` | FREEZE | USER |
| `WITHDRAW_SETTLE` | DEBIT | USER |
| `WITHDRAW_RELEASE` | UNFREEZE | USER |
| `IBFT_FREEZE` | FREEZE | USER |
| `IBFT_SETTLE` | DEBIT | USER |
| `IBFT_RELEASE` | UNFREEZE | USER |
| `PARTNER_PREFUND_CREDIT` | CREDIT | PARTNER |
| `DISBURSEMENT_DEBIT` | DEBIT | PARTNER |
| `PAYROLL_DEBIT` | DEBIT | MERCHANT |
| `MERCHANT_SETTLE_CREDIT` | CREDIT | MERCHANT |
| `ADJUSTMENT_*` | CREDIT/DEBIT | any (ops) |

---

## 7. Failure & compensation (wallet side)

Principle: wallet **never** reverses accounting. Compensation = new `wallet_tx` with new or sub-key `business_ref`, or orchestration saga step.

### 7.1 Deposit

| Failure | Wallet state | Recovery | Forbidden |
|---------|--------------|----------|-----------|
| Credit before POSTED | unchanged | Reject or no-op at orchestration | Credit on PENDING |
| Duplicate `DEPOSIT_CREDIT` | credited | Idempotent return same `wallet_tx_id` | Double credit |
| Same ref, different net amount | — | `WALLET_DUPLICATE_CONFLICT` 409 | Apply wrong net |
| Consumer retry after success | credited | No-op replay | Second credit |
| POSTED but consumer down | not credited | Retry command/event | Wallet pulls from COA |
| Ledger reversed later (ops) | credited | Orchestration **debit/adjustment** policy | Wallet calls accounting |

### 7.2 Payment

| Failure | Wallet state | Recovery | Forbidden |
|---------|--------------|----------|-----------|
| Insufficient `available` | unchanged | Reject debit; no merchant leg | Partial debit |
| User debited, merchant credit fails | user down | Retry `PAYMENT_CREDIT` idempotent | Auto-reverse user without policy |
| Post fails after debit | user down | `{businessRef}:comp` credit user (orchestration) | Silent loss |
| Duplicate full payment | done | Idempotent 200 | Double debit |
| Merchant wallet missing | user debited | Provision merchant wallet + retry credit | — |
| LOCKED user wallet | — | `WALLET_LOCKED` | Debit |

### 7.3 Transfer

| Failure | Wallet state | Recovery | Forbidden |
|---------|--------------|----------|-----------|
| Insufficient A | — | Reject | Debit A |
| A debited, B credit fails | A down | Retry `TRANSFER_CREDIT` | Debit A again |
| Post fails after A debit | A down | Compensating credit A | — |
| A = B same member | product reject | — | — |
| Wrong gross on debit | — | 409 if ref reused with new amount | — |

### 7.4 Withdraw (critical — double spend)

| Failure | Wallet state | Recovery | Forbidden |
|---------|--------------|----------|-----------|
| Insufficient for freeze | — | Reject accept | Partial freeze |
| Freeze OK, payout unknown | frozen | **Poll bank**; do NOT release on timeout | RELEASE on timeout |
| Bank reject (terminal) | frozen | `WITHDRAW_RELEASE` | SETTLE |
| Bank success, settle fails | frozen, bank paid | Retry `{ref}:settle` | Second bank pay |
| Duplicate freeze | frozen | Idempotent | Double freeze amount |
| SETTLE after RELEASE | — | State machine + sub-keys prevent | Debit available twice |
| LOCKED during freeze | — | Reject | — |

### 7.5 IBFT

Same failure class as §7.4 with `IBFT_*` tx_types. Frozen gross includes fee; net to beneficiary is accounting/bank — wallet only holds member leg.

### 7.6 Partner / payroll / disbursement

| Failure | Recovery |
|---------|----------|
| Insufficient MERCHANT/PARTNER | Reject batch leg |
| Partial batch failure | Per-recipient idempotency `{ref}:{recipientId}`; retry or release |
| Duplicate batch ref | Idempotent summary |

### 7.7 QR/POS / EOD

| Case | Wallet |
|------|--------|
| No per-txn movement | No `wallet_tx` until product defines EOD credit |
| `MERCHANT_SETTLE_CREDIT` | Idempotent on `(merchant, settlementDate)` key — product-defined `business_ref` |

---

## 8. Invariants

| # | Invariant |
|---|-----------|
| W1 | `available >= 0`, `frozen >= 0` |
| W2 | Balance change ⟺ one new `wallet_tx` same TX |
| W3 | Replay same triple → same `wallet_tx.id`, no balance effect |
| W4 | No `coa_*` access |
| W5 | `SUM(available+frozen)` by lane ≈ control account (timing tolerance) |

---

## 9. Edge cases

| Case | Behavior |
|------|----------|
| Concurrent debits on last cent | One wins; other `WALLET_INSUFFICIENT_BALANCE` |
| Optimistic version conflict | Retry command |
| Credit to CLOSED wallet | Reject or ops policy |
| FREEZE then LOCKED | Settle/release policy — v1: reject new freeze if LOCKED |
| Out-of-order credit command | Dedup `business_ref`; ignore if already credited |
| Scale / currency | v1 VND, scale 4; reject else |
| Replay `:settle` after settle | Idempotent no-op |

---

## 10. Integration (pointers)

| Surface | Role |
|---------|------|
| Orchestration | Sole caller of wallet services |
| Kafka `core.accounting.journal-posted` | May trigger deposit credit path |
| Kafka `core.wallet.credit-command` | Explicit credit |
| RabbitMQ `WALLET_CREDIT` | Command envelope |
| Kafka `core.wallet.credited` | Emitted after commit |
| S1 public API | **Not** in wallet module |

Index: [`integration-surfaces.md`](../integration-surfaces.md) §5–6.

---

## 11. Reconciliation (W5)

```
SUM(available + frozen) GROUP BY wallet_type  vs  accounting control 2110/2120/2130
```

| Drift | Action |
|-------|--------|
| Within SLA window (async lag) | Log |
| Beyond tolerance | Alert ops; **do not** write COA from wallet |
| Missing deposit credit | Ops + retry consumer |

---

## 12. Error codes (wallet layer)

| Code | When |
|------|------|
| `WALLET_INSUFFICIENT_BALANCE` | debit/freeze over available |
| `WALLET_NOT_FOUND` | no wallet row |
| `WALLET_LOCKED` | mutation on LOCKED |
| `WALLET_DUPLICATE_CONFLICT` | same triple, different amount |

Mapped to HTTP via orchestration ([`spec/contracts/async-api/core-events.yaml`](../spec/contracts/async-api/core-events.yaml) ErrorCode).

---

## 13. Open decisions

| # | Question |
|---|----------|
| W-O1 | ~~Credit LOCKED wallet?~~ **Closed** — reject; unlock + retry ([ADR-034](../adr/ADR-034-locked-wallet-deposit-credit-reject.md)) |
| W-O2 | MERCHANT_SETTLE_CREDIT mandatory or optional at EOD? |
| W-O3 | PARTNER lane v1 in production or internal-only? |
| W-O4 | Read-through cache for balance — yes/no v1? |

---

## 14. Related docs

| Need | Read |
|------|------|
| DDL / FR detail | [`core.wallet.md`](../core.wallet.md) |
| Ledger side | [`accounting.md`](./accounting.md) Part II |
| Per-use-case depth | This doc Part II §15–28 |
| Given/When/Then | [`acceptance.md`](./acceptance.md), [`core.acceptance-specs.md`](../core.acceptance-specs.md) |
| Industry references | [`references/README.md`](../references/README.md) § Wallet |
| Idempotency ADR | [ADR-005](../adr/ADR-005-idempotency-key-strategy.md) |

---

# Part II — Per use case (wallet-only depth)

Balance assertions only — ledger observables in [`accounting.md`](./accounting.md) Part II.

---

## 15. DEPOSIT (wallet leg)

### 15.1 Trigger

Orchestration invokes credit **only** after `coa_trans.status = POSTED` (event, command, or direct call).

### 15.2 Command shape (design)

| Field | Rule |
|-------|------|
| `memberId` | Mapped from VA |
| `walletType` | USER |
| `amount` | **net** — orchestration computed |
| `businessRef` | Bank txn id |
| `txType` | `DEPOSIT_CREDIT` |
| `coaTransId` | Correlation |

### 15.3 Balance observables

| After credit | Assert |
|--------------|--------|
| `available` | += net |
| `frozen` | unchanged |
| `wallet_tx` | 1 row CREDIT, amount=net |

### 15.4 Failure rows (wallet module)

| ID | Condition | Outcome |
|----|-----------|---------|
| WD-F1 | POSTED not verified | Reject at orchestration gate — wallet may still reject if called early |
| WD-F2 | Duplicate triple | Return existing `wallet_tx_id` |
| WD-F3 | amount ≠ prior same ref | 409 |
| WD-F4 | Wallet not found | Create on first credit (product) or reject |
| WD-F5 | LOCKED | Reject — ADR-034 |

---

## 16. PAYMENT (wallet legs)

### 16.1 Leg 1 — DEBIT payer

| Assert | |
|--------|--|
| `available` | −gross |
| `tx_type` | `PAYMENT_DEBIT` |
| Insufficient | `WALLET_INSUFFICIENT_BALANCE`, no row |

### 16.2 Leg 2 — CREDIT merchant

| Assert | |
|--------|--|
| `available` | +netToMerchant |
| `tx_type` | `PAYMENT_CREDIT` |
| Same `businessRef` | OK — different `tx_type` |

### 16.3 Compensation leg

| When | Command |
|------|---------|
| Post fails after debit | `credit(USER, gross, {ref}:comp, ADJUSTMENT_CREDIT or PAYMENT reversal type per policy)` |

---

## 17. TRANSFER (wallet legs)

| Leg | Member | Amount | `tx_type` |
|-----|--------|--------|-----------|
| 1 | A | gross | `TRANSFER_DEBIT` |
| 2 | B | net | `TRANSFER_CREDIT` |

Both must succeed for happy path; if leg 2 fails, retry leg 2 only. If post never happened, compensate leg 1.

---

## 18. WITHDRAW (freeze / settle / release)

### 18.1 State machine (per wallet)

```
AVAILABLE --FREEZE--> FROZEN_HOLD --SETTLE--> (frozen reduced)
                  \--RELEASE--> AVAILABLE restored
```

### 18.2 Observables per step

| Step | `available` | `frozen` | `tx_type` |
|------|-------------|----------|-----------|
| FREEZE | −gross | +gross | `WITHDRAW_FREEZE` |
| SETTLE | unchanged | −gross | `WITHDRAW_SETTLE` (DEBIT frozen) |
| RELEASE | +gross | −gross | `WITHDRAW_RELEASE` |

### 18.3 Invariants during in-flight

| Rule | |
|------|--|
| `available + frozen` | equals pre-freeze total until SETTLE |
| SETTLE after RELEASE | **Forbidden** — orchestration state machine |
| Timeout | frozen unchanged until RELEASE or SETTLE |

---

## 19. IBFT (wallet legs)

**Accounting reference:** [`accounting.md`](./accounting.md) §18 (transit **3400**, Napas **1112**). Wallet models **member leg only** — no Napas wire, no **5100** / **4130** (accounting).

### 19.1 Trigger

Orchestration returns **200 accept** only after `IBFT_FREEZE` succeeds. Bank/Napas payout is async (same class as withdraw §18).

### 19.2 Command shape

| Field | Rule |
|-------|------|
| `memberId` | Payer USER wallet |
| `amount` | **Gross** (principal + platform fee) — orchestration passes total hold |
| `businessRef` | S1 idempotency key |
| `txType` | `IBFT_FREEZE` → `IBFT_SETTLE` or `IBFT_RELEASE` |
| Sub-keys | `{businessRef}:settle`, `{businessRef}:release` (D5) |

Beneficiary bank account and Napas message id are **orchestration/bank adapter** — wallet stores no beneficiary fields.

### 19.3 State machine

Same diagram as §18.1 — replace `WITHDRAW_*` with `IBFT_*`.

### 19.4 Balance observables

| Step | `available` | `frozen` | `tx_type` |
|------|-------------|----------|-----------|
| FREEZE | −gross | +gross | `IBFT_FREEZE` |
| SETTLE | unchanged | −gross | `IBFT_SETTLE` |
| RELEASE | +gross | −gross | `IBFT_RELEASE` |

Net to beneficiary and fee revenue are **not** wallet columns — accounting posts those on bank success.

### 19.5 Failure matrix (wallet module)

| ID | Condition | Wallet outcome | Forbidden |
|----|-----------|----------------|-----------|
| I-W1 | Insufficient `available` at accept | Reject freeze | Partial freeze |
| I-W2 | Duplicate `IBFT_FREEZE` | Idempotent same `wallet_tx_id` | Double hold |
| I-W3 | Bank timeout (status unknown) | **Frozen unchanged** | `IBFT_RELEASE` on timeout alone |
| I-W4 | Bank terminal reject | `IBFT_RELEASE` | `IBFT_SETTLE` |
| I-W5 | Bank success, settle command fails | Retry `{ref}:settle` | Second bank pay |
| I-W6 | SETTLE after RELEASE | Reject / no-op per state | Debit `available` twice |
| I-W7 | `LOCKED` wallet at freeze | `WALLET_LOCKED` | Freeze |
| I-W8 | Same ref, different gross on replay | `WALLET_DUPLICATE_CONFLICT` 409 | Apply wrong amount |

**Acceptance:** Feature IBFT (5 scenarios) — [`acceptance.md`](./acceptance.md).

---

## 20. MERCHANT / PARTNER batch legs

| `tx_type` | Lane | When |
|-----------|------|------|
| `PAYROLL_DEBIT` | MERCHANT | Batch submit — gross |
| `DISBURSEMENT_DEBIT` | PARTNER | Per disbursement |
| `PARTNER_PREFUND_CREDIT` | PARTNER | Pre-fund |
| `MERCHANT_SETTLE_CREDIT` | MERCHANT | EOD optional |

Partial batch: failed recipient does not roll back succeeded `wallet_tx` rows — orchestration issues per-recipient keys.

### 20.1 PAYROLL (`PAYROLL_DEBIT`)

| Field | Rule |
|-------|------|
| Lane | `MERCHANT` |
| Amount | **Gross** batch (salaries + fee) from orchestration |
| `businessRef` | Batch id; per-recipient sub-keys `{ref}:{recipientId}` if product splits legs |

| Failure | Wallet outcome |
|---------|----------------|
| Insufficient merchant `available` | Reject — no partial debit unless product defines slice amounts |
| Duplicate batch ref + same `tx_type` | Idempotent |
| One recipient fails (orchestration) | Other recipients' wallet rows **not** rolled back by wallet |

**Accounting:** [`accounting.md`](./accounting.md) §20 (transit **3600**).

### 20.2 PARTNER pre-fund & disbursement

| `tx_type` | Direction | When |
|-----------|-----------|------|
| `PARTNER_PREFUND_CREDIT` | CREDIT | Partner escrow top-up — accounting **2130** posted |
| `DISBURSEMENT_DEBIT` | DEBIT | Per disbursement gross |

| Failure | Wallet outcome |
|---------|----------------|
| Insufficient PARTNER `available` | Reject leg |
| Duplicate `{ref}:{recipientId}` | Idempotent |
| Partial batch | Same as payroll — no automatic rollback |

**Accounting:** [`accounting.md`](./accounting.md) §21 (transit **3700**).

---

## 21. Concurrency model

### 21.1 Problem

Concurrent debits, freezes, or transfer legs on the **same** `wallet_id` must not drive `available` negative ([`stackoverflow-optimistic-locking-funds.md`](../references/stackoverflow-optimistic-locking-funds.md)). Cross-wallet transfer touches **two** wallets — no global lock across both.

### 21.2 v1 decision: optimistic locking on `wallet_balance.version`

| Aspect | Choice |
|--------|--------|
| Primary mechanism | `UPDATE wallet_balance SET …, version = version + 1 WHERE wallet_id = ? AND version = ?` |
| On `version` mismatch | Return retryable conflict; orchestration retries with backoff |
| Read path | `getBalance` — no lock; may be stale by one in-flight TX |
| Transfer | Debit wallet A, then credit wallet B — **sequential** commands; if B fails, compensate A (orchestration) |

**Why not pessimistic `SELECT … FOR UPDATE` as default:** acceptable for v1 at moderate QPS per wallet; optimistic avoids long-held row locks on hot merchant wallets ([`moderntreasury-locking.md`](../references/moderntreasury-locking.md)). Escalate to pessimistic per-wallet if profiling shows excessive version conflicts on a single `wallet_id`.

### 21.3 Hot-wallet mitigation (optional, W-O4)

| Technique | Owner | Wallet module |
|-----------|-------|---------------|
| Read-through cache for balance | Application | Not in v1 domain — cache invalidates on write |
| Per-member command serialization | Orchestration | Single-thread executor keyed by `wallet_id` |
| Sharded wallet rows | Infra | One row per member already isolates contention |

### 21.4 Forbidden patterns

| Pattern | Why |
|---------|-----|
| Cross-wallet single DB transaction | Violates bounded context; use saga |
| `available` check outside TX then update later | TOCTOU double spend |
| Retry debit with **new** `business_ref` after timeout | Use same triple for idempotency |

---

## 22. Wallet ↔ orchestration contract

| Orchestration must pass | Wallet must not |
|-------------------------|-----------------|
| Exact amount per leg | Recompute fee |
| Valid `tx_type` enum | Infer from HTTP |
| `businessRef` + sub-keys | Generate ref |
| POSTED gate for deposit | Query `coa_trans` |

---

## 23. Scenario index

| Use case | Acceptance feature |
|----------|-------------------|
| Deposit | 18 scenarios |
| Withdraw | 12 |
| Payment | 10 |
| Transfer | 6 |
| IBFT | 5 |
| Idempotency / lock / recon | 10+ |

Full list: [`acceptance.md`](./acceptance.md).

---

## 24. QR/POS (wallet leg — optional per product)

**Accounting reference:** [`accounting.md`](./accounting.md) §19 (capture via transit **3500**; EOD **3800+**).

### 24.1 Default v1

| Rule | Behavior |
|------|----------|
| Per capture txn | **No** `wallet_tx` — accounting records **1113** / **2120** / **3500** only |
| Merchant spendable balance | Unchanged until product defines an explicit wallet credit |

QR/POS acquirer settlement to bank does not require a merchant wallet movement for ledger correctness.

### 24.2 Optional: credit merchant wallet on capture

If product opts in, orchestration may issue `PAYMENT_CREDIT`-class or dedicated credit after accounting POSTED capture:

| Assert | |
|--------|--|
| `available` | += netToMerchant (from orchestration) |
| `businessRef` | Acquirer capture id — idempotent |
| `tx_type` | Product-defined; default pattern mirrors `PAYMENT_CREDIT` on MERCHANT lane |

### 24.3 Failure matrix

| ID | Condition | Wallet outcome |
|----|-----------|----------------|
| Q-W1 | Duplicate capture ref | Idempotent return existing row |
| Q-W2 | Amount mismatch on replay | 409 |
| Q-W3 | MERCHANT wallet missing | Provision + retry (orchestration) |
| Q-W4 | Accounting post not POSTED | Orchestration gate — wallet not called |

---

## 25. EOD SETTLEMENT (merchant wallet leg — optional)

**Accounting reference:** [`accounting.md`](./accounting.md) §22 (lock **2120** → **3800** / **3810** / **3820**).

### 25.1 Default v1

EOD moves aggregate merchant liability to bank in accounting. Wallet **does not** auto-sync unless product enables bulk merchant credit/debit at settlement time (W-O2).

### 25.2 Optional: `MERCHANT_SETTLE_CREDIT`

| Field | Rule |
|-------|------|
| `tx_type` | `MERCHANT_SETTLE_CREDIT` |
| `amount` | Net settlement amount from orchestration (after MDR) |
| `businessRef` | `{merchantId}:{settlementDate}` or file id — product-defined |
| Lane | `MERCHANT` |

| Step | Assert |
|------|--------|
| After credit | `available` += net |
| Idempotent | Same `(merchant, date)` ref → same `wallet_tx_id` |

### 25.3 Failure matrix

| ID | Condition | Wallet outcome |
|----|-----------|----------------|
| E-W1 | Accounting EOD blocked (file mismatch) | No wallet command — orchestration holds |
| E-W2 | Duplicate settlement ref | Idempotent |
| E-W3 | Amount ≠ prior same ref | 409 |
| E-W4 | Credit after accounting exception reversal | Orchestration policy — may require compensating debit (new ref) |

---

## 26. Cross-cutting wallet operations

### 26.1 Balance query semantics

| API field | Source | Semantics |
|-----------|--------|-----------|
| `available` | `wallet_balance.available` | Spendable now (§2.1) |
| `frozen` | `wallet_balance.frozen` | Held for withdraw/IBFT |
| `status` | `wallet.status` | `ACTIVE` / `LOCKED` / `CLOSED` |

Do not expose a synthetic "pending deposit" field from wallet — derive from orchestration/ledger status if product requires.

### 26.2 History (`wallet_tx`)

Append-only log is the audit source for per-member movements. Ops corrections use new rows (`ADJUSTMENT_*`) with new `business_ref` — never UPDATE historical rows (ADR-001 spirit for wallet).

### 26.3 Reconciliation interaction (W5)

Batch job compares `SUM(available + frozen)` by `wallet_type` to accounting control API. Wallet module **never** writes correcting entries to COA on drift — ops + accounting reversal/adjustment only.

### 26.4 Blnk-style inflight (informative)

If comparing to Blnk [`inflight_balance`](../references/blnk-balances-intro.md): project `frozen` ≈ inflight debit exposure on outflows; there is no separate inflight credit column — inbound deposit before wallet credit is **zero** in `wallet_*`.

---

## 27. Reference synthesis — wallet (`references/` full)

### 27.1 Balance types & spendable money

| Reference | Lesson | § |
|-----------|--------|---|
| [`moderntreasury-balance-types-part-i.md`](../references/moderntreasury-balance-types-part-i.md) | posted / pending / available | §2.1 |
| [`relayfi-pending-vs-available.md`](../references/relayfi-pending-vs-available.md) | Available excludes pending outflows | `available` |
| [`blnk-balances-intro.md`](../references/blnk-balances-intro.md) | inflight + queued attrs | §26.4 |
| [`blnk-balance-snapshots.md`](../references/blnk-balance-snapshots.md), [`blnk-historical-balances.md`](../references/blnk-historical-balances.md) | Point-in-time history | `wallet_tx` audit |
| [`blnk-balance-monitoring.md`](../references/blnk-balance-monitoring.md) | Balance alerts | **Gap** — ops |

### 27.2 Holds, freeze, double-spend

| Reference | Lesson | § |
|-----------|--------|---|
| [`mambu-transaction-holds.md`](../references/mambu-transaction-holds.md) | Hold → settle / reverse | §4.1 |
| [`mambu-authorization-holds.md`](../references/mambu-authorization-holds.md) | Card auth holds | Out of scope v1 |
| [`blnk-transaction-lifecycle.md`](../references/blnk-transaction-lifecycle.md) | INFLIGHT / VOID | §4.1, §18–19 |
| [`medium-double-spend-fintech.md`](../references/medium-double-spend-fintech.md) | Freeze before debit | §7.4, ADR-007 |

### 27.3 Concurrency

| Reference | Lesson | § |
|-----------|--------|---|
| [`moderntreasury-locking.md`](../references/moderntreasury-locking.md) | Optimistic vs pessimistic | §21 |
| [`devto-locking-strategies.md`](../references/devto-locking-strategies.md) | Locking overview | §21 |
| [`stackoverflow-optimistic-locking-funds.md`](../references/stackoverflow-optimistic-locking-funds.md) | Funds locking | §21.4 |

### 27.4 Wallet product & P2P

| Reference | Lesson | § |
|-----------|--------|---|
| [`moderntreasury-how-to-build-digital-wallet.md`](../references/moderntreasury-how-to-build-digital-wallet.md) | Wallet on ledger | §1 |
| [`moderntreasury-learn-digital-wallet.md`](../references/moderntreasury-learn-digital-wallet.md), [`moderntreasury-digital-wallet-tutorial.md`](../references/moderntreasury-digital-wallet-tutorial.md), [`moderntreasury-what-is-digital-wallet.md`](../references/moderntreasury-what-is-digital-wallet.md) | Wallet education | §5 |
| [`moderntreasury-fx-wallets-tutorial.md`](../references/moderntreasury-fx-wallets-tutorial.md) | FX wallets | Out of scope ADR-019 |
| [`finlego-wallet-as-a-service.md`](../references/finlego-wallet-as-a-service.md) | WaaS | `wallet_*` |
| [`medium-codefarm-digital-wallet-system.md`](../references/medium-codefarm-digital-wallet-system.md) | Paytm-style | §15–20 |
| [`tianpan-designing-paypal-transfer.md`](../references/tianpan-designing-paypal-transfer.md) | PayPal P2P dedup | §17 |
| [`crossmint-wallet-architecture-fintech.md`](../references/crossmint-wallet-architecture-fintech.md) | Wallet architecture | ADR-002 |

### 27.5 Settlement & VA

| Reference | Lesson | § |
|-----------|--------|---|
| [`stripe-payment-settlement-explained.md`](../references/stripe-payment-settlement-explained.md) | Settlement timing | §18–19 |
| [`moderntreasury-virtual-accounts.md`](../references/moderntreasury-virtual-accounts.md) | VA mapping | ADR-030 |
| [`increase-api-accounts.md`](../references/increase-api-accounts.md) | FBO API | ADR-030 |

### 27.6 Slope P3 (pending) → project model

[`medium-slope-payments-ledger-pitfalls.md`](../references/medium-slope-payments-ledger-pitfalls.md): industry uses separate pending:debit / pending:credit sub-accounts. **We use** `wallet.frozen` + accounting `PENDING`/3100 — fewer columns, same overspend prevention. See also [`accounting.md`](./accounting.md) §27 pitfalls.

---

## 28. Reference gaps (refs → chưa spec)

| Topic | Reference | Status |
|-------|-----------|--------|
| Balance monitoring | `blnk-balance-monitoring.md` | [ADR-032](../adr/ADR-032-wallet-balance-monitoring.md) |
| LOCKED deposit credit (W-O1) | wallet §12 | [ADR-034](../adr/ADR-034-locked-wallet-deposit-credit-reject.md) |
| Read-model cache | MT scale IV–V | W-O4 optional |
| Thought Machine | `medium-thought-machine-vault-core.md` | Informative only |
| Stripe Treasury | blocked scrape | N/A |

---

## 29. Related docs (Part II)

| Need | Read |
|------|------|
| DDL / FR detail | [`core.wallet.md`](../core.wallet.md) |
| Ledger synthesis | [`accounting.md`](./accounting.md) §26–27 |
| Orchestration synthesis | [`orchestration.md`](./orchestration.md) §25 |
| ADR AC/TC | [`adr/README.md`](../adr/README.md) |
| Full ref index (108 files) | [`references/README.md`](../references/README.md) |
| Gherkin | [`acceptance.md`](./acceptance.md) Part IV |
