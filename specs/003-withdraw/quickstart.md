# Quickstart: Withdraw Validation

**Feature**: `003-withdraw` | **Date**: 2026-06-19

Use this guide to validate the withdraw flow end-to-end at design level.

---

## Q1 — Accept happy path

**Setup**: member has `wallet_balance.available = 500,000`. Submit `createWithdrawal` with `principal=100,000`, `fee=1,000`, `businessRef="wdr-q1"`.

**Expected**:
1. `wallet_balance.available = 399,000` (− 101,000)
2. `wallet_balance.frozen = 101,000` (+ 101,000)
3. `wallet_tx` row: `tx_type=WITHDRAW_FREEZE`, `direction=FREEZE`, `amount=101,000`, `business_ref="wdr-q1"`
4. `coa_trans`: `status=PENDING`, `use_case=WITHDRAW`, `reference_id="wdr-q1"`
5. TB Account 2110: pending debit 101,000
6. TB Account 3200: pending credit 101,000
7. HTTP 200 returned before payout starts
8. Outbox row: `command_type=WITHDRAW_PAYOUT`, `status=PENDING`

---

## Q2 — Insufficient balance

**Setup**: member has `wallet_balance.available = 50,000`. Submit `createWithdrawal` with `gross=101,000`.

**Expected**:
- HTTP 422 `WALLET_INSUFFICIENT_BALANCE`
- No `wallet_tx` row
- No `coa_trans` row
- No outbox row

---

## Q3 — Settle (bank success)

**Setup**: Q1 completed, `WITHDRAW_PAYOUT` delivered to payout worker, bank responds SUCCESS.

**Expected**:
1. TB: `post_pending_transfer(hash("wdr-q1:withdrawA"))` → 2110/3200 pending closed
2. TB Transfer `hash("wdr-q1:1111")`: 3200 DR 100,000 / 1111 CR 100,000
3. TB Transfer `hash("wdr-q1:4120")`: 3200 DR 1,000 / 4120 CR 1,000
4. `account[3200].balance = 0` ✓
5. `coa_trans.status = POSTED`
6. `wallet_tx` row: `tx_type=WITHDRAW_SETTLE`, `direction=DEBIT`, `amount=101,000`, `business_ref="wdr-q1:settle"`
7. `wallet_balance.frozen = 0` (− 101,000)
8. `wallet_balance.available` unchanged

---

## Q4 — Release (bank fail)

**Setup**: Q1 completed, bank responds FAILED.

**Expected**:
1. TB: `void_pending_transfer(hash("wdr-q1:withdrawA"))` → 2110/3200 cleared
2. `account[3200].balance = 0` ✓
3. `coa_trans.status = FAILED`
4. `wallet_tx` row: `tx_type=WITHDRAW_RELEASE`, `direction=UNFREEZE`, `amount=101,000`, `business_ref="wdr-q1:release"`
5. `wallet_balance.frozen = 0` (− 101,000)
6. `wallet_balance.available = 500,000` (restored)

---

## Q5 — Double-spend protection (timeout ≠ release)

**Setup**: Q1 completed, bank returns no response for T2 seconds.

**Expected**:
- `wallet_balance.frozen = 101,000` — funds remain frozen
- No WITHDRAW_RELEASE issued
- Payout worker polls bank (ADR-033) until terminal result
- After Tmax: severity alert to ops; no auto-release

---

## Q6 — Idempotent accept (duplicate businessRef)

**Setup**: Q1 completed (full success). Re-submit `createWithdrawal` with same `businessRef="wdr-q1"`.

**Expected**:
- HTTP 200 (same response body)
- No second `wallet_tx` row for WITHDRAW_FREEZE
- No second `coa_trans` row
- No second outbox row

---

## Q7 — LOCKED wallet rejects freeze

**Setup**: member wallet `status=LOCKED`. Submit `createWithdrawal`.

**Expected**:
- HTTP 422 (wallet rejects freeze — ADR-029)
- No `coa_trans` row
- No outbox row

---

## Invariant cross-check (all Q scenarios)

After every completed withdraw (settled or released):

| Invariant | Check |
|-----------|-------|
| `account[3200].balance = 0` | Query TB account 3200 |
| `SUM(DR) = SUM(CR)` per journal | Query `coa_trans_data` or TB read-model |
| One FREEZE per `businessRef` | `SELECT COUNT(*) FROM wallet_tx WHERE business_ref='wdr-q1' AND tx_type='WITHDRAW_FREEZE'` = 1 |
| One terminal per `businessRef` | Either SETTLE or RELEASE, never both |
| No SETTLE + RELEASE for same ref | Idempotency keys `{ref}:settle` and `{ref}:release` are mutually exclusive |
