package com.gtelpay.core.wallet.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "wallet_tx", schema = "wallet",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_wallet_tx_idempotency",
                columnNames = {"wallet_id", "business_ref", "tx_type"}))
public class WalletTxEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "wallet_id", nullable = false)
    private long walletId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tx_type", nullable = false, length = 32)
    private WalletTxType txType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(32)")
    private TxDirection direction;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "available_after", nullable = false, precision = 19, scale = 4)
    private BigDecimal availableAfter;

    @Column(name = "frozen_after", nullable = false, precision = 19, scale = 4)
    private BigDecimal frozenAfter;

    @Column(name = "business_ref", nullable = false, length = 128)
    private String businessRef;

    @Column(name = "coa_trans_id")
    private Long coaTransId;

    @Column(name = "use_case", length = 32)
    private String useCase;

    @Column(length = 512)
    private String remark;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public long getWalletId() {
        return walletId;
    }

    public void setWalletId(long walletId) {
        this.walletId = walletId;
    }

    public WalletTxType getTxType() {
        return txType;
    }

    public void setTxType(WalletTxType txType) {
        this.txType = txType;
    }

    public TxDirection getDirection() {
        return direction;
    }

    public void setDirection(TxDirection direction) {
        this.direction = direction;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getAvailableAfter() {
        return availableAfter;
    }

    public void setAvailableAfter(BigDecimal availableAfter) {
        this.availableAfter = availableAfter;
    }

    public BigDecimal getFrozenAfter() {
        return frozenAfter;
    }

    public void setFrozenAfter(BigDecimal frozenAfter) {
        this.frozenAfter = frozenAfter;
    }

    public String getBusinessRef() {
        return businessRef;
    }

    public void setBusinessRef(String businessRef) {
        this.businessRef = businessRef;
    }

    public Long getCoaTransId() {
        return coaTransId;
    }

    public void setCoaTransId(Long coaTransId) {
        this.coaTransId = coaTransId;
    }

    public String getUseCase() {
        return useCase;
    }

    public void setUseCase(String useCase) {
        this.useCase = useCase;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
