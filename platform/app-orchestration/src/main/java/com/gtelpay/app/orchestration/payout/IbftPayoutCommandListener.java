package com.gtelpay.app.orchestration.payout;

import com.gtelpay.app.orchestration.usecase.IbftReleaseUseCase;
import com.gtelpay.app.orchestration.usecase.IbftSettleUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Consumes IBFT_PAYOUT (core-commands.yaml) — drives Napas dispatch then settle or release.
 * Idempotent: settle/release use cases are replay-safe on (businessRef, tx_type) unique constraint.
 */
@Component
@ConditionalOnProperty(name = "payout.amqp.enabled", havingValue = "true")
public class IbftPayoutCommandListener {

    private static final Logger log = LoggerFactory.getLogger(IbftPayoutCommandListener.class);

    private final BankStub bankStub;
    private final IbftSettleUseCase settleUseCase;
    private final IbftReleaseUseCase releaseUseCase;

    public IbftPayoutCommandListener(BankStub bankStub,
                                     IbftSettleUseCase settleUseCase,
                                     IbftReleaseUseCase releaseUseCase) {
        this.bankStub = bankStub;
        this.settleUseCase = settleUseCase;
        this.releaseUseCase = releaseUseCase;
    }

    @RabbitListener(queues = PayoutAmqpConfig.IBFT_PAYOUT_QUEUE)
    public void onIbftPayout(Map<String, Object> envelope) {
        var ctx = IbftPayoutContext.from(envelope);
        log.info("IBFT_PAYOUT received businessRef={} coaTransId={} gross={}",
                ctx.businessRef(), ctx.coaTransId(), ctx.grossAmount());

        BankStub.BankResult result = bankStub.dispatch(
                ctx.businessRef(), ctx.destinationBankAccountNumber(), ctx.destinationBankCode(), ctx.grossAmount());

        if (result == BankStub.BankResult.SUCCESS) {
            var settled = settleUseCase.execute(
                    ctx.coaTransId(), ctx.memberId(), ctx.businessRef(),
                    ctx.principalAmount(), ctx.platformFee(), ctx.napasCost());
            log.info("IBFT settled businessRef={} coaTransId={} walletTxId={} idempotent={}",
                    ctx.businessRef(), ctx.coaTransId(), settled.walletTxId(), settled.idempotent());
        } else {
            var released = releaseUseCase.execute(
                    ctx.coaTransId(), ctx.memberId(), ctx.businessRef(), ctx.grossAmount());
            log.info("IBFT released businessRef={} coaTransId={} walletTxId={}",
                    ctx.businessRef(), ctx.coaTransId(), released.walletTxId());
        }
    }

    @SuppressWarnings("unchecked")
    private record IbftPayoutContext(
            String businessRef, long memberId, long coaTransId,
            String principalAmount, String platformFee, String napasCost, String grossAmount,
            String destinationBankAccountNumber, String destinationBankCode) {

        static IbftPayoutContext from(Map<String, Object> env) {
            String businessRef = str(env, "businessRef");
            long memberId      = num(env, "memberId");
            var  payload       = (Map<String, Object>) required(env, "payload");
            long coaTransId    = num(payload, "coaTransId");
            String principal   = str(payload, "principalAmount");
            String fee         = strOr(payload, "platformFee", "0");
            String napasCost   = strOr(payload, "napasCost", "0");
            String gross       = str(payload, "grossAmount");
            String bankAcct    = strOr(payload, "destinationBankAccountNumber", "");
            String bankCode    = strOr(payload, "destinationBankCode", "");
            return new IbftPayoutContext(businessRef, memberId, coaTransId,
                    principal, fee, napasCost, gross, bankAcct, bankCode);
        }

        private static String str(Map<String, Object> m, String k) { return String.valueOf(required(m, k)); }
        private static String strOr(Map<String, Object> m, String k, String def) {
            return m.containsKey(k) && m.get(k) != null ? String.valueOf(m.get(k)) : def;
        }
        private static long num(Map<String, Object> m, String k) { return ((Number) required(m, k)).longValue(); }
        private static Object required(Map<String, Object> m, String k) {
            Object v = m.get(k); if (v == null) throw new IllegalArgumentException("IBFT_PAYOUT missing: " + k); return v;
        }
    }
}
