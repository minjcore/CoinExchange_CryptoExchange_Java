package com.gtelpay.core.accounting.service;

import java.math.BigDecimal;
import java.util.List;

public interface JournalService {

    JournalHeader createJournal(CreateJournalCommand cmd);

    void addLines(long coaTransId, List<JournalLineCommand> lines);

    PostJournalResult postJournal(long coaTransId);

    PostJournalResult confirmDeposit(long coaTransId, BigDecimal fee);

    JournalHeader reverseJournal(long coaTransId, ReverseJournalCommand cmd);
}
