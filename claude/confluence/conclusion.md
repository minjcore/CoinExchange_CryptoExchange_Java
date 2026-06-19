# Kết Luận — Tổng Kết Kiến Trúc GtelPay Core

> **CF page ID:** 51609737 | **Parent:** Data Plane Architecture (43581441)
> **Source of truth:** this file → push to CF
> **Reflects:** Constitution v1.0.0 (ratified 2026-06-15)

---

## 1. Ba quyết định nền tảng

| Quyết định | Nội dung | Hệ quả |
|-----------|---------|--------|
| **Hai domain tách biệt** | `core.wallet` và `core.accounting` không gọi nhau, không JOIN cross-schema | Độc lập scale và deploy; lỗi ở một domain không kéo domain kia |
| **Orchestration là sole sequencer** | `app-orchestration` điều phối flow qua HTTP (sync) hoặc outbox/queue (async) | Không 2PC, không distributed transaction; failure mode rõ ràng |
| **TigerBeetle cho hot-posting** | Chỉ `core.accounting` mở TB client; balance O(1); pending/post/void native | Ledger 1M+ TPS; deposit freeze/settle atomic |

---

## 2. Luồng deposit — minh họa toàn bộ kiến trúc

```
Bank webhook
    │ HTTP POST /deposits/notify → 202 (<200ms)
    ▼
app-orchestration ──[outbox + 202]────── Principle V
    │ RabbitMQ BANK_DEPOSIT
    ▼
app-accounting-worker
    ├── Phase A: TB pending(1111 DR / 3100 CR)         Principle II
    └── Phase B: TB post_pending + (3100→2110+4110)    ADR-037
         │ validate: account[3100] = 0                  INV-03
         │ RabbitMQ WALLET_CREDIT
         ▼
app-wallet-worker
    └── wallet_balance.available += net                 Principle III
         wallet_tx INSERT (DEPOSIT_CREDIT, businessRef) Principle IV
```

---

## 3. Bảy nguyên tắc — recap

| # | Nguyên tắc | Một câu tóm tắt |
|---|-----------|----------------|
| I | Two-Domain Separation | Wallet và accounting không chạm nhau — orchestration là cầu nối duy nhất |
| II | Immutable Balanced Ledger | POSTED lines chỉ append; sai → reversing journal mới |
| III | Wallet Hot Path | `wallet_balance` = one-row snapshot; mỗi thay đổi = một `wallet_tx` |
| IV | Idempotency End-to-End | Cùng `businessRef` → cùng outcome, dù deliver bao nhiêu lần |
| V | Orchestration Sole Sequencer | Outbox + at-least-once; không 2PC |
| VI | Money & Currency Discipline | VND v1; BigDecimal scale 4 HALF_UP; fee tính một lần tại orchestration |
| VII | Contracts & Conformance | OpenAPI/AsyncAPI = wire source of truth; SQL invariants trong CI |

---

## 4. Out of scope (v1)

- Multi-currency (cần ADR riêng)
- Bank/NAPAS integration detail (infrastructure concern)
- Tax, payroll, billing
- Cross-domain reporting JOIN (dùng read model / data warehouse)
- Blnk binary (chỉ hai pattern PoC: `WalletBalanceMonitor`, `getBalanceAt`)

---

## 5. Khi nào cần ADR mới

Bắt buộc viết ADR trước khi code nếu:
- Thêm/đổi cross-module dependency
- Đổi protocol tại service boundary (HTTP ↔ queue)
- Giới thiệu storage store mới
- Thay đổi money/currency/fee handling
- Sửa bất kỳ nguyên tắc Constitution nào
- Thêm TigerBeetle client vào module nào ngoài `core.accounting`

---

## 6. Điểm tiếp theo

**Developer mới:**
1. Architecture FAQ → Platform Boundaries → Business Process Deposit → Use Cases

**PR reviewer:**
1. Principle I vi phạm? (cross-domain dependency)
2. Protocol đúng chưa? (HTTP vs queue)
3. Fee recomputed ở worker? (không được)
4. TB client ngoài `core.accounting`? (không được)
5. POSTED row bị UPDATE? (không được)

---

## 7. Source of truth

Thiết kế kiến trúc được ghi thành văn bản. Confluence là mirror — khi conflict, **local source wins**.

| Artifact | Nơi lưu |
|----------|---------|
| Constitution (nguyên tắc) | `.specify/memory/constitution.md` |
| Architecture Decision Records | `adr/` |
| Domain TRDs & diagrams | `claude/confluence/` |
| OpenAPI / AsyncAPI | `specs/contracts/` |
