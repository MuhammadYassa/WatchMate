package com.project.watchmate.Clients;

import java.util.List;

import com.project.watchmate.Dto.TmdbGenreDTO;
import com.project.watchmate.Dto.TmdbMovieDTO;
import com.project.watchmate.Dto.TmdbResponseDTO;
import com.project.watchmate.Dto.TmdbTvDetailsDTO;
import com.project.watchmate.Dto.TmdbTvSeasonDTO;
import com.project.watchmate.Models.MediaType;

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

    TmdbResponseDTO searchMulti(String query, int page);

    TmdbResponseDTO discoverByGenre(String type, Long genreId, int page);
}
