package com.project.watchmate.social.dto;

import com.project.watchmate.user.domain.PrivacyStatuses;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request body for updating profile settings")
public class UpdateProfileRequestDTO {

    @NotNull(message = "privacyStatus is required")
    @Schema(description = "New privacy status", allowableValues = {"PUBLIC", "PRIVATE"})
    private PrivacyStatuses privacyStatus;
}
