package com.project.watchmate.dashboard.integration;

import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.project.watchmate.common.integration.support.AbstractIntegrationTest;
import com.project.watchmate.media.catalog.domain.Media;
import com.project.watchmate.media.catalog.domain.MediaType;
import com.project.watchmate.show.tracking.domain.UserShowTracking;
import com.project.watchmate.user.domain.Users;
import com.project.watchmate.media.catalog.domain.WatchStatus;

class DashboardCalendarIntegrationTest extends AbstractIntegrationTest {

    @Test
    void calendar_returns401_withoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/calendar")
            .param("from", "2099-01-01")
            .param("to", "2099-01-31"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("AUTH_FAILED"));
    }

    @Test
    void calendar_missingDateParams_returnsBadRequestUsingExistingErrorShape() throws Exception {
        Users user = saveUser("calendar-missing-user", true);

        mockMvc.perform(get("/api/v1/dashboard/calendar")
            .header("Authorization", bearerToken(user))
            .param("from", "2099-01-01"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
            .andExpect(jsonPath("$.fields[*].field", hasItem("to")));
    }

    @Test
    void calendar_invalidDateParams_returnsTypeMismatchError() throws Exception {
        Users user = saveUser("calendar-invalid-user", true);

        mockMvc.perform(get("/api/v1/dashboard/calendar")
            .header("Authorization", bearerToken(user))
            .param("from", "not-a-date")
            .param("to", "2099-01-31"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("TYPE_MISMATCH"))
            .andExpect(jsonPath("$.fields[*].field", hasItem("from")));
    }

    @Test
    void calendar_fromAfterTo_returnsBadRequest() throws Exception {
        Users user = saveUser("calendar-range-user", true);

        mockMvc.perform(get("/api/v1/dashboard/calendar")
            .header("Authorization", bearerToken(user))
            .param("from", "2099-02-01")
            .param("to", "2099-01-01"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
            .andExpect(jsonPath("$.message").value("Parameter 'from' must be on or before 'to'."));
    }

    @Test
    void calendar_returnsOnlyUsersTrackedShowsInsideRange_sortedAscending_andDoesNotCallTmdb() throws Exception {
        Users user = saveUser("calendar-user", true);
        Users otherUser = saveUser("calendar-other-user", true);

        Media inRangeFirst = mediaRepository.save(Media.builder()
            .tmdbId(8101L)
            .title("Alpha Show")
            .posterPath("/alpha.jpg")
            .backdropPath("/alpha-bg.jpg")
            .type(MediaType.SHOW)
            .nextEpisodeSeasonNumber(1)
            .nextEpisodeEpisodeNumber(4)
            .nextEpisodeName("Alpha Episode")
            .nextEpisodeAirDate(LocalDate.of(2099, 1, 10))
            .tmdbShowStatus("Returning Series")
            .build());
        Media inRangeSecond = mediaRepository.save(Media.builder()
            .tmdbId(8102L)
            .title("Bravo Show")
            .posterPath("/bravo.jpg")
            .backdropPath("/bravo-bg.jpg")
            .type(MediaType.SHOW)
            .nextEpisodeSeasonNumber(3)
            .nextEpisodeEpisodeNumber(1)
            .nextEpisodeName("Bravo Premiere")
            .nextEpisodeAirDate(LocalDate.of(2099, 1, 12))
            .tmdbShowStatus("Planned")
            .build());
        Media outOfRangeBefore = mediaRepository.save(Media.builder()
            .tmdbId(8103L)
            .title("Too Early Show")
            .type(MediaType.SHOW)
            .nextEpisodeAirDate(LocalDate.of(2098, 12, 31))
            .build());
        Media outOfRangeAfter = mediaRepository.save(Media.builder()
            .tmdbId(8104L)
            .title("Too Late Show")
            .type(MediaType.SHOW)
            .nextEpisodeAirDate(LocalDate.of(2099, 2, 1))
            .build());
        Media movie = mediaRepository.save(Media.builder()
            .tmdbId(8105L)
            .title("Movie Excluded")
            .type(MediaType.MOVIE)
            .nextEpisodeAirDate(LocalDate.of(2099, 1, 11))
            .build());
        Media watchedShow = mediaRepository.save(Media.builder()
            .tmdbId(8106L)
            .title("Watched Excluded")
            .type(MediaType.SHOW)
            .nextEpisodeAirDate(LocalDate.of(2099, 1, 11))
            .build());
        Media otherUsersShow = mediaRepository.save(Media.builder()
            .tmdbId(8107L)
            .title("Other Users Show")
            .type(MediaType.SHOW)
            .nextEpisodeAirDate(LocalDate.of(2099, 1, 11))
            .build());
        Media caughtUpShow = mediaRepository.save(Media.builder()
            .tmdbId(8108L)
            .title("Caught Up Show")
            .type(MediaType.SHOW)
            .nextEpisodeSeasonNumber(6)
            .nextEpisodeEpisodeNumber(2)
            .nextEpisodeName("Caught Up Episode")
            .nextEpisodeAirDate(LocalDate.of(2099, 1, 11))
            .tmdbShowStatus("Returning Series")
            .build());

        saveStatus(user, inRangeSecond, WatchStatus.TO_WATCH);
        saveStatus(user, caughtUpShow, WatchStatus.UP_TO_DATE);
        saveStatus(user, outOfRangeAfter, WatchStatus.WATCHING);
        saveStatus(user, watchedShow, WatchStatus.WATCHED);
        saveStatus(user, movie, WatchStatus.WATCHING);
        saveStatus(user, inRangeFirst, WatchStatus.WATCHING);
        saveStatus(user, outOfRangeBefore, WatchStatus.WATCHING);
        saveStatus(otherUser, otherUsersShow, WatchStatus.WATCHING);

        mockMvc.perform(get("/api/v1/dashboard/calendar")
            .header("Authorization", bearerToken(user))
            .param("from", "2099-01-01")
            .param("to", "2099-01-31"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(3))
            .andExpect(jsonPath("$.items[0].airDate").value("2099-01-10"))
            .andExpect(jsonPath("$.items[0].tmdbId").value(8101))
            .andExpect(jsonPath("$.items[0].type").value("SHOW"))
            .andExpect(jsonPath("$.items[0].title").value("Alpha Show"))
            .andExpect(jsonPath("$.items[0].posterPath").value("/alpha.jpg"))
            .andExpect(jsonPath("$.items[0].backdropPath").value("/alpha-bg.jpg"))
            .andExpect(jsonPath("$.items[0].seasonNumber").value(1))
            .andExpect(jsonPath("$.items[0].episodeNumber").value(4))
            .andExpect(jsonPath("$.items[0].episodeTitle").value("Alpha Episode"))
            .andExpect(jsonPath("$.items[0].showStatus").value("Returning Series"))
            .andExpect(jsonPath("$.items[0].watchStatus").value("WATCHING"))
            .andExpect(jsonPath("$.items[1].airDate").value("2099-01-11"))
            .andExpect(jsonPath("$.items[1].tmdbId").value(8108))
            .andExpect(jsonPath("$.items[1].watchStatus").value("UP_TO_DATE"))
            .andExpect(jsonPath("$.items[2].airDate").value("2099-01-12"))
            .andExpect(jsonPath("$.items[2].tmdbId").value(8102))
            .andExpect(jsonPath("$.items[2].watchStatus").value("TO_WATCH"));

        verifyNoInteractions(tmdbClient);
    }

    @Test
    void calendar_returnsEmptyItems_whenNoTrackedShowsMatchRange() throws Exception {
        Users user = saveUser("calendar-empty-user", true);

        mockMvc.perform(get("/api/v1/dashboard/calendar")
            .header("Authorization", bearerToken(user))
            .param("from", "2099-01-01")
            .param("to", "2099-01-31"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(0));

        verifyNoInteractions(tmdbClient);
    }

    private void saveStatus(Users user, Media media, WatchStatus watchStatus) {
        if (media.getType() != MediaType.SHOW) {
            return;
        }

        userShowTrackingRepository.save(UserShowTracking.builder()
            .user(user)
            .media(media)
            .status(watchStatus)
            .updatedAt(LocalDateTime.of(2026, 5, 12, 12, 0))
            .build());
    }
}






