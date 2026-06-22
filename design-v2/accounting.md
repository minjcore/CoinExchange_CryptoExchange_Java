# core.accounting — Design

**Status:** Draft · **Scope:** ledger domain only (`coa_*`) · **Not:** wallet, HTTP, fee rates, bank adapters

Posting amounts and DR/CR line templates live in [`core.sharedlib.md`](../core.sharedlib.md) Part II §8–16.  
This document defines **behavior, obligations, failures, and boundaries** — not a second copy of those tables.

---

## 1. Domain contract

Accounting answers one question: *what is the balanced financial truth for the platform, with an audit trail?*

| Guarantees | Never does |
|------------|------------|
| Every POSTED journal balances: `sum(DR) = sum(CR)` | Store per-member spendable balance |
| Transit account for a use case nets to **zero** at terminal POSTED | Import or JOIN `wallet_*` |
| Posted lines are immutable; fix = reversal journal | Recompute fees from product rules |
| `business_ref` idempotency on journal create | Expose public HTTP |
| Publishes `JournalPosted` after POSTED (adapter) | Auto-adjust COA because wallet drifted |

Orchestration sequences accounting with wallet. Accounting **never** waits on wallet success to mark POSTED (deposit is the exception pattern: POSTED then wallet credit is downstream).

---

## 2. Core concepts

| Term | Meaning |
|------|---------|
| **Journal** | `coa_trans` — one business event header (`use_case`, `business_ref`, `status`) |
| **Journal entry** | `coa_trans_data` — one DR or CR line |
| **Control account** | 2110 / 2120 / 2130 — aggregate liability mirrors wallet lanes (reconciliation only) |
| **Transit account** | 3100–3820 — must be zero when flow completes |
| **POSTED** | Terminal balanced state; lines immutable |

TRD naming trap: API “journal entry” = platform **journal** (`coa_trans`). See [`TERMINOLOGY.md`](../TERMINOLOGY.md).

---

## 3. Locked decisions

| ID | Decision | Source |
|----|----------|--------|
| L1 | Immutable ledger — no UPDATE on POSTED lines | ADR-001 |
| L2 | Schema `accounting`; no cross-schema FK to `wallet` | ADR-003 |
| L3 | `business_ref` end-to-end idempotency key | ADR-005 |
| L4 | Deposit is **two-phase** (`PENDING` → `POSTED`); transit 3100 holds until confirm | [ADR-006](../adr/ADR-006-two-phase-deposit.md) |
| L5 | Fee amounts arrive from orchestration; accounting records given lines | [ADR-009](../adr/ADR-009-fee-ownership-orchestration.md) |
| L6 | Wallet sync after POSTED via event/API — not accounting repository | foundation §3 |
| L7 | Transit account for use case nets **zero** at terminal POSTED | [ADR-010](../adr/ADR-010-transit-accounts-net-zero.md) |

**ADR conformance:** [ADR-001](../adr/ADR-001-immutable-ledger.md) · [ADR-003](../adr/ADR-003-dual-schema-single-postgres.md) · [ADR-005](../adr/ADR-005-idempotency-key-strategy.md) · [ADR-006](../adr/ADR-006-two-phase-deposit.md) · [ADR-010](../adr/ADR-010-transit-accounts-net-zero.md) — AC/TC in each file.

---

## 4. Use cases — accounting obligation

Sync = journal reaches POSTED in same orchestration request (wallet legs may follow).  
Async = journal may POSTED before external bank leg completes (withdraw/IBFT wallet freeze is orchestration; ledger may still POSTED on accept per product).

| Use case | Transit | Phases | Accounting completes when | Wallet handoff |
|----------|---------|--------|---------------------------|----------------|
| DEPOSIT | 3100 | PENDING → POSTED | POSTED and 3100 = 0 | After POSTED: credit USER net |
| WITHDRAW | 3200 | single POSTED | POSTED and 3200 = 0 | Freeze before 200; settle/release async |
| PAYMENT | 3500 | single POSTED | POSTED and 3500 = 0 | Debit user before post; credit merchant after |
| TRANSFER | 3300 | single POSTED | POSTED and 3300 = 0 | Debit A / credit B around post |
| IBFT | 3400 | single POSTED | POSTED and 3400 = 0 | Freeze pattern; bank async |
| QR/POS | 3500 | single POSTED | POSTED and 3500 = 0 | Often no per-txn wallet; 2120 pending EOD |
| PAYROLL | 3600 | batch POSTED | 3600 = 0 | MERCHANT wallet debit if product requires |
| DISBURSEMENT | 3700 | pre-fund + batch | 3700 = 0 | PARTNER lane if product requires |
| SETTLEMENT | 3800/3810/3820 | EOD batch | all three = 0 | Optional bulk merchant wallet |

DR/CR for each row: [`core.sharedlib.md`](../core.sharedlib.md) §8–16.

---

## 5. Journal state machine (global)

```
                    createJournal
                         │
                         ▼
              ┌───── PENDING ─────┐     (deposit phase A only)
              │         │         │
              │    addLines       │ failed / reverse
              │         ▼         ▼
              │      POSTED ◄── FAILED
              │         │
              │    reverseJournal
              │         ▼
              └──── REVERSED
```

| Status | Allowed ops | Forbidden |
|--------|---------------|-----------|
| PENDING | addLines, postJournal (deposit: phase B only), fail/reverse A | Treat as wallet-creditable |
| POSTED | reverseJournal, read | addLines that change economics, UPDATE lines |
| FAILED | reverse cleanup, archive | postJournal without new journal |
| REVERSED | read, link chain | re-POST original |

---

