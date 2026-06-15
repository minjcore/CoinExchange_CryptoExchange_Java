# Orchestration — failures & compensation

No distributed TX ([ADR-003](../adr/ADR-003-dual-schema-single-postgres.md)). Each step = local commit. Recovery = idempotency + forward-retry or compensate.

## Payment / transfer (sync)

| Failed after | Recovery |
|--------------|----------|
| Debit only | Compensate credit `{ref}:comp` to payer |
| Post OK, credit fail | Forward-retry credit (idempotent) |
| Response lost | Client retry same `businessRef` → replay |

## Deposit

| Failed after | Recovery |
|--------------|----------|
| POSTED, wallet credit fail | Retry credit; **never** edit ledger |
| VA unmapped | Stay PENDING; manual |
| Bank reversal | New reversing journal + wallet debit |

## Withdraw / IBFT

**Timeout ≠ failure.** Do not release frozen funds until bank gives terminal result.

| Failed after | Recovery |
|--------------|----------|
| Payout unknown | Poll bank; hold remains |
| Payout fail (terminal) | `RELEASE` / `WITHDRAW_RELEASE` |
| Settle fail after payout OK | Retry settle (idempotent) |

## Global

- Same `businessRef`, different amount → 409
- Aging jobs for PENDING deposit and frozen-without-payout
- Reconciliation W5: report drift, no auto-COA edit from wallet
