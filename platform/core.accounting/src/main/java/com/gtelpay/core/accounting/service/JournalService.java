package com.gtelpay.core.accounting.service;

import java.math.BigDecimal;
import java.util.List;

public interface JournalService {

    JournalHeader createJournal(CreateJournalCommand cmd);

    void addLines(long coaTransId, List<JournalLineCommand> lines);

    PostJournalResult postJournal(long coaTransId);

    PostJournalResult confirmDeposit(long coaTransId, BigDecimal fee);

    JournalHeader reverseJournal(long coaTransId, ReverseJournalCommand cmd);

    /** Accept phase: create PENDING journal + add Phase A lines (2110 DR / 3200 CR). Idempotent on businessRef. */
    JournalHeader createPendingWithdraw(String businessRef, BigDecimal gross, String currency);

    /** Settle phase: add Phase B lines (3200 DR / 1111 CR principal + 4120 CR fee) and post. Idempotent if already POSTED. */
    PostJournalResult confirmWithdraw(long coaTransId, BigDecimal principal, BigDecimal fee);

    /** Release phase: mark journal FAILED (void pending). Idempotent if already FAILED. */
    void voidWithdraw(long coaTransId);

    /** IBFT accept phase: PENDING journal + Phase A lines (2110 DR / 3400 CR). Idempotent on businessRef. */
    JournalHeader createPendingIbft(String businessRef, BigDecimal gross, String currency);

    /** IBFT settle phase: Phase B lines (3400 DR / 4130 CR + 1112 CR, plus 5100 DR / 1112 CR Napas cost) and post. */
    PostJournalResult confirmIbft(long coaTransId, BigDecimal principal, BigDecimal platformFee, BigDecimal napasCost);

    /** IBFT release phase: mark journal FAILED. Idempotent if already FAILED. */
    void voidIbft(long coaTransId);
}
