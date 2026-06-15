##### Collectives™ on Stack Overflow

Find centralized, trusted content and collaborate around the technologies you use most.

[Learn more about Collectives](https://stackoverflow.com/collectives)

**Stack Internal**

Knowledge at work

Bring the best of human thought and AI automation together at your work.

[Explore Stack Internal](https://stackoverflow.co/internal/?utm_medium=referral&utm_source=stackoverflow-community&utm_campaign=side-bar&utm_content=explore-teams-compact-popover)

# [Database schema design for a double entry accounting system? \[closed\]](https://stackoverflow.com/questions/2494343/database-schema-design-for-a-double-entry-accounting-system)

[Ask Question](https://stackoverflow.com/questions/ask)

Asked16 years, 2 months ago

Modified [4 years, 10 months ago](https://stackoverflow.com/questions/2494343/database-schema-design-for-a-double-entry-accounting-system?lastactivity "2021-07-16 21:43:52Z")

Viewed
94k times


This question shows research effort; it is useful and clear

59

This question does not show any research effort; it is unclear or not useful

Save this question.

[Timeline](https://stackoverflow.com/posts/2494343/timeline)

Show activity on this post.

Does anybody know or have any links to websites describing details of how to design a database schema for a double entry accounting system ??.

I did find a bunch of articles but non were very explanatory enough.
Would appreciate it if someone could help me on this.

- [database](https://stackoverflow.com/questions/tagged/database "show questions tagged 'database'")
- [database-design](https://stackoverflow.com/questions/tagged/database-design "show questions tagged 'database-design'")
- [accounting](https://stackoverflow.com/questions/tagged/accounting "show questions tagged 'accounting'")

[Share](https://stackoverflow.com/q/2494343)

Share a link to this question

Copy link [CC BY-SA 2.5](https://creativecommons.org/licenses/by-sa/2.5/ "The current license for this post: CC BY-SA 2.5")

Short permalink to this question

[Improve this question](https://stackoverflow.com/posts/2494343/edit "")

Follow



Follow this question to receive notifications

[edited Apr 2, 2011 at 21:10](https://stackoverflow.com/posts/2494343/revisions "show all edits to this post")

[![Kenny Evitt's user avatar](https://www.gravatar.com/avatar/88cc632c2ca44292d6c8fd6688381402?s=64&d=identicon&r=PG)](https://stackoverflow.com/users/173497/kenny-evitt)

[Kenny Evitt](https://stackoverflow.com/users/173497/kenny-evitt)

9,96488 gold badges7474 silver badges9797 bronze badges

asked Mar 22, 2010 at 17:24

[![soden's user avatar](https://www.gravatar.com/avatar/007f969bc7e98d4ecb3e5e82d38ac223?s=64&d=identicon&r=PG)](https://stackoverflow.com/users/299221/soden)

[soden](https://stackoverflow.com/users/299221/soden)

59711 gold badge55 silver badges66 bronze badges

3

- @soden - what specific part of which specific article do you wish to have help on?



DVK


–
[DVK](https://stackoverflow.com/users/119280/dvk "130,165 reputation")



2010-03-22 17:52:04 +00:00

[CommentedMar 22, 2010 at 17:52](https://stackoverflow.com/questions/2494343/database-schema-design-for-a-double-entry-accounting-system#comment2488295_2494343)


- i do have a basic understanding of double entry book keeping but converting this concept into a database schema ,,,, well i guesse my creative juices arent flowing in this one.



soden


–
[soden](https://stackoverflow.com/users/299221/soden "597 reputation")



2010-03-22 17:56:07 +00:00

[CommentedMar 22, 2010 at 17:56](https://stackoverflow.com/questions/2494343/database-schema-design-for-a-double-entry-accounting-system#comment2488327_2494343)

- 2





There is an awful lot of info on the web, that is confusing or incorrect or incomplete. Try this **StackOverflow Q & A [Relational Data Model for Double-Entry Accounting](https://stackoverflow.com/q/59432964/484814)**, it has complete explanations and graphics, suitable for developers.



PerformanceDBA


–
[PerformanceDBA](https://stackoverflow.com/users/484814/performancedba "34,014 reputation")



2020-01-07 22:47:41 +00:00

[CommentedJan 7, 2020 at 22:47](https://stackoverflow.com/questions/2494343/database-schema-design-for-a-double-entry-accounting-system#comment105436741_2494343)


[Add a comment](https://stackoverflow.com/questions/2494343/database-schema-design-for-a-double-entry-accounting-system# "Use comments to ask for more information or suggest improvements. Avoid answering questions in comments.") \| [Expand to show all comments on this post](https://stackoverflow.com/questions/2494343/database-schema-design-for-a-double-entry-accounting-system# "Expand to show all comments on this post")

## 2 Answers 2

Sorted by:
[Reset to default](https://stackoverflow.com/questions/2494343/database-schema-design-for-a-double-entry-accounting-system?answertab=scoredesc#tab-top)

Highest score (default)

Trending (recent votes count more)

Date modified (newest first)

Date created (oldest first)


This answer is useful

65

This answer is not useful

Save this answer.

Loading when this answer was accepted…

[Timeline](https://stackoverflow.com/posts/15583160/timeline)

Show activity on this post.

**Create the following tables**

- account
- transaction
- line\_item
- contact (can be a customer a supplier, or an employee).

To keep things simple, we will leave out the account\_type table, contact\_type table, etc.

**Identify the relationships between the tables and set them up**

- a contact can have many transactions, but each transaction can only have one contact (one-to-many relationship)
- an account can have many transactions, and one transaction can affect many accounts; line\_item is the join table between transaction table and account table (a many-to-many relationship)
- a transaction can have many line items, but each line item must relate to one transaction.

We have the following schema (a one-to-many relationship):

```
CONTACT ———< TRANSACTION ———< LINE_ITEM >——— ACCOUNT
```

**Add appropriate fields to each table**

- Contact

  - contactID
  - name
  - addr1
  - addr2
  - city
  - state
  - zip
  - phone
  - fax
  - email
- Transaction

  - transactionID
  - date
  - memo1
  - contactID
  - ref
- Line\_item

  - line\_itemID
  - transactionID
  - accountID
  - amount
  - memo2
- Account

  - accountID
  - account\_name
  - account\_type

**Create as many new transactions as needed**

For example to add a new transaction in the database, add a new record in the transaction table and fill in the fields, select a contact name, enter a date, etc. Then add new child records to the parent transaction record for each account affected. Each transaction record must have at least two child records (in a double-entry bookkeeping system). If I purchased some cheese for $20 cash, add a child record to the transaction record in the child record, select the Cash account and record −20.00 (negative) in the amount field. Add a new child record, select the Groceries account and record 20.00 (positive) in the amount field. The sum of the child records should be zero (i.e., 20.00 − 20.00 = 0.00).

**Create reports in the database based on the data stored in the above tables**

The query to give me all records in the database organized so that transaction line item child records are grouped by account, sorted by date then by transaction ID. Create a calculation field that gives the running total of the amount field in the transaction line\_items records and any other calculation fields you find necessary. If you prefer to show amounts in debit/credit format, create two calculation fields in the database query have one field called debit, and another called credit. In the debit calculation field, enter the formula "if the amount in the amount field from the line\_item table is positive, show the amount, otherwise null". In the credit calculation field, enter the formula "if the amount in the amount field from the line-Item table is negative, show the amount, otherwise null".

Based on this rather simple database design, you can continuously add more fields, tables and reports to add more complexity to your database to track yours or your business finances.

[Share](https://stackoverflow.com/a/15583160)

Share a link to this answer

Copy link [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/ "The current license for this post: CC BY-SA 4.0")

Short permalink to this answer

[Improve this answer](https://stackoverflow.com/posts/15583160/edit "")

Follow



Follow this answer to receive notifications

[edited Aug 15, 2018 at 4:59](https://stackoverflow.com/posts/15583160/revisions "show all edits to this post")

[![Olim Saidov's user avatar](https://www.gravatar.com/avatar/7681eb5c5f0105c8522d32c3b45c2590?s=64&d=identicon&r=PG)](https://stackoverflow.com/users/690085/olim-saidov)

[Olim Saidov](https://stackoverflow.com/users/690085/olim-saidov)

2,85411 gold badge2727 silver badges3333 bronze badges

answered Mar 23, 2013 at 3:55

[![bkire's user avatar](https://www.gravatar.com/avatar/5f7da6790a0e8598890d0f26b3f74e72?s=64&d=identicon&r=PG)](https://stackoverflow.com/users/2201453/bkire)

[bkire](https://stackoverflow.com/users/2201453/bkire)

65155 silver badges22 bronze badges

Sign up to request clarification or add additional context in comments.


## 1 Comment

Add a comment

[![](https://graph.facebook.com/100000716536412/picture?type=large)](https://stackoverflow.com/users/3199351/abdeali-chandanwala)

Abdeali Chandanwala

[Abdeali Chandanwala](https://stackoverflow.com/users/3199351/abdeali-chandanwala) [Over a year ago](https://stackoverflow.com/questions/2494343/database-schema-design-for-a-double-entry-accounting-system#comment109761024_15583160)

Hi bkire, thanks for your answer - liked your answer, is there any update since 2013 ? and can you please state what is account\_type like ? please use a example to explain. Thnks

2020-05-28T07:39:41.44Z+00:00

0

Reply

- Copy link

This answer is useful

9

This answer is not useful

Save this answer.

Loading when this answer was accepted…

[Timeline](https://stackoverflow.com/posts/5525806/timeline)

Show activity on this post.

I figured I might as well take a stab at it. Comments are appreciated – I'll refine the design based on feedback from anyone. I'm going to use SQL Server (2005) T-SQL syntax _for now_, but if anyone is interested in other languages, let me know and I'll add additional examples.

In a [double-entry bookkeeping system](http://en.wikipedia.org/wiki/Double-entry_bookkeeping_system), the basic elements are accounts and transactions. The basic 'theory' is the [accounting equation](http://en.wikipedia.org/wiki/Accounting_equation): Equity = Assets - Liabilities.

Combining the items in the accounting equation and two types of nominal accounts, Income and Expenses, the basic organization of the accounts is simply a forest of nested accounts, the root of the (minimum) five trees being one of: Assets, Liabilities, Equity, Income, and Expenses.

\[I'm researching good SQL designs for hierarchies generally ... I'll update this with specifics later.\]

One interesting hierarchy design is documented in the SQL Team article [More Trees & Hierarchies in SQL](http://www.sqlteam.com/article/more-trees-hierarchies-in-sql).

Every transaction consists of balanced debit and credit amounts. For every transaction, the total of the debit amounts and the total of the credit amounts must be exactly equal. Every debit and credit amount is tied to one account.

\[More to follow ...\]

[Share](https://stackoverflow.com/a/5525806)

Share a link to this answer

Copy link [CC BY-SA 3.0](https://creativecommons.org/licenses/by-sa/3.0/ "The current license for this post: CC BY-SA 3.0")

Short permalink to this answer

[Improve this answer](https://stackoverflow.com/posts/5525806/edit "")

Follow



Follow this answer to receive notifications

[edited Sep 13, 2012 at 8:31](https://stackoverflow.com/posts/5525806/revisions "show all edits to this post")

community wiki


[2 revs, 2 users 96%](https://stackoverflow.com/posts/5525806/revisions "show revision history for this post") [Kenny Evitt](https://stackoverflow.com/users/173497)

## 1 Comment

Add a comment

[![](https://www.gravatar.com/avatar/88cc632c2ca44292d6c8fd6688381402?s=48&d=identicon&r=PG)](https://stackoverflow.com/users/173497/kenny-evitt)

Kenny Evitt

[Kenny Evitt](https://stackoverflow.com/users/173497/kenny-evitt) [Over a year ago](https://stackoverflow.com/questions/2494343/database-schema-design-for-a-double-entry-accounting-system#comment6277000_5525806)

Note that my answer is 'community wiki' – please feel free to edit it yourself.

2011-04-02T20:56:20.25Z+00:00

0

Reply

- Copy link

Start asking to get answers

Find the answer to your question by asking.

[Ask question](https://stackoverflow.com/questions/ask)

Explore related questions

- [database](https://stackoverflow.com/questions/tagged/database "show questions tagged 'database'")
- [database-design](https://stackoverflow.com/questions/tagged/database-design "show questions tagged 'database-design'")
- [accounting](https://stackoverflow.com/questions/tagged/accounting "show questions tagged 'accounting'")

See similar questions with these tags.