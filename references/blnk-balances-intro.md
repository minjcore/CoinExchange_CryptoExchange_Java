\> ## Documentation Index
\> Fetch the complete documentation index at: https://docs.blnkfinance.com/llms.txt
\> Use this file to discover all available pages before exploring further.

\# Overview

\> Learn how balances work in Blnk

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
→](https://docs.blnkfinance.com/balances/%7Bhref%7D)

;
};

Ledger balances (or balances, for short) are used to represent store of value in your Blnk Ledger, i.e., accounts, wallets, card balance, etc. They also represent the source or destination of a transaction record.

All ledger balances in the Blnk Ledger each have 6 main balance attributes:

\| Attribute \| Description \|
\| :\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\- \| :\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\- \|
\| \`balance\` \| The current value held in the ledger balance \|
\| \`credit\_balance\` \| The total sum of all amounts received by a ledger balance \|
\| \`debit\_balance\` \| The total sum of all amounts sent by a ledger balance \|
\| \`inflight\_balance\` \| The net amount held inflight for a balance \|
\| \`inflight\_credit\_balance\` \| The total sum of all amounts waiting to be received by a ledger balance \|
\| \`inflight\_debit\_balance\` \| The total sum of all amounts waiting to be deducted from a ledger balance \|

\*\*Important to note:\*\* Balance amounts are immutable and can only be updated through transactions. You cannot manually set or alter a balance amount directly.

\\*\\*\\*

\## Creating a balance

To create a balance in Blnk, call the \*\*Create Balance\*\* endpoint:

 \`\`\`bash cURL wrap theme={"system"}
 curl -X POST "http://localhost:5001/balances" \
 -H "X-blnk-key: " \
 -H "Content-Type: application/json" \
 -d '{
 "ledger\_id": "ldg\_073f7ffe-9dfd-42ce-aa50-d1dca1788adc",
 "currency": "USD",
 "meta\_data": {
 "first\_name": "Alice",
 "last\_name": "Hart",
 "account\_number": "1234567890"
 }
 }'
 \`\`\`

 \`\`\`javascript TypeScript wrap theme={"system"}
 const { LedgerBalances } = blnk;

 const newBalance = await LedgerBalances.create({
 ledger\_id: "ldg\_073f7ffe-9dfd-42ce-aa50-d1dca1788adc",
 currency: "USD",
 meta\_data: {
 first\_name: "Alice",
 last\_name: "Hart",
 account\_number: "1234567890"
 }
 });
 console.log("Balance Created:", newBalance);
 \`\`\`

 \`\`\`go Go wrap theme={"system"}
 balance, \_, err := client.LedgerBalance.Create(blnkgo.CreateLedgerBalanceRequest{
 LedgerID: "ldg\_073f7ffe-9dfd-42ce-aa50-d1dca1788adc",
 Currency: "USD",
 MetaData: map\[string\]interface{}{
 "first\_name": "Alice",
 "last\_name": "Hart",
 "account\_number": "1234567890",
 },
 })
 if err != nil {
 log.Fatal(err)
 }
 fmt.Println("Balance Created:", balance.BalanceID)
 \`\`\`

 \`\`\`bash Blnk CLI wrap theme={"system"}
 blnk balances create
 ? Ledger ID:
 ? Currency:
 ? Metadata (JSON format):
 \`\`\`

\`\`\`json Response expandable theme={"system"}
{
 "balance": 0,
 "version": 0,
 "inflight\_balance": 0,
 "credit\_balance": 0,
 "inflight\_credit\_balance": 0,
 "debit\_balance": 0,
 "inflight\_debit\_balance": 0,
 "precision": 0,
 "ledger\_id": "ldg\_073f7ffe-9dfd-42ce-aa50-d1dca1788adc",
 "identity\_id": "",
 "balance\_id": "bln\_e39a239a-a6ca-4509-b0d9-29dcc5630f8a",
 "indicator": "",
 "currency": "USD",
 "created\_at": "2024-07-05T08:13:18.882616461Z",
 "meta\_data": {
 "customer\_internal\_id": "1234",
 "customer\_name": "Jerry"
 }
}
\`\`\`

All newly created balances in the Blnk Ledger start from 0. You cannot create a ledger balance with a predefined balance amount.

\\*\\*\\*

\## Queued balances

Available in version 0.8.3 and later.

You can track total transaction amounts in queue per balance via the \`queued\_credit\_balance\` and \`queued\_debit\_balance\` attributes. These attributes show the cumulative value of all queued transactions that will affect a balance once they're processed.

When you query a balance, you'll see:

1\. \*\*queued\\\_credit\\\_balance\*\*: Total amount of incoming transactions waiting to be processed.
2\. \*\*queued\\\_debit\\\_balance\*\*: Total amount of outgoing transactions waiting to be processed.

This helps you estimate the potential impact of queued transactions on your balance before they're processed.

 Use queued balances to check if a balance has pending transactions. If either \`queued\_credit\_balance\` or \`queued\_debit\_balance\` is greater than 0, there are transactions waiting in the queue for that balance.

 Queued balances are estimates and may change as transactions are processed or if queue processing fails. Always verify final balances after queue processing completes.

To view the queued balances of a ledger balance:

 \`\`\`bash cURL wrap theme={"system"}
 curl -X GET 'http://YOUR\_BLNK\_INSTANCE\_URL/balances/{balance\_id}?with\_queued=true' \
 -H 'X-blnk-key: '
 \`\`\`

\`\`\`json Response {9-10} expandable theme={"system"}
{
 "balance": 0,
 "version": 0,
 "inflight\_balance": 0,
 "credit\_balance": 0,
 "inflight\_credit\_balance": 0,
 "debit\_balance": 0,
 "inflight\_debit\_balance": 0,
 "queued\_credit\_balance": 0,
 "queued\_debit\_balance": 0,
 "precision": 0,
 "ledger\_id": "ldg\_073f7ffe-9dfd-42ce-aa50-d1dca1788adc",
 "identity\_id": "",
 "balance\_id": "bln\_e39a239a-a6ca-4509-b0d9-29dcc5630f8a",
 "indicator": "",
 "currency": "USD",
 "created\_at": "2024-07-05T08:13:18.882616461Z",
 "meta\_data": {
 "customer\_internal\_id": "1234",
 "customer\_name": "Jerry"
 }
}
\`\`\`

\### Calculating expected and available balances

Use these formulas to understand your balance position:

1\. \*\*Expected balance:\*\* What your balance will be after all queued credits are processed

 \`\`\`
 expected\_balance = current\_balance + queued\_credit\_balance
 \`\`\`

2\. \*\*Available balance:\*\* How much you can safely spend or transfer (accounting for pending debits)

 \`\`\`
 available\_balance = current\_balance - inflight\_debit\_balance - queued\_debit\_balance
 \`\`\`

 \*\*Example:\*\* If your current balance is $100, you have $50 in queued credits, and \\$20 in queued debits:

 \\* Expected balance: $100 + $50 = \\$150
 \\* Available balance: $100 - $0 - $20 = $80

\\*\\*\\*

\## Multi-currency balances

Blnk enables you to create and manage balances in multiple currencies within your ledger. When building multi-currency wallets with Blnk, keep the following in mind:

1\. \*\*Standardize \[precision\](/transactions/precision) across currencies:\*\* Most fiat currencies have a precision of 100. However, when managing currencies with different precision values, such as crypto and fiat, it's crucial to use the highest precision value for all transactions in your ledger. This ensures consistency when calculating transactions and converting exchange rates between currencies.

\\*\\*\\*

\## Balance overdrafts

An overdraft occurs when a transaction reduces a ledger balance below zero, resulting in a negative balance. This means that the amount debited exceeds the available funds in the account. While traditionally seen as a deficit, in Blnk, a negative balance is simply another balance state and can be used flexibly, especially depending on the type of financial product.

In Blnk, implementing overdrafts is straightforward. By setting the \`allow\_overdraft\` attribute when initiating a transaction, you can allow a ledger balance to fall below 0. This feature is valuable as it provides insights and reflects the position of a balance, especially useful in certain product contexts, such as lending or line-of-credit services.

See the following: \[Applying overdrafts\](/transactions/overdrafts)

\### Why negative balances?

In Blnk, a negative balance is simply a balance state and doesn’t necessarily mean there’s a problem. For example, it can simply show borrowed funds, as expected in products like loans or credit, where it reflects the amount owed.

It could also mean that a balance has more debits than credits — this is especially relevant for internal balances like \`@World\`. To learn more, see the following: \[Internal balances\](/balances/internal-balances)

Moreover, a negative balance is often temporary and can revert to a positive balance under certain conditions, such as:

1\. \*\*Incoming Credits:\*\* When new funds are credited to the account, they reduce or eliminate the negative balance.
2\. \*\*Scheduled Repayments:\*\* For products like loans, regular repayments or scheduled deposits will move the balance back toward positive territory over time, ultimately making it positive if the full overdraft is repaid.

\## Need help?

We are very happy to help you make the most of Blnk, regardless of whether it is your first time or you are switching from another tool.

To ask questions or discuss issues, please \[contact us\](mailto:support@blnkfinance.com) or \[join our Discord community\](https://discord.gg/7WNv94zPpx).

 Sign up and manage your ledger with our back-office dashboard. You can invite teammates to collaborate and manage your ledger operations directly from the dashboard.