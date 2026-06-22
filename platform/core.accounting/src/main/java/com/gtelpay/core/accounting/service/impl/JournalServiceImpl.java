package com.gtelpay.core.accounting.service.impl;

import com.gtelpay.core.accounting.domain.CoaTransDataEntity;
import com.gtelpay.core.accounting.domain.CoaTransEntity;
import com.gtelpay.core.accounting.domain.JournalStatus;
import com.gtelpay.core.accounting.domain.LineSide;
import com.gtelpay.core.accounting.domain.PeriodStatus;
import com.gtelpay.core.accounting.repository.CoaAccountRepository;
import com.gtelpay.core.accounting.repository.CoaPeriodRepository;
import com.gtelpay.core.accounting.repository.CoaTransDataRepository;
import com.gtelpay.core.accounting.repository.CoaTransRepository;
import com.gtelpay.core.accounting.service.CreateJournalCommand;
import com.gtelpay.core.accounting.service.JournalHeader;
import com.gtelpay.core.accounting.service.JournalLineCommand;
import com.gtelpay.core.accounting.service.JournalService;
import com.gtelpay.core.accounting.service.PostJournalResult;
import com.gtelpay.core.accounting.service.ReverseJournalCommand;
import com.gtelpay.core.accounting.validation.IbftPostingValidator;
import com.gtelpay.core.accounting.validation.WithdrawPostingValidator;
import com.gtelpay.core.accounting.validation.CoaAccountValidator;
import com.gtelpay.core.accounting.validation.CreateJournalCommandValidator;
import com.gtelpay.core.accounting.validation.JournalLineCommandValidator;
import com.gtelpay.core.sharedlib.exception.AccountingException;
import com.gtelpay.core.sharedlib.exception.ErrorCode;
import com.gtelpay.core.sharedlib.exception.ValidationException;
import com.gtelpay.core.sharedlib.util.MoneyUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class JournalServiceImpl implements JournalService {

    // Period status rarely changes (once per month at most). Cache OPEN periods to avoid
    // a SELECT coa_period on every postJournal call. Cache is invalidated by evicting the
    // entry when a CLOSED/LOCKED period is detected — forcing a re-check on next call.
    private final ConcurrentHashMap<String, Boolean> openPeriodCache = new ConcurrentHashMap<>();

    private static final String USE_CASE_DEPOSIT = "DEPOSIT";
    private static final String USE_CASE_WITHDRAW = "WITHDRAW";
    private static final String USE_CASE_IBFT = "IBFT";
    private static final String ACCOUNT_TRANSIT_DEPOSIT = "3100";
    private static final String ACCOUNT_TRANSIT_WITHDRAW = "3200";
    private static final String ACCOUNT_TRANSIT_IBFT = "3400";
    private static final String ACCOUNT_BANK = "1111";
    private static final String ACCOUNT_NAPAS_CLEARING = "1112";
    private static final String ACCOUNT_USER_LIABILITY = "2110";
    private static final String ACCOUNT_DEPOSIT_FEE_REVENUE = "4110";
    private static final String ACCOUNT_WITHDRAW_FEE_REVENUE = "4120";
    private static final String ACCOUNT_IBFT_FEE_REVENUE = "4130";
    private static final String ACCOUNT_NAPAS_COST_EXPENSE = "5100";

    private final CoaTransRepository coaTransRepository;
    private final CoaTransDataRepository coaTransDataRepository;
    private final CoaAccountRepository coaAccountRepository;
    private final CoaAccountValidator coaAccountValidator;
    private final CoaPeriodRepository coaPeriodRepository;

    public JournalServiceImpl(
            CoaTransRepository coaTransRepository,
            CoaTransDataRepository coaTransDataRepository,
            CoaAccountRepository coaAccountRepository,
            CoaAccountValidator coaAccountValidator,
            CoaPeriodRepository coaPeriodRepository) {
        this.coaTransRepository = coaTransRepository;
        this.coaTransDataRepository = coaTransDataRepository;
        this.coaAccountRepository = coaAccountRepository;
        this.coaAccountValidator = coaAccountValidator;
        this.coaPeriodRepository = coaPeriodRepository;
    }

    // ADR-023: posting is prohibited into a CLOSED or LOCKED period. Period = "yyyy-MM" of the
    // journal's posting_date. No row → OPEN by default.
    private void assertPeriodOpen(LocalDate postingDate) {
        LocalDate date = postingDate != null ? postingDate : LocalDate.now();
        String periodCode = String.format("%04d-%02d", date.getYear(), date.getMonthValue());
        if (openPeriodCache.containsKey(periodCode)) return;
        coaPeriodRepository.findById(periodCode).ifPresent(period -> {
            if (period.getStatus() != PeriodStatus.OPEN) {
                openPeriodCache.remove(periodCode);
                throw new AccountingException(
                        ErrorCode.ACCOUNTING_PERIOD_CLOSED,
                        "accounting period " + periodCode + " is " + period.getStatus());
            }
        });
        openPeriodCache.put(periodCode, Boolean.TRUE);
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
        assertPeriodOpen(journal.getPostingDate());
        List<CoaTransDataEntity> lines = coaTransDataRepository.findByCoaTransId(coaTransId);
        JournalBalanceValidator.assertBalanced(lines);
        // ADR-010: transit net-zero for EVERY use case at post time (not just deposit) — no
        // payment/transfer/withdraw/IBFT can POST with funds stranded in a 3xxx transit.
        JournalBalanceValidator.assertAllTransitZero(lines);
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

        assertPeriodOpen(journal.getPostingDate());

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

    @Override
    @Transactional
    public JournalHeader createPendingWithdraw(String businessRef, BigDecimal gross, String currency) {
        CoaTransEntity existing = coaTransRepository.findByReferenceIdAndUseCase(businessRef, USE_CASE_WITHDRAW).orElse(null);
        if (existing != null) {
            return toHeader(existing);
        }
        CoaTransEntity journal = insertJournal(
                new CreateJournalCommand(businessRef, USE_CASE_WITHDRAW, "withdraw accept - phase A", null));
        String grossStr = MoneyUtil.normalize(gross).toPlainString();
        for (JournalLineCommand line : WithdrawPostingValidator.acceptLines(grossStr, currency)) {
            persistLine(journal.getId(), line);
        }
        return toHeader(journal);
    }

    @Override
    @Transactional
    public PostJournalResult confirmWithdraw(long coaTransId, BigDecimal principal, BigDecimal fee) {
        CoaTransEntity journal = requireJournal(coaTransId);
        if (journal.getStatus() == JournalStatus.POSTED) {
            return new PostJournalResult(journal.getId(), journal.getStatus(), journal.getPostedAt(), true);
        }
        if (!USE_CASE_WITHDRAW.equals(journal.getUseCase())) {
            throw new com.gtelpay.core.sharedlib.exception.ValidationException("confirmWithdraw only for use_case WITHDRAW");
        }
        if (journal.getStatus() != JournalStatus.PENDING) {
            throw new AccountingException(ErrorCode.ACCOUNTING_JOURNAL_NOT_FOUND, "withdraw journal not PENDING");
        }
        assertPeriodOpen(journal.getPostingDate());

        BigDecimal normalizedPrincipal = MoneyUtil.normalize(principal);
        BigDecimal normalizedFee = MoneyUtil.normalizeAllowZero(fee);
        BigDecimal gross = normalizedPrincipal.add(normalizedFee);

        persistLine(coaTransId, line(ACCOUNT_TRANSIT_WITHDRAW, gross, LineSide.DEBIT));
        persistLine(coaTransId, line(ACCOUNT_BANK, normalizedPrincipal, LineSide.CREDIT));
        if (normalizedFee.compareTo(BigDecimal.ZERO) > 0) {
            persistLine(coaTransId, line(ACCOUNT_WITHDRAW_FEE_REVENUE, normalizedFee, LineSide.CREDIT));
        }

        List<CoaTransDataEntity> allLines = coaTransDataRepository.findByCoaTransId(coaTransId);
        JournalBalanceValidator.assertBalanced(allLines);
        JournalBalanceValidator.assertTransitZero(ACCOUNT_TRANSIT_WITHDRAW, allLines);

        journal.setStatus(JournalStatus.POSTED);
        journal.setPostedAt(Instant.now());
        coaTransRepository.save(journal);
        return new PostJournalResult(journal.getId(), journal.getStatus(), journal.getPostedAt(), false);
    }

    @Override
    @Transactional
    public void voidWithdraw(long coaTransId) {
        CoaTransEntity journal = requireJournal(coaTransId);
        if (journal.getStatus() == JournalStatus.FAILED) {
            return;
        }
        if (!USE_CASE_WITHDRAW.equals(journal.getUseCase())) {
            throw new com.gtelpay.core.sharedlib.exception.ValidationException("voidWithdraw only for use_case WITHDRAW");
        }
        if (journal.getStatus() != JournalStatus.PENDING) {
            throw new AccountingException(ErrorCode.ACCOUNTING_JOURNAL_NOT_FOUND,
                    "can only void a PENDING withdraw journal, current: " + journal.getStatus());
        }
        journal.setStatus(JournalStatus.FAILED);
        coaTransRepository.save(journal);
    }

    @Override
    @Transactional
    public JournalHeader createPendingIbft(String businessRef, BigDecimal gross, String currency) {
        CoaTransEntity existing = coaTransRepository.findByReferenceIdAndUseCase(businessRef, USE_CASE_IBFT).orElse(null);
        if (existing != null) {
            return toHeader(existing);
        }
        CoaTransEntity journal = insertJournal(
                new CreateJournalCommand(businessRef, USE_CASE_IBFT, "IBFT accept - phase A", null));
        String grossStr = MoneyUtil.normalize(gross).toPlainString();
        for (JournalLineCommand line : IbftPostingValidator.acceptLines(grossStr, currency)) {
            persistLine(journal.getId(), line);
        }
        return toHeader(journal);
    }

    @Override
    @Transactional
    public PostJournalResult confirmIbft(long coaTransId, BigDecimal principal, BigDecimal platformFee, BigDecimal napasCost) {
        CoaTransEntity journal = requireJournal(coaTransId);
        if (journal.getStatus() == JournalStatus.POSTED) {
            return new PostJournalResult(journal.getId(), journal.getStatus(), journal.getPostedAt(), true);
        }
        if (!USE_CASE_IBFT.equals(journal.getUseCase())) {
            throw new com.gtelpay.core.sharedlib.exception.ValidationException("confirmIbft only for use_case IBFT");
        }
        if (journal.getStatus() != JournalStatus.PENDING) {
            throw new AccountingException(ErrorCode.ACCOUNTING_JOURNAL_NOT_FOUND, "IBFT journal not PENDING");
        }
        assertPeriodOpen(journal.getPostingDate());

        BigDecimal normalizedPrincipal = MoneyUtil.normalize(principal);
        BigDecimal normalizedFee = MoneyUtil.normalizeAllowZero(platformFee);
        BigDecimal normalizedNapasCost = MoneyUtil.normalizeAllowZero(napasCost);
        BigDecimal gross = normalizedPrincipal.add(normalizedFee);

        // Phase B: clear transit 3400 (gross = principal + platformFee)
        persistLine(coaTransId, line(ACCOUNT_TRANSIT_IBFT, gross, LineSide.DEBIT));
        persistLine(coaTransId, line(ACCOUNT_NAPAS_CLEARING, normalizedPrincipal, LineSide.CREDIT));
        if (normalizedFee.compareTo(BigDecimal.ZERO) > 0) {
            persistLine(coaTransId, line(ACCOUNT_IBFT_FEE_REVENUE, normalizedFee, LineSide.CREDIT));
        }
        // Napas cost leg: platform expense (independent of 3400 balance)
        if (normalizedNapasCost.compareTo(BigDecimal.ZERO) > 0) {
            persistLine(coaTransId, line(ACCOUNT_NAPAS_COST_EXPENSE, normalizedNapasCost, LineSide.DEBIT));
            persistLine(coaTransId, line(ACCOUNT_NAPAS_CLEARING, normalizedNapasCost, LineSide.CREDIT));
        }

        List<CoaTransDataEntity> allLines = coaTransDataRepository.findByCoaTransId(coaTransId);
        JournalBalanceValidator.assertBalanced(allLines);
        JournalBalanceValidator.assertTransitZero(ACCOUNT_TRANSIT_IBFT, allLines);

        journal.setStatus(JournalStatus.POSTED);
        journal.setPostedAt(Instant.now());
        coaTransRepository.save(journal);
        return new PostJournalResult(journal.getId(), journal.getStatus(), journal.getPostedAt(), false);
    }

    @Override
    @Transactional
    public void voidIbft(long coaTransId) {
        CoaTransEntity journal = requireJournal(coaTransId);
        if (journal.getStatus() == JournalStatus.FAILED) {
            return;
        }
        if (!USE_CASE_IBFT.equals(journal.getUseCase())) {
            throw new com.gtelpay.core.sharedlib.exception.ValidationException("voidIbft only for use_case IBFT");
        }
        if (journal.getStatus() != JournalStatus.PENDING) {
            throw new AccountingException(ErrorCode.ACCOUNTING_JOURNAL_NOT_FOUND,
                    "can only void a PENDING IBFT journal, current: " + journal.getStatus());
        }
        journal.setStatus(JournalStatus.FAILED);
        coaTransRepository.save(journal);
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
        if (!coaAccountValidator.exists(line.accountCode())) {
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
