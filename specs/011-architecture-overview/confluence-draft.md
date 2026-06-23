# Platform Architecture Overview — Two-Domain, TigerBeetle, Blnk PoC

> **CF page ID:** 51609646 | **Parent:** 🏛️ Architecture & Principles (51642382)
> **Source of truth:** `specs/011-architecture-overview/confluence-draft.md` → push to CF

**Version:** 1.0.0 | **Reflects:** Constitution v1.0.0 (ratified 2026-06-15) | **Last reviewed:** 2026-06-18  
**Source of truth:** `specs/011-architecture-overview/` and `.specify/memory/constitution.md`

> This document covers the **target architecture** (design intent). Migration debt and legacy wiring are noted but not the focus. Bank/NAPAS external surfaces are explicitly out of scope — only internal boundaries are documented here.

---

## 1. Two-Domain Separation

GtelPay Core is split into exactly **two domains** that never share a schema, never JOIN across each other, and never call each other directly.

| Domain | Question it answers | Schema | Owns |
|--------|--------------------|---------|----- |
| `core.wallet` | "How much can this member spend right now?" | `wallet` | wallet, wallet_tx, wallet_balance_snapshot, member_limit |
| `core.accounting` | "What happened in the ledger?" | `accounting` | coa_account, coa_trans_data, accounting_period, tb_account_ref |

### Boundary Rules (ADR-002, ADR-003)

- No cross-schema foreign keys or JOINs.
- No direct domain → domain method calls.
- Communication via message (RabbitMQ command/event) or orchestrator HTTP call only.
- `core.accounting` is the **only** module that opens a TigerBeetle client.

---

## 2. Module Layout

```
core/
  core.sharedlib/          — shared value objects (Money, Currency, BusinessRef, IdempotencyKey)
  core.wallet/              — wallet domain (WalletService, WalletTxRepository)
  core.accounting/          — accounting domain (LedgerService, TigerBeetleGateway, JournalRepository)
app/
  app-orchestration/        — Saga orchestrator; calls wallet and accounting via HTTP gateways
  app-wallet/               — HTTP gateway for wallet operations (sync, called by orchestrator)
  app-accounting/           — HTTP gateway for accounting/ledger operations (sync, called by orchestrator)
  app-wallet-worker/        — RabbitMQ consumer for WALLET_CREDIT commands (async)
  app-accounting-worker/    — RabbitMQ consumer for BANK_DEPOSIT commands (async)
```

### HTTP Gateways vs. Message Consumers

Each domain has two separate entry points — one for sync HTTP calls from the orchestrator, one for async messages from RabbitMQ. These two are separated so that worker failure modes do not drag down the sync path SLA.

| Module | Type | Trigger | Calls into |
|--------|------|---------|------------|
| `app-wallet` | HTTP gateway | Sync HTTP call from orchestrator | `core.wallet` |
| `app-accounting` | HTTP gateway | Sync HTTP call from orchestrator | `core.accounting` |
| `app-wallet-worker` | Message consumer | `WALLET_CREDIT` command from RabbitMQ | `core.wallet` |
| `app-accounting-worker` | Message consumer | `BANK_DEPOSIT` command from RabbitMQ | `core.accounting` |

### Allowed Dependency Edges

| From | To | Allowed? |
|------|----|---------|
| `app-orchestration` | `app-wallet`, `app-accounting` | Yes — HTTP only |
| `app-wallet` | `core.wallet` | Yes |
| `app-accounting` | `core.accounting` | Yes |
| `app-wallet-worker` | `core.wallet` | Yes |
| `app-accounting-worker` | `core.accounting` | Yes |
| `core.wallet` | `core.accounting` | **No** — cross-domain forbidden |
| `core.accounting` | `core.wallet` | **No** — cross-domain forbidden |
| Any module | TigerBeetle (direct) | **No** — only `core.accounting` |

---

## 3. Data-Plane Architecture Diagram

```
                    ┌─────────────────────────────────────────┐
                    │          app-orchestration               │
                    │  (Saga; 1 DB write; Redis pre-loaded)   │
                    └──────────┬──────────────┬───────────────┘
                               │              │
                     HTTP call │              │ HTTP call
                               ▼              ▼
                    ┌──────────────┐  ┌──────────────────┐
                    │  app-wallet  │  │  app-accounting  │
                    │ (HTTP gateway│  │  (HTTP gateway)  │
                    └──────┬───────┘  └──────┬───────────┘
                           │                  │
                           ▼                  ▼
                    ┌──────────────┐  ┌──────────────────┐
                    │ core.wallet  │  │ core.accounting  │
                    │ (PostgreSQL) │  │ (PostgreSQL +    │
                    │             │  │  TigerBeetle)    │
                    └─────────────┘  └──────────────────┘

Async path (RabbitMQ) — two independent consumer lanes:

  Outbox relay ──► BANK_DEPOSIT ──► app-accounting-worker ──► core.accounting
                                         (Phase A/B TB)
                                              │
                                    (orchestrator calls confirmDeposit)
                                              │
                                              ▼
  Outbox relay ──► WALLET_CREDIT ──► app-wallet-worker ──► core.wallet
```

**Data-plane vs microservice**: single in-process orchestrator + Redis pre-loaded state + 1 PostgreSQL write per request. No multi-hop latency on the sync path. Async lanes (workers) are isolated — a worker lag or failure does not affect sync P99.

