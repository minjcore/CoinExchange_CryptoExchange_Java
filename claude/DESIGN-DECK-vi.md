% GtelPay Core — Thiết kế
% Core kế toán + ví (fiat)
% Một core xây cho tiền — đúng đắn tối thiểu cần thiết, không phải over-engineering

# Luận điểm (một câu)

> Thanh toán không phải CRUD. Tụi mình xây core **nhỏ nhất** mà vẫn **đúng khi dính tới tiền** — và nó đã chạy.

- Hai domain, idempotent ngay từ thiết kế, một sổ cái bất biến, điều phối bằng saga.
- Mọi thứ trong deck này đều ứng với một **kiểu mất tiền cụ thể** mà nó ngăn chặn.
- Không có gì xây "phòng hờ" — có hẳn danh sách những thứ tụi mình **từ chối xây**.

---

# Phần 1 — Vì sao chuyện này quan trọng

# Thanh toán là tiền — bug là mất tiền

Ba kiểu lỗi mà một bảng ví thông thường không xử lý an toàn được:

- **Tiêu hai lần (double-spend)** — retry / race / timeout làm cùng một khoản tiền ra hai lần.
- **Sổ lệch hoặc sửa được** — không tin được sổ cái → **kiểm toán và cơ quan quản lý đánh rớt**.
- **Nhanh vs đúng** — "member tiêu được bao nhiêu *bây giờ*?" cần đọc cỡ mili-giây; "đã xảy ra chuyện gì?" cần sổ cái bất biến, kiểm toán được.

# Vì sao một bảng là không đủ

- Một dòng `balance` mutable thì **nhanh nhưng không kiểm toán được** — không lịch sử, sửa được, không bằng chứng.
- Sổ cái thuần (tính số dư từ lịch sử) thì **đúng nhưng chậm** — không kham nổi đường nóng (hot path) khi tiêu tiền.
- Ép cả hai vào một mô hình → **một trong hai sẽ vỡ**.
- → Thiết kế đi ra từ mâu thuẫn này, không phải từ sở thích.

---

# Phần 2 — Thiết kế (ba ý tưởng)

# Ý tưởng 1 — Tách hai domain có chủ đích

| | `core.wallet` | `core.accounting` |
|---|---|---|
| Câu hỏi | "Member tiêu được bao nhiêu giờ?" | "Sổ cái đã ghi nhận gì?" |
| Hình thái | số dư 1 dòng, nhanh (hot path) | sổ cái ghi kép, bất biến |
| Sự thật | tiêu được ngay | nguồn sự thật (system of record) |

Hai bên **không chung storage, không gọi nhau** — cô lập, scale độc lập.

# Ý tưởng 2 — Đúng đắn ngay từ thiết kế

- **Idempotent (chống lặp)** — mọi thay đổi đều có khóa; retry trả về cùng kết quả, không áp lại.
- **Ghi cùng nhau** — không số dư nào đổi mà thiếu dòng giao dịch, trong **cùng một transaction**.
- **Cân + bất biến** — mỗi journal có nợ = có; không sửa, sai thì ghi bút toán đảo.
- **Trung chuyển về 0** — tiền đang luân chuyển được track về 0 ở mỗi luồng hoàn tất.

# Ý tưởng 3 — Điều phối, không phải distributed transaction

- Một **sequencer mỏng** điều phối hai domain theo từng use case.
- **Saga + bù trừ (compensation)** — lỗi giữa chừng thì bù, không dùng 2-phase commit mong manh.
- Sổ cái được nạp bất đồng bộ (outbox → queue) — back-office không chặn đường nóng khi tiêu tiền.

# Bức tranh tổng

```
        client
          │
   orchestration (saga)
       ┌──┴──┐
   wallet   accounting
  (tiêu được) (sổ cái bất biến)
   hot path    nguồn sự thật
```

Một sequencer, hai sự thật, không chung state.

---

# Phần 3 — "Vậy có phải over-engineering không?"

# Phép thử trung thực

Over-engineering = phức tạp mà **không đem lại gì**. Ở đây, từng mảnh đều tự trả giá cho mình:

