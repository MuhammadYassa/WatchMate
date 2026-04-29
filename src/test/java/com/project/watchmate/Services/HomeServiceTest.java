package com.project.watchmate.Services;

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

import com.project.watchmate.Dto.DiscoveryMediaItemDTO;
import com.project.watchmate.Dto.HomeResponseDTO;
import com.project.watchmate.Dto.HomeStatusDTO;
import com.project.watchmate.Models.ContentSyncResult;
import com.project.watchmate.Models.ContentSyncStatus;
import com.project.watchmate.Models.MediaType;
import com.project.watchmate.Repositories.ContentSyncStatusRepository;
import com.project.watchmate.Repositories.GenreLookupRepository;

@ExtendWith(MockitoExtension.class)
class HomeServiceTest {

    @Mock
    private DiscoverService discoverService;

    @Mock
    private GenreLookupRepository genreLookupRepository;

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
        when(genreLookupRepository.findDistinctNamesOrderByNameAsc()).thenReturn(List.of("Action", "Drama"));

        HomeResponseDTO result = homeService.getHome();

        assertEquals(1, result.getTrendingMovies().size());
        assertEquals(List.of("Action", "Drama"), result.getGenres());
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
