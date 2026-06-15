For AI agents: visit https://docs.moderntreasury.com/llms.txt for an index of all pages formatted in Markdown and endpoints in OpenAPI.

# Use Case Overview   [Skip link to Use Case Overview](https://docs.moderntreasury.com/ledgers/docs/digital-wallet-tutorial\#use-case-overview)

This tutorial will explain how to use **Modern Treasury’s** [Ledgers API](https://docs.moderntreasury.com/platform/reference/ledger-object#/) to build a digital wallet app. We’ll design a Ledger for a platform called **SendCash** that enables users to deposit money and send it to their friends.

In this guide, we’ll walk through design to implementation of a digital wallet ledger. We will do so by:

- Defining the most common transactions SendCash needs to record to the Ledger
- Creating the **Ledger** and **Ledger Accounts** (LA) required to support this use case
- Posting **Ledger Transactions** to change our LA balances, in response to user actions
- Future steps needed to get to a Production-ready application ledger

# Demo of Tutorial   [Skip link to Demo of Tutorial](https://docs.moderntreasury.com/ledgers/docs/digital-wallet-tutorial\#demo-of-tutorial)

You can click through a demo of this Tutorial below:

Digital Wallets — Ledger Demo

API Request

Digital Wallets

DepositTransferWithdrawal

Step 1 of 3

Deposit1. Deposit2. Transfer3. Withdrawal

| LEDGER ACCOUNT | ACCOUNT TYPE | DEBIT | CREDIT |
| --- | --- | --- | --- |

Press play to walk through each ledger transaction.

ResetShow API callView all entries

# Step 1. Defining Account and Transaction Logic   [Skip link to Step 1. Defining Account and Transaction Logic](https://docs.moderntreasury.com/ledgers/docs/digital-wallet-tutorial\#step-1-defining-account-and-transaction-logic)

Before we implement our ledger, we need to design our **accounts** and **transactions**.

**Ledger Accounts** represent the most specific balances that we need to track. Our digital wallet application requires that we track each end-user’s wallet balance as well as the total cash balance in our bank account. We’ll also track revenue earned from any direct transaction fees we charge.

| **Ledger Account Name** | **Normality** | Purpose |
| --- | --- | --- |
| **Cash** | Debit | Tracks total funds held in our custodian bank account. |
| **User Wallet(s)** | Credit | Tracks funds a specific user has available. One Ledger Account per user. |
| **Revenue** | Credit | Track revenue streams captured by our digital wallet product. |

**Ledger Transactions** represent the events that happen in our funds flow - what actions need to occur in SendCash's digital wallet, and how do they change our account balances above? The table below shows the most common user actions supported by digital wallet apps and how they should be recorded to the **Ledger**.

> 💡
>
> **Note**: For a refresher on account normality and how debits and credits work, review our [guide to debits and credits](https://docs.moderntreasury.com/ledgers/docs/guide-to-debits-and-credits). For more on how to design your chart of accounts, see the **[Learn More](https://docs.moderntreasury.com/ledgersdocs/digital-wallet-tutorial#learn-more-designing-your-chart-of-accounts)** section of this guide.

| **Sample Ledger Transaction** | **Debited Accounts** | **Credited Accounts** | **Notes** |
| --- | --- | --- | --- |
| **Deposit** | Cash account (increase) | User wallet account (increase) | When a user deposits funds to their account, we capture the increase in cash and corresponding increase in the user balance. |
| **In-App Transfer** | Sender's user wallet account (decrease) | Recipient's user wallet account (increase) | A transfer from user A to user B will reduce the sender's balance and increase the recipient's. |
| **Withdrawal** | User wallet account (decrease) | Cash account (decrease); Revenue from fees (increase) | This is the inverse flow to a deposit, with the exception that 2% of the withdrawal would be captured as Revenue. |

Your application code will write transactions to the **Ledger** when users take any of the actions above. Our team can help you structure your Ledger Transaction logic to meet your product and financial reporting requirements, and optimize app performance.

# Step 2. Create Your Ledger and Ledger Accounts   [Skip link to Step 2. Create Your Ledger and Ledger Accounts](https://docs.moderntreasury.com/ledgers/docs/digital-wallet-tutorial\#step-2-create-your-ledger-and-ledger-accounts)

Before we can post any **Ledger Transactions**, we’ll need to instantiate our Ledger.

Create a Ledger using the [Create Ledger endpoint](https://docs.moderntreasury.com/platform/reference/create-ledger) (`POST /ledgers`).

Shell

```shell
curl --request POST \
  -u ORGANIZATION_ID:API_KEY \
  --url https://app.moderntreasury.com/api/ledgers \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "SendCash Ledger",
    "description": "Represents our USD funds and User Balances"
  }'
```

Modern Treasury will return a **Ledger** object with a UUID to be used in subsequent steps.

Shell

```shell
{
    "id": "<ledger_id>",
    "object": "ledger",
    "name": "SendCash Ledger",
    "description": "Represents our USD funds and User Balances",
    "active": true,
    "metadata": {},
    "live_mode": true,
    "created_at": "2020-08-04T16:48:05Z",
    "updated_at": "2020-08-04T16:48:05Z"
}
```

Next, create the four **Ledger Accounts** required for our initial **Ledger Transactions** using the [Create Ledger Account endpoint](https://docs.moderntreasury.com/platform/reference/create-ledger-account#/) (`POST /ledger_accounts`).

> 💡
>
> **Note**: Our bank account balance is `normal_balance = debit` because it represents a cash balance that we hold, whereas the user wallet accounts are `normal_balance = credit` because they represent balances that we owe to our users.

Shell

```shell
curl --request POST \
  -u ORGANIZATION_ID:API_KEY \
  --url https://app.moderntreasury.com/api/ledger_accounts \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "Cash Account",
    "description": "Tracks our cash",
    "normal_balance": "debit",
    "currency": "USD",
    "ledger_id": "<ledger_id>"
  }'

curl --request POST \
  -u ORGANIZATION_ID:API_KEY \
  --url https://app.moderntreasury.com/api/ledger_accounts \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "Jane Doe Wallet",
    "description": "Tracks balance held on behalf of Jane Doe",
    "normal_balance": "credit",
    "currency": "USD",
    "ledger_id": "<ledger_id>"
  }'

curl --request POST \
  -u ORGANIZATION_ID:API_KEY \
  --url https://app.moderntreasury.com/api/ledger_accounts \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "John Doe Wallet",
    "description": "Tracks balance held on behalf of John Doe",
    "normal_balance": "credit",
    "currency": "USD",
    "ledger_id": "<ledger_id>"
  }'

curl --request POST \
  -u ORGANIZATION_ID:API_KEY \
  --url https://app.moderntreasury.com/api/ledger_accounts \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "Revenue",
    "description": "Tracks Revenue earned from fees",
    "normal_balance": "credit",
    "currency": "USD",
    "ledger_id": "<ledger_id>"
  }'
```

Modern Treasury will return the four **Ledger Accounts** you've created. Each has their own UUID that we can use in our **Ledger Transactions**.

Shell

```shell
{
    "id": "<cash_account_id>",
    "object": "ledger_account",
    "name": "Cash Account",
    "ledger_id": "<ledger_id>",
    "description": "Tracks our cash",
    "normal_balance": "debit",
    "currency": "USD",
    "currency_exponent": 2,
    "active": true,
    "metadata": {},
    "live_mode": true,
    "created_at": "2020-08-04T16:54:32Z",
    "updated_at": "2020-08-04T16:54:32Z"
}

{
    "id": "<jane_wallet_account_id>",
    "object": "ledger_account",
    "name": "Jane Doe Wallet",
    "ledger_id": "<ledger_id>",
    "description": "Tracks balance held on behalf of Jane Doe",
    "normal_balance": "credit",
    "currency": "USD",
    "currency_exponent": 2,
    "active": true,
    "metadata": {},
    "live_mode": true,
    "created_at": "2020-08-04T16:54:32Z",
    "updated_at": "2020-08-04T16:54:32Z"
}

{
    "id": "<john_wallet_account_id>",
    "object": "ledger_account",
    "name": "John Doe Wallet",
    "ledger_id": "<ledger_id>",
    "description": "Tracks balance held on behalf of John Doe",
    "normal_balance": "credit",
    "currency": "USD",
    "currency_exponent": 2,
    "active": true,
    "metadata": {},
    "live_mode": true,
    "created_at": "2020-08-04T16:54:32Z",
    "updated_at": "2020-08-04T16:54:32Z"
}

{
    "id": "<revenue_account_id>",
    "object": "ledger_account",
    "name": "Revenue",
    "ledger_id": "<ledger_id>",
    "description": "Tracks Revenue earned from fees",
    "normal_balance": "credit",
    "currency": "USD",
    "currency_exponent": 2,
    "active": true,
    "metadata": {},
    "live_mode": true,
    "created_at": "2020-08-04T16:54:32Z",
    "updated_at": "2020-08-04T16:54:32Z"
}
```

# Step 3. Record Ledger Transactions   [Skip link to Step 3. Record Ledger Transactions](https://docs.moderntreasury.com/ledgers/docs/digital-wallet-tutorial\#step-3-record-ledger-transactions)

Now that we’ve set up our **Ledger** and created some critical **Ledger Accounts**, let’s write to the **Ledger** to record some user actions.

First, create a **Ledger Transaction** using the [Create Ledger Transaction endpoint](https://docs.moderntreasury.com/platform/reference/create-ledger-transaction) (`POST /ledger_transactions`) to record that Jane Doe has deposited $100 in her user wallet account. This records that $100 has entered our bank account (asset), and that we’re now holding $100 on behalf of Jane Doe (liability).

Shell

```shell
curl --request POST \
  -u ORGANIZATION_ID:API_KEY \
  --url https://app.moderntreasury.com/api/ledger_transactions \
  -H 'Content-Type: application/json' \
  -d '{
    "description": "Jane Doe cash deposit",
    "effective_at": "2020-08-27",
    "status": "posted",
    "external_id": "<cash_deposit_id>",
    "ledger_entries": [\
      {\
        "amount": 10000,\
        "direction": "debit",\
        "ledger_account_id": "<cash_account_id>"\
      },\
      {\
        "amount": 10000,\
        "direction": "credit",\
        "ledger_account_id": "<jane_wallet_account_id>"\
      }\
    ]
  }'
```

Next, Jane uses our app to send $50 to John Doe's wallet. No cash transfer has to occur: we simply record that a portion of the cash we’re holding is now owed to a different user.

Create the following **Ledger Transaction** to record this transfer of funds between Jane and John’s user wallet accounts, with 2% of the transaction, or $1, earned as revenue.

Shell

```shell
curl --request POST \
  -u ORGANIZATION_ID:API_KEY \
  --url https://app.moderntreasury.com/api/ledger_transactions \
  -H 'Content-Type: application/json' \
  -d '{
    "description": "Jane Doe wallet transfer to John Doe",
    "effective_at": "2020-08-29",
    "status": "posted",
    "external_id": "<wallet_transfer_id>",
    "ledger_entries": [\
      {\
        "amount": 4900,\
        "direction": "credit",\
        "ledger_account_id": "<john_wallet_account_id>"\
      },\
      {\
        "amount": 5000,\
        "direction": "debit",\
        "ledger_account_id": "<jane_wallet_account_id>"\
      },\
      {\
        "amount": 100,\
        "direction": "credit",\
        "ledger_account_id": "<revenue_account_id>"\
      }\
    ]
  }'
```

Finally, John Doe withdraws $49 from his wallet. Create a **Ledger Transaction** recording that $49 has left our bank account, and that $49 is no longer held on behalf of John.

Shell

```shell
curl --request POST \
  -u ORGANIZATION_ID:API_KEY \
  --url https://app.moderntreasury.com/api/ledger_transactions \
  -H 'Content-Type: application/json' \
  -d '{
    "description": "John Doe cash withdrawal",
    "effective_at": "2020-08-30",
    "status": "posted",
    "external_id": "<john_withdrawal_id>",
    "ledger_entries": [\
      {\
        "amount": 4900,\
        "direction": "credit",\
        "ledger_account_id": "<cash_account_id>"\
      },\
      {\
        "amount": 4900,\
        "direction": "debit",\
        "ledger_account_id": "<john_wallet_account_id>"\
      }\
    ]
  }'
```

> 💡
>
> **Note**: At the end of this set of Ledger Transactions, we still have an additional $1 in our bank account representing the revenue we have earned from fees.

# Step 4. Read Ledger Account Balances   [Skip link to Step 4. Read Ledger Account Balances](https://docs.moderntreasury.com/ledgers/docs/digital-wallet-tutorial\#step-4-read-ledger-account-balances)

**Modern Treasury’s Ledger** serves as your consistent source of truth for your user transactions and balances at scale. For example, you'll query the **Ledger** for a user's wallet balance in order to display it in their app experience when they log in.

When Jane wants to see her wallet balance, your app can query the Ledgers API, using just the UUID for Jane’s user wallet account.

To test this, run [Get Ledger Account endpoint](https://docs.moderntreasury.com/platform/reference/get-ledger-transaction) on Jane’s wallet Ledger Account id (`GET /ledger_accounts/{jane_wallet_account_id}`.

Shell

```shell
curl --request GET \
  -u ORGANIZATION_ID:API_KEY \
   --url https://app.moderntreasury.com/api/ledger_accounts/61574fb6-7e8e-403e-980c-ff23e9fbd61b
```

This will return Jane's live wallet balance, which you can display in your app.

Shell

```shell
{
  "id":"61574fb6-7e8e-403e-980c-ff23e9fbd61b",
  "object":"ledger_account",
  "live_mode":true,
  "name":"Jane Doe Wallet",
  "ledger_id": "<jane_wallet_account_id>",
  "description": "Tracks balance held on behalf of Jane Doe",
  "lock_version":2,
  "normal_balance":"credit",
  "balances":{
    "pending_balance":{
      "credits":10000
      "debits":5000
      "amount":5000
      "currency":"USD",
      "currency_exponent":2
    },
    "posted_balance":{
      "credits":10000
      "debits":5000
      "amount":5000
      "currency":"USD",
      "currency_exponent":2
      },
    "available_balance":{
      "credits":10000
      "debits":5000
      "amount":5000
      "currency":"USD",
      "currency_exponent":2
    }
  },
  "metadata":{},
  "discarded_at":NULL,
  "created_at": "2020-08-04T16:54:32Z",
  "updated_at": "2020-08-04T17:23:12Z"
}
```

# Next Steps: Advanced Topics   [Skip link to Next Steps: Advanced Topics](https://docs.moderntreasury.com/ledgers/docs/digital-wallet-tutorial\#next-steps-advanced-topics)

Up to this point, we’ve covered the basics of creating **Ledger Accounts** and **Ledger Transactions** to record user actions in the Ledger, but we’re just scratching the surface of what it takes to build a Production-ready application ledger.

Below are some more Advanced Topics you should explore as you build out your Ledger design

## Attaching Metadata   [Skip link to Attaching Metadata](https://docs.moderntreasury.com/ledgers/docs/digital-wallet-tutorial\#attaching-metadata)

**Modern Treasury Ledgers** supports attaching free-form metadata to most objects, in the form of key-value pairs. We’ve seen our digital wallet customers attach some of the following metadata tags:

| Object | Metadata |
| --- | --- |
| Ledger | `productID` |
| Ledger Account | `walletID`; `userId`; `accountType` |
| Ledger Transaction | `transactionType` |

**Modern Treasury Ledgers** supports querying based on metadata. By using the [List Ledger Transactions endpoint](https://docs.moderntreasury.com/ledgers/reference/list-ledger-transactions) to query all transactions associated with User Wallet #31512, for instance, the system will return all the transactions that modified this account in the requested time period.

## Defining Ledger Account Categories   [Skip link to Defining Ledger Account Categories](https://docs.moderntreasury.com/ledgers/docs/digital-wallet-tutorial\#defining-ledger-account-categories)

Oftentimes, we need to retrieve a real-time balance that is the sum of all **Ledger Accounts** of a particular type.

For example, we’ll often want to review the sum of all User Balance Accounts to compare against a Bank Account balance, ensuring that all the funds in that Bank Account are properly accounted for.

Use cases like these are why we built [Ledger Account Categories](https://docs.moderntreasury.com/ledgers/docs/ledger-account-categories-overview). This enables you to easily “roll up” balances that comprise many Ledger Accounts, providing real-time access to aggregate balances to meet any UI or reporting needs.

| Ledger Account Category | Normality | Description | Contains the following Ledger Accounts |
| --- | --- | --- | --- |
| **Total User Balance** | Credit Normal | Sums all Ledger account balances across all user digital wallets. Balance represents total money SendCash is holding on behalf of users. | - User #1241241 Balance |
| **Total Expenses** | Credit Normal | Sums fees paid and amount payable to vendors. Balance represents total expenses SendCash is liable for. | - Credit card fees paid<br>- Payable to Vendor X |
| **Total Revenue** | Credit Normal | Sums deposit and transfer fees across all SendCash transactions. Balance represents total SendCash revenue. | - Revenue from deposit fees<br>- Revenue from transfer fees |
| **Total Operating Cash** | Debit Normal | Sums amount in operating cash accounts used for SendCash business. | - SendCash Operating Cash Account |

## Balance Locking   [Skip link to Balance Locking](https://docs.moderntreasury.com/ledgers/docs/digital-wallet-tutorial\#balance-locking)

You can make each Ledger Transaction **conditional** on the current balance of the Ledger Account.

You can include balance filters with a Ledger Entry to validate the corresponding Ledger Account's balance when creating or updating a transaction. If the transaction would push the account balance outside the specified range, the request will fail with a 422 error.

Available balance filter types include `pending_balance_amount`, `posted_balance_amount`, and `available_balance_amount`. You can filter balances using the operators `gt` (>), `gte` (>=), `eq` (=), `lte` (<=), and `lt` (<).

If Jane wants to send $50 to John (like described in the previous section), but only if Jane stays ≥ $0 `available_balance` on her Ledger Account, the `POST /ledger_transactions` call would read as follows:

Shell

```shell

curl --request POST \
  -u ORGANIZATION_ID:API_KEY \
  --url https://app.moderntreasury.com/api/ledger_transactions \
  -H 'Content-Type: application/json' \
  -d '{
    "description": "Jane Doe wallet transfer to John Doe",
    "effective_at": "2020-08-29",
    "status": "posted",
    "external_id": "<wallet_transfer_id>",
    "ledger_entries": [\
      {\
        "amount": 5000,\
        "direction": "credit",\
        "ledger_account_id": "<john_wallet_account_id>",\
      },\
      {\
        "amount": 5000,\
        "direction": "debit",\
        "ledger_account_id": "<jane_wallet_account_id>",\
	      "available_balance_amount": {"gte": 0}\
      },\
      ,\
      {\
        "amount": 100,\
        "direction": "credit",\
        "ledger_account_id": "<revenue_account_id>"\
      }\
    ]
  }'
```

# Learn More: Designing Your Chart of Accounts   [Skip link to Learn More: Designing Your Chart of Accounts](https://docs.moderntreasury.com/ledgers/docs/digital-wallet-tutorial\#learn-more-designing-your-chart-of-accounts)

Ledgers enforces double-entry accounting concepts, to ensure scalable consistency. If you are not familiar with debits and credits, start with our [guide to debits and credits](https://docs.moderntreasury.com/ledgers/docs/guide-to-debits-and-credits). We also recommend reviewing our [guide to Ledgers Objects](https://docs.moderntreasury.com/ledgers/docs/guide-to-ledger-objects), as it explains Ledger Accounts, Transactions, and Categories in detail.

Balances are tracked in the Ledger by way of Ledger Accounts. Implementing a digital wallet often requires the following sets of accounts:

- **User Balance Accounts:**
  - Balances held on behalf of particular users, available for sending or withdrawal;
  - Surfaced to users in UI, used to validate user transactions
- **Cash Accounts:**
  - Cash balances associated with your digital wallet product;
  - Used for financial reporting and bank account reconciliation
- **Expense Accounts:**
  - Track fees incurred in the operation of digital wallet product;
  - Used for financial reporting as well
- **Revenue Accounts:**
  - Track revenue streams captured by your digital wallet product

## User Balance Accounts   [Skip link to User Balance Accounts](https://docs.moderntreasury.com/ledgers/docs/digital-wallet-tutorial\#user-balance-accounts)

User Balance Accounts are **credit normal** accounts that track user balances. These are credit normal accounts because they represent funds your platform **owes to other parties** \- or sources of funds. (For more, read our [guide to debits and credits](https://docs.moderntreasury.com/ledgers/docs/guide-to-debits-and-credits)).

Your platform needs one Ledger Account per user. New accounts can be created using the [Create Ledger Account endpoint](https://docs.moderntreasury.com/ledgers/reference/create-ledger-account). Ledger Accounts (like all Modern Treasury objects) can be enriched with free-form metadata. Here is a sample User Balance Account:

| **Account Name** | **Normality** | **Represents** | **Increased By (Credits)** | **Decreased By (Debits)** | **Sample Metadata** |
| --- | --- | --- | --- | --- | --- |
| User #1241241 Balance | Credit Normal | Balance for user John Doe. | User deposits or credits. | User withdrawals, fees. | `accountType: "User Balance",                                                                                                                                                                                                                                                                                   userFirstName: "John",                                                                                                                                                                                                                                                                                   userLastName: "Doe",                                                                                                                                                                                                                                                                                   userState: "NY",                                                                                                                                                                                                                                                                                   userActive: yes` |
| User #1241242 Balance | Credit Normal | Balance for user Jane Doe. | User deposits or credits. | User withdrawals, fees. | `accountType: "User Balance",                                                                                                                                                                                                                                                                                   userFirstName: "Jane",                                                                                                                                                                                                                                                                                   userLastName: "Doe",                                                                                                                                                                                                                                                                                   userState: "NY",                                                                                                                                                                                                                                                                                   userActive: yes` |

## Cash Accounts   [Skip link to Cash Accounts](https://docs.moderntreasury.com/ledgers/docs/digital-wallet-tutorial\#cash-accounts)

Cash Accounts track different cash positions associated with the digital wallet app. They are **debit normal** given they represent funds your platform **owns**.

There are many different cash positions you might want to track in your digital wallet app:

- Reserve funds that hold cash available for user withdrawals
- Operating bank accounts holding pools of funds you direct income to and deduct expenses from
- Any other cash pools tied to operational or regulatory requirements

Here is a sample Cash Account:

| Account Name | Normality | Represents | Increased By (Debits) | Decreased By (Credits) | Sample Metadata |
| --- | --- | --- | --- | --- | --- |
| SendCash Operating Cash Account | Debit Normal | Overall cash position of SendCash app | Cash inflows of any kind | Cash outflows of any kind | `accountType: "Cash Omnibus",  active: yes` |

## Expense Accounts   [Skip link to Expense Accounts](https://docs.moderntreasury.com/ledgers/docs/digital-wallet-tutorial\#expense-accounts)

Expense Accounts track expenses incurred in the regular operation of the digital wallet app. These can be **debit normal** when they represent expenses **paid** (as they reflect money outflows in a regular course of business) and **credit normal** when they represent expenses **due** (as they reflect payables).

A few types of expense accounts include:

- Card fees incurred in the event of an account deposit or withdrawal
- Any taxes or fees to third parties paid on balances or transfers in the platform
- Banking fees associated with specific transactions

Here are two sample Expense Accounts:

| **Account Name** | **Normality** | **Represents** | **Increased By (Debits)** | **Decreased By (Credits)** | **Sample Metadata** |
| --- | --- | --- | --- | --- | --- |
| Credit card fees paid | Debit Normal | Total paid in credit card fees | New credit card fees paid | Typically not decreased except in the event of an adjusting entry | `accountType: "expense"` |
| Payable to Vendor X | Credit Normal | Balance due to Vendor X | New amounts are recorded as due to Vendor X | Payouts (i.e. settlement of Payable) | `accountType: "expense",                                                                                                                                                                                                                                                                                   vendor: "X"` |

## Revenue Accounts   [Skip link to Revenue Accounts](https://docs.moderntreasury.com/ledgers/docs/digital-wallet-tutorial\#revenue-accounts)

Revenue Accounts track money inflows categorized as Revenue by your digital wallet app. They are always **credit normal** accounts.

Different Revenue Accounts can be created to differentiate between revenue streams.

Here are two sample Revenue Accounts:

| **Account Name** | **Normality** | **Represents** | **Increased By (Credits)** | **Decreased By (Debits)** | **Sample Metadata** |
| --- | --- | --- | --- | --- | --- |
| Revenue from deposit fees | Credit Normal | Revenue incurred when users deposit funds in the app | Deposits | Typically ledger adjustments (e.g. refund or cancellation) | `accountType: "revenue",                                                                                                                                                                                                                                                                                   revenueStream: "deposit_fees"` |
| Revenue from transfer fees | Credit Normal | Revenue incurred when users transfer funds from each other in the platform | User to user transfers | Typically ledger adjustments (e.g. transfer cancellation) | `accountType: "revenue",                                                                                                                                                                                                                                                                                   revenueStream: "transfer_fees"` |

> 💡
>
> **Note**: In digital wallet or similar applications, you may instead charge usage or service fees separate from money movement, i.e. as an accruing invoice. In this case, as activity happens, you may accrue an outstanding invoice amount against two alternate accounts:
>
> - User Receivable (debit normal): A receivable account tracking how much a user owes for fees. You can optionally create an account per invoice.
> - Revenue (credit normal): Total revenue earned from fees. You can optionally track this amount as unreceived or unearned revenue until the fee has been paid.

Updated23 days ago

* * *

Copy Page