# Integration Surfaces — Contract Index

> **CF page ID:** 43220994 | **Parent:** 🔌 Contracts & Integration (51315085)
> **Source of truth:** this file → push to CF
> **Wire specs:** `spec/contracts/open-api/` and `spec/contracts/async-api/`

---

## Surface Catalog

| Surface | Description | Protocol | Producer | Consumer |
|---------|-------------|---------|---------|---------|
| s1-http-public | Public product API | HTTPS | `app-orchestration` | Mobile, partner, API Gateway |
| s2-http-internal | Accounting API | HTTPS (internal) | `app-accounting` | `app-orchestration` only |
| s3-kafka-events | Domain events | Kafka | Workers | Orchestration, downstream |
| s4-gateway-config | Gateway routing | Config | DevOps | Edge proxy |
| s5-shared-envelope | Shared envelope | Library | — | All `app-*` and `core.*` |
| s6-rabbitmq-cmds | Worker commands | RabbitMQ | `app-orchestration` (outbox) | Workers |

---

## Use Case × Surface Matrix

| Use case | s1-http-public | s2-http-internal | s6-rabbitmq-cmds queue | Wallet |
|----------|----------------|------------------|------------------------|--------|
| **Deposit** | `POST /deposits/notify` → **202** | Phase A+B (via worker, not direct) | `BANK_DEPOSIT` → `WALLET_CREDIT` | Credit USER after POSTED |
| **Payment** | `POST /v1/payments` → **200** | Create + post (sync) | — | Debit USER → credit MERCHANT |
| **Transfer** | `createTransfer` → **200** | Post via transit 3300 | — | Debit A → credit B |
| **Withdraw** | `createWithdrawal` → **200** | Post + bank async | `WITHDRAW_PAYOUT` | Freeze before 200 |
| **Balance read** | `GET /v1/wallets/balance` → **200** | — | — | Query only |

---

## s6-rabbitmq-cmds Command Envelope

Mọi message trên s6-rabbitmq-cmds dùng chung envelope:

```json
{
  "commandType": "BANK_DEPOSIT",
  "businessRef": "dep-20260618-abc123",
  "messageId": "msg-uuid-...",
  "correlationId": "trace-uuid-... (optional)",
  "payload": {
    "memberId": 1001,
    "grossAmount": "100000.0000",
    "fee": "1000.0000",
    "currency": "VND",
    "virtualAccountId": "VA-001"
  }
}
```

| Field | Mandatory | Vai trò |
|-------|-----------|---------|
| `commandType` | Yes | Routing key (BANK_DEPOSIT, WALLET_CREDIT, ...) |
| `businessRef` | Yes | = `X-Idempotency-Key` end-to-end |
| `messageId` | Yes | AMQP dedup per broker |
| `correlationId` | No | Trace only |
| `payload` | Yes | Business data per command type |

---

## Error Codes (Platform)

| Code | HTTP | Scenario |
|------|------|---------|
| `WALLET_INSUFFICIENT_BALANCE` | 422 | Debit > available |
| `WALLET_NOT_FOUND` | 404 | walletId không tồn tại |
| `WALLET_LOCKED` | 422 | Wallet bị lock, reject mutation |
| `WALLET_DUPLICATE_CONFLICT` | 409 | Cùng businessRef + khác amount |
| `JOURNAL_NOT_FOUND` | 404 | coaTransId không tồn tại |
| `JOURNAL_ALREADY_POSTED` | 409 | Phase B gọi lại sau POSTED |
| `JOURNAL_ALREADY_FAILED` | 409 | Thao tác trên journal đã FAILED |
| `IDEMPOTENCY_CONFLICT` | 409 | businessRef dùng lại với data khác |

---

## Forbidden Rules

| # | Rule |
|---|------|
| F1 | Partner / Gateway must not call s2-http-internal directly or write to `coa_*` / `wallet_*` |
| F2 | `core.wallet` must not import `core.accounting` |
| F3 | `core.accounting` must not mutate `wallet_balance` |
| F4 | Kafka payload field names must match OpenAPI field names for the same command |
| F5 | Do not UPDATE finalized `wallet_tx` or `coa_trans_data` — compensate with new entry |
