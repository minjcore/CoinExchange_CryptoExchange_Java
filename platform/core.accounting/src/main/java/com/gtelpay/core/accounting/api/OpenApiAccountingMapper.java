package com.gtelpay.core.accounting.api;

import com.gtelpay.core.accounting.api.dto.CreateJournalRequestWire;
import com.gtelpay.core.accounting.api.dto.JournalHeaderWire;
import com.gtelpay.core.accounting.api.dto.JournalLineWire;
import com.gtelpay.core.accounting.api.dto.PostJournalResultWire;
import com.gtelpay.core.accounting.domain.LineSide;
import com.gtelpay.core.accounting.service.CreateJournalCommand;
import com.gtelpay.core.accounting.service.JournalHeader;
import com.gtelpay.core.accounting.service.JournalLineCommand;
import com.gtelpay.core.accounting.service.PostJournalResult;
import com.gtelpay.core.sharedlib.util.MoneyUtil;

import java.math.BigDecimal;
import java.util.List;

/**
 * Maps S2 OpenAPI wire DTOs ↔ {@code core.accounting} domain commands/views.
 * Design source: {@code design/accounting/surface-map.md} + {@code design/accounting/api-internal.yaml}.
 */
public final class OpenApiAccountingMapper {

    private OpenApiAccountingMapper() {
    }

    public static CreateJournalCommand toCreateJournalCommand(CreateJournalRequestWire wire) {
        return new CreateJournalCommand(
                wire.reference_id(),
                wire.use_case(),
                wire.description(),
                wire.posting_date());
    }

    public static JournalHeaderWire toJournalHeader(JournalHeader header) {
        return new JournalHeaderWire(
                header.id(),
                header.referenceId(),
                header.status().name());
    }

    public static JournalLineCommand toJournalLineCommand(JournalLineWire wire) {
        return new JournalLineCommand(
                wire.account_code(),
                MoneyUtil.parseAmount(wire.amount()),
                LineSide.valueOf(wire.side()),
                wire.currency() != null ? wire.currency() : "VND");
    }

    public static List<JournalLineCommand> toJournalLineCommands(List<JournalLineWire> lines) {
        return lines.stream().map(OpenApiAccountingMapper::toJournalLineCommand).toList();
    }

    public static PostJournalResultWire toPostJournalResult(PostJournalResult result) {
        return new PostJournalResultWire(
                result.id(),
                result.status().name(),
                result.postedAt());
    }

    /** S1 businessRef → S2 reference_id (same string). */
    public static String toReferenceId(String businessRef) {
        return businessRef;
    }

    public static String formatMoney(BigDecimal amount) {
        return MoneyUtil.normalize(amount).toPlainString();
    }
}
