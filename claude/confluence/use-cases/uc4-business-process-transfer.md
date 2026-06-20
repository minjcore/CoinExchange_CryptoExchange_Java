# Business Process: Internal Transfer (UC-4)

> **CF page ID:** 51872241 | **Parent:** UC-4 Internal Transfer (51544073)
> **Source of truth:** this file → push to CF
> **See also:** `specs/005-internal-transfer/data-model.md`, `specs/005-internal-transfer/plan.md`

---

## Overview

| Attribute | Value |
|-----------|-------|
| Pattern | Fully synchronous — 3-commit saga |
| SLA (SC-001) | 1 second |
| Latency estimate | 3 sync hops × 35-75 ms ≈ 105-225 ms (within SLA) |
| Async component | None |
| Transit account | **3300** — must = 0 after every batch commit |
| Fee account | 4130 (transfer fee revenue) |
| Constraint | sender ≠ receiver (422 TRANSFER_SAME_MEMBER) |

---

## Happy Path Flow

```
Member → POST /transfers (businessRef, receiverMemberId, principalAmount, currency)
    │
    ▼
app-orchestration
    │  Validate: sender ≠ receiver; available ≥ gross; idempotency
    │
    │  Step 1: app-wallet: TRANSFER_DEBIT (sender)
    │          available -= gross (principal + fee)
    │
    │  Step 2: app-accounting: createJournal(TRANSFER, POSTED)
    │          TB Transfer 1: hash(ref+":debit") — 2110_sender DR / 3300 CR (gross)
    │          TB Transfer 2: hash(ref+":credit") — 3300 DR / 2110_receiver CR (net)
    │          TB Transfer 3: hash(ref+":4130") — 3300 DR / 4130 CR (fee)
    │          Assert: account[3300].balance = 0
    │          coa_trans.status = POSTED
    │
    │  Step 3: app-wallet: TRANSFER_CREDIT (receiver)
    │          available += net (= principal)
    │
    └─ Return 200 (grossDebited, netCredited, fee)
```

---

## Ledger Mapping (TigerBeetle)

| # | Transfer ID | Debit | Credit | Amount | Leg |
|---|-------------|-------|--------|--------|-----|
| 1 | hash(ref + ":debit") | 2110 (sender) | 3300 (transit) | gross × 10⁴ | Sender debit |
| 2 | hash(ref + ":credit") | 3300 (transit) | 2110 (receiver) | net × 10⁴ | Receiver credit |
| 3 | hash(ref + ":4130") | 3300 (transit) | 4130 (fee revenue) | fee × 10⁴ | Platform fee |

All 3 transfers are non-pending (flags.pending=false) — committed immediately. Invariant: account[3300].balance = 0 ← gross = net + fee (ADR-010).

> Both sender and receiver map to COA 2110 in TigerBeetle (aggregate liability). Per-member balances are tracked in PostgreSQL `wallet_balance`, not in TB accounts.

> Fee is NOT in a wallet TX — it lives only in the TB journal leg (3300 DR / 4130 CR).

---

## Compensation Path (Journal Fail after Sender Debit)

If createJournal fails after TRANSFER_DEBIT has committed:

```
[SENDER_DEBITED]
    │
    ✗ createJournal fails
    │
    ▼ ADJUSTMENT_CREDIT (businessRef:comp) — sender available += gross
    └─ Return 500 (receiver never touched; no TB entry)
```

Unlike withdraw, there is no outbox. Compensation is a direct synchronous call to app-wallet. No async worker.

---

## Forward-Retry Path (Receiver Credit Fail after POSTED)

```
[LEDGER_POSTED]
    │
    ✗ TRANSFER_CREDIT fails
    │
    ▼ Retry TRANSFER_CREDIT idempotently
      (receiverWalletId, businessRef, TRANSFER_CREDIT) prevents double-credit
    └─ Return 200
```

Do NOT reverse the TB journal (ADR-001).

---

## Wallet TX Types

| tx_type | Direction | Who | wallet_balance effect |
|---------|-----------|-----|-----------------------|
| TRANSFER_DEBIT | DEBIT | Sender wallet | available −= gross |
| TRANSFER_CREDIT | CREDIT | Receiver wallet | available += net (= principal) |
| ADJUSTMENT_CREDIT | CREDIT | Sender wallet (compensation) | available += gross (restore) |

---

## Error Responses

| Scenario | HTTP Code | Error Code |
|----------|-----------|------------|
| Sender = Receiver | 422 | TRANSFER_SAME_MEMBER |
| Sender insufficient balance | 422 | WALLET_INSUFFICIENT_BALANCE |
| Duplicate businessRef, different amount | 409 | IDEMPOTENCY_CONFLICT |
| Journal fail (after compensation) | 500 | INTERNAL_ERROR |

---

## Idempotency Keys

- Accept: `businessRef`
- TRANSFER_DEBIT: `(senderWalletId, businessRef, TRANSFER_DEBIT)`
- TRANSFER_CREDIT: `(receiverWalletId, businessRef, TRANSFER_CREDIT)`
- Compensation: `(senderWalletId, businessRef:comp, ADJUSTMENT_CREDIT)`
- TB Transfer IDs: deterministic hashes — crash-safe replay

---

## ADR References

- ADR-001: corrections are new journals
- ADR-005: idempotency end-to-end
- ADR-010: transit 3300 = 0 at terminal
- ADR-027: synchronous HTTP for inter-service calls
- ADR-031: SQL invariant CI
