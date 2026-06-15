# core.accounting

**Domain 1** — double-entry ledger. Owns `coa_*` only.

## Docs in this folder

| File | Content |
|------|---------|
| [`model.md`](./model.md) | Tables, journal lifecycle, invariants |
| [`surface-map.md`](./surface-map.md) | **S2 OpenAPI ↔ accounting** (`api-internal.yaml`, S6, S3) |
| [`coa.md`](./coa.md) | Full chart of accounts |
| [`postings.md`](./postings.md) | DR/CR per use case |
| [`api-internal.yaml`](./api-internal.yaml) | S2 REST (orchestration → accounting) |
| [`events.yaml`](./events.yaml) | Kafka: `JournalPosted` (accounting emits) |

## Does NOT own

- `wallet_*` tables or balance mutations
- Public HTTP (orchestration)
- Fee **computation** (orchestration passes fee amounts; accounting records the lines)
- RabbitMQ command consumption (worker adapter in orchestration/accounting-worker)

## Invariants

1. `sum(DR) = sum(CR)` per journal before POSTED.
2. Posted `coa_trans_data` is immutable — corrections = reversing journal ([ADR-001](../adr/ADR-001-immutable-ledger.md)).
3. Transit accounts return to **0** when the use case completes.
4. `(1111+1112+1113) = (2110+2120+2130)` at reconciliation tolerance.
