package com.project.watchmate.Clients;

import java.net.ConnectException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.project.watchmate.Dto.TmdbGenreDTO;
import com.project.watchmate.Dto.TmdbGenreResponseDTO;
import com.project.watchmate.Dto.TmdbMovieDTO;
import com.project.watchmate.Dto.TmdbResponseDTO;
import com.project.watchmate.Dto.TmdbTvDetailsDTO;
import com.project.watchmate.Dto.TmdbTvSeasonDTO;
import com.project.watchmate.Exception.MediaNotFoundException;
import com.project.watchmate.Exception.TmdbUnavailableException;
import com.project.watchmate.Models.MediaType;

import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.handler.timeout.WriteTimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.netty.http.client.PrematureCloseException;

@Component
@Slf4j
@RequiredArgsConstructor
public class TmdbClientImpl implements TmdbClient {
    private static final String TMDB_UNAVAILABLE_MESSAGE = "TMDB is temporarily unavailable. Please try again shortly.";

    private final WebClient tmdbWebClient;

    @Override
    public List<TmdbGenreDTO> fetchGenres(String type) {
        try {
            TmdbGenreResponseDTO response = tmdbWebClient.get()
                .uri("/genre/{type}/list", type)
                .retrieve()
                .bodyToMono(TmdbGenreResponseDTO.class)
                .block();

            return response != null ? response.getGenres() : List.of();
        } catch (WebClientResponseException ex) {
            throw handleWebClientResponseException(ex, "genre fetch", "type=" + type);
        } catch (Exception ex) {
            throw handleGenericException(ex, "genre fetch", "type=" + type);
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
            throw handleWebClientResponseException(ex, "list fetch", "label=" + label);
        } catch (Exception ex) {
            throw handleGenericException(ex, "list fetch", "label=" + label);
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
            throw handleWebClientResponseException(ex, "media lookup", "tmdbId=" + tmdbId + " type=" + type);
        } catch (Exception ex) {
            throw handleGenericException(ex, "media lookup", "tmdbId=" + tmdbId + " type=" + type);
        }
    }

    @Override
    public TmdbTvDetailsDTO fetchTvDetailsById(Long tmdbId) {
        String uri = "/tv/" + tmdbId + "?language=en-US";

        try {
            return tmdbWebClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(TmdbTvDetailsDTO.class)
                .blockOptional()
                .orElseThrow(() ->
                    new MediaNotFoundException("TMDB show not found for ID: " + tmdbId));
        } catch (WebClientResponseException.NotFound ex) {
            log.warn("TMDB show details not found tmdbId={}", tmdbId);
            throw new MediaNotFoundException("TMDB show not found for ID: " + tmdbId);
        } catch (WebClientResponseException ex) {
            throw handleWebClientResponseException(ex, "show details lookup", "tmdbId=" + tmdbId);
        } catch (Exception ex) {
            throw handleGenericException(ex, "show details lookup", "tmdbId=" + tmdbId);
        }
    }

    @Override
    public TmdbTvSeasonDTO fetchTvSeasonDetails(Long tmdbId, Integer seasonNumber) {
        String uri = "/tv/" + tmdbId + "/season/" + seasonNumber + "?language=en-US";

        try {
            return tmdbWebClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(TmdbTvSeasonDTO.class)
                .blockOptional()
                .orElseThrow(() ->
                    new MediaNotFoundException("TMDB season not found for show ID: " + tmdbId + " season: " + seasonNumber));
        } catch (WebClientResponseException.NotFound ex) {
            log.warn("TMDB season not found tmdbId={} season={}", tmdbId, seasonNumber);
            throw new MediaNotFoundException("TMDB season not found for show ID: " + tmdbId + " season: " + seasonNumber);
        } catch (WebClientResponseException ex) {
            throw handleWebClientResponseException(ex, "season lookup", "tmdbId=" + tmdbId + " season=" + seasonNumber);
        } catch (Exception ex) {
            throw handleGenericException(ex, "season lookup", "tmdbId=" + tmdbId + " season=" + seasonNumber);
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
            throw handleWebClientResponseException(ex, "search", "page=" + page);
        } catch (Exception ex) {
            throw handleGenericException(ex, "search", "page=" + page);
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
            throw handleWebClientResponseException(ex, "discover", "type=" + type + " genreId=" + genreId + " page=" + page);
        } catch (Exception ex) {
            throw handleGenericException(ex, "discover", "type=" + type + " genreId=" + genreId + " page=" + page);
        }
    }

    private RuntimeException handleWebClientResponseException(WebClientResponseException ex, String operation, String context) {
        if (ex.getStatusCode().is5xxServerError()) {
            log.error("TMDB {} unavailable context={} status={}", operation, context, ex.getStatusCode().value(), ex);
            return new TmdbUnavailableException(TMDB_UNAVAILABLE_MESSAGE, ex);
        }

        log.warn("TMDB {} failed context={} status={}", operation, context, ex.getStatusCode().value());
        return ex;
    }

    private RuntimeException handleGenericException(Exception ex, String operation, String context) {
        if (ex instanceof WebClientRequestException || isNetworkOrTimeoutFailure(ex)) {
            log.error("TMDB {} unavailable context={} reason={}", operation, context, ex.getClass().getSimpleName(), ex);
            return new TmdbUnavailableException(TMDB_UNAVAILABLE_MESSAGE, ex);
        }

        log.error("TMDB {} failed context={}", operation, context, ex);
        return ex instanceof RuntimeException runtimeException
            ? runtimeException
            : new RuntimeException(ex);
    }

    private boolean isNetworkOrTimeoutFailure(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof TimeoutException
                || current instanceof SocketTimeoutException
                || current instanceof ConnectException
                || current instanceof UnknownHostException
                || current instanceof SocketException
                || current instanceof ReadTimeoutException
                || current instanceof WriteTimeoutException
                || current instanceof PrematureCloseException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
