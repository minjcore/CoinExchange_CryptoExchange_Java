# External references (scraped)

**Updated:** 2026-06-08 (lần 8 — VAS/IFRS VN + ADR-036) · **Tool:** Firecrawl CLI · **Scope:** `10_core/references/` only

Tài liệu tham chiếu ngành — **không phải** spec dự án. Đối chiếu với `core.sharedlib.md`, `core.wallet.md`, `design-v2/`, ADR.

**Tổng:** 124 corpus files + README · **~37.820 dòng** markdown/txt

**Đã nạp vào design-v2:**

| Domain | Synthesis section | Files covered |
|--------|-------------------|---------------|
| Accounting | [`design-v2/accounting.md`](../design-v2/accounting.md) §26–27, **§29–29.6** | GAAP, VAS/IFRS VN, ADR-036 accrual |
| Wallet | [`design-v2/wallet.md`](../design-v2/wallet.md) §27–28 | Balance types, holds, P2P, VA |
| Orchestration | [`design-v2/orchestration.md`](../design-v2/orchestration.md) §25 | Saga, outbox, idempotency, Napas |

---

## Accounting principles (nguyên tắc kế toán — lần 7)

Synthesis: [`design-v2/accounting.md`](../design-v2/accounting.md) §29.

| File | URL | Map → dự án |
|------|-----|-------------|
| `investopedia-accounting-principles.md` | https://www.investopedia.com/terms/a/accounting-principles.asp | §29.2 — 12 core principles |
| `investopedia-gaap.md` | https://www.investopedia.com/terms/g/gaap.asp | GAAP overview |
| `investopedia-accrual-accounting.md` | https://www.investopedia.com/terms/a/accrualaccounting.asp | Accrual vs deposit PENDING |
| `investopedia-accounting-equation.md` | https://www.investopedia.com/terms/a/accounting-equation.asp | A=L+E; foundation §5 |
| `moderntreasury-enforcing-immutability.md` | https://www.moderntreasury.com/journal/enforcing-immutability-in-your-double-entry-ledger | §29.1 — ADR-001 |
| `moderntreasury-gaap-accounting-rules.md` | https://www.moderntreasury.com/learn/what-are-gaap-accounting-rules | GAAP 10 principles |
| `netsuite-debits-credits.md` | https://www.netsuite.com/portal/resource/articles/accounting/debits-credits.shtml | §29.3 DR/CR |
| `netsuite-cash-vs-accrual.md` | https://www.netsuite.com/portal/resource/articles/financial-management/cash-basis-accrual-basis.shtml | §29.2 accrual-like ledger |
| `lumen-debits-credits-rules.md` | https://courses.lumenlearning.com/suny-finaccounting/chapter/general-rules-for-debits-and-credits/ | Normal balance table |
| `becker-expense-recognition.md` | https://www.becker.com/blog/cpe/the-expense-recognition-principle | Matching + fee ADR-009 |

**Failed (lần 7):** VAS TT200 thuvienphapluat (login); Medium accrual ledger (504).

---

## VAS / IFRS Vietnam (lần 8)

Synthesis: [`design-v2/accounting.md`](../design-v2/accounting.md) §29.6 · Decision: [ADR-036](../adr/ADR-036-accrual-basis-ledger-v1.md).

| File | URL | Map → dự án |
|------|-----|-------------|
| `vn-luat-ke-toan-2015-dieu-5-7.md` | https://luatvietnam.vn/ke-toan/luat-ke-toan-nam-2015-so-88-2015-qh13-101336-d1.html | Điều 5–7 excerpt; substance, prudence |
| `hoiketoanhcm-nguyen-tac-luat-ke-toan-2015.md` | https://www.hoiketoanhcm.org.vn/vn/trao-doi/ban-ve-cac-nguyen-tac-ke-toan-trong-luat-ke-toan-2015/ | Phân tích Điều 6 |
| `acclime-ifrs-vas-vietnam.md` | https://vietnam.acclime.com/guides/vietnam-ifrs-and-vas/ | VAS vs IFRS roadmap QĐ 345/2020 |
| `incorp-ifrs-vas-vietnam.md` | https://vietnam.incorp.asia/ifrs-and-vas-in-vietnam-top-insights/ | Transition insights |
| `tapchicongthuong-luat-ke-toan-2015.md` | https://tapchicongthuong.vn/mot-so-diem-moi-cua-luat-ke-toan-viet-nam-sua-doi-2015-26675.htm | Luật KT 2015 context |

