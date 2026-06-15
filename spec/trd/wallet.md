# core.wallet — Wallet Service Design (TRD)

**Author:** Cao Khang Đoàn  
**Last updated:** 2026-06-10  
**Scope:** `10_core/` — `core.wallet`. Terms: [`TERMINOLOGY.md`](./TERMINOLOGY.md). **Status:** Implemented — `platform/core.wallet` (`WalletCommandService` / `WalletQueryService`, Flyway `V1__init_wallet.sql`); see §9 for the concurrency contract.

**Related:** [`integration-surfaces.md`](./integration-surfaces.md), [`core.foundation.md`](./core.foundation.md), [`openapi/README.md`](./openapi/README.md).  
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
     app-orchestration pod
              │
              │  WalletGateway HTTP (wallet-internal.yaml)
              ▼
         core.wallet pod
    wallet, wallet_balance, wallet_tx
              │
              ▼
       core.foundation  (envelope, errors — v1)
```

Orchestrator **không** embed wallet JAR — gọi qua mạng ([ADR-038](./adr/ADR-038-orchestrator-separate-service-gateway-seam.md)).

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

Orchestration order (Application — see [`openapi/gtelpay-core-internal.yaml`](./openapi/gtelpay-core-internal.yaml) `createPayment`):

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

The orchestrator is a **separate** service and calls wallet **over the network** through a gateway seam (`WalletGateway`); the table below is the wallet service contract it consumes. (In-process wiring is a transitional implementation only — not the target architecture; see [ADR-038](./adr/ADR-038-orchestrator-separate-service-gateway-seam.md).) Illustrative operations:

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

Paths and schemas: [`openapi/gtelpay-core-internal.yaml`](./openapi/gtelpay-core-internal.yaml) (orch inbound); public channel [`gtelpay-public.yaml`](./openapi/gtelpay-public.yaml) (paymentorches). Wallet module **does not** embed HTTP routes.

---

## 8. Idempotency and errors

| Situation | Behavior |
|-----------|----------|
| Duplicate `business_ref`, same effect | Return `walletTxId` + current balance (HTTP 200) |
| **Concurrent** duplicate `business_ref` (same triple, in-flight) | The loser of the row-lock race returns the same `walletTxId` as an idempotent replay — **not** a constraint-violation error. Guaranteed by the under-lock recheck (§9), backed by `UNIQUE (wallet_id, business_ref, tx_type)` |
| Duplicate `business_ref`, conflicting amount | Reject — `CONFLICT` / wallet error |
| Debit when `available` insufficient | Reject — no partial debit |
| Credit after POSTED retry | Safe no-op via idempotency |
| Ledger POSTED but wallet credit fails | Retry consumer; **do not** reverse ledger from wallet — ops reconciliation ([`core.foundation.md`](./core.foundation.md) Part II §8.5) |
| Wallet `LOCKED` | Reject debit/freeze |

**Error codes:** extend `core.foundation` `ErrorCode` — e.g. `WALLET_INSUFFICIENT_BALANCE`, `WALLET_NOT_FOUND`, `WALLET_LOCKED`, `WALLET_DUPLICATE_CONFLICT`.

---

## 9. Concurrency and storage

### 9.0 Performance profile (NFR)

`core.wallet` is the **customer hot path** — the high-RPS front for "how much can this member spend now?" ([ADR-004](./adr/ADR-004-wallet-balance-snapshot.md)). Accounting is back-office (`spec/trd/accounting.md` §4 positioning).

| Operation | Cost | Why it scales |
|-----------|------|---------------|
| **Balance query** | single-row `SELECT` on `wallet_balance` (PK = `wallet_id`); **no derive**, no `SUM(wallet_tx)` | Read is O(1) per wallet → **max RPS**; optional read-through cache (invalidate on write). Authoritative snapshot per [ADR-004](./adr/ADR-004-wallet-balance-snapshot.md). |
| **Mutation (credit/debit/freeze/…)** | locks **only the one `wallet_id` row** (or optimistic `version`), never a shared/global row | Different wallets write **fully in parallel** → throughput scales with wallet count, not serialized. **No synchronous aggregate row** ([ADR-039](./adr/ADR-039-no-synchronous-wallet-aggregate-row.md)) → no global write bottleneck. |
| **As-of (historical) balance** | indexed `wallet_tx` lookup ≤ T ([ADR-004](./adr/ADR-004-wallet-balance-snapshot.md) point 7) | Off the live row; does not contend with hot-path reads/writes. |

Targets (align with accounting TRD latency table): balance query **P95 < 100ms** at peak; mutation **P95 < 150ms** (single-row lock + one append). Contention is per-wallet only — a "hot wallet" is the sole serialization point, mitigated by shard/batch ([ADR-004](./adr/ADR-004-wallet-balance-snapshot.md) impl notes), and never spreads to other wallets.

### 9.1 Mechanics

- **Row lock:** `SELECT … FOR UPDATE` on `wallet_balance` (or optimistic `version` with retry) per command transaction.
- **Idempotency check ordering (W3):** re-check the `(wallet_id, business_ref, tx_type)` triple **after** taking the row lock, not only before — else concurrent same-triple requests double-apply. `UNIQUE (wallet_id, business_ref, tx_type)` (§3.4) backs it at the DB level and must live in both the Flyway migration and the JPA mapping. Impl + proof: `WalletCommandServiceImpl.execute()`, test `concurrentSameTriple_appliesOnceAndReplaysLoser`; [ADR-005](./adr/ADR-005-idempotency-key-strategy.md) AC-005-02.
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

Wallet credit is **out of scope** for the accounting service ([`accounting.md`](./accounting.md) §13).

---

## 11. Implementation checklist

- [ ] `wallet_*` tables; migrations owned by wallet module
- [ ] No `coa_*` import; wallet entities must not `@ManyToOne` accounting entities
- [ ] Every `wallet_balance` change → `wallet_tx` in one transaction
- [x] UNIQUE idempotency keys match orchestration contract — `(wallet_id, business_ref, tx_type)` in **both** Flyway migration and JPA `@Table` mapping
- [x] Idempotency existence check re-run under the `wallet_balance` row lock (§9) — closes the TOCTOU double-apply window; covered by `concurrentSameTriple_appliesOnceAndReplaysLoser`
- [ ] Deposit consumer credits only when POSTED (or explicit command with `coaTransId`)
- [ ] Shared code only via foundation ([ADR-002](./adr/ADR-002-core-foundation-shared-library.md))
- [ ] Reconciliation job spec documented in Application (W5)

---

## 12. Tài liệu tham khảo về ví (e-wallet / wallet ledger)

**Chỉ tài liệu về ví** — kiến trúc wallet, số dư spendable, freeze, ví điện tử VN. Không phải tài liệu kế toán chung (kế toán → [`references/accounting/`](./references/accounting/)).

**File đã tải:** [`spec/trd/references/wallet/`](./references/wallet/) — mở thư mục, đọc trực tiếp. Không normative; quy tắc nội bộ ở §1–§11 và [`core.foundation.md`](./core.foundation.md).

### 12.1 Kiến trúc & sản phẩm ví

| Nguồn | URL | File local |
|-------|-----|------------|
| SDK.finance — *Wallet Ledger* | https://sdk.finance/blog/what-is-a-wallet-ledger-how-psps-neobanks-and-payment-apps-use-it/ | [`sdk-finance-wallet-ledger.md`](./references/wallet/sdk-finance-wallet-ledger.md) |
| SDK.finance — *Mobile Wallet Features* | https://sdk.finance/blog/must-have-features-of-a-future-ready-mobile-wallet-in-2024/ | [`sdk-finance-mobile-wallet-features.md`](./references/wallet/sdk-finance-mobile-wallet-features.md) |
| SDK.finance — *Ledger vs Core Banking* | https://sdk.finance/blog/ledger-vs-core-banking-system-whats-the-difference/ | [`sdk-finance-ledger-vs-core-banking.md`](./references/wallet/sdk-finance-ledger-vs-core-banking.md) |
| SDK.finance — *Payment Reconciliation* | https://sdk.finance/blog/payment-reconciliation-core-fintech-infrastructure/ | [`sdk-finance-payment-reconciliation.md`](./references/wallet/sdk-finance-payment-reconciliation.md) |
| Modern Treasury — *Digital Wallet Product* | https://www.moderntreasury.com/journal/how-to-build-a-digital-wallet-product | [`mt-digital-wallet-product.md`](./references/wallet/mt-digital-wallet-product.md) |
| Modern Treasury — *FBO Account* | https://www.moderntreasury.com/journal/when-and-how-to-get-an-fbo-account | [`mt-fbo-account.md`](./references/wallet/mt-fbo-account.md) |
| Modern Treasury — *Virtual Accounts* | https://www.moderntreasury.com/learn/what-are-virtual-accounts | [`mt-virtual-accounts.md`](./references/wallet/mt-virtual-accounts.md) |
| Nicholas Idoko — *Wallets, Ledgers, Payouts* | https://medium.com/@nicholas-idoko/69-designing-wallets-ledgers-and-payouts-for-fintech-like-apps-d1117c74f979 | [`medium-wallets-ledgers-payouts.md`](./references/wallet/medium-wallets-ledgers-payouts.md) |
| dev.to — *Scalable Wallet System* | https://dev.to/priyanka_5ea7b93552aa7dd0/designing-a-scalable-wallet-system-for-modern-fintech-applications-4893 | [`devto-scalable-wallet-system.md`](./references/wallet/devto-scalable-wallet-system.md) |
| Austin Corso — *Payments System* | https://medium.com/@austinmcorso/software-architecture-payments-system-afc19c717a42 | [`medium-payments-system-architecture.md`](./references/wallet/medium-payments-system-architecture.md) |
| Stripe — *Ledger* | https://stripe.com/blog/ledger-stripe-system-for-tracking-and-validating-money-movement | [`stripe-ledger-money-movement.md`](./references/wallet/stripe-ledger-money-movement.md) |
| Stripe — *Account balances* | https://stripe.com/resources/more/account-balances | [`stripe-account-balances.md`](./references/wallet/stripe-account-balances.md) |
| Stripe — *Wallet-as-a-Service* | https://stripe.com/resources/more/wallet-as-a-service-explained-for-modern-businesses | [`stripe-wallet-as-a-service.md`](./references/wallet/stripe-wallet-as-a-service.md) |
| Kunle.app — *Reconciliation* | https://www.kunle.app/dec-2020-financial-reconciliation.html | [`kunle-financial-reconciliation.md`](./references/wallet/kunle-financial-reconciliation.md) |
| Flagship — *Ledger-as-a-Service* | https://insights.flagshipadvisorypartners.com/ledger-as-a-service-an-emerging-infrastructure-layer-powering-fintech-innovation | [`flagship-laas-wallet.md`](./references/wallet/flagship-laas-wallet.md) |
| FinLego — *WaaS Platform* | https://finlego.com/blog/how-to-build-a-scalable-wallet-as-a-service-platform | [`finlego-waas-platform.md`](./references/wallet/finlego-waas-platform.md) |

Map TRD: `available`/`frozen` → §3, §5, §8 · idempotency → §9 · recon → W5 · USER/MERCHANT/PARTNER → §3.2.

### 12.2 Quy định ví điện tử Việt Nam

| Nguồn | URL | File local |
|-------|-----|------------|
| TT **40/2024/TT-NHNN** (EN) | https://thuvienphapluat.vn/van-ban/EN/Tien-te-Ngan-hang/Circular-40-2024-TT-NHNN-on-provision-of-payment-intermediary-services/621475/tieng-anh.aspx | [`thuvienphapluat-circular-40-2024-en.md`](./references/wallet/thuvienphapluat-circular-40-2024-en.md) |
| TT **40/2024/TT-NHNN** (VN) | https://thuvienphapluat.vn/van-ban/Tien-te-Ngan-hang/Thong-tu-40-2024-TT-NHNN-huong-dan-hoat-dong-cung-ung-dich-vu-trung-gian-thanh-toan-615328.aspx | [`thuvienphapluat-circular-40-2024-vn.md`](./references/wallet/thuvienphapluat-circular-40-2024-vn.md) |
| TT **41/2025/TT-NHNN** (EN) | https://thuvienphapluat.vn/van-ban/EN/Tien-te-Ngan-hang/Circular-41-2025-TT-NHNN-amendment-to-Circular-40-2024-TT-NHNN-intermediary-payment-service/682302/tieng-anh.aspx | [`thuvienphapluat-circular-41-2025-en.md`](./references/wallet/thuvienphapluat-circular-41-2025-en.md) |
| TT **41/2025/TT-NHNN** (VN) | https://thuvienphapluat.vn/van-ban/Tien-te-Ngan-hang/Thong-tu-41-2025-TT-NHNN-sua-doi-Thong-tu-40-2024-TT-NHNN-679853.aspx | [`thuvienphapluat-circular-41-2025-vn.md`](./references/wallet/thuvienphapluat-circular-41-2025-vn.md) |
| Vietnam Law Magazine | https://vietnamlawmagazine.vn/central-bank-imposes-tighter-e-wallet-requirements-75939.html | [`vn-ewallet-circular41.md`](./references/wallet/vn-ewallet-circular41.md) |

---

## 13. Summary

`core.wallet` owns per-member `available` / `frozen` and append-only `wallet_tx`. Accounting owns **COA** and the ledger; Application sequences both via commands and events — wallet repo **must not** call accounting repo. Implement this TRD when the wallet module is added to the repo; until then, boundaries and orchestration order follow [`core.foundation.md`](./core.foundation.md) Part I §3 and Part II §8+.
