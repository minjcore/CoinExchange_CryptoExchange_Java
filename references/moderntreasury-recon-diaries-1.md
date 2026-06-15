[Journal](https://www.moderntreasury.com/resources/journal)

•December 9, 2022

# Recon Diaries, Entry 1: Cash Reconciliation Explained

In the first part of this series, we walk through a cash reconciliation example to show best practices and why it matters for the health of a business.

![Image of Zachary Gardner](https://www.moderntreasury.com/_next/image?url=https%3A%2F%2Fcdn.sanity.io%2Fimages%2F8nmbzj0x%2Fproduction%2F37931594c51e3ffbe8ab5cc62355055e61301af7-500x500.jpg&w=64&q=75)

Zachary Gardner/ PMM

Contents

Explore With AI

Topics

[Behind the Scenes](https://www.moderntreasury.com/resources/behind-the-scenes) [Ledgering](https://www.moderntreasury.com/resources/ledgering) [Reconciliation](https://www.moderntreasury.com/resources/reconciliation)

We’re introducing the Recon Diaries: a series explaining the world of reconciliation and its intricacies.

Today, we’re giving an overview of reconciliation and why it matters. In the weeks to come, we’ll dive into the challenges of reconciliation and introduce you to “the Recon Squad,” the engineering team at Modern Treasury dedicated to tackling them.

## Introduction to Cash Reconciliation

Cash reconciliation, or [bank reconciliation](https://www.moderntreasury.com/learn/bank-reconciliation), is the process of confirming payment accuracy by matching two separate data sources: an internal dataset recording “expected money movement,” and your bank’s dataset capturing “actual money movement.” By comparing, or reconciling, these two datasets against one another, a business can ensure that:

1. All expected payments were executed.
2. No unexpected payments were executed.

Cash reconciliation is important for all companies. Money is the lifeblood of commerce—accounting for that money (i.e., tracking it, attributing it, ensuring no leakage) is a fundamental business responsibility.

For companies that move money on behalf of others, reconciliation is especially important. [Automatic reconciliation](https://www.moderntreasury.com/journal/what-is-automatic-reconciliation) is the bedrock of a strong product and client experience. It promotes trust and transparency by demonstrating that client funds are appropriately managed, tracked, and attributed, and it delivers delight by enabling money movement to be coupled tightly within software workflows.

## Complications with Reconciliation: An Example

Let's illustrate the importance of reconciliation with an example: The University of Modern Treasury (UMT) is a renowned institution for higher learning. The school has a dedicated and involved alumni base that is eager to give back to its alma mater. The school receives thousands of alumni donations a year, amounting to millions of dollars. These gifts help fund the school’s operations and cover the school’s need-blind admissions policy.

The UMT Development Office, an office of 25, oversees all alumni relations and fundraising activity. The development team is responsible for hosting alumni events, soliciting gifts, providing updates to the alumni base, and managing the alumni portal. All this activity trickles down to an annual giving number, which is closely watched by the trustees of the school.

The UMT development team goes to great lengths to provide donors with a seamless, and integrated donor experience. As an example of this: the school offers donors the ability to give across a variety of payment methods, including check, wire, card, bank transfer, stocks, and even Bitcoin.

Offering multiple payment options, the office discovered, delivered a better converting experience; each donor could give in a manner that matched their preferences. However, it also presented significant operational difficulties for the university. One issue stands out. Donors, in some cases, would send funds to UMT via wire without clear documentation, creating a hairy back-end attribution nightmare.

## The Documentation Issue

Specifically, here’s what happens: the back-end operations team sees a wire payment for, let’s say, $2,500 on the UMT bank statement. This reflects a received donation from an alumnus (sample bank statement provided below).

![Received Donation](https://cdn.sanity.io/images/8nmbzj0x/production/655adaebae83ca089f82d11d0a495b67092408ae-1129x191.svg)

Bank statement line showing received donation of $2,500.00.

The problem is that the development office has no corresponding internal entry that recorded the “expected payment” and from which alumni they expected a donation. Without that data, the UMT development team has no way to categorize the gift and correctly attribute it to the appropriate donor.

The process of reconciliation has broken down.

One might wonder why this even matters: _“Don’t many donors give anonymously, anyway?”_ _“Isn’t money fungible?”_ However, to UMT and its alumni, it actually matters a lot. Here are some reasons why:

- **Taxes:** Donations (even anonymous ones) receive a charitable receipt. This is crucial for donors that are looking to get a special tax deduction on their gift.
- **Earmarked Funds:** Many donors earmark their gifts for specific purposes, such as financial aid or athletics. The inability to correctly attribute funds muddles that.

Most importantly from the university’s perspective, the inability to reconcile and attribute an incoming donation disrupts the product experience. It prevents the development office from delivering that “seamless, integrated experience,” which includes:

1. Acknowledgement of the donation and an expression of thanks.
2. Insight into how the funds will be used.
3. Invitation to future community and engagement events.

In practice, when the development office can’t attribute an incoming payment, the team must seek out that “payment expectation” in an attempt to reconcile. They reach out to the donor-facing associates to see if they were expecting a gift, or had received some hint from an alumnus. In some cases, an associate might dig up a cryptic email, or recall a conversation that would help the back-office team establish a match. “ _Here’s an email that I missed—this $2,500 is from Alex Hamilton._”

In sum, the impact of UMT’s inability to reconcile the incoming payment is felt two-fold: both operationally and within the product offering:

1. It creates significant operational work as teams had to investigate missing payments.
2. It also degrades the donor product experience, and quietly undermined faith in UMT as the best place for an alumni’s capital.

Reconciliation matters.

## A Holistic Solution

As these errors became more prevalent, the school instituted a new process to track donations. Front-line associates now explicitly assume responsibility for the gift documentation process (even if it affects the donor experience). When an alumnus mentions a donation, the development associate first books the payment in an internal system, which collects and memorializes the “expected payment.”

Here’s the new workflow:

1. Log into UMT’s internal database (i.e., a ledger).
2. Find the correct alumni, and tie the entry to their profile.
3. Book a “pledge”—a record entry expressing an expected donation.
4. Provide guidance for payment processing—collect payment details (i.e., ACH details) or share instructions for wires.

(Additionally, card transactions—for those paying on the university’s website—also automatically populate this internal database.)

![UMT Ledger](https://cdn.sanity.io/images/8nmbzj0x/production/5f03dfde4f6316ed472e0441b9c014ac2785945f-1269x667.svg)

Ledger of nine UMT donations detailing date, type of payment, name, description, amount, and status.

At UMT, they now say internally: “If it’s not in \[the internal database\], then it doesn’t exist.”

Now, when the back-office team sees a $2,500 wire transaction on the university’s bank statement, the team knows to match that payment with the corresponding “pledge” within their internal system of record.

Alex Hamilton on December 5th. Reconciled.

The “pledge” is then updated to a “gift” in the database, triggering a notification email to the donor.

By creating an internal system that standardizes and aggregates payment expectations across all methods (i.e., no more digging through email inboxes for clues), UMT can more easily attribute and reconcile all payments and provide their donors with a “seamless and integrated” experience.

## Reconciliation Broadly

Zooming out, cash reconciliation is the process of comparing “expected payments” within an internal database against “actual payments” within your bank’s database.

Payments, however, are broad and make up a number of business processes (e.g., accepting donations, collecting invoices, paying vendors, accepting rent etc.). Cash reconciliation, therefore, comes in various flavors and forms. Here are some variations:

- **Accounting / ERP.** Many companies reconcile transactions within their accounting systems or ERPs against their bank statement transactions. This is especially common for companies that are confirming (1) incoming payment receipt for an invoice (i.e., accounts receivable) or (2) outgoing payment confirmation for a purchase order (i.e., accounts payable). Accounting systems and ERPs are often central sources of truth for invoice and purchase order data (1st-party transaction information).
- **Loan Management Systems**. Digital lenders might use loan management systems (LMS) to capture payment information related to specific loan activity (i.e., outstanding balance, principal and interest payments).  For example, if a lender is expecting a servicing payment on December 10th, the lender will record an “expected payment” in their LMS, and then reconcile that expected payment against a corresponding transaction on their bank statement.
- **Property Management Software.** Property managers might use vertical specific ERPs, like Realpage, Appfolio, or Yardi to manage their operations. These systems capture expected rent payments from tenants, which then can be reconciled against bank statements to confirm rent payment receipt.
- **Modern Treasury’s Platform.** Many use-cases will not have suitable, performant databases that companies can utilize as an internal source of truth. These companies, like UMT, will have to build their own. Modern Treasury offers the [ledgering infrastructure](https://www.moderntreasury.com/products/ledgers) to enable companies to build their own flexible database applications so that they can deliver great user experiences, reach time-to-value faster, and scale more efficiently.

## Conclusion

Cash reconciliation is a core financial process that’s integral to company health and continuity, and provides insights into cash flow. At Modern Treasury, we assist with cash reconciliation in a number of ways:

1. We provide best-in-class financial [ledgering infrastructure](https://www.moderntreasury.com/products/ledgers) to help companies build their own scalable databases to centralize “expected payments,” track transactions, and record balances.
2. We integrate directly with [bank partners](https://www.moderntreasury.com/integrations) to provide real-time, automated access to  “actual money movement” within your bank.

Lastly, we provide the software bridge that programmatically links these two datasets. With Modern Treasury, companies can take advantage of automatic reconciliation to help deliver a better product, and simplify and automate their operations.

We’ll get into that more in future posts. In the meantime, if you want to learn more, [reach out to us](https://www.moderntreasury.com/talk-to-us).

Subscribe to our newsletter

Get the latest articles, guides, and insights delivered to your inbox.

Company Email\*

Subscribe

## Authors

![Image of Zachary Gardner](https://www.moderntreasury.com/_next/image?url=https%3A%2F%2Fcdn.sanity.io%2Fimages%2F8nmbzj0x%2Fproduction%2F37931594c51e3ffbe8ab5cc62355055e61301af7-500x500.jpg&w=1080&q=75)

Zachary GardnerPMM

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