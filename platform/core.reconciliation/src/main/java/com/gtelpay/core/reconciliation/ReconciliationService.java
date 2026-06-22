package com.gtelpay.core.reconciliation;

/**
 * EOD reconciliation: compare wallet_balance totals against coa_balance totals.
 * Detects drift between the wallet schema and the accounting schema.
 *
 * TODO (spec 010): implement runEod() — cross-join wallet_balance and coa_balance,
 * flag any member whose net wallet position does not match the corresponding ledger entries.
 */
public interface ReconciliationService {

    ReconciliationReport runEod(java.time.LocalDate date);
}
