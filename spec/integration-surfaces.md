# Danh sách kết nối giữa các service

**Author:** Cao Khang Đoàn  
**Last updated:** 2026-06-12  
**Status:** Draft  

**Câu hỏi file này trả lời:** Wallet và sổ kế toán chạy riêng — **service nào đứng giữa** để chạy lần lượt cả hai, và **file YAML nào** mô tả từng đường gọi?

Sơ đồ: [`architecture-overview.md`](./architecture-overview.md) · [`architecture.drawio`](./architecture.drawio).  
Thứ tự bước chi tiết: §5. Code monolith tạm: [`implementation.md`](./implementation.md) §9.

---

## 1. Ba lớp (từ ngoài vào trong)

Wallet và accounting **không gọi nhau** — mỗi nghiệp vụ (thanh toán, nạp tiền, chuyển…) lại cần **cả hai**. Vì vậy phải có **một lớp điều phối** ở giữa: chạy lần lượt trừ tiền → ghi sổ → cộng tiền, giữ `businessRef`, và là chỗ **duy nhất** được gọi xuống wallet/accounting.

| Lớp | Service | Làm gì |
|-----|---------|--------|
| **1 — paymentorches** | Ngoài repo GtelPay Core | **Service GtelPay:** API cho app/đối tác (`gtelpay-public`); nhận webhook bank; **gọi ra** PSP/bank/Napas khi cần chuyển tiền kênh; rồi gọi core khi cần ví/sổ. **Không** phải bank/PSP. |
| **2 — app-orchestration** | Trong GtelPay Core | Nhận từ paymentorches; gọi wallet + accounting; gửi queue/event khi cần |
| **3 — wallet · accounting** | Trong GtelPay Core | Chỉ xử lý nghiệp vụ riêng; chỉ nhận call từ lớp 2 |

Bank, Napas, PSP là **bên ngoài** — paymentorches **tích hợp** với họ, không thay thế họ.

```
Client (app, đối tác) → Gateway → paymentorches     (gtelpay-public.yaml)
Bank webhook ────────────────┘         │
                                       ├→ PSP / bank / Napas (bên ngoài)
                                       └→ app-orchestration (gtelpay-core-internal.yaml)
                                              → wallet / accounting
                                              → RabbitMQ / Kafka (chạy nền)
```

Lớp 1 và lớp 3 **không** gọi thẳng nhau. Mọi thứ đi qua lớp 2.

---

## 2. File YAML nào cho đường gọi nào

| Ai code | Ai gọi | File |
|---------|--------|------|
| paymentorches | Client, Gateway | [`openapi/gtelpay-public.yaml`](./openapi/gtelpay-public.yaml) |
| app-orchestration | paymentorches | [`openapi/gtelpay-core-internal.yaml`](./openapi/gtelpay-core-internal.yaml) |
| wallet | app-orchestration | [`openapi/wallet-internal.yaml`](./openapi/wallet-internal.yaml) |
| accounting | app-orchestration | [`openapi/accounting-internal.yaml`](./openapi/accounting-internal.yaml) |
| app-orchestration (+ worker) | nội bộ | [`asyncapi/core-commands.yaml`](./asyncapi/core-commands.yaml) (RabbitMQ) |
| app-orchestration / domain | nội bộ | [`asyncapi/core-events.yaml`](./asyncapi/core-events.yaml) (Kafka) |

Format response chung (`ApiResponse`, lỗi, phân trang): [`foundation.md`](./foundation.md) — thư viện dùng chung, không phải API riêng.

Gateway (platform) route về **paymentorches**, không route thẳng core — ví dụ [`gateway/routes.example.yaml`](./gateway/routes.example.yaml).

**Thanh toán (đồng bộ):** app-orchestration → wallet → accounting → wallet → trả kết quả.  
**Nạp tiền (nền):** app-orchestration trả **202** → queue → worker → (tuỳ chọn) event → cộng ví.

---

## 3. Bảng S1–S6 cũ (chỉ để tra tài liệu cũ)

