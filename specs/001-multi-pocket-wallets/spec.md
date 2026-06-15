# Feature Specification: USER Multi-Pocket Wallets

**Feature Branch**: `001-multi-pocket-wallets`

**Created**: 2026-06-15

**Status**: Draft

**Input**: User description: "USER multi-pocket wallets (ngăn ví). A USER member can hold multiple wallet pockets distinguished by a user-set label and instantiated from a wallet_pocket_def catalog; MERCHANT/PARTNER stay single-wallet. Scope: list pocket definitions, create a pocket, close a pocket (requires zero balance, default pocket not deletable), pocket-to-pocket transfer within the same member, and balance/spend operations targeting a specific pocket. Authoritative aggregate stays in accounting control 2110 (all USER pockets roll up). Per ADR-040, ADR-020, ADR-039, ADR-004; processes.md §11A; OpenAPI pocket endpoints already drafted."

**Governing decisions**: [ADR-040](../../adr/ADR-040-user-multi-pocket-wallets.md) (primary), [ADR-020](../../adr/ADR-020-wallet-lanes-coa-control-mapping.md), [ADR-039](../../adr/ADR-039-no-synchronous-wallet-aggregate-row.md), [ADR-004](../../adr/ADR-004-wallet-balance-snapshot.md) · Behavior: [`processes.md` §11A](../../spec/processes.md) · Constitution: Principle I (two-domain), III (wallet hot path).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Organize money into named pockets (Priority: P1)

A USER opens the app and separates their money into purpose-named pockets ("Chi tiêu", "Tiết kiệm", "Quỹ du lịch") chosen from a catalog of pocket kinds the product offers. Each pocket holds its own spendable balance.

**Why this priority**: This is the core of the feature — without the ability to create and see named pockets, nothing else has value. It is the MVP.

**Independent Test**: A user with only a default pocket lists the available pocket kinds, creates one new pocket with a custom name, and sees it appear in their pocket list with a zero balance — fully demonstrable without any transfer or spend.

**Acceptance Scenarios**:

1. **Given** a USER with only the `default` pocket, **When** they request the catalog of pocket kinds, **Then** they receive the active pocket definitions (e.g. SPENDING, SAVINGS, GOAL) they may create from.
2. **Given** a USER and an active pocket definition `SAVINGS`, **When** they create a pocket labeled "Tiết kiệm", **Then** a new pocket exists with that label, zero available and zero frozen, and it appears in their pocket list.
3. **Given** a USER who already has a pocket of a definition whose rules forbid duplicates, **When** they try to create a second pocket of that definition, **Then** the request is rejected with a clear "already exists" reason.
4. **Given** a USER, **When** they try to create a pocket with a label that already exists for them, **Then** the request is rejected as a duplicate.
5. **Given** a USER creating a pocket of a definition that allows multiples (e.g. GOAL), **When** they create a second pocket with a different label, **Then** both pockets coexist.

---

### User Story 2 - Move money between my own pockets (Priority: P2)

A USER moves funds from one of their pockets to another (e.g. from "Chi tiêu" into "Tiết kiệm"), instantly, with no fee by default. The user's total money is unchanged — funds simply relocate between pockets.

**Why this priority**: Pockets are only useful if money can flow between them; this is the second most valuable capability after having pockets.

**Independent Test**: A user with two pockets (one funded) moves an amount to the other and sees the source decrease and destination increase by exactly that amount, with their total unchanged.

**Acceptance Scenarios**:

1. **Given** a USER with pocket A (available 100,000) and pocket B (available 0), **When** they move 60,000 from A to B, **Then** A shows 40,000, B shows 60,000, and the user's total across pockets is unchanged.
2. **Given** a USER with pocket A available 50,000, **When** they try to move 80,000 from A to B, **Then** the move is rejected for insufficient funds and no balance changes.
3. **Given** a USER, **When** they try to move funds from a pocket to the same pocket, **Then** the request is rejected.
4. **Given** a move that was already completed, **When** the same move request is replayed, **Then** it returns the original result without moving funds twice.
5. **Given** a USER whose source or destination pocket is locked or closed, **When** they attempt a move, **Then** it is rejected.

---

### User Story 3 - Spend / read balance from a chosen pocket (Priority: P2)

A USER pays or checks balance against a specific pocket. When no pocket is named, the default pocket is used.

**Why this priority**: Pockets must integrate with everyday spend/read; equal value to transfers for daily use.

**Independent Test**: A user queries balance with and without naming a pocket and gets the correct pocket's figures; a payment that names a pocket debits only that pocket.

**Acceptance Scenarios**:

1. **Given** a USER with pockets A and B, **When** they query balance naming pocket B, **Then** they receive B's available and frozen, not A's.
2. **Given** a USER who queries balance without naming a pocket, **When** the request is processed, **Then** the `default` pocket's balance is returned.
3. **Given** a USER paying and naming pocket A, **When** the payment succeeds, **Then** only pocket A is debited and other pockets are unchanged.

---

### User Story 4 - Close a pocket I no longer need (Priority: P3)

A USER closes an empty pocket they created. The default pocket cannot be closed.

**Why this priority**: Lifecycle housekeeping; valuable but not required for the core experience.

**Independent Test**: A user empties a pocket, closes it, and it no longer appears as active; closing a non-empty or default pocket is refused.

**Acceptance Scenarios**:

