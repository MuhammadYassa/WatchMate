package com.project.watchmate.Services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import com.project.watchmate.Dto.ShowProgressDTO;
import com.project.watchmate.Dto.TmdbTvDetailsDTO;
import com.project.watchmate.Dto.TmdbTvSeasonSummaryDTO;
import com.project.watchmate.Dto.UpdateShowProgressRequestDTO;
import com.project.watchmate.Dto.UpdateWatchStatusRequestDTO;
import com.project.watchmate.Dto.UserMediaStatusDTO;
import com.project.watchmate.Models.Media;
import com.project.watchmate.Models.MediaType;
import com.project.watchmate.Models.ShowEpisode;
import com.project.watchmate.Models.ShowTrackingState;
import com.project.watchmate.Models.UserEpisodeProgress;
import com.project.watchmate.Models.UserMediaStatus;
import com.project.watchmate.Models.UserShowProgress;
import com.project.watchmate.Models.Users;
import com.project.watchmate.Models.WatchStatus;
import com.project.watchmate.Repositories.UserMediaStatusRepository;
import com.project.watchmate.Repositories.UserShowProgressRepository;

@ExtendWith(MockitoExtension.class)
class ShowProgressServiceTest {

    @Mock
    private MediaResolutionService mediaResolutionService;

    @Mock
    private ShowMetadataService showMetadataService;

    @Mock
    private TmdbService tmdbService;

    @Mock
    private ShowStatusCalculator showStatusCalculator;

    @Mock
    private UserMediaStatusRepository userMediaStatusRepository;

    @Mock
    private UserShowProgressRepository userShowProgressRepository;

    @Mock
    private TransactionTemplate transactionTemplate;

    @InjectMocks
    private ShowProgressService showProgressService;

    private final AtomicReference<UserMediaStatus> persistedStatus = new AtomicReference<>();
    private final AtomicReference<UserShowProgress> persistedProgress = new AtomicReference<>();

    private Users user;
    private Media show;
    private TmdbTvDetailsDTO ongoingShow;
    private TmdbTvDetailsDTO endedShow;
    private List<ShowEpisode> cachedEpisodes;

