# Phase 0 — Research: USER Multi-Pocket Wallets

The design space for this feature is **already resolved** by locked ADRs; this file consolidates
those decisions (no open `NEEDS CLARIFICATION`). Each entry: Decision · Rationale · Alternatives.

## R1 — Pocket representation

- **Decision**: A pocket is a **full `wallet` row** (own `wallet_balance`, own `wallet_tx`), not a
  sub-balance column. (ADR-040 §1)
- **Rationale**: Preserves the per-`wallet_id` row-lock concurrency model and single-row reads
  (ADR-004); pockets of one member never contend.
- **Alternatives**: JSON/columns sub-balances inside one row — rejected (one member's pockets would
  contend on one row, breaks ADR-004); separate `pocket` table under a wallet — rejected (a pocket
  *is* a wallet). Precedent: Formance/Blnk/TigerBeetle, Monzo Pots (see ADR-040 Prior art).

## R2 — Discriminator + catalog

- **Decision**: Pocket instantiated from `wallet_pocket_def` (catalog) via `pocket_code`; user sets
  a free `label`. Uniqueness = `(member_id, wallet_type, currency, label)`. (ADR-040 §2)
- **Rationale**: Catalog gives product control over offered pocket kinds; label gives user-facing
  flexibility. Mirrors core-banking "deposit product → account" instantiation (Mambu/Thought Machine).
- **Alternatives**: `purpose` enum only — rejected (less flexible); free label with no catalog —
  rejected (no product governance of pocket kinds).

## R3 — Scope (USER only)

- **Decision**: Multi-pocket is USER-only; MERCHANT/PARTNER single, enforced by a **partial unique
  index** `WHERE wallet_type <> 'USER'`. (ADR-040 §3)
- **Rationale**: No merchant/partner use case; settlement/payroll would need per-pocket routing for
  no benefit.
- **Alternatives**: multi-pocket all lanes — deferred until a real use case appears.

## R4 — Aggregate stays in accounting

- **Decision**: All USER pockets roll up to control **2110**; no wallet-side stored aggregate;
  member "total" = `SUM` over pockets; in-domain aggregates are async rollups. (ADR-040 §4, ADR-039, ADR-020)
- **Rationale**: Single authoritative aggregate (avoids drift triangle); no global write hotspot.
- **Alternatives**: synchronous wallet aggregate row — rejected by ADR-039 (hotspot + duplicate truth).

## R5 — Pocket-to-pocket movement

- **Decision**: Reuse the **internal-transfer** saga: `TRANSFER_DEBIT` source → `TRANSFER_CREDIT`
  destination; ledger 2110 DR/CR (net 0); default fee 0. (ADR-040 §6, processes.md §11A.3)
- **Rationale**: No new money-movement type or ledger pattern; accounting need not know it is the
  same member.
- **Alternatives**: a bespoke "pocket move" posting — rejected (duplicates transfer semantics).

## R6 — Pocket addressing (resolution)

- **Decision**: Orchestration resolves `memberId` + selector (`walletId` | `pocketCode`/`label` |
  default) → a concrete `wallet_id` **before** any wallet leg; wallet never guesses. (ADR-040 §5,
  orchestration §1.2)
- **Rationale**: Keeps wallet domain dumb about pocket selection; idempotency triple becomes
  pocket-scoped naturally.
- **Alternatives**: wallet resolves default pocket internally — rejected (leaks selection policy
  into the domain).

## R7 — Lifecycle (create / close)

- **Decision**: Create = insert wallet + `wallet_balance(0,0)`, **no `wallet_tx`, no ledger**;
  close requires `available=0 && frozen=0`, `'default'` not closable, CLOSED rejects mutation
  (like LOCKED). (ADR-040 §7, ADR-029, processes.md §11A.1/§11A.2)
- **Rationale**: Pocket lifecycle is pure account housekeeping; moving no money means no posting.
- **Alternatives**: post a zero journal on create — rejected (pointless ledger noise).

## R8 — Default pocket

- **Decision**: Every USER has exactly one auto-provisioned `'default'` pocket (def `DEFAULT`,
  `is_default=true`); it is the fallback when no pocket is named and is not user-deletable.
- **Rationale**: Backward compatibility — existing single-wallet callers keep working by defaulting.
- **Alternatives**: require explicit pocket always — rejected (breaks existing callers / UX).

**Output**: all decisions resolved; ready for Phase 1.
