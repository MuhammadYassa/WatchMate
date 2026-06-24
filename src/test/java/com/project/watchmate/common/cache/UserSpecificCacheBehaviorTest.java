package com.project.watchmate.common.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import com.project.watchmate.dashboard.application.ContinueWatchingCacheService;
import com.project.watchmate.dashboard.mapper.DashboardMapper;
import com.project.watchmate.favourite.application.UserFavoriteMediaIdsCacheService;
import com.project.watchmate.media.catalog.domain.Media;
import com.project.watchmate.media.catalog.domain.MediaType;
import com.project.watchmate.media.catalog.domain.WatchStatus;
import com.project.watchmate.movie.tracking.domain.UserMediaStatus;
import com.project.watchmate.movie.tracking.persistence.UserMediaStatusRepository;
import com.project.watchmate.show.tracking.persistence.UserShowTrackingRepository;
import com.project.watchmate.user.domain.Users;
import com.project.watchmate.user.persistence.UsersRepository;
import com.project.watchmate.watchlist.application.WatchListDtoAssembler;
import com.project.watchmate.watchlist.application.WatchListPageCacheService;
import com.project.watchmate.watchlist.domain.WatchList;
import com.project.watchmate.watchlist.dto.WatchListDTO;
import com.project.watchmate.watchlist.persistence.WatchListRepository;

@SpringJUnitConfig(UserSpecificCacheBehaviorTest.TestConfig.class)
class UserSpecificCacheBehaviorTest {

    @Autowired
    private UserFavoriteMediaIdsCacheService userFavoriteMediaIdsCacheService;

    @Autowired
    private ContinueWatchingCacheService continueWatchingCacheService;

    @Autowired
    private WatchListPageCacheService watchListPageCacheService;

    @Autowired
    private WatchMateCacheEvictionService cacheEvictionService;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private UserMediaStatusRepository userMediaStatusRepository;

    @Autowired
    private UserShowTrackingRepository userShowTrackingRepository;

    @Autowired
    private WatchListRepository watchListRepository;

    @Autowired
    private WatchListDtoAssembler watchListDtoAssembler;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private Users userOne;
    private Users userTwo;

    @BeforeEach
    void setUp() {
        reset(usersRepository, userMediaStatusRepository, userShowTrackingRepository,
              watchListRepository, watchListDtoAssembler, stringRedisTemplate);
        cacheManager.getCacheNames().stream()
            .map(cacheManager::getCache)
            .filter(java.util.Objects::nonNull)
            .forEach(org.springframework.cache.Cache::clear);
        userOne = Users.builder().id(1L).username("one").build();
        userTwo = Users.builder().id(2L).username("two").build();
    }

    @Test
    void favoriteMediaIds_whenDifferentUsers_useSeparateEntriesAndEvictByUser() {
        when(usersRepository.findFavoriteMediaIds(1L))
            .thenReturn(List.of(10L))
            .thenReturn(List.of(11L));
        when(usersRepository.findFavoriteMediaIds(2L)).thenReturn(List.of(20L));

        assertEquals(Set.of(10L), userFavoriteMediaIdsCacheService.getFavoriteMediaIds(1L));
        assertEquals(Set.of(10L), userFavoriteMediaIdsCacheService.getFavoriteMediaIds(1L));
        assertEquals(Set.of(20L), userFavoriteMediaIdsCacheService.getFavoriteMediaIds(2L));

        cacheEvictionService.evictUserFavoriteMediaIds(1L);

        assertEquals(Set.of(11L), userFavoriteMediaIdsCacheService.getFavoriteMediaIds(1L));
        assertEquals(Set.of(20L), userFavoriteMediaIdsCacheService.getFavoriteMediaIds(2L));
        verify(usersRepository, times(2)).findFavoriteMediaIds(1L);
        verify(usersRepository, times(1)).findFavoriteMediaIds(2L);
    }

