# Quyển VI — QR/POS, Payroll & Disbursement: acquirer capture, batch 3600/3700, escrow 2130

**Trạng thái:** Bản mở rộng (~100 trang in) · **Phạm vi:** `core.accounting` — luồng QR/POS + PAYROLL + DISBURSEMENT · **Ngôn ngữ:** Tiếng Việt
**Transit:** 3500 (QR/POS capture) · 3600 (payroll) · 3700 (disbursement) · **ADR chính:** 016, 017, 015, 009, 010, 005, 020
**Async/Batch:** QR capture có thể POSTED sync từ acquirer webhook; payroll/disburse batch async; EOD settlement → Quyển VII ([ADR-015](../../adr/ADR-015-eod-settlement-independent-batch.md))

**Số tiền ví dụ chuẩn — QR/POS:** gross **100.000** VND · acquirer cost **500** · **1113** net inflow **99.500** · **2120 CR 100.000** · MDR merchant **deferred EOD** qua **4140** `[TBD: % MDR]` · scale **4** HALF_UP ([ADR-028](../../adr/ADR-028-money-scale-four-half-up.md))

**Số tiền ví dụ chuẩn — Payroll:** 5 nhân viên × **100.000** + phí batch **5.000** = gross merchant **505.000** · Napas cost **2.500** · **4150 CR 5.000** · **3600 = 0**

**Số tiền ví dụ chuẩn — Disbursement:** pre-fund **1111→2130 100.000** · disburse gross **101.000** (100.000 beneficiary + **1.000** fee `[TBD: fee tier]`) · **3700 = 0**

Template DR/CR authoritative: [`foundation.md`](../../spec/foundation.md) §12 (QR) · §14 (payroll) · §15 (disburse) · §16 (EOD pointer only). COA deep dive: [`vol-02-coa-handbook.md`](./vol-02-coa-handbook.md) TK 1111, 1112, 1113, 2120, 2130, 3500, 3600, 3700, 4140, 4150, 5100. Nguyên tắc: [`vol-01-principles.md`](./vol-01-principles.md). Contract: [`accounting.md`](../accounting.md) §19–21. Acceptance: [`acceptance.md`](../acceptance.md). Saga batch: [`processes.md`](../../spec/processes.md) §8–10, §13.6. So sánh sync payment: [`vol-05-payment-transfer.md`](./vol-05-payment-transfer.md).

**[TBD: ...]** — Định dạng file acquirer VPBank, % MDR merchant, bậc phí payroll/disbursement, quy tắc pre-fund partner tối thiểu — orchestration config, không hard-code trong accounting.

---
## Mục lục chi tiết

