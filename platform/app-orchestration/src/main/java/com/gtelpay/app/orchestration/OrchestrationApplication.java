package com.gtelpay.app.orchestration;

import com.gtelpay.core.accounting.config.AccountingJpaConfig;
import com.gtelpay.core.wallet.config.WalletJpaConfig;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.retry.annotation.EnableRetry;

/**
 * Spring context for domain services only — HTTP is served by Vert.x ({@link Main}).
 */
@SpringBootApplication(scanBasePackages = {
        "com.gtelpay.app.orchestration",
        "com.gtelpay.core.wallet",
        "com.gtelpay.core.accounting"
})
@Import({WalletJpaConfig.class, AccountingJpaConfig.class})
@EnableRetry
public class OrchestrationApplication {
}
