package com.gtelpay.app.orchestration.usecase;

import java.math.BigDecimal;

public record WithdrawResult(
        String businessRef,
        String status,
        long walletId,
        long coaTransId,
        BigDecimal available,
        BigDecimal frozen) {
}
