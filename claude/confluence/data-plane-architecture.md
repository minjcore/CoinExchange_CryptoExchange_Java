# Data Plane Architecture — GtelPay Core Money Movement

> **CF page ID:** 43581441 | **Root page of this section**
> **Source of truth:** this file → push to CF
> **Reflects:** Constitution v1.1.0 (ratified 2026-06-15, amended 2026-06-19)

---

## Core Ideas (read first)

> **Three things to remember:**
>
> 1. **E-wallet = fast lane.** Client waits for this — must complete in < 100ms.
> 2. **General Ledger = slow lane.** Records ledger async afterward, never blocks the client.
> 3. **All logic (auth, fee, fraud) runs in-process** — no network hops, no DB queries on the hot path.

The entire architecture revolves around these three points. The rest of this page explains why and how to implement them.

---

## Concrete Example: Member Deposits Funds

Before the theory — here is what happens when the bank sends a deposit webhook:

```
Bank webhook ──► app-orchestration
                    │
                    ├─ [0.2ms] Redis: idempotency check, auth, limits
                    ├─ [0.5ms] Fee computation (in-process, no service calls)
                    ├─ [3ms]   Write outbox to PostgreSQL
                    │
                    └─► 202 Accepted ◄── BANK RECEIVES RESPONSE HERE (~4ms)

                    Then (async, without blocking the bank):
                    ├─ BANK_DEPOSIT → accounting worker → GL Phase A+B (TigerBeetle)
                    └─ WALLET_CREDIT → wallet worker → credit member e-wallet
```

Member sees funds in wallet once the wallet worker completes. GL is backoffice — records afterward, not a condition for returning the response.

---

## Overview

Two domains, two speeds, one orchestrator coordinating:

| Domain | Speed | Client waits? | Role |
|--------|-------|--------------|------|
| `core.wallet` — **E-Wallet** | Sync, < 10ms | **Yes** | Actual member wallet balance |
| `core.accounting` — **General Ledger** | Async, seconds | **No** | Accounting ledger, DR/CR entries |

`app-orchestration` is the sole bridge — the two domains never call each other directly, never JOIN their schemas.

---

## What Is the Data Plane?

Term borrowed from network infrastructure: **Data Plane** processes each packet as fast as possible; **Control Plane** makes routing decisions in the background.

In payments:

| | Data Plane | Control Plane |
|-|-----------|---------------|
| **Processing** | Each transaction, realtime, < 100ms | Batch, async, seconds→hours |
| **Example** | Auth check, e-wallet debit, route to bank | GL journal entry, audit log, reconciliation |
| **State access** | Redis pre-loaded (0.2ms/read) | PostgreSQL query (5-20ms/query) |
| **Failure impact** | Blocks customer transaction | Delays reporting only |

All pre-checks (auth, idempotency, limits, fraud) are **in-process function calls** — not network hops. State is pushed into Redis by the Control Plane before transactions arrive.

---

## Why Not Pure Microservices?

**Main reason: initial resources are very limited.**

Pure microservices require: service mesh, distributed tracing, per-service CI/CD pipeline, separate deployment, a dedicated ops team per service. For a small team in the early stage, operational cost exceeds the benefit — and that benefit only appears at a scale we don't yet have.

Data Plane solves this by keeping business logic in a single process, separating domains via **module boundary** (not network boundary) — independent enough to scale later, without ops cost now.

**Technical benefits that come with it:**

```
Pure microservice hot path (5–6 network hops):
  API GW → Auth Svc → Payment Svc → Fraud Svc → E-Wallet Svc → GL Svc
  Each hop = 5–15ms → total 40–90ms BEFORE calling the bank

Data Plane hot path (1 process):
  Orchestrator → [Redis checks in-process] → e-wallet DB write → Kafka emit
  ~3–7ms internal overhead, only 1 external hop (to bank)
```

---

## GtelPay Data Plane Architecture

