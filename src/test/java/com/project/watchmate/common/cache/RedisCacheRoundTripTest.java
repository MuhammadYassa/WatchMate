package com.project.watchmate.common.cache;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.project.watchmate.media.tmdb.client.TmdbClient;
import com.project.watchmate.media.tmdb.client.TmdbClientImpl;
import com.project.watchmate.media.tmdb.dto.TmdbMovieDTO;
import com.project.watchmate.media.tmdb.dto.TmdbResponseDTO;
import com.project.watchmate.show.metadata.dto.PublicShowEpisodeMetadataDTO;
import com.project.watchmate.show.metadata.dto.PublicShowSeasonMetadataDTO;

import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

@Testcontainers
@SpringJUnitConfig(RedisCacheRoundTripTest.TestConfig.class)
class RedisCacheRoundTripTest {

    @Container
    @SuppressWarnings("resource")
    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7.4-alpine"))
        .withExposedPorts(6379);

    @Autowired
    private TmdbClient tmdbClient;

    @Autowired
    private CountingExchangeFunction exchangeFunction;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private SeasonMetadataCacheProbe seasonMetadataCacheProbe;

    @Autowired
    private WatchlistPageCacheProbe watchlistPageCacheProbe;

    @Autowired
    private WatchMateCacheEvictionService cacheEvictionService;

    @BeforeEach
    void setUp() {
        exchangeFunction.reset();
        seasonMetadataCacheProbe.reset();
        watchlistPageCacheProbe.reset();
        flushRedis();
    }

    @Test
    void searchMulti_whenRedisRoundTrips_returnsConcreteTmdbDtosFromCache() {
        TmdbResponseDTO first = tmdbClient.searchMulti("matrix", 1);
        String rawValue = stringRedisTemplate.opsForValue().get("watchmate::tmdbSearch::matrix:1");
        Object cachedValue = cacheManager.getCache(TmdbCacheNames.TMDB_SEARCH)
            .get(TmdbCacheKeys.search("matrix", 1), Object.class);
        TmdbResponseDTO second = tmdbClient.searchMulti("matrix", 1);

        assertNotNull(first);
        assertNotNull(rawValue);
        assertNotNull(cachedValue);
        assertNotNull(second);
        assertEquals(1, exchangeFunction.requestCount());
        assertEquals(Set.of("watchmate::tmdbSearch::matrix:1"), stringRedisTemplate.keys("watchmate::tmdbSearch::*"));
        assertInstanceOf(TmdbResponseDTO.class, second);
        assertInstanceOf(TmdbMovieDTO.class, second.getResults().get(0));
        assertEquals("The Matrix", second.getResults().get(0).getTitle());
    }

    @Test
    void publicSeasonMetadata_whenRedisRoundTrips_preservesTmdbEpisodeIds() {
        PublicShowSeasonMetadataDTO first = seasonMetadataCacheProbe.getSeasonMetadata(200L, 1);
        PublicShowSeasonMetadataDTO second = seasonMetadataCacheProbe.getSeasonMetadata(200L, 1);
        Set<String> cacheKeys = stringRedisTemplate.keys("watchmate::*");
        String cacheKey = cacheKeys == null
            ? null
            : cacheKeys.stream()
                .filter(key -> key.contains(WatchMateCacheNames.PUBLIC_SEASON_METADATA))
                .filter(key -> key.endsWith(WatchMateCacheKeys.season(200L, 1)))
                .findFirst()
                .orElse(null);
        String rawValue = cacheKey == null ? null : stringRedisTemplate.opsForValue().get(cacheKey);

        assertNotNull(first);
        assertNotNull(second);
        assertNotNull(cacheKeys);
        assertNotNull(cacheKey);
        assertNotNull(rawValue);
        assertEquals(1, seasonMetadataCacheProbe.invocationCount());
        assertTrue(cacheKeys.stream().anyMatch(key -> key.contains(WatchMateCacheNames.PUBLIC_SEASON_METADATA)));
        assertEquals(2101L, second.getEpisodes().get(0).getTmdbEpisodeId());
        assertNull(second.getEpisodes().get(1).getTmdbEpisodeId());
        assertTrue(rawValue.contains("\"tmdbEpisodeId\":2101"));
    }

