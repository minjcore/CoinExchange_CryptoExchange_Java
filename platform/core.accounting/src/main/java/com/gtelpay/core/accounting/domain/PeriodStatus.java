package com.gtelpay.core.accounting.domain;

/** ADR-023: accounting period state. Posting allowed only when OPEN. */
public enum PeriodStatus {
    OPEN,
    CLOSED,
    LOCKED
}
