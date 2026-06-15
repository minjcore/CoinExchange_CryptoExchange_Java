# Machine-readable integration specs

Use these with API Gateway, client SDK generation, and AsyncAPI tooling — not markdown alone.

| File | Audience |
|------|----------|
| [`gtelpay-public.yaml`](./gtelpay-public.yaml) | Mobile, BFF, partners, **public Gateway routes** |
| [`accounting-internal.yaml`](./accounting-internal.yaml) | Orchestration → accounting (internal); aligned with TRD §6 |
| [`../asyncapi/core-events.yaml`](../asyncapi/core-events.yaml) | Kafka producers/consumers |
| [`../asyncapi/core-commands.yaml`](../asyncapi/core-commands.yaml) | RabbitMQ worker commands (full-body envelope) |
| [`../gateway/routes.example.yaml`](../gateway/routes.example.yaml) | Kong-style upstream example |

## Validate (optional)

```bash
cd 10_core
npm install -g @redocly/cli   # once
redocly lint openapi/gtelpay-public.yaml
redocly lint openapi/accounting-internal.yaml
```

AsyncAPI: [https://www.asyncapi.com/tools/generator](https://www.asyncapi.com/tools/generator)

Surface index: [`integration-surfaces.md`](../integration-surfaces.md) · Journals: [`core.foundation.md`](../core.foundation.md) §8–16 · Wallet: [`core.wallet.md`](../core.wallet.md)
