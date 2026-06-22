package com.gtelpay.core.accounting.validation;

import com.gtelpay.core.accounting.domain.LineSide;
import com.gtelpay.core.accounting.service.JournalLineCommand;
import com.gtelpay.core.sharedlib.exception.ValidationException;
import com.gtelpay.core.sharedlib.util.MoneyUtil;

import java.math.BigDecimal;
import java.util.List;

/**
 * Withdraw accept-phase template — {@code spec/foundation.md} §9, ADR-007 (freeze→settle).
 * On accept the user liability moves to transit 3200 (held until bank settles). Transit 3200
 * is cleared later by the settle journal (3200 DR principal + 1111 CR, 3200 DR fee + 4120 CR),
 * so the accept journal is balanced but does NOT net transit to zero yet.
 */
public final class WithdrawPostingValidator {

    private WithdrawPostingValidator() {
    }

    public static List<JournalLineCommand> acceptLines(String grossAmount, String currency) {
        String cur = currency != null ? currency.trim().toUpperCase() : "VND";
        if (cur.length() != 3) {
            throw new ValidationException("currency must be ISO-4217 3-letter code");
        }
        BigDecimal gross = MoneyUtil.parseAmount(grossAmount);
        if (gross.signum() <= 0) {
            throw new ValidationException("withdraw amount must be positive");
        }
        return List.of(
                new JournalLineCommand("2110", gross, LineSide.DEBIT, cur),   // user liability down (gross)
                new JournalLineCommand("3200", gross, LineSide.CREDIT, cur)); // held in transit until bank settle
    }
}
