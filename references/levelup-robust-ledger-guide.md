[Sitemap](https://levelup.gitconnected.com/sitemap/sitemap.xml)

[Open in app](https://play.google.com/store/apps/details?id=com.medium.reader&referrer=utm_source%3DmobileNavBar&source=post_page---top_nav_layout_nav-----------------------------------------)

Sign up

[Sign in](https://medium.com/m/signin?operation=login&redirect=https%3A%2F%2Flevelup.gitconnected.com%2Fbuilding-a-robust-ledger-an-engineers-guide-to-double-entry-accounting-79804045abb0&source=post_page---top_nav_layout_nav-----------------------global_nav------------------)

[Medium Logo](https://medium.com/?source=post_page---top_nav_layout_nav-----------------------------------------)

Get app

[Write](https://medium.com/m/signin?operation=register&redirect=https%3A%2F%2Fmedium.com%2Fnew-story&source=---top_nav_layout_nav-----------------------new_post_topnav------------------)

[Search](https://medium.com/search?source=post_page---top_nav_layout_nav-----------------------------------------)

Sign up

[Sign in](https://medium.com/m/signin?operation=login&redirect=https%3A%2F%2Flevelup.gitconnected.com%2Fbuilding-a-robust-ledger-an-engineers-guide-to-double-entry-accounting-79804045abb0&source=post_page---top_nav_layout_nav-----------------------global_nav------------------)

![Unknown user](https://miro.medium.com/v2/resize:fill:64:64/1*dmbNkD5D-u45r44go_cf0g.png)

[**Level Up Coding**](https://levelup.gitconnected.com/?source=post_page---publication_nav-5517fd7b58a6-79804045abb0---------------------------------------)

·

Follow publication

[![Level Up Coding](https://miro.medium.com/v2/resize:fill:76:76/1*5D9oYBd58pyjMkV_5-zXXQ.jpeg)](https://levelup.gitconnected.com/?source=post_page---post_publication_sidebar-5517fd7b58a6-79804045abb0---------------------------------------)

Coding tutorials and news. The developer homepage [gitconnected.com](http://gitconnected.com/) && [skilled.dev](http://skilled.dev/) && [levelup.dev](http://levelup.dev/)

Follow publication

# Building a Robust Ledger: An Engineer’s Guide to Double Entry Accounting

[![Jian Ruan](https://miro.medium.com/v2/resize:fill:64:64/1*MpCbd_hjEQ4R9PDYJ2CvzQ.jpeg)](https://medium.com/@jianruan18?source=post_page---byline--79804045abb0---------------------------------------)

[Jian Ruan](https://medium.com/@jianruan18?source=post_page---byline--79804045abb0---------------------------------------)

Follow

11 min read

·

Dec 17, 2024

157

[Listen](https://medium.com/m/signin?actionUrl=https%3A%2F%2Fmedium.com%2Fplans%3Fdimension%3Dpost_audio_button%26postId%3D79804045abb0&operation=register&redirect=https%3A%2F%2Flevelup.gitconnected.com%2Fbuilding-a-robust-ledger-an-engineers-guide-to-double-entry-accounting-79804045abb0&source=---header_actions--79804045abb0---------------------post_audio_button------------------)

Share

Press enter or click to view image in full size

![](https://miro.medium.com/v2/resize:fit:700/0*oMC_N39ViSQ3aSCT)

Photo by [Kelly Sikkema](https://unsplash.com/@kellysikkema?utm_source=medium&utm_medium=referral) on [Unsplash](https://unsplash.com/?utm_source=medium&utm_medium=referral)

## Introduction

As a technical founder in the fintech space, one of the first lessons I learned was how crucial it is to build a robust and scalable ledger. It is the backbone of any system that handles money — whether you’re building a fintech startup, launching a financial service, or a traditional business expanding into embedded finance. Without a reliable ledger that serves as the **single source of truth** for all money movement, managing complex financial transactions can quickly turn into a nightmare.

However, as an engineer, I first struggled with the core concept in building a robust ledger: **double-entry accounting**. While many tutorials explain accounting scenarios using tables, the concept often didn’t feel intuitive to me. It made logical sense, but I couldn’t quite connect it to the technical systems I was designing.

Everything finally clicked when I started **visualizing** these accounting principles as **vectors** — **with both magnitude and direction**. This approach gave me a clearer mental model, and in this post, I’ll share how I came to understand these concepts — and how you can too.

## What Is Double Entry Accounting?

Double-entry accounting means that **every transaction affects at least two accounts**, with the **total debits always equal the total credits**. In simple words, for every transaction, you track both where the money came from and how it was used, making sure that money doesn’t just appear or disappear out of nowhere.

## Understanding Debits and Credits Through the Expanded Accounting Equation

### Debits

Debits increase the value of **Assets** or **Expenses**.

Example: When your business receives cash, the cash account (an asset) is debited because its value increases.

### **Credits**

Credits increase the value of **Liabilities** or **Revenues**.

Example: If your business takes out a loan, the loan account (a liability) is credited to reflect the additional obligation.

**Assets = Liabilities + Equity** is the standard accounting equation, which can be expanded into a more detailed version as shown in the following chart.

Press enter or click to view image in full size

![](https://miro.medium.com/v2/resize:fit:700/1*oW5wmSLiLwsEKU_8JUpECQ.png)

The accounting equation: Assets, liabilities, and equity explained. Source: Created by the author.

## Debit Normal Accounts & Credit Normal Accounts

### Debit Normal Accounts

These accounts hold a **debit balance**, meaning their value _increases with debits_.

- **Assets:** cash in a bank account, equipment, or inventory, which represent investments or holdings.
- **Expenses:** salaries, rent, or utilities, which represent costs incurred.

### Credit Normal Accounts

These accounts hold a **credit balance**, meaning their value _increases with credits_.

- **Liabilities:** loans payable or accounts payable, which represent obligations or borrowed funds.
- **Revenue:** sales or service income, which represent money earned.

Press enter or click to view image in full size

![](https://miro.medium.com/v2/resize:fit:700/1*q3BiL_qtoIXQWrtRDgD76A.png)

Normal accounts: Debit (uses of money) and credit (sources of money). Source: Created by the author.

## Example: Lending Co.

Let’s break this down using **Lending Co.** as an example. Lending Co. is a platform that connects investors like Alice with borrowers like Bob. Here’s the scenario:

- **Alice (Investor):** She deposits $10,000 into Lending Co., expecting a 4.8% return on her investment ($480).
- **Bob (Borrower):** He borrows $5,000 from Lending Co. at 12% interest, meaning he owes $600 in interest.

## Accounting Ledger — Separating Debits and Credits into Two Columns

Here’s how these transactions are recorded in an accounting ledger, where debits and credits are clearly separated into two distinct columns. Each transaction is recorded as a row, ensuring that the **total debits always equal the total credits**. You can determine the balance for each account by adding up the entries in corresponding columns.

Press enter or click to view image in full size

![](https://miro.medium.com/v2/resize:fit:700/1*TcLZFzp0P0_fgoNiSTLs0Q.png)

Accounting ledger example: Debits and credits separated into two columns for human readability. Source: Created by the author.

## The Challenge — Translating Accounting Ledgers into Database Logic

Now, here’s where things get more complex. While reading an accounting ledger like the above is intuitive for humans, handling balances in a database becomes more challenging. To calculate a balance:

1. You first need to first filter all debits and credits for an account.
2. Then, based on the account type (debit normal or credit normal), decide whether to increase or decrease the balance. For example:

- For **debit normal accounts**, a debit increases the balance, and a credit decreases it.
- For **credit normal accounts**, it’s the reverse — a credit increases the balance, and a debit decreases it.

### Why This Can Feel Confusing 🤯

This process can feel like juggling two sets of rules. You’re constantly switching logic depending on the account type — when to increase, when to decrease — which can make simple calculations feel unnecessarily complicated.

Press enter or click to view image in full size

![](https://miro.medium.com/v2/resize:fit:700/1*ewSBv5tuv1HeFvAcTx48tg.png)

Reference table for when to increase or decrease different account types. Source: Created by the author.

## Database Ledger with `amount_signed`: A Streamlined Approach to Debits and Credits

Let’s explore a simpler and more efficient way to track balance in a ledger — using **signed amounts**. Instead of maintaining separate columns for debits and credits, you assign negative values to debits and positive values to credits. (Of course, you could reverse thisand assign negatives to credits instead, as I’ll explain later through the lens of vectors — these values simply represent direction.)

Here’s how the `amount_signed` version makes life easier:

- **Every transaction naturally balances to zero:** Debits and credits offset each other, ensuring accuracy.
- **Calculating balances becomes straightforward:** With a single column, you can sum the signed amounts to determine an account’s balance in one step.

Press enter or click to view image in full size

![](https://miro.medium.com/v2/resize:fit:700/1*0e6CRpBFr9nPhcWCxvitqQ.png)

Database ledger example using `amount_signed` to streamline tracking of debits and credits. Source: Created by the author.

## The Challenge: Interpreting Negative Numbers

While this system is efficient, interpreting **negative values** in real-life contexts can feel **counterintuitive**. For example, in the table above, Lending Co.’s cash account shows **-$120**, even though the company clearly has **$120** in the bank. _What does this negative value mean?_ It seems weird to think of cash amount in the bank as “negative.” This disconnect can make the concept feel awkward at first glance.

## Understanding Debits and Credits as Vectors

Here’s a helpful way to conceptualize this: debits and credits are not just scalar numbers — they’re **vectors** **with** **both magnitude (the dollar amount) and direction (debit or credit)**.

For consistency, I assign debits to the negative direction and credits to the positive direction. However, this is arbitrary — you can choose the reverse, as long as you apply it consistently throughout your system.

In this framework, **-$120** doesn’t mean a loss. It simply indicates _$120 in the debit direction_. It’s a directional marker, not a value judgment.

## Converting for User-Friendly Display

Okay, but here’s the practical question: how do you show this to end users who don’t want to think about vectors or directions? Well — you adjust the sign based on the account type:

### For Debit Normal Accounts (e.g., cash):

- If `amount_signed = -$120`:

Adjust the sign: `amount_signed * (-1) = $120`

Display: _"You have $120 in the bank!"_
- If `amount_signed = $120`:

Adjust the sign: `amount_signed * (-1) = -$120`

Display: _"Oops, your bank account is overdrawn by $120!"_

### For Credit Normal Accounts (e.g., loans):

- If `amount_signed = -$120`:

Adjust the sign: `amount_signed * (+1) = -$120`

Display: _"You overpaid $120 for your credit card!"_
- If `amount_signed = $120`:

Adjust the sign: `amount_signed * (+1) = $120`

Display: _"You still owe $120 on your credit card!"_

### Why This Works

By viewing debits and credits as vectors, this approach simplifies calculations while maintaining accuracy. Each transaction balances to zero, with signs adjusted to provide clear and intuitive displays for end users.

## Get Jian Ruan’s stories in your inbox

Join Medium for free to get updates from this writer.

Subscribe

Subscribe

Remember me for faster sign in

At first, seeing something like **-$120** in a cash account may feel odd, but once you grasp the concept of accounting directions, it becomes intuitive. Plus, no more tedious mental gymnastics trying to figure out when to increase or decrease balances!

## Visualizing Debit and Credit as Vectors: The Lending Co. Story

To make accounting concepts more intuitive, let’s visualize how debits and credits behave as vectors within Lending Co.’s example. In the following visualizations, each account is depicted as a plane, where the **vertical axis** indicates **direction** — **credit (+)** or **debit (-)** — and the **horizontal axis** represents **time**. Every entry is a vector, combining **magnitude** (the amount) with **direction** (credit or debit).

### Transaction \#1: Alice Invests $10,000

Alice invests $10,000 into Lending Co., expecting a return on her investment.

- **Lending Cash (Debit Normal Account):**

A debit vector of $10,000 moves downward, representing an increase in Lending Co.’s cash.
- **Investor Principal (Credit Normal Account):**

A credit vector of $10,000 moves upward, reflecting the liability Lending Co. owes to Alice.

Press enter or click to view image in full size

![](https://miro.medium.com/v2/resize:fit:700/1*hTsuy-BZiZt6O8DBrxbXvQ.png)

Transaction #1 vector visualization. Source: Created by the author.

### Transaction \#2: Bob Borrows $5,000

Bob then takes out a loan for **$5,000**.

- **Lending Cash (Debit Normal Account):**

A credit vector of $5,000 moves upward, decreasing Lending Co.’s cash balance.
- **Borrower Principal (Debit Normal Account):**

A debit vector of $5,000 moves downward, recording the loan balance Bob owes to Lending Co.

Press enter or click to view image in full size

![](https://miro.medium.com/v2/resize:fit:700/1*mQCd0GK9sX6V2WUFCVLSQQ.png)

Transaction #2 vector visualization. Source: Created by the author.

### Transaction \#3: Lending Co. Accrues Interest from Bob

Now, the loan starts generating income. Bob owes **$600** in interest, and Lending Co. keeps **$120** of that as a fee and paying the rest **$480** interest to investor Alice.

- **Borrower Interest (Debit Normal Account):**

A debit vector of $600 moves downward, reflecting the interest Bob owes to Lending Co.
- **Investor Interest (Credit Normal Account):**

A credit vector of $480 moves upward, representing the portion of interest Lending Co. owes to Alice.
- **Lending Fee Revenue (Credit Normal Account):**

A credit vector of $120 moves upward, representing Lending Co.’s earnings from the transaction.

Press enter or click to view image in full size

![](https://miro.medium.com/v2/resize:fit:700/1*gSyizXG7Eo1NXeZoJC8S0A.png)

Transaction #3 vector visualization. Source: Created by the author.

### Transaction \#4: Bob Pays Back Principal + Interest

Bob repays the $5,000 principal and $600 interest, clearing his obligations.

- **Lending Cash (Debit Normal Account):**

A debit vector of $5,600 moves downward, reflecting the payment Lending Co. receives.
- **Borrower Principal (Debit Normal Account):**

A credit vector of $5,000 moves upward, clearing Bob’s loan balance.
- **Borrower Interest (Debit Normal Account):**

A credit vector of $600 moves upward, clearing Bob’s interest obligations.

Press enter or click to view image in full size

![](https://miro.medium.com/v2/resize:fit:700/1*KtKvDtwxCiIJxwE5jWV2Zw.png)

Transaction #4 vector visualization. Source: Created by the author.

### Transaction \#5: Lending Co. Pays Back Alice

Finally, Lending Co. pays Alice her $10,000 principal and $480 in interest.

- **Lending Cash (Debit Normal Account):**

A credit vector of $10,480 moves upward, decreasing Lending Co.’s cash balance.
- **Investor Principal (Credit Normal Account):**

A debit vector of $10,000 moves downward, reflecting the repayment of Alice’s investment.
- **Investor Interest (Credit Normal Account):**

A debit vector of $480 moves downward, representing the payment of interest to Alice.

Press enter or click to view image in full size

![](https://miro.medium.com/v2/resize:fit:700/1*eOMl0DuB2qYI99WnyuYNBw.png)

Transaction #5 vector visualization. Source: Created by the author.

After completing all the transactions, Lending Co. has **$120** remaining in the bank, representing the revenue earned from lending fees.

It’s important to note that in this example, **Transaction #3: Lending Co. Accrues Interest from Bob** is recorded _before_ Bob repays Lending Co. This approach reflects the use of **accrual accounting** rather than **cash accounting**, where revenue and expenses are recognized when they are earned or incurred, not when cash is exchanged.

## Key Components of a Ledger

To better understand how the double-entry accounting principles are applied, let’s break down the key components of a ledger.

### **Ledger**

A ledger is an **immutable** record of all financial transactions, ensuring accuracy and transparency.

### Accounts

Accounts act as “buckets” of value to track specific balances, such as cash in the bank (a debit normal account) or investor principal (a credit normal account).

### Transactions

Transactions are financial events (e.g., loans, repayments) that generate **at least two entries** — a debit and a credit — to maintain zero-sum balance.

### **Entries**

Entries are the individual components of a transaction, categorized as either debit or credit.

### **Balances**

Each entry directly impacts the balance of the associated account.

Press enter or click to view image in full size

![](https://miro.medium.com/v2/resize:fit:700/1*KTLo7KOOb64YBJTsubfW7A.png)

Key components of a ledger. Source: Created by the author.

Press enter or click to view image in full size

![](https://miro.medium.com/v2/resize:fit:700/1*aSl90bIfmtNT6tN7YOBkvw.png)

Relationship between key components of a ledger. Source: Created by the author.

## How Leading Tech Companies Approach Ledger Design

To gain a deeper understanding of ledger system design, I delved into technical blogs from companies like Uber, Square, Airbnb, and Stripe — leaders in moving money at scale. While their ledger implementations differ, they all adhere to the fundamental principles of double-entry accounting. Here are the key takeaways:

**Uber (Gulfstream Payment Platform): Scalability \[1\]**

Uber handles millions of transactions daily across business lines such as ridesharing and food delivery. What stands out is their ability to operate two ledger systems simultaneously — a legacy system and a modern one — during migration, ensuring data consistency without downtime.

**Square (“Books” Ledger): Immutability \[2\]**

Square transitioned from single-entry to double-entry accounting to enhance accuracy and scalability — a move that enables greater product flexibility, such as offering SMB loans. (It’s hard to imagine how Square’s support team troubleshot money issues in the early days without double-entry accounting!)

**Airbnb (Event-Based Ledger): Decoupling \[3\]**

Airbnb transitioned from a MySQL-based data pipeline to an event-driven approach using Apache Spark, significantly speeding up financial reporting. They decoupled financial logic from product logic by separating platform events (product interactions) from payment events (money movement). Both types of events generate accounting entries that ultimately impact the ledger, creating a more efficient and scalable system.

**Stripe (Global “Ledger” System): Tracking and Tracing \[4\]**

As Stripe’s primary offering is its payment API, it must seamlessly handle billions of transactions at any given time. To achieve this, Stripe places a strong emphasis on tracking and tracing money movement. They use state-machine modeling to manage fund flows and have developed a data quality dashboard to ensure timely data, simplify troubleshooting, and uphold high data integrity.

## Conclusion

As a16z partner Angela Strange famously said, _“Every company will become a fintech company”_\[5\] _._ In today’s world, even businesses without prior financial expertise can leverage fintech and embedded finance to expand their offerings. However, at the core of any financial operation lies a robust ledger system, and building it correctly from the start is crucial.

For developers and founders working on ledger systems, here’s some key advice to keep in mind:

- **Prioritize Ledger Systems Early:** Plan and implement ledger systems from the start. While they might seem secondary initially, ignoring them can lead to complex technical and operational issues later.
- **Adopt Double-Entry Accounting from the Start:** Use double-entry accounting early to avoid the pain of migrating from single-entry systems. It ensures balanced transactions and establishes a solid foundation for growth.
- **Focus on Immutability:** Design your ledger as an immutable, single source of truth. This simplifies audits, increases data reliability, and ensures consistency as your business scales.

I hope this article has made the concepts of double-entry accounting clearer, especially through the lens of vectors — balancing magnitude and direction. A well-designed ledger is more than just a back-office necessity; it’s a foundation for building scalability, trust, and long-term growth.

Thank you for reading, and happy ledgering!

### **Reference**

\[1\] Uber, Revolutionizing Money Movements at Scale with Strong Data Consistency, [https://www.uber.com/blog/money-scale-strong-data/](https://www.uber.com/blog/money-scale-strong-data/)

\[2\] Square, Books: An Immutable Double-Entry Accounting Database Service, [https://developer.squareup.com/blog/books-an-immutable-double-entry-accounting-database-service/](https://developer.squareup.com/blog/books-an-immutable-double-entry-accounting-database-service/)

\[3\] Airbnb, Tracking the Money — Scaling Financial Reporting at Airbnb, [https://medium.com/airbnb-engineering/tracking-the-money-scaling-financial-reporting-at-airbnb-6d742b80f040](https://medium.com/airbnb-engineering/tracking-the-money-scaling-financial-reporting-at-airbnb-6d742b80f040)

\[4\] Stripe, Ledger: Stripe’s System for Tracking and Validating Money Movement, [https://stripe.com/blog/ledger-stripe-system-for-tracking-and-validating-money-movement](https://stripe.com/blog/ledger-stripe-system-for-tracking-and-validating-money-movement)

\[5\] A16z, Every Company Will Become a Fintech Company, [https://a16z.com/every-company-will-be-a-fintech-company/](https://a16z.com/every-company-will-be-a-fintech-company/)

### Additional Resources

\[6\] Hacker News, Discussion on Double Entry Accounting for Developers, [https://news.ycombinator.com/item?id=23964513](https://news.ycombinator.com/item?id=23964513)

\[7\] Anvil, Double Entry Accounting for Engineers, [https://anvil.works/blog/double-entry-accounting-for-engineers](https://anvil.works/blog/double-entry-accounting-for-engineers)

\[8\] Beancount Documentation, Double-Entry Bookkeeping, [https://beancount.github.io/docs/the\_double\_entry\_counting\_method.html#double-entry-bookkeeping](https://beancount.github.io/docs/the_double_entry_counting_method.html#double-entry-bookkeeping)

\[9\] Ledger-CLI, Double-Entry Accounting Software, [https://ledger-cli.org/](https://ledger-cli.org/)

Fintech

Ledger

Accounting

System Design Concepts

Database

[![Level Up Coding](https://miro.medium.com/v2/resize:fill:96:96/1*5D9oYBd58pyjMkV_5-zXXQ.jpeg)](https://levelup.gitconnected.com/?source=post_page---post_publication_info--79804045abb0---------------------------------------)

[![Level Up Coding](https://miro.medium.com/v2/resize:fill:128:128/1*5D9oYBd58pyjMkV_5-zXXQ.jpeg)](https://levelup.gitconnected.com/?source=post_page---post_publication_info--79804045abb0---------------------------------------)

Follow

[**Published in Level Up Coding**](https://levelup.gitconnected.com/?source=post_page---post_publication_info--79804045abb0---------------------------------------)

[334K followers](https://levelup.gitconnected.com/followers?source=post_page---post_publication_info--79804045abb0---------------------------------------)

· [Last published 11 hours ago](https://levelup.gitconnected.com/building-a-week-long-running-agentic-system-2ad79f8190bb?source=post_page---post_publication_info--79804045abb0---------------------------------------)

Coding tutorials and news. The developer homepage [gitconnected.com](http://gitconnected.com/) && [skilled.dev](http://skilled.dev/) && [levelup.dev](http://levelup.dev/)

Follow

[![Jian Ruan](https://miro.medium.com/v2/resize:fill:96:96/1*MpCbd_hjEQ4R9PDYJ2CvzQ.jpeg)](https://medium.com/@jianruan18?source=post_page---post_author_info--79804045abb0---------------------------------------)

[![Jian Ruan](https://miro.medium.com/v2/resize:fill:128:128/1*MpCbd_hjEQ4R9PDYJ2CvzQ.jpeg)](https://medium.com/@jianruan18?source=post_page---post_author_info--79804045abb0---------------------------------------)

Follow

[**Written by Jian Ruan**](https://medium.com/@jianruan18?source=post_page---post_author_info--79804045abb0---------------------------------------)

[19 followers](https://medium.com/@jianruan18/followers?source=post_page---post_author_info--79804045abb0---------------------------------------)

· [39 following](https://medium.com/@jianruan18/following?source=post_page---post_author_info--79804045abb0---------------------------------------)

CTO & Co-founder @ Orbbit \| Writing on AI / ML / Data & Startups \| Building smarter fintech solutions

Follow

[Help](https://help.medium.com/hc/en-us?source=post_page-----79804045abb0---------------------------------------)

[Status](https://status.medium.com/?source=post_page-----79804045abb0---------------------------------------)

[About](https://medium.com/about?autoplay=1&source=post_page-----79804045abb0---------------------------------------)

[Careers](https://medium.com/jobs-at-medium/work-at-medium-959d1a85284e?source=post_page-----79804045abb0---------------------------------------)

[Press](mailto:pressinquiries@medium.com)

[Blog](https://blog.medium.com/?source=post_page-----79804045abb0---------------------------------------)

[Store](https://medium.com/store)

[Privacy](https://policy.medium.com/medium-privacy-policy-f03bf92035c9?source=post_page-----79804045abb0---------------------------------------)

[Rules](https://policy.medium.com/medium-rules-30e5502c4eb4?source=post_page-----79804045abb0---------------------------------------)

[Terms](https://policy.medium.com/medium-terms-of-service-9db0094a1e0f?source=post_page-----79804045abb0---------------------------------------)

[Text to speech](https://speechify.com/medium?source=post_page-----79804045abb0---------------------------------------)

reCAPTCHA

Recaptcha requires verification.

protected by **reCAPTCHA**