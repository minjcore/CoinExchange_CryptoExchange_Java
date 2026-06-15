# core.wallet

**Domain 2** — per-member spendable balance. Owns `wallet_*` only.

## Docs in this folder

| File | Content |
|------|---------|
| [`model.md`](./model.md) | Tables, invariants |
| [`surface-map.md`](./surface-map.md) | **S1 OpenAPI ↔ wallet** (`http-public.yaml`) |
| [`operations.md`](./operations.md) | Commands + `tx_type` enum |
| [`events.yaml`](./events.yaml) | Kafka: `WalletCredited` (wallet emits) |

## Does NOT own

- `coa_*` / ledger posting
- Fee computation (receives `amount` from orchestration)
- Public HTTP
- Bank payout

## Question this domain answers

*"How much can this member spend right now?"* → `available` and `frozen`.
