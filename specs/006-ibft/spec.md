# Feature Specification: IBFT (Interbank Fund Transfer)

**Feature Branch**: `006-ibft`
**Created**: 2026-06-18
**Status**: Draft
**UC**: UC-5 (Orchestrator Use-Case Contracts)

## User Scenarios & Testing

### User Story 1 — Transfer Out Accepted, Funds Frozen (Priority: P1)

A USER member transfers funds to an external bank account via an interbank rail. The wallet freezes the gross amount synchronously and returns 200. The actual external payout is asynchronous.

**Why this priority**: Core outbound interbank flow; most complex outflow due to external rail uncertainty.

**Independent Test**: Submit IBFT; verify 200 returned, `available` decreases by gross, `frozen` increases by gross, ledger posts to transit 3400.

**Acceptance Scenarios**:

1. **Given** a member with sufficient balance, **When** `createTransfer` with external destination, **Then** 200 returned, gross frozen, ledger on 3400.
2. **Given** insufficient balance, **When** submitted, **Then** 422 rejected before any wallet mutation.
3. **Given** same `businessRef` twice, **When** processed, **Then** frozen exactly once.

---

### User Story 2 — Payout Succeeds, Funds Settled (Priority: P2)

After the interbank rail confirms the outbound transfer, frozen funds are permanently settled (debited).

**Acceptance Scenarios**:

1. **Given** frozen IBFT and rail success confirmation, **When** settle runs, **Then** `IBFT_SETTLE` debits frozen amount; 3400=0.
2. **Given** settle for already-settled ref, **When** processed, **Then** idempotent no-op.

---

### User Story 3 — Payout Fails, Funds Released (Priority: P2)

After confirmed terminal failure from the interbank rail, frozen funds are released.

**Acceptance Scenarios**:

1. **Given** frozen IBFT and terminal rail failure, **When** release runs, **Then** `IBFT_RELEASE` unfreezes amount; available restored.
2. **Given** unknown/timeout from rail, **When** timeout occurs, **Then** funds remain frozen — timeout is NOT a release signal.

---

### Edge Cases

- Rail acknowledges duplicate payout: bank adapter deduplicates on `businessRef`.
- Partial settle amount: reject; amounts must match the original freeze.
- LOCKED wallet during payout: ops resolution required; no auto-release.

## Requirements

### Functional Requirements

- **FR-001**: Freeze gross amount synchronously; return 200 before bank payout.
- **FR-002**: Post ledger acceptance on transit 3400 before returning 200.
- **FR-003**: Dispatch outbound interbank payout asynchronously.
- **FR-004**: On rail success: `IBFT_SETTLE` with ref `{businessRef}:settle`.
- **FR-005**: On terminal rail failure: `IBFT_RELEASE` with ref `{businessRef}:release`.
- **FR-006**: MUST NOT auto-release on timeout — poll until terminal status.
- **FR-007**: Every pending IBFT MUST have an aging job and polling path.
- **FR-008**: Recognize external bank fee (5100 DR) and net fee revenue (4130 CR) in accounting.
- **FR-009**: Transit 3400 MUST net to zero when use case completes.
- **FR-010**: Idempotent on `businessRef`.

### Key Entities

- **IBFT_FREEZE** (`wallet_tx`): freeze gross on accept.
- **IBFT_SETTLE** (`wallet_tx`): debit frozen on success; ref = `{businessRef}:settle`.
- **IBFT_RELEASE** (`wallet_tx`): unfreeze on failure; ref = `{businessRef}:release`.

## Success Criteria

- **SC-001**: 200 response in under 500 ms.
- **SC-002**: Transit 3400 nets to zero for every completed IBFT.
- **SC-003**: Zero double-debits; each `businessRef` produces at most one settled debit.
- **SC-004**: No permanently frozen funds — aging job resolves every pending IBFT within SLA.

## Assumptions

- External payout rail is asynchronous with non-deterministic latency.
- Bank fee (5100) is separate from platform fee (4130); both recognized in accounting.
- VND only.
