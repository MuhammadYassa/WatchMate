package com.project.watchmate.Controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.project.watchmate.Dto.LoginRequestDTO;
import com.project.watchmate.Dto.LogoutRequestDTO;
import com.project.watchmate.Dto.RefreshTokenRequestDTO;
import com.project.watchmate.Dto.RegisterRequestDTO;
import com.project.watchmate.Dto.ApiError;
import com.project.watchmate.Dto.LoginResponseDTO;
import com.project.watchmate.Services.EmailVerificationTokenService;
import com.project.watchmate.Services.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;


@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Validated
@Tag(name = "Authentication", description = "Authentication and account verification endpoints.")
public class UserController {

    private final UserService userService;

    private final EmailVerificationTokenService emailVerificationService;
    
    @PostMapping("/register")
    @Operation(summary = "Register account", description = "Creates a new account and sends a verification email.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "User registered"),
        @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "409", description = "Username or email already in use", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequestDTO registerRequest) {
        userService.register(registerRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body("User Registered Successfully, Verify email to login");
    }

    @GetMapping("/verify")
    @Operation(summary = "Verify email address", description = "Processes an email verification token.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Verification request processed"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<?> verifyUser(@RequestParam("token") @NotBlank String token){
        emailVerificationService.verifyToken(token);
        return ResponseEntity.ok("Email verified successfully");
    }

    @PostMapping("/verify/resend")
    @Operation(summary = "Resend verification email", description = "Sends a fresh verification email for an unverified account.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Verification email resent"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "409", description = "Email already verified", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<String> resendVerificationEmail(@RequestParam @NotBlank @Email String email) {
        emailVerificationService.resendVerificationEmail(email);
        return ResponseEntity.ok("Verification email resent");
    }
    

    @PostMapping("/login")
    @Operation(summary = "Authenticate user", description = "Authenticates a user and returns JWT access and refresh tokens.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Authentication successful", content = @Content(schema = @Schema(implementation = LoginResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "401", description = "Authentication failed", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<?> loginUser(@Valid @RequestBody LoginRequestDTO loginRequest){
        return ResponseEntity.ok(userService.verify(loginRequest));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh tokens", description = "Uses a refresh token to issue a new access token and refresh token pair.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Tokens refreshed", content = @Content(schema = @Schema(implementation = LoginResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "401", description = "Invalid refresh token", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<?> refreshToken (@Valid @RequestBody RefreshTokenRequestDTO request) {
        return ResponseEntity.ok(userService.refreshToken(request.getRefreshToken()));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout user", description = "Revokes the supplied refresh token for the authenticated user.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Logout successful"),
        @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "401", description = "Authentication failed", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<?> logout (@Valid @RequestBody LogoutRequestDTO request) {
        userService.logout(request.getRefreshToken());
        return ResponseEntity.ok("Logged out successfully");
    }
}
