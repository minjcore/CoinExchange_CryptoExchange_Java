package com.gtelpay.core.accounting.validation;

import com.gtelpay.core.accounting.domain.LineSide;
import com.gtelpay.core.foundation.exception.ValidationException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PaymentPostingValidatorTest {

    @Test
    void buildV1Lines_whenNetEqualsGross_returnsFourLines() {
        var lines = PaymentPostingValidator.buildV1Lines("100000.0000", null, "VND");

        assertEquals(4, lines.size());
        assertEquals("2110", lines.get(0).accountCode());
        assertEquals(LineSide.DEBIT, lines.get(0).side());
        assertEquals(new BigDecimal("100000.0000"), lines.get(0).amount());
    }

    @Test
    void buildV1Lines_rejectsFeeSplit() {
        assertThrows(
                ValidationException.class,
                () -> PaymentPostingValidator.buildV1Lines("100000.0000", "99000.0000", "VND"));
    }
}
