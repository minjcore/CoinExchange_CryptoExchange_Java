package com.gtelpay.core.wallet.service.impl;

import com.gtelpay.core.wallet.domain.WalletType;
import com.gtelpay.core.wallet.service.WalletCommandService;
import com.gtelpay.core.wallet.service.WalletMutationCommand;
import com.gtelpay.core.wallet.service.WalletTxResult;
import com.gtelpay.core.wallet.service.WalletView;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Wraps WalletCommandServiceImpl with @Retryable on optimistic lock conflicts.
 *
 * Retry MUST be in a separate bean (not on WalletCommandServiceImpl directly) so each
 * attempt calls through the Spring proxy, starting a fresh @Transactional. If @Retryable
 * and @Transactional were on the same method, the retry would be inside an already-failed
 * transaction context.
 *
 * Backoff: delay = one single-wallet TX time (~30 ms), multiplier = 2, random = true.
 * Random jitter prevents all conflicting retriers from waking simultaneously (thundering herd).
 */
@Primary
@Service
public class WalletCommandServiceRetryDecorator implements WalletCommandService {

    private final WalletCommandService delegate;

    public WalletCommandServiceRetryDecorator(WalletCommandServiceImpl delegate) {
        this.delegate = delegate;
    }

    @Override
    public WalletView provisionIfAbsent(long memberId, WalletType walletType, String currency) {
        return delegate.provisionIfAbsent(memberId, walletType, currency);
    }

    @Override
    @Retryable(
            retryFor = OptimisticLockingFailureException.class,
            maxAttempts = 5,
            backoff = @Backoff(delay = 30, multiplier = 2, random = true)
    )
    public WalletTxResult credit(WalletMutationCommand cmd) {
        return delegate.credit(cmd);
    }

    @Override
    @Retryable(
            retryFor = OptimisticLockingFailureException.class,
            maxAttempts = 5,
            backoff = @Backoff(delay = 30, multiplier = 2, random = true)
    )
    public WalletTxResult creditByWalletId(long walletId, String businessRef,
                                            BigDecimal netAmount, String currency,
                                            Long coaTransId, String useCase) {
        return delegate.creditByWalletId(walletId, businessRef, netAmount, currency, coaTransId, useCase);
    }

    @Override
    @Retryable(
            retryFor = OptimisticLockingFailureException.class,
            maxAttempts = 5,
            backoff = @Backoff(delay = 30, multiplier = 2, random = true)
    )
    public WalletTxResult debit(WalletMutationCommand cmd) {
        return delegate.debit(cmd);
    }

    @Override
    @Retryable(
            retryFor = OptimisticLockingFailureException.class,
            maxAttempts = 5,
            backoff = @Backoff(delay = 30, multiplier = 2, random = true)
    )
    public WalletTxResult freeze(WalletMutationCommand cmd) {
        return delegate.freeze(cmd);
    }

    @Override
    @Retryable(
            retryFor = OptimisticLockingFailureException.class,
            maxAttempts = 5,
            backoff = @Backoff(delay = 30, multiplier = 2, random = true)
    )
    public WalletTxResult unfreeze(WalletMutationCommand cmd) {
        return delegate.unfreeze(cmd);
    }
}
