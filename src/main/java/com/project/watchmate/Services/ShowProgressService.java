package com.project.watchmate.Services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.project.watchmate.Dto.EpisodeProgressDTO;
import com.project.watchmate.Dto.ShowProgressDTO;
import com.project.watchmate.Dto.TmdbMovieDTO;
import com.project.watchmate.Dto.TmdbTvDetailsDTO;
import com.project.watchmate.Dto.TmdbTvEpisodeDTO;
import com.project.watchmate.Dto.TmdbTvSeasonDTO;
import com.project.watchmate.Dto.TmdbTvSeasonSummaryDTO;
import com.project.watchmate.Dto.UpdateShowProgressRequestDTO;
import com.project.watchmate.Models.Media;
import com.project.watchmate.Models.MediaType;
import com.project.watchmate.Models.UserEpisodeProgress;
import com.project.watchmate.Models.UserMediaStatus;
import com.project.watchmate.Models.UserShowProgress;
import com.project.watchmate.Models.Users;
import com.project.watchmate.Models.WatchStatus;
import com.project.watchmate.Repositories.UserEpisodeProgressRepository;
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

    private final UserEpisodeProgressRepository userEpisodeProgressRepository;

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

    public ShowProgressDTO updateShowProgress(Users user, Long tmdbId, MediaType mediaType, UpdateShowProgressRequestDTO request) {
        Media media = resolveShowMedia(tmdbId, mediaType);
        TmdbTvDetailsDTO tvDetails = tmdbService.fetchTvDetails(tmdbId);
        validateAiredEpisode(tmdbId, request.getCurrentSeasonNumber(), request.getCurrentEpisodeNumber());

        return transactionTemplate.execute(tx ->
            updateShowProgressTransactional(user, media, request, tvDetails));
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
        validateEpisodeExists(tmdbId, seasonNumber, episodeNumber, Boolean.TRUE.equals(watched));

        return transactionTemplate.execute(tx ->
            markEpisodeWatchedTransactional(user, media, seasonNumber, episodeNumber, watched, tvDetails));
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
        TmdbTvSeasonDTO seasonDetails = tmdbService.fetchTvSeasonDetails(tmdbId, seasonNumber);
        validateSeasonEpisodes(seasonDetails, Boolean.TRUE.equals(watched));

        return transactionTemplate.execute(tx ->
            markSeasonWatchedTransactional(user, media, seasonNumber, watched, tvDetails, seasonDetails));
    }

    private ShowProgressDTO updateShowProgressTransactional(
        Users user,
        Media media,
        UpdateShowProgressRequestDTO request,
        TmdbTvDetailsDTO tvDetails
    ) {
        tmdbService.refreshShowSnapshot(media, tvDetails);

        UserMediaStatus status = loadOrCreateStatus(user, media);
        UserShowProgress progress = loadOrCreateProgress(user, media);
        LocalDateTime now = LocalDateTime.now();

        progress.setWatchPositionSeason(request.getCurrentSeasonNumber());
        progress.setWatchPositionEpisode(request.getCurrentEpisodeNumber());

        if (Boolean.TRUE.equals(request.getMarkPreviousEpisodesWatched())) {
            backfillEpisodesThroughPointer(progress, tvDetails, request.getCurrentSeasonNumber(), request.getCurrentEpisodeNumber(), now);
            markLaterEpisodesUnwatched(progress, request.getCurrentSeasonNumber(), request.getCurrentEpisodeNumber());
            recalculateFromEpisodeRows(progress, tvDetails);
        } else {
            markLaterEpisodesUnwatched(progress, request.getCurrentSeasonNumber(), request.getCurrentEpisodeNumber());
            recalculateFromEpisodeRows(progress, tvDetails);
        }

        status.setStatus(showStatusCalculator.calculate(progress, tvDetails, request.getStatus()));
        userShowProgressRepository.save(progress);
        userMediaStatusRepository.save(status);
        return mapToShowProgress(media, status, progress);
    }

    private ShowProgressDTO markEpisodeWatchedTransactional(
        Users user,
        Media media,
        Integer seasonNumber,
        Integer episodeNumber,
        boolean watched,
        TmdbTvDetailsDTO tvDetails
    ) {
        tmdbService.refreshShowSnapshot(media, tvDetails);

        UserMediaStatus status = userMediaStatusRepository.findByUserAndMedia(user, media).orElse(null);
        UserShowProgress progress = userShowProgressRepository.findWithEpisodeProgressByUserAndMedia(user, media).orElse(null);

        if (!Boolean.TRUE.equals(watched) && status == null && progress == null) {
            return buildEmptyProgress(media);
        }

        if (status == null) {
            status = loadOrCreateStatus(user, media);
        }
        if (progress == null) {
            progress = loadOrCreateProgress(user, media);
        }

        UserEpisodeProgress row = userEpisodeProgressRepository
            .findByUserShowProgressAndSeasonNumberAndEpisodeNumber(progress, seasonNumber, episodeNumber)
            .orElse(null);

        if (Boolean.TRUE.equals(watched)) {
            if (row == null) {
                row = UserEpisodeProgress.builder()
                    .userShowProgress(progress)
                    .seasonNumber(seasonNumber)
                    .episodeNumber(episodeNumber)
                    .build();
                progress.getEpisodeProgress().add(row);
            }
            row.setWatched(true);
            row.setWatchedAt(LocalDateTime.now());
        } else if (row != null) {
            row.setWatched(false);
            row.setWatchedAt(null);
        }

        recalculateFromEpisodeRows(progress, tvDetails);
        status.setStatus(showStatusCalculator.calculate(progress, tvDetails, null));
        userShowProgressRepository.save(progress);
        userMediaStatusRepository.save(status);
        return mapToShowProgress(media, status, progress);
    }

    private ShowProgressDTO markSeasonWatchedTransactional(
        Users user,
        Media media,
        Integer seasonNumber,
        Boolean watched,
        TmdbTvDetailsDTO tvDetails,
        TmdbTvSeasonDTO seasonDetails
    ) {
        tmdbService.refreshShowSnapshot(media, tvDetails);

        UserMediaStatus status = userMediaStatusRepository.findByUserAndMedia(user, media).orElse(null);
        UserShowProgress progress = userShowProgressRepository.findWithEpisodeProgressByUserAndMedia(user, media).orElse(null);

        if (!Boolean.TRUE.equals(watched) && status == null && progress == null) {
            return buildEmptyProgress(media);
        }

        if (status == null) {
            status = loadOrCreateStatus(user, media);
        }
        if (progress == null) {
            progress = loadOrCreateProgress(user, media);
        }

        Map<String, UserEpisodeProgress> existing = new HashMap<>();
        for (UserEpisodeProgress row : progress.getEpisodeProgress()) {
            existing.put(progressKey(row.getSeasonNumber(), row.getEpisodeNumber()), row);
        }

        LocalDateTime now = LocalDateTime.now();
        List<UserEpisodeProgress> newRows = new ArrayList<>();
        for (TmdbTvEpisodeDTO episode : seasonDetails.getEpisodes()) {
            Integer currentEpisodeNumber = episode.getEpisodeNumber();
            if (currentEpisodeNumber == null) {
                continue;
            }

            String key = progressKey(seasonNumber, currentEpisodeNumber);
            UserEpisodeProgress row = existing.get(key);

            if (Boolean.TRUE.equals(watched)) {
                if (row == null) {
                    row = UserEpisodeProgress.builder()
                        .userShowProgress(progress)
                        .seasonNumber(seasonNumber)
                        .episodeNumber(currentEpisodeNumber)
                        .watched(true)
                        .watchedAt(now)
                        .build();
                    newRows.add(row);
                    existing.put(key, row);
                    continue;
                }
                row.setWatched(true);
                row.setWatchedAt(now);
            } else if (row != null) {
                row.setWatched(false);
                row.setWatchedAt(null);
            }
        }

        if (!newRows.isEmpty()) {
            userShowProgressRepository.save(progress);
            userEpisodeProgressRepository.saveAll(newRows);
            progress.setEpisodeProgress(userEpisodeProgressRepository.findByUserShowProgressOrderBySeasonNumberAscEpisodeNumberAsc(progress));
        }

        recalculateFromEpisodeRows(progress, tvDetails);
        status.setStatus(showStatusCalculator.calculate(progress, tvDetails, null));
        userShowProgressRepository.save(progress);
        userMediaStatusRepository.save(status);
        return mapToShowProgress(media, status, progress);
    }

    private Media resolveShowMedia(Long tmdbId, MediaType mediaType) {
        showMetadataService.validateShowType(mediaType);
        Media media = mediaResolutionService.resolveMediaByTmdbId(tmdbId, mediaType);
        if (media.getType() != MediaType.SHOW) {
            throw new IllegalArgumentException("This endpoint is only valid for shows.");
        }
        return media;
    }

    private UserMediaStatus loadOrCreateStatus(Users user, Media media) {
        return userMediaStatusRepository.findByUserAndMedia(user, media)
            .orElse(UserMediaStatus.builder()
                .user(user)
                .media(media)
                .status(WatchStatus.NONE)
                .build());
    }

    private UserShowProgress loadOrCreateProgress(Users user, Media media) {
        return userShowProgressRepository.findWithEpisodeProgressByUserAndMedia(user, media)
            .orElse(UserShowProgress.builder()
                .user(user)
                .media(media)
                .episodesWatchedCount(0)
                .seasonsCompletedCount(0)
                .build());
    }

    private void backfillEpisodesThroughPointer(
        UserShowProgress progress,
        TmdbTvDetailsDTO tvDetails,
        Integer seasonNumber,
        Integer episodeNumber,
        LocalDateTime now
    ) {
        Map<String, UserEpisodeProgress> existing = new HashMap<>();
        for (UserEpisodeProgress row : progress.getEpisodeProgress()) {
            existing.put(progressKey(row.getSeasonNumber(), row.getEpisodeNumber()), row);
        }

        List<UserEpisodeProgress> rowsToInsert = new ArrayList<>();
        for (TmdbTvSeasonSummaryDTO seasonSummary : tvDetails.getSeasons()) {
            Integer currentSeason = seasonSummary.getSeasonNumber();
            if (currentSeason == null || currentSeason == 0 || currentSeason > seasonNumber) {
                continue;
            }

            int limit = Objects.equals(currentSeason, seasonNumber)
                ? episodeNumber
                : safeEpisodeCount(seasonSummary);

            for (int episode = 1; episode <= limit; episode++) {
                String key = progressKey(currentSeason, episode);
                UserEpisodeProgress row = existing.get(key);
                if (row == null) {
                    rowsToInsert.add(UserEpisodeProgress.builder()
                        .userShowProgress(progress)
                        .seasonNumber(currentSeason)
                        .episodeNumber(episode)
                        .watched(true)
                        .watchedAt(now)
                        .build());
                    continue;
                }
                row.setWatched(true);
                row.setWatchedAt(now);
            }
        }

        if (!rowsToInsert.isEmpty()) {
            userShowProgressRepository.save(progress);
            userEpisodeProgressRepository.saveAll(rowsToInsert);
            progress.setEpisodeProgress(userEpisodeProgressRepository.findByUserShowProgressOrderBySeasonNumberAscEpisodeNumberAsc(progress));
        }
    }

    private void markLaterEpisodesUnwatched(UserShowProgress progress, Integer seasonNumber, Integer episodeNumber) {
        for (UserEpisodeProgress row : progress.getEpisodeProgress()) {
            if (compareEpisodeOrder(row.getSeasonNumber(), row.getEpisodeNumber(), seasonNumber, episodeNumber) > 0) {
                row.setWatched(false);
                row.setWatchedAt(null);
            }
        }
    }

    private void recalculateFromEpisodeRows(UserShowProgress progress, TmdbTvDetailsDTO tvDetails) {
        List<UserEpisodeProgress> watchedRows = progress.getEpisodeProgress().stream()
            .filter(UserEpisodeProgress::isWatched)
            .sorted(Comparator.comparing(UserEpisodeProgress::getSeasonNumber).thenComparing(UserEpisodeProgress::getEpisodeNumber))
            .toList();

        List<UserEpisodeProgress> watchedStandardRows = watchedRows.stream()
            .filter(row -> row.getSeasonNumber() != null && row.getSeasonNumber() > 0)
            .toList();

        progress.setEpisodesWatchedCount(watchedStandardRows.size());
        progress.setSeasonsCompletedCount(countCompletedSeasonsFromRows(tvDetails, watchedStandardRows));

        UserEpisodeProgress latest = watchedStandardRows.isEmpty() ? null : watchedStandardRows.get(watchedStandardRows.size() - 1);
        progress.setCurrentSeasonNumber(latest == null ? null : latest.getSeasonNumber());
        progress.setCurrentEpisodeNumber(latest == null ? null : latest.getEpisodeNumber());
        progress.setLastWatchedAt(watchedRows.stream()
            .map(UserEpisodeProgress::getWatchedAt)
            .filter(Objects::nonNull)
            .max(LocalDateTime::compareTo)
            .orElse(null));
    }

    private int countCompletedSeasonsFromRows(TmdbTvDetailsDTO tvDetails, List<UserEpisodeProgress> watchedStandardRows) {
        Map<Integer, Long> watchedPerSeason = watchedStandardRows.stream()
            .collect(java.util.stream.Collectors.groupingBy(UserEpisodeProgress::getSeasonNumber, java.util.stream.Collectors.counting()));

        int completed = 0;
        for (TmdbTvSeasonSummaryDTO seasonSummary : tvDetails.getSeasons()) {
            if (seasonSummary.getSeasonNumber() == null || seasonSummary.getSeasonNumber() == 0) {
                continue;
            }
            long watchedCount = watchedPerSeason.getOrDefault(seasonSummary.getSeasonNumber(), 0L);
            if (safeEpisodeCount(seasonSummary) > 0 && watchedCount >= safeEpisodeCount(seasonSummary)) {
                completed++;
            }
        }
        return completed;
    }

    private void validateAiredEpisode(Long tmdbId, Integer seasonNumber, Integer episodeNumber) {
        TmdbTvSeasonDTO seasonDetails = tmdbService.fetchTvSeasonDetails(tmdbId, seasonNumber);
        validateEpisodeAgainstSeason(seasonDetails, episodeNumber, true);
    }

    private void validateEpisodeExists(Long tmdbId, Integer seasonNumber, Integer episodeNumber, boolean requireAired) {
        TmdbTvSeasonDTO seasonDetails = tmdbService.fetchTvSeasonDetails(tmdbId, seasonNumber);
        validateEpisodeAgainstSeason(seasonDetails, episodeNumber, requireAired);
    }

    private void validateSeasonEpisodes(TmdbTvSeasonDTO seasonDetails, boolean requireAired) {
        if (seasonDetails.getEpisodes() == null || seasonDetails.getEpisodes().isEmpty()) {
            throw new IllegalArgumentException("Season details are unavailable for this show.");
        }
        if (!requireAired) {
            return;
        }
        for (TmdbTvEpisodeDTO episode : seasonDetails.getEpisodes()) {
            validateEpisodeIsAired(episode);
        }
    }

    private void validateEpisodeAgainstSeason(TmdbTvSeasonDTO seasonDetails, Integer episodeNumber, boolean requireAired) {
        if (seasonDetails.getEpisodes() == null || seasonDetails.getEpisodes().isEmpty()) {
            throw new IllegalArgumentException("Season details are unavailable for this show.");
        }
        TmdbTvEpisodeDTO episode = seasonDetails.getEpisodes().stream()
            .filter(candidate -> Objects.equals(candidate.getEpisodeNumber(), episodeNumber))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Episode does not exist for the supplied season."));

        if (requireAired) {
            validateEpisodeIsAired(episode);
        }
    }

    private void validateEpisodeIsAired(TmdbTvEpisodeDTO episode) {
        LocalDate airDate = TmdbMovieDTO.parseDate(episode.getAirDate()).orElse(null);
        if (airDate != null && airDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Cannot mark an unaired episode as watched.");
        }
    }

    private int safeEpisodeCount(TmdbTvSeasonSummaryDTO seasonSummary) {
        return seasonSummary.getEpisodeCount() == null ? 0 : seasonSummary.getEpisodeCount();
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

    private ShowProgressDTO mapToShowProgress(Media media, UserMediaStatus status, UserShowProgress progress) {
        WatchStatus resolvedStatus = status == null ? WatchStatus.NONE : status.getStatus();
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
            .sorted(Comparator.comparing(UserEpisodeProgress::getSeasonNumber).thenComparing(UserEpisodeProgress::getEpisodeNumber))
            .map(row -> EpisodeProgressDTO.builder()
                .seasonNumber(row.getSeasonNumber())
                .episodeNumber(row.getEpisodeNumber())
                .watchedAt(row.getWatchedAt())
                .build())
            .toList();
    }
}
