[Journal](https://www.moderntreasury.com/resources/journal)

•August 17, 2022(Updated August 13, 2025)

# Accounting For Developers, Part I: The Fundamentals

In this first part of a three-part series, we walk through basic accounting principles for anyone building products that move and track money.

![Image of Lucas Rocha](https://www.moderntreasury.com/_next/image?url=https%3A%2F%2Fcdn.sanity.io%2Fimages%2F8nmbzj0x%2Fproduction%2F55163c3dc423adcb31083f7d47eca293dc95c46a-2875x2679.jpg&w=64&q=75)

Lucas Rocha/ Product Manager

Contents

Explore With AI

Topics

[Back Office](https://www.moderntreasury.com/resources/back-office) [Ledgering](https://www.moderntreasury.com/resources/ledgering)

## Introduction

As a payment operations startup, accounting principles are core to our work. While some of the largest fintechs and marketplaces implement accounting principles at scale, they seem an arcane topic for new startups. This [HackerNews thread](https://news.ycombinator.com/item?id=23964513), for example, is rather representative of the state of confusion around the topic.

Over the years, many guides such as this have tried to explain accounting to developers (for example, these two great pieces from [Martin Blais](https://beancount.github.io/docs/the_double_entry_counting_method.html#double-entry-bookkeeping) and [Martin Kleppmans](https://martin.kleppmann.com/2011/03/07/accounting-for-computer-scientists.html)). But in our experience, a concepts-first approach to explaining accounting comes in handy when you are designing systems that move or touch money.

Whether you're reconciling accounts, tracking balances, or building a ledger from scratch, this series is for engineers working on financial systems. We think that every engineer that builds or maintains such systems benefits from knowing the core principles of accounting.

Because, if you’re building a product that moves money, you’re already in the business of accounting. While traditional accounting might seem abstract or “not for developers,” this guide reframes it in technical terms. You’ll learn how to track balances, avoid financial inconsistencies, and structure systems that scale cleanly.

This three-part series includes:

- Part I (this post): Accounting foundations for engineers
- [Part II](https://www.moderntreasury.com/journal/accounting-for-developers-part-ii): Designing a ledger system to power a Venmo-style app
- [Part III](https://www.moderntreasury.com/journal/accounting-for-developers-part-iii): Applying principles to build a lending marketplace akin to Lending Club

## Who Should Read This Guide?

This guide is for:

- Developers at fintechs building products that move or hold money.
- Engineers integrating payment providers, ACH, RTP, or card systems.
- Product teams designing wallets, marketplaces, or accounting features.

If you write code that touches financial data, this guide will help you reduce bugs, align internal records with banking systems, and avoid common pitfalls.

## Why Developers Need to Understand Accounting

[Double-entry accounting](https://www.moderntreasury.com/learn/single-vs-double-entry-accounting) is the most reliable way to track money. It ensures every financial event is recorded accurately, with a clear source and destination for funds.

As a payments infrastructure company, we often get to see the architecture of some of the most successful software companies. Top companies rely on double-entry principles. Some build their applications from the start with accounting concepts in mind, but in most cases, companies need to retrofit them after painful incidents involving missing funds, broken payout flows, or inconsistent balances.

When software fails to track money properly, it does so in a number of common patterns. The most common failure mode is software accidentally creating or destroying records of funds. This leads to all sorts of inconsistencies. Every developer we know has horror stories about explaining to their finance team why a customer is owed money or what caused a payout to have an unexpected amount:

- Internal records differing from bank statements
- [Reconciliation](https://www.moderntreasury.com/resources/reconciliation) engines mismatching
- Balances that don’t make sense given a set of transactions

For more evidence that double-entry systems are a good standard for scalable applications, see the stories of [Uber](https://eng.uber.com/money-scale-strong-data/), [Square](https://developer.squareup.com/blog/books-an-immutable-double-entry-accounting-database-service/), and [Airbnb](https://medium.com/airbnb-engineering/tracking-the-money-scaling-financial-reporting-at-airbnb-6d742b80f040).

The core principle of double-entry accounting is that **every transaction should record both where the money came from and what the money was used for.**

With double-entry, you can:

- Reconstruct balances as of any date
- Track money movement with full auditability
- Align system logic with real-world finance

## Core Concepts: Accounts, Transactions, and Ledgers

Let's break down the building blocks of an accounting system.

### What is an Account in Accounting?

An **account** is a segregated pool of value. The easiest analogy here is your own bank checking account: money that a bank is holding on your behalf, clearly demarcated as yours. Any discrete balance can be an account: from a user’s balance on Venmo to the annual defense spending of the United States. Accounts generally correlate with the balances you want to track.

In accounting, accounts have _types_. More on this later.

### How Transactions Work in a Ledger

A **transaction** is an atomic event that affects account balances. Each transaction:

- Has at least two entries, each of which corresponds to one account
- Affects two or more accounts
- Keeps the ledger balanced

Let’s use a simple Venmo transfer as an example. Van is sending $50 to Tai:

![Transaction: Van sends $50 to Tai](https://cdn.sanity.io/images/8nmbzj0x/production/53ede423cbb48aa49e3d15c3e78bf324e91ffcd8-1341x236.svg)

Van sends $50 to Tai

The entries in this transaction tell which accounts were affected. If each user’s balance is set up as an account, a transaction can simultaneously write an entry against each account. This ensures the total money in the system doesn't change, it just moves.

Now, let’s expand this model with more accounts and a handful of additional events:

![Simple ledger](https://cdn.sanity.io/images/8nmbzj0x/production/521df413d5e81576783122521d51439168351140-1496x316.svg)

Simple ledger of Tai and Van's accounts

Here I have a [ledger](https://www.moderntreasury.com/learn/what-is-a-ledger)—a log of events with monetary impact. We often see developers mutating balances directly rather than computing a balance from a log of transactions. This is suboptimal.

While mutating a balance directly is more efficient and simpler to implement, it’s more accurate to store immutable transactions and always compute balances from those transactions. Mutating balances directly creates a system that is prone to errors, as it becomes non-trivial to detect and reconcile inaccuracies.

Notice how each transaction has multiple entries. Each entry belongs to a transaction and an account. By comparing entries side by side, you can see where the money came from and what it was used for.

Double-entry ensures that, as transactions are logged, sources and uses of funds are clearly shown, and balances can be reconstructed as of any date:

![Transaction #2 - Van sends funds to Tai](https://cdn.sanity.io/images/8nmbzj0x/production/3118100e1edd94e46f3bf2cc8215bb9d8391ce18-662x208.svg)

Zooming in on a transaction from Van to Tai

This core idea— **one transaction, at least two entries, one representing the source and the other representing the use of funds**—is one of the foundational ideas of double-entry accounting \[ [1](https://www.moderntreasury.com/journal/accounting-for-developers-part-i#2e74e9c1afb3)\]. We’ll expand more on this later.

## Understanding Debit Vs. Credit Normal Accounts

As mentioned before, each account has a _type._ The two types we will cover here are **debit normal** and **credit normal.**

By definition:

- Accounts that represent funds you own, or **uses** of money, are **debit normal** accounts.
  - Debit normal accounts increase with a debit entry and decrease with a credit entry.
  - Examples of **uses** of funds are **assets**(e.g., cash)and **expenses.**
  - Note: The term “use” here is broadly defined: letting cash sit in a bank account is a use of funds, as well as selling on credit to someone else (you are effectively ‘using’ the money you’d get on a sale by extending them credit).
- Accounts that represent funds you owe, or **sources** of money, are **credit normal** accounts **.**
  - Credit normal accounts increase with a credit entry and decrease with a debit entry.
  - Examples of **sources** of funds are **liabilities, equity, or revenue**; this can mean investors' capital, accumulated profits, or income.
  - Note: “Source” is broadly defined here, too: if you are buying on credit, for instance, that is a “source” of money for you in the sense that it prevents you from spending money right now.

Let’s illustrate that with a simple table with two columns, the right side listing credit normal accounts and the left side listing debit normal accounts. We will place accounts that track uses of funds on the debit normal side and accounts that track sources of funds on the credit normal side.

![Debit Normal v Credit Normal](https://cdn.sanity.io/images/8nmbzj0x/production/15b1955ea793ebd6980b47d9a0422b3e48967112-624x526.svg)

What ladders up to debit normal and credit normal accounts

Buying inventory? That's a use (debit). Taking out a loan? That's a source (credit).

![Account categories](https://cdn.sanity.io/images/8nmbzj0x/production/f72f2a81fc3192ec7b6c740da9a08e8875bfded6-659x406.svg)

Account categories with simple mnemonics and examples

## What are Debits and Credits in Accounting?

Some of the guides we mentioned at the beginning of this post advise developers to “save the confusion and flush out debits and credits from your mind.” We do recognize that debits and credits can be challenging to grasp, but we think fully mastering these concepts is important when creating transaction handling rules.

Part of the confusion is that “debits” and “credits” are often used as verbs: to debit or to credit an account, which trip up developers. Debits and credits can also refer to _entries._ Here's the simplest way to think about them:

- **Debit entry:** Adds to a debit normal account or subtracts from a credit normal account
- **Credit entry:** Adds to a credit normal account or subtracts from a debit normal account

In software terms, it's like flags for how a value should affect a given account based on its type.

| Account Type | Debit | Credit |
| --- | --- | --- |
| Asset | + | - |
| Liability | - | + |
| Equity | - | + |
| Revenue | - | + |
| Expense | + | - |

Let’s continue to model out a few transactions to drive this point home. Let’s use a fictitious startup called Modern Bagelry—an eCommerce store for premium bagels.

In this example, we will use four accounts: **Cash** and **Inventory**(both debit normal accounts) as well as **Equity** and **Loans** (both credit normal accounts). Let’s say this ledger starts on a day T, and we are measuring time in days.

![Modern Bagelry Example](https://cdn.sanity.io/images/8nmbzj0x/production/7fdce0b4d515accffd0bb836740addc8f7ba8536-1502x509.svg)

Modern Bagelry example of debits and credits

A common misconception is that one account needs to decrease while another needs to increase. However, they can both increase or decrease in tandem, depending on the debit and credit entries in the transaction and the account types. In the first transaction cash increases because it’s a debit entry in a debit normal account (cash); equity also increases because it’s a credit entry in a credit normal account (equity). Conversely, in the last transaction both balances decrease because we are adding a debit entry into a credit normal account (loans) and a credit entry into a debit normal account (cash).

## How to Ensure Your Ledger is Balanced

The power of double-entry comes from ensuring the sum of al credit normal balances always equals the sum of all debit normal balances.

Let’s say we are aggregating the balances for each account in the example above right after each transaction takes place:

![Ending balances](https://cdn.sanity.io/images/8nmbzj0x/production/d20467e5d3cdbd266db2b7bf2381b70986ff1560-1445x411.png)

Ending balances for Modern Bagelry

A system of accounts will _balance_ as long as the balance on debit normal accounts equals the balance on credit normal accounts.

- The ending balances of debit normal accounts, **Cash**($1.22M)and **Inventory** ($250k), sum to $1.47M.
- The ending balances of credit normal accounts, **Equity**($1M)and **Loans**($470k), also sum to $1.47M.
- Total Debits = Total Credits.

Our Ledger is balanced. Not matching would mean the system created or lost money out of nothing.

## Summary: Accounting Principles Developers Should Know

Let’s recap the principles we reviewed so far:

- A ledger is a timestamped log of events that have a monetary impact.
- An account is a discrete pool of value that represents a balance you want to track.
  - Accounts are classified as debit normal or credit normal.
- A transaction is an event recorded in the ledger.
  - Transactions must have two or more entries.
- Entries belong to a ledger transaction and also belong to an account.
  - Entries change balances based on account type and entry direction.
    - Debits—or entries on the debit side—increase the balance of debit normal accounts, while credits decrease it.
    - Credits—or entries on the credit side—increase the balance of credit normal accounts, while debits decrease it.
- The system is correct if the sum of balances of all credit normal accounts matches the sum of balances of all debit normal accounts. This means all money is properly accounted for.

### What's Next?

In [Part II,](https://www.moderntreasury.com/journal/accounting-for-developers-part-ii) we’ll add some complexity to transaction structures, and we’ll bring everything together by doing a walkthrough of how to build a Venmo clone.

Modern Treasury [Ledgers](https://www.moderntreasury.com/products/ledgers) gives developers the tools to design scalable, [GAAP](https://www.moderntreasury.com/learn/what-are-gaap-accounting-rules)-friendly double-entry systems—no accounting background required. [Contact us](https://www.moderntreasury.com/talk-to-us) to learn how to integrate a reliable ledger into your stack.

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