# Acceptance specs (Given / When / Then)

Language-agnostic conformance. **Ledger assertions** reference [`../spec/foundation.md`](../spec/foundation.md) §8–16; **wallet assertions** reference [`wallet.md`](./wallet.md). Amounts use foundation examples unless noted.

**ADR AC/TC:** Each ADR defines acceptance criteria (AC) and test cases (TC) in [`adr/`](../adr/README.md). Gherkin scenarios below implement TC rows — see **Part IV** index at end of file.

## Conformance tiers

| Tier | Scenarios | Parts | Release gate |
|------|-----------|-------|--------------|
| **v1** | **150** | Part I (75) + Part II (60) + ADR-031/032/033 (15) | **Required** |
| Full | 207 | + Part III remainder (ADR-034/035/036, ref gaps) | Stretch |

**Đọc theo flow:** Part I (core per use case) → Part II (extended matrix) → Part III (ADR ref gaps).

---

## Feature: Deposit (2-phase, transit 3100)

### Scenario: Happy — 100,000 gross, 1,000 fee

```gherkin
Given user 100234 has USER/VND wallet available=0
And bank webhook businessRef="dep-1" amount=100000
When orchestration processes deposit with fee=1000
Then journal "dep-1" use_case=DEPOSIT status=POSTED
And ledger: 1111 +100000, 2110 +99000, 4110 +1000, transit 3100 = 0
And wallet USER available=99000
And wallet_tx (wallet, "dep-1", DEPOSIT_CREDIT) amount=99000 exists
```

### Scenario: Phase A only — PENDING holds transit

```gherkin
Given webhook "dep-1a" amount=50000 mapped to member 100234
When phase A completes
Then coa_trans status=PENDING use_case=DEPOSIT
And transit 3100 CR 50000
And 1111 unchanged until phase B
And wallet available unchanged
```

### Scenario: Phase B confirm — POSTED clears transit

```gherkin
Given PENDING "dep-1b" with 3100 CR 50000
When bank confirm amount=50000 fee=0
Then status=POSTED
And 1111 DR 50000, 2110 CR 50000, 3100 net zero
And wallet credit command amount=50000
```

### Scenario: Zero fee deposit

```gherkin
Given gross=75000 fee=0
When deposit POSTED
Then 2110 CR 75000 and 4110 unchanged
And wallet credited 75000
```

### Scenario: Duplicate webhook — no double post

```gherkin
Given deposit "dep-2" already POSTED and wallet credited 99000
When same bank webhook arrives again
Then same coa_trans_id returned
And wallet available still 99000
And no second DEPOSIT_CREDIT row
```

### Scenario: Duplicate phase B — idempotent POSTED

```gherkin
Given "dep-2b" POSTED
When confirm retried with same payload
Then status still POSTED
And line count unchanged
And transit 3100 still zero
```

### Scenario: PENDING — wallet not credited

```gherkin
Given phase A posted PENDING for "dep-3" with 3100 CR 100000
When wallet credit attempted before POSTED
Then credit rejected or not invoked
And wallet available unchanged
```

### Scenario: POSTED but wallet consumer down

```gherkin
Given journal "dep-4" POSTED with 2110 +99000
And wallet credit not yet applied
When consumer retries WALLET_CREDIT with businessRef="dep-4"
Then wallet available becomes 99000 exactly once
```

### Scenario: VA unmapped — stays PENDING

```gherkin
Given webhook "dep-5" with unknown VA
When orchestration cannot resolve memberId
Then journal remains PENDING
And transit 3100 holds 100000
And ops queue receives alert
```

### Scenario: Amount mismatch — do not confirm

```gherkin
Given PENDING "dep-6" for 100000
When confirm attempted with bank actual 99000
Then phase B rejected
And status remains PENDING or moves to FAILED per policy
And wallet not credited
```

### Scenario: Confirm exceeds PENDING gross

```gherkin
Given PENDING "dep-7" for 100000
When confirm amount=150000
Then phase B rejected
And 3100 still holds original pending amount
```

### Scenario: FAILED from PENDING — transit released per policy

```gherkin
Given PENDING "dep-8" 3100 CR 80000
When ops marks FAILED after SLA
Then status=FAILED
And reversing entries or transit clearance per accounting policy
And wallet never credited
```

### Scenario: Concurrent webhooks same ref

```gherkin
Given two workers process webhook businessRef="dep-9" simultaneously
When both attempt create journal
Then exactly one coa_trans row for "dep-9"
And one POSTED outcome
```

### Scenario: Journal posted event emitted once

```gherkin
Given deposit "dep-10" reaches POSTED
When outbox dispatches
Then one JournalPosted with coaTransId and businessRef="dep-10"
And duplicate dispatch is no-op for wallet
```

### Scenario: Large deposit — balance precision

```gherkin
Given amount=999999999999 minor units valid for currency
When POSTED
Then all line amounts integer minor units
And SUM DR = SUM CR
```

### Scenario: Member wallet created on first credit

```gherkin
Given member 100999 has no USER wallet row
When POSTED deposit credits net 50000
Then wallet row created USER/VND
And available=50000
```

### Scenario: Wrong member mapping corrected before POSTED

```gherkin
Given PENDING "dep-11" mapped to wrong member
When ops remaps and confirm succeeds
Then POSTED credits correct member only
```

### Scenario: Fee change on replay — conflict

```gherkin
Given POSTED "dep-12" fee=1000
When replay command fee=2000 same businessRef
Then idempotent return without new lines
And 4110 still 1000
```

### Scenario TC-DEP-01: Full async chain — Phase A PENDING → Phase B POSTED → wallet credit

```gherkin
Given a BANK_DEPOSIT command with businessRef="dep-tc01" grossAmount=100000 fee=1000 walletId=42
When the accounting worker processes the message
Then a PENDING journal is created (TigerBeetle pending Transfer 1111←3100)
And the journal transitions to POSTED (TigerBeetle post_pending + transfers 3100→2110 net, 3100→4110 fee)
And a WALLET_CREDIT command is published to exchange "core.commands" key "core.commands.wallet-credit"
And the wallet worker credits walletId=42 with netAmount=99000
And available balance of walletId=42 increases by 99000
```

### Scenario TC-DEP-02: Fee deducted — wallet receives gross minus fee

```gherkin
Given a BANK_DEPOSIT command grossAmount=100000 fee=1000 walletId=42
When the full async chain completes
Then the WALLET_CREDIT payload carries netAmount=99000
And walletId=42 available balance increases by 99000 not 100000
And CoA account 4110 (fee income) holds 1000
And CoA account 2110 (customer liability) holds 99000
```

### Scenario TC-DEP-03: HTTP 202 returned before any journal write

```gherkin
Given app-orchestration receives a bank webhook for businessRef="dep-tc03"
When the orchestration layer handles the request
Then it responds HTTP 202 immediately
And the BANK_DEPOSIT command is written to the outbox in the same local transaction as the 202 response
And no JournalPosted event exists yet at the time 202 is returned
And the journal is written asynchronously by the accounting worker after 202
```

### Scenario TC-DEP-04: BANK_DEPOSIT delivered via RabbitMQ queue, not HTTP

```gherkin
Given app-orchestration has written a BANK_DEPOSIT command to the outbox
When the outbox relay dispatches the command
Then the message is published to RabbitMQ exchange "core.commands" with routing key "core.commands.bank-deposit"
And app-accounting-worker consumes it from queue "accounting.bank-deposit"
And no HTTP call is made from app-orchestration to app-accounting or app-accounting-worker
```

### Scenario TC-DEP-05: Unknown virtual account — ops hold, no journal, no wallet credit

```gherkin
Given virtualAccount="VA-UNKNOWN-999" is not present in orchestration's VA mapping table
When app-orchestration receives a bank deposit webhook for VA-UNKNOWN-999 with businessRef="dep-tc05"
Then HTTP 202 is returned to the bank gateway
And no BANK_DEPOSIT command is published to RabbitMQ
And no coa_trans row exists for businessRef="dep-tc05"
And no wallet_tx credit is created for businessRef="dep-tc05"
And an ops hold record is logged with virtualAccount="VA-UNKNOWN-999" and businessRef="dep-tc05"
```

### Scenario TC-DEP-06: Known VA — credit lands on the correct member's wallet only

```gherkin
Given member 1001 owns walletId=55 mapped to virtualAccount="VA-1001"
And member 1002 owns walletId=88 mapped to virtualAccount="VA-1002"
And a bank deposit notification for virtualAccount="VA-1001" grossAmount=100000 fee=1000
When the full async chain completes for businessRef="dep-tc06"
Then walletId=55 (member 1001) is credited with netAmount=99000
And walletId=88 (member 1002) is not credited
And no other wallet receives any credit for businessRef="dep-tc06"
```

