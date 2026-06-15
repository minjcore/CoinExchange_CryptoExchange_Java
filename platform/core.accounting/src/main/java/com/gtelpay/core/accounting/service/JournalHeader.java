package com.gtelpay.core.accounting.service;

import com.gtelpay.core.accounting.domain.JournalStatus;

import java.time.LocalDate;

public record JournalHeader(
        long id,
        String referenceId,
        String useCase,
        JournalStatus status,
        LocalDate postingDate) {
}
