package com.project.watchmate.Dto;

import java.time.LocalDateTime;

import com.project.watchmate.Models.FollowRequestStatuses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FollowRequestDTO {

    private Long requestId;
    
    private Long requesterUserId;

    private Long targetUserId;

    private String requesterUsername; 
    
    private String targetUsername;

    private LocalDateTime requestedAt;

    private FollowRequestStatuses status;
    
}
