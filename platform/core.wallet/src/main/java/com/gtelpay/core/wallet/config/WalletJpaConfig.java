package com.gtelpay.core.wallet.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = "com.gtelpay.core.wallet.repository")
@EntityScan(basePackages = "com.gtelpay.core.wallet.domain")
public class WalletJpaConfig {
}
