package com.gtelpay.core.wallet.service;

import com.gtelpay.core.wallet.domain.WalletTxType;
import com.gtelpay.core.wallet.domain.WalletType;

import java.math.BigDecimal;

public record WalletMutationCommand(
        long memberId,
        WalletType walletType,
        String currency,
        BigDecimal amount,
        String businessRef,
        WalletTxType txType,
        Long coaTransId,
        String useCase,
        String remark) {

    /** Copy with {@code coaTransId} set — used to correlate a leg to its posted journal. */
    public WalletMutationCommand withCoaTransId(Long coaTransId) {
        return new WalletMutationCommand(
                memberId, walletType, currency, amount, businessRef, txType, coaTransId, useCase, remark);
    }
}
