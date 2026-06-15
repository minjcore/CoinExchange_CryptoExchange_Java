package com.gtelpay.app.orchestration.usecase;

import com.gtelpay.app.orchestration.gateway.LedgerGateway;
import com.gtelpay.app.orchestration.gateway.WalletGateway;
import com.gtelpay.core.accounting.service.CreateJournalCommand;
import com.gtelpay.core.accounting.validation.PaymentPostingValidator;
import com.gtelpay.core.wallet.api.OpenApiWalletMapper;
import com.gtelpay.core.wallet.api.dto.PaymentRequestWire;
import com.gtelpay.core.wallet.domain.WalletType;
import com.gtelpay.core.wallet.service.WalletMutationCommand;
import com.gtelpay.core.wallet.service.WalletTxResult;
import com.gtelpay.core.wallet.validation.PaymentRequestValidator;
import org.springframework.stereotype.Service;

/**
 * Sync payment — {@code design/orchestration/flows.md} + {@code spec/implementation.md} §8.2.
 * Sequences both cores via gateways (wallet debit → ledger POSTED → wallet credit, ADR-027).
 */
@Service
public class PaymentUseCase {

    private final WalletGateway wallet;
    private final LedgerGateway ledger;

    public PaymentUseCase(WalletGateway wallet, LedgerGateway ledger) {
        this.wallet = wallet;
        this.ledger = ledger;
    }

    public PaymentResult execute(PaymentRequestWire req, String idempotencyKey) {
        PaymentRequestValidator.validate(req, idempotencyKey);

        wallet.provisionIfAbsent(req.memberId(), WalletType.USER, req.currency());
        wallet.provisionIfAbsent(req.merchantId(), WalletType.MERCHANT, req.currency());

        WalletTxResult debit = wallet.debit(OpenApiWalletMapper.toPaymentDebitCommand(req));

        var journal = ledger.createJournal(new CreateJournalCommand(
                req.businessRef(), "PAYMENT", "wallet payment", null));
        ledger.addLines(journal.id(), PaymentPostingValidator.buildV1Lines(
                req.amount(), req.netToMerchant(), req.currency()));
        var posted = ledger.postJournal(journal.id());

        WalletMutationCommand credit = OpenApiWalletMapper.toPaymentCreditCommand(req)
                .withCoaTransId(posted.id());
        WalletTxResult creditResult = wallet.credit(credit);

        return new PaymentResult(
                req.businessRef(),
                OpenApiWalletMapper.walletTxIdForOpenApi(debit),
                posted.id(),
                "SUCCESS");
    }
}
