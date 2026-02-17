package com.project.watchmate.Services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.project.watchmate.Clients.TmdbClient;
import com.project.watchmate.Dto.PaginatedSearchResponseDTO;
import com.project.watchmate.Dto.SearchItemDTO;
import com.project.watchmate.Dto.TmdbMovieDTO;
import com.project.watchmate.Dto.TmdbResponseDTO;
import com.project.watchmate.Models.Genre;
import com.project.watchmate.Repositories.GenreRepository;

@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    @Mock
    private GenreRepository genreRepository;

    @Mock
    private TmdbClient tmdbClient;

    @InjectMocks
    private SearchService searchService;

    @Nested
    @DisplayName("search")
    class SearchTests {

        @Test
        void search_WhenResultsReturned_ReturnsPaginatedDtoWithMappedItems() {
            TmdbMovieDTO resultItem = TmdbMovieDTO.builder()
            .id(1L)
            .title("Inception")
            .overview("A thief who steals corporate secrets...")
            .mediaType("movie")
            .posterPath("/path")
            .voteAverage(8.5)
            .genreIds(List.of(28L, 878L))
            .build();

            TmdbResponseDTO response = new TmdbResponseDTO(
                List.of(resultItem),
                1,
                1,
                1
            );

            when(tmdbClient.searchMulti("Inception", 1))
                .thenReturn(response);

            when(genreRepository.findAllById(any()))
                .thenReturn(List.of(
                    Genre.builder().id(28L).name("Action").build(),
                    Genre.builder().id(878L).name("Sci-Fi").build()
                ));


            PaginatedSearchResponseDTO result =
                searchService.search("Inception", 1);


            assertNotNull(result);
            assertEquals(1, result.getTotalPages());
            assertEquals(1, result.getTotalResults());

            SearchItemDTO item = result.getSearchResults().get(0);
            assertEquals(1L, item.getId());
            assertEquals("Inception", item.getTitle());
            assertEquals(List.of("Action", "Sci-Fi"), item.getGenres());
        }

        @Test
        void search_WhenEmptyOrNoResults_ReturnsEmptyPaginatedDto() {
            TmdbResponseDTO emptyResponse =
                new TmdbResponseDTO(List.of(), 1, 0, 0);

            when(tmdbClient.searchMulti("xyznonexistent", 1))
                .thenReturn(emptyResponse);

            PaginatedSearchResponseDTO result =
                searchService.search("xyznonexistent", 1);

            assertNotNull(result);
            assertEquals(0, result.getSearchResults().size());
            assertEquals(0, result.getTotalResults());
        }

        @Test
        void search_WhenClientReturnsEmptyOptional_ReturnsEmptyPaginatedDto() {
            when(tmdbClient.searchMulti("query", 1))
                .thenReturn(null);

            PaginatedSearchResponseDTO result =
                searchService.search("query", 1);

            assertNotNull(result);
            assertEquals(List.of(), result.getSearchResults());
            assertEquals(0, result.getTotalResults());

        }
    }
}
