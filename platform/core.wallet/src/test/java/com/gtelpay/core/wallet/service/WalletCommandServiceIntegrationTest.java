package com.gtelpay.core.wallet.service;

import com.gtelpay.core.foundation.exception.WalletException;
import com.gtelpay.core.wallet.domain.WalletTxType;
import com.gtelpay.core.wallet.domain.WalletType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = com.gtelpay.core.wallet.WalletModuleApplication.class)
class WalletCommandServiceIntegrationTest {

    @Autowired
    private WalletCommandService walletCommandService;

    @Autowired
    private WalletQueryService walletQueryService;

    @Test
    void creditThenDebit_balanceMatches() {
        long memberId = 1001L;
        walletCommandService.provisionIfAbsent(memberId, WalletType.USER, "VND");

        walletCommandService.credit(cmd(memberId, "dep-1", WalletTxType.DEPOSIT_CREDIT, "100000"));
        walletCommandService.debit(cmd(memberId, "pay-1", WalletTxType.PAYMENT_DEBIT, "40000"));

        var balance = walletQueryService.getBalance(memberId, WalletType.USER, "VND");
        assertEquals(new BigDecimal("60000.0000"), balance.available());
    }

    @Test
    void duplicateBusinessRef_replaysWithoutDoubleEffect() {
        long memberId = 1002L;
        walletCommandService.provisionIfAbsent(memberId, WalletType.USER, "VND");

        WalletTxResult first = walletCommandService.credit(
                cmd(memberId, "dep-dup", WalletTxType.DEPOSIT_CREDIT, "50000"));
        WalletTxResult second = walletCommandService.credit(
                cmd(memberId, "dep-dup", WalletTxType.DEPOSIT_CREDIT, "50000"));

        assertTrue(second.idempotentReplay());
        assertEquals(first.walletTxId(), second.walletTxId());

        var balance = walletQueryService.getBalance(memberId, WalletType.USER, "VND");
        assertEquals(new BigDecimal("50000.0000"), balance.available());
    }

    @Test
    void sameRefDifferentAmount_throws409() {
        long memberId = 1003L;
        walletCommandService.provisionIfAbsent(memberId, WalletType.USER, "VND");
        walletCommandService.credit(cmd(memberId, "conflict-1", WalletTxType.DEPOSIT_CREDIT, "100000"));

        assertThrows(WalletException.class, () -> walletCommandService.credit(
                cmd(memberId, "conflict-1", WalletTxType.DEPOSIT_CREDIT, "90000")));
    }

    @Test
    void paymentTwoLegs_sameBusinessRefDifferentTxType() {
        long user = 2001L;
        long merchant = 3001L;
        String ref = "pay-two-leg";

        walletCommandService.provisionIfAbsent(user, WalletType.USER, "VND");
        walletCommandService.provisionIfAbsent(merchant, WalletType.MERCHANT, "VND");
        walletCommandService.credit(cmd(user, "seed", WalletTxType.DEPOSIT_CREDIT, "200000"));

        walletCommandService.debit(new WalletMutationCommand(
                user, WalletType.USER, "VND", bd("100000"), ref,
                WalletTxType.PAYMENT_DEBIT, null, "PAYMENT", null));
        walletCommandService.credit(new WalletMutationCommand(
                merchant, WalletType.MERCHANT, "VND", bd("99000"), ref,
                WalletTxType.PAYMENT_CREDIT, null, "PAYMENT", null));

        assertEquals(new BigDecimal("100000.0000"),
                walletQueryService.getBalance(user, WalletType.USER, "VND").available());
        assertEquals(new BigDecimal("99000.0000"),
                walletQueryService.getBalance(merchant, WalletType.MERCHANT, "VND").available());
    }

