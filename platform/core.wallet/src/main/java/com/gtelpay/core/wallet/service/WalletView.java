package com.gtelpay.core.wallet.service;

import com.gtelpay.core.wallet.domain.WalletStatus;
import com.gtelpay.core.wallet.domain.WalletType;

public record WalletView(
        long walletId,
        long memberId,
        WalletType walletType,
        String currency,
        WalletStatus status) {
}