## 6. Failure & compensation

Principle: **POSTED is forward-only.** Recovery = retry idempotent downstream, or **new** reversal/compensation journal — never mutate posted lines.

### 6.1 Deposit

| Failure | State | Detection | Recovery | Forbidden |
|---------|-------|-----------|----------|-----------|
| Duplicate bank webhook | PENDING or POSTED | Same `business_ref` | Return existing `coa_trans_id` | Second phase A |
| VA not mapped | PENDING, 3100 > 0 | No memberId | Hold; ops map; then phase B | Force POSTED |
| Confirm amount ≠ phase A | PENDING | Validation | Reject phase B; optional reverse A | Post unbalanced |
| Phase B succeeds, wallet credit fails | POSTED | Consumer lag / alert | Retry `WALLET_CREDIT` / event consumer | Edit 2110 lines |
| Bank recall after POSTED | POSTED | Bank notice | Reversal journal + orchestration wallet case | DELETE journal |
| Stuck PENDING over SLA | PENDING | Aging job | Ops: reverse A or complete B | Auto-fail silently |

### 6.2 Withdraw

| Failure | State | Detection | Recovery | Forbidden |
|---------|-------|-----------|----------|-----------|
| Insufficient aggregate 2110 at post | — | Pre-post check | Reject journal | Post anyway |
| POSTED, bank timeout | POSTED | Payout timer | Poll bank; **do not** wallet RELEASE early | Assume failure on timeout |
| Bank reject | POSTED | Callback | Orchestration RELEASE; reversal if cash left bank | Double payout |
| 3200 ≠ 0 after post | — | Post validation | Reject post | — |
| Duplicate accept | POSTED | Idempotency | No-op | Second journal |

### 6.3 Payment

| Failure | State | Detection | Recovery | Forbidden |
|---------|-------|-----------|----------|-----------|
| User wallet debited, post fails | wallet debited, no POSTED | Orchestration error | Compensating user credit (orchestration) | Leave user debited silently |
| POSTED, merchant credit fails | POSTED | Partial saga | Retry merchant credit (idempotent) | Reverse ledger automatically |
| 3500 ≠ 0 | — | Post guard | Reject | — |
| Same ref, different amount | — | 409 / conflict | Reject | Apply second amount |

### 6.4 Transfer

| Failure | State | Detection | Recovery | Forbidden |
|---------|-------|-----------|----------|-----------|
| Debit A OK, post fails | A debited | Saga | Credit A back or retry post | Debit A twice |
| POSTED, credit B fails | POSTED | Saga | Retry B credit | — |
| Fee line omitted | — | Unbalanced | Reject post | — |

### 6.5 IBFT

Same compensation class as withdraw for bank leg. Ledger POSTED does not imply bank success.  
5100 / 4130 lines per foundation §11 — posting template there, not here.

### 6.6 QR/POS, payroll, disbursement, EOD

| Class | Rule |
|-------|------|
| Batch partial failure | Transit must not stay non-zero; reverse batch or complete remaining via ops |
| File mismatch (EOD) | Block settlement; reverse lock branch per foundation §16 exception |
| Duplicate batch id | Idempotent return |

---

## 7. Edge cases

| Case | Accounting behavior |
|------|---------------------|
| Idempotent replay `postJournal` on POSTED | No-op, return current |
| Same `business_ref`, different `use_case` | Allowed (separate journals) |
| Same `business_ref` + `use_case`, different amounts | Reject second create (conflict) |
| Out-of-order `JournalPosted` consumer | Consumer idempotent on `business_ref` |
| Period closed | Reject post dated in closed period |
| PENDING deposit across period boundary | Block close or ops playbook |
| Reversal of reversal | Allowed as new journal; maintain `reverses_id` chain |
| Inactive COA `account_code` | Reject at addLines |

---

## 8. Validation before POSTED

| Check | On fail |
|-------|---------|
| `sum(DR) = sum(CR)` | `ACCOUNTING_UNBALANCED_JOURNAL` |
| All accounts active | reject |
| Period open for `posting_date` | reject |
| Use-case transit net zero (when template complete) | reject |
| FK `account_code` exists | reject |

Deposit phase A may be intentionally unbalanced on 2110 (no user liability yet) — only 1111/3100 lines.

---

## 9. Integration surface (pointers)

| Surface | Role |
|---------|------|
| S2 [`spec/contracts/open-api/accounting-internal.yaml`](../spec/contracts/open-api/accounting-internal.yaml) | create / lines / post / reverse / balance |
| S6 `BANK_DEPOSIT` command | Worker creates deposit journal |
| S3 `JournalPosted` | [`spec/contracts/async-api/core-events.yaml`](../spec/contracts/async-api/core-events.yaml) |
| Step order | [`orchestration.md`](./orchestration.md), [`integration-surfaces.md`](../integration-surfaces.md) §4 |

Accounting module does not parse S1 public HTTP.

---

## 10. Reconciliation

| Check | Owner | Action on drift |
|-------|-------|-----------------|
| 2110/2120/2130 vs wallet sums | Application batch | **Report only** — no auto journal from wallet |
| Bank statement vs 1111/1112/1113 | Ops + accounting flags | Matched → reconciled state (TRD FR-10) |
| Transit aging ≠ 0 | Accounting report | Alert ops |

---

## 11. Open product decisions

Document honestly — not specified in existing `10_core` sources:

| # | Question |
|---|----------|
| O1 | Withdraw/IBFT: POSTED on HTTP accept vs only after bank ACK? |
| O2 | Payment saga: auto-compensate user on post fail vs manual ops queue? |
| O3 | QR/POS: per-txn 2120 only, or always EOD? |
| O4 | Multi-currency: v1 VND only — FX journal template TBD |
| O5 | `confirmDeposit` dedicated API vs generic addLines+post |

