package com.project.watchmate.Controllers;

import java.time.LocalDate;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.project.watchmate.Dto.ApiError;
import com.project.watchmate.Dto.CalendarResponseDTO;
import com.project.watchmate.Dto.ContinueWatchingResponseDTO;
import com.project.watchmate.Dto.UpcomingEpisodesResponseDTO;
import com.project.watchmate.Models.UserPrincipal;
import com.project.watchmate.Models.Users;
import com.project.watchmate.Services.DashboardService;

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
import lombok.RequiredArgsConstructor;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/dashboard")
@Tag(name = "Dashboard", description = "Authenticated dashboard endpoints.")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/continue-watching")
    @Operation(summary = "Get continue-watching items", description = "Returns the authenticated user's active in-progress media using local database data only.")
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
