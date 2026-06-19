<!--
SYNC IMPACT REPORT
Version change: (template) → 1.0.0
Bump rationale: Initial ratification — first concrete constitution derived from the
  41 locked ADRs and design/platform/boundaries.md of 10_core (GtelPay Core).
Principles defined (7):
  I.   Two-Domain Separation (Wallet ∥ Accounting)
  II.  Immutable, Balanced Ledger
  III. Wallet Is the Spendable Hot Path; Accounting Is Back-Office
  IV.  Idempotency End-to-End
  V.   Orchestration Is the Sole Sequencer — Saga, Not 2PC
  VI.  Money & Currency Discipline
  VII. Contracts & Conformance Are the Source of Truth
Added sections: Domain & Security Constraints; Development Workflow & Quality Gates; Governance
Removed sections: none (all template placeholders filled)
Templates reviewed:
  ✅ .specify/templates/plan-template.md — Constitution Check gate references this file generically; no edit needed
  ✅ .specify/templates/spec-template.md — scope/requirements model compatible; no edit needed
  ✅ .specify/templates/tasks-template.md — task categories compatible; no edit needed
Follow-up TODOs: none — RATIFICATION_DATE set to first adoption (2026-06-15).
-->

# GtelPay Core (10_core) Constitution

This constitution governs `10_core` — the design/spec for GtelPay Core. It is derived from and
kept in sync with the locked Architecture Decision Records ([`adr/`](../../adr/README.md)) and
[`design/platform/boundaries.md`](../../design/platform/boundaries.md). Where a principle and an
ADR appear to conflict, the ADR is the authority of record and this document must be amended.

## Core Principles

### I. Two-Domain Separation (Wallet ∥ Accounting)

