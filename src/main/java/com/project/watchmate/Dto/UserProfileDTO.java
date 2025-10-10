package com.project.watchmate.Dto;

import org.springframework.data.domain.Page;

import com.project.watchmate.Models.FollowStatuses;
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

    private Long followingCount;

    private Page<WatchListDTO> watchlists;

    private Page<ReviewResponseDTO> reviews;

    private Long moviesWatchedCount;

    private Long showsWatchedCount;

    private Page<SearchItemDTO> moviesWatched;

    private Page<SearchItemDTO> showsWatched;

    private FollowStatuses followStatus;

    private PrivacyStatuses privacyStatus;
    
}
