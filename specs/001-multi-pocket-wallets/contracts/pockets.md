# Phase 1 — Contracts: Pocket APIs

**Source of truth (wire):** [`spec/contracts/open-api/gtelpay-public.yaml`](../../../spec/contracts/open-api/gtelpay-public.yaml)
(tag `Pockets`) — already drafted. This file summarizes the contract surface and its
backward-compatibility properties; it does not duplicate the YAML.

## Endpoints (S1 public)

| Method · Path | operationId | Purpose | Idempotency |
|---------------|-------------|---------|-------------|
| `GET /wallets/pocket-defs` | `listPocketDefs` | Catalog of active pocket kinds to create from | n/a (read) |
| `GET /wallets/pockets` | `listPockets` | Member's pockets (label, def, status, available, frozen) | n/a (read) |
| `POST /wallets/pockets` | `createPocket` | Create pocket from a def; no ledger | `businessRef` |
| `POST /wallets/pockets/{walletId}/close` | `closePocket` | Close empty pocket; default not closable | `businessRef` |
| `POST /wallets/pocket-transfers` | `createPocketTransfer` | Move funds between two of caller's pockets | triple `(wallet_id, business_ref, tx_type)` |

## Backward-compatible changes to existing endpoints

**Additive & optional only** — existing clients unaffected:

| Endpoint | Added | Default when omitted |
|----------|-------|----------------------|
| `GET /wallets/balance` | query `walletId`, `pocketCode` | member's `'default'` pocket |
| `WalletBalanceData` schema | `walletId`, `pocketCode`, `label` | populated for resolved pocket |

> Payment/transfer/withdraw may later accept an optional pocket selector the same way; v1 keeps
> them defaulting to the default pocket so no existing call changes.

## Response / error contract

| Outcome | HTTP | errorCode (in `ApiResponseError`) |
|---------|------|-----------------------------------|
| Pocket created | 201 | — |
| Unknown/inactive def | 400/409 | `WALLET_POCKET_DEF_INVALID` |
| Def forbids second pocket | 409 | `WALLET_POCKET_EXISTS` |
| Duplicate label | 409 | `WALLET_DUPLICATE_CONFLICT` |
| Close non-empty / default | 409 | `WALLET_POCKET_NOT_EMPTY` / (default) |
| Move insufficient | 422 | `WALLET_INSUFFICIENT_BALANCE` |
| Move same pocket | 409 | `WALLET_INVALID_TRANSFER` |
| Pocket LOCKED/CLOSED | 403/409 | `WALLET_LOCKED` |

> `WALLET_POCKET_*` codes are new — follow-up to register them in `core.foundation` ErrorCode and
> `spec/contracts/async-api/core-events.yaml` (tracked in tasks, not blocking the contract).

## Conformance link

Each operation traces to ADR-040 AC/TC and the Gherkin in `design-v2/acceptance.md`
(pocket feature). See [quickstart.md](../quickstart.md) for the runnable validation mapping.
