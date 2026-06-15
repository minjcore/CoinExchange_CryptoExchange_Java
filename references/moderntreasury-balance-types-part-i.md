[Journal](https://www.moderntreasury.com/resources/journal)

•September 18, 2025(Updated December 9, 2025)

# Fintech Eng Challenges, Part I: Different Balance Types in a Wallet

Learn what balance types mean for a digital wallet product, and unpack the engineering challenges of (and strategies for) keeping them consistent at scale.

![Image of Lucas Rocha](https://www.moderntreasury.com/_next/image?url=https%3A%2F%2Fcdn.sanity.io%2Fimages%2F8nmbzj0x%2Fproduction%2F55163c3dc423adcb31083f7d47eca293dc95c46a-2875x2679.jpg&w=64&q=75)

Lucas Rocha/ Product Manager

Contents

Explore With AI

Topics

[Ledgering](https://www.moderntreasury.com/resources/ledgering) [Engineering](https://www.moderntreasury.com/resources/engineering)

When customers open their digital wallet, the “balance” they see can mean several different things, depending on the context. It can represent fully cleared money, authorized but unsettled money, or even remaining available credit for borrowing. Engineers building digital wallets must provide a source of truth for the question: _what’s my balance?_

Businesses rarely expose raw ledger entries directly to end users. Instead, they compute and surface different balance types depending on the use case. This article explains what balance types are, why they exist, how fintech companies define them differently, and the engineering challenges of supporting them at scale.

## What Balance Types Are Needed in a Wallet?

In a ledger database, every transaction is recorded immutably as a debit and a credit. A balance is simply an aggregation of those transactions over time. A balance type defines which subset of transactions count toward that aggregation (e.g., only posted transactions, or both posted and pending).

Wallet balance types are abstractions of this underlying ledger, and they allow systems to answer questions like:

- How much money has fully settled? ( **`posted_balance`** helps internal users for reconciliation purposes)
- How much can the customer spend right now? ( **`available_balance`** prevents overspending and failed transactions)
- What has the customer purchased recently? ( **`pending_balance`** helps end users see transactions in motion)

## Core Balance Types

### 1\. Posted Balance

The posted balance represents transactions that have fully settled. In a wallet product, the posted balance is the basis for reconciliation, the process of matching subledgers to the underlying bank. It reflects the final state of funds.

**Formula:** Posted Balance = Σ(Posted Inflows) – Σ(Posted Outflows)

**Example:** Let’s look at a merchant wallet for "TechGear, Inc" on an e-commerce platform.

![Wallet: TechGear, Inc](https://cdn.sanity.io/images/8nmbzj0x/production/1f289135d4553614afed78d3ed30b7ce04693363-1600x525.svg)

Posted debits and credits for TechGear, Inc.

**Posted Balance =** 1,000,000 + 284,000 - (175,000 + 8,420 + 2,150) = ~$1.1M

### 2\. Pending Balance

The pending balance includes all money movement, regardless of its settlement status. Pending balances give customers a more up-to-date view of their activity, but are less reliable since some pending items may never post. For example, an inflow funded by an ACH debit on an external bank account may fail because of insufficient funds. On the outflow side, an incidental hold on your card placed at the beginning of a hotel stay won’t ever post and will eventually expire.

**Formula:** Pending Balance = Σ(Posted Inflows + Pending Inflows) – Σ(Posted Outflows + Pending Outflows)

**Example:** Let’s look at a business account for “VeryVest,” processing end-of-day activities on an investing platform.

![Wallet: VeryVest ](https://cdn.sanity.io/images/8nmbzj0x/production/74f54682d4dcd925339b24b82b0e61940281e42c-1600x648.svg)

Pending debits and credits for VeryVest

**Pending Balance =** 1,000,000 + (500,000 + 12,450) - (750,000 + 225,000) = ~$547k. This balance represents authorized but unsettled transactions moving through various clearing networks (e.g., Nacha, Fedwire, Options Clearing Corporation).

### 3\. Available Balance

The available balance is more conservative than the posted balance. While the posted balance includes only posted money movement, available balance subtracts money that’s moving out of the account. This balance is useful when you want to display a spendable balance to customers. You don’t want them to spend money that’s already on the way out, even if that outflow could possibly fail.

**Formula**: Available Balance = (Posted Inflows) – (Posted Outflows + Pending Outflows) [(1)](https://www.moderntreasury.com/journal/fintech-eng-challenges-part-i-different-balance-types-in-a-wallet#4e372cea4a6d)

**Example:** Let’s look at a property management company "Mod Condo, LLC" holding funds for a rent-based rewards platform:

![Wallet: Mod Condo](https://cdn.sanity.io/images/8nmbzj0x/production/4ed463c73423bd9952dad3264907ae3831b9aeaf-1600x638.svg)

Posted and pending debits and credits for Mod Condo

**Available Balance =** 1,000,000 + (24,750) - (45,000 + 550,000) = ~$429K. This is how much the property manager can allocate for immediate rewards purchases or owner distributions.

## How Wallet Companies Use Balance Types

Fintechs designing wallets vary in their use of balance types. Although almost all have a form of the core balance types above, most wallet providers usually emphasize available balances for UX (e.g., Paypal, CashApp, Venmo are all consumer wallets that expose the available balance as a default). However, even this can vary:

### Alternative Balance Types and Calculations

**1\. One of our fintech-as-a-service (FaaS) customers** has a wallet they white-label for their clients. They designed this wallet to have four balance types:

- The usual suspects: `available`, `pending`, `posted`
- And another balance type: `reserved_balance`, for earmarked amounts not yet spent (e.g., holds). They differentiate this from `pending_balance` which includes balances awaiting completion.

This design supports wallets that must serve both customer-facing queries and compliance/reconciliation.

**2\. A fintech firm offering credit building services** has a wallet as the user-facing app; they calculate their `available_balance` with this formula:

Available = Posted Inflows + Pending Inflows + Posted Outflows

In this case, it is calculated nearly opposite to what we’d expect. It’s an optimistic balance: the balance is as large as possible (i.e., once the payment is initiated, they want to show the available balance going back up immediately). This makes sense for this company that is focused on credit-building and manipulating the customer-facing metric to help improve credit profiles.

**3\. For wallets that are the user-facing application** of a lending or credit product, balance types often include credit limits or remaining spend capacity.A credit limit can be calculated as: Available Credit = Credit Limit – Available Balance. The Available Balance represents what a customer owes, so subtracting it from the Credit Limit gives back how much purchasing power the customer has left.

These are just a few examples of how balance types will vary across businesses, and why it’s so important to accurately chart and model accounts, as varied definitions can increase engineering complexity. The most important thing to optimize for is customer trust: wallets need to show balances in a way that matches expectations.

## Engineering Challenges With Multiple Balance Types

### 1\. Atomicity and Consistency

When a transaction posts, multiple balances (posted, pending, available) need to update together. If only one updates, the system drifts out of sync.

Ruby

```
-- Example: two updates that should be atomic
UPDATE balances SET posted = posted + 1000 WHERE account_id = 'acct_123';
UPDATE balances SET available = available + 1000 WHERE account_id = 'acct_123';
```

If the second query fails (e.g., network blip), posted ≠ available. The ledger remains correct, but balance views are inconsistent. This risk multiplies under high concurrency, as many tries are going through at once. Failed payments = bad UX.

### 2\. Balance Drift

Cached balances may drift from true ledger values if writes fail or reconciliation lags (e.g., if the available balance shows as $5,000, but in reality it is $4,750 due to a missed pending entry). This can immediately break trust.

Ruby

```
# Cache update fails but ledger writes succeed
ledger.insert(txn)
try:
    cache.update_balance(txn)  # network error here
except:
    pass  # drift introduced: cache != ledger
```

### 3\. Multi-Currency Wallets

Customers and partners have the same rapid response expectations for their wallets, even with added complexity of multi-currency (e.g., stablecoins or foreign currency). This means balances need to be tracked for each currency separately, with FX conversions as a snapshot for total wallet balance calculations.

### Uplevel Your Ledger

For more best practices on designing and building a ledger database that grows with your company, check out our ebook _How to Scale a Ledger_.

[View page](https://www.moderntreasury.com/resources/ebooks/how-to-scale-a-ledger)

## Strategies to Address These Challenges

- **Always anchor balances to a ledger:** Keep balance logic modular: ledger = source of truth; APIs = interpretation layer. Document balance definitions explicitly for end users and developers.
- **Strong Ledger Guarantees:** Use immutable, double-entry transactions to ensure every balance calculation has a solid foundation.
- **Locking and Versioning:** Apply mechanisms like `lock_version` on ledger writes to avoid race conditions. Use pessimistic logic to help prevent failed transactions.
- **Balance Caching with Reconciliation:** Cache balances for performance. Periodically reconcile against the ledger to detect drift.
- **Flexible Balance Objects:** Expose balances through structured APIs (e.g., Modern Treasury’s [Ledger Balances](https://docs.moderntreasury.com/platform/reference/ledger-balances-object)), allowing different balance views to coexist. For lending wallets, define clear rules (e.g., what counts toward available balances and how limits interact).

If you’re designing a wallet, don’t treat “balance” as one field. These definitions shape customer trust. Getting them right means preventing failed payments, ensuring compliance, and enabling new products to thrive. Some tips for the road:

1. **Define your balance types up front.**
   - Document posted, pending, and available balances.
   - Decide which are customer-facing vs. internal.
2. **Tie balance updates to immutable ledger entries.**
   - Never update balances independently of transactions.
   - Use database transactions to keep posted, pending, and available balances consistent.
3. **Plan for drift detection.**
   - Build reconciliation jobs that compare cached balances against the ledger.
   - Alert on any mismatch. Even better, turn off caching as soon as a mismatch is detected.
4. **Expose balances clearly in APIs.**
   - Name fields explicitly (`available_balance`, `pending_balance`, etc.).
   - Link to docs that explain exactly how each is calculated.

## Conclusion

Multiple balance types are a necessary feature of wallets, but supporting them introduces challenges for engineers. Each definition solves a real business need, but only when implemented carefully.

Start by mapping which balances your wallet needs today. Then, build your schema so that adding new balance types later won’t require a rewrite. The challenge is to make sure your underlying ledger can scale across balance definitions without sacrificing reliability. We’ve done it, so we can help. Read our [full documentation](https://docs.moderntreasury.com/ledgers/docs/overview) or [reach out to us](https://www.moderntreasury.com/talk-to-us).

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