---

## 12. Out of scope (module)

Tax, invoice generation, payroll calculation, public API, wallet mutations, Gateway — per [`core.accounting.trd.md`](../core.accounting.trd.md) §2.

---

## 13. Related docs

| Need | Read |
|------|------|
| DR/CR line tables | [`core.sharedlib.md`](../core.sharedlib.md) Part II §8–16 |
| TRD FR/NFR | [`core.accounting.trd.md`](../core.accounting.trd.md) |
| Wallet mirror | [`wallet.md`](./wallet.md) |
| Given/When/Then | [`acceptance.md`](./acceptance.md) |
| End-to-end narrative | [`core.business-processes.md`](../core.business-processes.md) |

---

# Part II — Per use case (accounting-only depth)

Part II is the **expandable body** of this design. DR/CR amounts stay in [`core.sharedlib.md`](../core.sharedlib.md) §8–16 — here we specify **states, observables, failures, idempotency, and reconciliation** per use case. Target corpus (all `design-v2/` files + acceptance scenarios): on the order of **hundreds of pages** when printed; not duplicated padding.

**How to read each section**

| Block | Purpose |
|-------|---------|
| Obligation | What accounting must achieve |
| Observables | What to assert on `coa_*` at each terminal state |
| Phases | Status transitions accounting owns |
| Failures | Row-level recovery; wallet/orchestration columns note handoff only |
| Idempotency | Keys and conflict rules |
| Recon | Batch/control signals |
| Scenarios | Pointer to [`acceptance.md`](./acceptance.md) |

---

## 14. DEPOSIT (transit 3100, two-phase)

**Foundation reference:** [`core.sharedlib.md`](../core.sharedlib.md) §8 (example: gross 100,000, fee 1,000, net liability 99,000).

### 14.1 Obligation

1. Record bank inflow on **1111** without crediting member liability until confirmed.
2. Hold gross in **3100** while `PENDING`.
3. On confirm, clear **3100**, recognize **2110** net + **4110** fee, emit `JournalPosted`.
4. Never depend on wallet success to reach `POSTED`.

### 14.2 Phases (accounting)

| Phase | `coa_trans.status` | Lines present | 3100 balance effect | 2110 / 4110 |
|-------|-------------------|---------------|---------------------|-------------|
| A | `PENDING` | 1111 DR, 3100 CR (gross) | +gross (CR) | unchanged |
| B | `POSTED` | + steps 3–6 per foundation §8.1 | net zero | +net, +fee revenue |
| Fail | `FAILED` | reversal of A if posted | cleared per reversal | none |

### 14.3 Observables

**After phase A (`PENDING`):**

- `use_case = DEPOSIT`
- `business_ref` UNIQUE (bank txn id)
- Line count = 2 (1111, 3100)
- `sum(DR) = sum(CR) = gross`
- **3100 CR gross** — non-zero transit allowed
- No **4110** line yet (fee not recognized until B)

**After phase B (`POSTED`):**

- **3100 net = 0**
- **1111** increased by gross
- **2110** increased by net (mirrors eventual wallet credit)
- **4110** increased by fee (if fee > 0)
- `sum(DR) = sum(CR)` on full journal
- Immutable lines — no UPDATE

### 14.4 Failure matrix (accounting actions)

| # | Failure | Ledger state | Accounting action | Do not |
|---|---------|--------------|-------------------|--------|
| D-F1 | Duplicate webhook same `business_ref` | any | Return existing `coa_trans_id` | Second insert |
| D-F2 | VA unmapped | `PENDING`, 3100 > 0 | Hold; no phase B | Force POSTED |
| D-F3 | Confirm amount ≠ phase A gross | `PENDING` | Reject B; optional reverse A | Post partial |
| D-F4 | Confirm when already `POSTED` | `POSTED` | Idempotent no-op | New lines |
| D-F5 | Phase B unbalanced lines | — | Reject `postJournal` | Store draft |
| D-F6 | Wallet credit lag after POSTED | `POSTED` | None — outbox/event already fired | Edit 2110 |
| D-F7 | Bank recall after POSTED | `POSTED` | New **reversal** journal + link `reverses_id` | DELETE/UPDATE |
| D-F8 | Stuck `PENDING` > SLA | `PENDING` | Ops: complete B or reverse A | Silent auto-fail |
| D-F9 | Period closed before B | `PENDING` | Block B or ops playbook | Backdated post |
| D-F10 | Inactive 1111/3100/2110/4110 | — | Reject at `addLines` | — |
| D-F11 | Worker crash mid phase A | partial TX | Local TX rollback; retry idempotent | Orphan lines |
| D-F12 | Same ref, different gross | — | Conflict on create | Overwrite |
| D-F13 | Fee line missing at B | — | Reject — unbalanced vs gross | Post net-only |
| D-F14 | Zero-fee deposit | `POSTED` | 4110 unchanged; 2110 += gross | — |
| D-F15 | Multi-currency webhook (future) | — | Reject at boundary v1 | FX journal |

### 14.5 Idempotency

| Key | Scope |
|-----|-------|
| `business_ref` | One journal per bank deposit event |
| `postJournal` on `POSTED` | Safe replay |
| `confirmDeposit` / phase B | Same ref → no duplicate 2110 |

### 14.6 Reconciliation

