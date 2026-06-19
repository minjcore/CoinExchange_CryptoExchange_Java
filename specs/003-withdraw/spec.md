# Feature Specification: Withdraw

**Feature Branch**: `003-withdraw`
**Created**: 2026-06-18
**Status**: Draft
**UC**: UC-2 (Orchestrator Use-Case Contracts)

## User Scenarios & Testing

### User Story 1 — Withdraw Accepted, Funds Frozen (Priority: P1)

A USER member requests a withdrawal to a bank account. The system reserves the gross amount (principal + fee) from the wallet immediately and returns an accepted response. The member's available balance decreases instantly; frozen balance increases by the gross amount.

**Why this priority**: Core money-out flow. No payout can happen without a prior accepted freeze.

**Independent Test**: Submit a withdrawal request; verify wallet `available` decreases by gross and `frozen` increases by gross; verify 200 response returned before bank payout.

**Acceptance Scenarios**:

1. **Given** a member with sufficient available balance, **When** `createWithdrawal` is called with valid amount and fee, **Then** the system responds 200, `available` decreases by gross, `frozen` increases by gross.
2. **Given** a member with insufficient available balance, **When** `createWithdrawal` is called, **Then** the system rejects with `WALLET_INSUFFICIENT_BALANCE`.
3. **Given** the same `businessRef` is submitted twice, **When** both requests are processed, **Then** the wallet is frozen exactly once.

---

### User Story 2 — Payout Succeeds, Funds Settled (Priority: P2)

After the bank confirms the payout, the frozen funds are permanently debited from the wallet. The member's frozen balance returns to zero for this transaction.

**Why this priority**: Completes the money-out lifecycle; without settle, frozen funds are stuck.

**Independent Test**: After US1, simulate a bank payout success; verify `frozen` decreases and `available` is unchanged (net debit already happened on freeze).

**Acceptance Scenarios**:

1. **Given** a withdrawal with frozen funds, **When** bank confirms payout success, **Then** `WITHDRAW_SETTLE` debits the frozen amount; frozen returns to zero for this ref.
2. **Given** a settle command for an already-settled ref, **When** processed, **Then** idempotent no-op.

---

### User Story 3 — Payout Fails, Funds Released (Priority: P2)

After the bank confirms terminal failure or cancellation, the frozen funds are released back to available.

**Independent Test**: After US1, simulate a bank payout terminal failure; verify `frozen` decreases and `available` returns to pre-withdrawal value.

**Acceptance Scenarios**:

1. **Given** frozen funds and a terminal bank failure, **When** payout fails definitively, **Then** `WITHDRAW_RELEASE` unfreezes the gross amount; available returns to pre-withdrawal value.
2. **Given** an unknown/pending bank result, **When** timeout occurs, **Then** funds remain frozen — timeout is NOT a release signal.

---

### Edge Cases

- Wallet becomes LOCKED after freeze but before settle: reject settle; require ops resolution.
- Bank result is ambiguous (network timeout): poll until terminal; never auto-release.
- Partial settle amount differs from freeze amount: reject; amounts must match.

## Requirements

### Functional Requirements

- **FR-001**: System MUST freeze principal + fee (gross) from `available` and add to `frozen` synchronously before returning 200.
- **FR-002**: System MUST post the withdrawal acceptance in the ledger on transit account 3200 before returning 200.
- **FR-003**: System MUST dispatch a bank payout command asynchronously after the 200 response.
- **FR-004**: On bank payout success, system MUST debit frozen amount via `WITHDRAW_SETTLE` with ref `{businessRef}:settle`.
- **FR-005**: On bank payout terminal failure, system MUST unfreeze amount via `WITHDRAW_RELEASE` with ref `{businessRef}:release`.
- **FR-006**: System MUST NOT auto-release frozen funds on timeout — only on confirmed terminal failure.
- **FR-007**: Every async pending payout MUST have an aging job that polls for terminal status.
- **FR-008**: The entire flow MUST be idempotent on `businessRef`; duplicate requests return prior result.
- **FR-009**: Transit account 3200 MUST net to zero when the use case completes (settle or release).

### Key Entities

- **WithdrawAcceptance**: freeze + ledger posting; synchronous; returns 200.
- **PayoutCommand**: async dispatch to bank after accept; keyed by `businessRef`.
- **WITHDRAW_FREEZE** (`wallet_tx`): freeze gross on accept.
- **WITHDRAW_SETTLE** (`wallet_tx`): debit frozen on bank success; ref = `{businessRef}:settle`.
- **WITHDRAW_RELEASE** (`wallet_tx`): unfreeze on bank failure; ref = `{businessRef}:release`.

## Success Criteria

- **SC-001**: Member receives 200 in under 500 ms regardless of bank payout latency.
- **SC-002**: Transit 3200 nets to zero for every completed (settled or released) withdrawal.
- **SC-003**: Zero double-debits observed — each `businessRef` produces at most one settled debit.
- **SC-004**: No frozen funds remain permanently stuck — aging job resolves every pending payout within SLA.

## Assumptions

- Fee is computed by orchestration before freeze; wallet receives gross = principal + fee.
- Bank payout rail is external and may be slow or unreliable; polling SLA is defined by ops.
- LOCKED wallet rejects new freezes; already-frozen funds from prior request are handled by ops.
- VND only; single currency.
