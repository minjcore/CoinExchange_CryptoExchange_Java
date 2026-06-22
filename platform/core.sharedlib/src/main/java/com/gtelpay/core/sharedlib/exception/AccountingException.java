package com.gtelpay.core.sharedlib.exception;

public class AccountingException extends BaseException {

    public AccountingException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
