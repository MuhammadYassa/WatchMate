package com.project.watchmate.common.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.services.ses.model.SesException;

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

    @Test
    void handleSesException_returnsSanitizedMessageAndServiceUnavailable() {
        SesException sesException = (SesException) SesException.builder()
            .message("MessageRejected: AWS internal detail with requestId=abc123")
            .awsErrorDetails(AwsErrorDetails.builder()
                .errorCode("MessageRejected")
                .errorMessage("Email address is not verified in SES sandbox")
                .build())
            .build();

        ResponseEntity<ApiError> response = handler.handleSesException(sesException, request());

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals("EMAIL_SERVICE_UNAVAILABLE", response.getBody().code());
        assertEquals("Email service is temporarily unavailable. Please try again later.", response.getBody().message());
        assertFalse(response.getBody().message().contains("requestId"));
        assertFalse(response.getBody().message().contains("AWS"));
        assertFalse(response.getBody().message().contains("MessageRejected"));
    }

    private MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        return request;
    }
}
