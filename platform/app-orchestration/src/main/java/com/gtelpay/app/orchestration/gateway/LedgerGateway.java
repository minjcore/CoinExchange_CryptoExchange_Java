package com.gtelpay.app.orchestration.gateway;

import com.gtelpay.core.accounting.service.CreateJournalCommand;
import com.gtelpay.core.accounting.service.JournalHeader;
import com.gtelpay.core.accounting.service.JournalLineCommand;
import com.gtelpay.core.accounting.service.PostJournalResult;
import com.gtelpay.core.accounting.service.ReverseJournalCommand;

import java.math.BigDecimal;
import java.util.List;

/**
 * Orchestrator's seam to the accounting domain. In-process today
 * ({@code InProcessLedgerGateway}); an HTTP client (S2 {@code accounting-internal.yaml})
 * is a drop-in replacement since the orchestrator depends only on this interface.
 */
public interface LedgerGateway {

    /**
     * Create journal, add lines, and post atomically — TX 2 of the 3-commit pattern (ADR-027).
     * Each gateway implementation owns its own transaction boundary; callers must NOT wrap
     * this in an outer @Transactional.
     */
    PostJournalResult createAndPost(CreateJournalCommand cmd, List<JournalLineCommand> lines);

    PostJournalResult confirmDeposit(long coaTransId, BigDecimal fee);

    JournalHeader reverseJournal(long coaTransId, ReverseJournalCommand cmd);

    JournalHeader createPendingWithdraw(String businessRef, BigDecimal gross, String currency);

    PostJournalResult confirmWithdraw(long coaTransId, BigDecimal principal, BigDecimal fee);

    void voidWithdraw(long coaTransId);
}
