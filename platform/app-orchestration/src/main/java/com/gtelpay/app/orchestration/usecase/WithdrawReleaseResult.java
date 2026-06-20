package com.gtelpay.app.orchestration.usecase;

import java.math.BigDecimal;

public record WithdrawReleaseResult(
        String businessRef,
        long coaTransId,
        long walletTxId,
        BigDecimal available,
        boolean idempotent) {
}
