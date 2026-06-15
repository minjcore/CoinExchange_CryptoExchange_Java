package com.gtelpay.core.wallet.api.dto;

/**
 * Wire: {@code components/schemas/WithdrawalRequest}.
 */
public record WithdrawalRequestWire(
        String businessRef,
        long memberId,
        String amount,
        String currency,
        boolean useFreeze) {
}
