# core.wallet ↔ S1 OpenAPI (design map)

**Nguồn thiết kế** (đọc cạnh nhau):

| Artifact | Path |
|----------|------|
| S1 public HTTP | [`../orchestration/http-public.yaml`](../orchestration/http-public.yaml) (= [`spec/contracts/open-api/gtelpay-public.yaml`](../../spec/contracts/open-api/gtelpay-public.yaml)) |
| Wallet `tx_type` | [`operations.md`](./operations.md) |
| Wallet tables | [`model.md`](./model.md) |
| Step order | [`../orchestration/flows.md`](../orchestration/flows.md) |
| RabbitMQ credit | [`../messaging/commands.yaml`](../messaging/commands.yaml) → `WalletCreditPayload` |
| Kafka emit | [`events.yaml`](./events.yaml) → `WalletCredited` |
| Surface index | [`../../spec/integration-surfaces.md`](../../spec/integration-surfaces.md) §4, §7 |

Orchestration (BFF) implement S1; gọi `core.wallet` domain API — **wallet module không expose HTTP**.

---

## 1. Operation → wallet legs

| S1 `operationId` | HTTP | Wallet service | `WalletTxType` (step) | Sync? |
|------------------|------|----------------|------------------------|-------|
| `getWalletBalance` | `GET /wallets/balance` | `getBalance` | — (read `wallet_balance`) | 200 |
| `createPayment` | `POST /payments` | `debit` → `credit` | `PAYMENT_DEBIT` → `PAYMENT_CREDIT` | 200 |
| `createTransfer` | `POST /transfers` | `debit` → `credit` | `TRANSFER_DEBIT` → `TRANSFER_CREDIT` | 200 |
| `createWithdrawal` | `POST /withdrawals` | `freeze` | `WITHDRAW_FREEZE` | 200 |
| `notifyDeposit` | `POST /deposits/notify` | — on S1 | `DEPOSIT_CREDIT` after POSTED (worker) | 202 |
| `bankWebhook` | `POST /bank/webhook` | — on S1 | same as deposit | 202 |
| `getDepositStatus` | `GET /deposits/status` | — | poll only | 200 |

Withdraw bank leg: S6 `WITHDRAW_PAYOUT` → `WITHDRAW_SETTLE` / `WITHDRAW_RELEASE` ([`flows.md`](../orchestration/flows.md)).

Flows **không** có trên S1 v1: IBFT, Payroll, Disbursement, QR/POS — xem [`operations.md`](./operations.md) § Per-flow.

---

## 2. Request/response field → wallet DB

Idempotency: `X-Idempotency-Key` = body `businessRef` = `wallet_tx.business_ref` ([`../platform/idempotency.md`](../platform/idempotency.md)).

### 2.1 `getWalletBalance`

| OpenAPI | → Wallet |
|---------|----------|
| JWT → `memberId` (implicit) | `wallet.member_id` |
| query `walletType` (`USER` \| `MERCHANT`) | `wallet.wallet_type` |
| query `currency` | `wallet.currency` |
| `data.available` | `wallet_balance.available` (string decimal) |
| `data.frozen` | `wallet_balance.frozen` |
| `data.memberId`, `data.walletType`, `data.currency` | echo query + resolved member |

`WalletType` S1 **không** có `PARTNER` — lane nội bộ only.

### 2.2 `createPayment` — `PaymentRequest`

| OpenAPI field | Wallet command | Column / note |
|---------------|----------------|---------------|
| `businessRef` | both legs | `wallet_tx.business_ref` |
| `memberId` | `debit` | USER wallet |
| `amount` | `PAYMENT_DEBIT` | gross debit |
| `merchantId` | `credit` | MERCHANT wallet |
| `netToMerchant` (optional) | `PAYMENT_CREDIT` amount | default = `amount` |
| `currency` | both | `wallet.currency` |
| `data.walletTxId` (response) | debit leg id (convention) | `wallet_tx.id` |
| `data.coaTransId` | correlation | `wallet_tx.coa_trans_id` (optional, no FK) |

Step order: [`flows.md`](../orchestration/flows.md) Payment.

### 2.3 `createTransfer` — `TransferRequest`

