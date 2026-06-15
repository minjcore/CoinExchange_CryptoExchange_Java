package com.gtelpay.core.wallet.service;

import com.gtelpay.core.wallet.domain.TxDirection;
import com.gtelpay.core.wallet.domain.WalletTxType;

import java.math.BigDecimal;
import java.time.Instant;

public record WalletTxView(
        long walletTxId,
        long walletId,
        WalletTxType txType,
        TxDirection direction,
        BigDecimal amount,
        BigDecimal availableAfter,
        BigDecimal frozenAfter,
        String businessRef,
        Long coaTransId,
        String useCase,
        Instant createdAt) {
}
