package com.gtelpay.core.wallet.repository;

import com.gtelpay.core.wallet.domain.WalletBalanceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface WalletBalanceRepository extends JpaRepository<WalletBalanceEntity, Long> {

    // Optimistic locking via @Version on WalletBalanceEntity — no SELECT FOR UPDATE.
    // Conflict detected at commit; caller retries on OptimisticLockingFailureException.
    @Query("select b from WalletBalanceEntity b where b.walletId = :walletId")
    Optional<WalletBalanceEntity> findByWalletIdForUpdate(@Param("walletId") long walletId);
}
