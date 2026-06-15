# ADR-003: Dual schema in one PostgreSQL instance for wallet and accounting

| Field | Value |
|-------|-------|
| Status | Accepted |
| Date | 2026-06-08 |
| Deciders | Engineering |
| Related | [`IMPLEMENTATION.md`](../IMPLEMENTATION.md) §1.4, [`core.wallet.md`](../core.wallet.md) §9, [`core.accounting.trd.md`](../core.accounting.trd.md) §8, [ADR-002](ADR-002-core-foundation-shared-library.md) |

---

## Context

`core.wallet` (`wallet_*`) and `core.accounting` (`coa_*`) are separate bounded contexts ([ADR-002](ADR-002-core-foundation-shared-library.md)) but, in v1, deploy from one repo and one Spring Boot `app-orchestration` process ([`IMPLEMENTATION.md`](../IMPLEMENTATION.md) §1.2).

We need a storage layout that:

- Keeps the two domains physically isolated (no shared tables, no cross-domain JOIN) so the boundary in ADR-002 holds at the DB level, not just in code.
- Avoids premature operational cost of running two database servers for a v1 with modest volume.
- Lets us split into two databases later **without** changing entity mappings or domain code.

Options:

1. **One database, one schema (`public`)** — both `wallet_*` and `coa_*` together.
2. **One database, two schemas (`wallet`, `accounting`)** — isolation by schema.
3. **Two databases from day one** — strongest isolation, highest ops cost now.

---

## Decision

Use **one PostgreSQL 15+ instance with two schemas, `wallet` and `accounting`** ([`IMPLEMENTATION.md`](../IMPLEMENTATION.md) §1.4).

1. **Schema per domain** — `wallet_*` tables live in schema `wallet`; `coa_*` tables live in schema `accounting`. JPA entities pin the schema: `@Table(schema = "wallet")` / `@Table(schema = "accounting")`.
2. **No cross-schema coupling** — no JOIN and no foreign key across schemas. `wallet_tx.coa_trans_id` is a **correlation value only**, never a DB-enforced FK to `accounting.coa_trans` ([`core.wallet.md`](../core.wallet.md) §3.4, [`integration-surfaces.md`](../integration-surfaces.md) §8, F-rules F2/F3).
3. **Separate migrations** — each domain owns its Flyway history: `V*__*_wallet.sql` against schema `wallet`, `V*__*_accounting.sql` against schema `accounting`. Flyway runs per datasource (`spring.flyway.schemas`).
4. **Two `EntityManagerFactory` beans** — `app-orchestration` wires one EMF per schema for clarity, even though both point at the same JDBC URL in v1.
5. **Least-privilege DB users** — distinct roles (`wallet`, `accounting`) own their schema; neither is granted on the other's schema, enforcing isolation at the engine.
6. **Future split is a config change** — moving a schema to its own database server changes only the datasource URL/credentials; schemas, table names, and entity mappings stay identical.

---

## Consequences

### Positive

- DB-level enforcement of the ADR-002 boundary — a stray cross-domain JOIN fails (different schema, no grant) instead of silently coupling the contexts.
- One instance to back up, monitor, and connect to in v1 — lower operational overhead.
- Clean migration path to two databases with no domain-code change.
- Matches the "no shared tables / no shared repositories" rule already stated across the wallet and accounting docs.

### Negative / trade-offs

- A single instance is a shared blast radius: a wallet-side lock storm or vacuum stall can affect accounting until the split happens. Monitor per-schema (see [`operations.md`](../operations.md)).
- Two EMFs add Spring config complexity versus a single datasource with `currentSchema`.
- Cross-domain consistency that *looks* available via one transaction is **deliberately forbidden** — orchestration must still treat wallet and accounting as separate commits ([`IMPLEMENTATION.md`](../IMPLEMENTATION.md) §8).

### Implementation notes

