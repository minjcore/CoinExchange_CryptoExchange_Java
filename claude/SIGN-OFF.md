# GtelPay Core — Design Sign-Off

| Field | Value |
|-------|-------|
| Document | Design & verified-core sign-off |
| Component | `core.accounting` + `core.wallet` (+ `core.sharedlib`, `app-orchestration`) |
| Domain | Fiat payments (VND) |
| Status | **Draft — awaiting approval** |
| Prepared for | Architecture / Engineering / Finance / Product approval |

> **Purpose.** This document records approval that the GtelPay Core **design** and its **implemented core** are sound, scoped, and verified — and may proceed to the next phase. Section 6 lists open items so that approval is **informed**, not blind.

---

## 1. Scope being signed off

**In scope**
- `core.accounting` — immutable double-entry ledger (chart of accounts, journals, transit, EOD).
- `core.wallet` — per-member spendable balance (available / frozen), append-only movements.
- `core.sharedlib` — shared lib (envelope, errors, money utils).
- `app-orchestration` — saga sequencer across the two domains.
- Currency: **VND (fiat) only.**

**Explicitly out of scope** (not part of this sign-off)
- Crypto / hot-cold custody / C2C-OTC escrow / multi-currency.
- The reference codebase (`00_framework`, CoinExchange/BIZZAN) — technical reference only.

---

## 2. What is delivered

| Area | Delivered |
|------|-----------|
| Design decisions | **41 ADRs**, each with acceptance criteria (AC) + test cases (TC) |
| Behaviour | Process flows (`spec/processes.md`), foundation fund flows |
| Contracts | OpenAPI + AsyncAPI (`spec/contracts/`) as the wire source of truth |
| Conformance | **150+ acceptance scenarios** (Given/When/Then) |
| Implementation | `core.sharedlib` · `core.wallet` · `core.accounting` · `app-orchestration` (Java / Spring Boot 3) — building |
| Automated tests | **38 tests passing** (`mvn test`, BUILD SUCCESS) |

---

## 3. Correctness evidence (the safeguards are real and tested)

| Invariant | Guarantee | Verified by |
|-----------|-----------|-------------|
| Idempotency (under lock) | A retry never double-applies / double-spends | `concurrentSameTriple_appliesOnceAndReplaysLoser`, `duplicateBusinessRef_*` |
| Double-entry balanced | Every journal: debits = credits, else rejected | `unbalancedLines_rejectsPost` |
| Immutable ledger | A POSTED line cannot be edited | `postedLine_tamperIsIgnored_immutable` |
| Transit net-zero (all use cases) | No money stranded mid-flow at POST | `balancedButStrandedTransit_rejectsPost`, `payment...transit3500Zero` |
| Period close | No posting into a CLOSED/LOCKED period | `closedPeriod_rejectsPost` |
| Outflow holds funds | Settle debits **frozen**, not available (no double-spend) | `withdrawFreezeThenSettle`, `ibftFreezeThenSettle_deductsFrozenNotAvailable` |
| Wallet snapshot + log together | No balance change without a movement row, same TX | `creditThenDebit_balanceMatches`, balance integration tests |

---

## 4. Recently closed gaps (A1–A4)

| # | Gap | Fix | Commit |
|---|-----|-----|--------|
| A1 | Transit net-zero enforced only for deposit | Enforced for **every** use case at post time | `a362798` |
| A2 | IBFT settle could debit available → double-spend | `IBFT_SETTLE` deducts from frozen | `a362798` |
| A3 | Ledger immutability was convention-only | `@Immutable` hard ORM guard | `66fd72e` |
| A4 | Period close (ADR-023) not implemented | `coa_period` + post-time period guard | `66fd72e` |
| — | Proof tests for A1–A4 | red/green tests added | `ca06314`, `c28ce9e` |

---

## 5. Quality gates

- `mvn test` → **BUILD SUCCESS, 38 tests pass** (foundation 9 · wallet 16 · accounting 13).
- SQL ledger-invariant suite gates CI (ADR-031): balanced journals, transit net-zero.
- Contracts (OpenAPI/AsyncAPI) validated; pocket/error schemas resolve.

---

## 6. Open items & known limitations (approval is informed)

**Sign-off does not claim these are done.** They are accepted as known and deferred/tracked:

1. **Multi-pocket (ADR-040)** is designed, spec'd, and contract-drafted, but **not yet implemented** in `core.wallet` code.
2. **Period management is minimal** — a period can be marked CLOSED/LOCKED, but there is no automated period-end / close workflow yet.
3. **Operational surface** — RabbitMQ workers, the outbox relay, and reconciliation jobs require ops capacity to run and monitor.
4. **Eventual consistency** between wallet and accounting — a lag window exists; W5 reconciliation is **report-only** (drift requires manual follow-up, never auto-corrected).
5. **Not load-tested** — NFR figures (RPS, P95) are **targets**, not measured results.
6. **Documentation spans** `design/`, `design-v2/`, `spec/`, `specs/` — consolidation is pending.
7. **Wallet tx-type catalog** in code is a subset of the design catalog (PARTNER/PAYROLL/MERCHANT_SETTLE types not yet added).

---

## 7. Sign-off

By signing, the approver confirms they have reviewed Sections 1–6 and approve the design and verified core to proceed, subject to any conditions noted.

| Role | Name | Decision (Approve / Approve-with-conditions / Reject) | Conditions / Notes | Date | Signature |
|------|------|-------------------------------------------------------|--------------------|------|-----------|
| Architecture |  |  |  |  |  |
| Engineering Lead |  |  |  |  |  |
| Accounting / Finance owner |  |  |  |  |  |
| Product owner |  |  |  |  |  |
| Compliance / Audit *(if applicable)* |  |  |  |  |  |

**Overall decision:** ☐ Approved  ☐ Approved with conditions  ☐ Rejected

---

## 8. References (backing)

`adr/` (41 ADRs + AC/TC) · `spec/` (foundation, processes, contracts) · `design-v2/acceptance.md` (150+ scenarios) · `platform/` (running modules + tests) · `DESIGN-BRIEF.md` / `DESIGN-DECK.md` (summary + slides).
