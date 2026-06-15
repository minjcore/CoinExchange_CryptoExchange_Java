package com.gtelpay.app.orchestration.usecase;

public record PaymentResult(
        String businessRef,
        long walletTxId,
        long coaTransId,
        String status) {
}
