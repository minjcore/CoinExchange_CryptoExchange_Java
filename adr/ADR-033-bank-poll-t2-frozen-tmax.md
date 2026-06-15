# ADR-033: Bank poll interval T2 and frozen age Tmax (ops config contract)

| Field | Value |
|-------|-------|
| Status | Accepted |
| Date | 2026-06-08 |
| Source | [`design-v2/orchestration.md`](../design-v2/orchestration.md) §23, [`references/stripe-payment-settlement-explained.md`](../references/stripe-payment-settlement-explained.md), [ADR-007](ADR-007-freeze-settle-async-outflow.md), [ADR-021](ADR-021-aging-jobs-async-pending.md) |
| Related | [ADR-007](ADR-007-freeze-settle-async-outflow.md), [ADR-021](ADR-021-aging-jobs-async-pending.md), [ADR-022](ADR-022-mtls-bank-webhooks.md) |

---

## Decision

1. **T2** (poll interval for non-terminal bank status) and **Tmax** (max frozen age before high-severity alert) are **operations-owned** configuration — numeric values are **not** fixed in `10_core` until product publishes bank SLA.
2. Poll worker (`PAYOUT_STATUS_POLL`) reads T2 from config; **never RELEASE** on `UNKNOWN`/`TIMEOUT`/`504` ([ADR-007](ADR-007-freeze-settle-async-outflow.md)).
3. **Tmax** triggers ADR-021 aging alert — **manual resolution** only; no auto-release at Tmax.
4. Webhook terminal status **always preferred** over poll when both arrive — poll is backstop.
5. Config contract (names stable for implementers):

| Key | Type | Purpose |
|-----|------|---------|
| `bank.poll.interval_seconds` | int | T2 between poll attempts |
| `bank.poll.max_attempts` | int | Before DLQ (default 3 per orchestration §23) |
| `wallet.frozen.alert_age_hours` | int | Tmax for high-severity frozen alert |
| `bank.webhook.grace_seconds` | int | Wait before first poll if webhook expected |

6. IBFT and withdraw share the same config namespace with `use_case` discriminator.

---

## Acceptance criteria (AC-033)

| ID | Criterion |
|----|-----------|
| AC-033-01 | Poll respects T2 — no tight-loop hammering bank |
| AC-033-02 | UNKNOWN status never triggers RELEASE |
| AC-033-03 | Frozen age > Tmax emits high alert without RELEASE |
| AC-033-04 | Terminal SUCCESS triggers SETTLE once (idempotent) |
| AC-033-05 | Config reload without redeploy (if platform supports) |

---

## Test cases (TC-033)

| ID | Expected | Maps to |
|----|----------|---------|
| TC-033-01 | UNKNOWN poll — frozen unchanged | X-E13, Withdraw timeout |
| TC-033-02 | T2=300s — second poll not before 300s | Bank poll feature |
| TC-033-03 | Tmax exceeded — alert only | IBFT frozen 72h, ADR-021 |
| TC-033-04 | SUCCESS after TIMEOUT polls — SETTLE once | IBFT happy |
| TC-033-05 | max_attempts → DLQ | X-E07 pattern |

---

## References

- [`design-v2/orchestration.md`](../design-v2/orchestration.md) — §23, §25.7
- [`design-v2/acceptance.md`](../design-v2/acceptance.md) — Bank poll configuration feature
