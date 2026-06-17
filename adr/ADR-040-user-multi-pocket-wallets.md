# ADR-040: A USER member may hold multiple wallet pockets (label-discriminated); MERCHANT/PARTNER stay single

| Field | Value |
|-------|-------|
| Status | Accepted |
| Date | 2026-06-12 |
| Deciders | Engineering |
| Source | Design review 2026-06-12 ("1 user có nhiều ngăn ví") |
| Related | [ADR-003](ADR-003-dual-schema-single-postgres.md), [ADR-004](ADR-004-wallet-balance-snapshot.md), [ADR-005](ADR-005-idempotency-key-strategy.md), [ADR-020](ADR-020-wallet-lanes-coa-control-mapping.md), [ADR-039](ADR-039-no-synchronous-wallet-aggregate-row.md) |

---

## Context

v1 keyed a wallet by `(member_id, wallet_type, currency)` — one wallet per member per type. The product requires a USER member to hold **multiple pockets** ("ngăn ví" — e.g. Spending, Savings, a trip fund), each an independently spendable balance, while MERCHANT and PARTNER members keep exactly one wallet per type.

The question is how to represent a pocket without breaking the per-wallet concurrency model ([ADR-004](ADR-004-wallet-balance-snapshot.md), [ADR-039](ADR-039-no-synchronous-wallet-aggregate-row.md)) or the accounting boundary ([ADR-020](ADR-020-wallet-lanes-coa-control-mapping.md)).

---

## Decision

1. **A pocket is a first-class `wallet` row** — its own `id`, its own `wallet_balance` (1:1), its own `wallet_tx` log. There is no sub-balance inside a single wallet row. Per-wallet row locking is unchanged; pockets of the same member lock independently.
2. **Pockets are instantiated from a catalog** `wallet.wallet_pocket_def` — a place to *define* the kinds of pocket the product offers (`code`, `name`, `is_default`, `multi_allowed`, `active`). Creating a USER wallet **references** a def code; the wallet stores `pocket_code` (FK → def, same schema) as the template and `label` as the user-facing display name (defaults from `def.name`).
   - **Discriminator = free `label`** set by the user (e.g. "Chi tiêu", "Tiết kiệm"). Key is `UNIQUE (member_id, wallet_type, currency, label)`; `label` is `NOT NULL`; the primary pocket uses label `'default'` (def `'DEFAULT'`, `is_default = true`).
   - `def.multi_allowed` governs whether a member may instantiate more than one wallet from that def (e.g. `GOAL` = true, `DEFAULT`/`SPENDING` = false) — enforced in the provisioning service, on top of the label UNIQUE.
3. **Multi-pocket is USER-only.** MERCHANT and PARTNER are restricted to one wallet per `(member_id, wallet_type, currency)` by a **partial unique index** `WHERE wallet_type <> 'USER'`. (USER multiplicity is still bounded by the label UNIQUE.)
4. **Accounting is unchanged.** Every USER pocket maps to the same control account **2110** (lane USER, [ADR-020](ADR-020-wallet-lanes-coa-control-mapping.md)). `2110 = Σ(available+frozen)` across **all** USER pockets of **all** members. Accounting neither knows nor needs the pocket split — it sees member-level / lane-level postings only.
5. **Operations target a specific pocket by `wallet_id`.** Orchestration resolves member + pocket selection → a concrete `wallet_id` before calling the wallet domain; the wallet domain never guesses a pocket. Idempotency triple `(wallet_id, business_ref, tx_type)` is unchanged ([ADR-005](ADR-005-idempotency-key-strategy.md)) and is now naturally pocket-scoped.
6. **Pocket-to-pocket movement (same member) reuses internal transfer.** Two USER pockets → `TRANSFER_DEBIT` on source `wallet_id`, `TRANSFER_CREDIT` on destination `wallet_id`; accounting posts 2110 DR / 2110 CR (net zero on 2110) exactly as a normal internal transfer ([`spec/foundation.md`](../spec/foundation.md) §10). No new ledger pattern.
7. **Lifecycle.** A pocket is created with status `ACTIVE`; closing requires `available = 0` and `frozen = 0` then `status = CLOSED`. The `'default'` pocket is not user-deletable. LOCKED semantics ([ADR-029](ADR-029-wallet-locked-rejects-mutation.md)) apply per pocket.

