package com.project.watchmate.Dto;

import com.project.watchmate.Models.FollowStatuses;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(name = "FollowStatus", description = "Relationship status between the authenticated user and another user.")
public class FollowStatusDTO {

    @Schema(description = "Current follow relationship state.")
    private FollowStatuses followStatus;

}
