# Data model — two schemas, design level

**Scope:** conceptual + logical data model for `wallet` and `accounting` schemas. **Binding DDL (source of truth for exact SQL):** [`../../spec/implementation.md`](../../spec/implementation.md) §3–4. This doc is *design* — it explains shape, relationships, indexes, and invariants; it does not restate column types verbatim.

**Read first:** [`boundaries.md`](./boundaries.md) (why split, hard rules) · **Terms:** [`../../spec/terminology.md`](../../spec/terminology.md).

---

## 1. Two schemas, one instance (v1)

One PostgreSQL instance, two schemas: **`wallet`** and **`accounting`** ([ADR-003](../../adr/ADR-003-dual-schema-single-postgres.md)). Split to two DBs later = datasource URL change only.

**Hard rules at the data layer** ([`boundaries.md`](./boundaries.md)):
- No cross-schema **FK** — `wallet_tx.coa_trans_id` is correlation only, never an enforced FK.
- No cross-schema **JOIN** — a single query never touches both `wallet_*` and `coa_*`.
- Aggregate liability lives **only** in accounting (COA 2110/2120/2130); wallet holds per-member slices.

```
PostgreSQL instance
├── schema wallet        ← customer hot path (high RPS, single-row reads)
│   ├── wallet_pocket_def     (catalog of pocket kinds — reference data)
│   ├── wallet                (a pocket = one wallet row, refs a def)
│   ├── wallet_balance        (authoritative spendable snapshot)
│   ├── wallet_tx             (append-only movement log)
│   └── wallet_lane_rollup    (non-authoritative async aggregate)
└── schema accounting    ← back-office / system-of-record
    ├── coa_account
    ├── coa_trans             (journal — balanced DR/CR header)
    └── coa_trans_data        (journal entry — one DR or CR line)
```

---

## 2. ERD

```mermaid
erDiagram
    %% ===== schema: wallet =====
    WALLET_POCKET_DEF ||--o{ WALLET : "pocket_code (template, ADR-040)"
    WALLET ||--|| WALLET_BALANCE : "1:1 (wallet_id PK=FK)"
    WALLET ||--o{ WALLET_TX : "1:N movements"
    WALLET_LANE_ROLLUP }o..|| WALLET : "derived per wallet_type (async, no FK)"

    WALLET_POCKET_DEF {
        varchar code PK "DEFAULT|SPENDING|SAVINGS|GOAL"
        varchar name
        enum wallet_type "USER (v1)"
        boolean is_default
        boolean multi_allowed
        boolean active
    }
    WALLET {
        bigserial id PK
        bigint member_id
        enum wallet_type "USER|MERCHANT|PARTNER"
        char currency "VND"
        varchar pocket_code FK "-> wallet_pocket_def"
        varchar label "user display name"
        enum status "ACTIVE|LOCKED|CLOSED"
    }
    WALLET_BALANCE {
        bigint wallet_id PK_FK
        numeric available "CHECK >= 0"
        numeric frozen "CHECK >= 0"
        bigint version "optimistic lock"
    }
    WALLET_TX {
        bigserial id PK
        bigint wallet_id FK
        varchar tx_type
        enum direction "CREDIT|DEBIT|FREEZE|UNFREEZE"
        numeric amount "CHECK > 0"
        numeric available_after "after-snapshot"
        numeric frozen_after "after-snapshot"
        varchar business_ref
        bigint coa_trans_id "correlation only, NO FK"
    }
    WALLET_LANE_ROLLUP {
        enum wallet_type PK
        numeric sum_available
        numeric sum_frozen
        bigint wallet_count
        timestamptz computed_at
    }

    %% ===== schema: accounting =====
    COA_ACCOUNT ||--o{ COA_TRANS_DATA : "account_code"
    COA_TRANS ||--o{ COA_TRANS_DATA : "1:N lines"

    COA_ACCOUNT {
        varchar code PK "1111, 2110, 3100, ..."
        varchar name
        varchar account_type "ASSET|LIABILITY|TRANSIT|REVENUE|EXPENSE|EQUITY"
        boolean active
    }
    COA_TRANS {
        bigserial id PK
        varchar reference_id
        varchar use_case
        enum status "PENDING|POSTED|REVERSED"
        date posting_date
    }
    COA_TRANS_DATA {
        bigserial id PK
        bigint coa_trans_id FK
        varchar account_code FK
        enum side "DEBIT|CREDIT"
        numeric amount "CHECK > 0"
    }
```

