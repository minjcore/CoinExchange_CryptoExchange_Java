# Phase 1 — Data Model: USER Multi-Pocket Wallets

Design-level model + **deltas vs the existing schema**. Binding DDL: [`spec/implementation.md`](../../spec/implementation.md) §3.
ERD context: [`design/platform/data-model.md`](../../design/platform/data-model.md) §3.1.

## Entities

### `wallet_pocket_def` (NEW — catalog / reference data)

| Field | Type | Rule |
|-------|------|------|
| `code` | VARCHAR(32) PK | e.g. DEFAULT, SPENDING, SAVINGS, GOAL |
| `name` | VARCHAR(64) | default display name |
| `description` | VARCHAR(255) null | |
| `wallet_type` | enum | USER (v1) |
| `is_default` | bool | exactly one `true` (DEFAULT) — auto-created pocket |
| `multi_allowed` | bool | may a member hold >1 of this def (GOAL=true) |
| `active` | bool | inactive defs cannot be instantiated |
| `sort_order` | int | display order |

### `wallet` (MODIFIED — add 2 columns)

| Field | Change | Rule |
|-------|--------|------|
| `pocket_code` | **NEW** VARCHAR(32) NOT NULL DEFAULT `'DEFAULT'` FK→`wallet_pocket_def(code)` | template the pocket was created from |
| `label` | **NEW** VARCHAR(64) NOT NULL DEFAULT `'default'` | user display name |
| UNIQUE | **CHANGED** `(member_id, wallet_type, currency)` → `(member_id, wallet_type, currency, label)` | USER may repeat type/ccy with different label |
| partial unique | **NEW** `(member_id, wallet_type, currency) WHERE wallet_type <> 'USER'` | MERCHANT/PARTNER stay single |

### `wallet_balance`, `wallet_tx` — UNCHANGED

Keyed by `wallet_id`; a pocket reuses them as-is. `wallet_tx.available_after`/`frozen_after`
snapshots already support per-pocket as-of reads. **No `member_id` on balance** (member↔balance
separation preserved — data-model §3.1).

### State transitions (pocket = `wallet.status`)

```
(create) → ACTIVE ──lock──► LOCKED ──unlock──► ACTIVE
ACTIVE ──close (avail=0 & frozen=0, not 'default')──► CLOSED   [terminal; rejects mutation]
```

## Backward compatibility (PRIMARY CONCERN)

Introducing pockets must **not** break existing single-wallet callers, data, or flows. Levers:

| Surface | Old behavior | New behavior | Why compatible |
|---------|--------------|--------------|----------------|
| Existing `wallet` rows | one per (member,type,ccy) | become the `'default'` pocket | new columns have DEFAULTs (`'DEFAULT'`/`'default'`); old UNIQUE still holds since each member has one label `'default'` |
| `wallet_balance` / `wallet_tx` | keyed by `wallet_id` | unchanged | no schema change; existing rows valid |
| Public API (balance/payment/transfer/withdraw) | no pocket param | optional `walletId`/`pocketCode`; **omitted → default pocket** | new params are **additive & optional**; existing clients keep working unchanged |
| Orchestration resolution | memberId → the wallet | memberId (no pocket) → default pocket | R6/R8 default fallback |
| Idempotency triple | `(wallet_id, business_ref, tx_type)` | identical (wallet_id = default pocket id) | unchanged |
| MERCHANT/PARTNER | single wallet | single wallet (partial unique index) | guarantee preserved |
| Accounting / 2110 | USER lane → 2110 | USER pockets → 2110 (more rows, same control) | no accounting change; W5 still sums lane vs 2110 |

**Net:** a client that never heard of pockets behaves exactly as before — it transparently uses the
default pocket. Multi-pocket is **opt-in** via the new optional fields.

## Migration path

For a **deployed** system (10_core is design-only today, but document for rollout):

1. **`V_n__pocket_def.sql`** — create `wallet_pocket_def`; seed `DEFAULT`(is_default), `SPENDING`,
   `SAVINGS`, `GOAL`(multi_allowed). (DDL: implementation.md §3 seed block)
2. **`V_n+1__wallet_pockets.sql`** —
   - `ALTER TABLE wallet.wallet ADD COLUMN pocket_code VARCHAR(32) NOT NULL DEFAULT 'DEFAULT' REFERENCES wallet.wallet_pocket_def(code);`
   - `ADD COLUMN label VARCHAR(64) NOT NULL DEFAULT 'default';`
   - existing rows **backfill automatically** via DEFAULT — no data rewrite.
   - drop old `uq_wallet_member_type_ccy`; add `uq_wallet_member_type_ccy_label`.
   - create partial unique `uq_wallet_single_nonuser`.
3. **Order matters:** def table before the FK column. Backfill is implicit (DEFAULT), so the
   `ADD COLUMN ... NOT NULL DEFAULT` is safe on a populated table.
4. **Rollback**: additive columns + indexes are reversible; no destructive change to balances/tx.

> Because 10_core is pre-implementation, the **target V1 DDL** in implementation.md §3 already shows
> the final shape (columns + indexes inline). The two-step migration above is the path if pockets
> land **after** an initial single-wallet deployment.

## Validation rules (from spec FRs)

- Create: `pocket_code` exists & active (FR-003); `multi_allowed=false` ⇒ no second of that def
  (FR-003/AC-040-09); label unique per member/type/ccy (FR-003).
- Close: `available=0 && frozen=0` (FR-008); not `'default'` (FR-008/AC-040-07); CLOSED rejects
  mutation.
- Move: sufficient source funds (FR-006); source ≠ destination (FR-006); both not LOCKED/CLOSED
  (FR-006); idempotent (FR-011).
- Invariant: `Σ USER pockets (avail+frozen) = 2110` within tolerance (FR-009/SC-005, W5/ADR-014).
