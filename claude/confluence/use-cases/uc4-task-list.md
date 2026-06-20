# UC-4 Task List — Internal Transfer

> **CF page ID:** 52199450 | **Parent:** UC-4 Internal Transfer (51544073)
> **Source of truth:** this file → push to CF
> **See also:** `specs/005-internal-transfer/tasks.md`, `specs/005-internal-transfer/data-model.md`

---

## Service Entry Point Map

| Who sends | Protocol | Module receives | Queue / endpoint | Domain logic |
|-----------|----------|-----------------|------------------|--------------|
| Member | HTTP POST /transfers | app-orchestration | gtelpay-public.yaml | Validate sender≠receiver + fee + TRANSFER_DEBIT + createJournal POSTED + TRANSFER_CREDIT + 200 |
| Orchestration step 1 (sync) | HTTP | app-wallet | wallet-internal.yaml | TRANSFER_DEBIT (sender) |
| Orchestration step 2 (sync) | HTTP | app-accounting | accounting-internal.yaml | createJournal(TRANSFER, POSTED) — TB 3-transfer batch |
| Orchestration step 3 (sync) | HTTP | app-wallet | wallet-internal.yaml | TRANSFER_CREDIT (receiver) |
| Orchestration compensation (sync) | HTTP | app-wallet | wallet-internal.yaml | ADJUSTMENT_CREDIT (sender — only if step 2 fails) |

> Internal transfer is **fully synchronous**. No RabbitMQ, no outbox, no async worker. All steps complete before 200.

---

## Phase 1: Setup

- [ ] T001 — Verify spec-kit artifacts: plan.md, research.md, data-model.md, quickstart.md, tasks.md in `specs/005-internal-transfer/`
- [ ] T002 — Draw data-flow diagram: sync 3-step sequence, compensation path, forward-retry path

---

## Phase 2: Foundational

- [ ] T003 — Service Entry Point Map confirmed in data-model.md §0 — fully sync HTTP; no RabbitMQ; no outbox
- [ ] T004 — [P] Verify POST /transfers in gtelpay-public.yaml with X-Idempotency-Key header; responses 200/422/409
- [ ] T005 — [P] Verify wallet-internal.yaml has TRANSFER_DEBIT, TRANSFER_CREDIT, ADJUSTMENT_CREDIT; document in data-model.md §0
- [ ] T006 — [P] Verify accounting-internal.yaml has createJournal supporting use_case=TRANSFER and status=POSTED (no PENDING state)
- [ ] T007 — [P] Verify transit account 3300 and fee revenue account 4130 exist in COA table and TB account seeding
- [ ] T008 — Confirm coa_trans unique constraint (reference_id, use_case) covers use_case=TRANSFER
- [ ] T009 — [P] Verify wallet_tx unique constraint (wallet_id, business_ref, tx_type) covers TRANSFER_DEBIT, TRANSFER_CREDIT, ADJUSTMENT_CREDIT

---

## Phase 3: US1 — Transfer Happy Path

- [ ] T010 — [US1] spec/processes.md §13.5: 3-step sequence narrative; TB Transfer IDs hash(ref+":debit"), hash(ref+":credit"), hash(ref+":4130"); assert account[3300].balance=0; return 200
- [ ] T011 — [US1] design-v2/orchestration.md §15: steps table with TB mapping, idempotency keys, sender≠receiver guard, fee treatment note
- [ ] T012 — [P] [US1] Gherkin: TC-TFR-01 (happy path), TC-TFR-02 (insufficient balance → 422), TC-TFR-03 (duplicate businessRef → idempotent 200)
- [ ] T013 — [P] [US1] TigerBeetle mapping verified: 3 Transfer fields match data-model.md §1.3; gross=net+fee before batch; 3300=0 post-batch

---

## Phase 4: US2 — Compensation

- [ ] T014 — [US2] spec/processes.md §13.5: compensation subsection — step 2 fails after step 1 → ADJUSTMENT_CREDIT {ref}:comp restores sender gross; no TB reversal
- [ ] T015 — [US2] design-v2/orchestration.md §15: add compensation path; distinguish from forward-retry
- [ ] T016 — [P] [US2] Gherkin: TC-TFR-04 (step 2 fails → ADJUSTMENT_CREDIT, 500), TC-TFR-05 (compensation retried → idempotent)

---

## Phase 5: US3 — Forward Retry

- [ ] T017 — [US3] spec/processes.md §13.5: forward-retry subsection — step 3 fails after POSTED → retry TRANSFER_CREDIT idempotently; do NOT reverse TB journal (ADR-001)
- [ ] T018 — [P] [US3] Gherkin TC-TFR-06: step 3 fails → retry succeeds; coa_trans remains POSTED; 3300=0; eventual 200

---

## Phase 6: Polish & Cross-Cutting

- [ ] T019 — [P] Idempotency Gherkin: TC-TFR-07 (in-flight duplicate → 200), TC-TFR-08 (crash between step 2 and step 3 → retry → 200)
- [ ] T020 — [P] SQL invariant CI: account[3300].balance=0 after every POSTED transfer — add to acceptance.md Part IV; map to ADR-010 and ADR-031
- [ ] T021 — Protocol Decision note: why fully synchronous — both parties internal; 3 hops ≈ 105-225ms; SC-001 SLA 1s — margin sufficient
- [ ] T022 — [P] Sender ≠ receiver guard: HTTP 422 TRANSFER_SAME_MEMBER; document in spec/processes.md §13.5 and acceptance.md (TC-TFR-09)

---

## Task Summary

| Phase | Tasks | Count |
|-------|-------|-------|
| 1 — Setup | T001–T002 | 2 |
| 2 — Foundational | T003–T009 | 7 |
| 3 — US1 Happy Path | T010–T013 | 4 |
| 4 — US2 Compensation | T014–T016 | 3 |
| 5 — US3 Forward Retry | T017–T018 | 2 |
| 6 — Polish | T019–T022 | 4 |
| **Total** | | **22** |
