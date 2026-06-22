# Platform Boundaries

> **CF page ID:** 44826626 | **Parent:** 🏛️ Architecture & Principles (51642382)
> **Source of truth:** this file → push to CF

---

## Introduction

This page defines the boundary between GtelPay's two core domains: **E-Wallet** (`core.wallet`) and **General Ledger** (`core.accounting`). This boundary is non-negotiable — violating it leads to coupling that slows the hot path, makes scaling harder, and complicates auditing.

**Who should read this page:**
- Developers before writing code that touches `core.wallet` or `core.accounting`
- Reviewers when approving PRs related to wallet, accounting, or orchestration
- Architects when designing a new use case

**This page answers:**
- Which module is allowed to call which
- Which tables belong to which domain — and why cross-schema JOINs are forbidden
- Why E-Wallet is separated from GL instead of merged as in the traditional model

**See also:** [E-Wallet & General Ledger Architecture Overview] — flow diagram, messaging direction, hot path vs backoffice per use case.

---

## One-Sentence Summary

`core.accounting` answers: *What was recorded in the General Ledger (GL)?*
`core.wallet` answers: *What is this member's spendable e-wallet balance?*
`core.reconciliation` answers: *Does EOD e-wallet balance match the GL?*

---

## Domain Responsibilities

| Area | `core.accounting` — General Ledger | `core.wallet` — E-Wallet | `core.reconciliation` |
|------|-------------------------------------|--------------------------|----------------------|
| Question | What was posted to the GL? | What can this member spend right now? | Does EOD e-wallet balance = GL net? |
| Tables | `coa_account`, `coa_trans`, `coa_trans_data` | `wallet`, `wallet_balance`, `wallet_tx` | read-only via ports |
| Schema | `accounting` | `wallet` | reads both |
| Liabilities view | Aggregate GL accounts: COA 2110 / 2120 / 2130 | Per-member: `available` + `frozen` | Discrepancy list |

---

## Hard Rules (non-negotiable)

1. **No cross-import.** `core.wallet` ↔ `core.accounting` — zero imports in either direction.
2. **No cross-schema JOIN.** SQL must not join `wallet_*` with `coa_*`.
3. **No cross-schema FK.** `wallet_tx.coa_trans_id` is a GL correlation reference only — never enforced as FK.
4. **Sync only via orchestration.** Domains communicate through `app-orchestration` (HTTP) or messaging (queue).
5. **GL never mutates e-wallet.** All wallet credit/debit via orchestration → `app-wallet` or `app-wallet-worker`.
6. **E-wallet never posts to GL.** All GL DR/CR journal entries via orchestration → `app-accounting` or `app-accounting-worker`.
7. **Reconciliation is read-only.** `core.reconciliation` reads both domains via their ports; never writes to either.

---

## Allowed Call Paths

| Caller | → GL (`core.accounting`) | → E-Wallet (`core.wallet`) |
|--------|--------------------------|---------------------------|
| `app-orchestration` | ✅ HTTP | ✅ HTTP |
| `app-accounting-worker` | ✅ (own domain) | ❌ |
| `app-wallet-worker` | ❌ | ✅ (own domain) |
| `core.reconciliation` | ✅ read-only ports | ✅ read-only ports |
| API Gateway / Partner | ❌ | ❌ |

---

## Module Roster (5 built, 4 planned)

| Module | Status | Responsibility |
|--------|--------|---------------|
| `core.sharedlib` | ✓ built | Shared pure-Java primitives |
| `core.wallet` | ✓ built | **E-Wallet** domain — real-time spendable balance per member |
| `core.accounting` | ✓ built | **General Ledger (GL)** domain — double-entry journal entries, COA |
| `core.reconciliation` | ✓ shell | EOD cross-check e-wallet balance vs GL net (spec 010) |
| `app-orchestration` | ✓ built | Inbound HTTP, auth, saga sequencing |
| `app-wallet` | planned | HTTP adapter for e-wallet domain |
| `app-accounting` | planned | HTTP adapter for GL domain |
| `app-wallet-worker` | planned | RabbitMQ WALLET_CREDIT consumer |
| `app-accounting-worker` | planned | RabbitMQ BANK_DEPOSIT → GL Phase A+B consumer |

---

## Foundation Library Scope

`core.sharedlib` — shared pure-Java primitives only:
- `ApiResponse`, `ErrorCode`, `PageRequest`, `PageResult`, `MoneyUtil`
- **Must NOT contain** domain entities, domain commands, or cross-domain abstractions

---

## Hexagonal Architecture (Ports & Adapters)

Each domain module (`core.wallet`, `core.accounting`) follows hexagonal architecture:

