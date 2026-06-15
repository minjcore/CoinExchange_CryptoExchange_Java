package com.gtelpay.core.wallet.repository;

import com.gtelpay.core.wallet.domain.WalletEntity;
import com.gtelpay.core.wallet.domain.WalletType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WalletRepository extends JpaRepository<WalletEntity, Long> {

    Optional<WalletEntity> findByMemberIdAndWalletTypeAndCurrency(
            long memberId, WalletType walletType, String currency);
}
