package com.project.watchmate.Controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.project.watchmate.Dto.LoginRequestDTO;
import com.project.watchmate.Dto.LoginResponseDTO;
import com.project.watchmate.Dto.LogoutRequestDTO;
import com.project.watchmate.Dto.RefreshTokenRequestDTO;
import com.project.watchmate.Dto.RegisterRequestDTO;
import com.project.watchmate.Services.EmailVerificationTokenService;
import com.project.watchmate.Services.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;


@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    private final EmailVerificationTokenService emailVerificationService;
    
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequestDTO registerRequest, BindingResult bindingResult) {
         if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(bindingResult.getAllErrors());
        }
        userService.register(registerRequest);
        return ResponseEntity.ok("User Registered Successfully, Verify email to login");
    }

    @GetMapping("/verify")
    public ResponseEntity<?> verifyUser(@RequestParam("token") String token){
        boolean valid = emailVerificationService.verifyToken(token);

        if (valid) {
            return ResponseEntity.ok("Email verified successfully!");
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid or expired token");
        }
    }

    @PostMapping("/verify/resend")
    public ResponseEntity<String> resendVerificationEmail(@RequestParam String email) {
        try{
            emailVerificationService.resendVerificationEmail(email);
            return ResponseEntity.ok("Verification email resent");
        } catch (IllegalArgumentException e){
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@Valid @RequestBody LoginRequestDTO loginRequest, BindingResult bindingResult){
         if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(bindingResult.getAllErrors());
        }
        
        try {
            LoginResponseDTO response = userService.verify(loginRequest);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken (@Valid @RequestBody RefreshTokenRequestDTO request, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(bindingResult.getAllErrors());
        }

        try {
            LoginResponseDTO response = userService.refreshToken(request.getRefreshToken());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token refresh failed: " + e.getMessage());
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout (@Valid @RequestBody LogoutRequestDTO request, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(bindingResult.getAllErrors());
        }

        try {
            userService.logout(request.getRefreshToken());
            return ResponseEntity.ok("Logged out successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Logout failed: " + e.getMessage());
        }
    }
}