```
External client / Bank webhook
    │ HTTP (s1-http-public — orchestration-public.yaml)
    ▼
┌─────────────────────────────────────────────────────┐
│              app-orchestration                       │
│  Redis pre-checks: auth, idempotency, limits, fraud  │
│  Fee computation (once, here only)                   │
│  1 PostgreSQL write (outbox / e-wallet)              │
│  Kafka emit (async, non-blocking)                    │
└──────────────┬──────────────────────┬───────────────┘
               │ HTTP (sync UC)       │ RabbitMQ (async UC)
    ┌──────────┴──────┐    ┌──────────┴──────────────────┐
    │   app-wallet    │    │    app-accounting-worker     │
    │   app-accounting│    │    app-wallet-worker         │
    └──────────┬──────┘    └──────────┬──────────────────┘
               │                      │
    ┌──────────┴──────┐    ┌──────────┴──────┐
    │  core.wallet    │    │ core.accounting  │
    │  E-Wallet       │    │ General Ledger   │
    │  (PostgreSQL    │    │ (PostgreSQL +    │
    │   :5432)        │    │  TigerBeetle)    │
    └─────────────────┘    └─────────────────┘
         │
    ┌────┴────────────┐
    │  Blnk Finance   │  ← S2: BlnkWalletGateway
    │  (PostgreSQL    │     replaces core.wallet
    │   :5433)        │
    └─────────────────┘
```

**Hot path (deposit):** outbox write + 202 = ~7ms internal. GL Phase A/B runs async afterward.

**Async path (RabbitMQ) — two independent consumer lanes:**

```
Outbox relay ──► BANK_DEPOSIT ──► app-accounting-worker ──► core.accounting
                                       (Phase A/B TB)
                                            │
                                  (orchestrator calls confirmDeposit)
                                            │
                                            ▼
Outbox relay ──► WALLET_CREDIT ──► app-wallet-worker ──► core.wallet
```

The two worker lanes are **completely independent** — lag or failure in the accounting worker does not affect the wallet worker, and vice versa.

---

## Module Layout

```
platform/
  core.sharedlib/          — shared value objects (Money, Currency, BusinessRef, IdempotencyKey)
  core.wallet/              — E-Wallet domain
  core.accounting/          — General Ledger domain
app/
  app-orchestration/        — Saga orchestrator; calls wallet and accounting via HTTP gateways
  app-wallet/               — HTTP gateway for wallet operations (sync, called by orchestrator)
  app-accounting/           — HTTP gateway for accounting/ledger operations (sync, called by orchestrator)
  app-wallet-worker/        — RabbitMQ consumer for WALLET_CREDIT commands (async)
  app-accounting-worker/    — RabbitMQ consumer for BANK_DEPOSIT commands (async)
```

**Key classes in `core.wallet`** (`platform/core.wallet/`):

| Class | Role |
|-------|------|
| `WalletCommandService` | Balance mutations — credit, debit, freeze, settle |
| `WalletBalanceMutator` | Executes balance update + writes `wallet_tx` in one DB transaction |
| `WalletQueryService` | Balance reads — current balance, point-in-time |
| `WalletCreditCommandListener` | Receives `WALLET_CREDIT` from RabbitMQ → calls `WalletCommandService` |
| `WalletHttpVerticle` | HTTP server (Vert.x) — exposes endpoint for `app-wallet` to call |
| `S1IdempotencyValidator` | Checks `UNIQUE(wallet_id, business_ref, tx_type)` before mutation |
| `WalletTxType` | Enum: `DEPOSIT_CREDIT`, `TRANSFER_DEBIT`, `WITHDRAW_FREEZE`, ... |

**Key classes in `core.accounting`** (`platform/core.accounting/`):

| Class | Role |
|-------|------|
| `JournalService` | GL journal posting — create, add lines, post, reverse |
| `JournalBalanceValidator` | Validates DR = CR before posting |
| `BankDepositCommandListener` | Receives `BANK_DEPOSIT` from RabbitMQ → triggers Phase A+B |
| `DepositPostingValidator` | Validates COA accounts and amounts for the deposit use case |
| `CoaTransEntity` | Journal header (1 per transaction) |
| `CoaTransDataEntity` | Journal lines (DR/CR entries, human-readable) |
| `CoaAccountEntity` | Chart of Accounts master data |

Each domain has **two separate entry points** — one for sync HTTP calls from orchestrator, one for async messages from RabbitMQ. Separated so that worker failure modes do not pull down the sync path SLA.

| Module | Type | Trigger | Calls into |
|--------|------|---------|------------|
| `app-wallet` | HTTP gateway | Sync HTTP call from orchestrator | `core.wallet` |
| `app-accounting` | HTTP gateway | Sync HTTP call from orchestrator | `core.accounting` |
| `app-wallet-worker` | Message consumer | `WALLET_CREDIT` from RabbitMQ | `core.wallet` |
| `app-accounting-worker` | Message consumer | `BANK_DEPOSIT` from RabbitMQ | `core.accounting` |