Orchestration tính fee; wallet nhận amount đã tính ([`flows.md`](../orchestration/flows.md) Fee rule).

| OpenAPI field | Wallet command | Amount rule |
|---------------|----------------|-------------|
| `businessRef` | both legs | idempotency |
| `fromMemberId` | `TRANSFER_DEBIT` | USER |
| `toMemberId` | `TRANSFER_CREDIT` | USER |
| `amount` | credit net to B | net recipient |
| `feeAmount` | — (accounting fee lines) | debit gross = `amount` + `feeAmount` |
| `currency` | both | |

### 2.4 `createWithdrawal` — `WithdrawalRequest`

| OpenAPI field | Wallet command | Note |
|---------------|----------------|------|
| `businessRef` | `WITHDRAW_FREEZE` | |
| `memberId` | USER wallet | |
| `amount` | freeze amount | gross |
| `currency` | | |
| `useFreeze` | must be `true` v1 | `false` → reject (ADR-007 freeze→settle) |

Response `status`: `ACCEPTED` — chưa `WITHDRAW_SETTLE`.

### 2.5 Deposit (async) — `DepositNotifyRequest` / status poll

S1 không gọi wallet trực tiếp.

| Stage | Trigger | Wallet |
|-------|---------|--------|
| 1 | S1 **202** | — |
| 2 | S6 `BANK_DEPOSIT` | — (accounting) |
| 3 | journal POSTED | — |
| 4 | S6 `WALLET_CREDIT` or S3 `JournalPosted` | `DEPOSIT_CREDIT` |
| Poll | `getDepositStatus` → `WALLET_CREDITED` | `data.walletTxId` = `wallet_tx.id` |

S6 `WalletCreditPayload` → wallet ([`commands.yaml`](../messaging/commands.yaml)):

| Payload field | Wallet |
|---------------|--------|
| envelope `businessRef` | `wallet_tx.business_ref` |
| envelope `memberId` | USER wallet |
| `walletType` | `USER` \| `MERCHANT` \| `PARTNER` |
| `currency`, `amount` | mutation amount |
| `coaTransId` | `wallet_tx.coa_trans_id` |
| `txType` | e.g. `DEPOSIT_CREDIT` |

---

## 3. Errors (S1 ↔ wallet)

Từ [`operations.md`](./operations.md) § Errors — `ApiResponseError.data.errorCode`:

| `errorCode` | HTTP | Wallet cause |
|-------------|------|--------------|
| `WALLET_INSUFFICIENT_BALANCE` | 422 | debit/freeze exceeds `available` |
| `WALLET_LOCKED` | 422 | `wallet.status = LOCKED` |
| `WALLET_DUPLICATE_CONFLICT` | 409 | replay `(wallet_id, business_ref, tx_type)` |
| `WALLET_NOT_FOUND` | 404 | no row for `(member_id, wallet_type, currency)` |

---

## 4. Kafka emit (wallet → S3)

Sau commit CREDIT (và settle debit), adapter publish [`events.yaml`](./events.yaml) `WalletCredited`:

| Event field | Source |
|-------------|--------|
| `walletTxId` | `wallet_tx.id` |
| `memberId` | `wallet.member_id` |
| `amount` | `wallet_tx.amount` (string) |
| `businessRef` | `wallet_tx.business_ref` |
| `txType` | `wallet_tx.tx_type` enum name |

---

## 5. Schema cross-ref (OpenAPI ↔ design)

| OpenAPI schema (`http-public.yaml`) | Design doc |
|-------------------------------------|------------|
| `WalletType` | `model.md` → `wallet.wallet_type` (subset) |
| `WalletBalanceData` | `model.md` → `wallet` + `wallet_balance` |
| `PaymentRequest` | §2.2 + `operations.md` PAYMENT_* |
| `TransferRequest` | §2.3 + `operations.md` TRANSFER_* |
| `WithdrawalRequest` | §2.4 + `operations.md` WITHDRAW_* |
| `DepositNotifyRequest` | §2.5 + S6 `BankDepositPayload` |
| `ApiResponseDepositStatus` | poll states incl. `WALLET_CREDITED` |
