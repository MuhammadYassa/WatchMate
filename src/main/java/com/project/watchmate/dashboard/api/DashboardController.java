package com.project.watchmate.dashboard.api;

import java.time.LocalDate;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.project.watchmate.common.error.ApiError;
import com.project.watchmate.dashboard.dto.CalendarResponseDTO;
import com.project.watchmate.dashboard.dto.ContinueWatchingResponseDTO;
import com.project.watchmate.dashboard.dto.ToWatchItemDTO;
import com.project.watchmate.dashboard.dto.UpcomingEpisodesResponseDTO;
import com.project.watchmate.common.security.auth.UserPrincipal;
import com.project.watchmate.user.domain.Users;
import com.project.watchmate.dashboard.application.DashboardService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import lombok.RequiredArgsConstructor;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/dashboard")
@Tag(name = "Dashboard", description = "Authenticated dashboard endpoints.")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/continue-watching")
    @Operation(
        summary = "Get continue-watching items",
        description = "Returns the authenticated user's active in-progress media (both movies and shows) sorted by most-recent "
            + "activity descending. Movie rows carry watchStatus=WATCHING and no season/episode fields. "
            + "Show rows include resumeSeasonNumber, resumeEpisodeNumber, and nextSeason/Episode fields when locally available. "
            + "Uses local database data only; no live TMDB calls."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Continue-watching items returned", content = @Content(schema = @Schema(implementation = ContinueWatchingResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request parameter", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "401", description = "Authentication failed", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<ContinueWatchingResponseDTO> getContinueWatching(
        @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal,
        @RequestParam(defaultValue = "20") @Min(1) @Max(50) int limit
    ) {
        Users user = userPrincipal.getUser();
        return ResponseEntity.ok(dashboardService.getContinueWatching(user, limit));
    }

    @GetMapping("/upcoming-episodes")
    @Operation(summary = "Get upcoming tracked episodes", description = "Returns locally cached upcoming episode snapshots for the authenticated user's tracked shows.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Upcoming episode items returned", content = @Content(schema = @Schema(implementation = UpcomingEpisodesResponseDTO.class))),
        @ApiResponse(responseCode = "401", description = "Authentication failed", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<UpcomingEpisodesResponseDTO> getUpcomingEpisodes(
        @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        Users user = userPrincipal.getUser();
        return ResponseEntity.ok(dashboardService.getUpcomingEpisodesForUser(user));
    }

    @GetMapping("/to-watch")
    @Operation(
        summary = "Get to-watch items",
        description = "Returns a paginated list of all media the authenticated user has marked as TO_WATCH. " +
            "Movies and shows are combined and sorted by most-recently updated first. " +
            "Use the 'type' parameter to filter by MOVIE, SHOW, or ALL (default)."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "To-watch items returned (paginated)"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameter", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "401", description = "Authentication failed", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<Page<ToWatchItemDTO>> getToWatchItems(
        @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size,
        @RequestParam(defaultValue = "ALL") String type
    ) {
        Users user = userPrincipal.getUser();
        return ResponseEntity.ok(dashboardService.getToWatchItems(user, page, size, type));
    }

    @GetMapping("/calendar")
    @Operation(summary = "Get tracked show calendar items", description = "Returns local next-episode snapshots for tracked shows within the requested date range.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Calendar items returned", content = @Content(schema = @Schema(implementation = CalendarResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request parameter", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "401", description = "Authentication failed", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<CalendarResponseDTO> getCalendar(
        @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        Users user = userPrincipal.getUser();
        return ResponseEntity.ok(dashboardService.getCalendarForUser(user, from, to));
    }
}





