package com.project.watchmate.Dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "RefreshTokenRequest", description = "Refresh token payload used to obtain a new token pair.")
public class RefreshTokenRequestDTO {

    @NotBlank(message = "Refresh token is required")
    @Schema(description = "Previously issued refresh token.")
    private String refreshToken;
    
}
