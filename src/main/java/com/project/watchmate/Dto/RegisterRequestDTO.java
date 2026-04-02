package com.project.watchmate.Dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "RegisterRequest", description = "Details required to create a WatchMate account.")
public class RegisterRequestDTO {

    @NotBlank(message = "Username required")
    @Schema(description = "Unique username for the new account.", example = "cinephile42")
    private String username;

    @NotBlank(message = "Password required")
    @Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$",
        message = "Password must be at least 8 characters and contain a letter, number, and symbol"
    )
    @Schema(description = "Password with at least 8 characters, including a letter, number, and symbol.", example = "Pa$$word123")
    private String password;

    @NotBlank(message = "Email required")
    @Email(message = "Email must be Valid")
    @Schema(description = "Email address for verification and login flows.", example = "user@example.com")
    private String email;
    
}
