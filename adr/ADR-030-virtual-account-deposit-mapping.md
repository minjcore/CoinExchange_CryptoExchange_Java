# ADR-030: Virtual account maps bank deposit to memberId

| Field | Value |
|-------|-------|
| Status | Accepted |
| Date | 2026-06-08 |
| Source | [`core.sharedlib.md`](../core.sharedlib.md) §8.3, [`integration-surfaces.md`](../integration-surfaces.md) §4.1, [`references/moderntreasury-virtual-accounts.md`](../references/moderntreasury-virtual-accounts.md) |
| Related | [ADR-006](ADR-006-two-phase-deposit.md), [ADR-022](ADR-022-mtls-bank-webhooks.md) |

---

## Decision

1. Each user deposit VA (or sub-account number) maps to exactly one `memberId` + USER wallet lane.
2. Orchestration performs VA → `memberId` lookup on webhook — unmappable VA → hold PENDING for ops (no wallet credit).
3. Mapping table owned by **orchestration/Application** — not in `wallet_*` or `coa_*`.
4. Duplicate webhook same `businessRef` idempotent ([ADR-005](ADR-005-idempotency-key-strategy.md)).
5. Phase B confirm may validate amount against bank notification.

---

## Acceptance criteria (AC-030)

| ID | Criterion |
|----|-----------|
| AC-030-01 | Webhook VA resolves memberId before phase B |
| AC-030-02 | Unknown VA → no erroneous 2110 credit |
| AC-030-03 | One VA → one member (v1) |
| AC-030-04 | Mapping change does not retro-edit posted journals |

---

## Test cases (TC-030)

| ID | Expected | Maps to |
|----|----------|---------|
| TC-030-01 | Mapped VA deposit credits correct user | Deposit happy |
| TC-030-02 | Unknown VA manual queue | orchestration §11 |
| TC-030-03 | Wrong member mapping prevented | DEP-E wrong member |
| TC-030-04 | Duplicate webhook same ref | Deposit idempotent |

---

## References

- [`design-v2/orchestration.md`](../design-v2/orchestration.md) — §11.2 S2 map VA
