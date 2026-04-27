package com.project.watchmate.Services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import com.project.watchmate.Mappers.WatchMateMapper;
import com.project.watchmate.Models.Media;
import com.project.watchmate.Models.MediaType;
import com.project.watchmate.Models.Users;
import com.project.watchmate.Models.WatchStatus;
import com.project.watchmate.Repositories.ReviewRepository;
import com.project.watchmate.Repositories.UserMediaStatusRepository;
import com.project.watchmate.Repositories.UsersRepository;

@ExtendWith(MockitoExtension.class)
class MediaServiceTest {

    @Mock
    private MediaResolutionService mediaResolutionService;

    @Mock
    private UsersRepository usersRepository;

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
    @DisplayName("Get Media Details Tests")
    class GetMediaDetailsTests {

        @Test
        void getMediaDetails_WhenMediaResolves_ReturnsMappedDto() {
            when(usersRepository.findByIdWithFavorites(1L)).thenReturn(Optional.of(user));
            when(mediaResolutionService.resolveMediaByTmdbId(TMDB_ID, "movie")).thenReturn(media);
            when(reviewRepository.findByMedia(media)).thenReturn(List.of());
            when(userMediaStatusRepository.findByUserAndMedia(user, media)).thenReturn(Optional.empty());
            when(watchMateMapper.mapToMediaDetailsDTO(media, List.of(), false, WatchStatus.NONE)).thenReturn(expectedDto);

            MediaDetailsDTO result = mediaService.getMediaDetails(TMDB_ID, "movie", user);

            assertNotNull(result);
            assertEquals(TMDB_ID, result.getTmdbId());
            assertEquals("Test Media", result.getTitle());
            assertEquals(MediaType.MOVIE, result.getType());
            assertEquals(WatchStatus.NONE, result.getWatchStatus());
            assertEquals(List.of(), result.getReviews());
            assertEquals(false, result.isFavourited());
            verify(watchMateMapper).mapToMediaDetailsDTO(media, List.of(), false, WatchStatus.NONE);
        }

        @Test
        void getMediaDetails_WhenUserNotFound_ThrowsRuntimeException() {
            when(usersRepository.findByIdWithFavorites(1L)).thenReturn(Optional.empty());

            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> mediaService.getMediaDetails(TMDB_ID, "movie", user));
            assertEquals("User not found", exception.getMessage());
        }

    }

    @Nested
    @DisplayName("Get Watched Pages Tests")
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
