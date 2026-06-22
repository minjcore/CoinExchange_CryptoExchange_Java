package com.gtelpay.core.wallet.validation;

import com.gtelpay.core.sharedlib.exception.ValidationException;
import com.gtelpay.core.sharedlib.util.MoneyUtil;
import com.gtelpay.core.wallet.api.dto.IbftRequestWire;

import java.math.BigDecimal;

/**
 * Validates S1 {@code IbftRequest} before freeze leg — ADR-007.
 */
public final class IbftRequestValidator {

    private IbftRequestValidator() {
    }

    public record ValidatedIbft(
            String businessRef,
            long memberId,
            BigDecimal principal,
            BigDecimal platformFee,
            BigDecimal napasCost,
            BigDecimal gross,
            String currency,
            String destinationBankAccountNumber,
            String destinationBankCode) {
    }

    public static ValidatedIbft validate(IbftRequestWire req, String idempotencyKey) {
        if (req == null) {
            throw new ValidationException("IBFT request required");
        }
        String businessRef = BusinessRefValidator.normalize(req.businessRef());
        S1IdempotencyValidator.requireHeaderMatchesBody(businessRef, idempotencyKey);
        if (req.memberId() <= 0) {
            throw new ValidationException("memberId must be positive");
        }
        String currency = CurrencyValidator.normalize(req.currency());
        BigDecimal principal = MoneyUtil.normalize(MoneyUtil.parseAmount(req.principalAmount()));
        BigDecimal platformFee = (req.platformFee() != null && !req.platformFee().isBlank())
                ? MoneyUtil.normalizeAllowZero(MoneyUtil.parseAmount(req.platformFee()))
                : BigDecimal.ZERO;
        BigDecimal napasCost = (req.napasCost() != null && !req.napasCost().isBlank())
                ? MoneyUtil.normalizeAllowZero(MoneyUtil.parseAmount(req.napasCost()))
                : BigDecimal.ZERO;
        BigDecimal gross = principal.add(platformFee);
        if (gross.signum() <= 0) {
            throw new ValidationException("IBFT gross (principal + platformFee) must be positive");
        }
        if (req.destinationBankAccountNumber() == null || req.destinationBankAccountNumber().isBlank()) {
            throw new ValidationException("destinationBankAccountNumber required");
        }
        if (req.destinationBankCode() == null || req.destinationBankCode().isBlank()) {
            throw new ValidationException("destinationBankCode required");
        }
        return new ValidatedIbft(businessRef, req.memberId(), principal, platformFee,
                napasCost, gross, currency,
                req.destinationBankAccountNumber().trim(), req.destinationBankCode().trim());
    }
}
