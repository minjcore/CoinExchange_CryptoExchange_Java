# Research: Withdraw — Phase 0

**Feature**: `003-withdraw` | **Date**: 2026-06-19

All decisions below are locked by existing ADRs. No open unknowns remain for this feature.

---

## R1 — Freeze–settle–release pattern (ADR-007)

**Why not debit on accept?**

| Option | Problem |
|--------|---------|
| Debit wallet immediately on accept | Bank payout may fail → need to re-credit. Re-credit after debit is a correction entry (ADR-001: corrections are new journals, not reverts). Visible balance dip between accept and release. |
| **Freeze on accept; debit only on bank success** ✅ | Member sees `available` decrease immediately (effectively reserved). If bank fails: frozen → available restored with no net wallet debit. If bank succeeds: frozen debited — exactly once. |

**WITHDRAW_FREEZE** (`wallet_tx` direction=FREEZE):
- `wallet_balance.available -= gross`
- `wallet_balance.frozen += gross`

**WITHDRAW_SETTLE** (`wallet_tx` direction=DEBIT, ref=`{businessRef}:settle`):
- `wallet_balance.frozen -= gross`
- `wallet_balance.available` unchanged (debit already happened on freeze)

**WITHDRAW_RELEASE** (`wallet_tx` direction=UNFREEZE, ref=`{businessRef}:release`):
- `wallet_balance.frozen -= gross`
- `wallet_balance.available += gross`

> **No `WITHDRAW_DEBIT` transaction type.** The wallet never debits available twice — freeze acts as the debit commitment, settle closes it. (See `spec/implementation.md §2.1`.)

---

## R2 — TigerBeetle two-phase for withdraw (ADR-037, ADR-007)

Withdraw reuses the same TB pending-transfer pattern as deposit (ADR-006) but:
- Different accounts: `2110 DR / 3200 CR` (not `1111 / 3100`)
- Different timing: Phase A is **synchronous** (before 200), Phase B is **async** (on bank confirm)

**Accept — Phase A (pending):**

```
TB Transfer:
  id              = hash(businessRef + ":withdrawA")
  debit_account   = 2110  (USER wallet liability)
  credit_account  = 3200  (Transit Withdraw)
  amount          = gross × 10⁴
  flags.pending   = true
  user_data_128   = coa_trans.id
```

**Settle — Phase B (post_pending + closing legs):**

```
1. post_pending_transfer(id = hash(businessRef + ":withdrawA"))
   → closes 2110/3200 pending
2. Transfer: id = hash(businessRef + ":1111")
   debit = 3200, credit = 1111, amount = principal × 10⁴
3. Transfer: id = hash(businessRef + ":4120")
   debit = 3200, credit = 4120, amount = fee × 10⁴
→ assert account[3200].balance = 0
```

**Release — void:**

```
void_pending_transfer(id = hash(businessRef + ":withdrawA"))
→ 2110/3200 both cleared; assert account[3200].balance = 0
```

**Idempotency:** Transfer IDs are deterministic → TB rejects duplicate → same as deposit; settle and release have distinct `id` sets so they cannot cross-fire.

---

## R3 — Double-spend rule and bank polling (ADR-033, ADR-007)

**The hard rule:** `timeout ≠ failure`. A bank payout that returns no callback may still succeed. If orchestration auto-releases frozen funds and the payout later succeeds, the member gets a double credit: available was restored + principal left the bank. This is the withdraw double-spend.

```
Bank status    │ Orchestration action
───────────────┼───────────────────────────────────────────────────────
SUCCESS        │ confirmWithdraw → settle wallet → POSTED journal
FAILED         │ voidWithdraw → release wallet → FAILED journal
REJECTED       │ same as FAILED
UNKNOWN/TIMEOUT│ POLL bank (ADR-033: T2 poll interval, Tmax alarm)
               │ Never auto-release
```

**Aging (ADR-021, ADR-033):**
| Condition | Threshold | Action |
|-----------|-----------|--------|
| PENDING, no payout enqueued | T1 | Re-enqueue WITHDRAW_PAYOUT or RELEASE (ops decision) |
| Payout sent, no callback | T2 | Poll bank API |
| Frozen > Tmax | Severity alert | Manual ops resolution |

---

## R4 — Sync accept + async payout split

Why the accept phase is sync (unlike deposit which is fully async):

| Flow | Sync phase | Why |
|------|-----------|-----|
| Deposit | None (202 only) | Bank notifies us — we never block the bank |
| **Withdraw** | Freeze + PENDING journal | Member waits — must confirm reservation before 200; member must not see a 200 while balance is still not reserved |

The bank payout (W4) is async because:
1. Bank transfer latency is 100ms–10s (unpredictable)
2. Payout failure is recoverable → release frozen funds
3. At-least-once outbox ensures W4 is eventually dispatched even if orchestration crashes after 200

The sync accept path (2 HTTP calls) runs in ~70-150 ms (network + DB + logic per hop). SC-001 SLA is 500 ms — margin is sufficient.

---

## Resolved unknowns

No NEEDS CLARIFICATION entries — all locked by ADR-007, ADR-033, ADR-021, ADR-005, ADR-008, ADR-009, ADR-010, ADR-037.
