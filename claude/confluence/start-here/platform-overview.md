# Platform Overview — How Modules Talk

> **CF page ID:** 51839264 | **Parent:** 📌 Start Here (51315064)
> **Source of truth:** this file → push to CF via updateConfluencePage

---

## Tại sao có orchestrator? — Integration Surface

`app-orchestration` là **integration surface duy nhất** của hệ thống — điểm tiếp xúc duy nhất giữa external world và `core.accounting` + `core.wallet`. Nó không quan tâm protocol nào được expose (HTTP, gRPC, hay cái gì khác) — protocol là detail triển khai, không phải bản chất.

### Vị trí của nó

```
external world
      │
      ▼
┌─────────────────────┐
│  app-orchestration  │  ← integration surface
│                     │
│  biết: use case     │
│  biết: caller intent│
│  biết: fee policy   │
│  biết: VA mapping   │
└──────┬──────────────┘
       │
  ┌────┴────────────┐
  ▼                 ▼
core.accounting   core.wallet
(accounting       (wallet
 ledger)           ledger)
  │
  biết: double-entry
  biết: TigerBeetle
  biết: PENDING/POSTED
  biết: COA accounts

                   │
                   biết: available/frozen balance
                   biết: wallet_tx
                   biết: freeze/unfreeze/credit/debit
```

### Nhiệm vụ cụ thể — 4 việc, không hơn

| # | Nhiệm vụ | Ví dụ |
|---|----------|-------|
| 1 | **Auth + validate inbound request** | Verify mTLS cert của bank, validate businessRef có format đúng không |
| 2 | **Resolve context** — translate external identity sang internal identity | VA `VCB-001` → `memberId=1001`, `walletId=5001` |
| 3 | **Apply policy** — tính toán một lần duy nhất trước khi gọi domain | Fee = `grossAmount × 1%`, capped at 10,000 VND |
| 4 | **Sequence domain calls** — gọi đúng domain theo đúng thứ tự, không để domain tự gọi nhau | Deposit: outbox → accounting worker → wallet worker (theo thứ tự, có gate) |

### Những gì nó KHÔNG làm

- **Không ghi trực tiếp vào `coa_*` hay `wallet_*`** — đó là việc của domain
- **Không biết TigerBeetle tồn tại** — implementation detail của `core.accounting`
- **Không tái tính fee trong worker** — fee được tính một lần tại đây, worker nhận kết quả
- **Không implement business rule của domain** — "transit 3100 phải bằng 0" là rule của `core.accounting`, không phải orchestration

### Nếu không có tầng này

| Vấn đề | Hậu quả |
|--------|---------|
| `core.accounting` phải tự resolve VA → memberId | Domain thuần bị nhiễm integration concern |
| Bank webhook gọi thẳng vào accounting worker | Coupling: thay đổi bank protocol → sửa accounting |
| Fee computation nằm trong domain | Mỗi use case tính fee theo logic riêng, không nhất quán |
| Mobile app biết về RabbitMQ, TigerBeetle | Caller bị expose internal topology |

`core.accounting` và `core.wallet` chỉ nhận data đã được resolve và validated — không biết caller là ai, không biết protocol là gì.

---

## The Big Picture

GtelPay platform has **8 deployable modules** split into two layers:

- **Ledger libraries** (`core.*`) — pure Java, no framework, own the ledger rules and data
  - `core.accounting` = **accounting ledger** — double-entry, COA, TigerBeetle, PENDING/POSTED
  - `core.wallet` = **wallet ledger** — available/frozen balance, wallet_tx, mutation rules
- **Application shells** (`app-*`) — Spring Boot, wire ledger to HTTP or queue

No `core.*` module imports another `core.*` module. They share only `core.foundation`.

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
