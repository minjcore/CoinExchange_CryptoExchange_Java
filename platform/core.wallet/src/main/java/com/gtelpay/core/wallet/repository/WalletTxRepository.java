package com.gtelpay.core.wallet.repository;

import com.gtelpay.core.wallet.domain.WalletTxEntity;
import com.gtelpay.core.wallet.domain.WalletTxType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WalletTxRepository extends JpaRepository<WalletTxEntity, Long> {

    Optional<WalletTxEntity> findByWalletIdAndBusinessRefAndTxType(
            long walletId, String businessRef, WalletTxType txType);

    Page<WalletTxEntity> findByWalletIdOrderByCreatedAtDesc(long walletId, Pageable pageable);
}
