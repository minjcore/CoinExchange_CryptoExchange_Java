\> ## Documentation Index
\> Fetch the complete documentation index at: https://docs.blnkfinance.com/llms.txt
\> Use this file to discover all available pages before exploring further.

\# Retrieving Historical Balances

\> Retrieve accurate historical balance information.

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

Available in version 0.8.4 and later.

The \[Historical Balances\](/reference/historical-balances) endpoint allows users to retrieve balances (identified by \`balance\_id\`) at a particular historical point in time, specified by the timestamp parameter.

It leverages Blnk's balance snapshot feature to provide accurate historical data for financial reporting, auditing, or analysis.

 \`\`\`bash cURL wrap theme={"system"}
 curl -X GET "http://YOUR\_BLNK\_INSTANCE\_URL/balances/{balance\_id}/at?timestamp=2024-04-22T15:28:03.123456Z" \
 -H "X-blnk-key: "
 \`\`\`

 \`\`\`go Go wrap theme={"system"}
 at, \_ := time.Parse(time.RFC3339, "2024-04-22T15:28:03.123456Z")
 balance, resp, err := client.LedgerBalance.GetHistorical(
 "bln\_5ce86029-3c2e-4e2a-aae2-7fb931ca4c4f",
 at,
 false,
 )
 \`\`\`

 Always format the date input as 'YYYY-MM-DDTHH:MM:SS+00:00' (e.g., 2024-04-22T15:28:03.123456Z).

\`\`\`json Example response theme={"system"}
{
 "balance": {
 "balance": 9620000,
 "balance\_id": "bin\_be16c4a1-b5a6-4b64-a733-de2f6b24813d",
 "credit\_balance": 9620000,
 "currency": "USD",
 "debit\_balance": 0,
 "ledger\_id": ""
 },
 "timestamp": "2025-02-24T08:55:26.976106Z"
}
\`\`\`

\\*\\*\\*

\## How it works

When querying for a balance at a specific timestamp, Blnk follows these steps to ensure accuracy and reliability:

1\. \*\*Identify the most recent snapshot:\*\* The system retrieves the most recent snapshot taken before the requested timestamp. Snapshots are daily records of balances, captured manually by users as described in the Balance Snapshots feature.

 If no snapshot is found, Blnk builds the historical balance from genesis (using transactions only).

2\. \*\*Apply intervening transactions:\*\* It then applies all transactions that occurred between the time of the snapshot and the requested timestamp. This reconstructs the balance state by accounting for any credits, debits, or other financial activities that took place during that period.

3\. \*\*Return the computed balance:\*\* The final computed balance, reflecting the exact state at the requested timestamp, is returned in the response.

 Point-in-time copies of running balances.

\### Reconstruct balances from source

Available on version 0.10.1 or later.

You can choose to bypass balance snapshots and directly reconstruct your balances from their transactions alone.

To do this, include the query parameter \`from\_source=true\` in your request URL:

 \`\`\`bash cURL wrap theme={"system"}
 curl -X GET "http://YOUR\_BLNK\_INSTANCE\_URL/balances/{balance\_id}/at?timestamp=2024-04-22T15:28:03.123456Z&from\_source=true" \
 -H "X-blnk-key: "
 \`\`\`

 \`\`\`go Go wrap theme={"system"}
 at, \_ := time.Parse(time.RFC3339, "2024-04-22T15:28:03.123456Z")
 balance, resp, err := client.LedgerBalance.GetHistorical(
 "bln\_5ce86029-3c2e-4e2a-aae2-7fb931ca4c4f",
 at,
 true,
 )
 \`\`\`

\`\`\`json Response theme={"system"}
{
 "balance": {
 "balance": 9620000,
 "balance\_id": "bin\_be16c4a1-b5a6-4b64-a733-de2f6b24813d",
 "credit\_balance": 9620000,
 "currency": "USD",
 "debit\_balance": 0,
 "ledger\_id": ""
 },
 "timestamp": "2025-02-24T08:55:26.976106Z"
}
\`\`\`

\\*\\*\\*

\## Need help?

We are very happy to help you make the most of Blnk, regardless of whether it is your first time or you are switching from another tool.

To ask questions or discuss issues, please \[contact us\](mailto:support@blnkfinance.com) or \[join our Discord community\](https://discord.gg/7WNv94zPpx).

 Sign up and manage your ledger with our back-office dashboard. You can invite teammates to collaborate and manage your ledger operations directly from the dashboard.