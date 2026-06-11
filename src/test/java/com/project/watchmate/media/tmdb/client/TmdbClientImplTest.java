package com.project.watchmate.media.tmdb.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.project.watchmate.common.error.TmdbClientException;

import reactor.core.publisher.Mono;

class TmdbClientImplTest {

    @Test
    void searchMulti_whenTmdbReturnsBadRequest_wrapsAsTmdbClientException() {
        TmdbClientImpl client = new TmdbClientImpl(webClientReturning(HttpStatus.BAD_REQUEST, "{\"status_message\":\"bad query\"}"));

        TmdbClientException exception = assertThrows(
            TmdbClientException.class,
            () -> client.searchMulti("bad", 1)
        );

        assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatus());
        assertEquals("TMDB_CLIENT_ERROR", exception.getCode());
        assertEquals("TMDB request failed.", exception.getMessage());
        assertInstanceOf(WebClientResponseException.class, exception.getCause());
    }

    @Test
    void searchMulti_whenTmdbReturnsTooManyRequests_wrapsAsUnavailableWithoutRawPayload() {
        TmdbClientImpl client = new TmdbClientImpl(webClientReturning(HttpStatus.TOO_MANY_REQUESTS, "provider rate payload"));

        TmdbClientException exception = assertThrows(
            TmdbClientException.class,
            () -> client.searchMulti("matrix", 1)
        );

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatus());
        assertEquals("TMDB_UNAVAILABLE", exception.getCode());
        assertEquals("TMDB is temporarily unavailable. Please try again shortly.", exception.getMessage());
    }

    @Test
    void searchMulti_whenUnexpectedRuntimeFailure_wrapsAsTmdbClientException() {
        WebClient webClient = WebClient.builder()
            .exchangeFunction(request -> Mono.error(new IllegalStateException("decoder exploded")))
            .build();
        TmdbClientImpl client = new TmdbClientImpl(webClient);

        TmdbClientException exception = assertThrows(
            TmdbClientException.class,
            () -> client.searchMulti("matrix", 1)
        );

        assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatus());
        assertEquals("TMDB_CLIENT_ERROR", exception.getCode());
        assertEquals("TMDB request failed.", exception.getMessage());
        assertInstanceOf(IllegalStateException.class, exception.getCause());
    }

    private WebClient webClientReturning(HttpStatus status, String body) {
        return WebClient.builder()
            .exchangeFunction(request -> Mono.just(ClientResponse.create(status).body(body).build()))
            .build();
    }
}
