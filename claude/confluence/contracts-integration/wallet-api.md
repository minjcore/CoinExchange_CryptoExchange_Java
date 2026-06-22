# Wallet Internal API Contract

> **CF page ID:** 51610183 | **Parent:** 🔌 Contracts & Integration (51315085)
> **Source of truth:** `specs/contracts/open-api/wallet-internal.yaml`
> **Callers:** `app-orchestration` (sync), `app-wallet-worker` (async via s6-rabbitmq-cmds)

---

## Overview

`app-wallet` exposes 6 HTTP endpoints (Internal HTTP / s2-http-internal).
Not exposed externally.

Deposit async path: `app-wallet-worker` receives `WALLET_CREDIT` from s6-rabbitmq-cmds → calls `POST /wallets/{walletId}/credit`.
Sync path: `app-orchestration` calls directly for payment, transfer, and balance reads.

---

## Endpoints

| Method | Path | Operation | Use case |
|--------|------|-----------|---------|
| `POST` | `/wallets` | `provisionWallet` | Create wallet + zero balance (idempotent) |
| `POST` | `/wallets/{walletId}/credit` | `creditWallet` | Increase available (deposit credit, transfer in) |
| `POST` | `/wallets/{walletId}/debit` | `debitWallet` | Decrease available (payment, transfer out) |
| `POST` | `/wallets/{walletId}/freeze` | `freezeWallet` | available → frozen (withdrawal hold) |
| `POST` | `/wallets/{walletId}/unfreeze` | `unfreezeWallet` | frozen → available (withdrawal cancel) |
| `GET` | `/wallets/{walletId}/balance` | `getBalance` | Query available + frozen |

---

## Idempotency

All mutations are idempotent on `(walletId, business_ref, tx_type)`:

| Case | Result |
|------|--------|
| Duplicate, same amount | Return existing tx, `idempotent_replay: true` |
| Duplicate, different amount | 409 `WALLET_DUPLICATE_CONFLICT` |
| Wallet LOCKED | 422 `WALLET_LOCKED` |

---

## creditWallet — Deposit flow

Called by `app-wallet-worker` after journal is POSTED (Phase B complete).
**FR-3: credit only after POSTED — never before.**

```json
POST /wallets/5001/credit
{
  "amount": "99000.0000",
  "business_ref": "dep-20260618-abc123",
  "tx_type": "DEPOSIT_CREDIT",
  "coa_trans_id": 9001
}
```

Response:
```json
{
  "wallet_tx_id": 7001,
  "available": "99000.0000",
  "frozen": "0.0000",
  "currency": "VND",
  "idempotent_replay": false
}
```

---

## debitWallet — Payment / Transfer

Called by `app-orchestration` in the sync path.

```json
POST /wallets/5001/debit
{
  "amount": "50000.0000",
  "business_ref": "pay-20260618-xyz789",
  "tx_type": "PAYMENT_DEBIT"
}
```

Fails with `WALLET_INSUFFICIENT_BALANCE` if `available < amount`.

---

## freeze / unfreeze — Withdraw

```json
POST /wallets/5001/freeze
{
  "amount": "200000.0000",
  "business_ref": "wdl-20260618-def456"
}
→ available decreases, frozen increases — same transaction
```

Unfreeze: reverses the freeze. Used when a withdrawal is cancelled after the freeze.

---

## provisionWallet — Onboarding

Idempotent on `(member_id, wallet_type, currency)`:

```json
POST /wallets
{
  "member_id": 1001,
  "wallet_type": "USER",
  "currency": "VND"
}
→ { "wallet_id": 5001, "member_id": 1001, "wallet_type": "USER", "currency": "VND", "status": "ACTIVE" }
```

---

## Balance Invariants

| Invariant | Rule |
|-----------|------|
| W1 | `available >= 0`, `frozen >= 0` always |
| W2 | Every balance change = 1 `wallet_tx` in the same DB transaction |
| W3 | Duplicate `business_ref` → same effect, no duplicate movement |
| W4 | Wallet services never mutate `coa_*` |

---

## Identity Map (s2-http-internal ↔ end-to-end)

| s2-http-internal field | End-to-end | DB column |
|------------------------|-----------|----------|
| `business_ref` | `businessRef` (s6-rabbitmq-cmds / s1-http-public) = `X-Idempotency-Key` | `wallet_tx.business_ref` |
| `coa_trans_id` | `coaTransId` từ WALLET_CREDIT command | `wallet_tx.coa_trans_id` (no FK) |

---

## Error Codes

| `error_code` | HTTP | Situation |
|-------------|------|-----------|
| `WALLET_NOT_FOUND` | 404 | walletId does not exist |
| `WALLET_INSUFFICIENT_BALANCE` | 422 | Debit/freeze > available |
| `WALLET_LOCKED` | 422 | Wallet is locked, mutation rejected |
| `WALLET_DUPLICATE_CONFLICT` | 409 | Same `business_ref` + different `amount` |

---

## Full contract

`specs/contracts/open-api/wallet-internal.yaml`