### Scenario TC-DEP-07: VA mapping change does not alter historical POSTED journals

```gherkin
Given a POSTED journal for businessRef="dep-tc07" was created when VA-1001 mapped to member 1001 (walletId=55)
And wallet_tx DEPOSIT_CREDIT for businessRef="dep-tc07" is recorded against walletId=55
When ops updates the VA mapping so VA-1001 now points to member 1003 (walletId=77)
Then the coa_trans row for businessRef="dep-tc07" is unchanged (status=POSTED, lines immutable)
And wallet_tx for businessRef="dep-tc07" still belongs to walletId=55
And the mapping change only affects future deposit notifications for VA-1001
```

### Scenario TC-DEP-08: Phase-A cancellation — 3100 nets to zero, wallet unchanged

```gherkin
Given a BANK_DEPOSIT command with businessRef="dep-tc08" grossAmount=100000 has been processed
And a PENDING journal exists (coa_trans status=PENDING, TigerBeetle pending Transfer 1111←3100)
And no WALLET_CREDIT has been published
When ops or a bank cancellation event triggers Phase-A reversal for businessRef="dep-tc08"
Then TigerBeetle void_pending_transfer(hash("dep-tc08:phaseA")) is called
And coa_trans.status becomes FAILED for businessRef="dep-tc08"
And account[3100].balance = 0
And account[1111] is restored to its pre-deposit value
And wallet_balance.available is unchanged (no DEPOSIT_CREDIT wallet_tx for dep-tc08)
And no WALLET_CREDIT command is published
```

### Scenario TC-DEP-09: Amount mismatch — Phase B fails validation, Phase A reversed, wallet untouched

```gherkin
Given a PENDING journal for businessRef="dep-tc09" with grossAmount=100000 (Phase A done)
When confirmDeposit is called with fee=5000 but grossAmount does not equal net+fee (validation fails)
Then no TigerBeetle Phase B transfers are created
And void_pending_transfer(hash("dep-tc09:phaseA")) is called
And coa_trans.status becomes FAILED
And account[3100].balance = 0
And no WALLET_CREDIT command is published
And wallet_balance.available is unchanged
```

### Scenario TC-DEP-10: PENDING aging — ops alert triggered, no auto-reversal

```gherkin
Given a PENDING journal for businessRef="dep-tc10" with coa_trans.status=PENDING
When the PENDING journal exceeds the SLA threshold without a confirmDeposit call
Then an aging alert is raised to the ops channel
And coa_trans.status remains PENDING (no auto-reversal)
And account[3100] continues to hold the gross amount
And wallet_balance.available is unchanged
And ops must explicitly choose: call confirmDeposit (→ POSTED) or void (→ FAILED)
```

### Scenario TC-DEP-11: Duplicate deposit notification — exactly one journal, one wallet credit

```gherkin
Given a BANK_DEPOSIT command with businessRef="dep-tc11" has been fully processed (POSTED + wallet credited)
When the bank gateway re-sends the same deposit notification with businessRef="dep-tc11"
Then app-orchestration returns 202 (idempotent re-ack)
And no second outbox row is written for businessRef="dep-tc11"
And coa_trans has exactly one row for businessRef="dep-tc11" (status=POSTED)
And wallet_tx has exactly one DEPOSIT_CREDIT row for businessRef="dep-tc11"
And wallet_balance.available is not double-incremented
```

### Scenario TC-DEP-12: Worker crash between Phase A and Phase B — idempotent on redelivery

```gherkin
Given a BANK_DEPOSIT message with businessRef="dep-tc12" caused app-accounting-worker to crash after Phase A (coa_trans=PENDING, TigerBeetle pending Transfer created)
When RabbitMQ redelivers the BANK_DEPOSIT message for businessRef="dep-tc12"
Then createJournal returns the existing PENDING coa_trans row (idempotent — no duplicate insert)
And confirmDeposit proceeds with Phase B using the existing coaTransId
And Phase B runs exactly once (TigerBeetle Transfer IDs are deterministic — duplicate TB calls are no-ops)
And coa_trans.status becomes POSTED
And WALLET_CREDIT is published and wallet is credited exactly once
```

### Deposit — SQL invariant CI coverage note (ADR-031 INV-03)

TC-DEP-01..TC-DEP-09 collectively exercise INV-03: `account[3100].balance = 0` after every POSTED or FAILED deposit. Specifically:
- TC-DEP-01 (full happy path) — verifies 3100=0 after Phase B POSTED
- TC-DEP-08 (cancel/reversal) — verifies 3100=0 after `void_pending_transfer`
- TC-DEP-09 (mismatch) — verifies 3100=0 after Phase B abort + void

The SQL invariant CI job (ADR-031 INV-03) enforces this at the database layer on every CI run independently of Gherkin execution. See `## Feature: Ledger invariant CI (ADR-031)` below for the CI scenarios.

### Scenario TC-DEP-13: TigerBeetle boundary — only app-accounting-worker touches TB

```gherkin
Given the async deposit flow from bank webhook to wallet credit
When the full chain executes (app-orchestration → outbox → app-accounting-worker → app-wallet-worker)
Then only app-accounting-worker opens a TigerBeetle client connection (AC-037-01)
And app-orchestration makes no direct TigerBeetle API calls
And app-wallet-worker makes no direct TigerBeetle API calls
And app-wallet makes no direct TigerBeetle API calls
And the accounting-internal.yaml HTTP contract is unchanged by the TigerBeetle integration (AC-037-02)
```

---

## Feature: Wallet balance semantics (available / frozen / pending)

Maps [`wallet.md`](./wallet.md) §2.1 — no DR/CR in this feature.

### Scenario: Deposit PENDING — wallet unchanged

```gherkin
Given webhook "dep-sem-1" phase A complete status=PENDING
When member queries wallet balance
Then available=0 and frozen=0
And orchestration may show deposit status=PENDING separately
```

### Scenario: Spendable equals available not pending inflow

```gherkin
Given user available=200000 frozen=0
When payment debit gross=50000 attempted
Then debit uses available only
And available=150000 after success
```

### Scenario: Freeze reduces available increases frozen — total constant

```gherkin
Given available=300000 frozen=0
When WITHDRAW_FREEZE gross=101000
Then available=199000 frozen=101000
And available+frozen=300000
```

### Scenario: SETTLE reduces frozen only

```gherkin
Given available=199000 frozen=101000
When WITHDRAW_SETTLE gross=101000
Then available=199000 frozen=0
```

### Scenario: RELEASE restores available

```gherkin
Given available=199000 frozen=101000
When WITHDRAW_RELEASE gross=101000
Then available=300000 frozen=0
```

---

## Feature: Withdraw (transit 3200, freeze pattern)

### Scenario: Happy — freeze then settle

```gherkin
Given user available=200000
When withdraw businessRef="wd-1" principal=100000 fee=1000
Then WITHDRAW_FREEZE gross 101000 before 200
And journal POSTED 3200=0
And available=99000 frozen=101000
When bank payout succeeds
Then WITHDRAW_SETTLE debits frozen 101000
And frozen=0
```

### Scenario: Ledger POSTED lines on accept

```gherkin
Given withdraw principal=50000 fee=500 gross=50500
When accept returns 200
Then POSTED: 2110 DR 50000, 5100 DR 500, 4130 CR 500, 3200 net zero
And wallet frozen=50500
```

### Scenario: Bank reject — release

```gherkin
Given withdraw "wd-2" frozen 101000
When bank returns reject
Then WITHDRAW_RELEASE
And available restored by 101000
And frozen=0
```

### Scenario: Bank timeout — do not release early

```gherkin
Given withdraw "wd-3" frozen 101000
When payout times out with unknown status
Then funds remain frozen
And no WITHDRAW_RELEASE until terminal bank status
```

### Scenario: Duplicate accept

```gherkin
Given withdraw "wd-4" already frozen
When client retries same idempotency key
Then 200 same state
And single WITHDRAW_FREEZE
```

### Scenario: Insufficient available — no freeze

```gherkin
Given available=50000
When withdraw gross=101000
Then 4xx insufficient
And no journal POSTED
And frozen=0
```

### Scenario: SETTLE idempotent

```gherkin
Given "wd-5" frozen 50500 POSTED
When bank success triggers settle twice
Then frozen=0 after first settle
And second settle no-op same wallet_tx id
```

### Scenario: RELEASE after partial bank ambiguity

```gherkin
Given "wd-6" frozen
When bank returns explicit REJECTED
Then RELEASE once
And available increased by frozen amount before release
```

