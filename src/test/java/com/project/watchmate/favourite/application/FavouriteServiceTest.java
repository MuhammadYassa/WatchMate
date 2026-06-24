package com.project.watchmate.favourite.application;

import com.project.watchmate.media.catalog.application.MediaResolutionService;
import com.project.watchmate.media.catalog.application.UserWatchStatusResolver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
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

import com.project.watchmate.common.cache.WatchMateCacheEvictionService;
import com.project.watchmate.favourite.dto.FavouriteStatusDTO;
import com.project.watchmate.media.catalog.dto.MediaDetailsDTO;
import com.project.watchmate.common.error.DuplicateFavouriteException;
import com.project.watchmate.common.mapper.WatchMateMapper;
import com.project.watchmate.media.catalog.domain.Media;
import com.project.watchmate.user.domain.Users;
import com.project.watchmate.media.catalog.domain.WatchStatus;
import com.project.watchmate.user.persistence.UsersRepository;

@ExtendWith(MockitoExtension.class)
class FavouriteServiceTest {

    @Mock
    private MediaResolutionService mediaResolutionService;

    @Mock
    private UsersRepository usersRepository;

    @Mock
    private WatchMateMapper watchMateMapper;

    @Mock
    private UserWatchStatusResolver userWatchStatusResolver;

    @Mock
    private WatchMateCacheEvictionService cacheEvictionService;

    @InjectMocks
    private FavouriteService favouriteService;