| Signal | Meaning |
|--------|---------|
| Aging **3100** > T | Stuck deposits — ops queue |
| **1111** vs bank statement | FR-10 bank recon (TRD) |
| **2110** vs wallet sum | Application W5 — report only |
| POSTED without wallet credit | Drift report — not accounting fix |

**Acceptance:** [`acceptance.md`](./acceptance.md) Feature Deposit (18 scenarios).

---

## 15. WITHDRAW (transit 3200)

**Foundation reference:** §9 (example: gross 101,000 = principal 100,000 + fee 1,000).

### 15.1 Obligation

1. Recognize member liability reduction and fee revenue when product posts on accept (see O1).
2. Clear **3200** to zero at terminal `POSTED`.
3. Move principal from **1111** (bank asset down).
4. Record withdraw fee to **4120** when applicable.

### 15.2 Observables (`POSTED`)

- **2110** decreased by gross (aggregate; wallet uses freeze/settle)
- **3200** net zero
- **1111** decreased by principal
- **4120** increased by fee (if fee > 0)
- `use_case = WITHDRAW`

### 15.3 Failure matrix

| # | Failure | Accounting action | Do not |
|---|---------|-------------------|--------|
| W-F1 | Post with 3200 ≠ 0 after template | Reject | — |
| W-F2 | Duplicate accept same ref | Idempotent return | Second journal |
| W-F3 | Bank timeout (orchestration) | No accounting change until terminal | Reverse on timeout |
| W-F4 | Bank reject | No automatic reversal if POSTED on accept; ops reversal if cash moved | Assume ledger undone |
| W-F5 | Payout success, settle wallet fails | Ledger unchanged | Auto reverse 2110 |
| W-F6 | Insufficient 2110 at post time | Reject create/post | Post anyway |
| W-F7 | Same ref different gross | Conflict | — |
| W-F8 | Period closed | Reject post | — |
| W-F9 | Double payout at bank | Adapter idempotent; ledger single POSTED | Second 1111 CR |
| W-F10 | Fee zero | 4120 unchanged | — |

### 15.4 Open coupling (O1)

Accounting `POSTED` timing vs bank ACK is a **product** choice documented in §11 O1 — this module implements whichever policy orchestration calls; observables above assume POSTED on accept per current IMPLEMENTATION D5 pattern.

**Acceptance:** Feature Withdraw (12 scenarios).

---

## 16. PAYMENT (transit 3500, sync)

**Foundation reference:** §13 (user 2110 DR, merchant 2120 CR, 3500 clears).

### 16.1 Obligation

1. Move liability from user **2110** to merchant **2120** through **3500**.
2. Single-phase `POSTED` in same orchestration request as wallet debit (ordering: wallet debit → post → wallet credit).
3. **3500 = 0** at POSTED.

### 16.2 Observables

- **2110** −gross (user)
- **2120** +amount to merchant (gross or net per fee policy — orchestration passes lines)
- **3500** net zero
- If platform fee on payment: additional 41xx per product template (foundation §13 is fee-in-gross to merchant; fee split is orchestration line builder)

### 16.3 Failure matrix

| # | Failure | Ledger state | Accounting action |
|---|---------|--------------|-------------------|
| P-F1 | User debited, post fails | no POSTED | No journal; orchestration compensates wallet |
| P-F2 | POSTED, merchant credit fails | POSTED | None; retry wallet |
| P-F3 | 3500 ≠ 0 at guard | — | Reject post |
| P-F4 | Idempotent replay | POSTED | Return same `coa_trans_id` |
| P-F5 | Same ref, different amount | — | Conflict |
| P-F6 | Merchant 2120 inactive | — | Reject lines |
| P-F7 | Self-pay (same member) | — | Reject at orchestration; no post |
| P-F8 | Partial line set | — | `ACCOUNTING_UNBALANCED_JOURNAL` |

**Acceptance:** Feature Wallet payment (10 scenarios).

---

## 17. TRANSFER (transit 3300, sync)

**Foundation reference:** §10 (A gross 101,000, B net 100,000, **4130** fee 1,000).

### 17.1 Obligation

1. Reallocate **2110** between two users via **3300**.
2. Record transfer fee on **4130**.
3. No bank asset movement.

### 17.2 Observables

- **2110** pool: net −fee (A−B net transfer)
- **3300** = 0
- **4130** +fee

### 17.3 Failure matrix

| # | Failure | Accounting action |
|---|---------|-------------------|
| T-F1 | Debit A OK, post fails | No POSTED; orchestration restores A |
| T-F2 | POSTED, credit B fails | POSTED stands; retry B |
| T-F3 | A→A transfer | Reject at orchestration |
| T-F4 | Fee omitted | Reject unbalanced post |
| T-F5 | Duplicate ref | Idempotent |

**Acceptance:** Feature Internal transfer (6 scenarios).

---

## 18. IBFT (transit 3400)

**Foundation reference:** §11 (Napas **1112**, **5100** cost, **4130** fee).

### 18.1 Obligation

1. Same liability pattern as withdraw for user **2110** / **3400**.
2. Bank leg uses **1112** not **1111**.
3. Record Napas/bank expense **5100** per foundation template.

### 18.2 Failure matrix

Identical compensation **class** to withdraw (W-F3–W-F5): timeout ≠ ledger reversal. Accounting does not RELEASE wallet — orchestration does.

| # | IBFT-specific | Rule |
|---|---------------|------|
| I-F1 | 5100 line missing | Reject if template requires |
| I-F2 | Wrong clearing account | Must use 1112 for Napas leg |
| I-F3 | Double Napas submit | Bank adapter idempotent |

**Acceptance:** Feature IBFT (5 scenarios).

---

## 19. QR/POS (transit 3500 → EOD 3800+)