### Scenario: SETTLE without prior freeze — rejected

```gherkin
Given no WITHDRAW_FREEZE for "wd-7"
When settle command arrives
Then wallet rejects
And ops alert
```

### Scenario: Double RELEASE — idempotent

```gherkin
Given "wd-8" already RELEASED
When release retried
Then balances unchanged
And same wallet_tx id
```

### Scenario: POSTED before freeze — ordering violation prevented

```gherkin
Given orchestration attempts ledger POSTED before freeze succeeds
Then flow aborts or freeze-first ordering enforced
And no orphan POSTED without matching frozen balance
```

### Scenario: Zero fee withdraw

```gherkin
Given principal=100000 fee=0
When POSTED on accept
Then 5100 unchanged
And 2110 DR 100000
And frozen=100000
```

---

## Feature: Wallet payment (sync, transit 3500)

### Scenario: Happy — 100,000 gross

```gherkin
Given user 100234 available=200000
And merchant 200001 MERCHANT available=0
When createPayment businessRef="pay-1" amount=100000
Then PAYMENT_DEBIT user -100000
And journal POSTED with 3500 net zero
And PAYMENT_CREDIT merchant +100000
And response 200 with walletTxId and coaTransId
```

### Scenario: Ledger lines payment

```gherkin
Given payment gross=100000 fee=0
When POSTED
Then 2110 DR 100000, 2120 CR 100000, transit 3500 net zero
```

### Scenario: Insufficient balance

```gherkin
Given user available=50000
When createPayment amount=100000
Then 4xx insufficient balance
And no journal POSTED
And no merchant credit
```

### Scenario: User debited, post fails

```gherkin
Given user debited 100000 for "pay-2"
And accounting post fails
Then orchestration triggers compensation policy
And user balance restored OR case escalated (no silent loss)
```

### Scenario: Posted, merchant credit fails

```gherkin
Given journal POSTED for "pay-3"
And user debited 100000
When merchant credit fails transiently
Then retry PAYMENT_CREDIT succeeds idempotently
And merchant available=100000
```

### Scenario: Idempotent replay

```gherkin
Given payment "pay-4" completed
When client retries createPayment same X-Idempotency-Key
Then 200 with same ids
And balances unchanged
```

### Scenario: Debit succeeds credit fails — ledger already POSTED

```gherkin
Given POSTED "pay-5" with user debited
And merchant credit pending
When retry succeeds
Then merchant +100000 once
And coa_trans unchanged
```

### Scenario: Payment with platform fee (orchestration passes net)

```gherkin
Given gross=100000 fee=1000 netToMerchant=99000
When POSTED per foundation §9
Then 2120 CR 99000 and fee lines per §9
And PAYMENT_CREDIT merchant 99000
```

### Scenario: Self-payment blocked

```gherkin
Given payer wallet equals payee wallet
When createPayment
Then 4xx business rule
And no POSTED
```

### Scenario: MERCHANT locked — credit rejected

```gherkin
Given merchant wallet LOCKED
When payment POSTED attempted end-to-end
Then credit fails WALLET_LOCKED
And compensation policy for user debit applies
```

---

## Feature: Internal transfer (transit 3300)

### Scenario: Happy — fee 1000 on gross 101000

```gherkin
Given user A available=200000
And user B available=0
When transfer businessRef="xfer-1" amount=100000 fee=1000
Then TRANSFER_DEBIT A gross 101000
And journal POSTED 3300=0 and 4130 +1000
And TRANSFER_CREDIT B net 100000
And A available=99000
And B available=100000
```

### Scenario: Ledger transfer lines

```gherkin
Given net to B=100000 fee=1000
When POSTED
Then 2110 DR 101000 (A), 2110 CR 100000 (B), 4130 CR 1000, 3300 net zero
```

### Scenario: Debit OK, credit B fails

```gherkin
Given A debited for "xfer-2"
And credit B not applied
When retry transfer processing
Then B credited 100000 once
And A not debited twice
```

### Scenario: Same member A to A — rejected

```gherkin
Given fromWallet=toWallet
When transfer requested
Then 4xx
And no POSTED
```

### Scenario: Insufficient including fee

```gherkin
Given A available=100000
When transfer net=100000 fee=1000
Then insufficient
And no debit
```

### Scenario: Zero fee transfer

```gherkin
Given fee=0 gross=50000
When POSTED
Then 2110 DR 50000 CR 50000 same member aggregate pool
And B wallet +50000
```

---

## Feature: IBFT (transit 3400)

### Scenario: Happy — settle after bank OK

```gherkin
Given user available=300000
When IBFT businessRef="ibft-1" principal=100000 fee=1000
Then IBFT_FREEZE 101000
And ledger POSTED 3400=0
When Napas success
Then IBFT_SETTLE from frozen
```

### Scenario: Ledger IBFT POSTED on accept

```gherkin
Given principal=100000 fee=1000
When accept
Then 2110 DR 100000, 5100 DR 1000, 4130 CR 1000, 3400 net zero
```

### Scenario: Bank fail — release

```gherkin
Given IBFT "ibft-2" frozen
When Napas fail
Then IBFT_RELEASE
And available restored
```

### Scenario: Timeout — frozen held

```gherkin
Given "ibft-3" frozen
When Napas timeout unknown
Then no RELEASE
And frozen unchanged until terminal status
```

### Scenario: SETTLE idempotent after success

```gherkin
Given bank SUCCESS for "ibft-4"
When settle dispatched twice
Then frozen=0 once
```

---

## Feature: QR / POS acquirer (ledger-heavy, optional wallet)

### Scenario: Acquirer capture POSTED — no wallet per txn

```gherkin
Given QR capture businessRef="qr-1" gross=200000
When POSTED per foundation §12
Then acquirer transit lines net zero
And merchant wallet available unchanged (product default)
```

### Scenario: EOD optional MERCHANT_SETTLE_CREDIT

```gherkin
Given EOD settlement credits merchant wallet per product
When MERCHANT_SETTLE_CREDIT issued
Then merchant available increases per batch ref
And ledger §16 already POSTED
```

---

## Feature: Payroll batch

### Scenario: POSTED payroll debits merchant gross

```gherkin
Given payroll businessRef="pr-1" gross=5000000 fee=50000
When batch POSTED
Then 2120 DR gross, fee to 4130, transit per §14
And PAYROLL_DEBIT merchant available reduced by gross
```

### Scenario: Insufficient merchant — batch rejected

```gherkin
Given MERCHANT available < gross
When payroll submit
Then reject before POSTED
```

---

## Feature: Partner disbursement

### Scenario: Prefund then disburse

```gherkin
Given PARTNER prefund businessRef="pf-1" amount=10000000
When POSTED and PARTNER_PREFUND_CREDIT
Then PARTNER available=10000000
When disbursement gross=2000000
Then DISBURSEMENT_DEBIT and ledger §15 POSTED
```

### Scenario: Disburse exceeds partner available

```gherkin
Given PARTNER available=100000
When disbursement gross=500000
Then reject
And no POSTED
```

---

## Feature: Idempotency conflicts

### Scenario: Same ref, different amount

```gherkin
Given wallet_tx exists businessRef="x-1" amount=100000
When command arrives businessRef="x-1" amount=90000
Then WALLET_DUPLICATE_CONFLICT / HTTP 409
And balance unchanged
```

### Scenario: Payment two legs same ref

```gherkin
Given payment "pay-5" needs DEBIT and CREDIT
When both legs processed
Then UNIQUE (wallet_id, business_ref, tx_type) satisfied
And both legs succeed under same businessRef
```

### Scenario: Ledger same business_ref different use_case — rejected

```gherkin
Given coa_trans businessRef="z-1" use_case=DEPOSIT
When create PAYMENT same businessRef
Then accounting rejects duplicate ref conflict
```

---

## Feature: Wallet lock

### Scenario: Locked wallet rejects debit

```gherkin
Given wallet status=LOCKED
When debit attempted
Then WALLET_LOCKED error
And no wallet_tx
```

### Scenario: Locked wallet rejects freeze

```gherkin
Given USER wallet LOCKED
When withdraw accept
Then reject before freeze
```

### Scenario: Credit to LOCKED — rejected (ADR-034)

```gherkin
Given wallet LOCKED
When DEPOSIT_CREDIT after POSTED
Then WALLET_LOCKED
And no wallet_tx created
And saga remains AWAITING_WALLET
```

---

## Feature: Reconciliation (W5)

### Scenario: Timing drift within tolerance

```gherkin
Given POSTED deposit not yet credited (async lag)
When reconciliation runs
Then drift logged if within policy window
And no auto COA adjustment
```

### Scenario: Persistent drift

