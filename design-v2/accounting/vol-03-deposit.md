# Quyển III — Nạp tiền (Deposit): kế toán hai pha, transit 3100

**Trạng thái:** Bản mở rộng (~100 trang in) · **Phạm vi:** `core.accounting` — luồng DEPOSIT only · **Ngôn ngữ:** Tiếng Việt
**Transit:** 3100 · **ADR chính:** 006, 010, 005, 001, 036, 030, 024, 013, 022, 021, 023, 034, 031, 009
**Async:** HTTP 202 + worker RabbitMQ S6 · **Bank rail:** Vietinbank VA → 1111 · **Napas:** N/A cho deposit

**Số tiền ví dụ chuẩn:** gross **100.000** VND · phí nạp **1.000** VND · net credit ví **99.000** VND · Napas **N/A** · scale **4** HALF_UP ([ADR-028](../../adr/ADR-028-money-scale-four-half-up.md))

Template DR/CR authoritative: [`foundation.md`](../../spec/foundation.md) §8 — **link, không duplicate nguyên văn**. COA deep dive: [`vol-02-coa-handbook.md`](./vol-02-coa-handbook.md) TK 1111, 3100, 2110, 4110. Nguyên tắc: [`vol-01-principles.md`](./vol-01-principles.md). Contract: [`accounting.md`](../accounting.md) §14. Acceptance: [`acceptance.md`](../acceptance.md). Saga: [`processes.md`](../../spec/processes.md) §3 · Flow: [`flows.md`](../../design/orchestration/flows.md).

---
## Mục lục chi tiết

### Phần A — Dẫn nhập & nghiệp vụ

