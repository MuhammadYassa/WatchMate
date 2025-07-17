package com.project.watchmate.Services;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.project.watchmate.Dto.PaginatedSearchResponseDTO;
import com.project.watchmate.Dto.TmdbMovieDTO;
import com.project.watchmate.Dto.TmdbResponseDTO;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SearchService {

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
            List<TmdbMovieDTO> filtered = dto.getResults().stream()
            .filter(result -> result.getTitle() != null && !result.getTitle().isBlank())
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