> The dashed link `WALLET_LANE_ROLLUP ..` and the *absence* of any line between the wallet and accounting clusters are intentional — there is **no** referential path across schemas.

---

## 3. Tables by purpose

### 3.1 `wallet` schema — "how much can this member spend now?"

| Table | Role | Cardinality / key | Notes |
|-------|------|-------------------|-------|
| `wallet_pocket_def` | Catalog of pocket kinds ("ngăn ví") — reference/config | PK `code` | A wallet is created **referencing** a def; `multi_allowed` gates >1 pocket of a def ([ADR-040](../../adr/ADR-040-user-multi-pocket-wallets.md)) |
| `wallet` | One **pocket** = one wallet row | PK `id`; UNIQUE `(member_id, wallet_type, currency, label)`; partial UNIQUE `(member_id, wallet_type, currency) WHERE type<>USER` | USER may hold many pockets (label-distinguished, refs `pocket_code`); MERCHANT/PARTNER single. `wallet_type` → lane → COA control ([ADR-020](../../adr/ADR-020-wallet-lanes-coa-control-mapping.md), [ADR-040](../../adr/ADR-040-user-multi-pocket-wallets.md)) |
| `wallet_balance` | **Authoritative** spendable snapshot | PK `wallet_id` = 1:1 with `wallet` | `available` + `frozen` read directly on hot path ([ADR-004](../../adr/ADR-004-wallet-balance-snapshot.md)); `version` for optimistic lock |
| `wallet_tx` | **Append-only** movement log | PK `id`; UNIQUE `(wallet_id, business_ref, tx_type)` | Each row snapshots `available_after`/`frozen_after` → reconstructible + as-of query |
| `wallet_lane_rollup` | **Non-authoritative** async aggregate per lane | PK `wallet_type` | Fed by periodic job, **never** by the write TX ([ADR-039](../../adr/ADR-039-no-synchronous-wallet-aggregate-row.md)); consumed by monitors ([ADR-032](../../adr/ADR-032-wallet-balance-monitoring.md) MON-04) |

> **Member is separate from balance.** Member **identity** is not in the wallet schema at all — it lives in the auth/identity domain ([ADR-011](../../adr/ADR-011-auth-identity-jwt-subject.md)); `wallet.member_id` is only a reference. **`wallet_balance` holds balance numbers only** (`available`, `frozen`, `version`) keyed by `wallet_id` — **no `member_id`, no member attributes** on it. This keeps the hot row minimal (single-row read/lock, [ADR-004](../../adr/ADR-004-wallet-balance-snapshot.md)) and avoids coupling member profile changes to the balance write path. To go member → balances: `wallet.member_id` → `wallet.id` → `wallet_balance`.

### 3.2 `accounting` schema — "what happened in the ledger?"

| Table | Role | Cardinality / key | Notes |
|-------|------|-------------------|-------|
| `coa_account` | Chart of Accounts catalog | PK `code` | Seeded before go-live ([`../../spec/foundation.md`](../../spec/foundation.md) §6–7); a flow posting to a missing code fails FK |
| `coa_trans` | **Journal** — balanced DR/CR header | PK `id`; UNIQUE `(reference_id, use_case)` | `status` PENDING→POSTED (deposit 2-phase, [ADR-006](../../adr/ADR-006-two-phase-deposit.md)); immutable after POSTED ([ADR-001](../../adr/ADR-001-immutable-ledger.md)) |
| `coa_trans_data` | **Journal entry** — one DR or CR line | PK `id`; FK `coa_trans_id`, FK `account_code` | `SUM(DR)=SUM(CR)` per journal; no UPDATE after POSTED |

---

## 4. Indexes (design intent)

