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
 * IBFT settle phase — called by payout worker on Napas SUCCESS (ADR-007).
 * TX 1: confirmIbft — Phase B lines (3400 DR / 1112 CR + 4130 CR, plus 5100 DR / 1112 CR Napas cost), post.
 * TX 2: IBFT_SETTLE — deducts gross from frozen.
 */
@Service
public class IbftSettleUseCase {

    private final WalletGateway wallet;
    private final LedgerGateway ledger;

    public IbftSettleUseCase(WalletGateway wallet, LedgerGateway ledger) {
        this.wallet = wallet;
        this.ledger = ledger;
    }

    public IbftSettleResult execute(long coaTransId, long memberId, String businessRef,
                                    String principal, String platformFee, String napasCost) {
        BigDecimal principalAmount = MoneyUtil.normalize(MoneyUtil.parseAmount(principal));
        BigDecimal feeAmount = (platformFee != null && !platformFee.isBlank())
                ? MoneyUtil.normalizeAllowZero(MoneyUtil.parseAmount(platformFee)) : BigDecimal.ZERO;
        BigDecimal napasCostAmount = (napasCost != null && !napasCost.isBlank())
                ? MoneyUtil.normalizeAllowZero(MoneyUtil.parseAmount(napasCost)) : BigDecimal.ZERO;
        BigDecimal gross = principalAmount.add(feeAmount);

        // TX 1: confirm journal (Phase B + Napas cost leg)
        var posted = ledger.confirmIbft(coaTransId, principalAmount, feeAmount, napasCostAmount);

        // TX 2: settle wallet — deduct gross from frozen
        WalletTxResult settle = wallet.debit(new WalletMutationCommand(
                memberId, WalletType.USER, "VND", gross,
                businessRef + ":settle", WalletTxType.IBFT_SETTLE,
                coaTransId, "IBFT", null));

        return new IbftSettleResult(
                businessRef, coaTransId, settle.walletTxId(),
                settle.frozen(), posted.idempotentReplay());
    }
}
