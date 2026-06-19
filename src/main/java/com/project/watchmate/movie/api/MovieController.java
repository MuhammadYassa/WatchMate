package com.project.watchmate.movie.api;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.watchmate.common.error.ApiError;
import com.project.watchmate.movie.dto.MovieDetailsDTO;
import com.project.watchmate.review.dto.ReviewResponseDTO;
import com.project.watchmate.common.dto.UpdateWatchStatusRequestDTO;
import com.project.watchmate.movie.tracking.dto.UserMediaStatusDTO;
import com.project.watchmate.media.catalog.domain.MediaType;
import com.project.watchmate.common.security.auth.UserPrincipal;
import com.project.watchmate.user.domain.Users;
import com.project.watchmate.movie.application.MediaService;
import com.project.watchmate.review.application.ReviewService;
import com.project.watchmate.movie.tracking.application.StatusService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
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
@RequestMapping("/api/v1/movies")
@RequiredArgsConstructor
@Validated
@Tag(name = "Movies", description = "Movie details, reviews, and watch status endpoints.")
public class MovieController {

    private static final MediaType MEDIA_TYPE = MediaType.MOVIE;

    private final MediaService mediaService;

    private final StatusService statusService;

    private final ReviewService reviewService;

    @GetMapping("/{tmdbId}")
    @Operation(summary = "Get movie details", description = "Returns public movie metadata. When authenticated, the response also includes user-specific fields such as favourite and watch status.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Movie details returned. User-specific fields are null when unauthenticated.", content = @Content(schema = @Schema(implementation = MovieDetailsDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request parameter", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "401", description = "Authentication failed", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Movie not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<MovieDetailsDTO> getMovieDetails(
        @PathVariable @Min(1) Long tmdbId,
        @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        Users user = userPrincipal == null ? null : userPrincipal.getUser();
        MovieDetailsDTO dto = mediaService.getMovieDetails(tmdbId, user);
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/{tmdbId}/status")
    @Operation(summary = "Update movie watch status", description = "Updates the authenticated user's watch status for a movie.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Watch status updated", content = @Content(schema = @Schema(implementation = UserMediaStatusDTO.class))),
        @ApiResponse(responseCode = "400", description = "Validation failed or watch status is invalid", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "401", description = "Authentication failed", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Movie not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<UserMediaStatusDTO> updateStatus(
        @PathVariable @Min(1) Long tmdbId,
        @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal,
        @Valid @RequestBody UpdateWatchStatusRequestDTO request
    ) {
        Users user = userPrincipal.getUser();
        UserMediaStatusDTO dto = statusService.updateWatchStatus(user, tmdbId, MEDIA_TYPE, request);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/{tmdbId}/reviews")
    @Operation(summary = "List movie reviews", description = "Returns reviews for a movie identified by TMDB ID.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Movie reviews returned", content = @Content(array = @ArraySchema(schema = @Schema(implementation = ReviewResponseDTO.class)))),
        @ApiResponse(responseCode = "400", description = "Invalid path parameter", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "401", description = "Authentication failed", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Movie not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<List<ReviewResponseDTO>> getReviews(
        @PathVariable @Min(1) Long tmdbId
    ) {
        List<ReviewResponseDTO> reviewResponses = reviewService.getReviews(tmdbId, MEDIA_TYPE);
        return ResponseEntity.ok(reviewResponses);
    }
}







