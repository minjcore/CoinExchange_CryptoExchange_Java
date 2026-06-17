# GtelPay Core — System Design (consolidated)

One-page design of the whole `10_core` system: two domains, the orchestration layer, data model, the 9 flows, invariants, and contracts. Detail lives in `spec/`, `design-v2/`, `adr/`; this ties them together.

---

## 1. Components

```
External (mobile/partner/bank) ── HTTPS ──► API Gateway (S4)
                                              │
                                              ▼
                                   app-orchestration (BFF, thin)
                                   fee · auth · saga sequencing
                            WalletGateway │        │ LedgerGateway
                                          ▼        ▼
                               core.wallet     core.accounting
                               (wallet_*)        wrap: TB postings + PG COA/read-model
                                   └──── PostgreSQL (wallet + accounting metadata, ADR-003) ────┘
                                         TigerBeetle ← core.accounting adapter only (ADR-037)
                         async: RabbitMQ commands (S6) · Kafka events (S3)
```

| Component | Owns | Source of truth for |
|-----------|------|---------------------|
| `core.accounting` | Double-entry ledger, COA, transit, postings | Aggregate liabilities / fund flow — **TB postings** + **PG** master/report ([ADR-037](../adr/ADR-037-tigerbeetle-ledger-backing-store.md)) |
| `core.wallet` (`wallet_*`) | Per-member `available` / `frozen` | "How much can this member spend now?" |
| `app-orchestration` | Sequencing only (no domain logic) | Nothing — it coordinates |

Orchestrator is thin and is the **sole** caller of the domains; domains never call each other or share tables (ADR-012, ADR-002).

## 2. Data model

**Wallet** (`wallet`): `wallet(member,type,currency UNIQUE)` · `wallet_balance(available,frozen,version)` · `wallet_tx(tx_type,direction,amount,*_after,business_ref,coa_trans_id)` with `UNIQUE(wallet_id,business_ref,tx_type)`.

**Accounting** (`accounting`): `coa_account(code,type)` · `coa_trans(reference_id,use_case,status)` = journal · `coa_trans_data(coa_trans_id,account_code,DR/CR,amount)` = journal_entry. Immutable after POSTED (ADR-001); `UNIQUE(reference_id,use_case)`.

COA: assets 1111/1112/1113 · liabilities 2110/2120/2130 (user/merchant/partner) · transit 3100–3820 · revenue 41xx · expense 5100.

## 3. Flows (wallet legs × transit)

| # | Flow | Sync | Wallet legs | Transit |
|---|------|------|-------------|---------|
| 1 | Deposit | async 202 | `DEPOSIT_CREDIT` after POSTED | 3100 |
| 2 | Withdraw | wallet sync + bank async | `WITHDRAW_FREEZE` → `SETTLE`/`RELEASE` | 3200 |
| 3 | Payment | sync 200 | `PAYMENT_DEBIT` → `PAYMENT_CREDIT` | 3500 |
| 4 | Transfer | sync 200 | `TRANSFER_DEBIT` → `TRANSFER_CREDIT` | 3300 |
| 5 | IBFT | wallet sync + bank async | freeze → settle/release | 3400 |
| 6 | QR/POS | acquirer + EOD | none v1 | 3500→3800 |
| 7 | Payroll | batch | `PAYROLL_DEBIT` (gross, 1×) | 3600 |
| 8 | Disbursement | batch | `DISBURSEMENT_DEBIT` (per recipient) | 3700 |
| 9 | EOD settlement | batch | optional `MERCHANT_SETTLE_CREDIT` | 3800/3810/3820 |

Sync payment/transfer = **3 separate commits** (wallet debit → ledger POSTED → wallet credit), no 2PC (ADR-027/008).

## 4. Invariants

1. `Σ assets (1111+1112+1113) = Σ liabilities (2110+2120+2130)`.
2. Every transit account returns to **0** when a use case completes.
3. Per journal `sum(DR)=sum(CR)`; POSTED never edited (only reversing).
4. `available ≥ 0`, `frozen ≥ 0`; every balance change = exactly one `wallet_tx` in the same tx.

## 5. Cross-cutting

- **Idempotency:** `businessRef` end-to-end (S1 `X-Idempotency-Key` = `coa_trans.reference_id` = `wallet_tx.business_ref`). Replay → prior result, no second effect; concurrent same-triple → idempotent replay under the wallet row lock (ADR-005).
- **Saga:** forward-retry preferred; compensation when safe; POSTED ledger never auto-reversed by wallet (ADR-008/026). Aging jobs resolve every pending state (ADR-021).
- **Orchestrator boundary:** use cases depend on `WalletGateway`/`LedgerGateway` over **HTTP** (`wallet-internal.yaml`, S2 `accounting-internal.yaml`) per [ADR-038](../adr/ADR-038-orchestrator-separate-service-gateway-seam.md). In-process wiring in current code is migration debt only.
- **v1 scope:** VND only (ADR-019), accrual basis (ADR-036), wallet Postgres; accounting **hybrid TB + Postgres** (ADR-037).

## 6. Contracts (surfaces)

| ID | Transport | Spec |
|----|-----------|------|
| Public HTTPS | `spec/contracts/open-api/gtelpay-public.yaml` | **paymentorches** |
| Core inbound | `spec/contracts/open-api/gtelpay-core-internal.yaml` | **app-orchestration** |
| S2 | Accounting internal | `spec/contracts/open-api/accounting-internal.yaml` |
| — | Wallet internal | `spec/contracts/open-api/wallet-internal.yaml` |
| S3 | Kafka events | `spec/contracts/async-api/core-events.yaml` |
| S6 | RabbitMQ commands | `spec/contracts/async-api/core-commands.yaml` |

## 7. Decisions index

Full: [`adr/README.md`](../adr/README.md). Load-bearing: 001 immutable ledger · 003 dual-schema (wallet + accounting metadata) · 005 idempotency · 006 two-phase deposit · 007 freeze/settle · 008 saga no-2PC · 009 fee ownership · 012 orchestration-only · 017 partial batch · 027 sync 3-commit · 036 accrual · **037 accounting wraps TigerBeetle** · 038 orchestrator separate-service.
