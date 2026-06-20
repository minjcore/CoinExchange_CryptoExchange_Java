package com.gtelpay.app.orchestration.usecase;

import com.gtelpay.app.orchestration.gateway.LedgerGateway;
import com.gtelpay.app.orchestration.gateway.WalletGateway;
import com.gtelpay.core.wallet.api.dto.IbftRequestWire;
import com.gtelpay.core.wallet.domain.WalletTxType;
import com.gtelpay.core.wallet.domain.WalletType;
import com.gtelpay.core.wallet.service.WalletMutationCommand;
import com.gtelpay.core.wallet.service.WalletTxResult;
import com.gtelpay.core.wallet.validation.IbftRequestValidator;
import org.springframework.stereotype.Service;

/**
 * IBFT accept phase — sync before 200 (ADR-007, spec/006-ibft/plan.md).
 * TX 1: IBFT_FREEZE — moves gross from available to frozen.
 * TX 2: createPendingIbft — PENDING journal + Phase A lines (2110 DR / 3400 CR).
 */
@Service
public class IbftUseCase {

    private final WalletGateway wallet;
    private final LedgerGateway ledger;

    public IbftUseCase(WalletGateway wallet, LedgerGateway ledger) {
        this.wallet = wallet;
        this.ledger = ledger;
    }

    public IbftResult execute(IbftRequestWire req, String idempotencyKey) {
        var v = IbftRequestValidator.validate(req, idempotencyKey);

        wallet.provisionIfAbsent(v.memberId(), WalletType.USER, v.currency());

        // TX 1: freeze gross (available -= gross, frozen += gross)
        WalletTxResult freeze = wallet.freeze(new WalletMutationCommand(
                v.memberId(), WalletType.USER, v.currency(), v.gross(),
                v.businessRef(), WalletTxType.IBFT_FREEZE, null, "IBFT", null));

        // TX 2: create PENDING journal with Phase A lines
        var journal = ledger.createPendingIbft(v.businessRef(), v.gross(), v.currency());

        return new IbftResult(
                v.businessRef(), "ACCEPTED",
                freeze.walletId(), journal.id(),
                freeze.available(), freeze.frozen());
    }
}
