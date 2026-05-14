package com.project.watchmate.Services;

import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

import com.project.watchmate.Clients.TmdbClient;
import com.project.watchmate.Dto.PaginatedSearchResponseDTO;
import com.project.watchmate.Dto.SearchItemDTO;
import com.project.watchmate.Dto.TmdbMovieDTO;
import com.project.watchmate.Dto.TmdbResponseDTO;
import com.project.watchmate.Models.Genre;
import com.project.watchmate.Repositories.GenreRepository;

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

        List<SearchItemDTO> filtered = dto.getResults().stream()
            .filter(this::isSupportedMediaResult)
            .map(result -> SearchItemDTO.builder()
                .id(result.getId())
                .title(result.getTitle())
                .overview(result.getOverview())
                .mediaType(normalizeMediaType(result.getMediaType()))
                .posterPath(result.getPosterPath())
                .releaseDate(result.getReleaseDate())
                .voteAverage(result.getVoteAverage())
                .genres(resolveGenreNames(result))
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

    private List<String> resolveGenreNames(TmdbMovieDTO result) {
        List<Long> genreIds = result.getGenreIds();
        if (genreIds == null || genreIds.isEmpty()) {
            return List.of();
        }

        return genreRepository.findAllById(genreIds).stream()
            .map(Genre::getName)
            .toList();
    }

    private String normalizeMediaType(String mediaType) {
        if (mediaType == null) {
            return "";
        }
        return mediaType.toLowerCase(Locale.ROOT);
    }
}
