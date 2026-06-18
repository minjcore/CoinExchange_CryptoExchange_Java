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
External ──s1-http-public──► Gateway ──s1-http-public──► Orchestration (BFF)
                                           ├──► core.wallet      (sync HTTP via app-wallet)
                                           ├──► core.accounting  (sync HTTP via app-accounting)
                                           ├──► s6-rabbitmq-cmds (worker commands, async)
                                           └──► s3-kafka-events  (domain events, async)
```

---

## Module Roles

| Khối | Thành phần | Vai trò |
|------|-----------|---------|
| Public edge | API Gateway, bank webhook | Route vào orchestration qua s1-http-public |
| Application | `app-orchestration` | Điều phối flow, fee computation, outbox, auth |
| Domain cores | `core.wallet`, `core.accounting`, `core.shared` | Business capability, không cross-import |
| Async infra | s6-rabbitmq-cmds, s3-kafka-events, workers | Command handling, event fan-out |
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
| **Deposit** | Async → **202** | Async qua worker | s6-rabbitmq-cmds `BANK_DEPOSIT` → `WALLET_CREDIT` |
| **Payment** | Sync → **200** | Sync: debit → post → credit | — |
| **Transfer** | Sync → **200** | Sync: debit A → post → credit B | — |
| **Withdraw** | Sync accept → **200** | Sync freeze + async payout | s6-rabbitmq-cmds `WITHDRAW_PAYOUT` |
| **Balance read** | Sync → **200** | Query wallet only | — |

---

## Surface Map (s1–s6)

| Surface | Description | Protocol | Orchestration role |
|---------|-------------|---------|-------------------|
| s1-http-public | Public product API | HTTPS | **Implement** |
| s2-http-internal | Accounting internal | HTTPS | **Call** (sync use cases only) |
| s3-kafka-events | Domain events | Kafka | Publish / consume |
| s4-gateway-config | Gateway routes | Config | Edge → BFF |
| s5-shared-envelope | Shared envelope | Library | `ApiResponse`, errors |
| s6-rabbitmq-cmds | Worker commands | RabbitMQ | **Publish** full-body envelope |
