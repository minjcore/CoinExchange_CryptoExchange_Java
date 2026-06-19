# ADR-015: EOD settlement as independent batch — not inline with capture

| Field | Value |
|-------|-------|
| Status | Accepted |
| Date | 2026-06-08 |
| Source | [`core.foundation.md`](../core.foundation.md) §4, §16, [`core.wallet.md`](../core.wallet.md) §5.8 |
| Related | [ADR-010](ADR-010-transit-accounts-net-zero.md), [ADR-016](ADR-016-qr-pos-default-no-per-txn-wallet.md) |

---

## Decision

1. QR/POS **capture** posts per txn to **3500** / **2120** — not EOD inline.
2. **EOD batch** locks **2120** → **3800/3810/3820** → bank out ([`core.foundation.md`](../core.foundation.md) §16).
3. Idempotent per `(merchantId, settlementDate)`.
4. File mismatch → **block** entire settlement branch — no partial POSTED.
5. Bank out fail → amount stays **3810** — retry next cycle; never double-settle.
6. Optional `MERCHANT_SETTLE_CREDIT` wallet bulk — product flag (W-O2), after accounting EOD success.

---

## Acceptance criteria (AC-015)

| ID | Criterion |
|----|-----------|
| AC-015-01 | Capture does not run EOD lock in same request |
| AC-015-02 | EOD success clears 3800/3810/3820 |
| AC-015-03 | File total ≠ 2120 → block |
| AC-015-04 | Re-run same merchant+date idempotent |
| AC-015-05 | Bank fail retains 3810 for retry |

---

## Test cases (TC-015)

| ID | Expected | Maps to |
|----|----------|---------|
| TC-015-01 | EOD happy transits zero | EOD happy |
| TC-015-02 | File mismatch block | EOD file mismatch |
| TC-015-03 | Partial file no POSTED | EOD partial file |
| TC-015-04 | EOD-E bank retry 3810 | EOD-E extended |
| TC-015-05 | Optional MERCHANT_SETTLE_CREDIT | EOD-E12, QR EOD scenario |

---

## References

- [`design-v2/accounting.md`](../design-v2/accounting.md) — §22
- [`design-v2/orchestration.md`](../design-v2/orchestration.md) — §16.2, §17.3
