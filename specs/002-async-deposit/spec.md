# Feature Specification: Async Bank Deposit

**Feature Branch**: `002-async-deposit`

**Created**: 2026-06-18

**Status**: Draft

**Input**: User description: "Async Deposit flow: USER initiates a bank deposit into their GtelPay wallet via NAPAS 247 / virtual account. Orchestration returns 202 immediately, publishes an S6 command envelope. Accounting consumer processes the command: two-phase posting (transit 3100 → wallet credit via confirmDeposit). Wallet is credited only after ledger POSTED. Kafka events emitted: JournalPosted, WalletCredited, or core.operations.command-failed on failure. Idempotent on businessRef end-to-end."

## User Scenarios & Testing *(mandatory)*

### User Story 1 — Deposit Lands in Wallet (Priority: P1)

A USER member receives a bank transfer (NAPAS 247 or virtual account top-up) and, after the bank confirms receipt, sees their GtelPay wallet balance increase by the net deposit amount (gross minus any fee). The member does not wait while the ledger records the transaction.

**Why this priority**: Core value proposition — money in. Nothing else works without this.

**Independent Test**: Send a NAPAS 247 notification, observe wallet balance increases by net amount; check that the system returns 202 to the notifier before the ledger write completes.

**Acceptance Scenarios**:

1. **Given** a member with a mapped virtual account, **When** a bank deposit notification arrives for that VA, **Then** the system acknowledges with 202 and the member's wallet balance increases by the net amount only after the journal is POSTED.
2. **Given** the same deposit notification is delivered twice with the same `businessRef`, **When** both deliveries are processed, **Then** the wallet is credited exactly once (idempotent replay).
3. **Given** a deposit that includes a fee, **When** the journal is POSTED, **Then** the wallet is credited with the gross amount minus the fee (net).

---

### User Story 2 — Unknown Virtual Account Held for Ops (Priority: P2)

A bank notification arrives for a virtual account that is not mapped to any member. The system holds the deposit in a PENDING state for manual ops resolution rather than crediting an incorrect wallet or losing the funds.

**Why this priority**: Prevents erroneous credits and fund loss; required for regulatory integrity.

**Independent Test**: Send a deposit notification for an unmapped VA; confirm no wallet credit occurs and the journal remains PENDING for ops review.

**Acceptance Scenarios**:

1. **Given** a bank deposit notification with a VA not in the mapping table, **When** processed, **Then** no wallet credit is issued and the deposit stays in PENDING state for ops.
2. **Given** a mapped VA deposit followed by a VA mapping change, **When** the mapping is updated, **Then** historical POSTED journals are not retroactively altered.

---

### User Story 3 — Deposit Failure and PENDING Aging (Priority: P3)

A deposit notification is received but the confirmation (phase B) never arrives — due to a bank mismatch or cancellation. The system ages the PENDING journal and can reverse it without affecting the wallet (which was never credited).

**Why this priority**: Prevents stuck PENDING deposits from polluting the trial balance indefinitely.

**Independent Test**: Create a phase-A PENDING journal; cancel it; confirm transit 3100 nets to zero and wallet balance is unchanged throughout.

**Acceptance Scenarios**:

1. **Given** a deposit in PENDING state, **When** the bank cancels or a timeout triggers reversal, **Then** the phase-A reversal clears 1111 and 3100 only; wallet balance is unchanged.
2. **Given** a deposit notification and a bank mismatch on amount, **When** the confirmation step fails validation, **Then** the phase-A journal is reversed and no phase-B POSTED record is created.

---

### Edge Cases

