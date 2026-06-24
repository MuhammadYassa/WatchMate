package com.project.watchmate.media.integration;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.project.watchmate.media.tmdb.dto.TmdbCastMemberDTO;
import com.project.watchmate.media.tmdb.dto.TmdbCreditsDTO;
import com.project.watchmate.media.tmdb.dto.TmdbMovieDTO;
import com.project.watchmate.media.tmdb.dto.TmdbResponseDTO;
import com.project.watchmate.media.tmdb.dto.TmdbVideoDTO;
import com.project.watchmate.media.tmdb.dto.TmdbVideosResponseDTO;
import com.project.watchmate.media.tmdb.dto.TmdbWatchProviderEntryDTO;
import com.project.watchmate.media.tmdb.dto.TmdbWatchProviderRegionDTO;
import com.project.watchmate.media.tmdb.dto.TmdbWatchProvidersResponseDTO;
import com.project.watchmate.common.integration.support.AbstractIntegrationTest;
import com.project.watchmate.media.catalog.domain.Media;
import com.project.watchmate.media.catalog.domain.MediaType;
import com.project.watchmate.user.domain.Users;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ExternalMediaIntegrationTest extends AbstractIntegrationTest {

	@Test
	void search_returns200_whenTmdbClientProvidesResults() throws Exception {
		when(tmdbClient.searchMulti(eq("matrix"), eq(1)))
			.thenReturn(new TmdbResponseDTO(List.of(TmdbMovieDTO.builder()
				.id(603L)
				.title("The Matrix")
				.mediaType("movie")
				.overview("A computer hacker learns about the true nature of reality.")
				.posterPath("/matrix.jpg")
				.releaseDate("1999-03-31")
				.voteAverage(8.2)
				.genreIds(List.of())
				.build()), 1, 1, 1));

		mockMvc.perform(get("/api/v1/media/search")
			.param("query", "matrix")
			.param("page", "1"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.currentPage").value(1))
			.andExpect(jsonPath("$.totalPages").value(1))
			.andExpect(jsonPath("$.totalResults").value(1))
			.andExpect(jsonPath("$.searchResults[0].id").value(603))
			.andExpect(jsonPath("$.searchResults[0].title").value("The Matrix"));

		verify(tmdbClient).searchMulti(eq("matrix"), eq(1));
	}

	@Test
	void search_withPageBelowMinimum_returns400() throws Exception {
		mockMvc.perform(get("/api/v1/media/search")
			.param("query", "matrix")
			.param("page", "0"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}

	@Test
	void search_withPageAboveMaximum_returns400() throws Exception {
		mockMvc.perform(get("/api/v1/media/search")
			.param("query", "matrix")
			.param("page", "501"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}

	@Test
	void mediaDetails_fetchesAndPersistsMediaWhenMissing() throws Exception {
		Users user = saveUser("external-details-user", true);
		when(tmdbClient.fetchMediaById(eq(8101L), eq(MediaType.MOVIE)))
			.thenReturn(TmdbMovieDTO.builder()
				.id(8101L)
				.title("Fetched Movie")
				.overview("Fetched overview")
				.posterPath("/fetched.jpg")
				.releaseDate("2020-01-02")
				.voteAverage(7.4)
				.genres(List.of())
				.build());
		when(tmdbClient.fetchCredits(eq(8101L), eq(MediaType.MOVIE))).thenReturn(TmdbCreditsDTO.builder()
			.cast(List.of(TmdbCastMemberDTO.builder()
				.id(101L)
				.name("Fetched Actor")
				.character("Lead")
				.profilePath("/actor.jpg")
				.order(0)
				.knownForDepartment("Acting")
				.build()))
			.build());
		when(tmdbClient.fetchVideos(eq(8101L), eq(MediaType.MOVIE))).thenReturn(TmdbVideosResponseDTO.builder()
			.results(List.of(TmdbVideoDTO.builder()
				.key("movie-trailer")
				.name("Fetched Trailer")
				.site("YouTube")
				.type("Trailer")
				.official(true)
				.publishedAt("2026-01-01T00:00:00.000Z")
				.build()))
			.build());
		when(tmdbClient.fetchWatchProviders(eq(8101L), eq(MediaType.MOVIE))).thenReturn(TmdbWatchProvidersResponseDTO.builder()
			.results(Map.of("US", TmdbWatchProviderRegionDTO.builder()
				.link("https://example.com/us/movie")
				.flatrate(List.of(TmdbWatchProviderEntryDTO.builder()
					.providerId(1)
					.providerName("StreamCo")
					.logoPath("/streamco.jpg")
					.displayPriority(0)
					.build()))
				.build()))
			.build());

        mockMvc.perform(get("/api/v1/movies/{tmdbId}", 8101L)
            .header("Authorization", bearerToken(user)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tmdbId").value(8101))
            .andExpect(jsonPath("$.title").value("Fetched Movie"))
			.andExpect(jsonPath("$.type").value("MOVIE"))
			.andExpect(jsonPath("$.watchStatus").value("NONE"))
			.andExpect(jsonPath("$.cast", hasSize(1)))
			.andExpect(jsonPath("$.cast[0].tmdbPersonId").value(101))
			.andExpect(jsonPath("$.cast[0].name").value("Fetched Actor"))
			.andExpect(jsonPath("$.bestTrailer.key").value("movie-trailer"))
			.andExpect(jsonPath("$.bestTrailer.youtubeUrl").value("https://www.youtube.com/watch?v=movie-trailer"))
			.andExpect(jsonPath("$.watchProviders.region").value("US"))
			.andExpect(jsonPath("$.watchProviders.flatrate[0].providerName").value("StreamCo"));

		Media persisted = mediaRepository.findByTmdbIdAndType(8101L, MediaType.MOVIE).orElseThrow();

		assertThat(persisted.getTitle()).isEqualTo("Fetched Movie");
		assertThat(persisted.getType()).isEqualTo(MediaType.MOVIE);
	}

	@Test
	void mediaDetails_whenTmdbExtrasEmpty_returnsDefaultExtrasShape() throws Exception {
		when(tmdbClient.fetchMediaById(eq(8102L), eq(MediaType.MOVIE)))
			.thenReturn(TmdbMovieDTO.builder()
				.id(8102L)
				.title("No Extras Movie")
				.overview("No extras overview")
				.posterPath("/no-extras.jpg")
				.releaseDate("2020-01-02")
				.voteAverage(7.4)
				.genres(List.of())
				.build());

		mockMvc.perform(get("/api/v1/movies/{tmdbId}", 8102L))
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
}





