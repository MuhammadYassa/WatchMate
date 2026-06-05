package com.project.watchmate.show.tracking.application;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.project.watchmate.media.catalog.domain.ShowEpisode;
import com.project.watchmate.show.tracking.domain.ShowTrackingCalculationResult;
import com.project.watchmate.show.tracking.domain.UserEpisodeWatch;
import com.project.watchmate.show.tracking.domain.UserShowTracking;
import com.project.watchmate.media.catalog.domain.WatchStatus;

@Component
public class ShowStatusCalculator {

    public ShowTrackingCalculationResult calculate(
        UserShowTracking tracking,
        List<UserEpisodeWatch> watchedRows,
        List<ShowEpisode> totalEligibleEpisodes,
        List<ShowEpisode> airedEligibleEpisodes,
        boolean airedMetadataComplete,
        boolean totalMetadataComplete,
        boolean endedShow
    ) {
        List<UserEpisodeWatch> eligibleWatches = watchedRows.stream()
            .filter(this::isEligibleEpisodeWatch)
            .sorted(Comparator.comparing(UserEpisodeWatch::getWatchedAt, Comparator.nullsLast(LocalDateTime::compareTo))
                .thenComparing(UserEpisodeWatch::getSeasonNumber, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(UserEpisodeWatch::getEpisodeNumber, Comparator.nullsLast(Integer::compareTo)))
            .toList();

        int watchedEligibleCount = eligibleWatches.size();
        int seasonsCompletedCount = countCompletedSeasons(totalEligibleEpisodes, eligibleWatches);
        LocalDateTime lastWatchedAt = eligibleWatches.stream()
            .map(UserEpisodeWatch::getWatchedAt)
            .filter(Objects::nonNull)
            .max(LocalDateTime::compareTo)
            .orElse(null);
        UserEpisodeWatch latestWatched = eligibleWatches.isEmpty() ? null : eligibleWatches.get(eligibleWatches.size() - 1);

        WatchStatus resolvedStatus = resolveStatus(
            tracking,
            watchedEligibleCount,
            airedEligibleEpisodes.size(),
            totalEligibleEpisodes.size(),
            airedMetadataComplete,
            totalMetadataComplete,
            endedShow
        );

        return new ShowTrackingCalculationResult(
            resolvedStatus,
            watchedEligibleCount,
            seasonsCompletedCount,
            lastWatchedAt,
            latestWatched == null ? null : latestWatched.getSeasonNumber(),
            latestWatched == null ? null : latestWatched.getEpisodeNumber()
        );
    }

    private WatchStatus resolveStatus(
        UserShowTracking tracking,
        int watchedEligibleCount,
        int airedEligibleCount,
        int totalEligibleCount,
        boolean airedMetadataComplete,
        boolean totalMetadataComplete,
        boolean endedShow
    ) {
        if (tracking == null) {
            return WatchStatus.NONE;
        }

        if (watchedEligibleCount <= 0) {
            if (tracking.getStatus() == WatchStatus.TO_WATCH
                && tracking.getWatchPositionSeason() == null
                && tracking.getWatchPositionEpisode() == null) {
                return WatchStatus.TO_WATCH;
            }
            return WatchStatus.WATCHING;
        }

        if (endedShow && totalMetadataComplete && totalEligibleCount > 0 && watchedEligibleCount >= totalEligibleCount) {
            return WatchStatus.WATCHED;
        }

        if (!endedShow && airedMetadataComplete && airedEligibleCount > 0 && watchedEligibleCount >= airedEligibleCount) {
            return WatchStatus.UP_TO_DATE;
        }

        return WatchStatus.WATCHING;
    }

    private int countCompletedSeasons(List<ShowEpisode> eligibleEpisodes, List<UserEpisodeWatch> watchedRows) {
        Map<Integer, Long> watchedPerSeason = watchedRows.stream()
            .collect(Collectors.groupingBy(UserEpisodeWatch::getSeasonNumber, Collectors.counting()));
        Map<Integer, Long> eligiblePerSeason = eligibleEpisodes.stream()
            .collect(Collectors.groupingBy(ShowEpisode::getSeasonNumber, Collectors.counting()));

        int completed = 0;
        for (Map.Entry<Integer, Long> entry : eligiblePerSeason.entrySet()) {
            if (entry.getValue() > 0 && watchedPerSeason.getOrDefault(entry.getKey(), 0L) >= entry.getValue()) {
                completed++;
            }
        }
        return completed;
    }

    private boolean isEligibleEpisodeWatch(UserEpisodeWatch row) {
        return row.getSeasonNumber() != null
            && row.getSeasonNumber() > 0
            && row.getEpisodeNumber() != null;
    }
}




