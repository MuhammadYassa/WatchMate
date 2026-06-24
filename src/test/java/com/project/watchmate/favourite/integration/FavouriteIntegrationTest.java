package com.project.watchmate.favourite.integration;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.project.watchmate.media.tmdb.dto.TmdbMovieDTO;
import com.project.watchmate.common.integration.support.AbstractIntegrationTest;
import com.project.watchmate.media.catalog.domain.Media;
import com.project.watchmate.media.catalog.domain.MediaType;
import com.project.watchmate.user.domain.Users;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// NOTE: These integration tests require Docker/Testcontainers (MySQL + Redis).
// They cannot run in environments without Docker.
class FavouriteIntegrationTest extends AbstractIntegrationTest {

	@Test
	void addFavouriteByTmdbId_returns200_andImportsMissingMedia() throws Exception {
		Users user = saveUser("favourite-user", true);

		when(tmdbClient.fetchMediaById(eq(9101L), eq(MediaType.MOVIE)))
			.thenReturn(TmdbMovieDTO.builder()
				.id(9101L)
				.title("Favourite Import Movie")
				.overview("Imported for favourite")
				.posterPath("/favourite.jpg")
				.releaseDate("2024-02-03")
				.voteAverage(7.8)
				.genres(List.of())
				.build());

		mockMvc.perform(post("/api/v1/favourites/{tmdbId}", 9101L)
			.header("Authorization", bearerToken(user))
			.param("type", "MOVIE"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.tmdbId").value(9101))
			.andExpect(jsonPath("$.isFavourited").value(true));

		assertThat(mediaRepository.findByTmdbIdAndType(9101L, MediaType.MOVIE)).isPresent();
		Integer favouriteCount = jdbcTemplate.queryForObject(
			"select count(*) from user_favorites where users_id = ?",
			Integer.class,
			user.getId());
		assertThat(favouriteCount).isEqualTo(1);
	}

	@Test
	void getFavourites_defaultPagination_returnsPageOf20() throws Exception {
		Users user = saveUser("fav-list-user", true);
		for (int i = 1; i <= 25; i++) {
			Media m = saveMedia((long) (9200 + i), "Movie " + i, MediaType.MOVIE);
			jdbcTemplate.update(
				"insert into user_favorites (users_id, favorites_id) values (?, ?)",
				user.getId(), m.getId());
		}

		mockMvc.perform(get("/api/v1/favourites")
			.header("Authorization", bearerToken(user)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content.length()").value(20))
			.andExpect(jsonPath("$.totalElements").value(25))
			.andExpect(jsonPath("$.totalPages").value(2))
			.andExpect(jsonPath("$.size").value(20))
			.andExpect(jsonPath("$.number").value(0));
	}

	@Test
	void getFavourites_customPageAndSize_returnsPaginatedSubset() throws Exception {
		Users user = saveUser("fav-custom-page-user", true);
		for (int i = 1; i <= 5; i++) {
			Media m = saveMedia((long) (9300 + i), "Custom Movie " + i, MediaType.MOVIE);
			jdbcTemplate.update(
				"insert into user_favorites (users_id, favorites_id) values (?, ?)",
				user.getId(), m.getId());
		}

		mockMvc.perform(get("/api/v1/favourites")
			.header("Authorization", bearerToken(user))
			.param("page", "1")
			.param("size", "2"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content.length()").value(2))
			.andExpect(jsonPath("$.totalElements").value(5))
			.andExpect(jsonPath("$.number").value(1));
	}

	@Test
	void getFavourites_sizeCapAt50_returnsAtMost50() throws Exception {
		Users user = saveUser("fav-cap-user", true);
		for (int i = 1; i <= 60; i++) {
			Media m = saveMedia((long) (9400 + i), "Cap Movie " + i, MediaType.MOVIE);
			jdbcTemplate.update(
				"insert into user_favorites (users_id, favorites_id) values (?, ?)",
				user.getId(), m.getId());
		}

		mockMvc.perform(get("/api/v1/favourites")
			.header("Authorization", bearerToken(user))
			.param("size", "200"))
			.andExpect(status().isBadRequest()); // @Max(50) constraint returns 400
	}

	@Test
	void getFavourites_unauthenticated_returns401() throws Exception {
		mockMvc.perform(get("/api/v1/favourites"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void getFavourites_emptyFavourites_returnsEmptyPage() throws Exception {
		Users user = saveUser("fav-empty-user", true);

		mockMvc.perform(get("/api/v1/favourites")
			.header("Authorization", bearerToken(user)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content.length()").value(0))
			.andExpect(jsonPath("$.totalElements").value(0));
	}
}