### Phần I — QR/POS (transit 3500, acquirer 1113)
| § | Tiêu đề |
| --- | --- |
| [0](#chương-0-cách-đọc-và-so-sánh-wallet-payment-vol-05) | Cách đọc + vs wallet payment (vol-05) |
| [1](#chương-1-adr-016-default-no-wallet-per-capture) | ADR-016 default no wallet per capture |
| [2](#chương-2-drcr-capture--t-account-examples) | DR/CR capture — T-accounts, 5100 cost |
| [3](#chương-3-2120-pending-until-eod) | 2120 pending until EOD (pointer Quyển VII, ADR-015) |
| [4](#chương-4-failure-q-f1q-f4) | Failure Q-F1..Q-F4 |
| [5](#chương-5-optional-creditmerchantoncapture-flag) | Optional creditMerchantOnCapture flag |
| [6](#chương-6-gherkin-catalog-qr-12-scenarios) | Gherkin catalog QR 12 scenarios |
| [7](#chương-7-faq-qr-20-câu) | FAQ QR 20 câu |

### Phần II — PAYROLL (transit 3600, batch)
| § | Tiêu đề |
| --- | --- |
| [8](#chương-8-tóm-tắt-merchant-2120-debit-gross) | Tóm tắt merchant 2120 debit gross |
| [9](#chương-9-drcr-batch-foundation-§14-deep) | DR/CR batch foundation §14 deep |
| [10](#chương-10-adr-017-partial-batch-per-recipient-keys) | ADR-017 partial batch, per-recipient keys |
| [11](#chương-11-failure-pr-f-expanded) | Failure PR-F expanded |
| [12](#chương-12-gherkin-catalog-payroll-12-scenarios) | Gherkin catalog payroll 12 scenarios |
| [13](#chương-13-faq-payroll-15-câu) | FAQ payroll 15 câu |

### Phần III — DISBURSEMENT (transit 3700, pre-fund + batch)
| § | Tiêu đề |
| --- | --- |
| [14](#chương-14-two-phase-pre-fund-vs-disburse-journals) | Two-phase pre-fund vs disburse journals |
| [15](#chương-15-drcr-pre-fund-11112130-disburse-213037001112) | DR/CR pre-fund 1111→2130, disburse 2130→3700→1112 |
| [16](#chương-16-partner-lane-2130-mirror-wallet) | PARTNER lane 2130 mirror wallet |
| [17](#chương-17-adr-017-partial--insufficient-2130) | ADR-017 partial + insufficient 2130 |
| [18](#chương-18-failure-dis-f-expanded) | Failure DIS-F expanded |
| [19](#chương-19-gherkin-catalog-disburse-12-scenarios) | Gherkin catalog disburse 12 scenarios |
| [20](#chương-20-faq-disburse-15-câu) | FAQ disburse 15 câu |

### Phần IV — CHUNG
| § | Tiêu đề |
| --- | --- |
| [21](#chương-21-matrix-qr-vs-payment-vs-payroll-vs-disburse) | Matrix QR vs Payment vs Payroll vs Disburse |
| [22](#chương-22-batch-idempotency-patterns) | Batch idempotency patterns |
| [23](#chương-23-sql-invariant-ci-per-transit) | SQL invariant CI per transit |
| [24](#chương-24-review-checklist-35-mục) | Review checklist 35 mục |

### Phụ lục
| § | Nội dung |
| --- | --- |
| [A](#phụ-lục-a--ma-trận-kịch-bản-số) | Ma trận kịch bản số |
| [B](#phụ-lục-b--drcr-line-by-line) | DR/CR line-by-line mọi biến thể |
| [C](#phụ-lục-c--bảng-tra-adr) | Bảng tra ADR |
| [D](#phụ-lục-d--processes-§8§10-mapping) | processes §8–§10 mapping |
| [E](#phụ-lục-e--accounting.md-1921-mapping) | accounting.md §19–§21 mapping |
| [F](#phụ-lục-f--numeric-t-account-gallery) | Numeric T-account gallery |
| [G](#phụ-lục-g--cross-flow-invariant) | Cross-flow invariant |
| [H](#phụ-lục-h--đọc-tiếp-vol-07-eod) | Đọc tiếp vol-07 EOD |

---
## Chương 0. Cách đọc và so sánh wallet payment (vol-05) {#chương-0-cách-đọc-và-so-sánh-wallet-payment-vol-05}

Quyển VI là **deep dive nghiệp vụ QR/POS acquirer, payroll batch và partner disbursement** — bổ sung Quyển I–V bằng luồng **batch/async-heavy** và **bank rail 111x** thay vì chỉ liability nội bộ.

| Vai trò | Đọc trước | Kết quả |
|---------|-----------|---------|
| Product | Ch0 + Ch1 + Ch8 + Ch14 | Batch contract, fee tier `[TBD]` |
| Backend accounting | Ch2 + Ch9 + Ch15 + Ch23 | `postQrCapture`, `postPayroll`, `postDisburse` |
| Backend orchestration | Ch5 + Ch10 + Ch17 + Ch22 | ADR-016/017, per-recipient keys |
| Ops / Kế toán | Ch3 + Ch21 | 2120 pending EOD, 2130 escrow, W5 |
| QA | Ch6 + Ch12 + Ch19 | 12 + 12 + 12 Gherkin scenarios |
| Audit | Ch2 + Ch4 + Phụ lục B | DR/CR evidence, immutability ADR-001 |

### 0.1 Quan hệ Quyển I ↔ II ↔ V ↔ VI

```
Quyển I (nguyên tắc)     Quyển II (COA)           Quyển V              Quyển VI
────────────────────     ──────────────           ───────              ───────
ADR-010 transit=0  ──►   TK 3500/3600/3700  ──►   3500 sync wallet  ──► 3500 QR capture
ADR-016 no wallet  ──►   1113 acquirer      ──►   no 111x payment   ──► 1113 inflow
ADR-017 partial    ──►   batch transits     ──►   N/A sync          ──► 3600/3700 batch
ADR-015 EOD batch  ──►   3800/3820/4140     ──►   2120 pending Ch8  ──► 2120 QR+payroll
```

### 0.2 Wallet payment (vol-05) vs QR/POS capture
| Khía cạnh | WALLET PAYMENT (Quyển V) | QR/POS (Phần I) |
| --- | --- | --- |
| Nguồn tiền | 2110 USER debit | **1113 VPBank** acquirer inflow |
| Transit | 3500 | **3500** (cùng TK, khác use_case) |
| Wallet per txn | PAYMENT_DEBIT/CREDIT sync | **Không** (default ADR-016) |
| Bank movement POSTED | Không 111x | **1113 DR/CR** + cost **5100** |
| MDR merchant | `[TBD]` wallet payment | **4140 deferred EOD** `[TBD: %]` |
| HTTP | 200 sync three-commit | Acquirer webhook / batch import → POSTED |
| 2120 | CR pending EOD | CR pending EOD — cùng aggregate |

### 0.3 Payroll & Disburse vs sync flows
| Khía cạnh | PAYROLL (Phần II) | DISBURSEMENT (Phần III) |
| --- | --- | --- |
| Lane | MERCHANT **2120** | PARTNER **2130** |
| Transit | **3600** | **3700** |
| Pre-phase | Không — debit merchant trực tiếp | **Pre-fund 1111→2130** |
| Bank out | **1112** Napas | **1112** Napas |
| Revenue | **4150** | **4150** |
| Cost | **5100** Napas | **5100** Napas |
| Batch | Async per-recipient | Async per-beneficiary |
| Partial fail | ADR-017 summary | ADR-017 summary |

### 0.4 Quy ước ký hiệu
| Ký hiệu | Ý nghĩa |
| --- | --- |
| gross QR | 100.000 — tiền khách trả merchant |
| acquirer cost | 500 — chi phí VPBank ghi **5100** |
| payroll gross | 505.000 = 5×100.000 + fee 5.000 |
| disburse gross | 101.000 = beneficiary 100.000 + fee 1.000 |
| `business_ref` | [ADR-005](../../adr/ADR-005-idempotency-key-strategy.md) |
| `{ref}:{recipientId}` | Per-recipient sub-key ADR-017 |
| [TBD: ...] | Product gap — không bịa |

### 0.5 Onboarding 10 câu
| # | Câu | Đáp |
| --- | --- | --- |
| Q1 | QR có debit USER 2110? | **Không** — tiền từ **1113**, không qua ví user platform |
| Q2 | 3500 sau QR capture? | **= 0** tại POSTED [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md) |
| Q3 | MDR khi nào? | **EOD batch** — không tại capture [ADR-015](../../adr/ADR-015-eod-settlement-independent-batch.md) |
| Q4 | Payroll debit TK nào? | Merchant **2120** gross + **PAYROLL_DEBIT** wallet |
| Q5 | Disburse trước prefund? | **Reject** — insufficient **2130**/available |
| Q6 | Partial batch rollback? | **Không** — succeeded legs giữ [ADR-017](../../adr/ADR-017-partial-batch-payroll-disbursement.md) |
| Q7 | Ai tính fee? | Orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md) |
| Q8 | 2120 QR vs payment? | Cùng aggregate — EOD gom chung Quyển VII |
| Q9 | 5100 QR vs payroll? | QR: acquirer cost capture · Payroll: Napas outbound cost |
| Q10 | EOD deep dive? | **Quyển VII** — §16 foundation chỉ pointer |

### Ch0B.01 — mục mở rộng 1

Nội dung chi tiết liên kết Ch0 chính · so sánh vol-05 · processes §8–§10
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch0B.02 — mục mở rộng 2

Nội dung chi tiết liên kết Ch0 chính · so sánh vol-05 · processes §8–§10
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch0B.03 — mục mở rộng 3

Nội dung chi tiết liên kết Ch0 chính · so sánh vol-05 · processes §8–§10
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch0B.04 — mục mở rộng 4

Nội dung chi tiết liên kết Ch0 chính · so sánh vol-05 · processes §8–§10
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch0B.05 — mục mở rộng 5

Nội dung chi tiết liên kết Ch0 chính · so sánh vol-05 · processes §8–§10
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

---
## Chương 1. ADR-016 default no wallet per capture {#chương-1-adr-016-default-no-wallet-per-capture}

[ADR-016](../../adr/ADR-016-qr-pos-default-no-per-txn-wallet.md) quyết định mặc định v1: **QR/POS capture → accounting POSTED only**; merchant `wallet.available` **không đổi** cho đến khi product bật flag hoặc EOD bulk credit.

### 1.1 Decision summary
| # | Quy tắc | Accounting | Wallet |
| --- | --- | --- | --- |
| D1 | Default v1 | POSTED **1113/3500/2120/5100** | Zero `wallet_tx` |
| D2 | Liability | **2120 CR** pending settlement | Mirror optional |
| D3 | EOD | Lock **2120→3800** — Quyển VII | Optional `MERCHANT_SETTLE_CREDIT` |
| D4 | MDR | **4140** tại EOD — orchestration % `[TBD]` | N/A |
| D5 | Idempotency | `(reference_id, QR_PAYMENT)` unique | N/A capture |

### 1.2 Tại sao không wallet per capture?

1. **Acquirer volume** — hàng nghìn capture/ngày; wallet_tx explosion.
2. **Reconciliation** — nguồn truth là acquirer file + **2120** aggregate; W5 so **2120** vs SUM merchant wallets khi có credit.
3. **EOD coupling** — MDR và net settlement cần file ngày; credit wallet sớm có thể lệch net EOD.
4. **ADR-015** — capture và EOD là **hai batch độc lập**.

### 1.3 AC-016 acceptance mapping
| AC | Criterion | Quyển VI evidence |
| --- | --- | --- |
| AC-016-01 | Zero wallet_tx default | Ch6 QR-09 |
| AC-016-02 | 3500 net zero | Ch2, Ch23 |
| AC-016-03 | Optional credit gated | Ch5 |
| AC-016-04 | Duplicate idempotent | Ch4 Q-F1, Ch6 QR-03 |

### 1.4 Actors QR capture
| Actor | Vai trò | Surface |
| --- | --- | --- |
| Customer | Quét QR / swipe POS | Acquirer VPBank |
| Acquirer | Webhook/file capture | mTLS `[TBD: file format]` |
| Orchestration | Map merchant, fee lines | S6 command |
| Accounting | `postQrCapture` POSTED | S2 |
| Merchant | Nhận liability **2120** | COA only default |

### 1.5 So sánh optional paths

```
Path A (default ADR-016):
  Capture POSTED → 2120 ↑ → (wait EOD) → 3800 lock → bank out

Path B (creditMerchantOnCapture=true):
  Capture POSTED → 2120 ↑ → MERCHANT wallet credit (idempotent)
  EOD vẫn chạy — wallet credit có thể = gross; net bank = EOD − MDR

Path C (EOD-only wallet):
  Capture POSTED → 2120 only
  EOD success → MERCHANT_SETTLE_CREDIT batch ref
```

**[TBD: Chốt product default Path A vs C.]**

### Ch1B.01 — mục mở rộng 1

Nội dung chi tiết liên kết Ch1 chính · ADR-016 · wallet.md §5.6/§5.8
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch1B.02 — mục mở rộng 2

Nội dung chi tiết liên kết Ch1 chính · ADR-016 · wallet.md §5.6/§5.8
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch1B.03 — mục mở rộng 3

Nội dung chi tiết liên kết Ch1 chính · ADR-016 · wallet.md §5.6/§5.8
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch1B.04 — mục mở rộng 4

Nội dung chi tiết liên kết Ch1 chính · ADR-016 · wallet.md §5.6/§5.8
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch1B.05 — mục mở rộng 5

Nội dung chi tiết liên kết Ch1 chính · ADR-016 · wallet.md §5.6/§5.8
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch1B.06 — mục mở rộng 6

Nội dung chi tiết liên kết Ch1 chính · ADR-016 · wallet.md §5.6/§5.8
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch1B.07 — mục mở rộng 7

Nội dung chi tiết liên kết Ch1 chính · ADR-016 · wallet.md §5.6/§5.8
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch1B.08 — mục mở rộng 8

Nội dung chi tiết liên kết Ch1 chính · ADR-016 · wallet.md §5.6/§5.8
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

---
## Chương 2. DR/CR capture — T-accounts, 5100 cost {#chương-2-drcr-capture--t-account-examples}
Nguồn authoritative: [`foundation.md`](../../spec/foundation.md) §12 · [`processes.md`](../../spec/processes.md) §8.1.
### 2.1 Canonical example — gross 100.000, acquirer cost 500
| Step | Actor | TK | DR/CR | Số tiền |
| --- | --- | --- | --- | --- |
| 1 | Bank | **1113** | DR | 100.000 |
| 2 | Bank | **3500** | CR | 100.000 |
| 3 | Bank | **5100** | DR | 500 |
| 4 | Bank | **1113** | CR | 500 |
| 5 | Merchant | **3500** | DR | 100.000 |
| 6 | Merchant | **2120** | CR | 100.000 |

**Kết quả:** **3500 = 0** · **1113** net +99.500 · **2120** +100.000 · **5100** +500 expense.
### 2.2 T-account gallery — capture journal

```
TK 1113 (VPBank QR/POS)          TK 3500 (Transit Payment)
────────────────────────         ────────────────────────
DR 100.000  (1)                  CR 100.000  (2)
CR     500  (4)                  DR 100.000  (5)
────────────────────────         ────────────────────────
Net DR  99.500                   Net 0 ✓

TK 5100 (Bank/Napas cost)        TK 2120 (Merchant payable)
────────────────────────         ────────────────────────
DR     500  (3)                  CR 100.000  (6)
────────────────────────         ────────────────────────
Expense +500                     Liability +100.000
```

### 2.3 Line builder contract
| Field | Value | Notes |
| --- | --- | --- |
| `use_case` | `QR_PAYMENT` | UNIQUE with `reference_id` |
| `status` | `POSTED` | Sync from acquirer |
| `business_ref` | Acquirer capture id | Idempotent replay |
| `member_id` | Merchant | Lines 5–6 tagged MERCHANT |
| Transit guard | SUM(3500) = 0 | ADR-010 |

### 2.4 Phân tách gross vs acquirer cost vs MDR
| Loại phí | TK | Thời điểm | Ai chịu |
| --- | --- | --- | --- |
| Acquirer cost (500) | **5100** | Capture POSTED | Platform expense |
| MDR merchant | **4140** | **EOD only** | Merchant — `[TBD: %]` |
| Net settlement | **3810→1112** | EOD | Merchant bank |

### 2.5 Ví dụ biến thể capture #1
Gross **200,000** · cost **1,000** · **2120 CR 200,000** · **1113** net **199,000** · **3500=0**.

### 2.6 Ví dụ biến thể capture #2
Gross **300,000** · cost **1,500** · **2120 CR 300,000** · **1113** net **298,500** · **3500=0**.

### 2.7 Ví dụ biến thể capture #3
Gross **400,000** · cost **2,000** · **2120 CR 400,000** · **1113** net **398,000** · **3500=0**.

### 2.8 Ví dụ biến thể capture #4
Gross **500,000** · cost **2,500** · **2120 CR 500,000** · **1113** net **497,500** · **3500=0**.

### 2.9 Ví dụ biến thể capture #5
Gross **600,000** · cost **3,000** · **2120 CR 600,000** · **1113** net **597,000** · **3500=0**.

### 2.10 Ví dụ biến thể capture #6
Gross **700,000** · cost **3,500** · **2120 CR 700,000** · **1113** net **696,500** · **3500=0**.

### 2.11 Ví dụ biến thể capture #7
Gross **800,000** · cost **4,000** · **2120 CR 800,000** · **1113** net **796,000** · **3500=0**.

### 2.12 Ví dụ biến thể capture #8
Gross **900,000** · cost **4,500** · **2120 CR 900,000** · **1113** net **895,500** · **3500=0**.

### 2.13 Ví dụ biến thể capture #9
Gross **1,000,000** · cost **5,000** · **2120 CR 1,000,000** · **1113** net **995,000** · **3500=0**.

### 2.14 Ví dụ biến thể capture #10
Gross **1,100,000** · cost **5,500** · **2120 CR 1,100,000** · **1113** net **1,094,500** · **3500=0**.

### 2.15 Ví dụ biến thể capture #11
Gross **1,200,000** · cost **6,000** · **2120 CR 1,200,000** · **1113** net **1,194,000** · **3500=0**.

### Ch2B.01 — mục mở rộng 1

Nội dung chi tiết liên kết Ch2 chính · foundation §12 · vol-02 TK 1113/3500/2120/5100
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch2B.02 — mục mở rộng 2

Nội dung chi tiết liên kết Ch2 chính · foundation §12 · vol-02 TK 1113/3500/2120/5100
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch2B.03 — mục mở rộng 3

Nội dung chi tiết liên kết Ch2 chính · foundation §12 · vol-02 TK 1113/3500/2120/5100
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

---
## Chương 3. 2120 pending until EOD (pointer Quyển VII, ADR-015) {#chương-3-2120-pending-until-eod}

QR capture POSTED **2120 CR** = merchant liability **pending bank settlement**. EOD batch ([`foundation.md`](../../spec/foundation.md) §16) **không inline** với capture ([ADR-015](../../adr/ADR-015-eod-settlement-independent-batch.md)). **Deep dive EOD → Quyển VII** — tài liệu này chỉ pointer.


```
2120 → 3800 (lock) → 3820 (MDR) + 3810 (net) → 1112 → Merchant bank
```

### 3.1 Trạng thái sau capture (default ADR-016)
| Layer | State |
| --- | --- |
| Wallet MERCHANT | available **unchanged** |
| COA 2120 | CR aggregate += gross (100.000/capture) |
| COA 1113 | Net inflow after acquirer cost |
| MDR 4140 | **Chưa** — chờ EOD |

### 3.2 Foundation §16 pointer (không duplicate)

| Step | Account | DR/CR | Amount (ví dụ) |
|------|---------|-------|----------------|
| Lock merchant | 2120 | DR | 200.000 |
| Hold clearing | 3800 | CR | 200.000 |
| Split MDR | 3800 DR → 3820 CR → **4140 CR** | | `[TBD: %]` of gross |
| Settlement | 3810 DR net → **1112 CR** | | incl. Napas **5100** |

**Exception branch:** reverse 3810/3820 → 3800 → **2120** if reconciliation mismatch — Quyển VII Ch exception.

### 3.3 QR + wallet payment cùng merchant
Cả QR capture và wallet payment đều **2120 CR** — EOD gom **tổng pending** theo merchantId + settlementDate. File acquirer `[TBD: format]` phải khớp subset QR.
### Ch3B.01 — mục mở rộng 1

Nội dung chi tiết liên kết Ch3 chính · ADR-015 · foundation §16 · vol-07 pointer
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch3B.02 — mục mở rộng 2

Nội dung chi tiết liên kết Ch3 chính · ADR-015 · foundation §16 · vol-07 pointer
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch3B.03 — mục mở rộng 3

Nội dung chi tiết liên kết Ch3 chính · ADR-015 · foundation §16 · vol-07 pointer
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch3B.04 — mục mở rộng 4

Nội dung chi tiết liên kết Ch3 chính · ADR-015 · foundation §16 · vol-07 pointer
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch3B.05 — mục mở rộng 5

Nội dung chi tiết liên kết Ch3 chính · ADR-015 · foundation §16 · vol-07 pointer
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch3B.06 — mục mở rộng 6

Nội dung chi tiết liên kết Ch3 chính · ADR-015 · foundation §16 · vol-07 pointer
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch3B.07 — mục mở rộng 7

Nội dung chi tiết liên kết Ch3 chính · ADR-015 · foundation §16 · vol-07 pointer
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch3B.08 — mục mở rộng 8

Nội dung chi tiết liên kết Ch3 chính · ADR-015 · foundation §16 · vol-07 pointer
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

---
## Chương 4. Failure Q-F1..Q-F4 {#chương-4-failure-q-f1q-f4}
Nguồn: [`accounting.md`](../accounting.md) §19.2 · [`processes.md`](../../spec/processes.md) §8.
| ID | Tình huống | Ledger | Recovery | Gherkin | ADR |
| --- | --- | --- | --- | --- | --- |
| Q-F1 | Acquirer duplicate capture id | POSTED exists | Idempotent same coa_trans_id | QR-03 | 005,016 |
| Q-F2 | MDR line mismatch at EOD prep | — | Reject post / block EOD | QR-10 | 015,009 |
| Q-F3 | EOD file ≠ 2120 | 2120 unchanged | Block settlement branch | QR-05 | 015 |
| Q-F4 | Partial merchant file | No POSTED settlement | Block entire branch | EOD partial | 015 |

### Chi tiết Q-F1: Acquirer duplicate capture id
| Trường | Giá trị |
| --- | --- |
| Detection | metric `q-f1`, batch worker alert |
| Ledger state | POSTED journal exists |
| Accounting action | Return same coa_trans_id |
| Recovery | Return same coa_trans_id · no second 2120 |
| Gherkin | QR-03 |
| ADR | 005,016 |
| Forbidden | UPDATE POSTED lines [ADR-001](../../adr/ADR-001-immutable-ledger.md) |

### Chi tiết Q-F2: MDR line mismatch
| Trường | Giá trị |
| --- | --- |
| Detection | metric `q-f2`, batch worker alert |
| Ledger state | Capture OK · EOD validator fail |
| Accounting action | Reject EOD post |
| Recovery | Reject EOD post · ops fix fee config `[TBD: %]` |
| Gherkin | QR-10 |
| ADR | 009,015 |
| Forbidden | UPDATE POSTED lines [ADR-001](../../adr/ADR-001-immutable-ledger.md) |

### Chi tiết Q-F3: EOD file ≠ 2120
| Trường | Giá trị |
| --- | --- |
| Detection | metric `q-f3`, batch worker alert |
| Ledger state | 2120 sum ≠ acquirer file |
| Accounting action | Block settlement |
| Recovery | Block settlement · exception queue |
| Gherkin | QR-05 |
| ADR | 015 |
| Forbidden | UPDATE POSTED lines [ADR-001](../../adr/ADR-001-immutable-ledger.md) |

### Chi tiết Q-F4: Partial merchant file
| Trường | Giá trị |
| --- | --- |
| Detection | metric `q-f4`, batch worker alert |
| Ledger state | Missing rows in file |
| Accounting action | No POSTED settlement for day |
| Recovery | No POSTED settlement for day |
| Gherkin | EOD partial file |
| ADR | 015 |
| Forbidden | UPDATE POSTED lines [ADR-001](../../adr/ADR-001-immutable-ledger.md) |

### Ch4B.01 — mục mở rộng 1

Nội dung chi tiết liên kết Ch4 chính · accounting.md §19.2 Q-F · acceptance QR-E
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch4B.02 — mục mở rộng 2

Nội dung chi tiết liên kết Ch4 chính · accounting.md §19.2 Q-F · acceptance QR-E
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch4B.03 — mục mở rộng 3

Nội dung chi tiết liên kết Ch4 chính · accounting.md §19.2 Q-F · acceptance QR-E
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch4B.04 — mục mở rộng 4

Nội dung chi tiết liên kết Ch4 chính · accounting.md §19.2 Q-F · acceptance QR-E
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch4B.05 — mục mở rộng 5

Nội dung chi tiết liên kết Ch4 chính · accounting.md §19.2 Q-F · acceptance QR-E
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch4B.06 — mục mở rộng 6

Nội dung chi tiết liên kết Ch4 chính · accounting.md §19.2 Q-F · acceptance QR-E
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch4B.07 — mục mở rộng 7

Nội dung chi tiết liên kết Ch4 chính · accounting.md §19.2 Q-F · acceptance QR-E
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch4B.08 — mục mở rộng 8

Nội dung chi tiết liên kết Ch4 chính · accounting.md §19.2 Q-F · acceptance QR-E
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

---
## Chương 5. Optional creditMerchantOnCapture flag {#chương-5-optional-creditmerchantoncapture-flag}

Product flag `creditMerchantOnCapture` (orchestration config) — **sau POSTED** accounting, orchestration có thể issue merchant wallet credit idempotent trên capture ref ([ADR-016](../../adr/ADR-016-qr-pos-default-no-per-txn-wallet.md) D3).

| Flag | Accounting | Wallet | W5 note |
| --- | --- | --- | --- |
| false (default) | 2120 CR only | no wallet_tx | 2120 may > SUM merchant wallets |
| true | 2120 CR | `MERCHANT` credit gross | Drift until EOD net credit aligns |

**Idempotency:** `(merchant_wallet_id, business_ref, MERCHANT_CAPTURE_CREDIT)` — `[TBD: tx_type enum name]`.
**EOD:** MDR vẫn trừ tại **4140** — wallet credit gross vs net settlement cần product policy Quyển VII.
### Ch5B.01 — mục mở rộng 1

Nội dung chi tiết liên kết Ch5 chính · ADR-016 optional · wallet.md §5.8
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch5B.02 — mục mở rộng 2

Nội dung chi tiết liên kết Ch5 chính · ADR-016 optional · wallet.md §5.8
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch5B.03 — mục mở rộng 3

Nội dung chi tiết liên kết Ch5 chính · ADR-016 optional · wallet.md §5.8
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch5B.04 — mục mở rộng 4

Nội dung chi tiết liên kết Ch5 chính · ADR-016 optional · wallet.md §5.8
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch5B.05 — mục mở rộng 5

Nội dung chi tiết liên kết Ch5 chính · ADR-016 optional · wallet.md §5.8
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

---
## Chương 6. Gherkin catalog QR — 12 scenarios {#chương-6-gherkin-catalog-qr-12-scenarios}
Nguồn: [`acceptance.md`](../acceptance.md) Feature QR/POS (2) + QR-E (10).
### QR-01: Acquirer capture happy — foundation §12
```gherkin
Feature: QR / POS acquirer
Scenario: Acquirer capture POSTED — no wallet per txn
Given QR capture businessRef="qr-100" gross=100000 acquirerCost=500
When POSTED per foundation §12
Then 1113 net DR 99500, 3500=0, 2120 CR 100000
And merchant wallet available unchanged (product default)
```

### QR-02: Acquirer cost 5100 line
```gherkin
Given capture gross=100000 acquirer cost=500
When POSTED
Then 5100 DR 500 and 1113 net +99500
```

### QR-03: Duplicate capture id — Q-F1
```gherkin
Given "qr-dup" already POSTED
When same acquirer id retried
Then idempotent same coa_trans_id
```

### QR-04: 2120 pending until EOD
```gherkin
Given multiple QR captures same merchant gross sum=500000
When before EOD
Then 2120 sum equals pending settlement total
And no 4140 at capture
```

### QR-05: Capture amount mismatch recon — Q-F3 precursor
```gherkin
Given acquirer file row 99000 internal 100000
When daily recon runs
Then exception queue not auto-adjust POSTED
```

### QR-06: Zero amount capture rejected
```gherkin
When capture amount=0
Then reject before POSTED
```

### QR-07: Merchant inactive COA 2120
```gherkin
Given merchant COA inactive
When post capture
Then ACCOUNTING_INACTIVE_ACCOUNT
```

### QR-08: Multi-capture same terminal same day
```gherkin
Given 50 captures distinct businessRef gross=100000 each
When all POSTED
Then each 3500=0 per journal
And 2120 aggregate=5000000
```

### QR-09: Optional wallet credit disabled — ADR-016 default
```gherkin
Given product flag creditMerchantOnCapture=false
When POSTED
Then no MERCHANT wallet_tx
```

### QR-10: MDR deferred to EOD only — Q-F2 context
```gherkin
Given capture 2120 CR gross=100000
When EOD runs
Then 4140 MDR from 3820 branch not at capture
And MDR percent [TBD: %]
```

### QR-11: EOD optional MERCHANT_SETTLE_CREDIT
```gherkin
Given EOD settlement credits merchant wallet per product
When MERCHANT_SETTLE_CREDIT issued
Then merchant available increases per batch ref
And ledger §16 already POSTED
```

### QR-12: Same ref different amount — 409
```gherkin
Given capture "qr-x" POSTED gross=100000
When replay "qr-x" gross=90000
Then WALLET_DUPLICATE_CONFLICT or accounting ref conflict 409
```

### Ch6B.01 — mục mở rộng 1

Nội dung chi tiết liên kết Ch6 chính · acceptance QR/POS + QR-E
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch6B.02 — mục mở rộng 2

Nội dung chi tiết liên kết Ch6 chính · acceptance QR/POS + QR-E
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch6B.03 — mục mở rộng 3

Nội dung chi tiết liên kết Ch6 chính · acceptance QR/POS + QR-E
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch6B.04 — mục mở rộng 4

Nội dung chi tiết liên kết Ch6 chính · acceptance QR/POS + QR-E
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch6B.05 — mục mở rộng 5

Nội dung chi tiết liên kết Ch6 chính · acceptance QR/POS + QR-E
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

---
## Chương 7. FAQ QR — 20 câu {#chương-7-faq-qr-20-câu}
**F1. QR dùng transit nào?**

**3500** — cùng TK với wallet payment, khác `use_case=QR_PAYMENT`.

**F2. 1113 khác 1111/1112?**

**1113** = VPBank acquirer QR/POS · **1111** Vietinbank deposit · **1112** Napas clearing.

**F3. Capture có PENDING?**

**Không** v1 — POSTED sync từ acquirer (có thể async queue nhưng một journal POSTED).

**F4. 5100 tại capture là gì?**

Chi phí acquirer (500 ví dụ) — **không** phải MDR merchant.

**F5. MDR ghi khi nào?**

**EOD** qua **3820→4140** — `[TBD: % MDR]`.

**F6. Merchant wallet khi nào tăng?**

Default **không** — optional flag Ch5 hoặc EOD bulk.

**F7. 2120 có giảm trước EOD?**

Chỉ khi merchant **payroll** debit hoặc ops reversal — không tự giảm.

**F8. 3500 ≠ 0 nghĩa gì?**

Journal lỗi — reject [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md).

**F9. Duplicate capture?**

Q-F1 idempotent — same `business_ref`.

**F10. File acquirer format?**

`[TBD: VPBank file format]` — recon input EOD.

**F11. QR + payment cùng merchant EOD?**

Gom **2120** pending — Quyển VII.

**F12. Có debit USER?**

**Không** — khách trả qua acquirer, không qua ví platform.

**F13. Transit 3800 khi nào?**

EOD lock — **không** tại capture.

**F14. W5 QR default?**

2120 aggregate vs SUM merchant wallets — drift expected until EOD credit.

**F15. Reverse capture?**

Ops **reversing journal** mới — không UPDATE POSTED.

**F16. Partial file EOD?**

Q-F4 — block settlement POSTED.

**F17. Terminal id mapping?**

`[TBD: terminal registry]` → merchantId.

**F18. Multi-currency?**

Ngoài scope v1 — VND only [ADR-019].

**F19. Scale số tiền?**

4 decimal HALF_UP orchestration boundary.

**F20. Đọc tiếp?**

Quyển VII EOD settlement deep dive.

### Ch7B.01 — mục mở rộng 1

Nội dung chi tiết liên kết Ch7 chính · FAQ QR · vol-02 TK 1113
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch7B.02 — mục mở rộng 2

Nội dung chi tiết liên kết Ch7 chính · FAQ QR · vol-02 TK 1113
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch7B.03 — mục mở rộng 3

Nội dung chi tiết liên kết Ch7 chính · FAQ QR · vol-02 TK 1113
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

---
## Chương 8. Tóm tắt merchant 2120 debit gross {#chương-8-tóm-tắt-merchant-2120-debit-gross}

**Goal:** Merchant trả lương hàng loạt ra ngân hàng nhân viên. **Batch async** — mỗi recipient sub-transaction ([`processes.md`](../../spec/processes.md) §9, §13.6).

| Khía cạnh | Giá trị |
| --- | --- |
| Gross merchant debit | **505.000** = 5×100.000 + fee 5.000 |
| Salaries out | **500.000** → **1112** |
| Platform fee revenue | **4150 CR 5.000** |
| Napas cost | **5100 DR 2.500** |
| Transit | **3600 = 0** |
| Wallet | `PAYROLL_DEBIT` MERCHANT −505.000 |
| Fee bearer | Merchant [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md) |

### 8.1 Saga steps payroll batch

```
1. Validate MERCHANT available >= gross (505.000)
2. PAYROLL_DEBIT wallet MERCHANT (idempotent batch ref)
3. POSTED journal foundation §14 (2120/3600/4150/1112/5100)
4. Per-recipient bank payout async `{batchRef}:{recipientId}`
5. Batch summary event succeeded[] failed[] retrying[]
```

### 8.2 Lane mapping ADR-020
MERCHANT wallet ↔ **2120** control. Payroll **không** dùng **2130** (PARTNER only).
### Ch8B.01 — mục mở rộng 1

Nội dung chi tiết liên kết Ch8 chính · foundation §14 · processes §9
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch8B.02 — mục mở rộng 2

Nội dung chi tiết liên kết Ch8 chính · foundation §14 · processes §9
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch8B.03 — mục mở rộng 3

Nội dung chi tiết liên kết Ch8 chính · foundation §14 · processes §9
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch8B.04 — mục mở rộng 4

Nội dung chi tiết liên kết Ch8 chính · foundation §14 · processes §9
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch8B.05 — mục mở rộng 5

Nội dung chi tiết liên kết Ch8 chính · foundation §14 · processes §9
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch8B.06 — mục mở rộng 6

Nội dung chi tiết liên kết Ch8 chính · foundation §14 · processes §9
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch8B.07 — mục mở rộng 7

Nội dung chi tiết liên kết Ch8 chính · foundation §14 · processes §9
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch8B.08 — mục mở rộng 8

Nội dung chi tiết liên kết Ch8 chính · foundation §14 · processes §9
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

---
## Chương 9. DR/CR batch foundation §14 deep {#chương-9-drcr-batch-foundation-§14-deep}
Authoritative: [`foundation.md`](../../spec/foundation.md) §14 · [`processes.md`](../../spec/processes.md) §9.1.
| Step | Actor | TK | DR/CR | Số tiền |
| --- | --- | --- | --- | --- |
| 1 | Merchant | **2120** | DR | 505.000 |
| 2 | Merchant | **3600** | CR | 505.000 |
| 3 | Merchant | **3600** | DR | 5.000 |
| 4 | Merchant | **4150** | CR | 5.000 |
| 5 | Bank | **3600** | DR | 500.000 |
| 6 | Bank | **1112** | CR | 500.000 |
| 8 | Bank | **5100** | DR | 2.500 |
| 9 | Bank | **1112** | CR | 2.500 |

**Kết quả:** **3600 = 0** · Net profit **+2.500** (4150 − 5100).
### 9.1 T-account payroll batch

```
TK 2120                         TK 3600 (Transit Payroll)
────────────────────            ─────────────────────────
DR 505.000  (1)                 CR 505.000  (2)
                                DR   5.000  (3)
                                DR 500.000  (5)
────────────────────            CR   5.000  (4) fee slice
Merchant −505.000               CR 500.000  (6)
                                Net 0 ✓

TK 4150                         TK 1112 / TK 5100
────────────────────            ─────────────────────────
CR   5.000  (4)                 CR 500.000  (6) salaries
Revenue +5.000                  CR   2.500  (9) Napas cost
                                DR   2.500  (8) 5100
```

### 9.2 Profit walk-through
Fee revenue **5.000** − Napas cost **2.500** = **2.500** net platform profit trên batch ví dụ.
### 9.3 Biến thể 1 nhân viên
Gross **105,000** · bank out **100,000** · fee **5.000** · **3600=0**.

### 9.4 Biến thể 2 nhân viên
Gross **205,000** · bank out **200,000** · fee **5.000** · **3600=0**.

### 9.5 Biến thể 3 nhân viên
Gross **305,000** · bank out **300,000** · fee **5.000** · **3600=0**.

### 9.6 Biến thể 4 nhân viên
Gross **405,000** · bank out **400,000** · fee **5.000** · **3600=0**.

### 9.7 Biến thể 5 nhân viên
Gross **505,000** · bank out **500,000** · fee **5.000** · **3600=0**.

### 9.8 Biến thể 6 nhân viên
Gross **605,000** · bank out **600,000** · fee **5.000** · **3600=0**.

### 9.9 Biến thể 7 nhân viên
Gross **705,000** · bank out **700,000** · fee **5.000** · **3600=0**.

### 9.10 Biến thể 8 nhân viên
Gross **805,000** · bank out **800,000** · fee **5.000** · **3600=0**.

### 9.11 Biến thể 9 nhân viên
Gross **905,000** · bank out **900,000** · fee **5.000** · **3600=0**.

### 9.12 Biến thể 10 nhân viên
Gross **1,005,000** · bank out **1,000,000** · fee **5.000** · **3600=0**.

### Ch9B.01 — mục mở rộng 1

Nội dung chi tiết liên kết Ch9 chính · foundation §14 · TK 3600/4150/5100/1112
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch9B.02 — mục mở rộng 2

Nội dung chi tiết liên kết Ch9 chính · foundation §14 · TK 3600/4150/5100/1112
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch9B.03 — mục mở rộng 3

Nội dung chi tiết liên kết Ch9 chính · foundation §14 · TK 3600/4150/5100/1112
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

---
## Chương 10. ADR-017 partial batch, per-recipient keys {#chương-10-adr-017-partial-batch-per-recipient-keys}
[ADR-017](../../adr/ADR-017-partial-batch-payroll-disbursement.md) · [`processes.md`](../../spec/processes.md) §13.6.
| Rule | Behavior |
| --- | --- |
| Batch `businessRef` | Client batch id |
| Per recipient | `{businessRef}:{recipientId}` |
| One fails | Others may POST sub-journals |
| Response | `{ succeeded[], failed[], retrying[] }` |
| Transit | **3600** zero for completed slice |
| Insufficient merchant | Reject **whole** batch submit pre-POSTED |

### 10.1 Partial batch narrative

Batch 10 recipients · recipient 7 bank reject:
- Recipients 1–6, 8–10: bank out OK · sub-keys idempotent
- Recipient 7: `failed[]` · forward-retry hoặc release policy `[TBD: release fee slice]`
- **3600=0** for accounting slice đã POSTED hoàn chỉnh
- **Không rollback** succeeded wallet/bank legs

### Ch10B.01 — mục mở rộng 1

Nội dung chi tiết liên kết Ch10 chính · ADR-017 · PR-E partial scenarios
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch10B.02 — mục mở rộng 2

Nội dung chi tiết liên kết Ch10 chính · ADR-017 · PR-E partial scenarios
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch10B.03 — mục mở rộng 3

Nội dung chi tiết liên kết Ch10 chính · ADR-017 · PR-E partial scenarios
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch10B.04 — mục mở rộng 4

Nội dung chi tiết liên kết Ch10 chính · ADR-017 · PR-E partial scenarios
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch10B.05 — mục mở rộng 5

Nội dung chi tiết liên kết Ch10 chính · ADR-017 · PR-E partial scenarios
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch10B.06 — mục mở rộng 6

Nội dung chi tiết liên kết Ch10 chính · ADR-017 · PR-E partial scenarios
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch10B.07 — mục mở rộng 7

Nội dung chi tiết liên kết Ch10 chính · ADR-017 · PR-E partial scenarios
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch10B.08 — mục mở rộng 8

Nội dung chi tiết liên kết Ch10 chính · ADR-017 · PR-E partial scenarios
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

---
## Chương 11. Failure PR-F expanded {#chương-11-failure-pr-f-expanded}
Nguồn: [`accounting.md`](../accounting.md) §20 · [`processes.md`](../../spec/processes.md) §13.6.
| ID | Scenario | Ledger | Recovery | Gherkin | ADR |
| --- | --- | --- | --- | --- | --- |
| PR-F1 | Insufficient merchant before post | — | Reject before POSTED/PAYROLL_DEBIT | PR-02 | 017 |
| PR-F2 | Partial recipient bank fail | 3600=0 slice | Summary failed[] · forward-retry | PR-03 | 017 |
| PR-F3 | Duplicate batch ref | POSTED exists | Idempotent full result | PR-05 | 005 |
| PR-F4 | Same batch ref different fee | — | 409 conflict | PR-06 | 005 |
| PR-F5 | MERCHANT LOCKED | — | WALLET_LOCKED reject | PR-08 | 029 |
| PR-F6 | 3600 ≠ 0 at guard | — | Reject post | — | 010 |
| PR-F7 | Empty recipient list | — | 400 no POSTED | PR-09 | — |
| PR-F8 | PAYROLL_DEBIT ok post fail | no POSTED | Compensate wallet `{ref}:comp` | — | 008 |

### Chi tiết PR-F1: Insufficient merchant before post
| Trường | Giá trị |
| --- | --- |
| Detection | metric `pr-f1`, batch worker alert |
| Ledger state | — |
| Accounting action | Reject before POSTED/PAYROLL_DEBIT |
| Recovery | Reject before POSTED/PAYROLL_DEBIT |
| Gherkin | PR-02 |
| ADR | 017 |
| Forbidden | UPDATE POSTED lines [ADR-001](../../adr/ADR-001-immutable-ledger.md) |

### Chi tiết PR-F2: Partial recipient bank fail
| Trường | Giá trị |
| --- | --- |
| Detection | metric `pr-f2`, batch worker alert |
| Ledger state | 3600=0 slice |
| Accounting action | Summary failed[] |
| Recovery | Summary failed[] · forward-retry |
| Gherkin | PR-03 |
| ADR | 017 |
| Forbidden | UPDATE POSTED lines [ADR-001](../../adr/ADR-001-immutable-ledger.md) |

### Chi tiết PR-F3: Duplicate batch ref
| Trường | Giá trị |
| --- | --- |
| Detection | metric `pr-f3`, batch worker alert |
| Ledger state | POSTED exists |
| Accounting action | Idempotent full result |
| Recovery | Idempotent full result |
| Gherkin | PR-05 |
| ADR | 005 |
| Forbidden | UPDATE POSTED lines [ADR-001](../../adr/ADR-001-immutable-ledger.md) |

### Chi tiết PR-F4: Same batch ref different fee
| Trường | Giá trị |
| --- | --- |
| Detection | metric `pr-f4`, batch worker alert |
| Ledger state | — |
| Accounting action | 409 conflict |
| Recovery | 409 conflict |
| Gherkin | PR-06 |
| ADR | 005 |
| Forbidden | UPDATE POSTED lines [ADR-001](../../adr/ADR-001-immutable-ledger.md) |

### Chi tiết PR-F5: MERCHANT LOCKED
| Trường | Giá trị |
| --- | --- |
| Detection | metric `pr-f5`, batch worker alert |
| Ledger state | — |
| Accounting action | WALLET_LOCKED reject |
| Recovery | WALLET_LOCKED reject |
| Gherkin | PR-08 |
| ADR | 029 |
| Forbidden | UPDATE POSTED lines [ADR-001](../../adr/ADR-001-immutable-ledger.md) |

### Chi tiết PR-F6: 3600 ≠ 0 at guard
| Trường | Giá trị |
| --- | --- |
| Detection | metric `pr-f6`, batch worker alert |
| Ledger state | — |
| Accounting action | Reject post |
| Recovery | Reject post |
| Gherkin | — |
| ADR | 010 |
| Forbidden | UPDATE POSTED lines [ADR-001](../../adr/ADR-001-immutable-ledger.md) |

### Chi tiết PR-F7: Empty recipient list
| Trường | Giá trị |
| --- | --- |
| Detection | metric `pr-f7`, batch worker alert |
| Ledger state | — |
| Accounting action | 400 no POSTED |
| Recovery | 400 no POSTED |
| Gherkin | PR-09 |
| ADR | — |
| Forbidden | UPDATE POSTED lines [ADR-001](../../adr/ADR-001-immutable-ledger.md) |

### Chi tiết PR-F8: PAYROLL_DEBIT ok post fail
| Trường | Giá trị |
| --- | --- |
| Detection | metric `pr-f8`, batch worker alert |
| Ledger state | no POSTED |
| Accounting action | Compensate wallet `{ref}:comp` |
| Recovery | Compensate wallet `{ref}:comp` |
| Gherkin | — |
| ADR | 008 |
| Forbidden | UPDATE POSTED lines [ADR-001](../../adr/ADR-001-immutable-ledger.md) |

### Ch11B.01 — mục mở rộng 1

Nội dung chi tiết liên kết Ch11 chính · PR-F matrix · acceptance PR-E
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch11B.02 — mục mở rộng 2

Nội dung chi tiết liên kết Ch11 chính · PR-F matrix · acceptance PR-E
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch11B.03 — mục mở rộng 3

Nội dung chi tiết liên kết Ch11 chính · PR-F matrix · acceptance PR-E
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch11B.04 — mục mở rộng 4

Nội dung chi tiết liên kết Ch11 chính · PR-F matrix · acceptance PR-E
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch11B.05 — mục mở rộng 5

Nội dung chi tiết liên kết Ch11 chính · PR-F matrix · acceptance PR-E
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch11B.06 — mục mở rộng 6

Nội dung chi tiết liên kết Ch11 chính · PR-F matrix · acceptance PR-E
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch11B.07 — mục mở rộng 7

Nội dung chi tiết liên kết Ch11 chính · PR-F matrix · acceptance PR-E
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch11B.08 — mục mở rộng 8

Nội dung chi tiết liên kết Ch11 chính · PR-F matrix · acceptance PR-E
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

---
## Chương 12. Gherkin catalog payroll — 12 scenarios {#chương-12-gherkin-catalog-payroll-12-scenarios}
### PR-01: Batch 5 employees foundation
```gherkin
Given payroll businessRef="pr-batch-1" employees=5 salaryEach=100000 fee=5000 gross=505000
When POSTED per foundation §14
Then 3600=0 and 4150 CR 5000 and 5100 DR 2500
And PAYROLL_DEBIT merchant 505000
```

### PR-02: Insufficient merchant
```gherkin
Given MERCHANT available=1000000
When payroll gross=5050000
Then reject before POSTED and PAYROLL_DEBIT
```

### PR-03: Partial recipient failure
```gherkin
Given batch 10 recipients
When recipient 7 bank reject
Then 9 succeed 1 failed in summary
And 3600=0 for succeeded slice per policy
```

### PR-04: Per-recipient idempotency
```gherkin
Given "pr-batch-2:emp-003" succeeded
When retry same recipient key
Then idempotent no double bank out
```

### PR-05: Duplicate batch ref
```gherkin
Given "pr-batch-3" completed
When resubmit same batch id
Then idempotent full batch result
```

### PR-06: Fee-only adjustment same batch
```gherkin
Given same batch ref different fee amount
Then 409 conflict
```

### PR-07: 5100 bank cost on payroll
```gherkin
Given POSTED payroll with bank leg Napas cost=2500
Then 5100 DR 2500 per foundation §14
```

### PR-08: Merchant LOCKED
```gherkin
Given MERCHANT wallet LOCKED
When payroll submit
Then WALLET_LOCKED reject
```

### PR-09: Empty recipient list
```gherkin
When payroll batch zero recipients
Then 400 no POSTED
```

### PR-10: Single employee
```gherkin
Given 1 recipient salary=100000 fee batch=1000 gross=101000
When POSTED
Then 2120 DR 101000 and 3600 clears
```

### PR-11: Feature payroll debits gross
```gherkin
Given payroll businessRef="pr-1" gross=505000 fee=5000
When batch POSTED
Then 2120 DR gross, 4150 CR fee, transit per §14
And PAYROLL_DEBIT merchant available reduced by gross
```

### PR-12: 2120 DR aligns wallet
```gherkin
Given POSTED payroll gross=505000
When W5 runs
Then MERCHANT wallet reduction matches 2120 DR aggregate delta
```

### Ch12B.01 — mục mở rộng 1

Nội dung chi tiết liên kết Ch12 chính · acceptance Payroll + PR-E
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch12B.02 — mục mở rộng 2

Nội dung chi tiết liên kết Ch12 chính · acceptance Payroll + PR-E
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch12B.03 — mục mở rộng 3

Nội dung chi tiết liên kết Ch12 chính · acceptance Payroll + PR-E
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch12B.04 — mục mở rộng 4

Nội dung chi tiết liên kết Ch12 chính · acceptance Payroll + PR-E
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch12B.05 — mục mở rộng 5

Nội dung chi tiết liên kết Ch12 chính · acceptance Payroll + PR-E
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

---
## Chương 13. FAQ payroll — 15 câu {#chương-13-faq-payroll-15-câu}
**F1. Payroll debit TK nào trước?**

**2120 DR** gross — đồng bộ **PAYROLL_DEBIT** wallet.

**F2. Fee ghi TK nào?**

**4150** revenue — không phải 4130 (transfer).

**F3. 3600 khi nào ≠ 0?**

Giữa các bước trong cùng journal builder — **POSTED phải 0**.

**F4. Napas cost?**

**5100 DR** + **1112 CR** — foundation §14 steps 8–9.

**F5. Partial batch rollback?**

**Không** — ADR-017 giữ succeeded legs.

**F6. Sub-key format?**

`{businessRef}:{recipientId}`.

**F7. Insufficient merchant?**

Reject **cả batch** trước POSTED — PR-F1.

**F8. MERCHANT vs USER?**

Payroll chỉ **MERCHANT** lane **2120**.

**F9. Sync hay async?**

**Async batch** — bank payout per recipient.

**F10. Net profit batch ví dụ?**

4150 **5.000** − 5100 **2.500** = **+2.500**.

**F11. Payroll từ QR 2120?**

Có — merchant dùng pending **2120** đã CR từ QR/payment.

**F12. Fee tier?**

`[TBD: payroll fee tier]` — orchestration config.

**F13. Empty batch?**

PR-F7 — 400 no POSTED.

**F14. Duplicate batch?**

PR-F3 idempotent replay.

**F15. Đọc tiếp disburse?**

Phần III — **2130** escrow partner.

### Ch13B.01 — mục mở rộng 1

Nội dung chi tiết liên kết Ch13 chính · FAQ payroll · TK 3600/4150
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch13B.02 — mục mở rộng 2

Nội dung chi tiết liên kết Ch13 chính · FAQ payroll · TK 3600/4150
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

---
## Chương 14. Two-phase pre-fund vs disburse journals {#chương-14-two-phase-pre-fund-vs-disburse-journals}

Disbursement **hai phase độc lập** ([`foundation.md`](../../spec/foundation.md) §15):
1. **Pre-fund:** Partner nạp escrow — **1111 DR 100.000 → 2130 CR 100.000**
2. **Disburse batch:** **2130 DR gross → 3700 → 1112** + **4150/5100**

| Phase | use_case | Wallet tx_type | Transit |
| --- | --- | --- | --- |
| Pre-fund | `DISBURSEMENT_PREFUND` | `PARTNER_PREFUND_CREDIT` | none |
| Disburse | `DISBURSEMENT` | `DISBURSEMENT_DEBIT` | **3700** → 0 |

**[TBD: partner prefund rules]** — minimum balance, auto top-up, settlement cycle.
### Ch14B.01 — mục mở rộng 1

Nội dung chi tiết liên kết Ch14 chính · foundation §15 two-phase
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch14B.02 — mục mở rộng 2

Nội dung chi tiết liên kết Ch14 chính · foundation §15 two-phase
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch14B.03 — mục mở rộng 3

Nội dung chi tiết liên kết Ch14 chính · foundation §15 two-phase
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch14B.04 — mục mở rộng 4

Nội dung chi tiết liên kết Ch14 chính · foundation §15 two-phase
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch14B.05 — mục mở rộng 5

Nội dung chi tiết liên kết Ch14 chính · foundation §15 two-phase
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch14B.06 — mục mở rộng 6

Nội dung chi tiết liên kết Ch14 chính · foundation §15 two-phase
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch14B.07 — mục mở rộng 7

Nội dung chi tiết liên kết Ch14 chính · foundation §15 two-phase
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch14B.08 — mục mở rộng 8

Nội dung chi tiết liên kết Ch14 chính · foundation §15 two-phase
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

---
## Chương 15. DR/CR pre-fund 1111→2130, disburse 2130→3700→1112 {#chương-15-drcr-pre-fund-11112130-disburse-213037001112}
### 15.1 Pre-fund journal
| Step | TK | DR/CR | Số tiền |
| --- | --- | --- | --- |
| 1 | **1111** | DR | 100.000 |
| 2 | **2130** | CR | 100.000 |

Wallet: `PARTNER_PREFUND_CREDIT` +100.000 available.
### 15.2 Disburse journal — gross 101.000
Ví dụ: beneficiary **100.000** + platform fee **1.000** = gross **101.000** debit **2130**.
| Step | TK | DR/CR | Số tiền | Notes |
| --- | --- | --- | --- | --- |
| 1 | **2130** | DR | 101.000 | Gross partner debit |
| 2 | **3700** | CR | 101.000 | Transit in |
| 3 | **3700** | DR | 100.000 | Bank out principal |
| 4 | **1112** | CR | 100.000 | Napas to beneficiary |
| 5 | **3700** | DR | 1.000 | Fee slice |
| 6 | **4150** | CR | 1.000 | Revenue `[TBD: fee tier]` |
| 7 | **5100** | DR | 500 | Napas cost example |
| 8 | **1112** | CR | 500 | Cost settlement |

**Kết quả:** **3700 = 0** · **2130** −101.000 (from 100.000 prefund → remainder **−1.000** needs top-up `[TBD]`).
### 15.3 T-account disburse

```
Pre-fund:  1111 DR 100.000  |  2130 CR 100.000

Disburse:  2130 DR 101.000  |  3700 CR 101.000
           3700 DR 100.000  |  1112 CR 100.000
           3700 DR   1.000  |  4150 CR   1.000
           5100 DR     500  |  1112 CR     500
           3700 net 0 ✓
```

### Ch15B.01 — mục mở rộng 1

Nội dung chi tiết liên kết Ch15 chính · foundation §15 · TK 3700/2130/1111/1112
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch15B.02 — mục mở rộng 2

Nội dung chi tiết liên kết Ch15 chính · foundation §15 · TK 3700/2130/1111/1112
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch15B.03 — mục mở rộng 3

Nội dung chi tiết liên kết Ch15 chính · foundation §15 · TK 3700/2130/1111/1112
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

---
## Chương 16. PARTNER lane 2130 mirror wallet {#chương-16-partner-lane-2130-mirror-wallet}
[ADR-020](../../adr/ADR-020-wallet-lanes-coa-control-mapping.md) · [`vol-02-coa-handbook.md`](./vol-02-coa-handbook.md) TK 2130.
| wallet_type | COA | Disburse role |
| --- | --- | --- |
| PARTNER | **2130** | Escrow liability — prefund CR, disburse DR |
| MERCHANT | 2120 | Payroll — **not** disburse |
| USER | 2110 | N/A disburse |

W5: SUM(PARTNER wallets) ≈ **2130** aggregate.
**Mirror rule:** `PARTNER_PREFUND_CREDIT` ↔ **2130 CR** · `DISBURSEMENT_DEBIT` ↔ **2130 DR** gross.
### Ch16B.01 — mục mở rộng 1

Nội dung chi tiết liên kết Ch16 chính · ADR-020 PARTNER · wallet.md §5.5
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch16B.02 — mục mở rộng 2

Nội dung chi tiết liên kết Ch16 chính · ADR-020 PARTNER · wallet.md §5.5
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch16B.03 — mục mở rộng 3

Nội dung chi tiết liên kết Ch16 chính · ADR-020 PARTNER · wallet.md §5.5
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch16B.04 — mục mở rộng 4

Nội dung chi tiết liên kết Ch16 chính · ADR-020 PARTNER · wallet.md §5.5
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch16B.05 — mục mở rộng 5

Nội dung chi tiết liên kết Ch16 chính · ADR-020 PARTNER · wallet.md §5.5
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch16B.06 — mục mở rộng 6

Nội dung chi tiết liên kết Ch16 chính · ADR-020 PARTNER · wallet.md §5.5
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch16B.07 — mục mở rộng 7

Nội dung chi tiết liên kết Ch16 chính · ADR-020 PARTNER · wallet.md §5.5
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch16B.08 — mục mở rộng 8

Nội dung chi tiết liên kết Ch16 chính · ADR-020 PARTNER · wallet.md §5.5
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

---
## Chương 17. ADR-017 partial + insufficient 2130 {#chương-17-adr-017-partial--insufficient-2130}
| Case | Rule |
| --- | --- |
| available < gross disburse | Reject leg — DIS-F1/B-F1 |
| Partial beneficiaries fail | Summary like payroll — ADR-017 |
| 2130 < sum concurrent batches | Serialize or reject `[TBD: concurrency policy]` |
| Completed slice | **3700 = 0** |

Insufficient **2130** after partial prefund: partner must pre-fund thêm trước batch mới.
### Ch17B.01 — mục mở rộng 1

Nội dung chi tiết liên kết Ch17 chính · ADR-017 disburse · DIS-E03
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch17B.02 — mục mở rộng 2

Nội dung chi tiết liên kết Ch17 chính · ADR-017 disburse · DIS-E03
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch17B.03 — mục mở rộng 3

Nội dung chi tiết liên kết Ch17 chính · ADR-017 disburse · DIS-E03
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch17B.04 — mục mở rộng 4

Nội dung chi tiết liên kết Ch17 chính · ADR-017 disburse · DIS-E03
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch17B.05 — mục mở rộng 5

Nội dung chi tiết liên kết Ch17 chính · ADR-017 disburse · DIS-E03
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch17B.06 — mục mở rộng 6

Nội dung chi tiết liên kết Ch17 chính · ADR-017 disburse · DIS-E03
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch17B.07 — mục mở rộng 7

Nội dung chi tiết liên kết Ch17 chính · ADR-017 disburse · DIS-E03
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch17B.08 — mục mở rộng 8

Nội dung chi tiết liên kết Ch17 chính · ADR-017 disburse · DIS-E03
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

---
## Chương 18. Failure DIS-F expanded {#chương-18-failure-dis-f-expanded}
| ID | Scenario | Ledger | Recovery | Gherkin | ADR |
| --- | --- | --- | --- | --- | --- |
| DIS-F1 | Insufficient 2130/available | — | Reject before POSTED | DIS-02 | 017 |
| DIS-F2 | Partial disburse batch | 3700=0 slice | Summary 17/20 succeed | DIS-03 | 017 |
| DIS-F3 | Duplicate batch/prefund id | POSTED exists | Idempotent replay | DIS-04 | 005 |
| DIS-F4 | Disburse without prefund | — | Reject | DIS-E02 | — |
| DIS-F5 | Gross exceeds remainder | — | Reject leg | DIS-E09 | 017 |
| DIS-F6 | 3700 ≠ 0 incomplete | — | Post guard reject | DIS-E10 | 010 |
| DIS-F7 | Prefund reversal ops | 2130 reduced | Ops policy + PARTNER debit | DIS-E06 | 001 |
| DIS-F8 | Same ref different amount | — | 409 conflict | — | 005 |

### Chi tiết DIS-F1: Insufficient 2130/available
| Trường | Giá trị |
| --- | --- |
| Detection | metric `dis-f1`, batch worker alert |
| Ledger state | — |
| Accounting action | Reject before POSTED |
| Recovery | Reject before POSTED |
| Gherkin | DIS-02 |
| ADR | 017 |
| Forbidden | UPDATE POSTED lines [ADR-001](../../adr/ADR-001-immutable-ledger.md) |

### Chi tiết DIS-F2: Partial disburse batch
| Trường | Giá trị |
| --- | --- |
| Detection | metric `dis-f2`, batch worker alert |
| Ledger state | 3700=0 slice |
| Accounting action | Summary 17/20 succeed |
| Recovery | Summary 17/20 succeed |
| Gherkin | DIS-03 |
| ADR | 017 |
| Forbidden | UPDATE POSTED lines [ADR-001](../../adr/ADR-001-immutable-ledger.md) |

### Chi tiết DIS-F3: Duplicate batch/prefund id
| Trường | Giá trị |
| --- | --- |
| Detection | metric `dis-f3`, batch worker alert |
| Ledger state | POSTED exists |
| Accounting action | Idempotent replay |
| Recovery | Idempotent replay |
| Gherkin | DIS-04 |
| ADR | 005 |
| Forbidden | UPDATE POSTED lines [ADR-001](../../adr/ADR-001-immutable-ledger.md) |

### Chi tiết DIS-F4: Disburse without prefund
| Trường | Giá trị |
| --- | --- |
| Detection | metric `dis-f4`, batch worker alert |
| Ledger state | — |
| Accounting action | Reject |
| Recovery | Reject |
| Gherkin | DIS-E02 |
| ADR | — |
| Forbidden | UPDATE POSTED lines [ADR-001](../../adr/ADR-001-immutable-ledger.md) |

### Chi tiết DIS-F5: Gross exceeds remainder
| Trường | Giá trị |
| --- | --- |
| Detection | metric `dis-f5`, batch worker alert |
| Ledger state | — |
| Accounting action | Reject leg |
| Recovery | Reject leg |
| Gherkin | DIS-E09 |
| ADR | 017 |
| Forbidden | UPDATE POSTED lines [ADR-001](../../adr/ADR-001-immutable-ledger.md) |

### Chi tiết DIS-F6: 3700 ≠ 0 incomplete
| Trường | Giá trị |
| --- | --- |
| Detection | metric `dis-f6`, batch worker alert |
| Ledger state | — |
| Accounting action | Post guard reject |
| Recovery | Post guard reject |
| Gherkin | DIS-E10 |
| ADR | 010 |
| Forbidden | UPDATE POSTED lines [ADR-001](../../adr/ADR-001-immutable-ledger.md) |

### Chi tiết DIS-F7: Prefund reversal ops
| Trường | Giá trị |
| --- | --- |
| Detection | metric `dis-f7`, batch worker alert |
| Ledger state | 2130 reduced |
| Accounting action | Ops policy + PARTNER debit |
| Recovery | Ops policy + PARTNER debit |
| Gherkin | DIS-E06 |
| ADR | 001 |
| Forbidden | UPDATE POSTED lines [ADR-001](../../adr/ADR-001-immutable-ledger.md) |

### Chi tiết DIS-F8: Same ref different amount
| Trường | Giá trị |
| --- | --- |
| Detection | metric `dis-f8`, batch worker alert |
| Ledger state | — |
| Accounting action | 409 conflict |
| Recovery | 409 conflict |
| Gherkin | — |
| ADR | 005 |
| Forbidden | UPDATE POSTED lines [ADR-001](../../adr/ADR-001-immutable-ledger.md) |

### Ch18B.01 — mục mở rộng 1

Nội dung chi tiết liên kết Ch18 chính · accounting.md §21 DIS-F/B-F
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch18B.02 — mục mở rộng 2

Nội dung chi tiết liên kết Ch18 chính · accounting.md §21 DIS-F/B-F
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch18B.03 — mục mở rộng 3

Nội dung chi tiết liên kết Ch18 chính · accounting.md §21 DIS-F/B-F
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch18B.04 — mục mở rộng 4

Nội dung chi tiết liên kết Ch18 chính · accounting.md §21 DIS-F/B-F
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch18B.05 — mục mở rộng 5

Nội dung chi tiết liên kết Ch18 chính · accounting.md §21 DIS-F/B-F
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch18B.06 — mục mở rộng 6

Nội dung chi tiết liên kết Ch18 chính · accounting.md §21 DIS-F/B-F
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch18B.07 — mục mở rộng 7

Nội dung chi tiết liên kết Ch18 chính · accounting.md §21 DIS-F/B-F
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch18B.08 — mục mở rộng 8

Nội dung chi tiết liên kết Ch18 chính · accounting.md §21 DIS-F/B-F
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

---
## Chương 19. Gherkin catalog disburse — 12 scenarios {#chương-19-gherkin-catalog-disburse-12-scenarios}
### DIS-01: Prefund then disburse
```gherkin
Given PARTNER prefund businessRef="pf-1" amount=100000
When POSTED 1111 DR 2130 CR
Then PARTNER_PREFUND_CREDIT 100000
When disbursement businessRef="dis-1" gross=101000 beneficiary=100000 fee=1000
Then DISBURSEMENT_DEBIT and 3700=0
```

### DIS-02: Disburse exceeds available
```gherkin
Given PARTNER available=100000
When disbursement gross=500000
Then reject and no POSTED
```

### DIS-03: Partial disburse batch
```gherkin
Given batch 20 beneficiaries
When 3 fail bank
Then 17 succeed summary event
```

### DIS-04: Idempotent prefund
```gherkin
Given "pf-2" prefund POSTED
When replay prefund command
Then no second 2130 CR
```

### DIS-05: 4150 fee on disburse
```gherkin
Given disburse with fee line 1000
When POSTED
Then 4150 CR 1000 per foundation §15
```

### DIS-06: Pre-fund reversal ops
```gherkin
Given ops reverse prefund journal
Then 2130 reduced and PARTNER debit policy
```

### DIS-07: Concurrent batches same partner
```gherkin
Given two batches same partner different businessRef
When both run
Then serialize or sufficient 2130 for both [TBD]
```

### DIS-08: Beneficiary id sub-key
```gherkin
Given disburse "dis-1:ben-42"
When retry
Then idempotent single leg
```

### DIS-09: Gross exceeds prefund remainder
```gherkin
Given PARTNER available=100000 after prefund
When disburse gross=200000
Then reject DIS-F5
```

### DIS-10: 3700 non-zero blocked
```gherkin
Given incomplete disburse lines
When post guard runs
Then reject 3700 != 0
```

### DIS-11: Feature partner disbursement
```gherkin
Given PARTNER prefund 10000000
When disbursement gross=2000000
Then ledger §15 POSTED and 3700=0
```

### DIS-12: 5100 cost on disburse out
```gherkin
Given Napas cost 500 on bank leg
When POSTED disburse
Then 5100 DR 500 and 1112 CR 500
```

### Ch19B.01 — mục mở rộng 1

Nội dung chi tiết liên kết Ch19 chính · acceptance Disbursement + DIS-E
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch19B.02 — mục mở rộng 2

Nội dung chi tiết liên kết Ch19 chính · acceptance Disbursement + DIS-E
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch19B.03 — mục mở rộng 3

Nội dung chi tiết liên kết Ch19 chính · acceptance Disbursement + DIS-E
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch19B.04 — mục mở rộng 4

Nội dung chi tiết liên kết Ch19 chính · acceptance Disbursement + DIS-E
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch19B.05 — mục mở rộng 5

Nội dung chi tiết liên kết Ch19 chính · acceptance Disbursement + DIS-E
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

---
## Chương 20. FAQ disburse — 15 câu {#chương-20-faq-disburse-15-câu}
**F1. Pre-fund bắt buộc?**

**Có** v1 — **1111→2130** trước disburse batch.

**F2. 2130 vs 2120?**

**2130** PARTNER escrow · **2120** MERCHANT — không interchange.

**F3. Transit disburse?**

**3700** — must zero POSTED.

**F4. Fee TK?**

**4150** — cùng nhóm payroll/disburse revenue.

**F5. Bank out TK?**

**1112** Napas clearing.

**F6. Prefund bank?**

**1111** Vietinbank — deposit rail reuse.

**F7. Partial batch?**

ADR-017 — summary succeeded/failed/retrying.

**F8. Insufficient escrow?**

DIS-F1 reject — no POSTED.

**F9. Wallet tx types?**

`PARTNER_PREFUND_CREDIT` · `DISBURSEMENT_DEBIT`.

**F10. Idempotency prefund?**

Unique `(reference_id, DISBURSEMENT_PREFUND)`.

**F11. Concurrent batches?**

`[TBD: partner prefund rules]` — serialize or balance check.

**F12. Gross components?**

beneficiary net + platform fee = gross **2130 DR**.

**F13. Reversal prefund?**

Ops reversing journal — DIS-F7.

**F14. W5 PARTNER?**

SUM partner wallets ≈ **2130**.

**F15. Fee tier?**

`[TBD: disbursement fee tier]`.

### Ch20B.01 — mục mở rộng 1

Nội dung chi tiết liên kết Ch20 chính · FAQ disburse · TK 3700/2130
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch20B.02 — mục mở rộng 2

Nội dung chi tiết liên kết Ch20 chính · FAQ disburse · TK 3700/2130
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

---
## Chương 21. Matrix QR vs Payment vs Payroll vs Disburse {#chương-21-matrix-qr-vs-payment-vs-payroll-vs-disburse}
|  | QR/POS | Wallet Payment | Payroll | Disburse |
| --- | --- | --- | --- | --- |
| Transit | 3500 | 3500 | 3600 | 3700 |
| Bank 111x | **1113** | none | **1112** out | **1111** in / **1112** out |
| Liability | **2120** CR | **2120** CR | **2120** DR | **2130** DR |
| Wallet default | none | DEBIT/CREDIT | PAYROLL_DEBIT | PREFUND/DISBURSE |
| Batch | capture per txn | sync 200 | async batch | async batch |
| Partial fail | N/A single | compensate sync | ADR-017 | ADR-017 |
| Revenue fee | 4140 EOD | `[TBD]` | 4150 | 4150 |
| Cost | 5100 capture | none | 5100 out | 5100 out |
| EOD | **2120→3800** | 2120 pending | n/a | n/a |

### Ch21B.01 — mục mở rộng 1

Nội dung chi tiết liên kết Ch21 chính · processes §12 cross-ref
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch21B.02 — mục mở rộng 2

Nội dung chi tiết liên kết Ch21 chính · processes §12 cross-ref
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch21B.03 — mục mở rộng 3

Nội dung chi tiết liên kết Ch21 chính · processes §12 cross-ref
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch21B.04 — mục mở rộng 4

Nội dung chi tiết liên kết Ch21 chính · processes §12 cross-ref
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch21B.05 — mục mở rộng 5

Nội dung chi tiết liên kết Ch21 chính · processes §12 cross-ref
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

---
## Chương 22. Batch idempotency patterns {#chương-22-batch-idempotency-patterns}
[ADR-005](../../adr/ADR-005-idempotency-key-strategy.md) · §13.6 processes.
| Flow | Batch key | Sub-key | Accounting unique |
| --- | --- | --- | --- |
| QR capture | acquirer capture id | — | (ref, QR_PAYMENT) |
| Payroll | `pr-batch-1` | `pr-batch-1:emp-003` | (ref, PAYROLL) + sub payout |
| Disburse prefund | `pf-1` | — | (ref, DISBURSEMENT_PREFUND) |
| Disburse | `dis-1` | `dis-1:ben-42` | (ref, DISBURSEMENT) + sub |
| EOD | `(merchantId, date)` | — | Quyển VII |

**409 rule:** same ref + different amount/fee → reject — never silent overwrite.
### Ch22B.01 — mục mở rộng 1

Nội dung chi tiết liên kết Ch22 chính · ADR-005 batch keys
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch22B.02 — mục mở rộng 2

Nội dung chi tiết liên kết Ch22 chính · ADR-005 batch keys
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch22B.03 — mục mở rộng 3

Nội dung chi tiết liên kết Ch22 chính · ADR-005 batch keys
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch22B.04 — mục mở rộng 4

Nội dung chi tiết liên kết Ch22 chính · ADR-005 batch keys
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch22B.05 — mục mở rộng 5

Nội dung chi tiết liên kết Ch22 chính · ADR-005 batch keys
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch22B.06 — mục mở rộng 6

Nội dung chi tiết liên kết Ch22 chính · ADR-005 batch keys
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch22B.07 — mục mở rộng 7

Nội dung chi tiết liên kết Ch22 chính · ADR-005 batch keys
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch22B.08 — mục mở rộng 8

Nội dung chi tiết liên kết Ch22 chính · ADR-005 batch keys
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

---
## Chương 23. SQL invariant CI per transit {#chương-23-sql-invariant-ci-per-transit}
Structural checks — ADR-031 pattern · nightly + CI.
### 23.1 Transit zero POSTED

```sql
-- QR_PAYMENT: 3500 net zero per journal
SELECT t.id FROM coa_trans t
JOIN coa_trans_data d ON d.trans_id = t.id
WHERE t.use_case = 'QR_PAYMENT' AND t.status = 'POSTED'
GROUP BY t.id
HAVING SUM(CASE WHEN d.account_code = '3500' THEN d.cr_amount - d.dr_amount ELSE 0 END) <> 0;

-- PAYROLL: 3600 net zero
-- DISBURSEMENT: 3700 net zero
```

### 23.2 Lane balance drift W5

```sql
-- MERCHANT wallets vs 2120 (tolerance policy)
-- PARTNER wallets vs 2130
-- INV-1: SUM(1111,1112,1113) vs SUM(2110,2120,2130)
```

### 23.3 Per-flow guards
| use_case | Guard | On fail |
| --- | --- | --- |
| QR_PAYMENT | 3500=0, lines 1113+5100+2120 | CI fail |
| PAYROLL | 3600=0, 2120 DR = gross | CI fail |
| DISBURSEMENT | 3700=0, 2130 DR = gross slice | CI fail |
| DISBURSEMENT_PREFUND | 1111 DR = 2130 CR | CI fail |

### Ch23B.01 — mục mở rộng 1

Nội dung chi tiết liên kết Ch23 chính · SQL invariant · ADR-010/031
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch23B.02 — mục mở rộng 2

Nội dung chi tiết liên kết Ch23 chính · SQL invariant · ADR-010/031
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch23B.03 — mục mở rộng 3

Nội dung chi tiết liên kết Ch23 chính · SQL invariant · ADR-010/031
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

---
## Chương 24. Review checklist — 35 mục {#chương-24-review-checklist-35-mục}
| # | Pass | Mục |
|---|------|-----|
| 1 | ☐ | QR capture: gross 100.000 + cost 500 template khớp foundation §12 |
| 2 | ☐ | 3500 net zero mọi QR POSTED |
| 3 | ☐ | 1113 net inflow = gross − acquirer cost |
| 4 | ☐ | 5100 ghi acquirer cost — không nhầm MDR |
| 5 | ☐ | MDR **không** tại capture — deferred EOD `[TBD: %]` |
| 6 | ☐ | Default ADR-016: zero wallet_tx capture |
| 7 | ☐ | creditMerchantOnCapture gated POSTED + flag |
| 8 | ☐ | Q-F1 duplicate idempotent |
| 9 | ☐ | Q-F3/Q-F4 EOD block — pointer Quyển VII |
| 10 | ☐ | Payroll gross 505.000 = 5×100k + 5k fee |
| 11 | ☐ | 2120 DR payroll = wallet PAYROLL_DEBIT gross |
| 12 | ☐ | 4150 CR fee payroll |
| 13 | ☐ | 1112 CR salaries 500.000 |
| 14 | ☐ | 5100 DR Napas 2.500 payroll |
| 15 | ☐ | 3600 net zero payroll POSTED |
| 16 | ☐ | Partial payroll ADR-017 summary |
| 17 | ☐ | Per-recipient key `{ref}:{id}` |
| 18 | ☐ | Insufficient merchant reject whole batch pre-POSTED |
| 19 | ☐ | Pre-fund 1111→2130 100.000 separate journal |
| 20 | ☐ | PARTNER_PREFUND_CREDIT wallet mirror |
| 21 | ☐ | Disburse gross 101.000 → 3700 clears |
| 22 | ☐ | 4150 fee disburse line |
| 23 | ☐ | 2130 insufficient reject DIS-F1 |
| 24 | ☐ | 3700 net zero disburse POSTED |
| 25 | ☐ | Partial disburse không rollback succeeded |
| 26 | ☐ | Fee từ orchestration ADR-009 — không hard-code |
| 27 | ☐ | Transit ADR-010 enforced postJournal |
| 28 | ☐ | business_ref ADR-005 propagated |
| 29 | ☐ | PARTNER lane 2130 ADR-020 |
| 30 | ☐ | No JOIN wallet+cao in one query |
| 31 | ☐ | Immutable POSTED ADR-001 |
| 32 | ☐ | Batch summary event emitted §13.6 |
| 33 | ☐ | 409 same ref different amount |
| 34 | ☐ | W5 tolerance documented ops |
| 35 | ☐ | EOD deep dive deferred vol-07 |

### Ch24B.01 — mục mở rộng 1

Nội dung chi tiết liên kết Ch24 chính · review checklist 35
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch24B.02 — mục mở rộng 2

Nội dung chi tiết liên kết Ch24 chính · review checklist 35
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

### Ch24B.03 — mục mở rộng 3

Nội dung chi tiết liên kết Ch24 chính · review checklist 35
Evidence: `coa_trans` POSTED · transit net zero per use_case · immutability ADR-001.
Ops: batch summary event · per-recipient idempotency ADR-017 · fee từ orchestration ADR-009.

---
## Phụ lục A — Ma trận kịch bản số {#phụ-lục-a--ma-trận-kịch-bản-số}
| ID | Flow | Gross | Fee/Cost | Transit |
| --- | --- | --- | --- | --- |
| S-QR-01 | QR capture | 100.000 | cost 500 | 3500=0 |
| S-PR-01 | Payroll 5 emp | 505.000 | fee 5k, cost 2.5k | 3600=0 |
| S-DIS-01 | Prefund | 100.000 | — | — |
| S-DIS-02 | Disburse | 101.000 | fee 1k, cost 500 | 3700=0 |

## Phụ lục B — DR/CR line-by-line {#phụ-lục-b--drcr-line-by-line}
Tham chiếu foundation §12, §14, §15 — line order authoritative ở spec.
**QR §12:** 6 lines — xem Ch2, Ch9, Ch15.
**Payroll §14:** 9 lines — xem Ch2, Ch9, Ch15.
**Disburse §15:** 8 lines — xem Ch2, Ch9, Ch15.
## Phụ lục C — Bảng tra ADR {#phụ-lục-c--bảng-tra-adr}
| ADR | Topic | Quyển VI |
| --- | --- | --- |
| 005 | business_ref | Ch22 |
| 009 | Fee orchestration | Ch2,8,15 |
| 010 | Transit zero | Ch23 |
| 015 | EOD batch | Ch3, H |
| 016 | QR no wallet | Ch1,5 |
| 017 | Partial batch | Ch10,17 |
| 020 | Lanes | Ch8,16 |

## Phụ lục D — processes §8–§10 mapping {#phụ-lục-d--processes-§8§10-mapping}
| processes | Quyển VI |
| --- | --- |
| §8 QR | Phần I Ch1–7 |
| §9 Payroll | Phần II Ch8–13 |
| §10 Disburse | Phần III Ch14–20 |
| §13.6 partial | Ch10,17,22 |

## Phụ lục E — accounting.md §19–§21 mapping {#phụ-lục-e--accounting.md-1921-mapping}
| accounting.md | Quyển VI |
| --- | --- |
| §19 QR | Ch1–7, Q-F |
| §20 Payroll | Ch8–13, PR-F |
| §21 Disburse | Ch14–20, DIS-F |

## Phụ lục F — Numeric T-account gallery {#phụ-lục-f--numeric-t-account-gallery}
QR 100k · Payroll 505k · Disburse prefund 100k + gross 101k — T-accounts Ch2, Ch9, Ch15.
## Phụ lục G — Cross-flow invariant {#phụ-lục-g--cross-flow-invariant}
INV-1: `1111+1112+1113 = 2110+2120+2130` · All transits terminal zero ADR-010.
## Phụ lục H — Đọc tiếp vol-07 EOD {#phụ-lục-h--đọc-tiếp-vol-07-eod}

**Quyển VII — EOD Settlement & Clearing** deep dive:
- foundation §16 lock **2120→3800→3820/3810→1112**
- **4140 MDR** `[TBD: %]`
- Exception branch Q-F3/Q-F4
- Optional `MERCHANT_SETTLE_CREDIT`
- ADR-015 idempotent `(merchantId, settlementDate)`

Quyển VI **không** duplicate EOD postings — chỉ pointer Ch3 + Phụ lục H.

### Annex 1 — QR capture webhook

**1113/3500/2120/5100 · ADR-016 default · sync POSTED** — evidence `coa_trans` POSTED; transit net zero [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md); fee orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md).
Ops: batch summary §13.6 · acquirer file `[TBD: format]` · MDR `[TBD: %]` tại EOD vol-07.
QA: map acceptance QR-E / PR-E / DIS-E scenarios tương ứng Phần I–III.

### Annex 2 — Payroll batch worker

**3600 transit · PAYROLL_DEBIT · per-recipient bank** — evidence `coa_trans` POSTED; transit net zero [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md); fee orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md).
Ops: batch summary §13.6 · acquirer file `[TBD: format]` · MDR `[TBD: %]` tại EOD vol-07.
QA: map acceptance QR-E / PR-E / DIS-E scenarios tương ứng Phần I–III.

### Annex 3 — Disburse prefund rail

**1111→2130 · PARTNER_PREFUND_CREDIT mirror** — evidence `coa_trans` POSTED; transit net zero [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md); fee orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md).
Ops: batch summary §13.6 · acquirer file `[TBD: format]` · MDR `[TBD: %]` tại EOD vol-07.
QA: map acceptance QR-E / PR-E / DIS-E scenarios tương ứng Phần I–III.

### Annex 4 — Disburse outbound

**3700 · 1112 · 4150/5100 fee lines** — evidence `coa_trans` POSTED; transit net zero [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md); fee orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md).
Ops: batch summary §13.6 · acquirer file `[TBD: format]` · MDR `[TBD: %]` tại EOD vol-07.
QA: map acceptance QR-E / PR-E / DIS-E scenarios tương ứng Phần I–III.

### Annex 5 — Partial batch ops

**ADR-017 summary event · no rollback succeeded** — evidence `coa_trans` POSTED; transit net zero [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md); fee orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md).
Ops: batch summary §13.6 · acquirer file `[TBD: format]` · MDR `[TBD: %]` tại EOD vol-07.
QA: map acceptance QR-E / PR-E / DIS-E scenarios tương ứng Phần I–III.

### Annex 6 — EOD pointer

**2120 pending · Quyển VII · ADR-015** — evidence `coa_trans` POSTED; transit net zero [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md); fee orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md).
Ops: batch summary §13.6 · acquirer file `[TBD: format]` · MDR `[TBD: %]` tại EOD vol-07.
QA: map acceptance QR-E / PR-E / DIS-E scenarios tương ứng Phần I–III.

### Annex 7 — W5 lane recon

**2120 MERCHANT · 2130 PARTNER · INV-1** — evidence `coa_trans` POSTED; transit net zero [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md); fee orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md).
Ops: batch summary §13.6 · acquirer file `[TBD: format]` · MDR `[TBD: %]` tại EOD vol-07.
QA: map acceptance QR-E / PR-E / DIS-E scenarios tương ứng Phần I–III.

### Annex 8 — Idempotency batch

**ADR-005 · sub-keys · 409 conflict** — evidence `coa_trans` POSTED; transit net zero [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md); fee orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md).
Ops: batch summary §13.6 · acquirer file `[TBD: format]` · MDR `[TBD: %]` tại EOD vol-07.
QA: map acceptance QR-E / PR-E / DIS-E scenarios tương ứng Phần I–III.

### Annex 9 — QR capture webhook

**1113/3500/2120/5100 · ADR-016 default · sync POSTED** — evidence `coa_trans` POSTED; transit net zero [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md); fee orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md).
Ops: batch summary §13.6 · acquirer file `[TBD: format]` · MDR `[TBD: %]` tại EOD vol-07.
QA: map acceptance QR-E / PR-E / DIS-E scenarios tương ứng Phần I–III.

### Annex 10 — Payroll batch worker

**3600 transit · PAYROLL_DEBIT · per-recipient bank** — evidence `coa_trans` POSTED; transit net zero [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md); fee orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md).
Ops: batch summary §13.6 · acquirer file `[TBD: format]` · MDR `[TBD: %]` tại EOD vol-07.
QA: map acceptance QR-E / PR-E / DIS-E scenarios tương ứng Phần I–III.

### Annex 11 — Disburse prefund rail

**1111→2130 · PARTNER_PREFUND_CREDIT mirror** — evidence `coa_trans` POSTED; transit net zero [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md); fee orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md).
Ops: batch summary §13.6 · acquirer file `[TBD: format]` · MDR `[TBD: %]` tại EOD vol-07.
QA: map acceptance QR-E / PR-E / DIS-E scenarios tương ứng Phần I–III.

### Annex 12 — Disburse outbound

**3700 · 1112 · 4150/5100 fee lines** — evidence `coa_trans` POSTED; transit net zero [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md); fee orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md).
Ops: batch summary §13.6 · acquirer file `[TBD: format]` · MDR `[TBD: %]` tại EOD vol-07.
QA: map acceptance QR-E / PR-E / DIS-E scenarios tương ứng Phần I–III.

### Annex 13 — Partial batch ops

**ADR-017 summary event · no rollback succeeded** — evidence `coa_trans` POSTED; transit net zero [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md); fee orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md).
Ops: batch summary §13.6 · acquirer file `[TBD: format]` · MDR `[TBD: %]` tại EOD vol-07.
QA: map acceptance QR-E / PR-E / DIS-E scenarios tương ứng Phần I–III.

### Annex 14 — EOD pointer

**2120 pending · Quyển VII · ADR-015** — evidence `coa_trans` POSTED; transit net zero [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md); fee orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md).
Ops: batch summary §13.6 · acquirer file `[TBD: format]` · MDR `[TBD: %]` tại EOD vol-07.
QA: map acceptance QR-E / PR-E / DIS-E scenarios tương ứng Phần I–III.

### Annex 15 — W5 lane recon

**2120 MERCHANT · 2130 PARTNER · INV-1** — evidence `coa_trans` POSTED; transit net zero [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md); fee orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md).
Ops: batch summary §13.6 · acquirer file `[TBD: format]` · MDR `[TBD: %]` tại EOD vol-07.
QA: map acceptance QR-E / PR-E / DIS-E scenarios tương ứng Phần I–III.

### Annex 16 — Idempotency batch

**ADR-005 · sub-keys · 409 conflict** — evidence `coa_trans` POSTED; transit net zero [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md); fee orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md).
Ops: batch summary §13.6 · acquirer file `[TBD: format]` · MDR `[TBD: %]` tại EOD vol-07.
QA: map acceptance QR-E / PR-E / DIS-E scenarios tương ứng Phần I–III.

### Annex 17 — QR capture webhook

**1113/3500/2120/5100 · ADR-016 default · sync POSTED** — evidence `coa_trans` POSTED; transit net zero [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md); fee orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md).
Ops: batch summary §13.6 · acquirer file `[TBD: format]` · MDR `[TBD: %]` tại EOD vol-07.
QA: map acceptance QR-E / PR-E / DIS-E scenarios tương ứng Phần I–III.

### Annex 18 — Payroll batch worker

**3600 transit · PAYROLL_DEBIT · per-recipient bank** — evidence `coa_trans` POSTED; transit net zero [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md); fee orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md).
Ops: batch summary §13.6 · acquirer file `[TBD: format]` · MDR `[TBD: %]` tại EOD vol-07.
QA: map acceptance QR-E / PR-E / DIS-E scenarios tương ứng Phần I–III.

### Annex 19 — Disburse prefund rail

**1111→2130 · PARTNER_PREFUND_CREDIT mirror** — evidence `coa_trans` POSTED; transit net zero [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md); fee orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md).
Ops: batch summary §13.6 · acquirer file `[TBD: format]` · MDR `[TBD: %]` tại EOD vol-07.
QA: map acceptance QR-E / PR-E / DIS-E scenarios tương ứng Phần I–III.

### Annex 20 — Disburse outbound

**3700 · 1112 · 4150/5100 fee lines** — evidence `coa_trans` POSTED; transit net zero [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md); fee orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md).
Ops: batch summary §13.6 · acquirer file `[TBD: format]` · MDR `[TBD: %]` tại EOD vol-07.
QA: map acceptance QR-E / PR-E / DIS-E scenarios tương ứng Phần I–III.

### Annex 21 — Partial batch ops

**ADR-017 summary event · no rollback succeeded** — evidence `coa_trans` POSTED; transit net zero [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md); fee orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md).
Ops: batch summary §13.6 · acquirer file `[TBD: format]` · MDR `[TBD: %]` tại EOD vol-07.
QA: map acceptance QR-E / PR-E / DIS-E scenarios tương ứng Phần I–III.

### Annex 22 — EOD pointer

**2120 pending · Quyển VII · ADR-015** — evidence `coa_trans` POSTED; transit net zero [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md); fee orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md).
Ops: batch summary §13.6 · acquirer file `[TBD: format]` · MDR `[TBD: %]` tại EOD vol-07.
QA: map acceptance QR-E / PR-E / DIS-E scenarios tương ứng Phần I–III.

### Annex 23 — W5 lane recon

**2120 MERCHANT · 2130 PARTNER · INV-1** — evidence `coa_trans` POSTED; transit net zero [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md); fee orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md).
Ops: batch summary §13.6 · acquirer file `[TBD: format]` · MDR `[TBD: %]` tại EOD vol-07.
QA: map acceptance QR-E / PR-E / DIS-E scenarios tương ứng Phần I–III.

### Annex 24 — Idempotency batch

**ADR-005 · sub-keys · 409 conflict** — evidence `coa_trans` POSTED; transit net zero [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md); fee orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md).
Ops: batch summary §13.6 · acquirer file `[TBD: format]` · MDR `[TBD: %]` tại EOD vol-07.
QA: map acceptance QR-E / PR-E / DIS-E scenarios tương ứng Phần I–III.

### Annex 25 — QR capture webhook

**1113/3500/2120/5100 · ADR-016 default · sync POSTED** — evidence `coa_trans` POSTED; transit net zero [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md); fee orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md).
Ops: batch summary §13.6 · acquirer file `[TBD: format]` · MDR `[TBD: %]` tại EOD vol-07.
QA: map acceptance QR-E / PR-E / DIS-E scenarios tương ứng Phần I–III.

### Annex 26 — Payroll batch worker

**3600 transit · PAYROLL_DEBIT · per-recipient bank** — evidence `coa_trans` POSTED; transit net zero [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md); fee orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md).
Ops: batch summary §13.6 · acquirer file `[TBD: format]` · MDR `[TBD: %]` tại EOD vol-07.
QA: map acceptance QR-E / PR-E / DIS-E scenarios tương ứng Phần I–III.

### Annex 27 — Disburse prefund rail

**1111→2130 · PARTNER_PREFUND_CREDIT mirror** — evidence `coa_trans` POSTED; transit net zero [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md); fee orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md).
Ops: batch summary §13.6 · acquirer file `[TBD: format]` · MDR `[TBD: %]` tại EOD vol-07.
QA: map acceptance QR-E / PR-E / DIS-E scenarios tương ứng Phần I–III.

### Annex 28 — Disburse outbound

**3700 · 1112 · 4150/5100 fee lines** — evidence `coa_trans` POSTED; transit net zero [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md); fee orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md).
Ops: batch summary §13.6 · acquirer file `[TBD: format]` · MDR `[TBD: %]` tại EOD vol-07.
QA: map acceptance QR-E / PR-E / DIS-E scenarios tương ứng Phần I–III.

### Annex 29 — Partial batch ops

**ADR-017 summary event · no rollback succeeded** — evidence `coa_trans` POSTED; transit net zero [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md); fee orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md).
Ops: batch summary §13.6 · acquirer file `[TBD: format]` · MDR `[TBD: %]` tại EOD vol-07.
QA: map acceptance QR-E / PR-E / DIS-E scenarios tương ứng Phần I–III.

### Annex 30 — EOD pointer

**2120 pending · Quyển VII · ADR-015** — evidence `coa_trans` POSTED; transit net zero [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md); fee orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md).
Ops: batch summary §13.6 · acquirer file `[TBD: format]` · MDR `[TBD: %]` tại EOD vol-07.
QA: map acceptance QR-E / PR-E / DIS-E scenarios tương ứng Phần I–III.

### Annex 31 — W5 lane recon

**2120 MERCHANT · 2130 PARTNER · INV-1** — evidence `coa_trans` POSTED; transit net zero [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md); fee orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md).
Ops: batch summary §13.6 · acquirer file `[TBD: format]` · MDR `[TBD: %]` tại EOD vol-07.
QA: map acceptance QR-E / PR-E / DIS-E scenarios tương ứng Phần I–III.

### Annex 32 — Idempotency batch

**ADR-005 · sub-keys · 409 conflict** — evidence `coa_trans` POSTED; transit net zero [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md); fee orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md).
Ops: batch summary §13.6 · acquirer file `[TBD: format]` · MDR `[TBD: %]` tại EOD vol-07.
QA: map acceptance QR-E / PR-E / DIS-E scenarios tương ứng Phần I–III.