```gherkin
Given SUM(user wallets) != 2110 beyond tolerance for > SLA
When nightly recon runs
Then ops alert severity=high
And report lists candidate missing credits
```

### Scenario: Frozen sum vs in-flight withdraw ledger

```gherkin
Given many frozen withdraws
When recon runs
Then SUM(user frozen) aligns with 2110 reduction vs available within model
```

---

## Feature: EOD settlement (ledger only)

### Scenario: Happy lock and release

```gherkin
Given merchant 2120 balance 200000 pending settlement
When EOD job runs matched file
Then 3800/3810/3820 transits net zero
And 2120 reduced per settlement
```

### Scenario: File mismatch

```gherkin
Given acquirer file total != internal 2120
When EOD reconciliation fails
Then settlement blocked
And 2120 unchanged
And exception queue entry
```

### Scenario: Partial file — no POSTED

```gherkin
Given file missing merchant rows
When EOD validator runs
Then block settlement
And prior day 2120 intact
```

---

## Feature: Adjustment / ops (future)

### Scenario: Manual ADJUSTMENT after wrong credit

```gherkin
Given ops approved correction businessRef="adj-1"
When ADJUSTMENT POSTED
Then compensating lines per policy
And matching wallet_tx if member impact
```

---

# Part II — Extended conformance matrix

Scenario IDs for traceability. DR/CR detail: [`../spec/foundation.md`](../spec/foundation.md) §8–16.

---

## Feature: Deposit — extended (DEP-E)

### Scenario DEP-E01: Minimum amount 1 minor unit

```gherkin
Given gross=1 fee=0
When deposit POSTED
Then 2110 CR 1 and wallet credited 1
And 3100 net zero
```

### Scenario DEP-E02: Large gross 500,000,000 fee 5,000

```gherkin
Given gross=500000000 fee=5000
When POSTED
Then 2110 CR 499995000 and 4110 CR 5000
And wallet credited 499995000
```

### Scenario DEP-E03: Fee equals gross — net zero credit

```gherkin
Given gross=10000 fee=10000
When POSTED
Then 2110 CR 0 and 4110 CR 10000
And wallet credited 0 or credit skipped per product
```

### Scenario DEP-E04: Two deposits same user sequential

```gherkin
Given user available=99000 after "dep-a" POSTED
When "dep-b" gross=50000 fee=0 POSTED
Then available=149000
And two distinct coa_trans rows
```

### Scenario DEP-E05: PENDING aging alert

```gherkin
Given "dep-age" PENDING 3100 CR 200000 for 48h
When aging job runs
Then ops alert fired
And status still PENDING until human action
```

### Scenario DEP-E06: Phase B worker crash mid-TX

```gherkin
Given PENDING "dep-crash" valid
When phase B TX rolls back on crash
Then still PENDING
And retry phase B idempotent succeeds
```

### Scenario DEP-E07: JournalPosted before wallet consumer ready

```gherkin
Given POSTED "dep-evt" emitted
When no consumer for 10 minutes
Then recon flags lag
And eventual credit idempotent
```

### Scenario DEP-E08: Credit command duplicate from outbox relay

```gherkin
Given WALLET_CREDIT delivered twice at-least-once
When both processed
Then single DEPOSIT_CREDIT row
```

### Scenario DEP-E09: Wrong member credit attempt blocked

```gherkin
Given POSTED mapped to member 100234
When credit command targets member 100235
Then reject or orchestration never sends wrong member
```

### Scenario DEP-E10: Reversal after POSTED bank recall

```gherkin
Given POSTED "dep-rec" credited 99000
When reversal journal posted
Then 2110 reduced and ops wallet debit policy triggered
```

### Scenario DEP-E11: Deposit during wallet LOCKED

```gherkin
Given USER wallet LOCKED
When POSTED deposit net=50000
Then WALLET_CREDIT rejected WALLET_LOCKED
And 2110 POSTED stands until ops unlock and retry
```

### Scenario DEP-E12: Concurrent PENDING same ref

```gherkin
Given two workers phase A same businessRef
Then exactly one PENDING journal
```

### Scenario DEP-E13: getDepositStatus PENDING

```gherkin
Given "dep-st" PENDING
When client polls status
Then returns PENDING and 3100 non-zero
```

### Scenario DEP-E14: getDepositStatus POSTED

```gherkin
Given "dep-st2" POSTED credited
When client polls
Then POSTED and wallet matches net
```

### Scenario DEP-E15: S6 BANK_DEPOSIT poison → DLQ

```gherkin
Given malformed envelope
When worker max retries exceeded
Then message in DLQ and CommandFailed event
And no orphan POSTED
```

---

## Feature: Withdraw — extended (WD-E)

### Scenario WD-E01: Freeze exact available boundary

```gherkin
Given available=101000
When withdraw gross=101000 fee=0
Then freeze succeeds and available=0 frozen=101000
```

### Scenario WD-E01b: Freeze one over available

```gherkin
Given available=101000
When withdraw gross=101001
Then reject before freeze
```

### Scenario WD-E02: Napas poll SUCCESS after 3 timeouts

```gherkin
Given "wd-poll" frozen 50500
When bank unknown unknown then SUCCESS
Then SETTLE once frozen=0
And no RELEASE in between
```

### Scenario WD-E03: RELEASE then client retry accept

```gherkin
Given "wd-rel" RELEASED available restored
When client retries same idempotency key
Then 200 terminal released state
And no second freeze
```

### Scenario WD-E04: SETTLE retry 5 times

```gherkin
Given bank SUCCESS "wd-set"
When settle fails 4 times then succeeds
Then frozen=0 after first successful settle only
```

### Scenario WD-E05: POSTED 3200 guard on malformed lines

```gherkin
Given withdraw post missing 4120 line when fee>0
Then post rejected
And wallet freeze compensated per policy
```

### Scenario WD-E06: Two withdraws same user in flight

```gherkin
Given frozen 101000 for "wd-a"
And available=99000
When second withdraw gross=99000
Then second freeze succeeds if available sufficient
```

### Scenario WD-E07: Payout worker duplicate S6

```gherkin
Given payout command redelivered
When bank adapter idempotent
Then single bank movement
```

### Scenario WD-E08: Frozen user LOCKED after freeze

```gherkin
Given withdraw frozen in flight
When ops sets wallet LOCKED
Then settle still allowed or ops-gated per policy
And new withdraw rejected
```

### Scenario WD-E09: Principal 1 fee 0

```gherkin
Given withdraw principal=1
When POSTED and SETTLE
Then 2110 DR 1 and 1111 CR 1
```

### Scenario WD-E10: Bank SUCCESS but SETTLE before POSTED ordering bug

```gherkin
Given orchestration enforces freeze-before-post
When SETTLE attempted without freeze
Then wallet rejects
```

---

## Feature: Payment — extended (PAY-E)

### Scenario PAY-E01: Gross 1 VND

```gherkin
Given user available=100
When payment amount=1
Then POSTED 3500=0 and merchant +1
```

### Scenario PAY-E02: Net to merchant with fee 1000 on 100000

```gherkin
Given gross=100000 fee=1000 netToMerchant=99000
When completed
Then PAYMENT_DEBIT 100000 and PAYMENT_CREDIT 99000
```

### Scenario PAY-E03: Merchant auto-provision on first payment

```gherkin
Given merchant 200999 no wallet row
When payment completes
Then MERCHANT wallet created and credited
```

### Scenario PAY-E04: Compensation after post fail

```gherkin
Given user debited 50000 for "pay-comp"
And post fails
When compensation runs
Then user available restored via {ref}:comp
And no POSTED journal
```

### Scenario PAY-E05: Merchant credit 3 retries

```gherkin
Given POSTED "pay-retry"
When merchant credit fails twice then succeeds
Then merchant available correct once
```

### Scenario PAY-E06: Response 200 lost client retry

```gherkin
Given payment fully complete
When client retries same idempotency key
Then 200 same ids unchanged balances
```

### Scenario PAY-E07: Payment with zero merchant (invalid)

```gherkin
Given payee merchantId unknown
Then 404 before debit
```

### Scenario PAY-E08: Concurrent payments last cent race

```gherkin
Given available=10000
When two payments 10000 simultaneous
Then one succeeds one insufficient
```

### Scenario PAY-E09: PAYMENT_DEBIT without CREDIT stuck

```gherkin
Given user debited POSTED
And merchant credit permanently failing
Then ops alert saga partial
And ledger POSTED unchanged
```

### Scenario PAY-E10: Idempotency key cross-member rejected

```gherkin
Given member A used key "k-1" for payment
When member B sends same key
Then reject authorization scope
```

---

## Feature: Transfer — extended (TRF-E)

