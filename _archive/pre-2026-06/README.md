# 10_core

Design docs for architecture and integration; **TRDs** describe service requirements before implementation. Edit history: `git log`.

**Design**

- [`design/`](./design/) — split by domain (accounting / wallet / orchestration / messaging / platform)
- [`design-v2/`](./design-v2/) — compact 2-domain redesign (`accounting.md`, `wallet.md`, …)
- `TERMINOLOGY.md` — journal / journal_entry vs TRD names (quick lookup)
- `core.foundation.md` — Part I: `core.foundation` shared lib · Part II: accounting fund flow (`coa_*`, §8–16)
- `core.wallet.md` — wallet design (`wallet_*`; boundaries → foundation)
- `core.business-processes.md` — end-to-end flows, failure/compensation (design-level)
- `core.acceptance-specs.md` — Given/When/Then conformance scenarios
- `integration-surfaces.md` — HTTP/Kafka/RabbitMQ/Gateway contract index (not domain schema)
- `openapi/` — **gtelpay-public.yaml**, **accounting-internal.yaml** (Gateway / partners / BFF; see [`openapi/README.md`](openapi/README.md))
- `asyncapi/core-events.yaml` — Kafka topics + payloads
- `asyncapi/core-commands.yaml` — RabbitMQ worker commands + full-body envelope
- `gateway/routes.example.yaml` — example API Gateway routes
- `adr/` — ADRs: [001](adr/ADR-001-immutable-ledger.md) ledger, [002](adr/ADR-002-core-foundation-shared-library.md) shared lib = `core.foundation`
- `00_framework.design.md` — legacy repo reference, outside `10_core` contracts
- [`_legacy/`](./_legacy/) — snapshot index (pre-`design/` move); canonical files remain at root

**TRD (Technical Requirements Document)**

- `core.accounting.trd.md` — Accounting Service (draft 1.0)

**Implementation**

- [`IMPLEMENTATION.md`](./IMPLEMENTATION.md) — repo layout, locked decisions D1–D5, build order P0–P6, service API signatures

Quick links: [`core.wallet.md`](core.wallet.md) · [`integration-surfaces.md`](integration-surfaces.md) · [`core.foundation.md`](core.foundation.md) Part I · accounting flow Part II §8–16.

**Tooling**

- `md2pdf.sh` — export PDF (not described inside `.md` files)

```bash
chmod +x md2pdf.sh
./md2pdf.sh core.foundation.md
./md2pdf.sh core.wallet.md
./md2pdf.sh core.accounting.trd.md
./md2pdf.sh integration-surfaces.md
./md2pdf.sh core.business-processes.md
./md2pdf.sh core.acceptance-specs.md
./md2pdf.sh IMPLEMENTATION.md
```
