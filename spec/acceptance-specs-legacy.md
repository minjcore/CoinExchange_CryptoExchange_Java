# core.acceptance-specs — Deterministic behavior spec (Given/When/Then)

**Last updated:** 2026-06-08  
**Status:** Draft  
**Scope:** `10_core/` — language-agnostic acceptance/conformance scenarios for every flow. Two implementations (e.g. Java and Go) MUST produce identical observable behavior against these scenarios.

> **How to read:** Assertions reference only **observable state** — journal `status`, ledger account deltas, transit = 0, wallet `available`/`frozen`, `wallet_tx` rows, idempotency outcome, emitted events. No classes, SQL, or framework. Amounts follow the canonical examples in [`core.foundation.md`](./core.foundation.md) §8–16.  
> **Source of truth:** postings → [`core.foundation.md`](./core.foundation.md) §8–16 · flows & failures → [`core.business-processes.md`](./core.business-processes.md) §3–§15 · keys → [ADR-005](./adr/ADR-005-idempotency-key-strategy.md) · `tx_type` → [`IMPLEMENTATION.md`](./IMPLEMENTATION.md) §2.1.  
> **Conventions:** currency VND; amounts scale 4; `businessRef` is the idempotency key; "ledger delta" = net change to the COA account after the flow.

---

## 1. Deposit (2-phase, transit 3100)

```gherkin
Feature: Deposit

Background:
  Given account 1111, 2110, 4110, 3100 exist
  And user 100234 has wallet USER/VND with available=0, frozen=0

Scenario: Happy path — 100,000 with fee 1,000
  Given a bank deposit businessRef="dep-1", amount=100000 for user 100234
  When orchestration runs deposit with fee=1000
  Then journal "dep-1" (use_case=DEPOSIT) status=POSTED
  And ledger delta: 1111 +100000, 2110 +99000, 4110 +1000
  And transit 3100 = 0
  And wallet USER available=99000, frozen=0
  And a wallet_tx (wallet_USER, "dep-1", DEPOSIT_CREDIT) amount=99000 exists
  And event JournalPosted(useCase=DEPOSIT, businessRef="dep-1") is emitted

Scenario: Phase A only — funds in transit, not yet credited
  Given a bank deposit businessRef="dep-2", amount=100000
  When only phase A (PENDING) has run
  Then journal "dep-2" status=PENDING
  And ledger delta: 1111 +100000, 3100 +100000
  And wallet USER available=0
  And no DEPOSIT_CREDIT wallet_tx exists for "dep-2"

Scenario: Duplicate webhook — no double credit
  Given deposit "dep-1" is POSTED and wallet credited 99000
  When the same webhook businessRef="dep-1" arrives again
  Then no new journal is created
  And wallet USER available stays 99000
  And the response has idempotentReplay=true

Scenario: Wallet credit retried after POSTED
  Given journal "dep-1" is POSTED but the wallet credit previously errored
  When the credit is retried with businessRef="dep-1"
  Then the ledger is unchanged
  And wallet USER available=99000 (single effect)

Scenario: VA cannot be mapped to a user
  Given a webhook businessRef="dep-x" with an unknown vaNumber
  Then journal "dep-x" stays PENDING with 3100 holding the funds
  And no wallet credit occurs
  And an ops alert / command-failed signal is raised

Scenario: Bank reverses the deposit after POSTED
  Given deposit "dep-1" is POSTED and wallet credited 99000
  When the bank sends a reversal for "dep-1"
  Then a new reversing journal is created (original is not edited)
  And a compensating wallet debit returns USER available to 0
  And net ledger effect for 1111/2110/4110 is 0
```

---

## 2. Withdraw (freeze + async payout, transit 3200)

