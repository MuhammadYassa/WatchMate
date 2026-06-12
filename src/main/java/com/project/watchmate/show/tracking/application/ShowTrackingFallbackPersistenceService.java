package com.project.watchmate.show.tracking.application;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.watchmate.media.catalog.domain.Media;
import com.project.watchmate.media.catalog.domain.ShowEpisode;
import com.project.watchmate.media.catalog.domain.WatchStatus;
import com.project.watchmate.show.jobs.application.ShowTrackingJobService;
import com.project.watchmate.show.jobs.dto.ShowTrackingJobDTO;
import com.project.watchmate.show.tracking.domain.UserShowTracking;
import com.project.watchmate.show.tracking.persistence.UserShowTrackingRepository;
import com.project.watchmate.user.domain.Users;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ShowTrackingFallbackPersistenceService {

    private final ShowTrackingWriteSupport showTrackingWriteSupport;

    private final ShowTrackingJobService showTrackingJobService;

    private final UserShowTrackingRepository userShowTrackingRepository;

    @Transactional
    public ShowTrackingJobDTO savePointerAndCreateJob(
        Users user,
        Media media,
        Integer seasonNumber,
        Integer episodeNumber,
        List<ShowEpisode> eligibleEpisodes,
        List<ShowEpisode> airedEligibleEpisodes,
        boolean endedShow,
        Integer totalSeasons
    ) {
        UserShowTracking tracking = showTrackingWriteSupport.loadOrCreateTracking(user, media);
        tracking.setWatchPositionSeason(seasonNumber);
        tracking.setWatchPositionEpisode(episodeNumber);
        tracking.setStatus(WatchStatus.WATCHING);
        showTrackingWriteSupport.applyCalculationAndPersist(
            tracking,
            media,
            eligibleEpisodes,
            airedEligibleEpisodes,
            false,
            false,
            endedShow
        );

        return showTrackingJobService.createOrReuseMarkPreviousEpisodesWatchedJob(
            user,
            media,
            seasonNumber,
            episodeNumber,
            totalSeasons
        );
    }

    @Transactional
    public ShowTrackingJobDTO ensureTrackingRowAndCreateStatusJob(
        Users user,
        Media media,
        WatchStatus resolvedTargetStatus,
        Integer totalSeasons
    ) {
        ensureTrackingRowExists(user, media);
        return resolvedTargetStatus == WatchStatus.WATCHED
            ? showTrackingJobService.createOrReuseMarkWatchedJob(user, media, totalSeasons)
            : showTrackingJobService.createOrReuseMarkUpToDateJob(user, media, totalSeasons);
    }

    private void ensureTrackingRowExists(Users user, Media media) {
        UserShowTracking tracking = userShowTrackingRepository.findByUserAndMedia(user, media).orElse(null);
        if (tracking != null) {
            return;
        }

        tracking = showTrackingWriteSupport.loadOrCreateTracking(user, media);
        tracking.setStatus(WatchStatus.WATCHING);
        showTrackingWriteSupport.persistTrackingAndCleanupProjection(tracking);
    }
}
