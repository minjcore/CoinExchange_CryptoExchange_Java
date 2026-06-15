# core.wallet — Wallet Service Design (TRD)

**Author:** Cao Khang Đoàn  
**Last updated:** 2026-06-04  
**Scope:** `10_core/` — `core.wallet`. Terms: [`TERMINOLOGY.md`](./TERMINOLOGY.md). **Status:** Design only — not implemented in repo.

**Related:** [`integration-surfaces.md`](./integration-surfaces.md) (HTTP/Kafka index), [`core.foundation.md`](./core.foundation.md) (Part I §3 boundary, Part II §8–16 ledger), [`core.accounting.trd.md`](./core.accounting.trd.md), [`openapi/README.md`](./openapi/README.md).  
**ADR:** [ADR-002 — `core.foundation` shared library](./adr/ADR-002-core-foundation-shared-library.md).

**Integration:** Surface matrix and orchestration step order → [`integration-surfaces.md`](./integration-surfaces.md) §4. `wallet_*` detail → this doc; DR/CR → [`core.foundation.md`](./core.foundation.md) Part II §8+.

---

## 1. Overview

### 1.1 Purpose

`core.wallet` is the **runtime source of truth for spendable balance per member** (user, merchant, partner). It answers: *how much can this member spend right now?* via `available` and `frozen`.

Accounting (`core.accounting`) records **aggregate** liabilities on COA **2110 / 2120 / 2130**. The wallet records **per-member** balances in `wallet_*`. The two stay aligned via orchestration and reconciliation jobs — not shared tables or repositories.

### 1.2 Goals

**Business**

- Fast balance reads for payment, transfer, withdrawal
- Clear audit trail for every balance change
- USER / MERCHANT / PARTNER lanes and VND (v1)

**Technical**

- One DB transaction per balance change (no ad-hoc column updates outside a transaction)
- Idempotent writes on `business_ref`
- `wallet_tx` is append-only; no UPDATE on historical movements
- No compile-time dependency on `core.accounting` and no DB join to `coa_*`

### 1.3 Out of scope (wallet module)

| Out of scope | Owner |
|--------------|-------|
| Ledger posting, DR/CR lines, transit accounts | `core.accounting` |
| Bank webhooks, public HTTP, Kafka bindings | Application / integration layer |
| Fee revenue recognition, EOD settlement | `core.accounting` ([`core.foundation.md`](./core.foundation.md) Part II §16) |
| Crypto / on-chain custody | Not in fiat v1 design |

---

## 2. Architecture placement

```
     Application (orchestration, HTTP/Kafka)
              │
              │  commands only — GW must not SQL wallet_* directly
              ▼
         core.wallet
    wallet, wallet_balance, wallet_tx
              │
              ▼
       core.foundation  (envelope, errors — v1)
              │
              ▼
             JDK
```

**Rules** ([`core.foundation.md`](./core.foundation.md) Part I §3, [ADR-002](./adr/ADR-002-core-foundation-shared-library.md)):

- `core.wallet` **must not** import `core.accounting`, touch `coa_*`, or read `coa_trans`.
- Sync with accounting: **after** `coa_trans.status = POSTED` (deposit credit) or **within orchestration transaction boundaries** in the order Application defines (sync pay/transfer) — the wallet never posts the ledger.
- May import **`core.foundation`** only for shared types (errors, paging) — see [`core.foundation.md`](./core.foundation.md) Part I §5 (no command DTOs in foundation v1).

---

## 3. Domain model (`wallet_*`)

Prefix **`wallet_`** — mirrors accounting **`coa_`** ([`TERMINOLOGY.md`](./TERMINOLOGY.md)).

### 3.1 Entity relationships

```
member ──► wallet (1..n per lane × currency)
              │
              ├── wallet_balance (1:1)
              └── wallet_tx (1:n, append-only)
```

### 3.2 Table: `wallet`

One row = one **wallet instance** (member + lane + currency).

