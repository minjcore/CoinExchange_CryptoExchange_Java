[Journal](https://www.moderntreasury.com/resources/journal)

•October 24, 2023

# Reconciliation is a Knapsack Problem

Yesterday, we announced adding AI to our recon engine. Today, we're underscoring how reconciliation is a pain point by delving into the math.

![Image of Sean Bolton](https://www.moderntreasury.com/_next/image?url=https%3A%2F%2Fcdn.sanity.io%2Fimages%2F8nmbzj0x%2Fproduction%2F1cccdf4ec66a4ba1414248fc815a88ef2ef3e07a-4061x4061.jpg&w=64&q=75)

Sean Bolton/ Engineering

Explore With AI

Topics

[Behind the Scenes](https://www.moderntreasury.com/resources/behind-the-scenes) [Reconciliation](https://www.moderntreasury.com/resources/reconciliation)

Certain workflows in transaction reconciliation are well-suited to AI, while others are math problems well-suited to deterministic [engines.](https://www.moderntreasury.com/journal/reconciliation-is-a-knapsack-problem#c38d26b03013)

Imagine we have 10 identical $1 payments that we need to reconcile. Typically, on a bank statement, we would see some of those batched together, and they would appear something like this:

| Transactions |
| --- |
| $3 |
| $2 |
| $1 |
| $1 |
| $1 |
| $1 |
| $1 |

This totals $10, which is what we were expecting. Now, we need to match our business payments to these transactions.

Imagine our payments are tagged A through K (omitting “I” for readability). So, one possible solution to our reconciliation problem is this:

| Transactions | Payment Tags |
| --- | --- |
| $3 | ABC |
| $2 | DE |
| $1 | F |
| $1 | G |
| $1 | H |
| $1 | J |
| $1 | K |

The question is, _how many other possible solutions are there?_ If we solve the batches individually, we are selecting from a list of 10 items to put three into our “solution,” which follows this formula:

![Formula for possible solutions](https://cdn.sanity.io/images/8nmbzj0x/production/e8f037f4591b8e1dc4910c8a809e92b515f9943c-816x325.svg)

Formula for possible solutions

And equals:

![applied formula](https://cdn.sanity.io/images/8nmbzj0x/production/5111d0c94ecf753f221934ce43408f6426fb42a8-816x325.svg)

Applied formula for three of 10 $1 payments

Once that’s solved, we do the same with the $2 batch.

![applied formula 2](https://cdn.sanity.io/images/8nmbzj0x/production/304e6a895e0c868fb832d00d1a373a13b2a55a15-816x325.svg)

Applied formula for two of 10 $1 payments

And finally, we have to match the remaining five transactions. That’s an easier problem to manage, since it’s one-to-one matching, so the number of possibilities is:

![applied formula 3](https://cdn.sanity.io/images/8nmbzj0x/production/639d2549ff55d24d98454771fe09c40e9b08009a-816x325.svg)

Applied formula for the remaining $1 payments

The total number of possibilities—if you’re trying to solve all of this at once, as we are—becomes:

![total possibilities](https://cdn.sanity.io/images/8nmbzj0x/production/be1d05eaca84424527d45e63c3652586437b1b10-816x325.svg)

Total number of possible matches

That’s a lot of possible solutions. In Computer Science terms, this type of problem is a specific variant of the [Knapsack problem](https://developers.google.com/optimization/pack/knapsack), where each item has the same weight. In the example I’ve been describing, the knapsacks are the bank statement transactions, and the items are the equally weighted $1 payments. It’s a combinatorial optimization problem for a computer.

This is why, in most cash reconciliation or audit environments, teams give up on 1:1 transaction matching and opt instead to ensure the totals are equal—because crunching 302,400 options just isn’t worth it.

But that’s what computers are good at! Reconciliation engines, like the one we’ve built at Modern Treasury, have seen a lot of transactions, and every time they see another, it adds to their ability to match. These engines are entirely deterministic, picking up on IDs and data en masse, allowing them to reconcile transactions with 100% confidence and repeatability. The consequence of picking the wrong match in finance is too high to use a probabilistic approach.

But of course, there are edge cases.

So imagine instead of 10 transactions, your business is processing one thousand $1 transactions per day, and you run those using a recon engine like Modern Treasury. Let’s say 99% of those payments reconcile automatically with software. But those pesky ten would still remain, and though 10 out of 1000 doesn’t seem like that many, based on the math we just learned, it’s actually a lot. It’s a hard problem to solve.

This is where AI shines. Instead of trying to brute-force compute the way to a correct answer, you take in a bunch of real-world observations to train a model. It’s substituting abstract, mathematical determinism for a probabilistic model that is tuned to what our system is encountering in the real world at scale.

When you operate in money, the matches have to be 100% accurate. Therefore, the responsible way to use AI is to build it into workflows that humans oversee. It’s the combination of deterministic methods and AI that is a superpower against reconciliation as a knapsack problem. In our example, a deterministic reconciliation engine would operate in real-time for the 990 payments and near-real-time for the AI-suggested, human-approved 10 payments.

Interestingly, as we move into the new era of payments, where more money moves in real-time and settles instantly, the reconciliation challenge increases in multiple ways—first, speed. Teams running reconciliation processes need to find transaction matches, or knapsack objects, in real-time, whether that’s Sunday at 2am or the middle of Thursday afternoon.

Second, math. If, instead of showing up in a few batches, our payments arrive one by one, then we need to match this way:

To which there are 10! answers, or 3,628,800 solutions. That’s too much to brute force. Luckily, these new payment rails, such as RTP and FedNow, include more remittance information to disambiguate payments as they come in. Companies will need to implement software to take advantage of these types of remittance information and not end up in a 3.6M solution quagmire.

| Non-batch transaction matching |
| --- |
| 1 - A |
| 2 - B |
| 3 - C |
| 4 - D |
| 5 - E |
| 6 - F |
| 7 - G |
| 8 - H |
| 9 - J |
| 10 - K |

Here’s an illustrative example. Imagine we are reconciling transactions for something related to real estate, where physical address is a regular component of the remittance information we receive in these real-time payments. If the physical address is 101 South State Street, it may show up in the text in multiple ways:

![State St Examples](https://cdn.sanity.io/images/8nmbzj0x/production/bd6536bae250324d84b5b07d2bff0c281a7f37a7-825x882.svg)

The possible ways the address can appear within the remittance information

While it may be hard to deterministically model the three scenarios shown above, this is the type of thing that language models can do very easily.

Financial workflows are some of the best places to implement AI responsibly. The data is vast, and the solution space is even vaster, but the data is structured in a way that models can take advantage of.

If you have these kinds of high-volume reconciliation challenges, [reach out to us](https://www.moderntreasury.com/talk-to-us). Or send me a note if you want to nerd out on AI in money.

Subscribe to our newsletter

Get the latest articles, guides, and insights delivered to your inbox.

Company Email\*

Subscribe

## Authors

![Image of Sean Bolton](https://www.moderntreasury.com/_next/image?url=https%3A%2F%2Fcdn.sanity.io%2Fimages%2F8nmbzj0x%2Fproduction%2F1cccdf4ec66a4ba1414248fc815a88ef2ef3e07a-4061x4061.jpg&w=3840&q=75)

Sean BoltonEngineering

Related

## Behind the Scenes

[View topic →](https://www.moderntreasury.com/resources/behind-the-scenes)

[![Image for Why Are You Looking for a Different PSP? ](https://www.moderntreasury.com/_next/image?url=https%3A%2F%2Fcdn.sanity.io%2Fimages%2F8nmbzj0x%2Fproduction%2F8289fdf26202b26305c7a99fa4089bb039d8fad8-1920x1080.png&w=3840&q=75)](https://www.moderntreasury.com/resources/videos/why-are-you-looking-for-a-different-psp)

[Videos](https://www.moderntreasury.com/resources/videos) [Why Are You Looking for a Different PSP?](https://www.moderntreasury.com/resources/videos/why-are-you-looking-for-a-different-psp)

[![Image for Driving Enterprise Innovation: Building the Future of Financial Infrastructure](https://www.moderntreasury.com/_next/image?url=https%3A%2F%2Fcdn.sanity.io%2Fimages%2F8nmbzj0x%2Fproduction%2F21fd32f6ad5806799d435ec96b21540838a2d409-1600x900.png&w=3840&q=75)](https://www.moderntreasury.com/resources/videos/driving-enterprise-innovation)

[Videos](https://www.moderntreasury.com/resources/videos) [Driving Enterprise Innovation: Building the Future of Financial Infrastructure](https://www.moderntreasury.com/resources/videos/driving-enterprise-innovation)

[![Image for Streamline Your Workflows and Make Better Business Decisions](https://www.moderntreasury.com/_next/image?url=https%3A%2F%2Fcdn.sanity.io%2Fimages%2F8nmbzj0x%2Fproduction%2F8c8a44b7ba860a8bab4ee9e73a36bf0759d44c92-1600x900.png&w=3840&q=75)](https://www.moderntreasury.com/resources/videos/streamline-workflows)

[Videos](https://www.moderntreasury.com/resources/videos) [Streamline Your Workflows and Make Better Business Decisions](https://www.moderntreasury.com/resources/videos/streamline-workflows)

[![Image for Seven Years and $400 Billion: What Payments Look Like at Scale](https://www.moderntreasury.com/_next/image?url=https%3A%2F%2Fcdn.sanity.io%2Fimages%2F8nmbzj0x%2Fproduction%2F6b038c61a9eac839c5e7e07128d9a950b70b9369-1920x1080.png&w=3840&q=75)](https://www.moderntreasury.com/journal/seven-years-and-usd400-billion-what-payments-look-like-at-scale)

[Journal](https://www.moderntreasury.com/resources/journal) [Seven Years and $400 Billion: What Payments Look Like at Scale](https://www.moderntreasury.com/journal/seven-years-and-usd400-billion-what-payments-look-like-at-scale)

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