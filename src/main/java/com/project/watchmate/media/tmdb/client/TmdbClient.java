package com.project.watchmate.media.tmdb.client;

import java.util.List;

import com.project.watchmate.media.tmdb.dto.TmdbGenreDTO;
import com.project.watchmate.media.tmdb.dto.TmdbCreditsDTO;
import com.project.watchmate.media.tmdb.dto.TmdbMovieDTO;
import com.project.watchmate.media.tmdb.dto.TmdbResponseDTO;
import com.project.watchmate.media.tmdb.dto.TmdbVideosResponseDTO;
import com.project.watchmate.media.tmdb.dto.TmdbTvDetailsDTO;
import com.project.watchmate.media.tmdb.dto.TmdbTvSeasonDTO;
import com.project.watchmate.media.tmdb.dto.TmdbWatchProvidersResponseDTO;
import com.project.watchmate.media.catalog.domain.MediaType;

public interface TmdbClient {

    List<TmdbGenreDTO> fetchGenres(String type);

    List<TmdbMovieDTO> fetchPopular(String type);

    List<TmdbMovieDTO> fetchTrending(String type);

    List<TmdbMovieDTO> fetchUpcomingMovies();

    List<TmdbMovieDTO> fetchAiringToday();

    List<TmdbMovieDTO> fetchOnTheAir();

    TmdbMovieDTO fetchMediaById(Long tmdbId, MediaType type);

    TmdbTvDetailsDTO fetchTvDetailsById(Long tmdbId);

    TmdbTvSeasonDTO fetchTvSeasonDetails(Long tmdbId, Integer seasonNumber);

    TmdbCreditsDTO fetchCredits(Long tmdbId, MediaType type);

    TmdbVideosResponseDTO fetchVideos(Long tmdbId, MediaType type);

    TmdbWatchProvidersResponseDTO fetchWatchProviders(Long tmdbId, MediaType type);

    TmdbResponseDTO searchMulti(String query, int page);

    TmdbResponseDTO discoverByGenre(String type, Long genreId, int page);
}



