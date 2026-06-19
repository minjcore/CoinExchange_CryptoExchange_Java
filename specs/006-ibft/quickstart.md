# Quickstart: IBFT Validation

**Feature**: `006-ibft` | **Date**: 2026-06-19

Use this guide to validate the IBFT flow end-to-end at design level.

Numbers used throughout: `principal=100,000`, `platformFee=1,000`, `napasCost=500`, `gross=101,000`.

---

## Q1 — Accept happy path

**Setup**: member has `wallet_balance.available = 500,000`. Submit `POST /transfers` with `principal=100,000`, `businessRef="ibft-q1"`, external bank destination.

Orchestration computes: `platformFee=1,000`, `napasCost=500`, `gross=101,000`.

**Expected**:
1. `wallet_balance.available = 399,000` (− 101,000)
2. `wallet_balance.frozen = 101,000` (+ 101,000)
3. `wallet_tx` row: `tx_type=IBFT_FREEZE`, `direction=FREEZE`, `amount=101,000`, `business_ref="ibft-q1"`
4. `coa_trans`: `status=PENDING`, `use_case=IBFT`, `reference_id="ibft-q1"`
5. TB Account 2110: pending debit 101,000
6. TB Account 3400: pending credit 101,000
7. HTTP 200 returned before payout starts
8. Outbox row: `command_type=IBFT_PAYOUT`, `status=PENDING`

> `napasCost` is carried in the `IbftPayoutCommand` payload but does NOT affect wallet or TB at accept time.

---

## Q2 — Insufficient balance

**Setup**: member has `wallet_balance.available = 50,000`. Submit `POST /transfers` with `gross=101,000`.

**Expected**:
- HTTP 422 `WALLET_INSUFFICIENT_BALANCE`
- No `wallet_tx` row
- No `coa_trans` row
- No outbox row

---

## Q3 — Settle (Napas success)

**Setup**: Q1 completed, `IBFT_PAYOUT` delivered to payout worker, Napas responds SUCCESS.

**Expected**:
1. TB: `post_pending_transfer(hash("ibft-q1:ibftA"))` → 2110/3400 pending closed
2. TB Transfer `hash("ibft-q1:4130")`: 3400 DR 1,000 / 4130 CR 1,000 (platform fee)
3. TB Transfer `hash("ibft-q1:1112")`: 3400 DR 100,000 / 1112 CR 100,000 (principal out)
4. TB Transfer `hash("ibft-q1:5100")`: 5100 DR 500 / 1112 CR 500 (Napas cost)
5. `account[3400].balance = 0` ✓
6. `coa_trans.status = POSTED`
7. `wallet_tx` row: `tx_type=IBFT_SETTLE`, `direction=DEBIT`, `amount=101,000`, `business_ref="ibft-q1:settle"`
8. `wallet_balance.frozen = 0` (− 101,000)
9. `wallet_balance.available` unchanged

> Transfer 4 (5100/1112) does NOT touch account 3400. Net platform result: fee revenue 1,000 − Napas cost 500 = +500 on 1112.

---

## Q4 — Release (Napas fail)

**Setup**: Q1 completed, Napas responds FAILED.

**Expected**:
1. TB: `void_pending_transfer(hash("ibft-q1:ibftA"))` → 2110/3400 cleared
2. `account[3400].balance = 0` ✓
3. `coa_trans.status = FAILED`
4. **No** 5100/1112 transfer (Napas call did not complete — no cost incurred)
5. `wallet_tx` row: `tx_type=IBFT_RELEASE`, `direction=UNFREEZE`, `amount=101,000`, `business_ref="ibft-q1:release"`
6. `wallet_balance.frozen = 0` (− 101,000)
7. `wallet_balance.available = 500,000` (restored)

---

## Q5 — Double-spend protection (timeout ≠ release)

**Setup**: Q1 completed, Napas returns no response for T2 seconds.

**Expected**:
- `wallet_balance.frozen = 101,000` — funds remain frozen
- No IBFT_RELEASE issued
- Payout worker polls Napas (ADR-033) until terminal result
- After Tmax: severity alert to ops; no auto-release

---

## Q6 — Idempotent accept (duplicate businessRef)

**Setup**: Q1 completed (full success). Re-submit `POST /transfers` with same `businessRef="ibft-q1"`.

**Expected**:
- HTTP 200 (same response body)
- No second `wallet_tx` row for IBFT_FREEZE
- No second `coa_trans` row
- No second outbox row

---

## Q7 — LOCKED wallet rejects freeze

**Setup**: member wallet `status=LOCKED`. Submit `POST /transfers`.

**Expected**:
- HTTP 422 (wallet rejects freeze — ADR-029)
- No `coa_trans` row
- No outbox row

---

## Q8 — Napas cost only on settle, not on release

**Setup**: Q1 completed, then force a release path (Napas FAILED terminal).

**Expected**:
- `account[5100]` balance unchanged from pre-Q1 value
- No TB Transfer with `debit_account = 5100` for `businessRef="ibft-q1"`
- Only IBFT_RELEASE wallet_tx present; no IBFT_SETTLE

---

## Invariant cross-check (all Q scenarios)

After every completed IBFT (settled or released):

| Invariant | Check |
|-----------|-------|
| `account[3400].balance = 0` | Query TB account 3400 |
| `SUM(DR) = SUM(CR)` per journal | Query `coa_trans_data` or TB read-model |
| One FREEZE per `businessRef` | `SELECT COUNT(*) FROM wallet_tx WHERE business_ref='ibft-q1' AND tx_type='IBFT_FREEZE'` = 1 |
| One terminal per `businessRef` | Either SETTLE or RELEASE, never both |
| No SETTLE + RELEASE for same ref | Idempotency keys `{ref}:settle` and `{ref}:release` are mutually exclusive |
| 5100 posting only on SETTLE | No 5100/1112 transfer exists if `coa_trans.status = FAILED` |
| `account[5100]` NOT checked for zero | 5100 is an expense accumulator — DR balance grows over time |
