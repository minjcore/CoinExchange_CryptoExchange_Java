package com.gtelpay.core.accounting.repository;

import com.gtelpay.core.accounting.domain.CoaTransDataEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface CoaTransDataRepository extends JpaRepository<CoaTransDataEntity, Long> {

    List<CoaTransDataEntity> findByCoaTransId(long coaTransId);

    @Query("""
            select coalesce(sum(
                case when d.side = com.gtelpay.core.accounting.domain.LineSide.DEBIT then d.amount
                     else -d.amount end), 0)
            from CoaTransDataEntity d
            join CoaTransEntity t on t.id = d.coaTransId
            where d.accountCode = :accountCode
              and t.status = com.gtelpay.core.accounting.domain.JournalStatus.POSTED
              and t.postingDate <= :asOf
            """)
    BigDecimal sumPostedBalance(@Param("accountCode") String accountCode, @Param("asOf") LocalDate asOf);
}
