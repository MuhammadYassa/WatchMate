package com.project.watchmate.common.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import com.project.watchmate.common.mapper.WatchMateMapper;
import com.project.watchmate.media.catalog.application.MediaResolutionService;
import com.project.watchmate.media.catalog.application.UserWatchStatusResolver;
import com.project.watchmate.media.catalog.domain.Media;
import com.project.watchmate.media.catalog.domain.MediaType;
import com.project.watchmate.media.catalog.domain.ShowEpisode;
import com.project.watchmate.media.catalog.domain.ShowSeason;
import com.project.watchmate.media.catalog.domain.WatchStatus;
import com.project.watchmate.media.catalog.persistence.MediaRepository;
import com.project.watchmate.media.tmdb.application.TmdbService;
import com.project.watchmate.media.tmdb.dto.TmdbTvDetailsDTO;
import com.project.watchmate.movie.application.MediaService;
import com.project.watchmate.movie.application.PublicMediaDetailBaseCacheService;
import com.project.watchmate.movie.dto.MovieDetailsDTO;
import com.project.watchmate.movie.dto.PublicMovieDetailBaseDTO;
import com.project.watchmate.movie.tracking.persistence.UserMediaStatusRepository;
import com.project.watchmate.review.persistence.ReviewRepository;
import com.project.watchmate.show.catalog.application.ShowCatalogService;
import com.project.watchmate.show.metadata.application.PublicShowMetadataCacheService;
import com.project.watchmate.show.metadata.application.ShowMetadataService;
import com.project.watchmate.show.metadata.dto.PublicShowMetadataDTO;
import com.project.watchmate.show.metadata.dto.PublicShowSeasonMetadataDTO;
import com.project.watchmate.show.metadata.dto.ShowDetailsDTO;
import com.project.watchmate.show.metadata.dto.ShowSeasonsDetailsDTO;
import com.project.watchmate.show.metadata.mapper.ShowMetadataMapper;
import com.project.watchmate.show.tracking.domain.UserEpisodeWatch;
import com.project.watchmate.show.tracking.domain.UserShowTracking;
import com.project.watchmate.show.tracking.persistence.UserShowTrackingRepository;
import com.project.watchmate.user.domain.Users;
import com.project.watchmate.user.persistence.UsersRepository;

@SpringJUnitConfig(PublicDetailOverlayCacheBehaviorTest.TestConfig.class)
class PublicDetailOverlayCacheBehaviorTest {

    @Autowired
    private MediaService mediaService;

    @Autowired
    private ShowMetadataService showMetadataService;

    @Autowired
    private MediaRepository mediaRepository;

    @Autowired
    private MediaResolutionService mediaResolutionService;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private UserWatchStatusResolver userWatchStatusResolver;

    @Autowired
    private TmdbService tmdbService;

    @Autowired
    private ShowCatalogService showCatalogService;

    @Autowired
    private UserShowTrackingRepository userShowTrackingRepository;

    @Autowired
    private CacheManager cacheManager;

    private Media movie;

    private Media show;

    private Users userOne;

    private Users userTwo;

    @BeforeEach
    void setUp() {
        reset(
            mediaRepository,
            mediaResolutionService,
            usersRepository,
            reviewRepository,
            userWatchStatusResolver,
            tmdbService,
            showCatalogService,
            userShowTrackingRepository
        );
        cacheManager.getCacheNames().stream()
            .map(cacheManager::getCache)
            .filter(java.util.Objects::nonNull)
            .forEach(org.springframework.cache.Cache::clear);

        movie = Media.builder()
            .id(10L)
            .tmdbId(100L)
            .title("Cached Movie")
            .type(MediaType.MOVIE)
            .genres(List.of())
            .build();
        show = Media.builder()
            .id(20L)
            .tmdbId(200L)
            .title("Cached Show")
            .type(MediaType.SHOW)
            .genres(List.of())
            .build();
        userOne = Users.builder().id(1L).username("one").favorites(new ArrayList<>(List.of(movie, show))).build();
        userTwo = Users.builder().id(2L).username("two").favorites(new ArrayList<>()).build();
    }

