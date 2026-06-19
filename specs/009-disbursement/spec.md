# Feature Specification: Disbursement

**Feature Branch**: `009-disbursement`
**Created**: 2026-06-18
**Status**: Draft
**UC**: UC-8 (Orchestrator Use-Case Contracts)

## User Scenarios & Testing

### User Story 1 — Partner Pre-funds Escrow (Priority: P1)

A PARTNER member pre-loads funds into their escrow account (COA 2130) before disbursement. This increases the partner's available balance and records the liability.

**Why this priority**: No disbursement can happen without a pre-funded escrow balance.

**Independent Test**: Submit a prefund; verify partner `available` increases; accounting debits asset account and credits 2130.

**Acceptance Scenarios**:

1. **Given** a partner submits a prefund, **When** processed, **Then** partner wallet credited, 2130 liability recognized.
2. **Given** same prefund `businessRef` twice, **When** processed, **Then** idempotent — credited once.

---

### User Story 2 — Partner Disburses to Recipients (Priority: P2)

The partner disburses funds to individual recipients. Each recipient is processed independently. One failed recipient does not block others.

**Why this priority**: Core disbursement flow; delivers value to end recipients.

**Independent Test**: Disburse to 3 recipients; verify partner wallet debited per-recipient; each payout dispatched independently.

**Acceptance Scenarios**:

1. **Given** partner with sufficient balance, **When** disbursement submitted for N recipients, **Then** per-recipient debit `{businessRef}:{recipientId}`, transit 3700 posted, payouts dispatched.
2. **Given** insufficient balance for recipient, **When** that recipient's debit runs, **Then** that recipient fails; others continue.
3. **Given** failed recipient retried, **When** processed, **Then** idempotent on `{businessRef}:{recipientId}`.

---

### User Story 3 — Partial Completion is Valid (Priority: P3)

A disbursement batch where some recipients succeed and some fail is a valid terminal state.

**Acceptance Scenarios**:

1. **Given** partial success, **When** batch queried, **Then** per-recipient status returned.
2. **Given** failed recipients retried, **When** processed, **Then** successful recipients not re-processed.

---

### Edge Cases

- Partner wallet balance goes below recipient amount mid-batch: that recipient fails; prior ones stand.
- Duplicate recipientId in batch: treat as separate items if different `{businessRef}:{recipientId}`.

## Requirements

### Functional Requirements

- **FR-001**: Optional prefund step: credit partner wallet and debit 2130 escrow.
- **FR-002**: Validate partner available balance before each recipient debit.
- **FR-003**: Per-recipient debit with key `{businessRef}:{recipientId}` using `DISBURSEMENT_DEBIT`.
- **FR-004**: Post per-recipient leg to ledger transit 3700.
- **FR-005**: One failed recipient MUST NOT block others.
- **FR-006**: Recognize disbursement fee (4150 CR) and external bank fee (5100 DR) per recipient.
- **FR-007**: Transit 3700 MUST net to zero when all recipients reach terminal state.
- **FR-008**: Idempotent on `{businessRef}:{recipientId}`.

### Key Entities

- **PARTNER_PREFUND_CREDIT** (`wallet_tx`): credit partner on prefund.
- **DISBURSEMENT_DEBIT** (`wallet_tx`): per-recipient debit.

## Success Criteria

- **SC-001**: Each recipient payout independently retryable.
- **SC-002**: Transit 3700 nets to zero at batch terminal state.
- **SC-003**: Partial completion returns clear per-recipient status.

## Assumptions

- Prefund is optional; partner may already have balance from prior prefunds.
- Recipient destination is an external bank account.
- VND only.
