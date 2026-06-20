package com.gtelpay.app.orchestration.usecase;

import com.gtelpay.app.orchestration.gateway.LedgerGateway;
import com.gtelpay.app.orchestration.gateway.WalletGateway;
import com.gtelpay.core.foundation.util.MoneyUtil;
import com.gtelpay.core.wallet.domain.WalletTxType;
import com.gtelpay.core.wallet.domain.WalletType;
import com.gtelpay.core.wallet.service.WalletMutationCommand;
import com.gtelpay.core.wallet.service.WalletTxResult;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Withdraw release phase — called by payout worker on bank terminal FAIL (ADR-007).
 * TX 1: voidWithdraw — marks journal FAILED (no Phase B lines posted).
 * TX 2: WITHDRAW_RELEASE — returns gross to available (frozen -= gross, available += gross).
 */
@Service
public class WithdrawReleaseUseCase {

    private final WalletGateway wallet;
    private final LedgerGateway ledger;

    public WithdrawReleaseUseCase(WalletGateway wallet, LedgerGateway ledger) {
        this.wallet = wallet;
        this.ledger = ledger;
    }

    public WithdrawReleaseResult execute(long coaTransId, long memberId, String businessRef, String gross) {
        BigDecimal grossAmount = MoneyUtil.normalize(MoneyUtil.parseAmount(gross));

        // TX 1: void journal — mark FAILED
        ledger.voidWithdraw(coaTransId);

        // TX 2: release wallet — return gross to available
        WalletTxResult release = wallet.unfreeze(new WalletMutationCommand(
                memberId, WalletType.USER, "VND", grossAmount,
                businessRef + ":release", WalletTxType.WITHDRAW_RELEASE,
                coaTransId, "WITHDRAW", null));

        return new WithdrawReleaseResult(
                businessRef, coaTransId, release.walletTxId(),
                release.available(), false);
    }
}
