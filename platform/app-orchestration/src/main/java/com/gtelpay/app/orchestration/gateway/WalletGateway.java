package com.gtelpay.app.orchestration.gateway;

import com.gtelpay.core.wallet.domain.WalletType;
import com.gtelpay.core.wallet.service.BalanceView;
import com.gtelpay.core.wallet.service.WalletMutationCommand;
import com.gtelpay.core.wallet.service.WalletTxResult;
import com.gtelpay.core.wallet.service.WalletView;

/**
 * Orchestrator's seam to the wallet domain. In-process today
 * ({@code InProcessWalletGateway}); an HTTP client is a drop-in replacement
 * since the orchestrator depends only on this interface, not the domain service.
 */
public interface WalletGateway {

    WalletView provisionIfAbsent(long memberId, WalletType walletType, String currency);

    WalletTxResult credit(WalletMutationCommand cmd);

    WalletTxResult debit(WalletMutationCommand cmd);

    WalletTxResult freeze(WalletMutationCommand cmd);

    WalletTxResult unfreeze(WalletMutationCommand cmd);

    BalanceView getBalance(long memberId, WalletType walletType, String currency);
}
