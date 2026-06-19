# core.business-processes — Business processes (end-to-end)

**Last updated:** 2026-06-08  
**Status:** Draft  
**Scope:** `10_core/` — end-to-end business processes for the whole core (deposit, withdraw, payment, transfer, payroll, disbursement, EOD settlement).

> **What this doc is:** A **business / design** view — actors, preconditions, end-to-end flow, DR/CR postings, wallet effects, and the **non-happy paths**: failure & compensation (saga, §13), edge cases (§14), reliability patterns (§15), fee policy (§16), auth (§17). Stated in domain terms so it survives a language/runtime change. Not a replacement for schema / wire specs.  
> **Sources (read directly):** postings & COA → [`foundation.md`](./foundation.md) §6–16; wallet branch → [`trd/wallet.md`](./trd/wallet.md) §5; step order & surfaces → [`integration-surfaces.md`](./integration-surfaces.md) §4–7; accounting FR → [`trd/accounting.md`](./trd/accounting.md); terminology → [`terminology.md`](./terminology.md).  
> **ADR (38):** full index [`../adr/README.md`](../adr/README.md) — key: [001](../adr/ADR-001-immutable-ledger.md) ledger · [006](../adr/ADR-006-two-phase-deposit.md) deposit · [007](../adr/ADR-007-freeze-settle-async-outflow.md) freeze · [008](../adr/ADR-008-saga-compensation-no-2pc.md) saga · [009](../adr/ADR-009-fee-ownership-orchestration.md) fees · [011](../adr/ADR-011-auth-identity-jwt-subject.md) auth · [013](../adr/ADR-013-outbox-at-least-once-messaging.md) outbox · [014](../adr/ADR-014-reconciliation-w5-report-only.md) W5 · [015](../adr/ADR-015-eod-settlement-independent-batch.md) EOD · [017](../adr/ADR-017-partial-batch-payroll-disbursement.md) batch · [021](../adr/ADR-021-aging-jobs-async-pending.md) aging · [022](../adr/ADR-022-mtls-bank-webhooks.md) mTLS · [027](../adr/ADR-027-sync-payment-transfer-three-commits.md) sync pay.

---

## 1. Common concepts

### 1.1 Two sources of truth

| Domain | Owns | Answers |
|--------|------|---------|
| `core.accounting` (`coa_*`) | Double-entry ledger, COA **2110/2120/2130**, transit accounts, DR/CR postings | Aggregate liabilities and accounting fund flow |
| `core.wallet` (`wallet_*`) | Per-member balance: `available` / `frozen` | "How much can this member spend right now?" |

The two never share tables / JOIN; they stay aligned through **orchestration** (Application) via commands/events and reconciliation jobs ([`foundation.md`](./foundation.md) Part I §3, [`trd/wallet.md`](./trd/wallet.md) §1.1).

### 1.2 Actors

| Actor | Role |
|-------|------|
| Member (User / Merchant / Partner) | Wallet owner, initiates transactions |
| Bank / Napas | External party that moves real funds, sends webhooks |
| API Gateway (S4) | Edge proxy → routes to BFF/orchestration only |
| Orchestration / BFF | Sequences steps, calls accounting + wallet |
| `core.accounting` | Writes journal `coa_trans` + lines `coa_trans_data` |
| `core.wallet` | Updates `wallet_balance` + `wallet_tx` |
| Worker (RabbitMQ S6) | Handles async commands: bank deposit, wallet credit, payout |

### 1.3 Foundation invariants ([`foundation.md`](./foundation.md) §5)

1. `(1111 + 1112 + 1113)` = `(2110 + 2120 + 2130)` — bank assets = total wallet liabilities.
2. Actual bank balance = sum of wallet balances.
3. Every transit account (3100–3820) returns to **0** when the use case completes.
4. Each journal `coa_trans`: `sum(DR) = sum(CR)`; after POSTED no edit, only reversing entries ([ADR-001](./adr/ADR-001-immutable-ledger.md)).

### 1.4 Idempotency

`businessRef` is the idempotency key end-to-end: S1 header `X-Idempotency-Key` = S2 `reference_id` = S6 envelope `businessRef` = `wallet_tx.business_ref` = `coa_trans.reference_id` ([`integration-surfaces.md`](./integration-surfaces.md) §8). A duplicate `businessRef` with the same semantics → return the prior result, no second effect.

### 1.5 COA map used in this doc ([`foundation.md`](./foundation.md) §6)

| Group | Code | Meaning |
|-------|------|---------|
| Asset | 1111 / 1112 / 1113 | Vietinbank · Napas Clearing · VPBank (QR/POS) |
| Liability | 2110 / 2120 / 2130 | User wallet · Merchant wallet · Partner escrow |
| Transit | 3100 / 3200 / 3300 / 3400 / 3500 / 3600 / 3700 / 3800–3820 | Deposit · Withdraw · Internal transfer · IBFT · Payment · Payroll · Disbursement · EOD settlement |
| Revenue | 4110 / 4120 / 4130 / 4140 / 4150 | Deposit fee · withdraw fee · transfer fee · MDR · payroll/disbursement fee |
| Expense | 5100 | Bank / Napas fee expense |