    @Test
    void movieDetails_usesOnePublicBaseCacheEntryButDifferentUserOverlays() {
        when(mediaResolutionService.resolveMediaByTmdbId(100L, MediaType.MOVIE)).thenReturn(movie);
        when(mediaRepository.findByTmdbIdAndType(100L, MediaType.MOVIE)).thenReturn(Optional.of(movie));
        when(usersRepository.findByIdWithFavorites(1L)).thenReturn(Optional.of(userOne));
        when(usersRepository.findByIdWithFavorites(2L)).thenReturn(Optional.of(userTwo));
        when(reviewRepository.findByMedia(movie)).thenReturn(List.of());
        when(userWatchStatusResolver.resolveWatchStatus(userOne, movie)).thenReturn(WatchStatus.WATCHED);
        when(userWatchStatusResolver.resolveWatchStatus(userTwo, movie)).thenReturn(WatchStatus.TO_WATCH);

        MovieDetailsDTO first = mediaService.getMovieDetails(100L, userOne);
        MovieDetailsDTO second = mediaService.getMovieDetails(100L, userTwo);

        assertEquals(Boolean.TRUE, first.getIsFavourited());
        assertEquals(WatchStatus.WATCHED, first.getWatchStatus());
        assertEquals(Boolean.FALSE, second.getIsFavourited());
        assertEquals(WatchStatus.TO_WATCH, second.getWatchStatus());
        verify(mediaRepository, times(1)).findByTmdbIdAndType(100L, MediaType.MOVIE);

        Object cachedValue = cacheManager.getCache(WatchMateCacheNames.PUBLIC_MEDIA_DETAIL_BASE)
            .get(WatchMateCacheKeys.media(MediaType.MOVIE, 100L))
            .get();
        assertInstanceOf(PublicMovieDetailBaseDTO.class, cachedValue);
    }

    @Test
    void showDetails_usesOnePublicMetadataCacheEntryButDifferentUserOverlays() {
        TmdbTvDetailsDTO tvDetails = TmdbTvDetailsDTO.builder().id(200L).name("Cached Show").build();
        when(showCatalogService.validateShowType(MediaType.SHOW)).thenReturn(MediaType.SHOW);
        when(showCatalogService.findImportedShow(200L)).thenReturn(show);
        when(tmdbService.fetchTvDetails(200L)).thenReturn(tvDetails);
        when(tmdbService.refreshShowSnapshot(show, tvDetails)).thenReturn(show);
        when(usersRepository.findByIdWithFavorites(1L)).thenReturn(Optional.of(userOne));
        when(usersRepository.findByIdWithFavorites(2L)).thenReturn(Optional.of(userTwo));
        when(reviewRepository.findByMedia(show)).thenReturn(List.of());
        when(userShowTrackingRepository.findByUserAndMedia(userOne, show))
            .thenReturn(Optional.of(UserShowTracking.builder().user(userOne).media(show).status(WatchStatus.WATCHING).build()));
        when(userShowTrackingRepository.findByUserAndMedia(userTwo, show))
            .thenReturn(Optional.of(UserShowTracking.builder().user(userTwo).media(show).status(WatchStatus.TO_WATCH).build()));

        ShowDetailsDTO first = showMetadataService.getShowDetails(200L, MediaType.SHOW, userOne);
        ShowDetailsDTO second = showMetadataService.getShowDetails(200L, MediaType.SHOW, userTwo);

        assertEquals(Boolean.TRUE, first.getIsFavourited());
        assertEquals(WatchStatus.WATCHING, first.getWatchStatus());
        assertEquals(Boolean.FALSE, second.getIsFavourited());
        assertEquals(WatchStatus.TO_WATCH, second.getWatchStatus());
        verify(tmdbService, times(1)).fetchTvDetails(200L);

        Object cachedValue = cacheManager.getCache(WatchMateCacheNames.PUBLIC_SHOW_METADATA)
            .get(WatchMateCacheKeys.show(200L))
            .get();
        assertInstanceOf(PublicShowMetadataDTO.class, cachedValue);
    }

    @Test
    void seasonDetails_usesOnePublicSeasonCacheEntryButDifferentWatchedOverlays() {
        ShowSeason season = ShowSeason.builder()
            .media(show)
            .seasonNumber(1)
            .name("Season 1")
            .episodeCount(1)
            .lastTmdbSyncAt(LocalDateTime.now())
            .build();
        ShowEpisode episode = ShowEpisode.builder()
            .media(show)
            .seasonNumber(1)
            .episodeNumber(1)
            .tmdbEpisodeId(501L)
            .title("Episode 1")
            .airDate(LocalDate.of(2020, 1, 1))
            .build();
        UserShowTracking watchedTracking = UserShowTracking.builder()
            .user(userOne)
            .media(show)
            .status(WatchStatus.WATCHING)
            .episodeWatches(new ArrayList<>())
            .build();
        watchedTracking.getEpisodeWatches().add(UserEpisodeWatch.builder()
            .userShowTracking(watchedTracking)
            .seasonNumber(1)
            .episodeNumber(1)
            .watchedAt(LocalDateTime.now())
            .build());

        when(showCatalogService.validateShowType(MediaType.SHOW)).thenReturn(MediaType.SHOW);
        when(showCatalogService.ensureBasicShowImported(200L)).thenReturn(show);
        when(showCatalogService.ensureSeasonCached(show, 200L, 1))
            .thenReturn(new ShowCatalogService.CachedSeasonData(season, List.of(episode)));
        when(showCatalogService.findImportedShow(200L)).thenReturn(show);
        when(userShowTrackingRepository.findWithEpisodeWatchesByUserAndMedia(userOne, show))
            .thenReturn(Optional.of(watchedTracking));
        when(userShowTrackingRepository.findWithEpisodeWatchesByUserAndMedia(userTwo, show))
            .thenReturn(Optional.empty());

        ShowSeasonsDetailsDTO first = showMetadataService.getShowSeasonDetails(200L, 1, MediaType.SHOW, userOne);
        ShowSeasonsDetailsDTO second = showMetadataService.getShowSeasonDetails(200L, 1, MediaType.SHOW, userTwo);

        assertEquals(Boolean.TRUE, first.getEpisodes().get(0).getWatched());
        assertEquals(Boolean.FALSE, second.getEpisodes().get(0).getWatched());
        assertEquals(501L, first.getEpisodes().get(0).getTmdbEpisodeId());
        assertEquals(501L, second.getEpisodes().get(0).getTmdbEpisodeId());
        verify(showCatalogService, times(1)).ensureSeasonCached(show, 200L, 1);

        Object cachedValue = cacheManager.getCache(WatchMateCacheNames.PUBLIC_SEASON_METADATA)
            .get(WatchMateCacheKeys.season(200L, 1))
            .get();
        assertInstanceOf(PublicShowSeasonMetadataDTO.class, cachedValue);
        PublicShowSeasonMetadataDTO cachedSeason = (PublicShowSeasonMetadataDTO) cachedValue;
        assertEquals(501L, cachedSeason.getEpisodes().get(0).getTmdbEpisodeId());
    }

