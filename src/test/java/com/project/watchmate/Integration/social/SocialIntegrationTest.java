package com.project.watchmate.Integration.social;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.project.watchmate.Integration.support.AbstractIntegrationTest;
import com.project.watchmate.Models.FollowRequest;
import com.project.watchmate.Models.FollowRequestStatuses;
import com.project.watchmate.Models.PrivacyStatuses;
import com.project.watchmate.Models.Role;
import com.project.watchmate.Models.Users;
import com.project.watchmate.Models.WatchList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
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

		mockMvc.perform(get("/api/v1/social/user-profile/{userId}", target.getId())
			.header("Authorization", bearerToken(viewer)))
			.andExpect(status().isOk())
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

		mockMvc.perform(get("/api/v1/social/user-profile/{userId}", target.getId())
			.header("Authorization", bearerToken(viewer)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.username").value(target.getUsername()))
			.andExpect(jsonPath("$.privacyStatus").value("PRIVATE"))
			.andExpect(jsonPath("$.watchlists").doesNotExist());
	}

	@Test
	void userProfile_forPrivateUser_returnsSensitiveSectionsToModerator() throws Exception {
		Users moderator = saveUser("social-private-profile-moderator", true, Role.MODERATOR);
		Users target = savePrivateUser("social-private-profile-mod-target");
		saveWatchList(target, "Moderator Visible Watchlist");

		mockMvc.perform(get("/api/v1/social/user-profile/{userId}", target.getId())
			.header("Authorization", bearerToken(moderator)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.username").value(target.getUsername()))
			.andExpect(jsonPath("$.privacyStatus").value("PRIVATE"))
			.andExpect(jsonPath("$.watchlists.content.length()").value(1))
			.andExpect(jsonPath("$.watchlists.content[0].name").value("Moderator Visible Watchlist"));
	}

	@Test
	void userProfile_forPrivateUser_returnsSensitiveSectionsToAdmin() throws Exception {
		Users admin = saveUser("social-private-profile-admin", true, Role.ADMIN);
		Users target = savePrivateUser("social-private-profile-admin-target");
		saveWatchList(target, "Admin Visible Watchlist");

		mockMvc.perform(get("/api/v1/social/user-profile/{userId}", target.getId())
			.header("Authorization", bearerToken(admin)))
			.andExpect(status().isOk())
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
			.andExpect(jsonPath("$.content[*].requesterUsername", contains(pendingRequester.getUsername())))
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

	private void follow(Users user, Users target) throws Exception {
		mockMvc.perform(post("/api/v1/social/follow/{userId}", target.getId())
			.header("Authorization", bearerToken(user)))
			.andExpect(status().isOk());
	}
}