### Allowed Dependency Edges

| From | To | Allowed? |
|------|----|---------|
| `app-orchestration` | `app-wallet`, `app-accounting` | ✅ HTTP only |
| `app-wallet` | `core.wallet` | ✅ |
| `app-accounting` | `core.accounting` | ✅ |
| `app-wallet-worker` | `core.wallet` | ✅ |
| `app-accounting-worker` | `core.accounting` | ✅ |
| `core.wallet` | `core.accounting` | ❌ cross-domain forbidden |
| `core.accounting` | `core.wallet` | ❌ cross-domain forbidden |
| Any module | TigerBeetle (direct) | ❌ only `core.accounting` is allowed |

---

## The Two Core Domains

| Domain | Role | Question answered | Schema | ADR |
|--------|------|------------------|--------|-----|
| `core.wallet` | **E-Wallet** (hot path) | What is this member's spendable balance right now? | `wallet` | ADR-002 |
| `core.accounting` | **General Ledger — GL** (backoffice async) | What was posted to the GL? | `accounting` | ADR-003 |

**Hard boundaries (non-negotiable):**

- No cross-schema JOIN, no cross-domain import, no direct calls between them.
- `core.accounting` is the **only module** allowed to open a TigerBeetle client.
- Orchestration is the sole bridge between the two domains.

---

## TigerBeetle — ADR-037 Hybrid

TigerBeetle (TB) is a purpose-built financial database written in Zig, designed for double-entry bookkeeping at high throughput.

### Why TigerBeetle

- Native double-entry: Account + Transfer primitives — no aggregate SUM queries needed to get balance.
- O(1) balance reads — account `debits_posted`/`credits_posted` maintained atomically.
- u128 amounts (scale 4 for VND) — no floating-point risk.
- Deterministic consensus at 1M+ TPS on commodity hardware.
- Pure `pending` / `post_pending` / `void_pending` lifecycle for two-phase deposit/IBFT flows.

### Hybrid Layout (PostgreSQL + TigerBeetle)

PostgreSQL holds COA master data (`coa_account`) and the human-readable read model (`coa_trans_data`). TigerBeetle holds the hot-posting ledger. The accounting worker writes to both in the same logical write path — PostgreSQL first, then TB; if TB fails, the PostgreSQL entry is rolled back.

**Why not just use `SUM(amount)` on `coa_trans_data`?** PostgreSQL aggregate reads do not scale to O(1) at high throughput. TigerBeetle accounts maintain a running balance in hardware-atomic integers — balance read is a single TB account lookup.

### Pending/Post/Void Lifecycle

| TB Operation | Use case trigger | Effect |
|-------------|-----------------|--------|
| `create_transfer (PENDING)` | Deposit Phase A, IBFT freeze | Reserves amount — not yet posted to account balance |
| `post_pending_transfer` | Deposit Phase B, IBFT settle | Commits reserved amount to account balance |
| `void_pending_transfer` | IBFT release | Cancels reservation — amount returned |

---

## Blnk Finance — Wallet Engine

Blnk Finance 0.14.5 runs at `localhost:5002`. This is an open-source Go ledger engine.

### S1 vs S2

| | S1 (current) | S2 (planned) |
|---|---|---|
| **Wallet backend** | `core.wallet` (PostgreSQL :5432) | `BlnkWalletGateway implements WalletGateway` → Blnk API |
| **Balance engine** | PostgreSQL aggregate + snapshot | Blnk internal (PostgreSQL :5433 Docker) |
| **Migration trigger** | — | P8: implement `BlnkWalletGateway` |

### Blnk Patterns Ported to Java

Two patterns from Blnk have been ported into `core.wallet` as a proof-of-concept:

| Pattern | Class | What it does |
|---------|-------|-------------|
| Balance monitor | `WalletBalanceMonitor` | Listens for Spring `ApplicationEvent` on threshold breach; triggers alert or limit enforcement |
| Point-in-time balance | `WalletQueryService.getBalanceAt(walletId, timestamp)` | Reconstructs wallet balance at any historical timestamp using snapshot + tx replay |

