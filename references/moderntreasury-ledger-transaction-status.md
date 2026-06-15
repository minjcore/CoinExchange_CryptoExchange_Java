[Journal](https://www.moderntreasury.com/resources/journal)

•February 10, 2022

# The Ledger Transaction Status

It can be challenging to differentiate between money that’s in flight and settled. Ledger transactions have a status field that can help with this.

![Image of Koji Murase ](https://www.moderntreasury.com/_next/image?url=https%3A%2F%2Fcdn.sanity.io%2Fimages%2F8nmbzj0x%2Fproduction%2F39ad2878cbcce82b27a259594665f9e55b454e8e-258x286.png&w=64&q=75)

Koji Murase / Product

Contents

Explore With AI

Topics

[Changelog](https://www.moderntreasury.com/resources/changelog)

When you’re building a product that moves money, it can be challenging to differentiate between money that’s in flight and settled.

[Ledgers](https://www.moderntreasury.com/products/ledgers) lets you track money movement in your application. Ledger transactions have a `status` field reflecting whether a transaction is in progress, cancelled, or completed.

## Creating a Ledger Transaction

Say a user earns $100 from selling products on your marketplace application. You can use a [ledger account](https://docs.moderntreasury.com/reference#ledger-account-object) to track this user’s wallet balance as they accumulate earnings.

When the user decides to withdraw their $100 balance, your application should record the payout in your ledger so that you know the user’s balance has been zeroed out. To actually move the money, you can use the [Payment Orders](https://docs.moderntreasury.com/reference#payment-orders) API to initiate an ACH payment.

When the payment is initiated, you would create a ledger transaction with a `status` of `pending`. This specifies that funds are in the process of moving out of the user’s application balance. Even though funds haven’t moved yet, recording a pending transaction is good practice. It establishes an audit trail and makes sure funds are not accidentally overdrawn from the user’s account.

## Updating a Ledger Transaction Status

While a transaction is `pending`, it is mutable, meaning you can modify it with a PATCH request. You can also cancel the transaction by setting its status to `archived`. You might do this if a payment fails.

When a payment is settled, you can change the corresponding ledger transaction’s status to `posted`. Once a ledger transaction posts it is immutable, meaning its status and entries cannot be modified.

If you’re using a ledger transaction to track a Modern Treasury payment, you can automate this process of updating the transaction status. If you tie the ledger transaction to the Modern Treasury payment object, the ledger transaction status will automatically update in parallel with the status of the payment object. Once a ledger transaction is tied to another object, you can no longer manually update its status. For more on how to link ledger transactions to other Modern Treasury objects, see [here](https://docs.moderntreasury.com/docs/linking-to-other-modern-treasury-objects).

## Ledger Account Balances

Ledger accounts have two balances, `pending_balance` and `posted_balance`. The `posted_balance` is the sum of all posted entries on the ledger account.

The `pending_balance` is the sum of all pending _and_ posted entries on the ledger account.

In the example above, while the user’s payout is in progress their wallet ledger account would have a `posted_balance` of $100 (because the payout has not yet posted) and a `pending_balance` of $0.

### Get in Touch

Read more about Ledgers [here](https://www.moderntreasury.com/journal/announcing-ledgers). If you’re curious about how Modern Treasury can help your business track and move money, get in touch with us [here](https://www.moderntreasury.com/products/ledgers).

Subscribe to our newsletter

Get the latest articles, guides, and insights delivered to your inbox.

Company Email\*

Subscribe

## Authors

![Image of Koji Murase ](https://www.moderntreasury.com/_next/image?url=https%3A%2F%2Fcdn.sanity.io%2Fimages%2F8nmbzj0x%2Fproduction%2F39ad2878cbcce82b27a259594665f9e55b454e8e-258x286.png&w=640&q=75)

Koji Murase Product

Related

## Changelog

[View topic →](https://www.moderntreasury.com/resources/changelog)

[![Image for Announcing Stablecoin Payment Accounts](https://www.moderntreasury.com/_next/image?url=https%3A%2F%2Fcdn.sanity.io%2Fimages%2F8nmbzj0x%2Fproduction%2Fc9cd390dec71cda2e36b4bbe99ad113a5710e277-3072x1728.png&w=3840&q=75)](https://www.moderntreasury.com/journal/announcing-stablecoin-payment-accounts)

[Journal](https://www.moderntreasury.com/resources/journal) [Announcing Stablecoin Payment Accounts](https://www.moderntreasury.com/journal/announcing-stablecoin-payment-accounts)

[![Image for Introducing the Modern Treasury MCP Server](https://www.moderntreasury.com/_next/image?url=https%3A%2F%2Fcdn.sanity.io%2Fimages%2F8nmbzj0x%2Fproduction%2F038fb4890c80e3b6b77643004c35b5275b44d20e-3072x1728.png&w=3840&q=75)](https://www.moderntreasury.com/journal/introducing-the-modern-treasury-mcp-server)

[Journal](https://www.moderntreasury.com/resources/journal) [Introducing the Modern Treasury MCP Server](https://www.moderntreasury.com/journal/introducing-the-modern-treasury-mcp-server)

[![Image for Implementing Multi-Factor Authentication For Modern Treasury Users](https://www.moderntreasury.com/_next/image?url=https%3A%2F%2Fcdn.sanity.io%2Fimages%2F8nmbzj0x%2Fproduction%2F8a9eb8fbec2e5422ad5ef8c092ef69eed546d77f-3072x1728.png&w=3840&q=75)](https://www.moderntreasury.com/journal/implementing-mfa-for-modern-treasury-users)

[Journal](https://www.moderntreasury.com/resources/journal) [Implementing Multi-Factor Authentication For Modern Treasury Users](https://www.moderntreasury.com/journal/implementing-mfa-for-modern-treasury-users)

[![Image for July/August Changelog](https://www.moderntreasury.com/_next/image?url=https%3A%2F%2Fcdn.sanity.io%2Fimages%2F8nmbzj0x%2Fproduction%2F3044b87c39244f688c7b9a5f8f6315e0509ba809-3200x1800.png&w=3840&q=75)](https://www.moderntreasury.com/journal/july-august-changelog-2024)

[Journal](https://www.moderntreasury.com/resources/journal) [July/August Changelog](https://www.moderntreasury.com/journal/july-august-changelog-2024)

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