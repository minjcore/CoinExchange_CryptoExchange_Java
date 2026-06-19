# Accounting Service TRD

> **CF page ID:** 43221005 | **Parent:** 📋 Domain TRDs (51183937)
> **Source of truth:** this file → push to CF
> **ADR authority:** ADR-001, ADR-002, ADR-003, ADR-006, ADR-013, ADR-037, ADR-041
> **Wire contracts:** `specs/contracts/open-api/accounting-internal.yaml`, `specs/contracts/async-api/core-commands.yaml`

---

## GtelPay Implementation Note

This TRD uses industry-facing API names. Platform implementation uses `coa_*` tables and TigerBeetle as hot-posting store. See [Terminology Reference](../start-here/terminology.md) for mapping.

---

## Platform Architecture

### Two-Domain Separation
`core.accounting` and `core.wallet` are isolated bounded contexts — no cross-schema JOINs, no cross-schema FKs, no direct domain calls. (Principle I, ADR-002, ADR-003)

### Module Layout

| Module | Type | Owns |
|--------|------|------|
| `core.accounting` | Domain library (pure Java) | `coa_trans`, `coa_trans_data`, `LedgerService`, `TigerBeetleGateway` |
| `app-accounting` | HTTP gateway (sync) | Sync HTTP entry point — called by orchestrator only |
| `app-accounting-worker` | RabbitMQ consumer (async) | Consumes `BANK_DEPOSIT` → calls `core.accounting` |

### Service Entry Points

| Scenario | Protocol | Module | Contract |
|----------|---------|--------|---------|
| Async deposit (Phase A+B) | **RabbitMQ** | `app-accounting-worker` | `BANK_DEPOSIT` in `core-commands.yaml` |
| Sync use case | **HTTP** | `app-accounting` | `accounting-internal.yaml` |

> `app-orchestration` does NOT call `app-accounting` via HTTP in deposit flow — outbox → worker only.

---

## Scope

**In scope:** COA, journal entries (two-phase PENDING→POSTED), double-entry posting, balances (O(1) via TB), period management, reconciliation, audit logs, reporting.

**Out of scope v1:** Multi-currency, tax, payroll, billing, budget planning.

---

## LedgerService Interface

```java
public interface LedgerService {

    // Phase A — idempotent on (businessRef, useCase)
    // TB: pending Transfer  1111 DR / 3100 CR (grossAmount)
    CoaTrans createJournal(CreateJournalCommand cmd);

    // Phase B — post_pending + fee split; validates account[3100] = 0
    // TB: post_pending + Transfer 3100→2110 (net) + Transfer 3100→4110 (fee)
    CoaTrans confirmDeposit(long coaTransId, BigDecimal fee);

    // Reversal — void_pending TigerBeetle transfer → status FAILED
    CoaTrans voidPending(long coaTransId);

    // Query
    CoaTrans getJournal(long coaTransId);
}

public record CreateJournalCommand(
    String businessRef,
    UseCase useCase,       // DEPOSIT | PAYMENT | TRANSFER | ...
    BigDecimal grossAmount
) {}
```

**Rules:**
- `confirmDeposit` nhận `fee` riêng (được tính bởi orchestration, truyền vào qua BANK_DEPOSIT command) — phép tính `net = grossAmount − fee` thực hiện bên trong `confirmDeposit`
- `createJournal` idempotent: `UNIQUE(businessRef, useCase)` → duplicate trả về existing PENDING
- Dependencies: `core.foundation` only. Zero framework imports.

---

## TigerBeetle là gì?

TigerBeetle là database chuyên dụng cho financial transactions — không phải general-purpose DB. Viết bằng Zig, thiết kế từ đầu cho double-entry bookkeeping.

| Đặc điểm | Chi tiết |
|----------|---------|
| **Native pending/post/void** | Transfer có 3 trạng thái built-in — không cần tự implement saga |
| **O(1) balance reads** | Mỗi Account object tích lũy `debits_posted`, `credits_posted` — không cần `SUM` |
| **u128 amounts** | Không có floating-point. VND × 10⁴ stored as integer |
| **Throughput** | 1M+ TPS trên single node, deterministic consensus (VSSR) |
| **Immutability** | Transfer không bao giờ bị xóa hay sửa — chỉ append |

