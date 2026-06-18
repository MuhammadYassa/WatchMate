package com.project.watchmate.social.integration;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.project.watchmate.common.integration.support.AbstractIntegrationTest;
import com.project.watchmate.social.domain.FollowRequest;
import com.project.watchmate.social.domain.FollowRequestStatuses;
import com.project.watchmate.media.catalog.domain.Media;
import com.project.watchmate.media.catalog.domain.MediaType;
import com.project.watchmate.user.domain.PrivacyStatuses;
import com.project.watchmate.user.domain.Role;
import com.project.watchmate.movie.tracking.domain.UserMediaStatus;
import com.project.watchmate.user.domain.Users;
import com.project.watchmate.media.catalog.domain.WatchStatus;
import com.project.watchmate.watchlist.domain.WatchList;
import com.project.watchmate.watchlist.domain.WatchListItem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SocialIntegrationTest extends AbstractIntegrationTest {

	@Test
	void followPublicUser_createsFollowingRelationship() throws Exception {
		Users user = saveUser("social-follower", true);
		Users target = saveUser("social-public-target", true);

		mockMvc.perform(post("/api/v1/social/follow/{userId}", target.getId())
			.header("Authorization", bearerToken(user)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.followStatus").value("FOLLOWING"));

		assertThat(usersRepository.isFollowing(user.getId(), target.getId())).isTrue();
	}

	@Test
	void followPrivateUser_createsPendingFollowRequest() throws Exception {
		Users user = saveUser("social-requester", true);
		Users target = savePrivateUser("social-private-target");

		mockMvc.perform(post("/api/v1/social/follow/{userId}", target.getId())
			.header("Authorization", bearerToken(user)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.followStatus").value("NOT_FOLLOWING"));

		FollowRequest request = followRequestRepository.findByRequestUserAndTargetUser(user, target).orElseThrow();

		assertThat(request.getStatus()).isEqualTo(FollowRequestStatuses.PENDING);
		assertThat(usersRepository.isFollowing(user.getId(), target.getId())).isFalse();
	}

	@Test
	void acceptFollowRequest_createsFollowingRelationship() throws Exception {
		Users requester = saveUser("social-accept-requester", true);
		Users target = savePrivateUser("social-accept-target");
		FollowRequest request = saveFollowRequest(requester, target, FollowRequestStatuses.PENDING);

		mockMvc.perform(post("/api/v1/social/follow-request/{requestId}/accept", request.getId())
			.header("Authorization", bearerToken(target)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.newStatus").value("ACCEPTED"));

		assertThat(followRequestRepository.findById(request.getId()).orElseThrow().getStatus())
			.isEqualTo(FollowRequestStatuses.ACCEPTED);
		assertThat(usersRepository.isFollowing(requester.getId(), target.getId())).isTrue();
	}

	@Test
	void unfollowUser_returns200_andRemovesRelationship() throws Exception {
		Users user = saveUser("social-unfollow-user", true);
		Users target = saveUser("social-unfollow-target", true);
		follow(user, target);

		mockMvc.perform(delete("/api/v1/social/unfollow/{userId}", target.getId())
			.header("Authorization", bearerToken(user)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.followStatus").value("NOT_FOLLOWING"));

		assertThat(usersRepository.isFollowing(user.getId(), target.getId())).isFalse();
	}

	@Test
	void cancelFollowRequest_returns200_forRequestOwner() throws Exception {
		Users requester = saveUser("social-cancel-requester", true);
		Users target = savePrivateUser("social-cancel-target");
		FollowRequest request = saveFollowRequest(requester, target, FollowRequestStatuses.PENDING);

		mockMvc.perform(delete("/api/v1/social/follow-request/{requestId}/cancel", request.getId())
			.header("Authorization", bearerToken(requester)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.requestId").value(request.getId().intValue()))
			.andExpect(jsonPath("$.newStatus").value("CANCELED"));

		assertThat(followRequestRepository.findById(request.getId())).isEmpty();
	}

	@Test
	void blockUser_returnsBlocked_andRemovesExistingRelationships() throws Exception {
		Users user = saveUser("social-block-user", true);
		Users target = saveUser("social-block-target", true);
		follow(user, target);
		follow(target, user);

		mockMvc.perform(post("/api/v1/social/block/{userId}", target.getId())
			.header("Authorization", bearerToken(user)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.followStatus").value("BLOCKED"));

		assertThat(usersRepository.isBlockingUser(user.getId(), target.getId())).isTrue();
		assertThat(usersRepository.isFollowing(user.getId(), target.getId())).isFalse();
		assertThat(usersRepository.isFollowing(target.getId(), user.getId())).isFalse();
	}

	@Test
	void userProfile_forBlockedRelationship_hidesSensitiveSections() throws Exception {
		Users viewer = saveUser("social-profile-viewer", true);
		Users target = saveUser("social-profile-target", true);

		mockMvc.perform(post("/api/v1/social/block/{userId}", viewer.getId())
			.header("Authorization", bearerToken(target)))
			.andExpect(status().isOk());

		mockMvc.perform(get("/api/v1/social/profile/{username}", target.getUsername())
			.header("Authorization", bearerToken(viewer)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.userId").value(target.getId().intValue()))
			.andExpect(jsonPath("$.username").value(target.getUsername()))
			.andExpect(jsonPath("$.followStatus").value("BLOCKED"))
			.andExpect(jsonPath("$.followersCount").value(0))
			.andExpect(jsonPath("$.followingCount").value(0))
			.andExpect(jsonPath("$.watchlists").doesNotExist())
			.andExpect(jsonPath("$.reviews").doesNotExist());
	}

	@Test
	void userProfile_forPrivateUser_hidesSensitiveSectionsFromNonFollowingUser() throws Exception {
		Users viewer = saveUser("social-private-profile-viewer", true);
		Users target = savePrivateUser("social-private-profile-target");
		saveWatchList(target, "Private Watchlist");

		mockMvc.perform(get("/api/v1/social/profile/{username}", target.getUsername())
			.header("Authorization", bearerToken(viewer)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.userId").value(target.getId().intValue()))
			.andExpect(jsonPath("$.username").value(target.getUsername()))
			.andExpect(jsonPath("$.privacyStatus").value("PRIVATE"))
			.andExpect(jsonPath("$.watchlists").doesNotExist());
	}

	@Test
	void userProfile_forPrivateUser_returnsSensitiveSectionsToModerator() throws Exception {
		Users moderator = saveUser("social-private-profile-moderator", true, Role.MODERATOR);
		Users target = savePrivateUser("social-private-profile-mod-target");
		Media media = saveMedia(9801L, "Moderator Visible Movie", MediaType.MOVIE);
		WatchList watchList = saveWatchList(target, "Moderator Visible Watchlist");
		saveWatchListItem(watchList, media);
		saveFavorite(target, media);
		saveMovieStatus(target, media, WatchStatus.WATCHED);

		mockMvc.perform(get("/api/v1/social/profile/{username}", target.getUsername())
			.header("Authorization", bearerToken(moderator)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.userId").value(target.getId().intValue()))
			.andExpect(jsonPath("$.username").value(target.getUsername()))
			.andExpect(jsonPath("$.privacyStatus").value("PRIVATE"))
			.andExpect(jsonPath("$.watchlists.content.length()").value(1))
			.andExpect(jsonPath("$.watchlists.content[0].name").value("Moderator Visible Watchlist"))
			.andExpect(jsonPath("$.watchlists.content[0].media[0].isFavourited").value(false))
			.andExpect(jsonPath("$.watchlists.content[0].media[0].watchStatus").value("NONE"));
	}

	@Test
	void userProfile_forPrivateUser_returnsSensitiveSectionsToAdmin() throws Exception {
		Users admin = saveUser("social-private-profile-admin", true, Role.ADMIN);
		Users target = savePrivateUser("social-private-profile-admin-target");
		saveWatchList(target, "Admin Visible Watchlist");

		mockMvc.perform(get("/api/v1/social/profile/{username}", target.getUsername())
			.header("Authorization", bearerToken(admin)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.userId").value(target.getId().intValue()))
			.andExpect(jsonPath("$.username").value(target.getUsername()))
			.andExpect(jsonPath("$.privacyStatus").value("PRIVATE"))
			.andExpect(jsonPath("$.watchlists.content.length()").value(1))
			.andExpect(jsonPath("$.watchlists.content[0].name").value("Admin Visible Watchlist"));
	}

	@Test
	void receivedFollowRequests_returnsPendingRequestsOnly() throws Exception {
		Users pendingRequester = saveUser("social-pending-requester", true);
		Users acceptedRequester = saveUser("social-accepted-requester", true);
		Users target = savePrivateUser("social-requests-target");
		saveFollowRequest(pendingRequester, target, FollowRequestStatuses.PENDING);
		saveFollowRequest(acceptedRequester, target, FollowRequestStatuses.ACCEPTED);

		mockMvc.perform(get("/api/v1/social/follow-requests/received")
			.header("Authorization", bearerToken(target)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content.length()").value(1))
			.andExpect(jsonPath("$.content[0].requestId").isNumber())
			.andExpect(jsonPath("$.content[0].requesterUserId").value(pendingRequester.getId().intValue()))
			.andExpect(jsonPath("$.content[0].targetUserId").value(target.getId().intValue()))
			.andExpect(jsonPath("$.content[*].requesterUsername", contains(pendingRequester.getUsername())))
			.andExpect(jsonPath("$.content[*].targetUsername", contains(target.getUsername())))
			.andExpect(jsonPath("$.content[0].status").value("PENDING"));
	}

	@Test
	void followSelf_returns400() throws Exception {
		Users user = saveUser("social-self-user", true);

		mockMvc.perform(post("/api/v1/social/follow/{userId}", user.getId())
			.header("Authorization", bearerToken(user)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("SELF_FOLLOW_ERROR"));
	}

	@Test
	void searchUsers_returnsCaseInsensitiveMatchesOrderedByBestMatch() throws Exception {
		Users viewer = saveUser("viewer-user", true);
		Users exact = saveUser("muh", true);
		Users followedPrefix = saveUser("muha", true);
		saveUser("muhb", true);
		Users followedContains = saveUser("amuh", true);
		saveUser("other-user", true);
		follow(viewer, followedPrefix);
		follow(viewer, followedContains);

		mockMvc.perform(get("/api/v1/social/search")
			.param("query", "MUH")
			.header("Authorization", bearerToken(viewer)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(4)))
			.andExpect(jsonPath("$[0].userId").value(exact.getId().intValue()))
			.andExpect(jsonPath("$[*].username", contains("muh", "muha", "muhb", "amuh")))
			.andExpect(jsonPath("$[0].isFollowing").value(false))
			.andExpect(jsonPath("$[1].isFollowing").value(true))
			.andExpect(jsonPath("$[2].isFollowing").value(false))
			.andExpect(jsonPath("$[3].isFollowing").value(true))
			.andExpect(jsonPath("$[0].isSelf").value(false))
			.andExpect(jsonPath("$[0].privacyStatus").value(exact.getPrivacyStatus().name()))
			.andExpect(jsonPath("$[0].email").doesNotExist())
			.andExpect(jsonPath("$[0].password").doesNotExist())
			.andExpect(jsonPath("$[0].emailVerified").doesNotExist())
			.andExpect(jsonPath("$[0].token").doesNotExist());
	}

	@Test
	void searchUsers_withBlankQuery_returns400() throws Exception {
		Users viewer = saveUser("blank-query-viewer", true);

		mockMvc.perform(get("/api/v1/social/search")
			.param("query", "   ")
			.header("Authorization", bearerToken(viewer)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}

	@Test
	void getUserProfileByUsername_returnsProfileUsingCaseInsensitiveLookup() throws Exception {
		Users viewer = saveUser("profile-viewer", true);
		Users target = saveUser("CaseTarget", true);
		saveWatchList(target, "Visible Watchlist");

		mockMvc.perform(get("/api/v1/social/profile/{username}", "casetarget")
			.header("Authorization", bearerToken(viewer)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.userId").value(target.getId().intValue()))
			.andExpect(jsonPath("$.username").value("CaseTarget"))
			.andExpect(jsonPath("$.watchlists.content.length()").value(1))
			.andExpect(jsonPath("$.watchlists.content[0].name").value("Visible Watchlist"));
	}

	@Test
	void getUserProfileByUsername_forSelfReturnsWatchedMediaWithoutQueryError() throws Exception {
		Users user = saveUser("self-profile-watched", true);
		Media watchedMovie = saveMedia(9301L, "Watched Movie", MediaType.MOVIE);
		watchedMovie.setReleaseDate(LocalDate.of(2024, 1, 2));
		mediaRepository.save(watchedMovie);
		userMediaStatusRepository.save(UserMediaStatus.builder()
			.user(user)
			.media(watchedMovie)
			.status(WatchStatus.WATCHED)
			.build());

		mockMvc.perform(get("/api/v1/social/profile/{username}", user.getUsername())
			.header("Authorization", bearerToken(user)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.userId").value(user.getId().intValue()))
			.andExpect(jsonPath("$.username").value(user.getUsername()))
			.andExpect(jsonPath("$.moviesWatchedCount").value(1))
			.andExpect(jsonPath("$.moviesWatched.content.length()").value(1))
			.andExpect(jsonPath("$.moviesWatched.content[0].title").value("Watched Movie"));
	}

	@Test
	void getUserProfileByUsername_forPrivateUser_hidesSensitiveSectionsFromNonFollower() throws Exception {
		Users viewer = saveUser("private-profile-viewer", true);
		Users target = saveUser("private-profile-target", true);
		target.setPrivacyStatus(PrivacyStatuses.PRIVATE);
		usersRepository.save(target);
		saveWatchList(target, "Private Watchlist");

		mockMvc.perform(get("/api/v1/social/profile/{username}", target.getUsername())
			.header("Authorization", bearerToken(viewer)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.userId").value(target.getId().intValue()))
			.andExpect(jsonPath("$.username").value(target.getUsername()))
			.andExpect(jsonPath("$.privacyStatus").value("PRIVATE"))
			.andExpect(jsonPath("$.watchlists").doesNotExist());
	}

	@Test
	void userProfile_forPublicUser_usesViewerOverlayInsteadOfOwnerOverlay() throws Exception {
		Users owner = saveUser("overlay-owner-a", true);
		Users viewer = saveUser("overlay-viewer-a", true);
		Media media = saveMedia(9811L, "Overlay Movie A", MediaType.MOVIE);
		WatchList watchList = saveWatchList(owner, "Owner Overlay List");
		saveWatchListItem(watchList, media);
		saveFavorite(owner, media);
		saveMovieStatus(owner, media, WatchStatus.WATCHED);

		mockMvc.perform(get("/api/v1/social/profile/{username}", owner.getUsername())
			.header("Authorization", bearerToken(viewer)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.userId").value(owner.getId().intValue()))
			.andExpect(jsonPath("$.watchlists.content[0].media[0].isFavourited").value(false))
			.andExpect(jsonPath("$.watchlists.content[0].media[0].watchStatus").value("NONE"));
	}

	@Test
	void userProfile_forPublicUser_usesViewersOwnOverlayStateWhenPresent() throws Exception {
		Users owner = saveUser("overlay-owner-b", true);
		Users viewer = saveUser("overlay-viewer-b", true);
		Media media = saveMedia(9812L, "Overlay Movie B", MediaType.MOVIE);
		WatchList watchList = saveWatchList(owner, "Owner Overlay List B");
		saveWatchListItem(watchList, media);
		saveFavorite(owner, media);
		saveMovieStatus(owner, media, WatchStatus.WATCHED);
		saveFavorite(viewer, media);
		saveMovieStatus(viewer, media, WatchStatus.TO_WATCH);

		mockMvc.perform(get("/api/v1/social/profile/{username}", owner.getUsername())
			.header("Authorization", bearerToken(viewer)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.userId").value(owner.getId().intValue()))
			.andExpect(jsonPath("$.watchlists.content[0].media[0].isFavourited").value(true))
			.andExpect(jsonPath("$.watchlists.content[0].media[0].watchStatus").value("TO_WATCH"));
	}

	@Test
	void userProfile_forSelf_keepsOwnersOwnOverlayState() throws Exception {
		Users owner = saveUser("overlay-owner-self", true);
		Media media = saveMedia(9813L, "Overlay Movie Self", MediaType.MOVIE);
		WatchList watchList = saveWatchList(owner, "Self Overlay List");
		saveWatchListItem(watchList, media);
		saveFavorite(owner, media);
		saveMovieStatus(owner, media, WatchStatus.WATCHED);

		mockMvc.perform(get("/api/v1/social/profile/{username}", owner.getUsername())
			.header("Authorization", bearerToken(owner)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.userId").value(owner.getId().intValue()))
			.andExpect(jsonPath("$.watchlists.content[0].media[0].isFavourited").value(true))
			.andExpect(jsonPath("$.watchlists.content[0].media[0].watchStatus").value("WATCHED"));
	}

	@Test
	void followersAndFollowingLists_includeUserIdAndUsername() throws Exception {
		Users user = saveUser("social-list-owner", true);
		Users follower = saveUser("social-list-follower", true);
		Users following = saveUser("social-list-following", true);
		follow(follower, user);
		follow(user, following);

		mockMvc.perform(get("/api/v1/social/followers-list")
			.header("Authorization", bearerToken(user)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content.length()").value(1))
			.andExpect(jsonPath("$.content[0].userId").value(follower.getId().intValue()))
			.andExpect(jsonPath("$.content[0].username").value(follower.getUsername()));

		mockMvc.perform(get("/api/v1/social/following-list")
			.header("Authorization", bearerToken(user)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content.length()").value(1))
			.andExpect(jsonPath("$.content[0].userId").value(following.getId().intValue()))
			.andExpect(jsonPath("$.content[0].username").value(following.getUsername()));
	}

	@Test
	void getUserProfileByUsername_whenMissing_returns404() throws Exception {
		Users viewer = saveUser("missing-profile-viewer", true);

		mockMvc.perform(get("/api/v1/social/profile/{username}", "does-not-exist")
			.header("Authorization", bearerToken(viewer)))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
	}

	private Users savePrivateUser(String username) {
		Users user = saveUser(username, true);
		user.setPrivacyStatus(PrivacyStatuses.PRIVATE);
		return usersRepository.save(user);
	}

	private FollowRequest saveFollowRequest(Users requester, Users target, FollowRequestStatuses status) {
		return followRequestRepository.save(FollowRequest.builder()
			.requestUser(requester)
			.targetUser(target)
			.requestedAt(LocalDateTime.now())
			.status(status)
			.build());
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

	private void saveFavorite(Users user, Media media) {
		jdbcTemplate.update(
			"insert into user_favorites (users_id, favorites_id) values (?, ?)",
			user.getId(),
			media.getId());
	}

	private void saveMovieStatus(Users user, Media media, WatchStatus status) {
		userMediaStatusRepository.save(UserMediaStatus.builder()
			.user(user)
			.media(media)
			.status(status)
			.build());
	}

	private void follow(Users user, Users target) throws Exception {
		mockMvc.perform(post("/api/v1/social/follow/{userId}", target.getId())
			.header("Authorization", bearerToken(user)))
			.andExpect(status().isOk());
	}
}







