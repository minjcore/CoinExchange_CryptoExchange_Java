package com.gtelpay.core.accounting.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.Instant;

// ADR-001: journal lines are write-once. @Immutable makes Hibernate refuse to emit UPDATEs —
// a hard ORM guard on top of the INV-04 CI check. Lines are only ever INSERTed (persistLine);
// corrections are new reversing journals, never edits.
@Entity
@Immutable
@Table(name = "coa_trans_data", schema = "accounting",
        indexes = @Index(name = "idx_coa_trans_data_journal", columnList = "coa_trans_id"))
public class CoaTransDataEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "coa_trans_id", nullable = false)
    private long coaTransId;

    @Column(name = "account_code", nullable = false, length = 16)
    private String accountCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(16)")
    private LineSide side;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency = "VND";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public long getCoaTransId() {
        return coaTransId;
    }

    public void setCoaTransId(long coaTransId) {
        this.coaTransId = coaTransId;
    }

    public String getAccountCode() {
        return accountCode;
    }

    public void setAccountCode(String accountCode) {
        this.accountCode = accountCode;
    }

    public LineSide getSide() {
        return side;
    }

    public void setSide(LineSide side) {
        this.side = side;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
