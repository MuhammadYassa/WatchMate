package com.project.watchmate.Integration.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import com.project.watchmate.Dto.TmdbEpisodeSummaryDTO;
import com.project.watchmate.Dto.TmdbMovieDTO;
import com.project.watchmate.Dto.TmdbTvDetailsDTO;
import com.project.watchmate.Dto.TmdbTvEpisodeDTO;
import com.project.watchmate.Dto.TmdbTvSeasonDTO;
import com.project.watchmate.Dto.TmdbTvSeasonSummaryDTO;
import com.project.watchmate.Integration.support.AbstractIntegrationTest;
import com.project.watchmate.Models.Media;
import com.project.watchmate.Models.MediaType;
import com.project.watchmate.Models.ShowEpisode;
import com.project.watchmate.Models.ShowSeason;
import com.project.watchmate.Models.UserEpisodeWatch;
import com.project.watchmate.Models.UserShowTracking;
import com.project.watchmate.Models.Users;
import com.project.watchmate.Models.WatchStatus;

class ShowFeaturesIntegrationTest extends AbstractIntegrationTest {

    @Test
    void publicNextEpisode_returns200WithoutAuth_andRefreshesImportedSnapshot() throws Exception {
        Media show = saveMedia(9001L, "Imported Show", MediaType.SHOW);
        when(tmdbClient.fetchTvDetailsById(eq(9001L))).thenReturn(tmdbTvDetailsWithId(9001L));

        mockMvc.perform(get("/api/v1/shows/{tmdbId}/next-episode", 9001L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tmdbId").value(9001))
            .andExpect(jsonPath("$.nextEpisodeAirDate").value("2099-01-15"))
            .andExpect(jsonPath("$.seasonNumber").value(2))
            .andExpect(jsonPath("$.episodeNumber").value(3))
            .andExpect(jsonPath("$.episodeName").value("Next Episode"));

        Media refreshed = mediaRepository.findById(show.getId()).orElseThrow();
        assertThat(refreshed.getNextEpisodeName()).isEqualTo("Next Episode");
        assertThat(refreshed.getNextEpisodeSeasonNumber()).isEqualTo(2);
        assertThat(refreshed.getTmdbShowStatus()).isEqualTo("Returning Series");
    }

    @Test
    void publicShowDetails_returnsSeasonSummariesWithoutAuth_andDoesNotAutoImport() throws Exception {
        when(tmdbClient.fetchTvDetailsById(eq(9100L))).thenReturn(tmdbTvDetails());

        mockMvc.perform(get("/api/v1/shows/{tmdbId}", 9100L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tmdbId").value(9100))
            .andExpect(jsonPath("$.type").value("SHOW"))
            .andExpect(jsonPath("$.seasons", hasSize(3)))
            .andExpect(jsonPath("$.seasons[0].seasonNumber").value(0))
            .andExpect(jsonPath("$.seasons[1].name").value("Season 1"))
            .andExpect(jsonPath("$.seasons[1].overview").value("Season one summary"))
            .andExpect(jsonPath("$.seasons[2].episodeCount").value(3))
            .andExpect(jsonPath("$.seasons[2].posterPath").value("/s2.jpg"))
            .andExpect(jsonPath("$.seasons[1].episodes").doesNotExist())
            .andExpect(jsonPath("$.seasons[2].episodes").doesNotExist());

        assertThat(mediaRepository.findByTmdbIdAndType(9100L, MediaType.SHOW)).isEmpty();
        verify(tmdbClient, never()).fetchTvSeasonDetails(eq(9100L), anyInt());
    }

    @Test
    void publicShowSeasonEpisodes_returnsOnlyRequestedSeasonWithoutAuth() throws Exception {
        when(tmdbClient.fetchMediaById(eq(37854L), eq(MediaType.SHOW)))
            .thenReturn(TmdbMovieDTO.builder()
                .id(37854L)
                .title("One Piece")
                .overview("Pirate adventure")
                .posterPath("/one-piece.jpg")
                .releaseDate("1999-10-20")
                .genres(List.of())
                .build());
        when(tmdbClient.fetchTvSeasonDetails(eq(37854L), eq(21))).thenReturn(seasonTwentyOne());

        mockMvc.perform(get("/api/v1/shows/{tmdbId}/seasons/{seasonNumber}/episodes", 37854L, 21))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tmdbId").value(37854))
            .andExpect(jsonPath("$.seasonNumber").value(21))
            .andExpect(jsonPath("$.name").value("Season 21"))
            .andExpect(jsonPath("$.episodeCount").value(2))
            .andExpect(jsonPath("$.episodes", hasSize(2)))
            .andExpect(jsonPath("$.episodes[0].seasonNumber").value(21))
            .andExpect(jsonPath("$.episodes[0].episodeNumber").value(1))
            .andExpect(jsonPath("$.episodes[0].name").value("Departure"))
            .andExpect(jsonPath("$.episodes[1].episodeNumber").value(2))
            .andExpect(jsonPath("$.episodes[1].name").value("Arrival"));

        Media imported = mediaRepository.findByTmdbIdAndType(37854L, MediaType.SHOW).orElseThrow();
        assertThat(showSeasonRepository.findByMediaIdAndSeasonNumber(imported.getId(), 21)).isPresent();
        assertThat(showEpisodeRepository.findAllByMediaIdAndSeasonNumberOrderByEpisodeNumberAsc(imported.getId(), 21)).hasSize(2);
        assertThat(showSeasonRepository.findAllByMediaIdOrderBySeasonNumberAsc(imported.getId()))
            .extracting(ShowSeason::getSeasonNumber)
            .containsExactly(21);
        assertThat(showEpisodeRepository.findAllByMediaIdAndSeasonNumberOrderByEpisodeNumberAsc(imported.getId(), 20)).isEmpty();

        verify(tmdbClient).fetchTvSeasonDetails(eq(37854L), eq(21));
        verify(tmdbClient, never()).fetchTvSeasonDetails(eq(37854L), eq(20));
        verify(tmdbClient, never()).fetchTvSeasonDetails(eq(37854L), eq(22));
        verify(tmdbClient, never()).fetchTvDetailsById(eq(37854L));
    }

    @Test
    void publicShowSeasonEpisodes_secondRequestUsesDatabaseCacheWithoutTmdb() throws Exception {
        when(tmdbClient.fetchMediaById(eq(37854L), eq(MediaType.SHOW)))
            .thenReturn(TmdbMovieDTO.builder()
                .id(37854L)
                .title("One Piece")
                .overview("Pirate adventure")
                .posterPath("/one-piece.jpg")
                .releaseDate("1999-10-20")
                .genres(List.of())
                .build());
        when(tmdbClient.fetchTvSeasonDetails(eq(37854L), eq(21))).thenReturn(seasonTwentyOne());

        mockMvc.perform(get("/api/v1/shows/{tmdbId}/seasons/{seasonNumber}/episodes", 37854L, 21))
            .andExpect(status().isOk());

        clearInvocations(tmdbClient);

        mockMvc.perform(get("/api/v1/shows/{tmdbId}/seasons/{seasonNumber}/episodes", 37854L, 21))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.episodes", hasSize(2)))
            .andExpect(jsonPath("$.episodes[0].name").value("Departure"));

        verify(tmdbClient, never()).fetchTvSeasonDetails(eq(37854L), eq(21));
        verify(tmdbClient, never()).fetchTvDetailsById(eq(37854L));
    }

    @Test
    void publicShowSeasonEpisodes_staleCacheTriggersTmdbRefresh() throws Exception {
        Media importedShow = saveMedia(37854L, "One Piece", MediaType.SHOW);
        showSeasonRepository.save(ShowSeason.builder()
            .media(importedShow)
            .seasonNumber(21)
            .name("Old Season")
            .episodeCount(1)
            .lastTmdbSyncAt(java.time.LocalDateTime.now().minusDays(8))
            .build());
        showEpisodeRepository.save(ShowEpisode.builder()
            .media(importedShow)
            .seasonNumber(21)
            .episodeNumber(1)
            .title("Old Episode")
            .lastTmdbSyncAt(java.time.LocalDateTime.now().minusDays(8))
            .build());
        when(tmdbClient.fetchTvSeasonDetails(eq(37854L), eq(21))).thenReturn(seasonTwentyOne());

        mockMvc.perform(get("/api/v1/shows/{tmdbId}/seasons/{seasonNumber}/episodes", 37854L, 21))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Season 21"))
            .andExpect(jsonPath("$.episodes[0].name").value("Departure"));

        Media persistedShow = mediaRepository.findByTmdbIdAndType(37854L, MediaType.SHOW).orElseThrow();
        ShowSeason refreshedSeason = showSeasonRepository.findByMediaIdAndSeasonNumber(persistedShow.getId(), 21).orElseThrow();
        List<ShowEpisode> refreshedEpisodes = showEpisodeRepository.findAllByMediaIdAndSeasonNumberOrderByEpisodeNumberAsc(persistedShow.getId(), 21);

        assertThat(refreshedSeason.getName()).isEqualTo("Season 21");
        assertThat(refreshedEpisodes).hasSize(2);
        assertThat(refreshedEpisodes).extracting(ShowEpisode::getTitle).containsExactly("Departure", "Arrival");
        verify(tmdbClient).fetchTvSeasonDetails(eq(37854L), eq(21));
    }

    @Test
    void showTrackingEndpoint_returnsPersistedShowTracking() throws Exception {
        Users user = saveUser("show-progress-get-user", true);
        Media show = saveMedia(9400L, "Progress Show", MediaType.SHOW);
        UserShowTracking trackingRecord = UserShowTracking.builder()
            .user(user)
            .media(show)
            .status(WatchStatus.WATCHING)
            .episodesWatchedCount(2)
            .seasonsCompletedCount(1)
            .episodeWatches(new java.util.ArrayList<>())
            .build();
        trackingRecord.getEpisodeWatches().add(UserEpisodeWatch.builder()
            .userShowTracking(trackingRecord)
            .seasonNumber(2)
            .episodeNumber(1)
            .watchedAt(java.time.LocalDateTime.of(2026, 5, 2, 10, 0))
            .build());
        trackingRecord.getEpisodeWatches().add(UserEpisodeWatch.builder()
            .userShowTracking(trackingRecord)
            .seasonNumber(1)
            .episodeNumber(2)
            .watchedAt(java.time.LocalDateTime.of(2026, 5, 1, 10, 0))
            .build());
        userShowTrackingRepository.save(trackingRecord);

        mockMvc.perform(get("/api/v1/shows/{tmdbId}/progress", 9400L)
            .header("Authorization", bearerToken(user)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tmdbId").value(9400))
            .andExpect(jsonPath("$.status").value("WATCHING"))
            .andExpect(jsonPath("$.latestWatchedSeason").value(2))
            .andExpect(jsonPath("$.latestWatchedEpisode").value(1))
            .andExpect(jsonPath("$.episodesWatchedCount").value(2))
            .andExpect(jsonPath("$.watchedEpisodes", hasSize(2)))
            .andExpect(jsonPath("$.watchedEpisodes[0].seasonNumber").value(1))
            .andExpect(jsonPath("$.watchedEpisodes[0].episodeNumber").value(2))
            .andExpect(jsonPath("$.watchedEpisodes[1].seasonNumber").value(2))
            .andExpect(jsonPath("$.watchedEpisodes[1].episodeNumber").value(1));
    }

    @Test
    void watchedEpisodesEndpoint_returnsWatchedRowsOnly() throws Exception {
        Users user = saveUser("show-watched-list-user", true);
        Media show = saveMedia(9401L, "Watched List Show", MediaType.SHOW);
        UserShowTracking trackingRecord = UserShowTracking.builder()
            .user(user)
            .media(show)
            .status(WatchStatus.WATCHING)
            .episodeWatches(new java.util.ArrayList<>())
            .build();
        trackingRecord.getEpisodeWatches().add(UserEpisodeWatch.builder()
            .userShowTracking(trackingRecord)
            .seasonNumber(3)
            .episodeNumber(5)
            .watchedAt(java.time.LocalDateTime.of(2026, 5, 2, 10, 0))
            .build());
        trackingRecord.getEpisodeWatches().add(UserEpisodeWatch.builder()
            .userShowTracking(trackingRecord)
            .seasonNumber(1)
            .episodeNumber(1)
            .watchedAt(java.time.LocalDateTime.of(2026, 5, 1, 10, 0))
            .build());
        userShowTrackingRepository.save(trackingRecord);

        mockMvc.perform(get("/api/v1/shows/{tmdbId}/episodes/watched", 9401L)
            .header("Authorization", bearerToken(user)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].seasonNumber").value(1))
            .andExpect(jsonPath("$[0].episodeNumber").value(1))
            .andExpect(jsonPath("$[1].seasonNumber").value(3))
            .andExpect(jsonPath("$[1].episodeNumber").value(5));
    }

    @Test
    void updateWatchPosition_autoImportsShowAndPersistsTracking() throws Exception {
        Users user = saveUser("show-progress-user", true);
        when(tmdbClient.fetchMediaById(eq(9200L), eq(MediaType.SHOW)))
            .thenReturn(TmdbMovieDTO.builder()
                .id(9200L)
                .title("Imported Progress Show")
                .overview("Progress overview")
                .posterPath("/progress.jpg")
                .releaseDate("2020-01-01")
                .genres(List.of())
                .build());
        when(tmdbClient.fetchTvDetailsById(eq(9200L))).thenReturn(tmdbTvDetailsWithId(9200L));
        when(tmdbClient.fetchTvSeasonDetails(eq(9200L), eq(2))).thenReturn(seasonTwo());

        mockMvc.perform(put("/api/v1/shows/{tmdbId}/progress", 9200L)
            .header("Authorization", bearerToken(user))
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content("""
                {"watchPositionSeason":2,"watchPositionEpisode":1,"markPreviousEpisodesWatched":false}
                """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tmdbId").value(9200))
            .andExpect(jsonPath("$.status").value("WATCHING"))
            .andExpect(jsonPath("$.latestWatchedSeason").doesNotExist())
            .andExpect(jsonPath("$.latestWatchedEpisode").doesNotExist())
            .andExpect(jsonPath("$.watchPositionSeason").value(2))
            .andExpect(jsonPath("$.watchPositionEpisode").value(1))
            .andExpect(jsonPath("$.episodesWatchedCount").value(0))
            .andExpect(jsonPath("$.seasonsCompletedCount").value(0));

        Media imported = mediaRepository.findByTmdbIdAndType(9200L, MediaType.SHOW).orElseThrow();
        UserShowTracking persistedTracking = userShowTrackingRepository.findByUserAndMedia(user, imported).orElseThrow();
        assertThat(imported.getNextEpisodeName()).isEqualTo("Next Episode");
        assertThat(userMediaStatusRepository.findByUserAndMedia(user, imported)).isEmpty();
        assertThat(persistedTracking.getStatus()).isEqualTo(WatchStatus.WATCHING);
        assertThat(persistedTracking.getWatchPositionSeason()).isEqualTo(2);
        assertThat(persistedTracking.getWatchPositionEpisode()).isEqualTo(1);
        assertThat(persistedTracking.getEpisodesWatchedCount()).isZero();
        assertThat(showSeasonRepository.findAllByMediaIdOrderBySeasonNumberAsc(imported.getId()))
            .extracting(ShowSeason::getSeasonNumber)
            .containsExactly(2);
        assertThat(showEpisodeRepository.findAllByMediaIdAndSeasonNumberOrderByEpisodeNumberAsc(imported.getId(), 2)).hasSize(3);
        verify(tmdbClient, never()).fetchTvSeasonDetails(eq(9200L), eq(1));
    }

    @Test
    void markEpisodeWatched_whenUnwatchingMissingEpisode_returns200NoOp() throws Exception {
        Users user = saveUser("show-noop-user", true);
        saveMedia(9300L, "Existing Show", MediaType.SHOW);
        when(tmdbClient.fetchTvDetailsById(eq(9300L))).thenReturn(tmdbTvDetailsWithId(9300L));
        when(tmdbClient.fetchTvSeasonDetails(eq(9300L), eq(1))).thenReturn(seasonOne());
        when(tmdbClient.fetchTvSeasonDetails(eq(9300L), eq(2))).thenReturn(seasonTwo());

        mockMvc.perform(put("/api/v1/shows/{tmdbId}/episodes/{seasonNumber}/{episodeNumber}", 9300L, 1, 1)
            .header("Authorization", bearerToken(user))
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content("""
                {"watched":false}
                """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("NONE"))
            .andExpect(jsonPath("$.episodesWatchedCount").value(0))
            .andExpect(jsonPath("$.watchedEpisodes", hasSize(0)));

        Media media = mediaRepository.findByTmdbIdAndType(9300L, MediaType.SHOW).orElseThrow();
        assertThat(userMediaStatusRepository.findByUserAndMedia(user, media)).isEmpty();
        verify(tmdbClient, never()).fetchTvSeasonDetails(eq(9300L), eq(1));
    }

    @Test
    void updateShowStatus_watchedForOngoingShow_returns422WhenFullAiredMetadataIsMissing() throws Exception {
        Users user = saveUser("show-status-ongoing-user", true);
        when(tmdbClient.fetchMediaById(eq(9500L), eq(MediaType.SHOW)))
            .thenReturn(TmdbMovieDTO.builder()
                .id(9500L)
                .title("Ongoing Show")
                .overview("Show overview")
                .posterPath("/ongoing.jpg")
                .releaseDate("2020-01-01")
                .genres(List.of())
                .build());
        when(tmdbClient.fetchTvDetailsById(eq(9500L))).thenReturn(tmdbTvDetailsWithId(9500L));

        mockMvc.perform(put("/api/v1/shows/{tmdbId}/status", 9500L)
            .header("Authorization", bearerToken(user))
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content("""
                {"status":"WATCHED"}
                """))
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.code").value("SHOW_METADATA_SYNC_REQUIRED"));
    }

    @Test
    void updateShowStatus_watchedForOngoingShow_normalizesToUpToDateWhenAiredMetadataIsCached() throws Exception {
        Users user = saveUser("show-status-cached-user", true);
        Media show = saveMedia(9502L, "Cached Ongoing Show", MediaType.SHOW);
        showSeasonRepository.save(ShowSeason.builder()
            .media(show)
            .seasonNumber(1)
            .name("Season 1")
            .episodeCount(2)
            .lastTmdbSyncAt(java.time.LocalDateTime.now())
            .build());
        showSeasonRepository.save(ShowSeason.builder()
            .media(show)
            .seasonNumber(2)
            .name("Season 2")
            .episodeCount(3)
            .lastTmdbSyncAt(java.time.LocalDateTime.now())
            .build());
        showEpisodeRepository.saveAll(List.of(
            cachedEpisode(show, 1, 1, "2020-01-01"),
            cachedEpisode(show, 1, 2, "2020-01-08"),
            cachedEpisode(show, 2, 1, "2026-01-01"),
            cachedEpisode(show, 2, 2, "2026-01-08"),
            cachedEpisode(show, 2, 3, "2099-01-15")
        ));
        when(tmdbClient.fetchTvDetailsById(eq(9502L))).thenReturn(tmdbTvDetailsWithId(9502L));

        mockMvc.perform(put("/api/v1/shows/{tmdbId}/status", 9502L)
            .header("Authorization", bearerToken(user))
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content("""
                {"status":"WATCHED"}
                """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tmdbId").value(9502))
            .andExpect(jsonPath("$.status").value("UP_TO_DATE"));

        UserShowTracking persistedTracking = userShowTrackingRepository.findByUserAndMedia(user, show).orElseThrow();
        assertThat(persistedTracking.getStatus()).isEqualTo(WatchStatus.UP_TO_DATE);
        assertThat(persistedTracking.getEpisodesWatchedCount()).isEqualTo(4);
        assertThat(userEpisodeWatchRepository.findByUserShowTrackingOrderBySeasonNumberAscEpisodeNumberAsc(persistedTracking))
            .extracting(UserEpisodeWatch::getSeasonNumber, UserEpisodeWatch::getEpisodeNumber)
            .containsExactly(
                org.assertj.core.groups.Tuple.tuple(1, 1),
                org.assertj.core.groups.Tuple.tuple(1, 2),
                org.assertj.core.groups.Tuple.tuple(2, 1),
                org.assertj.core.groups.Tuple.tuple(2, 2)
            );
    }

    @Test
    void updateShowStatus_none_clearsSummaryAndProgressRows() throws Exception {
        Users user = saveUser("show-status-clear-user", true);
        Media show = saveMedia(9501L, "Clearable Show", MediaType.SHOW);
        UserShowTracking tracking = UserShowTracking.builder()
            .user(user)
            .media(show)
            .status(WatchStatus.WATCHING)
            .episodeWatches(new java.util.ArrayList<>())
            .episodesWatchedCount(1)
            .build();
        tracking.getEpisodeWatches().add(UserEpisodeWatch.builder()
            .userShowTracking(tracking)
            .seasonNumber(1)
            .episodeNumber(1)
            .watchedAt(java.time.LocalDateTime.now())
            .build());
        userShowTrackingRepository.save(tracking);

        mockMvc.perform(put("/api/v1/shows/{tmdbId}/status", 9501L)
            .header("Authorization", bearerToken(user))
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content("""
                {"status":"NONE"}
                """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("NONE"));

        assertThat(userMediaStatusRepository.findByUserAndMedia(user, show)).isEmpty();
        assertThat(userShowTrackingRepository.findByUserAndMedia(user, show)).isEmpty();
        assertThat(userEpisodeWatchRepository.findAll()).isEmpty();
    }

    private ShowEpisode cachedEpisode(Media show, int seasonNumber, int episodeNumber, String airDate) {
        return ShowEpisode.builder()
            .media(show)
            .seasonNumber(seasonNumber)
            .episodeNumber(episodeNumber)
            .title("Episode " + seasonNumber + "x" + episodeNumber)
            .airDate(java.time.LocalDate.parse(airDate))
            .lastTmdbSyncAt(java.time.LocalDateTime.now())
            .build();
    }

    private TmdbTvDetailsDTO tmdbTvDetails() {
        return tmdbTvDetailsWithId(9100L);
    }

    private TmdbTvDetailsDTO tmdbTvDetailsWithId(Long tmdbId) {
        return TmdbTvDetailsDTO.builder()
            .id(tmdbId)
            .name("TMDB Show")
            .overview("A full show payload")
            .posterPath("/show.jpg")
            .backdropPath("/show-bg.jpg")
            .firstAirDate("2020-01-01")
            .voteAverage(8.7)
            .numberOfSeasons(3)
            .numberOfEpisodes(6)
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
                .seasonNumber(2)
                .episodeNumber(2)
                .name("Last Episode")
                .build())
            .seasons(List.of(
                TmdbTvSeasonSummaryDTO.builder().id(100L).seasonNumber(0).name("Specials").overview("Special episodes").episodeCount(1).airDate("2019-12-25").posterPath("/s0.jpg").build(),
                TmdbTvSeasonSummaryDTO.builder().id(101L).seasonNumber(1).name("Season 1").overview("Season one summary").episodeCount(2).airDate("2020-01-01").posterPath("/s1.jpg").build(),
                TmdbTvSeasonSummaryDTO.builder().id(102L).seasonNumber(2).name("Season 2").overview("Season two summary").episodeCount(3).airDate("2026-01-01").posterPath("/s2.jpg").build()))
            .build();
    }

    private TmdbTvSeasonDTO seasonOne() {
        return TmdbTvSeasonDTO.builder()
            .id(101L)
            .seasonNumber(1)
            .name("Season 1")
            .posterPath("/s1.jpg")
            .episodes(List.of(
                TmdbTvEpisodeDTO.builder().id(1101L).seasonNumber(1).episodeNumber(1).name("Pilot").airDate("2020-01-01").runtime(45).stillPath("/pilot.jpg").build(),
                TmdbTvEpisodeDTO.builder().id(1102L).seasonNumber(1).episodeNumber(2).name("Second").airDate("2020-01-08").runtime(46).stillPath("/second.jpg").build()))
            .build();
    }

    private TmdbTvSeasonDTO seasonTwo() {
        return TmdbTvSeasonDTO.builder()
            .id(102L)
            .seasonNumber(2)
            .name("Season 2")
            .posterPath("/s2.jpg")
            .episodes(List.of(
                TmdbTvEpisodeDTO.builder().id(1201L).seasonNumber(2).episodeNumber(1).name("Return").airDate("2026-01-01").runtime(47).stillPath("/return.jpg").build(),
                TmdbTvEpisodeDTO.builder().id(1202L).seasonNumber(2).episodeNumber(2).name("Current").airDate("2026-01-08").runtime(48).stillPath("/current.jpg").build(),
                TmdbTvEpisodeDTO.builder().id(1203L).seasonNumber(2).episodeNumber(3).name("Next Episode").airDate("2099-01-15").runtime(49).stillPath("/next.jpg").build()))
            .build();
    }

    private TmdbTvSeasonDTO seasonTwentyOne() {
        return TmdbTvSeasonDTO.builder()
            .id(210L)
            .seasonNumber(21)
            .name("Season 21")
            .overview("Requested season only")
            .posterPath("/s21.jpg")
            .airDate("2017-04-09")
            .episodes(List.of(
                TmdbTvEpisodeDTO.builder().id(2102L).seasonNumber(21).episodeNumber(2).name("Arrival").overview("Second episode").airDate("2017-04-16").runtime(24).stillPath("/arrival.jpg").build(),
                TmdbTvEpisodeDTO.builder().id(2101L).seasonNumber(21).episodeNumber(1).name("Departure").overview("First episode").airDate("2017-04-09").runtime(24).stillPath("/departure.jpg").build()))
            .build();
    }
}
