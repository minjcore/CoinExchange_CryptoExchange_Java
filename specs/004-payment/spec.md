# Feature Specification: Wallet Payment

**Feature Branch**: `004-payment`
**Created**: 2026-06-18
**Status**: Draft
**UC**: UC-3 (Orchestrator Use-Case Contracts) — Implemented

## User Scenarios & Testing

### User Story 1 — User Pays Merchant (Priority: P1)

A USER member pays a MERCHANT from their wallet. The user's available balance decreases by the gross amount; the merchant's available balance increases by the net amount. The entire operation is synchronous and returns 200 only after all three commits complete.

**Why this priority**: Core revenue-generating flow. Payment is the primary P1 MVP.

**Independent Test**: Submit a payment; verify user `available` decreases by gross, merchant `available` increases by net, ledger transit 3500 nets to zero, all in one 200 response.

**Acceptance Scenarios**:

1. **Given** a user with sufficient balance and a valid merchant, **When** `POST /v1/payments` is called, **Then** user is debited gross, merchant is credited net, ledger 3500=0, response 200.
2. **Given** a user with insufficient balance, **When** payment is submitted, **Then** 422 `WALLET_INSUFFICIENT_BALANCE`; no partial state.
3. **Given** the same `businessRef` submitted twice, **When** both are processed, **Then** exactly one debit and one credit; second call returns prior result.

---

### User Story 2 — Compensation on Ledger Failure (Priority: P2)

If the ledger post fails after the user debit, the user receives a compensating credit to restore their balance. No partial state is exposed.

**Independent Test**: Inject ledger failure after user debit; verify compensating `ADJUSTMENT_CREDIT` restores user balance; merchant not credited.

**Acceptance Scenarios**:

1. **Given** user debited but ledger post fails, **When** compensation runs, **Then** `ADJUSTMENT_CREDIT` with ref `{businessRef}:comp` restores user balance exactly.
2. **Given** merchant credit fails after ledger POSTED, **When** retry runs, **Then** merchant credit retried forward idempotently; ledger not reversed.

---

### Edge Cases

- Payer and payee are the same member: reject.
- Net amount differs from gross in v1 (no fee policy defined): reject unless a valid fee policy is active.
- LOCKED payer wallet: reject at debit step; no ledger entry created.
- Merchant wallet not yet provisioned: provision before credit.

## Requirements

### Functional Requirements

- **FR-001**: System MUST execute in order: (1) user debit, (2) ledger post transit 3500, (3) merchant credit.
- **FR-002**: System MUST return 200 only after all three steps complete successfully.
- **FR-003**: On ledger failure after user debit, system MUST compensate with `ADJUSTMENT_CREDIT` ref `{businessRef}:comp`.
- **FR-004**: On merchant credit failure after POSTED, system MUST retry credit forward; MUST NOT reverse the ledger.
- **FR-005**: Transit 3500 MUST net to zero after POSTED.
- **FR-006**: System MUST be idempotent on `businessRef`; duplicate calls return prior result.
- **FR-007**: Insufficient balance MUST fail before any wallet mutation.
- **FR-008**: Response MUST include `walletTxId` and `coaTransId`.

### Key Entities

- **PAYMENT_DEBIT** (`wallet_tx`): debit user, step 1.
- **PAYMENT_CREDIT** (`wallet_tx`): credit merchant, step 3.
- **ADJUSTMENT_CREDIT** (`wallet_tx`): compensation if ledger fails after step 1.

## Success Criteria

- **SC-001**: Payment completes in under 1 second under normal load.
- **SC-002**: Transit 3500 nets to zero for every completed payment.
- **SC-003**: Zero partial states visible to client — either full success or full rollback.
- **SC-004**: Duplicate `businessRef` never produces double-debit.

## Assumptions

- In v1, net = gross (no per-transaction MDR on wallet payment; fee is 0 or externally handled).
- Merchant wallet is provisioned on first payment if absent.
- VND only.
