# Wallet operations & tx_type

Orchestration passes: `memberId`, `walletType`, `amount`, `businessRef`, `txType`, optional `coaTransId`.

Wallet **never** recomputes fees.

## `WalletTxType` (locked)

| tx_type | direction | lane | when |
|---------|-----------|------|------|
| `DEPOSIT_CREDIT` | CREDIT | USER | after ledger POSTED |
| `PAYMENT_DEBIT` | DEBIT | USER | payment step 1 |
| `PAYMENT_CREDIT` | CREDIT | MERCHANT | payment step 3 |
| `TRANSFER_DEBIT` | DEBIT | USER | gross (principal+fee) |
| `TRANSFER_CREDIT` | CREDIT | USER | net |
| `WITHDRAW_FREEZE` | FREEZE | USER | accept |
| `WITHDRAW_SETTLE` | DEBIT | USER | bank OK (`:settle` ref) |
| `WITHDRAW_RELEASE` | UNFREEZE | USER | bank fail (`:release` ref) |
| `IBFT_FREEZE` | FREEZE | USER | accept |
| `IBFT_SETTLE` | DEBIT | USER | bank OK |
| `IBFT_RELEASE` | UNFREEZE | USER | bank fail |
| `PAYROLL_DEBIT` | DEBIT | MERCHANT | batch gross |
| `PARTNER_PREFUND_CREDIT` | CREDIT | PARTNER | escrow top-up |
| `DISBURSEMENT_DEBIT` | DEBIT | PARTNER | batch gross |
| `MERCHANT_SETTLE_CREDIT` | CREDIT | MERCHANT | optional at EOD |

No `WITHDRAW_DEBIT` — freeze→settle only.

## Per-flow wallet legs

| Flow | Wallet legs |
|------|-------------|
| Deposit | `DEPOSIT_CREDIT` after POSTED |
| Payment | `PAYMENT_DEBIT` → `PAYMENT_CREDIT` |
| Transfer | `TRANSFER_DEBIT` (gross) → `TRANSFER_CREDIT` (net) |
| Withdraw | `WITHDRAW_FREEZE` → `SETTLE` or `RELEASE` |
| IBFT | `IBFT_FREEZE` → `SETTLE` or `RELEASE` |
| Payroll | `PAYROLL_DEBIT` |
| Disbursement | `PARTNER_PREFUND_CREDIT`, `DISBURSEMENT_DEBIT` |
| QR/POS | none v1 (optional `MERCHANT_SETTLE_CREDIT` at EOD) |

## Errors

| Code | HTTP |
|------|------|
| `WALLET_INSUFFICIENT_BALANCE` | 422 |
| `WALLET_LOCKED` | 422 |
| `WALLET_DUPLICATE_CONFLICT` | 409 |
| `WALLET_NOT_FOUND` | 404 |

**S1 field binding:** [`surface-map.md`](./surface-map.md).
