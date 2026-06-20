# ADR-037: `core.accounting` wraps TigerBeetle (hybrid ledger)

| Field | Value |
|-------|-------|
| Status | Pending review |
| Date | 2026-06-12 (updated from 2026-06-09 Proposed) |
| Source | Design discussion; [`spec/trd/accounting.md`](../spec/trd/accounting.md) §8, §12; [`sandbox/tigerbeetle/`](../sandbox/tigerbeetle/) spike |
| Related | [ADR-001](ADR-001-immutable-ledger.md), [ADR-003](ADR-003-dual-schema-single-postgres.md), [ADR-006](ADR-006-two-phase-deposit.md), [ADR-007](ADR-007-freeze-settle-async-outflow.md), [ADR-028](ADR-028-money-scale-four-half-up.md), [ADR-031](ADR-031-sql-ledger-invariant-ci.md), [ADR-041](ADR-041-rabbitmq-orch-to-accounting-worker.md) |

---

## Context

`core.accounting` exposes domain API (`accounting-internal.yaml`) to **app-orchestration** — orchestration **không** gọi TigerBeetle trực tiếp.

TigerBeetle ([tigerbeetle.com](https://tigerbeetle.com)) là engine double-entry: **accounts + transfers**, immutable, pending/post/void native, idempotency theo transfer `id`, throughput cao. Khớp deposit 2 phase ([ADR-006](ADR-006-two-phase-deposit.md)) và freeze/settle ([ADR-007](ADR-007-freeze-settle-async-outflow.md)).

Postgres-only `coa_trans` / `coa_trans_data` vẫn là mô hình thiết kế hợp lệ nhưng **không** còn là default khi implement.

---

## Decision

**`core.accounting` wrap TigerBeetle** cho **hot postings** (ghi sổ, số dư COA runtime). Module accounting vẫn là bounded context — TB là **implementation detail** phía sau port nội bộ.

```
app-orchestration ──HTTP──► core.accounting (accounting-internal.yaml)
                                    │
                    ┌───────────────┴───────────────┐
                    ▼                               ▼
            TigerBeetle cluster              PostgreSQL (schema accounting)
            transfers + balances             COA master, periods, recon state,
            (source of truth postings)       audit log, read-model projection
```

| Thuộc `core.accounting` | Store |
|-------------------------|--------|
| Post transfer, pending/post/void, COA balance runtime | **TigerBeetle** |
| `coa_account`, period close, reconciliation metadata | **PostgreSQL** |
| Trial balance / GL / báo cáo | **PostgreSQL read-model** (project từ TB — không query TB cho report) |
| `JournalPosted` event | Sau commit TB (+ projector) |

**Orchestration contract không đổi:** vẫn `LedgerGateway` → `accounting-internal.yaml`. Worker `BANK_DEPOSIT` ([ADR-041](ADR-041-rabbitmq-orch-to-accounting-worker.md)) gọi **domain accounting**, không gọi TB client trực tiếp.

---

## Mapping (GtelPay → TigerBeetle)

| Platform | TigerBeetle |
|----------|-------------|
| `coa_account.code` (1111, 3100, 2110…) | TB `Account` id (bảng map trong Postgres) |
| Một journal (`coa_trans`) | Một hoặc nhiều `Transfer` (linked batch nếu cần) |
| `reference_id` / `businessRef` | Transfer `id` và/hoặc `user_data_*` correlation |
| `PENDING` (deposit phase A) | `flags.pending` transfer |
| `POSTED` / `confirmDeposit` | `post_pending_transfer` (+ transfers phase B) |
| `sum(DR)=sum(CR)` | Enforced per transfer |
| VND scale 4 ([ADR-028](ADR-028-money-scale-four-half-up.md)) | u128 **minor units** (×10⁴) — không float |

Domain service (`JournalService`, `confirmDeposit`) giữ semantic `use_case`, transit zero, period rules — adapter map sang TB.

---

## Natural fit

| Platform need | TigerBeetle |
|---------------|-------------|
| Immutable ledger ([ADR-001](ADR-001-immutable-ledger.md)) | Native append-only |
| Two-phase deposit ([ADR-006](ADR-006-two-phase-deposit.md)) | pending → post/void |
| Freeze/settle ([ADR-007](ADR-007-freeze-settle-async-outflow.md)) | pending transfers + bounds flags |
| Idempotency ([ADR-005](ADR-005-idempotency-key-strategy.md)) | Transfer `id` uniqueness |
| NFR throughput / strong consistency | Purpose-built OLTP ledger |

---

## Trade-offs & ADR revisions

| Concern | Mitigation |
|---------|------------|
| Second datastore | TB cluster + Java client; ops/DR runbook riêng |
| [ADR-003](ADR-003-dual-schema-single-postgres.md) | **Partial:** wallet + accounting **metadata** vẫn Postgres; **postings** không còn `coa_trans_data` là source — revise khi implement |
| [ADR-031](ADR-031-sql-ledger-invariant-ci.md) | Invariants trên TB + projector tests; không SQL `SUM` trên posting table |
| Reporting | Projector TB → `coa_trans` / read-model; Metabase trên Postgres ([TRD §8–9](../spec/trd/accounting.md)) |

---

## Out of scope (unchanged)

- `core.wallet` — vẫn Postgres snapshot ([ADR-004](ADR-004-wallet-balance-snapshot.md))
- Orchestration sequencing — vẫn app-orchestration
- Full COA hierarchy / FR-8 period logic trong TB — ở Postgres control plane

---

## Acceptance criteria (AC-037)

| ID | Criterion |
|----|-----------|
| AC-037-01 | Chỉ `core.accounting` module mở TB client — không orch/wallet |
| AC-037-02 | `accounting-internal.yaml` unchanged wire; adapter đổi backend |
| AC-037-03 | Deposit PENDING/POSTED maps to TB pending/post per ADR-006 |
| AC-037-04 | Duplicate `reference_id` → TB idempotent no-op |
| AC-037-05 | Read-model projector eventual; reports không đọc TB trực tiếp |

---

## References

- [`spec/trd/accounting.md`](../spec/trd/accounting.md) — §8, §12
- [`references/tigerbeetle-data-modeling.md`](../references/tigerbeetle-data-modeling.md), [`references/alexandrubagu-tigerbeetle-overview.md`](../references/alexandrubagu-tigerbeetle-overview.md)
- https://docs.tigerbeetle.com
- [`sandbox/tigerbeetle/README.md`](../sandbox/tigerbeetle/README.md) — spike / mapping notes