### Scenario TRF-E01: Net 50M fee 1000

```gherkin
Given A available=100000000
When transfer net=50000000 fee=1000
Then A debited 50001000 B credited 50000000
And 4130 CR 1000 on ledger
```

### Scenario TRF-E02: Transfer net only no fee

```gherkin
Given fee=0 net=25000
When POSTED
Then 3300=0 and A -25000 B +25000
```

### Scenario TRF-E03: B credit retry after POSTED

```gherkin
Given A debited POSTED
When B credit fails then succeeds
Then B +net once
```

### Scenario TRF-E04: A compensate when post never happens

```gherkin
Given A debited no POSTED
When saga compensates
Then A restored gross
```

### Scenario TRF-E05: Different currency rejected

```gherkin
Given wallet VND only
When transfer with currency USD
Then 400 before mutation
```

### Scenario TRF-E06: Amount scale 5 rejected

```gherkin
Given amount with 5 decimal places
Then validation reject
```

---

## Feature: IBFT — extended (IBFT-E)

### Scenario IBFT-E01: Napas fee 5100 on ledger

```gherkin
Given IBFT POSTED per foundation §11
Then 5100 DR present and 3400 net zero
```

### Scenario IBFT-E02: Frozen 72h unknown then FAIL

```gherkin
Given frozen 3 days bank unknown
When bank returns FAILED terminal
Then IBFT_RELEASE once
```

### Scenario IBFT-E03: Duplicate :settle

```gherkin
Given SETTLE completed
When :settle replayed
Then idempotent no double frozen debit
```

### Scenario IBFT-E04: IBFT_FREEZE same as withdraw insufficient

```gherkin
Given available < gross
Then reject no 3400 POSTED
```

### Scenario IBFT-E05: 1112 not 1111 on bank leg

```gherkin
Given IBFT POSTED
Then bank leg uses 1112 per foundation §11
```

---

## Feature: Cross-cutting — extended (X-E)

### Scenario X-E01: business_ref max length boundary

```gherkin
Given businessRef at schema max length
When flow completes
Then stored and idempotent
```

### Scenario X-E02: Empty idempotency key

```gherkin
When S1 request without X-Idempotency-Key on mutating endpoint
Then 400
```

### Scenario X-E03: Period close blocks new post

```gherkin
Given accounting period closed
When post attempted dated in closed period
Then ACCOUNTING_PERIOD_CLOSED
```

### Scenario X-E04: Reversal chain reverses_id

```gherkin
Given reversal of POSTED journal
Then new journal links reverses_id
And original lines unchanged
```

### Scenario X-E05: W5 recon within 5 min async lag

```gherkin
Given POSTED 1 min ago wallet not credited
When recon runs
Then no high alert within tolerance window
```

### Scenario X-E06: W5 recon beyond SLA

```gherkin
Given POSTED 2h ago no wallet credit
When recon runs
Then high severity alert lists businessRef
```

### Scenario X-E07: Outbox publish failure retry

```gherkin
Given domain TX committed outbox pending
When relay retries
Then exactly one external publish effect at consumers
```

### Scenario X-E08: CommandFailed fan-out

```gherkin
Given worker permanent failure
Then core.operations.command-failed emitted
And ops can correlate businessRef
```

### Scenario X-E09: JWT memberId mismatch body

```gherkin
Given token sub=100234
When body payerId=100235
Then 403
```

### Scenario X-E10: mTLS bank webhook invalid cert

```gherkin
When notifyDeposit without valid mTLS
Then 401 and no PENDING
```

### Scenario X-E11: Optimistic version conflict retry

```gherkin
Given two concurrent debits on same wallet_id
When second TX hits version mismatch
Then retryable conflict returned to orchestration
And exactly one debit succeeds for last-cent case
```

### Scenario X-E12: IBFT RELEASE forbidden after SETTLE

```gherkin
Given IBFT "ibft-sm-1" SETTLE completed frozen=0
When RELEASE dispatched
Then no-op or state machine reject
And available unchanged
```

### Scenario X-E13: Orchestration poll UNKNOWN — no RELEASE

```gherkin
Given withdraw or IBFT frozen
When bank adapter returns UNKNOWN
Then orchestration schedules poll
And wallet frozen unchanged until terminal status
```

---

# Part III — Batch & acquirer flows

Foundation §12–16. Default: QR/POS **no per-txn wallet** until optional EOD `MERCHANT_SETTLE_CREDIT`.

---

## Feature: QR/POS — extended (QR-E)

### Scenario QR-E01: Acquirer capture happy

```gherkin
Given capture businessRef="qr-100" gross=100000
When POSTED per foundation §12
Then 1113 DR net of acquirer cost, 3500=0, 2120 CR 100000
And merchant wallet available unchanged
```

### Scenario QR-E02: Acquirer cost 5100 line

```gherkin
Given capture gross=100000 acquirer cost=500
When POSTED
Then 5100 DR 500 and 1113 net +99500
```

### Scenario QR-E03: Duplicate capture id

```gherkin
Given "qr-dup" already POSTED
When same acquirer id retried
Then idempotent same coa_trans_id
```

### Scenario QR-E04: 2120 pending until EOD

```gherkin
Given multiple QR captures same merchant
When before EOD
Then 2120 sum equals pending settlement total
```

### Scenario QR-E05: Capture amount mismatch recon

```gherkin
Given acquirer file row 99000 internal 100000
When daily recon runs
Then exception queue not auto-adjust
```

### Scenario QR-E06: Zero amount capture rejected

```gherkin
When capture amount=0
Then reject before POSTED
```

### Scenario QR-E07: Merchant inactive COA 2120

```gherkin
Given merchant COA inactive
When post capture
Then ACCOUNTING_INACTIVE_ACCOUNT
```

### Scenario QR-E08: Multi-capture same terminal same day

```gherkin
Given 50 captures distinct businessRef
When all POSTED
Then each 3500=0 per journal
```

### Scenario QR-E09: Optional wallet credit at capture disabled

```gherkin
Given product flag walletPerCapture=false
When POSTED
Then no MERCHANT wallet_tx
```

### Scenario QR-E10: MDR deferred to EOD only

```gherkin
Given capture 2120 CR gross
When EOD runs
Then 4140 MDR from 3820 branch not at capture
```

---

## Feature: Payroll — extended (PR-E)

### Scenario PR-E01: Batch 5 employees foundation example

```gherkin
Given payroll businessRef="pr-batch-1" gross=5050000 fee=50000
When POSTED per foundation §14
Then 3600=0 and 4150 CR 50000
And PAYROLL_DEBIT merchant 5050000
```

### Scenario PR-E02: Insufficient merchant before post

```gherkin
Given MERCHANT available=1000000
When payroll gross=5050000
Then reject before POSTED and PAYROLL_DEBIT
```

### Scenario PR-E03: Partial recipient failure

```gherkin
Given batch 10 recipients
When recipient 7 bank reject
Then 9 succeed 1 failed in summary
And 3600=0 for succeeded slice per policy
```

### Scenario PR-E04: Per-recipient idempotency

```gherkin
Given "pr-batch-2:emp-003" succeeded
When retry same recipient key
Then idempotent no double bank out
```

### Scenario PR-E05: Duplicate batch ref

```gherkin
Given "pr-batch-3" completed
When resubmit same batch id
Then idempotent full batch result
```

### Scenario PR-E06: Fee-only adjustment same batch

```gherkin
Given same batch ref different fee amount
Then 409 conflict
```

### Scenario PR-E07: 5100 bank cost on payroll

```gherkin
Given POSTED payroll with bank leg
Then 5100 DR per foundation §14 template
```

### Scenario PR-E08: Merchant LOCKED

```gherkin
Given MERCHANT wallet LOCKED
When payroll submit
Then WALLET_LOCKED reject
```

### Scenario PR-E09: Empty recipient list

```gherkin
When payroll batch zero recipients
Then 400 no POSTED
```

### Scenario PR-E10: Single employee net 100000

```gherkin
Given 1 recipient salary=100000 fee batch=1000
When POSTED
Then 2120 DR 101000 and 3600 clears
```

---

## Feature: Disbursement — extended (DIS-E)

### Scenario DIS-E01: Pre-fund then disburse

```gherkin
Given PARTNER prefund businessRef="pf-1" 10000000
When POSTED 1111 DR 2130 CR
Then PARTNER_PREFUND_CREDIT 10000000
When disbursement gross=2000000 fee=20000
Then DISBURSEMENT_DEBIT and 3700=0
```

### Scenario DIS-E02: Disburse without prefund

```gherkin
Given PARTNER available=0
When disbursement gross=500000
Then reject insufficient 2130/available
```

