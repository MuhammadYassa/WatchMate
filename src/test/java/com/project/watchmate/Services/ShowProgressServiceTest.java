package com.project.watchmate.Services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import com.project.watchmate.Dto.EpisodeProgressDTO;
import com.project.watchmate.Dto.ShowProgressDTO;
import com.project.watchmate.Dto.TmdbTvDetailsDTO;
import com.project.watchmate.Dto.TmdbTvEpisodeDTO;
import com.project.watchmate.Dto.TmdbTvSeasonDTO;
import com.project.watchmate.Dto.TmdbTvSeasonSummaryDTO;
import com.project.watchmate.Dto.UpdateShowProgressRequestDTO;
import com.project.watchmate.Models.Media;
import com.project.watchmate.Models.MediaType;
import com.project.watchmate.Models.UserEpisodeProgress;
import com.project.watchmate.Models.UserMediaStatus;
import com.project.watchmate.Models.UserShowProgress;
import com.project.watchmate.Models.Users;
import com.project.watchmate.Models.WatchStatus;
import com.project.watchmate.Repositories.UserEpisodeProgressRepository;
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
    private UserEpisodeProgressRepository userEpisodeProgressRepository;

    @Mock
    private TransactionTemplate transactionTemplate;

    @InjectMocks
    private ShowProgressService showProgressService;

    private Users user;
    private Media show;
    private TmdbTvDetailsDTO tvDetails;
    private TmdbTvSeasonDTO seasonOneDetails;
    private TmdbTvSeasonDTO seasonTwoDetails;

    @BeforeEach
    void setUp() {
        user = Users.builder().id(1L).username("user").build();
        show = Media.builder().id(10L).tmdbId(999L).type(MediaType.SHOW).title("Example Show").build();
        tvDetails = TmdbTvDetailsDTO.builder()
            .id(999L)
            .status("Returning Series")
            .numberOfSeasons(2)
            .numberOfEpisodes(4)
            .seasons(List.of(
                TmdbTvSeasonSummaryDTO.builder().seasonNumber(0).episodeCount(1).build(),
                TmdbTvSeasonSummaryDTO.builder().seasonNumber(1).episodeCount(2).build(),
                TmdbTvSeasonSummaryDTO.builder().seasonNumber(2).episodeCount(2).build()))
            .build();
        seasonOneDetails = TmdbTvSeasonDTO.builder()
            .seasonNumber(1)
            .episodes(List.of(
                TmdbTvEpisodeDTO.builder().seasonNumber(1).episodeNumber(1).airDate("2020-01-01").build(),
                TmdbTvEpisodeDTO.builder().seasonNumber(1).episodeNumber(2).airDate("2020-01-08").build()))
            .build();
        seasonTwoDetails = TmdbTvSeasonDTO.builder()
            .seasonNumber(2)
            .episodes(List.of(
                TmdbTvEpisodeDTO.builder().seasonNumber(2).episodeNumber(1).airDate("2020-02-01").build(),
                TmdbTvEpisodeDTO.builder().seasonNumber(2).episodeNumber(2).airDate("2099-02-08").build()))
            .build();

        lenient().when(showMetadataService.validateShowType(MediaType.SHOW)).thenReturn(MediaType.SHOW);
        lenient().when(mediaResolutionService.resolveMediaByTmdbId(999L, MediaType.SHOW)).thenReturn(show);
        lenient().when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<ShowProgressDTO> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });
    }

    @Test
    void updateShowProgress_withoutBackfill_setsWatchPositionWithoutCreatingEpisodeRows() {
        UpdateShowProgressRequestDTO request = UpdateShowProgressRequestDTO.builder()
            .currentSeasonNumber(2)
            .currentEpisodeNumber(1)
            .markPreviousEpisodesWatched(false)
            .build();

        when(tmdbService.fetchTvDetails(999L)).thenReturn(tvDetails);
        when(tmdbService.fetchTvSeasonDetails(999L, 2)).thenReturn(seasonTwoDetails);
        when(userMediaStatusRepository.findByUserAndMedia(user, show)).thenReturn(Optional.empty());
        when(userShowProgressRepository.findWithEpisodeProgressByUserAndMedia(user, show)).thenReturn(Optional.empty());
        when(userMediaStatusRepository.save(any(UserMediaStatus.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userShowProgressRepository.save(any(UserShowProgress.class))).thenAnswer(inv -> inv.getArgument(0));
        when(showStatusCalculator.calculate(any(UserShowProgress.class), eq(tvDetails), isNull())).thenReturn(WatchStatus.NONE);

        ShowProgressDTO result = showProgressService.updateShowProgress(user, 999L, MediaType.SHOW, request);

        assertEquals(WatchStatus.NONE, result.getStatus());
        assertNull(result.getCurrentSeasonNumber());
        assertNull(result.getCurrentEpisodeNumber());
        assertEquals(Integer.valueOf(2), result.getWatchPositionSeason());
        assertEquals(Integer.valueOf(1), result.getWatchPositionEpisode());
        assertEquals(Integer.valueOf(0), result.getEpisodesWatchedCount());
        assertEquals(Integer.valueOf(0), result.getSeasonsCompletedCount());
        assertNotNull(result.getWatchedEpisodes());
        assertEquals(0, result.getWatchedEpisodes().size());
        verify(userEpisodeProgressRepository, never()).saveAll(any());

        InOrder inOrder = inOrder(tmdbService, transactionTemplate);
        inOrder.verify(tmdbService).fetchTvDetails(999L);
        inOrder.verify(tmdbService).fetchTvSeasonDetails(999L, 2);
        inOrder.verify(transactionTemplate).execute(any());
    }

    @Test
    void getShowProgress_whenNothingExists_returnsEmptyProgress() {
        when(userMediaStatusRepository.findByUserAndMedia(user, show)).thenReturn(Optional.empty());
        when(userShowProgressRepository.findWithEpisodeProgressByUserAndMedia(user, show)).thenReturn(Optional.empty());

        ShowProgressDTO result = showProgressService.getShowProgress(user, 999L, MediaType.SHOW);

        assertEquals(999L, result.getTmdbId());
        assertEquals(MediaType.SHOW, result.getType());
        assertEquals(WatchStatus.NONE, result.getStatus());
        assertNull(result.getCurrentSeasonNumber());
        assertNull(result.getCurrentEpisodeNumber());
        assertNull(result.getWatchPositionSeason());
        assertNull(result.getWatchPositionEpisode());
        assertEquals(Integer.valueOf(0), result.getEpisodesWatchedCount());
        assertEquals(Integer.valueOf(0), result.getSeasonsCompletedCount());
        assertFalse(result.getCompleted());
    }

    @Test
    void getShowProgress_whenProgressExists_returnsSortedWatchedEpisodes() {
        UserMediaStatus existingStatus = UserMediaStatus.builder()
            .id(60L)
            .user(user)
            .media(show)
            .status(WatchStatus.WATCHING)
            .build();
        UserShowProgress existingProgress = UserShowProgress.builder()
            .id(61L)
            .user(user)
            .media(show)
            .currentSeasonNumber(2)
            .currentEpisodeNumber(1)
            .watchPositionSeason(2)
            .watchPositionEpisode(2)
            .episodesWatchedCount(2)
            .seasonsCompletedCount(1)
            .episodeProgress(new ArrayList<>())
            .build();
        existingProgress.getEpisodeProgress().add(UserEpisodeProgress.builder()
            .userShowProgress(existingProgress)
            .seasonNumber(2)
            .episodeNumber(1)
            .watched(true)
            .watchedAt(LocalDateTime.of(2026, 1, 2, 8, 0))
            .build());
        existingProgress.getEpisodeProgress().add(UserEpisodeProgress.builder()
            .userShowProgress(existingProgress)
            .seasonNumber(1)
            .episodeNumber(2)
            .watched(true)
            .watchedAt(LocalDateTime.of(2026, 1, 1, 8, 0))
            .build());
        existingProgress.getEpisodeProgress().add(UserEpisodeProgress.builder()
            .userShowProgress(existingProgress)
            .seasonNumber(3)
            .episodeNumber(1)
            .watched(false)
            .build());
        when(userMediaStatusRepository.findByUserAndMedia(user, show)).thenReturn(Optional.of(existingStatus));
        when(userShowProgressRepository.findWithEpisodeProgressByUserAndMedia(user, show)).thenReturn(Optional.of(existingProgress));

        ShowProgressDTO result = showProgressService.getShowProgress(user, 999L, MediaType.SHOW);

        assertEquals(WatchStatus.WATCHING, result.getStatus());
        assertEquals(Integer.valueOf(2), result.getEpisodesWatchedCount());
        assertEquals(Integer.valueOf(2), result.getWatchPositionSeason());
        assertEquals(Integer.valueOf(2), result.getWatchPositionEpisode());
        assertEquals(2, result.getWatchedEpisodes().size());
        assertEquals(Integer.valueOf(1), result.getWatchedEpisodes().get(0).getSeasonNumber());
        assertEquals(Integer.valueOf(2), result.getWatchedEpisodes().get(0).getEpisodeNumber());
        assertEquals(Integer.valueOf(2), result.getWatchedEpisodes().get(1).getSeasonNumber());
        assertEquals(Integer.valueOf(1), result.getWatchedEpisodes().get(1).getEpisodeNumber());
    }

    @Test
    void updateShowProgress_withBackfill_insertsMissingRowsAndRecalculatesFromRows() {
        UpdateShowProgressRequestDTO request = UpdateShowProgressRequestDTO.builder()
            .currentSeasonNumber(2)
            .currentEpisodeNumber(1)
            .markPreviousEpisodesWatched(true)
            .build();

        when(tmdbService.fetchTvDetails(999L)).thenReturn(tvDetails);
        when(tmdbService.fetchTvSeasonDetails(999L, 2)).thenReturn(seasonTwoDetails);
        when(userMediaStatusRepository.findByUserAndMedia(user, show)).thenReturn(Optional.empty());
        when(userShowProgressRepository.findWithEpisodeProgressByUserAndMedia(user, show)).thenReturn(Optional.empty());
        when(userMediaStatusRepository.save(any(UserMediaStatus.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userShowProgressRepository.save(any(UserShowProgress.class))).thenAnswer(inv -> inv.getArgument(0));
        when(showStatusCalculator.calculate(any(UserShowProgress.class), eq(tvDetails), isNull())).thenReturn(WatchStatus.WATCHING);

        ShowProgressDTO result = showProgressService.updateShowProgress(user, 999L, MediaType.SHOW, request);

        assertEquals(WatchStatus.WATCHING, result.getStatus());
        assertEquals(Integer.valueOf(2), result.getCurrentSeasonNumber());
        assertEquals(Integer.valueOf(1), result.getCurrentEpisodeNumber());
        assertEquals(Integer.valueOf(2), result.getWatchPositionSeason());
        assertEquals(Integer.valueOf(1), result.getWatchPositionEpisode());
        assertEquals(Integer.valueOf(3), result.getEpisodesWatchedCount());
        assertEquals(Integer.valueOf(1), result.getSeasonsCompletedCount());
        assertEquals(3, result.getWatchedEpisodes().size());
    }

    @Test
    void markEpisodeWatched_whenWatchedTrue_recalculatesFromEpisodeRows() {
        UserMediaStatus existingStatus = UserMediaStatus.builder()
            .id(50L)
            .user(user)
            .media(show)
            .status(WatchStatus.NONE)
            .build();
        UserShowProgress existingProgress = UserShowProgress.builder()
            .id(51L)
            .user(user)
            .media(show)
            .episodeProgress(new ArrayList<>())
            .build();

        when(tmdbService.fetchTvDetails(999L)).thenReturn(tvDetails);
        when(tmdbService.fetchTvSeasonDetails(999L, 1)).thenReturn(seasonOneDetails);
        when(userMediaStatusRepository.findByUserAndMedia(user, show)).thenReturn(Optional.of(existingStatus));
        when(userShowProgressRepository.findWithEpisodeProgressByUserAndMedia(user, show)).thenReturn(Optional.of(existingProgress));
        when(userMediaStatusRepository.save(any(UserMediaStatus.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userShowProgressRepository.save(any(UserShowProgress.class))).thenAnswer(inv -> inv.getArgument(0));
        when(showStatusCalculator.calculate(any(UserShowProgress.class), eq(tvDetails), isNull())).thenReturn(WatchStatus.WATCHING);

        ShowProgressDTO result = showProgressService.markEpisodeWatched(user, 999L, MediaType.SHOW, 1, 1, true);

        assertEquals(WatchStatus.WATCHING, result.getStatus());
        assertEquals(Integer.valueOf(1), result.getCurrentSeasonNumber());
        assertEquals(Integer.valueOf(1), result.getCurrentEpisodeNumber());
        assertEquals(Integer.valueOf(1), result.getEpisodesWatchedCount());
        assertEquals(Integer.valueOf(0), result.getSeasonsCompletedCount());
        assertEquals(1, result.getWatchedEpisodes().size());
    }

    @Test
    void markEpisodeWatched_whenWatchedFalse_clearsExistingEpisodeProgress() {
        UserMediaStatus existingStatus = UserMediaStatus.builder()
            .id(51L)
            .user(user)
            .media(show)
            .status(WatchStatus.WATCHING)
            .build();
        UserShowProgress existingProgress = UserShowProgress.builder()
            .id(52L)
            .user(user)
            .media(show)
            .currentSeasonNumber(1)
            .currentEpisodeNumber(1)
            .episodesWatchedCount(1)
            .seasonsCompletedCount(0)
            .episodeProgress(new ArrayList<>())
            .build();
        UserEpisodeProgress progress = UserEpisodeProgress.builder()
            .userShowProgress(existingProgress)
            .seasonNumber(1)
            .episodeNumber(1)
            .watched(true)
            .watchedAt(LocalDateTime.of(2026, 1, 1, 9, 0))
            .build();
        existingProgress.getEpisodeProgress().add(progress);

        when(tmdbService.fetchTvDetails(999L)).thenReturn(tvDetails);
        when(tmdbService.fetchTvSeasonDetails(999L, 1)).thenReturn(seasonOneDetails);
        when(userMediaStatusRepository.findByUserAndMedia(user, show)).thenReturn(Optional.of(existingStatus));
        when(userShowProgressRepository.findWithEpisodeProgressByUserAndMedia(user, show)).thenReturn(Optional.of(existingProgress));
        when(userMediaStatusRepository.save(any(UserMediaStatus.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userShowProgressRepository.save(any(UserShowProgress.class))).thenAnswer(inv -> inv.getArgument(0));
        when(showStatusCalculator.calculate(any(UserShowProgress.class), eq(tvDetails), isNull())).thenReturn(WatchStatus.NONE);

        ShowProgressDTO result = showProgressService.markEpisodeWatched(user, 999L, MediaType.SHOW, 1, 1, false);

        assertEquals(WatchStatus.NONE, result.getStatus());
        assertNull(result.getCurrentSeasonNumber());
        assertNull(result.getCurrentEpisodeNumber());
        assertEquals(Integer.valueOf(0), result.getEpisodesWatchedCount());
        assertEquals(0, result.getWatchedEpisodes().size());
        assertFalse(progress.isWatched());
        assertNull(progress.getWatchedAt());
    }

    @Test
    void getWatchedEpisodes_returnsOnlyWatchedRowsInOrder() {
        UserShowProgress existingProgress = UserShowProgress.builder()
            .id(61L)
            .user(user)
            .media(show)
            .episodeProgress(new ArrayList<>())
            .build();
        existingProgress.getEpisodeProgress().add(UserEpisodeProgress.builder()
            .userShowProgress(existingProgress)
            .seasonNumber(2)
            .episodeNumber(3)
            .watched(true)
            .watchedAt(LocalDateTime.of(2026, 1, 3, 10, 0))
            .build());
        existingProgress.getEpisodeProgress().add(UserEpisodeProgress.builder()
            .userShowProgress(existingProgress)
            .seasonNumber(1)
            .episodeNumber(4)
            .watched(true)
            .watchedAt(LocalDateTime.of(2026, 1, 1, 10, 0))
            .build());
        existingProgress.getEpisodeProgress().add(UserEpisodeProgress.builder()
            .userShowProgress(existingProgress)
            .seasonNumber(2)
            .episodeNumber(4)
            .watched(false)
            .build());
        when(userShowProgressRepository.findWithEpisodeProgressByUserAndMedia(user, show)).thenReturn(Optional.of(existingProgress));

        List<EpisodeProgressDTO> result = showProgressService.getWatchedEpisodes(user, 999L, MediaType.SHOW);

        assertEquals(2, result.size());
        assertEquals(Integer.valueOf(1), result.get(0).getSeasonNumber());
        assertEquals(Integer.valueOf(4), result.get(0).getEpisodeNumber());
        assertEquals(Integer.valueOf(2), result.get(1).getSeasonNumber());
        assertEquals(Integer.valueOf(3), result.get(1).getEpisodeNumber());
    }

    @Test
    void markSeasonWatched_whenWatchedTrue_marksAllEpisodesInSeason() {
        when(tmdbService.fetchTvDetails(999L)).thenReturn(tvDetails);
        when(tmdbService.fetchTvSeasonDetails(999L, 1)).thenReturn(seasonOneDetails);
        when(userMediaStatusRepository.findByUserAndMedia(user, show)).thenReturn(Optional.empty());
        when(userShowProgressRepository.findWithEpisodeProgressByUserAndMedia(user, show)).thenReturn(Optional.empty());
        when(userMediaStatusRepository.save(any(UserMediaStatus.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userShowProgressRepository.save(any(UserShowProgress.class))).thenAnswer(inv -> inv.getArgument(0));
        when(showStatusCalculator.calculate(any(UserShowProgress.class), eq(tvDetails), isNull())).thenReturn(WatchStatus.WATCHING);

        ShowProgressDTO result = showProgressService.markSeasonWatched(user, 999L, 1, true);

        assertEquals(WatchStatus.WATCHING, result.getStatus());
        assertEquals(Integer.valueOf(1), result.getCurrentSeasonNumber());
        assertEquals(Integer.valueOf(2), result.getCurrentEpisodeNumber());
        assertEquals(Integer.valueOf(2), result.getEpisodesWatchedCount());
        assertEquals(Integer.valueOf(1), result.getSeasonsCompletedCount());
        assertEquals(2, result.getWatchedEpisodes().size());

        InOrder inOrder = inOrder(tmdbService, transactionTemplate);
        inOrder.verify(tmdbService).fetchTvDetails(999L);
        inOrder.verify(tmdbService).fetchTvSeasonDetails(999L, 1);
        inOrder.verify(transactionTemplate).execute(any());
    }
}
