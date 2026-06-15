# Implementation — decisions & build order

**Author:** Cao Khang Đoàn  
**Last updated:** 2026-06-12  
**Status:** Draft — locks open choices so code can start.  
**Scope:** Repo layout, DDL, service contracts, orchestration classes, idempotency algorithms, build phases. Domain narrative stays in TRDs / wire specs.

**Related:** [`foundation.md`](./foundation.md) Part I · [`trd/wallet.md`](./trd/wallet.md) · [`trd/accounting.md`](./trd/accounting.md) · [`integration-surfaces.md`](./integration-surfaces.md) · [`processes.md`](./processes.md).

---

## 1. Repo layout

### 1.1 Top-level decisions

| Decision | Choice |
|----------|--------|
| Where | New Maven parent **`core/`** (sibling to `00_framework/`), **not** inside legacy exchange modules |
| Legacy | `00_framework/wallet` = crypto wallet cũ — **do not** extend for fiat `core.wallet` |
| Parent POM | `core/pom.xml` — Java **17**, Spring Boot **3.3.x** only on `app-*` modules |
| GroupId | `com.gtelpay.core` (adjust to org standard) |
| Build | `mvn -pl core.wallet -am test` per module; parent aggregates all |

### 1.2 Module tree

```
core/
├── pom.xml                          # parent BOM, dependencyManagement
├── core.foundation/
│   └── src/main/java/com/gtelpay/core/foundation/
│       ├── request/                 PageRequest, SortParam, SortDirection
│       ├── response/                ApiResponse
│       ├── page/                    PageResult
│       ├── exception/               ErrorCode, BaseException, WalletException, …
│       └── util/                    MoneyUtil, Clock (interface)
├── core.wallet/
│   └── src/main/java/com/gtelpay/core/wallet/
│       ├── domain/                  Wallet, WalletBalance, WalletTx, enums
│       ├── repository/              JPA repos + custom @Query
│       ├── service/                   WalletCommandService, WalletQueryService, impl
│       └── config/                  WalletJpaConfig (optional, no @SpringBootApplication)
│   └── src/main/resources/db/migration/
├── core.accounting/
│   └── src/main/java/com/gtelpay/core/accounting/
│       ├── domain/                  CoaAccount, CoaTrans, CoaTransData, enums
│       ├── repository/
│       ├── service/                   JournalService, AccountQueryService, impl
│       └── posting/                 PostingRuleRegistry (use_case → line templates)
│   └── src/main/resources/db/migration/
├── app-orchestration/                 # Vert.x core inbound; WalletGateway + LedgerGateway HTTP (ADR-038)
│   └── src/main/java/com/gtelpay/app/orchestration/
│       ├── Main.java
│       ├── vertx/                   HttpServerVerticle — gtelpay-core-internal inbound
│       ├── web/                     ApiExceptionHandler, MemberIdResolver
│       ├── gateway/                 WalletGateway, LedgerGateway (+ HTTP impl)
│       ├── usecase/                 PaymentUseCase, … — **no** domain service imports
│       └── messaging/               S6 publisher, S3 listeners — P5+
├── app-wallet/                      (target) wallet pod — HTTP adapter wallet-internal.yaml
│   └── …                            wraps core.wallet
├── app-accounting/                  (target) accounting pod — S2 adapter
│   └── …                            wraps core.accounting
└── app-wallet-worker/               optional P5+ — S6 consumer
```

### 1.3 Dependency matrix

| Module | Depends on | Spring | JPA | Flyway | Testcontainers |
|--------|------------|--------|-----|--------|----------------|
| `core.foundation` | JDK | ✗ | ✗ | ✗ | ✗ |
| `core.wallet` | foundation | `@Transactional`, `@Service` only | ✓ | wallet schema | ✓ in test |
| `core.accounting` | foundation | same | ✓ | accounting schema | ✓ in test |
| `app-orchestration` | foundation (+ HTTP clients only) | Boot **without** `starter-web`; Vert.x core inbound | **Không** JPA domain — gọi wallet/accounting pod HTTP | — | ✓ IT (Testcontainers + wiremock) |
| `app-wallet` | `core.wallet` | Boot + MVC/internal | wallet schema only | wallet | ✓ |
| `app-accounting` | `core.accounting` | Boot + MVC S2 | accounting schema only | accounting | ✓ |
| `app-wallet-worker` | `core.wallet` | Boot + AMQP | wallet only | wallet | ✓ |

**Gap (code hiện tại):** `app-orchestration` vẫn depend + scan `core.wallet` / `core.accounting` JAR trong một JVM — cần tách theo hàng trên.

**Hard rule:** `core.wallet` POM **must not** list `core.accounting` (and vice versa).

### 1.4 Database v1

