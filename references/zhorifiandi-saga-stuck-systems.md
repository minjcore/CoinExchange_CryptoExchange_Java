Share on:

![Code Review Illustration](https://zhorifiandi.github.io/docs/2024-09-14-solve-stuck-systems-flow-using-saga-pattern/illustration.webp)

Have you ever encountered case where you need to do manual reconciliation when your systems break? This often happens in transactional flow, in which every request matters.

- [Sample Case: E-commerce Order Flow](https://zhorifiandi.github.io/software-engineering/2024/09/14/solve-stuck-systems-flow-using-saga-pattern.html#sample-case-e-commerce-order-flow)
  - [What Could Go Wrong?](https://zhorifiandi.github.io/software-engineering/2024/09/14/solve-stuck-systems-flow-using-saga-pattern.html#what-could-go-wrong)
  - [Customer Complaints: Now What?](https://zhorifiandi.github.io/software-engineering/2024/09/14/solve-stuck-systems-flow-using-saga-pattern.html#customer-complaints-now-what)
  - [The Short-Term Fix: Error Handling Logic in the API](https://zhorifiandi.github.io/software-engineering/2024/09/14/solve-stuck-systems-flow-using-saga-pattern.html#the-short-term-fix-error-handling-logic-in-the-api)
    - [Poor UX for your customers](https://zhorifiandi.github.io/software-engineering/2024/09/14/solve-stuck-systems-flow-using-saga-pattern.html#poor-ux-for-your-customers)
    - [Not Robust Enough…](https://zhorifiandi.github.io/software-engineering/2024/09/14/solve-stuck-systems-flow-using-saga-pattern.html#not-robust-enough)
- [A Robust and Elegant Solution?](https://zhorifiandi.github.io/software-engineering/2024/09/14/solve-stuck-systems-flow-using-saga-pattern.html#a-robust-and-elegant-solution)
  - [Introducing Saga Pattern](https://zhorifiandi.github.io/software-engineering/2024/09/14/solve-stuck-systems-flow-using-saga-pattern.html#introducing-saga-pattern)
    - [“I’m using Monolithic, not Micro-services…”](https://zhorifiandi.github.io/software-engineering/2024/09/14/solve-stuck-systems-flow-using-saga-pattern.html#im-using-monolithic-not-micro-services)
    - [Asynchronous Processing](https://zhorifiandi.github.io/software-engineering/2024/09/14/solve-stuck-systems-flow-using-saga-pattern.html#asynchronous-processing)
  - [How Could we Apply Saga Pattern?](https://zhorifiandi.github.io/software-engineering/2024/09/14/solve-stuck-systems-flow-using-saga-pattern.html#how-could-we-apply-saga-pattern)
    - [Breaking Down Whole Transactions into Sagas](https://zhorifiandi.github.io/software-engineering/2024/09/14/solve-stuck-systems-flow-using-saga-pattern.html#breaking-down-whole-transactions-into-sagas)
    - [Coordinating the Saga](https://zhorifiandi.github.io/software-engineering/2024/09/14/solve-stuck-systems-flow-using-saga-pattern.html#coordinating-the-saga)
    - [How Could Saga Pattern Handle Failures?](https://zhorifiandi.github.io/software-engineering/2024/09/14/solve-stuck-systems-flow-using-saga-pattern.html#how-could-saga-pattern-handle-failures)
  - [Common Pitfalls ❌](https://zhorifiandi.github.io/software-engineering/2024/09/14/solve-stuck-systems-flow-using-saga-pattern.html#common-pitfalls-)
    - [When to determine whether we retry or trigger compensating Transaction?](https://zhorifiandi.github.io/software-engineering/2024/09/14/solve-stuck-systems-flow-using-saga-pattern.html#when-to-determine-whether-we-retry-or-trigger-compensating-transaction)
    - [What if Message gets processed Twice? (e.g. Double Payment Charged)](https://zhorifiandi.github.io/software-engineering/2024/09/14/solve-stuck-systems-flow-using-saga-pattern.html#what-if-message-gets-processed-twice-eg-double-payment-charged)
    - [What if a Compensating Transaction Fails?](https://zhorifiandi.github.io/software-engineering/2024/09/14/solve-stuck-systems-flow-using-saga-pattern.html#what-if-a-compensating-transaction-fails)
  - [Solution on E-commerce Order Flow](https://zhorifiandi.github.io/software-engineering/2024/09/14/solve-stuck-systems-flow-using-saga-pattern.html#solution-on-e-commerce-order-flow)
    - [State Machine Diagram](https://zhorifiandi.github.io/software-engineering/2024/09/14/solve-stuck-systems-flow-using-saga-pattern.html#state-machine-diagram)
    - [Architecture Diagram](https://zhorifiandi.github.io/software-engineering/2024/09/14/solve-stuck-systems-flow-using-saga-pattern.html#architecture-diagram)
    - [Happy Path Flow Diagram](https://zhorifiandi.github.io/software-engineering/2024/09/14/solve-stuck-systems-flow-using-saga-pattern.html#happy-path-flow-diagram)
    - [Failed Delivery Compensating Saga Flow Diagram](https://zhorifiandi.github.io/software-engineering/2024/09/14/solve-stuck-systems-flow-using-saga-pattern.html#failed-delivery-compensating-saga-flow-diagram)
- [Conclusion](https://zhorifiandi.github.io/software-engineering/2024/09/14/solve-stuck-systems-flow-using-saga-pattern.html#conclusion)
  - [What’s next? 🤔](https://zhorifiandi.github.io/software-engineering/2024/09/14/solve-stuck-systems-flow-using-saga-pattern.html#whats-next-)

![banner.png](https://zhorifiandi.github.io/docs/2024-09-14-solve-stuck-systems-flow-using-saga-pattern/banner.png)

Image of "stuck man" was generated by Stable Diffusion

* * *

# Sample Case: E-commerce Order Flow

Imagine you have an API to create new orders, which have very minimum functionalities:

- Reduce Product Stocks
- Charge Payment to Customers using 3rd party providers like Xendit, Stripe, etc.
- Arrange Delivery of the Product to Customers.

![image.png](https://zhorifiandi.github.io/docs/2024-09-14-solve-stuck-systems-flow-using-saga-pattern/image.png)

* * *

## What Could Go Wrong?

![image.png](https://zhorifiandi.github.io/docs/2024-09-14-solve-stuck-systems-flow-using-saga-pattern/image%201.png)

Imagine you received errors when creating delivery request after you’ve:

- Reduce Product Stocks Count
- Charge Customers with Payment

This might happen if somehow Delivery Partner API get some downtime. We can try to retry those requests. Let say retrying the requests to delivery service won’t help (they get very long / hours downtime….), You either need to rollback those actions in order to avoid “stuck” states, or manually retry and update the order later.

* * *

## Customer Complaints: Now What?

Customer got errors, and they want to reorder the product. However, We have charged customer’s money previously and the Product stocks have gone empty since we have reduced the stocks previously ( _while in fact, it’s not out of stocks yet!_). These are typical steps for developers to solve it in short term:

- Increase the product stocks in Database
- Refund the Payment in 3rd Party providers

Or yet another _hacky_ solution:

- Recreate new delivery to Delivery API (externals)
- Update Order status to Completed with Delivery details

If these issues only occur once in a while, it’s fine. Can you imagine you receiving >5x complaints per day? That’d be the most frustrating day to do tedious things over and over again!!!

* * *

## The Short-Term Fix: Error Handling Logic in the API

![image.png](https://zhorifiandi.github.io/docs/2024-09-14-solve-stuck-systems-flow-using-saga-pattern/image%202.png)

So, in case of errors when creating new delivery, your service will do several rollback strategies:

- Restore (Increasing back) the product stocks in Database
- Create Refund Request to 3rd Party providers
- Then, let the client app (frontend) to retry the order requests to your service.

While these measures may help temporarily, there are some drawbacks:

### Poor UX for your customers

- Additional Latencies before returning an error to the customer
- Consecutive Payment and Refund for unacceptable reasons, while some payment fees might occurs to Customers end.

### Not Robust Enough…

Now we already handled error on `Creating New Delivery`, what happen if get more errors? For example, you get error when you try to `Refund the payment steps`. In this case, you can’t apply the same approach as you handled error on `Create New Delivery steps` , you’ll still need manual intervention to retry the Refund outside of this API call.

* * *

# A Robust and Elegant Solution?

Key Points:

- Saga Pattern
- Asynchronous Processing

## Introducing Saga Pattern

Quoting from [https://microservices.io/patterns/data/saga.html](https://microservices.io/patterns/data/saga.html)

> Implement each business transaction that spans multiple services as a saga. A saga is a sequence of **_local transactions._**
>
> - Each local transaction **_updates the database_** and **_publishes a message or event_** to trigger the next local transaction in the saga.
> - If a local transaction f **_ails_** because it violates a business rule then the saga executes a series of **_compensating transactions_** that undo the changes that were made by the preceding local transactions.

I’d like to simplify Saga Pattern like this: `ACID, but on microservices / distributed systems` — except instead of one large transaction that either fully succeeds or fails, the transaction breaks the process into smaller transactions, each with its own success and rollback logic (compensating transaction).

In the context of an e-commerce order flow, each action (like reducing stock, charging a customer, and arranging delivery) is a saga. If one of these steps/sagas fails, the system can “undo” previous sagas to maintain data consistency. For example, if charging the customer succeeds but arranging the delivery fails, the system can trigger compensating transaction: a refund.

### “I’m using Monolithic, not Micro-services…”

Even if you’re using [Monolithic](https://microservices.io/patterns/monolithic.html), micro-service techniques will become handy . If you’re using Monolithic, but you rely on 3rd party API: you can treat external/3rd parties APIs as `other independent microservices`. By adding The Saga Pattern to your arsenal, it will help you manage these external dependencies and ensures that your system stays consistent without manual intervention.

### Asynchronous Processing

The Saga pattern leverage asynchronous processing — each step in the transaction doesn’t need to wait for the next to complete.

This means Users can complete their transactions without waiting for background tasks like delivery confirmation, making the system feel more responsive. If something fails at any point, the system can handle the rollback without interrupting the overall flow. At the same time, it ensures your system can handle failures gracefully and maintain consistency, even across multiple services or APIs.

* * *

## How Could we Apply Saga Pattern?

### Breaking Down Whole Transactions into Sagas

To apply the Saga Pattern, think of each step in your process (like reducing stock, charging the customer, and arranging delivery) as a separate transaction. Each transaction must have a compensating action to undo its effect if something goes wrong later.

**Example: E-commerce Order Flow with Saga**

1. **Reduce Product Stock**
   - _**Compensating Action**: Increase the stock if the order fails later_
2. **Charge Customer**
   - _**Compensating Action**: Refund the customer if delivery can’t be arranged._
3. **Arrange Delivery**
   - No compensating action needed here if it’s the final step.

Each step in this flow happens asynchronously, allowing the system to recover from errors without manual intervention. If something fails at any step, the saga will automatically trigger the compensating actions for the previous steps, ensuring that the system stays consistent.

* * *

### Coordinating the Saga

There are two common ways to coordinate sagas:

- **_Orchestration_**: A central controller (orchestrator) coordinates the saga, telling each service when to perform its action and when to trigger compensating actions.~~
- **_Choreography_**: Each service listens for events and decides on its own whether to take an action or trigger a compensating action based on the event’s outcome.~~

Both approaches have their pros and cons. **Orchestration** offers more control and is easier to debug, but can become complex. **Choreography** is more decentralized and works well when each service is independent, but it can be harder to track failures.

We’ll use the most simple use case, which will use monolithic architecture. Thus, the most relevant coordinating strategy would be orchestration.

* * *

### How Could Saga Pattern Handle Failures?

The key strength of the Saga Pattern is its ability to gracefully handle failures in distributed systems by breaking processes into smaller, manageable transactions. Each step in the saga either completes successfully, or it triggers a compensating transaction to undo any previous actions. This ensures that your system remains consistent, even when things go wrong.

Let’s revisit our e-commerce example:

1. **Reduce Product Stock**
   - **Success**: Move to the next step.
   - **Failure**: No action needed, as the system hasn’t made any changes yet.
2. **Charge Customer**
   - **Success**: Proceed to arrange delivery.
   - **Failure**: Trigger compensating transaction to increase stock back to the original amount.
3. **Arrange Delivery**
   - **Success**: The saga is complete.
   - **Failure**: Trigger compensating transactions for both previous steps:

     - Refund the customer.
     - Restore product stock.

* * *

## Common Pitfalls ❌

### When to determine whether we retry or trigger compensating Transaction?

You need to categorize type of errors in each saga/transactions, whether it’s retriable or not. My rule of thumbs to categorize it:

- Not retriable: If the errors caused by the business logic itself. In our E-commerce example, these could be:
  - Insufficient balance on Customer’s Bank Account
  - Bank Rejected the payment request
  - Delivery can’t be arranged due to logistic issues on partner side.
- Retriable: If the errors caused by network errors, partner downtime, and other unknown failures.

If the error is not retriable, thus we need to trigger the compensating sagas/transactions. Else, we will retry the transaction based on a predefined strategy (e.g., exponential backoff).

* * *

### What if Message gets processed Twice? (e.g. Double Payment Charged)

There are several factor that can lead to these conditions:

- Message gets published twice
- Consumer crashes in the middle of the process (e.g. after it calls external dependencies), causing consumers to retry the message consumptions.

The main principle to safeguard this is `idempotency`. This is very broad topic and will need dedicated page to cover. In nutshell, we need to ensure: Retrying any message consumption will always result the same behavior.

**Example Processing Payment Creation in Consumer**

- Payment API (External) usually require its consumer to provide `idempotency-key` for each payment request. This is to ensure that payment will only be created once in expected states.
- Common technique to generate `idempotency-key` from consumer side is to use unique value such as `order_id` .

* * *

### What if a Compensating Transaction Fails?

One of the trickiest problems in distributed systems is when the compensating transaction (e.g., refunding the customer) also fails. The Saga Pattern accounts for this by incorporating **retries** and allowing for **manual intervention** if needed. This way, you can ensure that failures don’t result in inconsistent states or data loss.

For example, if a refund fails after an order delivery error, the saga will retry the refund based on a predefined strategy (e.g., exponential backoff). If the retries still fail, the system can log the issue and flag it for manual resolution. This ensures that your system can recover even in edge cases. Edge cases will always exist, but we try to minimize them as much as possible.

* * *

## Solution on E-commerce Order Flow

### State Machine Diagram

It’s recommended to draw a state diagram, so you can understand the whole transaction from high level perspective. This will be beneficial as well for communicating with non-developers stakeholders.

```
received order from user

product stock reduced

payment requested to Payment Partner

succeeded to charge payment by Payment Partner

delivery requested to Delivery Partner

failed to arrange delivery

delivery succeeded to Delivery Partner

all sagas succeeded, marking order as succeeded

failed to arrange delivery

failed to charge payment to customer\n(e.g. insufficient balance, rejected by bank, etc.)

All compensating transaction has been executed

All compensating transaction has been executed

Order Accepted

Product Stock Reduced

Payment Requested

Payment Charged Sucessfully

Delivery Requested

Delivery Failed

Delivery Succeeded

Order Succeeded

Order Failed

Failed Payment Compensating Sagas

[compensating txn]\nProduct Stock Restored

Failed Delivery Compensating Sagas

successfully refund the money to customer

[compensating txn]\nProduct Stock Restored

[compensating txn]\nRefund Requested

Refund Succeeded
```

* * *

### Architecture Diagram

![image.png](https://zhorifiandi.github.io/docs/2024-09-14-solve-stuck-systems-flow-using-saga-pattern/image%203.png)

The main difference between the solution and initial architectures are:

- Decoupling Create Order API to several components based on defined sagas
  - Happy Path Flow
    - Creating Payment Worker
    - Creating Delivery Worker
  - Failed Delivery Compensating Sagas
    - Creating Refund Worker
    - Restore Product Stock Worker
  - Failed Payment Compensating Sagas
    - Restore Product Stock Worker
- Leveraging Message Broker/Queues as the communication hub between sagas component

While we’ve split the API into multiple components, this doesn’t mean you need separate servers. Depending on your needs (capacity planning), these components can run as simple as background processes within the same server.

* * *

### Happy Path Flow Diagram

![Happy Path Flow Diagram](https://zhorifiandi.github.io/docs/2024-09-14-solve-stuck-systems-flow-using-saga-pattern/image%204.png)

In this flow, each action succeeds and moves to the next sagas:

1. **Order Accepted**
1. **Create Order API** → Order Service creates the order and reduces product stock.
2. **Creating Payment Worker** → payment is requested from the payment partner.
2. **Payment Charged**
1. Payment Webhooks API → Order Service update the order status to PAYMENT\_CHARGED
2. Creating Delivery Worker → Order Service arranges product delivery.
3. **Delivery Succeeded**
1. Delivery Webhooks API → the order is marked as successful.

In this path, no compensating transactions are triggered, and the entire process completes smoothly.

* * *

### Failed Delivery Compensating Saga Flow Diagram

Here’s the scenario when the delivery proccess is failed.
![failed delivery flow](https://zhorifiandi.github.io/docs/2024-09-14-solve-stuck-systems-flow-using-saga-pattern/image%205.png)

1. **Order Accepted**
1. **Create Order API** → Order Service creates the order and reduces product stock.
2. **Creating Payment Worker** → payment is requested from the payment partner.
2. **Payment Charged**
1. Payment Webhooks API → Order Service update the order status to PAYMENT\_CHARGED
2. Creating Delivery Worker → Order Service arranges product delivery.
3. **Delivery Failed** → Trigger Compensating Sagas

![compensanting saga flow](https://zhorifiandi.github.io/docs/2024-09-14-solve-stuck-systems-flow-using-saga-pattern/image%206.png)

1. Trigger Refund
1. Create Refund Worker → the system triggers a refund request to the payment provider.
2. **Refund Succeeded** → the system restores product stock and marks the order as failed.
2. Trigger Product Stock Restored → the system restores product stock and marks the order as failed.

This ensures no “stuck” states exist, and the customer gets their money back.

* * *

# Conclusion

Even if you’re using monolithic architecture, you can leverage Saga Pattern for managing complex and multi-step processes in distributed systems. Implementing this pattern will reduce manual intervention, improves system reliability, and ensures smooth developer experiences even when things go wrong.

## What’s next? 🤔

- Code Implementation on E-commerce Order Flow using Saga Pattern
- Ensuring Idempotency in Message Consumption
- Monitoring and Observability in Sagas

* * *

[<\\
\\
Previous Post\\
\\
Optimize Latency using Parallelization in Go](https://zhorifiandi.github.io/software-engineering/2022/11/25/optimize-latency-using-parallelization-in-go.html "Optimize Latency using Parallelization in Go") [>\\
\\
Next Post\\
\\
Simplify Your Code Review Process with Automation Tools!](https://zhorifiandi.github.io/software-engineering/2024/12/16/simplify-your-code-review-process-with-automation.html "Simplify Your Code Review Process with Automation Tools!")