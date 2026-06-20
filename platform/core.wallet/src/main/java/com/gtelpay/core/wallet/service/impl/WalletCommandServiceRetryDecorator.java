package com.gtelpay.core.wallet.service.impl;

import com.gtelpay.core.wallet.domain.WalletType;
import com.gtelpay.core.wallet.service.WalletCommandService;
import com.gtelpay.core.wallet.service.WalletMutationCommand;
import com.gtelpay.core.wallet.service.WalletTxResult;
import com.gtelpay.core.wallet.service.WalletView;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Wraps WalletCommandServiceImpl with retry-on-lock logic.
 *
 * Retry MUST be in a separate bean so each attempt calls through the Spring proxy,
 * starting a fresh @Transactional. Putting @Retryable inside @Transactional would
 * retry within the same failed transaction — no good.
 *
 * Lock strategy: NOWAIT (fail immediately) + backoff here, rather than queueing at DB.
 * This frees the connection during the wait instead of holding it blocked in Postgres.
 */
@Primary
@Service
public class WalletCommandServiceRetryDecorator implements WalletCommandService {

    private static final int MAX_ATTEMPTS = 5;
    private static final long BASE_DELAY_MS = 5;

    private final WalletCommandService delegate;

    public WalletCommandServiceRetryDecorator(WalletCommandServiceImpl delegate) {
        this.delegate = delegate;
    }

    @Override
    public WalletView provisionIfAbsent(long memberId, WalletType walletType, String currency) {
        return delegate.provisionIfAbsent(memberId, walletType, currency);
    }

    @Override
    public WalletTxResult credit(WalletMutationCommand cmd) {
        return withRetry(() -> delegate.credit(cmd));
    }

    @Override
    public WalletTxResult creditByWalletId(long walletId, String businessRef,
                                            BigDecimal netAmount, String currency,
                                            Long coaTransId, String useCase) {
        return withRetry(() -> delegate.creditByWalletId(walletId, businessRef, netAmount, currency, coaTransId, useCase));
    }

    @Override
    public WalletTxResult debit(WalletMutationCommand cmd) {
        return withRetry(() -> delegate.debit(cmd));
    }

    @Override
    public WalletTxResult freeze(WalletMutationCommand cmd) {
        return withRetry(() -> delegate.freeze(cmd));
    }

    @Override
    public WalletTxResult unfreeze(WalletMutationCommand cmd) {
        return withRetry(() -> delegate.unfreeze(cmd));
    }

    private <T> T withRetry(ThrowingSupplier<T> action) {
        PessimisticLockingFailureException last = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return action.get();
            } catch (PessimisticLockingFailureException e) {
                last = e;
                if (attempt < MAX_ATTEMPTS) {
                    sleep(BASE_DELAY_MS * attempt);
                }
            }
        }
        throw last;
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get();
    }
}