---

## Consequences

### Positive
- No change to the hot-path concurrency model or accounting — pockets are just more `wallet` rows.
- Aggregate liability stays single-sourced in COA 2110 ([ADR-039](ADR-039-no-synchronous-wallet-aggregate-row.md) intact).
- Inter-pocket transfer needs no new flow.

### Negative / trade-offs
- Every USER-facing balance/spend API must carry a pocket selector (`wallet_id` or member+label) — clients can no longer assume "the" user wallet.
- "Total user balance" for a member is now `SUM` over their pockets — a per-member aggregate query (cheap, indexed by `member_id`), not a single row.
- W5 reconciliation sums all USER pockets vs 2110 — unchanged in principle, larger row count.

---

## Alternatives considered

| Alternative | Why rejected |
|-------------|--------------|
| Sub-balances inside one `wallet` row (JSON/columns per pocket) | Breaks the single-row snapshot + per-wallet lock; one member's pockets would contend on one row. |
| `purpose` enum discriminator | Less flexible than a user-named label; product wants free-form pockets. |
| Multi-pocket for all lanes | No merchant/partner use case; settlement/payroll would need per-pocket routing for no benefit. Revisit if a use case appears. |
| Separate `pocket` table under one wallet | Duplicates `wallet`/`wallet_balance` semantics; a pocket *is* a wallet. |

---

## Prior art / precedent

This is a mainstream pattern, not a bespoke risk. **Primary provenance is in-house** — the pattern
was learned from this project's own reference architecture, then confirmed against industry tiers.

**Source of record (where we actually took it from):** the `00_framework_temporaty_referrence_only` reference codebase
(BizzanCloud-derived, retained for technical reference only — not production). There,
`member_wallet` is keyed by **`(member_id, coin_id)`**, so **one member already owns many wallet
rows** (one per coin), and **account is separate from balance** (`Member` identity vs `MemberWallet`
numbers). ADR-040 lifts that structure directly:

| Reference (`00_framework_temporaty_referrence_only`) | ADR-040 (GtelPay) |
|----------------------------|-------------------|
| one member → many `member_wallet` rows, discriminated by `coin_id` | one USER → many `wallet` rows, discriminated by `pocket_code`/`label` |
| pocket = a full wallet row (not a sub-balance column) | same — pocket = a full `wallet` row |
| `Member` (account) ⊥ `MemberWallet` (balance) | `wallet.member_id` ref ⊥ `wallet_balance` (numbers only) — [`design/platform/data-model.md`](../design/platform/data-model.md) §3.1 |

What GtelPay **adds beyond the reference** (the reference has none of these): COA double-entry
ledger (00_framework_temporaty_referrence_only only logs `member_transaction`, no balanced DR/CR), idempotency triple,
immutable ledger + transit net-zero, and the wallet⊥accounting schema split.

**Industry confirmation** (the pattern is not idiosyncratic) — three tiers:

