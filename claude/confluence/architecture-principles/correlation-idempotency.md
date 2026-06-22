# Correlation & Idempotency — Identity Map

> **CF page ID:** 44859398 | **Parent:** 🏛️ Architecture & Principles (51642382)
> **Source of truth:** this file → push to CF
> **Binding:** ADR-005, integration-surfaces.md §8

---

## TL;DR

| Question | Answer |
|----------|--------|
| Is there a `request_id` across the entire system? | **No.** There is no `request_id` field. Tracing uses `correlationId` (optional, observability only). |
| `reference_id` vs `business_ref`? | **Same value.** Different names by layer: s2-http-internal/OpenAPI uses `reference_id`; DB uses `business_ref`; JSON wire uses `businessRef`. |
| End-to-end idempotency key? | `businessRef` = `X-Idempotency-Key` (ADR-005) |
| `messageId`? | Transport-level dedup at s6-rabbitmq-cmds only. Not a business key. |

---

## Identity Map

| Key | Wire / header | DB / domain | Role | Mandatory? |
|-----|--------------|-------------|------|-----------|
| `businessRef` | s1-http-public body + `X-Idempotency-Key`; s6-rabbitmq-cmds envelope | `coa_trans.business_ref`, `wallet_tx.business_ref` | **Business idempotency** | Mutation s1-http-public: yes |
| `reference_id` | s2-http-internal JSON accounting-internal.yaml | = `business_ref` on `coa_trans` | Alias of `businessRef` at s2-http-internal | s2-http-internal create: yes |
| `correlationId` | s6-rabbitmq-cmds envelope; s3-kafka-events (optional) | Not persisted | **Trace** HTTP → queue → Kafka | Optional |
| `messageId` | s6-rabbitmq-cmds envelope only | No | AMQP dedup per publish | s6-rabbitmq-cmds: yes |
| `coaTransId` | s2-http-internal response; s3-kafka-events `JournalPosted` | `coa_trans.id`; `wallet_tx.coa_trans_id` (no FK) | Correlation journal ↔ wallet | After posting |

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

**Casing:** JSON wire = `camelCase`; PostgreSQL = `snake_case`; s2-http-internal accounting OpenAPI = `snake_case` with name `reference_id`.

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

Same key from start to finish. Never transformed, never mapped to a different ID.

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

- `businessRef` is the canonical business idempotency key end-to-end
- `reference_id` is an alias of `businessRef` in s2-http-internal/accounting
- `correlationId` serves trace/observability only
- `messageId` and `eventId` must not be used in place of the business key
- `request_id` is not a standard field in the current spec
