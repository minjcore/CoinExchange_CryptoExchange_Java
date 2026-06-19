# Implementation Plan: IBFT (UC-5)

**Branch**: `006-ibft` | **Date**: 2026-06-19 | **Spec**: [spec.md](./spec.md)

## Summary

A USER member transfers funds to an external bank account via Napas interbank. Orchestration **synchronously** freezes the gross amount (principal + platformFee) and creates a PENDING accounting journal before returning 200. A payout worker then dispatches the Napas/bank transfer asynchronously. On bank confirmation: `confirmIbft` posts the transit leg (3400 nets to zero) plus a separate Napas cost expense leg, then settles the wallet. On terminal failure: `voidIbft` clears the PENDING journal and the wallet releases frozen funds back to available.

IBFT is structurally identical to withdraw (freeze→settle/release) with two differences: transit account is 3400 (not 3200), Napas clearing uses account 1112 (not 1111 Vietinbank Nostro — ADR-025), and settle posts an additional Napas cost leg (5100 DR / 1112 CR) that is independent of the 3400=0 invariant.

**Two-phase TigerBeetle pattern — same shape as withdraw but different accounts and extra cost leg:**
- Phase A (sync, before 200): TB pending Transfer `2110 DR / 3400 CR (gross)`
- Phase B (async, on settle): `post_pending` + 3 transfers `3400→4130 (platformFee)` + `3400→1112 (principal)` + `5100→1112 (napasCost)`; assert `account[3400].balance = 0`
- Release (async, on fail): `void_pending_transfer`; assert `account[3400].balance = 0`

## Technical Context

**Language/Version**: Java 17; Spring Boot 3.3.x on `app-*` modules; `core.*` = pure domain JARs

**Primary Dependencies**:
- `core.accounting` adapter: TigerBeetle Java client (pending Transfer on accept; post/void on settle/release) + PostgreSQL (`coa_trans` state)
- `core.wallet`: PostgreSQL snapshot — IBFT_FREEZE / IBFT_SETTLE / IBFT_RELEASE
- Messaging: RabbitMQ `IBFT_PAYOUT` command (async bank dispatch); Kafka optional for `IbftSettled` event
- Outbox: transactional outbox in PostgreSQL `accounting` schema (ADR-013)

**Storage**:
- PostgreSQL `wallet` schema: `wallet_balance`, `wallet_tx`
- PostgreSQL `accounting` schema: `coa_trans` (journal header + state), outbox
- TigerBeetle cluster: hot postings; accept = pending Transfer; settle = post_pending + 3 transfers; release = void_pending

**Latency budget (sync accept path)**:
- Each internal HTTP hop: ~20-50 ms network + ~5-10 ms DB + ~10-15 ms logic ≈ 35-75 ms
- Accept path: 2 sync calls (wallet freeze + accounting journal) ≈ 70-150 ms
- SC-001 SLA: 200 accepted in < 500 ms — budget met with margin

**Testing**: Gherkin acceptance in `design-v2/acceptance.md`; SQL invariant CI (ADR-031 INV for 3400=0)

**Target Platform**: Linux containers; sync accept path served by `app-orchestration`; async payout by `app-payout-worker`

**Constraints**: Two-domain boundary; VND only; fee computed by orchestration before freeze; napasCost paid by platform (not deducted from principal); wallet LOCKED rejects freeze (ADR-029); timeout ≠ release (ADR-033)

## Constitution Check

| Principle | Compliance | Evidence |
|-----------|-----------|----------|
| I. Two-domain separation | ✅ | Wallet freeze via HTTP to `app-wallet`; journal via HTTP to `app-accounting`; no cross-schema access (ADR-012) |
| II. Immutable, balanced ledger | ✅ | Accept = pending Transfer; settle = post via `confirmIbft`; release = void; transit 3400 = 0 at terminal; Napas cost leg 5100/1112 is separately balanced (ADR-007, ADR-010) |
| III. Wallet hot path; no sync aggregate | ✅ | IBFT_FREEZE single `wallet_tx` insert; settle/release each one `wallet_tx` insert (ADR-004) |
| IV. Idempotency end-to-end | ✅ | Accept key `businessRef`; settle key `{businessRef}:settle`; release key `{businessRef}:release`; TB Transfer id = hash(businessRef+leg) (ADR-005, ADR-007) |
| V. Orchestration sole sequencer; saga not 2PC | ✅ | Accept: orchestration freezes + journals sync; payout: async worker; settle/release: worker calls back to accounting+wallet; no 2PC (ADR-008) |
| VI. Money & currency discipline | ✅ | VND only; orchestration computes platformFee and napasCost once; gross = principal + platformFee (napasCost not in gross — platform bears it) (ADR-009, ADR-028) |
| VII. Contracts & conformance | ✅ | `wallet-internal.yaml` freeze/settle/release endpoints; `accounting-internal.yaml` createJournal/confirmIbft/voidIbft; new Gherkin traces ADR-007 |

**Result: PASS — no constitution violations.**

## Project Structure

### Documentation (this feature)

```text
specs/006-ibft/
├── plan.md              # This file
├── research.md          # R1: freeze-settle-release for IBFT; R2: TB accounts; R3: double-spend rule; R4: Napas cost leg
├── data-model.md        # Service entry map; entities; TB transfer mapping; inbound/outbound
├── quickstart.md        # Validation guide (accept, settle, release, idempotency, error paths)
├── contracts/           # IBFT_PAYOUT command + IbftSettled/Released event refs
└── tasks.md             # Phased tasks
```

### Artifacts in the main repo

```text
adr/ADR-007-freeze-settle-async-outflow.md       # freeze-settle-release authority
adr/ADR-025-napas-clearing-account.md            # 1112 not 1111 — Napas clearing
adr/ADR-033-bank-poll-t2-frozen-tmax.md          # polling + aging SLAs
spec/contracts/async-api/core-commands.yaml      # IBFT_PAYOUT command (add)
spec/contracts/async-api/core-events.yaml        # IbftSettled, IbftReleased (add)
spec/processes.md        §13.5                   # IBFT saga narrative
design-v2/acceptance.md                          # TC-IBFT Gherkin (add)
design-v2/orchestration.md §15                  # IBFT saga (update)
```
