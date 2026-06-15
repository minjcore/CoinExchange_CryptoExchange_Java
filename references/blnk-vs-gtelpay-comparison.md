# Blnk Finance vs GtelPay Core — so sánh kiến trúc ledger

**Loại:** Synthesis (không phải scrape) · **Updated:** 2026-06-12 · **Scope:** đối chiếu mô hình Blnk với design `10_core`

**Nguồn Blnk:** [`blnk-balances-intro.md`](./blnk-balances-intro.md) · [`blnk-double-entry-guide.md`](./blnk-double-entry-guide.md) · [`blnk-transactions-intro.md`](./blnk-transactions-intro.md) · [`blnk-transaction-lifecycle.md`](./blnk-transaction-lifecycle.md) · [`blnk-balance-monitoring.md`](./blnk-balance-monitoring.md) · [`blnk-balance-snapshots.md`](./blnk-balance-snapshots.md) · [`blnk-historical-balances.md`](./blnk-historical-balances.md) · [`blnk-reconciliations-overview.md`](./blnk-reconciliations-overview.md) · [`blnk-reconciliation-strategies.md`](./blnk-reconciliation-strategies.md) · index https://docs.blnkfinance.com/llms.txt

**Đối chiếu dự án:** [`spec/foundation.md`](../spec/foundation.md) Part II · [`design-v2/accounting.md`](../design-v2/accounting.md) · [`design-v2/wallet.md`](../design-v2/wallet.md) · [`design-v2/orchestration.md`](../design-v2/orchestration.md) · [`design/platform/boundaries.md`](../design/platform/boundaries.md) · `adr/`

> **Mục đích:** Blnk là một trong các corpus tham chiếu cho inflight/hold ([`design-v2/wallet.md`](../design-v2/wallet.md) §4.1) và immutability. Doc này làm rõ **chỗ nào hai mô hình trùng, chỗ nào khác, và 3 concept Blnk đáng vay**. Đây là tham chiếu — **không** phải quyết định; thay đổi design phải qua ADR.

---

## 1. Khác biệt gốc — một câu

**Blnk gộp "số dư" và "kế toán" làm một** (balance *chính là* account, double-entry ẩn qua `source → destination`). **GtelPay tách đôi có chủ đích**: `core.accounting` (sổ cái COA thật, double-entry tường minh) + `core.wallet` (cache "tiêu được bao nhiêu"), ranh giới cứng ([`design/platform/boundaries.md`](../design/platform/boundaries.md), [ADR-003](../adr/ADR-003-dual-schema-single-postgres.md)).

Mọi khác biệt còn lại đều phát sinh từ điểm này.

| | Blnk | GtelPay Core |
|---|------|--------------|
| Hình thái | Embedded ledger primitive (1 ledger hợp nhất) | Core thanh toán đa domain chuẩn ngân hàng VN |
| Số domain | 1 | 2 (`accounting` + `wallet`) + orchestration tách riêng |
| Double-entry | Ẩn (mỗi txn 1 src + 1 dst) | Tường minh (`coa_trans` header, N lines `coa_trans_data`, `sum(DR)=sum(CR)`) |
| Chart of Accounts | Không có (balance tự do) | COA có mã: 1xxx asset · 2xxx liability · 3xxx transit · 4xxx revenue · 5xxx expense · 6xxx equity |
| Số dư member | `balance` derived | `wallet_balance.available` lưu sẵn ([ADR-004](../adr/ADR-004-wallet-balance-snapshot.md)) + aggregate 2110/2120/2130 ở accounting |
| Tiền tệ | Multi, field `precision` per-txn | VND single ([ADR-019](../adr/ADR-019-vnd-single-currency-v1.md)), `BigDecimal` scale 4 HALF_UP ([ADR-028](../adr/ADR-028-money-scale-four-half-up.md)) |

---

## 2. Bảng ánh xạ concept

