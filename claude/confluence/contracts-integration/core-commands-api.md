# Core Commands & Events Contract (s6-rabbitmq-cmds + s3-kafka-events)

> **CF page ID:** 51643061 | **Parent:** 🔌 Contracts & Integration (51315085)
> **Source of truth:**
>   - `specs/contracts/async-api/core-commands.yaml` (s6-rabbitmq-cmds RabbitMQ)
>   - `specs/contracts/async-api/core-events.yaml` (s3-kafka-events Kafka)

---

## Tổng quan

Hai channel async của platform:

| Surface | Protocol | Direction | Contract file |
|---------|---------|-----------|--------------|
| s6-rabbitmq-cmds — Worker Commands | RabbitMQ | orchestration → workers | `core-commands.yaml` |
| s3-kafka-events — Domain Events | Kafka | workers → downstream | `core-events.yaml` |

---

## s6-rabbitmq-cmds — RabbitMQ Commands

**Producer:** `app-orchestration` (via transactional outbox).
**Consumers:** `app-accounting-worker`, `app-wallet-worker`.

### Command Envelope (chung cho mọi command)

```json
{
  "commandType": "BANK_DEPOSIT",
  "businessRef": "dep-20260618-abc123",
  "messageId": "msg-01j4kz7abc",
  "correlationId": "trace-xyz-001",
  "payload": { ... }
}
```

| Field | Mandatory | Vai trò |
|-------|-----------|---------|
| `commandType` | Yes | Routing key: `BANK_DEPOSIT` \| `WALLET_CREDIT` \| `WITHDRAW_PAYOUT` |
| `businessRef` | Yes | End-to-end idempotency key = `X-Idempotency-Key` |
| `messageId` | Yes | Per-publish AMQP dedup — không phải business key |
| `correlationId` | No | Trace/observability only — không persist |
| `payload` | Yes | Business data per command type |

---

### BANK_DEPOSIT

**Channel:** `core.commands.bank-deposit`
**Consumer:** `app-accounting-worker`
**Triggers:** Phase A + Phase B deposit processing

```json
{
  "commandType": "BANK_DEPOSIT",
  "businessRef": "dep-20260618-abc123",
  "messageId": "msg-01j4kz7abc",
  "payload": {
    "memberId": 1001,
    "virtualAccount": "VA-GTL-00123",
    "grossAmount": "100000.0000",
    "fee": "1000.0000",
    "currency": "VND",
    "bankRef": "BNK-REF-20260618-001"
  }
}
```

| Payload field | Type | Notes |
|--------------|------|-------|
| `memberId` | int64 | Resolved from virtualAccount by orchestration |
| `virtualAccount` | string | Raw VA từ bank notification |
| `grossAmount` | decimal string | Scale 4. Phase A: 1111 DR → 3100 CR |
| `fee` | decimal string | Scale 4. Phase B: 3100 DR → 4110 CR |
| `currency` | string | `VND` only (v1) |
| `bankRef` | string | Bank's own reference (audit trail) |

**Processing:**
1. `createJournal(businessRef, DEPOSIT, grossAmount)` → Phase A
2. Bank confirms → `confirmDeposit(coaTransId, fee)` → Phase B (domain method; HTTP operationId = `postJournal`)
3. Phase B POSTED → publish `WALLET_CREDIT` command

---

### WALLET_CREDIT

**Channel:** `core.commands.wallet-credit`
**Consumer:** `app-wallet-worker`
**Triggers:** Wallet credit sau Phase B POSTED

```json
{
  "commandType": "WALLET_CREDIT",
  "businessRef": "dep-20260618-abc123",
  "messageId": "msg-01j4kz8def",
  "payload": {
    "walletId": 5001,
    "netAmount": "99000.0000",
    "currency": "VND",
    "coaTransId": 9001
  }
}
```

| Payload field | Type | Notes |
|--------------|------|-------|
| `walletId` | int64 | Pocket wallet BIGINT PK |
| `netAmount` | decimal string | grossAmount − fee |
| `currency` | string | `VND` only (v1) |
| `coaTransId` | int64 | Correlation only — `wallet_tx.coa_trans_id` (no FK) |

