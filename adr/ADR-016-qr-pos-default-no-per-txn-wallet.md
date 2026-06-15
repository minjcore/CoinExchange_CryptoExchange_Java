# ADR-016: QR/POS default — ledger per capture, no per-txn wallet movement

| Field | Value |
|-------|-------|
| Status | Accepted |
| Date | 2026-06-08 |
| Source | [`core.wallet.md`](../core.wallet.md) §5.8, [`core.foundation.md`](../core.foundation.md) §12 |
| Related | [ADR-015](ADR-015-eod-settlement-independent-batch.md), [ADR-009](ADR-009-fee-ownership-orchestration.md) |

---

## Decision

1. **Default v1:** QR/POS capture → accounting POSTED only (**1113**, **2120**, **3500**); merchant `wallet.available` **unchanged**.
2. Merchant liability tracked on COA **2120** until EOD ([ADR-015](ADR-015-eod-settlement-independent-batch.md)).
3. **Optional product flag** `creditMerchantOnCapture`: orchestration may issue merchant wallet credit after POSTED — idempotent on capture ref.
4. MDR/fee lines from orchestration ([ADR-009](ADR-009-fee-ownership-orchestration.md)); revenue **4140**.

---

## Acceptance criteria (AC-016)

| ID | Criterion |
|----|-----------|
| AC-016-01 | Default capture: zero wallet_tx for merchant |
| AC-016-02 | Accounting POSTED 3500 net zero per capture |
| AC-016-03 | Optional credit gated on POSTED + product flag |
| AC-016-04 | Duplicate capture ref idempotent |

---

## Test cases (TC-016)

| ID | Expected | Maps to |
|----|----------|---------|
| TC-016-01 | QR capture POSTED wallet unchanged | QR/POS default scenario |
| TC-016-02 | QR-E01 acquirer happy 3500=0 | QR-E |
| TC-016-03 | Optional MERCHANT credit on flag | QR EOD optional |
| TC-016-04 | Duplicate qr ref idempotent | QR-E duplicate |

---

## References

- [`design-v2/wallet.md`](../design-v2/wallet.md) — §24
- [`design-v2/acceptance.md`](../design-v2/acceptance.md) — QR/POS, QR-E
