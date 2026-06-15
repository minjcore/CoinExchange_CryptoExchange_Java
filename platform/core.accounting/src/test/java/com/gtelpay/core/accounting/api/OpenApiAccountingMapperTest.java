package com.gtelpay.core.accounting.api;

import com.gtelpay.core.accounting.api.dto.CreateJournalRequestWire;
import com.gtelpay.core.accounting.api.dto.JournalLineWire;
import com.gtelpay.core.accounting.domain.JournalStatus;
import com.gtelpay.core.accounting.domain.LineSide;
import com.gtelpay.core.accounting.service.CreateJournalCommand;
import com.gtelpay.core.accounting.service.JournalHeader;
import com.gtelpay.core.accounting.service.JournalLineCommand;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OpenApiAccountingMapperTest {

    @Test
    void createJournalRequest_mapsToCommand() {
        CreateJournalRequestWire wire = new CreateJournalRequestWire(
                "dep-1", "DEPOSIT", "test", LocalDate.of(2026, 6, 8));

        CreateJournalCommand cmd = OpenApiAccountingMapper.toCreateJournalCommand(wire);

        assertEquals("dep-1", cmd.referenceId());
        assertEquals("DEPOSIT", cmd.useCase());
        assertEquals(LocalDate.of(2026, 6, 8), cmd.postingDate());
    }

    @Test
    void journalLine_mapsToCommand() {
        JournalLineWire wire = new JournalLineWire("2110", "100000.0000", "DEBIT", "VND");

        JournalLineCommand cmd = OpenApiAccountingMapper.toJournalLineCommand(wire);

        assertEquals("2110", cmd.accountCode());
        assertEquals(LineSide.DEBIT, cmd.side());
        assertEquals(new BigDecimal("100000.0000"), cmd.amount());
    }

    @Test
    void journalHeader_mapsToWire() {
        JournalHeader header = new JournalHeader(
                42L, "ref-1", "PAYMENT", JournalStatus.POSTED, LocalDate.now());

        var wire = OpenApiAccountingMapper.toJournalHeader(header);

        assertEquals(42L, wire.id());
        assertEquals("ref-1", wire.reference_id());
        assertEquals("POSTED", wire.status());
    }
}
