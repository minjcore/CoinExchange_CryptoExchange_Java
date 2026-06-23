# UC-1 Async Deposit — Feature Spec

> **CF page ID:** 51609603 | **Parent:** 📋 Feature Specs (GtelPay space)
> **Source of truth:** `specs/002-async-deposit/confluence-draft.md` → push to CF

**Feature:** UC-1 Async Bank Deposit  
**Spec directory:** `specs/002-async-deposit/` (source of truth)  
**Created:** 2026-06-18  
**Status:** Draft  
**Build phase:** P5 (per Implementation Decisions page)

**Summary**  
A USER member receives a bank transfer (NAPAS 247 or virtual account top-up). Orchestration returns 202 immediately. The accounting worker processes two-phase journal posting via TigerBeetle. The wallet is credited only after the journal is POSTED. Idempotent on `businessRef` end-to-end. Related ADRs: ADR-006, ADR-013, ADR-024, ADR-030, ADR-035, ADR-037.

---

# User Stories

## US1 — Deposit Lands in Wallet (Priority: P1)

A USER member receives a bank transfer and sees their GtelPay wallet balance increase by the net deposit amount (gross minus fee). The member does not wait while the ledger records the transaction.

**Independent test:** Send a NAPAS 247 notification; observe wallet balance increases by net amount; verify system returns 202 before ledger write completes.

1. **Given** a member with a mapped virtual account, **When** a bank deposit notification arrives, **Then** the system acknowledges with 202 and wallet balance increases by net amount only after journal is POSTED.
2. **Given** the same deposit notification delivered twice with the same `businessRef`, **When** both are processed, **Then** wallet is credited exactly once (idempotent replay).
3. **Given** a deposit with a fee, **When** journal is POSTED, **Then** wallet is credited with gross minus fee (net).

## US2 — Unknown Virtual Account Held for Ops (Priority: P2)

A bank notification arrives for a VA not mapped to any member. The system holds it PENDING for manual ops resolution — no erroneous credit, no fund loss.

1. **Given** a deposit notification for an unmapped VA, **When** processed, **Then** no wallet credit is issued and the deposit stays PENDING for ops.
2. **Given** a mapped VA deposit followed by a VA mapping change, **When** mapping is updated, **Then** historical POSTED journals are not retroactively altered.

## US3 — Deposit Failure and PENDING Aging (Priority: P3)

A deposit reaches PENDING (phase A) but phase B never arrives — bank mismatch or cancellation. System ages and reverses the PENDING journal without touching the wallet (which was never credited).

1. **Given** a deposit in PENDING state and a bank cancellation, **When** reversal runs, **Then** phase-A reversal clears 1111 and 3100 only; wallet unchanged.
2. **Given** a deposit with a bank amount mismatch, **When** confirmation fails validation, **Then** phase-A journal is reversed and no phase-B POSTED record is created.

---

# End-to-End Data Flow

```
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                        ASYNC DEPOSIT — FULL DATA FLOW                               │
│                                                                                     │
│  [Deposit notification]                                                             │
│       │ POST /deposits/notify                                                       │
│       │ { virtualAccount, grossAmount, businessRef }                                │
│       ▼                                                                             │
│  ┌──────────────────────────────────────────────────┐                              │
│  │              app-orchestration                   │                              │
│  │  1. Auth (mTLS — ADR-022)                        │                              │
│  │  2. VA → memberId lookup (ADR-030)               │ ── unknown VA ──► ops hold   │
│  │  3. Fee computation (ADR-009)                    │                              │
│  │  4. Write OUTBOX row (ADR-013)          ◄──┐     │                              │
│  │  5. Return 202 + businessRef            same tx  │                              │
│  └──────────────────────────────────────────────────┘                              │
│       │ (outbox relay — at-least-once)                                              │
│       │ RabbitMQ: core.commands.bank-deposit                                        │
│       │ { commandType: BANK_DEPOSIT, businessRef, memberId,                         │
│       │   grossAmount, fee, currency }                                              │
│       ▼                                                                             │
│  ┌──────────────────────────────────────────────────┐                              │
│  │           accounting worker                      │                              │
│  │  Phase A ── createJournal(DEPOSIT, PENDING):     │                              │
│  │    PostgreSQL: INSERT coa_trans (PENDING)         │                              │
│  │    TigerBeetle: pending Transfer                 │                              │
│  │      debit=1111, credit=3100, amt=gross×10⁴      │                              │
│  │      id=hash(businessRef+":phaseA")              │                              │
│  │                                                  │                              │
│  │  Phase B ── confirmDeposit(coaTransId, fee):     │                              │
│  │    TigerBeetle:                                  │                              │
│  │      1. post_pending_transfer(phaseA id)         │                              │
│  │      2. Transfer debit=3100, credit=2110, net    │                              │
│  │      3. Transfer debit=3100, credit=4110, fee    │                              │
│  │    Validate: TB account[3100].balance = 0 ✓      │                              │
│  │    PostgreSQL: UPDATE coa_trans → POSTED          │                              │
│  │  Publish WALLET_CREDIT command ──────────────────────────────────┐              │
│  └──────────────────────────────────────────────────┘              │              │
│       │ (optional) Kafka: core.accounting.journal-posted            │              │
│       ▼                                              RabbitMQ:      │              │
│  downstream consumers (reporting, audit)          WALLET_CREDIT ◄──┘              │
│                                                        │                           │
│                                                        ▼                           │
│                                             ┌────────────────────┐                 │
│                                             │   wallet worker    │                 │
│                                             │ Gate: coa_trans    │                 │
│                                             │   .status=POSTED   │                 │
│                                             │ PostgreSQL wallet: │                 │
│                                             │  FOR UPDATE        │                 │
│                                             │  wallet_balance    │                 │
│                                             │  INSERT wallet_tx  │                 │
│                                             │  (DEPOSIT_CREDIT,  │                 │
│                                             │   net amount)      │                 │
│                                             │  available += net  │                 │
│                                             └────────────────────┘                 │
│                                                        │ (optional)                │
│                                                        │ Kafka: WalletCredited     │
│                                                        ▼                           │
│                                                   member sees updated balance      │
└─────────────────────────────────────────────────────────────────────────────────────┘
```

