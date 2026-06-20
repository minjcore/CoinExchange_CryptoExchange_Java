# ADR-038: Orchestrator is a separate service; domains accessed via a gateway seam over the network

| Field | Value |
|-------|-------|
| Status | Accepted |
| Date | 2026-06-10 |
| Source | Design discussion; supersedes the in-process framing in [`spec/trd/wallet.md`](../spec/trd/wallet.md) §7.2 and [`spec/architecture-overview.md`](../spec/architecture-overview.md) §2 |
| Related | [ADR-002](ADR-002-core-sharedlib.md), [ADR-003](ADR-003-dual-schema-single-postgres.md), [ADR-008](ADR-008-saga-compensation-no-2pc.md), [ADR-012](ADR-012-orchestration-integration-forbidden-rules.md) |

---

## Context

`app-orchestration` is a thin BFF that sequences the two cores (`core.wallet`, `core.accounting`) per use case ([ADR-012](ADR-012-orchestration-integration-forbidden-rules.md): not a domain, sole sequencer). Earlier docs framed the orchestrator→domain call as **in-process** Java (`spec/trd/wallet.md` §7.2 "Application calls domain services directly"; `architecture-overview` §2 "S2 **or** in-process"). That framing is **not the intended architecture** — it couples the orchestrator to domain classes in one JVM.

## Decision

1. **The orchestrator is a separate service.** It calls the domains **over the network** — `core.accounting` via S2 ([`accounting-internal.yaml`](../spec/contracts/open-api/accounting-internal.yaml)); `core.wallet` via the wallet-internal HTTP surface ([`wallet-internal.yaml`](../spec/contracts/open-api/wallet-internal.yaml)).
2. **Gateway seam is the boundary.** Use cases depend only on `WalletGateway` / `LedgerGateway` interfaces — never on domain service classes directly. The network boundary is the design.
3. **In-process is transitional, not the design.** The current `InProcessWalletGateway` / `InProcessLedgerGateway` (delegating to domain services in one JVM) is a **temporary** implementation while the HTTP surfaces are built. It must not be documented as an architectural option; it is a migration step that the seam makes invisible to use cases.
4. **Thin orchestrator preserved.** Only IO + sequencing + fee/auth/idempotency-key propagation live here; domain logic stays in the cores. Saga semantics unchanged ([ADR-008](ADR-008-saga-compensation-no-2pc.md)).
5. **Language.** Orchestrator is Java/Vert.x today; because the boundary is HTTP, a non-Java orchestrator is a drop-in later — not required.

## Consequences

- Domains become independently deployable; the orchestrator can scale/deploy/rewrite separately.
- Splitting the JVM is a gateway-impl swap (in-process → HTTP client) with **no** change to use cases or domains.
- Revises the v1 "one Spring Boot process" assumption in [ADR-003](ADR-003-dual-schema-single-postgres.md) context for the orchestrator↔domain boundary (the dual-schema storage decision itself stands).
- `core.wallet` needs a network surface (wallet-internal contract) to match `core.accounting`'s S2 — tracked as follow-up.

## Acceptance criteria (AC-038)

| ID | Criterion |
|----|-----------|
| AC-038-01 | Orchestrator use-case code depends only on `WalletGateway` / `LedgerGateway` interfaces — no import of a `core.wallet` / `core.accounting` service class |
| AC-038-02 | Target call path to each domain is HTTP (S2 `accounting-internal`, wallet-internal) — the network boundary is the design, not an option |
| AC-038-03 | Swapping `InProcess*Gateway` → HTTP-client impl requires **no** change to use cases or domains |
| AC-038-04 | Orchestrator holds only IO + sequencing + fee/auth/idempotency-key propagation — no balance math, no DR/CR posting rules |
| AC-038-05 | Saga compensation semantics ([ADR-008](ADR-008-saga-compensation-no-2pc.md)) hold unchanged across the network seam (timeout/failure → compensating step) |
| AC-038-06 | The HTTP boundary admits a non-Java orchestrator with no domain change |

---

## Test cases (TC-038)

| ID | Expected | Maps to |
|----|----------|---------|
| TC-038-01 | Arch test: orchestrator use-case package imports no domain service class | AC-038-01 |
| TC-038-02 | Use cases pass identically against `InProcess*Gateway` and HTTP gateway impl | AC-038-03 |
| TC-038-03 | Review/arch test: no DR/CR or balance arithmetic in orchestrator | AC-038-04 |
| TC-038-04 | Payment over HTTP gateway: wallet debit OK, ledger post fails → compensating credit fires | AC-038-05, ADR-008 |
| TC-038-05 | *(structural)* `wallet-internal.yaml` surface exists, shape parallels `accounting-internal` S2 | AC-038-02 |

---

## References

- [`spec/trd/wallet.md`](../spec/trd/wallet.md) §7.2 — wallet service API (consumed via the seam)
- [`spec/architecture-overview.md`](../spec/architecture-overview.md) §2 — orchestrator wraps cores
- [`spec/contracts/open-api/accounting-internal.yaml`](../spec/contracts/open-api/accounting-internal.yaml) — S2
