[Sitemap](https://medium.com/sitemap/sitemap.xml)

[Open in app](https://play.google.com/store/apps/details?id=com.medium.reader&referrer=utm_source%3DmobileNavBar&source=post_page---top_nav_layout_nav-----------------------------------------)

Sign up

[Sign in](https://medium.com/m/signin?operation=login&redirect=https%3A%2F%2Fmedium.com%2F%40pv.gomes89%2Fhow-to-build-an-accrual-based-credit-ledger-2a13abeae988&source=post_page---top_nav_layout_nav-----------------------global_nav------------------)

[Medium Logo](https://medium.com/?source=post_page---top_nav_layout_nav-----------------------------------------)

Get app

[Write](https://medium.com/m/signin?operation=register&redirect=https%3A%2F%2Fmedium.com%2Fnew-story&source=---top_nav_layout_nav-----------------------new_post_topnav------------------)

[Search](https://medium.com/search?source=post_page---top_nav_layout_nav-----------------------------------------)

Sign up

[Sign in](https://medium.com/m/signin?operation=login&redirect=https%3A%2F%2Fmedium.com%2F%40pv.gomes89%2Fhow-to-build-an-accrual-based-credit-ledger-2a13abeae988&source=post_page---top_nav_layout_nav-----------------------global_nav------------------)

![Unknown user](https://miro.medium.com/v2/resize:fill:32:32/1*dmbNkD5D-u45r44go_cf0g.png)

# How to build an accrual-based credit ledger

[![Paulo Victor Gomes](https://miro.medium.com/v2/resize:fill:32:32/1*4x11PNlNBL3Fkhf0foEIAw.jpeg)](https://medium.com/@pv.gomes89?source=post_page---byline--2a13abeae988---------------------------------------)

[Paulo Victor Gomes](https://medium.com/@pv.gomes89?source=post_page---byline--2a13abeae988---------------------------------------)

Follow

13 min read

·

May 5, 2026

[Listen](https://medium.com/m/signin?actionUrl=https%3A%2F%2Fmedium.com%2Fplans%3Fdimension%3Dpost_audio_button%26postId%3D2a13abeae988&operation=register&redirect=https%3A%2F%2Fmedium.com%2F%40pv.gomes89%2Fhow-to-build-an-accrual-based-credit-ledger-2a13abeae988&source=---header_actions--2a13abeae988---------------------post_audio_button------------------)

Share

Press enter or click to view image in full size

![](https://miro.medium.com/v2/resize:fit:700/1*UMFxn0uv5LiNZPPZfhDThA.png)

Ledgers are the heartbeat of any financial companies, fintech or old school financial. Not the API gateway, not the mobile app, not the underwriting model.

**The ledger**

![](https://miro.medium.com/v2/resize:fit:498/0*bozuudHgbeE4arFo.gif)

Banks have known this for centuries. Fintechs sometimes need to rediscover it the hard way, or they learn during the way.

On fintech industry **Revolut** runs multi-currency, multi-product financial infrastructure across 35+ countries and counting 🚀…

**Stripe** moves money and extends credit infrastructure for millions of businesses, sometimes you don’t even know, but stripe is there.

![](https://miro.medium.com/v2/resize:fit:480/0*kuo0fHIgga-jnKTp.gif)

**Nubank** serves more than 130 million customers 🤯 and had to make credit work at Latin American scale, starting with high brazilian, my beloved country complexity and competitive bank system. Seriously, Nubank is just unique.

![](https://miro.medium.com/v2/resize:fit:480/0*O_k-pzf0UAw6t5mP.gif)

**Chime** built credit-builder products on top of a US neobank model, how to compete? Just use them 😌.

**Klarna** and **Affirm** made deferred payments mainstream, which also means ledger complexity at global BNPL scale, they didn’t change only finance, they scale and enable retail growth.

![](https://miro.medium.com/v2/resize:fit:480/0*tsxTU1K7PkmVNAgH.gif)

At this point you got me right? Different products, different geographies. But you know what they have in common? Their accounting HAVE to be in a good shape. So they share the same pressure: **move fast without corrupting the books.**

That is the uncomfortable thing about ledgers. The product team wants speed. The regulator wants auditability(is this a word?). Finance wants reconciliation. Engineering wants evolvability and not creating technical debts.

Customers just want their balance to be correct.

![](https://miro.medium.com/v2/resize:fit:360/0*ut5YCL5nrVVxcnXh.gif)

And ledger decisions made in year one become the ceiling in year five. The model that worked for 10,000 users becomes the bottleneck at 10 million, imagine 100 million? **(Hello Nubank!!!)**.

The cron job that looked pragmatic becomes the reason nobody trusts end-of-day balances.

The table that was “good enough for MVP” becomes the thing auditors stare at for three months.

The famous sharding infrastructure worked well till some millions, later, sharding and infrastructure scaling is not necessarily the problem.

The Synapse collapse in 2024 was a brutal reminder of this. When money movement companies cannot clearly reconcile who owns what, the failure is not only technical. It becomes customer harm. Millions of dollars can end up disputed, delayed, or unreconciled because the ledger was not strong enough to be the source of truth.

This article is about designing a credit ledger before that pain arrives.

One important boundary: this is intentionally about a **single-geography credit ledger**. No multi-country posting rules, no cross-currency accounting, no global consistency model. Those are a different beast. I will cover them in the next article.

## The evolution most fintechs go through

Most credit products do not start with a beautiful accrual ledger. They start with a product requirement:

> _“We need to charge interest.”_

Then a team adds a job. Later, events. Later, after enough incidents, a proper accrual model.

That evolution is normal. The trick is not pretending the first version is the final one.

## Stage 1: the job-based ledger

The simplest ledger is a scheduled job.

Every hour, every night, or every billing cycle, a cron job scans accounts and creates bookkeeping movements:

```
00:00 ───────────────────────────────────────────────► time
```

```
10:03  purchase happens
11:18  payment happens
14:42  fee is incurred23:59  ledger_job runs
      ├─ posts purchase movement
      ├─ posts payment movement
      └─ posts fee movement
```

This works surprisingly well at small scale. It is easy to reason about. You can query the database, calculate what changed, and insert rows into a ledger table. Many early fintech systems start here because it lets the team ship.

The problem is that job-based ledgers mix three different concepts:

1. when something happened,
2. when the system noticed it happened,
3. when accounting was posted.

At low volume, the difference is invisible. At scale, it becomes the whole problem.

A customer pays at 11:18, but the ledger does not reflect it until 23:59. A fee is incurred at 14:42, but the job fails and retries at 01:10. Two jobs overlap and double-post. A reprocessing script tries to fix yesterday but accidentally changes today.

The core issues are predictable:

- **Delayed consistency**: balances are stale by design.
- **Race conditions**: two jobs can process the same account or overlapping time windows.
- **Poor auditability**: “what was the balance at 14:00?” becomes hard if entries are posted later without the correct effective date.
- **Painful reprocessing**: fixing a bad job means deciding whether to delete, update, or overwrite ledger rows. All three are dangerous.
- **Scaling by brute force**: when volume increases, you make the job faster. Eventually the job becomes a monster.

The job-based ledger is not evil. It is just a starting point. The mistake is letting it become the foundation of a serious credit platform.

## Stage 2: the event-based ledger

The next step is to post ledger entries when business events happen.

A payment is received. A charge is applied. A late fee is created. A statement closes. Instead of waiting for a batch job, the system reacts immediately.

```
Domain event                         Ledger reaction
────────────                         ───────────────
PaymentReceived ───────────────────► post payment entry
PurchaseAuthorized ────────────────► post authorization entry
FeeIncurred ───────────────────────► post fee entry
StatementClosed ───────────────────► post billing entries
```

This is much better. The ledger is closer to real time, and the accounting logic sits near the business event that created it. Martin Fowler’s accounting patterns make this connection explicit: domain events and accounting entries should be linked, because accounting is a record of business reality, not an isolated reporting table.

But event-based does not automatically mean robust.

A common implementation looks like this:

```
API request
  ├─ update product state
  ├─ publish event
  └─ write ledger entry
```

Or worse:

```
API request
  ├─ update product state
  ├─ call ledger service synchronously
  └─ return success to customer
```

Now the ledger is coupled to the transaction path. If the ledger service is slow, the product is slow. If the event publish fails after the product state changes, the books are wrong. If the same event is delivered twice, you double-post. If events arrive out of order, the ledger reflects a reality that never existed.

```
┌──────────────┐
        │ Credit API   │
        └──────┬───────┘
               │ emits
               ▼
        ┌──────────────┐        tight coupling risk
        │ Domain event │ ──────────────────────────┐
        └──────┬───────┘                           │
               │ consumed                           ▼
               ▼                            ┌──────────────┐
        ┌──────────────┐                    │ User request │
        │ Ledger write │                    │ latency path │
        └──────────────┘                    └──────────────┘
```

Event-based ledgering is the middle ground. It is a necessary evolution from cron jobs, but it still needs three things to become safe:

- immutable events,
- idempotent processing,
- deterministic replay.

Without those, you do not have a ledger architecture. You have a distributed system hoping the happy path stays happy.

## Stage 3: the accrual-based ledger

A credit ledger should not only record when cash moves. It should record when value is **earned or incurred**.

That is the core idea of accrual accounting.

Interest is earned daily. Fees are incurred when the customer triggers them. A settlement is a separate event from the revenue already earned. Billing is a presentation and collection mechanism, not the moment the economics magically appear.

For a credit product, this distinction matters a lot.

If a customer carries a balance for 20 days, the platform is earning interest across those 20 days. Waiting until the statement closes to create one giant interest entry may look simpler, but it hides the actual economics. It also makes “balance as of” queries, partial reversals, mid-cycle adjustments, and audit trails harder than they need to be.

An accrual-based credit ledger treats accounting like this:

```
Day 1      Day 2      Day 3      ...      Cycle close        Payment
│          │          │                    │                  │
▼          ▼          ▼                    ▼                  ▼
Accrue     Accrue     Accrue               Bill accrued       Settle cash
interest   interest   interest             interest           receivable
```

```
Accounting view:
- daily: debit interest receivable, credit interest income
- cycle close: move accrued amounts into statement balance
- payment: debit cash/settlement account, credit customer receivable
```

This is how accounting actually works. Fintechs that skip it often end up retrofitting it later, usually after finance, risk, or regulators start asking questions the old model cannot answer.

## Double-entry is the checksum

At the center of this design is double-entry bookkeeping.

Every ledger entry has two sides. One account is debited. Another account is credited. The total must balance.

For a daily interest accrual, the entry might be:

```
Debit:  Interest receivable      1.25 USD
Credit: Interest income          1.25 USD
```

The customer owes more. The company earned revenue. Two sides of the same fact.

This is not accounting ceremony. It is a system invariant. If debits and credits do not net to zero, the ledger rejects the entry. That gives you a built-in checksum for every financial movement.

A single-sided balance table can tell you what you think a customer owes. A double-entry ledger can tell you whether the books still make sense.

That difference is everything.

## The ledger should be append-only

A serious ledger does not update old entries. It does not delete them either.

It appends.

If you posted the wrong amount, you post a reversal and then a corrected entry. The history remains sacred.

```
entry_id   type        debit_account          credit_account       amount
────────   ────────    ─────────────────      ─────────────────    ──────
E1         ACCRUAL     interest_receivable    interest_income      10.00
E2         REVERSAL    interest_income        interest_receivable  10.00
E3         ACCRUAL     interest_receivable    interest_income       9.50
```

That pattern feels annoying when you are moving fast. It is also the reason you can answer audit questions later.

Why was the customer balance different yesterday? Read the log.

What did the system believe at 10:35? Query entries with `effective_date <= 10:35` and `created_at <= investigation_cutoff`.

Can we reproduce last month’s statement? Replay the events and ledger entries as they existed then.

This is where Mettle’s Write Once Double Entry pattern is a great real-world reference. WODE is basically the grown-up version of what many fintech teams eventually learn: write once, balance always, correct by appending, and make the ledger boring enough to trust.

## Event sourcing is the backbone

Accrual ledgering and event sourcing fit naturally together.

The product emits immutable domain events:

- `PurchasePosted`
- `PaymentSettled`
- `InterestAccrued`
- `LateFeeIncurred`
- `StatementClosed`
- `ChargeReversed`

The ledger consumes those events and produces immutable accounting entries. Each entry points back to the event that caused it.

```
┌─────────────────┐
│ Domain command  │
│ "apply payment"│
└────────┬────────┘
         ▼
┌─────────────────┐
│ Domain event    │
│ PaymentSettled  │
└────────┬────────┘
         ▼
┌─────────────────┐
│ Accrual /       │
│ posting rules   │
└────────┬────────┘
         ▼
┌────────────────────────────────────────────┐
│ Immutable double-entry ledger entries       │
│ source_event_id = PaymentSettled.event_id   │
└────────────────────────────────────────────┘
```

This gives you a clean rule: product state is derived from product events, and accounting state is derived from accounting entries produced from those events.

## Get Paulo Victor Gomes’s stories in your inbox

Join Medium for free to get updates from this writer.

Subscribe

Subscribe

Remember me for faster sign in

If you replay all events through the same posting rules, you should get the same ledger. If you do not, either the rules are not deterministic or the event log is incomplete. Both are bugs worth finding early.

Event sourcing also changes how you think about failures. A failed projection is not a data-loss incident if the event is still there. You fix the processor, replay, and rebuild. That is the difference between a recoverable system and a spreadsheet with APIs.

## Eventual consistency is fine. Wrong money is not.

A common objection is: “But if the ledger is event-driven, it may be slightly behind.”

Yes. That is usually fine for credit.

A credit ledger being 200 milliseconds behind the authorization system is not a disaster. A ledger double-posting interest is. A missing payment is. A fee applied before the balance it depends on is. An out-of-order reversal that leaves the customer owing money they do not owe is.

The trade-off is not strong consistency versus chaos. The trade-off is where you need immediate consistency and where you need deterministic eventual consistency.

For most credit ledgers:

- the customer-facing authorization path may need immediate answers,
- the ledger may process asynchronously,
- the processing must be idempotent,
- events must have ordering guarantees per account or credit line,
- every ledger entry must carry an idempotency key,
- replay must not create duplicate entries.

An idempotency key can be simple and powerful:

```
interest-accrual:{credit_account_id}:{accrual_date}:{rate_version}
```

If the processor retries, the ledger sees the same key and refuses to post the same economic fact twice.

That is the difference between eventual consistency and eventual regret.

## A practical ledger entry shape

You can make the schema more sophisticated later, but a useful starting point looks like this:

```
ledger_entries(
  entry_id,
  type,
  debit_account,
  credit_account,
  amount,
  currency,
  effective_date,
  created_at,
  idempotency_key,
  source_event_id
)
```

A few fields matter more than they look:

- `effective_date` is when the economic event applies.
- `created_at` is when the system recorded it.
- `idempotency_key` prevents duplicate posting.
- `source_event_id` links accounting back to business reality.
- `currency` should be explicit even in a single-geography system. You will thank yourself later.

For a serious platform, you will also add account types, journal batches, metadata, posting rule versions, actor/service identifiers, and partition keys. But do not lose the core shape: debit, credit, amount, currency, time, idempotency, source.

## Daily interest accrual pseudocode

Here is the kind of logic I would expect in a first version. Not production code, but the shape is right.

```
for credit_account in active_credit_accounts:
    accrual_date = today_in_product_timezone()
```

```
    balance = principal_balance_as_of(
        credit_account.id,
        accrual_date.start
    )    if balance <= 0:
        continue    rate = interest_rate_for(
        credit_account.id,
        accrual_date
    )    daily_interest = round_money(
        balance * rate.annual_percentage / days_in_year(accrual_date)
    )    if daily_interest == 0:
        continue    idempotency_key = "interest-accrual:" +
        credit_account.id + ":" +
        accrual_date + ":" +
        rate.version    post_double_entry(
        type = "INTEREST_ACCRUAL",
        debit_account = account("interest_receivable", credit_account.id),
        credit_account = account("interest_income"),
        amount = daily_interest,
        currency = credit_account.currency,
        effective_date = accrual_date,
        source_event_id = current_accrual_event.id,
        idempotency_key = idempotency_key
    )
```

There are many details hidden here: day-count convention, rounding policy, grace periods, promotional rates, delinquency state, local regulation, charge-off treatment. Those are product and accounting rules, not reasons to avoid the accrual model.

Actually, they are reasons to prefer it. Complex rules are easier to manage when every economic fact has a precise entry and a source event.

## Correction entries, not edits

Imagine the system accrued 10.00 USD of interest, but later you discover the correct amount was 9.50 USD because a payment was effective one day earlier.

Do not update the original row.

Post this:

```
Original:
  Dr interest_receivable  10.00
  Cr interest_income      10.00
```

```
Reversal:
  Dr interest_income      10.00
  Cr interest_receivable  10.00Corrected:
  Dr interest_receivable   9.50
  Cr interest_income       9.50
```

Now the ledger tells the truth twice:

1. what the system originally believed,
2. how that belief was corrected.

That second part matters. A ledger that only stores the latest truth is not an audit trail. It is a mutable cache.

## Where teams usually get hurt

The hardest part of building a credit ledger is not the table design. It is resisting shortcuts that feel harmless.

Shortcuts like:

- storing only customer balances instead of entries,
- treating billing as the moment revenue is earned,
- letting jobs update historical rows,
- using timestamps without separating `effective_date` from `created_at`,
- allowing ledger writes without source events,
- ignoring idempotency because “the event only fires once,”
- building reconciliation after launch instead of as part of the ledger.

Every one of those shortcuts is understandable. Every one becomes expensive.

The better architecture is boring:

```
Domain events are immutable.
Posting rules are deterministic.
Ledger entries are double-entry.
Ledger writes are append-only.
Corrections are reversals.
Consumers are eventually consistent.
Reconciliation is continuous.
```

That is not overengineering. That is the minimum foundation for money.

## Build versus buy

There is a reason ledger-as-a-service companies and open-source ledgers are getting attention. Formance and Blnk are examples of the community moving toward purpose-built ledger infrastructure instead of every fintech reinventing the same accounting core. Temporal’s work on high-performance ledger patterns also points in the same direction: reliability, replayability, and operational resilience are not optional features.

My opinion: most fintechs should not casually build a ledger from scratch. If the ledger is not a core differentiator, buying or adopting a proven ledger engine is rational.

But credit platforms often have enough product-specific accounting behavior that the team still needs to deeply understand the model. Even if you buy the ledger infrastructure, you still own the posting rules. You still own correctness.

A vendor can provide the engine. It cannot decide your economics.

## The simple standard

A good credit ledger should let you answer these questions without panic:

- What does this customer owe right now?
- What did they owe at the end of last month?
- Which event created this entry?
- Did every debit have a matching credit?
- Can we reverse this without deleting history?
- Can we replay the ledger from events?
- Can finance reconcile cash, receivables, income, fees, and adjustments?
- Can auditors see both the original mistake and the correction?

If the answer is no, the platform is not ready for scale. It may still work as a product. It may even grow quickly. But the ledger is already putting a ceiling on the company.

That ceiling always gets lower as volume grows.

## Final thought…

The right ledger architecture will not make your fintech move slower. It will let you keep moving after the product becomes serious.

Job-based ledgers help you start. Event-based ledgers help you react. Accrual-based ledgers help you tell the economic truth.

For credit, that truth matters every day interest is earned, every time a fee is incurred, every time a customer pays, and every time finance needs to close the books.

This article deliberately stayed inside a single-geography model. Multi-geography ledgers, currency conversion at the ledger layer, local accounting rules, and global consistency are out of scope here.

That is the next article… I’m tired…

![](https://miro.medium.com/v2/resize:fit:500/0*zguoejYWshFa3rLO.gif)

## References

- My experience 👨🏻‍💻🇧🇷😀
- The downfall of Synapse [yahoo finance](https://finance.yahoo.com/personal-finance/banking/article/synapse-bankruptcy-fintech-safety-183845965.html)
- Martin Fowler, [Accounting Narrative](https://martinfowler.com/eaaDev/AccountingNarrative.html)
- Microsoft Azure Architecture Center, [Event Sourcing pattern](https://learn.microsoft.com/en-us/azure/architecture/patterns/event-sourcing)
- Mettle, [Innovation at Mettle: Double Entry and Event Sourcing](https://www.mettle.co.uk/blog/innovation-at-mettle-double-entry-and-event-sourcing)
- Temporal, [Designing high-performance financial ledgers with Temporal](https://temporal.io/blog/designing-high-performance-financial-ledgers-with-temporal)
- Apideck, [Money movement infrastructure: fintech ledger as a service](https://www.apideck.com/blog/money-movement-infrastructure-fintech-ledger-as-a-service)
- Martin Fowler, [Accounting Entry](https://martinfowler.com/eaaDev/AccountingEntry.html)
- Formance, [Open-source programmable ledger](https://github.com/formancehq/ledger)
- Blnk Finance, [Open-source immutable ledger](https://www.blnkfinance.com/)

[Fintech](https://medium.com/tag/fintech?source=post_page-----2a13abeae988---------------------------------------)

[Ledger](https://medium.com/tag/ledger?source=post_page-----2a13abeae988---------------------------------------)

[Bookkeeping](https://medium.com/tag/bookkeeping?source=post_page-----2a13abeae988---------------------------------------)

[Platform](https://medium.com/tag/platform?source=post_page-----2a13abeae988---------------------------------------)

[Software Engineering](https://medium.com/tag/software-engineering?source=post_page-----2a13abeae988---------------------------------------)

[![Paulo Victor Gomes](https://miro.medium.com/v2/resize:fill:48:48/1*4x11PNlNBL3Fkhf0foEIAw.jpeg)](https://medium.com/@pv.gomes89?source=post_page---post_author_info--2a13abeae988---------------------------------------)

[![Paulo Victor Gomes](https://miro.medium.com/v2/resize:fill:64:64/1*4x11PNlNBL3Fkhf0foEIAw.jpeg)](https://medium.com/@pv.gomes89?source=post_page---post_author_info--2a13abeae988---------------------------------------)

Follow

[**Written by Paulo Victor Gomes**](https://medium.com/@pv.gomes89?source=post_page---post_author_info--2a13abeae988---------------------------------------)

[25 followers](https://medium.com/@pv.gomes89/followers?source=post_page---post_author_info--2a13abeae988---------------------------------------)

· [12 following](https://medium.com/@pv.gomes89/following?source=post_page---post_author_info--2a13abeae988---------------------------------------)

Engineering Manager by profession, software engineer by passion

Follow

[Help](https://help.medium.com/hc/en-us?source=post_page-----2a13abeae988---------------------------------------)

[Status](https://status.medium.com/?source=post_page-----2a13abeae988---------------------------------------)

[About](https://medium.com/about?autoplay=1&source=post_page-----2a13abeae988---------------------------------------)

[Careers](https://medium.com/jobs-at-medium/work-at-medium-959d1a85284e?source=post_page-----2a13abeae988---------------------------------------)

[Press](mailto:pressinquiries@medium.com)

[Blog](https://blog.medium.com/?source=post_page-----2a13abeae988---------------------------------------)

[Store](https://medium.com/store)

[Privacy](https://policy.medium.com/medium-privacy-policy-f03bf92035c9?source=post_page-----2a13abeae988---------------------------------------)

[Rules](https://policy.medium.com/medium-rules-30e5502c4eb4?source=post_page-----2a13abeae988---------------------------------------)

[Terms](https://policy.medium.com/medium-terms-of-service-9db0094a1e0f?source=post_page-----2a13abeae988---------------------------------------)

[Text to speech](https://speechify.com/medium?source=post_page-----2a13abeae988---------------------------------------)

reCAPTCHA

Recaptcha requires verification.

protected by **reCAPTCHA**