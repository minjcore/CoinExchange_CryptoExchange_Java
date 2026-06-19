# 004-payment — Research Notes

## Overview

This document records the key design decisions, trade-off analysis, and rejected alternatives for the Wallet Payment feature (UC-3).

---

## Flow Design: Why Synchronous 3-Commit?

### Decision (ADR-027)

Payment is synchronous (not async/event-driven). The client waits for a 200 OK that confirms both the USER debit and the MERCHANT credit.

### Rationale

- Payments are user-initiated, interactive actions. Users expect immediate confirmation.
- Async flows (like `002-async-deposit`) are appropriate for external bank events where the initiating party is a third-party system with its own timeline.
- The 3-step sequence fits within a 1-second SLA (SC-001) given ~35–75 ms per service call.
- Keeping payment synchronous avoids a polling or webhook mechanism for a common, latency-sensitive operation.

### Rejected: Event-driven payment

An async payment (RabbitMQ → WALLET_CREDIT event) was considered but rejected. It adds ~1–5 seconds of end-to-end latency, requires a status polling endpoint, and increases surface area for partially-observed states. The SLA permits synchronous execution.

---

## 3-Step Order: Why Debit → Ledger → Credit?

The debit-first ordering ensures:

1. USER funds are reserved before any ledger entry exists
2. If the ledger write fails, the compensation (`ADJUSTMENT_CREDIT`) returns funds to USER — merchant was never in the picture
3. If the merchant credit fails, the ledger is already POSTED — we know money moved and can forward-retry safely

Inverting the order (ledger before debit) would risk a POSTED journal entry with no corresponding wallet debit, making compensation logic more complex.

---

## Ledger: Immediately POSTED (no pending flag)

For the withdraw spec (`003-withdraw`) the TigerBeetle transfers may use a pending flag when clearing external settlement. For internal wallet payments there is no external clearing event — both parties are on-platform. Therefore the journal entry is immediately POSTED (no `PENDING` → `POST` two-phase cycle).

This is simpler and matches the synchronous, same-platform nature of the transaction.

---

## Account Structure

### Accounts Used

| Code | Name | Type |
|------|------|------|
| 2110 | USER liability | Liability (platform owes USER) |
| 3500 | Transit payment | Clearing / transit |
| 2120 | MERCHANT liability | Liability (platform owes MERCHANT) |

### Transit Account 3500

The transit account (3500) is used as a pass-through to ensure double-entry integrity for each leg. After both transfers post, `balance(3500) = 0`. This is asserted at runtime.

### Why No Fee Account in v1?

v1 is zero-fee (net = gross). The fee account (e.g., 4100 Revenue) is not wired yet. ADR-009 specifies fee computation belongs at the orchestration layer. When fees are introduced, a third transfer `debit=3500 credit=4100 amount=fee×10⁴` will be added.

---

## TigerBeetle Batch Design

Two transfers per payment, submitted as a single batch (atomic):

| # | Transfer ID | Debit | Credit | Amount |
|---|-------------|-------|--------|--------|
| 1 | hash(businessRef + ":2110") | 2110 | 3500 | gross × 10⁴ |
| 2 | hash(businessRef + ":2120") | 3500 | 2120 | net × 10⁴ |

- Amount is stored in minor units × 10⁴ (e.g., 100.00 → 1 000 000)
- Transfer IDs are deterministic hashes of businessRef + account suffix — idempotent by construction
- Batch atomicity: either both transfers post or neither does

Post-batch assertion: `account[3500].balance == 0` (debits equal credits through transit).

---

## Compensation Design

### Compensation vs. Reversal

ADR-001 mandates an immutable ledger — once POSTED, entries cannot be deleted or modified. Compensation is the only mechanism.

### Scenario: Ledger fails after USER debit

```
PAYMENT_DEBIT   ✓  (USER -= gross, wallet_tx row exists)
createJournal   ✗  (TB batch rejected or network error)
PAYMENT_CREDIT  —  (never attempted)
```

Compensation: issue `ADJUSTMENT_CREDIT` with `businessRef = {original}:comp`.
- Restores USER available balance
- No ledger entry for the compensation (wallet adjustment only, since no TB entry was created)
- The `:comp` suffix creates a distinct businessRef, satisfying the unique constraint

### Scenario: Merchant credit fails after ledger POSTED

```
PAYMENT_DEBIT   ✓
createJournal   ✓  (POSTED — immutable)
PAYMENT_CREDIT  ✗
```

Correct action: forward-retry `PAYMENT_CREDIT` only.
- Ledger entry exists and is correct; reversing it would create an accounting error
- `PAYMENT_CREDIT` is idempotent via `(wallet_id, business_ref, PAYMENT_CREDIT)` unique constraint
- Retry can be immediate (same request) or via a background reconciliation job

---

## Idempotency Strategy

### wallet_tx Unique Constraint

`(wallet_id, business_ref, tx_type)` — three-part composite key.

This means the same `(wallet, ref, PAYMENT_DEBIT)` can only exist once. A retry of `POST /payments` will hit this constraint on `PAYMENT_DEBIT` and short-circuit, returning the cached result.

### TigerBeetle Idempotency

Transfer IDs are deterministic hashes. Submitting the same batch twice is a no-op (TigerBeetle deduplicates by transfer ID natively).

### businessRef Semantics (ADR-005)

The caller must supply a stable `businessRef` per payment intent. The orchestration layer rejects requests that omit `businessRef` or that reuse a `businessRef` with different amounts/parties.

---

## SLA Analysis

SC-001 = 1 000 ms end-to-end.

Each synchronous HTTP hop is estimated at 35–75 ms (intra-cluster, same datacenter). Three hops = 105–225 ms. This leaves >750 ms of headroom for:
- Auth/JWT validation at entry
- DB reads for wallet lookup
- Network jitter
- TigerBeetle batch write (~1–5 ms for in-memory + WAL)

The SLA is comfortably achievable with synchronous execution.

---

## Rejected Alternatives Summary

| Alternative | Reason rejected |
|-------------|----------------|
| Async queue for payment | Latency mismatch; polling required; adds complexity for common interactive flow |
| Pending → Post two-phase TB | No external clearing event; immediately POSTED is simpler |
| Reverse ledger on failure | ADR-001 prohibits reversal; compensation-only |
| Fee in v1 | Zero-fee is correct for v1; ADR-009 defers to orchestration layer |
| Single-transfer TB batch | Double-entry requires two legs; transit 3500 enforces balance integrity |
