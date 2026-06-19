# Data Flow: Async Deposit

**Feature**: `002-async-deposit` | **Date**: 2026-06-18

---

## End-to-End Flow

```
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                        ASYNC DEPOSIT — FULL DATA FLOW                               │
│                                                                                     │
│  [Deposit notification]                                                             │
│       │                                                                             │
│       │  POST /deposits/notify                                                      │
│       │  { virtualAccount, grossAmount, businessRef }                               │
│       ▼                                                                             │
│  ┌──────────────────────────────────────────────────┐                              │
│  │              app-orchestration                   │                              │
│  │                                                  │                              │
│  │  1. Auth (mTLS — ADR-022)                        │                              │
│  │  2. VA → memberId lookup (ADR-030)               │  ── unknown VA ──► ops hold  │
│  │  3. Fee computation (ADR-009)                    │                              │
│  │  4. Write OUTBOX row (ADR-013)          ◄──┐     │                              │
│  │  5. Return 202 + businessRef            same tx  │                              │
│  └──────────────────────────────────────────────────┘                              │
│       │ (outbox relay — at-least-once)                                              │
│       │                                                                             │
│       │  RabbitMQ: exchange=core.commands                                           │
│       │  queue=core.commands.bank-deposit                                           │
│       │  { commandType: BANK_DEPOSIT, businessRef, memberId,                        │
│       │    grossAmount, fee, currency }                                             │
│       ▼                                                                             │
│  ┌──────────────────────────────────────────────────┐                              │
│  │           accounting worker                      │                              │
│  │           (inside app-accounting pod)            │                              │
│  │                                                  │                              │
│  │  Phase A ── createJournal(DEPOSIT, PENDING):     │                              │
│  │    Postgres: INSERT coa_trans (PENDING)          │                              │
│  │    TigerBeetle: pending Transfer                 │                              │
│  │      debit=1111, credit=3100, amt=gross×10⁴      │                              │
│  │      id=hash(businessRef+":phaseA")              │                              │
│  │                                                  │                              │
│  │  Phase B ── confirmDeposit(coaTransId, fee):     │                              │
│  │    TigerBeetle:                                  │                              │
│  │      1. post_pending_transfer(phaseA id)         │                              │
│  │         closes 1111←3100 pending                │                              │
│  │      2. Transfer debit=3100, credit=2110         │                              │
│  │         amt=net×10⁴ (net = gross − fee)         │                              │
│  │      3. Transfer debit=3100, credit=4110         │                              │
│  │         amt=fee×10⁴                             │                              │
│  │    Validate: TB account[3100].balance = 0 ✓      │                              │
│  │    Postgres: UPDATE coa_trans → POSTED           │                              │
│  │                                                  │                              │
│  │  Publish WALLET_CREDIT command ─────────────────────────────────┐               │
│  └──────────────────────────────────────────────────┘             │               │
│                                                                    │               │
│       │ (optional) Kafka: core.events.journal-posted               │               │
│       │ { eventType: JournalPosted, useCase: DEPOSIT, ... }        │               │
│       ▼                                                            │               │
│  downstream consumers (reporting, audit — async)                   │               │
│                                                    RabbitMQ:       │               │
│                                                    WALLET_CREDIT ◄─┘               │
│                                                        │                           │
│                                                        ▼                           │
│                                             ┌────────────────────┐                 │
│                                             │   wallet worker    │                 │
│                                             │                    │                 │
│                                             │ Gate: coa_trans    │                 │
│                                             │   .status=POSTED   │                 │
│                                             │                    │                 │
│                                             │ Postgres wallet:   │                 │
│                                             │  FOR UPDATE        │                 │
│                                             │  wallet_balance    │                 │
│                                             │  INSERT wallet_tx  │                 │
│                                             │  (DEPOSIT_CREDIT,  │                 │
│                                             │   net amount)      │                 │
│                                             │  available += net  │                 │
│                                             └────────────────────┘                 │
│                                                        │                           │
│                                                        │ (optional)                │
│                                                        │ Kafka: WalletCredited     │
│                                                        ▼                           │
│                                                   member sees                      │
│                                                   updated balance                  │
└─────────────────────────────────────────────────────────────────────────────────────┘
```

---

## Error Paths

```
┌──────────────────────────────────────────────────────────────────────┐
│                       ERROR PATHS                                    │
│                                                                      │
│  Unknown VA                                                          │
│    app-orchestration ──► ops hold queue (no journal, no credit)      │
│                                                                      │
│  Phase A → Phase B mismatch (cancel / amount mismatch)               │
│    accounting worker ──► reverse Phase A only                        │
│      TigerBeetle: void_pending_transfer(phaseA id)                   │
│      Postgres: UPDATE coa_trans → FAILED                             │
│      wallet_balance: UNCHANGED (was never touched)                   │
│                                                                      │
│  Worker crash (between Phase A and Phase B)                          │
│    RabbitMQ redelivers BANK_DEPOSIT                                  │
│    createJournal idempotent on (reference_id, use_case)              │
│    confirmDeposit idempotent on already-PENDING coa_trans            │
│    Continues to POSTED safely                                        │
│                                                                      │
│  Poison message (schema invalid / memberId missing)                  │
│    After max retries → DLQ: core.commands.dlq                        │
│    Alert ops; manual triage                                          │
│                                                                      │
│  LOCKED / CLOSED wallet at credit time                               │
│    wallet worker rejects DEPOSIT_CREDIT                              │
│    POSTED journal already exists — ops resolution required           │
│    (no auto-credit, no auto-reversal of ledger)                      │
└──────────────────────────────────────────────────────────────────────┘
```

---

## State Machine: coa_trans for Deposit

```
               [deposit notify arrives]
                        │
                        ▼
              ┌─────────────────┐
              │  (not created)  │
              └────────┬────────┘
                       │ createJournal
                       ▼
              ┌─────────────────┐
              │    PENDING      │◄──── Phase A
              │ (1111 DR /      │      TB: pending transfer
              │  3100 CR)       │
              └────────┬────────┘
                       │                        │
               confirmDeposit           cancel /
                       │                mismatch
                       ▼                        ▼
              ┌─────────────────┐    ┌─────────────────┐
              │    POSTED       │    │    FAILED        │
              │ (3100=0, 2110,  │    │ (phaseA reversed │
              │  4110 credited) │    │  only)           │
              │ IMMUTABLE       │    └─────────────────┘
              └─────────────────┘
                       │
               wallet credited
               (DEPOSIT_CREDIT)
```

---

## Data Stores Touched Per Phase

| Phase | PostgreSQL `accounting` | TigerBeetle | PostgreSQL `wallet` |
|-------|------------------------|-------------|---------------------|
| 202 return | outbox INSERT | — | — |
| Phase A (PENDING) | coa_trans INSERT | pending Transfer | — |
| Phase B (POSTED) | coa_trans UPDATE | post_pending + 2 Transfers | — |
| Wallet credit | — | — | wallet_balance UPDATE + wallet_tx INSERT |

**Key invariant**: `wallet` schema is never touched until Phase B is complete and `coa_trans.status = POSTED`.