---

## 2. Process map

| # | Process | Sync? | HTTP (S1) | Transit | Source docs |
|---|---------|-------|-----------|---------|-------------|
| §3 | Deposit | Async → **202** | `notifyDeposit` / `getDepositStatus` | 3100 | foundation §8, wallet §5.1 |
| §4 | Withdraw | Wallet sync + bank async → **200** | `createWithdrawal` | 3200 | foundation §9, wallet §5.4 |
| §5 | Wallet payment | Sync → **200** | `createPayment` | 3500 | foundation §13, wallet §5.2 |
| §6 | Internal transfer | Sync → **200** | `createTransfer` | 3300 | foundation §10, wallet §5.3 |
| §7 | IBFT (interbank) | Wallet sync + bank async | `createTransfer` (external dest) | 3400 | foundation §11 |
| §8 | QR/POS payment | Acquirer + EOD | — (acquirer) | 3500 → 3800 | foundation §12, §16 |
| §9 | Payroll | Async (batch) | — (merchant channel) | 3600 | foundation §14 |
| §10 | Disbursement | Async (batch) | — (partner channel) | 3700 | foundation §15 |
| §11 | EOD settlement & clearing | Batch | — | 3800/3810/3820 | foundation §16 |
| §11A | Pocket ops (create/close/pocket→pocket) — USER | Sync | pocket APIs | 3300 (transfer only) | ADR-040, wallet §2 |

Use case × surface matrix and required step order: [`integration-surfaces.md`](./integration-surfaces.md) §4.

**Đọc tiếp theo flow:** §3 Deposit → §4 Withdraw → §5 Payment → §6 Transfer → §7 IBFT → §8 QR/POS → §9 Payroll → §10 Disbursement → §11 EOD → §13 Saga.

---

## 3. Deposit

**Goal:** A member transfers funds into a virtual account (VA); the system records it and credits the net amount to the wallet. **Two-phase**, tied to transit **3100**.

**Actors:** Member → Bank (Vietinbank) → Orchestration → Accounting worker → Wallet.  
**Canonical example:** principal 100,000 + deposit fee 1,000 → wallet receives net **99,000** ([`foundation.md`](./foundation.md) §8).

### 3.1 End-to-end flow ([`integration-surfaces.md`](./integration-surfaces.md) §4.1)

```
Member   Bank        Gateway/BFF       Worker(S6)     core.accounting   core.wallet
  |       |              |                 |                |               |
  |--xfer>|              |                 |                |               |
  |       |--webhook---->|  (S1 202 + businessRef)          |               |
  |       |              |--BANK_DEPOSIT-->|                |               |
  |       |              |                 |--post PENDING->| 1111 DR 100k  |
  |       |              |                 |                | 3100 CR 100k  |
  |       |              | map VA→userId   |                |               |
  |       |              |                 |--post POSTED-->| 3100=0,2110,4110
  |       |              |                 |--JournalPosted / WalletCreditCommand-->| credit +99k
```

### 3.2 Postings ([`foundation.md`](./foundation.md) §8.1)

| Step | Account | DR/CR | Amount | Phase |
|------|---------|-------|--------|-------|
| 1 | 1111 | DR | 100,000 | A (PENDING) |
| 2 | 3100 | CR | 100,000 | A |
| 3 | 3100 | DR | 100,000 | B (POSTED) |
| 4 | 2110 | CR | 99,000 | B |
| 5 | 2110 | DR | 1,000 | B |
| 6 | 4110 | CR | 1,000 | B |

**Result:** `1111 +100,000` · `2110 +99,000` · `4110 +1,000` · **3100 = 0**.

### 3.3 Wallet effect ([`trd/wallet.md`](./trd/wallet.md) §5.1)

- Phase A (PENDING): wallet **unchanged** — funds still in transit 3100.
- Phase B (POSTED): accounting worker (`app-accounting-worker`) publishes `WALLET_CREDIT` to RabbitMQ exchange `core.commands` (routing key `core.commands.wallet-credit`). `app-wallet-worker` consumes and calls `creditByWalletId(walletId, businessRef, 99000, currency, coaTransId)` → `wallet_tx` with `tx_type = DEPOSIT_CREDIT` (direction `CREDIT`). The wallet **does not** compute fees; it credits exactly `netAmount = gross − fee` received in the command.

> **Rule (ADR-041, ADR-038) — async bank deposit only:** Orchestration does **not** call `app-wallet` via HTTP for this flow. The wallet credit is triggered by a RabbitMQ command from `app-accounting-worker`, not a synchronous HTTP call. Other use cases (e.g., instant sync top-up) may use the S2 HTTP surface directly.

### 3.4 Rules & error handling ([`foundation.md`](./foundation.md) §8.5, [`trd/wallet.md`](./trd/wallet.md) §8)

