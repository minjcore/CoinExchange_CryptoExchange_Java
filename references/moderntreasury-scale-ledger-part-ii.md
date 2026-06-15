[Journal](https://www.moderntreasury.com/resources/journal)

•November 17, 2022(Updated August 22, 2025)

# How to Scale a Ledger, Part II: Mapping Financial Events

In this second part of the series, we'll look at what it means for a ledger to be immutable and double-entry at the source of financial events.

![Image of Matt McNierney](https://www.moderntreasury.com/_next/image?url=https%3A%2F%2Fcdn.sanity.io%2Fimages%2F8nmbzj0x%2Fproduction%2Ff760febc9eb45453974c4a7df4a04f1133977f9b-512x512.jpg&w=64&q=75)

Matt McNierney/ Engineering

Contents

Explore With AI

Topics

[Ledgering](https://www.moderntreasury.com/resources/ledgering) [Engineering](https://www.moderntreasury.com/resources/engineering)

This post is the second chapter of a broader technical paper, [_How to Scale a Ledger_](https://www.moderntreasury.com/resources/ebooks/how-to-scale-a-ledger). In [Part I](https://www.moderntreasury.com/journal/how-to-scale-a-ledger-part-i), we covered why you should use a ledger database. In this chapter, we’ll cover core objects of a ledger, how some common financial events map into them, and how they are implemented in the Ledgers API.

This section focuses on the core fields and won’t be exhaustive of every feature available in the Ledgers API. See [the documentation](https://docs.moderntreasury.com/reference/ledger-object) for a complete reference.

## Adding Double-Entry to Financial Events

What does it take for a ledger to be immutable and double-entry at the source of financial events? At the most basic level, it means:

1. **Every monetary event is logged** using consistent, double-entry data model.
2. All balances shown to users and systems are **read from this data model**.

All financial flows—from credit card authorizations to crypto on-ramps—can be modeled with three core objects: account, entry, and transaction.

![Core objects for financial events](https://cdn.sanity.io/images/8nmbzj0x/production/268d8710aafd40fa9d1120b7ace005df54aa11c2-542x191.svg)

Core objects for financial events

## Account: The Sum of All Balances

An **Account** represents a discrete pool of value, and tracks a sum of money, denominated in a currency. Examples of Accounts include a digital wallet, a company’s operating cash, and loan principal.

![Account fields and descriptions](https://cdn.sanity.io/images/8nmbzj0x/production/078b4378261f0e5bc96f88cb433424d0752b28b5-814x371.svg)

Account fields and descriptions

Money moves between Accounts instantly within a ledger, even when real-world money movement happens asynchronously. For example, money sent via ACH takes at least a day to show up in a recipient’s bank account. Because real money movement can’t happen instantly, Accounts should be able to report a few different balances:

- **Posted balance**: fully settled funds
- **Pending balance**: fully settled funds plus posted funds (the money that’s expected to settle)
- **Available balance:** funds available to send out (does not include expected outgoing funds nor incoming unsettled funds)

![Balance fields and descriptions](https://cdn.sanity.io/images/8nmbzj0x/production/71848e7507a57fa36edf365463317e5e6f541c6d-814x296.svg)

Balance fields and descriptions

Not all ledgers model posted, pending, and available balances; many require developers to model these balances separately as separate Accounts. We believe that application ledgers should have these core primitives, as they reflect fundamental concepts in payment flows and must be easily accessible and queryable.

### Example: Credit Card Lifecycle

Let’s follow an example of a credit card to see how these balances are affected by different actions in the Ledgers API.

![Credit card example](https://cdn.sanity.io/images/8nmbzj0x/production/27f7ddc68de59b541a429260935b539dca8d2cc1-1035x455.svg)

How events affect the various balances on a credit card

We report these three balances separately so that the customer knows how much can be spent from an Account at any given time. Depending on the use case and risk tolerance, an app may choose to use each balance for a different purpose. For example, a customer may be limited by their available balance for transfers out of their Account, but their past-due status for a loan may be optimistically determined by their pending balance.

## Entry: An Immutable Record of Movement

Account balances are never directly modified. Instead, changes in balances are recorded as entries written to the Account. Entries are a complete log of balance changes into and out of an account, and include:

- Immutable core fields (`amount`, `direction`, etc.)
- A mutable `discarded_at` field (used only for pending reversals, and set when an Entry is replaced by a later Entry)

Let’s follow the same credit card example to see how each action can be represented as an Entry.

1\. A credit card starts with a $10,000 credit limit on the account.

![$10,000 credit limit](https://cdn.sanity.io/images/8nmbzj0x/production/d56884cb36d4ef6866e29e2bf3c41afc85dc37c4-451x374.svg)

$10,000 credit limit

2\. A card is swiped to purchase a $1,000 plane ticket. This purchase starts out pending on the card account.

![$1000 plane ticket](https://cdn.sanity.io/images/8nmbzj0x/production/c3aabc92a8c30204b37486fad2adeda4c0b588fe-451x374.svg)

Entry debiting funds for the $1,000 ticket

3\. That night, the purchase settles.

![Discarding pending entry, replacing with posted entry](https://cdn.sanity.io/images/8nmbzj0x/production/4dcd73036a3c9c8ce1fd73196e2e44a2d174b214-872x398.svg)

Replacing a \_pending\_ entry with a \_posted\_ entry

4\. The card holder initiates a $1,000 card payment from their bank account.

![New entry_3](https://cdn.sanity.io/images/8nmbzj0x/production/db18f2e65c20e8ad8580a605f8d45b78ae8514ea-451x374.svg)

Create new entry for the bank payment

5\. The card payment from the bank account completes.

![Replace with new entry_4](https://cdn.sanity.io/images/8nmbzj0x/production/6137567c777b087e8a0fdba57268c373095719a3-872x398.svg)

Replace with Entry\_4

6\. A hotel places a $250 hold on the card at the beginning of the stay, for incidentals.

![Create new entry_5](https://cdn.sanity.io/images/8nmbzj0x/production/8129ae712e79d5f6afb2789265017c02624d3279-451x373.svg)

7\. The $250 hold is removed at the end of the stay.

![Discard entry_5](https://cdn.sanity.io/images/8nmbzj0x/production/f1591b1f25819724f068271572407ed5f647e6a9-872x398.svg)

Replacing \_pending\_ entry with an \_archived\_entry

## Discarding Entries

Only pending Entries can be replaced; posted and archived Entries are permanent. Pending Entries get replaced in the following two circumstances:

1. The pending amount of an Entry needs to change.
2. The state of the Entry needs to change.

Why introduce this mutability in the API? To see why, it helps to consider the alternative. Instead of discarding Entries, we could create a reversal Entry that _undoes_ the original Entry. In this model, moving an Entry from pending to posted would create two Entries instead of just one.

In that case, step five above would instead be:

![Reverse entry_3 ](https://cdn.sanity.io/images/8nmbzj0x/production/776c17d7d31b69495e6daa44fafb251771759008-872x338.svg)

While valid, this approach leads to a messy Account history. We can’t distinguish between debits that were client-initiated and debits that are generated by the ledger as reversals. Listing Entries on an Account doesn’t match our intuition for what Entries comprise the current balance of the Account.

Discarding provides a clean, audit-safe method of updating pending movements without compromising history. You can see the current Entries by excluding any Entries that have `discarded_at` set.

### Computing Account Balances

Now that all balance changes are logged as Entries, how do we compute Account balances? Here’s where the `normal_balance` field on Account comes into play. This property defines how balances should respond to debit and credit entries.

Every Account in a ledger is categorized as debit normal or credit normal:

- **Debit-Normal Accounts**represent uses of funds (assets, expenses)
  - Debit Entries increase the balance; credit Entries decrease it
- **Credit-Normal Accounts**represent sources of funds (liabilities, equity, revenue)
  - Credit Entries increase the balance; debit Entries decrease it

Why bother with debits, credits, and Account normality at all? Many ledgers try to avoid complications by using negative numbers to represent debits and positive numbers to represent credits. At first glance, this appears to align better with engineers’ intuitions. But we chose to include Account normality, because without it, double-entry accounting is messy.

Consider a simple flow where a user deposits money into a digital wallet. This flow will affect the company’s cash Account and the user’s wallet Account. Our intuition is that the cash Account will increase (the company got cash from the user), and also the wallet Account will increase (the user has a positive balance in the digital wallet).

With a positive/negative number approach, it is impossible for both Accounts to increase. We have to pick one Account to be negative (so that no money is created or destroyed), and it’s not clear to which we should apply negative numbers.

Credits and debits solve this problem. We should debit the cash Account, and its balance increases because it is debit normal. And we should credit the user's wallet Account, and its balance also increases because it is credit normal. This mirrors real-world accounting logic and supports scale.

This digital wallet scenario is just one example. For a full primer on double-entry accounting, check out our series on [Accounting for Developers](https://www.moderntreasury.com/journal/accounting-for-developers-part-i).

### Core Fields for Balance Calculation

Now, we’ll focus on how to actually implement a double-entry ledger. With some simple math, each type of balance (pending, posted, and available) can be calculated from the following five fields:

- `posted_debits`: sum of posted debit Entries
- `posted_credits`: sum of posted credit Entries
- `pending_debits`: `posted_debits` plus the sum of non-discarded pending debit Entries
- `pending_credits`: `posted_credits` plus the sum of non-discarded pending credit Entries
- `normal_balance`: One of credit or debit, stored on the Account

A [ledger database](https://www.moderntreasury.com/products/ledgers) should be optimized to retrieve these five fields quickly, and only compute posted balance, pending balance, and available balance upon request.

Each balance type is then computed as follows:

### Posted Balance

JSON

```
case normal_balance
when "credit"
  posted_credits - posted_debits
when "debit"
  posted_debits - posted_credits
end
```

### Pending Balance

JSON

```
case normal_balance
when "credit"
  pending_credits - pending_debits
when "debit"
  pending_debits - pending_credits
end
```

### Available Balance

JSON

```
case normal_balance
when "credit"
  posted_credits - pending_debits
when "debit"
  posted_debits - pending_credits
end
```

### Next Steps

This is the second chapter of a broader technical paper with a more comprehensive overview of the importance of solid ledgering and the amount of investment it takes to get right.

If you want to learn more, [download the paper](https://www.moderntreasury.com/resources/ebooks/how-to-scale-a-ledger), or [get in touch](https://www.moderntreasury.com/talk-to-us).

#### Read the rest of the series:

[Part I](https://www.moderntreasury.com/journal/how-to-scale-a-ledger-part-i) \| [Part III](https://www.moderntreasury.com/journal/how-to-scale-a-ledger-part-iii) \| [Part IV](https://www.moderntreasury.com/journal/how-to-scale-a-ledger-part-iv) \| [Part V](https://www.moderntreasury.com/journal/how-to-scale-a-ledger-part-v) \| [Part VI](https://www.moderntreasury.com/journal/how-to-scale-a-ledger-part-vi)

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