### Scenario DIS-E03: Partial disburse batch

```gherkin
Given batch 20 beneficiaries
When 3 fail bank
Then 17 succeed summary event
```

### Scenario DIS-E04: Idempotent prefund

```gherkin
Given "pf-2" prefund POSTED
When replay prefund command
Then no second 2130 CR
```

### Scenario DIS-E05: 4150 fee on disburse

```gherkin
Given disburse with fee line
When POSTED
Then 4150 CR per foundation §15
```

### Scenario DIS-E06: Pre-fund reversal ops

```gherkin
Given ops reverse prefund journal
Then 2130 reduced and PARTNER debit policy
```

### Scenario DIS-E07: Same partner concurrent batches

```gherkin
Given two batches same partner different businessRef
When both run
Then serialize or sufficient 2130 for both
```

### Scenario DIS-E08: Beneficiary id sub-key

```gherkin
Given disburse "dis-1:ben-42"
When retry
Then idempotent single leg
```

### Scenario DIS-E09: Gross exceeds prefund remainder

```gherkin
Given PARTNER available=100000
When disburse gross=200000
Then reject
```

### Scenario DIS-E10: 3700 non-zero blocked

```gherkin
Given incomplete disburse lines
When post guard runs
Then reject 3700 != 0
```

---

## Feature: EOD settlement — extended (EOD-E)

### Scenario EOD-E01: Happy lock split settle

```gherkin
Given merchant 2120=200000 pending
When EOD matched file
Then 3800/3810/3820 net zero and 2120 cleared
```

### Scenario EOD-E02: MDR 4140 from 3820

```gherkin
Given MDR 2000 on 200000 gross
When EOD POSTED
Then 4140 CR 2000 and merchant net via 3810
```

### Scenario EOD-E03: File mismatch block

```gherkin
Given file total != 2120
When EOD validator
Then settlement blocked exception queue
```

### Scenario EOD-E04: Exception reverse to 2120

```gherkin
Given mismatch detected post-lock
When exception branch runs
Then 3800 reversed and 2120 restored per §16
```

### Scenario EOD-E05: Idempotent same merchant date

```gherkin
Given EOD completed merchant 200001 date 2026-06-01
When job re-run
Then no double bank out
```

### Scenario EOD-E06: Outbound bank fail 3810 hold

```gherkin
Given settlement POSTED lock split
When bank out fails
Then amount remains 3810 retry next cycle
```

### Scenario EOD-E07: MERCHANT_SETTLE_CREDIT optional

```gherkin
Given product enables wallet EOD credit
When EOD completes
Then MERCHANT_SETTLE_CREDIT idempotent settlement ref
```

### Scenario EOD-E08: Multi-merchant file

```gherkin
Given file 100 merchants
When all match 2120
Then each lock settle independent keys
```

### Scenario EOD-E09: Zero settlement day

```gherkin
Given merchant no 2120 balance
When EOD runs
Then skip no empty lock
```

### Scenario EOD-E10: Napas 5100 on settlement out

```gherkin
Given settlement bank leg
When POSTED
Then 5100 on outflow per foundation §16
```

### Scenario EOD-E11: Partial file one merchant missing

```gherkin
Given file missing merchant X row
When validator runs
Then block entire batch or isolate X per policy
```

### Scenario EOD-E12: 2120 vs wallet optional recon

```gherkin
Given MERCHANT_SETTLE_CREDIT enabled
When EOD credit issued
Then wallet available increase matches net settlement
```

---

## Feature: Adjustment / reversal — extended (ADJ-E)

### Scenario ADJ-E01: Manual reversal linked

```gherkin
Given POSTED "orig-1"
When ops reversal approved
Then reverses_id set and lines mirrored
```

### Scenario ADJ-E02: Wallet adjustment after wrong credit

```gherkin
Given wrong DEPOSIT_CREDIT
When ops ADJUSTMENT_DEBIT
Then available reduced matching reversal net
```

### Scenario ADJ-E03: Cannot reverse PENDING deposit B only

```gherkin
Given PENDING phase A
When reverse called
Then reverse 1111/3100 only
```

---

## Feature: Ledger invariant CI (ADR-031)

Structural checks run in CI/nightly — scenarios describe expected pass/fail outcomes.

### Scenario INV-01: Balanced POSTED journal passes

```gherkin
Given POSTED deposit "inv-dep" with DR=CR per foundation
When invariant job INV-01 runs
Then pass zero violations
```

### Scenario INV-02: Unbalanced POSTED fails CI

```gherkin
Given fixture POSTED journal sum DR != sum CR
When INV-01 runs
Then fail with coa_trans_id in report
And merge gate blocked
```

### Scenario INV-03: Stuck transit 3100 on POSTED fails

```gherkin
Given POSTED deposit with 3100 net non-zero
When INV-03 runs
Then fail transit 3100 violation
```

### Scenario INV-04: Duplicate business_ref POSTED fails

```gherkin
Given two POSTED rows same business_ref and use_case
When INV-02 runs
Then fail duplicate key violation
```

### Scenario INV-05: W2 snapshot without wallet_tx fails

```gherkin
Given wallet_balance changed without wallet_tx in same TX window
When INV-05 spot-check runs
Then fail W2 violation
```

### Scenario INV-06: Nightly W5 drift over tolerance

```gherkin
Given SUM user 2110 drift beyond tolerance 2h
When nightly INV-07 runs
Then alert severity high
And no auto COA write
```

---

## Feature: Balance monitoring (ADR-032)

### Scenario MON-01: Negative available critical alert

```gherkin
Given wallet available=-1 after data corruption fixture
When balance monitor runs
Then critical alert with wallet_id
And no auto adjustment
```

### Scenario MON-02: Frozen LOCKED wallet aging

```gherkin
Given wallet LOCKED frozen=100000 for 25h
When monitor MON-02 runs
Then high severity alert
```

### Scenario MON-03: POSTED credit lag precursor

```gherkin
Given POSTED "mon-lag" 20m ago no wallet_tx
When MON-05 runs
Then medium alert lists business_ref candidate
And two-query correlation without cross-schema JOIN
```

### Scenario MON-04: Alert dedup within 1h

```gherkin
Given MON-01 fired for wallet W1
When MON-01 condition persists 30m later
Then no duplicate page within dedup window
```

### Scenario MON-05: Monitor job failure non-blocking

```gherkin
Given monitor worker down
When user payment attempted
Then payment path unaffected
And monitor failure logged separately
```

---

## Feature: Bank poll configuration (ADR-033)

### Scenario POLL-01: UNKNOWN does not RELEASE

```gherkin
Given withdraw frozen=50000 bank status UNKNOWN
When poll worker runs with T2 elapsed
Then frozen unchanged
And no RELEASE command
```

### Scenario POLL-02: T2 spacing respected

```gherkin
Given bank.poll.interval_seconds=300
When first poll at T0
Then second poll not before T0+300s
```

### Scenario POLL-03: Tmax alert without auto RELEASE

```gherkin
Given frozen age exceeds wallet.frozen.alert_age_hours
When aging job runs
Then high severity alert
And frozen still 50000 until terminal bank status
```

### Scenario POLL-04: Terminal SUCCESS SETTLE once

```gherkin
Given poll returns SUCCESS after prior TIMEOUT
When SETTLE sub-key "wd-poll:settle"
Then single settle wallet_tx
And idempotent replay unchanged
```

### Scenario POLL-05: max_attempts to DLQ

```gherkin
Given bank returns 504 for max_attempts
When poll budget exhausted
Then message DLQ
And frozen held for manual ops
```

---

## Feature: LOCKED deposit credit — extended (ADR-034)

### Scenario LCK-D01: Unlock then retry credit

```gherkin
Given LOCKED wallet POSTED deposit "lck-d" uncredited
When ops sets ACTIVE
And aging retries WALLET_CREDIT
Then single DEPOSIT_CREDIT wallet_tx
And available increased net amount
```

### Scenario LCK-D02: Duplicate retry after unlock idempotent

```gherkin
Given unlock retry succeeded "lck-d"
When WALLET_CREDIT delivered again
Then no second wallet_tx
```

### Scenario LCK-D03: W5 timing drift until unlock expected

```gherkin
Given LOCKED uncredited POSTED deposit
When W5 recon within 15m tolerance
Then log timing drift not high alert
When still uncredited 2h after unlock retry fails
Then high alert per ADR-014
```

---

## Feature: Accrual basis ledger v1 (ADR-036)

### Scenario ACC-E01: Bank webhook only — no cash-basis wallet credit

```gherkin
Given bank deposit webhook received gross=100000
And phase A PENDING journal created 3100 CR 100000
When no phase B confirm yet
Then wallet available unchanged
And 2110 not credited on POSTED ledger
```