    @BeforeEach
    void setUp() {
        user = Users.builder().id(1L).username("user").build();
        show = Media.builder().id(10L).tmdbId(999L).type(MediaType.SHOW).title("Example Show").build();
        ongoingShow = tvDetails("Returning Series");
        endedShow = tvDetails("Ended");
        cachedEpisodes = List.of(
            episode(1, 1, LocalDate.of(2020, 1, 1)),
            episode(1, 2, LocalDate.of(2020, 1, 8)),
            episode(2, 1, LocalDate.of(2020, 2, 1)),
            episode(2, 2, LocalDate.of(2099, 2, 8))
        );

        lenient().when(showMetadataService.validateShowType(MediaType.SHOW)).thenReturn(MediaType.SHOW);
        lenient().when(mediaResolutionService.resolveMediaByTmdbId(999L, MediaType.SHOW)).thenReturn(show);
        lenient().when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<Object> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });
        lenient().when(tmdbService.fetchTvDetails(999L)).thenReturn(ongoingShow);
        lenient().when(tmdbService.refreshShowSnapshot(eq(show), any(TmdbTvDetailsDTO.class))).thenReturn(show);
        lenient().when(showMetadataService.ensureStandardEpisodesCached(eq(show), eq(999L), any(TmdbTvDetailsDTO.class))).thenReturn(cachedEpisodes);
        ShowStatusCalculator realCalculator = new ShowStatusCalculator();
        lenient().when(showStatusCalculator.calculate(any(), anyInt(), anyInt(), anyInt(), anyBoolean())).thenAnswer(inv ->
            realCalculator.calculate(
                inv.getArgument(0),
                inv.getArgument(1),
                inv.getArgument(2),
                inv.getArgument(3),
                inv.getArgument(4)
            ));

        lenient().when(userMediaStatusRepository.findByUserAndMedia(user, show)).thenAnswer(inv -> Optional.ofNullable(persistedStatus.get()));
        lenient().when(userMediaStatusRepository.save(any(UserMediaStatus.class))).thenAnswer(inv -> {
            UserMediaStatus status = inv.getArgument(0);
            persistedStatus.set(status);
            return status;
        });
        lenient().doAnswer(inv -> {
            persistedStatus.set(null);
            return null;
        }).when(userMediaStatusRepository).delete(any(UserMediaStatus.class));

        lenient().when(userShowProgressRepository.findWithEpisodeProgressByUserAndMedia(user, show))
            .thenAnswer(inv -> Optional.ofNullable(persistedProgress.get()));
        lenient().when(userShowProgressRepository.save(any(UserShowProgress.class))).thenAnswer(inv -> {
            UserShowProgress progress = inv.getArgument(0);
            persistedProgress.set(progress);
            return progress;
        });
        lenient().doAnswer(inv -> {
            persistedProgress.set(null);
            return null;
        }).when(userShowProgressRepository).delete(any(UserShowProgress.class));
    }

    @Test
    void updateShowStatus_toWatch_clearsEpisodeRowsAndSyncsSummary() {
        UserShowProgress existingProgress = UserShowProgress.builder()
            .user(user)
            .media(show)
            .trackingState(ShowTrackingState.WATCHING)
            .episodeProgress(new java.util.ArrayList<>())
            .build();
        existingProgress.getEpisodeProgress().add(UserEpisodeProgress.builder()
            .userShowProgress(existingProgress)
            .seasonNumber(1)
            .episodeNumber(1)
            .watched(true)
            .watchedAt(LocalDateTime.now())
            .build());
        persistedProgress.set(existingProgress);
        persistedStatus.set(UserMediaStatus.builder().user(user).media(show).status(WatchStatus.WATCHING).build());

        UserMediaStatusDTO result = showProgressService.updateShowStatus(
            user,
            999L,
            MediaType.SHOW,
            UpdateWatchStatusRequestDTO.builder().status("TO_WATCH").build()
        );

        assertEquals(WatchStatus.TO_WATCH, result.getStatus());
        assertEquals(WatchStatus.TO_WATCH, persistedStatus.get().getStatus());
        assertEquals(ShowTrackingState.TO_WATCH, persistedProgress.get().getTrackingState());
        assertTrue(persistedProgress.get().getEpisodeProgress().isEmpty());
        assertEquals(0, persistedProgress.get().getEpisodesWatchedCount());
    }

    @Test
    void updateShowStatus_watching_createsZeroEpisodeTrackingWithoutFabricatingEpisodes() {
        UserMediaStatusDTO result = showProgressService.updateShowStatus(
            user,
            999L,
            MediaType.SHOW,
            UpdateWatchStatusRequestDTO.builder().status("WATCHING").build()
        );

        assertEquals(WatchStatus.WATCHING, result.getStatus());
        assertEquals(WatchStatus.WATCHING, persistedStatus.get().getStatus());
        assertEquals(ShowTrackingState.WATCHING, persistedProgress.get().getTrackingState());
        assertTrue(persistedProgress.get().getEpisodeProgress().isEmpty());
        assertEquals(0, persistedProgress.get().getEpisodesWatchedCount());
    }

    @Test
    void updateShowStatus_watchedForOngoingShow_marksAiredEpisodesAndNormalizesToUpToDate() {
        when(tmdbService.fetchTvDetails(999L)).thenReturn(ongoingShow);

        UserMediaStatusDTO result = showProgressService.updateShowStatus(
            user,
            999L,
            MediaType.SHOW,
            UpdateWatchStatusRequestDTO.builder().status("WATCHED").build()
        );

        assertEquals(WatchStatus.UP_TO_DATE, result.getStatus());
        assertEquals(WatchStatus.UP_TO_DATE, persistedStatus.get().getStatus());
        assertEquals(3, persistedProgress.get().getEpisodesWatchedCount());
        assertEquals(3, persistedProgress.get().getEpisodeProgress().size());
        assertFalse(persistedProgress.get().getEpisodeProgress().stream().anyMatch(row -> row.getSeasonNumber() == 2 && row.getEpisodeNumber() == 2));
    }

    @Test
    void updateShowStatus_watchedForEndedShow_marksAllEligibleEpisodesAndReturnsWatched() {
        when(tmdbService.fetchTvDetails(999L)).thenReturn(endedShow);

        UserMediaStatusDTO result = showProgressService.updateShowStatus(
            user,
            999L,
            MediaType.SHOW,
            UpdateWatchStatusRequestDTO.builder().status("WATCHED").build()
        );

        assertEquals(WatchStatus.WATCHED, result.getStatus());
        assertEquals(WatchStatus.WATCHED, persistedStatus.get().getStatus());
        assertEquals(4, persistedProgress.get().getEpisodesWatchedCount());
        assertEquals(4, persistedProgress.get().getEpisodeProgress().size());
    }

    @Test
    void updateShowProgress_withoutBackfill_setsManualWatchingState() {
        ShowProgressDTO result = showProgressService.updateShowProgress(
            user,
            999L,
            MediaType.SHOW,
            UpdateShowProgressRequestDTO.builder()
                .currentSeasonNumber(2)
                .currentEpisodeNumber(1)
                .markPreviousEpisodesWatched(false)
                .build()
        );

        assertEquals(WatchStatus.WATCHING, result.getStatus());
        assertEquals(Integer.valueOf(2), result.getWatchPositionSeason());
        assertEquals(Integer.valueOf(1), result.getWatchPositionEpisode());
        assertEquals(0, persistedProgress.get().getEpisodesWatchedCount());
        assertEquals(ShowTrackingState.WATCHING, persistedProgress.get().getTrackingState());
    }

    @Test
    void markEpisodeWatched_firstEpisodeMovesShowToWatching() {
        ShowProgressDTO result = showProgressService.markEpisodeWatched(user, 999L, MediaType.SHOW, 1, 1, true);

        assertEquals(WatchStatus.WATCHING, result.getStatus());
        assertEquals(Integer.valueOf(1), result.getCurrentSeasonNumber());
        assertEquals(Integer.valueOf(1), result.getCurrentEpisodeNumber());
        assertEquals(1, persistedProgress.get().getEpisodesWatchedCount());
    }

    @Test
    void markEpisodeWatched_unwatchFromUpToDate_downgradesToWatching() {
        when(tmdbService.fetchTvDetails(999L)).thenReturn(ongoingShow);
        showProgressService.updateShowStatus(
            user,
            999L,
            MediaType.SHOW,
            UpdateWatchStatusRequestDTO.builder().status("UP_TO_DATE").build()
        );

        ShowProgressDTO result = showProgressService.markEpisodeWatched(user, 999L, MediaType.SHOW, 2, 1, false);

        assertEquals(WatchStatus.WATCHING, result.getStatus());
        assertEquals(2, persistedProgress.get().getEpisodesWatchedCount());
        assertEquals(WatchStatus.WATCHING, persistedStatus.get().getStatus());
    }

    @Test
    void markSeasonWatched_marksOnlyAiredEligibleEpisodesInSeason() {
        ShowProgressDTO result = showProgressService.markSeasonWatched(user, 999L, 2, true);

        assertEquals(WatchStatus.WATCHING, result.getStatus());
        assertEquals(1, persistedProgress.get().getEpisodesWatchedCount());
        assertEquals(Integer.valueOf(2), persistedProgress.get().getCurrentSeasonNumber());
        assertEquals(Integer.valueOf(1), persistedProgress.get().getCurrentEpisodeNumber());
    }

    @Test
    void updateShowStatus_none_removesSummaryAndProgressRows() {
        showProgressService.updateShowStatus(
            user,
            999L,
            MediaType.SHOW,
            UpdateWatchStatusRequestDTO.builder().status("WATCHING").build()
        );

        UserMediaStatusDTO result = showProgressService.updateShowStatus(
            user,
            999L,
            MediaType.SHOW,
            UpdateWatchStatusRequestDTO.builder().status("NONE").build()
        );

        assertEquals(WatchStatus.NONE, result.getStatus());
        assertNull(persistedStatus.get());
        assertNull(persistedProgress.get());
    }

    private TmdbTvDetailsDTO tvDetails(String status) {
        return TmdbTvDetailsDTO.builder()
            .id(999L)
            .status(status)
            .seasons(List.of(
                TmdbTvSeasonSummaryDTO.builder().seasonNumber(0).episodeCount(1).build(),
                TmdbTvSeasonSummaryDTO.builder().seasonNumber(1).episodeCount(2).build(),
                TmdbTvSeasonSummaryDTO.builder().seasonNumber(2).episodeCount(2).build()
            ))
            .build();
    }

    private ShowEpisode episode(int seasonNumber, int episodeNumber, LocalDate airDate) {
        return ShowEpisode.builder()
            .media(show)
            .seasonNumber(seasonNumber)
            .episodeNumber(episodeNumber)
            .airDate(airDate)
            .build();
    }
}