| Column | Type | Constraint | Notes |
|--------|------|------------|-------|
| `id` | BIGINT / UUID | PK | Surrogate key |
| `member_id` | BIGINT | NOT NULL, indexed | Platform member |
| `wallet_type` | ENUM | NOT NULL | `USER`, `MERCHANT`, `PARTNER` |
| `currency` | CHAR(3) | NOT NULL | v1: `VND` |
| `status` | ENUM | NOT NULL | `ACTIVE`, `LOCKED`, `CLOSED` |
| `created_at` | TIMESTAMP | NOT NULL | |
| `updated_at` | TIMESTAMP | NOT NULL | |

**UNIQUE:** `(member_id, wallet_type, currency)`.

**COA control account mapping (reconciliation only — no FK stored):**

| `wallet_type` | COA control (aggregate) |
|---------------|-------------------------|
| `USER` | 2110 |
| `MERCHANT` | 2120 |
| `PARTNER` | 2130 |

### 3.3 Table: `wallet_balance`

Current snapshot; updated only in the same transaction as a new `wallet_tx` insert.

| Column | Type | Constraint | Notes |
|--------|------|------------|-------|
| `wallet_id` | FK → `wallet.id` | PK | 1:1 with `wallet` |
| `available` | DECIMAL(19,4) | NOT NULL, ≥ 0 | Spendable |
| `frozen` | DECIMAL(19,4) | NOT NULL, ≥ 0 | Held (e.g. withdraw in flight) |
| `version` | BIGINT | NOT NULL | Optimistic lock |
| `updated_at` | TIMESTAMP | NOT NULL | |

**Invariant:** `available + frozen` = economic exposure for that wallet instance (product may expose both fields separately on API).

### 3.4 Table: `wallet_tx`

Append-only movement log. **Never UPDATE** amount or direction after insert; corrections = compensating `wallet_tx` with a new `business_ref` (orchestration-owned).

| Column | Type | Constraint | Notes |
|--------|------|------------|-------|
| `id` | BIGINT | PK | Exposed as `walletTxId` on API/events |
| `wallet_id` | FK | NOT NULL, indexed | |
| `tx_type` | ENUM | NOT NULL | See §5.2 |
| `direction` | ENUM | NOT NULL | `CREDIT`, `DEBIT`, `FREEZE`, `UNFREEZE` |
| `amount` | DECIMAL(19,4) | NOT NULL, > 0 | Always positive; direction carries sign semantics |
| `available_after` | DECIMAL | NOT NULL | Snapshot after apply |
| `frozen_after` | DECIMAL | NOT NULL | Snapshot after apply |
| `business_ref` | VARCHAR(128) | NOT NULL | Idempotency key (= `X-Idempotency-Key` / `businessRef`) |
| `coa_trans_id` | BIGINT | NULL | Correlation only — **no enforced FK to `coa_trans`** across modules |
| `use_case` | VARCHAR(32) | NULL | e.g. `DEPOSIT`, `PAYMENT` — mirrors ledger `use_case` |
| `remark` | VARCHAR(512) | NULL | Ops / support |
| `created_at` | TIMESTAMP | NOT NULL | |

**UNIQUE:** `(wallet_id, business_ref, tx_type)` — locked in [`IMPLEMENTATION.md`](./IMPLEMENTATION.md) §2 D1–D5.

### 3.5 Java package (target)

```
core.wallet
├── domain      Wallet, WalletBalance, WalletTx, WalletType, WalletTxType
├── repository  WalletRepository, WalletBalanceRepository, WalletTxRepository
├── service     WalletCommandService, WalletQueryService
└── (no controller, no Kafka listener — Application calls services)
```

---

## 4. Functional requirements

### FR-1 Provision wallet

On first need (or member onboarding hook from Application):

1. Insert `wallet` + `wallet_balance` with zero amounts.
2. Idempotent on `(member_id, wallet_type, currency)`.

### FR-2 Query balance

