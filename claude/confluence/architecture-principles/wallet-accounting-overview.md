# E-Wallet & General Ledger Architecture Overview

> **CF page ID:** 45842592 | **Parent:** 🏛️ Architecture & Principles (51642382)
> **Source of truth:** this file → push to CF
> **See also:** `specs/011-architecture-overview/confluence-draft.md` (full technical detail)

---

## Core Concept (read first)

> **Two domains, two different questions:**
>
> - `core.wallet` — *"How much can this member spend right now?"* → **the client waits for this answer**
> - `core.accounting` — *"How is this transaction recorded in the general ledger?"* → **backoffice async, client doesn't wait**
>
> These two questions are never answered in the same DB transaction.

---

## Concrete Example: Member Transfer

Member A transfers 500k to member B. What happens:

```
app-orchestration
  │
  ├─ [sync] Debit wallet A, Credit wallet B  → core.wallet  → 200 OK ◄── CLIENT RECEIVES HERE
  │          (WalletCommandService)
  │
  └─ [async] Emit event → accounting worker
                                │
                                └─ DR 2110 / CR 2120 → core.accounting
                                   (JournalService + TigerBeetle)
                                   Completes after a few seconds — member does not wait
```

If GL fails after this step → member A and B already see the correct balance in their wallets. GL catches up via retry or ops fix — **wallet is not rolled back**.

---

## Hot path vs Backoffice

```
Client
  │
  ▼
app-orchestration ──► e-wallet write ──► 200/202 ← client receives response HERE
                             │
                             └── emit event/queue ──► accounting worker (backoffice async)
                                                           │
                                                           └── GL journal entry (DR/CR)
```

**Principle:** The client never waits for GL. E-wallet confirms first — General Ledger catches up after.

---

## Architecture diagram

```
External ──s1-http-public──► Gateway ──► Orchestration (BFF)
                                              │
                                              ├──[sync]──► e-wallet write (hot path)
                                              │                │ → 200/202 to client
                                              │
                                              └──[async]─► s6-rabbitmq-cmds
                                                               │
                                                   ┌───────────┴──────────────┐
                                                   ▼                          ▼
                                          app-accounting-worker     app-wallet-worker
                                          (backoffice GL posting)   (e-wallet credit async)
```

---

## Module Roles

| Block | Component | Role |
|-------|-----------|------|
| Public edge | API Gateway, bank webhook | Routes into orchestration via s1-http-public |
| Application | `app-orchestration` | Coordinates flow, fee computation, outbox, auth |
| Hot path | `core.wallet` | E-wallet balance mutations — client waits for this result |
| Backoffice | `core.accounting` | GL journal entries (DR/CR) — async, does not block client |
| Shared | `core.sharedlib`, `core.reconciliation` | Primitives + EOD cross-check |
| Async infra | s6-rabbitmq-cmds, s3-kafka-events, workers | Command handling, event fan-out |
| Storage | 3 databases (see below) | Schema isolation per domain |

---

## Database Architecture (3 DB)

| Database | Host | Schema / Purpose | Owner |
|----------|------|-----------------|-------|
| gtelpay DB | localhost:5432 | `wallet` + `accounting` + `virtual_account` | app-orchestration, app-wallet, app-accounting |
| Blnk DB | localhost:5433 (Docker) | Blnk internal — GL, balance, transaction | Blnk Finance 0.14.5 |
| accounting DB | planned | Dedicated `accounting` schema at scale | future app-accounting |

**S1 (current):** gtelpay DB hosts both schemas — easier deployment, single migration set.
**S2 (planned):** `BlnkWalletGateway implements WalletGateway` → Blnk API replaces `core.wallet`. Blnk DB owns e-wallet balances.

---

## Hard Rules

| Rule | Detail |
|------|--------|
| No cross-import | `core.wallet` and `core.accounting` do not import each other |
| No cross-schema JOIN/FK | `wallet_tx.coa_trans_id` = GL correlation ref only |
| E-wallet is source of truth for member balance | `wallet_balance.available` — never query from GL/COA |
| GL does not block hot path | GL posting is async — a GL failure does not return an error to the client |
| E-wallet never posts to GL | E-wallet does not create GL DR/CR entries directly |
| GL never mutates e-wallet | `core.accounting` does not credit/debit e-wallet directly |

---

## Hot Path vs Backoffice per Use Case

| Flow | Client response | E-Wallet (hot path) | General Ledger (backoffice async) |
|------|----------------|---------------------|----------------------------------|
| **Deposit** | Async → **202** | Credit after GL Phase B completes | Phase A+B via worker; e-wallet credit is the final step |
| **Payment** | Sync → **200** | Sync debit user + credit merchant | GL journal entry emitted async after e-wallet |
| **Transfer** | Sync → **200** | Sync debit A + credit B | GL journal entry emitted async after e-wallet |
| **Withdraw accept** | Sync → **200** | Sync freeze | GL journal entry when payout confirms (async) |
| **Balance read** | Sync → **200** | Query `wallet_balance` only | — |

> **Principle example:** Deposit — we credit the e-wallet first (member sees the funds immediately), and the General Ledger records afterwards. GL is backoffice, not a prerequisite for returning the response to the client.

---

## Surface Map (s1–s6)

| Surface | Description | Protocol | Orchestration role |
|---------|-------------|---------|-------------------|
| s1-http-public | Public product API | HTTPS | **Implement** |
| s2-http-internal | GL internal | HTTPS | **Call** (backoffice, not on hot path) |
| s3-kafka-events | Domain events | Kafka | Publish / consume |
| s4-gateway-config | Gateway routes | Config | Edge → BFF |
| s5-shared-envelope | Shared envelope | Library | `ApiResponse`, errors |
| s6-rabbitmq-cmds | Worker commands | RabbitMQ | **Publish** full-body envelope |

> **Messaging direction rule:**
> - **RabbitMQ (s6) = inbound commands** — orchestration → workers (`BANK_DEPOSIT`, `WALLET_CREDIT`)
> - **Kafka (s3) = outbound events** — workers → downstream after processing (`JournalPosted`, `WalletCredited`)
>
> Core services receive commands via RabbitMQ and emit events via Kafka. Kafka does not carry commands into core.
