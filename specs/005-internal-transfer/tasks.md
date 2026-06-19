# Task List: Internal Transfer (UC-4)

**Feature**: `005-internal-transfer` | **Date**: 2026-06-19 | **Source**: `specs/005-internal-transfer/plan.md`

This is a design/spec repo — "tests" = Gherkin acceptance scenarios in `design-v2/acceptance.md` (conformance), gated by SQL invariant CI (ADR-031).

## Service Entry Point Map

| Who sends | Protocol | Module receives | Queue / endpoint | Domain logic |
|-----------|----------|-----------------|------------------|--------------|
| Member | HTTP `POST /transfers` | app-orchestration | gtelpay-public.yaml | Validate sender≠receiver + fee + TRANSFER_DEBIT + createJournal POSTED + TRANSFER_CREDIT + 200 |
| Orchestration step 1 (sync) | HTTP | app-wallet | wallet-internal.yaml | TRANSFER_DEBIT (sender) |
| Orchestration step 2 (sync) | HTTP | app-accounting | accounting-internal.yaml | createJournal(TRANSFER, POSTED) → TB 3-transfer batch |
| Orchestration step 3 (sync) | HTTP | app-wallet | wallet-internal.yaml | TRANSFER_CREDIT (receiver) |
| Orchestration compensation (sync) | HTTP | app-wallet | wallet-internal.yaml | ADJUSTMENT_CREDIT (sender, only if step 2 fails after step 1) |

> Internal transfer is **fully synchronous**. No RabbitMQ, no outbox, no async worker. All steps complete before 200.

---

## Phase 1: Setup (Shared)

- [ ] T001 Verify spec-kit feature artifacts: plan.md, research.md, data-model.md, quickstart.md,
      tasks.md all present in `specs/005-internal-transfer/`
- [ ] T002 Draw data-flow diagram in `specs/005-internal-transfer/data-model.md §2` — sync
      3-step sequence, compensation path, forward-retry path

---

## Phase 2: Foundational

- [ ] T003 Service Entry Point Map confirmed in `data-model.md §0` — fully sync HTTP for all
      steps; no RabbitMQ; no outbox
- [ ] T004 [P] Verify `POST /transfers` endpoint exists in `gtelpay-public.yaml` with
      `X-Idempotency-Key` header and responses 200/422/409; add if missing
- [ ] T005 [P] Verify `wallet-internal.yaml` has TRANSFER_DEBIT, TRANSFER_CREDIT,
      ADJUSTMENT_CREDIT endpoints (or confirm in-process method on WalletService); document
      in `data-model.md §0`
- [ ] T006 [P] Verify `accounting-internal.yaml` has `createJournal` supporting
      `use_case=TRANSFER` and `status=POSTED` (no PENDING state for this use case); add note
      in `data-model.md §0`
- [ ] T007 [P] Verify transit account 3300 exists in COA table and TB account seeding
      (`spec/foundation.md §4` / `adr/ADR-010`); verify 4130 (transfer fee revenue) exists
- [ ] T008 Confirm `coa_trans` unique constraint `(reference_id, use_case)` covers
      `use_case=TRANSFER`; document in `data-model.md §1.1`
- [ ] T009 [P] Verify `wallet_tx` unique constraint `(wallet_id, business_ref, tx_type)`
      covers all three transfer TX types (TRANSFER_DEBIT, TRANSFER_CREDIT, ADJUSTMENT_CREDIT)

---

## Phase 3: US1 — Transfer happy path (T010–T013)

- [ ] T010 [US1] `spec/processes.md §13.5` — create with 3-step sequence narrative:
      (1) TRANSFER_DEBIT sender, (2) createJournal(TRANSFER, POSTED) TB batch,
      (3) TRANSFER_CREDIT receiver; assert account[3300].balance = 0; return 200;
      include TB Transfer ID formula: `hash(businessRef+":debit")`,
      `hash(businessRef+":credit")`, `hash(businessRef+":4130")`
- [ ] T011 [US1] `design-v2/orchestration.md §15` — update steps table with TB mapping,
      idempotency keys per leg, sender≠receiver guard inline, fee treatment note
