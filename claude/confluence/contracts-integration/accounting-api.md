# Accounting Internal API Contract

> **CF page ID:** 51544687 | **Parent:** 🔌 Contracts & Integration (51315085)
> **Source of truth:** `specs/contracts/open-api/accounting-internal.yaml`
> **Caller:** `app-orchestration` ONLY

---

## Tổng quan

`app-accounting` expose 4 endpoints HTTP (Internal HTTP / s2-http-internal).
Không expose ra ngoài — chỉ `app-orchestration` được gọi.

Trong deposit flow, `app-orchestration` KHÔNG gọi trực tiếp — dùng outbox → RabbitMQ → `app-accounting-worker`.
API này dành cho sync use cases (payment, transfer) hoặc query trạng thái.

---

## Endpoints

| Method | Path | Operation | Use case |
|--------|------|-----------|---------|
| `POST` | `/journals` | `createJournal` | Phase A — tạo PENDING journal + TB pending transfer |
| `POST` | `/journals/{coaTransId}/post` | `postJournal` | Phase B — post pending + split fee |
| `POST` | `/journals/{coaTransId}/void` | `voidPending` | Reversal — void pending TB transfer |
| `GET` | `/journals/{coaTransId}` | `getJournal` | Query trạng thái journal |

---

## createJournal — Phase A

**Idempotent:** `(reference_id, use_case)` → duplicate trả về existing PENDING journal.

```json
POST /journals
{
  "reference_id": "dep-20260618-abc123",
  "use_case": "DEPOSIT",
  "gross_amount": "100000.0000"
}
```

Response:
```json
{
  "coa_trans_id": 9001,
  "reference_id": "dep-20260618-abc123",
  "use_case": "DEPOSIT",
  "status": "PENDING",
  "gross_amount": "100000.0000",
  "fee": null,
  "net_amount": null,
  "currency": "VND",
  "created_at": "2026-06-18T10:00:00Z",
  "updated_at": "2026-06-18T10:00:00Z"
}
```

TigerBeetle: `pending Transfer 1111 DR → 3100 CR (grossAmount)`
TB transfer ID = `hash(businessRef + ":phaseA")` — deterministic idempotency.

---

## postJournal — Phase B

**Input:** `coaTransId` (từ Phase A) + `fee` (tính tại orchestration).

```json
POST /journals/9001/post
{
  "fee": "1000.0000"
}
```

TigerBeetle sequence:
1. `post_pending(pendingTransferId)` — full gross amount
2. `createTransfer(3100 DR → 2110 CR, net)` — net = 100000 - 1000 = 99000
3. `createTransfer(3100 DR → 4110 CR, fee)` — fee = 1000

Validate: `account[3100].net = 0` sau 3 operations.

Response: same shape, `status: "POSTED"`, `fee: "1000.0000"`, `net_amount: "99000.0000"`.

---

## voidPending — Reversal

Dùng khi bank deposit bị reverse sau Phase A hoặc Phase A cần rollback.
Không thể void journal đã POSTED — dùng compensating journal.

```
POST /journals/9001/void
→ 200  { status: "FAILED" }
```

---

## Identity Map (s2-http-internal ↔ end-to-end)

| s2-http-internal field | End-to-end | DB column |
|------------------------|-----------|----------|
| `reference_id` | `businessRef` (s6-rabbitmq-cmds) = `X-Idempotency-Key` (s1-http-public) | `coa_trans.business_ref` |
| `coa_trans_id` | → `coaTransId` trong WALLET_CREDIT command | `coa_trans.id` (BIGINT) |

---

## Error Codes

| `error_code` | HTTP | Tình huống |
|-------------|------|-----------|
| `JOURNAL_NOT_FOUND` | 404 | coaTransId không tồn tại |
| `JOURNAL_ALREADY_POSTED` | 409 | void gọi sau POSTED |
| `JOURNAL_ALREADY_FAILED` | 409 | postJournal gọi sau FAILED |
| `IDEMPOTENCY_CONFLICT` | 409 | reference_id dùng lại với data khác |

---

## Full contract

`specs/contracts/open-api/accounting-internal.yaml`
