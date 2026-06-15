package com.gtelpay.core.wallet.api.dto;

/**
 * Wire: {@code components/schemas/TransferRequest}.
 */
public record TransferRequestWire(
        String businessRef,
        long fromMemberId,
        long toMemberId,
        String amount,
        String currency,
        String feeAmount) {
}
