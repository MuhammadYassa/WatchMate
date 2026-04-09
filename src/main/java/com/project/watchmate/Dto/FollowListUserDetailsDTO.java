package com.project.watchmate.Dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(name = "FollowListUserDetails", description = "User summary returned in follower and following lists.")
public class FollowListUserDetailsDTO {

    @NotBlank
    @Schema(description = "Username of the listed user.", example = "cinephile42")
    private String username;

}
