package com.gtelpay.app.orchestration.vertx;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gtelpay.app.orchestration.deposit.DepositNotification;
import com.gtelpay.app.orchestration.deposit.DepositNotifyUseCase;
import com.gtelpay.app.orchestration.usecase.PaymentUseCase;
import com.gtelpay.app.orchestration.usecase.TransferUseCase;
import com.gtelpay.app.orchestration.usecase.WalletBalanceUseCase;
import com.gtelpay.app.orchestration.usecase.WithdrawUseCase;
import com.gtelpay.app.orchestration.usecase.WithdrawSettleUseCase;
import com.gtelpay.app.orchestration.usecase.WithdrawReleaseUseCase;
import com.gtelpay.app.orchestration.usecase.IbftUseCase;
import com.gtelpay.app.orchestration.usecase.IbftSettleUseCase;
import com.gtelpay.app.orchestration.usecase.IbftReleaseUseCase;
import com.gtelpay.core.wallet.api.dto.IbftRequestWire;
import com.gtelpay.core.wallet.api.dto.WithdrawalRequestWire;
import com.gtelpay.app.orchestration.web.ApiExceptionHandler;
import com.gtelpay.app.orchestration.web.MemberIdResolver;
import com.gtelpay.core.foundation.exception.ValidationException;
import com.gtelpay.core.foundation.response.ApiResponse;
import com.gtelpay.core.wallet.domain.WalletTxType;
import com.gtelpay.core.wallet.domain.WalletType;
import com.gtelpay.core.wallet.service.WalletCommandService;
import com.gtelpay.core.wallet.service.WalletMutationCommand;
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
        DepositNotifyUseCase depositNotifyUseCase = spring.getBean(DepositNotifyUseCase.class);
        WalletBalanceUseCase walletBalanceUseCase = spring.getBean(WalletBalanceUseCase.class);
        PaymentUseCase paymentUseCase = spring.getBean(PaymentUseCase.class);
        TransferUseCase transferUseCase = spring.getBean(TransferUseCase.class);
        WithdrawUseCase withdrawUseCase = spring.getBean(WithdrawUseCase.class);
        WithdrawSettleUseCase withdrawSettleUseCase = spring.getBean(WithdrawSettleUseCase.class);
        WithdrawReleaseUseCase withdrawReleaseUseCase = spring.getBean(WithdrawReleaseUseCase.class);
        IbftUseCase ibftUseCase = spring.getBean(IbftUseCase.class);
        IbftSettleUseCase ibftSettleUseCase = spring.getBean(IbftSettleUseCase.class);
        IbftReleaseUseCase ibftReleaseUseCase = spring.getBean(IbftReleaseUseCase.class);
        WalletCommandService walletCommandService = spring.getBean(WalletCommandService.class);
        ApiExceptionHandler errors = new ApiExceptionHandler(objectMapper);

        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());

        router.get("/health").handler(ctx -> ctx.response()
                .putHeader("Content-Type", "application/json")
                .end("{\"status\":\"UP\"}"));

        router.post("/v1/deposits/notify").handler(ctx -> {
            ctx.response().setStatusCode(202);
            blocking(ctx, errors, () -> {
                DepositNotification body = objectMapper.readValue(ctx.body().asString(), DepositNotification.class);
                return ApiResponse.ok(depositNotifyUseCase.execute(body));
            }, objectMapper);
        });

        router.get("/v1/wallets/balance").handler(ctx -> blocking(ctx, errors, () -> {
            long memberId = MemberIdResolver.requireMemberId(ctx);
            String walletType = ctx.queryParams().get("walletType");
            String currency = ctx.queryParams().get("currency");
            return ApiResponse.ok(walletBalanceUseCase.execute(memberId, walletType, currency));
        }, objectMapper));

        // bench-only: wallet balance read
        router.get("/v1/bench/wallet/balance").handler(ctx -> blocking(ctx, errors, () -> {
            long memberId = Long.parseLong(ctx.queryParams().get("memberId"));
            return ApiResponse.ok(walletBalanceUseCase.execute(memberId, "USER", "VND"));
        }, objectMapper));

        // bench-only: wallet debit+credit, no ledger
        router.post("/v1/bench/wallet").handler(ctx -> blocking(ctx, errors, () -> {
            var f = objectMapper.readTree(ctx.body().asString());
            long fromId = f.get("fromMemberId").asLong();
            long toId   = f.get("toMemberId").asLong();
            String ref  = f.get("businessRef").asText();
            String amt  = f.get("amount").asText();
            var debit = walletCommandService.debit(new WalletMutationCommand(
                    fromId, WalletType.USER, "VND", new java.math.BigDecimal(amt),
                    ref, WalletTxType.PAYMENT_DEBIT, null, "BENCH", null));
            var credit = walletCommandService.credit(new WalletMutationCommand(
                    toId, WalletType.USER, "VND", new java.math.BigDecimal(amt),
                    ref, WalletTxType.PAYMENT_CREDIT, null, "BENCH", null));
            return ApiResponse.ok(java.util.Map.of(
                    "debitTxId", debit.walletTxId(), "creditTxId", credit.walletTxId()));
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

        router.post("/v1/withdrawals").handler(ctx -> blocking(ctx, errors, () -> {
            WithdrawalRequestWire body = objectMapper.readValue(ctx.body().asString(), WithdrawalRequestWire.class);
            String idempotencyKey = ctx.request().getHeader("X-Idempotency-Key");
            return ApiResponse.ok(withdrawUseCase.execute(body, idempotencyKey));
        }, objectMapper));

        router.post("/v1/withdrawals/settle").handler(ctx -> blocking(ctx, errors, () -> {
            var f = objectMapper.readTree(ctx.body().asString());
            long coaTransId = f.get("coaTransId").asLong();
            long memberId = f.get("memberId").asLong();
            String businessRef = f.get("businessRef").asText();
            String principal = f.get("principal").asText();
            String fee = f.has("fee") ? f.get("fee").asText() : "0";
            return ApiResponse.ok(withdrawSettleUseCase.execute(coaTransId, memberId, businessRef, principal, fee));
        }, objectMapper));

        router.post("/v1/ibft").handler(ctx -> blocking(ctx, errors, () -> {
            IbftRequestWire body = objectMapper.readValue(ctx.body().asString(), IbftRequestWire.class);
            String idempotencyKey = ctx.request().getHeader("X-Idempotency-Key");
            return ApiResponse.ok(ibftUseCase.execute(body, idempotencyKey));
        }, objectMapper));

        router.post("/v1/ibft/settle").handler(ctx -> blocking(ctx, errors, () -> {
            var f = objectMapper.readTree(ctx.body().asString());
            long coaTransId = f.get("coaTransId").asLong();
            long memberId = f.get("memberId").asLong();
            String businessRef = f.get("businessRef").asText();
            String principal = f.get("principal").asText();
            String platformFee = f.has("platformFee") ? f.get("platformFee").asText() : "0";
            String napasCost = f.has("napasCost") ? f.get("napasCost").asText() : "0";
            return ApiResponse.ok(ibftSettleUseCase.execute(coaTransId, memberId, businessRef, principal, platformFee, napasCost));
        }, objectMapper));

        router.post("/v1/ibft/release").handler(ctx -> blocking(ctx, errors, () -> {
            var f = objectMapper.readTree(ctx.body().asString());
            long coaTransId = f.get("coaTransId").asLong();
            long memberId = f.get("memberId").asLong();
            String businessRef = f.get("businessRef").asText();
            String gross = f.get("gross").asText();
            return ApiResponse.ok(ibftReleaseUseCase.execute(coaTransId, memberId, businessRef, gross));
        }, objectMapper));

        router.post("/v1/withdrawals/release").handler(ctx -> blocking(ctx, errors, () -> {
            var f = objectMapper.readTree(ctx.body().asString());
            long coaTransId = f.get("coaTransId").asLong();
            long memberId = f.get("memberId").asLong();
            String businessRef = f.get("businessRef").asText();
            String gross = f.get("gross").asText();
            return ApiResponse.ok(withdrawReleaseUseCase.execute(coaTransId, memberId, businessRef, gross));
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
