package com.gtelpay.core.wallet.repository;

import com.gtelpay.core.wallet.domain.WalletBalanceEntity;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface WalletBalanceRepository extends JpaRepository<WalletBalanceEntity, Long> {

    // NOWAIT: fail immediately if locked → caller retries with backoff instead of queuing at DB level.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "javax.persistence.lock.timeout", value = "0"))
    @Query("select b from WalletBalanceEntity b where b.walletId = :walletId")
    Optional<WalletBalanceEntity> findByWalletIdForUpdate(@Param("walletId") long walletId);
}
