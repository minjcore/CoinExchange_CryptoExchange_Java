# ADR-018: OpenAPI and AsyncAPI as wire contract source of truth

| Field | Value |
|-------|-------|
| Status | Accepted |
| Date | 2026-06-08 |
| Source | [`core.foundation.md`](../core.foundation.md) §5, [`integration-surfaces.md`](../integration-surfaces.md) §1–3 |
| Related | [ADR-002](ADR-002-core-foundation-shared-library.md), [ADR-012](ADR-012-orchestration-integration-forbidden-rules.md) |

---

## Decision

1. **S1** [`spec/contracts/open-api/gtelpay-public.yaml`](../spec/contracts/open-api/gtelpay-public.yaml) — public HTTP shapes.
2. **S2** [`spec/contracts/open-api/accounting-internal.yaml`](../spec/contracts/open-api/accounting-internal.yaml) — internal journal API.
3. **S3** [`spec/contracts/async-api/core-events.yaml`](../spec/contracts/async-api/core-events.yaml) — Kafka events.
4. **S6** [`spec/contracts/async-api/core-commands.yaml`](../spec/contracts/async-api/core-commands.yaml) — RabbitMQ full-body envelope.
5. Foundation v1 **does not** duplicate command DTOs — Application maps wire → domain methods.
6. Field names consistent across S1/S3/S6 ([ADR-012](ADR-012-orchestration-integration-forbidden-rules.md) F4).
7. Gateway routes via S4 config to orchestration only — not domain modules.

---

## Acceptance criteria (AC-018)

| ID | Criterion |
|----|-----------|
| AC-018-01 | No public HTTP in wallet/accounting modules |
| AC-018-02 | OpenAPI lint passes in CI |
| AC-018-03 | AsyncAPI envelope matches integration-surfaces §6 |
| AC-018-04 | `businessRef` named consistently on all surfaces |
| AC-018-05 | Foundation has ApiResponse only — not full payment DTOs |

---

## Test cases (TC-018)

| ID | Expected | Maps to |
|----|----------|---------|
| TC-018-01 | `spec/contracts/open-api/README` lint green | CI |
| TC-018-02 | S6 sample validates against asyncapi schema | Contract test |
| TC-018-03 | S1 idempotency header documented | X-E02 |
| TC-018-04 | No duplicate DTO in foundation | AC-018-05, ADR-002 |

---

## References

- [`integration-surfaces.md`](../integration-surfaces.md)
- [`spec/contracts/open-api/README.md`](../spec/contracts/open-api/README.md)
