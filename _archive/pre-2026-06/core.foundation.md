# core.sharedlib — Shared Library Design

**Author:** Cao Khang Đoàn  
**Last updated:** 2026-06-04  
**Scope:** `10_core/` — `core.sharedlib` (shared library) + Part II accounting fund flow in this file.  
**Status:** Design only — not implemented in repo.

**Related:** [ADR-002](./adr/ADR-002-core-foundation-shared-library.md), [`TERMINOLOGY.md`](./TERMINOLOGY.md), [`integration-surfaces.md`](./integration-surfaces.md), [`core.wallet.md`](./core.wallet.md), [`core.accounting.trd.md`](./core.accounting.trd.md).  
**Wire:** [`open-api/`](./open-api/), [`async-api/`](./async-api/).

This file has **two parts:** **Part I** (sections below) = `core.sharedlib` only. **Part II** = fiat accounting fund flow (`coa_*`, use cases §8–16) kept in this file for document history; service FR/NFR also appear in [`core.accounting.trd.md`](./core.accounting.trd.md).

---

## Part I — `core.sharedlib`

### 1. Overview

#### 1.1 Purpose

`core.sharedlib` is the **shared library** at the bottom of the `core` stack — there is **no second “common” module** ([ADR-002](./adr/ADR-002-core-foundation-shared-library.md)). Application, `core.wallet`, and `core.accounting` share one set of:

- **Envelope** JSON HTTP (`ApiResponse`) and **error codes**
- **Pagination** (`PageRequest`, `PageResult`)
- **Util** — minimal, when needed (id, time — only when reused)

**Not in foundation (v1):** Duplicate command/event DTOs, port interfaces, Kafka/HTTP adapters. Wire shapes live in **`open-api/`** and **`async-api/`**; Application maps JSON → domain service method parameters.

Foundation **does not** contain wallet or accounting business rules, **does not** include persistence, and **there is no Java module in the repo yet** — `10_core` is design-only.

#### 1.2 Goals

| Area | Goal |
|------|------|
| Same format | One `ApiResponse` shape for all public APIs |
| Boundaries | Avoid a second “common” module holding entities |
| Contract | `businessRef` / idempotency described in OpenAPI + [`integration-surfaces.md`](./integration-surfaces.md) — enforced in Application, no extra DTO layer |
| Testing | Foundation tests run without Spring, DB, or Kafka |

#### 1.3 Out of scope

| Out of scope | Lives in |
|--------------|----------|
| `Wallet`, `CoaTrans` entities / repositories | `core.wallet`, `core.accounting` |
| Spring MVC, security filters, Kafka listeners | Application |
| COA posting rules, zero transit | Part II below / `core.accounting` |
| OpenAPI / AsyncAPI YAML | `open-api/`, `async-api/` (wire source of truth) |

---

### 2. Architecture placement

```
     Application (HTTP, Kafka adapters, orchestration)
              │
     ┌────────┴────────┐
     ▼                 ▼
  core.wallet      core.accounting
     │                 │
     └────────┬────────┘
              ▼
       core.sharedlib
              │
              ▼
             JDK
```

- **Application** maps HTTP query → `PageRequest`; wraps service results → `ApiResponse`.
- **Domain services** return `PageResult` or command results; not raw HTTP.
- **Foundation** does not open JDBC, Redis, or message brokers.

---

### 3. Bounded context (wallet vs accounting)

| | `core.wallet` | `core.accounting` |
|---|-----------------|-------------------|
| Tables | `wallet_*` | `coa_*` |
| Detail docs | [`core.wallet.md`](./core.wallet.md) | Part II below, [`core.accounting.trd.md`](./core.accounting.trd.md) |
| Must not access | `coa_trans`, `coa_trans_data` | `wallet_*` |

**May share:** this library only.  
**Must not share:** entities, repositories, SQL `JOIN` between `wallet_*` and `coa_*`.  
**Sync:** orchestration + events ([`integration-surfaces.md`](./integration-surfaces.md) §4); no direct module imports.

---

### 4. Components

#### 4.1 Request (`core.sharedlib.request`)

