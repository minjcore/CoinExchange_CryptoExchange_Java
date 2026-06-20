# Business Process: IBFT (UC-5)

> **CF page ID:** 52199477 | **Parent:** UC-5 IBFT (50332070)
> **Source of truth:** this file → push to CF
> **See also:** `specs/006-ibft/data-model.md`, `specs/006-ibft/plan.md`

---

## Overview

| Attribute | Value |
|-----------|-------|
| Pattern | Sync accept + async payout (freeze-settle-release) |
| SLA (SC-001) | 500 ms for accept (200) |
| Latency estimate (accept) | 2 sync hops × 35-75 ms ≈ 70-150 ms (within SLA) |
| Async component | app-payout-worker via RabbitMQ core.commands.ibft-payout |
| Transit account | **3400** — must = 0 after every POSTED or FAILED txn |
| Clearing account | **1112** (Napas, per ADR-025 — NOT 1111 Vietinbank nostro) |
| Expense account | **5100** (Napas cost — accumulates DR; NOT in 3400=0 invariant) |
| Gross to member | principal + platformFee (napasCost NOT in gross — platform bears it) |

---

## Accept Phase (Synchronous, before 200)

```
Member → POST /transfers (businessRef, destinationBankAccountNumber, destinationBankCode, principalAmount)
    │
    ▼
app-orchestration
    │  Validate: balance ≥ gross; idempotency; fee computation (platformFee, napasCost)
    │
    │  1. app-wallet: IBFT_FREEZE
    │     available -= gross (principal + platformFee)
    │     frozen += gross
    │
    │  2. app-accounting: createJournal(IBFT, PENDING)
    │     TB Phase A: hash(ref+":ibftA") — 2110 DR / 3400 CR (gross), flags.pending=true
    │     coa_trans.status = PENDING
    │
    │  3. INSERT outbox(IBFT_PAYOUT) ─┐ same transaction (ADR-013)
    │  4. Return 200                  ─┘
    │
    │  outbox relay → RabbitMQ: IBFT_PAYOUT
```

---

## Settle Phase (Async, on Napas SUCCESS)

```
app-payout-worker → Napas API
    │
    ├─ SUCCESS
    │       │
    │       ├─ app-accounting: confirmIbft(coaTransId, principal, platformFee, napasCost)
    │       │    TB: post_pending_transfer(hash(ref+":ibftA"))  ← closes Phase A
    │       │    TB Transfer hash(ref+":4130"): 3400 DR / 4130 CR  platformFee
    │       │    TB Transfer hash(ref+":1112"): 3400 DR / 1112 CR  principal
    │       │    TB Transfer hash(ref+":5100"): 5100 DR / 1112 CR  napasCost  ← separate expense leg
    │       │    Assert: account[3400].balance = 0
    │       │    coa_trans.status = POSTED
    │       │
    │       └─ app-wallet: IBFT_SETTLE — frozen -= gross
```

---

## Release Phase (Async, on Napas FAIL)

```
    └─ FAIL (terminal)
            │
            ├─ app-accounting: voidIbft(coaTransId)
            │    TB: void_pending_transfer(hash(ref+":ibftA"))
            │    coa_trans.status = FAILED
            │    (5100/1112 NOT posted — no Napas call completed)
            │    Assert: account[3400].balance = 0
            │
            └─ app-wallet: IBFT_RELEASE — frozen -= gross, available += gross

Napas UNKNOWN / TIMEOUT → POLL (ADR-033) — never auto-release
```

---

## Ledger Mapping (TigerBeetle)

### Phase A — PENDING (accept, sync)

| Field | Value |
|-------|-------|
| Transfer ID | hash(businessRef + ":ibftA") |
| Debit | 2110 (USER wallet liability) |
| Credit | 3400 (IBFT transit) |
| Amount | gross × 10⁴ |
| flags.pending | true |

### Phase B — Settle (async)

| # | Transfer ID | Debit | Credit | Amount | Notes |
|---|-------------|-------|--------|--------|-------|
| 1 | hash(ref+":ibftA") | — | — | — | post_pending closes Phase A |
| 2 | hash(ref+":4130") | 3400 | 4130 | platformFee × 10⁴ | Platform fee revenue |
| 3 | hash(ref+":1112") | 3400 | 1112 | principal × 10⁴ | Napas clearing |
| 4 | hash(ref+":5100") | 5100 | 1112 | napasCost × 10⁴ | Platform expense — does NOT touch 3400 |

**Invariant:** account[3400].balance = 0 after settle. account[5100] accumulates DR and is NOT checked.

---

## Wallet TX Types

| tx_type | Direction | When | wallet_balance effect |
|---------|-----------|------|-----------------------|
| IBFT_FREEZE | FREEZE | Accept (sync) | available −= gross, frozen += gross |
| IBFT_SETTLE | DEBIT | Napas success (async) | frozen −= gross |
| IBFT_RELEASE | UNFREEZE | Napas fail (async) | frozen −= gross, available += gross |

---

## Double-Spend Rule (ADR-033)

| Napas status | Action |
|--------------|--------|
| SUCCESS | confirmIbft → POSTED → IBFT_SETTLE |
| FAILED / REJECTED | voidIbft → FAILED → IBFT_RELEASE |
| UNKNOWN / TIMEOUT | Poll (ADR-033 T2 interval) → after Tmax: severity alert, manual ops |

Auto-releasing on timeout would cause double-credit: funds returned AND payout later confirmed by Napas.

---

## Napas Cost Accounting

The Napas clearing cost (napasCost) is a platform expense — the member is not charged for it. Posted only on settle (not on release). Net platform profit per IBFT = platformFee − napasCost.

- Account 5100 (expense) accumulates DR balance over time — NOT expected to net to zero
- Account 1112 is used per ADR-025 (Napas clearing, not Vietinbank nostro 1111)

---

## Orchestration Interface

### Inbound — IbftRequest (from member)
```
POST /transfers
IbftRequest {
  destinationBankAccountNumber : string
  destinationBankCode          : string
  principalAmount              : string (decimal, s4)
  businessRef                  : string
  currency                     : string
}
```

### Outbound — IbftAck (HTTP 200)
```
IbftAck {
  businessRef : string
  status      : "ACCEPTED"
  gross       : string   // principal + platformFee
  frozen      : string   // wallet_balance.frozen after freeze
}
```

### Outbound — IbftPayoutCommand (RabbitMQ outbox)
```
Exchange: core.commands  Key: core.commands.ibft-payout
IbftPayoutCommand {
  commandType, businessRef, memberId, walletId, coaTransId,
  principalAmount, platformFee, napasCost, grossAmount,
  destinationBankAccountNumber, destinationBankCode, currency
}
```

---

## ADR References

- ADR-007: freeze-settle-release pattern for outflows
- ADR-010: transit account 3400 = 0 at terminal
- ADR-013: transactional outbox (written with 200)
- ADR-025: use 1112 (Napas clearing) for IBFT, not 1111
- ADR-033: bank/Napas polling — UNKNOWN ≠ release
