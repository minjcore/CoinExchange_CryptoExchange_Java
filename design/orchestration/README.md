# Orchestration (Application / BFF)

**Not a domain.** **app-orchestration** = sole owner S1–S6; sequence wallet + accounting **qua HTTP gateway** ([ADR-038](../../adr/ADR-038-orchestrator-separate-service-gateway-seam.md)).

## Runtime

| | Kiến trúc (spec) |
|---|------------------|
| **Deploy** | 3 pod: `app-orchestration`, `core.wallet`, `core.accounting` |
| **Wallet** | `WalletGateway` → `wallet-internal.yaml` HTTP |
| **Accounting** | `LedgerGateway` → S2 `accounting-internal.yaml` HTTP |
| **S1** | Core inbound — caller: paymentorches |
| **Async** | S6 publish + S3 consume |

**Code gap:** repo IT vẫn `@Autowired` domain trong một JVM — vi phạm ADR-038; xem [`../spec/implementation.md`](../spec/implementation.md) §9.0.

Chi tiết: [`../spec/integration-surfaces.md`](../spec/integration-surfaces.md) §1.1, §2.1.

## Đọc theo flow

| # | File | Nội dung |
|---|------|----------|
| 0 | [`../spec/architecture-overview.md`](../spec/architecture-overview.md) | Sơ đồ tổng quan — ADR-038 split |
| 1 | [`flows.md`](./flows.md) | Step order — deposit, payment, transfer, withdraw, IBFT |
| 2 | [`../spec/processes.md`](../spec/processes.md) §3–11 | DR/CR + wallet effects per use case |
| 3 | [`failures.md`](./failures.md) | Saga, compensation, edge cases |
| 4 | [`../design-v2/orchestration.md`](../design-v2/orchestration.md) | Behavior đầy đủ (Part II) |
| 5 | [`http-public.yaml`](./http-public.yaml) | Public ref → paymentorches (`gtelpay-public`) |
| 6 | [`../spec/contracts/open-api/gtelpay-core-internal.yaml`](../spec/contracts/open-api/gtelpay-core-internal.yaml) | Core inbound — app-orchestration |

### Use case → doc map

| Use case | flows.md | processes.md | failures.md |
|----------|----------|--------------|-------------|
| Deposit | ✓ | §3 | §13.1 |
| Withdraw | ✓ | §4 | §13.4 |
| Payment | ✓ | §5 | §13.2 |
| Transfer | ✓ | §6 | §13.2 |
| IBFT | ✓ | §7 | §13.4 |
| QR/POS | — | §8 | §14 |
| Payroll | — | §9 | §13.6 |
| Disbursement | — | §10 | §13.6 |
| EOD | — | §11 | §15 |

## Must not

- INSERT into `wallet_*` or `coa_*` directly
- Import domain service classes — chỉ `WalletGateway` / `LedgerGateway`
- Import repositories across domains