- What happens when the accounting worker crashes between phase A and phase B? (At-least-once redelivery must be safe; the worker must be idempotent on `(commandType, businessRef)`.)
- What happens when the wallet is LOCKED or CLOSED at the time of wallet credit? (Credit must be rejected per W-O1; the POSTED journal already exists — ops must resolve.)
- What happens when phase B `confirmDeposit` is called on an already-POSTED journal? (Idempotent no-op — return current state, no duplicate lines.)
- What happens when the deposit amount is zero or negative? (Reject at notification intake; do not create a journal.)

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST return 202 to the deposit notifier before the accounting journal is written.
- **FR-002**: System MUST resolve the virtual account in the deposit notification to a `memberId` and USER wallet before creating any journal entry.
- **FR-003**: System MUST create a PENDING journal (phase A: `1111 DR` gross + `3100 CR` gross) on receipt of a valid deposit notification.
- **FR-004**: System MUST credit the member wallet only after the journal status is POSTED (phase B complete).
- **FR-005**: System MUST encapsulate phase B posting inside `JournalService.confirmDeposit(coaTransId, fee)` — orchestration MUST NOT construct phase-B lines directly.
- **FR-006**: System MUST verify that transit account 3100 nets to zero after phase B before marking the journal POSTED.
- **FR-007**: The wallet credit amount MUST equal gross amount minus fee (net), with fee computed by orchestration.
- **FR-008**: The entire flow MUST be idempotent end-to-end on `businessRef` — duplicate notifications, duplicate queue deliveries, and duplicate wallet-credit commands MUST each produce at most one credit.
- **FR-009**: System MUST publish the deposit command via outbox-backed messaging so that an orchestration crash after 202 does not lose the command.
- **FR-010**: System MUST route deposits for unmapped virtual accounts to an ops hold queue — no erroneous wallet credit or journal for an unresolved VA.
- **FR-011**: Phase-A reversal (cancel/mismatch) MUST clear only the 1111/3100 lines and MUST NOT affect wallet balance.
- **FR-012**: Wallet credit MUST be delivered via one of three paths (Kafka `JournalPosted`, RabbitMQ `WALLET_CREDIT`, or HTTP sync) as chosen by the deployment; all paths MUST gate on POSTED status before crediting.

### Key Entities

- **Deposit Notification**: Inbound event from a bank/NAPAS gateway carrying VA, gross amount, bank reference, and `businessRef`.
- **Virtual Account Mapping**: Orchestration-owned table mapping VA → `memberId` + wallet lane.
- **PENDING Journal** (`coa_trans` status=PENDING): Phase-A record holding gross in transit 3100. Not yet authoritative for the member's liability.
- **POSTED Journal** (`coa_trans` status=POSTED): Phase-B record. Transit 3100 nets to zero; liability 2110 and fee 4110 are written. Authoritative. Immutable.
- **Wallet Credit** (`wallet_tx` tx_type=`DEPOSIT_CREDIT`): Net amount credited to member's wallet; created only after POSTED.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: The deposit notifier receives a 202 acknowledgment in under 200 ms, regardless of how long the ledger write takes.
- **SC-002**: A member's wallet balance reflects the net deposit amount within 5 seconds of the journal being POSTED under normal system load.
- **SC-003**: Duplicate deposit notifications with the same `businessRef` result in exactly one wallet credit — zero double-credits observed in soak testing.
- **SC-004**: 100% of POSTED deposit journals have transit account 3100 at net zero — verifiable by the SQL invariant suite (ADR-031 INV-03).
- **SC-005**: Deposits for unmapped VAs never produce a wallet credit — zero erroneous credits in ops audit.
- **SC-006**: An accounting worker crash and restart during phase B results in correct eventual completion with no duplicate journal lines.

## Assumptions

- Virtual account → member mapping already exists in the orchestration database before the deposit notification arrives; this feature does not spec the VA provisioning flow.
- Fee for deposit is computed by orchestration at notification time; the accounting worker receives the final net amount and does not re-compute fees.
- The bank webhook is authenticated via mTLS (ADR-022); webhook authentication is out of scope for this spec.
- `businessRef` in the deposit notification equals the `X-Idempotency-Key` used throughout the flow (ADR-005).
- v1 uses RabbitMQ workers, not Temporal (ADR-035); Kafka `JournalPosted` event emission after POSTED is optional in v1 but the wallet credit path must support all three paths per ADR-024.
- LOCKED/CLOSED wallet at credit time is an ops-resolution scenario; this spec covers detection and rejection, not automatic remediation.
- Currency is VND only (ADR-019); multi-currency deposit is out of scope.