| Item | Choice |
|------|--------|
| Engine | PostgreSQL **15+** |
| Layout | One instance, schemas **`wallet`** and **`accounting`** |
| JPA | `@Table(schema = "wallet")` / `@Table(schema = "accounting")` |
| Cross-schema | **Forbidden** — no JOIN, no FK across schemas |
| Connection | `app-orchestration` holds two datasources OR one datasource with `currentSchema` per EMF — pick **two EntityManagerFactory** beans for clarity |
| Later scale | Split to two DBs — change datasource URL only; schemas unchanged |

---

## 2. Locked product decisions (D1–D5)

These were open in [`trd/wallet.md`](./trd/wallet.md) §3.4 / §5.2 — fixed here for migrations and orchestration.

### D1 — Wallet tx idempotency (UNIQUE)

```sql
CONSTRAINT uq_wallet_tx_idempotency UNIQUE (wallet_id, business_ref, tx_type)
```

| Rationale |
|-----------|
| One S1 `businessRef` drives **multiple** legs (payment debit USER + credit MERCHANT) on **different** `wallet_id` |
| `tx_type` disambiguates freeze / unfreeze / settle on the **same** wallet |

**Conflict rule:** same triple exists with different `amount` → throw `WalletDuplicateConflictException` → HTTP **409** / `WALLET_DUPLICATE_CONFLICT`.

**Safe replay:** same triple + same `amount` → return existing row, `idempotentReplay=true`, no balance change.

### D2 — Payment `businessRef`

| Leg | `business_ref` | `tx_type` | `wallet_type` |
|-----|----------------|-----------|---------------|
| User debit | S1 `businessRef` | `PAYMENT_DEBIT` | `USER` |
| Merchant credit | same | `PAYMENT_CREDIT` | `MERCHANT` |

Orchestration passes **one** ref from `createPayment`; wallet resolves target wallet from `memberId` + `walletType` + `tx_type`.

### D3 — Transfer `businessRef`

| Leg | `business_ref` | `tx_type` |
|-----|----------------|-----------|
| Debit A | S1 `businessRef` | `TRANSFER_DEBIT` |
| Credit B | same | `TRANSFER_CREDIT` |

Both legs `wallet_type=USER`; different `memberId` → different `wallet_id`.

### D4 — Deposit / async credit

| Step | Channel | `business_ref` / `reference_id` |
|------|---------|----------------------------------|
| Webhook ack | S1 | `X-Idempotency-Key` = `businessRef` |
| Worker command | S6 envelope | same `businessRef` |
| Journal header | S2 `reference_id` | same |
| Wallet credit | domain command | same; `tx_type=DEPOSIT_CREDIT`, `useCase=DEPOSIT` |

### D5 — Withdraw accept

| Step | `business_ref` | `tx_type` |
|------|----------------|-----------|
| Accept (hold) | S1 `businessRef` | `WITHDRAW_FREEZE` |
| Bank success settle | `{businessRef}:settle` | `WITHDRAW_SETTLE` |
| Bank fail / cancel | `{businessRef}:release` | `WITHDRAW_RELEASE` (`UNFREEZE`) |

v1 uses **freeze** on accept, not immediate debit ([ADR-007](./adr/ADR-007-freeze-settle-async-outflow.md)).

### 2.1 `WalletTxType` enum (implement exactly)

| Enum value | Direction | Use case |
|------------|-----------|----------|
| `DEPOSIT_CREDIT` | CREDIT | After ledger POSTED |
| `PAYMENT_DEBIT` | DEBIT | createPayment step 1 |
| `PAYMENT_CREDIT` | CREDIT | createPayment step 3 |
| `TRANSFER_DEBIT` | DEBIT | createTransfer |
| `TRANSFER_CREDIT` | CREDIT | createTransfer |
| `WITHDRAW_FREEZE` | FREEZE | createWithdrawal accept |
| `WITHDRAW_RELEASE` | UNFREEZE | payout failed / cancel |
| `WITHDRAW_SETTLE` | DEBIT | deduct from frozen after bank OK |
| `ADJUSTMENT_CREDIT` | CREDIT | ops (future) |
| `ADJUSTMENT_DEBIT` | DEBIT | ops (future) |

Store enum **name** in `wallet_tx.tx_type` column (`VARCHAR(32)`).

---

## 3. DDL — wallet schema (Flyway `V1__init_wallet.sql`)