### Scenario ACC-E02: POSTED before bank settlement complete

```gherkin
Given deposit "acc-2" phase B POSTED net=99000
And bank settlement still PENDING on rails
Then 2110 CR 99000 on ledger
And wallet credit path may proceed after POSTED
```

### Scenario ACC-E03: Withdraw freeze on accept not on bank debit

```gherkin
Given withdraw accept POSTED 3200 transit cleared on ledger
And bank payout not yet terminal
Then wallet frozen=50000
And available reduced before bank SETTLE
```

### Scenario ACC-E04: Fee matched same POSTED journal

```gherkin
Given deposit POSTED gross=100000 fee=1000
Then 4110 CR 1000 same coa_trans as 2110 movement
And orchestration single fee source ADR-009
```

### Scenario ACC-E05: UI deposit processing not wallet column

```gherkin
Given deposit PENDING saga state
When client queries balance API
Then spendable equals wallet.available only
And optional label deposit processing from saga not wallet_balance
```

---

## Feature: Async workers v1 (ADR-035)

### Scenario WRK-01: Saga resume after worker crash

```gherkin
Given saga step 3 of withdraw in progress
When worker process killed
And message redelivered
Then resume from DB saga state
And no double freeze
```

### Scenario WRK-02: Outbox duplicate delivery

```gherkin
Given outbox relay delivers WITHDRAW_SETTLE twice
When both consumed
Then single settle effect
```

### Scenario WRK-03: No Temporal in deployment

```gherkin
Given v1 platform topology
When dependency graph validated
Then no temporal-sdk or temporal service required
```

---

## Scenario index (Part I + II + III + ref gaps)

| Feature | Part I | Part II | Part III | Total | v1 gate (150) |
|---------|--------|---------|----------|-------|---------------|
| Balance semantics | 5 | — | — | 5 | ✓ |
| Deposit | 18 | 15 | — | 33 | ✓ |
| Withdraw | 12 | 10 | — | 22 | ✓ |
| Payment | 10 | 10 | — | 20 | ✓ |
| Transfer | 6 | 6 | — | 12 | ✓ |
| IBFT | 5 | 5 | — | 10 | ✓ |
| QR/POS | 2 | — | 10 | 12 | Part I only |
| Payroll | 2 | — | 10 | 12 | Part I only |
| Disbursement | 2 | — | 10 | 12 | Part I only |
| EOD | 3 | — | 12 | 15 | Part I only |
| Adjustment | 1 | — | 3 | 4 | Part I only |
| Cross-cutting | 10 | 13 | — | 23 | ✓ |
| Ledger invariant CI (ADR-031) | — | — | 6 | 6 | ✓ (6) |
| Balance monitoring (ADR-032) | — | — | 5 | 5 | ✓ (5) |
| Bank poll (ADR-033) | — | — | 5 | 5 | ✓ (4 of 5) |
| LOCKED deposit (ADR-034) | — | — | 3 | 3 | stretch |
| Accrual basis (ADR-036) | — | — | 5 | 5 | stretch |
| Async workers (ADR-035) | — | — | 3 | 3 | stretch |
| **Total** | **75** | **60** | **72** | **207** | **150** |

---

## Traceability

| Feature | Accounting | Wallet | Orchestration |
|---------|------------|--------|---------------|
| Balance semantics | — | wallet.md §2.1 | orchestration.md §24 |
| Deposit | accounting.md §14 | wallet.md §15 | orchestration.md §11 |
| Payment | accounting.md §16 | wallet.md §16 | orchestration.md §12 |
| Transfer | accounting.md §17 | wallet.md §17 | orchestration.md §13 |
| Withdraw | accounting.md §15 | wallet.md §18 | orchestration.md §14 |
| IBFT | accounting.md §18 | wallet.md §19 | orchestration.md §15 |
| QR/POS | accounting.md §19 | wallet.md §24 | orchestration.md §16 |
| Payroll | accounting.md §20 | wallet.md §20.1 | orchestration.md §17 |
| Disbursement | accounting.md §21 | wallet.md §20.2 | orchestration.md §17 |
| EOD | accounting.md §22 | wallet.md §25 | orchestration.md §16 |
| Cross-cutting | accounting.md §23 | wallet.md §21, §26 | orchestration.md §18, §23 |

---

## Feature: USER multi-pocket wallets (ADR-040)

Maps [ADR-040](../adr/ADR-040-user-multi-pocket-wallets.md) AC-040 / TC-040 and
[`../spec/processes.md`](../spec/processes.md) §11A. USER-only; MERCHANT/PARTNER single.
All USER pockets roll up to control **2110** (no ledger change for create/close).

### Scenario: Create pocket from catalog (TC-040-01)

```gherkin
Given USER 100234 with only the 'default' pocket
And pocket def "SAVINGS" is active
When createPocket businessRef="pk-1" pocketCode="SAVINGS" label="Tiết kiệm"
Then a new wallet row exists for member 100234 label="Tiết kiệm" pocket_code="SAVINGS"
And its wallet_balance available=0 frozen=0
And no coa_trans is posted
And listPockets returns both 'default' and "Tiết kiệm"
```

### Scenario: Duplicate label rejected (TC-040-02)

```gherkin
Given USER 100234 already has a pocket label="Tiết kiệm"
When createPocket businessRef="pk-2" pocketCode="GOAL" label="Tiết kiệm"
Then rejected WALLET_DUPLICATE_CONFLICT (409)
And no new wallet row is created
```

### Scenario: multi_allowed=false blocks a second pocket (TC-040, AC-040-09)

```gherkin
Given pocket def "SPENDING" has multi_allowed=false
And USER 100234 already has a "SPENDING" pocket
When createPocket businessRef="pk-3" pocketCode="SPENDING" label="Chi tiêu 2"
Then rejected WALLET_POCKET_EXISTS (409)
```

### Scenario: GOAL allows multiple pockets

```gherkin
Given pocket def "GOAL" has multi_allowed=true
When USER 100234 creates "GOAL" label="Du lịch" and "GOAL" label="Khẩn cấp"
Then both pockets exist with distinct labels
```

### Scenario: MERCHANT cannot hold a second wallet (AC-040-03)

```gherkin
Given MERCHANT 555 has one MERCHANT/VND wallet
When a second MERCHANT/VND wallet is attempted for member 555
Then rejected by partial unique index (single wallet per non-USER member/type/currency)
```

### Scenario: Pocket-to-pocket transfer — total unchanged, 2110 net zero (TC-040-05/06)

```gherkin
Given USER 100234 pocket A available=100000 and pocket B available=0
When createPocketTransfer businessRef="pt-1" fromWalletId=A toWalletId=B amount=60000
Then A available=40000 and B available=60000
And member total across pockets unchanged
And ledger posts 2110 DR 60000 (A leg) and 2110 CR 60000 (B leg), transit 3300 net zero
And wallet_tx A TRANSFER_DEBIT 60000 and wallet_tx B TRANSFER_CREDIT 60000
And only pockets A and B are locked
```

### Scenario: Pocket transfer insufficient funds

```gherkin
Given pocket A available=50000
When createPocketTransfer fromWalletId=A toWalletId=B amount=80000
Then rejected WALLET_INSUFFICIENT_BALANCE (422)
And A and B balances unchanged
```

### Scenario: Pocket transfer same source and destination

```gherkin
When createPocketTransfer fromWalletId=A toWalletId=A amount=1000
Then rejected WALLET_INVALID_TRANSFER (409)
```

### Scenario: Balance defaults to default pocket (AC-040-05, backward compat)

```gherkin
Given USER 100234 has pockets 'default' and "Tiết kiệm"
When getWalletBalance with no walletId and no pocketCode
Then the 'default' pocket available/frozen is returned
```

### Scenario: Balance targets a named pocket

```gherkin
Given USER 100234 pocket B (label "Tiết kiệm") available=60000
When getWalletBalance walletId=B
Then available=60000 for pocket B only
And other pockets are not read
```

### Scenario: Close empty pocket (TC-040-07)

```gherkin
Given USER pocket B available=0 frozen=0 and B is not the default pocket
When closePocket walletId=B businessRef="cl-1"
Then B status=CLOSED
And B rejects further debit/credit/freeze
And no coa_trans is posted
```

### Scenario: Close non-empty pocket rejected

```gherkin
Given USER pocket B available=60000
When closePocket walletId=B
Then rejected WALLET_POCKET_NOT_EMPTY (409)
And B status unchanged
```

### Scenario: Close default pocket rejected (AC-040-07)

