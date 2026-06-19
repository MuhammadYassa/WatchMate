package com.project.watchmate.show.tracking.application;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.watchmate.show.catalog.application.ShowCatalogService;
import com.project.watchmate.show.catalog.application.ShowHydrationProperties;
import com.project.watchmate.show.jobs.application.ShowTrackingJobProperties;
import com.project.watchmate.show.tracking.dto.ShowTrackingDTO;
import com.project.watchmate.show.tracking.dto.ShowTrackingStatusDTO;
import com.project.watchmate.media.tmdb.dto.TmdbTvDetailsDTO;
import com.project.watchmate.show.tracking.dto.UpdateShowTrackingPositionRequestDTO;
import com.project.watchmate.common.dto.UpdateWatchStatusRequestDTO;
import com.project.watchmate.show.tracking.dto.WatchedEpisodeDTO;
import com.project.watchmate.common.error.InvalidWatchStatusException;
import com.project.watchmate.common.error.ShowMetadataSyncRequiredException;
import com.project.watchmate.common.error.TmdbClientException;
import com.project.watchmate.common.error.TmdbUnavailableException;
import com.project.watchmate.media.catalog.domain.Media;
import com.project.watchmate.media.catalog.domain.MediaType;
import com.project.watchmate.media.catalog.domain.ShowEpisode;
import com.project.watchmate.show.tracking.domain.ShowTrackingCalculationResult;
import com.project.watchmate.show.tracking.domain.UserEpisodeWatch;
import com.project.watchmate.show.tracking.domain.UserShowTracking;
import com.project.watchmate.user.domain.Users;
import com.project.watchmate.media.catalog.domain.WatchStatus;
import com.project.watchmate.show.tracking.persistence.UserShowTrackingRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShowTrackingService {

    private final ShowCatalogService showCatalogService;

    private final ShowStatusCalculator showStatusCalculator;

    private final ShowTrackingWriteSupport showTrackingWriteSupport;

    private final ShowTrackingFallbackPersistenceService showTrackingFallbackPersistenceService;

    private final ShowHydrationProperties showHydrationProperties;

    private final ShowTrackingJobProperties showTrackingJobProperties;

    private final UserShowTrackingRepository userShowTrackingRepository;

    @Transactional(readOnly = true)
    public ShowTrackingDTO getShowTracking(Users user, Long tmdbId, MediaType mediaType) {
        showCatalogService.validateShowType(mediaType);
        Media media = showCatalogService.findImportedShow(tmdbId);
        if (media == null) {
            return buildEmptyTracking(tmdbId);
        }

        UserShowTracking tracking = userShowTrackingRepository.findWithEpisodeWatchesByUserAndMedia(user, media).orElse(null);
        if (tracking == null) {
            return buildEmptyTracking(tmdbId);
        }

        return mapToShowTracking(media, tracking);
    }

    public ShowTrackingActionResult<ShowTrackingStatusDTO> setShowStatus(
        Users user,
        Long tmdbId,
        MediaType mediaType,
        UpdateWatchStatusRequestDTO request
    ) {
        showCatalogService.validateShowType(mediaType);
        WatchStatus desiredStatus = parseShowWatchStatus(request.getStatus());

        if (desiredStatus == WatchStatus.NONE) {
            Media existingMedia = showCatalogService.findImportedShow(tmdbId);
            if (existingMedia != null) {
                showTrackingWriteSupport.deleteTracking(user, existingMedia);
            }
            return ShowTrackingActionResult.completed(mapToStatusDto(tmdbId, WatchStatus.NONE));
        }

        Media media = showCatalogService.ensureBasicShowImported(tmdbId);
        TmdbTvDetailsDTO tvDetails = showCatalogService.fetchAndRefreshShowDetails(tmdbId, media);

        return switch (desiredStatus) {
            case TO_WATCH -> {
                UserShowTracking tracking = showTrackingWriteSupport.loadOrCreateTracking(user, media);
                showTrackingWriteSupport.resetToWatch(tracking);
                showTrackingWriteSupport.persistTrackingAndCleanupProjection(tracking);
                yield ShowTrackingActionResult.completed(mapToStatusDto(tmdbId, WatchStatus.TO_WATCH));
            }
            case WATCHING -> {
                UserShowTracking tracking = showTrackingWriteSupport.loadOrCreateTracking(user, media);
                tracking.setStatus(WatchStatus.WATCHING);
                showTrackingWriteSupport.persistTrackingAndCleanupProjection(tracking);
                yield ShowTrackingActionResult.completed(mapToStatusDto(tmdbId, WatchStatus.WATCHING));
            }
            case UP_TO_DATE -> handleBulkStatusAction(user, tmdbId, media, tvDetails, WatchStatus.UP_TO_DATE);
            case WATCHED -> handleBulkStatusAction(
                user,
                tmdbId,
                media,
                tvDetails,
                showCatalogService.isEndedShow(tvDetails) ? WatchStatus.WATCHED : WatchStatus.UP_TO_DATE
            );
            case NONE -> throw new IllegalStateException("NONE is handled before switch evaluation.");
        };
    }

    public ShowTrackingActionResult<ShowTrackingDTO> updateWatchPosition(
        Users user,
        Long tmdbId,
        MediaType mediaType,
        UpdateShowTrackingPositionRequestDTO request
    ) {
        showCatalogService.validateShowType(mediaType);
        Media media = showCatalogService.ensureBasicShowImported(tmdbId);
        TmdbTvDetailsDTO tvDetails = showCatalogService.fetchAndRefreshShowDetails(tmdbId, media);
        showCatalogService.validateTrackableSeasonNumber(request.getWatchPositionSeason());

        ShowEpisode pointerEpisode = showCatalogService.requireEpisodeFromCachedSeason(
            media,
            tmdbId,
            request.getWatchPositionSeason(),
            request.getWatchPositionEpisode()
        );
        validateEpisodeIsAired(pointerEpisode);

        List<Integer> requiredSeasons = showCatalogService.getRequiredSeasonNumbersThroughPointer(
            tvDetails,
            request.getWatchPositionSeason()
        );

        if (showCatalogService.canHydrateSynchronously(media, tvDetails, requiredSeasons, showHydrationProperties)) {
            try {
                showCatalogService.hydrateMissingSeasons(
                    media,
                    tmdbId,
                    requiredSeasons,
                    showHydrationProperties.getMaxSynchronousMissingSeasons(),
                    showHydrationProperties.getMaxSynchronousEpisodes()
                );
                return ShowTrackingActionResult.completed(completeProgressUpdate(user, media, tvDetails, request));
            } catch (RuntimeException ex) {
                if (!isRecoverableAsyncFallbackFailure(ex) || !canQueueJobs()) {
                    log.warn("Synchronous show progress update failed without async fallback tmdbId={} season={} episode={}",
                        tmdbId,
                        request.getWatchPositionSeason(),
                        request.getWatchPositionEpisode(),
                        ex);
                    throw ex;
                }
                log.info("Queueing show progress job after recoverable synchronous failure tmdbId={} season={} episode={}",
                    tmdbId,
                    request.getWatchPositionSeason(),
                    request.getWatchPositionEpisode());
                return ShowTrackingActionResult.accepted(showTrackingFallbackPersistenceService.saveProgressAndCreateJob(
                    user,
                    media,
                    request.getWatchPositionSeason(),
                    request.getWatchPositionEpisode(),
                    eligibleEpisodesFromCache(media),
                    airedEligibleEpisodesFromCache(media),
                    showCatalogService.isEndedShow(tvDetails),
                    requiredSeasons.size()
                ));
            }
        }

        if (!canQueueJobs()) {
            throw new ShowMetadataSyncRequiredException(
                "Required season metadata must be synced before show progress can be set to this episode."
            );
        }

        return ShowTrackingActionResult.accepted(showTrackingFallbackPersistenceService.saveProgressAndCreateJob(
            user,
            media,
            request.getWatchPositionSeason(),
            request.getWatchPositionEpisode(),
            eligibleEpisodesFromCache(media),
            airedEligibleEpisodesFromCache(media),
            showCatalogService.isEndedShow(tvDetails),
            requiredSeasons.size()
        ));
    }

    @Transactional(readOnly = true)
    public List<WatchedEpisodeDTO> getWatchedEpisodes(Users user, Long tmdbId, MediaType mediaType) {
        showCatalogService.validateShowType(mediaType);
        Media media = showCatalogService.findImportedShow(tmdbId);
        if (media == null) {
            return List.of();
        }

        UserShowTracking tracking = userShowTrackingRepository.findWithEpisodeWatchesByUserAndMedia(user, media).orElse(null);
        return tracking == null ? List.of() : watchedEpisodeDtos(tracking);
    }

    private ShowTrackingActionResult<ShowTrackingStatusDTO> handleBulkStatusAction(
        Users user,
        Long tmdbId,
        Media media,
        TmdbTvDetailsDTO tvDetails,
        WatchStatus resolvedTargetStatus
    ) {
        List<Integer> requiredSeasons = resolvedTargetStatus == WatchStatus.WATCHED
            ? showCatalogService.getRequiredStandardSeasonNumbers(tvDetails)
            : showCatalogService.getRequiredAiredSeasonNumbers(tvDetails);

        if (showCatalogService.canHydrateSynchronously(media, tvDetails, requiredSeasons, showHydrationProperties)) {
            try {
                showCatalogService.hydrateMissingSeasons(
                    media,
                    tmdbId,
                    requiredSeasons,
                    showHydrationProperties.getMaxSynchronousMissingSeasons(),
                    showHydrationProperties.getMaxSynchronousEpisodes()
                );
                WatchStatus finalStatus = completeBulkStatusUpdate(user, media, tvDetails, resolvedTargetStatus);
                return ShowTrackingActionResult.completed(mapToStatusDto(tmdbId, finalStatus));
            } catch (RuntimeException ex) {
                if (!isRecoverableAsyncFallbackFailure(ex) || !canQueueJobs()) {
                    log.warn("Synchronous bulk status update failed without async fallback tmdbId={} requestedStatus={}",
                        tmdbId,
                        resolvedTargetStatus,
                        ex);
                    throw ex;
                }
                log.info("Queueing bulk status job after recoverable synchronous failure tmdbId={} requestedStatus={}",
                    tmdbId,
                    resolvedTargetStatus);
                return ShowTrackingActionResult.accepted(
                    showTrackingFallbackPersistenceService.ensureTrackingRowAndCreateStatusJob(
                        user,
                        media,
                        resolvedTargetStatus,
                        requiredSeasons.size()
                    )
                );
            }
        }

        if (!canQueueJobs()) {
            throw new ShowMetadataSyncRequiredException(syncRequiredMessageForStatus(resolvedTargetStatus));
        }

        return ShowTrackingActionResult.accepted(
            showTrackingFallbackPersistenceService.ensureTrackingRowAndCreateStatusJob(
                user,
                media,
                resolvedTargetStatus,
                requiredSeasons.size()
            )
        );
    }

    private WatchStatus completeBulkStatusUpdate(
        Users user,
        Media media,
        TmdbTvDetailsDTO tvDetails,
        WatchStatus resolvedTargetStatus
    ) {
        UserShowTracking tracking = showTrackingWriteSupport.loadOrCreateTracking(user, media);
        List<ShowEpisode> targetEpisodes = resolvedTargetStatus == WatchStatus.WATCHED
            ? showCatalogService.requireAllEligibleEpisodesFromCache(media, tvDetails)
            : showCatalogService.requireAiredEligibleEpisodesFromCache(media, tvDetails);

        if (targetEpisodes.isEmpty()) {
            throw new IllegalArgumentException("No eligible episodes are available to mark as watched.");
        }

        showTrackingWriteSupport.addEpisodeWatches(tracking, targetEpisodes, LocalDateTime.now());
        return showTrackingWriteSupport.applyCalculationAndPersist(
            tracking,
            media,
            eligibleEpisodesFromCache(media),
            airedEligibleEpisodesFromCache(media),
            true,
            resolvedTargetStatus == WatchStatus.WATCHED,
            resolvedTargetStatus == WatchStatus.WATCHED
        ).status();
    }

    private ShowTrackingDTO completeProgressUpdate(
        Users user,
        Media media,
        TmdbTvDetailsDTO tvDetails,
        UpdateShowTrackingPositionRequestDTO request
    ) {
        UserShowTracking tracking = showTrackingWriteSupport.loadOrCreateTracking(user, media);
        tracking.setWatchPositionSeason(request.getWatchPositionSeason());
        tracking.setWatchPositionEpisode(request.getWatchPositionEpisode());

        List<ShowEpisode> watchedThroughPointer = showCatalogService.requireEligibleEpisodesThroughPointerFromCache(
            media,
            tvDetails,
            request.getWatchPositionSeason(),
            request.getWatchPositionEpisode()
        );
        showTrackingWriteSupport.replaceEpisodeWatches(tracking, watchedThroughPointer, LocalDateTime.now());
        showTrackingWriteSupport.applyCalculationAndPersist(
            tracking,
            media,
            eligibleEpisodesFromCache(media),
            airedEligibleEpisodesFromCache(media),
            showCatalogService.isAiredMetadataAvailable(media, tvDetails),
            showCatalogService.isFullMetadataAvailable(media, tvDetails),
            showCatalogService.isEndedShow(tvDetails)
        );
        return mapToShowTracking(media, tracking);
    }

    private boolean canQueueJobs() {
        return showTrackingJobProperties.isEnabled();
    }

    private boolean isRecoverableAsyncFallbackFailure(RuntimeException ex) {
        return ex instanceof TmdbUnavailableException
            || ex instanceof TmdbClientException
            || ex instanceof ShowMetadataSyncRequiredException;
    }

    private List<ShowEpisode> eligibleEpisodesFromCache(Media media) {
        return showCatalogService.getAllCachedEpisodes(media.getId()).stream()
            .filter(showCatalogService::isEligibleEpisode)
            .toList();
    }

    private List<ShowEpisode> airedEligibleEpisodesFromCache(Media media) {
        return eligibleEpisodesFromCache(media).stream()
            .filter(showCatalogService::isAiredEpisode)
            .toList();
    }

    private void validateEpisodeIsAired(ShowEpisode episode) {
        if (!showCatalogService.isAiredEpisode(episode)) {
            throw new IllegalArgumentException("Cannot mark an unaired episode as watched.");
        }
    }

    private String syncRequiredMessageForStatus(WatchStatus status) {
        return status == WatchStatus.WATCHED
            ? "Full episode metadata must be synced before this show can be marked watched."
            : "Full aired episode metadata must be synced before this show can be marked up to date.";
    }

    private WatchStatus parseShowWatchStatus(String statusString) {
        if (statusString == null) {
            throw new InvalidWatchStatusException("Status must be provided.");
        }

        String normalized = statusString.trim().toUpperCase();
        return switch (normalized) {
            case "TO_WATCH" -> WatchStatus.TO_WATCH;
            case "WATCHING" -> WatchStatus.WATCHING;
            case "WATCHED" -> WatchStatus.WATCHED;
            case "UP_TO_DATE" -> WatchStatus.UP_TO_DATE;
            case "NONE" -> WatchStatus.NONE;
            default -> throw new InvalidWatchStatusException("Invalid status. Allowed: TO_WATCH, WATCHING, WATCHED, UP_TO_DATE, NONE");
        };
    }

    private ShowTrackingStatusDTO mapToStatusDto(Long tmdbId, WatchStatus status) {
        return ShowTrackingStatusDTO.builder()
            .tmdbId(tmdbId)
            .status(status)
            .build();
    }

    private ShowTrackingDTO mapToShowTracking(Media media, UserShowTracking tracking) {
        ShowTrackingCalculationResult snapshot = showStatusCalculator.calculate(
            tracking,
            tracking.getEpisodeWatches(),
            eligibleEpisodesFromCache(media),
            airedEligibleEpisodesFromCache(media),
            false,
            false,
            false
        );

        return ShowTrackingDTO.builder()
            .tmdbId(media.getTmdbId())
            .type(MediaType.SHOW)
            .status(tracking.getStatus())
            .watchPositionSeason(tracking.getWatchPositionSeason())
            .watchPositionEpisode(tracking.getWatchPositionEpisode())
            .latestWatchedSeason(snapshot.latestWatchedSeasonNumber())
            .latestWatchedEpisode(snapshot.latestWatchedEpisodeNumber())
            .episodesWatchedCount(tracking.getEpisodesWatchedCount())
            .seasonsCompletedCount(tracking.getSeasonsCompletedCount())
            .completed(tracking.getStatus() == WatchStatus.WATCHED)
            .watchedEpisodes(watchedEpisodeDtos(tracking))
            .build();
    }

    private ShowTrackingDTO buildEmptyTracking(Long tmdbId) {
        return ShowTrackingDTO.builder()
            .tmdbId(tmdbId)
            .type(MediaType.SHOW)
            .status(WatchStatus.NONE)
            .watchPositionSeason(null)
            .watchPositionEpisode(null)
            .latestWatchedSeason(null)
            .latestWatchedEpisode(null)
            .episodesWatchedCount(0)
            .seasonsCompletedCount(0)
            .completed(Boolean.FALSE)
            .watchedEpisodes(List.of())
            .build();
    }

    private List<WatchedEpisodeDTO> watchedEpisodeDtos(UserShowTracking tracking) {
        return tracking.getEpisodeWatches().stream()
            .sorted(Comparator.comparing(UserEpisodeWatch::getSeasonNumber).thenComparing(UserEpisodeWatch::getEpisodeNumber))
            .map(row -> WatchedEpisodeDTO.builder()
                .seasonNumber(row.getSeasonNumber())
                .episodeNumber(row.getEpisodeNumber())
                .watchedAt(row.getWatchedAt())
                .build())
            .toList();
    }
}





