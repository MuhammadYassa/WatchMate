package com.project.watchmate.Dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogoutRequestDTO {

    @NotBlank(message = "Refresh Token is required")
    private String refreshToken;

}
