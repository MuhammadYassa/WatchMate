package com.project.watchmate.Integration.dashboard;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.project.watchmate.Integration.support.AbstractIntegrationTest;
import com.project.watchmate.Models.Media;
import com.project.watchmate.Models.MediaType;
import com.project.watchmate.Models.UserMediaStatus;
import com.project.watchmate.Models.Users;
import com.project.watchmate.Models.WatchStatus;

class DashboardUpcomingEpisodesIntegrationTest extends AbstractIntegrationTest {

    @Test
    void upcomingEpisodes_returns401_withoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/upcoming-episodes"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("AUTH_FAILED"));
    }

    @Test
    void upcomingEpisodes_returnsOnlyUsersTrackedUpcomingShows_sortedByAirDate_andDoesNotCallTmdb() throws Exception {
        Users user = saveUser("upcoming-user", true);
        Users otherUser = saveUser("upcoming-other-user", true);

        Media firstUpcomingShow = mediaRepository.save(Media.builder()
            .tmdbId(8001L)
            .title("First Upcoming Show")
            .posterPath("/first.jpg")
            .backdropPath("/first-bg.jpg")
            .type(MediaType.SHOW)
            .nextEpisodeSeasonNumber(2)
            .nextEpisodeEpisodeNumber(3)
            .nextEpisodeName("First Episode")
            .nextEpisodeAirDate(LocalDate.of(2099, 1, 10))
            .tmdbShowStatus("Returning Series")
            .build());
        Media secondUpcomingShow = mediaRepository.save(Media.builder()
            .tmdbId(8002L)
            .title("Second Upcoming Show")
            .posterPath("/second.jpg")
            .backdropPath("/second-bg.jpg")
            .type(MediaType.SHOW)
            .nextEpisodeSeasonNumber(5)
            .nextEpisodeEpisodeNumber(1)
            .nextEpisodeName("Season Premiere")
            .nextEpisodeAirDate(LocalDate.of(2099, 2, 1))
            .tmdbShowStatus("Planned")
            .build());
        Media noAirDateShow = mediaRepository.save(Media.builder()
            .tmdbId(8003L)
            .title("Missing Snapshot Show")
            .type(MediaType.SHOW)
            .build());
        Media alreadyAiredShow = mediaRepository.save(Media.builder()
            .tmdbId(8004L)
            .title("Past Episode Show")
            .type(MediaType.SHOW)
            .nextEpisodeAirDate(LocalDate.of(2000, 1, 1))
            .build());
        Media movie = mediaRepository.save(Media.builder()
            .tmdbId(8005L)
            .title("Movie Should Be Excluded")
            .type(MediaType.MOVIE)
            .nextEpisodeAirDate(LocalDate.of(2099, 1, 5))
            .build());
        Media otherUsersShow = mediaRepository.save(Media.builder()
            .tmdbId(8006L)
            .title("Other Users Show")
            .type(MediaType.SHOW)
            .nextEpisodeAirDate(LocalDate.of(2099, 1, 1))
            .build());
        Media watchedShow = mediaRepository.save(Media.builder()
            .tmdbId(8007L)
            .title("Completed Show")
            .type(MediaType.SHOW)
            .nextEpisodeAirDate(LocalDate.of(2099, 1, 3))
            .build());

        saveStatus(user, firstUpcomingShow, WatchStatus.WATCHING);
        saveStatus(user, secondUpcomingShow, WatchStatus.TO_WATCH);
        saveStatus(user, noAirDateShow, WatchStatus.WATCHING);
        saveStatus(user, alreadyAiredShow, WatchStatus.WATCHING);
        saveStatus(user, movie, WatchStatus.WATCHING);
        saveStatus(user, watchedShow, WatchStatus.WATCHED);
        saveStatus(otherUser, otherUsersShow, WatchStatus.WATCHING);

        mockMvc.perform(get("/api/v1/dashboard/upcoming-episodes")
            .header("Authorization", bearerToken(user)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(2))
            .andExpect(jsonPath("$.items[0].tmdbId").value(8001))
            .andExpect(jsonPath("$.items[0].type").value("SHOW"))
            .andExpect(jsonPath("$.items[0].title").value("First Upcoming Show"))
            .andExpect(jsonPath("$.items[0].posterPath").value("/first.jpg"))
            .andExpect(jsonPath("$.items[0].backdropPath").value("/first-bg.jpg"))
            .andExpect(jsonPath("$.items[0].nextEpisodeSeasonNumber").value(2))
            .andExpect(jsonPath("$.items[0].nextEpisodeEpisodeNumber").value(3))
            .andExpect(jsonPath("$.items[0].nextEpisodeName").value("First Episode"))
            .andExpect(jsonPath("$.items[0].nextEpisodeAirDate").value("2099-01-10"))
            .andExpect(jsonPath("$.items[0].tmdbShowStatus").value("Returning Series"))
            .andExpect(jsonPath("$.items[1].tmdbId").value(8002))
            .andExpect(jsonPath("$.items[1].nextEpisodeAirDate").value("2099-02-01"));

        verifyNoInteractions(tmdbClient);
    }

    @Test
    void upcomingEpisodes_returnsEmptyItems_whenUserHasNoTrackedUpcomingShows() throws Exception {
        Users user = saveUser("upcoming-empty-user", true);

        mockMvc.perform(get("/api/v1/dashboard/upcoming-episodes")
            .header("Authorization", bearerToken(user)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(0));

        verifyNoInteractions(tmdbClient);
    }

    private void saveStatus(Users user, Media media, WatchStatus watchStatus) {
        userMediaStatusRepository.save(UserMediaStatus.builder()
            .user(user)
            .media(media)
            .status(watchStatus)
            .updatedAt(LocalDateTime.of(2026, 5, 12, 12, 0))
            .build());
    }
}
