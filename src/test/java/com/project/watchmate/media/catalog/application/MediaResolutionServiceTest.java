package com.project.watchmate.media.catalog.application;

import com.project.watchmate.media.tmdb.application.TmdbService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

import java.util.Optional;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.project.watchmate.common.cache.WatchMateCacheEvictionService;
import com.project.watchmate.common.error.MediaNotFoundException;
import com.project.watchmate.media.catalog.domain.Media;
import com.project.watchmate.media.catalog.domain.MediaType;
import com.project.watchmate.media.catalog.persistence.MediaRepository;

@ExtendWith(MockitoExtension.class)
class MediaResolutionServiceTest {

    private static final Long TMDB_ID = 1399L;

    @Mock
    private MediaRepository mediaRepository;

    @Mock
    private TmdbService tmdbService;

    @Mock
    private WatchMateCacheEvictionService cacheEvictionService;

    @Mock
    private PlatformTransactionManager transactionManager;

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
        lenient().when(transactionManager.getTransaction(any(TransactionDefinition.class)))
            .thenReturn(new SimpleTransactionStatus());
    }

    @Nested
    @DisplayName("Resolve Media By TMDB ID Tests")
    class ResolveMediaByTmdbIdTests {

        @Test
        void resolveMediaByTmdbId_WhenMediaExists_ReturnsStoredMedia() {
            when(mediaRepository.findByTmdbIdAndType(TMDB_ID, MediaType.SHOW)).thenReturn(Optional.of(media));

            Media result = mediaResolutionService.resolveMediaByTmdbId(TMDB_ID, "SHOW");

            assertNotNull(result);
            assertEquals(media, result);
            verify(tmdbService, never()).fetchMediaByTmdbId(any(), any());
        }

        @Test
        void resolveMediaByTmdbId_WhenMediaMissing_ImportsAndSavesMedia() {
            when(mediaRepository.findByTmdbIdAndType(TMDB_ID, MediaType.SHOW)).thenReturn(Optional.empty());
            when(tmdbService.fetchMediaByTmdbId(TMDB_ID, MediaType.SHOW)).thenReturn(media);
            when(mediaRepository.saveAndFlush(media)).thenReturn(media);

            Media result = mediaResolutionService.resolveMediaByTmdbId(TMDB_ID, "SHOW");

            assertNotNull(result);
            assertEquals(media, result);
            verify(tmdbService).fetchMediaByTmdbId(TMDB_ID, MediaType.SHOW);
            verify(mediaRepository).saveAndFlush(media);
        }

        @Test
        void resolveMediaByTmdbId_WhenSaveHitsDuplicateMediaKey_RequeriesAndReturnsExistingMedia() {
            Media imported = Media.builder()
                .tmdbId(TMDB_ID)
                .title("Imported Show")
                .type(MediaType.SHOW)
                .build();
            DataIntegrityViolationException duplicate = new DataIntegrityViolationException(
                "Duplicate entry '1399-SHOW' for key 'media.uq_media_tmdb_id_type'"
            );
            when(mediaRepository.findByTmdbIdAndType(TMDB_ID, MediaType.SHOW))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(media));
            when(tmdbService.fetchMediaByTmdbId(TMDB_ID, MediaType.SHOW)).thenReturn(imported);
            when(mediaRepository.saveAndFlush(imported)).thenThrow(duplicate);

            Media result = mediaResolutionService.resolveMediaByTmdbId(TMDB_ID, "SHOW");

            assertEquals(media, result);
            verify(mediaRepository).saveAndFlush(imported);
        }

        @Test
        void resolveMediaByTmdbId_WhenSaveHitsDifferentIntegrityViolation_PropagatesFailure() {
            DataIntegrityViolationException exception = new DataIntegrityViolationException("Foreign key failed");
            when(mediaRepository.findByTmdbIdAndType(TMDB_ID, MediaType.SHOW)).thenReturn(Optional.empty());
            when(tmdbService.fetchMediaByTmdbId(TMDB_ID, MediaType.SHOW)).thenReturn(media);
            when(mediaRepository.saveAndFlush(media)).thenThrow(exception);

            DataIntegrityViolationException result = assertThrows(
                DataIntegrityViolationException.class,
                () -> mediaResolutionService.resolveMediaByTmdbId(TMDB_ID, "SHOW")
            );

            assertEquals(exception, result);
            verify(mediaRepository, never()).findById(anyLong());
        }

        @Test
        void resolveMediaByTmdbId_WhenTypeMissingAndMediaMissing_ThrowsBadRequest() {
            when(mediaRepository.findAllByTmdbId(TMDB_ID)).thenReturn(List.of());

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
            when(mediaRepository.findByTmdbIdAndType(TMDB_ID, MediaType.SHOW)).thenReturn(Optional.empty());
            when(tmdbService.fetchMediaByTmdbId(TMDB_ID, MediaType.SHOW)).thenReturn(null);

            MediaNotFoundException exception = assertThrows(
                MediaNotFoundException.class,
                () -> mediaResolutionService.resolveMediaByTmdbId(TMDB_ID, "SHOW")
            );

            assertEquals("TMDB media not found for ID: " + TMDB_ID, exception.getMessage());
        }

        @Test
        void resolveMediaByTmdbId_WhenTypeMissingAndMultipleMatches_ThrowsBadRequest() {
            Media movie = Media.builder().id(11L).tmdbId(TMDB_ID).type(MediaType.MOVIE).build();
            when(mediaRepository.findAllByTmdbId(TMDB_ID)).thenReturn(List.of(media, movie));

            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> mediaResolutionService.resolveMediaByTmdbId(TMDB_ID, (String) null)
            );

            assertEquals("Multiple media items share this TMDB ID. Please supply the media type.", exception.getMessage());
        }
    }
}