**Foundation reference:** §12 capture; §16 settlement.

### 19.1 Obligation

1. Per capture: **1113** inflow, **2120** merchant liability via **3500**.
2. Acquirer cost **5100** on capture template.
3. EOD moves **2120** through **3800/3810/3820** — accounting batch, not wallet per txn (default).

### 19.2 Failure matrix

| # | Failure | Accounting action |
|---|---------|-------------------|
| Q-F1 | Acquirer duplicate capture id | Idempotent journal |
| Q-F2 | MDR line mismatch | Reject post |
| Q-F3 | EOD file ≠ 2120 | Block settlement branch §16 exception |
| Q-F4 | Partial merchant file | No POSTED settlement |

**Acceptance:** Feature QR/POS + EOD.

---

## 20. PAYROLL (transit 3600, batch)

**Foundation reference:** §14.

### 20.1 Obligation

1. Debit merchant **2120** gross (salaries + fee).
2. Clear **3600** after bank outflow lines.
3. **4150** fee revenue; **5100** bank cost per template.

### 20.2 Partial batch

| Rule | Behavior |
|------|----------|
| Per-recipient key | `{businessRef}:{recipientId}` |
| One recipient fails | Others may still POST sub-journals per product |
| Transit | **3600** must not remain non-zero for completed batch slice |

**Acceptance:** Feature Payroll batch.

---

## 21. DISBURSEMENT (transit 3700, pre-fund + batch)

**Foundation reference:** §15.

### 21.1 Obligation

1. **Pre-fund:** 1111 → **2130** (partner escrow liability).
2. **Disburse:** 2130 → **3700** → bank out + **4150** / **5100** per template.
3. **3700 = 0** when batch complete.

### 21.2 Failure matrix

| # | Failure | Rule |
|---|---------|------|
| B-F1 | Insufficient 2130 | Reject batch leg |
| B-F2 | Partial disburse | Same partial rules as payroll |
| B-F3 | Duplicate batch id | Idempotent |

**Acceptance:** Feature Partner disbursement.

---

## 22. EOD SETTLEMENT (3800, 3810, 3820)

**Foundation reference:** §16.

### 22.1 Obligation

1. Lock **2120** into **3800**.
2. Split MDR to **3820** / **4140**.
3. Settle net via **3810** to bank **1112**.
4. All three transits zero on success.

### 22.2 Exception branch

On file mismatch: reverse lock per foundation §16 — **2120** restored, transits cleared, settlement blocked.

### 22.3 Failure matrix

| # | Failure | Rule |
|---|---------|------|
| E-F1 | File total ≠ 2120 | Block; exception queue |
| E-F2 | Outbound bank fail | Amount stays in 3810; retry next cycle |
| E-F3 | Re-run same `(merchantId, date)` | Idempotent |
| E-F4 | Double settle | Forbidden — idempotent guard |

**Acceptance:** Feature EOD settlement.

---

## 23. Cross-cutting accounting operations

### 23.1 `reverseJournal`

| Input | Behavior |
|-------|----------|
| POSTED source | New journal `REVERSED` link; mirrored DR/CR |
| PENDING deposit A | Reverse 1111/3100 only |
| Period closed | New dated reversal in open period |

### 23.2 Period close

| Check | On fail |
|-------|---------|
| No non-zero transit | Block close |
| No open PENDING deposits | Block or waiver list |
| Trial balance | Must balance |

### 23.3 Bank reconciliation flags (TRD FR-10)

| State | Meaning |
|-------|---------|
| Unmatched | Statement line ≠ ledger |
| Matched | Ops confirmed |
| Adjusting entry | New journal — not line edit |

### 23.4 Error code surface (accounting module)

| Code | When |
|------|------|
| `ACCOUNTING_UNBALANCED_JOURNAL` | DR ≠ CR |
| `ACCOUNTING_PERIOD_CLOSED` | posting_date in closed period |
| `ACCOUNTING_DUPLICATE_REFERENCE` | `business_ref` + `use_case` clash |
| `ACCOUNTING_INACTIVE_ACCOUNT` | COA flag |
| `ACCOUNTING_INVALID_STATUS_TRANSITION` | e.g. addLines on POSTED |

---

## 24. Corpus map (honest sizing)

| Artifact | Current scale | Full corpus target |
|----------|---------------|-------------------|
| `accounting.md` Part I | ~13 sections | Done for contract layer |
| `accounting.md` Part II | §14–23 (this appendix) | +edge-case addenda per flow as product spec arrives |
| `wallet.md` | Part I §1–14 + Part II §15–28 | Balance semantics, hold lifecycle, IBFT/QR/EOD depth |
| `orchestration.md` | Part I §1–10 + Part II §11–27 | IBFT depth, bank poll policy, QR/EOD wallet optional |
| `acceptance.md` | **150** gate / **207** full | 150–300 target — gate met |
| `accounting/` corpus (VI) | Quyển I–III viết | Mục tiêu 1000 trang — [`accounting/README.md`](./accounting/README.md) |
| `spec/processes.md` | ~496 lines | End-to-end narrative |

**Corpus VI:** [`accounting/vol-01-principles.md`](./accounting/vol-01-principles.md) · [`vol-02-coa-handbook.md`](./accounting/vol-02-coa-handbook.md) · [`vol-03-deposit.md`](./accounting/vol-03-deposit.md). Còn Quyển IV–X.

**1000 printed pages** vẫn cần product tables (fee schedule, bank SLA, ops playbook) — chỗ thiếu ghi `[TBD]` trong corpus.

---

## 26. Reference synthesis — ledger & accounting (`references/`)

