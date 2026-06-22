# Core Platform Design — Foundation & Accounting (GtelPay Fund Flow)

> **CF page ID:** 42041345 | **Parent:** 🔌 Contracts & Integration (51315085)
> **Source of truth:** this file → push to CF

---

## Scope

Two parts:
1. **core.sharedlib** — shared base layer (pure Java, no domain logic)
2. **core.accounting** — COA, fund flow use cases, DR/CR postings

---

## Architecture Placement

```
Application (API, deploy)
      │
┌─────┴─────┐
▼           ▼
core.wallet  core.accounting
(wallet      (trans tables)
 tables)
      │
      └──────────┐
                 ▼
          core.sharedlib
                 │
                 ▼
               Java
```

---

## core.sharedlib Scope

**In:** `PageRequest`, `PageResult`, `ApiResponse`, `ErrorCode`, `BaseException`, pure util (id, time, hash)

**Not in:** entity, repository, service, controller, HTTP binding, DB/cache/MQ

---

## Domain Table Boundaries

| Module | Owns | Must not access |
|--------|------|----------------|
| `core.wallet` | `wallet`, `wallet_balance`, `wallet_tx` | `coa_*` tables |
| `core.accounting` | `coa_account`, `coa_trans`, `coa_trans_data` | `wallet_*` tables |

---

## Chart of Accounts (COA)

### Nhóm 1 — Tài Sản

| Mã TK | Tên |
|-------|-----|
| 1111 | TK Vietinbank — Chuyên dùng |
| 1112 | TK Napas Clearing |
| 1113 | TK VPBank — QR/POS |

### Nhóm 2 — Nợ Phải Trả

| Mã TK | Tên |
|-------|-----|
| 2110 | Wallet Balance — User |
| 2120 | Wallet Balance — Merchant |
| 2130 | Ký quỹ — Đối tác Chi hộ |

### Nhóm 3 — Transit

| Mã TK | Tên | Use case |
|-------|-----|---------|
| 3100 | Transit — Nạp tiền | Deposit |
| 3200 | Transit — Rút tiền | Withdraw |
| 3300 | Transit — Chuyển tiền nội bộ | Internal transfer |
| 3400 | Transit — IBFT | IBFT |
| 3500 | Transit — Thanh toán | Payment |
| 3600 | Transit — Chi Lương | Payroll |
| 3700 | Transit — Chi hộ | Disbursement |
| 3800 | Transit — Clearing | EOD settlement |

---

## Use Case: Deposit (Nạp tiền)

Gross 100,000 VND, fee 1,000 VND → net 99,000 VND vào wallet USER.

### Phase A — PENDING (TigerBeetle pending transfer)

| Step | Account | DR/CR | Amount |
|------|---------|-------|--------|
| 1 | 1111 — Vietinbank Nostro | DR | 100,000 |
| 2 | 3100 — Transit Deposit | CR | 100,000 |

### Phase B — POSTED (post_pending + 2 transfers)

| Step | Account | DR/CR | Amount |
|------|---------|-------|--------|
| 3 | 3100 — Transit Deposit | DR | 99,000 (net) |
| 4 | 2110 — Wallet USER | CR | 99,000 |
| 5 | 3100 — Transit Deposit | DR | 1,000 (fee) |
| 6 | 4110 — Fee Revenue | CR | 1,000 |

**After Phase B:** `1111 +100,000` | `2110 +99,000` | `4110 +1,000` | `3100 = 0` ✓

---

## Use Case: Payment (Thanh toán)

USER pays MERCHANT 50,000 VND, fee 500 VND.

| Step | Account | DR/CR | Amount | Timing |
|------|---------|-------|--------|--------|
| 1 | 2110 — Wallet USER | DR | 50,000 | Phase A (debit user) |
| 2 | 3500 — Transit Payment | CR | 50,000 | |
| 3 | 3500 — Transit Payment | DR | 49,500 (net) | Phase B |
| 4 | 2120 — Wallet MERCHANT | CR | 49,500 | |
| 5 | 3500 — Transit Payment | DR | 500 (fee) | |
| 6 | 4110 — Fee Revenue | CR | 500 | |

**After posting:** `2110 −50,000` | `2120 +49,500` | `4110 +500` | `3500 = 0` ✓

Flow: sync. orchestration → `app-accounting` (HTTP) → `app-wallet` (HTTP). No queue.

---

## Use Case: Transfer nội bộ (Internal Transfer)

Member A gửi 30,000 VND cho Member B, fee 0 VND.

| Step | Account | DR/CR | Amount |
|------|---------|-------|--------|
| 1 | 2110 — Wallet A (USER) | DR | 30,000 |
| 2 | 3300 — Transit Transfer | CR | 30,000 |
| 3 | 3300 — Transit Transfer | DR | 30,000 |
| 4 | 2110 — Wallet B (USER) | CR | 30,000 |

**After posting:** `Wallet A −30,000` | `Wallet B +30,000` | `3300 = 0` ✓

---

## Use Case: Withdraw (Rút tiền)

Member rút 200,000 VND ra ngân hàng, fee 2,000 VND.

### Sync accept (freeze)

| Step | Account | DR/CR | Amount |
|------|---------|-------|--------|
| 1 | 2110 — Wallet USER | DR | 200,000 |
| 2 | 3200 — Transit Withdraw | CR | 200,000 |

Wallet: `available −200,000` | `frozen +200,000`

### Async settle (khi bank confirm)

| Step | Account | DR/CR | Amount |
|------|---------|-------|--------|
| 3 | 3200 — Transit Withdraw | DR | 198,000 (net) |
| 4 | 1111 — Vietinbank Nostro | CR | 198,000 |
| 5 | 3200 — Transit Withdraw | DR | 2,000 (fee) |
| 6 | 4110 — Fee Revenue | CR | 2,000 |

**After settle:** `2110 −200,000` | `1111 −198,000` | `4110 +2,000` | `3200 = 0` ✓

---

## Transit Account Summary

| TK | Tên | Phải = 0 sau khi |
|----|-----|-----------------|
| 3100 | Transit Deposit | Mỗi deposit completed (Phase B done) |
| 3200 | Transit Withdraw | Mỗi withdrawal settled |
| 3300 | Transit Transfer | Mỗi transfer posted |
| 3500 | Transit Payment | Mỗi payment posted |

Transit = 0 là invariant CI-gated (INV-03, ADR-010, ADR-031).
