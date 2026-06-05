package com.project.watchmate.show.tracking.domain;

import java.time.LocalDateTime;

import com.project.watchmate.media.catalog.domain.WatchStatus;

public record ShowTrackingCalculationResult(
    WatchStatus status,
    int episodesWatchedCount,
    int seasonsCompletedCount,
    LocalDateTime lastWatchedAt,
    Integer latestWatchedSeasonNumber,
    Integer latestWatchedEpisodeNumber
) {
}