- [ ] T012 [P] [US1] Gherkin in `design-v2/acceptance.md`:
      - TC-TFR-01: happy path — 200, sender available -= gross, receiver available += net,
        3300.balance = 0, coa_trans POSTED
      - TC-TFR-02: insufficient balance — 422, no TRANSFER_DEBIT, no journal
      - TC-TFR-03: duplicate businessRef — idempotent 200, no double-debit, no double-credit
- [ ] T013 [P] [US1] TigerBeetle mapping verified: 3 Transfer fields match
      `data-model.md §1.3`; assert gross = net + fee before batch submission; 3300=0 post-batch

---

## Phase 4: US2 — Compensation (T014–T016)

- [ ] T014 [US2] `spec/processes.md §13.5` — add compensation subsection: step 2 fails after
      step 1 → ADJUSTMENT_CREDIT `{businessRef}:comp` restores sender gross;
      receiver never touched; coa_trans never created; return error to member
- [ ] T015 [US2] `design-v2/orchestration.md §15` — add compensation path to steps table;
      distinguish from forward-retry path; clarify no TB reversal on compensation
- [ ] T016 [P] [US2] Gherkin in `design-v2/acceptance.md`:
      - TC-TFR-04: step 2 (journal) fails after step 1 → ADJUSTMENT_CREDIT issued,
        sender available restored, no coa_trans row, 3300 unchanged, return 500
      - TC-TFR-05: ADJUSTMENT_CREDIT duplicate (compensation retried) → idempotent,
        no double-credit (unique constraint on `{businessRef}:comp`)

---

## Phase 5: US3 — Forward retry (T017–T018)

- [ ] T017 [US3] `spec/processes.md §13.5` — add forward-retry subsection: step 3 fails after
      step 2 is POSTED → retry TRANSFER_CREDIT idempotently; do NOT reverse TB journal
      (ADR-001); idempotent via `(wallet_id, businessRef, TRANSFER_CREDIT)` constraint;
      explain: TB is POSTED means ledger is balanced, receiver must be credited
- [ ] T018 [P] [US3] Gherkin in `design-v2/acceptance.md`:
      - TC-TFR-06: step 3 (receiver credit) fails after POSTED → retry TRANSFER_CREDIT
        succeeds on second attempt; coa_trans remains POSTED; 3300=0; eventual 200

---

## Phase 6: Polish & Cross-Cutting (T019–T022)

- [ ] T019 [P] Idempotency Gherkin in `design-v2/acceptance.md`:
      - TC-TFR-07: duplicate POST /transfers same businessRef while first in-flight →
        second returns 200 with same body, no double-debit
      - TC-TFR-08: orchestration crashes between step 2 and step 3; on retry:
        TRANSFER_DEBIT idempotent (already exists), createJournal idempotent (existing
        coa_trans returned), TRANSFER_CREDIT idempotent (already exists or applied) → 200
- [ ] T020 [P] SQL invariant CI note: `account[3300].balance = 0` after every POSTED
      transfer journal — add to `design-v2/acceptance.md` Part IV INV section;
      map to ADR-010 and ADR-031 invariant framework
- [ ] T021 Add "Protocol Decision" note in `spec/processes.md §13.5` or §13.5.1:
      why fully synchronous — both parties internal → no external gate; 3 sync hops
      ≈ 105-225 ms; SC-001 SLA 1 second — margin sufficient; no async complexity needed
- [ ] T022 [P] Sender ≠ receiver rule: confirm guard at orchestration layer before any
      wallet/accounting call; HTTP 422 code `TRANSFER_SAME_MEMBER`; document in
      `spec/processes.md §13.5` and `design-v2/acceptance.md` (add TC-TFR-09)

---

## Dependencies & Execution Order

```
Phase 1 → Phase 2 → Phase 3 → Phase 4 → Phase 5 → Phase 6
(no cross-phase blockers within each phase)
```

Phase 3 T012 Gherkin can run in parallel with T010/T011 spec updates.
Phase 6 is independent polish — can start any time after Phase 3.
