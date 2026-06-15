[Journal](https://www.moderntreasury.com/resources/journal)

•December 20, 2022(Updated August 25, 2025)

# How to Scale a Ledger, Part IV: Recording and Authorization

In the fourth part of this series, we'll look at how a ledger fits into a modern money movement system.

![Image of Matt McNierney](https://www.moderntreasury.com/_next/image?url=https%3A%2F%2Fcdn.sanity.io%2Fimages%2F8nmbzj0x%2Fproduction%2Ff760febc9eb45453974c4a7df4a04f1133977f9b-512x512.jpg&w=64&q=75)

Matt McNierney/ Engineering

Contents

Explore With AI

Topics

[Ledgering](https://www.moderntreasury.com/resources/ledgering) [Engineering](https://www.moderntreasury.com/resources/engineering)

This post is the fourth chapter of a broader technical paper, [_How to Scale a Ledger_](https://www.moderntreasury.com/resources/ebooks/how-to-scale-a-ledger). Here’s what we’ve covered so far:

- [Part I](https://www.moderntreasury.com/journal/how-to-scale-a-ledger-part-i): Why you should use a ledger database
- [Part 2](https://www.moderntreasury.com/journal/how-to-scale-a-ledger-part-ii): How to map financial events to double entry primitives
- [Part 3](https://www.moderntreasury.com/journal/how-to-scale-a-ledger-part-iii): How a transaction model enables atomic money movement

Now that we have introduced the basic data models that comprise a ledger, in this chapter, we’ll discuss how a ledger fits into a money movement system, focusing on two key operations: recording and authorizing.

## Two Ledger Modes: Recording Vs. Authorizing

Most ledgers implementations we’ve seen can operate at scale in one of the following two use cases:

1. **Recording:** Logging a consolidated record of money movement that happens in other systems.
2. **Authorizing:** Enforcing rules to approve or deny transactions.

Because most ledgers excel in only one use case, with any new business or new products, rather than scale their ledger, companies tend to build multiple ledgers that aren’t interoperable with each other. The most powerful ledgers—like Modern Treasury’s [Ledgers](https://www.moderntreasury.com/products/ledgers)—can operate in both models, allowing clients to choose at the Entry level which guarantees they need, depending on performance and consistency requirements.

## Recording

Recording means capturing financial events that occur in external systems (your bank, payment processors, card networks), translating them into the core data models, and making them available for query by both internal and external customers. It is characterized by:

- **High write throughput**: thousands of writes per second (or higher)
- **Asynchronous processing**: reads from the ledger may be stale for a few seconds, but will have eventual consistency
- **Complex query support**: enabling filtering and aggregating of the core data models

The ledger is not the source of truth—it reflects what already happened elsewhere.

![Recording](https://cdn.sanity.io/images/8nmbzj0x/production/961a386bb6de814f05eaef2eae3ac00b7f78080a-709x239.svg)

The Application Layer sits between the Event Source and the DBs.

### Architecture

The Application Layer sits between the Event Source and the Ledger Database (DB) and Domain Object DB.

1. **Event Source**: The source of the money movement. This can be any service that moves money, for example, a bank, a card payment processor, or a payments API like Modern Treasury.
2. **Application Layer**: Your application code that processes events. It translates the event source’s data model into your domain objects and Transactions in the Ledger DB.
3. **Ledger DB**: The immutable log of money movement events.
4. **Domain Object DB**: Your application data store, logging any state from the event unrelated to money movement.

### Maintaining Consistency

Notice that the Domain Object DB and the Ledger DB are dotted-lined from the Application Layer, indicating that this connection can be asynchronous and eventually consistent. This eventual consistency enables high throughput.

You may be asking, “ _How can I move money confidently with eventual consistency?_” You can keep a ledger internally consistent even if it is a delayed representation of money movement, by supporting client-supplied timestamps and Account balance versions.

Since money movement already occurred by the time it’s recorded by the ledger, clients need a way to specify when the money movement actually happened:

The `effective_at` timestamp lets the client backdate a transaction to when it actually occurred.

![Client supplied time stamp transaction](https://cdn.sanity.io/images/8nmbzj0x/production/f59ec68dc28d4027b1bdf953b6f6e4fb27a58fd6-819x181.svg)

The effective\_at timestamp shows when the transaction occurred.

To preserve atomicity, all Entries on a Transaction inherit the Transaction’s `effective_at` timestamp.

![Client supplied timestamp for entry](https://cdn.sanity.io/images/8nmbzj0x/production/81086746f2e358115ca8944406e3b9083cff21cc-819x196.svg)

The effective\_at field for an Entry shows when the corresponding transaction occurred in an external system.

The `effective_at`field allows clients to modify historical balances, so that they reflect the balances as they were in the external system. Using `effective_at`, we can also support querying historical balances to ensure transactions appear in the correct order.

Typically, the `effective_at` timestamp sent by the client will be close to the `created_at` timestamp set by the ledger. The difference is the delay between getting information from the external system into the ledger—usually on the order of seconds or minutes.

### Account Balance Versions

Allowing clients to modify historical balances introduces a problem: if balances in the past can change at any moment, how can we know which transactions correspond to a balance? Consider this race condition:

1. At time t: A client with a stored value wallet reads customer Annie’s account balance
2. At time t+1: A Transaction transaction\_1 is written to the ledger with effective time t-1
3. At time t+2: The client asks for all Transactions before T. The result will include transaction\_1, but the balance read will not. The client will see an inconsistency between the Transactions and the Account balance.

The Ledgers API allows for consistent Transaction and Account balance reads through fields on Account and Entry.

![Account balance version - Account](https://cdn.sanity.io/images/8nmbzj0x/production/7029c85582ab17134e20413f1d1df9e038f43962-819x193.svg)

Account version is incremented every time an Entry is created or modified.

![Account_version entry](https://cdn.sanity.io/images/8nmbzj0x/production/2d2f9d792f4e91036052921c0e1cb7c1bdb8d80a-819x192.svg)

The account\_version is the version of account associated with the Entry.

With these new fields, we can know exactly which Entries correspond with a given Account version. After reading the Account `posted_balance` at the effective time `2025-08-20T18:22:14+00:00`, if the Account version is 10, the Entries that correspond to that balance can be found by filtering Entries by:

- `account_id`
- Status of `posted`
- `effective_at` less than or equal to `2025-08-20T18:22:14+00:00`
- account\_version less than or equal to 10
- Not discarded

### Recording Use Cases

- **Displaying Account Details**: Account UIs for digital wallets, cards, brokerages, and other account-like products typically have a UI where the balance on an Account is displayed along with recent Entries. Using the Entry `account_version` field, we can ensure that the displayed balance and Entries are always consistent.
- **Payouts**: Marketplaces collect money on behalf of their customers, and pay that money out on a certain cadence (daily, weekly, monthly). These systems must tolerate a few seconds of staleness, because actual money movement is happening through payment processors outside of the ledger. Using `effective_at`, the ledger ensures that Transactions are in the correct order, even if they are recorded out of order. Account versions enable displaying to a user exactly which Entries correspond to a particular payout.
- **Loan Servicing**: Systems that service loans are complicated, but generally work asynchronously. Accruing interest is similar to a marketplace payout—take a snapshot of a past-due balance, get the Account version, and accrue interest based on the Entries that comprise that balance. Applying a payment to a balance benefits from client-supplied timestamps—these systems should compute past due status based on when a payment was processed, not when the lending ledger recorded the payment.
- **Crypto**: By definition, crypto transactions happen outside of your application’s ledger on a blockchain. Often it makes sense to keep a local copy of those transactions in a [ledger database](https://staging.moderntreasury.com/products/ledgers) for performance reasons. Because the transactions have already happened by the time they are written to the application ledger, they should be recorded with an `effective_at` timestamp matching when the transaction happened on the blockchain.

## Authorizing

Authorizing means the ledger actively approves or denies Transactions based on Account states. It’s characterized by:

- **Read-after-write consistenc** y: Updates from Entries are instantly applied to the associated Accounts
- **Lower transaction throughput**: Performance will degrade around 100 Entries per second on individual Accounts
- **Balance assertions**: The system is optimized to maintain invariants on Account balances or versions
- **Concurrency control:** Version or balance locking as well as atomicity to enforce balance assertions

This model is crucial when moving money in real-time (e.g., validating that a user has sufficient funds to process a transaction). Enforcing approval rules on each Transaction requires a strict ordering of Transactions, which also means that Transactions can only be processed one at a time. This limitation results in higher latencies because Transaction recording can no longer be done in parallel on individual Accounts.

There are two types of rules that the Ledgers API supports, one based on Account versions and another based on Account balance.

### Version Locking

**Version locking** is the simplest control that we can enforce while writing a Transaction to prevent out-of-order updates. Since `account_version` is updated every time an Entry is written to the Account, we can ensure writing order by having the client send an Account version along with the request to create a Transaction. The ledger will reject the write if the current Account version in the database is different from the version sent by the client.

This approval rule is similar to the concept of optimistic locking. We can enforce the rule at the database level using transactions following this algorithm:

1. Start a database transaction.
2. Write the ledger entry.
3. Update the Account version, including a condition on the current Account version.
4. If the Account was updated in step 3 (i.e., the client-provided `account_version` matched the one in the database), then commit the database transaction. Otherwise, roll it back.

### Balance Locking

Version locking is susceptible to “hot accounts” (this is what we call Accounts that see a high volume of writes). If the `account_version` is incrementing at a fast enough pace, some Transactions will never be able to commit.

To solve this problem, it helps to step back and think about the main use case for locking in the first place. In almost all cases, clients want version locking in order to enforce balance assertions. For example, the client might want to create a pending Transaction for a card authorization _only if_ there is enough available balance on the card.

To address the most common use case for locking, the Ledgers API supports **balance locking** at the Entry level. To implement this, we add a few new fields to the Entry data model: `pending_balance_amount` and `BalanceCondition`.

The `pending_balance_amount` details conditions that must be true on the Account's pending balance after the Transaction commits.

![Balance locking entry](https://cdn.sanity.io/images/8nmbzj0x/production/6d8ee64166711f10d542d79f5567a970854f571c-819x381.svg)

pending\_balance\_amount details conditions that must be true on the Account's pending balance after the Transaction commits.

The `BalanceCondition` fields specify what needs to be true about the account balance for the Transaction to commit.

![BalanceCondition](https://cdn.sanity.io/images/8nmbzj0x/production/afb5f21bc6eb1ea4aeef4eba55b1d6ef83bb186a-819x335.svg)

BalanceCondition fields and specifications

These new fields allow us to implement balance assertions when writing Transactions. Consider an Account with $1,000 available balance, with two Transactions written simultaneously on the account, one for $250 and one for $750. With version locking, the flow would be:

1\. $250 Transaction

- Read Account balance and version, and check that balance is greater than or equal to $250
- Write Transaction that includes an Entry on the Account with the version read in the previous step.

2\. $750 Transaction

- Read Account balance and version, and check that balance is greater than or equal to $750
- Write a Transaction on the Account, which fails because the $250 Transaction was written slightly before
- Again read Account balance and version
- Write Transaction

In total, we made a call to the ledger six times.

With balance locking, we can reach the same output in two calls:

1\. $250 Transaction

- Write Transaction with `gte: 0` on the relevant Entry.

2\. $750 Transaction

- Write Transaction with `gte: 0` on the relevant Entry.

Not only are we calling the ledger fewer times, but also the API better matches our intent to prevent a certain Account balance from going negative.

Implementing balance locking so that race conditions are handled properly and all possible combinations of locks on different balances are respected is beyond the scope of this paper. The logic is based on how version locking works—within a database transaction, attempt to write each Entry finding an Account with the required balance, and then check whether the update actually went through, rolling back the database transaction if it did not.

### Authorizing Use Cases

- **Digital Wallets**: A digital wallet holds a sum of money for a user that can be withdrawn into a bank account (typically by initiating an ACH credit). It’s important that digital wallets ensure that users have sufficient funds to initiate a withdrawal, which necessitates balance locking. Additionally, many digital wallets support closed-loop payments between users. Authorizing Entries enable such instant payments while making sure Account balances can’t go below 0.
- **Card Authorizations**: In order to respond to a card authorization request, a ledger must be able to know the exact available balance of an Account at a point in time. Balance locking enables this while also preventing double-spending. Implementations can reserve the authorized amount until it is cleared using pending Transactions.

## Mixed-Mode Ledgers: Automatically Recording or Authorizing

As we said above, most ledgers we’ve worked with implement only one of the recording or authorizing models. Because the guarantees and use-cases for each model are so different, it’s easy to see why.

As companies add new products that need recording or authorizing ledgers, they build new ledgers that are optimized for each product. The downside of this approach is that it’s an expensive strategy, as it requires staffing and maintenance overhead.

We’ve worked with some ledger implementations that get closer to being general purpose by designating certain Accounts as exclusively recording or authorizing. Some ledger implementations even allow different modes to be toggled by on-call engineers—if an Account is experiencing high Transaction throughput, an on-call engineer can put the Account into recording mode.

What we’ve realized by working with companies using ledgers for many use cases is that even an Account settings implementation is not sufficient. For the Ledgers API to flex to be truly multi-purpose and multi-product, the model must be determined at the Entry level.

### Example: Card Authorization

Even processing simple events—authorization and clearing—requires a mix of authorizing and recording. Card authorizations require a synchronous read of the Account balance. After a successful authorization, the card network will send a clearing event to indicate that the reserved funds should be marked as finalized. This event should be recorded by the ledger regardless of the Account state.

We can infer whether an Entry needs to be recorded or authorized based on whether the Entry contains an Account version or balance lock. Transactions may contain a combination of locked and not locked Entries. For example, here’s a sample card authorization Transaction:

![Sample transaciton](https://cdn.sanity.io/images/8nmbzj0x/production/a3378786f3b022d232acc71fd0b0cf50ec4775dc-819x332.svg)

Sample card authorization transaction.

- `card_entry_1`requirements:
  - Corresponds to a purchase on the Account
  - Should have a balance lock (must not bring balance of `card_account_id` below 0)
  - Needs strong consistency: balance lock cannot be processed without an up-to-date read of the current balance
  - Does not require high throughput (you can’t swipe a credit card 1,000 times per second)
- `processor_entry_1`requirements:
  - Corresponds to money to be sent to card issuer processor as part of a daily settlement (eventually this money goes to the issuing bank that fronted the money for Transactions)
  - Should not have a balance lock (card auth should be allowed regardless of state of the issuer processor’s account)
  - Needs eventual consistency: Only needs to read the balance of the Account during the settlement process at the end of the day
  - The associated account requires high throughput: Every card auth in the program will include a credit entry written to this account, meaning as the card program grows, this account can experience thousands of writes per second.

Both Entries must succeed or fail atomically—even with different consistency and throughput requirements.Recorded Entries should be processed asynchronously and in batches, but also should not be written if their containing Transaction was not authorized. Additionally, they should not be present in reads from the ledger until the containing Transaction is authorized.

This level of complexity is what makes building double-entry ledgers difficult. Single-entry systems skip these guarantees at the expense of data integrity. It would be easy for a system to accidentally approve a card authorization, but not include that authorization in a daily settlement with the issuer processor. With double-entry, that is not possible.

### Next Steps

This is the fourth chapter of a broader technical paper with a more comprehensive overview of the importance of solid ledgering and the amount of investment it takes to get right.

If you want to learn more, [download the paper](https://www.moderntreasury.com/resources/ebooks/how-to-scale-a-ledger), or [get in touch](https://www.moderntreasury.com/talk-to-us).

Read the rest of the series:

[Part I](https://www.moderntreasury.com/journal/how-to-scale-a-ledger-part-i) \| [Part II](https://www.moderntreasury.com/journal/how-to-scale-a-ledger-part-ii) \| [Part III](https://www.moderntreasury.com/journal/how-to-scale-a-ledger-part-iii) \| [Part V](https://www.moderntreasury.com/journal/how-to-scale-a-ledger-part-v) \| [Part VI](https://www.moderntreasury.com/journal/how-to-scale-a-ledger-part-vi)

Subscribe to our newsletter

Get the latest articles, guides, and insights delivered to your inbox.

Company Email\*

Subscribe

## Authors

![Image of Matt McNierney](https://www.moderntreasury.com/_next/image?url=https%3A%2F%2Fcdn.sanity.io%2Fimages%2F8nmbzj0x%2Fproduction%2Ff760febc9eb45453974c4a7df4a04f1133977f9b-512x512.jpg&w=1080&q=75)

Matt McNierneyEngineering

Matt McNierney serves as Engineering Manager for the Ledgers product at Modern Treasury, and is frequent contributor to Modern Treasury’s technical community. Prior to this role, Matt was an Engineer at Square. Matt holds a B.A. in Computer Science from Dartmouth College.

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