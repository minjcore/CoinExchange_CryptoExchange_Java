# Feature Specification: EOD Settlement & Clearing

**Feature Branch**: `010-eod-settlement`
**Created**: 2026-06-18
**Status**: Draft
**UC**: UC-9 (Orchestrator Use-Case Contracts)

## User Scenarios & Testing

### User Story 1 — Reconcile and Lock Merchant Liabilities (Priority: P1)

At end of day, the system reconciles the acquirer settlement file against the internal merchant-wallet control snapshot. If they match, merchant liabilities in 2120 are locked and moved to the settlement transit 3800.

**Why this priority**: Gate step — no settlement can proceed until reconciliation passes.

**Independent Test**: Run EOD with matching acquirer file; verify 2120 balances moved to 3800 per merchant; cycle locked for the settlement date.

**Acceptance Scenarios**:

1. **Given** acquirer file matches internal snapshot, **When** EOD runs, **Then** per-merchant 2120 → 3800; settlement date locked.
2. **Given** mismatch between file and snapshot, **When** EOD runs, **Then** cycle halts without moving any liabilities; ops alerted.
3. **Given** same settlement date re-run, **When** processed, **Then** idempotent on `(merchantId, settlementDate)`.

---

### User Story 2 — MDR Split and Net Payable (Priority: P2)

After locking, MDR is recognized and the net payable per merchant is moved to the settlement account 3810. MDR revenue goes to 3820.

**Independent Test**: Split 100,000 by MDR 1%; verify 3810 = 99,000, 3820 = 1,000, 3800 = 0.

**Acceptance Scenarios**:

1. **Given** locked 3800 balance, **When** MDR split runs, **Then** 3820 (MDR) and 3810 (net payable) sum equals original 3800; 3800=0.

---

### User Story 3 — Execute Bank Payout (Priority: P2)

Net payable in 3810 is transferred to merchant bank accounts. Failed payouts remain in 3810 for retry.

**Acceptance Scenarios**:

1. **Given** net payable in 3810, **When** bank payout executes, **Then** 3810 DR, 1112 CR; merchant bank credited.
2. **Given** payout failure, **When** it fails, **Then** amount stays in 3810; retry later without re-running reconciliation or MDR split.

---

### Edge Cases

- Acquirer file arrives after EOD window: process next day; do not retroactively alter closed periods.
- Partial payout failures: keep remaining balance in 3810; retry only failed items.
- Period already closed: reject new entries; create reversing journal if correction needed.

## Requirements

### Functional Requirements

- **FR-001**: Reconcile acquirer file totals against internal merchant-wallet control snapshot before any mutation.
- **FR-002**: HALT the EOD cycle if reconciliation fails; do not lock or move any liabilities.
- **FR-003**: Per-merchant, move 2120 → 3800 (lock) exactly once per settlement date.
- **FR-004**: Split 3800 into 3820 (MDR revenue) and 3810 (net payable).
- **FR-005**: Execute bank payout 3810 → 1112 per merchant.
- **FR-006**: On payout failure, keep balance in 3810 for safe retry; do not re-run reconciliation or MDR split.
- **FR-007**: Idempotent on `(merchantId, settlementDate)`.
- **FR-008**: Recognize MDR revenue (4140) at MDR split time.
- **FR-009**: Transit accounts 3800/3810/3820 MUST net to zero when settlement cycle completes.
- **FR-010**: EOD runs as an independent scheduled batch; MUST NOT be triggered by or block payment requests.

### Key Entities

- No per-transaction `wallet_tx` in v1 for QR/POS settlement.
- Accounting: 2120 (merchant liability), 3800 (lock), 3820 (MDR), 3810 (net payable), 1112 (bank asset), 4140 (MDR revenue).
- Settlement cycle key: `(merchantId, settlementDate)`.

## Success Criteria

- **SC-001**: EOD halts 100% of the time when reconciliation fails — zero silent discrepancies settled.
- **SC-002**: Transit 3800/3810/3820 all net to zero after completed settlement.
- **SC-003**: MDR revenue recognized separately from net payable for every merchant in every cycle.
- **SC-004**: Failed bank payouts retryable without re-running reconciliation.

## Assumptions

- EOD is scheduled (not event-driven); runs once per business day.
- Acquirer settlement file is pre-loaded into the system before EOD runs.
- Multiple acquirers may need separate cycles; v1 supports one acquirer lane.
- VND only.
