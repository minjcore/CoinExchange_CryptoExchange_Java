package com.gtelpay.core.accounting.service;

public record ReverseJournalCommand(
        String referenceId,
        String reason) {
}
