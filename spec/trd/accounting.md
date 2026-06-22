# Technical Requirements Document (TRD)

**Accounting Service (Backend)**

| Field | Value |
|-------|-------|
| Version | 1.0 |
| Status | Draft |
| Author | Engineering |
| Date | June 2026 |

**Related:** [`core.sharedlib.md`](./core.sharedlib.md), [`integration-surfaces.md`](./integration-surfaces.md), [`wallet.md`](./wallet.md), [`spec/contracts/open-api/accounting-internal.yaml`](../contracts/open-api/accounting-internal.yaml), [`spec/contracts/async-api/core-events.yaml`](../contracts/async-api/core-events.yaml).  
**ADR:** [ADR-001](./adr/ADR-001-immutable-ledger.md), [ADR-002](./adr/ADR-002-core-foundation-shared-library.md)

---

## 1. Overview

### 1.1 Purpose

The Accounting Service provides a centralized financial ledger system for recording, tracking, reconciling, and reporting financial transactions across products and business units.

The service acts as the source of truth for all accounting entries and ensures:

- Double-entry bookkeeping
- Auditability
- Financial accuracy
- Historical traceability
- Reporting consistency

### 1.2 Goals

**Business Goals**

- Maintain accurate financial records
- Support financial reporting
- Enable reconciliation workflows
- Support multiple currencies
- Support accounting periods and closing

**Technical Goals**

- Immutable ledger
- Strong consistency for postings
- Complete audit trail
- Horizontal scalability
- High availability
- Idempotent transaction processing

---

## 2. Scope

### In Scope

- Chart of accounts
- Journal entries
- Double-entry posting
- Account balances
- Period management
- Reconciliation
- Audit logs
- Reporting APIs
- Multi-currency support

### Out of Scope (v1)

- Tax calculation
- Payroll
- Billing
- Invoice generation
- Budget planning
- Financial forecasting

---

## 3. Functional Requirements

