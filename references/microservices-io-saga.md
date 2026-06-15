#### [Microservice Architecture](https://microservices.io/index.html)

**Supported by [Kong](https://konghq.com/)**

# Pattern: Saga [§](https://microservices.io/patterns/data/saga.html\#undefined)

[pattern](https://microservices.io/tags/pattern)[transaction management](https://microservices.io/tags/transaction%20management)[sagas](https://microservices.io/tags/sagas)[service collaboration](https://microservices.io/tags/service%20collaboration)[implementing commands](https://microservices.io/tags/implementing%20commands)

* * *

## Want to learn more about this pattern? [§](https://microservices.io/patterns/data/saga.html\#undefined)

Take a look at my [self-paced, online bootcamp](https://chrisrichardson.net/virtual-bootcamp-distributed-data-management.html) that teaches you how to use the Saga, API Composition, and CQRS patterns to design operations that span multiple services.


The regular price is $395/person but use coupon OFFEFKCW to sign up for $95 (valid until Sept 30th, 2025)


[![](https://microservices.io/i/Microservices_IO_Logo_White.png)](https://chrisrichardson.net/virtual-bootcamp-distributed-data-management.html)

## Context [§](https://microservices.io/patterns/data/saga.html\#context)

You have applied the [Database per Service](https://microservices.io/patterns/data/database-per-service.html) pattern.
Each service has its own database.
Some business transactions, however, span multiple service so you need a mechanism to implement transactions that span services.
For example, let’s imagine that you are building an e-commerce store where customers have a credit limit.
The application must ensure that a new order will not exceed the customer’s credit limit.
Since Orders and Customers are in different databases owned by different services the application cannot simply use a local ACID transaction.

## Problem [§](https://microservices.io/patterns/data/saga.html\#problem)

How to implement transactions that span services?

## Forces [§](https://microservices.io/patterns/data/saga.html\#forces)

- 2PC is not an option

## Solution [§](https://microservices.io/patterns/data/saga.html\#solution)

Implement each business transaction that spans multiple services as a saga.
A saga is a sequence of local transactions.
Each local transaction updates the database and publishes a message or event to trigger the next local transaction in the saga.
If a local transaction fails because it violates a business rule then the saga executes a series of compensating transactions that undo the changes that were made by the preceding local transactions.

![](https://microservices.io/i/sagas/From_2PC_To_Saga.png)

There are two ways of coordination sagas:

- Choreography - each local transaction publishes domain events that trigger local transactions in other services
- Orchestration - an orchestrator (object) tells the participants what local transactions to execute

## Example: Choreography-based saga [§](https://microservices.io/patterns/data/saga.html\#example-choreography-based-saga)

![](https://microservices.io/i/sagas/Create_Order_Saga.png)

An e-commerce application that uses this approach would create an order using a choreography-based saga that consists of the following steps:

1. The `Order Service` receives the `POST /orders` request and creates an `Order` in a `PENDING` state
2. It then emits an `Order Created` event
3. The `Customer Service`’s event handler attempts to reserve credit
4. It then emits an event indicating the outcome
5. The `OrderService`’s event handler either approves or rejects the `Order`

[Take a tour of an example saga](https://microservices.io/post/architecture/2024/03/20/tour-of-two-sagas.html)

## Example: Orchestration-based saga [§](https://microservices.io/patterns/data/saga.html\#example-orchestration-based-saga)

![](https://microservices.io/i/sagas/Create_Order_Saga_Orchestration.png)

An e-commerce application that uses this approach would create an order using an orchestration-based saga that consists of the following steps:

1. The `Order Service` receives the `POST /orders` request and creates the `Create Order` saga orchestrator
2. The saga orchestrator creates an `Order` in the `PENDING` state
3. It then sends a `Reserve Credit` command to the `Customer Service`
4. The `Customer Service` attempts to reserve credit
5. It then sends back a reply message indicating the outcome
6. The saga orchestrator either approves or rejects the `Order`

[Take a tour of an example saga](https://microservices.io/post/architecture/2024/03/20/tour-of-two-sagas.html)

## Resulting context [§](https://microservices.io/patterns/data/saga.html\#resulting-context)

This pattern has the following benefits:

- It enables an application to maintain data consistency across multiple services without using distributed transactions

This solution has the following drawbacks:

- Lack of automatic rollback - a developer must design compensating transactions that explicitly undo changes made earlier in a saga rather than relying on the automatic rollback feature of ACID transactions

- Lack of isolation (the “I” in ACID) - the lack of isolation means that there’s risk that the concurrent execution of multiple sagas and transactions can use data anomalies. consequently, a saga developer must typical use countermeasures, which are design techniques that implement isolation. Moreover, careful analysis is needed to select and correctly implement the countermeasures. See [Chapter 4/section 4.3 of my book Microservices Patterns](https://livebook.manning.com/book/microservices-patterns/chapter-4/143) for more information.


There are also the following issues to address:

- In order to be reliable, a service must atomically update its database _and_ publish a message/event.
It cannot use the traditional mechanism of a distributed transaction that spans the database and the message broker.
Instead, it must use one of the patterns listed below.

- A client that initiates the saga, which an asynchronous flow, using a synchronous request (e.g. HTTP `POST /orders`) needs to be able to determine its outcome.
There are several options, each with different trade-offs:
  - The service sends back a response once the saga completes, e.g. once it receives an `OrderApproved` or `OrderRejected` event.
  - The service sends back a response (e.g. containing the `orderID`) after initiating the saga and the client periodically polls (e.g. `GET /orders/{orderID}`) to determine the outcome
  - The service sends back a response (e.g. containing the `orderID`) after initiating the saga, and then sends an event (e.g. websocket, web hook, etc) to the client once the saga completes.

## Related patterns [§](https://microservices.io/patterns/data/saga.html\#related-patterns)

- The [Database per Service pattern](https://microservices.io/patterns/data/database-per-service.html) creates the need for this pattern
- The following patterns are ways to _atomically_ update state _and_ publish messages/events:

  - [Event sourcing](https://microservices.io/patterns/data/event-sourcing.html)
  - [Transactional Outbox](https://microservices.io/patterns/data/transactional-outbox.html)
- A choreography-based saga can publish events using [Aggregates](https://microservices.io/patterns/data/aggregate.html) and [Domain Events](https://microservices.io/patterns/data/domain-event.html)
- The [Command-side replica](https://microservices.io/patterns/data/command-side-replica.html) is an alternative pattern, which can replace saga step that query data

## Learn more [§](https://microservices.io/patterns/data/saga.html\#learn-more)

- My book [Microservices patterns](https://microservices.io/book) describes this pattern in a lot more detail. The book’s [example application](https://github.com/microservice-patterns/ftgo-application) implements orchestration-based sagas using the [Eventuate Tram Sagas framework](https://github.com/eventuate-tram/eventuate-tram-sagas)
- Take a look at my [self-paced, online bootcamp](https://chrisrichardson.net/virtual-bootcamp-distributed-data-management.html) that teaches you how to use the Saga, API Composition, and CQRS patterns to design operations that span multiple services.
- Read these [articles](https://microservices.io/tags/sagas.html) about the Saga pattern
- My [presentations](https://microservices.io/presentations) on sagas and asynchronous microservices.

## Example code [§](https://microservices.io/patterns/data/saga.html\#example-code)

The following examples implement the customers and orders example in different ways:

- [Choreography-based saga](https://github.com/eventuate-tram/eventuate-tram-examples-customers-and-orders) where the services publish domain events using the [Eventuate Tram framework](https://github.com/eventuate-tram/eventuate-tram-core)
- [Orchestration-based saga](https://github.com/eventuate-tram/eventuate-tram-sagas-examples-customers-and-orders) where the `Order Service` uses a saga orchestrator implemented using the [Eventuate Tram Sagas framework](https://github.com/eventuate-tram/eventuate-tram-sagas)
- [Choreography and event sourcing-based saga](https://github.com/eventuate-examples/eventuate-examples-java-customers-and-orders) where the services publish domain events using the [Eventuate event sourcing framework](http://eventuate.io/)

* * *

[pattern](https://microservices.io/tags/pattern)[transaction management](https://microservices.io/tags/transaction%20management)[sagas](https://microservices.io/tags/sagas)[service collaboration](https://microservices.io/tags/service%20collaboration)[implementing commands](https://microservices.io/tags/implementing%20commands)

* * *

* * *

[Modernization Help](https://chrisrichardson.net/)

Copyright © 2026 Chris Richardson • All rights reserved • Supported by [Kong](https://konghq.com/).

Disqus Recommendations

We were unable to load Disqus Recommendations. If you are a moderator please see our [troubleshooting guide](https://docs.disqus.com/help/83/).

Disqus Comments

We were unable to load Disqus. If you are a moderator please see our [troubleshooting guide](https://docs.disqus.com/help/83/).

#### About Microservices.io

![](https://gravatar.com/avatar/a290a8643359e2495e1c6312e662012f)

Microservices.io is created by [Chris Richardson](https://microservices.io/about.html), software architect, creator of the original CloudFoundry.com, and author of _Microservices Patterns_. Chris advises organizations on modernization, architecture, and building systems that avoid becoming modern legacy systems.


#### Need help modernizing your architecture?

Avoid the trap of creating a modern legacy system — a new architecture with the same old problems.

Contact me to discuss your modernization goals.


[Get Help](https://www.linkedin.com/in/pojos/)

#### Microservices Patterns, 2nd edition

![](https://microservices.io/i/posts/mp2e-book-cover.png)

I am very excited to announce that the MEAP for the second edition of my book, Microservices Patterns is now available!


[Learn more](https://microservices.io/post/architecture/2025/06/26/announcing-meap-microservices-patterns-2nd-edition.html)

#### ASK CHRIS

?

Got a question about microservices?

Fill in [this form](https://forms.gle/ppYDAF1JxHGec8Kn9). If I can, I'll write a blog post that answers your question.

#### NEED HELP?

![](https://microservices.io/i/posts/cxo-wondering.webp)

I help organizations improve agility and competitiveness through better software architecture.


Learn more about my [consulting engagements](https://chrisrichardson.net/consulting.html), and [training workshops](https://chrisrichardson.net/training.html).


#### PREMIUM CONTENT

![](https://microservices.io/i/posts/premium-logo.png)
Premium content now available for paid subscribers at [premium.microservices.io](https://premium.microservices.io/).



#### MICROSERVICES WORKSHOPS

![](https://microservices.io/i/workshop-kata_small.jpg)

Chris teaches [comprehensive workshops](http://chrisrichardson.net/training.html) for architects and developers that will enable your organization use microservices effectively.


Avoid the pitfalls of adopting microservices and learn essential topics, such as service decomposition and design and how to refactor a monolith to microservices.


[Learn more](http://chrisrichardson.net/training.html)

#### Remote consulting session

![](https://microservices.io/i/posts/zoom-consulting.webp)

Got a specific microservice architecture-related question? For example:

- Wondering whether your organization should adopt microservices?
- Want to know how to migrate your monolith to microservices?
- Facing a tricky microservice architecture design problem?

Consider signing up for a [two hour, highly focussed, consulting session.](https://chrisrichardson.net/consulting-office-hours.html)

#### ASSESS your architecture

Assess your application's microservice architecture and identify what needs to be improved. [Engage Chris](http://www.chrisrichardson.net/consulting.html) to conduct an architect review.

#### LEARN about microservices

Chris offers numerous other resources for learning the microservice architecture.

#### Get the book: Microservices Patterns

Read Chris Richardson's book:

[![](https://microservices.io/i/Microservices-Patterns-Cover-published.png)](https://microservices.io/book)

* * *

#### Example microservices applications

Want to see an example? Check out Chris Richardson's example applications.
[See code](http://eventuate.io/exampleapps.html)

#### Virtual bootcamp: Distributed data patterns in a microservice architecture

![](https://microservices.io/i/Chris_Speaking_Mucon_2018_a.jpg)

My virtual bootcamp, distributed data patterns in a microservice architecture, is now open for enrollment!

It covers the key distributed data management patterns including Saga, API Composition, and CQRS.

It consists of video lectures, code labs, and a weekly ask-me-anything video conference repeated in multiple timezones.

The regular price is $395/person but use coupon OFFEFKCW to sign up for $95 (valid until Sept 30th, 2025).
There are deeper discounts for buying multiple seats.


[Learn more](https://chrisrichardson.net/virtual-bootcamp-distributed-data-management.html)

#### Learn how to create a service template and microservice chassis

Take a look at my [Manning LiveProject](https://microservices.io/post/patterns/2022/03/15/service-template-chassis-live-project.html) that teaches you how to develop a service template and microservice chassis.


![](https://microservices.io/i/patterns/microservice-template-and-chassis/Microservice_chassis.png)

[Signup for the newsletter](http://visitor.r20.constantcontact.com/d.jsp?llr=ula8akwab&p=oi&m=1123470377332&sit=l6ktajjkb&f=15d9bba9-b33d-491f-b874-73a41bba8a76)

For Email Marketing you can trust.

#### BUILD microservices

Ready to start using the microservice architecture?



#### Consulting services

[Engage Chris](http://www.chrisrichardson.net/consulting.html) to create a microservices adoption roadmap and help you define your microservice architecture,

* * *

#### The Eventuate platform

Use the [Eventuate.io platform](https://eventuate.io/) to tackle distributed data management challenges in your microservices architecture.

[![](https://eventuate.io/i/logo.gif)](https://eventuate.io/)

Eventuate is Chris's latest startup. It makes it easy to use the Saga pattern to manage transactions and the CQRS pattern to implement queries.

* * *

Join the [microservices google group](https://groups.google.com/forum/#!forum/microservices)