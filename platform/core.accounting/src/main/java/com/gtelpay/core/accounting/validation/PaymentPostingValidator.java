package com.gtelpay.core.accounting.validation;

import com.gtelpay.core.accounting.domain.LineSide;
import com.gtelpay.core.accounting.service.JournalLineCommand;
import com.gtelpay.core.foundation.exception.ValidationException;
import com.gtelpay.core.foundation.util.MoneyUtil;

import java.math.BigDecimal;
import java.util.List;

/**
 * Payment journal template — {@code design/accounting/postings.md} · v1 requires net = gross.
 */
public final class PaymentPostingValidator {

    private PaymentPostingValidator() {
    }

    public static List<JournalLineCommand> buildV1Lines(
            String grossAmount,
            String netToMerchant,
            String currency) {
        String normalizedCurrency = currency != null ? currency.trim().toUpperCase() : "VND";
        if (normalizedCurrency.length() != 3) {
            throw new ValidationException("currency must be ISO-4217 3-letter code");
        }
        BigDecimal gross = MoneyUtil.parseAmount(grossAmount);
        String netRaw = netToMerchant != null && !netToMerchant.isBlank()
                ? netToMerchant
                : grossAmount;
        BigDecimal net = MoneyUtil.parseAmount(netRaw);
        if (net.compareTo(gross) != 0) {
            throw new ValidationException(
                    "v1 payment journal requires netToMerchant equals amount (fee lines not yet supported)");
        }
        return List.of(
                new JournalLineCommand("2110", gross, LineSide.DEBIT, normalizedCurrency),
                new JournalLineCommand("3500", gross, LineSide.CREDIT, normalizedCurrency),
                new JournalLineCommand("3500", gross, LineSide.DEBIT, normalizedCurrency),
                new JournalLineCommand("2120", net, LineSide.CREDIT, normalizedCurrency));
    }
}
