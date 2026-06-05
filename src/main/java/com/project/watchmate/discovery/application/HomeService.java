package com.project.watchmate.discovery.application;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.watchmate.discovery.dto.HomeResponseDTO;
import com.project.watchmate.discovery.dto.HomeStatusDTO;
import com.project.watchmate.discovery.domain.ContentSyncResult;
import com.project.watchmate.discovery.domain.ContentSyncStatus;
import com.project.watchmate.media.catalog.domain.Genre;
import com.project.watchmate.media.catalog.domain.MediaType;
import com.project.watchmate.discovery.persistence.ContentSyncStatusRepository;
import com.project.watchmate.media.catalog.persistence.GenreRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HomeService {

    private final DiscoverService discoverService;

    private final GenreRepository genreRepository;

    private final ContentSyncStatusRepository contentSyncStatusRepository;

    @Transactional(readOnly = true)
    public HomeResponseDTO getHome() {
        return HomeResponseDTO.builder()
            .trendingMovies(discoverService.getTrendingMovies())
            .trendingShows(discoverService.getTrendingShows())
            .popularNow(discoverService.getPopularNow())
            .airingToday(discoverService.getAiringToday())
            .upcoming(discoverService.getUpcoming())
            .recommendedLater(discoverService.getRecommendedLater())
            .movieGenres(genreNamesFor(MediaType.MOVIE))
            .showGenres(genreNamesFor(MediaType.SHOW))
            .build();
    }

    @Transactional(readOnly = true)
    public HomeStatusDTO getHomeStatus() {
        ContentSyncStatus status = contentSyncStatusRepository.findById(CuratedContentSyncService.STATUS_KEY)
            .orElse(ContentSyncStatus.builder()
                .statusKey(CuratedContentSyncService.STATUS_KEY)
                .lastResult(ContentSyncResult.NEVER)
                .trendingMoviesCount(0)
                .trendingShowsCount(0)
                .popularNowCount(0)
                .airingTodayCount(0)
                .upcomingCount(0)
                .recommendedLaterCount(0)
                .build());

        return HomeStatusDTO.builder()
            .lastAttemptedAt(status.getLastAttemptedAt())
            .lastSuccessfulAt(status.getLastSuccessfulAt())
            .lastFailedAt(status.getLastFailedAt())
            .lastResult(status.getLastResult())
            .lastErrorMessage(status.getLastErrorMessage())
            .trendingMoviesCount(status.getTrendingMoviesCount())
            .trendingShowsCount(status.getTrendingShowsCount())
            .popularNowCount(status.getPopularNowCount())
            .airingTodayCount(status.getAiringTodayCount())
            .upcomingCount(status.getUpcomingCount())
            .recommendedLaterCount(status.getRecommendedLaterCount())
            .build();
    }

    private List<String> genreNamesFor(MediaType mediaType) {
        return genreRepository.findCurrentByMediaTypeOrderByNameAsc(mediaType).stream()
            .map(Genre::getName)
            .toList();
    }
}




