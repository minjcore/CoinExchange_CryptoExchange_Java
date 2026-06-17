# design-v2 — Canonical platform design (fresh)

**Status:** Draft · **Language:** English · **Scope:** `10_core/` only

Monolith behavior — hai domain tách cứng; orchestration documented separately (not a domain).

## Đọc theo flow

### Flow 1 — Domain behavior (tuần tự)

| # | File | Owns |
|---|------|------|
| 1 | [`accounting.md`](./accounting.md) | Domain 1 — `coa_*`, Part I + Part II (§14–30), §29 principles |
| 1b | [`accounting/`](./accounting/) | **Corpus kế toán VI** — Quyển I–III (mục tiêu 1000 trang) |
| 2 | [`wallet.md`](./wallet.md) | Domain 2 — `wallet_*`, Part I (§1–14) + Part II (§15–29) |
| 3 | [`orchestration.md`](./orchestration.md) | BFF — Part I (§1–10) + Part II saga (§11–27) |
| 3b | [`batch.md`](./batch.md) | Cross-cutting — payroll / disbursement / EOD; consolidates orch §17 + wallet §20 + accounting vol-06 |

Modular (mỏng): [`../design/`](../design/).

### Flow 2 — Luồng nghiệp vụ → behavior map

Đọc [`../spec/processes.md`](../spec/processes.md) theo thứ tự §3–11; chi tiết behavior tương ứng:

| `processes.md` | Accounting | Wallet | Orchestration |
|----------------|------------|--------|---------------|
| §3 Deposit | `accounting.md` §8 | `wallet.md` §5.1 | `orchestration.md` §6 |
| §4 Withdraw | postings §9 | `wallet.md` freeze/settle | `orchestration.md` §7 |
| §5 Payment | §13 | `wallet.md` §5.2 | sync 3-commit |
| §6 Transfer | §10 | `wallet.md` §5.3 | sync 3-commit |
| §7 IBFT | §11 | IBFT tx types | async outflow |
| §8 QR/POS | §12, §16 | §5.8 | EOD batch |
| §9 Payroll | §14 | batch credit | §13.6 |
| §10 Disbursement | §15 | batch debit | §13.6 |
| §11 EOD | §16 | recon | ADR-015 |

Cross-cutting: `processes.md` §13–17 ↔ `orchestration.md` Part II.

### Flow 3 — Verify

| # | File | Nội dung |
|---|------|----------|
| 4 | [`acceptance.md`](./acceptance.md) | **150** gate (Part I+II+ADR-031/032/033) · **207** full |

ADR AC/TC → Gherkin: [`../adr/README.md`](../adr/README.md).

## Full stack read map (implement từ `10_core`)

| Cần | File |
|-----|------|
| DR/CR, COA, 9 luồng | [`../spec/foundation.md`](../spec/foundation.md) §6–16 |
| Accounting FR/NFR | [`../spec/trd/accounting.md`](../spec/trd/accounting.md) |
| Wallet FR, `tx_type` | [`../spec/trd/wallet.md`](../spec/trd/wallet.md) |
| Binding D1–D5, enum | [`../spec/implementation.md`](../spec/implementation.md) §2 |
| Step order, Kafka/RabbitMQ | [`../spec/integration-surfaces.md`](../spec/integration-surfaces.md) |
| Saga, fee, auth narrative | [`../spec/processes.md`](../spec/processes.md) §13–17 |
| Behavior contract (v2) | `accounting.md`, `wallet.md`, `orchestration.md` |
| Conformance | `acceptance.md` |
| Wire | [`../spec/contracts/`](../spec/contracts/) |
| ADR + AC/TC | [`../adr/README.md`](../adr/README.md) — **40 ADR** (001–040) |

**Thiếu trong repo (không gen):** fee schedule chính thức, bank SLA/timeout, ops runbook chi tiết.

## Reference corpus (`references/` — 108 files)

Industry material **synthesized** into design-v2 (not copied DR/CR). Index: [`../references/README.md`](../references/README.md).

| Corpus topic | Synthesis in design-v2 | Gaps flagged |
|--------------|------------------------|--------------|
| GAAP + VAS/IFRS VN, accrual | `accounting.md` §29 | **ADR-036** |
| Double-entry, immutable ledger, recon | `accounting.md` §26–27 | **ADR-031** |
| Balance types, holds, double-spend | `wallet.md` §27–28 | **ADR-032** |
| Saga, outbox, idempotency, VA, Napas | `orchestration.md` §25–25.7 | **ADR-033**, **ADR-035** |
| LOCKED deposit (W-O1) | `wallet.md` W-O1 | **ADR-034** |

**Rule:** refs inform **invariants and behaviors**; amounts/templates stay in `spec/foundation.md`.

## Relationship to existing files

| Legacy file | Role after v2 |
|-------------|----------------|
| `spec/foundation.md` | Part I valid. Part II postings summarized in `accounting.md` — v2 authoritative for new work |
| `spec/trd/wallet.md` | TRD FR; v2 `wallet.md` aligns naming per `spec/terminology.md` |
| `spec/trd/accounting.md` | TRD FR/NFR; v2 `accounting.md` aligns platform naming |
| `spec/implementation.md` | Implementation binding — **not** design |
| `spec/integration-surfaces.md` | Wire index; field detail in `spec/contracts/` |
| `adr/` | **40 ADR** (001–040) + **AC/TC** → `acceptance.md` |

## ADR index (40 decisions)

Full table: [`../adr/README.md`](../adr/README.md).

| Range | Domain |
|-------|--------|
| 001, 006, 010, 023, 025 | Ledger / accounting |
| 004, 007, 014, 016, 020, 024, 026, 029 | Wallet |
| 002, 003, 005, 008, 009, 012, 018, 028 | Platform boundaries |
| 011, 013, 015, 017, 019, 021, 022, 027, 030, 033, 035 | Orchestration / integration |
| 031, 032, 034, 036 | Reference-gap closure |

## Hard rules (both domains)

1. **No cross-domain JOIN** — [ADR-002](../adr/ADR-002-core-foundation-shared-library.md), [ADR-003](../adr/ADR-003-dual-schema-single-postgres.md).
2. **`business_ref`** — end-to-end idempotency ([ADR-005](../adr/ADR-005-idempotency-key-strategy.md)).
3. **Transit zero** — [ADR-010](../adr/ADR-010-transit-accounts-net-zero.md).
4. **Immutable posted lines** — [ADR-001](../adr/ADR-001-immutable-ledger.md).
5. **Wallet append-only** — [ADR-004](../adr/ADR-004-wallet-balance-snapshot.md).

## Wire contracts

- Public HTTP: [`../spec/contracts/open-api/gtelpay-public.yaml`](../spec/contracts/open-api/gtelpay-public.yaml)
- Accounting internal: `spec/contracts/open-api/accounting-internal.yaml`
- Kafka events: `spec/contracts/async-api/core-events.yaml`
- RabbitMQ commands: `spec/contracts/async-api/core-commands.yaml`
