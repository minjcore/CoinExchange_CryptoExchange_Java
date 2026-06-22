package com.gtelpay.core.wallet.service;

import com.gtelpay.core.sharedlib.page.PageResult;
import com.gtelpay.core.sharedlib.request.PageRequest;
import com.gtelpay.core.wallet.domain.WalletType;

public interface WalletQueryService {

    BalanceView getBalance(long memberId, WalletType walletType, String currency);

    PageResult<WalletTxView> listTx(WalletTxQuery query, PageRequest page);
}