| Type | Fields | Notes |
|------|--------|-------|
| `PageRequest` | `page`, `size`, `sort`, `direction` | `page` 0-based; cap `size` e.g. default 100 |
| `SortParam` | `field`, `direction` | `ASC` / `DESC` |
| `KeywordFilter` | `keyword` | Optional; each API in Application |

No servlet or Spring Web types in this package.

#### 4.2 Response (`core.sharedlib.response`)

**`ApiResponse<T>`** — aligns with public OpenAPI `ApiEnvelope` + `data`:

| Field | Type | Rule |
|-------|------|------|
| `code` | `int` | `0` = success; non-zero = business error |
| `message` | `String` | Human-readable description |
| `data` | `T` | Nullable on error |
| `timestamp` | `Instant` / ISO-8601 | Optional; set at Application boundary |

Factories (design): `ApiResponse.ok(data)`, `ApiResponse.fail(ErrorCode, message)`.

#### 4.3 Errors (`core.sharedlib.exception`)

| Type | Role |
|------|------|
| `ErrorCode` | Enum or constants: stable codes for clients |
| `BaseException` | Carries `ErrorCode`; core domain subclasses |
| `ValidationException` | Input validation (optional) |

**Foundation codes (illustrative — extend in one place):**

| Code | HTTP (Application maps) | Meaning |
|------|-------------------------|---------|
| `0` | 200 | Success |
| `COMMON_INVALID_REQUEST` | 400 | Invalid input |
| `COMMON_NOT_FOUND` | 404 | Not found |
| `COMMON_CONFLICT` | 409 | Idempotency / state conflict |
| `WALLET_INSUFFICIENT_BALANCE` | 422 | See [`core.wallet.md`](./core.wallet.md) |
| `ACCOUNTING_UNBALANCED_JOURNAL` | 422 | DR ≠ CR |

Application maps `ErrorCode` → HTTP status; foundation does not depend on `spring-web`.

#### 4.4 Pagination (`core.sharedlib.page`)

**`PageResult<T>`:**

| Field | Type |
|-------|------|
| `content` | `List<T>` |
| `total` | `long` |
| `page` | `int` |
| `size` | `int` |

Empty page: `content=[]`, `total=0`.

#### 4.5 Util (`core.sharedlib.util`)

| Area | Examples | Rules |
|------|----------|-------|
| Id | UUID v4, snowflake helper | No DB |
| Time | `Instant` now (injectable clock for tests) | UTC |
| Money (parse only) | `parseAmount(String)` → `BigDecimal` | Scale 4; no rounding policy here |
| Strings | trim, max length | No business-rule regex |

---

### 5. Wire contract — not in foundation (v1)

| Approach | Decision |
|----------|----------|
| OpenAPI / AsyncAPI YAML in `10_core` | **Yes** — specs for Gateway, partners, codegen |
| `CreditWalletCommand` + shared `WalletCommandPort` in foundation | **No (v1)** — duplicates spec; add only when ≥2 runtimes need the same Java type |
| Orchestration | Application calls `WalletService.debit(...)` / accounting service directly with primitives or module-local DTOs |

**Convention (no extra layer):** use `businessRef` on write APIs (`X-Idempotency-Key` = ref in body). DB columns may use `business_ref` (snake_case).

**Revisit when:** a second consumer (e.g. gRPC) needs an identical Java contract — generate from OpenAPI or a thin module; still do not put full port/adapter frameworks in foundation.

---

### 6. Identity and money

| Concept | Convention |
|---------|------------|
| `businessRef` | External idempotency key; max 128 chars; **UNIQUE** per operation semantics |
| `memberId` | Platform member `long` |
| Money on wire | Decimal `string` in OpenAPI (e.g. `"100000.0000"`) |
| Money in domain | `BigDecimal`, scale 4, `HALF_UP` at domain boundary (wallet/accounting modules) |
| Correlation | Optional `coaTransId`, `walletTxId` on events — store ids for reconciliation only; **not** a DB FK from wallet to `coa_trans` |

---

### 7. Dependency rules

```
Application  ──►  core.wallet | core.accounting  ──►  core.sharedlib  ──►  JDK
```

