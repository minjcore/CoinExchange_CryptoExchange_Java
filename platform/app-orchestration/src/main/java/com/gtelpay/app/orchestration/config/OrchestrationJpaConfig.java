package com.gtelpay.app.orchestration.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = "com.gtelpay.app.orchestration.deposit")
@EntityScan(basePackages = "com.gtelpay.app.orchestration.deposit")
public class OrchestrationJpaConfig {
}
