package com.gtelpay.core.accounting.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * ADR-023: an accounting period keyed by {@code period_code} = "yyyy-MM" of the posting date.
 * Absence of a row = the period is OPEN by default; a row marks it CLOSED or LOCKED.
 */
@Entity
@Table(name = "coa_period", schema = "accounting")
public class CoaPeriodEntity {

    @Id
    @Column(name = "period_code", length = 7)
    private String periodCode; // yyyy-MM

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PeriodStatus status = PeriodStatus.OPEN;

    public String getPeriodCode() {
        return periodCode;
    }

    public void setPeriodCode(String periodCode) {
        this.periodCode = periodCode;
    }

    public PeriodStatus getStatus() {
        return status;
    }

    public void setStatus(PeriodStatus status) {
        this.status = status;
    }
}
