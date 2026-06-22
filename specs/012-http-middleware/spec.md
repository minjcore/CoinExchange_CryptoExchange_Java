# Feature Specification: HTTP Middleware / Interceptor Chain

**Feature Branch**: `012-http-middleware`
**Created**: 2026-06-22
**Status**: Draft
**UC**: Cross-cutting — applies to all endpoints in `app-orchestration`

## Context

All business endpoints in `app-orchestration` share cross-cutting concerns that currently have no consistent implementation: authentication, audit logging, idempotency header validation, and rate limiting. These are implemented as a Vert.x handler chain inserted before each route's business handler.

The interceptor chain executes in this fixed order:

```
Request → [Auth] → [RateLimit] → [IdempotencyExtract] → [BusinessHandler] → [AuditLog] → Response
```

---

## User Scenarios & Testing

### User Story 1 — Unauthenticated Request Rejected (Priority: P1)

Any request without a valid `Authorization` header is rejected before reaching business logic.

**Acceptance Scenarios**:

1. **Given** a request with no `Authorization` header, **When** any `/v1/*` endpoint is called, **Then** 401 is returned; business handler is never invoked.
2. **Given** a request with an expired API key, **When** called, **Then** 401 is returned.
3. **Given** a valid API key, **When** called, **Then** request proceeds to the next interceptor.
4. **Given** `/health` endpoint, **When** called without auth, **Then** 200 is returned — health is exempt from auth.

---

### User Story 2 — Every Business Request Logged (Priority: P1)

Every request that reaches a business handler is recorded in an audit log with enough context to reconstruct what happened.

**Acceptance Scenarios**:

1. **Given** a successful transfer request, **When** the response is sent, **Then** an audit record is written containing: `businessRef`, `endpoint`, `memberId` (from request body), `statusCode`, `durationMs`, `requestedAt`.
2. **Given** a request that fails validation (4xx), **When** the response is sent, **Then** an audit record is written with `statusCode` and error reason.
3. **Given** a request that throws an unhandled exception (5xx), **When** the response is sent, **Then** audit record is still written — logging must not depend on business handler success.

---

### User Story 3 — Idempotency Key Extracted Uniformly (Priority: P2)

The `X-Idempotency-Key` header is the canonical `businessRef`. Currently each handler reads from the body independently. The interceptor extracts and validates it once, making it available to all handlers via routing context.

**Acceptance Scenarios**:

1. **Given** a POST request with `X-Idempotency-Key: abc-123`, **When** the interceptor runs, **Then** the routing context has `businessRef = "abc-123"` set.
2. **Given** a POST request without `X-Idempotency-Key`, **When** the interceptor runs, **Then** the body's `businessRef` field is used as fallback; no 400 is returned.
3. **Given** `X-Idempotency-Key` and `businessRef` in body are both present and differ, **When** the interceptor runs, **Then** `X-Idempotency-Key` wins; body value is ignored.

---

### User Story 4 — Rate Limiting per Caller (Priority: P3)

Each authenticated caller is limited to a configurable request rate to prevent a single caller from saturating the system.

**Acceptance Scenarios**:

1. **Given** a caller that exceeds `rate.limit.rps` (default: 1000 req/s per API key), **When** a request arrives, **Then** 429 is returned with `Retry-After` header.
2. **Given** a caller within the rate limit, **When** called, **Then** request proceeds normally.
3. **Given** rate limiting is disabled (`rate.limit.enabled=false`), **When** called, **Then** all requests proceed regardless of rate.

---

## Requirements

### Functional Requirements

- **FR-001**: Auth interceptor MUST reject any `/v1/*` request without a valid `Authorization: Bearer <api-key>` header with HTTP 401.
- **FR-002**: `/health` and `/v1/bench/*` endpoints MUST be exempt from authentication.
- **FR-003**: Audit log MUST be written for every request that enters the business handler, regardless of outcome.
- **FR-004**: Audit log MUST contain at minimum: `businessRef`, `path`, `method`, `statusCode`, `durationMs`, `callerKey` (masked), `requestedAt`.
- **FR-005**: Audit log MUST NOT contain request/response body — avoid PII and large payload logging.
- **FR-006**: Idempotency interceptor MUST extract `X-Idempotency-Key` and set it on the routing context as `ctx.put("businessRef", ...)`.
- **FR-007**: Rate limiter MUST operate per API key (caller identity), not per IP.
- **FR-008**: Rate limit configuration MUST be hot-reloadable without restart.
- **FR-009**: All interceptors MUST be non-blocking — no `Thread.sleep`, no synchronous DB calls in the interceptor chain.

### Non-Functional Requirements

- Interceptor chain overhead MUST be < 1ms p99 (excluding audit log async write).
- Audit log write MUST be asynchronous — fire-and-forget to a dedicated writer; never block the response path.

### Key Entities

- **ApiKey**: `id`, `keyHash` (SHA-256), `callerName`, `enabled`, `rpsLimit`
- **AuditEntry**: `id`, `businessRef`, `path`, `method`, `statusCode`, `durationMs`, `callerKey`, `requestedAt`
- **InterceptorChain**: Vert.x `Route.handler()` chain — order is fixed: Auth → RateLimit → IdempotencyExtract → Business → AuditLog

### Implementation Notes

- Auth: compare `Authorization` bearer token against `SHA-256(token)` stored in `api_keys` table. Cache in-memory with 60s TTL.
- Audit log: write to `audit_log` table async via a dedicated Spring `@Async` writer or a bounded in-memory queue draining to DB.
- Rate limiter: token bucket per API key, stored in-memory (`ConcurrentHashMap<String, TokenBucket>`). Not distributed — single-node only for S1.
- Idempotency: `ctx.put("businessRef", key)` — business handlers read from context, not body, after this interceptor runs.

### Out of Scope

- Distributed rate limiting (Redis-backed) — S2 concern.
- JWT validation — API key is sufficient for S1 internal callers.
- mTLS — handled at load balancer level.
