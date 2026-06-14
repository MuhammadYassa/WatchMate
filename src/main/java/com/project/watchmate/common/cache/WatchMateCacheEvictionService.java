package com.project.watchmate.common.cache;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import com.project.watchmate.media.catalog.domain.MediaType;

@Service
public class WatchMateCacheEvictionService {

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

    @Caching(evict = {
        @CacheEvict(
            cacheNames = WatchMateCacheNames.CONTINUE_WATCHING,
            key = "T(com.project.watchmate.common.cache.WatchMateCacheKeys).user(#userId)"
        ),
        @CacheEvict(cacheNames = WatchMateCacheNames.WATCHLIST_SUMMARY_PAGES, allEntries = true)
    })
    public void evictUserProgressCaches(Long userId) {
    }

    @Caching(evict = {
        @CacheEvict(
            cacheNames = WatchMateCacheNames.USER_FAVORITE_MEDIA_IDS,
            key = "T(com.project.watchmate.common.cache.WatchMateCacheKeys).user(#userId)"
        ),
        @CacheEvict(cacheNames = WatchMateCacheNames.WATCHLIST_SUMMARY_PAGES, allEntries = true)
    })
    public void evictFavoriteCaches(Long userId) {
    }
}