| Tier | Example | What matches our design |
|------|---------|--------------------------|
| Consumer apps | Monzo **Pots**, Starling **Spaces**, Revolut **Pockets/Vaults**, N26 **Spaces** | Multiple named sub-balances ("pockets") inside one customer relationship; each independently spendable, all part of the same customer liability. |
| Ledger primitives | **Formance** ([`references/formance-ledger-accounts.md`](../references/formance-ledger-accounts.md)) — free-form accounts `users:123:main` / `:savings`; **Blnk** — one identity owns many balances; **TigerBeetle** ([`references/alexandrubagu-tigerbeetle-overview.md`](../references/alexandrubagu-tigerbeetle-overview.md)) — cheap accounts, one entity → many; **Modern Treasury** ([`references/moderntreasury-digital-wallet-tutorial.md`](../references/moderntreasury-digital-wallet-tutorial.md)), **Increase** sub-accounts ([`references/increase-api-accounts.md`](../references/increase-api-accounts.md)) | "A pocket is a full account row, not a sub-balance column" — exactly how these model one user with many balances. |
| Core banking | A customer (CIF) holding many accounts (current/savings/term) → all map to **one GL control account**; products instantiated from a **product definition** (Mambu deposit products [`references/mambu-apis-overview.md`](../references/mambu-apis-overview.md); Thought Machine Vault product definitions) | Our `wallet_pocket_def` = product/account-type catalog; all USER pockets roll up to control **2110**. |

The three choices this ADR makes — (a) pocket = full wallet row, (b) all pockets roll up to one control account, (c) instantiate from a catalog — come from the in-house reference for (a) and (b) (one-member-many-wallets + account/balance separation), with (c) the catalog being the GtelPay addition; all three are independently confirmed by the industry tiers above. Member-separate-from-balance is the reference's own `Member`⊥`MemberWallet` split, also mirrored by customer-master-vs-account in Blnk/Modern Treasury.

---

## Acceptance criteria (AC-040)

| ID | Criterion |
|----|-----------|
| AC-040-01 | A USER member can hold >1 wallet row differing only by `label`; each has its own `wallet_balance` + `wallet_tx` |
| AC-040-08 | A wallet's `pocket_code` references an existing active `wallet_pocket_def`; provisioning rejects unknown/inactive codes |
| AC-040-09 | `def.multi_allowed = false` ⇒ provisioning rejects a second pocket of that def for the member |
| AC-040-02 | `UNIQUE (member_id, wallet_type, currency, label)` rejects duplicate label for the same member/type/currency |
| AC-040-03 | MERCHANT/PARTNER cannot create a second wallet for the same `(member_id, wallet_type, currency)` (partial unique index) |
| AC-040-04 | All USER pockets post to control account 2110; `2110 = Σ` over all USER pockets (W5) |
| AC-040-05 | A debit/freeze targets exactly one `wallet_id`; other pockets of the member are untouched and not locked |
| AC-040-06 | Pocket-to-pocket transfer uses `TRANSFER_DEBIT`/`TRANSFER_CREDIT`, 2110 DR/CR net zero |
| AC-040-07 | Closing a pocket requires zero `available` and `frozen`; `'default'` pocket not user-deletable |

---

## Test cases (TC-040)

| ID | Expected | Maps to |
|----|----------|---------|
| TC-040-01 | Create 2 USER pockets "Chi tiêu"/"Tiết kiệm" → 2 wallet rows, 2 balances | AC-040-01 |
| TC-040-02 | Duplicate label same member/type/ccy → conflict | AC-040-02 |
| TC-040-03 | Second MERCHANT wallet same member → rejected by partial unique index | AC-040-03 |
| TC-040-04 | Debit pocket A; pocket B balance + lock unaffected | AC-040-05 |
| TC-040-05 | Transfer A→B same member → 2110 DR/CR net zero, both pockets updated | AC-040-06, foundation §10 |
| TC-040-06 | W5: Σ USER pockets across members = 2110 | AC-040-04, ADR-014 |
| TC-040-07 | Close pocket with frozen>0 → rejected | AC-040-07 |

---

## References

- [`design/platform/data-model.md`](../design/platform/data-model.md) — wallet table, pocket discriminator
- [`spec/implementation.md`](../spec/implementation.md) §3 — wallet DDL (label column, partial unique index)
- [`design-v2/wallet.md`](../design-v2/wallet.md) §2 — wallet instance definition
- [ADR-020](ADR-020-wallet-lanes-coa-control-mapping.md), [ADR-039](ADR-039-no-synchronous-wallet-aggregate-row.md), [ADR-005](ADR-005-idempotency-key-strategy.md)
