package com.gtelpay.app.orchestration.usecase;

import java.math.BigDecimal;

public record IbftSettleResult(
        String businessRef,
        long coaTransId,
        long walletTxId,
        BigDecimal frozen,
        boolean idempotent) {
}