```gherkin
Feature: Withdraw

Background:
  Given user 100234 has wallet USER/VND available=200000, frozen=0

Scenario: Happy path — withdraw 100,000 fee 1,000
  Given a withdrawal businessRef="wd-1", amount=100000, fee=1000
  When orchestration accepts the withdrawal
  Then wallet USER available=99000, frozen=101000
  And a wallet_tx (wallet_USER, "wd-1", WITHDRAW_FREEZE) amount=101000 exists
  And the API responds ACCEPTED (200/202)
  When the bank payout for "wd-1" succeeds
  Then wallet USER available=99000, frozen=0
  And a wallet_tx (wallet_USER, "wd-1:settle", WITHDRAW_SETTLE) amount=101000 exists
  And ledger delta: 2110 -101000, 1111 -100000, 4120 +1000
  And transit 3200 = 0

Scenario: Insufficient balance — rejected before any hold
  Given a withdrawal businessRef="wd-2", amount=300000, fee=1000
  Then the request is rejected WALLET_INSUFFICIENT_BALANCE (422)
  And wallet USER available=200000, frozen=0 (unchanged)

Scenario: Payout fails terminally — release the hold
  Given withdrawal "wd-1" is frozen (available=99000, frozen=101000)
  When the bank payout for "wd-1" fails terminally
  Then a wallet_tx (wallet_USER, "wd-1:release", WITHDRAW_RELEASE) exists
  And wallet USER available=200000, frozen=0
  And no ledger debit of 2110 remains

Scenario: Payout result unknown (timeout) — MUST NOT release
  Given withdrawal "wd-1" is frozen and the bank result is unknown
  When the payout times out without a terminal result
  Then the hold remains (frozen=101000) and no release happens
  And the system queries the bank until a terminal success/failure is known

Scenario: Duplicate accept — single freeze
  Given withdrawal "wd-1" is already frozen
  When the same businessRef="wd-1" accept is retried
  Then no second freeze occurs (idempotentReplay=true)
  And frozen stays 101000
```

---

## 3. Wallet payment (sync, transit 3500)

```gherkin
Feature: Wallet payment

Background:
  Given user 100234 has wallet USER/VND available=100000
  And merchant 555 has wallet MERCHANT/VND available=0

Scenario: Happy path — pay 100,000
  Given a payment businessRef="pay-1", from user 100234 to merchant 555, amount=100000
  When orchestration runs payment
  Then ledger delta: 2110 -100000, 2120 +100000
  And transit 3500 = 0
  And wallet USER available=0
  And wallet MERCHANT available=100000
  And wallet_tx (wallet_USER, "pay-1", PAYMENT_DEBIT)=100000 and (wallet_MERCHANT, "pay-1", PAYMENT_CREDIT)=100000 exist
  And journal "pay-1" status=POSTED

Scenario: Insufficient balance — no debit, no journal
  Given a payment businessRef="pay-2", amount=150000
  Then the request is rejected WALLET_INSUFFICIENT_BALANCE (422)
  And wallet USER available=100000 (unchanged)
  And no journal "pay-2" exists

Scenario: Journal post fails after user debit — compensate
  Given payment "pay-1" debited USER but the journal post failed
  When recovery runs
  Then a compensating credit "pay-1:comp" restores USER available=100000
  And merchant is NOT credited

Scenario: Merchant credit fails after POSTED — forward retry
  Given payment "pay-1" has USER debited and journal POSTED, merchant not credited
  When the merchant credit is retried with businessRef="pay-1"
  Then wallet MERCHANT available=100000 (single effect)
  And the ledger is unchanged

Scenario: Duplicate with same amount — replay
  Given payment "pay-1" completed
  When the same businessRef="pay-1" amount=100000 is sent again
  Then idempotentReplay=true and balances are unchanged

Scenario: Same businessRef, different amount — conflict
  Given payment "pay-1" completed at amount=100000
  When businessRef="pay-1" is reused with amount=50000
  Then the request is rejected WALLET_DUPLICATE_CONFLICT (409)
```

---

## 4. Internal transfer (sync, transit 3300)

