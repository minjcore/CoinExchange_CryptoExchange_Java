# Terminology

Platform naming differs from TRD/API names. **Do not swap journal and journal_entry.**

| Platform term | Table | Meaning |
|---------------|-------|---------|
| COA account | `coa_account` | Chart of accounts row |
| **Journal** | `coa_trans` | One balanced posting header (`use_case`, `business_ref`, `status`) |
| **Journal entry** | `coa_trans_data` | One DR or CR line |
| Ledger entry | `coa_trans_data` when POSTED | Immutable after post |
| Business key | `business_ref` | Idempotency key (= `businessRef` on wire) |

| TRD / API | Platform |
|-----------|----------|
| `journal_entries` | **Journal** → `coa_trans` |
| `journal_lines` | **Journal entry** → `coa_trans_data` |
| `reference_id` | `business_ref` |
| `accounts` | `coa_account` |

**Wallet:** `wallet` = instance (member + lane + currency). `wallet_balance` = snapshot. `wallet_tx` = append-only movement.

**Lanes:** `USER` → control 2110 · `MERCHANT` → 2120 · `PARTNER` → 2130 (reconciliation mapping only, no FK).