| Allowed | Forbidden |
|---------|-----------|
| Domain → foundation | foundation → domain modules |
| Application → foundation + domain | foundation → Spring, JPA, Kafka, JDBC |
| Unit test POJOs/utils | foundation → `wallet_*` / `coa_*` entity classes |

---

### 8. Example flows (using foundation)

**Paged read**

1. `GET /v1/...?page=0&size=20` → Application builds `PageRequest`.
2. `WalletQueryService` / accounting query returns `PageResult<Dto>`.
3. `ApiResponse.ok(pageResult)` → JSON.

**Sync payment (orchestration)**

1. Application parses OpenAPI JSON → calls `core.wallet` service (debit).
2. Calls `core.accounting` (post journal).
3. Wraps result in `ApiResponse.ok(...)` — envelope from foundation only.

Step order: [`integration-surfaces.md`](./integration-surfaces.md) §4.

---

### 9. Foundation diagram

```
┌─────────────────────────────────────────────────────────┐
│                   core.sharedlib                        │
├─────────────────────────────────────────────────────────┤
│  request   PageRequest, SortParam                      │
│  response  ApiResponse<T>                                │
│  exception ErrorCode, BaseException                      │
│  page      PageResult<T>                                 │
│  util      minimal, when needed                          │
└─────────────────────────────────────────────────────────┘
                            │
                            ▼
                          JDK only
```

---

### 10. Checklist — add code to foundation?

| # | Question | If no → |
|---|----------|---------|
| 1 | Used by ≥ 2 of Application / wallet / accounting? | Keep local |
| 2 | No entity, repository, SQL, Kafka, HTTP? | Domain or Application |
| 3 | Pure unit test without Spring Boot? | Application starter |
| 4 | Name has no domain table semantics? | `core.wallet` / `core.accounting` |
| 5 | Duplicates OpenAPI as Java DTO in foundation? | Keep wire in YAML; map in Application |

---

### 11. Part I summary

`core.sharedlib` (when implemented) should stay **small**: `ApiResponse`, errors, pagination, small utils. Domain tables and posting live in `core.wallet` / `core.accounting` (Part II). Do not add `api.*` command layers until a concrete second consumer exists.

---

# Part II — `core.accounting` (fund flow design)

## Introduction

Accounting module: COA, **journal** (`coa_trans`), **journal_entry** (`coa_trans_data`), posting rules. [ADR-001 — Immutable ledger](./adr/ADR-001-immutable-ledger.md).

**Reference amounts (examples):** principal 100,000 | service fee 1,000 | bank/Napas fee 500 when applicable.

**In core.accounting:** entities, repositories, services for `coa_account`, `coa_trans`, `coa_trans_data`, and related tables.

**Not in core.accounting:** changing wallet balances (`2110`, `2120`, `2130`) — that is `core.wallet`. Accounting posts on the COA side (`coa_trans`); wallet syncs via **API or event (saga/outbox)**.

---

## 1. Goals

- Record **journal_entry** lines (`coa_trans_data`) under one **journal** (`coa_trans`).
- Query balances by COA `account_code` from POSTED `coa_trans_data`.
- Period close / reporting (if required by business).
- Maintain Chart of Accounts (COA): Assets, Liabilities, Transit, Revenue, Expense, Equity.
- Enforce balanced immutability; zero transit after each use case.
- Settlement & Clearing EOD as independent batch jobs.

---

## 2. Owned tables

### 2.0 `coa_` prefix convention

All **Chart of Accounts / ledger** tables in `core.accounting` use the **`coa_`** prefix:

| Table | Role |
|-------|------|
| `coa_account` | Account catalog (code, name, type) |
| `coa_trans` | **Journal** — balanced DR/CR header: `use_case`, `business_ref`, `status`, timestamp |
| `coa_trans_data` | **Journal entry** — one line: `coa_trans_id`, `account_code`, DR/CR, `amount` |

**Why `coa_`?**

- Clear separation from wallet (`wallet_*`) — avoids confusing “account” / “balance”.
- `coa_trans` avoids SQL keyword **`TRANSACTION`** (DB session).
- Names signal *COA domain*, not other business tables.

