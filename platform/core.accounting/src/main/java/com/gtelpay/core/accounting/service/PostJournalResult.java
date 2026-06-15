package com.gtelpay.core.accounting.service;

import com.gtelpay.core.accounting.domain.JournalStatus;

import java.time.Instant;

public record PostJournalResult(
        long id,
        JournalStatus status,
        Instant postedAt,
        boolean idempotentReplay) {
}
