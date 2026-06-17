# spec — Binding documents

Canonical **requirements, TRD, processes, wire index**. Behavior detail: [`design/`](../design/) (modular) và [`design-v2/`](../design-v2/) (behavior monolith docs).

## Đọc theo flow

### 0. Tổng quan kiến trúc

| # | File | Nội dung |
|---|------|----------|
| 0 | [`architecture-overview.md`](architecture-overview.md) | **Entry point** — orchestration wrap cores, S1–S6, sync/async, diagrams |
| 0b | [`correlation-id-map.md`](correlation-id-map.md) | Identity: `businessRef` / `reference_id` / trace keys |

### 1. Nền tảng

| # | File | Nội dung |
|---|------|----------|
| 1 | [`foundation.md`](foundation.md) Part I | Shared lib, boundaries, invariants |
| 2 | [`terminology.md`](terminology.md) | Naming quick lookup |
| 3 | [`foundation.md`](foundation.md) §6–16 | COA + DR/CR 9 luồng |

### 2. Domain TRD

| # | File | Nội dung |
|---|------|----------|
| 4 | [`trd/accounting.md`](trd/accounting.md) | Accounting FR/NFR |
| 5 | [`trd/wallet.md`](trd/wallet.md) | Wallet FR, `tx_type` |

### 3. Luồng nghiệp vụ (đọc tuần tự)

[`processes.md`](processes.md) — end-to-end, saga, failure, fee, auth.

| § | Use case | Sync? |
|---|----------|-------|
| §3 | Deposit | Async → 202 |
| §4 | Withdraw | Wallet sync + bank async |
| §5 | Payment | Sync → 200 |
| §6 | Internal transfer | Sync → 200 |
| §7 | IBFT | Wallet sync + bank async |
| §8 | QR/POS | Acquirer + EOD |
| §9 | Payroll | Async batch |
| §10 | Disbursement | Async batch |
| §11 | EOD settlement | Batch |
| §13–17 | Saga, edge, reliability, fee, auth | Cross-cutting |

Step order ngắn: [`design/orchestration/flows.md`](../design/orchestration/flows.md).

### 4. Wire

| # | File | Nội dung |
|---|------|----------|
| 6 | [`integration-surfaces.md`](integration-surfaces.md) | S1–S6 index, forbidden rules |
| 7 | [`contracts/`](contracts/) | OpenAPI + AsyncAPI YAML |

### 5. Implement & verify

| # | File | Nội dung |
|---|------|----------|
| 8 | [`implementation.md`](implementation.md) | Repo, D1–D5, DDL, P0–P6 |
| 9 | [`../design-v2/acceptance.md`](../design-v2/acceptance.md) | Gherkin — **150** release gate / 207 full |
| 10 | [`../adr/README.md`](../adr/README.md) | 36 ADR |

## Layout

| Path | Was (root) | Role |
|------|------------|------|
| [`foundation.md`](foundation.md) | `core.foundation.md` | Shared lib + accounting fund flow |
| [`trd/accounting.md`](trd/accounting.md) | `core.accounting.trd.md` | Accounting service TRD |
| [`trd/wallet.md`](trd/wallet.md) | `core.wallet.md` | Wallet design / TRD |
| [`processes.md`](processes.md) | `core.business-processes.md` | End-to-end flows, saga, failure |
| [`acceptance-specs-legacy.md`](acceptance-specs-legacy.md) | `core.acceptance-specs.md` | Legacy Gherkin (pre–design-v2) |
| [`implementation.md`](implementation.md) | `IMPLEMENTATION.md` | Repo layout, D1–D5, build order |
| [`integration-surfaces.md`](integration-surfaces.md) | `integration-surfaces.md` | HTTP/Kafka/RabbitMQ/Gateway index |
| [`terminology.md`](terminology.md) | `TERMINOLOGY.md` | Naming quick lookup |
| [`contracts/`](contracts/) | `spec/contracts/open-api/`, `spec/contracts/async-api/`, `spec/contracts/gateway/` | Wire truth (YAML) |

Root filenames (`core.foundation.md`, …) là **redirect stubs** — nội dung canonical nằm ở đây.