```gherkin
Given the 'default' pocket of USER 100234
When closePocket on the default pocket
Then rejected (default pocket not user-deletable)
```

### Scenario: Backward compatibility — legacy client unaffected (quickstart Q10)

```gherkin
Given an existing USER whose single wallet was migrated to the 'default' pocket
And a client that sends no pocket fields
When it calls getWalletBalance and createPayment as before
Then operations resolve to the 'default' pocket
And behavior is identical to the pre-pocket system
```

### Scenario: Reconciliation — USER pockets sum to 2110 (AC-040-04, SC-005)

```gherkin
Given many USER members each with multiple pockets
When W5 reconciliation runs
Then SUM(available+frozen) over all USER pockets equals control 2110 within tolerance
And it is report-only (no auto-adjust)
```

### Scenario: Idempotent create and transfer replay (FR-011, SC-004)

```gherkin
Given createPocket "pk-1" and createPocketTransfer "pt-1" already succeeded
When each is replayed with the same businessRef
Then createPocket returns the existing walletId without a new row
And the transfer returns the original result without double-debit
```

---

# Part IV — ADR conformance index

Maps [`adr/`](../adr/README.md) **TC** rows to Gherkin in this file. Structural TC (module graph, DDL) are manual/CI — not Gherkin.

| ADR | AC | TC | Primary Gherkin features |
|-----|----|----|--------------------------|
| [ADR-001](../adr/ADR-001-immutable-ledger.md) | AC-001 (7) | TC-001 (8) | Deposit (Phase A/B), Adjustment ADJ-E, X-E03/04 |
| [ADR-002](../adr/ADR-002-core-foundation-shared-library.md) | AC-002 (6) | TC-002 (5) | *(structural)* + orchestration §9 F1 |
| [ADR-003](../adr/ADR-003-dual-schema-single-postgres.md) | AC-003 (7) | TC-003 (7) | Reconciliation W5, Payment compensate, Idempotency |
| [ADR-004](../adr/ADR-004-wallet-balance-snapshot.md) | AC-004 (9) | TC-004 (9) | Balance semantics, Withdraw, X-E11, ADJ-E02, as-of balance |
| [ADR-005](../adr/ADR-005-idempotency-key-strategy.md) | AC-005 (10) | TC-005 (11) | Idempotency conflicts, Payment/Transfer legs, Withdraw/IBFT sub-keys, X-E02/07 |
| [ADR-006](../adr/ADR-006-two-phase-deposit.md) | AC-006 (7) | TC-006 (6) | Deposit Phase A/B, Balance semantics PENDING |
| [ADR-007](../adr/ADR-007-freeze-settle-async-outflow.md) | AC-007 (8) | TC-007 (7) | Withdraw, IBFT, Balance semantics, X-E12/13 |
| [ADR-008](../adr/ADR-008-saga-compensation-no-2pc.md) | AC-008 (7) | TC-008 (7) | Payment compensate, Transfer, X-E07/08 |
| [ADR-009](../adr/ADR-009-fee-ownership-orchestration.md) | AC-009 (7) | TC-009 (6) | Deposit/Payment/Transfer/Withdraw fee amounts |
| [ADR-010](../adr/ADR-010-transit-accounts-net-zero.md) | AC-010 (9) | TC-010 (9) | All POSTED flow features (transit net zero) |
| [ADR-031](../adr/ADR-031-sql-ledger-invariant-ci.md) | AC-031 (5) | TC-031 (6) | Ledger invariant CI INV-01…06 |
| [ADR-032](../adr/ADR-032-wallet-balance-monitoring.md) | AC-032 (6) | TC-032 (6) | Balance monitoring MON-01…05, out-of-band eval |
| [ADR-033](../adr/ADR-033-bank-poll-t2-frozen-tmax.md) | AC-033 (5) | TC-033 (5) | Bank poll POLL-01…05, X-E13 |
| [ADR-034](../adr/ADR-034-locked-wallet-deposit-credit-reject.md) | AC-034 (5) | TC-034 (4) | Wallet lock, DEP-E11, LCK-D01…03 |
| [ADR-035](../adr/ADR-035-rabbitmq-workers-not-temporal-v1.md) | AC-035 (5) | TC-035 (4) | WRK-01…03, DEP-E06/08, X-E07 |
| [ADR-036](../adr/ADR-036-accrual-basis-ledger-v1.md) | AC-036 (6) | TC-036 (6) | Accrual basis ACC-E01…05, Deposit/Withdraw |
| [ADR-037](../adr/ADR-037-tigerbeetle-ledger-backing-store.md) | AC-037-01…05 | TC TBD at implement | `core.accounting` wraps TB; orch không gọi TB |
| [ADR-038](../adr/ADR-038-orchestrator-separate-service-gateway-seam.md) | AC-038 (6) | TC-038 (5) | *(structural)* gateway seam, in-process→HTTP swap, saga over network |
| [ADR-039](../adr/ADR-039-no-synchronous-wallet-aggregate-row.md) | AC-039 (5) | TC-039 (4) | Per-wallet concurrency, async lane rollup, W5 unchanged |
| [ADR-040](../adr/ADR-040-user-multi-pocket-wallets.md) | AC-040 (9) | TC-040 (7) | USER multi-pocket, label/`pocket_def`, single non-USER, pocket transfer |
| 011–030 | see [`adr/README.md`](../adr/README.md) | per ADR | Auth X-E09/10, QR, EOD, Payroll, IBFT-E05, W5, … |

**ADR 011–030 summary:** 011 auth JWT · 012 F-rules · 013 outbox · 014 W5 · 015 EOD batch · 016 QR default · 017 partial batch · 018 wire specs · 019 VND · 020 lanes · 021 aging · 022 mTLS · 023 period close · 024 deposit paths · 025 IBFT 1112 · 026 wallet≠ledger · 027 3-commit pay · 028 scale 4 · 029 LOCKED · 030 VA map.

**ADR 031–039 (reference gaps + arch):** 031 SQL invariant CI · 032 balance monitors (out-of-band) · 033 bank T2/Tmax · 034 LOCKED deposit reject · 035 RabbitMQ not Temporal · **036 accrual-like ledger v1** · 037 TigerBeetle (deferred) · 038 orchestrator separate service / gateway seam · **039 no synchronous wallet aggregate row** · **040 USER multi-pocket wallets**.

### Feature: ADR-001 immutable ledger (ADR-001)

```gherkin
# TC-001-02 / AC-001-02 — covered by ADJ-E01, X-E04
Given POSTED journal "orig-1"
When ops reversal approved
Then new journal with reverses_id
And original coa_trans_data rows unchanged
```

### Feature: ADR-004 wallet snapshot (ADR-004)

```gherkin
# TC-004-02 / AC-004-01 — insufficient debit does not touch snapshot incorrectly
Given available=1000
When debit amount=2000
Then WALLET_INSUFFICIENT_BALANCE
And available=1000
And wallet_tx count unchanged
```

### Feature: ADR-004 as-of historical balance (ADR-004)

```gherkin
# TC-004-09 / AC-004-09 — balance at time T from wallet_tx after-snapshot
Given wallet_tx for wallet "w-1": mvt1 @T1 available_after=99000, mvt2 @T2 available_after=80000, mvt3 @T3 available_after=120000
When query as-of balance at T where T2 <= T < T3
Then return available=80000 frozen=mvt2.frozen_after
And query reads only wallet_tx (no coa_* JOIN)
And query before T1 returns available=0 frozen=0
```

### Feature: ADR-005 idempotency (ADR-005)

```gherkin
# TC-005-06 / AC-005-06 — withdraw settle sub-key
Given WITHDRAW_SETTLE completed for "wd-5" sub-key "wd-5:settle"
When SETTLE replayed with same sub-key
Then frozen unchanged
And idempotent wallet_tx_id returned
```

### Feature: ADR-032 monitor out-of-band (ADR-032)

```gherkin
# TC-032-06 / AC-032-06 — monitor evaluation never on the write path
Given a wallet debit transaction commits
When the wallet write transaction is inspected
Then it contains no balance-monitor query or alert call
And MON-04 lane spike is evaluated from the async rollup, not inline
```

### Feature: ADR-039 no synchronous wallet aggregate row (ADR-039)

```gherkin
# TC-039-01 / AC-039-01 — per-wallet concurrency, no shared aggregate lock
Given two members "m-1" and "m-2" each with a wallet
When both wallets are debited concurrently
Then neither waits on a shared aggregate row lock
And no shared "total wallet balance" row is updated in either transaction
And the authoritative aggregate remains COA 2110/2120/2130
```

**Run order for release gate:** TC-001…TC-010 Gherkin where mapped + TC-002/003 structural checks in CI.
