package com.gtelpay.app.orchestration.deposit;

public record DepositNotification(
        String virtualAccount,
        String grossAmount,
        String bankRef,
        String businessRef,
        String currency,
        String notifiedAt
) {}
