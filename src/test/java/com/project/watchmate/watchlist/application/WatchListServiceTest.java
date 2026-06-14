package com.project.watchmate.watchlist.application;

import com.project.watchmate.media.catalog.application.MediaResolutionService;

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
import org.springframework.data.domain.Page;
import org.springframework.dao.DataIntegrityViolationException;

import com.project.watchmate.common.cache.WatchMateCacheEvictionService;
import com.project.watchmate.watchlist.dto.WatchListDTO;
import com.project.watchmate.watchlist.dto.WatchListPageCacheDTO;
import com.project.watchmate.common.error.DuplicateWatchListMediaException;
import com.project.watchmate.common.error.MediaNotInWatchListException;
import com.project.watchmate.common.error.UnauthorizedWatchListAccessException;
import com.project.watchmate.common.error.WatchListNotFoundException;
import com.project.watchmate.common.error.WatchlistNameConflictException;
import com.project.watchmate.common.mapper.WatchMateMapper;
import com.project.watchmate.media.catalog.domain.Media;
import com.project.watchmate.media.catalog.domain.MediaType;
import com.project.watchmate.movie.tracking.persistence.UserMediaStatusRepository;
import com.project.watchmate.watchlist.domain.WatchList;
import com.project.watchmate.watchlist.domain.WatchListItem;
import com.project.watchmate.user.domain.Users;
import com.project.watchmate.review.persistence.ReviewRepository;
import com.project.watchmate.show.tracking.persistence.UserShowTrackingRepository;
import com.project.watchmate.user.persistence.UsersRepository;
import com.project.watchmate.watchlist.persistence.WatchListItemRepository;
import com.project.watchmate.watchlist.persistence.WatchListRepository;

@ExtendWith(MockitoExtension.class)
class WatchListServiceTest {

    @Mock
    private WatchListRepository watchListRepository;

    @Mock
    private MediaResolutionService mediaResolutionService;

    @Mock
    private WatchMateMapper watchMateMapper;

    @Mock
    private ReviewRepository reviewsRepo;

    @Mock
    private WatchListItemRepository watchListItemRepository;

    @Mock
    private UserMediaStatusRepository userMediaStatusRepository;

    @Mock
    private UserShowTrackingRepository userShowTrackingRepository;

    @Mock
    private UsersRepository usersRepository;

    @Mock
    private WatchListDtoAssembler watchListDtoAssembler;

    @Mock
    private WatchListPageCacheService watchListPageCacheService;

    @Mock
    private WatchMateCacheEvictionService cacheEvictionService;

    @InjectMocks
    private WatchListService watchListService;

    private Users user;
    private Users otherUser;
    private WatchList watchList;
    private Media media;
    private static final Long WATCHLIST_ID = 1L;
    private static final Long TMDB_ID = 100L;
    private static final String TYPE = "MOVIE";

    @BeforeEach
    void setUp() {
        user = Users.builder().id(1L).username("owner").favorites(new ArrayList<>()).build();
        otherUser = Users.builder().id(2L).username("other").build();
        watchList = WatchList.builder().id(WATCHLIST_ID).name("My List").user(user).items(new ArrayList<>()).build();
        media = Media.builder().id(10L).tmdbId(TMDB_ID).title("Movie").type(MediaType.MOVIE).build();
    }

    @Test
    void getAllWatchLists_batchesMappingLookupsForCurrentPage() {
        WatchListDTO dto = WatchListDTO.builder().id(WATCHLIST_ID).name("My List").media(List.of()).build();
        when(watchListPageCacheService.getAllWatchLists(user, 0, 20))
            .thenReturn(WatchListPageCacheDTO.builder()
                .content(List.of(dto))
                .page(0)
                .size(20)
                .totalElements(1)
                .build());

        Page<WatchListDTO> result = watchListService.getAllWatchLists(user, 0, 20);

        assertEquals(1, result.getContent().size());
        assertEquals(WATCHLIST_ID, result.getContent().get(0).getId());
        assertEquals(1, result.getTotalElements());
        verify(watchListPageCacheService).getAllWatchLists(user, 0, 20);
    }

    @Nested
    @DisplayName("Create WatchList Tests")
    class CreateWatchListTests {

        @Test
        void createWatchList_WithNewName_SavesAndReturnsDto() {
            when(watchListRepository.existsByUserAndNameIgnoreCase(user, "New List")).thenReturn(false);
            when(watchListRepository.saveAndFlush(any(WatchList.class))).thenAnswer(inv -> inv.getArgument(0));
            WatchListDTO dto = WatchListDTO.builder().id(1L).name("New List").build();
            when(watchListDtoAssembler.mapToWatchListDTO(any(WatchList.class))).thenReturn(dto);

            WatchListDTO result = watchListService.createWatchList(user, "New List");

            ArgumentCaptor<WatchList> captor = ArgumentCaptor.forClass(WatchList.class);
            verify(watchListRepository).saveAndFlush(captor.capture());
            WatchList saved = captor.getValue();
            assertNotNull(saved);
            assertEquals("New List", saved.getName());
            assertEquals("New List", result.getName());
            verify(cacheEvictionService).evictWatchlistSummaryPages();
        }

