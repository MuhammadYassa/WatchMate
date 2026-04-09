package com.project.watchmate.Dto;

import java.time.LocalDateTime;

import com.project.watchmate.Models.FollowRequestStatuses;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "FollowRequest", description = "Pending follow request information.")
public class FollowRequestDTO {

    @Schema(description = "Identifier of the follow request.", example = "12")
    private Long requestId;
    
    @Schema(description = "Identifier of the user who sent the request.", example = "5")
    private Long requesterUserId;

    @Schema(description = "Identifier of the target user.", example = "9")
    private Long targetUserId;

    @Schema(description = "Username of the user who sent the request.", example = "cinephile42")
    private String requesterUsername; 
    
    @Schema(description = "Username of the target user.", example = "moviebuff")
    private String targetUsername;

    @Schema(description = "Timestamp when the request was created.")
    private LocalDateTime requestedAt;

    @Schema(description = "Current status of the follow request.")
    private FollowRequestStatuses status;
    
}