Industry corpus (~108 files, ~32.7k lines) mapped to **this domain**. DR/CR amounts stay in [`core.sharedlib.md`](../core.sharedlib.md) — here we capture **behaviors and invariants** only.

### 26.1 Double-entry & immutable ledger

| Reference | Industry lesson | Project binding |
|-----------|-----------------|-----------------|
| [`square-books-double-entry-ledger.md`](../references/square-books-double-entry-ledger.md) | Append-only journal; no UPDATE on posted lines | ADR-001; `coa_trans_data` immutable POSTED |
| [`moderntreasury-accounting-dev-part-i.md`](../references/moderntreasury-accounting-dev-part-i.md) … **part-iii** | DR/CR mental model for engineers | Foundation §6–16 templates |
| [`moderntreasury-single-vs-double-entry.md`](../references/moderntreasury-single-vs-double-entry.md) | Why double-entry for fintech | COA + transit accounts |
| [`formance-defining-double-entry.md`](../references/formance-defining-double-entry.md), [`blnk-double-entry-guide.md`](../references/blnk-double-entry-guide.md) | Programmatic double-entry | `postJournal` DR=CR check |
| [`freecodecamp-bank-ledger-go.md`](../references/freecodecamp-bank-ledger-go.md), [`levelup-robust-ledger-guide.md`](../references/levelup-robust-ledger-guide.md) | Relational ledger schema patterns | `coa_trans` + `coa_trans_data` header/lines |
| [`stackoverflow-double-entry-schema.md`](../references/stackoverflow-double-entry-schema.md), [`stackoverflow-relational-double-entry.md`](../references/stackoverflow-relational-double-entry.md) | Schema Q&A | Aligns with TRD §3 tables |

### 26.2 Transaction status & pending/posted

| Reference | Industry lesson | Project binding |
|-----------|-----------------|-----------------|
| [`moderntreasury-ledger-transaction-status.md`](../references/moderntreasury-ledger-transaction-status.md) | pending → posted → failed lifecycle | `coa_trans.status`; deposit ADR-006 |
| [`moderntreasury-scale-ledger-part-i.md`](../references/moderntreasury-scale-ledger-part-i.md) … **part-v** | Scale immutable ledger | NFR batch recon; read replicas (TRD §8) |
| [`blnk-transaction-lifecycle.md`](../references/blnk-transaction-lifecycle.md) | QUEUED / INFLIGHT / APPLIED / VOID | Maps to wallet `frozen` + deposit PENDING (not Blnk engine) |
| [`tigerbeetle-financial-accounting.md`](../references/tigerbeetle-financial-accounting.md), [`tigerbeetle-data-modeling.md`](../references/tigerbeetle-data-modeling.md) | High-perf transfer model | **ADR-037** — TB behind `core.accounting` |

### 26.3 Reconciliation & data quality

| Reference | Industry lesson | Project binding |
|-----------|-----------------|-----------------|
| [`stripe-dev-ledger-system.md`](../references/stripe-dev-ledger-system.md) | DQ platform; explainability %; proactive alerting | W5 report-only + ops queue; not auto-fix COA (ADR-014) |
| [`moderntreasury-what-is-reconciliation.md`](../references/moderntreasury-what-is-reconciliation.md), [`moderntreasury-transaction-reconciliation.md`](../references/moderntreasury-transaction-reconciliation.md) | Bank vs ledger match | TRD FR-10; accounting §10 |
| [`moderntreasury-recon-diaries-1.md`](../references/moderntreasury-recon-diaries-1.md) | Cash recon complications; documentation gap | Bank statement ↔ 1111/1112/1113 |
| [`moderntreasury-recon-knapsack.md`](../references/moderntreasury-recon-knapsack.md) | Matching as optimization | Batch recon job design (ops) |
| [`stripe-payment-reconciliation-101.md`](../references/stripe-payment-reconciliation-101.md), [`optimus-payment-reconciliation.md`](../references/optimus-payment-reconciliation.md) | Payment recon 101 | EOD file vs **2120** |
| [`blnk-reconciliations-overview.md`](../references/blnk-reconciliations-overview.md), [`blnk-reconciliation-strategies.md`](../references/blnk-reconciliation-strategies.md) | Recon strategies | Informative for W5/EOD |
| [`reconart-iso-20022-reconciliation.md`](../references/reconart-iso-20022-reconciliation.md), [`swift-iso-20022-chapter2.md`](../references/swift-iso-20022-chapter2.md) | ISO 20022 richer payloads | Future bank file formats; not v1 wire |

### 26.4 Ledger engines (informative — v1 is custom `coa_*`)

| Reference | Use in reviews |
|-----------|----------------|
| [`formance-how-not-to-build-ledger.md`](../references/formance-how-not-to-build-ledger.md) | Anti-patterns when rolling own ledger |
| [`formance-ledger-module.md`](../references/formance-ledger-module.md), [`formance-accounting-model.md`](../references/formance-accounting-model.md) | OSS ledger module shape |
| [`dashdevs-ledger-fintech.md`](../references/dashdevs-ledger-fintech.md), [`finlego-real-time-ledger.md`](../references/finlego-real-time-ledger.md) | Real-time ledger product patterns |
| [`mambu-gl-journal-entries.md`](../references/mambu-gl-journal-entries.md) | Banking-core GL API vocabulary |

### 26.5 Bounded context (accounting side)