| Situation | Handling |
|-----------|----------|
| Duplicate webhook, same `bankRef` | Return existing `coa_trans_id`, no insert (`business_ref` UNIQUE) |
| Confirm when already `POSTED` | No-op |
| Cannot map VA → user | No `BANK_DEPOSIT` published; orchestration logs ops hold; 202 returned; no journal, no 3100 posting; re-submit after VA mapping added |
| Wallet credit fails after POSTED | Retry consumer; **do not** edit `coa_trans_data` — reconciliation job |
| Bank reports wrong amount | Do not confirm; reverse phase A with a reversing journal |

---

## 4. Withdraw

**Goal:** A member withdraws funds to a bank. **Wallet branch is synchronous** (hold funds) before returning **200**; **bank branch is asynchronous** (payout).  
**Example:** withdraw principal 100,000 + fee 1,000 → wallet debited **101,000** ([`foundation.md`](./foundation.md) §9).

### 4.1 End-to-end flow

```
Member    Gateway/BFF      core.wallet        Payout worker(S6)     Bank
  |            |                |                    |                |
  |--createWithdrawal-->|       |                    |                |
  |            |--freeze 101k-->| (available→frozen) |                |
  |<-- 200 (accepted) --|       |                    |                |
  |            |--WITHDRAW_PAYOUT->|                  |--transfer 100k->|
  |            |                |    success → settle (debit frozen)  |
  |            |                |    fail    → release (unfreeze)      |
```

### 4.2 Postings ([`foundation.md`](./foundation.md) §9)

| Step | Account | DR/CR | Amount |
|------|---------|-------|--------|
| 1 | 2110 | DR | 101,000 |
| 2 | 3200 | CR | 101,000 |
| 3 | 3200 | DR | 100,000 |
| 4 | 1111 | CR | 100,000 |
| 5 | 3200 | DR | 1,000 |
| 6 | 4120 | CR | 1,000 |

**Result:** `2110 -101,000` · `1111 -100,000` · `4120 +1,000` · **3200 = 0**.

### 4.3 Wallet effect ([`trd/wallet.md`](./trd/wallet.md) §5.4)

| `tx_type` | Direction | When |
|-----------|-----------|------|
| `WITHDRAW_FREEZE` | FREEZE | On accept — hold principal + fee (v1: hold on accept, **not** an immediate debit) |
| `WITHDRAW_SETTLE` | DEBIT | Bank payout success — deduct from `frozen` |
| `WITHDRAW_RELEASE` | UNFREEZE | Payout failed / cancelled |

> There is no `WITHDRAW_DEBIT`; the wallet never debits twice ([`implementation.md`](./implementation.md) §2.1).

---

## 5. Wallet payment

**Goal:** A user pays a merchant from the wallet, **synchronously** in one request → **200**.  
**Example:** 100,000 ([`foundation.md`](./foundation.md) §13).

### 5.1 Required step order ([`integration-surfaces.md`](./integration-surfaces.md) §4.2)

```
1. Wallet : debit USER (available -= amount)
2. S2     : journal POSTED (transit 3500 = 0)
3. Wallet : credit MERCHANT
4. S1     : 200 + walletTxId / coaTransId
```

### 5.2 Postings ([`foundation.md`](./foundation.md) §13)

| Step | Account | DR/CR | Amount |
|------|---------|-------|--------|
| 1 | 2110 (User) | DR | 100,000 |
| 2 | 3500 | CR | 100,000 |
| 3 | 3500 | DR | 100,000 |
| 4 | 2120 (Merchant) | CR | 100,000 |

**Result:** **3500 = 0** · `2120` pending settlement (see §11).

### 5.3 Wallet effect ([`trd/wallet.md`](./trd/wallet.md) §5.2)

| `tx_type` | Lane | Direction |
|-----------|------|-----------|
| `PAYMENT_DEBIT` | USER | DEBIT |
| `PAYMENT_CREDIT` | MERCHANT | CREDIT |

Same `businessRef`, one distinct `tx_type` per leg. Insufficient funds → reject, no partial debit ([`trd/wallet.md`](./trd/wallet.md) FR-4).

---

## 6. Internal transfer

**Goal:** Move funds wallet A → wallet B within the system, **synchronously**.  
**Example:** 100,000 + fee 1,000 ([`foundation.md`](./foundation.md) §10).

### 6.1 Postings

| Step | Actor | Account | DR/CR | Amount |
|------|-------|---------|-------|--------|
| 1 | User A | 2110 | DR | 101,000 |
| 2 | User A | 3300 | CR | 101,000 |
| 3 | User B | 3300 | DR | 100,000 |
| 4 | User B | 2110 | CR | 100,000 |
| 5 | User A | 3300 | DR | 1,000 |
| 6 | User A | 4130 | CR | 1,000 |

**Result:** **3300 = 0** · no bank movement.

### 6.2 Wallet effect ([`trd/wallet.md`](./trd/wallet.md) §5.3)

Debit A (USER) `tx_type = TRANSFER_DEBIT` for the **gross** (principal + fee, e.g. 101,000) → ledger POSTED (transit 3300, fee to 4130) → credit B (USER) `tx_type = TRANSFER_CREDIT` for the **net** (e.g. 100,000). The wallet models member legs only, **not** transit; the fee stays as accounting revenue (no separate wallet leg).

