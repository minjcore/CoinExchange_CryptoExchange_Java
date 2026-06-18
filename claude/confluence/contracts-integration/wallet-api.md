# Wallet Internal API Contract

> **CF page ID:** 51610183 | **Parent:** 🔌 Contracts & Integration (51315085)
> **Source of truth:** `specs/contracts/open-api/wallet-internal.yaml`
> **Callers:** `app-orchestration` (sync), `app-wallet-worker` (async via s6-rabbitmq-cmds)

---

## Tổng quan

`app-wallet` expose 6 endpoints HTTP (Internal HTTP / s2-http-internal).
Không expose ra ngoài.

Deposit async path: `app-wallet-worker` nhận `WALLET_CREDIT` từ s6-rabbitmq-cmds → gọi `POST /wallets/{walletId}/credit`.
Sync path: `app-orchestration` gọi trực tiếp cho payment, transfer, balance read.

---

## Endpoints

| Method | Path | Operation | Use case |
|--------|------|-----------|---------|
| `POST` | `/wallets` | `provisionWallet` | Tạo wallet + zero balance (idempotent) |
| `POST` | `/wallets/{walletId}/credit` | `creditWallet` | Tăng available (deposit credit, transfer in) |
| `POST` | `/wallets/{walletId}/debit` | `debitWallet` | Giảm available (payment, transfer out) |
| `POST` | `/wallets/{walletId}/freeze` | `freezeWallet` | available → frozen (withdraw hold) |
| `POST` | `/wallets/{walletId}/unfreeze` | `unfreezeWallet` | frozen → available (withdraw cancel) |
| `GET` | `/wallets/{walletId}/balance` | `getBalance` | Query available + frozen |

---

## Idempotency

Mọi mutation idempotent trên `(walletId, business_ref, tx_type)`:

| Trường hợp | Kết quả |
|-----------|---------|
| Duplicate, same amount | Return existing tx, `idempotent_replay: true` |
| Duplicate, different amount | 409 `WALLET_DUPLICATE_CONFLICT` |
| Wallet LOCKED | 422 `WALLET_LOCKED` |

---

## creditWallet — Deposit flow

Gọi bởi `app-wallet-worker` sau khi journal POSTED (Phase B complete).
**FR-3: credit chỉ sau POSTED — không credit trước.**

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

Gọi bởi `app-orchestration` trong sync path.

```json
POST /wallets/5001/debit
{
  "amount": "50000.0000",
  "business_ref": "pay-20260618-xyz789",
  "tx_type": "PAYMENT_DEBIT"
}
```

Fails với `WALLET_INSUFFICIENT_BALANCE` nếu `available < amount`.

---

## freeze / unfreeze — Withdraw

```json
POST /wallets/5001/freeze
{
  "amount": "200000.0000",
  "business_ref": "wdl-20260618-def456"
}
→ available giảm, frozen tăng — cùng transaction
```

Unfreeze: reverse. Dùng khi withdraw bị cancel sau freeze.

---

## provisionWallet — Onboarding

Idempotent trên `(member_id, wallet_type, currency)`:

```json
POST /wallets
{
  "member_id": 1001,
  "wallet_type": "USER",
  "currency": "VND"
}
→ { "wallet_id": 5001, "status": "ACTIVE" }
```

---

## Balance Invariants

| Invariant | Rule |
|-----------|------|
| W1 | `available >= 0`, `frozen >= 0` always |
| W2 | Mỗi balance change = 1 `wallet_tx` trong cùng DB transaction |
| W3 | Duplicate `business_ref` → same effect, no duplicate movement |
| W4 | Wallet services không bao giờ mutate `coa_*` |

---

## Identity Map (s2-http-internal ↔ end-to-end)

| s2-http-internal field | End-to-end | DB column |
|------------------------|-----------|----------|
| `business_ref` | `businessRef` (s6-rabbitmq-cmds / s1-http-public) = `X-Idempotency-Key` | `wallet_tx.business_ref` |
| `coa_trans_id` | `coaTransId` từ WALLET_CREDIT command | `wallet_tx.coa_trans_id` (no FK) |

---

## Error Codes

| `error_code` | HTTP | Tình huống |
|-------------|------|-----------|
| `WALLET_NOT_FOUND` | 404 | walletId không tồn tại |
| `WALLET_INSUFFICIENT_BALANCE` | 422 | Debit/freeze > available |
| `WALLET_LOCKED` | 422 | Wallet bị lock, reject mutation |
| `WALLET_DUPLICATE_CONFLICT` | 409 | Same `business_ref` + khác `amount` |

---

## Full contract

`specs/contracts/open-api/wallet-internal.yaml`
