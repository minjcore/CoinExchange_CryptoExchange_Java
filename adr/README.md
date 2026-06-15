# Architecture Decision Records (ADR)

**41 ADRs** extracted from main specs + reference gaps. Each includes **AC** + **TC** → [`design-v2/acceptance.md`](../design-v2/acceptance.md).

## Ledger & accounting

| ADR | Title | Source |
|-----|-------|--------|
| [001](ADR-001-immutable-ledger.md) | Immutable ledger | `core.accounting.trd.md`, foundation Part II |
| [006](ADR-006-two-phase-deposit.md) | Two-phase deposit, transit 3100 | foundation §8, `IMPLEMENTATION.md` §7.5 |
| [010](ADR-010-transit-accounts-net-zero.md) | Transit 3100–3820 net zero at POSTED | foundation §4–5 |
| [023](ADR-023-accounting-period-close.md) | Period close blocks closed dates | TRD FR-8 |
| [025](ADR-025-ibft-napas-clearing-1112.md) | IBFT uses 1112 not 1111 | foundation §11 |

## Wallet

| ADR | Title | Source |
|-----|-------|--------|
| [004](ADR-004-wallet-balance-snapshot.md) | Snapshot + append-only `wallet_tx` | `core.wallet.md` §3, §6 |
| [007](ADR-007-freeze-settle-async-outflow.md) | Freeze–settle–release withdraw/IBFT | `IMPLEMENTATION.md` D5, business-processes §13.4 |
| [014](ADR-014-reconciliation-w5-report-only.md) | W5 recon report-only | `core.wallet.md` W5 |
| [016](ADR-016-qr-pos-default-no-per-txn-wallet.md) | QR/POS default no wallet per txn | `core.wallet.md` §5.8 |
| [020](ADR-020-wallet-lanes-coa-control-mapping.md) | USER/MERCHANT/PARTNER → 2110/2120/2130 | foundation §6.2 |
| [024](ADR-024-deposit-wallet-credit-dual-path.md) | JournalPosted or WALLET_CREDIT | `core.wallet.md` §7.3 |
| [026](ADR-026-wallet-never-reverses-accounting.md) | Wallet never posts/reverses ledger | `design-v2/wallet.md` §7 |
| [029](ADR-029-wallet-locked-rejects-mutation.md) | LOCKED rejects debit/freeze | `core.wallet.md` FR-6 |
| [040](ADR-040-user-multi-pocket-wallets.md) | USER multi-pocket (label + `wallet_pocket_def`); merchant/partner single | design review 2026-06-12 |

## Platform & boundaries

| ADR | Title | Source |
|-----|-------|--------|
| [002](ADR-002-core-foundation-shared-library.md) | `core.foundation` only shared lib | foundation Part I |
| [003](ADR-003-dual-schema-single-postgres.md) | Schema `wallet` + `accounting` | `IMPLEMENTATION.md` §1.4 |
| [005](ADR-005-idempotency-key-strategy.md) | `business_ref` end-to-end | `integration-surfaces.md` §8 |
| [008](ADR-008-saga-compensation-no-2pc.md) | Saga, no 2PC | business-processes §13–15 |
| [009](ADR-009-fee-ownership-orchestration.md) | Fees at orchestration | business-processes §16 |
| [012](ADR-012-orchestration-integration-forbidden-rules.md) | Forbidden F1–F6 | `integration-surfaces.md` §10 |
| [018](ADR-018-openapi-asyncapi-wire-truth.md) | OpenAPI/AsyncAPI wire truth | foundation §5, integration-surfaces |
| [028](ADR-028-money-scale-four-half-up.md) | Scale 4 HALF_UP | foundation §6 |

## Orchestration & integration

| ADR | Title | Source |
|-----|-------|--------|
| [011](ADR-011-auth-identity-jwt-subject.md) | JWT sub = memberId | business-processes §17 |
| [013](ADR-013-outbox-at-least-once-messaging.md) | Outbox + at-least-once | business-processes §15 |
| [015](ADR-015-eod-settlement-independent-batch.md) | EOD batch not inline | foundation §4, §16 |
| [017](ADR-017-partial-batch-payroll-disbursement.md) | Partial batch OK | business-processes §13.6 |
| [019](ADR-019-vnd-single-currency-v1.md) | VND only v1 | wallet §3.2, business-processes §14 |
| [021](ADR-021-aging-jobs-async-pending.md) | Aging jobs | business-processes §14–15 |
| [022](ADR-022-mtls-bank-webhooks.md) | mTLS bank webhooks | business-processes §17 |
| [027](ADR-027-sync-payment-transfer-three-commits.md) | Payment/transfer 3 commits | `IMPLEMENTATION.md` §8 |
| [030](ADR-030-virtual-account-deposit-mapping.md) | VA → memberId | foundation §8.3 |

## Reference gaps (031–041)

| ADR | Title | Source |
|-----|-------|--------|
| [031](ADR-031-sql-ledger-invariant-ci.md) | SQL invariant CI / nightly | accounting §27, Slope P2 |
| [032](ADR-032-wallet-balance-monitoring.md) | Balance threshold monitors | wallet §28, Blnk |
| [033](ADR-033-bank-poll-t2-frozen-tmax.md) | Bank poll T2 / frozen Tmax config | orchestration §23 |
| [034](ADR-034-locked-wallet-deposit-credit-reject.md) | LOCKED rejects deposit credit (W-O1) | wallet W-O1, ADR-029 |
| [035](ADR-035-rabbitmq-workers-not-temporal-v1.md) | RabbitMQ workers v1, not Temporal | orchestration §25.7, ADR-013 |
| [041](ADR-041-rabbitmq-orch-to-accounting-worker.md) | `BANK_DEPOSIT`: app-orchestration → accounting worker | integration-surfaces §7, ADR-006/013 |
| [036](ADR-036-accrual-basis-ledger-v1.md) | Accrual-like ledger v1, not cash basis | accounting §29.2, ADR-006/007 |
| [037](ADR-037-tigerbeetle-ledger-backing-store.md) | **core.accounting wraps TigerBeetle** (hybrid PG) | accounting TRD §8/§12, sandbox/tigerbeetle |
| [038](ADR-038-orchestrator-separate-service-gateway-seam.md) | Orchestrator = separate service; HTTP gateway seam (in-process = migration debt only) | wallet §7.2, arch-overview §2, ADR-012 |
| [039](ADR-039-no-synchronous-wallet-aggregate-row.md) | No synchronous wallet aggregate row; aggregate = COA control + async rollup | design review 2026-06-12, ADR-004/020/014 |
| [040](ADR-040-user-multi-pocket-wallets.md) | USER multi-pocket wallets (label + `wallet_pocket_def`) | design review 2026-06-12, ADR-020/039 |

## Conformance

```
Main file → ADR (AC/TC) → design-v2/*.md → acceptance.md Gherkin
```

Structural TC (module graph, DDL, mTLS): CI/manual — not all Gherkin.

New ADR: `ADR-NNN-short-title.md` + AC/TC + update this index + link from source doc.

**Terms:** [`../spec/terminology.md`](../spec/terminology.md) · **Flow:** [`../README.md`](../README.md)