```gherkin
Feature: Internal transfer

Background:
  Given user A=100234 wallet USER/VND available=200000
  And user B=100777 wallet USER/VND available=0

Scenario: Happy path — transfer 100,000 fee 1,000
  Given a transfer businessRef="tr-1", from A to B, amount=100000, fee=1000
  When orchestration runs transfer
  Then ledger delta: A 2110 -101000, B 2110 +100000, 4130 +1000
  And transit 3300 = 0
  And wallet A available=99000, wallet B available=100000
  And wallet_tx (wallet_A, "tr-1", TRANSFER_DEBIT)=101000 and (wallet_B, "tr-1", TRANSFER_CREDIT)=100000 exist

Scenario: Journal post fails after debit A — compensate
  Given transfer "tr-1" debited A but the post failed
  Then a compensating credit "tr-1:comp" restores A available=200000
  And B is not credited

Scenario: Insufficient balance — rejected
  Given a transfer businessRef="tr-2", amount=250000, fee=1000
  Then rejected WALLET_INSUFFICIENT_BALANCE (422)
  And both wallets unchanged
```

---

## 5. IBFT (freeze + async bank, transit 3400)

```gherkin
Feature: Interbank transfer (IBFT)

Background:
  Given user 100234 wallet USER/VND available=200000

Scenario: Happy path — IBFT 100,000 fee 1,000, Napas cost 500
  Given an IBFT businessRef="ibft-1", amount=100000, fee=1000
  When orchestration accepts it
  Then wallet USER available=99000, frozen=101000
  And a wallet_tx (wallet_USER, "ibft-1", IBFT_FREEZE)=101000 exists
  When the Napas/bank leg succeeds
  Then wallet USER available=99000, frozen=0
  And a wallet_tx (wallet_USER, "ibft-1:settle", IBFT_SETTLE)=101000 exists
  And ledger delta: 2110 -101000, 1112 +100500, 4130 +1000, 5100 +500
  And transit 3400 = 0
  And net profit = 500 (fee 1000 - cost 500)

Scenario: Bank leg fails terminally — release
  Given IBFT "ibft-1" is frozen
  When the bank leg fails terminally
  Then wallet_tx (wallet_USER, "ibft-1:release", IBFT_RELEASE) exists
  And wallet USER available=200000, frozen=0

Scenario: Bank result unknown — MUST NOT release
  Given IBFT "ibft-1" is frozen and the bank result is unknown
  Then the hold remains until a terminal bank result is known
```

---

## 6. QR/POS payment (acquirer 1113, settle at EOD)

```gherkin
Feature: QR/POS payment

Background:
  Given merchant 555 has wallet MERCHANT/VND available=0

Scenario: Happy path — 100,000, acquiring fee 500
  Given a QR/POS capture businessRef="qr-1", amount=100000 for merchant 555
  When the acquirer flow posts
  Then ledger delta: 1113 +99500, 2120 +100000
  And transit 3500 = 0
  And the merchant balance 2120 is pending settlement
  And NO per-transaction wallet_tx is created in v1

Scenario: MDR is recognized at EOD, not per transaction
  Given QR/POS "qr-1" posted
  Then no 4140 (MDR) revenue is recognized at capture time
  And MDR is recognized only during EOD settlement (see §9)
```

---

## 7. Payroll (batch, transit 3600)

```gherkin
Feature: Payroll

Background:
  Given merchant 555 wallet MERCHANT/VND available=600000

Scenario: Happy path — 5 employees x 100,000, fee 5,000, Napas cost 2,500
  Given a payroll businessRef="pr-1" with 5 recipients of 100000 and fee=5000
  When orchestration runs the batch
  Then wallet MERCHANT available=95000
  And a wallet_tx (wallet_MERCHANT, "pr-1", PAYROLL_DEBIT)=505000 exists
  And ledger delta: 2120 -505000, 1112 +502500, 4150 +5000, 5100 +2500
  And transit 3600 = 0
  And net profit = 2500

Scenario: One recipient fails — partial batch, others succeed
  Given payroll "pr-1" where recipient 3 bank leg fails terminally
  Then recipients 1,2,4,5 settle
  And recipient 3 amount is released back to merchant (held by sub-key "pr-1:3")
  And a batch summary reports succeeded=4, failed=1

Scenario: Re-run the same batch — idempotent
  Given payroll "pr-1" completed
  When businessRef="pr-1" is submitted again
  Then no recipient is paid twice
```

