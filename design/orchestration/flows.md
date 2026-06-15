# Orchestration — step order

Sync = caller waits for terminal wallet+ledger state. Async = 202 + worker.

**Flow context:** đọc sau [`../platform/boundaries.md`](../platform/boundaries.md). DR/CR chi tiết → [`../../spec/processes.md`](../../spec/processes.md) §3–11. Behavior đầy đủ → [`../../design-v2/orchestration.md`](../../design-v2/orchestration.md).

## Deposit (async)

```
S1 webhook/notify → 202
  → RabbitMQ BANK_DEPOSIT
  → accounting: phase A PENDING (1111/3100)
  → accounting: confirmDeposit → POSTED
  → RabbitMQ WALLET_CREDIT (or consume JournalPosted → credit)
  → wallet: DEPOSIT_CREDIT (net amount from orchestration)
```

## Payment (sync)

```
wallet: PAYMENT_DEBIT (USER, gross)
  → accounting: post journal (3500=0)
  → wallet: PAYMENT_CREDIT (MERCHANT, netToMerchant)
  → S1 200
```

## Transfer (sync)

```
wallet: TRANSFER_DEBIT (A, gross = amount+fee)
  → accounting: post (3300=0, fee→4130)
  → wallet: TRANSFER_CREDIT (B, net)
  → S1 200
```

## Withdraw (wallet sync + bank async)

```
wallet: WITHDRAW_FREEZE (gross)
  → S1 200 ACCEPTED
  → RabbitMQ WITHDRAW_PAYOUT
  → bank adapter
  → success: WITHDRAW_SETTLE | fail: WITHDRAW_RELEASE
  → accounting: post withdraw journal when settle path completes
```

## IBFT

Same as withdraw pattern with `IBFT_*` tx types and transit 3400.

## Fee rule

Orchestration computes fee from config; passes net/gross to wallet and fee lines to accounting. Domains never derive fees.
