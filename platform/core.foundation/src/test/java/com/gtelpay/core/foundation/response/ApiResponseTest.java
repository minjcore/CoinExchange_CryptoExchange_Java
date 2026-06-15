package com.gtelpay.core.foundation.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gtelpay.core.foundation.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiResponseTest {

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test
    void okSerializesLikeOpenApiEnvelope() throws Exception {
        Instant ts = Instant.parse("2026-06-08T10:00:00Z");
        ApiResponse<Map<String, String>> response = ApiResponse.ok(Map.of("businessRef", "pay-1"), ts);

        String json = mapper.writeValueAsString(response);

        assertTrue(json.contains("\"code\":0"));
        assertTrue(json.contains("\"message\":\"OK\""));
        assertTrue(json.contains("\"businessRef\":\"pay-1\""));
        assertTrue(json.contains("2026-06-08T10:00:00Z"));
    }

    @Test
    void failUsesErrorApiCode() {
        ApiResponse<Void> response = ApiResponse.fail(ErrorCode.WALLET_DUPLICATE_CONFLICT, "duplicate");

        assertEquals(ErrorCode.WALLET_DUPLICATE_CONFLICT.apiCode(), response.code());
        assertEquals("duplicate", response.message());
        assertEquals(ErrorCode.WALLET_DUPLICATE_CONFLICT.suggestedHttpStatus(), 409);
    }
}
