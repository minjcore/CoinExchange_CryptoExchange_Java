package com.gtelpay.core.wallet.service;

import com.gtelpay.core.wallet.domain.WalletType;

import java.math.BigDecimal;

public interface WalletCommandService {

    WalletView provisionIfAbsent(long memberId, WalletType walletType, String currency);

    WalletTxResult credit(WalletMutationCommand cmd);

    /**
     * Credit a wallet identified directly by {@code walletId} (pre-resolved pocket PK).
     * Used by the async deposit path where orchestration has already resolved VA → walletId.
     * Gate: wallet must be ACTIVE (LOCKED → throws WALLET_LOCKED).
     */
    WalletTxResult creditByWalletId(long walletId, String businessRef,
                                    BigDecimal netAmount, String currency,
                                    Long coaTransId, String useCase);

    WalletTxResult debit(WalletMutationCommand cmd);

    WalletTxResult freeze(WalletMutationCommand cmd);

    WalletTxResult unfreeze(WalletMutationCommand cmd);
}