---

## 8. Disbursement (partner escrow, transit 3700)

```gherkin
Feature: Disbursement

Background:
  Given partner 900 has escrow wallet PARTNER/VND available=0

Scenario: Pre-fund the partner escrow — 100,000
  Given a pre-fund businessRef="pf-1", amount=100000 for partner 900
  When the pre-fund posts
  Then ledger delta: 1111 +100000, 2130 +100000
  And wallet PARTNER available=100000
  And a wallet_tx (wallet_PARTNER, "pf-1", PARTNER_PREFUND_CREDIT)=100000 exists

Scenario: Disburse 100,000 fee 1,000
  Given partner 900 escrow available=100000... (pre-funded for the gross)
  And a disbursement businessRef="ds-1", amount=100000, fee=1000
  When the batch runs and the bank leg succeeds
  Then a wallet_tx (wallet_PARTNER, "ds-1", DISBURSEMENT_DEBIT)=101000 exists
  And ledger delta: 2130 -101000, 1112 +100000 plus fee 4150 / cost 5100
  And transit 3700 = 0

Scenario: Bank leg unknown — MUST NOT release
  Given disbursement "ds-1" is in flight and the bank result is unknown
  Then the held amount is not released until a terminal result is known
```

---

## 9. EOD settlement & clearing (batch, transit 3800/3810/3820)

```gherkin
Feature: EOD settlement

Scenario: Happy path — settle merchant 555 for 200,000, MDR 2,000
  Given merchant 555 has 2120 pending=200000 for settlement date D
  When the EOD batch runs for (merchant 555, date D)
  Then 2120 -200000
  And MDR: 3820 holds 2000 and 4140 +2000 recognized
  And net outbound 198000 flows 3810 -> 1112 (incl. Napas fee 5100)
  And all transit (3800/3810/3820) = 0
  And 2120 pending for date D = 0

Scenario: Re-run same (merchant, date) — idempotent
  Given EOD for (merchant 555, date D) already completed
  When the batch is re-run for (merchant 555, date D)
  Then no second settlement occurs

Scenario: Outbound bank transfer fails — hold and retry
  Given EOD computed net 198000 in 3810 for merchant 555
  When the outbound bank transfer fails
  Then the amount remains in 3810 (not lost)
  And it is retried on the next cycle (never double-settled)
```

---

## 10. Cross-cutting invariants (apply to every scenario)

```gherkin
Scenario Outline: Global invariants hold after any completed flow
  Then sum(DR) = sum(CR) for every journal
  And every transit account used by the flow = 0 on completion
  And (1111+1112+1113) = (2110+2120+2130) within reconciliation tolerance
  And replaying the same businessRef yields the same result with no extra effect
  And a businessRef reused with a conflicting amount is rejected (409)
  And no finalized wallet_tx or journal line is ever edited (reversal only)
```

---

## 11. References

| Topic | Document |
|-------|----------|
| DR/CR postings per use case | [`core.foundation.md`](./core.foundation.md) §8–16 |
| Flow narrative + failure/compensation design | [`core.business-processes.md`](./core.business-processes.md) §3–§15 |
| `tx_type` enum | [`IMPLEMENTATION.md`](./IMPLEMENTATION.md) §2.1 |
| Idempotency keys & sub-keys | [ADR-005](./adr/ADR-005-idempotency-key-strategy.md) |
| Wire payloads | `openapi/`, `asyncapi/` |
