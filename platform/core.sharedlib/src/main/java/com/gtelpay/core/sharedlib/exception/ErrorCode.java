package com.gtelpay.core.sharedlib.exception;

/**
 * Stable error identifiers — names align with async-api/core-events.yaml {@code ErrorCode} enum.
 * {@link #apiCode()} is the numeric {@code ApiResponse.code} (0 reserved for success).
 */
public enum ErrorCode {

    COMMON_INVALID_REQUEST(1001, 400),
    COMMON_NOT_FOUND(1002, 404),
    COMMON_CONFLICT(1003, 409),
    COMMON_RETRY_EXHAUSTED(1004, 503),

    WALLET_INSUFFICIENT_BALANCE(2001, 422),
    WALLET_NOT_FOUND(2002, 404),
    WALLET_LOCKED(2003, 422),
    WALLET_DUPLICATE_CONFLICT(2004, 409),

    ACCOUNTING_UNBALANCED_JOURNAL(3001, 422),
    ACCOUNTING_JOURNAL_NOT_FOUND(3002, 404),
    ACCOUNTING_DUPLICATE_JOURNAL(3003, 409),
    ACCOUNTING_PERIOD_CLOSED(3004, 422),

    PAYOUT_BANK_REJECTED(4001, 422),
    PAYOUT_TIMEOUT(4002, 504);

    private final int apiCode;
    private final int suggestedHttpStatus;

    ErrorCode(int apiCode, int suggestedHttpStatus) {
        this.apiCode = apiCode;
        this.suggestedHttpStatus = suggestedHttpStatus;
    }

    public int apiCode() {
        return apiCode;
    }

    public int suggestedHttpStatus() {
        return suggestedHttpStatus;
    }

    /** Wire name for Kafka / AsyncAPI (enum constant name). */
    public String wireName() {
        return name();
    }
}