---

## 7. IBFT (interbank transfer)

**Goal:** A member transfers out to another bank via Napas. The wallet branch debits funds; the bank branch flows through 1112 (Napas Clearing).  
**Example:** principal 100,000 + fee 1,000, Napas cost 500 ([`foundation.md`](./foundation.md) §11).

### 7.1 Postings

| Step | Actor | Account | DR/CR | Amount |
|------|-------|---------|-------|--------|
| 1 | User | 2110 | DR | 101,000 |
| 2 | User | 3400 | CR | 101,000 |
| 3 | User | 3400 | DR | 1,000 |
| 4 | User | 4130 | CR | 1,000 |
| 5 | Bank | 3400 | DR | 100,000 |
| 6 | Bank | 1112 | CR | 100,000 |
| 8 | Bank | 5100 | DR | 500 |
| 9 | Bank | 1112 | CR | 500 |

**Result:** **3400 = 0** · net profit +500 (fee 1,000 − Napas cost 500).

---

## 8. QR/POS payment

**Goal:** A customer pays via QR/POS; funds land in the acquirer account **1113 (VPBank)**, crediting the merchant wallet **2120** (pending EOD settlement).  
**Example:** 100,000, acquiring fee 500 ([`foundation.md`](./foundation.md) §12).

### 8.1 Postings

| Step | Actor | Account | DR/CR | Amount |
|------|-------|---------|-------|--------|
| 1 | Bank | 1113 | DR | 100,000 |
| 2 | Bank | 3500 | CR | 100,000 |
| 3 | Bank | 5100 | DR | 500 |
| 4 | Bank | 1113 | CR | 500 |
| 5 | Merchant | 3500 | DR | 100,000 |
| 6 | Merchant | 2120 | CR | 100,000 |

**Result:** **3500 = 0** · `2120` pending settlement → handled in §11 (EOD).

> Wallet: no per-transaction `wallet_tx` for acquirer flow until the product decides to credit the merchant wallet ([`trd/wallet.md`](./trd/wallet.md) §5.6).

---

## 9. Payroll

**Goal:** A merchant pays salaries to employees in bulk, out to banks.  
**Example:** 5 employees × 100,000 + fee 5,000, Napas cost 2,500 ([`foundation.md`](./foundation.md) §14).

### 9.1 Postings

| Step | Actor | Account | DR/CR | Amount |
|------|-------|---------|-------|--------|
| 1 | Merchant | 2120 | DR | 505,000 |
| 2 | Merchant | 3600 | CR | 505,000 |
| 3 | Merchant | 3600 | DR | 5,000 |
| 4 | Merchant | 4150 | CR | 5,000 |
| 5 | Bank | 3600 | DR | 500,000 |
| 6 | Bank | 1112 | CR | 500,000 |
| 8 | Bank | 5100 | DR | 2,500 |
| 9 | Bank | 1112 | CR | 2,500 |

**Result:** **3600 = 0** · net profit +2,500.

---

## 10. Disbursement

**Goal:** A partner pre-funds an escrow (**2130**) then disburses out to recipient banks.  
([`foundation.md`](./foundation.md) §15)

### 10.1 Postings

- **Pre-fund:** `1111 DR 100,000` → `2130 CR 100,000`.
- **Disburse:** `2130 DR 101,000` → `3700 CR` → `1112 CR 100,000` + fees `4150` / cost `5100`.

**Result:** **3700 = 0**.

### 10.2 Wallet effect ([`trd/wallet.md`](./trd/wallet.md) §5.5)

`PARTNER` lane: credit/debit the partner wallet when orchestration issues commands — same FR rules as other lanes.

---

## 11. EOD settlement & clearing

**Goal:** At end of day, aggregate pending merchant balances (**2120**), split MDR, and settle the net to merchant banks. Runs as an **independent batch**, not inline with payment ([`foundation.md`](./foundation.md) §4, §16).

### 11.1 Flow

```
2120 → 3800 (lock) → 3820 (MDR) + 3810 (net) → 1112 → Merchant bank
```

### 11.2 Postings (example 200,000, MDR 2,000)

| Step | Account | DR/CR | Amount |
|------|---------|-------|--------|
| Lock merchant | 2120 | DR | 200,000 |
| Hold clearing | 3800 | CR | 200,000 |
| Split MDR | 3800 DR 2,000 → 3820 CR 2,000 → 4140 CR 2,000 | | |
| Settlement | 3810 DR 198,000 → 1112 CR 198,500 (incl. Napas fee 5100) | | |

**Result:** all transit = 0 · `2120` = 0 after settlement.

---

## 11A. Pocket operations — create / close / pocket-to-pocket (USER)

**ADR:** [ADR-040 — USER multi-pocket wallets](./adr/ADR-040-user-multi-pocket-wallets.md). **Scope:** USER only; MERCHANT/PARTNER hold a single wallet. A pocket ("ngăn ví") **is** a `wallet` row instantiated from `wallet_pocket_def` ([`design/platform/data-model.md`](../design/platform/data-model.md) §3.1).

