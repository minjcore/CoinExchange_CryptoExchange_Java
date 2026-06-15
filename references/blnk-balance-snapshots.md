\> ## Documentation Index
\> Fetch the complete documentation index at: https://docs.blnkfinance.com/llms.txt
\> Use this file to discover all available pages before exploring further.

\# Balance Snapshots

\> Capture and manage periodical balance snapshots for accurate financial reporting.

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

Blnk Finance offers a balance snapshotting feature that enables users to access historical balance data at any specific point in time.

This ensures precise financial reporting and analysis while maintaining efficient storage and retrieval systems.

\\*\\*\\*

\## Quick overview

\\* \*\*Purpose:\*\* Records a daily snapshot of each balance to preserve historical data, enabling users to review past financial states for auditing, reporting, or analytical purposes.

\\* \*\*Frequency:\*\*
 \\* Blnk takes one snapshot per day per balance when triggered.
 \\* If the snapshot is called multiple times in a single day, it records only the first instance, capturing the state of all balances at that time.
 \\* Subsequent calls on the same day will not create additional snapshots for balances already captured but will record snapshots for any new balances that lack a snapshot for that day.
 \\* The next snapshot for all balances can only be captured on the following day.

\\* \*\*Recommended timing:\*\* It’s best to take snapshots at midnight or at the user’s defined end-of-day period for consistency, as this minimizes disruptions and aligns with typical business closing times, providing a clear picture of daily financial positions.

 Users have flexibility in determining when to trigger snapshots, but each balance is limited to one snapshot per day.

\\*\\*\\*

\## Triggering a snapshot

Call the \[Balance Snapshots\](/reference/balances-snapshots) endpoint. This action initiates a snapshot of all balances, capturing their current state at the time of the request for future reference.

 \`\`\`bash cURL wrap theme={"system"}
 curl -X POST "http://YOUR\_BLNK\_INSTANCE\_URL/balances-snapshots" \
 -H "X-blnk-key: "
 \`\`\`

\`\`\`json Response theme={"system"}
{
 "message": "Snapshottings in progress. Should be completed shortly."
}
\`\`\`

 Historical balances from stored snapshots.

\\*\\*\\*

\## Key considerations

\\* \*\*Daily snapshots and reconstruction:\*\* Since snapshots are taken once per day per balance, balances between snapshots are reconstructed using transaction data.

\\* \*\*Optimal snapshot timing:\*\* Choosing the right time to trigger snapshots is crucial for maintaining consistency and accuracy. Users should initiate snapshots at their preferred end-of-day period, such as midnight or another consistent business closing time.

\\*\\*\\*

\## Need help?

We are very happy to help you make the most of Blnk, regardless of whether it is your first time or you are switching from another tool.

To ask questions or discuss issues, please \[contact us\](mailto:support@blnkfinance.com) or \[join our Discord community\](https://discord.gg/7WNv94zPpx).

 Sign up and manage your ledger with our back-office dashboard. You can invite teammates to collaborate and manage your ledger operations directly from the dashboard.