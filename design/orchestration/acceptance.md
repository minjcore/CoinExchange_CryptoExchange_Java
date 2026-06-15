# Acceptance specs (language-agnostic)

Any runtime must match these observable outcomes. Full scenarios migrated from legacy `core.acceptance-specs.md`.

## Deposit

- POSTED + fee 1k on 100k → 2110 +99k, 3100=0, wallet USER +99k, `DEPOSIT_CREDIT`
- PENDING only → wallet unchanged
- Duplicate webhook → no double credit
- Credit retry after POSTED → ledger unchanged, single wallet effect

## Payment

- 100k pay → USER 0, MERCHANT +100k, 3500=0, both tx_types under one `businessRef`
- Insufficient → 422, no journal
- Post fail after debit → compensate USER

## Withdraw

- Freeze 101k → payout OK → settle, 3200=0
- Payout fail → release, available restored
- Timeout → hold stays until terminal bank result

## Transfer

- Gross debit A, net credit B, 3300=0, fee in 4130 not in wallet legs

## Cross-cutting

- `sum(DR)=sum(CR)` every journal
- Transit = 0 on completion
- No edit of posted lines / wallet_tx
- Idempotent replay vs 409 on amount conflict

See legacy [`../../_legacy/core.acceptance-specs.md`](../../_legacy/core.acceptance-specs.md) for full Gherkin until ported here.
