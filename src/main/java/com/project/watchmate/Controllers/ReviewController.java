package com.project.watchmate.Controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.watchmate.Models.UserPrincipal;
import com.project.watchmate.Models.Users;
import com.project.watchmate.Dto.CreateReviewRequestDTO;
import com.project.watchmate.Dto.ReviewResponseDTO;
import com.project.watchmate.Dto.UpdateReviewRequestDTO;
import com.project.watchmate.Services.ReviewService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/reviews")
@Validated
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/create")
    public ResponseEntity<ReviewResponseDTO> createReview(
        @AuthenticationPrincipal UserPrincipal userPrincipal,
        @Valid @RequestBody CreateReviewRequestDTO reviewRequest
    ) {
        Users user = userPrincipal.getUser();
        ReviewResponseDTO reviewResponse = reviewService.createReview(user, reviewRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(reviewResponse);
    }

    @PatchMapping("/{reviewId}")
    public ResponseEntity<ReviewResponseDTO> updateReview(
        @AuthenticationPrincipal UserPrincipal userPrincipal,
        @Valid @RequestBody UpdateReviewRequestDTO updateReviewRequest,
        @PathVariable @Min(1) Long reviewId
    ) {
        Users user = userPrincipal.getUser();
        ReviewResponseDTO reviewResponse = reviewService.updateReview(user, updateReviewRequest, reviewId);
        return ResponseEntity.ok(reviewResponse);
    }
    
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> deleteReview(
        @AuthenticationPrincipal UserPrincipal userPrincipal,
        @PathVariable @Min(1) Long reviewId
    ) {
        Users user = userPrincipal.getUser();
        reviewService.deleteReview(user, reviewId);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/{reviewId}")
    public ResponseEntity<ReviewResponseDTO> getReview(
        @AuthenticationPrincipal UserPrincipal userPrincipal,
        @PathVariable @Min(1) Long reviewId
    ) {
        Users user = userPrincipal.getUser();
        ReviewResponseDTO reviewResponse = reviewService.getReview(user, reviewId);
        return ResponseEntity.ok(reviewResponse);
    }
    
}
