package com.gtelpay.core.wallet.service.impl;

import com.gtelpay.core.sharedlib.exception.ErrorCode;
import com.gtelpay.core.sharedlib.exception.WalletException;
import com.gtelpay.core.sharedlib.page.PageResult;
import com.gtelpay.core.sharedlib.request.PageRequest;
import com.gtelpay.core.wallet.domain.WalletEntity;
import com.gtelpay.core.wallet.domain.WalletTxEntity;
import com.gtelpay.core.wallet.domain.WalletType;
import com.gtelpay.core.wallet.repository.WalletBalanceRepository;
import com.gtelpay.core.wallet.repository.WalletRepository;
import com.gtelpay.core.wallet.repository.WalletTxRepository;
import com.gtelpay.core.wallet.service.BalanceView;
import com.gtelpay.core.wallet.service.WalletQueryService;
import com.gtelpay.core.wallet.service.WalletTxQuery;
import com.gtelpay.core.wallet.service.WalletTxView;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WalletQueryServiceImpl implements WalletQueryService {

    private final WalletRepository walletRepository;
    private final WalletBalanceRepository walletBalanceRepository;
    private final WalletTxRepository walletTxRepository;

    public WalletQueryServiceImpl(
            WalletRepository walletRepository,
            WalletBalanceRepository walletBalanceRepository,
            WalletTxRepository walletTxRepository) {
        this.walletRepository = walletRepository;
        this.walletBalanceRepository = walletBalanceRepository;
        this.walletTxRepository = walletTxRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public BalanceView getBalance(long memberId, WalletType walletType, String currency) {
        WalletEntity wallet = walletRepository
                .findByMemberIdAndWalletTypeAndCurrency(memberId, walletType, currency.trim().toUpperCase())
                .orElseThrow(() -> new WalletException(ErrorCode.WALLET_NOT_FOUND, "wallet not found"));

        var balance = walletBalanceRepository.findById(wallet.getId())
                .orElseThrow(() -> new WalletException(ErrorCode.WALLET_NOT_FOUND, "wallet balance missing"));

        return new BalanceView(
                wallet.getMemberId(),
                wallet.getWalletType(),
                wallet.getCurrency(),
                wallet.getStatus(),
                balance.getAvailable(),
                balance.getFrozen());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<WalletTxView> listTx(WalletTxQuery query, PageRequest page) {
        WalletEntity wallet = walletRepository
                .findByMemberIdAndWalletTypeAndCurrency(
                        query.memberId(), query.walletType(), query.currency().trim().toUpperCase())
                .orElseThrow(() -> new WalletException(ErrorCode.WALLET_NOT_FOUND, "wallet not found"));

        Page<WalletTxEntity> result = walletTxRepository.findByWalletIdOrderByCreatedAtDesc(
                wallet.getId(),
                org.springframework.data.domain.PageRequest.of(page.page(), page.size()));

        return new PageResult<>(
                result.getContent().stream().map(this::toView).toList(),
                result.getTotalElements(),
                page.page(),
                page.size());
    }

    private WalletTxView toView(WalletTxEntity tx) {
        return new WalletTxView(
                tx.getId(),
                tx.getWalletId(),
                tx.getTxType(),
                tx.getDirection(),
                tx.getAmount(),
                tx.getAvailableAfter(),
                tx.getFrozenAfter(),
                tx.getBusinessRef(),
                tx.getCoaTransId(),
                tx.getUseCase(),
                tx.getCreatedAt());
    }
}
