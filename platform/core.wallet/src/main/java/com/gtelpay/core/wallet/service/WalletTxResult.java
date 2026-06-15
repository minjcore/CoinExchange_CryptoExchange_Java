package com.gtelpay.core.wallet.service;

import java.math.BigDecimal;

public record WalletTxResult(
        long walletTxId,
        long walletId,
        BigDecimal available,
        BigDecimal frozen,
        boolean idempotentReplay) {
}