```sql
CREATE SCHEMA IF NOT EXISTS wallet;

CREATE TYPE wallet.wallet_status AS ENUM ('ACTIVE', 'LOCKED', 'CLOSED');
CREATE TYPE wallet.wallet_type AS ENUM ('USER', 'MERCHANT', 'PARTNER');
CREATE TYPE wallet.tx_direction AS ENUM ('CREDIT', 'DEBIT', 'FREEZE', 'UNFREEZE');

-- ADR-040: catalog of pocket definitions ("ngăn ví"). Reference/config data.
-- A wallet (pocket) is instantiated by referencing a def code.
CREATE TABLE wallet.wallet_pocket_def (
    code            VARCHAR(32) PRIMARY KEY,            -- 'DEFAULT', 'SPENDING', 'SAVINGS', 'GOAL', ...
    name            VARCHAR(64) NOT NULL,               -- default display name for the pocket
    description     VARCHAR(255) NULL,
    wallet_type     wallet.wallet_type NOT NULL DEFAULT 'USER',
    is_default      BOOLEAN NOT NULL DEFAULT false,     -- auto-created on member onboard
    multi_allowed   BOOLEAN NOT NULL DEFAULT false,     -- member may hold >1 pocket of this def (e.g. GOAL)
    active          BOOLEAN NOT NULL DEFAULT true,
    sort_order      INT NOT NULL DEFAULT 0
);

CREATE TABLE wallet.wallet (
    id              BIGSERIAL PRIMARY KEY,
    member_id       BIGINT NOT NULL,
    wallet_type     wallet.wallet_type NOT NULL,
    currency        CHAR(3) NOT NULL DEFAULT 'VND',
    pocket_code     VARCHAR(32) NOT NULL DEFAULT 'DEFAULT' REFERENCES wallet.wallet_pocket_def(code), -- ADR-040
    label           VARCHAR(64) NOT NULL DEFAULT 'default', -- user display name; defaults from def.name
    status          wallet.wallet_status NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    -- ADR-040: USER may hold multiple pockets distinguished by label.
    CONSTRAINT uq_wallet_member_type_ccy_label UNIQUE (member_id, wallet_type, currency, label)
);
CREATE INDEX idx_wallet_member ON wallet.wallet (member_id);
-- ADR-040: MERCHANT/PARTNER restricted to a single wallet per (member, type, currency).
CREATE UNIQUE INDEX uq_wallet_single_nonuser
    ON wallet.wallet (member_id, wallet_type, currency)
    WHERE wallet_type <> 'USER';

CREATE TABLE wallet.wallet_balance (
    wallet_id       BIGINT PRIMARY KEY REFERENCES wallet.wallet(id),
    available       NUMERIC(19,4) NOT NULL DEFAULT 0 CHECK (available >= 0),
    frozen          NUMERIC(19,4) NOT NULL DEFAULT 0 CHECK (frozen >= 0),
    version         BIGINT NOT NULL DEFAULT 0,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE wallet.wallet_tx (
    id              BIGSERIAL PRIMARY KEY,
    wallet_id       BIGINT NOT NULL REFERENCES wallet.wallet(id),
    tx_type         VARCHAR(32) NOT NULL,
    direction       wallet.tx_direction NOT NULL,
    amount          NUMERIC(19,4) NOT NULL CHECK (amount > 0),
    available_after NUMERIC(19,4) NOT NULL,
    frozen_after    NUMERIC(19,4) NOT NULL,
    business_ref    VARCHAR(128) NOT NULL,
    coa_trans_id    BIGINT NULL,
    use_case        VARCHAR(32) NULL,
    remark          VARCHAR(512) NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_wallet_tx_idempotency UNIQUE (wallet_id, business_ref, tx_type)
);
-- (wallet_id, created_at, id) covers both "latest movement" and as-of balance
-- (ADR-004 point 7): ORDER BY created_at DESC, id DESC LIMIT 1 with timestamp tie-break.
CREATE INDEX idx_wallet_tx_wallet_created ON wallet.wallet_tx (wallet_id, created_at DESC, id DESC);
CREATE INDEX idx_wallet_tx_business_ref ON wallet.wallet_tx (business_ref);

-- ADR-040: seed pocket catalog (V2__seed_pocket_def.sql). 'DEFAULT' auto-created on member onboard.
-- INSERT wallet.wallet_pocket_def (code, name, wallet_type, is_default, multi_allowed):
--   ('DEFAULT','Ví chính','USER', true,  false)
--   ('SPENDING','Chi tiêu','USER', false, false)
--   ('SAVINGS','Tiết kiệm','USER', false, false)
--   ('GOAL','Mục tiêu','USER',     false, true)   -- multi_allowed: many goal pockets

-- ADR-039: non-authoritative async lane rollup. Fed by a periodic job, NOT by the
-- wallet write transaction (no synchronous aggregate row). Authoritative aggregate
-- stays in accounting (COA 2110/2120/2130). Monitors (ADR-032 MON-04) read this.
CREATE TABLE wallet.wallet_lane_rollup (
    wallet_type     wallet.wallet_type PRIMARY KEY,   -- USER / MERCHANT / PARTNER
    sum_available   NUMERIC(19,4) NOT NULL DEFAULT 0,
    sum_frozen      NUMERIC(19,4) NOT NULL DEFAULT 0,
    wallet_count    BIGINT NOT NULL DEFAULT 0,
    computed_at     TIMESTAMPTZ NOT NULL DEFAULT now() -- staleness ≤ job interval
);
```

---

