package com.project.watchmate.show.tracking.application;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.watchmate.media.catalog.domain.Media;
import com.project.watchmate.media.catalog.domain.ShowEpisode;
import com.project.watchmate.show.tracking.domain.ShowTrackingCalculationResult;
import com.project.watchmate.show.tracking.domain.UserEpisodeWatch;
import com.project.watchmate.show.tracking.domain.UserShowTracking;
import com.project.watchmate.user.domain.Users;
import com.project.watchmate.media.catalog.domain.WatchStatus;
import com.project.watchmate.movie.tracking.persistence.UserMediaStatusRepository;
import com.project.watchmate.show.tracking.persistence.UserShowTrackingRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ShowTrackingWriteSupport {

    private final ShowStatusCalculator showStatusCalculator;

    private final UserMediaStatusRepository userMediaStatusRepository;

    private final UserShowTrackingRepository userShowTrackingRepository;

    @Transactional
    public UserShowTracking loadOrCreateTracking(Users user, Media media) {
        return userShowTrackingRepository.findWithEpisodeWatchesByUserAndMedia(user, media)
            .orElse(UserShowTracking.builder()
                .user(user)
                .media(media)
                .status(WatchStatus.WATCHING)
                .episodesWatchedCount(0)
                .seasonsCompletedCount(0)
                .episodeWatches(new ArrayList<>())
                .build());
    }

    @Transactional
    public void deleteTracking(Users user, Media media) {
        userShowTrackingRepository.findWithEpisodeWatchesByUserAndMedia(user, media)
            .ifPresent(userShowTrackingRepository::delete);
        deleteLegacyShowStatusProjection(user, media);
    }

    public void resetToWatch(UserShowTracking tracking) {
        tracking.getEpisodeWatches().clear();
        tracking.setStatus(WatchStatus.TO_WATCH);
        tracking.setWatchPositionSeason(null);
        tracking.setWatchPositionEpisode(null);
        tracking.setEpisodesWatchedCount(0);
        tracking.setSeasonsCompletedCount(0);
        tracking.setLastWatchedAt(null);
    }

    public void addEpisodeWatches(UserShowTracking tracking, List<ShowEpisode> targetEpisodes, LocalDateTime watchedAt) {
        for (ShowEpisode episode : targetEpisodes) {
            upsertWatchedEpisodeRow(
                tracking,
                findEpisodeWatch(tracking, episode.getSeasonNumber(), episode.getEpisodeNumber()),
                episode,
                watchedAt
            );
        }
    }

    public void replaceEpisodeWatches(UserShowTracking tracking, List<ShowEpisode> targetEpisodes, LocalDateTime watchedAt) {
        tracking.getEpisodeWatches().removeIf(row -> targetEpisodes.stream().noneMatch(episode ->
            Objects.equals(episode.getSeasonNumber(), row.getSeasonNumber())
                && Objects.equals(episode.getEpisodeNumber(), row.getEpisodeNumber())
        ));
        addEpisodeWatches(tracking, targetEpisodes, watchedAt);
    }

    public UserEpisodeWatch findEpisodeWatch(UserShowTracking tracking, Integer seasonNumber, Integer episodeNumber) {
        return tracking.getEpisodeWatches().stream()
            .filter(row -> Objects.equals(row.getSeasonNumber(), seasonNumber)
                && Objects.equals(row.getEpisodeNumber(), episodeNumber))
            .findFirst()
            .orElse(null);
    }

    public void upsertWatchedEpisodeRow(UserShowTracking tracking, UserEpisodeWatch existing, ShowEpisode episode, LocalDateTime watchedAt) {
        if (existing == null) {
            existing = UserEpisodeWatch.builder()
                .userShowTracking(tracking)
                .seasonNumber(episode.getSeasonNumber())
                .episodeNumber(episode.getEpisodeNumber())
                .build();
            tracking.getEpisodeWatches().add(existing);
        }
        existing.setWatchedAt(watchedAt);
    }

    @Transactional
    public ShowTrackingCalculationResult applyCalculationAndPersist(
        UserShowTracking tracking,
        Media media,
        List<ShowEpisode> eligibleEpisodes,
        List<ShowEpisode> airedEligibleEpisodes,
        boolean airedMetadataComplete,
        boolean totalMetadataComplete,
        boolean endedShow
    ) {
        ShowTrackingCalculationResult result = showStatusCalculator.calculate(
            tracking,
            tracking.getEpisodeWatches(),
            eligibleEpisodes,
            airedEligibleEpisodes,
            airedMetadataComplete,
            totalMetadataComplete,
            endedShow
        );
        tracking.setStatus(result.status());
        tracking.setEpisodesWatchedCount(result.episodesWatchedCount());
        tracking.setSeasonsCompletedCount(result.seasonsCompletedCount());
        tracking.setLastWatchedAt(result.lastWatchedAt());
        userShowTrackingRepository.save(tracking);
        deleteLegacyShowStatusProjection(tracking.getUser(), media);
        return result;
    }

    @Transactional
    public void persistTrackingAndCleanupProjection(UserShowTracking tracking) {
        userShowTrackingRepository.save(tracking);
        deleteLegacyShowStatusProjection(tracking.getUser(), tracking.getMedia());
    }

    private void deleteLegacyShowStatusProjection(Users user, Media media) {
        userMediaStatusRepository.findByUserAndMedia(user, media)
            .ifPresent(userMediaStatusRepository::delete);
    }
}





