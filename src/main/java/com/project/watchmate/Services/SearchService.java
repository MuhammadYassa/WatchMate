package com.project.watchmate.Services;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;

import com.project.watchmate.Clients.TmdbClient;
import com.project.watchmate.Dto.PaginatedSearchResponseDTO;
import com.project.watchmate.Dto.SearchItemDTO;
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
            .filter(result -> result.getTitle() != null && !result.getTitle().isBlank())
            .map(result -> SearchItemDTO.builder()
                .id(result.getId())
                .title(result.getTitle())
                .overview(result.getOverview())
                .mediaType(result.getMediaType())
                .posterPath(result.getPosterPath())
                .releaseDate(result.getReleaseDate())
                .voteAverage(result.getVoteAverage())
                .genres(
                    genreRepository
                        .findAllById(
                            Objects.requireNonNull(result.getGenreIds(), "genreIds")
                        )
                        .stream()
                        .map(Genre::getName)
                        .toList()
                )
                .build())
            .toList();

        return new PaginatedSearchResponseDTO(
            filtered,
            dto.getPage(),
            dto.getTotalPages(),
            dto.getTotalResults()
        );
    }
}
