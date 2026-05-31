package com.project.watchmate.Services;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.watchmate.Dto.NextEpisodeAiringDTO;
import com.project.watchmate.Dto.ShowDetailsDTO;
import com.project.watchmate.Dto.ShowSeasonsDetailsDTO;
import com.project.watchmate.Dto.TmdbMovieDTO;
import com.project.watchmate.Dto.TmdbTvDetailsDTO;
import com.project.watchmate.Dto.TmdbTvEpisodeDTO;
import com.project.watchmate.Dto.TmdbTvSeasonDTO;
import com.project.watchmate.Dto.TmdbTvSeasonSummaryDTO;
import com.project.watchmate.Mappers.ShowMetadataMapper;
import com.project.watchmate.Models.Media;
import com.project.watchmate.Models.MediaType;
import com.project.watchmate.Models.Review;
import com.project.watchmate.Models.ShowEpisode;
import com.project.watchmate.Models.ShowSeason;
import com.project.watchmate.Models.UserMediaStatus;
import com.project.watchmate.Models.Users;
import com.project.watchmate.Models.WatchStatus;
import com.project.watchmate.Repositories.MediaRepository;
import com.project.watchmate.Repositories.ReviewRepository;
import com.project.watchmate.Repositories.ShowEpisodeRepository;
import com.project.watchmate.Repositories.ShowSeasonRepository;
import com.project.watchmate.Repositories.UserMediaStatusRepository;
import com.project.watchmate.Repositories.UsersRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ShowMetadataService {

    private static final long SEASON_CACHE_TTL_DAYS = 7;

    private final MediaResolutionService mediaResolutionService;

    private final TmdbService tmdbService;

    private final ShowMetadataMapper showMetadataMapper;

    private final MediaRepository mediaRepository;

    private final UsersRepository usersRepository;

    private final ReviewRepository reviewRepository;

    private final UserMediaStatusRepository userMediaStatusRepository;

    private final ShowSeasonRepository showSeasonRepository;

    private final ShowEpisodeRepository showEpisodeRepository;

    @Transactional
    public NextEpisodeAiringDTO getNextEpisode(Long tmdbId, MediaType mediaType) {
        validateShowType(mediaType);
        TmdbTvDetailsDTO tvDetails = tmdbService.fetchTvDetails(tmdbId);
        tmdbService.refreshShowSnapshotIfImported(tmdbId, tvDetails);
        return showMetadataMapper.mapToNextEpisodeDto(tmdbId, tvDetails);
    }

    @Transactional
    public ShowDetailsDTO getShowDetails(Long tmdbId, MediaType mediaType, Users userParam) {
        validateShowType(mediaType);

        Media importedShow = userParam == null
            ? mediaRepository.findByTmdbIdAndType(tmdbId, MediaType.SHOW).orElse(null)
            : mediaResolutionService.resolveMediaByTmdbId(tmdbId, MediaType.SHOW);

        List<Review> reviews = importedShow == null ? List.of() : reviewRepository.findByMedia(importedShow);
        UserContext userContext = resolveUserContext(userParam, importedShow);

        TmdbTvDetailsDTO tvDetails = tmdbService.fetchTvDetails(tmdbId);
        tmdbService.refreshShowSnapshotIfImported(tmdbId, tvDetails);

        return showMetadataMapper.mapToShowDetailsDTO(tvDetails, reviews, userContext.isFavourited(), userContext.watchStatus());
    }

    @Transactional
    public ShowSeasonsDetailsDTO getShowSeasonDetails(Long tmdbId, Integer seasonNumber, MediaType mediaType) {
        validateShowType(mediaType);
        Media media = mediaResolutionService.resolveMediaByTmdbId(tmdbId, mediaType);

        ShowSeason cachedSeason = showSeasonRepository.findByMediaIdAndSeasonNumber(media.getId(), seasonNumber).orElse(null);
        List<ShowEpisode> cachedEpisodes = showEpisodeRepository.findAllByMediaIdAndSeasonNumberOrderByEpisodeNumberAsc(media.getId(), seasonNumber);
        if (isFreshSeasonCache(cachedSeason, cachedEpisodes)) {
            return showMetadataMapper.mapToShowSeasonDetails(tmdbId, cachedSeason, cachedEpisodes);
        }

        TmdbTvSeasonDTO seasonDetails = tmdbService.fetchTvSeasonDetails(tmdbId, seasonNumber);
        CachedSeasonData cachedData = cacheSeasonDetails(media, seasonNumber, seasonDetails);
        return showMetadataMapper.mapToShowSeasonDetails(tmdbId, cachedData.season(), cachedData.episodes());
    }

    @Transactional
    public List<ShowEpisode> ensureStandardEpisodesCached(Media media, Long tmdbId, TmdbTvDetailsDTO tvDetails) {
        for (TmdbTvSeasonSummaryDTO seasonSummary : tvDetails.getSeasons()) {
            Integer seasonNumber = seasonSummary.getSeasonNumber();
            if (seasonNumber == null || seasonNumber <= 0) {
                continue;
            }

            ShowSeason cachedSeason = showSeasonRepository.findByMediaIdAndSeasonNumber(media.getId(), seasonNumber).orElse(null);
            List<ShowEpisode> cachedEpisodes = showEpisodeRepository.findAllByMediaIdAndSeasonNumberOrderByEpisodeNumberAsc(media.getId(), seasonNumber);
            if (isFreshSeasonCache(cachedSeason, cachedEpisodes)) {
                continue;
            }

            TmdbTvSeasonDTO seasonDetails = tmdbService.fetchTvSeasonDetails(tmdbId, seasonNumber);
            cacheSeasonDetails(media, seasonNumber, seasonDetails);
        }

        return showEpisodeRepository.findAllByMediaIdOrderBySeasonNumberAscEpisodeNumberAsc(media.getId());
    }

    public MediaType validateShowType(MediaType mediaType) {
        if (mediaType == null) {
            throw new IllegalArgumentException("Media type is required.");
        }
        if (mediaType != MediaType.SHOW) {
            throw new IllegalArgumentException("This endpoint is only valid for shows.");
        }
        return mediaType;
    }

    private boolean isFreshSeasonCache(ShowSeason season, List<ShowEpisode> episodes) {
        if (season == null || season.getSyncedAt() == null) {
            return false;
        }

        LocalDateTime cutoff = LocalDateTime.now().minusDays(SEASON_CACHE_TTL_DAYS);
        if (season.getSyncedAt().isBefore(cutoff)) {
            return false;
        }

        int expectedEpisodeCount = season.getEpisodeCount() == null ? episodes.size() : season.getEpisodeCount();
        if (expectedEpisodeCount == 0) {
            return true;
        }

        return episodes.size() >= expectedEpisodeCount
            && episodes.stream().allMatch(episode -> episode.getSyncedAt() != null && !episode.getSyncedAt().isBefore(cutoff));
    }

    private CachedSeasonData cacheSeasonDetails(Media media, Integer seasonNumber, TmdbTvSeasonDTO seasonDetails) {
        LocalDateTime now = LocalDateTime.now();
        List<TmdbTvEpisodeDTO> seasonEpisodes = seasonDetails.getEpisodes() == null
            ? List.of()
            : seasonDetails.getEpisodes().stream()
                .sorted(Comparator.comparing(TmdbTvEpisodeDTO::getEpisodeNumber, Comparator.nullsLast(Integer::compareTo)))
                .toList();

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
        season.setSyncedAt(now);
        ShowSeason savedSeason = showSeasonRepository.save(season);

        showEpisodeRepository.deleteByMediaIdAndSeasonNumber(media.getId(), seasonNumber);
        showEpisodeRepository.flush();
        List<ShowEpisode> savedEpisodes = seasonEpisodes.isEmpty()
            ? List.of()
            : showEpisodeRepository.saveAll(seasonEpisodes.stream()
                .map(episode -> showMetadataMapper.mapToCachedEpisode(media, seasonNumber, episode, now))
                .toList());

        return new CachedSeasonData(savedSeason, savedEpisodes);
    }

    private UserContext resolveUserContext(Users userParam, Media media) {
        if (userParam == null) {
            return new UserContext(false, WatchStatus.NONE);
        }

        Long userId = java.util.Objects.requireNonNull(java.util.Objects.requireNonNull(userParam, "userParam").getId(), "userParam.id");
        Users user = usersRepository.findByIdWithFavorites(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        boolean isFavourited = media != null && user.getFavorites().contains(media);
        UserMediaStatus userStatus = media == null ? null : userMediaStatusRepository.findByUserAndMedia(user, media).orElse(null);
        WatchStatus watchStatus = userStatus != null ? userStatus.getStatus() : WatchStatus.NONE;

        return new UserContext(isFavourited, watchStatus);
    }

    private record CachedSeasonData(ShowSeason season, List<ShowEpisode> episodes) {
    }

    private record UserContext(boolean isFavourited, WatchStatus watchStatus) {
    }
}
