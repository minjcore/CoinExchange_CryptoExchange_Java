package com.gtelpay.core.accounting.validation;

import com.gtelpay.core.accounting.domain.LineSide;
import com.gtelpay.core.accounting.service.JournalLineCommand;
import com.gtelpay.core.foundation.exception.ValidationException;
import com.gtelpay.core.foundation.util.MoneyUtil;

import java.math.BigDecimal;
import java.util.List;

/**
 * Deposit phase-A template — {@code spec/foundation.md} §8.3 (PENDING): bank cash in,
 * funds held in transit 3100. Phase B (3100 DR, 2110 CR net, 4110 CR fee) is
 * {@code JournalService.confirmDeposit}.
 */
public final class DepositPostingValidator {

    private DepositPostingValidator() {
    }

    public static List<JournalLineCommand> phaseALines(String grossAmount, String currency) {
        String cur = currency != null ? currency.trim().toUpperCase() : "VND";
        if (cur.length() != 3) {
            throw new ValidationException("currency must be ISO-4217 3-letter code");
        }
        BigDecimal gross = MoneyUtil.parseAmount(grossAmount);
        if (gross.signum() <= 0) {
            throw new ValidationException("deposit amount must be positive");
        }
        return List.of(
                new JournalLineCommand("1111", gross, LineSide.DEBIT, cur),   // bank asset in
                new JournalLineCommand("3100", gross, LineSide.CREDIT, cur)); // funds in transit
    }
}
