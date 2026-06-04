package com.project.watchmate.Services;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.project.watchmate.Models.ShowEpisode;
import com.project.watchmate.Models.UserEpisodeWatch;
import com.project.watchmate.Models.UserShowTracking;
import com.project.watchmate.Models.WatchStatus;

class ShowStatusCalculatorTest {

    private ShowStatusCalculator showStatusCalculator;

    @BeforeEach
    void setUp() {
        showStatusCalculator = new ShowStatusCalculator();
    }

    @Test
    void calculate_whenNoTracking_returnsNone() {
        ShowTrackingCalculationResult result = showStatusCalculator.calculate(
            null,
            List.of(),
            List.of(),
            List.of(),
            false,
            false,
            false
        );

        assertEquals(WatchStatus.NONE, result.status());
    }

    @Test
    void calculate_whenToWatchTrackingWithoutEpisodes_returnsToWatch() {
        ShowTrackingCalculationResult result = showStatusCalculator.calculate(
            tracking(WatchStatus.TO_WATCH, null, null),
            List.of(),
            List.of(),
            List.of(),
            false,
            false,
            false
        );

        assertEquals(WatchStatus.TO_WATCH, result.status());
    }

    @Test
    void calculate_whenWatchingTrackingWithoutEpisodes_returnsWatching() {
        ShowTrackingCalculationResult result = showStatusCalculator.calculate(
            tracking(WatchStatus.WATCHING, 2, 1),
            List.of(),
            List.of(),
            List.of(),
            false,
            false,
            false
        );

        assertEquals(WatchStatus.WATCHING, result.status());
    }

    @Test
    void calculate_whenCaughtUpOnOngoingShowWithCompleteAiredMetadata_returnsUpToDate() {
        List<ShowEpisode> totalEpisodes = List.of(
            episode(1, 1, LocalDate.of(2020, 1, 1)),
            episode(1, 2, LocalDate.of(2020, 1, 8)),
            episode(2, 1, LocalDate.of(2020, 2, 1))
        );
        List<UserEpisodeWatch> watchedRows = List.of(
            watch(1, 1, LocalDateTime.of(2026, 5, 1, 10, 0)),
            watch(1, 2, LocalDateTime.of(2026, 5, 2, 10, 0)),
            watch(2, 1, LocalDateTime.of(2026, 5, 3, 10, 0))
        );

        ShowTrackingCalculationResult result = showStatusCalculator.calculate(
            tracking(WatchStatus.WATCHING, null, null),
            watchedRows,
            totalEpisodes,
            totalEpisodes,
            true,
            false,
            false
        );

        assertEquals(WatchStatus.UP_TO_DATE, result.status());
        assertEquals(3, result.episodesWatchedCount());
        assertEquals(2, result.seasonsCompletedCount());
        assertEquals(Integer.valueOf(2), result.latestWatchedSeasonNumber());
        assertEquals(Integer.valueOf(1), result.latestWatchedEpisodeNumber());
    }

    @Test
    void calculate_whenAllEpisodesWatchedOnEndedShowWithCompleteMetadata_returnsWatched() {
        List<ShowEpisode> totalEpisodes = List.of(
            episode(1, 1, LocalDate.of(2020, 1, 1)),
            episode(1, 2, LocalDate.of(2020, 1, 8))
        );
        List<UserEpisodeWatch> watchedRows = List.of(
            watch(1, 1, LocalDateTime.of(2026, 5, 1, 10, 0)),
            watch(1, 2, LocalDateTime.of(2026, 5, 2, 10, 0))
        );

        ShowTrackingCalculationResult result = showStatusCalculator.calculate(
            tracking(WatchStatus.WATCHING, null, null),
            watchedRows,
            totalEpisodes,
            totalEpisodes,
            true,
            true,
            true
        );

        assertEquals(WatchStatus.WATCHED, result.status());
    }

    private UserShowTracking tracking(WatchStatus status, Integer pointerSeason, Integer pointerEpisode) {
        return UserShowTracking.builder()
            .status(status)
            .watchPositionSeason(pointerSeason)
            .watchPositionEpisode(pointerEpisode)
            .build();
    }

    private UserEpisodeWatch watch(int seasonNumber, int episodeNumber, LocalDateTime watchedAt) {
        return UserEpisodeWatch.builder()
            .seasonNumber(seasonNumber)
            .episodeNumber(episodeNumber)
            .watchedAt(watchedAt)
            .build();
    }

    private ShowEpisode episode(int seasonNumber, int episodeNumber, LocalDate airDate) {
        return ShowEpisode.builder()
            .seasonNumber(seasonNumber)
            .episodeNumber(episodeNumber)
            .airDate(airDate)
            .build();
    }
}
