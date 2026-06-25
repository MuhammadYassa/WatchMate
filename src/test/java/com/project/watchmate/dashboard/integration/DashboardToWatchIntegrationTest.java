package com.project.watchmate.dashboard.integration;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.project.watchmate.common.integration.support.AbstractIntegrationTest;
import com.project.watchmate.media.catalog.domain.WatchStatus;
import com.project.watchmate.movie.tracking.domain.UserMediaStatus;
import com.project.watchmate.show.tracking.domain.UserShowTracking;
import com.project.watchmate.user.domain.Users;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DashboardToWatchIntegrationTest extends AbstractIntegrationTest {

    // -------------------------------------------------------------------------
    // Auth
    // -------------------------------------------------------------------------

    @Test
    void toWatch_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/to-watch"))
            .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------------------
    // Basic content
    // -------------------------------------------------------------------------

    @Test
    void toWatch_userWithToWatchMovie_returnsMovie() throws Exception {
        Users user = saveUser("tw-movie-user", true);
        com.project.watchmate.media.catalog.domain.Media movie =
            saveMedia(6001L, "To Watch Movie", com.project.watchmate.media.catalog.domain.MediaType.MOVIE);

        userMediaStatusRepository.save(UserMediaStatus.builder()
            .user(user).media(movie).status(WatchStatus.TO_WATCH).build());

        mockMvc.perform(get("/api/v1/dashboard/to-watch")
                .header("Authorization", bearerToken(user)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(1)))
            .andExpect(jsonPath("$.content[0].tmdbId").value(6001))
            .andExpect(jsonPath("$.content[0].type").value("MOVIE"))
            .andExpect(jsonPath("$.content[0].title").value("To Watch Movie"))
            .andExpect(jsonPath("$.content[0].watchStatus").value("TO_WATCH"))
            .andExpect(jsonPath("$.content[0].firstAirDate").doesNotExist())
            .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void toWatch_userWithToWatchShow_returnsShow() throws Exception {
        Users user = saveUser("tw-show-user", true);
        com.project.watchmate.media.catalog.domain.Media show =
            saveMedia(6002L, "To Watch Show", com.project.watchmate.media.catalog.domain.MediaType.SHOW);

        userShowTrackingRepository.save(UserShowTracking.builder()
            .user(user).media(show).status(WatchStatus.TO_WATCH).build());

        mockMvc.perform(get("/api/v1/dashboard/to-watch")
                .header("Authorization", bearerToken(user)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(1)))
            .andExpect(jsonPath("$.content[0].tmdbId").value(6002))
            .andExpect(jsonPath("$.content[0].type").value("SHOW"))
            .andExpect(jsonPath("$.content[0].watchStatus").value("TO_WATCH"))
            .andExpect(jsonPath("$.content[0].releaseDate").doesNotExist())
            .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void toWatch_userWithBothMovieAndShow_returnsBoth() throws Exception {
        Users user = saveUser("tw-both-user", true);
        com.project.watchmate.media.catalog.domain.Media movie =
            saveMedia(6003L, "Both Movie", com.project.watchmate.media.catalog.domain.MediaType.MOVIE);
        com.project.watchmate.media.catalog.domain.Media show =
            saveMedia(6004L, "Both Show", com.project.watchmate.media.catalog.domain.MediaType.SHOW);

        userMediaStatusRepository.save(UserMediaStatus.builder()
            .user(user).media(movie).status(WatchStatus.TO_WATCH).build());
        userShowTrackingRepository.save(UserShowTracking.builder()
            .user(user).media(show).status(WatchStatus.TO_WATCH).build());

        mockMvc.perform(get("/api/v1/dashboard/to-watch")
                .header("Authorization", bearerToken(user)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(2)))
            .andExpect(jsonPath("$.totalElements").value(2));
    }

    // -------------------------------------------------------------------------
    // Type filter
    // -------------------------------------------------------------------------

    @Test
    void toWatch_typeMovie_returnsOnlyMovies() throws Exception {
        Users user = saveUser("tw-type-movie-user", true);
        com.project.watchmate.media.catalog.domain.Media movie =
            saveMedia(6005L, "Type Movie", com.project.watchmate.media.catalog.domain.MediaType.MOVIE);
        com.project.watchmate.media.catalog.domain.Media show =
            saveMedia(6006L, "Type Show", com.project.watchmate.media.catalog.domain.MediaType.SHOW);

        userMediaStatusRepository.save(UserMediaStatus.builder()
            .user(user).media(movie).status(WatchStatus.TO_WATCH).build());
        userShowTrackingRepository.save(UserShowTracking.builder()
            .user(user).media(show).status(WatchStatus.TO_WATCH).build());

        mockMvc.perform(get("/api/v1/dashboard/to-watch")
                .header("Authorization", bearerToken(user))
                .param("type", "MOVIE"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(1)))
            .andExpect(jsonPath("$.content[0].type").value("MOVIE"))
            .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void toWatch_typeShow_returnsOnlyShows() throws Exception {
        Users user = saveUser("tw-type-show-user", true);
        com.project.watchmate.media.catalog.domain.Media movie =
            saveMedia(6007L, "Filter Movie", com.project.watchmate.media.catalog.domain.MediaType.MOVIE);
        com.project.watchmate.media.catalog.domain.Media show =
            saveMedia(6008L, "Filter Show", com.project.watchmate.media.catalog.domain.MediaType.SHOW);

        userMediaStatusRepository.save(UserMediaStatus.builder()
            .user(user).media(movie).status(WatchStatus.TO_WATCH).build());
        userShowTrackingRepository.save(UserShowTracking.builder()
            .user(user).media(show).status(WatchStatus.TO_WATCH).build());

        mockMvc.perform(get("/api/v1/dashboard/to-watch")
                .header("Authorization", bearerToken(user))
                .param("type", "SHOW"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(1)))
            .andExpect(jsonPath("$.content[0].type").value("SHOW"))
            .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void toWatch_invalidType_returns400() throws Exception {
        Users user = saveUser("tw-invalid-type-user", true);

        mockMvc.perform(get("/api/v1/dashboard/to-watch")
                .header("Authorization", bearerToken(user))
                .param("type", "ANIME"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    // -------------------------------------------------------------------------
    // Status exclusion
    // -------------------------------------------------------------------------

    @Test
    void toWatch_excludesWatching_watchedUpToDateAndNoneStatuses() throws Exception {
        Users user = saveUser("tw-exclusion-user", true);

        com.project.watchmate.media.catalog.domain.Media m1 =
            saveMedia(6010L, "Watching Movie", com.project.watchmate.media.catalog.domain.MediaType.MOVIE);
        com.project.watchmate.media.catalog.domain.Media m2 =
            saveMedia(6011L, "Watched Movie", com.project.watchmate.media.catalog.domain.MediaType.MOVIE);
        com.project.watchmate.media.catalog.domain.Media s1 =
            saveMedia(6012L, "Up To Date Show", com.project.watchmate.media.catalog.domain.MediaType.SHOW);
        com.project.watchmate.media.catalog.domain.Media s2 =
            saveMedia(6013L, "Watched Show", com.project.watchmate.media.catalog.domain.MediaType.SHOW);

        userMediaStatusRepository.save(UserMediaStatus.builder()
            .user(user).media(m1).status(WatchStatus.WATCHING).build());
        userMediaStatusRepository.save(UserMediaStatus.builder()
            .user(user).media(m2).status(WatchStatus.WATCHED).build());
        userShowTrackingRepository.save(UserShowTracking.builder()
            .user(user).media(s1).status(WatchStatus.UP_TO_DATE).build());
        userShowTrackingRepository.save(UserShowTracking.builder()
            .user(user).media(s2).status(WatchStatus.WATCHED).build());

        mockMvc.perform(get("/api/v1/dashboard/to-watch")
                .header("Authorization", bearerToken(user)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(0)))
            .andExpect(jsonPath("$.totalElements").value(0));
    }

    // -------------------------------------------------------------------------
    // Pagination
    // -------------------------------------------------------------------------

    @Test
    void toWatch_defaultPaginationReturns20Items() throws Exception {
        Users user = saveUser("tw-pagination-user", true);

        for (int i = 1; i <= 25; i++) {
            com.project.watchmate.media.catalog.domain.Media m =
                saveMedia(7000L + i, "Paged Movie " + i, com.project.watchmate.media.catalog.domain.MediaType.MOVIE);
            userMediaStatusRepository.save(UserMediaStatus.builder()
                .user(user).media(m).status(WatchStatus.TO_WATCH).build());
        }

        mockMvc.perform(get("/api/v1/dashboard/to-watch")
                .header("Authorization", bearerToken(user)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(20)))
            .andExpect(jsonPath("$.totalElements").value(25))
            .andExpect(jsonPath("$.number").value(0))
            .andExpect(jsonPath("$.size").value(20));
    }

    @Test
    void toWatch_page1WithSize5_returnsCorrectSlice() throws Exception {
        Users user = saveUser("tw-slice-user", true);

        for (int i = 1; i <= 8; i++) {
            com.project.watchmate.media.catalog.domain.Media m =
                saveMedia(8000L + i, "Slice Movie " + i, com.project.watchmate.media.catalog.domain.MediaType.MOVIE);
            userMediaStatusRepository.save(UserMediaStatus.builder()
                .user(user).media(m).status(WatchStatus.TO_WATCH).build());
        }

        mockMvc.perform(get("/api/v1/dashboard/to-watch")
                .header("Authorization", bearerToken(user))
                .param("page", "1")
                .param("size", "5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(3)))
            .andExpect(jsonPath("$.totalElements").value(8))
            .andExpect(jsonPath("$.number").value(1));
    }

    @Test
    void toWatch_sizeExceeding50_returns400() throws Exception {
        Users user = saveUser("tw-size-cap-user", true);

        mockMvc.perform(get("/api/v1/dashboard/to-watch")
                .header("Authorization", bearerToken(user))
                .param("size", "51"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void toWatch_emptyList_returnsEmptyPage() throws Exception {
        Users user = saveUser("tw-empty-user", true);

        mockMvc.perform(get("/api/v1/dashboard/to-watch")
                .header("Authorization", bearerToken(user)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(0)))
            .andExpect(jsonPath("$.totalElements").value(0));
    }

    // -------------------------------------------------------------------------
    // User isolation
    // -------------------------------------------------------------------------

    @Test
    void toWatch_userIsolation_userACannotSeeUserBItems() throws Exception {
        Users userA = saveUser("tw-isolation-a", true);
        Users userB = saveUser("tw-isolation-b", true);

        com.project.watchmate.media.catalog.domain.Media movieA =
            saveMedia(9001L, "User A Movie", com.project.watchmate.media.catalog.domain.MediaType.MOVIE);
        com.project.watchmate.media.catalog.domain.Media movieB =
            saveMedia(9002L, "User B Movie", com.project.watchmate.media.catalog.domain.MediaType.MOVIE);

        userMediaStatusRepository.save(UserMediaStatus.builder()
            .user(userA).media(movieA).status(WatchStatus.TO_WATCH).build());
        userMediaStatusRepository.save(UserMediaStatus.builder()
            .user(userB).media(movieB).status(WatchStatus.TO_WATCH).build());

        mockMvc.perform(get("/api/v1/dashboard/to-watch")
                .header("Authorization", bearerToken(userA)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(1)))
            .andExpect(jsonPath("$.content[0].tmdbId").value(9001))
            .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get("/api/v1/dashboard/to-watch")
                .header("Authorization", bearerToken(userB)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(1)))
            .andExpect(jsonPath("$.content[0].tmdbId").value(9002))
            .andExpect(jsonPath("$.totalElements").value(1));
    }

    // -------------------------------------------------------------------------
    // Sort order
    // -------------------------------------------------------------------------

    @Test
    void toWatch_sortsByUpdatedAtDescending() throws Exception {
        Users user = saveUser("tw-sort-user", true);

        com.project.watchmate.media.catalog.domain.Media older =
            saveMedia(5001L, "Older Movie", com.project.watchmate.media.catalog.domain.MediaType.MOVIE);
        com.project.watchmate.media.catalog.domain.Media newer =
            saveMedia(5002L, "Newer Movie", com.project.watchmate.media.catalog.domain.MediaType.MOVIE);

        LocalDateTime olderTime = LocalDateTime.now().minusDays(2);
        LocalDateTime newerTime = LocalDateTime.now().minusDays(1);

        userMediaStatusRepository.save(UserMediaStatus.builder()
            .user(user).media(older).status(WatchStatus.TO_WATCH).updatedAt(olderTime).build());
        userMediaStatusRepository.save(UserMediaStatus.builder()
            .user(user).media(newer).status(WatchStatus.TO_WATCH).updatedAt(newerTime).build());

        mockMvc.perform(get("/api/v1/dashboard/to-watch")
                .header("Authorization", bearerToken(user))
                .param("type", "MOVIE"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(2)))
            .andExpect(jsonPath("$.content[0].tmdbId").value(5002))
            .andExpect(jsonPath("$.content[1].tmdbId").value(5001));
    }
}
