package com.project.watchmate.Services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.project.watchmate.Dto.UpdateWatchStatusRequestDTO;
import com.project.watchmate.Dto.UserMediaStatusDTO;
import com.project.watchmate.Exception.InvalidWatchStatusException;
import com.project.watchmate.Models.Media;
import com.project.watchmate.Models.MediaType;
import com.project.watchmate.Models.UserMediaStatus;
import com.project.watchmate.Models.Users;
import com.project.watchmate.Models.WatchStatus;
import com.project.watchmate.Repositories.UserMediaStatusRepository;

@ExtendWith(MockitoExtension.class)
class StatusServiceTest {

    @Mock
    private MediaResolutionService mediaResolutionService;

    @Mock
    private UserMediaStatusRepository userMediaStatusRepository;

    @Mock
    private ShowProgressService showProgressService;

    @InjectMocks
    private StatusService statusService;

    private Users user;
    private Media movie;
    private static final Long TMDB_ID = 100L;

    @BeforeEach
    void setUp() {
        user = Users.builder().id(1L).username("user").build();
        movie = Media.builder().id(1L).tmdbId(TMDB_ID).title("Movie").type(MediaType.MOVIE).build();
    }

    @Test
    void updateWatchStatus_whenMovieStatusValid_savesAndReturnsDto() {
        UpdateWatchStatusRequestDTO request = UpdateWatchStatusRequestDTO.builder().status("WATCHED").build();
        when(mediaResolutionService.resolveMediaByTmdbId(TMDB_ID, MediaType.MOVIE)).thenReturn(movie);
        UserMediaStatus status = UserMediaStatus.builder().user(user).media(movie).status(WatchStatus.NONE).build();
        when(userMediaStatusRepository.findByUserAndMedia(user, movie)).thenReturn(Optional.of(status));
        when(userMediaStatusRepository.save(any(UserMediaStatus.class))).thenAnswer(inv -> inv.getArgument(0));

        UserMediaStatusDTO result = statusService.updateWatchStatus(user, TMDB_ID, MediaType.MOVIE, request);

        assertEquals(TMDB_ID, result.getTmdbId());
        assertEquals(WatchStatus.WATCHED, result.getStatus());
        verify(userMediaStatusRepository).save(status);
        verify(showProgressService, never()).updateShowStatus(any(), anyLong(), any(), any());
    }

    @Test
    void updateWatchStatus_whenMovieStatusIsUpToDate_rejectsRequest() {
        UpdateWatchStatusRequestDTO request = UpdateWatchStatusRequestDTO.builder().status("UP_TO_DATE").build();

        InvalidWatchStatusException exception = assertThrows(InvalidWatchStatusException.class,
            () -> statusService.updateWatchStatus(user, TMDB_ID, MediaType.MOVIE, request));

        assertEquals("Invalid status. Allowed: TO_WATCH, WATCHING, WATCHED, NONE", exception.getMessage());
        verify(mediaResolutionService, never()).resolveMediaByTmdbId(anyLong(), any(MediaType.class));
        verify(showProgressService, never()).updateShowStatus(any(), anyLong(), any(), any());
    }

    @Test
    void updateWatchStatus_whenShowStatusRequested_delegatesToShowProgressService() {
        UpdateWatchStatusRequestDTO request = UpdateWatchStatusRequestDTO.builder().status("UP_TO_DATE").build();
        UserMediaStatusDTO delegated = UserMediaStatusDTO.builder()
            .tmdbId(TMDB_ID)
            .status(WatchStatus.UP_TO_DATE)
            .build();
        when(showProgressService.updateShowStatus(user, TMDB_ID, MediaType.SHOW, request)).thenReturn(delegated);

        UserMediaStatusDTO result = statusService.updateWatchStatus(user, TMDB_ID, MediaType.SHOW, request);

        assertEquals(WatchStatus.UP_TO_DATE, result.getStatus());
        verify(showProgressService).updateShowStatus(user, TMDB_ID, MediaType.SHOW, request);
        verify(userMediaStatusRepository, never()).save(any());
    }

    @Test
    void updateWatchStatus_whenStatusNull_throwsInvalidWatchStatusException() {
        UpdateWatchStatusRequestDTO request = UpdateWatchStatusRequestDTO.builder().status(null).build();

        InvalidWatchStatusException exception = assertThrows(InvalidWatchStatusException.class,
            () -> statusService.updateWatchStatus(user, TMDB_ID, MediaType.MOVIE, request));

        assertEquals("Status must be provided.", exception.getMessage());
        verify(mediaResolutionService, never()).resolveMediaByTmdbId(anyLong(), any(MediaType.class));
    }
}
