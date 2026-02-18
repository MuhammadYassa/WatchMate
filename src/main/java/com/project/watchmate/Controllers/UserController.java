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
import com.project.watchmate.Services.EmailVerificationTokenService;
import com.project.watchmate.Services.UserService;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;


@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Validated
public class UserController {

    private final UserService userService;

    private final EmailVerificationTokenService emailVerificationService;
    
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequestDTO registerRequest) {
        userService.register(registerRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body("User Registered Successfully, Verify email to login");
    }

    @GetMapping("/verify")
    public ResponseEntity<?> verifyUser(@RequestParam("token") @NotBlank String token){
        emailVerificationService.verifyToken(token);
        return ResponseEntity.ok("Email verified successfully");
    }

    @PostMapping("/verify/resend")
    public ResponseEntity<String> resendVerificationEmail(@RequestParam @NotBlank @Email String email) {
        emailVerificationService.resendVerificationEmail(email);
        return ResponseEntity.ok("Verification email resent");
    }
    

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@Valid @RequestBody LoginRequestDTO loginRequest){
        return ResponseEntity.ok(userService.verify(loginRequest));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken (@Valid @RequestBody RefreshTokenRequestDTO request) {
        return ResponseEntity.ok(userService.refreshToken(request.getRefreshToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout (@Valid @RequestBody LogoutRequestDTO request) {
        userService.logout(request.getRefreshToken());
        return ResponseEntity.ok("Logged out successfully");
    }
}
