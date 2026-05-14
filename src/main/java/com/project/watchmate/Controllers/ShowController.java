package com.project.watchmate.Controllers;

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

import com.project.watchmate.Dto.ApiError;
import com.project.watchmate.Dto.EpisodeProgressDTO;
import com.project.watchmate.Dto.MarkEpisodeRequest;
import com.project.watchmate.Dto.MarkSeasonRequest;
import com.project.watchmate.Dto.NextEpisodeAiringDTO;
import com.project.watchmate.Dto.ReviewResponseDTO;
import com.project.watchmate.Dto.ShowDetailsDTO;
import com.project.watchmate.Dto.ShowProgressDTO;
import com.project.watchmate.Dto.ShowSeasonsDetailsDTO;
import com.project.watchmate.Dto.UpdateShowProgressRequestDTO;
import com.project.watchmate.Dto.UpdateWatchStatusRequestDTO;
import com.project.watchmate.Dto.UserMediaStatusDTO;
import com.project.watchmate.Models.MediaType;
import com.project.watchmate.Models.UserPrincipal;
import com.project.watchmate.Models.Users;
import com.project.watchmate.Services.ReviewService;
import com.project.watchmate.Services.ShowMetadataService;
import com.project.watchmate.Services.ShowProgressService;
import com.project.watchmate.Services.StatusService;

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

    private final StatusService statusService;

    private final ReviewService reviewService;

    private final ShowProgressService showProgressService;

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
    @Operation(summary = "Update show watch status", description = "Updates the authenticated user's watch status for a show.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Watch status updated", content = @Content(schema = @Schema(implementation = UserMediaStatusDTO.class))),
        @ApiResponse(responseCode = "400", description = "Validation failed or watch status is invalid", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "401", description = "Authentication failed", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Show not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
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

    @GetMapping("/{tmdbId}/progress")
    @Operation(summary = "Get show progress", description = "Returns show progress for the authenticated user.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Show progress returned", content = @Content(schema = @Schema(implementation = ShowProgressDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request parameter", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "401", description = "Authentication failed", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Show not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<ShowProgressDTO> getShowProgress(
        @PathVariable @Min(1) Long tmdbId,
        @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        Users user = userPrincipal.getUser();
        return ResponseEntity.ok(showProgressService.getShowProgress(user, tmdbId, MEDIA_TYPE));
    }

    @PutMapping("/{tmdbId}/progress")
    @Operation(summary = "Update show progress", description = "Updates summary progress for a show.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Show progress updated", content = @Content(schema = @Schema(implementation = ShowProgressDTO.class))),
        @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "401", description = "Authentication failed", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Show not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<ShowProgressDTO> updateShowProgress(
        @PathVariable @Min(1) Long tmdbId,
        @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal,
        @Valid @RequestBody UpdateShowProgressRequestDTO request
    ) {
        Users user = userPrincipal.getUser();
        return ResponseEntity.ok(showProgressService.updateShowProgress(user, tmdbId, MEDIA_TYPE, request));
    }

    @PutMapping("/{tmdbId}/episodes/{seasonNumber}/{episodeNumber}")
    @Operation(summary = "Mark episode watched", description = "Marks one show episode watched or unwatched.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Episode watch state updated", content = @Content(schema = @Schema(implementation = ShowProgressDTO.class))),
        @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "401", description = "Authentication failed", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Show or episode not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<ShowProgressDTO> markEpisodeWatched(
        @PathVariable @Min(1) Long tmdbId,
        @PathVariable @Min(0) Integer seasonNumber,
        @PathVariable @Min(1) Integer episodeNumber,
        @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal,
        @Valid @RequestBody MarkEpisodeRequest request
    ) {
        Users user = userPrincipal.getUser();
        return ResponseEntity.ok(showProgressService.markEpisodeWatched(user, tmdbId, MEDIA_TYPE, seasonNumber, episodeNumber, request.watched()));
    }

    @PutMapping("/{tmdbId}/seasons/{seasonNumber}/watched")
    @Operation(summary = "Mark season watched", description = "Marks every episode in a show season watched or unwatched.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Season watch state updated", content = @Content(schema = @Schema(implementation = ShowProgressDTO.class))),
        @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "401", description = "Authentication failed", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Show or season not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<ShowProgressDTO> markSeasonWatched(
        @PathVariable @Min(1) Long tmdbId,
        @PathVariable @Min(0) Integer seasonNumber,
        @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal,
        @Valid @RequestBody MarkSeasonRequest request
    ) {
        Users user = userPrincipal.getUser();
        return ResponseEntity.ok(showProgressService.markSeasonWatched(user, tmdbId, seasonNumber, request.watched()));
    }

    @GetMapping("/{tmdbId}/episodes/watched")
    @Operation(summary = "List watched show episodes", description = "Returns watched episodes for a show.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Watched episodes returned", content = @Content(array = @ArraySchema(schema = @Schema(implementation = EpisodeProgressDTO.class)))),
        @ApiResponse(responseCode = "400", description = "Invalid request parameter", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "401", description = "Authentication failed", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Show not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<List<EpisodeProgressDTO>> getWatchedEpisodes(
        @PathVariable @Min(1) Long tmdbId,
        @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        Users user = userPrincipal.getUser();
        return ResponseEntity.ok(showProgressService.getWatchedEpisodes(user, tmdbId, MEDIA_TYPE));
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
    @Operation(summary = "Get public show season episodes", description = "Returns public TMDB episode details for one requested season.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Public season episode details returned", content = @Content(schema = @Schema(implementation = ShowSeasonsDetailsDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request parameter", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Show or season not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<ShowSeasonsDetailsDTO> getShowSeasonDetails(
        @PathVariable @Min(1) Long tmdbId,
        @PathVariable @Min(0) Integer seasonNumber
    ) {
        return ResponseEntity.ok(showMetadataService.getShowSeasonDetails(tmdbId, seasonNumber, MEDIA_TYPE));
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
        @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal,
        @PathVariable @Min(1) Long tmdbId
    ) {
        Users user = userPrincipal.getUser();
        List<ReviewResponseDTO> reviewResponses = reviewService.getReviews(user, tmdbId, MEDIA_TYPE);
        return ResponseEntity.ok(reviewResponses);
    }
}
