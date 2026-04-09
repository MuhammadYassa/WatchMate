package com.project.watchmate.Dto;

import com.project.watchmate.Models.FollowRequestStatuses;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(name = "FollowRequestResponse", description = "Result of acting on a follow request.")
public class FollowRequestResponseDTO {

    @Schema(description = "Identifier of the follow request.", example = "12")
    private Long requestId;
    
    @Schema(description = "New status after the action is applied.")
    private FollowRequestStatuses newStatus;
    
}
