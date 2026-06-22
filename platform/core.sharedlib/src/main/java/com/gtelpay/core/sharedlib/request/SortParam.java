package com.gtelpay.core.sharedlib.request;

import java.util.Objects;

public final class SortParam {

    private final String field;
    private final SortDirection direction;

    public SortParam(String field, SortDirection direction) {
        this.field = requireNonBlank(field, "field");
        this.direction = Objects.requireNonNull(direction, "direction");
    }

    public String field() {
        return field;
    }

    public SortDirection direction() {
        return direction;
    }

    private static String requireNonBlank(String value, String name) {
        Objects.requireNonNull(value, name);
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return trimmed;
    }
}
