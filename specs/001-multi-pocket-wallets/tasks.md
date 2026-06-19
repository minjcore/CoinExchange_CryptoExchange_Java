---
description: "Task list — USER multi-pocket wallets"
---

# Tasks: USER Multi-Pocket Wallets

**Input**: Design documents from `/specs/001-multi-pocket-wallets/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md

**Tests**: This is a design/spec repo — "tests" = **Gherkin acceptance scenarios** in
`design-v2/acceptance.md` (conformance), gated by SQL invariant CI (ADR-031). They are the
deliverable, so acceptance tasks are included per story.

**Organization**: by user story (spec.md US1–US4). Many artifacts are already drafted; tasks are
phrased as **author / finalize / verify** against the real files, not greenfield creation.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: parallelizable (different file, no incomplete dependency)
- File paths are repo-relative from `10_core/`.

## Path Conventions

Artifacts (this repo): `adr/`, `spec/implementation.md`, `spec/contracts/open-api/`,
`spec/contracts/async-api/`, `spec/processes.md`, `design-v2/orchestration.md`,
`design-v2/acceptance.md`, `design/platform/data-model.md`.

---

## Phase 1: Setup (Shared)

**Purpose**: confirm the feature baseline is in place.

- [X] T001 Verify spec-kit feature artifacts present in `specs/001-multi-pocket-wallets/` (spec, plan, research, data-model, contracts, quickstart)
- [X] T002 Verify `wallet_pocket_def` seed block exists in `spec/implementation.md` §3 (DEFAULT/SPENDING/SAVINGS/GOAL)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: schema, contracts, error vocabulary, and resolution rule that ALL stories depend on.
**⚠️ MUST complete before any user story.**

- [X] T003 Finalize DDL in `spec/implementation.md` §3: `wallet_pocket_def` table, `wallet.pocket_code` FK + `label`, `uq_wallet_member_type_ccy_label`, partial unique `uq_wallet_single_nonuser`, `idx_wallet_tx_wallet_created (wallet_id, created_at DESC, id DESC)`
- [X] T004 [P] Author migration `V_n__pocket_def.sql` (create + seed `wallet_pocket_def`) per data-model.md §Migration
- [X] T005 [P] Author migration `V_n+1__wallet_pockets.sql` (ALTER wallet ADD pocket_code/label DEFAULTs; swap UNIQUE; add partial unique) — backfill via DEFAULT, reversible
- [X] T006 [P] Register `WALLET_POCKET_DEF_INVALID`, `WALLET_POCKET_EXISTS`, `WALLET_POCKET_NOT_EMPTY`, `WALLET_INVALID_TRANSFER` in `core.foundation` ErrorCode (per `spec/foundation.md` §4.3 error table)
- [X] T007 [P] Add the same `WALLET_POCKET_*` codes to `spec/contracts/async-api/core-events.yaml` ErrorCode enum (align with ADR-018)
- [X] T008 Finalize pocket→`wallet_id` resolution rule in `design-v2/orchestration.md` §1.2 (verify table: walletId | pocketCode/label | default | non-USER single)

**Checkpoint**: schema + contracts + error vocab ready — stories can proceed.

---

## Phase 3: User Story 1 — Create / list pockets (Priority: P1) 🎯 MVP

**Goal**: a USER lists pocket kinds and creates named pockets from the catalog.
**Independent test**: create one pocket from a def, see it listed with zero balance (quickstart Q1).

- [X] T009 [US1] Verify OpenAPI ops `listPocketDefs`, `listPockets`, `createPocket` + schemas (`PocketDefData`, `PocketData`, `CreatePocketRequest`, `ApiResponsePocket*`) in `spec/contracts/open-api/gtelpay-public.yaml`
- [X] T010 [US1] Finalize create behavior + rules in `spec/processes.md` §11A.1 (no ledger; def-invalid / multi_allowed / duplicate-label handling)
- [X] T011 [P] [US1] Add Gherkin to `design-v2/acceptance.md` (pocket feature): create happy (TC-040-01), duplicate label (TC-040-02), multi_allowed GOAL (TC-040-03), default-not-deletable provisioning
- [X] T012 [P] [US1] Document create validation rules in `design/platform/data-model.md` §3.1 (pocket_code active, multi_allowed, label unique) — verify consistency with FR-001/002/003

**Checkpoint**: US1 independently demonstrable (catalog → create → list).

---

## Phase 4: User Story 2 — Pocket-to-pocket transfer (Priority: P2)

**Goal**: move funds between two of the member's pockets, fee 0, total unchanged.
**Independent test**: fund A, move to B, A↓ B↑ same amount, total unchanged (quickstart Q4).

- [X] T013 [US2] Verify OpenAPI op `createPocketTransfer` + `PocketTransferRequest` in `gtelpay-public.yaml`
- [X] T014 [US2] Finalize transfer behavior in `spec/processes.md` §11A.3 (reuse internal-transfer saga; 2110 DR/CR net 0; only 2 pockets lock)
- [X] T015 [US2] Verify orchestration reuse in `design-v2/orchestration.md` §3.7 (pocket→pocket maps to TRANSFER saga)
- [X] T016 [P] [US2] Add Gherkin to `design-v2/acceptance.md`: move happy + total-unchanged + 2110 net 0 (TC-040-05/06), insufficient, same-pocket, LOCKED/CLOSED

**Checkpoint**: US2 demonstrable on top of US1.

---

## Phase 5: User Story 3 — Pocket-addressed balance / spend (Priority: P2)

**Goal**: read balance / spend against a chosen pocket; default when none named.
**Independent test**: balance with/without selector returns correct pocket (quickstart Q6/Q7).

- [X] T017 [US3] Verify `GET /wallets/balance` selector params (`walletId`, `pocketCode`) + `WalletBalanceData` fields in `gtelpay-public.yaml`
- [X] T018 [US3] Confirm default-pocket fallback in `design-v2/orchestration.md` §1.2 (memberId-only → 'default')
- [X] T019 [P] [US3] Add Gherkin to `design-v2/acceptance.md`: balance defaults to default pocket, balance targets named pocket, payment debits only the named pocket

**Checkpoint**: pocket addressing works for reads and spend.

---

## Phase 6: User Story 4 — Close pocket (Priority: P3)

**Goal**: close an empty pocket; default not closable; closed rejects mutation.
**Independent test**: empty + close succeeds; close non-empty/default rejected (quickstart Q8/Q9).

- [X] T020 [US4] Verify OpenAPI op `closePocket` (`/wallets/pockets/{walletId}/close`) in `gtelpay-public.yaml`
- [X] T021 [US4] Finalize close behavior in `spec/processes.md` §11A.2 (require avail=0 & frozen=0; default not closable; CLOSED rejects mutation; idempotent)
- [X] T022 [P] [US4] Add Gherkin to `design-v2/acceptance.md`: close empty (TC-040-07), close non-empty rejected, close default rejected, replay no-op

**Checkpoint**: full pocket lifecycle covered.

---

## Phase 7: Polish & Cross-Cutting

**Purpose**: backward-compat proof, reconciliation, index/contract hygiene.

- [X] T023 [P] Add backward-compat Gherkin to `design-v2/acceptance.md`: legacy single-wallet client (no pocket fields) behaves as before via default pocket (quickstart Q10)
- [X] T024 [P] Add reconciliation Gherkin: Σ USER pockets across members = control 2110 within W5 tolerance (quickstart Q11, FR-009/SC-005, ADR-014)
- [X] T025 [P] Add idempotency Gherkin: replay create + replay move → no duplicate pocket, no double-debit (quickstart Q12, FR-011)
- [X] T026 Update `design-v2/acceptance.md` Part IV index: ADR-040 row already counts (AC 9 / TC 7) — verify the new pocket Gherkin features are linked there
- [X] T027 [P] Validate `gtelpay-public.yaml` parses (YAML) and all pocket ops/schemas resolve (`$ref` integrity)
- [X] T028 Run SQL invariant CI check intent (ADR-031): confirm INV for "Σ USER pockets vs 2110" is covered by W5/INV-06 tolerance; note any gap
- [X] T029 [P] Refresh agent context: confirm `CLAUDE.md` SPECKIT marker points to `specs/001-multi-pocket-wallets/plan.md`

---

## Dependencies & Execution Order

- **Setup (P1)** → **Foundational (P2)** block everything.
- **US1 (P3)** is the MVP and unblocks US2/US3/US4 conceptually (pockets must exist).
- **US2, US3** (both P2) are independent of each other once US1 + Foundational done — can run in parallel.
- **US4 (P3)** depends only on US1 (a pocket to close).
- **Polish (P7)** after the stories it asserts (T023/T024/T025 after US1–US3).

```
Setup → Foundational → US1 ─┬─► US2 ─┐
                            ├─► US3 ─┼─► Polish
                            └─► US4 ─┘
```

## Parallel Opportunities

- Foundational: T004, T005, T006, T007 are different files → parallel.
- Within a story, Gherkin authoring (T011/T016/T019/T022) is the single-file `acceptance.md` — **serialize edits to that file** even though marked [P] for logical independence (same-file contention).
- US2 and US3 phases can be worked in parallel by two people after US1.

## Implementation Strategy

- **MVP = US1** (create/list pockets). Delivers visible value alone (organize money into named pockets).
- Increment: + US2 (move between pockets) → + US3 (spend/read by pocket) → + US4 (close).
- Backward-compat (T023) is non-negotiable: validate before considering any story "done".

## Notes

- Most foundational artifacts are **already drafted** (DDL, OpenAPI, processes, orchestration);
  their tasks are "finalize/verify", and the genuine new work is **acceptance Gherkin (T011–T025)**,
  **ErrorCode/AsyncAPI registration (T006/T007)**, and **migration scripts (T004/T005)**.
- Constitution: Principle I (accounting untouched, no cross-schema) and III (per-wallet lock, no
  aggregate row) are asserted by T024/T028 and the data-model; no task may introduce a synchronous
  aggregate row or a cross-schema JOIN.