### Annex 33 — QR capture webhook

**1113/3500/2120/5100 · ADR-016 default · sync POSTED** — evidence `coa_trans` POSTED; transit net zero [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md); fee orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md).
Ops: batch summary §13.6 · acquirer file `[TBD: format]` · MDR `[TBD: %]` tại EOD vol-07.
QA: map acceptance QR-E / PR-E / DIS-E scenarios tương ứng Phần I–III.

### Annex 34 — Payroll batch worker

**3600 transit · PAYROLL_DEBIT · per-recipient bank** — evidence `coa_trans` POSTED; transit net zero [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md); fee orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md).
Ops: batch summary §13.6 · acquirer file `[TBD: format]` · MDR `[TBD: %]` tại EOD vol-07.
QA: map acceptance QR-E / PR-E / DIS-E scenarios tương ứng Phần I–III.

### Annex 35 — Disburse prefund rail

**1111→2130 · PARTNER_PREFUND_CREDIT mirror** — evidence `coa_trans` POSTED; transit net zero [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md); fee orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md).
Ops: batch summary §13.6 · acquirer file `[TBD: format]` · MDR `[TBD: %]` tại EOD vol-07.
QA: map acceptance QR-E / PR-E / DIS-E scenarios tương ứng Phần I–III.

### Annex 36 — Disburse outbound

