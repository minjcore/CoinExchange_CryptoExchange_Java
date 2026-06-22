# Implementation Decisions and Build Order

> **CF page ID:** 49152021 | **Parent:** 🏗️ Build & Process (51315106)
> **Source of truth:** this file → push to CF

---

## Architecture Decisions (locked)

| Decision area | Locked choice | Why |
|--------------|--------------|-----|
| Repo root | Maven parent at `10_core/` inside monorepo `core/` | Isolates new fiat core from legacy modules (01_wallet_rpc, 02–09) |
| Java / Boot | Java 17; Spring Boot 3.3.x only on `app-*` modules | Clean domain jars, thin app adapters |
| GroupId | `com.gtelpay.core` | Clear ownership |
| Service split | Wallet and accounting = separate deployable pods | ADR-038; avoids in-process coupling |
| Database | 3 databases: gtelpay DB (wallet+accounting, :5432), Blnk DB (balance engine, :5433), future accounting DB separate | Blnk as wallet backend; schema isolation |
| Wallet engine | Blnk Finance 0.14.5 at localhost:5002 | Open-source balance engine; `BlnkWalletGateway implements WalletGateway` replaces `core.wallet` in S2 |
| Messaging | RabbitMQ = inbound commands; Kafka = outbound domain events | RabbitMQ: BANK_DEPOSIT → WALLET_CREDIT; Kafka: JournalPosted, WalletCredited |

---

## Repository Layout

```
core/
├── pom.xml
├── core.sharedlib/        ✓ built
├── core.wallet/           ✓ built
├── core.accounting/       ✓ built
├── core.reconciliation/   ✓ shell (EOD cross-check)
├── app-orchestration/     ✓ built
├── app-wallet/            planned
├── app-accounting/        planned
├── app-wallet-worker/     planned
└── app-accounting-worker/ planned
```

| Module | Responsibility | Not allowed |
|--------|---------------|-------------|
| `core.sharedlib` | Shared pure-Java primitives | Spring, JPA, Flyway |
| `core.wallet` | Wallet domain, balance mutations | accounting dependency |
| `core.accounting` | Journal domain, posting logic | wallet dependency |
| `core.reconciliation` | EOD cross-check wallet_balance vs coa_balance (spec 010) | Direct JPA — uses wallet + accounting ports |
| `app-orchestration` | Inbound HTTP, auth, saga sequencing | Direct JPA to domain tables |
| `app-wallet` | HTTP adapter for wallet domain | accounting schema access |
| `app-accounting` | HTTP adapter for accounting domain | wallet schema access |
| `app-accounting-worker` | RabbitMQ consumer — BANK_DEPOSIT → Phase A+B | wallet schema access |
| `app-wallet-worker` | RabbitMQ consumer — WALLET_CREDIT → wallet credit | accounting schema access |

---

## Orchestration Layer: Vert.x vs Spring WebFlux

`app-orchestration` is the inbound layer — receives bank webhooks, resolves VA, computes fee, publishes to RabbitMQ. Two reactive options:

| | Vert.x | Spring WebFlux |
|---|---|---|
| **Model** | Event loop, actor-style | Project Reactor (Flux/Mono) |
| **Throughput** | Higher at extreme load (native event bus) | Good, but adds reactor pipeline overhead |
| **Spring integration** | Manual wiring | Native — same stack as app-wallet / app-accounting |
| **Learning curve** | Different paradigm from Spring Boot | Consistent with the entire codebase |
| **Winpay precedent** | `lop81` uses Vert.x, proven at 40k txn/day | — |

**Locked: Vert.x** — `lop81` has proven it at production (40k txn/day, 2M total transactions), and this layer is already in the GtelPay stack.

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
| `IBFT_FREEZE` | FREEZE | IBFT accept — freeze gross |
| `IBFT_SETTLE` | DEBIT | Deduct from frozen after interbank success |
| `IBFT_RELEASE` | UNFREEZE | IBFT payout failed / cancelled |

---

## Zero-Fee Handling (Bug Reference)

`MoneyUtil.parseAmount()` rejects zero — use `normalizeAllowZero()` for fee fields that are legitimately zero.

| Site | Fix |
|------|-----|
| `DepositNotifyUseCase.publishBankDeposit()` | `normalizeAllowZero(BigDecimal.ZERO)` for deposit fee |
| `HttpServerVerticle` settle handlers | Default fee to `null` (null guard in use case skips parseAmount) |

---

## Build Phases

| Phase | Goal | Status |
|-------|------|--------|
| P0 | Foundation module (`core.sharedlib`) | Done |
| P1 | Wallet domain | Done |
| P2 | Accounting domain | Done |
| P3 | First HTTP slice | Done |
| P4 | Sync payment | Done |
| P5 | Async deposit (UC-1) — `specs/002-async-deposit/` | Done |
| P6 | Withdraw (UC-2) — freeze / settle / release | Done |
| P7 | Internal Transfer (UC-4) | Done |
| P8 | IBFT (UC-5) — freeze / settle / release | Done |
| P9 | HTTP middleware chain — `specs/012-http-middleware/` | Spec complete |
| P10 | EOD reconciliation — `core.reconciliation/` shell (spec 010) | Shell built |
| P11 | Blnk wallet integration — `BlnkWalletGateway` | Planned |

---

## Performance Baseline (k6 load tests)

| Scenario | Result | Date |
|----------|--------|------|
| Transfer 100 RPS, 60s | 0% error, p95=4ms, p99<5ms ✓ | 2026-06-22 |
| Withdraw 50 VU, 60s | Scripts ready — `k6/scenarios/withdraw.js` | — |
| IBFT settle+release 30 VU | Scripts ready — `k6/scenarios/ibft.js` | — |

---

## Pre-PR Checklist

- [ ] D1–D5 reflected in Flyway and orchestration `tx_type` usage
- [ ] `core.wallet` POM has no dependency on `core.accounting`
- [ ] Payment integration test: duplicate `businessRef` does not double-debit
- [ ] No `@RestController` in domain jars
- [ ] OpenAPI `createPayment` response exposes `walletTxId` and `coaTransId`
- [ ] Fee fields default to `null` (not `"0"`) in HTTP handlers
