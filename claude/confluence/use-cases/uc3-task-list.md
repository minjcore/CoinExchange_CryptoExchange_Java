# UC-3 Task List — Wallet Payment

> **CF page ID:** 51872220 | **Parent:** UC-3 Wallet Payment (51609624)
> **Source of truth:** this file → push to CF
> **See also:** `specs/004-payment/tasks.md`, `specs/004-payment/data-model.md`

---

## Service Entry Point Map

| Who sends | Protocol | Module receives | Queue / endpoint | Domain logic |
|-----------|----------|-----------------|------------------|--------------|
| Member | HTTP POST /payments | app-orchestration | gtelpay-public.yaml | Validate + PAYMENT_DEBIT + createJournal POSTED + PAYMENT_CREDIT + 200 |
| Orchestration step 1 (sync) | HTTP | app-wallet | wallet-internal.yaml | PAYMENT_DEBIT (USER) |
| Orchestration step 2 (sync) | HTTP | app-accounting | accounting-internal.yaml | createJournal(PAYMENT, POSTED) — TB 2-transfer batch |
| Orchestration step 3 (sync) | HTTP | app-wallet | wallet-internal.yaml | PAYMENT_CREDIT (MERCHANT) |
| Orchestration compensation (sync) | HTTP | app-wallet | wallet-internal.yaml | ADJUSTMENT_CREDIT (USER — only if step 2 fails) |

> Wallet Payment is **fully synchronous**. No RabbitMQ, no outbox. All steps complete before 200.

---

## Phase 1: Setup

- [ ] T001 — Create spec artifacts: plan.md, research.md, data-model.md, tasks.md, quickstart.md in `specs/004-payment/`
- [ ] T002 — Draw sequence diagram: 3-step happy path, compensation path (step 2 fail), forward-retry path (step 3 fail); label all account codes + assert(3500.balance=0)

---

## Phase 2: Foundational

- [ ] T003 — Document POST /payments in app-orchestration route table; confirm JWT auth (S1 inbound)
- [ ] T004 — Define PaymentRequest schema: businessRef, userWalletId, merchantWalletId, grossAmount, currency
- [ ] T005 — Define wallet TX DTOs: PAYMENT_DEBIT, PAYMENT_CREDIT, ADJUSTMENT_CREDIT with correct direction fields
- [ ] T006 — Confirm app-wallet endpoint supports txType discriminator: PAYMENT_DEBIT (reject if insufficient), PAYMENT_CREDIT, ADJUSTMENT_CREDIT
- [ ] T007 — Register PAYMENT journal type in app-accounting; map to 2-transfer TB batch; status=POSTED only (no PENDING)
- [ ] T008 — DB migration: UNIQUE (wallet_id, business_ref, tx_type); confirm backward-compatible with existing TX types
- [ ] T009 — Implement hash(businessRef+":2110") and hash(businessRef+":2120") deterministic ID utility; add unit test

---

## Phase 3: US1 — Payment Happy Path

- [ ] T010 — Update plan.md with US1 acceptance criteria: USER available −gross, MERCHANT available +net, TB account[3500]=0, 200 response
- [ ] T011 — Gherkin TC-PAY-01: happy path end-to-end (USER 500 → pay 200 → USER=300, MERCHANT+200, 3500=0)
- [ ] T012 — Gherkin TC-PAY-02: insufficient funds → 422, no wallet_tx, no TB transfers
- [ ] T013 — Implement 2-transfer TB batch in app-accounting; assert account[3500].balance==0 after commit; non-zero = fatal (no 200)

---

## Phase 4: US2 — Compensation on Ledger Fail

- [ ] T014 — Update plan.md with US2: USER restored to pre-payment amount, no MERCHANT credit, no TB entry, ADJUSTMENT_CREDIT row exists
- [ ] T015 — Gherkin TC-PAY-04: ledger failure → ADJUSTMENT_CREDIT issued, USER restored, no PAYMENT_CREDIT, 500 response
- [ ] T016 — Gherkin TC-PAY-05: compensation is idempotent on retry — only one ADJUSTMENT_CREDIT row

---

## Phase 5: US3 — Forward Retry on Merchant Credit Fail

- [ ] T017 — Update plan.md with US3: TB POSTED unchanged, PAYMENT_CREDIT retried until committed, final 200
- [ ] T018 — Gherkin TC-PAY-06: PAYMENT_CREDIT fails → retry succeeds; only one PAYMENT_CREDIT row; TB unchanged

---

## Phase 6: Polish

- [ ] T019 — Gherkin TC-PAY-07: duplicate POST /payments same businessRef → idempotent 200; balances unchanged
- [ ] T020 — Gherkin TC-PAY-08: duplicate businessRef with different amount → 409, no additional rows
- [ ] T021 — INV-P invariant: account[3500].balance==0 after every createJournal(PAYMENT, POSTED); monitoring alert for non-zero
- [ ] T022 — Protocol decision record: all 3 calls sync HTTP per ADR-027; async migration requires ADR-027 amendment

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
