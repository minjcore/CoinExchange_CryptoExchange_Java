# Feature Specification: Payroll

**Feature Branch**: `008-payroll`
**Created**: 2026-06-18
**Status**: Draft
**UC**: UC-7 (Orchestrator Use-Case Contracts)

## User Scenarios & Testing

### User Story 1 — Merchant Submits Payroll Batch (Priority: P1)

A MERCHANT submits a batch of recipient payouts. The merchant wallet is debited the gross batch amount in a single step. The batch is posted to ledger transit 3600. Individual recipient payouts are dispatched asynchronously.

**Why this priority**: Gate step — without merchant debit, no recipient payouts can proceed.

**Independent Test**: Submit a payroll batch; verify merchant available decreases by gross; transit 3600 credited; batch accepted with a batch reference.

**Acceptance Scenarios**:

1. **Given** a merchant with sufficient balance and a valid recipient list, **When** batch is submitted, **Then** merchant debited gross once, ledger posts to 3600, batch accepted.
2. **Given** merchant with insufficient balance, **When** submitted, **Then** rejected before any debit.
3. **Given** same batch `businessRef` resubmitted, **When** processed, **Then** idempotent — single debit.

---

### User Story 2 — Per-Recipient Payout Dispatched (Priority: P2)

Each recipient in the batch receives an independent payout via the external rail. Each recipient's payout is keyed independently. One failed recipient does not block others.

**Independent Test**: Batch of 3 recipients; simulate one failure; verify 2 succeed and 1 fails with its own error; merchant debit unchanged.

**Acceptance Scenarios**:

1. **Given** batch accepted with 3 recipients, **When** payouts processed, **Then** each dispatched independently with key `{businessRef}:{recipientId}`.
2. **Given** one recipient payout fails, **When** batch status queried, **Then** failed recipient marked failed; other recipients unaffected.
3. **Given** failed recipient retried, **When** processed, **Then** idempotent on `{businessRef}:{recipientId}`.

---

### User Story 3 — Partial Success is a Valid Outcome (Priority: P3)

A batch where some recipients succeed and some fail is a valid terminal state. Failed items can be retried without re-debiting the merchant.

**Acceptance Scenarios**:

1. **Given** a partially completed batch, **When** failed items are retried, **Then** only failed items are retried; successful items are not re-processed.
2. **Given** a batch with all items failed, **When** retried, **Then** merchant is NOT re-debited.

---

### Edge Cases

- Recipient bank account invalid: mark failed immediately; move to next recipient.
- Merchant wallet LOCKED mid-batch: stop new dispatches; keep already-dispatched items running.
- Duplicate recipient in same batch: treat as separate line items if different `recipientId`.

## Requirements

### Functional Requirements

- **FR-001**: Validate merchant has sufficient available balance for batch gross before any debit.
- **FR-002**: Debit merchant wallet exactly once for the full batch gross with `PAYROLL_DEBIT`.
- **FR-003**: Post batch to ledger transit 3600.
- **FR-004**: Dispatch each recipient payout independently, keyed `{businessRef}:{recipientId}`.
- **FR-005**: One failed recipient MUST NOT block or roll back other recipients.
- **FR-006**: Recognize payroll fee (4150 CR) and external bank fee (5100 DR) in accounting.
- **FR-007**: Transit 3600 MUST net to zero when all recipients in the batch reach a terminal state.
- **FR-008**: Idempotent on batch `businessRef` and per-recipient key.

### Key Entities

- **PAYROLL_DEBIT** (`wallet_tx`): single debit of merchant for full batch gross.
- Batch status: tracks per-recipient outcomes (succeeded, failed, retrying).

## Success Criteria

- **SC-001**: Merchant debit occurs exactly once per batch regardless of recipient outcomes.
- **SC-002**: Each recipient payout is independently retryable without re-debiting merchant.
- **SC-003**: Transit 3600 nets to zero when batch reaches terminal state.
- **SC-004**: Partial completion returned as valid response with per-recipient status.

## Assumptions

- Recipient payout destination is an external bank account (not a GtelPay wallet).
- Batch size is bounded; no streaming/chunking in v1.
- VND only.
