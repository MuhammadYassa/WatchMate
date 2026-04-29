package com.project.watchmate.Services;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.watchmate.Clients.TmdbClient;
import com.project.watchmate.Dto.TmdbGenreDTO;
import com.project.watchmate.Dto.TmdbMovieDTO;
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
                    existing.setGenres(media.getGenres());
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

    private Media mapToMedia(TmdbMovieDTO tmdbMedia, MediaType type) {
        List<Long> genreIds = resolveGenreIds(tmdbMedia);
        List<Genre> genres = genreRepository.findAllById(genreIds);

        return Media.builder()
            .tmdbId(Objects.requireNonNull(tmdbMedia.getId(), "tmdbMedia.id"))
            .title(tmdbMedia.getTitle())
            .overview(tmdbMedia.getOverview())
            .posterPath(tmdbMedia.getPosterPath())
            .backdropPath(tmdbMedia.getBackdropPath())
            .releaseDate(TmdbMovieDTO.parseDate(tmdbMedia.getReleaseDate()).orElse(null))
            .rating(tmdbMedia.getVoteAverage())
            .genres(genres)
            .type(type)
            .build();
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
}
