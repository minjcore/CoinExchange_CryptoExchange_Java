package com.gtelpay.core.wallet.api.dto;

/**
 * Wire shape: {@code gtelpay-public.yaml} → {@code WalletType} (S1 query/body).
 * Subset of domain {@code com.gtelpay.core.wallet.domain.WalletType} — no PARTNER on public API v1.
 */
public enum WalletTypeWire {
    USER,
    MERCHANT
}
