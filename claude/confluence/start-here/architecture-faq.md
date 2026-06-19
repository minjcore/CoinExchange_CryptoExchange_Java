# Architecture FAQ — Module Decision Guide

> **CF page ID:** 51544171 | **Parent:** 📌 Start Here (51315064)
> **Source of truth:** this file → push to CF via updateConfluencePage

**Purpose:** Single-page reference for architectural decisions, module ownership, and common Q&A.

---

## 1. Module Ownership Map

| Module | Type | Owns | Does NOT own |
|--------|------|------|--------------|
| `core.foundation` | Shared library | `Money`, `Currency`, `BusinessRef`, `IdempotencyKey` | Any domain logic, any entity |
| `core.wallet` | Domain | `wallet_balance`, `wallet_tx`, `WalletService` | Journal entries, TigerBeetle client, fee computation |
| `core.accounting` | Domain | `coa_trans`, `coa_trans_data`, `LedgerService`, `TigerBeetleGateway` | Wallet balance, `wallet_tx`, member identity |
| `app-orchestration` | Saga orchestrator | Use-case flows, outbox writes, fee computation, VA→memberId | Never writes `coa_*` or `wallet_*` directly |
| `app-wallet` | HTTP gateway (sync) | Sync HTTP entry point for `core.wallet` | Async consumers, domain logic |
| `app-accounting` | HTTP gateway (sync) | Sync HTTP entry point for `core.accounting` | Async consumers, domain logic |
| `app-wallet-worker` | RabbitMQ consumer (async) | Consumes `WALLET_CREDIT` → `core.wallet` | Sync HTTP serving, orchestration |
| `app-accounting-worker` | RabbitMQ consumer (async) | Consumes `BANK_DEPOSIT` → `core.accounting` (Phase A+B) | Sync HTTP serving, wallet logic |

---

## 2. Service Entry Points: HTTP vs Queue

| Entry scenario | Protocol | Module | Address / contract |
|----------------|----------|--------|--------------------|
| Async deposit — accounting Phase A+B | **RabbitMQ** | `app-accounting-worker` | exchange `core.commands` / queue `core.commands.bank-deposit` / msg `BANK_DEPOSIT` |
| Async deposit — wallet credit | **RabbitMQ** | `app-wallet-worker` | exchange `core.commands` / queue `core.commands.wallet-credit` / msg `WALLET_CREDIT` |
| Sync use case — freeze wallet | **HTTP** | `app-wallet` | `wallet-internal.yaml` |
| Sync use case — create journal | **HTTP** | `app-accounting` | `accounting-internal.yaml` / `createJournal` |
| Bank / NAPAS inbound | **HTTP** | `app-orchestration` | `orchestration-public.yaml` / `POST /deposits/notify` → 202 |

> **Rule:** In deposit flow, `app-orchestration` NEVER calls `app-accounting` or `app-wallet` via HTTP. HTTP gateways are for synchronous use cases only.

---

## 3. Decision Guide: Which Module to Touch

| Scenario | Module(s) | Protocol at boundary |
|----------|-----------|----------------------|
| New wallet tx type | `core.wallet`, `app-wallet-worker` (async) or `app-wallet` (sync) | async = queue cmd; sync = HTTP endpoint |
| New journal use case | `core.accounting`, `app-accounting-worker` or `app-accounting` | same pattern |
| New saga flow (UC-X) | `app-orchestration`, workers | outbox → queue |
| Add shared value object | `core.foundation` only | Java library import |
| New fee rule | `app-orchestration` only | never in workers/domain |

---

## 4. Q&A: Domain Boundaries

**Q: Can accounting query wallet balance?**
No. Principle I (ADR-003). `wallet_tx.coa_trans_id` is correlation only — never JOIN.

**Q: Can wallet call confirmDeposit on accounting?**
No. Principle III (ADR-026). Wallet must never post the ledger.

**Q: Difference between app-wallet and app-wallet-worker?**
`app-wallet` = HTTP gateway for sync calls (freeze, balance). `app-wallet-worker` = RabbitMQ consumer for async `WALLET_CREDIT`.

**Q: Can orchestration write directly to coa_trans?**
No. Principle V (ADR-012). Publishes via outbox; accounting worker owns all writes to `coa_*`.

---

## 5. Q&A: TigerBeetle & Ledger

