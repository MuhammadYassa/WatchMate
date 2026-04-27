package com.project.watchmate.Services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.project.watchmate.Dto.UpdateWatchStatusRequestDTO;
import com.project.watchmate.Dto.UserMediaStatusDTO;
import com.project.watchmate.Exception.InvalidWatchStatusException;
import com.project.watchmate.Models.Media;
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

    @InjectMocks
    private StatusService statusService;

    private Users user;
    private Media media;
    private static final Long TMDB_ID = 100L;

    @BeforeEach
    void setUp() {
        user = Users.builder().id(1L).username("user").build();
        media = Media.builder().id(1L).tmdbId(TMDB_ID).title("Movie").build();
    }

    @Nested
    @DisplayName("Update Watch Status Tests")
    class UpdateWatchStatusTests {

        @Test
        void updateWatchStatus_WhenValid_SavesAndReturnsDto() {
            UpdateWatchStatusRequestDTO request = UpdateWatchStatusRequestDTO.builder().tmdbId(TMDB_ID).type("MOVIE").status("WATCHED").build();
            when(mediaResolutionService.resolveMediaByTmdbId(TMDB_ID, "MOVIE")).thenReturn(media);
            UserMediaStatus status = UserMediaStatus.builder().user(user).media(media).status(WatchStatus.NONE).build();
            when(userMediaStatusRepository.findByUserAndMedia(user, media)).thenReturn(Optional.of(status));
            when(userMediaStatusRepository.save(any(UserMediaStatus.class))).thenReturn(status);

            UserMediaStatusDTO result = statusService.updateWatchStatus(user, request);

            assertEquals(TMDB_ID, result.getTmdbId());
            assertEquals(WatchStatus.WATCHED, result.getStatus());
            verify(userMediaStatusRepository).save(status);
        }

        @Test
        void updateWatchStatus_WhenNoExistingStatus_CreatesNewAndSaves() {
            UpdateWatchStatusRequestDTO request = UpdateWatchStatusRequestDTO.builder().tmdbId(TMDB_ID).type("MOVIE").status("TO_WATCH").build();
            when(mediaResolutionService.resolveMediaByTmdbId(TMDB_ID, "MOVIE")).thenReturn(media);
            when(userMediaStatusRepository.findByUserAndMedia(user, media)).thenReturn(Optional.empty());

            UserMediaStatusDTO result = statusService.updateWatchStatus(user, request);

            assertEquals(TMDB_ID, result.getTmdbId());
            assertEquals(WatchStatus.TO_WATCH, result.getStatus());
            verify(userMediaStatusRepository).save(any(UserMediaStatus.class));
        }

        @Test
        void updateWatchStatus_WhenStatusNull_ThrowsInvalidWatchStatusException() {
            UpdateWatchStatusRequestDTO request = UpdateWatchStatusRequestDTO.builder().tmdbId(TMDB_ID).status(null).build();

            InvalidWatchStatusException e = assertThrows(InvalidWatchStatusException.class,
                () -> statusService.updateWatchStatus(user, request));
            assertEquals("Status must be provided.", e.getMessage());
            verify(mediaResolutionService, never()).resolveMediaByTmdbId(anyLong(), anyString());
        }

        @Test
        void updateWatchStatus_WhenStatusInvalid_ThrowsInvalidWatchStatusException() {
            UpdateWatchStatusRequestDTO request = UpdateWatchStatusRequestDTO.builder().tmdbId(TMDB_ID).status("INVALID").build();

            InvalidWatchStatusException e = assertThrows(InvalidWatchStatusException.class,
                () -> statusService.updateWatchStatus(user, request));
            assertEquals("Invalid status. Allowed: TO_WATCH, WATCHING, WATCHED, NONE", e.getMessage());
            verify(mediaResolutionService, never()).resolveMediaByTmdbId(anyLong(), anyString());
        }
    }
}
