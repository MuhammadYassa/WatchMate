package com.project.watchmate.Integration.media;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.project.watchmate.Dto.TmdbMovieDTO;
import com.project.watchmate.Dto.TmdbResponseDTO;
import com.project.watchmate.Integration.support.AbstractIntegrationTest;
import com.project.watchmate.Models.Media;
import com.project.watchmate.Models.MediaType;
import com.project.watchmate.Models.PopularMedia;
import com.project.watchmate.Models.Users;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.mockito.ArgumentMatchers.eq;
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

		mockMvc.perform(get("/api/v1/media/{tmdbId}", 8101L)
			.header("Authorization", bearerToken(user))
			.param("type", "movie"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.tmdbId").value(8101))
			.andExpect(jsonPath("$.title").value("Fetched Movie"))
			.andExpect(jsonPath("$.type").value("MOVIE"))
			.andExpect(jsonPath("$.watchStatus").value("NONE"));

		Media persisted = mediaRepository.findByTmdbId(8101L).orElseThrow();

		assertThat(persisted.getTitle()).isEqualTo("Fetched Movie");
		assertThat(persisted.getType()).isEqualTo(MediaType.MOVIE);
	}

	@Test
	void popularMedia_returnsPersistedRankedItems() throws Exception {
		Media first = saveMedia(8201L, "Popular One", MediaType.MOVIE);
		Media second = saveMedia(8202L, "Popular Two", MediaType.SHOW);
		popularMediaRepository.save(PopularMedia.builder().media(first).popularityRank(1).build());
		popularMediaRepository.save(PopularMedia.builder().media(second).popularityRank(2).build());

		mockMvc.perform(get("/api/v1/media/popular"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(2))
			.andExpect(jsonPath("$[?(@.rank == 1)].title", contains("Popular One")))
			.andExpect(jsonPath("$[?(@.rank == 2)].title", contains("Popular Two")));
	}
}