## 4. DDL — accounting schema (Flyway `V1__init_accounting.sql`)

Minimal v1 — extend per [`trd/accounting.md`](./trd/accounting.md).

```sql
CREATE SCHEMA IF NOT EXISTS accounting;

CREATE TYPE accounting.journal_status AS ENUM ('PENDING', 'POSTED', 'REVERSED');
CREATE TYPE accounting.line_side AS ENUM ('DEBIT', 'CREDIT');

CREATE TABLE accounting.coa_account (
    code            VARCHAR(16) PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    account_type    VARCHAR(32) NOT NULL,
    active          BOOLEAN NOT NULL DEFAULT true
);

CREATE TABLE accounting.coa_trans (
    id              BIGSERIAL PRIMARY KEY,
    reference_id    VARCHAR(128) NOT NULL,
    use_case        VARCHAR(32) NOT NULL,
    status          accounting.journal_status NOT NULL DEFAULT 'PENDING',
    description     VARCHAR(512) NULL,
    posting_date    DATE NOT NULL DEFAULT CURRENT_DATE,
    posted_at       TIMESTAMPTZ NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_coa_trans_reference UNIQUE (reference_id, use_case)
);

CREATE TABLE accounting.coa_trans_data (
    id              BIGSERIAL PRIMARY KEY,
    coa_trans_id    BIGINT NOT NULL REFERENCES accounting.coa_trans(id),
    account_code    VARCHAR(16) NOT NULL REFERENCES accounting.coa_account(code),
    side            accounting.line_side NOT NULL,
    amount          NUMERIC(19,4) NOT NULL CHECK (amount > 0),
    currency        CHAR(3) NOT NULL DEFAULT 'VND',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_coa_trans_data_journal ON accounting.coa_trans_data (coa_trans_id);
```

### 4.1 COA seed (`V2__seed_coa.sql`)

Insert **every** account used by the v1 flows in [`foundation.md`](./foundation.md) §6–16 (a flow posting to a missing `account_code` fails the FK on `coa_trans_data`). `account_type` ∈ `ASSET | LIABILITY | TRANSIT | REVENUE | EXPENSE | EQUITY`.

| code | name | type | used by |
|------|------|------|---------|
| 1111 | Vietinbank — dedicated | ASSET | Deposit, withdraw |
| 1112 | Napas Clearing | ASSET | IBFT, payroll, disbursement, EOD |
| 1113 | VPBank — QR/POS | ASSET | QR/POS acquiring |
| 2110 | Wallet balance — User | LIABILITY | Deposit, payment, transfer, IBFT, withdraw |
| 2120 | Wallet balance — Merchant | LIABILITY | Payment credit, QR/POS, payroll, EOD |
| 2130 | Escrow — Disbursement partner | LIABILITY | Partner pre-fund, disbursement |
| 3100 | Transit — deposit | TRANSIT | Deposit |
| 3200 | Transit — withdraw | TRANSIT | Withdraw |
| 3300 | Transit — internal transfer | TRANSIT | Transfer |
| 3400 | Transit — IBFT | TRANSIT | IBFT |
| 3500 | Transit — payment | TRANSIT | Wallet payment, QR/POS |
| 3600 | Transit — payroll | TRANSIT | Payroll |
| 3700 | Transit — disbursement | TRANSIT | Disbursement |
| 3800 | Transit — clearing | TRANSIT | EOD settlement |
| 3810 | Transit — settlement outbound | TRANSIT | EOD settlement |
| 3820 | Transit — MDR holdback | TRANSIT | EOD settlement |
| 4110 | Fee revenue — deposit | REVENUE | Deposit fee |
| 4120 | Fee revenue — withdraw | REVENUE | Withdraw fee |
| 4130 | Fee revenue — transfer | REVENUE | Transfer, IBFT fee |
| 4140 | Fee revenue — MDR | REVENUE | QR/POS, EOD |
| 4150 | Fee revenue — payroll / disbursement | REVENUE | Payroll, disbursement fee |
| 5100 | Bank / Napas fee expense | EXPENSE | IBFT, payroll, disbursement, EOD |
| 6000 | Owner's equity | EQUITY | System initialization (§7 foundation) |

> v1 build phases P0–P6 exercise 1111, 2110, 2120, 3100, 3200, 3300, 3500, 4110, 4120, 4130. The remaining accounts are required once QR/POS, payroll, disbursement, and EOD settlement (flows: [`core.business-processes.md`](./core.business-processes.md) §8–§11; failure design §13) are implemented — seed them all up front so the ledger never rejects a valid posting.

---

## 5. `core.foundation` — implement first (P0)

Pure Java; **no** `spring-*` on compile classpath.

### 5.1 Class inventory

