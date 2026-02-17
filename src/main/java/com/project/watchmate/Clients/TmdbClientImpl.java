package com.project.watchmate.Clients;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.project.watchmate.Dto.TmdbGenreDTO;
import com.project.watchmate.Dto.TmdbGenreResponseDTO;
import com.project.watchmate.Dto.TmdbMovieDTO;
import com.project.watchmate.Dto.TmdbResponseDTO;
import com.project.watchmate.Exception.MediaNotFoundException;
import com.project.watchmate.Models.MediaType;

import io.jsonwebtoken.lang.Collections;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TmdbClientImpl implements TmdbClient {

    private final WebClient tmdbWebClient;

    @Override
    public List<TmdbGenreDTO> fetchGenres(String type) {
        try {
            TmdbGenreResponseDTO response = tmdbWebClient.get()
                .uri("/genre/{type}/list", type)
                .retrieve()
                .bodyToMono(TmdbGenreResponseDTO.class)
                .block();

            return response != null ? response.getGenres() : Collections.emptyList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    @Override
    public List<TmdbMovieDTO> fetchPopular(String type) {
        return tmdbWebClient.get()
            .uri("/" + type + "/popular?language=en-US&page=1")
            .retrieve()
            .bodyToMono(TmdbResponseDTO.class)
            .blockOptional()
            .map(TmdbResponseDTO::getResults)
            .orElse(List.of());
    }

    @Override
    public TmdbMovieDTO fetchMediaById(Long tmdbId, MediaType type) {
        String typePath = (type == MediaType.MOVIE) ? "movie" : "tv";
        String uri = "/" + typePath + "/" + tmdbId + "?language=en-US";

        try {
            return tmdbWebClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(TmdbMovieDTO.class)
                .blockOptional()
                .orElseThrow(() ->
                    new MediaNotFoundException("TMDB media not found for ID: " + tmdbId));
        } catch (WebClientResponseException.NotFound ex) {
            throw new MediaNotFoundException("TMDB media not found for ID: " + tmdbId);
        }
    }

    @Override
    public TmdbResponseDTO searchMulti(String query, int page) {
        return tmdbWebClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/search/multi")
                .queryParam("query", query)
                .queryParam("language", "en-US")
                .queryParam("page", page)
                .build())
            .retrieve()
            .bodyToMono(TmdbResponseDTO.class)
            .block();
    }
}