| Cũ | Tương ứng (tại app-orchestration) |
|----|----------------------------------|
| S1 | Public API thuộc paymentorches; core nhận qua `gtelpay-core-internal` |
| S2 | `accounting-internal.yaml` |
| S3 | `core-events.yaml` (Kafka) |
| S4 | Cấu hình Gateway — không thuộc core |
| S5 | `foundation` — thư viện, không phải API |
| S6 | `core-commands.yaml` (RabbitMQ) |

---

## 4. Đọc gì theo việc mình làm

| Bạn làm | Đọc trước | Đọc tiếp |
|---------|-----------|----------|
| app-orchestration | §2, §5 | `foundation.md`, `processes.md` |
| paymentorches | `gtelpay-public.yaml` | Gọi `gtelpay-core-internal.yaml` |
| wallet / accounting | TRD từng phần | Chỉ implement API nội bộ — không cần bảng §5 full |
| Worker | `core-commands.yaml` §7 | Cùng format message với app-orchestration gửi |

---

## 5. Nghiệp vụ × đường gọi

Chi tiết ví: [`trd/wallet.md` §5](./trd/wallet.md). Chi tiết sổ: [`foundation.md`](./foundation.md) §8–16.

| Nghiệp vụ | app-orchestration | accounting | RabbitMQ | Kafka | Wallet | Ghi chú |
|-----------|-------------------|------------|----------|-------|--------|---------|
| **Nạp tiền** | `notifyDeposit` → **202** | PENDING → POSTED | `BANK_DEPOSIT` | event (tuỳ chọn) | Cộng ví sau POSTED | Ưu tiên queue |
| **Thanh toán** | `createPayment` → **200** | tạo + ghi sổ | — | tuỳ chọn | trừ USER → cộng MERCHANT | Một request |
| **Chuyển** | `createTransfer` → **200** | ghi sổ transit | — | tuỳ chọn | trừ A → cộng B | Đồng bộ |
| **Rút** | `createWithdrawal` → **200** | ghi sổ + bank nền | `WITHDRAW_PAYOUT` | tuỳ chọn | trừ/đóng băng trước 200 | Nhánh bank chạy nền |
| **Xem số dư** | `getWalletBalance` | — | — | — | Chỉ đọc | Không ghi sổ |

### 5.1 Nạp tiền — thứ tự bước

| # | Ai | Việc |
|---|-----|------|
| 1 | app-orchestration | Nhận notify → kiểm tra → **202** + `businessRef` |
| 2 | app-orchestration | Gửi `BANK_DEPOSIT` vào queue → worker ghi sổ PENDING |
| 3 | app-orchestration → accounting | Xác nhận → **POSTED** |
| 4 | Kafka (tuỳ chọn) | `JournalPosted` hoặc lệnh cộng ví |
| 5 | app-orchestration → wallet | `credit` USER — trùng `businessRef` thì bỏ qua |

### 5.2 Thanh toán — thứ tự bước

| # | Ai | Việc |
|---|-----|------|
| 1 | app-orchestration → wallet | Trừ USER |
| 2 | app-orchestration → accounting | Ghi sổ POSTED |
| 3 | app-orchestration → wallet | Cộng MERCHANT |
| 4 | app-orchestration | Trả **200** + `walletTxId` / `coaTransId` |

---

## 6. Kafka (event)

Chi tiết schema: [`asyncapi/core-events.yaml`](./asyncapi/core-events.yaml).

| Topic | Ai gửi | Ai nhận | Ảnh hưởng ví |
|-------|--------------|-------------|---------------|
| `core.bank.deposit-received` | app-orchestration | Worker accounting | — |
| `core.accounting.journal-posted` | accounting | app-orchestration / worker ví | Cộng ví khi `useCase=DEPOSIT` |
| `core.wallet.credit-command` | app-orchestration | Worker ví | `wallet_tx` CREDIT |
| `core.wallet.credited` | wallet | Thông báo, báo cáo | — |
| `core.operations.command-failed` | Worker | Ops | — |

