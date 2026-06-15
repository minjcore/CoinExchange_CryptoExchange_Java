package com.gtelpay.core.accounting.api.dto;

import java.time.Instant;

/** Wire: {@code components/schemas/PostJournalResult}. */
public record PostJournalResultWire(
        long id,
        String status,
        Instant posted_at) {
}
