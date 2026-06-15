# Platform boundaries

## Two domains

| | `core.accounting` | `core.wallet` |
|---|-------------------|---------------|
| **Question** | What happened in the ledger? | How much can this member spend? |
| **Tables** | `coa_account`, `coa_trans`, `coa_trans_data` | `wallet`, `wallet_balance`, `wallet_tx` |
| **Schema** | `accounting` | `wallet` |
| **Aggregate liabilities** | COA 2110 / 2120 / 2130 | Per-member `available` + `frozen` |

## Why split wallet from ledger (rationale)

Root reason: **ledger needs to be correct & immutable for audit; wallet needs to be fast & read-your-write to prevent overspend** — conflicting requirements if forced into one table/model. The rest follows.

| # | Reason | Locked in |
|---|--------|-----------|
| 1 | **Two different questions** — "what happened in the ledger?" (audit truth) vs "how much can this member spend *now*?" (hot path) | this doc, table above |
| 2 | **Opposite consistency defaults** — ledger derives balance from immutable postings (`SUM`); wallet keeps a single-row snapshot for P95 < 100ms + read-your-write debit checks. Wallet is the *inverse default* of the ledger. | [ADR-001](../../adr/ADR-001-immutable-ledger.md), [ADR-004](../../adr/ADR-004-wallet-balance-snapshot.md) |
| 3 | **Hot path vs audit path** — wallet served at high QPS (every pay/transfer/withdraw checks funds); ledger is write-once audit. Different scale/perf profiles. | [ADR-004](../../adr/ADR-004-wallet-balance-snapshot.md) |
| 4 | **Fault isolation + independent scale** — two schemas now; split to two DBs later = datasource URL change only. One domain's load/outage doesn't block the other. | [ADR-003](../../adr/ADR-003-dual-schema-single-postgres.md) |
| 5 | **Accounting integrity / regulatory** — COA must be real double-entry, immutable, period-close, transit net-zero (bank/Napas/audit). Wallet is an operational cache, not the books → wallet never posts/reverses ledger. | [ADR-026](../../adr/ADR-026-wallet-never-reverses-accounting.md) |
| 6 | **Aggregate vs per-member** — aggregate liability = COA 2110/2120/2130; per-member slice = wallet; reconciled by W5. No synchronous wallet-side aggregate. | [ADR-020](../../adr/ADR-020-wallet-lanes-coa-control-mapping.md), [ADR-014](../../adr/ADR-014-reconciliation-w5-report-only.md), [ADR-039](../../adr/ADR-039-no-synchronous-wallet-aggregate-row.md) |
| 7 | **Clean bounded context (DDD)** — avoid confusing COA "account" with wallet "balance"; no god-module. Enforced by the hard rules below. | [ADR-002](../../adr/ADR-002-core-foundation-shared-library.md) |

Contrast: Blnk **merges** balance and ledger into one model (fine for an embedded ledger primitive); GtelPay **splits** them as a bank-grade payment core — see [`../../references/blnk-vs-gtelpay-comparison.md`](../../references/blnk-vs-gtelpay-comparison.md) §1, §5.

## Hard rules

1. **No cross-import** — `core.wallet` must not import `core.accounting` (and vice versa).
2. **No cross-schema JOIN** — no SQL joining `wallet_*` to `coa_*`.
3. **No cross-schema FK** — `wallet_tx.coa_trans_id` is correlation only, never enforced FK.
4. **Sync only via orchestration** — app-orchestration gọi mỗi domain qua **HTTP gateway** (`WalletGateway`, `LedgerGateway`); domains never call each other ([ADR-038](../../adr/ADR-038-orchestrator-separate-service-gateway-seam.md)).
5. **Accounting never mutates wallet** — wallet credit/debit is always orchestration → wallet.
6. **Wallet never posts ledger** — DR/CR is always orchestration → accounting.

## Who may call what

| Caller | Accounting | Wallet |
|--------|------------|--------|
| Orchestration | ✓ S2 HTTP client | ✓ wallet-internal HTTP client |
| API Gateway | ✗ (routes to orchestration) | ✗ |
| Partner HTTP | ✗ | ✗ |
| Accounting worker | ✓ (own module) | ✗ |
| Wallet worker | ✗ | ✓ (own module) |

## Storage (ADR-003)

One PostgreSQL instance, two schemas (`wallet`, `accounting`). Split to two DBs later = datasource URL change only.

## Foundation (shared lib)

Minimal: `ApiResponse`, `ErrorCode`, `PageRequest`, `PageResult`, `MoneyUtil`. No domain entities, no command DTOs in foundation v1. See [`foundation.md`](./foundation.md).

## Next in flow

| Step | Doc |
|------|-----|
| Terminology | [`terminology.md`](./terminology.md) |
| Data model (ERD, tables, indexes) | [`data-model.md`](./data-model.md) |
| Idempotency | [`idempotency.md`](./idempotency.md) |
| COA + 9 luồng DR/CR | [`../../spec/foundation.md`](../../spec/foundation.md) §6–16 |
| Domain modular | [`../accounting/`](../accounting/) · [`../wallet/`](../wallet/) |
| Luồng nghiệp vụ | [`../../spec/processes.md`](../../spec/processes.md) §3–11 |
