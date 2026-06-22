package com.gtelpay.core.accounting.validation;

import com.gtelpay.core.accounting.domain.LineSide;
import com.gtelpay.core.accounting.service.JournalLineCommand;
import com.gtelpay.core.sharedlib.exception.ValidationException;
import com.gtelpay.core.sharedlib.util.MoneyUtil;

import java.math.BigDecimal;
import java.util.List;

/**
 * IBFT accept-phase template — ADR-007 (freeze→settle), ADR-025 (Napas clearing 1112).
 * Phase A: 2110 DR / 3400 CR (gross). Transit 3400 cleared by confirmIbft Phase B.
 */
public final class IbftPostingValidator {

    private IbftPostingValidator() {
    }

    public static List<JournalLineCommand> acceptLines(String grossAmount, String currency) {
        String cur = currency != null ? currency.trim().toUpperCase() : "VND";
        if (cur.length() != 3) {
            throw new ValidationException("currency must be ISO-4217 3-letter code");
        }
        BigDecimal gross = MoneyUtil.parseAmount(grossAmount);
        if (gross.signum() <= 0) {
            throw new ValidationException("IBFT amount must be positive");
        }
        return List.of(
                new JournalLineCommand("2110", gross, LineSide.DEBIT, cur),
                new JournalLineCommand("3400", gross, LineSide.CREDIT, cur));
    }
}
