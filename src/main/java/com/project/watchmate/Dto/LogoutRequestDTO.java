package com.project.watchmate.Dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "LogoutRequest", description = "Refresh token payload used when logging out.")
public class LogoutRequestDTO {

    @NotBlank(message = "Refresh Token is required")
    @Schema(description = "Refresh token to revoke.")
    private String refreshToken;

}
