package com.gtelpay.core.wallet.domain;

import java.util.EnumSet;
import java.util.Set;

/** Locked values — spec/implementation.md §2.1 */
public enum WalletTxType {
    DEPOSIT_CREDIT(TxDirection.CREDIT, false),
    PAYMENT_DEBIT(TxDirection.DEBIT, false),
    PAYMENT_CREDIT(TxDirection.CREDIT, false),
    TRANSFER_DEBIT(TxDirection.DEBIT, false),
    TRANSFER_CREDIT(TxDirection.CREDIT, false),
    WITHDRAW_FREEZE(TxDirection.FREEZE, false),
    WITHDRAW_RELEASE(TxDirection.UNFREEZE, false),
    WITHDRAW_SETTLE(TxDirection.DEBIT, true),
    ADJUSTMENT_CREDIT(TxDirection.CREDIT, false),
    ADJUSTMENT_DEBIT(TxDirection.DEBIT, false);

    private static final Set<WalletTxType> CREDIT_TYPES = EnumSet.of(
            DEPOSIT_CREDIT, PAYMENT_CREDIT, TRANSFER_CREDIT, ADJUSTMENT_CREDIT);
    private static final Set<WalletTxType> DEBIT_TYPES = EnumSet.of(
            PAYMENT_DEBIT, TRANSFER_DEBIT, ADJUSTMENT_DEBIT, WITHDRAW_SETTLE);
    private static final Set<WalletTxType> FREEZE_TYPES = EnumSet.of(WITHDRAW_FREEZE);
    private static final Set<WalletTxType> UNFREEZE_TYPES = EnumSet.of(WITHDRAW_RELEASE);

    private final TxDirection direction;
    private final boolean deductFromFrozen;

    WalletTxType(TxDirection direction, boolean deductFromFrozen) {
        this.direction = direction;
        this.deductFromFrozen = deductFromFrozen;
    }

    public TxDirection direction() {
        return direction;
    }

    public boolean deductFromFrozen() {
        return deductFromFrozen;
    }

    public static boolean isCreditType(WalletTxType type) {
        return CREDIT_TYPES.contains(type);
    }

    public static boolean isDebitType(WalletTxType type) {
        return DEBIT_TYPES.contains(type);
    }

    public static boolean isFreezeType(WalletTxType type) {
        return FREEZE_TYPES.contains(type);
    }

    public static boolean isUnfreezeType(WalletTxType type) {
        return UNFREEZE_TYPES.contains(type);
    }
}