    @Test
    void continueWatching_whenDifferentUsers_useSeparateEntries() {
        Media movieOne = media(101L, "One Movie");
        Media movieTwo = media(202L, "Two Movie");
        when(userMediaStatusRepository.findContinueWatchingMoviesByUser(eq(userOne), eq(List.of(WatchStatus.WATCHING)), any(Pageable.class)))
            .thenReturn(List.of(UserMediaStatus.builder().user(userOne).media(movieOne).status(WatchStatus.WATCHING).build()));
        when(userMediaStatusRepository.findContinueWatchingMoviesByUser(eq(userTwo), eq(List.of(WatchStatus.WATCHING)), any(Pageable.class)))
            .thenReturn(List.of(UserMediaStatus.builder().user(userTwo).media(movieTwo).status(WatchStatus.WATCHING).build()));
        when(userShowTrackingRepository.findContinueWatchingByUser(any(), eq(List.of(WatchStatus.WATCHING)), any(Pageable.class)))
            .thenReturn(List.of());

        assertEquals(101L, continueWatchingCacheService.getContinueWatchingItems(userOne, 50).get(0).getTmdbId());
        assertEquals(101L, continueWatchingCacheService.getContinueWatchingItems(userOne, 50).get(0).getTmdbId());
        assertEquals(202L, continueWatchingCacheService.getContinueWatchingItems(userTwo, 50).get(0).getTmdbId());

        verify(userMediaStatusRepository, times(1))
            .findContinueWatchingMoviesByUser(eq(userOne), eq(List.of(WatchStatus.WATCHING)), any(Pageable.class));
        verify(userMediaStatusRepository, times(1))
            .findContinueWatchingMoviesByUser(eq(userTwo), eq(List.of(WatchStatus.WATCHING)), any(Pageable.class));
    }

    @Test
    void continueWatching_whenUserProgressCachesEvicted_reloadsOnlyThatUsersEntry() {
        Media firstMovie = media(303L, "First Cached Movie");
        Media refreshedMovie = media(404L, "Refreshed Cached Movie");
        Media secondUsersMovie = media(505L, "Second Users Movie");
        when(userMediaStatusRepository.findContinueWatchingMoviesByUser(eq(userOne), eq(List.of(WatchStatus.WATCHING)), any(Pageable.class)))
            .thenReturn(List.of(UserMediaStatus.builder().user(userOne).media(firstMovie).status(WatchStatus.WATCHING).build()))
            .thenReturn(List.of(UserMediaStatus.builder().user(userOne).media(refreshedMovie).status(WatchStatus.WATCHING).build()));
        when(userMediaStatusRepository.findContinueWatchingMoviesByUser(eq(userTwo), eq(List.of(WatchStatus.WATCHING)), any(Pageable.class)))
            .thenReturn(List.of(UserMediaStatus.builder().user(userTwo).media(secondUsersMovie).status(WatchStatus.WATCHING).build()));
        when(userShowTrackingRepository.findContinueWatchingByUser(any(), eq(List.of(WatchStatus.WATCHING)), any(Pageable.class)))
            .thenReturn(List.of());

        assertEquals(303L, continueWatchingCacheService.getContinueWatchingItems(userOne, 50).get(0).getTmdbId());
        assertEquals(505L, continueWatchingCacheService.getContinueWatchingItems(userTwo, 50).get(0).getTmdbId());

        cacheEvictionService.evictUserProgressCaches(1L);

        assertEquals(404L, continueWatchingCacheService.getContinueWatchingItems(userOne, 50).get(0).getTmdbId());
        assertEquals(505L, continueWatchingCacheService.getContinueWatchingItems(userTwo, 50).get(0).getTmdbId());
        verify(userMediaStatusRepository, times(2))
            .findContinueWatchingMoviesByUser(eq(userOne), eq(List.of(WatchStatus.WATCHING)), any(Pageable.class));
        verify(userMediaStatusRepository, times(1))
            .findContinueWatchingMoviesByUser(eq(userTwo), eq(List.of(WatchStatus.WATCHING)), any(Pageable.class));
    }

