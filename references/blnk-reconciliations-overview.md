\> ## Documentation Index
\> Fetch the complete documentation index at: https://docs.blnkfinance.com/llms.txt
\> Use this file to discover all available pages before exploring further.

\# Overview

\> Learn how to reconcile your Blnk Ledger.

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
→](https://docs.blnkfinance.com/reconciliations/%7Bhref%7D)

;
};

Available in version 0.7.0 and later.

Reconciliation compares and matches your Blnk Ledger with external records such as bank statements, payment processor reports, etc. to identify and resolve discrepancies in your financial data.

When performed effectively, reconciliation enhances operational efficiency, financial transparency, and stakeholder trust.

In this guide, you'll learn how to run reconciliation processes with Blnk Core.

\\*\\*\\*

\## Before you start

To successfully run reconciliation, you need to first do three things:

1\. \*\*Prepare your external data:\*\* Format your external data according to the specified structure to enable seamless matching with your Ledger.

 See \[External Data Preparation\](/reconciliations/external-data) for details.

2\. \*\*Define your matching rules:\*\* Set rules to compare fields like amount, date, or reference between your ledger and external records. These rules determine how closely records must match.

 See \[Matching Rules\](/reconciliations/matching-rules) for details.

3\. \*\*Set your reconciliation strategy:\*\* Specify how transactions are distributed between both records, e.g., one transaction in your ledger can be split into 2 or more in a bank statement.

 See \[Reconciliation Strategies\](/reconciliations/strategies) for details.

\\*\\*\\*

\## Reconciliation options

Blnk offers two reconciliation options based on your needs:

1\. \*\*Batch reconciliation:\*\* Ideal for scheduled bulk processing, such as daily bank reconciliations. It’s efficient for handling large volumes of transactions at once.

2\. \*\*Instant reconciliation:\*\* Best for real-time accuracy, like immediate fraud detection or on-the-fly transaction matching. It ensures your ledger stays up-to-date as transactions occur.

\\*\\*\\*

\## Option 1: Batch reconciliation

To reconcile in batches, you need to:

