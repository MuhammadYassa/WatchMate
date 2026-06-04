package com.project.watchmate.Mappers;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import org.springframework.stereotype.Component;

import com.project.watchmate.Dto.CalendarItemDTO;
import com.project.watchmate.Dto.ContinueWatchingItemDTO;
import com.project.watchmate.Dto.UpcomingEpisodeItemDTO;
import com.project.watchmate.Models.Media;
import com.project.watchmate.Models.MediaType;
import com.project.watchmate.Models.UserEpisodeWatch;
import com.project.watchmate.Models.UserMediaStatus;
import com.project.watchmate.Models.UserShowTracking;

@Component
public class DashboardMapper {

    public ContinueWatchingItemDTO mapToContinueWatchingItem(UserShowTracking tracking) {
        Media media = tracking.getMedia();
        boolean isShow = media.getType() == MediaType.SHOW;
        LatestEpisode latestWatched = latestWatchedEpisode(tracking);
        Integer resumeSeason = tracking == null ? null : tracking.getWatchPositionSeason();
        Integer resumeEpisode = tracking == null ? null : tracking.getWatchPositionEpisode();

        return ContinueWatchingItemDTO.builder()
            .tmdbId(media.getTmdbId())
            .type(media.getType())
            .title(media.getTitle())
            .posterPath(media.getPosterPath())
            .backdropPath(media.getBackdropPath())
            .watchStatus(tracking.getStatus())
            .resumeSeasonNumber(isShow ? (resumeSeason != null ? resumeSeason : latestWatched.seasonNumber()) : null)
            .resumeEpisodeNumber(isShow ? (resumeEpisode != null ? resumeEpisode : latestWatched.episodeNumber()) : null)
            .nextSeasonNumber(isShow ? media.getNextEpisodeSeasonNumber() : null)
            .nextEpisodeNumber(isShow ? media.getNextEpisodeEpisodeNumber() : null)
            .lastWatchedAt(isShow ? tracking.getLastWatchedAt() : null)
            .updatedAt(tracking.getUpdatedAt())
            .rating(media.getRating())
            .build();
    }

    public ContinueWatchingItemDTO mapToContinueWatchingItem(UserMediaStatus status) {
        Media media = status.getMedia();

        return ContinueWatchingItemDTO.builder()
            .tmdbId(media.getTmdbId())
            .type(media.getType())
            .title(media.getTitle())
            .posterPath(media.getPosterPath())
            .backdropPath(media.getBackdropPath())
            .watchStatus(status.getStatus())
            .resumeSeasonNumber(null)
            .resumeEpisodeNumber(null)
            .nextSeasonNumber(null)
            .nextEpisodeNumber(null)
            .lastWatchedAt(null)
            .updatedAt(status.getUpdatedAt())
            .rating(media.getRating())
            .build();
    }

    public UpcomingEpisodeItemDTO mapToUpcomingEpisodeItem(UserShowTracking tracking, LocalDate today) {
        Media media = tracking.getMedia();

        return UpcomingEpisodeItemDTO.builder()
            .tmdbId(media.getTmdbId())
            .type(media.getType())
            .title(media.getTitle())
            .posterPath(media.getPosterPath())
            .backdropPath(media.getBackdropPath())
            .nextEpisodeSeasonNumber(media.getNextEpisodeSeasonNumber())
            .nextEpisodeEpisodeNumber(media.getNextEpisodeEpisodeNumber())
            .nextEpisodeName(media.getNextEpisodeName())
            .nextEpisodeAirDate(media.getNextEpisodeAirDate())
            .daysUntilAirDate(media.getNextEpisodeAirDate() == null ? null : ChronoUnit.DAYS.between(today, media.getNextEpisodeAirDate()))
            .tmdbShowStatus(media.getTmdbShowStatus())
            .build();
    }

    public CalendarItemDTO mapToCalendarItem(UserShowTracking tracking) {
        Media media = tracking.getMedia();

        return CalendarItemDTO.builder()
            .airDate(media.getNextEpisodeAirDate())
            .tmdbId(media.getTmdbId())
            .type(media.getType())
            .title(media.getTitle())
            .posterPath(media.getPosterPath())
            .backdropPath(media.getBackdropPath())
            .seasonNumber(media.getNextEpisodeSeasonNumber())
            .episodeNumber(media.getNextEpisodeEpisodeNumber())
            .episodeTitle(media.getNextEpisodeName())
            .showStatus(media.getTmdbShowStatus())
            .watchStatus(tracking.getStatus())
            .build();
    }

    private LatestEpisode latestWatchedEpisode(UserShowTracking tracking) {
        if (tracking == null || tracking.getEpisodeWatches() == null || tracking.getEpisodeWatches().isEmpty()) {
            return new LatestEpisode(null, null);
        }

        UserEpisodeWatch latest = tracking.getEpisodeWatches().stream()
            .sorted(java.util.Comparator.comparing(
                UserEpisodeWatch::getWatchedAt,
                java.util.Comparator.nullsLast(java.time.LocalDateTime::compareTo)
            ).thenComparing(UserEpisodeWatch::getSeasonNumber).thenComparing(UserEpisodeWatch::getEpisodeNumber))
            .reduce((left, right) -> right)
            .orElse(null);

        return latest == null
            ? new LatestEpisode(null, null)
            : new LatestEpisode(latest.getSeasonNumber(), latest.getEpisodeNumber());
    }

    private record LatestEpisode(Integer seasonNumber, Integer episodeNumber) {
    }
}
