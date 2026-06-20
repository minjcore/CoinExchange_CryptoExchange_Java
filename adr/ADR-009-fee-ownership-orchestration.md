# ADR-009: Fee computation owned by orchestration

| Field | Value |
|-------|-------|
| Status | Accepted |
| Date | 2026-06-08 |
| Deciders | Engineering |
| Related | [`core.business-processes.md`](../core.business-processes.md) §16, [`design-v2/orchestration.md`](../design-v2/orchestration.md) §5, §20, [`core.sharedlib.md`](../core.sharedlib.md) §8–16 (fee accounts) |

---

## Context

Each use case splits **gross vs net** differently (deposit nets fee from credit; withdraw adds fee to gross freeze; payment debits gross, credits merchant net). If wallet or accounting recomputes fees independently, DR/CR and wallet amounts **diverge**.

[`core.business-processes.md`](../core.business-processes.md) §16 closes this gap: **orchestration** is the single calculator; wallet receives final amounts; accounting records given fee lines.

---

## Decision

1. **Orchestration computes** — Fee rates from config; amounts passed to wallet commands and accounting line builder.
2. **Wallet never splits fees** — `credit`/`debit`/`freeze` amounts are final ([`core.wallet.md`](../core.wallet.md) out of scope).
3. **Accounting records given lines** — 4110–4150 revenue, 5100 cost — no product rule engine in `core.accounting`.
4. **Single computed value** — Same `fee` number used for wallet gross/net split and accounting 4xxx line in one request.
5. **Rounding** — Scale 4, HALF_UP at orchestration boundary ([`core.sharedlib.md`](../core.sharedlib.md) §6).
6. **Bearer rules** — Deposit fee from user (netted); withdraw/transfer/IBFT added to user gross; MDR from merchant; payroll/disbursement per batch policy (§16 table).

Fee **rates** are configurable; **ownership and posting rules** are fixed by this ADR.

---

## Consequences

### Positive

- One source of truth per transaction — no wallet/ledger amount mismatch.
- Domain modules stay simple and testable with explicit amounts.

### Negative / trade-offs

- Fee schedule tables are product config — not fully specified in repo (documented as examples in orchestration §20).
- Tier/partner overrides live in orchestration config complexity.

---

## Relationship to other ADRs

| ADR | Topic |
|-----|--------|
| ADR-002 | Fee logic not in foundation |
| ADR-008 | Orchestration sequences fee + domain calls |

---

## Acceptance criteria (AC-009)

| ID | Criterion |
|----|-----------|
| AC-009-01 | Wallet module has no fee rate config or fee math |
| AC-009-02 | Accounting accepts fee lines from orchestration — no recomputation |
| AC-009-03 | Deposit: wallet credit = gross − fee; accounting 4110 = same fee |
| AC-009-04 | Withdraw/IBFT: freeze gross = principal + fee from orchestration |
| AC-009-05 | Transfer: debit A gross, credit B net, fee on ledger 4130 matches orchestration `fee` |
| AC-009-06 | Payment: user debit gross, merchant credit netToMerchant from orchestration |
| AC-009-07 | Rounding applied once at orchestration before domain calls |

---

## Test cases (TC-009)

| ID | Title | Expected | Maps to |
|----|-------|----------|---------|
| TC-009-01 | Deposit 100k fee 1k | wallet +99k; 4110 +1k | Deposit happy |
| TC-009-02 | Zero fee deposit | wallet +gross; no 4110 | Deposit zero fee |
| TC-009-03 | Withdraw gross | freeze 101k for 100k+1k fee | Withdraw happy |
| TC-009-04 | Transfer gross/net | A −101k B +100k 4130 +1k | Transfer scenarios |
| TC-009-05 | Payment gross/net | user −gross merchant +net | Payment scenarios |
| TC-009-06 | Wallet cannot recompute | Code review — no feeSchedule in wallet | AC-009-01 |

---

## References

- [`core.business-processes.md`](../core.business-processes.md) — §16 Fee policy
- [`design-v2/orchestration.md`](../design-v2/orchestration.md) — §5, §20
- [`core.sharedlib.md`](../core.sharedlib.md) — §6.4 revenue accounts, use-case fee lines
- [`design-v2/acceptance.md`](../design-v2/acceptance.md) — Deposit, Payment, Transfer, Withdraw
