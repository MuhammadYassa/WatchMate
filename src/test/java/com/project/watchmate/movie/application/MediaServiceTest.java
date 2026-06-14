package com.project.watchmate.movie.application;

import com.project.watchmate.media.catalog.application.MediaResolutionService;
import com.project.watchmate.media.catalog.application.UserWatchStatusResolver;

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

import com.project.watchmate.movie.dto.MovieDetailsDTO;
import com.project.watchmate.movie.dto.PublicMovieDetailBaseDTO;
import com.project.watchmate.common.error.UserNotFoundException;
import com.project.watchmate.common.mapper.WatchMateMapper;
import com.project.watchmate.media.catalog.domain.Media;
import com.project.watchmate.media.catalog.domain.MediaType;
import com.project.watchmate.user.domain.Users;
import com.project.watchmate.media.catalog.domain.WatchStatus;
import com.project.watchmate.media.catalog.persistence.MediaRepository;
import com.project.watchmate.review.persistence.ReviewRepository;
import com.project.watchmate.movie.tracking.persistence.UserMediaStatusRepository;
import com.project.watchmate.show.tracking.persistence.UserShowTrackingRepository;
import com.project.watchmate.user.persistence.UsersRepository;

@ExtendWith(MockitoExtension.class)
class MediaServiceTest {

    @Mock
    private MediaResolutionService mediaResolutionService;

    @Mock
    private MediaRepository mediaRepository;

    @Mock
    private UsersRepository usersRepository;

    @Mock
    private WatchMateMapper watchMateMapper;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private UserMediaStatusRepository userMediaStatusRepository;

    @Mock
    private UserShowTrackingRepository userShowTrackingRepository;

    @Mock
    private UserWatchStatusResolver userWatchStatusResolver;

    @Mock
    private PublicMediaDetailBaseCacheService publicMediaDetailBaseCacheService;

    @InjectMocks
    private MediaService mediaService;

    private static final Long TMDB_ID = 100L;
    private Users user;
    private Media media;

    @BeforeEach
    void setUp() {
        user = Users.builder().id(1L).username("user").favorites(new ArrayList<>()).build();
        media = Media.builder().id(1L).tmdbId(TMDB_ID).title("Test Movie").type(MediaType.MOVIE).reviews(new ArrayList<>()).build();
    }

    @Nested
    @DisplayName("Get Movie Details Tests")
    class GetMovieDetailsTests {

        @Test
        void getMovieDetails_WhenAuthenticatedMovieResolves_ReturnsMappedDto() {
            when(usersRepository.findByIdWithFavorites(1L)).thenReturn(Optional.of(user));
            when(mediaResolutionService.resolveMediaByTmdbId(TMDB_ID, MediaType.MOVIE)).thenReturn(media);
            when(publicMediaDetailBaseCacheService.getMovieBase(TMDB_ID, MediaType.MOVIE)).thenReturn(publicBase("Test Movie"));
            when(reviewRepository.findByMedia(media)).thenReturn(List.of());
            when(userWatchStatusResolver.resolveWatchStatus(user, media)).thenReturn(WatchStatus.NONE);

            MovieDetailsDTO result = mediaService.getMovieDetails(TMDB_ID, user);

            assertNotNull(result);
            assertEquals(TMDB_ID, result.getTmdbId());
            assertEquals("Test Movie", result.getTitle());
            assertEquals(MediaType.MOVIE, result.getType());
            assertEquals(WatchStatus.NONE, result.getWatchStatus());
            assertEquals(List.of(), result.getReviews());
            assertEquals(Boolean.FALSE, result.getIsFavourited());
        }

        @Test
        void getMovieDetails_WhenMovieIsPublic_ReturnsDtoWithNullUserFields() {
            Media publicMovie = Media.builder().tmdbId(TMDB_ID).title("Public Movie").type(MediaType.MOVIE).build();

            when(mediaRepository.findByTmdbIdAndType(TMDB_ID, MediaType.MOVIE)).thenReturn(Optional.of(publicMovie));
            when(publicMediaDetailBaseCacheService.getMovieBase(TMDB_ID, MediaType.MOVIE)).thenReturn(publicBase("Public Movie"));

            MovieDetailsDTO result = mediaService.getMovieDetails(TMDB_ID, null);

            assertNotNull(result);
            assertEquals(TMDB_ID, result.getTmdbId());
            assertEquals("Public Movie", result.getTitle());
            assertEquals(Boolean.FALSE, result.getIsFavourited());
            assertEquals(WatchStatus.NONE, result.getWatchStatus());
        }

        @Test
        void getMovieDetails_WhenUserNotFound_ThrowsUserNotFoundException() {
            when(usersRepository.findByIdWithFavorites(1L)).thenReturn(Optional.empty());
            when(mediaResolutionService.resolveMediaByTmdbId(TMDB_ID, MediaType.MOVIE)).thenReturn(media);
            when(publicMediaDetailBaseCacheService.getMovieBase(TMDB_ID, MediaType.MOVIE)).thenReturn(publicBase("Test Movie"));

            UserNotFoundException exception = assertThrows(UserNotFoundException.class,
                () -> mediaService.getMovieDetails(TMDB_ID, user));
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
            when(userShowTrackingRepository.findWatchedShowsByUser(eq(user), any(Pageable.class)))
                .thenReturn(page);

            Page<Media> result = mediaService.getShowsWatchedPage(user);

            assertNotNull(result);
            assertEquals(1, result.getContent().size());
            assertEquals(media, result.getContent().get(0));
            verify(userShowTrackingRepository).findWatchedShowsByUser(eq(user), any(Pageable.class));
        }
    }

    private PublicMovieDetailBaseDTO publicBase(String title) {
        return PublicMovieDetailBaseDTO.builder()
            .tmdbId(TMDB_ID)
            .title(title)
            .type(MediaType.MOVIE)
            .genres(List.of())
            .build();
    }
}








