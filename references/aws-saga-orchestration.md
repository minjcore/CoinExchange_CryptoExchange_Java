\# Saga orchestration pattern

\## Intent

The saga orchestration pattern uses a central coordinator (\*orchestrator\*) to help preserve data integrity in distributed transactions that span multiple services. In a distributed transaction, multiple services can be called before a transaction is completed. When the services store data in different data stores, it can be challenging to maintain data consistency across these data stores.

\## Motivation

A \*transaction\* is a single unit of work that might involve multiple steps, where all steps are completely executed or no step is executed, resulting in a data store that retains its consistent state. The terms \*atomicity, consistency, isolation, and durability (ACID)\* define the properties of a transaction. Relational databases provide ACID transactions to maintain data consistency.

To maintain consistency in a transaction, relational databases use the two-phase commit (2PC) method. This consists of a \*prepare phase\* and a \*commit phase\*.
\+ In the prepare phase, the coordinating process requests the transaction's participating processes (participants) to promise to either commit or roll back the transaction.
\+ In the commit phase, the coordinating process requests the participants to commit the transaction. If the participants cannot agree to commit in the prepare phase, the transaction is rolled back.

In distributed systems that follow a database-per-service design pattern, the two-phase commit is not an option. This is because each transaction is distributed across various databases, and there is no single controller that can coordinate a process that's similar to the two-phase commit in relational data stores. In this case, one solution is to use the saga orchestration pattern.

\## Applicability

Use the saga orchestration pattern when:
\+ Your system requires data integrity and consistency in distributed transactions that span multiple data stores.
\+ The data store doesn't provide 2PC to provide ACID transactions, and implementing 2PC within the application boundaries is a complex task.
\+ You have NoSQL databases, which do not provide ACID transactions, and you need to update multiple tables within a single transaction.

\## Issues and considerations

\+ \*\*Complexity\*\*: Compensatory transactions and retries add complexities to the application code, which can result in maintenance overhead.
\+ \*\*Eventual consistency\*\*: The sequential processing of local transactions results in eventual consistency, which can be a challenge in systems that require strong consistency. You can address this issue by setting your business teams' expectations for the consistency model or by switching to a data store that provides strong consistency.
\+ \*\*Idempotency\*\*: Saga participants need to be idempotent to allow repeated execution in case of transient failures caused by unexpected crashes and orchestrator failures.
\+ \*\*Transaction isolation\*\*: Saga lacks transaction isolation. Concurrent orchestration of transactions can lead to stale data. We recommend using semantic locking to handle such scenarios.
\+ \*\*Observability\*\*: Observability refers to detailed logging and tracing to troubleshoot issues in the execution and orchestration process. This becomes important when the number of saga participants increases, resulting in complexities in debugging.
\+ \*\*Latency issues\*\*: Compensatory transactions can add latency to the overall response time when the saga consists of several steps. Avoid synchronous calls in such cases.
\+ \*\*Single point of failure\*\*: The orchestrator can become a single point of failure because it coordinates the entire transaction. In some cases, the saga choreography pattern is preferred because of this issue.

\## Implementation

\### High-level architecture

In the following architecture diagram, the saga orchestrator has three participants: the order service, the inventory service, and the payment service. Three steps are required to complete the transaction: T1, T2, and T3. The saga orchestrator is aware of the steps and runs them in the required order. When step T3 fails (payment failure), the orchestrator runs the compensatory transactions C1 and C2 to restore the data to the initial state.

