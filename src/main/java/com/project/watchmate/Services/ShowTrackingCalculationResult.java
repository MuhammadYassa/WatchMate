package com.project.watchmate.Services;

import java.time.LocalDateTime;

import com.project.watchmate.Models.WatchStatus;

public record ShowTrackingCalculationResult(
    WatchStatus status,
    int episodesWatchedCount,
    int seasonsCompletedCount,
    LocalDateTime lastWatchedAt,
    Integer latestWatchedSeasonNumber,
    Integer latestWatchedEpisodeNumber
) {
}