- Input: `memberId`, `walletType`, `currency`.
- Output: `available`, `frozen`, `status` — aligns with public OpenAPI `WalletBalanceData` (schema names in [`openapi/gtelpay-public.yaml`](./openapi/gtelpay-public.yaml); wire spec is integration docs).

### FR-3 Credit

- Increase `available` by `amount`.
- Insert `wallet_tx` (`direction = CREDIT`).
- **Deposit path:** only after orchestration confirms ledger **POSTED** ([`core.foundation.md`](./core.foundation.md) Part II §8).
- Duplicate `business_ref`: return existing `wallet_tx` / `walletTxId` — no second credit.

### FR-4 Debit

- Require `available >= amount`.
- Decrease `available`; insert `wallet_tx` (`DEBIT`).
- Insufficient funds → wallet-layer error (orchestration maps to 4xx on public API).

### FR-5 Freeze / unfreeze

- **Freeze:** `available -= amount`, `frozen += amount` (same transaction).
- **Unfreeze:** reverse; used for withdraw pipeline before bank payout completes.
- Each step → `wallet_tx` with `FREEZE` / `UNFREEZE`.

### FR-6 Lock wallet

- Set `wallet.status = LOCKED` — reject debit/freeze (credit policy may vary; v1: reject all changes except admin unlock).

### FR-7 History (optional v1.1)

- Page `wallet_tx` by `wallet_id` / `member_id` + time range — use `PageResult` from foundation.

---

## 5. Use cases — wallet branch only

Ledger lines: [`core.foundation.md`](./core.foundation.md) Part II §8–16. Below is **only** what `core.wallet` does when Application calls it.

### 5.1 Deposit (fiat, async recommended)

```
Orchestration: ledger PENDING → POSTED (accounting)
  → JournalPosted OR WalletCreditCommand
  → core.wallet: credit USER wallet (net after fee — amount from command; wallet does not recompute fee)
```

| Step | Wallet action | When |
|------|---------------|------|
| A | None | Ledger `PENDING` — funds in transit **3100**, not in member wallet yet |
| B | `credit(member, USER, netAmount, businessRef, coaTransId)` | Ledger `POSTED` |

Example from foundation §8: user receives **99,000** net on 100,000 deposit — orchestration passes `amount = 99000` in `WalletCreditCommand`; wallet does not split fees.

**Idempotency:** same `business_ref` as bank / ledger `business_ref`.

### 5.2 Payment (wallet pay — sync)

Orchestration order (Application — see [`openapi/gtelpay-public.yaml`](./openapi/gtelpay-public.yaml) `createPayment`):

1. **Debit** user `available` (gross `amount` from request).
2. Accounting: create journal → lines → **POSTED**.
3. **Credit** merchant `available` (`netToMerchant` or default `amount`).

All three steps are orchestrated by Application; one S1 `businessRef` + distinct `tx_type` per leg — see [`IMPLEMENTATION.md`](./IMPLEMENTATION.md) §2 D2.

| `tx_type` | Lane | Direction |
|-----------|------|-----------|
| `PAYMENT_DEBIT` | USER | DEBIT |
| `PAYMENT_CREDIT` | MERCHANT | CREDIT |

### 5.3 Internal transfer (A → B, sync)

1. Debit A (USER), credit B (USER) — same `business_ref` family or linked refs.
2. After ledger POSTED for transit **3300** (accounting §10).

Wallet does not model transit; member legs only.

### 5.4 Withdraw (wallet branch sync + bank async)

1. **Freeze** user gross (principal + fee) before orchestration returns **200** accept — v1 holds on accept, **not** immediate debit ([`IMPLEMENTATION.md`](./IMPLEMENTATION.md) §2 D5).
2. Bank payout async. On success → **settle** (debit from frozen); on failure / cancel → **release** (unfreeze). Wallet never debits twice.

| `tx_type` | Direction | When |
|-----------|-----------|------|
| `WITHDRAW_FREEZE` | FREEZE | Accept — hold principal + fee |
| `WITHDRAW_SETTLE` | DEBIT | Bank success — deduct from frozen |
| `WITHDRAW_RELEASE` | UNFREEZE | Payout failed / cancel |