**Failed (lần 8):** MOF portal (thin/blocked); TT200/99 full PDF (login).

---

## Wallet (chỉ mục nhanh)

Spec dự án: [`core.wallet.md`](../core.wallet.md) · [`design-v2/wallet.md`](../design-v2/wallet.md) · freeze/settle → [`design-v2/orchestration.md`](../design-v2/orchestration.md)

| File | Chủ đề | Map → dự án |
|------|--------|-------------|
| `moderntreasury-how-to-build-digital-wallet.md` | Wallet product từ ledger | `core.wallet.md` §1 |
| `moderntreasury-learn-digital-wallet.md` | What is digital wallet | onboarding / glossary |
| `moderntreasury-digital-wallet-tutorial.md` | Tutorial build wallet | deposit + P2P flows |
| `moderntreasury-fx-wallets-tutorial.md` | Multi-currency wallet | out of scope v1 (VND only) |
| `moderntreasury-what-is-digital-wallet.md` | Learn hub | — |
| `moderntreasury-balance-types-part-i.md` | posted / pending / available | `available` + `frozen` vs deposit PENDING |
| `finlego-wallet-as-a-service.md` | WaaS platform | `wallet_*` schema |
| `medium-codefarm-digital-wallet-system.md` | Paytm-style interview design | top-up, P2P, withdraw |
| `tianpan-designing-paypal-transfer.md` | PayPal/Cash App transfer | deposit, P2P, dedup |
| `crossmint-wallet-architecture-fintech.md` | Wallet architecture fintech | bounded context |
| `stripe-payment-settlement-explained.md` | Settlement timing | withdraw settle, merchant credit |
| `medium-double-spend-fintech.md` | Double-spend patterns | freeze before debit |
| `relayfi-pending-vs-available.md` | Pending vs available | `available` definition |
| `blnk-balances-intro.md` | inflight + queued balances | frozen ≈ inflight debit |
| `blnk-balance-monitoring.md` | Balance alerts | ops (chưa spec) |
| `blnk-balance-snapshots.md` | Point-in-time balance | recon / EOD |
| `blnk-historical-balances.md` | Historical balance query | audit |
| `blnk-transaction-lifecycle.md` | INFLIGHT → APPLIED / VOID | withdraw freeze/settle/release |
| **`blnk-vs-gtelpay-comparison.md`** | **Synthesis** — Blnk vs design 10_core | so sánh kiến trúc + 3 concept đáng vay |
| `mambu-transaction-holds.md` | Hold → settle / reverse | freeze API semantics |
| `mambu-authorization-holds.md` | Card auth holds (khác transaction hold) | tham chiếu only |
| `increase-api-accounts.md` | FBO / sub-accounts API | VA deposit mapping |
| `medium-slope-payments-ledger-pitfalls.md` | 5 ledger pitfalls | invariant review |

---

## Modern Treasury

| File | URL |
|------|-----|
| `moderntreasury-accounting-dev-part-i.md` | https://www.moderntreasury.com/journal/accounting-for-developers-part-i |
| `moderntreasury-accounting-dev-part-ii.md` | https://www.moderntreasury.com/journal/accounting-for-developers-part-ii |
| `moderntreasury-accounting-dev-part-iii.md` | https://www.moderntreasury.com/journal/accounting-for-developers-part-iii |
| `moderntreasury-how-to-build-digital-wallet.md` | https://www.moderntreasury.com/journal/how-to-build-a-digital-wallet-product |
| `moderntreasury-learn-digital-wallet.md` | https://www.moderntreasury.com/learn/what-is-a-digital-wallet |
| `moderntreasury-digital-wallet-tutorial.md` | https://www.moderntreasury.com/learn/build-a-digital-wallet |
| `moderntreasury-fx-wallets-tutorial.md` | https://www.moderntreasury.com/learn/build-fx-wallets |
| `moderntreasury-what-is-digital-wallet.md` | https://www.moderntreasury.com/learn/digital-wallet |
| `moderntreasury-scale-ledger-part-i.md` … `part-v.md` | https://www.moderntreasury.com/journal/how-to-scale-a-ledger-part-i |
| `moderntreasury-ledger-transaction-status.md` | https://www.moderntreasury.com/journal/the-ledger-transaction-status |
| `moderntreasury-single-vs-double-entry.md` | https://www.moderntreasury.com/learn/single-vs-double-entry-accounting |
| `moderntreasury-what-is-ledger-database.md` | https://www.moderntreasury.com/learn/what-is-a-ledger-database |
| `moderntreasury-what-is-reconciliation.md` | https://www.moderntreasury.com/learn/what-is-reconciliation |
| `moderntreasury-transaction-reconciliation.md` | https://www.moderntreasury.com/learn/what-is-transaction-reconciliation |
| `moderntreasury-recon-diaries-1.md` | https://www.moderntreasury.com/journal/recon-diaries-entry-1-cash-reconciliation-explained |
| `moderntreasury-recon-knapsack.md` | https://www.moderntreasury.com/journal/reconciliation-is-a-knapsack-problem |
| `moderntreasury-balance-types-part-i.md` | https://www.moderntreasury.com/journal/fintech-eng-challenges-part-i-different-balance-types-in-a-wallet |
| `moderntreasury-locking.md` | https://www.moderntreasury.com/learn/pessimistic-locking-vs-optimistic-locking |
| `moderntreasury-virtual-accounts.md` | https://www.moderntreasury.com/learn/what-are-virtual-accounts |
| `moderntreasury-ledgering-investing.md` | https://www.moderntreasury.com/journal/key-ledgering-challenges-for-investing-platforms |

