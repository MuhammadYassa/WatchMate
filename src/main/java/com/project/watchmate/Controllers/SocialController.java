package com.project.watchmate.Controllers;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

import com.project.watchmate.Dto.ApiError;
import com.project.watchmate.Dto.FollowListUserDetailsDTO;
import com.project.watchmate.Dto.FollowRequestDTO;
import com.project.watchmate.Dto.FollowRequestResponseDTO;
import com.project.watchmate.Dto.FollowStatusDTO;
import com.project.watchmate.Dto.UserProfileDTO;
import com.project.watchmate.Models.FollowRequestStatuses;
import com.project.watchmate.Models.UserPrincipal;
import com.project.watchmate.Models.Users;
import com.project.watchmate.Services.SocialService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;



@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/social")
@Validated
@Tag(name = "Social", description = "Authenticated social graph and profile endpoints.")
@SecurityRequirement(name = "bearerAuth")
public class SocialController {

    private static final int MAX_SIZE = 50;

    private final SocialService socialService;
    
    @PostMapping("/follow/{userId}")
    @Operation(summary = "Follow user", description = "Follows a public user or creates a follow request for a private user.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Follow status updated", content = @Content(schema = @Schema(implementation = FollowStatusDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "401", description = "Authentication failed", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "Follow action is blocked", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Target user not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "409", description = "Already following or follow request already pending", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<FollowStatusDTO> followUser(@PathVariable @Min(1) Long userId, @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal) {
        Users user = userPrincipal.getUser();
        FollowStatusDTO response = socialService.followUser(userId, user);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/unfollow/{userId}")
    @Operation(summary = "Unfollow user", description = "Removes an existing follow relationship.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Follow status updated", content = @Content(schema = @Schema(implementation = FollowStatusDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "401", description = "Authentication failed", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "Unfollow action is blocked", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Target user not found or not currently followed", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<FollowStatusDTO> unfollowUser(@PathVariable @Min(1) Long userId, @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal){
        Users user = userPrincipal.getUser();
        FollowStatusDTO response = socialService.unfollowUser(userId, user);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/follow-request/{requestId}/accept")
    @Operation(summary = "Accept follow request", description = "Accepts a pending follow request addressed to the authenticated user.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Follow request accepted", content = @Content(schema = @Schema(implementation = FollowRequestResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid path parameter", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "401", description = "Authentication failed", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "Authenticated user cannot act on this request", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Follow request not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<FollowRequestResponseDTO> acceptFollowRequest(@PathVariable @Min(1) Long requestId, @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal) {
        Users user = userPrincipal.getUser();
        FollowRequestResponseDTO response = socialService.respondToFollowRequest(requestId, user, FollowRequestStatuses.ACCEPTED);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/follow-request/{requestId}/reject")
    @Operation(summary = "Reject follow request", description = "Rejects a pending follow request addressed to the authenticated user.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Follow request rejected", content = @Content(schema = @Schema(implementation = FollowRequestResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid path parameter", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "401", description = "Authentication failed", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "Authenticated user cannot act on this request", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Follow request not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<FollowRequestResponseDTO> rejectFollowRequest(@PathVariable @Min(1) Long requestId, @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal) {
        Users user = userPrincipal.getUser();
        FollowRequestResponseDTO response = socialService.respondToFollowRequest(requestId, user, FollowRequestStatuses.REJECTED);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/follow-request/{requestId}/cancel")
    @Operation(summary = "Cancel follow request", description = "Cancels a pending follow request created by the authenticated user.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Follow request canceled", content = @Content(schema = @Schema(implementation = FollowRequestResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid path parameter", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "401", description = "Authentication failed", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "Authenticated user cannot act on this request", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Follow request not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<FollowRequestResponseDTO> cancelFollowRequest(@PathVariable @Min(1) Long requestId, @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal) {
        Users user = userPrincipal.getUser();
        FollowRequestResponseDTO response = socialService.respondToFollowRequest(requestId, user, FollowRequestStatuses.CANCELED);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/follow-requests/received")
    @Operation(summary = "List received follow requests", description = "Returns pending follow requests addressed to the authenticated user.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Follow requests returned"),
        @ApiResponse(responseCode = "400", description = "Invalid pagination parameter", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "401", description = "Authentication failed", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<Page<FollowRequestDTO>> receivedRequests(
        @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "10") @Min(1) @Max(MAX_SIZE) int size
    ) {
        Users user = userPrincipal.getUser();
        Page<FollowRequestDTO> response = socialService.getReceivedRequests(user, page, size);
        return ResponseEntity.ok(response);
    }
    
    
    @GetMapping("/follow-status/{userId}")
    @Operation(summary = "Get follow status", description = "Returns the relationship status between the authenticated user and another user.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Follow status returned", content = @Content(schema = @Schema(implementation = FollowStatusDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid path parameter", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "401", description = "Authentication failed", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Target user not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<FollowStatusDTO> followStatus(@PathVariable @Min(1) Long userId, @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal) {
        Users user = userPrincipal.getUser();
        FollowStatusDTO response = socialService.getFollowStatus(userId, user);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/block/{userId}")
    @Operation(summary = "Block or unblock user", description = "Blocks a user, or removes an existing block if the user is already blocked.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Block status updated", content = @Content(schema = @Schema(implementation = FollowStatusDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "401", description = "Authentication failed", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Target user not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<FollowStatusDTO> blockUser(@PathVariable @Min(1) Long userId, @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal) {
        Users user = userPrincipal.getUser();
        FollowStatusDTO response = socialService.blockUser(userId, user);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/followers-list")
    @Operation(summary = "List followers", description = "Returns a paginated list of followers for the authenticated user.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Followers returned"),
        @ApiResponse(responseCode = "400", description = "Invalid pagination parameter", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "401", description = "Authentication failed", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<Page<FollowListUserDetailsDTO>> getFollowersList(
        @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "10") @Min(1) @Max(MAX_SIZE) int size
    ) {
        Users user = userPrincipal.getUser();
        Page<FollowListUserDetailsDTO> response = socialService.getFollowersList(user, page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/following-list")
    @Operation(summary = "List following", description = "Returns a paginated list of users followed by the authenticated user.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Following list returned"),
        @ApiResponse(responseCode = "400", description = "Invalid pagination parameter", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "401", description = "Authentication failed", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<Page<FollowListUserDetailsDTO>> getFollowingList(
        @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "10") @Min(1) @Max(MAX_SIZE) int size
    ) {
        Users user = userPrincipal.getUser();
        Page<FollowListUserDetailsDTO> response = socialService.getFollowingList(user, page, size);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/user-profile/{userId}")
    @Operation(summary = "Get user profile", description = "Returns the profile view available to the authenticated user for the requested user.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User profile returned", content = @Content(schema = @Schema(implementation = UserProfileDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid path parameter", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "401", description = "Authentication failed", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Target user not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<UserProfileDTO> getUserProfile(@Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal, @PathVariable @Min(1) Long userId) {
        Users user = userPrincipal.getUser();
        UserProfileDTO response = socialService.getUserProfile(userId, user);
        return ResponseEntity.ok(response);
    }

}
