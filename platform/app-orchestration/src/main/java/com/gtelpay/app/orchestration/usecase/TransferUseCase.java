package com.gtelpay.app.orchestration.usecase;

import com.gtelpay.app.orchestration.gateway.LedgerGateway;
import com.gtelpay.app.orchestration.gateway.WalletGateway;
import com.gtelpay.core.accounting.service.CreateJournalCommand;
import com.gtelpay.core.accounting.validation.TransferPostingValidator;
import com.gtelpay.core.sharedlib.exception.ValidationException;
import com.gtelpay.core.sharedlib.util.MoneyUtil;
import com.gtelpay.core.wallet.api.OpenApiWalletMapper;
import com.gtelpay.core.wallet.api.dto.TransferRequestWire;
import com.gtelpay.core.wallet.domain.WalletType;
import com.gtelpay.core.wallet.service.WalletTxResult;
import com.gtelpay.core.wallet.validation.BusinessRefValidator;
import com.gtelpay.core.wallet.validation.CurrencyValidator;
import com.gtelpay.core.wallet.validation.S1IdempotencyValidator;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Internal transfer A→B — sync 3-commit (spec/use-cases.md UC-4, foundation §10, transit 3300).
 * Orchestration owns the fee split (ADR-009): net = gross − fee.
 */
@Service
public class TransferUseCase {

    private final WalletGateway wallet;
    private final LedgerGateway ledger;

    public TransferUseCase(WalletGateway wallet, LedgerGateway ledger) {
        this.wallet = wallet;
        this.ledger = ledger;
    }

    public TransferResult execute(TransferRequestWire req, String idempotencyKey) {
        String businessRef = BusinessRefValidator.normalize(req.businessRef());
        S1IdempotencyValidator.requireHeaderMatchesBody(businessRef, idempotencyKey);
        if (req.fromMemberId() <= 0 || req.toMemberId() <= 0) {
            throw new ValidationException("fromMemberId and toMemberId must be positive");
        }
        if (req.fromMemberId() == req.toMemberId()) {
            throw new ValidationException("fromMemberId and toMemberId must differ");
        }
        String currency = CurrencyValidator.normalize(req.currency());
        BigDecimal gross = MoneyUtil.parseAmount(req.amount());
        BigDecimal fee = req.feeAmount() != null && !req.feeAmount().isBlank()
                ? MoneyUtil.parseAmount(req.feeAmount())
                : BigDecimal.ZERO;
        BigDecimal net = gross.subtract(fee);
        if (net.signum() <= 0) {
            throw new ValidationException("net (gross − fee) must be positive");
        }
        String netAmount = OpenApiWalletMapper.formatMoney(net);

        wallet.provisionIfAbsent(req.fromMemberId(), WalletType.USER, currency);
        wallet.provisionIfAbsent(req.toMemberId(), WalletType.USER, currency);

        // TX 1: debit sender
        WalletTxResult debit = wallet.debit(OpenApiWalletMapper.toTransferDebitCommand(req));

        // TX 2: post journal
        var posted = ledger.createAndPost(
                new CreateJournalCommand(businessRef, "TRANSFER", "internal transfer", null),
                TransferPostingValidator.buildV1Lines(req.amount(), netAmount, currency));

        // TX 3: credit receiver
        wallet.credit(OpenApiWalletMapper.toTransferCreditCommand(req, netAmount).withCoaTransId(posted.id()));

        return new TransferResult(businessRef, OpenApiWalletMapper.walletTxIdForOpenApi(debit), posted.id(), "SUCCESS");
    }
}
