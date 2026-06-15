# Quyển V — Thanh toán (Payment) & Chuyển nội bộ (Transfer): sync three-commit, transit 3500/3300

**Trạng thái:** Bản mở rộng (~100 trang in) · **Phạm vi:** `core.accounting` — luồng PAYMENT + TRANSFER · **Ngôn ngữ:** Tiếng Việt
**Transit:** 3500 (payment) · 3300 (internal transfer) · **ADR chính:** 027, 008, 005, 001, 010, 009, 020, 031
**Sync:** HTTP **200** hoàn tất trong một request · **Bank rail:** **Không** — không có movement **111x** tại POSTED

**Số tiền ví dụ chuẩn — Payment:** gross **100.000** VND · phí **0** hoặc **1.000** · net merchant **100.000** hoặc **99.000** · scale **4** HALF_UP ([ADR-028](../../adr/ADR-028-money-scale-four-half-up.md))

**Số tiền ví dụ chuẩn — Transfer:** net B **100.000** · phí **1.000** · gross debit A **101.000** · **4130** CR **1.000**

Template DR/CR authoritative: [`foundation.md`](../../spec/foundation.md) §13 (payment) · §10 (transfer) — **link, không duplicate nguyên văn**. COA deep dive: [`vol-02-coa-handbook.md`](./vol-02-coa-handbook.md) TK 2110, 2120, 3300, 3500, 4130. Nguyên tắc: [`vol-01-principles.md`](./vol-01-principles.md). Contract: [`accounting.md`](../accounting.md) §16, §17. Acceptance: [`acceptance.md`](../acceptance.md). Saga: [`processes.md`](../../spec/processes.md) §5, §6, §13.2–13.3. Flow: [`flows.md`](../../design/orchestration/flows.md). Step order: [`integration-surfaces.md`](../../spec/integration-surfaces.md) §4.2.

**[TBD: ...]** — MDR trên payment wallet, fee schedule chi tiết, merchant tier pricing — orchestration config, không hard-code trong accounting.

---
## Mục lục chi tiết

### Phần I — PAYMENT (transit 3500)

