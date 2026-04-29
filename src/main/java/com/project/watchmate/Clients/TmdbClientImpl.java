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
        } catch (WebClientResponseException ex) {
            log.warn("TMDB genre fetch failed type={} status={}", type, ex.getStatusCode().value());
            throw ex;
        } catch (Exception e) {
            log.warn("TMDB genre fetch failed type={} reason={}", type, e.getClass().getSimpleName());
            throw e;
        }
    }

    @Override
    public List<TmdbMovieDTO> fetchPopular(String type) {
        return fetchList("/" + type + "/popular?language=en-US&page=1", type + " popular");
    }

    @Override
    public List<TmdbMovieDTO> fetchTrending(String type) {
        return fetchList("/trending/" + type + "/day?language=en-US", type + " trending");
    }

    @Override
    public List<TmdbMovieDTO> fetchUpcomingMovies() {
        return fetchList("/movie/upcoming?language=en-US&page=1", "movie upcoming");
    }

    @Override
    public List<TmdbMovieDTO> fetchAiringToday() {
        return fetchList("/tv/airing_today?language=en-US&page=1", "tv airing today");
    }

    @Override
    public List<TmdbMovieDTO> fetchOnTheAir() {
        return fetchList("/tv/on_the_air?language=en-US&page=1", "tv on the air");
    }

    private List<TmdbMovieDTO> fetchList(String uri, String label) {
        try {
            return tmdbWebClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(TmdbResponseDTO.class)
                .blockOptional()
                .map(TmdbResponseDTO::getResults)
                .orElse(List.of());
        } catch (WebClientResponseException ex) {
            log.warn("TMDB list fetch failed label={} status={}", label, ex.getStatusCode().value());
            throw ex;
        } catch (Exception ex) {
            log.error("TMDB list fetch failed label={}", label, ex);
            throw ex;
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

    @Override
    public TmdbResponseDTO discoverByGenre(String type, Long genreId, int page) {
        try {
            return tmdbWebClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/discover/" + type)
                    .queryParam("with_genres", genreId)
                    .queryParam("language", "en-US")
                    .queryParam("page", page)
                    .build())
                .retrieve()
                .bodyToMono(TmdbResponseDTO.class)
                .block();
        } catch (WebClientResponseException ex) {
            log.warn("TMDB discover failed type={} genreId={} page={} status={}",
                type,
                genreId,
                page,
                ex.getStatusCode().value());
            throw ex;
        } catch (Exception ex) {
            log.error("TMDB discover failed type={} genreId={} page={}", type, genreId, page, ex);
            throw ex;
        }
    }
}
