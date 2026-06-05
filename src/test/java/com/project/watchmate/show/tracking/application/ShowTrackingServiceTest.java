package com.project.watchmate.show.tracking.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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

import com.project.watchmate.show.catalog.application.ShowCatalogService;
import com.project.watchmate.show.catalog.application.ShowHydrationProperties;
import com.project.watchmate.show.jobs.application.ShowTrackingJobProperties;
import com.project.watchmate.show.jobs.application.ShowTrackingJobService;
import com.project.watchmate.show.tracking.dto.ShowTrackingDTO;
import com.project.watchmate.show.jobs.dto.ShowTrackingJobDTO;
import com.project.watchmate.show.tracking.dto.ShowTrackingStatusDTO;
import com.project.watchmate.media.tmdb.dto.TmdbTvDetailsDTO;
import com.project.watchmate.show.tracking.dto.UpdateShowTrackingPositionRequestDTO;
import com.project.watchmate.common.dto.UpdateWatchStatusRequestDTO;
import com.project.watchmate.media.catalog.domain.Media;
import com.project.watchmate.media.catalog.domain.MediaType;
import com.project.watchmate.media.catalog.domain.ShowEpisode;
import com.project.watchmate.show.jobs.domain.ShowTrackingJobStatus;
import com.project.watchmate.show.jobs.domain.ShowTrackingJobType;
import com.project.watchmate.movie.tracking.domain.UserMediaStatus;
import com.project.watchmate.show.tracking.domain.UserShowTracking;
import com.project.watchmate.user.domain.Users;
import com.project.watchmate.media.catalog.domain.WatchStatus;
import com.project.watchmate.movie.tracking.persistence.UserMediaStatusRepository;
import com.project.watchmate.show.tracking.persistence.UserShowTrackingRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ShowTrackingServiceTest {

    @Mock
    private ShowCatalogService showCatalogService;

    @Mock
    private UserMediaStatusRepository userMediaStatusRepository;

    @Mock
    private UserShowTrackingRepository userShowTrackingRepository;

    @Mock
    private ShowTrackingJobService showTrackingJobService;

    @Mock
    private ShowHydrationProperties showHydrationProperties;

    @Mock
    private ShowTrackingJobProperties showTrackingJobProperties;

    private final ShowStatusCalculator showStatusCalculator = new ShowStatusCalculator();

    private ShowTrackingWriteSupport showTrackingWriteSupport;

    private ShowTrackingService showTrackingService;

    private final AtomicReference<UserMediaStatus> persistedStatus = new AtomicReference<>();
    private final AtomicReference<UserShowTracking> persistedTracking = new AtomicReference<>();

    private Users user;
    private Media show;
    private TmdbTvDetailsDTO ongoingShow;

    @BeforeEach
    void setUp() {
        showTrackingWriteSupport = new ShowTrackingWriteSupport(
            showStatusCalculator,
            userMediaStatusRepository,
            userShowTrackingRepository
        );
        showTrackingService = new ShowTrackingService(
            showCatalogService,
            showStatusCalculator,
            showTrackingWriteSupport,
            showTrackingJobService,
            showHydrationProperties,
            showTrackingJobProperties,
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
        when(showHydrationProperties.getMaxSynchronousMissingSeasons()).thenReturn(3);
        when(showHydrationProperties.getMaxSynchronousEpisodes()).thenReturn(100);
        when(showTrackingJobProperties.isEnabled()).thenReturn(true);
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
                com.project.watchmate.show.tracking.domain.UserEpisodeWatch.builder()
                    .userShowTracking(null)
                    .seasonNumber(1)
                    .episodeNumber(1)
                    .watchedAt(LocalDateTime.now())
                    .build()
            )))
            .build();
        persistedTracking.set(existing);

        ShowTrackingActionResult<ShowTrackingStatusDTO> result = showTrackingService.setShowStatus(
            user,
            999L,
            MediaType.SHOW,
            UpdateWatchStatusRequestDTO.builder().status("TO_WATCH").build()
        );

        assertFalse(result.isAccepted());
        assertEquals(WatchStatus.TO_WATCH, result.completedBody().getStatus());
        assertEquals(WatchStatus.TO_WATCH, persistedTracking.get().getStatus());
        assertTrue(persistedTracking.get().getEpisodeWatches().isEmpty());
        assertNull(persistedTracking.get().getWatchPositionSeason());
        assertNull(persistedTracking.get().getWatchPositionEpisode());
    }

    @Test
    void setShowStatus_watching_createsZeroEpisodeTracking() {
        ShowTrackingActionResult<ShowTrackingStatusDTO> result = showTrackingService.setShowStatus(
            user,
            999L,
            MediaType.SHOW,
            UpdateWatchStatusRequestDTO.builder().status("WATCHING").build()
        );

        assertFalse(result.isAccepted());
        assertEquals(WatchStatus.WATCHING, result.completedBody().getStatus());
        assertEquals(WatchStatus.WATCHING, persistedTracking.get().getStatus());
        assertTrue(persistedTracking.get().getEpisodeWatches().isEmpty());
        assertEquals(0, persistedTracking.get().getEpisodesWatchedCount());
    }

    @Test
    void setShowStatus_upToDate_whenMetadataMissing_returnsAcceptedJob() {
        when(showCatalogService.getRequiredAiredSeasonNumbers(ongoingShow)).thenReturn(List.of(1, 2));
        when(showCatalogService.canHydrateSynchronously(show, ongoingShow, List.of(1, 2), showHydrationProperties))
            .thenReturn(false);
        when(showTrackingJobService.createOrReuseMarkUpToDateJob(user, show, 2))
            .thenReturn(ShowTrackingJobDTO.builder()
                .jobId(44L)
                .status(ShowTrackingJobStatus.PENDING)
                .jobType(ShowTrackingJobType.MARK_SHOW_UP_TO_DATE)
                .tmdbId(999L)
                .mediaId(show.getId())
                .build());

        ShowTrackingActionResult<ShowTrackingStatusDTO> result = showTrackingService.setShowStatus(
            user,
            999L,
            MediaType.SHOW,
            UpdateWatchStatusRequestDTO.builder().status("UP_TO_DATE").build()
        );

        assertTrue(result.isAccepted());
        assertEquals(44L, result.acceptedJob().getJobId());
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

        ShowTrackingActionResult<ShowTrackingDTO> result = showTrackingService.updateWatchPosition(
            user,
            999L,
            MediaType.SHOW,
            UpdateShowTrackingPositionRequestDTO.builder()
                .watchPositionSeason(2)
                .watchPositionEpisode(1)
                .markPreviousEpisodesWatched(false)
                .build()
        );

        assertFalse(result.isAccepted());
        assertEquals(WatchStatus.WATCHING, result.completedBody().getStatus());
        assertEquals(Integer.valueOf(2), result.completedBody().getWatchPositionSeason());
        assertEquals(Integer.valueOf(1), result.completedBody().getWatchPositionEpisode());
        assertEquals(0, result.completedBody().getEpisodesWatchedCount());
        verify(showCatalogService, never()).requireEligibleEpisodesThroughPointerFromCache(any(), any(), any(), any());
    }
}






