# Conclusion — GtelPay Core Money Movement

> **CF page ID:** 51609737 | **Parent:** Data Plane Architecture (43581441)
> **Source of truth:** this file → push to CF
> **Reflects:** Constitution v1.0.0 (ratified 2026-06-15)

---

## 1. Three Foundation Decisions

| Decision | Detail | Consequence |
|----------|--------|-------------|
| **Two isolated domains** | `core.wallet` and `core.accounting` never call each other, never cross-schema JOIN | Independent scale and deploy; a failure in one domain does not drag the other |
| **Orchestration is the sole sequencer** | `app-orchestration` sequences flows via HTTP (sync) or outbox/queue (async) | No 2PC, no distributed transactions; failure modes are explicit |
| **TigerBeetle for hot-posting** | Only `core.accounting` opens a TB client; O(1) balance reads; native pending/post/void | Ledger at 1M+ TPS; deposit freeze/settle is atomic |

---

## 2. Deposit flow — full architecture illustrated

```
Bank webhook
    │ HTTP POST /deposits/notify → 202 (<200ms)
    ▼
app-orchestration ──[outbox + 202]────── Principle V
    │ RabbitMQ BANK_DEPOSIT
    ▼
app-accounting-worker
    ├── Phase A: TB pending(1111 DR / 3100 CR)         Principle II
    └── Phase B: TB post_pending + (3100→2110+4110)    ADR-037
         │ validate: account[3100] = 0                  INV-03
         │ RabbitMQ WALLET_CREDIT
         ▼
app-wallet-worker
    └── wallet_balance.available += net                 Principle III
         wallet_tx INSERT (DEPOSIT_CREDIT, businessRef) Principle IV
```

---

## 3. Seven Principles — recap

| # | Principle | One-line summary |
|---|-----------|-----------------|
| I | Two-Domain Separation | Wallet and accounting never touch each other — orchestration is the only bridge |
| II | Immutable Balanced Ledger | POSTED lines are append-only; corrections = a new reversing journal |
| III | Wallet Hot Path | `wallet_balance` = one-row snapshot; each change = one `wallet_tx` |
| IV | Idempotency End-to-End | Same `businessRef` → same outcome, regardless of how many times it is delivered |
| V | Orchestration Sole Sequencer | Outbox + at-least-once; no 2PC |
| VI | Money & Currency Discipline | VND v1; BigDecimal scale 4 HALF_UP; fee computed once at orchestration |
| VII | Contracts & Conformance | OpenAPI/AsyncAPI = wire source of truth; SQL invariants enforced in CI |

---

## 4. Out of scope (v1)

- Multi-currency (requires a dedicated ADR)
- Bank/NAPAS integration detail (infrastructure concern)
- Tax, payroll, billing
- Cross-domain reporting JOIN (use a read model / data warehouse)
- Blnk binary (only two PoC patterns: `WalletBalanceMonitor`, `getBalanceAt`)

---

## 5. When to write a new ADR

An ADR is mandatory before writing code if:
- Adding or changing a cross-module dependency
- Changing the protocol at a service boundary (HTTP ↔ queue)
- Introducing a new storage store
- Changing money, currency, or fee handling
- Modifying any principle in the Constitution
- Adding a TigerBeetle client to any module other than `core.accounting`

---

## 6. Next steps

**New developer:**
1. Architecture FAQ → Platform Boundaries → Business Process Deposit → Use Cases

**PR reviewer:**
1. Principle I violated? (cross-domain dependency)
2. Protocol correct? (HTTP vs queue)
3. Fee recomputed in worker? (not allowed)
4. TB client outside `core.accounting`? (not allowed)
5. POSTED row being UPDATEd? (not allowed)

---

## 7. Source of Truth

Architectural design is captured in written documents. Confluence is a mirror — when conflict, **local source wins**.

| Artifact | Location |
|----------|---------|
| Constitution (principles) | `.specify/memory/constitution.md` |
| Architecture Decision Records | `adr/` |
| Domain TRDs & diagrams | `claude/confluence/` |
| OpenAPI / AsyncAPI | `specs/contracts/` |
