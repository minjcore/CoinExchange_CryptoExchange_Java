[Journal](https://www.moderntreasury.com/resources/journal)

•January 4, 2023(Updated August 26, 2025)

# How to Scale a Ledger, Part V: Immutability and Double-Entry

In the fifth part of our series, we examine two of our four ledger guarantees, like immutability and double-entry, and how the API can provide them.

![Image of Matt McNierney](https://www.moderntreasury.com/_next/image?url=https%3A%2F%2Fcdn.sanity.io%2Fimages%2F8nmbzj0x%2Fproduction%2Ff760febc9eb45453974c4a7df4a04f1133977f9b-512x512.jpg&w=64&q=75)

Matt McNierney/ Engineering

Contents

Explore With AI

Topics

[Ledgering](https://www.moderntreasury.com/resources/ledgering) [Engineering](https://www.moderntreasury.com/resources/engineering)

This post is the fifth chapter of a broader technical paper, _[How to Scale a Ledger](https://www.moderntreasury.com/resources/ebooks/how-to-scale-a-ledger)._ Here’s what we’ve covered so far:

- [Part I](https://www.moderntreasury.com/journal/how-to-scale-a-ledger-part-i): Why you should use a ledger database
- [Part 2](https://www.moderntreasury.com/journal/how-to-scale-a-ledger-part-ii): How to map financial events to double entry primitives
- [Part 3](https://www.moderntreasury.com/journal/how-to-scale-a-ledger-part-iii): How a transaction model enables atomic money movement
- [Part 4](https://www.moderntreasury.com/journal/how-to-scale-a-ledger-part-iv): How ledgers support recording and authorizing

In this chapter, we’ll dig deeper into ledger guarantees of immutability and double-entry.

## Immutability: Reconstructing Every Ledger State

_Guarantee: Every state of the ledger must be recorded and can be easily reconstructed._

Immutability is the most important guarantee from a ledger. You may be wondering how immutability can be guaranteed if ledgers allow mutable fields:

- **Accounts**: The balances change as Entries are written to them.
- **Transactions**: The state can change from pending to either posted or archived. While the Transaction is pending, the Entries can change too.
- **Entries**: Entries can be discarded.

### The Immutable Log Underneath

These mutable fields on our core data model help the ledger match real world money movement. But ultimately, the data model is built on top of an immutable, append-only log. All changes are preserved, and any previous state can be reconstructed through: This log can be queried in a few different ways to reconstruct past states through:

- `effective_at` timestamps
- `account_version` numbers
- `transaction version` fields

### Querying Past States

We’ve already covered how `effective_at` allows us to see what the balance on an Account was at an earlier time. We can also see the Entries that were present on an Account at a timestamp, since no Entries are ever actually deleted. The Entries on Account `account_a` at effective time `timestamp_a`can be fetched by filtering:

Python

```
WHERE account_id = 'account_a'
AND effective_at <= 'timestamp_a'
AND (discarded_at IS NULL OR discarded_at >= 'timestamp_a')
```

This logic ensures consistent job reads, even if Entries arrive late or out of order.

### Using Versions for Precision

As we’ve seen before, timestamps may not be sufficient to allow clients to query all past states in ledgers that are operating at scale:

- Entries can be written at any point in the past using the `effective_at` timestamp
- Entries may share the same `effective_at` or `discarded_at` timestamp

Ledgers should solve the drawbacks of timestamps by introducing versions on the main data models: Accounts and Transactions. Account versions that are persisted on Entries allow us to query exactly which Entries correspond to a given Account balance.

Since the Entries on a Transaction can be modified until the Transaction is in the posted state, we need to preserve past versions of a Transaction to be able to fully reconstruct past states of the ledger. The [Ledger API](https://docs.moderntreasury.com/ledgers/docs/overview) stores all past states of the ledger with another field on Transaction:

![Version for Ledger API](https://cdn.sanity.io/images/8nmbzj0x/production/83315169dbdacb5c3cdf93576bf20777f6a816da-820x156.svg)

The version field in the ledger API tells the current transaction version and can show previous states.

Transactions report their current version, and past versions of the Transaction can be retrieved by specifying a version when querying.

Past Transaction versions are used when trying to reconstruct the history of a particular event that involves multiple changing Accounts.

### Example: A Bill-Splitting App

Let’s consider a bill-splitting app. A `pending` Transaction represents the bill when it’s first created, and users can add themselves to subsequent versions of the bill before it is posted:

**Version 0**: Chuck adds himself to the bill. Since he is the only party to the bill, the entry shows him paying for the whole amount.

![Version 0](https://cdn.sanity.io/images/8nmbzj0x/production/e0d491079c8bfdcdc9880795de2b8f914469172d-1041x542.svg)

Version 0

**Version 1**: Dani wants to split costs. Now the bill amount is split between entries on Chuck’s account and Dani’s account.

![Version 1](https://cdn.sanity.io/images/8nmbzj0x/production/24ec9016caf7aebe54329b582b2386c50d849383-1041x542.svg)

Version 1

**Version 2**: Elio is the final addition to split the bill three ways.

![Version 2](https://cdn.sanity.io/images/8nmbzj0x/production/29c2afbc655aacd7174eeda6d87369295dd845d7-1041x542.svg)

Version 2

**Version 3**: The bill payment is posted on each account.

![Version 3](https://cdn.sanity.io/images/8nmbzj0x/production/d3b3b13a4308e7f0c5571919f2f697d8377ae067-1041x542.svg)

Version 3

### Audit Logs

We’ve recorded past states of the main ledger data models, but we haven’t recorded _who_ made the changes. As ledgers scale to more product use cases, it’s common for the ledger to be modified by multiple API keys and by internal users through an admin UI. We recommend an audit log to live alongside the ledger to record that kind of information.

Our Ledgers API audit logs contain these fields to show what was changed, by whom, and when.

![Audit log fields](https://cdn.sanity.io/images/8nmbzj0x/production/de17f4324f64962a80fb6db28ba29e42cc355ada-874x361.svg)

A table showing audit log fields and their descriptions.

## Double-Entry: Preventing the Creation or Destruction of Funds

_Guarantee: All money movement must record the source and destination of funds._

Once a ledger is immutable, the next priority is to make sure money can’t be created or destroyed. We do this by validating that every Transaction has:

- At least two entries, one debit and one credit
- Debits and credits equal each other, per currency

We’ll focus just on the implementation details here—if you want a primer on how to use double-entry accounting, check out our guide, [_Accounting For Developers_](https://www.moderntreasury.com/resources/ebooks/accounting-for-developers).

### Why Currency-Level Balancing Matters

Double-entry accounting must be enforced at the application layer. When a client requests to create a Transaction, the application validates that the sum of the Entries with direction: credit equals the sum of the Entries with direction: debit. But what happens when a Transaction contains Entries from Accounts with different currencies?

Imagine a platform that allows its users to purchase and manage cryptocurrency. Let’s say Freya buys 1 ETH and the platform wants to represent that purchase as an atomic Transaction.

Here’s one way to structure that:

![Double Entry 1](https://cdn.sanity.io/images/8nmbzj0x/production/11b3a46caf8ac868d0f35ba2843433977522db1a-1041x376.svg)

One way to structure a transaction for an ETH purchase

This Transaction involves four accounts, two for Freya (one in USD and one in ETH, credit-normal), and two for the platform (one in USD and one in ETH, debit-normal). This is a valid Transaction, and the credit and debit Entries still match, using the original summing method.

However, the original method breaks down when the Entries across currencies match, but the Transaction isn’t balanced. Here’s an example:

![Double Entry 2](https://cdn.sanity.io/images/8nmbzj0x/production/a743504f61cad5deb109703f4539a10896451816-1041x376.svg)

Even though 1+1=2, this Transaction is crediting a fraction of ETH (created out of nowhere), and Freya was debited $1 (which disappeared).

The correct validation is to group Entries by currency and validate that credits and debits for each currency match. You might think that because Freya spends $4,586.51 to buy one ETH, we could implement currency conversion Transactions with just two entries—one for USD, and one for ETH—and that they could balance based on the exchange rate of USD to ETH. There are three main issues with that implementation:

1. Because currency exchange rates fluctuate over time, it would be difficult to verify that the ledger was balanced in the past.
2. There isn’t a universally agreed-upon exchange rate for all currencies. It would be very difficult for the platform and the ledger to agree that a Transaction is balanced.
3. Having just two Accounts doesn’t match the reality of how currency conversion is performed. To allow Freya to convert dollars to ETH, the platform must have an account of ETH from which to disburse the crypto and a pool of dollars in which to place Freya’s money. That process will always involve at least four Accounts.

### Next Steps

This is the fifth chapter of a broader technical paper with a more comprehensive overview of the importance of solid ledgering and the amount of investment it takes to get right. You now understand two of the guarantees that make a ledger scalable: immutability and double-entry. If you want to explore implementation details or see how Modern Treasury simplifies all of the above, [read our docs](https://docs.moderntreasury.com/ledgers/docs/overview) or [get in touch](https://www.moderntreasury.com/talk-to-us).

#### Read the rest of the series:

[Part I](https://www.moderntreasury.com/journal/how-to-scale-a-ledger-part-i) \| [Part II](https://www.moderntreasury.com/journal/how-to-scale-a-ledger-part-ii) \| [Part III](https://www.moderntreasury.com/journal/how-to-scale-a-ledger-part-iii) \| [Part IV](https://www.moderntreasury.com/journal/how-to-scale-a-ledger-part-iv) \| [Part VI](https://www.moderntreasury.com/journal/how-to-scale-a-ledger-part-vi)

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