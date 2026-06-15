[Sitemap](https://medium.com/sitemap/sitemap.xml)

[Open in app](https://play.google.com/store/apps/details?id=com.medium.reader&referrer=utm_source%3DmobileNavBar&source=post_page---top_nav_layout_nav-----------------------------------------)

Sign up

[Sign in](https://medium.com/m/signin?operation=login&redirect=https%3A%2F%2Fmedium.com%2Fslope-stories%2Fsolving-the-five-most-common-pitfalls-from-building-a-payments-ledger-0afe1a6eceae&source=post_page---top_nav_layout_nav-----------------------global_nav------------------)

[Medium Logo](https://medium.com/?source=post_page---top_nav_layout_nav-----------------------------------------)

Get app

[Write](https://medium.com/m/signin?operation=register&redirect=https%3A%2F%2Fmedium.com%2Fnew-story&source=---top_nav_layout_nav-----------------------new_post_topnav------------------)

[Search](https://medium.com/search?source=post_page---top_nav_layout_nav-----------------------------------------)

Sign up

[Sign in](https://medium.com/m/signin?operation=login&redirect=https%3A%2F%2Fmedium.com%2Fslope-stories%2Fsolving-the-five-most-common-pitfalls-from-building-a-payments-ledger-0afe1a6eceae&source=post_page---top_nav_layout_nav-----------------------global_nav------------------)

![Unknown user](https://miro.medium.com/v2/resize:fill:32:32/1*dmbNkD5D-u45r44go_cf0g.png)

[**Slope Stories**](https://medium.com/slope-stories?source=post_page---publication_nav-c0a8177fa770-0afe1a6eceae---------------------------------------)

·

Follow publication

[![Slope Stories](https://miro.medium.com/v2/resize:fill:38:38/1*4kJGB3WqlIznH94C_aoJVg.png)](https://medium.com/slope-stories?source=post_page---post_publication_sidebar-c0a8177fa770-0afe1a6eceae---------------------------------------)

The B2B Payments Platform

Follow publication

# Solving the five most common pitfalls from building a payments ledger

[![Alvin Sng](https://miro.medium.com/v2/resize:fill:32:32/1*DKKaRvQPRgwjREi6SEAF7Q.jpeg)](https://medium.com/@alvinsng?source=post_page---byline--0afe1a6eceae---------------------------------------)

[Alvin Sng](https://medium.com/@alvinsng?source=post_page---byline--0afe1a6eceae---------------------------------------)

Follow

12 min read

·

Nov 29, 2023

200

1

[Listen](https://medium.com/m/signin?actionUrl=https%3A%2F%2Fmedium.com%2Fplans%3Fdimension%3Dpost_audio_button%26postId%3D0afe1a6eceae&operation=register&redirect=https%3A%2F%2Fmedium.com%2Fslope-stories%2Fsolving-the-five-most-common-pitfalls-from-building-a-payments-ledger-0afe1a6eceae&source=---header_actions--0afe1a6eceae---------------------post_audio_button------------------)

Share

Here at Slope, we're always exploring new ways to innovate in the B2B payments space, and a core component requires having a robust payments foundation that accurately tracks and handles all our money movements. Over the past year, we've built our own payment transactions ledger in-house and have addressed common challenges that any fintech may encounter. This post describes the top 5 most common pitfalls when building a ledger and how we solved them.

## Why build a ledger?

Before delving into the details of our ledger design, we should first understand why one would need a double-entry ledger at all and what problems it is solving.

The most common payment problems that stem from lacking a ledger are:

1. **Lack of a single, centralized schema for the history of payments:** Imagine you're a hotel booking site, but you store payments from customers in one table and refunds and surcharges in a different table with different schemas. To see the full picture, you'll need to join all the tables to know the balance.
2. **Not tracking money lost from refunds:** It is common to have a separate table storing all the special credits issued back to unhappy customers. When you're only noting one side of the money movement, you're not tracking where those funds are coming from and the totals of each group of losses.
3. **Updating the totals of an order when refunds are issued:** This leads to unstable reports as your numbers are shifting. You also lose clarity on the full history of the amount changes.
4. **Irreconcilable balances:** Irreconcilable balances occur when money movements are not standardized or are not deducted from an opposite party. In such cases, you won’t have balances that can be reconciled with external sources, such as a bank account or payments gateway.

To counter some of these problems, each different payment table begins to store its own ledger-like history of changes, which starts becoming a "second source of truth." This turns ugly quite quickly when the rows don't stay in sync. Over time, the problems grow exponentially as the business deals with more nuisance edge cases.

## Benefits of a double entry ledger

At the core of many payments processing fintech is a double-entry transactions ledger. The basic concepts of how a ledger works are quite simple and have been battle-tested throughout history long before computers were around. We see ledgers all around us, from credit card statements to our utility bills. Fundamentally, it answers two important questions:

1. How much money was moved, when, and where?
2. What’s the balance of an account at any given point in time?

The “double entry” part means that all transactions must specify both the from and to accounts. This ensures all money must be moved somewhere and can’t be produced or removed from thin air. Debiting one account must credit a different account the same amount, which automatically affects the balance. This is because the balance is computed by summing all transactions and not stored.

As for the “any point in time”, this is solved by having an immutable design. This means we only ever insert new rows into our ledger and never update or delete entries. If a mistake needs to be reverted, you would need to insert a new entry that contains the opposite amounts. The immutability part allows for:

1. Being able to construct historical financial reports showing the balances at any previous time.
2. Allowing for a clear audit trail of when and where money movement was performed.
3. Enabling us to have clear cutoff points for transactions. This is necessary when slicing monthly statements or exporting to accounting systems for month-end closing of books.

## A basic ledger design

To better understand how a basic ledger works, let’s look at a simple relational database schema that consists of three tables: `ledger_accounts`, `ledger_transactions`, and `ledger_entries`

![](https://miro.medium.com/v2/resize:fit:571/1*PxC-NJ_GyPGsVJbxGxtIKA.png)

A basic ledger database schema

```
Table ledger_accounts {
  id varchar [primary key]
  name varchar
  type varchar
}

Table ledger_transactions {
  id varchar [primary key]
  posted_at timestamp
}

Table ledger_entries {
  id varchar [primary key]
  ledger_account_id varchar
  ledger_transaction_id varchar
  amount_signed integer
}

Ref: ledger_accounts.id < ledger_entries.ledger_account_id

Ref: ledger_transactions.id < ledger_entries.ledger_transaction_id
```

With the above schema, we can answer foundational questions, such as knowing the current balance, with a simple SQL query:

```
SELECT SUM(amount_signed) FROM ledger_entry WHERE ledger_account_id={$1}
```

### Alternative schema design: dropping the `ledger_transactions` table

Depending on your use case, one could omit the entire `ledger_transactions` table and instead add a `transaction_id`and `posted_at` column to the `ledger_entry` . The `transaction_id` can be any unique key your system generates. Generally speaking, most products will leverage a `ledger_transactions` table which provides a place to store shared product-level metadata about the transaction.

## Common pitfalls and how we solved them

### 1\. Inconsistent directions for debits & credits

The first problem you may encounter is how to consistently represent additions and subtractions to an account’s balance throughout the entire product journey. This is simple when you’re only dealing with a single opposite party in a two-sided marketplace, i.e., You’re the merchant, and you only have customers. However, when you want to track money movement between multiple parties, i.e., you’re a platform that accepts incoming payments and distributes funds to other parties similar to that of Ebay, Venmo, or Airbnb.

Our solution is to follow [Generally Accepted Accounting Principles (GAAP)](https://en.wikipedia.org/wiki/Generally_Accepted_Accounting_Principles_(United_States)), which is how an accountant would count debits vs credits for an account’s entry. This also means all accounts are viewed from the perspective of Slope being the owner. When we say an account is a liability, it means the opposite party’s asset, and when extending a loan to a customer, it is counted as an asset for us but would be a liability viewed from the customer’s perspective.

Press enter or click to view image in full size

![](https://miro.medium.com/v2/resize:fit:700/1*6oBTMpr0whgTCjLq1IDEHA.png)

Flow of funds between different ledger accounts

Diagram showing flow of funds between different ledger accounts

By following a consistent formula for counting the direction of a ledger entry, we’re able to move funds between any other ledger account without resorting to confusing hacks like flipping directions depending on the party you’re facing. With our design, all internal ledger accounts will eventually balance to zero, whereas external ledger accounts will grow indefinitely.

Our original method of solving the storage of directions was to use a `direction` column, which was an enum of `credit`or `debit`, and an `amount` column, which was always a positive number. However, this approach was subpar and had two problems:

1. Performing a range filter between 0 was complex. If we wanted to filter and sort transactions between -$5 and $10, we would need to filter by both debits and credits, plus perform two different orderings of sorting it, then combining the results.
2. Various callers, including downstream ETLs, would always need to write longer queries to compute the balance using `SUM ( CASE ())` statements to check `debit` vs `credit`.

The simple solution was to drop the `direction` column and instead use a signed integer column called `amount_signed`, where negative meant `debit`, and positive meant `credit`.

## Get Alvin Sng’s stories in your inbox

Join Medium for free to get updates from this writer.

Subscribe

Subscribe

Remember me for faster sign in

Within the ledger, whether account balances are positive vs. negative may not match how the user expects to see the number, which we often flip the sign when displayed on the UI. For example, a user expects to see a loan with a positive balance as a debt they owe, whereas internally it may be negative in the ledger.

### 2\. Lacking data integrity

Thankfully, by using a relational database like Postgres, we automatically get all the ACID properties out of the box. This is how we solved some of our “easy” problems:

1. **Preventing Duplicate Transactions:** Using idempotency keys and leveraging unique indexes to automatically throw errors if duplicates are created.
2. **Preventing Unbalanced Entries for a Transaction:** Using a DB transaction when inserting entries and adding a constraint check before commit to ensure the sums always equal 0.
3. **Preventing Overdrawing an Account:** Acquiring a pessimistic lock on the associated ledger accounts and only performing the entries inside the lock. Note: Depending on the product use case, it is valid for some accounts to go negative, i.e., a returned or disputed payment is triggered by the customer.

However, even with those checks in place, it is still possible to encounter integrity issues from engineering bugs, admin actions, or money being moved to the wrong accounts. To add an additional layer of protection, we run DBT tests. These are multiple SQL queries that each scan a particular type of problem and return a subset of rows that don’t add up. Below is an example DBT test that returns rows where the ledger amount does not match the orders table:

```
SELECT
    ledger_account_with_balance.*,
    orders.*,
FROM
    ledger_account_with_balance
    JOIN orders ON orders.id = ledger_account_with_balance.subtype
WHERE
    ledger_account_with_balance.type = 'order'
    AND ledger_account_with_balance.state = 'settled'
    AND orders.status = 'finalized'
    AND orders.amount_outstanding != ledger_account_with_balance.balance
```

Press enter or click to view image in full size

![](https://miro.medium.com/v2/resize:fit:700/1*Cy-ZlOKETwHF2X9z-ZtUoQ.png)

Our DBT tests dashboard showing failures

For example, one query is to return all loans with a negative balance, indicating a customer has overpaid their loan and is likely due for a refund. Another consistency check can match the amounts we’ve ingested from external payments providers with what we’ve inserted into our ledger.

### 3\. Omitting first-class support for pending transactions

Unfortunately, much of America’s payments processing still relies on the Automated Clearing House, or ACH, which dates back to the 1970s and is not real-time. Unless you’re only accepting credit cards, you’ll need to support functionality for pending payments directly in your ledger, or you’ll be exposed to issues like fraud or facing duplicate overpayments from customers. For example, these are problems you may encounter without supporting pending payments:

1. If you’ve initiated an ACH payment to a customer, you’ll want to immediately deduct their “available balance,” or else you may continue to allow multiple payments that exceed their limit.
2. If you’ve initiated an ACH pull from a customer, you’ll want to apply a pending credit to their account so you know not to double pull funds from them.

In both cases above, you’ll want to separately track the pending debit and pending credit for each account, which cannot be combined into a single pending account. Why? Let's say a customer has an order with a $1,000 settled balance. You’ve initiated a pending credit of $400, which would optimistically bring the settled balance to $1,400 if it succeeds. However, if you initiated a pending debit of $1,300, which would still even out the pending balance to be above $0 at $100, you’ll be out of luck when the first $400 payment fails!

To solve this, we automatically create 3 `ledger_accounts` for every real “account.” We use a column called `state`, which is an enum of `settled`, `pending:credit`, and `pending:debit`.

Below is a series of tables showing the above example where no restrictions are checked and how it would be stored in the ledger.

Press enter or click to view image in full size

![](https://miro.medium.com/v2/resize:fit:700/1*Ej-Zj89l8qU8X-X9M4cF0A.png)

By the end of Day 4, the Order account’s settled balance has gone from $1,000 to -$300 balance because there were no checks on the pending accounts’ balances.

To solve this case, we have an “available balance” figure for an order account, which would be SUM(order:settled) + SUM(order:pending-debit). We would ignore order:pending-credits as we don’t want to optimistically factor in pending funds coming in, but we want to pessimistically factor in pending debits and assume they have already occurred. The same could be done in either direction for any account depending on your product use case. Below is an illustration of the 3 total accounts showing a different scenario where there is an inflight pending debit and pending credit.

Press enter or click to view image in full size

![](https://miro.medium.com/v2/resize:fit:700/1*lgDReZj9zcYq-om-m4cVbA.png)

Visualizing pending amounts

### 4\. Mixing business logic in the foundational ledger

In a perfect world, our ledger contains no business logic and is the sole storer for payments data. However, there are some other downsides to account for:

1. This can be hard to accomplish when transitioning iteratively from a non-ledger-based payments schema as you’ll likely still have many existing dependencies that rely on the old schema.
2. There are also cases where relying solely on computing off the ledger is slow, and you need to denormalize values stored in additional places.
3. It will be hard for downstream product, data, or finance teams to understand how to query and work with ledger data. A data science or product team would have a much easier job if they had a simple `amount_outstanding`column on the `orders` table to quickly determine how much a customer owes on a given order rather than knowing which ledger tables to JOIN & SUM, which can quickly lead to confusion.

To better balance the needs of an accurate payments ledger and the needs of reducing business logic complexity for users, we instead apply a different set of rules on how we think about our ledger:

1. Our ledger is the single source of truth for payments data; however, we allow product tables to replicate denormalized values whenever a ledger entry is added. This provides a fast and simple column that all teams can use to power their products and workflows.
2. All ledger transactions must belong to and be initiated from a product table. We utilize an `entity_id` column on our `ledger_transaction` table, which is the foreign key association. This means to trigger any money movement, a product table row must be created first. For example, an order row must be created before issuing a debt on the order’s ledger account balance. For a payment to be received from our payment provider, a row must be created on a separate `external_payments` table, which may contain data about the credit card or any charge declined messages. This keeps all product metadata out of ledger tables while still ensuring we have a strict requirement that requires a pre-existing product table associated with all ledger transactions.

Press enter or click to view image in full size

![](https://miro.medium.com/v2/resize:fit:700/1*smgKquX1VkVuq6AhznZT6w.png)

The ledger is a central intersection for different providers to view or add money movement

Another way to view our ledger is as a central intersection where many disjoint product tables will meet at and insert their movement into a conformed spec then fan out in their ways afterward when changes occur. You can almost imagine it like an airport where various different modes of transportation will meet up and split off.

### 5\. Growing code complexity

Soon after piecing together all the code for a payments ledger, you’ll find the codebase growing in size and complexity, making it very difficult to follow or maintain, especially when adding support for pending transactions.

A solution to this is to keep the definition of ledger transaction movement in a simple format and have a framework that automates the complexity of pending transactions and the dynamic creation of ledger accounts on the fly. For example, below is our sample code in TypeScript for how we define the creation of ledger transactions, ledger entries, and ledger accounts all in one simple file:

```
export class OrderFinalizeLedgerTransaction extends BaseLedgerTransaction {
  order: OrderEntity

  public constructor(
    ledgerAccountModel: LedgerAccountModel,
    ledgerTransactionModel: LedgerTransactionModel,
    order: OrderEntity
  ) {
    super({
      ledgerAccountModel,
      ledgerTransactionModel,
      currency: order.currency,
      entityId: order.id,
      postedAt: order.finalizedAt
    })

    this.order = order
  }

  protected compute(): UnloadedCreateLedgerTransactionDto[] {
    const transactions = [\
      this.generateTransaction({\
        operation: LedgerTransactionOperation.ORDER_FINALIZE_PRINCIPAL,\
        amount: this.order.total,\
        debit: this.getOrderAccount(this.order.id),\
        credit: this.getMerchantWalletAccount(this.order.merchantId)\
      })\
    ]

    if (this.order.customerFee > 0) {
      transactions.push(
        this.generateTransaction({
          operation: LedgerTransactionOperation.ORDER_FINALIZE_CUSTOMER_FEE,
          amount: this.order.customerFee,
          debit: this.getOrderAccount(this.order.id),
          credit: this.getCustomerRevenueAccount()
        })
      )
    }
  }
}
```

Two callouts with what the code above does:

1. The functions `getOrderAccount` and `getMerchantWalletAccount` are helpers that simplify the lookup for the correct ledger account. That way, the caller does not need to know the account type (liability, asset, etc.) and the format of how to look it up. On top of that, in the event that the ledger account does not exist, it can also create one on the fly. An example of what the helper function looks like:

```
protected getOrderAccount(orderId: string): AccountSearchParams {
  return {
    type: LedgerAccountType.ORDER,
    subtype: orderId,
    state: LedgerAccountState.SETTLED,
    accountingType: LedgerAccountAccountingType.ASSET,
    accountingSubtype: LedgerAccountAccountingSubtype.ACCOUNTS_RECEIVABLE,
    normalBalance: LedgerDirection.DEBIT
  }
}
```

1. The generation of pending transactions is fully automated by simply looking at the definition of how settled money is moved and then deriving the pending logic. The caller passes in a `movement` parameter to specify whether it is moving settled, pending start, or pending failed. The shared parent class has code that looks like this:

```
public async run(
    movement: LedgerTransactionMovement = LedgerTransactionMovement.SETTLED
  ): Promise<CreateLedgerTransactionDto[]> {
    const settledTransactions = this.computeSettled()
    if (movement === LedgerTransactionMovement.SETTLED) {
      const reversePending = await this.derivePending(
        LedgerTransactionMovement.PENDING_REVERSE,
        settledTransactions
      )

			return settledTransactions.concat(reversePending)
    } else {
      return await this.derivePending(
        movement,
        settledTransactions
      )
    }
  }
```

Press enter or click to view image in full size

![](https://miro.medium.com/v2/resize:fit:700/1*-0nN3I9Y7ckE3ETetP-tTw.png)

How payment events trigger pending or settled money movement

## Final Takeaways

At Slope, our original ledger design has remained relatively unchanged for over a year, which shows how stable these foundational ledger concepts are. Even as new payment technologies emerge from RTP or FedNow, the same core concepts of a payment ledger do not need to change. Once you support moving money in both directions and a pending state, any payments application can be built on top of it.

Hopefully, you’ve found these various tips on how to overcome common challenges when building and designing a ledger to be helpful. You can read more about [our work at Slope](https://medium.com/slope-stories) or learn more about Slope at [https://slopepay.com/](https://slopepay.com/)

[Fintech](https://medium.com/tag/fintech?source=post_page-----0afe1a6eceae---------------------------------------)

[Technology](https://medium.com/tag/technology?source=post_page-----0afe1a6eceae---------------------------------------)

[AI](https://medium.com/tag/ai?source=post_page-----0afe1a6eceae---------------------------------------)

[Engineering](https://medium.com/tag/engineering?source=post_page-----0afe1a6eceae---------------------------------------)

[Finance](https://medium.com/tag/finance?source=post_page-----0afe1a6eceae---------------------------------------)

[![Slope Stories](https://miro.medium.com/v2/resize:fill:48:48/1*4kJGB3WqlIznH94C_aoJVg.png)](https://medium.com/slope-stories?source=post_page---post_publication_info--0afe1a6eceae---------------------------------------)

[![Slope Stories](https://miro.medium.com/v2/resize:fill:64:64/1*4kJGB3WqlIznH94C_aoJVg.png)](https://medium.com/slope-stories?source=post_page---post_publication_info--0afe1a6eceae---------------------------------------)

Follow

[**Published in Slope Stories**](https://medium.com/slope-stories?source=post_page---post_publication_info--0afe1a6eceae---------------------------------------)

[50 followers](https://medium.com/slope-stories/followers?source=post_page---post_publication_info--0afe1a6eceae---------------------------------------)

· [Last published Feb 27, 2025](https://medium.com/slope-stories/shipbob-capital-powered-by-slope-177d6b02e5f4?source=post_page---post_publication_info--0afe1a6eceae---------------------------------------)

The B2B Payments Platform

Follow

[![Alvin Sng](https://miro.medium.com/v2/resize:fill:48:48/1*DKKaRvQPRgwjREi6SEAF7Q.jpeg)](https://medium.com/@alvinsng?source=post_page---post_author_info--0afe1a6eceae---------------------------------------)

[![Alvin Sng](https://miro.medium.com/v2/resize:fill:64:64/1*DKKaRvQPRgwjREi6SEAF7Q.jpeg)](https://medium.com/@alvinsng?source=post_page---post_author_info--0afe1a6eceae---------------------------------------)

Follow

[**Written by Alvin Sng**](https://medium.com/@alvinsng?source=post_page---post_author_info--0afe1a6eceae---------------------------------------)

[103 followers](https://medium.com/@alvinsng/followers?source=post_page---post_author_info--0afe1a6eceae---------------------------------------)

· [99 following](https://medium.com/@alvinsng/following?source=post_page---post_author_info--0afe1a6eceae---------------------------------------)

MTS @ [Factory.ai](http://factory.ai/)

Follow

[Help](https://help.medium.com/hc/en-us?source=post_page-----0afe1a6eceae---------------------------------------)

[Status](https://status.medium.com/?source=post_page-----0afe1a6eceae---------------------------------------)

[About](https://medium.com/about?autoplay=1&source=post_page-----0afe1a6eceae---------------------------------------)

[Careers](https://medium.com/jobs-at-medium/work-at-medium-959d1a85284e?source=post_page-----0afe1a6eceae---------------------------------------)

[Press](mailto:pressinquiries@medium.com)

[Blog](https://blog.medium.com/?source=post_page-----0afe1a6eceae---------------------------------------)

[Store](https://medium.com/store)

[Privacy](https://policy.medium.com/medium-privacy-policy-f03bf92035c9?source=post_page-----0afe1a6eceae---------------------------------------)

[Rules](https://policy.medium.com/medium-rules-30e5502c4eb4?source=post_page-----0afe1a6eceae---------------------------------------)

[Terms](https://policy.medium.com/medium-terms-of-service-9db0094a1e0f?source=post_page-----0afe1a6eceae---------------------------------------)

[Text to speech](https://speechify.com/medium?source=post_page-----0afe1a6eceae---------------------------------------)

reCAPTCHA

Recaptcha requires verification.

protected by **reCAPTCHA**