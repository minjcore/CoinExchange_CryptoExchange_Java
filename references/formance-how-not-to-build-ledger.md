[Skip to content](https://www.formance.com/blog/engineering/how-not-to-build-a-ledger#main-content)

[Term of the day\\
Idempotency](https://www.formance.com/glossary/idempotency)

![How Not to Build a Ledger](https://formance01.b-cdn.net/blog/how-not-to-build-a-ledger.png)

So you’re an engineer, and you’ve been tasked with building a ledger to record funds movements across your platform. Building a robust, reliable ledgering system might not seem like a difficult engineering challenge, but a good understanding of accounting practices can help you avoid certain hidden traps that could derail your accounting.

In this article, we want to walk through some common anti-patterns in ledger design we’ve seen, point out those hidden traps, and arm you with the accounting basics you need to avoid those traps. You’ll be implementing more robust, reliable, and auditable ledgering systems in no time.

# The Scenario

Let’s suppose we’re building an online marketplace for people to buy and sell their second-hand goods. Sellers can list an item for sale for free, and we take a 10% commission from the sale price when someone else buys it.

Suppose moreover that Bob has a guitar listed for $100, and Alice—our first customer!—wants to buy it. She will transfer $100 to us through a payment processor, and once she confirms receipt of the guitar, we will release $90 to Bob and take our $10 commission. How should we represent these transactions internally?

# Zero-Entry Ledgers

Perhaps one of the most common ways to go about managing transactions is to have a table with account balances for each user. Here is an example of what such a table might look like. At the very start, all accounts will have a zero balance.

| Account | Balance |
| --- | --- |
| Alice | $0 |
| Bob | $0 |
| Commission | $0 |

For Alice to buy Bob’s guitar, she first needs to make a payment to the platform for the total sale amount. We will update her account balance once we receive confirmation from the payment processor:

| Account | Balance |
| --- | --- |
| Alice | $100 |
| Bob | $0 |
| Commission | $0 |

Two days later, Alice confirms receipt of the guitar, and we can release the funds to the seller, plus our commission. To record the result of the sale, we update Alice’s account balance to $0, Bob’s account balance to $90, and increase commission account balance by $10.

| Account | Balance |
| --- | --- |
| Alice | $0 |
| Bob | $90 |
| Commission | $10 |

One of the reasons this model is so commonly deployed is that it is simple and straightforward—there is never any question of what each account balance is at any given time, it’s right there in the table!

## Yes, But…

This is called zero-entry ledgering because we do not record the details of individual transactions, only the final results. It’s easy, but…this accounting model is deeply flawed, and represents a significant business risk.

Why? Because the balances are _mutable_. There is nothing in the design of this ledger to prevent a bug in your code (or a malicious actor, for that matter) from simply updating an account balance incorrectly.

Suppose for example, there was a bug in the commission calculation, and it was calculating a 10% commission when updating the seller’s account balance, but a 12% when updating the commission account balance, so we end up with final balances after the guitar sale that like this:

| Account | Balance |
| --- | --- |
| Alice | $0 |
| Bob | $90 |
| Commission | $12 |

There is an extra $2 in the system now, which we won’t detect until we reconcile the balance with what’s in our actual bank account. And once we do find that we have an extra $2 in our ledger, we are utterly unable to answer important questions like: Where did this $2 come from? Which account or accounts is it in? How did it get there? Was it a mistake, or is this fraud?

Basically, with a zero-entry ledger, we’re flying blind. We cannot detect drift in our ledger before reconciliation. And we simply aren’t storing enough information about our transactions to understand how to correct any drift. This is a very risky situation we’ve put ourselves in.

But we can solve a lot of these problems by recording individual transactions rather than account balances in our ledger. Let’s see how this might work.

# Single-Entry Ledgers

The next logical step many developers will take is to upgrade the system to a single-entry ledger, a ledger that stores information about individual transactions.

Instead of account balances, a single-entry ledger records information about individual transactions into and out of an account. Account balances are computed on the basis of all the transactions involving that account.

If you’ve ever had to balance a checkbook, or done household budgeting, these are examples of single-entry ledgers. Most small businesses—especially those that operate on a cash-only basis—use single-entry ledgers to manage their accounting.

Let’s retrace the example of Alice buying a guitar from Bob, and see how we would record that in a single-entry ledger.

| Account | Amount | Date | Balance |
| --- | --- | --- | --- |
| Alice | +$100 | 20 Feb 2025 | $100 |
| Alice | -$100 | 22 Feb 2025 | $0 |
| Bob | +$90 | 22 Feb 2025 | $90 |
| Commissions | +$10 | 22 Feb 2025 | $10 |
| BALANCE | $100 |  |  |

Now we have some more information. We can see that Alice initiated the sale on 20 February, when we charged her card for $100. Then, Alice verified receipt of the guitar on 22 February, at which point we released the funds from her account, and [credited](https://www.formance.com/blog/engineering/debits-and-credits-for-the-befuddled) Bob’s account $90, and our commissions account $10.

Moreover, we can see the flow of money reasonably clearly. Unlike a zero-entry ledger, a single-entry ledger is _immutable_. Adding a new transaction creates a new line, rather than overwriting existing data. This allows us to attach timestamps to each transaction, so we can see how the ledger has evolved over time. This allows us to do some rudimentary tracing of funds, which is important for detecting and correcting drift in the ledger when we do reconciliation.

## Not Out of the Woods

Although we are recording additional information that allows us to mitigate the worst problems of zero-entry ledgering, single-entry ledger still has some significant limitations. In particular, as our source of truth about the transactions that pass through our system, it is still susceptible to errors in entry that cannot be detected until reconciliation.

Let’s return to the example above where we mistakenly calculate our commission at 12%. After reconciliation, we can spot that the excess $2 is in the Commissions account, because we recorded that transaction. This can still happen because there are no constraints on how we record our transactions. And, real world ledgers tend to be a bit messier, and it won’t always be so easy to find once you have millions of accounts transacting with each other.

Moreover, we still don’t have a way to detect this error until we’ve done reconciliation. It would be nice if we could detect (and correct) it earlier. We need more data still.

| Account | Amount | Date | Balance |
| --- | --- | --- | --- |
| Alice | +$100 | 20 Feb 2025 | $100 |
| Alice | -$100 | 22 Feb 2025 | $0 |
| Bob | +$90 | 22 Feb 2025 | $90 |
| Commissions | +$12 | 22 Feb 2025 | $12 |
| BALANCE | $102 |  |  |

There is another question we cannot answer, one that our finance people very much need to be able to answer: Which balances are assets, and which are liabilities? We’re holding $100 in the bank—that’s an asset. But we owe Bob $90 (and the Commissions account $10)—these are liabilities. But this isn’t encoded in any way in our ledger, so our accountants cannot generate balance sheets for our business, and they cannot tell us whether our books are out of balance. We might appear to be flush with cash, when in fact we’re operating in the red.

# Double-Entry Ledgers

Double-entry ledgers take the idea of recording transaction individually, but add a critical constraint that must be applied to each transaction as recorded: A change on one account must always be balanced by an opposite change on another account. This constraint ensures that we will never record a transaction that makes money appear out of or disappear into thin air, unlike the simpler models discussed above.

To do this, we’ll add two details to our ledger. First, we split the “Amount” column into “Debit” and “Credit” columns. Second, we label each account as being an asset or a liability. Any account that is holding money for someone else, like our customers, is a liability. Any account that holds revenue, like our commissions account, is also considered a liability (at least until we’ve paid out the vendors). Any account that represents actual cash in our hands is an asset.

Next, we need to note two important rules for recording transactions.

The first rule is that:

`Assets = Liabilities`

Put slightly differently, every transaction must contain one or more debits, one or more credits, and the total of those debits must equal the total of those credits.

This ensures that after each transaction has been recorded, when we sum the assets and sum the liabilities, those will be equal to each other. This is how we ensure that no errors have been made when recording a transaction.

[The second rule is that](https://www.formance.com/blog/engineering/debits-and-credits-for-the-befuddled):

- A _debit_ increases an asset, or decreases a liability.
- A _credit_ decreases an asset, or increases a liability.

This rule tells us how to record each transaction properly, and how to properly sum assets and liabilities.

Finally, a little additional terminology. An account that represents an asset is called “debit normal”, because debits increase the value of the account. An account that represents a liability is called “credit normal”, because credits increase the value of the account. So, let’s note that the Payments account is our only debit-normal account; the remainder are credit-normal.

Let’s put these ideas to work!

| Account | Debit | Credit | Transaction | Date |
| --- | --- | --- | --- | --- |
| Bank (Debit) | $100 |  | 0001 | 20 Feb 2025 |
| Alice (Credit) |  | $100 | 0001 | 20 Feb 2025 |

On 20 February, Alice makes a card payment to us in the amount of $100. But we can’t just record that Alice’s balance has increased by $100—we must balance credits and debits! So we introduce a new account, our bank account, where that money has been deposited. And because this is cold, hard cash in our bank account, it is thus an asset. That said, the money belongs to Alice, it’s not our money. So we record her credit card payment as a debit to the Bank account (an increase in $100 in assets), and a credit to Alice (an increase in $100 in liabilities—because we owe this money to Alice).

So far so good! Our debits ($100) equal our credits ($100).

Next, Alice receives the guitar, and we release the funds to Bob (and take our commission). Because we want to remove money from Alice’s account, which is credit-normal, we mark the $100 payment as a debit on her account. We deposit $90 into Bob’s account, which since it is also credit-normal, is marked as a credit. Likewise, our $10 commission is marked as a credit in the Commission account, because it is also credit-normal.

| Account | Debit | Credit | Transaction | Date |
| --- | --- | --- | --- | --- |
| Bank (Debit) | $100 |  | 0001 | 20 Feb 2025 |
| Alice (Credit) |  | $100 | 0001 | 20 Feb 2025 |
| Alice (Credit) | $100 |  | 0002 | 22 Feb 2025 |
| Bob (Credit) |  | $90 | 0002 | 22 Feb 2025 |
| Commissions (Credit) |  | $10 | 0002 | 22 Feb 2025 |

In this transaction, we debited $100 from Alice’s account, and we credited $90 to Bob’s account and $10 to the Commissions account. Our debits ($100) equal our credits ($100) for this transaction, so it is valid.

Moreover, we can see after these two transactions that our assets equal our liabilities. After the first transaction our assets were $100, and our liabilities were also $100. In the second transaction, the debit to Alice’s account adds $100 to our assets (because we no longer owe her that money, as she no longer owns it, having exchanged that money for the guitar). The credits to Bob’s account and the Commissions accounts are a liability, so we add $90 and $10 to our liabilities.

Now: Assets = $100 + $100 = $200.

And: Liabilities = $100 + $90 + $10 = $200.

Our Assets remain the same as our liabilities, so we know everything is correct!

## What Have We Gained?

We can now answer a very broad range of important questions about the money movements recorded in our ledger. In particular, we can make strong guarantees that our ledger is free of drift resulting from the incorrect entry of transactions, because the constraints of double-entry ledgering make it is impossible to create or destroy money with an invalid transaction. Of course, drift can still happen if a transaction isn’t recorded, or is recorded more than once, but this isn’t a problem with the ledger itself.

Moreover, we can see both sides of a transaction very clearly, which makes funds traceability much easier. We know with certainty that the $90 in Bob’s account came from Alice’s account, because of how transactions are recorded. Even with millions of accounts transacting, there is never a question of where the money in each account came from and why.

Moreover, because we have a very clear picture of our assets and liabilities, our accountants can create balance sheets and other documents to show investors and regulators that everything is being accounted for.

# Conclusion

Creating a robust, scalable, auditable ledgering system requires more than sound engineering principles, it also requires a basic understanding of accounting principles as well.

Zero-entry ledgers are the easiest to implement, but offer no assurances against errors or fraud—it is trivially easy to create or destroy money when writing an account balance. They also offer no information about how accounts change over time, making it impossible to trace the movements of funds.

Single-entry ledgers are only slightly more complex to implement, as you are recording a chain of individual account balance changes over time. This historical data makes it somewhat easier to spot errors and fraud, and offers a rudimentary possibility for tracing the movements of funds.

Double-entry ledgers are the gold standard for recording transactions. The constrained structure of entries offers built-in assurance that each transaction is being recorded correctly, and makes it clear where the money is coming from and where it is headed. The possibility of errors and fraud hiding in the ledger is eliminated from the ledger (though obviously there are plenty of other ways for this to happen!), funds traceability is greatly increased. And our accountants can now produce the documents they need to reassure investors and regulators that our business is operating properly.

## Related Articles

[![Breaking Down Stablecoin Regulations Under the GENIUS Act and MiCA](https://formance01.b-cdn.net/blog/stablecoin-regulation.png)\\
\\
August 27, 2025\\
\\
**Breaking Down Stablecoin Regulations Under the GENIUS Act and MiCA** \\
\\
Learn how to prepare to meet new regulatory obligations headed your way.\\
\\
StablecoinsE-MoneyRegulation](https://www.formance.com/blog/financial-operations/stablecoin-regulation) [![How Not to Build a Ledger](https://formance01.b-cdn.net/blog/how-not-to-build-a-ledger-2.png)\\
\\
December 16, 2025\\
\\
**How Not to Build a Ledger** \\
\\
Part 2: Race Conditions, Throughput Cliffs, and the Hidden Risks of Financial Software\\
\\
LedgerGuide](https://www.formance.com/blog/engineering/how-not-to-build-a-ledger-2) [![Ensuring Ledger Integrity](https://formance01.b-cdn.net/blog/ensuring-ledger-integrity.png)\\
\\
December 2, 2025\\
\\
**Ensuring Ledger Integrity** \\
\\
The Non-Negotiables Every NBFI Must Enforce\\
\\
LedgerRegulation](https://www.formance.com/blog/financial-operations/ensuring-ledger-integrity) [![The Fiat-to-Digital Asset Playbook](https://formance01.b-cdn.net/blog/the-fiat-to-digital-asset-playbook.png)\\
\\
January 27, 2026\\
\\
**The Fiat-to-Digital Asset Playbook** \\
\\
A Practical Guide to Solving the Settlement Gap Between Banks and Blockchains\\
\\
LedgerReconciliationStablecoins](https://www.formance.com/blog/financial-operations/the-fiat-to-digital-asset-playbook) [![That Time a $400 Giveaway Became an $8M mistake](https://formance01.b-cdn.net/blog/that-time-a-400-giveaway-became-an-8m-mistake.png)\\
\\
February 24, 2026\\
\\
**That Time a $400 Giveaway Became an $8M mistake** \\
\\
What the Bithumb Incident Reveals About the Need for Double-Entry Core Ledgers\\
\\
LedgerAccountingStablecoins](https://www.formance.com/blog/industry-analysis/that-time-a-400-giveaway-became-an-8m-mistake) [![The Stablecoin Sandwich](https://formance01.b-cdn.net/blog/the-stablecoin-sandwich.png)\\
\\
March 3, 2026\\
\\
**The Stablecoin Sandwich** \\
\\
How to Track a Payment Across Four Systems That Don’t Talk to Each Other\\
\\
LedgerReconciliationStablecoins](https://www.formance.com/blog/financial-operations/the-stablecoin-sandwich) [![Build or Buy a Core Ledger](https://formance01.b-cdn.net/blog/build-or-buy-a-core-ledger.png)\\
\\
March 10, 2026\\
\\
**Build or Buy a Core Ledger** \\
\\
A ledger isn't your product, but getting it wrong affects everything.\\
\\
GuideLedger](https://www.formance.com/blog/engineering/build-or-buy-a-core-ledger)

Prismic Toolbar iFrame