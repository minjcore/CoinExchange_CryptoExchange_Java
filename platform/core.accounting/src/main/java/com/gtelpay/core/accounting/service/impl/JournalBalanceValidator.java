package com.gtelpay.core.accounting.service.impl;

import com.gtelpay.core.accounting.domain.CoaTransDataEntity;
import com.gtelpay.core.accounting.domain.LineSide;
import com.gtelpay.core.sharedlib.exception.ErrorCode;
import com.gtelpay.core.sharedlib.exception.AccountingException;
import com.gtelpay.core.sharedlib.util.MoneyUtil;

import java.math.BigDecimal;
import java.util.List;

public final class JournalBalanceValidator {

    private JournalBalanceValidator() {
    }

    static void assertBalanced(List<CoaTransDataEntity> lines) {
        BigDecimal debits = BigDecimal.ZERO;
        BigDecimal credits = BigDecimal.ZERO;
        for (CoaTransDataEntity line : lines) {
            if (line.getSide() == LineSide.DEBIT) {
                debits = debits.add(line.getAmount());
            } else {
                credits = credits.add(line.getAmount());
            }
        }
        if (debits.compareTo(credits) != 0) {
            throw new AccountingException(
                    ErrorCode.ACCOUNTING_UNBALANCED_JOURNAL,
                    "sum(DR)=" + debits + " sum(CR)=" + credits);
        }
    }

    public static BigDecimal transitNet(String transitAccount, List<CoaTransDataEntity> lines) {
        BigDecimal net = BigDecimal.ZERO;
        for (CoaTransDataEntity line : lines) {
            if (!transitAccount.equals(line.getAccountCode())) {
                continue;
            }
            if (line.getSide() == LineSide.DEBIT) {
                net = net.add(line.getAmount());
            } else {
                net = net.subtract(line.getAmount());
            }
        }
        return net.setScale(MoneyUtil.MONEY_SCALE, MoneyUtil.MONEY_ROUNDING);
    }

    static void assertTransitZero(String transitAccount, List<CoaTransDataEntity> lines) {
        BigDecimal net = transitNet(transitAccount, lines);
        if (net.compareTo(BigDecimal.ZERO) != 0) {
            throw new AccountingException(
                    ErrorCode.ACCOUNTING_UNBALANCED_JOURNAL,
                    "transit " + transitAccount + " net=" + net + " (expected 0)");
        }
    }

    /**
     * ADR-010: every transit account (COA group 3, code prefix "3") used in a journal must net to
     * zero when it POSTs — for ALL use cases, not just deposit. Enforced inline at post time so a
     * payment/transfer/withdraw/IBFT cannot POST with funds stranded in 3200/3300/3400/3500/...
     */
    static void assertAllTransitZero(List<CoaTransDataEntity> lines) {
        java.util.Set<String> transitCodes = new java.util.LinkedHashSet<>();
        for (CoaTransDataEntity line : lines) {
            String code = line.getAccountCode();
            if (code != null && code.startsWith("3")) {
                transitCodes.add(code);
            }
        }
        for (String code : transitCodes) {
            assertTransitZero(code, lines);
        }
    }
}
