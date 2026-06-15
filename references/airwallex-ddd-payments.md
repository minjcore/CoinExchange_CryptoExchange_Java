[Sitemap](https://medium.com/sitemap/sitemap.xml)

[Open in app](https://play.google.com/store/apps/details?id=com.medium.reader&referrer=utm_source%3DmobileNavBar&source=post_page---top_nav_layout_nav-----------------------------------------)

Sign up

[Sign in](https://medium.com/m/signin?operation=login&redirect=https%3A%2F%2Fmedium.com%2Fairwallex-engineering%2Fdomain-driven-design-practice-modeling-payments-system-f7bc5cf64bb3&source=post_page---top_nav_layout_nav-----------------------global_nav------------------)

[Medium Logo](https://medium.com/?source=post_page---top_nav_layout_nav-----------------------------------------)

Get app

[Write](https://medium.com/m/signin?operation=register&redirect=https%3A%2F%2Fmedium.com%2Fnew-story&source=---top_nav_layout_nav-----------------------new_post_topnav------------------)

[Search](https://medium.com/search?source=post_page---top_nav_layout_nav-----------------------------------------)

Sign up

[Sign in](https://medium.com/m/signin?operation=login&redirect=https%3A%2F%2Fmedium.com%2Fairwallex-engineering%2Fdomain-driven-design-practice-modeling-payments-system-f7bc5cf64bb3&source=post_page---top_nav_layout_nav-----------------------global_nav------------------)

![Unknown user](https://miro.medium.com/v2/resize:fill:32:32/1*dmbNkD5D-u45r44go_cf0g.png)

[**Airwallex Engineering**](https://medium.com/airwallex-engineering?source=post_page---publication_nav-ee6b7141ce3a-f7bc5cf64bb3---------------------------------------)

·

Follow publication

[![Airwallex Engineering](https://miro.medium.com/v2/resize:fill:38:38/1*VLV39j6OSNJl44290XF_Pw.png)](https://medium.com/airwallex-engineering?source=post_page---post_publication_sidebar-ee6b7141ce3a-f7bc5cf64bb3---------------------------------------)

Insights, ideas & learnings from the Airwallex engineering team. We’re a determined team of engineers passionate about technology & innovation. We build global financial infrastructure to scale the digital economy. Join us at: [https://www.airwallex.com/careers](https://www.airwallex.com/careers)

Follow publication

# Domain-driven design practice — Modelling the payments system

[![Chaojie Xiao](https://miro.medium.com/v2/resize:fill:32:32/1*7YwB3mYR4ttQa49Usvfnpw.jpeg)](https://medium.com/@chaojie.xiao?source=post_page---byline--f7bc5cf64bb3---------------------------------------)

[Chaojie Xiao](https://medium.com/@chaojie.xiao?source=post_page---byline--f7bc5cf64bb3---------------------------------------)

Follow

7 min read

·

Feb 22, 2022

747

7

[Listen](https://medium.com/m/signin?actionUrl=https%3A%2F%2Fmedium.com%2Fplans%3Fdimension%3Dpost_audio_button%26postId%3Df7bc5cf64bb3&operation=register&redirect=https%3A%2F%2Fmedium.com%2Fairwallex-engineering%2Fdomain-driven-design-practice-modeling-payments-system-f7bc5cf64bb3&source=---header_actions--f7bc5cf64bb3---------------------post_audio_button------------------)

Share

Press enter or click to view image in full size

![](https://miro.medium.com/v2/resize:fit:700/1*qDPcyMrFveOMSB4xB7UdQg.jpeg)

Photo [Unsplash](https://unsplash.com/?utm_source=medium&utm_medium=referral)

At Airwallex, [Domain-driven design](https://en.wikipedia.org/wiki/Domain-driven_design) (DDD) methodology is adopted to guide our engineers on how complex business problems and system designs are modelled.

In this blog, we provide a comprehensive workflow looking at the practice of modelling the payments system with DDD patterns.

## Introduction

The global payments system is complex and ever-changing, with a variety of payment methods to clearing and settlement.

When dealing with a complex system, there are some consistent problems most developers encounter:

- Responsibilities can overlap: Using a range of models and strategies on one application means boundaries and responsibilities are not clear.
- No isolation and modularity: Complex business workflows and processes are mixed together and hard to scale.
- No separation of concerns: Core business logic and strategy can overlap with technical implementation challenges, which makes problems more complex.

In this blog, we provide a comprehensive workflow of how we are applying Domain-driven design at Airwallex, the lessons learned, and what we will be working on next.

## What is DDD?

Domain-driven design, made famous by [**Eric Evans**](https://en.wikipedia.org/wiki/Domain-driven_design), is a set of ideas, principles, and patterns that help design software systems based on the underlying model of the business domain. DDD has two distinct spaces, **problem space,** and **solution space**.

In the problem space, you are defining the large-scale structure of the system with strategic patterns, which focus on the analysis of a domain, subdomains, and ubiquitous language.

While in the **solution space**, tactical patterns are adopted to provide a set of design patterns that you can use to create the domain model. These patterns include bounded context, context mapping, entities, aggregates, domain events, domain services, application services, and infrastructure. These tactical patterns will help you to design microservices that are both loosely coupled and cohesive.

Press enter or click to view image in full size

![](https://miro.medium.com/v2/resize:fit:700/0*Z9dO42xMgnHla-eb)

DDD Layers

## How to apply DDD in practice

Here’s a practical example: A customer wants to buy a t-shirt on the merchant’s website, with the price being $10. The customer can pay for the t-shirt with a variety of payment methods, like a Visa card or WeChat wallet.

After the customer pays, the merchant gets a notification from the payment gateway which displays the payment has been ‘successful’ to the customer.

The merchant can then view the payment details in the Airwallex webapp, including the purchase price, merchant fees, and when the funds will be settled into the Airwallex Global Account wallet.

### Workflow

Applying the DDD pattern to the above scenario would involve the following workflow:

1. Analyze the real-world business use case to work out the domain and subdomains in the problem space. Usually, Event Storming is an effective tool to support this
2. Define the bounded context in solution space
3. Within a bounded context, apply a tactical DDD pattern to define entities, aggregates, domain services, domain events, etc.
4. Use the results from step 3 to identify the microservices in your team.

Here is the analysis result.

### Problem space

**Domain:** Payments system

**Subdomain**

- **Payments processing**: Merchants can accept payments from customers with a variety of payment methods
- **Finance**: Clearing and settlement of the payment funds to the merchants.

**Ubiquitous language**

After a discussion with the domain experts, the following ubiquitous language is accepted across all the teams:

- **Payment Intent**: Orders created by the merchant to specify the price, product, customer, etc.
- **Payment Attempt**: Transactions created by the merchant to accept money from customers for a particular order
- **Payment Method**: The way that customers pay for a product or service
- **Payment Settlement**: A set of payments settled to a merchant wallet in one batch
- **Payment View**: An aggregated payment detail view, containing all the data related to one payment.

### Solution space

**Bounded Context**

## Get Chaojie Xiao’s stories in your inbox

Join Medium for free to get updates from this writer.

Subscribe

Subscribe

Remember me for faster sign in

A bounded context (BC) delimits the scope of a domain model. From the analysis result of problem space, we can define the following bounded contexts:

- **Payment Gateway**: API Gateway to provide restful API to the merchants to create or view the payments
- **Payment core:** Payment intent, attempt, method resource management
- **Payment Adaptor**: Integration with one external PSP, for example, WeChat, Alipay, Visa, Mastercard, etc.
- **Payment Settlement**: Calculate and settle the principle and fee of each payment for merchants
- **Payment Fusion**: Aggregated view of payment details.

Below is an example of the resulting **Context Map:**

Press enter or click to view image in full size

![](https://miro.medium.com/v2/resize:fit:700/0*--3JJoElBzf8tbCX)

**Domain model**

From the scenario and ubiquitous language we analyzedabove, we can identify the following aggregate, entities, value object, and domain events.

Press enter or click to view image in full size

![](https://miro.medium.com/v2/resize:fit:700/1*W8Lk7f6vC9_DncAHhicQkg.png)

domain model

**Domain service**

In our experience, domain service uses business logic service for a single aggregate, following the single responsibility pattern. Normally, we will encapsulate the domain repository, aggregate change, and domain events published in the domain service. With **PaymentAttemptExecutorService** as an example:

Press enter or click to view image in full size

![](https://miro.medium.com/v2/resize:fit:700/1*I6CPNnhHRm18pG2ujypxyQ.png)

**PaymentAttempt domain service**

**Domain events**

Domain events can make the system more scalable and avoid any coupling — one aggregate should not determine what the other aggregates should do, and temporal coupling — the successful completion of payment doesn’t depend on all the processes to be available at the same time.

Press enter or click to view image in full size

![](https://miro.medium.com/v2/resize:fit:700/0*IDxRaiZ0uGh3yrAa)

domain events

For example, when **PaymentCaptureCommand** changes the payment status to paid, the domain event **PaymentAttemptCapturedEvent** is sent to notify that the aggregate PaymentAttempt has been captured. In the domain event handler for the **PaymentAttemptCapturedEvent**, we can put any ‘side effects’ on business logic. For example, notifying the payment fusion bounded context to update the payment details and, the payment settlement bounded context to calculate settlement amount and fee.

Press enter or click to view image in full size

![](https://miro.medium.com/v2/resize:fit:700/1*Oqd11QN_vkjvz9lK-epyCA.png)

domain event handler

**Infrastructure**

In the DDD pattern, the infrastructure layer is used to separate the core business domain from technical implementation details. Usually, the **Anticorruption-layer**(ACL) pattern is adopted in this layer. Using domain repository as an example:

Press enter or click to view image in full size

![](https://miro.medium.com/v2/resize:fit:700/1*YgE7K3H1Us3zrF3-39tyqA.png)

domain repository

Domain repository only defines the interface capability, but the implementation detail should be hidden inside the infrastructure layer, like using PostgreSQL or MongoDB to save the data. For example, in the infrastructure layer, the **PaymentAttemptPgRepository** is the specific implementation based on PostgreSQL, and the **toPO** is a mapper used to transform the domain object PaymentAttempt, to the persistence object.

Press enter or click to view image in full size

![](https://miro.medium.com/v2/resize:fit:700/1*FsmxscmYTY_hK3IGCITewA.png)

domain repository implementation in infra layer

Therefore, in the domain layer, we just focus on the domain model, which is completely decoupled from infrastructure technologies. When there is any change in the infrastructure layer, there is no need to change in the domain layer.

**From domain model to microservices**

Now, we have defined a set of bounded contexts for the payments system and identified a set of entities, aggregates, and domain events services inside each bounded context. The next step is to go from domain model to application microservices design. Here we choose to map one bounded context to one microservice.

Press enter or click to view image in full size

![](https://miro.medium.com/v2/resize:fit:700/0*0tIG3rRn0TNSInfH)

microservices

## Benefits

The adoption of DDD can provide many benefits, like clear communication across all the teams and a mature pattern to manage the complexity and provide better scalability when designing the system, for example:

- With the **ubiquitous languages**, we can achieve more self-descriptive class names and function names.
- with the **aggregate** pattern, we can achieve clear boundaries and single responsibility.
- with the **domain events** pattern, we can split the core business processes with side effects on the aggregates.
- With the **infrastructure** layer and **ACL** pattern, we can separate the core business domain model from technical implementation details.
- With the **bounded context** pattern, we can derive the potential microservice candidates.

## Challenges and lessons

There are some challenges and lessons we want to share when applying DDD in practice:

- DDD is a collection of complex patterns that take a long time for the entire team to learn and understand, but the benefits to your system design make it worthwhile.
- DDD does not provide a framework for how to apply it in practice. The workflow we provide in this blog is based on our long-term practices.
- It’s important not to be limited by standard answers. Sometimes it could cost you a lot of time to discuss one model name or boundary context definition.
- DDD is only suitable for complex system design and therefore, cannot be applied to every project.

## Conclusion

In this blog, we touched upon various DDD concepts and strategies and provided a comprehensive workflow of applying DDD patterns in payments system design at Airwallex.

DDD pattern is a vast topic (and is impossible to cover in full detail), but we want to introduce some of the critical topics and our experience in practicing the pattern.

In the future, we will continue to deep dive into each topic in the DDD pattern, like layer management, domain event store, context map pattern, etc., and how to apply them in the system design.

Chaojie Xiao is a Staff Software Engineer at Airwallex.

[Modeling](https://medium.com/tag/modeling?source=post_page-----f7bc5cf64bb3---------------------------------------)

[Payments](https://medium.com/tag/payments?source=post_page-----f7bc5cf64bb3---------------------------------------)

[Architecture](https://medium.com/tag/architecture?source=post_page-----f7bc5cf64bb3---------------------------------------)

[Ddd](https://medium.com/tag/ddd?source=post_page-----f7bc5cf64bb3---------------------------------------)

[Domain Driven Design](https://medium.com/tag/domain-driven-design?source=post_page-----f7bc5cf64bb3---------------------------------------)

[![Airwallex Engineering](https://miro.medium.com/v2/resize:fill:48:48/1*VLV39j6OSNJl44290XF_Pw.png)](https://medium.com/airwallex-engineering?source=post_page---post_publication_info--f7bc5cf64bb3---------------------------------------)

[![Airwallex Engineering](https://miro.medium.com/v2/resize:fill:64:64/1*VLV39j6OSNJl44290XF_Pw.png)](https://medium.com/airwallex-engineering?source=post_page---post_publication_info--f7bc5cf64bb3---------------------------------------)

Follow

[**Published in Airwallex Engineering**](https://medium.com/airwallex-engineering?source=post_page---post_publication_info--f7bc5cf64bb3---------------------------------------)

[225 followers](https://medium.com/airwallex-engineering/followers?source=post_page---post_publication_info--f7bc5cf64bb3---------------------------------------)

· [Last published Feb 13, 2026](https://medium.com/airwallex-engineering/airdev-how-we-taught-ai-agents-to-ship-production-code-ee705ef16d37?source=post_page---post_publication_info--f7bc5cf64bb3---------------------------------------)

Insights, ideas & learnings from the Airwallex engineering team. We’re a determined team of engineers passionate about technology & innovation. We build global financial infrastructure to scale the digital economy. Join us at: [https://www.airwallex.com/careers](https://www.airwallex.com/careers)

Follow

[![Chaojie Xiao](https://miro.medium.com/v2/resize:fill:48:48/1*7YwB3mYR4ttQa49Usvfnpw.jpeg)](https://medium.com/@chaojie.xiao?source=post_page---post_author_info--f7bc5cf64bb3---------------------------------------)

[![Chaojie Xiao](https://miro.medium.com/v2/resize:fill:64:64/1*7YwB3mYR4ttQa49Usvfnpw.jpeg)](https://medium.com/@chaojie.xiao?source=post_page---post_author_info--f7bc5cf64bb3---------------------------------------)

Follow

[**Written by Chaojie Xiao**](https://medium.com/@chaojie.xiao?source=post_page---post_author_info--f7bc5cf64bb3---------------------------------------)

[223 followers](https://medium.com/@chaojie.xiao/followers?source=post_page---post_author_info--f7bc5cf64bb3---------------------------------------)

· [1 following](https://medium.com/@chaojie.xiao/following?source=post_page---post_author_info--f7bc5cf64bb3---------------------------------------)

Staff Engineer at Airwallex

Follow

[Help](https://help.medium.com/hc/en-us?source=post_page-----f7bc5cf64bb3---------------------------------------)

[Status](https://status.medium.com/?source=post_page-----f7bc5cf64bb3---------------------------------------)

[About](https://medium.com/about?autoplay=1&source=post_page-----f7bc5cf64bb3---------------------------------------)

[Careers](https://medium.com/jobs-at-medium/work-at-medium-959d1a85284e?source=post_page-----f7bc5cf64bb3---------------------------------------)

[Press](mailto:pressinquiries@medium.com)

[Blog](https://blog.medium.com/?source=post_page-----f7bc5cf64bb3---------------------------------------)

[Privacy](https://policy.medium.com/medium-privacy-policy-f03bf92035c9?source=post_page-----f7bc5cf64bb3---------------------------------------)

[Rules](https://policy.medium.com/medium-rules-30e5502c4eb4?source=post_page-----f7bc5cf64bb3---------------------------------------)

[Terms](https://policy.medium.com/medium-terms-of-service-9db0094a1e0f?source=post_page-----f7bc5cf64bb3---------------------------------------)

[Text to speech](https://speechify.com/medium?source=post_page-----f7bc5cf64bb3---------------------------------------)

reCAPTCHA

Recaptcha requires verification.

protected by **reCAPTCHA**