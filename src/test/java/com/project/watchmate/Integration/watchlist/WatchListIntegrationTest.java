package com.project.watchmate.Integration.watchlist;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.project.watchmate.Integration.support.AbstractIntegrationTest;
import com.project.watchmate.Models.Users;
import com.project.watchmate.Models.WatchList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WatchListIntegrationTest extends AbstractIntegrationTest {

	@Test
	void createWatchlist_returns201_forAuthenticatedUser() throws Exception {
		Users user = saveUser("watchlist-create-user", true);

		mockMvc.perform(post("/api/v1/watchlists")
			.header("Authorization", bearerToken(user))
			.contentType(MediaType.APPLICATION_JSON)
			.content(createWatchListBody("Weekend Movies")))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.id").isNumber())
			.andExpect(jsonPath("$.name").value("Weekend Movies"));

		WatchList watchList = watchListRepository.findByUserAndNameIgnoreCase(user, "Weekend Movies").orElseThrow();

		assertThat(watchList.getUser().getId()).isEqualTo(user.getId());
	}

	@Test
	void getWatchlists_returnsOnlyAuthenticatedUsersWatchlists() throws Exception {
		Users owner = saveUser("watchlist-owner", true);
		Users otherUser = saveUser("watchlist-other", true);
		saveWatchList(owner, "Owner List A");
		saveWatchList(owner, "Owner List B");
		saveWatchList(otherUser, "Other List");

		mockMvc.perform(get("/api/v1/watchlists")
			.header("Authorization", bearerToken(owner)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$").isArray())
			.andExpect(jsonPath("$", hasSize(2)))
			.andExpect(jsonPath("$[*].name", containsInAnyOrder("Owner List A", "Owner List B")));
	}

	@Test
	void renameWatchlist_returns200_forOwner() throws Exception {
		Users owner = saveUser("watchlist-rename-owner", true);
		WatchList watchList = saveWatchList(owner, "Before Rename");

		mockMvc.perform(patch("/api/v1/watchlists/{id}", watchList.getId())
			.header("Authorization", bearerToken(owner))
			.contentType(MediaType.APPLICATION_JSON)
			.content(renameWatchListBody("After Rename")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(watchList.getId().intValue()))
			.andExpect(jsonPath("$.name").value("After Rename"));

		assertThat(watchListRepository.findById(watchList.getId()).orElseThrow().getName())
			.isEqualTo("After Rename");
	}

	@Test
	void deleteWatchlist_returns204_forOwner() throws Exception {
		Users owner = saveUser("watchlist-delete-owner", true);
		WatchList watchList = saveWatchList(owner, "Delete Me");

		mockMvc.perform(delete("/api/v1/watchlists/{id}", watchList.getId())
			.header("Authorization", bearerToken(owner)))
			.andExpect(status().isNoContent());

		assertThat(watchListRepository.findById(watchList.getId())).isEmpty();
	}

	@Test
	void deleteWatchlist_returns403_forNonOwner() throws Exception {
		Users owner = saveUser("watchlist-real-owner", true);
		Users nonOwner = saveUser("watchlist-non-owner", true);
		WatchList watchList = saveWatchList(owner, "Owner Only");

		mockMvc.perform(delete("/api/v1/watchlists/{id}", watchList.getId())
			.header("Authorization", bearerToken(nonOwner)))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("UNAUTHORIZED_WATCHLIST_ACCESS"));

		assertThat(watchListRepository.findById(watchList.getId())).isPresent();
	}

	private WatchList saveWatchList(Users user, String name) {
		return watchListRepository.save(WatchList.builder()
			.name(name)
			.user(user)
			.build());
	}

	private String bearerToken(Users user) {
		return "Bearer " + createAccessToken(user);
	}

	private String createWatchListBody(String name) {
		return """
			{"name":"%s"}
			""".formatted(name);
	}

	private String renameWatchListBody(String newName) {
		return """
			{"newName":"%s"}
			""".formatted(newName);
	}
}
