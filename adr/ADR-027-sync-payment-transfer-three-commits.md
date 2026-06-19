# ADR-027: Sync payment and transfer — three independent commits

| Field | Value |
|-------|-------|
| Status | Accepted |
| Date | 2026-06-08 |
| Source | [`IMPLEMENTATION.md`](../IMPLEMENTATION.md) §8, [`integration-surfaces.md`](../integration-surfaces.md) §4.2 |
| Related | [ADR-008](ADR-008-saga-compensation-no-2pc.md), [ADR-005](ADR-005-idempotency-key-strategy.md) |

---

## Decision

**Payment** and **transfer** S1 return **200** only after orchestration completes:

| Step | Commit |
|------|--------|
| 1 | Wallet debit (USER or A) |
| 2 | Accounting POSTED (3500/3300 = 0) |
| 3 | Wallet credit (MERCHANT or B) |

- Three **separate** local transactions — no XA ([ADR-008](ADR-008-saga-compensation-no-2pc.md)).
- Client retry same `X-Idempotency-Key` → idempotent **200** if all done.
- Partial failure policies per flow §13.2–13.3 in business-processes.

---

## Acceptance criteria (AC-027)

| ID | Criterion |
|----|-----------|
| AC-027-01 | createPayment sync 200 not 202 |
| AC-027-02 | Three commits measurable in integration test |
| AC-027-03 | Post fail after debit → compensate user |
| AC-027-04 | Post OK credit fail → forward retry merchant/B |

---

## Test cases (TC-027)

| ID | Expected | Maps to |
|----|----------|---------|
| TC-027-01 | Payment happy 200 | Payment feature |
| TC-027-02 | Transfer happy 200 | Transfer |
| TC-027-03 | Post fails compensate | Payment compensate |
| TC-027-04 | TRF-E04 compensate A | Transfer extended |

---

## References

- [`design-v2/orchestration.md`](../design-v2/orchestration.md) — §12–13