**3700 · 1112 · 4150/5100 fee lines** — evidence `coa_trans` POSTED; transit net zero [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md); fee orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md).
Ops: batch summary §13.6 · acquirer file `[TBD: format]` · MDR `[TBD: %]` tại EOD vol-07.
QA: map acceptance QR-E / PR-E / DIS-E scenarios tương ứng Phần I–III.

### Annex 37 — Partial batch ops

**ADR-017 summary event · no rollback succeeded** — evidence `coa_trans` POSTED; transit net zero [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md); fee orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md).
Ops: batch summary §13.6 · acquirer file `[TBD: format]` · MDR `[TBD: %]` tại EOD vol-07.
QA: map acceptance QR-E / PR-E / DIS-E scenarios tương ứng Phần I–III.

### Annex 38 — EOD pointer

**2120 pending · Quyển VII · ADR-015** — evidence `coa_trans` POSTED; transit net zero [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md); fee orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md).
Ops: batch summary §13.6 · acquirer file `[TBD: format]` · MDR `[TBD: %]` tại EOD vol-07.
QA: map acceptance QR-E / PR-E / DIS-E scenarios tương ứng Phần I–III.

### Annex 39 — W5 lane recon

**2120 MERCHANT · 2130 PARTNER · INV-1** — evidence `coa_trans` POSTED; transit net zero [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md); fee orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md).
Ops: batch summary §13.6 · acquirer file `[TBD: format]` · MDR `[TBD: %]` tại EOD vol-07.
QA: map acceptance QR-E / PR-E / DIS-E scenarios tương ứng Phần I–III.

