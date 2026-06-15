package com.gtelpay.core.foundation.exception;

public class WalletException extends BaseException {

    public WalletException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
