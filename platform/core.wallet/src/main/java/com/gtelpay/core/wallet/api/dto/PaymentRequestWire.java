package com.gtelpay.core.wallet.api.dto;

/**
 * Wire: {@code components/schemas/PaymentRequest}.
 */
public record PaymentRequestWire(
        String businessRef,
        long memberId,
        long merchantId,
        String amount,
        String currency,
        String netToMerchant) {
}
