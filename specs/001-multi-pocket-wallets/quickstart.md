# Quickstart — Validating USER Multi-Pocket Wallets

A validation guide proving the feature end-to-end. Since 10_core is a design/spec repo, "running"
means asserting the **artifacts** are coherent and the **acceptance scenarios** hold; once
`core.wallet`/`app-orchestration` implement it, the same scenarios run as integration tests.

## Prerequisites

- Pocket catalog seeded: `wallet_pocket_def` has `DEFAULT`(is_default), `SPENDING`, `SAVINGS`, `GOAL`(multi_allowed). (implementation.md §3 seed)
- A USER member exists with an auto-provisioned `'default'` pocket.
- Schema migration applied (data-model.md §Migration) — or target V1 DDL for fresh installs.

## Artifact coherence checks (this repo)

| Check | Where | Pass condition |
|-------|-------|----------------|
| DDL has pocket columns + indexes | `spec/implementation.md` §3 | `pocket_code`, `label`, `uq_wallet_member_type_ccy_label`, `uq_wallet_single_nonuser`, `wallet_pocket_def` present |
| OpenAPI parses & has pocket ops | `spec/contracts/openapi/gtelpay-public.yaml` | `listPocketDefs/listPockets/createPocket/closePocket/createPocketTransfer` present; YAML valid |
| Behavior documented | `spec/processes.md` §11A | create/close/pocket-transfer flows present |
| Orchestration resolution | `design-v2/orchestration.md` §1.2/§3.7 | member+pocket→wallet_id rule present |
| Constitution gate | this plan §Constitution Check | all 7 principles ✅ |

## End-to-end validation scenarios (map to spec acceptance)

| # | Scenario | Steps | Expected | Spec ref |
|---|----------|-------|----------|----------|
| Q1 | Create pocket (P1) | `GET /wallets/pocket-defs` → `POST /wallets/pockets {pocketCode:SAVINGS,label:"Tiết kiệm"}` | 201; pocket in `GET /wallets/pockets`; available=0 | US1 / FR-001,002 |
| Q2 | Duplicate label | repeat Q2 create same label | 409 `WALLET_DUPLICATE_CONFLICT` | US1 / FR-003 |
| Q3 | Multi-allowed GOAL | create two `GOAL` pockets diff labels | both exist | US1 / FR-003 |
| Q4 | Pocket→pocket move (P2) | fund A; `POST /wallets/pocket-transfers {from:A,to:B,amount:60000}` | A−60k, B+60k, member total unchanged, 2110 net 0 | US2 / FR-005 |
| Q5 | Move insufficient | move > A.available | 422; no change | US2 / FR-006 |
| Q6 | Balance defaults to default pocket | `GET /wallets/balance` (no pocket) | returns `'default'` pocket | US3 / FR-007 |
| Q7 | Balance targets pocket | `GET /wallets/balance?walletId=B` | returns B only | US3 / FR-007 |
| Q8 | Close empty pocket (P3) | empty a pocket → `POST .../{walletId}/close` | CLOSED; rejects further mutation | US4 / FR-008 |
| Q9 | Close non-empty / default | close funded pocket; close default | 409 both | US4 / FR-008 |
| Q10 | Backward compat | existing single-wallet client calls balance/pay with **no** pocket fields | behaves as before (default pocket) | data-model §Backward compat |
| Q11 | Reconciliation | sum all USER pockets vs 2110 | within W5 tolerance | FR-009 / SC-005 |
| Q12 | Idempotent replay | replay Q1 create & Q4 move | no duplicate pocket; no double-debit | FR-011 / SC-004 |

## Done

Feature is validated when Q1–Q12 pass as Gherkin in `design-v2/acceptance.md` and the artifact
coherence table is green. `/speckit-tasks` will turn these into an ordered task list (incl.
acceptance authoring + ErrorCode registration follow-ups).
