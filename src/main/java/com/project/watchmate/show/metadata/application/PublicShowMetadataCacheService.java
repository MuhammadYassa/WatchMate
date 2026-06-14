package com.project.watchmate.show.metadata.application;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.project.watchmate.common.cache.WatchMateCacheNames;
import com.project.watchmate.media.catalog.domain.Media;
import com.project.watchmate.media.catalog.domain.MediaType;
import com.project.watchmate.media.catalog.domain.ShowEpisode;
import com.project.watchmate.media.catalog.domain.ShowSeason;
import com.project.watchmate.media.tmdb.application.TmdbService;
import com.project.watchmate.media.tmdb.dto.TmdbGenreDTO;
import com.project.watchmate.media.tmdb.dto.TmdbMovieDTO;
import com.project.watchmate.media.tmdb.dto.TmdbTvDetailsDTO;
import com.project.watchmate.media.tmdb.dto.TmdbTvSeasonSummaryDTO;
import com.project.watchmate.show.catalog.application.ShowCatalogService;
import com.project.watchmate.show.metadata.dto.PublicShowEpisodeMetadataDTO;
import com.project.watchmate.show.metadata.dto.PublicShowMetadataDTO;
import com.project.watchmate.show.metadata.dto.PublicShowSeasonMetadataDTO;
import com.project.watchmate.show.metadata.dto.ShowSeasonSummaryDTO;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PublicShowMetadataCacheService {

    private final TmdbService tmdbService;

    private final ShowCatalogService showCatalogService;

    @Cacheable(
        cacheNames = WatchMateCacheNames.PUBLIC_SHOW_METADATA,
        key = "T(com.project.watchmate.common.cache.WatchMateCacheKeys).show(#tmdbId)",
        unless = "#result == null"
    )
    public PublicShowMetadataDTO getShowMetadata(Long tmdbId) {
        TmdbTvDetailsDTO tvDetails = tmdbService.fetchTvDetails(tmdbId);
        Media importedShow = showCatalogService.findImportedShow(tmdbId);
        if (importedShow != null) {
            tmdbService.refreshShowSnapshot(importedShow, tvDetails);
        }
        return toPublicShowMetadata(tvDetails);
    }

    @Cacheable(
        cacheNames = WatchMateCacheNames.PUBLIC_SEASON_METADATA,
        key = "T(com.project.watchmate.common.cache.WatchMateCacheKeys).season(#tmdbId, #seasonNumber)",
        unless = "#result == null"
    )
    public PublicShowSeasonMetadataDTO getSeasonMetadata(Long tmdbId, Integer seasonNumber) {
        var media = showCatalogService.ensureBasicShowImported(tmdbId);
        ShowCatalogService.CachedSeasonData cachedSeason = showCatalogService.ensureSeasonCached(media, tmdbId, seasonNumber);
        return toPublicSeasonMetadata(tmdbId, cachedSeason.season(), cachedSeason.episodes());
    }

    private PublicShowMetadataDTO toPublicShowMetadata(TmdbTvDetailsDTO tvDetails) {
        return PublicShowMetadataDTO.builder()
            .tmdbId(tvDetails.getId())
            .type(MediaType.SHOW)
            .title(tvDetails.getName())
            .overview(tvDetails.getOverview())
            .posterPath(tvDetails.getPosterPath())
            .backdropPath(tvDetails.getBackdropPath())
            .firstAirDate(TmdbMovieDTO.parseDate(tvDetails.getFirstAirDate()).orElse(null))
            .rating(tvDetails.getVoteAverage())
            .genres(tvDetails.getGenres() == null ? List.of() : tvDetails.getGenres().stream().map(TmdbGenreDTO::getName).toList())
            .numberOfSeasons(tvDetails.getNumberOfSeasons())
            .numberOfEpisodes(tvDetails.getNumberOfEpisodes())
            .lastAirDate(TmdbMovieDTO.parseDate(tvDetails.getLastAirDate()).orElse(null))
            .tmdbShowStatus(tvDetails.getStatus())
            .nextEpisodeAirDate(TmdbMovieDTO.parseDate(tvDetails.getNextEpisodeToAir() == null ? null : tvDetails.getNextEpisodeToAir().getAirDate()).orElse(null))
            .nextEpisodeSeasonNumber(tvDetails.getNextEpisodeToAir() == null ? null : tvDetails.getNextEpisodeToAir().getSeasonNumber())
            .nextEpisodeEpisodeNumber(tvDetails.getNextEpisodeToAir() == null ? null : tvDetails.getNextEpisodeToAir().getEpisodeNumber())
            .nextEpisodeName(tvDetails.getNextEpisodeToAir() == null ? null : tvDetails.getNextEpisodeToAir().getName())
            .lastEpisodeToAirSeasonNumber(tvDetails.getLastEpisodeToAir() == null ? null : tvDetails.getLastEpisodeToAir().getSeasonNumber())
            .lastEpisodeToAirEpisodeNumber(tvDetails.getLastEpisodeToAir() == null ? null : tvDetails.getLastEpisodeToAir().getEpisodeNumber())
            .lastEpisodeToAirName(tvDetails.getLastEpisodeToAir() == null ? null : tvDetails.getLastEpisodeToAir().getName())
            .seasons(mapSeasonSummaries(tvDetails.getSeasons() == null ? List.of() : tvDetails.getSeasons()))
            .build();
    }

    private PublicShowSeasonMetadataDTO toPublicSeasonMetadata(Long tmdbId, ShowSeason season, List<ShowEpisode> episodes) {
        return PublicShowSeasonMetadataDTO.builder()
            .tmdbId(tmdbId)
            .seasonNumber(season.getSeasonNumber())
            .name(season.getName())
            .overview(season.getOverview())
            .posterPath(season.getPosterPath())
            .airDate(season.getAirDate())
            .episodeCount(season.getEpisodeCount())
            .episodes(episodes.stream()
                .sorted(Comparator.comparing(ShowEpisode::getEpisodeNumber, Comparator.nullsLast(Integer::compareTo)))
                .map(this::toPublicEpisodeMetadata)
                .toList())
            .build();
    }

    private PublicShowEpisodeMetadataDTO toPublicEpisodeMetadata(ShowEpisode episode) {
        LocalDate airDate = episode.getAirDate();
        return PublicShowEpisodeMetadataDTO.builder()
            .tmdbEpisodeId(null)
            .seasonNumber(episode.getSeasonNumber())
            .episodeNumber(episode.getEpisodeNumber())
            .name(episode.getTitle())
            .overview(episode.getOverview())
            .airDate(airDate)
            .runtime(episode.getRuntime())
            .stillPath(episode.getStillPath())
            .isAired(airDate != null && !airDate.isAfter(LocalDate.now()))
            .build();
    }

    private List<ShowSeasonSummaryDTO> mapSeasonSummaries(List<TmdbTvSeasonSummaryDTO> seasons) {
        return seasons.stream()
            .sorted(Comparator.comparing(TmdbTvSeasonSummaryDTO::getSeasonNumber, Comparator.nullsLast(Integer::compareTo)))
            .map(this::mapSeasonSummary)
            .toList();
    }

    private ShowSeasonSummaryDTO mapSeasonSummary(TmdbTvSeasonSummaryDTO seasonSummary) {
        return ShowSeasonSummaryDTO.builder()
            .tmdbSeasonId(seasonSummary.getId())
            .seasonNumber(seasonSummary.getSeasonNumber())
            .name(seasonSummary.getName())
            .overview(seasonSummary.getOverview())
            .airDate(TmdbMovieDTO.parseDate(seasonSummary.getAirDate()).orElse(null))
            .episodeCount(seasonSummary.getEpisodeCount())
            .posterPath(seasonSummary.getPosterPath())
            .build();
    }
}
