# ADR-001: Immutable ledger as source of truth

| Field | Value |
|-------|-------|
| Status | Accepted |
| Date | 2026-06-03 |
| Deciders | Engineering |
| Related | [`core.accounting.trd.md`](../core.accounting.trd.md), [`core.foundation.md`](../core.foundation.md) |

---

## Context

The Accounting Service must support double-entry bookkeeping, audit trails, reconciliation, period close, and high-volume posting (see TRD NFR-2, NFR-3, FR-5, FR-6).

Two common approaches:

1. **Mutable balances** — update an `account_balance` row on each post (fast reads, hard to audit, risky under concurrency and partial failures).
2. **Immutable ledger** — append-only postings; balances are **derived** (or cached as non-authoritative snapshots).

GtelPay fund flow models a **journal** (`coa_trans`) with append-only **journal_entry** rows (`coa_trans_data`), status transitions (e.g. deposit PENDING → POSTED), and reversals via new journals — not in-place edits.

Wallet user balances (`core.wallet`) are a separate domain and must not be the accounting ledger’s source of truth.

---

## Decision

We adopt an **immutable ledger** as the accounting source of truth from day one.

1. **Authoritative data** — Posted journal data is stored as immutable records:
   - TRD: `ledger_entries` (and supporting `journal_entries` / `journal_lines` until post).
   - Platform: **journal** `coa_trans` + **journal_entry** rows `coa_trans_data` after `status = POSTED` (and lines written for a given phase per use case).

2. **No in-place edits after post** — Corrections use **reversing journals** (FR-6) linked to the original `business_ref` / `coa_trans_id`. Period-close rules prohibit mutating closed `coa_trans_data` rows (foundation §4).

3. **Balances are derived** — Account balances are computed as `SUM(debits) − SUM(credits)` (or equivalent by account normal balance) over posted lines. Any `balance_snapshot` table is a **cache**, rebuildable from the ledger, never written instead of a missing posting.

4. **Strong consistency on post** — A single ACID transaction persists header + all lines + ledger immutability marker; posting fails if `sum(DR) ≠ sum(CR)` or period is not open (FR-4, FR-8).

5. **Idempotency at the business boundary** — `reference_id` / `business_ref` (e.g. bank webhook ref) prevents duplicate posts; retries return the existing `coa_trans_id`.

6. **Wallet sync is downstream** — After POSTED, accounting publishes an event or calls `core.wallet` API; wallet tables are not joined in accounting repositories (foundation §3–4).

---

## Consequences

### Positive

- Full audit trail: every financial change is an append-only fact.
- Reconciliation and regulatory review simplify to “replay the ledger.”
- Posting failures do not leave half-updated balance rows.
- Aligns with TRD FR-5, FR-6, FR-11 and foundation transit invariant (e.g. **3100 = 0** when use case completes).

### Negative / trade-offs

- Balance reads may require aggregation or maintained snapshots (TRD FR-7, NFR-5).
- Storage grows monotonically; archiving strategy needed later (out of v1 TRD scope).
- Two-phase flows (e.g. deposit PENDING) temporarily hold transit balances until POSTED — operational monitoring required.

### Implementation notes

| Concern | Approach |
|---------|----------|
| Read latency (20k balance reads/sec) | Optional periodic or incremental snapshots + Redis cache (TRD §8); invalidate on post |
| Reporting (P95 < 2s) | Read replicas or Elasticsearch read model fed by `JournalPosted` events |
| Multi-currency (FR-9) | Store currency + rate on each line at post time; rates immutable after post |
| Draft work in progress | TRD Draft / `coa_trans.status = PENDING` — journal not in trial balance until Posted |

---

## Alternatives considered

| Alternative | Why rejected |
|-------------|--------------|
| Update `accounts.balance` on every line | Loses trivial point-in-time audit; concurrency and partial-update risk |
| Edit posted lines in place | Breaks immutability; violates FR-6 and period-close rules |
| Use wallet balance as ledger | Wrong bounded context; `2110` mirrors wallet but wallet owns its tables |

---

## Compliance with TRD / foundation

- **TRD §12 design recommendation** — This ADR records that recommendation as an accepted architectural decision.
- **`coa_trans` / `coa_trans_data`** — Journal + journal_entry tables in `core.accounting`.
- **Reversal** — New journal (`coa_trans`) with inverted journal_entry rows; no UPDATE on `coa_trans_data` after close.

## Terminology: `coa_*` vs journal / journal_entry

| Platform term | Table | TRD name (API / FR) |
|---------------|-------|---------------------|
| Chart of accounts row | `coa_account` | `accounts` |
| **Journal** | `coa_trans` | `journal_entries` (header — one balanced posting) |
| **Journal entry** | `coa_trans_data` | `journal_lines` (one DR/CR line; FK `coa_trans_id`) |
| Posted line | `coa_trans_data` | `ledger_entries` (after POSTED) |
| Idempotency key | `business_ref` on `coa_trans` | `reference_id` |

**Rule:** `coa_trans` = **journal**; `coa_trans_data` = **journal_entry** — not “journal entry = header”. TRD REST may still say `/v1/journal-entries` for the journal resource. See [`core.foundation.md` §2.2](../core.foundation.md).

---

## References

- [`core.accounting.trd.md`](../core.accounting.trd.md) — §12 Recommended Architecture; FR-5, FR-6, FR-7; NFR-2, NFR-3
- [`core.foundation.md`](../core.foundation.md) — `coa_*` tables §2, posting flow §3, deposit two-phase §8