    @Test
    void withdrawFreezeThenSettle() {
        long memberId = 1004L;
        walletCommandService.provisionIfAbsent(memberId, WalletType.USER, "VND");
        walletCommandService.credit(cmd(memberId, "dep-wd", WalletTxType.DEPOSIT_CREDIT, "101000"));

        walletCommandService.freeze(cmd(memberId, "wd-1", WalletTxType.WITHDRAW_FREEZE, "101000"));
        var frozen = walletQueryService.getBalance(memberId, WalletType.USER, "VND");
        assertEquals(new BigDecimal("0.0000"), frozen.available());
        assertEquals(new BigDecimal("101000.0000"), frozen.frozen());

        walletCommandService.debit(cmd(memberId, "wd-1:settle", WalletTxType.WITHDRAW_SETTLE, "101000"));
        var after = walletQueryService.getBalance(memberId, WalletType.USER, "VND");
        assertEquals(new BigDecimal("0.0000"), after.available());
        assertEquals(new BigDecimal("0.0000"), after.frozen());
    }

    // A2: IBFT settle must deduct from FROZEN (not available) — else the held funds would be
    // spent twice. Mirrors withdraw; guards WalletTxType.IBFT_SETTLE.deductFromFrozen=true.
    @Test
    void ibftFreezeThenSettle_deductsFrozenNotAvailable() {
        long memberId = 1006L;
        walletCommandService.provisionIfAbsent(memberId, WalletType.USER, "VND");
        walletCommandService.credit(cmd(memberId, "dep-ibft", WalletTxType.DEPOSIT_CREDIT, "150000"));

        walletCommandService.freeze(cmd(memberId, "ibft-1", WalletTxType.IBFT_FREEZE, "101000"));
        var frozen = walletQueryService.getBalance(memberId, WalletType.USER, "VND");
        assertEquals(new BigDecimal("49000.0000"), frozen.available());
        assertEquals(new BigDecimal("101000.0000"), frozen.frozen());

        walletCommandService.debit(cmd(memberId, "ibft-1:settle", WalletTxType.IBFT_SETTLE, "101000"));
        var after = walletQueryService.getBalance(memberId, WalletType.USER, "VND");
        // settle hit frozen only: available stays 49000, frozen back to 0.
        assertEquals(new BigDecimal("49000.0000"), after.available());
        assertEquals(new BigDecimal("0.0000"), after.frozen());
    }

    @Test
    void concurrentSameTriple_appliesOnceAndReplaysLoser() throws Exception {
        long memberId = 1005L;
        walletCommandService.provisionIfAbsent(memberId, WalletType.USER, "VND");

        WalletMutationCommand credit = cmd(memberId, "race-1", WalletTxType.DEPOSIT_CREDIT, "75000");

        int threads = 2;
        var pool = java.util.concurrent.Executors.newFixedThreadPool(threads);
        var barrier = new java.util.concurrent.CyclicBarrier(threads);
        var results = new java.util.concurrent.CopyOnWriteArrayList<WalletTxResult>();
        var errors = new java.util.concurrent.CopyOnWriteArrayList<Throwable>();

        try {
            var futures = new java.util.ArrayList<java.util.concurrent.Future<?>>();
            for (int i = 0; i < threads; i++) {
                futures.add(pool.submit(() -> {
                    try {
                        barrier.await();
                        results.add(walletCommandService.credit(credit));
                    } catch (Throwable t) {
                        errors.add(t);
                    }
                }));
            }
            for (var f : futures) {
                f.get(10, java.util.concurrent.TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }

        assertTrue(errors.isEmpty(), () -> "no thread should fail; got " + errors);
        assertEquals(threads, results.size());
        long applied = results.stream().filter(r -> !r.idempotentReplay()).count();
        assertEquals(1L, applied, "exactly one leg applies the balance effect");
        assertEquals(results.get(0).walletTxId(), results.get(1).walletTxId(),
                "both legs resolve to the same wallet_tx row");

        var balance = walletQueryService.getBalance(memberId, WalletType.USER, "VND");
        assertEquals(new BigDecimal("75000.0000"), balance.available(),
                "credit applied exactly once despite the race");
    }

    private static WalletMutationCommand cmd(
            long memberId, String ref, WalletTxType txType, String amount) {
        return new WalletMutationCommand(
                memberId, WalletType.USER, "VND", bd(amount), ref, txType, null, null, null);
    }

    private static BigDecimal bd(String amount) {
        return new BigDecimal(amount).setScale(4, java.math.RoundingMode.HALF_UP);
    }
}
