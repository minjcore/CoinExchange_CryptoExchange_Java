#### [Microservice Architecture](https://microservices.io/index.html)

**Supported by [Kong](https://konghq.com/)**

# Pattern: Transactional outbox [§](https://microservices.io/patterns/data/transactional-outbox.html\#undefined)

[pattern](https://microservices.io/tags/pattern)[transactional messaging](https://microservices.io/tags/transactional%20messaging)[service design](https://microservices.io/tags/service%20design)[inter-service communication](https://microservices.io/tags/inter-service%20communication)

* * *

## Also known as [§](https://microservices.io/patterns/data/transactional-outbox.html\#also-known-as)

Application events

## Context [§](https://microservices.io/patterns/data/transactional-outbox.html\#context)

A service command typically needs to create/update/delete [aggregates](https://microservices.io/patterns/data/aggregate.html) in the database **and** send messages/events to a message broker.
For example, a service that participates in a [saga](https://microservices.io/patterns/data/saga.html) needs to update business entities and send messages/events.
Similarly, a service that publishes a [domain event](https://microservices.io/patterns/data/domain-event.html) must update an [aggregate](https://microservices.io/patterns/data/aggregate.html) and publish an event.

The command must atomically update the database and send messages in order to avoid data inconsistencies and bugs.
However, it is not viable to use a traditional distributed transaction (2PC) that spans the database and the message broker
The database and/or the message broker might not support 2PC.
And even if they do, it’s often undesirable to couple the service to both the database and the message broker.

But without using 2PC, sending a message in the middle of a transaction is not reliable.
There’s no guarantee that the transaction will commit.
Similarly, if a service sends a message after committing the transaction there’s no guarantee that it won’t crash before sending the message.

In addition, messages must be sent to the message broker in the order they were sent by the service.
They must usually be delivered to each consumer in the same order although that’s outside the scope of this pattern.
For example, let’s suppose that an aggregate is updated by a series of transactions `T1`, `T2`, etc.
This transactions might be performed by the same service instance or by different service instances.
Each transaction publishes a corresponding event: `T1 -> E1`, `T2 -> E2`, etc.
Since `T1` precedes `T2`, event `E1` must be published before `E2`.

## Problem [§](https://microservices.io/patterns/data/transactional-outbox.html\#problem)

How to atomically update the database and send messages to a message broker?

## Forces [§](https://microservices.io/patterns/data/transactional-outbox.html\#forces)

- 2PC is not an option. The database and/or the message broker might not support 2PC. Also, it’s often undesirable to couple the service to both the database and the message broker.
- If the database transaction commits then the messages must be sent. Conversely, if the database rolls back, the messages must not be sent
- Messages must be sent to the message broker in the order they were sent by the service. This ordering must be preserved across multiple service instances that update the same aggregate.

## Solution [§](https://microservices.io/patterns/data/transactional-outbox.html\#solution)

The solution is for the service that sends the message to first store the message in the database as part of the transaction that updates the business entities.
A separate process then sends the messages to the message broker.

![](https://microservices.io/i/patterns/data/ReliablePublication.png)

The participants in this pattern are:

- Sender - the service that sends the message
- Database - the database that stores the business entities and message outbox
- Message outbox - if it’s a relational database, this is a table that stores the messages to be sent. Otherwise, if it’s a NoSQL database, the outbox is a property of each database record (e.g. document or item)
- Message relay - sends the messages stored in the outbox to the message broker

## Result context [§](https://microservices.io/patterns/data/transactional-outbox.html\#result-context)

This pattern has the following benefits:

- 2PC is not used
- Messages are guaranteed to be sent if and only if the database transaction commits
- Messages are sent to the message broker in the order they were sent by the application

This pattern has the following drawbacks:

- Potentially error prone since the developer might forget to publish the message/event after updating the database.

This pattern also has the following issues:

- The Message relay might publish a message more than once.
It might, for example, crash after publishing a message but before recording the fact that it has done so.
When it restarts, it will then publish the message again.
As a result, a message consumer must be idempotent, perhaps by tracking the IDs of the messages that it has already processed.
Fortunately, since message Consumers usually need to be idempotent (because a message broker can deliver messages more than once) this is typically not a problem.

## Related patterns [§](https://microservices.io/patterns/data/transactional-outbox.html\#related-patterns)

- The [Saga](https://microservices.io/patterns/data/saga.html) and [Domain event](https://microservices.io/patterns/data/domain-event.html) patterns create the need for this pattern.
- The [Event sourcing](https://microservices.io/patterns/data/event-sourcing.html) is an alternative solution
- There are two patterns for implementing the Message relay:
  - The [Transaction log tailing](https://microservices.io/patterns/data/transaction-log-tailing.html) pattern
  - The [Polling publisher](https://microservices.io/patterns/data/polling-publisher.html) pattern

## Learn more [§](https://microservices.io/patterns/data/transactional-outbox.html\#learn-more)

- My book [Microservices patterns](https://microservices.io/book) describes this pattern in a lot more detail.
- The [Eventuate Tram framework](https://github.com/eventuate-tram/eventuate-tram-core) implements this pattern

* * *

[pattern](https://microservices.io/tags/pattern)[transactional messaging](https://microservices.io/tags/transactional%20messaging)[service design](https://microservices.io/tags/service%20design)[inter-service communication](https://microservices.io/tags/inter-service%20communication)

* * *

* * *

[Modernization Help](https://chrisrichardson.net/)

Copyright © 2026 Chris Richardson • All rights reserved • Supported by [Kong](https://konghq.com/).

Disqus Recommendations

We were unable to load Disqus Recommendations. If you are a moderator please see our [troubleshooting guide](https://docs.disqus.com/help/83/).

❮

- 3 years ago
- 4 comments

Exploring transactional messaging in Oracle 23c Free - developer release …

- a year ago
- 1 comment

The importance of flow state for developers Explore DDD workshop, April 14-15, …

- 3 years ago
- 1 comment

Beer and tacos and the metaphorical origins of Docker containers …

- 3 years ago
- 4 comments

Maintain a queryable replica of the data in the service that implements the …

- a year ago
- 1 comment

Authentication and authorization in a microservice architecture: …

- 3 years ago
- 1 comment

How modular can your monolith go? Part 6 - transaction management …

- 3 years ago
- 2 comments

Assemblage overview: Part 3 - What’s a service architecture? In previous …

- 3 years ago
- 3 comments

DDD, necessary but insufficient: physical design principles for …

❯

Disqus Comments

We were unable to load Disqus. If you are a moderator please see our [troubleshooting guide](https://docs.disqus.com/help/83/).

G

Join the discussion…

﻿

Comment

###### Log in with

###### or sign up with Disqus  or pick a name

### Disqus is a discussion network

- Don't be a jerk or do anything illegal. Everything is easier that way.

[Read full terms and conditions](https://docs.disqus.com/kb/terms-and-policies/)

This comment platform is hosted by Disqus, Inc. I authorize Disqus and its affiliates to:

- Use, sell, and share my information to enable me to use its comment services and for marketing purposes, including cross-context behavioral advertising, as described in our [Terms of Service](https://help.disqus.com/customer/portal/articles/466260-terms-of-service) and [Privacy Policy](https://disqus.com/privacy-policy), including supplementing that information with other data about me, such as my browsing and location data.
- Contact me or enable others to contact me by email with offers for goods or services
- Process any sensitive personal information that I submit in a comment. See our [Privacy Policy](https://disqus.com/privacy-policy) for more information

Acknowledge I am 18 or older

- [6](https://disqus.com/embed/comments/?base=default&f=microservices&t_u=https%3A%2F%2Fmicroservices.io%2Fpatterns%2Fdata%2Ftransactional-outbox.html&t_d=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&t_t=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&s_o=default# "Favorite this discussion")

  - ## Discussion Favorited!



    Favoriting means this is a discussion worth sharing. It gets shared to your followers' Disqus feeds, and gives the creator kudos!


     [Find More Discussions](https://disqus.com/home/?utm_source=disqus_embed&utm_content=recommend_btn)

[Share](https://disqus.com/embed/comments/?base=default&f=microservices&t_u=https%3A%2F%2Fmicroservices.io%2Fpatterns%2Fdata%2Ftransactional-outbox.html&t_d=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&t_t=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&s_o=default#)

  - Tweet this discussion
  - Share this discussion on Facebook
  - Share this discussion via email
  - Copy link to discussion

  - [Best](https://disqus.com/embed/comments/?base=default&f=microservices&t_u=https%3A%2F%2Fmicroservices.io%2Fpatterns%2Fdata%2Ftransactional-outbox.html&t_d=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&t_t=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&s_o=default#)
  - [Newest](https://disqus.com/embed/comments/?base=default&f=microservices&t_u=https%3A%2F%2Fmicroservices.io%2Fpatterns%2Fdata%2Ftransactional-outbox.html&t_d=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&t_t=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&s_o=default#)
  - [Oldest](https://disqus.com/embed/comments/?base=default&f=microservices&t_u=https%3A%2F%2Fmicroservices.io%2Fpatterns%2Fdata%2Ftransactional-outbox.html&t_d=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&t_t=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&s_o=default#)

- - [−](https://disqus.com/embed/comments/?base=default&f=microservices&t_u=https%3A%2F%2Fmicroservices.io%2Fpatterns%2Fdata%2Ftransactional-outbox.html&t_d=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&t_t=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&s_o=default# "Collapse")
  - [+](https://disqus.com/embed/comments/?base=default&f=microservices&t_u=https%3A%2F%2Fmicroservices.io%2Fpatterns%2Fdata%2Ftransactional-outbox.html&t_d=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&t_t=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&s_o=default# "Expand")
  - [Flag as inappropriate](https://disqus.com/embed/comments/?base=default&f=microservices&t_u=https%3A%2F%2Fmicroservices.io%2Fpatterns%2Fdata%2Ftransactional-outbox.html&t_d=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&t_t=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&s_o=default# "Flag as inappropriate")


A

How will this pattern work if you need to scale the database horizontally i.e. multiple instances? Should the message relay poll outbox table across all instances?

see more

[Show more replies](https://disqus.com/embed/comments/?base=default&f=microservices&t_u=https%3A%2F%2Fmicroservices.io%2Fpatterns%2Fdata%2Ftransactional-outbox.html&t_d=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&t_t=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&s_o=default#)

- - [−](https://disqus.com/embed/comments/?base=default&f=microservices&t_u=https%3A%2F%2Fmicroservices.io%2Fpatterns%2Fdata%2Ftransactional-outbox.html&t_d=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&t_t=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&s_o=default# "Collapse")
  - [+](https://disqus.com/embed/comments/?base=default&f=microservices&t_u=https%3A%2F%2Fmicroservices.io%2Fpatterns%2Fdata%2Ftransactional-outbox.html&t_d=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&t_t=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&s_o=default# "Expand")
  - [Flag as inappropriate](https://disqus.com/embed/comments/?base=default&f=microservices&t_u=https%3A%2F%2Fmicroservices.io%2Fpatterns%2Fdata%2Ftransactional-outbox.html&t_d=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&t_t=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&s_o=default# "Flag as inappropriate")


J

I believe each service should have their own outbox table. In this case, the DB transaction you are making and the outbox table update gets committed in the same transaction and the message can be delivered later, like after the transaction. If the massage delivery was not successful, you can retry to send the message because, it's in your database and flagged as unsuccessful

see more

[Show more replies](https://disqus.com/embed/comments/?base=default&f=microservices&t_u=https%3A%2F%2Fmicroservices.io%2Fpatterns%2Fdata%2Ftransactional-outbox.html&t_d=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&t_t=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&s_o=default#)

- - [−](https://disqus.com/embed/comments/?base=default&f=microservices&t_u=https%3A%2F%2Fmicroservices.io%2Fpatterns%2Fdata%2Ftransactional-outbox.html&t_d=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&t_t=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&s_o=default# "Collapse")
  - [+](https://disqus.com/embed/comments/?base=default&f=microservices&t_u=https%3A%2F%2Fmicroservices.io%2Fpatterns%2Fdata%2Ftransactional-outbox.html&t_d=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&t_t=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&s_o=default# "Expand")
  - [Flag as inappropriate](https://disqus.com/embed/comments/?base=default&f=microservices&t_u=https%3A%2F%2Fmicroservices.io%2Fpatterns%2Fdata%2Ftransactional-outbox.html&t_d=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&t_t=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&s_o=default# "Flag as inappropriate")


M

Using this pattern and thinking in microservices, once each service has its own database, should I have an Outbox table in each service database or should I have a common (centralized) database/service?

see more

  - - [−](https://disqus.com/embed/comments/?base=default&f=microservices&t_u=https%3A%2F%2Fmicroservices.io%2Fpatterns%2Fdata%2Ftransactional-outbox.html&t_d=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&t_t=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&s_o=default# "Collapse")
    - [+](https://disqus.com/embed/comments/?base=default&f=microservices&t_u=https%3A%2F%2Fmicroservices.io%2Fpatterns%2Fdata%2Ftransactional-outbox.html&t_d=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&t_t=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&s_o=default# "Expand")
    - [Flag as inappropriate](https://disqus.com/embed/comments/?base=default&f=microservices&t_u=https%3A%2F%2Fmicroservices.io%2Fpatterns%2Fdata%2Ftransactional-outbox.html&t_d=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&t_t=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&s_o=default# "Flag as inappropriate")


J

yes, I believe each service should have their own outbox table. In this case, DB transaction you are making and the outbox table update gets committed in the same transaction and the message can be delivered later, like after the transaction. If the massage delivery was not successful, you can retry to send the message because, it's in your database and flagged as unsuccessful

see more

    - - [−](https://disqus.com/embed/comments/?base=default&f=microservices&t_u=https%3A%2F%2Fmicroservices.io%2Fpatterns%2Fdata%2Ftransactional-outbox.html&t_d=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&t_t=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&s_o=default# "Collapse")
      - [+](https://disqus.com/embed/comments/?base=default&f=microservices&t_u=https%3A%2F%2Fmicroservices.io%2Fpatterns%2Fdata%2Ftransactional-outbox.html&t_d=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&t_t=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&s_o=default# "Expand")
      - [Flag as inappropriate](https://disqus.com/embed/comments/?base=default&f=microservices&t_u=https%3A%2F%2Fmicroservices.io%2Fpatterns%2Fdata%2Ftransactional-outbox.html&t_d=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&t_t=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&s_o=default# "Flag as inappropriate")


![Avatar for rony](https://c.disquscdn.com/uploads/users/19390/1960/avatar92.jpg?1763976672)

I see the concern, before I thought it can be handled by add state for each event for example for Order \[PENDING, PENDING\_CONFIRM, CREATED\]

PENDING\_CONFIRM means when message is successfully delivered to another services before changing to next state. But, this one can't relied on when distributed message to more than one services (as consumer). Tks

see more

[Show more replies](https://disqus.com/embed/comments/?base=default&f=microservices&t_u=https%3A%2F%2Fmicroservices.io%2Fpatterns%2Fdata%2Ftransactional-outbox.html&t_d=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&t_t=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&s_o=default#)

[Show more replies](https://disqus.com/embed/comments/?base=default&f=microservices&t_u=https%3A%2F%2Fmicroservices.io%2Fpatterns%2Fdata%2Ftransactional-outbox.html&t_d=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&t_t=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&s_o=default#)

[Show more replies](https://disqus.com/embed/comments/?base=default&f=microservices&t_u=https%3A%2F%2Fmicroservices.io%2Fpatterns%2Fdata%2Ftransactional-outbox.html&t_d=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&t_t=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&s_o=default#)

- - [−](https://disqus.com/embed/comments/?base=default&f=microservices&t_u=https%3A%2F%2Fmicroservices.io%2Fpatterns%2Fdata%2Ftransactional-outbox.html&t_d=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&t_t=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&s_o=default# "Collapse")
  - [+](https://disqus.com/embed/comments/?base=default&f=microservices&t_u=https%3A%2F%2Fmicroservices.io%2Fpatterns%2Fdata%2Ftransactional-outbox.html&t_d=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&t_t=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&s_o=default# "Expand")
  - [Flag as inappropriate](https://disqus.com/embed/comments/?base=default&f=microservices&t_u=https%3A%2F%2Fmicroservices.io%2Fpatterns%2Fdata%2Ftransactional-outbox.html&t_d=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&t_t=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&s_o=default# "Flag as inappropriate")


![Avatar for Ovidiu Bratosin](https://c.disquscdn.com/uploads/users/19859/9407/avatar92.jpg?1719602749)

I was looking today for an elegant and easy way to solve this issue and I ended up here also. I decided to implement this pattern with Node.js, MariaDB and RabbitMQ and started to write an article on the implementation: [https://wipsquare.dev/2024/...](https://wipsquare.dev/2024/06/28/microservices-transactional-outbox-pattern-part-1/ "https://wipsquare.dev/2024/06/28/microservices-transactional-outbox-pattern-part-1/")

see more

[Show more replies](https://disqus.com/embed/comments/?base=default&f=microservices&t_u=https%3A%2F%2Fmicroservices.io%2Fpatterns%2Fdata%2Ftransactional-outbox.html&t_d=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&t_t=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&s_o=default#)

- - [−](https://disqus.com/embed/comments/?base=default&f=microservices&t_u=https%3A%2F%2Fmicroservices.io%2Fpatterns%2Fdata%2Ftransactional-outbox.html&t_d=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&t_t=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&s_o=default# "Collapse")
  - [+](https://disqus.com/embed/comments/?base=default&f=microservices&t_u=https%3A%2F%2Fmicroservices.io%2Fpatterns%2Fdata%2Ftransactional-outbox.html&t_d=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&t_t=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&s_o=default# "Expand")
  - [Flag as inappropriate](https://disqus.com/embed/comments/?base=default&f=microservices&t_u=https%3A%2F%2Fmicroservices.io%2Fpatterns%2Fdata%2Ftransactional-outbox.html&t_d=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&t_t=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&s_o=default# "Flag as inappropriate")


C

Later in the book is mentioned how a single operation can generate multiple events that are returned as part of the response i'm not sure how is this implemented in the examples i see that the order for the services is usually.

\- make something on the aggregate(this generate events).

\- save the changes.

\- publish the events.

so is not actually making atomically the update and the save in the OUTBOX, how will this look in real tearm do you pass to the repository the aggregate to be save with the events and then the events get save at the same time in the outbox table to be pull later.(or is there some scenarios when save and then in another step send domain events is okay?)

the thing above i believe is because the methods are marked as transactional in java, but this approach i seem to believe is specially for languajes when that is not available

see more

[Show more replies](https://disqus.com/embed/comments/?base=default&f=microservices&t_u=https%3A%2F%2Fmicroservices.io%2Fpatterns%2Fdata%2Ftransactional-outbox.html&t_d=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&t_t=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&s_o=default#)

- - [−](https://disqus.com/embed/comments/?base=default&f=microservices&t_u=https%3A%2F%2Fmicroservices.io%2Fpatterns%2Fdata%2Ftransactional-outbox.html&t_d=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&t_t=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&s_o=default# "Collapse")
  - [+](https://disqus.com/embed/comments/?base=default&f=microservices&t_u=https%3A%2F%2Fmicroservices.io%2Fpatterns%2Fdata%2Ftransactional-outbox.html&t_d=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&t_t=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&s_o=default# "Expand")
  - [Flag as inappropriate](https://disqus.com/embed/comments/?base=default&f=microservices&t_u=https%3A%2F%2Fmicroservices.io%2Fpatterns%2Fdata%2Ftransactional-outbox.html&t_d=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&t_t=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&s_o=default# "Flag as inappropriate")


N

_"An service that uses a NoSQL database appends the messages/events to attribute of the record (e.g. document or item) being updated."_

How is the Message Relay supposed to tail or fetch messages/events from all of the records? In case of crash how would Message Relay restart in case of NoSQL? I tried finding solution in you book but wasn't successful.

see more

[Show more replies](https://disqus.com/embed/comments/?base=default&f=microservices&t_u=https%3A%2F%2Fmicroservices.io%2Fpatterns%2Fdata%2Ftransactional-outbox.html&t_d=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&t_t=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&s_o=default#)

- - [−](https://disqus.com/embed/comments/?base=default&f=microservices&t_u=https%3A%2F%2Fmicroservices.io%2Fpatterns%2Fdata%2Ftransactional-outbox.html&t_d=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&t_t=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&s_o=default# "Collapse")
  - [+](https://disqus.com/embed/comments/?base=default&f=microservices&t_u=https%3A%2F%2Fmicroservices.io%2Fpatterns%2Fdata%2Ftransactional-outbox.html&t_d=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&t_t=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&s_o=default# "Expand")
  - [Flag as inappropriate](https://disqus.com/embed/comments/?base=default&f=microservices&t_u=https%3A%2F%2Fmicroservices.io%2Fpatterns%2Fdata%2Ftransactional-outbox.html&t_d=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&t_t=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&s_o=default# "Flag as inappropriate")


K

After the event is processed by the message relay, how is the outbox table updated atomically?

see more

  - - [−](https://disqus.com/embed/comments/?base=default&f=microservices&t_u=https%3A%2F%2Fmicroservices.io%2Fpatterns%2Fdata%2Ftransactional-outbox.html&t_d=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&t_t=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&s_o=default# "Collapse")
    - [+](https://disqus.com/embed/comments/?base=default&f=microservices&t_u=https%3A%2F%2Fmicroservices.io%2Fpatterns%2Fdata%2Ftransactional-outbox.html&t_d=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&t_t=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&s_o=default# "Expand")
    - [Flag as inappropriate](https://disqus.com/embed/comments/?base=default&f=microservices&t_u=https%3A%2F%2Fmicroservices.io%2Fpatterns%2Fdata%2Ftransactional-outbox.html&t_d=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&t_t=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&s_o=default# "Flag as inappropriate")


A

There is no way to update outbox table atomically (i.e. if and only if message was successfully sent to broker). This is the reason why exactly-once delivery is impossible and message broker should be idempotent

see more

    - - [−](https://disqus.com/embed/comments/?base=default&f=microservices&t_u=https%3A%2F%2Fmicroservices.io%2Fpatterns%2Fdata%2Ftransactional-outbox.html&t_d=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&t_t=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&s_o=default# "Collapse")
      - [+](https://disqus.com/embed/comments/?base=default&f=microservices&t_u=https%3A%2F%2Fmicroservices.io%2Fpatterns%2Fdata%2Ftransactional-outbox.html&t_d=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&t_t=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&s_o=default# "Expand")
      - [Flag as inappropriate](https://disqus.com/embed/comments/?base=default&f=microservices&t_u=https%3A%2F%2Fmicroservices.io%2Fpatterns%2Fdata%2Ftransactional-outbox.html&t_d=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&t_t=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&s_o=default# "Flag as inappropriate")


![Avatar for Chris Richardson](https://c.disquscdn.com/uploads/users/16696/3119/avatar92.jpg?1775169868)

See the resulting context section. The consumer can detect and discard messages that were published more than once.

see more

[Show more replies](https://disqus.com/embed/comments/?base=default&f=microservices&t_u=https%3A%2F%2Fmicroservices.io%2Fpatterns%2Fdata%2Ftransactional-outbox.html&t_d=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&t_t=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&s_o=default#)

    - - [−](https://disqus.com/embed/comments/?base=default&f=microservices&t_u=https%3A%2F%2Fmicroservices.io%2Fpatterns%2Fdata%2Ftransactional-outbox.html&t_d=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&t_t=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&s_o=default# "Collapse")
      - [+](https://disqus.com/embed/comments/?base=default&f=microservices&t_u=https%3A%2F%2Fmicroservices.io%2Fpatterns%2Fdata%2Ftransactional-outbox.html&t_d=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&t_t=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&s_o=default# "Expand")
      - [Flag as inappropriate](https://disqus.com/embed/comments/?base=default&f=microservices&t_u=https%3A%2F%2Fmicroservices.io%2Fpatterns%2Fdata%2Ftransactional-outbox.html&t_d=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&t_t=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&s_o=default# "Flag as inappropriate")


![Avatar for Consent](https://c.disquscdn.com/uploads/users/29007/3940/avatar92.jpg?1610731824)

So that's mean that I have to add to my document which represents some domain object additional - domain or integration events as a queue?

see more

[Show more replies](https://disqus.com/embed/comments/?base=default&f=microservices&t_u=https%3A%2F%2Fmicroservices.io%2Fpatterns%2Fdata%2Ftransactional-outbox.html&t_d=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&t_t=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&s_o=default#)

[Show more replies](https://disqus.com/embed/comments/?base=default&f=microservices&t_u=https%3A%2F%2Fmicroservices.io%2Fpatterns%2Fdata%2Ftransactional-outbox.html&t_d=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&t_t=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&s_o=default#)

[Show more replies](https://disqus.com/embed/comments/?base=default&f=microservices&t_u=https%3A%2F%2Fmicroservices.io%2Fpatterns%2Fdata%2Ftransactional-outbox.html&t_d=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&t_t=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&s_o=default#)

- - [−](https://disqus.com/embed/comments/?base=default&f=microservices&t_u=https%3A%2F%2Fmicroservices.io%2Fpatterns%2Fdata%2Ftransactional-outbox.html&t_d=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&t_t=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&s_o=default# "Collapse")
  - [+](https://disqus.com/embed/comments/?base=default&f=microservices&t_u=https%3A%2F%2Fmicroservices.io%2Fpatterns%2Fdata%2Ftransactional-outbox.html&t_d=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&t_t=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&s_o=default# "Expand")
  - [Flag as inappropriate](https://disqus.com/embed/comments/?base=default&f=microservices&t_u=https%3A%2F%2Fmicroservices.io%2Fpatterns%2Fdata%2Ftransactional-outbox.html&t_d=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&t_t=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&s_o=default# "Flag as inappropriate")


![Avatar for avinash kumar](https://c.disquscdn.com/uploads/users/33973/561/avatar92.jpg?1594191263)

What do you mean by message relay ?

see more

[Show more replies](https://disqus.com/embed/comments/?base=default&f=microservices&t_u=https%3A%2F%2Fmicroservices.io%2Fpatterns%2Fdata%2Ftransactional-outbox.html&t_d=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&t_t=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&s_o=default#)

- - [−](https://disqus.com/embed/comments/?base=default&f=microservices&t_u=https%3A%2F%2Fmicroservices.io%2Fpatterns%2Fdata%2Ftransactional-outbox.html&t_d=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&t_t=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&s_o=default# "Collapse")
  - [+](https://disqus.com/embed/comments/?base=default&f=microservices&t_u=https%3A%2F%2Fmicroservices.io%2Fpatterns%2Fdata%2Ftransactional-outbox.html&t_d=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&t_t=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&s_o=default# "Expand")
  - [Flag as inappropriate](https://disqus.com/embed/comments/?base=default&f=microservices&t_u=https%3A%2F%2Fmicroservices.io%2Fpatterns%2Fdata%2Ftransactional-outbox.html&t_d=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&t_t=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&s_o=default# "Flag as inappropriate")


![Avatar for Zariga](https://c.disquscdn.com/uploads/users/9840/2794/avatar92.jpg?1432289742)

Inspired by this concept drafted

[leanpub.com/microops](http://disq.us/url?url=http%3A%2F%2Fleanpub.com%2Fmicroops%3Akr6J-9qsWJMzvW8BkerbJBzyKHA&cuid=3716990 "leanpub.com/microops")

see more

[Show more replies](https://disqus.com/embed/comments/?base=default&f=microservices&t_u=https%3A%2F%2Fmicroservices.io%2Fpatterns%2Fdata%2Ftransactional-outbox.html&t_d=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&t_t=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&s_o=default#)

- - [−](https://disqus.com/embed/comments/?base=default&f=microservices&t_u=https%3A%2F%2Fmicroservices.io%2Fpatterns%2Fdata%2Ftransactional-outbox.html&t_d=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&t_t=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&s_o=default# "Collapse")
  - [+](https://disqus.com/embed/comments/?base=default&f=microservices&t_u=https%3A%2F%2Fmicroservices.io%2Fpatterns%2Fdata%2Ftransactional-outbox.html&t_d=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&t_t=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&s_o=default# "Expand")
  - [Flag as inappropriate](https://disqus.com/embed/comments/?base=default&f=microservices&t_u=https%3A%2F%2Fmicroservices.io%2Fpatterns%2Fdata%2Ftransactional-outbox.html&t_d=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&t_t=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&s_o=default# "Flag as inappropriate")


![Avatar for Jijie Chen](https://c.disquscdn.com/uploads/users/11596/2103/avatar92.jpg?1543845499)

It appears that NServiceBus also implements this pattern: [https://docs.particular.net/nservicebus/outbox/](https://docs.particular.net/nservicebus/outbox/ "https://docs.particular.net/nservicebus/outbox/")

see more

[Show more replies](https://disqus.com/embed/comments/?base=default&f=microservices&t_u=https%3A%2F%2Fmicroservices.io%2Fpatterns%2Fdata%2Ftransactional-outbox.html&t_d=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&t_t=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&s_o=default#)

[Load more comments](https://disqus.com/embed/comments/?base=default&f=microservices&t_u=https%3A%2F%2Fmicroservices.io%2Fpatterns%2Fdata%2Ftransactional-outbox.html&t_d=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&t_t=Pattern%3A%20Transactional%20outbox%C2%A0%C2%A7&s_o=default#)

![](https://io.narrative.io/?companyId=19&id=disqus_id%3Acp7mtv4tt5hh3&ret=img&ref=https%3A%2F%2Fmicroservices.io%2Fpatterns%2Fdata%2Ftransactional-outbox.html)![](https://io.narrative.io/?companyId=1952&id=disqus_id%3Acp7mtv4tt5hh3&red=https%3A%2F%2Fpx.ads.linkedin.com%2Fdb_sync%3Fpid%3D16269%26puuid%3D%24%7Bnarrative.id.value%7D%26rand%3D0.714751417205)

live.rezync.com

# live.rezync.com is blocked

This page has been blocked by an extension

- Try disabling your extensions.

ERR\_BLOCKED\_BY\_CLIENT

Reload


This page has been blocked by an extension

![](<Base64-Image-Removed>)![](<Base64-Image-Removed>)

pippio.com

# pippio.com is blocked

This page has been blocked by an extension

- Try disabling your extensions.

ERR\_BLOCKED\_BY\_CLIENT

Reload


This page has been blocked by an extension

![](<Base64-Image-Removed>)![](<Base64-Image-Removed>)

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