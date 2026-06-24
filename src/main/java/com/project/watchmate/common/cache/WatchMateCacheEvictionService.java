package com.project.watchmate.common.cache;

import java.util.ArrayList;
import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.project.watchmate.media.catalog.domain.MediaType;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class WatchMateCacheEvictionService {

    /**
     * Prefix shared by every watchlist summary page entry in Redis.
     *
     * <p>Key anatomy (built by Spring Cache + CacheConfig):
     * <pre>
     *   watchmate::                           ← prefixCacheNameWith() in CacheConfig
     *   watchlistSummaryPages                 ← WatchMateCacheNames.WATCHLIST_SUMMARY_PAGES
     *   ::                                    ← Spring separator
     *   user:{userId}:page:{p}:size:{s}:sort:id_asc  ← WatchMateCacheKeys.watchlistPage(...)
     * </pre>
     *
     * Full key example:
     * {@code watchmate::watchlistSummaryPages::user:1:page:0:size:20:sort:id_asc}
     */
    static final String WATCHLIST_PAGE_KEY_PREFIX = "watchmate::watchlistSummaryPages::";

    private final StringRedisTemplate stringRedisTemplate;

    public WatchMateCacheEvictionService() {
        this(null);
    }

    public WatchMateCacheEvictionService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Caching(evict = {
        @CacheEvict(cacheNames = WatchMateCacheNames.DISCOVERY_HOMEPAGE_SECTIONS, allEntries = true),
        @CacheEvict(cacheNames = WatchMateCacheNames.CURATED_CONTENT_LISTS, allEntries = true)
    })
    public void evictDiscoveryContentCaches() {
    }

    @CacheEvict(
        cacheNames = WatchMateCacheNames.PUBLIC_MEDIA_DETAIL_BASE,
        key = "T(com.project.watchmate.common.cache.WatchMateCacheKeys).media(#type, #tmdbId)"
    )
    public void evictPublicMediaDetailBase(MediaType type, Long tmdbId) {
    }

    @CacheEvict(
        cacheNames = WatchMateCacheNames.PUBLIC_SHOW_METADATA,
        key = "T(com.project.watchmate.common.cache.WatchMateCacheKeys).show(#tmdbId)"
    )
    public void evictPublicShowMetadata(Long tmdbId) {
    }

    @CacheEvict(
        cacheNames = WatchMateCacheNames.PUBLIC_SEASON_METADATA,
        key = "T(com.project.watchmate.common.cache.WatchMateCacheKeys).season(#tmdbId, #seasonNumber)"
    )
    public void evictPublicSeasonMetadata(Long tmdbId, Integer seasonNumber) {
    }

    @CacheEvict(
        cacheNames = TmdbCacheNames.TMDB_MEDIA_CREDITS,
        key = "T(com.project.watchmate.common.cache.TmdbCacheKeys).credits(#type, #tmdbId)"
    )
    public void evictTmdbMediaCredits(MediaType type, Long tmdbId) {
    }

    @CacheEvict(
        cacheNames = TmdbCacheNames.TMDB_MEDIA_VIDEOS,
        key = "T(com.project.watchmate.common.cache.TmdbCacheKeys).videos(#type, #tmdbId)"
    )
    public void evictTmdbMediaVideos(MediaType type, Long tmdbId) {
    }

    @CacheEvict(
        cacheNames = TmdbCacheNames.TMDB_MEDIA_WATCH_PROVIDERS,
        key = "T(com.project.watchmate.common.cache.TmdbCacheKeys).watchProviders(#type, #tmdbId)"
    )
    public void evictTmdbMediaWatchProviders(MediaType type, Long tmdbId) {
    }

    @CacheEvict(
        cacheNames = WatchMateCacheNames.CONTINUE_WATCHING,
        key = "T(com.project.watchmate.common.cache.WatchMateCacheKeys).user(#userId)"
    )
    public void evictContinueWatching(Long userId) {
    }

    @CacheEvict(
        cacheNames = WatchMateCacheNames.USER_FAVORITE_MEDIA_IDS,
        key = "T(com.project.watchmate.common.cache.WatchMateCacheKeys).user(#userId)"
    )
    public void evictUserFavoriteMediaIds(Long userId) {
    }

    @CacheEvict(cacheNames = WatchMateCacheNames.WATCHLIST_SUMMARY_PAGES, allEntries = true)
    public void evictWatchlistSummaryPages() {
    }

    @CacheEvict(
        cacheNames = WatchMateCacheNames.CONTINUE_WATCHING,
        key = "T(com.project.watchmate.common.cache.WatchMateCacheKeys).user(#userId)"
    )
    public void evictUserProgressCaches(Long userId) {
        // Watchlist overlays include watch-status indicators, so a progress update
        // must also invalidate the user's watchlist summary pages.
        evictWatchlistSummaryPagesForUser(userId);
    }

    @CacheEvict(
        cacheNames = WatchMateCacheNames.USER_FAVORITE_MEDIA_IDS,
        key = "T(com.project.watchmate.common.cache.WatchMateCacheKeys).user(#userId)"
    )
    public void evictFavoriteCaches(Long userId) {
        // Watchlist DTOs include per-item "is-favourited" flags, so a favourites
        // mutation must also invalidate the user's watchlist summary pages.
        evictWatchlistSummaryPagesForUser(userId);
    }

    /**
     * Evicts all cached watchlist summary page entries for a specific user without
     * touching any other user's entries.
     *
     * <p>Uses Redis SCAN (not the blocking KEYS command) to find all page-variant
     * entries for the given user and deletes them in a single DEL call per cursor
     * batch. SCAN is non-blocking and safe for production Redis instances.
     *
     * <p>Eviction failure is best-effort: a Redis connectivity problem is logged as
     * WARN and the caller's request continues normally. The 5-minute TTL guarantees
     * eventual consistency even if eviction is skipped.
     */
    public void evictWatchlistSummaryPagesForUser(Long userId) {
        if (stringRedisTemplate == null) {
            log.debug("No Redis bean; skipping user-scoped watchlist cache eviction for user={}", userId);
            return;
        }
        String pattern = buildWatchlistEvictionPattern(userId);
        try {
            long deleted = scanAndDeleteByPattern(pattern);
            if (deleted > 0) {
                log.debug("Evicted {} watchlist summary page entries for user={}", deleted, userId);
            }
        } catch (Exception e) {
            log.warn("Failed to evict watchlist cache for user={}: {}", userId, e.getMessage());
        }
    }

    /**
     * Returns the Redis SCAN glob pattern that matches every watchlist summary page
     * cache entry belonging to {@code userId}.
     *
     * <p>Package-private so unit tests can verify the pattern against the actual key
     * format produced by {@link WatchMateCacheKeys#watchlistPage} without needing a
     * live Redis instance.
     */
    static String buildWatchlistEvictionPattern(Long userId) {
        return WATCHLIST_PAGE_KEY_PREFIX + "user:" + userId + ":*";
    }

    /**
     * Scans Redis for keys matching {@code pattern} and deletes them in a single
     * {@code DEL} call per cursor batch.
     *
     * <p>Uses {@code SCAN} with {@code COUNT 100} (a hint to Redis about how many
     * keys to return per iteration — Redis may return more or fewer). The scan loops
     * until the full keyspace is covered and all matching keys have been collected.
     *
     * @return number of keys deleted
     */
    private long scanAndDeleteByPattern(String pattern) {
        Long deleted = stringRedisTemplate.execute((RedisCallback<Long>) connection -> {
            List<byte[]> keysToDelete = new ArrayList<>();
            ScanOptions opts = ScanOptions.scanOptions().match(pattern).count(100).build();
            try (Cursor<byte[]> cursor = connection.keyCommands().scan(opts)) {
                cursor.forEachRemaining(keysToDelete::add);
            }
            if (keysToDelete.isEmpty()) {
                return 0L;
            }
            connection.keyCommands().del(keysToDelete.toArray(new byte[0][]));
            return (long) keysToDelete.size();
        });
        return deleted == null ? 0L : deleted;
    }
}
