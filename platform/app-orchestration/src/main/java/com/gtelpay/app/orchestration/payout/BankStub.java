package com.gtelpay.app.orchestration.payout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Random;

/**
 * Stub bank/Napas API client for dev/test. Replace with a real HTTP client in production.
 * payout.bank.stub.mode: always-success (default) | always-fail | random
 */
@Component
public class BankStub {

    private static final Logger log = LoggerFactory.getLogger(BankStub.class);
    private static final Random RNG = new Random();

    @Value("${payout.bank.stub.mode:always-success}")
    private String mode;

    public enum BankResult { SUCCESS, FAIL }

    public BankResult dispatch(String businessRef, String bankAccountNumber, String bankCode, String amount) {
        BankResult result = switch (mode) {
            case "always-fail" -> BankResult.FAIL;
            case "random"      -> RNG.nextBoolean() ? BankResult.SUCCESS : BankResult.FAIL;
            default            -> BankResult.SUCCESS;
        };
        log.info("bank stub dispatch businessRef={} bank={}/{} amount={} → {}",
                businessRef, bankCode, bankAccountNumber, amount, result);
        return result;
    }
}
