package com.project.watchmate.Services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.project.watchmate.Dto.CalendarResponseDTO;
import com.project.watchmate.Dto.ContinueWatchingResponseDTO;
import com.project.watchmate.Dto.UpcomingEpisodesResponseDTO;
import com.project.watchmate.Mappers.DashboardMapper;
import com.project.watchmate.Models.Media;
import com.project.watchmate.Models.MediaType;
import com.project.watchmate.Models.UserShowTracking;
import com.project.watchmate.Models.Users;
import com.project.watchmate.Models.WatchStatus;
import com.project.watchmate.Repositories.UserMediaStatusRepository;
import com.project.watchmate.Repositories.UserShowTrackingRepository;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private UserShowTrackingRepository userShowTrackingRepository;

    @Mock
    private UserMediaStatusRepository userMediaStatusRepository;

    private final DashboardMapper dashboardMapper = new DashboardMapper();

    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardService(userMediaStatusRepository, userShowTrackingRepository, dashboardMapper);
    }

    @Test
    void getContinueWatching_onlyRequestsWatchingStatuses() {
        Users user = Users.builder().id(99L).username("continue-user").build();
        UserShowTracking watchingStatus = UserShowTracking.builder()
            .user(user)
            .status(WatchStatus.WATCHING)
            .media(Media.builder().tmdbId(9100L).title("Active Show").type(MediaType.SHOW).build())
            .build();
        watchingStatus.setWatchPositionSeason(1);
        watchingStatus.setWatchPositionEpisode(2);

        when(userShowTrackingRepository.findContinueWatchingByUser(
            eq(user),
            eq(List.of(WatchStatus.WATCHING)),
            any()))
            .thenReturn(List.of(watchingStatus));
        when(userMediaStatusRepository.findContinueWatchingMoviesByUser(
            eq(user),
            eq(List.of(WatchStatus.WATCHING)),
            any()))
            .thenReturn(List.of());

        ContinueWatchingResponseDTO result = dashboardService.getContinueWatching(user, 10);

        assertEquals(1, result.getItems().size());
        assertEquals(WatchStatus.WATCHING, result.getItems().get(0).getWatchStatus());
        verify(userShowTrackingRepository).findContinueWatchingByUser(
            eq(user),
            eq(List.of(WatchStatus.WATCHING)),
            any());
        verify(userMediaStatusRepository).findContinueWatchingMoviesByUser(
            eq(user),
            eq(List.of(WatchStatus.WATCHING)),
            any());
    }

    @Test
    void getUpcomingEpisodesForUser_mapsLocalSnapshotFields_andCalculatesDaysUntilAirDate() {
        Users user = Users.builder().id(1L).username("dashboard-user").build();
        LocalDate firstAirDate = LocalDate.now().plusDays(2);
        LocalDate secondAirDate = LocalDate.now().plusDays(7);

        UserShowTracking firstStatus = UserShowTracking.builder()
            .user(user)
            .status(WatchStatus.WATCHING)
            .media(Media.builder()
                .tmdbId(8201L)
                .title("Sooner Show")
                .type(MediaType.SHOW)
                .posterPath("/soon.jpg")
                .backdropPath("/soon-bg.jpg")
                .nextEpisodeSeasonNumber(1)
                .nextEpisodeEpisodeNumber(2)
                .nextEpisodeName("Soon Episode")
                .nextEpisodeAirDate(firstAirDate)
                .tmdbShowStatus("Returning Series")
                .build())
            .build();
        UserShowTracking secondStatus = UserShowTracking.builder()
            .user(user)
            .status(WatchStatus.TO_WATCH)
            .media(Media.builder()
                .tmdbId(8202L)
                .title("Later Show")
                .type(MediaType.SHOW)
                .nextEpisodeAirDate(secondAirDate)
                .build())
            .build();

        when(userShowTrackingRepository.findUpcomingEpisodesByUser(
            eq(user),
            eq(List.of(WatchStatus.WATCHING, WatchStatus.UP_TO_DATE, WatchStatus.TO_WATCH)),
            any(LocalDate.class)))
            .thenReturn(List.of(firstStatus, secondStatus));

        UpcomingEpisodesResponseDTO result = dashboardService.getUpcomingEpisodesForUser(user);

        assertEquals(2, result.getItems().size());
        assertEquals(8201L, result.getItems().get(0).getTmdbId());
        assertEquals("Soon Episode", result.getItems().get(0).getNextEpisodeName());
        assertEquals(firstAirDate, result.getItems().get(0).getNextEpisodeAirDate());
        assertEquals(2L, result.getItems().get(0).getDaysUntilAirDate());
        assertEquals(8202L, result.getItems().get(1).getTmdbId());
        assertEquals(7L, result.getItems().get(1).getDaysUntilAirDate());

        verify(userShowTrackingRepository).findUpcomingEpisodesByUser(
            eq(user),
            eq(List.of(WatchStatus.WATCHING, WatchStatus.UP_TO_DATE, WatchStatus.TO_WATCH)),
            any(LocalDate.class));
    }

        @Test
    void getUpcomingEpisodesForUser_returnsEmptyItems_whenNothingIsTracked() {
        Users user = Users.builder().id(2L).username("dashboard-empty").build();
        when(userShowTrackingRepository.findUpcomingEpisodesByUser(
            eq(user),
            eq(List.of(WatchStatus.WATCHING, WatchStatus.UP_TO_DATE, WatchStatus.TO_WATCH)),
            any(LocalDate.class)))
            .thenReturn(List.of());

        UpcomingEpisodesResponseDTO result = dashboardService.getUpcomingEpisodesForUser(user);

        assertTrue(result.getItems().isEmpty());
    }

    @Test
    void getCalendarForUser_mapsLocalSnapshotFieldsWithinRequestedRange() {
        Users user = Users.builder().id(3L).username("calendar-user").build();
        LocalDate from = LocalDate.of(2099, 1, 1);
        LocalDate to = LocalDate.of(2099, 1, 31);

        UserShowTracking status = UserShowTracking.builder()
            .user(user)
            .status(WatchStatus.WATCHING)
            .media(Media.builder()
                .tmdbId(8301L)
                .title("Calendar Show")
                .type(MediaType.SHOW)
                .posterPath("/calendar.jpg")
                .backdropPath("/calendar-bg.jpg")
                .nextEpisodeSeasonNumber(4)
                .nextEpisodeEpisodeNumber(8)
                .nextEpisodeName("Calendar Episode")
                .nextEpisodeAirDate(LocalDate.of(2099, 1, 15))
                .tmdbShowStatus("Returning Series")
                .build())
            .build();

        when(userShowTrackingRepository.findCalendarItemsByUser(
            user,
            List.of(WatchStatus.WATCHING, WatchStatus.UP_TO_DATE, WatchStatus.TO_WATCH),
            from,
            to))
            .thenReturn(List.of(status));

        CalendarResponseDTO result = dashboardService.getCalendarForUser(user, from, to);

        assertEquals(1, result.getItems().size());
        assertEquals(LocalDate.of(2099, 1, 15), result.getItems().get(0).getAirDate());
        assertEquals(8301L, result.getItems().get(0).getTmdbId());
        assertEquals("Calendar Episode", result.getItems().get(0).getEpisodeTitle());
        assertEquals(WatchStatus.WATCHING, result.getItems().get(0).getWatchStatus());
    }

    @Test
    void getCalendarForUser_whenFromAfterTo_throwsIllegalArgumentException() {
        Users user = Users.builder().id(4L).username("calendar-invalid").build();

        org.junit.jupiter.api.Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> dashboardService.getCalendarForUser(
                user,
                LocalDate.of(2099, 2, 1),
                LocalDate.of(2099, 1, 1))
        );
    }
}