    @Configuration
    @EnableCaching
    static class TestConfig {

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager();
        }

        @Bean
        MediaRepository mediaRepository() {
            return org.mockito.Mockito.mock(MediaRepository.class);
        }

        @Bean
        MediaResolutionService mediaResolutionService() {
            return org.mockito.Mockito.mock(MediaResolutionService.class);
        }

        @Bean
        UsersRepository usersRepository() {
            return org.mockito.Mockito.mock(UsersRepository.class);
        }

        @Bean
        ReviewRepository reviewRepository() {
            return org.mockito.Mockito.mock(ReviewRepository.class);
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
        UserWatchStatusResolver userWatchStatusResolver() {
            return org.mockito.Mockito.mock(UserWatchStatusResolver.class);
        }

        @Bean
        TmdbService tmdbService() {
            return org.mockito.Mockito.mock(TmdbService.class);
        }

        @Bean
        ShowCatalogService showCatalogService() {
            return org.mockito.Mockito.mock(ShowCatalogService.class);
        }

        @Bean
        ShowMetadataMapper showMetadataMapper() {
            return org.mockito.Mockito.mock(ShowMetadataMapper.class);
        }

        @Bean
        WatchMateMapper watchMateMapper() {
            return new WatchMateMapper();
        }

        @Bean
        PublicMediaDetailBaseCacheService publicMediaDetailBaseCacheService(
            MediaRepository mediaRepository,
            TmdbService tmdbService
        ) {
            return new PublicMediaDetailBaseCacheService(mediaRepository, tmdbService);
        }

        @Bean
        PublicShowMetadataCacheService publicShowMetadataCacheService(
            TmdbService tmdbService,
            ShowCatalogService showCatalogService
        ) {
            return new PublicShowMetadataCacheService(tmdbService, showCatalogService);
        }

        @Bean
        MediaService mediaService(
            MediaResolutionService mediaResolutionService,
            MediaRepository mediaRepository,
            UsersRepository usersRepository,
            WatchMateMapper watchMateMapper,
            ReviewRepository reviewRepository,
            UserMediaStatusRepository userMediaStatusRepository,
            UserShowTrackingRepository userShowTrackingRepository,
            UserWatchStatusResolver userWatchStatusResolver,
            PublicMediaDetailBaseCacheService publicMediaDetailBaseCacheService
        ) {
            return new MediaService(
                mediaResolutionService,
                mediaRepository,
                usersRepository,
                watchMateMapper,
                reviewRepository,
                userMediaStatusRepository,
                userShowTrackingRepository,
                userWatchStatusResolver,
                publicMediaDetailBaseCacheService
            );
        }

        @Bean
        ShowMetadataService showMetadataService(
            ShowCatalogService showCatalogService,
            TmdbService tmdbService,
            ShowMetadataMapper showMetadataMapper,
            WatchMateMapper watchMateMapper,
            UsersRepository usersRepository,
            ReviewRepository reviewRepository,
            UserShowTrackingRepository userShowTrackingRepository,
            PublicShowMetadataCacheService publicShowMetadataCacheService
        ) {
            return new ShowMetadataService(
                showCatalogService,
                tmdbService,
                showMetadataMapper,
                watchMateMapper,
                usersRepository,
                reviewRepository,
                userShowTrackingRepository,
                publicShowMetadataCacheService
            );
        }
    }
}
