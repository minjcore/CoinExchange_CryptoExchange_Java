package com.gtelpay.core.foundation.exception;

public class AccountingException extends BaseException {

    public AccountingException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
