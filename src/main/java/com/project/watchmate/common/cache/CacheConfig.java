package com.project.watchmate.common.cache;

import java.time.Duration;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.json.JsonMapper;

@Configuration
@EnableCaching
@ConditionalOnProperty(name = "watchmate.cache.enabled", havingValue = "true", matchIfMissing = true)
public class CacheConfig implements CachingConfigurer {

    private static final Duration DEFAULT_TTL = Duration.ofMinutes(30);
    private static final Duration DETAILS_TTL = Duration.ofHours(12);
    private static final Duration GENRES_TTL = Duration.ofHours(24);
    private static final Duration SEARCH_TTL = Duration.ofMinutes(10);
    private static final Duration DISCOVERY_TTL = Duration.ofMinutes(30);

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        RedisCacheConfiguration defaultConfig = redisCacheConfiguration(DEFAULT_TTL);

        Map<String, RedisCacheConfiguration> cacheConfigurations = Map.ofEntries(
            Map.entry(TmdbCacheNames.TMDB_MEDIA_DETAILS, redisCacheConfiguration(DETAILS_TTL)),
            Map.entry(TmdbCacheNames.TMDB_SHOW_DETAILS, redisCacheConfiguration(DETAILS_TTL)),
            Map.entry(TmdbCacheNames.TMDB_SEASON_DETAILS, redisCacheConfiguration(DETAILS_TTL)),
            Map.entry(TmdbCacheNames.TMDB_GENRES, redisCacheConfiguration(GENRES_TTL)),
            Map.entry(TmdbCacheNames.TMDB_SEARCH, redisCacheConfiguration(SEARCH_TTL)),
            Map.entry(TmdbCacheNames.TMDB_DISCOVER_BY_GENRE, redisCacheConfiguration(DISCOVERY_TTL)),
            Map.entry(TmdbCacheNames.TMDB_POPULAR, redisCacheConfiguration(DISCOVERY_TTL)),
            Map.entry(TmdbCacheNames.TMDB_TRENDING, redisCacheConfiguration(DISCOVERY_TTL)),
            Map.entry(TmdbCacheNames.TMDB_UPCOMING_MOVIES, redisCacheConfiguration(DISCOVERY_TTL)),
            Map.entry(TmdbCacheNames.TMDB_AIRING_TODAY, redisCacheConfiguration(DISCOVERY_TTL)),
            Map.entry(TmdbCacheNames.TMDB_ON_THE_AIR, redisCacheConfiguration(DISCOVERY_TTL))
        );

        return RedisCacheManager.builder(redisConnectionFactory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigurations)
            .build();
    }

    @Bean
    @Override
    public CacheErrorHandler errorHandler() {
        return new FailOpenCacheErrorHandler();
    }

    private RedisCacheConfiguration redisCacheConfiguration(Duration ttl) {
        return RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(ttl)
            .disableCachingNullValues()
            .prefixCacheNameWith("watchmate::")
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJacksonJsonRedisSerializer(new JsonMapper())));
    }

    @Slf4j
    private static class FailOpenCacheErrorHandler implements CacheErrorHandler {

        @Override
        public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
            log.warn("Cache get failed cache={} key={}", cache.getName(), key, exception);
        }

        @Override
        public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
            log.warn("Cache put failed cache={} key={}", cache.getName(), key, exception);
        }

        @Override
        public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
            log.warn("Cache evict failed cache={} key={}", cache.getName(), key, exception);
        }

        @Override
        public void handleCacheClearError(RuntimeException exception, Cache cache) {
            log.warn("Cache clear failed cache={}", cache.getName(), exception);
        }
    }
}