---

### WITHDRAW_PAYOUT

**Channel:** `core.commands.withdraw-payout`
**Consumer:** `app-payment-worker`
**Triggers:** Bank payout sau wallet freeze

```json
{
  "commandType": "WITHDRAW_PAYOUT",
  "businessRef": "wdl-20260618-def456",
  "messageId": "msg-01j4kz9pqr",
  "payload": {
    "walletId": 5001,
    "amount": "200000.0000",
    "currency": "VND",
    "bankAccountNumber": "1234567890",
    "bankCode": "BIDV"
  }
}
```

| Payload field | Type | Notes |
|--------------|------|-------|
| `walletId` | int64 | Wallet với frozen amount cần payout |
| `amount` | decimal string | Frozen amount to release to bank |
| `currency` | string | `VND` only (v1) |
| `bankAccountNumber` | string | Destination bank account |
| `bankCode` | string | BIC/SWIFT or local bank code |

---

## s3-kafka-events — Kafka Events

**Producers:** `app-accounting-worker`, `app-wallet-worker`.
**Consumers:** `app-orchestration`, downstream services.

### JournalPosted

**Channel:** `core.events.journal-posted`
**Emitted when:** Phase B complete, `coa_trans.status = POSTED`

```json
{
  "eventType": "JournalPosted",
  "eventId": "evt-01j4kz9abc",
  "businessRef": "dep-20260618-abc123",
  "occurredAt": "2026-06-18T10:01:30Z",
  "coaTransId": 9001,
  "useCase": "DEPOSIT",
  "grossAmount": "100000.0000",
  "fee": "1000.0000",
  "netAmount": "99000.0000",
  "currency": "VND"
}
```

### WalletCredited

**Channel:** `core.events.wallet-credited`
**Emitted when:** `wallet_tx CREDIT` inserted

```json
{
  "eventType": "WalletCredited",
  "eventId": "evt-01j4kz9def",
  "businessRef": "dep-20260618-abc123",
  "occurredAt": "2026-06-18T10:01:31Z",
  "walletId": 5001,
  "walletTxId": 7001,
  "netAmount": "99000.0000",
  "currency": "VND",
  "idempotentReplay": false
}
```

### CommandFailed

**Channel:** `core.events.command-failed`
**Emitted when:** Worker command fails sau max retries

```json
{
  "eventType": "CommandFailed",
  "eventId": "evt-01j4kzfail",
  "businessRef": "dep-20260618-abc123",
  "occurredAt": "2026-06-18T10:05:00Z",
  "commandType": "BANK_DEPOSIT",
  "reason": "TigerBeetle connection timeout after 3 retries",
  "failedAt": "2026-06-18T10:05:00Z"
}
```

---

## Deposit Flow: Full Message Sequence

```
app-orchestration
  outbox.write(BANK_DEPOSIT, businessRef)
  → HTTP 202

app-orchestration outbox relay
  → RabbitMQ: BANK_DEPOSIT

app-accounting-worker
  createJournal(businessRef, DEPOSIT, grossAmount)   ← Phase A (domain method)
  confirmDeposit(coaTransId, fee)                     ← Phase B (domain method; HTTP operationId = postJournal)
  → Kafka: JournalPosted(coaTransId, POSTED)
  → RabbitMQ: WALLET_CREDIT

app-wallet-worker
  POST /wallets/{walletId}/credit(netAmount, businessRef)
  → Kafka: WalletCredited
```

---

## businessRef Identity

```
X-Idempotency-Key (s1-http-public)
  = businessRef (s6-rabbitmq-cmds BANK_DEPOSIT envelope)
  = reference_id (accounting-internal.yaml)
  = coa_trans.business_ref (DB)
  = businessRef (s6-rabbitmq-cmds WALLET_CREDIT envelope)
  = business_ref (wallet-internal.yaml)
  = wallet_tx.business_ref (DB)
```

Không đổi tên, không map sang ID khác, end-to-end.