Khóa phân vùng: `businessRef` (nạp/thanh toán) hoặc `memberId`.

---

## 7. RabbitMQ (lệnh worker)

Chi tiết schema: [`asyncapi/core-commands.yaml`](./asyncapi/core-commands.yaml).

**ADR:** [ADR-041](../adr/ADR-041-rabbitmq-orch-to-accounting-worker.md) (orch → accounting qua `BANK_DEPOSIT`) · [ADR-013](../adr/ADR-013-outbox-at-least-once-messaging.md) · [ADR-035](../adr/ADR-035-rabbitmq-workers-not-temporal-v1.md) · [ADR-005](../adr/ADR-005-idempotency-key-strategy.md)

**Hai kênh:** RabbitMQ = gửi lệnh cho worker (point-to-point). Kafka = phát event cho nhiều bên nghe. Có thể dùng cả hai trong cùng một luồng.

**Orch → accounting (nạp tiền):** app-orchestration **publish** `BANK_DEPOSIT`; **accounting worker** consume và gọi domain ghi sổ — không phải HTTP thẳng từ orch sang pod accounting trên luồng này.

Exchange: `core.commands` (topic, durable).

| Queue / routing key | `commandType` | Ai gửi | Ai nhận | Kết quả |
|-------------------|---------------|--------------|-------------|--------|
| `core.commands.bank-deposit` | `BANK_DEPOSIT` | app-orchestration (sau **202**) | Worker accounting | Sổ PENDING → POSTED |
| `core.commands.wallet-credit` | `WALLET_CREDIT` | app-orchestration | Worker ví | `wallet_tx` CREDIT |
| `core.commands.withdraw-payout` | `WITHDRAW_PAYOUT` | app-orchestration (sau trừ ví) | Adapter bank | Chuyển khoản nền |

Routing key khi publish: `{commandType}.{memberId}` chữ thường, vd. `bank_deposit.100234`. Chống trùng = `businessRef` trong body (không chỉ header).

### 7.1 Cấu trúc message (`CommandEnvelope`)

| Field | Bắt buộc | Ghi chú |
|-------|----------|---------|
| `messageId` | Có | UUID v4 mỗi lần publish |
| `businessRef` | Có | Khóa chống trùng — trùng với `X-Idempotency-Key` HTTP |
| `memberId` | Có | Mã thành viên |
| `commandType` | Có | `BANK_DEPOSIT` \| `WALLET_CREDIT` \| `WITHDRAW_PAYOUT` |
| `occurredAt` | Có | ISO-8601 UTC |
| `schemaVersion` | Có | vd. `"1.0"` |
| `payload` | Có | Dữ liệu lệnh — cùng tên field với OpenAPI / Kafka |
| `correlationId` | Không | Truy vết HTTP → queue → Kafka |
| `causationId` | Không | `messageId` của bước trước |
| `source` | Không | Service gửi |

`businessRef` nằm ngoài `payload`, không nhét trong payload.

### 7.2 Worker phải tuân

| # | Quy tắc |
|---|---------|
| C1 | Trùng `(commandType, businessRef)` + cùng nghĩa → ACK, không xử lý lại |
| C2 | ACK sau khi commit DB (hoặc replay an toàn) |
| C3 | Lỗi tạm → NACK + requeue; lỗi poison → DLQ `core.commands.dlq` |
| C4 | Nạp thành công → có thể bắn Kafka `JournalPosted`; cộng ví theo §5.1 |
| C5 | Log `messageId`, `businessRef`, `memberId`, `correlationId` |

### 7.3 Ví dụ — nạp tiền sau webhook **202**

```json
{
  "messageId": "550e8400-e29b-41d4-a716-446655440000",
  "businessRef": "dep-20260605-001",
  "memberId": 100234,
  "commandType": "BANK_DEPOSIT",
  "occurredAt": "2026-06-05T10:15:30Z",
  "correlationId": "trace-abc123",
  "source": "orchestration",
  "schemaVersion": "1.0",
  "payload": {
    "amount": "500000.0000",
    "vaNumber": "9704188888888",
    "bankCode": "VCB",
    "receivedAt": "2026-06-05T10:15:28Z"
  }
}
```

