package com.project.watchmate.common.security.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import com.project.watchmate.common.integration.support.AbstractIntegrationTest;
import com.project.watchmate.media.tmdb.dto.TmdbEpisodeSummaryDTO;
import com.project.watchmate.media.tmdb.dto.TmdbMovieDTO;
import com.project.watchmate.media.tmdb.dto.TmdbTvDetailsDTO;
import com.project.watchmate.media.tmdb.dto.TmdbTvSeasonSummaryDTO;
import com.project.watchmate.user.domain.Users;

class CorsIntegrationTest extends AbstractIntegrationTest {

    private static final String ALLOWED_ORIGIN = "http://localhost:5173";
    private static final String DISALLOWED_ORIGIN = "http://localhost:3000";

    @Test
    void preflightRequest_fromAllowedOrigin_toProtectedRouteReturnsCorsHeaders() throws Exception {
        MvcResult result = mockMvc.perform(options("/api/v1/watchlists")
                .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization,content-type"))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN))
            .andReturn();

        String allowMethods = headerValue(result, HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS);
        String allowHeaders = headerValue(result, HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS);

        assertThat(normalize(allowMethods)).contains("get");
        assertThat(normalize(allowHeaders)).contains("authorization");
        assertThat(normalize(allowHeaders)).contains("content-type");
    }

    @Test
    void authenticatedRequest_fromAllowedOriginIncludesCorsAllowOriginHeader() throws Exception {
        Users user = saveUser("cors-watchlists-user", true);

        mockMvc.perform(get("/api/v1/watchlists")
                .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                .header(HttpHeaders.AUTHORIZATION, bearerToken(user)))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN));
    }

    @Test
    void acceptedShowStatusResponse_exposesPollingHeadersForAllowedOrigin() throws Exception {
        Users user = saveUser("cors-show-status-user", true);
        when(tmdbClient.fetchMediaById(eq(9500L), eq(com.project.watchmate.media.catalog.domain.MediaType.SHOW)))
            .thenReturn(TmdbMovieDTO.builder()
                .id(9500L)
                .title("Ongoing Show")
                .overview("Show overview")
                .posterPath("/ongoing.jpg")
                .releaseDate("2020-01-01")
                .genres(List.of())
                .build());
        when(tmdbClient.fetchTvDetailsById(eq(9500L))).thenReturn(largeOngoingShowDetailsWithId(9500L));

        MvcResult result = mockMvc.perform(put("/api/v1/shows/{tmdbId}/status", 9500L)
                .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                .header(HttpHeaders.AUTHORIZATION, bearerToken(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"status":"WATCHED"}
                    """))
            .andExpect(status().isAccepted())
            .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN))
            .andExpect(header().exists(HttpHeaders.LOCATION))
            .andExpect(header().string(HttpHeaders.RETRY_AFTER, "2"))
            .andReturn();

        String exposedHeaders = headerValue(result, HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS);
        assertThat(normalize(exposedHeaders)).contains("location");
        assertThat(normalize(exposedHeaders)).contains("retry-after");

        verify(tmdbClient, never()).fetchTvSeasonDetails(eq(9500L), anyInt());
    }

    @Test
    void preflightRequest_fromDisallowedOriginDoesNotReturnAllowOriginHeader() throws Exception {
        MvcResult result = mockMvc.perform(options("/api/v1/watchlists")
                .header(HttpHeaders.ORIGIN, DISALLOWED_ORIGIN)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization,content-type"))
            .andReturn();

        assertThat(result.getResponse().getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isNull();
        assertThat(result.getResponse().getStatus()).isIn(200, 403);
    }

    private TmdbTvDetailsDTO largeOngoingShowDetailsWithId(Long tmdbId) {
        return TmdbTvDetailsDTO.builder()
            .id(tmdbId)
            .name("Large Ongoing Show")
            .overview("A large ongoing show payload")
            .posterPath("/ongoing-large.jpg")
            .backdropPath("/ongoing-large-bg.jpg")
            .firstAirDate("2020-01-01")
            .voteAverage(8.1)
            .numberOfSeasons(4)
            .numberOfEpisodes(4)
            .lastAirDate("2026-04-01")
            .status("Returning Series")
            .genres(List.of())
            .seasons(List.of(
                TmdbTvSeasonSummaryDTO.builder().id(301L).seasonNumber(1).name("Season 1").episodeCount(1).airDate("2020-01-01").build(),
                TmdbTvSeasonSummaryDTO.builder().id(302L).seasonNumber(2).name("Season 2").episodeCount(1).airDate("2021-01-01").build(),
                TmdbTvSeasonSummaryDTO.builder().id(303L).seasonNumber(3).name("Season 3").episodeCount(1).airDate("2022-01-01").build(),
                TmdbTvSeasonSummaryDTO.builder().id(304L).seasonNumber(4).name("Season 4").episodeCount(1).airDate("2026-04-01").build()
            ))
            .nextEpisodeToAir(TmdbEpisodeSummaryDTO.builder()
                .airDate("2099-01-15")
                .seasonNumber(5)
                .episodeNumber(1)
                .name("Next Episode")
                .build())
            .build();
    }

    private String headerValue(MvcResult result, String name) {
        return result.getResponse().getHeader(name);
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
