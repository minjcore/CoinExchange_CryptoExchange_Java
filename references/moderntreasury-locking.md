[Glossary](https://www.moderntreasury.com/resources/glossary)

•August 1, 2025

# Pessimistic Locking Vs. Optimistic Locking

Pessimistic locking and optimistic locking are types of concurrency controls designed to handle concurrent updates in a ledger system, helping prevent race conditions and maintain immutability in financial ledgers.

Contents

Explore With AI

Topics

[Ledgering](https://www.moderntreasury.com/resources/ledgering) [Engineering](https://www.moderntreasury.com/resources/engineering)

Pessimistic locking and optimistic locking are types of concurrency controls designed to handle concurrent updates in a ledger system, helping prevent race conditions and maintain immutability in financial ledgers.

## Why are Concurrency Controls Important in Ledger Databases?

When several transactions take place at the same time through concurrent access, a ledger database needs a way to maintain its integrity and consistency. Concurrent access, or allowing multiple users to access a computer system or database at the same time, is common in many financial applications.

For example, consider the Starbucks app: a popular digital wallet. In the app, you’re able to manage your funds and order drinks, and the database behind the scenes maintains your balance and updates it as necessary.

All the money across everyone’s Starbucks wallet is pooled in an account. Starbucks needs to track and record which funds belong to you (versus every other person) and when you have spent them.

The balance of the pooled Starbucks account changes constantly as people order drinks, make purchases, or add money to their personal balances. This happens on a large scale, with thousands of people likely doing these things at the same time. The way they keep track of everything is with concurrency controls.

There are two types of concurrency controls: pessimistic locking and optimistic locking. These are designed to handle concurrent updates in a ledger system, prevent race conditions in databases, and maintain immutability in financial ledgers.

## Transaction Isolation Levels and Multi-Version Concurrency Control (MVCC)

Before discussing pessimistic or optimistic locking, it’s important to understand a broader concept of transaction isolation levels. Transaction isolation levels are the rules that determine how transactions interact when they happen at the same time (concurrently).

Within financial ledgers, immutability—the inability to be changed or altered—is of the utmost importance. With MVCC, databases can maintain multiple versions of records, allowing for updates that affect only the most current version to reduce the potential for conflicts.

## What is Pessimistic Locking?

Pessimistic locking is a concept based on the assumption that conflicts are likely to occur when multiple users or processes attempt to access and update the same resource simultaneously. With this approach, the idea is to lock first and update the information later, ensuring there are fewer conflicts.

### How Does Pessimistic Locking Work?

When a transaction occurs, the database record is accessed. That’s when the record is locked. No other transaction can take place or update that record until the lock is released.

Think of this like standing in line at a store. You’re queued and ready to make a purchase, but only the first person in the line can check out. Not everyone can check out at the same time.

### The Benefits and Downsides of Pessimistic Locking

Pessimistic locking has a few benefits, including:

- Ensuring data integrity by avoiding conflicts when updating a core record
- Granting exclusive access in a consistent manner that simplifies transaction logging

Pessimistic logging is beneficial for systems where it is common to have a few conflicting updates occurring simultaneously, but it starts to fail at scale (the queue at that store on Black Friday is a lot more overwhelming than on a Monday morning).

This has the potential for deadlocks in financial systems, which can mean reduced throughput. For the end user, this typically means a longer latency period (lag) when attempting to make a payment.

## What Is Optimistic Locking?

Optimistic locking takes an alternative approach to updating databases and records. The process assumes that conflicts _aren’t_ likely to happen and doesn’t lock resources, meaning. Transactions can take place simultaneously. This process checks for conflicts after the transactions finalize.

### How Does [Optimistic Locking](https://systemdesignschool.io/blog/optimistic-locking) Work?

1. Every account has its own version number or timestamp to keep track of it.
2. When a transaction happens, it reads the account version and records it.
3. When the entry is added to the ledger, it compares its version to the current version. If the versions don’t align, the update fails. If they do align, the transaction goes through.

Consider this example to better understand how optimistic locking works. If you and a friend are using peer-to-peer paying apps, like CashApp, at the same time, you might try to send $50 while your friend requests $40. These actions would both influence your [account balance](https://www.moderntreasury.com/learn/single-vs-double-entry-accounting).

Optimistic locking doesn’t require the transactions to queue. Instead, it looks at your balance at the time of each transaction, processes both transactions in parallel, and, when it’s time to complete the transactions, checks if your [balance’s version has changed](https://www.moderntreasury.com/journal/how-to-scale-a-ledger-part-iv).

If the version has changed, the second transaction will be rejected and retried based on your new balance version. This helps avoid a deadlock, but the system may need to retry one of the transactions to get it to go through.

### The Benefits and Downsides of Optimistic Locking

The benefits of optimistic locking include:

- Higher throughput, since transactions are able to happen simultaneously
- No deadlocks, so reads aren’t blocked by writes

Optimistic locking is ideal for systems where the risk of conflict is low or infrequent, such as read-heavy systems, or for systems with short transactions, which can help users avoid unnecessary wait times

While transaction attempts can happen concurrently, some may fail and require retrying. Those retries can create more complex situations for developers working on databases, and it may not be the best solution if conflicts occur frequently.

## How Do Pessimistic and Optimistic Locking Handle Race Conditions in Financial Systems?

Race conditions, which happen when two transactions or processes try to update the same record at the same time, are handled differently by pessimistic and optimistic locking procedures.

- Pessimistic locking makes transactions queue. A transaction cannot be completed until the one ahead of it has updated the record.
- Optimistic locking allows transactions to happen simultaneously. Validation is performed when the transactions are committed to the record, which may require retries if the record version has changed.

Both of these processes can handle concurrent transactions. For financial situations in which conflicts are likely and volume is low, pessimistic locking can be a sensible approach. However, for most modern ledger database designs needing immutability and continuous performance, optimistic locking makes more sense, as it is more flexible.

Subscribe to our newsletter

Get the latest articles, guides, and insights delivered to your inbox.

Company Email\*

Subscribe

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

## Latest Glossary Entries

[View all →](https://www.moderntreasury.com/learn)

[![Image for What Is Blockchain Treasury Management?](https://www.moderntreasury.com/_next/image?url=https%3A%2F%2Fcdn.sanity.io%2Fimages%2F8nmbzj0x%2Fproduction%2F21c72167a6fe33298b48fa75a2956319aeb3fbbe-1600x900.jpg&w=3840&q=75)](https://www.moderntreasury.com/learn/what-is-blockchain-treasury-management)

[What Is Blockchain Treasury Management?](https://www.moderntreasury.com/learn/what-is-blockchain-treasury-management)

[![Image for What Are Stablecoin Reserves?](https://www.moderntreasury.com/_next/image?url=https%3A%2F%2Fcdn.sanity.io%2Fimages%2F8nmbzj0x%2Fproduction%2F9304d133189a6fcf632a1466485d04ef15947f04-1600x900.jpg&w=3840&q=75)](https://www.moderntreasury.com/learn/what-are-stablecoin-reserves)

[What Are Stablecoin Reserves?](https://www.moderntreasury.com/learn/what-are-stablecoin-reserves)

[![Image for What Are Algorithmic vs. Collateralized Stablecoins?](https://www.moderntreasury.com/_next/image?url=https%3A%2F%2Fcdn.sanity.io%2Fimages%2F8nmbzj0x%2Fproduction%2Fc22d51323ff75e11f209cbc5ab8923047020531f-1600x900.jpg&w=3840&q=75)](https://www.moderntreasury.com/learn/what-are-algorithmic-vs-collateralized-stablecoins)

[What Are Algorithmic vs. Collateralized Stablecoins?](https://www.moderntreasury.com/learn/what-are-algorithmic-vs-collateralized-stablecoins)

[![Image for How the GENIUS Act Impacts Stablecoin Issuers](https://www.moderntreasury.com/_next/image?url=https%3A%2F%2Fcdn.sanity.io%2Fimages%2F8nmbzj0x%2Fproduction%2F4fb7038cd160ea7202ca78d6a6cfea6aca6ef02a-1600x900.jpg&w=3840&q=75)](https://www.moderntreasury.com/learn/how-the-genius-act-impacts-stablecoin-issuers)

[How the GENIUS Act Impacts Stablecoin Issuers](https://www.moderntreasury.com/learn/how-the-genius-act-impacts-stablecoin-issuers)

We use cookies to improve your experience.By using our website, you’re agreeing to the collection of data described in our [Privacy Policy](https://www.moderntreasury.com/privacy).

Allow allDeny all

Show preferences