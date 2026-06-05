package com.project.watchmate.dashboard.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.project.watchmate.common.integration.support.AbstractIntegrationTest;
import com.project.watchmate.media.catalog.domain.Media;
import com.project.watchmate.media.catalog.domain.MediaType;
import com.project.watchmate.movie.tracking.domain.UserMediaStatus;
import com.project.watchmate.show.tracking.domain.UserShowTracking;
import com.project.watchmate.user.domain.Users;
import com.project.watchmate.media.catalog.domain.WatchStatus;

class DashboardContinueWatchingIntegrationTest extends AbstractIntegrationTest {

    @Test
    void continueWatching_returns401_withoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/continue-watching"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("AUTH_FAILED"));
    }

    @Test
    void continueWatching_returnsOnlyUsersActiveItems_andIncludesShowProgressFields() throws Exception {
        Users user = saveUser("dashboard-user", true);
        Users otherUser = saveUser("dashboard-other", true);

        Media show = mediaRepository.save(Media.builder()
            .tmdbId(7001L)
            .title("Dashboard Show")
            .posterPath("/show.jpg")
            .backdropPath("/show-bg.jpg")
            .type(MediaType.SHOW)
            .rating(8.9)
            .nextEpisodeSeasonNumber(2)
            .nextEpisodeEpisodeNumber(4)
            .build());
        Media movie = mediaRepository.save(Media.builder()
            .tmdbId(7002L)
            .title("Dashboard Movie")
            .posterPath("/movie.jpg")
            .backdropPath("/movie-bg.jpg")
            .type(MediaType.MOVIE)
            .rating(7.2)
            .build());
        Media watchedMovie = saveMedia(7003L, "Completed Movie", MediaType.MOVIE);
        Media otherUsersShow = saveMedia(7004L, "Other User Show", MediaType.SHOW);
        Media caughtUpShow = saveMedia(7005L, "Caught Up Show", MediaType.SHOW);

        saveStatus(user, show, WatchStatus.WATCHING, 2, 3,
            LocalDateTime.of(2026, 5, 11, 10, 30),
            LocalDateTime.of(2026, 5, 11, 10, 30));
        saveStatus(user, movie, WatchStatus.WATCHING, null, null,
            null,
            LocalDateTime.of(2026, 5, 10, 9, 0));
        saveStatus(user, watchedMovie, WatchStatus.WATCHED, null, null,
            null,
            LocalDateTime.of(2026, 5, 12, 8, 0));
        saveStatus(user, caughtUpShow, WatchStatus.UP_TO_DATE, 3, 8,
            LocalDateTime.of(2026, 5, 12, 9, 0),
            LocalDateTime.of(2026, 5, 12, 9, 0));
        saveStatus(otherUser, otherUsersShow, WatchStatus.WATCHING, 1, 2,
            LocalDateTime.of(2026, 5, 12, 11, 0),
            LocalDateTime.of(2026, 5, 12, 11, 0));

        mockMvc.perform(get("/api/v1/dashboard/continue-watching")
            .header("Authorization", bearerToken(user)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(2))
            .andExpect(jsonPath("$.items[0].tmdbId").value(7001))
            .andExpect(jsonPath("$.items[0].type").value("SHOW"))
            .andExpect(jsonPath("$.items[0].title").value("Dashboard Show"))
            .andExpect(jsonPath("$.items[0].posterPath").value("/show.jpg"))
            .andExpect(jsonPath("$.items[0].backdropPath").value("/show-bg.jpg"))
            .andExpect(jsonPath("$.items[0].watchStatus").value("WATCHING"))
            .andExpect(jsonPath("$.items[0].resumeSeasonNumber").value(2))
            .andExpect(jsonPath("$.items[0].resumeEpisodeNumber").value(3))
            .andExpect(jsonPath("$.items[0].nextSeasonNumber").value(2))
            .andExpect(jsonPath("$.items[0].nextEpisodeNumber").value(4))
            .andExpect(jsonPath("$.items[0].lastWatchedAt").value("2026-05-11T10:30:00"))
            .andExpect(jsonPath("$.items[0].updatedAt").value("2026-05-11T10:30:00"))
            .andExpect(jsonPath("$.items[0].rating").value(8.9))
            .andExpect(jsonPath("$.items[1].tmdbId").value(7002))
            .andExpect(jsonPath("$.items[1].type").value("MOVIE"))
            .andExpect(jsonPath("$.items[1].watchStatus").value("WATCHING"));
    }

    @Test
    void continueWatching_returnsEmptyItems_whenUserHasNoActiveProgress() throws Exception {
        Users user = saveUser("dashboard-empty-user", true);

        mockMvc.perform(get("/api/v1/dashboard/continue-watching")
            .header("Authorization", bearerToken(user)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(0));
    }

    @Test
    void continueWatching_sortsByLastWatchedOrUpdatedAt_descending() throws Exception {
        Users user = saveUser("dashboard-sort-user", true);

        Media movieUpdatedMostRecently = saveMedia(7101L, "Newest Update", MediaType.MOVIE);
        Media showWatchedSecond = saveMedia(7102L, "Second Item", MediaType.SHOW);
        Media movieOldest = saveMedia(7103L, "Oldest Item", MediaType.MOVIE);
        Media caughtUpExcluded = saveMedia(7104L, "Caught Up Excluded", MediaType.SHOW);

        saveStatus(user, movieOldest, WatchStatus.WATCHING, null, null,
            null,
            LocalDateTime.of(2026, 5, 9, 8, 0));
        saveStatus(user, showWatchedSecond, WatchStatus.WATCHING, 1, 4,
            LocalDateTime.of(2026, 5, 10, 12, 0),
            LocalDateTime.of(2026, 5, 1, 12, 0));
        saveStatus(user, movieUpdatedMostRecently, WatchStatus.WATCHING, null, null,
            null,
            LocalDateTime.of(2026, 5, 11, 7, 0));
        saveStatus(user, caughtUpExcluded, WatchStatus.UP_TO_DATE, 2, 8,
            LocalDateTime.of(2026, 5, 11, 8, 0),
            LocalDateTime.of(2026, 5, 11, 8, 0));

        mockMvc.perform(get("/api/v1/dashboard/continue-watching")
            .header("Authorization", bearerToken(user)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(3))
            .andExpect(jsonPath("$.items[0].tmdbId").value(7101))
            .andExpect(jsonPath("$.items[1].tmdbId").value(7102))
            .andExpect(jsonPath("$.items[2].tmdbId").value(7103));
    }

    private void saveStatus(
        Users user,
        Media media,
        WatchStatus watchStatus,
        Integer watchPositionSeason,
        Integer watchPositionEpisode,
        LocalDateTime lastWatchedAt,
        LocalDateTime updatedAt
    ) {
        if (media.getType() == MediaType.SHOW) {
            userShowTrackingRepository.save(UserShowTracking.builder()
                .user(user)
                .media(media)
                .status(watchStatus)
                .watchPositionSeason(watchPositionSeason)
                .watchPositionEpisode(watchPositionEpisode)
                .episodesWatchedCount(watchPositionEpisode == null ? 0 : watchPositionEpisode)
                .seasonsCompletedCount(0)
                .lastWatchedAt(lastWatchedAt)
                .updatedAt(updatedAt)
                .build());
            return;
        }

        userMediaStatusRepository.save(UserMediaStatus.builder()
            .user(user)
            .media(media)
            .status(watchStatus)
            .updatedAt(updatedAt)
            .build());
    }
}







