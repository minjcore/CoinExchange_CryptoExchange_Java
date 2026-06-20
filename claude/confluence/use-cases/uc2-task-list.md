# UC-2 Task List — Withdraw

> **CF page ID:** 51806750 | **Parent:** UC-2 Withdraw (50332049)
> **Source of truth:** this file → push to CF
> **See also:** `specs/003-withdraw/tasks.md`, `specs/003-withdraw/data-model.md`

---

## Service Entry Point Map

| Who sends | Protocol | Module receives | Queue / endpoint | Domain logic |
|-----------|----------|-----------------|------------------|--------------|
| Member | HTTP POST /withdrawals | app-orchestration | gtelpay-public.yaml | Validate + fee + WITHDRAW_FREEZE + createPendingWithdraw + 200 |
| Orchestration (sync) | HTTP | app-wallet | wallet-internal.yaml | WITHDRAW_FREEZE |
| Orchestration (sync) | HTTP | app-accounting | accounting-internal.yaml | createPendingWithdraw (PENDING, Phase A) |
| Outbox relay | **RabbitMQ** | app-payout-worker | core.commands / core.commands.withdraw-payout | Bank dispatch |
| Payout worker (success) | HTTP | app-accounting | accounting-internal.yaml | confirmWithdraw → POSTED |
| Payout worker (success) | HTTP | app-wallet | wallet-internal.yaml | WITHDRAW_SETTLE |
| Payout worker (fail) | HTTP | app-accounting | accounting-internal.yaml | voidWithdraw → FAILED |
| Payout worker (fail) | HTTP | app-wallet | wallet-internal.yaml | WITHDRAW_RELEASE |

> Accept is **sync HTTP** (freeze + journal before 200). Payout and outcome are async via RabbitMQ worker.

---

## Phase 1: Setup

- [x] T001 — Spec artifacts created: plan.md, research.md, data-model.md, quickstart.md, tasks.md in `specs/003-withdraw/`
- [ ] T002 — Data-flow diagram in `data-model.md §2` — sync accept path + async settle/release paths

---

## Phase 2: Foundational

- [x] T003 — Service Entry Point Map confirmed (sync HTTP accept, RabbitMQ async payout)
- [ ] T004 — `WITHDRAW_PAYOUT` command schema in `core-commands.yaml`
- [ ] T005 — `WithdrawSettledEvent` and `WithdrawReleasedEvent` schemas in `core-events.yaml`
- [x] T006 — `POST /withdrawals` endpoint implemented in app-orchestration
- [x] T007 — wallet-internal: WITHDRAW_FREEZE / WITHDRAW_SETTLE / WITHDRAW_RELEASE in-process
- [x] T008 — accounting-internal: createPendingWithdraw / confirmWithdraw / voidWithdraw implemented
- [ ] T009 — Outbox command_type `WITHDRAW_PAYOUT` added to outbox schema
- [x] T010 — Transit account 3200 seeded in COA (V2 migration + CoaBootstrap)

---

## Phase 3: US1 — Accept ✅

- [ ] T011 — `spec/processes.md §13.4` — Phase A TB detail: pending Transfer 2110 DR / 3200 CR
- [ ] T012 — `design-v2/orchestration.md §14` — steps table with TB mapping + idempotency keys
- [ ] T013 — Gherkin TC-WDR-01/02/03: happy path, insufficient balance, duplicate accept
- [ ] T014 — TigerBeetle mapping verified against `data-model.md §1.3`

---

## Phase 4: US2 — Settle

- [ ] T015 — `spec/processes.md §13.4` — settle subsection: confirmWithdraw + 3200 = 0 + POSTED
- [ ] T016 — Gherkin TC-WDR-04/05: bank success → 3200=0 POSTED; duplicate settle idempotent
- [ ] T017 — Transit 3200 = 0 invariant added to `design-v2/acceptance.md` Part IV

---

## Phase 5: US3 — Release

- [ ] T018 — `spec/processes.md §13.4` — release + aging: voidWithdraw + FAILED + 3200=0; ADR-033 T1/T2/Tmax
- [ ] T019 — Gherkin TC-WDR-06/07/08: bank fail release; timeout frozen (NOT released); partial mismatch reject
- [ ] T020 — Double-spend rule documented: UNKNOWN ≠ release (ADR-033)

---

## Phase 6: Polish

- [ ] T021 — Gherkin TC-WDR-09/10: idempotent accept; payout worker crash idempotent redelivery
- [ ] T022 — SQL invariant CI: account[3200].balance = 0 after POSTED or FAILED (INV-W)
- [ ] T023 — Protocol decision note: why sync accept + async payout
- [ ] T024 — Verify TB boundary: only `app-accounting` → `core.accounting` → TB; no direct TB client elsewhere
- [ ] T025 — Validate `core-commands.yaml` + `core-events.yaml` YAML parse
- [ ] T026 — Gherkin TC-WDR-11: wallet LOCKED → 422, no journal, no outbox (ADR-029)
- [ ] T027 — Data model §4 orchestration inbound/outbound complete

---

## Task Summary

| Phase | Tasks | Count | Status |
|-------|-------|-------|--------|
| 1 — Setup | T001–T002 | 2 | 1/2 done |
| 2 — Foundational | T003–T010 | 8 | 6/8 done |
| 3 — US1 Accept | T011–T014 | 4 | 0/4 |
| 4 — US2 Settle | T015–T017 | 3 | 0/3 |
| 5 — US3 Release | T018–T020 | 3 | 0/3 |
| 6 — Polish | T021–T027 | 7 | 0/7 |
| **Total** | | **27** | **7/27** |
