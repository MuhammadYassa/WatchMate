package com.project.watchmate.Services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
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

import com.project.watchmate.Dto.MovieDetailsDTO;
import com.project.watchmate.Mappers.WatchMateMapper;
import com.project.watchmate.Models.Media;
import com.project.watchmate.Models.MediaType;
import com.project.watchmate.Models.Users;
import com.project.watchmate.Models.WatchStatus;
import com.project.watchmate.Repositories.MediaRepository;
import com.project.watchmate.Repositories.ReviewRepository;
import com.project.watchmate.Repositories.UserMediaStatusRepository;
import com.project.watchmate.Repositories.UserShowTrackingRepository;
import com.project.watchmate.Repositories.UsersRepository;

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
    private TmdbService tmdbService;

    @InjectMocks
    private MediaService mediaService;

    private static final Long TMDB_ID = 100L;
    private Users user;
    private Media media;
    private MovieDetailsDTO expectedDto;

    @BeforeEach
    void setUp() {
        user = Users.builder().id(1L).username("user").favorites(new ArrayList<>()).build();
        media = Media.builder().id(1L).tmdbId(TMDB_ID).title("Test Movie").type(MediaType.MOVIE).reviews(new ArrayList<>()).build();
        expectedDto = MovieDetailsDTO.builder()
            .tmdbId(TMDB_ID)
            .type(MediaType.MOVIE)
            .watchStatus(WatchStatus.NONE)
            .isFavourited(Boolean.FALSE)
            .reviews(List.of())
            .title("Test Movie")
            .build();
    }

    @Nested
    @DisplayName("Get Movie Details Tests")
    class GetMovieDetailsTests {

        @Test
        void getMovieDetails_WhenAuthenticatedMovieResolves_ReturnsMappedDto() {
            when(usersRepository.findByIdWithFavorites(1L)).thenReturn(Optional.of(user));
            when(mediaResolutionService.resolveMediaByTmdbId(TMDB_ID, MediaType.MOVIE)).thenReturn(media);
            when(reviewRepository.findByMedia(media)).thenReturn(List.of());
            when(userWatchStatusResolver.resolveWatchStatus(user, media)).thenReturn(WatchStatus.NONE);
            when(watchMateMapper.mapToMovieDetailsDTO(media, List.of(), Boolean.FALSE, WatchStatus.NONE)).thenReturn(expectedDto);

            MovieDetailsDTO result = mediaService.getMovieDetails(TMDB_ID, user);

            assertNotNull(result);
            assertEquals(TMDB_ID, result.getTmdbId());
            assertEquals("Test Movie", result.getTitle());
            assertEquals(MediaType.MOVIE, result.getType());
            assertEquals(WatchStatus.NONE, result.getWatchStatus());
            assertEquals(List.of(), result.getReviews());
            assertEquals(Boolean.FALSE, result.getIsFavourited());
            verify(watchMateMapper).mapToMovieDetailsDTO(any(Media.class), anyList(), anyBoolean(), any(WatchStatus.class));
        }

        @Test
        void getMovieDetails_WhenMovieIsPublic_ReturnsDtoWithNullUserFields() {
            Media publicMovie = Media.builder().tmdbId(TMDB_ID).title("Public Movie").type(MediaType.MOVIE).build();
            MovieDetailsDTO publicDto = MovieDetailsDTO.builder().tmdbId(TMDB_ID).title("Public Movie").type(MediaType.MOVIE).build();

            when(mediaRepository.findByTmdbIdAndType(TMDB_ID, MediaType.MOVIE)).thenReturn(Optional.empty());
            when(tmdbService.fetchMediaByTmdbId(TMDB_ID, MediaType.MOVIE)).thenReturn(publicMovie);
            when(watchMateMapper.mapToMovieDetailsDTO(publicMovie, List.of(), false, WatchStatus.NONE)).thenReturn(publicDto);

            MovieDetailsDTO result = mediaService.getMovieDetails(TMDB_ID, null);

            assertNotNull(result);
            assertEquals(TMDB_ID, result.getTmdbId());
            verify(watchMateMapper).mapToMovieDetailsDTO(any(Media.class), anyList(), anyBoolean(), any(WatchStatus.class));
        }

        @Test
        void getMovieDetails_WhenUserNotFound_ThrowsRuntimeException() {
            when(usersRepository.findByIdWithFavorites(1L)).thenReturn(Optional.empty());
            when(mediaResolutionService.resolveMediaByTmdbId(TMDB_ID, MediaType.MOVIE)).thenReturn(media);

            RuntimeException exception = assertThrows(RuntimeException.class,
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
}
