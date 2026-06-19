package com.project.watchmate.show.api;

import java.net.URI;
import java.util.List;

import org.springframework.http.HttpHeaders;
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
import com.project.watchmate.show.metadata.dto.NextEpisodeAiringDTO;
import com.project.watchmate.review.dto.ReviewResponseDTO;
import com.project.watchmate.show.metadata.dto.ShowDetailsDTO;
import com.project.watchmate.show.tracking.dto.ShowTrackingDTO;
import com.project.watchmate.show.jobs.dto.ShowTrackingJobDTO;
import com.project.watchmate.show.tracking.dto.ShowTrackingStatusDTO;
import com.project.watchmate.show.metadata.dto.ShowSeasonsDetailsDTO;
import com.project.watchmate.show.tracking.dto.UpdateShowTrackingPositionRequestDTO;
import com.project.watchmate.common.dto.UpdateWatchStatusRequestDTO;
import com.project.watchmate.show.tracking.dto.WatchedEpisodeDTO;
import com.project.watchmate.media.catalog.domain.MediaType;
import com.project.watchmate.common.security.auth.UserPrincipal;
import com.project.watchmate.user.domain.Users;
import com.project.watchmate.review.application.ReviewService;
import com.project.watchmate.show.metadata.application.ShowMetadataService;
import com.project.watchmate.show.tracking.application.ShowTrackingActionResult;
import com.project.watchmate.show.tracking.application.ShowTrackingService;

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
@RequestMapping("/api/v1/shows")
@RequiredArgsConstructor
@Validated
@Tag(name = "Shows", description = "Show details, progress, reviews, and metadata endpoints.")
public class ShowController {

    private static final MediaType MEDIA_TYPE = MediaType.SHOW;

    private final ReviewService reviewService;

    private final ShowTrackingService showTrackingService;

    private final ShowMetadataService showMetadataService;

