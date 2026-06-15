package com.gtelpay.core.accounting.service;

import com.gtelpay.core.accounting.domain.LineSide;

import java.math.BigDecimal;

public record JournalLineCommand(
        String accountCode,
        BigDecimal amount,
        LineSide side,
        String currency) {
}
