package com.project.watchmate.review.application;

import com.project.watchmate.media.catalog.application.MediaResolutionService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.project.watchmate.common.cache.WatchMateCacheEvictionService;
import com.project.watchmate.review.dto.CreateReviewRequestDTO;
import com.project.watchmate.review.dto.ReviewResponseDTO;
import com.project.watchmate.review.dto.UpdateReviewRequestDTO;
import com.project.watchmate.common.error.DuplicateReviewException;
import com.project.watchmate.common.error.ReviewNotFoundException;
import com.project.watchmate.common.error.UnauthorizedReviewAccessException;
import com.project.watchmate.common.mapper.WatchMateMapper;
import com.project.watchmate.media.catalog.domain.Media;
import com.project.watchmate.media.catalog.domain.MediaType;
import com.project.watchmate.review.domain.Review;
import com.project.watchmate.user.domain.Role;
import com.project.watchmate.user.domain.Users;
import com.project.watchmate.review.persistence.ReviewRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;

    private final MediaResolutionService mediaResolutionService;

    private final WatchMateMapper watchMateMapper;

    private final WatchMateCacheEvictionService cacheEvictionService;

    public ReviewResponseDTO createReview(Users user, CreateReviewRequestDTO createReviewRequest) {
        Media media = mediaResolutionService.resolveMediaByTmdbId(createReviewRequest.getTmdbId(), createReviewRequest.getType());
        if (reviewRepository.existsByUserAndMedia(user, media)) {
            throw new DuplicateReviewException("You have already reviewed this media");
        }
        Review review = reviewRepository.save(Objects.requireNonNull(Review.builder()
        .user(user)
        .media(media)
        .rating(createReviewRequest.getStarRating())
        .comment(createReviewRequest.getComment())
        .datePosted(LocalDateTime.now())
        .dateLastModified(LocalDateTime.now())
        .build()));
        cacheEvictionService.evictWatchlistSummaryPages();
        return watchMateMapper.mapToReviewResponseDTO(review);
    }

    public ReviewResponseDTO updateReview(Users user, UpdateReviewRequestDTO updateReviewRequest, Long reviewId) {
        Review review = reviewRepository.findById(reviewId).orElseThrow(() -> new ReviewNotFoundException("Review not found"));
        if (!review.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedReviewAccessException("You do not own this review");
        }
        review.setRating(updateReviewRequest.getStarRating());
        review.setComment(updateReviewRequest.getComment());
        review.setDateLastModified(LocalDateTime.now());
        reviewRepository.save(review);
        cacheEvictionService.evictWatchlistSummaryPages();
        return watchMateMapper.mapToReviewResponseDTO(review);
    }

    public void deleteReview(Users user, Long reviewId) {
        Review review = reviewRepository.findById(Objects.requireNonNull(reviewId, "reviewId")).orElseThrow(() -> new ReviewNotFoundException("Review not found"));
        if (!canDeleteReview(user, review)) {
            throw new UnauthorizedReviewAccessException("You do not own this review");
        }
        reviewRepository.delete(review);
        cacheEvictionService.evictWatchlistSummaryPages();
        return;
    }

    public List<ReviewResponseDTO> getReviews(Users user, Long tmdbId, MediaType mediaType) {
        Media media = mediaResolutionService.resolveMediaByTmdbId(tmdbId, mediaType);
        List<Review> reviews = reviewRepository.findByMedia(media);
        return reviews.stream().map(review -> watchMateMapper.mapToReviewResponseDTO(review)).collect(Collectors.toList());
    }

    public ReviewResponseDTO getReview(Users user, Long reviewId) {
        Review review = reviewRepository.findById(Objects.requireNonNull(reviewId, "reviewId")).orElseThrow(() -> new ReviewNotFoundException("Review not found"));
        return watchMateMapper.mapToReviewResponseDTO(review);
    }

    public Page<Review> getReviewPage(Users user){
        Pageable pageable = PageRequest.of(0, 5, Sort.by("dateLastModified").descending().and(Sort.by("datePosted").descending()));
        return reviewRepository.findAllByUser(user, pageable);
    }

    private boolean canDeleteReview(Users user, Review review) {
        return review.getUser().getId().equals(user.getId())
            || user.getRole() == Role.MODERATOR
            || user.getRole() == Role.ADMIN;
    }

}






