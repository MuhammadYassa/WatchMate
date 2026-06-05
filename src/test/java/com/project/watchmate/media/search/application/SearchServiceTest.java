package com.project.watchmate.media.search.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.watchmate.media.tmdb.client.TmdbClient;
import com.project.watchmate.media.search.dto.PaginatedSearchResponseDTO;
import com.project.watchmate.media.search.dto.SearchItemDTO;
import com.project.watchmate.media.tmdb.dto.TmdbMovieDTO;
import com.project.watchmate.media.tmdb.dto.TmdbResponseDTO;
import com.project.watchmate.media.catalog.domain.Genre;
import com.project.watchmate.media.catalog.domain.MediaType;
import com.project.watchmate.media.catalog.persistence.GenreRepository;

@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private GenreRepository genreRepository;

    @Mock
    private TmdbClient tmdbClient;

    @InjectMocks
    private SearchService searchService;

    @Nested
    @DisplayName("Search Tests")
    class SearchTests {

        @Test
        void search_WhenMovieResultReturned_MapsMovieTitleAndReleaseDate() {
            TmdbMovieDTO movieResult = tmdbMovie("""
                {
                  "id": 1,
                  "media_type": "movie",
                  "title": "Batman Begins",
                  "overview": "Bruce Wayne becomes Batman.",
                  "poster_path": "/batman-begins.jpg",
                  "release_date": "2005-06-15",
                  "vote_average": 8.2,
                  "genre_ids": [28, 80]
                }
                """);

            when(tmdbClient.searchMulti("batman", 1))
                .thenReturn(new TmdbResponseDTO(List.of(movieResult), 1, 3, 40));
            when(genreRepository.findByTmdbGenreIdInAndMediaType(List.of(28L, 80L), MediaType.MOVIE))
                .thenReturn(List.of(
                    Genre.builder().tmdbGenreId(28L).name("Action").mediaType(MediaType.MOVIE).build(),
                    Genre.builder().tmdbGenreId(80L).name("Crime").mediaType(MediaType.MOVIE).build()
                ));

            PaginatedSearchResponseDTO result = searchService.search("batman", 1);

            assertNotNull(result);
            assertEquals(1, result.getCurrentPage());
            assertEquals(3, result.getTotalPages());
            assertEquals(40, result.getTotalResults());

            SearchItemDTO item = result.getSearchResults().get(0);
            assertEquals(1L, item.getId());
            assertEquals("Batman Begins", item.getTitle());
            assertEquals("2005-06-15", item.getReleaseDate());
            assertEquals("movie", item.getMediaType());
            assertEquals("/batman-begins.jpg", item.getPosterPath());
            assertEquals(8.2, item.getVoteAverage());
            assertEquals(List.of("Action", "Crime"), item.getGenres());
        }

        @Test
        void search_WhenTvResultReturned_MapsNameAndFirstAirDate() {
            TmdbMovieDTO tvResult = tmdbMovie("""
                {
                  "id": 2,
                  "media_type": "tv",
                  "name": "Batman: The Animated Series",
                  "overview": "Batman protects Gotham City.",
                  "poster_path": "/batman-animated.jpg",
                  "first_air_date": "1992-09-05",
                  "vote_average": 8.5,
                  "genre_ids": [16]
                }
                """);

            when(tmdbClient.searchMulti("batman", 1))
                .thenReturn(new TmdbResponseDTO(List.of(tvResult), 1, 2, 12));
            when(genreRepository.findByTmdbGenreIdInAndMediaType(List.of(16L), MediaType.SHOW))
                .thenReturn(List.of(Genre.builder().tmdbGenreId(16L).name("Animation").mediaType(MediaType.SHOW).build()));

            PaginatedSearchResponseDTO result = searchService.search("batman", 1);

            SearchItemDTO item = result.getSearchResults().get(0);
            assertEquals("Batman: The Animated Series", item.getTitle());
            assertEquals("1992-09-05", item.getReleaseDate());
            assertEquals("tv", item.getMediaType());
        }

        @Test
        void search_WhenPersonResultReturned_ExcludesIt() {
            TmdbMovieDTO personResult = tmdbMovie("""
                {
                  "id": 3,
                  "media_type": "person",
                  "name": "Christian Bale",
                  "profile_path": "/christian-bale.jpg"
                }
                """);

            when(tmdbClient.searchMulti("batman", 1))
                .thenReturn(new TmdbResponseDTO(List.of(personResult), 1, 1, 1));

            PaginatedSearchResponseDTO result = searchService.search("batman", 1);

            assertEquals(List.of(), result.getSearchResults());
            verifyNoInteractions(genreRepository);
        }

        @Test
        void search_WhenPosterPathMissing_ReturnsNullPosterPath() {
            TmdbMovieDTO movieResult = tmdbMovie("""
                {
                  "id": 4,
                  "media_type": "movie",
                  "title": "Batman: Gotham Knight",
                  "overview": "Animated anthology.",
                  "release_date": "2008-07-08",
                  "vote_average": 6.7
                }
            """);

            when(tmdbClient.searchMulti("batman", 1))
                .thenReturn(new TmdbResponseDTO(List.of(movieResult), 1, 1, 1));

            PaginatedSearchResponseDTO result = searchService.search("batman", 1);

            SearchItemDTO item = result.getSearchResults().get(0);
            assertNull(item.getPosterPath());
            assertEquals("Batman: Gotham Knight", item.getTitle());
        }

        @Test
        void search_WhenResultHasNoUsableTitle_FiltersItOut() {
            TmdbMovieDTO blankTitleResult = tmdbMovie("""
                {
                  "id": 5,
                  "media_type": "movie",
                  "overview": "No title provided.",
                  "release_date": "2024-01-01"
                }
                """);

            when(tmdbClient.searchMulti("batman", 1))
                .thenReturn(new TmdbResponseDTO(List.of(blankTitleResult), 1, 1, 1));

            PaginatedSearchResponseDTO result = searchService.search("batman", 1);

            assertEquals(List.of(), result.getSearchResults());
            verifyNoInteractions(genreRepository);
        }

        @Test
        void search_WhenEmptyOrNoResults_ReturnsEmptyPaginatedDto() {
            TmdbResponseDTO emptyResponse = new TmdbResponseDTO(List.of(), 1, 0, 0);

            when(tmdbClient.searchMulti("xyznonexistent", 1))
                .thenReturn(emptyResponse);

            PaginatedSearchResponseDTO result = searchService.search("xyznonexistent", 1);

            assertNotNull(result);
            assertEquals(0, result.getSearchResults().size());
            assertEquals(0, result.getTotalResults());
        }

        @Test
        void search_WhenClientReturnsEmptyOptional_ReturnsEmptyPaginatedDto() {
            when(tmdbClient.searchMulti("query", 1))
                .thenReturn(null);

            PaginatedSearchResponseDTO result = searchService.search("query", 1);

            assertNotNull(result);
            assertEquals(List.of(), result.getSearchResults());
            assertEquals(0, result.getTotalResults());
        }

        @Test
        void search_WhenMovieAndShowShareGenreIds_UsesTypedLookupForEachResult() {
            TmdbMovieDTO movieResult = tmdbMovie("""
                {
                  "id": 6,
                  "media_type": "movie",
                  "title": "Shared Genre Movie",
                  "overview": "Movie overview.",
                  "genre_ids": [18]
                }
                """);
            TmdbMovieDTO showResult = tmdbMovie("""
                {
                  "id": 7,
                  "media_type": "tv",
                  "name": "Shared Genre Show",
                  "overview": "Show overview.",
                  "genre_ids": [18]
                }
                """);

            when(tmdbClient.searchMulti("shared", 1))
                .thenReturn(new TmdbResponseDTO(List.of(movieResult, showResult), 1, 1, 2));
            when(genreRepository.findByTmdbGenreIdInAndMediaType(List.of(18L), MediaType.MOVIE))
                .thenReturn(List.of(Genre.builder().tmdbGenreId(18L).name("Drama Movie").mediaType(MediaType.MOVIE).build()));
            when(genreRepository.findByTmdbGenreIdInAndMediaType(List.of(18L), MediaType.SHOW))
                .thenReturn(List.of(Genre.builder().tmdbGenreId(18L).name("Drama Show").mediaType(MediaType.SHOW).build()));

            PaginatedSearchResponseDTO result = searchService.search("shared", 1);

            assertEquals(List.of("Drama Movie"), result.getSearchResults().get(0).getGenres());
            assertEquals(List.of("Drama Show"), result.getSearchResults().get(1).getGenres());
        }

        private TmdbMovieDTO tmdbMovie(String json) {
            try {
                return objectMapper.readValue(json, TmdbMovieDTO.class);
            } catch (JsonProcessingException ex) {
                throw new IllegalArgumentException("Invalid TMDB test payload", ex);
            }
        }
    }
}



