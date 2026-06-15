# Messaging

## Split by purpose

| Transport | File | Content |
|-----------|------|---------|
| **RabbitMQ commands** | [`commands.yaml`](./commands.yaml) | Do work: `BANK_DEPOSIT`, `WALLET_CREDIT`, `WITHDRAW_PAYOUT` |
| **Kafka accounting** | [`../accounting/events.yaml`](../accounting/events.yaml) | Fact: `JournalPosted` |
| **Kafka wallet** | [`../wallet/events.yaml`](../wallet/events.yaml) | Fact: `WalletCredited` |
| **Kafka ingress/ops** | [`ingress.yaml`](./ingress.yaml) | `BankDepositReceived`, `CommandFailed` |

## Rules

1. **Never** put a command on Kafka.
2. **Never** duplicate `WALLET_CREDIT` on Kafka and RabbitMQ — command lives on RabbitMQ only.
3. Envelope `businessRef` on RabbitMQ full body ([`../platform/idempotency.md`](../platform/idempotency.md)).
4. Consumer ACK after DB commit; idempotent on `(commandType, businessRef)`.

## Who publishes / consumes

| Message | Publisher | Consumer |
|---------|-----------|----------|
| `BANK_DEPOSIT` | Orchestration | Accounting worker |
| `WALLET_CREDIT` | Orchestration | Wallet worker |
| `WITHDRAW_PAYOUT` | Orchestration | Payout worker |
| `JournalPosted` | Accounting adapter | Orchestration |
| `WalletCredited` | Wallet adapter | Notify/analytics |
| `CommandFailed` | Workers | Ops |
