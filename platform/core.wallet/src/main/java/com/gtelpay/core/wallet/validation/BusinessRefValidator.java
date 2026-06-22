package com.gtelpay.core.wallet.validation;

import com.gtelpay.core.sharedlib.exception.ValidationException;

public final class BusinessRefValidator {

    public static final int MAX_LENGTH = 128;

    private BusinessRefValidator() {
    }

    public static String normalize(String businessRef) {
        if (businessRef == null) {
            throw new ValidationException("businessRef required");
        }
        String trimmed = businessRef.trim();
        if (trimmed.isEmpty() || trimmed.length() > MAX_LENGTH) {
            throw new ValidationException("businessRef length must be 1.." + MAX_LENGTH);
        }
        return trimmed;
    }
}
