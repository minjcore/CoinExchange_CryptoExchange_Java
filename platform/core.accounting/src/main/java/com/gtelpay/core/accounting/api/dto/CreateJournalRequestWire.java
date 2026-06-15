package com.gtelpay.core.accounting.api.dto;

import java.time.LocalDate;

/** Wire: {@code components/schemas/CreateJournalRequest}. */
public record CreateJournalRequestWire(
        String reference_id,
        String use_case,
        String description,
        LocalDate posting_date) {
}