### 11A.1 Create pocket

**Goal:** A USER creates a new pocket from a `pocket_code`, **synchronously** → **201**.  
**Actors:** Member → Gateway/BFF → Wallet.

- **Accounting:** **none** — an empty pocket moves no money; no journal, no transit.
- **Wallet:** insert one `wallet` row `(member_id, USER, currency, pocket_code, label, status=ACTIVE)` + a `wallet_balance` row `(available=0, frozen=0)`. **No `wallet_tx`** (no movement).

| Situation | Handling |
|-----------|----------|
| `pocket_code` unknown / `active=false` | Reject — `WALLET_POCKET_DEF_INVALID` (AC-040-08) |
| `def.multi_allowed = false` and member already has a pocket of that def | Reject — `WALLET_POCKET_EXISTS` (AC-040-09) |
| Duplicate `label` for `(member, USER, currency)` | **409** `WALLET_DUPLICATE_CONFLICT` (UNIQUE) |
| Idempotent retry (same `businessRef`) | Return the existing `wallet_id` |

### 11A.2 Close pocket

**Goal:** A USER closes a pocket, **synchronously** → **200**.

- **Precondition:** `available = 0` **and** `frozen = 0`. Funds must be moved out first (11A.3 / withdraw).
- **Accounting:** **none**.
- **Wallet:** set `status = CLOSED`; a CLOSED pocket rejects all mutation (same as LOCKED, [ADR-029](./adr/ADR-029-wallet-locked-rejects-mutation.md)).

| Situation | Handling |
|-----------|----------|
| `available > 0` or `frozen > 0` | Reject — `WALLET_POCKET_NOT_EMPTY` |
| Pocket is the `'default'` pocket | Reject — not user-deletable (AC-040-07) |
| Already `CLOSED` | No-op (idempotent) |

### 11A.3 Pocket-to-pocket transfer (same member)

**Goal:** Move funds between two USER pockets of the **same** member, **synchronously** → **200**. This is internal transfer (§6) specialized to one member; default **fee = 0** for own-pocket moves (product config, [ADR-009](./adr/ADR-009-fee-ownership-orchestration.md)).

#### Postings (example 100,000, fee 0)

| Step | Actor | Account | DR/CR | Amount |
|------|-------|---------|-------|--------|
| 1 | Pocket A | 2110 | DR | 100,000 |
| 2 | Pocket A | 3300 | CR | 100,000 |
| 3 | Pocket B | 3300 | DR | 100,000 |
| 4 | Pocket B | 2110 | CR | 100,000 |

**Result:** **3300 = 0** · **2110 net 0** (DR one pocket, CR the other) — aggregate user liability unchanged; funds never left the user. Accounting sees two `2110` legs and cannot (need not) tell it is the same member.

#### Wallet effect

Debit pocket A (`wallet_id` A) `tx_type = TRANSFER_DEBIT` gross → ledger POSTED (transit 3300; fee to 4130 only if fee > 0) → credit pocket B (`wallet_id` B) `tx_type = TRANSFER_CREDIT` net. Same `businessRef`, one `tx_type` per leg. **Only the two target pockets lock**; other pockets of the member are untouched ([ADR-040](./adr/ADR-040-user-multi-pocket-wallets.md) AC-040-05, [ADR-004](./adr/ADR-004-wallet-balance-snapshot.md)).

| Situation | Handling |
|-----------|----------|
| Insufficient in A | Reject; no leg on B (no partial debit) |
| A debited, B credit fails | Retry `TRANSFER_CREDIT` (idempotent); else compensating credit A `{businessRef}:comp` (§13.3) |
| A = B (same pocket) | Reject — `WALLET_INVALID_TRANSFER` |
| A or B `LOCKED`/`CLOSED` | Reject ([ADR-029](./adr/ADR-029-wallet-locked-rejects-mutation.md)) |

---

## 12. Process ↔ wallet ↔ accounting cross-reference

| Process | Transit | Revenue | `wallet_tx.tx_type` | Sync |
|---------|---------|---------|---------------------|------|
| Deposit | 3100 | 4110 | `DEPOSIT_CREDIT` | Async, credit after POSTED |
| Withdraw | 3200 | 4120 | `WITHDRAW_FREEZE` → `WITHDRAW_SETTLE`/`WITHDRAW_RELEASE` | Sync hold + bank async |
| Wallet payment | 3500 | — | `PAYMENT_DEBIT` / `PAYMENT_CREDIT` | Sync |
| Internal transfer | 3300 | 4130 | `TRANSFER_DEBIT` / `TRANSFER_CREDIT` | Sync |
| IBFT | 3400 | 4130 (−5100) | `IBFT_FREEZE` → `IBFT_SETTLE`/`IBFT_RELEASE` | Sync wallet + bank async |
| Payroll | 3600 | 4150 (−5100) | `PAYROLL_DEBIT` (MERCHANT) | Batch |
| Disbursement | 3700 | 4150 (−5100) | `PARTNER_PREFUND_CREDIT` / `DISBURSEMENT_DEBIT` (PARTNER) | Batch |
| QR/POS | 3500 → 3800 | 4140 (−5100) | none v1 (optional `MERCHANT_SETTLE_CREDIT` at EOD) | Acquirer + EOD |
| EOD settlement | 3800/3810/3820 | 4140 | none (accounting only) | Batch |
| Pocket create/close (§11A) | — | — | none (no movement) | Sync |
| Pocket→pocket transfer (§11A) | 3300 | — (fee 0 default) | `TRANSFER_DEBIT` / `TRANSFER_CREDIT` | Sync |

