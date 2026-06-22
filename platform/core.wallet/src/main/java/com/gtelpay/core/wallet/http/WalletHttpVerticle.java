package com.gtelpay.core.wallet.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gtelpay.core.sharedlib.exception.BaseException;
import com.gtelpay.core.wallet.domain.WalletType;
import com.gtelpay.core.wallet.service.WalletCommandService;
import com.gtelpay.core.wallet.service.WalletMutationCommand;
import com.gtelpay.core.wallet.service.WalletQueryService;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import org.springframework.context.ConfigurableApplicationContext;

import java.time.Instant;

/**
 * Wallet-internal HTTP surface ({@code spec/contracts/open-api/wallet-internal.yaml}, ADR-038).
 * Thin adapter over the wallet domain services; the orchestrator's {@code WalletGateway}
 * calls this instead of the domain classes once split out of the JVM.
 */
public class WalletHttpVerticle extends AbstractVerticle {

    private final ConfigurableApplicationContext spring;
    private final int port;

    public WalletHttpVerticle(ConfigurableApplicationContext spring, int port) {
        this.spring = spring;
        this.port = port;
    }

    @Override
    public void start(Promise<Void> startPromise) {
        ObjectMapper mapper = spring.getBean(ObjectMapper.class);
        WalletCommandService command = spring.getBean(WalletCommandService.class);
        WalletQueryService query = spring.getBean(WalletQueryService.class);

        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());

        router.get("/health").handler(ctx -> ctx.response()
                .putHeader("Content-Type", "application/json").end("{\"status\":\"UP\"}"));

        router.post("/v1/wallets/provision").handler(ctx -> blocking(ctx, mapper, () -> {
            ProvisionRequest r = mapper.readValue(ctx.body().asString(), ProvisionRequest.class);
            return command.provisionIfAbsent(r.memberId(), r.walletType(), r.currency());
        }));

        router.post("/v1/wallets/credit").handler(ctx -> mutation(ctx, mapper, command::credit));
        router.post("/v1/wallets/debit").handler(ctx -> mutation(ctx, mapper, command::debit));
        router.post("/v1/wallets/freeze").handler(ctx -> mutation(ctx, mapper, command::freeze));
        router.post("/v1/wallets/unfreeze").handler(ctx -> mutation(ctx, mapper, command::unfreeze));

        router.get("/v1/wallets/balance").handler(ctx -> blocking(ctx, mapper, () -> {
            long memberId = Long.parseLong(ctx.queryParams().get("memberId"));
            WalletType walletType = WalletType.valueOf(ctx.queryParams().get("walletType"));
            String currency = ctx.queryParams().get("currency");
            return query.getBalance(memberId, walletType, currency);
        }));

        vertx.createHttpServer().requestHandler(router).listen(port)
                .onSuccess(s -> {
                    System.out.println("Wallet-internal HTTP listening on http://localhost:" + port);
                    startPromise.complete();
                })
                .onFailure(startPromise::fail);
    }

    private void mutation(RoutingContext ctx, ObjectMapper mapper,
                          java.util.function.Function<WalletMutationCommand, Object> op) {
        blocking(ctx, mapper, () -> op.apply(
                mapper.readValue(ctx.body().asString(), WalletMutationCommand.class)));
    }

    @FunctionalInterface
    private interface Task {
        Object run() throws Exception;
    }

    private static void blocking(RoutingContext ctx, ObjectMapper mapper, Task task) {
        ctx.vertx().executeBlocking(() -> {
            try {
                return task.run();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).onSuccess(result -> writeJson(ctx, mapper, 200, result))
          .onFailure(err -> writeError(ctx, mapper, err.getCause() != null ? err.getCause() : err));
    }

    private static void writeError(RoutingContext ctx, ObjectMapper mapper, Throwable err) {
        Throwable c = err;
        while (c != null) {
            if (c instanceof BaseException base) {
                writeJson(ctx, mapper, base.errorCode().suggestedHttpStatus(),
                        new ErrorBody(base.errorCode().apiCode(), base.getMessage(), Instant.now()));
                return;
            }
            c = c.getCause();
        }
        writeJson(ctx, mapper, 500, new ErrorBody(5000,
                err.getMessage() != null ? err.getMessage() : "internal error", Instant.now()));
    }

    private static void writeJson(RoutingContext ctx, ObjectMapper mapper, int status, Object body) {
        try {
            ctx.response().setStatusCode(status)
                    .putHeader("Content-Type", "application/json")
                    .end(mapper.writeValueAsString(body));
        } catch (Exception e) {
            ctx.fail(e);
        }
    }

    private record ProvisionRequest(long memberId, WalletType walletType, String currency) {
    }

    private record ErrorBody(int code, String message, Instant timestamp) {
    }
}
