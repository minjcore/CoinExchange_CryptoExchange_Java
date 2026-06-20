# ADR-020: Wallet lanes USER / MERCHANT / PARTNER map to COA control accounts

| Field | Value |
|-------|-------|
| Status | Pending review |
| Date | 2026-06-08 |
| Source | [`core.sharedlib.md`](../core.sharedlib.md) §6.2, [`core.wallet.md`](../core.wallet.md) §3.2, [`TERMINOLOGY.md`](../TERMINOLOGY.md) |
| Related | [ADR-014](ADR-014-reconciliation-w5-report-only.md) |

---

## Decision

| `wallet_type` | COA control (aggregate liability) | v1 usage |
|---------------|-----------------------------------|----------|
| `USER` | **2110** | Consumer wallet |
| `MERCHANT` | **2120** | Merchant accept / payroll / optional QR credit |
| `PARTNER` | **2130** | Escrow pre-fund + disbursement |

1. Mapping is **reconciliation only** — no FK, no COA code on `wallet` row.
2. One wallet instance per `(member_id, wallet_type, currency)`.
3. W5 sums by `wallet_type` compare to respective control ([ADR-014](ADR-014-reconciliation-w5-report-only.md)).
4. Foundation identity: `(1111+1112+1113) = (2110+2120+2130)` at platform level ([`core.sharedlib.md`](../core.sharedlib.md) §5).

---

## Acceptance criteria (AC-020)

| ID | Criterion |
|----|-----------|
| AC-020-01 | UNIQUE (member_id, wallet_type, currency) |
| AC-020-02 | USER payment debits 2110 aggregate via ledger |
| AC-020-03 | MERCHANT credit increases 2120 aggregate |
| AC-020-04 | PARTNER prefund increases 2130 |
| AC-020-05 | W5 grouped by wallet_type |

---

## Test cases (TC-020)

| ID | Expected | Maps to |
|----|----------|---------|
| TC-020-01 | Payment 2110 DR user liability | Payment scenarios |
| TC-020-02 | Payroll 2120 DR | Payroll |
| TC-020-03 | Partner prefund 2130 | Disbursement prefund |
| TC-020-04 | W5 lane sum vs control | Reconciliation W5 |

---

## References

- [`core.sharedlib.md`](../core.sharedlib.md) — §5–6.2
- [`design-v2/wallet.md`](../design-v2/wallet.md) — §2 lanes
