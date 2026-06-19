# Task List: Withdraw (UC-2)

**Feature**: `003-withdraw` | **Date**: 2026-06-19 | **Source**: `specs/003-withdraw/plan.md`

This is a design/spec repo — "tests" = Gherkin acceptance scenarios in `design-v2/acceptance.md` (conformance), gated by SQL invariant CI (ADR-031).

## Service Entry Point Map

| Who sends | Protocol | Module receives | Queue / endpoint | Domain logic |
|-----------|----------|-----------------|------------------|--------------|
| Member | HTTP `POST /withdrawals` | app-orchestration | gtelpay-public.yaml | Validate + fee + freeze + journal PENDING + 200 |
| Orchestration (sync) | HTTP | app-wallet | wallet-internal.yaml | WITHDRAW_FREEZE |
| Orchestration (sync) | HTTP | app-accounting | accounting-internal.yaml | createJournal(WITHDRAW, PENDING) |
| Outbox relay | **RabbitMQ** | app-payout-worker | exchange core.commands / key core.commands.withdraw-payout | Bank dispatch |
| Payout worker (success) | HTTP | app-accounting | accounting-internal.yaml | confirmWithdraw → POSTED |
| Payout worker (success) | HTTP | app-wallet | wallet-internal.yaml | WITHDRAW_SETTLE |
| Payout worker (fail) | HTTP | app-accounting | accounting-internal.yaml | voidWithdraw → FAILED |
| Payout worker (fail) | HTTP | app-wallet | wallet-internal.yaml | WITHDRAW_RELEASE |

> Withdraw accept is **sync HTTP** (orchestration → wallet + accounting before 200). Payout and outcome are async via RabbitMQ worker. This is the inverse of deposit.

---

## Phase 1: Setup (Shared)

- [ ] T001 Verify spec-kit feature artifacts: plan.md, research.md, data-model.md, quickstart.md,
      tasks.md, contracts/ all present in `specs/003-withdraw/`
- [ ] T002 Draw data-flow diagram in `specs/003-withdraw/data-model.md §2` — include sync accept
      path and async payout / settle / release paths

---

## Phase 2: Foundational

- [ ] T003 Service Entry Point Map confirmed in `data-model.md §0` — sync HTTP for accept,
      RabbitMQ for payout async worker
- [ ] T004 [P] `WITHDRAW_PAYOUT` command schema in `core-commands.yaml` — all fields:
      `businessRef`, `coaTransId`, `principalAmount`, `fee`, `grossAmount`, `bankAccountNumber`,
      `bankCode`, `memberId`, `walletId`, `currency`
- [ ] T005 [P] `WithdrawSettledEvent` and `WithdrawReleasedEvent` schemas in `core-events.yaml`
- [ ] T006 [P] Verify `POST /withdrawals` endpoint exists in `gtelpay-public.yaml` with idempotency
      key header and response 200/422/409
- [ ] T007 [P] Verify `wallet-internal.yaml` has freeze / settle / release endpoints
      (`WITHDRAW_FREEZE`, `WITHDRAW_SETTLE`, `WITHDRAW_RELEASE`) or confirm in-process method
- [ ] T008 [P] Verify `accounting-internal.yaml` has `createJournal` for WITHDRAW use_case;
      confirm `confirmWithdraw` and `voidWithdraw` are in-process methods on `JournalService`
      (not HTTP endpoints); add note in `data-model.md §0`
- [ ] T009 Outbox command_type `WITHDRAW_PAYOUT` added to outbox table schema in
      `spec/implementation.md` — same table as deposit, new command_type
- [ ] T010 Verify transit account 3200 exists in COA table and TB account seeding
      (`spec/foundation.md §4` / `adr/ADR-010`)

---

## Phase 3: US1 — Accept (freeze + PENDING journal) ✅ pending

- [ ] T011 [US1] `spec/processes.md §13.4` — update with TigerBeetle Phase A detail:
      TB pending Transfer `2110 DR / 3200 CR (gross)`, `id=hash(businessRef+":withdrawA")`;
      sync accept sequence (wallet freeze → journal → 200 → outbox)
- [ ] T012 [US1] `design-v2/orchestration.md §14` — update steps table with TB mapping,
      idempotency keys per leg, double-spend rule inline
