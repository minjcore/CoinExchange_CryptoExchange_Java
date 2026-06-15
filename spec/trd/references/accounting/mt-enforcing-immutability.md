[Journal](https://www.moderntreasury.com/resources/journal)

•December 14, 2021

# Enforcing Immutability in your Double-Entry Ledger

Immutability is one of three core principles to adhere to good engineering and accounting principles.

![Image of Jason Jong](https://www.moderntreasury.com/_next/image?url=https%3A%2F%2Fcdn.sanity.io%2Fimages%2F8nmbzj0x%2Fproduction%2Fb6118932a4c267f70dd9c236d2d467b709fedbbb-500x499.png&w=64&q=75)

Jason Jong/ Engineering

Contents

Explore With AI

Topics

[Ledgering](https://www.moderntreasury.com/resources/ledgering) [Back Office](https://www.moderntreasury.com/resources/back-office) [Engineering](https://www.moderntreasury.com/resources/engineering)

## Background

At Modern Treasury, we’ve designed our [Ledgers API](https://www.moderntreasury.com/journal/announcing-ledgers) to flexibly track any type of money movement. This makes it particularly easy to adapt ledgers to many of our customer use cases, like helping ClassPass track studio payouts when customers attend classes. We’ve been particularly intentional that we never violate three core principles for our ledger system: double-entry, auditability, and immutability. Oftentimes, we’ll get the question: why immutability? In this blog post we’ll discuss the importance of immutability in a ledger system.

## Fallacies with Mutability

When engineers design their first MVP, it’s not surprising that accounting systems and principles tend to be an afterthought.

Imagine designing a marketplace with merchants and consumers where consumers can place orders from merchants. This marketplace collects payment from consumers immediately and disburses these in a weekly payout to the merchant. The MVP might contain an Orders table. Once a week a cron job is run to calculate last week’s orders and generate a payout to the merchant.

![Diagram showing orders and payouts flow.](https://cdn.sanity.io/images/8nmbzj0x/production/3d033b25bf1694bbe265e64e5cd9481c4fb37444-2000x1032.png)

The Orders table is effectively your “ledger” and tracks how much money has been collected and will be paid out. This would work in a simple world. However, the real world is much more complex. Order adjustments, fraudulent credit card transactions, returns, and refunds can all change the final amount your platform collects for an order.

It would be simple to modify the _amount_ in the Orders table. However, this then leads to many downstream accounting problems. What if an adjustment happened two weeks later? The payout you've made to the merchant is now incorrect and you have no way to attribute the adjustment to the original payout. Equally difficult is tracking down the discrepancy in the payment to the merchant. Because the payout is the sum of many orders, your accounting team would need to dig through your list of orders to find the offending order. Imagine if a merchant had thousands of orders and complained about a $10 discrepancy in their $10,000 payout. It could take days to track this down.

## Implementing Immutability

In a [double-entry system](https://www.moderntreasury.com/journal/what-is-double-entry-accounting), accountants can generally reverse-engineer the discrepancies that have happened. However, if the data has been mutated, then the data is irreversibly destroyed and then becomes impossible to figure out what changed.

In the above example, it’s important to isolate business-level objects (e.g. _Orders_) from your accounting system. The business-level objects are a mutable presentation to your end-user whereas the accounting-level objects represent trackable money movement and the ultimate “source of truth.”

![Diagram show business-level objects (orders, payouts) flowing into accounting-level objects (ledger transactions). ](https://cdn.sanity.io/images/8nmbzj0x/production/8361f6ae05855cf3a91ce16bb552f34b7a5c3f88-2000x1227.png)

Let’s imagine you have a table called “Ledger Transactions” that records accounting-level entries. When an _Order_ is created, the system should create a _Ledger Transaction_ to represent the money collected by you but owed to the merchant.

If order adjustments are made the ledger transaction stays immutable. There are two approaches here:

1. Completely reverse the ledger transaction by creating a new ledger transaction with the opposite amount. Then, then create a new ledger transaction with the new correct amount
2. Calculate the difference between the ledger transaction’s new correct amount, and create a new ledger transaction that sums the two to the correct amount

In the above multiple _Ledger Transactions_ can point to one _Order_. It becomes particularly easy to pull up all the ledger transactions tied to an Order. The sum of all ledger transactions should equal the mutable _Order.amount_ column, but of course this may not be the case for all systems (e.g. if the platform takes a cut of the payment for facilitating the ledger transaction).

This functionality becomes crucial if an adjustment is made after a weekly Payout. An order can then technically be paid out in more than one payout. This conceptually makes sense—if an adjustment is made in the following week it should not affect a prior week’s payout.

![Diagram: Order → Ledger Transaction (x2) ← Payout](https://cdn.sanity.io/images/8nmbzj0x/production/17fda6efa7b2b0a5bf2ac4e6986067485e891e6a-2000x1107.png)

With immutability enforced your product will have a paper trail and the above situation becomes significantly easier to figure out discrepancies.

## Why does Ledger Transaction have a pending and posted state?

In our ledger system, we have a status column on _Ledger Transactions_. This status can be either _pending_ or _posted_. A ledger transaction is mutable while pending and immutable once posted.

Payments, like [ACH payments](https://docs.moderntreasury.com/docs/ach) or [wires](https://docs.moderntreasury.com/docs/wire), go through a complex flow in the banking system to transition from in-progress to completed. Ledger transactions need to track these state transitions. For the merchant’s weekly ACH payout, for example, the ledger transaction begins in the _pending_ state. The ledger transaction transitions to _posted_ if completed, but if any issues arise, we can alternatively mark the ledger transaction as _archived_. The reason for this is that most bank payments are not instant: they take up to three days to post at which point they may still fail. This process closely mirrors what you see in your bank account statement, which is at its core a ledger. We’ve outlined how these statuses are mapped [here](https://docs.moderntreasury.com/docs/linking-to-other-modern-treasury-objects).

You might wonder why we don’t just make everything immutable. Why wouldn’t we simply create a new reversed ledger transaction if a payment fails? This is a different but valid design as well, but leaves a messier paper trail when honest mistakes happen. Imagine if your keyboard didn’t have a backspace key and mistakes had to be corrected with a reversal. We simply chose our design to reflect exactly what is happening in the bank rails. Of course the _pending_ state may not make sense for all products, and it’s up to the discretion of the developer to decide when this is appropriate.

## Summary

Immutability in a double-entry ledger system is one of the core defining principles to adhere to good engineering and accounting principles. By using Modern Treasury’s double-entry ledger we enforce immutability in a way that your current and future finance and accounting team will thank you.

If you’re interested in learning more about our [Ledgers API,](https://docs.moderntreasury.com/docs/ledgers-overview) please reach out to our team [here](https://www.moderntreasury.com/talk-to-us).

Subscribe to our newsletter

Get the latest articles, guides, and insights delivered to your inbox.

Company Email\*

Subscribe

## Authors

![Image of Jason Jong](https://www.moderntreasury.com/_next/image?url=https%3A%2F%2Fcdn.sanity.io%2Fimages%2F8nmbzj0x%2Fproduction%2Fb6118932a4c267f70dd9c236d2d467b709fedbbb-500x499.png&w=1080&q=75)

Jason JongEngineering

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