1\. \[Prepare and upload your external data\](/reconciliations/external-data) in a CSV or JSON file for upload.
2\. Pass the \`upload\_id\` from your successful upload in the request body.
3\. Set your \[matching rules\](/reconciliations/matching-rules) and \[reconciliation strategy\](/reconciliations/strategies).

To run your batch reconciliation, call the \[Start Reconciliation\](/reference/start-reconciliation) endpoint:

 \`\`\`bash cURL wrap theme={"system"}
 curl -X POST "http://YOUR\_BLNK\_INSTANCE\_URL/reconciliation/start" \
 -H "X-blnk-key: " \
 -H "Content-Type: application/json" \
 -d '{
 "upload\_id": "upload\_8c700d1b-09c0-4ef4-9ab1-a0decf3d0aa3",
 "strategy": "one\_to\_many",
 "dry\_run": false,
 "grouping\_criteria": "reference",
 "matching\_rule\_ids": \[\
 "rule\_890bdbc4-467f-4670-8424-5667e41daf29",\
 "rule\_a1b2c3d4-5678-90ef-ghij-klmnopqrstuv"\
 \]
 }'
 \`\`\`

 \`\`\`javascript TypeScript wrap theme={"system"}
 const response = await blnk.Reconciliation.run({
 upload\_id: 'upload\_8c700d1b-09c0-4ef4-9ab1-a0decf3d0aa3',
 strategy: 'one\_to\_many',
 dry\_run: false,
 grouping\_criteria: 'reference',
 matching\_rule\_ids: \[\
 'rule\_890bdbc4-467f-4670-8424-5667e41daf29',\
 'rule\_a1b2c3d4-5678-90ef-ghij-klmnopqrstuv',\
 \],
 });
 \`\`\`

 \`\`\`go Go wrap theme={"system"}
 result, resp, err := client.Reconciliation.Run(blnkgo.RunReconData{
 UploadID: "upload\_8c700d1b-09c0-4ef4-9ab1-a0decf3d0aa3",
 Strategy: blnkgo.ReconciliationStrategyOneToMany,
 DryRun: false,
 GroupingCriteria: "reference",
 MatchingRuleIDs: \[\]string{
 "rule\_890bdbc4-467f-4670-8424-5667e41daf29",
 "rule\_a1b2c3d4-5678-90ef-ghij-klmnopqrstuv",
 },
 })
 \`\`\`

\`\`\`json Response theme={"system"}
{
 "reconciliation\_id": "recon\_6e6feddd-930b-4e3-8ba-1a3eee659bb3",
 "status": "in\_progress"
}
\`\`\`

\| \*\*Field\*\* \| \*\*Type\*\* \| \*\*Description\*\* \|
\| :\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\- \| :\-\-\-\-\-\-\- \| :\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\- \|
\| \`upload\_id\` \| String \| Unique identifier of the uploaded external record for reconciliation. \|
\| \`strategy\` \| String \| Defines the reconciliation strategy to be used. \|
\| \`matching\_rule\_ids\` \| Array \| An array of rule ids that determine how the records are matched. \|
\| \`grouping\_criteria\` \| String \| Specifies the field used to group related records during reconciliation. Not required for \`one\_to\_one\`. \|
\| \`dry\_run\` \| Boolean \| If \`false\`, result is saved to the database. If \`true\`, it is not. \|

 \`dry\_run\` is useful for debugging and testing strategies when performing reconciliation. If set to \`true\`, Blnk performs the reconciliation and prints the result without saving to the database.

\\*\\*\\*

\## Option 2: Instant reconciliation

Available on version 0.10.1 or later.

To reconcile transactions instantly, provide the external records as an array of transactions in your request body instead of a bulk upload.

To run your reconciliation, call the \[Instant Reconciliation\](/reference/instant-reconciliation) endpoint:

 \`\`\`bash cURL wrap expandable theme={"system"}
 curl -X POST "http://YOUR\_BLNK\_INSTANCE\_URL/reconciliation/start-instant" \
 -H "X-blnk-key: " \
 -H "Content-Type: application/json" \
 -d '{
 "external\_transactions": \[\
 {\
 "id": "txn1a2b3c4d5e6f7g8h9i0",\
 "amount": 5.49,\
 "reference": "INV-2023-002",\
 "currency": "GBP",\
 "description": "Card payment",\
 "date": "2024-11-15T14:25:30Z",\
 "source": "bank-api"\
 },\
 {\
 "id": "txn9z8y7x6w5v4u3t2s1",\
 "amount": 12.75,\
 "reference": "INV-2023-003",\
 "currency": "GBP",\
 "description": "Subscription renewal",\
 "date": "2024-11-15T14:40:15Z",\
 "source": "bank-api"\
 },\
 {\
 "id": "txn5p4o3n2m1l0k9j8h7",\
 "amount": 8.99,\
 "reference": "INV-2023-004",\
 "currency": "GBP",\
 "description": "Online purchase",\
 "date": "2024-11-15T14:55:10Z",\
 "source": "bank-api"\
 }\
 \],
 "strategy": "one\_to\_one",
 "matching\_rule\_ids": \[\
 "rule\_04c4a59e-08a1-423b-bfae-48feebe16473"\
 \]
 }'
 \`\`\`

 \`\`\`javascript TypeScript wrap expandable theme={"system"}
 const response = await blnk.Reconciliation.run({
 external\_transactions: \[\
 {\
 id: 'txn1a2b3c4d5e6f7g8h9i0',\
 amount: 5.49,\
 reference: 'INV-2023-002',\
 currency: 'GBP',\
 description: 'Card payment',\
 date: '2024-11-15T14:25:30Z',\
 source: 'bank-api',\
 },\
 \],
 strategy: 'one\_to\_one',
 matching\_rule\_ids: \[\
 'rule\_04c4a59e-08a1-423b-bfae-48feebe16473',\
 \],
 });
 \`\`\`

 \`\`\`go Go wrap expandable theme={"system"}
 result, resp, err := client.Reconciliation.Run(blnkgo.RunReconData{
 ExternalTransactions: \[\]blnkgo.ExternalTransaction{
 {
 ID: "txn1a2b3c4d5e6f7g8h9i0",
 Amount: 5.49,
 Reference: "INV-2023-002",
 Currency: "GBP",
 Description: "Card payment",
 Date: "2024-11-15T14:25:30Z",
 Source: "bank-api",
 },
 },
 Strategy: blnkgo.ReconciliationStrategyOneToOne,
 MatchingRuleIDs: \[\]string{
 "rule\_04c4a59e-08a1-423b-bfae-48feebe16473",
 },
 })
 \`\`\`

\`\`\`json Response theme={"system"}
{
 "reconciliation\_id": "recon\_6e6feddd-930b-4e3-8ba-1a3eee659bb3",
 "upload\_id": "instant\_ad4f809c-7e2a-4c9d-8a65-1f76131afdf1",
 "status": "completed",
 "matched\_transactions": 3,
 "unmatched\_transactions": 0,
 "is\_dry\_run": false,
 "started\_at": "2025-03-15T17:43:26.463849Z",
 "completed\_at": "2025-03-15T17:43:26.508514Z"
}
\`\`\`

\### Verifying reconciled transactions

You can verify if a transaction has been reconciled with \*\*Instant Reconciliation\*\* by retrieving the transaction details with the \[Get Transaction\](/reference/get-transaction) endpoint.

 \`\`\`bash cURL wrap theme={"system"}
 curl -X GET "https://YOUR\_BLNK\_INSTANCE\_URL/transactions/{transaction\_id}" \
 -H "X-blnk-key: "
 \`\`\`

 \`\`\`javascript TypeScript wrap theme={"system"}
 const response = await blnk.Transactions.get('{transaction\_id}');
 \`\`\`

 \`\`\`go Go wrap theme={"system"}
 txn, resp, err := client.Transaction.Get("{transaction\_id}")
 \`\`\`

You'll find the reconciliation results in the \`meta\_data\` object as shown below:

\`\`\`json Response {5} theme={"system"}
{
 ...
 "meta\_data": {
 "external\_txn\_id": "txn9z8y7x6w5v4u3t2s1",
 "reconciled": true,
 "reconciled\_at": "2025-03-15T18:11:52.61+00",
 "reconciliation\_amount": 12.75,
 "reconciliation\_id": "recon\_9134cb8-a3f6-47e9-b566-4fb93fa051"
 }
}
\`\`\`

\| Field \| Description \|
\| :\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\- \| :\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\- \|
\| \`external\_txn\_id\` \| ID of the matched external transaction. \|
\| \`reconciled: true\` \| If matched successfully. \|
\| \`reconciled\_at\` \| When the reconciliation was done. \|
\| \`reconciliation\_amount\` \| Amount from the external record. \|
\| \`reconciliation\_id\` \| Unique ID of the reconciliation process. \|

\\*\\*\\*

\## Common use cases

Reconciliation can be a great fit for you if you are building:

1\. \*\*BNPL platforms:\*\* Verify loan disbursements and repayments match merchant records, preventing billing errors and ensuring smooth settlements.

2\. \*\*Fintech apps:\*\* Sync payments from multiple sources (e.g., cards, wallets) for accurate reporting and user trust.

3\. \*\*Crypto exchanges:\*\* Reconcile transactions between wallets, smart contracts, and fiat ramps to maintain balance integrity.

4\. \*\*Payment processors:\*\* Ensure funds move correctly between merchants, banks, and users, avoiding costly mismatches.

5\. \*\*Multi-currency systems:\*\* Align balances across regions and providers, simplifying global operations.

\\*\\*\\*

\## Need help?

We are very happy to help you make the most of Blnk, regardless of whether it is your first time or you are switching from another tool.

To ask questions or discuss issues, please \[contact us\](mailto:support@blnkfinance.com) or \[join our Discord community\](https://discord.gg/7WNv94zPpx).

 Sign up and manage your ledger with our back-office dashboard. You can invite teammates to collaborate and manage your ledger operations directly from the dashboard.