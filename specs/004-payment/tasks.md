# 004-payment — Tasks

## Phase 1: Setup

### T001 — Create spec artifacts
- Create `specs/004-payment/` directory with plan.md, research.md, data-model.md, tasks.md, quickstart.md
- Confirm UC-3 entry in the architecture overview references this spec

### T002 — Draw sequence diagram
- Add PlantUML or Mermaid sequence diagram to plan.md covering the 3-step happy path, the compensation path (step 3 fail), and the forward-retry path (step 4 fail)
- Diagram must label each HTTP call, account codes, and the assert(3500.balance=0) step

---

## Phase 2: Foundational

### T003 — Entry map
- Document `POST /payments` in `app-orchestration` route table
- Confirm JWT auth (S1 inbound) applies
- Record request/response schema references pointing to data-model.md

### T004 — Request schema
- Define `PaymentRequest` schema: `businessRef`, `userWalletId`, `merchantWalletId`, `grossAmount`, `currency`
- Add validation rules: `grossAmount > 0`, both wallet IDs required, `businessRef` non-empty

### T005 — Wallet TX schemas
- Define internal DTOs for `PAYMENT_DEBIT`, `PAYMENT_CREDIT`, `ADJUSTMENT_CREDIT`
- Ensure `direction` field is set correctly for each type (DEBIT / CREDIT)

### T006 — app-wallet endpoint wiring
- Confirm `app-wallet` exposes a generic wallet-transaction endpoint (or extend existing one)
- Endpoint must accept `txType` as a discriminator and apply the correct balance mutation
- `PAYMENT_DEBIT`: `available -= amount` (reject if insufficient funds)
- `PAYMENT_CREDIT`: `available += amount`
- `ADJUSTMENT_CREDIT`: `available += amount`

### T007 — app-accounting journal type
- Register `PAYMENT` journal type in `app-accounting`
- Map to two-transfer TB batch (see data-model.md TigerBeetle section)
- Confirm `status = POSTED` (no pending flag) is the only accepted value for this type

### T008 — wallet_tx unique constraint migration
- Write DB migration adding `UNIQUE (wallet_id, business_ref, tx_type)` if not already present
- Confirm migration is backward-compatible with existing TX types (DEPOSIT, WITHDRAW, etc.)

### T009 — TigerBeetle transfer ID hashing
- Implement `hash(businessRef + ":2110")` and `hash(businessRef + ":2120")` utility
- Use deterministic hash (SHA-256 truncated to 128 bits or UUID v5)
- Add unit test: same input always produces same output; different suffixes produce different IDs

---

## Phase 3: US1 — Payment Happy Path

**User story:** As a USER, I can pay a MERCHANT from my wallet so that the transaction settles immediately.

### T010 — Update spec with US1 acceptance criteria
- Add US1 to plan.md with acceptance criteria:
  - USER available balance decreases by gross
  - MERCHANT available balance increases by net (= gross in v1)
  - TigerBeetle batch posts with balance(3500) = 0
  - Response is 200 with settled payment record

### T011 — Gherkin: TC-PAY-01 (happy path full flow)
```gherkin
Feature: Wallet Payment

  Scenario: TC-PAY-01 — Successful payment end-to-end
    Given USER wallet has available balance 500.00 VND
    And MERCHANT wallet has available balance 100.00 VND
    When POST /payments with businessRef "pay_001" grossAmount 200.00
    Then USER wallet available balance is 300.00 VND
    And MERCHANT wallet available balance is 300.00 VND
    And TigerBeetle account 3500 balance is 0
    And response status is 200
```

### T012 — Gherkin: TC-PAY-02 (insufficient funds)
```gherkin
  Scenario: TC-PAY-02 — Payment rejected — insufficient funds
    Given USER wallet has available balance 50.00 VND
    When POST /payments with businessRef "pay_002" grossAmount 200.00
    Then response status is 422
    And USER wallet available balance is 50.00 VND
    And no wallet_tx rows exist for businessRef "pay_002"
    And no TigerBeetle transfers exist for businessRef "pay_002"
```

### T013 — TigerBeetle batch mapping
- Implement the two-transfer batch in `app-accounting`
- Assert `account[3500].balance == 0` after batch commit
- Treat non-zero transit as a fatal error (do not return 200)

---

## Phase 4: US2 — Compensation on Ledger Fail

**User story:** As the platform, if the ledger write fails after USER debit, the USER balance must be fully restored.

