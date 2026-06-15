package com.gtelpay.core.wallet.service.impl;

import com.gtelpay.core.foundation.exception.ErrorCode;
import com.gtelpay.core.foundation.exception.WalletException;
import com.gtelpay.core.wallet.domain.WalletBalanceEntity;
import com.gtelpay.core.wallet.domain.WalletTxType;

import java.math.BigDecimal;

final class WalletBalanceMutator {

    private WalletBalanceMutator() {
    }

    static void apply(WalletBalanceEntity balance, WalletTxType txType, BigDecimal amount) {
        if (WalletTxType.isCreditType(txType)) {
            creditAvailable(balance, amount);
            return;
        }
        if (WalletTxType.isFreezeType(txType)) {
            freeze(balance, amount);
            return;
        }
        if (WalletTxType.isUnfreezeType(txType)) {
            unfreeze(balance, amount);
            return;
        }
        if (txType.deductFromFrozen()) {
            deductFrozen(balance, amount);
            return;
        }
        if (WalletTxType.isDebitType(txType)) {
            debitAvailable(balance, amount);
            return;
        }
        throw new WalletException(ErrorCode.COMMON_INVALID_REQUEST, "unsupported txType: " + txType);
    }

    private static void creditAvailable(WalletBalanceEntity balance, BigDecimal amount) {
        balance.setAvailable(balance.getAvailable().add(amount));
    }

    private static void debitAvailable(WalletBalanceEntity balance, BigDecimal amount) {
        if (balance.getAvailable().compareTo(amount) < 0) {
            throw new WalletException(ErrorCode.WALLET_INSUFFICIENT_BALANCE, "insufficient available balance");
        }
        balance.setAvailable(balance.getAvailable().subtract(amount));
    }

    private static void freeze(WalletBalanceEntity balance, BigDecimal amount) {
        if (balance.getAvailable().compareTo(amount) < 0) {
            throw new WalletException(ErrorCode.WALLET_INSUFFICIENT_BALANCE, "insufficient available to freeze");
        }
        balance.setAvailable(balance.getAvailable().subtract(amount));
        balance.setFrozen(balance.getFrozen().add(amount));
    }

    private static void unfreeze(WalletBalanceEntity balance, BigDecimal amount) {
        if (balance.getFrozen().compareTo(amount) < 0) {
            throw new WalletException(ErrorCode.WALLET_INSUFFICIENT_BALANCE, "insufficient frozen to release");
        }
        balance.setFrozen(balance.getFrozen().subtract(amount));
        balance.setAvailable(balance.getAvailable().add(amount));
    }

    private static void deductFrozen(WalletBalanceEntity balance, BigDecimal amount) {
        if (balance.getFrozen().compareTo(amount) < 0) {
            throw new WalletException(ErrorCode.WALLET_INSUFFICIENT_BALANCE, "insufficient frozen to settle");
        }
        balance.setFrozen(balance.getFrozen().subtract(amount));
    }
}
