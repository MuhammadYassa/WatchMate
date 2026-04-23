package com.project.watchmate.Integration.watchlist;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.project.watchmate.Integration.support.AbstractIntegrationTest;
import com.project.watchmate.Models.Media;
import com.project.watchmate.Models.Role;
import com.project.watchmate.Models.Users;
import com.project.watchmate.Models.WatchList;
import com.project.watchmate.Models.WatchListItem;

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

	@Test
	void addMediaToWatchlist_returns200_andPersistsItem() throws Exception {
		Users owner = saveUser("watchlist-add-owner", true);
		WatchList watchList = saveWatchList(owner, "With Items");
		Media media = saveMedia(9001L, "Item Movie", com.project.watchmate.Models.MediaType.MOVIE);

		mockMvc.perform(post("/api/v1/watchlists/{watchListId}/items/{tmdbId}", watchList.getId(), media.getTmdbId())
			.header("Authorization", bearerToken(owner)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(watchList.getId().intValue()))
			.andExpect(jsonPath("$.media[0].tmdbId").value(media.getTmdbId().intValue()))
			.andExpect(jsonPath("$.media[0].title").value(media.getTitle()));

		Integer itemCount = jdbcTemplate.queryForObject(
			"select count(*) from watchlist_items where watchlist_id = ? and media_id = ?",
			Integer.class,
			watchList.getId(),
			media.getId());

		assertThat(itemCount).isEqualTo(1);
	}

	@Test
	void removeMediaFromWatchlist_returns200_andRemovesItem() throws Exception {
		Users owner = saveUser("watchlist-remove-owner", true);
		WatchList watchList = saveWatchList(owner, "Remove Items");
		Media media = saveMedia(9002L, "Removable Movie", com.project.watchmate.Models.MediaType.MOVIE);

		mockMvc.perform(post("/api/v1/watchlists/{watchListId}/items/{tmdbId}", watchList.getId(), media.getTmdbId())
			.header("Authorization", bearerToken(owner)))
			.andExpect(status().isOk());

		mockMvc.perform(delete("/api/v1/watchlists/{watchListId}/items/{tmdbId}", watchList.getId(), media.getTmdbId())
			.header("Authorization", bearerToken(owner)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(watchList.getId().intValue()))
			.andExpect(jsonPath("$.media").isEmpty());

		Integer itemCount = jdbcTemplate.queryForObject(
			"select count(*) from watchlist_items where watchlist_id = ? and media_id = ?",
			Integer.class,
			watchList.getId(),
			media.getId());

		assertThat(itemCount).isZero();
	}

	@Test
	void removeMediaFromWatchlist_returns403_forNonOwnerUser() throws Exception {
		Users owner = saveUser("watchlist-remove-user-owner", true);
		Users nonOwner = saveUser("watchlist-remove-non-owner", true);
		WatchList watchList = saveWatchList(owner, "User Protected Items");
		Media media = saveMedia(9003L, "Protected Movie", com.project.watchmate.Models.MediaType.MOVIE);
		saveWatchListItem(watchList, media);

		mockMvc.perform(delete("/api/v1/watchlists/{watchListId}/items/{tmdbId}", watchList.getId(), media.getTmdbId())
			.header("Authorization", bearerToken(nonOwner)))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("UNAUTHORIZED_WATCHLIST_ACCESS"));

		assertWatchListItemCount(watchList, media, 1);
	}

	@Test
	void removeMediaFromWatchlist_returns403_forModeratorNonOwner() throws Exception {
		Users owner = saveUser("watchlist-remove-mod-owner", true);
		Users moderator = saveUser("watchlist-remove-moderator", true, Role.MODERATOR);
		WatchList watchList = saveWatchList(owner, "Moderator Protected Items");
		Media media = saveMedia(9004L, "Moderator Protected Movie", com.project.watchmate.Models.MediaType.MOVIE);
		saveWatchListItem(watchList, media);

		mockMvc.perform(delete("/api/v1/watchlists/{watchListId}/items/{tmdbId}", watchList.getId(), media.getTmdbId())
			.header("Authorization", bearerToken(moderator)))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("UNAUTHORIZED_WATCHLIST_ACCESS"));

		assertWatchListItemCount(watchList, media, 1);
	}

	@Test
	void removeMediaFromWatchlist_returns200_forAdminNonOwner() throws Exception {
		Users owner = saveUser("watchlist-remove-admin-owner", true);
		Users admin = saveUser("watchlist-remove-admin", true, Role.ADMIN);
		WatchList watchList = saveWatchList(owner, "Admin Removable Items");
		Media media = saveMedia(9005L, "Admin Removable Movie", com.project.watchmate.Models.MediaType.MOVIE);
		saveWatchListItem(watchList, media);

		mockMvc.perform(delete("/api/v1/watchlists/{watchListId}/items/{tmdbId}", watchList.getId(), media.getTmdbId())
			.header("Authorization", bearerToken(admin)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(watchList.getId().intValue()))
			.andExpect(jsonPath("$.media").isEmpty());

		assertWatchListItemCount(watchList, media, 0);
	}

	private WatchList saveWatchList(Users user, String name) {
		return watchListRepository.save(WatchList.builder()
			.name(name)
			.user(user)
			.build());
	}

	private void saveWatchListItem(WatchList watchList, Media media) {
		watchList.getItems().add(WatchListItem.builder()
			.watchList(watchList)
			.media(media)
			.addedAt(LocalDateTime.now())
			.build());
		watchListRepository.save(watchList);
	}

	private void assertWatchListItemCount(WatchList watchList, Media media, int expectedCount) {
		Integer itemCount = jdbcTemplate.queryForObject(
			"select count(*) from watchlist_items where watchlist_id = ? and media_id = ?",
			Integer.class,
			watchList.getId(),
			media.getId());

		assertThat(itemCount).isEqualTo(expectedCount);
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
