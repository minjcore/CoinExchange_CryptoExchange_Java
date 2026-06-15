# ADR-028: Money scale 4 and HALF_UP rounding at orchestration boundary

| Field | Value |
|-------|-------|
| Status | Accepted |
| Date | 2026-06-08 |
| Source | [`core.foundation.md`](../core.foundation.md) §6, [`TERMINOLOGY.md`](../TERMINOLOGY.md) |
| Related | [ADR-019](ADR-019-vnd-single-currency-v1.md), [ADR-009](ADR-009-fee-ownership-orchestration.md) |

---

## Decision

1. Domain money: `BigDecimal` **scale 4**.
2. Wire: decimal string in OpenAPI (e.g. `"100000.0000"`).
3. Rounding **HALF_UP** at orchestration when computing fee/net/gross splits.
4. Wallet `DECIMAL(19,4)` columns; accounting lines same scale.
5. Reject amounts with scale > 4 or ≤ 0 at boundary (unless product allows zero fee).

---

## Acceptance criteria (AC-028)

| ID | Criterion |
|----|-----------|
| AC-028-01 | DDL wallet/accounting scale 4 |
| AC-028-02 | Fee split rounded once at orchestration |
| AC-028-03 | OpenAPI amount string format documented |
| AC-028-04 | Invalid scale rejected 400 |

---

## Test cases (TC-028)

| ID | Expected | Maps to |
|----|----------|---------|
| TC-028-01 | Deposit 100k fee 1k → 99k net | Deposit happy |
| TC-028-02 | DEP-E01 amount=1 | minimum unit |
| TC-028-03 | gross/net consistent payment | Payment scenarios |

---

## References

- [`core.foundation.md`](../core.foundation.md) — §6 Identity and money
