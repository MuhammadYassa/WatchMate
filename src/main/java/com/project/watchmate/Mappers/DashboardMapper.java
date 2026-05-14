package com.project.watchmate.Mappers;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import org.springframework.stereotype.Component;

import com.project.watchmate.Dto.CalendarItemDTO;
import com.project.watchmate.Dto.ContinueWatchingItemDTO;
import com.project.watchmate.Dto.UpcomingEpisodeItemDTO;
import com.project.watchmate.Models.Media;
import com.project.watchmate.Models.MediaType;
import com.project.watchmate.Models.UserMediaStatus;
import com.project.watchmate.Models.UserShowProgress;

@Component
public class DashboardMapper {

    public ContinueWatchingItemDTO mapToContinueWatchingItem(UserMediaStatus status) {
        Media media = status.getMedia();
        boolean isShow = media.getType() == MediaType.SHOW;
        UserShowProgress progress = status.getShowProgress();

        return ContinueWatchingItemDTO.builder()
            .tmdbId(media.getTmdbId())
            .type(media.getType())
            .title(media.getTitle())
            .posterPath(media.getPosterPath())
            .backdropPath(media.getBackdropPath())
            .watchStatus(status.getStatus())
            .currentSeasonNumber(isShow && progress != null ? progress.getCurrentSeasonNumber() : null)
            .currentEpisodeNumber(isShow && progress != null ? progress.getCurrentEpisodeNumber() : null)
            .nextSeasonNumber(isShow ? media.getNextEpisodeSeasonNumber() : null)
            .nextEpisodeNumber(isShow ? media.getNextEpisodeEpisodeNumber() : null)
            .lastWatchedAt(isShow && progress != null ? progress.getLastWatchedAt() : null)
            .updatedAt(status.getUpdatedAt())
            .rating(media.getRating())
            .build();
    }

    public UpcomingEpisodeItemDTO mapToUpcomingEpisodeItem(UserMediaStatus status, LocalDate today) {
        Media media = status.getMedia();

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

    public CalendarItemDTO mapToCalendarItem(UserMediaStatus status) {
        Media media = status.getMedia();

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
            .watchStatus(status.getStatus())
            .build();
    }
}
