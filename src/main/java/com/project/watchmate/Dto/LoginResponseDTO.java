package com.project.watchmate.Dto;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(name = "LoginResponse", description = "Token payload returned after successful authentication or refresh.")
public class LoginResponseDTO {

    @Schema(description = "Short-lived JWT used to authorize API requests.")
    private String accessToken;

    @Schema(description = "Refresh token used to obtain a new access token.")
    private String refreshToken;

    @Schema(description = "Timestamp when the access token expires.")
    private LocalDateTime accessTokenExpiry;

    @Builder.Default
    @Schema(description = "Authentication scheme for the access token.", example = "Bearer")
    private String tokenType = "Bearer";
}