`core.wallet` ("how much can this member spend now?") and `core.accounting` ("what happened in
the ledger?") are distinct bounded contexts and MUST stay isolated.

- No cross-import, **no cross-schema JOIN, no cross-schema FK** — `wallet_tx.coa_trans_id` is
  correlation only. (ADR-003)
- Domains MUST NOT call each other; all cross-domain sequencing goes through orchestration. (ADR-012, ADR-026)
- Only `core.shared` is shared — no second "common" module holding entities. (ADR-002)
- Aggregate liability lives **only** in accounting (COA 2110/2120/2130); wallet holds per-member
  slices. (ADR-020, ADR-039)

**Rationale:** the two contexts have opposite consistency/latency needs and must scale and fail
independently; coupling them re-creates the monolith the split exists to prevent.

### II. Immutable, Balanced Ledger

The accounting ledger is append-only and always balanced.

- Every journal (`coa_trans`) MUST satisfy `SUM(DR) = SUM(CR)`; posting fails otherwise. (ADR-001)
- POSTED journal lines (`coa_trans_data`) are immutable — corrections are **new reversing
  journals**, never edits. (ADR-001)
- Every transit account (3100–3820) MUST net to **0** when a use case completes. (ADR-010)
- Deposits are two-phase (PENDING → POSTED), wallet credited only after POSTED. (ADR-006, ADR-024)
- The ledger is accrual-like, not cash-basis v1. (ADR-036)

**Rationale:** auditability and regulatory integrity require an unalterable system of record.

### III. Wallet Is the Spendable Hot Path; Accounting Is Back-Office

The wallet serves the customer hot path; accounting is the back-office system of record.

- `wallet_balance` is the authoritative spendable snapshot, read as a single row — **never**
  re-derived per read on the hot path. (ADR-004)
- Every balance change writes exactly **one** `wallet_tx` in the **same** DB transaction (W2);
  `wallet_tx` is append-only. (ADR-004)
- Mutations lock **only** the one `wallet_id` row; there is **no synchronous aggregate row** —
  in-domain aggregates are async rollups. (ADR-039)
- Wallet MUST NOT post or reverse the ledger; corrections are compensating wallet movements. (ADR-026)
- Member identity is separate from balance — `wallet_balance` holds numbers only, keyed by
  `wallet_id`. (ADR-011, data-model)
- Async outflow (withdraw/IBFT) uses freeze → settle/release; **timeout ≠ release** (poll until
  terminal bank outcome). (ADR-007, ADR-033)

**Rationale:** read-your-write spendable balance at high RPS demands a snapshot model opposite to
the derive-authoritative ledger.

### IV. Idempotency End-to-End

Every mutation is replay-safe.

- `business_ref` flows end-to-end and equals `X-Idempotency-Key`; accounting `reference_id`
  carries the same value. (ADR-005)
- Wallet idempotency is the triple `(wallet_id, business_ref, tx_type)`, UNIQUE, re-checked
  **under** the row lock; conflicting amount on the same key → 409. (ADR-004, ADR-005)
- Accounting journal idempotency is UNIQUE `(reference_id, use_case)`. (ADR-001)
- Batch legs use per-recipient sub-keys; replay never double-applies. (ADR-017)

**Rationale:** at-least-once messaging and retries are assumed; correctness cannot depend on
exactly-once delivery.

### V. Orchestration Is the Sole Sequencer — Saga, Not 2PC

Cross-domain use cases are sequenced by orchestration, never by a distributed transaction.

- The orchestrator is a **separate service**; domains are reached over a gateway seam (HTTP S2 /
  async S6), never by one domain calling another. In-process gateways are migration debt, not the
  design. (ADR-038, ADR-012)
- Consistency is achieved by **saga + compensation, no 2PC**; compensation is preferred over blind
  rollback (a POSTED ledger is undone only by a reversing journal). (ADR-008)
- Accounting is fed via **outbox + RabbitMQ, at-least-once**, idempotent on `reference_id`;
  RabbitMQ workers, not Temporal, in v1. (ADR-013, ADR-035, ADR-041)
- Orchestration owns fees, auth, and idempotency-key propagation; it holds no domain logic. (ADR-009)
- Orchestration resolves member + pocket → a concrete `wallet_id` before any wallet leg. (ADR-040)

**Rationale:** the domain split makes a neutral, non-coupling sequencer mandatory.

### VI. Money & Currency Discipline

Money handling is uniform and exact.

- VND single-currency in v1. (ADR-019)
- Money is `BigDecimal` scale 4, `HALF_UP` at the domain boundary; money on the wire is a decimal
  string. (ADR-028)
- Fees are computed once, at orchestration; domains receive final amounts and never re-compute. (ADR-009)

**Rationale:** silent rounding or duplicated fee math is a class of financial bug we refuse to risk.

### VII. Contracts & Conformance Are the Source of Truth

Wire contracts and acceptance criteria bind the implementation.

- OpenAPI / AsyncAPI YAML is the wire source of truth; code maps to it. (ADR-018)
- Every ADR carries **AC** (Acceptance Criteria) + **TC** (Test Cases) traced into
  [`design-v2/acceptance.md`](../../design-v2/acceptance.md).
- SQL ledger invariants run in CI and nightly; a failed invariant **fails the build** — no silent
  drift. (ADR-031)
- Reconciliation (W5, wallet sum vs COA control) is **report-only**; it never auto-adjusts. (ADR-014)

**Rationale:** a contract-first, invariant-gated pipeline keeps the two domains honest over time.

## Domain & Security Constraints

- **Storage:** one PostgreSQL instance, schemas `wallet` and `accounting`; split to two DBs later =
  datasource URL change only. (ADR-003)
- **Deposit mapping:** virtual account → `memberId`; unmapped deposits stay PENDING. (ADR-030)
- **LOCKED / CLOSED wallets** reject debit/freeze (and deposit credit per W-O1). (ADR-029, ADR-034)
- **USER multi-pocket:** a USER may hold multiple pockets (label + `wallet_pocket_def`);
  MERCHANT/PARTNER are single. All USER pockets roll up to control 2110. (ADR-040)
- **EOD settlement / period close** run as independent batch jobs, never inline with payment.
  (ADR-015, ADR-023)
- **Bank webhooks** are mutually authenticated (mTLS). (ADR-022)
- **Auth:** JWT `sub` = `memberId`. (ADR-011)
- **Ledger backing store:** `core.accounting` may wrap TigerBeetle behind the accounting service
  (hybrid with PostgreSQL) — a decision behind the gateway seam, invisible to callers. (ADR-037)

## Development Workflow & Quality Gates

- **A locked decision is an ADR.** New or changed governance/behavior MUST be captured as an ADR
  (`ADR-NNN-short-title.md`) with AC/TC, the `adr/README.md` index updated, and the source doc
  linked. Amending a principle here requires the corresponding ADR.
- **Spec flow:** use the spec-kit workflow — `/speckit-specify` → `/speckit-plan` →
  `/speckit-tasks` → `/speckit-implement`; `/speckit-clarify` and `/speckit-analyze` to de-risk.
  Each plan MUST pass a Constitution Check against the principles above.
- **Contracts first:** changes to behavior update OpenAPI/AsyncAPI before/with code. (ADR-018)
- **Conformance gate:** release gate v1 = 150 acceptance scenarios; SQL invariant CI green is
  mandatory to merge. (ADR-031)
- **Boundaries are enforced:** no cross-schema JOIN/FK, no domain→domain call — reviewers reject
  violations.

## Governance

- This constitution **supersedes** ad-hoc practice. Where it and an ADR diverge, the ADR is
  authoritative and this document MUST be amended to match.
- **Amendment procedure:** open/modify an ADR → if it adds/removes/redefines a principle or
  constraint here, update this file and bump the version → propagate to `.specify/templates/*`.
- **Versioning policy (semantic):** MAJOR = a principle removed or redefined incompatibly; MINOR =
  a principle/section added or materially expanded; PATCH = clarifications/wording.
- **Compliance review:** every PR/review verifies alignment with these principles and the cited
  ADRs; complexity that appears to break a principle MUST be justified or rejected. The SQL
  invariant suite (ADR-031) is the automated backstop.

**Version**: 1.0.0 | **Ratified**: 2026-06-15 | **Last Amended**: 2026-06-15