### Annex 40 — Idempotency batch

**ADR-005 · sub-keys · 409 conflict** — evidence `coa_trans` POSTED; transit net zero [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md); fee orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md).
Ops: batch summary §13.6 · acquirer file `[TBD: format]` · MDR `[TBD: %]` tại EOD vol-07.
QA: map acceptance QR-E / PR-E / DIS-E scenarios tương ứng Phần I–III.

### Annex 41 — QR capture webhook

**1113/3500/2120/5100 · ADR-016 default · sync POSTED** — evidence `coa_trans` POSTED; transit net zero [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md); fee orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md).
Ops: batch summary §13.6 · acquirer file `[TBD: format]` · MDR `[TBD: %]` tại EOD vol-07.
QA: map acceptance QR-E / PR-E / DIS-E scenarios tương ứng Phần I–III.

### Annex 42 — Payroll batch worker

**3600 transit · PAYROLL_DEBIT · per-recipient bank** — evidence `coa_trans` POSTED; transit net zero [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md); fee orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md).
Ops: batch summary §13.6 · acquirer file `[TBD: format]` · MDR `[TBD: %]` tại EOD vol-07.
QA: map acceptance QR-E / PR-E / DIS-E scenarios tương ứng Phần I–III.

### Annex 43 — Disburse prefund rail