| Concept | Blnk | GtelPay 10_core | Ghi chú |
|---|---|---|---|
| Đơn vị ghi sổ | `transaction` (source/dest) | `coa_trans` (journal) | Blnk 1 txn = 1 cặp; GtelPay 1 journal = N lines |
| Dòng ghi | implicit | `coa_trans_data` (mỗi line 1 DR *hoặc* CR) | |
| Account | `balance` (internal/external) | `coa_account` (COA) + `wallet_balance` (per-member) | GtelPay tách aggregate vs per-member |
| Net balance | `balance = credit_balance − debit_balance` | Accounting: derive từ POSTED lines · Wallet: `available` lưu sẵn | |
| Hold / pending out | `inflight_balance` | `wallet_balance.frozen` + transit `3xxx` | **Blnk 1 cơ chế, GtelPay 2** — xem §3a |
| Pending inflow | `queued_balance` | *không model trong wallet* | Deposit chưa POSTED → `available` không đổi |
| Idempotency | `reference` UNIQUE | `business_ref`/`reference_id` UNIQUE + triple `(wallet_id, business_ref, tx_type)` | [ADR-005](../adr/ADR-005-idempotency-key-strategy.md) |
| Two-phase | `INFLIGHT → commit/void` (+ partial commit) | Deposit `PENDING→POSTED` ([ADR-006](../adr/ADR-006-two-phase-deposit.md)) · Withdraw `FREEZE→SETTLE/RELEASE` ([ADR-007](../adr/ADR-007-freeze-settle-async-outflow.md)) | |
| Multi-leg | `atomic` multi-source/dest | N lines/journal + transit net-zero ([ADR-010](../adr/ADR-010-transit-accounts-net-zero.md)) | |
| Immutability | transaction immutable | [ADR-001](../adr/ADR-001-immutable-ledger.md); sửa = reversal journal | Cùng nguyên tắc |
| Reconciliation | recompute balance từ txns, **ghi đè** (1:1/1:N/N:1) | W5 **report-only** ([ADR-014](../adr/ADR-014-reconciliation-w5-report-only.md)) + invariant SQL CI ([ADR-031](../adr/ADR-031-sql-ledger-invariant-ci.md)) | Xem §3c |
| Precision | field `precision` per-txn | scale 4 cố định | VND single nên không cần per-txn |
| Monitor / alert | `balance.monitor` webhook | invariant CI offline (chưa có realtime alert) | **Gap** — §4 |
| Snapshot / historical | snapshot job + historical-balance API | snapshot ([ADR-004](../adr/ADR-004-wallet-balance-snapshot.md), [ADR-032](../adr/ADR-032-wallet-balance-snapshot.md)) | API "balance tại T" — pattern Blnk tốt |
| Scheduling | `SCHEDULED` txn (future-dated) | EOD batch job ([ADR-015](../adr/ADR-015-eod-settlement-independent-batch.md)) | Khác phạm vi |
| Hooks | `PRE_TRANSACTION` / `POST_TRANSACTION` | orchestration saga steps ([ADR-012](../adr/ADR-012-orchestration-integration-forbidden-rules.md)) | |
| Identity | `identity` + PII tokenization (AES-GCM) | thuộc auth/identity layer ([ADR-011](../adr/ADR-011-auth-identity-jwt-subject.md)) | Ngoài core ledger |

---

## 3. Bốn điểm phân kỳ quan trọng

### a) Inflight: Blnk một cơ chế, GtelPay hai cơ chế theo nghĩa kinh tế

Blnk dùng *cùng* `inflight_balance` cho mọi hold. GtelPay tách theo bản chất dòng tiền:

| Tình huống | Tiền thật đã có? | GtelPay giữ chỗ ở đâu | Vì sao |
|---|---|---|---|
| Deposit chưa xác nhận | Chưa | **transit 3100 (accounting)** — wallet *không đổi* | Chưa có tiền → không được tính vào "tiêu được" |
| Withdraw / IBFT in-flight | Có (của member) | **`frozen` (wallet)** + transit 3200/3400 | Đã có tiền → giữ chỗ ngay ở wallet |

→ Blnk không phân biệt tự nhiên hai nghĩa này. GtelPay tách để `available` luôn phản ánh đúng "tiêu được ngay bây giờ".

### b) "Timeout ≠ RELEASE" — GtelPay nghiêm ngặt hơn

