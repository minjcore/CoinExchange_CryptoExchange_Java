package com.gtelpay.core.accounting.service;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface AccountQueryService {

    BigDecimal getBalance(String accountCode, LocalDate asOf);
}
