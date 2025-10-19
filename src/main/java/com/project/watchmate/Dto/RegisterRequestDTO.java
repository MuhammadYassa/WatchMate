package com.project.watchmate.Dto;

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
public class RegisterRequestDTO {

    @NotBlank(message = "Username required")
    private String username;

    @NotBlank(message = "Password required")
    @Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$",
        message = "Password must be at least 8 characters and contain a letter, number, and symbol"
    )
    private String password;

    @NotBlank(message = "Email required")
    @Email(message = "Email must be Valid")
    private String email;
    
}
