package com.gtelpay.core.wallet.api;

import com.gtelpay.core.sharedlib.exception.ValidationException;
import com.gtelpay.core.sharedlib.util.MoneyUtil;
import com.gtelpay.core.wallet.api.dto.PaymentRequestWire;
import com.gtelpay.core.wallet.api.dto.TransferRequestWire;
import com.gtelpay.core.wallet.api.dto.WalletBalanceDataWire;
import com.gtelpay.core.wallet.api.dto.WalletTypeWire;
import com.gtelpay.core.wallet.api.dto.WithdrawalRequestWire;
import com.gtelpay.core.wallet.domain.WalletTxType;
import com.gtelpay.core.wallet.domain.WalletType;
import com.gtelpay.core.wallet.service.BalanceView;
import com.gtelpay.core.wallet.service.WalletMutationCommand;
import com.gtelpay.core.wallet.service.WalletTxResult;

import java.math.BigDecimal;

/**
 * Maps S1 OpenAPI wire DTOs ↔ {@code core.wallet} domain commands/views.
 * Design source of truth: {@code design/wallet/surface-map.md} + {@code design/orchestration/http-public.yaml}.
 */
public final class OpenApiWalletMapper {

    private OpenApiWalletMapper() {
    }

    // --- Query: GET /wallets/balance → WalletQueryService.getBalance ---

    public static WalletType toDomain(WalletTypeWire wire) {
        return WalletType.valueOf(wire.name());
    }

    public static WalletTypeWire toWire(WalletType domain) {
        if (domain == WalletType.PARTNER) {
            throw new ValidationException("PARTNER wallet is not exposed on S1 WalletType enum");
        }
        return WalletTypeWire.valueOf(domain.name());
    }

    public static WalletBalanceDataWire toWalletBalanceData(BalanceView view) {
        return new WalletBalanceDataWire(
                view.memberId(),
                toWire(view.walletType()),
                view.currency(),
                formatMoney(view.available()),
                formatMoney(view.frozen()));
    }

    // --- Payment: POST /payments → orchestration → wallet debit + credit ---

    public static WalletMutationCommand toPaymentDebitCommand(PaymentRequestWire req) {
        return new WalletMutationCommand(
                req.memberId(),
                WalletType.USER,
                req.currency(),
                MoneyUtil.parseAmount(req.amount()),
                req.businessRef(),
                WalletTxType.PAYMENT_DEBIT,
                null,
                "PAYMENT",
                null);
    }

    public static WalletMutationCommand toPaymentCreditCommand(PaymentRequestWire req) {
        String net = req.netToMerchant() != null && !req.netToMerchant().isBlank()
                ? req.netToMerchant()
                : req.amount();
        return new WalletMutationCommand(
                req.merchantId(),
                WalletType.MERCHANT,
                req.currency(),
                MoneyUtil.parseAmount(net),
                req.businessRef(),
                WalletTxType.PAYMENT_CREDIT,
                null,
                "PAYMENT",
                null);
    }

    // --- Transfer: POST /transfers ---

    /** Gross debit from sender (principal + fee when orchestration passes gross in amount). */
    public static WalletMutationCommand toTransferDebitCommand(TransferRequestWire req) {
        return new WalletMutationCommand(
                req.fromMemberId(),
                WalletType.USER,
                req.currency(),
                MoneyUtil.parseAmount(req.amount()),
                req.businessRef(),
                WalletTxType.TRANSFER_DEBIT,
                null,
                "TRANSFER",
                null);
    }

    /** Net credit to recipient — orchestration must pass net in amount or compute before call. */
    public static WalletMutationCommand toTransferCreditCommand(
            TransferRequestWire req, String netAmount) {
        return new WalletMutationCommand(
                req.toMemberId(),
                WalletType.USER,
                req.currency(),
                MoneyUtil.parseAmount(netAmount),
                req.businessRef(),
                WalletTxType.TRANSFER_CREDIT,
                null,
                "TRANSFER",
                null);
    }

    // --- Withdraw: POST /withdrawals → WITHDRAW_FREEZE (v1 ADR-007) ---

    /** Call {@link com.gtelpay.core.wallet.validation.WithdrawalRequestValidator} before mapping. */
    public static WalletMutationCommand toWithdrawFreezeCommand(WithdrawalRequestWire req) {
        return new WalletMutationCommand(
                req.memberId(),
                WalletType.USER,
                req.currency(),
                MoneyUtil.parseAmount(req.amount()),
                req.businessRef(),
                WalletTxType.WITHDRAW_FREEZE,
                null,
                "WITHDRAW",
                null);
    }

    // --- Deposit async: wallet leg after ledger POSTED ---

    public static WalletMutationCommand toDepositCreditCommand(
            long memberId,
            String currency,
            String amount,
            String businessRef,
            Long coaTransId) {
        return new WalletMutationCommand(
                memberId,
                WalletType.USER,
                currency,
                MoneyUtil.parseAmount(amount),
                businessRef,
                WalletTxType.DEPOSIT_CREDIT,
                coaTransId,
                "DEPOSIT",
                null);
    }

    public static long walletTxIdForOpenApi(WalletTxResult result) {
        return result.walletTxId();
    }

    public static String formatMoney(BigDecimal amount) {
        if (amount == null || amount.signum() == 0) {
            return "0.0000";
        }
        return MoneyUtil.normalize(amount).toPlainString();
    }
}