**PoC scope limits:**
- No Blnk binary — patterns only, adapted to Java.
- `WalletBalanceMonitor` is a PoC alert hook — not a blocking transaction guard.
- Does not replace `core.wallet` in S1 — Blnk is a reference, not a dependency.

---

## Hot Path Flow: Deposit (UC-1)

Step-by-step to understand the full data plane in action:

```
1. Bank webhook → s1-http-public → app-orchestration
   │
   ├─ Redis: idempotency check (businessRef seen?)
   ├─ Redis: auth/VA lookup
   ├─ Redis: member limit check
   │
2. app-orchestration: compute fee (normalizeAllowZero for zero fee)
   │
3. app-orchestration: outbox write (PostgreSQL, 1 DB tx)
   │   → response 202 Accepted immediately to bank webhook
   │
4. Outbox relay → RabbitMQ: BANK_DEPOSIT command
   │
5. app-accounting-worker receives BANK_DEPOSIT
   ├─ Phase A: create_transfer PENDING (TigerBeetle)
   │            + INSERT coa_trans_data (PostgreSQL)
   ├─ confirmDeposit signal → app-orchestration
   │
   ├─ Phase B: post_pending_transfer (TigerBeetle)
   │            + UPDATE coa_trans_data.status = POSTED
   │            + emit JournalPostedEvent (Kafka)
   │
6. Orchestrator receives JournalPostedEvent
   │   → publish WALLET_CREDIT command (RabbitMQ)
   │
7. app-wallet-worker receives WALLET_CREDIT
   ├─ credit wallet_balance.available
   ├─ INSERT wallet_tx (DEPOSIT_CREDIT)
   └─ emit WalletCreditedEvent (Kafka)
```

**Client receives 202 at step 3.** Everything from step 4 onward is backoffice — member sees funds when step 7 completes.

---

## Failure Modes and Recovery

| Failure point | Behavior | Recovery |
|--------------|---------|---------|
| TB Phase A fail | PostgreSQL write rolled back; BANK_DEPOSIT requeued (at-least-once) | Worker retry with idempotency key |
| TB Phase B fail | Phase A entry persists (PENDING); orchestrator does not receive confirmDeposit | Dead letter queue → ops review; manual void if needed |
| Kafka emit fail | Event does not reach downstream | Outbox pattern: retry relay until acked |
| Worker lag | Queue builds up | GL backlog does not affect member balance — already credited in S1 |
| Duplicate BANK_DEPOSIT | `businessRef` idempotency key detects duplicate | Return existing outcome, no new journal entry created |
| WALLET_CREDIT retry | `UNIQUE(wallet_id, business_ref, tx_type)` | Return existing `wallet_tx` row, `idempotentReplay=true` |

---

## Integration Surfaces (Internal)

### Inbound Commands (consumed by workers)

| Command | Consumer | Purpose |
|---------|---------|---------|
| `BANK_DEPOSIT` | `app-accounting-worker` | Triggers Phase A/B accounting for inbound deposit |
| `WALLET_CREDIT` | `app-wallet-worker` | Credits net amount to member wallet after Phase B |

### Outbound Events (published by workers)

| Event | Publisher | Meaning |
|-------|---------|---------|
| `JournalPostedEvent` | `app-accounting-worker` | Phase B committed to TigerBeetle + PostgreSQL |
| `WalletCreditedEvent` | `app-wallet-worker` | Wallet balance updated successfully |
| `CommandFailedEvent` | Any worker | Command exhausted retries; requires ops intervention |

### HTTP Surfaces

| Gateway | Module | Callers |
|---------|--------|---------|
| `WalletGateway` | `app-wallet` | `app-orchestration` only |
| `LedgerGateway` | `app-accounting` | `app-orchestration` only |

**Messaging direction rule:**
- **RabbitMQ (s6) = inbound commands** — orchestration → workers (`BANK_DEPOSIT`, `WALLET_CREDIT`)
- **Kafka (s3) = outbound events** — workers → downstream after processing (`JournalPosted`, `WalletCredited`)

Core services receive commands via RabbitMQ, emit events via Kafka. Kafka does not carry commands into core.

---

## Inbound/Outbound Chain per Service

Each service has exactly one inbound and one outbound. This is intentional — it keeps retry, DLQ, and monitoring scoped to one entry point per service.

