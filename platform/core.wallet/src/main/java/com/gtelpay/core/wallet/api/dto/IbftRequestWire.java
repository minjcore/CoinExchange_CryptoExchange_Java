package com.gtelpay.core.wallet.api.dto;

/**
 * Wire: {@code components/schemas/IbftRequest} — interbank fund transfer (UC-5).
 * gross = principalAmount + platformFee; napasCost is platform-borne, not in gross.
 */
public record IbftRequestWire(
        String businessRef,
        long memberId,
        String principalAmount,
        String platformFee,
        String napasCost,
        String currency,
        String destinationBankAccountNumber,
        String destinationBankCode) {
}
