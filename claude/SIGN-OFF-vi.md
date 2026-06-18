# GtelPay Core — Phê duyệt thiết kế (Sign-Off)

| Mục | Giá trị |
|-----|---------|
| Tài liệu | Phê duyệt thiết kế & core đã kiểm chứng |
| Thành phần | `core.accounting` + `core.wallet` (+ `core.foundation`, `app-orchestration`) |
| Lĩnh vực | Thanh toán fiat (VND) |
| Trạng thái | **Bản nháp — chờ phê duyệt** |
| Trình cho | Architecture / Engineering / Finance / Product duyệt |

> **Mục đích.** Tài liệu này ghi nhận phê duyệt rằng **thiết kế** GtelPay Core và **core đã hiện thực** là vững, đúng phạm vi, đã kiểm chứng — và được phép sang giai đoạn kế. Mục 6 liệt kê các điểm còn hở để việc ký là **phê duyệt có hiểu biết**, không phải ký mù.

---

## 1. Phạm vi phê duyệt

**Trong phạm vi**
- `core.accounting` — sổ cái ghi kép bất biến (chart of accounts, journal, transit, EOD).
- `core.wallet` — số dư tiêu được theo member (available / frozen), movement chỉ-thêm.
- `core.foundation` — shared lib (envelope, error, money utils).
- `app-orchestration` — sequencer saga điều phối hai domain.
- Tiền tệ: **chỉ VND (fiat).**

**Ngoài phạm vi** (không thuộc lần ký này)
- Crypto / hot-cold custody / C2C-OTC escrow / đa tiền tệ.
- Codebase tham chiếu (`00_framework`, CoinExchange/BIZZAN) — chỉ là tham khảo kỹ thuật.

---

## 2. Đã giao gì

| Hạng mục | Nội dung |
|----------|----------|
| Quyết định thiết kế | **41 ADR**, mỗi cái có tiêu chí nghiệm thu (AC) + test case (TC) |
| Hành vi | Luồng nghiệp vụ (`spec/processes.md`), fund flow foundation |
| Contracts | OpenAPI + AsyncAPI (`spec/contracts/`) là nguồn sự thật của wire |
| Conformance | **150+ kịch bản nghiệm thu** (Given/When/Then) |
| Hiện thực | `core.foundation` · `core.wallet` · `core.accounting` · `app-orchestration` (Java / Spring Boot 3) — build được |
| Test tự động | **38 test pass** (`mvn test`, BUILD SUCCESS) |

---

## 3. Bằng chứng đúng đắn (lá chắn là thật và có test)

| Invariant | Đảm bảo | Kiểm bằng |
|-----------|---------|-----------|
| Idempotency (dưới lock) | Retry không áp hai lần / không double-spend | `concurrentSameTriple_appliesOnceAndReplaysLoser`, `duplicateBusinessRef_*` |
| Ghi kép cân | Mỗi journal: nợ = có, lệch là từ chối | `unbalancedLines_rejectsPost` |
| Sổ cái bất biến | POSTED line không sửa được | `postedLine_tamperIsIgnored_immutable` |
| Transit về 0 (mọi use case) | Không tiền kẹt giữa luồng khi POST | `balancedButStrandedTransit_rejectsPost`, `payment...transit3500Zero` |
| Period close | Không post vào kỳ CLOSED/LOCKED | `closedPeriod_rejectsPost` |
| Outflow giữ tiền | Settle trừ **frozen**, không trừ available (không double-spend) | `withdrawFreezeThenSettle`, `ibftFreezeThenSettle_deductsFrozenNotAvailable` |
| Snapshot + log cùng nhau | Không đổi số dư mà thiếu movement, cùng TX | `creditThenDebit_balanceMatches`, các test balance |

---

## 4. Các lỗ hổng vừa đóng (A1–A4)

| # | Lỗ hổng | Fix | Commit |
|---|---------|-----|--------|
| A1 | Transit-zero chỉ enforce cho deposit | Enforce cho **mọi** use case lúc post | `a362798` |
| A2 | IBFT settle có thể trừ available → double-spend | `IBFT_SETTLE` trừ từ frozen | `a362798` |
| A3 | Bất biến chỉ là quy ước | `@Immutable` — chốt cứng ở ORM | `66fd72e` |
| A4 | Period close (ADR-023) chưa làm | `coa_period` + guard lúc post | `66fd72e` |
| — | Test chứng minh A1–A4 | test red/green | `ca06314`, `c28ce9e` |

---

## 5. Cổng chất lượng

- `mvn test` → **BUILD SUCCESS, 38 test pass** (foundation 9 · wallet 16 · accounting 13).
- Bộ invariant SQL gác CI (ADR-031): journal cân, transit về 0.
- Contracts (OpenAPI/AsyncAPI) validate; schema pocket/error resolve.

---

## 6. Điểm còn hở & giới hạn đã biết (phê duyệt có hiểu biết)

**Ký không có nghĩa là mấy điểm này đã xong.** Chúng được chấp nhận là đã biết và hoãn/theo dõi:

1. **Multi-pocket (ADR-040)** đã thiết kế, spec, draft contract, nhưng **chưa hiện thực** trong code `core.wallet`.
2. **Quản lý kỳ tối thiểu** — đánh dấu kỳ CLOSED/LOCKED được, nhưng **chưa có workflow chốt sổ cuối kỳ tự động**.
3. **Gánh vận hành** — RabbitMQ worker, outbox relay, job đối soát cần năng lực ops để chạy/monitor.
4. **Eventual consistency** wallet ↔ accounting — có cửa sổ trễ; W5 **chỉ báo cáo** (lệch phải xử thủ công, không tự sửa).
5. **Chưa load-test** — số NFR (RPS, P95) là **mục tiêu**, không phải kết quả đo.
6. **Tài liệu phân tán** ở `design/`, `design-v2/`, `spec/`, `specs/` — chưa hợp nhất.
7. **Catalog tx-type** trong code là tập con của thiết kế (chưa thêm PARTNER/PAYROLL/MERCHANT_SETTLE).

---

## 7. Phê duyệt

Bằng việc ký, người duyệt xác nhận đã đọc Mục 1–6 và **đồng ý** cho thiết kế + core đã kiểm chứng tiến tới giai đoạn kế, kèm điều kiện (nếu có).

| Vai trò | Tên | Quyết định (Duyệt / Duyệt-kèm-điều-kiện / Từ chối) | Điều kiện / Ghi chú | Ngày | Chữ ký |
|---------|-----|----------------------------------------------------|---------------------|------|--------|
| Architecture |  |  |  |  |  |
| Engineering Lead |  |  |  |  |  |
| Kế toán / Tài chính |  |  |  |  |  |
| Product |  |  |  |  |  |
| Compliance / Audit *(nếu có)* |  |  |  |  |  |

**Quyết định tổng:** ☐ Duyệt  ☐ Duyệt kèm điều kiện  ☐ Từ chối

---

## 8. Tài liệu nền

`adr/` (41 ADR + AC/TC) · `spec/` (foundation, processes, contracts) · `design-v2/acceptance.md` (150+ scenario) · `platform/` (module chạy + test) · `DESIGN-BRIEF-vi.md` / `DESIGN-DECK-vi.pptx`.
