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
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

@Configuration
@EnableCaching
@ConditionalOnProperty(name = "watchmate.cache.enabled", havingValue = "true", matchIfMissing = true)
public class CacheConfig implements CachingConfigurer {

    private static final Duration DEFAULT_TTL = Duration.ofMinutes(30);
    private static final Duration DETAILS_TTL = Duration.ofHours(12);
    private static final Duration GENRES_TTL = Duration.ofHours(24);
    private static final Duration SEARCH_TTL = Duration.ofMinutes(10);
    private static final Duration DISCOVERY_TTL = Duration.ofMinutes(30);
    private static final Duration TMDB_MEDIA_CREDITS_TTL = Duration.ofHours(24);
    private static final Duration TMDB_MEDIA_VIDEOS_TTL = Duration.ofHours(6);
    private static final Duration TMDB_MEDIA_WATCH_PROVIDERS_TTL = Duration.ofHours(2);
    private static final Duration PUBLIC_MEDIA_DETAIL_BASE_TTL = Duration.ofHours(6);
    private static final Duration PUBLIC_SHOW_METADATA_TTL = Duration.ofHours(6);
    private static final Duration PUBLIC_SEASON_METADATA_TTL = Duration.ofHours(6);
    private static final Duration CONTINUE_WATCHING_TTL = Duration.ofMinutes(5);
    private static final Duration WATCHLIST_SUMMARY_PAGES_TTL = Duration.ofMinutes(5);
    private static final Duration USER_FAVORITE_MEDIA_IDS_TTL = Duration.ofMinutes(10);
    private static final String TYPE_HINT_PROPERTY = "@class";

    @Bean
    public RedisCacheManager cacheManager(
        RedisConnectionFactory redisConnectionFactory,
        ObjectMapper objectMapper
    ) {
        ObjectMapper cacheObjectMapper = objectMapper.rebuild().build();
        GenericJacksonJsonRedisSerializer valueSerializer = GenericJacksonJsonRedisSerializer
            .builder(cacheObjectMapper::rebuild)
            .enableDefaultTyping(cachePolymorphicTypeValidator())
            .typePropertyName(TYPE_HINT_PROPERTY)
            .build();
        RedisCacheWriter cacheWriter = RedisCacheWriter.create(redisConnectionFactory, config -> config.immediateWrites(true));
        RedisCacheConfiguration defaultConfig = redisCacheConfiguration(DEFAULT_TTL, valueSerializer);

        Map<String, RedisCacheConfiguration> cacheConfigurations = Map.ofEntries(
            Map.entry(TmdbCacheNames.TMDB_MEDIA_DETAILS, redisCacheConfiguration(DETAILS_TTL, valueSerializer)),
            Map.entry(TmdbCacheNames.TMDB_SHOW_DETAILS, redisCacheConfiguration(DETAILS_TTL, valueSerializer)),
            Map.entry(TmdbCacheNames.TMDB_SEASON_DETAILS, redisCacheConfiguration(DETAILS_TTL, valueSerializer)),
            Map.entry(TmdbCacheNames.TMDB_GENRES, redisCacheConfiguration(GENRES_TTL, valueSerializer)),
            Map.entry(TmdbCacheNames.TMDB_SEARCH, redisCacheConfiguration(SEARCH_TTL, valueSerializer)),
            Map.entry(TmdbCacheNames.TMDB_DISCOVER_BY_GENRE, redisCacheConfiguration(DISCOVERY_TTL, valueSerializer)),
            Map.entry(TmdbCacheNames.TMDB_POPULAR, redisCacheConfiguration(DISCOVERY_TTL, valueSerializer)),
            Map.entry(TmdbCacheNames.TMDB_TRENDING, redisCacheConfiguration(DISCOVERY_TTL, valueSerializer)),
            Map.entry(TmdbCacheNames.TMDB_UPCOMING_MOVIES, redisCacheConfiguration(DISCOVERY_TTL, valueSerializer)),
            Map.entry(TmdbCacheNames.TMDB_AIRING_TODAY, redisCacheConfiguration(DISCOVERY_TTL, valueSerializer)),
            Map.entry(TmdbCacheNames.TMDB_ON_THE_AIR, redisCacheConfiguration(DISCOVERY_TTL, valueSerializer)),
            Map.entry(TmdbCacheNames.TMDB_MEDIA_CREDITS, redisCacheConfiguration(TMDB_MEDIA_CREDITS_TTL, valueSerializer)),
            Map.entry(TmdbCacheNames.TMDB_MEDIA_VIDEOS, redisCacheConfiguration(TMDB_MEDIA_VIDEOS_TTL, valueSerializer)),
            Map.entry(TmdbCacheNames.TMDB_MEDIA_WATCH_PROVIDERS, redisCacheConfiguration(TMDB_MEDIA_WATCH_PROVIDERS_TTL, valueSerializer)),
            Map.entry(WatchMateCacheNames.DISCOVERY_HOMEPAGE_SECTIONS, redisCacheConfiguration(DISCOVERY_TTL, valueSerializer)),
            Map.entry(WatchMateCacheNames.CURATED_CONTENT_LISTS, redisCacheConfiguration(DISCOVERY_TTL, valueSerializer)),
            Map.entry(WatchMateCacheNames.PUBLIC_MEDIA_DETAIL_BASE, redisCacheConfiguration(PUBLIC_MEDIA_DETAIL_BASE_TTL, valueSerializer)),
            Map.entry(WatchMateCacheNames.PUBLIC_SHOW_METADATA, redisCacheConfiguration(PUBLIC_SHOW_METADATA_TTL, valueSerializer)),
            Map.entry(WatchMateCacheNames.PUBLIC_SEASON_METADATA, redisCacheConfiguration(PUBLIC_SEASON_METADATA_TTL, valueSerializer)),
            Map.entry(WatchMateCacheNames.CONTINUE_WATCHING, redisCacheConfiguration(CONTINUE_WATCHING_TTL, valueSerializer)),
            Map.entry(WatchMateCacheNames.WATCHLIST_SUMMARY_PAGES, redisCacheConfiguration(WATCHLIST_SUMMARY_PAGES_TTL, valueSerializer)),
            Map.entry(WatchMateCacheNames.USER_FAVORITE_MEDIA_IDS, redisCacheConfiguration(USER_FAVORITE_MEDIA_IDS_TTL, valueSerializer))
        );

        return RedisCacheManager.builder(cacheWriter)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigurations)
            .build();
    }

    @Bean
    @Override
    public CacheErrorHandler errorHandler() {
        return new FailOpenCacheErrorHandler();
    }

    private RedisCacheConfiguration redisCacheConfiguration(Duration ttl, GenericJacksonJsonRedisSerializer valueSerializer) {
        return RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(ttl)
            .disableCachingNullValues()
            .prefixCacheNameWith("watchmate::")
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(valueSerializer));
    }

    private BasicPolymorphicTypeValidator cachePolymorphicTypeValidator() {
        return BasicPolymorphicTypeValidator.builder()
            .allowIfSubType("com.project.watchmate.")
            .allowIfSubType(java.util.Collection.class)
            .allowIfSubType(java.util.Map.class)
            .allowIfSubType(java.lang.Number.class)
            .allowIfSubType(java.lang.Boolean.class)
            .allowIfSubType(java.lang.CharSequence.class)
            .allowIfSubType(java.time.LocalDate.class)
            .allowIfSubType(java.time.LocalDateTime.class)
            .build();
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
