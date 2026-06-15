package com.gtelpay.core.accounting.service.impl;

import com.gtelpay.core.accounting.domain.CoaTransDataEntity;
import com.gtelpay.core.accounting.domain.CoaTransEntity;
import com.gtelpay.core.accounting.domain.JournalStatus;
import com.gtelpay.core.accounting.domain.LineSide;
import com.gtelpay.core.accounting.repository.CoaAccountRepository;
import com.gtelpay.core.accounting.repository.CoaTransDataRepository;
import com.gtelpay.core.accounting.repository.CoaTransRepository;
import com.gtelpay.core.accounting.service.CreateJournalCommand;
import com.gtelpay.core.accounting.service.JournalHeader;
import com.gtelpay.core.accounting.service.JournalLineCommand;
import com.gtelpay.core.accounting.service.JournalService;
import com.gtelpay.core.accounting.service.PostJournalResult;
import com.gtelpay.core.accounting.service.ReverseJournalCommand;
import com.gtelpay.core.accounting.validation.CreateJournalCommandValidator;
import com.gtelpay.core.accounting.validation.JournalLineCommandValidator;
import com.gtelpay.core.foundation.exception.AccountingException;
import com.gtelpay.core.foundation.exception.ErrorCode;
import com.gtelpay.core.foundation.exception.ValidationException;
import com.gtelpay.core.foundation.util.MoneyUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class JournalServiceImpl implements JournalService {

    private static final String USE_CASE_DEPOSIT = "DEPOSIT";
    private static final String ACCOUNT_TRANSIT_DEPOSIT = "3100";
    private static final String ACCOUNT_BANK = "1111";
    private static final String ACCOUNT_USER_LIABILITY = "2110";
    private static final String ACCOUNT_DEPOSIT_FEE_REVENUE = "4110";

    private final CoaTransRepository coaTransRepository;
    private final CoaTransDataRepository coaTransDataRepository;
    private final CoaAccountRepository coaAccountRepository;

    public JournalServiceImpl(
            CoaTransRepository coaTransRepository,
            CoaTransDataRepository coaTransDataRepository,
            CoaAccountRepository coaAccountRepository) {
        this.coaTransRepository = coaTransRepository;
        this.coaTransDataRepository = coaTransDataRepository;
        this.coaAccountRepository = coaAccountRepository;
    }

    @Override
    @Transactional
    public JournalHeader createJournal(CreateJournalCommand cmd) {
        CreateJournalCommandValidator.validate(cmd);
        return coaTransRepository.findByReferenceIdAndUseCase(cmd.referenceId(), cmd.useCase())
                .map(this::toHeader)
                .orElseGet(() -> toHeader(insertJournal(cmd)));
    }

    @Override
    @Transactional
    public void addLines(long coaTransId, List<JournalLineCommand> lines) {
        CoaTransEntity journal = requireMutableJournal(coaTransId);
        JournalLineCommandValidator.requireNonEmpty(lines);
        for (JournalLineCommand line : lines) {
            persistLine(journal.getId(), line);
        }
    }

    @Override
    @Transactional
    public PostJournalResult postJournal(long coaTransId) {
        CoaTransEntity journal = requireJournal(coaTransId);
        if (journal.getStatus() == JournalStatus.POSTED) {
            return new PostJournalResult(
                    journal.getId(), journal.getStatus(), journal.getPostedAt(), true);
        }
        if (journal.getStatus() != JournalStatus.PENDING) {
            throw new AccountingException(
                    ErrorCode.ACCOUNTING_JOURNAL_NOT_FOUND, "journal not postable: " + journal.getStatus());
        }
        List<CoaTransDataEntity> lines = coaTransDataRepository.findByCoaTransId(coaTransId);
        JournalBalanceValidator.assertBalanced(lines);
        journal.setStatus(JournalStatus.POSTED);
        journal.setPostedAt(Instant.now());
        coaTransRepository.save(journal);
        return new PostJournalResult(journal.getId(), journal.getStatus(), journal.getPostedAt(), false);
    }

    @Override
    @Transactional
    public PostJournalResult confirmDeposit(long coaTransId, BigDecimal fee) {
        CoaTransEntity journal = requireJournal(coaTransId);
        if (journal.getStatus() == JournalStatus.POSTED) {
            return new PostJournalResult(
                    journal.getId(), journal.getStatus(), journal.getPostedAt(), true);
        }
        if (!USE_CASE_DEPOSIT.equals(journal.getUseCase())) {
            throw new ValidationException("confirmDeposit only for use_case DEPOSIT");
        }
        if (journal.getStatus() != JournalStatus.PENDING) {
            throw new AccountingException(
                    ErrorCode.ACCOUNTING_JOURNAL_NOT_FOUND, "deposit journal not PENDING");
        }

        BigDecimal normalizedFee = MoneyUtil.normalizeAllowZero(fee);

        BigDecimal gross = resolveDepositGross(coaTransId);
        BigDecimal net = MoneyUtil.normalize(gross.subtract(normalizedFee));
        if (net.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("fee exceeds gross deposit amount");
        }

        persistLine(coaTransId, line(ACCOUNT_TRANSIT_DEPOSIT, gross, LineSide.DEBIT));
        persistLine(coaTransId, line(ACCOUNT_USER_LIABILITY, net, LineSide.CREDIT));
        if (normalizedFee.compareTo(BigDecimal.ZERO) > 0) {
            persistLine(coaTransId, line(ACCOUNT_DEPOSIT_FEE_REVENUE, normalizedFee, LineSide.CREDIT));
        }

        List<CoaTransDataEntity> allLines = coaTransDataRepository.findByCoaTransId(coaTransId);
        JournalBalanceValidator.assertBalanced(allLines);
        JournalBalanceValidator.assertTransitZero(ACCOUNT_TRANSIT_DEPOSIT, allLines);

        journal.setStatus(JournalStatus.POSTED);
        journal.setPostedAt(Instant.now());
        coaTransRepository.save(journal);
        return new PostJournalResult(journal.getId(), journal.getStatus(), journal.getPostedAt(), false);
    }

    @Override
    @Transactional
    public JournalHeader reverseJournal(long coaTransId, ReverseJournalCommand cmd) {
        CoaTransEntity original = requireJournal(coaTransId);
        if (original.getStatus() != JournalStatus.POSTED) {
            throw new AccountingException(
                    ErrorCode.ACCOUNTING_JOURNAL_NOT_FOUND, "only POSTED journals can be reversed");
        }
        String reversalRef = cmd.referenceId() != null && !cmd.referenceId().isBlank()
                ? cmd.referenceId()
                : original.getReferenceId() + ":rev";
        CreateJournalCommand create = new CreateJournalCommand(
                reversalRef,
                original.getUseCase() + "_REVERSAL",
                cmd.reason(),
                original.getPostingDate());
        JournalHeader reversalHeader = createJournal(create);

        List<CoaTransDataEntity> originalLines = coaTransDataRepository.findByCoaTransId(coaTransId);
        List<JournalLineCommand> reversed = new ArrayList<>();
        for (CoaTransDataEntity line : originalLines) {
            LineSide opposite = line.getSide() == LineSide.DEBIT ? LineSide.CREDIT : LineSide.DEBIT;
            reversed.add(new JournalLineCommand(
                    line.getAccountCode(), line.getAmount(), opposite, line.getCurrency()));
        }
        addLines(reversalHeader.id(), reversed);
        postJournal(reversalHeader.id());

        original.setStatus(JournalStatus.REVERSED);
        coaTransRepository.save(original);
        return reversalHeader;
    }

    private CoaTransEntity insertJournal(CreateJournalCommand cmd) {
        CoaTransEntity entity = new CoaTransEntity();
        entity.setReferenceId(cmd.referenceId());
        entity.setUseCase(cmd.useCase());
        entity.setDescription(cmd.description());
        entity.setPostingDate(cmd.postingDate() != null ? cmd.postingDate() : LocalDate.now());
        entity.setStatus(JournalStatus.PENDING);
        return coaTransRepository.save(entity);
    }

    private void persistLine(long coaTransId, JournalLineCommand line) {
        JournalLineCommandValidator.validateLine(line);
        if (!coaAccountRepository.existsById(line.accountCode())) {
            throw new AccountingException(
                    ErrorCode.ACCOUNTING_JOURNAL_NOT_FOUND, "unknown account: " + line.accountCode());
        }
        BigDecimal amount = MoneyUtil.normalize(line.amount());
        CoaTransDataEntity entity = new CoaTransDataEntity();
        entity.setCoaTransId(coaTransId);
        entity.setAccountCode(line.accountCode());
        entity.setSide(line.side());
        entity.setAmount(amount);
        entity.setCurrency(line.currency() != null ? line.currency() : "VND");
        coaTransDataRepository.save(entity);
    }

    private BigDecimal resolveDepositGross(long coaTransId) {
        List<CoaTransDataEntity> lines = coaTransDataRepository.findByCoaTransId(coaTransId);
        for (CoaTransDataEntity line : lines) {
            if (ACCOUNT_BANK.equals(line.getAccountCode()) && line.getSide() == LineSide.DEBIT) {
                return line.getAmount();
            }
        }
        for (CoaTransDataEntity line : lines) {
            if (ACCOUNT_TRANSIT_DEPOSIT.equals(line.getAccountCode()) && line.getSide() == LineSide.CREDIT) {
                return line.getAmount();
            }
        }
        throw new AccountingException(
                ErrorCode.ACCOUNTING_UNBALANCED_JOURNAL, "deposit phase A missing gross amount");
    }

    private CoaTransEntity requireJournal(long coaTransId) {
        return coaTransRepository.findById(coaTransId)
                .orElseThrow(() -> new AccountingException(
                        ErrorCode.ACCOUNTING_JOURNAL_NOT_FOUND, "journal not found: " + coaTransId));
    }

    private CoaTransEntity requireMutableJournal(long coaTransId) {
        CoaTransEntity journal = requireJournal(coaTransId);
        if (journal.getStatus() != JournalStatus.PENDING) {
            throw new AccountingException(
                    ErrorCode.ACCOUNTING_JOURNAL_NOT_FOUND, "cannot add lines to " + journal.getStatus());
        }
        return journal;
    }

    private JournalLineCommand line(String account, BigDecimal amount, LineSide side) {
        return new JournalLineCommand(account, amount, side, "VND");
    }

    private JournalHeader toHeader(CoaTransEntity entity) {
        return new JournalHeader(
                entity.getId(),
                entity.getReferenceId(),
                entity.getUseCase(),
                entity.getStatus(),
                entity.getPostingDate());
    }
}