**Tại sao dùng cho `core.accounting`?**
PostgreSQL có thể làm được double-entry, nhưng cần `SUM(amount) WHERE account_id = X` mỗi lần đọc balance — O(n) theo số dòng. TigerBeetle giữ running balance trong Account object — O(1) bất kể có bao nhiêu transfers.

**Chỉ `core.accounting` mở TB client** — ADR-037. Không module nào khác được access TigerBeetle trực tiếp.

---

## TigerBeetleGateway Interface

```java
public interface TigerBeetleGateway {

    // Phase A — create pending transfer
    void createPendingTransfer(TbTransferCommand cmd);

    // Phase B step 1 — post the pending transfer (full amount, no partial)
    void postPendingTransfer(long pendingTransferId, long postTransferId);

    // Phase B step 2/3 — immediate transfer for net + fee legs
    void createTransfer(TbTransferCommand cmd);

    // Reversal — void pending transfer
    void voidPendingTransfer(long pendingTransferId, long voidTransferId);

    // Balance read — O(1), no SUM query
    AccountBalance getAccountBalance(long accountId);
}

public record TbTransferCommand(
    long transferId,      // deterministic: hash(businessRef + ":phaseA") etc.
    long debitAccountId,  // COA code: 1111, 3100, 2110, 4110...
    long creditAccountId, // COA code
    BigDecimal amount
) {}

public record AccountBalance(
    long accountId,       // COA code
    BigDecimal debitsPosted,
    BigDecimal creditsPosted,
    BigDecimal debitsPending,
    BigDecimal creditsPending
) {
    public BigDecimal net() {
        return creditsPosted.subtract(debitsPosted);
    }
}
```

**Locked decisions:**
- `transferId` = `long` (64-bit) — đủ cho scale VN
- `accountId` = COA code (1111, 3100...) — không phải internal DB id
- `postPendingTransfer` không nhận `amount` — TB post full, không partial

---

## Repository Ports

Repository interfaces live in `core.accounting/port/` — pure Java, no JPA annotations.
Implemented by `app-accounting` using Spring Data JPA.

```java
public interface CoaTransRepository {

    // Idempotency check — trước khi INSERT
    Optional<CoaTrans> findByBusinessRefAndUseCase(String businessRef, UseCase useCase);

    // Load for Phase B mutation
    Optional<CoaTrans> findById(long coaTransId);

    // Insert (Phase A) + update status (Phase B)
    CoaTrans save(CoaTrans coaTrans);
}
```

```java
public interface CoaTransDataRepository {

    // Read-model lines for a journal (reporting / reconciliation)
    List<CoaTransData> findByCoaTransId(long coaTransId);

    // Append-only — populated after TB Transfer committed (read-model projection, not hot write path)
    CoaTransData save(CoaTransData line);
}
```

**Notes:**
- `CoaTransRepository.findByBusinessRefAndUseCase` drives idempotency for `createJournal`.
- `CoaTransDataRepository` is append-only. Lines are never updated after POSTED.
- `save` on `CoaTrans` handles both INSERT (Phase A) and status UPDATE (Phase B `PENDING → POSTED`).
- No Spring/JPA annotation in these interfaces. Zero framework imports.

---

## Terminology Mapping

| TRD / API | Platform | Table |
|-----------|---------|-------|
| `journal_entries` | Journal header | `coa_trans` |
| `journal_lines` | Journal entry line | `coa_trans_data` (PostgreSQL read-model) / TigerBeetle Transfer (hot store) |
| `reference_id` | Idempotency key | `business_ref` on `coa_trans` |
| Ledger entry | Immutable posted line | TB Transfer when `coa_trans.status = POSTED` |

---

## Functional Requirements

### FR-2 Journal Entries (`coa_trans`)

