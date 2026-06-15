package com.gtelpay.core.foundation.exception;

public class ValidationException extends BaseException {

    public ValidationException(String message) {
        super(ErrorCode.COMMON_INVALID_REQUEST, message);
    }
}
