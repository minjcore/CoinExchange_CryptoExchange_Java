# Orchestration Public API Contract (s1-http-public)

> **CF page ID:** 51643159 | **Parent:** 🔌 Contracts & Integration (51315085)
> **Source of truth:** `specs/contracts/open-api/orchestration-public.yaml`
> **Caller:** Mobile apps, partner systems, bank webhooks → API Gateway → `app-orchestration`

---

## Overview

`s1-http-public` is the sole external entry point. Every request passes through API Gateway → `app-orchestration`. No other service is exposed externally.

```
Client / Bank webhook
    │ HTTPS  (s1-http-public)
    ▼
API Gateway
    │
    ▼
app-orchestration       ← all inbound logic handled here
    ├─ Redis pre-checks (auth, idempotency, limits)
    ├─ Fee computation
    └─ outbox write → RabbitMQ (async) or sync DB write
```

---

## Authentication

| Caller | Mechanism |
|--------|-----------|
| Mobile / Partner | `Authorization: Bearer <JWT>` — JWT issued by GtelPay auth service |
| Bank webhook | `X-Bank-Signature: <HMAC-SHA256>` — shared secret per bank |

---

## Idempotency

All mutation endpoints require the header:

```
X-Idempotency-Key: dep-20260618-abc123
```

- This value = `businessRef` — carried end-to-end through s6-rabbitmq-cmds and DB
- Duplicate request with the same key → returns the original response, not reprocessed
- Suggested format: `{prefix}-{YYYYMMDD}-{random}` (max 64 characters)

---

## Endpoints

| Method | Path | Use case | Response |
|--------|------|---------|---------|
| `POST` | `/v1/deposits/notify` | Bank webhook deposit | **202** async |
| `POST` | `/v1/payments` | Payment debit/credit | **200** sync |
| `POST` | `/v1/transfers` | Wallet-to-wallet transfer | **200** sync |
| `POST` | `/v1/withdrawals` | Withdraw to bank | **200** (freeze sync, payout async) |
| `GET` | `/v1/wallets/balance` | Query balance | **200** |

---

## POST /v1/deposits/notify — Async deposit

**Auth:** `X-Bank-Signature` (bank shared secret)

```json
POST /v1/deposits/notify
X-Idempotency-Key: dep-20260618-abc123
X-Bank-Signature: <hmac>

{
  "virtual_account": "VA-GTL-00123",
  "gross_amount": "100000.0000",
  "currency": "VND",
  "bank_ref": "BNK-REF-20260618-001",
  "notified_at": "2026-06-18T10:00:00Z"
}
```

Errors: **401** X-Bank-Signature missing/invalid · **403** bank not authorized · **409** idempotency conflict · **422** VA not found

Response **202** — không chờ xử lý, không block:

```json
{
  "business_ref": "dep-20260618-abc123",
  "status": "ACCEPTED",
  "message": "Deposit queued for processing"
}
```

Async flow sau 202:

```
outbox → BANK_DEPOSIT → app-accounting-worker
  → Phase A (TB pending 1111→3100)
  → Phase B (TB post + split fee 3100→2110/4110)
  → WALLET_CREDIT → app-wallet-worker
  → wallet credited
```

---

## POST /v1/payments — Sync payment

```json
POST /v1/payments
X-Idempotency-Key: pay-20260618-xyz789
Authorization: Bearer <jwt>

{
  "payer_wallet_id": 5001,
  "payee_wallet_id": 6001,
  "amount": "50000.0000",
  "currency": "VND"
}
```

Response **200** — wallet debit + ledger POSTED trước khi trả về:

```json
{
  "business_ref": "pay-20260618-xyz789",
  "status": "COMPLETED",
  "payer_available": "49000.0000",
  "completed_at": "2026-06-18T10:01:00Z",
  "wallet_tx_id": 7001,
  "coa_trans_id": 9001
}
```

---

## POST /v1/transfers — Wallet-to-wallet

```json
POST /v1/transfers
X-Idempotency-Key: txf-20260618-abc456
Authorization: Bearer <jwt>

{
  "sender_wallet_id": 5001,
  "receiver_wallet_id": 7001,
  "amount": "100000.0000",
  "currency": "VND"
}
```

Response **200** — cả hai wallets updated + ledger POSTED:

```json
{
  "business_ref": "txf-20260618-abc456",
  "status": "COMPLETED",
  "sender_available": "49000.0000",
  "completed_at": "2026-06-18T10:01:00Z",
  "wallet_tx_id": 7002,
  "coa_trans_id": 9002
}
```

---

## POST /v1/withdrawals — Withdraw to bank

Freeze immediately → 200. Payout async afterwards.

```json
POST /v1/withdrawals
X-Idempotency-Key: wdl-20260618-def456
Authorization: Bearer <jwt>

{
  "wallet_id": 5001,
  "amount": "200000.0000",
  "currency": "VND",
  "bank_account_number": "1234567890",
  "bank_code": "VCB"
}
```

Response **200** — amount đã frozen, payout in progress:

```json
{
  "business_ref": "wdl-20260618-def456",
  "status": "ACCEPTED",
  "frozen_amount": "200000.0000",
  "accepted_at": "2026-06-18T10:02:00Z"
}
```

---

## GET /v1/wallets/balance

```
GET /v1/wallets/balance?currency=VND&wallet_type=USER
Authorization: Bearer <jwt>
```

```json
{
  "wallet_id": 5001,
  "member_id": 1001,
  "available": "99000.0000",
  "frozen": "0.0000",
  "currency": "VND",
  "wallet_type": "USER"
}
```

---

## Error Codes

| `error_code` | HTTP | Situation |
|-------------|------|-----------|
| `WALLET_INSUFFICIENT_BALANCE` | 422 | Debit / freeze > available |
| `WALLET_NOT_FOUND` | 404 | Member wallet not yet provisioned |
| `WALLET_LOCKED` | 422 | Wallet is locked |
| `VIRTUAL_ACCOUNT_NOT_FOUND` | 422 | VA does not exist in the system |
| `IDEMPOTENCY_CONFLICT` | 409 | X-Idempotency-Key reused with different payload |
| `CURRENCY_NOT_SUPPORTED` | 422 | Only VND supported in v1 |
| _(no error_code)_ | 401 | X-Bank-Signature missing or invalid — `/deposits/notify` only |
| _(no error_code)_ | 403 | Bank not authorized to call the endpoint — `/deposits/notify` only |

---

## businessRef end-to-end

```
X-Idempotency-Key (s1-http-public header)
  = businessRef (s6-rabbitmq-cmds BANK_DEPOSIT envelope)
  = reference_id (s2-http-internal accounting API)
  = coa_trans.business_ref (DB)
  = businessRef (s6-rabbitmq-cmds WALLET_CREDIT envelope)
  = business_ref (s2-http-internal wallet API)
  = wallet_tx.business_ref (DB)
```

---

Full contract: `specs/contracts/open-api/orchestration-public.yaml`
