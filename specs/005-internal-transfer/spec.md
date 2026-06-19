# Feature Specification: Internal Transfer

**Feature Branch**: `005-internal-transfer`
**Created**: 2026-06-18
**Status**: Draft
**UC**: UC-4 (Orchestrator Use-Case Contracts)

## User Scenarios & Testing

### User Story 1 — Transfer Between Two Members (Priority: P1)

A USER member transfers value to another USER member within the platform. Sender's available balance decreases by gross (principal + fee); receiver's available balance increases by net (principal only). Operation is synchronous.

**Why this priority**: Core P2P money movement; enables intra-platform fund distribution.

**Independent Test**: Transfer 101,000 VND (fee 1,000) from A to B; verify A decreases 101,000, B increases 100,000, transit 3300=0.

**Acceptance Scenarios**:

1. **Given** sender A with sufficient balance and valid receiver B, **When** `createTransfer` (internal destination), **Then** A debited 101,000, B credited 100,000, transit 3300=0, 200 returned.
2. **Given** sender with insufficient balance, **When** transfer submitted, **Then** 422 rejected; no state change.
3. **Given** same `businessRef` submitted twice, **When** processed, **Then** exactly one debit/credit pair.

---

### User Story 2 — Compensation on Ledger Failure (Priority: P2)

If ledger posting fails after sender debit, sender receives a compensating credit. Receiver is not credited.

**Acceptance Scenarios**:

1. **Given** sender debited but ledger fails, **When** compensation runs, **Then** `ADJUSTMENT_CREDIT` ref `{businessRef}:comp` restores sender balance.
2. **Given** receiver credit fails after POSTED, **When** retry runs, **Then** forward retry until receiver credited; ledger not reversed.

---

### Edge Cases

- Sender and receiver are the same member: reject.
- Receiver wallet not provisioned: provision before credit.
- LOCKED sender: reject at debit step.

## Requirements

### Functional Requirements

- **FR-001**: Ordered steps: (1) sender debit gross, (2) ledger post transit 3300, (3) receiver credit net.
- **FR-002**: Return 200 only after all three complete.
- **FR-003**: Compensate sender if ledger fails post-debit via `ADJUSTMENT_CREDIT`.
- **FR-004**: Forward retry receiver credit if it fails post-POSTED.
- **FR-005**: Transit 3300 MUST net to zero after POSTED.
- **FR-006**: Idempotent on `businessRef`.
- **FR-007**: Sender and receiver MUST be different members.

### Key Entities

- **TRANSFER_DEBIT** (`wallet_tx`): debit sender, step 1.
- **TRANSFER_CREDIT** (`wallet_tx`): credit receiver, step 3.
- Fee recognized in accounting only (4130 CR); no separate wallet leg for fee.

## Success Criteria

- **SC-001**: Transfer completes in under 1 second.
- **SC-002**: Transit 3300 nets to zero for every completed transfer.
- **SC-003**: Zero double-debits on replay.

## Assumptions

- Fee deducted from sender; receiver gets net. Fee revenue recognized in accounting (4130).
- VND only.