```
[Bank / External / Mobile]
        │
        │ inbound: HTTP / gRPC
        ▼
  app-orchestration
        │
        │ outbound: outbox → RabbitMQ BANK_DEPOSIT (async)
        │ outbound: HTTP → app-accounting / app-wallet (sync UCs)
        ▼
  app-accounting-worker
        │ inbound:  RabbitMQ BANK_DEPOSIT
        │ outbound: RabbitMQ WALLET_CREDIT  ← command to wallet worker
        │ outbound: Kafka JournalPostedEvent (optional — reporting/audit)
        ▼
  app-wallet-worker
        │ inbound:  RabbitMQ WALLET_CREDIT
        │ outbound: Kafka WalletCreditedEvent (optional — reporting/audit)
```

| Service | Inbound | Outbound |
|---------|---------|----------|
| `app-orchestration` | HTTP/gRPC from external | RabbitMQ `BANK_DEPOSIT` (outbox); HTTP to `app-accounting` / `app-wallet` |
| `app-accounting-worker` | RabbitMQ `BANK_DEPOSIT` | RabbitMQ `WALLET_CREDIT`; Kafka `JournalPostedEvent` (optional) |
| `app-wallet-worker` | RabbitMQ `WALLET_CREDIT` | Kafka `WalletCreditedEvent` (optional) |
| `app-accounting` | HTTP from orchestrator | *(sync response only)* |
| `app-wallet` | HTTP from orchestrator | *(sync response only)* |

**Rule:** RabbitMQ carries commands (inbound work to be done). Kafka carries events (outbound facts that already happened). A service never publishes a RabbitMQ command to itself, and never reads Kafka as its primary trigger.

---

## Latency Budget

| Component | Microservice | Data Plane |
|-----------|-------------|-----------|
| Pre-checks (auth, idempotency, limits, fraud) | 40ms (4× network+DB) | 0.8ms (4× Redis) |
| Business logic | 10ms (network) | ~0ms (in-process) |
| E-wallet write | 15ms (network+DB) | 3ms (in-process PostgreSQL) |
| GL journal entry + audit + notification | 35ms (network) | ~0ms (async Kafka) |
| **Total internal** | **~100ms** | **~4ms** |

**Measured baseline (k6):** Transfer 100 RPS, 60s → 0% error, p95=4ms, p99<5ms ✓ (2026-06-22)

---

## Control Plane Contracts

```
CP → DP:  Push state to Redis before transactions arrive.
          Never insert yourself in the request path.

DP → CP:  Emit events to Kafka.
          Never wait for acknowledgment.
          If Kafka unreachable → write to outbox; relay when available.

Rule:     DP never calls CP synchronously.
          CP never handles live customer traffic.
```

**Redis pre-loaded state — what the Control Plane must push before transactions arrive:**

| Key type | TTL | Pushed by |
|----------|-----|---------|
| Auth token → member mapping | 15 min | Auth service (login) |
| Virtual account → member mapping | No expiry | VA provisioning |
| Member limit config | 5 min | Limit management |
| Fraud rule flags | 1 min | Fraud engine |

---

## Configuration: File-first vs DB

**Principle:** prefer file config first; use DB config only when runtime changes are needed without redeployment.

| | File config (`application.yml`, ConfigMap) | DB config (table in PostgreSQL) |
|---|---|---|
| **When read** | Startup; no DB hit on hot path | Requires DB query or Redis cache |
| **Changes** | Requires redeploy (or ConfigMap reload) | Runtime — no redeploy needed |
| **Latency** | 0ms (in-memory after startup) | 0ms if pre-loaded into Redis; 5–20ms if querying DB directly |
| **Audit trail** | Git history | Requires separate audit table |
| **Suitable for** | Fee rate, routing rule, stable feature flags | Member limits, fraud rules that operators need to change quickly |

**Choosing between them:**

- **File config** — sufficient for everything that does not change after deploy: database URL, queue name, service endpoint, static fee schedule. Zero overhead, zero DB dependency.
- **DB config** — only when operators need to change without triggering a redeploy: per-member limit, dynamic fee tier, fraud threshold. Must be pre-loaded into Redis before transactions arrive — do not query DB directly on the hot path.
- **Do not use DB config as source of truth for the hot path.** If Redis dies and DB config has not been pre-loaded → hot path fails. File config has no such risk.

**Default rule:** start with file config. Switch to DB config only when a specific use case requires runtime change.

