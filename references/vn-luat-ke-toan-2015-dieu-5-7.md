# Luật Kế toán 2015 (88/2015/QH13) — Điều 5–7 (excerpt)

**Source:** https://luatvietnam.vn/ke-toan/luat-ke-toan-nam-2015-so-88-2015-qh13-101336-d1.html  
**Scraped:** 2026-06-08 · **Tool:** Firecrawl · Excerpt only — không phải văn bản gốc đầy đủ.

---

## Điều 3. Đơn vị tiền tệ trong kế toán (tóm tắt)

Đơn vị tiền tệ trong kế toán mặc định là **Đồng Việt Nam (VND)**. DN có thể chọn ngoại tệ làm đơn vị ghi sổ nếu đáp ứng tiêu chuẩn (thu/chi chủ yếu bằng ngoại tệ) và thông báo cơ quan thuế.

→ Map: ADR-019 (VND only v1).

---

## Điều 5. Yêu cầu kế toán

1. Phản ánh **đầy đủ** nghiệp vụ kinh tế, tài chính vào chứng từ, sổ, BCTC.
2. Phản ánh **kịp thời**, đúng thời gian quy định.
3. Phản ánh **rõ ràng, dễ hiểu, chính xác**.
4. Phản ánh **trung thực, khách quan** hiện trạng, **bản chất** sự việc.
5. Phản ánh **liên tục** từ khi phát sinh đến khi kết thúc; kỳ sau kế tiếp kỳ trước.
6. Phân loại, sắp xếp **có hệ thống**, so sánh và kiểm chứng được.

---

## Điều 6. Nguyên tắc kế toán

1. **Giá gốc / giá trị hợp lý:** Tài sản và nợ ghi nhận ban đầu theo **giá gốc**; một số loại biến động theo thị trường có thể đo lại **giá trị hợp lý** cuối kỳ.
2. **Nhất quán (consistency):** Phương pháp kế toán đã chọn áp dụng nhất quán trong năm; đổi phương pháp → giải trình BCTC.
3. **Khách quan, đầy đủ, đúng kỳ:** Thu thập và phản ánh đúng thực tế, đúng kỳ phát sinh.
4. **Công khai BCTC:** Lập, gửi, công khai theo luật.
5. **Thận trọng (prudence):** Đánh giá tài sản và phân bổ thu/chi thận trọng, không làm sai lệch kết quả.
6. **Bản chất hơn hình thức (substance over form):** BCTC phản ánh **đúng bản chất** giao dịch hơn tên gọi/hình thức.
7. (Cơ quan NN, đơn vị sự nghiệp) Thêm mục lục ngân sách nhà nước.

---

## Điều 7. Chuẩn mực kế toán (VAS)

1. Chuẩn mực kế toán = quy định và phương pháp cơ bản lập BCTC.
2. Bộ Tài chính ban hành trên cơ sở **chuẩn mực quốc tế** phù hợp điều kiện VN.
3. → **VAS** (26 CMKT 2001–2005) + **TT 99/2025** thay TT200 từ 01/01/2026.

---

## Map → `10_core`

| VN principle | Platform binding |
|--------------|------------------|
| Điều 6 k.6 substance over form | `use_case` + template, không ghi theo tên API |
| Điều 6 k.5 prudence | ADR-007 UNKNOWN không RELEASE; ADR-034 LOCKED |
| Điều 6 k.2 consistency | ADR-028 scale; template cố định |
| Điều 5 k.4 trung thực/khách quan | ADR-001 immutable; W5 report-only |
| VND Điều 3 | ADR-019 |
| Accrual (VAS/IFRS roadmap) | **ADR-036** accrual-like ledger v1 |

**Phân tích thêm:** [`hoiketoanhcm-nguyen-tac-luat-ke-toan-2015.md`](./hoiketoanhcm-nguyen-tac-luat-ke-toan-2015.md)
