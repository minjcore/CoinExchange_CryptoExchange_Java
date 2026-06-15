package com.gtelpay.app.orchestration;

import com.gtelpay.app.orchestration.vertx.HttpServerVerticle;
import io.vertx.core.Vertx;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        ConfigurableApplicationContext spring = SpringApplication.run(OrchestrationApplication.class, args);
        Vertx vertx = Vertx.vertx();
        int port = spring.getEnvironment().getProperty("orchestration.http.port", Integer.class, 8080);

        vertx.deployVerticle(new HttpServerVerticle(spring, port))
                .onFailure(err -> {
                    err.printStackTrace();
                    spring.close();
                    vertx.close();
                    System.exit(1);
                });
    }
}
