package com.gtelpay.app.orchestration.usecase;

import com.gtelpay.app.orchestration.gateway.LedgerGateway;
import com.gtelpay.app.orchestration.gateway.WalletGateway;
import com.gtelpay.core.wallet.api.dto.WithdrawalRequestWire;
import com.gtelpay.core.wallet.domain.WalletTxType;
import com.gtelpay.core.wallet.domain.WalletType;
import com.gtelpay.core.wallet.service.WalletMutationCommand;
import com.gtelpay.core.wallet.service.WalletTxResult;
import com.gtelpay.core.wallet.validation.WithdrawalRequestValidator;
import org.springframework.stereotype.Service;

/**
 * Withdraw accept phase — sync before 200 (ADR-007, spec/003-withdraw/plan.md).
 * TX 1: WITHDRAW_FREEZE — moves gross from available to frozen.
 * TX 2: createPendingWithdraw — PENDING journal + Phase A lines (2110 DR / 3200 CR).
 */
@Service
public class WithdrawUseCase {

    private final WalletGateway wallet;
    private final LedgerGateway ledger;

    public WithdrawUseCase(WalletGateway wallet, LedgerGateway ledger) {
        this.wallet = wallet;
        this.ledger = ledger;
    }

    public WithdrawResult execute(WithdrawalRequestWire req, String idempotencyKey) {
        var v = WithdrawalRequestValidator.validate(req, idempotencyKey);

        wallet.provisionIfAbsent(v.memberId(), WalletType.USER, v.currency());

        // TX 1: freeze gross (available -= gross, frozen += gross)
        WalletTxResult freeze = wallet.freeze(new WalletMutationCommand(
                v.memberId(), WalletType.USER, v.currency(), v.amount(),
                v.businessRef(), WalletTxType.WITHDRAW_FREEZE, null, "WITHDRAW", null));

        // TX 2: create PENDING journal with Phase A lines
        var journal = ledger.createPendingWithdraw(v.businessRef(), v.amount(), v.currency());

        return new WithdrawResult(
                v.businessRef(), "ACCEPTED",
                freeze.walletId(), journal.id(),
                freeze.available(), freeze.frozen());
    }
}
