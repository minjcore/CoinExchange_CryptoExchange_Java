# Implementation Plan: Async Bank Deposit (P5)

**Branch**: `002-async-deposit` | **Date**: 2026-06-18 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/002-async-deposit/spec.md`

## Summary

A USER member deposits funds via NAPAS 247 or virtual account. Orchestration acknowledges with 202 immediately, then publishes a `BANK_DEPOSIT` command via outbox→RabbitMQ. An accounting worker executes two-phase posting: Phase A creates a PENDING journal (1111 DR / 3100 CR); Phase B (`confirmDeposit`) clears transit 3100, books liability 2110 and fee 4110, and emits a `JournalPosted` event. Wallet is credited (net amount, `DEPOSIT_CREDIT`) only after POSTED. The full flow is idempotent on `businessRef`.

**TigerBeetle replaces `coa_trans_data` as the hot-posting store** inside `core.accounting` per ADR-037. `core.wallet` PoC evaluates Blnk patterns for balance monitoring and historical snapshots.

## Technical Context

**Language/Version**: Java 17; Spring Boot 3.3.x on `app-*` modules; `core.*` = pure domain JARs

**Primary Dependencies**:
- `core.accounting` adapter: TigerBeetle Java client (hot postings) + PostgreSQL (COA master, period control, read-model)
- `core.wallet`: PostgreSQL snapshot + optional Blnk PoC (balance monitor, historical balance API patterns)
- Messaging: RabbitMQ (BANK_DEPOSIT command); Kafka optional for `JournalPosted` event
- Outbox: transactional outbox in PostgreSQL `accounting` schema (ADR-013)

**Storage**:
- PostgreSQL `wallet` schema: `wallet_balance`, `wallet_tx`, `wallet_pocket_def`
- PostgreSQL `accounting` schema: `coa_account`, `coa_trans` (journal header + state), period tables — **not** `coa_trans_data` for posting lines in v2
- TigerBeetle cluster: hot postings (transfers + account balances), source of truth for DR/CR

**Testing**: Gherkin acceptance scenarios in `design-v2/acceptance.md`; SQL invariant CI (ADR-031); TigerBeetle adapter unit tests against TB mock/sandbox

**Target Platform**: Linux containers; two deployable pods — `app-accounting` + `app-orchestration`

**Performance Goals**: 202 ack to notifier in < 200 ms; wallet balance reflects net deposit within 5 s of POSTED (SC-001/SC-002); TB hot-path throughput matches platform goals (millions of TPS headroom — deposit volume is orders of magnitude lower)

**Constraints**: Two-domain boundary (no cross-schema JOIN/FK); VND single currency; fee computed by orchestration before publish; LOCKED/CLOSED wallet → ops resolution, not auto-credit

**Scale/Scope**: USER lane only; single virtual account → single member; v1 does not spec partial commit (Blnk concept noted for future ADR)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Compliance | Evidence |
|-----------|-----------|----------|
| I. Two-domain separation | ✅ | Wallet and accounting updated via separate domain calls; outbox+RabbitMQ is the seam; orchestration never writes to `coa_*` or `wallet_*` directly (ADR-012) |
| II. Immutable, balanced ledger | ✅ | Phase A pending + Phase B post via `confirmDeposit` only; transit 3100 net zero enforced before POSTED; corrections are new reversing journals (ADR-006, ADR-010) |
| III. Wallet hot path; no sync aggregate | ✅ | `wallet_balance.available` unchanged until POSTED; single `wallet_tx` insert per credit (ADR-004, ADR-039) |
| IV. Idempotency end-to-end | ✅ | `businessRef` = `X-Idempotency-Key` end-to-end; wallet triple `(wallet_id, businessRef, DEPOSIT_CREDIT)` UNIQUE; TB Transfer `id` = hash(businessRef) (ADR-005) |
| V. Orchestration sole sequencer; saga not 2PC | ✅ | Orchestration publishes BANK_DEPOSIT via outbox; accounting worker executes both phases; wallet credit is a separate downstream step; no 2PC (ADR-008, ADR-041) |
| VI. Money & currency discipline | ✅ | VND only; orchestration computes fee once; net amount = gross − fee passed to wallet; TB stores u128 minor units (×10⁴) (ADR-009, ADR-028) |
| VII. Contracts & conformance | ✅ | `accounting-internal.yaml` wire unchanged (ADR-037 AC-037-02); new Gherkin for deposit flow traces AC-006/AC-024/AC-041; SQL invariant INV-03 covers 3100=0 |

**TigerBeetle integration check (ADR-037)**: TB is an **implementation detail** of `core.accounting` behind the internal port. Orchestration sees no change. ADR-003 partially revised: wallet + accounting metadata stay Postgres; hot postings move to TB. No cross-domain concern introduced.

**Result: PASS — no constitution violations.**

**Post-Design re-check (after Phase 1): PASS.** TigerBeetle hybrid architecture, Blnk PoC, and dual wallet-credit paths (ADR-024) introduce no new violations. All domain boundaries preserved.

## Project Structure

### Documentation (this feature)

```text
specs/002-async-deposit/
├── plan.md              # This file
├── research.md          # Phase 0 — TigerBeetle, Blnk, dual credit paths
├── data-model.md        # Phase 1 — TB account/transfer mapping, wallet_tx, outbox
├── quickstart.md        # Phase 1 — validation guide (phase A/B, idempotency, error paths)
├── contracts/           # Phase 1 — BANK_DEPOSIT command + JournalPosted event schema refs
└── tasks.md             # Phase 2 — generated by /speckit-tasks
```

### Artifacts in the main repo

```text
adr/ADR-037-tigerbeetle-ledger-backing-store.md   # TB authority
adr/ADR-006-two-phase-deposit.md                  # two-phase protocol
adr/ADR-041-rabbitmq-orch-to-accounting-worker.md # RabbitMQ BANK_DEPOSIT path
adr/ADR-024-deposit-wallet-credit-dual-path.md    # wallet credit paths (A/B/C)
spec/contracts/async-api/core-commands.yaml       # BANK_DEPOSIT envelope (update)
spec/contracts/async-api/core-events.yaml         # JournalPosted, WalletCredited
spec/processes.md        §13                      # deposit process narrative
design-v2/acceptance.md                           # deposit Gherkin (to extend)
sandbox/tigerbeetle/                              # spike code + stack.sh
references/blnk-vs-gtelpay-comparison.md          # Blnk comparison reference
```

**Structure Decision**: Design/spec repo — artifacts above express the deposit flow. `core.accounting` and `app-orchestration` are downstream Java implementations.
