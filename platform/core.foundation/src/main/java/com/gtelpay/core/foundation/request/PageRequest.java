package com.gtelpay.core.foundation.request;

import java.util.Objects;
import java.util.Optional;

/**
 * 0-based page index. Max page size capped at {@link #MAX_SIZE} per spec/implementation.md §5.
 */
public final class PageRequest {

    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    private final int page;
    private final int size;
    private final SortParam sort;

    public PageRequest(int page, int size) {
        this(page, size, null);
    }

    public PageRequest(int page, int size, SortParam sort) {
        if (page < 0) {
            throw new IllegalArgumentException("page must be >= 0");
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and " + MAX_SIZE);
        }
        this.page = page;
        this.size = size;
        this.sort = sort;
    }

    public static PageRequest of(int page, int size) {
        return new PageRequest(page, size);
    }

    public static PageRequest firstPage() {
        return new PageRequest(0, DEFAULT_SIZE);
    }

    public int page() {
        return page;
    }

    public int size() {
        return size;
    }

    public Optional<SortParam> sort() {
        return Optional.ofNullable(sort);
    }

    public long offset() {
        return (long) page * size;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PageRequest that)) {
            return false;
        }
        return page == that.page && size == that.size && Objects.equals(sort, that.sort);
    }

    @Override
    public int hashCode() {
        return Objects.hash(page, size, sort);
    }
}
