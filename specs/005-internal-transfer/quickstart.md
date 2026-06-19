# Quickstart: Internal Transfer Validation

**Feature**: `005-internal-transfer` | **Date**: 2026-06-19

Use this guide to validate the internal transfer flow end-to-end at design level.

---

## Q1 — Happy path transfer

**Setup**: Sender has `wallet_balance.available = 500,000`. Receiver has `wallet_balance.available = 200,000`. Submit `POST /transfers` with `principal=100,000`, `fee=500`, `businessRef="tfr-q1"`.

**Expected**:
1. Sender `wallet_balance.available = 399,500` (− 100,500 gross)
2. Receiver `wallet_balance.available = 300,000` (+ 100,000 net)
3. Sender `wallet_tx` row: `tx_type=TRANSFER_DEBIT`, `direction=DEBIT`, `amount=100,500`, `business_ref="tfr-q1"`
4. Receiver `wallet_tx` row: `tx_type=TRANSFER_CREDIT`, `direction=CREDIT`, `amount=100,000`, `business_ref="tfr-q1"`
5. `coa_trans`: `status=POSTED`, `use_case=TRANSFER`, `reference_id="tfr-q1"`
6. TB Transfer `hash("tfr-q1:debit")`: 2110_sender DR 100,500 / 3300 CR 100,500
7. TB Transfer `hash("tfr-q1:credit")`: 3300 DR 100,000 / 2110_receiver CR 100,000
8. TB Transfer `hash("tfr-q1:4130")`: 3300 DR 500 / 4130 CR 500
9. `account[3300].balance = 0` ✓ (100,500 − 100,000 − 500 = 0)
10. HTTP 200 returned with `status=COMPLETED`

---

## Q2 — Insufficient balance

**Setup**: Sender has `wallet_balance.available = 50,000`. Submit `POST /transfers` with `gross=100,500`.

**Expected**:
- HTTP 422 `WALLET_INSUFFICIENT_BALANCE`
- No `wallet_tx` rows for either party
- No `coa_trans` row
- `account[3300].balance = 0` (unchanged)

---

## Q3 — Sender equals receiver (same memberId)

**Setup**: Submit `POST /transfers` where `receiverMemberId` equals the sender's own `memberId` (from JWT).

**Expected**:
- HTTP 422 `TRANSFER_SAME_MEMBER`
- No `wallet_tx` rows
- No `coa_trans` row
- Rejection happens before any wallet or accounting call

---

## Q4 — Compensation (journal fails after sender debit)

**Setup**: Q1 scenario, but `app-accounting` returns an error on `createJournal` (step 2 fails after step 1 succeeds).

**Expected**:
1. Sender `wallet_tx` TRANSFER_DEBIT was created: `amount=100,500`, `business_ref="tfr-q1"`
2. No `coa_trans` row — journal was never created
3. TB accounts unchanged — 3 transfers were never submitted
4. `account[3300].balance = 0` ✓ (never touched)
5. Compensation: `wallet_tx` row for sender: `tx_type=ADJUSTMENT_CREDIT`, `direction=CREDIT`, `amount=100,500`, `business_ref="tfr-q1:comp"`
6. Sender `wallet_balance.available = 500,000` (fully restored)
7. Receiver `wallet_balance.available = 200,000` (never touched)
8. HTTP 500 / error returned to member

---

## Q5 — Forward retry (receiver credit fails after POSTED journal)

**Setup**: Q1 scenario, but `app-wallet` TRANSFER_CREDIT call (step 3) fails transiently. Orchestration retries.

**Expected**:
1. `coa_trans.status = POSTED` — journal committed, 3 TB transfers exist
2. `account[3300].balance = 0` ✓ — ledger already balanced
3. TB transfers NOT reversed — ADR-001 prohibits reversing POSTED journals
4. On retry: TRANSFER_CREDIT re-sent with same `businessRef`; unique constraint
   `(receiver_wallet_id, "tfr-q1", TRANSFER_CREDIT)` ensures idempotent insert
5. Receiver `wallet_balance.available = 300,000` (+ 100,000 net) — credited once
6. HTTP 200 eventually returned

---

## Q6 — Idempotent transfer (duplicate businessRef)

**Setup**: Q1 completed (full success). Re-submit `POST /transfers` with same `businessRef="tfr-q1"`.

**Expected**:
- HTTP 200 (same response body)
- No second TRANSFER_DEBIT row (unique constraint guards sender wallet)
- No second `coa_trans` row (unique constraint `(reference_id, use_case)`)
- No second TRANSFER_CREDIT row (unique constraint guards receiver wallet)
- TB rejects duplicate transfer IDs — no double-posting

---

## Q7 — Compensation idempotency (ADJUSTMENT_CREDIT retried)

**Setup**: Q4 completed (compensation issued). Orchestration retries the compensation call.

**Expected**:
- Second ADJUSTMENT_CREDIT call with `business_ref="tfr-q1:comp"` hits unique constraint
- No second wallet credit issued
- Sender `wallet_balance.available = 500,000` (restored exactly once)

---

## Invariant cross-check (all Q scenarios)

After every completed or compensated transfer:

| Invariant | Check |
|-----------|-------|
| `account[3300].balance = 0` | Query TB account 3300 after any terminal state |
| `SUM(DR) = SUM(CR)` per journal | Query `coa_trans_data` or TB read-model for `reference_id` |
| One TRANSFER_DEBIT per `businessRef` per sender | `SELECT COUNT(*) FROM wallet_tx WHERE business_ref='tfr-q1' AND tx_type='TRANSFER_DEBIT'` = 1 |
| One TRANSFER_CREDIT per `businessRef` per receiver | Same check for TRANSFER_CREDIT |
| No TRANSFER_CREDIT if ADJUSTMENT_CREDIT exists | Compensation path means receiver was never credited |
| ADJUSTMENT_CREDIT amount = TRANSFER_DEBIT amount | Gross fully restored on compensation |
