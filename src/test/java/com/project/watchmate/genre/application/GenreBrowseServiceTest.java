package com.project.watchmate.genre.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.project.watchmate.media.tmdb.client.TmdbClient;
import com.project.watchmate.discovery.dto.DiscoveryMediaItemDTO;
import com.project.watchmate.genre.dto.GenreBrowseResponseDTO;
import com.project.watchmate.media.tmdb.dto.TmdbMovieDTO;
import com.project.watchmate.media.tmdb.dto.TmdbResponseDTO;
import com.project.watchmate.common.error.GenreNotFoundException;
import com.project.watchmate.common.mapper.WatchMateMapper;
import com.project.watchmate.media.catalog.domain.Genre;
import com.project.watchmate.media.catalog.domain.MediaType;
import com.project.watchmate.media.catalog.persistence.GenreRepository;

@ExtendWith(MockitoExtension.class)
class GenreBrowseServiceTest {

    @Mock
    private GenreRepository genreRepository;

    @Mock
    private TmdbClient tmdbClient;

    @Mock
    private WatchMateMapper watchMateMapper;

    @InjectMocks
    private GenreBrowseService genreBrowseService;

    @Test
    void browseMovies_ResolvesGenreAndMapsPagedResults() {
        Genre genre = Genre.builder().tmdbGenreId(28L).name("Action").mediaType(MediaType.MOVIE).build();
        TmdbMovieDTO tmdbMovie = TmdbMovieDTO.builder().id(10L).title("Action Hit").build();
        DiscoveryMediaItemDTO dto = DiscoveryMediaItemDTO.builder().tmdbId(10L).title("Action Hit").type(MediaType.MOVIE).build();

        when(genreRepository.findCurrentByNameIgnoreCaseAndMediaType("Action", MediaType.MOVIE)).thenReturn(Optional.of(genre));
        when(tmdbClient.discoverByGenre("movie", 28L, 1)).thenReturn(new TmdbResponseDTO(List.of(tmdbMovie), 1, 4, 80));
        when(watchMateMapper.mapToDiscoveryMediaItemDTO(eq(tmdbMovie), eq(MediaType.MOVIE))).thenReturn(dto);

        GenreBrowseResponseDTO result = genreBrowseService.browseMovies("Action", 1, 20);

        assertEquals("Action", result.getGenre());
        assertEquals(1, result.getResults().size());
        assertEquals(80, result.getTotalResults());
    }

    @Test
    void browseShows_WhenGenreMissing_ThrowsNotFound() {
        when(genreRepository.findCurrentByNameIgnoreCaseAndMediaType("Horror", MediaType.SHOW)).thenReturn(Optional.empty());

        GenreNotFoundException exception = assertThrows(
            GenreNotFoundException.class,
            () -> genreBrowseService.browseShows("Horror", 1, 20)
        );

        assertEquals("Genre not found: Horror", exception.getMessage());
    }
}