## Data Stores Touched Per Phase

| Phase | PostgreSQL `accounting` | TigerBeetle | PostgreSQL `wallet` |
|-------|------------------------|-------------|---------------------|
| 202 return | outbox INSERT | — | — |
| Phase A (PENDING) | coa_trans INSERT | pending Transfer (1111→3100) | — |
| Phase B (POSTED) | coa_trans UPDATE | post_pending + 2 Transfers (3100→2110, 3100→4110) | — |
| Wallet credit | — | — | wallet_balance UPDATE + wallet_tx INSERT |

**Key invariant**  
The `wallet` schema is never touched until Phase B is complete and `coa_trans.status = POSTED`. Transit account 3100 must net to zero after Phase B.

---

# Error Paths

| Error | Behavior |
|-------|----------|
| Unknown virtual account | app-orchestration routes to ops hold queue — no journal, no wallet credit |
| Phase A→B mismatch (cancel / amount mismatch) | TigerBeetle: `void_pending_transfer(phaseA id)`; PostgreSQL: coa_trans → FAILED; wallet unchanged |
| Worker crash between Phase A and Phase B | RabbitMQ redelivers BANK_DEPOSIT; `createJournal` idempotent on `(reference_id, use_case)`; `confirmDeposit` idempotent on already-PENDING journal; completes correctly |
| Poison message (schema invalid / memberId missing) | After max retries → DLQ `core.commands.dlq`; alert ops |
| LOCKED/CLOSED wallet at credit time | wallet worker rejects DEPOSIT_CREDIT; POSTED journal already exists — ops resolution required; no auto-credit or auto-reversal |

---

# Functional Requirements

- **FR-001**: Return 202 to deposit notifier before any accounting journal write.
- **FR-002**: Resolve virtual account → memberId + wallet before creating any journal entry.
- **FR-003**: Create PENDING journal (phase A: 1111 DR gross + 3100 CR gross) on valid deposit notification.
- **FR-004**: Credit member wallet only after journal status is POSTED (phase B complete).
- **FR-005**: Encapsulate phase B inside `JournalService.confirmDeposit(coaTransId, fee)` — orchestration must not construct phase-B lines directly.
- **FR-006**: Verify transit account 3100 nets to zero after phase B before marking POSTED.
- **FR-007**: Wallet credit amount = gross − fee (net); fee computed by orchestration.
- **FR-008**: Idempotent end-to-end on `businessRef` — duplicate notifications or queue deliveries produce at most one credit.
- **FR-009**: Publish deposit command via outbox-backed messaging so an orchestration crash after 202 does not lose the command.
- **FR-010**: Route deposits for unmapped VAs to ops hold queue — no erroneous journal or wallet credit.
- **FR-011**: Phase-A reversal clears only 1111/3100; wallet balance is unchanged.
- **FR-012**: Wallet credit supported via three paths (Kafka, RabbitMQ, HTTP sync); all paths gate on POSTED status. (ADR-024)

---

# Success Criteria

- **SC-001**: Deposit notifier receives 202 in under 200 ms regardless of ledger write duration.
- **SC-002**: Member wallet reflects net deposit within 5 seconds of journal POSTED under normal load.
- **SC-003**: Duplicate deposit notifications with same `businessRef` produce exactly one wallet credit — zero double-credits in soak testing.
- **SC-004**: 100% of POSTED deposit journals have transit account 3100 at net zero — verifiable by SQL invariant suite (ADR-031 INV-03).
- **SC-005**: Deposits for unmapped VAs never produce a wallet credit — zero erroneous credits in ops audit.
- **SC-006**: Accounting worker crash and restart during phase B results in correct eventual completion with no duplicate journal lines.