```
  core.wallet (E-Wallet)
  ┌─────────────────────────────────┐
  │  Domain (entities + service)     │
  │  WalletService (port)            │
  │  WalletRepository (port)         │  ← pure Java interface, no @Annotation
  │  WalletTxRepository (port)       │
  └────────────────┬────────────────┘
                   │ implemented by
  app-wallet       │
  ┌────────────────▼────────────────┐
  │  WalletRepositoryImpl (adapter) │  ← Spring Data JPA, @Repository
  │  WalletServiceImpl (adapter)    │
  └─────────────────────────────────┘
```

**Rule:** Domain jar (`core.*`) contains no Spring/JPA annotations. Only `app-*` modules have framework dependencies.

---

## Enforcement

| Layer | Mechanism |
|-------|----------|
| Compile-time | `core.wallet/pom.xml` has no `spring-boot`, `jakarta.persistence` dependency |
| Compile-time | `core.accounting/pom.xml` has no `spring-boot`, `jakarta.persistence` dependency |
| CI | Arch unit test: `noClass().in("core.wallet").should().dependOn("app.*")` |
| CI | SQL invariant tests: no cross-schema JOIN; GL transit accounts = 0 post-completion |
| Code review | PR checklist: `core.wallet` POM, `core.accounting` POM |

---

## Anti-Patterns

| Anti-pattern | Violation | Correct |
|-------------|-----------|---------|
| `@Repository` in `core.wallet` | Framework leak into domain | Port interface in domain, `@Repository` impl in `app-wallet` |
| `coa_trans` read from `core.wallet` | Cross-domain table access | Read `wallet_tx.coa_trans_id` for GL correlation only |
| `core.accounting` service call from wallet | Hard Rule 1 | Route through orchestration |
| SQL JOIN `wallet_tx` ↔ `coa_trans` | Cross-schema | Two queries, correlate by `businessRef` in app layer |
| FK from `wallet_tx` to `coa_trans` | Cross-schema FK | `coa_trans_id` column = BigInt, no FK constraint |
| Shared domain DTOs in `core.sharedlib` | Domain leak | Each domain owns its own entities |
| GL posting on hot path (sync, client waits) | GL latency blocks client response | E-wallet write first → 200 to client → GL journal entry async via worker |

---

## Conclusion & Recommendations

### Why separate e-wallet and General Ledger — vs the traditional model

The traditional model merges e-wallet + GL into a single service. GtelPay separates them. Here is why:

| | Traditional model (merged) | GtelPay (separated) |
|---|---|---|
| **Hot path** | Each transaction = e-wallet write + GL journal entry in the same DB transaction | E-wallet write → respond immediately; GL journal entry async after |
| **GL failure** | Slow or failed GL posting → entire transaction fails, member doesn't see funds | GL backlog does not affect members — `wallet_balance` is independent of `coa_*` |
| **Scale** | Must scale e-wallet + GL together even if the bottleneck is only in one | Scale `app-wallet-worker` vs `app-accounting-worker` independently by load |
| **Compliance changes** | Changing COA / GL structure → modify wallet code | Changing COA → only touch `core.accounting`, hot path unaffected |
| **Testing** | Unit tests need to mock both wallet tables and GL tables | `core.wallet` and `core.accounting` are pure Java — test independently, no Spring context needed |
| **Audit trail** | E-wallet balance and GL journal entries in the same transaction — inconsistent on partial rollback | `wallet_tx.coa_trans_id` is a GL correlation ref — two domains have separate audit trails, cross-checked via `core.reconciliation` |

**Conclusion:** Separation is not about adding complexity — it ensures the e-wallet hot path never bears risk from GL backoffice operations. Members always see the correct balance instantly. The General Ledger catches up after.

### PR Review Checklist

- [ ] `core.wallet/pom.xml` has no `core.accounting` dependency (and vice versa)
- [ ] No SQL JOIN `wallet_*` ↔ `coa_*` in the same query
- [ ] Fee computed exactly once — at `app-orchestration`, not recalculated in workers
- [ ] `wallet_tx.coa_trans_id` is a GL correlation ref — not used as a FK constraint
- [ ] Domain jar (`core.*`) has no `@RestController`, `@Repository`, or Spring Data `@Service`
- [ ] `core.reconciliation` is read-only — no INSERT/UPDATE via any port
- [ ] GL posting is not on the hot path — client response does not depend on the GL journal entry result

### Next steps

| Goal | Where to read |
|------|--------------|
| Understand deposit flow end-to-end | Business Process: Deposit |
| See GL DR/CR journal entries for each use case | Core Platform Design |
| Add a new use case | Use Cases (UC-1 → UC-9), then open an ADR |
| Separate GL DB | ADR required before writing code |
