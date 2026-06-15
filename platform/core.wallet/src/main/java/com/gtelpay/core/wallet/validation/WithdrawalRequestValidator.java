package com.gtelpay.core.wallet.validation;

import com.gtelpay.core.foundation.exception.ValidationException;
import com.gtelpay.core.foundation.util.MoneyUtil;
import com.gtelpay.core.wallet.api.dto.WithdrawalRequestWire;

import java.math.BigDecimal;

/**
 * Validates S1 {@code WithdrawalRequest} before freeze leg — ADR-007.
 */
public final class WithdrawalRequestValidator {

    private WithdrawalRequestValidator() {
    }

    public record ValidatedWithdrawal(
            String businessRef,
            long memberId,
            BigDecimal amount,
            String currency) {
    }

    public static ValidatedWithdrawal validate(WithdrawalRequestWire req, String idempotencyKey) {
        if (req == null) {
            throw new ValidationException("withdrawal request required");
        }
        if (!req.useFreeze()) {
            throw new ValidationException("v1 withdraw requires useFreeze=true (freeze→settle pipeline)");
        }
        String businessRef = BusinessRefValidator.normalize(req.businessRef());
        S1IdempotencyValidator.requireHeaderMatchesBody(businessRef, idempotencyKey);
        if (req.memberId() <= 0) {
            throw new ValidationException("memberId must be positive");
        }
        String currency = CurrencyValidator.normalize(req.currency());
        BigDecimal amount = MoneyUtil.parseAmount(req.amount());
        return new ValidatedWithdrawal(businessRef, req.memberId(), amount, currency);
    }
}
