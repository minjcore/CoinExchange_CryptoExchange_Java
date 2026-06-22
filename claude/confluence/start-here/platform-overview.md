# Platform Overview — How Modules Talk

> **CF page ID:** 51839264 | **Parent:** 📌 Start Here (51315064)
> **Source of truth:** this file → push to CF via updateConfluencePage

---

## Why an orchestrator? — Integration Surface

`app-orchestration` is the system's **sole integration surface** — the only contact point between the external world and `core.accounting` + `core.wallet`. It does not care which protocol is exposed (HTTP, gRPC, or anything else) — protocol is an implementation detail, not the essence.

### Where it sits

```
external world
      │
      ▼
┌─────────────────────┐
│  app-orchestration  │  ← integration surface
│                     │
│  knows: use case    │
│  knows: caller intent│
│  knows: fee policy  │
│  knows: VA mapping  │
└──────┬──────────────┘
       │
  ┌────┴────────────┐
  ▼                 ▼
core.accounting   core.wallet
(accounting       (wallet
 ledger)           ledger)
  │
  knows: double-entry
  knows: TigerBeetle
  knows: PENDING/POSTED
  knows: COA accounts

                   │
                   knows: available/frozen balance
                   knows: wallet_tx
                   knows: freeze/unfreeze/credit/debit
```

### Exactly 4 responsibilities

| # | Responsibility | Example |
|---|---------------|---------|
| 1 | **Auth + validate inbound request** | Verify bank mTLS cert, validate businessRef format |
| 2 | **Resolve context** — translate external identity to internal identity | VA `VCB-001` → `memberId=1001`, `walletId=5001` |
| 3 | **Apply policy** — compute once before calling any domain | Fee = `grossAmount × 1%`, capped at 10,000 VND |
| 4 | **Sequence domain calls** — call the right domain in the right order, domains never call each other | Deposit: outbox → accounting worker → wallet worker (in order, with gate) |

### What it does NOT do

- **Does not write directly to `coa_*` or `wallet_*`** — that is the domain's job
- **Does not know TigerBeetle exists** — implementation detail of `core.accounting`
- **Does not recompute fee in workers** — fee is computed once here; workers receive the result
- **Does not implement domain business rules** — "transit 3100 must equal 0" is a `core.accounting` rule, not orchestration's

### Without this layer

| Problem | Consequence |
|---------|------------|
| `core.accounting` must resolve VA → memberId itself | Pure domain is contaminated with integration concerns |
| Bank webhook calls accounting worker directly | Coupling: change bank protocol → modify accounting |
| Fee computation lives in the domain | Each use case computes fee differently, inconsistent |
| Mobile app knows about RabbitMQ and TigerBeetle | Internal topology is exposed to callers |

`core.accounting` and `core.wallet` only receive data that has already been resolved and validated — they have no knowledge of who the caller is or what protocol was used.

---

## The Big Picture

GtelPay platform has **8 deployable modules** split into two layers:

- **Ledger libraries** (`core.*`) — pure Java, no framework, own the ledger rules and data
  - `core.accounting` = **accounting ledger** — double-entry, COA, TigerBeetle, PENDING/POSTED
  - `core.wallet` = **wallet ledger** — available/frozen balance, wallet_tx, mutation rules
- **Application shells** (`app-*`) — Spring Boot, wire ledger to HTTP or queue

No `core.*` module imports another `core.*` module. They share only `core.sharedlib`.

---