*(Optional post-v1: `coa_period`, `coa_audit_log` — same prefix.)*

Suggested Java entities: `CoaTrans`, `CoaTransData`, `CoaAccount` — 1:1 table names.

### 2.1 Tables (detail)

| Table | Role |
|-------|------|
| `coa_account` | Chart of accounts (code, name, type) |
| `coa_trans` | **Journal** — one balanced posting header: use_case, business_ref, time, status (total DR = total CR of lines) |
| `coa_trans_data` | **Journal entry** — one DR/CR line: `coa_trans_id`, `account_code`, amount |

All writes go only through repositories in `core.accounting`.

**Constraints (design):**

| Table / column | Constraint |
|----------------|------------|
| `coa_trans.business_ref` | **UNIQUE** per `use_case` (or globally per product rule) — idempotency |
| `coa_trans_data` | FK `coa_trans_id` → `coa_trans`; no UPDATE after POSTED ([ADR-001](./adr/ADR-001-immutable-ledger.md)) |

### 2.2 Terminology: `coa_trans` = **journal**, `coa_trans_data` = **journal_entry**

Platform mapping (differs from TRD table names `journal_entries` / `journal_lines`):

| Platform term | Table | Short description |
|---------------|-------|-------------------|
| **Journal** | **`coa_trans`** | One balanced DR/CR event (deposit, withdraw, …); `use_case`, `business_ref`, `status` |
| **Journal entry** | **`coa_trans_data`** | One DR or CR line (`account_code`, DR/CR, `amount`) |
| Ledger entry | **`coa_trans_data` (POSTED)** | Same table; immutable after `coa_trans.status = POSTED` |
| `reference_id` (TRD) | **`business_ref`** | Business idempotency key |
| Draft / Posted | **`PENDING` / `POSTED` / …** | Journal status; deposit 2-phase: `PENDING` → `POSTED` |

**TRD vs platform (do not confuse API names with platform meaning):**

| TRD (API / FR) | Platform meaning | Table |
|----------------|------------------|-------|
| `journal_entries` | **Journal** (header) | `coa_trans` |
| `journal_lines` | **Journal entry** (each line) | `coa_trans_data` |

**Why DB uses `coa_*` not `journal_*`?**

- `coa_` prefix separates COA from `wallet_*`; avoids journal / journal entry / ledger confusion.
- **`coa_trans`** = journal (one header, many lines).
- **`coa_trans_data`** = journal_entry (FK `coa_trans_id` → journal).

Standard rule: each `coa_trans` must have `sum(DR) = sum(CR)`; after period close only **reversal / new journal**, no UPDATE to `coa_trans_data` ([ADR-001](./adr/ADR-001-immutable-ledger.md)). Full map: [`core.accounting.trd.md` §13](./core.accounting.trd.md).

---

## 3. Posting flow (within `core.accounting`)

1. Receive posting request: `use_case`, `business_ref`, DR/CR lines.
2. Validate COA, open/closed period, `sum(DR) = sum(CR)`, transit to 0 when use case completes.
3. Persist one **journal** (`coa_trans`) + **journal_entry** lines (`coa_trans_data`) — immutable after period close.
4. If wallet update needed: after `status = POSTED`, notify `core.wallet` (event or API) — **do not** import wallet repositories.

---

## 4. Rules

- Access only tables listed in §2.
- **Journal entry** lines (`coa_trans_data`) are **immutable** after period close (adjust via reversal journal / supplemental `coa_trans` if needed).
- Each use case must bring transit accounts to **0** before completion.
- Settlement & Clearing runs as **independent EOD batch** — not inline with payment.
- `core.accounting` **does not** import wallet repositories; sync only via event/API.
- External integration → Application or a dedicated integration layer.

---

## 5. Balance invariants

| # | Invariant |
|---|-----------|
| 1 | `(1111 + 1112 + 1113) = (2110 + 2120 + 2130)` — bank assets = total wallet liabilities |
| 2 | Actual bank balance = sum of wallet balances |
| 3 | Each transit (3100–3820) returns to **0** after the use case completes |

