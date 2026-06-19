# Implementation Plan: Internal Transfer (UC-4)

**Branch**: `005-internal-transfer` | **Date**: 2026-06-19 | **Spec**: [spec.md](./spec.md)

## Summary

A USER member transfers funds to another USER member within the platform. Orchestration **synchronously** debits the sender, creates an immediately-POSTED accounting journal (3 TigerBeetle transfers), credits the receiver, and returns 200. There is no async phase — the 3-commit pattern completes in a single synchronous round-trip.

**Three-commit TigerBeetle pattern (non-pending, immediately POSTED):**
- Debit leg: `2110_sender DR / 3300 CR` amount=gross×10⁴
- Credit leg: `3300 DR / 2110_receiver CR` amount=net×10⁴ (net = principal)
- Fee leg: `3300 DR / 4130 CR` amount=fee×10⁴
- Assert `account[3300].balance = 0` (gross = net + fee)

No pending transfers, no async worker, no outbox. All steps complete before 200.

## Technical Context

**Language/Version**: Java 17; Spring Boot 3.3.x on `app-*` modules; `core.*` = pure domain JARs

**Primary Dependencies**:
- `core.accounting` adapter: TigerBeetle Java client (non-pending batch of 3 transfers) + PostgreSQL (`coa_trans` state)
- `core.wallet`: PostgreSQL snapshot — TRANSFER_DEBIT / TRANSFER_CREDIT wallet TX types
- No RabbitMQ — fully synchronous; no outbox required
- No async worker

**Storage**:
- PostgreSQL `wallet` schema: `wallet_balance`, `wallet_tx`
- PostgreSQL `accounting` schema: `coa_trans` (journal header, immediately POSTED)
- TigerBeetle cluster: 3 non-pending transfers in a single batch

**Latency budget (sync path)**:
- Each internal HTTP hop: ~20-50 ms network + ~5-10 ms DB + ~10-15 ms logic ≈ 35-75 ms
- Transfer path: 3 sync HTTP calls (TRANSFER_DEBIT + createJournal + TRANSFER_CREDIT) ≈ 105-225 ms
- SC-001 SLA: 1 second — budget met with significant margin

**Testing**: Gherkin acceptance in `design-v2/acceptance.md`; SQL invariant CI (ADR-031 INV for 3300=0)

**Target Platform**: Linux containers; entire flow served synchronously by `app-orchestration`

**Constraints**: Sender ≠ receiver (different memberId); VND only; fee computed by orchestration before debit (ADR-009); no freeze/unfreeze cycle — direct debit on sender, direct credit on receiver; compensation via ADJUSTMENT_CREDIT if step 3 fails after step 2

## Constitution Check

| Principle | Compliance | Evidence |
|-----------|-----------|----------|
| I. Two-domain separation | ✅ | Wallet debit/credit via HTTP to `app-wallet`; journal via HTTP to `app-accounting`; no cross-schema access (ADR-012) |
| II. Immutable, balanced ledger | ✅ | 3 non-pending transfers in one TB batch; transit 3300 = 0 after batch (ADR-007, ADR-010); POSTED immediately |
| III. Wallet hot path; no sync aggregate | ✅ | TRANSFER_DEBIT single `wallet_tx` insert; TRANSFER_CREDIT single `wallet_tx` insert (ADR-004) |
| IV. Idempotency end-to-end | ✅ | TB Transfer IDs = `hash(businessRef+":debit")`, `hash(businessRef+":credit")`, `hash(businessRef+":4130")`; wallet unique constraint `(wallet_id, business_ref, tx_type)` (ADR-005) |
| V. Orchestration sole sequencer; saga not 2PC | ✅ | Orchestration sequences all 3 steps; compensation ADJUSTMENT_CREDIT if step 3 fails; no 2PC (ADR-008, ADR-027) |
| VI. Money & currency discipline | ✅ | VND only; orchestration computes fee once; gross = principal + fee; net = principal; fee = revenue only (ADR-009, ADR-028) |
| VII. Contracts & conformance | ✅ | `wallet-internal.yaml` debit/credit endpoints; `accounting-internal.yaml` createJournal(TRANSFER, POSTED); Gherkin traces ADR-007 |

**Result: PASS — no constitution violations.**

## Project Structure

### Documentation (this feature)

```text
specs/005-internal-transfer/
├── plan.md              # This file
├── research.md          # R1: sync 3-commit pattern; R2: TB non-pending for transfer; R3: compensation model
├── data-model.md        # Service entry map; entities; TB transfer mapping; inbound/outbound
├── quickstart.md        # Validation guide (happy path, compensation, forward retry, idempotency, error paths)
└── tasks.md             # Phased tasks
```

### Artifacts in the main repo

```text
adr/ADR-027-internal-transfer-sync-3commit.md        # 3-commit authority
spec/processes.md        §13.5                        # internal transfer narrative (add)
design-v2/acceptance.md                              # TC-TFR Gherkin (add)
design-v2/orchestration.md §15                       # TRANSFER saga (update)
```
