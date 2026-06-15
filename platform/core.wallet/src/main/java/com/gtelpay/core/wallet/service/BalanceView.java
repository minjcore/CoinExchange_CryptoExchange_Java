package com.gtelpay.core.wallet.service;

import com.gtelpay.core.wallet.domain.WalletStatus;
import com.gtelpay.core.wallet.domain.WalletType;

import java.math.BigDecimal;

public record BalanceView(
        long memberId,
        WalletType walletType,
        String currency,
        WalletStatus status,
        BigDecimal available,
        BigDecimal frozen) {
}
