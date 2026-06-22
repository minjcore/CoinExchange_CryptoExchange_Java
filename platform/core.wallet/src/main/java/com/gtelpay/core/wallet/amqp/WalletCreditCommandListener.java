package com.gtelpay.core.wallet.amqp;

import com.gtelpay.core.sharedlib.util.MoneyUtil;
import com.gtelpay.core.wallet.service.WalletCommandService;
import com.gtelpay.core.wallet.service.WalletTxResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Consumes {@code WALLET_CREDIT} (async-api/core-commands.yaml §WalletCreditPayload) and
 * credits the resolved pocket wallet after accounting POSTED (ADR-024 Path B, ADR-026).
 *
 * <p>Gate: wallet must be ACTIVE. LOCKED → throws {@code WALLET_LOCKED}, message goes to DLQ
 * after retries; ops resolves the lock then retries manually (ADR-034).
 *
 * <p>Idempotent on {@code (walletId, businessRef, DEPOSIT_CREDIT)}: re-delivery after a
 * successful credit returns the existing {@code walletTxId} with no balance change (ADR-005).
 */
@Component
@ConditionalOnProperty(name = "wallet.amqp.enabled", havingValue = "true")
public class WalletCreditCommandListener {

    private static final Logger log = LoggerFactory.getLogger(WalletCreditCommandListener.class);

    private final WalletCommandService walletCommandService;

    public WalletCreditCommandListener(WalletCommandService walletCommandService) {
        this.walletCommandService = walletCommandService;
    }

    @RabbitListener(queues = WalletAmqpConfig.WALLET_CREDIT_QUEUE,
                    containerFactory = WalletAmqpConfig.LISTENER_FACTORY)
    public void onWalletCredit(Map<String, Object> envelope) {
        var ctx = CreditContext.from(envelope);

        WalletTxResult result = walletCommandService.creditByWalletId(
                ctx.walletId(),
                ctx.businessRef(),
                ctx.netAmount(),
                ctx.currency(),
                ctx.coaTransId(),
                "DEPOSIT");

        if (result.idempotentReplay()) {
            log.debug("WALLET_CREDIT replay businessRef={} walletId={} — existing walletTxId={}",
                    ctx.businessRef(), ctx.walletId(), result.walletTxId());
        } else {
            log.info("wallet credited businessRef={} walletId={} net={} availableAfter={} walletTxId={}",
                    ctx.businessRef(), ctx.walletId(), ctx.netAmount().toPlainString(),
                    result.available().toPlainString(), result.walletTxId());
        }
    }

    // -------------------------------------------------------------------------

    private record CreditContext(
            String     businessRef,
            long       walletId,
            BigDecimal netAmount,
            String     currency,
            Long       coaTransId) {

        @SuppressWarnings("unchecked")
        static CreditContext from(Map<String, Object> envelope) {
            String businessRef = (String) required(envelope, "businessRef");

            var payload     = (Map<String, Object>) required(envelope, "payload");
            long walletId   = ((Number) required(payload, "walletId")).longValue();
            String net      = String.valueOf(required(payload, "netAmount"));
            String currency = payload.containsKey("currency")
                    ? String.valueOf(payload.get("currency")) : "VND";
            Long coaTransId = payload.containsKey("coaTransId") && payload.get("coaTransId") != null
                    ? ((Number) payload.get("coaTransId")).longValue() : null;

            return new CreditContext(businessRef, walletId,
                    MoneyUtil.parseAmount(net), currency, coaTransId);
        }

        private static Object required(Map<String, Object> map, String key) {
            Object v = map.get(key);
            if (v == null) throw new IllegalArgumentException("WALLET_CREDIT missing field: " + key);
            return v;
        }
    }
}