| Class | Package | Notes |
|-------|---------|-------|
| `PageRequest` | `request` | `page`, `size`, `sort`, `direction`; cap `size` max **100** |
| `SortParam` | `request` | field + ASC/DESC |
| `PageResult<T>` | `page` | `content`, `total`, `page`, `size`; empty → `content=[]`, `total=0` |
| `ApiResponse<T>` | `response` | `code` int 0=ok; `message`; `data`; `timestamp` Instant |
| `ErrorCode` | `exception` | enum — mirror [`asyncapi/core-events.yaml`](./asyncapi/core-events.yaml) `ErrorCode` |
| `BaseException` | `exception` | `ErrorCode code`, `String message` |
| `WalletException` | `exception` | extends BaseException |
| `AccountingException` | `exception` | extends BaseException |
| `ValidationException` | `exception` | `COMMON_INVALID_REQUEST` |
| `MoneyUtil` | `util` | `parseAmount(String)`, `normalize(BigDecimal)` scale 4 HALF_UP |

### 5.2 Factory methods

```java
ApiResponse.ok(T data)                    // code=0
ApiResponse.fail(ErrorCode code, String msg)  // code mapped in app layer
```

### 5.3 Tests (P0 done when)

- [ ] `MoneyUtil` scale/rounding cases
- [ ] `PageRequest` rejects negative page / oversize
- [ ] `ApiResponse` JSON snapshot (Jackson on **test** classpath only)

---

## 6. `core.wallet` — domain services

### 6.1 Public interfaces

```java
public interface WalletQueryService {
    BalanceView getBalance(long memberId, WalletType walletType, String currency);
    PageResult<WalletTxView> listTx(WalletTxQuery query, PageRequest page);
}

public interface WalletCommandService {
    WalletView provisionIfAbsent(long memberId, WalletType walletType, String currency);
    WalletTxResult credit(WalletMutationCommand cmd);
    WalletTxResult debit(WalletMutationCommand cmd);
    WalletTxResult freeze(WalletMutationCommand cmd);
    WalletTxResult unfreeze(WalletMutationCommand cmd);
}
```

### 6.2 `WalletMutationCommand` (one type for all mutations)

| Field | Type | Required |
|-------|------|----------|
| `memberId` | long | yes |
| `walletType` | WalletType | yes |
| `currency` | String | yes (v1 `VND`) |
| `amount` | BigDecimal | yes, normalized scale 4 |
| `businessRef` | String | yes, max 128 |
| `txType` | WalletTxType | yes |
| `coaTransId` | Long | no |
| `useCase` | String | no (`DEPOSIT`, `PAYMENT`, …) |
| `remark` | String | no |

### 6.3 `WalletTxResult`

| Field | Type |
|-------|------|
| `walletTxId` | long |
| `walletId` | long |
| `available` | BigDecimal |
| `frozen` | BigDecimal |
| `idempotentReplay` | boolean |

### 6.4 Command algorithm (single `@Transactional` per call)

Applies to `credit`, `debit`, `freeze`, `unfreeze`:

```
1. Normalize amount (MoneyUtil)
2. provisionIfAbsent(memberId, walletType, currency) if wallet row missing
3. SELECT wallet_balance WHERE wallet_id = ? FOR UPDATE
4. If wallet.status = LOCKED → throw WALLET_LOCKED
5. SELECT wallet_tx WHERE (wallet_id, business_ref, tx_type) = ?
   5a. If found AND amount matches → return result(idempotentReplay=true)
   5b. If found AND amount differs → throw WALLET_DUPLICATE_CONFLICT
6. Apply mutation on balance row:
     CREDIT:   available += amount
     DEBIT:    if available < amount → WALLET_INSUFFICIENT_BALANCE; available -= amount
     FREEZE:   if available < amount → …; available -= amount; frozen += amount
     UNFREEZE: if frozen < amount → …; frozen -= amount; available += amount
7. INSERT wallet_tx (direction from txType, snapshots available_after/frozen_after)
8. UPDATE wallet_balance SET version = version + 1, updated_at = now()
9. Return WalletTxResult(idempotentReplay=false)
```

**Concurrency:** prefer **`FOR UPDATE`** over optimistic-only for v1 (simpler correctness). Optional optimistic retry later.

### 6.5 Wallet errors

| Condition | ErrorCode | App HTTP |
|-----------|-----------|----------|
| Insufficient available | `WALLET_INSUFFICIENT_BALANCE` | 422 |
| Wallet not found (strict mode) | `WALLET_NOT_FOUND` | 404 |
| LOCKED | `WALLET_LOCKED` | 422 |
| Idempotency amount mismatch | `WALLET_DUPLICATE_CONFLICT` | 409 |
| Invalid amount / ref | `COMMON_INVALID_REQUEST` | 400 |

### 6.6 Tests (P1 done when)

- [ ] Credit → debit → balance matches W1
- [ ] Duplicate `(wallet_id, business_ref, tx_type)` → replay, balance unchanged (W3)
- [ ] Same ref different amount → 409
- [ ] Concurrent debits only one succeeds on last cent
- [ ] Payment D2: same `businessRef`, two `tx_type`, two wallets — both succeed

