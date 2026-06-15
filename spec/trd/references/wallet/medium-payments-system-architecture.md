[Sitemap](https://medium.com/sitemap/sitemap.xml)

[Open in app](https://play.google.com/store/apps/details?id=com.medium.reader&referrer=utm_source%3DmobileNavBar&source=post_page---top_nav_layout_nav-----------------------------------------)

Sign up

[Sign in](https://medium.com/m/signin?operation=login&redirect=https%3A%2F%2Fmedium.com%2F%40austinmcorso%2Fsoftware-architecture-payments-system-afc19c717a42&source=post_page---top_nav_layout_nav-----------------------global_nav------------------)

[Medium Logo](https://medium.com/?source=post_page---top_nav_layout_nav-----------------------------------------)

Get app

[Write](https://medium.com/m/signin?operation=register&redirect=https%3A%2F%2Fmedium.com%2Fnew-story&source=---top_nav_layout_nav-----------------------new_post_topnav------------------)

[Search](https://medium.com/search?source=post_page---top_nav_layout_nav-----------------------------------------)

Sign up

[Sign in](https://medium.com/m/signin?operation=login&redirect=https%3A%2F%2Fmedium.com%2F%40austinmcorso%2Fsoftware-architecture-payments-system-afc19c717a42&source=post_page---top_nav_layout_nav-----------------------global_nav------------------)

![Unknown user](https://miro.medium.com/v2/resize:fill:32:32/1*dmbNkD5D-u45r44go_cf0g.png)

# Software Architecture — Payments System

[![Austin Corso](https://miro.medium.com/v2/resize:fill:32:32/1*dmbNkD5D-u45r44go_cf0g.png)](https://medium.com/@austinmcorso?source=post_page---byline--afc19c717a42---------------------------------------)

[Austin Corso](https://medium.com/@austinmcorso?source=post_page---byline--afc19c717a42---------------------------------------)

Follow

4 min read

·

Mar 10, 2020

3

[Listen](https://medium.com/m/signin?actionUrl=https%3A%2F%2Fmedium.com%2Fplans%3Fdimension%3Dpost_audio_button%26postId%3Dafc19c717a42&operation=register&redirect=https%3A%2F%2Fmedium.com%2F%40austinmcorso%2Fsoftware-architecture-payments-system-afc19c717a42&source=---header_actions--afc19c717a42---------------------post_audio_button------------------)

Share

_Originally published at_ [_http://austincorso.com_](http://austincorso.com/2020/03/10/payments-system.html) _on March 10, 2020 as part of the_ [_Software Architecture Series_](http://austincorso.com/blog/) _._

In this system design architecture post we will design a payments system. Payments systems are found across the internet for (1) maintaining a ledger of accounts, balances, and transactions; and (2) the processing of financial transactions between individuals, businesses, and banks. Though simple on the surface to customers, payment systems are complex with many failure scenarios, edge cases, and critical customer and business impact if things go wrong. These systems are critical to each party’s financial interests and their trust in a software platform.

## Get Austin Corso’s stories in your inbox

Join Medium for free to get updates from this writer.

Subscribe

Subscribe

Remember me for faster sign in

When designing such a complex system where data consistency is of utmost importance we will emphasis designing for (1) data consistency and durability, (2) double-entry accounting (every transaction between two parties is zero-sum), (3) idempotency and exactly-once processing, and (4) immutability.

### Design Principles:

1. Data Consistency and Durability
2. Double-Entry Accounting
3. Idempotency and Exactly Once Processing
4. Immutability

Lets dive into user and technical requirements, followed by high-level-design, user and data flows, persistence, performance, security, and edge cases.

## Requirements

There are multiple areas of concern we will need to consider to detail out the user and technical requirements. We will need to manage accounts billable, maintain an authoritative ledger, a risk analysis engine to detect and block fraudulent activity, and a payments processing gateway to encapsulate various payment processors and provide availability if one is unavailable. As many businesses do, payment processing we will offload to third-party services specialized in these bank transactions with established relationships and integrations in place. This reduces our security and compliance requirements as well. As a platform, our payments system will hold a wallet for each user and support both a pay-in flow to add funds and a pay-out flow to receive funds. We will support real-time and scheduled payments. Let’s take a closer look at these business requirements.

### User Requirements:

1. Extremely high durability (99.9999%) and strong consistency, no amount of data loss or inconsistency is acceptable
2. Scalable to 5 million transactions daily (~50 transactions per second)
3. Support for multiple payment options — bank checking account, credit card, Paypal, and Apple Pay / Google Pay
4. Support for multiple plug-and-play third-party payment processing integrations
5. Support both for submitting payments to the platform and receiving payments from the platform
6. Support for payments triggered by user request or using automated scheduled payments

### Out-of-Scope:

1. Currency exchange
2. Taxes
3. Analytics and data processing (see [Metrics post](http://austincorso.com/2020/05/17/metrics-alarms-monitoring-observability-system.html))
4. Multi-region and disaster recovery

Given we require scaling this solution to 5 million transactions a day, let’s evaluate what the throughput requirement is for each key access pattern. We will assume a 2-to-1 ratio of requests viewing funds and past payments versus submitting new payments, whether pay-in or pay-out. 5 million requests per day comes to about 50 transactions-per-second (TPS). This is not an overly massive volume of traffic and should be easily achievable with proper design.

### Access Patterns:

1. View funds and past payments (33.3 TPS, read)
2. Submit payment, either pay-in or pay-out (16.7 TPS, write)

## High Level Design

With those requirements in mind, let’s consider the components and services we will need to design. All the services within this payments system will fall under the Payments domain. Following domain-driven-design and cellular architecture we expose one service from the Payments domain, the Payment Platform Service. Internally the Payment Platform Service will integrate with multiple internal services with separate areas of responsibility. This allows separate services to be deployed independently with isolated availability, whereas the internal concepts are abstracted away from clients external to the payments domain.

### Components:

01. \[External\] Edge Router — network firewall, DDOS protection, authentication, block list
02. \[External\] Payment Platform Service — external APIs supporting payments between parties
03. \[Internal\] Risk Engine — rules-based service for evaluating risk-level of a payment request
04. \[Internal\] Payment Processing Service — abstraction layer over individual pluggable payment processing integrations, responsible for processing a single payment
05. \[Internal\] Payment Scheduler — service for triggering automated payments on configurable frequencies
06. \[Internal\] Event Log — write-ahead append-only log of events
07. \[Internal\] Ledger — source of truth for transactions using double-entry accounting
08. \[Internal\] Wallet — maintains account balances for parties
09. \[Internal\] Reconciliation Processor — reconciles PSP settlements against internal ledger
10. \[3rd-Party\] Payment Service Providers (PSP) — third-party services for processing payments

Press enter or click to view image in full size

![](https://miro.medium.com/v2/resize:fit:700/0*WPJSZBQkqNQTtOLr.png)

> Continue reading at [http://austincorso.com/2020/03/10/payments-system.html](http://austincorso.com/2020/03/10/payments-system.html)

_Originally published at_ [_http://austincorso.com_](http://austincorso.com/2020/03/10/payments-system.html) _on March 10, 2020._

[Payments](https://medium.com/tag/payments?source=post_page-----afc19c717a42---------------------------------------)

[System Design Interview](https://medium.com/tag/system-design-interview?source=post_page-----afc19c717a42---------------------------------------)

[Ledger](https://medium.com/tag/ledger?source=post_page-----afc19c717a42---------------------------------------)

[Digital Wallet](https://medium.com/tag/digital-wallet?source=post_page-----afc19c717a42---------------------------------------)

[Payment Processing](https://medium.com/tag/payment-processing?source=post_page-----afc19c717a42---------------------------------------)

[![Austin Corso](https://miro.medium.com/v2/resize:fill:48:48/1*dmbNkD5D-u45r44go_cf0g.png)](https://medium.com/@austinmcorso?source=post_page---post_author_info--afc19c717a42---------------------------------------)

[![Austin Corso](https://miro.medium.com/v2/resize:fill:64:64/1*dmbNkD5D-u45r44go_cf0g.png)](https://medium.com/@austinmcorso?source=post_page---post_author_info--afc19c717a42---------------------------------------)

Follow

[**Written by Austin Corso**](https://medium.com/@austinmcorso?source=post_page---post_author_info--afc19c717a42---------------------------------------)

[64 followers](https://medium.com/@austinmcorso/followers?source=post_page---post_author_info--afc19c717a42---------------------------------------)

· [67 following](https://medium.com/@austinmcorso/following?source=post_page---post_author_info--afc19c717a42---------------------------------------)

[http://austincorso.com](http://austincorso.com/) [https://www.linkedin.com/in/austincorso](https://www.linkedin.com/in/austincorso)

Follow

[Help](https://help.medium.com/hc/en-us?source=post_page-----afc19c717a42---------------------------------------)

[Status](https://status.medium.com/?source=post_page-----afc19c717a42---------------------------------------)

[About](https://medium.com/about?autoplay=1&source=post_page-----afc19c717a42---------------------------------------)

[Careers](https://medium.com/jobs-at-medium/work-at-medium-959d1a85284e?source=post_page-----afc19c717a42---------------------------------------)

[Press](mailto:pressinquiries@medium.com)

[Blog](https://blog.medium.com/?source=post_page-----afc19c717a42---------------------------------------)

[Store](https://medium.com/store)

[Privacy](https://policy.medium.com/medium-privacy-policy-f03bf92035c9?source=post_page-----afc19c717a42---------------------------------------)

[Rules](https://policy.medium.com/medium-rules-30e5502c4eb4?source=post_page-----afc19c717a42---------------------------------------)

[Terms](https://policy.medium.com/medium-terms-of-service-9db0094a1e0f?source=post_page-----afc19c717a42---------------------------------------)

[Text to speech](https://speechify.com/medium?source=post_page-----afc19c717a42---------------------------------------)

reCAPTCHA

Recaptcha requires verification.

protected by **reCAPTCHA**