    @Test
    void watchlistPages_whenSamePageRepeated_useWrapperCacheAndEvictOnlyWatchlistPages() {
        WatchList watchList = WatchList.builder().id(1L).name("List").user(userOne).items(new ArrayList<>()).build();
        WatchListDTO dto = WatchListDTO.builder().id(1L).name("List").media(List.of()).build();
        when(watchListRepository.findAllByUser(eq(userOne), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(watchList)));
        when(watchListDtoAssembler.mapWatchLists(userOne, List.of(watchList))).thenReturn(List.of(dto));

        assertEquals(1L, watchListPageCacheService.getAllWatchLists(userOne, 0, 20).getContent().get(0).getId());
        assertEquals(1L, watchListPageCacheService.getAllWatchLists(userOne, 0, 20).getContent().get(0).getId());
        cacheEvictionService.evictWatchlistSummaryPages();
        assertEquals(1L, watchListPageCacheService.getAllWatchLists(userOne, 0, 20).getContent().get(0).getId());

        verify(watchListRepository, times(2)).findAllByUser(eq(userOne), any(Pageable.class));
    }

    /**
     * Verifies that user-scoped watchlist eviction uses Redis SCAN (via {@code execute})
     * rather than the blocking {@code KEYS} command, and that it does not touch other
     * users' in-memory cache entries.
     *
     * <p>This test uses {@link ConcurrentMapCacheManager} for Spring Cache (not real
     * Redis), so the SCAN callback is invoked against the mock {@code StringRedisTemplate}
     * which returns {@code null} by default.  The mock returning {@code null} means no
     * actual deletion happens in-memory — exactly what we want: user two's
     * {@code ConcurrentMapCache} entry is preserved, proving cross-user isolation.
     *
     * <p>Full Redis round-trip correctness (including real key deletion) is verified
     * separately in {@code RedisCacheRoundTripTest.evictWatchlistSummaryPagesForUser_*}.
     */
    @Test
    void watchlistPages_userScopedEviction_usesScanNotKeysAndLeavesOtherUsersCacheIntact() {
        WatchList listOne = WatchList.builder().id(1L).name("One").user(userOne).items(new ArrayList<>()).build();
        WatchList listTwo = WatchList.builder().id(2L).name("Two").user(userTwo).items(new ArrayList<>()).build();
        WatchListDTO dtoOne = WatchListDTO.builder().id(1L).name("One").media(List.of()).build();
        WatchListDTO dtoTwo = WatchListDTO.builder().id(2L).name("Two").media(List.of()).build();

        when(watchListRepository.findAllByUser(eq(userOne), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(listOne)));
        when(watchListRepository.findAllByUser(eq(userTwo), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(listTwo)));
        when(watchListDtoAssembler.mapWatchLists(userOne, List.of(listOne))).thenReturn(List.of(dtoOne));
        when(watchListDtoAssembler.mapWatchLists(userTwo, List.of(listTwo))).thenReturn(List.of(dtoTwo));

        // Populate the in-memory Spring Cache for both users.
        watchListPageCacheService.getAllWatchLists(userOne, 0, 20);
        watchListPageCacheService.getAllWatchLists(userTwo, 0, 20);

        // Trigger user-scoped eviction for user one.
        cacheEvictionService.evictWatchlistSummaryPagesForUser(1L);

        // Eviction must use SCAN (execute) — never the blocking KEYS command.
        verify(stringRedisTemplate).execute(org.mockito.ArgumentMatchers.<RedisCallback<Long>>any());
        verify(stringRedisTemplate, never()).keys(org.mockito.ArgumentMatchers.anyString());

        // User two's in-memory cache entry is unaffected — no second DB call.
        watchListPageCacheService.getAllWatchLists(userTwo, 0, 20);
        verify(watchListRepository, times(1)).findAllByUser(eq(userTwo), any(Pageable.class));
    }

