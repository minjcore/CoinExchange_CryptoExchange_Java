# Corpus kế toán GtelPay Core — mục tiêu 1000 trang in

**Ngôn ngữ:** Tiếng Việt · **Phạm vi:** `core.accounting` (`coa_*`) · **Không bao gồm:** `wallet_*`, HTTP public, tính phí

DR/CR mẫu số: [`../../spec/foundation.md`](../../spec/foundation.md) §8–16 (single source — **không copy bảng** vào từng quyển, chỉ tham chiếu + ví dụ).

Contract ngắn (English): [`../accounting.md`](../accounting.md).

---

## Đọc theo flow

| Bước | Quyển | File | ~Trang |
|------|-------|------|--------|
| 1 | I | [`vol-01-principles.md`](./vol-01-principles.md) | **~100** (2.994 dòng — 21 chương + phụ lục) |
| 2 | II | [`vol-02-coa-handbook.md`](./vol-02-coa-handbook.md) | **~100** (2.855 dòng — 23 TK + checklist/FAQ) |
| 3 | III | [`vol-03-deposit.md`](./vol-03-deposit.md) | **~100** (2.485 dòng — 2-phase, 33 Gherkin) |
| 4 | IV | [`vol-04-withdraw-ibft.md`](./vol-04-withdraw-ibft.md) | **~100** (2.600 dòng — Withdraw + IBFT) |
| 5 | V | [`vol-05-payment-transfer.md`](./vol-05-payment-transfer.md) | **~100** (2.572 dòng — Payment + Transfer sync) |
| 6 | VI | [`vol-06-qr-payroll-disburse.md`](./vol-06-qr-payroll-disburse.md) | **~100** (2.804 dòng — QR + Payroll + Disburse) |
| 7 | VII | `vol-07-eod-settlement.md` | 100 | *(chưa viết)* |
| 8 | VIII | `vol-08-reconciliation.md` | 100 | *(chưa viết)* |
| 9 | IX | `vol-09-period-close-ops.md` | 100 | *(chưa viết)* |
| 10 | X | `vol-10-api-implementation.md` | 100 | *(chưa viết)* |

**Đã viết:** Quyển I (2.994), II (2.855), III (2.485), IV (2.600), V (2.572), VI (2.804). Còn Quyển VII–X (~200 trang).

---

## Traceability

| Cần | Đọc |
|-----|------|
| 9 luồng narrative | [`../../spec/processes.md`](../../spec/processes.md) §3–11 |
| FR/NFR service | [`../../spec/trd/accounting.md`](../../spec/trd/accounting.md) |
| Gherkin 150 gate | [`../acceptance.md`](../acceptance.md) |
| ADR 001–036 | [`../../adr/README.md`](../../adr/README.md) |
| Luật KT / VAS refs | [`../../references/vn-luat-ke-toan-2015-dieu-5-7.md`](../../references/vn-luat-ke-toan-2015-dieu-5-7.md) |

---

## Quy tắc viết corpus

1. Mỗi quyển = **depth** (failure matrix, ví dụ số, recon) — không padding.
2. Số tiền ví dụ: principal **100.000**, phí **1.000**, chi phí Napas **500** (VND, scale 4 — ADR-028).
3. Chỗ thiếu input product → ghi `[TBD: …]` rõ ràng, không bịa fee tier.
4. Immutable POSTED — ADR-001; transit zero — ADR-010.
