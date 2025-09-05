package com.project.watchmate.Dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponseDTO {

    private String accessToken;

    private String refreshToken;

    private LocalDateTime accessTokenExpiry;

    @Builder.Default
    private String tokenType = "Bearer";
}