        @Test
        void createWatchList_WhenNameExists_ThrowsWatchlistNameConflictException() {
            when(watchListRepository.existsByUserAndNameIgnoreCase(user, "Existing")).thenReturn(true);

            WatchlistNameConflictException e = assertThrows(WatchlistNameConflictException.class,
                () -> watchListService.createWatchList(user, "Existing"));

            assertEquals("Watchlist Already Exists.", e.getMessage());
            verify(watchListRepository, never()).saveAndFlush(any(WatchList.class));
        }

        @Test
        void createWatchList_WhenDatabaseConstraintFails_ThrowsWatchlistNameConflictException() {
            when(watchListRepository.existsByUserAndNameIgnoreCase(user, "Existing")).thenReturn(false);
            when(watchListRepository.saveAndFlush(any(WatchList.class)))
                .thenThrow(new DataIntegrityViolationException("constraint [uk_watchlist_user_name]"));

            WatchlistNameConflictException e = assertThrows(WatchlistNameConflictException.class,
                () -> watchListService.createWatchList(user, "Existing"));

            assertEquals("Watchlist Already Exists.", e.getMessage());
        }
    }

    @Nested
    @DisplayName("Delete WatchList Tests")
    class DeleteWatchListTests {

        @Test
        void deleteWatchList_WhenOwner_DeletesWatchList() {
            when(watchListRepository.findById(WATCHLIST_ID)).thenReturn(Optional.of(watchList));

            watchListService.deleteWatchList(user, WATCHLIST_ID);

            verify(watchListRepository).delete(watchList);
            verify(cacheEvictionService).evictWatchlistSummaryPages();
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
    @DisplayName("Add Media to WatchList Tests")
    class AddMediaToWatchListTests {

        @Test
        void addMediaToWatchList_WhenValid_AddsItemAndSaves() {
            when(watchListRepository.findById(WATCHLIST_ID)).thenReturn(Optional.of(watchList));
            when(mediaResolutionService.resolveMediaByTmdbId(TMDB_ID, TYPE)).thenReturn(media);
            when(watchListRepository.save(any(WatchList.class))).thenReturn(watchList);
            WatchListDTO dto = WatchListDTO.builder().id(WATCHLIST_ID).name("My List").build();
            when(watchListDtoAssembler.mapToWatchListDTO(any(WatchList.class))).thenReturn(dto);

            WatchListDTO result = watchListService.addMediaToWatchList(user, WATCHLIST_ID, TMDB_ID, TYPE);

            assertNotNull(result);
            assertEquals(WATCHLIST_ID, result.getId());
            assertEquals("My List", result.getName());
            assertEquals(1, watchList.getItems().size());
            verify(watchListRepository).save(watchList);
            verify(cacheEvictionService).evictWatchlistSummaryPages();
        }

        @Test
        void addMediaToWatchList_WhenNotOwner_ThrowsUnauthorizedWatchListAccessException() {
            when(watchListRepository.findById(WATCHLIST_ID)).thenReturn(Optional.of(watchList));

            UnauthorizedWatchListAccessException e = assertThrows(UnauthorizedWatchListAccessException.class,
                () -> watchListService.addMediaToWatchList(otherUser, WATCHLIST_ID, TMDB_ID, TYPE));
            
            assertEquals("You do not own this WatchList", e.getMessage());
            verify(watchListRepository, never()).save(any(WatchList.class));
        }

        @Test
        void addMediaToWatchList_WhenMediaAlreadyInList_ThrowsDuplicateWatchListMediaException() {
            WatchListItem existing = WatchListItem.builder().media(media).watchList(watchList).build();
            watchList.getItems().add(existing);
            when(watchListRepository.findById(WATCHLIST_ID)).thenReturn(Optional.of(watchList));
            when(mediaResolutionService.resolveMediaByTmdbId(TMDB_ID, TYPE)).thenReturn(media);

            DuplicateWatchListMediaException e = assertThrows(DuplicateWatchListMediaException.class,
                () -> watchListService.addMediaToWatchList(user, WATCHLIST_ID, TMDB_ID, TYPE));

            assertEquals("Media already exists in current WatchList", e.getMessage());
        }
    }

    @Nested
    @DisplayName("Remove Media from WatchList Tests")
    class RemoveMediaFromWatchListTests {

        @Test
        void removeMediaFromWatchList_WhenItemExists_RemovesAndSaves() {
            WatchListItem item = WatchListItem.builder().media(media).watchList(watchList).build();
            watchList.getItems().add(item);
            when(watchListRepository.findById(WATCHLIST_ID)).thenReturn(Optional.of(watchList));
            when(mediaResolutionService.resolveMediaByTmdbId(TMDB_ID, TYPE)).thenReturn(media);
            when(watchListRepository.save(any(WatchList.class))).thenReturn(watchList);
            WatchListDTO dto = WatchListDTO.builder().id(WATCHLIST_ID).name("My List").build();
            when(watchListDtoAssembler.mapToWatchListDTO(any(WatchList.class))).thenReturn(dto);

            WatchListDTO result = watchListService.removeMediaFromWatchList(user, WATCHLIST_ID, TMDB_ID, TYPE);

            assertNotNull(result);
            assertEquals(WATCHLIST_ID, result.getId());
            assertEquals("My List", result.getName());
            assertEquals(0, watchList.getItems().size());
            verify(watchListRepository).save(watchList);
            verify(cacheEvictionService).evictWatchlistSummaryPages();
        }

        @Test
        void removeMediaFromWatchList_WhenMediaNotInList_ThrowsMediaNotInWatchListException() {
            when(watchListRepository.findById(WATCHLIST_ID)).thenReturn(Optional.of(watchList));
            when(mediaResolutionService.resolveMediaByTmdbId(TMDB_ID, TYPE)).thenReturn(media);

            MediaNotInWatchListException e = assertThrows(MediaNotInWatchListException.class,
                () -> watchListService.removeMediaFromWatchList(user, WATCHLIST_ID, TMDB_ID, TYPE));

            assertEquals("Media is not in this watchlist", e.getMessage());
        }

        @Test
        void removeMediaFromWatchList_WhenNotOwner_ThrowsUnauthorizedWatchListAccessException() {
            when(watchListRepository.findById(WATCHLIST_ID)).thenReturn(Optional.of(watchList));

            UnauthorizedWatchListAccessException e = assertThrows(UnauthorizedWatchListAccessException.class,
                () -> watchListService.removeMediaFromWatchList(otherUser, WATCHLIST_ID, TMDB_ID, TYPE));

            assertEquals("You do not own this WatchList", e.getMessage());
        }
    }

    @Nested
    @DisplayName("Rename WatchList Tests")
    class RenameWatchListTests {

        @Test
        void renameWatchList_WhenOwnerAndNewNameUnique_SavesAndReturnsDto() {
            when(watchListRepository.findById(WATCHLIST_ID)).thenReturn(Optional.of(watchList));
            when(watchListRepository.existsByUserAndNameIgnoreCase(user, "New Name")).thenReturn(false);
            when(watchListRepository.saveAndFlush(any(WatchList.class))).thenReturn(watchList);
            WatchListDTO dto = WatchListDTO.builder().id(WATCHLIST_ID).name("New Name").build();
            when(watchListDtoAssembler.mapToWatchListDTO(any(WatchList.class))).thenReturn(dto);

            WatchListDTO result = watchListService.renameWatchList(user, WATCHLIST_ID, "New Name");

            assertNotNull(result);
            assertEquals(WATCHLIST_ID, result.getId());
            assertEquals("New Name", watchList.getName());
            verify(watchListRepository).saveAndFlush(watchList);
            verify(cacheEvictionService).evictWatchlistSummaryPages();
        }

        @Test
        void renameWatchList_WhenNotOwner_ThrowsUnauthorizedWatchListAccessException() {
            when(watchListRepository.findById(WATCHLIST_ID)).thenReturn(Optional.of(watchList));

            UnauthorizedWatchListAccessException e = assertThrows(UnauthorizedWatchListAccessException.class,
                () -> watchListService.renameWatchList(otherUser, WATCHLIST_ID, "New Name"));
            
            assertEquals("You do not own this WatchList.", e.getMessage());
        }

        @Test
        void renameWatchList_WhenNewNameAlreadyExists_ThrowsWatchlistNameConflictException() {
            when(watchListRepository.findById(WATCHLIST_ID)).thenReturn(Optional.of(watchList));
            when(watchListRepository.existsByUserAndNameIgnoreCase(user, "Taken")).thenReturn(true);

            WatchlistNameConflictException e = assertThrows(WatchlistNameConflictException.class,
                () -> watchListService.renameWatchList(user, WATCHLIST_ID, "Taken"));
                
            assertEquals("A WatchList with this name Already Exists", e.getMessage());
        }

        @Test
        void renameWatchList_WhenDatabaseConstraintFails_ThrowsWatchlistNameConflictException() {
            when(watchListRepository.findById(WATCHLIST_ID)).thenReturn(Optional.of(watchList));
            when(watchListRepository.existsByUserAndNameIgnoreCase(user, "Taken")).thenReturn(false);
            when(watchListRepository.saveAndFlush(watchList))
                .thenThrow(new DataIntegrityViolationException("constraint [uk_watchlist_user_name]"));

            WatchlistNameConflictException e = assertThrows(WatchlistNameConflictException.class,
                () -> watchListService.renameWatchList(user, WATCHLIST_ID, "Taken"));

            assertEquals("A WatchList with this name Already Exists", e.getMessage());
        }
    }
}






