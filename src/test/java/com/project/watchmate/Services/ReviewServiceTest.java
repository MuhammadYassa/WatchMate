package com.project.watchmate.Services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.project.watchmate.Dto.CreateReviewRequestDTO;
import com.project.watchmate.Dto.ReviewResponseDTO;
import com.project.watchmate.Dto.UpdateReviewRequestDTO;
import com.project.watchmate.Exception.DuplicateReviewException;
import com.project.watchmate.Exception.MediaNotFoundException;
import com.project.watchmate.Exception.ReviewNotFoundException;
import com.project.watchmate.Exception.UnauthorizedReviewAccessException;
import com.project.watchmate.Mappers.WatchMateMapper;
import com.project.watchmate.Models.Media;
import com.project.watchmate.Models.Review;
import com.project.watchmate.Models.Users;
import com.project.watchmate.Repositories.MediaRepository;
import com.project.watchmate.Repositories.ReviewRepository;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private MediaRepository mediaRepository;

    @Mock
    private WatchMateMapper watchMateMapper;

    @InjectMocks
    private ReviewService reviewService;

    private Users user;
    private Users otherUser;
    private Media media;
    private Review review;
    private static final Long MEDIA_ID = 1L;
    private static final Long REVIEW_ID = 10L;

    @BeforeEach
    void setUp() {
        user = Users.builder().id(1L).username("author").build();
        otherUser = Users.builder().id(2L).username("other").build();
        media = Media.builder().id(MEDIA_ID).tmdbId(100L).title("Movie").build();
        review = Review.builder().id(REVIEW_ID).user(user).media(media).rating(5).comment("Great").build();
    }

    @Nested
    @DisplayName("createReview")
    class CreateReviewTests {

        @Test
        void createReview_WhenValid_SavesAndReturnsDto() {
            CreateReviewRequestDTO request = CreateReviewRequestDTO.builder().mediaId(MEDIA_ID).starRating(5).comment("Great").build();
            when(mediaRepository.findById(MEDIA_ID)).thenReturn(Optional.of(media));
            when(reviewRepository.existsByUserAndMedia(user, media)).thenReturn(false);
            when(reviewRepository.save(any(Review.class))).thenReturn(review);
            ReviewResponseDTO dto = ReviewResponseDTO.builder().reviewId(REVIEW_ID).build();
            when(watchMateMapper.mapToReviewResponseDTO(any(Review.class))).thenReturn(dto);

            ReviewResponseDTO result = reviewService.createReview(user, request);

            assertEquals(REVIEW_ID, result.getReviewId());
            verify(reviewRepository).save(any(Review.class));
        }

        @Test
        void createReview_WhenMediaNotFound_ThrowsMediaNotFoundException() {
            CreateReviewRequestDTO request = CreateReviewRequestDTO.builder().mediaId(MEDIA_ID).build();
            when(mediaRepository.findById(MEDIA_ID)).thenReturn(Optional.empty());
            
            MediaNotFoundException exception = assertThrows(MediaNotFoundException.class, () -> reviewService.createReview(user, request));
            assertEquals("Media not found", exception.getMessage());
            verify(reviewRepository, never()).save(any(Review.class));
        }

        @Test
        void createReview_WhenAlreadyReviewed_ThrowsDuplicateReviewException() {
            CreateReviewRequestDTO request = CreateReviewRequestDTO.builder().mediaId(MEDIA_ID).build();
            when(mediaRepository.findById(MEDIA_ID)).thenReturn(Optional.of(media));
            when(reviewRepository.existsByUserAndMedia(user, media)).thenReturn(true);

            DuplicateReviewException e = assertThrows(DuplicateReviewException.class,
                () -> reviewService.createReview(user, request));
            assertEquals("You have already reviewed this media", e.getMessage());
            verify(reviewRepository, never()).save(any(Review.class));
        }
    }

    @Nested
    @DisplayName("updateReview")
    class UpdateReviewTests {

        @Test
        void updateReview_WhenOwner_UpdatesAndSaves() {
            UpdateReviewRequestDTO request = UpdateReviewRequestDTO.builder().starRating(4).comment("Updated").build();
            when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));
            when(reviewRepository.save(any(Review.class))).thenReturn(review);
            ReviewResponseDTO dto = ReviewResponseDTO.builder().reviewId(REVIEW_ID).build();
            when(watchMateMapper.mapToReviewResponseDTO(any(Review.class))).thenReturn(dto);

            reviewService.updateReview(user, request, REVIEW_ID);

            assertEquals(4, review.getRating());
            assertEquals("Updated", review.getComment());
            verify(reviewRepository).save(review);
        }

        @Test
        void updateReview_WhenNotOwner_ThrowsUnauthorizedReviewAccessException() {
            UpdateReviewRequestDTO request = UpdateReviewRequestDTO.builder().build();
            when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));

            UnauthorizedReviewAccessException e = assertThrows(UnauthorizedReviewAccessException.class,
                () -> reviewService.updateReview(otherUser, request, REVIEW_ID));
            assertEquals("You do not own this review", e.getMessage());
            verify(reviewRepository, never()).save(any(Review.class));
        }

        @Test
        void updateReview_WhenReviewNotFound_ThrowsReviewNotFoundException() {
            UpdateReviewRequestDTO request = UpdateReviewRequestDTO.builder().build();
            when(reviewRepository.findById(999L)).thenReturn(Optional.empty());

            ReviewNotFoundException e = assertThrows(ReviewNotFoundException.class, () -> reviewService.updateReview(user, request, 999L));
            assertEquals("Review not found", e.getMessage());
            verify(reviewRepository, never()).save(any(Review.class));
        }
    }

    @Nested
    @DisplayName("deleteReview")
    class DeleteReviewTests {

        @Test
        void deleteReview_WhenOwner_DeletesReview() {
            when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));

            reviewService.deleteReview(user, REVIEW_ID);

            verify(reviewRepository).delete(review);
        }

        @Test
        void deleteReview_WhenNotOwner_ThrowsUnauthorizedReviewAccessException() {
            when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));

            UnauthorizedReviewAccessException e = assertThrows(UnauthorizedReviewAccessException.class,
                () -> reviewService.deleteReview(otherUser, REVIEW_ID));

            assertEquals("You do not own this review", e.getMessage());
            verify(reviewRepository, never()).save(any(Review.class));
            verify(reviewRepository, never()).delete(any(Review.class));
        }

        @Test
        void deleteReview_WhenReviewNotFound_ThrowsReviewNotFoundException() {
            when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.empty());

            ReviewNotFoundException e = assertThrows(ReviewNotFoundException.class, () -> reviewService.deleteReview(user, REVIEW_ID));

            assertEquals("Review not found", e.getMessage());
        }
    }

    @Nested
    @DisplayName("getReviews")
    class GetReviewsTests {

        @Test
        void getReviews_WhenMediaExists_ReturnsMappedList() {
            when(mediaRepository.findById(MEDIA_ID)).thenReturn(Optional.of(media));
            when(reviewRepository.findByMedia(media)).thenReturn(List.of(review));
            ReviewResponseDTO dto = ReviewResponseDTO.builder().reviewId(REVIEW_ID).build();
            when(watchMateMapper.mapToReviewResponseDTO(any(Review.class))).thenReturn(dto);

            List<ReviewResponseDTO> result = reviewService.getReviews(user, MEDIA_ID);

            assertEquals(1, result.size());
            assertEquals(REVIEW_ID, result.get(0).getReviewId());
        }

        @Test
        void getReviews_WhenMediaNotFound_ThrowsMediaNotFoundException() {
            when(mediaRepository.findById(MEDIA_ID)).thenReturn(Optional.empty());

            MediaNotFoundException e = assertThrows(MediaNotFoundException.class, () -> reviewService.getReviews(user, MEDIA_ID));

            assertEquals("Media not found", e.getMessage());
        }
    }

    @Nested
    @DisplayName("getReview")
    class GetReviewTests {

        @Test
        void getReview_WhenExists_ReturnsMappedDto() {
            when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));
            ReviewResponseDTO dto = ReviewResponseDTO.builder().reviewId(REVIEW_ID).build();
            when(watchMateMapper.mapToReviewResponseDTO(review)).thenReturn(dto);

            ReviewResponseDTO result = reviewService.getReview(user, REVIEW_ID);

            assertNotNull(result);
            assertEquals(REVIEW_ID, result.getReviewId());
        }

        @Test
        void getReview_WhenNotFound_ThrowsReviewNotFoundException() {
            when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.empty());

            assertThrows(ReviewNotFoundException.class, () -> reviewService.getReview(user, REVIEW_ID));
        }
    }
}
