package com.gtelpay.app.orchestration.usecase;

import com.gtelpay.app.orchestration.gateway.WalletGateway;
import com.gtelpay.core.wallet.api.OpenApiWalletMapper;
import com.gtelpay.core.wallet.api.dto.WalletBalanceDataWire;
import com.gtelpay.core.wallet.validation.WalletBalanceQueryValidator;
import org.springframework.stereotype.Service;

@Service
public class WalletBalanceUseCase {

    private final WalletGateway wallet;

    public WalletBalanceUseCase(WalletGateway wallet) {
        this.wallet = wallet;
    }

    public WalletBalanceDataWire execute(long memberId, String walletType, String currency) {
        var query = WalletBalanceQueryValidator.validate(walletType, currency);
        var view = wallet.getBalance(
                memberId,
                OpenApiWalletMapper.toDomain(query.walletType()),
                query.currency());
        return OpenApiWalletMapper.toWalletBalanceData(view);
    }
}
