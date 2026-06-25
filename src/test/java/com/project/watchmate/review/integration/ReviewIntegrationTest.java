package com.project.watchmate.review.integration;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.project.watchmate.common.integration.support.AbstractIntegrationTest;
import com.project.watchmate.media.catalog.domain.Media;
import com.project.watchmate.media.tmdb.dto.TmdbMovieDTO;
import com.project.watchmate.review.domain.Review;
import com.project.watchmate.user.domain.Role;
import com.project.watchmate.user.domain.Users;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReviewIntegrationTest extends AbstractIntegrationTest {

	@Test
	void createReview_returns201_andPersistsReview() throws Exception {
		Users user = saveUser("review-create-user", true);
		Media media = saveMedia(7001L, "Reviewed Movie", com.project.watchmate.media.catalog.domain.MediaType.MOVIE);

		mockMvc.perform(post("/api/v1/reviews")
			.header("Authorization", bearerToken(user))
			.contentType(MediaType.APPLICATION_JSON)
			.content(createReviewBody(media.getTmdbId(), "MOVIE", 5, "Loved it")))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.reviewId").isNumber())
			.andExpect(jsonPath("$.tmdbId").value(media.getTmdbId().intValue()))
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
		Media media = saveMedia(7002L, "Duplicate Movie", com.project.watchmate.media.catalog.domain.MediaType.MOVIE);
		saveReview(user, media, 4, "First");

		mockMvc.perform(post("/api/v1/reviews")
			.header("Authorization", bearerToken(user))
			.contentType(MediaType.APPLICATION_JSON)
			.content(createReviewBody(media.getTmdbId(), "MOVIE", 3, "Second")))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("DUPLICATE_REVIEW"));

		assertThat(reviewRepository.findByMedia(media)).hasSize(1);
	}

	@Test
	void reviewUpdate_byNonOwner_returns403() throws Exception {
		Users owner = saveUser("review-owner", true);
		Users nonOwner = saveUser("review-non-owner", true);
		Media media = saveMedia(7003L, "Owned Review Movie", com.project.watchmate.media.catalog.domain.MediaType.MOVIE);
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
		Media media = saveMedia(7004L, "Moderator Update Movie", com.project.watchmate.media.catalog.domain.MediaType.MOVIE);
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
	void getReviewsByTmdbId_unauthenticated_returns200() throws Exception {
		Users user = saveUser("review-list-user", true);
		Media media = saveMedia(7009L, "Review Listing Movie", com.project.watchmate.media.catalog.domain.MediaType.MOVIE);
		saveReview(user, media, 5, "Excellent");

		mockMvc.perform(get("/api/v1/movies/{tmdbId}/reviews", media.getTmdbId()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[0].reviewId").isNumber())
			.andExpect(jsonPath("$.content[0].tmdbId").value(media.getTmdbId().intValue()))
			.andExpect(jsonPath("$.content[0].starRating").value(5))
			.andExpect(jsonPath("$.content[0].comment").value("Excellent"))
			.andExpect(jsonPath("$.totalElements").value(1));
	}

	@Test
	void getMovieReviews_whenMediaMissing_importsMediaAndReturnsEmptyPage() throws Exception {
		when(tmdbClient.fetchMediaById(eq(7013L), eq(com.project.watchmate.media.catalog.domain.MediaType.MOVIE)))
			.thenReturn(TmdbMovieDTO.builder()
				.id(7013L)
				.title("Imported Review Movie")
				.overview("Imported for review listing")
				.posterPath("/imported-review-movie.jpg")
				.releaseDate("2024-02-01")
				.voteAverage(7.1)
				.genres(List.of())
				.build());

		mockMvc.perform(get("/api/v1/movies/{tmdbId}/reviews", 7013L))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content.length()").value(0))
			.andExpect(jsonPath("$.totalElements").value(0));

		assertThat(mediaRepository.findByTmdbIdAndType(7013L, com.project.watchmate.media.catalog.domain.MediaType.MOVIE)).isPresent();
	}

	@Test
	void getShowReviewsByTmdbId_unauthenticated_returns200() throws Exception {
		Users user = saveUser("review-show-list-user", true);
		Media media = saveMedia(7010L, "Review Listing Show", com.project.watchmate.media.catalog.domain.MediaType.SHOW);
		saveReview(user, media, 4, "Great show");

		mockMvc.perform(get("/api/v1/shows/{tmdbId}/reviews", media.getTmdbId()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[0].reviewId").isNumber())
			.andExpect(jsonPath("$.content[0].starRating").value(4))
			.andExpect(jsonPath("$.content[0].comment").value("Great show"))
			.andExpect(jsonPath("$.totalElements").value(1));
	}

	@Test
	void getShowReviews_whenMediaMissing_importsMediaAndReturnsEmptyPage() throws Exception {
		when(tmdbClient.fetchMediaById(eq(7014L), eq(com.project.watchmate.media.catalog.domain.MediaType.SHOW)))
			.thenReturn(TmdbMovieDTO.builder()
				.id(7014L)
				.title("Imported Review Show")
				.overview("Imported show for review listing")
				.posterPath("/imported-review-show.jpg")
				.releaseDate("2024-03-01")
				.voteAverage(7.6)
				.genres(List.of())
				.build());

		mockMvc.perform(get("/api/v1/shows/{tmdbId}/reviews", 7014L))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content.length()").value(0))
			.andExpect(jsonPath("$.totalElements").value(0));

		assertThat(mediaRepository.findByTmdbIdAndType(7014L, com.project.watchmate.media.catalog.domain.MediaType.SHOW)).isPresent();
	}

	@Test
	void getMovieReviews_withSizeParam_returnsPaginatedSubset() throws Exception {
		Users user1 = saveUser("review-page-user-1", true);
		Users user2 = saveUser("review-page-user-2", true);
		Media media = saveMedia(7012L, "Paginated Review Movie", com.project.watchmate.media.catalog.domain.MediaType.MOVIE);
		saveReview(user1, media, 5, "First");
		saveReview(user2, media, 4, "Second");

		mockMvc.perform(get("/api/v1/movies/{tmdbId}/reviews", media.getTmdbId())
			.param("page", "0")
			.param("size", "1"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content.length()").value(1))
			.andExpect(jsonPath("$.totalElements").value(2))
			.andExpect(jsonPath("$.totalPages").value(2));
	}

	@Test
	void getReviewById_unauthenticated_returns200() throws Exception {
		Users user = saveUser("review-get-user", true);
		Media media = saveMedia(7011L, "Single Review Movie", com.project.watchmate.media.catalog.domain.MediaType.MOVIE);
		Review review = saveReview(user, media, 3, "Decent");

		mockMvc.perform(get("/api/v1/reviews/{reviewId}", review.getId()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.reviewId").value(review.getId().intValue()))
			.andExpect(jsonPath("$.starRating").value(3))
			.andExpect(jsonPath("$.comment").value("Decent"));
	}

	@Test
	void deleteReview_byOwner_returns204() throws Exception {
		Users owner = saveUser("review-delete-owner", true);
		Media media = saveMedia(7005L, "Owner Delete Movie", com.project.watchmate.media.catalog.domain.MediaType.MOVIE);
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
		Media media = saveMedia(7006L, "User Delete Movie", com.project.watchmate.media.catalog.domain.MediaType.MOVIE);
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
		Media media = saveMedia(7007L, "Moderator Delete Movie", com.project.watchmate.media.catalog.domain.MediaType.MOVIE);
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
		Media media = saveMedia(7008L, "Admin Delete Movie", com.project.watchmate.media.catalog.domain.MediaType.MOVIE);
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

	private String createReviewBody(Long tmdbId, String type, int starRating, String comment) {
		return """
			{"tmdbId":%d,"type":"%s","starRating":%d,"comment":"%s"}
			""".formatted(tmdbId, type, starRating, comment);
	}

	private String updateReviewBody(int starRating, String comment) {
		return """
			{"starRating":%d,"comment":"%s"}
			""".formatted(starRating, comment);
	}
}






