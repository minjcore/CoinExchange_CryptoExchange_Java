package com.gtelpay.core.accounting.api.dto;

import java.util.List;

/** Wire: {@code components/schemas/AddJournalLinesRequest}. */
public record AddJournalLinesRequestWire(List<JournalLineWire> lines) {
}