Initial equity (payment reserve fund) is a buffer; daily operations run on user deposits.

---

## 6. Chart of Accounts (COA)

### 6.1 Group 1 — Assets

| Code | Account name |
|------|--------------|
| 1111 | Vietinbank — dedicated |
| 1112 | Napas Clearing |
| 1113 | VPBank — QR/POS |

### 6.2 Group 2 — Liabilities

| Code | Account name |
|------|--------------|
| 2110 | Wallet balance — User |
| 2120 | Wallet balance — Merchant |
| 2130 | Escrow — Disbursement partner |

### 6.3 Group 3 — Transit

| Code | Account name | Use case |
|------|--------------|----------|
| 3100 | Transit — Deposit | §8 Deposit |
| 3200 | Transit — Withdraw | §9 Withdraw |
| 3300 | Transit — Internal transfer | §10 Internal |
| 3400 | Transit — IBFT | §11 IBFT |
| 3500 | Transit — Payment | §12, §13 Payment |
| 3600 | Transit — Payroll | §14 Payroll |
| 3700 | Transit — Disbursement | §15 Disbursement |
| 3800 | Transit — Clearing | §16 Settlement EOD |
| 3810 | Transit — Settlement Outbound | §16 Settlement EOD |
| 3820 | Transit — MDR Holdback | §16 Settlement EOD |

### 6.4 Group 4 — Revenue

4110 Deposit fee | 4120 Withdraw fee | 4130 Transfer fee | 4140 MDR fee | 4150 Payroll / disbursement fee

### 6.5 Group 5 — Expense

5100 Bank / Napas fee expense

### 6.6 Group 6 — Equity

6000 Owner's equity

---

## 7. System initialization (before go-live)

| Step | Account | DR/CR | Amount |
|------|---------|-------|--------|
| Capital to Vietinbank | 1111 | DR | 1,000,000,000 |
| Record charter capital | 6000 | CR | 1,000,000,000 |
| Capital to VPBank | 1113 | DR | 500,000,000 |
| Record charter capital | 6000 | CR | 500,000,000 |
| Fund Napas Clearing | 1112 | DR | 500,000,000 |
| Record charter capital | 6000 | CR | 500,000,000 |

---

## 8. Use case — Deposit

**Example:** principal 100,000 + deposit fee 1,000 → user receives **99,000** in wallet.

### 8.1 Journal entries (accounting)

| Step | Actor | Account | DR/CR | Amount |
|------|-------|---------|-------|--------|
| 1 | Bank | 1111 | DR | 100,000 |
| 2 | Bank | 3100 | CR | 100,000 |
| 3 | Bank | 3100 | DR | 100,000 |
| 4 | User | 2110 | CR | 99,000 |
| 5 | User | 2110 | DR | 1,000 |
| 6 | User | 4110 | CR | 1,000 |

**Result:** `1111 +100,000` | `2110 +99,000` | `4110 +1,000` | Transit 3100 = 0

### 8.2 Transaction status (`coa_trans.status`)

| Status | Meaning |
|--------|---------|
| `PENDING` | Bank reports funds in; transit 3100; wallet not credited yet |
| `POSTED` | Confirmed; credit wallet + fee; transit 3100 = 0 |
| `FAILED` | Cancel / mismatch — reverse `coa_trans` if steps 1–2 were posted |

### 8.3 Overview flow (2-phase)

```
User     Vietinbank    Application      core.accounting    core.wallet      DB
  |            |              |                  |                |              |
  |-- CK 100k->|              |                  |                |              |
  |            |-- webhook -->|                  |                |              |
  |            |              | Phase A: post PENDING             |              |
  |            |              |----------------->|                |              |
  |            |              |                  | 1111 DR 100k   |------------->|
  |            |              |                  | 3100 CR 100k   |              |
  |            |              | map VA → userId  |                |              |
  |            |              | Phase B: post POSTED              |              |
  |            |              |----------------->|                |              |
  |            |              |                  | 3100 DR, 2110,4110|           |
  |            |              |                  | 3100 = 0       |              |
  |            |              | notify credit wallet |----------->  | +99k net     |
  |<-- notify -|              |                  |                |              |
```

