package com.gtelpay.core.wallet.validation;

import com.gtelpay.core.sharedlib.exception.ValidationException;
import com.gtelpay.core.wallet.api.dto.PaymentRequestWire;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PaymentRequestValidatorTest {

    @Test
    void validate_acceptsMatchingIdempotencyKey() {
        PaymentRequestWire req = new PaymentRequestWire(
                "pay-1", 1L, 2L, "1000.0000", "VND", null);

        var validated = PaymentRequestValidator.validate(req, "pay-1");

        assertEquals("pay-1", validated.businessRef());
        assertEquals(1L, validated.memberId());
        assertEquals(2L, validated.merchantId());
    }

    @Test
    void validate_rejectsMissingIdempotencyKey() {
        PaymentRequestWire req = new PaymentRequestWire(
                "pay-1", 1L, 2L, "1000.0000", "VND", null);

        assertThrows(ValidationException.class, () -> PaymentRequestValidator.validate(req, null));
    }

    @Test
    void validate_rejectsSameMemberAndMerchant() {
        PaymentRequestWire req = new PaymentRequestWire(
                "pay-1", 5L, 5L, "1000.0000", "VND", null);

        assertThrows(ValidationException.class, () -> PaymentRequestValidator.validate(req, "pay-1"));
    }
}