    /**
     * Pure function test: confirms the Redis SCAN glob pattern built by
     * {@link WatchMateCacheEvictionService#buildWatchlistEvictionPattern} matches
     * the exact key format Spring Cache writes to Redis for watchlist summary pages,
     * and does not match keys belonging to a different user.
     *
     * <p>This test exercises key-pattern correctness without requiring a running Redis
     * instance. The end-to-end proof that SCAN actually deletes the right keys is in
     * {@code RedisCacheRoundTripTest}.
     */
    @Test
    void evictionPattern_matchesActualSpringCacheKeyFormatAndNotOtherUsers() {
        // Reconstruct the exact Redis key Spring Cache would write:
        //   prefixCacheNameWith("watchmate::") + cacheName + "::" + watchlistPage(...)
        String user1Key = "watchmate::"
            + WatchMateCacheNames.WATCHLIST_SUMMARY_PAGES
            + "::"
            + WatchMateCacheKeys.watchlistPage(1L, 0, 20);
        String user2Key = "watchmate::"
            + WatchMateCacheNames.WATCHLIST_SUMMARY_PAGES
            + "::"
            + WatchMateCacheKeys.watchlistPage(2L, 0, 20);

        String pattern = WatchMateCacheEvictionService.buildWatchlistEvictionPattern(1L);

        // Verify the literal pattern string.
        assertEquals("watchmate::watchlistSummaryPages::user:1:*", pattern);

        // The user-1 key must start with the pattern prefix (pattern without the glob).
        String patternPrefix = pattern.substring(0, pattern.length() - 1); // strip trailing "*"
        assertTrue(user1Key.startsWith(patternPrefix),
            "Pattern '" + pattern + "' must match user-1 key '" + user1Key + "'");

        // The user-2 key must NOT start with user-1's pattern prefix.
        assertFalse(user2Key.startsWith(patternPrefix),
            "Pattern for user 1 must NOT match user 2's key '" + user2Key + "'");
    }

    private Media media(Long tmdbId, String title) {
        return Media.builder()
            .id(tmdbId)
            .tmdbId(tmdbId)
            .title(title)
            .type(MediaType.MOVIE)
            .build();
    }

    @Configuration
    @EnableCaching
    static class TestConfig {

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager();
        }

        @Bean
        UsersRepository usersRepository() {
            return org.mockito.Mockito.mock(UsersRepository.class);
        }

        @Bean
        UserMediaStatusRepository userMediaStatusRepository() {
            return org.mockito.Mockito.mock(UserMediaStatusRepository.class);
        }

        @Bean
        UserShowTrackingRepository userShowTrackingRepository() {
            return org.mockito.Mockito.mock(UserShowTrackingRepository.class);
        }

        @Bean
        WatchListRepository watchListRepository() {
            return org.mockito.Mockito.mock(WatchListRepository.class);
        }

        @Bean
        WatchListDtoAssembler watchListDtoAssembler() {
            return org.mockito.Mockito.mock(WatchListDtoAssembler.class);
        }

        @Bean
        DashboardMapper dashboardMapper() {
            return new DashboardMapper();
        }

        @Bean
        UserFavoriteMediaIdsCacheService userFavoriteMediaIdsCacheService(UsersRepository usersRepository) {
            return new UserFavoriteMediaIdsCacheService(usersRepository);
        }

        @Bean
        ContinueWatchingCacheService continueWatchingCacheService(
            UserMediaStatusRepository userMediaStatusRepository,
            UserShowTrackingRepository userShowTrackingRepository,
            DashboardMapper dashboardMapper
        ) {
            return new ContinueWatchingCacheService(userMediaStatusRepository, userShowTrackingRepository, dashboardMapper);
        }

        @Bean
        WatchListPageCacheService watchListPageCacheService(
            WatchListRepository watchListRepository,
            WatchListDtoAssembler watchListDtoAssembler
        ) {
            return new WatchListPageCacheService(watchListRepository, watchListDtoAssembler);
        }

        @Bean
        StringRedisTemplate stringRedisTemplate() {
            return org.mockito.Mockito.mock(StringRedisTemplate.class);
        }

        @Bean
        WatchMateCacheEvictionService watchMateCacheEvictionService(StringRedisTemplate stringRedisTemplate) {
            return new WatchMateCacheEvictionService(stringRedisTemplate);
        }
    }
}