    private static final Long TMDB_ID = 12345L;
    private static final String TYPE = "MOVIE";
    private Media media;
    private Users user;
    private Users managedUser;

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
        managedUser = Users.builder()
            .id(1L)
            .username("testuser")
            .favorites(new ArrayList<>())
            .build();
    }

    @Nested
    @DisplayName("Add to Favourites Tests")
    class AddToFavouritesTests {

        @Test
        void addToFavourites_WhenMediaExistsAndNotFavourited_AddsMediaAndReturnsDtoWithTrue() {
            when(usersRepository.findByIdWithFavorites(user.getId())).thenReturn(Optional.of(managedUser));
            when(mediaResolutionService.resolveMediaByTmdbId(TMDB_ID, TYPE)).thenReturn(media);

            FavouriteStatusDTO result = favouriteService.addToFavourites(TMDB_ID, TYPE, user);

            assertEquals(TMDB_ID, result.getTmdbId());
            assertTrue(result.isFavourited());
            assertTrue(managedUser.getFavorites().contains(media));
            verify(usersRepository).findByIdWithFavorites(user.getId());
            verify(cacheEvictionService).evictFavoriteCaches(user.getId());
        }

        @Test
        void addToFavourites_WhenAlreadyFavourited_ThrowsDuplicateFavouriteException() {
            managedUser.getFavorites().add(media);
            when(usersRepository.findByIdWithFavorites(user.getId())).thenReturn(Optional.of(managedUser));
            when(mediaResolutionService.resolveMediaByTmdbId(TMDB_ID, TYPE)).thenReturn(media);

            DuplicateFavouriteException exception = assertThrows(
                DuplicateFavouriteException.class,
                () -> favouriteService.addToFavourites(TMDB_ID, TYPE, user)
            );

            assertEquals("Media has already been favourited.", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Remove from Favourites Tests")
    class RemoveFromFavouritesTests {

        @Test
        void removeFromFavourites_WhenMediaExistsAndFavourited_RemovesAndReturnsDtoWithFalse() {
            managedUser.getFavorites().add(media);
            when(usersRepository.findByIdWithFavorites(user.getId())).thenReturn(Optional.of(managedUser));
            when(mediaResolutionService.resolveMediaByTmdbId(TMDB_ID, TYPE)).thenReturn(media);

            FavouriteStatusDTO result = favouriteService.removeFromFavourites(TMDB_ID, TYPE, user);

            assertEquals(TMDB_ID, result.getTmdbId());
            assertFalse(result.isFavourited());
            assertFalse(managedUser.getFavorites().contains(media));
            verify(cacheEvictionService).evictFavoriteCaches(user.getId());
        }

        @Test
        void removeFromFavourites_WhenMediaExistsButNotFavourited_ReturnsFalse() {
            when(usersRepository.findByIdWithFavorites(user.getId())).thenReturn(Optional.of(managedUser));
            when(mediaResolutionService.resolveMediaByTmdbId(TMDB_ID, TYPE)).thenReturn(media);

            FavouriteStatusDTO result = favouriteService.removeFromFavourites(TMDB_ID, TYPE, user);

            assertEquals(TMDB_ID, result.getTmdbId());
            assertFalse(result.isFavourited());
        }

    }

    @Nested
    @DisplayName("Is Favourited Tests")
    class IsFavouritedTests {

        @Test
        void isFavourited_WhenFavourited_ReturnsDtoWithTrue() {
            when(mediaResolutionService.resolveMediaByTmdbId(TMDB_ID, TYPE)).thenReturn(media);
            when(usersRepository.isFavouritedByUser(user.getId(), media.getId())).thenReturn(true);

            FavouriteStatusDTO result = favouriteService.isFavourited(TMDB_ID, TYPE, user);

            assertEquals(TMDB_ID, result.getTmdbId());
            assertTrue(result.isFavourited());
        }

        @Test
        void isFavourited_WhenNotFavourited_ReturnsDtoWithFalse() {
            when(mediaResolutionService.resolveMediaByTmdbId(TMDB_ID, TYPE)).thenReturn(media);
            when(usersRepository.isFavouritedByUser(user.getId(), media.getId())).thenReturn(false);

            FavouriteStatusDTO result = favouriteService.isFavourited(TMDB_ID, TYPE, user);

            assertEquals(TMDB_ID, result.getTmdbId());
            assertFalse(result.isFavourited());
        }

        @Test
        void isFavourited_DoesNotLoadFullFavouritesList() {
            when(mediaResolutionService.resolveMediaByTmdbId(TMDB_ID, TYPE)).thenReturn(media);
            when(usersRepository.isFavouritedByUser(user.getId(), media.getId())).thenReturn(true);

            favouriteService.isFavourited(TMDB_ID, TYPE, user);

            verify(usersRepository).isFavouritedByUser(user.getId(), media.getId());
            // findByIdWithFavorites must NOT be called for a simple status check
            verify(usersRepository, org.mockito.Mockito.never()).findByIdWithFavorites(any());
        }
    }

    @Nested
    @DisplayName("Get User Favourites Tests")
    class GetUserFavouritesTests {

        @Test
        void getUserFavourites_WhenUserHasFavourites_ReturnsPageWithMappedContent() {
            MediaDetailsDTO mappedDto = MediaDetailsDTO.builder().tmdbId(TMDB_ID).title("Test Media").build();
            when(usersRepository.findFavoritesByUserId(eq(user.getId()), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(media)));
            when(userWatchStatusResolver.resolveWatchStatus(user, media)).thenReturn(WatchStatus.WATCHED);
            when(watchMateMapper.mapToMediaDetailsDTO(any(), any(), eq(true), eq(WatchStatus.WATCHED)))
                .thenReturn(mappedDto);

            Page<MediaDetailsDTO> result = favouriteService.getUserFavourites(user, 0, 20);

            assertEquals(1, result.getTotalElements());
            assertEquals(1, result.getContent().size());
            assertEquals(mappedDto, result.getContent().get(0));
        }

        @Test
        void getUserFavourites_WhenUserHasNoFavourites_ReturnsEmptyPage() {
            when(usersRepository.findFavoritesByUserId(eq(user.getId()), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

            Page<MediaDetailsDTO> result = favouriteService.getUserFavourites(user, 0, 20);

            assertEquals(0, result.getTotalElements());
            assertTrue(result.getContent().isEmpty());
        }

        @Test
        void getUserFavourites_WhenNoUserMediaStatus_UsesWatchStatusNone() {
            MediaDetailsDTO mappedDto = MediaDetailsDTO.builder().tmdbId(TMDB_ID).build();
            when(usersRepository.findFavoritesByUserId(eq(user.getId()), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(media)));
            when(userWatchStatusResolver.resolveWatchStatus(user, media)).thenReturn(WatchStatus.NONE);
            when(watchMateMapper.mapToMediaDetailsDTO(any(), any(), eq(true), eq(WatchStatus.NONE)))
                .thenReturn(mappedDto);

            Page<MediaDetailsDTO> result = favouriteService.getUserFavourites(user, 0, 20);

            assertEquals(1, result.getTotalElements());
            verify(watchMateMapper).mapToMediaDetailsDTO(any(), any(), eq(true), eq(WatchStatus.NONE));
        }

        @Test
        void getUserFavourites_SizeCappedAt50() {
            when(usersRepository.findFavoritesByUserId(eq(user.getId()), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

            favouriteService.getUserFavourites(user, 0, 200);

            verify(usersRepository).findFavoritesByUserId(eq(user.getId()),
                argThat(p -> p.getPageSize() == 50));
        }
    }
}