Enum values are locked in [`IMPLEMENTATION.md`](./IMPLEMENTATION.md) §2.1 — there is **no** `WITHDRAW_DEBIT`.

### 5.5 Interbank transfer (IBFT, bank async)

Like withdraw, the bank leg is async, so the wallet **freezes on accept** then settles/releases ([`core.foundation.md`](./core.foundation.md) Part II §11; ledger transit **3400**).

1. **Freeze** user gross (principal + fee) before orchestration returns **200** accept.
2. Napas/bank payout async. On success → **settle** (debit from frozen); on failure / cancel → **release** (unfreeze).

| `tx_type` | Direction | When |
|-----------|-----------|------|
| `IBFT_FREEZE` | FREEZE | Accept — hold principal + fee |
| `IBFT_SETTLE` | DEBIT | Bank success — deduct from frozen |
| `IBFT_RELEASE` | UNFREEZE | Payout failed / cancel |

Wallet models the member leg only; the bank cost (5100) and fee revenue (4130) are accounting-side ([`core.foundation.md`](./core.foundation.md) §11).

### 5.6 Partner lane (`PARTNER`)

Partner escrow / disbursement pre-fund ([`core.foundation.md`](./core.foundation.md) Part II §15): credit/debit **PARTNER** wallet when orchestration issues commands — same FR rules.

| `tx_type` | Direction | When |
|-----------|-----------|------|
| `PARTNER_PREFUND_CREDIT` | CREDIT | Partner escrow top-up (2130) |
| `DISBURSEMENT_DEBIT` | DEBIT | Disbursement batch — gross (principal + fee) |

### 5.7 Merchant lane batches

| `tx_type` | Direction | When |
|-----------|-----------|------|
| `PAYROLL_DEBIT` | DEBIT | Payroll batch — gross (salaries + fee), debits MERCHANT |
| `MERCHANT_SETTLE_CREDIT` | CREDIT | **Optional** — only if the product credits the merchant wallet during EOD settlement |

### 5.8 Outside wallet scope

| Flow | Wallet |
|------|--------|
| QR/POS acquirer settlement to bank | No per-txn wallet movement until product credits merchant wallet |
| EOD **2120 → bank** | Accounting §16 only; bulk merchant wallet adjustment if product requires — orchestration-defined |

---

## 6. Balance invariants

| # | Invariant |
|---|-----------|
| W1 | `available >= 0`, `frozen >= 0` always |
| W2 | Every `wallet_balance` change has exactly one new `wallet_tx` in the same DB transaction |
| W3 | Replay `business_ref` → same `wallet_tx.id`, no duplicate balance effect |
| W4 | Wallet services never mutate `coa_*` |
| W5 | Sum of `available` across USER (+ MERCHANT + PARTNER) per currency reconciles to COA control **2110+2120+2130** within tolerance (timing: async deposit, batch reconciliation) |

Reconciliation job (Application or ops): compare `SUM(wallet_balance.available + frozen)` by `wallet_type` to accounting control balance API — log drift; **do not** adjust COA from wallet.

---

## 7. Commands, events, and integration

### 7.1 Who may call wallet

| Caller | Allowed |
|--------|---------|
| Application orchestration | Yes — primary |
| API Gateway | **No** — no direct SQL or wallet module calls |
| `core.accounting` | **No** — emits `JournalPosted`; must not import wallet repo |
| External partner | **No** — public HTTP only via orchestration |

### 7.2 Service API (v1 — in `core.wallet`, not foundation)

Application calls domain services directly (illustrative method names):

| Operation | Maps to |
|-----------|---------|
| `credit(memberId, walletType, amount, businessRef, …)` | FR-3 |
| `debit(...)` | FR-4 |
| `freeze` / `unfreeze` | FR-5 |
| `getBalance(...)` | FR-2 |

HTTP/Kafka parsing stays in Application; payload shapes from OpenAPI / AsyncAPI.

### 7.3 Async messaging (Kafka + RabbitMQ)