- [ ] T013 [P] [US1] Gherkin in `design-v2/acceptance.md`:
      - TC-WDR-01: happy path accept — 200, available -= gross, frozen += gross
      - TC-WDR-02: insufficient balance — 422, no freeze, no journal
      - TC-WDR-03: duplicate businessRef — idempotent 200, no double-freeze
- [ ] T014 [P] [US1] TigerBeetle mapping verified: Phase A Transfer fields match `data-model.md §1.3`

---

## Phase 4: US2 — Settle (post_pending + 3200 = 0)

- [ ] T015 [US2] `spec/processes.md §13.4` — add settle subsection: `confirmWithdraw` sequence,
      TB `post_pending(withdrawA)` + 2 transfers (`3200→1111 principal`, `3200→4120 fee`),
      assert `account[3200].balance = 0`, `coa_trans` POSTED, wallet WITHDRAW_SETTLE
- [ ] T016 [P] [US2] Gherkin in `design-v2/acceptance.md`:
      - TC-WDR-04: bank success → frozen -= gross, 3200 = 0, journal POSTED
      - TC-WDR-05: duplicate settle command → idempotent no-op
- [ ] T017 [P] [US2] Transit 3200 = 0 invariant note added in `design-v2/acceptance.md`
      (Part IV / INV section); maps to ADR-010 and ADR-031 INV-03 equivalent for withdraw

---

## Phase 5: US3 — Release (void + frozen → available)

- [ ] T018 [US3] `spec/processes.md §13.4` — add release + aging subsection:
      `voidWithdraw` sequence, TB `void_pending_transfer(withdrawA)`, `coa_trans` FAILED,
      wallet WITHDRAW_RELEASE, 3200 = 0; double-spend rule (timeout ≠ release — ADR-033);
      aging: T1 (re-enqueue), T2 (poll), Tmax (alert)
- [ ] T019 [P] [US3] Gherkin in `design-v2/acceptance.md`:
      - TC-WDR-06: bank fail → frozen released, 3200 = 0, journal FAILED
      - TC-WDR-07: bank timeout — funds remain frozen, NOT released; alert after Tmax
      - TC-WDR-08: partial amount mismatch (settle amount ≠ freeze amount) → reject
- [ ] T020 [P] [US3] Double-spend rule documented: `spec/processes.md §13.4` or
      `design-v2/orchestration.md §14.2` confirms `UNKNOWN ≠ release`

---

## Phase 6: Polish & Cross-Cutting

- [ ] T021 [P] Idempotency Gherkin in `design-v2/acceptance.md`:
      - TC-WDR-09: duplicate createWithdrawal same businessRef → one freeze, one journal
      - TC-WDR-10: payout worker crash between dispatch and settle → idempotent on redelivery
- [ ] T022 [P] SQL invariant CI note: `account[3200].balance = 0` after POSTED or FAILED
      withdraw — equivalent to INV-03 for deposit; add to `design-v2/acceptance.md` Part IV
- [ ] T023 Add "Protocol Decision" note in `spec/processes.md §13.4` or new §13.4.1: why
      sync accept + async payout — DB 5-10ms + logic 10-15ms + network 20-50ms per hop;
      2 sync calls ≈ 70-150ms; SC-001 SLA 500ms; payout async because bank is unpredictable
- [ ] T024 [P] Verify TigerBeetle out-of-scope boundary: confirm `app-orchestration`,
      `app-wallet`, `app-payout-worker` have NO direct TB client; only `app-accounting` →
      `core.accounting` → TB (AC-037-01)
- [ ] T025 [P] Validate `core-commands.yaml` and `core-events.yaml` YAML parse; confirm
      WITHDRAW_PAYOUT schema matches entry-point map at top of this file
- [ ] T026 [P] Gherkin TC-WDR-11: wallet LOCKED at time of freeze → 422, no journal,
      no outbox publish (ADR-029)
- [ ] T027 Data model §4 orchestration inbound/outbound complete (WithdrawRequest,
      WithdrawAck, WithdrawPayoutCommand, WithdrawSettledEvent, WithdrawReleasedEvent)

---

## Dependencies & Execution Order

```
Phase 1 → Phase 2 → Phase 3 → Phase 4 → Phase 5 → Phase 6
(no cross-phase blockers within each phase)
```

Phase 3 T013 Gherkin can run in parallel with T011/T012 spec updates.
Phase 6 is independent polish — can start any time after Phase 3.
