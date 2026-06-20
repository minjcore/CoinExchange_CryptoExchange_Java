# UC-5 Task List — IBFT

> **CF page ID:** 51839642 | **Parent:** UC-5 IBFT (50332070)
> **Source of truth:** this file → push to CF
> **See also:** `specs/006-ibft/tasks.md`, `specs/006-ibft/data-model.md`

---

## Service Entry Point Map

| Who sends | Protocol | Module receives | Queue / endpoint | Domain logic |
|-----------|----------|-----------------|------------------|--------------|
| Member | HTTP POST /transfers | app-orchestration | gtelpay-public.yaml | Validate + fee + freeze + journal PENDING + 200 |
| Orchestration (sync) | HTTP | app-wallet | wallet-internal.yaml | IBFT_FREEZE |
| Orchestration (sync) | HTTP | app-accounting | accounting-internal.yaml | createJournal(IBFT, PENDING) |
| Outbox relay | **RabbitMQ** | app-payout-worker | core.commands / core.commands.ibft-payout | Napas dispatch |
| Payout worker (success) | HTTP | app-accounting | accounting-internal.yaml | confirmIbft → POSTED |
| Payout worker (success) | HTTP | app-wallet | wallet-internal.yaml | IBFT_SETTLE |
| Payout worker (fail) | HTTP | app-accounting | accounting-internal.yaml | voidIbft → FAILED |
| Payout worker (fail) | HTTP | app-wallet | wallet-internal.yaml | IBFT_RELEASE |

> IBFT accept is **sync HTTP** (before 200). Payout and outcome are async via RabbitMQ worker. Identical pattern to withdraw.

---

## Phase 1: Setup

- [ ] T001 — Verify spec-kit artifacts: plan.md, research.md, data-model.md, quickstart.md, tasks.md, contracts/ in `specs/006-ibft/`
- [ ] T002 — Draw data-flow diagram: sync accept + async payout/settle/release; annotate Napas cost leg (5100/1112) as separate from 3400 transit

---

## Phase 2: Foundational

- [ ] T003 — Service Entry Point Map confirmed in data-model.md §0; napasCost field explicit in IbftPayoutCommand
- [ ] T004 — [P] IBFT_PAYOUT command schema in core-commands.yaml: businessRef, coaTransId, principalAmount, platformFee, napasCost, grossAmount, destinationBankAccountNumber, destinationBankCode, memberId, walletId, currency
- [ ] T005 — [P] IbftSettledEvent and IbftReleasedEvent schemas in core-events.yaml
- [ ] T006 — [P] Verify POST /transfers endpoint in gtelpay-public.yaml with idempotency key; destination bank fields distinct from domestic withdraw
- [ ] T007 — [P] Verify wallet-internal.yaml has IBFT_FREEZE, IBFT_SETTLE, IBFT_RELEASE; add note in data-model.md §0
- [ ] T008 — [P] Verify accounting-internal.yaml has createJournal(IBFT); confirm confirmIbft and voidIbft are in-process methods on JournalService; confirmIbft signature includes napasCost param
- [ ] T009 — Outbox command_type IBFT_PAYOUT added to outbox table schema in spec/implementation.md
- [ ] T010 — Verify transit account 3400, Napas clearing 1112, and expense account 5100 exist in COA and TB seeding; confirm 3400=transit (expected 0), 5100=expense (accumulates DR)

---

## Phase 3: US1 — Accept (freeze + PENDING journal)

- [ ] T011 — [US1] spec/processes.md §13.5: IBFT accept narrative; TB pending Transfer 2110 DR / 3400 CR (gross), id=hash(ref+":ibftA"); sync accept sequence; napasCost NOT in gross
- [ ] T012 — [US1] design-v2/orchestration.md §15: IBFT saga steps table; TB mapping; idempotency keys; double-spend rule; ADR-025 for 1112
- [ ] T013 — [P] [US1] Gherkin: TC-IBFT-01 (happy path accept), TC-IBFT-02 (insufficient balance → 422), TC-IBFT-03 (duplicate businessRef → idempotent 200)
- [ ] T014 — [P] [US1] TigerBeetle Phase A Transfer fields match data-model.md §1.3; user_data_128=coa_trans.id set correctly

---

## Phase 4: US2 — Settle (post_pending + 3400 = 0)

- [ ] T015 — [US2] spec/processes.md §13.5: settle subsection; confirmIbft sequence; TB post_pending(ibftA) + 3 transfers (3400→4130, 3400→1112, 5100→1112); assert account[3400].balance=0; 5100 leg separate; coa_trans POSTED; wallet IBFT_SETTLE
- [ ] T016 — [P] [US2] Gherkin: TC-IBFT-04 (Napas success → 3400=0, POSTED, 5100 DR napasCost), TC-IBFT-05 (duplicate settle → idempotent)
- [ ] T017 — [P] [US2] Transit 3400=0 invariant in acceptance.md Part IV; note account[5100] NOT checked for zero (expense accumulator)

---

## Phase 5: US3 — Release (void + frozen → available)

- [ ] T018 — [US3] spec/processes.md §13.5: release + aging subsection; voidIbft; coa_trans FAILED; wallet IBFT_RELEASE; 3400=0; 5100/1112 NOT posted on release; double-spend rule; aging (T1, T2, Tmax)
- [ ] T019 — [P] [US3] Gherkin: TC-IBFT-06 (Napas fail → released, 3400=0), TC-IBFT-07 (Napas timeout → frozen stays, alert), TC-IBFT-08 (amount mismatch → reject)
- [ ] T020 — [P] [US3] Double-spend rule documented: UNKNOWN ≠ release (ADR-033)

---

## Phase 6: Polish & Cross-Cutting

- [ ] T021 — [P] Idempotency Gherkin: TC-IBFT-09 (duplicate createIbft → one freeze), TC-IBFT-10 (payout worker crash → idempotent on redelivery)
- [ ] T022 — [P] SQL invariant CI: account[3400].balance=0 after POSTED or FAILED IBFT; explicitly note account[5100] NOT checked
- [ ] T023 — Protocol Decision note: sync accept + async payout; 2 hops ≈ 70-150ms; SC-001 SLA 500ms; Napas latency unpredictable
- [ ] T024 — Napas cost explanation: 5100/1112 is expense leg on settle only; uses 1112 per ADR-025 (not 1111); net profit = platformFee − napasCost
- [ ] T025 — [P] Verify TB out-of-scope boundary: app-orchestration, app-wallet, app-payout-worker have NO direct TB client; only app-accounting → core.accounting → TB
- [ ] T026 — [P] Validate core-commands.yaml and core-events.yaml YAML parse; IBFT_PAYOUT schema matches entry-point map; napasCost field present
- [ ] T027 — Data model §3 orchestration inbound/outbound complete: IbftRequest, IbftAck, IbftPayoutCommand, IbftSettledEvent, IbftReleasedEvent; destinationBankCode present

---

## Task Summary

| Phase | Tasks | Count |
|-------|-------|-------|
| 1 — Setup | T001–T002 | 2 |
| 2 — Foundational | T003–T010 | 8 |
| 3 — US1 Accept | T011–T014 | 4 |
| 4 — US2 Settle | T015–T017 | 3 |
| 5 — US3 Release | T018–T020 | 3 |
| 6 — Polish | T021–T027 | 7 |
| **Total** | | **27** |
