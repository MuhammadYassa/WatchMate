package com.project.watchmate.movie.application;

import java.util.List;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.watchmate.common.cache.WatchMateCacheNames;
import com.project.watchmate.media.catalog.domain.Genre;
import com.project.watchmate.media.catalog.domain.Media;
import com.project.watchmate.media.catalog.domain.MediaType;
import com.project.watchmate.media.catalog.persistence.MediaRepository;
import com.project.watchmate.media.tmdb.application.TmdbService;
import com.project.watchmate.movie.dto.PublicMovieDetailBaseDTO;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PublicMediaDetailBaseCacheService {

    private final MediaRepository mediaRepository;

    private final TmdbService tmdbService;

    @Transactional(readOnly = true)
    @Cacheable(
        cacheNames = WatchMateCacheNames.PUBLIC_MEDIA_DETAIL_BASE,
        key = "T(com.project.watchmate.common.cache.WatchMateCacheKeys).media(#type, #tmdbId)",
        unless = "#result == null"
    )
    public PublicMovieDetailBaseDTO getMovieBase(Long tmdbId, MediaType type) {
        Media media = mediaRepository.findByTmdbIdAndType(tmdbId, type)
            .orElseGet(() -> tmdbService.fetchMediaByTmdbId(tmdbId, type));
        return toPublicBase(media);
    }

    private PublicMovieDetailBaseDTO toPublicBase(Media media) {
        List<String> genres = media.getGenres() == null
            ? List.of()
            : media.getGenres().stream().map(Genre::getName).toList();

        return PublicMovieDetailBaseDTO.builder()
            .tmdbId(media.getTmdbId())
            .title(media.getTitle())
            .overview(media.getOverview())
            .posterPath(media.getPosterPath())
            .backdropPath(media.getBackdropPath())
            .releaseDate(media.getReleaseDate())
            .rating(media.getRating())
            .type(media.getType())
            .genres(genres)
            .build();
    }
}
