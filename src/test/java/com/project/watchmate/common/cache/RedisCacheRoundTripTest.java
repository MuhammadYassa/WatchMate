package com.project.watchmate.common.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.net.URI;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
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
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.project.watchmate.media.tmdb.client.TmdbClient;
import com.project.watchmate.media.tmdb.client.TmdbClientImpl;
import com.project.watchmate.media.tmdb.dto.TmdbMovieDTO;
import com.project.watchmate.media.tmdb.dto.TmdbResponseDTO;

import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

@Testcontainers
@SpringJUnitConfig(RedisCacheRoundTripTest.TestConfig.class)
class RedisCacheRoundTripTest {

    @Container
    @SuppressWarnings("resource")
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7.4-alpine")
        .withExposedPorts(6379);

    @Autowired
    private TmdbClient tmdbClient;

    @Autowired
    private CountingExchangeFunction exchangeFunction;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        exchangeFunction.reset();
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
}
