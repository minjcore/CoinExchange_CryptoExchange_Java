package com.gtelpay.core.wallet.validation;

import com.gtelpay.core.foundation.exception.ValidationException;
import com.gtelpay.core.wallet.api.dto.WalletTypeWire;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WalletBalanceQueryValidatorTest {

    @Test
    void validate_normalizesCurrency() {
        var query = WalletBalanceQueryValidator.validate("user", "vnd");

        assertEquals(WalletTypeWire.USER, query.walletType());
        assertEquals("VND", query.currency());
    }

    @Test
    void validate_rejectsMissingWalletType() {
        assertThrows(ValidationException.class, () -> WalletBalanceQueryValidator.validate(" ", "VND"));
    }
}
