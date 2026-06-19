package com.project.watchmate.dashboard.integration;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.project.watchmate.common.integration.support.AbstractIntegrationTest;
import com.project.watchmate.media.catalog.domain.Media;
import com.project.watchmate.media.catalog.domain.MediaType;
import com.project.watchmate.media.tmdb.dto.TmdbEpisodeSummaryDTO;
import com.project.watchmate.media.tmdb.dto.TmdbMovieDTO;
import com.project.watchmate.media.tmdb.dto.TmdbTvDetailsDTO;
import com.project.watchmate.media.tmdb.dto.TmdbTvEpisodeDTO;
import com.project.watchmate.media.tmdb.dto.TmdbTvSeasonDTO;
import com.project.watchmate.media.tmdb.dto.TmdbTvSeasonSummaryDTO;
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

    @Test
    void continueWatching_reflectsBackwardProgressCorrectionAfterCachePriming() throws Exception {
        Users user = saveUser("dashboard-progress-cache-user", true);
        when(tmdbClient.fetchMediaById(org.mockito.ArgumentMatchers.eq(7201L), org.mockito.ArgumentMatchers.eq(MediaType.SHOW)))
            .thenReturn(TmdbMovieDTO.builder()
                .id(7201L)
                .title("Dashboard Progress Show")
                .overview("Dashboard progress overview")
                .posterPath("/dashboard-progress.jpg")
                .releaseDate("2020-01-01")
                .genres(List.of())
                .build());
        when(tmdbClient.fetchTvDetailsById(org.mockito.ArgumentMatchers.eq(7201L))).thenReturn(contiguousProgressShowDetailsWithId(7201L));
        when(tmdbClient.fetchTvSeasonDetails(org.mockito.ArgumentMatchers.eq(7201L), org.mockito.ArgumentMatchers.eq(1))).thenReturn(contiguousSeasonOne());

        mockMvc.perform(put("/api/v1/shows/{tmdbId}/progress", 7201L)
            .header("Authorization", bearerToken(user))
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content("""
                {"watchPositionSeason":1,"watchPositionEpisode":5}
                """))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/dashboard/continue-watching")
            .header("Authorization", bearerToken(user)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].resumeSeasonNumber").value(1))
            .andExpect(jsonPath("$.items[0].resumeEpisodeNumber").value(5));

        mockMvc.perform(put("/api/v1/shows/{tmdbId}/progress", 7201L)
            .header("Authorization", bearerToken(user))
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content("""
                {"watchPositionSeason":1,"watchPositionEpisode":3}
                """))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/dashboard/continue-watching")
            .header("Authorization", bearerToken(user)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].resumeSeasonNumber").value(1))
            .andExpect(jsonPath("$.items[0].resumeEpisodeNumber").value(3));
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

    private TmdbTvDetailsDTO contiguousProgressShowDetailsWithId(Long tmdbId) {
        return TmdbTvDetailsDTO.builder()
            .id(tmdbId)
            .name("Dashboard Progress Show")
            .overview("A show used for dashboard progress cache tests")
            .posterPath("/dashboard-progress.jpg")
            .backdropPath("/dashboard-progress-bg.jpg")
            .firstAirDate("2020-01-01")
            .voteAverage(8.4)
            .numberOfSeasons(2)
            .numberOfEpisodes(8)
            .lastAirDate("2026-01-08")
            .status("Returning Series")
            .genres(List.of())
            .nextEpisodeToAir(TmdbEpisodeSummaryDTO.builder()
                .airDate("2099-01-15")
                .seasonNumber(2)
                .episodeNumber(3)
                .name("Next Episode")
                .build())
            .lastEpisodeToAir(TmdbEpisodeSummaryDTO.builder()
                .airDate("2026-01-08")
                .seasonNumber(2)
                .episodeNumber(2)
                .name("Latest Aired Episode")
                .build())
            .seasons(List.of(
                TmdbTvSeasonSummaryDTO.builder().id(801L).seasonNumber(1).name("Season 1").episodeCount(5).airDate("2020-01-01").build(),
                TmdbTvSeasonSummaryDTO.builder().id(802L).seasonNumber(2).name("Season 2").episodeCount(3).airDate("2026-01-01").build()
            ))
            .build();
    }

    private TmdbTvSeasonDTO contiguousSeasonOne() {
        return TmdbTvSeasonDTO.builder()
            .id(801L)
            .seasonNumber(1)
            .name("Season 1")
            .episodes(List.of(
                TmdbTvEpisodeDTO.builder().id(8101L).seasonNumber(1).episodeNumber(1).name("Episode 1").airDate("2020-01-01").runtime(45).stillPath("/d-s1-e1.jpg").build(),
                TmdbTvEpisodeDTO.builder().id(8102L).seasonNumber(1).episodeNumber(2).name("Episode 2").airDate("2020-01-08").runtime(45).stillPath("/d-s1-e2.jpg").build(),
                TmdbTvEpisodeDTO.builder().id(8103L).seasonNumber(1).episodeNumber(3).name("Episode 3").airDate("2020-01-15").runtime(45).stillPath("/d-s1-e3.jpg").build(),
                TmdbTvEpisodeDTO.builder().id(8104L).seasonNumber(1).episodeNumber(4).name("Episode 4").airDate("2020-01-22").runtime(45).stillPath("/d-s1-e4.jpg").build(),
                TmdbTvEpisodeDTO.builder().id(8105L).seasonNumber(1).episodeNumber(5).name("Episode 5").airDate("2020-01-29").runtime(45).stillPath("/d-s1-e5.jpg").build()
            ))
            .build();
    }
}







