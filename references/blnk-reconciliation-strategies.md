\> ## Documentation Index
\> Fetch the complete documentation index at: https://docs.blnkfinance.com/llms.txt
\> Use this file to discover all available pages before exploring further.

\# Reconciliation Strategies

\> Choose the strategy that best fits your financial workflows.

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

Reconciliation strategies define the relationship between external and internal transactions.

For example, a single internal transaction may correspond to multiple external transactions, or vice versa. With reconciliation strategies, you can specify how you want run your reconciliation.

Blnk support three types of strategies:

1\. \*\*One-to-One:\*\* Ideal for straightforward payments where each external transaction has a single internal match.

2\. \*\*One-to-Many:\*\* Suited for split transactions, such as a loan repayment split across multiple internal disbursements.

3\. \*\*Many-to-One:\*\* Perfect for aggregating transactions, like daily sales combined into one internal ledger entry.

\\*\\*\\*

\## One-to-One (1:1) strategy

This matches a single external transaction directly to a single internal transaction based on your \[matching rules\](/reconciliations/matching-rules).

\`\`\`json theme={"system"}
{
 ...
 "strategy": "one\_to\_one"
}
\`\`\`

 \`grouping\_criteria\` is not needed for one-to-one.

\\*\\*\\*

\## One-to-Many (1:N) strategy

This matches a single external transaction with multiple internal transactions.

\`\`\`json theme={"system"}
{
 ...
 "strategy": "one\_to\_many",
 "grouping\_criteria": "parent\_transaction"
}
\`\`\`

 \`grouping\_criteria\` is required to combine related internal transactions before matching.

\\*\\*\\*

\## Many-to-One (N:1) strategy

This is the inverse of the One-to-Many strategy. It matches multiple external transactions to a single internal transaction.

\`\`\`json theme={"system"}
{
 ...
 "strategy": "many\_to\_one",
 "grouping\_criteria": "description"
}
\`\`\`

 \`grouping\_criteria\` is required to combine related external transactions before matching.

\\*\\*\\*

\## Choosing the right grouping criteria

Grouping criteria define how transactions are clustered before matching, ensuring that reconciliation strategies like One-to-Many and Many-to-One work correctly.

Choosing the right grouping criteria prevents incorrect matches and improves reconciliation accuracy.

1\. \*\*One-to-Many:\*\* You can group similar transactions by \`parent\_transaction\`, \`description\` or \`reference\`.

2\. \*\*Many-to-One:\*\* You can group by \`description\`.

\`\`\`json Example theme={"system"}
{
 ...
 "grouping\_criteria": "parent\_transaction"
}
\`\`\`

\### How it works

1\. Before applying matching rules, transactions are grouped using a shared attribute.
2\. Grouping ensures that unrelated transactions are not mistakenly matched.

\\*\\*\\*

\## Need help?

We are very happy to help you make the most of Blnk, regardless of whether it is your first time or you are switching from another tool.

To ask questions or discuss issues, please \[contact us\](mailto:support@blnkfinance.com) or \[join our Discord community\](https://discord.gg/7WNv94zPpx).

 Sign up and manage your ledger with our back-office dashboard. You can invite teammates to collaborate and manage your ledger operations directly from the dashboard.