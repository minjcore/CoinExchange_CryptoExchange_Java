# Task List: IBFT (UC-5)

**Feature**: `006-ibft` | **Date**: 2026-06-19 | **Source**: `specs/006-ibft/plan.md`

This is a design/spec repo — "tests" = Gherkin acceptance scenarios in `design-v2/acceptance.md` (conformance), gated by SQL invariant CI (ADR-031).

## Service Entry Point Map

| Who sends | Protocol | Module receives | Queue / endpoint | Domain logic |
|-----------|----------|-----------------|------------------|--------------|
| Member | HTTP `POST /transfers` | app-orchestration | gtelpay-public.yaml | Validate + fee + freeze + journal PENDING + 200 |
| Orchestration (sync) | HTTP | app-wallet | wallet-internal.yaml | IBFT_FREEZE |
| Orchestration (sync) | HTTP | app-accounting | accounting-internal.yaml | createJournal(IBFT, PENDING) |
| Outbox relay | **RabbitMQ** | app-payout-worker | exchange core.commands / key core.commands.ibft-payout | Napas dispatch |
| Payout worker (success) | HTTP | app-accounting | accounting-internal.yaml | confirmIbft → POSTED |
| Payout worker (success) | HTTP | app-wallet | wallet-internal.yaml | IBFT_SETTLE |
| Payout worker (fail) | HTTP | app-accounting | accounting-internal.yaml | voidIbft → FAILED |
| Payout worker (fail) | HTTP | app-wallet | wallet-internal.yaml | IBFT_RELEASE |

> IBFT accept is **sync HTTP** (orchestration → wallet + accounting before 200). Payout and outcome are async via RabbitMQ worker. Identical pattern to withdraw.

---

## Phase 1: Setup (Shared)

- [ ] T001 Verify spec-kit feature artifacts: plan.md, research.md, data-model.md, quickstart.md,
      tasks.md, contracts/ all present in `specs/006-ibft/`
- [ ] T002 Draw data-flow diagram in `specs/006-ibft/data-model.md §2` — include sync accept
      path and async payout / settle / release paths; annotate Napas cost leg (5100/1112) as
      separate from 3400 transit

---

## Phase 2: Foundational

- [ ] T003 Service Entry Point Map confirmed in `data-model.md §0` — sync HTTP for accept,
      RabbitMQ for payout async worker; napasCost field explicit in IbftPayoutCommand
- [ ] T004 [P] `IBFT_PAYOUT` command schema in `core-commands.yaml` — all fields:
      `businessRef`, `coaTransId`, `principalAmount`, `platformFee`, `napasCost`, `grossAmount`,
      `destinationBankAccountNumber`, `destinationBankCode`, `memberId`, `walletId`, `currency`
- [ ] T005 [P] `IbftSettledEvent` and `IbftReleasedEvent` schemas in `core-events.yaml`
- [ ] T006 [P] Verify `POST /transfers` endpoint exists in `gtelpay-public.yaml` with idempotency
      key header and response 200/422/409; destination bank fields distinct from domestic withdraw
- [ ] T007 [P] Verify `wallet-internal.yaml` has freeze / settle / release endpoints
      (`IBFT_FREEZE`, `IBFT_SETTLE`, `IBFT_RELEASE`) or confirm in-process method; add note
      in `data-model.md §0`
- [ ] T008 [P] Verify `accounting-internal.yaml` has `createJournal` for IBFT use_case;
      confirm `confirmIbft` and `voidIbft` are in-process methods on `JournalService`
      (not HTTP endpoints); `confirmIbft` signature includes `napasCost` param
- [ ] T009 Outbox command_type `IBFT_PAYOUT` added to outbox table schema in
      `spec/implementation.md` — same table as deposit/withdraw, new command_type
- [ ] T010 Verify transit account 3400, Napas clearing account 1112, and expense account 5100
      exist in COA table and TB account seeding (`spec/foundation.md §4` / ADR-010, ADR-025);
      confirm 3400 is transit (expected 0 at end), 5100 is expense (accumulates DR balance)

---

## Phase 3: US1 — Accept (freeze + PENDING journal)

- [ ] T011 [US1] `spec/processes.md §13.5` — new subsection: IBFT accept narrative; TB pending
      Transfer `2110 DR / 3400 CR (gross)`, `id=hash(businessRef+":ibftA")`; sync accept
      sequence (wallet freeze → journal → 200 → outbox); note napasCost NOT in gross
- [ ] T012 [US1] `design-v2/orchestration.md §15` — new section: IBFT saga steps table with
      TB mapping, idempotency keys per leg, double-spend rule inline; reference ADR-025 for
      1112 account choice
- [ ] T013 [P] [US1] Gherkin in `design-v2/acceptance.md`:
      - TC-IBFT-01: happy path accept — 200, available -= gross, frozen += gross, gross = principal + platformFee
      - TC-IBFT-02: insufficient balance — 422, no freeze, no journal, no outbox
      - TC-IBFT-03: duplicate businessRef — idempotent 200, no double-freeze
