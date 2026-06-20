package com.gtelpay.app.orchestration.payout;

import com.gtelpay.app.orchestration.usecase.WithdrawReleaseUseCase;
import com.gtelpay.app.orchestration.usecase.WithdrawSettleUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Consumes WITHDRAW_PAYOUT (core-commands.yaml) — drives bank dispatch then settle or release.
 * Idempotent: settle/release use cases are replay-safe on (businessRef, tx_type) unique constraint.
 */
@Component
@ConditionalOnProperty(name = "payout.amqp.enabled", havingValue = "true")
public class WithdrawPayoutCommandListener {

    private static final Logger log = LoggerFactory.getLogger(WithdrawPayoutCommandListener.class);

    private final BankStub bankStub;
    private final WithdrawSettleUseCase settleUseCase;
    private final WithdrawReleaseUseCase releaseUseCase;

    public WithdrawPayoutCommandListener(BankStub bankStub,
                                         WithdrawSettleUseCase settleUseCase,
                                         WithdrawReleaseUseCase releaseUseCase) {
        this.bankStub = bankStub;
        this.settleUseCase = settleUseCase;
        this.releaseUseCase = releaseUseCase;
    }

    @RabbitListener(queues = PayoutAmqpConfig.WITHDRAW_PAYOUT_QUEUE)
    public void onWithdrawPayout(Map<String, Object> envelope) {
        var ctx = WithdrawPayoutContext.from(envelope);
        log.info("WITHDRAW_PAYOUT received businessRef={} coaTransId={} gross={}",
                ctx.businessRef(), ctx.coaTransId(), ctx.grossAmount());

        BankStub.BankResult result = bankStub.dispatch(
                ctx.businessRef(), ctx.bankAccountNumber(), ctx.bankCode(), ctx.grossAmount());

        if (result == BankStub.BankResult.SUCCESS) {
            var settled = settleUseCase.execute(
                    ctx.coaTransId(), ctx.memberId(), ctx.businessRef(),
                    ctx.principalAmount(), ctx.fee());
            log.info("WITHDRAW settled businessRef={} coaTransId={} walletTxId={} idempotent={}",
                    ctx.businessRef(), ctx.coaTransId(), settled.walletTxId(), settled.idempotent());
        } else {
            var released = releaseUseCase.execute(
                    ctx.coaTransId(), ctx.memberId(), ctx.businessRef(), ctx.grossAmount());
            log.info("WITHDRAW released businessRef={} coaTransId={} walletTxId={}",
                    ctx.businessRef(), ctx.coaTransId(), released.walletTxId());
        }
    }

    @SuppressWarnings("unchecked")
    private record WithdrawPayoutContext(
            String businessRef, long memberId, long coaTransId,
            String principalAmount, String fee, String grossAmount,
            String bankAccountNumber, String bankCode) {

        static WithdrawPayoutContext from(Map<String, Object> env) {
            String businessRef = str(env, "businessRef");
            long memberId      = num(env, "memberId");
            var  payload       = (Map<String, Object>) required(env, "payload");
            long coaTransId    = num(payload, "coaTransId");
            String principal   = str(payload, "principalAmount");
            String fee         = payload.containsKey("fee") ? String.valueOf(payload.get("fee")) : "0";
            String gross       = str(payload, "grossAmount");
            String bankAcct    = strOr(payload, "bankAccountNumber", "");
            String bankCode    = strOr(payload, "bankCode", "");
            return new WithdrawPayoutContext(businessRef, memberId, coaTransId,
                    principal, fee, gross, bankAcct, bankCode);
        }

        private static String str(Map<String, Object> m, String k) { return String.valueOf(required(m, k)); }
        private static String strOr(Map<String, Object> m, String k, String def) {
            return m.containsKey(k) && m.get(k) != null ? String.valueOf(m.get(k)) : def;
        }
        private static long num(Map<String, Object> m, String k) { return ((Number) required(m, k)).longValue(); }
        private static Object required(Map<String, Object> m, String k) {
            Object v = m.get(k); if (v == null) throw new IllegalArgumentException("WITHDRAW_PAYOUT missing: " + k); return v;
        }
    }
}
