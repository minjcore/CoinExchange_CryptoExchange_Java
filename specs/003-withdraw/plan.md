# Implementation Plan: Withdraw (P5)

**Branch**: `003-withdraw` | **Date**: 2026-06-19 | **Spec**: [spec.md](./spec.md)

## Summary

A USER member withdraws funds to a bank account. Orchestration **synchronously** freezes the gross amount (principal + fee) and creates a PENDING accounting journal before returning 200. A payout worker then dispatches the bank transfer asynchronously. On bank confirmation: `confirmWithdraw` posts the transit leg (3200 nets to zero) and the wallet settles frozen funds. On terminal failure: `voidWithdraw` clears the PENDING journal and the wallet releases frozen funds back to available.

**Two-phase TigerBeetle pattern — same shape as deposit but different accounts and sync split:**
- Phase A (sync, before 200): TB pending Transfer `2110 DR / 3200 CR`
- Phase B (async, on settle): `post_pending` + 2 transfers `3200 DR / 1111 CR` + `3200 DR / 4120 CR`; assert `account[3200].balance = 0`
- Release (async, on fail): `void_pending_transfer`; assert `account[3200].balance = 0`

## Technical Context

**Language/Version**: Java 17; Spring Boot 3.3.x on `app-*` modules; `core.*` = pure domain JARs

**Primary Dependencies**:
- `core.accounting` adapter: TigerBeetle Java client (pending Transfer on accept; post/void on settle/release) + PostgreSQL (`coa_trans` state)
- `core.wallet`: PostgreSQL snapshot — WITHDRAW_FREEZE / WITHDRAW_SETTLE / WITHDRAW_RELEASE
- Messaging: RabbitMQ `WITHDRAW_PAYOUT` command (async bank dispatch); Kafka optional for `WithdrawSettled` event
- Outbox: transactional outbox in PostgreSQL `accounting` schema (ADR-013)

**Storage**:
- PostgreSQL `wallet` schema: `wallet_balance`, `wallet_tx`
- PostgreSQL `accounting` schema: `coa_trans` (journal header + state), outbox
- TigerBeetle cluster: hot postings; accept = pending Transfer; settle = post_pending + 2 transfers; release = void_pending

**Latency budget (sync accept path)**:
- Each internal HTTP hop: ~20-50 ms network + ~5-10 ms DB + ~10-15 ms logic ≈ 35-75 ms
- Accept path: 2 sync calls (wallet freeze + accounting journal) ≈ 70-150 ms
- SC-001 SLA: 200 accepted in < 500 ms — budget met with margin

**Testing**: Gherkin acceptance in `design-v2/acceptance.md`; SQL invariant CI (ADR-031 INV for 3200=0)

**Target Platform**: Linux containers; sync accept path served by `app-orchestration`; async payout by `app-payout-worker`

**Constraints**: Two-domain boundary; VND only; fee computed by orchestration before freeze; wallet LOCKED rejects freeze (ADR-029); timeout ≠ release (ADR-033)

## Constitution Check

| Principle | Compliance | Evidence |
|-----------|-----------|----------|
| I. Two-domain separation | ✅ | Wallet freeze via HTTP to `app-wallet`; journal via HTTP to `app-accounting`; no cross-schema access (ADR-012) |
| II. Immutable, balanced ledger | ✅ | Accept = pending Transfer; settle = post via `confirmWithdraw`; release = void; transit 3200 = 0 at terminal (ADR-007, ADR-010) |
| III. Wallet hot path; no sync aggregate | ✅ | WITHDRAW_FREEZE single `wallet_tx` insert; settle/release each one `wallet_tx` insert (ADR-004) |
| IV. Idempotency end-to-end | ✅ | Accept key `businessRef`; settle key `{businessRef}:settle`; release key `{businessRef}:release`; TB Transfer id = hash(businessRef+leg) (ADR-005, ADR-007) |
| V. Orchestration sole sequencer; saga not 2PC | ✅ | Accept: orchestration freezes + journals sync; payout: async worker; settle/release: worker calls back to accounting+wallet; no 2PC (ADR-008) |
| VI. Money & currency discipline | ✅ | VND only; orchestration computes fee once; gross = principal + fee passed atomically (ADR-009, ADR-028) |
| VII. Contracts & conformance | ✅ | `wallet-internal.yaml` freeze/settle/release endpoints; `accounting-internal.yaml` createJournal/confirmWithdraw/voidWithdraw; new Gherkin traces ADR-007 |

**Result: PASS — no constitution violations.**

## Project Structure

### Documentation (this feature)

```text
specs/003-withdraw/
├── plan.md              # This file
├── research.md          # R1: freeze-settle-release; R2: TB for withdraw; R3: double-spend rule
├── data-model.md        # Service entry map; entities; TB transfer mapping; inbound/outbound
├── quickstart.md        # Validation guide (accept, settle, release, idempotency, error paths)
├── contracts/           # WITHDRAW_PAYOUT command + WithdrawSettled/Released event refs
└── tasks.md             # Phased tasks
```

### Artifacts in the main repo

```text
adr/ADR-007-freeze-settle-async-outflow.md       # freeze-settle-release authority
adr/ADR-033-bank-poll-t2-frozen-tmax.md          # polling + aging SLAs
spec/contracts/async-api/core-commands.yaml      # WITHDRAW_PAYOUT command (add)
spec/contracts/async-api/core-events.yaml        # WithdrawSettled, WithdrawReleased (add)
spec/processes.md        §13.4                   # withdraw saga narrative
design-v2/acceptance.md                          # TC-WDR Gherkin (add)
design-v2/orchestration.md §14                  # WITHDRAW saga (update)
```
