package com.gtelpay.core.wallet.service;

import com.gtelpay.core.wallet.domain.WalletType;

public interface WalletCommandService {

    WalletView provisionIfAbsent(long memberId, WalletType walletType, String currency);

    WalletTxResult credit(WalletMutationCommand cmd);

    WalletTxResult debit(WalletMutationCommand cmd);

    WalletTxResult freeze(WalletMutationCommand cmd);

    WalletTxResult unfreeze(WalletMutationCommand cmd);
}
