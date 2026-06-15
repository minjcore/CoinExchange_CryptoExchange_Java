[Journal](https://www.moderntreasury.com/resources/journal)

•October 7, 2025

# Announcing Instant Payments Statuses

We’re updating how payment order states appear in Modern Treasury to better align with real-world settlement and reconciliation.

![Image of Rob Kingscote](https://www.moderntreasury.com/_next/image?url=https%3A%2F%2Fcdn.sanity.io%2Fimages%2F8nmbzj0x%2Fproduction%2Fc6957045204a5e73d185487e483cc89be9e86367-512x512.jpg&w=64&q=75)

Rob Kingscote/ Product Manager

Contents

Explore With AI

Topics

[Faster Payments](https://www.moderntreasury.com/resources/faster-payments)

## A Clearer View of Payment States

We’re updating how payment order states appear in Modern Treasury to better align with real-world settlement and reconciliation. This change will make it easier for customers to build downstream processes that reflect what’s happening in real time.

Until now, the `completed` state was triggered only after reconciliation, which sometimes lagged behind the actual transfer of funds. Going forward, `completed` will reflect confirmation directly from the bank or network, while a new `reconciled` event will indicate when a payment has been fully reconciled.

This change applies consistently across payment orders, returns, reversals, and incoming payments.

## What’s New

- **Faster confirmation:** The `completed` event will now trigger when the bank confirms that a payment has been posted to your account.
- **Dedicated reconciliation signal:** A new `reconciled` event indicates when a payment has been fully reconciled with a posted transaction. This webhook is already live to support migration.
- **Improved handling of returns:** When a payment is `returned`, you’ll immediately see both `completed` and `returned` events, instead of waiting until reconciliation.

## Accurately Reflecting Real-Time Money Movement

With this update, you can now act on events that more accurately reflect real-time money movement. This is especially important for instant payments, where funds complete in seconds but reconciliation details follow later.

By separating completion from reconciliation, you gain:

- **Faster visibility** into real-world payment states.
- **Cleaner downstream logic**, with distinct events for completion and reconciliation.
- **Consistent event behavior** across different payment types.

## Rollout Timeline

We’re rolling this out in stages:

- **New reconciliation attribute** → First, we will introduce the new back end reconciled attribute unlocking more performant reconciliation processing. This will have no impact on customers.
- **Instant payments (RTP)** → We will migrate instant payment customers, unlocking a truly real-time experience: a completed signal in ~10 seconds.
- **Wires** → Moving wires and enabling earlier completed signals where available from banks will create a more responsive wires experience.
- **All other payment types (ACH, check, etc.)** → We will migrate all payment types to have a distinct reconciled and complete signal, opting to refine how complete is triggered at a later date.

| Payment Type | Migration Starting |
| --- | --- |
| Back-end reconciliation attribute | October 6, 2025 |
| RTP | October 7, 2025 |
| Wires | November 10, 2025 |
| Remaining Rails (ACH, check, etc.) | November 25, 2025 |

## How This Impacts You

- If your use case relies on **reconciliation details**(e.g., transaction IDs), use the new `reconciled` webhook.
- If you need **bank confirmation**, continue using `completed`.
- If you previously subscribed to both `tentatively_reconciled` and `completed`, you should now rely on `tentatively_reconciled` and `reconciled`.
- Auto-ledgering rules will now trigger on `reconciled` rather than `completed`.
- The **Reconciliation Status** for payment orders will be available in the UI, API, and push to warehouse. These will appear ahead of the migration.

## Next Steps

- Review your webhook subscriptions and update any downstream logic where necessary.
- Test with the new reconciled webhook (available now).
- Connect with your Customer Success Manager for specific migration timelines.

## Get Started

Modern Treasury is committed to making payment states more accurate, real-time, and actionable for our customers. If you’d like guidance on migrating to the new model, [reach out to us](https://www.moderntreasury.com/talk-to-us) or visit our [documentation](https://docs.moderntreasury.com/).

Subscribe to our newsletter

Get the latest articles, guides, and insights delivered to your inbox.

Company Email\*

Subscribe

## Authors

![Image of Rob Kingscote](https://www.moderntreasury.com/_next/image?url=https%3A%2F%2Fcdn.sanity.io%2Fimages%2F8nmbzj0x%2Fproduction%2Fc6957045204a5e73d185487e483cc89be9e86367-512x512.jpg&w=1080&q=75)

Rob KingscoteProduct Manager

Related

## Faster Payments

[View topic →](https://www.moderntreasury.com/resources/faster-payments)

[![Image for What are QR Code Payments?](https://www.moderntreasury.com/_next/image?url=https%3A%2F%2Fcdn.sanity.io%2Fimages%2F8nmbzj0x%2Fproduction%2F5ff182a730bb79a502e57bf633f0d1d4a919b6e8-1600x900.jpg&w=3840&q=75)](https://www.moderntreasury.com/learn/qr-code-payments)

[Glossary](https://www.moderntreasury.com/resources/glossary) [What are QR Code Payments?](https://www.moderntreasury.com/learn/qr-code-payments)

[![Image for What is RTP?](https://www.moderntreasury.com/_next/image?url=https%3A%2F%2Fcdn.sanity.io%2Fimages%2F8nmbzj0x%2Fproduction%2F21c72167a6fe33298b48fa75a2956319aeb3fbbe-1600x900.jpg&w=3840&q=75)](https://www.moderntreasury.com/learn/what-is-rtp)

[Glossary](https://www.moderntreasury.com/resources/glossary) [What is RTP?](https://www.moderntreasury.com/learn/what-is-rtp)

[![Image for Modern Treasury Payments: One API for Fiat and Stablecoins](https://www.moderntreasury.com/_next/image?url=https%3A%2F%2Fcdn.sanity.io%2Fimages%2F8nmbzj0x%2Fproduction%2F7414890ed52e05021ada7bb72df0120a73d3087c-2400x1350.png&w=3840&q=75)](https://www.moderntreasury.com/resources/videos/modern-treasury-payments-one-api-for-fiat-and-stablecoins)

[Videos](https://www.moderntreasury.com/resources/videos) [Modern Treasury Payments: One API for Fiat and Stablecoins](https://www.moderntreasury.com/resources/videos/modern-treasury-payments-one-api-for-fiat-and-stablecoins)

[![Image for What is an Open Loop Payment System?](https://www.moderntreasury.com/_next/image?url=https%3A%2F%2Fcdn.sanity.io%2Fimages%2F8nmbzj0x%2Fproduction%2Fc22d51323ff75e11f209cbc5ab8923047020531f-1600x900.jpg&w=3840&q=75)](https://www.moderntreasury.com/learn/open-loop-payment-system)

[Glossary](https://www.moderntreasury.com/resources/glossary) [What is an Open Loop Payment System?](https://www.moderntreasury.com/learn/open-loop-payment-system)

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