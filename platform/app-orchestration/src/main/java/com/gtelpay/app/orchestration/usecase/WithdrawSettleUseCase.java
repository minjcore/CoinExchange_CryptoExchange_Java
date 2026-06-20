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
 * Withdraw settle phase — called by payout worker on bank SUCCESS (ADR-007).
 * TX 1: confirmWithdraw — adds Phase B journal lines (3200 DR / 1111 CR + 4120 CR), posts.
 * TX 2: WITHDRAW_SETTLE — deducts gross from frozen (frozen -= gross).
 */
@Service
public class WithdrawSettleUseCase {

    private final WalletGateway wallet;
    private final LedgerGateway ledger;

    public WithdrawSettleUseCase(WalletGateway wallet, LedgerGateway ledger) {
        this.wallet = wallet;
        this.ledger = ledger;
    }

    public WithdrawSettleResult execute(long coaTransId, long memberId, String businessRef,
                                        String principal, String fee) {
        BigDecimal principalAmount = MoneyUtil.normalize(MoneyUtil.parseAmount(principal));
        BigDecimal feeAmount = (fee != null && !fee.isBlank())
                ? MoneyUtil.normalizeAllowZero(MoneyUtil.parseAmount(fee))
                : BigDecimal.ZERO;
        BigDecimal gross = principalAmount.add(feeAmount);

        // TX 1: confirm journal (Phase B lines + post)
        var posted = ledger.confirmWithdraw(coaTransId, principalAmount, feeAmount);

        // TX 2: settle wallet — deduct gross from frozen
        WalletTxResult settle = wallet.debit(new WalletMutationCommand(
                memberId, WalletType.USER, "VND", gross,
                businessRef + ":settle", WalletTxType.WITHDRAW_SETTLE,
                coaTransId, "WITHDRAW", null));

        return new WithdrawSettleResult(
                businessRef, coaTransId, settle.walletTxId(),
                settle.frozen(), posted.idempotentReplay());
    }
}
