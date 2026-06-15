\> ## Documentation Index
\> Fetch the complete documentation index at: https://docs.blnkfinance.com/llms.txt
\> Use this file to discover all available pages before exploring further.

\# Balance Monitoring

\> Monitor balances and get notified via webhook when they meet set conditions.

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

Balance monitors let you keep track of balances in your Blnk Ledger. This is useful for scenarios where balances should meet specific thresholds.

You can monitor all 3 sub-balances of a ledger balance — credit balance (\`credit\_balance\`), debit balance (\`debit\_balance\`) and total balance (\`balance\`).

\*\*Let's get started ✨\*\*

What we'll cover …

1\. \[Why monitor balances?\](#1-why-monitor-balances)
2\. \[Set up balance monitors\](#2-set-up-balance-monitors)
3\. \[Supported operators\](#3-supported-operators)
4\. \[Manage existing balance monitors\](#4-manage-existing-balance-monitors)

\\*\\*\\*

\## 1: Why monitor balances?

1\. \*\*Fraud detection:\*\* Unusual balance changes can be an early indication of fraudulent activities. Monitoring can trigger alerts for suspicious transactions and ensure timely intervention.

2\. \*\*Regulatory compliance:\*\* Many financial regulations require institutions to maintain specific balance thresholds. Real-time balance monitoring makes it easy to comply with these regulations.

3\. \*\*Customer notifications:\*\* Customers can be notified in real-time if their balance crosses a specific threshold. It can also be used for segmenting your customers in your application.

4\. \*\*Operational efficiency:\*\* Instantly knowing when a balance reaches a certain threshold can trigger automatic actions, such as transferring funds between accounts or purchasing assets.

\\*\\*\\*

\## 2: Set up balance monitors

To set up balance monitoring, you need to determine your conditions, e.g., notify me when the \`debit\_balance\` is above 100000.

Next, call the \*\*create-balance-monitor\*\* endpoint and provide the following request body:

 \`\`\`bash cURL wrap theme={"system"}
 curl -X POST "http://localhost:5001/balance-monitors" \
 -H "X-Blnk-Key: " \
 -H "Content-Type: application/json" \
 -d '{
 "balance\_id": "bln\_0be360ca-86fe-457d-be43-daa3f966d8f0",
 "condition": {
 "field": "debit\_balance",
 "operator": ">",
 "value": 1000,
 "precision": 100
 },
 "description": "Tier 1 Account"
 }'
 \`\`\`

 \`\`\`javascript TypeScript wrap theme={"system"}
 const response = await blnk.BalanceMonitor.create({
 balance\_id: 'bln\_0be360ca-86fe-457d-be43-daa3f966d8f0',
 condition: {
 field: 'debit\_balance',
 operator: '>',
 value: 1000,
 precision: 100,
 },
 description: 'Tier 1 Account',
 });
 \`\`\`

 \`\`\`go Go wrap theme={"system"}
 monitor, resp, err := client.BalanceMonitor.Create(blnkgo.MonitorData{
 BalanceID: "bln\_0be360ca-86fe-457d-be43-daa3f966d8f0",
 Condition: blnkgo.MonitorCondition{
 Field: "debit\_balance",
 Operator: blnkgo.OperatorGreaterThan,
 Value: 1000,
 Precision: 100,
 },
 Description: "Tier 1 Account",
 })
 \`\`\`

\| Field \| Description \| Required? \| Type \|
\| :\-\-\-\-\-\-\-\-\-\-\-\- \| :\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\- \| :\-\-\-\-\-\-\-\- \| :\-\-\-\-\-\-\- \|
\| \`balance\_id\` \| Unique identifier of the balance to be monitored. \| Yes \| \`string\` \|
\| \`condition\` \| Object representing the condition to be satisfied. \| Yes \| \|
\| \`field\` \| Specific sub-balance to monitor. It can be \`debit\_balance\`, \`credit\_balance\`, or \`balance\`. \| Yes \| \`string\` \|
\| \`operator\` \| Indicates the comparison operation to be performed between the field and operator. \*\*See below → \[Supported operators\](#supported-operators).\*\* \| Yes \| \`string\` \|
\| \`value\` \| The value against which the \`field\` is compared. \| Yes \| \`int64\` \|
\| \`precision\` \| Converts the value to lowest possible unit. Ensure that the precision specified is the same as the precision applied to the balance \| Yes \| \`int64\` \|
\| \`description\` \| Description of your balance monitor. It is left empty if it's not passed in the request. \| No \| \`string\` \|

In this example, Blnk is asked to monitor the balance and send a notification when its \`debit\_balance\` is greater than 100000.

 You can also include a \`meta\_data\` in your request if you need to add custom data to your balance monitor.

Once the request is received, Blnk stores your balance monitor with a unique \`monitor\_id\`. When the condition is met, you will get instantly notified via the \`balance.monitor\` webhook event.

\`\`\`json Response theme={"system"}
{
 "monitor\_id": "mon\_e0e77b0c-4985-472a-9bf5-76a48b0259b0",
 "balance\_id": "bln\_0be360ca-86fe-457d-be43-daa3f966d8f0",
 "condition": {
 "field": "debit\_balance",
 "operator": ">",
 "value": 1000000
 },
 "description": "Tier 1 Account",
 "created\_at": "2024-02-20T05:56:58.257315054Z"
}
\`\`\`

\| Field \| Description \| Type \|
\| :\-\-\-\-\-\-\-\-\-\-\- \| :\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\- \| :\-\-\-\-\-\-\- \|
\| \`monitor\_id\` \| Unique identifier for your balance monitor. \| \`string\` \|
\| \`created\_at\` \| Date and time of creation. \| \`string\` \|

\\*\\*\\*

\## 3: Supported operators

This is a list of all supported operators by the Balance monitor:

\| Operators \| Symbol \| Description \|
\| :\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\- \| :\-\-\-\-\- \| :\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\-\- \|
\| Greater than \| > \| Checks if the specified balance in \`field\` is greater than \`value\` \|
\| Less than \| \\< \| Checks if the specified balance in \`field\` is less than \`value\` \|
\| Equal to \| = \| Checks if the specified balance in \`field\` is exactly equal to \`value\` \|
\| Not equal to \| != \| Checks if the specified balance in \`field\` is not equal to \`value\` \|
\| Greater than or equal to \| >= \| Checks if the specified balance in \`field\` is greater than or equal to \`value\` \|
\| Less than or equal to \| \\<= \| Checks if the specified balance in \`field\` is less than or equal to \`value\` \|

\\*\\*\\*

\## 4: Manage existing balance monitors

You can request to view or update the details of a particular monitor.

To view balance monitors, call the \*\*get-balance-monitor\*\* endpoint:

 \`\`\`bash cURL wrap theme={"system"}
 curl -X GET "http://YOUR\_BLNK\_INSTANCE\_URL/balance-monitors/{monitor-id}" \
 -H "X-blnk-key: "
 \`\`\`

 \`\`\`javascript TypeScript wrap theme={"system"}
 const response = await blnk.BalanceMonitor.get('{monitor-id}');
 \`\`\`

 \`\`\`go Go wrap theme={"system"}
 monitor, resp, err := client.BalanceMonitor.Get("mon\_e0e77b0c-4985-472a-9bf5-76a48b0259b0")
 \`\`\`

\`\`\`json Response theme={"system"}
{
 "monitor\_id": "mon\_e0e77b0c-4985-472a-9bf5-76a48b0259b0",
 "balance\_id": "bln\_0be360ca-86fe-457d-be43-daa3f966d8f0",
 "condition": {
 "field": "debit\_balance",
 "operator": ">",
 "value": 100000
 },
 "description": "Tier 1 Account",
 "created\_at": "2024-02-20T05:56:58.257315054Z"
}
\`\`\`

 List all balance monitors:

 \`\`\`bash cURL wrap theme={"system"}
 curl -X GET "http://YOUR\_BLNK\_INSTANCE\_URL/balance-monitors" \
 -H "X-blnk-key: "
 \`\`\`

 \`\`\`javascript TypeScript wrap theme={"system"}
 const response = await blnk.BalanceMonitor.list();
 \`\`\`

 \`\`\`go Go wrap theme={"system"}
 monitors, resp, err := client.BalanceMonitor.List()
 \`\`\`

\`\`\`json Response theme={"system"}
\[\
 {\
 "monitor\_id": "mon\_e0e77b0c-4985-472a-9bf5-76a48b0259b0",\
 "balance\_id": "bln\_0be360ca-86fe-457d-be43-daa3f966d8f0",\
 "condition": {\
 "field": "debit\_balance",\
 "operator": ">",\
 "value": 100000\
 },\
 "description": "Tier 1 Account",\
 "created\_at": "2024-02-20T05:56:58.257315054Z"\
 }\
\]
\`\`\`

To update a balance monitor, call the \*\*update-balance-monitor\*\* endpoint and provide the updated conditions in the request body:

 \`\`\`bash cURL wrap theme={"system"}
 curl -X PUT "http://YOUR\_BLNK\_INSTANCE\_URL/balance-monitors/{monitor-id}" \
 -H "X-blnk-key: " \
 -H "Content-Type: application/json" \
 -d '{
 "balance\_id": "bln\_0be360ca-86fe-457d-be43-daa3f966d8f0",
 "condition": {
 "field": "debit\_balance",
 "operator": ">",
 "value": 100000
 },
 "description": "Tier 1 Account"
 }'
 \`\`\`

 \`\`\`javascript TypeScript wrap theme={"system"}
 const response = await blnk.BalanceMonitor.update(
 'mon\_e0e77b0c-4985-472a-9bf5-76a48b0259b0',
 {
 balance\_id: 'bln\_0be360ca-86fe-457d-be43-daa3f966d8f0',
 condition: {
 field: 'debit\_balance',
 operator: '>',
 value: 100000,
 },
 description: 'Tier 1 Account',
 },
 );
 \`\`\`

 \`\`\`go Go wrap theme={"system"}
 monitor, resp, err := client.BalanceMonitor.Update("mon\_e0e77b0c-4985-472a-9bf5-76a48b0259b0", blnkgo.MonitorData{
 BalanceID: "bln\_0be360ca-86fe-457d-be43-daa3f966d8f0",
 Condition: blnkgo.MonitorCondition{
 Field: "debit\_balance",
 Operator: blnkgo.OperatorGreaterThan,
 Value: 100000,
 },
 Description: "Tier 1 Account",
 })
 \`\`\`

\`\`\`json Response theme={"system"}
{
 "monitor\_id": "mon\_e0e77b0c-4985-472a-9bf5-76a48b0259b0",
 "balance\_id": "bln\_0be360ca-86fe-457d-be43-daa3f966d8f0",
 "condition": {
 "field": "debit\_balance",
 "operator": ">",
 "value": 100000
 },
 "description": "Tier 1 Account",
 "created\_at": "2024-02-20T05:56:58.257315054Z"
}
\`\`\`

\\*\\*\\*

\## Need help?

We are very happy to help you make the most of Blnk, regardless of whether it is your first time or you are switching from another tool.

To ask questions or discuss issues, please \[contact us\](mailto:support@blnkfinance.com) or \[join our Discord community\](https://discord.gg/7WNv94zPpx).

 Sign up and manage your ledger with our back-office dashboard. You can invite teammates to collaborate and manage your ledger operations directly from the dashboard.