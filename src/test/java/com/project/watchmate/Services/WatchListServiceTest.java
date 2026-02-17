package com.project.watchmate.Services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.project.watchmate.Dto.MediaDetailsDTO;
import com.project.watchmate.Dto.WatchListDTO;
import com.project.watchmate.Exception.DuplicateWatchListMediaException;
import com.project.watchmate.Exception.MediaNotInWatchListException;
import com.project.watchmate.Exception.MediaNotFoundException;
import com.project.watchmate.Exception.UnauthorizedWatchListAccessException;
import com.project.watchmate.Exception.WatchListNotFoundException;
import com.project.watchmate.Mappers.WatchMateMapper;
import com.project.watchmate.Models.Media;
import com.project.watchmate.Models.WatchList;
import com.project.watchmate.Models.WatchListItem;
import com.project.watchmate.Models.Users;
import com.project.watchmate.Repositories.MediaRepository;
import com.project.watchmate.Repositories.ReviewRepository;
import com.project.watchmate.Repositories.UserMediaStatusRepository;
import com.project.watchmate.Repositories.WatchListRepository;

@ExtendWith(MockitoExtension.class)
class WatchListServiceTest {

    @Mock
    private WatchListRepository watchListRepository;

    @Mock
    private MediaRepository mediaRepository;

    @Mock
    private WatchMateMapper watchMateMapper;

    @Mock
    private ReviewRepository reviewsRepo;

    @Mock
    private UserMediaStatusRepository userMediaStatusRepository;

    @InjectMocks
    private WatchListService watchListService;

    private Users user;
    private Users otherUser;
    private WatchList watchList;
    private Media media;
    private static final Long WATCHLIST_ID = 1L;
    private static final Long TMDB_ID = 100L;

    @BeforeEach
    void setUp() {
        user = Users.builder().id(1L).username("owner").favorites(new ArrayList<>()).build();
        otherUser = Users.builder().id(2L).username("other").build();
        watchList = WatchList.builder().id(WATCHLIST_ID).name("My List").user(user).items(new ArrayList<>()).build();
        media = Media.builder().id(10L).tmdbId(TMDB_ID).title("Movie").build();
    }

    @Nested
    @DisplayName("createWatchList")
    class CreateWatchListTests {

        @Test
        void createWatchList_WithNewName_SavesAndReturnsDto() {
            when(watchListRepository.existsByUserAndNameIgnoreCase(user, "New List")).thenReturn(false);
            when(watchListRepository.save(any(WatchList.class))).thenAnswer(inv -> inv.getArgument(0));
            WatchListDTO dto = WatchListDTO.builder().id(1L).name("New List").build();
            when(watchMateMapper.mapToWatchListDTO(any(WatchList.class), any())).thenReturn(dto);

            WatchListDTO result = watchListService.createWatchList(user, "New List");

            ArgumentCaptor<WatchList> captor = ArgumentCaptor.forClass(WatchList.class);
            verify(watchListRepository).save(captor.capture());
            WatchList saved = captor.getValue();
            assertNotNull(saved);
            assertEquals("New List", saved.getName());
            assertEquals("New List", result.getName());
        }

        @Test
        void createWatchList_WhenNameExists_ThrowsIllegalArgumentException() {
            when(watchListRepository.existsByUserAndNameIgnoreCase(user, "Existing")).thenReturn(true);

            IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> watchListService.createWatchList(user, "Existing"));