## Module Map

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         EXTERNAL                                        │
│   Bank / NAPAS webhook     Mobile app / Partner API    Internal service │
│         │                         │                          │          │
│         │ POST /deposits/notify   │ HTTPS / gRPC             │ gRPC     │
│         └─────────────┬───────────┴──────────────────────────┘          │
│                       ▼                                                 │
│              ┌──────────────────────────┐                               │
│              │     app-orchestration    │  ← sole entry point all UCs  │
│              │  HTTP (public + internal)│    computes fee, resolves VA  │
│              │  gRPC (internal callers) │                               │
│              └──────┬───────────────────┘                               │
│                     │                                                   │
│          ┌──────────┴──────────┐                                        │
│          │  ASYNC path         │  SYNC path                             │
│          │  (deposit)          │  (payment, transfer, withdraw)         │
│          ▼                     ▼                                        │
│   outbox (Postgres)     ┌──────────────┐   ┌──────────────┐            │
│          │              │ app-accounting│   │  app-wallet  │            │
│          │ RabbitMQ     │ (HTTP sync)  │   │ (HTTP sync)  │            │
│          ▼              └──────┬───────┘   └──────┬───────┘            │
│   ┌─────────────────┐          │                   │                    │
│   │ app-accounting  │          ▼                   ▼                    │
│   │ -worker         │   core.accounting        core.wallet              │
│   │ (RabbitMQ)      │   ─────────────────      ──────────────           │
│   └────────┬────────┘   accounting ledger       wallet ledger           │
│            │            TigerBeetle (hot)        wallet_balance          │
│            │            coa_trans (PG)           wallet_tx (PG)          │
│            │                                                             │
│            │  Phase A+B complete → publish WALLET_CREDIT                │
│            ▼                                                             │
│   ┌─────────────────┐                                                   │
│   │ app-wallet      │                                                   │
│   │ -worker         │──► core.wallet (credit after POSTED only)         │
│   │ (RabbitMQ)      │                                                   │
│   └─────────────────┘                                                   │
│                                                                          │
│   (optional) Kafka events: JournalPosted, WalletCredited                │
│   → reporting, audit, notification service                               │
└──────────────────────────────────────────────────────────────────────────┘
```

---

## Who Talks to Whom — Protocol Summary

| From | To | Protocol | When |
|------|----|----------|------|
| Bank / NAPAS | `app-orchestration` | HTTPS (mTLS) | Deposit webhook |
| Mobile / partner | `app-orchestration` | HTTPS | Payment, transfer, withdraw, balance |
| Internal service (BFF, etc.) | `app-orchestration` | **gRPC** | High-throughput or streaming internal calls |
| `app-orchestration` | `app-accounting` | HTTP (internal) | Sync use cases only (not deposit) |
| `app-orchestration` | `app-wallet` | HTTP (internal) | Sync: freeze, balance read |
| `app-orchestration` | RabbitMQ outbox | Transactional outbox → RabbitMQ | Deposit async kickoff |
| RabbitMQ | `app-accounting-worker` | RabbitMQ consume | `BANK_DEPOSIT` command |
| `app-accounting-worker` | RabbitMQ | RabbitMQ publish | `WALLET_CREDIT` command after POSTED |
| RabbitMQ | `app-wallet-worker` | RabbitMQ consume | `WALLET_CREDIT` command |
| `app-accounting-worker` | Kafka (optional) | Kafka publish | `JournalPosted` event |
| `app-wallet-worker` | Kafka (optional) | Kafka publish | `WalletCredited` event |

**gRPC on orchestrator — scope:**
- Exposed on a separate port (default: 9090) alongside HTTP (8080)
- Serves the same use cases as HTTP internal: payment, transfer, withdraw, balance read
- Does NOT replace the HTTPS public endpoint (bank webhook stays HTTP)
- Does NOT talk to `app-accounting` or `app-wallet` via gRPC — those downstream calls stay HTTP internal
- Proto contracts live in `specs/contracts/grpc/orchestration.proto`

**Hard rules (unchanged):**
- `app-orchestration` never writes to `coa_*` or `wallet_*` tables directly
- `app-wallet-worker` never calls `app-accounting` — no cross-domain HTTP
- `core.accounting` is the only module that opens a TigerBeetle client
- `core.wallet` never imports `core.accounting`

---

## Deposit Flow Step-by-Step

This is the most complex flow — all others (payment, transfer, withdraw) are simpler synchronous variants.

```
1.  Bank sends POST /deposits/notify
2.  app-orchestration: auth → VA lookup → fee compute → write outbox → return 202
3.  Outbox relay publishes BANK_DEPOSIT to RabbitMQ
4.  app-accounting-worker receives BANK_DEPOSIT:
      Phase A: createJournal(PENDING) — TB pending transfer 1111→3100
      Phase B: confirmDeposit(fee)   — TB post + 3100→2110(net) + 3100→4110(fee)
              validate: account[3100] = 0
              update coa_trans → POSTED
5.  app-accounting-worker publishes WALLET_CREDIT to RabbitMQ
6.  app-wallet-worker receives WALLET_CREDIT:
      gate: coa_trans.status = POSTED
      INSERT wallet_tx (DEPOSIT_CREDIT, net amount)
      UPDATE wallet_balance.available += net
7.  Member sees updated balance (within 5s of POSTED)
```

Key property: step 2 (202 back to bank) happens **before** steps 3–7. The bank gets its ack fast; ledger and wallet update async.

**Retry-safe at every step — return existing immediately with IDs:**

| Step | Retried with same data | Response |
|------|----------------------|---------|
| Step 1 re-sent (bank retry) | Same `X-Idempotency-Key` → outbox already exists | **202** + same `businessRef` immediately, no new outbox row |
| Step 4 re-delivered (RabbitMQ redeliver) | Same `(reference_id, use_case)` → `coa_trans` exists | Return existing **`coaTransId`**, skip Phase A/B |
| Step 6 re-delivered | Same `(wallet_id, business_ref, tx_type)` → `wallet_tx` exists | Return existing **`walletTxId`**, `idempotentReplay=true`, balance unchanged |

> **Rule:** Replay response MUST include the existing transaction IDs (`coaTransId`, `walletTxId`) — not just a status flag. Caller uses these IDs to confirm the transaction completed and to correlate with downstream systems.

Any step can be retried with original data — the chain self-heals without double-effect. (ADR-005, ADR-013, ADR-021)

---

## Sync Flows (Payment / Transfer / Withdraw)

These go directly orchestration → HTTP gateway → domain, no queue.
Callers may use **HTTP or gRPC** to reach orchestration — the downstream path is identical either way.

```
Payment:
  caller (HTTP or gRPC) → app-orchestration
    → POST app-accounting/journals (create+post sync)
    → POST app-wallet/debit (USER) + POST app-wallet/credit (MERCHANT)

Transfer:
  caller (HTTP or gRPC) → app-orchestration
    → POST app-accounting/journals (transit 3300)
    → POST app-wallet/debit (A) + POST app-wallet/credit (B)

Withdraw:
  caller (HTTP or gRPC) → app-orchestration
    → POST app-wallet/freeze (hold funds)
    → return 200/OK to caller
    → (later) POST outbox WITHDRAW_PAYOUT → worker → bank payout
```

---

## Data Store Ownership

| Store | Owner | No one else may... |
|-------|-------|--------------------|
| `accounting` schema (PG) | `core.accounting` | write `coa_*` from outside |
| TigerBeetle cluster | `core.accounting` | open TB client from another module |
| `wallet` schema (PG) | `core.wallet` | write `wallet_*` from outside |
| `outbox` table | `app-orchestration` | — (relay reads it, but domain owns it) |

---

## The One Rule to Remember

> **businessRef** threads the entire flow end-to-end.
> It is the `X-Idempotency-Key` on the HTTP API, the metadata key on gRPC calls, the `businessRef` in the RabbitMQ envelope, the `reference_id` on `coa_trans`, and the `business_ref` on `wallet_tx`.
> Any retry at any layer is safe because every write is keyed on it.
