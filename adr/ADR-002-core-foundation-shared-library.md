# ADR-002: `core.foundation` as the only shared library

| Field | Value |
|-------|-------|
| Status | Accepted |
| Date | 2026-06-03 |
| Deciders | Engineering |
| Related | [`core.foundation.md`](../core.foundation.md), [ADR-001](ADR-001-immutable-ledger.md) |

---

## Context

The platform splits into:

- **Application** — HTTP, security, Kafka consumers, bank webhooks
- **core.wallet** — `wallet_*` balances and movements
- **core.accounting** — `coa_*` journals and COA

All three need the same API shape (envelope, paging, errors, ids/time). Without a rule, shared concerns drift into a “common” module with domain entities, cross-domain coupling, or a second shared layer between wallet and accounting.

---

## Decision

**`core.foundation` is the single shared design layer** at the bottom of the `core` stack (documented in [`core.foundation.md`](../core.foundation.md); technology choice is out of scope for this ADR).

1. **Only foundation is shared** among Application, `core.wallet`, and `core.accounting`.
2. **Foundation contains (design)** — shared API envelope, paging, error conventions, util rules; alignment with [`integration-surfaces.md`](../integration-surfaces.md), `spec/contracts/open-api/`, `spec/contracts/async-api/`.
3. **Foundation must not contain** — domain entities for `wallet_*` / `coa_*`, persistence mapping, domain services, HTTP/Kafka bindings, wallet or accounting business rules.
4. **Domain cores must not depend on each other** — no `core.wallet` ↔ `core.accounting` direct coupling; sync via Application event/API ([`core.foundation.md` §3](../core.foundation.md)).
5. **New shared material** — add to foundation only if used by **≥ 2** consumers and passes checklist in [`core.foundation.md` §10](../core.foundation.md); otherwise keep in domain module or Application.

**Dependency direction (design):**

```
Application  →  core.wallet | core.accounting  →  core.foundation
```

`core.foundation` must not depend on `core.wallet` or `core.accounting`.

---

## Consequences

### Positive

- One place for API envelope and error codes — consistent client experience.
- Wallet and accounting stay isolated; no “god” common module.
- Foundation stays small and validation-friendly (shared shapes only).
- Clear onboarding: “shared = foundation only.”

### Negative / trade-offs

- Some duplication at Application boundary (HTTP mapping) is intentional — not duplicated into foundation.
- Cross-cutting concerns (logging MDC, tracing) may live in Application starter, not foundation, unless promoted via ADR.

### What this ADR does **not** decide

- Packaging and language — implementation detail, decided when coding.
- Whether TRD REST paths live in Application only — yes, per foundation §4.

---

## Alternatives considered

| Alternative | Why rejected |
|-------------|--------------|
| Single `core-common` with entities + DAOs | Blurs wallet/accounting boundaries; encourages JOINs and shared tables |
| `core.wallet` imports `core.accounting` (or reverse) | Violates bounded context; blocks independent deploy |
| Duplicate envelope/error in each module | Inconsistent APIs and error codes |
| Put everything in Application only | Domain services cannot reuse paging/errors without pulling HTTP stack |

---

## Relationship to other ADRs

| ADR | Topic |
|-----|--------|
| [ADR-001](ADR-001-immutable-ledger.md) | Accounting data model (`coa_*`, immutable journal) — lives in **core.accounting**, not foundation |
| ADR-002 (this) | **What** may be shared across modules — **only** foundation |

---

## Acceptance criteria (AC-002)

| ID | Criterion | Verified at |
|----|-----------|-------------|
| AC-002-01 | Only `core.foundation` is imported by both `core.wallet` and `core.accounting` for shared types | Build / module graph |
| AC-002-02 | `core.wallet` does **not** import `core.accounting` (and vice versa) | Build / static analysis |
| AC-002-03 | Foundation contains no `wallet_*` / `coa_*` entities, repositories, or domain services | Code review / package scan |
| AC-002-04 | HTTP routes and Kafka listeners live in Application/orchestration, not domain modules | Module layout |
| AC-002-05 | Cross-domain sync uses orchestration API/events only — no shared repository | Runtime architecture |
| AC-002-06 | New shared code requires ≥2 consumers and foundation §10 checklist | ADR process |

---

## Test cases (TC-002)

| ID | Title | Procedure (summary) | Expected | Maps to |
|----|-------|---------------------|----------|---------|
| TC-002-01 | Module dependency graph | Inspect `core.wallet` / `core.accounting` POM or imports | No cross-domain import | AC-002-01, AC-002-02 |
| TC-002-02 | Foundation package scan | List `core.foundation` packages | No `Wallet`, `CoaTrans` entities | AC-002-03 |
| TC-002-03 | Gateway forbidden SQL | Gateway config / integration test | No direct SQL on `wallet_*` / `coa_*` | `orchestration.md` §9 F1; `integration-surfaces.md` §9 |
| TC-002-04 | Accounting consumer isolation | `JournalPosted` consumer in wallet module absent | Wallet credited via orchestration/command only | AC-002-05 |
| TC-002-05 | Envelope shape consistency | S1 error + paging responses | Same envelope from foundation types | `core.foundation.md` Part I |

---

## References

- [`core.foundation.md`](../core.foundation.md) — Part I (foundation design §1–11)
- [`TERMINOLOGY.md`](../TERMINOLOGY.md) — `coa_*` vs `wallet_*` prefixes
- [`design-v2/orchestration.md`](../design-v2/orchestration.md) — §9 Forbidden (F1–F6)
- [`integration-surfaces.md`](../integration-surfaces.md) — §9 forbidden rules
