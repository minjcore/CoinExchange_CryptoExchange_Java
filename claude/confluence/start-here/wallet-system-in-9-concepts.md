# 9 Things to Know Before Coding GtelPay Core

> **CF page ID:** 51839571 | **Parent:** 📌 Start Here (51315064)
> **Source of truth:** this file → push to CF

These 9 fundamentals underpin every e-money system. After reading them you will be able to picture the full money flow — where it enters, where it is stored, and why it cannot be duplicated or lost.

---

1. **Schema boundary** — `wallet.*` and `accounting.*` never JOIN or share foreign keys. `wallet_tx.coa_trans_id` = correlation only. (ADR-003)

2. **Wallet balance is a snapshot** — read one row, never derived from a sum. Each mutation = one `wallet_tx` in the same DB transaction. (ADR-004)

3. **Deposit is two-phase** — Phase A: TB `flags.pending` (1111→3100). Phase B: `confirmDeposit` → post + 3100→2110+4110. Transit 3100 = 0 after Phase B, no exceptions. (ADR-006, ADR-010)

4. **RabbitMQ is inbound** — orchestration publishes commands → worker. Kafka = outbound events from workers to downstream. (ADR-041, ADR-013)

5. **businessRef runs end-to-end** — `X-Idempotency-Key` → outbox → `BANK_DEPOSIT` → `coa_trans.reference_id` → `WALLET_CREDIT` → `wallet_tx.business_ref`. Replay with the same data = return existing IDs, no new records created. (ADR-005)

6. **Fee computed once at orchestration** — workers receive `grossAmount` + pre-computed `fee`, never recompute it. Fee 4110 is on the same journal as the movement. (ADR-009, ADR-028)

7. **POSTED = immutable** — never UPDATE or DELETE `coa_trans_data` of a POSTED journal. Corrections = a new reversing journal. (ADR-001)

8. **Wallet credit only after POSTED** — mandatory gate: `coa_trans.status = POSTED` before INSERT `wallet_tx`. No credit on Phase A. (ADR-024, ADR-026)

9. **Fail-fast at every boundary** — VND + scale≤4 + JWT `sub` enforced at orchestration. LOCKED wallet rejects debit/deposit. Transit≠0 rejects `postJournal`. No silent absorption. (ADR-019, ADR-029, ADR-010, ADR-011)

---

> For detail on each concept, see [Architecture FAQ](https://nivc.atlassian.net/wiki/spaces/GtelPay/pages/51544171) and [ADR pre-reading checklist](https://nivc.atlassian.net/wiki/spaces/GtelPay/pages/51872153).
