# Quickstart: Async Deposit Validation Guide

**Feature**: `002-async-deposit` | **Date**: 2026-06-18

This guide maps to the acceptance scenarios in `design-v2/acceptance.md` (Deposit feature). Each scenario can be run against the sandbox or integration test suite.

---

## Prerequisites

- TigerBeetle sandbox running: `cd sandbox/tigerbeetle && ./stack.sh start`
- PostgreSQL with `wallet` + `accounting` schemas migrated (Flyway)
- RabbitMQ running with exchange `core.commands` and queue `core.commands.bank-deposit`
- Seed: COA accounts (1111, 3100, 2110, 4110) provisioned in both Postgres and TB

---

## Q1 — Deposit happy path (Phase A → Phase B → wallet credit)

**Scenario**: A deposit notification arrives for a mapped VA; wallet balance increases by net amount.

**Steps**:
1. Send `POST /deposits/notify` with `virtualAccount=VA-001`, `grossAmount=100000.0000`, `businessRef=DEP-001`
2. Assert response: `202 Accepted` + `businessRef=DEP-001`
3. Assert `coa_trans.status = PENDING` where `reference_id = DEP-001`
4. Assert `wallet_balance.available` **unchanged** at this point
5. Allow accounting worker to process `BANK_DEPOSIT` message
6. Assert `coa_trans.status = POSTED`
7. Assert TB `account[3100].debits_posted - account[3100].credits_posted = 0` (transit zero)
8. Allow wallet worker to process `WALLET_CREDIT` message
9. Assert `wallet_balance.available` increased by `99000.0000` (net of 1000.0000 fee)
10. Assert `wallet_tx` row exists: `tx_type=DEPOSIT_CREDIT`, `amount=99000.0000`

**Maps to**: AC-006-01 through AC-006-07, TC-006-01, TC-006-02, TC-041-01

---

## Q2 — Idempotency: duplicate deposit notification

**Scenario**: Same `businessRef` delivered twice — single credit only.

**Steps**:
1. Send `POST /deposits/notify` with `businessRef=DEP-002` → 202
2. Send same request again with `businessRef=DEP-002` → 202 (idempotent ack)
3. Allow both `BANK_DEPOSIT` messages to process
4. Assert `coa_trans` count = 1 for `reference_id=DEP-002`
5. Assert `wallet_tx` count = 1 for `(wallet_id, business_ref=DEP-002, tx_type=DEPOSIT_CREDIT)`

**Maps to**: AC-006-06, TC-006-04, AC-041-03, TC-041-02

---

## Q3 — Unknown virtual account → ops hold

**Scenario**: Deposit arrives for VA with no member mapping.

**Steps**:
1. Send `POST /deposits/notify` with `virtualAccount=VA-UNKNOWN`, `businessRef=DEP-003` → 202
2. Allow accounting worker to process `BANK_DEPOSIT`
3. Assert **no** `coa_trans` row created for `reference_id=DEP-003`
4. Assert `wallet_balance.available` unchanged
5. Assert deposit logged in ops hold queue / audit log

**Maps to**: AC-030-01, AC-030-02, TC-030-01, TC-030-02

---

## Q4 — Deposit cancel: phase-A reversal

**Scenario**: Deposit enters PENDING but bank cancels → transit 3100 nets to zero, wallet unchanged.

**Steps**:
1. Create a PENDING deposit (Q1 steps 1–4)
2. Trigger reversal (bank cancel event or ops manual reversal command)
3. Assert `coa_trans.status = FAILED` (or reversal journal created)
4. Assert TB: reversal transfers clear 1111/3100 only — 2110 and 4110 **not touched**
5. Assert `wallet_balance.available` unchanged throughout

**Maps to**: AC-006-05, TC-006-05

---

## Q5 — Accounting worker crash + redelivery

**Scenario**: Worker crashes after Phase A, before Phase B; RabbitMQ redelivers.

**Steps**:
1. Inject fault: worker crashes after `createJournal` PENDING, before `confirmDeposit`
2. Restart worker; RabbitMQ redelivers `BANK_DEPOSIT` message
3. Assert Phase A journal not duplicated (idempotent `createJournal` on same `reference_id`)
4. Assert `confirmDeposit` proceeds to POSTED
5. Assert wallet credit occurs exactly once

**Maps to**: AC-041-02, AC-041-03, TC-041-03

---

## Q6 — TigerBeetle transit invariant verification

**Scenario**: After any POSTED deposit, TB `account[3100]` net = 0.

**Steps**:
1. Run any successful deposit (Q1)
2. Query TB: `account[id=hash("3100")].debits_posted - credits_posted`
3. Assert result = 0

**Maps to**: AC-037-03, INV-03 (SQL invariant CI), SC-004

---

## Q7 — Blnk PoC: balance monitor fires on threshold

**PoC only — not production path**

**Steps**:
1. Register monitor: `walletId=W-001`, `threshold=50000.0000`, `direction=ABOVE`
2. Execute deposit so `available` crosses 50000.0000
3. Assert `WalletBalanceEvent` fired with `currentAvailable > 50000.0000`

---

## Q8 — Blnk PoC: historical balance query

**PoC only — not production path**

**Steps**:
1. Record timestamp `T1` before deposit
2. Execute deposit (Q1)
3. Record timestamp `T2` after wallet credit
4. Call `getBalanceAt(walletId, T1)` → assert returns balance **before** deposit
5. Call `getBalanceAt(walletId, T2)` → assert returns balance **after** deposit
