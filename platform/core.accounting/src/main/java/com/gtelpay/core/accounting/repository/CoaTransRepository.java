package com.gtelpay.core.accounting.repository;

import com.gtelpay.core.accounting.domain.CoaTransEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CoaTransRepository extends JpaRepository<CoaTransEntity, Long> {

    Optional<CoaTransEntity> findByReferenceIdAndUseCase(String referenceId, String useCase);
}
