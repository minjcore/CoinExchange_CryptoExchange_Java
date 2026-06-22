package com.gtelpay.core.sharedlib.exception;

public class WalletException extends BaseException {

    public WalletException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
