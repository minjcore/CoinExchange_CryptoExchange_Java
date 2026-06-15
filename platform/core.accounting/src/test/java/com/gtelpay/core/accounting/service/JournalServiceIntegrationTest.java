package com.gtelpay.core.accounting.service;

import com.gtelpay.core.accounting.domain.LineSide;
import com.gtelpay.core.accounting.service.impl.JournalBalanceValidator;
import com.gtelpay.core.accounting.domain.CoaTransDataEntity;
import com.gtelpay.core.accounting.repository.CoaTransDataRepository;
import com.gtelpay.core.accounting.validation.DepositPostingValidator;
import com.gtelpay.core.foundation.exception.AccountingException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = com.gtelpay.core.accounting.AccountingModuleApplication.class)
@Sql(scripts = "/coa-seed.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
class JournalServiceIntegrationTest {

    @Autowired
    private JournalService journalService;

    @Autowired
    private CoaTransDataRepository coaTransDataRepository;

    @Test
    void deposit_pendingToPosted_transit3100Zero() {
        JournalHeader header = journalService.createJournal(
                new CreateJournalCommand("dep-001", "DEPOSIT", null, null));

        journalService.addLines(header.id(), List.of(
                line("1111", "100000", LineSide.DEBIT),
                line("3100", "100000", LineSide.CREDIT)));

        PostJournalResult posted = journalService.confirmDeposit(header.id(), bd("1000"));

        assertEquals(false, posted.idempotentReplay());
        List<CoaTransDataEntity> lines = coaTransDataRepository.findByCoaTransId(header.id());
        assertEquals(0, JournalBalanceValidator.transitNet("3100", lines).compareTo(BigDecimal.ZERO));

        PostJournalResult replay = journalService.confirmDeposit(header.id(), bd("1000"));
        assertTrue(replay.idempotentReplay());
    }

    @Test
    void duplicateReferenceId_returnsSameJournalId() {
        CreateJournalCommand cmd = new CreateJournalCommand("dep-dup", "DEPOSIT", null, null);
        JournalHeader first = journalService.createJournal(cmd);
        JournalHeader second = journalService.createJournal(cmd);
        assertEquals(first.id(), second.id());
    }

    @Test
    void unbalancedLines_rejectsPost() {
        JournalHeader header = journalService.createJournal(
                new CreateJournalCommand("pay-bad", "PAYMENT", null, null));
        journalService.addLines(header.id(), List.of(
                line("2110", "100000", LineSide.DEBIT),
                line("3500", "50000", LineSide.CREDIT)));

        assertThrows(AccountingException.class, () -> journalService.postJournal(header.id()));
    }

    @Test
    void paymentLines_transit3500Zero() {
        JournalHeader header = journalService.createJournal(
                new CreateJournalCommand("pay-001", "PAYMENT", null, null));

        journalService.addLines(header.id(), List.of(
                line("2110", "100000", LineSide.DEBIT),
                line("3500", "100000", LineSide.CREDIT),
                line("3500", "100000", LineSide.DEBIT),
                line("2120", "100000", LineSide.CREDIT)));

        journalService.postJournal(header.id());

        List<CoaTransDataEntity> lines = coaTransDataRepository.findByCoaTransId(header.id());
        assertEquals(0, JournalBalanceValidator.transitNet("3500", lines).compareTo(BigDecimal.ZERO));
    }

    @Test
    void depositFlow_viaListenerTemplate_postsLedgerTransitZero() {
        // Mirrors BankDepositCommandListener: createJournal -> phase-A template -> confirmDeposit(fee 0).
        JournalHeader header = journalService.createJournal(
                new CreateJournalCommand("dep-flow", "DEPOSIT", "bank deposit", null));
        journalService.addLines(header.id(), DepositPostingValidator.phaseALines("100000", "VND"));

        // Listener passes fee 0 (no fee in BANK_DEPOSIT command) — must be accepted.
        PostJournalResult posted = journalService.confirmDeposit(header.id(), BigDecimal.ZERO);

        assertEquals(false, posted.idempotentReplay());
        List<CoaTransDataEntity> lines = coaTransDataRepository.findByCoaTransId(header.id());
        assertEquals(0, JournalBalanceValidator.transitNet("3100", lines).compareTo(BigDecimal.ZERO));
        // fee 0 -> 2110 credited net = gross 100000, no 4110 line; balanced + transit cleared.
    }

    private static JournalLineCommand line(String account, String amount, LineSide side) {
        return new JournalLineCommand(account, bd(amount), side, "VND");
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value).setScale(4);
    }
}
