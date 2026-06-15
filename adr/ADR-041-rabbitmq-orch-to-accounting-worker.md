# ADR-041: Deposit journal via RabbitMQ — app-orchestration → accounting worker

| Field | Value |
|-------|-------|
| Status | Accepted |
| Date | 2026-06-12 |
| Source | [`spec/integration-surfaces.md`](../spec/integration-surfaces.md) §5.1, §7 · [`spec/contracts/asyncapi/core-commands.yaml`](../spec/contracts/asyncapi/core-commands.yaml) |
| Related | [ADR-006](ADR-006-two-phase-deposit.md), [ADR-013](ADR-013-outbox-at-least-once-messaging.md), [ADR-035](ADR-035-rabbitmq-workers-not-temporal-v1.md), [ADR-005](ADR-005-idempotency-key-strategy.md), [ADR-038](ADR-038-orchestrator-separate-service-gateway-seam.md) |

---

## Context

Nạp tiền là luồng **bất đồng bộ** ([ADR-006](ADR-006-two-phase-deposit.md)): app-orchestration trả **202** rồi mới ghi sổ. Wallet và accounting tách pod ([ADR-038](ADR-038-orchestrator-separate-service-gateway-seam.md)) — orchestration **không** INSERT trực tiếp `coa_*` ([ADR-012](ADR-012-orchestration-integration-forbidden-rules.md)).

Cần chốt **đường gọi** từ app-orchestration sang accounting cho phase A/B deposit: queue RabbitMQ, ai publish, ai consume, và khi nào được gọi HTTP sync thay thế.

---

## Decision

1. **Publisher duy nhất (v1 deposit):** **app-orchestration** publish lệnh `BANK_DEPOSIT` lên exchange `core.commands` (queue `core.commands.bank-deposit`) sau khi nhận notify deposit từ paymentorches và trả **202** + `businessRef`.

2. **Consumer:** **accounting worker** (process chạy cùng module/pod accounting, không phải app-orchestration). Worker đọc message → gọi API/domain accounting (`JournalService` / `confirmDeposit`) — **không** SQL từ orch.

3. **Không phải HTTP trực tiếp orch → accounting pod** trên luồng nạp tiền khuyến nghị — RabbitMQ tách thời gian (webhook nhanh, ghi sổ nền). HTTP sync `LedgerGateway` vẫn dùng cho **thanh toán / chuyển** ([ADR-027](ADR-027-sync-payment-transfer-three-commits.md)); deposit **ưu tiên** queue ([`integration-surfaces.md`](../spec/integration-surfaces.md) §5).

4. **Wire contract:** [`asyncapi/core-commands.yaml`](../spec/contracts/asyncapi/core-commands.yaml) — envelope full-body; `businessRef` = idempotency ([ADR-005](ADR-005-idempotency-key-strategy.md), F6 [ADR-012](ADR-012-orchestration-integration-forbidden-rules.md)).

5. **Publish an toàn:** ghi outbox trong cùng transaction với bước saga (nếu có) rồi relay publish **at-least-once** ([ADR-013](ADR-013-outbox-at-least-once-messaging.md)). Consumer idempotent trên `(commandType, businessRef)`.

6. **Sau POSTED:** cộng ví qua path riêng ([ADR-024](ADR-024-deposit-wallet-credit-dual-path.md)) — Kafka `JournalPosted` hoặc RabbitMQ `WALLET_CREDIT` hoặc HTTP sync; không gộp vào message `BANK_DEPOSIT`.

7. **v1 engine:** RabbitMQ worker + DB saga state — không Temporal ([ADR-035](ADR-035-rabbitmq-workers-not-temporal-v1.md)).

---

## Luồng (tóm tắt)

```
paymentorches ──HTTP──► app-orchestration (202)
                              │
                              │ publish BANK_DEPOSIT (outbox → RabbitMQ)
                              ▼
                        accounting worker
                              │
                              │ domain call (PENDING → confirmDeposit → POSTED)
                              ▼
                        accounting DB (coa_*)
                              │
                              └──► (tuỳ chọn) Kafka JournalPosted
                                   hoặc orch publish WALLET_CREDIT → wallet worker
```

---

## Consequences

### Positive

- Webhook/notify không block trên ghi sổ.
- Accounting scale/retry độc lập API pod orch.
- Cùng pattern với payout worker (`WITHDRAW_PAYOUT`).

### Trade-offs

- At-least-once → bắt buộc idempotency worker.
- Quan sát ops cần DLQ + correlation log.

---

## Acceptance criteria (AC-041)

| ID | Criterion |
|----|-----------|
| AC-041-01 | Chỉ app-orchestration publish `BANK_DEPOSIT` — accounting/wallet không publish lệnh này |
| AC-041-02 | Accounting worker ACK sau commit journal (hoặc no-op idempotent) |
| AC-041-03 | Replay cùng `businessRef` → một journal deposit |
| AC-041-04 | Phase A/B tuân [ADR-006](ADR-006-two-phase-deposit.md) — worker không credit wallet |
| AC-041-05 | Message validate schema `core-commands.yaml` |
| AC-041-06 | Publish qua outbox hoặc tương đương — không mất lệnh sau crash orch |

---

## Test cases (TC-041)

| ID | Expected | Maps to |
|----|----------|---------|
| TC-041-01 | notify 202 → một message `BANK_DEPOSIT` | TC-006-06 |
| TC-041-02 | Duplicate delivery → một PENDING/POSTED | TC-013-02, TC-005-09 |
| TC-041-03 | Worker crash mid-handle → redelivery safe | TC-035-01 |
| TC-041-04 | Poison → DLQ `core.commands.dlq` | TC-013-03 |

---

## References

- [`spec/integration-surfaces.md`](../spec/integration-surfaces.md) — §5.1, §7
- [`spec/contracts/asyncapi/core-commands.yaml`](../spec/contracts/asyncapi/core-commands.yaml)
- [`spec/contracts/openapi/accounting-internal.yaml`](../spec/contracts/openapi/accounting-internal.yaml) — sync path (non-deposit)