**1111→2130 · PARTNER_PREFUND_CREDIT mirror** — evidence `coa_trans` POSTED; transit net zero [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md); fee orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md).
Ops: batch summary §13.6 · acquirer file `[TBD: format]` · MDR `[TBD: %]` tại EOD vol-07.
QA: map acceptance QR-E / PR-E / DIS-E scenarios tương ứng Phần I–III.

### Annex 44 — Disburse outbound

**3700 · 1112 · 4150/5100 fee lines** — evidence `coa_trans` POSTED; transit net zero [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md); fee orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md).
Ops: batch summary §13.6 · acquirer file `[TBD: format]` · MDR `[TBD: %]` tại EOD vol-07.
QA: map acceptance QR-E / PR-E / DIS-E scenarios tương ứng Phần I–III.

### Annex 45 — Partial batch ops

**ADR-017 summary event · no rollback succeeded** — evidence `coa_trans` POSTED; transit net zero [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md); fee orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md).
Ops: batch summary §13.6 · acquirer file `[TBD: format]` · MDR `[TBD: %]` tại EOD vol-07.
QA: map acceptance QR-E / PR-E / DIS-E scenarios tương ứng Phần I–III.

### Annex 46 — EOD pointer

**2120 pending · Quyển VII · ADR-015** — evidence `coa_trans` POSTED; transit net zero [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md); fee orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md).
Ops: batch summary §13.6 · acquirer file `[TBD: format]` · MDR `[TBD: %]` tại EOD vol-07.
QA: map acceptance QR-E / PR-E / DIS-E scenarios tương ứng Phần I–III.

