# I/O Bound vs CPU Bound — Thread Model & Performance

> **CF page ID:** 51872636 | **Parent:** 🏛️ Architecture & Principles (51642382)
> **Source of truth:** this file → push to CF

---

## Core Concept (read first)

> **Two types of bottleneck, two different handling strategies:**
>
> - **I/O bound** — code is *waiting*: waiting for the DB to respond, for Redis, for a Kafka ack, for a bank API. The thread/event loop is idle during this time.
> - **CPU bound** — code is *computing*: validating amounts, computing fees, serializing JSON, verifying HMAC. The thread is genuinely busy.
>
> Confusing the two → wrong thread model → performance cliff or resource waste.

---

## Concrete Example in GtelPay

A deposit webhook request does the following. Which type is each step:

| Step | Operation | Type | Typical latency |
|------|-----------|------|----------------|
| 1 | Redis: idempotency + auth + limit check | **I/O bound** | 0.2ms × 3 = 0.6ms |
| 2 | Compute fee (`BigDecimal` scale 4) | **CPU bound** | < 0.1ms |
| 3 | Validate amount, currency, businessRef | **CPU bound** | < 0.1ms |
| 4 | PostgreSQL: INSERT outbox row | **I/O bound** | 3ms |
| 5 | RabbitMQ: publish BANK_DEPOSIT | **I/O bound** | 0.5ms |
| 6 | Serialize HTTP response JSON | **CPU bound** | < 0.1ms |

**Result:** ~99% of request time is I/O wait. CPU-bound work accounts for < 1%.

→ Vert.x event loop model is a perfect fit: while one request is waiting for Redis, the event loop handles another request.

---

## I/O Bound

**Characteristic:** the thread sends a request to an external resource, then *waits*. CPU is idle during the wait.

**I/O points in GtelPay:**

| Resource | Operation | Typical latency |
|----------|-----------|----------------|
| Redis | GET/SET (auth, idempotency, limits) | 0.2–0.5ms |
| PostgreSQL | INSERT wallet_tx / outbox | 2–5ms |
| TigerBeetle | create_transfer, post_pending | 1–3ms |
| RabbitMQ | publish command | 0.5–2ms |
| Kafka | emit event | 1–5ms |
| Bank API (external) | HTTP POST settlement | 50–500ms |

**Suitable thread model:** non-blocking I/O (Vert.x event loop, async/reactive). One thread can serve thousands of concurrent I/O waits — no thread-per-request needed.

**Anti-pattern:** blocking call on the Vert.x event loop thread:
```java
// ❌ Blocks event loop — the entire verticle hangs while the query runs
String result = jdbcTemplate.queryForObject(...);

// ✅ Offload to worker thread pool
vertx.executeBlocking(() -> jdbcTemplate.queryForObject(...), res -> { ... });
```

---

## CPU Bound

**Characteristic:** the thread is genuinely computing — not waiting on anyone. CPU is 100% busy for the full duration.

**CPU-bound points in GtelPay:**

| Operation | Class | Notes |
|-----------|-------|-------|
| Fee computation | `FeeCalculator` | `BigDecimal` arithmetic, scale 4 HALF_UP |
| Amount validation | `MoneyUtil.parseAmount()` | Reject zero, reject negative, reject overflow |
| Idempotency key check | `BusinessRefValidator` | String comparison, no crypto |
| HMAC signature verify | Webhook handler | Crypto — can take ~0.5ms on large payloads |
| JSON serialize/deserialize | Jackson mapper | ~0.1ms for small payloads |
| COA account lookup (in-memory) | `CoaAccountValidator` | Cached in map — O(1), not I/O |

**Suitable thread model:** CPU-bound work that is short (< 1ms) can run directly on the event loop. Long CPU-bound work (> 1ms, e.g. bulk report generation, PDF export) must be offloaded to a worker thread pool — never block the event loop.

**Rule:** if a method runs > 1ms with no I/O, profile it before putting it on the event loop.

---

## Vert.x Event Loop — Why It Fits GtelPay

`app-orchestration` uses Vert.x. The event loop model handles I/O bound workloads efficiently:

```
Thread (event loop)
  │
  ├─ Request A arrives → start Redis call (non-blocking) → move on
  ├─ Request B arrives → start Redis call (non-blocking) → move on
  ├─ Request C arrives → start Redis call (non-blocking) → move on
  │
  ├─ Redis callback for A → compute fee (CPU, < 0.1ms) → start DB write → move on
  ├─ Redis callback for B → compute fee → start DB write → move on
  │
  ├─ DB callback for A → publish RabbitMQ → return 202
  └─ DB callback for B → publish RabbitMQ → return 202
```

A single thread can handle hundreds of concurrent requests because each request only uses the thread for < 0.2ms total (CPU time); the rest is I/O wait.

**Comparison with thread-per-request (Spring MVC blocking):**

| | Vert.x Event Loop | Spring MVC (blocking) |
|---|---|---|
| **Suited for** | I/O bound — many concurrent waits | CPU bound — few requests, heavy compute |
| **Threads for 1000 concurrent req** | 1–4 event loop threads | 1000 threads (200 default pool → queue) |
| **Memory** | Low — no stack allocation per request | High — each thread ~1MB stack |
| **Latency at high load** | Stable as long as no blocking calls | Increases when thread pool is exhausted |
| **Risk** | Blocking call on event loop = entire verticle hangs | Thread pool exhaustion = timeout |

---

## Practical Rules

| Situation | Action |
|-----------|--------|
| Redis / DB / queue call in Vert.x | Use async client (Vert.x Redis client, async JDBC) — do not block |
| CPU work < 1ms (fee calc, validation) | Run directly on event loop — OK |
| CPU work > 1ms (batch, PDF, report) | `vertx.executeBlocking()` — offload to worker pool |
| External HTTP call (bank API) | Use Vert.x `WebClient` async — do not use blocking `RestTemplate` |
| Spring Data JPA (blocking by default) | Only in `app-wallet`, `app-accounting` — not in `app-orchestration` |

---

## Module Thread Model

| Module | Thread model | Reason |
|--------|-------------|--------|
| `app-orchestration` | Vert.x event loop | I/O bound — Redis + DB + queue, high concurrent requests |
| `app-wallet` | Spring Boot (blocking OK) | Sync HTTP, lower throughput, Spring Data JPA |
| `app-accounting` | Spring Boot (blocking OK) | Sync HTTP, TigerBeetle calls via blocking client |
| `app-wallet-worker` | Spring AMQP listener | Message consumer — throughput controlled by prefetch |
| `app-accounting-worker` | Spring AMQP listener | Message consumer — throughput controlled by prefetch |

**Note:** `app-wallet` and `app-accounting` use blocking I/O without issue because they are not public-facing hot-path services — they only receive requests from the orchestrator at controlled concurrency.

---

## Source of truth

`claude/confluence/` is the local source of truth. Confluence is the mirror — when conflict, **local wins**.