| § | Tiêu đề |
|---|---------|
| [0](#chương-0-cách-đọc-và-quan-hệ-với-quyển-ii) | Cách đọc & quan hệ Quyển I/II |
| [1](#chương-1-tóm-tắt-nghiệp-vụ--actors) | Tóm tắt nghiệp vụ & actors |
| [2](#chương-2-accrual-vs-cash--deposit-only-adr-036) | Accrual vs cash — deposit only (ADR-036) |

### Phần B — Hai pha kế toán

| § | Tiêu đề |
|---|---------|
| [3](#chương-3-pha-a-pending--deep-dive) | Pha A PENDING — deep dive |
| [4](#chương-4-pha-b-posted--deep-dive) | Pha B POSTED — deep dive |
| [5](#chương-5-va-mapping-adr-030) | VA mapping (ADR-030) |
| [6](#chương-6-wallet-handoff--journalposted-vs-wallet_credit-adr-024) | Wallet handoff (ADR-024) |

### Phần C — Lỗi, đảo, idempotency

| § | Tiêu đề |
|---|---------|
| [7](#chương-7-ma-trận-lỗi-d-f1d-f15) | Ma trận lỗi D-F1..D-F15 |
| [8](#chương-8-reversal--bank-recall-adr-001) | Reversal & bank recall (ADR-001) |
| [9](#chương-9-idempotency-end-to-end-adr-005) | Idempotency end-to-end (ADR-005) |

### Phần D — Hạ tầng & vận hành

| § | Tiêu đề |
|---|---------|
| [10](#chương-10-messaging-s6-s3-outbox-adr-013) | Messaging S6, S3, outbox (ADR-013) |
| [11](#chương-11-security-mtls-webhook-adr-022) | Security mTLS webhook (ADR-022) |
| [12](#chương-12-aging-pending-adr-021) | Aging PENDING (ADR-021) |
| [13](#chương-13-period-close-adr-023) | Period close (ADR-023) |
| [14](#chương-14-locked-wallet-deposit-credit-adr-034) | LOCKED wallet (ADR-034) |
| [15](#chương-15-reconciliation-fr-10-w5-4110) | Reconciliation FR-10, W5, 4110 |

### Phần E — Diagram, API, CI, catalog

| § | Tiêu đề |
|---|---------|
| [16](#chương-16-state-machine--timeline-diagrams) | State machine & timeline |
| [17](#chương-17-s2-api-deposit-operations-field-guide) | S2 API field guide |
| [18](#chương-18-sql-invariant-ci-adr-031) | SQL invariant CI (ADR-031) |
| [19](#chương-19-gherkin-catalog--33-scenarios) | Gherkin catalog 33 scenarios |
| [20](#chương-20-ops-runbook-outline) | Ops runbook outline |
| [21](#chương-21-faq-deposit--40-câu) | FAQ 40 câu |
| [22](#chương-22-review-checklist--30-mục) | Review checklist 30 mục |

### Phụ lục

| § | Nội dung |
|---|----------|
| [A](#phụ-lục-a--ma-trận-kịch-bản-số) | Ma trận kịch bản số |
| [B](#phụ-lục-b--dr-cr-line-by-line-mọi-biến-thể) | DR/CR line-by-line |
| [C](#phụ-lục-c--bảng-tra-adr-deposit) | Bảng tra ADR |
| [Đọc tiếp](#đọc-tiếp) | Liên kết corpus |
| [I](#phụ-lục-i--accounting-md-14) | Mapping accounting.md §14 |

---
## Chương 0. Cách đọc và quan hệ với Quyển II {#chương-0-cách-đọc-và-quan-hệ-với-quyển-ii}

Quyển III là **deep dive nghiệp vụ nạp tiền** — bổ sung Quyển I (nguyên tắc accrual, transit, immutability) và Quyển II (handbook TK 1111/3100/2110/4110) bằng **end-to-end lifecycle** deposit: webhook → PENDING → POSTED → wallet credit → recon.

| Vai trò | Đọc trước | Kết quả |
|---------|-----------|---------|
| Product | Ch0 + Ch1 + [TBD] | Gap fee tier, VA multi, aging T |
| Backend accounting | Ch3–4 + Ch7 + Ch17–18 | `confirmDeposit`, transit validate |
| Backend orchestration | Ch5–6 + Ch9–10 | S6/S3, idempotency |
| Ops / Kế toán | Ch7–8 + Ch12–15 + Ch20 | PENDING aging, recall, W5 |
| QA | Ch19 | 33 Gherkin scenarios |
| Audit | Ch2 + Ch8 + Phụ lục B | DR/CR evidence |

### 0.1 Quan hệ Quyển I ↔ Quyển II ↔ Quyển III

```
Quyển I (nguyên tắc)     Quyển II (COA/TK)        Quyển III (deposit)
────────────────────     ─────────────────        ───────────────────
ADR-036 accrual    ──►   TK 2110           ──►   Ch2: không credit webhook
ADR-006 two-phase  ──►   TK 3100           ──►   Ch3–4: phase A/B
ADR-010 transit=0  ──►   TK 3100 terminal  ──►   Ch4: confirmDeposit
Ch10 revenue       ──►   TK 4110           ──►   Ch4: fee variants
Ch8 reversal       ──►   mọi TK            ──►   Ch8: bank recall
```

### 0.2 Quy ước

| Ký hiệu | Ý nghĩa |
|---------|---------|
| gross / fee / net | 100.000 / 1.000 / 99.000 (chuẩn) |
| `business_ref` | Bank txn id — [ADR-005](../../adr/ADR-005-idempotency-key-strategy.md) |
| [TBD: ...] | Product gap — không implement ngầm |

### 0.3 Onboarding 10 câu

| # | Câu | Đáp |
|---|---|-----|
| Q1 | Credit wallet khi webhook? | Không — PENDING [ADR-006](../../adr/ADR-006-two-phase-deposit.md) |
| Q2 | 3100 ≠ 0 lỗi? | Chỉ POSTED; PENDING OK [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md) |
| Q3 | Ai tính fee? | Orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md) |
| Q4 | Phase B qua addLines? | Không — confirmDeposit (AC-006-03) |
| Q5 | Sửa POSTED? | Reversal [ADR-001](../../adr/ADR-001-immutable-ledger.md) |
| Q6 | VA unmapped? | PENDING — D-F2 |
| Q7 | LOCKED wallet? | Credit reject [ADR-034](../../adr/ADR-034-locked-wallet-deposit-credit-reject.md) |
| Q8 | Duplicate webhook? | Idempotent — D-F1 |
| Q9 | Cash basis? | Rejected [ADR-036](../../adr/ADR-036-accrual-basis-ledger-v1.md) |
| Q10 | Period close? | Block B [ADR-023](../../adr/ADR-023-accounting-period-close.md) |

---
## Chương 1. Tóm tắt nghiệp vụ & actors {#chương-1-tóm-tắt-nghiệp-vụ--actors}

### 1.1 Mục tiêu nghiệp vụ

Member chuyển khoản vào **Virtual Account (VA)** Vietinbank được cấp cho user. Platform:

1. Ghi nhận tiền vào **1111** (tài sản ngân hàng) qua webhook async.
2. Treo gross trên **3100** (transit deposit) trong trạng thái **PENDING**.
3. Sau khi map VA → member và confirm amount, **POSTED** phase B: clear 3100, ghi **2110** net + **4110** fee.
4. Orchestration credit ví USER **net** — wallet không tự trừ phí.

**Ví dụ chuẩn:** gross **100.000** · fee **1.000** · net **99.000** · Napas **N/A**.

### 1.2 Bảng quyết định kiến trúc

| Khía cạnh | Quyết định | ADR |
|-----------|------------|-----|
| Pha kế toán | Hai pha PENDING (A) → POSTED (B) | [006](../../adr/ADR-006-two-phase-deposit.md) |
| Transit | 3100 giữ gross đến confirm | [010](../../adr/ADR-010-transit-accounts-net-zero.md) |
| Wallet credit | Sau POSTED only — không phụ thuộc wallet để POSTED | [006](../../adr/ADR-006-two-phase-deposit.md), [024](../../adr/ADR-024-deposit-wallet-credit-dual-path.md) |
| HTTP S1 | 202 Accepted — async worker | [006](../../adr/ADR-006-two-phase-deposit.md) |
| Bank rail | Vietinbank webhook → 1111 | [022](../../adr/ADR-022-mtls-bank-webhooks.md) |
| Fee | Orchestration tính; user bears (netted) | [009](../../adr/ADR-009-fee-ownership-orchestration.md) |
| Recognition | Accrual-like — POSTED = 2110 | [036](../../adr/ADR-036-accrual-basis-ledger-v1.md) |
| Immutability | POSTED lines không UPDATE | [001](../../adr/ADR-001-immutable-ledger.md) |
| Idempotency | `business_ref` = bank txn id | [005](../../adr/ADR-005-idempotency-key-strategy.md) |

### 1.3 Actors & ranh giới trách nhiệm

| Actor | Trách nhiệm deposit | Không làm |
|-------|---------------------|-----------|
| **Member** | CK vào VA đúng số tiền | Không gọi accounting API |
| **Vietinbank** | Webhook amount + VA + bank ref | Không map memberId |
| **Gateway S1** | mTLS terminate, 202 ack | Không post journal |
| **Orchestration** | Map VA→memberId; tính fee; enqueue S6; trigger phase B; WALLET_CREDIT | Không JOIN wallet+accounting SQL |
| **Worker S6** | Consume `BANK_DEPOSIT`; gọi accounting | Không tính fee từ rule riêng |
| **core.accounting** | `coa_trans` PENDING/POSTED; `confirmDeposit`; emit JournalPosted | Không biết `wallet_balance` |
| **core.wallet** | `DEPOSIT_CREDIT` net sau POSTED | Không post 2110 |
| **Ops** | PENDING aging; VA unmapped; recall reversal approve | Không UPDATE `coa_trans_data` |
| **Recon job** | FR-10 bank; W5 drift report | Không auto-fix COA [014](../../adr/ADR-014-reconciliation-w5-report-only.md) |

### 1.4 Luồng tóm tắt (ASCII)

```
Member ──CK gross──► Vietinbank VA
                         │
                         ▼ webhook (mTLS)
                    Gateway S1 ──202──► Orchestration
                         │                  │
                         │                  ├── map VA → memberId
                         │                  └── enqueue S6 BANK_DEPOSIT
                         ▼
                    Worker ──phase A──► Accounting: 1111 DR / 3100 CR [PENDING]
                         │
                         ▼ confirm + fee
                    Worker ──phase B──► Accounting: confirmDeposit [POSTED]
                         │
                         ├──► Kafka JournalPosted (S3)
                         └──► RabbitMQ WALLET_CREDIT (S6)
                                    │
                                    ▼
                              Wallet: available += net
```

Chi tiết step order: [`processes.md`](../../spec/processes.md) §3 · [`flows.md`](../../design/orchestration/flows.md).

### 1.5 So sánh deposit vs các luồng khác

| Luồng | Sync/Async | Transit | Wallet timing | Napas |
|-------|------------|---------|---------------|-------|
| **DEPOSIT** | Async 202 | 3100 two-phase | Credit **sau** POSTED | N/A |
| WITHDRAW | Async | 3200 | Freeze trước post | N/A (1111) |
| PAYMENT | Sync | 3500 | Debit/credit cùng TX | N/A |
| IBFT | Async | 3400 | Freeze + SETTLE | **1112** |

### 1.6 Trạng thái `coa_trans` — deposit lifecycle

| Status | 3100 net | 2110 | 4110 | 1111 | Wallet available |
|--------|----------|------|------|------|------------------|
| (none) | — | — | — | — | unchanged |
| PENDING | +gross (CR hold) | unchanged | unchanged | +gross | **unchanged** |
| POSTED | **0** | +net | +fee | +gross | +net (async) |
| FAILED | 0* | unchanged | unchanged | reversed* | unchanged |
| REVERSED | per reversal journal | reversed | reversed | reversed | orchestration debit |

*FAILED: reversal phase A only — 1111 CR + 3100 DR clear hold.

### 1.7 Invariants sau POSTED (foundation §8.6)

```
1111  += gross     (100.000)
2110  += net       (99.000)   — aggregate control, mirror wallet USER sum
4110  += fee       (1.000)
3100  =  0
wallet_balance(user) += net (99.000) — eventual, idempotent
sum(DR) = sum(CR) per coa_trans
```

### 1.8 Product gaps [TBD]

| ID | Câu hỏi mở | Ảnh hưởng |
|----|------------|-----------|
| TBD-FEE | Fee schedule %/min/max per merchant? | 4110 amount, orchestration config |
| TBD-VA | Một member nhiều VA active? | Map table, Ch5 |
| TBD-RECALL | SLA reversal tự động sau bank recall? | Ch8 ops |
| TBD-AGING | Ngưỡng PENDING T ngày/giờ? | Ch12 alert D-F8 |

### 1.9 Chi tiết actors — deposit path

| Actor | Input | Output | Không làm |
|-------|-------|--------|-----------|
| Member | Bank CK gross | — | Gọi API accounting |
| Vietinbank | Webhook | amount, VA, businessRef | Map memberId |
| Gateway S1 | mTLS webhook | 202 + ref | Post journal |
| Orchestration | VA map, fee calc | S6 commands, confirm trigger | JOIN SQL cross-schema |
| Worker S6 | BANK_DEPOSIT | S2 createJournal | Tính fee |
| Accounting | DR/CR lines | POSTED + JournalPosted | Wallet credit |
| Wallet | WALLET_CREDIT | available += net | Post 2110 |
| Ops | Manual cases | Map VA, approve reversal | UPDATE coa_trans_data |
| Recon | Nightly batch | FR-10/W5 alerts | Auto-fix COA |

### 1.10 Deposit trong ecosystem messaging

Luồng deposit chạm **S1** (webhook), **S6** (BANK_DEPOSIT, WALLET_CREDIT), **S3** (JournalPosted, CommandFailed), **S2** (createJournal, confirmDeposit). Chi tiết envelope: Ch10, Ch17B.

### 1.11 Invariants tóm tắt (cross-ref Quyển I)

1. `sum(DR)=sum(CR)` mọi POSTED
2. Transit 3100=0 tại POSTED terminal
3. `business_ref` idempotent end-to-end
4. POSTED immutable — reversal only
5. Wallet credit sau POSTED — net only
6. Accrual-like — không cash basis webhook
7. Orchestration owns fee — accounting records
8. W5 mismatch report-only
9. Period close blocks back-date
10. mTLS required bank webhook

---
## Chương 2. Accrual vs cash — deposit only (ADR-036) {#chương-2-accrual-vs-cash--deposit-only-adr-036}

[ADR-036](../../adr/ADR-036-accrual-basis-ledger-v1.md) quyết định platform ledger v1 dùng **accrual-like recognition** — không cash basis. Deposit là use case minh họa rõ nhất: bank cash vào trước, nghĩa vụ user (2110) chỉ ghi khi POSTED.

### 2.1 Bảng so sánh accrual-like vs cash basis (deposit)

| Sự kiện | Accrual-like v1 (chấp nhận) | Cash basis (từ chối v1) |
|---------|------------------------------|-------------------------|
| Webhook bank gross 100k | Phase A: 1111 DR, 3100 CR — wallet **0** | Credit wallet 100k ngay — **cấm** |
| POSTED phase B | 2110 CR 99k + 4110 CR 1k — wallet credit net | Không cần 2110? — **sai domain** |
| Bank settlement T+N | 1111 đã DR phase A; settlement async | N/A |
| PENDING 3100 | In-flight accrual state — visible ops | Coi như "đã có tiền user" — **double risk** |
| UI "deposit processing" | Saga/orchestration status — **không** wallet column | Hiển thị pending inflow — **reject** ACC-E05 |

### 2.2 Timeline accrual deposit

```
T0  Member CK 100k ─────────────────────────────────────────► Bank
T1  Webhook ──► Phase A PENDING
    Ledger: 1111 +100k, 3100 hold +100k
    Wallet: available = 0  ◄── accrual: chưa có quyền chi tiêu

T2  Map VA + confirm ──► Phase B POSTED
    Ledger: 3100=0, 2110 +99k, 4110 +1k
    Wallet: credit 99k (async)  ◄── accrual: nghĩa vụ ghi nhận

T3  Bank settlement file (T+N) ──► FR-10 recon 1111
    Không thay POSTED journal
```

### 2.3 Substance over form (Luật KT 2015 Điều 6)

- **Hình thức:** Webhook báo "tiền vào VA".
- **Bản chất phase A:** Platform nhận tiền ngân hàng (1111) nhưng **chưa xác định** member liability — treo 3100.
- **Bản chất phase B:** Xác định member → ghi nghĩa vụ 2110 net + doanh thu phí 4110.

Quyển I [Ch6 VAS vs platform](../vol-01-principles.md) — ledger platform không phải BCTC statutory đầy đủ nhưng tuân substance over form.

### 2.4 Matching fee (ADR-009 + ADR-036)

Fee 4110 **cùng journal POSTED** với movement 2110 — không orphan revenue journal sau. ACC-E04.

| Leg | TK | Amount | Phase |
|-----|-----|--------|-------|
| Net liability | 2110 CR | 99.000 | B |
| Fee presentation | 2110 DR | 1.000 | B |
| Revenue | 4110 CR | 1.000 | B |

### 2.5 Cấm cash-basis shortcuts (code review)

| Anti-pattern | Vi phạm | Detection |
|--------------|---------|-----------|
| `wallet.credit()` trong webhook handler | AC-036-04 | Static review S1 |
| 2110 CR khi status=PENDING | AC-036-01 | INV-03 + Gherkin |
| Skip 3100 "instant deposit" | ADR-006 | Architecture review |
| UI hiển thị pending inflow trong `available` | ACC-E05 | API contract |

### 2.6 Gherkin accrual deposit

| Scenario | Feature | Ý nghĩa |
|----------|---------|---------|
| ACC-E01 | Accrual basis | Webhook only → wallet 0 |
| ACC-E02 | Accrual basis | POSTED trước bank settlement OK |
| ACC-E04 | Accrual basis | Fee cùng POSTED journal |
| ACC-E05 | Accrual basis | UI deposit processing ≠ wallet column |

### 2.10 Bảng quyết định recognition — deposit only

| Câu hỏi thiết kế | Quyết định v1 | ADR |
|------------------|---------------|-----|
| Ghi 2110 khi webhook? | Không | 036 |
| Ghi 2110 khi POSTED? | Có — net | 006 |
| Ghi 4110 khi PENDING? | Không | 006 |
| Ghi 4110 khi POSTED fee>0? | Có | 009 |
| Credit wallet khi webhook? | Không | 006 |
| Credit wallet khi POSTED? | Có — net async | 024 |
| Hiển thị pending trên wallet.available? | Không | 036 ACC-E05 |
| Bank settlement file trước POSTED? | Không bắt buộc | ACC-E02 |

### 2.11 Đọc thêm accrual

Quyển I [Ch7 ADR-036](./vol-01-principles.md#chương-7-accrual-like-ledger-v1--adr-036) · [`references/netsuite-cash-vs-accrual.md`](../../references/netsuite-cash-vs-accrual.md) · [`references/investopedia-accrual-accounting.md`](../../references/investopedia-accrual-accounting.md)

---
## Chương 3. Pha A PENDING — deep dive {#chương-3-pha-a-pending--deep-dive}

Phase A ghi nhận **tiền bank đã vào** mà **chưa** ghi nghĩa vụ user. Template: [`foundation.md`](../../spec/foundation.md) §8.1 bước 1–2.

### 3.1 Kích hoạt phase A

| Trigger | Nguồn | Precondition |
|---------|-------|--------------|
| Bank webhook | S1 `notifyDeposit` → S6 `BANK_DEPOSIT` | mTLS valid [ADR-022](../../adr/ADR-022-mtls-bank-webhooks.md) |
| Payload | `amount=gross`, `vaNumber`, `businessRef=bankTxnId` | `businessRef` UNIQUE |
| Worker action | `createJournal(use_case=DEPOSIT, status=PENDING)` | Period open |

### 3.2 Bút toán phase A

| # | TK | DR | CR | Amount (chuẩn) |
|---|-----|----|----|----------------|
| 1 | 1111 | 100.000 | | gross |
| 2 | 3100 | | 100.000 | gross |

| `coa_trans` field | Giá trị |
|-------------------|---------|
| `use_case` | `DEPOSIT` |
| `status` | `PENDING` |
| `business_ref` | bank txn id (UNIQUE per use_case) |

### 3.3 T-account phase A (số chuẩn)

```
    1111 Vietinbank              |    3100 Transit Deposit
─────────────────────────────────┼──────────────────────────────
    DR 100.000 (webhook)         |              CR 100.000
    SD Nợ +100.000               |    SD Có +100.000 (HOLD)
                                 |
    2110 User Payable            |    4110 Fee Revenue
─────────────────────────────────┼──────────────────────────────
    (không đổi)                  |    (không đổi)
    Wallet available = 0         |
```

### 3.4 Observables sau phase A (checklist)

| # | Kiểm tra | Kỳ vọng | SQL hint |
|---|----------|---------|----------|
| O-A1 | `status` | `PENDING` | `coa_trans.status` |
| O-A2 | Line count | 2 | count `coa_trans_data` |
| O-A3 | Accounts present | 1111, 3100 only | account_code IN (...) |
| O-A4 | Balance | `sum(DR)=sum(CR)=gross` | per journal |
| O-A5 | Transit 3100 | CR gross — **≠ 0 OK** | net per journal |
| O-A6 | 2110 | unchanged | trial balance delta |
| O-A7 | 4110 | unchanged | no fee yet |
| O-A8 | Wallet | **unchanged** | W5 may lag — expected |
| O-A9 | `business_ref` | set, unique | UNIQUE constraint |
| O-A10 | Trial balance | 1111↑ 3100↑ — mirror hold | FR-10 |

### 3.5 Cấm tuyệt đối sau phase A

| # | Cấm | Lý do |
|---|-----|-------|
| F-A1 | Gọi wallet credit | ADR-006 AC-006-02 |
| F-A2 | Ghi 4110 phí | Revenue phase B only |
| F-A3 | Ghi 2110 CR net | Liability chưa xác định member |
| F-A4 | POSTED không qua confirmDeposit | AC-006-03 |
| F-A5 | JOIN wallet để "kiểm tra đủ tiền" | ADR-003 |
| F-A6 | UPDATE amount trên lines | ADR-001 |
| F-A7 | Skip 3100 — 1111 DR trực tiếp 2110 | Phá two-phase |

### 3.6 Mười ví dụ số phase A


| ID | Mô tả | gross | business_ref | Kết quả 3100 |
|----|-------|-------|--------------|--------------|
| EX-A01 | Happy chuẩn | 100,000 | `dep-1` | 1111 DR 100k, 3100 CR 100k |
| EX-A02 | Zero fee prep — gross 75k | 75,000 | `dep-zf` | PENDING hold 75k — fee chưa biết đến phase B |
| EX-A03 | Large gross 500M | 500,000,000 | `dep-lg` | 3100 CR 500M — scale 4 OK |
| EX-A04 | Min 1 unit | 1 | `dep-min` | 3100 CR 1 — DEP-E01 prep |
| EX-A05 | VA mapped member 100234 | 100,000 | `dep-map` | PENDING dù đã map — vẫn chưa POSTED |
| EX-A06 | VA unmapped | 100,000 | `dep-5` | PENDING hold — ops queue D-F2 |
| EX-A07 | Duplicate webhook replay | 100,000 | `dep-2` | Same coa_trans_id — không DR 1111 lần 2 |
| EX-A08 | Concurrent workers same ref | 100,000 | `dep-9` | Exactly one journal — DEP-E12 |
| EX-A09 | Partial TX rollback crash | 100,000 | `dep-crash` | No orphan lines — D-F11 |
| EX-A10 | Period open required | 100,000 | `dep-pc` | Reject nếu period closed at create |
| EX-A11 | Inactive TK 1111 | 100,000 | `dep-inact` | Reject addLines — D-F10 |
| EX-A12 | Webhook 50k variant | 50,000 | `dep-1a` | Acceptance Phase A only scenario |

### 3.7 Phase A — sequence chi tiết

```
Vietinbank          S1/Gateway       Orchestration       Worker(S6)       core.accounting
     |                   |                  |                  |                  |
     |-- webhook mTLS -->|                  |                  |                  |
     |                   |-- 202 + ref ---->|                  |                  |
     |                   |                  |-- BANK_DEPOSIT ->|                  |
     |                   |                  |                  |-- createJournal->|
     |                   |                  |                  |                  | PENDING
     |                   |                  |                  |                  | 1111 DR gross
     |                   |                  |                  |                  | 3100 CR gross
     |                   |                  |                  |<-- coaTransId --|
     |                   |                  |<-- ack ----------|                  |
```

### 3.8 Phase A vs bank statement timing

1111 DR phase A có thể **trước** dòng sao kê T+0/T+1. FR-10 recon dùng tolerance window — không reverse chỉ vì sao kê chưa có nếu webhook hợp lệ mTLS.

### 3.9 Failed transition từ phase A

| Situation | Next status | Accounting |
|-----------|-------------|------------|
| VA never mapped + SLA | FAILED or hold | Reverse A: 1111 CR, 3100 DR |
| Amount dispute | FAILED | Reverse A per ops |
| Fraud block | FAILED | Reverse A + ops case |

Reverse scope phase A: **chỉ 1111/3100** — không đụng 2110 (ADJ-E03, TC-006-05).

---
## Chương 4. Pha B POSTED — deep dive {#chương-4-pha-b-posted--deep-dive}

Phase B xác nhận deposit, clear transit 3100, ghi liability + revenue, emit events. Template: [`foundation.md`](../../spec/foundation.md) §8.1 bước 3–6.

### 4.1 Kích hoạt phase B (`confirmDeposit`)

| Precondition | Kiểm tra |
|--------------|----------|
| Journal exists | `status=PENDING`, `use_case=DEPOSIT` |
| VA mapped | `memberId` resolved [ADR-030](../../adr/ADR-030-virtual-account-deposit-mapping.md) |
| Amount match | `confirmAmount == gross` phase A |
| Period | Open [ADR-023](../../adr/ADR-023-accounting-period-close.md) |
| Fee supplied | Orchestration passes `fee` — accounting validates balance |

### 4.2 Bút toán phase B (fee > 0, số chuẩn)

| # | TK | DR | CR | Amount |
|---|-----|----|----|--------|
| 3 | 3100 | 100.000 | | gross |
| 4 | 2110 | | 99.000 | net |
| 5 | 2110 | 1.000 | | fee |
| 6 | 4110 | | 1.000 | fee |

**Validation:** `gross = net + fee` → `100.000 = 99.000 + 1.000` ✓

### 4.3 T-account sau POSTED (full journal A+B)

```
1111:  DR 100.000                          → SD +100.000
3100:  CR 100.000 + DR 100.000            → SD 0
2110:  CR 99.000 + DR 1.000 (fee present) → net SD Có +99.000
4110:  CR 1.000                           → SD Có +1.000
Wallet: available += 99.000 (async)
```

### 4.4 Observables sau POSTED

| Kiểm tra | Kỳ vọng |
|----------|---------|
| `status` | `POSTED` |
| Line count | 6 (fee>0) or 4 (fee=0) |
| Transit 3100 | **net = 0** per journal |
| 1111 | +gross cumulative |
| 2110 | +net aggregate |
| 4110 | +fee (if fee>0) |
| `sum(DR)=sum(CR)` | full journal |
| Immutability | no UPDATE on lines [ADR-001](../../adr/ADR-001-immutable-ledger.md) |
| Event | `JournalPosted` once [ADR-013](../../adr/ADR-013-outbox-at-least-once-messaging.md) |

### 4.5 Biến thể fee — deep dive

#### 4.5.1 Zero fee (fee = 0)

| # | TK | DR | CR | Amount |
|---|-----|----|----|--------|
| 3 | 3100 | 75.000 | | gross |
| 4 | 2110 | | 75.000 | net=gross |

- Bỏ bước 5–6; 4110 unchanged.
- Wallet credit **75.000**.
- Acceptance: zero-fee deposit scenario.

#### 4.5.2 Fee = gross (net = 0) — DEP-E03

| gross | fee | net | 2110 CR | 4110 CR | Wallet |
|-------|-----|-----|---------|---------|--------|
| 10.000 | 10.000 | 0 | 0 | 10.000 | credit 0 or skip |

POSTED **hợp lệ** — 3100 vẫn zero; orchestration quyết định skip `DEPOSIT_CREDIT` vs credit 0.

#### 4.5.3 Large deposit 500M — DEP-E02

| gross | fee | net | 2110 CR | 4110 CR |
|-------|-----|-----|---------|---------|
| 500.000.000 | 5.000 | 499.995.000 | 499.995.000 | 5.000 |

Integer minor units — scale 4 HALF_UP. Precision scenario acceptance.

#### 4.5.4 Minimum 1 unit — DEP-E01

| gross | fee | net |
|-------|-----|-----|
| 1 | 0 | 1 |

`2110 CR 1`, wallet credited 1.

### 4.6 confirmDeposit idempotency

| Replay | Kết quả |
|--------|---------|
| Same ref, already POSTED | No-op — no duplicate 2110 (D-F4) |
| Same ref, fee conflict | Return existing — no 4110 change |
| Same ref, PENDING | Complete B once |

### 4.7 Cấm phase B

| Cấm | ID |
|-----|-----|
| confirmAmount ≠ gross A | D-F3 |
| Post unbalanced lines | D-F5 |
| Thiếu fee leg khi fee>0 | D-F13 |
| Phase B khi period closed | D-F9 |
| Orchestration raw addLines for deposit B | AC-006-03 |

### 4.8 confirmDeposit validation rules (exhaustive)

| Rule ID | Validation | Error if fail |
|---------|------------|---------------|
| V-B01 | coaTransId exists | NOT_FOUND |
| V-B02 | use_case = DEPOSIT | USE_CASE_MISMATCH |
| V-B03 | status = PENDING (or POSTED idempotent) | INVALID_STATE |
| V-B04 | confirmAmount = phase A gross | AMOUNT_MISMATCH |
| V-B05 | fee >= 0 | INVALID_FEE |
| V-B06 | net = gross - fee >= 0 | NEGATIVE_NET |
| V-B07 | period open for posting_date | PERIOD_CLOSED |
| V-B08 | 1111/3100/2110/4110 active | INACTIVE_ACCOUNT |
| V-B09 | append lines balanced | UNBALANCED |
| V-B10 | 3100 net zero after append | TRANSIT_NONZERO |
| V-B11 | fee>0 implies 4110 line present | FEE_LINE_MISSING |
| V-B12 | fee=0 implies no 4110 line | FEE_LINE_UNEXPECTED |

### 4.9 POSTED event payload checklist

| Field JournalPosted | Source |
|---------------------|--------|
| coaTransId | coa_trans.id |
| businessRef | coa_trans.business_ref |
| useCase | DEPOSIT |
| status | POSTED |
| postedAt | commit timestamp |

Wallet path consumes net from orchestration command — not recalculated from ledger lines in wallet service.

---
## Chương 5. VA mapping (ADR-030) {#chương-5-va-mapping-adr-030}

[ADR-030](../../adr/ADR-030-virtual-account-deposit-mapping.md): mỗi VA map **một** `memberId` + lane USER. Bảng mapping thuộc **orchestration/Application** — không trong `coa_*` hay `wallet_*`.

### 5.1 Lookup flow

```
Webhook vaNumber ──► Orchestration VA table ──► memberId
                              │
                    miss ─────┴──── hit
                      │              │
                      ▼              ▼
              PENDING hold      proceed phase B
              ops queue D-F2    confirmDeposit
```

### 5.2 Unmapped VA

- Phase A vẫn có thể post (tiền bank thật vào 1111).
- Phase B **blocked** — không 2110 CR.
- 3100 aging until map or reverse.

### 5.3 Wrong member

- Correct **before** POSTED: ops remap → confirm credits correct member (acceptance dep-11).
- POSTED wrong member: reversal + repost — không UPDATE lines.

### 5.4 Multi-VA [TBD]

`[TBD: Một member nhiều VA active — policy rotate, primary VA, merge rules.]`

### 5.5 Gherkin

TC-030-01..04 · VA unmapped · DEP-E09 wrong member blocked.

### 5.6 VA lifecycle states [TBD]

| State | Webhook | Phase B |
|-------|---------|---------|
| ACTIVE | Process normally | Allowed if mapped |
| INACTIVE | `[TBD: reject or hold?]` | Blocked |
| PENDING_ACTIVATION | Hold PENDING | Block until ACTIVE |

### 5.7 Mapping table ownership

Bảng VA **không** nằm trong `coa_*` hay `wallet_*` — orchestration/Application schema. Thay đổi mapping **không** retro-edit journal POSTED ([ADR-030](../../adr/ADR-030-virtual-account-deposit-mapping.md) AC-030-04).

### 5.8 Wrong member — decision tree

```
PENDING + wrong map discovered
  ├── before confirm: fix map → confirmDeposit → correct 2110 aggregate timing
  └── after POSTED: reversal journal → repost with correct member (orchestration wallet)
```

---
## Chương 6. Wallet handoff — JournalPosted vs WALLET_CREDIT (ADR-024) {#chương-6-wallet-handoff--journalposted-vs-wallet_credit-adr-024}

Accounting boundary **kết thúc** tại POSTED + `JournalPosted`. Wallet credit là downstream — orchestration owns.

### 6.1 Ba path ([ADR-024](../../adr/ADR-024-deposit-wallet-credit-dual-path.md))

| Path | Transport | Trigger |
|------|-----------|---------|
| A | Kafka S3 `JournalPosted` | Consumer filter DEPOSIT POSTED |
| B | RabbitMQ S6 `WALLET_CREDIT` | Explicit command |
| C | In-process | orchestration.wallet.credit() |

**Deployment:** một path primary — duplicate A+B same ref → one `DEPOSIT_CREDIT` (DEP-E08).

### 6.2 Contract wallet credit

| Field | Value |
|-------|-------|
| amount | **net** (99.000) — not gross |
| business_ref | same bank txn id |
| tx_type | DEPOSIT_CREDIT |
| coaTransId | correlation optional |

### 6.3 Accounting không biết wallet outcome

POSTED + 2110 CR stands even if wallet rejects LOCKED ([ADR-034](../../adr/ADR-034-locked-wallet-deposit-credit-reject.md)). W5 timing drift until unlock retry.

### 6.4 Sequence

```
Accounting POSTED ──► outbox ──► JournalPosted (S3)
                      │
                      └──► orchestration ──► WALLET_CREDIT (S6)
                                              │
                                              ▼
                                        wallet available += net
```

---
## Chương 7. Ma trận lỗi D-F1..D-F15 {#chương-7-ma-trận-lỗi-d-f1d-f15}

Mỗi failure ID mở rộng: **detection**, **accounting action**, **ops**, **forbidden**, **Gherkin ref**.

| ID | Tình huống | Ledger state | Accounting action | Cấm | Ops | Gherkin | ADR |
|----|------------|--------------|-------------------|-----|-----|---------|-----|
| D-F1 | Webhook trùng business_ref | any | Return coa_trans_id cũ; idempotentReplay=true | Insert journal thứ 2; double 1111 DR | Monitor duplicate webhook rate | Duplicate webhook scenario | AC-005, AC-006 |
| D-F2 | VA chưa map memberId | PENDING, 3100>0 | Giữ PENDING; ops alert; retry sau map | Force POSTED; ghi 2110 sai member | Manual VA mapping queue | VA unmapped — stays PENDING | ADR-030 |
| D-F3 | Confirm amount ≠ gross phase A | PENDING | Reject phase B; optional reverse A → FAILED | Post lệch amount; partial 2110 | Investigate bank vs ledger | Amount mismatch; Confirm exceeds | foundation §8.5 |
| D-F4 | Confirm khi đã POSTED | POSTED | Idempotent no-op; same response | Append lines; double 2110 | None — expected replay | Duplicate phase B | AC-006-06 |
| D-F5 | Phase B lines không cân | — | Reject postJournal; TX rollback | Lưu draft unbalanced | Fix orchestration fee calc | — | INV-01 |
| D-F6 | POSTED xong wallet consumer lag | POSTED, 2110 OK | None accounting — retry WALLET_CREDIT | Sửa 2110; DELETE lines | Aging job wallet_credit_lag | POSTED but consumer down; DEP-E07 | ADR-021, ADR-024 |
| D-F7 | Bank recall sau POSTED | POSTED | New reversal journal + reverses_id; ops wallet debit | UPDATE/DELETE coa_trans_data | Finance approve reversal | DEP-E10 Reversal after recall | ADR-001 |
| D-F8 | PENDING > SLA aging | PENDING | Ops: complete B or reverse A; alert fired | Auto-fail im lặng | Aging dashboard 3100 | DEP-E05 PENDING aging alert | ADR-021 [TBD thresholds] |
| D-F9 | Period closed trước phase B | PENDING | Block confirmDeposit; ACCOUNTING_PERIOD_CLOSED | Backdate posting_date | Ops waiver hoặc mở kỳ | X-E03 period close | ADR-023 |
| D-F10 | TK 1111/3100/2110/4110 inactive | — | Reject addLines / confirmDeposit | Post vào TK inactive | COA admin activate TK | — | coa_account.is_active |
| D-F11 | Worker crash mid phase A TX | partial? | Local TX rollback; retry idempotent | Orphan coa_trans_data rows | Check INV-01 orphan | DEP-E06 mid-TX (phase B similar) | ADR-035 |
| D-F12 | Cùng ref, gross khác | — | Conflict reject; không overwrite | Overwrite amount | Ops merge bank refs | Fee change on replay conflict | AC-005-03 style |
| D-F13 | Thiếu line fee khi fee>0 | — | Reject unbalanced vs gross | Post net-only skip 4110 | Fix fee legs template | — | ADR-009 matching |
| D-F14 | Zero fee deposit | POSTED | 4110 skip; 2110 CR gross | Force 4110 CR 0 line | Normal path | Zero fee deposit | — |
| D-F15 | Multi-currency webhook (future) | — | Reject v1 boundary | FX journal ad-hoc | N/A v1 | — | ADR-019 VND only |

### 7.F1 Chi tiết D-F1: Webhook trùng business_ref

**Detection:** Monitoring, API error code, hoặc SQL invariant liên quan D-F1.
**Accounting action:** Xem bảng trên — accounting **không** compensates wallet; orchestration owns wallet debit sau recall.
**Ops playbook:** Escalate theo severity; không sửa SQL trực tiếp `coa_trans_data`.
**Forbidden:** Mọi UPDATE POSTED lines — [ADR-001](../../adr/ADR-001-immutable-ledger.md).


### 7.F2 Chi tiết D-F2: VA chưa map memberId

**Detection:** Monitoring, API error code, hoặc SQL invariant liên quan D-F2.
**Accounting action:** Xem bảng trên — accounting **không** compensates wallet; orchestration owns wallet debit sau recall.
**Ops playbook:** Escalate theo severity; không sửa SQL trực tiếp `coa_trans_data`.
**Forbidden:** Mọi UPDATE POSTED lines — [ADR-001](../../adr/ADR-001-immutable-ledger.md).


### 7.F3 Chi tiết D-F3: Confirm amount ≠ gross phase A

**Detection:** Monitoring, API error code, hoặc SQL invariant liên quan D-F3.
**Accounting action:** Xem bảng trên — accounting **không** compensates wallet; orchestration owns wallet debit sau recall.
**Ops playbook:** Escalate theo severity; không sửa SQL trực tiếp `coa_trans_data`.
**Forbidden:** Mọi UPDATE POSTED lines — [ADR-001](../../adr/ADR-001-immutable-ledger.md).


### 7.F4 Chi tiết D-F4: Confirm khi đã POSTED

**Detection:** Monitoring, API error code, hoặc SQL invariant liên quan D-F4.
**Accounting action:** Xem bảng trên — accounting **không** compensates wallet; orchestration owns wallet debit sau recall.
**Ops playbook:** Escalate theo severity; không sửa SQL trực tiếp `coa_trans_data`.
**Forbidden:** Mọi UPDATE POSTED lines — [ADR-001](../../adr/ADR-001-immutable-ledger.md).


### 7.F5 Chi tiết D-F5: Phase B lines không cân

**Detection:** Monitoring, API error code, hoặc SQL invariant liên quan D-F5.
**Accounting action:** Xem bảng trên — accounting **không** compensates wallet; orchestration owns wallet debit sau recall.
**Ops playbook:** Escalate theo severity; không sửa SQL trực tiếp `coa_trans_data`.
**Forbidden:** Mọi UPDATE POSTED lines — [ADR-001](../../adr/ADR-001-immutable-ledger.md).


### 7.F6 Chi tiết D-F6: POSTED xong wallet consumer lag

**Detection:** Monitoring, API error code, hoặc SQL invariant liên quan D-F6.
**Accounting action:** Xem bảng trên — accounting **không** compensates wallet; orchestration owns wallet debit sau recall.
**Ops playbook:** Escalate theo severity; không sửa SQL trực tiếp `coa_trans_data`.
**Forbidden:** Mọi UPDATE POSTED lines — [ADR-001](../../adr/ADR-001-immutable-ledger.md).


### 7.F7 Chi tiết D-F7: Bank recall sau POSTED

**Detection:** Monitoring, API error code, hoặc SQL invariant liên quan D-F7.
**Accounting action:** Xem bảng trên — accounting **không** compensates wallet; orchestration owns wallet debit sau recall.
**Ops playbook:** Escalate theo severity; không sửa SQL trực tiếp `coa_trans_data`.
**Forbidden:** Mọi UPDATE POSTED lines — [ADR-001](../../adr/ADR-001-immutable-ledger.md).


### 7.F8 Chi tiết D-F8: PENDING > SLA aging

**Detection:** Monitoring, API error code, hoặc SQL invariant liên quan D-F8.
**Accounting action:** Xem bảng trên — accounting **không** compensates wallet; orchestration owns wallet debit sau recall.
**Ops playbook:** Escalate theo severity; không sửa SQL trực tiếp `coa_trans_data`.
**Forbidden:** Mọi UPDATE POSTED lines — [ADR-001](../../adr/ADR-001-immutable-ledger.md).


### 7.F9 Chi tiết D-F9: Period closed trước phase B

**Detection:** Monitoring, API error code, hoặc SQL invariant liên quan D-F9.
**Accounting action:** Xem bảng trên — accounting **không** compensates wallet; orchestration owns wallet debit sau recall.
**Ops playbook:** Escalate theo severity; không sửa SQL trực tiếp `coa_trans_data`.
**Forbidden:** Mọi UPDATE POSTED lines — [ADR-001](../../adr/ADR-001-immutable-ledger.md).


### 7.10 Chi tiết D-F10: TK 1111/3100/2110/4110 inactive

**Detection:** Monitoring, API error code, hoặc SQL invariant liên quan D-F10.
**Accounting action:** Xem bảng trên — accounting **không** compensates wallet; orchestration owns wallet debit sau recall.
**Ops playbook:** Escalate theo severity; không sửa SQL trực tiếp `coa_trans_data`.
**Forbidden:** Mọi UPDATE POSTED lines — [ADR-001](../../adr/ADR-001-immutable-ledger.md).


### 7.11 Chi tiết D-F11: Worker crash mid phase A TX

**Detection:** Monitoring, API error code, hoặc SQL invariant liên quan D-F11.
**Accounting action:** Xem bảng trên — accounting **không** compensates wallet; orchestration owns wallet debit sau recall.
**Ops playbook:** Escalate theo severity; không sửa SQL trực tiếp `coa_trans_data`.
**Forbidden:** Mọi UPDATE POSTED lines — [ADR-001](../../adr/ADR-001-immutable-ledger.md).


### 7.12 Chi tiết D-F12: Cùng ref, gross khác

**Detection:** Monitoring, API error code, hoặc SQL invariant liên quan D-F12.
**Accounting action:** Xem bảng trên — accounting **không** compensates wallet; orchestration owns wallet debit sau recall.
**Ops playbook:** Escalate theo severity; không sửa SQL trực tiếp `coa_trans_data`.
**Forbidden:** Mọi UPDATE POSTED lines — [ADR-001](../../adr/ADR-001-immutable-ledger.md).


### 7.13 Chi tiết D-F13: Thiếu line fee khi fee>0

**Detection:** Monitoring, API error code, hoặc SQL invariant liên quan D-F13.
**Accounting action:** Xem bảng trên — accounting **không** compensates wallet; orchestration owns wallet debit sau recall.
**Ops playbook:** Escalate theo severity; không sửa SQL trực tiếp `coa_trans_data`.
**Forbidden:** Mọi UPDATE POSTED lines — [ADR-001](../../adr/ADR-001-immutable-ledger.md).


### 7.14 Chi tiết D-F14: Zero fee deposit

**Detection:** Monitoring, API error code, hoặc SQL invariant liên quan D-F14.
**Accounting action:** Xem bảng trên — accounting **không** compensates wallet; orchestration owns wallet debit sau recall.
**Ops playbook:** Escalate theo severity; không sửa SQL trực tiếp `coa_trans_data`.
**Forbidden:** Mọi UPDATE POSTED lines — [ADR-001](../../adr/ADR-001-immutable-ledger.md).


### 7.15 Chi tiết D-F15: Multi-currency webhook (future)

**Detection:** Monitoring, API error code, hoặc SQL invariant liên quan D-F15.
**Accounting action:** Xem bảng trên — accounting **không** compensates wallet; orchestration owns wallet debit sau recall.
**Ops playbook:** Escalate theo severity; không sửa SQL trực tiếp `coa_trans_data`.
**Forbidden:** Mọi UPDATE POSTED lines — [ADR-001](../../adr/ADR-001-immutable-ledger.md).

---
## Chương 8. Reversal & bank recall (ADR-001) {#chương-8-reversal--bank-recall-adr-001}

Bank recall sau POSTED → **new reversal journal** — không DELETE/UPDATE.

### 8.1 Template reversal POSTED chuẩn (gross 100k fee 1k posted)

Journal mới `use_case=DEPOSIT` hoặc `ADJUSTMENT` policy, `reverses_id=<original>`, lines đảo:

| Line | TK | DR | CR |
|------|-----|-----|-----|
| 1 | 4110 | 1.000 | |
| 2 | 2110 | 99.000 | |
| 3 | 2110 | | 1.000 |
| 4 | 3100 | | 100.000 |
| 5 | 3100 | 100.000 | |
| 6 | 1111 | | 100.000 |

Net effect: undo 1111, 2110, 4110. Wallet debit net via orchestration — [ADR-026](../../adr/ADR-026-wallet-never-reverses-accounting.md).

### 8.2 Reverse phase A only (FAILED)

| Line | TK | DR | CR |
|------|-----|-----|-----|
| 1 | 3100 | 100.000 | |
| 2 | 1111 | | 100.000 |

Scope: **không** 2110 — ADJ-E03.

### 8.3 Recall SLA [TBD]

`[TBD: SLA cho phép reversal tự động vs approval Finance sau bank recall.]`

---
## Chương 9. Idempotency end-to-end (ADR-005) {#chương-9-idempotency-end-to-end-adr-005}

`business_ref` = bank txn id — propagated S1→S2→S6→S3→wallet unchanged.

| Operation | Key | Replay |
|-----------|-----|--------|
| Webhook / createJournal A | business_ref + DEPOSIT | Same coa_trans_id |
| confirmDeposit B | same ref | No-op if POSTED |
| WALLET_CREDIT | business_ref + DEPOSIT_CREDIT | One wallet_tx |
| S6 redelivery | messageId transport; businessRef business | Idempotent consumer |

Withdraw sub-keys `:settle` **không** áp dụng deposit.

---
## Chương 10. Messaging S6 BANK_DEPOSIT, S3, outbox (ADR-013) {#chương-10-messaging-s6-s3-outbox-adr-013}

### 10.1 S6 command BANK_DEPOSIT

Envelope: `commandType`, `businessRef`, `messageId` (UUID per publish), payload amount/va/memberId.

Worker: idempotent `(BANK_DEPOSIT, businessRef)`.

### 10.2 S3 event JournalPosted

Emitted POSTED same TX as outbox insert [ADR-013](../../adr/ADR-013-outbox-at-least-once-messaging.md).

Payload: `coaTransId`, `businessRef`, `use_case=DEPOSIT`, amounts.

### 10.3 Outbox relay

At-least-once — consumer dedup businessRef. Poison → DLQ DEP-E15.

---
## Chương 11. Security mTLS webhook (ADR-022) {#chương-11-security-mtls-webhook-adr-022}

`notifyDeposit` requires valid mTLS client cert — not JWT.

| Case | HTTP | Ledger |
|------|------|--------|
| Invalid cert | 401 | No PENDING |
| Valid cert | 202 + businessRef | Enqueue S6 |

Gherkin: notifyDeposit without valid mTLS → reject.

---
## Chương 12. Aging PENDING (ADR-021) {#chương-12-aging-pending-adr-021}

Metric: `deposit_pending_3100_age`.

| State | Job action |
|-------|------------|
| PENDING > T | Alert ops — retry confirm or reverse A |

`[TBD: Threshold T = ? hours/days — product ops chốt.]`

DEP-E05: 48h PENDING → alert fired.

---
## Chương 13. Period close (ADR-023) {#chương-13-period-close-adr-023}

Phase B `confirmDeposit` blocked if posting_date in closed period → `ACCOUNTING_PERIOD_CLOSED`.

Period close blocked if transit 3100 non-zero aggregate (policy) or open PENDING waiver list.

Corrections: reversal in **open** period with `reverses_id`.

---
## Chương 14. LOCKED wallet deposit credit (ADR-034) {#chương-14-locked-wallet-deposit-credit-adr-034}

W-O1 v1: `wallet.status=LOCKED` → reject DEPOSIT_CREDIT `WALLET_LOCKED`.

| Layer | Behavior |
|-------|----------|
| Accounting | POSTED normal — 2110 CR stands |
| Wallet | Reject credit |
| Saga | AWAITING_WALLET until unlock |
| Ops | Unlock → aging retry → one DEPOSIT_CREDIT |

DEP-E11 · LCK-D feature stretch.

---
## Chương 15. Reconciliation FR-10, W5, 4110 {#chương-15-reconciliation-fr-10-w5-4110}

| Signal | Meaning | Action |
|--------|---------|--------|
| 3100 aging > T | Stuck PENDING | Ops queue |
| 1111 vs bank stmt | FR-10 bank recon | Per-txn investigate |
| 2110 vs sum wallet USER | W5 drift | Report only ADR-014 |
| POSTED no wallet credit | Consumer lag | Retry S6/S3 |
| 4110 vs fee report | Deposit fee revenue | Management report |

---
## Chương 16. State machine & timeline diagrams {#chương-16-state-machine--timeline-diagrams}

### 16.1 coa_trans state machine

```
                    createJournal A
    (none) ──────────────────────► PENDING
                                      │
                         confirmDeposit│ reverse A
                                      ▼
                                   POSTED ─── reversal ───► REVERSED
                                      │
                              ops FAIL policy
                                      ▼
                                   FAILED
```

### 16.2 End-to-end timeline (số chuẩn)

```
T+0s   Member CK 100k
T+1s   Webhook → 202
T+2s   S6 → Phase A PENDING (1111/3100)
T+3s   VA map OK
T+4s   confirmDeposit POSTED (3100=0, 2110/4110)
T+5s   JournalPosted + WALLET_CREDIT
T+6s   wallet available = 99k
T+1d   FR-10 bank statement match 1111
```

---
## Chương 17. S2 API deposit operations field guide {#chương-17-s2-api-deposit-operations-field-guide}

| Endpoint | Phase | Key fields |
|----------|-------|------------|
| createJournal | A → PENDING | use_case=DEPOSIT, reference_id, lines[{1111 DR, 3100 CR}] |
| confirmDeposit | B → POSTED | coaTransId, fee, memberId validation orchestration-side |
| postJournal | B | Internal — prefer confirmDeposit for deposit |
| getJournal | query | status, business_ref, lines |

OpenAPI: `spec/contracts/open-api/accounting-internal.yaml`.

---
## Chương 18. SQL invariant CI (ADR-031) {#chương-18-sql-invariant-ci-adr-031}

Deposit-specific checks [ADR-031](../../adr/ADR-031-sql-ledger-invariant-ci.md):

```sql
-- INV-01: balanced POSTED
SELECT coa_trans_id FROM coa_trans t
JOIN (
  SELECT coa_trans_id, SUM(dr_amount) dr, SUM(cr_amount) cr
  FROM coa_trans_data GROUP BY 1
) x ON x.coa_trans_id = t.id
WHERE t.use_case = 'DEPOSIT' AND t.status = 'POSTED' AND dr <> cr;

-- INV-03: 3100 zero per POSTED deposit journal
SELECT t.business_ref FROM coa_trans t
JOIN coa_trans_data d ON d.coa_trans_id = t.id AND d.account_code = '3100'
WHERE t.use_case = 'DEPOSIT' AND t.status = 'POSTED'
GROUP BY t.id HAVING SUM(d.dr_amount) - SUM(d.cr_amount) <> 0;

-- INV-02: duplicate POSTED business_ref
SELECT business_ref, COUNT(*) FROM coa_trans
WHERE use_case = 'DEPOSIT' AND status = 'POSTED'
GROUP BY business_ref HAVING COUNT(*) > 1;
```

---
## Chương 20. Ops runbook outline {#chương-20-ops-runbook-outline}

`[TBD: Chi tiết từng bước ops — draft outline v1]`

| Case | Steps [TBD] |
|------|-------------|
| PENDING aging | 1. Identify business_ref 2. Check VA map 3. Confirm or reverse A |
| VA unmapped | 1. Map VA table 2. Retry confirm 3. Or reverse |
| POSTED wallet lag | 1. Check consumer 2. Retry WALLET_CREDIT 3. W5 report |
| LOCKED uncredited | 1. Verify POSTED 2. Unlock wallet 3. Retry credit |
| Bank recall | 1. Verify bank notice 2. Approve reversal 3. Wallet debit |
| Amount dispute | 1. Hold PENDING 2. Contact bank 3. Reverse A if invalid |

---
## Chương 1B. Actors & integration surfaces (mở rộng) {#chương-1b-actors-mở-rộng}

### 1B.1 Bảng message / event deposit

| Surface | Name | Direction | Payload keys |
|---------|------|-----------|--------------|
| S1 HTTP | notifyDeposit | Bank → Platform | amount, vaNumber, businessRef |
| S1 HTTP | getDepositStatus | Client → Platform | businessRef → PENDING/POSTED |
| S6 | BANK_DEPOSIT | Orch → Worker | businessRef, gross, vaNumber |
| S6 | WALLET_CREDIT | Orch → Worker | businessRef, net, memberId, coaTransId |
| S3 Kafka | JournalPosted | Accounting → Orch | coaTransId, use_case=DEPOSIT |
| S3 Kafka | CommandFailed | Worker → Ops | DLQ ref |

### 1B.2 Chi tiết orchestration saga deposit

| Step | Saga state | Accounting | Wallet |
|------|------------|------------|--------|
| 1 | RECEIVED | — | — |
| 2 | PHASE_A | PENDING 1111/3100 | unchanged |
| 3 | MAPPED | hold or proceed | unchanged |
| 4 | PHASE_B | POSTED | — |
| 5 | CREDITING | — | DEPOSIT_CREDIT net |
| 6 | DONE | — | available += net |

---
## Chương 3B. Phase A — mười hai ví dụ T-account bổ sung {#chương-3b-phase-a-ví-dụ-bổ-sung}

#### Chuẩn — gross 100,000 · `dep-1`

```
1111 DR 100,000  |  3100 CR 100,000
2110: —  |  4110: —  |  wallet: 0
```


#### Zero-fee prep — gross 75,000 · `dep-zf`

```
1111 DR 75,000  |  3100 CR 75,000
2110: —  |  4110: —  |  wallet: 0
```


#### Large DEP-E02 prep — gross 500,000,000 · `dep-lg`

```
1111 DR 500,000,000  |  3100 CR 500,000,000
2110: —  |  4110: —  |  wallet: 0
```


#### Min unit — gross 1 · `dep-min`

```
1111 DR 1  |  3100 CR 1
2110: —  |  4110: —  |  wallet: 0
```


#### Acceptance phase A 50k — gross 50,000 · `dep-1a`

```
1111 DR 50,000  |  3100 CR 50,000
2110: —  |  4110: —  |  wallet: 0
```


#### VA unmapped hold — gross 100,000 · `dep-5`

```
1111 DR 100,000  |  3100 CR 100,000
2110: —  |  4110: —  |  wallet: 0
```


#### Concurrent ref — gross 100,000 · `dep-9`

```
1111 DR 100,000  |  3100 CR 100,000
2110: —  |  4110: —  |  wallet: 0
```


#### FAILED candidate — gross 80,000 · `dep-8`

```
1111 DR 80,000  |  3100 CR 80,000
2110: —  |  4110: —  |  wallet: 0
```


#### Aging candidate — gross 200,000 · `dep-age`

```
1111 DR 200,000  |  3100 CR 200,000
2110: —  |  4110: —  |  wallet: 0
```


#### Credit-before-POSTED block test — gross 100,000 · `dep-3`

```
1111 DR 100,000  |  3100 CR 100,000
2110: —  |  4110: —  |  wallet: 0
```


#### Period check — gross 100,000 · `dep-pc`

```
1111 DR 100,000  |  3100 CR 100,000
2110: —  |  4110: —  |  wallet: 0
```


#### Inactive TK reject — gross 100,000 · `dep-inact`

```
1111 DR 100,000  |  3100 CR 100,000
2110: —  |  4110: —  |  wallet: 0
```

## Chương 4B. Phase B — walkthrough số học {#chương-4b-phase-b-walkthrough}

### 4B.1 Chuẩn 100.000 / 1.000 / 99.000

| Bước | 3100 SD | 2110 SD | 4110 SD | Wallet |
|------|---------|---------|---------|--------|
| Sau A | Có 100k | 0 | 0 | 0 |
| Line 3 DR | 0 | 0 | 0 | 0 |
| Line 4 CR net | 0 | Có 99k | 0 | 0 |
| Lines 5-6 fee | 0 | net 99k | Có 1k | 0 |
| Wallet credit | 0 | 99k | 1k | 99k |

### 4B.2 Zero fee 75.000

Lines 1-4 only; 2110 CR 75k; wallet 75k.

### 4B.3 Fee=gross 10.000 → net 0

2110 CR 0 + DR 10k fee presentation; 4110 CR 10k; wallet 0/skip.

---
## Chương 23. COA pointer — Quyển II {#chương-23-coa-pointer-quyển-ii}

Tra handbook đầy đủ — không duplicate:

| TK | Deposit role | Link |
|----|--------------|------|
| 1111 | Bank DR phase A | [vol-02 TK 1111](./vol-02-coa-handbook.md#tk-1111--tiền-gửi-ngân-hàng-vietinbank-chuyên-dụng) |
| 3100 | Transit two-phase | [vol-02 TK 3100](./vol-02-coa-handbook.md#tk-3100--transit--deposit) |
| 2110 | User liability net | [vol-02 TK 2110](./vol-02-coa-handbook.md#tk-2110--wallet-payable--user-lane) |
| 4110 | Deposit fee revenue | [vol-02 TK 4110](./vol-02-coa-handbook.md#tk-4110--doanh-thu-phí-nạp-tiền) |

---
## Chương 1C. Deposit trong bức tranh platform {#chương-1c-deposit-platform-context}

### 1C.1 Deposit trong ma trận 9 use cases

Deposit là use case **async duy nhất** credit wallet **sau** POSTED two-phase. Các use case khác: withdraw (3200), transfer (3300), IBFT (3400), payment (3500), QR (3500+1113), payroll (3600), disbursement (3700), EOD (3800-3820).

### 1C.2 Napas N/A — tại sao không 1112/5100

Deposit Vietinbank VA → **1111** only. Không qua Napas clearing → không **1112**, không chi phí Napas **5100** trên luồng deposit v1.

### 1C.3 HTTP 202 contract

| Response | Ý nghĩa | Không có nghĩa |
|----------|---------|----------------|
| 202 + businessRef | Accepted async processing | POSTED |
| 202 | Webhook hợp lệ mTLS | Wallet đã credit |

Client poll `getDepositStatus` hoặc subscribe notification — DEP-E13/E14.

### 1C.4 Observability tags

Log fields bắt buộc: `businessRef`, `coaTransId`, `use_case=DEPOSIT`, `phase`, `idempotentReplay`, `messageId`.

---
## Chương 2B. Accrual — câu hỏi kiểm toán {#chương-2b-accrual-audit-qa}

| Câu hỏi auditor | Trả lời ngắn | Evidence |
|-----------------|--------------|----------|
| Tiền bank vào có = user spend ngay? | Không | PENDING 3100; ACC-E01 |
| 2110 khi nào tăng? | POSTED phase B | coa_trans_data 2110 CR |
| Fee khi nào? | Cùng POSTED journal | 4110 line 6; ACC-E04 |
| UI pending inflow? | Saga label — không wallet | ACC-E05 |
| Cash basis shortcut? | Rejected v1 | ADR-036 AC-036-04 |

---
## Chương 3C. Phase A — SQL audit queries {#chương-3c-phase-a-sql}

```sql
-- PENDING deposit must have exactly 2 lines 1111+3100
SELECT t.business_ref, COUNT(*) cnt
FROM coa_trans t
JOIN coa_trans_data d ON d.coa_trans_id = t.id
WHERE t.use_case='DEPOSIT' AND t.status='PENDING'
GROUP BY t.id HAVING COUNT(*) <> 2;

-- PENDING must not touch 2110/4110
SELECT t.business_ref FROM coa_trans t
JOIN coa_trans_data d ON d.coa_trans_id = t.id
WHERE t.use_case='DEPOSIT' AND t.status='PENDING'
  AND d.account_code IN ('2110','4110');
```

---
## Chương 4C. Ma trận fee variants — accounting validation {#chương-4c-fee-matrix}
| gross | fee | net | lines | validation |
|-------|-----|-----|-------|------------|
| 100,000 | 1,000 | 99,000 | 6 | OK |
| 75,000 | 0 | 75,000 | 4 | OK zero fee |
| 10,000 | 10,000 | 0 | 6 | OK fee=gross |
| 500,000,000 | 5,000 | 499,995,000 | 6 | OK large |
| 1 | 0 | 1 | 4 | OK min |
| 100,000 | 2,000 | 98,000 | 6 | OK custom fee |
| 100,000 | 100,001 | -1 | 0 | REJECT net<0 |
| 100,000 | 0 | 100,000 | 4 | OK zero fee full net |

---

## Chương 7B. Ops playbook chi tiết D-F1..D-F15 {#chương-7b-ops-playbook-d-f}

### D-F1 playbook

1. Verify duplicate webhook same businessRef
2. Query coa_trans — expect single row
3. Return existing coa_trans_id — no new insert
4. Monitor metric duplicate_webhook_deposit


### D-F2 playbook

1. Identify PENDING journal + 3100 hold
2. Check VA mapping table — vaNumber → memberId
3. If unmapped: ops ticket — do NOT force POSTED
4. After map: retry confirmDeposit idempotent


### D-F3 playbook

1. Compare phase A gross vs bank confirm amount
2. If mismatch: reject phase B
3. Optional: ops reverse phase A → FAILED
4. Never post partial 2110


### D-F6 playbook

1. Confirm POSTED + 2110 lines exist
2. Check wallet_tx DEPOSIT_CREDIT missing
3. Replay WALLET_CREDIT / restart consumer
4. Do NOT edit 2110 in accounting


### D-F7 playbook

1. Verify bank recall documentation
2. Finance approve reversal journal Ch8 template
3. Post reversal — link reverses_id
4. Orchestration wallet debit net idempotent


### D-F8 playbook

1. [TBD: Check age > threshold T]
2. Alert ops dashboard deposit_pending_3100_age
3. Resolve: map VA + confirm OR reverse A
4. Document resolution in ops ticket


### D-F11 playbook

1. Check for orphan coa_trans_data after worker crash
2. INV-01 CI should catch unbalanced partial
3. Retry createJournal — idempotent on businessRef
4. TX boundary must rollback partial lines

---

## Chương 8B. Reversal — journal metadata {#chương-8b-reversal-metadata}

| Field reversal journal | Value |
|------------------------|-------|
| use_case | DEPOSIT or ADJUSTMENT per policy |
| reverses_id | original coa_trans.id |
| business_ref | `{original}-REV` or bank recall ref |
| status | POSTED |
| posting_date | open period only |

Wallet compensation: orchestration — **không** trong accounting service.

---
## Chương 9B. Idempotency — test matrix {#chương-9b-idempotency-tests}

| Test | Action | Expected |
|------|--------|----------|
| T1 | Duplicate webhook | same coa_trans_id |
| T2 | Duplicate confirm POSTED | no-op |
| T3 | Duplicate WALLET_CREDIT | one wallet_tx |
| T4 | Redelivery new messageId | one effect |
| T5 | Same ref different gross | conflict reject |
| T6 | Same ref different use_case | accounting reject |

---
## Chương 10B. Outbox deposit sequence {#chương-10b-outbox-sequence}

1. BEGIN 2. POSTED lines 3. outbox INSERT 4. COMMIT 5. relay publish 6. consumer ACK after wallet commit or no-op.

---

## Chương 11B. mTLS cert rotation {#chương-11b-mtls-rotation}

Bank cert rotation: dual-trust window `[TBD: ops runbook]` — invalid cert → 401 no ledger effect.

---

## Chương 12B. Aging metrics dashboard {#chương-12b-aging-dashboard}

Panels: PENDING count, max age, 3100 SUM hold, wallet_credit_lag_after_posted.

---

## Chương 13B. Period close waiver list {#chương-13b-period-waiver}

`[TBD: PENDING deposit waiver policy for month-end close — Finance approval required.]`

---

## Chương 14B. LOCKED ops unlock procedure {#chương-14b-locked-unlock}

1. Verify POSTED 2. Confirm LOCKED reason cleared 3. Set ACTIVE 4. Aging job retries credit 5. W5 verify.

---

## Chương 15B. FR-10 daily recon steps {#chương-15b-fr10-daily}

1. Export 1111 movements 2. Match bank file 3. Flag PENDING holds 4. Escalate exceptions.

---

## Chương 17B. S2 request/response examples {#chương-17b-s2-examples}

**createJournal phase A (JSON illustrative):**

```json
{
  "useCase": "DEPOSIT",
  "referenceId": "bank-txn-12345",
  "status": "PENDING",
  "lines": [
    {"accountCode": "1111", "debit": 100000, "credit": 0},
    {"accountCode": "3100", "debit": 0, "credit": 100000}
  ]
}
```

**confirmDeposit phase B:**

```json
{
  "coaTransId": "uuid",
  "confirmAmount": 100000,
  "fee": 1000
}
```

---
## Chương 18B. SQL — POSTED deposit fee integrity {#chương-18b-sql-fee}

```sql
-- fee>0 POSTED must have 4110 line
SELECT t.business_ref FROM coa_trans t
WHERE t.use_case='DEPOSIT' AND t.status='POSTED'
  AND EXISTS (
    SELECT 1 FROM coa_trans_data d
    WHERE d.coa_trans_id=t.id AND d.account_code='2110' AND d.side='DR'
  )
  AND NOT EXISTS (
    SELECT 1 FROM coa_trans_data d
    WHERE d.coa_trans_id=t.id AND d.account_code='4110'
  );
-- expect 0 for fee>0 deposits
```

---
## Chương 19B. Gherkin — notes triển khai QA {#chương-19b-gherkin-qa-notes}

| Scenario | QA focus | Fixture |
|----------|----------|---------|
| Happy 100k/1k | End-to-end DR/CR + wallet | dep-1 |
| Phase A only | wallet=0 invariant | dep-1a |
| Zero fee | 4 lines only | gross 75k |
| Duplicate webhook | idempotent | dep-2 |
| VA unmapped | 3100 hold | dep-5 |
| DEP-E02 500M | integer overflow guard | scale 4 |
| DEP-E11 LOCKED | ledger≠wallet timing | LOCKED fixture |
| DEP-E15 DLQ | no orphan POSTED | malformed S6 |

Full Gherkin: [`acceptance.md`](../acceptance.md).

---
## Chương 20B. Runbook chi tiết [TBD steps] {#chương-20b-runbook-detail}

### 20B.1 PENDING > [TBD T] — chi tiết

1. `SELECT * FROM coa_trans WHERE status='PENDING' AND use_case='DEPOSIT' ORDER BY created_at`
2. Với mỗi `business_ref`: log gross, age_hours, vaNumber
3. Tra VA map — nếu thiếu: liên hệ member support lấy VA đúng
4. Nếu map OK: verify fee config → `confirmDeposit`
5. Nếu invalid/fraud: reverse phase A → FAILED
6. Close ticket với coaTransId final state

### 20B.2 Bank recall — chi tiết

1. Nhận recall từ bank với `businessRef`
2. `SELECT status FROM coa_trans WHERE business_ref=?`
3. Nếu POSTED: draft reversal journal Ch8 — Finance approve
4. Post reversal trong open period
5. Trigger wallet debit — verify W5
6. `[TBD: SLA hours from recall notice to reversal post]`

---
## Phụ lục D. Trial balance snapshots {#phụ-lục-d--trial-balance-snapshots}

### D.1 Sau phase A chuẩn (single deposit)

| TK | Nợ | Có |
|----|-----|-----|
| 1111 | 100.000 | |
| 3100 | | 100.000 |

### D.2 Sau POSTED chuẩn

| TK | Nợ | Có |
|----|-----|-----|
| 1111 | 100.000 | |
| 2110 | | 99.000 |
| 4110 | | 1.000 |
| 3100 | 0 | 0 |

### D.3 Hai POSTED tuần tự (DEP-E04)

After dep-a (99k net) + dep-b (50k net):

| TK | Δ Có |
|----|------|
| 1111 | +150.000 Nợ |
| 2110 | +149.000 |
| 4110 | +1.000 |

---
## Phụ lục E. 20 kịch bản số worked examples {#phụ-lục-e--20-kịch-bản-số}
| ID | gross | fee | net | mô tả |
|----|-------|-----|-----|-------|
| E01 | 100,000 | 1,000 | 99,000 | Happy POSTED |
| E02 | 75,000 | 0 | 75,000 | Zero fee |
| E03 | 10,000 | 10,000 | 0 | Fee=gross |
| E04 | 500,000,000 | 5,000 | 499,995,000 | Large |
| E05 | 1 | 0 | 1 | Min unit |
| E06 | 50,000 | 0 | 50,000 | Phase B 50k |
| E07 | 100,000 | 0 | 100,000 | 100k zero fee |
| E08 | 200,000 | 2,000 | 198,000 | 200k fee 2k |
| E09 | 999,999 | 999 | 999,000 | Near million |
| E10 | 100,000 | 500 | 99,500 | Fee 500 |
| E11 | 100,000 | 0 | 100,000 | PENDING A only |
| E12 | 80,000 | 800 | 79,200 | FAILED candidate |
| E13 | 100,000 | 1,000 | 99,000 | Duplicate idempotent |
| E14 | 100,000 | 1,000 | 99,000 | LOCKED credit lag |
| E15 | 100,000 | 1,000 | 99,000 | Recall reversal |
| E16 | 100,000 | 1,000 | 99,000 | Period close block |
| E17 | 100,000 | 1,000 | 99,000 | VA unmapped hold |
| E18 | 100,000 | 1,000 | 99,000 | Consumer lag |
| E19 | 100,000 | 1,000 | 99,000 | mTLS valid |
| E20 | 100,000 | 1,000 | 99,000 | Outbox replay |


Mỗi kịch bản E01–E10: trace DR/CR theo Phụ lục B. E11–E20: trace failure/playbook Ch7.

---
## Chương 7C. Ma trận lỗi — mở rộng từng ID {#chương-7c-ma-trận-mở-rộng}

### D-F1: Webhook trùng business_ref

| Field | Detail |
|-------|--------|
| Gherkin | Duplicate webhook |
| ADR/Ref | AC-005-07 |
| Detection | Bank gửi lại cùng bankTxnId do timeout hoặc retry bank-side. |
| Accounting | SELECT id FROM coa_trans WHERE business_ref=? AND use_case='DEPOSIT' — row exists. |
| Action | Return existing coa_trans_id; idempotentReplay=true; HTTP 200. |
| **Forbidden** | Second insert; double 1111 DR. |
| Ops | None — expected behavior. |

**Ledger observables:** tra bảng Ch7 chính — verify 3100, 2110, 4110, `status`.



### D-F2: VA chưa map

| Field | Detail |
|-------|--------|
| Gherkin | VA unmapped — stays PENDING |
| ADR/Ref | ADR-030 |
| Detection | Webhook VA không có trong orchestration mapping table. |
| Accounting | PENDING journal + 3100 CR; confirmDeposit blocked. |
| Action | Hold PENDING; alert ops queue; retry after map. |
| **Forbidden** | Force POSTED without memberId. |
| Ops | Manual VA mapping — `[TBD: SLA]`. |

**Ledger observables:** tra bảng Ch7 chính — verify 3100, 2110, 4110, `status`.



### D-F3: Confirm ≠ gross A

| Field | Detail |
|-------|--------|
| Gherkin | Amount mismatch |
| ADR/Ref | foundation §8.5 |
| Detection | Bank confirm amount khác gross phase A. |
| Accounting | confirmAmount != SUM(3100 CR) phase A. |
| Action | Reject phase B; status remains PENDING or FAILED per policy. |
| **Forbidden** | Post partial 2110; adjust amount silently. |
| Ops | Contact bank; reverse A if fraud/error. |

**Ledger observables:** tra bảng Ch7 chính — verify 3100, 2110, 4110, `status`.



### D-F4: Confirm đã POSTED

| Field | Detail |
|-------|--------|
| Gherkin | Duplicate phase B |
| ADR/Ref | AC-006-06 |
| Detection | Worker/orchestration retry confirmDeposit sau POSTED. |
| Accounting | status=POSTED already. |
| Action | Idempotent no-op; return same coaTransId. |
| **Forbidden** | Append duplicate 2110 lines. |
| Ops | None. |

**Ledger observables:** tra bảng Ch7 chính — verify 3100, 2110, 4110, `status`.



### D-F5: Lines B không cân

| Field | Detail |
|-------|--------|
| Gherkin | Unbalanced journal |
| ADR/Ref | INV-01 |
| Detection | Orchestration gửi fee/net không khớp gross. |
| Accounting | sum(DR) != sum(CR) at post boundary. |
| Action | Reject postJournal; TX rollback. |
| **Forbidden** | Store unbalanced draft. |
| Ops | Fix fee calculation orchestration. |

**Ledger observables:** tra bảng Ch7 chính — verify 3100, 2110, 4110, `status`.



### D-F6: Wallet lag POSTED

| Field | Detail |
|-------|--------|
| Gherkin | POSTED consumer down |
| ADR/Ref | DEP-E07 |
| Detection | 2110 POSTED nhưng wallet consumer down/lag. |
| Accounting | POSTED + no wallet_tx DEPOSIT_CREDIT. |
| Action | Accounting: none. Retry WALLET_CREDIT. |
| **Forbidden** | Edit 2110; DELETE lines. |
| Ops | Aging job wallet_credit_lag; alert if > tolerance. |

**Ledger observables:** tra bảng Ch7 chính — verify 3100, 2110, 4110, `status`.



### D-F7: Bank recall

| Field | Detail |
|-------|--------|
| Gherkin | DEP-E10 reversal |
| ADR/Ref | ADR-001 |
| Detection | Bank thu hồi deposit sau POSTED. |
| Accounting | Bank recall notice + original POSTED exists. |
| Action | New reversal journal + reverses_id; wallet debit. |
| **Forbidden** | UPDATE coa_trans_data. |
| Ops | Finance approve; `[TBD: recall SLA]`. |

**Ledger observables:** tra bảng Ch7 chính — verify 3100, 2110, 4110, `status`.



### D-F8: PENDING aging

| Field | Detail |
|-------|--------|
| Gherkin | DEP-E05 aging alert |
| ADR/Ref | ADR-021 |
| Detection | PENDING quá `[TBD: T]` without resolution. |
| Accounting | deposit_pending_3100_age > threshold. |
| Action | Ops: complete B or reverse A; alert fired. |
| **Forbidden** | Auto-fail silently. |
| Ops | Dashboard + ticket queue. |

**Ledger observables:** tra bảng Ch7 chính — verify 3100, 2110, 4110, `status`.



### D-F9: Period closed

| Field | Detail |
|-------|--------|
| Gherkin | X-E03 period close |
| ADR/Ref | ADR-023 |
| Detection | confirmDeposit when posting_date in closed period. |
| Accounting | ACCOUNTING_PERIOD_CLOSED error. |
| Action | Block phase B; ops waiver or next period. |
| **Forbidden** | Backdate posting. |
| Ops | Finance open period or waiver list. |

**Ledger observables:** tra bảng Ch7 chính — verify 3100, 2110, 4110, `status`.



### D-F10: TK inactive

| Field | Detail |
|-------|--------|
| Gherkin | Inactive COA |
| ADR/Ref | coa_account |
| Detection | 1111/3100/2110/4110 is_active=false. |
| Accounting | Reject at addLines/confirmDeposit. |
| Action | Error to orchestration. |
| **Forbidden** | Post to inactive TK. |
| Ops | COA admin activate account. |

**Ledger observables:** tra bảng Ch7 chính — verify 3100, 2110, 4110, `status`.



### D-F11: Worker crash

| Field | Detail |
|-------|--------|
| Gherkin | DEP-E06 mid-TX |
| ADR/Ref | ADR-035 |
| Detection | Worker dies mid transaction phase A or B. |
| Accounting | Partial coa_trans_data possible if no TX. |
| Action | Local TX rollback; retry idempotent. |
| **Forbidden** | Orphan lines without header balance. |
| Ops | INV-01 CI; manual cleanup if TX broken. |

**Ledger observables:** tra bảng Ch7 chính — verify 3100, 2110, 4110, `status`.



### D-F12: Same ref gross khác

| Field | Detail |
|-------|--------|
| Gherkin | Fee change replay conflict |
| ADR/Ref | AC-005 |
| Detection | Replay with different gross/fee same businessRef. |
| Accounting | Conflict on amount vs existing POSTED. |
| Action | Reject conflict; return existing POSTED state. |
| **Forbidden** | Overwrite amount. |
| Ops | Ops investigate bank duplicate ref issue. |

**Ledger observables:** tra bảng Ch7 chính — verify 3100, 2110, 4110, `status`.



### D-F13: Thiếu fee leg

| Field | Detail |
|-------|--------|
| Gherkin | Missing 4110 |
| ADR/Ref | ADR-009 |
| Detection | fee>0 but only net 2110 CR without fee legs. |
| Accounting | gross != net without 4110 line. |
| Action | Reject unbalanced confirmDeposit. |
| **Forbidden** | Post net-only. |
| Ops | Fix orchestration line builder. |

**Ledger observables:** tra bảng Ch7 chính — verify 3100, 2110, 4110, `status`.



### D-F14: Zero fee

| Field | Detail |
|-------|--------|
| Gherkin | Zero fee deposit |
| ADR/Ref | — |
| Detection | fee=0 valid POSTED. |
| Accounting | 4 lines only; 4110 unchanged. |
| Action | 2110 CR gross; wallet credit gross. |
| **Forbidden** | Force 4110 CR 0 line. |
| Ops | Normal — no ops action. |

**Ledger observables:** tra bảng Ch7 chính — verify 3100, 2110, 4110, `status`.



### D-F15: Multi-currency

| Field | Detail |
|-------|--------|
| Gherkin | Future FX |
| ADR/Ref | ADR-019 |
| Detection | Non-VND webhook v1. |
| Accounting | Currency != VND at boundary. |
| Action | Reject v1. |
| **Forbidden** | FX journal ad-hoc. |
| Ops | N/A v1 — VND only. |

**Ledger observables:** tra bảng Ch7 chính — verify 3100, 2110, 4110, `status`.


## Phụ lục F. T-account POSTED — 15 gross variants {#phụ-lục-f--t-account-variants}

#### F.1 gross=100,000 fee=1,000 net=99,000

```
1111 +100,000  |  3100 0  |  2110 +99,000  |  4110 +1,000  |  wallet +99,000
```


#### F.2 gross=75,000 fee=0 net=75,000

```
1111 +75,000  |  3100 0  |  2110 +75,000  |  4110 +0  |  wallet +75,000
```


#### F.3 gross=10,000 fee=10,000 net=0

```
1111 +10,000  |  3100 0  |  2110 +0  |  4110 +10,000  |  wallet +0
```


#### F.4 gross=500,000,000 fee=5,000 net=499,995,000

```
1111 +500,000,000  |  3100 0  |  2110 +499,995,000  |  4110 +5,000  |  wallet +499,995,000
```


#### F.5 gross=1 fee=0 net=1

```
1111 +1  |  3100 0  |  2110 +1  |  4110 +0  |  wallet +1
```


#### F.6 gross=50,000 fee=0 net=50,000

```
1111 +50,000  |  3100 0  |  2110 +50,000  |  4110 +0  |  wallet +50,000
```


#### F.7 gross=200,000 fee=2,000 net=198,000

```
1111 +200,000  |  3100 0  |  2110 +198,000  |  4110 +2,000  |  wallet +198,000
```


#### F.8 gross=999,999 fee=999 net=999,000

```
1111 +999,999  |  3100 0  |  2110 +999,000  |  4110 +999  |  wallet +999,000
```


#### F.9 gross=100,000 fee=500 net=99,500

```
1111 +100,000  |  3100 0  |  2110 +99,500  |  4110 +500  |  wallet +99,500
```


#### F.10 gross=25,000 fee=250 net=24,750

```
1111 +25,000  |  3100 0  |  2110 +24,750  |  4110 +250  |  wallet +24,750
```


#### F.11 gross=1,000,000 fee=10,000 net=990,000

```
1111 +1,000,000  |  3100 0  |  2110 +990,000  |  4110 +10,000  |  wallet +990,000
```


#### F.12 gross=100,000 fee=0 net=100,000

```
1111 +100,000  |  3100 0  |  2110 +100,000  |  4110 +0  |  wallet +100,000
```


#### F.13 gross=100,000 fee=99,000 net=1,000

```
1111 +100,000  |  3100 0  |  2110 +1,000  |  4110 +99,000  |  wallet +1,000
```


#### F.14 gross=100,000 fee=1 net=99,999

```
1111 +100,000  |  3100 0  |  2110 +99,999  |  4110 +1  |  wallet +99,999
```


#### F.15 gross=100,000 fee=50,000 net=50,000

```
1111 +100,000  |  3100 0  |  2110 +50,000  |  4110 +50,000  |  wallet +50,000
```

---

## Chương 21B. FAQ — giải thích mở rộng {#chương-21b-faq-extended}

**Q1. Tại sao không credit wallet ngay webhook?**

Webhook chỉ báo tiền vào VA — phase A ghi 1111/3100 PENDING. Member liability (2110) chưa xác định cho đến map VA + confirm. Credit sớm = cash basis — vi phạm ADR-036 và rủi ro double-count nếu phase B fail.


**Q2. 3100 ≠ 0 có phải lỗi?**

Chỉ lỗi khi status=POSTED. PENDING intentionally holds gross trên 3100 — visible ops và trial balance. POSTED bắt buộc 3100 net zero — ADR-010.


**Q3. Ai owns fee 1.000?**

Orchestration tính theo config merchant/tier `[TBD]` — accounting nhận số fee trong confirmDeposit. ADR-009 — accounting không có fee engine.


**Q4. confirmDeposit vs addLines?** — Xem Chương tương ứng và ADR binding trong Phụ lục C.


**Q5. Immutability POSTED?** — Xem Chương tương ứng và ADR binding trong Phụ lục C.


**Q6. Duplicate webhook?** — Xem Chương tương ứng và ADR binding trong Phụ lục C.


**Q7. VA mapping owner?** — Xem Chương tương ứng và ADR binding trong Phụ lục C.


**Q8. Amount mismatch policy?** — Xem Chương tương ứng và ADR binding trong Phụ lục C.


**Q9. Zero fee 4110?** — Xem Chương tương ứng và ADR binding trong Phụ lục C.


**Q10. Fee equals gross wallet?** — Xem Chương tương ứng và ADR binding trong Phụ lục C.


**Q11. Napas deposit?** — Xem Chương tương ứng và ADR binding trong Phụ lục C.


**Q12. JOIN prohibition?** — Xem Chương tương ứng và ADR binding trong Phụ lục C.


**Q13. W5 drift handling?** — Xem Chương tương ứng và ADR binding trong Phụ lục C.


**Q14. LOCKED wallet?** — Xem Chương tương ứng và ADR binding trong Phụ lục C.


**Q15. Unlock retry?** — Xem Chương tương ứng và ADR binding trong Phụ lục C.


**Q16. Period close PENDING?** — Xem Chương tương ứng và ADR binding trong Phụ lục C.


**Q17. Reverse scope PENDING?** — Xem Chương tương ứng và ADR binding trong Phụ lục C.


**Q18. Bank recall?** — Xem Chương tương ứng và ADR binding trong Phụ lục C.


**Q19. JournalPosted path?** — Xem Chương tương ứng và ADR binding trong Phụ lục C.


**Q20. messageId semantics?** — Xem Chương tương ứng và ADR binding trong Phụ lục C.


**Q21. mTLS requirement?** — Xem Chương tương ứng và ADR binding trong Phụ lục C.


**Q22. HTTP 202 meaning?** — Xem Chương tương ứng và ADR binding trong Phụ lục C.


**Q23. getDepositStatus API?** — Xem Chương tương ứng và ADR binding trong Phụ lục C.


**Q24. Aging threshold TBD?** — Xem Chương tương ứng và ADR binding trong Phụ lục C.


**Q25. Multi VA TBD?** — Xem Chương tương ứng và ADR binding trong Phụ lục C.


**Q26. Fee tier TBD?** — Xem Chương tương ứng và ADR binding trong Phụ lục C.


**Q27. Recall SLA TBD?** — Xem Chương tương ứng và ADR binding trong Phụ lục C.


**Q28. 2110 aggregate?** — Xem Chương tương ứng và ADR binding trong Phụ lục C.


**Q29. Cash basis rejected?** — Xem Chương tương ứng và ADR binding trong Phụ lục C.


**Q30. UI pending label?** — Xem Chương tương ứng và ADR binding trong Phụ lục C.


**Q31. S6 poison DLQ?** — Xem Chương tương ứng và ADR binding trong Phụ lục C.


**Q32. Worker crash TX?** — Xem Chương tương ứng và ADR binding trong Phụ lục C.


**Q33. Large 500M deposit?** — Xem Chương tương ứng và ADR binding trong Phụ lục C.


**Q34. Min 1 VND?** — Xem Chương tương ứng và ADR binding trong Phụ lục C.


**Q35. Sequential deposits?** — Xem Chương tương ứng và ADR binding trong Phụ lục C.


**Q36. 1111 statement recon?** — Xem Chương tương ứng và ADR binding trong Phụ lục C.


**Q37. 4110 fee report?** — Xem Chương tương ứng và ADR binding trong Phụ lục C.


**Q38. SQL INV-03?** — Xem Chương tương ứng và ADR binding trong Phụ lục C.


**Q39. Idempotent POSTED confirm?** — Xem Chương tương ứng và ADR binding trong Phụ lục C.


**Q40. Transit philosophy?** — Xem Chương tương ứng và ADR binding trong Phụ lục C.


**Q41. Orchestration saga states?** — Xem Chương tương ứng và ADR binding trong Phụ lục C.


**Q42. Outbox at-least-once?** — Xem Chương tương ứng và ADR binding trong Phụ lục C.


---

## Phụ lục G. DR/CR đầy đủ 10 kịch bản numeric {#phụ-lục-g--dr-cr-10-scenarios}

### G01 Happy — gross 100,000 fee 1,000 net 99,000

| Line | Phase | TK | DR | CR |
|------|-------|-----|-----|-----|
| 1 | A | 1111 | 100,000 | |
| 2 | A | 3100 | | 100,000 |
| 3 | B | 3100 | 100,000 | |
| 4 | B | 2110 | | 99,000 |
| 5 | B | 2110 | 1,000 | |
| 6 | B | 4110 | | 1,000 |


### G02 Zero fee — gross 75,000 fee 0 net 75,000

| Line | Phase | TK | DR | CR |
|------|-------|-----|-----|-----|
| 1 | A | 1111 | 75,000 | |
| 2 | A | 3100 | | 75,000 |
| 3 | B | 3100 | 75,000 | |
| 4 | B | 2110 | | 75,000 |


### G03 Fee=gross — gross 10,000 fee 10,000 net 0

| Line | Phase | TK | DR | CR |
|------|-------|-----|-----|-----|
| 1 | A | 1111 | 10,000 | |
| 2 | A | 3100 | | 10,000 |
| 3 | B | 3100 | 10,000 | |
| 4 | B | 2110 | | 0 |
| 5 | B | 2110 | 10,000 | |
| 6 | B | 4110 | | 10,000 |


### G04 Large — gross 500,000,000 fee 5,000 net 499,995,000

| Line | Phase | TK | DR | CR |
|------|-------|-----|-----|-----|
| 1 | A | 1111 | 500,000,000 | |
| 2 | A | 3100 | | 500,000,000 |
| 3 | B | 3100 | 500,000,000 | |
| 4 | B | 2110 | | 499,995,000 |
| 5 | B | 2110 | 5,000 | |
| 6 | B | 4110 | | 5,000 |


### G05 Min — gross 1 fee 0 net 1

| Line | Phase | TK | DR | CR |
|------|-------|-----|-----|-----|
| 1 | A | 1111 | 1 | |
| 2 | A | 3100 | | 1 |
| 3 | B | 3100 | 1 | |
| 4 | B | 2110 | | 1 |


### G06 50k — gross 50,000 fee 0 net 50,000

| Line | Phase | TK | DR | CR |
|------|-------|-----|-----|-----|
| 1 | A | 1111 | 50,000 | |
| 2 | A | 3100 | | 50,000 |
| 3 | B | 3100 | 50,000 | |
| 4 | B | 2110 | | 50,000 |


### G07 200k/2k — gross 200,000 fee 2,000 net 198,000

| Line | Phase | TK | DR | CR |
|------|-------|-----|-----|-----|
| 1 | A | 1111 | 200,000 | |
| 2 | A | 3100 | | 200,000 |
| 3 | B | 3100 | 200,000 | |
| 4 | B | 2110 | | 198,000 |
| 5 | B | 2110 | 2,000 | |
| 6 | B | 4110 | | 2,000 |


### G08 fee 500 — gross 100,000 fee 500 net 99,500

| Line | Phase | TK | DR | CR |
|------|-------|-----|-----|-----|
| 1 | A | 1111 | 100,000 | |
| 2 | A | 3100 | | 100,000 |
| 3 | B | 3100 | 100,000 | |
| 4 | B | 2110 | | 99,500 |
| 5 | B | 2110 | 500 | |
| 6 | B | 4110 | | 500 |


### G09 fee 50k — gross 100,000 fee 50,000 net 50,000

| Line | Phase | TK | DR | CR |
|------|-------|-----|-----|-----|
| 1 | A | 1111 | 100,000 | |
| 2 | A | 3100 | | 100,000 |
| 3 | B | 3100 | 100,000 | |
| 4 | B | 2110 | | 50,000 |
| 5 | B | 2110 | 50,000 | |
| 6 | B | 4110 | | 50,000 |


### G10 1M/10k — gross 1,000,000 fee 10,000 net 990,000

| Line | Phase | TK | DR | CR |
|------|-------|-----|-----|-----|
| 1 | A | 1111 | 1,000,000 | |
| 2 | A | 3100 | | 1,000,000 |
| 3 | B | 3100 | 1,000,000 | |
| 4 | B | 2110 | | 990,000 |
| 5 | B | 2110 | 10,000 | |
| 6 | B | 4110 | | 10,000 |

## Chương 22B. Review checklist — rationale {#chương-22b-checklist-rationale}
| # | Mục | Rationale ADR |
|---|-----|---------------|
| 1 | Phase A chỉ 1111+3100 | ADR-006 AC-006-01 |
| 2 | PENDING wallet unchanged | ADR-006 AC-006-02 |
| 3 | confirmDeposit only phase B | AC-006-03 |
| 4 | POSTED 3100=0 | ADR-010 AC-010-05 |
| 5 | fee legs when fee>0 | ADR-009 matching |
| 6 | business_ref idempotent | ADR-005 |
| 7 | Immutability POSTED | ADR-001 |
| 8 | Reversal not UPDATE | ADR-001 AC-001-02 |
| 9 | Wallet credit after POSTED | ADR-006 AC-006-05 |
| 10 | WALLET_CREDIT net not gross | ADR-024 |
| 11 | mTLS webhook | ADR-022 |
| 12 | 202 async ack | AC-006-07 |
| 13 | Outbox same TX | ADR-013 |
| 14 | No JOIN wallet accounting | ADR-003 |
| 15 | W5 report-only | ADR-014 |
| 16 | LOCKED reject credit | ADR-034 |
| 17 | VA map before B | ADR-030 |
| 18 | Period close block | ADR-023 |
| 19 | Accrual not cash basis | ADR-036 |
| 20 | Napas N/A deposit | 1111 only — not 1112 |

*(10 mục checklist còn lại — xem Ch22.)*

---

## Phụ lục H. Alignment processes.md §3 {#phụ-lục-h--processes-alignment}

| processes.md §3 step | Quyển III | Accounting action |
|---------------------|-----------|-------------------|
| Member CK VA | Ch1 | — |
| Webhook 202 | Ch11 | — |
| S6 BANK_DEPOSIT | Ch10 | — |
| Phase A PENDING | Ch3 | 1111 DR, 3100 CR |
| Map VA | Ch5 | — |
| Phase B POSTED | Ch4 | confirmDeposit |
| Wallet credit net | Ch6 | — (orchestration) |
| Error duplicate | Ch9 D-F1 | idempotent |
| Error unmapped VA | Ch5 D-F2 | hold PENDING |
| Bank recall | Ch8 | reversal |

Narrative đầy đủ: [`processes.md`](../../spec/processes.md) §3 · [`flows.md`](../../design/orchestration/flows.md).

---
## Phụ lục I. Mapping accounting.md §14 DEPOSIT {#phụ-lục-i--accounting-md-14}

| accounting.md §14 | Quyển III | Notes |
|-------------------|-----------|-------|
| §14.1 Obligation record 1111 without liability | Ch3 phase A | 1111 DR, no 2110 |
| §14.1 Hold gross 3100 PENDING | Ch3 observables O-A5 | transit ≠ 0 OK |
| §14.1 On confirm clear 3100, 2110+4110 | Ch4 phase B | confirmDeposit |
| §14.1 Emit JournalPosted | Ch10 | outbox ADR-013 |
| §14.1 Never depend wallet for POSTED | Ch6 boundary | ADR-006 |
| §14.2 Phase A table | Ch3 §3.2 | foundation §8 link |
| §14.2 Phase B table | Ch4 §4.2 | fee variants §4.5 |
| §14.3 Observables PENDING | Ch3 §3.4 | 10 checks |
| §14.3 Observables POSTED | Ch4 §4.4 | 3100=0 |
| §14.4 Failure matrix D-F1..15 | Ch7 + Ch7B + Ch7C | expanded |
| §14.5 Idempotency | Ch9 | business_ref |
| §14.6 Reconciliation | Ch15 | FR-10, W5, 4110 |

Contract English: [`accounting.md`](../accounting.md) §14 — Quyển III là bản tiếng Việt operational depth.

---
## Chương 3D. Bảng mười webhook phase A — observables {#chương-3d-webhook-observables}
| # | gross | ref | 1111 | 3100 | 2110 | wallet | status |
|---|-------|-----|------|------|------|--------|--------|
| W1 | 100,000 | `dep-1` | +100k | CR 100k | 0 | 0 | PENDING |
| W2 | 50,000 | `dep-1a` | +50k | CR 50k | 0 | 0 | PENDING |
| W3 | 75,000 | `dep-zf` | +75k | CR 75k | 0 | 0 | PENDING |
| W4 | 500,000,000 | `dep-lg` | +500M | CR 500M | 0 | 0 | PENDING |
| W5 | 1 | `dep-min` | +1 | CR 1 | 0 | 0 | PENDING |
| W6 | 100,000 | `dep-5` | +100k | CR 100k | 0 | 0 | PENDING unmapped |
| W7 | 100,000 | `dep-2` | idempotent | no 2nd DR | — | 0 | POSTED if replay |
| W8 | 100,000 | `dep-9` | single | one DR | one CR | 0 | PENDING |
| W9 | 80,000 | `dep-8` | +80k | CR 80k | 0 | 0 | PENDING→FAILED? |
| W10 | 200,000 | `dep-age` | +200k | CR 200k | 0 | 0 | PENDING aging |

---

## Chương 19. Gherkin catalog — 33 scenarios {#chương-19-gherkin-catalog--33-scenarios}

### 19.1 Part I — Feature Deposit (18 scenarios)

| # | Scenario | Given (tóm tắt) | When | Then (tóm tắt) |
|---|----------|-----------------|------|----------------|
| I-01 | Happy — 100k gross 1k fee | user wallet 0; webhook dep-1 100k | orchestration deposit fee=1000 | POSTED; 1111+100k 2110+99k 4110+1k; wallet 99k |
| I-02 | Phase A only PENDING | webhook dep-1a 50k mapped | phase A completes | PENDING; 3100 CR 50k; wallet unchanged |
| I-03 | Phase B confirm POSTED | PENDING dep-1b 3100 CR 50k | confirm fee=0 | POSTED; 1111 DR 50k 2110 CR 50k; wallet 50k |
| I-04 | Zero fee deposit | gross 75k fee 0 | deposit POSTED | 2110 CR 75k; 4110 unchanged; wallet 75k |
| I-05 | Duplicate webhook | dep-2 POSTED credited 99k | same webhook again | same coa_trans_id; no second DEPOSIT_CREDIT |
| I-06 | Duplicate phase B | dep-2b POSTED | confirm retried | still POSTED; line count unchanged |
| I-07 | PENDING wallet not credited | dep-3 PENDING 3100 CR 100k | credit before POSTED | rejected/not invoked; wallet unchanged |
| I-08 | POSTED consumer down | dep-4 POSTED 2110 +99k uncredited | retry WALLET_CREDIT | wallet 99k exactly once |
| I-09 | VA unmapped | dep-5 unknown VA | cannot resolve memberId | PENDING; 3100 holds 100k; ops alert |
| I-10 | Amount mismatch | PENDING dep-6 100k | confirm 99k | phase B rejected; wallet not credited |
| I-11 | Confirm exceeds gross | PENDING dep-7 100k | confirm 150k | rejected; 3100 holds original |
| I-12 | FAILED from PENDING | PENDING dep-8 3100 CR 80k | ops marks FAILED SLA | FAILED; reversal policy; wallet never credited |
| I-13 | Concurrent webhooks same ref | two workers dep-9 | both create journal | exactly one coa_trans; one POSTED |
| I-14 | JournalPosted once | dep-10 POSTED | outbox dispatches | one JournalPosted; duplicate no-op wallet |
| I-15 | Large precision | amount max minor units | POSTED | integer amounts; SUM DR=CR |
| I-16 | First wallet on credit | member 100999 no wallet | POSTED net 50k | wallet USER/VND created available 50k |
| I-17 | Wrong member corrected | PENDING dep-11 wrong map | ops remap confirm | POSTED credits correct member |
| I-18 | Fee change replay conflict | POSTED dep-12 fee 1000 | replay fee 2000 | idempotent; 4110 still 1000 |

### 19.2 Part II — Feature Deposit extended DEP-E (15 scenarios)

| # | Scenario | Given | When | Then |
|---|----------|-------|------|------|
| E-01 | DEP-E01 Min 1 unit | gross 1 fee 0 | POSTED | 2110 CR 1; wallet 1; 3100 zero |
| E-02 | DEP-E02 Large 500M | gross 500M fee 5k | POSTED | 2110 CR 499995000; 4110 5000 |
| E-03 | DEP-E03 Fee=gross net 0 | gross 10k fee 10k | POSTED | 2110 CR 0; 4110 10k; wallet 0/skip |
| E-04 | DEP-E04 Two sequential | user 99k after dep-a | dep-b 50k fee 0 POSTED | available 149k; two journals |
| E-05 | DEP-E05 PENDING aging | dep-age PENDING 200k 48h | aging job runs | ops alert; still PENDING |
| E-06 | DEP-E06 Worker crash mid-TX | PENDING dep-crash valid | phase B rollback crash | still PENDING; retry succeeds |
| E-07 | DEP-E07 JournalPosted lag | POSTED dep-evt emitted | no consumer 10min | recon flags lag; eventual credit |
| E-08 | DEP-E08 Duplicate credit command | WALLET_CREDIT twice | both processed | single DEPOSIT_CREDIT |
| E-09 | DEP-E09 Wrong member blocked | POSTED member 100234 | credit 100235 | reject/wrong target blocked |
| E-10 | DEP-E10 Bank recall reversal | POSTED dep-rec 99k credited | reversal journal | 2110 reduced; wallet debit policy |
| E-11 | DEP-E11 LOCKED wallet | USER LOCKED | POSTED net 50k | WALLET_CREDIT rejected; 2110 POSTED stands |
| E-12 | DEP-E12 Concurrent PENDING same ref | two workers phase A | same businessRef | exactly one PENDING journal |
| E-13 | DEP-E13 getDepositStatus PENDING | dep-st PENDING | client polls | returns PENDING; 3100 non-zero |
| E-14 | DEP-E14 getDepositStatus POSTED | dep-st2 POSTED credited | client polls | POSTED; wallet matches net |
| E-15 | DEP-E15 S6 poison DLQ | malformed envelope | max retries | DLQ + CommandFailed; no orphan POSTED |

**Tổng: 18 + 15 = 33 scenarios** — gate 150 acceptance. Pointer: [`acceptance.md`](../acceptance.md).

---
## Chương 21. FAQ deposit — 40 câu {#chương-21-faq-deposit--40-câu}
| # | Câu hỏi | Trả lời ngắn |
|---|---------|--------------|
| 1 | Credit wallet ngay webhook? | Không — PENDING phase A only. ADR-006. |
| 2 | 3100 ≠ 0 có lỗi? | Lỗi nếu POSTED; OK nếu PENDING. ADR-010. |
| 3 | Ai tính phí 1.000? | Orchestration — ADR-009. |
| 4 | Accounting có fee engine? | Không — nhận số từ orchestration. |
| 5 | Phase B qua addLines? | Không — confirmDeposit only. AC-006-03. |
| 6 | Sửa dòng POSTED? | Reversal journal — ADR-001. |
| 7 | Duplicate webhook? | Same coa_trans_id — idempotent. |
| 8 | VA chưa map? | Giữ PENDING — D-F2. |
| 9 | Confirm amount khác gross? | Reject B — D-F3. |
| 10 | Zero fee có ghi 4110? | Không — skip legs 5-6. |
| 11 | Fee = gross? | POSTED valid; net 0 — DEP-E03. |
| 12 | Napas có trong deposit? | N/A — 1111 only, không 1112. |
| 13 | JOIN wallet accounting? | Cấm — ADR-003. |
| 14 | W5 lệch 2110 vs wallet? | Report only — ADR-014. |
| 15 | LOCKED wallet POSTED? | 2110 POSTED; credit reject — ADR-034. |
| 16 | Unlock sau LOCKED? | Retry WALLET_CREDIT idempotent. |
| 17 | Period close + PENDING? | Block phase B — ADR-023. |
| 18 | Reverse PENDING chỉ B? | Không — scope A only 1111/3100. ADJ-E03. |
| 19 | Bank recall sau POSTED? | Reversal journal + wallet debit — Ch8. |
| 20 | JournalPosted vs WALLET_CREDIT? | Một path active — ADR-024. |
| 21 | messageId vs businessRef? | messageId transport only — ADR-005. |
| 22 | mTLS webhook? | Bắt buộc — ADR-022. |
| 23 | 202 response nghĩa gì? | Accepted async — chưa POSTED. |
| 24 | getDepositStatus PENDING? | Trả PENDING + 3100 hold — DEP-E13. |
| 25 | Aging PENDING bao lâu? | [TBD: threshold T ngày/giờ]. |
| 26 | Multi VA per member? | [TBD: VA rules]. |
| 27 | Fee tier merchant? | [TBD: fee schedule]. |
| 28 | Recall SLA auto? | [TBD: recall SLA]. |
| 29 | 2110 per member? | Không — aggregate control. ADR-020. |
| 30 | Cash basis v1? | Rejected — ADR-036. |
| 31 | UI pending inflow? | Saga label — không wallet column. ACC-E05. |
| 32 | Poison S6 message? | DLQ — DEP-E15. |
| 33 | Worker crash phase A? | TX rollback retry — D-F11. |
| 34 | Large 500M deposit? | Integer minor — DEP-E02. |
| 35 | Min 1 VND? | Supported — DEP-E01. |
| 36 | Two deposits same user? | Two journals — DEP-E04. |
| 37 | 1111 vs sao kê lệch? | FR-10 recon T+0/T+1 tolerance. |
| 38 | 4110 báo cáo? | Fee report management — Ch15. |
| 39 | SQL INV-03 deposit? | 3100 zero POSTED — Ch18. |
| 40 | Confirm idempotent POSTED? | No-op — D-F4. |

---
## Chương 22. Review checklist — 30 mục {#chương-22-review-checklist--30-mục}
| # | Mục kiểm | Pass |
|---|----------|------|
| 1 | Phase A chỉ 1111 DR + 3100 CR | ☐ |
| 2 | Phase A không ghi 2110/4110 | ☐ |
| 3 | Phase A wallet available unchanged | ☐ |
| 4 | confirmDeposit centralizes phase B | ☐ |
| 5 | Orchestration không addLines deposit B | ☐ |
| 6 | POSTED 3100 net zero per journal | ☐ |
| 7 | fee>0 có đủ legs 2110 DR + 4110 CR | ☐ |
| 8 | fee=0 skip 4110 | ☐ |
| 9 | gross = net + fee validated | ☐ |
| 10 | business_ref UNIQUE idempotent | ☐ |
| 11 | Duplicate webhook no double 1111 | ☐ |
| 12 | Duplicate confirm no-op POSTED | ☐ |
| 13 | VA map trước phase B | ☐ |
| 14 | Unmapped VA giữ PENDING | ☐ |
| 15 | Amount mismatch reject B | ☐ |
| 16 | Period close block B | ☐ |
| 17 | POSTED lines immutable | ☐ |
| 18 | Reversal dùng journal mới | ☐ |
| 19 | Reverse PENDING scope 1111/3100 only | ☐ |
| 20 | Wallet credit sau POSTED only | ☐ |
| 21 | WALLET_CREDIT amount = net not gross | ☐ |
| 22 | LOCKED reject credit not ledger | ☐ |
| 23 | JournalPosted or WALLET_CREDIT not both duplicate | ☐ |
| 24 | mTLS required webhook | ☐ |
| 25 | 202 not 200 sync deposit | ☐ |
| 26 | Outbox same TX as post | ☐ |
| 27 | S6 consumer idempotent businessRef | ☐ |
| 28 | FR-10 1111 recon scheduled | ☐ |
| 29 | W5 report-only no auto COA | ☐ |
| 30 | CI INV-01 INV-03 pass deposit fixtures | ☐ |

---

## Chương 20C. flows.md deposit alignment {#chương-20c-flows-alignment}

Theo [`flows.md`](../../design/orchestration/flows.md) deposit (async):

```
S1 webhook/notify → 202
  → RabbitMQ BANK_DEPOSIT
  → accounting: phase A PENDING (1111/3100)
  → accounting: confirmDeposit → POSTED
  → RabbitMQ WALLET_CREDIT (or consume JournalPosted → credit)
  → wallet: DEPOSIT_CREDIT (net amount from orchestration)
```

| flows.md step | Accounting owns? | Quyển III |
|---------------|------------------|-----------|
| 202 ack | No | Ch11 |
| BANK_DEPOSIT | No (worker) | Ch10 |
| phase A PENDING | **Yes** | Ch3 |
| confirmDeposit POSTED | **Yes** | Ch4 |
| WALLET_CREDIT | No | Ch6 |
| DEPOSIT_CREDIT net | No | Ch6 |

---

## Chương 19C. ACC-E accrual scenarios (deposit legs) {#chương-19c-acc-e-deposit}

| Scenario | Deposit relevance |
|----------|-------------------|
| ACC-E01 | Webhook only — PENDING, wallet 0 |
| ACC-E02 | POSTED before bank settlement — 2110 OK |
| ACC-E04 | Fee 4110 same POSTED journal as 2110 |
| ACC-E05 | UI deposit processing ≠ wallet column |

Pointer: [`acceptance.md`](../acceptance.md) Feature Accrual basis (ADR-036).

---

## Chương 22C. Pre-POSTED vs POSTED review gate {#chương-22c-pre-posted-gate}

| Gate | PENDING OK? | POSTED required? |
|------|-------------|------------------|
| 3100 ≠ 0 | Yes | No — must be 0 |
| 2110 CR | No | Yes (net) |
| 4110 CR | No | Yes if fee>0 |
| wallet credit | No | After POSTED |
| JournalPosted | No | Yes |
| Trial balance 3100 | Shows hold | Cleared |

### 22D. Sign-off roles

| Role | Signs off on |
|------|--------------|
| Backend accounting | Ch3–4, Ch7, Ch17–18 |
| Backend orchestration | Ch5–6, Ch9–10 |
| Ops | Ch12, Ch20, Ch20B |
| QA | Ch19 catalog — 33/33 green |
| Finance/Audit | Ch8 reversal, Ch15 recon |

---
## Phụ lục A. Ma trận kịch bản số {#phụ-lục-a--ma-trận-kịch-bản-số}
| ID | Mô tả | gross | fee | net | lines | status |
|----|-------|-------|-----|-----|-------|--------|
| V01 | Chuẩn | 100000 | 1000 | 99000 | 6 | POSTED |
| V02 | Zero fee | 75000 | 0 | 75000 | 4 | POSTED |
| V03 | Fee=gross | 10000 | 10000 | 0 | 6 | POSTED |
| V04 | Large | 500000000 | 5000 | 499995000 | 6 | POSTED |
| V05 | Min unit | 1 | 0 | 1 | 4 | POSTED |
| V06 | Phase A only | 100000 | — | — | 2 | PENDING |
| V07 | 50k zero fee B | 50000 | 0 | 50000 | 4 | POSTED |
## Phụ lục B. DR/CR line-by-line mọi biến thể {#phụ-lục-b--dr-cr-line-by-line-mọi-biến-thể}

### B.1 V01 — Chuẩn gross 100.000 fee 1.000 (foundation §8.1)

| Line | Phase | TK | DR | CR |
|------|-------|-----|-----|-----|
| 1 | A | 1111 | 100.000 | |
| 2 | A | 3100 | | 100.000 |
| 3 | B | 3100 | 100.000 | |
| 4 | B | 2110 | | 99.000 |
| 5 | B | 2110 | 1.000 | |
| 6 | B | 4110 | | 1.000 |

### B.2 V02 — Zero fee gross 75.000

| Line | Phase | TK | DR | CR |
|------|-------|-----|-----|-----|
| 1 | A | 1111 | 75.000 | |
| 2 | A | 3100 | | 75.000 |
| 3 | B | 3100 | 75.000 | |
| 4 | B | 2110 | | 75.000 |

### B.3 V03 — Fee equals gross net 0

| Line | Phase | TK | DR | CR |
|------|-------|-----|-----|-----|
| 1 | A | 1111 | 10.000 | |
| 2 | A | 3100 | | 10.000 |
| 3 | B | 3100 | 10.000 | |
| 4 | B | 2110 | | 0 |
| 5 | B | 2110 | 10.000 | |
| 6 | B | 4110 | | 10.000 |

### B.4 V04 — Large 500.000.000 fee 5.000

| Line | Phase | TK | DR | CR |
|------|-------|-----|-----|-----|
| 1 | A | 1111 | 500.000.000 | |
| 2 | A | 3100 | | 500.000.000 |
| 3 | B | 3100 | 500.000.000 | |
| 4 | B | 2110 | | 499.995.000 |
| 5 | B | 2110 | 5.000 | |
| 6 | B | 4110 | | 5.000 |

### B.5 Reverse phase A (FAILED) — gross 100.000

| Line | TK | DR | CR |
|------|-----|-----|-----|
| R1 | 1111 | | 100.000 |
| R2 | 3100 | 100.000 | |

### B.6 Full reversal POSTED recall — chuẩn

Đảo toàn bộ lines 1–6 journal gốc — xem [Ch8](#chương-8-reversal--bank-recall-adr-001).


## Phụ lục C. Bảng tra ADR deposit {#phụ-lục-c--bảng-tra-adr-deposit}
| ADR | Chủ đề | Quyển III |
|-----|--------|-----------|
| [ADR-001](../../adr/ADR-001-immutable-ledger.md) | Immutable ledger | Ch8 reversal |
| [ADR-005](../../adr/ADR-005-idempotency-key-strategy.md) | Idempotency business_ref | Ch9 |
| [ADR-006](../../adr/ADR-006-two-phase-deposit.md) | Two-phase deposit | Ch3–4 |
| [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md) | Fee ownership orchestration | Ch4 fee |
| [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md) | Transit 3100 zero POSTED | Ch3–4 |
| [ADR-013](../../adr/ADR-013-outbox-at-least-once-messaging.md) | Outbox at-least-once | Ch10 |
| [ADR-014](../../adr/ADR-014-reconciliation-w5-report-only.md) | W5 report-only | Ch15 |
| [ADR-021](../../adr/ADR-021-aging-jobs-async-pending.md) | Aging pending | Ch12 |
| [ADR-022](../../adr/ADR-022-mtls-bank-webhooks.md) | mTLS webhooks | Ch11 |
| [ADR-023](../../adr/ADR-023-accounting-period-close.md) | Period close | Ch13 |
| [ADR-024](../../adr/ADR-024-deposit-wallet-credit-dual-path.md) | Wallet credit dual path | Ch6 |
| [ADR-030](../../adr/ADR-030-virtual-account-deposit-mapping.md) | VA mapping | Ch5 |
| [ADR-031](../../adr/ADR-031-sql-ledger-invariant-ci.md) | SQL invariant CI | Ch18 |
| [ADR-034](../../adr/ADR-034-locked-wallet-deposit-credit-reject.md) | LOCKED deposit reject | Ch14 |
| [ADR-036](../../adr/ADR-036-accrual-basis-ledger-v1.md) | Accrual-like ledger | Ch2 |
## Đọc tiếp {#đọc-tiếp}

| Tiếp theo | File |
|-----------|------|
| Withdraw + IBFT | `vol-04-withdraw-ibft.md` *(chưa viết)* |
| COA 3100/1111/2110/4110 | [`vol-02-coa-handbook.md`](./vol-02-coa-handbook.md) |
| Nguyên tắc accrual/transit | [`vol-01-principles.md`](./vol-01-principles.md) |
| Saga narrative | [`processes.md`](../../spec/processes.md) §3 |
| DR/CR template | [`foundation.md`](../../spec/foundation.md) §8 |
| Acceptance Gherkin | [`acceptance.md`](../acceptance.md) |
| Orchestration flow | [`flows.md`](../../design/orchestration/flows.md) |

