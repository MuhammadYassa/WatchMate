package com.project.watchmate.Controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.watchmate.Dto.FollowListDTO;
import com.project.watchmate.Dto.FollowStatusDTO;
import com.project.watchmate.Dto.UserProfileDTO;
import com.project.watchmate.Models.Users;
import com.project.watchmate.Models.UserPrincipal;
import com.project.watchmate.Services.SocialService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;





@RestController
@RequiredArgsConstructor
@RequestMapping("/social")
public class SocialController {

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
    public ResponseEntity<FollowListDTO> getFollowersList(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        Users user = userPrincipal.getUser();
        FollowListDTO response = socialService.getFollowersList(user);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/following-list")
    public ResponseEntity<FollowListDTO> getFollowingLIst(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        Users user = userPrincipal.getUser();
        FollowListDTO response = socialService.getFollowingList(user);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/user-profile/{userId}")
    public ResponseEntity<UserProfileDTO> getUserProfile(@AuthenticationPrincipal UserPrincipal userPrincipal, @PathVariable Long userId) {
        Users user = userPrincipal.getUser();
        UserProfileDTO resonse = socialService.getUserProfile(user, userId);
        return ResponseEntity.ok(resonse);
    }
    
    // Implement Follow requesting and accepting.
    
}