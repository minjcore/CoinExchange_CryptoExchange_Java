package com.gtelpay.app.orchestration.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gtelpay.core.foundation.exception.BaseException;
import com.gtelpay.core.foundation.exception.ErrorCode;
import com.gtelpay.core.foundation.response.ApiResponse;
import io.vertx.ext.web.RoutingContext;

public final class ApiExceptionHandler {

    private final ObjectMapper objectMapper;

    public ApiExceptionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void handle(RoutingContext ctx, Throwable err) {
        Throwable current = err;
        while (current != null) {
            if (current instanceof BaseException base) {
                write(ctx, base.errorCode().suggestedHttpStatus(), ApiResponse.fail(base.errorCode(), base.getMessage()));
                return;
            }
            current = current.getCause();
        }
        write(ctx, 500, ApiResponse.fail(ErrorCode.COMMON_RETRY_EXHAUSTED, err.getMessage() != null ? err.getMessage() : "internal error"));
    }

    private void write(RoutingContext ctx, int status, ApiResponse<?> body) {
        try {
            ctx.response()
                    .setStatusCode(status)
                    .putHeader("Content-Type", "application/json")
                    .end(objectMapper.writeValueAsString(body));
        } catch (Exception ex) {
            ctx.fail(ex);
        }
    }
}