Phase A: bank webhook → `coa_trans.status = PENDING`, transit **3100** holds 100k.  
Phase B: confirm → `POSTED`, **3100 = 0**, credit wallet net 99k (after §8.1 steps 3–6).

### 8.4 DB state

#### After phase A (`PENDING`)

| `coa_trans` | |
|-------------|--|
| `use_case` | `DEPOSIT` |
| `business_ref` | bank transaction id (**UNIQUE**) |
| `status` | `PENDING` |

| `coa_trans_data` | `account_code` | DR | CR |
|------------------|----------------|----|----|
| line 1 | 1111 | 100,000 | 0 |
| line 2 | 3100 | 0 | 100,000 |

#### After phase B (`POSTED`)

| `coa_trans` | |
|-------------|--|
| `status` | `POSTED` |

| `coa_trans_data` (add steps 3–6) | `account_code` | DR | CR |
|----------------------------------|----------------|----|----|
| | 3100 | 100,000 | 0 |
| | 2110 | 0 | 99,000 |
| | 2110 | 1,000 | 0 |
| | 4110 | 0 | 1,000 |

User wallet: `wallet_balance += 99,000` (only after POSTED; sync via event/API, no accounting repo `JOIN`).

### 8.5 Idempotency & errors

| Situation | Handling |
|-----------|----------|
| Duplicate webhook same `bankRef` | Return existing `coa_trans_id`, no insert (`business_ref` **UNIQUE**) |
| Confirm when `status=POSTED` | No-op |
| Cannot map VA → user | Keep `PENDING`, 3100 holds 100k, manual handling |
| Wallet credit fails after POSTED | Retry consumer; **do not** edit `coa_trans_data` — reconciliation job |
| Bank reports wrong amount | Do not confirm; reverse phase A with reversal journal |

### 8.6 Invariants after POSTED

```
1111  += 100,000
2110  +=  99,000   (mirror wallet_balance user)
4110  +=   1,000
3100  =  0
wallet_balance(user) = 99,000 (net)
```

### 8.7 Summary

Deposit is a **two-phase** flow tied to transit **3100**:

1. **Phase A (PENDING):** Bank webhook posts **1111** DR and **3100** CR — wallet not credited, no **4110** yet.
2. **Phase B (POSTED):** Map VA → user, post §8.1 steps 3–6 → **3100 = 0**, **2110** net +99k, **4110** +1k fee → credit wallet 99k.

| Aspect | Decision |
|--------|----------|
| Boundary | Accounting owns `coa_trans`/`coa_trans_data`; wallet owns balance — sync event/API, wallet repo **no** `JOIN` to accounting repo |
| Idempotent | `business_ref` = bank txn id — duplicate webhook/confirm does not double-post (**UNIQUE**) |
| Transit | **3100 = 0** when POSTED |
| Error after POSTED | Do not change `coa_trans_data`; retry wallet sync or reconciliation |

---

## 9. Use case — Withdraw

101,000 debited from wallet (principal 100,000 + fee 1,000)

| Step | Actor | Account | DR/CR | Amount |
|------|-------|---------|-------|--------|
| 1 | User | 2110 | DR | 101,000 |
| 2 | User | 3200 | CR | 101,000 |
| 3 | Bank | 3200 | DR | 100,000 |
| 4 | Bank | 1111 | CR | 100,000 |
| 5 | Bank | 3200 | DR | 1,000 |
| 6 | Bank | 4120 | CR | 1,000 |

**Result:** `2110 -101,000` | `1111 -100,000` | `4120 +1,000` | Transit 3200 = 0

---

## 10. Use case — Wallet to wallet (Internal)

| Step | Actor | Account | DR/CR | Amount |
|------|-------|---------|-------|--------|
| 1 | User A | 2110 | DR | 101,000 |
| 2 | User A | 3300 | CR | 101,000 |
| 3 | User B | 3300 | DR | 100,000 |
| 4 | User B | 2110 | CR | 100,000 |
| 5 | User A | 3300 | DR | 1,000 |
| 6 | User A | 4130 | CR | 1,000 |

