# Correlation & Idempotency — Identity Map

> **CF page ID:** 44859398 | **Parent:** 🏛️ Architecture & Principles (51642382)
> **Source of truth:** this file → push to CF
> **Binding:** ADR-005, integration-surfaces.md §8

---

## TL;DR

| Question | Answer |
|----------|--------|
| `request_id` xuyên suốt toàn hệ? | **Không.** Không có field `request_id`. Trace dùng `correlationId` (optional, observability only). |
| `reference_id` vs `business_ref`? | **Cùng một giá trị.** Khác tên theo lớp: s2-http-internal/OpenAPI dùng `reference_id`; DB dùng `business_ref`; JSON wire dùng `businessRef`. |
| Khóa idempotent end-to-end? | `businessRef` = `X-Idempotency-Key` (ADR-005) |
| `messageId`? | Chỉ dedup transport tại s6-rabbitmq-cmds. Không phải business key. |

---

## Identity Map

| Key | Wire / header | DB / domain | Vai trò | Bắt buộc? |
|-----|--------------|-------------|---------|-----------|
| `businessRef` | s1-http-public body + `X-Idempotency-Key`; s6-rabbitmq-cmds envelope | `coa_trans.business_ref`, `wallet_tx.business_ref` | **Idempotency nghiệp vụ** | Mutation s1-http-public: yes |
| `reference_id` | s2-http-internal JSON accounting-internal.yaml | = `business_ref` trên `coa_trans` | Alias của `businessRef` tại s2-http-internal | s2-http-internal create: yes |
| `correlationId` | s6-rabbitmq-cmds envelope; s3-kafka-events (optional) | Không persist | **Trace** HTTP → queue → Kafka | Optional |
| `messageId` | s6-rabbitmq-cmds envelope only | Không | Dedup một lần publish AMQP | s6-rabbitmq-cmds: yes |
| `coaTransId` | s2-http-internal response; s3-kafka-events `JournalPosted` | `coa_trans.id`; `wallet_tx.coa_trans_id` (no FK) | Correlation journal ↔ wallet | Sau khi post |

---

## Canonical mapping

```
X-Idempotency-Key
  = body.businessRef
  = reference_id (s2-http-internal)
  = coa_trans.business_ref
  = envelope.businessRef
  = wallet_tx.business_ref
```

**Casing:** JSON wire = `camelCase`; PostgreSQL = `snake_case`; s2-http-internal accounting OpenAPI = `snake_case` với tên `reference_id`.

---

## businessRef Flow — Deposit

```
Client
  X-Idempotency-Key: "dep-20260618-abc123"
      │
      ▼
app-orchestration
  outbox.business_ref = "dep-20260618-abc123"
  [s6-rabbitmq-cmds BANK_DEPOSIT envelope]
  businessRef = "dep-20260618-abc123"
      │
      ▼
app-accounting-worker
  coa_trans.business_ref = "dep-20260618-abc123"
  TB transfer ID = hash("dep-20260618-abc123:phaseA")
  [s6-rabbitmq-cmds WALLET_CREDIT envelope]
  businessRef = "dep-20260618-abc123"
      │
      ▼
app-wallet-worker
  wallet_tx.business_ref = "dep-20260618-abc123"
  UNIQUE(wallet_id, business_ref, tx_type) → idempotency
```

Cùng một key từ đầu đến cuối. Không biến đổi, không map sang ID khác.

---

## Duplicate Handling

| Layer | Duplicate arrives | Result |
|-------|-----------------|--------|
| Orchestration outbox | Same `X-Idempotency-Key` | outbox row exists → return 202, no re-write |
| `createJournal` | Same `(reference_id, use_case)` | Return existing `CoaTrans` (PENDING or POSTED) |
| TigerBeetle Phase A | Same transfer ID | TB rejects as duplicate, worker proceeds to Phase B check |
| `wallet_tx` insert | Same `(wallet_id, business_ref, tx_type)`, same amount | Return existing row, `idempotentReplay=true` |
| `wallet_tx` insert | Same key, **different amount** | Reject 409 `WALLET_DUPLICATE_CONFLICT` |

---

## Decisions (locked)

- `businessRef` là canonical business idempotency key end-to-end
- `reference_id` là alias của `businessRef` trong s2-http-internal/accounting
- `correlationId` chỉ phục vụ trace/observability
- `messageId` và `eventId` không được dùng thay cho business key
- `request_id` không phải field chuẩn trong spec hiện tại
