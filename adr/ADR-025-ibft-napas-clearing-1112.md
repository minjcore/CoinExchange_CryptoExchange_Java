# ADR-025: IBFT bank leg uses Napas clearing 1112 — not 1111

| Field | Value |
|-------|-------|
| Status | Pending review |
| Date | 2026-06-08 |
| Source | [`core.sharedlib.md`](../core.sharedlib.md) §11, [`design-v2/accounting.md`](../design-v2/accounting.md) §18 |
| Related | [ADR-010](ADR-010-transit-accounts-net-zero.md), [ADR-007](ADR-007-freeze-settle-async-outflow.md) |

---

## Decision

1. IBFT accounting template uses transit **3400** (not 3200 withdraw).
2. Bank/Napas outflow leg posts to **1112 Napas Clearing** — **not 1111** Vietinbank dedicated.
3. Napas/bank cost **5100** and fee revenue **4130** per foundation §11.
4. Wallet leg: same freeze-settle-release as withdraw ([ADR-007](ADR-007-freeze-settle-async-outflow.md)).
5. Napas adapter idempotent on `businessRef`; product list ref [`napas-api-portal-intro.md`](../references/napas-api-portal-intro.md) — wire spec gated on portal.
6. Orchestration rejects line build if IBFT uses **1111** for Napas leg (I-O8).

---

## Acceptance criteria (AC-025)

| ID | Criterion |
|----|-----------|
| AC-025-01 | IBFT POSTED 3400 net zero |
| AC-025-02 | Bank leg account 1112 |
| AC-025-03 | 5100 present when template requires |
| AC-025-04 | 1111 on IBFT bank leg rejected |
| AC-025-05 | Wallet IBFT_FREEZE/SETTLE/RELEASE same as withdraw class |

---

## Test cases (TC-025)

| ID | Expected | Maps to |
|----|----------|---------|
| TC-025-01 | IBFT ledger POSTED 3400=0 | IBFT happy |
| TC-025-02 | 1112 not 1111 | IBFT-E05 |
| TC-025-03 | 5100 on ledger | IBFT-E01 |
| TC-025-04 | Wrong 1111 rejected at build | orchestration I-O8 |
| TC-025-05 | IBFT wallet freeze settle | TC-007 IBFT cases |

---

## References

- [`core.sharedlib.md`](../core.sharedlib.md) — §11 IBFT
- [`design-v2/accounting.md`](../design-v2/accounting.md) — §18
