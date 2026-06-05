package com.project.watchmate.discovery.integration;

import static org.hamcrest.Matchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.project.watchmate.media.tmdb.dto.TmdbMovieDTO;
import com.project.watchmate.media.tmdb.dto.TmdbResponseDTO;
import com.project.watchmate.common.integration.support.AbstractIntegrationTest;
import com.project.watchmate.discovery.domain.ContentSyncResult;
import com.project.watchmate.discovery.domain.ContentSyncStatus;
import com.project.watchmate.discovery.domain.CuratedContent;
import com.project.watchmate.discovery.domain.CuratedContentCategory;
import com.project.watchmate.media.catalog.domain.Genre;
import com.project.watchmate.media.catalog.domain.Media;
import com.project.watchmate.media.catalog.domain.MediaType;
import com.project.watchmate.user.domain.Role;
import com.project.watchmate.user.domain.Users;
import com.project.watchmate.discovery.application.CuratedContentSyncService;

class DiscoveryIntegrationTest extends AbstractIntegrationTest {

    @Test
    void home_returnsCachedBucketsAndGenresWithoutAuth() throws Exception {
        Media trendingMovie = saveMedia(9301L, "Home Trending", MediaType.MOVIE);
        Media recommended = saveMedia(9302L, "Home Recommended", MediaType.SHOW);

        curatedContentRepository.save(CuratedContent.builder()
            .categoryKey(CuratedContentCategory.TRENDING_MOVIES)
            .media(trendingMovie)
            .mediaType(MediaType.MOVIE)
            .rankPosition(1)
            .syncedAt(LocalDateTime.now())
            .build());
        curatedContentRepository.save(CuratedContent.builder()
            .categoryKey(CuratedContentCategory.RECOMMENDED_LATER)
            .media(recommended)
            .mediaType(MediaType.SHOW)
            .rankPosition(1)
            .syncedAt(LocalDateTime.now())
            .build());
        genreRepository.save(Genre.builder()
            .tmdbGenreId(28L)
            .name("Action")
            .mediaType(MediaType.MOVIE)
            .syncedAt(LocalDateTime.now())
            .build());
        genreRepository.save(Genre.builder()
            .tmdbGenreId(18L)
            .name("Drama")
            .mediaType(MediaType.SHOW)
            .syncedAt(LocalDateTime.now())
            .build());

        mockMvc.perform(get("/api/v1/home"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.trendingMovies[0].title").value("Home Trending"))
            .andExpect(jsonPath("$.recommendedLater[0].title").value("Home Recommended"))
            .andExpect(jsonPath("$.movieGenres", contains("Action")))
            .andExpect(jsonPath("$.showGenres", contains("Drama")));
    }

    @Test
    void homeStatus_returns401_withoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/home/status"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("Authentication failed"))
            .andExpect(jsonPath("$.code").value("AUTH_FAILED"));
    }

    @Test
    void homeStatus_returns403_forNonAdmin() throws Exception {
        Users user = saveUser("home-status-user", true);

        mockMvc.perform(get("/api/v1/home/status")
            .header("Authorization", bearerToken(user)))
            .andExpect(status().isForbidden());
    }

    @Test
    void homeStatus_returnsPersistedStatus_forAdmin() throws Exception {
        Users admin = saveUser("home-status-admin", true, Role.ADMIN);

        contentSyncStatusRepository.save(ContentSyncStatus.builder()
            .statusKey(CuratedContentSyncService.STATUS_KEY)
            .lastResult(ContentSyncResult.SUCCESS)
            .trendingMoviesCount(20)
            .trendingShowsCount(20)
            .popularNowCount(15)
            .airingTodayCount(9)
            .upcomingCount(11)
            .recommendedLaterCount(7)
            .build());

        mockMvc.perform(get("/api/v1/home/status")
            .header("Authorization", bearerToken(admin)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.lastResult").value("SUCCESS"))
            .andExpect(jsonPath("$.popularNowCount").value(15))
            .andExpect(jsonPath("$.recommendedLaterCount").value(7));
    }

    @Test
    void discoverTrendingMovies_returnsCachedBucketWithoutAuth() throws Exception {
        Media first = saveMedia(9401L, "Trending One", MediaType.MOVIE);
        Media second = saveMedia(9402L, "Trending Two", MediaType.MOVIE);
        curatedContentRepository.save(CuratedContent.builder()
            .categoryKey(CuratedContentCategory.TRENDING_MOVIES)
            .media(first)
            .mediaType(MediaType.MOVIE)
            .rankPosition(1)
            .syncedAt(LocalDateTime.now())
            .build());
        curatedContentRepository.save(CuratedContent.builder()
            .categoryKey(CuratedContentCategory.TRENDING_MOVIES)
            .media(second)
            .mediaType(MediaType.MOVIE)
            .rankPosition(2)
            .syncedAt(LocalDateTime.now())
            .build());

        mockMvc.perform(get("/api/v1/discover/trending-movies"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].title").value("Trending One"))
            .andExpect(jsonPath("$[1].title").value("Trending Two"));
    }

    @Test
    void genreMovies_returnsPagedResultsWhenGenreExists() throws Exception {
        genreRepository.save(Genre.builder()
            .tmdbGenreId(28L)
            .name("Action")
            .mediaType(MediaType.MOVIE)
            .syncedAt(LocalDateTime.now())
            .build());
        when(tmdbClient.discoverByGenre(eq("movie"), eq(28L), eq(1)))
            .thenReturn(new TmdbResponseDTO(List.of(TmdbMovieDTO.builder()
                .id(9501L)
                .title("Genre Movie")
                .overview("Genre overview")
                .posterPath("/genre.jpg")
                .releaseDate("2024-06-01")
                .voteAverage(7.6)
                .build()), 1, 3, 60));

        mockMvc.perform(get("/api/v1/genre/Action/movies")
            .param("page", "1")
            .param("size", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.genre").value("Action"))
            .andExpect(jsonPath("$.mediaType").value("MOVIE"))
            .andExpect(jsonPath("$.results[0].title").value("Genre Movie"))
            .andExpect(jsonPath("$.totalResults").value(60));
    }

    @Test
    void genreShows_returns404WhenGenreMissing() throws Exception {
        mockMvc.perform(get("/api/v1/genre/Horror/shows"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("GENRE_NOT_FOUND"));
    }

    @Test
    void publicDiscoveryRoutes_areAccessibleWithoutAuthentication() throws Exception {
        Media upcoming = saveMedia(9601L, "Public Upcoming", MediaType.MOVIE);
        curatedContentRepository.save(CuratedContent.builder()
            .categoryKey(CuratedContentCategory.UPCOMING)
            .media(upcoming)
            .mediaType(MediaType.MOVIE)
            .rankPosition(1)
            .syncedAt(LocalDateTime.now())
            .build());

        mockMvc.perform(get("/api/v1/discover/upcoming"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].title").value("Public Upcoming"));
    }
}