**Q: What does TigerBeetle do that PostgreSQL cannot?**
O(1) balance reads, native pending/post/void lifecycle, u128 amounts (scale 4 VND), deterministic consensus 1M+ TPS. (ADR-037)

**Q: Which module opens a TigerBeetle client?**
Only `core.accounting`. Any other module violates ADR-037 AC-037-01.

**Q: What is the pending/post/void lifecycle?**
- `pending` (Phase A): reserves amount
- `post_pending` (Phase B): commits reservation
- `void_pending` (reversal): cancels reservation

**Q: What is transit account 3100?**
Deposit transit. Must be 0 after any completed deposit (INV-03, ADR-031).

---

## 6. Q&A: Async Pattern & Messaging

**Q: Why does deposit use async queue instead of sync HTTP?**
202 SLA < 200ms must be met before any ledger write. Worker failures must not affect sync P99. At-least-once RabbitMQ + idempotent workers = durability without 2PC. (Principle V, ADR-013, ADR-041)

**Q: What is the outbox pattern?**
`BANK_DEPOSIT` command written to `outbox` table in same PostgreSQL transaction as 202. Relay publishes to RabbitMQ. (ADR-013, FR-009)

**Q: What if app-accounting-worker crashes between Phase A and Phase B?**
RabbitMQ redelivers. `createJournal` idempotent on `(reference_id, use_case)` — existing PENDING returned. `confirmDeposit` proceeds to Phase B. (ADR-005, ADR-041)

---

## 7. Q&A: Money, Fee & Idempotency

**Q: Where is fee computed?**
In `app-orchestration`, exactly once. Workers receive final `grossAmount` and `fee` — never recompute. (Principle VI, ADR-009)

**Q: What is businessRef?**
End-to-end idempotency key = `X-Idempotency-Key`. Flows through outbox → `BANK_DEPOSIT` → `coa_trans.reference_id` → `WALLET_CREDIT` → `wallet_tx.business_ref`. (Principle IV, ADR-005)

**Q: How are amounts stored in TigerBeetle?**
u128 integers. VND × 10⁴. Wire: decimal string `"100000.0000"`. Never `double`/`float`. (ADR-028)

---

## 8. Q&A: Constitution & ADRs

**7 Constitution Principles (brief):**

| # | Principle | Core rule | ADR |
|---|-----------|-----------|-----|
| I | Two-Domain Separation | wallet/accounting isolated; no cross JOIN/FK/import | ADR-002, ADR-003 |
| II | Immutable Balanced Ledger | POSTED = append-only; transit nets to zero | ADR-001, ADR-010 |
| III | Wallet Hot Path | wallet_balance = single-row read; one wallet_tx per change | ADR-004 |
| IV | Idempotency End-to-End | businessRef keyed; duplicate = identical outcome | ADR-005 |
| V | Orchestration Sole Sequencer | outbox at-least-once; no 2PC | ADR-006, ADR-013 |
| VI | Money & Currency Discipline | VND v1; BigDecimal scale 4 HALF_UP; fee once at orchestration | ADR-019, ADR-028 |
| VII | Contracts & Conformance | OpenAPI/AsyncAPI = wire source of truth; SQL invariants in CI | ADR-018, ADR-031 |

---

## 9. Anti-Patterns

| Anti-pattern | Why wrong | Correct alternative |
|--------------|-----------|---------------------|
| `SELECT ... FROM wallet.wallet_tx JOIN accounting.coa_trans` | Cross-schema JOIN — Principle I (ADR-003) | Two separate queries, correlate in app via businessRef |
| `INSERT INTO coa_trans` from `app-orchestration` | Direct domain write — Principles I+V | Publish BANK_DEPOSIT via outbox |
| Recomputing fee in worker | Fee computed twice — Principle VI (ADR-009) | Receive grossAmount+fee from orchestration |
| TB client in app-wallet / app-orchestration | Only core.accounting may access TB — ADR-037 AC-037-01 | Route through core.accounting |
| UPDATE POSTED `coa_trans_data` | POSTED immutable — Principle II | New reversing journal |
| `double`/`float` for money | Floating-point rounding — Principle VI | BigDecimal scale 4 HALF_UP |
| Call app-wallet-worker HTTP from orchestration | Workers are queue consumers — Principle V | Publish WALLET_CREDIT to RabbitMQ |
| Cross-domain import: `import core.accounting.*` in `core.wallet` | Domain-to-domain import — Principle I | Only core.foundation is shared |
