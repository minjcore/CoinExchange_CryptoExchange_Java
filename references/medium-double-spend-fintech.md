[Sitemap](https://medium.com/sitemap/sitemap.xml)

[Open in app](https://play.google.com/store/apps/details?id=com.medium.reader&referrer=utm_source%3DmobileNavBar&source=post_page---top_nav_layout_nav-----------------------------------------)

Sign up

[Sign in](https://medium.com/m/signin?operation=login&redirect=https%3A%2F%2Fmedium.com%2Fcodetodeploy%2Fsolving-the-double-spend-system-design-patterns-for-bulletproof-fintech-ee5d73f33415&source=post_page---top_nav_layout_nav-----------------------global_nav------------------)

[Medium Logo](https://medium.com/?source=post_page---top_nav_layout_nav-----------------------------------------)

Get app

[Write](https://medium.com/m/signin?operation=register&redirect=https%3A%2F%2Fmedium.com%2Fnew-story&source=---top_nav_layout_nav-----------------------new_post_topnav------------------)

[Search](https://medium.com/search?source=post_page---top_nav_layout_nav-----------------------------------------)

Sign up

[Sign in](https://medium.com/m/signin?operation=login&redirect=https%3A%2F%2Fmedium.com%2Fcodetodeploy%2Fsolving-the-double-spend-system-design-patterns-for-bulletproof-fintech-ee5d73f33415&source=post_page---top_nav_layout_nav-----------------------global_nav------------------)

![Unknown user](https://miro.medium.com/v2/resize:fill:32:32/1*dmbNkD5D-u45r44go_cf0g.png)

[**CodeToDeploy**](https://medium.com/codetodeploy?source=post_page---publication_nav-c8b549b355f4-ee5d73f33415---------------------------------------)

·

[![CodeToDeploy](https://miro.medium.com/v2/resize:fill:38:38/1*s4SuUoJSUCQqhZfIZuM85A.png)](https://medium.com/codetodeploy?source=post_page---post_publication_sidebar-c8b549b355f4-ee5d73f33415---------------------------------------)

Tech Insights, Career Growth & High-Paying Opportunities

# Solving the Double Spend: System Design Patterns for Bulletproof Fintech

[![Roman Fedytskyi](https://miro.medium.com/v2/resize:fill:32:32/1*xUFTpu45tCJsJ_s-VVYGlA.png)](https://medium.com/@roman_fedyskyi?source=post_page---byline--ee5d73f33415---------------------------------------)

[Roman Fedytskyi](https://medium.com/@roman_fedyskyi?source=post_page---byline--ee5d73f33415---------------------------------------)

15 min read

·

Mar 7, 2026

--

2

[Listen](https://medium.com/m/signin?actionUrl=https%3A%2F%2Fmedium.com%2Fplans%3Fdimension%3Dpost_audio_button%26postId%3Dee5d73f33415&operation=register&redirect=https%3A%2F%2Fmedium.com%2Fcodetodeploy%2Fsolving-the-double-spend-system-design-patterns-for-bulletproof-fintech-ee5d73f33415&source=---header_actions--ee5d73f33415---------------------post_audio_button------------------)

Share

_A deep technical guide on preventing double-spending in fintech. Learn how Stripe and Adyen use idempotency, optimistic locking, and immutable double-entry ledgers to ensure 99.999% consistency._

The digital representation of value presents a unique engineering challenge that distinguishes the financial technology (fintech) sector from almost every other domain of software development. While social media platforms might tolerate an occasional lost “like” or a stale notification, and a ticketing system might resolve a double-booked seat through a refund and an apology, a financial system faces an existential threat when digital scarcity is compromised. In fintech, the failure to prevent a double-spend represents a fundamental violation of banking records, the creation of “phantom money,” and the potential loss of unrecoverable capital. The core challenge is not merely one of concurrency; it is maintaining data integrity at a massive scale across inherently unreliable networks.

This article provides a comprehensive technical investigation into the system design patterns that underpin modern fintech infrastructure. It examines how industry leaders such as **Stripe**, **Adyen**, and **Block** use a layered hierarchy of safety patterns, ranging from API-level idempotency to database-level optimistic locking and immutable double-entry bookkeeping, to process trillions of dollars in transaction volume with near-perfect reliability.

## The Fundamental Problem of Digital Scarcity and the Race Condition

The double-spend problem is the risk that a single unit of digital currency could be spent twice. Unlike physical cash, which is governed by the laws of physics and can only exist in one location at a time, digital information is trivially replicable. In centralized financial systems, this issue is traditionally handled by a central authority, a bank or payment processor , that maintains a master ledger to validate and update transactions in real-time. However, as transaction volumes scale and systems become increasingly distributed, the latency and contention inherent in central validation create significant architectural bottlenecks.

## The Race to Zero and Distributed Concurrency

The most common manifestation of the double-spend in modern API design is the “Check-then-Act” race condition. This occurs when a system reads a balance, validates that sufficient funds exist, and then attempts to update the balance based on that stale read. In a high-concurrency environment, multiple request threads may read the same balance simultaneously before any single update is committed.

Press enter or click to view image in full size

![](https://miro.medium.com/v2/resize:fit:700/1*AM-UaVqQ70V3AUltmAsH7w.png)

Press enter or click to view image in full size

![](https://miro.medium.com/v2/resize:fit:700/1*gA2nCV0aQ1x6gLWLoVVBkA.png)

This “Race to Zero” is exacerbated in distributed systems where multiple API nodes process requests against a shared database cluster. Without specific coordination patterns, the database becomes a site of intense contention. Furthermore, the “Two Generals Problem” in distributed systems theory proves that it is impossible for two parties to reach an absolute consensus over an unreliable communication channel. In fintech, this means that a client may send a request, the server may process it, but the response may never reach the client due to a network timeout. The client, unaware of the server’s success, will likely retry the request, leading to a potential double-charge if idempotency is not enforced.

## Theoretical Constraints: CAP Theorem and the Two Generals Problem

To build “bank-grade” systems, engineers must first acknowledge that absolute reliability is a theoretical impossibility in a distributed environment. The CAP theorem states that a distributed system can only guarantee two of the following three properties at any given time: Consistency, Availability, and Partition Tolerance.

In fintech, the architectural choice is almost always skewed toward Consistency and Partition Tolerance (CP). While availability is a high priority, a ledger that is “maybe correct” is far more damaging than a system that is temporarily unavailable. If a ledger becomes inconsistent even for a fraction of a second, it risks duplicated balances, incorrect financial reporting, and unrecoverable double-spending. Conversely, trading systems or high-frequency retail environments might occasionally favor Availability over strong Consistency (AP) for user-facing actions, provided that the underlying system of record eventually settles to a consistent state.

The “Two Generals Problem” specifically addresses the unreliability of communication. If a soldier (a network packet) is sent to confirm a state change but disappears in transit, the sender cannot distinguish between a server failure, a network partition, or a lost response. Consequently, fintech systems must be designed for “convergence of state” through repeated retries, which necessitates the first major pattern: Idempotency.

## Pattern 0: Pessimistic Locking (The Brute Force Shield)

Pessimistic locking assumes that conflicts will happen and prevents them by locking the data record as soon as it is accessed. In standard SQL databases, this is achieved using the `SELECT... FOR UPDATE` construct within a database transaction.

### Technical Mechanism and Implementation

When a thread executes a `SELECT... FOR UPDATE`, the database engine places an exclusive lock on the specific row(s). If another request arrives and attempts to read or update that row, it must wait in a queue until the first transaction either commits or rolls back.

```
BEGIN;
-- Step 1: Lock the row immediately upon reading
SELECT balance FROM accounts WHERE user_id = 'user_123' FOR UPDATE;
-- Step 2: Application logic validates the balance
-- If balance < required_amount, ROLLBACK;
-- Step 3: Atomic update
UPDATE accounts SET balance = balance - 100 WHERE user_id = 'user_123';
COMMIT; -- Step 4: Lock is released for the next requester
```

### The Throughput Bottleneck

While pessimistic locking provides the strongest possible consistency within a single database, it is highly detrimental to system throughput. In a high-scale environment, particularly for “hot” accounts such as a major merchant receiving hundreds of small payments per second , the database spends more time managing lock wait-queues and handling potential deadlocks than processing actual financial data.

If the lock duration is prolonged by a slow application server or a latent network call within the transaction block, the entire system can experience “contention collapse.” This has led high-scale fintechs like Stripe and Adyen to move toward “lock-free” or “distributed guard” systems that use Pattern 1 through Pattern 3.

## Pattern 1: API-Level Idempotency Keys (The Gateway Shield)

Idempotency is the property of an operation whereby it can be applied multiple times without changing the result beyond the initial application. In fintech APIs, this pattern is the primary defense against the “Retry Problem” caused by flaky mobile networks, server timeouts, and load balancer failures.

### Implementation and Lifecycle

A client generates a unique identifier, known as an Idempotency-Key (typically a V4 UUID), and includes it in the header of every mutating API request (POST, PATCH, DELETE). The server uses this key to track the request's lifecycle.

Press enter or click to view image in full size

![](https://miro.medium.com/v2/resize:fit:700/1*Hyc2kqJTH0fvWybbIyVA-g.png)

Press enter or click to view image in full size

![](https://miro.medium.com/v2/resize:fit:700/1*tkogJrHG16cbzu05fwgnVA.png)

Stripe’s idempotency implementation is widely cited as the industry standard. They persist idempotency records for 24 hours. After this period, if the same key is reused, the system treats it as a new request. This 24-hour window provides a practical trade-off: it is long enough to handle retries during extended outages but short enough to prevent infinite storage growth.

### The Idempotency Layer Architecture

To implement a robust idempotency layer, systems typically use an atomic “Check-and-Set” (CAS) operation against a high-performance key-value store like Redis.

- **Atomicity:** The system must verify the key’s existence and set the key to “LOCKED” in a single atomic operation. In Redis, this is achieved using the `SET key value NX EX 30` command, which sets the key only if it does not already exist, with a 30-second expiration to prevent deadlocks in case of a crash.
- **Payload Verification:** To prevent accidental misuse, the server compares the retry parameters against those of the original request. If a client attempts to use the same idempotency key for a $10.00 charge and a $100.00 charge, the system must reject it as an `IdempotencyError`.
- **Persistence:** Once the request completes, the full response body and status code are saved. Subsequent retries return this cached data, ensuring the client remains in sync with the server regardless of how many network failures occur.

### Global Idempotency and Cross-Region State

For global payment providers like Adyen, idempotency becomes a cross-region coordination problem. If a retry is routed to a different geographic data center due to a regional failover, a local idempotency cache would fail to prevent a double-charge. Adyen manages this through a globally visible metadata layer. While cross-region replication introduces some latency, the priority is placed on financial correctness. This ensures that a retry landing in Singapore for a transaction initiated in London still maps to the same logical transaction.

## Pattern 2: Optimistic Locking and Versioning (The Database Guard)

Optimistic locking is a strategy for managing concurrency without using heavy database row-locks. It assumes that most transactions will not conflict and only checks for a collision at the final moment of the update.

## The Versioning Mechanism

In this pattern, every balance-bearing row includes a version column. When the application reads the balance, it also reads the current version number. When it attempts to write the new balance, it includes the version number in the SQL statement's `WHERE`clause.

Press enter or click to view image in full size

![](https://miro.medium.com/v2/resize:fit:700/1*QpqHDDvIGheT-T8iBRFnbA.png)

```
-- Step 1: Application reads initial state
-- Result: balance = 150.00, version = 5
-- Step 2: Application performs logic and attempts update
UPDATE accounts SET
    balance = balance - 100.00,
    version = version + 1
WHERE
    user_id = 'user_123'
    AND version = 5        -- The "Guard": Fails if version has changed
    AND balance >= 100.00; -- Final safety check for sufficient funds
```

If two concurrent threads attempt this update, the first to commit will increment the version to 6. The second thread’s update will affect zero rows because `version = 5` no longer exists. The application can then catch this "Zero Rows Affected" result and implement a retry strategy.

## Retries and Exponential Backoff

When an optimistic lock fails, the system does not simply give up. Instead, it “replays” the entire request: it reads the new balance and version, re-validates the business logic, and attempts the update again. To prevent “retry storms” or the “thundering herd” problem, where many clients retry at the exact same moment and continue to collide with fintech systems, implement exponential backoff with jitter.

The backoff time is calculated using the formula:

`WaitTime = (2^n) + random_jitter`

where `n` is the number of failed attempts. This ensures that retries are spread out over time, allowing the system to stabilize under heavy load.

## Pattern 3: Immutable Double-Entry Bookkeeping (The Source of Truth)

At the highest level of fintech engineering, the practice of “overwriting” a balance in a single database cell is discarded entirely. Overwriting is considered dangerous because it destroys the audit trail; if a balance is calculated incorrectly, there is no way to reconstruct the sequence of events that led to the error. Instead, professional fintechs utilize double-entry bookkeeping on append-only, immutable ledgers.

## The Accounting Core and DEALER Acronym

Double-entry bookkeeping is a centuries-old accounting method that ensures every transaction is balanced. For every debit (an entry that increases a destination account), there must be a corresponding credit (an entry that decreases a source account). The total value of all debits must always equal the total value of all credits.

This system is governed by the fundamental accounting equation:

`Assets = Liabilities + Equity`

For software engineers, the “DEALER” acronym is used to remember how different account types are affected by debits and credits :

Press enter or click to view image in full size

![](https://miro.medium.com/v2/resize:fit:700/1*1Gxw5sgX4-IcG-jD-cw1pw.png)

## Ledger Architecture and Data Modeling

A “bank-grade” ledger system (such as Stripe Ledger or TigerBeetle) standardizes the representation of money movement. It serves as a semantic data store where money moves between “accounts” (states).

## Get Roman Fedytskyi’s stories in your inbox

Join Medium for free to get updates from this writer.

Subscribe

Subscribe

Remember me for faster sign in

The standard data model for a double-entry system includes three core entities:

- **Accounts:** These are the “buckets” of value. Examples include a “Cash” asset account (money the bank holds) and a “Customer Wallet” liability account (money the bank owes to a user).
- **Transactions:** These represent the high-level intent or business event (e.g., “Charge #555” or “Refund #999”).
- **Journal Entries (Postings):** These are the atomic lines of the ledger. A single transaction must contain at least two journal entries that sum to zero.

Press enter or click to view image in full size

![](https://miro.medium.com/v2/resize:fit:700/1*SPHx-etU_FZqkDS4PNPzRQ.png)

Press enter or click to view image in full size

![](https://miro.medium.com/v2/resize:fit:700/1*VVPdq2huejWTbGsJybdZhg.png)

This append-only nature ensures that the “Why” and “When” of every cent are preserved. If a system crash occurs, the current state can be perfectly reconstructed by replaying the immutable log.

## Modern Specialized Architectures: The TigerBeetle Innovation

Traditional general-purpose databases (PostgreSQL, MySQL) are designed for read-heavy or balanced workloads involving variable-length strings and complex relational joins. However, they often become a bottleneck in high-volume transaction processing due to row contention and the overhead of general-purpose SQL processing.

TigerBeetle is a specialized “Online Transaction Processing” (OLTP) database built for safety and extreme performance. It redefines ledger architecture by focusing on first-principles optimization.

## Performance and Reliability Primitives

TigerBeetle achieves state-of-the-art performance (100K-500K Transactions Per Second) through several key design choices:

- **Fixed-Size Integers:** TigerBeetle avoids the overhead of variable-length strings. It uses 128-bit identifiers and fixed-size data structures for accounts and transfers, allowing for massive batching and efficient CPU cache utilization.
- **Zero-Lock Contention:** By using a single-core design for the storage engine and a consensus protocol for replication, TigerBeetle ensures strict serializability without the need for traditional row-level locks. This eliminates the “contention collapse” seen in general-purpose databases when many transactions hit a single hot account.
- **Large-Batch Processing:** Unlike standard SQL where one transaction usually requires multiple round-trip queries, TigerBeetle can execute up to 8,189 transactions in a single batch query, maximizing disk I/O efficiency.
- **End-to-End Integrity:** TigerBeetle protects every byte with 128-bit checksums and handles “Gray Failure” (where a disk becomes slow but doesn’t fail completely) by falling back on other replicas in the cluster.

## System of Record vs. System of Reference

In a modern fintech stack, TigerBeetle is used alongside a general-purpose database (referred to as an OLGP database). The OLGP database (e.g., Postgres) acts as the “System of Reference,” storing metadata such as account holder names, addresses, and terms. TigerBeetle acts as the “System of Record,” storing only the integers representing accounts and the transfers between them.

Press enter or click to view image in full size

![](https://miro.medium.com/v2/resize:fit:700/1*8dWy4avge6S6S48YZ_s_6g.png)

## Distributed Transaction Coordination: Two-Phase Commit vs. Sagas

As fintech systems transition from monoliths to microservices, a single financial operation often spans multiple independent services (e.g., an Order Service, a Payment Service, and a Warehouse Service). Coordinating these services while maintaining the ACID properties of a transaction is a significant distributed systems challenge.

## Two-Phase Commit (2PC)

2PC is a protocol designed to ensure that all nodes in a distributed system either commit or roll back a transaction as a single atomic unit. It uses a central coordinator to manage two distinct phases.

- **Prepare Phase:** The coordinator asks each participating node if it can promise to carry out the update.
- **Commit Phase:** If all nodes respond positively, the coordinator sends the commit command. If any node fails, all nodes are told to abort.

**The Bottleneck:** 2PC provides strong consistency but is slow and vulnerable to failure. Because it requires resource locks to be held across all participating services until the final commit is acknowledged, it is prone to performance issues in high-latency or high-volume environments. It is generally avoided for long-running processes or those involving external APIs.

## The Saga Pattern

The Saga pattern addresses the limitations of 2PC by embracing eventual consistency. A saga decomposes a large transaction into a sequence of smaller, independent local transactions. Each local transaction updates the state of a single service and commits immediately, releasing its locks.

Press enter or click to view image in full size

![](https://miro.medium.com/v2/resize:fit:700/1*bCiPo4a8H_MWzNfWZGeC7g.png)

If a step in the sequence fails, the saga does not “rollback” in the traditional sense. Instead, it executes a series of “compensating transactions” to undo the changes made by previous successful steps. For example, if the Payment Service fails after the Inventory Service has already reduced stock, the saga triggers a “Restock Inventory” command.

Sagas utilize three types of transactions :

- **Compensable Transactions:** Steps that can be reversed.
- **Pivot Transaction:** The “go/no-go” point. Once this step commits, the saga is guaranteed to run to completion.
- **Retryable Transactions:** Steps following the pivot that are guaranteed to eventually succeed.

Press enter or click to view image in full size

![](https://miro.medium.com/v2/resize:fit:700/1*vXStpdGExMitA7xruuY81w.png)

## Global Compliance, Risk, and Regulatory Engineering

Architecture in fintech is constrained as much by law and compliance as it is by latency and throughput. Companies like Adyen and Stripe must design for strict regulatory environments including PCI DSS, GDPR, and Anti-Money Laundering (AML) laws.

## Regional Failure Isolation and Data Residency

Compliance often shapes the topology of a global payment platform. Certain jurisdictions require that payment data remains within geographic boundaries. This necessitates a “Regional Data Plane” architecture where transactions are processed and stored locally, while a “Global Control Plane” manages cross-region routing and aggregated reporting.

Regional failure isolation is critical. If a card network in Europe experiences an outage, it must not degrade the performance of transactions in the United States. Regions are treated as distinct failure domains that must operate independently under pressure.

## Fraud Detection and Real-Time AI (Stripe Radar & Adyen Uplift)

Fraud prevention is an integral layer of the fintech stack. Stripe Radar and Adyen Uplift utilize machine learning to assess the risk of a transaction in real-time, often in less than 100 milliseconds. These systems analyze over 1,000 characteristics of a transaction, including device fingerprints, IP addresses, customer journey metrics, and behavioral patterns.

**Fraud SignalAnalysis MechanismInput Speed** Analyzes the time between keystrokes at checkout to detect bots. **Velocity** Monitors number of failed attempts in a 10-minute window to detect card testing. **3DS Context** Analyzes if authentication friction (3D Secure) is necessary for the risk level. **Device DNA** Uses cookies, hardware signals, and IP geofencing to identify bad actors.

This risk assessment is conditional: it determines the choice of payment rails, the retry strategy if a payment fails, and whether intrusive authentication (like 3D Secure) is required.

## Transaction Lifecycle: The Immutable State Machine

A financial transaction is not a single, stateless event; it is a long-lived state machine. In systems like Blnk, every transaction progresses through a defined lifecycle, with each state change stored as a separate, immutable record.

### The Lifecycle States

- **QUEUED:** The initial state where a transaction is received and waiting for processing.
- **INFLIGHT:** A critical state for “holds” or “authorizations.” When a user swipes a card at a gas station, a $100.00 inflight hold might be placed on their balance. The funds are neither “available” nor “spent”; they are in limbo.
- **APPLIED:** The successful completion of the transaction where funds are permanently moved between accounts.
- **VOID:** Occurs when an in-flight transaction is cancelled (e.g., the hold is released). The balance is restored without ever having “left” the account.
- **REJECTED:** The transaction was denied due to insufficient funds, risk flags, or processor errors.

Press enter or click to view image in full size

![](https://miro.medium.com/v2/resize:fit:700/1*jfe5RiPoVrWx0hgm886PdA.png)

### Traceability and Lineage

Because every state change is a separate record in the database, the system maintains a perfect lineage. Each new state record contains a `parent_transaction_id` pointing to the previous state. This allows for "Balance Reconstruction" at any point in time and provides auditors with a verifiable history of how an initial authorization evolved into a final settlement or a refund.

### Distributed Locking Strategies: Redis vs. Zookeeper

When fintech applications require coordination across distributed nodes to ensure “correctness” (e.g., preventing the same merchant from withdrawing their balance twice through different API nodes), the choice of a locking mechanism is critical.

## Performance vs. Correctness Trade-offs

The choice between Redis and ZooKeeper is a classic trade-off between speed and safety.

- **Redis (Redlock):** Prioritizes performance. As an in-memory store, it is extremely fast. However, it relies on a Time-to-Live (TTL) for lock safety. If a process experiences a long garbage collection pause that exceeds the TTL, the lock may expire while the process still believes it holds it, leading to potential data corruption.
- **Zookeeper:** Prioritizes consistency and reliability. It uses ephemeral znodes and the ZAB consensus protocol. If a client dies, the lock is automatically removed. It provides “fencing tokens” (like the zxid), which the database can use to reject writes from a client that lost its lock during a network partition.

Press enter or click to view image in full size

![](https://miro.medium.com/v2/resize:fit:700/1*p0aIjwLtZyZ4MD3FsG3jPA.png)

## Conclusion: The Hierarchy of Safety

Building a “bank-grade” fintech system is not a matter of choosing a single pattern, but of layering them like an onion. Each layer assumes that the layer beneath it may fail, creating a robust, multi-layered defense against data corruption and unrecoverable capital loss.

Press enter or click to view image in full size

![](https://miro.medium.com/v2/resize:fit:700/1*wi_KKrwzJAIHx5odE2YMBQ.png)

The hierarchy begins at the API Edge with idempotency keys to handle the volatility of network communication and user error. Beneath this, the Concurrency Layer uses optimistic locking and specialized OLTP architectures to manage the high-throughput demands of modern financial paths without the performance-killing bottlenecks of row-level locks. At the Core Database Layer, the practice of overwriting data is replaced by immutable, append-only double-entry bookkeeping, providing a mathematically verifiable “Source of Truth”. Finally, the Distributed Coordination Layer utilizes Sagas and smart retries to ensure that complex, multi-service workflows eventually reach a consistent state, even in the face of partial system failure.

Fintech system design is fundamentally rooted in a philosophy of pessimism. By assuming that the network will fail, the database will be under load, and the user will double-tap, engineers can construct systems that quite literally cannot lie. As transaction volumes continue to grow and global regulations become more complex, these patterns remain the essential building blocks for moving money at the speed of the internet while maintaining the absolute trust required for a global financial ecosystem.

[Programming](https://medium.com/tag/programming?source=post_page-----ee5d73f33415---------------------------------------)

[Software Development](https://medium.com/tag/software-development?source=post_page-----ee5d73f33415---------------------------------------)

[Software Engineering](https://medium.com/tag/software-engineering?source=post_page-----ee5d73f33415---------------------------------------)

[Technology](https://medium.com/tag/technology?source=post_page-----ee5d73f33415---------------------------------------)

[![CodeToDeploy](https://miro.medium.com/v2/resize:fill:48:48/1*s4SuUoJSUCQqhZfIZuM85A.png)](https://medium.com/codetodeploy?source=post_page---post_publication_info--ee5d73f33415---------------------------------------)

[![CodeToDeploy](https://miro.medium.com/v2/resize:fill:64:64/1*s4SuUoJSUCQqhZfIZuM85A.png)](https://medium.com/codetodeploy?source=post_page---post_publication_info--ee5d73f33415---------------------------------------)

[**Published in CodeToDeploy**](https://medium.com/codetodeploy?source=post_page---post_publication_info--ee5d73f33415---------------------------------------)

[4.4K followers](https://medium.com/codetodeploy/followers?source=post_page---post_publication_info--ee5d73f33415---------------------------------------)

· [Last published 13 hours ago](https://medium.com/codetodeploy/understand-sockets-the-kernel-level-abstraction-67cab43c1443?source=post_page---post_publication_info--ee5d73f33415---------------------------------------)

Tech Insights, Career Growth & High-Paying Opportunities

[![Roman Fedytskyi](https://miro.medium.com/v2/resize:fill:48:48/1*xUFTpu45tCJsJ_s-VVYGlA.png)](https://medium.com/@roman_fedyskyi?source=post_page---post_author_info--ee5d73f33415---------------------------------------)

[![Roman Fedytskyi](https://miro.medium.com/v2/resize:fill:64:64/1*xUFTpu45tCJsJ_s-VVYGlA.png)](https://medium.com/@roman_fedyskyi?source=post_page---post_author_info--ee5d73f33415---------------------------------------)

[**Written by Roman Fedytskyi**](https://medium.com/@roman_fedyskyi?source=post_page---post_author_info--ee5d73f33415---------------------------------------)

[3.5K followers](https://medium.com/@roman_fedyskyi/followers?source=post_page---post_author_info--ee5d73f33415---------------------------------------)

· [9.3K following](https://medium.com/@roman_fedyskyi/following?source=post_page---post_author_info--ee5d73f33415---------------------------------------)

Engineer and product thinker with 10+ years in fintech and e-commerce, writing about AI-driven frontend architecture, design systems, and digital experiences.

[Help](https://help.medium.com/hc/en-us?source=post_page-----ee5d73f33415---------------------------------------)

[Status](https://status.medium.com/?source=post_page-----ee5d73f33415---------------------------------------)

[About](https://medium.com/about?autoplay=1&source=post_page-----ee5d73f33415---------------------------------------)

[Careers](https://medium.com/jobs-at-medium/work-at-medium-959d1a85284e?source=post_page-----ee5d73f33415---------------------------------------)

[Press](mailto:pressinquiries@medium.com)

[Blog](https://blog.medium.com/?source=post_page-----ee5d73f33415---------------------------------------)

[Store](https://medium.com/store)

[Privacy](https://policy.medium.com/medium-privacy-policy-f03bf92035c9?source=post_page-----ee5d73f33415---------------------------------------)

[Rules](https://policy.medium.com/medium-rules-30e5502c4eb4?source=post_page-----ee5d73f33415---------------------------------------)

[Terms](https://policy.medium.com/medium-terms-of-service-9db0094a1e0f?source=post_page-----ee5d73f33415---------------------------------------)

[Text to speech](https://speechify.com/medium?source=post_page-----ee5d73f33415---------------------------------------)

reCAPTCHA

Recaptcha requires verification.

protected by **reCAPTCHA**