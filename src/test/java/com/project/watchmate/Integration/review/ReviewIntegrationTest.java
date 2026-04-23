package com.project.watchmate.Integration.review;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.project.watchmate.Integration.support.AbstractIntegrationTest;
import com.project.watchmate.Models.Media;
import com.project.watchmate.Models.Review;
import com.project.watchmate.Models.Role;
import com.project.watchmate.Models.Users;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReviewIntegrationTest extends AbstractIntegrationTest {

	@Test
	void createReview_returns201_andPersistsReview() throws Exception {
		Users user = saveUser("review-create-user", true);
		Media media = saveMedia(7001L, "Reviewed Movie", com.project.watchmate.Models.MediaType.MOVIE);

		mockMvc.perform(post("/api/v1/reviews/create")
			.header("Authorization", bearerToken(user))
			.contentType(MediaType.APPLICATION_JSON)
			.content(createReviewBody(media.getId(), 5, "Loved it")))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.reviewId").isNumber())
			.andExpect(jsonPath("$.mediaId").value(media.getId().intValue()))
			.andExpect(jsonPath("$.starRating").value(5))
			.andExpect(jsonPath("$.comment").value("Loved it"));

		Review review = reviewRepository.findByMedia(media).getFirst();

		assertThat(review.getUser().getId()).isEqualTo(user.getId());
		assertThat(review.getRating()).isEqualTo(5);
		assertThat(review.getComment()).isEqualTo("Loved it");
	}

	@Test
	void duplicateReview_returns409() throws Exception {
		Users user = saveUser("review-duplicate-user", true);
		Media media = saveMedia(7002L, "Duplicate Movie", com.project.watchmate.Models.MediaType.MOVIE);
		saveReview(user, media, 4, "First");

		mockMvc.perform(post("/api/v1/reviews/create")
			.header("Authorization", bearerToken(user))
			.contentType(MediaType.APPLICATION_JSON)
			.content(createReviewBody(media.getId(), 3, "Second")))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("DUPLICATE_REVIEW"));

		assertThat(reviewRepository.findByMedia(media)).hasSize(1);
	}

	@Test
	void reviewUpdate_byNonOwner_returns403() throws Exception {
		Users owner = saveUser("review-owner", true);
		Users nonOwner = saveUser("review-non-owner", true);
		Media media = saveMedia(7003L, "Owned Review Movie", com.project.watchmate.Models.MediaType.MOVIE);
		Review review = saveReview(owner, media, 4, "Owner review");

		mockMvc.perform(patch("/api/v1/reviews/{reviewId}", review.getId())
			.header("Authorization", bearerToken(nonOwner))
			.contentType(MediaType.APPLICATION_JSON)
			.content(updateReviewBody(2, "Non-owner edit")))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("UNAUTHORIZED_REVIEW_ACCESS"));

		Review unchanged = reviewRepository.findById(review.getId()).orElseThrow();

		assertThat(unchanged.getRating()).isEqualTo(4);
		assertThat(unchanged.getComment()).isEqualTo("Owner review");
	}

	@Test
	void reviewUpdate_byModeratorNonOwner_returns403() throws Exception {
		Users owner = saveUser("review-update-mod-owner", true);
		Users moderator = saveUser("review-update-moderator", true, Role.MODERATOR);
		Media media = saveMedia(7004L, "Moderator Update Movie", com.project.watchmate.Models.MediaType.MOVIE);
		Review review = saveReview(owner, media, 4, "Owner review");

		mockMvc.perform(patch("/api/v1/reviews/{reviewId}", review.getId())
			.header("Authorization", bearerToken(moderator))
			.contentType(MediaType.APPLICATION_JSON)
			.content(updateReviewBody(2, "Moderator edit")))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("UNAUTHORIZED_REVIEW_ACCESS"));

		Review unchanged = reviewRepository.findById(review.getId()).orElseThrow();

		assertThat(unchanged.getRating()).isEqualTo(4);
		assertThat(unchanged.getComment()).isEqualTo("Owner review");
	}

	@Test
	void deleteReview_byOwner_returns204() throws Exception {
		Users owner = saveUser("review-delete-owner", true);
		Media media = saveMedia(7005L, "Owner Delete Movie", com.project.watchmate.Models.MediaType.MOVIE);
		Review review = saveReview(owner, media, 4, "Owner review");

		mockMvc.perform(delete("/api/v1/reviews/{reviewId}", review.getId())
			.header("Authorization", bearerToken(owner)))
			.andExpect(status().isNoContent());

		assertThat(reviewRepository.findById(review.getId())).isEmpty();
	}

	@Test
	void deleteReview_byNonOwnerUser_returns403() throws Exception {
		Users owner = saveUser("review-delete-user-owner", true);
		Users nonOwner = saveUser("review-delete-non-owner-user", true);
		Media media = saveMedia(7006L, "User Delete Movie", com.project.watchmate.Models.MediaType.MOVIE);
		Review review = saveReview(owner, media, 4, "Owner review");

		mockMvc.perform(delete("/api/v1/reviews/{reviewId}", review.getId())
			.header("Authorization", bearerToken(nonOwner)))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("UNAUTHORIZED_REVIEW_ACCESS"));

		assertThat(reviewRepository.findById(review.getId())).isPresent();
	}

	@Test
	void deleteReview_byModerator_returns204() throws Exception {
		Users owner = saveUser("review-delete-mod-owner", true);
		Users moderator = saveUser("review-delete-moderator", true, Role.MODERATOR);
		Media media = saveMedia(7007L, "Moderator Delete Movie", com.project.watchmate.Models.MediaType.MOVIE);
		Review review = saveReview(owner, media, 4, "Owner review");

		mockMvc.perform(delete("/api/v1/reviews/{reviewId}", review.getId())
			.header("Authorization", bearerToken(moderator)))
			.andExpect(status().isNoContent());

		assertThat(reviewRepository.findById(review.getId())).isEmpty();
	}

	@Test
	void deleteReview_byAdmin_returns204() throws Exception {
		Users owner = saveUser("review-delete-admin-owner", true);
		Users admin = saveUser("review-delete-admin", true, Role.ADMIN);
		Media media = saveMedia(7008L, "Admin Delete Movie", com.project.watchmate.Models.MediaType.MOVIE);
		Review review = saveReview(owner, media, 4, "Owner review");

		mockMvc.perform(delete("/api/v1/reviews/{reviewId}", review.getId())
			.header("Authorization", bearerToken(admin)))
			.andExpect(status().isNoContent());

		assertThat(reviewRepository.findById(review.getId())).isEmpty();
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

	private String createReviewBody(Long mediaId, int starRating, String comment) {
		return """
			{"mediaId":%d,"starRating":%d,"comment":"%s"}
			""".formatted(mediaId, starRating, comment);
	}

	private String updateReviewBody(int starRating, String comment) {
		return """
			{"starRating":%d,"comment":"%s"}
			""".formatted(starRating, comment);
	}
}
