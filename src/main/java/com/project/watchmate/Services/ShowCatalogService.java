package com.project.watchmate.Services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.project.watchmate.Dto.TmdbMovieDTO;
import com.project.watchmate.Dto.TmdbTvDetailsDTO;
import com.project.watchmate.Dto.TmdbTvEpisodeDTO;
import com.project.watchmate.Dto.TmdbTvSeasonDTO;
import com.project.watchmate.Dto.TmdbTvSeasonSummaryDTO;
import com.project.watchmate.Exception.ShowMetadataSyncRequiredException;
import com.project.watchmate.Mappers.ShowMetadataMapper;
import com.project.watchmate.Models.Media;
import com.project.watchmate.Models.MediaType;
import com.project.watchmate.Models.ShowEpisode;
import com.project.watchmate.Models.ShowSeason;
import com.project.watchmate.Repositories.MediaRepository;
import com.project.watchmate.Repositories.ShowEpisodeRepository;
import com.project.watchmate.Repositories.ShowSeasonRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ShowCatalogService {

    private static final long SEASON_CACHE_TTL_DAYS = 7;

    private final MediaResolutionService mediaResolutionService;

    private final TmdbService tmdbService;

    private final ShowMetadataMapper showMetadataMapper;

    private final MediaRepository mediaRepository;

    private final ShowSeasonRepository showSeasonRepository;

    private final ShowEpisodeRepository showEpisodeRepository;

    private final PlatformTransactionManager transactionManager;

    public MediaType validateShowType(MediaType mediaType) {
        if (mediaType == null) {
            throw new IllegalArgumentException("Media type is required.");
        }
        if (mediaType != MediaType.SHOW) {
            throw new IllegalArgumentException("This endpoint is only valid for shows.");
        }
        return mediaType;
    }

    @Transactional(readOnly = true)
    public Media findImportedShow(Long tmdbId) {
        return mediaRepository.findByTmdbIdAndType(tmdbId, MediaType.SHOW).orElse(null);
    }

    @Transactional
    public Media ensureBasicShowImported(Long tmdbId) {
        return mediaResolutionService.resolveMediaByTmdbId(tmdbId, MediaType.SHOW);
    }

    public TmdbTvDetailsDTO fetchAndRefreshShowDetails(Long tmdbId, Media media) {
        TmdbTvDetailsDTO tvDetails = tmdbService.fetchTvDetails(tmdbId);
        tmdbService.refreshShowSnapshot(media, tvDetails);
        return tvDetails;
    }

    public CachedSeasonData ensureSeasonCached(Media media, Long tmdbId, Integer seasonNumber) {
        CachedSeasonData cachedData = getCachedSeasonData(media.getId(), seasonNumber);
        ShowSeason cachedSeason = cachedData.season();
        List<ShowEpisode> cachedEpisodes = cachedData.episodes();
        if (isFreshSeasonCache(cachedSeason, cachedEpisodes)) {
            return cachedData;
        }

        TmdbTvSeasonDTO seasonDetails = tmdbService.fetchTvSeasonDetails(tmdbId, seasonNumber);
        return cacheSeasonDetails(media, seasonNumber, seasonDetails);
    }

    @Transactional(readOnly = true)
    public List<ShowEpisode> getCachedEpisodesForSeason(Long mediaId, Integer seasonNumber) {
        return showEpisodeRepository.findAllByMediaIdAndSeasonNumberOrderByEpisodeNumberAsc(mediaId, seasonNumber);
    }

    @Transactional(readOnly = true)
    public List<ShowEpisode> getAllCachedEpisodes(Long mediaId) {
        return showEpisodeRepository.findAllByMediaIdOrderBySeasonNumberAscEpisodeNumberAsc(mediaId);
    }

    @Transactional(readOnly = true)
    public boolean isAiredMetadataAvailable(Media media, TmdbTvDetailsDTO tvDetails) {
        return areRequiredSeasonsCached(media, requiredAiredSeasonNumbers(tvDetails));
    }

    @Transactional(readOnly = true)
    public boolean isFullMetadataAvailable(Media media, TmdbTvDetailsDTO tvDetails) {
        return areRequiredSeasonsCached(media, requiredStandardSeasonNumbers(tvDetails));
    }

    @Transactional(readOnly = true)
    public List<Integer> findMissingOrStaleRequiredSeasons(Media media, Collection<Integer> seasonNumbers) {
        List<Integer> missingSeasons = new ArrayList<>();
        for (Integer seasonNumber : seasonNumbers) {
            CachedSeasonData cachedSeasonData = getCachedSeasonData(media.getId(), seasonNumber);
            if (!isFreshSeasonCache(cachedSeasonData.season(), cachedSeasonData.episodes())) {
                missingSeasons.add(seasonNumber);
            }
        }
        return missingSeasons.stream().sorted().toList();
    }

    public int estimateEpisodeCountForSeasons(TmdbTvDetailsDTO tvDetails, Collection<Integer> seasonNumbers) {
        Set<Integer> targetSeasons = new LinkedHashSet<>(seasonNumbers);
        return tvDetails.getSeasons().stream()
            .filter(season -> targetSeasons.contains(season.getSeasonNumber()))
            .map(TmdbTvSeasonSummaryDTO::getEpisodeCount)
            .filter(java.util.Objects::nonNull)
            .mapToInt(Integer::intValue)
            .sum();
    }

    public boolean canHydrateSynchronously(
        Media media,
        TmdbTvDetailsDTO tvDetails,
        Collection<Integer> requiredSeasonNumbers,
        ShowHydrationProperties properties
    ) {
        List<Integer> missingSeasonNumbers = findMissingOrStaleRequiredSeasons(media, requiredSeasonNumbers);
        if (missingSeasonNumbers.isEmpty()) {
            return true;
        }
        if (missingSeasonNumbers.size() > properties.getMaxSynchronousMissingSeasons()) {
            return false;
        }
        return estimateEpisodeCountForSeasons(tvDetails, missingSeasonNumbers) <= properties.getMaxSynchronousEpisodes();
    }

    public List<Integer> hydrateMissingSeasons(
        Media media,
        Long tmdbId,
        Collection<Integer> requiredSeasonNumbers,
        int maxSeasons,
        int maxEpisodes
    ) {
        List<Integer> missingSeasonNumbers = findMissingOrStaleRequiredSeasons(media, requiredSeasonNumbers);
        if (missingSeasonNumbers.isEmpty()) {
            return List.of();
        }
        if (missingSeasonNumbers.size() > maxSeasons) {
            throw new IllegalArgumentException("Missing season count exceeds synchronous hydration limit.");
        }

        int hydratedEpisodes = 0;
        List<Integer> hydratedSeasons = new ArrayList<>();
        for (Integer seasonNumber : missingSeasonNumbers) {
            TmdbTvSeasonDTO seasonDetails = tmdbService.fetchTvSeasonDetails(tmdbId, seasonNumber);
            int episodeCount = seasonDetails.getEpisodes() == null ? 0 : seasonDetails.getEpisodes().size();
            if (hydratedEpisodes + episodeCount > maxEpisodes) {
                throw new IllegalArgumentException("Missing episode count exceeds synchronous hydration limit.");
            }
            cacheSeasonDetails(media, seasonNumber, seasonDetails);
            hydratedEpisodes += episodeCount;
            hydratedSeasons.add(seasonNumber);
        }
        return hydratedSeasons;
    }

    @Transactional(readOnly = true)
    public List<ShowEpisode> requireAiredEligibleEpisodesFromCache(Media media, TmdbTvDetailsDTO tvDetails) {
        return requireEligibleEpisodesFromCache(
            media,
            requiredAiredSeasonNumbers(tvDetails),
            "Full aired episode metadata must be synced before this show can be marked up to date."
        ).stream()
            .filter(this::isAiredEpisode)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<ShowEpisode> requireAllEligibleEpisodesFromCache(Media media, TmdbTvDetailsDTO tvDetails) {
        return requireEligibleEpisodesFromCache(
            media,
            requiredStandardSeasonNumbers(tvDetails),
            "Full episode metadata must be synced before this show can be marked watched."
        );
    }

    @Transactional(readOnly = true)
    public List<ShowEpisode> requireEligibleEpisodesThroughPointerFromCache(
        Media media,
        TmdbTvDetailsDTO tvDetails,
        Integer seasonNumber,
        Integer episodeNumber
    ) {
        validateTrackableSeasonNumber(seasonNumber);

        List<Integer> requiredSeasons = tvDetails.getSeasons().stream()
            .map(TmdbTvSeasonSummaryDTO::getSeasonNumber)
            .filter(this::isTrackableSeasonNumber)
            .filter(currentSeason -> currentSeason <= seasonNumber)
            .sorted()
            .toList();

        List<ShowEpisode> eligibleEpisodes = requireEligibleEpisodesFromCache(
            media,
            requiredSeasons,
            "Previous seasons must be synced before earlier episodes can be marked watched from this pointer."
        );

        return eligibleEpisodes.stream()
            .filter(this::isAiredEpisode)
            .filter(episode -> compareEpisodeOrder(
                episode.getSeasonNumber(),
                episode.getEpisodeNumber(),
                seasonNumber,
                episodeNumber
            ) <= 0)
            .toList();
    }

    public ShowEpisode requireEpisodeFromCachedSeason(Media media, Long tmdbId, Integer seasonNumber, Integer episodeNumber) {
        CachedSeasonData cachedSeason = ensureSeasonCached(media, tmdbId, seasonNumber);
        return cachedSeason.episodes().stream()
            .filter(episode -> episodeNumber.equals(episode.getEpisodeNumber()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Episode does not exist for the supplied season."));
    }

    @Transactional(readOnly = true)
    public boolean isTrackableSeasonNumber(Integer seasonNumber) {
        return seasonNumber != null && seasonNumber > 0;
    }

    public void validateTrackableSeasonNumber(Integer seasonNumber) {
        if (!isTrackableSeasonNumber(seasonNumber)) {
            throw new IllegalArgumentException("Season 0 specials cannot be tracked.");
        }
    }

    public boolean isEligibleEpisode(ShowEpisode episode) {
        return episode.getSeasonNumber() != null
            && episode.getSeasonNumber() > 0
            && episode.getEpisodeNumber() != null;
    }

    public boolean isAiredEpisode(ShowEpisode episode) {
        return episode.getAirDate() != null && !episode.getAirDate().isAfter(LocalDate.now());
    }

    public boolean isEndedShow(TmdbTvDetailsDTO tvDetails) {
        if (tvDetails.getStatus() == null) {
            return false;
        }

        String normalized = tvDetails.getStatus().trim().toUpperCase();
        return normalized.equals("ENDED")
            || normalized.equals("CANCELED")
            || normalized.equals("CANCELLED");
    }

    public List<Integer> getRequiredAiredSeasonNumbers(TmdbTvDetailsDTO tvDetails) {
        return requiredAiredSeasonNumbers(tvDetails);
    }

    public List<Integer> getRequiredStandardSeasonNumbers(TmdbTvDetailsDTO tvDetails) {
        return requiredStandardSeasonNumbers(tvDetails);
    }

    public List<Integer> getRequiredSeasonNumbersThroughPointer(TmdbTvDetailsDTO tvDetails, Integer seasonNumber) {
        validateTrackableSeasonNumber(seasonNumber);
        return tvDetails.getSeasons().stream()
            .map(TmdbTvSeasonSummaryDTO::getSeasonNumber)
            .filter(this::isTrackableSeasonNumber)
            .filter(currentSeason -> currentSeason <= seasonNumber)
            .sorted()
            .toList();
    }

    private List<ShowEpisode> requireEligibleEpisodesFromCache(Media media, List<Integer> seasonNumbers, String message) {
        List<ShowEpisode> episodes = new ArrayList<>();
        for (Integer seasonNumber : seasonNumbers) {
            CachedSeasonData cachedSeasonData = getCachedSeasonData(media.getId(), seasonNumber);
            if (!isFreshSeasonCache(cachedSeasonData.season(), cachedSeasonData.episodes())) {
                throw new ShowMetadataSyncRequiredException(message);
            }
            cachedSeasonData.episodes().stream()
                .filter(this::isEligibleEpisode)
                .sorted(Comparator.comparing(ShowEpisode::getSeasonNumber).thenComparing(ShowEpisode::getEpisodeNumber))
                .forEach(episodes::add);
        }
        return episodes;
    }

    private boolean areRequiredSeasonsCached(Media media, Collection<Integer> seasonNumbers) {
        for (Integer seasonNumber : seasonNumbers) {
            CachedSeasonData cachedSeasonData = getCachedSeasonData(media.getId(), seasonNumber);
            if (!isFreshSeasonCache(cachedSeasonData.season(), cachedSeasonData.episodes())) {
                return false;
            }
        }
        return true;
    }

    private List<Integer> requiredAiredSeasonNumbers(TmdbTvDetailsDTO tvDetails) {
        LocalDate today = LocalDate.now();
        return requiredSeasonNumbers(tvDetails, season -> {
            LocalDate airDate = TmdbMovieDTO.parseDate(season.getAirDate()).orElse(null);
            return airDate != null && !airDate.isAfter(today);
        });
    }

    private List<Integer> requiredStandardSeasonNumbers(TmdbTvDetailsDTO tvDetails) {
        return requiredSeasonNumbers(tvDetails, season -> true);
    }

    private List<Integer> requiredSeasonNumbers(TmdbTvDetailsDTO tvDetails, Predicate<TmdbTvSeasonSummaryDTO> filter) {
        Set<Integer> seasonNumbers = new LinkedHashSet<>();
        for (TmdbTvSeasonSummaryDTO season : tvDetails.getSeasons()) {
            Integer seasonNumber = season.getSeasonNumber();
            if (!isTrackableSeasonNumber(seasonNumber) || !filter.test(season)) {
                continue;
            }
            seasonNumbers.add(seasonNumber);
        }
        return seasonNumbers.stream().sorted().toList();
    }

    private boolean isFreshSeasonCache(ShowSeason season, List<ShowEpisode> episodes) {
        if (season == null || season.getLastTmdbSyncAt() == null) {
            return false;
        }

        LocalDateTime cutoff = LocalDateTime.now().minusDays(SEASON_CACHE_TTL_DAYS);
        if (season.getLastTmdbSyncAt().isBefore(cutoff)) {
            return false;
        }

        int expectedEpisodeCount = season.getEpisodeCount() == null ? episodes.size() : season.getEpisodeCount();
        if (expectedEpisodeCount == 0) {
            return true;
        }

        return episodes.size() >= expectedEpisodeCount
            && episodes.stream().allMatch(episode ->
                episode.getLastTmdbSyncAt() != null && !episode.getLastTmdbSyncAt().isBefore(cutoff)
            );
    }

    private CachedSeasonData getCachedSeasonData(Long mediaId, Integer seasonNumber) {
        return new CachedSeasonData(
            showSeasonRepository.findByMediaIdAndSeasonNumber(mediaId, seasonNumber).orElse(null),
            getCachedEpisodesForSeason(mediaId, seasonNumber)
        );
    }

    private CachedSeasonData cacheSeasonDetails(Media media, Integer seasonNumber, TmdbTvSeasonDTO seasonDetails) {
        LocalDateTime now = LocalDateTime.now();
        List<TmdbTvEpisodeDTO> seasonEpisodes = seasonDetails.getEpisodes() == null
            ? List.of()
            : seasonDetails.getEpisodes().stream()
                .sorted(Comparator.comparing(TmdbTvEpisodeDTO::getEpisodeNumber, Comparator.nullsLast(Integer::compareTo)))
                .toList();

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        return transactionTemplate.execute(status -> {
            ShowSeason season = showSeasonRepository.findByMediaIdAndSeasonNumber(media.getId(), seasonNumber)
                .orElse(ShowSeason.builder()
                    .media(media)
                    .seasonNumber(seasonNumber)
                    .episodeCount(0)
                    .build());
            season.setName(seasonDetails.getName());
            season.setOverview(seasonDetails.getOverview());
            season.setPosterPath(seasonDetails.getPosterPath());
            season.setAirDate(TmdbMovieDTO.parseDate(seasonDetails.getAirDate()).orElse(null));
            season.setEpisodeCount(seasonDetails.getEpisodeCount() == null ? seasonEpisodes.size() : seasonDetails.getEpisodeCount());
            season.setLastTmdbSyncAt(now);
            ShowSeason savedSeason = showSeasonRepository.save(season);

            showEpisodeRepository.deleteByMediaIdAndSeasonNumber(media.getId(), seasonNumber);
            showEpisodeRepository.flush();
            List<ShowEpisode> savedEpisodes = seasonEpisodes.isEmpty()
                ? List.of()
                : showEpisodeRepository.saveAll(seasonEpisodes.stream()
                    .map(episode -> showMetadataMapper.mapToCachedEpisode(media, seasonNumber, episode, now))
                    .toList());

            return new CachedSeasonData(savedSeason, savedEpisodes);
        });
    }

    private int compareEpisodeOrder(Integer leftSeason, Integer leftEpisode, Integer rightSeason, Integer rightEpisode) {
        int seasonCompare = Integer.compare(leftSeason, rightSeason);
        if (seasonCompare != 0) {
            return seasonCompare;
        }
        return Integer.compare(leftEpisode, rightEpisode);
    }

    public record CachedSeasonData(ShowSeason season, List<ShowEpisode> episodes) {
    }
}
