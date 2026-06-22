package com.gtelpay.core.sharedlib.util;

import com.gtelpay.core.sharedlib.exception.ValidationException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MoneyUtilTest {

    @Test
    void parseAmount_normalizesScaleFourHalfUp() {
        assertEquals(new BigDecimal("100000.0000"), MoneyUtil.parseAmount("100000"));
        assertEquals(new BigDecimal("100000.5000"), MoneyUtil.parseAmount("100000.5"));
        assertEquals(new BigDecimal("1.2346"), MoneyUtil.parseAmount("1.23456"));
    }

    @Test
    void parseAmount_rejectsNonPositive() {
        assertThrows(ValidationException.class, () -> MoneyUtil.parseAmount("0"));
        assertThrows(ValidationException.class, () -> MoneyUtil.parseAmount("-1"));
    }

    @Test
    void parseAmount_rejectsInvalid() {
        assertThrows(ValidationException.class, () -> MoneyUtil.parseAmount("abc"));
        assertThrows(ValidationException.class, () -> MoneyUtil.parseAmount("  "));
    }
}
