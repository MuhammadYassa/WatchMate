package com.project.watchmate.Services;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.watchmate.Dto.ShowTrackingDTO;
import com.project.watchmate.Dto.ShowTrackingJobDTO;
import com.project.watchmate.Dto.ShowTrackingStatusDTO;
import com.project.watchmate.Dto.TmdbTvDetailsDTO;
import com.project.watchmate.Dto.UpdateShowTrackingPositionRequestDTO;
import com.project.watchmate.Dto.UpdateWatchStatusRequestDTO;
import com.project.watchmate.Dto.WatchedEpisodeDTO;
import com.project.watchmate.Exception.InvalidWatchStatusException;
import com.project.watchmate.Exception.ShowMetadataSyncRequiredException;
import com.project.watchmate.Models.Media;
import com.project.watchmate.Models.MediaType;
import com.project.watchmate.Models.ShowEpisode;
import com.project.watchmate.Models.UserEpisodeWatch;
import com.project.watchmate.Models.UserShowTracking;
import com.project.watchmate.Models.Users;
import com.project.watchmate.Models.WatchStatus;
import com.project.watchmate.Repositories.UserShowTrackingRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ShowTrackingService {

    private final ShowCatalogService showCatalogService;

    private final ShowStatusCalculator showStatusCalculator;

    private final ShowTrackingWriteSupport showTrackingWriteSupport;

    private final ShowTrackingJobService showTrackingJobService;

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

        if (!Boolean.TRUE.equals(request.getMarkPreviousEpisodesWatched())) {
            UserShowTracking tracking = showTrackingWriteSupport.loadOrCreateTracking(user, media);
            tracking.setWatchPositionSeason(request.getWatchPositionSeason());
            tracking.setWatchPositionEpisode(request.getWatchPositionEpisode());
            tracking.setStatus(WatchStatus.WATCHING);
            showTrackingWriteSupport.applyCalculationAndPersist(
                tracking,
                media,
                eligibleEpisodesFromCache(media),
                airedEligibleEpisodesFromCache(media),
                false,
                false,
                showCatalogService.isEndedShow(tvDetails)
            );
            return ShowTrackingActionResult.completed(mapToShowTracking(media, tracking));
        }

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
                return ShowTrackingActionResult.completed(completePointerBackfill(user, media, tvDetails, request));
            } catch (RuntimeException ex) {
                if (!canQueueJobs()) {
                    throw ex;
                }
                savePointerOnly(user, media, request.getWatchPositionSeason(), request.getWatchPositionEpisode(), tvDetails);
                return ShowTrackingActionResult.accepted(createPointerJob(
                    user,
                    media,
                    request.getWatchPositionSeason(),
                    request.getWatchPositionEpisode(),
                    requiredSeasons.size()
                ));
            }
        }

        if (!canQueueJobs()) {
            throw new ShowMetadataSyncRequiredException(
                "Previous seasons must be synced before earlier episodes can be marked watched from this pointer."
            );
        }

        savePointerOnly(user, media, request.getWatchPositionSeason(), request.getWatchPositionEpisode(), tvDetails);
        return ShowTrackingActionResult.accepted(createPointerJob(
            user,
            media,
            request.getWatchPositionSeason(),
            request.getWatchPositionEpisode(),
            requiredSeasons.size()
        ));
    }

    @Transactional
    public ShowTrackingDTO markEpisodeWatched(
        Users user,
        Long tmdbId,
        MediaType mediaType,
        Integer seasonNumber,
        Integer episodeNumber,
        boolean watched
    ) {
        showCatalogService.validateShowType(mediaType);
        showCatalogService.validateTrackableSeasonNumber(seasonNumber);

        Media media = showCatalogService.ensureBasicShowImported(tmdbId);
        TmdbTvDetailsDTO tvDetails = showCatalogService.fetchAndRefreshShowDetails(tmdbId, media);

        UserShowTracking tracking = userShowTrackingRepository.findWithEpisodeWatchesByUserAndMedia(user, media).orElse(null);
        if (!watched && tracking == null) {
            return buildEmptyTracking(tmdbId);
        }

        ShowEpisode episode = showCatalogService.requireEpisodeFromCachedSeason(media, tmdbId, seasonNumber, episodeNumber);
        if (watched) {
            validateEpisodeIsAired(episode);
        }

        if (tracking == null) {
            tracking = showTrackingWriteSupport.loadOrCreateTracking(user, media);
        }

        UserEpisodeWatch existing = showTrackingWriteSupport.findEpisodeWatch(tracking, seasonNumber, episodeNumber);
        if (watched) {
            showTrackingWriteSupport.upsertWatchedEpisodeRow(tracking, existing, episode, LocalDateTime.now());
        } else if (existing != null) {
            tracking.getEpisodeWatches().remove(existing);
        }

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

    @Transactional
    public ShowTrackingDTO markSeasonWatched(Users user, Long tmdbId, Integer seasonNumber, Boolean watched) {
        showCatalogService.validateTrackableSeasonNumber(seasonNumber);

        Media media = showCatalogService.ensureBasicShowImported(tmdbId);
        TmdbTvDetailsDTO tvDetails = showCatalogService.fetchAndRefreshShowDetails(tmdbId, media);

        UserShowTracking tracking = userShowTrackingRepository.findWithEpisodeWatchesByUserAndMedia(user, media).orElse(null);
        if (!Boolean.TRUE.equals(watched) && tracking == null) {
            return buildEmptyTracking(tmdbId);
        }

        ShowCatalogService.CachedSeasonData cachedSeason = showCatalogService.ensureSeasonCached(media, tmdbId, seasonNumber);
        List<ShowEpisode> airedSeasonEpisodes = cachedSeason.episodes().stream()
            .filter(showCatalogService::isEligibleEpisode)
            .filter(showCatalogService::isAiredEpisode)
            .toList();

        if (Boolean.TRUE.equals(watched) && airedSeasonEpisodes.isEmpty()) {
            throw new IllegalArgumentException("No eligible aired episodes are available for this season.");
        }

        if (tracking == null) {
            tracking = showTrackingWriteSupport.loadOrCreateTracking(user, media);
        }

        if (Boolean.TRUE.equals(watched)) {
            showTrackingWriteSupport.addEpisodeWatches(tracking, airedSeasonEpisodes, LocalDateTime.now());
        } else {
            tracking.getEpisodeWatches().removeIf(row -> Objects.equals(row.getSeasonNumber(), seasonNumber));
        }

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
                if (!canQueueJobs()) {
                    throw ex;
                }
                ensureTrackingRowExists(user, media);
                return ShowTrackingActionResult.accepted(createStatusJob(user, media, resolvedTargetStatus, requiredSeasons.size()));
            }
        }

        if (!canQueueJobs()) {
            throw new ShowMetadataSyncRequiredException(syncRequiredMessageForStatus(resolvedTargetStatus));
        }

        ensureTrackingRowExists(user, media);
        return ShowTrackingActionResult.accepted(createStatusJob(user, media, resolvedTargetStatus, requiredSeasons.size()));
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

    private ShowTrackingDTO completePointerBackfill(
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
        showTrackingWriteSupport.addEpisodeWatches(tracking, watchedThroughPointer, LocalDateTime.now());
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

    private void savePointerOnly(Users user, Media media, Integer seasonNumber, Integer episodeNumber, TmdbTvDetailsDTO tvDetails) {
        UserShowTracking tracking = showTrackingWriteSupport.loadOrCreateTracking(user, media);
        tracking.setWatchPositionSeason(seasonNumber);
        tracking.setWatchPositionEpisode(episodeNumber);
        tracking.setStatus(WatchStatus.WATCHING);
        showTrackingWriteSupport.applyCalculationAndPersist(
            tracking,
            media,
            eligibleEpisodesFromCache(media),
            airedEligibleEpisodesFromCache(media),
            false,
            false,
            showCatalogService.isEndedShow(tvDetails)
        );
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

    private ShowTrackingJobDTO createStatusJob(Users user, Media media, WatchStatus resolvedTargetStatus, Integer totalSeasons) {
        return resolvedTargetStatus == WatchStatus.WATCHED
            ? showTrackingJobService.createOrReuseMarkWatchedJob(user, media, totalSeasons)
            : showTrackingJobService.createOrReuseMarkUpToDateJob(user, media, totalSeasons);
    }

    private ShowTrackingJobDTO createPointerJob(
        Users user,
        Media media,
        Integer targetSeasonNumber,
        Integer targetEpisodeNumber,
        Integer totalSeasons
    ) {
        return showTrackingJobService.createOrReuseMarkPreviousEpisodesWatchedJob(
            user,
            media,
            targetSeasonNumber,
            targetEpisodeNumber,
            totalSeasons
        );
    }

    private boolean canQueueJobs() {
        return showTrackingJobProperties.isEnabled();
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