    /**
     * Verifies that SCAN-based eviction deletes all Redis keys belonging to the target
     * user and leaves a different user's keys intact.
     *
     * <p>This is the end-to-end proof that:
     * <ul>
     *   <li>The eviction pattern {@code watchmate::watchlistSummaryPages::user:{id}:*}
     *       actually matches the Redis keys written by Spring Cache.</li>
     *   <li>SCAN correctly iterates and deletes only matching keys.</li>
     *   <li>Keys for another user are not touched.</li>
     * </ul>
     */
    @Test
    void evictWatchlistSummaryPagesForUser_evictsOnlyTargetUsersRedisEntries() {
        // Populate Redis via the @Cacheable proxy for two different users.
        watchlistPageCacheProbe.getPage(1L, 0, 20);
        watchlistPageCacheProbe.getPage(2L, 0, 20);
        assertEquals(2, watchlistPageCacheProbe.invocationCount(), "Both calls should be cache misses initially");

        String user1Key = WatchMateCacheEvictionService.WATCHLIST_PAGE_KEY_PREFIX + WatchMateCacheKeys.watchlistPage(1L, 0, 20);
        String user2Key = WatchMateCacheEvictionService.WATCHLIST_PAGE_KEY_PREFIX + WatchMateCacheKeys.watchlistPage(2L, 0, 20);

        // Both keys exist before eviction.
        assertNotNull(stringRedisTemplate.opsForValue().get(user1Key), "User 1 key must exist before eviction");
        assertNotNull(stringRedisTemplate.opsForValue().get(user2Key), "User 2 key must exist before eviction");

        // Evict only user 1's entries.
        cacheEvictionService.evictWatchlistSummaryPagesForUser(1L);

        // User 1's key is gone; user 2's key survives.
        assertNull(stringRedisTemplate.opsForValue().get(user1Key), "User 1 key must be deleted after eviction");
        assertNotNull(stringRedisTemplate.opsForValue().get(user2Key), "User 2 key must survive user 1 eviction");

        // Next call for user 1 is a cache miss → probe invoked again.
        watchlistPageCacheProbe.getPage(1L, 0, 20);
        assertEquals(3, watchlistPageCacheProbe.invocationCount(), "User 1 call after eviction must be a cache miss");

        // Next call for user 2 is still a cache hit → probe NOT invoked again.
        watchlistPageCacheProbe.getPage(2L, 0, 20);
        assertEquals(3, watchlistPageCacheProbe.invocationCount(), "User 2 call after user 1 eviction must still be a cache hit");
    }

    /**
     * Verifies that SCAN on an empty Redis keyspace does not throw, does not call the
     * cache probe, and produces no error log.
     */
    @Test
    void evictWatchlistSummaryPagesForUser_whenNoMatchingKeys_doesNothing() {
        assertDoesNotThrow(() -> cacheEvictionService.evictWatchlistSummaryPagesForUser(1L),
            "Eviction on empty Redis must not throw");
        assertEquals(0, watchlistPageCacheProbe.invocationCount(),
            "No cache population occurred so probe invocation count must be 0");
    }

    private void flushRedis() {
        RedisConnectionFactory connectionFactory = stringRedisTemplate.getConnectionFactory();
        if (connectionFactory != null) {
            connectionFactory.getConnection().serverCommands().flushDb();
        }
    }

    @Configuration
    @EnableCaching
    @Import(CacheConfig.class)
    static class TestConfig {

        @Bean
        RedisConnectionFactory redisConnectionFactory() {
            LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(6379));
            connectionFactory.afterPropertiesSet();
            return connectionFactory;
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        StringRedisTemplate stringRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
            return new StringRedisTemplate(redisConnectionFactory);
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
        TmdbClientImpl tmdbClient(WebClient tmdbWebClient) {
            return new TmdbClientImpl(tmdbWebClient);
        }

