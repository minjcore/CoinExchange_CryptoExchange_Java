[Journal](https://www.moderntreasury.com/resources/journal)

•August 24, 2022(Updated August 14, 2025)

# Accounting For Developers, Part II: Ledgering for a Wallet App

In this second post of our three-part series, we build a ledger for a Venmo clone by applying the accounting principles we learned in Part I.

![Image of Lucas Rocha](https://www.moderntreasury.com/_next/image?url=https%3A%2F%2Fcdn.sanity.io%2Fimages%2F8nmbzj0x%2Fproduction%2F55163c3dc423adcb31083f7d47eca293dc95c46a-2875x2679.jpg&w=64&q=75)

Lucas Rocha/ Product Manager

Contents

Explore With AI

Topics

[Back Office](https://www.moderntreasury.com/resources/back-office) [Ledgering](https://www.moderntreasury.com/resources/ledgering)

## Introduction

Welcome back to our _Accounting for Developers_ series. If you missed the first part, we recommend starting with [Part I](https://www.moderntreasury.com/journal/accounting-for-developers-part-i), where we cover accounting foundations.

In this guide, we will walk through how to design the [ledger](https://www.moderntreasury.com/learn/what-is-a-ledger) for a Venmo-style digital wallet app. You'll see how to apply the [double-entry accounting](https://www.moderntreasury.com/learn/single-vs-double-entry-accounting) principles as we model user transfers, deposits, and withdrawals. We will also share how to structure this using a relational database.

If you’re curious about the API calls and system design considerations of designing a [digital wallet](https://www.moderntreasury.com/learn/digital-wallet) app, you can also check out our guide on [how to build a digital wallet](https://www.moderntreasury.com/journal/how-to-build-a-digital-wallet-product).

## Why a Double-Entry Ledger is Essential for Wallet Apps

To gain consistency, transparency, and correctness in a financial system, your architecture should:

- Model money flow using **accounts** and **transactions**
- Classify accounts as either **debit normal** or **credit normal**
- Enforce **double-entry** on every transaction (at least one debit + one credit)
- Ensure that total **debits = credits** across all entries (the aggregate balance of credit normal and debit normal accounts should net to zero)

This structure makes your system auditable, scalable, and resistant to bugs in payment flow.

## Designing a Ledger, Step 1: Define Product Requirements for the Wallet App

Let’s begin with what users should be able to do if the app works correctly:

- View their wallet account balance
- Add funds to their balance via card or bank payments
- Send money to (and receive money from) other users in the app
- Withdraw their balance into a bank account via ACH or instant payment
- Pay a small fee when they make a withdrawal from the app, to be deducted from their wallet balance

From a product perspective, we also want to:

- Distinguish user-specific balances and expose them to said users consistently and accurately
- Ensure the sum of all user balances equals the cash in our bank account
- Properly calculate and collect revenue from fees
- Account for a 3% card transaction processing fee for each deposit, paid by us

## Step 2: Designing the Chart of Accounts

With these requirements in mind, let’s map our [chart of accounts](https://www.moderntreasury.com/learn/chart-of-accounts) (COA). The COA is a simple depiction of the accounts we will need, their type, and normality:

![Sample Chart of Accounts - Venmo Clone](https://cdn.sanity.io/images/8nmbzj0x/production/08921d77a73dfa5666d9d6e8d6ab93ffb936ed91-1456x317.svg)

A simplified version of our wallet app's Chart of Accounts

To review:

- **Cash** represents funds we actually hold in our bank account. Because it represents an **asset** or **use** of funds, it's a **debit normal** account.
- **User's Wallet Balance** represents funds we hold on behalf of our users. Because users should be able to withdraw them at any time, they are funds we "owe"—or **liabilities**. Those funds are technically now available for our "use," meaning they are **sources** of funds, and thus **credit normal** accounts.
  - We need one User Balance account for each customer that creates an account with us.
- **Card Processing Fees** represent **expenses** or **uses** of funds; therefore, this is a **debit normal** account. This account’s balance will increase every time we pay off fees.
- **Revenue** from fees we collect in each transaction are **sources of funds**, so they are **credit normal** accounts.

## Step 3: Modeling Key Transactions

We should consider the typical events that will affect the ledger. For the sake of this example, we will model three core transaction types:

1. **Transfers**: User A sends money from their balance to User B.
2. **Deposits**: User A adds cash into their account balance. At the time of transfer, we need to account for the credit card processing fee. (Let’s assume, for the sake of this example, that credit card fees are paid by us.)
3. **Withdrawals**: User B withdraws from their account balance. We charge a fee when users withdraw from the app, deducted from their balance. At the time oftransfer, we need to account forour own service fee as revenue.

### Example 1. A Transfer

![Venmo Clone Transfer Map](https://cdn.sanity.io/images/8nmbzj0x/production/69189571c50d632fb22c08f4aed48996b507a7fe-1471x349.svg)

Mapped funds transfer of $100 from Art to Brittany

This example shows Art transferring $100 to Brittany. In this case, the transaction amount is debited (deducted) from Art’s Wallet (who’s initiating the transfer) and credited (added) to Brittany’s Wallet (who’s the receiver).

Note that this logic can be used for any in-app transfer—we just have to designate which wallet is initiating and which is receiving in each case. As marked in our COA above, User Wallets are **credit normal** accounts. If Brittany was sending money to Art, then Brittany’s balance would be debited (decrease), and Art’s balance would be credited (increase).

### Example 2. A Deposit

![Art's Deposit](https://cdn.sanity.io/images/8nmbzj0x/production/7ab6551c3ad12c76c01cf061fd4437f16e5b1fdb-1481x367.svg)

Art's Deposit of $300

In this model, three accounts are involved: Art's Wallet, Cash, and Card Processing Expenses(recall that for the sake of this example, our app is paying for card fees).

- Art deposits $300 in his wallet balance using a credit card.
- To counterbalance the $300 credit (increase) on Art’s Wallet, we need two debit entries:
  - One on the card processing fees account (increases by $6, or 2% of the transaction)
  - One on the cash account. Given we are recording this expense as paid off to our credit card vendor, our cash balance increases by $294 ($300-$6).

Without double-entry, we would need a way for the system to recognize all of the deposit transactions and properly account for card fees. By recording all of the money movement in a single event (in this case, a deposit) with multiple entries, we make sure our system is consistent. As debits equal credits, money in equals money out.

### Example 3. A Withdrawal

![Brittany's Withdrawal](https://cdn.sanity.io/images/8nmbzj0x/production/a68957a5c5b184da6816eb9b97c6618fb18a6923-1481x367.svg)

Brittany's Withdrawal of $500

A withdrawal is similar to a deposit, except that in this case, we are charging an extra fee from the user and recognizing it as revenue from fees. This transaction will decrease Brittany’s Wallet and Cash but will increase Revenue.

- Brittany withdraws $500 from her wallet balance (Brittany knows that she will pay a fee).
- Let’s assume that the fee is 0.5% of the withdrawal amount, or $2.50.
- Her user wallet gets deducted for $500 + $2.50, or $502.50
- We need to wire Brittany her money, so we add a credit entry to deduct our Cash account; however, we owe $2.50 less to Brittany, a small amount we can recognize as Revenue.

There are many different ways to model this. We could have chosen to have Brittany receive $497.50 ($500-$2.50), for example. In this case, we would add/credit the $2.50 we kept to revenue from fees similarly, but our cash would only decrease/credit by $497.50. The ledger would still balance. Thinking in terms of credit and debit normality gives you the flexibility to log transactions in the best way for your business.

## Step 4: Database Modeling and Application Logic

Let’s review the logical elements we would need to create to service this use case:

- One ledger object that represents the entire collection of accounts and transactions. All of our accounts and transactions should belong to a single ledger.
- At least four types of account objects:
  - User Wallets (one per user, credit normal)
  - Cash (single account, debit normal)
  - Revenue from Fees (single account, credit normal)
  - Card Processing Expenses (single account, debit normal)
- At least three modeled transactions
  - User Transfer
  - Deposit
  - Withdrawal

In dev terms:

- Accounts table fields: `id`, `user_id`, `account_type`, `normality`
- Transactions table fields: `id`, `timestamp`, `description`
- Entries table fields: `id`, `transaction_id`, `account_id`, `amount`, `direction`
- For entries, enforce: sum of debit amounts == sum of credit amounts, per transaction
- For each transaction type, create logic that writes 2+ entries with totals that net to zero (determine entry direction based on account type)

## Conclusion: Why Ledgers are Worth Building Right

By setting up the ledger as a double-entry system, we ensure that our Wallet App:

- **Scales without drift or inconsistency**
- **Produces accurate balances and reconciliation data**
- **Extends to new features with minimal refactoring** (as new product requirements come up or functionalities are rolled out, we can update our COA and the transaction models to represent them in the ledger appropriately)

**Up next:** Go on to [Part III](https://www.moderntreasury.com/journal/accounting-for-developers-part-iii) of this series, where we apply these principles to build a lending marketplace.

And remember, whatever you're building, you don't have to do it alone. Implementing a robust ledger from scratch is non-trivial, and it can be onerous for generic databases to reliably handle double-entry accounting. If you're developing a product that moves money, the opportunity cost of doing this in house can be high.

[Modern Treasury Ledgers](https://www.moderntreasury.com/products/ledgers) provides a production-ready, developer-friendly, double-entry ledger. [Reach out to us](https://www.moderntreasury.com/talk-to-us) to learn more.

Subscribe to our newsletter

Get the latest articles, guides, and insights delivered to your inbox.

Company Email\*

Subscribe

## Authors

![Image of Lucas Rocha](https://www.moderntreasury.com/_next/image?url=https%3A%2F%2Fcdn.sanity.io%2Fimages%2F8nmbzj0x%2Fproduction%2F55163c3dc423adcb31083f7d47eca293dc95c46a-2875x2679.jpg&w=3840&q=75)

Lucas RochaProduct Manager

Lucas Rocha is the PM on the Ledgers product, driving strategy for the company’s database for money movement. Before Modern Treasury, Lucas worked in VC at JetBlue Technology Ventures and Unshackled Ventures. He earned his MBA from Harvard Business School and his bachelor’s degree from Northeastern University.

Read more

Related

## Back Office

[View topic →](https://www.moderntreasury.com/resources/back-office)

[![Image for What is a Sweep Account?](https://www.moderntreasury.com/_next/image?url=https%3A%2F%2Fcdn.sanity.io%2Fimages%2F8nmbzj0x%2Fproduction%2F92504fa92e295cbada85b8295febef9e6a043361-1600x900.jpg&w=3840&q=75)](https://www.moderntreasury.com/learn/what-is-a-sweep-account)

[Glossary](https://www.moderntreasury.com/resources/glossary) [What is a Sweep Account?](https://www.moderntreasury.com/learn/what-is-a-sweep-account)

[![Image for What is an Issuer Identification Number (IIN)?](https://www.moderntreasury.com/_next/image?url=https%3A%2F%2Fcdn.sanity.io%2Fimages%2F8nmbzj0x%2Fproduction%2Fc017bd4dd320ee7c60e32382993687c59d1cec8a-1600x900.jpg&w=3840&q=75)](https://www.moderntreasury.com/learn/issuer-identification-number)

[Glossary](https://www.moderntreasury.com/resources/glossary) [What is an Issuer Identification Number (IIN)?](https://www.moderntreasury.com/learn/issuer-identification-number)

[![Image for ISO 20022](https://www.moderntreasury.com/_next/image?url=https%3A%2F%2Fcdn.sanity.io%2Fimages%2F8nmbzj0x%2Fproduction%2Fbd302bd13aae0398e5e77366f11fe1ea67f512fd-1600x900.jpg&w=3840&q=75)](https://www.moderntreasury.com/learn/iso-20022)

[Glossary](https://www.moderntreasury.com/resources/glossary) [ISO 20022](https://www.moderntreasury.com/learn/iso-20022)

[![Image for What is an Identity Verification API?](https://www.moderntreasury.com/_next/image?url=https%3A%2F%2Fcdn.sanity.io%2Fimages%2F8nmbzj0x%2Fproduction%2Fd7be0f811830c39997b2500d6fed47863559522b-1600x900.jpg&w=3840&q=75)](https://www.moderntreasury.com/learn/what-is-an-identity-verification-api)

[Glossary](https://www.moderntreasury.com/resources/glossary) [What is an Identity Verification API?](https://www.moderntreasury.com/learn/what-is-an-identity-verification-api)

What's New

## Latest Articles

[View all →](https://www.moderntreasury.com/journal)

[![Image for $100M in 100 Days: What we learned about Payments](https://www.moderntreasury.com/_next/image?url=https%3A%2F%2Fcdn.sanity.io%2Fimages%2F8nmbzj0x%2Fproduction%2Fa2cfd7e1804d43cb7f869d2f33f15b4bd7877b60-3840x2160.png&w=3840&q=75)](https://www.moderntreasury.com/journal/usd100m-in-100-days-what-we-learned-about-payments)

[$100M in 100 Days: What we learned about Payments](https://www.moderntreasury.com/journal/usd100m-in-100-days-what-we-learned-about-payments)

[![Image for Upgrading Postgres Clusters With Minimal Downtime](https://www.moderntreasury.com/_next/image?url=https%3A%2F%2Fcdn.sanity.io%2Fimages%2F8nmbzj0x%2Fproduction%2Fd71159f8eac461752587294d2f10bd723c2bd322-1920x1080.png&w=3840&q=75)](https://www.moderntreasury.com/journal/upgrading-postgres-clusters-with-minimal-downtime)

[Upgrading Postgres Clusters With Minimal Downtime](https://www.moderntreasury.com/journal/upgrading-postgres-clusters-with-minimal-downtime)

[![Image for Thoughts on the May 19 Executive Order on Fintech Integration](https://www.moderntreasury.com/_next/image?url=https%3A%2F%2Fcdn.sanity.io%2Fimages%2F8nmbzj0x%2Fproduction%2F10bf0c844cdebe0253f5c76aa7335e8303f7e7d6-3200x1800.png&w=3840&q=75)](https://www.moderntreasury.com/journal/thoughts-on-the-may-19-executive-order-on-fintech-integration)

[Thoughts on the May 19 Executive Order on Fintech Integration](https://www.moderntreasury.com/journal/thoughts-on-the-may-19-executive-order-on-fintech-integration)

[![Image for Why We Built Global USD Accounts](https://www.moderntreasury.com/_next/image?url=https%3A%2F%2Fcdn.sanity.io%2Fimages%2F8nmbzj0x%2Fproduction%2F0966ed98a4623cb61d1baafd25b7b0387b906f90-3200x1800.png&w=3840&q=75)](https://www.moderntreasury.com/journal/why-we-built-global-usd-accounts)

[Why We Built Global USD Accounts](https://www.moderntreasury.com/journal/why-we-built-global-usd-accounts)

We use cookies to improve your experience.By using our website, you’re agreeing to the collection of data described in our [Privacy Policy](https://www.moderntreasury.com/privacy).

Allow allDeny all

Show preferences