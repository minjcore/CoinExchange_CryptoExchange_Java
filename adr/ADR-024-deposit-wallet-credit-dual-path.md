# ADR-024: Deposit wallet credit — event or command path

| Field | Value |
|-------|-------|
| Status | Accepted |
| Date | 2026-06-08 |
| Source | [`core.wallet.md`](../core.wallet.md) §7.3, [`integration-surfaces.md`](../integration-surfaces.md) §4.1 |
| Related | [ADR-006](ADR-006-two-phase-deposit.md), [ADR-013](ADR-013-outbox-at-least-once-messaging.md) |

---

## Decision

After deposit POSTED, wallet credit may arrive via **either** path (orchestration chooses one per deployment):

| Path | Transport | Trigger |
|------|-----------|---------|
| A | Kafka `core.accounting.journal-posted` | Consumer filters `use_case=DEPOSIT`, POSTED |
| B | RabbitMQ `core.commands.wallet-credit` | Explicit `WALLET_CREDIT` command |
| C | Sync HTTP | Orchestration `WalletGateway` → `wallet-internal` `credit()` |

**All paths:**

1. Gate: `coa_trans.status = POSTED` verified before credit.
2. Same `businessRef` idempotency ([ADR-005](ADR-005-idempotency-key-strategy.md)).
3. Amount = **net** from orchestration ([ADR-009](ADR-009-fee-ownership-orchestration.md)).
4. `coaTransId` correlation optional on `wallet_tx` — not idempotency key.

Wallet module does not prefer one path over another — orchestration owns wiring.

---

## Acceptance criteria (AC-024)

| ID | Criterion |
|----|-----------|
| AC-024-01 | JournalPosted consumer credits only DEPOSIT POSTED |
| AC-024-02 | WALLET_CREDIT command same semantics as path A |
| AC-024-03 | Duplicate path A+B same ref → one credit |
| AC-024-04 | credit includes coaTransId when available |

---

## Test cases (TC-024)

| ID | Expected | Maps to |
|----|----------|---------|
| TC-024-01 | Event path credit after POSTED | Deposit happy |
| TC-024-02 | Command path same net | integration §4.1 step 5 |
| TC-024-03 | Replay consumer idempotent | TC-006-04 |
| TC-024-04 | POSTED lag then credit | X-E05/E06 |

---

## References

- [`spec/contracts/async-api/core-events.yaml`](../spec/contracts/async-api/core-events.yaml)
- [`spec/contracts/async-api/core-commands.yaml`](../spec/contracts/async-api/core-commands.yaml)
