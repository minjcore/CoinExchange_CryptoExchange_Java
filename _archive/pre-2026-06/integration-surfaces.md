# Integration surfaces — contract index

**Author:** Cao Khang Đoàn  
**Last updated:** 2026-06-05  
**Status:** Draft  

> **Purpose:** List integration surfaces (S1–S6) — who publishes/consumes, which spec file, which domain doc to read.  
> **Public HTTP:** S1 (`gtelpay-public.yaml`) — implement trong **BFF/orchestration**, route qua S4 Gateway. **`core.wallet` / `core.accounting`:** domain library, không tự expose HTTP. **Internal:** S2, S3, S6.  
> **Does not replace:** `wallet_*` schema → [`core.wallet.md`](./core.wallet.md); COA / transit postings → [`core.sharedlib.md`](./core.sharedlib.md) §8–16; accounting FR → [`core.accounting.trd.md`](./core.accounting.trd.md).

---

## 1. What this file is

| | |
|---|---|
| **Includes** | Surface catalog, use case × channel matrix, Kafka + RabbitMQ, idempotency, forbidden rules, spec map by role |
| **Excludes** | Table DDL, step-by-step DR/CR, Java implementation |

**Wire schemas (payloads):**

| Channel | Exposure | Spec file |
|---------|----------|-----------|
| HTTP S1 | Public (via Gateway → BFF) | [`open-api/gtelpay-public.yaml`](./open-api/gtelpay-public.yaml) |
| HTTP S2 | Internal (orchestration only) | [`open-api/accounting-internal.yaml`](./open-api/accounting-internal.yaml) |
| Kafka S3 | Internal | [`async-api/core-events.yaml`](./async-api/core-events.yaml) |
| RabbitMQ S6 | Internal | [`async-api/core-commands.yaml`](./async-api/core-commands.yaml) |

**AsyncAPI** = format YAML mô tả topic/queue + message body (giống OpenAPI cho HTTP). File này = index surface + step order; chi tiết field → bảng trên.

---

## 2. Surface catalog

| ID | Surface | Protocol | Spec | Producer | Consumer |
|----|---------|----------|------|----------|----------|
| S1 | Public product API | HTTPS | [`open-api/gtelpay-public.yaml`](./open-api/gtelpay-public.yaml) | Orchestration (BFF) | Mobile, partners, **API Gateway** |
| S2 | Accounting API | HTTPS (internal) | [`open-api/accounting-internal.yaml`](./open-api/accounting-internal.yaml) | `core.accounting` (adapter) | Orchestration only |
| S3 | Domain events (internal) | Kafka | [`async-api/core-events.yaml`](./async-api/core-events.yaml) | Orchestration, accounting, wallet adapters | Orchestration, wallet consumer |
| S4 | Gateway routes (public edge) | Config | [`gateway/routes.example.yaml`](./gateway/routes.example.yaml) | DevOps | Edge proxy → BFF only |
| S5 | Shared envelope (errors, paging) | Library (design) | [`core.sharedlib.md`](./core.sharedlib.md) Part I §4 | — | Application, `core.*` |
| S6 | Worker commands (internal) | AMQP (RabbitMQ) | [`async-api/core-commands.yaml`](./async-api/core-commands.yaml) | Orchestration | Accounting worker, wallet worker, payout worker |

**Default runtime flow:**

```
External ──S1──► Gateway ──S1──► Orchestration ──S2──► accounting
                              ├── command (full-body) ──S6──► workers
                              └── domain event ──S3──► subscribers / wallet
```

