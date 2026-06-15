package com.gtelpay.core.wallet.http;

import com.gtelpay.core.wallet.WalletModuleApplication;
import io.vertx.core.Vertx;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Runs core.wallet as a standalone service: Spring domain context + Vert.x HTTP
 * ({@link WalletHttpVerticle}) exposing wallet-internal.yaml (ADR-038).
 */
public final class WalletServiceMain {

    private WalletServiceMain() {
    }

    public static void main(String[] args) {
        ConfigurableApplicationContext spring = SpringApplication.run(WalletModuleApplication.class, args);
        Vertx vertx = Vertx.vertx();
        int port = spring.getEnvironment().getProperty("wallet.http.port", Integer.class, 8082);
        vertx.deployVerticle(new WalletHttpVerticle(spring, port))
                .onFailure(err -> {
                    err.printStackTrace();
                    spring.close();
                    vertx.close();
                    System.exit(1);
                });
    }
}
