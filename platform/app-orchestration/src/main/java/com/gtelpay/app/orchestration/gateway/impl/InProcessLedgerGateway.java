package com.gtelpay.app.orchestration.gateway.impl;

import com.gtelpay.app.orchestration.gateway.LedgerGateway;
import com.gtelpay.core.accounting.service.CreateJournalCommand;
import com.gtelpay.core.accounting.service.JournalHeader;
import com.gtelpay.core.accounting.service.JournalLineCommand;
import com.gtelpay.core.accounting.service.JournalService;
import com.gtelpay.core.accounting.service.PostJournalResult;
import com.gtelpay.core.accounting.service.ReverseJournalCommand;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * v1 in-process accounting gateway — same JVM as the orchestrator (ADR-003).
 * Swap for an HTTP client (S2) to split into a separate process.
 *
 * This gateway owns its transaction boundaries. With HTTP the network call is
 * the natural TX boundary; here @Transactional makes that explicit so callers
 * (use-cases) must NOT add an outer @Transactional.
 */
@Component
public class InProcessLedgerGateway implements LedgerGateway {

    private final JournalService journalService;

    public InProcessLedgerGateway(JournalService journalService) {
        this.journalService = journalService;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public PostJournalResult createAndPost(CreateJournalCommand cmd, List<JournalLineCommand> lines) {
        var journal = journalService.createJournal(cmd);
        journalService.addLines(journal.id(), lines);
        return journalService.postJournal(journal.id());
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public PostJournalResult confirmDeposit(long coaTransId, BigDecimal fee) {
        return journalService.confirmDeposit(coaTransId, fee);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public JournalHeader reverseJournal(long coaTransId, ReverseJournalCommand cmd) {
        return journalService.reverseJournal(coaTransId, cmd);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public JournalHeader createPendingWithdraw(String businessRef, BigDecimal gross, String currency) {
        return journalService.createPendingWithdraw(businessRef, gross, currency);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public PostJournalResult confirmWithdraw(long coaTransId, BigDecimal principal, BigDecimal fee) {
        return journalService.confirmWithdraw(coaTransId, principal, fee);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void voidWithdraw(long coaTransId) {
        journalService.voidWithdraw(coaTransId);
    }
}
