package com.gtelpay.app.orchestration;

import com.gtelpay.app.orchestration.vertx.HttpServerVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        ConfigurableApplicationContext spring = SpringApplication.run(OrchestrationApplication.class, args);
        int workerPoolSize = spring.getEnvironment()
                .getProperty("orchestration.vertx.worker-pool-size", Integer.class, 50);
        Vertx vertx = Vertx.vertx(new VertxOptions().setWorkerPoolSize(workerPoolSize));
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
