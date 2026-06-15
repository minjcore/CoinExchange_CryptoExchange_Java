##### Collectives™ on Stack Overflow

Find centralized, trusted content and collaborate around the technologies you use most.

[Learn more about Collectives](https://stackoverflow.com/collectives)

**Stack Internal**

Knowledge at work

Bring the best of human thought and AI automation together at your work.

[Explore Stack Internal](https://stackoverflow.co/internal/?utm_medium=referral&utm_source=stackoverflow-community&utm_campaign=side-bar&utm_content=explore-teams-compact-popover)

# [Relational Data Model for Double-Entry Accounting](https://stackoverflow.com/questions/59432964/relational-data-model-for-double-entry-accounting)

[Ask Question](https://stackoverflow.com/questions/ask)

Asked6 years, 5 months ago

Modified [8 months ago](https://stackoverflow.com/questions/59432964/relational-data-model-for-double-entry-accounting?lastactivity "2025-10-01 06:43:28Z")

Viewed
61k times


This question shows research effort; it is useful and clear

66

This question does not show any research effort; it is unclear or not useful

Save this question.

[Timeline](https://stackoverflow.com/posts/59432964/timeline)

Show activity on this post.

Assume there is a bank, a large shop, etc, that wants the accounting to be done correctly, for both internal accounts, and keeping track of customer accounts. Rather than implementing that which satisfies the current simple and narrow requirement, which would a 'home brew': those turn out to be a temporary crutch for the current simple requirement, and difficult or impossible to extend when new requirements come it.

As I understand it, **[Double-Entry Accounting](https://en.wikipedia.org/wiki/Double-entry_bookkeeping_system)** is a method that is well-established, and serves all Accounting and Audit requirements, including those that are not contemplated at the current moment. If that is implemented, it would:

- eliminate the incremental enhancements that would occur over time, and the expense,
- there will not be a need for future enhancement.

I have studied this Answer to another question: [Derived account balance vs stored account balance for a simple bank account?](https://stackoverflow.com/questions/29688982/derived-account-balance-vs-stored-account-balance-for-a-simple-bank-account/29713230#29713230), it provides good information, for internal Accounts. A data model is required, so that one can understand the entities; their interaction; their relations, and @PerformanceDBA has given that. This model is taken from that Answer:

![](https://www.softwaregems.com.au/Documents/Student%20Resolutions/Anmol%20Gupta%20TA%20Account.png)

Whereas that is satisfactory for simple internal accounts, I need to see a data model that provides the full Double-Entry Accounting method.

The articles are need to be added are `Journal`; internal vs external `Transactions`; etc..

Ideally I would like to see what those double entry rows look like in database terms, what the whole process will look like in SQL, which entities are affected in each case, etc. Cases like:

1. A Client deposits cash to his account
2. The Bank charges fees once a month to all Clients accounts (sample batch job),
3. A Client does some operation over the counter, and the Bank charges a fee (cash withdrawal + withdrawal fee),
4. Mary sends some money from her account, to John's account, which is in the same bank

Let's just call it `System` instead of `Bank`, `Bank` may be too complex to model, and let the question be about _imaginary_ system which operates with accounts and assets. Customers perform a set of operations with system (deposits, withdrawals, fee for latter, batch fees), and with each other (transfer).

- [sql](https://stackoverflow.com/questions/tagged/sql "show questions tagged 'sql'")
- [database](https://stackoverflow.com/questions/tagged/database "show questions tagged 'database'")
- [database-design](https://stackoverflow.com/questions/tagged/database-design "show questions tagged 'database-design'")
- [relational-database](https://stackoverflow.com/questions/tagged/relational-database "show questions tagged 'relational-database'")
- [accounting](https://stackoverflow.com/questions/tagged/accounting "show questions tagged 'accounting'")

[Share](https://stackoverflow.com/q/59432964)

Share a link to this question

Copy link [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/ "The current license for this post: CC BY-SA 4.0")

Short permalink to this question

[Improve this question](https://stackoverflow.com/posts/59432964/edit "")

Follow



Follow this question to receive notifications

[edited Dec 23, 2019 at 18:27](https://stackoverflow.com/posts/59432964/revisions "show all edits to this post")

[![PerformanceDBA's user avatar](https://www.gravatar.com/avatar/d14596734c0c2c409886b612063553cf?s=64&d=identicon&r=PG)](https://stackoverflow.com/users/484814/performancedba)

[PerformanceDBA](https://stackoverflow.com/users/484814/performancedba)

34k1010 gold badges7373 silver badges9696 bronze badges

asked Dec 21, 2019 at 1:54

[![Alex's user avatar](https://www.gravatar.com/avatar/f091206a7c5d52203ccd7a4368b93697?s=64&d=identicon&r=PG&f=y&so-version=2)](https://stackoverflow.com/users/4896540/alex)

[Alex](https://stackoverflow.com/users/4896540/alex)

1,10411 gold badge1111 silver badges1818 bronze badges

4

- Comments are not for extended discussion; this conversation has been [moved to chat](https://chat.stackoverflow.com/rooms/204693/discussion-on-question-by-alex-relational-model-for-double-entry-accounting).



samliew


–
[samliew](https://stackoverflow.com/users/584192/samliew "79,622 reputation")



2019-12-22 21:36:35 +00:00

[CommentedDec 22, 2019 at 21:36](https://stackoverflow.com/questions/59432964/relational-data-model-for-double-entry-accounting#comment105078977_59432964)


- 1





Great edit, thank you. There was `Batch` mentioned, I don't know if it makes sense to bring it back, perhaps it only exist in my current understanding that it must be a special table.



Alex


–
[Alex](https://stackoverflow.com/users/4896540/alex "1,104 reputation")



2019-12-24 01:59:11 +00:00

[CommentedDec 24, 2019 at 1:59](https://stackoverflow.com/questions/59432964/relational-data-model-for-double-entry-accounting#comment105106832_59432964)

- 1





1) `Batch` does not need a data table for anything regarding the data content that the batch is processing. 2) Separately, one may have a `Batch` table for the purpose of administering the batch queue; controlling restart points; parallel processing (Threads); etc. All of which I have. But that is an Utility table, with no data content from the database proper. 3) Check my Answer to see if the batch issue is covered to your satisfaction. If not, please comment, and I will edit the Answer.



PerformanceDBA


–
[PerformanceDBA](https://stackoverflow.com/users/484814/performancedba "34,014 reputation")



2019-12-24 07:27:00 +00:00

[CommentedDec 24, 2019 at 7:27](https://stackoverflow.com/questions/59432964/relational-data-model-for-double-entry-accounting#comment105110563_59432964)

- 3





This guy wanted to leave a comment but didn't have enough rep: [stackoverflow.com/q/59521817/6456163](https://stackoverflow.com/q/59521817/6456163)



Aaron Meese


–
[Aaron Meese](https://stackoverflow.com/users/6456163/aaron-meese "2,383 reputation")



2019-12-29 17:48:43 +00:00

[CommentedDec 29, 2019 at 17:48](https://stackoverflow.com/questions/59432964/relational-data-model-for-double-entry-accounting#comment105214581_59432964)


[Add a comment](https://stackoverflow.com/questions/59432964/relational-data-model-for-double-entry-accounting# "Use comments to ask for more information or suggest improvements. Avoid answering questions in comments.") \| [Expand to show all comments on this post](https://stackoverflow.com/questions/59432964/relational-data-model-for-double-entry-accounting# "Expand to show all comments on this post")

## 2 Answers 2

Sorted by:
[Reset to default](https://stackoverflow.com/questions/59432964/relational-data-model-for-double-entry-accounting?answertab=scoredesc#tab-top)

Highest score (default)

Trending (recent votes count more)

Date modified (newest first)

Date created (oldest first)


This answer is useful

209

This answer is not useful

Save this answer.

Loading when this answer was accepted…

+50


This answer has been awarded bounties worth 50 reputation by Alex

[Timeline](https://stackoverflow.com/posts/59465148/timeline)

Show activity on this post.

# A. Preliminary

### Your Approach

First and foremost, I must commend your attitude. It is rare to find someone who not only thinks and works from a solid grounding, and who wishes to understand and implement a Double-Entry Accounting system, instead of:

- either _not_ implementing DEA, thus suffering multiple re-writes, and pain at each increment, each new requirement,

- or implementing DEA, but re-inventing the wheel from scratch, by figuring it out for oneself, and suffering the pain at each exposure of error, and the demanded bug fixes, a sequence that never ends.


To avoid all that, and to seek the standard Method, is highly commended.

Further, you want that in the form of a Relational data model, you are not enslaved by the Date; Darwen; Fagin; et al views that prescribes a `Record ID` based Record Filing Systems, that cripples both the modelling exercise and the resulting "database". These days, some people are obsessed with primitive RFS and suppress Dr E F Codd's _Relational Model_.

## 1\. Approach for the Answer

If you do not mind, I will explain things from the top, in logical order, so that I can avoid repeats, rather than just answering your particular requests. I apologise if you have complete knowledge of any of these points.

### Obstacle

> Ideally I would like to see what those **double entry rows** look like in database terms

That is an obstacle to the proper approach that is required for modelling or defining anything.

- In the same way that stamping an `ID` field on every file, and making it the "key", cripples the modelling exercise, because it prevents analysis of the data (what the thing that the data represents actually is), expecting two rows for a Credit/Debit pair at the start will cripple the understanding of what the thing is; what the accounting actions are; what effect those actions have; and most important, how the data will be modelled. Particularly when one is learning.

> **Aristotle** teaches us that:
>
> > _the least initial deviation from the truth is multiplied later a thousandfold ..._
> >
> > _a principle is great, rather in power, than in extent; hence that which was small \[mistake\] at the start turns out a giant \[mistake\] at the end._

Paraphrased as, a small mistake at the beginning (eg. principles; definitions) turns out to be a large mistake at the end.

Therefore the intellectual requirement, the first thing, is to clear your mind regarding what it will be at the end of the modelling exercise. Of course, that is also required when one is learning what it is, in accounting terms.

## 2\. Scope for the Answer

> Assume there is a bank, a large shop, etc, that wants the accounting to be done correctly, for both internal accounts, and keeping track of customer accounts.
>
> Let's just call it `System` instead of `Bank`, `Bank` may be too complex to model ...
>
> Customers perform a set of operations with system (deposits, withdrawals, fee for latter, batch fees), and with each other (transfer).

To be clear, I have determined the scope to be as follows. Please correct me if it is not:

- Not a small business with a General Ledger only, with no Customer Accounts
- But a small community Bank, with no branches (the head office is _the_ branch)
- You want both the **internal** Accounts, which consists of:
- a simple General **Ledger**,
- as well as **external** Accounts, one for each Customer
- The best concept that I have in mind is a small community Bank, or a business that operates like one. An agricultural cooperative, where each farmer has an **Account** that he purchases against, and is billed and paid monthly, and the cooperative operates like a small bank, with a full General **Ledger**, and offers some simple bank facilities.
- A single Casino (not a chain) has the same requirement.
- Not a large Bank with multiple branches; various financial products; etc.
- Instead of `System` or `Bank`, I will call it `House`. The relevance of that will be clear later.

Anyone seeking the Double-Entry method for _just_ the **Ledger**, _without_ the external Customer **Account**, can glean that easily from this Answer.

In the same vein, the data model given here is easy to expand, the `Ledger` can be larger than the simple one given.

* * *

# B. Solution

## 1\. Double-Entry Accounting

### 1.1. Concept

To know what that it is by name; that it has great value; that it is better than a roll-your-own system, is one thing, knowing what it is deeply enough to implement it, is another.

1. First, one needs to have a decent understanding of a General Ledger, and general Accounting principles.

2. Second, understand the concept that money represents value. Value cannot be created or destroyed, it can only be moved. **From** one bucket in the accounts **to** another bucket, otherwise known as Debit (the from-account) and Credit (the to-account).

3. While it is true that _the SUM( all Credits ) = SUM( all Debits )_, and one can obtain such a report from a DEA system, that is not the understanding required for implementation, that is just one end result. There is more to it.


- While it is true that _every transaction consists of a pair: one Credit and one Debit for the same amount_, there is more to that as well.

- Each leg of the pair; the Credit and Debit, is not in the same Account or Ledger, they are in different Accounts, or Ledgers, or Accounts-and-Ledgers.

- The _SUM( all Credits )_ is not simple, because they are in those different places (sets). They are not in two rows in the same table (they could be, more later). Likewise, _the SUM( all Debits )_.

- Thus each of the two SUM()s cover quite different sets (Relational Sets), and have to be obtained first, before the two SUM()s can be compared.


### 1.2. Understanding Double-Entry Accounting

Before attempting a DEA implementation, we need to understand the thing that we are implementing, properly. I advise the following:

1. You are right, the first principle is to hold the **perspective of the Credit/Debit Pair**, when dealing with anything in the books, the General Ledger; the Customer Accounts; the bank Accounts; etc.

- This is the overarching mindset to hold, separate to whatever needs to be done in this or that Account or Ledger.

- I have positioned it at the top; left, in the data model, such that the subordination of all articles to it is rendered visually.


2. The **purpose** or goal of a Double-Entry Accounting system is:

- Eliminate (not just reduce) what is known as:

  - "lost" money

  - "lost" Transactions (one or the other side of the Credit/Debit pair)

  - and the time wasted in chasing it down.

  - Not only can money be found easily, but exactly what happened to it, and where it is now, can be determined quickly.
- Full Audit functionality


It is not good enough to keep good Accounts, it is imperative for a business that accounts for other people's money, to be readily audit-able. That is, any accountant or auditor must be able to examine the books without let or hindrance.

  - This is why the first thing an outsider, eg. an auditor, wants to know is, _does the SUM( all Credits ) = SUM( all Debits )_. This also explains why the DEA concept is above any Accounts or accounting system that the company may be keeping.
- The great benefit, although tertiary, is that the everyday or month end tasks, such as a Trial Balance or closing the books, can be closed easily and quickly. All reports; Statements; Balance Sheets; etc, can be obtained simply (and with a single `SELECT` if the database is Relation).


3. _Then_ ready the Wikipedia entry for **[Double-Entry Bookkeeping](https://en.wikipedia.org/wiki/Double-entry_bookkeeping_system)**.

- The internet has plenty of misleading information, and Wikipedia is particularly awful that is forever changing (truth does not change, falsity changes with the weather), but sorry, that is all we have. Use it only to obtain an overview, it has no structural or logical descriptions, despite its length. Follow the links for better info.

- I do not entirely agree with the terminology in the [Wikipedia article](https://en.wikipedia.org/wiki/Double-entry_bookkeeping_system). Nevertheless, in order to avoid avoidable confusion, I will use those terms.

- There are tutorials available on the web, some better than others. These are recommended for anyone who is implementing a proper Accounting system, with or without DEA. That takes time, it is not relevant to an answer such as this, and that is why I have linked the [Wikipedia article](https://en.wikipedia.org/wiki/Double-entry_bookkeeping_system).


## 2\. Business Transaction

> Ideally I would like to see what ~~those~~ double entry ~~rows~~ looks like in database terms, what the whole process will look like in SQL, which entities are affected in each case, etc.

Ok. Let's go with the Transactions first, then build up to understanding the data model that supports them, then inspect the example rows. Any other order would be counter-productive, and cause unnecessary back-and-forth.

Your numbering. Green is `House` in the General `Ledger`, blue is external Customer `Account`, black is neutral.

- This is the first increment of **Treatment**, how a thing is treated, in different scenarios (your concern, and your request for specific examples, is precisely correct).

- **Credit/Debit Pairs**


This is the first principle of DEA, understand the pair, as the pair, and nothing but the pair.


Do not worry about how the General `Ledger` or the `Account` is set up, or what the data model looks like. Think in terms of an accountant (what has to be done in the books), not in terms of a developer (what has to be done in the system).

Notice that the each leg of the pair is in the one set (the `Ledger`), or in two sets (one leg in the `Ledger`, the other leg in `Account`). There are no pairs in which both legs are in `Account`.

- Because DEA is implemented, each **Business Transaction** (as distinct from a database Transaction), consists of two actions, one for each Credit/Debit leg. The two actions are two entries in a paper-based account book.

> 1. A Client deposits cash to his account

![Op11](https://www.softwaregems.com.au/Documents/Student_Resolutions/Alex/Alex%20Op%201_1.png)![Op12](https://www.softwaregems.com.au/Documents/Student_Resolutions/Alex/Alex%20Op%201_2.png)

- During the DayEnd procedure, among other tasks, all cash is accounted for and checked. The day is closed. All cash sitting in `HouseCash` that is beyond whatever the bank deems necessary for everyday cash Transactions, is moved to `HouseReserve`.

![Op13](https://www.softwaregems.com.au/Documents/Student_Resolutions/Alex/Alex%20Op%201_3.png)

> 2. The Bank charges fees once a month to all Clients accounts (sample batch job)

![Op2](https://www.softwaregems.com.au/Documents/Student_Resolutions/Alex/Alex%20Op%202.png)

- This charges each `Account` with the `Fee`
- `Fee` is dependent on `AccountType_Ext`
- This is the simple case. If the `Fee` is dependent on something else, such as the number of transactions in the `Account`; or the `CurrentBalance` being below or above some limit; etc, that is not shown. I am sure you can figure that out.

> 3. A Client does some operation over the counter, and the Bank charges a fee (cash withdrawal + withdrawal fee),

- Simple Transactions do not incur fees, and Deposit/Withdrawal has already been given. Let's examine a business Transaction that actually attracts a fee.

![Op3](https://www.softwaregems.com.au/Documents/Student_Resolutions/Alex/Alex%20Op%203.png)

- Mary sends $500 USD to her son Fred, who is travelling overseas looking for whales to save, and has run out of money. The bank charges $30 for an Overseas Bank Transfer. Fred can collect the funds (in local currency equivalent of $500 USD) at any partner bank branch.
- To actually transfer the money to the foreign bank, the `House` has to interact with a local big bank that provides international settlement and currency exchange services. That is not relevant to us, and not shown. In any case, all those types of `Interbank` transactions are batched and dealt with once per day, not once per `AccountTransaction`.
- In this simple DEA system, the `House` does not have currency accounts in the `Ledger`. That is easy enough to implement.

> 4. Mary sends some money from her account, to John's account, which is in the same bank

![Op4](https://www.softwaregems.com.au/Documents/Student_Resolutions/Alex/Alex%20Op%204.png)

- The money is currently in Mary's account (deposited on a day prior to today), that is why it is in `HouseReserve`, not `HouseCash`
- The money is moved from `HouseReserve` into `HouseCash` because John may come into the bank today and withdraw it.
- As described in example \[1.3\] above, at the DayEnd procedure, any money sitting in `HouseCash` in all `Accounts` will be moved to `HouseReserve`. Not shown.

* * *

## 3\. Relational Data Model • Initial

Now let's see what the data modeller has done, to support the accountant's needs, the business Transactions.

- This is of course, the second increment of **Treatment**, what the modeller has understood the real world business Transactions to be, expressed in Relational terms (FOPC; _RM_; Logic; Normalisation)

- This is not the simplest data model that is required to satisfy the restated scope.

- There are simpler models (more later), but they have problems that this one does not have, problems that are desirable, if not imperative, to avoid.

- The image is too large for in-line viewing. Open the image in a new tab, to appreciate it in full size.


![TA](https://www.softwaregems.com.au/Documents/Student_Resolutions/Alex/Alex%20TA.png)

### 3.1. Notation

- All my data models are rendered in **[IDEF1X](https://www.iso.org/standard/60614.html)**, the Standard for modelling Relational databases since 1993.

- My **[IDEF1X Introduction](https://www.softwaregems.com.au/Documents/Documentary%20Examples/IDEF1X%20Introduction.pdf)** is essential reading for those who are new to the _Relational Model_, or its modelling method. Note that IDEF1X models are rich in detail and precision, showing _all_ required details, whereas home-grown models, being unaware of the imperatives of the Standard, have far less definition. Which means, the notation needs to be fully understood.


### 3.2. Content

- The main differences between the best data model produced by someone else (ie. genuine Relational, genuine DEA, as opposed to anti-Relational `RecordID` or questional DEA), and mine, are:

1. the **Business Transaction**, the **Credit/Debit pair** (always two actions; two legs, one per Credit/Debit) is affected by a **single row** with two sides,


     in `AccountTransaction` or `LedgerTransaction`.

2. Most modellers will model two rows for the Credit/Debit pair, one for each leg or side


     ( _hey, one leg is a Credit, and the other leg is a Debit, if I Normalise that, I get two rows_).

     - Wrong. If I tell you that Fred is Sally's father, you know, from that single Fact, that Sally is Fred's daughter.
     - A `FOREIGN KEY` needs to be declared just once, not once for each side (as advised by the perverted academics).
     - Further, the number of rows in the `%Transaction` tables is halved.
3. Likewise, the **Credit/Debit pair** is a single **Database Transaction**,


     a single Atomic article, that can be perceived from either Side, like two sides of one coin. Modelled as such.

     - Even for those with sub-standard OLTP code, or open-farce "sql" (which has no Transactions or OLTP), which causes quite preventable concurrency problems, if this method is implemented, this is one article wherein those problems will not arise.
- All manner of preventable bugs are prevented, the search for the "missing" leg is eliminated.

- I have arranged the relevant entities, such that the

_External_`Account`

_Internal_`Ledger`

_Internal_`LedgerTransaction`

_Internal-External_`AccountTransaction`


are clear.

- Along with a nugget of definition from the [Wikipedia entry](https://en.wikipedia.org/wiki/Double-entry_bookkeeping_system).

- Having familiarised yourself with the DEA Credit/Debit pairs, now study the **Treatment** of the pair. Notice that the Treatment is different, it is based on a number of criteria (three account types; six `Ledger` types; etc), which in turn is based on the complexity of the General Ledger.

- This `Ledger` is simple, with `Asset/Liability` accounts only. Of course, you are free to expand that.

  - For a fully defined `Ledger`, a proper Ledger Hierarchy ("Chart of Accounts"), refer to this Q & A **[Relational Data Model for double-entry accounting with job costing](https://stackoverflow.com/a/59857806/484814)**

  - To understand the proper implementation of Hierarchies (Trees) within the _Relational Model_, refer to my **[Hierarchy](https://www.softwaregems.com.au/Documents/Article/Database/Relational%20Model/Hierarchy.pdf)** document.
- The eagle-eyed will notice that `AccountStatement.ClosingBalance` and `LedgerStatement.ClosingBalance` can actually be derived, and thus (on the face of it), should not be stored. However, these are published figures, eg. the Monthly Bank Statement for each Account, and thus subject to **Audit**, and therefore it _must_ be stored.

  - For a full treatment of that issue, including considerations; definition; treatment, refer to this Q & A:
    **[Derived account balance vs stored account balance for a simple bank account?](https://stackoverflow.com/a/29713230/484814)**

### 3.3. Summary

In closing this section, we should have reached this understanding:

- The overarching principle of DEA, the Credit/Debit pairs, purely intellectual

- The typical business Transactions, always a Credit/Debit pair, two legs, two entries in the accounting books

- A deeper understanding of the Treatment of said Transactions

- The environment that the `House` (small bank; cooperative; casino) manages (internal `Ledger` and external customer `Account`)

- A first look at a data model that is proposed to handle all that.


* * *

## 4\. Relational Data Model • Full

Here it is again, with a full set of sample data.

- Re the **Primary Keys**:

- Note that `LedgerNo` and `AccountNo` are not surrogates, they have meaning for the organisation, in ordering and structuring the `Ledger`, etc. They are stable numbers, not an `AUTOINCREMENT` or `IDENTITY` or anything of the sort.

- The Primary Keys for `LedgerTransaction` and `AccountTransaction` are pure, composite Relational Keys.

- It is not a Transaction Number of some kind that is beloved of paper-based accountants.

- It is not a crippling `Record ID` either.

- The **Alternate Keys** are more meaningful to humans, hence I have used them in the examples (Business Transactions, above \[2\], and below \[5\]). This Answer is already layered, it would be a nightmare trying to relate hundreds of `1's, 2's` and `3’s` to each other.

- If we wish to understand what something means, we need to hold onto the meaning that exists in the thing, rather than excising the meaning by giving it a number.

- In the example data, the Primary Keys are bold.


![TAdata](https://www.softwaregems.com.au/Documents/Student_Resolutions/Alex/Alex%20TA%20Data.png)

* * *

## 5\. Business Transaction with Row

> Ideally I would like to see what ~~those~~ double entry ~~rows~~ looks like in database terms, what the whole process will look like in SQL, which entities are affected in each case, etc.

Now that we understand the Business Transactions, and the data model that services the requirement, we can examine the Business Transactions _along with_ affected rows.

- Each Business Transaction, in DEA terms, has two legs, two entries in the paper-based account books, for each of the Credit/Debit pair,


is yet a single Business Transaction, and now:


it is affected by **a single row** with two sides, for each of the Credit/Debit pair.

- This is the third increment in understanding **Treatment**: the business Transactions; data model to implement them; and now, the affected rows

- The example database rows are prefixed with the table name in short form.


Plus means `INSERT`


Minus means `DELETE`


Equal means `UPDATE`.


> 1. A Client deposits cash to his account

![Row11](https://www.softwaregems.com.au/Documents/Student_Resolutions/Alex/Alex%20Row%201_1.png)![Row12](https://www.softwaregems.com.au/Documents/Student_Resolutions/Alex/Alex%20Row%201_2.png)![Row13](https://www.softwaregems.com.au/Documents/Student_Resolutions/Alex/Alex%20Row%201_3.png)

> 2. The Bank charges fees once a month to all Clients accounts (sample batch job)

![Row2](https://www.softwaregems.com.au/Documents/Student_Resolutions/Alex/Alex%20Row%202.png)

- This, too, is a batch job, just one task in the MonthEnd procedure.
- Notice the date is the first day of the month.

> 3. A Client does some operation over the counter, and the Bank charges a fee (cash withdrawal + withdrawal fee),

![Row3](https://www.softwaregems.com.au/Documents/Student_Resolutions/Alex/Alex%20Row%203.png)

- To be clear, that is three Business Transactions; two entries each, one for each side of the Credit/Debit pair; affected by one database row each.

> 4. Mary sends some money from her account, to John's account, which is in the same bank

![Row4](https://www.softwaregems.com.au/Documents/Student_Resolutions/Alex/Alex%20Row%204.png)

* * *

## 6\. SQL Code

There are usually several ways to skin a cat (code), but very few if the cat is alive (code for a high concurrency system).

- The _Relational Model_ is **founded** on First Order Predicate Calculus (aka First Order Logic), all definitions (DDL) and thus all queries (DML) are entirely Logical.

- A data model that conforms to that understanding, is therefore entirely Logical.

- The queries against such a data model are dead easy: Logical and straight-forward. They have none of the convoluted code that is required for `Record ID` based filing systems.


Therefore, out of the several methods that are possible for the SQL code requests, I give the most direct and logical.

> The code examples are that which is appropriate for SO, it is imperative that you trap and recover from errors; that you do not attempt anything that will fail (check the validity of the action before using a verb), and follow OLTP Standards for ACID Transactions, etc. The example code given here are the relevant snippets only.

### 6.1. SQL View • Account Current Balance

Since this code segment gets used in many places, let's do the right thing and create a View.

- Note that on genuine SQL platforms, source code is compiled and run when it is submitted, Stored Procs and Views are stored in their compiled form, thus eliminating the compilation on every execution. Unlike the mickey mouse NONsql suites.

- High-end commercial SQL platforms do a lot more, such as caching the Query Plans for Views, and the queries in Stored Procs.


```sql
CREATE VIEW Account_Current_V
    AS
SELECT  AccountNo,
    Date = DATEADD( DD, -1, GETDATE() ),     -- show /as of/ previous day
    ASS.ClosingBalance,                      -- 1st of this month
    TotalCredit = (
        SELECT SUM( Amount )
            FROM AccountTransaction  ATT
            WHERE ATT.AccountNo = ASS.AccountNo
                AND XactTypeCode_Ext IN ( "AC", "Dp" )
                -- >= 1st day of this month yy.mm.01  /AND <= current date/
                AND DateTime >= CONVERT( CHAR(6), GETDATE(), 2 ) + "01"
            ),
    TotalDebit = (
        SELECT SUM( Amount )
            FROM AccountTransaction ATT
            WHERE ATT.AccountNo = ASS.AccountNo
                AND XactTypeCode_Ext NOT IN ( "AC", "Dp" )
                AND DateTime >= CONVERT( CHAR(6), GETDATE(), 2 ) + "01"
                ),
    CurrentBalance = ClosingBalance +
        <TotalCredit> -   -- subquery above
        <TotalDebit>      -- subquery above
    FROM AccountStatement  ASS
                                             -- 1st day of this month
    WHERE ASS.Date = CONVERT( CHAR(6), GETDATE(), 2 ) + "01"
```

### 6.2. SQL Transaction • \[1.2\] Withdraw from \[External\] Account

A proc for another DEA business Transaction.

```sql
CREATE PROC Account_Withdraw_tr (
    @AccountNo,
    @Amount
    ) AS
    IF EXISTS ( SELECT 1                       -- validate before verb
            FROM AccountCurrent_V
            WHERE AccountNo = @AccountNo
                AND CurrentBalance >= @Amount  -- withdrawal is possible
            )
        BEGIN
        SELECT @LedgerNo = LedgerNo
            FROM Ledger
            WHERE Name = "HouseCash"
        BEGIN TRAN
        INSERT AccountTransaction
            VALUES ( @LedgerNo, GETDATE(), "Cr", "Wd", @AccountNo, @Amount )
        COMMIT TRAN
        END
```

### 6.3. SQL Transaction • \[1.1\] Deposit to \[External\] Account

A proc, set up as an SQL Transaction, to execute a DEA business Transaction.

```sql
CREATE PROC Account_Deposit_tr (
    @AccountNo,
    @Amount
    ) AS
    ... IF EXISTS, etc ...                   -- validate before verb
        BEGIN
        SELECT @LedgerNo ...
        BEGIN TRAN
        INSERT AccountTransaction
            VALUES ( @LedgerNo, GETDATE(), "Dr", "Dp", @AccountNo, @Amount )
        COMMIT TRAN
        END
```

### 6.4. SQL Transaction • \[Internal\] Ledger Account Transfer

A proc to add any business Transaction to `LedgerAccount`. It is always:

- one `LedgerTransaction.LedgerNo`, which is the `Credit` leg
- one `LedgerTransaction.LedgerNo_Dr`, which is the `Debit` leg.
- given by the caller.

```sql
CREATE PROC Ledger_Xact_tr (
    @LedgerNo,    -- Credit Ledger Account
    @LedgerNo_Dr, -- Debit  Ledger Account
    @Amount
    ) AS
    ... IF EXISTS, etc ...
        BEGIN
        SELECT @LedgerNo ...
        BEGIN TRAN
        INSERT LedgerTransaction
            VALUES ( @LedgerNo, GETDATE(), @LedgerNo_Dr, @Amount )
        COMMIT TRAN
        END
```

### 6.5. SQL Batch Task • Account Month End

This uses a View that is similar to **\[6.1 Account Current Balance\]**, for any month (views are generic), with the values constrained to the month. The caller selects the previous month.

- This Answer now exceeds the SO limit, thus it is provided in a link [Account\_Month\_V](https://www.softwaregems.com.au/Documents/Student_Resolutions/Alex/Account_Month_V.sql).

Just one Task, in a stored proc, to process the Month End for `AccountStatement`, which is executed as a batch job. Again, just the essential code, the infrastructure needs to be added.

```sql
CREATE PROC Account_MonthEnd_btr ( ... )
    AS
... begin loop
... batch transaction control (eg. 500 rows per xact), etc ...
INSERT AccountStatement
    SELECT  ACT.AccountNo,
            CONVERT( CHAR(6), GETDATE(), 2 ) + "01",  -- 1st day THIS month
            AMV.ClosingBalance,                       -- for PREVIOUS month
            AMV.TotalCredit,
            AMV.TotalDebit
        FROM Account ACT
            JOIN Account_Month_V AMV               -- follow link for code
                ON ACT.AccountNo = AMV.AccountNo
                                                   -- 1st day PREVIOUS month
        WHERE AMV.OpeningDate = DATEADD( MM, -1, ACT.Date )
... end loop
... batch transaction control, etc ...
```

### 6.6. SQL Report • SUM( Credit ) vs SUM( Debit )

> > While it is true that _the SUM( all Credits ) = SUM( all Debits )_, and one can obtain such a report from a DEA system, that is not the **understanding**. There is **more** to it.

Hopefully, I have given the Method and details, and covered the _understanding_ and the _more_, such that you can now write the required `SELECT` to produce the required report with ease.

Or perhaps the Monthly Statement for external `Accounts`, with a running total `AccountBalance` column. Think: a Bank Statement.

- One of the many, great efficiencies of a genuine Relational database is, any report can be serviced via a **single `SELECT` command**.

* * *

## One PDF

Last but not least, it is desirable to have the Data Model; the example Transactions; the code snippets, all organised in a single **[PDF](https://www.softwaregems.com.au/Documents/Student_Resolutions/Alex/Alex%20Account%20TA.pdf)**, in A3 (11x17 for my American friends). For studying and annotation, print in A2 (17x22).

* * *

[Share](https://stackoverflow.com/a/59465148)

Share a link to this answer

Copy link [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/ "The current license for this post: CC BY-SA 4.0")

Short permalink to this answer

[Improve this answer](https://stackoverflow.com/posts/59465148/edit "")

Follow



Follow this answer to receive notifications

[edited Jul 10, 2025 at 13:15](https://stackoverflow.com/posts/59465148/revisions "show all edits to this post")

answered Dec 24, 2019 at 7:11

[![PerformanceDBA's user avatar](https://www.gravatar.com/avatar/d14596734c0c2c409886b612063553cf?s=64&d=identicon&r=PG)](https://stackoverflow.com/users/484814/performancedba)

[PerformanceDBA](https://stackoverflow.com/users/484814/performancedba)

34k1010 gold badges7373 silver badges9696 bronze badges

Sign up to request clarification or add additional context in comments.


## 74 Comments

Add a comment

[![](https://i.sstatic.net/eO77F.jpg?s=64)](https://stackoverflow.com/users/2840542/antc)

AntC

[AntC](https://stackoverflow.com/users/2840542/antc) [Over a year ago](https://stackoverflow.com/questions/59432964/relational-data-model-for-double-entry-accounting#comment105220117_59465148)

This answer is remarkable for two things (apart from its length): a) there are no citations to established practice/textbooks; b) the design of "a business Transaction is a single row" is not used by any ERP or accounting package, and for a good reason: it is inadequate. It's true that many transactions (especially in banking) consist of only two legs, debit and credit (equal and opposite). But in general there might be several legs -- typically with tax and/or charges, multiple distributions from one account to several others.

2019-12-30T00:48:04.47Z+00:00

34

Reply

- Copy link

[![](https://i.sstatic.net/eO77F.jpg?s=64)](https://stackoverflow.com/users/2840542/antc)

AntC

[AntC](https://stackoverflow.com/users/2840542/antc) [Over a year ago](https://stackoverflow.com/questions/59432964/relational-data-model-for-double-entry-accounting#comment105220688_59465148)

Oh, I should add: a typical requirement in a fully-featured account system is to hold 'pending' or 'registered not complete' transactions (sometimes called an 'inbox'). For example we've received an invoice for $100; we know the vendor's account (and we want to show the $100 against that vendor in an enquiry/balance owing); but we don't know our account coding for the line items/expenses (not all of them). So we post a 'one-sided' transaction, at a pending status to distinguish it from fully-posted/balancing transactions. 'Single-entry' bookkeeping is inadequate to meet this requirement.

2019-12-30T01:49:08.91Z+00:00

3

Reply

- Copy link

[![](https://www.gravatar.com/avatar/4a9972ad0a7d68685060702a04ddb28b?s=48&d=identicon&r=PG)](https://stackoverflow.com/users/1017313/rex-bloom)

Rex Bloom

[Rex Bloom](https://stackoverflow.com/users/1017313/rex-bloom) [Over a year ago](https://stackoverflow.com/questions/59432964/relational-data-model-for-double-entry-accounting#comment118227556_59465148)

@Edward My experience in banking is that complex transactions like you mention are common. The important thing is that the Ledger is in balance not that each transaction has a matching credit and debit. Payroll, Credit Card Bills,Settlements, all have large credits and debits that may offset to more than one ledger account. Even Quick Books has a Split function for this same purpose. If I deposit 1,000 (credit) and need to assign that to two invoices (debit) I don't create two credit transactions - it is one credit with two debits that balance.

2021-03-31T07:32:07.683Z+00:00

4

Reply

- Copy link

[![](https://www.gravatar.com/avatar/d14596734c0c2c409886b612063553cf?s=48&d=identicon&r=PG)](https://stackoverflow.com/users/484814/performancedba)

PerformanceDBA

[PerformanceDBA](https://stackoverflow.com/users/484814/performancedba) [Over a year ago](https://stackoverflow.com/questions/59432964/relational-data-model-for-double-entry-accounting#comment118244242_59465148)

@RexBloom QuickBooks is not DEA. QuickBooks cannot be used in Banking (which demands DEA). You are performing a process (balancing) which is **manual, outside the system**, before entering your non-DEA 'transactions". You don't need to do that under DEA, because the system is always balanced, because each Transaction is balanced. No manual control required. It is not "two credit transactions", it is two DEA Transactions, each a Credit **and** a Debit.

2021-03-31T17:48:56.293Z+00:00

4

Reply

- Copy link

[![](https://i.sstatic.net/eO77F.jpg?s=64)](https://stackoverflow.com/users/2840542/antc)

AntC

[AntC](https://stackoverflow.com/users/2840542/antc) [Over a year ago](https://stackoverflow.com/questions/59432964/relational-data-model-for-double-entry-accounting#comment119632565_59465148)

thank you @taffer, and well done for persisting. Typically a receivables-invoice transaction (entered to a Double-Entry Accounting system) uses at least three accounts _within the one transaction_: Customer (receivable total); tax (for transfer to the government); product sold (revenue for us). In American jurisdictions, we must account separately for Federal vs State vs City/local taxes -- so at least 5 account entries _within the one transaction_. Legally we must not represent the tax as 'our' revenue -- because we're merely collecting it on behalf of the government.

2021-05-25T06:28:18.883Z+00:00

4

Reply

- Copy link

Add a comment

This answer is useful

9

This answer is not useful

Save this answer.

Loading when this answer was accepted…

[Timeline](https://stackoverflow.com/posts/74175761/timeline)

Show activity on this post.

Here is my schema (using sqlite as an example) with 2 tables:

**Tables**

```sql
-- This is a list of your chart of accounts
CREATE TABLE "accounts" (
    "name"      TEXT,
    "number"    INTEGER,
    "normal"    INTEGER
)

-- This is a table of each transaction
CREATE TABLE "transactions"
  (
     "id"        INTEGER,
     "date"      TEXT,
     "amount"    REAL,
     "account"   INTEGER,
     "direction" INTEGER
  )
```

With this convention, the `accounts.normal` and `transaction.direction` fields are set to `1` for debit and `-1` for credit. The end user never sees this but it makes arithmetic easy.

When you create a journal entry, it will have at least 2 rows in the `transactions` table - a debit and a credit. They should share the same `id`.

To see your balances, you can run this query:

```sql
select
  (account) as a,
  name,
  sum(amount * direction * normal) as balance
from
  transactions
  left join accounts on a = accounts.number
group by
  name
order by
  a,
  name;
```

To view the ledger, you can run this:

```sql
select
  id,
  date,
  name,
  case when direction == 1 then amount end as DR,
  case when direction == -1 then amount end as CR
from
  transactions
  left join accounts on account = accounts.number
order by
  id,
  date,
  CR,
  DR;
```

I have a much more [detailed post](https://web.archive.org/web/20240104205714/https://blog.journalize.io/posts/an-elegant-db-schema-for-double-entry-accounting/) of different queries you can run, along with example data. But, with the above two tables, you can create a working double-entry system.

[Share](https://stackoverflow.com/a/74175761)

Share a link to this answer

Copy link [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/ "The current license for this post: CC BY-SA 4.0")

Short permalink to this answer

[Improve this answer](https://stackoverflow.com/posts/74175761/edit "")

Follow



Follow this answer to receive notifications

[edited Oct 1, 2025 at 6:43](https://stackoverflow.com/posts/74175761/revisions "show all edits to this post")

[![armandino's user avatar](https://www.gravatar.com/avatar/2155c92be66863c4634778bf522efe14?s=64&d=identicon&r=PG)](https://stackoverflow.com/users/45112/armandino)

[armandino](https://stackoverflow.com/users/45112/armandino)

18.8k1717 gold badges7676 silver badges8686 bronze badges

answered Oct 24, 2022 at 0:46

[![poundifdef's user avatar](https://www.gravatar.com/avatar/cad0436b3e7120f39e4d0fda8c90b89f?s=64&d=identicon&r=PG)](https://stackoverflow.com/users/3788/poundifdef)

[poundifdef](https://stackoverflow.com/users/3788/poundifdef)

19.5k2828 gold badges101101 silver badges141141 bronze badges

## 3 Comments

Add a comment

[![](https://i.sstatic.net/YKIu6.jpg?s=64)](https://stackoverflow.com/users/14517835/kwaku-biney)

Kwaku Biney

[Kwaku Biney](https://stackoverflow.com/users/14517835/kwaku-biney) [Over a year ago](https://stackoverflow.com/questions/59432964/relational-data-model-for-double-entry-accounting#comment136181762_74175761)

In this scenario, your `Transaction` table does not have a primary key?

2023-10-06T19:50:19.853Z+00:00

1

Reply

- Copy link

[![](https://www.gravatar.com/avatar/cad0436b3e7120f39e4d0fda8c90b89f?s=48&d=identicon&r=PG)](https://stackoverflow.com/users/3788/poundifdef)

poundifdef

[poundifdef](https://stackoverflow.com/users/3788/poundifdef) [Over a year ago](https://stackoverflow.com/questions/59432964/relational-data-model-for-double-entry-accounting#comment136182518_74175761)

In this example, each row does not have a unique ID. However, you could add one.

2023-10-06T21:17:04.193Z+00:00

0

Reply

- Copy link

[![](https://i.sstatic.net/zG5KD.jpg?s=64)](https://stackoverflow.com/users/687137/suchislife801)

suchislife801

[suchislife801](https://stackoverflow.com/users/687137/suchislife801) [Mar 28 at 16:58](https://stackoverflow.com/questions/59432964/relational-data-model-for-double-entry-accounting#comment141023624_74175761)

This post awesome. Genius simplicity.

2026-03-28T16:58:06.013Z+00:00

0

Reply

- Copy link

Add a comment

Start asking to get answers

Find the answer to your question by asking.

[Ask question](https://stackoverflow.com/questions/ask)

Explore related questions

- [sql](https://stackoverflow.com/questions/tagged/sql "show questions tagged 'sql'")
- [database](https://stackoverflow.com/questions/tagged/database "show questions tagged 'database'")
- [database-design](https://stackoverflow.com/questions/tagged/database-design "show questions tagged 'database-design'")
- [relational-database](https://stackoverflow.com/questions/tagged/relational-database "show questions tagged 'relational-database'")
- [accounting](https://stackoverflow.com/questions/tagged/accounting "show questions tagged 'accounting'")

See similar questions with these tags.

lang-sql

![](https://stackoverflow.com/js-true.gif)