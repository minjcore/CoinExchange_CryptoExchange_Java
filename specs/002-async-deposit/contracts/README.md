# Contracts: Async Deposit

**Feature**: `002-async-deposit` | **Date**: 2026-06-18

Wire contracts live in the main repo. This directory is a navigation index.

---

## Relevant contracts

| Contract | Location | What to verify |
|----------|----------|---------------|
| `BANK_DEPOSIT` command envelope | `spec/contracts/async-api/core-commands.yaml` | `commandType: BANK_DEPOSIT`; `businessRef`; `memberId`; `grossAmount`; `virtualAccount`; `fee` |
| `WALLET_CREDIT` command envelope | `spec/contracts/async-api/core-commands.yaml` | `commandType: WALLET_CREDIT`; `businessRef`; `walletId`; `netAmount`; `coaTransId` |
| `JournalPosted` event | `spec/contracts/async-api/core-events.yaml` | `eventType: JournalPosted`; `useCase: DEPOSIT`; `businessRef`; `coaTransId` |
| `WalletCredited` event | `spec/contracts/async-api/core-events.yaml` | `eventType: WalletCredited`; `businessRef`; `walletId`; `netAmount` |
| `core.operations.command-failed` event | `spec/contracts/async-api/core-events.yaml` | `businessRef`; `commandType`; `reason` |
| Deposit notify (s1-http-public inbound) | `spec/contracts/open-api/gtelpay-public.yaml` | `POST /deposits/notify` → 202 + `businessRef` |
| Accounting internal | `spec/contracts/open-api/accounting-internal.yaml` | `createJournal`, `confirmDeposit` — unchanged wire (ADR-037 AC-037-02) |

---

## Required additions to `core-commands.yaml`

The `BANK_DEPOSIT` envelope (ADR-041) needs these fields confirmed:

```yaml
BANK_DEPOSIT:
  businessRef: string        # X-Idempotency-Key end-to-end
  memberId: string           # resolved from virtual account
  virtualAccount: string     # raw VA from bank notification
  grossAmount: string        # decimal string, scale 4 (e.g. "100000.0000")
  fee: string                # decimal string, computed by orchestration
  currency: string           # "VND" always
  bankRef: string            # bank's own reference (for audit)
```

`WALLET_CREDIT`:

```yaml
WALLET_CREDIT:
  businessRef: string
  walletId: string           # resolved pocket wallet_id
  netAmount: string          # grossAmount − fee
  coaTransId: string         # optional correlation
  currency: string
```

---

## Validation checklist

- [ ] `BANK_DEPOSIT` schema present and matches fields above in `core-commands.yaml`
- [ ] `WALLET_CREDIT` schema present in `core-commands.yaml`
- [ ] `JournalPosted` includes `useCase` field in `core-events.yaml`
- [ ] `WalletCredited` schema present in `core-events.yaml`
- [ ] `accounting-internal.yaml` `confirmDeposit` endpoint unchanged
- [ ] `gtelpay-public.yaml` deposit notify returns 202 with `businessRef`