            assertEquals("Watchlist Already Exists.", e.getMessage());
            verify(watchListRepository, never()).save(any(WatchList.class));
        }
    }

    @Nested
    @DisplayName("deleteWatchList")
    class DeleteWatchListTests {

        @Test
        void deleteWatchList_WhenOwner_DeletesWatchList() {
            when(watchListRepository.findById(WATCHLIST_ID)).thenReturn(Optional.of(watchList));

            watchListService.deleteWatchList(user, WATCHLIST_ID);

            verify(watchListRepository).delete(watchList);
        }

        @Test
        void deleteWatchList_WhenNotOwner_ThrowsUnauthorizedWatchListAccessException() {
            when(watchListRepository.findById(WATCHLIST_ID)).thenReturn(Optional.of(watchList));

            UnauthorizedWatchListAccessException e = assertThrows(UnauthorizedWatchListAccessException.class,
                () -> watchListService.deleteWatchList(otherUser, WATCHLIST_ID));

            assertEquals("You do not own this watchlist", e.getMessage());
            verify(watchListRepository, never()).delete(any(WatchList.class));
        }

        @Test
        void deleteWatchList_WhenWatchListNotFound_ThrowsWatchListNotFoundException() {
            when(watchListRepository.findById(WATCHLIST_ID)).thenReturn(Optional.empty());

            WatchListNotFoundException e = assertThrows(WatchListNotFoundException.class,
                () -> watchListService.deleteWatchList(user, WATCHLIST_ID));

            assertEquals("WatchList not found", e.getMessage());
            verify(watchListRepository, never()).delete(any(WatchList.class));
        }
    }

    @Nested
    @DisplayName("addMediaToWatchList")
    class AddMediaToWatchListTests {

        @Test
        void addMediaToWatchList_WhenValid_AddsItemAndSaves() {
            when(watchListRepository.findById(WATCHLIST_ID)).thenReturn(Optional.of(watchList));
            when(mediaRepository.findByTmdbId(TMDB_ID)).thenReturn(Optional.of(media));
            when(watchListRepository.save(any(WatchList.class))).thenReturn(watchList);
            when(reviewsRepo.findByMedia(media)).thenReturn(List.of());
            when(userMediaStatusRepository.findByUserAndMedia(user, media)).thenReturn(Optional.empty());
            WatchListDTO dto = WatchListDTO.builder().id(WATCHLIST_ID).name("My List").build();
            when(watchMateMapper.mapToMediaDetailsDTO(any(), any(), any(Boolean.class), any())).thenReturn(MediaDetailsDTO.builder().build());
            when(watchMateMapper.mapToWatchListDTO(any(WatchList.class), any())).thenReturn(dto);

            WatchListDTO result = watchListService.addMediaToWatchList(user, WATCHLIST_ID, TMDB_ID);

            assertNotNull(result);
            assertEquals(WATCHLIST_ID, result.getId());
            assertEquals("My List", result.getName());
            assertEquals(1, watchList.getItems().size());
            verify(watchListRepository).save(watchList);
        }

        @Test
        void addMediaToWatchList_WhenNotOwner_ThrowsUnauthorizedWatchListAccessException() {
            when(watchListRepository.findById(WATCHLIST_ID)).thenReturn(Optional.of(watchList));

            UnauthorizedWatchListAccessException e = assertThrows(UnauthorizedWatchListAccessException.class,
                () -> watchListService.addMediaToWatchList(otherUser, WATCHLIST_ID, TMDB_ID));
            
            assertEquals("You do not own this WatchList", e.getMessage());
            verify(watchListRepository, never()).save(any(WatchList.class));
        }

        @Test
        void addMediaToWatchList_WhenMediaNotFound_ThrowsMediaNotFoundException() {
            when(watchListRepository.findById(WATCHLIST_ID)).thenReturn(Optional.of(watchList));
            when(mediaRepository.findByTmdbId(TMDB_ID)).thenReturn(Optional.empty());

            MediaNotFoundException e = assertThrows(MediaNotFoundException.class,
                () -> watchListService.addMediaToWatchList(user, WATCHLIST_ID, TMDB_ID));
            
            assertEquals("Media does not exist.", e.getMessage());
        }

        @Test
        void addMediaToWatchList_WhenMediaAlreadyInList_ThrowsDuplicateWatchListMediaException() {
            WatchListItem existing = WatchListItem.builder().media(media).watchList(watchList).build();
            watchList.getItems().add(existing);
            when(watchListRepository.findById(WATCHLIST_ID)).thenReturn(Optional.of(watchList));
            when(mediaRepository.findByTmdbId(TMDB_ID)).thenReturn(Optional.of(media));

            DuplicateWatchListMediaException e = assertThrows(DuplicateWatchListMediaException.class,
                () -> watchListService.addMediaToWatchList(user, WATCHLIST_ID, TMDB_ID));

            assertEquals("Media already exists in current WatchList", e.getMessage());
        }
    }

    @Nested
    @DisplayName("removeMediaFromWatchList")
    class RemoveMediaFromWatchListTests {

        @Test
        void removeMediaFromWatchList_WhenItemExists_RemovesAndSaves() {
            WatchListItem item = WatchListItem.builder().media(media).watchList(watchList).build();
            watchList.getItems().add(item);
            when(watchListRepository.findById(WATCHLIST_ID)).thenReturn(Optional.of(watchList));
            when(mediaRepository.findByTmdbId(TMDB_ID)).thenReturn(Optional.of(media));
            when(watchListRepository.save(any(WatchList.class))).thenReturn(watchList);
            WatchListDTO dto = WatchListDTO.builder().id(WATCHLIST_ID).name("My List").build();
            when(watchMateMapper.mapToWatchListDTO(any(WatchList.class), any())).thenReturn(dto);

            WatchListDTO result = watchListService.removeMediaFromWatchList(user, WATCHLIST_ID, TMDB_ID);

            assertNotNull(result);
            assertEquals(WATCHLIST_ID, result.getId());
            assertEquals("My List", result.getName());
            assertEquals(0, watchList.getItems().size());
            verify(watchListRepository).save(watchList);
        }

        @Test
        void removeMediaFromWatchList_WhenMediaNotInList_ThrowsMediaNotInWatchListException() {
            when(watchListRepository.findById(WATCHLIST_ID)).thenReturn(Optional.of(watchList));
            when(mediaRepository.findByTmdbId(TMDB_ID)).thenReturn(Optional.of(media));

            MediaNotInWatchListException e = assertThrows(MediaNotInWatchListException.class,
                () -> watchListService.removeMediaFromWatchList(user, WATCHLIST_ID, TMDB_ID));

            assertEquals("Media is not in this watchlist", e.getMessage());
        }

        @Test
        void removeMediaFromWatchList_WhenNotOwner_ThrowsUnauthorizedWatchListAccessException() {
            when(watchListRepository.findById(WATCHLIST_ID)).thenReturn(Optional.of(watchList));

            UnauthorizedWatchListAccessException e = assertThrows(UnauthorizedWatchListAccessException.class,
                () -> watchListService.removeMediaFromWatchList(otherUser, WATCHLIST_ID, TMDB_ID));

            assertEquals("You do not own this WatchList", e.getMessage());
        }
    }

    @Nested
    @DisplayName("renameWatchList")
    class RenameWatchListTests {

        @Test
        void renameWatchList_WhenOwnerAndNewNameUnique_SavesAndReturnsDto() {
            when(watchListRepository.findById(WATCHLIST_ID)).thenReturn(Optional.of(watchList));
            when(watchListRepository.existsByUserAndNameIgnoreCase(user, "New Name")).thenReturn(false);
            when(watchListRepository.save(any(WatchList.class))).thenReturn(watchList);
            WatchListDTO dto = WatchListDTO.builder().id(WATCHLIST_ID).name("New Name").build();
            when(watchMateMapper.mapToWatchListDTO(any(WatchList.class), any())).thenReturn(dto);

            WatchListDTO result = watchListService.renameWatchList(user, WATCHLIST_ID, "New Name");

            assertNotNull(result);
            assertEquals(WATCHLIST_ID, result.getId());
            assertEquals("New Name", watchList.getName());
            verify(watchListRepository).save(watchList);
        }

        @Test
        void renameWatchList_WhenNotOwner_ThrowsUnauthorizedWatchListAccessException() {
            when(watchListRepository.findById(WATCHLIST_ID)).thenReturn(Optional.of(watchList));

            UnauthorizedWatchListAccessException e = assertThrows(UnauthorizedWatchListAccessException.class,
                () -> watchListService.renameWatchList(otherUser, WATCHLIST_ID, "New Name"));
            
            assertEquals("You do not own this WatchList.", e.getMessage());
        }

        @Test
        void renameWatchList_WhenNewNameAlreadyExists_ThrowsIllegalArgumentException() {
            when(watchListRepository.findById(WATCHLIST_ID)).thenReturn(Optional.of(watchList));
            when(watchListRepository.existsByUserAndNameIgnoreCase(user, "Taken")).thenReturn(true);

            IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> watchListService.renameWatchList(user, WATCHLIST_ID, "Taken"));
                
            assertEquals("A WatchList with this name Already Exists", e.getMessage());
        }
    }
}