| Reference | Lesson | Project |
|-----------|--------|---------|
| [`martinfowler-bounded-context.md`](../references/martinfowler-bounded-context.md) | Separate models per context | ADR-002, ADR-003 |
| [`airwallex-ddd-payments.md`](../references/airwallex-ddd-payments.md) | Payments DDD | `coa_*` vs `wallet_*` |
| [`oracle-ddd-composable-banking.md`](../references/oracle-ddd-composable-banking.md) | Composable banking | Orchestration composes domains |

### 26.6 Event sourcing (optional v1)

| Reference | Lesson | Project |
|-----------|--------|---------|
| [`microsoft-event-sourcing.md`](../references/microsoft-event-sourcing.md), [`redpanda-event-sourcing-database.md`](../references/redpanda-event-sourcing-database.md) | Event sourcing pattern | **Not required v1** — append-only lines suffice (ADR-001) |
| [`oceanobe-event-driven-ledger.md`](../references/oceanobe-event-driven-ledger.md) | Event-driven ledger arch | Kafka `JournalPosted` fan-out only |

### 26.7 Vietnam / Napas (accounting legs)

| Reference | Lesson | Project |
|-----------|--------|---------|
| [`napas-api-portal-intro.md`](../references/napas-api-portal-intro.md) | DPP product list | IBFT **3400**, **1112** — ADR-025 |
| [`adyen-napas-card-api.md`](../references/adyen-napas-card-api.md) | Napas card API shape | Partner integration reference |

---

## 27. Industry pitfalls checklist (Slope + Formance → invariants)

From [`medium-slope-payments-ledger-pitfalls.md`](../references/medium-slope-payments-ledger-pitfalls.md) and [`formance-how-not-to-build-ledger.md`](../references/formance-how-not-to-build-ledger.md). Use in design review / AC extension.

| # | Industry pitfall | How `10_core` addresses it |
|---|------------------|----------------------------|
| P1 | Inconsistent DR/CR direction | Foundation COA normal balance; one journal balances DR=CR (FR-4) |
| P2 | Lacking data integrity (dup, unbalanced) | `business_ref` + `use_case` UNIQUE; post rejects DR≠CR; ADR-005 |
| P3 | No first-class pending | Deposit `PENDING`/3100 (ADR-006); withdraw `frozen` not pending column (wallet §2.1) |
| P4 | Business logic inside ledger tables | Fee at orchestration (ADR-009); `use_case` on journal; wallet separate domain |
| P5 | Growing posting code complexity | `confirmDeposit` encapsulates deposit template (`IMPLEMENTATION.md` §7.5) |
| P6 | Mutable posted lines | ADR-001 reversal only |
| P7 | Single schema for all payment types | `use_case` enum + transit per flow (ADR-010) |
| P8 | No explainability at scale | W5 + Stripe-style DQ aspiration (ops runbook **not in repo**) |

**Closed v1:** SQL invariant CI — [ADR-031](../adr/ADR-031-sql-ledger-invariant-ci.md) (INV-01…07); Gherkin in [`acceptance.md`](./acceptance.md) Ledger invariant CI feature.

---

## 29. Accounting principles — synthesis (internet corpus)

Nguyên tắc kế toán từ GAAP/IFRS và tài liệu fintech — **không thay** COA hay DR/CR trong `core.sharedlib.md`. Dùng khi review thiết kế ledger và khi giải thích vì sao platform chọn accrual + immutable + two-phase.

### 29.1 Ba trụ cột kỹ thuật (Modern Treasury)

Từ [`moderntreasury-enforcing-immutability.md`](../references/moderntreasury-enforcing-immutability.md):

| Trụ cột | Ý nghĩa | Binding `10_core` |
|---------|---------|-------------------|
| **Double-entry** | Mọi movement có from/to; DR=CR | `coa_trans_data`; ADR-010 |
| **Auditability** | Paper trail, không sửa số đã post | ADR-001; reversal journal |
| **Immutability** | POSTED không UPDATE; pending mutable đến khi post | `coa_trans.status` PENDING→POSTED (ADR-006); tương tự bank rails |

**Tách lớp:** business object (order, payout UI) **mutable**; ledger lines **immutable** — khớp wallet snapshot + `wallet_tx` (ADR-004) tách khỏi `coa_*`.

### 29.2 Nguyên tắc GAAP cốt lõi → ledger fintech

Tổng hợp [`investopedia-accounting-principles.md`](../references/investopedia-accounting-principles.md), [`moderntreasury-gaap-accounting-rules.md`](../references/moderntreasury-gaap-accounting-rules.md):

| Nguyên tắc | Định nghĩa ngắn | Áp dụng platform |
|------------|-----------------|------------------|
| **Revenue recognition** | Ghi doanh thu khi **earned**, không nhất thiết khi nhận tiền | Deposit: phase B POSTED mới credit 2110; fee 4110 cùng kỳ (ADR-006, ADR-009) |
| **Matching** | Chi phí/phí gắn cùng kỳ với revenue liên quan | Fee orchestration tính một lần → cùng journal với gross movement |
| **Accrual basis** | Ghi nhận khi phát sinh quyền/nghĩa vụ | **ADR-036** — PENDING 3100; `frozen`; POSTED trước bank settle |
| **Conservatism / Prudence** | Thận trọng ghi lỗ, không ghi lãi khi chưa chắc | Không credit wallet khi VA unmapped; không RELEASE khi bank UNKNOWN (ADR-007, ADR-033) |
| **Consistency** | Cùng phương pháp qua các kỳ | `use_case` + template foundation cố định; scale 4 HALF_UP (ADR-028) |
| **Economic entity** | Tách entity owner vs business | `memberId` wallet vs platform COA; không lẫn 111x cá nhân |
| **Going concern / Continuity** | Giả định hoạt động liên tục | Period close chặn back-date (ADR-023); EOD batch độc lập (ADR-015) |
| **Materiality / Full disclosure** | Thông tin đủ ảnh hưởng quyết định | W5 recon report + ops queue (ADR-014); chưa có báo cáo tài chính công khai |
| **Reliability / Objectivity** | Chứng từ, không ý kiến chủ quan | `business_ref` + bank webhook/mTLS (ADR-005, ADR-022) |
| **Monetary unit** | Chỉ đơn vị tiền đo lường được | VND only v1 (ADR-019) |
| **Time period / Periodicity** | Báo cáo theo kỳ | EOD settlement; period close |

