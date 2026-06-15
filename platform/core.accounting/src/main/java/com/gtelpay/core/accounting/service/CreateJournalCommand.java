package com.gtelpay.core.accounting.service;

import java.time.LocalDate;

public record CreateJournalCommand(
        String referenceId,
        String useCase,
        String description,
        LocalDate postingDate) {
}
