# Feature Specification: QR/POS Payment

**Feature Branch**: `007-qr-pos`
**Created**: 2026-06-18
**Status**: Draft
**UC**: UC-6 (Orchestrator Use-Case Contracts)

## User Scenarios & Testing

### User Story 1 — Acquirer Intake: Funds Recognized in Transit (Priority: P1)

A customer pays via QR or POS terminal. The acquirer confirms the payment; funds land in acquirer account 1113 and are held in transit 3500 pending EOD settlement. No wallet leg is created in v1.

**Why this priority**: Establishes the accounting record that EOD settlement depends on.

**Independent Test**: Process acquirer confirmation; verify 1113 debited, 3500 credited, merchant wallet unchanged, external bank fee recognized.

**Acceptance Scenarios**:

1. **Given** an acquirer payment confirmation, **When** processed, **Then** 1113 DR, 3500 CR, 5100 DR (bank fee), 1113 CR (fee offset).
2. **Given** a duplicate acquirer reference, **When** processed, **Then** idempotent — no duplicate journal line.

---

### User Story 2 — EOD Settlement: Transit Cleared to Merchant Liability (Priority: P2)

At end of day, the transit account 3500 is cleared and merchant liability 2120 is credited. MDR is recognized.

**Why this priority**: Completes the accounting cycle; without EOD, merchant liability is never recognized.

**Independent Test**: Run EOD settlement; verify 3500 DR, 2120 CR, MDR (4140) recognized; 3500=0 after settlement.

**Acceptance Scenarios**:

1. **Given** QR/POS transactions accumulated in 3500, **When** EOD settlement runs, **Then** 3500 cleared, 2120 credited (net of MDR), 4140 credited (MDR).
2. **Given** EOD run for an already-settled batch, **When** re-run, **Then** idempotent no-op.

---

### Edge Cases

- Acquirer file arrives late: process and clear to 3500; trigger EOD retry if missed window.
- Settlement mismatch between acquirer file and internal state: halt cycle, alert ops.
- Product later adds merchant wallet credit at EOD: use explicit `MERCHANT_SETTLE_CREDIT` tx_type; do not silently reuse existing types.

## Requirements

### Functional Requirements

- **FR-001**: On acquirer confirmation, credit transit 3500 and debit acquirer account 1113.
- **FR-002**: Recognize external bank fee (5100 DR) at acquirer intake time.
- **FR-003**: In v1, no per-transaction wallet leg is created for QR/POS payments.
- **FR-004**: EOD settlement MUST debit 3500 and credit merchant 2120 (net of MDR) and revenue 4140 (MDR).
- **FR-005**: EOD settlement MUST NOT reprocess already-settled transactions.
- **FR-006**: Settlement MUST halt if acquirer file totals do not reconcile with internal transit balance.

### Key Entities

- No `wallet_tx` in v1 for QR/POS.
- Journal accounts: 1113 (acquirer asset), 3500 (payment transit), 5100 (bank fee expense), 2120 (merchant liability), 4140 (MDR revenue).

## Success Criteria

- **SC-001**: Every acquirer payment recognized in accounting same day.
- **SC-002**: Transit 3500 nets to zero after each EOD settlement cycle.
- **SC-003**: MDR revenue recognized separately from net merchant settlement for every batch.

## Assumptions

- No wallet leg for QR/POS in v1; wallet credit (if introduced later) uses a new explicit tx_type.
- EOD runs as an independent scheduled batch (not inline with payment).
- VND only.
