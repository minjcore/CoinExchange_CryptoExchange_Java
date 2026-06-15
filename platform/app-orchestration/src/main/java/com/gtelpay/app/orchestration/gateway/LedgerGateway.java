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

    JournalHeader createJournal(CreateJournalCommand cmd);

    void addLines(long coaTransId, List<JournalLineCommand> lines);

    PostJournalResult postJournal(long coaTransId);

    PostJournalResult confirmDeposit(long coaTransId, BigDecimal fee);

    JournalHeader reverseJournal(long coaTransId, ReverseJournalCommand cmd);
}
