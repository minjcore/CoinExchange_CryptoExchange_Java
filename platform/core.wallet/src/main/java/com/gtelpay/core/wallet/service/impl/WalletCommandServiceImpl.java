package com.gtelpay.core.wallet.service.impl;

import com.gtelpay.core.foundation.exception.ErrorCode;
import com.gtelpay.core.foundation.exception.ValidationException;
import com.gtelpay.core.foundation.exception.WalletException;
import com.gtelpay.core.foundation.util.MoneyUtil;
import com.gtelpay.core.wallet.domain.WalletBalanceEntity;
import com.gtelpay.core.wallet.domain.WalletEntity;
import com.gtelpay.core.wallet.domain.WalletStatus;
import com.gtelpay.core.wallet.domain.WalletTxEntity;
import com.gtelpay.core.wallet.domain.WalletTxType;
import com.gtelpay.core.wallet.domain.WalletType;
import com.gtelpay.core.wallet.repository.WalletBalanceRepository;
import com.gtelpay.core.wallet.repository.WalletRepository;
import com.gtelpay.core.wallet.repository.WalletTxRepository;
import com.gtelpay.core.wallet.service.WalletCommandService;
import com.gtelpay.core.wallet.service.WalletMutationCommand;
import com.gtelpay.core.wallet.service.WalletTxResult;
import com.gtelpay.core.wallet.service.WalletView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class WalletCommandServiceImpl implements WalletCommandService {

    private static final int BUSINESS_REF_MAX = 128;

    // walletId is stable after creation; status changes not supported in v1.
    private final ConcurrentHashMap<String, WalletEntity> walletCache = new ConcurrentHashMap<>();

    private final WalletRepository walletRepository;
    private final WalletBalanceRepository walletBalanceRepository;
    private final WalletTxRepository walletTxRepository;

    public WalletCommandServiceImpl(
            WalletRepository walletRepository,
            WalletBalanceRepository walletBalanceRepository,
            WalletTxRepository walletTxRepository) {
        this.walletRepository = walletRepository;
        this.walletBalanceRepository = walletBalanceRepository;
        this.walletTxRepository = walletTxRepository;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public WalletView provisionIfAbsent(long memberId, WalletType walletType, String currency) {
        String normalizedCurrency = normalizeCurrency(currency);
        WalletEntity wallet = resolveOrCreate(memberId, walletType, normalizedCurrency);
        return toView(wallet);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public WalletTxResult credit(WalletMutationCommand cmd) {
        assertTxType(cmd, WalletTxType::isCreditType, "credit");
        return execute(cmd);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public WalletTxResult debit(WalletMutationCommand cmd) {
        assertTxType(cmd, WalletTxType::isDebitType, "debit");
        return execute(cmd);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public WalletTxResult freeze(WalletMutationCommand cmd) {
        assertTxType(cmd, WalletTxType::isFreezeType, "freeze");
        return execute(cmd);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public WalletTxResult unfreeze(WalletMutationCommand cmd) {
        assertTxType(cmd, WalletTxType::isUnfreezeType, "unfreeze");
        return execute(cmd);
    }

    private WalletTxResult execute(WalletMutationCommand cmd) {
        BigDecimal amount = MoneyUtil.normalize(cmd.amount());
        String businessRef = normalizeBusinessRef(cmd.businessRef());
        WalletEntity wallet = resolveWallet(cmd.memberId(), cmd.walletType(), cmd.currency());
        assertMutable(wallet);

        // Fast path: obvious replay returns without taking the row lock.
        var existing = walletTxRepository.findByWalletIdAndBusinessRefAndTxType(
                wallet.getId(), businessRef, cmd.txType());
        if (existing.isPresent()) {
            return replayExisting(existing.get(), amount, wallet.getId());
        }

        // Optimistic lock via @Version on WalletBalanceEntity. No SELECT FOR UPDATE needed.
        // Duplicate idempotency on concurrent conflict: version mismatch at commit → retry →
        // fast-path above finds the committed wallet_tx and replays idempotently.
        WalletBalanceEntity balance = walletBalanceRepository.findByWalletIdForUpdate(wallet.getId())
                .orElseThrow(() -> new WalletException(ErrorCode.WALLET_NOT_FOUND, "wallet balance missing"));

        WalletBalanceMutator.apply(balance, cmd.txType(), amount);
        walletBalanceRepository.save(balance);

        WalletTxEntity tx = new WalletTxEntity();
        tx.setWalletId(wallet.getId());
        tx.setTxType(cmd.txType());
        tx.setDirection(cmd.txType().direction());
        tx.setAmount(amount);
        tx.setAvailableAfter(balance.getAvailable());
        tx.setFrozenAfter(balance.getFrozen());
        tx.setBusinessRef(businessRef);
        tx.setCoaTransId(cmd.coaTransId());
        tx.setUseCase(cmd.useCase());
        tx.setRemark(cmd.remark());
        WalletTxEntity saved = walletTxRepository.save(tx);

        return new WalletTxResult(saved.getId(), wallet.getId(), balance.getAvailable(), balance.getFrozen(), false);
    }

    private WalletTxResult replayExisting(WalletTxEntity existing, BigDecimal amount, long walletId) {
        if (existing.getAmount().compareTo(amount) != 0) {
            throw new WalletException(ErrorCode.WALLET_DUPLICATE_CONFLICT, "businessRef reused with different amount");
        }
        WalletBalanceEntity balance = walletBalanceRepository.findById(walletId)
                .orElseThrow(() -> new WalletException(ErrorCode.WALLET_NOT_FOUND, "wallet balance missing"));
        return new WalletTxResult(
                existing.getId(), walletId, balance.getAvailable(), balance.getFrozen(), true);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public WalletTxResult creditByWalletId(long walletId, String businessRef,
                                            BigDecimal netAmount, String currency,
                                            Long coaTransId, String useCase) {
        BigDecimal amount = MoneyUtil.normalize(netAmount);
        String ref = normalizeBusinessRef(businessRef);
        WalletEntity wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new WalletException(ErrorCode.WALLET_NOT_FOUND, "wallet not found: " + walletId));
        assertMutable(wallet);

        var existing = walletTxRepository.findByWalletIdAndBusinessRefAndTxType(
                walletId, ref, WalletTxType.DEPOSIT_CREDIT);
        if (existing.isPresent()) {
            return replayExisting(existing.get(), amount, walletId);
        }

        WalletBalanceEntity balance = walletBalanceRepository.findByWalletIdForUpdate(walletId)
                .orElseThrow(() -> new WalletException(ErrorCode.WALLET_NOT_FOUND, "wallet balance missing"));

        WalletBalanceMutator.apply(balance, WalletTxType.DEPOSIT_CREDIT, amount);
        walletBalanceRepository.save(balance);

        WalletTxEntity tx = new WalletTxEntity();
        tx.setWalletId(walletId);
        tx.setTxType(WalletTxType.DEPOSIT_CREDIT);
        tx.setDirection(WalletTxType.DEPOSIT_CREDIT.direction());
        tx.setAmount(amount);
        tx.setAvailableAfter(balance.getAvailable());
        tx.setFrozenAfter(balance.getFrozen());
        tx.setBusinessRef(ref);
        tx.setCoaTransId(coaTransId);
        tx.setUseCase(useCase);
        WalletTxEntity saved = walletTxRepository.save(tx);

        return new WalletTxResult(saved.getId(), walletId, balance.getAvailable(), balance.getFrozen(), false);
    }

    private WalletEntity resolveWallet(long memberId, WalletType walletType, String currency) {
        return resolveOrCreate(memberId, walletType, normalizeCurrency(currency));
    }

    private WalletEntity resolveOrCreate(long memberId, WalletType walletType, String normalizedCurrency) {
        String key = memberId + ":" + walletType + ":" + normalizedCurrency;
        WalletEntity cached = walletCache.get(key);
        if (cached != null) return cached;
        WalletEntity wallet = walletRepository
                .findByMemberIdAndWalletTypeAndCurrency(memberId, walletType, normalizedCurrency)
                .orElseGet(() -> createWalletEntity(memberId, walletType, normalizedCurrency));
        walletCache.put(key, wallet);
        return wallet;
    }

    private WalletView createWallet(long memberId, WalletType walletType, String currency) {
        return toView(createWalletEntity(memberId, walletType, currency));
    }

    private WalletEntity createWalletEntity(long memberId, WalletType walletType, String currency) {
        WalletEntity wallet = new WalletEntity();
        wallet.setMemberId(memberId);
        wallet.setWalletType(walletType);
        wallet.setCurrency(currency);
        wallet.setStatus(WalletStatus.ACTIVE);
        WalletEntity saved = walletRepository.save(wallet);

        WalletBalanceEntity balance = new WalletBalanceEntity();
        balance.setWalletId(saved.getId());
        walletBalanceRepository.save(balance);
        return saved;
    }

    private void assertMutable(WalletEntity wallet) {
        if (wallet.getStatus() == WalletStatus.LOCKED) {
            throw new WalletException(ErrorCode.WALLET_LOCKED, "wallet is locked");
        }
        if (wallet.getStatus() == WalletStatus.CLOSED) {
            throw new WalletException(ErrorCode.COMMON_INVALID_REQUEST, "wallet is closed");
        }
    }

    private static void assertTxType(
            WalletMutationCommand cmd,
            java.util.function.Predicate<WalletTxType> predicate,
            String operation) {
        if (!predicate.test(cmd.txType())) {
            throw new ValidationException("txType " + cmd.txType() + " invalid for " + operation);
        }
    }

    private static String normalizeCurrency(String currency) {
        Objects.requireNonNull(currency, "currency");
        String normalized = currency.trim().toUpperCase();
        if (normalized.length() != 3) {
            throw new ValidationException("currency must be ISO-4217 3-letter code");
        }
        return normalized;
    }

    private static String normalizeBusinessRef(String businessRef) {
        Objects.requireNonNull(businessRef, "businessRef");
        String trimmed = businessRef.trim();
        if (trimmed.isEmpty() || trimmed.length() > BUSINESS_REF_MAX) {
            throw new ValidationException("businessRef length must be 1..128");
        }
        return trimmed;
    }

    private WalletView toView(WalletEntity wallet) {
        return new WalletView(wallet.getId(), wallet.getMemberId(), wallet.getWalletType(),
                wallet.getCurrency(), wallet.getStatus());
    }
}
