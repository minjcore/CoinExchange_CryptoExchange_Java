[Sitemap](https://levelup.gitconnected.com/sitemap/sitemap.xml)

[Open in app](https://play.google.com/store/apps/details?id=com.medium.reader&referrer=utm_source%3DmobileNavBar&source=post_page---top_nav_layout_nav-----------------------------------------)

Sign up

[Sign in](https://medium.com/m/signin?operation=login&redirect=https%3A%2F%2Flevelup.gitconnected.com%2Fsystem-architecture-idempotency-in-payment-transactions-1d7888480648&source=post_page---top_nav_layout_nav-----------------------global_nav------------------)

[Medium Logo](https://medium.com/?source=post_page---top_nav_layout_nav-----------------------------------------)

Get app

[Write](https://medium.com/m/signin?operation=register&redirect=https%3A%2F%2Fmedium.com%2Fnew-story&source=---top_nav_layout_nav-----------------------new_post_topnav------------------)

[Search](https://medium.com/search?source=post_page---top_nav_layout_nav-----------------------------------------)

Sign up

[Sign in](https://medium.com/m/signin?operation=login&redirect=https%3A%2F%2Flevelup.gitconnected.com%2Fsystem-architecture-idempotency-in-payment-transactions-1d7888480648&source=post_page---top_nav_layout_nav-----------------------global_nav------------------)

![Unknown user](https://miro.medium.com/v2/resize:fill:64:64/1*dmbNkD5D-u45r44go_cf0g.png)

[**Level Up Coding**](https://levelup.gitconnected.com/?source=post_page---publication_nav-5517fd7b58a6-1d7888480648---------------------------------------)

·

Follow publication

[![Level Up Coding](https://miro.medium.com/v2/resize:fill:76:76/1*5D9oYBd58pyjMkV_5-zXXQ.jpeg)](https://levelup.gitconnected.com/?source=post_page---post_publication_sidebar-5517fd7b58a6-1d7888480648---------------------------------------)

Coding tutorials and news. The developer homepage [gitconnected.com](http://gitconnected.com/) && [skilled.dev](http://skilled.dev/) && [levelup.dev](http://levelup.dev/)

Follow publication

Member-only story

# System architecture : Idempotency in Payment Transactions

[![ScalaBrix](https://miro.medium.com/v2/resize:fill:64:64/1*bpk0nttx-BClykzEyWw2MA.png)](https://scalabrix.medium.com/?source=post_page---byline--1d7888480648---------------------------------------)

[ScalaBrix](https://scalabrix.medium.com/?source=post_page---byline--1d7888480648---------------------------------------)

Follow

9 min read

·

Apr 25, 2025

35

1

1

[Listen](https://medium.com/m/signin?actionUrl=https%3A%2F%2Fmedium.com%2Fplans%3Fdimension%3Dpost_audio_button%26postId%3D1d7888480648&operation=register&redirect=https%3A%2F%2Flevelup.gitconnected.com%2Fsystem-architecture-idempotency-in-payment-transactions-1d7888480648&source=---header_actions--1d7888480648---------------------post_audio_button------------------)

Share

_Design a Scalable and Production-Ready Idempotent Payment Processing System to Prevent Double Transactions Due to Retries_

Press enter or click to view image in full size

![](https://miro.medium.com/v2/resize:fit:700/1*7H3-tmv4_AY8uETmbRNHZw.png)

### Problem Statement

In a large-scale payment system, network glitches, client retries, server timeouts, or gateway failures can result in **duplicate payment submissions**.

Need to design a **scalable**, **fault-tolerant**, and **production-grade** idempotent payment system that **guarantees exactly-once transaction processing** even under retries.

### Constraints & Requirements

- **High Throughput**: System should support thousands to millions of transactions per second.
- **Low Latency**: Payment operations must complete within 150–300ms for best user experience.
- **Idempotency Guarantees**: Same payment request (even if retried) must not create duplicate charges.
- **Eventual Consistency**: Downstream systems (ledger, wallet, bank) must eventually reflect correct status.
- **Fault Tolerance**: Handle client retries, backend retries, and partial failures gracefully.
- **Storage Cost**: Maintain idempotency records efficiently without blowing up storage.
- **Availability**: System must be highly available (>=99.99%).

### **_Proposed Architecture_**

**Stateless Microservices + Idempotency Layer + Gateway-Aware Coordination**

**a. Microservice-Oriented**: All components (payment processing, notification, ledger, audit, etc.) are independently deployable and loosely coupled.

**b. Stateless Core Services**: The main orchestrator and adapters do not maintain session state — enabling horizontal scaling.

**c. Idempotency Layer**: A dedicated Redis/DynamoDB-backed store acts as a memory for replay protection.

**d. External Gateway Aware**: The architecture is built with awareness of 3rd-party payment gateway idempotency.

**e. Retry-Resilient**: Clients, orchestrators, and gateways are all resilient to retries and partial failures.

**f. Asynchronous Fan-Out**: Notification, ledger, and observability use event-driven or…

## Create an account to read the full story.

The author made this story available to Medium members only.

If you’re new to Medium, create a new account to read this story on us.

[Continue in app](https://play.google.com/store/apps/details?id=com.medium.reader&referrer=utm_source%3Dregwall&source=-----1d7888480648---------------------post_regwall------------------)

Or, continue in mobile web

[Sign up with Google](https://medium.com/m/connect/google?state=google-%7Chttps%3A%2F%2Flevelup.gitconnected.com%2Fsystem-architecture-idempotency-in-payment-transactions-1d7888480648%3Fsource%3D-----1d7888480648---------------------post_regwall------------------%26skipOnboarding%3D1%7Cregister%7Cremember_me&source=-----1d7888480648---------------------post_regwall------------------)

[Sign up with Facebook](https://medium.com/m/connect/facebook?state=facebook-%7Chttps%3A%2F%2Flevelup.gitconnected.com%2Fsystem-architecture-idempotency-in-payment-transactions-1d7888480648%3Fsource%3D-----1d7888480648---------------------post_regwall------------------%26skipOnboarding%3D1%7Cregister%7Cremember_me&source=-----1d7888480648---------------------post_regwall------------------)

Sign up with email

Already have an account? [Sign in](https://medium.com/m/signin?operation=login&redirect=https%3A%2F%2Flevelup.gitconnected.com%2Fsystem-architecture-idempotency-in-payment-transactions-1d7888480648&source=-----1d7888480648---------------------post_regwall------------------)

[![Level Up Coding](https://miro.medium.com/v2/resize:fill:96:96/1*5D9oYBd58pyjMkV_5-zXXQ.jpeg)](https://levelup.gitconnected.com/?source=post_page---post_publication_info--1d7888480648---------------------------------------)

[![Level Up Coding](https://miro.medium.com/v2/resize:fill:128:128/1*5D9oYBd58pyjMkV_5-zXXQ.jpeg)](https://levelup.gitconnected.com/?source=post_page---post_publication_info--1d7888480648---------------------------------------)

Follow

[**Published in Level Up Coding**](https://levelup.gitconnected.com/?source=post_page---post_publication_info--1d7888480648---------------------------------------)

[335K followers](https://levelup.gitconnected.com/followers?source=post_page---post_publication_info--1d7888480648---------------------------------------)

· [Last published 17 hours ago](https://levelup.gitconnected.com/edasm-part-2-solving-ai-context-collapse-and-unsafe-actions-tri-tier-memory-relevance-scoring-d102c4fd2974?source=post_page---post_publication_info--1d7888480648---------------------------------------)

Coding tutorials and news. The developer homepage [gitconnected.com](http://gitconnected.com/) && [skilled.dev](http://skilled.dev/) && [levelup.dev](http://levelup.dev/)

Follow

[![ScalaBrix](https://miro.medium.com/v2/resize:fill:96:96/1*bpk0nttx-BClykzEyWw2MA.png)](https://scalabrix.medium.com/?source=post_page---post_author_info--1d7888480648---------------------------------------)

[![ScalaBrix](https://miro.medium.com/v2/resize:fill:128:128/1*bpk0nttx-BClykzEyWw2MA.png)](https://scalabrix.medium.com/?source=post_page---post_author_info--1d7888480648---------------------------------------)

Follow

[**Written by ScalaBrix**](https://scalabrix.medium.com/?source=post_page---post_author_info--1d7888480648---------------------------------------)

[1.4K followers](https://scalabrix.medium.com/followers?source=post_page---post_author_info--1d7888480648---------------------------------------)

· [110 following](https://medium.com/@scalabrix/following?source=post_page---post_author_info--1d7888480648---------------------------------------)

System Design & Architecture \| 🚀 Next-gen architecture content \| 📈 Built for scale \| Tech Blog

Follow

[Help](https://help.medium.com/hc/en-us?source=post_page-----1d7888480648---------------------------------------)

[Status](https://status.medium.com/?source=post_page-----1d7888480648---------------------------------------)

[About](https://medium.com/about?autoplay=1&source=post_page-----1d7888480648---------------------------------------)

[Careers](https://medium.com/jobs-at-medium/work-at-medium-959d1a85284e?source=post_page-----1d7888480648---------------------------------------)

[Press](mailto:pressinquiries@medium.com)

[Blog](https://blog.medium.com/?source=post_page-----1d7888480648---------------------------------------)

[Store](https://medium.com/store)

[Privacy](https://policy.medium.com/medium-privacy-policy-f03bf92035c9?source=post_page-----1d7888480648---------------------------------------)

[Rules](https://policy.medium.com/medium-rules-30e5502c4eb4?source=post_page-----1d7888480648---------------------------------------)

[Terms](https://policy.medium.com/medium-terms-of-service-9db0094a1e0f?source=post_page-----1d7888480648---------------------------------------)

[Text to speech](https://speechify.com/medium?source=post_page-----1d7888480648---------------------------------------)

reCAPTCHA

Recaptcha requires verification.

protected by **reCAPTCHA**