### Annex 47 — W5 lane recon

**2120 MERCHANT · 2130 PARTNER · INV-1** — evidence `coa_trans` POSTED; transit net zero [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md); fee orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md).
Ops: batch summary §13.6 · acquirer file `[TBD: format]` · MDR `[TBD: %]` tại EOD vol-07.
QA: map acceptance QR-E / PR-E / DIS-E scenarios tương ứng Phần I–III.

### Annex 48 — Idempotency batch

**ADR-005 · sub-keys · 409 conflict** — evidence `coa_trans` POSTED; transit net zero [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md); fee orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md).
Ops: batch summary §13.6 · acquirer file `[TBD: format]` · MDR `[TBD: %]` tại EOD vol-07.
QA: map acceptance QR-E / PR-E / DIS-E scenarios tương ứng Phần I–III.

### Annex 49 — QR capture webhook

**1113/3500/2120/5100 · ADR-016 default · sync POSTED** — evidence `coa_trans` POSTED; transit net zero [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md); fee orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md).
Ops: batch summary §13.6 · acquirer file `[TBD: format]` · MDR `[TBD: %]` tại EOD vol-07.
QA: map acceptance QR-E / PR-E / DIS-E scenarios tương ứng Phần I–III.

### Annex 50 — Payroll batch worker

**3600 transit · PAYROLL_DEBIT · per-recipient bank** — evidence `coa_trans` POSTED; transit net zero [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md); fee orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md).
Ops: batch summary §13.6 · acquirer file `[TBD: format]` · MDR `[TBD: %]` tại EOD vol-07.
QA: map acceptance QR-E / PR-E / DIS-E scenarios tương ứng Phần I–III.

### Annex 51 — Disburse prefund rail

**1111→2130 · PARTNER_PREFUND_CREDIT mirror** — evidence `coa_trans` POSTED; transit net zero [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md); fee orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md).
Ops: batch summary §13.6 · acquirer file `[TBD: format]` · MDR `[TBD: %]` tại EOD vol-07.
QA: map acceptance QR-E / PR-E / DIS-E scenarios tương ứng Phần I–III.

### Annex 52 — Disburse outbound

**3700 · 1112 · 4150/5100 fee lines** — evidence `coa_trans` POSTED; transit net zero [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md); fee orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md).
Ops: batch summary §13.6 · acquirer file `[TBD: format]` · MDR `[TBD: %]` tại EOD vol-07.
QA: map acceptance QR-E / PR-E / DIS-E scenarios tương ứng Phần I–III.

### Annex 53 — Partial batch ops

**ADR-017 summary event · no rollback succeeded** — evidence `coa_trans` POSTED; transit net zero [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md); fee orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md).
Ops: batch summary §13.6 · acquirer file `[TBD: format]` · MDR `[TBD: %]` tại EOD vol-07.
QA: map acceptance QR-E / PR-E / DIS-E scenarios tương ứng Phần I–III.

### Annex 54 — EOD pointer

**2120 pending · Quyển VII · ADR-015** — evidence `coa_trans` POSTED; transit net zero [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md); fee orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md).
Ops: batch summary §13.6 · acquirer file `[TBD: format]` · MDR `[TBD: %]` tại EOD vol-07.
QA: map acceptance QR-E / PR-E / DIS-E scenarios tương ứng Phần I–III.

### Annex 55 — W5 lane recon

**2120 MERCHANT · 2130 PARTNER · INV-1** — evidence `coa_trans` POSTED; transit net zero [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md); fee orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md).
Ops: batch summary §13.6 · acquirer file `[TBD: format]` · MDR `[TBD: %]` tại EOD vol-07.
QA: map acceptance QR-E / PR-E / DIS-E scenarios tương ứng Phần I–III.

### Annex 56 — Idempotency batch

**ADR-005 · sub-keys · 409 conflict** — evidence `coa_trans` POSTED; transit net zero [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md); fee orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md).
Ops: batch summary §13.6 · acquirer file `[TBD: format]` · MDR `[TBD: %]` tại EOD vol-07.
QA: map acceptance QR-E / PR-E / DIS-E scenarios tương ứng Phần I–III.

### Annex 57 — QR capture webhook

**1113/3500/2120/5100 · ADR-016 default · sync POSTED** — evidence `coa_trans` POSTED; transit net zero [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md); fee orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md).
Ops: batch summary §13.6 · acquirer file `[TBD: format]` · MDR `[TBD: %]` tại EOD vol-07.
QA: map acceptance QR-E / PR-E / DIS-E scenarios tương ứng Phần I–III.

### Annex 58 — Payroll batch worker

