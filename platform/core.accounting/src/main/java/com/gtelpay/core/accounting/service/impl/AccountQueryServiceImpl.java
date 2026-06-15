package com.gtelpay.core.accounting.service.impl;

import com.gtelpay.core.accounting.repository.CoaTransDataRepository;
import com.gtelpay.core.accounting.service.AccountQueryService;
import com.gtelpay.core.foundation.exception.AccountingException;
import com.gtelpay.core.foundation.exception.ErrorCode;
import com.gtelpay.core.foundation.util.MoneyUtil;
import com.gtelpay.core.accounting.repository.CoaAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class AccountQueryServiceImpl implements AccountQueryService {

    private final CoaTransDataRepository coaTransDataRepository;
    private final CoaAccountRepository coaAccountRepository;

    public AccountQueryServiceImpl(
            CoaTransDataRepository coaTransDataRepository,
            CoaAccountRepository coaAccountRepository) {
        this.coaTransDataRepository = coaTransDataRepository;
        this.coaAccountRepository = coaAccountRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getBalance(String accountCode, LocalDate asOf) {
        if (!coaAccountRepository.existsById(accountCode)) {
            throw new AccountingException(
                    ErrorCode.ACCOUNTING_JOURNAL_NOT_FOUND, "unknown account: " + accountCode);
        }
        LocalDate effectiveAsOf = asOf != null ? asOf : LocalDate.now();
        BigDecimal balance = coaTransDataRepository.sumPostedBalance(accountCode, effectiveAsOf);
        return MoneyUtil.normalize(balance != null ? balance : BigDecimal.ZERO);
    }
}
