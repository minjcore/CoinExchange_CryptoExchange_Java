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
 * IBFT release phase — called by payout worker on Napas terminal FAIL (ADR-007).
 * TX 1: voidIbft — marks journal FAILED.
 * TX 2: IBFT_RELEASE — returns gross to available.
 */
@Service
public class IbftReleaseUseCase {

    private final WalletGateway wallet;
    private final LedgerGateway ledger;

    public IbftReleaseUseCase(WalletGateway wallet, LedgerGateway ledger) {
        this.wallet = wallet;
        this.ledger = ledger;
    }

    public IbftReleaseResult execute(long coaTransId, long memberId, String businessRef, String gross) {
        BigDecimal grossAmount = MoneyUtil.normalize(MoneyUtil.parseAmount(gross));

        // TX 1: void journal — mark FAILED
        ledger.voidIbft(coaTransId);

        // TX 2: release wallet — return gross to available
        WalletTxResult release = wallet.unfreeze(new WalletMutationCommand(
                memberId, WalletType.USER, "VND", grossAmount,
                businessRef + ":release", WalletTxType.IBFT_RELEASE,
                coaTransId, "IBFT", null));

        return new IbftReleaseResult(
                businessRef, coaTransId, release.walletTxId(),
                release.available(), false);
    }
}