| Lá chắn | Lỗi nó ngăn |
|---------|-------------|
| Idempotency (dưới lock) | Tiêu / ghi có hai lần |
| Ghi kép + bất biến | Rớt kiểm toán / cơ quan quản lý |
| Tách wallet ⊥ accounting | Tiêu chậm **hoặc** sổ không tin được |
| Trung chuyển về 0 | Tiền kẹt giữa luồng |
| Saga + bù trừ | Tiền mắc kẹt khi sự cố |

# Vậy câu hỏi thật là…

> **Anh muốn bỏ lá chắn nào — và chấp nhận mất mát kiểu gì?**

- Bỏ idempotency → chấp nhận double-spend mỗi đợt retry dồn.
- Bỏ sổ cái bất biến → chấp nhận rớt kiểm toán.
- Mỗi lần "đơn giản hóa" đều có tên gọi và cái giá của nó.

# Những thứ tụi mình CỐ Ý không xây

*Over-engineering thật sự là mạ vàng. Tụi mình từ chối — ghi rõ trong ADR:*

- **4 module, không phải 40** — một shared lib tối thiểu (ADR-002)
- **Không 2PC, không Temporal** — RabbitMQ worker thường (ADR-035)
- **Không tự chế DB sổ cái** — PostgreSQL thường, hai schema (ADR-003)
- **Không event-sourcing / CQRS** — số dư snapshot + log chỉ-thêm (ADR-004)
- **Một tiền tệ (VND) v1** — không xây gì "phòng hờ" (ADR-019)

# Đây là chuẩn ngành, không phải đồ lạ

- Ghi kép, idempotency, sổ cái bất biến là thứ **ngân hàng, Stripe, Modern Treasury** đều xài.
- Gọi mấy cái này là "over-engineering" tức là gọi cả ngành thanh toán over-engineering.
- Tụi mình không phát minh primitive — chỉ áp chuẩn có sẵn, một cách tối thiểu.

---

# Phần 4 — Bằng chứng (nó chạy)

# Không phải giấy — code chạy thật

- `core.foundation` · `core.wallet` · `core.accounting` · `app-orchestration`
  → **đã build, test pass** (Java / Spring Boot 3).
- Luật trọng yếu về tiền được **code và test**, không chỉ mô tả:
  - idempotency check lại **dưới lock** → không áp hai lần
  - journal cân + bất biến · nạp tiền hai pha (chỉ credit sau khi sổ cái post)

# Quyết định được khóa và thực thi

- **41 ADR** — mỗi cái có tiêu chí nghiệm thu (AC) + test case (TC).
- **150+ kịch bản nghiệm thu** (Given/When/Then) làm cổng conformance.
- **Bộ invariant SQL gác build** — mọi journal phải cân, trung chuyển về 0 — lệch là **rớt CI**, không lọt production.

---

# Phần 5 — Quyết định

# Tự xây hay mua sẵn

- Blnk / Formance / TigerBeetle **gộp số dư + sổ cái** — tốt cho ledger nhúng.
- Nhu cầu của mình: **chart of accounts thật · trung chuyển về 0 · settlement cuối ngày · audit chuẩn cơ quan quản lý · số dư tiêu được RPS cao.**
- **Quyết định:** mượn pattern đã được chứng minh; chỉ tự xây phần phải khớp thực tế ngân hàng/kiểm toán.
- Phần build **nhỏ, tập trung, và đã chạy.**

# Rủi ro được kiểm soát

- **Tương thích ngược** — tính năng mới là opt-in; client cũ không đổi gì.
- **Đối soát chỉ báo cáo** — phát hiện lệch, không tự sửa sổ.
- **Hợp đồng API (OpenAPI / AsyncAPI) là nguồn sự thật** — wire không lệch khỏi thiết kế.

# Phạm vi

- `accounting` (sổ cái) + `wallet` (số dư) + `foundation` + `orchestration`.
- **Fiat (VND).**
- Tài liệu nền khi cần: 41 ADR · luồng nghiệp vụ · contracts · 150+ kịch bản nghiệm thu.

# Đề nghị

- Đây là **thiết kế đơn giản nhất mà không mất tiền.**
- *"Đơn giản là tốt — và đây **chính là** bản đơn giản nhất mà vẫn đúng."*
- Muốn đơn giản hơn? **Nói rõ lá chắn nào cắt, và gánh cái mất mát đó.**

# Cảm ơn

**GtelPay Core** — core kế toán + ví (fiat).
Xây cho tiền. Đã chạy. Tài liệu nền có sẵn khi cần.
