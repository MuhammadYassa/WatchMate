package com.project.watchmate.Dto;

import com.project.watchmate.Models.FollowStatuses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FollowRequestResponseDTO {

    private Long requestId;
    
    private FollowStatuses newStatus;
    
}
