package com.gtelpay.core.accounting.validation;

import com.gtelpay.core.accounting.domain.LineSide;
import com.gtelpay.core.accounting.service.JournalLineCommand;
import com.gtelpay.core.sharedlib.exception.ValidationException;
import com.gtelpay.core.sharedlib.util.MoneyUtil;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Internal transfer journal template — {@code spec/foundation.md} §10 (transit 3300).
 * gross = net + fee; fee revenue to 4130. Transit 3300 nets to 0.
 */
public final class TransferPostingValidator {

    private TransferPostingValidator() {
    }

    public static List<JournalLineCommand> buildV1Lines(
            String grossAmount,
            String netAmount,
            String currency) {
        String cur = currency != null ? currency.trim().toUpperCase() : "VND";
        if (cur.length() != 3) {
            throw new ValidationException("currency must be ISO-4217 3-letter code");
        }
        BigDecimal gross = MoneyUtil.parseAmount(grossAmount);
        BigDecimal net = MoneyUtil.parseAmount(netAmount);
        BigDecimal fee = gross.subtract(net);
        if (fee.signum() < 0) {
            throw new ValidationException("netToRecipient must not exceed gross");
        }
        List<JournalLineCommand> lines = new ArrayList<>(List.of(
                new JournalLineCommand("2110", gross, LineSide.DEBIT, cur),   // sender A liability down (gross)
                new JournalLineCommand("3300", gross, LineSide.CREDIT, cur),
                new JournalLineCommand("3300", net, LineSide.DEBIT, cur),
                new JournalLineCommand("2110", net, LineSide.CREDIT, cur)));   // recipient B liability up (net)
        if (fee.signum() > 0) {
            lines.add(new JournalLineCommand("3300", fee, LineSide.DEBIT, cur));
            lines.add(new JournalLineCommand("4130", fee, LineSide.CREDIT, cur)); // transfer fee revenue
        }
        return List.copyOf(lines);
    }
}