---

## 8 Governing Principles (Constitution v1.1.0)

Every design change must validate all 8 principles. An ADR amendment is required if any principle is modified.

| # | Principle | Rule summary | ADR |
|---|-----------|-------------|-----|
| I | Two-Domain Separation | wallet and accounting isolated schemas; no cross-domain JOIN/FK/import; communication via message or orchestrator HTTP | ADR-002, ADR-003 |
| II | Immutable Balanced Ledger | Posted journal lines are append-only; corrections via reversing journal; balance equation enforced at write time | ADR-001 |
| III | Wallet Hot Path | `wallet_balance` is the authoritative spendable snapshot — not re-derived on each read; balance change writes exactly one `wallet_tx` in the same DB transaction | ADR-004 |
| IV | Idempotency End-to-End | Every command keyed on `businessRef` (= `X-Idempotency-Key`); duplicate delivery → identical outcome, no duplicate state | ADR-005 |
| V | Orchestration Sole Sequencer | Saga orchestrator sequences all multi-step flows; outbox at-least-once delivery; no 2PC or distributed transaction | ADR-006, ADR-013 |
| VI | Money and Currency Discipline | VND single-currency v1; amounts are `BigDecimal` scale 4 `HALF_UP` at domain boundary; fees computed once at orchestration | ADR-019, ADR-028 |
| VII | Contracts and Conformance Source of Truth | OpenAPI/AsyncAPI YAML is the wire source of truth; SQL ledger invariants run in CI — build fails on drift | ADR-018, ADR-031 |
| VIII | Fail-Fast at Boundaries | Validate at the earliest entry point before any mutation; reject with explicit error codes; do not propagate invalid input into inner layers | ADR-007, ADR-010, ADR-011, ADR-019, ADR-029 |

---

## Trade-offs

| Trade-off | Gain | Cost |
|-----------|------|------|
| Redis pre-loaded state | Sub-ms checks on every transaction | ~1 min/year outage risk; Redis HA investment required |
| GL async (eventual) | Hot path < 100ms; e-wallet response does not wait for GL | GL lags seconds; reconciliation catches drift |
| Business logic concentrated in orchestrator | No inter-service latency | Orchestrator is larger, more complex |
| E-wallet = only DB write on hot path | Minimal disk I/O on hot path | Must shard e-wallet DB at scale |
| Module boundary (not network boundary) | Full ops stack from day one — no service mesh needed | Modules in same process — must enforce via POM and arch tests |

---

## Source of Truth

`claude/confluence/` is the local source of truth. Confluence is a mirror — when there is a conflict, **local wins**. Do not edit directly on CF (breaks sync).

---

## Navigation — Documents in This Section

| Section | Page | Content |
|---------|------|---------|
| 📌 Start Here | Architecture FAQ | Module ownership, HTTP vs queue, Q&A, anti-patterns |
| 📌 Start Here | Terminology Reference | Platform naming vs TRD/REST mapping |
| 🏛️ Architecture & Principles | E-Wallet & General Ledger Architecture Overview | Two-domain, hot path vs backoffice, surface map |
| 🏛️ Architecture & Principles | Platform Boundaries | Hard rules, allowed call paths, E-Wallet vs GL |
| 🏛️ Architecture & Principles | Correlation & Idempotency | businessRef flow end-to-end |
| 🏛️ Architecture & Principles | I/O Bound vs CPU Bound | Thread model, Vert.x event loop, I/O vs CPU classification per module |
| 📋 Domain TRDs | Accounting TRD | FR/NFR core.accounting (GL), two-phase posting |
| 📋 Domain TRDs | Wallet TRD | FR/NFR core.wallet (E-Wallet), balance invariants |
| 🔌 Contracts & Integration | Integration Surfaces | s1–s6 surface catalog, use case matrix |
| 🔌 Contracts & Integration | Core Platform Design | COA, fund flow, GL DR/CR entries |
| 🏗️ Build & Process | Business Process: Deposit | Full deposit flow, GL DR/CR, non-happy paths |
| 🏗️ Build & Process | Implementation Decisions | Module layout, build phases, DDL decisions |
| 🔄 Use Cases | UC-1 through UC-9 | Orchestration contracts per use case |
| ✅ Conclusion | Conclusion | 3 core decisions, 7 principles recap, next steps |
