package com.gtelpay.core.accounting.validation;

import com.gtelpay.core.accounting.service.JournalLineCommand;
import com.gtelpay.core.sharedlib.exception.ValidationException;
import com.gtelpay.core.sharedlib.util.MoneyUtil;

import java.math.BigDecimal;
import java.util.List;

public final class JournalLineCommandValidator {

    private JournalLineCommandValidator() {
    }

    public static void requireNonEmpty(List<JournalLineCommand> lines) {
        if (lines == null || lines.isEmpty()) {
            throw new ValidationException("lines must not be empty");
        }
    }

    public static void validateLine(JournalLineCommand line) {
        if (line == null) {
            throw new ValidationException("journal line required");
        }
        if (line.accountCode() == null || line.accountCode().isBlank()) {
            throw new ValidationException("accountCode required");
        }
        BigDecimal amount = MoneyUtil.normalize(line.amount());
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("line amount must be > 0");
        }
    }
}
