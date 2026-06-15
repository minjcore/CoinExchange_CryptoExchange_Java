# Machine-readable integration specs

Use these with API Gateway, client SDK generation, and AsyncAPI tooling — not markdown alone.

| File | Audience |
|------|----------|
| [`gtelpay-public.yaml`](./gtelpay-public.yaml) | **paymentorches** — mobile, partner, Gateway |
| [`gtelpay-core-internal.yaml`](./gtelpay-core-internal.yaml) | **app-orchestration** — paymentorches → core inbound |
| [`accounting-internal.yaml`](./accounting-internal.yaml) | **app-orchestration** → accounting pod (`LedgerGateway`) |
| [`wallet-internal.yaml`](./wallet-internal.yaml) | **app-orchestration** → wallet pod (`WalletGateway`) |
| [`../asyncapi/core-events.yaml`](../asyncapi/core-events.yaml) | Kafka producers/consumers |
| [`../asyncapi/core-commands.yaml`](../asyncapi/core-commands.yaml) | RabbitMQ worker commands (full-body envelope) |
| [`../gateway/routes.example.yaml`](../gateway/routes.example.yaml) | Kong-style upstream → **paymentorches** (not core) |

## Validate (optional)

```bash
cd 10_core
npm install -g @redocly/cli   # once
redocly lint spec/contracts/openapi/gtelpay-public.yaml
redocly lint spec/contracts/openapi/gtelpay-core-internal.yaml
redocly lint spec/contracts/openapi/accounting-internal.yaml
redocly lint spec/contracts/openapi/wallet-internal.yaml
```

AsyncAPI: [https://www.asyncapi.com/tools/generator](https://www.asyncapi.com/tools/generator)

Surface index: [`integration-surfaces.md`](../integration-surfaces.md) §1–§2 · Journals: [`foundation.md`](../foundation.md) §8–16 · Wallet: [`trd/wallet.md`](../trd/wallet.md)
