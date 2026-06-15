package com.gtelpay.core.accounting.api.dto;

/** Wire: {@code components/schemas/JournalLine}. */
public record JournalLineWire(
        String account_code,
        String amount,
        String side,
        String currency) {
}