> Control principle: every transit returns to **0** on completion; total `wallet_balance` per lane reconciles to control **2110+2120+2130** ([`trd/wallet.md`](./trd/wallet.md) §6 W5).

---

## 13. Failure & compensation design (saga)

**ADR:** [ADR-008 — Saga + compensation, no 2PC](./adr/ADR-008-saga-compensation-no-2pc.md) · withdraw/IBFT hold: [ADR-007](./adr/ADR-007-freeze-settle-async-outflow.md).

Tech-agnostic — holds regardless of implementation language. Each flow is a **saga of independent steps**, each with its own local commit; there is **no distributed transaction** ([ADR-003](./adr/ADR-003-dual-schema-single-postgres.md)). Recovery relies on **idempotency** ([ADR-005](./adr/ADR-005-idempotency-key-strategy.md)) + **compensation**, never cross-service rollback.

Per step we define: failure point · resulting state · how detected · recovery (forward-retry vs compensate) · terminal state. Idempotency key = `businessRef` (+ derived sub-keys for follow-up legs).

### 13.1 Deposit (async, 2-phase)

Steps: S0 ack **202** → S1 `BANK_DEPOSIT` outbox → RabbitMQ → accounting worker:
- **Phase A** — TigerBeetle pending Transfer (`id=hash(businessRef+":phaseA")`, `debit=1111`, `credit=3100`, `flags.pending=true`) + `coa_trans` PENDING
- **Phase B** via `confirmDeposit` — TB `post_pending_transfer` + 2 transfers (`hash(businessRef+":2110")` net credit, `hash(businessRef+":4110")` fee) + `coa_trans` POSTED; transit `account[3100].balance = 0` enforced
- accounting worker publishes **`WALLET_CREDIT`** → `core.commands.wallet-credit` (RabbitMQ) → wallet worker → `DEPOSIT_CREDIT`

| Failure point | State | Detection | Recovery | Terminal |
|---------------|-------|-----------|----------|----------|
| Ack lost before enqueue | nothing posted | client re-notifies same `businessRef` | idempotent re-ack + enqueue | safe |
| Journal PENDING fails | none | worker NACK→requeue, poison→DLQ | retry; VA unmappable → keep manual | none / PENDING |
| Confirm POSTED fails | funds in 3100 | aging job: 3100 older than T | retry `confirmDeposit` (idempotent) | eventually POSTED |
| Wallet credit fails after POSTED | ledger done, wallet not | credit error / drift job (W5) | retry credit (idempotent `businessRef`); **never** reverse ledger | eventually credited |
| Bank reverses deposit later | already POSTED | bank reversal webhook | **new reversing journal + wallet debit** (compensation), not edit | net zero |

### 13.1.2 Unknown VA — ops hold (US2)

When the virtual account in the webhook is **not in orchestration's mapping table**, no accounting entry is created and no queue message is published:

| Step | Actor | Action | Result |
|------|-------|--------|--------|
| S0 | Orchestration | Webhook received; VA lookup returns no match | — |
| S0a | Orchestration | Ops hold record logged (`businessRef`, `virtualAccount`, `receivedAt`); 202 returned | No outbox row, no `coa_trans` |
| — | Accounting worker | Not involved | No Phase A, no 3100 posting |
| Manual | Ops | Maps `virtualAccount` → `memberId` + `walletId` in orchestration DB | VA ready |
| Re-trigger | Ops | Re-submits deposit notification with same `businessRef` | Normal S0–S5 (US1) resumes |

**Invariants:**
- `coa_trans` MUST NOT have a row for `businessRef` when VA is unresolved.
- Account 3100 MUST NOT hold any amount for this `businessRef` until Phase A runs.
- Funds sit in bank nostro (1111) **unbooked** until ops triggers re-processing; the reconciliation job (ADR-021) flags this as an unbooked inflow.

**Idempotency:** Re-submission with the same `businessRef` after VA is mapped follows the normal Phase A idempotency guard (`UNIQUE(reference_id, use_case)` on `coa_trans`).

**VA mapping change after POSTED (immutability):** If a VA mapping is updated after a deposit has already been POSTED with the old mapping, the existing `coa_trans` lines and wallet credit are immutable (ADR-001, ADR-026). The mapping change affects only future deposits.

### 13.1.3 Phase-A reversal and PENDING aging (US3)

