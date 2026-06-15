package com.gtelpay.core.foundation.page;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class PageResult<T> {

    private final List<T> content;
    private final long total;
    private final int page;
    private final int size;

    public PageResult(List<T> content, long total, int page, int size) {
        this.content = List.copyOf(content);
        this.total = total;
        this.page = page;
        this.size = size;
    }

    public static <T> PageResult<T> empty(int page, int size) {
        return new PageResult<>(Collections.emptyList(), 0L, page, size);
    }

    public List<T> content() {
        return content;
    }

    public long total() {
        return total;
    }

    public int page() {
        return page;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return content.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PageResult<?> that)) {
            return false;
        }
        return total == that.total
                && page == that.page
                && size == that.size
                && content.equals(that.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(content, total, page, size);
    }
}
