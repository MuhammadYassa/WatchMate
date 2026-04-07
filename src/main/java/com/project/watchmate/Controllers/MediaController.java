package com.project.watchmate.Controllers;

import org.springframework.web.bind.annotation.RestController;

import com.project.watchmate.Dto.ApiError;
import com.project.watchmate.Dto.MediaDetailsDTO;
import com.project.watchmate.Dto.PaginatedSearchResponseDTO;
import com.project.watchmate.Dto.ReviewResponseDTO;
import com.project.watchmate.Dto.UpdateWatchStatusRequestDTO;
import com.project.watchmate.Dto.UserMediaStatusDTO;
import com.project.watchmate.Dto.PopularMediaResponseDTO;
import com.project.watchmate.Models.UserPrincipal;
import com.project.watchmate.Models.Users;
import com.project.watchmate.Services.MediaService;
import com.project.watchmate.Services.ReviewService;
import com.project.watchmate.Services.SearchService;
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
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;


@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
@Validated
@Tag(name = "Media", description = "Media discovery, details, reviews, and watch status endpoints.")
public class MediaController {

    private final MediaService mediaService;

    private final SearchService searchService;

    private final StatusService statusService;

    private final ReviewService reviewService;

    @GetMapping("/{tmdbId}")
    @Operation(summary = "Get media details", description = "Returns detailed media information for the authenticated user, including favourite and watch status.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Media details returned", content = @Content(schema = @Schema(implementation = MediaDetailsDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request parameter", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "401", description = "Authentication failed", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Media not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<MediaDetailsDTO> getMediaDetails(
        @PathVariable @Min(1) Long tmdbId,
        @RequestParam("type") @NotBlank String typeStr,
        @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        Users user = userPrincipal.getUser();
        MediaDetailsDTO dto = mediaService.getMediaDetails(tmdbId, typeStr, user);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/search")
    @Operation(summary = "Search media", description = "Searches movies and shows by query text.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Search results returned", content = @Content(schema = @Schema(implementation = PaginatedSearchResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid query parameter", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<PaginatedSearchResponseDTO> getSearchResponse(
        @RequestParam("query") @NotBlank String query,
        @RequestParam(value = "page", defaultValue = "1") @Min(1) int page
    ) {
        PaginatedSearchResponseDTO dto = searchService.search(query, page);
        return ResponseEntity.ok(dto);
    }
    
    @GetMapping("/popular")
    @Operation(summary = "List popular media", description = "Returns the currently stored popular media ranking.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Popular media returned", content = @Content(array = @ArraySchema(schema = @Schema(implementation = PopularMediaResponseDTO.class)))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public List<PopularMediaResponseDTO> getPopularMedia() {
                return mediaService.getPopularMedia();
    }

    @PostMapping("/update")
    @Operation(summary = "Update watch status", description = "Updates the authenticated user's watch status for a media item.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Watch status updated", content = @Content(schema = @Schema(implementation = UserMediaStatusDTO.class))),
        @ApiResponse(responseCode = "400", description = "Validation failed or watch status is invalid", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "401", description = "Authentication failed", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Media not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
	public ResponseEntity<UserMediaStatusDTO> updateStatus(
			@Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal,
			@Valid @RequestBody UpdateWatchStatusRequestDTO request) {
		Users user = userPrincipal.getUser();
		UserMediaStatusDTO dto = statusService.updateWatchStatus(user, request);
		return ResponseEntity.ok(dto);
	}

    @GetMapping("/{mediaId}/reviews")
    @Operation(summary = "List media reviews", description = "Returns reviews for a media item for the authenticated user.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Media reviews returned", content = @Content(array = @ArraySchema(schema = @Schema(implementation = ReviewResponseDTO.class)))),
        @ApiResponse(responseCode = "400", description = "Invalid path parameter", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "401", description = "Authentication failed", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Media not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<List<ReviewResponseDTO>> getReviews(@Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal, @PathVariable @Min(1) Long mediaId) {
        Users user = userPrincipal.getUser();
        List<ReviewResponseDTO> reviewResponses = reviewService.getReviews(user, mediaId);
        return ResponseEntity.ok(reviewResponses);
    }
}
