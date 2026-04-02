package com.project.watchmate.Dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "LoginRequest", description = "Credentials used to authenticate a WatchMate user.")
public class LoginRequestDTO {

    @NotBlank
    @Schema(description = "Username used to sign in.", example = "cinephile42")
    private String username;

    @NotBlank
    @Schema(description = "Account password.", example = "Pa$$word123")
    private String password;

}
