package com.gtelpay.core.wallet.validation;

import com.gtelpay.core.foundation.exception.ValidationException;

/**
 * S1 idempotency wire rules — {@code design/platform/idempotency.md}.
 */
public final class S1IdempotencyValidator {

    private S1IdempotencyValidator() {
    }

    public static void requireHeaderMatchesBody(String businessRef, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new ValidationException("X-Idempotency-Key required");
        }
        String ref = BusinessRefValidator.normalize(businessRef);
        if (!idempotencyKey.trim().equals(ref)) {
            throw new ValidationException("X-Idempotency-Key must equal body businessRef");
        }
    }
}
