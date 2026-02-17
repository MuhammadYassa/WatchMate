package com.project.watchmate.Services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.project.watchmate.Dto.MediaDetailsDTO;
import com.project.watchmate.Exception.MediaNotFoundException;
import com.project.watchmate.Mappers.WatchMateMapper;
import com.project.watchmate.Models.Media;
import com.project.watchmate.Models.MediaType;
import com.project.watchmate.Models.Users;
import com.project.watchmate.Models.WatchStatus;
import com.project.watchmate.Repositories.MediaRepository;
import com.project.watchmate.Repositories.ReviewRepository;
import com.project.watchmate.Repositories.UserMediaStatusRepository;
import com.project.watchmate.Repositories.UsersRepository;

@ExtendWith(MockitoExtension.class)
class MediaServiceTest {

    @Mock
    private MediaRepository mediaRepository;

    @Mock
    private UsersRepository usersRepository;

    @Mock
    private TmdbService tmdbService;

    @Mock
    private WatchMateMapper watchMateMapper;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private UserMediaStatusRepository userMediaStatusRepository;

    @InjectMocks
    private MediaService mediaService;

    private static final Long TMDB_ID = 100L;
    private Users user;
    private Media media;
    private MediaDetailsDTO expectedDto;

    @BeforeEach
    void setUp() {
        user = Users.builder().id(1L).username("user").favorites(new ArrayList<>()).build();
        media = Media.builder().id(1L).tmdbId(TMDB_ID).title("Test Media").reviews(new ArrayList<>()).build();
        expectedDto = MediaDetailsDTO.builder().tmdbId(TMDB_ID).type(MediaType.MOVIE).watchStatus(WatchStatus.NONE).reviews(List.of()).title("Test Media").build();
    }

    @Nested
    @DisplayName("getMediaDetails")
    class GetMediaDetailsTests {

        @Test
        void getMediaDetails_WhenMediaInDb_ReturnsMappedDtoAndSavesMedia() {
            when(usersRepository.findById(1L)).thenReturn(Optional.of(user));
            when(mediaRepository.findByTmdbId(TMDB_ID)).thenReturn(Optional.of(media));
            when(reviewRepository.findByMedia(media)).thenReturn(List.of());
            when(userMediaStatusRepository.findByUserAndMedia(user, media)).thenReturn(Optional.empty());
            when(watchMateMapper.mapToMediaDetailsDTO(media, List.of(), false, WatchStatus.NONE)).thenReturn(expectedDto);

            MediaDetailsDTO result = mediaService.getMediaDetails(TMDB_ID, MediaType.MOVIE, user);

            assertNotNull(result);
            assertEquals(TMDB_ID, result.getTmdbId());
            assertEquals("Test Media", result.getTitle());
            assertEquals(MediaType.MOVIE, result.getType());
            assertEquals(WatchStatus.NONE, result.getWatchStatus());
            assertEquals(List.of(), result.getReviews());
            assertEquals(false, result.isFavourited());
            verify(mediaRepository).save(media);
            verify(watchMateMapper).mapToMediaDetailsDTO(media, List.of(), false, WatchStatus.NONE);
        }

        @Test
        void getMediaDetails_WhenMediaNotInDb_FetchesFromTmdbThenSavesAndReturnsDto() {
            when(usersRepository.findById(1L)).thenReturn(Optional.of(user));
            when(mediaRepository.findByTmdbId(TMDB_ID)).thenReturn(Optional.empty());
            when(tmdbService.fetchMediaByTmdbId(TMDB_ID, MediaType.MOVIE)).thenReturn(media);
            when(mediaRepository.save(any(Media.class))).thenReturn(media);
            when(reviewRepository.findByMedia(media)).thenReturn(List.of());
            when(userMediaStatusRepository.findByUserAndMedia(user, media)).thenReturn(Optional.empty());
            when(watchMateMapper.mapToMediaDetailsDTO(any(), any(), eq(false), eq(WatchStatus.NONE))).thenReturn(expectedDto);

            MediaDetailsDTO result = mediaService.getMediaDetails(TMDB_ID, MediaType.MOVIE, user);

            assertNotNull(result);
            assertEquals(TMDB_ID, result.getTmdbId());
            assertEquals("Test Media", result.getTitle());
            assertEquals(MediaType.MOVIE, result.getType());
            assertEquals(WatchStatus.NONE, result.getWatchStatus());
            assertEquals(List.of(), result.getReviews());
            assertEquals(false, result.isFavourited());
            verify(tmdbService).fetchMediaByTmdbId(TMDB_ID, MediaType.MOVIE);
            verify(mediaRepository).save(media);
        }

        @Test
        void getMediaDetails_WhenUserNotFound_ThrowsRuntimeException() {
            when(usersRepository.findById(1L)).thenReturn(Optional.empty());

            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> mediaService.getMediaDetails(TMDB_ID, MediaType.MOVIE, user));
            assertEquals("User not found", exception.getMessage());
            verify(mediaRepository, never()).save(any(Media.class));
        }

        @Test
        void getMediaDetails_WhenMediaNotInDbAndTmdbFails_ThrowsMediaNotFoundException() {
            when(usersRepository.findById(1L)).thenReturn(Optional.of(user));
            when(mediaRepository.findByTmdbId(TMDB_ID)).thenReturn(Optional.empty());
            when(tmdbService.fetchMediaByTmdbId(TMDB_ID, MediaType.MOVIE))
                .thenThrow(new MediaNotFoundException("TMDB media not found for ID: " + TMDB_ID));

            MediaNotFoundException exception = assertThrows(MediaNotFoundException.class,
                () -> mediaService.getMediaDetails(TMDB_ID, MediaType.MOVIE, user));
            assertEquals("TMDB media not found for ID: " + TMDB_ID, exception.getMessage());
        }
    }

    @Nested
    @DisplayName("getMoviesWatchedPage / getShowsWatchedPage")
    class GetWatchedPagesTests {

        @Test
        void getMoviesWatchedPage_DelegatesToRepository() {
            Page<Media> page = new PageImpl<>(List.of(media));
            when(userMediaStatusRepository.findWatchedMoviesByUser(eq(user), any(Pageable.class)))
                .thenReturn(page);

            Page<Media> result = mediaService.getMoviesWatchedPage(user);
            
            assertNotNull(result);
            assertEquals(1, result.getContent().size());
            assertEquals(media, result.getContent().get(0));
            verify(userMediaStatusRepository).findWatchedMoviesByUser(eq(user), any(Pageable.class));
        }

        @Test
        void getShowsWatchedPage_DelegatesToRepository() {
            Page<Media> page = new PageImpl<>(List.of(media));
            when(userMediaStatusRepository.findWatchedShowsByUser(eq(user), any(Pageable.class)))
                .thenReturn(page);

            Page<Media> result = mediaService.getShowsWatchedPage(user);

            assertNotNull(result);
            assertEquals(1, result.getContent().size());
            assertEquals(media, result.getContent().get(0));
            verify(userMediaStatusRepository).findWatchedShowsByUser(eq(user), any(Pageable.class));
        }
    }
}