**Triggers for Phase-A reversal:**
- Bank cancellation — bank reports the transfer is cancelled before confirmation
- Amount mismatch — `confirmDeposit` called with gross/fee that fails Phase B balance validation
- Ops manual void — ops voids an aging PENDING journal after SLA

**Reversal sequence:**

| Step | Actor | Action | Result |
|------|-------|--------|--------|
| 1 | Ops / cancel event | Signals reversal for `businessRef` | — |
| 2 | `app-accounting-worker` | TigerBeetle: `void_pending_transfer(id=hash(businessRef+":phaseA"))` | 1111/3100 cleared — both nets = 0 |
| 3 | `app-accounting-worker` | `coa_trans.status = FAILED` | Journal closed |
| 4 | — | No `WALLET_CREDIT` published | Wallet untouched throughout |

**Invariants after reversal:**
- `account[3100].balance = 0` (INV-03 still satisfied)
- `wallet_balance.available` unchanged — no `DEPOSIT_CREDIT` `wallet_tx` row for this `businessRef`
- `coa_trans.status = FAILED` is terminal and immutable (ADR-001)

**Amount-mismatch path:** `confirmDeposit(coaTransId, fee)` validates that `net + fee = grossFromPhaseA`. If this check fails, no TigerBeetle Phase B transfers are created; the worker proceeds to `void_pending_transfer` and marks the journal FAILED. The wallet never receives a `WALLET_CREDIT`.

**PENDING aging (ADR-021):**

| Condition | Detection | Ops action |
|-----------|-----------|------------|
| `coa_trans.status = PENDING` past SLA | Aging job polls `coa_trans` | Alert to ops channel |
| Bank confirms late | Ops calls `confirmDeposit` | → Phase B + wallet credit (normal path) |
| Bank cancels / no response | Ops calls void | → `void_pending_transfer` → FAILED |

> **Rule:** Aging does NOT auto-reverse. Only ops chooses the terminal outcome (POSTED or FAILED). `timeout ≠ failure` — same principle as withdraw (§13.4).

### 13.2 Payment (sync, 3 commits)

Steps: debit USER → post journal (3500→0) → credit MERCHANT. **Primary strategy = forward recovery** because after step 2 the ledger is already balanced.

| Failure after | State | Recovery | Terminal |
|---------------|-------|----------|----------|
| Step 1 (debit) | only user debited, no ledger | **compensate**: reversing credit to USER `{businessRef}:comp`; return 422 | user made whole |
| Step 2 (post) | user debited + ledger POSTED, merchant not credited | **forward-retry** merchant credit (idempotent); persistent → ops reconcile (merchant owed, funds safe) | eventually credited |
| Step 3 (response lost) | all done | client retry same `businessRef` → idempotent replay | consistent |

### 13.3 Transfer

Same shape as payment, both legs USER. Compensation reverses debit A `{businessRef}:comp` only if the journal post fails; otherwise forward-retry credit B.

### 13.4 Withdraw (freeze + async payout) — the double-spend-critical flow

Steps: freeze → accept 200 → payout (bank) → settle | release.

| Failure point | State | Detection | Recovery | Terminal |
|---------------|-------|-----------|----------|----------|
| After freeze, before payout enqueue | funds frozen | aging: frozen w/o payout > T | release (unfreeze) or retry enqueue | returned or proceed |
| Payout result **unknown** (bank timeout) | frozen, payout ambiguous | payout poll / timeout | **do NOT release** on timeout — query bank to a terminal result first | exactly-once payout |
| Payout success, settle fails | money left bank, still frozen | drift: paid but frozen | retry settle (idempotent) | settled |
| Payout failed (terminal) | frozen | bank failure event | release (unfreeze) | funds returned |
| Redelivered payout command | — | bank adapter idempotent on `businessRef` | dedup at adapter | no double pay |

**Hard rule:** timeout ≠ failure. Releasing a frozen hold while a payout may still succeed = double-spend. Release only on a bank-confirmed terminal failure.

### 13.5 IBFT

Identical pattern to withdraw (freeze → settle/release, transit 3400) plus Napas cost 5100 on the accounting side. Same double-spend rule.

### 13.6 Payroll / Disbursement (batch)

- Each recipient is a sub-transaction keyed `{businessRef}:{recipientId}`.
- One failed recipient must **not** block the rest; produce a **partial batch result** (succeeded / failed / retrying).
- Failed legs: forward-retry or release the held amount back to the merchant/partner wallet.
- Emit a batch summary event for reconciliation.

### 13.7 EOD settlement (batch)

- Idempotent per `(merchantId, settlementDate)` — re-running a day is safe.
- Outbound bank transfer fails → amount stays in 3810, retried next cycle; **never double-settle**.

---

## 14. Edge cases & invariants under partial failure