### T014 — Update spec with US2 acceptance criteria
- Add US2 to plan.md:
  - USER available is restored to pre-payment amount
  - No MERCHANT credit occurs
  - No TigerBeetle entry exists for the payment
  - Compensation row exists in wallet_tx with tx_type=ADJUSTMENT_CREDIT

### T015 — Gherkin: TC-PAY-04 (ledger failure, compensation triggered)
```gherkin
  Scenario: TC-PAY-04 — Ledger failure triggers compensation
    Given USER wallet has available balance 500.00 VND
    And app-accounting will return 500 for createJournal
    When POST /payments with businessRef "pay_004" grossAmount 200.00
    Then USER wallet available balance is 500.00 VND
    And wallet_tx for businessRef "pay_004:comp" type ADJUSTMENT_CREDIT exists
    And no PAYMENT_CREDIT wallet_tx exists for businessRef "pay_004"
    And response status is 500
```

### T016 — Gherkin: TC-PAY-05 (compensation itself is idempotent)
```gherkin
  Scenario: TC-PAY-05 — Compensation is idempotent on retry
    Given a compensation ADJUSTMENT_CREDIT already exists for "pay_004:comp"
    When orchestration retries the compensation
    Then only one ADJUSTMENT_CREDIT row exists for "pay_004:comp"
    And USER wallet available balance has not changed
```

---

## Phase 5: US3 — Forward Retry on Merchant Credit Fail

**User story:** As the platform, if MERCHANT credit fails after the ledger has POSTED, the credit must be retried until it succeeds without reversing the ledger.

### T017 — Update spec with US3 acceptance criteria
- Add US3 to plan.md:
  - Ledger entry remains POSTED (not reversed)
  - PAYMENT_CREDIT is retried until committed
  - Final state: MERCHANT balance increased, USER balance decreased, 200 returned

### T018 — Gherkin: TC-PAY-06 (forward retry on merchant credit fail)
```gherkin
  Scenario: TC-PAY-06 — Forward retry after PAYMENT_CREDIT failure
    Given USER wallet has available balance 500.00 VND
    And MERCHANT wallet has available balance 100.00 VND
    And PAYMENT_DEBIT and createJournal succeed for businessRef "pay_006"
    And the first PAYMENT_CREDIT call returns 503
    When orchestration retries PAYMENT_CREDIT for "pay_006"
    Then MERCHANT wallet available balance is 300.00 VND
    And only one PAYMENT_CREDIT wallet_tx row exists for "pay_006"
    And TigerBeetle transfers for "pay_006" remain POSTED unchanged
    And response status is 200
```

---

## Phase 6: Polish

### T019 — Gherkin: TC-PAY-07 (idempotent duplicate payment)
```gherkin
  Scenario: TC-PAY-07 — Duplicate POST /payments with same businessRef is idempotent
    Given a payment with businessRef "pay_007" has already completed
    When POST /payments is called again with businessRef "pay_007" same params
    Then response status is 200
    And USER wallet available balance has not changed from the first call
    And MERCHANT wallet available balance has not changed from the first call
    And exactly one PAYMENT_DEBIT row exists for "pay_007"
    And exactly one PAYMENT_CREDIT row exists for "pay_007"
```

### T020 — Gherkin: TC-PAY-08 (idempotent duplicate with different amount rejected)
```gherkin
  Scenario: TC-PAY-08 — Duplicate businessRef with different amount is rejected
    Given a payment with businessRef "pay_008" grossAmount 100.00 has completed
    When POST /payments is called with businessRef "pay_008" grossAmount 200.00
    Then response status is 409
    And no additional wallet_tx rows are created
```

### T021 — INV-P note: transit balance invariant
- Document as an invariant: after every successful `createJournal(PAYMENT, POSTED)`, `account[3500].balance == 0`
- Add monitoring alert: if `account[3500].balance != 0` after batch commit, page on-call immediately
- Label: `INV-P` in architecture overview

### T022 — Protocol decision record
- Confirm all three inter-service calls use synchronous HTTP (not RabbitMQ, not gRPC) per ADR-027
- Record in plan.md that this decision is final for v1 and requires an ADR amendment to change
- Add note: if any service migrates to async, ADR-027 must be revisited before that service can participate in the payment flow

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

---

## Dependencies

```
T001 → T002
T003, T004, T005 → T006, T007
T006, T007, T008, T009 → T010
T010 → T011, T012, T013
T013 → T014
T014 → T015, T016
T015, T016 → T017
T017 → T018
T018 → T019, T020, T021, T022
```
