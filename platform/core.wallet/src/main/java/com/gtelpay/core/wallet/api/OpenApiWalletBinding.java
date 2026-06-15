package com.gtelpay.core.wallet.api;

import com.gtelpay.core.wallet.domain.WalletTxType;
import com.gtelpay.core.wallet.service.WalletCommandService;
import com.gtelpay.core.wallet.service.WalletQueryService;

/**
 * S1 {@code gtelpay-public.yaml} operationId → {@code core.wallet} service entry points.
 * Orchestration (BFF) implements controllers; this catalog is the binding reference.
 */
public enum OpenApiWalletBinding {

    GET_WALLET_BALANCE(
            "getWalletBalance",
            "GET /wallets/balance",
            WalletQueryService.class,
            "getBalance(memberId from JWT, walletType, currency)",
            null),

    CREATE_PAYMENT_DEBIT(
            "createPayment",
            "POST /payments",
            WalletCommandService.class,
            "debit(PAYMENT_DEBIT) — step 1",
            WalletTxType.PAYMENT_DEBIT),

    CREATE_PAYMENT_CREDIT(
            "createPayment",
            "POST /payments",
            WalletCommandService.class,
            "credit(PAYMENT_CREDIT) — step 3",
            WalletTxType.PAYMENT_CREDIT),

    CREATE_TRANSFER_DEBIT(
            "createTransfer",
            "POST /transfers",
            WalletCommandService.class,
            "debit(TRANSFER_DEBIT)",
            WalletTxType.TRANSFER_DEBIT),

    CREATE_TRANSFER_CREDIT(
            "createTransfer",
            "POST /transfers",
            WalletCommandService.class,
            "credit(TRANSFER_CREDIT)",
            WalletTxType.TRANSFER_CREDIT),

    CREATE_WITHDRAWAL_FREEZE(
            "createWithdrawal",
            "POST /withdrawals",
            WalletCommandService.class,
            "freeze(WITHDRAW_FREEZE)",
            WalletTxType.WITHDRAW_FREEZE),

    DEPOSIT_CREDIT_ASYNC(
            "notifyDeposit / bankWebhook → S6 → POSTED",
            "POST /deposits/notify (indirect)",
            WalletCommandService.class,
            "credit(DEPOSIT_CREDIT) after coa_trans POSTED",
            WalletTxType.DEPOSIT_CREDIT);

    private final String operationId;
    private final String httpPath;
    private final Class<?> serviceType;
    private final String serviceMethod;
    private final WalletTxType txType;

    OpenApiWalletBinding(
            String operationId,
            String httpPath,
            Class<?> serviceType,
            String serviceMethod,
            WalletTxType txType) {
        this.operationId = operationId;
        this.httpPath = httpPath;
        this.serviceType = serviceType;
        this.serviceMethod = serviceMethod;
        this.txType = txType;
    }

    public String operationId() {
        return operationId;
    }

    public String httpPath() {
        return httpPath;
    }

    public Class<?> serviceType() {
        return serviceType;
    }

    public String serviceMethod() {
        return serviceMethod;
    }

    public WalletTxType txType() {
        return txType;
    }
}
