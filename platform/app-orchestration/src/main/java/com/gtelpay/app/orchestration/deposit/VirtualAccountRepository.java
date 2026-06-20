package com.gtelpay.app.orchestration.deposit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VirtualAccountRepository extends JpaRepository<VirtualAccountEntity, Long> {
    Optional<VirtualAccountEntity> findByVaNumber(String vaNumber);
}
