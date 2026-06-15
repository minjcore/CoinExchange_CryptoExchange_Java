[Journal](https://www.moderntreasury.com/resources/journal)

•October 20, 2022(Updated August 14, 2025)

# Accounting for Developers, Part III: Building a Lending Marketplace

In this final part of our Accounting for Developers series, we explore the ins and outs of building a ledger to power a lending marketplace product.

![Image of Lucas Rocha](https://www.moderntreasury.com/_next/image?url=https%3A%2F%2Fcdn.sanity.io%2Fimages%2F8nmbzj0x%2Fproduction%2F55163c3dc423adcb31083f7d47eca293dc95c46a-2875x2679.jpg&w=64&q=75)

Lucas Rocha/ Product Manager

Contents

Explore With AI

Topics

[Back Office](https://www.moderntreasury.com/resources/back-office) [Ledgering](https://www.moderntreasury.com/resources/ledgering)

## Introduction

In the first two parts of our _Accounting for Developers_ series, we covered foundational accounting principles ( [Part I](https://www.moderntreasury.com/journal/accounting-for-developers-part-i)) and applied them to a Venmo-style wallet app ( [Part II](https://www.moderntreasury.com/journal/accounting-for-developers-part-ii)).

Now, we’ll walk through how to design a **double-entry ledger for a lending marketplace**, akin to Lending Club, using a simplified version of a consumer platform we’ll call **Modern Lending**. These concepts apply to most lending [ledger](https://www.moderntreasury.com/learn/what-is-a-ledger) systems, including P2P platforms, embedded lenders, and credit fintech apps.

## Who This Guide is For

This guide is ideal for engineers building or scaling products that feature a lending component. Whether you're supporting investor returns, loan disbursements, or repayment flows, double-entry accounting is your best tool for ensuring correctness and traceability at scale.

Double-entry:

- Prevents funds from being created or destroyed
- Ensures balances are always accurate
- Provides a clear snapshot of uses and sources for each transaction, which is useful for reporting

Using double-entry constraints in database development is a best practice for fintechs and other companies that move money (read more about this in [How And Why Homegrown Ledgers Break](https://www.moderntreasury.com/journal/how-and-why-homegrown-ledgers-break)).

## A Quick Refresher on Double-Entry

As a reminder, obeying double-entry accounting rules boils down to following these principles:

- Your ledger should be composed of **accounts** and **transactions**
- Accounts represent the balances your ledger will track; they can be classified as **debit normal** or **credit normal**
- Transactions represent business events that have a monetary impact
  - They are composed of at least two **entries**
  - Entries can be on the **debit side** or **credit side**
- The aggregate balance of all credit normal accounts and all debit normal accounts in a ledger should net to zero (credits = debits).

(For more on these concepts, revisit [Part I](https://www.moderntreasury.com/journal/accounting-for-developers-part-i) of our series.)

## Building Modern Lending, Step 1: Lending Use Case Overview

Our fictional lending platform, Modern Lending, is a peer-to-peer lending marketplace where individuals can set their risk preferences, accept interest rates, and invest directly on the platform.

Modern Lending then lends out the money to borrowers on the platform, adjusting the amount and interest rate based on their creditworthiness. Borrowers pay back their principal balance over time, and after repayments, Modern Lending returns capital to investors, plus interest.

Modern Lending recognizes (books) the spread between the interest it gets paid by borrowers and the interest it pays to investors as revenue.

Let's review the product requirements for this platform, acknowledging that users should be able to self-select as _investors_ or _borrowers_ during onboarding:

### For investors:

- During onboarding, they select their investing options, varying by **term**(when they get their money back) and **interest rate** (how much they get in return).
- After onboarding, they deposit funds into Modern Lending's pooled cash account. This investment would be represented in their account as a balance.
- At term maturity, they receive their principal back plus interest.

### For borrowers:

- During onboarding, they submit financial data (e.g., income) to determine creditworthiness.
- They choose their loan option—varying by amount, term, and interest rate—and receive disbursement via wire, ACH, or [RTP](https://www.moderntreasury.com/learn/what-is-rtp) transfer.
- They make monthly payments against their principal and interest balance, which would be represented in-app.

### Additional product assumptions:

- There are no transaction costs or fees.
- We are building this application for scale—Modern Lending should be configured to handle thousands of investors and borrowers triggering disbursements and collections every day.
- We would like to build our ledger flexibly to accommodate future product expansions.

### This scenario creates technical requirements for a ledger that:

- **Logs transactions in real-time.** For this, we will need a [ledger API](https://www.moderntreasury.com/learn/ledger-api) that embeds directly into our application code and writes into the [ledger database](https://www.moderntreasury.com/learn/ledger-database) as events happen. Modern Lending needs to parse financial transactions and translate how to write these in the ledger—we’ll call this translation service transaction handling logic.
- **Keeps balances up to date consistently and automatically**. Transactions need to be parsed appropriately, aggregations need to be efficient, and balances need to be updated with minimal latency. They also need to be queryable to support transactions such as showing the user an updated balance after a transaction is completed. For this to be true, we need to map our chart of accounts to their given _normality—_ a set of constraints that will help the ledger obey double-entry rules.
- **Accommodates a high volume of transactions and support programmatic expansion.** In essence, this needs to be a central ledger that is divorced from fund movements (i.e., our underlying bank account setup) and is fully programmatic and flexible. The ledger’s functionality and underlying data models should not be tightly coupled to business logic.

## Step 2: Understanding the Lending Data Flow

Before we start with the ledgering setup, it’s important for us to understand how data flows in a lending system. Modern Lending would need to implement the following services infrastructure alongside its ledger.

![How Modern Lending's systems come together](https://cdn.sanity.io/images/8nmbzj0x/production/92eeeb340ff315e3aeba8763c318246db06f6038-1432x401.svg)

How Modern Lending's systems come together

There are two kinds of data Modern Lending needs to keep track of:

1. **Historical data,** or data that reliably represents the current financial state of Modern Lending based on posted transactions.
2. **Prospective data,** specifically those that are defined by the business model: interest rates, loan terms, payment amounts, and the payment breakdown of interest versus principal.

Modern Lending will need to make use of **an [amortization schedule](https://www.investopedia.com/terms/a/amortization_schedule.asp)**, a tool that calculates monthly payments based on interest rates, principal, and loan terms. Lending businesses often front-load a larger percentage of a customer’s payments towards interest versus principal. An internal service on Modern Lending’s backend can track this data and modify outputs based on new inputs (i.e., new loan terms).

The [ledger database](https://www.moderntreasury.com/learn/ledger-database) acts in tandem with the amortization schedule as the source of truth for historical information. Every time the amortization schedule service needs to provide an updated view of current balances, it cross-references the amortization schedule with the ledger database to provide accurate data.

Keeping historical information in the ledger and the amortization schedule outside of the ledger keeps Modern Lending’s data store clean and referencable over the course of the lifetime of the loan.

Notice that the ledger also needs to interact with another service divorced from the underlying ledger: **payments and reconciliation logic**. This service represents Modern Lending’s payment processor of choice: this can be a card processor or Modern Treasury Payments API. Transactions come in as webhooks and get parsed as ledger-compatible entries according to the rules presented below.

## Step 3: Building A Chart Of Accounts for Lending

A chart of accounts is a simple depiction of the accounts Modern Lending will need, their type, and normality.

![Modern Lending's Chart of Accounts](https://cdn.sanity.io/images/8nmbzj0x/production/b22b878ccaf6288dfc1efd748087a728a1735d0d-1456x426.svg)

Modern Lending's Chart of Accounts

First, we have two general accounts: cash and revenue.

- **Cash** tracks the overall cash position of Modern Lending. As it represents a **use of funds**, it is a **debit normal** account.
- **Revenue** is a tally of how much money we recognize (book) as revenue during the regular course of business. It is a **credit normal** account because it represents a **source** of funds. For the sake of this example, Modern Lending has a single revenue stream: interest.

We also have a set of “n of” accounts. This is because each investor and each borrower will have **two sets** of accounts: one tracking principal and one tracking interest. While we will only need one cash and one revenueaccount, we need multiple sets of user accounts.

- **Principal accounts** track total capital invested by investors and total capital on loan to borrowers. The investor principal accounts are **credit normal** because they represent **sources** of funds—or funds Modern Lending owes. Conversely, borrower principal accounts are **debit normal** because they represent **uses** of funds—akin to receivables.
- **Interest accounts** track the net interest earned from the spread. They follow the same normality rules as the principal accounts: they are **credit normal** when they track interest due to investors and **debit normal** when they track interest owed to Modern Lending.

## Step 4: Mapping Lending Transaction Logic

Let’s define the five core transaction types for lending and show how they translate into entries on a double-entry ledger. (Note: these examples are meant to be illustrative of what transaction handling logic looks like; these are not exhaustive of all transaction types Modern Lending would need to parse out.)

1. **Investor deposit**. An investor adds money to their balance.
2. **Borrower disbursement**. A borrower initiates a loan and receives funds.
3. **Interest calculation.** Monthly interest gets added to the interest balance for both borrowers and investors. As part of this calculation, part of the interest gets recognized as booked revenue.
4. **Borrower repayment**. A borrower repays monthly installments covering both interest and principal.
5. **Investor withdrawal**. At the end of their term, Modern Lending sends principal plus interest owed to investors.

To illustrate this, we’ll simplify our marketplace down to one investor and one borrower, and assume the following data comes from our amortization schedule:

- **Lucy: Investor**
  - Depositing $10,000
  - Expects a 4.8% return upon completion of her 1-year term ($10,480)
- **Desi: Borrower**
  - Borrowing $5,000
  - Will pay 12% annual interest over the course of his 1-year loan ($5,600)
  - Therefore, Desi needs to make monthly payments of $466.67, with $416.67 ($5,000 ÷ 12) of this being directed towards principal, and $50 ($600 ÷ 12) directed towards interest.

### Transaction 1. Investor Deposit

![Transaction Type 1: Investor Deposit](https://cdn.sanity.io/images/8nmbzj0x/production/9c92a2d46a58335289198be17600b235fd88ffe6-1509x392.svg)

Lucy's $10K investor deposit

As Lucy deposits money on the Modern Lending platform, we **debit (increase)** the cash account and **credit (increase)** Lucy's investor balance account.

Notice that we don’t record any kind of interest due to Lucy on the ledger at this time. Interest is ledgered in a separate transaction (see below).

### Transaction 2. Borrower Disbursement

![Transaction Type 2: Borrower Disbursement](https://cdn.sanity.io/images/8nmbzj0x/production/e0c004abf31db40c303d74a694f1e756687c117b-1467x265.svg)

Desi gets disbursed a $5K loan

When Desi is approved for a loan and receives $5,000, we **credit (decrease)** our cash account and **debit (increase)** Desi's principal due account. As above, notice we do not record interest owed.

### Transaction 3. Interest Accrual (Monthly)

![Transaction Type 3: Interest Calculation](https://cdn.sanity.io/images/8nmbzj0x/production/66c3e2221cf9418a5a93718195720385c19c9335-1475x302.svg)

Monthly interest accrual

Lucy lent $10,000 and expects a 4.8% return in a year. That means her effective interest payment at maturity equates to $480. Assuming simple interest, we should add $40 to her interest balance every month ($480 ÷12). This is a **credit** because we are **increasing** a **credit normal** account.

Desi borrowed $5,000 at 12% to be paid in a year. This means Desi's final interest balance will be $600 at maturity. Assuming simple interest again, our ledger should add $50 every month to Desi's interest balance ($600 ÷ 12). This is a **debit** because we are **increasing** a **debit normal** account. (Let’s assume Lucy's remaining $5,000 just sits on Modern Lending’s cash account for now).

At the end of the year, Modern Lending will have received $600 from Desi and will owe Lucy $480 in interest. The difference of $120 is recognized as **revenue**( [1](https://www.moderntreasury.com/journal/accounting-for-developers-part-iii#260561223298)).Every month, as interest gets calculated, we add $10 to our revenue account ($120 ÷ 12).

### Transaction 4. Borrower Repayment (Monthly)

![Transaction Type 4: Borrower Repayment](https://cdn.sanity.io/images/8nmbzj0x/production/c8c26d3851ff1af015a4c87117b792fe7ab93f3a-1468x293.svg)

Desi pays his monthly installment

Desi will have to pay a total of $5,600 over 12 months ($5,000 in principal, and $600 in interest), or a monthly payment of $466.67.

After the transaction clears, we **debit (increase)** our cash account by $466.67. In the same transaction, we **credit (decrease)** both principal and interest balances for Desi. Principal gets deducted by $416.67 ($5,000 ÷ 12) and interest gets deducted by $50 ($600 ÷ 12).

The ledger shows one payment made by Desi, a principal balance of $4,583.33 ($5,000 - $416.67), and an _interest balance of zero._ This happens because we recognized interest in the “interest calculation” section and then immediately zeroed it out as the payment was made.

The sum of credits towards Desi's interest balance represents the total paid in interest. We can contrast this with the data from Modern Lending’s amortization schedule to derive Desi's remaining principal balance.

### Transaction 5. Investor Payout (Maturity)

![Transaction Type 5: Investor Disbursement](https://cdn.sanity.io/images/8nmbzj0x/production/314b1a05b8394c119918bede797baccd2c3b2fb7-1447x289.svg)

Lucy gets repaid at maturity

At term, Lucy is repaid her principal of $10,000 in addition to the 4.8% return—or $480—promised to her. We **credit (decrease)** our cash account, and simultaneously lower her principal and interest down to zero by **debiting (decreasing)** $10,000 and $480, respectively.

## Step 5: Bringing It All Together

Let’s review the architecture we would need to support this use case:

1. A **central ledger** that represents the entire collection of accounts and transactions and stores them with double-entry constrains
2. Two services:
1. A **Transaction Logic Service** that streams transaction data (typically a payments API like [Modern Treasury](https://www.moderntreasury.com/)) and converts events into valid ledger entries for at least the following transaction types:
      1. Investor deposit
      2. Borrower disbursement
      3. Interest calculation
      4. Borrower repayment
      5. Investor disbursement
2. An **Amortization Schedule** to properly calculate monthly payments and update balances
3. A **Chart of Accounts**, with _at least_ six types:
1. One debit normal cash account
2. One credit normal revenue from interest account
3. One credit normal investor principal account per investor in the platform
4. One credit normal investor interest account per investor in the platform
5. One debit normal borrower interest account per borrower in the platform
6. One debit normal borrower principal account per borrower in the platform.

## Conclusion: Building Trust into Lending Systems

Double-entry ledgering helps you:

- Maintain a clean and auditable system of record
- Prevent inconsistencies between scheduled payments and actual balances
- Enable future scalability (more loans, terms, investors, repayment flows)

Using double-entry is the best way to ensure integrity of the financial information flowing through Modern Lending’s product, but it requires a bit of setup and understanding of accounting principles. The hard part—we believe—is setting up the underlying ledger database with the right constraints and level of flexibility.

If you’re building a lending product and want to skip the complexity of building and maintaining ledger infrastructure in-house, take a look at [Modern Treasury Ledgers](https://www.moderntreasury.com/products/ledgers) and [reach out](https://www.moderntreasury.com/talk-to-us) if we can be helpful.

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