# GtelPay Core — Design (modular)

**Status:** Draft · **Last updated:** 2026-06-08

Tách theo domain — file mỏng, đọc cạnh YAML trong từng folder. **Behavior đầy đủ (Part I + II):** [`../design-v2/`](../design-v2/).

```
                    ┌─────────────────────┐
                    │  app-orchestration  │
                    │  S1–S6 boundary     │
                    └──────────┬──────────┘
              ┌────────────────┴────────────────┐
              │ HTTP                    HTTP    │
              ▼                                 ▼
    ┌──────────────────┐              ┌──────────────────┐
    │ core.accounting  │              │    core.wallet   │
    │  pod · S2        │              │  pod · internal  │
    └──────────────────┘              └──────────────────┘
```

## Đọc theo flow

### Phase 1 — Platform (đọc trước domain)

| # | Folder / file | Nội dung |
|---|---------------|----------|
| 1 | [`platform/boundaries.md`](platform/boundaries.md) | Hai domain, hard rules |
| 2 | [`platform/terminology.md`](platform/terminology.md) | Naming |
| 3 | [`platform/idempotency.md`](platform/idempotency.md) | `business_ref` end-to-end |
| 4 | [`platform/foundation.md`](platform/foundation.md) | Shared lib scope |

Binding chi tiết: [`../spec/foundation.md`](../spec/foundation.md).

### Phase 2 — Domains (song song)

| # | Folder | Docs | YAML |
|---|--------|------|------|
| 5a | [`accounting/`](accounting/) | model, coa, postings, **surface-map** | `api-internal.yaml`, `events.yaml` |
| 5b | [`wallet/`](wallet/) | model, operations, **surface-map** | `events.yaml` |

Monolith: [`../design-v2/accounting.md`](../design-v2/accounting.md) · [`../design-v2/wallet.md`](../design-v2/wallet.md).

### Phase 3 — Luồng nghiệp vụ

**Runtime:** Kiến trúc = tách pod + HTTP ([ADR-038](../adr/ADR-038-orchestrator-separate-service-gateway-seam.md), [`integration-surfaces.md`](../spec/integration-surfaces.md) §2.1). Code monolith cùng JVM = **gap migration** — không dùng làm diagram tham chiếu.

| # | Folder / file | Nội dung |
|---|---------------|----------|
| 6 | [`orchestration/flows.md`](orchestration/flows.md) | Step order per use case |
| 7 | [`orchestration/failures.md`](orchestration/failures.md) | Saga, compensation |
| 8 | [`../spec/processes.md`](../spec/processes.md) §3–11 | DR/CR + wallet per flow |

| Use case | flows.md | processes.md |
|----------|----------|--------------|
| Deposit | ✓ | §3 |
| Withdraw | ✓ | §4 |
| Payment | ✓ | §5 |
| Transfer | ✓ | §6 |
| IBFT | ✓ | §7 |
| QR/POS | — | §8 |
| Payroll | — | §9 |
| Disbursement | — | §10 |
| EOD | — | §11 |

### Phase 4 — Messaging & wire

| # | Folder | Nội dung |
|---|--------|----------|
| 9 | [`messaging/`](messaging/) | RabbitMQ commands, ingress |
| 10 | [`orchestration/http-public.yaml`](orchestration/http-public.yaml) | S1 public API |
| 11 | [`../spec/integration-surfaces.md`](../spec/integration-surfaces.md) | Surface index S1–S6 |

### Phase 5 — Conformance & decisions

| # | Đi tới | Nội dung |
|---|--------|----------|
| 12 | [`../design-v2/acceptance.md`](../design-v2/acceptance.md) | Gherkin — **150** gate / 207 full |
| 13 | [`../adr/`](../adr/) | 36 ADR (snapshot 5: [`adr/`](adr/)) |

## Wire layout

| Transport | Canonical YAML | Copy trong `design/` |
|-----------|----------------|----------------------|
| HTTP public | [`../spec/contracts/open-api/gtelpay-public.yaml`](../spec/contracts/open-api/gtelpay-public.yaml) | `orchestration/http-public.yaml` |
| HTTP internal | `spec/contracts/open-api/accounting-internal.yaml` | `accounting/api-internal.yaml` |
| Kafka | `spec/contracts/async-api/core-events.yaml` | `accounting/events.yaml`, `wallet/events.yaml` |
| RabbitMQ | `spec/contracts/async-api/core-commands.yaml` | `messaging/commands.yaml` |

**Rule:** `spec/contracts/` là wire truth; bản trong `design/` để đọc cạnh domain doc.

## Spec & archive

- Binding docs: [`../spec/`](../spec/)
- Legacy snapshot: [`../_archive/pre-2026-06/`](../_archive/pre-2026-06/)
