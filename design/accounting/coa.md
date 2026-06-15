# Chart of Accounts

**Sổ tay đầy đủ (~100 trang, Tiếng Việt):** [`../../design-v2/accounting/vol-02-coa-handbook.md`](../../design-v2/accounting/vol-02-coa-handbook.md) — 2.855 dòng, 23 TK

**Corpus 1000 trang:** [`../../design-v2/accounting/README.md`](../../design-v2/accounting/README.md)

DR/CR mẫu: [`postings.md`](./postings.md) · Canonical: [`../../spec/foundation.md`](../../spec/foundation.md) §6.

---

## Assets (111x)

| Code | Name |
|------|------|
| 1111 | Vietinbank — dedicated |
| 1112 | Napas Clearing |
| 1113 | VPBank — QR/POS |

## Liabilities (211x)

| Code | Name | Lane mirror |
|------|------|-------------|
| 2110 | Wallet balance — User | USER |
| 2120 | Wallet balance — Merchant | MERCHANT |
| 2130 | Escrow — Disbursement partner | PARTNER |

## Transit (31xx–38xx)

| Code | Use case |
|------|----------|
| 3100 | Deposit |
| 3200 | Withdraw |
| 3300 | Internal transfer |
| 3400 | IBFT |
| 3500 | Payment / QR-POS |
| 3600 | Payroll |
| 3700 | Disbursement |
| 3800 | EOD clearing lock |
| 3810 | EOD settlement outbound |
| 3820 | EOD MDR holdback |

## Revenue (41xx)

4110 deposit · 4120 withdraw · 4130 transfer/IBFT · 4140 MDR · 4150 payroll/disbursement

## Expense

5100 Bank / Napas fee

## Equity

6000 Owner's equity (system init)