1. **Given** a USER pocket with zero available and zero frozen, **When** they close it, **Then** the pocket becomes closed and rejects further movement.
2. **Given** a USER pocket holding funds, **When** they try to close it, **Then** the request is rejected as "not empty".
3. **Given** the `default` pocket, **When** the user tries to close it, **Then** the request is rejected.
4. **Given** an already-closed pocket, **When** the close request is replayed, **Then** it is a no-op success.

### Edge Cases

- A pocket with funds **frozen** (in-flight outflow) is treated as non-empty and cannot be closed until the freeze settles or releases.
- Creating a pocket from an **inactive/unknown** definition is rejected.
- Multi-pocket applies to **USER only**: MERCHANT/PARTNER attempts to create a second wallet for the same type/currency are rejected.
- The member's **"total balance"** is the sum across their pockets, not a single stored number; a member with many pockets still reads each pocket as a single fast lookup.
- Concurrent creation of two pockets with the same label by the same member resolves to exactly one created, the other rejected as duplicate.
- A pocket-to-pocket move is internally a transfer; if the destination credit lags after the source debit, the system retries the credit and never double-debits.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: A USER MUST be able to retrieve the catalog of available pocket definitions (active only), each with a code, display name, whether it is the default, and whether multiple instances are allowed.
- **FR-002**: A USER MUST be able to create a pocket by referencing a pocket definition and providing a display label; the new pocket starts with zero available and zero frozen and moves no money (no ledger posting).
- **FR-003**: The system MUST reject pocket creation when the definition is unknown/inactive, when the definition forbids duplicates and one already exists for the member, or when the label duplicates an existing pocket for that member/type/currency.
- **FR-004**: Each pocket MUST maintain its own independent spendable balance; operations on one pocket MUST NOT affect or block other pockets of the same member.
- **FR-005**: A USER MUST be able to move funds between two of their own pockets synchronously, with a default fee of zero, debiting the source and crediting the destination by the same amount; the member's total across pockets MUST be unchanged by the move.
- **FR-006**: The system MUST reject a pocket-to-pocket move on insufficient source funds, on same source-and-destination, or when either pocket is locked or closed.
- **FR-007**: Balance reads and spend operations MUST target a specific pocket when one is identified, and MUST default to the member's `default` pocket when none is identified.
- **FR-008**: A USER MUST be able to close a pocket only when its available and frozen balances are both zero; the `default` pocket MUST NOT be closable; a closed pocket MUST reject further movement.
- **FR-009**: All USER pockets MUST roll up to the single accounting control account 2110; the authoritative aggregate liability MUST remain in accounting, never duplicated as a stored total in the wallet.
- **FR-010**: Multi-pocket MUST apply only to USER members; MERCHANT and PARTNER MUST remain limited to a single wallet per type and currency.
- **FR-011**: Every pocket operation that moves money (pocket-to-pocket, spend) MUST be idempotent on replay, returning the original outcome without re-applying.
- **FR-012**: Listing a member's pockets MUST return each pocket's label, definition, status, available and frozen.

### Key Entities *(include if feature involves data)*

- **Pocket Definition**: A catalog entry describing a kind of pocket the product offers — code, display name, whether it is the default, whether a member may hold more than one. Reference/config data; the source a pocket is created from.
- **Pocket**: A USER's instance of a pocket — belongs to one member, references one pocket definition, has a user-set label, a status (active / locked / closed), and its own spendable (available) and held (frozen) balances. A member may have many; MERCHANT/PARTNER have exactly one wallet per type/currency.
- **Pocket Movement**: A record of a balance change on a pocket (credit, debit, freeze, etc.), append-only, used to reconstruct balance and prove idempotency.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A USER can create a new named pocket and see it in their list in under 5 seconds, end to end.
- **SC-002**: A pocket-to-pocket move reflects the new balances on both pockets immediately on success, with the member's total across pockets unchanged in 100% of moves.
- **SC-003**: Reading a pocket balance returns within the same latency target as a single-wallet balance read, regardless of how many pockets the member holds (no degradation with pocket count).
- **SC-004**: 100% of replayed pocket moves and pocket creations produce no duplicate effect (no double-debit, no duplicate pocket).
- **SC-005**: At all times, the sum of all USER pockets across all members reconciles to the accounting control 2110 within the agreed reconciliation tolerance.
- **SC-006**: 0 occurrences of a MERCHANT/PARTNER holding more than one wallet per type/currency.
- **SC-007**: 0 occurrences of a closed or default-pocket rule being bypassed (no funds-bearing pocket closed; default never closed).

## Assumptions

- Pocket definitions are product/admin-curated reference data; their initial set (e.g. DEFAULT, SPENDING, SAVINGS, GOAL) is seeded and not user-editable in v1.
- Every USER member has exactly one `default` pocket, auto-provisioned, which always exists.
- Pocket-to-pocket transfers are free by default; any non-zero fee is a later product configuration and out of scope for v1 behavior.
- Single currency (VND) per the platform v1 scope; pockets do not introduce cross-currency conversion.
- The aggregate "system of record" remains the accounting ledger; this feature adds no new authoritative balance store (the wallet keeps per-pocket balances only).
- Existing deposit, payment, withdraw, and internal-transfer flows are reused; this feature specializes wallet addressing and adds pocket lifecycle, rather than inventing new money-movement types.
- The selection of which pocket an operation targets is resolved before the wallet acts (by explicit pocket id, by definition code/label, or by defaulting) — the wallet is never asked to guess.

## Dependencies

- Member identity / authentication (a pocket belongs to an authenticated member).
- The accounting control-account mapping for the USER lane (2110).
- The existing internal-transfer behavior, reused for pocket-to-pocket moves.
