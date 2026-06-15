package com.gtelpay.core.foundation.response;

import com.gtelpay.core.foundation.exception.ErrorCode;

import java.time.Instant;
import java.util.Objects;

/**
 * Aligns with OpenAPI {@code ApiEnvelope}: {@code code}, {@code message}, optional {@code timestamp}, {@code data}.
 */
public final class ApiResponse<T> {

    private final int code;
    private final String message;
    private final T data;
    private final Instant timestamp;

    private ApiResponse(int code, String message, T data, Instant timestamp) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = timestamp;
    }

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(0, "OK", data, Instant.now());
    }

    public static <T> ApiResponse<T> ok(T data, Instant timestamp) {
        return new ApiResponse<>(0, "OK", data, timestamp);
    }

    public static <T> ApiResponse<T> fail(ErrorCode errorCode, String message) {
        Objects.requireNonNull(errorCode, "errorCode");
        return new ApiResponse<>(errorCode.apiCode(), message, null, Instant.now());
    }

    public int code() {
        return code;
    }

    public String message() {
        return message;
    }

    public T data() {
        return data;
    }

    public Instant timestamp() {
        return timestamp;
    }

    public boolean success() {
        return code == 0;
    }

    /** Jackson / JSON binding (OpenAPI envelope). */
    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ApiResponse<?> that)) {
            return false;
        }
        return code == that.code
                && Objects.equals(message, that.message)
                && Objects.equals(data, that.data)
                && Objects.equals(timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, message, data, timestamp);
    }
}
