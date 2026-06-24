package com.project.watchmate.show.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import com.project.watchmate.show.jobs.dto.ShowTrackingJobDTO;
import com.project.watchmate.media.tmdb.dto.TmdbEpisodeSummaryDTO;
import com.project.watchmate.media.tmdb.dto.TmdbCastMemberDTO;
import com.project.watchmate.media.tmdb.dto.TmdbCreditsDTO;
import com.project.watchmate.media.tmdb.dto.TmdbMovieDTO;
import com.project.watchmate.media.tmdb.dto.TmdbVideoDTO;
import com.project.watchmate.media.tmdb.dto.TmdbVideosResponseDTO;
import com.project.watchmate.media.tmdb.dto.TmdbTvDetailsDTO;
import com.project.watchmate.media.tmdb.dto.TmdbTvEpisodeDTO;
import com.project.watchmate.media.tmdb.dto.TmdbTvSeasonDTO;
import com.project.watchmate.media.tmdb.dto.TmdbTvSeasonSummaryDTO;
import com.project.watchmate.media.tmdb.dto.TmdbWatchProviderEntryDTO;
import com.project.watchmate.media.tmdb.dto.TmdbWatchProviderRegionDTO;
import com.project.watchmate.media.tmdb.dto.TmdbWatchProvidersResponseDTO;
import com.project.watchmate.common.integration.support.AbstractIntegrationTest;
import com.project.watchmate.media.catalog.domain.Media;
import com.project.watchmate.media.catalog.domain.MediaType;
import com.project.watchmate.media.catalog.domain.ShowEpisode;
import com.project.watchmate.media.catalog.domain.ShowSeason;
import com.project.watchmate.show.jobs.domain.ShowTrackingJob;
import com.project.watchmate.show.jobs.domain.ShowTrackingJobStatus;
import com.project.watchmate.show.jobs.domain.ShowTrackingJobType;
import com.project.watchmate.show.tracking.domain.UserEpisodeWatch;
import com.project.watchmate.show.tracking.domain.UserShowTracking;
import com.project.watchmate.user.domain.Users;
import com.project.watchmate.media.catalog.domain.WatchStatus;

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
        when(tmdbClient.fetchCredits(eq(9100L), eq(MediaType.SHOW))).thenReturn(TmdbCreditsDTO.builder()
            .cast(List.of(TmdbCastMemberDTO.builder()
                .id(501L)
                .name("Show Actor")
                .character("Lead")
                .order(0)
                .knownForDepartment("Acting")
                .build()))
            .build());
        when(tmdbClient.fetchVideos(eq(9100L), eq(MediaType.SHOW))).thenReturn(TmdbVideosResponseDTO.builder()
            .results(List.of(TmdbVideoDTO.builder()
                .key("show-trailer")
                .name("Show Trailer")
                .site("YouTube")
                .type("Trailer")
                .official(true)
                .publishedAt("2026-01-01T00:00:00.000Z")
                .build()))
            .build());
        when(tmdbClient.fetchWatchProviders(eq(9100L), eq(MediaType.SHOW))).thenReturn(TmdbWatchProvidersResponseDTO.builder()
            .results(Map.of("US", TmdbWatchProviderRegionDTO.builder()
                .link("https://example.com/us/show")
                .flatrate(List.of(TmdbWatchProviderEntryDTO.builder()
                    .providerId(2)
                    .providerName("ShowStream")
                    .displayPriority(0)
                    .build()))
                .build()))
            .build());

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
            .andExpect(jsonPath("$.seasons[2].episodes").doesNotExist())
            .andExpect(jsonPath("$.cast", hasSize(1)))
            .andExpect(jsonPath("$.cast[0].tmdbPersonId").value(501))
            .andExpect(jsonPath("$.bestTrailer.key").value("show-trailer"))
            .andExpect(jsonPath("$.watchProviders.region").value("US"))
            .andExpect(jsonPath("$.watchProviders.flatrate[0].providerName").value("ShowStream"));

        assertThat(mediaRepository.findByTmdbIdAndType(9100L, MediaType.SHOW)).isEmpty();
        verify(tmdbClient, never()).fetchTvSeasonDetails(eq(9100L), anyInt());
    }

    @Test
    void publicShowDetails_whenTmdbExtrasEmpty_returnsDefaultExtrasShape() throws Exception {
        when(tmdbClient.fetchTvDetailsById(eq(9101L))).thenReturn(tmdbTvDetailsWithId(9101L));

        mockMvc.perform(get("/api/v1/shows/{tmdbId}", 9101L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cast", hasSize(0)))
            .andExpect(jsonPath("$.bestTrailer").value(nullValue()))
            .andExpect(jsonPath("$.watchProviders.region").value("US"))
            .andExpect(jsonPath("$.watchProviders.flatrate", hasSize(0)))
            .andExpect(jsonPath("$.watchProviders.rent", hasSize(0)))
            .andExpect(jsonPath("$.watchProviders.buy", hasSize(0)))
            .andExpect(jsonPath("$.watchProviders.ads", hasSize(0)))
            .andExpect(jsonPath("$.watchProviders.free", hasSize(0)));
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
            .andExpect(jsonPath("$.episodes[0].tmdbEpisodeId").value(2101))
            .andExpect(jsonPath("$.episodes[0].seasonNumber").value(21))
            .andExpect(jsonPath("$.episodes[0].episodeNumber").value(1))
            .andExpect(jsonPath("$.episodes[0].name").value("Departure"))
            .andExpect(jsonPath("$.episodes[1].tmdbEpisodeId").value(2102))
            .andExpect(jsonPath("$.episodes[1].episodeNumber").value(2))
            .andExpect(jsonPath("$.episodes[1].name").value("Arrival"));

        Media imported = mediaRepository.findByTmdbIdAndType(37854L, MediaType.SHOW).orElseThrow();
        assertThat(showSeasonRepository.findByMediaIdAndSeasonNumber(imported.getId(), 21)).isPresent();
        assertThat(showEpisodeRepository.findAllByMediaIdAndSeasonNumberOrderByEpisodeNumberAsc(imported.getId(), 21))
            .extracting(ShowEpisode::getTmdbEpisodeId)
            .containsExactly(2101L, 2102L);
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
            .andExpect(jsonPath("$.episodes[0].tmdbEpisodeId").value(2101))
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
        assertThat(refreshedEpisodes).extracting(ShowEpisode::getTmdbEpisodeId).containsExactly(2101L, 2102L);
        verify(tmdbClient).fetchTvSeasonDetails(eq(37854L), eq(21));
    }

    @Test
    void publicShowSeasonEpisodes_legacyNullTmdbEpisodeIdStillReturns200() throws Exception {
        Media importedShow = saveMedia(37855L, "Legacy Show", MediaType.SHOW);
        showSeasonRepository.save(ShowSeason.builder()
            .media(importedShow)
            .seasonNumber(3)
            .name("Season 3")
            .episodeCount(1)
            .lastTmdbSyncAt(java.time.LocalDateTime.now())
            .build());
        showEpisodeRepository.save(ShowEpisode.builder()
            .media(importedShow)
            .seasonNumber(3)
            .episodeNumber(1)
            .tmdbEpisodeId(null)
            .title("Legacy Episode")
            .overview("Old cached row without TMDB episode id")
            .airDate(java.time.LocalDate.of(2020, 1, 1))
            .lastTmdbSyncAt(java.time.LocalDateTime.now())
            .build());

        mockMvc.perform(get("/api/v1/shows/{tmdbId}/seasons/{seasonNumber}/episodes", 37855L, 3))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tmdbId").value(37855))
            .andExpect(jsonPath("$.seasonNumber").value(3))
            .andExpect(jsonPath("$.episodes", hasSize(1)))
            .andExpect(jsonPath("$.episodes[0].tmdbEpisodeId").value(nullValue()))
            .andExpect(jsonPath("$.episodes[0].name").value("Legacy Episode"));

        verify(tmdbClient, never()).fetchTvSeasonDetails(eq(37855L), eq(3));
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
    void updateWatchPosition_setsContiguousForwardProgress() throws Exception {
        Users user = saveUser("show-progress-forward-user", true);
        when(tmdbClient.fetchMediaById(eq(9200L), eq(MediaType.SHOW)))
            .thenReturn(TmdbMovieDTO.builder()
                .id(9200L)
                .title("Imported Progress Show")
                .overview("Progress overview")
                .posterPath("/progress.jpg")
                .releaseDate("2020-01-01")
                .genres(List.of())
                .build());
        when(tmdbClient.fetchTvDetailsById(eq(9200L))).thenReturn(contiguousProgressShowDetailsWithId(9200L));
        when(tmdbClient.fetchTvSeasonDetails(eq(9200L), eq(1))).thenReturn(contiguousSeasonOne());

        mockMvc.perform(put("/api/v1/shows/{tmdbId}/progress", 9200L)
            .header("Authorization", bearerToken(user))
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content("""
                {"watchPositionSeason":1,"watchPositionEpisode":5}
                """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tmdbId").value(9200))
            .andExpect(jsonPath("$.status").value("WATCHING"))
            .andExpect(jsonPath("$.latestWatchedSeason").value(1))
            .andExpect(jsonPath("$.latestWatchedEpisode").value(5))
            .andExpect(jsonPath("$.watchPositionSeason").value(1))
            .andExpect(jsonPath("$.watchPositionEpisode").value(5))
            .andExpect(jsonPath("$.episodesWatchedCount").value(5))
            .andExpect(jsonPath("$.seasonsCompletedCount").value(1));

        Media imported = mediaRepository.findByTmdbIdAndType(9200L, MediaType.SHOW).orElseThrow();
        UserShowTracking persistedTracking = userShowTrackingRepository.findByUserAndMedia(user, imported).orElseThrow();
        assertThat(imported.getNextEpisodeName()).isEqualTo("Next Episode");
        assertThat(userMediaStatusRepository.findByUserAndMedia(user, imported)).isEmpty();
        assertThat(persistedTracking.getStatus()).isEqualTo(WatchStatus.WATCHING);
        assertThat(persistedTracking.getWatchPositionSeason()).isEqualTo(1);
        assertThat(persistedTracking.getWatchPositionEpisode()).isEqualTo(5);
        assertThat(persistedTracking.getEpisodesWatchedCount()).isEqualTo(5);
        assertThat(persistedTracking.getSeasonsCompletedCount()).isEqualTo(1);
        assertWatchedRows(persistedTracking, "1x1", "1x2", "1x3", "1x4", "1x5");
        assertThat(showSeasonRepository.findAllByMediaIdOrderBySeasonNumberAsc(imported.getId()))
            .extracting(ShowSeason::getSeasonNumber)
            .containsExactly(1);
        assertThat(showEpisodeRepository.findAllByMediaIdAndSeasonNumberOrderByEpisodeNumberAsc(imported.getId(), 1)).hasSize(5);
    }

    @Test
    void updateWatchPosition_backtracksAndDeletesLaterRows() throws Exception {
        Users user = saveUser("show-progress-backward-user", true);
        Media show = saveMedia(9201L, "Backward Progress Show", MediaType.SHOW);
        UserShowTracking tracking = UserShowTracking.builder()
            .user(user)
            .media(show)
            .status(WatchStatus.WATCHING)
            .watchPositionSeason(1)
            .watchPositionEpisode(5)
            .episodesWatchedCount(5)
            .seasonsCompletedCount(1)
            .episodeWatches(new java.util.ArrayList<>())
            .build();
        for (int episodeNumber = 1; episodeNumber <= 5; episodeNumber++) {
            tracking.getEpisodeWatches().add(UserEpisodeWatch.builder()
                .userShowTracking(tracking)
                .seasonNumber(1)
                .episodeNumber(episodeNumber)
                .watchedAt(java.time.LocalDateTime.of(2026, 5, 1, episodeNumber, 0))
                .build());
        }
        userShowTrackingRepository.saveAndFlush(tracking);
        when(tmdbClient.fetchTvDetailsById(eq(9201L))).thenReturn(contiguousProgressShowDetailsWithId(9201L));
        when(tmdbClient.fetchTvSeasonDetails(eq(9201L), eq(1))).thenReturn(contiguousSeasonOne());

        mockMvc.perform(put("/api/v1/shows/{tmdbId}/progress", 9201L)
            .header("Authorization", bearerToken(user))
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content("""
                {"watchPositionSeason":1,"watchPositionEpisode":3}
                """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("WATCHING"))
            .andExpect(jsonPath("$.latestWatchedSeason").value(1))
            .andExpect(jsonPath("$.latestWatchedEpisode").value(3))
            .andExpect(jsonPath("$.watchPositionSeason").value(1))
            .andExpect(jsonPath("$.watchPositionEpisode").value(3))
            .andExpect(jsonPath("$.episodesWatchedCount").value(3));

        UserShowTracking persistedTracking = userShowTrackingRepository.findByUserAndMedia(user, show).orElseThrow();
        assertThat(persistedTracking.getWatchPositionSeason()).isEqualTo(1);
        assertThat(persistedTracking.getWatchPositionEpisode()).isEqualTo(3);
        assertThat(persistedTracking.getEpisodesWatchedCount()).isEqualTo(3);
        assertWatchedRows(persistedTracking, "1x1", "1x2", "1x3");
    }

    @Test
    void updateWatchPosition_setsCrossSeasonProgressContiguously() throws Exception {
        Users user = saveUser("show-progress-cross-user", true);
        when(tmdbClient.fetchMediaById(eq(9202L), eq(MediaType.SHOW)))
            .thenReturn(TmdbMovieDTO.builder()
                .id(9202L)
                .title("Cross Season Show")
                .overview("Cross season overview")
                .posterPath("/cross.jpg")
                .releaseDate("2020-01-01")
                .genres(List.of())
                .build());
        when(tmdbClient.fetchTvDetailsById(eq(9202L))).thenReturn(contiguousProgressShowDetailsWithId(9202L));
        when(tmdbClient.fetchTvSeasonDetails(eq(9202L), eq(1))).thenReturn(contiguousSeasonOne());
        when(tmdbClient.fetchTvSeasonDetails(eq(9202L), eq(2))).thenReturn(contiguousSeasonTwo());

        mockMvc.perform(put("/api/v1/shows/{tmdbId}/progress", 9202L)
            .header("Authorization", bearerToken(user))
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content("""
                {"watchPositionSeason":2,"watchPositionEpisode":2}
                """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP_TO_DATE"))
            .andExpect(jsonPath("$.latestWatchedSeason").value(2))
            .andExpect(jsonPath("$.latestWatchedEpisode").value(2))
            .andExpect(jsonPath("$.watchPositionSeason").value(2))
            .andExpect(jsonPath("$.watchPositionEpisode").value(2))
            .andExpect(jsonPath("$.episodesWatchedCount").value(7))
            .andExpect(jsonPath("$.seasonsCompletedCount").value(1));

        Media imported = mediaRepository.findByTmdbIdAndType(9202L, MediaType.SHOW).orElseThrow();
        UserShowTracking persistedTracking = userShowTrackingRepository.findByUserAndMedia(user, imported).orElseThrow();
        assertThat(persistedTracking.getStatus()).isEqualTo(WatchStatus.UP_TO_DATE);
        assertWatchedRows(persistedTracking, "1x1", "1x2", "1x3", "1x4", "1x5", "2x1", "2x2");
    }


    @Test
    void updateWatchPosition_withLargeMissingMetadata_returnsAcceptedJobAndCompletesWithReplacement() throws Exception {
        Users user = saveUser("show-progress-job-user", true);
        Media show = saveMedia(9301L, "Async Progress Show", MediaType.SHOW);
        UserShowTracking tracking = UserShowTracking.builder()
            .user(user)
            .media(show)
            .status(WatchStatus.WATCHING)
            .watchPositionSeason(6)
            .watchPositionEpisode(1)
            .episodesWatchedCount(3)
            .episodeWatches(new java.util.ArrayList<>())
            .build();
        tracking.getEpisodeWatches().add(UserEpisodeWatch.builder()
            .userShowTracking(tracking)
            .seasonNumber(1)
            .episodeNumber(1)
            .watchedAt(java.time.LocalDateTime.of(2026, 5, 1, 10, 0))
            .build());
        tracking.getEpisodeWatches().add(UserEpisodeWatch.builder()
            .userShowTracking(tracking)
            .seasonNumber(2)
            .episodeNumber(1)
            .watchedAt(java.time.LocalDateTime.of(2026, 5, 2, 10, 0))
            .build());
        tracking.getEpisodeWatches().add(UserEpisodeWatch.builder()
            .userShowTracking(tracking)
            .seasonNumber(6)
            .episodeNumber(1)
            .watchedAt(java.time.LocalDateTime.of(2026, 5, 3, 10, 0))
            .build());
        userShowTrackingRepository.saveAndFlush(tracking);

        when(tmdbClient.fetchTvDetailsById(eq(9301L))).thenReturn(largeProgressShowDetailsWithId(9301L));
        when(tmdbClient.fetchTvSeasonDetails(eq(9301L), eq(5))).thenReturn(singleEpisodeSeason(5, "2020-02-05"));

        MvcResult mvcResult = mockMvc.perform(put("/api/v1/shows/{tmdbId}/progress", 9301L)
            .header("Authorization", bearerToken(user))
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content("""
                {"watchPositionSeason":5,"watchPositionEpisode":1}
                """))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.jobType").value("SET_SHOW_PROGRESS"))
            .andReturn();

        ShowTrackingJobDTO job = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ShowTrackingJobDTO.class);
        when(tmdbClient.fetchTvSeasonDetails(eq(9301L), eq(1))).thenReturn(singleEpisodeSeason(1, "2020-01-01"));
        when(tmdbClient.fetchTvSeasonDetails(eq(9301L), eq(2))).thenReturn(singleEpisodeSeason(2, "2020-01-08"));
        when(tmdbClient.fetchTvSeasonDetails(eq(9301L), eq(3))).thenReturn(singleEpisodeSeason(3, "2020-01-15"));
        when(tmdbClient.fetchTvSeasonDetails(eq(9301L), eq(4))).thenReturn(singleEpisodeSeason(4, "2020-01-22"));

        showTrackingJobService.pollPendingJobs();

        assertThat(showTrackingJobRepository.findById(job.getJobId())).get()
            .extracting(ShowTrackingJob::getStatus)
            .isEqualTo(ShowTrackingJobStatus.COMPLETED);
        UserShowTracking persistedTracking = userShowTrackingRepository.findByUserAndMedia(user, show).orElseThrow();
        assertThat(persistedTracking.getWatchPositionSeason()).isEqualTo(5);
        assertThat(persistedTracking.getWatchPositionEpisode()).isEqualTo(1);
        assertThat(persistedTracking.getStatus()).isEqualTo(WatchStatus.WATCHING);
        assertWatchedRows(persistedTracking, "1x1", "2x1", "3x1", "4x1", "5x1");
    }

    @Test
    void updateShowStatus_watchedForOngoingShow_returns202WhenFullAiredMetadataIsMissing() throws Exception {
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
        when(tmdbClient.fetchTvDetailsById(eq(9500L))).thenReturn(largeOngoingShowDetailsWithId(9500L));

        mockMvc.perform(put("/api/v1/shows/{tmdbId}/status", 9500L)
            .header("Authorization", bearerToken(user))
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content("""
                {"status":"WATCHED"}
                """))
            .andExpect(status().isAccepted())
            .andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("/api/v1/show-tracking-jobs/")))
            .andExpect(header().string("Retry-After", "2"))
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.jobType").value("MARK_SHOW_UP_TO_DATE"));

        verify(tmdbClient, never()).fetchTvSeasonDetails(eq(9500L), anyInt());
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

    @Test
    void updateShowStatus_watchedForEndedShow_withLargeMissingMetadata_returns202AndCompletesLater() throws Exception {
        Users user = saveUser("show-status-ended-job-user", true);
        Media show = saveMedia(9600L, "Ended Job Show", MediaType.SHOW);
        when(tmdbClient.fetchTvDetailsById(eq(9600L))).thenReturn(endedShowDetailsWithId(9600L));

        MvcResult mvcResult = mockMvc.perform(put("/api/v1/shows/{tmdbId}/status", 9600L)
            .header("Authorization", bearerToken(user))
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content("""
                {"status":"WATCHED"}
                """))
            .andExpect(status().isAccepted())
            .andReturn();

        ShowTrackingJobDTO job = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ShowTrackingJobDTO.class);
        assertThat(showTrackingJobRepository.findById(job.getJobId())).isPresent();
        verify(tmdbClient, never()).fetchTvSeasonDetails(eq(9600L), anyInt());

        when(tmdbClient.fetchTvSeasonDetails(eq(9600L), eq(1))).thenReturn(singleEpisodeSeason(1, "2020-01-01"));
        when(tmdbClient.fetchTvSeasonDetails(eq(9600L), eq(2))).thenReturn(singleEpisodeSeason(2, "2020-01-08"));
        when(tmdbClient.fetchTvSeasonDetails(eq(9600L), eq(3))).thenReturn(singleEpisodeSeason(3, "2020-01-15"));
        when(tmdbClient.fetchTvSeasonDetails(eq(9600L), eq(4))).thenReturn(singleEpisodeSeason(4, "2020-01-22"));

        showTrackingJobService.pollPendingJobs();

        assertThat(showTrackingJobRepository.findById(job.getJobId())).get()
            .extracting(com.project.watchmate.show.jobs.domain.ShowTrackingJob::getStatus)
            .isEqualTo(ShowTrackingJobStatus.COMPLETED);
        UserShowTracking persistedTracking = userShowTrackingRepository.findByUserAndMedia(user, show).orElseThrow();
        assertThat(persistedTracking.getStatus()).isEqualTo(WatchStatus.WATCHED);
        assertThat(persistedTracking.getEpisodesWatchedCount()).isEqualTo(4);
    }

    @Test
    void updateShowStatus_repeatingSameBulkAction_returnsExistingPendingJob() throws Exception {
        Users user = saveUser("show-status-duplicate-job-user", true);
        when(tmdbClient.fetchMediaById(eq(9601L), eq(MediaType.SHOW)))
            .thenReturn(TmdbMovieDTO.builder()
                .id(9601L)
                .title("Duplicate Job Show")
                .overview("Duplicate Job overview")
                .posterPath("/dup.jpg")
                .releaseDate("2020-01-01")
                .genres(List.of())
                .build());
        when(tmdbClient.fetchTvDetailsById(eq(9601L))).thenReturn(endedShowDetailsWithId(9601L));

        MvcResult first = mockMvc.perform(put("/api/v1/shows/{tmdbId}/status", 9601L)
            .header("Authorization", bearerToken(user))
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content("""
                {"status":"WATCHED"}
                """))
            .andExpect(status().isAccepted())
            .andReturn();

        MvcResult second = mockMvc.perform(put("/api/v1/shows/{tmdbId}/status", 9601L)
            .header("Authorization", bearerToken(user))
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content("""
                {"status":"WATCHED"}
                """))
            .andExpect(status().isAccepted())
            .andReturn();

        ShowTrackingJobDTO firstJob = objectMapper.readValue(first.getResponse().getContentAsString(), ShowTrackingJobDTO.class);
        ShowTrackingJobDTO secondJob = objectMapper.readValue(second.getResponse().getContentAsString(), ShowTrackingJobDTO.class);

        assertThat(firstJob.getJobId()).isEqualTo(secondJob.getJobId());
        assertThat(showTrackingJobRepository.count()).isEqualTo(1);
    }

    @Test
    void failedShowTrackingJob_marksFailedWithoutCorruptingTracking() throws Exception {
        Users user = saveUser("show-status-failed-job-user", true);
        Media show = saveMedia(9602L, "Failed Job Show", MediaType.SHOW);
        when(tmdbClient.fetchTvDetailsById(eq(9602L))).thenReturn(endedShowDetailsWithId(9602L));

        MvcResult mvcResult = mockMvc.perform(put("/api/v1/shows/{tmdbId}/status", 9602L)
            .header("Authorization", bearerToken(user))
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content("""
                {"status":"WATCHED"}
                """))
            .andExpect(status().isAccepted())
            .andReturn();

        ShowTrackingJobDTO job = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ShowTrackingJobDTO.class);
        when(tmdbClient.fetchTvSeasonDetails(eq(9602L), eq(1)))
            .thenThrow(new com.project.watchmate.common.error.TmdbUnavailableException("TMDB temporarily unavailable"));

        showTrackingJobService.pollPendingJobs();

        assertThat(showTrackingJobRepository.findById(job.getJobId())).get()
            .extracting(com.project.watchmate.show.jobs.domain.ShowTrackingJob::getStatus)
            .isEqualTo(ShowTrackingJobStatus.FAILED);
        UserShowTracking persistedTracking = userShowTrackingRepository.findByUserAndMedia(user, show).orElseThrow();
        assertThat(persistedTracking.getStatus()).isEqualTo(WatchStatus.WATCHING);
        assertThat(userEpisodeWatchRepository.findByUserShowTrackingOrderBySeasonNumberAscEpisodeNumberAsc(persistedTracking)).isEmpty();
    }

    @Test
    void showTrackingJobEndpoint_returns404ForDifferentUser() throws Exception {
        Users owner = saveUser("show-job-owner", true);
        Users otherUser = saveUser("show-job-other", true);
        Media show = saveMedia(9603L, "Job Visibility Show", MediaType.SHOW);
        ShowTrackingJob job = showTrackingJobRepository.save(ShowTrackingJob.builder()
            .user(owner)
            .media(show)
            .jobType(ShowTrackingJobType.MARK_SHOW_WATCHED)
            .status(ShowTrackingJobStatus.PENDING)
            .requestedStatus(WatchStatus.WATCHED)
            .totalSeasons(4)
            .completedSeasons(0)
            .build());

        mockMvc.perform(get("/api/v1/show-tracking-jobs/{jobId}", job.getId())
            .header("Authorization", bearerToken(otherUser)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("SHOW_TRACKING_JOB_NOT_FOUND"));
    }

    @Test
    void claimPendingJob_allowsOnlyOneSuccessfulClaim() {
        Users owner = saveUser("show-job-claim-user", true);
        Media show = saveMedia(9604L, "Claimable Job Show", MediaType.SHOW);
        ShowTrackingJob job = showTrackingJobRepository.save(ShowTrackingJob.builder()
            .user(owner)
            .media(show)
            .jobType(ShowTrackingJobType.MARK_SHOW_UP_TO_DATE)
            .status(ShowTrackingJobStatus.PENDING)
            .requestedStatus(WatchStatus.UP_TO_DATE)
            .totalSeasons(4)
            .completedSeasons(0)
            .build());

        assertThat(showTrackingJobService.claimPendingJob(job.getId())).isTrue();
        assertThat(showTrackingJobService.claimPendingJob(job.getId())).isFalse();
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

    private TmdbTvDetailsDTO contiguousProgressShowDetailsWithId(Long tmdbId) {
        return TmdbTvDetailsDTO.builder()
            .id(tmdbId)
            .name("Contiguous Progress Show")
            .overview("A show used for contiguous progress tests")
            .posterPath("/contiguous.jpg")
            .backdropPath("/contiguous-bg.jpg")
            .firstAirDate("2020-01-01")
            .voteAverage(8.5)
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
                TmdbTvSeasonSummaryDTO.builder().id(401L).seasonNumber(1).name("Season 1").episodeCount(5).airDate("2020-01-01").build(),
                TmdbTvSeasonSummaryDTO.builder().id(402L).seasonNumber(2).name("Season 2").episodeCount(3).airDate("2026-01-01").build()
            ))
            .build();
    }

    private TmdbTvDetailsDTO largeProgressShowDetailsWithId(Long tmdbId) {
        return TmdbTvDetailsDTO.builder()
            .id(tmdbId)
            .name("Large Progress Show")
            .overview("A show used for async progress tests")
            .posterPath("/large-progress.jpg")
            .backdropPath("/large-progress-bg.jpg")
            .firstAirDate("2020-01-01")
            .voteAverage(8.0)
            .numberOfSeasons(6)
            .numberOfEpisodes(6)
            .lastAirDate("2020-02-12")
            .status("Returning Series")
            .genres(List.of())
            .seasons(List.of(
                TmdbTvSeasonSummaryDTO.builder().id(501L).seasonNumber(1).name("Season 1").episodeCount(1).airDate("2020-01-01").build(),
                TmdbTvSeasonSummaryDTO.builder().id(502L).seasonNumber(2).name("Season 2").episodeCount(1).airDate("2020-01-08").build(),
                TmdbTvSeasonSummaryDTO.builder().id(503L).seasonNumber(3).name("Season 3").episodeCount(1).airDate("2020-01-15").build(),
                TmdbTvSeasonSummaryDTO.builder().id(504L).seasonNumber(4).name("Season 4").episodeCount(1).airDate("2020-01-22").build(),
                TmdbTvSeasonSummaryDTO.builder().id(505L).seasonNumber(5).name("Season 5").episodeCount(1).airDate("2020-02-05").build(),
                TmdbTvSeasonSummaryDTO.builder().id(506L).seasonNumber(6).name("Season 6").episodeCount(1).airDate("2020-02-12").build()
            ))
            .build();
    }

    private TmdbTvDetailsDTO endedShowDetailsWithId(Long tmdbId) {
        return TmdbTvDetailsDTO.builder()
            .id(tmdbId)
            .name("Ended Show")
            .overview("A fully ended show payload")
            .posterPath("/ended.jpg")
            .backdropPath("/ended-bg.jpg")
            .firstAirDate("2020-01-01")
            .voteAverage(8.3)
            .numberOfSeasons(4)
            .numberOfEpisodes(4)
            .lastAirDate("2020-01-22")
            .status("Ended")
            .genres(List.of())
            .seasons(List.of(
                TmdbTvSeasonSummaryDTO.builder().id(201L).seasonNumber(1).name("Season 1").episodeCount(1).airDate("2020-01-01").build(),
                TmdbTvSeasonSummaryDTO.builder().id(202L).seasonNumber(2).name("Season 2").episodeCount(1).airDate("2020-01-08").build(),
                TmdbTvSeasonSummaryDTO.builder().id(203L).seasonNumber(3).name("Season 3").episodeCount(1).airDate("2020-01-15").build(),
                TmdbTvSeasonSummaryDTO.builder().id(204L).seasonNumber(4).name("Season 4").episodeCount(1).airDate("2020-01-22").build()
            ))
            .build();
    }

    private TmdbTvDetailsDTO largeOngoingShowDetailsWithId(Long tmdbId) {
        return TmdbTvDetailsDTO.builder()
            .id(tmdbId)
            .name("Large Ongoing Show")
            .overview("A large ongoing show payload")
            .posterPath("/ongoing-large.jpg")
            .backdropPath("/ongoing-large-bg.jpg")
            .firstAirDate("2020-01-01")
            .voteAverage(8.1)
            .numberOfSeasons(4)
            .numberOfEpisodes(4)
            .lastAirDate("2026-04-01")
            .status("Returning Series")
            .genres(List.of())
            .seasons(List.of(
                TmdbTvSeasonSummaryDTO.builder().id(301L).seasonNumber(1).name("Season 1").episodeCount(1).airDate("2020-01-01").build(),
                TmdbTvSeasonSummaryDTO.builder().id(302L).seasonNumber(2).name("Season 2").episodeCount(1).airDate("2021-01-01").build(),
                TmdbTvSeasonSummaryDTO.builder().id(303L).seasonNumber(3).name("Season 3").episodeCount(1).airDate("2022-01-01").build(),
                TmdbTvSeasonSummaryDTO.builder().id(304L).seasonNumber(4).name("Season 4").episodeCount(1).airDate("2026-04-01").build()
            ))
            .build();
    }


    private TmdbTvSeasonDTO contiguousSeasonOne() {
        return TmdbTvSeasonDTO.builder()
            .id(401L)
            .seasonNumber(1)
            .name("Season 1")
            .posterPath("/contiguous-s1.jpg")
            .episodes(List.of(
                TmdbTvEpisodeDTO.builder().id(4101L).seasonNumber(1).episodeNumber(1).name("Episode 1").airDate("2020-01-01").runtime(45).stillPath("/c-s1-e1.jpg").build(),
                TmdbTvEpisodeDTO.builder().id(4102L).seasonNumber(1).episodeNumber(2).name("Episode 2").airDate("2020-01-08").runtime(45).stillPath("/c-s1-e2.jpg").build(),
                TmdbTvEpisodeDTO.builder().id(4103L).seasonNumber(1).episodeNumber(3).name("Episode 3").airDate("2020-01-15").runtime(45).stillPath("/c-s1-e3.jpg").build(),
                TmdbTvEpisodeDTO.builder().id(4104L).seasonNumber(1).episodeNumber(4).name("Episode 4").airDate("2020-01-22").runtime(45).stillPath("/c-s1-e4.jpg").build(),
                TmdbTvEpisodeDTO.builder().id(4105L).seasonNumber(1).episodeNumber(5).name("Episode 5").airDate("2020-01-29").runtime(45).stillPath("/c-s1-e5.jpg").build()
            ))
            .build();
    }

    private TmdbTvSeasonDTO contiguousSeasonTwo() {
        return TmdbTvSeasonDTO.builder()
            .id(402L)
            .seasonNumber(2)
            .name("Season 2")
            .posterPath("/contiguous-s2.jpg")
            .episodes(List.of(
                TmdbTvEpisodeDTO.builder().id(4201L).seasonNumber(2).episodeNumber(1).name("Episode 1").airDate("2026-01-01").runtime(45).stillPath("/c-s2-e1.jpg").build(),
                TmdbTvEpisodeDTO.builder().id(4202L).seasonNumber(2).episodeNumber(2).name("Episode 2").airDate("2026-01-08").runtime(45).stillPath("/c-s2-e2.jpg").build(),
                TmdbTvEpisodeDTO.builder().id(4203L).seasonNumber(2).episodeNumber(3).name("Next Episode").airDate("2099-01-15").runtime(45).stillPath("/c-s2-e3.jpg").build()
            ))
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

    private TmdbTvSeasonDTO singleEpisodeSeason(int seasonNumber, String airDate) {
        return TmdbTvSeasonDTO.builder()
            .id(3000L + seasonNumber)
            .seasonNumber(seasonNumber)
            .name("Season " + seasonNumber)
            .airDate(airDate)
            .episodeCount(1)
            .episodes(List.of(
                TmdbTvEpisodeDTO.builder()
                    .id(4000L + seasonNumber)
                    .seasonNumber(seasonNumber)
                    .episodeNumber(1)
                    .name("Episode " + seasonNumber)
                    .airDate(airDate)
                    .runtime(45)
                    .stillPath("/episode-" + seasonNumber + ".jpg")
                    .build()
            ))
            .build();
    }

    private void assertWatchedRows(UserShowTracking tracking, String... expected) {
        assertThat(userEpisodeWatchRepository.findByUserShowTrackingOrderBySeasonNumberAscEpisodeNumberAsc(tracking))
            .extracting(row -> row.getSeasonNumber() + "x" + row.getEpisodeNumber())
            .containsExactly(expected);
    }
}

