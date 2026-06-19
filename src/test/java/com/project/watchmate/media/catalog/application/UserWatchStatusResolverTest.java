package com.project.watchmate.media.catalog.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.project.watchmate.media.catalog.domain.Media;
import com.project.watchmate.media.catalog.domain.MediaType;
import com.project.watchmate.media.catalog.domain.WatchStatus;
import com.project.watchmate.movie.tracking.domain.UserMediaStatus;
import com.project.watchmate.movie.tracking.persistence.UserMediaStatusRepository;
import com.project.watchmate.show.tracking.persistence.UserShowTrackingRepository;
import com.project.watchmate.user.domain.Users;

@ExtendWith(MockitoExtension.class)
class UserWatchStatusResolverTest {

    @Mock
    private UserMediaStatusRepository userMediaStatusRepository;

    @Mock
    private UserShowTrackingRepository userShowTrackingRepository;

    @InjectMocks
    private UserWatchStatusResolver userWatchStatusResolver;

    private Users user;
    private Media movie;
    private Media show;

    @BeforeEach
    void setUp() {
        user = Users.builder().id(1L).username("resolver-user").build();
        movie = Media.builder().id(10L).tmdbId(100L).title("Resolver Movie").type(MediaType.MOVIE).build();
        show = Media.builder().id(20L).tmdbId(200L).title("Resolver Show").type(MediaType.SHOW).build();
    }

    @Test
    void resolveWatchStatus_whenMovieRowMissing_returnsNone() {
        when(userMediaStatusRepository.findByUserAndMedia(user, movie)).thenReturn(Optional.empty());

        WatchStatus result = userWatchStatusResolver.resolveWatchStatus(user, movie);

        assertEquals(WatchStatus.NONE, result);
        verify(userMediaStatusRepository).findByUserAndMedia(user, movie);
    }

    @Test
    void resolveWatchStatus_whenMovieRowExists_returnsPersistedStatus() {
        when(userMediaStatusRepository.findByUserAndMedia(user, movie)).thenReturn(Optional.of(
            UserMediaStatus.builder()
                .user(user)
                .media(movie)
                .status(WatchStatus.WATCHED)
                .build()
        ));

        WatchStatus result = userWatchStatusResolver.resolveWatchStatus(user, movie);

        assertEquals(WatchStatus.WATCHED, result);
    }

    @Test
    void resolveWatchStatus_whenShowRowMissing_returnsNone() {
        when(userShowTrackingRepository.findByUserAndMedia(user, show)).thenReturn(Optional.empty());

        WatchStatus result = userWatchStatusResolver.resolveWatchStatus(user, show);

        assertEquals(WatchStatus.NONE, result);
        verify(userShowTrackingRepository).findByUserAndMedia(user, show);
    }

    @Test
    void resolveWatchStatus_whenUserOrMediaMissing_returnsNone() {
        assertEquals(WatchStatus.NONE, userWatchStatusResolver.resolveWatchStatus(null, movie));
        assertEquals(WatchStatus.NONE, userWatchStatusResolver.resolveWatchStatus(user, null));
    }
}
