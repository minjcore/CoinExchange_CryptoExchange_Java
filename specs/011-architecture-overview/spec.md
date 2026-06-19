# Feature Specification: Architecture Overview

**Feature Branch**: `011-architecture-overview`

**Created**: 2026-06-18

**Status**: Draft

**Input**: User description: "describe architecture"

## User Scenarios & Testing *(mandatory)*

### User Story 1 — Engineer Understands Domain Boundaries Before Coding (Priority: P1)

An engineer picking up a feature task can find a single document explaining the two-domain separation (wallet vs accounting), module layout, and dependency rules — without reading 41 individual ADRs.

**Why this priority**: The two-domain boundary is the most violated invariant when engineers don't have a clear reference. Missing this leads to cross-schema JOINs, cross-domain imports, and broken CI.

**Independent Test**: Hand the document to a new engineer; ask them to name: (a) which module owns wallet balance, (b) which module owns journal posting, (c) whether accounting can call wallet directly. All three must be answered correctly.

**Acceptance Scenarios**:

1. **Given** a new engineer joining the project, **When** they read the architecture overview, **Then** they can correctly identify which domain owns any given table or operation without consulting ADRs.
2. **Given** a code reviewer, **When** they find a suspicious cross-domain call, **Then** they can cite a specific rule from the architecture overview to block it.
3. **Given** the architecture document, **When** a module dependency is added to a PR, **Then** the document is sufficient to determine if the dependency is allowed.

---

### User Story 2 — Team Understands TigerBeetle and Blnk Choices (Priority: P2)

A developer or architect can understand why TigerBeetle is used for accounting postings and what the Blnk PoC adds to the wallet domain — without searching external documentation.

**Why this priority**: TigerBeetle and Blnk are non-standard choices. Without clear rationale, engineers will route around them or re-introduce PostgreSQL-only patterns that defeat the design.

**Independent Test**: Ask a developer unfamiliar with TigerBeetle: "What happens when you call `post_pending_transfer`?" and "Why doesn't accounting use `SUM(amount)` over coa_trans_data?" The document must provide both answers.

**Acceptance Scenarios**:

1. **Given** a developer new to TigerBeetle, **When** they read the overview, **Then** they can explain the pending/post/void transfer lifecycle and why TB accounts provide O(1) balance reads.
2. **Given** a question about why `WalletBalanceMonitor` exists, **When** the developer reads the Blnk section, **Then** they understand it is a PoC pattern — not a production dependency and not a replacement for `core.wallet`.
3. **Given** a proposal to bypass TigerBeetle and write postings directly to PostgreSQL, **When** the reviewer checks the document, **Then** the rationale section explicitly covers why that would reintroduce the problems TB solves.

---

### User Story 3 — Architect Validates Proposals Against Constitution (Priority: P3)

A technical architect or senior engineer can check any proposed design change against the 7 governing principles in one place to determine whether it requires an ADR amendment.

**Why this priority**: Without a consolidated principles view, reviewers apply the constitution inconsistently and new ADRs may contradict existing ones silently.

**Independent Test**: Give an architect a proposed design that violates Principle II (mutable ledger correction inline instead of reversing journal). The document must make the violation detectable without looking up ADR-001.

**Acceptance Scenarios**:

1. **Given** a proposed design that skips the outbox and calls accounting synchronously, **When** reviewed against the document, **Then** Principle V (saga, not 2PC; outbox at-least-once) clearly identifies this as a violation.
2. **Given** all 7 principles, **When** each is read, **Then** every principle cites its authoritative ADR so the architect can navigate to the full decision record.
3. **Given** a Principle amendment needed, **When** the document is updated, **Then** the corresponding ADR is also updated — the document notes this requirement explicitly.

---

### Edge Cases

- Partial reads: a developer who only reads the module layout section must not receive a misleading picture — any critical boundary rule must be visible in context, not buried deep in a later section.
- Version drift: if an ADR is amended, the architecture overview may become stale — the document must carry a "last reviewed" date and reference the constitution version.
- Multiple audiences: the document is useful to both technical architects (needing ADR citations) and developers (needing concrete examples); both audiences must be served without splitting into two documents.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Document MUST describe the two-domain separation — `core.wallet` vs `core.accounting` — including what question each domain answers, which tables it owns, and which schema it uses.
- **FR-002**: Document MUST show the full module layout (`core.shared`, `core.wallet`, `core.accounting`, `app-orchestration`, `app-wallet`, `app-accounting`, `app-wallet-worker`) with allowed and forbidden dependency edges.
- **FR-003**: Document MUST explain TigerBeetle as the accounting hot-posting backing store — core concepts (Account, Transfer, pending/post/void), why it is preferred over pure PostgreSQL for postings, and the ADR-037 hybrid layout.
- **FR-004**: Document MUST explain the Blnk PoC — what Blnk is, which two patterns are adapted (`WalletBalanceMonitor`, `getBalanceAt`), scope limits (no Blnk binary, not production path, not a replacement for `core.wallet`).
- **FR-005**: Document MUST define the internal integration interface boundaries — inbound commands (`BANK_DEPOSIT`, `WALLET_CREDIT`), outbound events (`JournalPostedEvent`, `WalletCreditedEvent`, `CommandFailedEvent`), and HTTP surfaces (`WalletGateway`, `LedgerGateway`). Bank/NAPAS external interfaces are out of scope.
- **FR-006**: Document MUST list all 7 constitution principles with ADR citations.
- **FR-007**: Document MUST include a data-plane architecture diagram (ASCII is acceptable) showing the orchestration → wallet and orchestration → accounting call paths.
- **FR-008**: Document MUST carry a version/last-reviewed date aligned with the constitution version it reflects.

### Key Entities

- **Architecture Overview document**: the single artifact produced by this feature; lives at `specs/011-architecture-overview/` or a designated design directory.
- **Constitution v1.0.0**: the governing document this overview reflects (``.specify/memory/constitution.md``).
- **ADR index**: the set of locked ADRs cited throughout (ADR-001 through ADR-041 in `adr/`).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A new engineer with no prior GtelPay context can correctly answer 5/5 domain boundary questions after reading the document — verifiable by a structured onboarding quiz.
- **SC-002**: All 7 constitution principles are represented in the document — verifiable by counting principle sections against constitution v1.0.0.
- **SC-003**: All integration boundary entities (2 inbound commands, 3 outbound events, 2 HTTP surfaces) are documented — verifiable by comparing against `specs/002-async-deposit/data-model.md` §4.
- **SC-004**: Zero cross-domain calls appear in PRs that cite "not in architecture overview" as justification — measured over the 30 days after the document is published.
- **SC-005**: The document is reviewed and re-dated within 2 weeks of any ADR amendment to a cited principle.

## Assumptions

- The architecture overview covers the **target** architecture (the design intent), not the current migration state (in-process gateways, legacy monolith wiring). Migration debt is noted but not the focus.
- TigerBeetle and Blnk sections are written at the conceptual level — no code samples, no API call signatures. Those belong in implementation task files.
- Bank/NAPAS integration surfaces are explicitly excluded — only internal system boundaries are documented.
- The document is authored in Markdown for the local repo and mirrored to Confluence; local repo is the source of truth.
- Constitution v1.0.0 (ratified 2026-06-15) is the baseline; the overview does not pre-empt future ADR amendments.
