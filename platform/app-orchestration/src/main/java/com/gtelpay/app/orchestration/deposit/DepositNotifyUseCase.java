package com.gtelpay.app.orchestration.deposit;

import com.gtelpay.core.sharedlib.exception.ValidationException;
import com.gtelpay.core.sharedlib.util.MoneyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Receives bank deposit webhook, validates VA, and publishes BANK_DEPOSIT command to
 * the accounting worker via RabbitMQ (ADR-041, ADR-006).
 *
 * <p>Returns 202 immediately — no journal writes in this path (ADR-038).
 * When deposit.amqp.enabled=false the command is logged but not published (stub mode).
 */
@Service
public class DepositNotifyUseCase {

    private static final Logger log = LoggerFactory.getLogger(DepositNotifyUseCase.class);

    private final VirtualAccountRepository vaRepo;

    // null when deposit.amqp.enabled=false
    @Autowired(required = false)
    @Qualifier("depositRabbitTemplate")
    private RabbitTemplate depositRabbitTemplate;

    public DepositNotifyUseCase(VirtualAccountRepository vaRepo) {
        this.vaRepo = vaRepo;
    }

    public DepositAck execute(DepositNotification req) {
        if (req.businessRef() == null || req.businessRef().isBlank()) {
            throw new ValidationException("businessRef is required");
        }
        if (req.virtualAccount() == null || req.virtualAccount().isBlank()) {
            throw new ValidationException("virtualAccount is required");
        }
        MoneyUtil.parseAmount(req.grossAmount()); // validates > 0, scale 4

        VirtualAccountEntity va = vaRepo.findByVaNumber(req.virtualAccount())
                .orElseThrow(() -> new ValidationException("UNKNOWN_VA: " + req.virtualAccount()));

        publishBankDeposit(req, va.getMemberId(), va.getWalletId());

        log.info("deposit ACCEPTED businessRef={} memberId={} walletId={}",
                req.businessRef(), va.getMemberId(), va.getWalletId());

        return new DepositAck(req.businessRef(), "ACCEPTED");
    }

    private void publishBankDeposit(DepositNotification req, long memberId, long walletId) {
        // v1: no deposit fee
        String fee = MoneyUtil.normalizeAllowZero(BigDecimal.ZERO).toPlainString();

        Map<String, Object> payload = new HashMap<>();
        payload.put("grossAmount", req.grossAmount());
        payload.put("fee", fee);
        payload.put("currency", req.currency() != null ? req.currency() : "VND");
        payload.put("walletId", walletId);
        payload.put("virtualAccount", req.virtualAccount());
        payload.put("bankRef", req.bankRef());

        Map<String, Object> cmd = new HashMap<>();
        cmd.put("messageId", UUID.randomUUID().toString());
        cmd.put("businessRef", req.businessRef());
        cmd.put("memberId", memberId);
        cmd.put("commandType", "BANK_DEPOSIT");
        cmd.put("occurredAt", Instant.now().toString());
        cmd.put("schemaVersion", "1.0");
        cmd.put("source", "orchestration");
        cmd.put("payload", payload);

        if (depositRabbitTemplate == null) {
            log.warn("deposit.amqp.enabled=false — BANK_DEPOSIT not published (businessRef={})", req.businessRef());
            return;
        }

        depositRabbitTemplate.convertAndSend(
                DepositAmqpConfig.EXCHANGE,
                DepositAmqpConfig.BANK_DEPOSIT_ROUTING_KEY,
                cmd);

        log.debug("BANK_DEPOSIT published businessRef={} memberId={} gross={}",
                req.businessRef(), memberId, req.grossAmount());
    }
}
