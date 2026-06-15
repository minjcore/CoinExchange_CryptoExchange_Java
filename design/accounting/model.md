# Accounting — data model

Schema: **`accounting`**. Prefix: **`coa_`**.

## Tables

### `coa_account`

| Column | Notes |
|--------|-------|
| `code` | PK, e.g. `2110` |
| `name` | |
| `account_type` | ASSET, LIABILITY, TRANSIT, REVENUE, EXPENSE, EQUITY |
| `active` | |

### `coa_trans` (journal)

| Column | Notes |
|--------|-------|
| `id` | PK |
| `reference_id` | = `businessRef`, **UNIQUE** with `use_case` |
| `use_case` | DEPOSIT, PAYMENT, WITHDRAW, … |
| `status` | PENDING → POSTED → REVERSED |
| `posting_date` | |
| `posted_at` | set on POSTED |

### `coa_trans_data` (journal entry)

| Column | Notes |
|--------|-------|
| `id` | PK |
| `coa_trans_id` | FK → journal |
| `account_code` | FK → `coa_account` |
| `side` | DEBIT / CREDIT |
| `amount` | > 0, scale 4 |

## Journal lifecycle

```
createJournal(reference_id, use_case)
  → addLines(DR/CR rows)
  → postJournal()     # validates balance, sets POSTED
```

**Deposit exception:** two-phase via `confirmDeposit(coaTransId, fee)` — phase A lines (PENDING), phase B appends fee/net lines and POSTED in one accounting-owned method.

## Service API (domain)

| Method | Purpose |
|--------|---------|
| `createJournal` | Idempotent on `(reference_id, use_case)` |
| `addLines` | Append journal_entry rows |
| `postJournal` | Balance check + POSTED |
| `confirmDeposit` | Deposit phase B (locked) |
| `reverseJournal` | New reversing journal, link original |
| `getBalance(accountCode, asOf)` | Derived from POSTED lines |

## Errors

| Code | When |
|------|------|
| `ACCOUNTING_UNBALANCED_JOURNAL` | sum(DR) ≠ sum(CR) |
| `ACCOUNTING_JOURNAL_NOT_FOUND` | |
| `ACCOUNTING_DUPLICATE_JOURNAL` | conflicting replay |

**S2 OpenAPI binding:** [`surface-map.md`](./surface-map.md) — map `api-internal.yaml` ↔ service methods ↔ `coa_*`.