| Edge case | Required behavior |
|-----------|-------------------|
| Same `businessRef`, different amount | Reject `WALLET_DUPLICATE_CONFLICT` / 409 — never silently overwrite ([ADR-005](./adr/ADR-005-idempotency-key-strategy.md)) |
| Concurrent debit on last balance | Serialize per wallet (row lock / version); exactly one wins, other → insufficient |
| Wallet `LOCKED` mid-flow | Reject new debit/freeze; settling an already-frozen leg is allowed (or ops-gated) |
| "Accepted but unknown" hangs | Every async pending state (deposit PENDING, frozen withdraw) needs an **aging job** + terminal resolution; nothing stays pending forever |
| Currency mismatch | Reject at boundary; v1 VND only; no implicit FX |
| Amount ≤ 0 or scale > 4 | Validation reject before any mutation |
| Reconciliation drift (W5) beyond tolerance | Alert + stop auto-adjust; manual investigation; wallet never mutates COA |
| Reversal after period close | New reversing journal, never edit ([ADR-001](./adr/ADR-001-immutable-ledger.md)) |
| Out-of-order events (credit before POSTED) | Buffer / dedup by `businessRef` + state guard |

---

## 15. Reliability patterns (cross-cutting, tech-agnostic)

| Pattern | Rule |
|---------|------|
| Saga + outbox | Each service writes domain state + outbox record in **one local commit**; a relay publishes → no lost events, at-least-once |
| At-least-once delivery | All consumers idempotent ([ADR-005](./adr/ADR-005-idempotency-key-strategy.md)); transport dedup by `messageId` |
| DLQ | Poison messages → `core.commands.dlq`; failures fan-out via `core.operations.command-failed` |
| Aging / timeout jobs | One per async pending state; resolves to a terminal state |
| Reconciliation (W5) | Periodic wallet ↔ COA control compare; **report-only**, no auto-COA mutation |
| Compensation over rollback | Every effect that can be left dangling must have a defined compensating action |

> These are stated in domain terms (steps, states, keys) so the design survives a language/runtime change (e.g. Java → Go): only the wire (`spec/contracts/open-api/`, `spec/contracts/async-api/`) and idempotency keys are contractual; the recovery semantics above are the spec.

---

## 16. Fee policy (design)

**ADR:** [ADR-009 — Fee ownership at orchestration](./adr/ADR-009-fee-ownership-orchestration.md).

Tech-agnostic. Fees are a **design gap closed here**, not left to implementation choice.

| Rule | Decision |
|------|----------|
| Who computes fees | **Orchestration** only. The wallet never recomputes a fee; accounting only records the fee lines it is given |
| Fee schedule | **Configuration** (per use case, optionally per tier/partner), not hard-coded. v1 example values: deposit 1,000, withdraw 1,000, transfer/IBFT 1,000, QR/POS MDR = % of amount, payroll/disbursement per-recipient |
| Who bears the fee | Deposit → user (netted from credit); withdraw / transfer / IBFT → user (added to gross debit); QR/POS MDR → merchant; payroll → merchant; disbursement → partner |
| Revenue mapping | 4110 deposit · 4120 withdraw · 4130 transfer/IBFT · 4140 MDR · 4150 payroll/disbursement; bank/Napas cost → 5100 |
| Rounding | Scale 4, HALF_UP at the boundary (foundation §6) |
| Consistency invariant | The fee value orchestration sends to the wallet (gross/net) and to accounting (4xxx line) must be the **same computed number** — single source = orchestration |

> Only the **rates** are configurable; the **ownership and posting rules** above are the contract. Switching runtime (Java→Go) does not change them.

---

## 17. Authentication & identity (design)

Wire/IdP specifics live at the Gateway; the **binding rules** below are the design contract.

| Rule | Decision |
|------|----------|
| Public S1 | Bearer JWT (OIDC) terminated at / validated behind the Gateway |
| Partner / bank channel | mTLS (`notifyDeposit`, `bankWebhook`) — see `spec/contracts/open-api/gtelpay-public.yaml` |
| `memberId` source | Derived from the **token subject**, never trusted from the request body for authorization. Body `memberId` is for routing only and must match the principal |
| Authorization scope | A user acts only on own wallet; a merchant on own merchant wallet; a partner on own escrow. Server enforces token-subject ↔ target wallet owner |
| Internal accounting API (S2) | Service-to-service auth; RBAC roles from `trd/accounting.md` NFR-6: Accountant, Auditor, Admin, ReadOnly |
| Idempotency scope | `X-Idempotency-Key` is per principal; a key from one member cannot replay another's action |

> Implication: any flow above that takes `memberId` in the body assumes the orchestration has already verified it against the authenticated principal. This closes the earlier "auth = stub" gap at the design level (the IdP wiring remains a Gateway integration task).

---

## 18. References

| Question | Document |
|----------|----------|
| Detailed DR/CR postings, COA, transit | [`foundation.md`](./foundation.md) §6–16 |
| `wallet_*` tables, credit/debit/freeze FR | [`trd/wallet.md`](./trd/wallet.md) |
| Surfaces, step order, idempotency, Kafka/RabbitMQ | [`integration-surfaces.md`](./integration-surfaces.md) |
| Accounting FR / NFR | [`trd/accounting.md`](./trd/accounting.md) |
| journal / journal_entry terminology | [`terminology.md`](./terminology.md) |
| Locked implementation decisions D1–D5 | [`implementation.md`](./implementation.md) §2 |
