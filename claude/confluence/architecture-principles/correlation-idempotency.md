# Correlation & Idempotency — Identity Map

> **CF page ID:** 44859398 | **Parent:** 🏛️ Architecture & Principles (51642382)
> **Source of truth:** this file → push to CF
> **Binding:** ADR-005, integration-surfaces.md §8

---

## TL;DR

| Question | Answer |
|----------|--------|
| `request_id` xuyên suốt toàn hệ? | **Không.** Không có field `request_id`. Trace dùng `correlationId` (optional, observability only). |
| `reference_id` vs `business_ref`? | **Cùng một giá trị.** Khác tên theo lớp: S2/OpenAPI dùng `reference_id`; DB dùng `business_ref`; JSON wire dùng `businessRef`. |
| Khóa idempotent end-to-end? | `businessRef` = `X-Idempotency-Key` (ADR-005) |
| `messageId`? | Chỉ dedup transport tại S6. Không phải business key. |

---

## Identity Map

| Key | Wire / header | DB / domain | Vai trò | Bắt buộc? |
|-----|--------------|-------------|---------|-----------|
| `businessRef` | S1 body + `X-Idempotency-Key`; S6 envelope | `coa_trans.business_ref`, `wallet_tx.business_ref` | **Idempotency nghiệp vụ** | Mutation S1: yes |
| `reference_id` | S2 JSON accounting-internal.yaml | = `business_ref` trên `coa_trans` | Alias của `businessRef` tại S2 | S2 create: yes |
| `correlationId` | S6 envelope; S3 events (optional) | Không persist | **Trace** HTTP → queue → Kafka | Optional |
| `messageId` | S6 envelope only | Không | Dedup một lần publish AMQP | S6: yes |
| `coaTransId` | S2 response; S3 `JournalPosted` | `coa_trans.id`; `wallet_tx.coa_trans_id` (no FK) | Correlation journal ↔ wallet | Sau khi post |

---

## Canonical mapping

```
X-Idempotency-Key
  = body.businessRef
  = reference_id (S2)
  = coa_trans.business_ref
  = envelope.businessRef
  = wallet_tx.business_ref
```

**Casing:** JSON wire = `camelCase`; PostgreSQL = `snake_case`; S2 accounting OpenAPI = `snake_case` với tên `reference_id`.

---

## businessRef Flow — Deposit

```
Client
  X-Idempotency-Key: "dep-20260618-abc123"
      │
      ▼
app-orchestration
  outbox.business_ref = "dep-20260618-abc123"
  [S6 BANK_DEPOSIT envelope]
  businessRef = "dep-20260618-abc123"
      │
      ▼
app-accounting-worker
  coa_trans.business_ref = "dep-20260618-abc123"
  TB transfer ID = hash("dep-20260618-abc123:phaseA")
  [S6 WALLET_CREDIT envelope]
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
- `reference_id` là alias của `businessRef` trong S2/accounting
- `correlationId` chỉ phục vụ trace/observability
- `messageId` và `eventId` không được dùng thay cho business key
- `request_id` không phải field chuẩn trong spec hiện tại
