package com.project.watchmate.watchlist.integration;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.project.watchmate.common.integration.support.AbstractIntegrationTest;
import com.project.watchmate.media.catalog.domain.Media;
import com.project.watchmate.media.catalog.domain.WatchStatus;
import com.project.watchmate.movie.tracking.domain.UserMediaStatus;
import com.project.watchmate.review.domain.Review;
import com.project.watchmate.show.tracking.domain.UserShowTracking;
import com.project.watchmate.user.domain.Role;
import com.project.watchmate.user.domain.Users;
import com.project.watchmate.watchlist.domain.WatchList;
import com.project.watchmate.watchlist.domain.WatchListItem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.dao.DataIntegrityViolationException;

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
	void createWatchlist_returns409_whenNameAlreadyExists() throws Exception {
		Users user = saveUser("watchlist-create-conflict-user", true);
		saveWatchList(user, "Weekend Movies");

		mockMvc.perform(post("/api/v1/watchlists")
			.header("Authorization", bearerToken(user))
			.contentType(MediaType.APPLICATION_JSON)
			.content(createWatchListBody("weekend movies")))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("WATCHLIST_NAME_CONFLICT"));
	}

	@Test
	void createWatchlist_allowsSameNameForDifferentUsers() throws Exception {
		Users firstUser = saveUser("watchlist-shared-name-user-a", true);
		Users secondUser = saveUser("watchlist-shared-name-user-b", true);
		saveWatchList(firstUser, "Shared Name");

		mockMvc.perform(post("/api/v1/watchlists")
			.header("Authorization", bearerToken(secondUser))
			.contentType(MediaType.APPLICATION_JSON)
			.content(createWatchListBody("Shared Name")))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.name").value("Shared Name"));

		assertThat(watchListRepository.findAll()).hasSize(2);
	}

	@Test
	void directRepositorySaveAndFlush_rejectsSameUserCaseInsensitiveDuplicate() {
		Users user = saveUser("watchlist-db-constraint-user", true);
		watchListRepository.saveAndFlush(WatchList.builder()
			.name("Favorites")
			.user(user)
			.build());

		assertThatThrownBy(() -> watchListRepository.saveAndFlush(WatchList.builder()
			.name("favorites")
			.user(user)
			.build()))
			.isInstanceOf(DataIntegrityViolationException.class);
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
			.andExpect(jsonPath("$.content").isArray())
			.andExpect(jsonPath("$.content", hasSize(2)))
			.andExpect(jsonPath("$.totalElements").value(2))
			.andExpect(jsonPath("$.content[*].name", containsInAnyOrder("Owner List A", "Owner List B")));
	}

	@Test
	void getWatchlists_withoutPaginationParams_usesDefaultFirstPage() throws Exception {
		Users owner = saveUser("watchlist-default-page-owner", true);
		for (int i = 1; i <= 21; i++) {
			saveWatchList(owner, "Default Page List " + i);
		}

		mockMvc.perform(get("/api/v1/watchlists")
			.header("Authorization", bearerToken(owner)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content", hasSize(20)))
			.andExpect(jsonPath("$.number").value(0))
			.andExpect(jsonPath("$.size").value(20))
			.andExpect(jsonPath("$.totalElements").value(21));
	}

	@Test
	void getWatchlists_withPageAndSize_returnsRequestedPage() throws Exception {
		Users owner = saveUser("watchlist-sized-page-owner", true);
		saveWatchList(owner, "First Page List");
		saveWatchList(owner, "Second Page List");

		mockMvc.perform(get("/api/v1/watchlists")
			.header("Authorization", bearerToken(owner))
			.param("page", "0")
			.param("size", "1"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content", hasSize(1)))
			.andExpect(jsonPath("$.content[0].name").value("First Page List"))
			.andExpect(jsonPath("$.number").value(0))
			.andExpect(jsonPath("$.size").value(1))
			.andExpect(jsonPath("$.totalElements").value(2));
	}

	@Test
	void getWatchlists_withInvalidPagination_returns400() throws Exception {
		Users owner = saveUser("watchlist-invalid-page-owner", true);

		mockMvc.perform(get("/api/v1/watchlists")
			.header("Authorization", bearerToken(owner))
			.param("size", "51"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

		mockMvc.perform(get("/api/v1/watchlists")
			.header("Authorization", bearerToken(owner))
			.param("size", "0"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

		mockMvc.perform(get("/api/v1/watchlists")
			.header("Authorization", bearerToken(owner))
			.param("page", "-1"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}

	@Test
	void getWatchlists_mapsMultipleWatchlistsWithBatchedRelatedData() throws Exception {
		Users owner = saveUser("watchlist-batch-owner", true);
		Users reviewer = saveUser("watchlist-batch-reviewer", true);
		WatchList movieList = saveWatchList(owner, "Movies");
		WatchList showList = saveWatchList(owner, "Shows");
		Media movie = saveMedia(9101L, "Batch Movie", com.project.watchmate.media.catalog.domain.MediaType.MOVIE);
		Media show = saveMedia(9102L, "Batch Show", com.project.watchmate.media.catalog.domain.MediaType.SHOW);
		saveWatchListItem(movieList, movie);
		saveWatchListItem(showList, show);
		saveReview(reviewer, movie, 5, "Great batch movie");
		saveFavorite(owner, movie);
		userMediaStatusRepository.save(UserMediaStatus.builder()
			.user(owner)
			.media(movie)
			.status(WatchStatus.WATCHED)
			.build());
		userShowTrackingRepository.save(UserShowTracking.builder()
			.user(owner)
			.media(show)
			.status(WatchStatus.UP_TO_DATE)
			.build());

		mockMvc.perform(get("/api/v1/watchlists")
			.header("Authorization", bearerToken(owner))
			.param("page", "0")
			.param("size", "10"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content", hasSize(2)))
			.andExpect(jsonPath("$.content[0].media[0].tmdbId").value(movie.getTmdbId().intValue()))
			.andExpect(jsonPath("$.content[0].media[0].isFavourited").value(true))
			.andExpect(jsonPath("$.content[0].media[0].watchStatus").value("WATCHED"))
			.andExpect(jsonPath("$.content[0].media[0].reviews[0].comment").value("Great batch movie"))
			.andExpect(jsonPath("$.content[1].media[0].tmdbId").value(show.getTmdbId().intValue()))
			.andExpect(jsonPath("$.content[1].media[0].isFavourited").value(false))
			.andExpect(jsonPath("$.content[1].media[0].watchStatus").value("UP_TO_DATE"));
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
	void renameWatchlist_returns409_whenNameAlreadyExists() throws Exception {
		Users owner = saveUser("watchlist-rename-conflict-owner", true);
		WatchList watchList = saveWatchList(owner, "Before Rename");
		saveWatchList(owner, "Taken Name");

		mockMvc.perform(patch("/api/v1/watchlists/{id}", watchList.getId())
			.header("Authorization", bearerToken(owner))
			.contentType(MediaType.APPLICATION_JSON)
			.content(renameWatchListBody("taken name")))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("WATCHLIST_NAME_CONFLICT"));
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
		Media media = saveMedia(9001L, "Item Movie", com.project.watchmate.media.catalog.domain.MediaType.MOVIE);

		mockMvc.perform(post("/api/v1/watchlists/{watchListId}/items/{tmdbId}", watchList.getId(), media.getTmdbId())
			.header("Authorization", bearerToken(owner))
			.param("type", "MOVIE"))
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
		Media media = saveMedia(9002L, "Removable Movie", com.project.watchmate.media.catalog.domain.MediaType.MOVIE);

		mockMvc.perform(post("/api/v1/watchlists/{watchListId}/items/{tmdbId}", watchList.getId(), media.getTmdbId())
			.header("Authorization", bearerToken(owner))
			.param("type", "MOVIE"))
			.andExpect(status().isOk());

		mockMvc.perform(delete("/api/v1/watchlists/{watchListId}/items/{tmdbId}", watchList.getId(), media.getTmdbId())
			.header("Authorization", bearerToken(owner))
			.param("type", "MOVIE"))
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
		Media media = saveMedia(9003L, "Protected Movie", com.project.watchmate.media.catalog.domain.MediaType.MOVIE);
		saveWatchListItem(watchList, media);

		mockMvc.perform(delete("/api/v1/watchlists/{watchListId}/items/{tmdbId}", watchList.getId(), media.getTmdbId())
			.header("Authorization", bearerToken(nonOwner))
			.param("type", "MOVIE"))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("UNAUTHORIZED_WATCHLIST_ACCESS"));

		assertWatchListItemCount(watchList, media, 1);
	}

	@Test
	void removeMediaFromWatchlist_returns403_forModeratorNonOwner() throws Exception {
		Users owner = saveUser("watchlist-remove-mod-owner", true);
		Users moderator = saveUser("watchlist-remove-moderator", true, Role.MODERATOR);
		WatchList watchList = saveWatchList(owner, "Moderator Protected Items");
		Media media = saveMedia(9004L, "Moderator Protected Movie", com.project.watchmate.media.catalog.domain.MediaType.MOVIE);
		saveWatchListItem(watchList, media);

		mockMvc.perform(delete("/api/v1/watchlists/{watchListId}/items/{tmdbId}", watchList.getId(), media.getTmdbId())
			.header("Authorization", bearerToken(moderator))
			.param("type", "MOVIE"))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("UNAUTHORIZED_WATCHLIST_ACCESS"));

		assertWatchListItemCount(watchList, media, 1);
	}

	@Test
	void removeMediaFromWatchlist_returns200_forAdminNonOwner() throws Exception {
		Users owner = saveUser("watchlist-remove-admin-owner", true);
		Users admin = saveUser("watchlist-remove-admin", true, Role.ADMIN);
		WatchList watchList = saveWatchList(owner, "Admin Removable Items");
		Media media = saveMedia(9005L, "Admin Removable Movie", com.project.watchmate.media.catalog.domain.MediaType.MOVIE);
		saveWatchListItem(watchList, media);

		mockMvc.perform(delete("/api/v1/watchlists/{watchListId}/items/{tmdbId}", watchList.getId(), media.getTmdbId())
			.header("Authorization", bearerToken(admin))
			.param("type", "MOVIE"))
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

	private void saveReview(Users user, Media media, int rating, String comment) {
		reviewRepository.save(Review.builder()
			.user(user)
			.media(media)
			.rating(rating)
			.comment(comment)
			.datePosted(LocalDateTime.now())
			.dateLastModified(LocalDateTime.now())
			.build());
	}

	private void saveFavorite(Users user, Media media) {
		jdbcTemplate.update(
			"insert into user_favorites (users_id, favorites_id) values (?, ?)",
			user.getId(),
			media.getId());
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






