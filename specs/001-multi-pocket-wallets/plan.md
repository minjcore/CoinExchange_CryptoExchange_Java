# Implementation Plan: USER Multi-Pocket Wallets

**Branch**: `001-multi-pocket-wallets` | **Date**: 2026-06-15 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/001-multi-pocket-wallets/spec.md`

## Summary

Let a USER member hold multiple wallet pockets ("ngăn ví") — each a full wallet row with its own
spendable balance — instantiated from a `wallet_pocket_def` catalog and distinguished by a user
label; MERCHANT/PARTNER stay single-wallet. Capabilities: list pocket defs, create pocket, close
pocket (zero-balance, default not closable), pocket-to-pocket transfer (same member), and
pocket-addressed balance/spend. All USER pockets roll up to accounting control **2110**; no new
authoritative balance store.

**Technical approach (this repo):** the decision is already locked in [ADR-040](../../adr/ADR-040-user-multi-pocket-wallets.md);
this plan does **not** invent design — it sequences the **spec/contract/DDL artifacts** and
acceptance coverage that realize the feature, and verifies they satisfy the constitution. The
runtime implementation (Java `core.wallet` + `app-orchestration`) is downstream of these artifacts.

## Technical Context

**Repo type**: Design/spec repository (artifacts: ADR, spec, DDL, OpenAPI/AsyncAPI, Gherkin). No
runtime code committed here; `platform/core.wallet` + `app-orchestration` are the downstream targets.

**Primary artifacts touched**:
- DDL — `spec/implementation.md` §3 (`wallet_pocket_def`, `wallet.pocket_code`, `label`, partial unique index) — **drafted**
- Contracts — `spec/contracts/open-api/gtelpay-public.yaml` pocket endpoints + schemas — **drafted**
- Behavior — `spec/processes.md` §11A; `design-v2/orchestration.md` §1.2 / §3.7 — **drafted**
- Data model design — `design/platform/data-model.md` §3.1 — **drafted**
- Conformance — `design-v2/acceptance.md` (pocket Gherkin) — **to extend** (ADR-040 AC/TC)

**Storage**: PostgreSQL, schema `wallet` only (no `accounting` schema change — 2110 mapping already exists). (ADR-003, ADR-020)

**Testing / conformance**: Gherkin acceptance scenarios + SQL ledger-invariant CI (ADR-031); the W5 reconciliation already covers the 2110 rollup invariant.

**Target consumers**: mobile/partner via S1 public API; `app-orchestration` resolves pocket → `wallet_id`.

**Performance goals**: pocket balance read = single-row lookup, no degradation with pocket count (SC-003); mutation locks one `wallet_id` row only (SC per ADR-004/039).

**Constraints**: two-domain boundary (no cross-schema JOIN/FK); no synchronous aggregate row; VND single currency; fee 0 default for own-pocket move.

**Scale/Scope**: per-member small pocket count (units–tens); USER-lane only.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Compliance | Evidence |
|-----------|-----------|----------|
| I. Two-domain separation | ✅ | Feature is wallet-schema only; accounting unchanged; pockets roll up to existing 2110; no cross-schema JOIN/FK (ADR-040 §4, ADR-020) |
| II. Immutable, balanced ledger | ✅ | Create/close post **no** ledger; pocket-to-pocket reuses balanced internal-transfer journal (2110 DR/CR net 0) — no new ledger pattern |
| III. Wallet hot path; no sync aggregate | ✅ | Pocket = full wallet row, per-`wallet_id` lock; "total = Σ pockets" derived, no stored aggregate (ADR-039); default-pocket resolution keeps single-row reads (ADR-004) |
| IV. Idempotency end-to-end | ✅ | Create idempotent on `businessRef`; pocket-transfer uses `(wallet_id, business_ref, tx_type)` triple (ADR-005) |
| V. Orchestration sole sequencer | ✅ | Orchestration resolves member+pocket→`wallet_id` before any wallet leg; pocket-transfer reuses transfer saga (ADR-038/040 §5, orchestration §1.2/§3.7) |
| VI. Money & currency discipline | ✅ | VND single; fee 0 default owned by orchestration (ADR-009/019/028) |
| VII. Contracts & conformance | ✅ | OpenAPI pocket endpoints drafted; ADR-040 AC/TC to be traced into acceptance.md (ADR-018/031) |

**Result: PASS — no violations.** Complexity Tracking section omitted (nothing to justify).

**Post-Design re-check (after Phase 1): PASS.** The backward-compatibility design (new columns with
DEFAULTs, additive/optional API fields, default-pocket fallback — data-model.md) introduces **no**
new violation: accounting still untouched (I), no new ledger pattern (II), single-row reads & no
aggregate row preserved (III), idempotency triple unchanged (IV), orchestration still resolves
`wallet_id` (V), VND/fee-0 (VI), contracts additive + AC/TC traceable (VII).

## Project Structure

### Documentation (this feature)

```text
specs/001-multi-pocket-wallets/
├── plan.md              # This file
├── research.md          # Phase 0 — decisions consolidated from ADRs (no open unknowns)
├── data-model.md        # Phase 1 — pocket entities, deltas vs existing DDL
├── quickstart.md        # Phase 1 — validation guide mapped to acceptance scenarios
├── contracts/           # Phase 1 — pocket API contract summary (points to gtelpay-public.yaml)
└── tasks.md             # Phase 2 — created by /speckit-tasks, not here
```

### Artifacts in the main repo (the real "source" for a spec repo)

```text
adr/ADR-040-user-multi-pocket-wallets.md          # authority
spec/implementation.md          §3               # DDL: wallet_pocket_def, wallet.pocket_code, indexes
spec/contracts/open-api/gtelpay-public.yaml         # pocket endpoints + schemas
spec/processes.md               §11A              # create / close / pocket-to-pocket behavior
design-v2/orchestration.md      §1.2, §3.7        # pocket→wallet_id resolution, pocket ops
design/platform/data-model.md   §3.1              # ERD + member-vs-balance separation
design-v2/acceptance.md                            # pocket Gherkin (to extend with ADR-040 AC/TC)
```

**Structure Decision**: This feature changes **only the `wallet` schema** and the orchestration
seam; the `accounting` schema is untouched (2110 mapping pre-exists). Work is expressed as edits to
the artifacts above plus new acceptance scenarios — consistent with a design/spec repo. No new
top-level module is introduced.
