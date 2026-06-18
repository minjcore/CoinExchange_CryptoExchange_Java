# Data Plane Architecture — GtelPay Core Money Movement

> **CF page ID:** 43581441 | **Root page of this section**
> **Source of truth:** this file → push to CF
> **Reflects:** Constitution v1.0.0 (ratified 2026-06-15)

---

## Tổng quan

GtelPay Core xử lý luồng tiền qua hai domain: `core.accounting` quản lý double-entry ledger, `core.wallet` quản lý số dư per-member. Hai domain được điều phối bởi `app-orchestration` — không gọi trực tiếp nhau.

---

## Data Plane là gì?

Thuật ngữ mượn từ network infrastructure: **Data Plane** là đường forwarding nhanh xử lý từng packet, **Control Plane** đưa ra quyết định routing bất đồng bộ.

Trong thanh toán:

| | Data Plane | Control Plane |
|-|-----------|---------------|
| **Xử lý** | Mỗi giao dịch, realtime, < 100ms | Batch, async, seconds→hours |
| **Ví dụ** | Auth check, wallet debit, route to bank | Ledger write, audit log, reconciliation |
| **State access** | Redis pre-loaded (0.2ms/read) | PostgreSQL query (5-20ms/query) |
| **Failure impact** | Blocks customer transaction | Delays reporting only |

---

## Tại sao không dùng microservice thuần?

```
Microservice hot path (5–6 network hops):
  API GW → Auth Svc → Payment Svc → Fraud Svc → Wallet Svc → Ledger Svc
  Mỗi hop = 5–15ms → tổng 40–90ms TRƯỚC KHI gọi bank

Data Plane hot path (1 process):
  Orchestrator → [Redis checks in-process] → wallet DB write → Kafka emit
  ~3–7ms internal overhead, chỉ 1 external hop (đến bank)
```

**The fundamental shift:** pre-checks, routing, fraud screening = **in-process function calls**, không phải network hops. State được **Control Plane push sẵn vào Redis** — hot path không bao giờ query relational DB cho config hay rules.

---

## Kiến trúc Data Plane GtelPay

```
External client / Bank webhook
    │ HTTP (S1 — gtelpay-public.yaml)
    ▼
┌─────────────────────────────────────────────────────┐
│              app-orchestration                       │
│  Redis pre-checks: auth, idempotency, limits, fraud  │
│  Fee computation (once, here only)                   │
│  1 PostgreSQL write (outbox / wallet)                │
│  Kafka emit (async, non-blocking)                    │
└──────────────┬──────────────────────┬───────────────┘
               │ HTTP (sync UC)       │ RabbitMQ (async UC)
    ┌──────────┴──────┐    ┌──────────┴──────────────────┐
    │   app-wallet    │    │    app-accounting-worker     │
    │   app-accounting│    │    app-wallet-worker         │
    └──────────┬──────┘    └──────────┬──────────────────┘
               │                      │
    ┌──────────┴──────┐    ┌──────────┴──────┐
    │  core.wallet    │    │ core.accounting  │
    │  (PostgreSQL)   │    │ (PostgreSQL +    │
    └─────────────────┘    │  TigerBeetle)    │
                           └─────────────────┘
```

**Hot path (deposit):** outbox write + 202 = ~7ms internal. Ledger Phase A/B chạy async sau đó.

---

## Hai domain cốt lõi

| Domain | Câu hỏi trả lời | Schema | ADR |
|--------|----------------|--------|-----|
| `core.wallet` | Member này có thể chi tiêu bao nhiêu? | `wallet` | ADR-002 |
| `core.accounting` | Điều gì đã xảy ra trên ledger? | `accounting` | ADR-003 |

**Ranh giới cứng:** không cross-schema JOIN, không cross-domain import, không gọi trực tiếp nhau. Orchestration là cầu nối duy nhất.

---

## Latency Budget

| Thành phần | Microservice | Data Plane |
|-----------|-------------|-----------|
| Pre-checks (auth, idempotency, limits, fraud) | 40ms (4× network+DB) | 0.8ms (4× Redis) |
| Business logic | 10ms (network) | ~0ms (in-process) |
| Wallet write | 15ms (network+DB) | 3ms (in-process PostgreSQL) |
| Ledger + audit + notification | 35ms (network) | ~0ms (async Kafka) |
| **Total internal** | **~100ms** | **~4ms** |

---

## Control Plane contracts

```
CP → DP:  Push state to Redis before transactions arrive. Never insert yourself in the request path.
DP → CP:  Emit events to Kafka. Never wait for acknowledgment.
          If Kafka unreachable → write to outbox; relay when available.
Rule:     DP never calls CP synchronously. CP never handles live customer traffic.
```

---

## Navigation — Tài liệu trong section này

| Section | Trang | Nội dung |
|---------|-------|---------|
| 📌 Start Here | Architecture FAQ | Module ownership, HTTP vs queue, Q&A, anti-patterns |
| 📌 Start Here | Terminology Reference | Platform naming vs TRD/REST mapping |
| 🏛️ Architecture & Principles | Wallet & Accounting Overview | Two-domain, orchestration, surface map |
| 🏛️ Architecture & Principles | Platform Boundaries | Hard rules, allowed call paths |
| 🏛️ Architecture & Principles | Correlation & Idempotency | businessRef flow end-to-end |
| 📋 Domain TRDs | Accounting TRD | FR/NFR core.accounting, two-phase posting |
| 📋 Domain TRDs | Wallet TRD | FR/NFR core.wallet, balance invariants |
| 🔌 Contracts & Integration | Integration Surfaces | S1–S6 surface catalog, use case matrix |
| 🔌 Contracts & Integration | Core Platform Design | COA, fund flow, foundation scope |
| 🏗️ Build & Process | Business Process: Deposit | Full deposit flow, DR/CR, non-happy paths |
| 🏗️ Build & Process | Implementation Decisions | Module layout, build phases, DDL decisions |
| 🔄 Use Cases | UC-1 through UC-9 | Orchestration contracts per use case |
| ✅ Kết Luận | Conclusion | 3 core decisions, 7 principles recap, next steps |

---

## Trade-offs

| Trade-off | Gain | Cost |
|-----------|------|------|
| Redis pre-loaded state | Sub-ms checks on every transaction | ~1 min/year outage risk; must invest Redis HA |
| Ledger async (eventual) | Hot path < 100ms | Ledger lags seconds; reconciliation catches drift |
| Business logic concentrated in orchestrator | No inter-service latency | Orchestrator is larger, more complex |
| Wallet = only DB write on hot path | Minimal disk I/O | Must shard wallet DB at scale |

---

## Source of truth

`claude/confluence/` là local source of truth. Confluence là mirror — khi conflict, **local wins**.