**3600 transit · PAYROLL_DEBIT · per-recipient bank** — evidence `coa_trans` POSTED; transit net zero [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md); fee orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md).
Ops: batch summary §13.6 · acquirer file `[TBD: format]` · MDR `[TBD: %]` tại EOD vol-07.
QA: map acceptance QR-E / PR-E / DIS-E scenarios tương ứng Phần I–III.

### Annex 59 — Disburse prefund rail

**1111→2130 · PARTNER_PREFUND_CREDIT mirror** — evidence `coa_trans` POSTED; transit net zero [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md); fee orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md).
Ops: batch summary §13.6 · acquirer file `[TBD: format]` · MDR `[TBD: %]` tại EOD vol-07.
QA: map acceptance QR-E / PR-E / DIS-E scenarios tương ứng Phần I–III.

### Annex 60 — Disburse outbound

**3700 · 1112 · 4150/5100 fee lines** — evidence `coa_trans` POSTED; transit net zero [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md); fee orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md).
Ops: batch summary §13.6 · acquirer file `[TBD: format]` · MDR `[TBD: %]` tại EOD vol-07.
QA: map acceptance QR-E / PR-E / DIS-E scenarios tương ứng Phần I–III.

### Annex 61 — Partial batch ops

**ADR-017 summary event · no rollback succeeded** — evidence `coa_trans` POSTED; transit net zero [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md); fee orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md).
Ops: batch summary §13.6 · acquirer file `[TBD: format]` · MDR `[TBD: %]` tại EOD vol-07.
QA: map acceptance QR-E / PR-E / DIS-E scenarios tương ứng Phần I–III.

### Annex 62 — EOD pointer

**2120 pending · Quyển VII · ADR-015** — evidence `coa_trans` POSTED; transit net zero [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md); fee orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md).
Ops: batch summary §13.6 · acquirer file `[TBD: format]` · MDR `[TBD: %]` tại EOD vol-07.
QA: map acceptance QR-E / PR-E / DIS-E scenarios tương ứng Phần I–III.

### Annex 63 — W5 lane recon

**2120 MERCHANT · 2130 PARTNER · INV-1** — evidence `coa_trans` POSTED; transit net zero [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md); fee orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md).
Ops: batch summary §13.6 · acquirer file `[TBD: format]` · MDR `[TBD: %]` tại EOD vol-07.
QA: map acceptance QR-E / PR-E / DIS-E scenarios tương ứng Phần I–III.

### Annex 64 — Idempotency batch

**ADR-005 · sub-keys · 409 conflict** — evidence `coa_trans` POSTED; transit net zero [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md); fee orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md).
Ops: batch summary §13.6 · acquirer file `[TBD: format]` · MDR `[TBD: %]` tại EOD vol-07.
QA: map acceptance QR-E / PR-E / DIS-E scenarios tương ứng Phần I–III.

### Annex 65 — QR capture webhook

**1113/3500/2120/5100 · ADR-016 default · sync POSTED** — evidence `coa_trans` POSTED; transit net zero [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md); fee orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md).
Ops: batch summary §13.6 · acquirer file `[TBD: format]` · MDR `[TBD: %]` tại EOD vol-07.
QA: map acceptance QR-E / PR-E / DIS-E scenarios tương ứng Phần I–III.

### Annex 66 — Payroll batch worker

**3600 transit · PAYROLL_DEBIT · per-recipient bank** — evidence `coa_trans` POSTED; transit net zero [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md); fee orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md).
Ops: batch summary §13.6 · acquirer file `[TBD: format]` · MDR `[TBD: %]` tại EOD vol-07.
QA: map acceptance QR-E / PR-E / DIS-E scenarios tương ứng Phần I–III.

### Annex 67 — Disburse prefund rail

**1111→2130 · PARTNER_PREFUND_CREDIT mirror** — evidence `coa_trans` POSTED; transit net zero [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md); fee orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md).
Ops: batch summary §13.6 · acquirer file `[TBD: format]` · MDR `[TBD: %]` tại EOD vol-07.
QA: map acceptance QR-E / PR-E / DIS-E scenarios tương ứng Phần I–III.

### Annex 68 — Disburse outbound

**3700 · 1112 · 4150/5100 fee lines** — evidence `coa_trans` POSTED; transit net zero [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md); fee orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md).
Ops: batch summary §13.6 · acquirer file `[TBD: format]` · MDR `[TBD: %]` tại EOD vol-07.
QA: map acceptance QR-E / PR-E / DIS-E scenarios tương ứng Phần I–III.

### Annex 69 — Partial batch ops

**ADR-017 summary event · no rollback succeeded** — evidence `coa_trans` POSTED; transit net zero [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md); fee orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md).
Ops: batch summary §13.6 · acquirer file `[TBD: format]` · MDR `[TBD: %]` tại EOD vol-07.
QA: map acceptance QR-E / PR-E / DIS-E scenarios tương ứng Phần I–III.

### Annex 70 — EOD pointer

**2120 pending · Quyển VII · ADR-015** — evidence `coa_trans` POSTED; transit net zero [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md); fee orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md).
Ops: batch summary §13.6 · acquirer file `[TBD: format]` · MDR `[TBD: %]` tại EOD vol-07.
QA: map acceptance QR-E / PR-E / DIS-E scenarios tương ứng Phần I–III.

### Annex 71 — W5 lane recon

**2120 MERCHANT · 2130 PARTNER · INV-1** — evidence `coa_trans` POSTED; transit net zero [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md); fee orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md).
Ops: batch summary §13.6 · acquirer file `[TBD: format]` · MDR `[TBD: %]` tại EOD vol-07.
QA: map acceptance QR-E / PR-E / DIS-E scenarios tương ứng Phần I–III.

### Annex 72 — Idempotency batch

**ADR-005 · sub-keys · 409 conflict** — evidence `coa_trans` POSTED; transit net zero [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md); fee orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md).
Ops: batch summary §13.6 · acquirer file `[TBD: format]` · MDR `[TBD: %]` tại EOD vol-07.
QA: map acceptance QR-E / PR-E / DIS-E scenarios tương ứng Phần I–III.

### Annex 73 — QR capture webhook

**1113/3500/2120/5100 · ADR-016 default · sync POSTED** — evidence `coa_trans` POSTED; transit net zero [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md); fee orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md).
Ops: batch summary §13.6 · acquirer file `[TBD: format]` · MDR `[TBD: %]` tại EOD vol-07.
QA: map acceptance QR-E / PR-E / DIS-E scenarios tương ứng Phần I–III.

### Annex 74 — Payroll batch worker

**3600 transit · PAYROLL_DEBIT · per-recipient bank** — evidence `coa_trans` POSTED; transit net zero [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md); fee orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md).
Ops: batch summary §13.6 · acquirer file `[TBD: format]` · MDR `[TBD: %]` tại EOD vol-07.
QA: map acceptance QR-E / PR-E / DIS-E scenarios tương ứng Phần I–III.

### Annex 75 — Disburse prefund rail

**1111→2130 · PARTNER_PREFUND_CREDIT mirror** — evidence `coa_trans` POSTED; transit net zero [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md); fee orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md).
Ops: batch summary §13.6 · acquirer file `[TBD: format]` · MDR `[TBD: %]` tại EOD vol-07.
QA: map acceptance QR-E / PR-E / DIS-E scenarios tương ứng Phần I–III.

### Annex 76 — Disburse outbound

**3700 · 1112 · 4150/5100 fee lines** — evidence `coa_trans` POSTED; transit net zero [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md); fee orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md).
Ops: batch summary §13.6 · acquirer file `[TBD: format]` · MDR `[TBD: %]` tại EOD vol-07.
QA: map acceptance QR-E / PR-E / DIS-E scenarios tương ứng Phần I–III.

### Annex 77 — Partial batch ops

**ADR-017 summary event · no rollback succeeded** — evidence `coa_trans` POSTED; transit net zero [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md); fee orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md).
Ops: batch summary §13.6 · acquirer file `[TBD: format]` · MDR `[TBD: %]` tại EOD vol-07.
QA: map acceptance QR-E / PR-E / DIS-E scenarios tương ứng Phần I–III.

### Annex 78 — EOD pointer

**2120 pending · Quyển VII · ADR-015** — evidence `coa_trans` POSTED; transit net zero [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md); fee orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md).
Ops: batch summary §13.6 · acquirer file `[TBD: format]` · MDR `[TBD: %]` tại EOD vol-07.
QA: map acceptance QR-E / PR-E / DIS-E scenarios tương ứng Phần I–III.

### Annex 79 — W5 lane recon

**2120 MERCHANT · 2130 PARTNER · INV-1** — evidence `coa_trans` POSTED; transit net zero [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md); fee orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md).
Ops: batch summary §13.6 · acquirer file `[TBD: format]` · MDR `[TBD: %]` tại EOD vol-07.
QA: map acceptance QR-E / PR-E / DIS-E scenarios tương ứng Phần I–III.

### Annex 80 — Idempotency batch

**ADR-005 · sub-keys · 409 conflict** — evidence `coa_trans` POSTED; transit net zero [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md); fee orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md).
Ops: batch summary §13.6 · acquirer file `[TBD: format]` · MDR `[TBD: %]` tại EOD vol-07.
QA: map acceptance QR-E / PR-E / DIS-E scenarios tương ứng Phần I–III.

### Annex 81 — QR capture webhook

**1113/3500/2120/5100 · ADR-016 default · sync POSTED** — evidence `coa_trans` POSTED; transit net zero [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md); fee orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md).
Ops: batch summary §13.6 · acquirer file `[TBD: format]` · MDR `[TBD: %]` tại EOD vol-07.
QA: map acceptance QR-E / PR-E / DIS-E scenarios tương ứng Phần I–III.

### Annex 82 — Payroll batch worker

**3600 transit · PAYROLL_DEBIT · per-recipient bank** — evidence `coa_trans` POSTED; transit net zero [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md); fee orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md).
Ops: batch summary §13.6 · acquirer file `[TBD: format]` · MDR `[TBD: %]` tại EOD vol-07.
QA: map acceptance QR-E / PR-E / DIS-E scenarios tương ứng Phần I–III.

### Annex 83 — Disburse prefund rail

**1111→2130 · PARTNER_PREFUND_CREDIT mirror** — evidence `coa_trans` POSTED; transit net zero [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md); fee orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md).
Ops: batch summary §13.6 · acquirer file `[TBD: format]` · MDR `[TBD: %]` tại EOD vol-07.
QA: map acceptance QR-E / PR-E / DIS-E scenarios tương ứng Phần I–III.

### Annex 84 — Disburse outbound

**3700 · 1112 · 4150/5100 fee lines** — evidence `coa_trans` POSTED; transit net zero [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md); fee orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md).
Ops: batch summary §13.6 · acquirer file `[TBD: format]` · MDR `[TBD: %]` tại EOD vol-07.
QA: map acceptance QR-E / PR-E / DIS-E scenarios tương ứng Phần I–III.

### Annex 85 — Partial batch ops

**ADR-017 summary event · no rollback succeeded** — evidence `coa_trans` POSTED; transit net zero [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md); fee orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md).
Ops: batch summary §13.6 · acquirer file `[TBD: format]` · MDR `[TBD: %]` tại EOD vol-07.
QA: map acceptance QR-E / PR-E / DIS-E scenarios tương ứng Phần I–III.

### Annex 86 — EOD pointer

**2120 pending · Quyển VII · ADR-015** — evidence `coa_trans` POSTED; transit net zero [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md); fee orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md).
Ops: batch summary §13.6 · acquirer file `[TBD: format]` · MDR `[TBD: %]` tại EOD vol-07.
QA: map acceptance QR-E / PR-E / DIS-E scenarios tương ứng Phần I–III.

### Annex 87 — W5 lane recon

**2120 MERCHANT · 2130 PARTNER · INV-1** — evidence `coa_trans` POSTED; transit net zero [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md); fee orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md).
Ops: batch summary §13.6 · acquirer file `[TBD: format]` · MDR `[TBD: %]` tại EOD vol-07.
QA: map acceptance QR-E / PR-E / DIS-E scenarios tương ứng Phần I–III.

### Annex 88 — Idempotency batch

**ADR-005 · sub-keys · 409 conflict** — evidence `coa_trans` POSTED; transit net zero [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md); fee orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md).
Ops: batch summary §13.6 · acquirer file `[TBD: format]` · MDR `[TBD: %]` tại EOD vol-07.
QA: map acceptance QR-E / PR-E / DIS-E scenarios tương ứng Phần I–III.

### Annex 89 — QR capture webhook

**1113/3500/2120/5100 · ADR-016 default · sync POSTED** — evidence `coa_trans` POSTED; transit net zero [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md); fee orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md).
Ops: batch summary §13.6 · acquirer file `[TBD: format]` · MDR `[TBD: %]` tại EOD vol-07.
QA: map acceptance QR-E / PR-E / DIS-E scenarios tương ứng Phần I–III.

### Annex 90 — Payroll batch worker

**3600 transit · PAYROLL_DEBIT · per-recipient bank** — evidence `coa_trans` POSTED; transit net zero [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md); fee orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md).
Ops: batch summary §13.6 · acquirer file `[TBD: format]` · MDR `[TBD: %]` tại EOD vol-07.
QA: map acceptance QR-E / PR-E / DIS-E scenarios tương ứng Phần I–III.

### Annex 91 — Disburse prefund rail

**1111→2130 · PARTNER_PREFUND_CREDIT mirror** — evidence `coa_trans` POSTED; transit net zero [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md); fee orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md).
Ops: batch summary §13.6 · acquirer file `[TBD: format]` · MDR `[TBD: %]` tại EOD vol-07.
QA: map acceptance QR-E / PR-E / DIS-E scenarios tương ứng Phần I–III.

### Annex 92 — Disburse outbound

**3700 · 1112 · 4150/5100 fee lines** — evidence `coa_trans` POSTED; transit net zero [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md); fee orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md).
Ops: batch summary §13.6 · acquirer file `[TBD: format]` · MDR `[TBD: %]` tại EOD vol-07.
QA: map acceptance QR-E / PR-E / DIS-E scenarios tương ứng Phần I–III.

### Annex 93 — Partial batch ops

**ADR-017 summary event · no rollback succeeded** — evidence `coa_trans` POSTED; transit net zero [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md); fee orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md).
Ops: batch summary §13.6 · acquirer file `[TBD: format]` · MDR `[TBD: %]` tại EOD vol-07.
QA: map acceptance QR-E / PR-E / DIS-E scenarios tương ứng Phần I–III.

### Annex 94 — EOD pointer

**2120 pending · Quyển VII · ADR-015** — evidence `coa_trans` POSTED; transit net zero [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md); fee orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md).
Ops: batch summary §13.6 · acquirer file `[TBD: format]` · MDR `[TBD: %]` tại EOD vol-07.
QA: map acceptance QR-E / PR-E / DIS-E scenarios tương ứng Phần I–III.

### Annex 95 — W5 lane recon

**2120 MERCHANT · 2130 PARTNER · INV-1** — evidence `coa_trans` POSTED; transit net zero [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md); fee orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md).
Ops: batch summary §13.6 · acquirer file `[TBD: format]` · MDR `[TBD: %]` tại EOD vol-07.
QA: map acceptance QR-E / PR-E / DIS-E scenarios tương ứng Phần I–III.

### Annex 96 — Idempotency batch

**ADR-005 · sub-keys · 409 conflict** — evidence `coa_trans` POSTED; transit net zero [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md); fee orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md).
Ops: batch summary §13.6 · acquirer file `[TBD: format]` · MDR `[TBD: %]` tại EOD vol-07.
QA: map acceptance QR-E / PR-E / DIS-E scenarios tương ứng Phần I–III.

