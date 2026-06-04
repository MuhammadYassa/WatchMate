package com.project.watchmate.Services;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.watchmate.Dto.ShowTrackingDTO;
import com.project.watchmate.Dto.ShowTrackingStatusDTO;
import com.project.watchmate.Dto.TmdbTvDetailsDTO;
import com.project.watchmate.Dto.UpdateShowTrackingPositionRequestDTO;
import com.project.watchmate.Dto.UpdateWatchStatusRequestDTO;
import com.project.watchmate.Dto.WatchedEpisodeDTO;
import com.project.watchmate.Exception.InvalidWatchStatusException;
import com.project.watchmate.Models.Media;
import com.project.watchmate.Models.MediaType;
import com.project.watchmate.Models.ShowEpisode;
import com.project.watchmate.Models.UserEpisodeWatch;
import com.project.watchmate.Models.UserShowTracking;
import com.project.watchmate.Models.Users;
import com.project.watchmate.Models.WatchStatus;
import com.project.watchmate.Repositories.UserMediaStatusRepository;
import com.project.watchmate.Repositories.UserShowTrackingRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ShowTrackingService {

    private final ShowCatalogService showCatalogService;

    private final ShowStatusCalculator showStatusCalculator;

    private final UserMediaStatusRepository userMediaStatusRepository;

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

    @Transactional
    public ShowTrackingStatusDTO setShowStatus(Users user, Long tmdbId, MediaType mediaType, UpdateWatchStatusRequestDTO request) {
        showCatalogService.validateShowType(mediaType);
        WatchStatus desiredStatus = parseShowWatchStatus(request.getStatus());

        if (desiredStatus == WatchStatus.NONE) {
            Media existingMedia = showCatalogService.findImportedShow(tmdbId);
            if (existingMedia != null) {
                deleteTracking(user, existingMedia);
            }
            return mapToStatusDto(tmdbId, WatchStatus.NONE);
        }

        Media media = showCatalogService.ensureBasicShowImported(tmdbId);
        TmdbTvDetailsDTO tvDetails = showCatalogService.fetchAndRefreshShowDetails(tmdbId, media);
        UserShowTracking tracking = loadOrCreateTracking(user, media);

        return switch (desiredStatus) {
            case TO_WATCH -> {
                resetToWatch(tracking);
                persistTrackingAndCleanupProjection(user, media, tracking);
                yield mapToStatusDto(tmdbId, WatchStatus.TO_WATCH);
            }
            case WATCHING -> {
                tracking.setStatus(WatchStatus.WATCHING);
                persistTrackingAndCleanupProjection(user, media, tracking);
                yield mapToStatusDto(tmdbId, tracking.getStatus());
            }
            case UP_TO_DATE -> {
                List<ShowEpisode> airedEligibleEpisodes = showCatalogService.requireAiredEligibleEpisodesFromCache(media, tvDetails);
                if (airedEligibleEpisodes.isEmpty()) {
                    throw new IllegalArgumentException("No eligible aired episodes are available to mark as watched.");
                }
                replaceEpisodeWatches(tracking, airedEligibleEpisodes, LocalDateTime.now());
                boolean airedMetadataComplete = true;
                boolean totalMetadataComplete = showCatalogService.isFullMetadataAvailable(media, tvDetails);
                ShowTrackingCalculationResult result = recalculateTracking(
                    tracking,
                    media,
                    tvDetails,
                    airedMetadataComplete,
                    totalMetadataComplete
                );
                yield mapToStatusDto(tmdbId, result.status());
            }
            case WATCHED -> {
                List<ShowEpisode> targetEpisodes;
                boolean airedMetadataComplete;
                boolean totalMetadataComplete;

                if (showCatalogService.isEndedShow(tvDetails)) {
                    targetEpisodes = showCatalogService.requireAllEligibleEpisodesFromCache(media, tvDetails);
                    airedMetadataComplete = showCatalogService.isAiredMetadataAvailable(media, tvDetails);
                    totalMetadataComplete = true;
                    if (targetEpisodes.isEmpty()) {
                        throw new IllegalArgumentException("No eligible episodes are available to mark as watched.");
                    }
                } else {
                    targetEpisodes = showCatalogService.requireAiredEligibleEpisodesFromCache(media, tvDetails);
                    airedMetadataComplete = true;
                    totalMetadataComplete = showCatalogService.isFullMetadataAvailable(media, tvDetails);
                    if (targetEpisodes.isEmpty()) {
                        throw new IllegalArgumentException("No eligible aired episodes are available to mark as watched.");
                    }
                }

                replaceEpisodeWatches(tracking, targetEpisodes, LocalDateTime.now());
                ShowTrackingCalculationResult result = recalculateTracking(
                    tracking,
                    media,
                    tvDetails,
                    airedMetadataComplete,
                    totalMetadataComplete
                );
                yield mapToStatusDto(tmdbId, result.status());
            }
            case NONE -> throw new IllegalStateException("NONE is handled before switch evaluation.");
        };
    }

    @Transactional
    public ShowTrackingDTO updateWatchPosition(Users user, Long tmdbId, MediaType mediaType, UpdateShowTrackingPositionRequestDTO request) {
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

        UserShowTracking tracking = loadOrCreateTracking(user, media);
        tracking.setWatchPositionSeason(request.getWatchPositionSeason());
        tracking.setWatchPositionEpisode(request.getWatchPositionEpisode());

        if (Boolean.TRUE.equals(request.getMarkPreviousEpisodesWatched())) {
            List<ShowEpisode> watchedThroughPointer = showCatalogService.requireEligibleEpisodesThroughPointerFromCache(
                media,
                tvDetails,
                request.getWatchPositionSeason(),
                request.getWatchPositionEpisode()
            );
            replaceEpisodeWatches(tracking, watchedThroughPointer, LocalDateTime.now());
            recalculateTracking(
                tracking,
                media,
                tvDetails,
                showCatalogService.isAiredMetadataAvailable(media, tvDetails),
                showCatalogService.isFullMetadataAvailable(media, tvDetails)
            );
        } else {
            tracking.setStatus(WatchStatus.WATCHING);
            applyCalculation(tracking, showStatusCalculator.calculate(
                tracking,
                tracking.getEpisodeWatches(),
                eligibleEpisodesFromCache(media),
                airedEligibleEpisodesFromCache(media),
                false,
                false,
                showCatalogService.isEndedShow(tvDetails)
            ));
            tracking.setStatus(WatchStatus.WATCHING);
            persistTrackingAndCleanupProjection(user, media, tracking);
        }

        return mapToShowTracking(media, tracking);
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
            tracking = loadOrCreateTracking(user, media);
        }

        UserEpisodeWatch existing = findEpisodeWatch(tracking, seasonNumber, episodeNumber);
        if (watched) {
            upsertWatchedEpisodeRow(tracking, existing, episode, LocalDateTime.now());
        } else if (existing != null) {
            tracking.getEpisodeWatches().remove(existing);
        }

        recalculateTracking(
            tracking,
            media,
            tvDetails,
            showCatalogService.isAiredMetadataAvailable(media, tvDetails),
            showCatalogService.isFullMetadataAvailable(media, tvDetails)
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
            tracking = loadOrCreateTracking(user, media);
        }

        if (Boolean.TRUE.equals(watched)) {
            for (ShowEpisode episode : airedSeasonEpisodes) {
                upsertWatchedEpisodeRow(tracking, findEpisodeWatch(tracking, episode.getSeasonNumber(), episode.getEpisodeNumber()), episode, LocalDateTime.now());
            }
        } else {
            tracking.getEpisodeWatches().removeIf(row -> Objects.equals(row.getSeasonNumber(), seasonNumber));
        }

        recalculateTracking(
            tracking,
            media,
            tvDetails,
            showCatalogService.isAiredMetadataAvailable(media, tvDetails),
            showCatalogService.isFullMetadataAvailable(media, tvDetails)
        );
        return mapToShowTracking(media, tracking);
    }

    private ShowTrackingCalculationResult recalculateTracking(
        UserShowTracking tracking,
        Media media,
        TmdbTvDetailsDTO tvDetails,
        boolean airedMetadataComplete,
        boolean totalMetadataComplete
    ) {
        ShowTrackingCalculationResult result = showStatusCalculator.calculate(
            tracking,
            tracking.getEpisodeWatches(),
            eligibleEpisodesFromCache(media),
            airedEligibleEpisodesFromCache(media),
            airedMetadataComplete,
            totalMetadataComplete,
            showCatalogService.isEndedShow(tvDetails)
        );
        applyCalculation(tracking, result);
        persistTrackingAndCleanupProjection(tracking.getUser(), media, tracking);
        return result;
    }

    private void applyCalculation(UserShowTracking tracking, ShowTrackingCalculationResult result) {
        tracking.setStatus(result.status());
        tracking.setEpisodesWatchedCount(result.episodesWatchedCount());
        tracking.setSeasonsCompletedCount(result.seasonsCompletedCount());
        tracking.setLastWatchedAt(result.lastWatchedAt());
    }

    private void persistTrackingAndCleanupProjection(Users user, Media media, UserShowTracking tracking) {
        userShowTrackingRepository.save(tracking);
        deleteLegacyShowStatusProjection(user, media);
    }

    private void deleteTracking(Users user, Media media) {
        userShowTrackingRepository.findWithEpisodeWatchesByUserAndMedia(user, media)
            .ifPresent(userShowTrackingRepository::delete);
        deleteLegacyShowStatusProjection(user, media);
    }

    private void resetToWatch(UserShowTracking tracking) {
        tracking.getEpisodeWatches().clear();
        tracking.setStatus(WatchStatus.TO_WATCH);
        tracking.setWatchPositionSeason(null);
        tracking.setWatchPositionEpisode(null);
        tracking.setEpisodesWatchedCount(0);
        tracking.setSeasonsCompletedCount(0);
        tracking.setLastWatchedAt(null);
    }

    private UserShowTracking loadOrCreateTracking(Users user, Media media) {
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

    private void replaceEpisodeWatches(UserShowTracking tracking, List<ShowEpisode> targetEpisodes, LocalDateTime watchedAt) {
        Map<String, UserEpisodeWatch> existingByKey = new HashMap<>();
        for (UserEpisodeWatch row : tracking.getEpisodeWatches()) {
            existingByKey.put(progressKey(row.getSeasonNumber(), row.getEpisodeNumber()), row);
        }

        Map<String, ShowEpisode> targetByKey = new HashMap<>();
        for (ShowEpisode episode : targetEpisodes) {
            targetByKey.put(progressKey(episode.getSeasonNumber(), episode.getEpisodeNumber()), episode);
        }

        tracking.getEpisodeWatches().removeIf(row -> !targetByKey.containsKey(progressKey(row.getSeasonNumber(), row.getEpisodeNumber())));
        for (ShowEpisode episode : targetEpisodes) {
            upsertWatchedEpisodeRow(
                tracking,
                existingByKey.get(progressKey(episode.getSeasonNumber(), episode.getEpisodeNumber())),
                episode,
                watchedAt
            );
        }
    }

    private void upsertWatchedEpisodeRow(UserShowTracking tracking, UserEpisodeWatch existing, ShowEpisode episode, LocalDateTime watchedAt) {
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

    private UserEpisodeWatch findEpisodeWatch(UserShowTracking tracking, Integer seasonNumber, Integer episodeNumber) {
        return tracking.getEpisodeWatches().stream()
            .filter(row -> Objects.equals(row.getSeasonNumber(), seasonNumber)
                && Objects.equals(row.getEpisodeNumber(), episodeNumber))
            .findFirst()
            .orElse(null);
    }

    private void validateEpisodeIsAired(ShowEpisode episode) {
        if (!showCatalogService.isAiredEpisode(episode)) {
            throw new IllegalArgumentException("Cannot mark an unaired episode as watched.");
        }
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

    private void deleteLegacyShowStatusProjection(Users user, Media media) {
        userMediaStatusRepository.findByUserAndMedia(user, media)
            .ifPresent(userMediaStatusRepository::delete);
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

    private String progressKey(Integer seasonNumber, Integer episodeNumber) {
        return seasonNumber + ":" + episodeNumber;
    }
}