- [ ] T014 [P] [US1] TigerBeetle mapping verified: Phase A Transfer fields match
      `data-model.md §1.3`; confirm `user_data_128 = coa_trans.id` is set correctly

---

## Phase 4: US2 — Settle (post_pending + 3400 = 0)

- [ ] T015 [US2] `spec/processes.md §13.5` — add settle subsection: `confirmIbft` sequence;
      TB `post_pending(ibftA)` + 3 transfers (`3400→4130 platformFee`, `3400→1112 principal`,
      `5100→1112 napasCost`); assert `account[3400].balance = 0`; explain 5100/1112 leg is
      separate and does not affect 3400 invariant; `coa_trans` POSTED; wallet IBFT_SETTLE
- [ ] T016 [P] [US2] Gherkin in `design-v2/acceptance.md`:
      - TC-IBFT-04: Napas success → frozen -= gross, 3400 = 0, journal POSTED,
        5100 DR napasCost / 1112 CR napasCost
      - TC-IBFT-05: duplicate settle command → idempotent no-op; 3400 still = 0
- [ ] T017 [P] [US2] Transit 3400 = 0 invariant note added in `design-v2/acceptance.md`
      (Part IV / INV section); maps to ADR-010 and ADR-031; note that 5100 balance is
      NOT part of the INV — it accumulates as platform expense

---

## Phase 5: US3 — Release (void + frozen → available)

- [ ] T018 [US3] `spec/processes.md §13.5` — add release + aging subsection: `voidIbft`
      sequence; TB `void_pending_transfer(ibftA)`; `coa_trans` FAILED; wallet IBFT_RELEASE;
      3400 = 0; note 5100/1112 expense leg is NOT posted on release (no Napas call completed);
      double-spend rule (timeout ≠ release — ADR-033); aging: T1 (re-enqueue), T2 (poll
      Napas), Tmax (severity alert)
- [ ] T019 [P] [US3] Gherkin in `design-v2/acceptance.md`:
      - TC-IBFT-06: Napas fail → frozen released, 3400 = 0, journal FAILED, no 5100 posting
      - TC-IBFT-07: Napas timeout — funds remain frozen, NOT released; alert after Tmax
      - TC-IBFT-08: partial amount mismatch (settle amount ≠ freeze amount) → reject
- [ ] T020 [P] [US3] Double-spend rule documented: `spec/processes.md §13.5` or
      `design-v2/orchestration.md §15.2` confirms `UNKNOWN ≠ release`; same as ADR-033
      withdraw precedent

---

## Phase 6: Polish & Cross-Cutting

- [ ] T021 [P] Idempotency Gherkin in `design-v2/acceptance.md`:
      - TC-IBFT-09: duplicate createIbft same businessRef → one freeze, one journal
      - TC-IBFT-10: payout worker crash between dispatch and settle → idempotent on redelivery
- [ ] T022 [P] SQL invariant CI note: `account[3400].balance = 0` after POSTED or FAILED IBFT
      — equivalent to INV-03 for withdraw; add to `design-v2/acceptance.md` Part IV; explicitly
      note that `account[5100]` is NOT checked for zero (expense accumulator)
- [ ] T023 Add "Protocol Decision" note in `spec/processes.md §13.5` or new §13.5.1: why sync
      accept + async payout — same rationale as withdraw; 2 sync calls ≈ 70-150ms; SC-001 SLA
      500ms; payout async because Napas latency is unpredictable and at-least-once is required
- [ ] T024 Add "Napas cost explanation" note in `spec/processes.md §13.5` or `data-model.md`:
      5100/1112 is a platform expense leg posted only on settle; not gated by 3400=0; uses 1112
      per ADR-025 (not 1111); net profit per IBFT = platformFee − napasCost
- [ ] T025 [P] Verify TigerBeetle out-of-scope boundary: confirm `app-orchestration`,
      `app-wallet`, `app-payout-worker` have NO direct TB client; only `app-accounting` →
      `core.accounting` → TB (AC-037-01)
- [ ] T026 [P] Validate `core-commands.yaml` and `core-events.yaml` YAML parse; confirm
      IBFT_PAYOUT schema matches entry-point map at top of this file; confirm napasCost is
      present as a field
- [ ] T027 Data model §3 orchestration inbound/outbound complete (IbftRequest, IbftAck,
      IbftPayoutCommand, IbftSettledEvent, IbftReleasedEvent); confirm destinationBankCode
      field present (IBFT is external bank, not domestic)

---

## Dependencies & Execution Order

```
Phase 1 → Phase 2 → Phase 3 → Phase 4 → Phase 5 → Phase 6
(no cross-phase blockers within each phase)
```

Phase 3 T013 Gherkin can run in parallel with T011/T012 spec updates.
Phase 6 is independent polish — can start any time after Phase 3.
