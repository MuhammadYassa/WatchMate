package com.project.watchmate.common.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;

import com.project.watchmate.common.error.TmdbClientException;
import com.project.watchmate.media.tmdb.client.TmdbClient;
import com.project.watchmate.media.tmdb.client.TmdbClientImpl;

import reactor.core.publisher.Mono;

@SpringJUnitConfig(TmdbClientCachingTest.TestConfig.class)
class TmdbClientCachingTest {

    @Autowired
    private TmdbClient tmdbClient;

    @Autowired
    private CountingExchangeFunction exchangeFunction;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        exchangeFunction.reset();
        cacheManager.getCacheNames().stream()
            .map(cacheManager::getCache)
            .filter(java.util.Objects::nonNull)
            .forEach(org.springframework.cache.Cache::clear);
    }

    @Test
    void fetchMediaById_whenSameRequestRepeated_usesCachedValue() {
        assertEquals("The Matrix", tmdbClient.fetchMediaById(100L, com.project.watchmate.media.catalog.domain.MediaType.MOVIE).getTitle());
        assertEquals("The Matrix", tmdbClient.fetchMediaById(100L, com.project.watchmate.media.catalog.domain.MediaType.MOVIE).getTitle());

        assertEquals(1, exchangeFunction.requestCount());
    }

    @Test
    void searchMulti_whenQueryDiffersOnlyByCaseAndWhitespace_usesSameCacheEntry() {
        assertEquals(1, tmdbClient.searchMulti(" Matrix ", 1).getTotalResults());
        assertEquals(1, tmdbClient.searchMulti("matrix", 1).getTotalResults());

        assertEquals(1, exchangeFunction.requestCount());
    }

    @Test
    void searchMulti_whenPageDiffers_usesSeparateCacheEntries() {
        tmdbClient.searchMulti("matrix", 1);
        tmdbClient.searchMulti("matrix", 2);
        tmdbClient.searchMulti("matrix", 1);

        assertEquals(2, exchangeFunction.requestCount());
    }

    @Test
    void discoverByGenre_whenTypeGenreOrPageDiffers_usesSeparateCacheEntries() {
        tmdbClient.discoverByGenre("movie", 28L, 1);
        tmdbClient.discoverByGenre(" MOVIE ", 28L, 1);
        tmdbClient.discoverByGenre("tv", 28L, 1);
        tmdbClient.discoverByGenre("movie", 35L, 1);
        tmdbClient.discoverByGenre("movie", 28L, 2);

        assertEquals(4, exchangeFunction.requestCount());
    }

    @Test
    void fetchGenres_whenTypeDiffersOnlyByCaseAndWhitespace_usesSameCacheEntry() {
        assertEquals("Action", tmdbClient.fetchGenres(" Movie ").get(0).getName());
        assertEquals("Action", tmdbClient.fetchGenres("movie").get(0).getName());

        assertEquals(1, exchangeFunction.requestCount());
    }

    @Test
    void fetchCredits_whenSameRequestRepeated_usesCachedValue() {
        assertEquals("First Actor", tmdbClient.fetchCredits(200L, com.project.watchmate.media.catalog.domain.MediaType.MOVIE).getCast().get(0).getName());
        assertEquals("First Actor", tmdbClient.fetchCredits(200L, com.project.watchmate.media.catalog.domain.MediaType.MOVIE).getCast().get(0).getName());

        assertEquals(1, exchangeFunction.requestCount());
    }

    @Test
    void fetchVideos_whenSameRequestRepeated_usesCachedValue() {
        assertEquals("trailer-key", tmdbClient.fetchVideos(201L, com.project.watchmate.media.catalog.domain.MediaType.MOVIE).getResults().get(0).getKey());
        assertEquals("trailer-key", tmdbClient.fetchVideos(201L, com.project.watchmate.media.catalog.domain.MediaType.MOVIE).getResults().get(0).getKey());

        assertEquals(1, exchangeFunction.requestCount());
    }

    @Test
    void fetchWatchProviders_whenSameRequestRepeated_cachesRawResponseByTypeAndTmdbId() {
        assertEquals("https://example.com/us", tmdbClient.fetchWatchProviders(202L, com.project.watchmate.media.catalog.domain.MediaType.MOVIE).getResults().get("US").getLink());
        assertEquals("https://example.com/us", tmdbClient.fetchWatchProviders(202L, com.project.watchmate.media.catalog.domain.MediaType.MOVIE).getResults().get("US").getLink());

        assertEquals(1, exchangeFunction.requestCount());
        assertEquals("movie:202", TmdbCacheKeys.watchProviders(com.project.watchmate.media.catalog.domain.MediaType.MOVIE, 202L));
    }

    @Test
    void searchMulti_whenTmdbThrows_exceptionIsNotCached() {
        exchangeFunction.failSearch();

        assertThrows(TmdbClientException.class, () -> tmdbClient.searchMulti("matrix", 1));
        assertThrows(TmdbClientException.class, () -> tmdbClient.searchMulti("matrix", 1));

        assertEquals(2, exchangeFunction.requestCount());
    }

    @Configuration
    @EnableCaching
    static class TestConfig {

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager();
        }

        @Bean
        CountingExchangeFunction exchangeFunction() {
            return new CountingExchangeFunction();
        }

        @Bean
        WebClient tmdbWebClient(CountingExchangeFunction exchangeFunction) {
            return WebClient.builder()
                .exchangeFunction(exchangeFunction)
                .build();
        }

        @Bean
        TmdbClientImpl tmdbClientImpl(WebClient tmdbWebClient) {
            return new TmdbClientImpl(tmdbWebClient);
        }
    }

    static class CountingExchangeFunction implements ExchangeFunction {

        private final AtomicInteger requestCount = new AtomicInteger();
        private boolean failSearch;

        @Override
        public Mono<ClientResponse> exchange(ClientRequest request) {
            requestCount.incrementAndGet();
            URI url = request.url();
            if (failSearch && url.getPath().equals("/search/multi")) {
                return Mono.just(response(HttpStatus.BAD_REQUEST, "{\"status_message\":\"bad query\"}"));
            }
            return Mono.just(response(HttpStatus.OK, bodyFor(url)));
        }

        int requestCount() {
            return requestCount.get();
        }

        void failSearch() {
            failSearch = true;
        }

        void reset() {
            requestCount.set(0);
            failSearch = false;
        }

        private ClientResponse response(HttpStatus status, String body) {
            return ClientResponse.create(status)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(body)
                .build();
        }

        private String bodyFor(URI url) {
            String path = url.getPath();
            if (path.startsWith("/genre/")) {
                return "{\"genres\":[{\"id\":28,\"name\":\"Action\"}]}";
            }
            if (path.endsWith("/credits")) {
                return "{\"id\":200,\"cast\":[{\"id\":1,\"name\":\"First Actor\",\"character\":\"Lead\",\"order\":0}]}";
            }
            if (path.endsWith("/videos")) {
                return "{\"id\":201,\"results\":[{\"key\":\"trailer-key\",\"name\":\"Trailer\",\"site\":\"YouTube\",\"type\":\"Trailer\",\"official\":true,\"published_at\":\"2026-01-01T00:00:00.000Z\"}]}";
            }
            if (path.endsWith("/watch/providers")) {
                return "{\"id\":202,\"results\":{\"US\":{\"link\":\"https://example.com/us\",\"flatrate\":[{\"provider_id\":1,\"provider_name\":\"Provider\",\"display_priority\":0}]},\"GB\":{\"link\":\"https://example.com/gb\"}}}";
            }
            if (path.startsWith("/movie/100")) {
                return "{\"id\":100,\"title\":\"The Matrix\",\"genre_ids\":[28]}";
            }
            if (path.startsWith("/discover/")) {
                int page = page(url);
                return responseBody(page, 20L + page, "Discovered");
            }
            if (path.equals("/search/multi")) {
                int page = page(url);
                return responseBody(page, 10L + page, "The Matrix");
            }
            return responseBody(1, 1L, "Cached Result");
        }

        private String responseBody(int page, Long id, String title) {
            return """
                {"results":[{"id":%d,"media_type":"movie","title":"%s"}],"page":%d,"total_pages":2,"total_results":1}
                """.formatted(id, title, page);
        }

        private int page(URI url) {
            String query = url.getQuery();
            if (query == null) {
                return 1;
            }
            for (String param : query.split("&")) {
                String[] parts = param.split("=", 2);
                if (parts.length == 2 && parts[0].equals("page")) {
                    return Integer.parseInt(parts[1]);
                }
            }
            return 1;
        }
    }
}
