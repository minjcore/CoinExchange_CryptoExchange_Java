---
description: "Task list — Async Bank Deposit (002)"
---

# Tasks: Async Bank Deposit

**Input**: Design documents from `specs/002-async-deposit/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, data-flow.md, contracts/, quickstart.md

**Tests**: Design/spec repo — "tests" = **Gherkin acceptance scenarios** in
`design-v2/acceptance.md` (conformance), gated by SQL invariant CI (ADR-031).

**Organization**: by user story (spec.md US1–US3).

## Format: `[ID] [P?] [Story] Description`

- **[P]**: parallelizable (different file, no incomplete dependency)
- File paths are repo-relative from `10_core/`.

---

## ⚡ Service Entry Point Map (read before implementing)

This table is the authoritative reference for which protocol is used to enter each domain
service in the async deposit flow. Every task that touches a service boundary must match this map.

| Who sends | Protocol | Module that receives | Artifact / address | Domain logic called |
|-----------|----------|---------------------|-------------------|---------------------|
| Bank / NAPAS gateway | HTTP `POST /deposits/notify` | `app-orchestration` | `orchestration-public.yaml` | VA lookup + fee compute + outbox write |
| Outbox relay | **RabbitMQ** (at-least-once) | `app-accounting-worker` | exchange `core.commands` / queue `core.commands.bank-deposit` / msg `BANK_DEPOSIT` | `core.accounting` Phase A + Phase B |
| `app-accounting-worker` (after POSTED) | **RabbitMQ** | `app-wallet-worker` | exchange `core.commands` / queue `core.commands.wallet-credit` / msg `WALLET_CREDIT` | `core.wallet` DEPOSIT_CREDIT |
| `app-accounting-worker` (optional) | Kafka publish | Downstream consumers | topic `core.accounting.journal-posted` / event `JournalPosted` | Reporting, audit, reconciliation |
| `app-wallet-worker` (optional) | Kafka publish | Downstream consumers | topic `core.wallet.wallet-credited` / event `WalletCredited` | Notification service |

> **Key rules derived from this map:**
>
> 1. `app-orchestration` → accounting: **NO HTTP call to `app-accounting`** in the deposit flow.
>    Orchestration only writes to the outbox; the worker takes it from there.
> 2. `app-accounting-worker` → wallet: **NO HTTP call to `app-wallet`**.
>    The worker publishes `WALLET_CREDIT` to RabbitMQ; the wallet worker consumes it.
> 3. HTTP gateways (`app-accounting`, `app-wallet`) are used for **synchronous use cases only**
>    (e.g., freeze in withdrawal, balance query). They are **not in the deposit data path**.
> 4. Both Phase A and Phase B of accounting are handled inside one `app-accounting-worker`
>    consumer. Phase A calls `core.accounting.createJournal()`; Phase B calls
>    `core.accounting.confirmDeposit()` — both are in-process Java calls, not HTTP.

---

## Path Conventions

Artifacts (this repo): `adr/`, `spec/implementation.md`, `spec/contracts/async-api/`,
`spec/contracts/open-api/`, `spec/processes.md`, `design-v2/orchestration.md`,
`design-v2/acceptance.md`, `specs/002-async-deposit/`.

---

## Phase 1: Setup (Shared)

**Purpose**: confirm design artifacts are in place and the data-flow is drawn.

- [x] T001 Verify spec-kit feature artifacts present in `specs/002-async-deposit/` (spec, plan, research, data-model, data-flow, contracts, quickstart)
- [x] T002 Draw data-flow diagram in `specs/002-async-deposit/data-flow.md` (end-to-end flow: bank notify → orchestration → RabbitMQ → accounting worker → Phase A/B → wallet credit; error paths; state machine; per-phase store table)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: service entry point map, DDL, queue schemas, HTTP schemas, and error vocabulary that ALL
stories depend on.

**⚠️ MUST complete before any user story.**
**Entry-point context**: every contract task below is tagged with the protocol it governs — queue or HTTP.

- [x] T003 Author "Service Entry Point Map" in `specs/002-async-deposit/data-model.md §0` (or
      `data-flow.md §0`) — reproduce the table above as a section header so it is the first thing
      a developer reads; cross-reference `spec/contracts/async-api/core-commands.yaml` (queue
      contracts) and `spec/contracts/open-api/accounting-internal.yaml` (HTTP contracts)
- [x] T004 [P] **[QUEUE entry — accounting]** Finalize `BANK_DEPOSIT` command schema in
      `spec/contracts/async-api/core-commands.yaml` — this is the **RabbitMQ entry point** for
      `app-accounting-worker`; required fields: `commandType`, `businessRef`, `memberId`,
      `virtualAccount`, `grossAmount`, `fee`, `currency`, `bankRef`
- [x] T005 [P] **[QUEUE entry — wallet]** Finalize `WALLET_CREDIT` command schema in
      `spec/contracts/async-api/core-commands.yaml` — this is the **RabbitMQ entry point** for
      `app-wallet-worker`; required fields: `commandType`, `businessRef`, `walletId`, `netAmount`,
      `coaTransId`, `currency`
- [x] T006 [P] **[QUEUE outbound — accounting]** Verify `JournalPosted` event schema in
      `spec/contracts/async-api/core-events.yaml` — published by `app-accounting-worker` to Kafka
      after POSTED; must include: `eventType`, `useCase: DEPOSIT`, `businessRef`, `coaTransId`,
      `status: POSTED`
- [x] T007 [P] **[QUEUE outbound — wallet]** Verify `WalletCredited` event schema in
      `spec/contracts/async-api/core-events.yaml` — published by `app-wallet-worker` to Kafka after
      credit; must include: `eventType`, `businessRef`, `walletId`, `netAmount`, `availableAfter`
- [x] T008 [P] **[QUEUE outbound — ops]** Verify `core.operations.command-failed` event in
      `spec/contracts/async-api/core-events.yaml` — published by any worker on DLQ; fields:
      `businessRef`, `commandType`, `reason`, `failedAt` *(schema uses `originCommandType` +
      `errorCode` + `message` — superset of required fields; verified OK)*
- [x] T009 **[HTTP inbound — orchestration]** Verify deposit notify endpoint in
      `spec/contracts/open-api/orchestration-public.yaml` — `POST /deposits/notify` → 202 + `businessRef`;
      this is the **only HTTP call** from outside in the deposit flow; orchestration turns this into
      the RabbitMQ queue entry (T004) via outbox *(file is `gtelpay-public.yaml`, not
      `orchestration-public.yaml`; endpoint verified at line 289 → 202 + `mTLSPartner` security)*
- [x] T010 Finalize outbox table DDL in `spec/implementation.md` — add `outbox` table to
      `accounting` schema (columns: id, command_type, business_ref, payload JSONB, status,
      created_at, published_at) per data-model.md §5; this table bridges the HTTP inbound (T009)
      to the queue outbound (T004)
- [x] T011 Add note in `specs/002-async-deposit/data-model.md §0` (entry-point map) explicitly
      stating that `app-accounting` (HTTP gateway) and `app-wallet` (HTTP gateway) are **NOT** in
      the deposit data path — they are for synchronous use cases (freeze/release, balance queries)
      only; link to architecture-overview for the full module table

**Checkpoint**: Entry-point map documented. Queue schemas ready. HTTP inbound verified. Outbox DDL complete — stories can proceed.

---

## Phase 3: User Story 1 — Deposit Lands in Wallet (Priority: P1) 🎯 MVP

**Goal**: A USER member sends a bank transfer; their wallet balance increases by net amount after
ledger POSTED. Orchestration returns 202 before any ledger write.

**Entry-point chain for this story**:
`HTTP POST /deposits/notify` → `app-orchestration` → outbox → **RabbitMQ `BANK_DEPOSIT`** →
`app-accounting-worker` → [Phase A + Phase B in core.accounting] → **RabbitMQ `WALLET_CREDIT`** →
`app-wallet-worker` → core.wallet DEPOSIT_CREDIT.

**Independent test**: Send notify → get 202 → confirm wallet balance increases by net only after POSTED
(quickstart Q1).

- [x] T012 [US1] Finalize deposit process in `spec/processes.md §13` — two-phase protocol:
      Phase A PENDING (1111 DR / 3100 CR via TigerBeetle pending Transfer), Phase B via
      `confirmDeposit` only (3100=0, 2110+4110 via TB post_pending + transfers), wallet credit
      after POSTED via **RabbitMQ WALLET_CREDIT** to `app-wallet-worker`; 202 async ack returned by
      orchestration before any accounting write
      <!-- done: §3.3 corrected to RabbitMQ path; §13.1 expanded with Phase A/B TigerBeetle detail -->
- [x] T013 [US1] Verify orchestration deposit saga in `design-v2/orchestration.md §11` — VA→memberId
      resolution, outbox publish, 202 return; confirm **no HTTP call from orchestration to
      `app-accounting` or `app-wallet`** in this flow (outbox → queue only); confirm
      `accounting-internal.yaml` wire is NOT used by orchestration for deposit
      <!-- done: §11.2 steps table rewritten, §11.6 Forbidden section updated -->
- [x] T014 [P] [US1] Add Gherkin to `design-v2/acceptance.md` (deposit feature):
      - TC-DEP-01: happy path Phase A PENDING → Phase B POSTED → wallet credit net amount
      - TC-DEP-02: fee handling (wallet receives gross minus fee)
      - TC-DEP-03: 202 returned before journal written (verify outbox write happens in same tx as 202)
      - TC-DEP-04: entry-point chain — `BANK_DEPOSIT` arrives via RabbitMQ queue, not HTTP gateway
- [x] T015 [P] [US1] Verify TigerBeetle mapping documented in `specs/002-async-deposit/data-model.md §1.2–1.3`
      is consistent with `sandbox/tigerbeetle/README.md` — pending Transfer for Phase A,
      post_pending + liability transfers for Phase B, transit 3100=0 invariant; note that these
      are **in-process Java calls within `app-accounting-worker`**, not HTTP calls
      <!-- verified: consistent — hash(businessRef) as TB uint128 IDs, Phase A pending flag, Phase B post_pending + 2 transfers, transit 3100 net=0 confirmed -->

**Checkpoint**: US1 independently demonstrable (notify → 202 → POSTED → wallet credited).

---

## Phase 4: User Story 2 — Unknown VA Held for Ops (Priority: P2)

**Goal**: Deposit for an unmapped virtual account is held for ops review; no wallet credit, no
journal created.

**Entry-point behavior for this story**: same queue entry (BANK_DEPOSIT → app-accounting-worker),
but worker routes to ops hold instead of Phase A when VA lookup fails.

**Independent test**: Send notify with unknown VA → no wallet credit, no coa_trans row, ops hold
logged (quickstart Q3).

- [ ] T016 [US2] Finalize VA resolution + ops hold behavior in `spec/processes.md §13` — unknown VA
      path: `app-accounting-worker` detects unmapped VA after receiving `BANK_DEPOSIT` from queue;
      no journal, no credit; hold logged for ops; duplicate same VA-unknown idempotent; note that
      VA lookup happens **inside the worker** (not in orchestration for this path)
- [ ] T017 [P] [US2] Add Gherkin to `design-v2/acceptance.md`:
      - TC-DEP-05: unknown VA ops hold (no journal, no credit after worker processes queue message)
      - TC-DEP-06: correct member credit (mapped VA credits exactly the right member)
      - TC-DEP-07: mapping-change immutability (VA mapping update does not retroactively alter POSTED journals)

**Checkpoint**: US2 demonstrable on top of US1.

---

## Phase 5: User Story 3 — Deposit Failure and PENDING Aging (Priority: P3)

**Goal**: A cancelled or mismatched deposit reverses Phase A only; wallet balance unchanged
throughout.

**Entry-point behavior for this story**: reversal is triggered by an ops command or bank cancel
event, processed by `app-accounting-worker`. The wallet worker never receives `WALLET_CREDIT` for
cancelled deposits — no queue message means no wallet mutation.

**Independent test**: Create PENDING deposit, cancel it, confirm 3100=0 and wallet balance unchanged
(quickstart Q4).

- [ ] T018 [US3] Finalize Phase-A reversal process in `spec/processes.md §13` — cancel/mismatch
      clears 1111/3100 only via `void_pending_transfer` inside `app-accounting-worker`; wallet
      never touched because **no `WALLET_CREDIT` command is published** when Phase B fails; coa_trans
      → FAILED; POSTED journals are immutable (ADR-001)
- [ ] T019 [P] [US3] Add Gherkin to `design-v2/acceptance.md`:
      - TC-DEP-08: cancel happy (Phase A reversed, 3100=0, wallet unchanged, no WALLET_CREDIT queued)
      - TC-DEP-09: mismatch-amount (Phase B fails validation, Phase A reversed, no wallet queue message)
      - TC-DEP-10: aging PENDING (PENDING journal not resolved triggers aging alert per ADR-021)

**Checkpoint**: Full deposit lifecycle (create, confirm, cancel) covered.

---

## Phase 6: Polish & Cross-Cutting

**Purpose**: idempotency proof, invariant coverage, worker resilience, contract hygiene, and
confirming no HTTP gateway is called where a queue entry is required.

- [ ] T020 [P] Add idempotency Gherkin to `design-v2/acceptance.md`:
      - TC-DEP-11: duplicate notify same businessRef → one journal one credit (quickstart Q2)
      - TC-DEP-12: worker crash + redelivery → `app-accounting-worker` is idempotent on
        `(commandType, businessRef)` — no duplicate Phase A or Phase B (quickstart Q5)
- [ ] T021 [P] Add SQL invariant CI note in `design-v2/acceptance.md` Part IV: INV-03 covers
      `TB account[3100].balance = 0` after POSTED deposit — verify gap or confirm coverage (ADR-031)
- [ ] T022 [P] Validate `core-commands.yaml` and `core-events.yaml` YAML parse and all deposit
      schemas resolve (`$ref` integrity); confirm queue message schemas match the entry-point map
      table at the top of this file
- [ ] T023 [P] Validate `accounting-internal.yaml` — confirm `createJournal` and `confirmDeposit`
      endpoints exist (ADR-037 AC-037-02) and are marked as **internal-only, not called by
      orchestration in the deposit flow**; add a note in `data-model.md §0` clarifying this
- [ ] T024 Add a "Protocol Decision" note in `spec/processes.md §13` explaining WHY deposit uses
      async queue (not sync HTTP) for both accounting and wallet: decouples 202 latency from ledger
      write time; isolates worker failures from sync P99; enables at-least-once + idempotent replay
- [ ] T025 Verify TigerBeetle out-of-scope boundary in `design-v2/acceptance.md` or
      `spec/processes.md`: confirm `app-orchestration`, `app-wallet`, `app-wallet-worker` have NO
      direct TB client dependency (AC-037-01); only `app-accounting-worker` → `core.accounting`
      → TB
- [ ] T026 [P] Confirm `CLAUDE.md` SPECKIT marker points to `specs/002-async-deposit/plan.md`

---

## Dependencies & Execution Order

- **Setup (P1)** → **Foundational (P2)** block everything.
- **US1 (P3)** is MVP — wallet credit requires process + orchestration + Gherkin for Phase A/B.
- **US2 (P4)** and **US3 (P5)** are independent of each other once US1 + Foundational done.
- **Polish (P6)** after the stories it asserts.

```
Setup → Foundational → US1 ─┬─► US2 ─┐
                             └─► US3 ─┴─► Polish