---

## 4. TigerBeetle (ADR-037 Hybrid)

TigerBeetle (TB) is a purpose-built financial database written in Zig, designed for double-entry bookkeeping at high throughput.

### Why TigerBeetle

- Native double-entry: Account + Transfer primitives; no aggregate SUM queries for balances.
- O(1) balance reads — account `debits_posted`/`credits_posted` maintained atomically.
- u128 amounts (scale 4 for VND) — no floating-point risk.
- Deterministic consensus at 1M+ TPS on commodity hardware.
- Pure `pending` / `post_pending` / `void_pending` lifecycle for two-phase deposit/IBFT flows.

### ADR-037 Hybrid Layout

PostgreSQL retains COA master data (`coa_account`) and the human-readable read model (`coa_trans_data`). TigerBeetle holds the hot-posting ledger. The accounting worker writes to both in the same logical write path — PostgreSQL first, then TB; if TB fails the PostgreSQL entry is rolled back.

### Pending/Post/Void Lifecycle

| TB Operation | Use case trigger | Effect |
|-------------|-----------------|--------|
| `create_transfer (PENDING)` | Deposit Phase A, IBFT freeze | Reserves amount; does not post to account balance yet |
| `post_pending_transfer` | Deposit Phase B, IBFT settle | Commits reserved amount to account balance |
| `void_pending_transfer` | IBFT release | Cancels reservation; amount returned |

**Why not `SUM(amount)` over `coa_trans_data`?** PostgreSQL aggregate reads do not scale to O(1) at high throughput. TigerBeetle accounts maintain a running balance in hardware-atomic integers — balance read is a single TB account lookup.

---

## 5. Blnk PoC (core.wallet)

Blnk is an open-source Go ledger engine. Two patterns were ported to Java `core.wallet` as a proof-of-concept.

### Ported Patterns

| Pattern | Class | What it does |
|---------|-------|-------------|
| Balance monitor | `WalletBalanceMonitor` | Listens for Spring `ApplicationEvent` on threshold breach; triggers alert or limit enforcement |
| Point-in-time balance | `WalletQueryService.getBalanceAt(walletId, timestamp)` | Reconstructs wallet balance at any historical timestamp using snapshot + tx replay |

### Scope Limits

- No Blnk binary — only patterns are adapted.
- Not the production path for balance computation — `core.wallet` computes balances directly.
- Not a replacement for `core.wallet` in v1 — Blnk is a reference, not a dependency. S2 plan: `BlnkWalletGateway implements WalletGateway` (requires ADR before implementation).
- `WalletBalanceMonitor` is a PoC alert hook, not a blocking transaction guard.

---

## 6. Integration Surfaces (Internal Only)

All surfaces listed here are **internal**. Bank/NAPAS external adapters are handled by separate infrastructure and are out of scope for this document.

### Inbound Commands (consumed by workers)

| Command | Consumer | Purpose |
|---------|---------|---------|
| `BANK_DEPOSIT` | `app-accounting-worker` | Trigger Phase A/B accounting for an inbound deposit |
| `WALLET_CREDIT` | `app-wallet-worker` | Credit net amount to member wallet after accounting Phase B |

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

---

## 7. Constitution v1.1.0 — 8 Governing Principles

Every design change MUST be validated against all 8 principles. An ADR amendment is required if any principle is altered.  
Source: `.specify/memory/constitution.md`, ratified 2026-06-15, amended 2026-06-19.

| # | Principle | Rule summary | ADR |
|---|-----------|-------------|-----|
| I | Two-Domain Separation | wallet and accounting are isolated schemas; no cross-domain JOIN/FK/import; communication via message or orchestrator HTTP only | ADR-002, ADR-003 |
| II | Immutable Balanced Ledger | Posted journal lines are append-only; corrections via reversing journal; balance equation enforced at write time | ADR-001 |
| III | Wallet Hot Path | `wallet_balance` is the authoritative spendable snapshot read as a single row — never re-derived per read; balance change writes exactly one `wallet_tx` in the same DB transaction | ADR-004 |
| IV | Idempotency End-to-End | Every command keyed on `businessRef` (equals `X-Idempotency-Key`); duplicate delivery produces identical outcome, no duplicate state | ADR-005 |
| V | Orchestration Sole Sequencer | Saga orchestrator sequences all multi-step flows; outbox at-least-once delivery; no 2PC or distributed transaction | ADR-006, ADR-013 |
| VI | Money and Currency Discipline | VND single-currency v1; amounts are `BigDecimal` scale 4 `HALF_UP` at domain boundary; fees computed once at orchestration | ADR-019, ADR-028 |
| VII | Contracts and Conformance Source of Truth | OpenAPI/AsyncAPI YAML is the wire source of truth; SQL ledger invariants run in CI and fail the build on drift | ADR-018, ADR-031 |
| VIII | Fail-Fast at Boundaries | Validate at the earliest entry point before any mutation; reject with explicit error codes; never propagate invalid input to inner layers | ADR-007, ADR-010, ADR-011, ADR-019, ADR-029 |

> **Amendment rule**: when this document is updated following an ADR amendment, the version/date at the top MUST be incremented. The corresponding ADR file in `adr/` must also be updated — these two documents move together.
