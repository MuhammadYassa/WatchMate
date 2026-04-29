package com.project.watchmate.Dto;

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
@Schema(name = "SearchListUserDetails", description = "User summary returned in username search results.")
public class SearchListUserDetailsDTO {

    @Schema(description = "Username of the matched user.", example = "cinephile42")
    private String username;

    @Schema(description = "Whether the authenticated user already follows this account.", example = "true")
    private Boolean isFollowing;

    @Schema(description = "Whether the matched account belongs to the authenticated user.", example = "false")
    private Boolean isSelf;

    @Schema(description = "Privacy setting of the matched user.")
    private PrivacyStatuses privacyStatus;
}
