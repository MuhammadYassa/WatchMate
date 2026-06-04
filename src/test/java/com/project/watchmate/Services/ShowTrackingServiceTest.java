package com.project.watchmate.Services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;

import com.project.watchmate.Dto.ShowTrackingDTO;
import com.project.watchmate.Dto.ShowTrackingStatusDTO;
import com.project.watchmate.Dto.TmdbTvDetailsDTO;
import com.project.watchmate.Dto.UpdateShowTrackingPositionRequestDTO;
import com.project.watchmate.Dto.UpdateWatchStatusRequestDTO;
import com.project.watchmate.Exception.ShowMetadataSyncRequiredException;
import com.project.watchmate.Models.Media;
import com.project.watchmate.Models.MediaType;
import com.project.watchmate.Models.ShowEpisode;
import com.project.watchmate.Models.UserMediaStatus;
import com.project.watchmate.Models.UserShowTracking;
import com.project.watchmate.Models.Users;
import com.project.watchmate.Models.WatchStatus;
import com.project.watchmate.Repositories.UserMediaStatusRepository;
import com.project.watchmate.Repositories.UserShowTrackingRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ShowTrackingServiceTest {

    @Mock
    private ShowCatalogService showCatalogService;

    @Mock
    private UserMediaStatusRepository userMediaStatusRepository;

    @Mock
    private UserShowTrackingRepository userShowTrackingRepository;

    private final ShowStatusCalculator showStatusCalculator = new ShowStatusCalculator();

    private ShowTrackingService showTrackingService;

    private final AtomicReference<UserMediaStatus> persistedStatus = new AtomicReference<>();
    private final AtomicReference<UserShowTracking> persistedTracking = new AtomicReference<>();

    private Users user;
    private Media show;
    private TmdbTvDetailsDTO ongoingShow;

    @BeforeEach
    void setUp() {
        showTrackingService = new ShowTrackingService(
            showCatalogService,
            showStatusCalculator,
            userMediaStatusRepository,
            userShowTrackingRepository
        );

        user = Users.builder().id(1L).username("user").build();
        show = Media.builder().id(10L).tmdbId(999L).type(MediaType.SHOW).title("Example Show").build();
        ongoingShow = TmdbTvDetailsDTO.builder().id(999L).status("Returning Series").build();

        when(showCatalogService.validateShowType(MediaType.SHOW)).thenReturn(MediaType.SHOW);
        when(showCatalogService.findImportedShow(999L)).thenReturn(show);
        when(showCatalogService.ensureBasicShowImported(999L)).thenReturn(show);
        when(showCatalogService.fetchAndRefreshShowDetails(999L, show)).thenReturn(ongoingShow);
        when(showCatalogService.isEndedShow(any())).thenReturn(false);
        when(showCatalogService.isAiredMetadataAvailable(any(), any())).thenReturn(false);
        when(showCatalogService.isFullMetadataAvailable(any(), any())).thenReturn(false);
        when(showCatalogService.getAllCachedEpisodes(show.getId())).thenReturn(List.of());
        when(showCatalogService.isAiredEpisode(any())).thenAnswer(inv -> {
            ShowEpisode episode = inv.getArgument(0);
            return episode.getAirDate() != null && !episode.getAirDate().isAfter(LocalDate.now());
        });

        when(userMediaStatusRepository.findByUserAndMedia(user, show)).thenAnswer(inv -> Optional.ofNullable(persistedStatus.get()));
        when(userMediaStatusRepository.save(any(UserMediaStatus.class))).thenAnswer(inv -> {
            UserMediaStatus status = inv.getArgument(0);
            persistedStatus.set(status);
            return status;
        });

        when(userShowTrackingRepository.findByUserAndMedia(user, show)).thenAnswer(inv -> Optional.ofNullable(persistedTracking.get()));
        when(userShowTrackingRepository.findWithEpisodeWatchesByUserAndMedia(user, show)).thenAnswer(inv -> Optional.ofNullable(persistedTracking.get()));
        when(userShowTrackingRepository.save(any(UserShowTracking.class))).thenAnswer(inv -> {
            UserShowTracking tracking = inv.getArgument(0);
            persistedTracking.set(tracking);
            return tracking;
        });
    }

    @Test
    void getShowTracking_whenNoTrackingRowExists_returnsNoneWithoutPersistingAnything() {
        when(showCatalogService.findImportedShow(999L)).thenReturn(null);

        ShowTrackingDTO result = showTrackingService.getShowTracking(user, 999L, MediaType.SHOW);

        assertEquals(WatchStatus.NONE, result.getStatus());
        assertEquals(0, result.getEpisodesWatchedCount());
        assertNull(persistedTracking.get());
        verify(userShowTrackingRepository, never()).save(any());
    }

    @Test
    void setShowStatus_toWatch_clearsEpisodeWatchesAndPointer() {
        UserShowTracking existing = UserShowTracking.builder()
            .user(user)
            .media(show)
            .status(WatchStatus.WATCHING)
            .watchPositionSeason(2)
            .watchPositionEpisode(3)
            .episodeWatches(new java.util.ArrayList<>(List.of(
                com.project.watchmate.Models.UserEpisodeWatch.builder()
                    .userShowTracking(null)
                    .seasonNumber(1)
                    .episodeNumber(1)
                    .watchedAt(LocalDateTime.now())
                    .build()
            )))
            .build();
        persistedTracking.set(existing);

        ShowTrackingStatusDTO result = showTrackingService.setShowStatus(
            user,
            999L,
            MediaType.SHOW,
            UpdateWatchStatusRequestDTO.builder().status("TO_WATCH").build()
        );

        assertEquals(WatchStatus.TO_WATCH, result.getStatus());
        assertEquals(WatchStatus.TO_WATCH, persistedTracking.get().getStatus());
        assertTrue(persistedTracking.get().getEpisodeWatches().isEmpty());
        assertNull(persistedTracking.get().getWatchPositionSeason());
        assertNull(persistedTracking.get().getWatchPositionEpisode());
    }

    @Test
    void setShowStatus_watching_createsZeroEpisodeTracking() {
        ShowTrackingStatusDTO result = showTrackingService.setShowStatus(
            user,
            999L,
            MediaType.SHOW,
            UpdateWatchStatusRequestDTO.builder().status("WATCHING").build()
        );

        assertEquals(WatchStatus.WATCHING, result.getStatus());
        assertEquals(WatchStatus.WATCHING, persistedTracking.get().getStatus());
        assertTrue(persistedTracking.get().getEpisodeWatches().isEmpty());
        assertEquals(0, persistedTracking.get().getEpisodesWatchedCount());
    }

    @Test
    void setShowStatus_upToDate_whenMetadataMissing_throwsControlledException() {
        when(showCatalogService.requireAiredEligibleEpisodesFromCache(show, ongoingShow))
            .thenThrow(new ShowMetadataSyncRequiredException("sync required"));

        ShowMetadataSyncRequiredException exception = assertThrows(
            ShowMetadataSyncRequiredException.class,
            () -> showTrackingService.setShowStatus(
                user,
                999L,
                MediaType.SHOW,
                UpdateWatchStatusRequestDTO.builder().status("UP_TO_DATE").build()
            )
        );

        assertEquals("sync required", exception.getMessage());
    }

    @Test
    void updateWatchPosition_withoutBackfill_setsManualPointerAndWatchingStatus() {
        ShowEpisode pointerEpisode = ShowEpisode.builder()
            .media(show)
            .seasonNumber(2)
            .episodeNumber(1)
            .airDate(LocalDate.of(2020, 2, 1))
            .build();
        when(showCatalogService.requireEpisodeFromCachedSeason(show, 999L, 2, 1)).thenReturn(pointerEpisode);

        ShowTrackingDTO result = showTrackingService.updateWatchPosition(
            user,
            999L,
            MediaType.SHOW,
            UpdateShowTrackingPositionRequestDTO.builder()
                .watchPositionSeason(2)
                .watchPositionEpisode(1)
                .markPreviousEpisodesWatched(false)
                .build()
        );

        assertEquals(WatchStatus.WATCHING, result.getStatus());
        assertEquals(Integer.valueOf(2), result.getWatchPositionSeason());
        assertEquals(Integer.valueOf(1), result.getWatchPositionEpisode());
        assertEquals(0, result.getEpisodesWatchedCount());
        verify(showCatalogService, never()).requireEligibleEpisodesThroughPointerFromCache(any(), any(), any(), any());
    }
}
