package com.gtelpay.app.orchestration.gateway.impl;

import com.gtelpay.app.orchestration.gateway.LedgerGateway;
import com.gtelpay.core.accounting.service.CreateJournalCommand;
import com.gtelpay.core.accounting.service.JournalHeader;
import com.gtelpay.core.accounting.service.JournalLineCommand;
import com.gtelpay.core.accounting.service.JournalService;
import com.gtelpay.core.accounting.service.PostJournalResult;
import com.gtelpay.core.accounting.service.ReverseJournalCommand;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * v1 in-process accounting gateway — same JVM as the orchestrator (ADR-003).
 * Swap for an HTTP client (S2) to split into a separate process.
 */
@Component
public class InProcessLedgerGateway implements LedgerGateway {

    private final JournalService journalService;

    public InProcessLedgerGateway(JournalService journalService) {
        this.journalService = journalService;
    }

    @Override
    public JournalHeader createJournal(CreateJournalCommand cmd) {
        return journalService.createJournal(cmd);
    }

    @Override
    public void addLines(long coaTransId, List<JournalLineCommand> lines) {
        journalService.addLines(coaTransId, lines);
    }

    @Override
    public PostJournalResult postJournal(long coaTransId) {
        return journalService.postJournal(coaTransId);
    }

    @Override
    public PostJournalResult confirmDeposit(long coaTransId, BigDecimal fee) {
        return journalService.confirmDeposit(coaTransId, fee);
    }

    @Override
    public JournalHeader reverseJournal(long coaTransId, ReverseJournalCommand cmd) {
        return journalService.reverseJournal(coaTransId, cmd);
    }
}
