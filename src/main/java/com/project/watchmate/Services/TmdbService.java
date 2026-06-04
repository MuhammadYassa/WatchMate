package com.project.watchmate.Services;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.watchmate.Clients.TmdbClient;
import com.project.watchmate.Dto.TmdbGenreDTO;
import com.project.watchmate.Dto.TmdbMovieDTO;
import com.project.watchmate.Dto.TmdbTvDetailsDTO;
import com.project.watchmate.Dto.TmdbTvSeasonDTO;
import com.project.watchmate.Exception.MediaNotFoundException;
import com.project.watchmate.Models.Genre;
import com.project.watchmate.Models.Media;
import com.project.watchmate.Models.MediaType;
import com.project.watchmate.Repositories.GenreRepository;
import com.project.watchmate.Repositories.MediaRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class TmdbService {

    private final TmdbClient tmdbClient;

    private final MediaRepository mediaRepository;

    private final GenreRepository genreRepository;

    @Transactional
    public List<Media> upsertMediaFromTmdb(List<TmdbMovieDTO> tmdbResults, MediaType mediaType) {
        List<Media> mediaList = tmdbResults.stream()
            .map(result -> mapToMedia(result, mediaType))
            .toList();
        return saveAndUpdateMedia(mediaList);
    }

    @Transactional
    public List<Media> saveAndUpdateMedia(List<Media> mediaList) {
        return mediaList.stream()
            .map(media -> mediaRepository.findByTmdbIdAndType(media.getTmdbId(), media.getType())
                .map(existing -> {
                    existing.setTitle(media.getTitle());
                    existing.setOverview(media.getOverview());
                    existing.setPosterPath(media.getPosterPath());
                    existing.setBackdropPath(media.getBackdropPath());
                    existing.setReleaseDate(media.getReleaseDate());
                    existing.setRating(media.getRating());
                    syncGenres(existing, media.getGenres());
                    return mediaRepository.save(existing);
                })
                .orElseGet(() -> mediaRepository.save(media)))
            .toList();
    }

    public Media fetchMediaByTmdbId(Long tmdbId, MediaType type) {
        TmdbMovieDTO tmdbMedia = tmdbClient.fetchMediaById(tmdbId, type);
        if (tmdbMedia == null) {
            throw new MediaNotFoundException("TMDB media not found for ID: " + tmdbId);
        }

        Media media = mapToMedia(tmdbMedia, type);
        log.info("Fetched media details from TMDB tmdbId={} type={} genreCount={}", tmdbId, type, media.getGenres().size());
        return media;
    }

    public TmdbTvDetailsDTO fetchTvDetails(Long tmdbId) {
        TmdbTvDetailsDTO tvDetails = tmdbClient.fetchTvDetailsById(tmdbId);
        if (tvDetails == null) {
            throw new MediaNotFoundException("TMDB show not found for ID: " + tmdbId);
        }
        return tvDetails;
    }

    public TmdbTvSeasonDTO fetchTvSeasonDetails(Long tmdbId, Integer seasonNumber) {
        TmdbTvSeasonDTO seasonDetails = tmdbClient.fetchTvSeasonDetails(tmdbId, seasonNumber);
        if (seasonDetails == null) {
            throw new MediaNotFoundException("TMDB season not found for show ID: " + tmdbId + " season: " + seasonNumber);
        }
        return seasonDetails;
    }

    @Transactional
    public Media refreshShowSnapshot(Media media, TmdbTvDetailsDTO tvDetails) {
        if (media.getType() != MediaType.SHOW) {
            throw new IllegalArgumentException("Only shows can store next-airing snapshot data.");
        }

        LocalDateTime now = LocalDateTime.now();
        media.setTitle(tvDetails.getName());
        media.setOverview(tvDetails.getOverview());
        media.setPosterPath(tvDetails.getPosterPath());
        media.setBackdropPath(tvDetails.getBackdropPath());
        media.setReleaseDate(TmdbMovieDTO.parseDate(tvDetails.getFirstAirDate()).orElse(null));
        media.setRating(tvDetails.getVoteAverage());
        media.setNextEpisodeAirDate(TmdbMovieDTO.parseDate(tvDetails.getNextEpisodeToAir() == null ? null : tvDetails.getNextEpisodeToAir().getAirDate()).orElse(null));
        media.setNextEpisodeSeasonNumber(tvDetails.getNextEpisodeToAir() == null ? null : tvDetails.getNextEpisodeToAir().getSeasonNumber());
        media.setNextEpisodeEpisodeNumber(tvDetails.getNextEpisodeToAir() == null ? null : tvDetails.getNextEpisodeToAir().getEpisodeNumber());
        media.setNextEpisodeName(tvDetails.getNextEpisodeToAir() == null ? null : tvDetails.getNextEpisodeToAir().getName());
        media.setLastEpisodeToAirSeasonNumber(tvDetails.getLastEpisodeToAir() == null ? null : tvDetails.getLastEpisodeToAir().getSeasonNumber());
        media.setLastEpisodeToAirEpisodeNumber(tvDetails.getLastEpisodeToAir() == null ? null : tvDetails.getLastEpisodeToAir().getEpisodeNumber());
        media.setLastEpisodeToAirName(tvDetails.getLastEpisodeToAir() == null ? null : tvDetails.getLastEpisodeToAir().getName());
        media.setLastAirDate(TmdbMovieDTO.parseDate(tvDetails.getLastAirDate()).orElse(null));
        media.setNumberOfSeasons(tvDetails.getNumberOfSeasons());
        media.setNumberOfEpisodes(tvDetails.getNumberOfEpisodes());
        media.setTmdbShowStatus(tvDetails.getStatus());
        media.setNextAiringSyncedAt(now);
        media.setLastTmdbSyncAt(now);
        return mediaRepository.save(media);
    }

    @Transactional
    public void refreshShowSnapshotIfImported(Long tmdbId, TmdbTvDetailsDTO tvDetails) {
        mediaRepository.findByTmdbIdAndType(tmdbId, MediaType.SHOW)
            .ifPresent(media -> refreshShowSnapshot(media, tvDetails));
    }

    private Media mapToMedia(TmdbMovieDTO tmdbMedia, MediaType type) {
        List<Long> genreIds = resolveGenreIds(tmdbMedia);
        List<Genre> genres = resolveGenres(genreIds, type);

        return Media.builder()
            .tmdbId(Objects.requireNonNull(tmdbMedia.getId(), "tmdbMedia.id"))
            .title(tmdbMedia.getTitle())
            .overview(tmdbMedia.getOverview())
            .posterPath(tmdbMedia.getPosterPath())
            .backdropPath(tmdbMedia.getBackdropPath())
            .releaseDate(TmdbMovieDTO.parseDate(tmdbMedia.getReleaseDate()).orElse(null))
            .rating(tmdbMedia.getVoteAverage())
            .genres(new ArrayList<>(genres))
            .type(type)
            .build();
    }

    private List<Genre> resolveGenres(List<Long> genreIds, MediaType mediaType) {
        if (genreIds.isEmpty()) {
            return new ArrayList<>();
        }

        Map<Long, Genre> genresByTmdbId = new LinkedHashMap<>();
        genreRepository.findByTmdbGenreIdInAndMediaType(genreIds, mediaType)
            .forEach(genre -> genresByTmdbId.putIfAbsent(genre.getTmdbGenreId(), genre));

        return genreIds.stream()
            .map(genresByTmdbId::get)
            .filter(Objects::nonNull)
            .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }

    private List<Long> resolveGenreIds(TmdbMovieDTO tmdbMedia) {
        if (tmdbMedia.getGenreIds() != null && !tmdbMedia.getGenreIds().isEmpty()) {
            return tmdbMedia.getGenreIds();
        }
        if (tmdbMedia.getGenres() == null) {
            return List.of();
        }
        return tmdbMedia.getGenres().stream()
            .map(TmdbGenreDTO::getId)
            .toList();
    }

    private void syncGenres(Media existing, List<Genre> genres) {
        existing.getGenres().clear();
        existing.getGenres().addAll(genres);
    }
}