**Result:** Transit 3300 = 0 | No bank movement

---

## 11. Use case — IBFT (interbank transfer)

| Step | Actor | Account | DR/CR | Amount |
|------|-------|---------|-------|--------|
| 1 | User | 2110 | DR | 101,000 |
| 2 | User | 3400 | CR | 101,000 |
| 3 | User | 3400 | DR | 1,000 |
| 4 | User | 4130 | CR | 1,000 |
| 5 | Bank | 3400 | DR | 100,000 |
| 6 | Bank | 1112 | CR | 100,000 |
| 8 | Bank | 5100 | DR | 500 |
| 9 | Bank | 1112 | CR | 500 |

**Result:** Transit 3400 = 0 | Net profit +500

---

## 12. Use case — QR/POS payment

Settlement to merchant bank → §16 EOD.

| Step | Actor | Account | DR/CR | Amount |
|------|-------|---------|-------|--------|
| 1 | Bank | 1113 | DR | 100,000 |
| 2 | Bank | 3500 | CR | 100,000 |
| 3 | Bank | 5100 | DR | 500 |
| 4 | Bank | 1113 | CR | 500 |
| 5 | Merchant | 3500 | DR | 100,000 |
| 6 | Merchant | 2120 | CR | 100,000 |

**Result:** Transit 3500 = 0 | `2120` pending settlement

---

## 13. Use case — Wallet payment

| Step | Actor | Account | DR/CR | Amount |
|------|-------|---------|-------|--------|
| 1 | User | 2110 | DR | 100,000 |
| 2 | User | 3500 | CR | 100,000 |
| 3 | Merchant | 3500 | DR | 100,000 |
| 4 | Merchant | 2120 | CR | 100,000 |

**Result:** Transit 3500 = 0 | `2120` pending settlement

---

## 14. Use case — Payroll

5 employees × 100,000 + fee 5,000

| Step | Actor | Account | DR/CR | Amount |
|------|-------|---------|-------|--------|
| 1 | Merchant | 2120 | DR | 505,000 |
| 2 | Merchant | 3600 | CR | 505,000 |
| 3 | Merchant | 3600 | DR | 5,000 |
| 4 | Merchant | 4150 | CR | 5,000 |
| 5 | Bank | 3600 | DR | 500,000 |
| 6 | Bank | 1112 | CR | 500,000 |
| 8 | Bank | 5100 | DR | 2,500 |
| 9 | Bank | 1112 | CR | 2,500 |

**Result:** Transit 3600 = 0 | Net profit +2,500

---

## 15. Use case — Disbursement

**Pre-fund:** 1111 DR 100,000 → 2130 CR 100,000

**Disburse:** 2130 DR 101,000 → 3700 CR → 1112 CR 100,000 + fees 4150/5100

**Result:** Transit 3700 = 0

---

## 16. Use case — Settlement & Clearing (EOD)

```
2120 → 3800 (lock) → 3820 (MDR) + 3810 (net) → 1112 → Merchant bank
```

| Step | Account | DR/CR | Amount |
|------|---------|-------|--------|
| Lock merchant | 2120 | DR | 200,000 |
| Hold clearing | 3800 | CR | 200,000 |
| Split MDR | 3800 DR 2,000 → 3820 CR 2,000 → 4140 CR 2,000 |
| Settlement | 3810 DR 198,000 → 1112 CR 198,500 (incl. Napas fee 5100) |

**Result:** All transit = 0 | `2120` = 0 after settlement

Exception branch: reverse 3810/3820 → 3800 → 2120 if reconciliation mismatch.

---

## Conclusion

`core.accounting` owns accounting end-to-end: COA, `coa_trans` / `coa_trans_data`, transit accounts, and EOD settlement (fund flows §8–16). Per-member balances (`wallet_*`) belong in [`core.wallet.md`](./core.wallet.md) — synchronized via event/API ([`integration-surfaces.md`](./integration-surfaces.md)); wallet and accounting code **must not** `JOIN` in one query. All modules depend on **Part I** `core.sharedlib`.
