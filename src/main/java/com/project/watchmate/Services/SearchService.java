package com.project.watchmate.Services;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

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

    private final WebClient tmdbWebClient;

    public PaginatedSearchResponseDTO search(String query, int page){
        
        return tmdbWebClient.get()
        .uri(uriBuilder -> uriBuilder
        .path("/search/multi")
        .queryParam("query", query)
        .queryParam("language", "en-US")
        .queryParam("page", page)
        .build())
        .retrieve()
        .bodyToMono(TmdbResponseDTO.class)
        .blockOptional()
        .map(dto -> {
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
                .genres(genreRepository.findAllById(Objects.requireNonNull(result.getGenreIds(), "genreIds")).stream().map(Genre::getName).toList())
                .build())
                .toList();
            return new PaginatedSearchResponseDTO(
                filtered,
                dto.getPage(),
                dto.getTotalPages(),
                dto.getTotalResults()

            );
        })
        .orElseGet(() -> new PaginatedSearchResponseDTO(List.of(), page, 0, 0));
    }
}
