package com.gtelpay.core.accounting.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = "com.gtelpay.core.accounting.repository")
@EntityScan(basePackages = "com.gtelpay.core.accounting.domain")
public class AccountingJpaConfig {
}
