# 10_core

Design và spec cho GtelPay Core — hai domain `coa_*` / `wallet_*`, orchestration tách riêng. Edit history: `git log`.

## Đọc theo flow

Chọn **một** lộ trình — mỗi bước trỏ file kế tiếp.

### Flow A — Onboard (hiểu hệ thống)

| Bước | Đọc | Mục đích |
|------|-----|----------|
| A1 | [`design/platform/boundaries.md`](design/platform/boundaries.md) | Hai domain, hard rules, ai gọi ai |
| A2 | [`spec/terminology.md`](spec/terminology.md) | Naming `business_ref`, COA, `tx_type` |
| A3 | [`spec/foundation.md`](spec/foundation.md) Part I | Shared lib + invariants |
| A4 | [`spec/foundation.md`](spec/foundation.md) §6–16 | COA map + DR/CR 9 luồng (tóm tắt) |

### Flow B — Domain deep-dive

| Bước | Đọc | Mục đích |
|------|-----|----------|
| B1 | [`design/accounting/`](design/accounting/) hoặc [`design-v2/accounting.md`](design-v2/accounting.md) | Ledger, journal, postings |
| B2 | [`design/wallet/`](design/wallet/) hoặc [`design-v2/wallet.md`](design-v2/wallet.md) | Balance, freeze, `tx_type` |
| B3 | [`spec/trd/accounting.md`](spec/trd/accounting.md) · [`spec/trd/wallet.md`](spec/trd/wallet.md) | FR/NFR service |

### Flow C — Luồng nghiệp vụ (9 use cases)

Đọc tuần tự trong [`spec/processes.md`](spec/processes.md) — mỗi § có actors, DR/CR, wallet, failure path.

| # | Use case | `processes.md` | Step order | Saga / edge |
|---|----------|----------------|------------|-------------|
| 1 | Deposit | §3 | [`design/orchestration/flows.md`](design/orchestration/flows.md) | §13.1 |
| 2 | Withdraw | §4 | flows.md · [`design-v2/accounting/vol-04-withdraw-ibft.md`](design-v2/accounting/vol-04-withdraw-ibft.md) Phần I | §13.4 |
| 3 | Payment | §5 | flows.md · [`design-v2/accounting/vol-05-payment-transfer.md`](design-v2/accounting/vol-05-payment-transfer.md) Phần I | §13.2 |
| 4 | Internal transfer | §6 | flows.md · vol-05 Phần II | §13.2 |
| 5 | IBFT | §7 | flows.md · vol-04 Phần II | §13.4 |
| 6 | QR/POS | §8 | flows.md · [`design-v2/accounting/vol-06-qr-payroll-disburse.md`](design-v2/accounting/vol-06-qr-payroll-disburse.md) Phần I | §14 |
| 7 | Payroll | §9 | flows.md · vol-06 Phần II | §13.6 |
| 8 | Disbursement | §10 | flows.md · vol-06 Phần III | §13.6 |
| 9 | EOD settlement | §11 | — | §15 |

Sau §11: §13 saga · §14 edge · §15 reliability · §16 fee · §17 auth.

Behavior đầy đủ (Part II, reference synthesis): [`design-v2/orchestration.md`](design-v2/orchestration.md).

### Flow D — Wire & integration

| Bước | Đọc | Mục đích |
|------|-----|----------|
| D1 | [`spec/integration-surfaces.md`](spec/integration-surfaces.md) §1–§2 | Luồng + bảng contract YAML |
| D2 | [`spec/contracts/openapi/`](spec/contracts/openapi/) | HTTP payloads |
| D3 | [`spec/contracts/asyncapi/`](spec/contracts/asyncapi/) | Kafka events + RabbitMQ commands |
| D4 | [`design/messaging/`](design/messaging/) | Ai publish/consume gì |

### Flow E — Implement

| Bước | Đọc | Mục đích |
|------|-----|----------|
| E1 | [`spec/implementation.md`](spec/implementation.md) §1–2 | Repo layout, module graph, D1–D5 |
| E2 | `implementation.md` §7–8 | DDL, posting registry, sync 3-commit |
| E3 | [`adr/README.md`](adr/README.md) | 40 quyết định đã khóa |
| E4 | Build order P0–P6 | `implementation.md` cuối file |

### Flow F — Verify (150 use cases)

| Bước | Đọc | Mục đích |
|------|-----|----------|
| F1 | [`design-v2/acceptance.md`](design-v2/acceptance.md) Part I | 75 scenarios — core per 9 use case |
| F2 | `acceptance.md` Part II | 60 scenarios — extended matrix (DEP-E, WD-E, …) |
| F3 | `acceptance.md` Part III (ADR-031/032/033) | 15 scenarios — CI invariant, monitors, bank poll |
| F4 | `adr/ADR-*.md` AC/TC | Criteria per decision |
| F5 | `acceptance.md` Part III remainder | +57 stretch (ADR-034/035/036, QR/Payroll/EOD depth) → full **207** |
| F6 | [`references/README.md`](references/README.md) | Corpus tham chiếu (khi cần justify) |

**Release gate v1 = 150 scenarios** (F1+F2+F3). Full corpus = 207.

---

## Cấu trúc thư mục

```
10_core/
├── spec/                 # Binding: foundation, TRD, processes, implementation, wire index
│   ├── contracts/        # openapi/, asyncapi/, gateway/ (YAML truth)
│   └── trd/
├── design/               # Modular theo domain (mỏng, đọc cạnh YAML)
│   └── platform|accounting|wallet|orchestration|messaging/
├── design-v2/            # Monolith behavior + Gherkin (150 gate / 207 full)
├── adr/                  # 40 ADR + AC/TC
├── references/           # Corpus tham chiếu (scraped)
├── tooling/              # md2pdf, exports/*.pdf
└── _archive/pre-2026-06/ # Snapshot cũ
```

## Chọn doc theo vai trò

| Vai trò | Bắt đầu |
|---------|---------|
| Kế toán / audit (VI, depth) | [`design-v2/accounting/`](design-v2/accounting/) Quyển I → VI (→ VII EOD khi có) |
| Architect / reviewer | Flow A → B → C |
| Backend implementer | Flow A (A1–A3) → E → D → F |
| QA / conformance | Flow C (map) → F |
| Integration / API | Flow D → `spec/contracts/` |
| Domain owner (accounting) | B1 + `design-v2/accounting.md` |
| Domain owner (wallet) | B2 + `design-v2/wallet.md` |

## Redirect ở root

Các file `core.*.md`, `IMPLEMENTATION.md`, `integration-surfaces.md`, `TERMINOLOGY.md` ở root là **stub redirect** → `spec/`. Không xóa để giữ link cũ.

`openapi/`, `asyncapi/`, `gateway/`, `_legacy/` ở root: stub README → `spec/contracts/` hoặc `_archive/`.

## Tooling

```bash
chmod +x tooling/md2pdf.sh
./tooling/md2pdf.sh spec/foundation.md
./tooling/md2pdf.sh spec/trd/wallet.md
./tooling/md2pdf.sh spec/trd/accounting.md
./tooling/md2pdf.sh spec/integration-surfaces.md
./tooling/md2pdf.sh spec/processes.md
./tooling/md2pdf.sh spec/acceptance-specs-legacy.md
./tooling/md2pdf.sh spec/implementation.md
```

PDF output: `tooling/exports/`.
