# 9 điều cần biết trước khi code GtelPay Core

> **CF page ID:** 51839571 | **Parent:** 📌 Start Here (51315064)
> **Source of truth:** this file → push to CF

9 điều này là nền tảng kỹ thuật của mọi hệ thống ví tiền. Đọc xong là hình dung được toàn bộ luồng tiền đi từ đâu, lưu ở đâu, và tại sao không bị trùng hay mất.

---

1. **Schema boundary** — `wallet.*` và `accounting.*` không bao giờ JOIN/FK nhau. `wallet_tx.coa_trans_id` = correlation only. (ADR-003)

2. **Wallet balance là snapshot** — đọc 1 row, không derive từ sum. Mỗi mutation = 1 `wallet_tx` trong cùng DB transaction. (ADR-004)

3. **Deposit là two-phase** — Phase A: TB `flags.pending` (1111→3100). Phase B: `confirmDeposit` → post + 3100→2110+4110. Transit 3100 = 0 sau Phase B, không có ngoại lệ. (ADR-006, ADR-010)

4. **RabbitMQ is inbound** — orchestration publish command → worker. Kafka = outbound events từ worker ra ngoài. (ADR-041, ADR-013)

5. **businessRef chạy end-to-end** — `X-Idempotency-Key` → outbox → `BANK_DEPOSIT` → `coa_trans.reference_id` → `WALLET_CREDIT` → `wallet_tx.business_ref`. Replay với cùng data = trả existing IDs, không tạo mới. (ADR-005)

6. **Fee tính 1 lần tại orchestration** — worker nhận `grossAmount` + `fee` đã tính sẵn, không tính lại. Fee 4110 nằm trên cùng journal với movement. (ADR-009, ADR-028)

7. **POSTED = immutable** — không UPDATE/DELETE `coa_trans_data` của POSTED. Sửa sai = journal đảo ngược mới. (ADR-001)

8. **Wallet credit chỉ sau POSTED** — gate bắt buộc: `coa_trans.status = POSTED` trước khi INSERT `wallet_tx`. Không credit trên Phase A. (ADR-024, ADR-026)

9. **Fail-fast tại mọi boundary** — VND + scale≤4 + JWT `sub` tại orchestration. LOCKED wallet reject debit/deposit. Transit≠0 reject `postJournal`. Không silent absorb. (ADR-019, ADR-029, ADR-010, ADR-011)

---

> Chi tiết từng khái niệm: xem [Architecture FAQ](https://nivc.atlassian.net/wiki/spaces/GtelPay/pages/51544171) và [ADR pre-reading checklist](https://nivc.atlassian.net/wiki/spaces/GtelPay/pages/51872153).
