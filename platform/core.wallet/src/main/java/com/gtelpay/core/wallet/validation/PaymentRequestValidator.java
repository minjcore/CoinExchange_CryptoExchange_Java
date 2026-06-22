package com.gtelpay.core.wallet.validation;

import com.gtelpay.core.sharedlib.exception.ValidationException;
import com.gtelpay.core.sharedlib.util.MoneyUtil;
import com.gtelpay.core.wallet.api.dto.PaymentRequestWire;

import java.math.BigDecimal;

/**
 * Validates S1 {@code PaymentRequest} before wallet legs — {@code design/wallet/surface-map.md} §2.2.
 */
public final class PaymentRequestValidator {

    private PaymentRequestValidator() {
    }

    public record ValidatedPayment(
            String businessRef,
            long memberId,
            long merchantId,
            BigDecimal grossAmount,
            String currency) {
    }

    public static ValidatedPayment validate(PaymentRequestWire req, String idempotencyKey) {
        if (req == null) {
            throw new ValidationException("payment request required");
        }
        String businessRef = BusinessRefValidator.normalize(req.businessRef());
        S1IdempotencyValidator.requireHeaderMatchesBody(businessRef, idempotencyKey);
        if (req.memberId() <= 0) {
            throw new ValidationException("memberId must be positive");
        }
        if (req.merchantId() <= 0) {
            throw new ValidationException("merchantId must be positive");
        }
        if (req.memberId() == req.merchantId()) {
            throw new ValidationException("memberId and merchantId must differ");
        }
        String currency = CurrencyValidator.normalize(req.currency());
        BigDecimal gross = MoneyUtil.parseAmount(req.amount());
        return new ValidatedPayment(businessRef, req.memberId(), req.merchantId(), gross, currency);
    }
}
