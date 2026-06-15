package com.gtelpay.app.orchestration.vertx;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gtelpay.app.orchestration.usecase.PaymentUseCase;
import com.gtelpay.app.orchestration.usecase.TransferUseCase;
import com.gtelpay.app.orchestration.usecase.WalletBalanceUseCase;
import com.gtelpay.app.orchestration.web.ApiExceptionHandler;
import com.gtelpay.app.orchestration.web.MemberIdResolver;
import com.gtelpay.core.foundation.exception.ValidationException;
import com.gtelpay.core.foundation.response.ApiResponse;
import com.gtelpay.core.wallet.api.dto.PaymentRequestWire;
import com.gtelpay.core.wallet.api.dto.TransferRequestWire;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import org.springframework.context.ConfigurableApplicationContext;

public class HttpServerVerticle extends AbstractVerticle {

    private final ConfigurableApplicationContext spring;
    private final int port;

    public HttpServerVerticle(ConfigurableApplicationContext spring, int port) {
        this.spring = spring;
        this.port = port;
    }

    @Override
    public void start(Promise<Void> startPromise) {
        ObjectMapper objectMapper = spring.getBean(ObjectMapper.class);
        WalletBalanceUseCase walletBalanceUseCase = spring.getBean(WalletBalanceUseCase.class);
        PaymentUseCase paymentUseCase = spring.getBean(PaymentUseCase.class);
        TransferUseCase transferUseCase = spring.getBean(TransferUseCase.class);
        ApiExceptionHandler errors = new ApiExceptionHandler(objectMapper);

        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());

        router.get("/health").handler(ctx -> ctx.response()
                .putHeader("Content-Type", "application/json")
                .end("{\"status\":\"UP\"}"));

        router.get("/v1/wallets/balance").handler(ctx -> blocking(ctx, errors, () -> {
            long memberId = MemberIdResolver.requireMemberId(ctx);
            String walletType = ctx.queryParams().get("walletType");
            String currency = ctx.queryParams().get("currency");
            return ApiResponse.ok(walletBalanceUseCase.execute(memberId, walletType, currency));
        }, objectMapper));

        router.post("/v1/payments").handler(ctx -> blocking(ctx, errors, () -> {
            PaymentRequestWire body = readPayment(ctx, objectMapper);
            String idempotencyKey = ctx.request().getHeader("X-Idempotency-Key");
            return ApiResponse.ok(paymentUseCase.execute(body, idempotencyKey));
        }, objectMapper));

        router.post("/v1/transfers").handler(ctx -> blocking(ctx, errors, () -> {
            TransferRequestWire body = objectMapper.readValue(ctx.body().asString(), TransferRequestWire.class);
            String idempotencyKey = ctx.request().getHeader("X-Idempotency-Key");
            return ApiResponse.ok(transferUseCase.execute(body, idempotencyKey));
        }, objectMapper));

        vertx.createHttpServer()
                .requestHandler(router)
                .listen(port)
                .onSuccess(server -> {
                    System.out.println("Vert.x S1 listening on http://localhost:" + port);
                    startPromise.complete();
                })
                .onFailure(startPromise::fail);
    }

    /** Accepts both {@code application/json} and {@code application/x-www-form-urlencoded}. */
    private static PaymentRequestWire readPayment(RoutingContext ctx, ObjectMapper mapper) throws Exception {
        String contentType = ctx.request().getHeader("Content-Type");
        if (contentType != null && contentType.contains("application/x-www-form-urlencoded")) {
            var f = ctx.request().formAttributes();
            return new PaymentRequestWire(
                    f.get("businessRef"),
                    parseLong(f.get("memberId"), "memberId"),
                    parseLong(f.get("merchantId"), "merchantId"),
                    f.get("amount"),
                    f.get("currency"),
                    f.get("netToMerchant"));
        }
        return mapper.readValue(ctx.body().asString(), PaymentRequestWire.class);
    }

    private static long parseLong(String value, String field) {
        try {
            return Long.parseLong(value == null ? "" : value.trim());
        } catch (NumberFormatException e) {
            throw new ValidationException(field + " must be a number");
        }
    }

    @FunctionalInterface
    private interface BlockingTask {
        Object run() throws Exception;
    }

    private static void blocking(
            RoutingContext ctx,
            ApiExceptionHandler errors,
            BlockingTask task,
            ObjectMapper objectMapper) {
        ctx.vertx().executeBlocking(() -> {
            try {
                return task.run();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }).onSuccess(result -> {
            try {
                ctx.response()
                        .putHeader("Content-Type", "application/json")
                        .end(objectMapper.writeValueAsString(result));
            } catch (Exception ex) {
                errors.handle(ctx, ex);
            }
        }).onFailure(err -> {
            Throwable cause = err.getCause() != null ? err.getCause() : err;
            errors.handle(ctx, cause);
        });
    }
}
