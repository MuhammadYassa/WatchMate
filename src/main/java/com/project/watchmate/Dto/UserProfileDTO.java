package com.project.watchmate.Dto;

import java.util.List;

import com.project.watchmate.Models.PrivacyStatuses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDTO {

    private String username;

    private Long followersCount;

    private Long FollowingCount;

    private List<WatchListDTO> watchlists;

    private List<ReviewResponseDTO> reviews;

    private Long moviesWatchedCount;

    private Long showsWatchedCount;

    private FollowStatusDTO followStatus;

    private PrivacyStatuses privacyStatus;
    
}
