% GtelPay Core — Design
% Fiat accounting + wallet core
% A core built for money — minimum viable correctness, not over-engineering

# The thesis (in one line)

> Payments are not CRUD. We built the **smallest** core that is still **correct under money** — and it already runs.

- Two domains, idempotent by construction, an immutable ledger, sequenced by saga.
- Everything in this deck maps to a **specific money-loss failure** it prevents.
- Nothing was built "just in case" — there's a list of what we **refused**.

---

# Part 1 — Why this matters (the stakes)

# Payments are money — bugs are cash

Three failure modes a generic wallet table cannot handle safely:

- **Double-spend** — a retry, race, or timeout lets the same funds leave twice.
- **Unbalanced / editable books** — the ledger can't be trusted; **audit and regulators fail**.
- **Speed vs truth** — "how much can this member spend *now*?" needs millisecond reads; "what happened?" needs an immutable, auditable ledger.

# Why one table isn't enough

- A single mutable `balance` row is **fast but unauditable** — no history, editable, no proof.
- A pure ledger (derive balance from history) is **correct but slow** — can't serve the spend hot path.
- Force them into one model and **one of them breaks**.
- → The design follows from this tension, not from taste.

---

# Part 2 — The design (three ideas)

# Idea 1 — Two domains, split on purpose

| | `core.wallet` | `core.accounting` |
|---|---|---|
| Question | "Can this member spend now?" | "What happened in the ledger?" |
| Shape | fast single-row balance (hot path) | immutable double-entry ledger |
| Truth | spendable now | system of record |

They **never share storage and never call each other** — isolated, scale independently.

# Idea 2 — Correctness by construction

- **Idempotent** — every change keyed; a retry returns the same result, never re-applies.
- **Recorded together** — no balance moves without a movement row, in the **same transaction**.
- **Balanced + immutable** — every journal has debits = credits, never edited; fixes are reversing entries.
- **Transit nets to zero** — money in flight is tracked to zero on every completed flow.

# Idea 3 — Orchestration, not distributed transactions

- A **thin sequencer** coordinates the two domains per use case.
- **Saga + compensation** — on partial failure, compensate; never a fragile 2-phase commit.
- The ledger is fed asynchronously (outbox → queue) — back-office never blocks the spend hot path.

# The whole picture

```
        client
          │
   orchestration (saga)
       ┌──┴──┐
   wallet   accounting
 (spendable) (immutable ledger)
   hot path   system of record
```

One sequencer, two truths, no shared state.

---

# Part 3 — "Isn't this over-engineering?"

# The honest test

Over-engineering = complexity with **no payoff**. Here, each piece pays for itself:

| Safeguard | Failure it prevents |
|-----------|---------------------|
| Idempotency (under lock) | Double-spend / double-credit |
| Double-entry + immutable | Audit / regulator failure |
| Wallet ⊥ accounting | Slow spend **or** untrustworthy books |
| Transit net-zero | Money stuck mid-flow |
| Saga + compensation | Stranded funds on an outage |

# So the real question is…

> **Which safeguard do you want to remove — and which loss do you accept?**

- Remove idempotency → accept double-spend on every retry storm.
- Remove the immutable ledger → accept failing audit.
- Each "simplification" has a name and a price.

# What we deliberately did NOT build

*Real over-engineering is gold-plating. We refused it — on the record (ADRs):*

- **4 modules, not 40** — one minimal shared lib (ADR-002)
- **No 2PC, no Temporal** — plain RabbitMQ workers (ADR-035)
- **No custom ledger DB** — plain PostgreSQL, two schemas (ADR-003)
- **No event-sourcing / CQRS** — a snapshot balance + an append-only log (ADR-004)
- **Single currency (VND) v1** — nothing built "just in case" (ADR-019)

# This is table-stakes, not exotic

- Double-entry, idempotency, immutable ledgers are what **banks, Stripe, Modern Treasury** all run.
- Calling these "over-engineering" is calling the entire payments industry over-engineered.
- We didn't invent primitives — we applied the standard ones, minimally.

---

# Part 4 — Proof (it runs)

# Not paper — running code

- `core.foundation` · `core.wallet` · `core.accounting` · `app-orchestration`
  → **built, tests passing** (Java / Spring Boot 3).
- Money-critical rules are **coded and tested**, not just described:
  - idempotency re-checked **under lock** → no double-apply
  - balanced, immutable journals · two-phase deposit (credit only after the ledger posts)

# Decisions are locked and enforced

- **41 ADRs** — each with acceptance criteria + test cases.
- **150+ acceptance scenarios** (Given/When/Then) as the conformance gate.
- **SQL invariant suite gates the build** — every journal balances, transit nets zero — so drift **fails CI**, not production.

---

# Part 5 — The decision

# Build vs. buy

- Blnk / Formance / TigerBeetle **merge balance + ledger** — great for an embedded ledger primitive.
- Our needs: **real chart of accounts · transit net-zero · EOD settlement · regulator-grade audit · high-RPS spendable balance.**
- **Decision:** borrow their proven patterns; build only the parts that must fit bank/audit reality.
- The build is **small, focused, and already running.**

# Risk is controlled

- **Backward compatible** — new capability is opt-in; existing callers unaffected.
- **Reconciliation is report-only** — surfaces drift, never rewrites the books.
- **Contracts (OpenAPI / AsyncAPI) are the source of truth** — wire can't drift from intent.

# Scope

- `accounting` (ledger) + `wallet` (balance) + `foundation` + `orchestration`.
- **Fiat (VND).**
- Backing on request: 41 ADRs · process flows · contracts · 150+ acceptance scenarios.

# The ask

- This is the **simplest design that doesn't lose money.**
- *"Simple is good — and this **is** the simplest that's still correct."*
- Want it simpler? **Name the safeguard to cut, and own the loss.**

# Thank you

**GtelPay Core** — a fiat accounting + wallet core.
Built for money. Already running. Backing material available on request.