        @Bean
        SeasonMetadataCacheProbe seasonMetadataCacheProbe() {
            return new SeasonMetadataCacheProbe();
        }

        @Bean
        WatchlistPageCacheProbe watchlistPageCacheProbe() {
            return new WatchlistPageCacheProbe();
        }

        @Bean
        WatchMateCacheEvictionService cacheEvictionService(StringRedisTemplate stringRedisTemplate) {
            return new WatchMateCacheEvictionService(stringRedisTemplate);
        }
    }

    static class CountingExchangeFunction implements ExchangeFunction {

        private final AtomicInteger requestCount = new AtomicInteger();

        @Override
        public Mono<ClientResponse> exchange(ClientRequest request) {
            requestCount.incrementAndGet();
            return Mono.just(ClientResponse.create(HttpStatus.OK)
                .header(HttpHeaders.CONTENT_TYPE, org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
                .body(bodyFor(request.url()))
                .build());
        }

        int requestCount() {
            return requestCount.get();
        }

        void reset() {
            requestCount.set(0);
        }

        private String bodyFor(URI url) {
            if ("/search/multi".equals(url.getPath())) {
                return """
                    {"results":[{"id":11,"media_type":"movie","title":"The Matrix","genre_ids":[28]}],"page":1,"total_pages":2,"total_results":1}
                    """;
            }
            return """
                {"results":[{"id":1,"media_type":"movie","title":"Cached Result"}],"page":1,"total_pages":1,"total_results":1}
                """;
        }
    }

    /**
     * Thin cache probe for watchlist summary pages.
     *
     * <p>Uses the same {@code cacheNames} and {@code key} expression as the production
     * {@link com.project.watchmate.watchlist.application.WatchListPageCacheService}, so
     * the Redis keys it writes are identical to the ones {@link WatchMateCacheEvictionService}
     * is designed to delete.
     */
    static class WatchlistPageCacheProbe {

        private final AtomicInteger invocationCount = new AtomicInteger();

        @Cacheable(
            cacheNames = WatchMateCacheNames.WATCHLIST_SUMMARY_PAGES,
            key = "T(com.project.watchmate.common.cache.WatchMateCacheKeys).watchlistPage(#userId, #page, #size)"
        )
        public String getPage(Long userId, int page, int size) {
            invocationCount.incrementAndGet();
            return "page-for-user-" + userId;
        }

        int invocationCount() {
            return invocationCount.get();
        }

        void reset() {
            invocationCount.set(0);
        }
    }

    static class SeasonMetadataCacheProbe {

        private final AtomicInteger invocationCount = new AtomicInteger();

        @Cacheable(
            cacheNames = WatchMateCacheNames.PUBLIC_SEASON_METADATA,
            key = "T(com.project.watchmate.common.cache.WatchMateCacheKeys).season(#tmdbId, #seasonNumber)"
        )
        public PublicShowSeasonMetadataDTO getSeasonMetadata(Long tmdbId, Integer seasonNumber) {
            invocationCount.incrementAndGet();
            return PublicShowSeasonMetadataDTO.builder()
                .tmdbId(tmdbId)
                .seasonNumber(seasonNumber)
                .name("Season " + seasonNumber)
                .airDate(LocalDate.of(2020, 1, 1))
                .episodeCount(2)
                .episodes(List.of(
                    PublicShowEpisodeMetadataDTO.builder()
                        .tmdbEpisodeId(2101L)
                        .seasonNumber(seasonNumber)
                        .episodeNumber(1)
                        .name("Departure")
                        .isAired(Boolean.TRUE)
                        .build(),
                    PublicShowEpisodeMetadataDTO.builder()
                        .tmdbEpisodeId(null)
                        .seasonNumber(seasonNumber)
                        .episodeNumber(2)
                        .name("Arrival")
                        .isAired(Boolean.TRUE)
                        .build()
                ))
                .build();
        }

        int invocationCount() {
            return invocationCount.get();
        }

        void reset() {
            invocationCount.set(0);
        }
    }
}
