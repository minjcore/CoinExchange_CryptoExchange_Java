package com.gtelpay.core.wallet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Test / local bootstrap only — production uses app-orchestration. */
@SpringBootApplication(scanBasePackages = "com.gtelpay.core.wallet")
public class WalletModuleApplication {

    public static void main(String[] args) {
        SpringApplication.run(WalletModuleApplication.class, args);
    }
}