---

## 7. `core.accounting` — journal services

### 7.1 Public interfaces

```java
public interface JournalService {
    JournalHeader createJournal(CreateJournalCommand cmd);
    void addLines(long coaTransId, List<JournalLineCommand> lines);
    PostJournalResult postJournal(long coaTransId);
    PostJournalResult confirmDeposit(long coaTransId, BigDecimal fee); // deposit phase B (§7.5)
    JournalHeader reverseJournal(long coaTransId, ReverseJournalCommand cmd);
}

public interface AccountQueryService {
    BigDecimal getBalance(String accountCode, LocalDate asOf);
}
```

### 7.2 Commands

**CreateJournalCommand:** `referenceId` (= businessRef), `useCase`, `description`, `postingDate`

**JournalLineCommand:** `accountCode`, `amount`, `side` (DEBIT/CREDIT), `currency`

### 7.3 Idempotency

UNIQUE `(reference_id, use_case)` on `coa_trans`:

- Duplicate `createJournal` → return existing header (same as [`foundation.md`](./foundation.md) §8.5)
- `postJournal` when already POSTED → no-op, return current state
- Before post: validate **sum(DR) = sum(CR)** → else `ACCOUNTING_UNBALANCED_JOURNAL`

### 7.4 Posting — wallet payment (use_case `PAYMENT`)

Orchestration builds lines from [`foundation.md`](./foundation.md) §13 (amount = gross 100,000 example):

| Step | account | side | amount |
|------|---------|------|--------|
| 1 | 2110 | DR | gross |
| 2 | 3500 | CR | gross |
| 3 | 3500 | DR | gross |
| 4 | 2120 | CR | netToMerchant (or gross if no fee split) |

All lines in one `addLines` + single `postJournal` — transit **3500 = 0** after POSTED.

### 7.5 Posting — deposit phase A (`PENDING`)

| account | side | amount |
|---------|------|--------|
| 1111 | DR | gross bank amount |
| 3100 | CR | gross |

Phase B (`POSTED`): add lines per [`foundation.md`](./foundation.md) §8.1 steps 3–6 (3100 DR, 2110 CR net, fee to 4110).

**Locked:** phase B is a dedicated `confirmDeposit(long coaTransId, BigDecimal fee)` method on `JournalService` ([ADR-006](./adr/ADR-006-two-phase-deposit.md)). It (1) loads the PENDING journal, (2) appends the §8.1 steps 3–6 lines (3100 DR gross, 2110 CR net, 2110 DR fee, 4110 CR fee), (3) validates `sum(DR)=sum(CR)` and transit 3100 = 0, (4) sets `status = POSTED`, `posted_at = now()`. Do **not** use a bare `addLines` + `postJournal` from orchestration for deposit — the two-phase template lives inside accounting so the transit-zero invariant is enforced in one place. Idempotent: calling `confirmDeposit` on an already-POSTED journal is a no-op returning current state.

### 7.6 Tests (P2 done when)

- [ ] Deposit PENDING → POSTED → transit 3100 net zero
- [ ] Duplicate reference_id → same journal id
- [ ] Unbalanced lines → reject post
- [ ] Payment lines → transit 3500 zero

---

## 8. Orchestration transaction boundaries

| Use case | Steps | TX | Partial failure |
|----------|-------|-----|-----------------|
| **getBalance** | wallet query | 1× read | — |
| **Payment** | debit → post journal → credit | **3×** separate commits | See §8.1 |
| **Transfer** | debit A → post → credit B | 3× | Same as payment |
| **Deposit** | S1 202 → S6 → accounting → wallet credit | async, no distributed TX | Retry + `command-failed` Kafka |
| **Withdraw** | freeze → 200 → S6 payout | freeze TX sync; bank async | `:release` unfreeze |

Orchestration **must not** INSERT into `wallet_*` / `coa_*` directly ([`integration-surfaces.md`](./integration-surfaces.md) §10 F1).

### 8.1 Payment failure matrix

| Failed after | System state | Action |
|--------------|--------------|--------|
| Step 1 (debit) | No ledger | Return 422/409 to client; no compensate |
| Step 2 (post) | User debited, journal maybe PENDING/POSTED | **Ops alert** — manual/compensating debit reversal via new `businessRef` (orchestration playbook); wallet does not call accounting |
| Step 3 (credit) | User debited, ledger POSTED | Retry merchant credit with same ref+txType (idempotent); persistent fail → reconciliation |

### 8.2 `PaymentUseCase` (pseudocode)

