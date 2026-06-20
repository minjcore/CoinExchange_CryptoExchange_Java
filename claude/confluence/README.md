# claude/confluence/

Local source of truth for Confluence pages under **Data Plane Architecture** (CF page 43581441).

## Workflow

```
Edit local .md file  →  convert to HTML  →  updateConfluencePage
(source of truth)                            (mirror)
```

**Never** author content directly in CF API calls without a corresponding local file here.

## Structure → CF Mapping

```
claude/confluence/
├── README.md                           (this file)
├── data-plane-architecture.md          → CF 43581441 (root page)
├── conclusion.md                       → CF 51609737 (parent: 43581441)
│
├── start-here/
│   ├── architecture-faq.md             → CF 51544171 (parent: 51315064)
│   └── terminology.md                  → CF 44924932 (parent: 51315064)
│
├── architecture-principles/
│   ├── wallet-accounting-overview.md   → CF 45842592 (parent: 51642382)
│   ├── platform-boundaries.md          → CF 44826626 (parent: 51642382)
│   └── correlation-idempotency.md      → CF 44859398 (parent: 51642382)
│
├── domain-trds/
│   ├── accounting-trd.md               → CF 43221005 (parent: 51183937)
│   └── wallet-trd.md                   → CF 43843585 (parent: 51183937)
│
├── contracts-integration/
│   ├── integration-surfaces.md         → CF 43220994 (parent: 51315085)
│   ├── core-platform-design.md         → CF 42041345 (parent: 51315085)
│   ├── orchestration-public-api.md     → CF 51643159 (parent: 51315085)
│   ├── accounting-api.md               → CF 51544687 (parent: 51315085)
│   ├── wallet-api.md                   → CF 51610183 (parent: 51315085)
│   └── core-commands-api.md            → CF 51643061 (parent: 51315085)
│
├── build-process/
│   ├── business-process-deposit.md     → CF 44859411 (parent: 51315106)
│   └── implementation-decisions.md     → CF 49152021 (parent: 51315106)
│
└── use-cases/
    ├── uc3-task-list.md                → CF 51872220 (parent: 51609624)
    ├── uc3-business-process-payment.md → CF 51806772 (parent: 51609624)
    ├── uc4-task-list.md                → CF 52199450 (parent: 51544073)
    ├── uc4-business-process-transfer.md → CF 51872241 (parent: 51544073)
    ├── uc5-task-list.md                → CF 51839642 (parent: 50332070)
    └── uc5-business-process-ibft.md    → CF 52199477 (parent: 50332070)
```

## Section Container Pages (nav only, no local content needed)

| CF ID | Title | Parent |
|-------|-------|--------|
| 51315064 | 📌 Start Here | 43581441 |
| 51642382 | 🏛️ Architecture & Principles | 43581441 |
| 51183937 | 📋 Domain TRDs | 43581441 |
| 51315085 | 🔌 Contracts & Integration | 43581441 |
| 51315106 | 🏗️ Build & Process | 43581441 |
| 47743025 | 🔄 Use Cases | 43581441 |
| 51642522 | 📋 Architecture Decision Records | 43581441 |

## ADR Pages (source: `adr/`, synced via cf_sync.py)

| Local file | CF ID | Parent |
|-----------|-------|--------|
| adr/ADR-001-immutable-ledger.md | 47972359 | 51642522 |
| adr/ADR-002-core-foundation-shared-library.md | 51544266 | 51642522 |
| adr/ADR-003-dual-schema-single-postgres.md | 48594986 | 51642522 |
| adr/ADR-004-wallet-balance-snapshot.md | 51184014 | 51642522 |
| adr/ADR-005-idempotency-key-strategy.md | 51642542 | 51642522 |
| adr/ADR-006-two-phase-deposit.md | 48594970 | 51642522 |
| adr/ADR-007-freeze-settle-async-outflow.md | 51544287 | 51642522 |
| adr/ADR-008-saga-compensation-no-2pc.md | 51609821 | 51642522 |
| adr/ADR-009-fee-ownership-orchestration.md | 45777090 | 51642522 |
| adr/ADR-010-transit-accounts-net-zero.md | 51184034 | 51642522 |
| adr/ADR-011-auth-identity-jwt-subject.md | 51184055 | 51642522 |
| adr/ADR-012-orchestration-integration-forbidden-rules.md | 50987432 | 51642522 |
| adr/ADR-013-outbox-at-least-once-messaging.md | 48595305 | 51642522 |
| adr/ADR-014-reconciliation-w5-report-only.md | 50987452 | 51642522 |
| adr/ADR-015-eod-settlement-independent-batch.md | 51184075 | 51642522 |
| adr/ADR-016-qr-pos-default-no-per-txn-wallet.md | 51609841 | 51642522 |
| adr/ADR-017-partial-batch-payroll-disbursement.md | 51544307 | 51642522 |
| adr/ADR-018-openapi-asyncapi-wire-truth.md | 51642563 | 51642522 |
| adr/ADR-019-vnd-single-currency-v1.md | 50987472 | 51642522 |
| adr/ADR-020-wallet-lanes-coa-control-mapping.md | 51642583 | 51642522 |
| adr/ADR-021-aging-jobs-async-pending.md | 51544328 | 51642522 |
| adr/ADR-022-mtls-bank-webhooks.md | 51609861 | 51642522 |
| adr/ADR-023-accounting-period-close.md | 51642603 | 51642522 |
| adr/ADR-024-deposit-wallet-credit-dual-path.md | 50987492 | 51642522 |
| adr/ADR-025-ibft-napas-clearing-1112.md | 51642623 | 51642522 |
| adr/ADR-026-wallet-never-reverses-accounting.md | 50987512 | 51642522 |
| adr/ADR-027-sync-payment-transfer-three-commits.md | 51642643 | 51642522 |
| adr/ADR-028-money-scale-four-half-up.md | 48267270 | 51642522 |
| adr/ADR-029-wallet-locked-rejects-mutation.md | 51642663 | 51642522 |
| adr/ADR-030-virtual-account-deposit-mapping.md | 51609881 | 51642522 |
| adr/ADR-031-sql-ledger-invariant-ci.md | 50987532 | 51642522 |
| adr/ADR-032-wallet-balance-monitoring.md | 51642683 | 51642522 |
| adr/ADR-033-bank-poll-t2-frozen-tmax.md | 50987552 | 51642522 |
| adr/ADR-034-locked-wallet-deposit-credit-reject.md | 51642703 | 51642522 |
| adr/ADR-035-rabbitmq-workers-not-temporal-v1.md | 47972375 | 51642522 |
| adr/ADR-036-accrual-basis-ledger-v1.md | 51642723 | 51642522 |
| adr/ADR-037-tigerbeetle-ledger-backing-store.md | 51544348 | 51642522 |
| adr/ADR-038-orchestrator-separate-service-gateway-seam.md | 51609901 | 51642522 |
| adr/ADR-039-no-synchronous-wallet-aggregate-row.md | 51544368 | 51642522 |
| adr/ADR-040-user-multi-pocket-wallets.md | 51642743 | 51642522 |
| adr/ADR-041-rabbitmq-orch-to-accounting-worker.md | 50987572 | 51642522 |

## Pages NOT mirrored here (existing content, managed elsewhere)

- `specs/011-architecture-overview/confluence-draft.md` → platform architecture overview
- Use case child pages (UC-1, UC-2, UC-6..UC-9) → not yet mirrored locally
