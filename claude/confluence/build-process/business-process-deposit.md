# Business Process: Async Bank Deposit

> **CF page ID:** 44859411 | **Parent:** 🏗️ Build & Process (51315106)
> **Source of truth:** this file → push to CF
> **See also:** `specs/002-async-deposit/data-flow.md`, `specs/002-async-deposit/data-model.md`

---

## Overview

| Attribute | Value |
|-----------|-------|
| Execution shape | Async — client receives **202** before any ledger write |
| HTTP entry point | `POST /deposits/notify` → `app-orchestration` |
| Async entry (queue) | RabbitMQ `BANK_DEPOSIT` → `app-accounting-worker` |
| Transit account | **3100** — must = 0 after Phase B |
| Wallet entry (queue) | RabbitMQ `WALLET_CREDIT` → `app-wallet-worker` |
| Idempotency key | `businessRef` = `X-Idempotency-Key` header |

---

## Entry-Point Chain

```
Bank / NAPAS
    │  HTTP POST /deposits/notify
    ▼
app-orchestration
    │  1. Validate + VA → memberId lookup
    │  2. Fee computation (once, here only)
    │  3. INSERT outbox row  ─┐ same PostgreSQL tx
    │  4. Return 202          ─┘
    │
    │  outbox relay → RabbitMQ: BANK_DEPOSIT
    ▼
app-accounting-worker
    │  Phase A — createJournal(PENDING)
    │    PostgreSQL: INSERT coa_trans (status=PENDING)
    │    TigerBeetle: pending Transfer  1111 DR / 3100 CR (gross)
    │
    │  Phase B — confirmDeposit(coaTransId, fee)
    │    TigerBeetle: post_pending(phaseA)
    │    TigerBeetle: Transfer  3100 DR / 2110 CR (net)
    │    TigerBeetle: Transfer  3100 DR / 4110 CR (fee)
    │    Validate: account[3100].balance = 0
    │    PostgreSQL: coa_trans.status = POSTED
    │
    │  RabbitMQ: WALLET_CREDIT
    ▼
app-wallet-worker
    │  Gate: coa_trans.status = POSTED
    │  INSERT wallet_tx (DEPOSIT_CREDIT, net)
    │  UPDATE wallet_balance.available += net
    └── Done
```

---

## Accounting Postings

Example: 100,000 VND gross, 1,000 VND fee → 99,000 VND net to wallet.

### Phase A — PENDING

| Step | Account | DR/CR | Amount | Store |
|------|---------|-------|--------|-------|
| 1 | 1111 — Vietinbank Nostro | DR | 100,000 | TigerBeetle (pending) |
| 2 | 3100 — Transit Deposit | CR | 100,000 | TigerBeetle (pending) |

### Phase B — POSTED

| Step | Account | DR/CR | Amount | Store |
|------|---------|-------|--------|-------|
| 3 | 3100 — Transit Deposit | DR | 99,000 | TigerBeetle (post_pending net) |
| 4 | 2110 — Wallet Balance USER | CR | 99,000 | TigerBeetle |
| 5 | 3100 — Transit Deposit | DR | 1,000 | TigerBeetle (fee leg) |
| 6 | 4110 — Fee Revenue | CR | 1,000 | TigerBeetle |

**After Phase B:** `1111 +100,000` | `2110 +99,000` | `4110 +1,000` | `3100 = 0` ✓

---

## Wallet Effect

| Field | Before | After |
|-------|--------|-------|
| `wallet_balance.available` | X | X + 99,000 |
| `wallet_tx` (new row) | — | DEPOSIT_CREDIT, amount=99,000, business_ref=businessRef |

---

## Idempotency Per Layer

| Layer | Key | On duplicate |
|-------|-----|-------------|
| Orchestration | `X-Idempotency-Key` | outbox exists → return 202, no re-write |
| Accounting worker | `UNIQUE(reference_id, use_case)` on `coa_trans` | return existing PENDING row |
| TigerBeetle Phase A | Transfer ID = `hash(businessRef + ":phaseA")` | TB rejects duplicate |
| Wallet worker | `UNIQUE(wallet_id, business_ref, tx_type)` | same amount → return existing; different → 409 |

---

## Non-Happy Paths

### Unknown VA
1. Orchestration: VA lookup fails → no outbox write, no journal
2. Log ops hold; retry after mapping is added

### Phase A Reversal (cancel / mismatch)
1. Ops command → `app-accounting-worker`
2. TigerBeetle: `void_pending_transfer(phaseA)` → 1111/3100 cleared
3. `coa_trans.status = FAILED`
4. No `WALLET_CREDIT` published → wallet untouched

### Worker crash between Phase A and Phase B
1. RabbitMQ redelivers `BANK_DEPOSIT`
2. `createJournal` idempotent → returns existing PENDING
3. `confirmDeposit` continues Phase B

### Wallet LOCKED when WALLET_CREDIT arrives
1. Worker rejects DEPOSIT_CREDIT (W-O1, ADR-029)
2. Journal stays POSTED — no auto-reverse
3. Ops escalation: unlock wallet and retry

### PENDING aging
- `coa_trans.status = PENDING` past timeout → aging alert (ADR-021)
- Ops decides: void (cancel) or confirm (delayed bank confirmation)

---

## Invariants (CI-gated)

| ID | Invariant | ADR |
|----|-----------|-----|
| INV-01 | `SUM(DR) = SUM(CR)` per journal | ADR-001 |
| INV-02 | POSTED lines are append-only | ADR-001 |
| INV-03 | `account[3100].balance = 0` after completed deposit | ADR-010, ADR-031 |
| INV-04 | Wallet credit only after `coa_trans.status = POSTED` | ADR-026 |
| INV-05 | Fee computed once at orchestration — never recomputed in workers | ADR-009 |
