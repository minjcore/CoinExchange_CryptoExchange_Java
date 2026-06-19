# Wallet Service TRD

> **CF page ID:** 43843585 | **Parent:** 📋 Domain TRDs (51183937)
> **Source of truth:** this file → push to CF
> **ADR authority:** ADR-002, ADR-004, ADR-005, ADR-026, ADR-029

---

## Overview

`core.wallet` is the runtime source of truth for **spendable balance per member**.
It answers: *How much can this member spend right now?*

`core.accounting` records aggregate liabilities (2110/2120/2130). They stay aligned through orchestration — never through shared tables or direct calls.

---

## Architecture Placement

```
Application (orchestration)
      │
      ▼
 core.wallet
 wallet, wallet_balance, wallet_tx
      │
      ▼
 core.shared (shared primitives only)
```

**Rules:**
- `core.wallet` must not import `core.accounting`, touch `coa_*`, or read `coa_trans`
- Wallet never posts the ledger
- `wallet_tx.coa_trans_id` = correlation only (no FK)

---

## Domain Model

### wallet

| Column | Type | Notes |
|--------|------|-------|
| `id` | BIGINT PK | Auto-increment. UUID bị loại: 2× storage index + random insert gây page split. |
| `member_id` | BIGINT | NOT NULL, indexed |
| `wallet_type` | ENUM | USER, MERCHANT, PARTNER |
| `currency` | CHAR(3) | VND v1 |
| `status` | ENUM | ACTIVE, LOCKED, CLOSED |

### wallet_balance (1:1 with wallet)

| Column | Type | Notes |
|--------|------|-------|
| `wallet_id` | FK → wallet.id PK | |
| `available` | DECIMAL(19,4) | Spendable, ≥ 0 |
| `frozen` | DECIMAL(19,4) | Held, ≥ 0 |
| `version` | BIGINT | Optimistic lock |

### wallet_tx (append-only)

| Column | Type | Notes |
|--------|------|-------|
| `id` | BIGINT PK | |
| `wallet_id` | FK | NOT NULL |
| `tx_type` | ENUM | |
| `direction` | ENUM | CREDIT, DEBIT, FREEZE, UNFREEZE |
| `amount` | DECIMAL(19,4) | Always positive |
| `business_ref` | VARCHAR(64) | Idempotency key |
| `coa_trans_id` | BIGINT | Correlation only, no FK |

**Idempotency uniqueness:** `(wallet_id, business_ref, tx_type)`

> **ID type: BIGINT (locked).** UUID bị loại — 16 bytes vs 8 bytes/FK index, random v4 gây B-tree page split, write chậm hơn ~50%. BIGINT sequential = optimal.

---

## WalletService Interface

```java
public interface WalletService {

    // FR-1 — idempotent on (memberId, walletType, currency)
    Wallet provision(long memberId, WalletType walletType, String currency);

    // FR-3 — credit available; duplicate businessRef → replay
    WalletTxResult credit(long walletId, BigDecimal amount,
                          String businessRef, WalletTxType txType);

    // FR-4 — throws InsufficientBalanceException if available < amount
    WalletTxResult debit(long walletId, BigDecimal amount,
                         String businessRef, WalletTxType txType);

    // FR-5 — txType = WITHDRAW_FREEZE (fixed)
    WalletTxResult freeze(long walletId, BigDecimal amount, String businessRef);

    // FR-5 — txType = WITHDRAW_RELEASE (fixed)
    WalletTxResult unfreeze(long walletId, BigDecimal amount, String businessRef);

    // Query
    WalletBalance getBalance(long walletId);
}

public record WalletTxResult(
    long walletTxId,
    BigDecimal available,
    BigDecimal frozen,
    boolean idempotentReplay  // true nếu businessRef đã tồn tại
) {}
```

**Dependencies:** `core.shared` only (`BigDecimal`, `WalletTxType`, `WalletType`). Zero framework imports.

---

## Repository Ports

Repository interfaces live in `core.wallet/port/` — pure Java, no JPA annotations.
Implemented by `app-wallet` using Spring Data JPA.

```java
public interface WalletRepository {

    // Provision — idempotent lookup
    Optional<Wallet> findById(long walletId);
    Optional<Wallet> findByMemberIdAndTypeAndCurrency(
            long memberId, WalletType type, String currency);

    // Save wallet + initial balance (provision)
    Wallet save(Wallet wallet);

    // Load balance with optimistic lock for mutation (credit / debit / freeze)
    // adapter increments wallet_balance.version on save — throws on stale read
    WalletBalance findBalanceForUpdate(long walletId);

    // Persist updated balance
    void updateBalance(WalletBalance balance);
}
```

```java
public interface WalletTxRepository {

    // Idempotency check before insert
    Optional<WalletTx> findByWalletIdAndBusinessRefAndTxType(
            long walletId, String businessRef, WalletTxType txType);

    // Append-only — no update / delete
    WalletTx save(WalletTx tx);
}
```

**Notes:**
- `WalletRepository` owns wallet + balance — same aggregate, same DB transaction.
- `WalletTxRepository` is append-only. `WalletService` only calls `save` after idempotency check passes.
- `findBalanceForUpdate` dùng **optimistic lock** — `wallet_balance.version` tăng mỗi lần update. Adapter ném exception khi version stale. Domain không import `@Lock` hay `@Version`.
- No Spring/JPA annotation in these interfaces. Zero framework imports.

---

## Functional Requirements

| FR | Rule |
|----|------|
| FR-1 Provision | Create `wallet` + `wallet_balance` (zero). Idempotent on `(member_id, wallet_type, currency)`. |
| FR-3 Credit | Increase `available`. Insert `wallet_tx CREDIT`. For deposit: credit only after POSTED. Duplicate `business_ref` → return existing. |
| FR-4 Debit | Require `available >= amount`. Decrease `available`. Insert `wallet_tx DEBIT`. |
| FR-5 Freeze | Move `available` → `frozen` in same tx. Unfreeze reverses. |

---

## Balance Invariants

| ID | Invariant |
|----|-----------|
| W1 | `available >= 0` and `frozen >= 0` always |
| W2 | Every balance change = exactly one new `wallet_tx` in same DB transaction |
| W3 | Same `business_ref` replay → same effect, no duplicate balance movement |
| W4 | Wallet services never mutate `coa_*` state |

---

## Idempotency & Errors

| Situation | Behavior |
|-----------|---------|
| Duplicate `business_ref`, same amount | Return existing `walletTxId` + current balance |
| Duplicate `business_ref`, different amount | Reject 409 conflict |
| Debit with insufficient balance | Reject, no partial debit |
| Wallet LOCKED | Reject debit + freeze; v1 rejects all balance changes except unlock |

**Error codes:** `WALLET_INSUFFICIENT_BALANCE`, `WALLET_NOT_FOUND`, `WALLET_LOCKED`, `WALLET_DUPLICATE_CONFLICT`
