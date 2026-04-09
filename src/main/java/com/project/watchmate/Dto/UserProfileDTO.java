package com.project.watchmate.Dto;

import org.springframework.data.domain.Page;

import com.project.watchmate.Models.FollowStatuses;
import com.project.watchmate.Models.PrivacyStatuses;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "UserProfile", description = "Profile data visible to the authenticated user.")
public class UserProfileDTO {

    @Schema(description = "Username of the profile owner.", example = "cinephile42")
    private String username;

    @Schema(description = "Number of followers visible to the requester.", example = "124")
    private Long followersCount;

    @Schema(description = "Number of followed users visible to the requester.", example = "87")
    private Long followingCount;

    @Schema(description = "Visible watchlists for the profile owner.")
    private Page<WatchListDTO> watchlists;

    @Schema(description = "Visible reviews for the profile owner.")
    private Page<ReviewResponseDTO> reviews;

    @Schema(description = "Count of watched movies visible to the requester.", example = "42")
    private Long moviesWatchedCount;

    @Schema(description = "Count of watched shows visible to the requester.", example = "15")
    private Long showsWatchedCount;

    @Schema(description = "Visible watched movies for the profile owner.")
    private Page<SearchItemDTO> moviesWatched;

    @Schema(description = "Visible watched shows for the profile owner.")
    private Page<SearchItemDTO> showsWatched;

    @Schema(description = "Relationship status between the requester and the profile owner.")
    private FollowStatuses followStatus;

    @Schema(description = "Privacy setting of the profile owner.")
    private PrivacyStatuses privacyStatus;
    
}
