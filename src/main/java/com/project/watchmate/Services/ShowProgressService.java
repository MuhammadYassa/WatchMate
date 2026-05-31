package com.project.watchmate.Services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.project.watchmate.Dto.EpisodeProgressDTO;
import com.project.watchmate.Dto.ShowProgressDTO;
import com.project.watchmate.Dto.TmdbTvDetailsDTO;
import com.project.watchmate.Dto.UpdateShowProgressRequestDTO;
import com.project.watchmate.Dto.UpdateWatchStatusRequestDTO;
import com.project.watchmate.Dto.UserMediaStatusDTO;
import com.project.watchmate.Exception.InvalidWatchStatusException;
import com.project.watchmate.Exception.MediaNotFoundException;
import com.project.watchmate.Models.Media;
import com.project.watchmate.Models.MediaType;
import com.project.watchmate.Models.ShowEpisode;
import com.project.watchmate.Models.ShowTrackingState;
import com.project.watchmate.Models.UserEpisodeProgress;
import com.project.watchmate.Models.UserMediaStatus;
import com.project.watchmate.Models.UserShowProgress;
import com.project.watchmate.Models.Users;
import com.project.watchmate.Models.WatchStatus;
import com.project.watchmate.Repositories.UserMediaStatusRepository;
import com.project.watchmate.Repositories.UserShowProgressRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ShowProgressService {

    private final MediaResolutionService mediaResolutionService;

    private final ShowMetadataService showMetadataService;

    private final TmdbService tmdbService;

    private final ShowStatusCalculator showStatusCalculator;

    private final UserMediaStatusRepository userMediaStatusRepository;

    private final UserShowProgressRepository userShowProgressRepository;

    private final TransactionTemplate transactionTemplate;

    @Transactional(readOnly = true)
    public ShowProgressDTO getShowProgress(Users user, Long tmdbId, MediaType mediaType) {
        Media media = resolveShowMedia(tmdbId, mediaType);
        UserMediaStatus status = userMediaStatusRepository.findByUserAndMedia(user, media).orElse(null);
        UserShowProgress progress = userShowProgressRepository.findWithEpisodeProgressByUserAndMedia(user, media).orElse(null);
        if (status == null && progress == null) {
            return buildEmptyProgress(media);
        }
        return mapToShowProgress(media, status, progress);
    }

    public UserMediaStatusDTO updateShowStatus(Users user, Long tmdbId, MediaType mediaType, UpdateWatchStatusRequestDTO request) {
        Media media = resolveShowMedia(tmdbId, mediaType);
        WatchStatus desiredStatus = parseShowWatchStatus(request.getStatus());

        return transactionTemplate.execute(tx -> updateShowStatusTransactional(user, media, tmdbId, desiredStatus));
    }

    public ShowProgressDTO updateShowProgress(Users user, Long tmdbId, MediaType mediaType, UpdateShowProgressRequestDTO request) {
        Media media = resolveShowMedia(tmdbId, mediaType);
        TmdbTvDetailsDTO tvDetails = tmdbService.fetchTvDetails(tmdbId);

        return transactionTemplate.execute(tx ->
            updateShowProgressTransactional(user, media, tmdbId, request, tvDetails));
    }

    public ShowProgressDTO markEpisodeWatched(
        Users user,
        Long tmdbId,
        MediaType mediaType,
        Integer seasonNumber,
        Integer episodeNumber,
        boolean watched
    ) {
        Media media = resolveShowMedia(tmdbId, mediaType);
        TmdbTvDetailsDTO tvDetails = tmdbService.fetchTvDetails(tmdbId);

        return transactionTemplate.execute(tx ->
            markEpisodeWatchedTransactional(user, media, tmdbId, seasonNumber, episodeNumber, watched, tvDetails));
    }

    @Transactional(readOnly = true)
    public List<EpisodeProgressDTO> getWatchedEpisodes(Users user, Long tmdbId, MediaType mediaType) {
        Media media = resolveShowMedia(tmdbId, mediaType);
        UserShowProgress progress = userShowProgressRepository.findWithEpisodeProgressByUserAndMedia(user, media).orElse(null);
        if (progress == null) {
            return List.of();
        }
        return watchedEpisodeDtos(progress);
    }

    public ShowProgressDTO markSeasonWatched(Users user, Long tmdbId, Integer seasonNumber, Boolean watched) {
        Media media = resolveShowMedia(tmdbId, MediaType.SHOW);
        TmdbTvDetailsDTO tvDetails = tmdbService.fetchTvDetails(tmdbId);

        return transactionTemplate.execute(tx ->
            markSeasonWatchedTransactional(user, media, tmdbId, seasonNumber, Boolean.TRUE.equals(watched), tvDetails));
    }

    private UserMediaStatusDTO updateShowStatusTransactional(
        Users user,
        Media media,
        Long tmdbId,
        WatchStatus desiredStatus
    ) {
        return switch (desiredStatus) {
            case NONE -> {
                clearShowTracking(user, media);
                yield mapToStatusDto(media, WatchStatus.NONE);
            }
            case TO_WATCH -> {
                TmdbTvDetailsDTO tvDetails = tmdbService.fetchTvDetails(tmdbId);
                tmdbService.refreshShowSnapshot(media, tvDetails);
                UserShowProgress progress = loadOrCreateProgress(user, media);
                resetTracking(progress, ShowTrackingState.TO_WATCH);
                WatchStatus resolved = recalculateAndPersistShowTracking(user, media, progress, emptySyncData(tvDetails), ShowTrackingState.TO_WATCH);
                yield mapToStatusDto(media, resolved);
            }
            case WATCHING -> {
                TmdbTvDetailsDTO tvDetails = tmdbService.fetchTvDetails(tmdbId);
                tmdbService.refreshShowSnapshot(media, tvDetails);
                UserShowProgress progress = loadOrCreateProgress(user, media);
                progress.setTrackingState(ShowTrackingState.WATCHING);
                WatchStatus resolved = recalculateAndPersistShowTracking(user, media, progress, emptySyncData(tvDetails), ShowTrackingState.WATCHING);
                yield mapToStatusDto(media, resolved);
            }
            case WATCHED -> {
                TmdbTvDetailsDTO tvDetails = tmdbService.fetchTvDetails(tmdbId);
                tmdbService.refreshShowSnapshot(media, tvDetails);
                UserShowProgress progress = loadOrCreateProgress(user, media);
                ShowSyncData syncData = loadShowSyncData(media, tmdbId, tvDetails);
                List<ShowEpisode> targetEpisodes = syncData.endedShow()
                    ? syncData.totalEligibleEpisodes()
                    : syncData.airedEligibleEpisodes();
                if (targetEpisodes.isEmpty()) {
                    throw new IllegalArgumentException(syncData.endedShow()
                        ? "No eligible episodes are available to mark as watched."
                        : "No eligible aired episodes are available to mark as watched.");
                }
                replaceWatchedEpisodes(progress, targetEpisodes, LocalDateTime.now());
                WatchStatus resolved = recalculateAndPersistShowTracking(user, media, progress, syncData, ShowTrackingState.WATCHING);
                yield mapToStatusDto(media, resolved);
            }
            case UP_TO_DATE -> {
                TmdbTvDetailsDTO tvDetails = tmdbService.fetchTvDetails(tmdbId);
                tmdbService.refreshShowSnapshot(media, tvDetails);
                UserShowProgress progress = loadOrCreateProgress(user, media);
                ShowSyncData syncData = loadShowSyncData(media, tmdbId, tvDetails);
                if (syncData.airedEligibleEpisodes().isEmpty()) {
                    throw new IllegalArgumentException("No eligible aired episodes are available to mark as watched.");
                }
                replaceWatchedEpisodes(progress, syncData.airedEligibleEpisodes(), LocalDateTime.now());
                WatchStatus resolved = recalculateAndPersistShowTracking(user, media, progress, syncData, ShowTrackingState.WATCHING);
                yield mapToStatusDto(media, resolved);
            }
        };
    }

    private ShowProgressDTO updateShowProgressTransactional(
        Users user,
        Media media,
        Long tmdbId,
        UpdateShowProgressRequestDTO request,
        TmdbTvDetailsDTO tvDetails
    ) {
        tmdbService.refreshShowSnapshot(media, tvDetails);

        UserShowProgress progress = loadOrCreateProgress(user, media);
        progress.setWatchPositionSeason(request.getCurrentSeasonNumber());
        progress.setWatchPositionEpisode(request.getCurrentEpisodeNumber());

        ShowSyncData syncData = loadShowSyncData(media, tmdbId, tvDetails);
        ShowEpisode pointerEpisode = requireEpisode(syncData, request.getCurrentSeasonNumber(), request.getCurrentEpisodeNumber());
        validateEpisodeIsAired(pointerEpisode);

        if (Boolean.TRUE.equals(request.getMarkPreviousEpisodesWatched())) {
            replaceWatchedEpisodes(progress, eligibleEpisodesThroughPointer(syncData, request.getCurrentSeasonNumber(), request.getCurrentEpisodeNumber()), LocalDateTime.now());
        }

        ShowTrackingState zeroEpisodeState = resolveZeroEpisodeStateForProgressRequest(request.getStatus());
        WatchStatus resolved = recalculateAndPersistShowTracking(user, media, progress, syncData, zeroEpisodeState);
        UserMediaStatus status = userMediaStatusRepository.findByUserAndMedia(user, media).orElse(null);
        if (status == null && resolved == WatchStatus.NONE) {
            return buildEmptyProgress(media);
        }
        return mapToShowProgress(media, status, progress);
    }

    private ShowProgressDTO markEpisodeWatchedTransactional(
        Users user,
        Media media,
        Long tmdbId,
        Integer seasonNumber,
        Integer episodeNumber,
        boolean watched,
        TmdbTvDetailsDTO tvDetails
    ) {
        tmdbService.refreshShowSnapshot(media, tvDetails);

        UserMediaStatus status = userMediaStatusRepository.findByUserAndMedia(user, media).orElse(null);
        UserShowProgress progress = userShowProgressRepository.findWithEpisodeProgressByUserAndMedia(user, media).orElse(null);
        if (!watched && status == null && progress == null) {
            return buildEmptyProgress(media);
        }

        ShowSyncData syncData = loadShowSyncData(media, tmdbId, tvDetails);
        ShowEpisode episode = requireEpisode(syncData, seasonNumber, episodeNumber);
        if (watched) {
            validateEpisodeIsAired(episode);
        }

        if (progress == null) {
            progress = loadOrCreateProgress(user, media);
        }

        UserEpisodeProgress row = findEpisodeProgressRow(progress, seasonNumber, episodeNumber);
        if (watched) {
            upsertWatchedEpisodeRow(progress, row, episode, LocalDateTime.now());
        } else if (row == null || !row.isWatched()) {
            return mapToShowProgress(media, status, progress);
        } else {
            row.setWatched(false);
            row.setWatchedAt(null);
        }

        recalculateAndPersistShowTracking(user, media, progress, syncData, ShowTrackingState.WATCHING);
        UserMediaStatus persistedStatus = userMediaStatusRepository.findByUserAndMedia(user, media).orElse(null);
        return persistedStatus == null && !progressHasTracking(progress)
            ? buildEmptyProgress(media)
            : mapToShowProgress(media, persistedStatus, progress);
    }

    private ShowProgressDTO markSeasonWatchedTransactional(
        Users user,
        Media media,
        Long tmdbId,
        Integer seasonNumber,
        boolean watched,
        TmdbTvDetailsDTO tvDetails
    ) {
        tmdbService.refreshShowSnapshot(media, tvDetails);

        UserMediaStatus status = userMediaStatusRepository.findByUserAndMedia(user, media).orElse(null);
        UserShowProgress progress = userShowProgressRepository.findWithEpisodeProgressByUserAndMedia(user, media).orElse(null);
        if (!watched && status == null && progress == null) {
            return buildEmptyProgress(media);
        }

        ShowSyncData syncData = loadShowSyncData(media, tmdbId, tvDetails);
        validateSeasonNumber(seasonNumber);
        List<ShowEpisode> seasonEpisodes = airedEligibleEpisodesForSeason(syncData, seasonNumber);
        if (watched && seasonEpisodes.isEmpty()) {
            throw new IllegalArgumentException("No eligible aired episodes are available for this season.");
        }

        if (progress == null) {
            progress = loadOrCreateProgress(user, media);
        }

        boolean changed = setEpisodeWatchState(progress, seasonEpisodes, watched, LocalDateTime.now());
        if (!changed) {
            return mapToShowProgress(media, status, progress);
        }

        recalculateAndPersistShowTracking(user, media, progress, syncData, ShowTrackingState.WATCHING);
        UserMediaStatus persistedStatus = userMediaStatusRepository.findByUserAndMedia(user, media).orElse(null);
        return persistedStatus == null && !progressHasTracking(progress)
            ? buildEmptyProgress(media)
            : mapToShowProgress(media, persistedStatus, progress);
    }

    private Media resolveShowMedia(Long tmdbId, MediaType mediaType) {
        showMetadataService.validateShowType(mediaType);
        Media media = mediaResolutionService.resolveMediaByTmdbId(tmdbId, mediaType);
        if (media.getType() != MediaType.SHOW) {
            throw new IllegalArgumentException("This endpoint is only valid for shows.");
        }
        return media;
    }

    private ShowSyncData loadShowSyncData(Media media, Long tmdbId, TmdbTvDetailsDTO tvDetails) {
        try {
            List<ShowEpisode> cachedEpisodes = showMetadataService.ensureStandardEpisodesCached(media, tmdbId, tvDetails);
            List<ShowEpisode> totalEligibleEpisodes = cachedEpisodes.stream()
                .filter(this::isEligibleEpisode)
                .toList();
            List<ShowEpisode> airedEligibleEpisodes = totalEligibleEpisodes.stream()
                .filter(this::isAiredEpisode)
                .toList();

            return new ShowSyncData(
                tvDetails,
                totalEligibleEpisodes,
                airedEligibleEpisodes,
                isEndedShow(tvDetails)
            );
        } catch (MediaNotFoundException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Unable to load required episode data for this show right now.");
        }
    }

    private ShowSyncData emptySyncData(TmdbTvDetailsDTO tvDetails) {
        return new ShowSyncData(tvDetails, List.of(), List.of(), isEndedShow(tvDetails));
    }

    private WatchStatus recalculateAndPersistShowTracking(
        Users user,
        Media media,
        UserShowProgress progress,
        ShowSyncData syncData,
        ShowTrackingState zeroEpisodeState
    ) {
        pruneUnwatchedRows(progress);
        recalculateFromEpisodeRows(progress, syncData.totalEligibleEpisodes());

        if (progress.getEpisodesWatchedCount() > 0) {
            progress.setTrackingState(ShowTrackingState.WATCHING);
        } else {
            progress.setTrackingState(zeroEpisodeState);
        }

        WatchStatus derivedStatus = showStatusCalculator.calculate(
            progress,
            progress.getEpisodesWatchedCount(),
            syncData.airedEligibleEpisodes().size(),
            syncData.totalEligibleEpisodes().size(),
            syncData.endedShow()
        );

        if (derivedStatus == WatchStatus.NONE) {
            clearShowTracking(user, media);
            return WatchStatus.NONE;
        }

        userShowProgressRepository.save(progress);
        UserMediaStatus status = userMediaStatusRepository.findByUserAndMedia(user, media)
            .orElse(UserMediaStatus.builder()
                .user(user)
                .media(media)
                .status(derivedStatus)
                .build());
        status.setStatus(derivedStatus);
        userMediaStatusRepository.save(status);
        return derivedStatus;
    }

    private void clearShowTracking(Users user, Media media) {
        userShowProgressRepository.findWithEpisodeProgressByUserAndMedia(user, media)
            .ifPresent(userShowProgressRepository::delete);
        userMediaStatusRepository.findByUserAndMedia(user, media)
            .ifPresent(userMediaStatusRepository::delete);
    }

    private void resetTracking(UserShowProgress progress, ShowTrackingState trackingState) {
        progress.getEpisodeProgress().clear();
        progress.setTrackingState(trackingState);
        progress.setCurrentSeasonNumber(null);
        progress.setCurrentEpisodeNumber(null);
        progress.setWatchPositionSeason(null);
        progress.setWatchPositionEpisode(null);
        progress.setEpisodesWatchedCount(0);
        progress.setSeasonsCompletedCount(0);
        progress.setLastWatchedAt(null);
    }

    private UserShowProgress loadOrCreateProgress(Users user, Media media) {
        return userShowProgressRepository.findWithEpisodeProgressByUserAndMedia(user, media)
            .orElse(UserShowProgress.builder()
                .user(user)
                .media(media)
                .episodesWatchedCount(0)
                .seasonsCompletedCount(0)
                .episodeProgress(new ArrayList<>())
                .build());
    }

    private List<ShowEpisode> eligibleEpisodesThroughPointer(ShowSyncData syncData, Integer seasonNumber, Integer episodeNumber) {
        validateSeasonNumber(seasonNumber);
        return syncData.airedEligibleEpisodes().stream()
            .filter(episode -> compareEpisodeOrder(
                episode.getSeasonNumber(),
                episode.getEpisodeNumber(),
                seasonNumber,
                episodeNumber
            ) <= 0)
            .toList();
    }

    private void replaceWatchedEpisodes(UserShowProgress progress, List<ShowEpisode> targetEpisodes, LocalDateTime watchedAt) {
        Map<String, UserEpisodeProgress> existing = progress.getEpisodeProgress().stream()
            .collect(Collectors.toMap(
                row -> progressKey(row.getSeasonNumber(), row.getEpisodeNumber()),
                row -> row,
                (left, right) -> left,
                HashMap::new
            ));

        Set<String> targetKeys = targetEpisodes.stream()
            .map(episode -> progressKey(episode.getSeasonNumber(), episode.getEpisodeNumber()))
            .collect(Collectors.toSet());

        for (UserEpisodeProgress row : progress.getEpisodeProgress()) {
            if (!targetKeys.contains(progressKey(row.getSeasonNumber(), row.getEpisodeNumber()))) {
                row.setWatched(false);
                row.setWatchedAt(null);
            }
        }

        for (ShowEpisode episode : targetEpisodes) {
            String key = progressKey(episode.getSeasonNumber(), episode.getEpisodeNumber());
            UserEpisodeProgress row = existing.get(key);
            upsertWatchedEpisodeRow(progress, row, episode, watchedAt);
        }
    }

    private boolean setEpisodeWatchState(
        UserShowProgress progress,
        List<ShowEpisode> episodes,
        boolean watched,
        LocalDateTime watchedAt
    ) {
        boolean changed = false;
        for (ShowEpisode episode : episodes) {
            UserEpisodeProgress row = findEpisodeProgressRow(progress, episode.getSeasonNumber(), episode.getEpisodeNumber());
            if (watched) {
                if (row == null || !row.isWatched()) {
                    upsertWatchedEpisodeRow(progress, row, episode, watchedAt);
                    changed = true;
                }
                continue;
            }

            if (row != null && row.isWatched()) {
                row.setWatched(false);
                row.setWatchedAt(null);
                changed = true;
            }
        }
        return changed;
    }

    private void upsertWatchedEpisodeRow(UserShowProgress progress, UserEpisodeProgress row, ShowEpisode episode, LocalDateTime watchedAt) {
        if (row == null) {
            row = UserEpisodeProgress.builder()
                .userShowProgress(progress)
                .seasonNumber(episode.getSeasonNumber())
                .episodeNumber(episode.getEpisodeNumber())
                .build();
            progress.getEpisodeProgress().add(row);
        }
        row.setWatched(true);
        row.setWatchedAt(watchedAt);
    }

    private void pruneUnwatchedRows(UserShowProgress progress) {
        progress.getEpisodeProgress().removeIf(row -> !row.isWatched());
    }

    private void recalculateFromEpisodeRows(UserShowProgress progress, List<ShowEpisode> eligibleEpisodes) {
        List<UserEpisodeProgress> watchedRows = progress.getEpisodeProgress().stream()
            .filter(UserEpisodeProgress::isWatched)
            .filter(this::isEligibleEpisodeProgress)
            .sorted(Comparator.comparing(UserEpisodeProgress::getSeasonNumber).thenComparing(UserEpisodeProgress::getEpisodeNumber))
            .toList();

        progress.setEpisodesWatchedCount(watchedRows.size());
        progress.setSeasonsCompletedCount(countCompletedSeasons(eligibleEpisodes, watchedRows));

        UserEpisodeProgress latest = watchedRows.isEmpty() ? null : watchedRows.get(watchedRows.size() - 1);
        progress.setCurrentSeasonNumber(latest == null ? null : latest.getSeasonNumber());
        progress.setCurrentEpisodeNumber(latest == null ? null : latest.getEpisodeNumber());
        progress.setLastWatchedAt(watchedRows.stream()
            .map(UserEpisodeProgress::getWatchedAt)
            .filter(Objects::nonNull)
            .max(LocalDateTime::compareTo)
            .orElse(null));
    }

    private int countCompletedSeasons(List<ShowEpisode> eligibleEpisodes, List<UserEpisodeProgress> watchedRows) {
        Map<Integer, Long> watchedPerSeason = watchedRows.stream()
            .collect(Collectors.groupingBy(UserEpisodeProgress::getSeasonNumber, Collectors.counting()));
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

    private ShowEpisode requireEpisode(ShowSyncData syncData, Integer seasonNumber, Integer episodeNumber) {
        validateSeasonNumber(seasonNumber);
        return syncData.totalEligibleEpisodes().stream()
            .filter(episode -> Objects.equals(episode.getSeasonNumber(), seasonNumber)
                && Objects.equals(episode.getEpisodeNumber(), episodeNumber))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Episode does not exist for the supplied season."));
    }

    private List<ShowEpisode> airedEligibleEpisodesForSeason(ShowSyncData syncData, Integer seasonNumber) {
        validateSeasonNumber(seasonNumber);
        return syncData.airedEligibleEpisodes().stream()
            .filter(episode -> Objects.equals(episode.getSeasonNumber(), seasonNumber))
            .toList();
    }

    private void validateSeasonNumber(Integer seasonNumber) {
        if (seasonNumber == null || seasonNumber <= 0) {
            throw new IllegalArgumentException("Season 0 specials cannot be tracked.");
        }
    }

    private void validateEpisodeIsAired(ShowEpisode episode) {
        if (!isAiredEpisode(episode)) {
            throw new IllegalArgumentException("Cannot mark an unaired episode as watched.");
        }
    }

    private boolean isEligibleEpisode(ShowEpisode episode) {
        return episode.getSeasonNumber() != null
            && episode.getSeasonNumber() > 0
            && episode.getEpisodeNumber() != null;
    }

    private boolean isEligibleEpisodeProgress(UserEpisodeProgress row) {
        return row.getSeasonNumber() != null
            && row.getSeasonNumber() > 0
            && row.getEpisodeNumber() != null;
    }

    private boolean isAiredEpisode(ShowEpisode episode) {
        return episode.getAirDate() != null && !episode.getAirDate().isAfter(LocalDate.now());
    }

    private boolean isEndedShow(TmdbTvDetailsDTO tvDetails) {
        if (tvDetails.getStatus() == null) {
            return false;
        }

        String normalized = tvDetails.getStatus().trim().toUpperCase();
        return normalized.equals("ENDED")
            || normalized.equals("CANCELED")
            || normalized.equals("CANCELLED");
    }

    private ShowTrackingState resolveZeroEpisodeStateForProgressRequest(String requestedStatus) {
        if (requestedStatus == null || requestedStatus.isBlank()) {
            return ShowTrackingState.WATCHING;
        }

        String normalized = requestedStatus.trim().toUpperCase();
        return switch (normalized) {
            case "TO_WATCH" -> ShowTrackingState.TO_WATCH;
            case "WATCHING", "WATCHED", "UP_TO_DATE", "NONE" -> ShowTrackingState.WATCHING;
            default -> throw new InvalidWatchStatusException("Invalid status. Allowed: TO_WATCH, WATCHING, WATCHED, UP_TO_DATE, NONE");
        };
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

    private UserEpisodeProgress findEpisodeProgressRow(
        UserShowProgress progress,
        Integer seasonNumber,
        Integer episodeNumber
    ) {
        return progress.getEpisodeProgress().stream()
            .filter(row -> Objects.equals(row.getSeasonNumber(), seasonNumber)
                && Objects.equals(row.getEpisodeNumber(), episodeNumber))
            .findFirst()
            .orElse(null);
    }

    private int compareEpisodeOrder(Integer leftSeason, Integer leftEpisode, Integer rightSeason, Integer rightEpisode) {
        int seasonCompare = Integer.compare(leftSeason, rightSeason);
        if (seasonCompare != 0) {
            return seasonCompare;
        }
        return Integer.compare(leftEpisode, rightEpisode);
    }

    private String progressKey(Integer seasonNumber, Integer episodeNumber) {
        return seasonNumber + ":" + episodeNumber;
    }

    private boolean progressHasTracking(UserShowProgress progress) {
        return progress != null && (progress.getTrackingState() != null || progress.getEpisodesWatchedCount() > 0);
    }

    private UserMediaStatusDTO mapToStatusDto(Media media, WatchStatus status) {
        return UserMediaStatusDTO.builder()
            .tmdbId(media.getTmdbId())
            .status(status)
            .build();
    }

    private ShowProgressDTO mapToShowProgress(Media media, UserMediaStatus status, UserShowProgress progress) {
        WatchStatus resolvedStatus = resolveReadStatus(status, progress);
        return ShowProgressDTO.builder()
            .tmdbId(media.getTmdbId())
            .type(MediaType.SHOW)
            .status(resolvedStatus)
            .currentSeasonNumber(progress == null ? null : progress.getCurrentSeasonNumber())
            .currentEpisodeNumber(progress == null ? null : progress.getCurrentEpisodeNumber())
            .watchPositionSeason(progress == null ? null : progress.getWatchPositionSeason())
            .watchPositionEpisode(progress == null ? null : progress.getWatchPositionEpisode())
            .episodesWatchedCount(progress == null ? 0 : progress.getEpisodesWatchedCount())
            .seasonsCompletedCount(progress == null ? 0 : progress.getSeasonsCompletedCount())
            .completed(resolvedStatus == WatchStatus.WATCHED)
            .watchedEpisodes(progress == null ? List.of() : watchedEpisodeDtos(progress))
            .build();
    }

    private WatchStatus resolveReadStatus(UserMediaStatus status, UserShowProgress progress) {
        if (status != null) {
            return status.getStatus();
        }
        if (progress == null) {
            return WatchStatus.NONE;
        }
        if (progress.getEpisodesWatchedCount() > 0) {
            return WatchStatus.WATCHING;
        }
        if (progress.getTrackingState() == ShowTrackingState.TO_WATCH) {
            return WatchStatus.TO_WATCH;
        }
        if (progress.getTrackingState() == ShowTrackingState.WATCHING) {
            return WatchStatus.WATCHING;
        }
        return WatchStatus.NONE;
    }

    private ShowProgressDTO buildEmptyProgress(Media media) {
        return ShowProgressDTO.builder()
            .tmdbId(media.getTmdbId())
            .type(MediaType.SHOW)
            .status(WatchStatus.NONE)
            .currentSeasonNumber(null)
            .currentEpisodeNumber(null)
            .watchPositionSeason(null)
            .watchPositionEpisode(null)
            .episodesWatchedCount(0)
            .seasonsCompletedCount(0)
            .completed(Boolean.FALSE)
            .watchedEpisodes(List.of())
            .build();
    }

    private List<EpisodeProgressDTO> watchedEpisodeDtos(UserShowProgress progress) {
        return progress.getEpisodeProgress().stream()
            .filter(UserEpisodeProgress::isWatched)
            .filter(this::isEligibleEpisodeProgress)
            .sorted(Comparator.comparing(UserEpisodeProgress::getSeasonNumber).thenComparing(UserEpisodeProgress::getEpisodeNumber))
            .map(row -> EpisodeProgressDTO.builder()
                .seasonNumber(row.getSeasonNumber())
                .episodeNumber(row.getEpisodeNumber())
                .watchedAt(row.getWatchedAt())
                .build())
            .toList();
    }

    private record ShowSyncData(
        TmdbTvDetailsDTO tvDetails,
        List<ShowEpisode> totalEligibleEpisodes,
        List<ShowEpisode> airedEligibleEpisodes,
        boolean endedShow
    ) {
    }
}
