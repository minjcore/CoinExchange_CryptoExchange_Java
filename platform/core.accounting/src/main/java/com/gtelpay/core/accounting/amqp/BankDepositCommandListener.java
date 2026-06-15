package com.gtelpay.core.accounting.amqp;

import com.gtelpay.core.accounting.domain.JournalStatus;
import com.gtelpay.core.accounting.service.CreateJournalCommand;
import com.gtelpay.core.accounting.service.JournalHeader;
import com.gtelpay.core.accounting.service.JournalService;
import com.gtelpay.core.accounting.validation.DepositPostingValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Consumes {@code BANK_DEPOSIT} (asyncapi/core-commands.yaml) and drives the two-phase
 * deposit journal PENDING → POSTED (foundation §8, use-cases.md UC-1).
 * Idempotent on {@code businessRef}: createJournal + confirmDeposit are replay-safe;
 * a redelivery after POSTED is a no-op.
 */
@Component
@ConditionalOnProperty(name = "accounting.amqp.enabled", havingValue = "true")
public class BankDepositCommandListener {

    private static final Logger log = LoggerFactory.getLogger(BankDepositCommandListener.class);

    private final JournalService journalService;

    public BankDepositCommandListener(JournalService journalService) {
        this.journalService = journalService;
    }

    @RabbitListener(queues = AccountingAmqpConfig.BANK_DEPOSIT_QUEUE)
    public void onBankDeposit(Map<String, Object> envelope) {
        String businessRef = (String) envelope.get("businessRef");
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) envelope.get("payload");
        String amount = payload != null ? String.valueOf(payload.get("amount")) : null;
        if (businessRef == null || amount == null) {
            throw new IllegalArgumentException("BANK_DEPOSIT envelope missing businessRef/amount");
        }

        JournalHeader journal = journalService.createJournal(
                new CreateJournalCommand(businessRef, "DEPOSIT", "bank deposit", null));
        if (journal.status() == JournalStatus.POSTED) {
            log.debug("deposit {} already POSTED — idempotent no-op", businessRef);
            return;
        }
        journalService.addLines(journal.id(), DepositPostingValidator.phaseALines(amount, "VND"));
        // Command carries no fee → net = gross (no 4110 line). Fee schedule is orchestration-owned.
        journalService.confirmDeposit(journal.id(), BigDecimal.ZERO);
        log.info("deposit {} POSTED (coaTransId={})", businessRef, journal.id());
    }
}
