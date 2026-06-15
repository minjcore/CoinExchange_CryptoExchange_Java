# Idempotency (ADR-005)

`businessRef` is the single business idempotency key across HTTP, accounting, wallet, and RabbitMQ.

| Surface | Field |
|---------|-------|
| Public HTTP | header `X-Idempotency-Key` = body `businessRef` |
| Accounting | `reference_id` on `coa_trans` |
| Wallet | `business_ref` on `wallet_tx` |
| RabbitMQ | envelope `businessRef` (full body, not header-only) |

## Uniqueness

| Store | Constraint | Why |
|-------|------------|-----|
| `wallet_tx` | `UNIQUE (wallet_id, business_ref, tx_type)` | One action, multiple legs (debit USER + credit MERCHANT) |
| `coa_trans` | `UNIQUE (reference_id, use_case)` | One journal per business action |

## Sub-keys (withdraw / IBFT follow-up legs)

| Leg | `business_ref` |
|-----|----------------|
| Accept (freeze) | S1 `businessRef` |
| Settle | `{businessRef}:settle` |
| Release | `{businessRef}:release` |
| Compensate (payment/transfer rollback) | `{businessRef}:comp` |

## Replay semantics

- Same key + same semantics → return prior result, `idempotentReplay=true`, no second effect.
- Same key + **different amount** → `WALLET_DUPLICATE_CONFLICT` / HTTP 409.
- `messageId` (RabbitMQ) = transport dedup only, **not** the business key.
