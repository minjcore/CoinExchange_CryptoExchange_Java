# Machine-readable integration specs

Use these with API Gateway, client SDK generation, and AsyncAPI tooling — not markdown alone.

| File | Audience |
|------|----------|
| [`gtelpay-public.yaml`](./gtelpay-public.yaml) | Mobile, BFF, partners, **public Gateway routes** |
| [`accounting-internal.yaml`](./accounting-internal.yaml) | Orchestration → accounting (internal); aligned with TRD §6 |
| [`../async-api/core-events.yaml`](../async-api/core-events.yaml) | Kafka producers/consumers |
| [`../async-api/core-commands.yaml`](../async-api/core-commands.yaml) | RabbitMQ worker commands (full-body envelope) |
| [`../gateway/routes.example.yaml`](../gateway/routes.example.yaml) | Kong-style upstream example |

## Validate (optional)

```bash
cd 10_core
npm install -g @redocly/cli   # once
redocly lint open-api/gtelpay-public.yaml
redocly lint open-api/accounting-internal.yaml
```

AsyncAPI: [https://www.asyncapi.com/tools/generator](https://www.asyncapi.com/tools/generator)

Surface index: [`integration-surfaces.md`](../integration-surfaces.md) · Journals: [`core.sharedlib.md`](../core.sharedlib.md) §8–16 · Wallet: [`core.wallet.md`](../core.wallet.md)
