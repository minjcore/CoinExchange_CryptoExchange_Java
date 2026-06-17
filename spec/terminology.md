# Terminology — platform vs TRD / REST

Quick lookup when reviewing. Detail: [`core.foundation.md`](./core.foundation.md) Part II §2.2, [`core.accounting.trd.md`](./core.accounting.trd.md) §13, [ADR-001](./adr/ADR-001-immutable-ledger.md).

## Platform (DB / code)

| Term | Table | Key fields |
|------|-------|------------|
| COA account | `coa_account` | `code`, `type`, … |
| **Journal** | `coa_trans` | `use_case`, `reference_id`, `status` |
| **Journal entry** | `coa_trans_data` | `coa_trans_id`, `account_code`, DR/CR, `amount` |

**Rules**

- `coa_trans` = **journal** (one balanced DR/CR header).
- `coa_trans_data` = **journal_entry** (one line) — not the header.
- Balances: derived from POSTED `coa_trans_data`; cache is optional.
- Wallet: only `core.wallet`; member wallet credit after `coa_trans.status = POSTED` (deposit), or sync orchestration for pay/transfer.

## TRD / REST (industry names)

| TRD / API | Platform term | Platform table |
|-----------|---------------|----------------|
| `accounts` | COA account | `coa_account` |
| `journal_entries` | **Journal** | `coa_trans` |
| `journal_lines` | **Journal entry** | `coa_trans_data` |
| `journal_lines.journal_entry_id` | FK → journal | `coa_trans_id` |
| `reference_id` | Business idempotency key (S2 / accounting) | `coa_trans.reference_id` (= same value as S1 `businessRef`, `wallet_tx.business_ref`) |
| `ledger_entries` | Posted journal entry | `coa_trans_data` (POSTED) |

## Common mistakes

| Wrong | Right |
|-------|-------|
| “journal entry = header” / `coa_trans` = journal entry | **Journal** = `coa_trans` |
| “journal line” = `coa_trans` | **Journal entry** = `coa_trans_data` |
| TRD `journal_entry_id` on a line = platform journal id | Line FK → **`coa_trans_id`** |
| Edit POSTED `coa_trans_data` | New journal + reversing lines |

## Table prefixes

| Area | Prefix | Docs |
|------|--------|------|
| Accounting | **`coa_`** | [`core.foundation.md`](./core.foundation.md) Part II, [`core.accounting.trd.md`](./core.accounting.trd.md) |
| Wallet | **`wallet_`** | [`core.wallet.md`](./core.wallet.md) |

COA **2110 / 2120 / 2130** = aggregate liabilities (accounting). **`wallet_balance`** = per member (wallet).

## Integration

Contract index: [`integration-surfaces.md`](./integration-surfaces.md). Wire: `spec/contracts/open-api/`, `spec/contracts/async-api/`.
