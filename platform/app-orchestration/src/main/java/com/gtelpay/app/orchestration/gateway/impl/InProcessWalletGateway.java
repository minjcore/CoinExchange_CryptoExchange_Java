package com.gtelpay.app.orchestration.gateway.impl;

import com.gtelpay.app.orchestration.gateway.WalletGateway;
import com.gtelpay.core.wallet.domain.WalletType;
import com.gtelpay.core.wallet.service.BalanceView;
import com.gtelpay.core.wallet.service.WalletCommandService;
import com.gtelpay.core.wallet.service.WalletMutationCommand;
import com.gtelpay.core.wallet.service.WalletQueryService;
import com.gtelpay.core.wallet.service.WalletTxResult;
import com.gtelpay.core.wallet.service.WalletView;
import org.springframework.stereotype.Component;

/**
 * v1 in-process wallet gateway — domains run in the same JVM as the orchestrator
 * (ADR-003). Swap for an HTTP client to split into a separate process.
 */
@Component
public class InProcessWalletGateway implements WalletGateway {

    private final WalletCommandService commandService;
    private final WalletQueryService queryService;

    public InProcessWalletGateway(WalletCommandService commandService, WalletQueryService queryService) {
        this.commandService = commandService;
        this.queryService = queryService;
    }

    @Override
    public WalletView provisionIfAbsent(long memberId, WalletType walletType, String currency) {
        return commandService.provisionIfAbsent(memberId, walletType, currency);
    }

    @Override
    public WalletTxResult credit(WalletMutationCommand cmd) {
        return commandService.credit(cmd);
    }

    @Override
    public WalletTxResult debit(WalletMutationCommand cmd) {
        return commandService.debit(cmd);
    }

    @Override
    public WalletTxResult freeze(WalletMutationCommand cmd) {
        return commandService.freeze(cmd);
    }

    @Override
    public WalletTxResult unfreeze(WalletMutationCommand cmd) {
        return commandService.unfreeze(cmd);
    }

    @Override
    public BalanceView getBalance(long memberId, WalletType walletType, String currency) {
        return queryService.getBalance(memberId, walletType, currency);
    }
}
