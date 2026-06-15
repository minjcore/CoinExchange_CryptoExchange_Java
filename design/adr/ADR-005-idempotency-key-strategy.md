# ADR-005: Idempotency key strategy across surfaces (`business_ref`)

| Field | Value |
|-------|-------|
| Status | Accepted |
| Date | 2026-06-08 |
| Deciders | Engineering |
| Related | [`integration-surfaces.md`](../integration-surfaces.md) §8, [`IMPLEMENTATION.md`](../IMPLEMENTATION.md) §2 (D1–D5), [`core.wallet.md`](../core.wallet.md) §8, [`core.accounting.trd.md`](../core.accounting.trd.md) §13 |

---

## Context

A single business action (deposit, payment, transfer, withdraw) crosses several surfaces — public HTTP (S1), accounting (S2), Kafka events (S3), RabbitMQ commands (S6) — and is retried at every hop (client retry, AMQP redelivery, consumer requeue, payout retry). Without one consistent key, the same action can post a journal twice, double-credit a wallet, or double-pay a bank.

Complications already present in the design:

- One S1 action drives **multiple** wallet legs on **different** wallets (payment: debit USER + credit MERCHANT).
- The **same** wallet can receive several distinct operations under one action (withdraw: freeze, then settle, then maybe release).
- Transport-level retries (AMQP) need their own dedup distinct from business idempotency.

This ADR records the keying rules already scattered across `IMPLEMENTATION.md` D1–D5, `integration-surfaces.md` §8, and the per-service idempotency sections, so there is one authoritative statement.

---

## Decision

**`business_ref` is the single business-level idempotency key, propagated unchanged across every surface.** Per-store uniqueness constraints disambiguate the legs of one action.

1. **One key, all surfaces** — the S1 `X-Idempotency-Key` becomes `businessRef`, carried as: S2 `reference_id`, S3 payload field, the S6 envelope `businessRef` (full body, **not** header-only, F6), and `wallet_tx.business_ref` ([`integration-surfaces.md`](../integration-surfaces.md) §8).
2. **Wallet uniqueness** — `UNIQUE (wallet_id, business_ref, tx_type)` ([`IMPLEMENTATION.md`](../IMPLEMENTATION.md) D1). The `tx_type` and `wallet_id` let one `businessRef` drive several legs while each leg stays singular:
   - Payment D2: same `businessRef`, `PAYMENT_DEBIT` on USER wallet + `PAYMENT_CREDIT` on MERCHANT wallet.
   - Transfer D3: same `businessRef`, `TRANSFER_DEBIT` / `TRANSFER_CREDIT` on different wallets.
   - Withdraw D5: derived sub-keys `{businessRef}:settle` / `{businessRef}:release` for follow-up legs on the same wallet.
3. **Accounting uniqueness** — `UNIQUE (reference_id, use_case)` on `coa_trans` ([`IMPLEMENTATION.md`](../IMPLEMENTATION.md) §7.3); duplicate `createJournal` returns the existing `coa_trans_id`.
4. **Transport dedup is separate** — the S6 envelope `messageId` (UUID per physical publish) dedups redeliveries; it is **distinct** from `businessRef` and is never the business key ([`integration-surfaces.md`](../integration-surfaces.md) §8, §6.1).
5. **Replay semantics** — same key + same semantics → return the prior result, no second effect (HTTP 200, `idempotentReplay=true`). Same key + **conflicting** payload (e.g. different amount) → reject: `WALLET_DUPLICATE_CONFLICT` → HTTP 409 ([`IMPLEMENTATION.md`](../IMPLEMENTATION.md) D1, [`core.wallet.md`](../core.wallet.md) §8).
6. **Consumer rule** — workers are idempotent on `(commandType, businessRef)` and ACK only after the DB commit or a safe no-op replay ([`integration-surfaces.md`](../integration-surfaces.md) §6.2, C1–C2).
7. **`coaTransId` is correlation, not idempotency** — it links a `wallet_tx` to its journal for tracing; it never enforces a cross-module FK and is not an idempotency key ([`integration-surfaces.md`](../integration-surfaces.md) §8).

---

## Consequences

### Positive

- One mental model end-to-end: "same `businessRef` ⇒ same outcome."
- Multi-leg actions are safe without inventing a new key per leg (uniqueness triple does the work).
- Transport redelivery cannot double-apply business effects (`messageId` vs `businessRef` separation).
- Conflicting reuse is detected, not silently absorbed (409 on amount mismatch).

### Negative / trade-offs

- Derived sub-keys (`:settle`, `:release`) are a string convention orchestration must apply consistently — documented in D5 and [`operations.md`](../operations.md).
- Clients must generate a stable `X-Idempotency-Key` and reuse it on retry; a fresh key per retry defeats idempotency (client contract in `openapi/`).
- `tx_type` becomes part of correctness, not just labeling — its enum is locked in [`IMPLEMENTATION.md`](../IMPLEMENTATION.md) §2.1.

### Implementation notes

| Concern | Approach |
|---------|----------|
| Key format | Opaque ≤ 128 chars; orchestration may prefix use case (`dep-…`, `pay-…`) for readability |
| Sub-key derivation | `{businessRef}:settle` / `{businessRef}:release` only for withdraw follow-up legs (D5) |
| Conflict response | `WALLET_DUPLICATE_CONFLICT` / `409`; accounting unbalanced is a *different* error (`ACCOUNTING_UNBALANCED_JOURNAL`) |
| Observability | Log `businessRef`, `messageId`, `idempotentReplay` on every mutation ([`IMPLEMENTATION.md`](../IMPLEMENTATION.md) §12) |

---

## Alternatives considered

| Alternative | Why rejected |
|-------------|--------------|
| One `businessRef` UNIQUE per wallet (no `tx_type`) | Cannot represent multi-leg actions on the same wallet (freeze + settle) |
| Use `messageId` as the business key | Changes per publish; retries would create duplicates |
| Idempotency only in AMQP headers | Lost on re-publish / cross-transport; violates F6 |
| Separate generated key per leg | Orchestration must persist a mapping; brittle vs deterministic triple/sub-key |

---

## Relationship to other ADRs

| ADR | Topic |
|-----|--------|
| [ADR-001](ADR-001-immutable-ledger.md) | Accounting `reference_id` idempotency on immutable journals |
| [ADR-003](ADR-003-dual-schema-single-postgres.md) | Separate commits per domain — idempotency makes retry-after-partial-failure safe |
| [ADR-004](ADR-004-wallet-balance-snapshot.md) | Replay-safe single-transaction balance write keyed by the uniqueness triple |

---

## References

- [`integration-surfaces.md`](../integration-surfaces.md) — §8 Idempotency & correlation, §6.1–6.2 envelope + consumer rules, §10 F6
- [`IMPLEMENTATION.md`](../IMPLEMENTATION.md) — §2 D1–D5, §2.1 `WalletTxType`, §7.3 accounting idempotency
- [`core.wallet.md`](../core.wallet.md) — §8 idempotency and errors
- [`core.accounting.trd.md`](../core.accounting.trd.md) — §13 alignment (idempotency)
