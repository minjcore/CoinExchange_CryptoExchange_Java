package com.gtelpay.core.accounting;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Test / local bootstrap only — production uses app-orchestration. */
@SpringBootApplication(scanBasePackages = "com.gtelpay.core.accounting")
public class AccountingModuleApplication {

    public static void main(String[] args) {
        SpringApplication.run(AccountingModuleApplication.class, args);
    }
}