---

## 8. Bảng API HTTP

### 8.1 app-orchestration (paymentorches gọi vào)

Đường dẫn đầy đủ: [`openapi/gtelpay-core-internal.yaml`](./openapi/gtelpay-core-internal.yaml).

| Path | `operationId` | Trả về | Ví? |
|------|-----------------|--------|-----|
| `GET /v1/wallets/balance` | `getWalletBalance` | 200 | Chỉ đọc |
| `POST /v1/payments` | `createPayment` | 200 | Trừ + cộng |
| `POST /v1/transfers` | `createTransfer` | 200 | Trừ + cộng |
| `POST /v1/withdrawals` | `createWithdrawal` | 200 | Trừ/đóng băng |
| `POST /v1/deposits/notify` | `notifyDeposit` | 202 | Sau worker |
| `GET /v1/deposits/{businessRef}/status` | `getDepositStatus` | 200 | Hỏi trạng thái |

### 8.2 paymentorches (API công khai)

Đường dẫn đầy đủ: [`openapi/gtelpay-public.yaml`](./openapi/gtelpay-public.yaml). Thêm pocket, `bankWebhook`, auth kênh — **không** code trong app-orchestration.

API ghi sổ nội bộ **không** mở ra Gateway công khai.

---

## 9. Khóa chống trùng & liên kết log

Sơ đồ: [`correlation-id-map.md`](./correlation-id-map.md).

| Khóa | Dùng ở đâu | Quy tắc |
|------|------------|---------|
| `businessRef` | Header HTTP, sổ, queue, `wallet_tx` | Cùng ref + cùng nghĩa → trả kết quả cũ, không xử lý lại |
| `memberId` | Body HTTP, queue, lệnh ví | Định tuyến theo user — không phải khóa chống trùng thứ hai |
| `messageId` | Envelope queue | Mỗi lần publish một ID — khác `businessRef` |
| `coaTransId` | Response sổ, event, `wallet_tx` | Chỉ để liên kết — **không** FK DB ví → sổ |
| `walletTxId` | Response HTTP, event | ID dòng biến động ví |
| `correlationId` | Envelope queue (tuỳ chọn) | Truy vết xuyên HTTP → queue → Kafka |

---

## 10. Đồng bộ hay chạy nền

| Luồng | Client chờ? | Cách chạy |
|-------|-------------|-----------|
| Thanh toán / chuyển | Có → **200** | Một request HTTP: ví + sổ trong app-orchestration |
| Webhook nạp tiền | Không (nên) → **202** | Trả nhanh; worker queue → (tuỳ chọn) event → cộng ví |
| Rút tiền | Có (nhánh ví) | **200** sau trừ/đóng băng; bank chạy nền qua queue |
| `postJournal` | — | Luôn một transaction trong accounting |

---

## 11. Cấm

| # | Quy tắc |
|---|---------|
| F1 | Đối tác/Gateway gọi thẳng API sổ hoặc INSERT `coa_*` / `wallet_*` |
| F2 | `core.wallet` import code accounting |
| F3 | `core.accounting` sửa `wallet_balance` |
| F4 | Cùng lệnh mà đặt tên field khác nhau giữa HTTP / Kafka / queue |
| F5 | UPDATE `wallet_tx` / `coa_trans_data` đã chốt — chỉ thêm dòng mới |
| F6 | Chống trùng chỉ trong header AMQP — phải có `businessRef` trong body |

---

## 12. Tài liệu liên quan

