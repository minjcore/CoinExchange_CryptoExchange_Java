# ADR → Confluence Page Map

Each local ADR file maps 1:1 to one Confluence page.
cloudId: `nivc.atlassian.net` · space: `GtelPay`

| Local file | CF page ID | CF URL |
|------------|-----------|--------|
| ADR-001-immutable-ledger.md | 47972359 | /pages/47972359 |
| ADR-002-core-sharedlib.md | 51544266 | /pages/51544266 |
| ADR-003-dual-schema-single-postgres.md | 48594986 | /pages/48594986 |
| ADR-004-wallet-balance-snapshot.md | 51184014 | /pages/51184014 |
| ADR-005-idempotency-key-strategy.md | 51642542 | /pages/51642542 |
| ADR-006-two-phase-deposit.md | 48594970 | /pages/48594970 |
| ADR-007-freeze-settle-async-outflow.md | 51544287 | /pages/51544287 |
| ADR-008-saga-compensation-no-2pc.md | 51609821 | /pages/51609821 |
| ADR-009-fee-ownership-orchestration.md | 45777090 | /pages/45777090 |
| ADR-010-transit-accounts-net-zero.md | 51184034 | /pages/51184034 |
| ADR-011-auth-identity-jwt-subject.md | 51184055 | /pages/51184055 |
| ADR-012-orchestration-integration-forbidden-rules.md | 50987432 | /pages/50987432 |
| ADR-013-outbox-at-least-once-messaging.md | 48595305 | /pages/48595305 |
| ADR-014-reconciliation-w5-report-only.md | 50987452 | /pages/50987452 |
| ADR-015-eod-settlement-independent-batch.md | 51184075 | /pages/51184075 |
| ADR-016-qr-pos-default-no-per-txn-wallet.md | 51609841 | /pages/51609841 |
| ADR-017-partial-batch-payroll-disbursement.md | 51544307 | /pages/51544307 |
| ADR-018-openapi-asyncapi-wire-truth.md | 51642563 | /pages/51642563 |
| ADR-019-vnd-single-currency-v1.md | 50987472 | /pages/50987472 |
| ADR-020-wallet-lanes-coa-control-mapping.md | 51642583 | /pages/51642583 |
| ADR-021-aging-jobs-async-pending.md | 51544328 | /pages/51544328 |
| ADR-022-mtls-bank-webhooks.md | 51609861 | /pages/51609861 |
| ADR-023-accounting-period-close.md | 51642603 | /pages/51642603 |
| ADR-024-deposit-wallet-credit-dual-path.md | 50987492 | /pages/50987492 |
| ADR-025-ibft-napas-clearing-1112.md | 51642623 | /pages/51642623 |
| ADR-026-wallet-never-reverses-accounting.md | 50987512 | /pages/50987512 |
| ADR-027-sync-payment-transfer-three-commits.md | 51642643 | /pages/51642643 |
| ADR-028-money-scale-four-half-up.md | 48267270 | /pages/48267270 |
| ADR-029-wallet-locked-rejects-mutation.md | 51642663 | /pages/51642663 |
| ADR-030-virtual-account-deposit-mapping.md | 51609881 | /pages/51609881 |
| ADR-031-sql-ledger-invariant-ci.md | 50987532 | /pages/50987532 |
| ADR-032-wallet-balance-monitoring.md | 51642683 | /pages/51642683 |
| ADR-033-bank-poll-t2-frozen-tmax.md | 50987552 | /pages/50987552 |
| ADR-034-locked-wallet-deposit-credit-reject.md | 51642703 | /pages/51642703 |
| ADR-035-rabbitmq-workers-not-temporal-v1.md | 47972375 | /pages/47972375 |
| ADR-036-accrual-basis-ledger-v1.md | 51642723 | /pages/51642723 |
| ADR-037-tigerbeetle-ledger-backing-store.md | 51544348 | /pages/51544348 |
| ADR-038-orchestrator-separate-service-gateway-seam.md | 51609901 | /pages/51609901 |
| ADR-039-no-synchronous-wallet-aggregate-row.md | 51544368 | /pages/51544368 |
| ADR-040-user-multi-pocket-wallets.md | 51642743 | /pages/51642743 |
| ADR-041-rabbitmq-orch-to-accounting-worker.md | 50987572 | /pages/50987572 |

## Deprecated CF pages (no local equivalent — orphans)

These 3 pages are superseded by canonical pages above. Content is stale.

| CF page ID | Title | Superseded by |
|-----------|-------|---------------|
| 48005121 | ADR-002 Decision Summary: core.sharedlib... | 51544266 |
| 48300046 | ADR-004 Wallet Balance Snapshot over Append-Only wallet_tx | 51184014 |
| 48627732 | ADR-005: Idempotency Key Strategy Across Surfaces | 51642542 |
