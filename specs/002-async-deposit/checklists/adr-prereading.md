# ADR Pre-Reading Checklist — Async Deposit (002)

> **CF page ID:** 51872153 | **Parent:** 📌 Start Here (51315064)
> **Source of truth:** this file → push to CF

**Purpose:** Đọc các ADRs này trước khi bắt đầu code. Mỗi ADR map đến một bước cụ thể trong implementation.
Tick khi đã đọc và hiểu acceptance criteria.

---

## Phase 1 — DB Schema & Money

- [ ] **ADR-003** — Dual schema (`wallet` / `accounting`), separate Flyway, separate DB roles. Hiểu trước khi viết DDL.
- [ ] **ADR-028** — `NUMERIC(19,4)`, `BigDecimal` scale 4 `HALF_UP`, wire = decimal string. Áp dụng cho mọi amount column.
- [ ] **ADR-019** — VND only v1, reject non-VND trước mọi mutation. Áp dụng validate tại orchestration boundary.

---

## Phase 2 — Wallet Domain

- [ ] **ADR-004** — `wallet_balance` = snapshot, 1:1 với `wallet_tx`. W2: một tx per balance change, cùng DB transaction. Đọc trước khi viết `WalletService`.
- [ ] **ADR-005** — Idempotency triple `(wallet_id, business_ref, tx_type)` UNIQUE. Conflicting amount → 409. Đọc trước khi implement `creditWallet` / `debitWallet`.
- [ ] **ADR-029** — LOCKED wallet reject debit/freeze. Settle/release vẫn allowed.
- [ ] **ADR-034** — LOCKED wallet cũng reject `DEPOSIT_CREDIT`. Saga chuyển `AWAITING_WALLET`.

---

## Phase 3 — Accounting Domain & TigerBeetle

- [ ] **ADR-006** — Two-phase deposit: Phase A `PENDING` (1111 DR + 3100 CR), Phase B `POSTED` via `confirmDeposit` only. **Quan trọng nhất cho accounting worker.**
- [ ] **ADR-037** — TigerBeetle là backing store cho hot postings. Mapping: `PENDING` → `flags.pending`; `confirmDeposit` → `post_pending_transfer`. Đọc trước khi implement `JournalService`.
- [ ] **ADR-010** — Transit 3100 phải net = 0 sau Phase B. `postJournal` reject nếu non-zero. Validate trước khi publish `WALLET_CREDIT`.
- [ ] **ADR-001** — POSTED journal immutable. `coa_trans_data` không được UPDATE/DELETE. Đọc trước khi implement `confirmDeposit`.

---

## Phase 4 — Async Wiring (Outbox → RabbitMQ)

- [ ] **ADR-013** — Transactional outbox + at-least-once. Consumer idempotent trên `(commandType, businessRef)`. Poison → DLQ.
- [ ] **ADR-041** — `app-orchestration` publish `BANK_DEPOSIT` → `core.commands` exchange. Accounting worker consume → Phase A/B. Worker không credit wallet.
- [ ] **ADR-035** — RabbitMQ workers + DB saga state, không Temporal. Saga resume = redelivery + read DB state.

---

## Phase 5 — HTTP Endpoint & Auth

- [ ] **ADR-011** — `memberId` = JWT `sub`. Bank webhook = mTLS, không JWT. Không trust body alone.
- [ ] **ADR-022** — mTLS cho bank webhook. Invalid/missing cert → 401, không enqueue.
- [ ] **ADR-012** — Forbidden rules: không cross-import, không cross-write, `businessRef` trong body không phải header.

---

## Phase 6 — Wallet Credit Path

- [ ] **ADR-024** — 3 paths cho wallet credit sau POSTED: Kafka event / RabbitMQ `WALLET_CREDIT` / HTTP sync. **Current spec = Path B (RabbitMQ).**
- [ ] **ADR-026** — Wallet không bao giờ reverse accounting. POSTED + credit fail → retry credit, không reverse journal.

---

## Phase 7 — Idempotency End-to-End

- [ ] **ADR-005** *(re-check)* — `businessRef` chạy end-to-end: `X-Idempotency-Key` → outbox → `BANK_DEPOSIT` → `coa_trans.reference_id` → `WALLET_CREDIT` → `wallet_tx.business_ref`. Sub-keys `:settle` / `:release` cho IBFT.
- [ ] **ADR-030** — VA → `memberId` lookup tại orchestration. Unknown VA → hold PENDING, không credit 2110 sai.

---

## Phase 8 — Aging Jobs & Monitoring

- [ ] **ADR-021** — Aging job cho mọi async pending state. Retry-safe vì ADR-005. Không có state nào stuck forever.
- [ ] **ADR-033** — `bank.poll.interval_seconds` (T2), `wallet.frozen.alert_age_hours` (Tmax). Không auto-release khi UNKNOWN.
- [ ] **ADR-032** — MON-05: POSTED deposit > 15m không có `wallet_tx` → alert. Out-of-band, không block payment path.

---

## Phase 9 — CI Invariants

- [ ] **ADR-031** — INV-01 (DR=CR), INV-02 (no dup business_ref POSTED), INV-03 (transit 3100=0), INV-04 (no UPDATE/DELETE POSTED), INV-05 (W2 spot-check). Fail → build fail.

---

## ADRs có thể đọc sau (không block MVP)

| ADR | Khi nào cần |
|-----|------------|
| ADR-023 | Implement period close feature |
| ADR-015 | Implement EOD settlement batch |
| ADR-036 | Cần giải thích accrual basis cho stakeholder |
| ADR-039 | Nếu có proposal thêm aggregate row vào wallet |
| ADR-040 | Implement multi-pocket USER wallets |
| ADR-038 | Swap `InProcessGateway` → HTTP client |

---

## Retry-safe reminder (đọc trước khi viết bất kỳ write nào)

Mọi write phải trả ra **existing IDs** khi replay với cùng data — không chỉ status flag:

| Step | UNIQUE key | Returns on replay |
|------|-----------|-------------------|
| S1 `POST /deposits/notify` | `outbox.business_ref` | 202 + same `businessRef` |
| `BANK_DEPOSIT` consumer | `coa_trans(reference_id, use_case)` | existing `coaTransId` |
| `confirmDeposit` | `coa_trans.status = POSTED` | existing `coaTransId` |
| `WALLET_CREDIT` consumer | `wallet_tx(wallet_id, business_ref, tx_type)` | existing `walletTxId` |
