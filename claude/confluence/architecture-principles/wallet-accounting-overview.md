# Wallet & Accounting Architecture Overview

> **CF page ID:** 45842592 | **Parent:** 🏛️ Architecture & Principles (51642382)
> **Source of truth:** this file → push to CF
> **See also:** `specs/011-architecture-overview/confluence-draft.md` (full technical detail)

---

## Tóm tắt một câu

Orchestration là lớp application điều phối `core.wallet` và `core.accounting`; hai core không gọi trực tiếp nhau, không cross-import, không cross-schema JOIN/FK.

---

## Architecture diagram

```
External ──S1──► Gateway ──S1──► Orchestration (BFF)
                                    ├──► core.wallet      (sync HTTP via app-wallet)
                                    ├──► core.accounting  (sync HTTP via app-accounting)
                                    ├──► S6 RabbitMQ      (worker commands, async)
                                    └──► S3 Kafka         (domain events, async)
```

---

## Module Roles

| Khối | Thành phần | Vai trò |
|------|-----------|---------|
| Public edge | API Gateway, bank webhook | Route vào orchestration qua S1 |
| Application | `app-orchestration` | Điều phối flow, fee computation, outbox, auth |
| Domain cores | `core.wallet`, `core.accounting`, `core.shared` | Business capability, không cross-import |
| Async infra | RabbitMQ S6, Kafka S3, workers | Command handling, event fan-out |
| Storage | PostgreSQL schemas `wallet` + `accounting` | Tách schema, cùng DB v1 |

---

## Ranh giới cứng

| Rule | Detail |
|------|--------|
| No cross-import | `core.wallet` và `core.accounting` không import nhau |
| No cross-schema JOIN/FK | `wallet_tx.coa_trans_id` = correlation only |
| Sync alignment | Domains không gọi nhau; orchestration align step order |
| Wallet → ledger | Wallet không tự tạo DR/CR |
| Ledger → wallet | Accounting không tự credit/debit ví |

---

## Sync vs Async theo use case

| Luồng | Client response | Wallet ↔ Accounting | Messaging |
|-------|----------------|---------------------|-----------|
| **Deposit** | Async → **202** | Async qua worker | S6 `BANK_DEPOSIT` → `WALLET_CREDIT` |
| **Payment** | Sync → **200** | Sync: debit → post → credit | — |
| **Transfer** | Sync → **200** | Sync: debit A → post → credit B | — |
| **Withdraw** | Sync accept → **200** | Sync freeze + async payout | S6 `WITHDRAW_PAYOUT` |
| **Balance read** | Sync → **200** | Query wallet only | — |

---

## Surface Map S1–S6

| ID | Surface | Protocol | Orchestration role |
|----|---------|---------|-------------------|
| S1 | Public product API | HTTPS | **Implement** |
| S2 | Accounting internal | HTTPS | **Call** (sync use cases only) |
| S3 | Domain events | Kafka | Publish / consume |
| S4 | Gateway routes | Config | Edge → BFF |
| S5 | Shared envelope | Library | `ApiResponse`, errors |
| S6 | Worker commands | RabbitMQ | **Publish** full-body envelope |