Blnk inflight có **expiry** (tự void khi hết hạn). GtelPay **cấm** release theo timeout ([ADR-007](../adr/ADR-007-freeze-settle-async-outflow.md), [`design-v2/wallet.md`](../design-v2/wallet.md) §4.1): bank/Napas chưa rõ kết cục thì **poll**, `frozen` giữ tới khi có terminal outcome. Áp thẳng Blnk-expiry vào fiat outflow sẽ tạo rủi ro double-spend.

### c) Reconciliation: Blnk có thể sửa, GtelPay report-only

Blnk recompute balance từ transactions rồi **ghi đè** (hợp lệ vì balance là derived). GtelPay W5 **chỉ báo lệch** ([ADR-014](../adr/ADR-014-reconciliation-w5-report-only.md)) — wallet là cache và **không được tự sửa theo accounting** ([ADR-026](../adr/ADR-026-wallet-never-reverses-accounting.md)). Khác biệt bắt buộc do kiến trúc 2 domain.

### d) Partial commit — Blnk có, GtelPay chưa

Blnk commit *một phần* inflight (settle dần). GtelPay freeze/settle/release **toàn phần** theo `business_ref`. Là **gap** đáng cân nhắc, đặc biệt khớp với partial batch payroll/disbursement ([ADR-017](../adr/ADR-017-partial-batch-payroll-disbursement.md)).

---

## 4. Concept Blnk đáng vay (candidate, cần ADR)

| Concept Blnk | Đánh giá | Khớp với |
|---|---|---|
| **Partial commit inflight** | **Đáng** — settle per-recipient từng phần | [ADR-017](../adr/ADR-017-partial-batch-payroll-disbursement.md) partial batch |
| **Balance monitor realtime** (`balance.monitor` webhook) | **Đáng** — hiện chỉ có invariant CI offline; thiếu cảnh báo realtime khi 2110 lệch / frozen bất thường | [ADR-031](../adr/ADR-031-sql-ledger-invariant-ci.md) |
| **Historical-balance API** ("số dư tại thời điểm T") | **Đáng** — bổ trợ snapshot hiện có | [ADR-004](../adr/ADR-004-wallet-balance-snapshot.md), [ADR-032](../adr/ADR-032-wallet-balance-snapshot.md) |
| `queued_balance` (hiển thị pending inflow) | Cân nhắc UX — GtelPay cố ý không model; UI "đang xử lý" lấy từ accounting state | — |
| `precision` per-txn | **Không cần** — VND single, scale 4 cố định tốt hơn | [ADR-019](../adr/ADR-019-vnd-single-currency-v1.md), [ADR-028](../adr/ADR-028-money-scale-four-half-up.md) |

---

## 5. GtelPay khác/mạnh hơn có lý do

- **COA thật + transit net-zero**: bắt buộc cho ngân hàng/Napas, audit, EOD settlement. Source/dest của Blnk không tự cho báo cáo theo nhóm tài khoản (asset/liability/revenue/expense/equity).
- **Tách wallet/accounting**: cô lập sự cố, scale độc lập (split 2 DB chỉ đổi datasource URL — [ADR-003](../adr/ADR-003-dual-schema-single-postgres.md)). Ledger monolithic của Blnk không có ranh giới này.
- **Saga + outbox, no 2PC** ([ADR-008](../adr/ADR-008-saga-compensation-no-2pc.md), [ADR-013](../adr/ADR-013-outbox-at-least-once-messaging.md)): quản lý nhất quán xuyên domain tường minh; Blnk không cần vì chỉ 1 ledger.

---

## 6. Kết luận

Blnk = *embedded ledger primitive* (số dư = kế toán, inflight thống nhất, recon tự sửa). GtelPay = *core thanh toán đa domain chuẩn ngân hàng VN* (COA thật + wallet cache, hold tách theo nghĩa kinh tế, recon report-only). **Không thay thế nhau.** GtelPay có thể vay 3 concept cụ thể: **partial commit, balance monitor realtime, historical-balance API** — mỗi cái cần một ADR riêng trước khi đưa vào design.