| Transport | Channel | Direction | Wallet role |
|-----------|---------|-----------|-------------|
| Kafka | `core.accounting.journal-posted` | In (consumer) | Credit on `DEPOSIT` if orchestration uses event path |
| RabbitMQ | `core.commands.wallet-credit` | In (consumer) | Explicit credit command — envelope `commandType=WALLET_CREDIT` |
| Kafka | `core.wallet.credited` | Out | After `wallet_tx` commit |
| Kafka | `core.operations.command-failed` | Out | Failure fan-out |

Kafka payloads: [`asyncapi/core-events.yaml`](./asyncapi/core-events.yaml). RabbitMQ full-body envelope (`businessRef`, `memberId`, `messageId`, `payload`): [`asyncapi/core-commands.yaml`](./asyncapi/core-commands.yaml). Field `businessRef` = column `business_ref`.

### 7.4 Public HTTP

Paths and schemas: [`openapi/gtelpay-public.yaml`](./openapi/gtelpay-public.yaml); orchestration maps to wallet commands. Wallet module **does not** embed Spring MVC routes.

---

## 8. Idempotency and errors

| Situation | Behavior |
|-----------|----------|
| Duplicate `business_ref`, same effect | Return `walletTxId` + current balance (HTTP 200) |
| Duplicate `business_ref`, conflicting amount | Reject — `CONFLICT` / wallet error |
| Debit when `available` insufficient | Reject — no partial debit |
| Credit after POSTED retry | Safe no-op via idempotency |
| Ledger POSTED but wallet credit fails | Retry consumer; **do not** reverse ledger from wallet — ops reconciliation ([`core.foundation.md`](./core.foundation.md) Part II §8.5) |
| Wallet `LOCKED` | Reject debit/freeze |

**Error codes:** extend `core.foundation` `ErrorCode` — e.g. `WALLET_INSUFFICIENT_BALANCE`, `WALLET_NOT_FOUND`, `WALLET_LOCKED`, `WALLET_DUPLICATE_CONFLICT`.

---

## 9. Concurrency and storage

- **Row lock:** `SELECT … FOR UPDATE` on `wallet_balance` (or optimistic `version` with retry) per command transaction.
- **DB:** PostgreSQL recommended (ACID, aligned with accounting TRD).
- **Cache:** optional read-through for balance — invalidate on write; **DB remains source of truth**.
- **Scale:** shard key = `member_id` for Kafka consumer affinity.

---

## 10. Alignment with accounting TRD

| TRD / API term | Wallet term |
|----------------|-------------|
| `reference_id` | `business_ref` |
| Journal POSTED | Prerequisite for deposit credit |
| COA 2110 / 2120 / 2130 | Aggregate mirror — wallet rows do not store COA codes on balance |
| `journal_entry_id` on events | Maps to `coa_trans_id` on `wallet_tx` (correlation) |

Wallet credit is **out of scope** for the accounting service ([`core.accounting.trd.md`](./core.accounting.trd.md) §13).

---

## 11. Implementation checklist

- [ ] `wallet_*` tables; migrations owned by wallet module
- [ ] No `coa_*` import; wallet entities must not `@ManyToOne` accounting entities
- [ ] Every `wallet_balance` change → `wallet_tx` in one transaction
- [ ] UNIQUE idempotency keys match orchestration contract
- [ ] Deposit consumer credits only when POSTED (or explicit command with `coaTransId`)
- [ ] Shared code only via foundation ([ADR-002](./adr/ADR-002-core-foundation-shared-library.md))
- [ ] Reconciliation job spec documented in Application (W5)

---

## 12. Summary

`core.wallet` owns per-member `available` / `frozen` and append-only `wallet_tx`. Accounting owns **COA** and the ledger; Application sequences both via commands and events — wallet repo **must not** call accounting repo. Implement this TRD when the wallet module is added to the repo; until then, boundaries and orchestration order follow [`core.foundation.md`](./core.foundation.md) Part I §3 and Part II §8+.