    @GetMapping("/{tmdbId}")
    @Operation(summary = "Get show details", description = "Returns public show metadata including season summaries. When authenticated, the response also includes user-specific fields such as favourite and watch status.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Show details returned. User-specific fields are null when unauthenticated.", content = @Content(schema = @Schema(implementation = ShowDetailsDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request parameter", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "401", description = "Authentication failed", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Show not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<ShowDetailsDTO> getShowDetails(
        @PathVariable @Min(1) Long tmdbId,
        @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        Users user = userPrincipal == null ? null : userPrincipal.getUser();
        ShowDetailsDTO dto = showMetadataService.getShowDetails(tmdbId, MEDIA_TYPE, user);
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/{tmdbId}/status")
    @Operation(summary = "Set show tracking status", description = "Sets the authenticated user's canonical show tracking status. NONE is returned but not stored. WATCHED on ongoing shows normalizes to UP_TO_DATE. When required metadata is already local, the action completes immediately with 200. Small missing metadata gaps are hydrated synchronously, while larger gaps return 202 and continue in the background.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Show tracking status updated", content = @Content(schema = @Schema(implementation = ShowTrackingStatusDTO.class))),
        @ApiResponse(responseCode = "202", description = "Show tracking update accepted and processing in the background. Poll the Location header for job state.", content = @Content(schema = @Schema(implementation = ShowTrackingJobDTO.class))),
        @ApiResponse(responseCode = "400", description = "Validation failed or watch status is invalid", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "401", description = "Authentication failed", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "422", description = "The server could not complete or queue the required metadata hydration for the requested bulk status change", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Show not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<?> updateStatus(
        @PathVariable @Min(1) Long tmdbId,
        @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal,
        @Valid @RequestBody UpdateWatchStatusRequestDTO request
    ) {
        Users user = userPrincipal.getUser();
        ShowTrackingActionResult<ShowTrackingStatusDTO> result = showTrackingService.setShowStatus(user, tmdbId, MEDIA_TYPE, request);
        return result.isAccepted()
            ? acceptedJobResponse(result.acceptedJob())
            : ResponseEntity.ok(result.completedBody());
    }

    @GetMapping("/{tmdbId}/progress")
    @Operation(summary = "Get show tracking", description = "Returns canonical show tracking for the authenticated user. When no tracking row exists, the response returns status NONE without storing it.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Show tracking returned", content = @Content(schema = @Schema(implementation = ShowTrackingDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request parameter", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "401", description = "Authentication failed", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Show not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<ShowTrackingDTO> getShowTracking(
        @PathVariable @Min(1) Long tmdbId,
        @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        Users user = userPrincipal.getUser();
        return ResponseEntity.ok(showTrackingService.getShowTracking(user, tmdbId, MEDIA_TYPE));
    }

    @PutMapping("/{tmdbId}/progress")
    @Operation(summary = "Set show watch position", description = "Sets the authenticated user's canonical show watch position. Watched episode rows are replaced with the exact contiguous prefix from the first episode through the supplied season and episode. Small metadata gaps may be hydrated synchronously and larger gaps are accepted for background processing with 202.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Show tracking updated", content = @Content(schema = @Schema(implementation = ShowTrackingDTO.class))),
        @ApiResponse(responseCode = "202", description = "Show progress update accepted and processing in the background. Poll the Location header for job state.", content = @Content(schema = @Schema(implementation = ShowTrackingJobDTO.class))),
        @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "401", description = "Authentication failed", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "422", description = "The server could not complete or queue the metadata hydration required to set show progress", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Show not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<?> updateWatchPosition(
        @PathVariable @Min(1) Long tmdbId,
        @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal,
        @Valid @RequestBody UpdateShowTrackingPositionRequestDTO request
    ) {
        Users user = userPrincipal.getUser();
        ShowTrackingActionResult<ShowTrackingDTO> result = showTrackingService.updateWatchPosition(user, tmdbId, MEDIA_TYPE, request);
        return result.isAccepted()
            ? acceptedJobResponse(result.acceptedJob())
            : ResponseEntity.ok(result.completedBody());
    }

    @GetMapping("/{tmdbId}/episodes/watched")
    @Operation(summary = "List watched show episodes", description = "Returns canonical watched-episode rows for a tracked show. Row exists means watched.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Watched episodes returned", content = @Content(array = @ArraySchema(schema = @Schema(implementation = WatchedEpisodeDTO.class)))),
        @ApiResponse(responseCode = "400", description = "Invalid request parameter", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "401", description = "Authentication failed", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Show not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<List<WatchedEpisodeDTO>> getWatchedEpisodes(
        @PathVariable @Min(1) Long tmdbId,
        @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        Users user = userPrincipal.getUser();
        return ResponseEntity.ok(showTrackingService.getWatchedEpisodes(user, tmdbId, MEDIA_TYPE));
    }

    @GetMapping("/{tmdbId}/next-episode")
    @Operation(summary = "Get next episode airing", description = "Returns public next-episode metadata for a show.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Next episode metadata returned", content = @Content(schema = @Schema(implementation = NextEpisodeAiringDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request parameter", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Show not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<NextEpisodeAiringDTO> getNextEpisode(
        @PathVariable @Min(1) Long tmdbId
    ) {
        return ResponseEntity.ok(showMetadataService.getNextEpisode(tmdbId, MEDIA_TYPE));
    }

    @GetMapping("/{tmdbId}/seasons/{seasonNumber}/episodes")
    @Operation(summary = "Get public show season episodes", description = "Returns public episode details for one requested season. Season and episode metadata is cached lazily per season, and authenticated responses include watched flags from canonical watched-episode rows.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Public season episode details returned", content = @Content(schema = @Schema(implementation = ShowSeasonsDetailsDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request parameter", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Show or season not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<ShowSeasonsDetailsDTO> getShowSeasonDetails(
        @PathVariable @Min(1) Long tmdbId,
        @PathVariable @Min(0) Integer seasonNumber,
        @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        Users user = userPrincipal == null ? null : userPrincipal.getUser();
        return ResponseEntity.ok(showMetadataService.getShowSeasonDetails(tmdbId, seasonNumber, MEDIA_TYPE, user));
    }

    @GetMapping("/{tmdbId}/reviews")
    @Operation(summary = "List show reviews", description = "Returns reviews for a show identified by TMDB ID.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Show reviews returned", content = @Content(array = @ArraySchema(schema = @Schema(implementation = ReviewResponseDTO.class)))),
        @ApiResponse(responseCode = "400", description = "Invalid path parameter", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "401", description = "Authentication failed", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Show not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<List<ReviewResponseDTO>> getReviews(
        @PathVariable @Min(1) Long tmdbId
    ) {
        List<ReviewResponseDTO> reviewResponses = reviewService.getReviews(tmdbId, MEDIA_TYPE);
        return ResponseEntity.ok(reviewResponses);
    }

    private ResponseEntity<ShowTrackingJobDTO> acceptedJobResponse(ShowTrackingJobDTO job) {
        return ResponseEntity.accepted()
            .location(URI.create("/api/v1/show-tracking-jobs/" + job.getJobId()))
            .header(HttpHeaders.RETRY_AFTER, "2")
            .body(job);
    }
}





