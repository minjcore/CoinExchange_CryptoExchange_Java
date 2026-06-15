package com.gtelpay.core.accounting.api;

import com.gtelpay.core.accounting.service.AccountQueryService;
import com.gtelpay.core.accounting.service.JournalService;

/**
 * S2 {@code api-internal.yaml} operationId → {@code core.accounting} service entry points.
 */
public enum OpenApiAccountingBinding {

    CREATE_JOURNAL(
            "createJournal",
            "POST /journal-entries",
            JournalService.class,
            "createJournal"),

    ADD_JOURNAL_LINES(
            "addJournalLines",
            "POST /journal-entries/{id}/lines",
            JournalService.class,
            "addLines"),

    POST_JOURNAL(
            "postJournal",
            "POST /journal-entries/{id}/post",
            JournalService.class,
            "postJournal"),

    REVERSE_JOURNAL(
            "reverseJournal",
            "POST /journal-entries/{id}/reverse",
            JournalService.class,
            "reverseJournal"),

    GET_ACCOUNT_BALANCE(
            "getAccountBalance",
            "GET /accounts/{id}/balance",
            AccountQueryService.class,
            "getBalance"),

    CONFIRM_DEPOSIT_DOMAIN(
            "confirmDeposit",
            "domain-only (ADR-006)",
            JournalService.class,
            "confirmDeposit");

    private final String operationId;
    private final String httpPath;
    private final Class<?> serviceType;
    private final String serviceMethod;

    OpenApiAccountingBinding(
            String operationId,
            String httpPath,
            Class<?> serviceType,
            String serviceMethod) {
        this.operationId = operationId;
        this.httpPath = httpPath;
        this.serviceType = serviceType;
        this.serviceMethod = serviceMethod;
    }

    public String operationId() {
        return operationId;
    }

    public String httpPath() {
        return httpPath;
    }

    public Class<?> serviceType() {
        return serviceType;
    }

    public String serviceMethod() {
        return serviceMethod;
    }
}