Orchestration **must not** write `wallet_*` / `coa_*` directly ([§9](#9-forbidden)).

---

## 3. Spec map by role

| Role | Read first | Then |
|------|------------|------|
| API Gateway | S4, S1 | `open-api/README.md` (lint) |
| BFF / orchestration | This file §4, S1+S2+S3+S6 | `core.sharedlib.md` Part I §3, Part II §8+ (step order) |
| Implement `core.wallet` | [`core.wallet.md`](./core.wallet.md) | S3 events + S6 `WALLET_CREDIT` consumer; do not implement S1/S2 inside wallet module |
| Implement `core.accounting` | `core.accounting.trd.md` | S2; consume S6 `BANK_DEPOSIT`; publish `JournalPosted` (S3) |
| Worker / adapter | S6 envelope §6.1–6.3 | Same field names as S1/S3 ([§8](#8-idempotency--correlation)) |
| Partner / bank | S1 only | No S2 |

---

## 4. Use case × surface matrix

**Wallet branch** (detail): [`core.wallet.md` §5](./core.wallet.md). **Ledger branch:** [`core.sharedlib.md`](./core.sharedlib.md) §8–16.

| Use case | S1 HTTP public | S2 Accounting | S6 RabbitMQ (worker) | S3 Kafka (fan-out) | Wallet (orchestration calls) | Notes |
|----------|----------------|---------------|----------------------|-------------------|------------------------------|-------|
| **Deposit** | `notifyDeposit` / `bankWebhook` → **202** | PENDING → POSTED | `BANK_DEPOSIT` → accounting worker | `BankDepositReceived` (optional mirror) → `JournalPosted` → credit | Credit USER after POSTED | Prefer async via S6 |
| **Payment** | `createPayment` → **200** | create → lines → post (sync) | — | Optional | debit USER → post → credit MERCHANT | Single-request sync |
| **Transfer** | `createTransfer` → **200** | post (transit 3300) | — | Optional | debit A → post → credit B | Sync |
| **Withdraw** | `createWithdrawal` → **200** (wallet branch) | post + bank async | `WITHDRAW_PAYOUT` → payout worker | payout status event (optional) | debit/freeze before 200 | Bank branch async |
| **Balance read** | `getWalletBalance` | — (COA balance = other S2 API) | — | Query only | No ledger write |

### 4.1 Deposit — required step order

| # | Layer | Action |
|---|--------|--------|
| 1 | S1 | Webhook/notify → validate → **202** + `businessRef` |
| 2 | S6 (preferred) or sync | Publish `BANK_DEPOSIT` full-body envelope → accounting worker → journal PENDING (transit 3100) |
| 3 | S2 or domain service | Confirm → **POSTED**, transit 3100 = 0 |
| 4 | S3 | `JournalPosted` or `WalletCreditCommand` |
| 5 | Wallet | `credit` USER — idempotent `businessRef` |

### 4.2 Payment — required step order

| # | Layer | Action |
|---|--------|--------|
| 1 | Wallet | `debit` USER |
| 2 | S2 | Journal POSTED (transit 3500 = 0) |
| 3 | Wallet | `credit` MERCHANT |
| 4 | S1 | **200** + `walletTxId` / `coaTransId` |

---

## 5. Kafka topics

Schema detail: [`async-api/core-events.yaml`](./async-api/core-events.yaml).

| Topic | Published by | Consumed by | Wallet effect |
|-------|--------------|-------------|---------------|
| `core.bank.deposit-received` | Orchestration | Accounting worker | — |
| `core.accounting.journal-posted` | Accounting adapter | Orchestration / wallet worker | Credit when `useCase=DEPOSIT` |
| `core.wallet.credit-command` | Orchestration | Wallet worker | `wallet_tx` CREDIT |
| `core.wallet.credited` | Wallet adapter | Notify, analytics | — |
| `core.operations.command-failed` | Workers | Ops, notify | — |

Partition key: `businessRef` (deposit/pay) or `memberId`.

---

## 6. RabbitMQ command queues (S6)

Schema detail: [`async-api/core-commands.yaml`](./async-api/core-commands.yaml).

**Transport split:** S6 = point-to-point worker commands. S3 = domain event fan-out. Orchestration may publish both when workers need a queue and other services need events.

Exchange: `core.commands` (topic, durable).

| Queue / routing key | `commandType` | Published by | Consumed by | Effect |
|-------------------|---------------|--------------|-------------|--------|
| `core.commands.bank-deposit` | `BANK_DEPOSIT` | Orchestration (after S1 **202**) | Accounting worker | Journal PENDING → POSTED |
| `core.commands.wallet-credit` | `WALLET_CREDIT` | Orchestration | Wallet worker | `wallet_tx` CREDIT |
| `core.commands.withdraw-payout` | `WITHDRAW_PAYOUT` | Orchestration (after wallet leg) | Payout / bank adapter | Bank transfer async |

AMQP routing key (publish): `{commandType}.{memberId}` lowercase, e.g. `bank_deposit.100234`. Idempotency = envelope `businessRef` (full body, not header-only).

### 6.1 Full-body envelope (`CommandEnvelope`)

| Field | Required | Rule |
|-------|----------|------|
| `messageId` | Yes | UUID v4 per physical publish |
| `businessRef` | Yes | Idempotency key — equals S1 `X-Idempotency-Key` |
| `memberId` | Yes | Platform member id (**mid**) |
| `commandType` | Yes | `BANK_DEPOSIT` \| `WALLET_CREDIT` \| `WITHDRAW_PAYOUT` |
| `occurredAt` | Yes | ISO-8601 UTC |
| `schemaVersion` | Yes | e.g. `"1.0"` |
| `payload` | Yes | Full command fields — align OpenAPI / S3 |
| `correlationId` | No | HTTP → queue → Kafka trace |
| `causationId` | No | Prior `messageId` |
| `source` | No | Publisher service |

`businessRef` lives on the envelope, not inside `payload`.

### 6.2 Consumer rules

| # | Rule |
|---|------|
| C1 | Idempotent on `(commandType, businessRef)` — same semantics → ACK, no double effect |
| C2 | ACK only after DB commit (or safe no-op replay) |
| C3 | NACK + requeue transient errors only; poison → DLQ `core.commands.dlq` |
| C4 | Deposit success → may publish S3 `JournalPosted`; wallet credit per §4.1 |
| C5 | Log `messageId`, `businessRef`, `memberId`, `correlationId` |

### 6.3 Example — deposit after webhook **202**

```json
{
  "messageId": "550e8400-e29b-41d4-a716-446655440000",
  "businessRef": "dep-20260605-001",
  "memberId": 100234,
  "commandType": "BANK_DEPOSIT",
  "occurredAt": "2026-06-05T10:15:30Z",
  "correlationId": "trace-abc123",
  "source": "orchestration",
  "schemaVersion": "1.0",
  "payload": {
    "amount": "500000.0000",
    "vaNumber": "9704188888888",
    "bankCode": "VCB",
    "receivedAt": "2026-06-05T10:15:28Z"
  }
}
```

---

## 7. Public HTTP quick map (S1)

Full paths: [`open-api/gtelpay-public.yaml`](./open-api/gtelpay-public.yaml).

| Path | `operationId` | Sync response | Wallet? |
|------|-----------------|---------------|---------|
| `GET /v1/wallets/balance` | `getWalletBalance` | 200 | Query |
| `POST /v1/payments` | `createPayment` | 200 | debit + credit |
| `POST /v1/transfers` | `createTransfer` | 200 | debit + credit |
| `POST /v1/withdrawals` | `createWithdrawal` | 200 | debit/freeze |
| `POST /v1/deposits/notify` | `notifyDeposit` | 202 | After worker |
| `GET /v1/deposits/status` | `getDepositStatus` | 200 | Poll |

Internal journal API (S2) is **not** routed on the public Gateway.

---

## 8. Idempotency & correlation

| Key | Used on | Rule |
|-----|---------|------|
| `businessRef` | S1 header `X-Idempotency-Key`, S2 `reference_id`, S3 payload, **S6 envelope**, `wallet_tx.business_ref` | Same ref + same semantics → no-op, return prior result |
| `memberId` (**mid**) | S1 body, **S6 envelope**, wallet commands | Routing affinity; not a second idempotency key |
| `messageId` | **S6 envelope** only | Transport dedup per publish; distinct from `businessRef` |
| `coaTransId` | S2 response, S3 `JournalPosted`, S6 `WALLET_CREDIT` payload, `wallet_tx.coa_trans_id` | Correlation only — **no** DB FK wallet → `coa_trans` |
| `walletTxId` | S1 response, S3 `WalletCredited` | Movement line id |
| `correlationId` | S6 envelope (optional) | Trace across HTTP → queue → Kafka |
| Field names | S1, S3, S6 | Same command → same names ([ADR-002](./adr/ADR-002-core-foundation-shared-library.md)) |

---

## 9. Sync vs async

| Pattern | Caller waits? | Surface |
|---------|---------------|---------|
| Pay / transfer | Yes → **200** | S1 + wallet + S2 in one orchestration |
| Deposit webhook | No (recommended) → **202** | S1 fast ack; **S6** worker → optional S3 fan-out → wallet |
| Withdraw accept | Yes (wallet branch) | S1 **200** after debit/freeze; bank via **S6** payout worker |
| `postJournal` | N/A (internal) | Always one ACID transaction in accounting |

---

## 10. Forbidden

| # | Rule |
|---|------|
| F1 | Partner/Gateway calls S2 directly or INSERTs into `coa_*` / `wallet_*` |
| F2 | `core.wallet` imports accounting repository / API |
| F3 | `core.accounting` mutates `wallet_balance` |
| F4 | Same command uses different field names across S1 / S3 / S6 (OpenAPI wire is baseline) |
| F5 | UPDATE finalized `wallet_tx` / `coa_trans_data` — only new journal / movement |
| F6 | Idempotency only in AMQP headers — must be in S6 full-body `businessRef` |

---

## 11. Doc map

| Question | Document |
|----------|----------|
| `wallet` tables, credit/debit FR? | [`core.wallet.md`](./core.wallet.md) |
| Boundary between two cores? | [`core.sharedlib.md`](./core.sharedlib.md) Part I §3 |
| Transit 3100, deposit DR/CR? | [`core.sharedlib.md`](./core.sharedlib.md) Part II §8 |
| Accounting NFR / API FR? | [`core.accounting.trd.md`](./core.accounting.trd.md) |
| Lint OpenAPI? | [`open-api/README.md`](./open-api/README.md) |
| RabbitMQ command envelope? | [`async-api/core-commands.yaml`](./async-api/core-commands.yaml) · §6 |
| Immutable ledger? | [ADR-001](./adr/ADR-001-immutable-ledger.md) |
