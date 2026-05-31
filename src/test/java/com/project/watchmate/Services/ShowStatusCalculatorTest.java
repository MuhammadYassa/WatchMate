package com.project.watchmate.Services;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.project.watchmate.Models.ShowTrackingState;
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
        WatchStatus result = showStatusCalculator.calculate(null, 0, 0, 0, false);

        assertEquals(WatchStatus.NONE, result);
    }

    @Test
    void calculate_whenToWatchTrackingWithoutEpisodes_returnsToWatch() {
        WatchStatus result = showStatusCalculator.calculate(progress(ShowTrackingState.TO_WATCH), 0, 0, 0, false);

        assertEquals(WatchStatus.TO_WATCH, result);
    }

    @Test
    void calculate_whenWatchingTrackingWithoutEpisodes_returnsWatching() {
        WatchStatus result = showStatusCalculator.calculate(progress(ShowTrackingState.WATCHING), 0, 0, 0, false);

        assertEquals(WatchStatus.WATCHING, result);
    }

    @Test
    void calculate_whenPartiallyWatched_returnsWatching() {
        WatchStatus result = showStatusCalculator.calculate(progress(ShowTrackingState.WATCHING), 2, 5, 8, false);

        assertEquals(WatchStatus.WATCHING, result);
    }

    @Test
    void calculate_whenCaughtUpOnOngoingShow_returnsUpToDate() {
        WatchStatus result = showStatusCalculator.calculate(progress(ShowTrackingState.WATCHING), 5, 5, 8, false);

        assertEquals(WatchStatus.UP_TO_DATE, result);
    }

    @Test
    void calculate_whenAllEpisodesWatchedOnEndedShow_returnsWatched() {
        WatchStatus result = showStatusCalculator.calculate(progress(ShowTrackingState.WATCHING), 8, 8, 8, true);

        assertEquals(WatchStatus.WATCHED, result);
    }

    private UserShowProgress progress(ShowTrackingState trackingState) {
        return UserShowProgress.builder()
            .trackingState(trackingState)
            .build();
    }
}
