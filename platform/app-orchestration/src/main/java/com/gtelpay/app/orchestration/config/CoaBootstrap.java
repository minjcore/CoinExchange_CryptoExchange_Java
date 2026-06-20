package com.gtelpay.app.orchestration.config;

import com.gtelpay.core.accounting.domain.AccountType;
import com.gtelpay.core.accounting.domain.CoaAccountEntity;
import com.gtelpay.core.accounting.repository.CoaAccountRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class CoaBootstrap implements ApplicationRunner {

    private final CoaAccountRepository coaAccountRepository;

    public CoaBootstrap(CoaAccountRepository coaAccountRepository) {
        this.coaAccountRepository = coaAccountRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (coaAccountRepository.count() > 0) {
            return;
        }
        seed("1111", "Vietinbank — dedicated", AccountType.ASSET);
        seed("2110", "Wallet balance — User", AccountType.LIABILITY);
        seed("2120", "Wallet balance — Merchant", AccountType.LIABILITY);
        seed("3100", "Transit — deposit", AccountType.TRANSIT);
        seed("3300", "Transit — internal transfer", AccountType.TRANSIT);
        seed("3500", "Transit — payment", AccountType.TRANSIT);
        seed("3200", "Transit — withdraw", AccountType.TRANSIT);
        seed("3400", "Transit — IBFT", AccountType.TRANSIT);
        seed("4110", "Fee revenue — deposit", AccountType.REVENUE);
        seed("4120", "Fee revenue — withdraw", AccountType.REVENUE);
        seed("4130", "Fee revenue — transfer", AccountType.REVENUE);
        seed("1112", "Napas Clearing", AccountType.ASSET);
        seed("5100", "Bank / Napas fee expense", AccountType.EXPENSE);
    }

    private void seed(String code, String name, AccountType type) {
        CoaAccountEntity entity = new CoaAccountEntity();
        entity.setCode(code);
        entity.setName(name);
        entity.setAccountType(type);
        entity.setActive(true);
        coaAccountRepository.save(entity);
    }
}