| Concern | Approach |
|---------|----------|
| Connection pooling | One pool per datasource bean; size independently per domain load |
| Backup / PITR | Instance-level PITR in v1 ([`core.accounting.trd.md`](../core.accounting.trd.md) §11); per-database after split |
| Search path | Do not rely on `search_path`; always qualify via `@Table(schema=...)` |
| Test isolation | Testcontainers spins one Postgres; run both Flyway migrators against the two schemas |

---

## Alternatives considered

| Alternative | Why rejected |
|-------------|--------------|
| Single `public` schema | No physical boundary; invites cross-domain JOIN and FK, contradicts ADR-002 |
| Two databases now | Higher ops cost for v1 volume; no functional benefit until scale demands it |
| One datasource + `currentSchema` per request | Harder to reason about than two explicit EMFs; risk of leaking the wrong schema |

---

## Relationship to other ADRs

| ADR | Topic |
|-----|--------|
| [ADR-001](ADR-001-immutable-ledger.md) | Accounting ledger model inside schema `accounting` |
| [ADR-002](ADR-002-core-foundation-shared-library.md) | **Code** boundary between domains — this ADR is the **storage** boundary |
| [ADR-005](ADR-005-idempotency-key-strategy.md) | Idempotency keys that make separate-commit orchestration safe |

---

## Acceptance criteria (AC-003)

| ID | Criterion | Verified at |
|----|-----------|-------------|
| AC-003-01 | All `wallet_*` tables in PostgreSQL schema `wallet` | DDL / `@Table(schema="wallet")` |
| AC-003-02 | All `coa_*` tables in PostgreSQL schema `accounting` | DDL / `@Table(schema="accounting")` |
| AC-003-03 | No SQL JOIN across `wallet` and `accounting` schemas in application or domain code | Static analysis / review |
| AC-003-04 | No FK from `wallet.wallet_tx.coa_trans_id` to `accounting.coa_trans` | DDL |
| AC-003-05 | Separate Flyway histories per schema | `V*__*_wallet.sql` vs `V*__*_accounting.sql` |
| AC-003-06 | DB roles `wallet` / `accounting` cannot SELECT/WRITE peer schema | GRANT audit |
| AC-003-07 | Wallet and accounting mutations commit in **separate** local transactions | Integration / saga tests |

---

## Test cases (TC-003)

| ID | Title | Procedure (summary) | Expected | Maps to |
|----|-------|---------------------|----------|---------|
| TC-003-01 | Schema qualification | `\d wallet.wallet_balance` and `\d accounting.coa_trans` | Tables in correct schema | AC-003-01, AC-003-02 |
| TC-003-02 | Cross-schema JOIN fails | Run ad-hoc JOIN wallet + accounting as app user | Permission denied or not in codebase | AC-003-03 |
| TC-003-03 | coa_trans_id no FK | Inspect `wallet_tx` DDL | `coa_trans_id` nullable BIGINT, no FK | AC-003-04 |
| TC-003-04 | Flyway separation | Run migrations fresh | Both schemas populated; independent version tables | AC-003-05 |
| TC-003-05 | Role isolation | Connect as `wallet` role; query `accounting.coa_trans` | Denied | AC-003-06 |
| TC-003-06 | Payment partial failure | Debit wallet TX commits; accounting post fails | Wallet debited; compensating saga; no single cross-schema TX | `acceptance.md` Payment compensate |
| TC-003-07 | Reconciliation without JOIN | W5 job uses two queries + compare | Drift report without cross-schema SQL | `acceptance.md` W5 scenarios |

---

## References

- [`IMPLEMENTATION.md`](../IMPLEMENTATION.md) — §1.4 Database v1, §3–4 DDL, §11 datasource config
- [`integration-surfaces.md`](../integration-surfaces.md) — §10 Forbidden (F2, F3, F5)
- [`core.wallet.md`](../core.wallet.md) — §9 Concurrency and storage
- [`core.accounting.trd.md`](../core.accounting.trd.md) — §8 Storage Requirements
- [`design-v2/wallet.md`](../design-v2/wallet.md) — §8 W4, §11 reconciliation
- [`design-v2/orchestration.md`](../design-v2/orchestration.md) — §9 F2, §18.1 saga record
