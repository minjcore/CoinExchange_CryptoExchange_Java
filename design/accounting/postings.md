# Accounting postings (DR/CR)

Canonical example amounts: principal **100,000** · service fee **1,000** · bank/Napas cost **500** where applicable.

Every use case: **transit = 0** on completion · `sum(DR)=sum(CR)` per journal.

## Deposit (3100, 2-phase)

Fee 1,000 → net to user **99,000**.

| Phase | Account | DR | CR |
|-------|---------|----|----|
| A PENDING | 1111 | 100,000 | |
| A | 3100 | | 100,000 |
| B POSTED | 3100 | 100,000 | |
| B | 2110 | | 99,000 |
| B | 2110 | 1,000 | |
| B | 4110 | | 1,000 |

## Withdraw (3200)

Gross **101,000** (100k + 1k fee).

| Account | DR | CR |
|---------|----|----|
| 2110 | 101,000 | |
| 3200 | | 101,000 |
| 3200 | 100,000 | |
| 1111 | | 100,000 |
| 3200 | 1,000 | |
| 4120 | | 1,000 |

## Internal transfer (3300)

Gross 101,000, net 100,000, fee 1,000.

| Account | DR | CR |
|---------|----|----|
| 2110 (A) | 101,000 | |
| 3300 | | 101,000 |
| 3300 | 100,000 | |
| 2110 (B) | | 100,000 |
| 3300 | 1,000 | |
| 4130 | | 1,000 |

## IBFT (3400)

| Account | DR | CR |
|---------|----|----|
| 2110 | 101,000 | |
| 3400 | | 101,000 |
| 3400 | 1,000 | |
| 4130 | | 1,000 |
| 3400 | 100,000 | |
| 1112 | | 100,000 |
| 5100 | 500 | |
| 1112 | | 500 |

## Wallet payment (3500)

| Account | DR | CR |
|---------|----|----|
| 2110 | 100,000 | |
| 3500 | | 100,000 |
| 3500 | 100,000 | |
| 2120 | | 100,000 |

## QR/POS (3500 → EOD)

| Account | DR | CR |
|---------|----|----|
| 1113 | 100,000 | |
| 3500 | | 100,000 |
| 5100 | 500 | |
| 1113 | | 500 |
| 3500 | 100,000 | |
| 2120 | | 100,000 |

## Payroll (3600)

5 × 100k + fee 5k.

| Account | DR | CR |
|---------|----|----|
| 2120 | 505,000 | |
| 3600 | | 505,000 |
| 3600 | 5,000 | |
| 4150 | | 5,000 |
| 3600 | 500,000 | |
| 1112 | | 500,000 |
| 5100 | 2,500 | |
| 1112 | | 2,500 |

## Disbursement (3700)

Pre-fund: `1111 DR 100k` → `2130 CR 100k`. Disburse: `2130 DR 101k` → `3700` → `1112 CR 100k` + fees.

## EOD settlement (3800/3810/3820)

Lock 2120 → 3800 → split MDR to 3820/4140 → net 3810 → 1112 → merchant bank. All transit = 0.

**Wire / field binding:** [`surface-map.md`](./surface-map.md) — S2 request fields và use-case step order.
