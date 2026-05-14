package com.project.watchmate.Services;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.project.watchmate.Dto.TmdbTvDetailsDTO;
import com.project.watchmate.Dto.TmdbTvSeasonSummaryDTO;
import com.project.watchmate.Models.UserShowProgress;
import com.project.watchmate.Models.WatchStatus;

class ShowStatusCalculatorTest {

    private ShowStatusCalculator showStatusCalculator;

    @BeforeEach
    void setUp() {
        showStatusCalculator = new ShowStatusCalculator();
    }

    @Test
    void calculate_whenNoProgress_returnsNone() {
        WatchStatus result = showStatusCalculator.calculate(progress(0, null), ongoingShow(), null);

        assertEquals(WatchStatus.NONE, result);
    }

    @Test
    void calculate_whenToWatchRequestedAndNoProgress_returnsToWatch() {
        WatchStatus result = showStatusCalculator.calculate(progress(0, null), ongoingShow(), "TO_WATCH");

        assertEquals(WatchStatus.TO_WATCH, result);
    }

    @Test
    void calculate_whenPartiallyWatchedOngoingShow_returnsWatching() {
        WatchStatus result = showStatusCalculator.calculate(progress(2, 1), ongoingShow(), null);

        assertEquals(WatchStatus.WATCHING, result);
    }

    @Test
    void calculate_whenAllEpisodesWatchedOnEndedShow_returnsWatched() {
        WatchStatus result = showStatusCalculator.calculate(progress(4, 2), endedShow(), null);

        assertEquals(WatchStatus.WATCHED, result);
    }

    @Test
    void calculate_whenAllEpisodesWatchedOnOngoingShow_returnsWatching() {
        WatchStatus result = showStatusCalculator.calculate(progress(4, 2), ongoingShow(), null);

        assertEquals(WatchStatus.WATCHING, result);
    }

    private UserShowProgress progress(int episodesWatchedCount, Integer currentSeasonNumber) {
        return UserShowProgress.builder()
            .episodesWatchedCount(episodesWatchedCount)
            .currentSeasonNumber(currentSeasonNumber)
            .build();
    }

    private TmdbTvDetailsDTO ongoingShow() {
        return TmdbTvDetailsDTO.builder()
            .status("Returning Series")
            .seasons(seasons())
            .build();
    }

    private TmdbTvDetailsDTO endedShow() {
        return TmdbTvDetailsDTO.builder()
            .status("Ended")
            .seasons(seasons())
            .build();
    }

    private List<TmdbTvSeasonSummaryDTO> seasons() {
        return List.of(
            TmdbTvSeasonSummaryDTO.builder().seasonNumber(0).episodeCount(1).build(),
            TmdbTvSeasonSummaryDTO.builder().seasonNumber(1).episodeCount(2).build(),
            TmdbTvSeasonSummaryDTO.builder().seasonNumber(2).episodeCount(2).build()
        );
    }
}
