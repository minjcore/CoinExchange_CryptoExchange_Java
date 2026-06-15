\> ## Documentation Index
\> Fetch the complete documentation index at: https://docs.blnkfinance.com/llms.txt
\> Use this file to discover all available pages before exploring further.

\# Understanding the Double-Entry Principle

\> An engineer's guide to the Double Entry principle with Blnk.

export const CtaCallout = props => {
 const {title, buttonLabel, href, trackingEvent, buttonTarget, rel = "noopener noreferrer", children} = props;
 const handleCtaClick = () => {
 if (typeof window === "undefined" \|\| !trackingEvent) {
 return;
 }
 try {
 window.dispatchEvent(new CustomEvent("blnk:docs-cta", {
 detail: {
 name: trackingEvent,
 href
 }
 }));
 } catch {}
 try {
 window.posthog?.capture?.(trackingEvent, {
 href
 });
 } catch {}
 const gaPayload = {
 cta\_href: href
 };
 try {
 window.gtag?.("event", trackingEvent, gaPayload);
 } catch {}
 try {
 window.dataLayer = window.dataLayer \|\| \[\];
 window.dataLayer.push({
 event: trackingEvent,
 ...gaPayload
 });
 } catch {}
 };
 const isExternal = typeof href === "string" && (/^https?:\\/\\//i).test(href);
 const target = buttonTarget ?? (isExternal ? "\_blank" : undefined);
 const linkRel = isExternal ? rel : undefined;
 return

{title ?

{title}


: null}


{children}


[{buttonLabel}\\
\\
→](https://docs.blnkfinance.com/guides/%7Bhref%7D)

;
};

If you've read any part of the Blnk documentation, you must have heard about the Double Entry Accounting principle.

It is a big deal in the business world of accounting today. Every business, small, medium, or large, relies on this simple principle to do accurate bookkeeping and reconciliation of their books.

For financial applications, it means keeping the books of an organization across multiple nodes in your system. As a software engineer or technical founder, this can be very new territory for you, and it is even more daunting when you have to build a financial application. Don't worry! Thousands of others like you across the world also face this problem.

In this guide, we're going to learn all about the basics of bookkeeping and the Double Entry principle with Blnk. To get started, please \[install Blnk on your local machine\](/home/install).

\\*\\*\\*

\## The Double Entry Accounting principle

The Double Entry Accounting principle states that for every entry into an account, there needs to be a corresponding and opposite entry into a different account.

Only two types of entries can happen in an account — a credit and a debit. When an account is credited, money is added to the account, and when it is debited, money is removed from the account. So the principle states that if there's a credit entry in one account, there must be an equal debit entry in another account.

\> Another way to visualize the Double Entry principle is that for every transaction, there must be a source and a destination.

\\*\\*\\*

\## Part 1: The Basics

There are two rules to the double-entry principle:

1\. Every financial category in your organization is represented by an account.
2\. Every financial transaction can be modeled as a "transfer" between accounts.

For example, let's say User A has \\$5,000 in their account and they pay the company \\$3,000. If we tracked only the company's accounts, we see only a \\$3,000 credit with no information on where it came from, and that's not true — it moved from User A to the company's account.

So the records look like:

\| \| User A \| Company \|
\| \-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\- \| :---------: \| :---------: \|
\| Starting balance \| \\$5,000 \| \\$0 \|
\| Invoice paid \| –\\$3,000 \| \|
\| \| \| +\\$3,000 \|
\| Ending balance \| \*\*\\$2,000\*\* \| \*\*\\$3,000\*\* \|

The payment is entered twice (hence, "double" entry); it is removed from User A and added to the company account. The amount inputted on both accounts must always be equal, so that no value is lost — it's just been moved around.

 \*\*Why is it important for the value to be equal?\*\*

 Because the double entry aims to be the source of truth for your financial records. When you say you received \\$3,000, the assumption is that someone else paid \\$3,000 to you. If you received \\$3,000 despite being paid \\$4,000, it becomes a case of fraud and dishonesty which is terrible for any business.

\### Let's build it with Blnk

Let's model this on our Blnk server.

 First, we create a new ledger to group our balances in, and we'll name it "User Balances".

 To do this call the endpoint URL, \`/ledgers\`, and pass the following in its request body:


 \`\`\`bash cURL wrap theme={"system"}
 curl -X POST "http://YOUR\_BLNK\_INSTANCE\_URL/ledgers" \
 -H "X-blnk-key: " \
 -H "Content-Type: application/json" \
 -d '{
 "name": "User balances",
 "meta\_data": {
 "project\_owner": "Blnk Finance"
 }
 }'
 \`\`\`

 \`\`\`javascript TypeScript wrap theme={"system"}
 const response = await blnk.Ledgers.create({
 name: 'User balances',
 meta\_data: { project\_owner: 'Blnk Finance' },
 });
 \`\`\`

 \`\`\`go Go wrap theme={"system"}
 ledger, resp, err := client.Ledger.Create(blnkgo.CreateLedgerRequest{
 Name: "User balances",
 MetaData: map\[string\]interface{}{
 "project\_owner": "Blnk Finance",
 },
 })
 \`\`\`


 \`\`\`json Response theme={"system"}
 {
 "ledger\_id": "ldg\_073f7ffe-9dfd-42ce-aa50-d1dca1788adc",
 "name": "User balances",
 "created\_at": "2024-12-21T01:36:46.997063436Z"
 }
 \`\`\`

 Copy the \`ledger\_id\` from the response. You'll need it for the next step.

 Next, create a balance for the user.

 To do this, call the endpoint URL, \`/balances\`, and pass the following in its request body:


 \`\`\`bash cURL wrap theme={"system"}
 curl -X POST "http://YOUR\_BLNK\_INSTANCE\_URL/balances" \
 -H "X-blnk-key: " \
 -H "Content-Type: application/json" \
 -d '{
 "ledger\_id": "ldg\_073f7ffe-9dfd-42ce-aa50-d1dca1788adc",
 "currency": "USD"
 }'
 \`\`\`

 \`\`\`javascript TypeScript wrap theme={"system"}
 const response = await blnk.LedgerBalances.create({
 ledger\_id: 'ldg\_073f7ffe-9dfd-42ce-aa50-d1dca1788adc',
 currency: 'USD',
 });
 \`\`\`

 \`\`\`go Go wrap theme={"system"}
 balance, resp, err := client.LedgerBalance.Create(blnkgo.CreateLedgerBalanceRequest{
 LedgerID: "ldg\_073f7ffe-9dfd-42ce-aa50-d1dca1788adc",
 Currency: "USD",
 })
 \`\`\`


 \`\`\`json Response theme={"system"}
 {
 "balance\_id": "bln\_ebcd230f-6265-4d4a-a4ca-45974c47f746",
 "ledger\_id": "ldg\_073f7ffe-9dfd-42ce-aa50-d1dca1788adc",
 "currency": "USD",
 "balance": 0,
 "created\_at": "2024-12-21T01:36:46.997063436Z"
 }
 \`\`\`

 Copy the \`balance\_id\` from the response. You'll need it for the next step.

 To do this, call the endpoint URL, \`/transactions\`, and pass the following in its request body:


 \`\`\`bash cURL wrap theme={"system"}
 curl -X POST "http://YOUR\_BLNK\_INSTANCE\_URL/transactions" \
 -H "X-blnk-key: " \
 -H "Content-Type: application/json" \
 -d '{
 "reference": "ref\_8728781718",
 "amount": 3000,
 "currency": "USD",
 "precision": 100,
 "source": "bln\_ebcd230f-6265-4d4a-a4ca-45974c47f746",
 "destination": "@CompanyRevenue",
 "description": "Invoice payment",
 "meta\_data": {
 "user\_id": "user\_a"
 }
 }'
 \`\`\`

 \`\`\`javascript TypeScript wrap theme={"system"}
 const response = await blnk.Transactions.create({
 reference: 'ref\_8728781718',
 amount: 3000,
 currency: 'USD',
 precision: 100,
 source: 'bln\_ebcd230f-6265-4d4a-a4ca-45974c47f746',
 destination: '@CompanyRevenue',
 description: 'Invoice payment',
 meta\_data: { user\_id: 'user\_a' },
 });
 \`\`\`

 \`\`\`go Go wrap theme={"system"}
 transaction, resp, err := client.Transaction.Create(blnkgo.CreateTransactionRequest{
 ParentTransaction: blnkgo.ParentTransaction{
 Amount: 3000,
 Reference: "ref\_8728781718",
 Currency: "USD",
 Precision: 100,
 Source: "bln\_ebcd230f-6265-4d4a-a4ca-45974c47f746",
 Destination: "@CompanyRevenue",
 Description: "Invoice payment",
 },
 })
 \`\`\`


 \`\`\`json Response theme={"system"}
 {
 "transaction\_id": "txn\_171821872187",
 "reference": "ref\_8728781718",
 "amount": 3000,
 "precise\_amount": 300000,
 "currency": "USD",
 "source": "bln\_ebcd230f-6265-4d4a-a4ca-45974c47f746",
 "destination": "@CompanyRevenue",
 "status": "QUEUED",
 "created\_at": "2024-04-23T12:32:00Z"
 }
 \`\`\`

 This moves 3000 from User A's balance to your revenue account (represented by an \[internal balance\](/balances/internal-balances) in your ledger).

 You can see at first glance that this transaction has a source (where it was debited from) and a destination (where it was credited to).

 \| id \| reference \| amount \| currency \| source \| destination \| description \| created\\\_at \|
 \| \-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\- \| \-\-\-\-\-\-\-\-\-\-\-\-\-\-\- \| \-\-\-\-\-\- \| \-\-\-\-\-\-\-\- \| \-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\- \| \-\-\-\-\-\-\-\-\-\-\-\-\-\-\- \| \-\-\-\-\-\-\-\-\-\-\-\-\-\-\- \| \-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\- \|
 \| txn\\\_171821872187 \| ref\\\_8728781718 \| 300000 \| USD \| bln\\\_ebcd230f-6265-4d4a-a4ca-45974c47f746 \| @CompanyRevenue \| Invoice payment \| 2024-04-23 12:32 UTC \|


 The amount was converted to its lowest unit (cents, in this case) to ensure correctness when computing the balances. To learn more, read: \[Understanding precision\](/transactions/precision).


\\*\\*\\*

\## Part 2: Dealing with the real world

Dealing with double entry when you manage both accounts is easy. But what happens when you deal with the world? What happens if User A sends the same \\$3,000 to an external bank account outside of our system? How do we represent it with the double entry principle?

To do this with Blnk, we recommend that you create an internal balance to represent all transactions with the outside world. We call it \`"@World"\`. This way you can track all money in and out of your system from one balance.

To apply it, use \`@World\` in the source or destination field depending on what kind of transaction it is, and you now have a corresponding, traceable entry equal to your user balance's entry. For example, User A sent money out, your transaction records would look like this:

\| id \| reference \| amount \| currency \| source \| destination \| description \| created\\\_at \|
\| \-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\- \| \-\-\-\-\-\-\-\-\-\-\-\-\-\-\- \| \-\-\-\-\-\- \| \-\-\-\-\-\-\-\- \| \-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\- \| \-\-\-\-\-\-\-\-\-\-\- \| \-\-\-\-\-\-\-\-\-\-\-\-\-\-\- \| \-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\- \|
\| txn\\\_171821872187 \| ref\\\_8728781718 \| 300000 \| USD \| bln\\\_ebcd230f-6265-4d4a-a4ca-45974c47f746 \| @World \| Invoice payment \| 2024-04-23 12:32 UTC \|

\\*\\*\\*

\## Applying this to your financial application

When building your financial application, it is important to apply the double entry principle to ensure that you can guard against fraud and dishonesty. Blnk allows you to be able to see both entries in one record, instead of recording the CR and DR entries in separate records in your database.

Good financial recording also saves you a lot of time and effort in performing settlements and reconciliation, a necessary operation for any financial application in today's world.

With Blnk, you don't have to worry about building the structures for Double Entry recording. You have instant access to all the tools you need to correctly record and track all transactions in your application.

\## Need help?

We are very happy to help you make the most of Blnk, regardless of whether it is your first time or you are switching from another tool.

To ask questions or discuss issues, please \[contact us\](mailto:support@blnkfinance.com) or \[join our Discord community\](https://discord.gg/7WNv94zPpx).

 Sign up and manage your ledger with our back-office dashboard. You can invite teammates to collaborate and manage your ledger operations directly from the dashboard.

\\*\\*\\*

\## References

A big part of this guide was referenced from this amazing article by Meredydd: \[An Engineer's Guide to Double-Entry Bookkeeping\](https://anvil.works/blog/double-entry-accounting-for-engineers).

Check it out to learn more about bookkeeping. Other references include:

1\. \[How To Do A Bank Reconciliation (EASY WAY)\](https://www.youtube.com/watch?v=zhmO3DM3YiY) by Accounting Stuff
2\. \[Ledger: Stripe’s system for tracking and validating money movement\](https://stripe.com/blog/ledger-stripe-system-for-tracking-and-validating-money-movement) by Ilya Ganelin
3\. \[Double Entry: What It Means in Accounting and How It's Used\](https://www.investopedia.com/terms/d/double-entry.asp) by Adam Hayes