**Gated:** [Building a Digital Wallet ebook](https://www.moderntreasury.com/resources/ebooks/building-a-digital-wallet-from-ledger-to-launch)

---

## Saga & distributed transactions

| File | URL |
|------|-----|
| `microsoft-data-store-overview.md` | https://learn.microsoft.com/en-us/azure/architecture/data-guide/ |
| `microsoft-saga-pattern.md` | https://learn.microsoft.com/en-us/azure/architecture/patterns/saga |
| `aws-saga-orchestration.md` | https://docs.aws.amazon.com/prescriptive-guidance/latest/cloud-design-patterns/saga-orchestration.md |
| `temporal-saga-patterns.md` | https://temporal.io/blog/mastering-saga-patterns-for-distributed-transactions-in-microservices |
| `zhorifiandi-saga-stuck-systems.md` | https://zhorifiandi.github.io/software-engineering/2024/09/14/solve-stuck-systems-flow-using-saga-pattern.html |
| `microservices-io-saga.md` | https://microservices.io/patterns/data/saga.html |
| `microservices-io-transactional-outbox.md` | https://microservices.io/patterns/data/transactional-outbox.html |
| `infoq-saga-orchestration-outbox.md` | https://www.infoq.com/articles/saga-orchestration-outbox/ |
| `decodable-outbox-pattern.md` | https://www.decodable.co/blog/revisiting-the-outbox-pattern |
| `confluent-chris-richardson-saga.md` | https://developer.confluent.io/learn-more/podcasts/choreographing-the-saga-pattern-in-microservices-ft-chris-richardson/ |

---

## Stripe (engineering)

| File | URL |
|------|-----|
| `stripe-dev-ledger-system.md` | https://stripe.dev/blog/ledger-stripe-system-for-tracking-and-validating-money-movement |
| `stripe-dev-ledger-system-markdown.md` | same — raw `.md` from Stripe |
| `stripe-dev-payment-api-design.md` | https://stripe.dev/blog/payment-api-design |
| `stripe-dev-stripe-credits.md` | https://stripe.dev/blog/how-we-built-stripe-credits-a-programmable-auditable-way-to-pay-your-stripe-fees |
| `stripe-idempotency-blog.md` | https://stripe.com/blog/idempotency |
| `stripe-api-idempotent-requests.md` | https://docs.stripe.com/api/idempotent_requests |
| `stripe-payment-reconciliation-101.md` | https://stripe.com/resources/more/payment-reconciliation-101 |
| `adyen-api-idempotency.md` | https://docs.adyen.com/development-resources/api-idempotency |
| `adyen-webhooks.md` | https://docs.adyen.com/development-resources/webhooks/ |
| `levelup-idempotency-payments.md` | https://levelup.gitconnected.com/system-architecture-idempotency-in-payment-transactions-1d7888480648 |
| `pragmatic-engineer-designing-payment-system.md` | https://newsletter.pragmaticengineer.com/p/designing-a-payment-system |

---

## Ledger engines & open source

| File | URL |
|------|-----|
| `square-books-double-entry-ledger.md` | https://developer.squareup.com/blog/books-an-immutable-double-entry-accounting-database-service/ |
| `tigerbeetle-financial-accounting.md` | https://docs.tigerbeetle.com/coding/financial-accounting/ |
| `tigerbeetle-data-modeling.md` | https://docs.tigerbeetle.com/coding/data-modeling/ |
| `alexandrubagu-tigerbeetle-overview.md` | https://alexandrubagu.github.io/blog/tigerbeetle-financial-database.html |
| `interledger-rafiki-tigerbeetle.md` | https://interledger.org/developers/blog/rafiki-tigerbeetle-integration/ |
| `formance-ledger-module.md` | https://docs.formance.com/modules/ledger |
| `formance-accounting-model.md` | https://docs.formance.com/modules/ledger/accounting-model/introduction |
| `formance-ledger-accounts.md` | https://docs.formance.com/modules/ledger/core-concepts/accounts |
| `formance-how-not-to-build-ledger.md` | https://www.formance.com/blog/engineering/how-not-to-build-a-ledger |
| `formance-defining-double-entry.md` | https://www.formance.com/blog/engineering/defining-double-entry |
| `blnk-llms-index.txt` | https://docs.blnkfinance.com/llms.txt — full doc index |
| `blnk-balances-intro.md` | https://docs.blnkfinance.com/balances/introduction.md |
| `blnk-transactions-intro.md` | https://docs.blnkfinance.com/transactions/introduction.md |
| `blnk-double-entry-guide.md` | https://docs.blnkfinance.com/guides/double-entry.md |
| `blnk-transaction-lifecycle.md` | INFLIGHT / APPLIED / VOID |
| `blnk-reconciliations-overview.md` | https://docs.blnkfinance.com/reconciliations/overview.md |
| `blnk-reconciliation-strategies.md` | https://docs.blnkfinance.com/reconciliations/strategies.md |
| `blnk-balance-monitoring.md` | https://docs.blnkfinance.com/balances/balance-monitoring.md |
| `blnk-balance-snapshots.md` | https://docs.blnkfinance.com/balances/balance-snapshots.md |
| `blnk-historical-balances.md` | https://docs.blnkfinance.com/balances/historical-balances.md |
| `formance-ledger-quick-start.md` | https://docs.formance.com/modules/ledger/quick-start |
| `formance-examples-intro.md` | https://docs.formance.com/examples/introduction |
| `tigerbeetle-create-accounts.md` | https://docs.tigerbeetle.com/coding/requests/create_accounts/ |

---

## DDD & bounded context (wallet vs accounting)

| File | URL |
|------|-----|
| `martinfowler-bounded-context.md` | https://martinfowler.com/bliki/BoundedContext.html |
| `martinfowler-two-hard-things.md` | https://martinfowler.com/bliki/TwoHardThings.html |
| `airwallex-ddd-payments.md` | https://medium.com/airwallex-engineering/domain-driven-design-practice-modeling-payments-system-f7bc5cf64bb3 |
| `devto-ddd-bounded-contexts.md` | https://dev.to/aws-builders/modeling-shared-entities-across-bounded-contexts-in-domain-driven-design-5hih |
| `oracle-ddd-composable-banking.md` | https://www.oracle.com/bz/financial-services/domain-driven-design-composable-banking/ |

---

## ISO 20022 & reconciliation standards

| File | URL |
|------|-----|
| `swift-iso-20022-chapter2.md` | https://www.swift.com/standards/iso-20022/supercharge-your-payments-business/chapter-2 |
| `payments-canada-iso-20022.md` | https://www.payments.ca/payment-resources/iso-20022 |
| `reconart-iso-20022-reconciliation.md` | https://www.reconart.com/blog/iso-20022-adoption-transform-reconciliation-enhance-payments-unlock-efficiencies/ |
| `aci-iso-20022-migration.md` | https://www.aciworldwide.com/how-to-migrate-to-iso-20022 |

---

## Vietnam / Napas (wire tham chiếu)

| File | URL |
|------|-----|
| `napas-api-portal-intro.md` | https://api.napas.com.vn/page/introduce — danh sách sản phẩm DPP |
| `adyen-napas-card-api.md` | https://docs.adyen.com/payment-methods/napas-card/api-only |

**Lưu ý:** Spec IBFT/QR Napas đầy đủ thường gated trên portal Napas — không public scrape hết.

---

## Banking cores & Thought Machine

| File | URL |
|------|-----|
| `medium-thought-machine-vault-core.md` | Thought Machine Vault Core principles |

---

## Engineering guides & reconciliation

| File | URL |
|------|-----|
| `freecodecamp-bank-ledger-go.md` | https://www.freecodecamp.org/news/build-a-bank-ledger-in-go-with-postgresql-using-the-double-entry-accounting-principle/ |
| `levelup-robust-ledger-guide.md` | https://levelup.gitconnected.com/building-a-robust-ledger-an-engineers-guide-to-double-entry-accounting-79804045abb0 |
| `medium-double-spend-fintech.md` | https://medium.com/codetodeploy/solving-the-double-spend-system-design-patterns-for-bulletproof-fintech-ee5d73f33415 |
| `finlego-real-time-ledger.md` | https://finlego.com/blog/designing-a-real-time-ledger-system-with-double-entry-logic |
| `finlego-wallet-as-a-service.md` | https://finlego.com/blog/how-to-build-a-scalable-wallet-as-a-service-platform |
| `medium-codefarm-digital-wallet-system.md` | https://codefarm0.medium.com/designing-a-digital-wallet-system-balance-management-top-up-p2p-transfers-eed46f757dc7 |
| `tianpan-designing-paypal-transfer.md` | https://tianpan.co/notes/167-designing-paypal-money-transfer |
| `crossmint-wallet-architecture-fintech.md` | https://blog.crossmint.com/wallet-architecture-for-fintech/ |
| `stripe-payment-settlement-explained.md` | https://stripe.com/resources/more/payment-settlement-explained |
| `medium-slope-payments-ledger-pitfalls.md` | https://medium.com/slope-stories/solving-the-five-most-common-pitfalls-from-building-a-payments-ledger-0afe1a6eceae |
| `dashdevs-ledger-fintech.md` | https://dashdevs.com/blog/what-is-a-ledger-in-banking-and-fintech/ |
| `optimus-payment-reconciliation.md` | https://optimus.tech/knowledge-base/payment-reconciliation |

---

## Event sourcing & event-driven ledger

| File | URL |
|------|-----|
| `microsoft-event-sourcing.md` | https://learn.microsoft.com/en-us/azure/architecture/patterns/event-sourcing |
| `redpanda-event-sourcing-database.md` | https://www.redpanda.com/guides/event-stream-processing-event-sourcing-database |
| `baytech-event-sourcing.md` | https://www.baytechconsulting.com/blog/event-sourcing-explained-2025 |
| `oceanobe-event-driven-ledger.md` | https://oceanobe.com/news/event-driven-ledger-architectures/1872 |

---

## Concurrency & balance types

| File | URL |
|------|-----|
| `moderntreasury-locking.md` | https://www.moderntreasury.com/learn/pessimistic-locking-vs-optimistic-locking |
| `devto-locking-strategies.md` | https://dev.to/hirushi_nethmini_41168bb8/optimistic-locking-pessimistic-locking-2p30 |
| `stackoverflow-optimistic-locking-funds.md` | https://stackoverflow.com/questions/68236336/is-optimistic-locking-enough-to-secure-operations-such-as-funds-transfer |
| `relayfi-pending-vs-available.md` | https://relayfi.com/blog/does-available-balance-include-pending-transactions/ |
| `moderntreasury-balance-types-part-i.md` | posted / pending / available |

---

## Schema design (Stack Overflow) & banking cores

| File | URL |
|------|-----|
| `stackoverflow-double-entry-schema.md` | https://stackoverflow.com/questions/2494343/database-schema-design-for-a-double-entry-accounting-system |
| `stackoverflow-relational-double-entry.md` | https://stackoverflow.com/questions/59432964/relational-data-model-for-double-entry-accounting |
| `mambu-apis-overview.md` | https://docs.mambu.com/docs/mambu-apis/ |
| `mambu-gl-journal-entries.md` | https://docs.mambu.com/api/api-v2/gljournalentries/create/ |
| `mambu-transaction-holds.md` | https://docs.mambu.com/docs/transaction-holds/ |
| `mambu-authorization-holds.md` | https://docs.mambu.com/docs/authorization-holds/ |
| `increase-api-accounts.md` | https://increase.com/documentation/api/accounts |
| `tigerbeetle-create-transfers.md` | https://docs.tigerbeetle.com/coding/requests/create_transfers/ |
| `formance-numscript.md` | https://docs.formance.com/modules/numscript |

---

## Failed / đã xóa

| URL | Lý do |
|-----|-------|
| sdk.finance (2 blog) | Cookie wall |
| medium.com/@rurutia1027 payment architecture | Gateway timeout |
| MT URLs guessed (reconciliation-101, double-entry-for-engineers) | 404 |
| blnkfinance.com/docs root | Page not found — dùng `llms.txt` + `.md` URLs |
| docs.stripe.com/treasury/* | hCaptcha block — không scrape được |
| sdk.finance double-entry ledger | Cookie wall |
| docs.mambu.com/docs/deposit-accounts/ | 404 |
| blnk queued-balances.md URL đoán | null response — đã có trong `blnk-balances-intro.md` |
| MT URLs đoán (immutable-ledger) | 404 |
| medium.com Nicholas Idoko wallets Nigeria | Gateway timeout |

---

## Map → `10_core`

| Chủ đề | Reference | Dự án |
|--------|-----------|-------|
| Double-entry COA | MT accounting I–III, Square Books | `core.sharedlib.md` §6–16 |
| Wallet vs ledger | MT build wallet, Finlego WaaS | `core.wallet.md`, `design-v2/wallet.md` |
| Transaction pending/posted | MT scale III–V, TigerBeetle | `accounting.md` §5, deposit §8 |
| Immutable ledger | Square Books, Formance | ADR-001 |
| Saga / stuck flows | Microsoft, AWS, Zhorifiandi | `core.business-processes.md` §13 |
| Idempotency | Stripe, Adyen | ADR-005, `IMPLEMENTATION.md` §2 |
| Reconciliation | MT recon diaries, Stripe 101 | `accounting.md` §10, W5 |
| Double-spend / freeze | medium-double-spend | `wallet.md` §7.4, withdraw saga |
| Outbox + saga | microservices.io, InfoQ | `orchestration.md` §18, `business-processes.md` §15 |
| Posted/pending/available | MT balance types I | `wallet.md` available/frozen vs deposit PENDING |
| Optimistic locking | MT locking, StackOverflow | `wallet.md` §9, `IMPLEMENTATION.md` version |
| Event sourcing (optional) | Microsoft, Redpanda | ADR-001 append-only; không bắt buộc v1 |
| VA / deposit mapping | MT virtual accounts | `integration-surfaces.md` deposit §4.1 |
| Bounded context wallet/accounting | Fowler, Airwallex | ADR-002, ADR-003, `core.sharedlib.md` §3 |
| Stripe-scale ledger QA | stripe-dev-ledger | recon W5, `accounting.md` §10 |
| Napas products index | napas-api-portal-intro | IBFT transit 3400, `foundation.md` §11 |
| INFLIGHT/APPLIED (Blnk) | blnk-transaction-lifecycle | deposit PENDING, withdraw frozen |

---

## Master index (124 files — alphabetical)

| File | Lines | Primary map |
|------|------:|-------------|
| `acclime-ifrs-vas-vietnam.md` | 303 | accounting §29.6, ADR-036 |
| `aci-iso-20022-migration.md` | 285 | accounting §26.7 (future) |
| `adyen-api-idempotency.md` | 183 | orchestration §25.3, ADR-005 |
| `adyen-napas-card-api.md` | 298 | accounting §26.7, Napas |
| `adyen-webhooks.md` | 137 | orchestration §25.2 |
| `airwallex-ddd-payments.md` | 321 | accounting §26.5, ADR-002 |
| `alexandrubagu-tigerbeetle-overview.md` | 1001 | accounting §26.4 (informative) |
| `aws-saga-orchestration.md` | 165 | orchestration §25.1 |
| `baytech-event-sourcing.md` | 359 | accounting §26.6 |
| `becker-expense-recognition.md` | 193 | accounting §29.4 |
| `blnk-balance-monitoring.md` | 321 | ADR-032 balance monitors |
| `blnk-balance-snapshots.md` | 121 | wallet §27.1 |
| `blnk-balances-intro.md` | 270 | wallet §27.1, §26.4 |
| `blnk-double-entry-guide.md` | 315 | accounting §26.1 |
| `blnk-historical-balances.md` | 160 | wallet §27.1 |
| `blnk-llms-index.txt` | 225 | index only |
| `blnk-reconciliation-strategies.md` | 155 | accounting §26.3 |
| `blnk-reconciliations-overview.md` | 339 | accounting §26.3 |
| `blnk-transaction-lifecycle.md` | 198 | wallet §27.2, accounting §26.2 |
| `blnk-transactions-intro.md` | 442 | accounting §26.4 |
| `confluent-chris-richardson-saga.md` | 998 | orchestration §25.1 |
| `crossmint-wallet-architecture-fintech.md` | 122 | wallet §27.4, ADR-002 |
| `dashdevs-ledger-fintech.md` | 446 | accounting §26.4 |
| `decodable-outbox-pattern.md` | 311 | orchestration §25.2, ADR-013 |
| `devto-ddd-bounded-contexts.md` | 192 | orchestration §25.5 |
| `devto-locking-strategies.md` | 167 | wallet §27.3 |
| `finlego-real-time-ledger.md` | 270 | accounting §26.4 |
| `finlego-wallet-as-a-service.md` | 353 | wallet §27.4 |
| `hoiketoanhcm-nguyen-tac-luat-ke-toan-2015.md` | 160 | accounting §29.6 |
| `formance-accounting-model.md` | 10 | accounting §26.4 |
| `formance-defining-double-entry.md` | 120 | accounting §26.1 |
| `formance-examples-intro.md` | 149 | accounting §26.4 |
| `formance-how-not-to-build-ledger.md` | 239 | accounting §27 pitfalls |
| `formance-ledger-accounts.md` | 104 | accounting §26.4 |
| `formance-ledger-module.md` | 50 | accounting §26.4 |
| `formance-ledger-quick-start.md` | 101 | accounting §26.4 |
| `formance-numscript.md` | 104 | accounting §26.4 |
| `freecodecamp-bank-ledger-go.md` | 815 | accounting §26.1 |
| `incorp-ifrs-vas-vietnam.md` | 222 | accounting §29.6 |
| `increase-api-accounts.md` | 1127 | wallet §27.5, ADR-030 |
| `investopedia-accounting-equation.md` | 523 | accounting §29.3 |
| `investopedia-accounting-principles.md` | 568 | accounting §29.2 |
| `investopedia-accrual-accounting.md` | 498 | accounting §29.2 |
| `investopedia-gaap.md` | 609 | accounting §29.2 |
| `infoq-saga-orchestration-outbox.md` | 489 | orchestration §25.1–25.2 |
| `interledger-rafiki-tigerbeetle.md` | 130 | accounting §26.4 |
| `levelup-idempotency-payments.md` | 174 | orchestration §25.3 |
| `levelup-robust-ledger-guide.md` | 488 | accounting §26.1 |
| `lumen-debits-credits-rules.md` | 88 | accounting §29.3 |
| `mambu-apis-overview.md` | 48 | schema § (informative) |
| `mambu-authorization-holds.md` | 248 | wallet §27.2 (out of scope) |
| `mambu-gl-journal-entries.md` | 640 | accounting §26.4 |
| `mambu-transaction-holds.md` | 218 | wallet §27.2 |
| `martinfowler-bounded-context.md` | 110 | accounting §26.5, ADR-002 |
| `martinfowler-two-hard-things.md` | 61 | naming (informative) |
| `medium-codefarm-digital-wallet-system.md` | 137 | wallet §27.4, orchestration §25.6 |
| `medium-double-spend-fintech.md` | 401 | wallet §27.2, ADR-007 |
| `medium-slope-payments-ledger-pitfalls.md` | 415 | accounting §27, ADR-031 |
| `medium-thought-machine-vault-core.md` | 166 | wallet §28 gap |
| `microservices-io-saga.md` | 294 | orchestration §25.1 |
| `microservices-io-transactional-outbox.md` | 598 | orchestration §25.2, ADR-013 |
| `microsoft-data-store-overview.md` | 501 | architecture (informative) |
| `microsoft-event-sourcing.md` | 344 | accounting §26.6 |
| `microsoft-saga-pattern.md` | 237 | orchestration §25.1, ADR-008 |
| `moderntreasury-accounting-dev-part-i.md` | 275 | accounting §26.1, foundation §6 |
| `moderntreasury-accounting-dev-part-ii.md` | 223 | accounting §26.1 |
| `moderntreasury-accounting-dev-part-iii.md` | 295 | accounting §26.1 |
| `moderntreasury-balance-types-part-i.md` | 237 | wallet §27.1 |
| `moderntreasury-digital-wallet-tutorial.md` | 553 | wallet §27.4 |
| `moderntreasury-enforcing-immutability.md` | 135 | accounting §29.1, ADR-001 |
| `moderntreasury-fx-wallets-tutorial.md` | 342 | out of scope ADR-019 |
| `moderntreasury-gaap-accounting-rules.md` | 142 | accounting §29.2 |
| `moderntreasury-how-to-build-digital-wallet.md` | 346 | wallet §27.4, orchestration §25.6 |
| `moderntreasury-learn-digital-wallet.md` | 129 | wallet §27.4 |
| `moderntreasury-ledger-transaction-status.md` | 114 | accounting §26.2 |
| `moderntreasury-ledgering-investing.md` | 159 | accounting (informative) |
| `moderntreasury-locking.md` | 149 | wallet §27.3 |
| `moderntreasury-recon-diaries-1.md` | 188 | accounting §26.3 |
| `moderntreasury-recon-knapsack.md` | 183 | accounting §26.3 |
| `moderntreasury-scale-ledger-part-i.md` | 188 | accounting §26.2 |
| `moderntreasury-scale-ledger-part-ii.md` | 283 | accounting §26.2 |
| `moderntreasury-scale-ledger-part-iii.md` | 174 | accounting §26.2 |
| `moderntreasury-scale-ledger-part-iv.md` | 307 | accounting §26.2, wallet §28 |
| `moderntreasury-scale-ledger-part-v.md` | 229 | accounting §26.2 |
| `moderntreasury-single-vs-double-entry.md` | 109 | accounting §26.1 |
| `moderntreasury-transaction-reconciliation.md` | 105 | accounting §26.3 |
| `moderntreasury-virtual-accounts.md` | 94 | wallet §27.5, ADR-030 |
| `moderntreasury-what-is-digital-wallet.md` | 10 | wallet §27.4 |
| `moderntreasury-what-is-ledger-database.md` | 10 | accounting (glossary) |
| `moderntreasury-what-is-reconciliation.md` | 10 | accounting §26.3 |
| `napas-api-portal-intro.md` | 1431 | accounting §26.7, orchestration §25.4 |
| `netsuite-cash-vs-accrual.md` | 551 | accounting §29.2 |
| `netsuite-debits-credits.md` | 425 | accounting §29.3 |
| `oceanobe-event-driven-ledger.md` | 294 | accounting §26.6 |
| `optimus-payment-reconciliation.md` | 355 | accounting §26.3 |
| `oracle-ddd-composable-banking.md` | 574 | accounting §26.5 |
| `payments-canada-iso-20022.md` | 355 | accounting §26.7 (future) |
| `pragmatic-engineer-designing-payment-system.md` | 849 | orchestration §25.3 |
| `reconart-iso-20022-reconciliation.md` | 192 | accounting §26.3, §26.7 |
| `redpanda-event-sourcing-database.md` | 317 | accounting §26.6 |
| `relayfi-pending-vs-available.md` | 135 | wallet §27.1 |
| `square-books-double-entry-ledger.md` | 165 | accounting §26.1, ADR-001 |
| `stackoverflow-double-entry-schema.md` | 370 | accounting §26.1 |
| `stackoverflow-optimistic-locking-funds.md` | 338 | wallet §27.3 |
| `stackoverflow-relational-double-entry.md` | 1038 | accounting §26.1 |
| `stripe-api-idempotent-requests.md` | 1491 | orchestration §25.3, ADR-005 |
| `stripe-dev-ledger-system.md` | 259 | accounting §26.3, W5 |
| `stripe-dev-ledger-system-markdown.md` | 177 | accounting §26.3 |
| `stripe-dev-payment-api-design.md` | 348 | orchestration §25.3 |
| `stripe-dev-stripe-credits.md` | 211 | fee model (informative) |
| `stripe-idempotency-blog.md` | 116 | orchestration §25.3 |
| `stripe-payment-reconciliation-101.md` | 275 | accounting §26.3 |
| `stripe-payment-settlement-explained.md` | 277 | wallet §27.5, orchestration §25.4 |
| `swift-iso-20022-chapter2.md` | 86 | accounting §26.7 |
| `tapchicongthuong-luat-ke-toan-2015.md` | 463 | accounting §29.6 (context) |
| `temporal-saga-patterns.md` | 243 | ADR-035 (informative; v1 RabbitMQ) |
| `tigerbeetle-create-accounts.md` | 4 | accounting §26.4 (stub) |
| `tigerbeetle-create-transfers.md` | 4 | accounting §26.4 (stub) |
| `tigerbeetle-data-modeling.md` | 305 | accounting §26.2 |
| `tigerbeetle-financial-accounting.md` | 238 | accounting §26.2 |
| `tianpan-designing-paypal-transfer.md` | 246 | wallet §27.4, orchestration §25.6 |
| `vn-luat-ke-toan-2015-dieu-5-7.md` | 58 | accounting §29.6, ADR-036 |
| `zhorifiandi-saga-stuck-systems.md` | 377 | orchestration §25.1, ADR-021 |
