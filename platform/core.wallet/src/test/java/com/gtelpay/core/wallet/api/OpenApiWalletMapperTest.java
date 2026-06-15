package com.gtelpay.core.wallet.api;

import com.gtelpay.core.wallet.api.dto.PaymentRequestWire;
import com.gtelpay.core.wallet.api.dto.TransferRequestWire;
import com.gtelpay.core.wallet.api.dto.WalletBalanceDataWire;
import com.gtelpay.core.wallet.api.dto.WalletTypeWire;
import com.gtelpay.core.wallet.api.dto.WithdrawalRequestWire;
import com.gtelpay.core.wallet.domain.WalletTxType;
import com.gtelpay.core.wallet.domain.WalletType;
import com.gtelpay.core.wallet.service.BalanceView;
import com.gtelpay.core.wallet.service.WalletMutationCommand;
import com.gtelpay.core.wallet.validation.WithdrawalRequestValidator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OpenApiWalletMapperTest {

    @Test
    void walletBalanceData_matchesOpenApiShape() {
        BalanceView view = new BalanceView(
                100234L,
                WalletType.USER,
                "VND",
                com.gtelpay.core.wallet.domain.WalletStatus.ACTIVE,
                new BigDecimal("99000.0000"),
                new BigDecimal("1000.0000"));

        WalletBalanceDataWire wire = OpenApiWalletMapper.toWalletBalanceData(view);

        assertEquals(100234L, wire.memberId());
        assertEquals(WalletTypeWire.USER, wire.walletType());
        assertEquals("VND", wire.currency());
        assertEquals("99000.0000", wire.available());
        assertEquals("1000.0000", wire.frozen());
    }

    @Test
    void paymentRequest_mapsToDebitAndCreditCommands() {
        PaymentRequestWire req = new PaymentRequestWire(
                "pay-001", 1L, 99L, "100000.0000", "VND", "99000.0000");

        WalletMutationCommand debit = OpenApiWalletMapper.toPaymentDebitCommand(req);
        WalletMutationCommand credit = OpenApiWalletMapper.toPaymentCreditCommand(req);

        assertEquals(WalletTxType.PAYMENT_DEBIT, debit.txType());
        assertEquals(1L, debit.memberId());
        assertEquals(WalletType.USER, debit.walletType());
        assertEquals(new BigDecimal("100000.0000"), debit.amount());

        assertEquals(WalletTxType.PAYMENT_CREDIT, credit.txType());
        assertEquals(99L, credit.memberId());
        assertEquals(WalletType.MERCHANT, credit.walletType());
        assertEquals(new BigDecimal("99000.0000"), credit.amount());
        assertEquals("pay-001", debit.businessRef());
        assertEquals("pay-001", credit.businessRef());
    }

    @Test
    void transferRequest_mapsDebitCommand() {
        TransferRequestWire req = new TransferRequestWire(
                "tr-1", 10L, 20L, "50000.0000", "VND", "1000.0000");

        WalletMutationCommand debit = OpenApiWalletMapper.toTransferDebitCommand(req);

        assertEquals(WalletTxType.TRANSFER_DEBIT, debit.txType());
        assertEquals(10L, debit.memberId());
        assertEquals(new BigDecimal("50000.0000"), debit.amount());
    }

    @Test
    void withdrawalRequest_mapsFreezeCommand() {
        WithdrawalRequestWire req = new WithdrawalRequestWire(
                "wd-1", 5L, "101000.0000", "VND", true);
        WithdrawalRequestValidator.validate(req, "wd-1");

        WalletMutationCommand cmd = OpenApiWalletMapper.toWithdrawFreezeCommand(req);

        assertEquals(WalletTxType.WITHDRAW_FREEZE, cmd.txType());
        assertEquals("wd-1", cmd.businessRef());
        assertEquals(new BigDecimal("101000.0000"), cmd.amount());
    }
}
