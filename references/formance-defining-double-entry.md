[Skip to content](https://www.formance.com/blog/engineering/defining-double-entry#main-content)

[Term of the day\\
Idempotency](https://www.formance.com/glossary/idempotency)

![Defining Double Entry](https://formance01.b-cdn.net/blog/defining-double-entry.png)

A little while ago we discussed the [concept of a ledger](https://www.formance.com/blog/engineering/ledgering-all-the-way-up). And while there are many kinds of ledgers, in accounting we are mostly concerned with _double-entry_ ledgers. The term “double-entry” can be a little confusing, and conjures up a variety of ideas in people’s minds. So let’s take a moment to disentangle some misconceptions about what “double-entry” means, and then dive into getting clear on its true meaning by building a formal model of a double-entry ledger. The formalism we develop is going to prove immensely useful in understanding how to construct other kinds of account ledgers that focus on different tradeoffs and concerns.

# Ambiguous notions

When we talk to folks, they often ask if [Formance Ledger](https://www.formance.com/modules/ledger) is double-entry. It turns out they are often using the term as short-hand for one of the four following concerns, some of which aren’t actually related to the concept of double-entry accounting.

## Immutability

We do generally want our accounting ledgers to capture a stream of financial events, and to represent an auditable history of the movement of funds across accounts. That’s what an immutable ledger does—it is a log of events over time, and any corrections must be appended to the end (thus capturing that there was a need for an edit at a particular
time).

But you can have a mutable double-entry ledger. Probably not a good idea, but one could technically design a ledger that allows editing past entries. Likewise, you can have an immutable single-entry ledger—think of a logfile, for example. So while questions of immutability are a legitimate concern, that concept is unrelated to the concerns that double-entry accounting aims to solve.

## Data duplication

In the days that ledgers were handwritten into books, implementing a double-entry ledger required making two entries, in two different books. Moreover, keeping records in two places helped reduce casual accounting errors. And of course books are remarkably fragile things, and having a backup is always a good idea. But the idea that you have to write each movement twice is a side-effect of double-entry accounting, and not its goal.

In the computer age we can implement double-entry ledgers without literally making two writes. Moreover, computers eliminate the need to keep backups in the form of duplicate physical ledger entries, and guarding against typos is much less of a concern. So this doesn’t quite capture what’s important about double-entry accounting.

## Debits & credits

It might be that our curious questioner is wondering whether the ledger transactions are backed by some form of movement reciprocity model. In such models, you cannot make a change to one account without impacting a different account. And this concern is very close to the true meaning of “double-entry”—every transaction implies a movement of value _from_ one account _into_ another account. Hence the two entries: One to record the movement from, and one the movement into.

But the concept of [debits and credits is not only pretty confusing](https://www.formance.com/blog/engineering/debits-and-credits-for-the-befuddled), it’s also only one way to talk about reciprocal value movement. There are other, less confusing ways. Nevertheless, tracking debits and credits gets us close to the purpose of double-entry accounting, but there is more detail that we need to add for a complete picture.

## GAAP & IFRS

[GAAP](https://en.wikipedia.org/wiki/Generally_Accepted_Accounting_Principles_(United_States)) (Generally Accepted Accounting Practices) and [IFRS](https://en.wikipedia.org/wiki/International_Financial_Reporting_Standards) (International Financial Reporting Standards) are two sets of accounting standards that guide accountants in describing the financial position of a business in a way that ensures other accountants will understand them, as well as defining business rules that go beyond the pure mathematical structure of a ledger.

While these standards mandate the use of double-entry ledgers, they are better thought of as a set of extensions that describe how the accounts should be reported and understood, much like coding standards and linters for a programming language.

# Building Blocks of Accounting

To really get at what “double-entry” means, we are going to develop a formalism for describing accounting models. We’ll then use this formalism to construct a model for double-entry accounting. But first let’s think more generally about what an accounting model needs.

An accounting model comprises two entities: _accounts_, and _transactions_.

## Accounts

_Accounts_ have a structure, and a balance equation that determines how to calculate the contents of the account at any given time.

## Transactions

_Transactions_ also have a structure, and a set of constraints (such as the number of accounts involved in a transaction).

This definition is very abstract, so let’s build an accounting model for double-entry accounting to help understand how this formalism works.

# A Formal Model of Double-Entry Accounting

Let’s use these building blocks to developer a formal model of double-entry accounting, and to see why double-entry ledgers are not (just) the concepts at the top of this article.

## Accounts

First, let’s look at accounts. In a double-entry model, accounts are structured in three dimensions.

- First is the _position_ in the chart of account—that is, roughly, whether it represents assets or liabilities and what kind specifically.
- Second is the _normality_—it can be debit normal or credit normal, that is, it either increases in value as it is debited or as it is credited.
- Finally is the list of debits and credits that forms the core of the account.

Accounts also have a balance equation:

- ∑(credits)−∑(debits)∑(credits)-∑(debits)∑(credits)−∑(debits) for credit normal accounts, or
- ∑(debits)−∑(credits∑(debits)-∑(credits∑(debits)−∑(credits`)` for debit normal accounts

## Transactions

Then we have the transactions. They have a structure:

- A list of quantified debits and credits, related to the accounts they target

And a set of constraints:

- A transaction must contain at least one debit and one credit
- The sum of the debits must equal the sum of the credits
- At least two accounts are involved (this one can be considered debatable, some implementations allowing transactions to credit and debit a the same account)

Let’s look at an example to see how these parts fit together to form an accounting model.

Imagine that we have three accounts:

- **banks:main**, a debit-normal assets account representing a platform’s cash assets,
- **users:alice**, a credit-normal liability account representing a customer’s funds,
- **platform:fees**, a credit-normal liability account representing funds collected from users relative to fees.

Now, suppose that Alice wants to deposit 100 USD into her account, but that the business charges a 10% fee on such movements. The transaction would be structured as such:

```
DEBIT banks:main 100
CREDIT users:alice 90
CREDIT platform:fees 10
```

We can see that the constraints we described are all met:

1. There is at least one debit and one credit
2. The sum of the debits equals the sum of the credits
3. At least two accounts are involved.

But notice as well, we’re not writing this down twice. This is one transaction. What makes it “double-entry” is the satisfaction of those constraints. Supposing that the accounts were entirely empty prior to this transaction, if we apply the balance equations to each, we can
calculate the following balances:

- **banks:main** is a debit-normal account, so we subtract the credits (0 USD) from the debits (100 USD) to obtain balance of 100-0 = 100 USD.
- **users:alice** is a credit-normal account, so we subtract the debits (0 USD) from the credits (90 USD) to obtain its balance of 90-0 = 90 USD.
- And likewise for the credit-normal account **platform:fees**, its new balance is 10-0 = 10 USD.

As a consequence, we can see that across all accounts, the total debits (100 USD) is equal to the total credits (90+10). And that equilibrium ensures that we are working with a closed system, no value is being created out of thin air, and this equilibrium between assets and liabilities is the aim of double-entry accounting.

# Conclusion

A business’s finances are a closed system, it cannot make money appear or disappear at will. Double-entry accounting was designed to capture the idea that changing the contents of one account cannot be done without reciprocal, matching changes in other accounts. Here we have gone over some concepts that are commonly conflated with double-entry bookkeeping, and developed a formal model of double-entry accounting to illustrate precisely what we mean when we talk about “double-entry”.

More excitingly, this formalism opens the doors to exploring other kinds of accounting systems that focus on other aspects of financial movements. With this formalism in hand, we can start formally describing and imagining other accounting models optimized for different trade-offs and concerns.

Prismic Toolbar iFrame