package com.project.watchmate.show.tracking.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

import com.project.watchmate.common.cache.WatchMateCacheEvictionService;
import com.project.watchmate.show.catalog.application.ShowCatalogService;
import com.project.watchmate.show.catalog.application.ShowHydrationProperties;
import com.project.watchmate.show.jobs.application.ShowTrackingJobProperties;
import com.project.watchmate.show.tracking.dto.ShowTrackingDTO;
import com.project.watchmate.show.jobs.dto.ShowTrackingJobDTO;
import com.project.watchmate.show.tracking.dto.ShowTrackingStatusDTO;
import com.project.watchmate.media.tmdb.dto.TmdbTvDetailsDTO;
import com.project.watchmate.show.tracking.dto.UpdateShowTrackingPositionRequestDTO;
import com.project.watchmate.common.dto.UpdateWatchStatusRequestDTO;
import com.project.watchmate.common.error.TmdbUnavailableException;
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
    private ShowTrackingFallbackPersistenceService showTrackingFallbackPersistenceService;

    @Mock
    private ShowHydrationProperties showHydrationProperties;

    @Mock
    private ShowTrackingJobProperties showTrackingJobProperties;

    @Mock
    private WatchMateCacheEvictionService cacheEvictionService;

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
            userShowTrackingRepository,
            cacheEvictionService
        );
        showTrackingService = new ShowTrackingService(
            showCatalogService,
            showStatusCalculator,
            showTrackingWriteSupport,
            showTrackingFallbackPersistenceService,
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
        verify(cacheEvictionService).evictUserProgressCaches(user.getId());
    }

    @Test
    void setShowStatus_upToDate_whenMetadataMissing_returnsAcceptedJob() {
        when(showCatalogService.getRequiredAiredSeasonNumbers(ongoingShow)).thenReturn(List.of(1, 2));
        when(showCatalogService.canHydrateSynchronously(show, ongoingShow, List.of(1, 2), showHydrationProperties))
            .thenReturn(false);
        when(showTrackingFallbackPersistenceService.ensureTrackingRowAndCreateStatusJob(user, show, WatchStatus.UP_TO_DATE, 2))
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
    void setShowStatus_upToDate_whenSynchronousHydrationHasRecoverableFailure_returnsAcceptedJob() {
        when(showCatalogService.getRequiredAiredSeasonNumbers(ongoingShow)).thenReturn(List.of(1, 2));
        when(showCatalogService.canHydrateSynchronously(show, ongoingShow, List.of(1, 2), showHydrationProperties))
            .thenReturn(true);
        when(showCatalogService.hydrateMissingSeasons(eq(show), eq(999L), eq(List.of(1, 2)), eq(3), eq(100)))
            .thenThrow(new TmdbUnavailableException("TMDB temporarily unavailable"));
        when(showTrackingFallbackPersistenceService.ensureTrackingRowAndCreateStatusJob(user, show, WatchStatus.UP_TO_DATE, 2))
            .thenReturn(ShowTrackingJobDTO.builder()
                .jobId(45L)
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
        assertEquals(45L, result.acceptedJob().getJobId());
    }

    @Test
    void setShowStatus_upToDate_whenSynchronousHydrationHasUnexpectedFailure_rethrows() {
        when(showCatalogService.getRequiredAiredSeasonNumbers(ongoingShow)).thenReturn(List.of(1, 2));
        when(showCatalogService.canHydrateSynchronously(show, ongoingShow, List.of(1, 2), showHydrationProperties))
            .thenReturn(true);
        when(showCatalogService.hydrateMissingSeasons(eq(show), eq(999L), eq(List.of(1, 2)), eq(3), eq(100)))
            .thenThrow(new IllegalStateException("database write failed"));

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> showTrackingService.setShowStatus(
                user,
                999L,
                MediaType.SHOW,
                UpdateWatchStatusRequestDTO.builder().status("UP_TO_DATE").build()
            )
        );

        assertEquals("database write failed", exception.getMessage());
        verify(showTrackingFallbackPersistenceService, never()).ensureTrackingRowAndCreateStatusJob(any(), any(), any(), any());
    }

    @Test
    void updateWatchPosition_replacesEpisodeRowsWithContiguousPrefix() {
        UserShowTracking existing = UserShowTracking.builder()
            .user(user)
            .media(show)
            .status(WatchStatus.WATCHING)
            .watchPositionSeason(1)
            .watchPositionEpisode(5)
            .episodeWatches(new java.util.ArrayList<>(List.of(
                watch(existingTrackingRef(), 1, 1, LocalDateTime.of(2026, 5, 1, 10, 0)),
                watch(existingTrackingRef(), 1, 2, LocalDateTime.of(2026, 5, 1, 11, 0)),
                watch(existingTrackingRef(), 1, 3, LocalDateTime.of(2026, 5, 1, 12, 0)),
                watch(existingTrackingRef(), 1, 4, LocalDateTime.of(2026, 5, 1, 13, 0)),
                watch(existingTrackingRef(), 1, 5, LocalDateTime.of(2026, 5, 1, 14, 0))
            )))
            .build();
        existing.getEpisodeWatches().forEach(row -> row.setUserShowTracking(existing));
        persistedTracking.set(existing);

        ShowEpisode pointerEpisode = airedEpisode(1, 3);
        List<ShowEpisode> targetEpisodes = List.of(
            airedEpisode(1, 1),
            airedEpisode(1, 2),
            airedEpisode(1, 3)
        );
        when(showCatalogService.requireEpisodeFromCachedSeason(show, 999L, 1, 3)).thenReturn(pointerEpisode);
        when(showCatalogService.getRequiredSeasonNumbersThroughPointer(ongoingShow, 1)).thenReturn(List.of(1));
        when(showCatalogService.canHydrateSynchronously(show, ongoingShow, List.of(1), showHydrationProperties)).thenReturn(true);
        when(showCatalogService.requireEligibleEpisodesThroughPointerFromCache(show, ongoingShow, 1, 3)).thenReturn(targetEpisodes);
        when(showCatalogService.getAllCachedEpisodes(show.getId())).thenReturn(targetEpisodes);

        ShowTrackingActionResult<ShowTrackingDTO> result = showTrackingService.updateWatchPosition(
            user,
            999L,
            MediaType.SHOW,
            UpdateShowTrackingPositionRequestDTO.builder()
                .watchPositionSeason(1)
                .watchPositionEpisode(3)
                .build()
        );

        assertFalse(result.isAccepted());
        assertEquals(WatchStatus.WATCHING, result.completedBody().getStatus());
        assertEquals(Integer.valueOf(1), result.completedBody().getWatchPositionSeason());
        assertEquals(Integer.valueOf(3), result.completedBody().getWatchPositionEpisode());
        assertEquals(Integer.valueOf(1), result.completedBody().getLatestWatchedSeason());
        assertEquals(Integer.valueOf(3), result.completedBody().getLatestWatchedEpisode());
        assertEquals(3, result.completedBody().getEpisodesWatchedCount());
        assertEquals(List.of("1x1", "1x2", "1x3"), persistedTracking.get().getEpisodeWatches().stream()
            .sorted(java.util.Comparator.comparing(com.project.watchmate.show.tracking.domain.UserEpisodeWatch::getSeasonNumber)
                .thenComparing(com.project.watchmate.show.tracking.domain.UserEpisodeWatch::getEpisodeNumber))
            .map(row -> row.getSeasonNumber() + "x" + row.getEpisodeNumber())
            .toList());
    }

    @Test
    void updateWatchPosition_whenMetadataMissing_returnsAcceptedProgressJob() {
        ShowEpisode pointerEpisode = airedEpisode(2, 1);
        when(showCatalogService.requireEpisodeFromCachedSeason(show, 999L, 2, 1)).thenReturn(pointerEpisode);
        when(showCatalogService.getRequiredSeasonNumbersThroughPointer(ongoingShow, 2)).thenReturn(List.of(1, 2));
        when(showCatalogService.canHydrateSynchronously(show, ongoingShow, List.of(1, 2), showHydrationProperties))
            .thenReturn(false);
        when(showTrackingFallbackPersistenceService.saveProgressAndCreateJob(
            user,
            show,
            2,
            1,
            List.of(),
            List.of(),
            false,
            2
        )).thenReturn(ShowTrackingJobDTO.builder()
            .jobId(55L)
            .status(ShowTrackingJobStatus.PENDING)
            .jobType(ShowTrackingJobType.SET_SHOW_PROGRESS)
            .tmdbId(999L)
            .mediaId(show.getId())
            .build());

        ShowTrackingActionResult<ShowTrackingDTO> result = showTrackingService.updateWatchPosition(
            user,
            999L,
            MediaType.SHOW,
            UpdateShowTrackingPositionRequestDTO.builder()
                .watchPositionSeason(2)
                .watchPositionEpisode(1)
                .build()
        );

        assertTrue(result.isAccepted());
        assertEquals(55L, result.acceptedJob().getJobId());
    }

    @Test
    void updateWatchPosition_withRecoverableHydrationFailure_returnsAcceptedProgressJob() {
        ShowEpisode pointerEpisode = airedEpisode(2, 1);
        when(showCatalogService.requireEpisodeFromCachedSeason(show, 999L, 2, 1)).thenReturn(pointerEpisode);
        when(showCatalogService.getRequiredSeasonNumbersThroughPointer(ongoingShow, 2)).thenReturn(List.of(1, 2));
        when(showCatalogService.canHydrateSynchronously(show, ongoingShow, List.of(1, 2), showHydrationProperties))
            .thenReturn(true);
        when(showCatalogService.hydrateMissingSeasons(eq(show), eq(999L), eq(List.of(1, 2)), eq(3), eq(100)))
            .thenThrow(new TmdbUnavailableException("TMDB temporarily unavailable"));
        when(showTrackingFallbackPersistenceService.saveProgressAndCreateJob(
            user,
            show,
            2,
            1,
            List.of(),
            List.of(),
            false,
            2
        )).thenReturn(ShowTrackingJobDTO.builder()
            .jobId(56L)
            .status(ShowTrackingJobStatus.PENDING)
            .jobType(ShowTrackingJobType.SET_SHOW_PROGRESS)
            .tmdbId(999L)
            .mediaId(show.getId())
            .build());

        ShowTrackingActionResult<ShowTrackingDTO> result = showTrackingService.updateWatchPosition(
            user,
            999L,
            MediaType.SHOW,
            UpdateShowTrackingPositionRequestDTO.builder()
                .watchPositionSeason(2)
                .watchPositionEpisode(1)
                .build()
        );

        assertTrue(result.isAccepted());
        assertEquals(56L, result.acceptedJob().getJobId());
    }

    private UserShowTracking existingTrackingRef() {
        return persistedTracking.get();
    }

    private ShowEpisode airedEpisode(int seasonNumber, int episodeNumber) {
        return ShowEpisode.builder()
            .media(show)
            .seasonNumber(seasonNumber)
            .episodeNumber(episodeNumber)
            .airDate(LocalDate.of(2020, seasonNumber, Math.min(episodeNumber, 28)))
            .build();
    }

    private com.project.watchmate.show.tracking.domain.UserEpisodeWatch watch(
        UserShowTracking tracking,
        int seasonNumber,
        int episodeNumber,
        LocalDateTime watchedAt
    ) {
        return com.project.watchmate.show.tracking.domain.UserEpisodeWatch.builder()
            .userShowTracking(tracking)
            .seasonNumber(seasonNumber)
            .episodeNumber(episodeNumber)
            .watchedAt(watchedAt)
            .build();
    }
}






