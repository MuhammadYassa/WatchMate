package com.project.watchmate.discovery.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.project.watchmate.discovery.dto.DiscoveryMediaItemDTO;
import com.project.watchmate.discovery.dto.HomeResponseDTO;
import com.project.watchmate.discovery.dto.HomeStatusDTO;
import com.project.watchmate.discovery.domain.ContentSyncResult;
import com.project.watchmate.discovery.domain.ContentSyncStatus;
import com.project.watchmate.media.catalog.domain.Genre;
import com.project.watchmate.media.catalog.domain.MediaType;
import com.project.watchmate.discovery.persistence.ContentSyncStatusRepository;
import com.project.watchmate.media.catalog.persistence.GenreRepository;

@ExtendWith(MockitoExtension.class)
class HomeServiceTest {

    @Mock
    private DiscoverService discoverService;

    @Mock
    private GenreRepository genreRepository;

    @Mock
    private ContentSyncStatusRepository contentSyncStatusRepository;

    @InjectMocks
    private HomeService homeService;

    @Test
    void getHome_MapsBucketsFromLocalServices() {
        DiscoveryMediaItemDTO item = DiscoveryMediaItemDTO.builder().tmdbId(1L).title("Cached").type(MediaType.MOVIE).build();
        when(discoverService.getTrendingMovies()).thenReturn(List.of(item));
        when(discoverService.getTrendingShows()).thenReturn(List.of());
        when(discoverService.getPopularNow()).thenReturn(List.of());
        when(discoverService.getAiringToday()).thenReturn(List.of());
        when(discoverService.getUpcoming()).thenReturn(List.of());
        when(discoverService.getRecommendedLater()).thenReturn(List.of());
        when(genreRepository.findCurrentByMediaTypeOrderByNameAsc(MediaType.MOVIE)).thenReturn(List.of(
            Genre.builder().tmdbGenreId(28L).name("Action").mediaType(MediaType.MOVIE).build()
        ));
        when(genreRepository.findCurrentByMediaTypeOrderByNameAsc(MediaType.SHOW)).thenReturn(List.of(
            Genre.builder().tmdbGenreId(18L).name("Drama").mediaType(MediaType.SHOW).build()
        ));

        HomeResponseDTO result = homeService.getHome();

        assertEquals(1, result.getTrendingMovies().size());
        assertEquals(List.of("Action"), result.getMovieGenres());
        assertEquals(List.of("Drama"), result.getShowGenres());
    }

    @Test
    void getHomeStatus_ReturnsPersistedStatus() {
        LocalDateTime now = LocalDateTime.now();
        when(contentSyncStatusRepository.findById(CuratedContentSyncService.STATUS_KEY))
            .thenReturn(Optional.of(ContentSyncStatus.builder()
                .statusKey(CuratedContentSyncService.STATUS_KEY)
                .lastAttemptedAt(now)
                .lastSuccessfulAt(now)
                .lastResult(ContentSyncResult.SUCCESS)
                .trendingMoviesCount(20)
                .trendingShowsCount(20)
                .popularNowCount(20)
                .airingTodayCount(20)
                .upcomingCount(20)
                .recommendedLaterCount(10)
                .build()));

        HomeStatusDTO result = homeService.getHomeStatus();

        assertEquals(ContentSyncResult.SUCCESS, result.getLastResult());
        assertEquals(20, result.getPopularNowCount());
        assertEquals(10, result.getRecommendedLaterCount());
    }
}