```

## Parallel Opportunities

- Foundational: T004, T005, T006, T007, T008 are different files → parallel.
- T009, T010, T011 depend on T004/T005 being understood — serialize with them.
- Within US1: T014 (Gherkin) and T015 (TB mapping check) are different files → parallel.
- Within US2: T017 (Gherkin) is independent once T016 (process) is done.
- Within US3: T019 (Gherkin) is independent once T018 (process) is done.
- Polish: T020, T021, T022, T023 are all different files → parallel.
- **Serialize edits to `design-v2/acceptance.md`** even for [P] tasks — same-file contention.

## Implementation Strategy

- **MVP = US1** (deposit lands in wallet). Delivers the core money-in flow.
- Increment: + US2 (unknown VA safety) → + US3 (cancel/aging) → + Polish.
- Entry-point map (top of this file) is the contract before any code — any PR that crosses a
  service boundary must name the protocol (queue or HTTP) in the PR description.

## Notes

- Most foundational artifacts are partially drafted; tasks are "finalize/verify and fill gaps".
- Primary new work: Gherkin acceptance scenarios (T014, T017, T019, T020), outbox DDL (T010),
  queue command schemas (T004/T005), and the entry-point map documentation (T003, T011, T024).
- TigerBeetle details belong inside `core.accounting` — no task here introduces a direct TB call
  from orchestration or wallet (would violate AC-037-01).
- Blnk PoC interfaces (WalletBalanceMonitor, getBalanceAt) from data-model.md §3 are PoC patterns
  for a separate ADR — not included as deposit feature tasks.
