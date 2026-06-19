# Platform Boundaries

> **CF page ID:** 44826626 | **Parent:** 🏛️ Architecture & Principles (51642382)
> **Source of truth:** this file → push to CF

---

## Tóm tắt một câu

`core.accounting` trả lời: *Điều gì đã xảy ra trên ledger?*
`core.wallet` trả lời: *Member này có thể chi tiêu bao nhiêu?*

---

## Domain Responsibilities

| Area | `core.accounting` | `core.wallet` |
|------|------------------|---------------|
| Question | What happened in the ledger? | How much can this member spend? |
| Tables | `coa_account`, `coa_trans`, `coa_trans_data` | `wallet`, `wallet_balance`, `wallet_tx` |
| Schema | `accounting` | `wallet` |
| Liabilities view | Aggregate: COA 2110 / 2120 / 2130 | Per-member: `available` + `frozen` |

---

## Hard Rules (non-negotiable)

1. **No cross-import.** `core.wallet` ↔ `core.accounting` — zero imports in either direction.
2. **No cross-schema JOIN.** SQL must not join `wallet_*` with `coa_*`.
3. **No cross-schema FK.** `wallet_tx.coa_trans_id` is correlation data only — never enforced as FK.
4. **Sync only via orchestration.** Domains communicate through `app-orchestration` (HTTP) or messaging (queue).
5. **Accounting never mutates wallet.** All wallet credit/debit via orchestration → `app-wallet` or `app-wallet-worker`.
6. **Wallet never posts ledger.** All DR/CR via orchestration → `app-accounting` or `app-accounting-worker`.

---

## Allowed Call Paths

| Caller | → accounting | → wallet |
|--------|-------------|---------|
| `app-orchestration` | ✅ HTTP | ✅ HTTP |
| `app-accounting-worker` | ✅ (own domain) | ❌ |
| `app-wallet-worker` | ❌ | ✅ (own domain) |
| API Gateway / Partner | ❌ | ❌ |

---

## Foundation Library Scope

`core.foundation` — shared pure-Java primitives only:
- `ApiResponse`, `ErrorCode`, `PageRequest`, `PageResult`, `MoneyUtil`
- **Must NOT contain** domain entities, domain commands, or cross-domain abstractions

---

## Hexagonal Architecture (Ports & Adapters)

Mỗi domain module (`core.wallet`, `core.accounting`) tuân theo hexagonal architecture:

```
  core.wallet
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

**Rule:** Domain jar (`core.*`) không chứa Spring/JPA annotation. Chỉ `app-*` modules mới có framework dependency.

---

## Enforcement

| Layer | Mechanism |
|-------|----------|
| Compile-time | `core.wallet/pom.xml` không có `spring-boot`, `jakarta.persistence` dependency |
| Compile-time | `core.accounting/pom.xml` không có `spring-boot`, `jakarta.persistence` dependency |
| CI | Arch unit test: `noClass().in("core.wallet").should().dependOn("app.*")` |
| CI | SQL invariant tests: no cross-schema JOIN; transit = 0 post-completion |
| Code review | PR checklist: `core.wallet` POM, `core.accounting` POM |

---

## Anti-Patterns

| Anti-pattern | Violation | Correct |
|-------------|-----------|---------|
| `@Repository` in `core.wallet` | Framework leak into domain | Port interface in domain, `@Repository` impl in `app-wallet` |
| `coa_trans` read from `core.wallet` | Cross-domain table access | Read `wallet_tx.coa_trans_id` for correlation only |
| `core.accounting` service call from wallet | Principle I | Route through orchestration |
| SQL JOIN `wallet_tx` ↔ `coa_trans` | Cross-schema | Two queries, correlate by `businessRef` in app layer |
| FK from `wallet_tx` to `coa_trans` | Cross-schema FK | `coa_trans_id` column = BigInt, no FK constraint |
| Shared domain DTOs in `core.foundation` | Domain leak | Each domain owns its own entities |