```java
@Transactional // wallet TX #1 only — or call walletService without outer TX
WalletTxResult debit = walletCommand.debit(cmd(user, PAYMENT_DEBIT, businessRef, gross));

JournalHeader j = journalService.createJournal(ref, "PAYMENT", ...);
journalService.addLines(j.id(), paymentLines(gross, netToMerchant));
PostJournalResult posted = journalService.postJournal(j.id());

WalletTxResult credit = walletCommand.credit(cmd(merchant, PAYMENT_CREDIT, businessRef, netToMerchant)
    .coaTransId(posted.id()));

return PaymentResult.of(debit.walletTxId(), credit.walletTxId(), posted.id());
```

Each service method `@Transactional` — orchestration method **not** wrapping all three in one TX.

---

## 9. Wire → code mapping

### 9.0 `app-orchestration` runtime

| Layer | Technology | Vai trò |
|-------|------------|---------|
| **Orchestration (core inbound)** | Vert.x handler + `MemberIdResolver` | Auth + validate; target JWT [ADR-011](./adr/ADR-011-auth-identity-jwt-subject.md) |
| **Workflow** | `PaymentUseCase`, `WalletBalanceUseCase` | Sequence qua `WalletGateway` / `LedgerGateway` only |
| **Domain access (design)** | HTTP clients | `wallet-internal.yaml`, S2 `accounting-internal.yaml` — **pod riêng** [ADR-038](./adr/ADR-038-orchestrator-separate-service-gateway-seam.md) |

**Code gap (hiện tại):** use case vẫn `@Autowired` `WalletCommandService` / `JournalService` trong cùng JVM — **vi phạm ADR-038**, cần thay bằng `HttpWalletGateway` / `HttpLedgerGateway`. Không document như kiến trúc đích.

Target: core inbound = auth + validate + map wire; delegate **chỉ** qua gateway interfaces — orchestration **không** scan domain JAR. Spec: [`gtelpay-core-internal.yaml`](./contracts/openapi/gtelpay-core-internal.yaml). Xem [`integration-surfaces.md`](./integration-surfaces.md) §2.1.

### 9.1 Core inbound → orchestration classes

**Spec:** [`gtelpay-core-internal.yaml`](./contracts/openapi/gtelpay-core-internal.yaml). **Code gap:** Vert.x routes vẫn map từ `gtelpay-public` paths trong monolith IT.

**Implemented (Vert.x route → use case):**

| OpenAPI operationId | HTTP (v1) | Class | Delegates to |
|---------------------|-----------|-------|--------------|
| `getWalletBalance` | `GET /v1/wallets/balance` | `HttpServerVerticle` | `WalletBalanceUseCase` → `WalletQueryService` |
| `createPayment` | `POST /v1/payments` | `HttpServerVerticle` | `PaymentUseCase.execute` |
| — | `GET /health` | `HttpServerVerticle` | liveness stub |

**Target (Spring MVC hoặc Vert.x — chưa có):**

| OpenAPI operationId | Target handler | Delegates to |
|---------------------|----------------|--------------|
| `createTransfer` | `TransferController` / route | `TransferUseCase.execute` |
| `createWithdrawal` | `WithdrawController` / route | `WithdrawUseCase.execute` |
| `notifyDeposit` / `bankWebhook` | `DepositController` / route | `DepositUseCase.acceptAsync` → S6 publish |

Wrap responses: `ApiResponse.ok(data)`; map exceptions in `ApiExceptionHandler` (v1) / `GlobalExceptionHandler` (target):

| ErrorCode | HTTP |
|-----------|------|
| `COMMON_INVALID_REQUEST` | 400 |
| `COMMON_NOT_FOUND` | 404 |
| `COMMON_CONFLICT` | 409 |
| `WALLET_INSUFFICIENT_BALANCE` | 422 |
| `WALLET_LOCKED` | 422 |
| `WALLET_DUPLICATE_CONFLICT` | 409 |
| `ACCOUNTING_UNBALANCED_JOURNAL` | 422 |

### 9.2 Orchestration → domain — HTTP only (ADR-038)

| Đích | Wire spec | Kiến trúc |
|------|-----------|-----------|
| **Wallet** | [`wallet-internal.yaml`](./openapi/wallet-internal.yaml) | `WalletGateway` HTTP → wallet pod |
| **Accounting** | [`accounting-internal.yaml`](./openapi/accounting-internal.yaml) | `LedgerGateway` S2 HTTP → accounting pod |

| Gap | Hiện trạng code | Cần làm |
|-----|-----------------|---------|
| Gateway impl | `InProcess*Gateway` / `@Autowired` domain class | HTTP client; use case không đổi |
| Deploy | Một JVM | Tách 3 pod (orch, wallet, accounting) |

Wallet và accounting **không** chạy in-process trong orchestration — đó là nợ migration, không phải option thiết kế.

### 9.3 S6 — publish

```java
// DepositUseCase after 202
CommandEnvelope env = CommandEnvelope.builder()
    .messageId(UUID.randomUUID())
    .businessRef(businessRef)
    .memberId(memberId)
    .commandType(BANK_DEPOSIT)
    .occurredAt(Instant.now())
    .schemaVersion("1.0")
    .correlationId(traceId)
    .source("orchestration")
    .payload(bankDepositPayload)
    .build();
rabbitTemplate.convertAndSend("core.commands", "bank_deposit." + memberId, env);
```

