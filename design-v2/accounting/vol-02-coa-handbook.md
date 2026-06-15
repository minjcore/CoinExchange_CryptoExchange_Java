# Quyển II — Sổ tay Hệ thống tài khoản (COA)

**Trạng thái:** Bản chuẩn (~120 trang in) · **Phạm vi:** `core.accounting` (`coa_*` only) · **Ngôn ngữ:** Tiếng Việt  
**Đối tượng đọc:** Kế toán nội bộ, backend, ops, product, kiểm toán  
**Số tiền ví dụ chuẩn:** gross **100.000** VND · phí dịch vụ **1.000** VND · chi phí Napas/bank **500** VND · scale **4** HALF_UP ([ADR-028](../../adr/ADR-028-money-scale-four-half-up.md))

**Liên kết:** [`foundation.md`](../../spec/foundation.md) § 6–16 · [`adr/`](../../adr/) · [`vol-01-principles.md`](vol-01-principles.md) · [`vol-03-deposit.md`](vol-03-deposit.md) · [`postings.md`](../../design/accounting/postings.md)

---

## Mục lục chi tiết

### Phần A — Dẫn nhập COA

| § | Tiêu đề | Trang ước |
|---|---------|-----------|
| [0](#chương-0-cách-đọc-sổ-tay--quan-hệ-quyển-i) | Cách đọc sổ tay & quan hệ Quyển I | 4 |
| [COA-P](#chương-coa-pairs-ma-trận-cặp-bắt-buộc) | Ma trận cặp tài khoản bắt buộc (COA-Pairs) | 6 |
| [TB](#chương-tb-trial-balance--kiểm-tra-cân) | Trial balance & kiểm tra cân | 5 |
| [DDL](#chương-ddl-seed--ddl-coa_account) | Seed & DDL `coa_account` | 5 |
| [VAS](#chương-vas-mapping-tt200vas-informative) | Mapping TT200/VAS → COA nội bộ (informative) | 4 |
| [CK](#chương-ck-review-checklist-40-mục) | Review checklist 40 mục trước release | 5 |
| [FAQ-COA](#chương-faq-coa-40-câu-hỏi) | FAQ COA 40 câu hỏi | 8 |

### Phần B — 23 Tài khoản chi tiết

| # | Mã | Tên tài khoản | Trang ước |
|---|-----|---------------|-----------|
| B.1 | [1111](#b1-tk-1111--tiền-gửi-vietinbank-chuyên-dụng) | Tiền gửi Vietinbank chuyên dụng | 5 |
| B.2 | [1112](#b2-tk-1112--napas-clearing) | Napas Clearing | 5 |
| B.3 | [1113](#b3-tk-1113--vpbank-qrpos-acquirer) | VPBank QR/POS acquirer | 5 |
| B.4 | [2110](#b4-tk-2110--wallet-payable--user) | Wallet payable — User | 5 |
| B.5 | [2120](#b5-tk-2120--wallet-payable--merchant) | Wallet payable — Merchant | 5 |
| B.6 | [2130](#b6-tk-2130--escrow-payable--disbursement-partner) | Escrow payable — Disbursement partner | 5 |
| B.7 | [3100](#b7-tk-3100--transit-deposit) | Transit — Deposit | 5 |
| B.8 | [3200](#b8-tk-3200--transit-withdraw) | Transit — Withdraw | 5 |
| B.9 | [3300](#b9-tk-3300--transit-internal-transfer) | Transit — Internal transfer | 5 |
| B.10 | [3400](#b10-tk-3400--transit-ibft) | Transit — IBFT | 5 |
| B.11 | [3500](#b11-tk-3500--transit-payment) | Transit — Payment | 5 |
| B.12 | [3600](#b12-tk-3600--transit-payroll) | Transit — Payroll | 5 |
| B.13 | [3700](#b13-tk-3700--transit-disbursement) | Transit — Disbursement | 5 |
| B.14 | [3800](#b14-tk-3800--transit-clearing-lock) | Transit — Clearing lock | 5 |
| B.15 | [3810](#b15-tk-3810--transit-settlement-outbound) | Transit — Settlement outbound | 5 |
| B.16 | [3820](#b16-tk-3820--transit-mdr-holdback) | Transit — MDR holdback | 5 |
| B.17 | [4110](#b17-tk-4110--doanh-thu-phí-nạp-tiền) | Doanh thu phí nạp tiền | 5 |
| B.18 | [4120](#b18-tk-4120--doanh-thu-phí-rút-tiền) | Doanh thu phí rút tiền | 5 |
| B.19 | [4130](#b19-tk-4130--doanh-thu-phí-chuyển-khoản) | Doanh thu phí chuyển khoản | 5 |
| B.20 | [4140](#b20-tk-4140--doanh-thu-mdr-merchant) | Doanh thu MDR merchant | 5 |
| B.21 | [4150](#b21-tk-4150--doanh-thu-phí-payrolldisbursement) | Doanh thu phí payroll/disbursement | 5 |
| B.22 | [5100](#b22-tk-5100--chi-phí-banknapas) | Chi phí bank/Napas | 5 |
| B.23 | [6000](#b23-tk-6000--vốn-chủ-sở-hữu) | Vốn chủ sở hữu | 5 |

### Phần C — Phụ lục

| # | Tiêu đề | Trang ước |
|---|---------|-----------|
| [A](#phụ-lục-a-ma-trận-tk--use_case-mở-rộng) | Ma trận TK × use_case mở rộng | 4 |
| [B](#phụ-lục-b-quick-reference-card) | Quick reference card vận hành | 3 |
| [Đọc tiếp](#đọc-tiếp) | Liên kết corpus | 1 |

---

## Chương 0. Cách đọc sổ tay & quan hệ Quyển I

### 0.1 Mục tiêu Quyển II

Quyển II là **handbook tác nghiệp**: mỗi tài khoản COA được mô tả đầy đủ — từ bản chất kinh tế, T-account, DR/CR theo use case, lỗi thường gặp, cho đến ADR binding và câu hỏi nhanh. Quyển I ([`vol-01-principles.md`](vol-01-principles.md)) giải thích *vì sao*; Quyển II giải thích *làm thế nào*.

| Vai trò | Đọc theo thứ tự | Kết quả mong đợi |
|---------|----------------|------------------|
| **Product** | Ch0 → COA-Pairs → FAQ-COA | Biết `[TBD]` cần chốt, không bịa rule kế toán |
| **Backend** | COA-Pairs → DDL → từng TK mình cần | Build journal đúng cặp, đúng transit |
| **Accounting / Ops** | TB → CK → từng TK liên quan → Phụ lục A | Soát lệch, xử lý period close |
| **Audit** | ADR binding từng TK → checklist CK | Trace evidence theo control, FR-10 |

### 0.2 Quan hệ với Quyển I

```
Quyển I (nguyên tắc)          Quyển II (COA handbook)
─────────────────────          ──────────────────────────
Ch 1: Domain accounting   →    Ch 0: Scope & boundaries
Ch 2: Phương trình         →    Phụ lục A: TK × use_case matrix
Ch 3: DR/CR masterclass    →    Phần B: T-account từng TK
Ch 9: Triết lý transit     →    B.7–B.16: 10 TK transit 3100–3820
Ch 18: FR-10/W5            →    Mục "Recon FR-10/W5" từng TK
```

### 0.3 Quy ước trong file này

- **`[TBD: ...]`** — rule sản phẩm chưa chốt; KHÔNG ngầm định số liệu.
- **E01–E25** — tham chiếu ví dụ trong Quyển I §3.4.
- **foundation §8–16** — tham chiếu [`foundation.md`](../../spec/foundation.md).
- **ADR-006, ADR-010, ADR-014, ADR-020, ADR-025, ADR-028** — quyết định kiến trúc liên quan.
- **FR-10** = functional requirement đối soát ngân hàng · **W5** = wallet–COA reconciliation.
- Số dư bình thường: **Nợ** = tài sản/chi phí tăng bằng DR; **Có** = nợ/doanh thu/vốn tăng bằng CR.

### 0.4 Cấu trúc mỗi phần tài khoản

Mỗi trong 23 tài khoản có 11 tiểu mục cố định:
1. Metadata (bảng) · 2. Định nghĩa kinh tế · 3. T-account ASCII
4. Bảng DR/CR theo use case · 5. Cặp đối ứng bắt buộc · 6. Trạng thái PENDING/POSTED
7. Lỗi thường gặp (≥5) · 8. Recon FR-10/W5 · 9. ADR binding
10. FAQ 3 câu (UNIQUE) · 11. Gherkin pointer

`[TBD: Chốt policy partial-settlement cho merchant T+0/T+1 ảnh hưởng 2120 và 3810.]`
`[TBD: Chốt đảo bút toán EOD tự động hay bán tự động khi callback quá hạn 3800.]`
`[TBD: Chốt currency expansion — hiện VND only, scale 4, ADR-019/ADR-028.]`

---

## Chương COA-Pairs. Ma trận cặp tài khoản bắt buộc

### COA-P.1 Ma trận use_case → cặp tài khoản

Ma trận này là **checklist tối thiểu** khi build journal lines. Thiếu bất kỳ cột nào → DR ≠ CR reject.

| use_case | Phase | TK Nợ (DR) | TK Có (CR) | Terminal transit | Nguồn |
|----------|-------|------------|------------|-----------------|-------|
| DEPOSIT | A PENDING | 1111 | 3100 | 3100 ≠ 0 OK | foundation §8 |
| DEPOSIT | B POSTED | 3100, 2110 | 2110, 4110 | 3100 = 0 | ADR-006 |
| WITHDRAW | POSTED | 2110, 3200, 3200 | 3200, 1111, 4120 | 3200 = 0 | foundation §9 |
| INTERNAL_TRANSFER | POSTED | 2110(A), 3300, 3300 | 3300, 2110(B), 4130 | 3300 = 0 | foundation §10 |
| IBFT | POSTED | 2110, 3400, 3400, 5100 | 3400, 4130, 1112, 1112 | 3400 = 0 | ADR-025 |
| WALLET_PAYMENT | POSTED | 2110, 3500 | 3500, 2120 | 3500 = 0 | foundation §13 |
| QR_PAYMENT | POSTED | 1113, 5100, 3500 | 3500, 1113, 2120 | 3500 = 0 | foundation §12 |
| PAYROLL | POSTED | 2120, 3600, 3600, 5100 | 3600, 4150, 1112, 1112 | 3600 = 0 | foundation §14 |
| DISBURSEMENT | POSTED | 1111, 2130, 3700, 5100 | 2130, 3700, 1112, 1112 | 3700 = 0 | foundation §15 |
| EOD_CLEARING | POSTED | 2120, 3800, 3800 | 3800, 3820, 3810 | 3800 = 0 | foundation §16 |
| EOD_SETTLEMENT | POSTED | 3810, 5100 | 1112, 1112 | 3810 = 0 | foundation §16 |
| EOD_MDR | POSTED | 3820 | 4140 | 3820 = 0 | foundation §16 |
| INIT_CAPITAL | ONE-TIME | 6000 | 1111, 1112, 1113 | — | foundation §7 |

### COA-P.2 Quy tắc kiểm tra cặp

```
Trước khi postJournal:
1. sum(DR lines) == sum(CR lines)         ← bắt buộc
2. transit(use_case) sẽ = 0 khi POSTED    ← ADR-010
3. TK tài sản bank đúng với use_case      ← ADR-025 (IBFT → 1112, không 1111)
4. Revenue TK đúng loại                   ← 4110 deposit / 4120 withdraw / 4130 transfer+IBFT / 4140 MDR / 4150 payroll
5. 5100 chỉ xuất hiện khi có bank cost    ← matching principle (vol-01 Ch11)
```

### COA-P.3 Mapping wallet_type → COA control

| wallet_type | COA control | Ghi chú |
|-------------|-------------|---------|
| USER | **2110** | Deposit credit, withdraw debit, payment debit |
| MERCHANT | **2120** | Payment credit, payroll debit, EOD clearing debit |
| PARTNER | **2130** | Disbursement escrow |

Mapping này **chỉ để recon** — không có FK từ wallet sang COA ([ADR-020](../../adr/ADR-020-wallet-lanes-coa-control-mapping.md)).

---

## Chương TB. Trial balance & kiểm tra cân

### TB.1 Định nghĩa trial balance trên platform

Trial balance là bảng tổng hợp `SUM(DR) - SUM(CR)` theo `account_code` từ `coa_trans_data` với `coa_trans.status = 'POSTED'` trong kỳ.

```sql
-- Query mẫu trial balance (thiết kế, chưa implement)
SELECT
    d.account_code,
    a.account_name,
    SUM(d.dr_amount) AS total_dr,
    SUM(d.cr_amount) AS total_cr,
    SUM(d.dr_amount) - SUM(d.cr_amount) AS net_balance
FROM coa_trans_data d
JOIN coa_account a ON a.account_code = d.account_code
JOIN coa_trans t ON t.id = d.coa_trans_id
WHERE t.status = 'POSTED'
  AND t.posted_at BETWEEN :period_start AND :period_end
GROUP BY d.account_code, a.account_name
ORDER BY d.account_code;
```

### TB.2 Kiểm tra 3 invariant

| # | Invariant | SQL kiểm tra | Alert khi |
|---|-----------|-------------|-----------|
| INV-1 | 1111+1112+1113 = 2110+2120+2130 | SUM asset groups vs liability groups | Chênh > tolerance |
| INV-2 | Mỗi transit 310x–382x = 0 | WHERE account_code LIKE '3%' | Net ≠ 0 ngoài PENDING deposit |
| INV-3 | sum(DR) = sum(CR) toàn kỳ | SUM(dr_amount) vs SUM(cr_amount) POSTED | Bất kỳ chênh |

### TB.3 Transit PENDING hợp lệ

Trong kỳ, `3100` có thể ≠ 0 nếu còn deposit `PENDING`. Trial balance hiển thị trạng thái **3100 net** với chú thích "PENDING deposits: N items". Không báo lỗi cho 3100 PENDING — chỉ alert nếu aging > SLA.

### TB.4 Period close chặn

Khi đóng kỳ: nếu bất kỳ transit 3100–3820 ≠ 0 (trừ 3100 PENDING trong waiver list) → **block close** ([ADR-023](../../adr/ADR-023-accounting-period-close.md)). Ops phải xử lý hoặc đưa vào exception list.

### TB.5 Bảng kỳ vọng số dư normal balance

| Nhóm TK | Kỳ vọng net | Dấu hiệu bất thường |
|---------|-------------|---------------------|
| 111x (Assets) | **DR dương** | CR net → bank âm, cần check ngay |
| 211x–213x (Liabilities) | **CR dương** | DR net → nợ âm user, lỗi nghiêm trọng |
| 31xx–38xx (Transit) | **= 0** sau POSTED | ≠ 0 → stuck flow, ADR-010 violation |
| 411x–415x (Revenue) | **CR dương** | DR net → doanh thu âm, kiểm tra reversal |
| 5100 (Expense) | **DR dương** | CR net → chi phí âm, kiểm tra entry |
| 6000 (Equity) | **CR dương** | Chỉ thay đổi khi init capital |

---

## Chương DDL. Seed & DDL `coa_account`

### DDL.1 Cấu trúc bảng `coa_account`

```sql
-- DDL thiết kế (chưa implement trong repo)
CREATE TABLE coa_account (
    id            BIGSERIAL PRIMARY KEY,
    account_code  VARCHAR(10)  NOT NULL UNIQUE,
    account_name  VARCHAR(200) NOT NULL,
    account_type  VARCHAR(20)  NOT NULL,   -- ASSET / LIABILITY / TRANSIT / REVENUE / EXPENSE / EQUITY
    normal_balance VARCHAR(6)  NOT NULL,   -- DEBIT / CREDIT
    lane_mirror   VARCHAR(20),             -- USER / MERCHANT / PARTNER / NULL
    use_cases     TEXT,                    -- CSV: DEPOSIT,WITHDRAW,...
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
```

### DDL.2 Seed data — 23 tài khoản

```sql
INSERT INTO coa_account (account_code, account_name, account_type, normal_balance, lane_mirror, use_cases) VALUES
-- Group 1: Assets
('1111','Tiền gửi Vietinbank chuyên dụng','ASSET','DEBIT',NULL,'DEPOSIT,WITHDRAW,DISBURSEMENT'),
('1112','Napas Clearing','ASSET','DEBIT',NULL,'IBFT,PAYROLL,DISBURSEMENT,EOD'),
('1113','VPBank QR/POS acquirer','ASSET','DEBIT',NULL,'QR_PAYMENT'),
-- Group 2: Liabilities
('2110','Wallet payable — User','LIABILITY','CREDIT','USER','DEPOSIT,WITHDRAW,INTERNAL_TRANSFER,IBFT,WALLET_PAYMENT'),
('2120','Wallet payable — Merchant','LIABILITY','CREDIT','MERCHANT','WALLET_PAYMENT,QR_PAYMENT,PAYROLL,EOD'),
('2130','Escrow payable — Disbursement partner','LIABILITY','CREDIT','PARTNER','DISBURSEMENT'),
-- Group 3: Transit
('3100','Transit — Deposit','TRANSIT','CREDIT',NULL,'DEPOSIT'),
('3200','Transit — Withdraw','TRANSIT','CREDIT',NULL,'WITHDRAW'),
('3300','Transit — Internal transfer','TRANSIT','CREDIT',NULL,'INTERNAL_TRANSFER'),
('3400','Transit — IBFT','TRANSIT','CREDIT',NULL,'IBFT'),
('3500','Transit — Payment','TRANSIT','CREDIT',NULL,'WALLET_PAYMENT,QR_PAYMENT'),
('3600','Transit — Payroll','TRANSIT','CREDIT',NULL,'PAYROLL'),
('3700','Transit — Disbursement','TRANSIT','CREDIT',NULL,'DISBURSEMENT'),
('3800','Transit — Clearing lock','TRANSIT','CREDIT',NULL,'EOD'),
('3810','Transit — Settlement outbound','TRANSIT','CREDIT',NULL,'EOD'),
('3820','Transit — MDR holdback','TRANSIT','CREDIT',NULL,'EOD'),
-- Group 4: Revenue
('4110','Doanh thu phí nạp tiền','REVENUE','CREDIT',NULL,'DEPOSIT'),
('4120','Doanh thu phí rút tiền','REVENUE','CREDIT',NULL,'WITHDRAW'),
('4130','Doanh thu phí chuyển khoản','REVENUE','CREDIT',NULL,'INTERNAL_TRANSFER,IBFT'),
('4140','Doanh thu MDR merchant','REVENUE','CREDIT',NULL,'EOD'),
('4150','Doanh thu phí payroll/disbursement','REVENUE','CREDIT',NULL,'PAYROLL,DISBURSEMENT'),
-- Group 5: Expense
('5100','Chi phí bank/Napas','EXPENSE','DEBIT',NULL,'IBFT,QR_PAYMENT,PAYROLL,DISBURSEMENT,EOD'),
-- Group 6: Equity
('6000','Vốn chủ sở hữu','EQUITY','CREDIT',NULL,'INIT_CAPITAL');
```

---

## Chương VAS. Mapping TT200/VAS → COA nội bộ (informative)

> **Lưu ý:** Mapping này chỉ có tính **tham khảo/informative**. GtelPay Core v1 là ledger vận hành fintech — không phải sổ kế toán doanh nghiệp đầy đủ TT200. BCTC chính thức cần ánh xạ lại qua tầng kế toán bên ngoài.

| COA nội bộ | TT200 gần nhất | VAS gần nhất | Ghi chú |
|-----------|---------------|-------------|---------|
| 1111, 1112, 1113 | TK 112 — TGNH | IAS 7 Cash equiv | Tiền gửi thanh toán ngân hàng |
| 2110, 2120, 2130 | TK 338 — Phải trả khác | IAS 37 | Phải trả ví người dùng / merchant |
| 3100–3820 | Không tương đương trực tiếp | — | Tài khoản trung gian nội bộ, net zero |
| 4110–4150 | TK 511 — Doanh thu DV | IAS 18 / IFRS 15 | Phí dịch vụ ghi nhận khi POSTED |
| 5100 | TK 641/642 | IAS 7 | Chi phí giao dịch ngân hàng |
| 6000 | TK 411 — Vốn đầu tư CSH | IAS 1 | Vốn chủ khởi tạo |

---

## Chương CK. Review Checklist 40 mục trước release

Mỗi mục được kiểm tra một lần độc lập — không lặp nội dung. **Tích (✓) = pass; (✗) = block release.**

| # | Hạng mục kiểm tra | Căn cứ |
|---|-------------------|--------|
| CK-01 | Seed 23 TK đã insert đủ, không thiếu mã nào trong danh sách §DDL.2 | DDL.2 |
| CK-02 | `coa_account.account_code` UNIQUE constraint tồn tại trong DDL | DDL.1 |
| CK-03 | Deposit phase A: chỉ có dòng 1111 DR + 3100 CR, KHÔNG có 2110 | ADR-006 AC-06-01 |
| CK-04 | Deposit phase B: 3100 net = 0 trước khi set POSTED | ADR-010 AC-10-05 |
| CK-05 | Wallet credit chỉ kích hoạt sau `coa_trans.status = POSTED` | ADR-006 AC-06-02 |
| CK-06 | Withdraw: 2110 DR gross (principal + fee), không DR net | foundation §9 |
| CK-07 | Withdraw: 4120 CR = fee amount, không CR gross | foundation §9 step 6 |
| CK-08 | Internal transfer: 2110(A) DR 101.000, 2110(B) CR 100.000, 4130 CR 1.000 | foundation §10 |
| CK-09 | IBFT bank leg: account_code = 1112, KHÔNG phải 1111 | ADR-025 AC-025-02 |
| CK-10 | IBFT: 5100 DR 500 xuất hiện đúng khi có Napas cost | ADR-025 AC-025-03 |
| CK-11 | QR/POS: 1113 DR acquirer amount, 5100 DR acquirer cost | foundation §12 |
| CK-12 | Wallet payment: không có dòng 111x (nội bộ, không qua bank) | foundation §13 |
| CK-13 | Payroll: 2120 DR (merchant), 1112 CR (bank), 5100 DR cost | foundation §14 |
| CK-14 | EOD: 3800 net = 0 sau khi tách MDR và net | foundation §16 |
| CK-15 | EOD: 3810 = 0 sau settlement outbound về 1112 | foundation §16 |
| CK-16 | EOD: 3820 = 0 sau khi MDR → 4140 | foundation §16 |
| CK-17 | 4130 ghi nhận cho CẢ INTERNAL_TRANSFER và IBFT, không chỉ một | foundation §10,11 |
| CK-18 | 4150 ghi nhận cho CẢ PAYROLL và DISBURSEMENT | foundation §14,15 |
| CK-19 | 5100 KHÔNG xuất hiện trong DEPOSIT, WITHDRAW, INTERNAL_TRANSFER, WALLET_PAYMENT | matching principle |
| CK-20 | 6000 chỉ xuất hiện trong INIT_CAPITAL journal, không trong runtime | foundation §7 |
| CK-21 | `business_ref` UNIQUE per use_case trong `coa_trans` | foundation §2.1 |
| CK-22 | Duplicate `business_ref` → idempotent return, không tạo journal thứ hai | ADR-005 |
| CK-23 | Dòng POSTED trong `coa_trans_data` không có UPDATE sau posting | ADR-001 |
| CK-24 | Reversal journal tạo entries âm ngược, KHÔNG xóa/sửa dòng gốc | ADR-001 |
| CK-25 | Trial balance query chỉ đọc POSTED, không đọc PENDING/FAILED | TB.1 |
| CK-26 | W5 recon dùng hai query riêng — không JOIN cross-schema wallet + coa | ADR-003, ADR-014 |
| CK-27 | W5 tolerance: lag < SLA không alert; > SLA mới alert | ADR-014 AC-014-02 |
| CK-28 | Period close block nếu transit ≠ 0 (ngoài PENDING deposit waiver) | ADR-023 |
| CK-29 | Tất cả amount BigDecimal scale 4, HALF_UP tại orchestration boundary | ADR-028 |
| CK-30 | OpenAPI amount field là decimal string, không float | ADR-028 AC-028-03 |
| CK-31 | `normal_balance` column trong `coa_account` khớp với mã nhóm (1xxx → DEBIT) | DDL.1 |
| CK-32 | 2110 control account không có per-member breakdown trên `coa_*` | ADR-020, vol-01 §2.2 |
| CK-33 | Nếu wallet credit fail sau POSTED: retry consumer, không sửa `coa_trans_data` | ADR-006 |
| CK-34 | Disbursement pre-fund: 1111 DR → 2130 CR riêng biệt trước disburse | foundation §15 |
| CK-35 | EOD không chạy inline với payment — là independent batch job | foundation §4 |
| CK-36 | `account_type = TRANSIT` trong seed, transit không có normal_balance cố định | DDL.2 |
| CK-37 | Integration test: deposit happy path → 1111+100k, 2110+99k, 4110+1k, 3100=0 | ADR-006 TC-006-02 |
| CK-38 | Integration test: IBFT happy path → 3400=0, 1112 net −100.5k, 4130+1k, 5100+500 | ADR-025 TC-025-01 |
| CK-39 | Integration test: EOD → 3800=0, 3810=0, 3820=0, 4140+MDR, 1112 net | foundation §16 |
| CK-40 | Docs: tất cả `[TBD]` trong Quyển II đã có Jira ticket hoặc quyết định chốt | Ch 0.3 |

---

## Chương FAQ-COA. FAQ COA 40 câu hỏi

Mỗi câu là câu hỏi **độc lập**; tránh lặp "Khi nào FR-10".

| # | Câu hỏi | Trả lời ngắn |
|---|---------|-------------|
| F-01 | Tại sao cần tài khoản transit thay vì ghi thẳng 2110? | Transit làm visible tiền in-flight, giúp trial balance có ý nghĩa khi pending |
| F-02 | Khi 3100 ≠ 0 có phải lỗi không? | Không, nếu deposit PENDING chưa confirm; LÀ lỗi nếu đã POSTED |
| F-03 | Sao phí deposit (4110) không ghi phase A? | Phí chỉ chắc chắn khi confirm POSTED — PENDING có thể FAILED |
| F-04 | 2110 có per-member hay không? | KHÔNG — 2110 là control account tổng hợp; chi tiết ở wallet_balance |
| F-05 | Khi user withdraw, 2110 DR hay CR? | DR — giảm liability (nợ user giảm), bên Nợ |
| F-06 | IBFT dùng 1111 hay 1112? | **1112** Napas Clearing — xem ADR-025; 1111 trên IBFT bị reject |
| F-07 | 5100 xuất hiện ở use case nào? | IBFT, QR/POS, Payroll, Disbursement, EOD — bất kỳ khi có bank/Napas cost |
| F-08 | Payment nội bộ (wallet-to-wallet) có dùng 111x không? | Không — chỉ 2110, 3500, 2120; không có bank movement |
| F-09 | 4130 dùng cho transfer hay IBFT hay cả hai? | Cả hai: internal transfer và IBFT đều ghi 4130 revenue |
| F-10 | Disbursement pre-fund ghi TK nào? | 1111 DR / 2130 CR (escrow cho partner) |
| F-11 | EOD clearing dùng bao nhiêu transit? | Ba: 3800 (lock), 3810 (net outbound), 3820 (MDR) — cả ba = 0 sau EOD |
| F-12 | 6000 có tham gia vào luồng hàng ngày không? | Không — chỉ ghi khi init capital trước go-live |
| F-13 | Tại sao 4140 là MDR riêng mà không gộp 4130? | MDR là MDR merchant (%) — khác phí chuyển khoản flat fee; tách để báo cáo |
| F-14 | Khi nào dùng reversal journal? | Khi sửa dòng POSTED đã sai — tạo journal âm ngược, không sửa dòng gốc |
| F-15 | W5 recon lệch nhỏ có tự sửa không? | Không — report-only; chỉ alert nếu > tolerance (ADR-014) |
| F-16 | Scale 4 nghĩa là gì trong thực tế? | 100.000 lưu thành 100000.0000; 99.000 = 99000.0000 |
| F-17 | Deposit FAILED thì đảo gì? | Chỉ đảo phase A: 1111 CR / 3100 DR — KHÔNG đụng 2110 vì chưa credit |
| F-18 | Internal transfer không có dòng 111x vì sao? | Không có bank movement; tiền chuyển trong nội bộ platform |
| F-19 | `coa_trans.business_ref` UNIQUE theo scope nào? | Unique per use_case (hoặc global tuỳ product rule) — idempotency key |
| F-20 | Payroll batch 5 người → mấy journal? | Thiết kế: 1 journal với nhiều dòng 2110/1112; hoặc 1 journal per employee — `[TBD]` |
| F-21 | Khi Napas timeout, 3400 non-zero phải làm gì? | Alert aging; saga retry hoặc reversal; ops xử lý; không để stuck |
| F-22 | Sao không dùng TK 111x cho IBFT outbound? | IBFT đi qua Napas clearing — 1112 tracks Napas balance riêng (ADR-025) |
| F-23 | QR/POS: tiền từ acquirer VPBank vào TK nào trước? | 1113 DR acquirer amount → 3500 CR transit → sau đó 2120 CR merchant |
| F-24 | 3800 và 3810 khác nhau thế nào? | 3800 = lock toàn bộ settlement; 3810 = net sau trừ MDR để outbound về bank |
| F-25 | Khi nào dùng 2130 thay 2120? | 2130 dành riêng cho PARTNER (disbursement partner escrow) — ADR-020 |
| F-26 | Ai quyết định fee 1.000 trên gross 100.000? | Orchestration — accounting chỉ nhận số đã tính, không tự tính fee (ADR-009) |
| F-27 | Lỗi DC-04 nghĩa là gì? | IBFT ghi sai 1111 thay vì 1112 cho bank leg — vi phạm ADR-025 |
| F-28 | Deposit song song hai webhook cùng bankRef xử lý sao? | `business_ref` UNIQUE → duplicate webhook → idempotent return existing journal |
| F-29 | Trial balance có query PENDING không? | Không — chỉ POSTED; PENDING hiển thị riêng ở mục monitoring transit |
| F-30 | Khi user refund merchant — ghi TK nào? | `[TBD: Chốt refund policy]` — likely 2120 DR / 3500 CR / 2110 CR (reverse payment) |
| F-31 | 4110 có thể bằng 0 không? | Có — nếu zero-fee deposit; không có dòng 4110 trong journal đó |
| F-32 | Mình có thể thêm TK mới ngoài 23 TK này không? | Phải qua ADR mới — thêm TK ảnh hưởng matching, trial balance, recon |
| F-33 | Period close xảy ra vào lúc nào? | Cuối tháng/kỳ theo ADR-023; block nếu transit ≠ 0 |
| F-34 | `coa_trans_data` có cột `balance` không? | Không — balance derive từ SUM(DR)-SUM(CR); không lưu running balance |
| F-35 | Khi nào ghi 5100 DR và khi nào không? | Khi có thực chi bank cost (IBFT Napas 500, payroll Napas, acquirer fee QR) |
| F-36 | EOD partial failure (một merchant fail) thì 3810 còn lại sao? | 3810 giữ phần fail; retry theo EOD recovery; không để qua kỳ mới (ADR-010) |
| F-37 | Sao `coa_account` không lưu per-member? | COA là control/aggregate; per-member = wallet_balance (tách schema, ADR-003) |
| F-38 | IBFT profit net là bao nhiêu? | Revenue 4130 +1.000 − Expense 5100 +500 = **+500 net** (E16–E17 vol-01) |
| F-39 | Disbursement: ai DR 2130 khi disburse? | Orchestration DR 2130 gross → 3700 transit → 1112 outbound (foundation §15) |
| F-40 | Có thể xem số dư TK bất kỳ thời điểm? | Có — SUM POSTED `coa_trans_data` đến timestamp đó; không cần snapshot table |

---

## Phần B — 23 Tài khoản chi tiết

---

### B.1 TK 1111 — Tiền gửi Vietinbank chuyên dụng

#### Metadata

| Trường | Giá trị |
|--------|---------|
| Mã tài khoản | **1111** |
| Nhóm | 1 — Tài sản |
| Loại | ASSET |
| Số dư bình thường | **Nợ (Debit)** |
| Lane mirror | — (bank asset, không map wallet lane) |
| Use case | DEPOSIT, WITHDRAW, DISBURSEMENT (pre-fund) |
| ADR chính | ADR-006, ADR-010 |

#### Định nghĩa kinh tế & bản chất

TK 1111 đại diện cho **tài khoản ngân hàng chuyên dụng tại Vietinbank** — nơi người dùng nạp tiền qua chuyển khoản VA (virtual account). Đây là cửa ngõ vào platform cho luồng deposit và cửa ra cho withdraw/disbursement. Số dư TK 1111 phải **khớp với sao kê Vietinbank** trong FR-10. Không dùng cho IBFT (dùng 1112) và không dùng cho QR/POS (dùng 1113).

Invariant vận hành: `1111 + 1112 + 1113 = 2110 + 2120 + 2130` ([`foundation.md`](../../spec/foundation.md) §5).

#### T-account

```
         TK 1111 — Vietinbank dedicated
         (Tài sản — Nợ)
 ─────────────────────────────────────────
  DR (Nợ / Tăng)  │  CR (Có / Giảm)
 ─────────────────│───────────────────────
  +100.000        │             (Deposit Phase A webhook)
  (Deposit 1111   │  −100.000   (Withdraw bank payout E07)
   DR webhook)    │  −100.000   (Disbursement disburse leg)
                  │
  SD Nợ: dương   │  Dư Nợ tích lũy theo deposits
```

#### Bảng DR/CR theo use case

| use_case | Phase | TK 1111 | DR | CR | Đối ứng | Nguồn |
|----------|-------|---------|----|----|---------|-------|
| DEPOSIT | A PENDING | DR | 100.000 | — | 3100 CR | foundation §8 E02 |
| DEPOSIT | FAILED reverse | CR | — | 100.000 | 3100 DR | foundation §8.5 E05 |
| WITHDRAW | POSTED | CR | — | 100.000 | 3200 DR | foundation §9 E07 |
| DISBURSEMENT | Pre-fund | DR | 100.000 | — | 2130 CR | foundation §15 E23 |
| INIT_CAPITAL | One-time | DR | 1.000.000.000 | — | 6000 CR | foundation §7 |

> **Không xuất hiện trong:** IBFT (→ 1112), QR/POS (→ 1113), INTERNAL_TRANSFER, WALLET_PAYMENT, EOD.

#### Cặp đối ứng bắt buộc

| 1111 DR khi | 1111 CR khi |
|-------------|------------|
| Deposit webhook (Phase A) | Withdraw bank payout |
| Disbursement pre-fund | Deposit FAILED reverse |
| Init capital | — |

#### Trạng thái PENDING / POSTED

| Trạng thái journal | Hành vi TK 1111 |
|-------------------|-----------------|
| DEPOSIT PENDING | DR +100.000 (đã vào bank, chưa credit user) |
| DEPOSIT POSTED | Không thay đổi thêm (đã DR ở phase A) |
| DEPOSIT FAILED | CR −100.000 (reversal phase A) |
| WITHDRAW POSTED | CR −100.000 (tiền ra Vietinbank) |

#### Lỗi thường gặp

| Mã | Mô tả lỗi | Nguyên nhân | Xử lý |
|----|-----------|-------------|-------|
| 1111-E01 | 1111 CR trên deposit phase A | Nhầm DR/CR direction | Fix template, retest ADR-006 |
| 1111-E02 | 1111 DR trên withdraw bank leg | Nhầm withdraw với deposit | Review foundation §9 |
| 1111-E03 | 1111 xuất hiện trong IBFT journal | Dùng sai TK — phải là 1112 | DC-04, ADR-025 |
| 1111-E04 | Duplicate deposit: 1111 DR double | business_ref không UNIQUE | Enforce UNIQUE constraint |
| 1111-E05 | 1111 tham chiếu trong QR journal | QR dùng 1113, không phải 1111 | Fix posting template QR |
| 1111-E06 | Số tiền 1111 khác bankRef amount | Orchestration parse sai webhook | Validate amount match |

#### Recon FR-10 / W5

**FR-10 bank reconciliation:** Sao kê Vietinbank ngày N → SUM 1111 POSTED (`coa_trans_data`) ngày N = số dư sao kê. Chênh do timing PENDING deposit chưa confirmed — trong tolerance. Chênh ngoài SLA → alert ops.

**W5:** 1111 không tham gia W5 trực tiếp (W5 so sánh 2110+2120+2130 vs wallet). 1111 tham gia qua INV-1: `SUM(111x) = SUM(211x)`.

#### ADR binding

| ADR | Quyết định liên quan đến TK 1111 |
|-----|----------------------------------|
| [ADR-006](../../adr/ADR-006-two-phase-deposit.md) | Phase A: 1111 DR trước wallet credit |
| [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md) | 3100 về 0 khi POSTED (1111 đã ghi xong phase A) |
| [ADR-025](../../adr/ADR-025-ibft-napas-clearing-1112.md) | 1111 KHÔNG dùng cho IBFT leg — dùng 1112 |

#### FAQ 3 câu — TK 1111

**Q1:** Tại sao phase A deposit ghi 1111 DR trước khi biết user là ai?  
**A:** Bank webhook xác nhận tiền đã vào VA; việc map VA → userId là bước sau. Ghi 1111 DR ngay đảm bảo không mất dấu tiền, 3100 giữ placeholder trong khi map.

**Q2:** Nếu Vietinbank sao kê ngày T+1 thì FR-10 thực hiện lúc nào?  
**A:** FR-10 chạy sau khi nhận file sao kê (T+1 sáng); so sánh với SUM(1111 POSTED) trong kỳ. Deposit PENDING đêm T chưa confirm là timing gap hợp lệ.

**Q3:** Withdraw 100.000 → 1111 CR 100.000 nhưng user bị trừ 101.000 — 1.000 còn lại đi đâu?  
**A:** 1.000 fee đi vào 4120 CR qua 3200 DR 1.000 / 4120 CR 1.000. Tổng 3200 DR: 101.000 = 100.000 (bank) + 1.000 (fee). 3200 net = 0.

#### Gherkin pointer

```
Scenarios trong acceptance.md:
  → "Deposit happy path — Phase A PENDING"  (1111 DR 100.000 / 3100 CR 100.000)
  → "Deposit happy path — Phase B POSTED"   (3100 = 0; 2110 +99.000; 4110 +1.000)
  → "Withdraw ledger POSTED"                (2110 DR 101.000; 1111 CR 100.000)
  → "Deposit FAILED reverse"                (1111 CR 100.000; 3100 DR 100.000)
```

---

### B.2 TK 1112 — Napas Clearing

#### Metadata

| Trường | Giá trị |
|--------|---------|
| Mã tài khoản | **1112** |
| Nhóm | 1 — Tài sản |
| Loại | ASSET |
| Số dư bình thường | **Nợ (Debit)** |
| Lane mirror | — |
| Use case | IBFT, PAYROLL, DISBURSEMENT (outbound), EOD settlement |
| ADR chính | ADR-025, ADR-010 |

#### Định nghĩa kinh tế & bản chất

TK 1112 đại diện cho **tài khoản Napas Clearing** — rail thanh toán liên ngân hàng. Mọi outflow đến ngân hàng đích (IBFT, payroll batch, disbursement) đều đi qua 1112. Đây là điểm tracking chi phí Napas: mỗi giao dịch Napas phát sinh `5100 DR 500 / 1112 CR 500` kèm theo. Số dư 1112 theo dõi tổng tiền đang trên Napas clearing settlement.

**Không dùng 1111 cho IBFT** — phân tách giúp FR-10 bank reconciliation per-account rõ ràng ([ADR-025](../../adr/ADR-025-ibft-napas-clearing-1112.md)).

#### T-account

```
         TK 1112 — Napas Clearing
         (Tài sản — Nợ)
 ─────────────────────────────────────────
  DR (Nợ / Tăng)  │  CR (Có / Giảm)
 ─────────────────│───────────────────────
  (funded by      │  −100.000  IBFT outbound (E16)
   capital init)  │  −500      IBFT Napas cost (E17)
                  │  −500.000  Payroll batch (E22)
                  │  −2.500    Payroll Napas cost
                  │  −198.000  EOD settlement net
  SD Nợ: giảm    │  dần theo outflow
```

#### Bảng DR/CR theo use case

| use_case | Dòng | TK 1112 | DR | CR | Đối ứng | Nguồn |
|----------|------|---------|----|----|---------|-------|
| IBFT | Bank outbound | CR | — | 100.000 | 3400 DR | foundation §11 E16 |
| IBFT | Napas cost | CR | — | 500 | 5100 DR | foundation §11 E17 |
| PAYROLL | Bank outbound | CR | — | 500.000 | 3600 DR | foundation §14 E22 |
| PAYROLL | Napas cost | CR | — | 2.500 | 5100 DR | foundation §14 (5×500) |
| DISBURSEMENT | Disburse bank | CR | — | 100.000 | 3700 DR | foundation §15 |
| EOD_SETTLEMENT | Net outbound | CR | — | 198.000 | 3810 DR | foundation §16 |
| INIT_CAPITAL | Fund Napas | DR | 500.000.000 | — | 6000 CR | foundation §7 |

> **Không xuất hiện trong:** DEPOSIT, WITHDRAW (→ 1111), QR/POS (→ 1113), WALLET_PAYMENT.

#### Cặp đối ứng bắt buộc

| 1112 CR khi | Đối ứng DR |
|-------------|-----------|
| IBFT principal outbound | 3400 |
| IBFT Napas fee cost | 5100 |
| Payroll batch outbound | 3600 |
| Payroll Napas cost | 5100 |
| Disbursement disburse | 3700 |
| EOD net settlement | 3810 |

#### Trạng thái PENDING / POSTED

| Trạng thái | Hành vi TK 1112 |
|------------|-----------------|
| IBFT POSTED | CR gross: 100.000 + 500 Napas cost |
| PAYROLL POSTED | CR 500.000 + 2.500 cost (batch) |
| EOD POSTED | CR net settlement amount + Napas fee |
| INIT_CAPITAL | DR one-time fund |

#### Lỗi thường gặp

| Mã | Mô tả lỗi | Nguyên nhân | Xử lý |
|----|-----------|-------------|-------|
| 1112-E01 | IBFT dùng 1111 thay 1112 | Nhầm TK bank asset | DC-04; fix template, ADR-025 |
| 1112-E02 | 5100 thiếu trên IBFT journal | Quên Napas cost line | Thêm cost line, revalidate |
| 1112-E03 | 1112 CR xuất hiện trên DEPOSIT | DEPOSIT phải dùng 1111 | Sai template deposit |
| 1112-E04 | Payroll: 1112 CR > 500k với 5 employees | Nhầm cost nhân per-batch | Cost = 5 × 500 = 2.500, không 500k |
| 1112-E05 | EOD: 1112 CR amount không khớp sao kê Napas | Tính sai net settlement | Verify 3810 DR = 1112 CR |
| 1112-E06 | 1112 DR trong runtime (không phải init) | Nhầm chiều khi top-up Napas | Dùng journal đặc biệt top-up, không ad-hoc |

#### Recon FR-10 / W5

**FR-10:** Sao kê Napas clearing → SUM(1112 POSTED) trong kỳ. Đặc biệt chú ý Napas settlement lag (có thể T+1 hoặc T+2 tùy Napas batch window).

**W5:** 1112 tham gia INV-1 phía asset. Không trực tiếp map wallet lane.

#### ADR binding

| ADR | Quyết định liên quan đến TK 1112 |
|-----|----------------------------------|
| [ADR-025](../../adr/ADR-025-ibft-napas-clearing-1112.md) | IBFT bank leg = 1112, không 1111 |
| [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md) | 3400 = 0 khi IBFT POSTED (1112 đã ghi) |

#### FAQ 3 câu — TK 1112

**Q1:** Tại sao IBFT outbound ghi 1112 CR chứ không ghi 1111 CR?  
**A:** IBFT đi qua rail Napas — tiền ra khỏi tài khoản Napas clearing (1112), không phải tài khoản Vietinbank VA (1111). Tách giúp FR-10 per-account sạch, tránh sao kê chéo.

**Q2:** Napas cost 500 trên mỗi IBFT — 1112 giảm tổng 100.500, có đúng không?  
**A:** Đúng: 3400 DR 100.000 / 1112 CR 100.000 (principal) + 5100 DR 500 / 1112 CR 500 (cost). 1112 CR tổng 100.500; 3400 chỉ liên quan đến 100.000 — transit 3400 = 0 riêng.

**Q3:** EOD settlement 1112 CR bao nhiêu khi MDR 1% và gross 200.000?  
**A:** MDR = 2.000 (3820); net = 198.000 (3810 → 1112 CR 198.000). Nếu có thêm Napas EOD fee: 1112 CR thêm phần đó. `[TBD: Napas EOD fee structure chưa chốt.]`

#### Gherkin pointer

```
Scenarios trong acceptance.md:
  → "IBFT happy path — ledger"        (3400=0; 1112 CR 100.500; 5100 DR 500)
  → "IBFT — Napas 1112 not 1111"      (IBFT-E05 ADR-025)
  → "Payroll — bank outbound"         (3600=0; 1112 CR 502.500)
  → "EOD settlement — happy path"     (3810=0; 1112 CR 198.xxx)
```

---

### B.3 TK 1113 — VPBank QR/POS acquirer

#### Metadata

| Trường | Giá trị |
|--------|---------|
| Mã tài khoản | **1113** |
| Nhóm | 1 — Tài sản |
| Loại | ASSET |
| Số dư bình thường | **Nợ (Debit)** |
| Lane mirror | — |
| Use case | QR_PAYMENT (acquirer leg) |
| ADR chính | ADR-010 |

#### Định nghĩa kinh tế & bản chất

TK 1113 đại diện cho **tài khoản acquirer tại VPBank** cho luồng QR/POS. Khi người dùng quét QR thanh toán, acquirer VPBank nhận tiền và báo platform qua webhook. Platform ghi nhận 1113 DR (tiền vào acquirer account) và 3500 CR (transit payment). Acquirer fee (cost) được khấu trừ ngay: `5100 DR 500 / 1113 CR 500`. Số dư 1113 theo dõi tiền đang chờ settlement từ VPBank.

Sau EOD, 1113 không xuất hiện trong EOD settlement (EOD dùng 1112 Napas). `[TBD: Chốt EOD VPBank→Napas settlement flow nếu VPBank tự transfer.]`

#### T-account

```
         TK 1113 — VPBank QR/POS acquirer
         (Tài sản — Nợ)
 ─────────────────────────────────────────
  DR (Nợ / Tăng)  │  CR (Có / Giảm)
 ─────────────────│───────────────────────
  +100.000        │  −500      Acquirer fee cost (E19)
  (QR acquirer    │
   in, E18)       │
  SD Nợ: +99.500 │  (net setelah biaya)
```

#### Bảng DR/CR theo use case

| use_case | Dòng | TK 1113 | DR | CR | Đối ứng | Nguồn |
|----------|------|---------|----|----|---------|-------|
| QR_PAYMENT | Acquirer nhận | DR | 100.000 | — | 3500 CR | foundation §12 E18 |
| QR_PAYMENT | Acquirer cost | CR | — | 500 | 5100 DR | foundation §12 E19 |
| INIT_CAPITAL | Fund VPBank | DR | 500.000.000 | — | 6000 CR | foundation §7 |

#### Cặp đối ứng bắt buộc

| 1113 DR | Đối ứng | 1113 CR | Đối ứng |
|---------|---------|---------|---------|
| QR acquirer amount | 3500 CR | QR acquirer cost | 5100 DR |
| Init capital | 6000 CR | — | — |

#### Trạng thái PENDING / POSTED

| Trạng thái | Hành vi TK 1113 |
|------------|-----------------|
| QR_PAYMENT POSTED | DR +100.000; CR −500 (acquirer cost) |
| Init | DR +500.000.000 |

#### Lỗi thường gặp

| Mã | Mô tả lỗi | Nguyên nhân | Xử lý |
|----|-----------|-------------|-------|
| 1113-E01 | QR dùng 1111 thay 1113 | Sai TK acquirer | Fix template QR |
| 1113-E02 | 5100 thiếu trong QR journal | Quên acquirer cost | Thêm cost line |
| 1113-E03 | 1113 xuất hiện trong IBFT | IBFT dùng 1112 | DC-04 variant |
| 1113-E04 | 1113 DR = gross − cost thay vì gross full | Cost trừ trên TK trước khi ghi | Ghi full gross DR rồi CR cost riêng |
| 1113-E05 | 1113 trong EOD journal | EOD không liên quan 1113 trực tiếp | Remove hoặc thiết kế đặc biệt VPBank |

#### Recon FR-10 / W5

**FR-10:** Sao kê VPBank acquirer account → SUM(1113 POSTED net). Acquirer settlement timing có thể lag so platform. Alert nếu 1113 âm (không hợp lý).

#### ADR binding

| ADR | Quyết định liên quan |
|-----|---------------------|
| [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md) | 3500 = 0 khi QR_PAYMENT POSTED |

#### FAQ 3 câu — TK 1113

**Q1:** Tại sao QR dùng 1113 riêng mà không dùng 1112 hay 1111?  
**A:** QR/POS là acquirer account VPBank (khác Napas clearing 1112 và VA Vietinbank 1111). Tách TK giúp FR-10 per-account và visibility cho acquirer reconciliation.

**Q2:** Acquirer cost 500 ghi vào 5100 DR — 1113 giảm chỉ 500, user thấy full 100.000 vào merchant?  
**A:** Đúng: platform hấp thụ cost 500 (expense 5100), merchant nhận full 100.000 (2120 CR 100.000). Platform "lãi" gộp từ các nguồn khác.

**Q3:** Nếu VPBank không có acquirer fee thì journal QR thay đổi gì?  
**A:** Bỏ dòng `5100 DR 500 / 1113 CR 500`. Journal còn: `1113 DR 100.000 / 3500 CR 100.000` + `3500 DR 100.000 / 2120 CR 100.000`. Transit 3500 = 0.

#### Gherkin pointer

```
Scenarios trong acceptance.md:
  → "QR/POS payment — acquirer path"  (1113 DR 100k; 5100 DR 500; 3500=0; 2120+100k)
  → "QR/POS — 1113 not 1112"
```

---

### B.4 TK 2110 — Wallet payable — User

#### Metadata

| Trường | Giá trị |
|--------|---------|
| Mã tài khoản | **2110** |
| Nhóm | 2 — Nợ phải trả |
| Loại | LIABILITY |
| Số dư bình thường | **Có (Credit)** |
| Lane mirror | **USER** (`wallet_type = 'USER'`) |
| Use case | DEPOSIT, WITHDRAW, INTERNAL_TRANSFER, IBFT, WALLET_PAYMENT |
| ADR chính | ADR-006, ADR-020, ADR-014 |

#### Định nghĩa kinh tế & bản chất

TK 2110 là **control account tổng hợp** thể hiện tổng nợ nền tảng đối với tất cả người dùng (lane USER). Số dư Có tương đương `SUM(wallet_balance.available + wallet_balance.frozen)` của tất cả user — đây là lượng tiền platform đang "giữ hộ". Không có sub-ledger per-member trên `coa_*`; chi tiết per-member thuộc `wallet_balance` ([ADR-020](../../adr/ADR-020-wallet-lanes-coa-control-mapping.md)).

**Nguyên tắc số dư:** Khi user nạp thêm → 2110 tăng (CR). Khi user tiêu/rút → 2110 giảm (DR).

#### T-account

```
         TK 2110 — Wallet payable User
         (Nợ phải trả — Có)
 ─────────────────────────────────────────
  DR (Giảm nợ)    │  CR (Tăng nợ)
 ─────────────────│───────────────────────
  −101.000        │  +99.000   Deposit POSTED net (E03)
  (Withdraw gross │  +100.000  Transfer credit B (E10)
   E06)           │  +100.000  Payment receive (not user)
  −101.000        │            …
  (IBFT E14)      │
  −100.000        │
  (payment E12)   │
  SD Có: dương   │  Tổng nợ platform đối user
```

#### Bảng DR/CR theo use case

| use_case | Dòng | TK 2110 | DR | CR | Đối ứng | Nguồn |
|----------|------|---------|----|----|---------|-------|
| DEPOSIT | Phase A | — | — | — | Không ghi phase A | ADR-006 |
| DEPOSIT | Phase B CR net | CR | — | 99.000 | 3100 DR | foundation §8 E03 |
| DEPOSIT | Phase B fee gross-up | DR | 1.000 | — | 4110 CR | foundation §8 step 5 |
| WITHDRAW | Gross debit | DR | 101.000 | — | 3200 CR | foundation §9 E06 |
| INTERNAL_TRANSFER | User A gross | DR | 101.000 | — | 3300 CR | foundation §10 E09 |
| INTERNAL_TRANSFER | User B credit | CR | — | 100.000 | 3300 DR | foundation §10 E10 |
| IBFT | User gross | DR | 101.000 | — | 3400 CR | foundation §11 E14 |
| WALLET_PAYMENT | User debit | DR | 100.000 | — | 3500 CR | foundation §13 E12 |

#### Cặp đối ứng bắt buộc

| 2110 DR khi | Đối ứng CR | 2110 CR khi | Đối ứng DR |
|-------------|-----------|------------|-----------|
| Withdraw gross | 3200 | Deposit net | 3100 |
| Transfer A gross | 3300 | Transfer B credit | 3300 |
| IBFT gross | 3400 | Deposit fee gross-up (nội bộ) | — |
| Wallet payment | 3500 | — | — |
| Fee gross-up | 4110 | — | — |

#### Trạng thái PENDING / POSTED

| Trạng thái | Hành vi TK 2110 |
|------------|-----------------|
| DEPOSIT PENDING | Không thay đổi — chưa credit user |
| DEPOSIT POSTED | CR +99.000 net; DR +1.000 fee gross-up |
| WITHDRAW POSTED | DR −101.000 |
| PAYMENT POSTED | DR −100.000 |

#### Lỗi thường gặp

| Mã | Mô tả lỗi | Nguyên nhân | Xử lý |
|----|-----------|-------------|-------|
| 2110-E01 | 2110 CR ngay phase A deposit | Ghi trước khi POSTED | Vi phạm ADR-006; sửa flow |
| 2110-E02 | 2110 DR net thay gross trên withdraw | Quên include fee | 2110 DR = principal + fee |
| 2110-E03 | 2110 per-member sub-ledger trên coa_* | Nhầm ledger với wallet | Sub-ledger thuộc wallet_balance |
| 2110-E04 | JOIN 2110 với wallet_balance trong SQL | Vi phạm bounded context | ADR-003: hai query riêng |
| 2110-E05 | 2110 trong QR journal (không phải wallet payment) | QR dùng 1113, 3500, 2120 | 2110 không xuất hiện trong QR |
| 2110-E06 | 2110 số dư âm sau hàng loạt withdraw | Thiếu kiểm tra insufficient balance | wallet kiểm tra available; accounting post sau |

#### Recon FR-10 / W5

**W5:** `SUM(wallet_balance.available + frozen WHERE wallet_type='USER')` vs `SUM(2110 POSTED net)`. Tolerance cho async lag deposit POSTED → wallet credit. Drift ngoài SLA → alert, ops reversal + wallet ADJUSTMENT. **Không tự sửa 2110 từ wallet job** ([ADR-014](../../adr/ADR-014-reconciliation-w5-report-only.md)).

#### ADR binding

| ADR | Quyết định liên quan |
|-----|---------------------|
| [ADR-006](../../adr/ADR-006-two-phase-deposit.md) | 2110 chỉ credit sau POSTED phase B |
| [ADR-014](../../adr/ADR-014-reconciliation-w5-report-only.md) | W5 report-only; không auto-adjust 2110 |
| [ADR-020](../../adr/ADR-020-wallet-lanes-coa-control-mapping.md) | 2110 mirror lane USER aggregate |

#### FAQ 3 câu — TK 2110

**Q1:** User A chuyển 100.000 cho user B — 2110 thay đổi thế nào?  
**A:** 2110(A) DR 101.000 (gross+fee) / 3300 CR 101.000; rồi 3300 DR 100.000 / 2110(B) CR 100.000; 3300 DR 1.000 / 4130 CR 1.000. Tổng 2110 net: −1.000 (fee rời platform).

**Q2:** Tại sao deposit phase A không ghi 2110?  
**A:** Tiền đã vào bank nhưng chưa map được VA → user, chưa confirm. Credit 2110 sớm rủi ro double-credit nếu sau đó bank báo mismatch.

**Q3:** W5 drift 50.000 — kế toán có được sửa 2110 trực tiếp không?  
**A:** Không. W5 report-only (ADR-014). Kế toán lập reversal journal + ops tạo ADJUSTMENT trên wallet với businessRef mới, audit trail rõ ràng.

#### Gherkin pointer

```
Scenarios trong acceptance.md:
  → "Deposit Phase A — 2110 unchanged"
  → "Deposit Phase B — 2110 credit net 99k"
  → "Withdraw — 2110 DR gross 101k"
  → "Payment user 2110 DR 100k"
  → "W5 reconciliation drift alert"
```

---

### B.5 TK 2120 — Wallet payable — Merchant

#### Metadata

| Trường | Giá trị |
|--------|---------|
| Mã tài khoản | **2120** |
| Nhóm | 2 — Nợ phải trả |
| Loại | LIABILITY |
| Số dư bình thường | **Có (Credit)** |
| Lane mirror | **MERCHANT** (`wallet_type = 'MERCHANT'`) |
| Use case | WALLET_PAYMENT, QR_PAYMENT, PAYROLL, EOD |
| ADR chính | ADR-020, ADR-014, ADR-010 |

#### Định nghĩa kinh tế & bản chất

TK 2120 là control account tổng hợp thể hiện nợ nền tảng với tất cả merchant. Số dư Có = tổng tiền merchant chưa settle. Merchant nhận tiền qua payment/QR (2120 CR) và bị trừ khi payroll hoặc EOD clearing (2120 DR). EOD settlement chuyển 2120 → 3800 để gửi về ngân hàng merchant.

`[TBD: Chốt policy T+0 vs T+1 settlement cho merchant — ảnh hưởng thời điểm 2120 DR trong EOD.]`

#### T-account

```
         TK 2120 — Wallet payable Merchant
         (Nợ phải trả — Có)
 ─────────────────────────────────────────
  DR (Giảm nợ)    │  CR (Tăng nợ)
 ─────────────────│───────────────────────
  −505.000        │  +100.000  Wallet payment (E13)
  (Payroll gross  │  +100.000  QR payment (E20)
   E21)           │  …
  −200.000        │
  (EOD lock E24)  │
  SD Có: dương   │  Chờ settlement
```

#### Bảng DR/CR theo use case

| use_case | Dòng | TK 2120 | DR | CR | Đối ứng | Nguồn |
|----------|------|---------|----|----|---------|-------|
| WALLET_PAYMENT | Merchant receive | CR | — | 100.000 | 3500 DR | foundation §13 E13 |
| QR_PAYMENT | Merchant receive | CR | — | 100.000 | 3500 DR | foundation §12 E20 |
| PAYROLL | Merchant gross debit | DR | 505.000 | — | 3600 CR | foundation §14 E21 |
| EOD_CLEARING | Lock settlement | DR | 200.000 | — | 3800 CR | foundation §16 E24 |

#### Cặp đối ứng bắt buộc

| 2120 DR khi | Đối ứng CR | 2120 CR khi | Đối ứng DR |
|-------------|-----------|------------|-----------|
| Payroll gross | 3600 | Wallet payment | 3500 |
| EOD clearing lock | 3800 | QR payment | 3500 |

#### Trạng thái PENDING / POSTED

| Trạng thái | Hành vi TK 2120 |
|------------|-----------------|
| PAYMENT POSTED | CR +100.000 per payment |
| PAYROLL POSTED | DR −505.000 (5 × 100k + fee) |
| EOD POSTED | DR −200.000 (clearing lock) |

#### Lỗi thường gặp

| Mã | Mô tả lỗi | Nguyên nhân | Xử lý |
|----|-----------|-------------|-------|
| 2120-E01 | 2120 CR trên IBFT (không phải payment) | IBFT dùng 2110 DR / 1112 | Sai use case mapping |
| 2120-E02 | 2120 DR khi merchant nhận payment | DR giảm nợ merchant sai | Merchant nhận = CR |
| 2120-E03 | Payroll: 2120 DR net thay gross | Quên fee 5.000 trong batch | DR = 505.000 (500k + 5k) |
| 2120-E04 | EOD: 2120 DR sau EOD vẫn dương | EOD không lock hết | Verify EOD covers all settled |
| 2120-E05 | 2120 per-merchant sub-ledger trên coa_* | Nhầm với wallet | Chi tiết thuộc wallet_balance MERCHANT |

#### Recon FR-10 / W5

**W5 MERCHANT:** `SUM(wallet_balance.available + frozen WHERE wallet_type='MERCHANT')` vs SUM(2120 POSTED net). Chú ý EOD lag: 2120 DR sau EOD nhưng wallet credit merchant vẫn còn — tolerance window.

#### ADR binding

| ADR | Quyết định liên quan |
|-----|---------------------|
| [ADR-020](../../adr/ADR-020-wallet-lanes-coa-control-mapping.md) | 2120 mirror lane MERCHANT |
| [ADR-014](../../adr/ADR-014-reconciliation-w5-report-only.md) | W5 report-only cho 2120 |
| [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md) | 3800/3810/3820 = 0 sau EOD |

#### FAQ 3 câu — TK 2120

**Q1:** Merchant có 100 thanh toán trong ngày — 2120 tăng 100 lần CR?  
**A:** Đúng — mỗi PAYMENT POSTED tạo một `coa_trans_data` dòng 2120 CR. Trial balance SUM cho ra tổng.

**Q2:** Sau EOD, số dư 2120 có về 0 không?  
**A:** 2120 về 0 cho phần đã settle. Nếu có merchant chưa settle (T+1), 2120 vẫn giữ phần đó đến EOD tiếp. `[TBD: Chốt T+0/T+1 policy.]`

**Q3:** Payroll DR 505.000 từ 2120 — nếu merchant không đủ số dư thì sao?  
**A:** Wallet kiểm tra available trước khi orchestration build journal; nếu insufficient → reject, không tạo journal. Accounting không kiểm tra business rule này.

#### Gherkin pointer

```
Scenarios trong acceptance.md:
  → "Wallet payment — 2120 CR 100k"
  → "QR payment — 2120 CR merchant"
  → "Payroll — 2120 DR 505k"
  → "EOD clearing lock — 2120 DR"
```

---

### B.6 TK 2130 — Escrow payable — Disbursement partner

#### Metadata

| Trường | Giá trị |
|--------|---------|
| Mã tài khoản | **2130** |
| Nhóm | 2 — Nợ phải trả |
| Loại | LIABILITY |
| Số dư bình thường | **Có (Credit)** |
| Lane mirror | **PARTNER** (`wallet_type = 'PARTNER'`) |
| Use case | DISBURSEMENT (pre-fund + disburse) |
| ADR chính | ADR-020, ADR-010 |

#### Định nghĩa kinh tế & bản chất

TK 2130 là escrow liability cho đối tác disbursement (fintech partner). Pre-fund: platform nhận tiền từ partner (1111 DR / 2130 CR) — escrow. Khi disburse thực: 2130 DR gross → 3700 transit → 1112 outbound. Số dư 2130 = tiền đang giữ hộ partner chưa giải ngân.

`[TBD: Chốt model — partner transfer trước hay after-the-fact settlement.]`

#### T-account

```
         TK 2130 — Escrow Disbursement partner
         (Nợ phải trả — Có)
 ─────────────────────────────────────────
  DR (Giải ngân)  │  CR (Pre-fund nhận)
 ─────────────────│───────────────────────
  −101.000        │  +100.000  Pre-fund (E23 variant)
  (Disburse gross │
   → 3700)        │
  SD Có: dương   │  Escrow chưa giải
```

#### Bảng DR/CR theo use case

| use_case | Dòng | TK 2130 | DR | CR | Đối ứng | Nguồn |
|----------|------|---------|----|----|---------|-------|
| DISBURSEMENT | Pre-fund | CR | — | 100.000 | 1111 DR | foundation §15 E23 |
| DISBURSEMENT | Disburse gross | DR | 101.000 | — | 3700 CR | foundation §15 |

#### Cặp đối ứng bắt buộc

| 2130 CR khi | Đối ứng | 2130 DR khi | Đối ứng |
|-------------|---------|------------|---------|
| Partner pre-fund vào | 1111 DR | Giải ngân gross | 3700 CR |

#### Trạng thái PENDING / POSTED

| Trạng thái | Hành vi |
|------------|---------|
| Pre-fund POSTED | CR +100.000 |
| Disburse POSTED | DR −101.000 (principal + fee) |

#### Lỗi thường gặp

| Mã | Mô tả lỗi | Nguyên nhân | Xử lý |
|----|-----------|-------------|-------|
| 2130-E01 | Bỏ qua pre-fund, ghi 2130 DR trực tiếp | Thiếu bước pre-fund | Thêm pre-fund journal |
| 2130-E02 | 2130 trong payment journal | Payment dùng 2110/2120 | Sai template |
| 2130-E03 | 2130 DR net thay gross | Quên fee line | Gross = principal + fee |
| 2130-E04 | 2130 âm sau disburse | Disburse > pre-fund | Kiểm tra available partner |
| 2130-E05 | Dùng 2120 thay 2130 cho partner | Nhầm lane | Partner = PARTNER lane → 2130 |

#### Recon FR-10 / W5

**W5 PARTNER:** SUM(wallet_balance PARTNER) vs SUM(2130 POSTED net). Đặc biệt kiểm tra 2130 = 0 sau khi partner giải ngân xong batch.

#### ADR binding

| ADR | Quyết định liên quan |
|-----|---------------------|
| [ADR-020](../../adr/ADR-020-wallet-lanes-coa-control-mapping.md) | 2130 mirror lane PARTNER |
| [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md) | 3700 = 0 sau DISBURSEMENT POSTED |

#### FAQ 3 câu — TK 2130

**Q1:** Tại sao disbursement cần pre-fund riêng thay vì debit merchant 2120?  
**A:** Disbursement partner là PARTNER lane riêng, không phải merchant. Pre-fund escrow tách bạch liability partner vs merchant, tránh nhầm W5 lane.

**Q2:** Sau khi disburse 100.000 (principal) + fee 1.000, 2130 net là bao nhiêu?  
**A:** Nếu pre-fund 100.000 rồi disburse 101.000 → 2130 net = 100.000 − 101.000 = −1.000 (âm — cần top-up). Thực tế partner pre-fund gross trước.

**Q3:** 2130 và 2110 có tổng vào invariant 1 không?  
**A:** Có: `INV-1 = 1111+1112+1113 = 2110+2120+2130`. 2130 là thành phần liability side.

#### Gherkin pointer

```
Scenarios trong acceptance.md:
  → "Disbursement pre-fund POSTED"    (1111 DR 100k / 2130 CR 100k)
  → "Disbursement disburse POSTED"    (2130 DR 101k; 3700=0; 1112 CR)
```


---

### B.7 TK 3100 — Transit — Deposit

#### Metadata

| Trường | Giá trị |
|--------|---------|
| Mã tài khoản | **3100** |
| Nhóm | 3 — Transit |
| Loại | TRANSIT |
| Số dư bình thường | **Net zero** tại POSTED |
| Lane mirror | — |
| Use case | DEPOSIT (Phase A + B) |
| ADR chính | ADR-006, ADR-010 |

#### Định nghĩa kinh tế & bản chất

TK 3100 là **transit duy nhất** của luồng deposit. Phase A: `1111 DR / 3100 CR` — tiền đã vào bank nhưng user chưa được credit (chờ map VA → userId và confirm). Phase B: `3100 DR` để clear transit, sau đó `2110 CR net` + `4110 CR fee`. Khi POSTED hoàn thành: **3100 = 0**.

3100 ≠ 0 chỉ hợp lệ trong trạng thái PENDING. Ops monitor aging 3100 để phát hiện deposit stuck. Period close block nếu 3100 ≠ 0 ngoài waiver list.

#### T-account

```
         TK 3100 — Transit Deposit
         (Transit — Net zero)
 ─────────────────────────────────────────
  DR (Clear)      │  CR (Hold)
 ─────────────────│───────────────────────
  +100.000        │  +100.000  Phase A CR (E02)
  Phase B clear   │            (lock in-flight)
  (E03 step 1)    │
                  │
  SD = 0         │  (sau POSTED phase B)
```

#### Bảng DR/CR theo use case

| use_case | Phase | TK 3100 | DR | CR | Đối ứng | Nguồn |
|----------|-------|---------|----|----|---------|-------|
| DEPOSIT | A PENDING | CR | — | 100.000 | 1111 DR | foundation §8 E02 |
| DEPOSIT | B POSTED | DR | 100.000 | — | 2110 CR 99k + 2110 DR 1k + 4110 CR 1k | foundation §8 E03 |
| DEPOSIT | FAILED reverse | DR | 100.000 | — | 1111 CR | foundation §8.5 E05 |

#### Cặp đối ứng bắt buộc

| 3100 CR khi | Đối ứng | 3100 DR khi | Đối ứng |
|-------------|---------|------------|---------|
| Phase A hold | 1111 DR | Phase B clear | 2110 CR + 4110 CR |
| — | — | FAILED reverse | 1111 CR |

#### Trạng thái PENDING / POSTED

| Trạng thái `coa_trans` | 3100 net | Hành vi |
|------------------------|---------|---------|
| PENDING (phase A done) | +100.000 Có | Hold in-flight — hợp lệ |
| POSTED (phase B done) | **0** | Clear — bắt buộc ADR-010 |
| FAILED | 0 | Đã reverse phase A |

#### Lỗi thường gặp

| Mã | Mô tả lỗi | Nguyên nhân | Xử lý |
|----|-----------|-------------|-------|
| 3100-E01 | 3100 ≠ 0 sau POSTED deposit | Thiếu clear dòng phase B | ADR-010 vi phạm; review confirmDeposit |
| 3100-E02 | 3100 DR không khớp 3100 CR trước đó | Amount mismatch phase A/B | Validate coa_trans_id và amount |
| 3100-E03 | 3100 xuất hiện trong WITHDRAW journal | Transit withdraw = 3200 | Sai template |
| 3100-E04 | Deposit POSTED nhưng 3100 CR thêm dòng | Duplicate phase A ghi | business_ref UNIQUE |
| 3100-E05 | Aging 3100 PENDING > 24h không alert | Thiếu monitoring | Cấu hình aging alert |
| 3100-E06 | Phase B ghi `addLines` thay `confirmDeposit` | Bypass ADR-006 | Bắt buộc dùng `confirmDeposit` |

#### Recon FR-10 / W5

**Transit monitoring:** `SELECT SUM(net) WHERE account_code='3100' GROUP BY coa_trans_id` — mỗi deposit PENDING có 3100 Có +100k; sau POSTED = 0. Alert nếu có PENDING > SLA aging threshold. FR-10 không trực tiếp recon 3100; 3100 là nội bộ tracking.

#### ADR binding

| ADR | Quyết định liên quan |
|-----|---------------------|
| [ADR-006](../../adr/ADR-006-two-phase-deposit.md) | 3100 lifecycle: phase A hold, phase B clear |
| [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md) | 3100 = 0 mandatory khi POSTED |
| [ADR-001](../../adr/ADR-001-immutable-ledger.md) | Phase A lines immutable sau ghi; phase B thêm lines mới |

#### FAQ 3 câu — TK 3100

**Q1:** Khi nào 3100 ≠ 0 là bình thường?  
**A:** Chỉ khi `coa_trans.status = 'PENDING'` — deposit đã vào bank nhưng chưa confirm. Mọi POSTED deposit phải có 3100 = 0.

**Q2:** Nếu phase B fail ở giữa (credit 2110 ok nhưng 4110 fail), 3100 = 0 chưa?  
**A:** Chưa — `confirmDeposit` ghi tất cả phase B lines trong một transaction; nếu fail, rollback, 3100 vẫn ≠ 0. Retry sau.

**Q3:** Deposit FAILED — đảo phase A hay phase B?  
**A:** Chỉ đảo phase A (1111 CR / 3100 DR). Nếu chưa có phase B (user chưa được credit), không có gì thêm để đảo.

#### Gherkin pointer

```
Scenarios trong acceptance.md:
  → "Deposit Phase A — 3100 CR 100k PENDING"
  → "Deposit Phase B — 3100 = 0 POSTED"
  → "Deposit FAILED — 3100 reverse"
  → "Period close block — 3100 non-zero"
```

---

### B.8 TK 3200 — Transit — Withdraw

#### Metadata

| Trường | Giá trị |
|--------|---------|
| Mã tài khoản | **3200** |
| Nhóm | 3 — Transit |
| Loại | TRANSIT |
| Số dư bình thường | **Net zero** tại POSTED |
| Lane mirror | — |
| Use case | WITHDRAW |
| ADR chính | ADR-010 |

#### Định nghĩa kinh tế & bản chất

TK 3200 là transit của luồng withdraw — giữ tiền in-flight trong khoảng thời gian orchestration xử lý từ debit wallet đến bank payout. Khác 3100 (2 phase), 3200 thường complete trong **một journal POSTED** với 5 dòng: `2110 DR 101k / 3200 CR 101k / 3200 DR 100k / 1111 CR 100k / 3200 DR 1k / 4120 CR 1k`. Kết quả: 3200 = 0.

#### T-account

```
         TK 3200 — Transit Withdraw
         (Transit — Net zero)
 ─────────────────────────────────────────
  DR (Clear)           │  CR (Hold)
 ─────────────────────│───────────────────
  +100.000 bank payout │  +101.000  User gross hold (E06)
  +1.000 fee           │
                       │
  SD = 0              │  (POSTED)
```

#### Bảng DR/CR theo use case

| Bước | TK 3200 | DR | CR | Đối ứng | Nguồn |
|------|---------|----|----|---------|-------|
| User gross hold | CR | — | 101.000 | 2110 DR | foundation §9 E06 |
| Bank payout | DR | 100.000 | — | 1111 CR | foundation §9 E07 |
| Fee revenue | DR | 1.000 | — | 4120 CR | foundation §9 E08 |

Net: DR 101.000 = CR 101.000 → **3200 = 0**

#### Cặp đối ứng bắt buộc

| 3200 CR | Đối ứng | 3200 DR | Đối ứng |
|---------|---------|---------|---------|
| Gross hold | 2110 DR 101k | Bank payout leg | 1111 CR 100k |
| — | — | Fee leg | 4120 CR 1k |

#### Trạng thái PENDING / POSTED

| Trạng thái | 3200 net | Ghi chú |
|------------|---------|---------|
| Trong journal đơn POSTED | 0 | Tất cả lines trong 1 transaction |
| FAILED / reverse | 0 (sau đảo) | Reversal journal |

#### Lỗi thường gặp

| Mã | Mô tả lỗi | Nguyên nhân | Xử lý |
|----|-----------|-------------|-------|
| 3200-E01 | 3200 ≠ 0 sau POSTED | Thiếu DR fee line hoặc bank line | Validate transit zero |
| 3200-E02 | 3200 CR net thay gross (100k không 101k) | Quên fee line trên hold | 2110 DR 101k / 3200 CR 101k đủ gross |
| 3200-E03 | 3200 xuất hiện trong DEPOSIT journal | Deposit transit = 3100 | Sai template |
| 3200-E04 | 1111 CR = 101.000 thay 100.000 | Gộp fee vào bank payout | Bank nhận đúng principal; fee 4120 |
| 3200-E05 | Thiếu dòng 4120 | Quên fee revenue | DR ≠ CR fail |

#### Recon FR-10 / W5

**Monitoring:** 3200 không có PENDING phase — luôn = 0 sau POSTED. Nếu 3200 ≠ 0 trong ledger → stuck journal; alert ngay.

#### ADR binding

| ADR | Quyết định |
|-----|-----------|
| [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md) | 3200 = 0 mandatory POSTED |

#### FAQ 3 câu — TK 3200

**Q1:** Withdraw không có 2 phase như deposit — 3200 có staging trung gian không?  
**A:** Không — withdraw là sync: journal POSTED một lần với 5 dòng, 3200 về 0 ngay. Nếu bank callback async, kiến trúc xử lý ngoài journal (saga/outbox) — `[TBD: Chốt async withdraw flow.]`

**Q2:** User rút 100.000, bị trừ 101.000 — 3200 CR bao nhiêu?  
**A:** 3200 CR 101.000 (gross = principal 100k + fee 1k). Sau đó 3200 DR 100k (bank) + 3200 DR 1k (fee) = 3200 = 0.

**Q3:** 4120 CR 1.000 nằm trong cùng journal 3200 không?  
**A:** Có — tất cả 5 dòng là lines trong 1 `coa_trans`. DR total = 101.000; CR total = 101.000.

#### Gherkin pointer

```
Scenarios trong acceptance.md:
  → "Withdraw — ledger POSTED 3200=0"  (2110 DR 101k; 1111 CR 100k; 4120 CR 1k)
```

---

### B.9 TK 3300 — Transit — Internal transfer

#### Metadata

| Trường | Giá trị |
|--------|---------|
| Mã tài khoản | **3300** |
| Nhóm | 3 — Transit |
| Loại | TRANSIT |
| Số dư bình thường | **Net zero** tại POSTED |
| Lane mirror | — |
| Use case | INTERNAL_TRANSFER |
| ADR chính | ADR-010 |

#### Định nghĩa kinh tế & bản chất

TK 3300 là transit cho chuyển khoản nội bộ ví-sang-ví trong cùng platform. **Không có dòng 111x** — tiền không rời bank. 3300 CR 101.000 (hold gross từ user A) → 3300 DR 100.000 (credit user B) + 3300 DR 1.000 (fee). Net 3300 = 0. Đây là use case duy nhất có hai dòng 2110 trong cùng journal (A ghi Nợ, B ghi Có).

#### T-account

```
         TK 3300 — Transit Internal Transfer
         (Transit — Net zero)
 ─────────────────────────────────────────
  DR (Clear)           │  CR (Hold)
 ─────────────────────│───────────────────
  +100.000 credit B    │  +101.000  User A gross (E09)
  +1.000 fee           │
                       │
  SD = 0              │  (POSTED)
```

#### Bảng DR/CR theo use case

| Bước | TK 3300 | DR | CR | Đối ứng | Nguồn |
|------|---------|----|----|---------|-------|
| User A gross | CR | — | 101.000 | 2110(A) DR | foundation §10 E09 |
| Credit user B | DR | 100.000 | — | 2110(B) CR | foundation §10 E10 |
| Fee revenue | DR | 1.000 | — | 4130 CR | foundation §10 E11 |

Net: DR 101.000 = CR 101.000 → **3300 = 0**

#### Cặp đối ứng bắt buộc

| 3300 CR | Đối ứng | 3300 DR | Đối ứng |
|---------|---------|---------|---------|
| A gross | 2110(A) DR 101k | B credit | 2110(B) CR 100k |
| — | — | Fee | 4130 CR 1k |

#### Trạng thái PENDING / POSTED

Internal transfer không có PENDING phase — 1 journal POSTED trực tiếp (trừ khi có saga async `[TBD]`).

#### Lỗi thường gặp

| Mã | Mô tả lỗi | Nguyên nhân | Xử lý |
|----|-----------|-------------|-------|
| 3300-E01 | Có dòng 111x trong journal transfer | Transfer nội bộ không qua bank | Xóa 111x lines |
| 3300-E02 | 3300 CR 100k (net) thay 101k (gross) | Không include fee trong hold | A phải DR 101k gross |
| 3300-E03 | 2110(A) DR = 2110(B) CR (bằng nhau) | Quên fee; zero-fee OK nếu dụng ý | Kiểm tra fee policy |
| 3300-E04 | Nhầm 4130 với 4110 (phí nạp) | Fee transfer = 4130, không 4110 | Fix revenue TK |
| 3300-E05 | 3300 ≠ 0 sau POSTED | Thiếu DR fee line | Validate transit zero |

#### Recon FR-10 / W5

3300 không tham gia FR-10 bank (không bank movement). W5: 2110 net giảm theo fee 1.000 mỗi transfer; reconcile aggregate thường.

#### ADR binding

| ADR | Quyết định |
|-----|-----------|
| [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md) | 3300 = 0 POSTED |

#### FAQ 3 câu — TK 3300

**Q1:** Tại sao cần transit 3300 cho chuyển khoản nội bộ — không ghi thẳng 2110(A) DR / 2110(B) CR?  
**A:** Transit tách hai legs rõ ràng, dễ trace, và cho phép 4130 fee line tự nhiên. Nếu ghi thẳng, DR ≠ CR khi có fee.

**Q2:** Cùng user chuyển cho chính mình (A=B) có xảy ra không?  
**A:** Thiết kế nên block use case này tại Application/orchestration level. Nếu xảy ra: 2110 DR và CR same TK, net = 0; 4130 CR fee. `[TBD: Business rule same-user transfer.]`

**Q3:** Internal transfer 0 fee — journal có dòng 4130 không?  
**A:** Không — nếu fee = 0, không tạo dòng 4130. Journal: 2110(A) DR 100k / 3300 CR 100k; 3300 DR 100k / 2110(B) CR 100k. 3300 = 0.

#### Gherkin pointer

```
Scenarios trong acceptance.md:
  → "Internal transfer — 3300=0 POSTED"  (2110A DR 101k; 2110B CR 100k; 4130 CR 1k)
```

---

### B.10 TK 3400 — Transit — IBFT

#### Metadata

| Trường | Giá trị |
|--------|---------|
| Mã tài khoản | **3400** |
| Nhóm | 3 — Transit |
| Loại | TRANSIT |
| Số dư bình thường | **Net zero** tại POSTED |
| Lane mirror | — |
| Use case | IBFT (Interbank Fund Transfer) |
| ADR chính | ADR-025, ADR-010 |

#### Định nghĩa kinh tế & bản chất

TK 3400 là transit cho IBFT — chuyển khoản liên ngân hàng qua Napas. Khác internal transfer: tiền rời platform qua 1112 Napas Clearing. Journal 8 dòng: user gross hold → fee revenue 4130 → bank outbound 1112 → Napas cost 5100. **1111 không xuất hiện** ([ADR-025](../../adr/ADR-025-ibft-napas-clearing-1112.md)).

IBFT có thể async (Napas callback) — staging PENDING cho 3400 là hợp lệ nếu thiết kế saga. `[TBD: Chốt sync vs async IBFT — ảnh hưởng lifecycle 3400.]`

#### T-account

```
         TK 3400 — Transit IBFT
         (Transit — Net zero)
 ─────────────────────────────────────────
  DR (Clear)           │  CR (Hold)
 ─────────────────────│───────────────────
  +1.000 fee           │  +101.000  User gross (E14)
  +100.000 bank out    │
                       │
  SD = 0              │  (POSTED)
  (5100 không qua 3400)│
```

#### Bảng DR/CR theo use case

| Bước | TK 3400 | DR | CR | Đối ứng | Nguồn |
|------|---------|----|----|---------|-------|
| User gross hold | CR | — | 101.000 | 2110 DR | foundation §11 E14 |
| Fee revenue | DR | 1.000 | — | 4130 CR | foundation §11 E15 |
| Bank outbound | DR | 100.000 | — | 1112 CR | foundation §11 E16 |
| Napas cost | — | — | — | 5100 DR / 1112 CR (+500) | foundation §11 E17 (outside 3400) |

Net 3400: DR 101.000 = CR 101.000 → **3400 = 0**
Dòng 5100/1112 độc lập — không qua 3400.

#### Cặp đối ứng bắt buộc

| 3400 CR | Đối ứng | 3400 DR | Đối ứng |
|---------|---------|---------|---------|
| User gross | 2110 DR 101k | Fee | 4130 CR 1k |
| — | — | Bank outbound | 1112 CR 100k |

> Note: 5100 DR / 1112 CR là cặp riêng, tổng journal DR=CR vẫn cân nhờ cả 8 dòng.

#### Trạng thái PENDING / POSTED

| Trạng thái | 3400 net | Ghi chú |
|------------|---------|---------|
| IBFT POSTED | 0 | Bắt buộc ADR-010 |
| `[TBD: IBFT async PENDING]` | ≠ 0 tạm | Nếu Napas callback async |

#### Lỗi thường gặp

| Mã | Mô tả lỗi | Nguyên nhân | Xử lý |
|----|-----------|-------------|-------|
| 3400-E01 | IBFT dùng 1111 thay 1112 | DC-04; vi phạm ADR-025 | Fix template; retest |
| 3400-E02 | 5100 không có trong journal | Quên Napas cost | Thêm cost line |
| 3400-E03 | 3400 ≠ 0 sau POSTED | Thiếu DR line (fee hoặc bank) | Validate transit zero |
| 3400-E04 | 4130 nhầm thành 4120 (phí rút) | Nhầm revenue TK | IBFT fee = 4130, không 4120 |
| 3400-E05 | 5100 qua 3400 thay vì line riêng | Hiểu sai cấu trúc | 5100 là cặp riêng với 1112 |
| 3400-E06 | 2110 DR net 100k thay 101k | Quên fee hold | User chịu gross principal + fee |

#### Recon FR-10 / W5

**FR-10 Napas:** SUM(1112 POSTED DR-CR) cho kỳ = số dư Napas. 3400 không trực tiếp recon nhưng net 0 sau POSTED là điều kiện đủ journal hợp lệ.

#### ADR binding

| ADR | Quyết định |
|-----|-----------|
| [ADR-025](../../adr/ADR-025-ibft-napas-clearing-1112.md) | IBFT bank leg = 1112 |
| [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md) | 3400 = 0 POSTED |

#### FAQ 3 câu — TK 3400

**Q1:** Profit platform từ IBFT là bao nhiêu?  
**A:** 4130 CR +1.000 − 5100 DR +500 = **net +500** (E16–E17). Đây là matching principle.

**Q2:** 5100 DR có thuộc transit 3400 không?  
**A:** Không — 5100 DR / 1112 CR là cặp riêng trong cùng journal. Khi kiểm tra transit: chỉ check 3400 net = 0, không include 5100.

**Q3:** Nếu Napas reject IBFT sau khi đã POSTED, xử lý thế nào?  
**A:** Tạo reversal journal (âm ngược tất cả 8 dòng). Wallet refund user gross. Cần saga retry hoặc ops xử lý. `[TBD: Chốt Napas reject flow.]`

#### Gherkin pointer

```
Scenarios trong acceptance.md:
  → "IBFT happy path"              (3400=0; 1112 CR 100.5k; 5100 DR 500; 4130 CR 1k)
  → "IBFT — 1112 not 1111"         (IBFT-E05)
  → "IBFT — 5100 present"          (IBFT-E01)
```

---

### B.11 TK 3500 — Transit — Payment

#### Metadata

| Trường | Giá trị |
|--------|---------|
| Mã tài khoản | **3500** |
| Nhóm | 3 — Transit |
| Loại | TRANSIT |
| Số dư bình thường | **Net zero** tại POSTED |
| Lane mirror | — |
| Use case | WALLET_PAYMENT, QR_PAYMENT |
| ADR chính | ADR-010 |

#### Định nghĩa kinh tế & bản chất

TK 3500 phục vụ **hai** use case payment: wallet-to-merchant (không bank) và QR/POS (acquirer VPBank). Hai luồng khác nhau phía input nhưng cùng output: 3500 DR / 2120 CR (merchant credit). **Wallet payment**: `2110 DR / 3500 CR / 3500 DR / 2120 CR` — không có 111x. **QR payment**: `1113 DR / 3500 CR` (acquirer in) + `5100 DR / 1113 CR` (cost) + `3500 DR / 2120 CR`.

Trong cả hai case, **3500 = 0** sau POSTED.

#### T-account

```
         TK 3500 — Transit Payment
         (Transit — Net zero)
 ─────────────────────────────────────────────────────
  Wallet Payment             │  QR Payment
  DR (Clear)  │ CR (Hold)    │  DR (Clear)  │ CR (Hold)
 ─────────────┼──────────────│─────────────┼───────────
  +100k merch │ +100k user   │  +100k merch │ +100k 1113
              │              │              │
  SD = 0     │              │  SD = 0     │
```

#### Bảng DR/CR theo use case

| use_case | Bước | TK 3500 | DR | CR | Đối ứng | Nguồn |
|----------|------|---------|----|----|---------|-------|
| WALLET_PAYMENT | User hold | CR | — | 100.000 | 2110 DR | foundation §13 E12 |
| WALLET_PAYMENT | Merchant credit | DR | 100.000 | — | 2120 CR | foundation §13 E13 |
| QR_PAYMENT | Acquirer in | CR | — | 100.000 | 1113 DR | foundation §12 E18 |
| QR_PAYMENT | Merchant credit | DR | 100.000 | — | 2120 CR | foundation §12 E20 |

#### Cặp đối ứng bắt buộc

| 3500 CR | Đối ứng | 3500 DR | Đối ứng |
|---------|---------|---------|---------|
| WALLET: user debit | 2110 DR | WALLET: merchant credit | 2120 CR |
| QR: acquirer in | 1113 DR | QR: merchant credit | 2120 CR |

#### Trạng thái PENDING / POSTED

| Trạng thái | 3500 | Ghi chú |
|------------|------|---------|
| POSTED (both) | 0 | Sync flow đơn giản |

#### Lỗi thường gặp

| Mã | Mô tả lỗi | Nguyên nhân | Xử lý |
|----|-----------|-------------|-------|
| 3500-E01 | Wallet payment có dòng 111x | Không qua bank — xóa | foundation §13 |
| 3500-E02 | QR: 1111 DR thay 1113 DR | Sai acquirer TK | Fix template QR |
| 3500-E03 | 3500 ≠ 0 sau POSTED | Thiếu merchant credit | Validate transit zero |
| 3500-E04 | 2110 CR thay 2120 CR (merchant) | Nhầm lane merchant/user | Merchant = 2120 |
| 3500-E05 | Wallet payment: phí xuất hiện | Wallet payment thường 0 fee | `[TBD: fee model]` |

#### Recon FR-10 / W5

3500 không bank movement với wallet payment. QR: FR-10 VPBank (1113) indirect. W5: 2120 tăng theo payment; 2110 giảm.

#### ADR binding

| ADR | Quyết định |
|-----|-----------|
| [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md) | 3500 = 0 POSTED cả hai use case |

#### FAQ 3 câu — TK 3500

**Q1:** 3500 dùng cho cả wallet payment và QR — có nhầm lẫn không?  
**A:** Thiết kế gộp vì cùng bản chất "payment transit". Phân biệt qua `coa_trans.use_case = 'WALLET_PAYMENT'` vs `'QR_PAYMENT'`.

**Q2:** Wallet payment có fee không?  
**A:** `[TBD: Chốt fee model wallet payment.]` Hiện ví dụ chuẩn không có fee; nếu có fee → thêm dòng `2110 DR fee / 4xxx CR fee`.

**Q3:** Merchant nhận 2120 CR — khi nào merchant có thể withdraw?  
**A:** Sau EOD settlement merchant tự transfer hoặc qua payroll. 2120 giảm (DR) khi EOD clearing lock. `[TBD: T+0/T+1 policy.]`

#### Gherkin pointer

```
Scenarios trong acceptance.md:
  → "Wallet payment — 3500=0"  (2110 DR 100k; 2120 CR 100k)
  → "QR payment — 3500=0"      (1113 DR 100k; 5100 DR 500; 2120 CR 100k)
```

---

### B.12 TK 3600 — Transit — Payroll

#### Metadata

| Trường | Giá trị |
|--------|---------|
| Mã tài khoản | **3600** |
| Nhóm | 3 — Transit |
| Loại | TRANSIT |
| Số dư bình thường | **Net zero** tại POSTED |
| Lane mirror | — |
| Use case | PAYROLL |
| ADR chính | ADR-010 |

#### Định nghĩa kinh tế & bản chất

TK 3600 là transit của luồng payroll — merchant trả lương cho nhân viên qua Napas. Merchant bị DR gross (500.000 + fee 5.000 = 505.000) từ 2120. Transit 3600 giữ in-flight. Sau đó 3600 DR: trả fee 4150, transfer qua Napas (1112 CR), trả Napas cost (5100 DR). **3600 = 0** sau POSTED. Ví dụ chuẩn: **5 employees × 100.000 + fee 5.000**.

#### T-account

```
         TK 3600 — Transit Payroll
         (Transit — Net zero)
 ─────────────────────────────────────────
  DR (Clear)           │  CR (Hold)
 ─────────────────────│───────────────────
  +5.000  fee (E21b)   │  +505.000  Merchant gross
  +500.000 bank out    │            (2120 DR, E21)
                       │
  SD = 0              │  (5.000+500.000=505.000)
  (5100/1112 riêng)   │
```

#### Bảng DR/CR theo use case

| Bước | TK 3600 | DR | CR | Đối ứng | Nguồn |
|------|---------|----|----|---------|-------|
| Merchant gross | CR | — | 505.000 | 2120 DR | foundation §14 E21 |
| Fee revenue | DR | 5.000 | — | 4150 CR | foundation §14 |
| Bank outbound | DR | 500.000 | — | 1112 CR | foundation §14 E22 |
| Napas cost (×5) | — (riêng) | — | — | 5100 DR 2.500 / 1112 CR 2.500 | foundation §14 |

Net 3600: DR 505.000 = CR 505.000 → **3600 = 0**

#### Cặp đối ứng bắt buộc

| 3600 CR | Đối ứng | 3600 DR | Đối ứng |
|---------|---------|---------|---------|
| Merchant gross | 2120 DR 505k | Fee | 4150 CR 5k |
| — | — | Bank batch | 1112 CR 500k |

#### Trạng thái PENDING / POSTED

| Trạng thái | Ghi chú |
|------------|---------|
| PAYROLL POSTED | 3600 = 0; 2120 giảm 505k; 1112 giảm 502.5k; 4150 +5k; 5100 +2.5k |

#### Lỗi thường gặp

| Mã | Mô tả lỗi | Nguyên nhân | Xử lý |
|----|-----------|-------------|-------|
| 3600-E01 | 3600 CR 500k (net) thay 505k (gross) | Quên fee trong hold | Merchant gross = principal + fee |
| 3600-E02 | 5100 DR 500 thay 2.500 | Nhân per-txn thay per-batch | 5×500=2.500 Napas |
| 3600-E03 | 4150 nhầm thành 4130 | Nhầm revenue payroll/transfer | Payroll fee = 4150 |
| 3600-E04 | 1112 CR 502.500 thay 500.000 | Gộp Napas cost vào bank outbound | Tách riêng: bank 500k + cost 2.5k |
| 3600-E05 | 3600 ≠ 0 sau POSTED | Thiếu DR hoặc amount sai | Validate |

#### Recon FR-10 / W5

FR-10 Napas: 1112 CR tổng từ payroll batch. W5: 2120 giảm gross; kiểm tra wallet merchant sau payroll.

#### ADR binding

| ADR | Quyết định |
|-----|-----------|
| [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md) | 3600 = 0 POSTED |

#### FAQ 3 câu — TK 3600

**Q1:** Payroll 5 employees → 5 journals hay 1 journal?  
**A:** `[TBD: Chốt batch design.]` Ví dụ chuẩn mô tả 1 journal với lines đại diện batch. Nếu per-employee thì 5 journals, mỗi cái 3600=0.

**Q2:** Napas cost 2.500 = 5 × 500 — nếu số employees thay đổi?  
**A:** Cost = N × 500. Template orchestration tính động. Journal lines cập nhật theo N.

**Q3:** Merchant không đủ số dư 505.000 — hệ thống từ chối ở đâu?  
**A:** Wallet kiểm tra 2120 available tại orchestration trước khi build lines. Accounting nhận lines đã validated; không có partial-payroll.

#### Gherkin pointer

```
Scenarios trong acceptance.md:
  → "Payroll — 5 employees POSTED"  (2120 DR 505k; 1112 CR 500k; 5100 DR 2.5k; 4150 CR 5k; 3600=0)
```

---

### B.13 TK 3700 — Transit — Disbursement

#### Metadata

| Trường | Giá trị |
|--------|---------|
| Mã tài khoản | **3700** |
| Nhóm | 3 — Transit |
| Loại | TRANSIT |
| Số dư bình thường | **Net zero** tại POSTED |
| Lane mirror | — |
| Use case | DISBURSEMENT |
| ADR chính | ADR-010 |

#### Định nghĩa kinh tế & bản chất

TK 3700 là transit của luồng disbursement — giải ngân từ escrow partner (2130) ra Napas (1112). Flow: `2130 DR 101.000 / 3700 CR 101.000` → `3700 DR 5.000 / 4150 CR 5.000` (fee) → `3700 DR 100.000 / 1112 CR 100.000` (bank) → `5100 DR 500 / 1112 CR 500` (cost). 3700 = 0 sau POSTED. Cần pre-fund trước (TK 2130).

#### T-account

```
         TK 3700 — Transit Disbursement
         (Transit — Net zero)
 ─────────────────────────────────────────
  DR (Clear)           │  CR (Hold)
 ─────────────────────│───────────────────
  +1.000 fee           │  +101.000  Escrow gross
  +100.000 bank out    │            (2130 DR)
                       │
  SD = 0              │  (POSTED)
```

#### Bảng DR/CR theo use case

| Bước | TK 3700 | DR | CR | Đối ứng | Nguồn |
|------|---------|----|----|---------|-------|
| Escrow gross | CR | — | 101.000 | 2130 DR | foundation §15 |
| Fee revenue | DR | 1.000 | — | 4150 CR | foundation §15 |
| Bank outbound | DR | 100.000 | — | 1112 CR | foundation §15 |
| Napas cost | — (riêng) | — | — | 5100 DR 500 / 1112 CR 500 | foundation §15 |

#### Cặp đối ứng bắt buộc

| 3700 CR | Đối ứng | 3700 DR | Đối ứng |
|---------|---------|---------|---------|
| Escrow gross | 2130 DR | Fee | 4150 CR |
| — | — | Bank | 1112 CR |

#### Trạng thái PENDING / POSTED

3700 thường sync POSTED; nếu Napas async → staging PENDING. `[TBD: Chốt flow.]`

#### Lỗi thường gặp

| Mã | Mô tả lỗi | Nguyên nhân | Xử lý |
|----|-----------|-------------|-------|
| 3700-E01 | 3700 mà không có pre-fund 2130 | Thiếu bước pre-fund | Phải pre-fund trước |
| 3700-E02 | 4150 nhầm 4130 | Nhầm disbursement/transfer fee | Disbursement = 4150 |
| 3700-E03 | Napas cost thiếu 5100 | Quên cost line | Thêm 5100 DR / 1112 CR |
| 3700-E04 | 3700 ≠ 0 sau POSTED | Amount mismatch | Validate |
| 3700-E05 | 1111 thay 1112 cho bank outbound | IBFT/disburse đều dùng 1112 | Napas rail = 1112 |

#### Recon / ADR binding

FR-10 Napas tracking. [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md): 3700 = 0.

#### FAQ 3 câu — TK 3700

**Q1:** Pre-fund và disburse là 2 journal riêng biệt?  
**A:** Đúng — pre-fund: `1111 DR / 2130 CR` (journal INIT_PREFUND). Disburse: `2130 DR / 3700 CR / ...` (journal DISBURSEMENT). Tách để audit trail rõ ràng.

**Q2:** 4150 dùng cho cả payroll và disbursement — phân biệt bằng gì?  
**A:** Phân biệt qua `coa_trans.use_case = 'PAYROLL'` vs `'DISBURSEMENT'`. Cùng TK revenue nhưng có thể tách báo cáo theo use_case filter.

**Q3:** Partner escrow 2130 = 0 sau disburse — có đúng không?  
**A:** Đúng nếu disburse đúng bằng pre-fund. Nếu pre-fund 100k, disburse 101k (có fee) → 2130 = −1.000 (partner nợ thêm). Partner phải top-up.

#### Gherkin pointer

```
Scenarios trong acceptance.md:
  → "Disbursement disburse POSTED"  (2130 DR 101k; 3700=0; 1112 CR 100k; 4150 CR 1k)
```

---

### B.14 TK 3800 — Transit — Clearing lock

#### Metadata

| Trường | Giá trị |
|--------|---------|
| Mã tài khoản | **3800** |
| Nhóm | 3 — Transit |
| Loại | TRANSIT |
| Số dư bình thường | **Net zero** tại POSTED |
| Lane mirror | — |
| Use case | EOD_CLEARING (lock phase) |
| ADR chính | ADR-010 |

#### Định nghĩa kinh tế & bản chất

TK 3800 là **bước đầu** của EOD settlement — lock merchant funds để tránh concurrent modification. `2120 DR 200.000 / 3800 CR 200.000`. Sau khi lock, EOD tách: MDR phần vào 3820, net phần vào 3810. Kết thúc: `3800 DR 200.000` (split MDR + net) = 3800 = 0. 3800 đảm bảo 2120 không bị dùng khi EOD đang chạy.

`[TBD: Policy partial-settlement: nếu một merchant fail, 3800 rollback hay giữ phần kia?]`

#### T-account

```
         TK 3800 — Transit Clearing lock
         (Transit — Net zero)
 ─────────────────────────────────────────
  DR (Split ra)        │  CR (Lock vào)
 ─────────────────────│───────────────────
  +2.000  → 3820       │  +200.000  Merchant lock
  +198.000 → 3810      │            (2120 DR E24)
                       │
  SD = 0              │  (POSTED)
```

#### Bảng DR/CR theo use case

| Bước | TK 3800 | DR | CR | Đối ứng | Nguồn |
|------|---------|----|----|---------|-------|
| Lock merchant | CR | — | 200.000 | 2120 DR | foundation §16 E24 |
| Split MDR | DR | 2.000 | — | 3820 CR | foundation §16 E25 |
| Split net | DR | 198.000 | — | 3810 CR | foundation §16 |

Net 3800: DR 200.000 = CR 200.000 → **3800 = 0**

#### Cặp đối ứng bắt buộc

| 3800 CR | Đối ứng | 3800 DR | Đối ứng |
|---------|---------|---------|---------|
| Lock | 2120 DR | MDR split | 3820 CR |
| — | — | Net split | 3810 CR |

#### Lỗi thường gặp

| Mã | Mô tả lỗi | Nguyên nhân | Xử lý |
|----|-----------|-------------|-------|
| 3800-E01 | 3800 ≠ 0 sau split | MDR + net ≠ lock amount | Verify 2k + 198k = 200k |
| 3800-E02 | 3800 còn khi EOD fail partial | Rollback không complete | Recovery: reverse 3800 → 2120 |
| 3800-E03 | 2120 DR âm sau lock | merchant không đủ fund | EOD validation trước lock |
| 3800-E04 | Gộp 3800/3810/3820 thành 1 TK | Mất visibility từng giai đoạn | Giữ tách biệt 3 TK |
| 3800-E05 | EOD inline với payment | EOD là independent batch | ADR: not inline |

#### ADR binding / Recon

[ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md): 3800 = 0. FR-10: không trực tiếp; monitoring EOD batch completion.

#### FAQ 3 câu — TK 3800

**Q1:** Tại sao cần 3800 trước khi tách 3810/3820?  
**A:** 3800 là "atomic lock" — đảm bảo 2120 không bị dùng trong khi EOD đang tách MDR. Tách 3810/3820 là các steps tiếp theo trong cùng batch.

**Q2:** MDR là gì và 1% từ đâu?  
**A:** MDR = Merchant Discount Rate — phí trừ trên doanh thu merchant. 1% là minh họa; rate thực `[TBD: rate table chưa chốt]`.

**Q3:** 3800 có PENDING state không?  
**A:** Trong batch EOD, các bước có thể có intermediate state. `[TBD: EOD transaction design.]` Monitoring alert nếu 3800 ≠ 0 sau EOD hoàn thành.

#### Gherkin pointer

```
Scenarios trong acceptance.md:
  → "EOD clearing lock — 3800 CR 200k"
  → "EOD split MDR/net — 3800=0"
```

---

### B.15 TK 3810 — Transit — Settlement outbound

#### Metadata

| Trường | Giá trị |
|--------|---------|
| Mã tài khoản | **3810** |
| Nhóm | 3 — Transit |
| Loại | TRANSIT |
| Số dư bình thường | **Net zero** tại POSTED |
| Lane mirror | — |
| Use case | EOD_SETTLEMENT (net outbound) |
| ADR chính | ADR-010 |

#### Định nghĩa kinh tế & bản chất

TK 3810 giữ **net settlement amount** (sau MDR) chờ gửi về ngân hàng merchant. Từ 3800 split: `3800 DR 198.000 / 3810 CR 198.000`. Sau đó: `3810 DR 198.000 / 1112 CR 198.000` (+ Napas EOD fee riêng). 3810 = 0 sau outbound thành công. Nếu bank fail → 3810 ≠ 0 → retry/escalate.

#### T-account

```
         TK 3810 — Transit Settlement outbound
         (Transit — Net zero)
 ─────────────────────────────────────────
  DR (Gửi ra)          │  CR (Net vào)
 ─────────────────────│───────────────────
  +198.000 → 1112      │  +198.000  From 3800 split
                       │
  SD = 0              │  (POSTED success)
  ≠ 0 nếu bank fail   │
```

#### Bảng DR/CR theo use case

| Bước | TK 3810 | DR | CR | Đối ứng | Nguồn |
|------|---------|----|----|---------|-------|
| Net từ 3800 | CR | — | 198.000 | 3800 DR | foundation §16 |
| Outbound Napas | DR | 198.000 | — | 1112 CR | foundation §16 |

#### Cặp đối ứng bắt buộc

| 3810 CR | Đối ứng | 3810 DR | Đối ứng |
|---------|---------|---------|---------|
| Net split từ 3800 | 3800 DR | Bank outbound | 1112 CR |

#### Lỗi thường gặp

| Mã | Mô tả lỗi | Nguyên nhân | Xử lý |
|----|-----------|-------------|-------|
| 3810-E01 | 3810 ≠ 0 sau EOD ngày | Bank timeout/reject | Retry; escalate; ADR-010 |
| 3810-E02 | 3810 CR ≠ (3800 lock − MDR) | Tính sai MDR | Verify 200k − 2k = 198k |
| 3810-E03 | 3810 DR → 1111 thay 1112 | EOD settlement qua Napas | Fix TK bank |
| 3810-E04 | Period close với 3810 ≠ 0 | EOD incomplete | ADR-023 block; xử lý trước close |

#### FAQ 3 câu — TK 3810

**Q1:** 3810 ≠ 0 qua ngày hôm sau — có vấn đề gì?  
**A:** Có — ADR-010 yêu cầu EOD transit = 0 sau batch success. Nếu bank fail → retry trong ngày; nếu không xong → ops escalate, period close block.

**Q2:** Napas EOD fee có ghi riêng không?  
**A:** Có: `5100 DR fee / 1112 CR fee` là cặp riêng khi gửi EOD batch qua Napas. `[TBD: Napas EOD fee structure.]`

**Q3:** 3810 và 3800 dùng có thể gộp một TK không?  
**A:** Không khuyến nghị — tách tường minh từng giai đoạn (lock → net → outbound) giúp debug EOD fail dễ hơn.

#### Gherkin pointer

```
Scenarios trong acceptance.md:
  → "EOD settlement outbound — 3810=0"  (3810 DR 198k; 1112 CR 198k)
  → "EOD fail partial — 3810 non-zero"
```

---

### B.16 TK 3820 — Transit — MDR holdback

#### Metadata

| Trường | Giá trị |
|--------|---------|
| Mã tài khoản | **3820** |
| Nhóm | 3 — Transit |
| Loại | TRANSIT |
| Số dư bình thường | **Net zero** tại POSTED |
| Lane mirror | — |
| Use case | EOD_MDR |
| ADR chính | ADR-010 |

#### Định nghĩa kinh tế & bản chất

TK 3820 giữ **MDR (Merchant Discount Rate)** — phần doanh thu platform từ merchant. Từ 3800 split: `3800 DR 2.000 / 3820 CR 2.000`. Sau đó: `3820 DR 2.000 / 4140 CR 2.000` (MDR revenue). 3820 = 0 sau MDR ghi nhận. Tách 3820 ra khỏi 3810 giúp visibility MDR riêng với net settlement.

#### T-account

```
         TK 3820 — Transit MDR holdback
         (Transit — Net zero)
 ─────────────────────────────────────────
  DR (Vào doanh thu)   │  CR (Hold MDR)
 ─────────────────────│───────────────────
  +2.000 → 4140        │  +2.000  From 3800 (E25)
                       │
  SD = 0              │  (MDR recognized)
```

#### Bảng DR/CR theo use case

| Bước | TK 3820 | DR | CR | Đối ứng | Nguồn |
|------|---------|----|----|---------|-------|
| MDR split từ 3800 | CR | — | 2.000 | 3800 DR | foundation §16 E25 |
| MDR revenue ghi nhận | DR | 2.000 | — | 4140 CR | foundation §16 |

#### Cặp đối ứng bắt buộc

| 3820 CR | Đối ứng | 3820 DR | Đối ứng |
|---------|---------|---------|---------|
| MDR split | 3800 DR | MDR revenue | 4140 CR |

#### Lỗi thường gặp

| Mã | Mô tả lỗi | Nguyên nhân | Xử lý |
|----|-----------|-------------|-------|
| 3820-E01 | 3820 ≠ 0 sau EOD | Thiếu dòng 4140 | Thêm MDR → 4140 |
| 3820-E02 | MDR ghi 4130 thay 4140 | Nhầm revenue MDR/transfer | EOD MDR = 4140 |
| 3820-E03 | 3820 CR 0 (bỏ qua MDR) | Merchant không bị charge | `[TBD: zero-MDR case]` |
| 3820-E04 | 3820 không split từ 3800, ghi thẳng | Bypass 3800 lock | Phải qua 3800 trước |

#### FAQ 3 câu — TK 3820

**Q1:** Tại sao MDR không ghi thẳng từ 2120 vào 4140?  
**A:** EOD atomic: 3800 lock toàn bộ 2120 trước, rồi tách rõ MDR (3820) và net (3810). Nếu ghi thẳng, partial failure khó trace và atomic lock không rõ.

**Q2:** MDR 1% trên 200.000 = 2.000 — nếu MDR rate thay đổi thì sao?  
**A:** Template orchestration tính MDR rate từ config. `[TBD: MDR rate table per merchant chưa chốt.]`

**Q3:** 3820 xuất hiện trong EOD journal — mỗi merchant có 1 dòng 3820 không?  
**A:** `[TBD: EOD per-merchant journal design.]` Có thể batch hoặc per-merchant. Cả hai case 3820 = 0 là bắt buộc.

#### Gherkin pointer

```
Scenarios trong acceptance.md:
  → "EOD MDR holdback — 3820=0"  (3820 DR 2k; 4140 CR 2k)
  → "EOD happy path — all transit zero"
```


---

### B.17 TK 4110 — Doanh thu phí nạp tiền

#### Metadata

| Trường | Giá trị |
|--------|---------|
| Mã tài khoản | **4110** |
| Nhóm | 4 — Doanh thu |
| Loại | REVENUE |
| Số dư bình thường | **Có (Credit)** |
| Lane mirror | — |
| Use case | DEPOSIT |
| ADR chính | ADR-006, ADR-009 |

#### Định nghĩa kinh tế & bản chất

TK 4110 ghi nhận **doanh thu phí nạp tiền** — 1.000 VND trên mỗi deposit gross 100.000. Phí chỉ ghi nhận tại **phase B POSTED** — không ghi ở phase A PENDING (vì deposit có thể FAILED). Đây là accrual-like recognition: revenue ngay khi confirm POSTED. Orchestration tính phí 1.000 và truyền vào `confirmDeposit(coaTransId, fee=1000)` — accounting không tự tính.

Nếu deposit zero-fee: không có dòng 4110 trong journal. `[TBD: Free tier / zero-fee deposit policy.]`

#### T-account

```
         TK 4110 — Doanh thu phí nạp
         (Doanh thu — Có)
 ─────────────────────────────────────────
  DR (Đảo/reversal) │  CR (Ghi nhận)
 ─────────────────│───────────────────────
  (reversal only)  │  +1.000  Phase B POSTED (E03)
                   │  +1.000  Mỗi deposit có phí
                   │
  SD Có: dương    │  Tích lũy phí nạp kỳ
```

#### Bảng DR/CR theo use case

| use_case | Phase | TK 4110 | DR | CR | Đối ứng | Nguồn |
|----------|-------|---------|----|----|---------|-------|
| DEPOSIT | Phase B POSTED | CR | — | 1.000 | 2110 DR 1.000 | foundation §8 E03 step 6 |
| DEPOSIT | Zero fee | — | — | — | Không có dòng | — |
| DEPOSIT | Reversal | DR | 1.000 | — | 2110 CR 1.000 | ADR-001 reversal |

Kết quả phase B: `3100 DR 100.000 / 2110 CR 99.000 / 2110 DR 1.000 / 4110 CR 1.000`.  
Kiểm tra: DR = 101.000; CR = 99.000 + 1.000 + 1.000 = 101.000 ✓

#### Cặp đối ứng bắt buộc

| 4110 CR | Đối ứng DR |
|---------|-----------|
| Phase B phí nạp | 2110 DR 1.000 (fee gross-up from user) |

#### Trạng thái PENDING / POSTED

| Trạng thái | 4110 |
|------------|------|
| PENDING (phase A) | Không ghi 4110 |
| POSTED (phase B) | CR +1.000 |
| FAILED | Không ghi 4110 (chưa reach phase B) |

#### Lỗi thường gặp

| Mã | Mô tả lỗi | Nguyên nhân | Xử lý |
|----|-----------|-------------|-------|
| 4110-E01 | 4110 ghi ở phase A | Revenue ghi sớm | Vi phạm ADR-006; chỉ phase B |
| 4110-E02 | 4110 CR không bằng fee param | Orchestration truyền sai fee | Validate fee từ config |
| 4110-E03 | Nhầm 4110 với 4130 (transfer fee) | Deposit fee = 4110 | Fix template |
| 4110-E04 | 4110 CR = gross 100.000 thay 1.000 | Nhầm gross với fee | Fee = 1.000, gross ≠ revenue |
| 4110-E05 | 4110 trong WITHDRAW journal | Withdraw fee = 4120 | Sai template |
| 4110-E06 | 4110 DR 1.000 nhưng 2110 không DR tương ứng | Mất gross-up | DR ≠ CR fail |

#### Recon FR-10 / W5

**Kỳ kế toán:** SUM(4110 CR POSTED) trong kỳ = tổng phí nạp kỳ. Báo cáo doanh thu theo kỳ từ trial balance. FR-10 không liên quan 4110 trực tiếp.

#### ADR binding

| ADR | Quyết định |
|-----|-----------|
| [ADR-006](../../adr/ADR-006-two-phase-deposit.md) | 4110 chỉ phase B |
| [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md) | Fee tính bởi orchestration, accounting nhận |

#### FAQ 3 câu — TK 4110

**Q1:** Tại sao 2110 DR 1.000 (fee gross-up) rồi lại 4110 CR 1.000 — không phải ghi thẳng 4110 CR?  
**A:** Presentation: user "trả" phí 1.000 (DR 2110), platform thu (CR 4110). Nếu gộp, 2110 CR net 99.000 — ít transparent. Foundation §8 tách bước 5–6 để rõ ràng.

**Q2:** 4110 vs 4130 — cả hai đều là doanh thu phí. Phân biệt bằng gì?  
**A:** 4110 = phí DEPOSIT. 4130 = phí INTERNAL_TRANSFER và IBFT. Tách để báo cáo doanh thu theo loại giao dịch.

**Q3:** Khi đảo deposit POSTED (có 4110), dòng 4110 xử lý sao?  
**A:** Reversal journal tạo 4110 DR 1.000 (dòng âm ngược). Không sửa dòng gốc — bất biến ([ADR-001](../../adr/ADR-001-immutable-ledger.md)).

#### Gherkin pointer

```
Scenarios trong acceptance.md:
  → "Deposit Phase B — 4110 CR 1.000"
  → "Deposit zero fee — no 4110 line"
  → "Deposit reversal — 4110 DR 1.000"
```

---

### B.18 TK 4120 — Doanh thu phí rút tiền

#### Metadata

| Trường | Giá trị |
|--------|---------|
| Mã tài khoản | **4120** |
| Nhóm | 4 — Doanh thu |
| Loại | REVENUE |
| Số dư bình thường | **Có (Credit)** |
| Lane mirror | — |
| Use case | WITHDRAW |
| ADR chính | ADR-010, ADR-009 |

#### Định nghĩa kinh tế & bản chất

TK 4120 ghi nhận **doanh thu phí rút tiền** — 1.000 VND phí rút trên gross 101.000 (100.000 principal + 1.000 fee). Ghi nhận tức thì cùng journal POSTED vì withdraw không có 2-phase như deposit. Journal 5 dòng: `2110 DR 101.000 / 3200 CR 101.000 / 3200 DR 100.000 / 1111 CR 100.000 / 3200 DR 1.000 / 4120 CR 1.000`.

Orchestration tính fee và include vào gross debit user. Accounting nhận số đã tính.

#### T-account

```
         TK 4120 — Doanh thu phí rút
         (Doanh thu — Có)
 ─────────────────────────────────────────
  DR (reversal)    │  CR (Ghi nhận)
 ─────────────────│───────────────────────
  (chỉ khi đảo)   │  +1.000  Withdraw POSTED (E08)
                   │
  SD Có: dương    │  Tích lũy phí rút kỳ
```

#### Bảng DR/CR theo use case

| Bước | TK 4120 | DR | CR | Đối ứng | Nguồn |
|------|---------|----|----|---------|-------|
| Withdraw fee | CR | — | 1.000 | 3200 DR | foundation §9 E08 |
| Reversal | DR | 1.000 | — | 3200 CR | ADR-001 |

#### Cặp đối ứng bắt buộc

| 4120 CR | Đối ứng DR |
|---------|-----------|
| Withdraw fee | 3200 DR 1.000 |

#### Lỗi thường gặp

| Mã | Mô tả lỗi | Nguyên nhân | Xử lý |
|----|-----------|-------------|-------|
| 4120-E01 | Nhầm 4120 với 4110 (deposit fee) | Withdraw fee = 4120 | Fix template |
| 4120-E02 | 4120 CR = 101.000 (gross) | Fee ≠ gross | CR 1.000, không 101.000 |
| 4120-E03 | 4120 trong IBFT journal | IBFT fee = 4130 | Sai template |
| 4120-E04 | Thiếu dòng 4120 | DR ≠ CR fail | Validate |
| 4120-E05 | 4120 xuất hiện khi zero-fee withdraw | `[TBD: zero-fee withdraw]` | Không ghi 4120 nếu fee = 0 |

#### Recon / ADR / FAQ

**Recon:** SUM(4120 CR POSTED) kỳ = tổng doanh thu phí rút. [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md): fee tính bởi orchestration.

**Q1:** 4120 ghi cùng 3200 → 1111 — có thể fail 4120 nhưng 1111 CR thành công không?  
**A:** Không — tất cả trong 1 `coa_trans` transaction. Rollback toàn bộ nếu fail.

**Q2:** Nếu bank trả tiền nhưng fail ghi 4120, xử lý sao?  
**A:** Rollback toàn bộ journal; retry. Nếu bank đã chuyển, cần saga compensation.

**Q3:** Doanh thu phí rút có phân tách theo user tier không?  
**A:** Không trên ledger — 4120 tổng hợp. Phân tích tier thực hiện trên tầng reporting, filter theo orchestration metadata. `[TBD: reporting spec.]`

#### Gherkin pointer

```
Scenarios trong acceptance.md:
  → "Withdraw — 4120 CR 1.000 POSTED"
```

---

### B.19 TK 4130 — Doanh thu phí chuyển khoản

#### Metadata

| Trường | Giá trị |
|--------|---------|
| Mã tài khoản | **4130** |
| Nhóm | 4 — Doanh thu |
| Loại | REVENUE |
| Số dư bình thường | **Có (Credit)** |
| Lane mirror | — |
| Use case | INTERNAL_TRANSFER, IBFT |
| ADR chính | ADR-009, ADR-025 |

#### Định nghĩa kinh tế & bản chất

TK 4130 ghi nhận doanh thu phí cho **cả hai** use case: internal transfer và IBFT. Không tách 4130 per use case — phân tích theo `coa_trans.use_case` filter trên báo cáo. Fee 1.000 mỗi trường hợp.

- **Internal transfer:** `3300 DR 1.000 / 4130 CR 1.000` (E11)
- **IBFT:** `3400 DR 1.000 / 4130 CR 1.000` (E15) — đồng thời có `5100 DR 500 / 1112 CR 500` Napas cost

Net margin IBFT từ 4130: 1.000 − 5100 500 = **+500** ([Quyển I §3.6](vol-01-principles.md#36-ví-dụ-e16e17--ibft-lãi-gộp-500)).

#### T-account

```
         TK 4130 — Doanh thu phí chuyển khoản
         (Doanh thu — Có)
 ─────────────────────────────────────────
  DR (reversal)    │  CR (Ghi nhận)
 ─────────────────│───────────────────────
  (chỉ đảo)       │  +1.000  Internal transfer (E11)
                   │  +1.000  IBFT (E15)
                   │
  SD Có: dương    │  Tổng phí transfer + IBFT
```

#### Bảng DR/CR theo use case

| use_case | Bước | TK 4130 | DR | CR | Đối ứng | Nguồn |
|----------|------|---------|----|----|---------|-------|
| INTERNAL_TRANSFER | Fee | CR | — | 1.000 | 3300 DR | foundation §10 E11 |
| IBFT | Fee | CR | — | 1.000 | 3400 DR | foundation §11 E15 |
| (reversal) | | DR | 1.000 | — | 3300/3400 CR | ADR-001 |

#### Cặp đối ứng bắt buộc

| 4130 CR | Đối ứng DR |
|---------|-----------|
| Internal transfer fee | 3300 DR 1.000 |
| IBFT fee | 3400 DR 1.000 |

#### Lỗi thường gặp

| Mã | Mô tả lỗi | Nguyên nhân | Xử lý |
|----|-----------|-------------|-------|
| 4130-E01 | IBFT dùng 4120 (rút) thay 4130 | Nhầm TK revenue | IBFT/transfer fee = 4130 |
| 4130-E02 | IBFT dùng 4110 (nạp) thay 4130 | Nhầm nghiêm trọng | Fix template |
| 4130-E03 | Thiếu dòng 4130 trong transfer | DR ≠ CR | Validate |
| 4130-E04 | 4130 CR = 101.000 (gross) | Nhầm fee với gross | Fee = 1.000 |
| 4130-E05 | 4130 không xuất hiện trong IBFT | Quên fee line | IBFT có fee 1.000 → 4130 |

#### Recon / ADR / FAQ

**Recon:** SUM(4130 POSTED) grouped by `use_case` = phí transfer vs IBFT. [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md).

**Q1:** Có khi nào 4130 = 0 (zero-fee transfer)?  
**A:** Có — nếu orchestration truyền fee=0, không ghi dòng 4130. Journal transfer vẫn valid với 4 dòng 2110/3300.

**Q2:** Margin IBFT = 4130 CR − 5100 DR = 500 — đây là lợi nhuận thực sự không?  
**A:** Đây là gross margin per transaction trên ledger. P&L thực sự còn include overhead, OPEX. Dùng làm KPI operational metric.

**Q3:** Phí IBFT và phí internal transfer có thể khác nhau không?  
**A:** Có thể — `[TBD: fee rule per use_case.]` Accounting ghi số nhận được; tách qua `use_case` filter trên báo cáo.

#### Gherkin pointer

```
Scenarios trong acceptance.md:
  → "Internal transfer — 4130 CR 1.000"
  → "IBFT — 4130 CR 1.000 + 5100 DR 500"
```

---

### B.20 TK 4140 — Doanh thu MDR merchant

#### Metadata

| Trường | Giá trị |
|--------|---------|
| Mã tài khoản | **4140** |
| Nhóm | 4 — Doanh thu |
| Loại | REVENUE |
| Số dư bình thường | **Có (Credit)** |
| Lane mirror | — |
| Use case | EOD_MDR |
| ADR chính | ADR-010 |

#### Định nghĩa kinh tế & bản chất

TK 4140 ghi nhận **MDR (Merchant Discount Rate)** — phần trăm doanh thu merchant mà platform thu trong EOD settlement. Ghi nhận qua `3820 DR / 4140 CR` khi EOD transit MDR về revenue. MDR không ghi trong từng payment — chỉ batch EOD. Ví dụ chuẩn: MDR 1% × 200.000 = 2.000.

`[TBD: MDR rate table per merchant tier chưa chốt.]`

#### T-account

```
         TK 4140 — Doanh thu MDR
         (Doanh thu — Có)
 ─────────────────────────────────────────
  DR (reversal)    │  CR (MDR recognized)
 ─────────────────│───────────────────────
  (chỉ đảo)       │  +2.000  EOD MDR (E25)
                   │
  SD Có: dương    │  MDR theo kỳ
```

#### Bảng DR/CR theo use case

| use_case | Bước | TK 4140 | DR | CR | Đối ứng | Nguồn |
|----------|------|---------|----|----|---------|-------|
| EOD_MDR | MDR recognized | CR | — | 2.000 | 3820 DR | foundation §16 E25 |
| (reversal) | | DR | 2.000 | — | 3820 CR | ADR-001 |

#### Cặp đối ứng bắt buộc

| 4140 CR | Đối ứng DR |
|---------|-----------|
| MDR recognized | 3820 DR |

#### Lỗi thường gặp

| Mã | Mô tả lỗi | Nguyên nhân | Xử lý |
|----|-----------|-------------|-------|
| 4140-E01 | MDR ghi 4130 thay 4140 | MDR ≠ transfer fee | Fix template EOD |
| 4140-E02 | 4140 ghi trong từng payment (không EOD) | MDR là EOD batch only | Sai design |
| 4140-E03 | 4140 CR = gross 200.000 | CR = MDR amount (2.000), không gross | Fix calculation |
| 4140-E04 | 3820 ≠ 0 sau ghi 4140 | Thiếu dòng hoặc amount sai | Validate 3820=0 |
| 4140-E05 | Không có 4140 trong EOD journal | MDR không ghi nhận | Confirm MDR policy `[TBD]` |

#### Recon / FAQ

**Recon:** SUM(4140 CR POSTED) = tổng MDR kỳ. Báo cáo merchant doanh số vs MDR.

**Q1:** MDR ghi nhận ở thời điểm EOD hay theo từng payment?  
**A:** EOD batch — đây là "deferred revenue recognition" qua transit 3820. Tại thời điểm payment, merchant 2120 CR full; MDR lấy ra vào EOD.

**Q2:** Nếu merchant không có MDR (0% rate), EOD không có 3820/4140?  
**A:** `[TBD: zero-MDR merchant.]` Nếu 0% MDR: 3800 split hết vào 3810; không có 3820/4140 trong journal đó.

**Q3:** 4140 có liên quan đến 5100 (bank cost) không?  
**A:** Không trực tiếp — 4140 là MDR revenue; 5100 là bank cost riêng. EOD P&L: 4140 − 5100 = net margin EOD.

#### Gherkin pointer

```
Scenarios trong acceptance.md:
  → "EOD MDR — 4140 CR 2.000; 3820=0"
```

---

### B.21 TK 4150 — Doanh thu phí payroll/disbursement

#### Metadata

| Trường | Giá trị |
|--------|---------|
| Mã tài khoản | **4150** |
| Nhóm | 4 — Doanh thu |
| Loại | REVENUE |
| Số dư bình thường | **Có (Credit)** |
| Lane mirror | — |
| Use case | PAYROLL, DISBURSEMENT |
| ADR chính | ADR-009, ADR-010 |

#### Định nghĩa kinh tế & bản chất

TK 4150 ghi nhận phí cho **cả** payroll (merchant trả lương) và disbursement (partner giải ngân). Fee 5.000 cho payroll batch 5 người (1.000/người), fee 1.000 cho disbursement đơn. Tương tự 4130 (hai use case chung TK) — phân tách qua `use_case` filter.

#### T-account

```
         TK 4150 — Doanh thu phí payroll/disbursement
         (Doanh thu — Có)
 ─────────────────────────────────────────
  DR (reversal)    │  CR (Ghi nhận)
 ─────────────────│───────────────────────
  (chỉ đảo)       │  +5.000  Payroll batch 5×1k
                   │  +1.000  Disbursement đơn
                   │
  SD Có: dương    │
```

#### Bảng DR/CR theo use case

| use_case | Bước | TK 4150 | DR | CR | Đối ứng | Nguồn |
|----------|------|---------|----|----|---------|-------|
| PAYROLL | Fee batch | CR | — | 5.000 | 3600 DR | foundation §14 |
| DISBURSEMENT | Fee đơn | CR | — | 1.000 | 3700 DR | foundation §15 |

#### Cặp đối ứng bắt buộc

| 4150 CR | Đối ứng DR |
|---------|-----------|
| Payroll batch fee | 3600 DR |
| Disbursement fee | 3700 DR |

#### Lỗi thường gặp

| Mã | Mô tả lỗi | Nguyên nhân | Xử lý |
|----|-----------|-------------|-------|
| 4150-E01 | 4150 dùng 4130 (transfer) | Nhầm revenue TK | Payroll/disburse = 4150 |
| 4150-E02 | 4150 CR = 5 thay 5.000 | Sai đơn vị (đồng vs nghìn) | Scale 4: 5000.0000 |
| 4150-E03 | Payroll zero-fee, 4150 CR xuất hiện | Không có fee → không ghi | Validate fee=0 logic |
| 4150-E04 | 3600 và 3700 không bằng 4150 tương ứng | Amount mismatch | Validate per use_case |

#### FAQ 3 câu

**Q1:** 4150 dùng cho cả payroll và disbursement — báo cáo tách được không?  
**A:** Được — filter `coa_trans.use_case IN ('PAYROLL','DISBURSEMENT')` từ `coa_trans` JOIN `coa_trans_data WHERE account_code='4150'`.

**Q2:** Payroll 10 employees fee = 10.000 — 4150 CR 10.000?  
**A:** Đúng nếu fee = 1.000/người × 10. Template tính động theo N.

**Q3:** Disbursement nhiều lần trong ngày — 4150 là tổng hay per-journal?  
**A:** Per-journal: mỗi DISBURSEMENT POSTED tạo một dòng 4150 CR riêng. Trial balance SUM tất cả.

#### Gherkin pointer

```
Scenarios trong acceptance.md:
  → "Payroll — 4150 CR 5.000"
  → "Disbursement — 4150 CR 1.000"
```

---

### B.22 TK 5100 — Chi phí bank/Napas

#### Metadata

| Trường | Giá trị |
|--------|---------|
| Mã tài khoản | **5100** |
| Nhóm | 5 — Chi phí |
| Loại | EXPENSE |
| Số dư bình thường | **Nợ (Debit)** |
| Lane mirror | — |
| Use case | IBFT, QR_PAYMENT, PAYROLL, DISBURSEMENT, EOD (bất kỳ có bank cost) |
| ADR chính | ADR-025, ADR-010 |

#### Định nghĩa kinh tế & bản chất

TK 5100 ghi nhận **chi phí thực trả** cho Napas/bank mỗi khi platform dùng external rail. 500 VND mỗi giao dịch Napas (IBFT) hoặc acquirer fee (QR/POS). Không xuất hiện trong DEPOSIT, WITHDRAW (Vietinbank VA không có Napas cost), INTERNAL_TRANSFER, WALLET_PAYMENT.

**Matching principle (Quyển I Ch11):** 5100 ghi nhận cùng journal với revenue tương ứng — IBFT: 4130 CR 1.000 và 5100 DR 500 trong cùng `coa_trans`. Gross margin = 500.

#### T-account

```
         TK 5100 — Chi phí bank/Napas
         (Chi phí — Nợ)
 ─────────────────────────────────────────
  DR (Ghi nhận)    │  CR (reversal)
 ─────────────────│───────────────────────
  +500  IBFT (E17) │  (chỉ khi đảo)
  +500  QR cost    │
  +2.500 Payroll   │
  (5×500)          │
                   │
  SD Nợ: dương    │  Tích lũy chi phí Napas
```

#### Bảng DR/CR theo use case

| use_case | Dòng | TK 5100 | DR | CR | Đối ứng | Nguồn |
|----------|------|---------|----|----|---------|-------|
| IBFT | Napas cost | DR | 500 | — | 1112 CR | foundation §11 E17 |
| QR_PAYMENT | Acquirer cost | DR | 500 | — | 1113 CR | foundation §12 E19 |
| PAYROLL | Napas batch cost | DR | 2.500 | — | 1112 CR | foundation §14 (5×500) |
| DISBURSEMENT | Napas cost | DR | 500 | — | 1112 CR | foundation §15 |
| EOD | Settlement Napas | DR | ? | — | 1112 CR | `[TBD: EOD Napas fee]` |
| (reversal) | | CR | — | 500 | 1112 DR | ADR-001 |

> **Không xuất hiện:** DEPOSIT, WITHDRAW, INTERNAL_TRANSFER, WALLET_PAYMENT.

#### Cặp đối ứng bắt buộc

| 5100 DR | Đối ứng CR |
|---------|-----------|
| IBFT Napas cost | 1112 CR 500 |
| QR acquirer cost | 1113 CR 500 |
| Payroll Napas cost | 1112 CR (N×500) |
| Disbursement Napas | 1112 CR 500 |

#### Trạng thái PENDING / POSTED

5100 chỉ ghi khi POSTED; cùng journal với revenue. Không staging riêng.

#### Lỗi thường gặp

| Mã | Mô tả lỗi | Nguyên nhân | Xử lý |
|----|-----------|-------------|-------|
| 5100-E01 | 5100 trong DEPOSIT journal | Deposit qua VA — không có Napas cost | Xóa dòng |
| 5100-E02 | 5100 trong INTERNAL_TRANSFER | Không external rail | Xóa |
| 5100-E03 | 5100 DR 500 nhưng 1111 CR thay 1112 | Sai TK bank asset | IBFT cost từ 1112 |
| 5100-E04 | 5100 trong QR nhưng đối ứng 1112 thay 1113 | QR cost từ VPBank (1113) | Fix đối ứng |
| 5100-E05 | Payroll: 5100 DR = 500 (chỉ 1 txn) thay 2.500 | Nhân per-employee | N × 500 |
| 5100-E06 | 5100 không ghi nhận khi có Napas cost | Matching không đủ | Thêm cost line |

#### Recon FR-10 / W5

**Chi phí kỳ:** SUM(5100 DR POSTED) kỳ = tổng chi phí Napas. Báo cáo P&L: 411x − 5100. FR-10: 5100 map vào phần bank fee trên sao kê 1112/1113.

#### ADR binding

| ADR | Quyết định |
|-----|-----------|
| [ADR-025](../../adr/ADR-025-ibft-napas-clearing-1112.md) | 5100 IBFT → 1112 CR |
| [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md) | Cost amount do orchestration truyền |

#### FAQ 3 câu — TK 5100

**Q1:** 5100 là chi phí của ai — platform hay user?  
**A:** Platform — platform trả phí Napas để thực hiện IBFT/payroll. User trả phí service 4130 (revenue); platform dùng phần đó bù chi phí 5100.

**Q2:** Nếu Napas tăng phí từ 500 lên 800, cần thay đổi gì trong accounting?  
**A:** Chỉ cần orchestration truyền `napas_cost=800` vào journal builder. COA không thay đổi. Ledger tự ghi đúng số.

**Q3:** QR cost 500 ghi 5100 DR / 1113 CR — vậy 1113 giảm 500 ngay khi giao dịch?  
**A:** Đúng — 1113 net giảm 500 (acquirer fee trừ ngay). 1113 DR +100.000 (acquirer in) − 500 (cost) = 1113 net +99.500 per giao dịch.

#### Gherkin pointer

```
Scenarios trong acceptance.md:
  → "IBFT — 5100 DR 500 present"         (IBFT-E01)
  → "QR — 5100 DR 500 acquirer cost"
  → "Payroll — 5100 DR 2.500 batch cost"
  → "Deposit — 5100 NOT present"
```

---

### B.23 TK 6000 — Vốn chủ sở hữu

#### Metadata

| Trường | Giá trị |
|--------|---------|
| Mã tài khoản | **6000** |
| Nhóm | 6 — Vốn chủ |
| Loại | EQUITY |
| Số dư bình thường | **Có (Credit)** |
| Lane mirror | — |
| Use case | INIT_CAPITAL (one-time trước go-live) |
| ADR chính | — |

#### Định nghĩa kinh tế & bản chất

TK 6000 đại diện cho **vốn chủ sở hữu** ban đầu — số tiền góp vốn vào bank accounts trước khi platform hoạt động. Chỉ có 3 journal khởi tạo (foundation §7): `1111 DR 1 tỷ / 6000 CR 1 tỷ`, `1113 DR 500 triệu / 6000 CR 500 triệu`, `1112 DR 500 triệu / 6000 CR 500 triệu`. Sau go-live, 6000 **không thay đổi** trong runtime hàng ngày — doanh thu/chi phí tích lũy vào retained earnings (tầng BCTC ngoài scope v1).

#### T-account

```
         TK 6000 — Vốn chủ sở hữu
         (Equity — Có)
 ─────────────────────────────────────────
  DR (giảm vốn)    │  CR (Ghi nhận vốn)
 ─────────────────│───────────────────────
  (không có DR     │  +1.000.000.000  → 1111 (E01)
   trong runtime)  │  +500.000.000   → 1113
                   │  +500.000.000   → 1112
                   │
  SD Có: 2 tỷ    │  (fixed sau init)
```

#### Bảng DR/CR theo use case

| use_case | Dòng | TK 6000 | DR | CR | Đối ứng | Nguồn |
|----------|------|---------|----|----|---------|-------|
| INIT_CAPITAL | Vốn vào Vietinbank | CR | — | 1.000.000.000 | 1111 DR | foundation §7 E01 |
| INIT_CAPITAL | Vốn vào VPBank | CR | — | 500.000.000 | 1113 DR | foundation §7 |
| INIT_CAPITAL | Fund Napas | CR | — | 500.000.000 | 1112 DR | foundation §7 |

> **Không xuất hiện** trong bất kỳ runtime use case nào (DEPOSIT, WITHDRAW, IBFT...).

#### Cặp đối ứng bắt buộc

| 6000 CR | Đối ứng DR |
|---------|-----------|
| Vốn vào Vietinbank | 1111 DR |
| Vốn vào VPBank | 1113 DR |
| Vốn vào Napas | 1112 DR |

#### Trạng thái PENDING / POSTED

Init capital journal: POSTED one-time. Không có lifecycle PENDING.

#### Lỗi thường gặp

| Mã | Mô tả lỗi | Nguyên nhân | Xử lý |
|----|-----------|-------------|-------|
| 6000-E01 | 6000 xuất hiện trong journal runtime | Nhầm init với runtime | 6000 chỉ INIT_CAPITAL |
| 6000-E02 | 6000 DR trong lúc vận hành | Vốn không giảm runtime | Chỉ init; bất thường → review |
| 6000-E03 | Thiếu 3 journal init trước go-live | Quên fund | Seed data kiểm tra DDL.2 |
| 6000-E04 | 6000 CR cộng vào W5 kiểm tra | 6000 không tham gia W5 | Chỉ 2110+2120+2130 vs wallet |
| 6000-E05 | Dùng 6000 để "fix" trial balance mất cân | Không được dùng runtime | Tìm nguyên nhân mất cân |

#### Recon FR-10 / W5

6000 không tham gia W5 (liabilities vs wallet). Tham gia bảng cân đối kế toán BCTC (tầng bên ngoài scope). Trial balance: 6000 CR fixed = 2.000.000.000 sau init.

#### ADR binding

Không có ADR chuyên biệt cho 6000 — chỉ liên quan foundation §7 init sequence.

#### FAQ 3 câu — TK 6000

**Q1:** Lợi nhuận tích lũy (4110–4150 minus 5100) có vào 6000 không?  
**A:** Không — v1 ledger không tính retained earnings vào 6000. Báo cáo P&L tích lũy qua revenue/expense TK; BCTC công ty tổng hợp ở tầng khác.

**Q2:** 6000 chỉ có 3 journal init — nếu cần top-up vốn, làm thế nào?  
**A:** Tạo thêm INIT_CAPITAL journal với DR TK bank + CR 6000. Cần approval ops. `[TBD: capital top-up process.]`

**Q3:** Sau go-live, 6000 luôn = 2.000.000.000 — có đúng không?  
**A:** Đúng trên ledger v1 — nếu không có capital change journal. 6000 không biến động theo revenue/expense.

#### Gherkin pointer

```
Scenarios trong acceptance.md:
  → "System init — 3 capital journals POSTED"  (1111+1113+1112 DR; 6000 CR 2B)
  → "Runtime — 6000 NOT in any journal"
```


---

## Phụ lục A. Ma trận TK × use_case mở rộng

Bảng dưới liệt kê **tất cả** tài khoản xuất hiện trong từng use_case. `DR` = ghi Nợ; `CR` = ghi Có; `—` = không xuất hiện; `*` = riêng (cặp với 1112/1113, không qua transit chính).

| TK | DEPOSIT | WITHDRAW | INT_TRANSFER | IBFT | WALLET_PAY | QR_PAY | PAYROLL | DISBURSEMENT | EOD |
|----|---------|----------|-------------|------|-----------|--------|---------|-------------|-----|
| **1111** | DR(A) | CR | — | — | — | — | — | DR(pre) | — |
| **1112** | — | — | — | CR | — | — | CR | CR | CR |
| **1113** | — | — | — | — | — | DR | — | — | — |
| **2110** | CR(B) | DR | DR(A)/CR(B) | DR | DR | — | — | — | — |
| **2120** | — | — | — | — | CR | CR | DR | — | DR |
| **2130** | — | — | — | — | — | — | — | CR/DR | — |
| **3100** | CR(A)/DR(B) | — | — | — | — | — | — | — | — |
| **3200** | — | CR/DR | — | — | — | — | — | — | — |
| **3300** | — | — | CR/DR | — | — | — | — | — | — |
| **3400** | — | — | — | CR/DR | — | — | — | — | — |
| **3500** | — | — | — | — | CR/DR | CR/DR | — | — | — |
| **3600** | — | — | — | — | — | — | CR/DR | — | — |
| **3700** | — | — | — | — | — | — | — | CR/DR | — |
| **3800** | — | — | — | — | — | — | — | — | CR/DR |
| **3810** | — | — | — | — | — | — | — | — | CR/DR |
| **3820** | — | — | — | — | — | — | — | — | CR/DR |
| **4110** | CR(B) | — | — | — | — | — | — | — | — |
| **4120** | — | CR | — | — | — | — | — | — | — |
| **4130** | — | — | CR | CR | — | — | — | — | — |
| **4140** | — | — | — | — | — | — | — | — | CR |
| **4150** | — | — | — | — | — | — | CR | CR | — |
| **5100** | — | — | — | DR* | DR* | DR* | DR* | DR* | DR* |
| **6000** | — | — | — | — | — | — | — | — | — |

> **Ghi chú:** 5100 `*` là cặp riêng `5100 DR / 1112 CR` (hoặc `1113 CR` cho QR), không nằm trong transit chính.  
> 6000 chỉ xuất hiện trong `INIT_CAPITAL` (one-time, không trong bảng runtime).

### Phụ lục A.2 — Kiểm tra nhanh "TK này dùng use case nào?"

| TK | Use cases (liệt kê) |
|----|---------------------|
| 1111 | DEPOSIT (A phase), WITHDRAW, DISBURSEMENT pre-fund |
| 1112 | IBFT, PAYROLL, DISBURSEMENT disburse, EOD settlement |
| 1113 | QR_PAYMENT acquirer |
| 2110 | DEPOSIT, WITHDRAW, INT_TRANSFER, IBFT, WALLET_PAYMENT |
| 2120 | WALLET_PAY receive, QR_PAY receive, PAYROLL debit, EOD lock |
| 2130 | DISBURSEMENT escrow |
| 3100 | DEPOSIT only |
| 3200 | WITHDRAW only |
| 3300 | INT_TRANSFER only |
| 3400 | IBFT only |
| 3500 | WALLET_PAY + QR_PAY |
| 3600 | PAYROLL only |
| 3700 | DISBURSEMENT only |
| 3800 | EOD lock |
| 3810 | EOD net outbound |
| 3820 | EOD MDR |
| 4110 | DEPOSIT fee |
| 4120 | WITHDRAW fee |
| 4130 | INT_TRANSFER + IBFT fee |
| 4140 | EOD MDR revenue |
| 4150 | PAYROLL + DISBURSEMENT fee |
| 5100 | IBFT, QR, PAYROLL, DISBURSEMENT, EOD (bank cost) |
| 6000 | INIT_CAPITAL only |

---

## Phụ lục B. Quick reference card vận hành

### B.1 Cheat sheet DR/CR nhanh

```
DEPOSIT Phase A  : 1111 DR 100k / 3100 CR 100k
DEPOSIT Phase B  : 3100 DR 100k / 2110 CR 99k / 2110 DR 1k / 4110 CR 1k
WITHDRAW         : 2110 DR 101k / 3200 CR 101k → 3200 DR 100k / 1111 CR 100k → 3200 DR 1k / 4120 CR 1k
INT_TRANSFER     : 2110A DR 101k / 3300 CR 101k → 3300 DR 100k / 2110B CR 100k → 3300 DR 1k / 4130 CR 1k
IBFT             : 2110 DR 101k / 3400 CR 101k → 3400 DR 1k / 4130 CR 1k → 3400 DR 100k / 1112 CR 100k
                   + 5100 DR 500 / 1112 CR 500
WALLET_PAY       : 2110 DR 100k / 3500 CR 100k → 3500 DR 100k / 2120 CR 100k
QR_PAY           : 1113 DR 100k / 3500 CR 100k → 5100 DR 500 / 1113 CR 500 → 3500 DR 100k / 2120 CR 100k
PAYROLL(5)       : 2120 DR 505k / 3600 CR 505k → 3600 DR 5k / 4150 CR 5k → 3600 DR 500k / 1112 CR 500k
                   + 5100 DR 2.5k / 1112 CR 2.5k
DISBURSEMENT     : [Pre] 1111 DR 100k / 2130 CR 100k
                   [Dis] 2130 DR 101k / 3700 CR 101k → 3700 DR 1k / 4150 CR 1k → 3700 DR 100k / 1112 CR 100k
                   + 5100 DR 500 / 1112 CR 500
EOD              : 2120 DR 200k / 3800 CR 200k → 3800 DR 2k / 3820 CR 2k → 3800 DR 198k / 3810 CR 198k
                   → 3820 DR 2k / 4140 CR 2k → 3810 DR 198k / 1112 CR 198k
INIT_CAPITAL     : 1111 DR 1B / 6000 CR 1B; 1113 DR 500M / 6000 CR 500M; 1112 DR 500M / 6000 CR 500M
```

### B.2 Normal balance nhanh

| Normal Nợ (DR) | Normal Có (CR) | Net zero (Transit) |
|---------------|----------------|-------------------|
| 1111, 1112, 1113, 5100 | 2110, 2120, 2130, 4110–4150, 6000 | 3100–3820 |

### B.3 Transit zero checklist

| Transit | Use case | Zero khi nào? |
|---------|----------|--------------|
| 3100 | DEPOSIT | POSTED phase B |
| 3200 | WITHDRAW | POSTED |
| 3300 | INT_TRANSFER | POSTED |
| 3400 | IBFT | POSTED |
| 3500 | WALLET_PAY / QR | POSTED |
| 3600 | PAYROLL | POSTED |
| 3700 | DISBURSEMENT | POSTED |
| 3800 | EOD | Sau split MDR/net |
| 3810 | EOD | Sau bank outbound |
| 3820 | EOD | Sau MDR revenue |

### B.4 Revenue & matching

| Revenue | Chi phí đối ứng | Net margin mỗi txn |
|---------|----------------|-------------------|
| 4110 (deposit 1.000) | — | +1.000 |
| 4120 (withdraw 1.000) | — | +1.000 |
| 4130 (transfer 1.000) | — | +1.000 |
| 4130 (IBFT 1.000) | 5100 (500) | **+500** |
| 4140 (MDR 2.000) | 5100 (EOD) | `[TBD]` |
| 4150 (payroll 5.000) | 5100 (2.500) | **+2.500** |

### B.5 Liên kết ops nhanh

| Tình huống | Hành động | Căn cứ |
|-----------|-----------|--------|
| 3100 PENDING > SLA | Alert; xem deposit aging | ADR-006 |
| Transit ≠ 0 sau POSTED | Block; investigate journal | ADR-010 |
| W5 drift > tolerance | Alert ops; reversal + adjustment | ADR-014 |
| Muốn sửa dòng POSTED | Không — tạo reversal journal | ADR-001 |
| IBFT dùng 1111 | Reject; sửa template | ADR-025 |
| Amount scale > 4 | 400 error; fix tại boundary | ADR-028 |

---

## Đọc tiếp

| Tài liệu | Nội dung | Khi nào đọc |
|---------|---------|------------|
| [`vol-01-principles.md`](vol-01-principles.md) | Nguyên tắc, invariant, DR/CR masterclass E01–E25 | Trước Quyển II; context "vì sao" |
| [`vol-03-deposit.md`](vol-03-deposit.md) | Deep-dive use case Deposit (2-phase, saga, edge case) | Sau B.7, B.1 |
| [`foundation.md §8–16`](../../spec/foundation.md) | Nguồn gốc DR/CR tất cả use case | Source of truth DR/CR |
| [`postings.md`](../../design/accounting/postings.md) | Bảng DR/CR rút gọn | Quick reference |
| [`ADR-006`](../../adr/ADR-006-two-phase-deposit.md) | Two-phase deposit chi tiết | 3100, 1111, 2110, 4110 |
| [`ADR-010`](../../adr/ADR-010-transit-accounts-net-zero.md) | Transit zero — tất cả transit TK | B.7–B.16 |
| [`ADR-014`](../../adr/ADR-014-reconciliation-w5-report-only.md) | W5 recon report-only | 2110, 2120, 2130 |
| [`ADR-020`](../../adr/ADR-020-wallet-lanes-coa-control-mapping.md) | Lane → COA control mapping | 2110, 2120, 2130 |
| [`ADR-025`](../../adr/ADR-025-ibft-napas-clearing-1112.md) | IBFT → 1112, không 1111 | 1112, 3400, 5100 |
| [`ADR-028`](../../adr/ADR-028-money-scale-four-half-up.md) | Scale 4 HALF_UP | Tất cả TK |
| [`acceptance.md`](../../design-v2/acceptance.md) | Gherkin scenarios (source of truth test) | Khi viết integration test |

---

*Quyển II — Sổ tay Hệ thống tài khoản (COA) — hết*  
*Mọi `[TBD: ...]` trong tài liệu cần Jira ticket hoặc quyết định chốt trước release.*