!\[Saga orchestrator high-level architecture\](http://docs.aws.amazon.com/prescriptive-guidance/latest/cloud-design-patterns/images/saga-orchestration-1.png)

You can use \[AWS Step Functions\](https://aws.amazon.com/step-functions/) to implement saga orchestration when the transaction is distributed across multiple databases.

\### Implementation using AWS services

The sample solution uses the standard workflow in Step Functions to implement the saga orchestration pattern.

!\[Implementing the saga workflow with Step Functions\](http://docs.aws.amazon.com/prescriptive-guidance/latest/cloud-design-patterns/images/saga-orchestration-2.png)

When a customer calls the API, the Lambda function is invoked, and preprocessing occurs in the Lambda function. The function starts the Step Functions workflow to start processing the distributed transaction. If preprocessing isn't required, you can \[initiate the Step Functions workflow directly\](https://serverlessland.com/patterns/apigw-sfn) from API Gateway without using the Lambda function.

The use of Step Functions mitigates the single point of failure issue, which is inherent in the implementation of the saga orchestration pattern. Step Functions has built-in fault tolerance and maintains service capacity across multiple Availability Zones in each AWS Region to protect applications against individual machine or data center failures. This helps ensure high availability for both the service itself and for the application workflow it operates.

\#### The Step Functions workflow

The Step Functions state machine allows you to configure the decision-based control flow requirements for the pattern implementation. The Step Functions workflow calls the individual services for order placement, inventory update, and payment processing to complete the transaction and sends an event notification for further processing. The Step Functions workflow acts as the orchestrator to coordinate the transactions. If the workflow contains any errors, the orchestrator runs the compensatory transactions to ensure that data integrity is maintained across services.

The following diagram shows the steps that run inside the Step Functions workflow. The \`Place Order\`, \`Update Inventory\`, and \`Make Payment\` steps indicate the success path. The order is placed, the inventory is updated, and the payment is processed before a \`Success\` state is returned to the caller.

The \`Revert Payment\`, \`Revert Inventory\`, and \`Remove Order\` Lambda functions indicate the compensatory transactions that the orchestrator runs when any step in the workflow fails. If the workflow fails at the \`Update Inventory\` step, the orchestrator calls the \`Revert Inventory\` and \`Remove Order\` steps before returning a \`Fail\` state to the caller. These compensatory transactions ensure that data integrity is maintained. The inventory returns to its original level and the order is reverted.

!\[Saga Step Functions workflow\](http://docs.aws.amazon.com/prescriptive-guidance/latest/cloud-design-patterns/images/saga-orchestration-3.png)

\### Sample code

The following sample code shows how you can create a saga orchestrator by using Step Functions. To view the complete code, see the \[GitHub repository\](https://github.com/aws-samples/saga-orchestration-netcore-blog) for this example.

\#### Task definitions

\`\`\`
var successState = new Succeed(this,"SuccessState");
var failState = new Fail(this, "Fail");

var placeOrderTask = new LambdaInvoke(this, "Place Order", new LambdaInvokeProps
{
 LambdaFunction = placeOrderLambda,
 Comment = "Place Order",
 RetryOnServiceExceptions = false,
 PayloadResponseOnly = true
});

var updateInventoryTask = new LambdaInvoke(this,"Update Inventory", new LambdaInvokeProps
{
 LambdaFunction = updateInventoryLambda,
 Comment = "Update inventory",
 RetryOnServiceExceptions = false,
 PayloadResponseOnly = true
});

var makePaymentTask = new LambdaInvoke(this,"Make Payment", new LambdaInvokeProps
{
 LambdaFunction = makePaymentLambda,
 Comment = "Make Payment",
 RetryOnServiceExceptions = false,
 PayloadResponseOnly = true
});

var removeOrderTask = new LambdaInvoke(this, "Remove Order", new LambdaInvokeProps
{
 LambdaFunction = removeOrderLambda,
 Comment = "Remove Order",
 RetryOnServiceExceptions = false,
 PayloadResponseOnly = true
}).Next(failState);

var revertInventoryTask = new LambdaInvoke(this,"Revert Inventory", new LambdaInvokeProps
{
 LambdaFunction = revertInventoryLambda,
 Comment = "Revert inventory",
 RetryOnServiceExceptions = false,
 PayloadResponseOnly = true
}).Next(removeOrderTask);

var revertPaymentTask = new LambdaInvoke(this,"Revert Payment", new LambdaInvokeProps
{
 LambdaFunction = revertPaymentLambda,
 Comment = "Revert Payment",
 RetryOnServiceExceptions = false,
 PayloadResponseOnly = true
}).Next(revertInventoryTask);

var waitState = new Wait(this, "Wait state", new WaitProps
{
 Time = WaitTime.Duration(Duration.Seconds(30))
}).Next(revertInventoryTask);
\`\`\`

\#### Step function and state machine definitions

\`\`\`
var stepDefinition = placeOrderTask
 .Next(new Choice(this, "Is order placed")
 .When(Condition.StringEquals("$.Status", "ORDER\_PLACED"), updateInventoryTask
 .Next(new Choice(this, "Is inventory updated")
 .When(Condition.StringEquals("$.Status", "INVENTORY\_UPDATED"),
 makePaymentTask.Next(new Choice(this, "Is payment success")
 .When(Condition.StringEquals("$.Status", "PAYMENT\_COMPLETED"), successState)
 .When(Condition.StringEquals("$.Status", "ERROR"), revertPaymentTask)))
 .When(Condition.StringEquals("$.Status", "ERROR"), waitState)))
 .When(Condition.StringEquals("$.Status", "ERROR"), failState));

var stateMachine = new StateMachine(this, "DistributedTransactionOrchestrator", new StateMachineProps {
 StateMachineName = "DistributedTransactionOrchestrator",
 StateMachineType = StateMachineType.STANDARD,
 Role = iamStepFunctionRole,
 TracingEnabled = true,
 Definition = stepDefinition
});
\`\`\`

\### GitHub repository

For a complete implementation of the sample architecture for this pattern, see the GitHub repository at \[https://github.com/aws-samples/saga-orchestration-netcore-blog\](https://github.com/aws-samples/saga-orchestration-netcore-blog).

\## Blog references

\+ \[Building a serverless distributed application using Saga Orchestration pattern\](https://aws.amazon.com/blogs/compute/building-a-serverless-distributed-application-using-a-saga-orchestration-pattern/)

\## Related content

\+ \[Saga choreography pattern\](saga-choreography.md)
\+ \[Transactional outbox pattern\](transactional-outbox.md)

\## Videos

The following video discusses how to implement the saga orchestration pattern by using AWS Step Functions.