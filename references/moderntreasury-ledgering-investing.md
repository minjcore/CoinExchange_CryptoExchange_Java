[Journal](https://www.moderntreasury.com/resources/journal)

•January 27, 2025

# Key Ledgering Challenges for Investing Platforms

Here, one of our software engineers looks at the challenges investing platforms face and explores how designing a ledger can better address them.

![Image of Sahil Altekar](https://www.moderntreasury.com/_next/image?url=https%3A%2F%2Fcdn.sanity.io%2Fimages%2F8nmbzj0x%2Fproduction%2Ff7022994ed53458776039521dcc399b84a387950-512x512.png&w=64&q=75)

Sahil Altekar/ Software Engineer

Contents

Explore With AI

Topics

[Ledgering](https://www.moderntreasury.com/resources/ledgering) [Engineering](https://www.moderntreasury.com/resources/engineering)

Investing platforms process large numbers of securities-related transactions every second. As each user makes trades, the platform has to check their spending power, track the transfer and settlement of funds, and allocate securities to the end user.

To do this effectively, a good investing platform needs a good ledger. But maintaining a ledger is not easy. Things like complex calculations, collecting incoming data from financial intermediaries, and scaling the overall investing platform can make ledgering a challenge.

![How a ledger fits into an investing platform’s architecture.](https://cdn.sanity.io/images/8nmbzj0x/production/b8d1a8c4868196ac8cb9a68a3e0c7844f0efe33f-1600x923.jpg)

How a ledger fits into an investing platform’s architecture.

In this article, we will go through the challenges investing platforms face and how designing a ledger can better address them.

## **Typical Architectural Challenges in Ledgering**

### **Balance Queries**

Securities transactions require rapid querying of balances to compute buying power, margin calculations, and real-time balances. These queries must be well-structured, accurate, and fast. Additionally, providing an accurate record of historical balances is necessary to do aggregate calculations across time periods such as one month, six months, year-to-date (YTD), or other significant increments of time.

Some investment platforms choose to do live aggregations on their database to pull these balances, which can create additional performance and accuracy issues for users.

### **Reconciliation with Financial Intermediaries**

Recording transactions from users and reconciling them with reports from clearinghouses can be challenging due to the complexity of transactions. Transactions can be partially settled, have different timings than the investment platform’s internal system, or be missing entirely. This, along with the high volume of transactions, makes reconciliation a challenge.

Some investment platforms rely on the clearinghouse as the definitive source of information. This reliance means that, if there are discrepancies, the investment platform must overwrite its own database in order to update it, which can affect auditability and risk management. It also can introduce errors or cause further discrepancies that necessitate manual investigation in order to explain to users or auditors what happened to a transaction.

### **Scalability**

Processing large volumes of transactions accurately and swiftly, especially when reacting to events with “bursty” traffic like dividend payouts, is not [easy.](https://www.moderntreasury.com/journal/key-ledgering-challenges-for-investing-platforms#91f4a8ed45bb) For example, when a dividend is processed, any user who holds the underlying equity will receive a dividend, meaning many cash balances have to be updated simultaneously.

When a system isn’t architected to handle these bursts it can create high latency, which can lead to lag and even timeouts. This has downstream effects for the user when displaying up-to-date balances and impact buying power calculations.

**How a Well-Architected Ledger Helps**

For an investing platform, a well-architected ledger database has two primary responsibilities:

1. Store user transactions & holdings data in an accurate and structured way
2. Easily handle incoming data from financial intermediaries

With a ledger, an investing platform is able to store, persist, and retrieve data at low latencies, process billions of transactions quickly, and reconcile records effectively.

![A look at how a ledger can fit into the life cycle of a trading event.](https://cdn.sanity.io/images/8nmbzj0x/production/21862bbf2d5b756bce700e6dd62f991146cb963f-1600x951.jpg)

A look at how a ledger can fit into the life cycle of a trading event.

### **Storing Data**

An investing platform must accurately record all transactions and money movement, so its ledger needs the following features:

**Pending and Posted Balances**: A ledger should be able to model different balance types to encompass all transaction activity, including unsettled transactions. Knowing where a transaction is in its lifecycle helps especially when making buying power calculations in real-time especially when these calculations become more complex like margin trading.

**Conditional Transactions**: A ledger should be able to support rules for transaction settling to prevent misallocations and errors. For example, conditional transactions allow actions like taking funds out of a user’s account _only if_ they have sufficient balance.

**Idempotency:** A ledger should ensure requests are rejected if they are accidentally sent twice. This helps prevent user error and ensures availability of funds.

**Immutability:** A ledger should maintain a fixed record of all transactions and be able to easily revert to prior states during error handling.

**Multi-Currency**: A ledger should be built to support global currencies and as well as be able to model arbitrary units, like security counts.

**Double-Entry Accounting:** A ledger should be modeled to have matching credits and debits. This ensures that money is always tracked both within user liabilities and as an asset, which helps prevent errors. For example, when a user purchases a security, we can easily see the exchange of USD to stock units with [double-entry accounting](https://www.moderntreasury.com/journal/what-is-double-entry-accounting):

![](https://cdn.sanity.io/images/8nmbzj0x/production/21a86ec81dd202f14c4b0f1507be1f1decd09883-1600x432.jpg)

**Auditability**: A ledger should maintain versioning on user balances corresponding to transactions. This maintains historical accuracy and provides visibility into the origination of balance changes.

### **Dealing with Financial Intermediaries**

Investing platforms need to maintain their own ledger independent from any third parties and reconcile to the intermediaries’ systems. Some good rules of thumb:

1. **Maintain business logic outside of the intermediary.** For example, the investing platform may have more context on margin offerings which will affect buying power calculations (whereas a clearinghouse would be the source of truth for securities holdings but not cash).
2. **Avoid interacting with the clearinghouse API unnecessarily**, which is costly and can lead to latency problems over time, as transaction volumes scale.
3. **Operate in real-time.** Third parties may not operate in real-time, but the optimal user experience is providing instant updates when they make actions like trading and viewing balances.
4. **Choose how to store their data in a way that's beneficial for the business.** For example, storing your data in the ways described above makes it easier to retrieve the data in an efficient manner necessary for investing use cases and powering critical business needs.
5. **Spot check clearinghouses.** With an independent ledger, investing platforms can cross-verify data with third-party sources, help identify discrepancies, and decide when it's necessary to update records.

Dealing with latency requirements, scale, complex aggregations, and incoming data—while also maintaining real-time visibility to your users—is hard. However, the investment in a high-quality ledger is well worth it to know that your data and transactions are being accurately recorded.

If you’re ready to find a ledgering solution that works at scale for your business, [reach out to us](https://www.moderntreasury.com/talk-to-us).

Subscribe to our newsletter

Get the latest articles, guides, and insights delivered to your inbox.

Company Email\*

Subscribe

## Authors

![Image of Sahil Altekar](https://www.moderntreasury.com/_next/image?url=https%3A%2F%2Fcdn.sanity.io%2Fimages%2F8nmbzj0x%2Fproduction%2Ff7022994ed53458776039521dcc399b84a387950-512x512.png&w=1080&q=75)

Sahil AltekarSoftware Engineer

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