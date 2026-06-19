# Terminology Reference: Platform vs TRD / REST

> **CF page ID:** 44924932 | **Parent:** 📌 Start Here (51315064)
> **Source of truth:** this file → push to CF

---

## Platform Terms

| Term | Table | Key fields |
|------|-------|-----------|
| **Journal** | `coa_trans` | `use_case`, `business_ref`, `status` |
| **Journal entry (line)** | `coa_trans_data` | `coa_trans_id`, `account_code`, DR/CR, `amount` |
| COA account | `coa_account` | `code`, `type` |

## Core Rules

- `coa_trans` = **journal header** — one balanced DR/CR event
- `coa_trans_data` = **journal line** — one debit or credit row
- POSTED `coa_trans_data` rows are **immutable** (ADR-001)
- Wallet lives only in `core.wallet`; no cross-schema access

## TRD / REST → Platform Mapping

| TRD / API term | Platform term | Platform table |
|----------------|---------------|----------------|
| `journal_entries` | **Journal** | `coa_trans` |
| `journal_lines` | **Journal entry line** | `coa_trans_data` |
| `journal_lines.journal_entry_id` | FK → journal | `coa_trans_id` |
| `reference_id` | Business idempotency key | `business_ref` |
| `ledger_entries` | Posted journal line | `coa_trans_data` (POSTED) |

## Common Mistakes

| Wrong | Right |
|-------|-------|
| "journal entry" = `coa_trans` | **Journal** = `coa_trans` |
| "journal line" = `coa_trans` | **Journal entry line** = `coa_trans_data` |
| Edit POSTED `coa_trans_data` | Create new reversing journal |

## Table Prefixes

| Area | Prefix |
|------|--------|
| Accounting | `coa_` |
| Wallet | `wallet_` |

---

## Wallet Terms

| Term | Table/Field | Ý nghĩa |
|------|-------------|---------|
| **Available** | `wallet_balance.available` | Số dư có thể chi tiêu ngay |
| **Frozen** | `wallet_balance.frozen` | Đang giữ cho giao dịch đang xử lý (VD: rút tiền) |
| **wallet_tx** | `wallet_tx` | Mỗi thay đổi balance = một dòng append-only |
| **Provision** | Tạo `wallet` + `wallet_balance(0)` | Khởi tạo ví lần đầu cho member |

**Balance invariant:** `available >= 0`, `frozen >= 0`, mỗi thay đổi = 1 `wallet_tx` trong cùng transaction.

---

## Messaging Terms

| Term | Surface | Ý nghĩa |
|------|---------|---------|
| `businessRef` | s1-http-public body, s6-rabbitmq-cmds envelope, DB | End-to-end idempotency key = `X-Idempotency-Key` |
| `reference_id` | s2-http-internal OpenAPI (accounting) | Alias của `businessRef` tại accounting API |
| `correlationId` | s6-rabbitmq-cmds envelope, s3-kafka-events (optional) | Trace observability only — không persist vào DB |
| `messageId` | s6-rabbitmq-cmds envelope | AMQP dedup per-publish — không phải business key |
| `BANK_DEPOSIT` | s6-rabbitmq-cmds command | Trigger Phase A+B tại `app-accounting-worker` |
| `WALLET_CREDIT` | s6-rabbitmq-cmds command | Trigger wallet credit tại `app-wallet-worker` |

---

## Status Flow Terms

### coa_trans.status

```
PENDING ──► POSTED   (Phase B success)
        └─► FAILED   (void_pending reversal)
```

### wallet.status

```
ACTIVE ──► LOCKED   (compliance / ops hold)
       └─► CLOSED   (account closed)
```

### outbox.status

```
PENDING ──► PUBLISHED  (relay success)
        └─► FAILED     (max retry exceeded, ops intervention)
```

---

## Module Shorthand

| Shorthand | Full module | Role |
|-----------|------------|------|
| Orchestration | `app-orchestration` | Inbound HTTP, saga, fee computation, outbox |
| Accounting worker | `app-accounting-worker` | RabbitMQ consumer → Phase A/B |
| Wallet worker | `app-wallet-worker` | RabbitMQ consumer → wallet credit |
| Accounting HTTP | `app-accounting` | Sync HTTP gateway for `core.accounting` |
| Wallet HTTP | `app-wallet` | Sync HTTP gateway for `core.wallet` |
