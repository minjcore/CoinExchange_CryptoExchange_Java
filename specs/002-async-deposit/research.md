# Research: Async Deposit — Phase 0

**Feature**: `002-async-deposit` | **Date**: 2026-06-18

All decisions below are locked by existing ADRs. No open unknowns remain for this feature.

---

## R1 — TigerBeetle là gì và tại sao dùng cho `core.accounting`

### TigerBeetle là gì?

**TigerBeetle** ([tigerbeetle.com](https://tigerbeetle.com)) là một **database chuyên dụng cho kế toán double-entry**. Không phải PostgreSQL, không phải Redis — nó là một engine riêng biệt được xây từ đầu chỉ để làm một việc: ghi sổ cái tài chính với tốc độ cao và đảm bảo tuyệt đối.

**Ngôn ngữ**: Zig (cùng tác giả với Bun.js runtime).

**Khái niệm cốt lõi**:

| Khái niệm | Mô tả |
|-----------|-------|
| `Account` | Một tài khoản kế toán (tương đương `coa_account` — ví dụ: 1111, 3100, 2110). Lưu số dư DR/CR. |
| `Transfer` | Một bút toán double-entry: debit một account, credit một account. Immutable sau khi committed. |
| `pending` flag | Transfer được tạo ở trạng thái PENDING — chưa ảnh hưởng tới số dư thật. |
| `post_pending_transfer` | Xác nhận một pending transfer → chuyển thành POSTED, cập nhật số dư. |
| `void_pending_transfer` | Huỷ một pending transfer — không có tiền nào di chuyển. |
| Transfer `id` | u128, do caller tự đặt, **idempotency key** tự nhiên — gửi lại cùng `id` → no-op. |
| Amounts | u128 nguyên — không có floating point. GtelPay dùng ×10⁴ (scale 4) để biểu diễn VND. |

**Tại sao không PostgreSQL cho postings?**

| Vấn đề với Postgres | TigerBeetle giải quyết thế nào |
|--------------------|-------------------------------|
| `SUM(amount)` trên `coa_trans_data` để lấy số dư tài khoản → full scan khi bảng lớn | Account balance là field sẵn trên `Account` — O(1) read |
| Row locking khi concurrent postings lên cùng một COA account | Single-threaded event loop + batched I/O — không lock contention |
| Thêm index cho performance → write amplification | Append-only log — không index overhead trên ghi |
| Reporting queries và hot-path postings cạnh tranh I/O | TB chỉ làm postings; reporting đọc từ Postgres read-model (projector) |

**Throughput**: TB thiết kế để xử lý **1+ triệu transfers/giây** trên 1 node. GtelPay deposit volume thấp hơn nhiều bậc — dùng TB nghĩa là headroom gần như vô hạn và không bao giờ cần shard accounting.

### Mapping GtelPay → TigerBeetle cho Async Deposit

```
Phase A (PENDING journal → ADR-006):
  GtelPay: coa_trans status=PENDING, lines: 1111 DR gross / 3100 CR gross
  TigerBeetle: pending Transfer: debit_account=1111, credit_account=3100, amount=gross×10⁴, id=hash(businessRef)

Phase B (POSTED via confirmDeposit → ADR-006):
  GtelPay: coa_trans status=POSTED, transit 3100=0, 2110+4110
  TigerBeetle:
    1. post_pending_transfer(id=hash(businessRef))       → closes 1111/3100 pending
    2. Transfer: debit_account=3100, credit_account=2110, amount=net×10⁴, id=hash(businessRef+":2110")
    3. Transfer: debit_account=3100, credit_account=4110, amount=fee×10⁴, id=hash(businessRef+":4110")
    → 3100 balance = 0 after step 1+2+3

Idempotency:
  Transfer id = deterministic hash(businessRef, leg) → replay same id = TB no-op
```

**`core.accounting` architecture** (ADR-037 hybrid):

```
app-orchestration ──HTTP──► core.accounting (accounting-internal.yaml)
                                    │
                    ┌───────────────┴───────────────┐
                    ▼                               ▼
            TigerBeetle cluster              PostgreSQL (schema accounting)
            ─────────────────────            ───────────────────────────────
            Transfers (hot postings)         coa_account (COA master data)
            Account balances (runtime)       coa_trans (journal header + status)
            Source of truth for DR/CR        Period close, reconciliation state
                                             Read-model (coa_trans_data projected)
                                             Audit log
```

**Ranh giới quan trọng**:
- Chỉ `core.accounting` adapter mở TB client — orchestration và wallet không bao giờ gọi TB trực tiếp (AC-037-01).
- `accounting-internal.yaml` không thay đổi — TB là implementation detail (AC-037-02).
- Reporting không query TB trực tiếp — đọc từ Postgres read-model mà projector build từ TB events (AC-037-05).

---

## R2 — Blnk là gì và PoC ở `core.wallet`

### Blnk là gì?

**Blnk** ([blnkfinance.com](https://blnkfinance.com)) là một **open-source ledger engine viết bằng Go**. Được thiết kế như một *embedded ledger primitive* — một layer để build wallet/ledger trên đó, không phải một giải pháp full banking.

**Điểm khác biệt cốt lõi**:

| Blnk | GtelPay `core.wallet` |
|------|-----------------------|
| Balance **chính là** account — double-entry ẩn qua `source → destination` | `wallet_balance` là cache "tiêu được" — accounting COA là nguồn chính thức |
| 1 ledger hợp nhất (số dư = kế toán) | 2 domain tách biệt: wallet cache + accounting ledger |
| `inflight_balance` cho tất cả holds | `frozen` (wallet) cho withdraw in-flight; transit 3100 (accounting) cho deposit chưa xác nhận |
| Reconciliation có thể ghi đè balance | Recon là report-only — wallet không tự sửa theo accounting (ADR-014, ADR-026) |
| REST API service | Domain Java module, không phải standalone service |

**3 concept Blnk đáng vay cho GtelPay** (từ `references/blnk-vs-gtelpay-comparison.md`):

| Concept Blnk | Áp dụng cho GtelPay | Status |
|---|---|---|
| **Balance monitor realtime** — `balance.monitor` webhook trigger khi balance vượt ngưỡng | Hiện chỉ có invariant CI offline. Gap cảnh báo realtime khi 2110 lệch hoặc frozen bất thường | Candidate — cần ADR riêng |
| **Historical-balance API** — "số dư của member lúc T" bổ trợ snapshot | Hữu ích cho audit, dispute resolution, statement | Candidate — cần ADR riêng |
| **Partial commit inflight** — settle từng phần một lô | Relevant cho partial payroll disbursement (ADR-017) | Candidate — cần ADR riêng |

### PoC Blnk cho `core.wallet` — phạm vi của feature này

**Phạm vi PoC trong feature 002 là hẹp và không ảnh hưởng production path**:

1. **Balance monitor pattern**: thiết kế interface `WalletBalanceMonitor` trong `core.wallet` — khi `wallet_balance.available` thay đổi, fire event nếu vượt ngưỡng. Lấy cảm hứng từ Blnk `balance.monitor`, triển khai bằng Spring ApplicationEvent (không phụ thuộc Blnk binary).

2. **Historical balance query**: trong `WalletQueryService` thêm `getBalanceAt(walletId, timestamp)` — reconstruct từ `wallet_tx` snapshots. Blnk làm việc này với snapshot job; GtelPay đã có `wallet_tx` after-balance snapshot nên pattern tương tự khả thi.

**Blnk KHÔNG thay thế `core.wallet`** — kiến trúc hai domain vẫn giữ nguyên. PoC chỉ implement 2 pattern từ Blnk vào Java domain code, không import Blnk dependency.

---

## R3 — Dual wallet-credit path (ADR-024)

Sau khi journal POSTED, wallet credit có thể đến từ 3 path:

| Path | Transport | Khi nào dùng |
|------|-----------|-------------|
| A | Kafka `core.accounting.journal-posted` | Consumer filter `use_case=DEPOSIT` + `status=POSTED` |
| B | RabbitMQ `core.commands.wallet-credit` | Orchestration explicit command sau khi accounting worker báo POSTED |
| C | HTTP sync `WalletGateway` | Migration path / fallback khi queue không available |

**v1 recommendation**: Path B (RabbitMQ) — đồng nhất với BANK_DEPOSIT pattern, accounting worker publish `WALLET_CREDIT` command sau `confirmDeposit` thành công. Wallet worker nhận, gate trên `coa_trans.status = POSTED`, credit idempotent.

---

## R4 — Outbox safety (ADR-013)

`BANK_DEPOSIT` command phải được publish **at-least-once** ngay cả khi orchestration crash sau 202. Pattern:

1. Orchestration ghi `outbox` row trong cùng transaction với saga state (nếu có).
2. Outbox relay process poll và publish lên RabbitMQ exchange `core.commands`.
3. Accounting worker idempotent trên `(commandType, businessRef)` — duplicate delivery = no-op.
4. DLQ `core.commands.dlq` nhận poison messages sau max retries.

---

## Resolved unknowns

Không có NEEDS CLARIFICATION nào còn tồn tại — tất cả đã locked bởi ADR-006, ADR-013, ADR-024, ADR-030, ADR-035, ADR-037, ADR-041.