| Cần biết | File |
|----------|------|
| Bảng ví, credit/debit? | [`trd/wallet.md`](./trd/wallet.md) |
| Ranh giới wallet ↔ accounting? | [`foundation.md`](./foundation.md) Part I §3 |
| Nạp tiền, transit 3100? | [`foundation.md`](./foundation.md) Part II §8 |
| Yêu cầu accounting? | [`trd/accounting.md`](./trd/accounting.md) |
| Lint OpenAPI? | [`openapi/README.md`](./openapi/README.md) |
| Envelope RabbitMQ? | [`asyncapi/core-commands.yaml`](./asyncapi/core-commands.yaml) · §7 |
| Ngân hàng / NAPAS / NĐ 52? | [`trd/references/banking/`](./trd/references/banking/) · §13 |
| Sổ bất biến? | [ADR-001](./adr/ADR-001-immutable-ledger.md) |
| Tất cả ADR? | [`adr/README.md`](./adr/README.md) |

---

## 13. Tài liệu tham khảo ngân hàng

**Chỉ tài liệu ngân hàng / thanh toán / bank adapter** — deposit webhook, withdraw/IBFT async, payment rails, quy định VN. File đã tải: [`trd/references/banking/`](./trd/references/banking/) (**42 file**). Ví → [`trd/references/wallet/`](./trd/references/wallet/) · Kế toán → [`trd/references/accounting/`](./trd/references/accounting/).

### 13.1 Pháp luật & quy định VN

| Nguồn | File local |
|-------|------------|
| **NĐ 52/2024/NĐ-CP** — thanh toán không dùng tiền mặt (VN) | [`thuvienphapluat-nghi-dinh-52-2024-vn.md`](./trd/references/banking/thuvienphapluat-nghi-dinh-52-2024-vn.md) |
| NĐ 52/2024 (EN) | [`luatvietnam-decree-52-2024-en.md`](./trd/references/banking/luatvietnam-decree-52-2024-en.md) |
| Hướng dẫn NĐ 52 | [`thuvienphapluat-huong-dan-nd52.md`](./trd/references/banking/thuvienphapluat-huong-dan-nd52.md) |
| Baker McKenzie — summary NĐ 52 | [`bakermckenzie-decree-52-summary.md`](./trd/references/banking/bakermckenzie-decree-52-summary.md) |
| Tilleke — summary NĐ 52 | [`tilleke-decree-52-summary.md`](./trd/references/banking/tilleke-decree-52-summary.md) |
| **Luật TCTD 32/2024/QH15** (VN) | [`thuvienphapluat-luat-tctd-2024-vn.md`](./trd/references/banking/thuvienphapluat-luat-tctd-2024-vn.md) |
| Luật TCTD sửa 2025 (VN) | [`thuvienphapluat-luat-tctd-sua-2025-vn.md`](./trd/references/banking/thuvienphapluat-luat-tctd-sua-2025-vn.md) |
| TT 40/2024 TT-NHNN highlights (intermediary payment) | [`vision-associates-circular-40-highlights.md`](./trd/references/banking/vision-associates-circular-40-highlights.md) |
| TT 40/2024 (EN, banking copy) | [`thuvienphapluat-circular-40-2024-en-banking.md`](./trd/references/banking/thuvienphapluat-circular-40-2024-en-banking.md) |

