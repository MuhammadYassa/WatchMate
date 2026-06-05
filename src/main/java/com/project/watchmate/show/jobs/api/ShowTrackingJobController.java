package com.project.watchmate.show.jobs.api;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.watchmate.common.error.ApiError;
import com.project.watchmate.show.jobs.dto.ShowTrackingJobDTO;
import com.project.watchmate.common.security.auth.UserPrincipal;
import com.project.watchmate.user.domain.Users;
import com.project.watchmate.show.jobs.application.ShowTrackingJobService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/show-tracking-jobs")
@Validated
@RequiredArgsConstructor
@Tag(name = "Show Tracking Jobs", description = "Background job polling endpoints for asynchronous show catalog hydration and tracking backfill.")
public class ShowTrackingJobController {

    private final ShowTrackingJobService showTrackingJobService;

    @GetMapping("/{jobId}")
    @Operation(summary = "Get show tracking job status", description = "Returns the authenticated user's background show tracking job. 200 means the job exists and is visible to the caller, while 404 is returned for missing jobs and jobs owned by another user.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Show tracking job returned", content = @Content(schema = @Schema(implementation = ShowTrackingJobDTO.class))),
        @ApiResponse(responseCode = "401", description = "Authentication failed", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Show tracking job not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<ShowTrackingJobDTO> getJob(
        @PathVariable @Min(1) Long jobId,
        @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        Users user = userPrincipal.getUser();
        return ResponseEntity.ok(showTrackingJobService.getUserJob(user, jobId));
    }
}





