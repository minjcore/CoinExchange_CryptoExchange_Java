package com.gtelpay.core.wallet.validation;

import com.gtelpay.core.sharedlib.exception.ValidationException;
import com.gtelpay.core.wallet.api.dto.WalletTypeWire;

/**
 * Validates {@code GET /v1/wallets/balance} query params — {@code design/wallet/surface-map.md} §2.1.
 */
public final class WalletBalanceQueryValidator {

    private WalletBalanceQueryValidator() {
    }

    public record ValidatedBalanceQuery(WalletTypeWire walletType, String currency) {
    }

    public static ValidatedBalanceQuery validate(String walletType, String currency) {
        if (walletType == null || walletType.isBlank()) {
            throw new ValidationException("walletType required");
        }
        WalletTypeWire wireType;
        try {
            wireType = WalletTypeWire.valueOf(walletType.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ValidationException("invalid walletType: " + walletType);
        }
        return new ValidatedBalanceQuery(wireType, CurrencyValidator.normalize(currency));
    }
}
