package com.project.watchmate.social.integration;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.project.watchmate.common.integration.support.AbstractIntegrationTest;
import com.project.watchmate.media.catalog.domain.Media;
import com.project.watchmate.media.catalog.domain.MediaType;
import com.project.watchmate.review.domain.Review;
import com.project.watchmate.user.domain.PrivacyStatuses;
import com.project.watchmate.user.domain.Users;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProfileReviewsIntegrationTest extends AbstractIntegrationTest {

    @Test
    void getReviewsByUserId_publicUser_returnsReviews() throws Exception {
        Users viewer = saveUser("rev-viewer-pub", true);
        Users author = saveUser("rev-author-pub", true);
        Media media = saveMedia(1001L, "Film A", MediaType.MOVIE);
        saveReview(author, media, 4, "Great film");

        mockMvc.perform(get("/api/v1/social/profile/by-id/{userId}/reviews", author.getId())
                .header("Authorization", bearerToken(viewer)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(1)))
            .andExpect(jsonPath("$.content[0].username").value(author.getUsername()))
            .andExpect(jsonPath("$.content[0].comment").value("Great film"))
            .andExpect(jsonPath("$.content[0].starRating").value(4));
    }

    @Test
    void getReviewsByUserId_selfProfile_returnsOwnReviews() throws Exception {
        Users owner = saveUser("rev-author-self", true);
        Media media = saveMedia(1002L, "Film B", MediaType.MOVIE);
        saveReview(owner, media, 5, "Love it");

        mockMvc.perform(get("/api/v1/social/profile/by-id/{userId}/reviews", owner.getId())
                .header("Authorization", bearerToken(owner)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(1)))
            .andExpect(jsonPath("$.content[0].username").value(owner.getUsername()));
    }

    @Test
    void getReviewsByUserId_privateUser_nonFollower_returns403() throws Exception {
        Users viewer = saveUser("rev-viewer-private", true);
        Users author = savePrivateUser("rev-author-private");
        Media media = saveMedia(1003L, "Film C", MediaType.MOVIE);
        saveReview(author, media, 3, "It was fine");

        mockMvc.perform(get("/api/v1/social/profile/by-id/{userId}/reviews", author.getId())
                .header("Authorization", bearerToken(viewer)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("PRIVATE_PROFILE"));
    }

    @Test
    void getReviewsByUserId_privateUser_approvedFollower_returnsReviews() throws Exception {
        Users viewer = saveUser("rev-follower-priv", true);
        Users author = savePrivateUser("rev-priv-author");
        Media media = saveMedia(1004L, "Film D", MediaType.MOVIE);
        saveReview(author, media, 5, "Masterpiece");

        jdbcTemplate.update(
            "insert into user_following (follower_id, following_id) values (?, ?)",
            viewer.getId(), author.getId()
        );

        mockMvc.perform(get("/api/v1/social/profile/by-id/{userId}/reviews", author.getId())
                .header("Authorization", bearerToken(viewer)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(1)))
            .andExpect(jsonPath("$.content[0].comment").value("Masterpiece"));
    }

    @Test
    void getReviewsByUserId_blockedByTarget_returns403() throws Exception {
        Users viewer = saveUser("rev-viewer-blocked", true);
        Users author = saveUser("rev-author-blocker", true);
        Media media = saveMedia(1005L, "Film E", MediaType.MOVIE);
        saveReview(author, media, 2, "Not for me");

        mockMvc.perform(post("/api/v1/social/block/{userId}", viewer.getId())
                .header("Authorization", bearerToken(author)))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/social/profile/by-id/{userId}/reviews", author.getId())
                .header("Authorization", bearerToken(viewer)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("USER_BLOCKED"));
    }

    @Test
    void getReviewsByUserId_pagination_sizeOneLimitsResults() throws Exception {
        Users viewer = saveUser("rev-viewer-page", true);
        Users author = saveUser("rev-author-page", true);
        Media m1 = saveMedia(1006L, "Film F", MediaType.MOVIE);
        Media m2 = saveMedia(1007L, "Film G", MediaType.MOVIE);
        saveReview(author, m1, 3, "Review 1");
        saveReview(author, m2, 4, "Review 2");

        mockMvc.perform(get("/api/v1/social/profile/by-id/{userId}/reviews", author.getId())
                .param("page", "0")
                .param("size", "1")
                .header("Authorization", bearerToken(viewer)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(1)))
            .andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void getReviewsByUsername_publicUser_returnsReviews() throws Exception {
        Users viewer = saveUser("rev-viewer-byname", true);
        Users author = saveUser("rev-author-byname", true);
        Media media = saveMedia(1008L, "Film H", MediaType.MOVIE);
        saveReview(author, media, 5, "Incredible");

        mockMvc.perform(get("/api/v1/social/profile/{username}/reviews", author.getUsername())
                .header("Authorization", bearerToken(viewer)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(1)))
            .andExpect(jsonPath("$.content[0].comment").value("Incredible"));
    }

    @Test
    void getReviewsByUserId_unauthenticated_returns401() throws Exception {
        Users author = saveUser("rev-author-unauth", true);

        mockMvc.perform(get("/api/v1/social/profile/by-id/{userId}/reviews", author.getId()))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void getReviewsByUserId_userNotFound_returns404() throws Exception {
        Users viewer = saveUser("rev-viewer-notfound", true);

        mockMvc.perform(get("/api/v1/social/profile/by-id/{userId}/reviews", 999999L)
                .header("Authorization", bearerToken(viewer)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
    }

    @Test
    void getReviewsByUserId_noReviews_returnsEmptyPage() throws Exception {
        Users viewer = saveUser("rev-viewer-empty", true);
        Users author = saveUser("rev-author-empty", true);

        mockMvc.perform(get("/api/v1/social/profile/by-id/{userId}/reviews", author.getId())
                .header("Authorization", bearerToken(viewer)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(0)))
            .andExpect(jsonPath("$.totalElements").value(0));
    }

    private Users savePrivateUser(String username) {
        Users user = saveUser(username, true);
        user.setPrivacyStatus(PrivacyStatuses.PRIVATE);
        return usersRepository.save(user);
    }

    private Review saveReview(Users user, Media media, int rating, String comment) {
        return reviewRepository.save(Review.builder()
            .user(user)
            .media(media)
            .rating(rating)
            .comment(comment)
            .datePosted(LocalDateTime.now())
            .dateLastModified(LocalDateTime.now())
            .build());
    }
}
