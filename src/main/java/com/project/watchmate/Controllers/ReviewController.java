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
import com.project.watchmate.Dto.ApiError;
import com.project.watchmate.Dto.CreateReviewRequestDTO;
import com.project.watchmate.Dto.ReviewResponseDTO;
import com.project.watchmate.Dto.UpdateReviewRequestDTO;
import com.project.watchmate.Services.ReviewService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/reviews")
@Validated
@Tag(name = "Reviews", description = "Authenticated review management endpoints.")
@SecurityRequirement(name = "bearerAuth")
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/create")
    @Operation(summary = "Create review", description = "Creates a new review for a media item.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Review created", content = @Content(schema = @Schema(implementation = ReviewResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "401", description = "Authentication failed", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Media not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "409", description = "Review already exists for this media", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<ReviewResponseDTO> createReview(
        @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal,
        @Valid @RequestBody CreateReviewRequestDTO reviewRequest
    ) {
        Users user = userPrincipal.getUser();
        ReviewResponseDTO reviewResponse = reviewService.createReview(user, reviewRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(reviewResponse);
    }

    @PatchMapping("/{reviewId}")
    @Operation(summary = "Update review", description = "Updates an existing review owned by the authenticated user.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Review updated", content = @Content(schema = @Schema(implementation = ReviewResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "401", description = "Authentication failed", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "Review not owned by user", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Review not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<ReviewResponseDTO> updateReview(
        @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal,
        @Valid @RequestBody UpdateReviewRequestDTO updateReviewRequest,
        @PathVariable @Min(1) Long reviewId
    ) {
        Users user = userPrincipal.getUser();
        ReviewResponseDTO reviewResponse = reviewService.updateReview(user, updateReviewRequest, reviewId);
        return ResponseEntity.ok(reviewResponse);
    }
    
    @DeleteMapping("/{reviewId}")
    @Operation(summary = "Delete review", description = "Deletes an existing review owned by the authenticated user.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Review deleted"),
        @ApiResponse(responseCode = "400", description = "Invalid path parameter", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "401", description = "Authentication failed", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "Review not owned by user", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Review not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<Void> deleteReview(
        @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal,
        @PathVariable @Min(1) Long reviewId
    ) {
        Users user = userPrincipal.getUser();
        reviewService.deleteReview(user, reviewId);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/{reviewId}")
    @Operation(summary = "Get review", description = "Returns a review by its identifier.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Review returned", content = @Content(schema = @Schema(implementation = ReviewResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid path parameter", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "401", description = "Authentication failed", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Review not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<ReviewResponseDTO> getReview(
        @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal,
        @PathVariable @Min(1) Long reviewId
    ) {
        Users user = userPrincipal.getUser();
        ReviewResponseDTO reviewResponse = reviewService.getReview(user, reviewId);
        return ResponseEntity.ok(reviewResponse);
    }
    
}