Consumer (accounting worker or same app `@RabbitListener`): idempotent on `(BANK_DEPOSIT, businessRef)`.

### 9.4 S3 — events (P5+)

| Event | Publisher | Consumer action |
|-------|-----------|-----------------|
| `JournalPosted` | accounting adapter after post | orchestration → `wallet.credit` if `useCase=DEPOSIT` |
| `WalletCredited` | after wallet credit TX | notify / analytics |
| `core.operations.command-failed` | worker DLQ bridge | ops; deposit poll → FAILED |

---

## 10. Build phases (detailed)

### P0 — foundation (1–2 days)

- [x] Maven module `core.foundation`
- [x] All classes §5.1 + unit tests
- [x] Parent POM dependencyManagement (Jackson, JUnit 5, AssertJ — test scope in child modules)

### P1 — wallet domain (3–5 days)

- [x] Flyway V1 wallet schema
- [x] JPA entities + repositories
- [x] `WalletCommandServiceImpl` with algorithm §6.4
- [x] Testcontainers integration tests §6.6
- [x] Spring `@Configuration` for wallet EMF (used by app later)

### P2 — accounting domain (3–5 days)

- [x] Flyway V1 + V2 seed COA
- [x] `JournalServiceImpl` — create / addLines / post / idempotency / `confirmDeposit`
- [x] Deposit two-phase helper (PENDING + POSTED lines)
- [x] Payment line template §7.4
- [x] Tests §7.6

### P3 — first HTTP slice (2–3 days)

- [x] `app-orchestration` — Spring context + **Vert.x** S1 (không Spring MVC)
- [x] `GET /v1/wallets/balance` → OpenAPI shape
- [ ] Auth stub (JWT parse `memberId` from claims — detail in gateway)
- [x] `GET /health` (v1 stub; Actuator chưa bật)
- [ ] Dual datasource PostgreSQL (v1 dùng H2)

### P4 — sync payment (3–4 days)

- [x] `POST /v1/payments` §8.2 (fee split `netToMerchant ≠ amount` — reject v1)
- [x] Idempotency header → `businessRef`
- [x] IT: pay → balances; duplicate header → replay

### P5 — async deposit (4–5 days)

- [ ] `notifyDeposit` 202 + S6 publish
- [ ] Accounting consumer: phase A + B
- [ ] Wallet credit (sync call or S6 `WALLET_CREDIT`)
- [ ] Optional `JournalPosted` Kafka publish

### P6 — withdraw (3–4 days)

- [ ] Freeze on accept D5
- [ ] S6 `WITHDRAW_PAYOUT` stub
- [ ] `:settle` / `:release` commands

---

## 11. Configuration (`application.yml` sketch)

```yaml
spring:
  datasource:
    wallet:
      jdbc-url: jdbc:postgresql://localhost:5432/gtelpay
      username: wallet
      password: ${WALLET_DB_PASSWORD}
    accounting:
      jdbc-url: jdbc:postgresql://localhost:5432/gtelpay
      username: accounting
      password: ${ACCOUNTING_DB_PASSWORD}

gtelpay:
  wallet:
    default-currency: VND
  rabbit:
    exchange: core.commands
  kafka:
    bootstrap-servers: localhost:9092
```

Flyway: configure **per datasource** — `spring.flyway.schemas=wallet` on wallet EMF bean.

---

## 12. Observability — log fields (every mutation)

| Field | Source |
|-------|--------|
| `businessRef` | command / header |
| `memberId` | command |
| `walletTxId` / `coaTransId` | result |
| `txType` / `useCase` | command |
| `idempotentReplay` | result |
| `correlationId` | MDC from HTTP or S6 envelope |

---

## 13. Checklist before first PR

- [ ] D1–D5 in Flyway + orchestration `tx_type` values
- [ ] `core.wallet` POM: no `core.accounting`
- [ ] Payment IT: duplicate `businessRef` → no double debit
- [ ] No `@RestController` in domain jars
- [ ] OpenAPI `createPayment` response includes `walletTxId`, `coaTransId`

---

## 14. Doc map

| Task | Read |
|------|------|
| Table columns / FR | [`trd/wallet.md`](./trd/wallet.md), [`trd/accounting.md`](./trd/accounting.md) |
| DR/CR / transit | [`foundation.md`](./foundation.md) Part II §8–16 |
| Step order | [`integration-surfaces.md`](./integration-surfaces.md) §4 |
| HTTP / Kafka / RabbitMQ payloads | `openapi/`, `asyncapi/` |
| Shared types | [`foundation.md`](./foundation.md) Part I §4 |
| **This file** | Layout, DDL, algorithms, phases, class names |
