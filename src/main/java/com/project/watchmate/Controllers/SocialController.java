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

import com.project.watchmate.Dto.FollowListUserDetailsDTO;
import com.project.watchmate.Dto.FollowRequestDTO;
import com.project.watchmate.Dto.FollowRequestResponseDTO;
import com.project.watchmate.Dto.FollowStatusDTO;
import com.project.watchmate.Dto.UserProfileDTO;
import com.project.watchmate.Models.FollowRequestStatuses;
import com.project.watchmate.Models.UserPrincipal;
import com.project.watchmate.Models.Users;
import com.project.watchmate.Services.SocialService;

import lombok.RequiredArgsConstructor;



@RestController
@RequiredArgsConstructor
@RequestMapping("/social")
public class SocialController {

    private final int MAX_SIZE = 50;

    private final SocialService socialService;
    
    @PostMapping("/follow/{userId}")
    public ResponseEntity<FollowStatusDTO> followUser(@PathVariable Long userId, @AuthenticationPrincipal UserPrincipal userPrincipal) {
        Users user = userPrincipal.getUser();
        FollowStatusDTO response = socialService.followUser(userId, user);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/unfollow/{userId}")
    public ResponseEntity<FollowStatusDTO> unfollowUser(@PathVariable Long userId, @AuthenticationPrincipal UserPrincipal userPrincipal){
        Users user = userPrincipal.getUser();
        FollowStatusDTO response = socialService.unfollowUser(userId, user);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/follow-request/{requestId}/accept")
    public ResponseEntity<FollowRequestResponseDTO> acceptFollowRequest(@PathVariable Long requestId, @AuthenticationPrincipal UserPrincipal userPrincipal) {
        Users user = userPrincipal.getUser();
        FollowRequestResponseDTO response = socialService.respondToFollowRequest(requestId, user, FollowRequestStatuses.ACCEPTED);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/follow-request/{requestId}/reject")
    public ResponseEntity<FollowRequestResponseDTO> rejectFollowRequest(@PathVariable Long requestId, @AuthenticationPrincipal UserPrincipal userPrincipal) {
        Users user = userPrincipal.getUser();
        FollowRequestResponseDTO response = socialService.respondToFollowRequest(requestId, user, FollowRequestStatuses.REJECTED);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/follow-request/{requestId}/cancel")
    public ResponseEntity<FollowRequestResponseDTO> cancelFollowRequest(@PathVariable Long requestId, @AuthenticationPrincipal UserPrincipal userPrincipal) {
        Users user = userPrincipal.getUser();
        FollowRequestResponseDTO response = socialService.respondToFollowRequest(requestId, user, FollowRequestStatuses.CANCELED);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/follow-requests/received")
    public ResponseEntity<Page<FollowRequestDTO>> receivedRequests(@AuthenticationPrincipal UserPrincipal userPrincipal, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        size = Math.min(size, MAX_SIZE);
        size = Math.max(size, 1);
        page = Math.max(page, 0);
        Users user = userPrincipal.getUser();
        Page<FollowRequestDTO> response = socialService.getReceivedRequests(user, page, size);
        return ResponseEntity.ok(response);
    }
    
    
    @GetMapping("/follow-status/{userId}")
    public ResponseEntity<FollowStatusDTO> followStatus(@PathVariable Long userId, @AuthenticationPrincipal UserPrincipal userPrincipal) {
        Users user = userPrincipal.getUser();
        FollowStatusDTO response = socialService.getFollowStatus(userId, user);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/block/{userId}")
    public ResponseEntity<FollowStatusDTO> blockUser(@PathVariable Long userId, @AuthenticationPrincipal UserPrincipal userPrincipal) {
        Users user = userPrincipal.getUser();
        FollowStatusDTO response = socialService.blockUser(userId, user);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/followers-list")
    public ResponseEntity<Page<FollowListUserDetailsDTO>> getFollowersList(@AuthenticationPrincipal UserPrincipal userPrincipal, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        size = Math.min(size, MAX_SIZE);
        size = Math.max(size, 1);
        page = Math.max(page, 0);
        Users user = userPrincipal.getUser();
        Page<FollowListUserDetailsDTO> response = socialService.getFollowersList(user, page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/following-list")
    public ResponseEntity<Page<FollowListUserDetailsDTO>> getFollowingList(@AuthenticationPrincipal UserPrincipal userPrincipal, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        size = Math.min(size, MAX_SIZE);
        size = Math.max(size, 1);
        page = Math.max(page, 0);
        Users user = userPrincipal.getUser();
        Page<FollowListUserDetailsDTO> response = socialService.getFollowingList(user, page, size);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/user-profile/{userId}")
    public ResponseEntity<UserProfileDTO> getUserProfile(@AuthenticationPrincipal UserPrincipal userPrincipal, @PathVariable Long userId) {
        Users user = userPrincipal.getUser();
        UserProfileDTO response = socialService.getUserProfile(userId, user);
        return ResponseEntity.ok(response);
    }

}