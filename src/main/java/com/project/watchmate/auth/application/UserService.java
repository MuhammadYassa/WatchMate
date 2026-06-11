package com.project.watchmate.auth.application;

import java.util.Objects;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.watchmate.auth.dto.LoginRequestDTO;
import com.project.watchmate.auth.dto.LoginResponseDTO;
import com.project.watchmate.auth.dto.RegisterRequestDTO;
import com.project.watchmate.common.error.EmailException;
import com.project.watchmate.common.error.RegistrationConflictException;
import com.project.watchmate.common.error.UserNotFoundException;
import com.project.watchmate.common.error.UsernameException;
import com.project.watchmate.common.security.auth.UserPrincipal;
import com.project.watchmate.common.security.jwt.JwtService;
import com.project.watchmate.auth.domain.RefreshToken;
import com.project.watchmate.user.domain.Role;
import com.project.watchmate.user.domain.Users;
import com.project.watchmate.user.persistence.UsersRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

    private final UsersRepository userRepo;

    private final AuthenticationManager authManager;

    private final JwtService jwtService;

    private final EmailVerificationTokenService emailService;

    private final RefreshTokenService refreshTokenService;

    private final BCryptPasswordEncoder encoder;

    @Transactional
    public void register(RegisterRequestDTO registerRequest){
        if (userRepo.existsByUsername(registerRequest.getUsername())) {
            log.warn("Registration rejected username already exists username={}", registerRequest.getUsername());
            throw new UsernameException("Username is already taken.");
        }
        if (userRepo.existsByEmail(registerRequest.getEmail())) {
            log.warn("Registration rejected email already in use");
            throw new EmailException("Email is already in use.");
        }

        try {
            Users user = Objects.requireNonNull(Users.builder()
                .username(registerRequest.getUsername())
                .password(encoder.encode(registerRequest.getPassword()))
                .email(registerRequest.getEmail())
                .emailVerified(false)
                .role(Role.USER)
                .build());
            userRepo.saveAndFlush(user);

            emailService.sendVerificationEmail(user.getEmail(), emailService.createToken(user));
            log.info("Registered user username={}", user.getUsername());
        } catch (DataIntegrityViolationException ex){
            log.warn("Registration failed due to database conflict username={}", registerRequest.getUsername(), ex);
            throw mapRegistrationConflict(ex);
        }
    }

    public LoginResponseDTO authenticateAndIssueTokens(LoginRequestDTO loginRequest) {
        Authentication auth = authManager.authenticate(new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

        if (auth.isAuthenticated()){
            Users user = userRepo.findByUsername(loginRequest.getUsername())
                    .orElseThrow(() -> new UserNotFoundException("User not found"));
            String accessToken = jwtService.generateAccessToken(loginRequest.getUsername());
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);
            log.info("Login succeeded username={}", loginRequest.getUsername());

            return LoginResponseDTO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .accessTokenExpiry(jwtService.getAccessTokenExpiry())
                .build();
        }
        log.warn("Authentication manager returned unauthenticated result username={}", loginRequest.getUsername());
        throw new AuthenticationServiceException("Authentication Failed");
    }

    public LoginResponseDTO refreshToken(String refreshTokenString) {
        RefreshToken newRefreshToken = refreshTokenService.rotateRefreshToken(refreshTokenString);
        Users user = newRefreshToken.getUser();

        String newAccessToken = jwtService.generateAccessToken(user.getUsername());
        log.info("Refresh token issued username={}", user.getUsername());

        return LoginResponseDTO.builder()
            .accessToken(newAccessToken)
            .refreshToken(newRefreshToken.getToken())
            .accessTokenExpiry(jwtService.getAccessTokenExpiry())
            .build();
    }

    public void logout(UserPrincipal userPrincipal, String refreshTokenString) {
        Users authenticatedUser = userPrincipal.getUser();
        refreshTokenService.revokeRefreshTokenForUser(refreshTokenString, authenticatedUser);
        log.info("Refresh token revoked username={}", authenticatedUser.getUsername());
    }

    private RuntimeException mapRegistrationConflict(DataIntegrityViolationException ex) {
        String message = collectMessages(ex).toLowerCase();
        if (message.contains("uq_users_username") || message.contains("users.username")) {
            return new UsernameException("Username is already taken.");
        }
        if (message.contains("uq_users_email") || message.contains("users.email")) {
            return new EmailException("Email is already in use.");
        }
        return new RegistrationConflictException("Account already exists.");
    }

    private String collectMessages(Throwable throwable) {
        StringBuilder builder = new StringBuilder();
        Throwable current = throwable;
        while (current != null) {
            if (current.getMessage() != null) {
                builder.append(current.getMessage()).append(' ');
            }
            current = current.getCause();
        }
        return builder.toString();
    }
}



