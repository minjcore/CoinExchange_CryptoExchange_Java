# ADR-006: Two-phase deposit with transit account 3100

| Field | Value |
|-------|-------|
| Status | Accepted |
| Date | 2026-06-08 |
| Deciders | Engineering |
| Related | [`core.sharedlib.md`](../core.sharedlib.md) §8, [`IMPLEMENTATION.md`](../IMPLEMENTATION.md) §7.5, [`core.business-processes.md`](../core.business-processes.md) §13.1, [ADR-001](ADR-001-immutable-ledger.md) |

---

## Context

Bank deposits are **async**: funds may be reported before final confirmation. Crediting the member wallet immediately risks double-credit or credit-before-bank-settle.

[`core.sharedlib.md`](../core.sharedlib.md) §8 defines a **two-phase** journal: phase A (`PENDING`) holds gross in transit **3100**; phase B (`POSTED`) clears transit, records net liability **2110** and fee **4110**, then downstream wallet credit.

[`IMPLEMENTATION.md`](../IMPLEMENTATION.md) §7.5 locks phase B in accounting via `confirmDeposit()` — not ad-hoc `addLines` from orchestration.

---

## Decision

1. **Phase A (`PENDING`)** — On bank webhook: journal `use_case=DEPOSIT`, `status=PENDING`, lines **1111 DR** gross + **3100 CR** gross. Wallet **not** credited.
2. **Phase B (`POSTED`)** — On confirm: append foundation §8.1 steps 3–6 inside **`JournalService.confirmDeposit(coaTransId, fee)`**; validate **3100 net zero** and DR=CR; set `POSTED`.
3. **Wallet credit** — Only after POSTED; orchestration passes **net** amount ([ADR-005](ADR-005-idempotency-key-strategy.md) same `business_ref`).
4. **S1 pattern** — `notifyDeposit` returns **202**; status via poll / event — not blocking wallet credit in HTTP thread.
5. **FAILED** — Mismatch/cancel reverses phase A only (1111/3100) per foundation §8.2 — not in-place edit of POSTED lines.

---

## Consequences

### Positive

- Wallet never credits before ledger confirms liability.
- Transit **3100** makes in-flight deposit visible for ops and trial balance.
- `confirmDeposit` centralizes transit-zero validation.

### Negative / trade-offs

- PENDING deposits need aging/retry ([`core.business-processes.md`](../core.business-processes.md) §13.1).
- Async lag between POSTED and wallet credit — W5 tolerance required.

---

## Relationship to other ADRs

| ADR | Topic |
|-----|--------|
| ADR-001 | POSTED lines immutable; reversal not UPDATE |
| ADR-006 | Two-phase deposit; **202** trước ghi sổ |
| ADR-041 | RabbitMQ `BANK_DEPOSIT`: orch publish → accounting worker |
| ADR-005 | Same `business_ref` webhook → queue → journal → wallet |
| ADR-010 | Transit **3100 = 0** at POSTED |

---

## Acceptance criteria (AC-006)

| ID | Criterion |
|----|-----------|
| AC-006-01 | Phase A creates `PENDING` with 1111 DR + 3100 CR only |
| AC-006-02 | `wallet.available` unchanged after phase A |
| AC-006-03 | Phase B via `confirmDeposit` only — not orchestration `addLines`+`post` for deposit |
| AC-006-04 | POSTED deposit has transit **3100 net zero** |
| AC-006-05 | Wallet `DEPOSIT_CREDIT` only after POSTED with net amount from orchestration |
| AC-006-06 | `confirmDeposit` on already-POSTED journal is idempotent no-op |
| AC-006-07 | Deposit notify returns **202** (async ack); journal qua RabbitMQ [ADR-041](ADR-041-rabbitmq-orch-to-accounting-worker.md) |

---

## Test cases (TC-006)

| ID | Title | Expected | Maps to |
|----|-------|----------|---------|
| TC-006-01 | Phase A PENDING | 3100 CR; wallet unchanged | `acceptance.md` Phase A only |
| TC-006-02 | Phase B POSTED | 3100=0; 2110+4110; wallet +net | Deposit happy |
| TC-006-03 | Credit before POSTED rejected | No wallet_tx | `wallet.md` §15, orchestration §11.6 |
| TC-006-04 | confirmDeposit idempotent | Second call no duplicate lines | AC-006-06 |
| TC-006-05 | PENDING reverse scope | 1111/3100 only | ADJ-E03 |
| TC-006-06 | Async 202 ack | notifyDeposit 202 + businessRef | orchestration §11.4 |

---

## References

- [`core.sharedlib.md`](../core.sharedlib.md) — §8 Deposit
- [`IMPLEMENTATION.md`](../IMPLEMENTATION.md) — §7.5, §8 Deposit boundaries
- [`design-v2/accounting.md`](../design-v2/accounting.md) — §14 DEPOSIT
- [`design-v2/orchestration.md`](../design-v2/orchestration.md) — §11
- [`design-v2/acceptance.md`](../design-v2/acceptance.md) — Deposit feature
