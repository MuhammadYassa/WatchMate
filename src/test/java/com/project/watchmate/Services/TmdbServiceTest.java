package com.project.watchmate.Services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.project.watchmate.Clients.TmdbClient;
import com.project.watchmate.Dto.TmdbMovieDTO;
import com.project.watchmate.Exception.MediaNotFoundException;
import com.project.watchmate.Models.Media;
import com.project.watchmate.Models.MediaType;
import com.project.watchmate.Repositories.GenreRepository;
import com.project.watchmate.Repositories.MediaRepository;

@ExtendWith(MockitoExtension.class)
class TmdbServiceTest {

    @Mock
    private TmdbClient tmdbClient;

    @Mock
    private MediaRepository mediaRepository;

    @Mock
    private GenreRepository genreRepository;

    @InjectMocks
    private TmdbService tmdbService;

    private TmdbMovieDTO tmdbMovieDto;
    private static final Long TMDB_ID = 100L;

    @BeforeEach
    void setUp() {
        tmdbMovieDto = TmdbMovieDTO.builder()
            .id(TMDB_ID)
            .title("Test Movie")
            .overview("Overview")
            .posterPath("/path")
            .voteAverage(8.0)
            .genres(List.of())
            .build();
    }

    @Nested
    @DisplayName("Fetch Media By TMDB ID Tests")
    class FetchMediaByTmdbIdTests {

        @Test
        void fetchMediaByTmdbId_WhenSuccess_ReturnsMappedMedia() {
            when(tmdbClient.fetchMediaById(TMDB_ID, MediaType.MOVIE))
            .thenReturn(tmdbMovieDto);
            when(genreRepository.findAllById(any())).thenReturn(List.of());

            Media result = tmdbService.fetchMediaByTmdbId(TMDB_ID, MediaType.MOVIE);

            assertNotNull(result);
            assertEquals(TMDB_ID, result.getTmdbId());
            assertEquals("Test Movie", result.getTitle());
            assertEquals(MediaType.MOVIE, result.getType());
        }

        @Test
        void fetchMediaByTmdbId_WhenTmdbReturnsNull_ThrowsMediaNotFoundException() {
            when(tmdbClient.fetchMediaById(TMDB_ID, MediaType.MOVIE)).thenReturn(null);

            MediaNotFoundException exception = assertThrows(MediaNotFoundException.class,
                () -> tmdbService.fetchMediaByTmdbId(TMDB_ID, MediaType.MOVIE));

            assertEquals("TMDB media not found for ID: " + TMDB_ID, exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Save and Update Media Tests")
    class SaveAndUpdateMediaTests {

        @Test
        void saveAndUpdateMedia_WhenNewMedia_SavesEachAndReturnsList() {
            Media newMedia = Media.builder().tmdbId(200L).title("New").type(MediaType.MOVIE).build();
            when(mediaRepository.findByTmdbIdAndType(200L, MediaType.MOVIE)).thenReturn(Optional.empty());
            when(mediaRepository.save(any(Media.class)))
            .thenAnswer(inv -> inv.getArgument(0));

            List<Media> result = tmdbService.saveAndUpdateMedia(List.of(newMedia));

            assertEquals(1, result.size());
            assertEquals(200L, result.get(0).getTmdbId());
            verify(mediaRepository).save(newMedia);
        }

        @Test
        void saveAndUpdateMedia_WhenMediaExists_UpdatesAndSavesExisting() {
            Media inputMedia = Media.builder().tmdbId(TMDB_ID).title("Updated").overview("New overview").type(MediaType.MOVIE).build();
            Media existingMedia = Media.builder().id(1L).tmdbId(TMDB_ID).title("Old").build();
            when(mediaRepository.findByTmdbIdAndType(TMDB_ID, MediaType.MOVIE)).thenReturn(Optional.of(existingMedia));
            when(mediaRepository.save(any(Media.class)))
            .thenAnswer(inv -> inv.getArgument(0));

            List<Media> result = tmdbService.saveAndUpdateMedia(List.of(inputMedia));

            assertEquals(1, result.size());
            assertEquals("Updated", existingMedia.getTitle());
            assertEquals("New overview", existingMedia.getOverview());
            verify(mediaRepository).save(existingMedia);
        }
    }
}
