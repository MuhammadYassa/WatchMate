package com.project.watchmate.Services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.project.watchmate.Dto.CreateReviewRequestDTO;
import com.project.watchmate.Dto.ReviewResponseDTO;
import com.project.watchmate.Dto.UpdateReviewRequestDTO;
import com.project.watchmate.Exception.MediaNotFoundException;
import com.project.watchmate.Exception.DuplicateReviewException;
import com.project.watchmate.Exception.ReviewNotFoundException;
import com.project.watchmate.Exception.UnauthorizedReviewAccessException;
import com.project.watchmate.Models.Review;
import com.project.watchmate.Models.Users;
import com.project.watchmate.Mappers.WatchMateMapper;
import com.project.watchmate.Repositories.MediaRepository;
import com.project.watchmate.Repositories.ReviewRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;

    private final MediaRepository mediaRepository;

    private final WatchMateMapper watchMateMapper;

    public ReviewResponseDTO createReview(Users user, CreateReviewRequestDTO createReviewRequest) {
        if (reviewRepository.existsByUserAndMedia(user, mediaRepository.findById(createReviewRequest.getMediaId()).orElseThrow(() -> new MediaNotFoundException("Media not found")))) {
            throw new DuplicateReviewException("You have already reviewed this media");
        }
        Review review = reviewRepository.save(Review.builder()
        .user(user)
        .media(mediaRepository.findById(createReviewRequest.getMediaId()).orElseThrow(() -> new MediaNotFoundException("Media not found")))
        .rating(createReviewRequest.getStarRating())
        .comment(createReviewRequest.getComment())
        .datePosted(LocalDateTime.now())
        .dateLastModified(LocalDateTime.now())
        .build());
        return watchMateMapper.mapToReviewResponseDTO(review);
    }

    public ReviewResponseDTO updateReview(Users user, UpdateReviewRequestDTO updateReviewRequest) {
        Review review = reviewRepository.findById(updateReviewRequest.getReviewId()).orElseThrow(() -> new ReviewNotFoundException("Review not found"));
        if (!review.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedReviewAccessException("You do not own this review");
        }
        review.setRating(updateReviewRequest.getStarRating());
        review.setComment(updateReviewRequest.getComment());
        review.setDateLastModified(LocalDateTime.now());
        reviewRepository.save(review);
        return watchMateMapper.mapToReviewResponseDTO(review);
    }

    public void deleteReview(Users user, Long reviewId) {
        Review review = reviewRepository.findById(reviewId).orElseThrow(() -> new ReviewNotFoundException("Review not found"));
        if (!review.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedReviewAccessException("You do not own this review");
        }
        reviewRepository.delete(review);
        return;
    }

    public List<ReviewResponseDTO> getReviews(Users user, Long mediaId) {
        List<Review> reviews = reviewRepository.findByMedia(mediaRepository.findById(mediaId).orElseThrow(() -> new MediaNotFoundException("Media not found")));
        return reviews.stream().map(review -> watchMateMapper.mapToReviewResponseDTO(review)).collect(Collectors.toList());
    }

    public ReviewResponseDTO getReview(Users user, Long reviewId) {
        Review review = reviewRepository.findById(reviewId).orElseThrow(() -> new ReviewNotFoundException("Review not found"));
        return watchMateMapper.mapToReviewResponseDTO(review);
    }

}