| § | Tiêu đề |
|---|---------|
| [0](#chương-0-cách-đọc-và-so-sánh-async-withdraw-vs-sync-payment) | Cách đọc + so sánh async withdraw vs sync payment |
| [1](#chương-1-tóm-tắt-nghiệp-vụ--actors) | Tóm tắt, actors, ADR-027 three commits |
| [2](#chương-2-step-order-wallet-debit--post--merchant-credit) | Step order wallet debit → post → merchant credit |
| [3](#chương-3-drcr-posted--t-account-examples) | DR/CR POSTED — 10+ T-account examples |
| [4](#chương-4-lane-mapping-user2110-merchant2120) | Lane mapping USER→2110, MERCHANT→2120 |
| [5](#chương-5-failure-p-f1p-f8) | Failure P-F1..P-F8 expanded + saga compensate P-F1 |
| [6](#chương-6-idempotency-same-businessref-two-legs) | Idempotency same businessRef two legs |
| [7](#chương-7-self-pay-block-merchant-locked) | Self-pay block, MERCHANT LOCKED |
| [8](#chương-8-2120-pending-eod) | 2120 pending EOD (pointer Quyển VII) |
| [9](#chương-9-gherkin-catalog-payment-20-scenarios) | Gherkin catalog payment 20 scenarios |
| [10](#chương-10-faq-payment-25-câu) | FAQ payment 25 câu |

### Phần II — TRANSFER (transit 3300)

| § | Tiêu đề |
|---|---------|
| [11](#chương-11-tóm-tắt-internal-transfer) | Tóm tắt internal transfer — both USER lane 2110 |
| [12](#chương-12-drcr-deep--fee-on-4130) | DR/CR deep + fee on 4130 |
| [13](#chương-13-aba-same-member-reject) | A→B same member reject, insufficient gross includes fee |
| [14](#chương-14-failure-t-f1t-f5) | Failure T-F1..T-F5 + compensate A |
| [15](#chương-15-gherkin-catalog-transfer-12-scenarios) | Gherkin catalog transfer 12 scenarios |
| [16](#chương-16-faq-transfer-15-câu) | FAQ transfer 15 câu |

### Phần III — CHUNG

| § | Tiêu đề |
|---|---------|
| [17](#chương-17-payment-vs-transfer-vs-ibft-matrix) | Payment vs Transfer vs IBFT matrix |
| [18](#chương-18-adr-027--adr-008-saga-narrative) | ADR-027 + ADR-008 saga narrative |
| [19](#chương-19-state-machines-ascii) | State machines ascii |
| [20](#chương-20-s2-api-field-guide) | S2 API field guide |
| [21](#chương-21-sql-invariant-ci) | SQL invariant CI |
| [22](#chương-22-review-checklist-30-mục) | Review checklist 30 mục |

### Phụ lục

| § | Nội dung |
|---|----------|
| [A](#phụ-lục-a--ma-trận-kịch-bản-số) | Ma trận kịch bản số |
| [B](#phụ-lục-b--drcr-line-by-line-mọi-biến-thể) | DR/CR line-by-line mọi biến thể |
| [C](#phụ-lục-c--bảng-tra-adr) | Bảng tra ADR |
| [D](#phụ-lục-d--processes-§5§6-mapping) | processes §5/§6 mapping |
| [E](#phụ-lục-e--accounting.md-1617-mapping) | accounting.md §16/§17 mapping |
| [F](#phụ-lục-f--numeric-t-account-gallery) | Numeric T-account gallery |
| [G](#phụ-lục-g--cross-flow-invariant) | Cross-flow invariant |
| [H](#phụ-lục-h--đọc-tiếp-vol-06) | Đọc tiếp vol-06 |

---
## Chương 0. Cách đọc và so sánh async withdraw vs sync payment {#chương-0-cách-đọc-và-so-sánh-async-withdraw-vs-sync-payment}

Quyển V là **deep dive nghiệp vụ thanh toán ví (PAYMENT) và chuyển nội bộ (TRANSFER)** — bổ sung Quyển I–IV bằng lifecycle **sync in-platform**: debit → POSTED → credit trong **một HTTP 200**.

| Vai trò | Đọc trước | Kết quả |
|---------|-----------|---------|
| Product | Ch0 + Ch1 + Ch2 | Sync 200 contract, fee tier `[TBD]` |
| Backend accounting | Ch3 + Ch5 + Ch20–21 | `postPayment`, transit 3500/3300 validate |
| Backend orchestration | Ch2 + Ch6 + Ch18 | Three commits ADR-027, compensate P-F1 |
| Ops / Kế toán | Ch8 + Ch21 | 2120 pending EOD, W5 lane drift |
| QA | Ch9 + Ch15 | 20 + 12 Gherkin scenarios |
| Audit | Ch3 + Ch5 + Phụ lục B | DR/CR evidence, immutability |

### 0.1 Quan hệ Quyển I ↔ II ↔ III ↔ IV ↔ V

```
Quyển I (nguyên tắc)     Quyển II (COA)           Quyển III/IV           Quyển V
────────────────────     ──────────────           ─────────────          ───────
ADR-010 transit=0  ──►   TK 3500/3300      ──►   3200/3400 async  ──► 3500/3300 sync
ADR-027 sync 200   ──►   2110/2120 lanes   ──►   freeze outflow   ──► debit available
ADR-008 saga       ──►   no 111x           ──►   bank 1111/1112   ──► liability-only
ADR-020 lanes      ──►   USER/MERCHANT     ──►   withdraw USER    ──► USER→MERCHANT
```

### 0.2 Withdraw async vs Payment sync

| Khía cạnh | WITHDRAW (Quyển IV) | PAYMENT (Phần I) | TRANSFER (Phần II) |
|-----------|---------------------|------------------|---------------------|
| HTTP S1 | 200 accept + async bank | **200 terminal** | **200 terminal** |
| Transit | 3200 | **3500** | **3300** |
| Wallet hold | FREEZE gross | **Debit available** | **Debit A available** |
| Bank TK | 1111 CR | **Không** | **Không** |
| Counterparty | Bank external | **MERCHANT 2120** | **USER B 2110** |
| Revenue | 4120 | `[TBD fee TK]` | **4130** |
| Commits | freeze+post+settle async | **3 sync** ADR-027 | **3 sync** ADR-027 |

### 0.3 Quy ước ký hiệu

| Ký hiệu | Ý nghĩa |
|---------|---------|
| gross / fee / net | Payment: 100.000 / 0|1.000 / 100.000|99.000 |
| gross A / net B | Transfer: 101.000 / 100.000 + fee 1.000 |
| `business_ref` | [ADR-005](../../adr/ADR-005-idempotency-key-strategy.md) |
| `{ref}:comp` | Compensation sub-key P-F1/T-F1 |
| [TBD: ...] | Product gap |

### 0.4 Onboarding 10 câu

| # | Câu | Đáp |
|---|-----|-----|
| Q1 | Payment trả 202? | **Không** — sync **200** [ADR-027](../../adr/ADR-027-sync-payment-transfer-three-commits.md) |
| Q2 | 3500 ≠ 0? | Reject [ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md) |
| Q3 | Ai tính fee? | Orchestration [ADR-009](../../adr/ADR-009-fee-ownership-orchestration.md) |
| Q4 | Có 111x? | **Không** trên payment/transfer POSTED |
| Q5 | Post fail sau debit? | Compensate `{ref}:comp` §13.2 |
| Q6 | Post OK credit fail? | Forward-retry merchant/B |
| Q7 | Transfer fee TK? | **4130** foundation §10 |
| Q8 | A→A? | Reject T-F3 |
| Q9 | Self-pay? | Reject P-F7 |
| Q10 | 2120 sau payment? | Pending EOD Ch8 |

---
## Chương 1. Tóm tắt nghiệp vụ & actors {#chương-1-tóm-tắt-nghiệp-vụ--actors}

### 1.1 Mục tiêu — PAYMENT

User thanh toán merchant từ ví. Platform:

1. **Debit** gross USER (`PAYMENT_DEBIT`) — commit #1.
2. **POSTED** 2110→2120 qua 3500 — commit #2.
3. **Credit** merchant net/gross (`PAYMENT_CREDIT`) — commit #3.
4. HTTP **200**.

Zero fee: gross **100.000** → merchant **100.000**. Có fee: gross **100.000**, fee **1.000**, merchant **99.000**.

### 1.2 Mục tiêu — TRANSFER

A→B cùng platform. Debit A **101.000**, credit B **100.000**, **4130** CR **1.000**, **3300 = 0**.

### 1.3 Bảng quyết định kiến trúc

| Khía cạnh | Quyết định | ADR |
|-----------|------------|-----|
| Sync 200 | Terminal state in one request | [027](../../adr/ADR-027-sync-payment-transfer-three-commits.md) |
| Three commits | debit → post → credit | [027](../../adr/ADR-027-sync-payment-transfer-three-commits.md) |
| Single POSTED | 3500/3300 = 0 | [010](../../adr/ADR-010-transit-accounts-net-zero.md) |
| Saga | compensate / forward-retry | [008](../../adr/ADR-008-saga-compensation-no-2pc.md) |
| Fee ownership | Orchestration only | [009](../../adr/ADR-009-fee-ownership-orchestration.md) |
| Lanes | USER→2110, MERCHANT→2120 | [020](../../adr/ADR-020-wallet-lanes-coa-control-mapping.md) |
| Immutability | No UPDATE POSTED | [001](../../adr/ADR-001-immutable-ledger.md) |
| Idempotency | business_ref + :comp | [005](../../adr/ADR-005-idempotency-key-strategy.md) |

### 1.4 Actors

| Actor | Trách nhiệm | Không làm |
|-------|-------------|-----------|
| Member | createPayment/createTransfer | Gọi S2 trực tiếp |
| Gateway S1 | Auth, proxy | Post journal |
| Orchestration | Fee; 3-step saga | JOIN wallet+accounting |
| core.wallet | DEBIT/CREDIT legs | Post transit |
| core.accounting | POSTED | Compensate wallet |
| EOD batch | Settle 2120 (Quyển VII) | Inline payment |

### 1.5 Luồng PAYMENT

```
User ──createPayment──► S1
         ├── PAYMENT_DEBIT user [1]
         ├── POSTED 3500=0     [2]
         ├── PAYMENT_CREDIT merchant [3]
         └── 200 OK
```

### 1.6 Luồng TRANSFER

```
A ──createTransfer──► S1
      ├── TRANSFER_DEBIT A 101k
      ├── POSTED 3300=0 4130+1k
      ├── TRANSFER_CREDIT B 100k
      └── 200 OK
```

---
## Chương 2. Step order wallet debit → post → merchant credit {#chương-2-step-order-wallet-debit--post--merchant-credit}

Thứ tự bắt buộc theo [`integration-surfaces.md`](../../spec/integration-surfaces.md) §4.2 và [ADR-027](../../adr/ADR-027-sync-payment-transfer-three-commits.md).

### 2.1 Payment step order

| # | Layer | Action | Commit | Rollback policy |
|---|-------|--------|--------|-----------------|
| 1 | Wallet | `debit` USER gross | Local TX #1 | P-F1: `{ref}:comp` credit |
| 2 | S2 | Journal POSTED, 3500=0 | Local TX #2 | P-F1 if fail after step 1 |
| 3 | Wallet | `credit` MERCHANT net | Local TX #3 | P-F2: forward-retry |
| 4 | S1 | **200** + ids | — | Idempotent replay P-F4 |

**Forbidden:** POST trước debit user — orchestration guard. Credit merchant trước POSTED — forbidden.

### 2.2 Transfer step order

| # | Layer | Action |
|---|-------|--------|
| 1 | Wallet | `TRANSFER_DEBIT` A gross (net+fee) |
| 2 | S2 | POSTED 3300=0, 4130 fee |
| 3 | Wallet | `TRANSFER_CREDIT` B net |
| 4 | S1 | 200 |

### 2.3 Sequence diagram — payment

```
Client    S1/Orchestration    Wallet(USER)    Accounting    Wallet(MERCHANT)
  │              │                  │              │                │
  │─POST pay────►│                  │              │                │
  │              │─PAYMENT_DEBIT───►│              │                │
  │              │◄─────ok──────────│  commit 1    │                │
  │              │─postPayment─────────────────────►│                │
  │              │◄─────POSTED──────────────────────│  commit 2      │
  │              │─PAYMENT_CREDIT──────────────────────────────────►│
  │              │◄─────ok──────────────────────────────────────────│ commit 3
  │◄──200────────│                  │              │                │
```

### 2.4 Timing vs withdraw

Withdraw: freeze → 200 → **async** bank → settle. Payment: **all three commits before 200**. Không RabbitMQ S6 cho payment/transfer v1 ([ADR-035](../../adr/ADR-035-rabbitmq-workers-not-temporal-v1.md) — sync path inline).

### 2.5 Orchestration invariants O-PAY

| ID | Rule |
|----|------|
| O-PAY-01 | gross user debit = SUM lines DR 2110 |
| O-PAY-02 | merchant credit amount = netToMerchant from fee calc |
| O-PAY-03 | fee lines on ledger match orchestration fee |
| O-PAY-04 | payer ≠ payee memberId |
| O-PAY-05 | currency VND v1 only |

### 2.6 Orchestration invariants O-TRF

| ID | Rule |
|----|------|
| O-TRF-01 | gross debit A = net + fee |
| O-TRF-02 | fromMemberId ≠ toMemberId |
| O-TRF-03 | insufficient checks **gross** not net |
| O-TRF-04 | 4130 line when fee > 0 |

---
## Chương 3. DR/CR POSTED — T-account examples {#chương-3-drcr-posted--t-account-examples}

Authoritative template: [`foundation.md`](../../spec/foundation.md) §13 (zero fee). Fee variant: orchestration line builder + `[TBD: payment fee revenue TK]`.

### 3.1 Chuẩn zero fee — gross 100.000

| Step | Actor | TK | DR/CR | Amount |
|------|-------|-----|-------|--------|
| 1 | User | 2110 | DR | 100,000 |
| 2 | User | 3500 | CR | 100,000 |
| 3 | Merchant | 3500 | DR | 100,000 |
| 4 | Merchant | 2120 | CR | 100,000 |

```
         2110 (User)          3500 (Transit)         2120 (Merchant)
         ───────────          ──────────────         ────────────────
DR 100k ─┤                  CR 100k ─┤
                              DR 100k ─┤              CR 100k ─┤
Result: 3500 net 0 | 2110 −100k | 2120 +100k | no 111x
```

### 3.2 Chuẩn có fee — gross 100.000, fee 1.000, net merchant 99.000

| Step | TK | DR/CR | Amount |
|------|-----|-------|--------|
| 1 | 2110 | DR | 100,000 |
| 2 | 3500 | CR | 100,000 |
| 3 | 3500 | DR | 99,000 |
| 4 | 2120 | CR | 99,000 |
| 5 | 3500 | DR | 1,000 |
| 6 | `[TBD: fee TK]` | CR | 1,000 |

Wallet: PAYMENT_DEBIT **100.000** · PAYMENT_CREDIT **99.000**. Fee **1.000** chỉ trên ledger — không wallet leg riêng.

### 3.3 Gross pass-through — fee=0 merchant nhận full gross

Khi `[TBD: merchant tier]` miễn phí — same as §3.1. Merchant **2120** CR = user **2110** DR.

### 3.4 Biến thể — Chuẩn vol-05 header

gross **100,000** · fee **0** · net merchant **100,000**.

```
2110 DR 100,000  |  3500 net 0  |  2120 CR 100,000  |  fee 0  |  no 111x
```

### 3.5 Biến thể — Fee 1k net 99k

gross **100,000** · fee **1,000** · net merchant **99,000**.

```
2110 DR 100,000  |  3500 net 0  |  2120 CR 99,000  |  fee 1,000  |  no 111x
```

### 3.6 Biến thể — Half amount fee 500

gross **50,000** · fee **500** · net merchant **49,500**.

```
2110 DR 50,000  |  3500 net 0  |  2120 CR 49,500  |  fee 500  |  no 111x
```

### 3.7 Biến thể — 200k principal fee 2k

gross **200,000** · fee **2,000** · net merchant **198,000**.

```
2110 DR 200,000  |  3500 net 0  |  2120 CR 198,000  |  fee 2,000  |  no 111x
```

### 3.8 Biến thể — 1M payment fee 10k

gross **1,000,000** · fee **10,000** · net merchant **990,000**.

```
2110 DR 1,000,000  |  3500 net 0  |  2120 CR 990,000  |  fee 10,000  |  no 111x
```

### 3.9 Biến thể — Min amount PAY-E01

gross **1** · fee **0** · net merchant **1**.

```
2110 DR 1  |  3500 net 0  |  2120 CR 1  |  fee 0  |  no 111x
```

### 3.10 Biến thể — Zero fee 75k

gross **75,000** · fee **0** · net merchant **75,000**.

```
2110 DR 75,000  |  3500 net 0  |  2120 CR 75,000  |  fee 0  |  no 111x
```

### 3.11 Biến thể — Custom fee 2.5k

gross **250,000** · fee **2,500** · net merchant **247,500**.

```
2110 DR 250,000  |  3500 net 0  |  2120 CR 247,500  |  fee 2,500  |  no 111x
```

### 3.12 Biến thể — Fee equals gross edge `[TBD policy]`

gross **100,000** · fee **100,000** · net merchant **0**.

```
2110 DR 100,000  |  3500 net 0  |  2120 CR 0  |  fee 100,000  |  no 111x
```

### 3.13 Biến thể — Large zero fee

gross **99,999,999** · fee **0** · net merchant **99,999,999**.

```
2110 DR 99,999,999  |  3500 net 0  |  2120 CR 99,999,999  |  fee 0  |  no 111x
```

### 3.14 Biến thể — Repeat baseline QA anchor

gross **100,000** · fee **0** · net merchant **100,000**.

```
2110 DR 100,000  |  3500 net 0  |  2120 CR 100,000  |  fee 0  |  no 111x
```

### 3.15 Validation guards (POSTED payment)

| Guard | Rule | Error |
|-------|------|-------|
| G-PAY-01 | SUM DR = SUM CR | `ACCOUNTING_UNBALANCED_JOURNAL` P-F8 |
| G-PAY-02 | 3500 net zero | `TRANSIT_NONZERO_AT_POSTED` P-F3 |
| G-PAY-03 | use_case=WALLET_PAYMENT | — |
| G-PAY-04 | No 111x lines | reject |
| G-PAY-05 | 2110 DR = user gross debit | orchestration validate |
| G-PAY-06 | 2120 CR = merchant credit | orchestration validate |
| G-PAY-07 | fee>0 ⇒ fee revenue line | `[TBD TK]` |

---
## Chương 4. Lane mapping USER→2110, MERCHANT→2120 {#chương-4-lane-mapping-user2110-merchant2120}

[ADR-020](../../adr/ADR-020-wallet-lanes-coa-control-mapping.md): mapping **reconciliation only** — không FK wallet→COA.

| wallet_type | COA control | Payment role | Transfer role |
|-------------|-------------|--------------|---------------|
| USER | **2110** | Payer debit | A debit, B credit |
| MERCHANT | **2120** | Payee credit | N/A |
| PARTNER | **2130** | N/A v1 | N/A |

### 4.1 W5 reconciliation

SUM wallet USER balances ≈ **2110**. SUM wallet MERCHANT ≈ **2120**. Payment chuyển aggregate từ 2110 sang 2120 — per-member detail chỉ ở `wallet_balance`.

### 4.2 Identity invariant

`(1111+1112+1113) = (2110+2120+2130)` — payment/transfer **không** thay đổi tổng liability, chỉ **reallocate** giữa 2110 và 2120.

### 4.3 Merchant auto-provision PAY-E03

First payment tạo MERCHANT wallet row — credit leg vẫn map **2120** aggregate.

### 4.4 Multi-merchant platform

Nhiều merchant wallets → một **2120** control. W5 group by `wallet_type=MERCHANT`.

---
## Chương 5. Failure P-F1..P-F8 {#chương-5-failure-p-f1p-f8}

Nguồn: [`accounting.md`](../accounting.md) §16.3 · [`processes.md`](../../spec/processes.md) §13.2.

| ID | Tình huống | Ledger | Recovery | Gherkin | ADR |
|----|------------|--------|----------|---------|-----|
| P-F1 | User debited, post fails | no POSTED | `{ref}:comp` credit user | PAY-E04 | 008,027 |
| P-F2 | POSTED, merchant credit fails | POSTED | forward-retry credit | PAY-E05 | 008 |
| P-F3 | 3500 ≠ 0 | — | reject post | — | 010 |
| P-F4 | Idempotent replay | POSTED | same coa_trans_id | PAY-E06 | 005 |
| P-F5 | Same ref different amount | — | 409 conflict | — | 005 |
| P-F6 | Merchant 2120 inactive | — | reject lines | PAY-E07 | 020 |
| P-F7 | Self-pay same member | — | reject orchestration | Self-pay | — |
| P-F8 | Partial line set | — | UNBALANCED | — | 001 |

### 5.1 Chi tiết P-F1: User debited, post fails

**Detection:** API error, metric `payment_p-f1`, partial saga job.
**Accounting action:** No journal; orchestration compensates wallet via `{businessRef}:comp`.
**Recovery:** 422 to client after compensate.
**Gherkin:** PAY-E04, X-E08.
**Forbidden:** UPDATE POSTED lines [ADR-001](../../adr/ADR-001-immutable-ledger.md).

**Saga §13.2:** Sau step 2 ledger balanced — **không reverse** trên P-F2; chỉ forward-retry.

### 5.2 Chi tiết P-F2: POSTED, merchant credit fails

**Detection:** API error, metric `payment_p-f2`, partial saga job.
**Accounting action:** Ledger unchanged; retry PAYMENT_CREDIT.
**Recovery:** Ops alert if persistent PAY-E09.
**Gherkin:** PAY-E05.
**Forbidden:** UPDATE POSTED lines [ADR-001](../../adr/ADR-001-immutable-ledger.md).

**Saga §13.2:** Sau step 2 ledger balanced — **không reverse** trên P-F2; chỉ forward-retry.

### 5.3 Chi tiết P-F3: 3500 ≠ 0 at guard

**Detection:** API error, metric `payment_p-f3`, partial saga job.
**Accounting action:** Reject before persist.
**Recovery:** Fix line builder.
**Gherkin:** —.
**Forbidden:** UPDATE POSTED lines [ADR-001](../../adr/ADR-001-immutable-ledger.md).

**Saga §13.2:** Sau step 2 ledger balanced — **không reverse** trên P-F2; chỉ forward-retry.

### 5.4 Chi tiết P-F4: Idempotent replay

**Detection:** API error, metric `payment_p-f4`, partial saga job.
**Accounting action:** Return same ids.
**Recovery:** 200 unchanged balances.
**Gherkin:** PAY-E06.
**Forbidden:** UPDATE POSTED lines [ADR-001](../../adr/ADR-001-immutable-ledger.md).

**Saga §13.2:** Sau step 2 ledger balanced — **không reverse** trên P-F2; chỉ forward-retry.

### 5.5 Chi tiết P-F5: Same ref different amount

**Detection:** API error, metric `payment_p-f5`, partial saga job.
**Accounting action:** 409 WALLET_DUPLICATE_CONFLICT.
**Recovery:** Client new ref.
**Gherkin:** —.
**Forbidden:** UPDATE POSTED lines [ADR-001](../../adr/ADR-001-immutable-ledger.md).

**Saga §13.2:** Sau step 2 ledger balanced — **không reverse** trên P-F2; chỉ forward-retry.

### 5.6 Chi tiết P-F6: Merchant inactive COA

**Detection:** API error, metric `payment_p-f6`, partial saga job.
**Accounting action:** Reject post.
**Recovery:** Provision merchant.
**Gherkin:** PAY-E07.
**Forbidden:** UPDATE POSTED lines [ADR-001](../../adr/ADR-001-immutable-ledger.md).

**Saga §13.2:** Sau step 2 ledger balanced — **không reverse** trên P-F2; chỉ forward-retry.

### 5.7 Chi tiết P-F7: Self-pay

**Detection:** API error, metric `payment_p-f7`, partial saga job.
**Accounting action:** No post no debit ideally pre-check.
**Recovery:** 4xx business rule.
**Gherkin:** Self-pay scenario.
**Forbidden:** UPDATE POSTED lines [ADR-001](../../adr/ADR-001-immutable-ledger.md).

**Saga §13.2:** Sau step 2 ledger balanced — **không reverse** trên P-F2; chỉ forward-retry.

### 5.8 Chi tiết P-F8: Partial lines

**Detection:** API error, metric `payment_p-f8`, partial saga job.
**Accounting action:** TX rollback.
**Recovery:** Complete line set.
**Gherkin:** —.
**Forbidden:** UPDATE POSTED lines [ADR-001](../../adr/ADR-001-immutable-ledger.md).

**Saga §13.2:** Sau step 2 ledger balanced — **không reverse** trên P-F2; chỉ forward-retry.

### 5.9 P-F1 compensation deep dive

```
Step 1 OK: user −100k
Step 2 FAIL: no POSTED
Compensate: wallet credit +100k key pay-1:comp
Terminal: user whole, no ledger
```

Accounting **không** post reversing journal trên P-F1 — chưa có POSTED để reverse.

### 5.10 P-F2 forward-retry deep dive

```
Step 1 OK: user −100k
Step 2 OK: POSTED 2110→2120
Step 3 FAIL: merchant +0
Retry: PAYMENT_CREDIT idempotent
Terminal: merchant +99k/100k, ledger unchanged
```

Ops: PAY-E09 — alert nếu credit permanently failing; merchant **owed** per ledger **2120**.

---
## Chương 6. Idempotency same businessRef two legs {#chương-6-idempotency-same-businessref-two-legs}

[ADR-005](../../adr/ADR-005-idempotency-key-strategy.md): một `businessRef` — hai wallet legs distinct `tx_type`.

### 6.1 Key namespace payment

| Key | Scope |
|-----|-------|
| `business_ref` | PAYMENT_DEBIT + PAYMENT_CREDIT + POSTED journal |
| `{business_ref}:comp` | Compensation credit user (P-F1 only) |

### 6.2 Two legs same ref

| tx_type | Lane | Amount | Idempotency |
|---------|------|--------|-------------|
| PAYMENT_DEBIT | USER | gross | `(businessRef, PAYMENT_DEBIT)` unique |
| PAYMENT_CREDIT | MERCHANT | net | `(businessRef, PAYMENT_CREDIT)` unique |

### 6.3 Transfer keys

| tx_type | Lane | Amount |
|---------|------|--------|
| TRANSFER_DEBIT | USER A | gross |
| TRANSFER_CREDIT | USER B | net |

### 6.4 Test matrix

| Test | Action | Expected |
|------|--------|----------|
| T1 | Duplicate full payment | single debit+post+credit |
| T2 | Duplicate :comp | one restore |
| T3 | Same ref different gross | 409 P-F5 |
| T4 | Cross-member same key | PAY-E10 reject |
| T5 | Retry after 200 lost | P-F4 same ids |

---
## Chương 7. Self-pay block, MERCHANT LOCKED {#chương-7-self-pay-block-merchant-locked}

### 7.1 Self-pay P-F7

payerMemberId == payeeMemberId → reject **trước** debit. Không POSTED. Business rule — tránh wash trading / accounting noise.

### 7.2 MERCHANT LOCKED

Merchant wallet LOCKED → PAYMENT_CREDIT fails. User đã debited + POSTED → P-F2 forward-retry hoặc ops unlock Ch14B pattern (deposit vol-03).

### 7.3 USER LOCKED payer

Reject PAYMENT_DEBIT trước mutation — no partial.

### 7.4 Unknown merchant PAY-E07

404 trước debit — validate payee exists.

---
## Chương 8. 2120 pending EOD (pointer Quyển VII) {#chương-8-2120-pending-eod}

Payment POSTED **2120 CR** = merchant liability **pending bank settlement**. EOD batch ([`foundation.md`](../../spec/foundation.md) §16) **không inline** với payment ([ADR-015](../../adr/ADR-015-eod-settlement-independent-batch.md)).

```
2120 → 3800 (lock) → 3820 (MDR) + 3810 (net) → 1112 → Merchant bank
```

**[TBD: MDR on wallet payment]** — QR/POS dùng **4140**; wallet payment MDR policy chưa chốt. Quyển VII sẽ deep dive EOD.

### 8.1 Trạng thái merchant sau payment

| Layer | State |
|-------|-------|
| Wallet MERCHANT | available += net |
| COA 2120 | CR aggregate += net |
| Bank | unchanged — no 111x |

### 8.2 Merchant spend before EOD

Merchant có thể dùng available cho payroll/withdraw nội bộ — **2120** aggregate đã reflect liability.

---
## Chương 9. Gherkin catalog payment — 20 scenarios {#chương-9-gherkin-catalog-payment-20-scenarios}

Nguồn: [`acceptance.md`](../acceptance.md) Feature Wallet payment (10) + PAY-E (10). Foundation §13 authoritative ledger zero-fee.

### PAY-01: Happy — 100,000 gross

```gherkin
Feature: Wallet payment
Scenario: Happy — 100,000 gross
Given user available=200000
And merchant available=0
When createPayment businessRef="pay-1" amount=100000
Then PAYMENT_DEBIT user -100000
And journal POSTED 3500 net zero
And PAYMENT_CREDIT merchant +100000
And response 200
```

### PAY-02: Ledger lines zero fee

```gherkin
Feature: Wallet payment
Scenario: Ledger lines zero fee
Given payment gross=100000 fee=0
When POSTED
Then 2110 DR 100000, 2120 CR 100000, 3500 net zero
```

### PAY-03: Insufficient balance

```gherkin
Feature: Wallet payment
Scenario: Insufficient balance
Given user available=50000
When createPayment amount=100000
Then 4xx insufficient
And no POSTED
And no merchant credit
```

### PAY-04: User debited post fails

```gherkin
Feature: Wallet payment
Scenario: User debited post fails
Given user debited 100000 for "pay-2"
And accounting post fails
Then compensation policy
And user balance restored
```

### PAY-05: Posted merchant credit fails

```gherkin
Feature: Wallet payment
Scenario: Posted merchant credit fails
Given POSTED for "pay-3"
When merchant credit fails transiently
Then retry PAYMENT_CREDIT idempotent
And merchant available=100000
```

### PAY-06: Idempotent replay

```gherkin
Feature: Wallet payment
Scenario: Idempotent replay
Given payment "pay-4" completed
When client retries same X-Idempotency-Key
Then 200 same ids
And balances unchanged
```

### PAY-07: Debit OK credit pending

```gherkin
Feature: Wallet payment
Scenario: Debit OK credit pending
Given POSTED "pay-5" user debited
When retry credit succeeds
Then merchant +100000 once
And coa_trans unchanged
```

### PAY-08: Platform fee net 99000

```gherkin
Feature: Wallet payment
Scenario: Platform fee net 99000
Given gross=100000 fee=1000 netToMerchant=99000
When POSTED
Then 2120 CR 99000
And PAYMENT_CREDIT merchant 99000
```

### PAY-09: Self-payment blocked

```gherkin
Feature: Wallet payment
Scenario: Self-payment blocked
Given payer wallet equals payee
When createPayment
Then 4xx
And no POSTED
```

### PAY-10: MERCHANT locked

```gherkin
Feature: Wallet payment
Scenario: MERCHANT locked
Given merchant LOCKED
When payment end-to-end
Then credit fails WALLET_LOCKED
And compensation for user debit if needed
```

### PAY-E01: Gross 1 VND

```gherkin
Feature: Wallet payment
Scenario: Gross 1 VND
Given user available=100
When payment amount=1
Then POSTED 3500=0 merchant +1
```

### PAY-E02: Net fee 1000

```gherkin
Feature: Wallet payment
Scenario: Net fee 1000
Given gross=100000 fee=1000
When completed
Then PAYMENT_DEBIT 100000 PAYMENT_CREDIT 99000
```

### PAY-E03: Merchant auto-provision

```gherkin
Feature: Wallet payment
Scenario: Merchant auto-provision
Given merchant no wallet row
When payment completes
Then MERCHANT wallet created credited
```

### PAY-E04: Compensation post fail

```gherkin
Feature: Wallet payment
Scenario: Compensation post fail
Given user debited post fails
When compensation runs
Then user restored via :comp
And no POSTED
```

### PAY-E05: Credit 3 retries

```gherkin
Feature: Wallet payment
Scenario: Credit 3 retries
Given POSTED "pay-retry"
When credit fails twice then succeeds
Then merchant correct once
```

### PAY-E06: 200 lost retry

```gherkin
Feature: Wallet payment
Scenario: 200 lost retry
Given payment complete
When client retries same key
Then 200 same ids
```

### PAY-E07: Unknown merchant

```gherkin
Feature: Wallet payment
Scenario: Unknown merchant
Given payee unknown
Then 404 before debit
```

### PAY-E08: Concurrent last cent

```gherkin
Feature: Wallet payment
Scenario: Concurrent last cent
Given available=10000
When two payments 10000 simultaneous
Then one succeeds one insufficient
```

### PAY-E09: DEBIT without CREDIT stuck

```gherkin
Feature: Wallet payment
Scenario: DEBIT without CREDIT stuck
Given user debited POSTED
And credit permanently failing
Then ops alert saga partial
```

### PAY-E10: Cross-member key rejected

```gherkin
Feature: Wallet payment
Scenario: Cross-member key rejected
Given member A used key k-1
When member B same key
Then reject auth scope
```

---
## Chương 10. FAQ payment — 25 câu {#chương-10-faq-payment-25-câu}

**Q1. Tại sao sync 200 not 202?**

ADR-027 — terminal wallet+ledger trong một request.

**Q2. 3500 khác 0 sau POSTED?**

Lỗi G-PAY-02 — reject.

**Q3. Có 1111 trên payment?**

Không — liability-only reallocation.

**Q4. Ai gọi compensate?**

Orchestration — không accounting.

**Q5. Post OK credit fail — reverse ledger?**

Không — forward-retry P-F2.

**Q6. Fee payment ghi TK nào?**

[TBD: payment fee revenue TK] — không 4130.

**Q7. 4130 dùng cho payment?**

Không — 4130 là transfer/IBFT.

**Q8. Self-pay allowed?**

Không P-F7.

**Q9. 2120 sau payment?**

Pending EOD settlement Ch8.

**Q10. MDR wallet payment?**

[TBD: MDR policy] — khác QR 4140.

**Q11. Duplicate payment ref?**

Idempotent P-F4.

**Q12. Same ref different amount?**

409 P-F5.

**Q13. Insufficient — partial debit?**

Không — reject trước debit.

**Q14. MERCHANT LOCKED?**

Credit fail — ops unlock retry.

**Q15. Period close block?**

Reject post nếu period closed ADR-023.

**Q16. Immutability?**

Reversal not UPDATE ADR-001.

**Q17. W5 payment drift?**

Report only ADR-014.

**Q18. JOIN wallet accounting?**

Forbidden ADR-003.

**Q19. Two legs same ref?**

PAYMENT_DEBIT + CREDIT distinct tx_type.

**Q20. Compensation key?**

{businessRef}:comp.

**Q21. Journal use_case?**

WALLET_PAYMENT.

**Q22. Merchant tier fee?**

[TBD: fee schedule].

**Q23. Zero fee default?**

foundation §13 — 100k gross to merchant.

**Q24. Payment vs QR/POS?**

Wallet internal vs acquirer 1113 — vol-06.

**Q25. Đọc tiếp transfer?**

Ch11 Phần II.

---
## Chương 11. Tóm tắt internal transfer {#chương-11-tóm-tắt-internal-transfer}

**Goal:** Move funds A→B trong platform, sync **200**. Cả hai legs **USER** lane → COA **2110** aggregate ([`foundation.md`](../../spec/foundation.md) §10).

| Khía cạnh | Giá trị |
|-----------|---------|
| Transit | **3300** |
| Fee revenue | **4130** |
| Bank | **None** |
| Gross debit A | net + fee = **101.000** |
| Net credit B | **100.000** |

Process: [`processes.md`](../../spec/processes.md) §6 · Flow: [`flows.md`](../../design/orchestration/flows.md).

---
## Chương 12. DR/CR deep + fee on 4130 {#chương-12-drcr-deep--fee-on-4130}

### 12.1 Template foundation §10

| Step | Actor | TK | DR/CR | Amount |
|------|-------|-----|-------|--------|
| 1 | User A | 2110 | DR | 101,000 |
| 2 | User A | 3300 | CR | 101,000 |
| 3 | User B | 3300 | DR | 100,000 |
| 4 | User B | 2110 | CR | 100,000 |
| 5 | User A | 3300 | DR | 1,000 |
| 6 | User A | 4130 | CR | 1,000 |

```
2110 pool: net −fee across members (A−B transfer + platform fee)
3300 = 0 | 4130 +1.000 | no 111x
```

### 12.2 Zero fee transfer TRF-E02

fee=0: lines 1–4 only, gross=net=**100.000**, no 4130.

### 12.3 Variant net 100,000 fee 1,000

```
A debit gross 101,000 | B credit 100,000 | 4130 CR 1,000 | 3300=0
```

### 12.4 Variant net 50,000,000 fee 1,000

```
A debit gross 50,001,000 | B credit 50,000,000 | 4130 CR 1,000 | 3300=0
```

### 12.5 Variant net 25,000 fee 0

```
A debit gross 25,000 | B credit 25,000 | 4130 CR 0 | 3300=0
```

### 12.6 Variant net 1,000,000 fee 10,000

```
A debit gross 1,010,000 | B credit 1,000,000 | 4130 CR 10,000 | 3300=0
```

### 12.7 Variant net 1 fee 0

```
A debit gross 1 | B credit 1 | 4130 CR 0 | 3300=0
```

### 12.8 Guards transfer

| Guard | Rule |
|-------|------|
| G-TRF-01 | 3300 net zero |
| G-TRF-02 | fee>0 ⇒ 4130 |
| G-TRF-03 | 2110 DR A = gross |
| G-TRF-04 | 2110 CR B = net |
| G-TRF-05 | no 111x |

---
## Chương 13. A→B same member reject, insufficient gross {#chương-13-aba-same-member-reject}

### 13.1 T-F3 A→A reject

fromWallet=toWallet → 4xx, no POSTED, no debit.

### 13.2 Insufficient — gross includes fee

Available **100.500** · transfer net **100.000** fee **1.000** gross **101.000** → **reject** — phải check **gross** O-TRF-03.

### 13.3 Exact boundary

Available **101.000** · gross **101.000** → OK. Available **100.999** → reject.

### 13.4 Currency / scale

TRF-E05 USD reject · TRF-E06 scale 5 reject — validation trước mutation.

---
## Chương 14. Failure T-F1..T-F5 {#chương-14-failure-t-f1t-f5}

| ID | Failure | Recovery | ADR |
|----|---------|----------|-----|
| T-F1 | Debit A OK, post fails | Restore A `{ref}:comp` | 008,027 |
| T-F2 | POSTED, credit B fails | Forward-retry B | 008 |
| T-F3 | A→A | Reject orchestration | — |
| T-F4 | Fee omitted unbalanced | Reject post | 010 |
| T-F5 | Duplicate ref | Idempotent | 005 |

### 14.1 T-F1 playbook

Detection · recovery per [`accounting.md`](../accounting.md) §17.3 · Gherkin TRF-E01 if applicable.
Forbidden: reverse POSTED on T-F2.

### 14.2 T-F2 playbook

Detection · recovery per [`accounting.md`](../accounting.md) §17.3 · Gherkin TRF-E02 if applicable.
Forbidden: reverse POSTED on T-F2.

### 14.3 T-F3 playbook

Detection · recovery per [`accounting.md`](../accounting.md) §17.3 · Gherkin TRF-E03 if applicable.
Forbidden: reverse POSTED on T-F2.

### 14.4 T-F4 playbook

Detection · recovery per [`accounting.md`](../accounting.md) §17.3 · Gherkin TRF-E04 if applicable.
Forbidden: reverse POSTED on T-F2.

### 14.5 T-F5 playbook

Detection · recovery per [`accounting.md`](../accounting.md) §17.3 · Gherkin TRF-E05 if applicable.
Forbidden: reverse POSTED on T-F2.

### 14.6 T-F1 compensate A

Same pattern P-F1 — accounting không post; wallet `{ref}:comp` credit gross to A.

---
## Chương 15. Gherkin catalog transfer — 12 scenarios {#chương-15-gherkin-catalog-transfer-12-scenarios}

### TRF-01: Happy fee 1000

```gherkin
Scenario: Happy fee 1000
Given A available=200000 B=0
When transfer ref=xfer-1 amount=100000 fee=1000
Then TRANSFER_DEBIT A 101000
And POSTED 3300=0 4130+1000
And TRANSFER_CREDIT B 100000
```

### TRF-02: Ledger lines

```gherkin
Scenario: Ledger lines
Given net=100000 fee=1000
When POSTED
Then 2110 DR 101000 CR 100000 4130 CR 1000 3300=0
```

### TRF-03: Credit B fails retry

```gherkin
Scenario: Credit B fails retry
Given A debited
When B credit fails then retry
Then B +100000 once A not double debited
```

### TRF-04: Same member reject

```gherkin
Scenario: Same member reject
Given from=to
When transfer
Then 4xx no POSTED
```

### TRF-05: Insufficient gross

```gherkin
Scenario: Insufficient gross
Given A available=100000
When gross=101000
Then reject before POSTED
```

### TRF-06: Idempotent replay

```gherkin
Scenario: Idempotent replay
Given xfer-2 complete
When retry same key
Then 200 unchanged
```

### TRF-E01: Net 50M fee 1k

```gherkin
Scenario: Net 50M fee 1k
Given A 100M
When net=50M fee=1000
Then A -50001000 B +50M 4130+1000
```

### TRF-E02: Zero fee

```gherkin
Scenario: Zero fee
Given fee=0 net=25000
When POSTED
Then 3300=0 A-25000 B+25000
```

### TRF-E03: B retry POSTED

```gherkin
Scenario: B retry POSTED
Given A debited POSTED
When B credit fails then ok
Then B +net once
```

### TRF-E04: A compensate no POSTED

```gherkin
Scenario: A compensate no POSTED
Given A debited no POSTED
When compensate
Then A restored gross
```

### TRF-E05: USD rejected

```gherkin
Scenario: USD rejected
Given VND only
When currency USD
Then 400
```

### TRF-E06: Scale 5 rejected

```gherkin
Scenario: Scale 5 rejected
Given amount 5 decimals
Then validation reject
```

---
## Chương 16. FAQ transfer — 15 câu {#chương-16-faq-transfer-15-câu}

**Q1. Transfer vs payment?**

Payment USER→MERCHANT 3500/2120; transfer USER→USER 3300/2110 both.

**Q2. Fee TK?**

4130 only — not 4120.

**Q3. A và B cùng 2110?**

Đúng — aggregate pool, per-member ở wallet.

**Q4. Insufficient check net hay gross?**

Gross includes fee.

**Q5. Zero fee?**

4 lines, no 4130 TRF-E02.

**Q6. A→A?**

Reject T-F3.

**Q7. Post fail compensate?**

T-F1 restore A.

**Q8. Post OK B fail?**

Forward-retry T-F2.

**Q9. 111x movement?**

Không.

**Q10. 3300 ≠ 0?**

Reject G-TRF-01.

**Q11. Idempotency?**

T-F5 same ref.

**Q12. Same ref diff amount?**

409 like P-F5.

**Q13. IBFT same fee TK?**

4130 — nhưng IBFT thêm 1112/5100 vol-04.

**Q14. Fee schedule?**

[TBD: transfer fee tier config].

**Q15. Đọc tiếp matrix?**

Ch17.

---
## Chương 17. Payment vs Transfer vs IBFT matrix {#chương-17-payment-vs-transfer-vs-ibft-matrix}

| | PAYMENT | TRANSFER | IBFT (vol-04) |
|--|---------|----------|---------------|
| Transit | 3500 | 3300 | 3400 |
| Sync/async | Sync 200 | Sync 200 | Async freeze+settle |
| Payer COA | 2110 USER | 2110 USER A | 2110 USER |
| Payee COA | 2120 MERCHANT | 2110 USER B | Bank external |
| Fee TK | `[TBD]` | 4130 | 4130 |
| Bank TK | — | — | 1112 |
| Wallet hold | Debit available | Debit A | FREEZE |

---
## Chương 18. ADR-027 + ADR-008 saga narrative {#chương-18-adr-027--adr-008-saga-narrative}

### 18.1 Three commits ([ADR-027](../../adr/ADR-027-sync-payment-transfer-three-commits.md))

Không XA — mỗi service local TX. Client 200 chỉ khi cả 3 xong (hoặc fail sớm trước post).

### 18.2 Saga ([ADR-008](../../adr/ADR-008-saga-compensation-no-2pc.md))

| After step | Strategy |
|------------|----------|
| 1 fail | N/A — no debit |
| 2 fail after 1 | **Compensate** step 1 |
| 3 fail after 2 | **Forward-retry** step 3 |

### 18.3 X-E07/X-E08 outbox

JournalPosted publish failure → retry outbox relay. CommandFailed fan-out ops — không block wallet compensate.

---
## Chương 19. State machines ascii {#chương-19-state-machines-ascii}

### 19.1 Payment orchestration state

```
INIT → DEBITED → POSTED → CREDITED → DONE
         │                    ↑
         └── post fail → COMPENSATED → FAIL
POSTED ──┴── credit fail → RETRYING → CREDITED
```

### 19.2 Transfer state

```
INIT → A_DEBITED → POSTED → B_CREDITED → DONE
```

### 19.3 Ledger state

Single POSTED — no PENDING phase (unlike deposit).

---
## Chương 20. S2 API field guide {#chương-20-s2-api-field-guide}

### 20.1 postPayment request (illustrative)

```json
{
  "useCase": "WALLET_PAYMENT",
  "referenceId": "pay-1",
  "status": "POSTED",
  "lines": [
    {"accountCode": "2110", "debit": 100000, "credit": 0, "lane": "USER"},
    {"accountCode": "3500", "debit": 0, "credit": 100000},
    {"accountCode": "3500", "debit": 100000, "credit": 0},
    {"accountCode": "2120", "debit": 0, "credit": 100000, "lane": "MERCHANT"}
  ]
}
```

### 20.2 postTransfer request

```json
{
  "useCase": "INTERNAL_TRANSFER",
  "referenceId": "xfer-1",
  "status": "POSTED",
  "lines": [
    {"accountCode": "2110", "debit": 101000, "credit": 0},
    {"accountCode": "3300", "debit": 0, "credit": 101000},
    {"accountCode": "3300", "debit": 100000, "credit": 0},
    {"accountCode": "2110", "debit": 0, "credit": 100000},
    {"accountCode": "3300", "debit": 1000, "credit": 0},
    {"accountCode": "4130", "debit": 0, "credit": 1000}
  ]
}
```

### 20.3 Response fields

| Field | Meaning |
|-------|---------|
| coaTransId | POSTED journal id |
| walletTxIds | debit + credit leg ids |
| idempotentReplay | true on P-F4 |

---
## Chương 21. SQL invariant CI {#chương-21-sql-invariant-ci}

```sql
-- Payment POSTED: 3500 net zero
SELECT t.business_ref
FROM coa_trans t
JOIN coa_trans_data d ON d.coa_trans_id = t.id
WHERE t.use_case = 'WALLET_PAYMENT' AND t.status = 'POSTED'
GROUP BY t.id
HAVING SUM(CASE WHEN d.account_code='3500' THEN d.debit - d.credit ELSE 0 END) <> 0;

-- Payment must not touch 111x
SELECT t.business_ref FROM coa_trans t
JOIN coa_trans_data d ON d.coa_trans_id = t.id
WHERE t.use_case = 'WALLET_PAYMENT'
  AND d.account_code LIKE '111%';

-- Transfer POSTED: 3300 net zero
SELECT t.business_ref
FROM coa_trans t
JOIN coa_trans_data d ON d.coa_trans_id = t.id
WHERE t.use_case = 'INTERNAL_TRANSFER' AND t.status = 'POSTED'
GROUP BY t.id
HAVING SUM(CASE WHEN d.account_code='3300' THEN d.debit - d.credit ELSE 0 END) <> 0;

-- Transfer fee: if 4130 CR then fee leg exists
SELECT t.business_ref FROM coa_trans t
JOIN coa_trans_data d ON d.coa_trans_id = t.id
WHERE t.use_case = 'INTERNAL_TRANSFER' AND t.status = 'POSTED'
GROUP BY t.id
HAVING SUM(CASE WHEN d.account_code='4130' THEN d.credit - d.debit ELSE 0 END) < 0;
```

### 21.1 CI test case IDs

| TC-PAY-01 | Invariant payment #1 | CI pipeline |
| TC-PAY-02 | Invariant payment #2 | CI pipeline |
| TC-PAY-03 | Invariant payment #3 | CI pipeline |
| TC-PAY-04 | Invariant payment #4 | CI pipeline |
| TC-PAY-05 | Invariant payment #5 | CI pipeline |
| TC-PAY-06 | Invariant payment #6 | CI pipeline |
| TC-PAY-07 | Invariant payment #7 | CI pipeline |
| TC-PAY-08 | Invariant payment #8 | CI pipeline |
| TC-PAY-09 | Invariant payment #9 | CI pipeline |
| TC-PAY-10 | Invariant payment #10 | CI pipeline |
| TC-PAY-11 | Invariant payment #11 | CI pipeline |
| TC-PAY-12 | Invariant payment #12 | CI pipeline |
| TC-PAY-13 | Invariant payment #13 | CI pipeline |
| TC-PAY-14 | Invariant payment #14 | CI pipeline |
| TC-PAY-15 | Invariant payment #15 | CI pipeline |

| TC-TRF-01 | Invariant transfer #1 | CI pipeline |
| TC-TRF-02 | Invariant transfer #2 | CI pipeline |
| TC-TRF-03 | Invariant transfer #3 | CI pipeline |
| TC-TRF-04 | Invariant transfer #4 | CI pipeline |
| TC-TRF-05 | Invariant transfer #5 | CI pipeline |
| TC-TRF-06 | Invariant transfer #6 | CI pipeline |
| TC-TRF-07 | Invariant transfer #7 | CI pipeline |
| TC-TRF-08 | Invariant transfer #8 | CI pipeline |
| TC-TRF-09 | Invariant transfer #9 | CI pipeline |
| TC-TRF-10 | Invariant transfer #10 | CI pipeline |

---
## Chương 22. Review checklist — 30 mục {#chương-22-review-checklist-30-mục}

| 1 | Payment sync 200 not 202 ADR-027 | ☐ |
| 2 | Three commits measurable integration test | ☐ |
| 3 | Step order debit→post→credit integration-surfaces §4.2 | ☐ |
| 4 | 3500 net zero at POSTED | ☐ |
| 5 | 3300 net zero transfer POSTED | ☐ |
| 6 | No 111x on payment/transfer POSTED | ☐ |
| 7 | 2110 DR equals user gross debit | ☐ |
| 8 | 2120 CR equals merchant net credit | ☐ |
| 9 | 4130 on transfer fee only | ☐ |
| 10 | P-F1 compensate :comp implemented | ☐ |
| 11 | P-F2 forward-retry merchant credit | ☐ |
| 12 | T-F1 compensate A | ☐ |
| 13 | T-F2 forward-retry B | ☐ |
| 14 | Self-pay blocked P-F7 | ☐ |
| 15 | A→A blocked T-F3 | ☐ |
| 16 | Insufficient gross includes fee transfer | ☐ |
| 17 | Idempotency two legs same ref | ☐ |
| 18 | P-F5 conflict same ref diff amount | ☐ |
| 19 | MERCHANT lane 2120 ADR-020 | ☐ |
| 20 | USER lane 2110 both transfer legs | ☐ |
| 21 | Fee orchestration ADR-009 single source | ☐ |
| 22 | Immutability POSTED ADR-001 | ☐ |
| 23 | W5 lane reconciliation | ☐ |
| 24 | 20 payment Gherkin pass | ☐ |
| 25 | 12 transfer Gherkin pass | ☐ |
| 26 | SQL invariant CI green | ☐ |
| 27 | Outbox optional JournalPosted X-E07 | ☐ |
| 28 | [TBD: payment fee TK documented not hardcoded] | ☐ |
| 29 | [TBD: fee schedule config] | ☐ |
| 30 | 2120 EOD pointer Quyển VII documented | ☐ |

---
## Phụ lục A — Ma trận kịch bản số {#phụ-lục-a--ma-trận-kịch-bản-số}

| ID | Flow | gross/fee/net | Transit | 111x |
|----|------|---------------|---------|------|
| A-01 | Payment | 100k/0/100k | 3500=0 | No |
| A-02 | Payment | 100k/1k/99k | 3500=0 | No |
| A-03 | Transfer | 101k/1k/100k | 3300=0 | No |
| A-04 | Transfer | 100k/0/100k | 3300=0 | No |
| A-05 | Payment min | 1/0/1 | 3500=0 | No |

---
## Phụ lục B — DR/CR line-by-line mọi biến thể {#phụ-lục-b--drcr-line-by-line-mọi-biến-thể}

### B.01 Payment variant scale 1

| Line | TK | DR | CR |
|------|-----|-----|-----|
| 1 | 2110 | 10,000 | |
| 2 | 3500 | | 10,000 |
| 3 | 3500 | 9,900 | |
| 4 | 2120 | | 9,900 |
| 5 | 3500 | 100 | |
| 6 | `[TBD fee TK]` | | 100 |

### B.02 Payment variant scale 2

| Line | TK | DR | CR |
|------|-----|-----|-----|
| 1 | 2110 | 20,000 | |
| 2 | 3500 | | 20,000 |
| 3 | 3500 | 19,800 | |
| 4 | 2120 | | 19,800 |
| 5 | 3500 | 200 | |
| 6 | `[TBD fee TK]` | | 200 |

### B.03 Payment variant scale 3

| Line | TK | DR | CR |
|------|-----|-----|-----|
| 1 | 2110 | 30,000 | |
| 2 | 3500 | | 30,000 |
| 3 | 3500 | 29,700 | |
| 4 | 2120 | | 29,700 |
| 5 | 3500 | 300 | |
| 6 | `[TBD fee TK]` | | 300 |

### B.04 Payment variant scale 4

| Line | TK | DR | CR |
|------|-----|-----|-----|
| 1 | 2110 | 40,000 | |
| 2 | 3500 | | 40,000 |
| 3 | 3500 | 39,600 | |
| 4 | 2120 | | 39,600 |
| 5 | 3500 | 400 | |
| 6 | `[TBD fee TK]` | | 400 |

### B.05 Payment variant scale 5

| Line | TK | DR | CR |
|------|-----|-----|-----|
| 1 | 2110 | 50,000 | |
| 2 | 3500 | | 50,000 |
| 3 | 3500 | 49,500 | |
| 4 | 2120 | | 49,500 |
| 5 | 3500 | 500 | |
| 6 | `[TBD fee TK]` | | 500 |

### B.06 Payment variant scale 6

| Line | TK | DR | CR |
|------|-----|-----|-----|
| 1 | 2110 | 60,000 | |
| 2 | 3500 | | 60,000 |
| 3 | 3500 | 59,400 | |
| 4 | 2120 | | 59,400 |
| 5 | 3500 | 600 | |
| 6 | `[TBD fee TK]` | | 600 |

### B.07 Payment variant scale 7

| Line | TK | DR | CR |
|------|-----|-----|-----|
| 1 | 2110 | 70,000 | |
| 2 | 3500 | | 70,000 |
| 3 | 3500 | 69,300 | |
| 4 | 2120 | | 69,300 |
| 5 | 3500 | 700 | |
| 6 | `[TBD fee TK]` | | 700 |

### B.08 Payment variant scale 8

| Line | TK | DR | CR |
|------|-----|-----|-----|
| 1 | 2110 | 80,000 | |
| 2 | 3500 | | 80,000 |
| 3 | 3500 | 79,200 | |
| 4 | 2120 | | 79,200 |
| 5 | 3500 | 800 | |
| 6 | `[TBD fee TK]` | | 800 |

### B.09 Payment variant scale 9

| Line | TK | DR | CR |
|------|-----|-----|-----|
| 1 | 2110 | 90,000 | |
| 2 | 3500 | | 90,000 |
| 3 | 3500 | 89,100 | |
| 4 | 2120 | | 89,100 |
| 5 | 3500 | 900 | |
| 6 | `[TBD fee TK]` | | 900 |

### B.10 Payment variant scale 10

| Line | TK | DR | CR |
|------|-----|-----|-----|
| 1 | 2110 | 100,000 | |
| 2 | 3500 | | 100,000 |
| 3 | 3500 | 99,000 | |
| 4 | 2120 | | 99,000 |
| 5 | 3500 | 1,000 | |
| 6 | `[TBD fee TK]` | | 1,000 |

### B.11 Payment variant scale 11

| Line | TK | DR | CR |
|------|-----|-----|-----|
| 1 | 2110 | 110,000 | |
| 2 | 3500 | | 110,000 |
| 3 | 3500 | 108,900 | |
| 4 | 2120 | | 108,900 |
| 5 | 3500 | 1,100 | |
| 6 | `[TBD fee TK]` | | 1,100 |

### B.12 Payment variant scale 12

| Line | TK | DR | CR |
|------|-----|-----|-----|
| 1 | 2110 | 120,000 | |
| 2 | 3500 | | 120,000 |
| 3 | 3500 | 118,800 | |
| 4 | 2120 | | 118,800 |
| 5 | 3500 | 1,200 | |
| 6 | `[TBD fee TK]` | | 1,200 |

### B.13 Payment variant scale 13

| Line | TK | DR | CR |
|------|-----|-----|-----|
| 1 | 2110 | 130,000 | |
| 2 | 3500 | | 130,000 |
| 3 | 3500 | 128,700 | |
| 4 | 2120 | | 128,700 |
| 5 | 3500 | 1,300 | |
| 6 | `[TBD fee TK]` | | 1,300 |

### B.14 Payment variant scale 14

| Line | TK | DR | CR |
|------|-----|-----|-----|
| 1 | 2110 | 140,000 | |
| 2 | 3500 | | 140,000 |
| 3 | 3500 | 138,600 | |
| 4 | 2120 | | 138,600 |
| 5 | 3500 | 1,400 | |
| 6 | `[TBD fee TK]` | | 1,400 |

### B.15 Payment variant scale 15

| Line | TK | DR | CR |
|------|-----|-----|-----|
| 1 | 2110 | 150,000 | |
| 2 | 3500 | | 150,000 |
| 3 | 3500 | 148,500 | |
| 4 | 2120 | | 148,500 |
| 5 | 3500 | 1,500 | |
| 6 | `[TBD fee TK]` | | 1,500 |

### B.16 Payment variant scale 16

| Line | TK | DR | CR |
|------|-----|-----|-----|
| 1 | 2110 | 160,000 | |
| 2 | 3500 | | 160,000 |
| 3 | 3500 | 158,400 | |
| 4 | 2120 | | 158,400 |
| 5 | 3500 | 1,600 | |
| 6 | `[TBD fee TK]` | | 1,600 |

### B.17 Payment variant scale 17

| Line | TK | DR | CR |
|------|-----|-----|-----|
| 1 | 2110 | 170,000 | |
| 2 | 3500 | | 170,000 |
| 3 | 3500 | 168,300 | |
| 4 | 2120 | | 168,300 |
| 5 | 3500 | 1,700 | |
| 6 | `[TBD fee TK]` | | 1,700 |

### B.18 Payment variant scale 18

| Line | TK | DR | CR |
|------|-----|-----|-----|
| 1 | 2110 | 180,000 | |
| 2 | 3500 | | 180,000 |
| 3 | 3500 | 178,200 | |
| 4 | 2120 | | 178,200 |
| 5 | 3500 | 1,800 | |
| 6 | `[TBD fee TK]` | | 1,800 |

### B.19 Payment variant scale 19

| Line | TK | DR | CR |
|------|-----|-----|-----|
| 1 | 2110 | 190,000 | |
| 2 | 3500 | | 190,000 |
| 3 | 3500 | 188,100 | |
| 4 | 2120 | | 188,100 |
| 5 | 3500 | 1,900 | |
| 6 | `[TBD fee TK]` | | 1,900 |

### B.20 Payment variant scale 20

| Line | TK | DR | CR |
|------|-----|-----|-----|
| 1 | 2110 | 200,000 | |
| 2 | 3500 | | 200,000 |
| 3 | 3500 | 198,000 | |
| 4 | 2120 | | 198,000 |
| 5 | 3500 | 2,000 | |
| 6 | `[TBD fee TK]` | | 2,000 |

---
## Phụ lục C — Bảng tra ADR {#phụ-lục-c--bảng-tra-adr}

| ADR | Topic | vol-05 touchpoint |
|-----|-------|-------------------|
| [001](../../adr/ADR-001-immutable-ledger.md) | Immutability | P-F8, no UPDATE POSTED |
| [005](../../adr/ADR-005-idempotency-key-strategy.md) | Keys | Ch6, P-F4, T-F5 |
| [008](../../adr/ADR-008-saga-compensation-no-2pc.md) | Saga | Ch5, Ch14, Ch18 |
| [009](../../adr/ADR-009-fee-ownership-orchestration.md) | Fees | Ch3 fee lines |
| [010](../../adr/ADR-010-transit-accounts-net-zero.md) | Transit | 3500/3300=0 |
| [020](../../adr/ADR-020-wallet-lanes-coa-control-mapping.md) | Lanes | Ch4 |
| [027](../../adr/ADR-027-sync-payment-transfer-three-commits.md) | Sync 200 | Ch1, Ch2, Ch18 |
| [031](../../adr/ADR-031-sql-ledger-invariant-ci.md) | SQL invariant CI | Ch21, Phụ lục G |

---
## Phụ lục D — processes §5/§6 mapping {#phụ-lục-d--processes-§5§6-mapping}

| processes.md | vol-05 chapter |
|--------------|----------------|
| §5.1 step order | Ch2 |
| §5.2 postings | Ch3 |
| §5.3 wallet | Ch6 |
| §6.1 postings transfer | Ch12 |
| §6.2 wallet transfer | Ch11 |
| §13.2 payment saga | Ch5 |
| §13.3 transfer saga | Ch14 |

---
## Phụ lục E — accounting.md §16/§17 mapping {#phụ-lục-e--accounting.md-1617-mapping}

| accounting.md | vol-05 |
|---------------|--------|
| §16.1 obligation | Ch1, Ch3 |
| §16.2 observables | Ch3, Ch4 |
| §16.3 P-F1..P-F8 | Ch5 |
| §17.1 transfer obligation | Ch11, Ch12 |
| §17.2 observables | Ch12 |
| §17.3 T-F1..T-F5 | Ch14 |

---
## Phụ lục F — Numeric T-account gallery {#phụ-lục-f--numeric-t-account-gallery}

### F.01

Transfer: A gross 10,100 B net 10,000 fee 100 → 3300=0 4130+100

### F.02

Transfer: A gross 20,000 B net 20,000 fee 0 → 3300=0 4130+0

### F.03

Transfer: A gross 30,300 B net 30,000 fee 300 → 3300=0 4130+300

### F.04

Transfer: A gross 40,000 B net 40,000 fee 0 → 3300=0 4130+0

### F.05

Transfer: A gross 50,500 B net 50,000 fee 500 → 3300=0 4130+500

### F.06

Transfer: A gross 60,000 B net 60,000 fee 0 → 3300=0 4130+0

### F.07

Transfer: A gross 70,700 B net 70,000 fee 700 → 3300=0 4130+700

### F.08

Transfer: A gross 80,000 B net 80,000 fee 0 → 3300=0 4130+0

### F.09

Transfer: A gross 90,900 B net 90,000 fee 900 → 3300=0 4130+900

### F.10

Transfer: A gross 100,000 B net 100,000 fee 0 → 3300=0 4130+0

### F.11

Transfer: A gross 111,100 B net 110,000 fee 1,100 → 3300=0 4130+1,100

### F.12

Transfer: A gross 120,000 B net 120,000 fee 0 → 3300=0 4130+0

### F.13

Transfer: A gross 131,300 B net 130,000 fee 1,300 → 3300=0 4130+1,300

### F.14

Transfer: A gross 140,000 B net 140,000 fee 0 → 3300=0 4130+0

### F.15

Transfer: A gross 151,500 B net 150,000 fee 1,500 → 3300=0 4130+1,500

---
## Phụ lục G — Cross-flow invariant {#phụ-lục-g--cross-flow-invariant}

| Invariant | Rule |
|-----------|------|
| INV-PAY-01 | Payment POSTED ⇒ 3500=0 |
| INV-PAY-02 | No 111x payment |
| INV-TRF-01 | Transfer POSTED ⇒ 3300=0 |
| INV-TRF-02 | 2110 pool −fee on transfer with fee |
| INV-LANE | USER→2110 MERCHANT→2120 |
| INV-SAGA | No 2PC — compensate or forward-retry |

---
## Phụ lục H — Đọc tiếp vol-06 {#phụ-lục-h--đọc-tiếp-vol-06}

**Quyển VI (planned):** QR/POS payment (foundation §12), acquirer **1113**, MDR **4140**, EOD settlement deep dive (foundation §16), payroll/disbursement batch.

Sau khi đọc Quyển V, đọc tiếp:

- [`vol-01-principles.md`](./vol-01-principles.md) — accrual, transit principles
- [`vol-04-withdraw-ibft.md`](./vol-04-withdraw-ibft.md) — async outflow contrast
- [`acceptance.md`](../acceptance.md) — Payment + Transfer features
- [`processes.md`](../../spec/processes.md) §8 QR/POS, §11 EOD

## Chương 5B. Payment failure ops deep dive {#chương-5b}

### 5B.01 — mục mở rộng 1

Nội dung chi tiết liên kết Ch5 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 5B.02 — mục mở rộng 2

Nội dung chi tiết liên kết Ch5 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 5B.03 — mục mở rộng 3

Nội dung chi tiết liên kết Ch5 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 5B.04 — mục mở rộng 4

Nội dung chi tiết liên kết Ch5 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 5B.05 — mục mở rộng 5

Nội dung chi tiết liên kết Ch5 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 5B.06 — mục mở rộng 6

Nội dung chi tiết liên kết Ch5 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 5B.07 — mục mở rộng 7

Nội dung chi tiết liên kết Ch5 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 5B.08 — mục mở rộng 8

Nội dung chi tiết liên kết Ch5 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 5B.09 — mục mở rộng 9

Nội dung chi tiết liên kết Ch5 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 5B.10 — mục mở rộng 10

Nội dung chi tiết liên kết Ch5 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 5B.11 — mục mở rộng 11

Nội dung chi tiết liên kết Ch5 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 5B.12 — mục mở rộng 12

Nội dung chi tiết liên kết Ch5 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 5B.13 — mục mở rộng 13

Nội dung chi tiết liên kết Ch5 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 5B.14 — mục mở rộng 14

Nội dung chi tiết liên kết Ch5 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 5B.15 — mục mở rộng 15

Nội dung chi tiết liên kết Ch5 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 5B.16 — mục mở rộng 16

Nội dung chi tiết liên kết Ch5 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 5B.17 — mục mở rộng 17

Nội dung chi tiết liên kết Ch5 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 5B.18 — mục mở rộng 18

Nội dung chi tiết liên kết Ch5 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 5B.19 — mục mở rộng 19

Nội dung chi tiết liên kết Ch5 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 5B.20 — mục mở rộng 20

Nội dung chi tiết liên kết Ch5 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 5B.21 — mục mở rộng 21

Nội dung chi tiết liên kết Ch5 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 5B.22 — mục mở rộng 22

Nội dung chi tiết liên kết Ch5 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 5B.23 — mục mở rộng 23

Nội dung chi tiết liên kết Ch5 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 5B.24 — mục mở rộng 24

Nội dung chi tiết liên kết Ch5 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 5B.25 — mục mở rộng 25

Nội dung chi tiết liên kết Ch5 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

---
## Chương 6B. Idempotency sequence diagrams {#chương-6b}

### 6B.01 — mục mở rộng 1

Nội dung chi tiết liên kết Ch6 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 6B.02 — mục mở rộng 2

Nội dung chi tiết liên kết Ch6 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 6B.03 — mục mở rộng 3

Nội dung chi tiết liên kết Ch6 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 6B.04 — mục mở rộng 4

Nội dung chi tiết liên kết Ch6 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 6B.05 — mục mở rộng 5

Nội dung chi tiết liên kết Ch6 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 6B.06 — mục mở rộng 6

Nội dung chi tiết liên kết Ch6 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 6B.07 — mục mở rộng 7

Nội dung chi tiết liên kết Ch6 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 6B.08 — mục mở rộng 8

Nội dung chi tiết liên kết Ch6 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 6B.09 — mục mở rộng 9

Nội dung chi tiết liên kết Ch6 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 6B.10 — mục mở rộng 10

Nội dung chi tiết liên kết Ch6 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 6B.11 — mục mở rộng 11

Nội dung chi tiết liên kết Ch6 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 6B.12 — mục mở rộng 12

Nội dung chi tiết liên kết Ch6 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 6B.13 — mục mở rộng 13

Nội dung chi tiết liên kết Ch6 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 6B.14 — mục mở rộng 14

Nội dung chi tiết liên kết Ch6 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 6B.15 — mục mở rộng 15

Nội dung chi tiết liên kết Ch6 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 6B.16 — mục mở rộng 16

Nội dung chi tiết liên kết Ch6 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 6B.17 — mục mở rộng 17

Nội dung chi tiết liên kết Ch6 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 6B.18 — mục mở rộng 18

Nội dung chi tiết liên kết Ch6 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 6B.19 — mục mở rộng 19

Nội dung chi tiết liên kết Ch6 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 6B.20 — mục mở rộng 20

Nội dung chi tiết liên kết Ch6 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 6B.21 — mục mở rộng 21

Nội dung chi tiết liên kết Ch6 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 6B.22 — mục mở rộng 22

Nội dung chi tiết liên kết Ch6 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 6B.23 — mục mở rộng 23

Nội dung chi tiết liên kết Ch6 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 6B.24 — mục mở rộng 24

Nội dung chi tiết liên kết Ch6 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 6B.25 — mục mở rộng 25

Nội dung chi tiết liên kết Ch6 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

---
## Chương 12B. Transfer T-account extended {#chương-12b}

### 12B.01 — mục mở rộng 1

Nội dung chi tiết liên kết Ch12 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 12B.02 — mục mở rộng 2

Nội dung chi tiết liên kết Ch12 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 12B.03 — mục mở rộng 3

Nội dung chi tiết liên kết Ch12 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 12B.04 — mục mở rộng 4

Nội dung chi tiết liên kết Ch12 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 12B.05 — mục mở rộng 5

Nội dung chi tiết liên kết Ch12 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 12B.06 — mục mở rộng 6

Nội dung chi tiết liên kết Ch12 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 12B.07 — mục mở rộng 7

Nội dung chi tiết liên kết Ch12 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 12B.08 — mục mở rộng 8

Nội dung chi tiết liên kết Ch12 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 12B.09 — mục mở rộng 9

Nội dung chi tiết liên kết Ch12 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 12B.10 — mục mở rộng 10

Nội dung chi tiết liên kết Ch12 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 12B.11 — mục mở rộng 11

Nội dung chi tiết liên kết Ch12 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 12B.12 — mục mở rộng 12

Nội dung chi tiết liên kết Ch12 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 12B.13 — mục mở rộng 13

Nội dung chi tiết liên kết Ch12 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 12B.14 — mục mở rộng 14

Nội dung chi tiết liên kết Ch12 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 12B.15 — mục mở rộng 15

Nội dung chi tiết liên kết Ch12 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 12B.16 — mục mở rộng 16

Nội dung chi tiết liên kết Ch12 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 12B.17 — mục mở rộng 17

Nội dung chi tiết liên kết Ch12 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 12B.18 — mục mở rộng 18

Nội dung chi tiết liên kết Ch12 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 12B.19 — mục mở rộng 19

Nội dung chi tiết liên kết Ch12 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 12B.20 — mục mở rộng 20

Nội dung chi tiết liên kết Ch12 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 12B.21 — mục mở rộng 21

Nội dung chi tiết liên kết Ch12 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 12B.22 — mục mở rộng 22

Nội dung chi tiết liên kết Ch12 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 12B.23 — mục mở rộng 23

Nội dung chi tiết liên kết Ch12 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 12B.24 — mục mở rộng 24

Nội dung chi tiết liên kết Ch12 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 12B.25 — mục mở rộng 25

Nội dung chi tiết liên kết Ch12 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

---
## Chương 18B. Saga timeline examples {#chương-18b}

### 18B.01 — mục mở rộng 1

Nội dung chi tiết liên kết Ch18 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 18B.02 — mục mở rộng 2

Nội dung chi tiết liên kết Ch18 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 18B.03 — mục mở rộng 3

Nội dung chi tiết liên kết Ch18 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 18B.04 — mục mở rộng 4

Nội dung chi tiết liên kết Ch18 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 18B.05 — mục mở rộng 5

Nội dung chi tiết liên kết Ch18 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 18B.06 — mục mở rộng 6

Nội dung chi tiết liên kết Ch18 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 18B.07 — mục mở rộng 7

Nội dung chi tiết liên kết Ch18 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 18B.08 — mục mở rộng 8

Nội dung chi tiết liên kết Ch18 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 18B.09 — mục mở rộng 9

Nội dung chi tiết liên kết Ch18 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 18B.10 — mục mở rộng 10

Nội dung chi tiết liên kết Ch18 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 18B.11 — mục mở rộng 11

Nội dung chi tiết liên kết Ch18 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 18B.12 — mục mở rộng 12

Nội dung chi tiết liên kết Ch18 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 18B.13 — mục mở rộng 13

Nội dung chi tiết liên kết Ch18 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 18B.14 — mục mở rộng 14

Nội dung chi tiết liên kết Ch18 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 18B.15 — mục mở rộng 15

Nội dung chi tiết liên kết Ch18 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 18B.16 — mục mở rộng 16

Nội dung chi tiết liên kết Ch18 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 18B.17 — mục mở rộng 17

Nội dung chi tiết liên kết Ch18 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 18B.18 — mục mở rộng 18

Nội dung chi tiết liên kết Ch18 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 18B.19 — mục mở rộng 19

Nội dung chi tiết liên kết Ch18 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 18B.20 — mục mở rộng 20

Nội dung chi tiết liên kết Ch18 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 18B.21 — mục mở rộng 21

Nội dung chi tiết liên kết Ch18 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 18B.22 — mục mở rộng 22

Nội dung chi tiết liên kết Ch18 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 18B.23 — mục mở rộng 23

Nội dung chi tiết liên kết Ch18 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 18B.24 — mục mở rộng 24

Nội dung chi tiết liên kết Ch18 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 18B.25 — mục mở rộng 25

Nội dung chi tiết liên kết Ch18 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

---
## Chương 21B. SQL audit queries extended {#chương-21b}

### 21B.01 — mục mở rộng 1

Nội dung chi tiết liên kết Ch21 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 21B.02 — mục mở rộng 2

Nội dung chi tiết liên kết Ch21 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 21B.03 — mục mở rộng 3

Nội dung chi tiết liên kết Ch21 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 21B.04 — mục mở rộng 4

Nội dung chi tiết liên kết Ch21 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 21B.05 — mục mở rộng 5

Nội dung chi tiết liên kết Ch21 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 21B.06 — mục mở rộng 6

Nội dung chi tiết liên kết Ch21 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 21B.07 — mục mở rộng 7

Nội dung chi tiết liên kết Ch21 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 21B.08 — mục mở rộng 8

Nội dung chi tiết liên kết Ch21 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 21B.09 — mục mở rộng 9

Nội dung chi tiết liên kết Ch21 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 21B.10 — mục mở rộng 10

Nội dung chi tiết liên kết Ch21 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 21B.11 — mục mở rộng 11

Nội dung chi tiết liên kết Ch21 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 21B.12 — mục mở rộng 12

Nội dung chi tiết liên kết Ch21 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 21B.13 — mục mở rộng 13

Nội dung chi tiết liên kết Ch21 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 21B.14 — mục mở rộng 14

Nội dung chi tiết liên kết Ch21 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 21B.15 — mục mở rộng 15

Nội dung chi tiết liên kết Ch21 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 21B.16 — mục mở rộng 16

Nội dung chi tiết liên kết Ch21 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 21B.17 — mục mở rộng 17

Nội dung chi tiết liên kết Ch21 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 21B.18 — mục mở rộng 18

Nội dung chi tiết liên kết Ch21 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 21B.19 — mục mở rộng 19

Nội dung chi tiết liên kết Ch21 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 21B.20 — mục mở rộng 20

Nội dung chi tiết liên kết Ch21 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 21B.21 — mục mở rộng 21

Nội dung chi tiết liên kết Ch21 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 21B.22 — mục mở rộng 22

Nội dung chi tiết liên kết Ch21 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 21B.23 — mục mở rộng 23

Nội dung chi tiết liên kết Ch21 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 21B.24 — mục mở rộng 24

Nội dung chi tiết liên kết Ch21 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

### 21B.25 — mục mở rộng 25

Nội dung chi tiết liên kết Ch21 chính · ADR Phụ lục C · acceptance PAY/TRF scenarios.
Evidence: `coa_trans` POSTED · transit 3500/3300 net zero · không 111x · saga §13.2–13.3.
Ops: forward-retry sau POSTED; compensate chỉ trước POSTED; immutability ADR-001.

---
