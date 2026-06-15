\> ## Documentation Index
\> Fetch the complete documentation index at: https://docs.blnkfinance.com/llms.txt
\> Use this file to discover all available pages before exploring further.

\# About Transactions

\> Learn how transactions are recorded and processed in Blnk

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
→](https://docs.blnkfinance.com/transactions/%7Bhref%7D)

;
};

Transactions enable money movement between two or more balances. These can be payments, transfers, settlements, internal treasury management, etc. All transactions in Blnk are recorded with the \[double entry principle\](/resources/double-entry) — every transaction has a source and a corresponding destination.

Transactions happen between balances, and \[balances are created\](/balances/introduction) within \[ledgers\](/ledgers/introduction).

In this guide, you'll learn about:

1\. \[Transaction properties\](#transaction-properties)
2\. \[Recording a transaction\](#recording-a-transaction)
3\. \[Verifying transactions\](#verifying-transactions)

\\*\\*\\*

\## Transaction properties

1\. \*\*Immutability:\*\* Once recorded, transactions in Blnk cannot be modified or deleted. This fundamental property ensures the integrity and reliability of your transaction history, preventing any unauthorized alterations. See also: \[Transaction hashing\](/transactions/hash)

2\. \*\*Idempotency:\*\* Each transaction in Blnk produces the same result whether executed once or multiple times. This is crucial for maintaining data consistency, especially during network failures or system retries.


 Blnk implements idempotency by requiring a unique \`reference\` for every transaction. This reference serves as a transaction identifier, preventing duplicate processing and ensuring consistent outcomes.


\\*\\*\\*

\## Recording a transaction

To record a transaction, call the \*\*record-transaction\*\* endpoint:

 \`\`\`bash cURL wrap theme={"system"}
 curl -X POST "http://YOUR\_BLNK\_INSTANCE\_URL/transactions" \
 -H "X-blnk-key: " \
 -H "Content-Type: application/json" \
 -d '{
 "amount": 750,
 "reference": "ref\_001adcfgf",
 "currency": "USD",
 "precision": 100,
 "source": "@FundingPool",
 "destination": "bln\_ebcd230f-6265-4d4a-a4ca-45974c47f746",
 "description": "Fund with starting balance amount",
 "allow\_overdraft": true,
 "skip\_queue": false,
 "meta\_data": {}
 }'
 \`\`\`

 \`\`\`javascript TypeScript wrap theme={"system"}
 const response = await blnk.Transactions.create({
 amount: 750,
 reference: 'ref\_001adcfgf',
 currency: 'USD',
 precision: 100,
 source: '@FundingPool',
 destination: 'bln\_ebcd230f-6265-4d4a-a4ca-45974c47f746',
 description: 'Fund with starting balance amount',
 allow\_overdraft: true,
 skip\_queue: false,
 meta\_data: {},
 });
 \`\`\`

 \`\`\`go Go wrap theme={"system"}
 transaction, resp, err := client.Transaction.Create(blnkgo.CreateTransactionRequest{
 ParentTransaction: blnkgo.ParentTransaction{
 Amount: 750,
 Reference: "ref\_001adcfgf",
 Currency: "USD",
 Precision: 100,
 Source: "@FundingPool",
 Destination: "bln\_ebcd230f-6265-4d4a-a4ca-45974c47f746",
 Description: "Fund with starting balance amount",
 },
 AllowOverdraft: true,
 SkipQueue: false,
 })
 \`\`\`

 \`\`\`bash Blnk CLI wrap theme={"system"}
 blnk transactions create
 \`\`\`

\`\`\`json Response expandable theme={"system"}
{
 "id": "txn\_6164573b-6cc8-45a4-ad2e-7b4ba6a60f7d",
 "source": "@FundingPool",
 "destination": "bln\_ebcd230f-6265-4d4a-a4ca-45974c47f746",
 "reference": "ref\_001adcfgf",
 "amount": 750,
 "precision": 100,
 "precise\_amount": 75000,
 "currency": "USD",
 "description": "Fund with starting balance amount",
 "status": "QUEUED",
 "created\_at": "2024-12-21T01:36:46.997063436Z",
 "meta\_data": {
 "sender\_name": "John Doe",
 "sender\_account": "00000000000"
 }
}
\`\`\`

 If this is your first transaction, the participating balances will start at 0. To ensure the transaction is successful, enable overdrafts as shown above, allowing the source balance to go negative.

 Learn more about \[Overdrafts\](/transactions/overdrafts) and \[Negative Balances\](/resources/negative-balances).

 \| Field \| Description \| Required \| Type \|
 \| \-\-\-\-\-\-\-\-\-\-\-\-\- \| \-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\- \| \-\-\-\-\-\-\-\- \| \-\-\-\-\-\-\-\- \|
 \| \`amount\` \| The transaction amount. \| Yes \| \`float\` \|
 \| \`reference\` \| Your unique reference to ensure idempotency. \| Yes \| \`string\` \|
 \| \`currency\` \| Short code for your asset class. \| Yes \| \`string\` \|
 \| \`precision\` \| Precision for the currency/asset passed. See also: \[Precision\](/transactions/precision) \| No \| \`int64\` \|
 \| \`source\` \| Sender's balance ID \| Yes \| \`string\` \|
 \| \`destination\` \| Recipient's balance ID. \| Yes \| \`string\` \|
 \| \`description\` \| Description or narration of the transaction. \| No \| \`string\` \|
 \| \`meta\_data\` \| Custom data associated with the transaction \| No \| \`object\` \|

 \| Field \| Description \| Type \| \|
 \| \-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\- \| \-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\- \| \-\-\-\-\-\-\-\-\- \| \-\-\-\-\-\-\-\- \|
 \| \`amount\` \| The transaction amount. \| Yes \| \`float\` \|
 \| \`reference\` \| Your unique reference to ensure idempotency. \| Yes \| \`string\` \|
 \| \`currency\` \| Short code for your asset class. \| Yes \| \`string\` \|
 \| \`precision\` \| Precision for the currency/asset passed. See also: \[Precision\](/transactions/precision) \| No \| \`int64\` \|
 \| \`source\` \| Sender's balance ID \| Yes \| \`string\` \|
 \| \`destination\` \| Recipient's balance ID. \| Yes \| \`string\` \|
 \| \`description\` \| Description or narration of the transaction. \| No \| \`string\` \|
 \| \`meta\_data\` \| Custom data associated with the transaction \| No \| \`object\` \|
 \| \`id\` \| Unique id for the transaction. This is generated by Blnk. \| \`string\` \| \|
 \| \`precise\_amount\` \| The transaction amount recorded after the \`precision\` value has been applied. See also: \[Precision\](/transactions/precision) \| \`integer\` \| \|
 \| \`status\` \| Current state of your transaction record. See also: \[Transaction statuses\](/transactions/transaction-lifecycle#statuses) \| \`string\` \| \|
 \| \`created\_at\` \| Date and time of the transaction record. \| \`string\` \| \|

 Passing detailed data with the \`meta\_data\` object is encouraged; it provides you with 360-degree insights about each transaction record. Examples of data you can pass include \`sender\_name\`, \`account\_number\`, \`bank\_name\`, \`receiver\_name\`, \`payment\_id\`, \`ip\_address\`, \`location\`, \`payment\_method\`, etc.

\\*\\*\\*

\## Verifying transactions

After recording a transaction, you can verify its status using different methods depending on whether you're using the default queue system or the skip queue feature.

\### Verifying transactions with queue (default)

When using the default queue system, every transaction starts as \`QUEUED\`. To verify your transaction status, you have two options:

1\. \*\*Webhooks\*\*: Blnk sends \[webhook notifications\](/advanced/notifications) when transaction states change.
2\. \*\*Direct API calls\*\*: Query the transaction status using the reference or transaction ID. See below:

 Blnk appends a \`\_q\` suffix to your original reference after processing a \`QUEUED\` transaction. To verify the updated status, retrieve the transaction using your original reference plus the \`\_q\` suffix.


 \`\`\`bash cURL wrap theme={"system"}
 curl -X GET "http://YOUR\_BLNK\_INSTANCE\_URL/transactions/reference/{reference}" \
 -H "X-blnk-key: "
 \`\`\`


 \`\`\`json Response theme={"system"}
 {
 "transaction\_id": "txn\_6164573b-6cc8-45a4-ad2e-7b4ba6a60f7d",
 "reference": "ref\_001adcfgf",
 "status": "APPLIED"
 }
 \`\`\`

 Use the \*\*Search API\*\* to look up your transaction by reference. If the transaction exists, the response will include its current status.


 \`\`\`bash cURL wrap theme={"system"}
 curl -X POST "http://YOUR\_BLNK\_INSTANCE\_URL/search/transactions" \
 -H "Content-Type: application/json" \
 -d '{
 "q": "",
 "query\_by": "reference"
 }'
 \`\`\`

 \`\`\`javascript TypeScript wrap theme={"system"}
 const response = await blnk.Search.search(
 { q: '', query\_by: 'reference' },
 'transactions',
 );
 \`\`\`

 \`\`\`go Go wrap theme={"system"}
 results, resp, err := client.Search.SearchDocument(
 blnkgo.SearchParams{
 Q: "",
 QueryBy: "reference",
 },
 blnkgo.Transactions,
 )
 \`\`\`


 \`\`\`json Response theme={"system"}
 {
 "found": 1,
 "hits": \[\
 {\
 "document": {\
 "transaction\_id": "txn\_6164573b-6cc8-45a4-ad2e-7b4ba6a60f7d",\
 "reference": "ref\_001adcfgf",\
 "status": "APPLIED"\
 }\
 }\
 \]
 }
 \`\`\`

 To check the updated status, take the queued transaction ID and search with it as the \`parent\_transaction\`. This is because Blnk treats the queued transaction as the parent for the next transaction state. See also: \[Transaction lifecycle\](/transactions/transaction-lifecycle)


 \`\`\`bash cURL wrap theme={"system"}
 curl -X POST "http://YOUR\_BLNK\_INSTANCE\_URL/search/transactions" \
 -H "Content-Type: application/json" \
 -d '{
 "q": "",
 "query\_by": "parent\_transaction"
 }'
 \`\`\`

 \`\`\`javascript TypeScript wrap theme={"system"}
 const response = await blnk.Search.search(
 { q: '', query\_by: 'parent\_transaction' },
 'transactions',
 );
 \`\`\`

 \`\`\`go Go wrap theme={"system"}
 results, resp, err := client.Search.SearchDocument(
 blnkgo.SearchParams{
 Q: "",
 QueryBy: "parent\_transaction",
 },
 blnkgo.Transactions,
 )
 \`\`\`


 \`\`\`json Response theme={"system"}
 {
 "found": 1,
 "hits": \[\
 {\
 "document": {\
 "transaction\_id": "txn\_6164573b-6cc8-45a4-ad2e-7b4ba6a60f7d",\
 "parent\_transaction": "txn\_6164573b-6cc8-45a4-ad2e-7b4ba6a60f7d",\
 "status": "APPLIED"\
 }\
 }\
 \]
 }
 \`\`\`


\### Verifying transactions without queue (skip queue)

When using \`skip\_queue: true\`, transactions are processed immediately and you can verify them from the direct response. Learn more about \[skip queue\](/transactions/transaction-lifecycle#skip-transaction-queue).

 \`\`\`bash cURL wrap theme={"system"}
 curl -X POST "http://YOUR\_BLNK\_INSTANCE\_URL/transactions" \
 -H "X-blnk-key: " \
 -H "Content-Type: application/json" \
 -d '{
 "amount": 750,
 "reference": "ref\_001adcfgf",
 "currency": "USD",
 "precision": 100,
 "source": "@FundingPool",
 "destination": "bln\_ebcd230f-6265-4d4a-a4ca-45974c47f746",
 "description": "Fund with starting balance amount",
 "allow\_overdraft": true,
 "skip\_queue": true
 }'
 \`\`\`

 \`\`\`javascript TypeScript wrap theme={"system"}
 const response = await blnk.Transactions.create({
 amount: 750,
 reference: 'ref\_001adcfgf',
 currency: 'USD',
 precision: 100,
 source: '@FundingPool',
 destination: 'bln\_ebcd230f-6265-4d4a-a4ca-45974c47f746',
 description: 'Fund with starting balance amount',
 allow\_overdraft: true,
 skip\_queue: true,
 });
 \`\`\`

 \`\`\`go Go wrap theme={"system"}
 transaction, resp, err := client.Transaction.Create(blnkgo.CreateTransactionRequest{
 ParentTransaction: blnkgo.ParentTransaction{
 Amount: 750,
 Reference: "ref\_001adcfgf",
 Currency: "USD",
 Precision: 100,
 Source: "@FundingPool",
 Destination: "bln\_ebcd230f-6265-4d4a-a4ca-45974c47f746",
 Description: "Fund with starting balance amount",
 },
 AllowOverdraft: true,
 SkipQueue: true,
 })
 \`\`\`

\`\`\`json Response {11} theme={"system"}
{
 "id": "txn\_6164573b-6cc8-45d4-ad2e-7b4ba6a60f7d",
 "source": "@FundingPool",
 "destination": "bln\_ebcd230f-6265-4d4a-a4ca-45974c47f746",
 "reference": "ref\_001adcfgf",
 "amount": 750,
 "precision": 100,
 "precise\_amount": 75000,
 "currency": "USD",
 "description": "Fund with starting balance amount",
 "status": "APPLIED",
 "created\_at": "2024-12-21T01:36:46.997063436Z",
 "meta\_data": {}
}
\`\`\`

The response immediately shows the final status (\`INFLIGHT\`, \`APPLIED\`, or \`REJECTED\`), allowing you to confirm the transaction result without waiting for webhooks or additional API calls.

 When using \`skip\_queue: true\`, you may encounter lock errors if multiple transactions are processed simultaneously on the same balance. Learn how to handle these scenarios in our \[Handling Hot Balances\](/guides/hot-balances) guide.

\\*\\*\\*

\## Discarded transactions

A discarded transaction is not recorded in the ledger. Blnk discards transactions for two reasons:

1\. \*\*Duplicate reference:\*\* Your new transaction \`reference\` matches an existing \`reference\` in your ledger. Blnk requires unique \`reference\` values per transaction. Options are timestamps (e.g. UNIX timestamp), random string or UUID (e.g. \`ref\_e55c4f33-bff7-4c30-9b9f-5d2d10a29b7a\`), or a business identifier like an \`order\_id\`.

2\. \*\*Zero amount:\*\* Your transaction amount is 0. Zero amounts are not recorded in the Blnk ledger.

Make sure your request body match the Blnk Ledger API specifications. For example, avoid passing \`apply\_overdraft\` instead of \`allow\_overdraft\`.

\\*\\*\\*

\## Managing insufficient funds

 Available in version 0.11.0 and later. For versions 0.10.8 and older, see our \[insufficient funds guide\](/guides/insufficient-funds).

Blnk performs comprehensive balance checks before processing any transaction to ensure you have sufficient funds available. The system computes an available balance by calculating \`balance - inflight\_debit\_balance\` on the source balance.

 Inflight debit balance is the amount waiting to be deducted from the source balance from inflight transactions. Learn more: \[Inflight transactions\](/transactions/inflight).

If the transaction amount is more than the available balance, the source has insufficient funds and:

1\. The transaction is rejected and status is recorded as \`REJECTED\`.
2\. A webhook notification to inform your system of the rejected transaction and a reason in its \`meta\_data\`.

\### Using overdrafts

If you want a transaction to proceed even when it exceeds the available balance, you can enable overdrafts by setting \`allow\_overdraft: true\` in your transaction request.

 Overdrafts and negative balances.

\\*\\*\\*

\## Dive deeper

 States from creation through completion.

 Decimal handling for accurate amounts.

 What each status means and when it applies.


\\*\\*\\*

\## Need help?

We are very happy to help you make the most of Blnk, regardless of whether it is your first time or you are switching from another tool.

To ask questions or discuss issues, please \[contact us\](mailto:support@blnkfinance.com) or \[join our Discord community\](https://discord.gg/7WNv94zPpx).

 Sign up and manage your ledger with our back-office dashboard. You can invite teammates to collaborate and manage your ledger operations directly from the dashboard.