# k6 Load Tests

## Setup

```bash
brew install k6
```

## Pre-conditions

Tests assume wallet members are pre-seeded with sufficient balance. See `seed/` for SQL scripts (TODO).

| Scenario | Member range | Min balance (VND) |
|---|---|---|
| transfer | 1001–1100 | 1,000,000,000 |
| withdraw | 2001–2100 | 10,000,000,000 |
| ibft | 3001–3100 | 100,000,000,000 |

## Running

```bash
# Sanity check — all endpoints reachable
k6 run smoke.js

# Baseline TPS — internal transfer (simplest sync write path)
k6 run scenarios/transfer.js

# Withdraw 3-phase load (50 concurrent users)
k6 run -e CONCURRENT_USERS=50 scenarios/withdraw.js

# IBFT 3-phase load (30 concurrent users)
k6 run -e CONCURRENT_USERS=30 scenarios/ibft.js

# Against staging
k6 run -e BASE_URL=http://staging-host:8080 scenarios/transfer.js
```

## Key metrics to watch

| Metric | Target |
|---|---|
| `transfer_latency` p95 | < 300ms |
| `withdraw_accept_latency` p95 | < 400ms |
| `ibft_accept_latency` p95 | < 500ms |
| error rate | < 1% |

## Bottleneck hypothesis

Vert.x I/O thread is non-blocking; Spring JPA + PostgreSQL is the expected bottleneck.
If p95 degrades under concurrent load → check HikariCP pool size and PostgreSQL connection limit.
