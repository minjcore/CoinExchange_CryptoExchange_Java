# Research: IBFT — Phase 0

**Feature**: `006-ibft` | **Date**: 2026-06-19

All decisions below are locked by existing ADRs. No open unknowns remain for this feature.

---

## R1 — Freeze–settle–release pattern for IBFT (ADR-007)

IBFT reuses the same freeze-settle-release pattern as withdraw. The rationale is identical: the member must not see a 200 while funds are not yet reserved, and a failed payout must not leave a debit on the wallet without a corresponding bank transfer.

| Option | Problem |
|--------|---------|
| Debit wallet immediately on accept | Napas payout may fail → need to re-credit. Re-credit is a correction entry (ADR-001). Visible balance dip between accept and release. |
| **Freeze on accept; debit only on Napas success** ✅ | Member sees `available` decrease immediately (effectively reserved). If Napas fails: frozen → available restored with no net wallet debit. If Napas succeeds: frozen debited — exactly once. |

**IBFT_FREEZE** (`wallet_tx` direction=FREEZE):
- `wallet_balance.available -= gross` (gross = principal + platformFee)
- `wallet_balance.frozen += gross`

**IBFT_SETTLE** (`wallet_tx` direction=DEBIT, ref=`{businessRef}:settle`):
- `wallet_balance.frozen -= gross`
- `wallet_balance.available` unchanged (debit already happened on freeze)

**IBFT_RELEASE** (`wallet_tx` direction=UNFREEZE, ref=`{businessRef}:release`):
- `wallet_balance.frozen -= gross`
- `wallet_balance.available += gross`

> **No `IBFT_DEBIT` transaction type.** The wallet never debits available twice — freeze acts as the debit commitment, settle closes it. Same semantics as withdraw.

> **napasCost is NOT in gross.** The platform pays the Napas interchange fee. It is posted as a separate expense leg (5100 DR / 1112 CR) and does not affect the member's frozen or available balance.

---

## R2 — TigerBeetle two-phase for IBFT (ADR-037, ADR-007)

IBFT reuses the same TB pending-transfer pattern as withdraw but:
- Different accounts: `2110 DR / 3400 CR` (not 2110/3200)
- Different clearing: `3400 → 1112` (not 1111 — ADR-025: Napas uses its own clearing account)
- Extra Napas cost leg on settle: `5100 DR / 1112 CR` (platform expense; not gated by 3400=0)

**Accept — Phase A (pending):**

```
TB Transfer:
  id              = hash(businessRef + ":ibftA")
  debit_account   = 2110  (USER wallet liability)
  credit_account  = 3400  (Transit IBFT)
  amount          = gross × 10⁴
  flags.pending   = true
  user_data_128   = coa_trans.id
```

**Settle — Phase B (post_pending + closing legs):**

```
1. post_pending_transfer(id = hash(businessRef + ":ibftA"))
   → closes 2110/3400 pending
2. Transfer: id = hash(businessRef + ":4130")
   debit = 3400, credit = 4130, amount = platformFee × 10⁴
3. Transfer: id = hash(businessRef + ":1112")
   debit = 3400, credit = 1112, amount = principal × 10⁴
4. Transfer: id = hash(businessRef + ":5100")
   debit = 5100, credit = 1112, amount = napasCost × 10⁴
→ assert account[3400].balance = 0
```

**Why 3400 = 0 holds:** post_pending credits 3400 with gross = principal + platformFee. Steps 2 and 3 debit 3400 by platformFee + principal = gross. Step 4 does not touch 3400 — it is a pure platform expense between 5100 and 1112.

**Release — void:**

```
void_pending_transfer(id = hash(businessRef + ":ibftA"))
→ 2110/3400 both cleared; assert account[3400].balance = 0
```

**Idempotency:** Transfer IDs are deterministic → TB rejects duplicate → same as withdraw; settle and release have distinct `id` sets so they cannot cross-fire.

---

## R3 — Double-spend rule and bank polling (ADR-033, ADR-007)

**The hard rule:** `timeout ≠ failure`. A Napas payout that returns no callback may still succeed. If orchestration auto-releases frozen funds and the payout later succeeds, the member gets a double credit: available was restored + principal left the bank. This is the IBFT double-spend.

```
Napas status    │ Orchestration action
────────────────┼───────────────────────────────────────────────────────
SUCCESS         │ confirmIbft → settle wallet → POSTED journal
FAILED          │ voidIbft → release wallet → FAILED journal
REJECTED        │ same as FAILED
UNKNOWN/TIMEOUT │ POLL Napas (ADR-033: T2 poll interval, Tmax alarm)
                │ Never auto-release
```

**Aging (ADR-021, ADR-033):**
| Condition | Threshold | Action |
|-----------|-----------|--------|
| PENDING, no payout enqueued | T1 | Re-enqueue IBFT_PAYOUT or RELEASE (ops decision) |
| Payout sent, no callback | T2 | Poll Napas API |
| Frozen > Tmax | Severity alert | Manual ops resolution |

---

## R4 — Napas cost leg design (ADR-025)

**Why a separate expense leg?**

The Napas interchange fee (napasCost) is an operating expense borne by the platform, not the member. It must be recorded as a debit to account 5100 (Napas cost expense) offset by a credit to account 1112 (Napas clearing). This posting:
1. Is independent of the 3400 transit balance — it cannot break the 3400=0 invariant
2. Is posted only on settle (not on release — no Napas call was completed on failure)
3. Uses account 1112 per ADR-025: Napas clearing is distinct from Vietinbank Nostro (1111)

**Why 1112, not 1111?**

ADR-025 establishes that Napas settlements flow through account 1112 (Napas Clearing), not 1111 (Vietinbank Nostro). 1111 is used exclusively for Vietinbank-direct transfers. Mixing them would corrupt the nostro reconciliation.

**Why 5100 is not transit:**

5100 is an expense account — it accumulates a debit balance over time representing platform cost of Napas transactions. It is not a transit account and is not expected to net to zero per transaction. The INV for IBFT concerns only 3400.

**Numeric example:** principal=100,000 platformFee=1,000 napasCost=500:
- Phase A: 2110 DR 101,000 / 3400 CR 101,000 (pending)
- Phase B:
  1. post_pending(ibftA) → closes 2110/3400
  2. 3400 DR 1,000 / 4130 CR 1,000 (platform fee revenue)
  3. 3400 DR 100,000 / 1112 CR 100,000 (principal to Napas clearing)
  4. 5100 DR 500 / 1112 CR 500 (Napas cost expense)
- 3400 = 0 ✓; net platform profit = 1,000 − 500 = +500

---

## R5 — Sync accept + async payout split

| Flow | Sync phase | Why |
|------|-----------|-----|
| Deposit | None (202 only) | Bank notifies us — we never block the bank |
| Withdraw | Freeze + PENDING journal | Member waits — must confirm reservation before 200 |
| **IBFT** | Freeze + PENDING journal | Same as withdraw — member blocks until funds are reserved |

The Napas payout (async) is dispatched via outbox relay after 200. Bank transfer latency is 100ms–10s (unpredictable). At-least-once outbox ensures IBFT_PAYOUT is eventually dispatched even if orchestration crashes after 200.

The sync accept path (2 HTTP calls) runs in ~70-150 ms. SC-001 SLA is 500 ms — margin is sufficient.

---

## Resolved unknowns

No NEEDS CLARIFICATION entries — all locked by ADR-007, ADR-025, ADR-033, ADR-021, ADR-005, ADR-008, ADR-009, ADR-010, ADR-037.
