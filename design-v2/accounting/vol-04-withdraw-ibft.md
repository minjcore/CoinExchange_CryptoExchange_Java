# Quyển IV — Rút tiền (Withdraw) & IBFT: freeze-settle, transit 3200/3400

**Trạng thái:** Bản mở rộng (~100 trang in) · **Phạm vi:** `core.accounting` — luồng WITHDRAW + IBFT · **Ngôn ngữ:** Tiếng Việt
**Transit:** 3200 (withdraw) · 3400 (IBFT) · **ADR chính:** 007, 025, 033, 005, 001, 010, 009, 008, 013, 023, 031, 036
**Async:** HTTP 200 accept + worker RabbitMQ S6 · **Bank rail:** Vietinbank payout → 1111 · **Napas:** IBFT → 1112 + 5100

**Số tiền ví dụ chuẩn — Withdraw:** principal **100.000** VND · phí rút **1.000** VND · gross debit ví **101.000** VND · bank out **100.000** qua **1111** · scale **4** HALF_UP ([ADR-028](../../adr/ADR-028-money-scale-four-half-up.md))

**Số tiền ví dụ chuẩn — IBFT:** cùng principal/fee/gross · Napas cost **500** qua **1112**/**5100** · lãi gộp net **500** (4130 − 5100)

Template DR/CR authoritative: [`foundation.md`](../../spec/foundation.md) §9 (withdraw) · §11 (IBFT) — **link, không duplicate nguyên văn**. COA deep dive: [`vol-02-coa-handbook.md`](./vol-02-coa-handbook.md) TK 1111, 1112, 2110, 3200, 3400, 4120, 4130, 5100. Nguyên tắc: [`vol-01-principles.md`](./vol-01-principles.md). Contract: [`accounting.md`](../accounting.md) §15, §18, O1. Acceptance: [`acceptance.md`](../acceptance.md). Saga: [`processes.md`](../../spec/processes.md) §4, §7, §13.4. Flow: [`flows.md`](../../design/orchestration/flows.md).

---
## Mục lục chi tiết

### Phần I — WITHDRAW (transit 3200)

| § | Tiêu đề |
|---|---------|
| [0](#chương-0-cách-đọc-và-so-sánh-deposit-vs-withdraw) | Cách đọc + so sánh deposit vs withdraw |
| [1](#chương-1-tóm-tắt-nghiệp-vụ--actors) | Tóm tắt, actors, sync wallet + async bank |
| [2](#chương-2-o1-open-coupling--posted-on-accept-vs-bank-ack) | O1 open coupling — POSTED on accept vs bank ACK |
| [3](#chương-3-freeze-settle-release-adr-007) | Freeze-settle-release (ADR-007) |
| [4](#chương-4-drcr-posted-single-phase--t-account-examples) | DR/CR POSTED single-phase — T-account examples |
| [5](#chương-5-ma-trận-lỗi-w-f1w-f10) | Failure W-F1..W-F10 expanded |
| [6](#chương-6-idempotency-sub-keys-settle-release-adr-005) | Idempotency sub-keys :settle :release |
| [7](#chương-7-messaging-withdraw_payout-s6) | Messaging WITHDRAW_PAYOUT S6 |
| [8](#chương-8-bank-poll-t2tmax-adr-033) | Bank poll T2/Tmax ADR-033 |
| [9](#chương-9-reversal-ops-when-bank-fails-after-posted) | Reversal ops when bank fails after POSTED |
| [10](#chương-10-recon-1111-vs-statement-2110-vs-wallet-frozen) | Recon 1111 vs statement, 2110 vs wallet frozen |
| [11](#chương-11-gherkin-catalog-withdraw-22-scenarios) | Gherkin catalog withdraw 22 scenarios |
| [12](#chương-12-faq-withdraw--25-câu) | FAQ withdraw 25 câu |

### Phần II — IBFT (transit 3400)

| § | Tiêu đề |
|---|---------|
| [13](#chương-13-tóm-tắt-ibft-vs-withdraw) | Tóm tắt IBFT vs withdraw — same wallet class, different bank rail |
| [14](#chương-14-adr-025--1112-not-1111-51004130) | ADR-025 — 1112 not 1111, 5100+4130, lãi gộp |
| [15](#chương-15-drcr-deep--t-accounts) | DR/CR deep + T-accounts |
| [16](#chương-16-failure-i-f1i-f3--inherit-w-f-class) | Failure I-F1..I-F3 + inherit W-F class |
| [17](#chương-17-napas-adapter-idempotency) | Napas adapter idempotency |
| [18](#chương-18-gherkin-catalog-ibft-10-scenarios) | Gherkin catalog IBFT 10 scenarios |
| [19](#chương-19-faq-ibft--15-câu) | FAQ IBFT 15 câu |

### Phần III — CHUNG

| § | Tiêu đề |
|---|---------|
| [20](#chương-20-so-sánh-withdraw-vs-ibft-matrix) | So sánh withdraw vs IBFT matrix |
| [21](#chương-21-state-machines-ascii) | State machines ascii (wallet + ledger timeline) |
| [22](#chương-22-s2-api-field-guide) | S2 API field guide |
| [23](#chương-23-sql-invariant-ci) | SQL invariant CI |
| [24](#chương-24-ops-runbook-outline) | Ops runbook outline |
| [25](#chương-25-review-checklist--35-mục) | Review checklist 35 mục |

### Phụ lục

| § | Nội dung |
|---|----------|
| [A](#phụ-lục-a--ma-trận-kịch-bản-số) | Ma trận kịch bản số |
| [B](#phụ-lục-b--drcr-line-by-line-mọi-biến-thể) | DR/CR line-by-line mọi biến thể |
| [C](#phụ-lục-c--bảng-tra-adr) | Bảng tra ADR |
| [D](#phụ-lục-d--saga-13.4-pointer) | Saga §13.4 pointer |
| [E](#phụ-lục-e--processes.md-alignment) | processes.md alignment |
| [F](#phụ-lục-f--accounting.md-1518-mapping) | accounting.md §15/§18 mapping |
| [G](#phụ-lục-g--numeric-t-account-gallery) | Numeric T-account gallery |
| [H](#phụ-lục-h--cross-flow-invariant) | Cross-flow invariant |
| [I](#phụ-lục-i--đọc-tiếp-vol-05) | Đọc tiếp vol-05 |
---
## Chương 0. Cách đọc và so sánh deposit vs withdraw {#chương-0-cách-đọc-và-so-sánh-deposit-vs-withdraw}

Quyển IV là **deep dive nghiệp vụ rút tiền (WITHDRAW) và chuyển liên ngân hàng (IBFT)** — bổ sung Quyển I (nguyên tắc accrual, transit, immutability), Quyển II (handbook TK 1111/1112/3200/3400/4120/4130/5100) và Quyển III (deposit hai pha) bằng **end-to-end lifecycle outflow**: freeze → accept 200 → POSTED → payout async → settle | release → recon.

| Vai trò | Đọc trước | Kết quả |
|---------|-----------|---------|
| Product | Ch0 + Ch1 + Ch2 [TBD] | O1 POSTED timing, fee tier, bank SLA |
| Backend accounting | Ch4 + Ch5 + Ch22–23 | `postWithdraw`, transit 3200/3400 validate |
| Backend orchestration | Ch3 + Ch6–8 | S6 WITHDRAW_PAYOUT, idempotency, poll |
| Ops / Kế toán | Ch9–10 + Ch24 | Frozen aging, reversal, FR-10 |
| QA | Ch11 + Ch18 | 22 + 10 Gherkin scenarios |
| Audit | Ch4 + Ch9 + Phụ lục B | DR/CR evidence, immutability |

### 0.1 Quan hệ Quyển I ↔ II ↔ III ↔ IV

```
Quyển I (nguyên tắc)     Quyển II (COA)           Quyển III (deposit)    Quyển IV (withdraw/IBFT)
────────────────────     ──────────────           ───────────────────    ─────────────────────────
ADR-036 accrual    ──►   TK 2110           ──►   credit sau POSTED  ──► DR 2110 on POSTED (O1)
ADR-010 transit=0  ──►   TK 3200/3400      ──►   3100 two-phase     ──► single-phase POSTED
ADR-007 freeze     ──►   wallet frozen     ──►   N/A inbound        ──► FREEZE before 200
ADR-025 IBFT rail  ──►   1112 not 1111     ──►   deposit 1111 only  ──► withdraw 1111 / IBFT 1112
```

### 0.2 Deposit vs Withdraw — bảng đối chiếu

| Khía cạnh | DEPOSIT (Quyển III) | WITHDRAW (Phần I) | IBFT (Phần II) |
|-----------|---------------------|-------------------|----------------|
| Hướng tiền | Bank → platform → ví | Ví → bank | Ví → Napas → bank khác |
| Transit | 3100 | 3200 | 3400 |
| Pha kế toán | Hai pha PENDING→POSTED | Một pha POSTED (O1) | Một pha POSTED (O1) |
| HTTP S1 | 202 async webhook | 200 sync accept | 200 sync accept |
| Wallet timing | Credit **sau** POSTED | **Freeze** trước 200; settle sau bank OK | Cùng class withdraw |
| Bank TK | 1111 DR (in) | 1111 CR (out) | 1112 CR (Napas) |
| Revenue TK | 4110 | 4120 | 4130 |
| Expense TK | N/A | N/A | 5100 (Napas cost) |
| Double-spend risk | Thấp (credit sau confirm) | **Cao** — timeout ≠ release | **Cao** — cùng saga |

### 0.3 Quy ước ký hiệu

| Ký hiệu | Ý nghĩa |
|---------|---------|
| principal / fee / gross | 100.000 / 1.000 / 101.000 (chuẩn withdraw & IBFT) |
| Napas cost | 500 (IBFT only) |
| net profit IBFT | fee − Napas = 500 → 4130 CR 1.000 − 5100 DR 500 |
| `business_ref` | Client idempotency key — [ADR-005](../../adr/ADR-005-idempotency-key-strategy.md) |
| `:settle` / `:release` | Wallet sub-keys — ADR-005 + ADR-007 |
| [TBD: ...] | Product gap — không implement ngầm |

### 0.4 Onboarding 10 câu

| # | Câu | Đáp |
|---|-----|-----|
| Q1 | Debit ví khi accept? | **Freeze** gross — không debit `available` trực tiếp [ADR-007](../../adr/ADR-007-freeze-settle-async-outflow.md) |
| Q2 | 3200 ≠ 0 lỗi? | POSTED bắt buộc 3200 net zero [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md) |
| Q3 | Ai tính fee? | Orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md) |
| Q4 | POSTED khi nào? | O1 — mặc định **on accept** per IMPLEMENTATION D5; `[TBD: bank ACK variant]` |
| Q5 | Timeout bank → release? | **Không** — poll đến terminal [processes.md](../../spec/processes.md) §13.4 |
| Q6 | Accounting RELEASE? | **Không** — orchestration/wallet only |
| Q7 | IBFT dùng 1111? | **Không** — 1112 [ADR-025](../../adr/ADR-025-ibft-napas-clearing-1112.md) |
| Q8 | Duplicate accept? | Idempotent — W-F2 |
| Q9 | Bank fail sau POSTED? | Ops reversal — không auto undo ledger [W-F4](../../design-v2/accounting.md) |
| Q10 | Period close? | Block post [ADR-023](../../adr/ADR-023-accounting-period-close.md) |
---
## Chương 1. Tóm tắt nghiệp vụ & actors {#chương-1-tóm-tắt-nghiệp-vụ--actors}

### 1.1 Mục tiêu nghiệp vụ — WITHDRAW

Member yêu cầu rút tiền về tài khoản ngân hàng đã đăng ký. Platform:

1. **Freeze** gross (principal + fee) trên ví USER **trước** HTTP 200.
2. Ghi nhận **POSTED** journal một pha — giảm **2110**, clear **3200**, ghi **4120** fee, giảm **1111** principal (theo O1 / foundation §9).
3. Enqueue **WITHDRAW_PAYOUT** — worker gọi bank adapter chuyển principal ra Vietinbank.
4. Bank **SUCCESS** → `WITHDRAW_SETTLE` (debit frozen). Bank **FAIL** terminal → `WITHDRAW_RELEASE` (unfreeze).

**Ví dụ chuẩn:** principal **100.000** · fee **1.000** · gross **101.000** · bank out **100.000** · Napas **N/A**.

### 1.2 Mục tiêu nghiệp vụ — IBFT

Member chuyển tiền ra ngân hàng khác qua Napas. Cùng wallet class withdraw; khác bank rail (**1112**, **5100**). Xem Phần II.

### 1.3 Bảng quyết định kiến trúc

| Khía cạnh | Quyết định | ADR |
|-----------|------------|-----|
| Wallet hold | FREEZE on accept — no `WITHDRAW_DEBIT` | [007](../../adr/ADR-007-freeze-settle-async-outflow.md) |
| Pha kế toán | Single-phase POSTED (O1 default: accept) | [010](../../adr/ADR-010-transit-accounts-net-zero.md) |
| HTTP S1 | 200 Accepted — sync wallet branch | [007](../../adr/ADR-007-freeze-settle-async-outflow.md) |
| Bank rail WD | Vietinbank payout → 1111 CR | foundation §9 |
| Bank rail IBFT | Napas → 1112 CR + 5100 DR | [025](../../adr/ADR-025-ibft-napas-clearing-1112.md) |
| Fee | Orchestration; user bears (added to gross) | [009](../../adr/ADR-009-fee-ownership-orchestration.md) |
| Recognition | Accrual-like — POSTED = 2110 DR | [036](../../adr/ADR-036-accrual-basis-ledger-v1.md) |
| Immutability | POSTED lines không UPDATE | [001](../../adr/ADR-001-immutable-ledger.md) |
| Idempotency | `business_ref` + `:settle`/`:release` | [005](../../adr/ADR-005-idempotency-key-strategy.md) |
| Saga | No 2PC; compensate forward-retry | [008](../../adr/ADR-008-saga-compensation-no-2pc.md) |

### 1.4 Actors & ranh giới trách nhiệm

| Actor | Trách nhiệm withdraw/IBFT | Không làm |
|-------|---------------------------|-----------|
| **Member** | Gọi createWithdrawal / createTransfer | Không gọi accounting S2 trực tiếp |
| **Gateway S1** | Auth, idempotency header, proxy orchestration | Không post journal |
| **Orchestration** | Fee calc; FREEZE; POSTED call; enqueue payout; SETTLE/RELEASE | Không JOIN wallet+accounting SQL |
| **core.wallet** | FREEZE / SETTLE / RELEASE | Không post 3200/3400 |
| **Worker S6** | Consume `WITHDRAW_PAYOUT` / `IBFT_PAYOUT` | Không tự RELEASE on timeout |
| **Bank adapter** | Payout idempotent on `business_ref` | Không mutate ledger |
| **core.accounting** | POSTED journal; emit JournalPosted | Không RELEASE frozen; không poll bank |
| **Ops** | Frozen aging; reversal approve; recon | Không UPDATE `coa_trans_data` |
| **Recon job** | FR-10 1111/1112; W5 frozen vs 2110 | Không auto-fix COA [014](../../adr/ADR-014-reconciliation-w5-report-only.md) |

### 1.5 Luồng tóm tắt WITHDRAW (ASCII)

```
Member ──createWithdrawal──► Gateway S1
                                │
                                ├── wallet: WITHDRAW_FREEZE 101k
                                ├── accounting: POSTED (3200=0)  [O1 on accept]
                                └── 200 ACCEPTED
                                │
                                ▼ RabbitMQ WITHDRAW_PAYOUT
                           Payout worker ──transfer 100k──► Vietinbank
                                │
                    SUCCESS ───┴─── FAIL (terminal)
                       │              │
                       ▼              ▼
              WITHDRAW_SETTLE    WITHDRAW_RELEASE
              frozen → 0         available restored
```

Chi tiết step order: [`processes.md`](../../spec/processes.md) §4 · [`flows.md`](../../design/orchestration/flows.md).

### 1.6 Trạng thái `coa_trans` — withdraw lifecycle (O1 accept)

| Status | 3200 net | 2110 Δ | 4120 | 1111 Δ | Wallet available | Wallet frozen |
|--------|----------|--------|------|--------|------------------|---------------|
| — (pre) | 0 | 0 | 0 | 0 | 200.000 | 0 |
| POSTED accept | 0 | −101.000 | +1.000 | −100.000 | 99.000 | 101.000 |
| After SETTLE | 0 | −101.000 | +1.000 | −100.000 | 99.000 | 0 |
| After RELEASE | 0 | −101.000* | +1.000* | −100.000* | 200.000* | 0 |

\* Sau RELEASE: ledger POSTED **không** tự đảo — cần ops reversal nếu policy yêu cầu (Ch9). Wallet restored; ledger có thể lệch tạm cho đến reversal.

### 1.7 Fee schedule [TBD]

`[TBD: Bảng phí rút theo tier/partner — v1 example flat 1.000 VND per processes.md §14.] `
---
## Chương 2. O1 open coupling — POSTED on accept vs bank ACK {#chương-2-o1-open-coupling--posted-on-accept-vs-bank-ack}

### 2.1 Câu hỏi O1 ([`accounting.md`](../accounting.md) §11)

| Policy | Mô tả | Ledger khi | Wallet khi |
|--------|-------|------------|------------|
| **A — POSTED on accept** (D5 default) | Ghi nhận 2110 DR + 3200 clear ngay khi FREEZE OK + 200 | Accept | Frozen giữ gross |
| **B — POSTED on bank ACK** | Chỉ POSTED sau bank SUCCESS | Bank SUCCESS | Frozen đến SETTLE |

**Trạng thái corpus:** O1 chưa chốt product — Quyển IV document **cả hai**; implement theo orchestration call; observables [`accounting.md`](../accounting.md) §15.4 giả định **Policy A**.

### 2.2 Policy A — timeline (mặc định D5)

```
T0  FREEZE 101k     wallet: avail 99k, frozen 101k
T1  POSTED journal  ledger: 2110 DR, 3200=0, 1111 CR, 4120 CR
T2  200 response    client sees accepted
T3  bank payout     1111 thực tế giảm (đã ghi T1 accrual)
T4  SETTLE          frozen → 0 (wallet mirror settle)
```

**Accrual rationale [ADR-036]:** Nghĩa vụ với member giảm khi accept có hiệu lực pháp lý — không chờ bank rail. Bank out là execution leg.

### 2.3 Policy B — timeline (alternative)

```
T0  FREEZE only     no POSTED yet
T1  200 response
T2  bank SUCCESS
T3  POSTED + SETTLE same saga step
```

Accounting module **không** chọn policy — orchestration quyết định **khi** gọi `postJournal`.

### 2.4 Coupling matrix

| Event | Policy A ledger | Policy B ledger | Wallet (cả hai) |
|-------|-----------------|-----------------|-----------------|
| Accept | POSTED | — | FREEZE |
| Bank UNKNOWN | POSTED unchanged | — | frozen unchanged |
| Bank FAIL | POSTED + ops reversal? | no POSTED | RELEASE |
| Bank SUCCESS | POSTED (done) | POSTED | SETTLE |

### 2.5 `[TBD: Product default confirmation]`

IMPLEMENTATION D5 + ADR-007 AC-007-06 nêu POSTED on accept. Nếu product chọn Policy B → cập nhật acceptance + Ch9 reversal matrix.
---
## Chương 3. Freeze-settle-release (ADR-007) {#chương-3-freeze-settle-release-adr-007}

### 3.1 Ranh giới wallet vs accounting

| Layer | Owns | Không owns |
|-------|------|------------|
| **Wallet** | `available`, `frozen`, `WITHDRAW_*` tx | `coa_trans`, RELEASE decision on timeout |
| **Accounting** | `coa_trans` POSTED, transit 3200/3400 | `wallet_balance`, RELEASE, SETTLE |

Accounting **không** gọi RELEASE — đây là điểm hay nhầm khi debug saga partial.

### 3.2 Ba lệnh wallet ([`processes.md`](../../spec/processes.md) §4.3)

| `tx_type` | Direction | Khi | Sub-key |
|-----------|-----------|-----|---------|
| `WITHDRAW_FREEZE` | FREEZE | Trước 200 | `business_ref` |
| `WITHDRAW_SETTLE` | DEBIT frozen | Bank SUCCESS | `{business_ref}:settle` |
| `WITHDRAW_RELEASE` | UNFREEZE | Bank FAIL terminal | `{business_ref}:release` |

IBFT: `IBFT_FREEZE` / `IBFT_SETTLE` / `IBFT_RELEASE` — cùng semantics.

> Không có `WITHDRAW_DEBIT` debiting `available` on accept — [ADR-007](../../adr/ADR-007-freeze-settle-async-outflow.md) AC-007-02.

### 3.3 Invariant freeze period

```
available + frozen = constant (trước và trong hold)
SETTLE: frozen -= gross; available unchanged
RELEASE: frozen -= gross; available += gross
```

### 3.4 State machine wallet (withdraw)

```
                    ┌──────────────┐
         accept     │   FROZEN     │
    ───────────────►│ frozen=gross │
                    └──────┬───────┘
                           │
              ┌────────────┼────────────┐
              ▼            ▼            ▼
         SETTLE OK    RELEASE OK    UNKNOWN
              │            │            │
              ▼            ▼            ▼
         frozen=0    avail restored   stay FROZEN
         (terminal)  (terminal)      (poll T2)
```

**Hard rule [§13.4]:** UNKNOWN/timeout **≠** RELEASE. Double-spend nếu release while payout may succeed.

### 3.5 SETTLE after RELEASE — forbidden

X-E12 / AC-007-06: state machine reject hoặc no-op. IBFT cùng rule.

### 3.6 LOCKED wallet during frozen

WD-E08: withdraw mới reject; settle in-flight có thể ops-gated. Accounting POSTED không phụ thuộc LOCKED.
---
## Chương 4. DR/CR POSTED single-phase — T-account examples {#chương-4-drcr-posted-single-phase--t-account-examples}

Authoritative template: [`foundation.md`](../../spec/foundation.md) §9 — Quyển IV minh họa T-account, không thay thế spec.

### 4.1 Line-by-line chuẩn (principal 100k, fee 1k)

| Step | TK | DR | CR | Diễn giải |
|------|-----|-----|-----|-----------|
| 1 | 2110 | 101,000 | — | User liability ↓ gross |
| 2 | 3200 | — | 101,000 | Transit hold gross |
| 3 | 3200 | 100,000 | — | Clear principal leg |
| 4 | 1111 | — | 100,000 | Bank asset ↓ principal |
| 5 | 3200 | 1,000 | — | Clear fee leg |
| 6 | 4120 | — | 1,000 | Withdraw fee revenue |

**Kết quả:** `2110 −101.000` · `1111 −100.000` · `4120 +1.000` · **3200 = 0**.

### 4.2 `coa_trans` sau POSTED

| `coa_trans` | |
|-------------|--|
| `use_case` | `WITHDRAW` |
| `business_ref` | client idempotency key (**UNIQUE**) |
| `status` | `POSTED` |

| `coa_trans_data` | `account_code` | DR | CR |
|------------------|----------------|----|----|
| line 1 | 2110 | 101,000 | 0 |
| line 2 | 3200 | 0 | 101,000 |
| line 3 | 3200 | 100,000 | 0 |
| line 4 | 1111 | 0 | 100,000 |
| line 5 | 3200 | 1,000 | 0 |
| line 6 | 4120 | 0 | 1,000 |

### 4.3 T-account gallery — happy path chuẩn

```
TK 2110 (LIABILITY)          TK 3200 (TRANSIT)           TK 1111 (ASSET)
──────────────────          ─────────────────           ────────────────
    DR 101.000                   CR 101.000                  CR 100.000
    (gross out)                  DR 100.000                  (bank payout)
                                 DR   1.000
                                 (net zero)

TK 4120 (REVENUE)
──────────────────
    CR   1.000
    (withdraw fee)
```

### 4.4 Biến thể — Zero fee

principal **100,000** · fee **0** · gross **100,000**. 2110 DR 100k; 4120 skip; frozen 100k.

```
2110 DR 100,000  |  3200 net 0  |  1111 CR 100,000  |  4120 CR 0  |  frozen 100,000
```

### 4.5 Biến thể — Min amount

principal **1** · fee **0** · gross **1**. WD-E09: 2110 DR 1; 1111 CR 1.

```
2110 DR 1  |  3200 net 0  |  1111 CR 1  |  4120 CR 0  |  frozen 1
```

### 4.6 Biến thể — Large principal

principal **500,000,000** · fee **5,000** · gross **500,005,000**. 2110 DR 500.005.000; scale 4 HALF_UP.

```
2110 DR 500,005,000  |  3200 net 0  |  1111 CR 500,000,000  |  4120 CR 5,000  |  frozen 500,005,000
```

### 4.7 Biến thể — Fee only on gross boundary

principal **50,000** · fee **500** · gross **50,500**. gross 50.500.

```
2110 DR 50,500  |  3200 net 0  |  1111 CR 50,000  |  4120 CR 500  |  frozen 50,500
```

### 4.8 Biến thể — Custom fee 2k

principal **200,000** · fee **2,000** · gross **202,000**. gross 202.000.

```
2110 DR 202,000  |  3200 net 0  |  1111 CR 200,000  |  4120 CR 2,000  |  frozen 202,000
```

### 4.9 Biến thể — Principal 75k fee 750

principal **75,000** · fee **750** · gross **75,750**. gross 75.750.

```
2110 DR 75,750  |  3200 net 0  |  1111 CR 75,000  |  4120 CR 750  |  frozen 75,750
```

### 4.10 Biến thể — Principal 1M fee 10k

principal **1,000,000** · fee **10,000** · gross **1,010,000**. gross 1.010.000.

```
2110 DR 1,010,000  |  3200 net 0  |  1111 CR 1,000,000  |  4120 CR 10,000  |  frozen 1,010,000
```

### 4.11 Biến thể — Fee 0 large

principal **10,000,000** · fee **0** · gross **10,000,000**. no 4120 line.

```
2110 DR 10,000,000  |  3200 net 0  |  1111 CR 10,000,000  |  4120 CR 0  |  frozen 10,000,000
```

### 4.12 Biến thể — Gross boundary avail

principal **101,000** · fee **0** · gross **101,000**. WD-E01 exact freeze.

```
2110 DR 101,000  |  3200 net 0  |  1111 CR 101,000  |  4120 CR 0  |  frozen 101,000
```

### 4.13 Biến thể — Two in-flight

principal **100,000** · fee **1,000** · gross **101,000**. WD-E06 sequential freeze.

```
2110 DR 101,000  |  3200 net 0  |  1111 CR 100,000  |  4120 CR 1,000  |  frozen 101,000
```

### 4.14 Validation guards (POSTED)

| Guard | Rule | Error |
|-------|------|-------|
| G-WD-01 | SUM DR = SUM CR | `ACCOUNTING_UNBALANCED_JOURNAL` |
| G-WD-02 | 3200 net zero | `TRANSIT_NONZERO_AT_POSTED` |
| G-WD-03 | fee>0 ⇒ 4120 line | WD-E05 reject |
| G-WD-04 | 1111 CR = principal only | not gross |
| G-WD-05 | No 5100 on withdraw | CK-19 vol-02 |
| G-WD-06 | use_case=WITHDRAW | — |
---
## Chương 5. Ma trận lỗi W-F1..W-F10 {#chương-5-ma-trận-lỗi-w-f1w-f10}

Mỗi ID mở rộng: detection · accounting · forbidden · ops · Gherkin · ADR.

| ID | Tình huống | Accounting action | Cấm | Gherkin | ADR |
|----|------------|-------------------|-----|---------|-----|
| W-F1 | Post với 3200 ≠ 0 sau template | Reject post | — | WD-E05 | 010 |
| W-F2 | Duplicate accept same ref | Idempotent return coa_trans_id | Second journal | Duplicate accept | 005 |
| W-F3 | Bank timeout UNKNOWN | No ledger change on timeout | Reverse on timeout | Bank timeout | 007,033 |
| W-F4 | Bank reject terminal | No auto reversal if POSTED on accept | Assume ledger undone | Bank reject release | 001,008 |
| W-F5 | Payout success, settle fails | Ledger unchanged | Auto reverse 2110 | SETTLE retry | 008 |
| W-F6 | Insufficient 2110 at post | Reject create/post | Post anyway | Insufficient | 026 |
| W-F7 | Same ref different gross | Conflict 409 | Overwrite | — | 005 |
| W-F8 | Period closed | Reject post | Backdate | X-E03 | 023 |
| W-F9 | Double payout bank | Adapter idempotent; single POSTED | Second 1111 CR | WD-E07 | 005,007 |
| W-F10 | Fee zero | 4120 unchanged / skip line | Force 4120 CR 0 | Zero fee | 009 |

### 5.1 Chi tiết W-F1: Post với 3200 ≠ 0 sau template

**Detection:** metric `withdraw_w-f1`, API error, hoặc W5/frozen drift job.
**Accounting action:** Xem bảng — accounting không RELEASE wallet.
**Ops playbook:** Escalate theo severity; không UPDATE `coa_trans_data` POSTED.
**Forbidden:** UPDATE/DELETE POSTED lines [ADR-001](../../adr/ADR-001-immutable-ledger.md).

**Recovery saga ([§13.4](../../spec/processes.md)):** forward-retry settle; RELEASE chỉ orchestration sau terminal bank FAIL.

### 5.2 Chi tiết W-F2: Duplicate accept same ref

**Detection:** metric `withdraw_w-f2`, API error, hoặc W5/frozen drift job.
**Accounting action:** Xem bảng — accounting không RELEASE wallet.
**Ops playbook:** Escalate theo severity; không UPDATE `coa_trans_data` POSTED.
**Forbidden:** UPDATE/DELETE POSTED lines [ADR-001](../../adr/ADR-001-immutable-ledger.md).

**Recovery saga ([§13.4](../../spec/processes.md)):** forward-retry settle; RELEASE chỉ orchestration sau terminal bank FAIL.

### 5.3 Chi tiết W-F3: Bank timeout UNKNOWN

**Detection:** metric `withdraw_w-f3`, API error, hoặc W5/frozen drift job.
**Accounting action:** Xem bảng — accounting không RELEASE wallet.
**Ops playbook:** Escalate theo severity; không UPDATE `coa_trans_data` POSTED.
**Forbidden:** UPDATE/DELETE POSTED lines [ADR-001](../../adr/ADR-001-immutable-ledger.md).

**Recovery saga ([§13.4](../../spec/processes.md)):** forward-retry settle; RELEASE chỉ orchestration sau terminal bank FAIL.

### 5.4 Chi tiết W-F4: Bank reject terminal

**Detection:** metric `withdraw_w-f4`, API error, hoặc W5/frozen drift job.
**Accounting action:** Xem bảng — accounting không RELEASE wallet.
**Ops playbook:** Escalate theo severity; không UPDATE `coa_trans_data` POSTED.
**Forbidden:** UPDATE/DELETE POSTED lines [ADR-001](../../adr/ADR-001-immutable-ledger.md).

**Recovery saga ([§13.4](../../spec/processes.md)):** forward-retry settle; RELEASE chỉ orchestration sau terminal bank FAIL.

### 5.5 Chi tiết W-F5: Payout success, settle fails

**Detection:** metric `withdraw_w-f5`, API error, hoặc W5/frozen drift job.
**Accounting action:** Xem bảng — accounting không RELEASE wallet.
**Ops playbook:** Escalate theo severity; không UPDATE `coa_trans_data` POSTED.
**Forbidden:** UPDATE/DELETE POSTED lines [ADR-001](../../adr/ADR-001-immutable-ledger.md).

**Recovery saga ([§13.4](../../spec/processes.md)):** forward-retry settle; RELEASE chỉ orchestration sau terminal bank FAIL.

### 5.6 Chi tiết W-F6: Insufficient 2110 at post

**Detection:** metric `withdraw_w-f6`, API error, hoặc W5/frozen drift job.
**Accounting action:** Xem bảng — accounting không RELEASE wallet.
**Ops playbook:** Escalate theo severity; không UPDATE `coa_trans_data` POSTED.
**Forbidden:** UPDATE/DELETE POSTED lines [ADR-001](../../adr/ADR-001-immutable-ledger.md).

**Recovery saga ([§13.4](../../spec/processes.md)):** forward-retry settle; RELEASE chỉ orchestration sau terminal bank FAIL.

### 5.7 Chi tiết W-F7: Same ref different gross

**Detection:** metric `withdraw_w-f7`, API error, hoặc W5/frozen drift job.
**Accounting action:** Xem bảng — accounting không RELEASE wallet.
**Ops playbook:** Escalate theo severity; không UPDATE `coa_trans_data` POSTED.
**Forbidden:** UPDATE/DELETE POSTED lines [ADR-001](../../adr/ADR-001-immutable-ledger.md).

**Recovery saga ([§13.4](../../spec/processes.md)):** forward-retry settle; RELEASE chỉ orchestration sau terminal bank FAIL.

### 5.8 Chi tiết W-F8: Period closed

**Detection:** metric `withdraw_w-f8`, API error, hoặc W5/frozen drift job.
**Accounting action:** Xem bảng — accounting không RELEASE wallet.
**Ops playbook:** Escalate theo severity; không UPDATE `coa_trans_data` POSTED.
**Forbidden:** UPDATE/DELETE POSTED lines [ADR-001](../../adr/ADR-001-immutable-ledger.md).

**Recovery saga ([§13.4](../../spec/processes.md)):** forward-retry settle; RELEASE chỉ orchestration sau terminal bank FAIL.

### 5.9 Chi tiết W-F9: Double payout bank

**Detection:** metric `withdraw_w-f9`, API error, hoặc W5/frozen drift job.
**Accounting action:** Xem bảng — accounting không RELEASE wallet.
**Ops playbook:** Escalate theo severity; không UPDATE `coa_trans_data` POSTED.
**Forbidden:** UPDATE/DELETE POSTED lines [ADR-001](../../adr/ADR-001-immutable-ledger.md).

**Recovery saga ([§13.4](../../spec/processes.md)):** forward-retry settle; RELEASE chỉ orchestration sau terminal bank FAIL.

### 5.0 Chi tiết W-F10: Fee zero

**Detection:** metric `withdraw_w-f10`, API error, hoặc W5/frozen drift job.
**Accounting action:** Xem bảng — accounting không RELEASE wallet.
**Ops playbook:** Escalate theo severity; không UPDATE `coa_trans_data` POSTED.
**Forbidden:** UPDATE/DELETE POSTED lines [ADR-001](../../adr/ADR-001-immutable-ledger.md).

**Recovery saga ([§13.4](../../spec/processes.md)):** forward-retry settle; RELEASE chỉ orchestration sau terminal bank FAIL.
---
## Chương 6. Idempotency sub-keys :settle :release (ADR-005) {#chương-6-idempotency-sub-keys-settle-release-adr-005}

### 6.1 Key namespace

| Key | Scope | Consumer |
|-----|-------|----------|
| `business_ref` | Accept FREEZE + POSTED journal | S1, S2, wallet |
| `{business_ref}:settle` | WITHDRAW_SETTLE / IBFT_SETTLE | wallet |
| `{business_ref}:release` | WITHDRAW_RELEASE / IBFT_RELEASE | wallet |
| `business_ref` (bank) | Payout adapter dedup | bank/Napas |

### 6.2 Conflict rules

Cùng `business_ref`, gross khác → `WALLET_DUPLICATE_CONFLICT` / 409. WD-E03: RELEASE terminal — retry accept no second freeze.

### 6.3 Test matrix

| Test | Action | Expected |
|------|--------|----------|
| T1 | Duplicate accept | single FREEZE + single POSTED |
| T2 | Duplicate :settle | frozen=0 after first |
| T3 | Duplicate :release | no-op same wallet_tx id |
| T4 | :settle after :release | reject X-E12 |
| T5 | Payout redelivery S6 | single bank movement WD-E07 |
---
## Chương 7. Messaging WITHDRAW_PAYOUT S6 {#chương-7-messaging-withdraw_payout-s6}

### 7.1 Envelope

| Field | Value |
|-------|-------|
| `command` | `WITHDRAW_PAYOUT` / `IBFT_PAYOUT` |
| `businessRef` | same as HTTP idempotency |
| `principal` | 100.000 (bank out, not gross) |
| `memberId` | payout beneficiary context |
| `bankAccountId` | registered account ref |

### 7.2 Sequence

```
Orchestration ──publish──► RabbitMQ S6 WITHDRAW_PAYOUT
                                │
                                ▼
                          Payout worker
                                │
                    ┌───────────┴───────────┐
                    ▼                       ▼
              bank SUCCESS              bank FAIL
                    │                       │
                    ▼                       ▼
            wallet :settle            wallet :release
```

### 7.3 Outbox JournalPosted [ADR-013]

POSTED journal + outbox same local TX. Kafka S3 optional consumer — wallet SETTLE do orchestration command, không phụ thuộc JournalPosted cho withdraw.
---
## Chương 8. Bank poll T2/Tmax ADR-033 {#chương-8-bank-poll-t2tmax-adr-033}

### 8.1 Config contract [TBD numeric values]

| Key | Purpose |
|-----|---------|
| `bank.poll.interval_seconds` | **T2** — interval giữa poll |
| `bank.poll.max_attempts` | Trước DLQ (default 3) |
| `wallet.frozen.alert_age_hours` | **Tmax** — alert frozen quá lâu |
| `bank.webhook.grace_seconds` | Chờ webhook trước poll đầu |

`[TBD: T2 và Tmax giá trị cụ thể — chờ bank SLA product publish.]`

### 8.2 Poll policy

UNKNOWN → schedule poll — **không RELEASE** (X-E13, WD-E02). SUCCESS sau N timeout → SETTLE once. Tmax → alert only — **không auto-release** [ADR-033](../../adr/ADR-033-bank-poll-t2-frozen-tmax.md).

### 8.3 IBFT shared namespace

Withdraw và IBFT dùng cùng config keys; discriminator `use_case` trên poll job.
---
## Chương 9. Reversal ops when bank fails after POSTED {#chương-9-reversal-ops-when-bank-fails-after-posted}

### 9.1 Khi nào cần reversal

Policy A: POSTED on accept + bank FAIL terminal + RELEASE wallet → ledger vẫn ghi 2110 DR. Ops post **reversal journal** + optional wallet adjustment nếu SETTLE chưa chạy.

### 9.2 Template reversal withdraw (gross 101k posted)

| Line | TK | DR | CR |
|------|-----|-----|-----|
| 1 | 4120 | 1.000 | |
| 2 | 1111 | 100.000 | |
| 3 | 3200 | | 101.000 |
| 4 | 3200 | 101.000 | |
| 5 | 2110 | | 101.000 |

Journal mới `reverses_id=<original>`, `business_ref={orig}-REV` — [ADR-001](../../adr/ADR-001-immutable-ledger.md).

### 9.3 Bank success sau RELEASE (edge)

Hiếm — adapter idempotent should prevent. Nếu xảy ra: ops incident P1; không auto RELEASE on UNKNOWN.
---
## Chương 10. Recon 1111 vs statement, 2110 vs wallet frozen {#chương-10-recon-1111-vs-statement-2110-vs-wallet-frozen}

### 10.1 FR-10 — 1111 vs Vietinbank statement

POSTED 1111 CR 100k per withdraw phải match payout line trên sao kê. Payout in-flight: ledger đã CR (Policy A) — bank file có thể lag 1 ngày.

### 10.2 W5 — 2110 vs wallet

Control: SUM wallet USER `available+frozen` ≈ **2110** aggregate. Trong hold: `available` giảm, `frozen` tăng — 2110 đã DR (Policy A).

### 10.3 Frozen aging dashboard

Panels: frozen count, max age, SUM frozen by use_case, poll UNKNOWN queue. Tmax alert [ADR-021](../../adr/ADR-021-aging-jobs-async-pending.md).
---
## Chương 11. Gherkin catalog withdraw — 22 scenarios {#chương-11-gherkin-catalog-withdraw-22-scenarios}

Nguồn: [`acceptance.md`](../acceptance.md) Feature Withdraw (12) + WD-E (10). Foundation §9 authoritative cho ledger lines.

### WD-01: Happy — freeze then settle

```gherkin
Feature: Withdraw
Scenario: Happy — freeze then settle
Given user available=200000
When withdraw businessRef="wd-1" principal=100000 fee=1000
Then WITHDRAW_FREEZE gross 101000 before 200
And journal POSTED 3200=0
```

### WD-02: Ledger POSTED lines on accept

```gherkin
Feature: Withdraw
Scenario: Ledger POSTED lines on accept
Given principal=100000 fee=1000 gross=101000
When accept returns 200
Then POSTED: 2110 DR 101000, 3200 net zero, 1111 CR 100000, 4120 CR 1000
```

### WD-03: Bank reject — release

```gherkin
Feature: Withdraw
Scenario: Bank reject — release
Given withdraw "wd-2" frozen 101000
When bank returns reject
Then WITHDRAW_RELEASE
And available restored
```

### WD-04: Bank timeout — do not release

```gherkin
Feature: Withdraw
Scenario: Bank timeout — do not release
Given "wd-3" frozen 101000
When payout times out unknown
Then funds remain frozen
And no WITHDRAW_RELEASE until terminal
```

### WD-05: Duplicate accept

```gherkin
Feature: Withdraw
Scenario: Duplicate accept
Given "wd-4" already frozen
When client retries same key
Then 200 same state
And single WITHDRAW_FREEZE
```

### WD-06: Insufficient available

```gherkin
Feature: Withdraw
Scenario: Insufficient available
Given available=50000
When withdraw gross=101000
Then 4xx insufficient
And no journal POSTED
```

### WD-07: SETTLE idempotent

```gherkin
Feature: Withdraw
Scenario: SETTLE idempotent
Given "wd-5" frozen POSTED
When bank success triggers settle twice
Then frozen=0 after first
```

### WD-08: RELEASE explicit REJECTED

```gherkin
Feature: Withdraw
Scenario: RELEASE explicit REJECTED
Given "wd-6" frozen
When bank REJECTED
Then RELEASE once
```

### WD-09: SETTLE without freeze — rejected

```gherkin
Feature: Withdraw
Scenario: SETTLE without freeze — rejected
Given no FREEZE for "wd-7"
When settle arrives
Then wallet rejects
```

### WD-10: Double RELEASE idempotent

```gherkin
Feature: Withdraw
Scenario: Double RELEASE idempotent
Given "wd-8" RELEASED
When release retried
Then balances unchanged
```

### WD-11: POSTED before freeze prevented

```gherkin
Feature: Withdraw
Scenario: POSTED before freeze prevented
Given orchestration freeze-first
Then no orphan POSTED without frozen
```

### WD-12: Zero fee withdraw

```gherkin
Feature: Withdraw
Scenario: Zero fee withdraw
Given principal=100000 fee=0
When POSTED
Then 2110 DR 100000
And frozen=100000
```

### WD-E01: Freeze exact boundary

```gherkin
Feature: Withdraw
Scenario: Freeze exact boundary
Given available=101000
When gross=101000
Then freeze succeeds available=0
```

### WD-E01b: One over available

```gherkin
Feature: Withdraw
Scenario: One over available
Given available=101000
When gross=101001
Then reject before freeze
```

### WD-E02: Poll SUCCESS after timeouts

```gherkin
Feature: Withdraw
Scenario: Poll SUCCESS after timeouts
Given "wd-poll" frozen
When unknown then SUCCESS
Then SETTLE once no RELEASE
```

### WD-E03: RELEASE then retry accept

```gherkin
Feature: Withdraw
Scenario: RELEASE then retry accept
Given "wd-rel" RELEASED
When retry same key
Then terminal released no second freeze
```

### WD-E04: SETTLE retry 5x

```gherkin
Feature: Withdraw
Scenario: SETTLE retry 5x
Given bank SUCCESS
When settle fails 4x then ok
Then frozen=0 once
```

### WD-E05: POSTED 3200 guard

```gherkin
Feature: Withdraw
Scenario: POSTED 3200 guard
Given post missing 4120 when fee>0
Then post rejected
```

### WD-E06: Two withdraws in flight

```gherkin
Feature: Withdraw
Scenario: Two withdraws in flight
Given frozen 101k available=99000
When second gross=99000
Then second freeze if sufficient
```

### WD-E07: S6 duplicate payout

```gherkin
Feature: Withdraw
Scenario: S6 duplicate payout
Given payout redelivered
When adapter idempotent
Then single bank movement
```

### WD-E08: LOCKED after freeze

```gherkin
Feature: Withdraw
Scenario: LOCKED after freeze
Given frozen in flight wallet LOCKED
Then new withdraw rejected
```

### WD-E09: Principal 1 fee 0

```gherkin
Feature: Withdraw
Scenario: Principal 1 fee 0
Given principal=1
When POSTED SETTLE
Then 2110 DR 1 1111 CR 1
```

### WD-E10: SETTLE without freeze

```gherkin
Feature: Withdraw
Scenario: SETTLE without freeze
Given SETTLE without freeze
Then wallet rejects
```

---
## Chương 12. FAQ withdraw — 25 câu {#chương-12-faq-withdraw--25-câu}

**Q1. Tại sao freeze thay vì debit available?**

Tránh double-spend khi bank timeout — ADR-007.

**Q2. 3200 khác 0 sau POSTED?**

Lỗi — reject G-WD-02.

**Q3. Ai gọi RELEASE?**

Orchestration sau bank FAIL terminal — không accounting.

**Q4. POSTED trước bank payout?**

O1 Policy A default D5 — accrual ADR-036.

**Q5. Timeout có release?**

Không — §13.4 hard rule.

**Q6. 4120 vs 4130 withdraw?**

Withdraw dùng 4120 only — 4130 là transfer/IBFT.

**Q7. 1111 CR bao nhiêu?**

Principal only — 100.000, không gross.

**Q8. Có 5100 withdraw?**

Không — CK-19 vol-02.

**Q9. SETTLE làm gì ledger?**

Không đổi — wallet only.

**Q10. Bank fail sau POSTED?**

RELEASE wallet + ops reversal ledger Ch9.

**Q11. Duplicate accept?**

Idempotent W-F2.

**Q12. Insufficient?**

Reject trước freeze W-F6.

**Q13. Hai withdraw song song?**

WD-E06 nếu available đủ từng gross.

**Q14. Frozen + LOCKED?**

WD-E08 — settle có thể ops-gated.

**Q15. Poll T2 giá trị?**

[TBD: bank SLA] ADR-033.

**Q16. Tmax auto-release?**

Không — alert only.

**Q17. Fee tier?**

[TBD: fee schedule config].

**Q18. Period close?**

Block W-F8.

**Q19. Immutability?**

Reversal not UPDATE ADR-001.

**Q20. W5 drift frozen?**

Report only ADR-014.

**Q21. JOIN wallet accounting?**

Forbidden ADR-003.

**Q22. ACC-E03 nghĩa?**

Freeze on accept — frozen trước SETTLE.

**Q23. flows.md post timing?**

Orchestration owns call — O1 resolves accept vs settle.

**Q24. Net profit IBFT?**

Không áp withdraw — xem Ch14.

**Q25. Đọc tiếp?**

vol-05 payment-transfer.

---
## Chương 13. Tóm tắt IBFT vs withdraw {#chương-13-tóm-tắt-ibft-vs-withdraw}

IBFT và withdraw **cùng wallet class** freeze-settle-release [ADR-007](../../adr/ADR-007-freeze-settle-async-outflow.md). Khác: transit **3400**, bank rail **1112**, revenue **4130**, expense **5100** Napas cost.

| | WITHDRAW | IBFT |
|--|----------|------|
| Transit | 3200 | 3400 |
| Bank TK | 1111 Vietinbank | 1112 Napas Clearing |
| Revenue | 4120 | 4130 |
| Expense | — | 5100 (500) |
| Net P&L | +1.000 fee | +500 (1.000−500) |
| Wallet tx | WITHDRAW_* | IBFT_* |

Process: [`processes.md`](../../spec/processes.md) §7 · Foundation: §11.
---
## Chương 14. ADR-025 — 1112 not 1111, 5100+4130, lãi gộp {#chương-14-adr-025--1112-not-1111-51004130}

### 14.1 Decision summary

1. IBFT dùng transit **3400** — không 3200.
2. Bank leg **1112** — reject 1111 (I-O8 orchestration, IBFT-E05).
3. Fee **4130** CR; Napas cost **5100** DR + **1112** CR.
4. Lãi gộp = 4130 − 5100 = **500** trên ví dụ chuẩn.

### 14.2 Matching principle

5100 chỉ khi có bank cost thực — foundation §11 steps 8–9. Không ghi 5100 trên withdraw.

### 14.3 AC-025 mapping

| AC | Quyển IV |
|----|----------|
| AC-025-01 | Ch15 3400=0 |
| AC-025-02 | Ch15 1112 bank leg |
| AC-025-03 | Ch16 I-F1 5100 present |
| AC-025-04 | Ch16 reject 1111 |
| AC-025-05 | Ch3 IBFT_* wallet |
---
## Chương 15. DR/CR deep + T-accounts {#chương-15-drcr-deep--t-accounts}

Authoritative: [`foundation.md`](../../spec/foundation.md) §11.

| Step | TK | DR | CR |
|------|-----|-----|-----|
| 1 | 2110 | 101,000 | — |
| 2 | 3400 | — | 101,000 |
| 3 | 3400 | 1,000 | — |
| 4 | 4130 | — | 1,000 |
| 5 | 3400 | 100,000 | — |
| 6 | 1112 | — | 100,000 |
| 8 | 5100 | 500 | — |
| 9 | 1112 | — | 500 |

### 15.1 T-account IBFT happy

```
2110          3400              1112              4130        5100
DR 101k       CR 101k           CR 100k           CR 1k       DR 500
              DR 1k             CR 500
              DR 100k           (net −100.5k)
              (net 0)
```

**1112 net:** −100.000 − 500 = −100.500 · **P&L:** 1.000 − 500 = **+500**.

### 15.x Zero Napas cost [hypothetical]

principal 100,000 fee 1,000 Napas 0 gross 101,000 profit 1,000


### 15.x Large 50M

principal 50,000,000 fee 50,000 Napas 25,000 gross 50,050,000 profit 25,000


### 15.x Min principal 1

principal 1 fee 0 Napas 0 gross 1 profit 0


### 15.x Fee 2k Napas 1k

principal 200,000 fee 2,000 Napas 1,000 gross 202,000 profit 1,000

---
## Chương 16. Failure I-F1..I-F3 + inherit W-F {#chương-16-failure-i-f1i-f3--inherit-w-f-class}

| ID | IBFT-specific | Rule |
|----|---------------|------|
| I-F1 | 5100 line missing when required | Reject post |
| I-F2 | Wrong clearing 1111 | Must 1112 |
| I-F3 | Double Napas submit | Adapter idempotent |

**Inherited W-F3–W-F5:** timeout ≠ ledger reversal; settle retry; RELEASE orchestration only — [`accounting.md`](../accounting.md) §18.2.

### 16.1 I-F1

**Rule:** 5100 missing. **Playbook:** giống W-F tương ứng + verify 1112 statement Napas.


### 16.2 I-F2

**Rule:** 1111 wrong. **Playbook:** giống W-F tương ứng + verify 1112 statement Napas.


### 16.3 I-F3

**Rule:** double submit. **Playbook:** giống W-F tương ứng + verify 1112 statement Napas.

---
## Chương 17. Napas adapter idempotency {#chương-17-napas-adapter-idempotency}

Payout key = `business_ref`. Redelivery S6 → single Napas movement. Poll backstop [ADR-033]. Wire spec: [`napas-api-portal-intro.md`](../../references/napas-api-portal-intro.md) — gated portal.

| Check | Expected |
|-------|----------|
| Same ref replay | Same napasTxnId / no second debit 1112 |
| UNKNOWN | Poll — no RELEASE |
| SUCCESS duplicate callback | SETTLE idempotent |
---
## Chương 18. Gherkin catalog IBFT — 10 scenarios {#chương-18-gherkin-catalog-ibft-10-scenarios}

### IBFT-01: Happy settle

```gherkin
Scenario: Happy settle
Given available=300000
When IBFT ref="ibft-1" principal=100000 fee=1000
Then IBFT_FREEZE 101000
And POSTED 3400=0
```

### IBFT-02: POSTED on accept

```gherkin
Scenario: POSTED on accept
Given principal=100000 fee=1000
When accept
Then 2110 DR 101000 4130 CR 1000 1112 CR 100000 5100 DR 500
```

### IBFT-03: Bank fail release

```gherkin
Scenario: Bank fail release
Given "ibft-2" frozen
When Napas fail
Then IBFT_RELEASE
```

### IBFT-04: Timeout frozen

```gherkin
Scenario: Timeout frozen
Given "ibft-3" frozen
When timeout unknown
Then no RELEASE
```

### IBFT-05: SETTLE idempotent

```gherkin
Scenario: SETTLE idempotent
Given SETTLE twice
Then frozen=0 once
```

### IBFT-E01: 5100 on ledger

```gherkin
Scenario: 5100 on ledger
Given POSTED per §11
Then 5100 DR present 3400=0
```

### IBFT-E02: 72h unknown FAIL

```gherkin
Scenario: 72h unknown FAIL
Given frozen 3 days
When FAILED
Then RELEASE once
```

### IBFT-E03: Duplicate :settle

```gherkin
Scenario: Duplicate :settle
Given SETTLE done
When :settle replay
Then idempotent
```

### IBFT-E04: Insufficient

```gherkin
Scenario: Insufficient
Given available < gross
Then reject no POSTED
```

### IBFT-E05: 1112 not 1111

```gherkin
Scenario: 1112 not 1111
Given POSTED
Then bank leg 1112
```

---
## Chương 19. FAQ IBFT — 15 câu {#chương-19-faq-ibft--15-câu}

**Q1. IBFT khác withdraw?**

3400, 1112, 4130, 5100 — cùng freeze class.

**Q2. Tại sao 1112?**

Napas clearing ADR-025 — không Vietinbank 1111.

**Q3. Lãi gộp 500?**

4130 1.000 − 5100 500.

**Q4. Withdraw fee TK?**

4120 — không 4130.

**Q5. 5100 khi nào?**

Khi có Napas cost template.

**Q6. RELEASE ai gọi?**

Orchestration — không accounting.

**Q7. Timeout?**

Giữ frozen — poll T2.

**Q8. Double Napas?**

I-F3 adapter idempotent.

**Q9. Reversal IBFT?**

Đảo 4130/5100/1112/3400/2110 — ops Ch9 pattern.

**Q10. W5 IBFT?**

2110 DR + frozen mirror Policy A.

**Q11. Poll config?**

[TBD T2/Tmax] ADR-033.

**Q12. S6 command?**

IBFT_PAYOUT.

**Q13. Period close?**

W-F8 inherit.

**Q14. O1 IBFT?**

Cùng withdraw O1 Ch2.

**Q15. Đọc tiếp?**

vol-05 payment-transfer.

---
## Chương 20. So sánh withdraw vs IBFT matrix {#chương-20-so-sánh-withdraw-vs-ibft-matrix}

| Dimension | WITHDRAW | IBFT |
|-----------|----------|------|
| foundation | §9 | §11 |
| processes | §4 | §7 |
| accounting.md | §15 | §18 |
| acceptance | 12+10 | 5+5 |
| transit | 3200 | 3400 |
| sync wallet | FREEZE | IBFT_FREEZE |
| async bank | Vietinbank 1111 | Napas 1112 |
| revenue | 4120 | 4130 |
| expense | — | 5100 |
| saga | §13.4 | §13.5 |
| double-spend rule | yes | yes |
---
## Chương 21. State machines ascii {#chương-21-state-machines-ascii}

### 21.1 Wallet + ledger timeline (Policy A withdraw)

```
time ─────────────────────────────────────────────────────────────►
      FREEZE    POSTED      payout      SETTLE
wallet avail↓↓  avail       avail       avail
       frozen↑  frozen↑     frozen↑     frozen→0
ledger  —       2110 DR     —           —
                3200=0
                1111 CR
```

### 21.2 Saga terminal states

| Terminal | Wallet | Ledger (A) | Bank |
|----------|--------|------------|------|
| SUCCESS | SETTLE frozen=0 | POSTED | paid |
| FAIL | RELEASE | POSTED + reversal? | rejected |
| UNKNOWN | frozen hold | POSTED | poll |
---
## Chương 22. S2 API field guide {#chương-22-s2-api-field-guide}

### 22.1 postJournal WITHDRAW

```json
{
  "useCase": "WITHDRAW",
  "referenceId": "wd-client-ref-001",
  "status": "POSTED",
  "lines": [
    {"accountCode": "2110", "debit": 101000, "credit": 0},
    {"accountCode": "3200", "debit": 0, "credit": 101000},
    {"accountCode": "3200", "debit": 100000, "credit": 0},
    {"accountCode": "1111", "debit": 0, "credit": 100000},
    {"accountCode": "3200", "debit": 1000, "credit": 0},
    {"accountCode": "4120", "debit": 0, "credit": 1000}
  ]
}
```

### 22.2 postJournal IBFT

```json
{
  "useCase": "IBFT",
  "referenceId": "ibft-ref-001",
  "status": "POSTED",
  "lines": [
    {"accountCode": "2110", "debit": 101000, "credit": 0},
    {"accountCode": "3400", "debit": 0, "credit": 101000},
    {"accountCode": "3400", "debit": 1000, "credit": 0},
    {"accountCode": "4130", "debit": 0, "credit": 1000},
    {"accountCode": "3400", "debit": 100000, "credit": 0},
    {"accountCode": "1112", "debit": 0, "credit": 100000},
    {"accountCode": "5100", "debit": 500, "credit": 0},
    {"accountCode": "1112", "debit": 0, "credit": 500}
  ]
}
```
---
## Chương 23. SQL invariant CI {#chương-23-sql-invariant-ci}

```sql
-- WITHDRAW POSTED: 3200 net zero
SELECT t.business_ref FROM coa_trans t
JOIN coa_trans_data d ON d.coa_trans_id = t.id
WHERE t.use_case='WITHDRAW' AND t.status='POSTED'
GROUP BY t.id
HAVING SUM(CASE WHEN d.account_code='3200' THEN d.credit - d.debit ELSE 0 END) <> 0;

-- IBFT: no 1111 on bank leg
SELECT t.business_ref FROM coa_trans t
JOIN coa_trans_data d ON d.coa_trans_id = t.id
WHERE t.use_case='IBFT' AND d.account_code='1111';

-- IBFT POSTED: 3400 net zero
SELECT t.business_ref FROM coa_trans t
JOIN coa_trans_data d ON d.coa_trans_id = t.id
WHERE t.use_case='IBFT' AND t.status='POSTED'
GROUP BY t.id
HAVING SUM(CASE WHEN d.account_code='3400' THEN d.credit - d.debit ELSE 0 END) <> 0;
```

CI binding [ADR-031](../../adr/ADR-031-sql-ledger-invariant-ci.md) · vol-02 CK-06..CK-10.
---
## Chương 24. Ops runbook outline {#chương-24-ops-runbook-outline}

`[TBD: Runbook chi tiết — escalation matrix, bank SLA contacts, reversal approval workflow.] `

### 24.1 Frozen UNKNOWN playbook

1. Verify poll job running · 2. Check bank adapter status · 3. **Không** manual RELEASE · 4. Escalate Tmax · 5. Document terminal resolution.

### 24.2 POSTED + RELEASE mismatch

Finance approve reversal Ch9 · link `reverses_id` · verify wallet available restored.
---
## Chương 25. Review checklist — 35 mục {#chương-25-review-checklist--35-mục}

| # | Mục | ADR/Evidence |
|---|-----|--------------|
| 1 | FREEZE before 200 | ADR-007 AC-007-01 |
| 2 | No WITHDRAW_DEBIT | ADR-007 AC-007-02 |
| 3 | 3200=0 POSTED WD | ADR-010 |
| 4 | 3400=0 POSTED IBFT | ADR-010 |
| 5 | 1111 WD principal only | foundation §9 |
| 6 | 1112 IBFT not 1111 | ADR-025 |
| 7 | 5100 IBFT when cost | ADR-025 AC-025-03 |
| 8 | No 5100 withdraw | CK-19 |
| 9 | 4120 withdraw fee | foundation §9 |
| 10 | 4130 IBFT fee | foundation §11 |
| 11 | business_ref UNIQUE | ADR-005 |
| 12 | :settle :release sub-keys | ADR-005 |
| 13 | Timeout no RELEASE | §13.4 |
| 14 | Poll T2 not tight-loop | ADR-033 |
| 15 | Tmax alert no auto-release | ADR-033 |
| 16 | Immutability POSTED | ADR-001 |
| 17 | Reversal not UPDATE | ADR-001 |
| 18 | O1 documented | accounting.md §11 |
| 19 | Fee orchestration | ADR-009 |
| 20 | Accrual Policy A | ADR-036 |
| 21 | Period close block | ADR-023 |
| 22 | Outbox same TX | ADR-013 |
| 23 | No JOIN wallet SQL | ADR-003 |
| 24 | W5 report only | ADR-014 |
| 25 | Saga no 2PC | ADR-008 |
| 26 | Adapter idempotent payout | ADR-007 AC-007-08 |
| 27 | SETTLE after RELEASE blocked | X-E12 |
| 28 | UNKNOWN poll X-E13 | ADR-033 |
| 29 | ACC-E03 freeze on accept | acceptance |
| 30 | WD 22 Gherkin mapped | acceptance |
| 31 | IBFT 10 Gherkin mapped | acceptance |
| 32 | SQL INV CI | ADR-031 |
| 33 | FR-10 1111 recon | vol-02 B.1 |
| 34 | Frozen dashboard | ADR-021 |
| 35 | [TBD] fee schedule | processes §14 |
| 36 | [TBD] bank SLA T2/Tmax | ADR-033 |
---
## Phụ lục A — Ma trận kịch bản số {#phụ-lục-a--ma-trận-kịch-bản-số}

| ID | Flow | principal | fee | bank TK | Napas | gross |
|----|------|-----------|-----|---------|-------|-------|
| A01 | WD happy | 100,000 | 1,000 | 1111 | 0 | 101,000 |
| A02 | WD zero fee | 100,000 | 0 | 1111 | 0 | 100,000 |
| A03 | WD min 1 | 1 | 0 | 1111 | 0 | 1 |
| A04 | WD large | 500,000,000 | 5,000 | 1111 | 0 | 500,005,000 |
| A05 | IBFT happy | 100,000 | 1,000 | 1112 | 500 | 101,000 |
| A06 | IBFT 50M | 50,000,000 | 50,000 | 1112 | 25,000 | 50,050,000 |
| A07 | IBFT zero fee | 100,000 | 0 | 1112 | 500 | 100,000 |
| A08 | WD fee 2k | 200,000 | 2,000 | 1111 | 0 | 202,000 |
| A09 | IBFT profit 0 | 100,000 | 500 | 1112 | 500 | 100,500 |
| A10 | WD boundary | 101,000 | 0 | 1111 | 0 | 101,000 |
---
## Phụ lục B — DR/CR line-by-line {#phụ-lục-b--dr-cr-line-by-line-mọi-biến-thể}

### B.1 WITHDRAW — foundation §9

- Step 1: 2110 DR 101,000 CR — — User liability ↓ gross
- Step 2: 3200 DR — CR 101,000 — Transit hold gross
- Step 3: 3200 DR 100,000 CR — — Clear principal leg
- Step 4: 1111 DR — CR 100,000 — Bank asset ↓ principal
- Step 5: 3200 DR 1,000 CR — — Clear fee leg
- Step 6: 4120 DR — CR 1,000 — Withdraw fee revenue

### B.2 IBFT — foundation §11

- Step 1: 2110 DR 101,000 CR — — User liability ↓ gross
- Step 2: 3400 DR — CR 101,000 — Transit hold gross
- Step 3: 3400 DR 1,000 CR — — Fee leg DR transit
- Step 4: 4130 DR — CR 1,000 — Transfer fee revenue
- Step 5: 3400 DR 100,000 CR — — Principal leg DR transit
- Step 6: 1112 DR — CR 100,000 — Napas clearing out
- Step 8: 5100 DR 500 CR — — Napas/bank expense
- Step 9: 1112 DR — CR 500 — Cash out Napas fee

#### B.WD-V01 principal=10,000 fee=100
gross=10,100; 1111 CR 10,000; 4120 CR 100


#### B.WD-V02 principal=20,000 fee=200
gross=20,200; 1111 CR 20,000; 4120 CR 200


#### B.WD-V03 principal=30,000 fee=300
gross=30,300; 1111 CR 30,000; 4120 CR 300


#### B.WD-V04 principal=40,000 fee=400
gross=40,400; 1111 CR 40,000; 4120 CR 400


#### B.WD-V05 principal=50,000 fee=500
gross=50,500; 1111 CR 50,000; 4120 CR 500


#### B.WD-V06 principal=60,000 fee=600
gross=60,600; 1111 CR 60,000; 4120 CR 600


#### B.WD-V07 principal=70,000 fee=700
gross=70,700; 1111 CR 70,000; 4120 CR 700


#### B.WD-V08 principal=80,000 fee=800
gross=80,800; 1111 CR 80,000; 4120 CR 800


#### B.WD-V09 principal=90,000 fee=900
gross=90,900; 1111 CR 90,000; 4120 CR 900


#### B.WD-V10 principal=100,000 fee=1,000
gross=101,000; 1111 CR 100,000; 4120 CR 1,000


#### B.WD-V11 principal=110,000 fee=1,100
gross=111,100; 1111 CR 110,000; 4120 CR 1,100


#### B.WD-V12 principal=120,000 fee=1,200
gross=121,200; 1111 CR 120,000; 4120 CR 1,200


#### B.WD-V13 principal=130,000 fee=1,300
gross=131,300; 1111 CR 130,000; 4120 CR 1,300


#### B.WD-V14 principal=140,000 fee=1,400
gross=141,400; 1111 CR 140,000; 4120 CR 1,400


#### B.WD-V15 principal=150,000 fee=1,500
gross=151,500; 1111 CR 150,000; 4120 CR 1,500

---
## Phụ lục C — Bảng tra ADR {#phụ-lục-c--bảng-tra-adr}

| ADR | Topic | Quyển IV |
|-----|-------|----------|
| [ADR-001](../../adr/ADR-001-immutable-ledger.md) | Immutability | Ch9 reversal |
| [ADR-005](../../adr/ADR-005-idempotency-key-strategy.md) | Idempotency | Ch6 |
| [ADR-007](../../adr/ADR-007-freeze-settle-async-outflow.md) | Freeze-settle | Ch3 |
| [ADR-008](../../adr/ADR-008-saga-compensation-no-2pc.md) | Saga | Ch5, Phụ lục D |
| [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md) | Fee ownership | Ch1 |
| [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md) | Transit zero | Ch4, Ch15 |
| [ADR-013](../../adr/ADR-013-outbox-at-least-once-messaging.md) | Outbox | Ch7 |
| [ADR-023](../../adr/ADR-023-accounting-period-close.md) | Period close | W-F8 |
| [ADR-025](../../adr/ADR-025-ibft-napas-clearing-1112.md) | IBFT 1112 | Ch14 |
| [ADR-031](../../adr/ADR-031-sql-ledger-invariant-ci.md) | SQL CI | Ch23 |
| [ADR-033](../../adr/ADR-033-bank-poll-t2-frozen-tmax.md) | Bank poll | Ch8 |
| [ADR-036](../../adr/ADR-036-accrual-basis-ledger-v1.md) | Accrual | Ch2 |
---
## Phụ lục D — Saga §13.4 pointer {#phụ-lục-d--saga-134-pointer}

Withdraw freeze saga: [`processes.md`](../../spec/processes.md) §13.4 — failure matrix W-F3/W-F4/W-F5 maps 1:1.

| Failure point | Recovery | Quyển IV |
|---------------|----------|----------|
| After freeze before enqueue | retry enqueue / release | Ch7 |
| Payout UNKNOWN | poll — no RELEASE | Ch8, X-E13 |
| Success settle fails | retry :settle | Ch6, W-F5 |
| Payout FAIL | :release | Ch3, W-F4 |
| Redelivered payout | adapter dedup | Ch6, W-F9 |

IBFT: §13.5 identical + 5100 accounting.
---
## Phụ lục E — processes.md alignment {#phụ-lục-e--processes.md-alignment}

| processes § | Quyển IV |
|-------------|----------|
| §4 Withdraw | Phần I Ch1–12 |
| §7 IBFT | Phần II Ch13–19 |
| §12 cross-ref | Ch20 |
| §13.4–13.5 | Phụ lục D |
---
## Phụ lục F — accounting.md §15/§18 {#phụ-lục-f--accounting.md-1518-mapping}

### §15 WITHDRAW

- 15.1 Obligation → Ch1, Ch4
- 15.2 Observables → Ch4.14
- 15.3 W-F1..10 → Ch5
- 15.4 O1 → Ch2
### §18 IBFT

- 18.1 Obligation → Ch14–15
- 18.2 Failures → Ch16
- I-F1..3 → Ch16
---
## Phụ lục G — Numeric T-account gallery {#phụ-lục-g--numeric-t-account-gallery}

#### G01 WD principal=5,000 fee=50
```
2110 DR 5,050 | 1111 CR 5,000 | 4120 CR 50 | 3200=0
```

#### G02 WD principal=10,000 fee=100
```
2110 DR 10,100 | 1111 CR 10,000 | 4120 CR 100 | 3200=0
```

#### G03 WD principal=15,000 fee=150
```
2110 DR 15,150 | 1111 CR 15,000 | 4120 CR 150 | 3200=0
```

#### G04 WD principal=20,000 fee=200
```
2110 DR 20,200 | 1111 CR 20,000 | 4120 CR 200 | 3200=0
```

#### G05 WD principal=25,000 fee=250
```
2110 DR 25,250 | 1111 CR 25,000 | 4120 CR 250 | 3200=0
```

#### G06 WD principal=30,000 fee=300
```
2110 DR 30,300 | 1111 CR 30,000 | 4120 CR 300 | 3200=0
```

#### G07 WD principal=35,000 fee=350
```
2110 DR 35,350 | 1111 CR 35,000 | 4120 CR 350 | 3200=0
```

#### G08 WD principal=40,000 fee=400
```
2110 DR 40,400 | 1111 CR 40,000 | 4120 CR 400 | 3200=0
```

#### G09 WD principal=45,000 fee=450
```
2110 DR 45,450 | 1111 CR 45,000 | 4120 CR 450 | 3200=0
```

#### G10 WD principal=50,000 fee=500
```
2110 DR 50,500 | 1111 CR 50,000 | 4120 CR 500 | 3200=0
```

#### G11 WD principal=55,000 fee=550
```
2110 DR 55,550 | 1111 CR 55,000 | 4120 CR 550 | 3200=0
```

#### G12 WD principal=60,000 fee=600
```
2110 DR 60,600 | 1111 CR 60,000 | 4120 CR 600 | 3200=0
```

#### G13 WD principal=65,000 fee=650
```
2110 DR 65,650 | 1111 CR 65,000 | 4120 CR 650 | 3200=0
```

#### G14 WD principal=70,000 fee=700
```
2110 DR 70,700 | 1111 CR 70,000 | 4120 CR 700 | 3200=0
```

#### G15 WD principal=75,000 fee=750
```
2110 DR 75,750 | 1111 CR 75,000 | 4120 CR 750 | 3200=0
```

#### G16 WD principal=80,000 fee=800
```
2110 DR 80,800 | 1111 CR 80,000 | 4120 CR 800 | 3200=0
```

#### G17 WD principal=85,000 fee=850
```
2110 DR 85,850 | 1111 CR 85,000 | 4120 CR 850 | 3200=0
```

#### G18 WD principal=90,000 fee=900
```
2110 DR 90,900 | 1111 CR 90,000 | 4120 CR 900 | 3200=0
```

#### G19 WD principal=95,000 fee=950
```
2110 DR 95,950 | 1111 CR 95,000 | 4120 CR 950 | 3200=0
```

#### G20 WD principal=100,000 fee=1,000
```
2110 DR 101,000 | 1111 CR 100,000 | 4120 CR 1,000 | 3200=0
```

---
## Phụ lục H — Cross-flow invariant {#phụ-lục-h--cross-flow-invariant}

```
1111 + 1112 + 1113 = 2110 + 2120 + 2130  (aggregate control)
Every transit → 0 at POSTED terminal
wallet available+frozen ↔ 2110 timing per O1
```
---
## Phụ lục I — Đọc tiếp {#phụ-lục-i--đọc-tiếp-vol-05}

| Tiếp theo | Nội dung |
|-----------|----------|
| **Quyển V** | Payment & internal transfer — transit 3500/3300 |
| [`vol-03-deposit.md`](./vol-03-deposit.md) | Inbound hai pha 3100 |
| [`vol-02-coa-handbook.md`](./vol-02-coa-handbook.md) | TK deep dive |
| [`vol-01-principles.md`](./vol-01-principles.md) | Nguyên tắc accrual, matching |

## Đọc tiếp {#đọc-tiếp}

→ [`vol-05-payment-transfer.md`](./vol-05-payment-transfer.md) — wallet payment sync 3500, internal transfer 3300.
## Chương 1B. Deposit vs Withdraw — hướng tiền đảo ngược {#chương-1b-deposit-vs-withdraw-direction}

Deposit: bank **tăng** 1111 → transit 3100 → credit 2110. Withdraw: **giảm** 2110 → transit 3200 → **giảm** 1111. Cùng nguyên tắc transit về 0 tại POSTED terminal — khác **timing wallet**: deposit credit sau POSTED; withdraw freeze trước POSTED (O1).

| Bước | DEPOSIT | WITHDRAW |
|------|---------|----------|
| User initiates | CK vào VA | API createWithdrawal |
| Wallet first | Không (async webhook) | FREEZE sync |
| Transit | 3100 two-phase | 3200 single-phase |
| Bank asset | 1111 DR (in) | 1111 CR (out) |
| Fee revenue | 4110 | 4120 |
---
## Chương 2B. O1 — auditor Q&A {#chương-2b-o1-auditor-qa}

| Câu hỏi | Policy A | Policy B |
|---------|----------|----------|
| 2110 giảm khi nào? | Accept POSTED | Bank SUCCESS POSTED |
| Tiền bank ra khi nào? | Sau accept (accrual) | Cùng POSTED |
| RELEASE ảnh hưởng ledger? | Không tự đảo — reversal manual | Không POSTED nếu fail |
| Evidence | coa_trans timestamp ≈ accept | coa_trans ≈ bank callback |
---
## Chương 3B. Wallet tx audit trail {#chương-3b-wallet-tx-audit-trail}

Mỗi withdraw tạo tối đa 3 `wallet_tx`: FREEZE (bắt buộc), SETTLE hoặc RELEASE (terminal). Không có tx thứ 4 debiting available trực tiếp.

```sql
SELECT tx_type, amount, created_at FROM wallet_tx
WHERE business_ref LIKE 'wd-%' ORDER BY created_at;
```
---
## Chương 4B. POSTED observables — 12 checks {#chương-4b-posted-observables}

| # | Check | Expected (chuẩn) |
|---|-------|------------------|
| O1 | use_case | WITHDRAW |
| O2 | status | POSTED |
| O3 | 3200 net | 0 |
| O4 | 2110 Δ | −101.000 |
| O5 | 1111 Δ | −100.000 |
| O6 | 4120 Δ | +1.000 |
| O7 | line count | 6 (fee>0) |
| O8 | SUM DR = SUM CR | 202.000 |
| O9 | no 5100 | true |
| O10 | no 1112 | true |
| O11 | business_ref unique | true |
| O12 | posting_date open period | true |
---
## Chương 4C. IBFT POSTED observables — 14 checks {#chương-4c-ibft-posted-observables}

| # | Check | Expected |
|---|-------|----------|
| O1 | use_case | IBFT |
| O2 | 3400 net | 0 |
| O3 | 2110 Δ | −101.000 |
| O4 | 4130 Δ | +1.000 |
| O5 | 1112 Δ principal | −100.000 |
| O6 | 1112 Δ napas | −500 |
| O7 | 5100 Δ | +500 |
| O8 | no 1111 | true |
| O9 | no 3200 | true |
| O10 | no 4120 | true |
| O11 | net P&L | +500 |
| O12 | 8 lines | fee+napas |
| O13 | ADR-025 | pass |
| O14 | profit formula | 4130−5100 |
---
## Chương 5B.1. Ops deep dive W-F1 {#chương-5b1-ops-w-f1}

Playbook mở rộng cho **W-F1** — liên kết [`accounting.md`](../accounting.md) §15.3.

1. **Detect:** metric + log `businessRef` + `use_case=WITHDRAW`.
2. **Triage:** xác định Policy O1 A hay B — ảnh hưởng ledger expectation.
3. **Recover:** forward-retry hoặc compensate theo §13.4 — không sửa SQL POSTED.
4. **Close:** document terminal state; W5 verify nếu wallet involved.
5. **Prevent:** CI invariant + acceptance scenario regression.

**Forbidden:** UPDATE `coa_trans_data`; RELEASE on UNKNOWN (W-F3); double 1111 CR (W-F9).
---
## Chương 5B.2. Ops deep dive W-F2 {#chương-5b2-ops-w-f2}

Playbook mở rộng cho **W-F2** — liên kết [`accounting.md`](../accounting.md) §15.3.

1. **Detect:** metric + log `businessRef` + `use_case=WITHDRAW`.
2. **Triage:** xác định Policy O1 A hay B — ảnh hưởng ledger expectation.
3. **Recover:** forward-retry hoặc compensate theo §13.4 — không sửa SQL POSTED.
4. **Close:** document terminal state; W5 verify nếu wallet involved.
5. **Prevent:** CI invariant + acceptance scenario regression.

**Forbidden:** UPDATE `coa_trans_data`; RELEASE on UNKNOWN (W-F3); double 1111 CR (W-F9).
---
## Chương 5B.3. Ops deep dive W-F3 {#chương-5b3-ops-w-f3}

Playbook mở rộng cho **W-F3** — liên kết [`accounting.md`](../accounting.md) §15.3.

1. **Detect:** metric + log `businessRef` + `use_case=WITHDRAW`.
2. **Triage:** xác định Policy O1 A hay B — ảnh hưởng ledger expectation.
3. **Recover:** forward-retry hoặc compensate theo §13.4 — không sửa SQL POSTED.
4. **Close:** document terminal state; W5 verify nếu wallet involved.
5. **Prevent:** CI invariant + acceptance scenario regression.

**Forbidden:** UPDATE `coa_trans_data`; RELEASE on UNKNOWN (W-F3); double 1111 CR (W-F9).
---
## Chương 5B.4. Ops deep dive W-F4 {#chương-5b4-ops-w-f4}

Playbook mở rộng cho **W-F4** — liên kết [`accounting.md`](../accounting.md) §15.3.

1. **Detect:** metric + log `businessRef` + `use_case=WITHDRAW`.
2. **Triage:** xác định Policy O1 A hay B — ảnh hưởng ledger expectation.
3. **Recover:** forward-retry hoặc compensate theo §13.4 — không sửa SQL POSTED.
4. **Close:** document terminal state; W5 verify nếu wallet involved.
5. **Prevent:** CI invariant + acceptance scenario regression.

**Forbidden:** UPDATE `coa_trans_data`; RELEASE on UNKNOWN (W-F3); double 1111 CR (W-F9).
---
## Chương 5B.5. Ops deep dive W-F5 {#chương-5b5-ops-w-f5}

Playbook mở rộng cho **W-F5** — liên kết [`accounting.md`](../accounting.md) §15.3.

1. **Detect:** metric + log `businessRef` + `use_case=WITHDRAW`.
2. **Triage:** xác định Policy O1 A hay B — ảnh hưởng ledger expectation.
3. **Recover:** forward-retry hoặc compensate theo §13.4 — không sửa SQL POSTED.
4. **Close:** document terminal state; W5 verify nếu wallet involved.
5. **Prevent:** CI invariant + acceptance scenario regression.

**Forbidden:** UPDATE `coa_trans_data`; RELEASE on UNKNOWN (W-F3); double 1111 CR (W-F9).
---
## Chương 5B.6. Ops deep dive W-F6 {#chương-5b6-ops-w-f6}

Playbook mở rộng cho **W-F6** — liên kết [`accounting.md`](../accounting.md) §15.3.

1. **Detect:** metric + log `businessRef` + `use_case=WITHDRAW`.
2. **Triage:** xác định Policy O1 A hay B — ảnh hưởng ledger expectation.
3. **Recover:** forward-retry hoặc compensate theo §13.4 — không sửa SQL POSTED.
4. **Close:** document terminal state; W5 verify nếu wallet involved.
5. **Prevent:** CI invariant + acceptance scenario regression.

**Forbidden:** UPDATE `coa_trans_data`; RELEASE on UNKNOWN (W-F3); double 1111 CR (W-F9).
---
## Chương 5B.7. Ops deep dive W-F7 {#chương-5b7-ops-w-f7}

Playbook mở rộng cho **W-F7** — liên kết [`accounting.md`](../accounting.md) §15.3.

1. **Detect:** metric + log `businessRef` + `use_case=WITHDRAW`.
2. **Triage:** xác định Policy O1 A hay B — ảnh hưởng ledger expectation.
3. **Recover:** forward-retry hoặc compensate theo §13.4 — không sửa SQL POSTED.
4. **Close:** document terminal state; W5 verify nếu wallet involved.
5. **Prevent:** CI invariant + acceptance scenario regression.

**Forbidden:** UPDATE `coa_trans_data`; RELEASE on UNKNOWN (W-F3); double 1111 CR (W-F9).
---
## Chương 5B.8. Ops deep dive W-F8 {#chương-5b8-ops-w-f8}

Playbook mở rộng cho **W-F8** — liên kết [`accounting.md`](../accounting.md) §15.3.

1. **Detect:** metric + log `businessRef` + `use_case=WITHDRAW`.
2. **Triage:** xác định Policy O1 A hay B — ảnh hưởng ledger expectation.
3. **Recover:** forward-retry hoặc compensate theo §13.4 — không sửa SQL POSTED.
4. **Close:** document terminal state; W5 verify nếu wallet involved.
5. **Prevent:** CI invariant + acceptance scenario regression.

**Forbidden:** UPDATE `coa_trans_data`; RELEASE on UNKNOWN (W-F3); double 1111 CR (W-F9).
---
## Chương 5B.9. Ops deep dive W-F9 {#chương-5b9-ops-w-f9}

Playbook mở rộng cho **W-F9** — liên kết [`accounting.md`](../accounting.md) §15.3.

1. **Detect:** metric + log `businessRef` + `use_case=WITHDRAW`.
2. **Triage:** xác định Policy O1 A hay B — ảnh hưởng ledger expectation.
3. **Recover:** forward-retry hoặc compensate theo §13.4 — không sửa SQL POSTED.
4. **Close:** document terminal state; W5 verify nếu wallet involved.
5. **Prevent:** CI invariant + acceptance scenario regression.

**Forbidden:** UPDATE `coa_trans_data`; RELEASE on UNKNOWN (W-F3); double 1111 CR (W-F9).
---
## Chương 5B.10. Ops deep dive W-F10 {#chương-5b10-ops-w-f10}

Playbook mở rộng cho **W-F10** — liên kết [`accounting.md`](../accounting.md) §15.3.

1. **Detect:** metric + log `businessRef` + `use_case=WITHDRAW`.
2. **Triage:** xác định Policy O1 A hay B — ảnh hưởng ledger expectation.
3. **Recover:** forward-retry hoặc compensate theo §13.4 — không sửa SQL POSTED.
4. **Close:** document terminal state; W5 verify nếu wallet involved.
5. **Prevent:** CI invariant + acceptance scenario regression.

**Forbidden:** UPDATE `coa_trans_data`; RELEASE on UNKNOWN (W-F3); double 1111 CR (W-F9).
---
## Chương 6B. Idempotency sequence diagrams {#chương-6b-idempotency-diagrams}

```
Client ──accept(ref)──► Orch ──FREEZE(ref)──► Wallet
Client ──accept(ref)──► Orch ──FREEZE(ref)──► Wallet (idempotent replay)
         └── same coa_trans_id, frozen unchanged
```
---
## Chương 7B. S6 poison / DLQ {#chương-7b-s6-dlq}

Malformed WITHDRAW_PAYOUT → retry → DLQ → CommandFailed. Frozen giữ cho đến ops RELEASE hoặc fix + re-enqueue. **Không** auto-release on DLQ.

---
## Chương 8B. Poll timeline example [TBD values] {#chương-8b-poll-timeline}

```
T+0s   payout sent → UNKNOWN
T+T2   poll #1 → UNKNOWN
T+2·T2 poll #2 → SUCCESS → SETTLE
Frozen unchanged entire UNKNOWN window
```
---
## Chương 9B. Reversal metadata {#chương-9b-reversal-metadata}

| Field | Value |
|-------|-------|
| reverses_id | original |
| business_ref | {orig}-REV |
| use_case | WITHDRAW / ADJUSTMENT |
---
## Chương 10B. FR-10 daily steps withdraw {#chương-10b-fr10-daily}

1. Bước recon 1111 withdraw — match payout batch line 1.
2. Bước recon 1111 withdraw — match payout batch line 2.
3. Bước recon 1111 withdraw — match payout batch line 3.
4. Bước recon 1111 withdraw — match payout batch line 4.
5. Bước recon 1111 withdraw — match payout batch line 5.
6. Bước recon 1111 withdraw — match payout batch line 6.
7. Bước recon 1111 withdraw — match payout batch line 7.
8. Bước recon 1111 withdraw — match payout batch line 8.
---
## Chương 11B. Gherkin — notes triển khai {#chương-11b-gherkin-notes}

22 scenarios map acceptance Feature Withdraw (12) + WD-E (10). Ledger lines theo **foundation §9** — không copy typo acceptance scenario dùng 4130/5100 cho withdraw.

---
## Chương 12B. FAQ extended rationale {#chương-12b-faq-extended}

**Q1 rationale.** — Xem Chương tương ứng + ADR Phụ lục C. Câu hỏi onboarding tại Ch0.4.

**Q2 rationale.** — Xem Chương tương ứng + ADR Phụ lục C. Câu hỏi onboarding tại Ch0.4.

**Q3 rationale.** — Xem Chương tương ứng + ADR Phụ lục C. Câu hỏi onboarding tại Ch0.4.

**Q4 rationale.** — Xem Chương tương ứng + ADR Phụ lục C. Câu hỏi onboarding tại Ch0.4.

**Q5 rationale.** — Xem Chương tương ứng + ADR Phụ lục C. Câu hỏi onboarding tại Ch0.4.

**Q6 rationale.** — Xem Chương tương ứng + ADR Phụ lục C. Câu hỏi onboarding tại Ch0.4.

**Q7 rationale.** — Xem Chương tương ứng + ADR Phụ lục C. Câu hỏi onboarding tại Ch0.4.

**Q8 rationale.** — Xem Chương tương ứng + ADR Phụ lục C. Câu hỏi onboarding tại Ch0.4.

**Q9 rationale.** — Xem Chương tương ứng + ADR Phụ lục C. Câu hỏi onboarding tại Ch0.4.

**Q10 rationale.** — Xem Chương tương ứng + ADR Phụ lục C. Câu hỏi onboarding tại Ch0.4.

**Q11 rationale.** — Xem Chương tương ứng + ADR Phụ lục C. Câu hỏi onboarding tại Ch0.4.

**Q12 rationale.** — Xem Chương tương ứng + ADR Phụ lục C. Câu hỏi onboarding tại Ch0.4.

**Q13 rationale.** — Xem Chương tương ứng + ADR Phụ lục C. Câu hỏi onboarding tại Ch0.4.

**Q14 rationale.** — Xem Chương tương ứng + ADR Phụ lục C. Câu hỏi onboarding tại Ch0.4.

**Q15 rationale.** — Xem Chương tương ứng + ADR Phụ lục C. Câu hỏi onboarding tại Ch0.4.

**Q16 rationale.** — Xem Chương tương ứng + ADR Phụ lục C. Câu hỏi onboarding tại Ch0.4.

**Q17 rationale.** — Xem Chương tương ứng + ADR Phụ lục C. Câu hỏi onboarding tại Ch0.4.

**Q18 rationale.** — Xem Chương tương ứng + ADR Phụ lục C. Câu hỏi onboarding tại Ch0.4.

**Q19 rationale.** — Xem Chương tương ứng + ADR Phụ lục C. Câu hỏi onboarding tại Ch0.4.

**Q20 rationale.** — Xem Chương tương ứng + ADR Phụ lục C. Câu hỏi onboarding tại Ch0.4.

**Q21 rationale.** — Xem Chương tương ứng + ADR Phụ lục C. Câu hỏi onboarding tại Ch0.4.

**Q22 rationale.** — Xem Chương tương ứng + ADR Phụ lục C. Câu hỏi onboarding tại Ch0.4.

**Q23 rationale.** — Xem Chương tương ứng + ADR Phụ lục C. Câu hỏi onboarding tại Ch0.4.

**Q24 rationale.** — Xem Chương tương ứng + ADR Phụ lục C. Câu hỏi onboarding tại Ch0.4.

**Q25 rationale.** — Xem Chương tương ứng + ADR Phụ lục C. Câu hỏi onboarding tại Ch0.4.

---
## Chương 13B. IBFT rail diagram {#chương-13b-ibft-rail}

```
User wallet ──FREEZE──► Platform ledger 3400
                              │
                              ▼ Napas 1112
                         Beneficiary bank
                              │
                         5100 Napas fee
```
---
## Chương 14B. P&L walkthrough {#chương-14b-pl-walkthrough}

Gross fee 1.000 CR 4130 · Napas 500 DR 5100 + CR 1112 · **Net contribution margin 500** — vol-01 E16–E17 matching principle.

---
## Chương 15B.1. IBFT T-account variant 1 {#ch15b1}

```
principal 10,000 fee 100 napas 50 profit 50
2110 DR 10,100 | 4130 CR 100 | 5100 DR 50 | 1112 CR 10,050 | 3400=0
```

## Chương 15B.2. IBFT T-account variant 2 {#ch15b2}

```
principal 20,000 fee 200 napas 100 profit 100
2110 DR 20,200 | 4130 CR 200 | 5100 DR 100 | 1112 CR 20,100 | 3400=0
```

## Chương 15B.3. IBFT T-account variant 3 {#ch15b3}

```
principal 30,000 fee 300 napas 150 profit 150
2110 DR 30,300 | 4130 CR 300 | 5100 DR 150 | 1112 CR 30,150 | 3400=0
```

## Chương 15B.4. IBFT T-account variant 4 {#ch15b4}

```
principal 40,000 fee 400 napas 200 profit 200
2110 DR 40,400 | 4130 CR 400 | 5100 DR 200 | 1112 CR 40,200 | 3400=0
```

## Chương 15B.5. IBFT T-account variant 5 {#ch15b5}

```
principal 50,000 fee 500 napas 250 profit 250
2110 DR 50,500 | 4130 CR 500 | 5100 DR 250 | 1112 CR 50,250 | 3400=0
```

## Chương 15B.6. IBFT T-account variant 6 {#ch15b6}

```
principal 60,000 fee 600 napas 300 profit 300
2110 DR 60,600 | 4130 CR 600 | 5100 DR 300 | 1112 CR 60,300 | 3400=0
```

## Chương 15B.7. IBFT T-account variant 7 {#ch15b7}

```
principal 70,000 fee 700 napas 350 profit 350
2110 DR 70,700 | 4130 CR 700 | 5100 DR 350 | 1112 CR 70,350 | 3400=0
```

## Chương 15B.8. IBFT T-account variant 8 {#ch15b8}

```
principal 80,000 fee 800 napas 400 profit 400
2110 DR 80,800 | 4130 CR 800 | 5100 DR 400 | 1112 CR 80,400 | 3400=0
```

## Chương 15B.9. IBFT T-account variant 9 {#ch15b9}

```
principal 90,000 fee 900 napas 450 profit 450
2110 DR 90,900 | 4130 CR 900 | 5100 DR 450 | 1112 CR 90,450 | 3400=0
```

## Chương 15B.10. IBFT T-account variant 10 {#ch15b10}

```
principal 100,000 fee 1,000 napas 500 profit 500
2110 DR 101,000 | 4130 CR 1,000 | 5100 DR 500 | 1112 CR 100,500 | 3400=0
```

---
## Chương 16B. IBFT + W-F combined matrix {#chương-16b-combined-matrix}

| W-F | IBFT note |
|-----|-----------|
| W-F3 | Poll Napas — no RELEASE |
| W-F4 | Reversal includes 5100/4130 |
| W-F9 | Napas adapter dedup |
---
## Chương 17B. Napas callback vs poll {#chương-17b-napas-callback}

Webhook terminal preferred; poll backstop ADR-033. Cùng idempotency `business_ref`.

---
## Chương 18B. IBFT Gherkin implementation notes {#chương-18b-ibft-gherkin-notes}

10 scenarios = Feature IBFT (5) + IBFT-E (5). IBFT-02 ledger lines phải khớp foundation §11 (8 dòng).

---
## Chương 19B. IBFT FAQ cross-links {#chương-19b-ibft-faq-cross}

- IBFT FAQ Q1 ↔ Ch13–17 ↔ ADR-025/007/033

- IBFT FAQ Q2 ↔ Ch13–17 ↔ ADR-025/007/033

- IBFT FAQ Q3 ↔ Ch13–17 ↔ ADR-025/007/033

- IBFT FAQ Q4 ↔ Ch13–17 ↔ ADR-025/007/033

- IBFT FAQ Q5 ↔ Ch13–17 ↔ ADR-025/007/033

- IBFT FAQ Q6 ↔ Ch13–17 ↔ ADR-025/007/033

- IBFT FAQ Q7 ↔ Ch13–17 ↔ ADR-025/007/033

- IBFT FAQ Q8 ↔ Ch13–17 ↔ ADR-025/007/033

- IBFT FAQ Q9 ↔ Ch13–17 ↔ ADR-025/007/033

- IBFT FAQ Q10 ↔ Ch13–17 ↔ ADR-025/007/033

- IBFT FAQ Q11 ↔ Ch13–17 ↔ ADR-025/007/033

- IBFT FAQ Q12 ↔ Ch13–17 ↔ ADR-025/007/033

- IBFT FAQ Q13 ↔ Ch13–17 ↔ ADR-025/007/033

- IBFT FAQ Q14 ↔ Ch13–17 ↔ ADR-025/007/033

- IBFT FAQ Q15 ↔ Ch13–17 ↔ ADR-025/007/033

---
## Chương 20B. Three-way compare deposit/withdraw/IBFT {#chương-20b-three-way}

| | Deposit | Withdraw | IBFT |
|--|---------|----------|------|
| Risk | credit early | payout double | payout double |
| Transit | 3100 | 3200 | 3400 |
| Wallet | credit after | freeze first | freeze first |
---
## Chương 21B. Timeline IBFT Policy A {#chương-21b-ibft-timeline}

```
FREEZE → POSTED(3400,4130,5100,1112) → Napas → SETTLE
```
---
## Chương 22B. S2 error codes {#chương-22b-s2-errors}

- `ACCOUNTING_UNBALANCED_JOURNAL` — reject before persist

- `TRANSIT_NONZERO_AT_POSTED` — reject before persist

- `ACCOUNTING_PERIOD_CLOSED` — reject before persist

- `ACCOUNTING_DUPLICATE_CONFLICT` — reject before persist

- `ACCOUNTING_INVALID_ACCOUNT` — reject before persist

---
## Chương 23B. CI test cases TC-WD / TC-IBFT {#chương-23b-ci-tests}

| TC-WD-01 | Invariant withdraw #1 | CI pipeline |

| TC-WD-02 | Invariant withdraw #2 | CI pipeline |

| TC-WD-03 | Invariant withdraw #3 | CI pipeline |

| TC-WD-04 | Invariant withdraw #4 | CI pipeline |

| TC-WD-05 | Invariant withdraw #5 | CI pipeline |

| TC-WD-06 | Invariant withdraw #6 | CI pipeline |

| TC-WD-07 | Invariant withdraw #7 | CI pipeline |

| TC-WD-08 | Invariant withdraw #8 | CI pipeline |

| TC-WD-09 | Invariant withdraw #9 | CI pipeline |

| TC-WD-10 | Invariant withdraw #10 | CI pipeline |

| TC-WD-11 | Invariant withdraw #11 | CI pipeline |

| TC-WD-12 | Invariant withdraw #12 | CI pipeline |

| TC-WD-13 | Invariant withdraw #13 | CI pipeline |

| TC-WD-14 | Invariant withdraw #14 | CI pipeline |

| TC-WD-15 | Invariant withdraw #15 | CI pipeline |

| TC-IBFT-01 | Invariant IBFT #1 | ADR-025 |

| TC-IBFT-02 | Invariant IBFT #2 | ADR-025 |

| TC-IBFT-03 | Invariant IBFT #3 | ADR-025 |

| TC-IBFT-04 | Invariant IBFT #4 | ADR-025 |

| TC-IBFT-05 | Invariant IBFT #5 | ADR-025 |

| TC-IBFT-06 | Invariant IBFT #6 | ADR-025 |

| TC-IBFT-07 | Invariant IBFT #7 | ADR-025 |

| TC-IBFT-08 | Invariant IBFT #8 | ADR-025 |

| TC-IBFT-09 | Invariant IBFT #9 | ADR-025 |

| TC-IBFT-10 | Invariant IBFT #10 | ADR-025 |

---
## Chương 24B. Runbook escalation [TBD] {#chương-24b-escalation}

| Severity | Condition | Action |
|----------|-----------|--------|
| P1 | RELEASE after suspected SUCCESS | Stop manual RELEASE; bank trace |
| P2 | frozen > Tmax | Ops + bank ticket |
| P3 | W5 drift | Report reconcile |
---
## Chương 25B. Checklist rationale {#chương-25b-checklist-rationale}

| 1 | Mục #1 | Rationale: ADR/evidence trong Ch25 — verify implementation traceability |

| 2 | Mục #2 | Rationale: ADR/evidence trong Ch25 — verify implementation traceability |

| 3 | Mục #3 | Rationale: ADR/evidence trong Ch25 — verify implementation traceability |

| 4 | Mục #4 | Rationale: ADR/evidence trong Ch25 — verify implementation traceability |

| 5 | Mục #5 | Rationale: ADR/evidence trong Ch25 — verify implementation traceability |

| 6 | Mục #6 | Rationale: ADR/evidence trong Ch25 — verify implementation traceability |

| 7 | Mục #7 | Rationale: ADR/evidence trong Ch25 — verify implementation traceability |

| 8 | Mục #8 | Rationale: ADR/evidence trong Ch25 — verify implementation traceability |

| 9 | Mục #9 | Rationale: ADR/evidence trong Ch25 — verify implementation traceability |

| 10 | Mục #10 | Rationale: ADR/evidence trong Ch25 — verify implementation traceability |

| 11 | Mục #11 | Rationale: ADR/evidence trong Ch25 — verify implementation traceability |

| 12 | Mục #12 | Rationale: ADR/evidence trong Ch25 — verify implementation traceability |

| 13 | Mục #13 | Rationale: ADR/evidence trong Ch25 — verify implementation traceability |

| 14 | Mục #14 | Rationale: ADR/evidence trong Ch25 — verify implementation traceability |

| 15 | Mục #15 | Rationale: ADR/evidence trong Ch25 — verify implementation traceability |

| 16 | Mục #16 | Rationale: ADR/evidence trong Ch25 — verify implementation traceability |

| 17 | Mục #17 | Rationale: ADR/evidence trong Ch25 — verify implementation traceability |

| 18 | Mục #18 | Rationale: ADR/evidence trong Ch25 — verify implementation traceability |

| 19 | Mục #19 | Rationale: ADR/evidence trong Ch25 — verify implementation traceability |

| 20 | Mục #20 | Rationale: ADR/evidence trong Ch25 — verify implementation traceability |

| 21 | Mục #21 | Rationale: ADR/evidence trong Ch25 — verify implementation traceability |

| 22 | Mục #22 | Rationale: ADR/evidence trong Ch25 — verify implementation traceability |

| 23 | Mục #23 | Rationale: ADR/evidence trong Ch25 — verify implementation traceability |

| 24 | Mục #24 | Rationale: ADR/evidence trong Ch25 — verify implementation traceability |

| 25 | Mục #25 | Rationale: ADR/evidence trong Ch25 — verify implementation traceability |

| 26 | Mục #26 | Rationale: ADR/evidence trong Ch25 — verify implementation traceability |

| 27 | Mục #27 | Rationale: ADR/evidence trong Ch25 — verify implementation traceability |

| 28 | Mục #28 | Rationale: ADR/evidence trong Ch25 — verify implementation traceability |

| 29 | Mục #29 | Rationale: ADR/evidence trong Ch25 — verify implementation traceability |

| 30 | Mục #30 | Rationale: ADR/evidence trong Ch25 — verify implementation traceability |

| 31 | Mục #31 | Rationale: ADR/evidence trong Ch25 — verify implementation traceability |

| 32 | Mục #32 | Rationale: ADR/evidence trong Ch25 — verify implementation traceability |

| 33 | Mục #33 | Rationale: ADR/evidence trong Ch25 — verify implementation traceability |

| 34 | Mục #34 | Rationale: ADR/evidence trong Ch25 — verify implementation traceability |

| 35 | Mục #35 | Rationale: ADR/evidence trong Ch25 — verify implementation traceability |

---
## Phụ lục B2. Withdraw DR/CR 20 biến thể numeric {#phụ-lục-b2}

### B2.01
| Line | TK | DR | CR |
|------|-----|-----|-----|
| 1 | 2110 | 1,010 | |
| 4 | 1111 | | 1,000 |
| 6 | 4120 | | 10 |

### B2.02
| Line | TK | DR | CR |
|------|-----|-----|-----|
| 1 | 2110 | 2,020 | |
| 4 | 1111 | | 2,000 |
| 6 | 4120 | | 20 |

### B2.03
| Line | TK | DR | CR |
|------|-----|-----|-----|
| 1 | 2110 | 3,000 | |
| 4 | 1111 | | 3,000 |
| net | 2110 | 3,000 | |

### B2.04
| Line | TK | DR | CR |
|------|-----|-----|-----|
| 1 | 2110 | 4,040 | |
| 4 | 1111 | | 4,000 |
| 6 | 4120 | | 40 |

### B2.05
| Line | TK | DR | CR |
|------|-----|-----|-----|
| 1 | 2110 | 5,050 | |
| 4 | 1111 | | 5,000 |
| 6 | 4120 | | 50 |

### B2.06
| Line | TK | DR | CR |
|------|-----|-----|-----|
| 1 | 2110 | 6,000 | |
| 4 | 1111 | | 6,000 |
| net | 2110 | 6,000 | |

### B2.07
| Line | TK | DR | CR |
|------|-----|-----|-----|
| 1 | 2110 | 7,070 | |
| 4 | 1111 | | 7,000 |
| 6 | 4120 | | 70 |

### B2.08
| Line | TK | DR | CR |
|------|-----|-----|-----|
| 1 | 2110 | 8,080 | |
| 4 | 1111 | | 8,000 |
| 6 | 4120 | | 80 |

### B2.09
| Line | TK | DR | CR |
|------|-----|-----|-----|
| 1 | 2110 | 9,000 | |
| 4 | 1111 | | 9,000 |
| net | 2110 | 9,000 | |

### B2.10
| Line | TK | DR | CR |
|------|-----|-----|-----|
| 1 | 2110 | 1,000,100 | |
| 4 | 1111 | | 1,000,000 |
| 6 | 4120 | | 100 |

### B2.11
| Line | TK | DR | CR |
|------|-----|-----|-----|
| 1 | 2110 | 1,100,110 | |
| 4 | 1111 | | 1,100,000 |
| 6 | 4120 | | 110 |

### B2.12
| Line | TK | DR | CR |
|------|-----|-----|-----|
| 1 | 2110 | 1,200,000 | |
| 4 | 1111 | | 1,200,000 |
| net | 2110 | 1,200,000 | |

### B2.13
| Line | TK | DR | CR |
|------|-----|-----|-----|
| 1 | 2110 | 1,300,130 | |
| 4 | 1111 | | 1,300,000 |
| 6 | 4120 | | 130 |

### B2.14
| Line | TK | DR | CR |
|------|-----|-----|-----|
| 1 | 2110 | 1,400,140 | |
| 4 | 1111 | | 1,400,000 |
| 6 | 4120 | | 140 |

### B2.15
| Line | TK | DR | CR |
|------|-----|-----|-----|
| 1 | 2110 | 1,500,000 | |
| 4 | 1111 | | 1,500,000 |
| net | 2110 | 1,500,000 | |

### B2.16
| Line | TK | DR | CR |
|------|-----|-----|-----|
| 1 | 2110 | 1,600,160 | |
| 4 | 1111 | | 1,600,000 |
| 6 | 4120 | | 160 |

### B2.17
| Line | TK | DR | CR |
|------|-----|-----|-----|
| 1 | 2110 | 1,700,170 | |
| 4 | 1111 | | 1,700,000 |
| 6 | 4120 | | 170 |

### B2.18
| Line | TK | DR | CR |
|------|-----|-----|-----|
| 1 | 2110 | 1,800,000 | |
| 4 | 1111 | | 1,800,000 |
| net | 2110 | 1,800,000 | |

### B2.19
| Line | TK | DR | CR |
|------|-----|-----|-----|
| 1 | 2110 | 1,900,190 | |
| 4 | 1111 | | 1,900,000 |
| 6 | 4120 | | 190 |

### B2.20
| Line | TK | DR | CR |
|------|-----|-----|-----|
| 1 | 2110 | 2,000,200 | |
| 4 | 1111 | | 2,000,000 |
| 6 | 4120 | | 200 |

---
## Phụ lục B3. IBFT DR/CR 15 biến thể {#phụ-lục-b3}

#### B3.1 p=5,000 f=100 nap=50
profit=50 gross=5100

#### B3.2 p=10,000 f=200 nap=100
profit=100 gross=10200

#### B3.3 p=15,000 f=300 nap=150
profit=150 gross=15300

#### B3.4 p=20,000 f=400 nap=0
profit=400 gross=20400

#### B3.5 p=25,000 f=0 nap=50
profit=-50 gross=25000

#### B3.6 p=30,000 f=100 nap=100
profit=0 gross=30100

#### B3.7 p=35,000 f=200 nap=150
profit=50 gross=35200

#### B3.8 p=40,000 f=300 nap=0
profit=300 gross=40300

#### B3.9 p=45,000 f=400 nap=50
profit=350 gross=45400

#### B3.10 p=50,000 f=0 nap=100
profit=-100 gross=50000

#### B3.11 p=55,000 f=100 nap=150
profit=-50 gross=55100

#### B3.12 p=60,000 f=200 nap=0
profit=200 gross=60200

#### B3.13 p=65,000 f=300 nap=50
profit=250 gross=65300

#### B3.14 p=70,000 f=400 nap=100
profit=300 gross=70400

#### B3.15 p=75,000 f=0 nap=150
profit=-150 gross=75000

---
## Phụ lục G2. T-account batch 30 scenarios {#phụ-lục-g2}

**G2-01** WD: 2110 DR 3,030 · 1111 CR 3,000 · 4120 CR 30

**G2-02** WD: 2110 DR 6,060 · 1111 CR 6,000 · 4120 CR 60

**G2-03** WD: 2110 DR 9,090 · 1111 CR 9,000 · 4120 CR 90

**G2-04** WD: 2110 DR 12,120 · 1111 CR 12,000 · 4120 CR 120

**G2-05** WD: 2110 DR 15,150 · 1111 CR 15,000 · 4120 CR 150

**G2-06** WD: 2110 DR 18,180 · 1111 CR 18,000 · 4120 CR 180

**G2-07** WD: 2110 DR 21,210 · 1111 CR 21,000 · 4120 CR 210

**G2-08** WD: 2110 DR 24,240 · 1111 CR 24,000 · 4120 CR 240

**G2-09** WD: 2110 DR 27,270 · 1111 CR 27,000 · 4120 CR 270

**G2-10** WD: 2110 DR 30,300 · 1111 CR 30,000 · 4120 CR 300

**G2-11** WD: 2110 DR 33,330 · 1111 CR 33,000 · 4120 CR 330

**G2-12** WD: 2110 DR 36,360 · 1111 CR 36,000 · 4120 CR 360

**G2-13** WD: 2110 DR 39,390 · 1111 CR 39,000 · 4120 CR 390

**G2-14** WD: 2110 DR 42,420 · 1111 CR 42,000 · 4120 CR 420

**G2-15** WD: 2110 DR 45,450 · 1111 CR 45,000 · 4120 CR 450

**G2-16** WD: 2110 DR 48,480 · 1111 CR 48,000 · 4120 CR 480

**G2-17** WD: 2110 DR 51,510 · 1111 CR 51,000 · 4120 CR 510

**G2-18** WD: 2110 DR 54,540 · 1111 CR 54,000 · 4120 CR 540

**G2-19** WD: 2110 DR 57,570 · 1111 CR 57,000 · 4120 CR 570

**G2-20** WD: 2110 DR 60,600 · 1111 CR 60,000 · 4120 CR 600

**G2-21** WD: 2110 DR 63,630 · 1111 CR 63,000 · 4120 CR 630

**G2-22** WD: 2110 DR 66,660 · 1111 CR 66,000 · 4120 CR 660

**G2-23** WD: 2110 DR 69,690 · 1111 CR 69,000 · 4120 CR 690

**G2-24** WD: 2110 DR 72,720 · 1111 CR 72,000 · 4120 CR 720

**G2-25** WD: 2110 DR 75,750 · 1111 CR 75,000 · 4120 CR 750

**G2-26** WD: 2110 DR 78,780 · 1111 CR 78,000 · 4120 CR 780

**G2-27** WD: 2110 DR 81,810 · 1111 CR 81,000 · 4120 CR 810

**G2-28** WD: 2110 DR 84,840 · 1111 CR 84,000 · 4120 CR 840

**G2-29** WD: 2110 DR 87,870 · 1111 CR 87,000 · 4120 CR 870

**G2-30** WD: 2110 DR 90,900 · 1111 CR 90,000 · 4120 CR 900

---
## Phụ lục J. ACC-E03 / X-E12 / X-E13 deep dive {#phụ-lục-j-cross-acceptance}

**ACC-E03:** freeze on accept — `frozen=gross` trước bank SETTLE; ledger có thể POSTED (O1 A).

**X-E12:** RELEASE sau SETTLE forbidden — state machine terminal.

**X-E13:** UNKNOWN → poll — frozen unchanged.

---
## Phụ lục K. vol-02 CK checklist withdraw/IBFT {#phụ-lục-k-vol02-ck}

- CK-06 — vol-02 review checklist item 6

- CK-07 — vol-02 review checklist item 7

- CK-08 — vol-02 review checklist item 8

- CK-09 — vol-02 review checklist item 9

- CK-10 — vol-02 review checklist item 10

---
## Phụ lục L. Messaging envelope full spec {#phụ-lục-l-messaging}

```json
{"command":"WITHDRAW_PAYOUT","businessRef":"wd-1","principal":100000,"fee":1000,"gross":101000,"memberId":"M001","bankAccountId":"BA001"}
```
---
## Phụ lục M. Frozen vs 2110 reconciliation SQL {#phụ-lục-m-frozen-recon}

```sql
-- Policy A: 2110 already DR; SUM frozen should match in-flight withdraw gross
SELECT SUM(frozen) FROM wallet_balance WHERE lane='USER';
-- Compare to open withdraw count * gross — W5 report
```
---
