package com.project.watchmate.social.dto;

import com.project.watchmate.user.domain.PrivacyStatuses;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response after updating profile settings")
public class UpdateProfileResponseDTO {

    @Schema(description = "User ID")
    private Long userId;

    @Schema(description = "Updated privacy status")
    private PrivacyStatuses privacyStatus;
}
