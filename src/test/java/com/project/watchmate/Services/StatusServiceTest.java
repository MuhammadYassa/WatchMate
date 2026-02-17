package com.project.watchmate.Services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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
import com.project.watchmate.Exception.MediaNotFoundException;
import com.project.watchmate.Models.Media;
import com.project.watchmate.Models.UserMediaStatus;
import com.project.watchmate.Models.Users;
import com.project.watchmate.Models.WatchStatus;
import com.project.watchmate.Repositories.MediaRepository;
import com.project.watchmate.Repositories.UserMediaStatusRepository;

@ExtendWith(MockitoExtension.class)
class StatusServiceTest {

    @Mock
    private MediaRepository mediaRepository;

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
    @DisplayName("updateWatchStatus")
    class UpdateWatchStatusTests {

        @Test
        void updateWatchStatus_WhenValid_SavesAndReturnsDto() {
            UpdateWatchStatusRequestDTO request = UpdateWatchStatusRequestDTO.builder().tmdbId(TMDB_ID).status("WATCHED").build();
            when(mediaRepository.findByTmdbId(TMDB_ID)).thenReturn(Optional.of(media));
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
            UpdateWatchStatusRequestDTO request = UpdateWatchStatusRequestDTO.builder().tmdbId(TMDB_ID).status("TO_WATCH").build();
            when(mediaRepository.findByTmdbId(TMDB_ID)).thenReturn(Optional.of(media));
            when(userMediaStatusRepository.findByUserAndMedia(user, media)).thenReturn(Optional.empty());

            UserMediaStatusDTO result = statusService.updateWatchStatus(user, request);

            assertEquals(TMDB_ID, result.getTmdbId());
            assertEquals(WatchStatus.TO_WATCH, result.getStatus());
            verify(userMediaStatusRepository).save(any(UserMediaStatus.class));
        }

        @Test
        void updateWatchStatus_WhenMediaNotFound_ThrowsMediaNotFoundException() {
            UpdateWatchStatusRequestDTO request = UpdateWatchStatusRequestDTO.builder().tmdbId(TMDB_ID).status("WATCHED").build();
            when(mediaRepository.findByTmdbId(TMDB_ID)).thenReturn(Optional.empty());

            MediaNotFoundException e = assertThrows(MediaNotFoundException.class, () -> statusService.updateWatchStatus(user, request));

            assertEquals("Media does not exist.", e.getMessage());
            verify(userMediaStatusRepository,never()).save(any(UserMediaStatus.class));
        }

        @Test
        void updateWatchStatus_WhenStatusNull_ThrowsInvalidWatchStatusException() {
            UpdateWatchStatusRequestDTO request = UpdateWatchStatusRequestDTO.builder().tmdbId(TMDB_ID).status(null).build();
            when(mediaRepository.findByTmdbId(TMDB_ID)).thenReturn(Optional.of(media));

            InvalidWatchStatusException e = assertThrows(InvalidWatchStatusException.class,
                () -> statusService.updateWatchStatus(user, request));
            assertEquals("Status must be provided.", e.getMessage());
        }

        @Test
        void updateWatchStatus_WhenStatusInvalid_ThrowsInvalidWatchStatusException() {
            UpdateWatchStatusRequestDTO request = UpdateWatchStatusRequestDTO.builder().tmdbId(TMDB_ID).status("INVALID").build();
            when(mediaRepository.findByTmdbId(TMDB_ID)).thenReturn(Optional.of(media));

            InvalidWatchStatusException e = assertThrows(InvalidWatchStatusException.class,
                () -> statusService.updateWatchStatus(user, request));
            assertEquals("Invalid status. Allowed: TO_WATCH, WATCHING, WATCHED, NONE", e.getMessage());
        }
    }
}
