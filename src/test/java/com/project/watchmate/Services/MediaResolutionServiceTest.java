package com.project.watchmate.Services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.project.watchmate.Exception.MediaNotFoundException;
import com.project.watchmate.Models.Media;
import com.project.watchmate.Models.MediaType;
import com.project.watchmate.Repositories.MediaRepository;

@ExtendWith(MockitoExtension.class)
class MediaResolutionServiceTest {

    private static final Long TMDB_ID = 1399L;

    @Mock
    private MediaRepository mediaRepository;

    @Mock
    private TmdbService tmdbService;

    @InjectMocks
    private MediaResolutionService mediaResolutionService;

    private Media media;

    @BeforeEach
    void setUp() {
        media = Media.builder()
            .id(10L)
            .tmdbId(TMDB_ID)
            .title("Resolved Show")
            .type(MediaType.SHOW)
            .build();
    }

    @Nested
    @DisplayName("Resolve Media By TMDB ID Tests")
    class ResolveMediaByTmdbIdTests {

        @Test
        void resolveMediaByTmdbId_WhenMediaExists_ReturnsStoredMedia() {
            when(mediaRepository.findByTmdbId(TMDB_ID)).thenReturn(Optional.of(media));

            Media result = mediaResolutionService.resolveMediaByTmdbId(TMDB_ID, "SHOW");

            assertNotNull(result);
            assertEquals(media, result);
            verify(tmdbService, never()).fetchMediaByTmdbId(any(), any());
        }

        @Test
        void resolveMediaByTmdbId_WhenMediaMissing_ImportsAndSavesMedia() {
            when(mediaRepository.findByTmdbId(TMDB_ID)).thenReturn(Optional.empty());
            when(tmdbService.fetchMediaByTmdbId(TMDB_ID, MediaType.SHOW)).thenReturn(media);
            when(mediaRepository.save(media)).thenReturn(media);

            Media result = mediaResolutionService.resolveMediaByTmdbId(TMDB_ID, "SHOW");

            assertNotNull(result);
            assertEquals(media, result);
            verify(tmdbService).fetchMediaByTmdbId(TMDB_ID, MediaType.SHOW);
            verify(mediaRepository).save(media);
        }

        @Test
        void resolveMediaByTmdbId_WhenTypeMissingAndMediaMissing_ThrowsBadRequest() {
            when(mediaRepository.findByTmdbId(TMDB_ID)).thenReturn(Optional.empty());

            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> mediaResolutionService.resolveMediaByTmdbId(TMDB_ID, (String) null)
            );

            assertEquals("Media type is required when the media item has not been imported yet.", exception.getMessage());
            verify(tmdbService, never()).fetchMediaByTmdbId(any(), any());
        }

        @Test
        void resolveMediaByTmdbId_WhenTypeInvalid_ThrowsBadRequest() {
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> mediaResolutionService.resolveMediaByTmdbId(TMDB_ID, "DOCUMENTARY")
            );

            assertEquals("Invalid media type. Allowed values: MOVIE, SHOW", exception.getMessage());
        }

        @Test
        void resolveMediaByTmdbId_WhenTmdbServiceReturnsNull_ThrowsMediaNotFound() {
            when(mediaRepository.findByTmdbId(TMDB_ID)).thenReturn(Optional.empty());
            when(tmdbService.fetchMediaByTmdbId(TMDB_ID, MediaType.SHOW)).thenReturn(null);

            MediaNotFoundException exception = assertThrows(
                MediaNotFoundException.class,
                () -> mediaResolutionService.resolveMediaByTmdbId(TMDB_ID, "SHOW")
            );

            assertEquals("TMDB media not found for ID: " + TMDB_ID, exception.getMessage());
        }
    }
}
