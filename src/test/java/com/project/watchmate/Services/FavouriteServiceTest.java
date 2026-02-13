package com.project.watchmate.Services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.project.watchmate.Dto.FavouriteStatusDTO;
import com.project.watchmate.Dto.MediaDetailsDTO;
import com.project.watchmate.Dto.UserFavouritesDTO;
import com.project.watchmate.Exception.DuplicateFavouriteException;
import com.project.watchmate.Exception.MediaNotFoundException;
import com.project.watchmate.Mappers.WatchMateMapper;
import com.project.watchmate.Models.Media;
import com.project.watchmate.Models.UserMediaStatus;
import com.project.watchmate.Models.Users;
import com.project.watchmate.Models.WatchStatus;
import com.project.watchmate.Repositories.MediaRepository;
import com.project.watchmate.Repositories.UserMediaStatusRepository;
import com.project.watchmate.Repositories.UsersRepository;

@ExtendWith(MockitoExtension.class)
class FavouriteServiceTest {

    @Mock
    private MediaRepository mediaRepository;

    @Mock
    private UsersRepository usersRepository;

    @Mock
    private WatchMateMapper watchMateMapper;

    @Mock
    private UserMediaStatusRepository userMediaStatusRepository;

    @InjectMocks
    private FavouriteService favouriteService;

    private static final Long TMDB_ID = 12345L;
    private Media media;
    private Users user;

    @BeforeEach
    void setUp() {
        media = Media.builder()
            .id(1L)
            .tmdbId(TMDB_ID)
            .title("Test Media")
            .reviews(new ArrayList<>())
            .build();
        user = Users.builder()
            .id(1L)
            .username("testuser")
            .favorites(new ArrayList<>())
            .build();
    }

    @Nested
    @DisplayName("addToFavourites")
    class AddToFavouritesTests {

        @Test
        void addToFavourites_WhenMediaExistsAndNotFavourited_AddsMediaAndReturnsDtoWithTrue() {
            when(mediaRepository.findByTmdbId(TMDB_ID)).thenReturn(Optional.of(media));
            when(usersRepository.save(any(Users.class))).thenReturn(user);

            FavouriteStatusDTO result = favouriteService.addToFavourites(TMDB_ID, user);

            assertEquals(TMDB_ID, result.getTmdbId());
            assertTrue(result.isFavourited());
            assertTrue(user.getFavorites().contains(media));
            verify(usersRepository).save(user);
        }

        @Test
        void addToFavourites_WhenMediaNotFound_ThrowsMediaNotFoundException() {
            when(mediaRepository.findByTmdbId(TMDB_ID)).thenReturn(Optional.empty());

            MediaNotFoundException exception = assertThrows(
                MediaNotFoundException.class,
                () -> favouriteService.addToFavourites(TMDB_ID, user)
            );

            assertEquals("Media does not exist!", exception.getMessage());
            verify(usersRepository, never()).save(any(Users.class));
        }

        @Test
        void addToFavourites_WhenAlreadyFavourited_ThrowsDuplicateFavouriteException() {
            user.getFavorites().add(media);
            when(mediaRepository.findByTmdbId(TMDB_ID)).thenReturn(Optional.of(media));

            DuplicateFavouriteException exception = assertThrows(
                DuplicateFavouriteException.class,
                () -> favouriteService.addToFavourites(TMDB_ID, user)
            );

            assertEquals("Media has already been favourited.", exception.getMessage());
            verify(usersRepository, never()).save(any(Users.class));
        }
    }

    @Nested
    @DisplayName("removeFromFavourites")
    class RemoveFromFavouritesTests {

        @Test
        void removeFromFavourites_WhenMediaExistsAndFavourited_RemovesAndReturnsDtoWithFalse() {
            user.getFavorites().add(media);
            when(mediaRepository.findByTmdbId(TMDB_ID)).thenReturn(Optional.of(media));
            when(usersRepository.save(any(Users.class))).thenReturn(user);

            FavouriteStatusDTO result = favouriteService.removeFromFavourites(TMDB_ID, user);

            assertEquals(TMDB_ID, result.getTmdbId());
            assertFalse(result.isFavourited());
            assertFalse(user.getFavorites().contains(media));
            verify(usersRepository).save(user);
        }

