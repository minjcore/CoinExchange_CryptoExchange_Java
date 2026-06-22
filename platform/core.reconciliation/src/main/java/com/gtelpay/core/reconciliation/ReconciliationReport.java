package com.gtelpay.core.reconciliation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Result of a single EOD reconciliation run.
 */
public record ReconciliationReport(
        LocalDate date,
        int totalMembers,
        int matchedMembers,
        List<Discrepancy> discrepancies
) {

    public boolean isClean() {
        return discrepancies.isEmpty();
    }

    public record Discrepancy(
            long memberId,
            String currency,
            BigDecimal walletAvailable,
            BigDecimal walletFrozen,
            BigDecimal ledgerNet,
            String reason
    ) {}
}
