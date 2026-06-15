package com.gtelpay.core.foundation.request;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PageRequestTest {

    @Test
    void acceptsValidPageAndSize() {
        PageRequest req = PageRequest.of(0, 20);
        assertEquals(0, req.page());
        assertEquals(20, req.size());
        assertEquals(0L, req.offset());
    }

    @Test
    void rejectsNegativePage() {
        assertThrows(IllegalArgumentException.class, () -> PageRequest.of(-1, 20));
    }

    @Test
    void rejectsOversize() {
        assertThrows(IllegalArgumentException.class, () -> PageRequest.of(0, 101));
        assertThrows(IllegalArgumentException.class, () -> PageRequest.of(0, 0));
    }

    @Test
    void offsetComputedFromPageAndSize() {
        assertEquals(40L, PageRequest.of(2, 20).offset());
    }
}
