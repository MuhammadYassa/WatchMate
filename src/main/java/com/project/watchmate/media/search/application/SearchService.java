package com.project.watchmate.media.search.application;

import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;

import com.project.watchmate.media.tmdb.client.TmdbClient;
import com.project.watchmate.media.search.dto.PaginatedSearchResponseDTO;
import com.project.watchmate.media.search.dto.SearchItemDTO;
import com.project.watchmate.media.tmdb.dto.TmdbMovieDTO;
import com.project.watchmate.media.tmdb.dto.TmdbResponseDTO;
import com.project.watchmate.media.catalog.domain.Genre;
import com.project.watchmate.media.catalog.domain.MediaType;
import com.project.watchmate.media.catalog.persistence.GenreRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final GenreRepository genreRepository;

    private final TmdbClient tmdbClient;

    public PaginatedSearchResponseDTO search(String query, int page){
        
        TmdbResponseDTO dto = tmdbClient.searchMulti(query, page);

        if (dto == null || dto.getResults() == null) {
            return new PaginatedSearchResponseDTO(List.of(), page, 0, 0);
        }

        List<TmdbMovieDTO> filteredResults = dto.getResults().stream()
            .filter(this::isSupportedMediaResult)
            .toList();
        Map<MediaType, Map<Long, Genre>> genresByMediaType = resolveGenresByMediaType(filteredResults);

        List<SearchItemDTO> filtered = filteredResults.stream()
            .map(result -> SearchItemDTO.builder()
                .id(result.getId())
                .title(result.getTitle())
                .overview(result.getOverview())
                .mediaType(normalizeMediaType(result.getMediaType()))
                .posterPath(result.getPosterPath())
                .releaseDate(result.getReleaseDate())
                .voteAverage(result.getVoteAverage())
                .genres(resolveGenreNames(result, genresByMediaType))
                .build())
            .toList();

        return new PaginatedSearchResponseDTO(
            filtered,
            dto.getPage(),
            dto.getTotalPages(),
            dto.getTotalResults()
        );
    }

    private boolean isSupportedMediaResult(TmdbMovieDTO result) {
        String mediaType = normalizeMediaType(result.getMediaType());
        return ("movie".equals(mediaType) || "tv".equals(mediaType))
            && result.getTitle() != null
            && !result.getTitle().isBlank();
    }

    private Map<MediaType, Map<Long, Genre>> resolveGenresByMediaType(List<TmdbMovieDTO> results) {
        Map<MediaType, List<Long>> genreIdsByMediaType = new EnumMap<>(MediaType.class);
        for (TmdbMovieDTO result : results) {
            MediaType mediaType = resolveMediaType(result);
            List<Long> genreIds = result.getGenreIds();
            if (mediaType == null || genreIds == null || genreIds.isEmpty()) {
                continue;
            }
            List<Long> idsForType = genreIdsByMediaType.computeIfAbsent(mediaType, key -> new ArrayList<>());
            genreIds.stream()
                .filter(Objects::nonNull)
                .filter(id -> !idsForType.contains(id))
                .forEach(idsForType::add);
        }

        Map<MediaType, Map<Long, Genre>> genresByMediaType = new EnumMap<>(MediaType.class);
        genreIdsByMediaType.forEach((mediaType, genreIds) -> {
            Map<Long, Genre> genresByTmdbId = new LinkedHashMap<>();
            genreRepository.findByTmdbGenreIdInAndMediaType(genreIds, mediaType)
                .forEach(genre -> genresByTmdbId.putIfAbsent(genre.getTmdbGenreId(), genre));
            genresByMediaType.put(mediaType, genresByTmdbId);
        });
        return genresByMediaType;
    }

    private List<String> resolveGenreNames(TmdbMovieDTO result, Map<MediaType, Map<Long, Genre>> genresByMediaType) {
        List<Long> genreIds = result.getGenreIds();
        MediaType mediaType = resolveMediaType(result);
        if (genreIds == null || genreIds.isEmpty() || mediaType == null) {
            return List.of();
        }

        Map<Long, Genre> genresByTmdbId = genresByMediaType.getOrDefault(mediaType, Map.of());

        return genreIds.stream()
            .map(genresByTmdbId::get)
            .filter(Objects::nonNull)
            .map(Genre::getName)
            .toList();
    }

    private MediaType resolveMediaType(TmdbMovieDTO result) {
        return switch (normalizeMediaType(result.getMediaType())) {
            case "movie" -> MediaType.MOVIE;
            case "tv" -> MediaType.SHOW;
            default -> null;
        };
    }

    private String normalizeMediaType(String mediaType) {
        if (mediaType == null) {
            return "";
        }
        return mediaType.toLowerCase(Locale.ROOT);
    }
}



