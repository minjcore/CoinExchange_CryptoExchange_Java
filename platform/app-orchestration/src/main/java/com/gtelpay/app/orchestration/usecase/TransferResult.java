package com.gtelpay.app.orchestration.usecase;

public record TransferResult(
        String businessRef,
        long walletTxId,
        long coaTransId,
        String status) {
}
