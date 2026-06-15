package com.gtelpay.app.orchestration.web;

import com.gtelpay.core.foundation.exception.ValidationException;
import io.vertx.ext.web.RoutingContext;

/**
 * Dev auth stub — production: parse JWT {@code memberId} claim (Gateway forwards Bearer).
 */
public final class MemberIdResolver {

    private static final String HEADER_MEMBER_ID = "X-Member-Id";

    private MemberIdResolver() {
    }

    public static long requireMemberId(RoutingContext ctx) {
        String raw = ctx.request().getHeader(HEADER_MEMBER_ID);
        if (raw == null || raw.isBlank()) {
            throw new ValidationException("missing " + HEADER_MEMBER_ID + " (dev auth stub)");
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException ex) {
            throw new ValidationException("invalid " + HEADER_MEMBER_ID);
        }
    }
}
