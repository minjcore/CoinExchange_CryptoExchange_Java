# Wallet — data model

Schema: **`wallet`**. Prefix: **`wallet_`**.

## `wallet`

One row = member + lane + currency.

| Column | Notes |
|--------|-------|
| `member_id` | |
| `wallet_type` | USER, MERCHANT, PARTNER |
| `currency` | v1: VND |
| `status` | ACTIVE, LOCKED, CLOSED |

**UNIQUE:** `(member_id, wallet_type, currency)`

## `wallet_balance`

| Column | Notes |
|--------|-------|
| `wallet_id` | PK, 1:1 |
| `available` | ≥ 0, spendable |
| `frozen` | ≥ 0, held |
| `version` | optimistic lock |

**Invariant W1:** `available ≥ 0`, `frozen ≥ 0` always.

## `wallet_tx` (append-only)

| Column | Notes |
|--------|-------|
| `tx_type` | see [`operations.md`](./operations.md) |
| `direction` | CREDIT, DEBIT, FREEZE, UNFREEZE |
| `amount` | > 0 |
| `available_after`, `frozen_after` | snapshot |
| `business_ref` | idempotency |
| `coa_trans_id` | correlation only, **no FK** |

**UNIQUE:** `(wallet_id, business_ref, tx_type)` ([`../platform/idempotency.md`](../platform/idempotency.md))

## Invariants

| # | Rule |
|---|------|
| W2 | Every balance change = exactly one new `wallet_tx` in same transaction |
| W3 | Replay same triple → same `wallet_tx.id`, no duplicate effect |
| W4 | Never touch `coa_*` |
| W5 | Sum `available+frozen` by lane reconciles to 2110+2120+2130 (timing tolerance) |

## Service API (domain)

| Method | |
|--------|--|
| `provisionIfAbsent` | |
| `getBalance` | |
| `credit` | |
| `debit` | |
| `freeze` | |
| `unfreeze` | |

One `@Transactional` per mutation. Row lock on `wallet_balance`.

**S1 OpenAPI binding:** [`surface-map.md`](./surface-map.md) — map `http-public.yaml` ↔ operations ↔ tables.
