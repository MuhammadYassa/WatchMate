package com.project.watchmate.show.metadata.mapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.project.watchmate.show.metadata.dto.NextEpisodeAiringDTO;
import com.project.watchmate.show.metadata.dto.ShowEpisodeDetailsDTO;
import com.project.watchmate.show.metadata.dto.ShowDetailsDTO;
import com.project.watchmate.show.metadata.dto.ShowSeasonSummaryDTO;
import com.project.watchmate.show.metadata.dto.ShowSeasonsDetailsDTO;
import com.project.watchmate.media.tmdb.dto.TmdbGenreDTO;
import com.project.watchmate.media.tmdb.dto.TmdbMovieDTO;
import com.project.watchmate.media.tmdb.dto.TmdbTvDetailsDTO;
import com.project.watchmate.media.tmdb.dto.TmdbTvEpisodeDTO;
import com.project.watchmate.media.tmdb.dto.TmdbTvSeasonSummaryDTO;
import com.project.watchmate.common.mapper.WatchMateMapper;
import com.project.watchmate.media.catalog.domain.Media;
import com.project.watchmate.media.catalog.domain.MediaType;
import com.project.watchmate.review.domain.Review;
import com.project.watchmate.media.catalog.domain.ShowEpisode;
import com.project.watchmate.media.catalog.domain.ShowSeason;
import com.project.watchmate.media.catalog.domain.WatchStatus;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ShowMetadataMapper {

    private final WatchMateMapper watchMateMapper;

    public NextEpisodeAiringDTO mapToNextEpisodeDto(Long tmdbId, TmdbTvDetailsDTO tvDetails) {
        return NextEpisodeAiringDTO.builder()
            .tmdbId(tmdbId)
            .nextEpisodeAirDate(TmdbMovieDTO.parseDate(tvDetails.getNextEpisodeToAir() == null ? null : tvDetails.getNextEpisodeToAir().getAirDate()).orElse(null))
            .seasonNumber(tvDetails.getNextEpisodeToAir() == null ? null : tvDetails.getNextEpisodeToAir().getSeasonNumber())
            .episodeNumber(tvDetails.getNextEpisodeToAir() == null ? null : tvDetails.getNextEpisodeToAir().getEpisodeNumber())
            .episodeName(tvDetails.getNextEpisodeToAir() == null ? null : tvDetails.getNextEpisodeToAir().getName())
            .lastEpisodeToAirSeasonNumber(tvDetails.getLastEpisodeToAir() == null ? null : tvDetails.getLastEpisodeToAir().getSeasonNumber())
            .lastEpisodeToAirEpisodeNumber(tvDetails.getLastEpisodeToAir() == null ? null : tvDetails.getLastEpisodeToAir().getEpisodeNumber())
            .lastEpisodeToAirName(tvDetails.getLastEpisodeToAir() == null ? null : tvDetails.getLastEpisodeToAir().getName())
            .build();
    }

    public ShowDetailsDTO mapToShowDetailsDTO(
        TmdbTvDetailsDTO tvDetails,
        List<Review> reviews,
        Boolean isFavourited,
        WatchStatus watchStatus
    ) {
        LocalDate firstAirDate = TmdbMovieDTO.parseDate(tvDetails.getFirstAirDate()).orElse(null);

        return ShowDetailsDTO.builder()
            .tmdbId(tvDetails.getId())
            .type(MediaType.SHOW)
            .title(tvDetails.getName())
            .overview(tvDetails.getOverview())
            .posterPath(tvDetails.getPosterPath())
            .backdropPath(tvDetails.getBackdropPath())
            .firstAirDate(firstAirDate)
            .rating(tvDetails.getVoteAverage())
            .genres(tvDetails.getGenres().stream().map(TmdbGenreDTO::getName).toList())
            .reviews(reviews.stream().map(watchMateMapper::mapToReviewDTO).toList())
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
            .seasons(mapSeasonSummaries(tvDetails.getSeasons()))
            .isFavourited(isFavourited)
            .watchStatus(watchStatus)
            .build();
    }

    public ShowEpisode mapToCachedEpisode(Media media, Integer seasonNumber, TmdbTvEpisodeDTO episode, LocalDateTime syncedAt) {
        return ShowEpisode.builder()
            .media(media)
            .seasonNumber(episode.getSeasonNumber() == null ? seasonNumber : episode.getSeasonNumber())
            .episodeNumber(episode.getEpisodeNumber())
            .title(episode.getName())
            .overview(episode.getOverview())
            .stillPath(episode.getStillPath())
            .airDate(TmdbMovieDTO.parseDate(episode.getAirDate()).orElse(null))
            .runtime(episode.getRuntime())
            .lastTmdbSyncAt(syncedAt)
            .build();
    }

    public ShowSeasonsDetailsDTO mapToShowSeasonDetails(Long tmdbId, ShowSeason season, List<ShowEpisode> episodes) {
        return mapToShowSeasonDetails(tmdbId, season, episodes, Set.of());
    }

    public ShowSeasonsDetailsDTO mapToShowSeasonDetails(Long tmdbId, ShowSeason season, List<ShowEpisode> episodes, Set<String> watchedEpisodeKeys) {
        return ShowSeasonsDetailsDTO.builder()
            .tmdbId(tmdbId)
            .seasonNumber(season.getSeasonNumber())
            .name(season.getName())
            .overview(season.getOverview())
            .posterPath(season.getPosterPath())
            .airDate(season.getAirDate())
            .episodeCount(season.getEpisodeCount())
            .episodes(episodes.stream()
                .sorted(Comparator.comparing(ShowEpisode::getEpisodeNumber, Comparator.nullsLast(Integer::compareTo)))
                .map(episode -> mapCachedEpisode(episode, watchedEpisodeKeys))
                .toList())
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

    private ShowEpisodeDetailsDTO mapCachedEpisode(ShowEpisode episode, Set<String> watchedEpisodeKeys) {
        LocalDate airDate = episode.getAirDate();
        return ShowEpisodeDetailsDTO.builder()
            .tmdbEpisodeId(null)
            .seasonNumber(episode.getSeasonNumber())
            .episodeNumber(episode.getEpisodeNumber())
            .name(episode.getTitle())
            .overview(episode.getOverview())
            .airDate(airDate)
            .runtime(episode.getRuntime())
            .stillPath(episode.getStillPath())
            .isAired(airDate != null && !airDate.isAfter(LocalDate.now()))
            .watched(watchedEpisodeKeys.contains(progressKey(episode.getSeasonNumber(), episode.getEpisodeNumber())))
            .build();
    }

    private String progressKey(Integer seasonNumber, Integer episodeNumber) {
        return seasonNumber + ":" + episodeNumber;
    }
}