| Field | Type | Notes |
|-------|------|-------|
| `id` | BIGINT PK | Auto-increment. UUID rejected — 2× index storage + random insert gây page split. |
| `reference_id` (`business_ref`) | VARCHAR(64) | = `X-Idempotency-Key` |
| `use_case` | VARCHAR(32) | `DEPOSIT`, `PAYMENT`, ... |
| `status` | ENUM | `PENDING` → `POSTED` \| `FAILED` |
| `gross_amount` | NUMERIC(19,4) | Total amount including fee. Set on Phase A. |
| `fee` | NUMERIC(19,4) | Fee leg. Null until Phase B. |
| `net_amount` | NUMERIC(19,4) | `gross_amount − fee`. Null until Phase B. |
| `currency` | VARCHAR(3) | `VND` v1. |
| `created_at` | TIMESTAMPTZ | immutable |
| `updated_at` | TIMESTAMPTZ | Phase B only |

**Unique constraint:** `(reference_id, use_case)` — idempotent on duplicate `createJournal`.

### FR-5 Two-Phase Posting Protocol

| Phase | Operation | Effect | Entry point |
|-------|-----------|--------|-------------|
| Phase A — PENDING | `createJournal(DEPOSIT, PENDING)` | TB pending Transfer: 1111 DR → 3100 CR (gross) | RabbitMQ `BANK_DEPOSIT` → `app-accounting-worker` |
| Phase B — POSTED | `confirmDeposit(coaTransId, fee)` | TB post_pending + 2 Transfers: 3100 DR → 2110 CR (net), 3100 DR → 4110 CR (fee); validate account[3100]=0 | Same consumer, no separate trigger |

**TB pending transfer ID:** `hash(businessRef + ":phaseA")` — deterministic idempotency.

### FR-7 Account Balances
TigerBeetle O(1) reads via `debits_posted`/`credits_posted` — no `SUM` query needed.

### FR-9 Currency
VND only v1. BigDecimal scale 4 HALF_UP. Wire: decimal string `"100000.0000"`. TB: u128 integer = VND × 10⁴.

---

## Data Model

```sql
-- Journal header
coa_trans (
  id BIGINT PK,                -- Auto-increment. UUID rejected (2× index + page split).
  reference_id VARCHAR(64),    -- = businessRef = X-Idempotency-Key
  use_case VARCHAR(32),        -- DEPOSIT | PAYMENT | ...
  status VARCHAR(16),          -- PENDING → POSTED | FAILED
  gross_amount NUMERIC(19,4),  -- Set on Phase A.
  fee NUMERIC(19,4),           -- Null until Phase B.
  net_amount NUMERIC(19,4),    -- gross_amount − fee. Null until Phase B.
  currency VARCHAR(3),         -- VND v1.
  created_at TIMESTAMPTZ,
  updated_at TIMESTAMPTZ,
  UNIQUE (reference_id, use_case)
)

-- Journal lines (PostgreSQL read-model; TB = hot source of truth)
coa_trans_data (
  id BIGINT PK,
  coa_trans_id BIGINT FK,      -- → coa_trans.id
  account_id BIGINT,           -- COA code (1111, 3100, 2110, 4110...)
  debit_amount NUMERIC(19,4),
  credit_amount NUMERIC(19,4),
  currency VARCHAR
)

-- Transactional outbox
outbox (
  id BIGINT PK,
  command_type VARCHAR(32),    -- BANK_DEPOSIT | WALLET_CREDIT
  business_ref VARCHAR(64),
  payload JSONB,
  status VARCHAR(16),          -- PENDING → PUBLISHED → FAILED
  created_at TIMESTAMPTZ,
  published_at TIMESTAMPTZ
)
```

---

## Non-Functional Requirements

| Metric | Target |
|--------|--------|
| Journal posts | 2,000/s |
| Balance reads | 20,000/s (O(1) TB) |
| 202 ack P95 | < 200 ms |
| Phase A+B P95 | < 2 s from queue consume to POSTED |
| Balance query P95 | < 100 ms |
| Availability | 99.95% monthly |
| RPO | < 5 min |
| RTO | < 30 min |
