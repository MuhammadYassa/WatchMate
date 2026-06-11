package com.project.watchmate.common.error;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.reactive.function.client.WebClientResponseException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleTmdbClientException_usesExceptionStatusAndCode() {
        TmdbClientException exception = new TmdbClientException(
            "TMDB request failed.",
            HttpStatus.BAD_GATEWAY,
            "TMDB_CLIENT_ERROR",
            new RuntimeException("provider")
        );

        ResponseEntity<ApiError> response = handler.handleTmdbClientException(exception, request());

        assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
        assertEquals("TMDB request failed.", response.getBody().message());
        assertEquals("TMDB_CLIENT_ERROR", response.getBody().code());
    }

    @Test
    void handleWebClientResponse_whenLeakedRateLimit_returnsStableExternalUnavailable() {
        WebClientResponseException exception = WebClientResponseException.create(
            429,
            "Too Many Requests",
            org.springframework.http.HttpHeaders.EMPTY,
            "provider payload".getBytes(java.nio.charset.StandardCharsets.UTF_8),
            java.nio.charset.StandardCharsets.UTF_8
        );

        ResponseEntity<ApiError> response = handler.handleWebClientResponse(exception, request());

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals("EXTERNAL_SERVICE_UNAVAILABLE", response.getBody().code());
        assertEquals("External service is temporarily unavailable. Please try again shortly.", response.getBody().message());
    }

    @Test
    void handleWebClientResponse_whenLeakedProviderError_returnsStableBadGateway() {
        WebClientResponseException exception = WebClientResponseException.create(
            400,
            "Bad Request",
            org.springframework.http.HttpHeaders.EMPTY,
            "provider payload".getBytes(java.nio.charset.StandardCharsets.UTF_8),
            java.nio.charset.StandardCharsets.UTF_8
        );

        ResponseEntity<ApiError> response = handler.handleWebClientResponse(exception, request());

        assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
        assertEquals("EXTERNAL_SERVICE_ERROR", response.getBody().code());
        assertEquals("External service request failed.", response.getBody().message());
    }

    private MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        return request;
    }
}
