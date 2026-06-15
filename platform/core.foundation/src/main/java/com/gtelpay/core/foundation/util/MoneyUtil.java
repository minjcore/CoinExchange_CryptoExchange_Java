package com.gtelpay.core.foundation.util;

import com.gtelpay.core.foundation.exception.ValidationException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Parse/normalize money amounts — scale 4, HALF_UP (ADR-028). No business rounding rules here beyond normalize.
 */
public final class MoneyUtil {

    public static final int MONEY_SCALE = 4;
    public static final RoundingMode MONEY_ROUNDING = RoundingMode.HALF_UP;

    private MoneyUtil() {
    }

    public static BigDecimal parseAmount(String raw) {
        if (raw == null) {
            throw new ValidationException("amount is required");
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            throw new ValidationException("amount must not be blank");
        }
        try {
            BigDecimal value = new BigDecimal(trimmed);
            if (value.signum() <= 0) {
                throw new ValidationException("amount must be positive");
            }
            return normalize(value);
        } catch (NumberFormatException ex) {
            throw new ValidationException("amount is not a valid decimal: " + raw);
        }
    }

    public static BigDecimal normalize(BigDecimal amount) {
        if (amount == null) {
            throw new ValidationException("amount is required");
        }
        if (amount.signum() <= 0) {
            throw new ValidationException("amount must be positive");
        }
        return amount.setScale(MONEY_SCALE, MONEY_ROUNDING);
    }

    /** Like {@link #normalize} but allows zero — for optional charges (e.g. deposit fee = 0). */
    public static BigDecimal normalizeAllowZero(BigDecimal amount) {
        if (amount == null) {
            throw new ValidationException("amount is required");
        }
        if (amount.signum() < 0) {
            throw new ValidationException("amount must be >= 0");
        }
        return amount.setScale(MONEY_SCALE, MONEY_ROUNDING);
    }
}
