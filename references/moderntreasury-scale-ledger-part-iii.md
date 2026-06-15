[Journal](https://www.moderntreasury.com/resources/journal)

•December 6, 2022(Updated August 22, 2025)

# How to Scale a Ledger, Part III: Transaction Models

In the third part of this series, we'll look at how Transaction models enable atomic money movement and enforce double-entry.

![Image of Matt McNierney](https://www.moderntreasury.com/_next/image?url=https%3A%2F%2Fcdn.sanity.io%2Fimages%2F8nmbzj0x%2Fproduction%2Ff760febc9eb45453974c4a7df4a04f1133977f9b-512x512.jpg&w=64&q=75)

Matt McNierney/ Engineering

Contents

Explore With AI

Topics

[Ledgering](https://www.moderntreasury.com/resources/ledgering) [Engineering](https://www.moderntreasury.com/resources/engineering)

_Updated on August 22, 2025._

This post is the third chapter of a broader technical paper, [_How to Scale a Ledger_](https://www.moderntreasury.com/resources/ebooks/how-to-scale-a-ledger). In [Part I](https://www.moderntreasury.com/journal/how-to-scale-a-ledger-part-i), we covered why you should use a ledger database, and in [Part II](https://www.moderntreasury.com/journal/how-to-scale-a-ledger-part-ii), we mapped common financial events to double entry primitives like Accounts and Entries. In this chapter, we look at a transaction model to enable atomic movement.

## Why Transactions Matter in Ledger Systems

While Accounts and Entries within [ledger databases](https://www.moderntreasury.com/learn/ledger-database) provide an immutable audit trails of balance changes, they aren’t enough on their own. Without a Transaction object, it’s possible to create inconsistent states if only part of a money movement is recorded.

A well designed transaction model provides:

- **Atomicity:** All Entries must succeed or fail together
- **Consistency:** Prevents partial state changes
- **Double-entry enforcement:** Ensures balanced Entries for every transaction

![Transaction](https://cdn.sanity.io/images/8nmbzj0x/production/7d7150a78699d83f3d3f2bfe3a38953215855ad7-780x265.svg)

Transaction fields and descriptions

## Transaction Schema: Digital Wallet Transfer Example

Consider a simple transfer of money between accounts in a peer-to-peer wallet app. Let’s say Bobby sends Alice $10; we can represent that transfer as two Entries: `bobby_entry` and `alice_entry`.

![Two Digital Wallet Entries](https://cdn.sanity.io/images/8nmbzj0x/production/173df218f41eb7998c879021e61550328e452285-780x335.svg)

Two entries representing a $10 digital wallet transfer from Bobby to Alice

Imagine that `bobby_entry` was successfully written, but the corresponding `alice_entry` failed to write (maybe the database was having network issues). Now the ledger is in an inconsistent state—Bobby was debited money, but Alice didn’t get anything. Money is lost.

Transactions solve this consistency problem by allowing us to specify groups of Entries that either must _all succeed_ or _all fail_. In order to guarantee atomicity, all the non-discarded Entries on a Transaction must share the status of the Transaction. This ensures that all Entries progress in status at the same time, all-or-nothing.

A [Ledger API](https://docs.moderntreasury.com/ledgers/docs/overview) should only allow clients to directly create Transactions, not Entries. This limitation helps ensure clients don’t run into consistency problems. However, that means a Ledger API must manage creating Entries itself. There are three operations to implement, corresponding to the possible states of the Transaction:

1. Pending (initial state)
2. Posted (finalized)
3. Archived (canceled before posting)

## Creating a Pending Transaction

This is generally the first step in the lifecycle of a Transaction. The system persists the debit and credit Entries as `pending`, but the transaction is not finalized.

![Persisting the two entries](https://cdn.sanity.io/images/8nmbzj0x/production/e784c9f18d7b3b68655d9db8bb853070a2faeeab-780x335.svg)

Persisting the $10 digital wallet transfer from Bob to Alice to create a Transaction

## Posting a Transaction (Finalizing It)

Since Entries are immutable, when we move a Transaction from `pending` to `posted`, we need to:

1\. Discard the pending Entries (`bobby_entry_1` and `alice_entry_1`).

![Discard entry_1](https://cdn.sanity.io/images/8nmbzj0x/production/b139be8e0fb47e92952887956f76cad09a2f2bd3-780x357.svg)

Discarding pending entries bobby\_entry\_1 and alice\_entry\_1

2\. Create new posted Entries. This preserves immutability: posted Entries are never modified.

![Create posted entries](https://cdn.sanity.io/images/8nmbzj0x/production/9531794d1354a105f041218ed8c1da24d7e1cfc7-780x335.svg)

Now posted, the $10 transaction is immutable

## Archiving a Transaction (Canceling It)

Posted Transactions are immutable, and so cannot be archived. So what if the transaction was canceled?

Pending Transactions _can_ have their status changed to `archived`, following a similar process to posting.

1\. Discard pending `bobby_entry_1` and `alice_entry_1`.

![Discard pending transactions to be archived](https://cdn.sanity.io/images/8nmbzj0x/production/b139be8e0fb47e92952887956f76cad09a2f2bd3-780x357.svg)

Discarding pending transactions bob\_entry\_1 and alice\_entry\_1 to be archived

2\. Create new Entries with `archived` status.

![Create archived entries](https://cdn.sanity.io/images/8nmbzj0x/production/2a96c6d5fb2f95f81c9acd0d646c1c970dfceaa5-780x335.svg)

Archived entries show the $10 transaction was canceled, but retains the history

### Next Steps

This is the third chapter of a broader technical paper with a more comprehensive overview of the importance of solid ledgering and the amount of investment it takes to get right.

If you want to learn more, [download the paper](https://www.moderntreasury.com/resources/ebooks/how-to-scale-a-ledger), or [get in touch](https://www.moderntreasury.com/talk-to-us).

#### Read the rest of the series:

[Part I](https://www.moderntreasury.com/journal/how-to-scale-a-ledger-part-i) \| [Part II](https://www.moderntreasury.com/journal/how-to-scale-a-ledger-part-ii) \| [Part IV](https://www.moderntreasury.com/journal/how-to-scale-a-ledger-part-iv) \| [Part V](https://www.moderntreasury.com/journal/how-to-scale-a-ledger-part-v) \| [Part VI](https://www.moderntreasury.com/journal/how-to-scale-a-ledger-part-vi)

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