Map: bank webhook / S6 payout → §4–§7 · transit **3100**/**3400** → [`foundation.md`](./foundation.md) Part II §8–11.

### 12.2 Hạ tầng thanh toán Việt Nam

| Nguồn | File local |
|-------|------------|
| NAPAS — Digital Payment Handbook | [`napas-digital-payment-handbook.md`](./trd/references/banking/napas-digital-payment-handbook.md) |
| Lightspark — Instant payments Vietnam | [`lightspark-instant-payments-vietnam.md`](./trd/references/banking/lightspark-instant-payments-vietnam.md) |
| Norbr — Payment methods in Vietnam | [`norbr-payment-methods-vietnam.md`](./trd/references/banking/norbr-payment-methods-vietnam.md) |
| HSBC — NAPAS 247 instant transfer | [`hsbc-napas247.md`](./trd/references/banking/hsbc-napas247.md) |
| Indovina — NAPAS IBFT | [`indovina-napas-ibft.md`](./trd/references/banking/indovina-napas-ibft.md) |

### 12.3 Payment rails, orchestration, bank ops

| Nguồn | File local |
|-------|------------|
| SDK.finance — Payment rails | [`sdk-finance-payment-rails.md`](./trd/references/banking/sdk-finance-payment-rails.md) |
| SDK.finance — Payment orchestration vs PSP | [`sdk-finance-payment-orchestration.md`](./trd/references/banking/sdk-finance-payment-orchestration.md) |
| Stripe — Payment orchestration | [`stripe-payment-orchestration.md`](./trd/references/banking/stripe-payment-orchestration.md) |
| Payrails — Payment orchestration | [`payrails-payment-orchestration.md`](./trd/references/banking/payrails-payment-orchestration.md) |
| NetSuite — Payment orchestration | [`netsuite-payment-orchestration.md`](./trd/references/banking/netsuite-payment-orchestration.md) |
| Solidgate — Orchestration platforms | [`solidgate-payment-orchestration.md`](./trd/references/banking/solidgate-payment-orchestration.md) |
| Modern Treasury — Working with banks | [`mt-working-with-banks.md`](./trd/references/banking/mt-working-with-banks.md) |
| Modern Treasury — Products / Payments | [`mt-products-payments.md`](./trd/references/banking/mt-products-payments.md) |
| Modern Treasury — Instant payment statuses | [`mt-instant-payments-statuses.md`](./trd/references/banking/mt-instant-payments-statuses.md) |
| Modern Treasury — Balance reconciliation | [`mt-balance-reconciliation.md`](./trd/references/banking/mt-balance-reconciliation.md) |
| Modern Treasury — Real-time payments | [`mt-real-time-payments.md`](./trd/references/banking/mt-real-time-payments.md) |
| Modern Treasury — Payment orders (docs) | [`mt-docs-payment-orders.md`](./trd/references/banking/mt-docs-payment-orders.md) |
| The Clearing House — RTP | [`tch-rtp.md`](./trd/references/banking/tch-rtp.md) |

### 12.4 Core banking & BaaS

| Nguồn | File local |
|-------|------------|
| FinHost — Core banking architecture | [`finhost-core-banking.md`](./trd/references/banking/finhost-core-banking.md) |
| Cross River — Full-stack banking | [`crossriver-full-stack-banking.md`](./trd/references/banking/crossriver-full-stack-banking.md) |
| Wharton — BaaS primer | [`wharton-baas-primer.md`](./trd/references/banking/wharton-baas-primer.md) |
| Flagright — BaaS | [`flagright-baas.md`](./trd/references/banking/flagright-baas.md) |

### 12.5 Open banking

| Nguồn | File local |
|-------|------------|
| Kong — Open banking guide | [`kong-open-banking-guide.md`](./trd/references/banking/kong-open-banking-guide.md) |
| Innovate Finance — Open banking architecture | [`innovatefinance-open-banking-architecture.md`](./trd/references/banking/innovatefinance-open-banking-architecture.md) |
| Plaid — Open banking | [`plaid-open-banking.md`](./trd/references/banking/plaid-open-banking.md) |
| Prove — PSD2 / open banking | [`prove-psd2-open-banking.md`](./trd/references/banking/prove-psd2-open-banking.md) |

### 12.6 Correspondent & cross-border

| Nguồn | File local |
|-------|------------|
| Thunes — Correspondent banking | [`thunes-correspondent-banking.md`](./trd/references/banking/thunes-correspondent-banking.md) |
| OpenDue — Correspondent banking | [`opendue-correspondent-banking.md`](./trd/references/banking/opendue-correspondent-banking.md) |
| Xflow — Nostro / Vostro | [`xflow-nostro-vostro.md`](./trd/references/banking/xflow-nostro-vostro.md) |
| TradeEconomics — NĐ 52 summary | [`tradeeconomics-decree-52.md`](./trd/references/banking/tradeeconomics-decree-52.md) |
| DashDevs — Digital wallet (banking layer) | [`dashdevs-digital-wallet-banking.md`](./trd/references/banking/dashdevs-digital-wallet-banking.md) |