> **Platform naming ([§13](#13-alignment-with-corefoundation-gtelpay)):** TRD sections below use API/industry table names. In `core.accounting`: **`coa_trans` = journal** (TRD `journal_entries`); **`coa_trans_data` = journal_entry** (TRD `journal_lines`). Do not read TRD “journal entry” as `coa_trans_data`.

### FR-1 Chart of Accounts

The system shall support creation and management of accounts.

**Account Types**

- Asset
- Liability
- Equity
- Revenue
- Expense

**Account Fields**

| Field | Type |
|-------|------|
| account_id | UUID |
| code | String |
| name | String |
| type | Enum |
| parent_account_id | UUID |
| currency | String |
| active | Boolean |
| created_at | Timestamp |

**Requirements**

- Account code must be unique
- Hierarchical accounts supported
- Account deactivation supported
- Account deletion prohibited after use

### FR-2 Journal Entries

> TRD resource `journal_entries` = platform **journal** (`coa_trans`) — one balanced business posting (header).

Represents a business event (deposit, payment, transfer, …).

**Fields** (TRD / API; platform → `coa_trans`)

| Field | Type |
|-------|------|
| journal_entry_id | UUID |
| reference_id | String |
| description | String |
| posting_date | Date |
| status | Enum |
| created_by | User |
| created_at | Timestamp |

**Status**

- Draft
- Posted
- Reversed

### FR-3 Journal Lines

> TRD resource `journal_lines` = platform **journal_entry** (`coa_trans_data`) — one DR or CR line. FK `journal_entry_id` → parent journal (`coa_trans_id`).

Each journal (`journal_entries` / `coa_trans`) contains multiple lines.

**Fields** (TRD / API; platform → `coa_trans_data`)

| Field | Type |
|-------|------|
| line_id | UUID |
| journal_entry_id | UUID |
| account_id | UUID |
| debit_amount | Decimal |
| credit_amount | Decimal |
| currency | String |

**Validation**

- Debit ≥ 0
- Credit ≥ 0
- One side must be zero

### FR-4 Double Entry Accounting

The service must enforce:

```
SUM(debits) = SUM(credits)
```

for every journal (`journal_entries` / `coa_trans`) — sum of its journal_entry lines.

Posting must fail if the equation does not balance.

### FR-5 Ledger Posting

Posting creates immutable ledger records.

**Requirements**

- Atomic operation
- ACID transaction
- No partial posting
- Ledger entries immutable

**Ledger Entry Fields**

| Field | Type |
|-------|------|
| ledger_entry_id | UUID |
| journal_entry_id | UUID |
| account_id | UUID |
| amount | Decimal |
| direction | Debit/Credit |
| posting_timestamp | Timestamp |

### FR-6 Reversals

Journal entries cannot be edited after posting.

Instead:

- Create reversing entry
- Link to original entry
- Maintain audit chain

### FR-7 Account Balances

System shall provide:

**Current Balance**

- Real-time balance calculation.

**Historical Balance**

- Balance as-of specific timestamp.

**Trial Balance**

- Balances by account.

### FR-8 Accounting Periods

**Period States**

- Open
- Closed
- Locked

**Rules**

| State | Rule |
|-------|------|
| Open | Posting allowed |
| Closed | Posting prohibited |
| Locked | Administrative changes prohibited |

### FR-9 Multi-Currency Support

**Requirements**

Store:

- Transaction currency
- Functional currency
- Exchange rate

**Example**

```json
{
  "transaction_currency": "EUR",
  "transaction_amount": 100,
  "functional_currency": "USD",
  "exchange_rate": 1.08
}
```

**Historical Rates**

Rates used during posting must be immutable.

### FR-10 Reconciliation

Support reconciliation of:

- Bank transactions
- Payment processor settlements
- External accounting systems

**States**

- Unmatched
- Matched
- Reconciled

### FR-11 Audit Logging

Every change must generate audit records.

**Track**

- User
- Timestamp
- Before state
- After state
- Action

**Actions**

- Create
- Update
- Post
- Reverse
- Close period

---

## 4. Non-Functional Requirements

> **Positioning — `core.accounting` is a back-office / system-of-record domain, not the customer hot path.** Customer-facing reads ("how much can I spend now?") at peak RPS are served by `core.wallet`'s single-row snapshot ([ADR-004](./adr/ADR-004-wallet-balance-snapshot.md), `spec/trd/wallet.md` §9). Accounting's high read figures below are for **back-office / reporting / reconciliation** consumers (ops, audit, EOD, W5), not per-customer balance lookups on the payment path. "Balance Queries" here = COA control / account-code aggregates (2110/2120/2130, reports), derived from POSTED `coa_trans_data` — not the per-member spendable balance.

### NFR-1 Availability

Target: **99.95%** monthly uptime.

### NFR-2 Consistency

Posting operations require:

- Strong consistency
- No eventual consistency for ledger writes

### NFR-3 Durability

Once posted:

- No data loss accepted
- Transactions must survive node failures

### NFR-4 Scalability

| Metric | Target | Consumer |
|--------|--------|----------|
| Journal Posts | 2,000/sec | orchestration (write path) |
| Balance Reads (COA aggregate / account-code) | 20,000/sec | back-office, reporting, recon — **not** per-customer hot path |
| Reporting Reads | 5,000/sec | back-office / audit |

Per-customer spendable-balance RPS is a **wallet** NFR (`spec/trd/wallet.md` §9), not accounting.

### NFR-5 Latency

| Operation | Target |
|-----------|--------|
| Posting | P95 < 300ms |
| Balance Queries | P95 < 100ms |
| Reporting Queries | P95 < 2s |

### NFR-6 Security

**Authentication**

- OAuth2
- OIDC
- Service-to-service JWT

**Authorization**

- RBAC

**Roles**

- Accountant
- Auditor
- Admin
- ReadOnly

---

## 5. Data Model

### Accounts

```
accounts
--------
id
code
name
type
parent_id
currency
active
created_at
```

### Journal Entries (TRD) → journal `coa_trans`

```
journal_entries          -- platform: coa_trans (journal)
---------------
id                       -- coa_trans.id
reference_id             -- same value as S1 businessRef / wallet_tx.business_ref
description
status                   -- PENDING | POSTED | …
posting_date
created_at
```

### Journal Lines (TRD) → journal_entry `coa_trans_data`

```
journal_lines            -- platform: coa_trans_data (journal_entry)
-------------
id
journal_entry_id         -- coa_trans_id (FK → journal)
account_id               -- coa_account / account_code
debit_amount
credit_amount
currency
```

### Ledger Entries

```
ledger_entries
--------------
id
journal_entry_id
account_id
amount
direction
currency
posting_timestamp
```

### Accounting Periods

```
accounting_periods
------------------
id
period_start
period_end
status
```

---

## 6. API Requirements

> REST paths use TRD names. `POST /v1/journal-entries` creates a **journal** (`coa_trans`); `.../lines` adds **journal_entry** rows (`coa_trans_data`).

### Create Journal Entry

`POST /v1/journal-entries`

**Request:**

```json
{
  "reference_id": "PAYMENT-123",
  "description": "Customer payment"
}
```

### Add Journal Line

`POST /v1/journal-entries/{id}/lines`

### Post Entry

`POST /v1/journal-entries/{id}/post`

**Validation:**

- Entry balanced
- Period open
- Accounts active

### Reverse Entry

`POST /v1/journal-entries/{id}/reverse`

### Get Account Balance

`GET /v1/accounts/{id}/balance`

### Trial Balance

`GET /v1/reports/trial-balance`

---

## 7. Event Architecture

Publish domain events after successful posting.

**Events**

- JournalPosted
- JournalReversed
- AccountCreated
- PeriodClosed
- PeriodOpened
- ReconciliationCompleted

**Example**

```json
{
  "event_type": "JournalPosted",
  "journal_entry_id": "uuid",
  "posted_at": "timestamp"
}
```

---

## 8. Storage Requirements

### Ledger engine (postings)

**Chosen:** [TigerBeetle](https://tigerbeetle.com) behind `core.accounting` ([ADR-037](../adr/ADR-037-tigerbeetle-ledger-backing-store.md)).

- Hot path: accounts + transfers (immutable, pending/post/void)
- Orchestration / workers call **accounting API** only — not TB directly
- VND minor units (scale 4) as u128 per [ADR-028](../adr/ADR-028-money-scale-four-half-up.md)

### Control plane & read models (PostgreSQL)

**PostgreSQL** (schema `accounting`) for:

- COA master (`coa_account`), periods, reconciliation, audit log
- Reporting read-model projected from TB transfers (trial balance, GL — §9)
- ACID for metadata; **not** the live posting store

**Requirements**

- Point-in-time recovery on Postgres metadata
- TB cluster DR per ops runbook (separate from wallet PITR)

### Read Models

**Optional**

- PostgreSQL replicas for reports
- Projector: TB → `coa_trans` / aggregate tables for Metabase

### Cache

**Optional:** Redis — metadata only. **Never cache posting transactions.**

---

## 9. Reporting Requirements

Generate:

- General Ledger
- Trial Balance
- Balance Sheet
- Income Statement
- Account Activity
- Period Activity

Reports must support:

- Date ranges
- Currency filters
- Account filters
- Export (CSV, XLSX)

---

## 10. Observability

**Metrics**

- Journal posting rate
- Posting failures
- Reconciliation lag
- Balance query latency
- Event publishing latency

**Logging**

- Structured JSON logs
- Correlation IDs
- Request tracing

**Tracing**

- OpenTelemetry

---

## 11. Disaster Recovery

| Metric | Target |
|--------|--------|
| RPO | < 5 minutes |
| RTO | < 30 minutes |

**Requirements**

- Daily backups
- PITR enabled
- Multi-region backup copies

---

## 12. Recommended Architecture

For a modern fintech-grade accounting system:

```
                +------------------+
                | app-orchestration |
                +---------+--------+
                          | accounting-internal HTTP
                          v
                +------------------+
                | core.accounting  |
                | (domain + wrap)  |
                +----+---------+---+
                     |         |
                     v         v
            +-------------+  +------------------+
            | TigerBeetle |  | PostgreSQL       |
            | postings    |  | COA · periods ·  |
            +-------------+  | read-model       |
                     |       +------------------+
                     v
            +------------------+
            | Event Bus (S3)   |
            +------------------+
```

**ADR-037:** TB = posting engine; Postgres = control plane + reports. Wire contract unchanged (`accounting-internal.yaml`).

### Design recommendation

Model the system as an **immutable ledger from day one**. Avoid storing balances as the source of truth. Store only postings in the ledger and derive balances from them (optionally with snapshots for performance). This simplifies audits, reconciliation, corrections, and regulatory compliance.

**Recorded as:** [ADR-001 — Immutable ledger as source of truth](./adr/ADR-001-immutable-ledger.md)

---

## 13. Alignment with `core.sharedlib` (GtelPay)

This TRD uses **industry API names** (`journal_entries`, `journal_lines`). **`core.accounting` semantics:**

| Platform term | Meaning | Platform table / field |
|---------------|---------|-------------------------|
| **Journal** | One balanced business posting (header) | **`coa_trans`** |
| **Journal entry** | One debit or credit line | **`coa_trans_data`** |
| Ledger entry | Posted journal entry (immutable) | **`coa_trans_data`** when `coa_trans.status = POSTED` |
| Chart of accounts row | COA master | **`coa_account`** |
| Reference id | External idempotency key | **`reference_id`** on `coa_trans` (= S1 `businessRef`) |

**Naming:** Tables use prefix **`coa_`**. Semantic map: **`coa_trans` = journal**, **`coa_trans_data` = journal_entry** (not the other way around). See [`core.sharedlib.md` §2.2](./core.sharedlib.md) and [ADR-001](./adr/ADR-001-immutable-ledger.md).

TRD FR/API names vs platform:

| TRD (this document) | Platform term | `core.sharedlib` / GtelPay table |
|---------------------|---------------|-----------------------------------|
| `journal_entries` | **Journal** | `coa_trans` (`use_case`, `reference_id`, `status`, time) |
| `journal_lines` | **Journal entry** | `coa_trans_data` (`coa_trans_id`, `account_code`, DR/CR, amount) |
| `accounts` (TRD) | `coa_account` |
| Draft → Posted | e.g. deposit **PENDING** → **POSTED** (two-phase with transit **3100**) |
| `reference_id` | `coa_trans.reference_id` (idempotent key, e.g. bank ref; = S1 `businessRef`) |
| Reversal | Additional `coa_trans` with reversed lines; no edit after close |
| Wallet credit | **Out of scope** for accounting service — event/API to `core.wallet` after POSTED ([`core.sharedlib.md` §3](./core.sharedlib.md)) |
| COA account types | Asset, Liability, Transit, Revenue, Expense, Equity (§6 COA in foundation) |

**Invariant (foundation):** every use case leaves transit accounts at **zero** when complete; `sum(DR) = sum(CR)` per `coa_trans`.

**Idempotency (foundation):** duplicate `reference_id` / webhook must not double-post.

---

## Conclusion

The Accounting Service TRD defines domain model, ledger architecture, consistency, APIs, integrations, compliance-oriented auditability, and operational targets before implementation. Implement `core.accounting` against this TRD while honoring boundaries and use cases documented in `core.sharedlib.md`.
