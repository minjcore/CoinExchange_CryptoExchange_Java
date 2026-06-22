# Implementation Decisions and Build Order

> **CF page ID:** 49152021 | **Parent:** 🏗️ Build & Process (51315106)
> **Source of truth:** this file → push to CF

---

## Architecture Decisions (locked)

| Decision area | Locked choice | Why |
|--------------|--------------|-----|
| Repo root | Maven parent `core/`, sibling to `00_framework/` | Isolates new fiat core from legacy exchange |
| Java / Boot | Java 17; Spring Boot 3.3.x only on `app-*` modules | Clean domain jars, thin app adapters |
| GroupId | `com.gtelpay.core` | Clear ownership |
| Service split | Wallet and accounting = separate deployable pods | ADR-038; avoids in-process coupling |
| Database v1 | PostgreSQL 15+ with schemas `wallet` and `accounting` | Clean separation, easier DB split later |

---

## Repository Layout

```
core/
├── pom.xml
├── core.sharedlib/
├── core.wallet/
├── core.accounting/
├── app-orchestration/
├── app-wallet/
├── app-accounting/
├── app-wallet-worker/
└── app-accounting-worker/
```

| Module | Responsibility | Not allowed |
|--------|---------------|-------------|
| `core.sharedlib` | Shared pure-Java primitives | Spring, JPA, Flyway |
| `core.wallet` | Wallet domain, balance mutations | accounting dependency |
| `core.accounting` | Journal domain, posting logic | wallet dependency |
| `app-orchestration` | Inbound HTTP, auth, saga sequencing | Direct JPA to domain tables |
| `app-wallet` | HTTP adapter for wallet domain | accounting schema access |
| `app-accounting` | HTTP adapter for accounting domain | wallet schema access |
| `app-accounting-worker` | RabbitMQ consumer — BANK_DEPOSIT → Phase A+B | wallet schema access |
| `app-wallet-worker` | RabbitMQ consumer — WALLET_CREDIT → wallet credit | accounting schema access |

---

## Orchestration Layer: Vert.x vs Spring WebFlux

`app-orchestration` là inbound layer — nhận bank webhook, resolve VA, tính fee, publish RabbitMQ. Hai lựa chọn reactive:

| | Vert.x | Spring WebFlux |
|---|---|---|
| **Model** | Event loop, actor-style | Project Reactor (Flux/Mono) |
| **Throughput** | Cao hơn ở extreme load (native event bus) | Tốt, nhưng thêm overhead reactor pipeline |
| **Spring integration** | Manual wiring | Native — cùng stack với app-wallet / app-accounting |
| **Learning curve** | Khác paradigm so với Spring Boot | Đồng nhất với toàn bộ codebase |
| **Winpay precedent** | `lop81` dùng Vert.x, proven tại 40k txn/day | — |

**Locked:** Vert.x — vì `lop81` đã proven tại production (40k txn/day, 2M total transactions), và layer này đã có sẵn trong GtelPay stack.

---

## Wallet Transaction Idempotency

Key: `UNIQUE(wallet_id, business_ref, tx_type)`

- Same triple + same amount → return existing row, `idempotentReplay=true`
- Same triple + different amount → throw `WALLET_DUPLICATE_CONFLICT` → HTTP 409

---

## Wallet Transaction Types

| Enum | Direction | Use case |
|------|-----------|---------|
| `DEPOSIT_CREDIT` | CREDIT | After ledger POSTED |
| `TRANSFER_CREDIT` | CREDIT | Receiver wallet credit |
| `TRANSFER_DEBIT` | DEBIT | Sender wallet debit |
| `PAYMENT_DEBIT` | DEBIT | Payment step 1 |
| `PAYMENT_CREDIT` | CREDIT | Payment step 3 |
| `WITHDRAW_FREEZE` | FREEZE | Withdrawal accept |
| `WITHDRAW_RELEASE` | UNFREEZE | Payout failed / cancelled |
| `WITHDRAW_SETTLE` | DEBIT | Deduct from frozen after bank success |

---

## Build Phases

| Phase | Goal | Status |
|-------|------|--------|
| P0 | Foundation module | Done |
| P1 | Wallet domain | Done |
| P2 | Accounting domain | Done |
| P3 | First HTTP slice | Done |
| P4 | Sync payment | Done |
| P5 | Async deposit (UC-1) | **Spec complete** — `specs/002-async-deposit/` |
| P6 | Withdraw | Planned |

---

## Pre-PR Checklist

- [ ] D1–D5 reflected in Flyway and orchestration `tx_type` usage
- [ ] `core.wallet` POM has no dependency on `core.accounting`
- [ ] Payment integration test: duplicate `businessRef` does not double-debit
- [ ] No `@RestController` in domain jars
- [ ] OpenAPI `createPayment` response exposes `walletTxId` and `coaTransId`
