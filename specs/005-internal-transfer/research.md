# Research: Internal Transfer — Phase 0

**Feature**: `005-internal-transfer` | **Date**: 2026-06-19

All decisions below are locked by existing ADRs. No open unknowns remain for this feature.

---

## R1 — Sync 3-commit pattern (ADR-027)

**Why not freeze-settle like withdraw?**

| Option | Problem |
|--------|---------|
| Freeze sender, then settle after confirming receiver credit | No external bank is involved — there is no async gate. Adding a freeze-settle cycle adds latency and complexity with no benefit. |
| Debit sender immediately, credit receiver, one round-trip | ✅ Both parties are internal; if credit fails, orchestration compensates with ADJUSTMENT_CREDIT. No double-spend risk because receiver is also internal. |

**TRANSFER_DEBIT** (`wallet_tx` direction=DEBIT):
- `wallet_balance.available -= gross` (gross = principal + fee)

**TRANSFER_CREDIT** (`wallet_tx` direction=CREDIT):
- `wallet_balance.available += net` (net = principal; fee is revenue only — no wallet leg)

> **No freeze/unfreeze cycle.** Because there is no external counterparty, the sender debit is permanent on step 2. If accounting fails in step 3, orchestration immediately issues ADJUSTMENT_CREDIT (not an unfreeze — ADR-001: corrections are new journal entries).

---

## R2 — TigerBeetle non-pending batch (ADR-027, ADR-010)

Internal transfer uses **non-pending** TigerBeetle transfers — no `flags.pending`, no post/void phase. The 3 transfers are submitted as a single batch, atomically POSTED.

**Batch (3 non-pending transfers, submitted together):**

```
Transfer 1 — debit leg:
  id              = hash(businessRef + ":debit")
  debit_account   = 2110_sender   (USER wallet liability, sender)
  credit_account  = 3300          (Transit Internal Transfer)
  amount          = gross × 10⁴
  flags.pending   = false

Transfer 2 — credit leg:
  id              = hash(businessRef + ":credit")
  debit_account   = 3300          (Transit Internal Transfer)
  credit_account  = 2110_receiver (USER wallet liability, receiver)
  amount          = net × 10⁴    (net = principal)
  flags.pending   = false

Transfer 3 — fee leg:
  id              = hash(businessRef + ":4130")
  debit_account   = 3300          (Transit Internal Transfer)
  credit_account  = 4130          (Transfer Fee Revenue)
  amount          = fee × 10⁴
  flags.pending   = false
```

After the batch: `account[3300].balance = 0` ✓
- 3300 credit = gross×10⁴
- 3300 debit = net×10⁴ + fee×10⁴ = (principal + fee)×10⁴ = gross×10⁴

**Note on 2110:** Both sender and receiver map to the same aggregate COA account 2110 in TigerBeetle. Per-member balances are tracked in PostgreSQL `wallet_balance`, not in TB. TigerBeetle sees two logical sub-ledger IDs (one per member) but they share the same COA code.

**Idempotency:** Transfer IDs are deterministic hashes → TB rejects duplicate with known error code → same result, no double-posting. The three IDs are disjoint and cannot cross-fire with any other use case because they embed `businessRef`.

---

## R3 — Compensation model (ADR-027, ADR-008)

Because the flow is synchronous (no async worker), compensation is straightforward but must be designed carefully:

**Step ordering:**
1. `TRANSFER_DEBIT` (sender wallet)
2. `createJournal(TRANSFER, POSTED)` (accounting + TB batch)
3. `TRANSFER_CREDIT` (receiver wallet)

**Failure scenarios:**

| Failure point | State before failure | Compensation |
|--------------|---------------------|-------------|
| Step 2 fails after step 1 | Sender debited, no TB journal | Issue `ADJUSTMENT_CREDIT` ref `{businessRef}:comp` to restore sender. Receiver never touched. |
| Step 3 fails after step 2 | Sender debited, TB POSTED | Forward-retry `TRANSFER_CREDIT` idempotently. Do NOT reverse ledger (ADR-001: POSTED journals are immutable). |
| Step 1 fails | Nothing committed | Return error. No compensation needed. |

**Why not reverse the TB journal when step 3 fails?**

ADR-001 prohibits reversing POSTED journals. Instead:
- Retry `TRANSFER_CREDIT` idempotently using `(wallet_id, business_ref, tx_type)` unique constraint.
- If retry exhausted: raise an ops alert — the TB ledger is POSTED, the receiver's wallet must eventually be credited. Manual intervention may be required but the ledger is already balanced.

**ADJUSTMENT_CREDIT** (compensation for step 2 failure):
- `wallet_tx.tx_type = ADJUSTMENT_CREDIT`
- `wallet_tx.direction = CREDIT`
- `business_ref = {businessRef}:comp`
- `wallet_balance.available += gross` (restores sender to pre-transfer state)
- This is a new positive journal entry, not a reversal.

---

## R4 — Sender ≠ receiver constraint

Reject at orchestration layer before any wallet or accounting call. If `request.senderId == request.receiverId`, return HTTP 422 with error code `TRANSFER_SAME_MEMBER`. This prevents a trivially self-referential transaction that would produce zero net ledger effect but consume resources and create confusing audit records.

---

## R5 — Fee wallet treatment

The transfer fee is captured as accounting revenue only (account 4130 in TB). There is no wallet TX for the fee on the sender side — the fee is embedded in `gross` which is the total debited from `available`. The fee leg exists solely in the TB journal:

```
3300 DR / 4130 CR   amount = fee × 10⁴
```

This is consistent with ADR-009: orchestration computes fee once, passes gross+net+fee to accounting. Wallet sees `TRANSFER_DEBIT amount=gross`, but the split into principal vs fee lives only in the TB batch.

---

## Resolved unknowns

No NEEDS CLARIFICATION entries — all locked by ADR-001, ADR-005, ADR-008, ADR-009, ADR-010, ADR-027.
