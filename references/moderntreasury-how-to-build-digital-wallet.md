[Journal](https://www.moderntreasury.com/resources/journal)

•April 29, 2021(Updated September 25, 2025)

# How to Build a Digital Wallet Product

Digital wallets are some of the most widely-used financial products today. This guide describes the bank and payments infrastructure underlying digital wallets and how you could build one using Modern Treasury.

![Image of Pranav Deshpande](https://www.moderntreasury.com/_next/image?url=https%3A%2F%2Fcdn.sanity.io%2Fimages%2F8nmbzj0x%2Fproduction%2Fd03171929208a178ac7371eebc93d4997c2eedbe-500x458.jpg&w=64&q=75)

Pranav Deshpande/ PMM

![Image of Chris Frakes](https://www.moderntreasury.com/_next/image?url=https%3A%2F%2Fcdn.sanity.io%2Fimages%2F8nmbzj0x%2Fproduction%2Fd9398e9bdadf4d1712be1aa1f7374ff5ecbca466-2030x2030.jpg&w=64&q=75)

Chris Frakes/ Head of Content

Contents

Explore With AI

Topics

[Ledgering](https://www.moderntreasury.com/resources/ledgering)

## Why Build a Digital Wallet?

Companies like Apple, Starbucks, Cash App, and Splitwise have shown that embedding wallet functionality into products unlocks new business models, improves customer experience, and creates more control over money movement. It's why Juniper Research predicts that [over two-thirds of the global population](https://www.juniperresearch.com/resources/free-research/why-56-billion-people-will-adopt-digital-wallets/) (5.6 billion people) will have at least one digital wallet by 2029.

More product teams are choosing to bring payments in-house—either to control the user experience, reduce third-party fees, or enable features like balance-based spending and instant payouts. Wallets can help:

- Lower costs by moving funds over ACH or RTP instead of card rails
- Improve cash flow through balance preloading
- Increase loyalty and retention through faster, smoother payment experiences
- Centralize transaction data and simplify reconciliation

In this guide, we’ll walk through how to set up the core infrastructure behind a digital wallet. These steps cover the concepts of underlying architecture of a wallet, creating a ledger, provisioning user-level ledger accounts, connecting external accounts, and enabling money movement.

## Considerations for Your Digital Wallet: Architecture

When building a digital wallet, the first technical decisions often revolve around banks, payment rails, and ledgers. But none of that matters if the user experience doesn’t work. For products that move money—especially wallets—it’s the user-facing parts that shape trust: signup flows, balance visibility, and payout timing.

Behind every good wallet UX is a robust payment ops setup. To support real-time balances, accurate reporting, and regulatory compliance, you’ll need to make key architectural decisions about how funds are held, tracked, and reconciled. Here’s how to approach building the payments infrastructure for a product like Pay2Day.

### **Step 1**: Decide on the structure of your underlying bank account.

The most straightforward option would be to [set up an FBO](https://www.moderntreasury.com/journal/when-and-how-to-get-an-fbo-account), or “For Benefit of,” account with your bank partner. An FBO account allows your company to hold and manage funds on behalf of their users, without assuming legal ownership of those funds—an important distinction when serving consumers or contractors at scale. Funds held in an FBO account are legally attributed to the end-users but operationally controlled by the platform.

An FBO account offers regulatory coverage, helping companies avoid the cumbersome process of becoming a money services business (MSB); though, this is another option. Instead, they can attribute ownership of the account to the bank’s EIN, or their tax ID, to avoid these regulations. Banks are de facto money transmitters, so they don’t have to worry about registering for a Money Transmitter License (MTL), a time-consuming process that varies from state to state.

Aside from regulatory coverage, businesses may also open an FBO account for insurance purposes. In some cases, you can have up to $250,000 in FDIC insurance on each virtual account under an FBO account. For neobanks, this gives clients a personalized experience with insurance benefits.

### **Step 2:** Decide on a mechanism for issuing separate accounts for each user.

To isolate funds and enable direct inflows and outflows for individual users, you’ll want to issue [virtual accounts](https://www.moderntreasury.com/learn/what-are-virtual-accounts) (also known as subsidiary accounts or sub-accounts) under the umbrella of the FBO account (often referred to as the omnibus account). Each virtual account has its own account and routing number, making it possible to:

- Streamline incoming and outgoing transaction data
- Attribute incoming deposits to specific users
- Simplify reconciliation with unique identifiers

Virtual accounts function similarly to standard bank accounts. The most notable difference is that virtual accounts cannot _actually_ hold money. They receive it, collect necessary information about the sender, and pass it over to the omnibus FBO account. It’s advantageous to pair them with a double-entry ledger to track all of your business transactions.

### **Step 3:** Implement a Double-Entry Ledger for Tracking Transactions and Balances

A ledger is a timestamped log of events that have a monetary impact, ensuring:

- [ACID](https://www.moderntreasury.com/learn/what-is-acid) compliance, including Immutability and traceability of transactions
- Clean audit logs for regulatory and accounting needs
- Accurate balances exposed to the end-user

Because the balance of your omnibus FBO account only reflects the aggregate balance held across all users, this ledger helps maintain accurate balances at the user (virtual account) level. These accounts are classified as debit normal or credit normal.

Transactions recorded in the ledger have two or more entries, which belong to an account.

- Entries change balances based on account type and entry direction.
  - **Debits**—or entries on the debit side—increase the balance of debit normal accounts, while credits decrease it.
  - **Credits**—or entries on the credit side—increase the balance of credit normal accounts, while debits decrease it.
- The system is correct if the sum of balances of all credit normal accounts matches the sum of balances of all debit normal accounts. This means all money is properly accounted for.

How can you apply this with software? To implement double-entry accounting in code, you’ll need:

- **Ledger Object:** Represents the entire system of accounts and transactions
- **Account Object**
  - Has a normality: debit or credit
  - Represents a balance bucket
- **Transaction Object**
  - Contains two or more entries
  - Must be balanced: debits = credits
- **Entry Object**
  - Belongs to one transaction and one account
  - Includes amount and direction (debit or credit)

Modern Treasury [partners with several banks](https://docs.moderntreasury.com/docs/banks) that provide FBO accounts with virtual account capabilities. With our [Virtual Accounts API](https://docs.moderntreasury.com/docs/virtual-accounts), you can:

- Programmatically issue accounts to users directly from your app
- Receive unique account/routing numbers per user
- Monitor deposits and activity in real-time

In addition, our [Ledgers API](https://docs.moderntreasury.com/docs/ledgers-overview), lets you:

- Create double-entry ledger accounts for every wallet user
- Post ledger transactions for deposits, withdrawals, and transfers
- Query balances and transaction histories

## Understanding the UX: A Wallet User Flow

Let’s say you’re building a wallet called Pay2Day designed for gig workers: food delivery couriers, rideshare drivers, and other shift-based earners. Pay2Day’s value prop is providing access to earned wages daily, so gig workers don’t need to wait until the end of the paycycle.

To do this, Pay2Day integrates directly with partner marketplaces to access worker earnings data and prefunds corresponding amounts available. At the end of the paycycle, Pay2Day collects repayment from the marketplace to settle the funds they’ve advanced to the worker.

Here’s what the user flow could look like:

1. **User sign up:** Wendy Worker signs up for a Pay2Day account, and connects it to her account for the gig platform she works for.
2. **KYC + account creation**: Pay2Day verifies Wendy’s identity and tax information, then provisions a wallet account.
3. **Balance available**: Pay2Day fetches verified earnings data on GoGrocery and deposits the corresponding amount to Wendy’s Pay2Day wallet.
4. **Funds access**: Wendy can either initiate a withdrawal to her linked bank account, or spend directly from her Pay2Day balance.

### Technical Requirements to Enable This Flow

To make the above experience work in production, you need to stitch together several financial infrastructure and payments capabilities directly into Pay2Day:

1. **Virtual account issuance**: A partner bank must provide and manage virtual accounts to hold wallet funds.
2. **Payout support**: You’ll need to be able to send and receive money via ACH debit/credit, and possibly real-time rails such as RTP or FedNow.
3. **A double-entry ledger:** Every wallet account must be backed by a ledger that logs debits, credits, and balance updates immutably.
4. **Balance calculation logic**: Real-time account balances must reflect pending and posted transactions.
5. **Transaction reconciliation**: Incoming and outgoing payments must be [reconciled](https://www.moderntreasury.com/learn/what-is-balance-reconciliation) against bank statements accurately.

The infrastructure decisions you make here will shape your system’s performance, ledger consistency, and support for edge cases.

## Principles in Practice: Setting Up Your Wallet's Ledger with APIs

## Step 1: Set up Pay2Day’s Ledger

We’ll begin with a double-entry ledger. This ledger will include:

- Individual user accounts to track each user’s balance
- A shared cash account to track the funds cash held by Pay2Day.

We’ll restrict our setup to these two account types for the sake of simplicity.

To set up the Pay2Day Ledger, make the following API call to the [Ledgers endpoint](https://docs.moderntreasury.com/reference#ledger-object):

JSON

```
curl --request POST \
  --url https://app.moderntreasury.com/api/ledgers \
  --header 'Content-Type: application/json' \
  --data '{
    "name": "Pay2Day",
    "currency": "usd",
    "description": "General Ledger for Pay2Day account balances"
  }'
```

The response includes the `ledger_id`, which you’ll use throughout the setup process. You can also add metadata for tracking user types, regions, or internal IDs.

## Step 2: Create a Ledger Account for Each User

Create an individual Ledger Account for each Pay2Day user. In this example, we’re creating a ledger account for Wendy Worker; you’ll make a request to the [Ledger Accounts endpoint](https://docs.moderntreasury.com/reference#ledger-account-object):

JSON

```
curl --request POST \
--url https://app.moderntreasury.com/api/ledger_accounts \
--header 'Content-Type: application/json' \
--data '{
"name": "Wendy Worker",
"description": "Ledger account for user Wendy Worker",
"normal_balance": "credit",
"ledger_id": "89c8bd30-e06a-4a79-b396-e6c7e13e7a12"
}'
```

Note that you need to provide the `ledger_id` from Step 1. Use the normal balance: “credit” to indicate account normality, because the funds you (Pay2Day) hold on behalf of the user is considered a liability. Also note that the normal balance is set to credit, since cash you hold on behalf of the user should be treated as a liability.

You can learn more about the conventions around designating accounts as assets or liabilities from our primer, _[Accounting for Developers](https://www.moderntreasury.com/resources/ebooks/accounting-for-developers)_.

## Step 3: Create a Cash Ledger Account for Pay2Day

Track total cash held by your platform with a dedicated debit-normal account. This account will be updated whenever there are deposits and withdrawals from an individual user’s account.

Again, make the request to the [Ledger Accounts endpoint](https://docs.moderntreasury.com/reference#ledger-account-object) that includes the same `ledger_id`:

JSON

```
curl --request POST \
  --url https://app.moderntreasury.com/api/ledger_accounts \
  --header 'Content-Type: application/json' \
  --data '{
    "name": "cash",
    "description": "Ledger account for cash held in Pay2Day",
    "normal_balance": "debit",
    "ledger_id": "89c8bd30-e06a-4a79-b396-e6c7e13e7a12"
  }'
```

## Step 4: Create a Counterparty for Each User

A [Counterparty](https://docs.moderntreasury.com/reference#counterparty-object) represents any external individual or business that you transact with. Creating a counterparty makes it easy to withdraw from or deposit funds to the external bank accounts used by Pay2Day users. To create a new counterparty object for Wendy, make the following API call:

Shell

```
curl --request POST \
  --url https://app.moderntreasury.com/api/counterparties \
  --header 'Content-Type: application/json' \
  --data '{
    "name": "Wendy Worker",
    "email": "wworker@workermail.com"
  }'
```

The response includes a `counterparty_id` to associate with their external accounts.

## Step 5: Create a Virtual Account for Each User

This is the last step required to enable users to store funds within your wallet on Pay2Day. Virtual Accounts enable users to receive and send funds via their own unique routing and account numbers.

Link each Virtual Account to the user’s Counterparty and their credit/debit Ledger Accounts.

JSON

```
curl --request POST \
  --url https://app.moderntreasury.com/api/virtual_accounts \
  --header 'Content-Type: application/json' \
  --data '{
    "name": "wworker",
    "internal_account_id": "f7b2fc7a-3f22-4f82-b651-fa9e2f8d2653",
    "credit_ledger_account_id": "f1c7e474-e6d5-4741-9f76-04510c8b6d7a",
    "debit_ledger_account_id": "e2f8g871-f6d3-4340-9c75-04510c8b6d7b",
    "counterparty_id": "928db55e-6552-4aaf-96d7-10c693922b1f"
  }'
```

Modern Treasury will automatically create ledger transactions for incoming and outgoing payments, updating the correct user and platform accounts.The balance of each account will be updated automatically so that it can be exposed to the user within the Pay2Day app.

![Deposit Flow ](https://cdn.sanity.io/images/8nmbzj0x/production/9fbdea08e5281f7c72006b502b41ecca9e151364-1289x706.svg)

Example deposit flow and ledger for Wendy in Pay2Day's app

## Step 6: Create a Payment Order When a User Withdraws Funds

When Wendy wants to access her daily earnings, you’ll need to create a [Payment Order](https://docs.moderntreasury.com/reference#payment-order-object) to move funds from the external bank account Pay2Day is using to fund wage advances.

For the sake of simplicity, we’ll use ACH as our payment method, but this flow would look similar with RTP, FedNow, or other rails:

JSON

```
curl --request POST \
  --url https://app.moderntreasury.com/api/payment_orders \
  --header 'Content-Type: application/json' \
  --data '{
    "type": "ach",
    "amount": 30000,
    "direction": "debit",
    "originating_account_id": "2e0296c1-1daf-4b3e-954d-fb9ec7be56f6",
    "receiving_account_id": "004e664f-98ab-4606-8b05-c282a3430034"
  }'
```

You can use [webhooks](https://docs.moderntreasury.com/reference#webhooks) to track payment status and query the [Ledger Account Balance endpoint](https://docs.moderntreasury.com/reference#get-ledger-account-balance) to update in-app displays in real-time

![Withdrawal flow](https://cdn.sanity.io/images/8nmbzj0x/production/dc1e7b02cf5af6fd3a01c1d17adffbbfaca9ee10-1289x706.svg)

Example withdrawal flow for Wendy in Pay2Day's app

That’s the high-level flow: set up a ledger, issue accounts, link to external rails, track and reconcile balances.

### Get Started

This guide outlined a simple but robust approach to designing a digital wallet product. For any specific questions about a product you are building, or to see how Modern Treasury can help, [reach out](https://www.moderntreasury.com/talk-to-us) or [sign up today](https://app.moderntreasury.com/sign_up).

Subscribe to our newsletter

Get the latest articles, guides, and insights delivered to your inbox.

Company Email\*

Subscribe

## Authors

![Image of Pranav Deshpande](https://www.moderntreasury.com/_next/image?url=https%3A%2F%2Fcdn.sanity.io%2Fimages%2F8nmbzj0x%2Fproduction%2Fd03171929208a178ac7371eebc93d4997c2eedbe-500x458.jpg&w=1080&q=75)

Pranav DeshpandePMM

![Image of Chris Frakes](https://www.moderntreasury.com/_next/image?url=https%3A%2F%2Fcdn.sanity.io%2Fimages%2F8nmbzj0x%2Fproduction%2Fd9398e9bdadf4d1712be1aa1f7374ff5ecbca466-2030x2030.jpg&w=3840&q=75)

Chris FrakesHead of Content

**Chris Frakes** is the Head of Content, Brand Marketing, and Communications at Modern Treasury. She likes to write and edit pieces that help demystify complex products. She's formerly a journalist, banker, and consultant, and has over a decade of experience in fintech.

Read more

Related

## Ledgering

[View topic →](https://www.moderntreasury.com/resources/ledgering)

[![Image for What is ACID? ](https://www.moderntreasury.com/_next/image?url=https%3A%2F%2Fcdn.sanity.io%2Fimages%2F8nmbzj0x%2Fproduction%2F21c72167a6fe33298b48fa75a2956319aeb3fbbe-1600x900.jpg&w=3840&q=75)](https://www.moderntreasury.com/learn/what-is-acid)

[Glossary](https://www.moderntreasury.com/resources/glossary) [What is ACID?](https://www.moderntreasury.com/learn/what-is-acid)

[![Image for What is Ledger Sharding?](https://www.moderntreasury.com/_next/image?url=https%3A%2F%2Fcdn.sanity.io%2Fimages%2F8nmbzj0x%2Fproduction%2Fbd302bd13aae0398e5e77366f11fe1ea67f512fd-1600x900.jpg&w=3840&q=75)](https://www.moderntreasury.com/learn/what-is-ledger-sharding)

[Glossary](https://www.moderntreasury.com/resources/glossary) [What is Ledger Sharding?](https://www.moderntreasury.com/learn/what-is-ledger-sharding)

[![Image for Single vs. Double Entry Accounting](https://www.moderntreasury.com/_next/image?url=https%3A%2F%2Fcdn.sanity.io%2Fimages%2F8nmbzj0x%2Fproduction%2Fc22d51323ff75e11f209cbc5ab8923047020531f-1600x900.jpg&w=3840&q=75)](https://www.moderntreasury.com/learn/single-vs-double-entry-accounting)

[Glossary](https://www.moderntreasury.com/resources/glossary) [Single vs. Double Entry Accounting](https://www.moderntreasury.com/learn/single-vs-double-entry-accounting)

[![Image for What are GAAP Accounting Rules?](https://www.moderntreasury.com/_next/image?url=https%3A%2F%2Fcdn.sanity.io%2Fimages%2F8nmbzj0x%2Fproduction%2F9304d133189a6fcf632a1466485d04ef15947f04-1600x900.jpg&w=3840&q=75)](https://www.moderntreasury.com/learn/what-are-gaap-accounting-rules)

[Glossary](https://www.moderntreasury.com/resources/glossary) [What are GAAP Accounting Rules?](https://www.moderntreasury.com/learn/what-are-gaap-accounting-rules)

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