package com.project.watchmate.Controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.watchmate.Models.UserPrincipal;
import com.project.watchmate.Models.Users;
import com.project.watchmate.Dto.CreateReviewRequestDTO;
import com.project.watchmate.Dto.ReviewResponseDTO;
import com.project.watchmate.Dto.UpdateReviewRequestDTO;
import com.project.watchmate.Services.ReviewService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/create")
    public ResponseEntity<ReviewResponseDTO> createReview(@AuthenticationPrincipal UserPrincipal userPrincipal, @RequestBody CreateReviewRequestDTO reviewRequest) {
        Users user = userPrincipal.getUser();
        ReviewResponseDTO reviewResponse = reviewService.createReview(user, reviewRequest);
        return ResponseEntity.ok(reviewResponse);
    }

    @PutMapping("/update")
    public ResponseEntity<ReviewResponseDTO> updateReview(@AuthenticationPrincipal UserPrincipal userPrincipal, @RequestBody UpdateReviewRequestDTO updateReviewRequest) {
        Users user = userPrincipal.getUser();
        ReviewResponseDTO reviewResponse = reviewService.updateReview(user, updateReviewRequest);
        return ResponseEntity.ok(reviewResponse);
    }
    
    @DeleteMapping("/delete/{reviewId}")
    public ResponseEntity<Void> deleteReview(@AuthenticationPrincipal UserPrincipal userPrincipal, @PathVariable Long reviewId) {
        Users user = userPrincipal.getUser();
        reviewService.deleteReview(user, reviewId);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/get/{mediaId}")
    public ResponseEntity<List<ReviewResponseDTO>> getReviews(@AuthenticationPrincipal UserPrincipal userPrincipal, @PathVariable Long mediaId) {
        Users user = userPrincipal.getUser();
        List<ReviewResponseDTO> reviewResponses = reviewService.getReviews(user, mediaId);
        return ResponseEntity.ok(reviewResponses);
    }
    
    @GetMapping("/get/{reviewId}")
    public ResponseEntity<ReviewResponseDTO> getReview(@AuthenticationPrincipal UserPrincipal userPrincipal, @PathVariable Long reviewId) {
        Users user = userPrincipal.getUser();
        ReviewResponseDTO reviewResponse = reviewService.getReview(user, reviewId);
        return ResponseEntity.ok(reviewResponse);
    }
    
}