| Index | Table | Serves |
|-------|-------|--------|
| `idx_wallet_member` | `wallet (member_id)` | Look up a member's wallets |
| `idx_wallet_tx_wallet_created` `(wallet_id, created_at DESC, id DESC)` | `wallet_tx` | Latest movement **and** as-of balance ≤ T with deterministic tie-break ([ADR-004](../../adr/ADR-004-wallet-balance-snapshot.md) point 7) |
| `idx_wallet_tx_business_ref` | `wallet_tx (business_ref)` | Idempotency / support lookup by ref |
| `uq_wallet_tx_idempotency` | `wallet_tx (wallet_id, business_ref, tx_type)` | Idempotency triple ([ADR-005](../../adr/ADR-005-idempotency-key-strategy.md)) — also the under-lock recheck backing |
| `idx_coa_trans_data_journal` | `coa_trans_data (coa_trans_id)` | Fetch all lines of a journal; balance build per account |
| `uq_coa_trans_reference` | `coa_trans (reference_id, use_case)` | Journal idempotency |

---

## 5. Invariants (enforced in code / CI, not all by constraints)

### Wallet (W1–W5)
| # | Invariant | Where |
|---|-----------|-------|
| W1 | `available >= 0` and `frozen >= 0` | CHECK constraints |
| W2 | Every `wallet_balance` change ⇒ exactly one `wallet_tx` in the **same** TX | code + [ADR-004](../../adr/ADR-004-wallet-balance-snapshot.md); CI INV-05 |
| W3 | Idempotency recheck **under** the row lock | `WalletCommandServiceImpl`; [ADR-005](../../adr/ADR-005-idempotency-key-strategy.md) |
| W4 | No UPDATE of historical `wallet_tx`; corrections = new movement | [ADR-004](../../adr/ADR-004-wallet-balance-snapshot.md) |
| W5 | `SUM(wallet_balance)` per lane vs COA control = report-only drift | [ADR-014](../../adr/ADR-014-reconciliation-w5-report-only.md) (two queries, no JOIN) |

### Accounting
| # | Invariant | Where |
|---|-----------|-------|
| A1 | `SUM(DR)=SUM(CR)` per POSTED journal | CI INV-01 ([ADR-031](../../adr/ADR-031-sql-ledger-invariant-ci.md)) |
| A2 | Transit 3100–3820 net **0** per completed use case | CI INV-03 ([ADR-010](../../adr/ADR-010-transit-accounts-net-zero.md)) |
| A3 | No UPDATE/DELETE on `coa_trans_data` of POSTED parent | CI INV-04 ([ADR-001](../../adr/ADR-001-immutable-ledger.md)) |
| A4 | `(1111+1112+1113) = (2110+2120+2130)` (tolerance for async lag) | nightly INV-06 |

---

## 6. Performance shape (why the model is split this way)

| Path | Cost | Reference |
|------|------|-----------|
| Balance read (customer) | single-row `wallet_balance` PK lookup — **max RPS**, no derive | [ADR-004](../../adr/ADR-004-wallet-balance-snapshot.md), `trd/wallet.md` §9.0 |
| Mutation | locks **only** the one `wallet_id` row — different wallets parallel; no global aggregate row | [ADR-039](../../adr/ADR-039-no-synchronous-wallet-aggregate-row.md) |
| Accounting reads | back-office / reporting, not the per-customer hot path | `trd/accounting.md` §4 positioning |

---

## 7. Not in v1 (extension points, same prefix)

`coa_period`, `coa_audit_log` (accounting period/audit) · outbox table (messaging, [ADR-013](../../adr/ADR-013-outbox-at-least-once-messaging.md)) · any per-txn QR wallet (default off, [ADR-016](../../adr/ADR-016-qr-pos-default-no-per-txn-wallet.md)). Add with the owning schema's prefix; never introduce a cross-schema FK.

---

## Next in flow

| Step | Doc |
|------|-----|
| Binding DDL (exact SQL, Flyway) | [`../../spec/implementation.md`](../../spec/implementation.md) §3–4 |
| COA codes + DR/CR per use case | [`../../spec/foundation.md`](../../spec/foundation.md) §6–16 |
| Wallet behavior | [`../../design-v2/wallet.md`](../../design-v2/wallet.md) |
| Accounting behavior | [`../../design-v2/accounting.md`](../../design-v2/accounting.md) |