        @Test
        void removeFromFavourites_WhenMediaExistsButNotFavourited_StillSavesAndReturnsFalse() {
            when(mediaRepository.findByTmdbId(TMDB_ID)).thenReturn(Optional.of(media));
            when(usersRepository.save(any(Users.class))).thenReturn(user);

            FavouriteStatusDTO result = favouriteService.removeFromFavourites(TMDB_ID, user);

            assertEquals(TMDB_ID, result.getTmdbId());
            assertFalse(result.isFavourited());
            verify(usersRepository).save(user);
        }

        @Test
        void removeFromFavourites_WhenMediaNotFound_ThrowsMediaNotFoundException() {
            when(mediaRepository.findByTmdbId(TMDB_ID)).thenReturn(Optional.empty());

            MediaNotFoundException exception = assertThrows(
                MediaNotFoundException.class,
                () -> favouriteService.removeFromFavourites(TMDB_ID, user)
            );

            assertEquals("Media does not exist!", exception.getMessage());
            verify(usersRepository, never()).save(any(Users.class));
        }
    }

    @Nested
    @DisplayName("isFavourited")
    class IsFavouritedTests {

        @Test
        void isFavourited_WhenMediaExistsAndFavourited_ReturnsDtoWithTrue() {
            user.getFavorites().add(media);
            when(mediaRepository.findByTmdbId(TMDB_ID)).thenReturn(Optional.of(media));

            FavouriteStatusDTO result = favouriteService.isFavourited(TMDB_ID, user);

            assertEquals(TMDB_ID, result.getTmdbId());
            assertTrue(result.isFavourited());
            verify(usersRepository, never()).save(any(Users.class));
        }

        @Test
        void isFavourited_WhenMediaExistsAndNotFavourited_ReturnsDtoWithFalse() {
            when(mediaRepository.findByTmdbId(TMDB_ID)).thenReturn(Optional.of(media));

            FavouriteStatusDTO result = favouriteService.isFavourited(TMDB_ID, user);

            assertEquals(TMDB_ID, result.getTmdbId());
            assertFalse(result.isFavourited());
        }

        @Test
        void isFavourited_WhenMediaNotFound_ThrowsMediaNotFoundException() {
            when(mediaRepository.findByTmdbId(TMDB_ID)).thenReturn(Optional.empty());

            MediaNotFoundException exception = assertThrows(
                MediaNotFoundException.class,
                () -> favouriteService.isFavourited(TMDB_ID, user)
            );

            assertEquals("Media does not exist!", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("getUserFavourites")
    class GetUserFavouritesTests {

        @Test
        void getUserFavourites_WhenUserHasFavourites_ReturnsDtoWithMappedListAndCount() {
            user.getFavorites().add(media);
            MediaDetailsDTO mappedDto = MediaDetailsDTO.builder().tmdbId(TMDB_ID).title("Test Media").build();
            when(userMediaStatusRepository.findByUserAndMedia(user, media))
                .thenReturn(Optional.of(UserMediaStatus.builder().status(WatchStatus.WATCHED).build()));
            when(watchMateMapper.mapToMediaDetailsDTO(any(), any(), eq(true), eq(WatchStatus.WATCHED)))
                .thenReturn(mappedDto);

            UserFavouritesDTO result = favouriteService.getUserFavourites(user);

            assertEquals(1, result.getTotalCount());
            assertEquals(1, result.getFavourites().size());
            assertEquals(mappedDto, result.getFavourites().get(0));
        }

        @Test
        void getUserFavourites_WhenUserHasNoFavourites_ReturnsEmptyListAndZeroCount() {
            UserFavouritesDTO result = favouriteService.getUserFavourites(user);

            assertEquals(0, result.getTotalCount());
            assertTrue(result.getFavourites().isEmpty());
        }

        @Test
        void getUserFavourites_WhenNoUserMediaStatus_UsesWatchStatusNone() {
            user.getFavorites().add(media);
            MediaDetailsDTO mappedDto = MediaDetailsDTO.builder().tmdbId(TMDB_ID).build();
            when(userMediaStatusRepository.findByUserAndMedia(user, media)).thenReturn(Optional.empty());
            when(watchMateMapper.mapToMediaDetailsDTO(any(), any(), eq(true), eq(WatchStatus.NONE)))
                .thenReturn(mappedDto);

            UserFavouritesDTO result = favouriteService.getUserFavourites(user);

            assertEquals(1, result.getTotalCount());
            verify(watchMateMapper).mapToMediaDetailsDTO(any(), any(), eq(true), eq(WatchStatus.NONE));
        }
    }
}
