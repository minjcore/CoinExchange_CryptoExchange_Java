package com.gtelpay.core.accounting.amqp;

import com.gtelpay.core.accounting.domain.JournalStatus;
import com.gtelpay.core.accounting.service.CreateJournalCommand;
import com.gtelpay.core.accounting.service.JournalHeader;
import com.gtelpay.core.accounting.service.JournalService;
import com.gtelpay.core.accounting.service.PostJournalResult;
import com.gtelpay.core.accounting.validation.DepositPostingValidator;
import com.gtelpay.core.sharedlib.util.MoneyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Consumes {@code BANK_DEPOSIT} (async-api/core-commands.yaml §BankDepositPayload) and
 * drives the two-phase deposit journal PENDING → POSTED (ADR-006, ADR-041).
 *
 * <p>After POSTED, publishes {@code WALLET_CREDIT} to {@code core.commands} exchange so the
 * wallet worker can credit the member's pocket (ADR-024 Path B).
 *
 * <p>Idempotent on {@code businessRef}: createJournal + confirmDeposit are replay-safe.
 * A redelivery after POSTED is a no-op; WALLET_CREDIT is still published to handle the case
 * where the previous publish was lost before ack.
 */
@Component
@ConditionalOnProperty(name = "accounting.amqp.enabled", havingValue = "true")
public class BankDepositCommandListener {

    private static final Logger log = LoggerFactory.getLogger(BankDepositCommandListener.class);

    private final JournalService  journalService;
    private final RabbitTemplate  rabbitTemplate;

    public BankDepositCommandListener(JournalService journalService,
                                      @Qualifier("accountingRabbitTemplate") RabbitTemplate rabbitTemplate) {
        this.journalService = journalService;
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(queues = AccountingAmqpConfig.BANK_DEPOSIT_QUEUE,
                    containerFactory = AccountingAmqpConfig.LISTENER_FACTORY)
    public void onBankDeposit(Map<String, Object> envelope) {
        var ctx = DepositContext.from(envelope);

        JournalHeader journal = journalService.createJournal(
                new CreateJournalCommand(ctx.businessRef(), "DEPOSIT", "bank deposit", null));

        if (journal.status() != JournalStatus.POSTED) {
            journalService.addLines(journal.id(),
                    DepositPostingValidator.phaseALines(ctx.grossAmount(), ctx.currency()));
            journalService.confirmDeposit(journal.id(), ctx.fee());
            log.info("deposit {} POSTED (coaTransId={})", ctx.businessRef(), journal.id());
        } else {
            log.debug("deposit {} already POSTED (coaTransId={}) — idempotent no-op",
                    ctx.businessRef(), journal.id());
        }

        publishWalletCredit(ctx, journal.id());
    }

    private void publishWalletCredit(DepositContext ctx, long coaTransId) {
        var netAmount = MoneyUtil.normalize(
                MoneyUtil.parseAmount(ctx.grossAmount()).subtract(ctx.fee()));

        Map<String, Object> payload = new HashMap<>();
        payload.put("walletId",   ctx.walletId());
        payload.put("netAmount",  netAmount.toPlainString());
        payload.put("currency",   ctx.currency());
        payload.put("coaTransId", coaTransId);

        Map<String, Object> cmd = new HashMap<>();
        cmd.put("messageId",     UUID.randomUUID().toString());
        cmd.put("businessRef",   ctx.businessRef());
        cmd.put("memberId",      ctx.memberId());
        cmd.put("commandType",   "WALLET_CREDIT");
        cmd.put("occurredAt",    Instant.now().toString());
        cmd.put("schemaVersion", "1.0");
        cmd.put("source",        "accounting-worker");
        cmd.put("payload",       payload);

        rabbitTemplate.convertAndSend(
                AccountingAmqpConfig.EXCHANGE,
                AccountingAmqpConfig.WALLET_CREDIT_ROUTING_KEY,
                cmd);
        log.debug("WALLET_CREDIT published for businessRef={} walletId={} net={}",
                ctx.businessRef(), ctx.walletId(), netAmount.toPlainString());
    }

    // -------------------------------------------------------------------------

    /**
     * Typed view of the {@code BANK_DEPOSIT} command envelope.
     * Field names match {@code core-commands.yaml §BankDepositPayload}.
     */
    private record DepositContext(
            String     businessRef,
            long       memberId,
            String     grossAmount,
            BigDecimal fee,
            String     currency,
            long       walletId) {

        @SuppressWarnings("unchecked")
        static DepositContext from(Map<String, Object> envelope) {
            String businessRef = (String) required(envelope, "businessRef");
            long   memberId    = ((Number) required(envelope, "memberId")).longValue();

            var payload     = (Map<String, Object>) required(envelope, "payload");
            String gross    = String.valueOf(required(payload, "grossAmount"));
            String feStr    = payload.containsKey("fee") ? String.valueOf(payload.get("fee")) : "0.0000";
            String currency = payload.containsKey("currency") ? String.valueOf(payload.get("currency")) : "VND";
            long   walletId = ((Number) required(payload, "walletId")).longValue();

            return new DepositContext(businessRef, memberId, gross,
                    MoneyUtil.parseAmount(feStr), currency, walletId);
        }

        private static Object required(Map<String, Object> map, String key) {
            Object v = map.get(key);
            if (v == null) throw new IllegalArgumentException("BANK_DEPOSIT missing field: " + key);
            return v;
        }
    }
}
