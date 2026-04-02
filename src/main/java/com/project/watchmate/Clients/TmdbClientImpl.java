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
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
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
            log.warn("TMDB genre fetch failed type={} reason={}", type, e.getClass().getSimpleName());
            return Collections.emptyList();
        }
    }

    @Override
    public List<TmdbMovieDTO> fetchPopular(String type) {
        try {
            return tmdbWebClient.get()
                .uri("/" + type + "/popular?language=en-US&page=1")
                .retrieve()
                .bodyToMono(TmdbResponseDTO.class)
                .blockOptional()
                .map(TmdbResponseDTO::getResults)
                .orElse(List.of());
        } catch (WebClientResponseException ex) {
            log.warn("TMDB popular fetch failed type={} status={}", type, ex.getStatusCode().value());
            return List.of();
        } catch (Exception ex) {
            log.error("TMDB popular fetch failed type={}", type, ex);
            return List.of();
        }
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
            log.warn("TMDB media not found tmdbId={} type={}", tmdbId, type);
            throw new MediaNotFoundException("TMDB media not found for ID: " + tmdbId);
        } catch (WebClientResponseException ex) {
            log.error("TMDB media lookup failed tmdbId={} type={} status={}", tmdbId, type, ex.getStatusCode().value(), ex);
            throw ex;
        }
    }

    @Override
    public TmdbResponseDTO searchMulti(String query, int page) {
        try {
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
        } catch (WebClientResponseException ex) {
            log.warn("TMDB search failed page={} status={}", page, ex.getStatusCode().value());
            throw ex;
        } catch (Exception ex) {
            log.error("TMDB search failed page={}", page, ex);
            throw ex;
        }
    }
}
