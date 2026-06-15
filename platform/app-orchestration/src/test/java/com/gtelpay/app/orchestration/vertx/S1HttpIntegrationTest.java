package com.gtelpay.app.orchestration.vertx;

import com.gtelpay.core.wallet.domain.WalletTxType;
import com.gtelpay.core.wallet.domain.WalletType;
import com.gtelpay.core.wallet.service.WalletCommandService;
import com.gtelpay.core.wallet.service.WalletMutationCommand;
import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(VertxExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class S1HttpIntegrationTest {

    private static final int PORT = 18080;

    private ConfigurableApplicationContext spring;
    private Vertx vertx;

    @BeforeEach
    void setUp(VertxTestContext ctx) throws Exception {
        System.setProperty("orchestration.http.port", String.valueOf(PORT));
        spring = SpringApplication.run(com.gtelpay.app.orchestration.OrchestrationApplication.class);
        seedUserWallet();

        vertx = Vertx.vertx();
        vertx.deployVerticle(new HttpServerVerticle(spring, PORT))
                .toCompletionStage()
                .toCompletableFuture()
                .get(10, TimeUnit.SECONDS);
        ctx.completeNow();
    }

    @AfterEach
    void tearDown() {
        if (vertx != null) {
            vertx.close();
        }
        if (spring != null) {
            spring.close();
        }
    }

    @Test
    @Order(1)
    void getWalletBalance_returnsOpenApiShape(VertxTestContext testContext) {
        WebClient client = WebClient.create(vertx);
        client.get(PORT, "localhost", "/v1/wallets/balance")
                .addQueryParam("walletType", "USER")
                .addQueryParam("currency", "VND")
                .putHeader("X-Member-Id", "9001")
                .send()
                .onComplete(ar -> {
                    if (ar.failed()) {
                        testContext.failNow(ar.cause());
                        return;
                    }
                    try {
                        String body = ar.result().bodyAsString();
                        assertEquals(200, ar.result().statusCode(), () -> body);
                        assertTrue(body.contains("\"code\":0"), () -> body);
                        assertTrue(body.contains("\"available\":\"200000.0000\""), () -> body);
                        testContext.completeNow();
                    } catch (Throwable t) {
                        testContext.failNow(t);
                    }
                });
    }

    @Test
    @Order(2)
    void createPayment_syncFlow(VertxTestContext testContext) {
        WebClient client = WebClient.create(vertx);
        String ref = "pay-vertx-1";
        String json = """
                {
                  "businessRef": "%s",
                  "memberId": 9001,
                  "merchantId": 9002,
                  "amount": "50000.0000",
                  "currency": "VND"
                }
                """.formatted(ref);

        client.post(PORT, "localhost", "/v1/payments")
                .putHeader("Content-Type", "application/json")
                .putHeader("X-Idempotency-Key", ref)
                .sendBuffer(io.vertx.core.buffer.Buffer.buffer(json))
                .onComplete(ar -> {
                    if (ar.failed()) {
                        testContext.failNow(ar.cause());
                        return;
                    }
                    try {
                        assertEquals(200, ar.result().statusCode());
                        String body = ar.result().bodyAsString();
                        assertTrue(body.contains("\"status\":\"SUCCESS\""));
                        assertTrue(body.contains("\"coaTransId\""));
                        testContext.completeNow();
                    } catch (Throwable t) {
                        testContext.failNow(t);
                    }
                });
    }

    private void seedUserWallet() {
        WalletCommandService wallet = spring.getBean(WalletCommandService.class);
        wallet.provisionIfAbsent(9001L, WalletType.USER, "VND");
        wallet.provisionIfAbsent(9002L, WalletType.MERCHANT, "VND");
        wallet.credit(new WalletMutationCommand(
                9001L, WalletType.USER, "VND", new BigDecimal("200000.0000"),
                "seed-9001", WalletTxType.DEPOSIT_CREDIT, null, "DEPOSIT", null));
    }
}
