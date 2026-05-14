package com.project.watchmate.Services;

import org.springframework.stereotype.Component;

import com.project.watchmate.Dto.TmdbTvDetailsDTO;
import com.project.watchmate.Dto.TmdbTvSeasonSummaryDTO;
import com.project.watchmate.Models.UserShowProgress;
import com.project.watchmate.Models.WatchStatus;

@Component
public class ShowStatusCalculator {

    public WatchStatus calculate(UserShowProgress progress, TmdbTvDetailsDTO tvDetails, String requestedStatus) {
        if (progress.getEpisodesWatchedCount() <= 0 || progress.getCurrentSeasonNumber() == null) {
            WatchStatus fallback = parseOptionalStatus(requestedStatus);
            return fallback == WatchStatus.TO_WATCH ? WatchStatus.TO_WATCH : WatchStatus.NONE;
        }

        if (isEndedShow(tvDetails) && progress.getEpisodesWatchedCount() >= totalNonSpecialEpisodes(tvDetails)) {
            return WatchStatus.WATCHED;
        }

        return WatchStatus.WATCHING;
    }

    private int totalNonSpecialEpisodes(TmdbTvDetailsDTO tvDetails) {
        return tvDetails.getSeasons().stream()
            .filter(season -> season.getSeasonNumber() != null && season.getSeasonNumber() > 0)
            .mapToInt(this::safeEpisodeCount)
            .sum();
    }

    private boolean isEndedShow(TmdbTvDetailsDTO tvDetails) {
        if (tvDetails.getStatus() == null) {
            return false;
        }
        String normalized = tvDetails.getStatus().trim().toUpperCase();
        return normalized.equals("ENDED") || normalized.equals("CANCELED") || normalized.equals("CANCELLED");
    }

    private int safeEpisodeCount(TmdbTvSeasonSummaryDTO seasonSummary) {
        return seasonSummary.getEpisodeCount() == null ? 0 : seasonSummary.getEpisodeCount();
    }

    private WatchStatus parseOptionalStatus(String statusString) {
        if (statusString == null || statusString.isBlank()) {
            return null;
        }
        String normalized = statusString.trim().toUpperCase();
        return switch (normalized) {
            case "TO_WATCH" -> WatchStatus.TO_WATCH;
            case "WATCHING" -> WatchStatus.WATCHING;
            case "WATCHED" -> WatchStatus.WATCHED;
            case "NONE" -> WatchStatus.NONE;
            default -> throw new IllegalArgumentException("Invalid status. Allowed: TO_WATCH, WATCHING, WATCHED, NONE");
        };
    }
}