**Cash vs accrual** ([`netsuite-cash-vs-accrual.md`](../references/netsuite-cash-vs-accrual.md)): platform ledger v1 là **accrual-like** — user `available` phản ánh quyền chi tiêu sau POSTED, không phải “tiền đã vào bank”.

### 29.3 Debit / Credit và phương trình kế toán

[`investopedia-accounting-equation.md`](../references/investopedia-accounting-equation.md), [`lumen-debits-credits-rules.md`](../references/lumen-debits-credits-rules.md), [`netsuite-debits-credits.md`](../references/netsuite-debits-credits.md):

```
Assets = Liabilities + Equity
```

| Nhóm | Normal balance | Tăng | Giảm |
|------|----------------|------|------|
| Assets (111x) | Debit | DR | CR |
| Liabilities (211x–213x) | Credit | CR | DR |
| Revenue (411x) | Credit | CR | DR |
| Expense (51xx) | Debit | DR | CR |

**Duality:** mỗi `coa_trans` — tổng DR = tổng CR ([`lumen-debits-credits-rules.md`](../references/lumen-debits-credits-rules.md)). Foundation §5 invariant 1: bank assets = wallet liabilities.

### 29.4 Expense recognition & fee

[`becker-expense-recognition.md`](../references/becker-expense-recognition.md): chi phí ghi khi **incurred** và **matched** với doanh thu liên quan. Platform: phí user → 4110 CR cùng journal với movement; orchestration là single source (ADR-009).

### 29.5 Tham chiếu corpus mới

| File | Chủ đề |
|------|--------|
| `investopedia-accounting-principles.md` | 12+ core principles, GAAP vs IFRS |
| `investopedia-gaap.md` | GAAP overview |
| `investopedia-accrual-accounting.md` | Accrual basis |
| `investopedia-accounting-equation.md` | A = L + E |
| `moderntreasury-enforcing-immutability.md` | Immutability + pending/posted |
| `moderntreasury-gaap-accounting-rules.md` | GAAP 10 principles for engineers |
| `netsuite-debits-credits.md` | DR/CR rules |
| `netsuite-cash-vs-accrual.md` | Cash vs accrual |
| `lumen-debits-credits-rules.md` | Normal balance cheat sheet |
| `becker-expense-recognition.md` | Matching / expense timing |

Index: [`references/README.md`](../references/README.md) § Accounting principles.

### 29.6 VAS / IFRS Việt Nam → platform (informative)

Corpus VN (lần 8): [`references/README.md`](../references/README.md) § VAS/IFRS Vietnam.

| Nguồn | Nội dung |
|-------|----------|
| [`vn-luat-ke-toan-2015-dieu-5-7.md`](../references/vn-luat-ke-toan-2015-dieu-5-7.md) | Điều 5–7: yêu cầu + nguyên tắc (giá gốc, nhất quán, thận trọng, **bản chất > hình thức**) |
| [`hoiketoanhcm-nguyen-tac-luat-ke-toan-2015.md`](../references/hoiketoanhcm-nguyen-tac-luat-ke-toan-2015.md) | Phân tích Điều 6 Luật KT 2015 |
| [`acclime-ifrs-vas-vietnam.md`](../references/acclime-ifrs-vas-vietnam.md) | VAS rule-based vs IFRS principles-based; roadmap QĐ 345/2020 |
| [`incorp-ifrs-vas-vietnam.md`](../references/incorp-ifrs-vas-vietnam.md) | IFRS vs VAS differences |

| VN / IFRS concept | Platform v1 (fintech ledger, không phải BCTC công ty) |
|-------------------|--------------------------------------------------------|
| Điều 6.6 bản chất > hình thức | `use_case` + DR/CR template theo nghiệp vụ, không theo tên API |
| Điều 6.5 thận trọng | ADR-007, ADR-033, ADR-034 |
| VAS dồn tích / IFRS accrual | **ADR-036** — POSTED ≠ bank cash |
| VND Điều 3 | ADR-019 |
| TT200/99 hệ thống TK | COA foundation §6 — **không** copy bảng TK DN đầy đủ |
| IFRS fair value | Out of scope v1 (historical cost / posted amount) |

**Chưa scrape:** TT200/99 full text (login); MOF portal (thin); Medium accrual ledger (504).

---

## 30. Related docs (Part II)

| Need | Read |
|------|------|
| DR/CR amounts | [`core.sharedlib.md`](../core.sharedlib.md) §8–16 |
| ADR AC/TC | [`adr/README.md`](../adr/README.md) — incl. **ADR-036** accrual basis |
| Full ref index | [`references/README.md`](../references/README.md) — 124 files |
| Accounting principles refs | §29.5 above |
| Saga narrative | [`core.business-processes.md`](../core.business-processes.md) §13–17 |
| Orchestration steps | [`orchestration.md`](./orchestration.md) §3 |
| Wallet handoff | [`wallet.md`](./wallet.md) §7 |
| Gherkin conformance | [`acceptance.md`](./acceptance.md) Part IV |
