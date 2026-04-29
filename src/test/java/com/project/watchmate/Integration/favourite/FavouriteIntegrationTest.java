package com.project.watchmate.Integration.favourite;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.project.watchmate.Dto.TmdbMovieDTO;
import com.project.watchmate.Integration.support.AbstractIntegrationTest;
import com.project.watchmate.Models.MediaType;
import com.project.watchmate.Models.Users;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

		mockMvc.perform(post("/api/v1/favourites/add/{tmdbId}", 9101L)
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
}
