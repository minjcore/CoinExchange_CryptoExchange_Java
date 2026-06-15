package com.gtelpay.core.accounting.repository;

import com.gtelpay.core.accounting.domain.CoaAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CoaAccountRepository extends JpaRepository<CoaAccountEntity, String> {
}
