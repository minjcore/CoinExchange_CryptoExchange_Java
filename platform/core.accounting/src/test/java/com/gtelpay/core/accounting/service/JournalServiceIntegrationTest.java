package com.gtelpay.core.accounting.service;

import com.gtelpay.core.accounting.domain.LineSide;
import com.gtelpay.core.accounting.domain.CoaPeriodEntity;
import com.gtelpay.core.accounting.domain.PeriodStatus;
import com.gtelpay.core.accounting.service.impl.JournalBalanceValidator;
import com.gtelpay.core.accounting.domain.CoaTransDataEntity;
import com.gtelpay.core.accounting.repository.CoaPeriodRepository;
import com.gtelpay.core.accounting.repository.CoaTransDataRepository;
import com.gtelpay.core.accounting.validation.DepositPostingValidator;
import com.gtelpay.core.sharedlib.exception.AccountingException;
import com.gtelpay.core.sharedlib.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;
import java.time.LocalDate;
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

    @Autowired
    private CoaPeriodRepository coaPeriodRepository;

    // A1 (ADR-010): a balanced journal that still strands a transit account must NOT post.
    @Test
    void balancedButStrandedTransit_rejectsPost() {
        JournalHeader header = journalService.createJournal(
                new CreateJournalCommand("trf-stranded", "TRANSFER", null, null));
        // DR = CR (balanced), but 3300 nets -100000 (never cleared) → must be rejected.
        journalService.addLines(header.id(), List.of(
                line("2110", "100000", LineSide.DEBIT),
                line("3300", "100000", LineSide.CREDIT)));

        AccountingException ex = assertThrows(
                AccountingException.class, () -> journalService.postJournal(header.id()));
        assertEquals(ErrorCode.ACCOUNTING_UNBALANCED_JOURNAL, ex.errorCode());
    }

    // A4 (ADR-023): posting into a CLOSED period is rejected. Uses a distinct month (2020-01)
    // so it can't affect other tests posting in the current month.
    @Test
    void closedPeriod_rejectsPost() {
        coaPeriodRepository.save(period("2020-01", PeriodStatus.CLOSED));

        JournalHeader header = journalService.createJournal(
                new CreateJournalCommand("pay-closed", "PAYMENT", null, LocalDate.of(2020, 1, 15)));
        // balanced + no transit → only the period guard can reject this.
        journalService.addLines(header.id(), List.of(
                line("2110", "100000", LineSide.DEBIT),
                line("1111", "100000", LineSide.CREDIT)));

        AccountingException ex = assertThrows(
                AccountingException.class, () -> journalService.postJournal(header.id()));
        assertEquals(ErrorCode.ACCOUNTING_PERIOD_CLOSED, ex.errorCode());
    }

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

    // A3 (ADR-001): a POSTED journal line is write-once. @Immutable makes Hibernate refuse the
    // UPDATE — tampering the amount in-memory and saving must NOT change the stored row.
    @Test
    void postedLine_tamperIsIgnored_immutable() {
        JournalHeader header = journalService.createJournal(
                new CreateJournalCommand("imm-1", "PAYMENT", null, null));
        journalService.addLines(header.id(), List.of(
                line("2110", "100000", LineSide.DEBIT),
                line("3500", "100000", LineSide.CREDIT),
                line("3500", "100000", LineSide.DEBIT),
                line("2120", "100000", LineSide.CREDIT)));
        journalService.postJournal(header.id());

        CoaTransDataEntity target = coaTransDataRepository.findByCoaTransId(header.id()).get(0);
        Long lineId = target.getId();
        BigDecimal original = target.getAmount();

        // Attempt to tamper a POSTED line and persist it.
        target.setAmount(bd("999999"));
        coaTransDataRepository.save(target);

        // Re-read fresh from the DB: the UPDATE was never emitted (@Immutable).
        CoaTransDataEntity reloaded = coaTransDataRepository.findById(lineId).orElseThrow();
        assertEquals(0, reloaded.getAmount().compareTo(original),
                "POSTED line amount must be unchanged after a tamper attempt");
    }

    private static JournalLineCommand line(String account, String amount, LineSide side) {
        return new JournalLineCommand(account, bd(amount), side, "VND");
    }

    private static CoaPeriodEntity period(String code, PeriodStatus status) {
        CoaPeriodEntity p = new CoaPeriodEntity();
        p.setPeriodCode(code);
        p.setStatus(status);
        return p;
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value).setScale(4);
    }
}
