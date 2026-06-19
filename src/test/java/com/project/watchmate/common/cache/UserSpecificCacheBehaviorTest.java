package com.project.watchmate.common.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

    private Users userOne;

    private Users userTwo;

    @BeforeEach
    void setUp() {
        reset(usersRepository, userMediaStatusRepository, userShowTrackingRepository, watchListRepository, watchListDtoAssembler);
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
        WatchMateCacheEvictionService watchMateCacheEvictionService() {
            return new WatchMateCacheEvictionService();
        }
    }
}
