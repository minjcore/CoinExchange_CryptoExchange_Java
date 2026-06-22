package com.gtelpay.core.wallet.validation;

import com.gtelpay.core.sharedlib.exception.ValidationException;

public final class CurrencyValidator {

    private CurrencyValidator() {
    }

    public static String normalize(String currency) {
        if (currency == null || currency.isBlank()) {
            throw new ValidationException("currency required");
        }
        String normalized = currency.trim().toUpperCase();
        if (normalized.length() != 3) {
            throw new ValidationException("currency must be ISO-4217 3-letter code");
        }
        return normalized;
    }
}
