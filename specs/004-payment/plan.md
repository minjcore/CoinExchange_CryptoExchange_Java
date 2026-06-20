# 004-payment — Wallet Payment (UC-3)

## Status

| Field | Value |
|-------|-------|
| Feature | Wallet Payment |
| Use-case | UC-3 |
| Flow type | Synchronous, 3-commit |
| ADR | ADR-027 |
| SLA | SC-001 = 1 second |
| v1 fee model | Zero-fee (net = gross) |

---

## Problem Statement

Users need to pay merchants directly from their wallet. The payment must be atomic across three services (orchestration → wallet debit → ledger → wallet credit), consistent under concurrent submission, and settled immediately (synchronous, no async queue).

---

## Solution Overview

`app-orchestration` coordinates a 3-commit synchronous flow:

1. Debit USER wallet (available -= gross)
2. Post journal entry to `app-accounting` (TigerBeetle, immediately POSTED — no pending flag)
3. Credit MERCHANT wallet (available += net)

In v1 net = gross (zero-fee). A fee account is not involved.

---

## Architecture

### Services Involved

| Service | Role |
|---------|------|
| `app-orchestration` | Entry point; drives the 3-step sequence; owns compensation logic |
| `app-wallet` | Holds USER and MERCHANT wallet balances; exposes `PAYMENT_DEBIT` and `PAYMENT_CREDIT` |
| `app-accounting` | TigerBeetle ledger; receives `createJournal(PAYMENT, POSTED)` |

### Protocol

All calls are synchronous HTTP (REST). No message queue in the critical path.

### Sequence (ADR-027)

```
Client ──POST /payments──▶ app-orchestration
                                │
                         PAYMENT_DEBIT (HTTP)
                                │
                          app-wallet (USER -= gross)
                                │
                      createJournal(PAYMENT, POSTED) (HTTP)
                                │
                          app-accounting (TigerBeetle batch)
                                │
                         PAYMENT_CREDIT (HTTP)
                                │
                          app-wallet (MERCHANT += net)
                                │
                         ◀── 200 OK ───────────────
```

---

## Compensation & Forward Retry

### If step 3 (ledger) fails after step 2 (USER debit)

- Issue `ADJUSTMENT_CREDIT` to `app-wallet` with `businessRef = {original}:comp`
- Restores USER available balance
- Merchant is never credited
- Ledger is never reversed (ADR-001 — immutable ledger)

### If step 4 (MERCHANT credit) fails after step 3 (ledger POSTED)

- Ledger is already POSTED and immutable; do NOT reverse
- Forward-retry `PAYMENT_CREDIT` idempotently until success
- Retry uses same `businessRef` — wallet idempotency constraint prevents double-credit

---

## Idempotency

- `wallet_tx` unique constraint: `(wallet_id, business_ref, tx_type)`
- Duplicate `POST /payments` with same `businessRef` returns the same result without re-executing
- ADR-005 governs businessRef idempotency semantics

---

## Fee Model (v1)

- net = gross (zero per-transaction fee)
- Fee account not allocated in this phase
- ADR-009 specifies that fees are computed at orchestration layer when introduced in a future phase

---

## Latency Budget

| Step | Estimated |
|------|-----------|
| PAYMENT_DEBIT (HTTP) | 35–75 ms |
| createJournal (HTTP + TigerBeetle) | 35–75 ms |
| PAYMENT_CREDIT (HTTP) | 35–75 ms |
| **Total** | **~105–225 ms** |

SLA SC-001 = 1 000 ms. Budget leaves >750 ms headroom.

---

## Benchmark — In-Process JPA/Postgres (v1 impl)

> Measured 2026-06-20. Environment: MacBook local, PostgreSQL 16 in Docker, Vert.x worker pool=50, HikariCP pool-size=50, `wrk -t4 -c50 -d15s`, 4 distinct wallet pairs (artificial hot-row stress test).

### Layer isolation

| Layer tested | Endpoint | TPS | Avg latency | Notes |
|---|---|---|---|---|
| Wallet read | `GET /v1/bench/wallet/balance` | **2 566** | 19 ms | Single SELECT by memberId |
| Wallet write (debit + credit) | `POST /v1/bench/wallet` | **~430** | ~110 ms | No ledger; 4 wallet pairs |
| Full payment (wallet + JPA ledger) | `POST /v1/payments` | **143** | ~350 ms | 17 DB round trips total |

### Bottleneck breakdown

Each wallet mutation (`debit` or `credit`) performs 6 DB round trips inside one transaction:

```
1. SELECT wallet          (resolve memberId → walletId)
2. SELECT wallet_tx       (fast-path idempotency)
3. SELECT FOR UPDATE NOWAIT wallet_balance
4. SELECT wallet_tx       (recheck under lock)
5. UPDATE wallet_balance
6. INSERT wallet_tx
```

A single payment = 2 × 6 wallet ops + ~5 ledger ops = **~17 DB round trips**, 1 commit (outer `@Transactional` on `PaymentUseCase.execute()`).

**Optimizations tested and their effect:**

| Optimization | Result | Why |
|---|---|---|
| Direct UPDATE (no SELECT FOR UPDATE) | No change (~447) | Bottleneck is round-trip count, not lock hold duration |
| NOWAIT + retry decorator | No change (~428) | Retry sleep overhead cancels freed-connection benefit under hot-row |
| Worker pool 20→50, HikariCP 20→50 | No change (~428) | Thread pools were not the bottleneck; hot-row serialization was |

The ceiling is **hot-row contention** (4 wallets × 50 connections = ~12 queued writers per row). In production with 200k distinct member wallets, contention per row approaches zero — TPS scales linearly with concurrency up to the round-trip ceiling.

`SELECT FOR UPDATE NOWAIT` is kept (vs waiting): fail-fast frees the DB connection during retry backoff instead of blocking it in Postgres. `WalletCommandServiceRetryDecorator` handles retry outside `@Transactional` so each attempt starts a fresh transaction.

### Ceiling analysis

| Backend | Expected ceiling | Reason |
|---|---|---|
| JPA + Postgres (current) | ~500–1 500 TPS | Round-trip count (~17) + hot-row under stress test |
| Postgres + walletId cache | ~700–2 000 TPS | Eliminate 2 × SELECT wallet per payment |
| Redis balance + async Postgres | ~5 000–10 000 TPS | In-memory compare-and-swap |
| TigerBeetle | ~1 000 000+ TPS | Purpose-built; hardware-atomic balance |

**Winpay context**: 40 k txn/day ≈ 0.5 TPS average; peak ×100 = 50 TPS — well within current 143 TPS ceiling. TigerBeetle migration is a future concern, not a v1 blocker.

---

## Key ADRs

| ADR | Title | Relevance |
|-----|-------|-----------|
| ADR-001 | Immutable ledger | No reversal after POSTED |
| ADR-005 | businessRef idempotency | Duplicate-safe at wallet and ledger |
| ADR-009 | Fee at orchestration | Zero-fee v1; fee added in orchestration later |
| ADR-027 | Sync payment 3 commits | Defines the 3-step flow |

---

## Out of Scope (v1)

- Per-transaction fees
- Async / queue-backed payment flow
- Refund / reversal flows
- Multi-currency conversion
