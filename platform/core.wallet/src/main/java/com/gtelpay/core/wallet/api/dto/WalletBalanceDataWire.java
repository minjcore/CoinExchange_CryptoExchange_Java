package com.gtelpay.core.wallet.api.dto;

/**
 * Wire: {@code components/schemas/WalletBalanceData}.
 */
public record WalletBalanceDataWire(
        long memberId,
        WalletTypeWire walletType,
        String currency,
        String available,
        String frozen) {
}
