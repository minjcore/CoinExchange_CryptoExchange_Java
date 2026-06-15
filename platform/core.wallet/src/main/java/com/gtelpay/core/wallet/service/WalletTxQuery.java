package com.gtelpay.core.wallet.service;

import com.gtelpay.core.wallet.domain.WalletType;

public record WalletTxQuery(long memberId, WalletType walletType, String currency) {
}
