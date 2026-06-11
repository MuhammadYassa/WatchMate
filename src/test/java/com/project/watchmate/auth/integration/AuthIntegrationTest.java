package com.project.watchmate.auth.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.project.watchmate.auth.domain.EmailVerificationToken;
import com.project.watchmate.auth.domain.RefreshToken;
import com.project.watchmate.auth.dto.LoginResponseDTO;
import com.project.watchmate.auth.dto.RefreshTokenRequestDTO;
import com.project.watchmate.auth.dto.RegisterRequestDTO;
import com.project.watchmate.common.integration.support.AbstractIntegrationTest;
import com.project.watchmate.user.domain.Users;

class AuthIntegrationTest extends AbstractIntegrationTest {

    @Test
    void register_createsUserAndVerificationToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(registerRequestBody("new-user", "new-user@example.com")))
            .andExpect(status().isCreated());

        Users user = usersRepository.findByUsername("new-user").orElseThrow();
        List<EmailVerificationToken> tokens = verificationTokensFor(user);

        assertThat(user.getEmail()).isEqualTo("new-user@example.com");
        assertThat(user.isEmailVerified()).isFalse();
        assertThat(passwordEncoder.matches(TEST_PASSWORD, user.getPassword())).isTrue();
        assertThat(tokens).hasSize(1);
        assertThat(tokens.getFirst().getToken()).isNotBlank();
        assertThat(tokens.getFirst().getExpiresAt()).isAfter(LocalDateTime.now());
    }

    @Test
    void register_rejectsDuplicateUsername_orEmail() throws Exception {
        Users existingUser = saveUser("duplicate-user", false);

        mockMvc.perform(post("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(registerRequestBody(existingUser.getUsername(), "unused@example.com")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("DUPLICATE_USERNAME"));

        mockMvc.perform(post("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(registerRequestBody("unused-user", existingUser.getEmail())))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("DUPLICATE_EMAIL"));

        assertThat(usersRepository.count()).isEqualTo(1);
        assertThat(emailVerificationTokenRepository.findAll()).isEmpty();
    }

    @Test
    void verify_withExistingToken_marksUserVerified() throws Exception {
        Users user = saveUser("verify-user", false);
        EmailVerificationToken verificationToken = saveVerificationToken(user, "verify-token");

        mockMvc.perform(get("/api/v1/auth/verify")
            .param("token", verificationToken.getToken()))
            .andExpect(status().isOk());

        Users verifiedUser = usersRepository.findById(user.getId()).orElseThrow();

        assertThat(verifiedUser.isEmailVerified()).isTrue();
        assertThat(emailVerificationTokenRepository.getByToken(verificationToken.getToken())).isEmpty();
    }

    @Test
    void resendVerification_replacesOldToken_forUnverifiedUser() throws Exception {
        Users user = saveUser("resend-user", false);
        EmailVerificationToken oldToken = saveVerificationToken(user, "old-verification-token");

        mockMvc.perform(post("/api/v1/auth/verify/resend")
            .param("email", user.getEmail()))
            .andExpect(status().isOk());

        List<EmailVerificationToken> tokens = verificationTokensFor(user);
        Users unverifiedUser = usersRepository.findById(user.getId()).orElseThrow();

        assertThat(emailVerificationTokenRepository.getByToken(oldToken.getToken())).isEmpty();
        assertThat(tokens).hasSize(1);
        assertThat(tokens.getFirst().getToken()).isNotEqualTo(oldToken.getToken());
        assertThat(tokens.getFirst().getExpiresAt()).isAfter(LocalDateTime.now());
        assertThat(unverifiedUser.isEmailVerified()).isFalse();
    }

    @Test
    void login_twice_createsTwoConcurrentRefreshSessions() throws Exception {
        Users user = saveUser("multi-session-user", true);

        LoginResponseDTO firstLogin = login(user.getUsername());
        LoginResponseDTO secondLogin = login(user.getUsername());

        List<RefreshToken> tokens = refreshTokenRepository.findAll();

        assertThat(firstLogin.getRefreshToken()).isNotEqualTo(secondLogin.getRefreshToken());
        assertThat(tokens).hasSize(2);
        assertThat(tokens).noneMatch(token -> token.getTokenHash().equals(firstLogin.getRefreshToken()));
        assertThat(tokens).noneMatch(token -> token.getTokenHash().equals(secondLogin.getRefreshToken()));
        assertThat(refreshTokenFor(firstLogin.getRefreshToken())).isNotNull();
        assertThat(refreshTokenFor(secondLogin.getRefreshToken())).isNotNull();
        assertThat(tokens).allMatch(token -> !token.isRevoked());
    }

    @Test
    void refresh_rotatesOnlyPresentedToken_andLeavesOtherSessionValid() throws Exception {
        Users user = saveUser("refresh-user", true);

        LoginResponseDTO firstLogin = login(user.getUsername());
        LoginResponseDTO secondLogin = login(user.getUsername());

        LoginResponseDTO refreshResponse = refresh(firstLogin.getRefreshToken());

        RefreshToken oldFirstToken = refreshTokenFor(firstLogin.getRefreshToken());
        RefreshToken secondToken = refreshTokenFor(secondLogin.getRefreshToken());
        RefreshToken replacementToken = refreshTokenFor(refreshResponse.getRefreshToken());

        assertThat(oldFirstToken.isRevoked()).isTrue();
        assertThat(secondToken.isRevoked()).isFalse();
        assertThat(replacementToken.isRevoked()).isFalse();
        assertThat(refreshResponse.getRefreshToken()).isNotEqualTo(firstLogin.getRefreshToken());
        assertThat(replacementToken.getTokenHash()).isEqualTo(hashRefreshToken(refreshResponse.getRefreshToken()));
        assertThat(replacementToken.getTokenHash()).isNotEqualTo(refreshResponse.getRefreshToken());
        assertThat(refreshTokenRepository.findAll()).hasSize(3);

        mockMvc.perform(post("/api/v1/auth/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .content(refreshRequestBody(secondLogin.getRefreshToken())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.refreshToken").isString());
    }

    @Test
    void refresh_withRevokedToken_returnsUnauthorized() throws Exception {
        Users user = saveUser("revoked-refresh-user", true);
        LoginResponseDTO loginResponse = login(user.getUsername());

        refresh(loginResponse.getRefreshToken());

        mockMvc.perform(post("/api/v1/auth/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .content(refreshRequestBody(loginResponse.getRefreshToken())))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void refresh_sameTokenConcurrently_allowsOnlyOneSuccess() throws Exception {
        Users user = saveUser("concurrent-refresh-user", true);
        LoginResponseDTO loginResponse = login(user.getUsername());
        String requestJson = refreshRequestBody(loginResponse.getRefreshToken());
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Integer> first = executor.submit(() -> mockMvc.perform(post("/api/v1/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson))
                .andReturn()
                .getResponse()
                .getStatus());
            Future<Integer> second = executor.submit(() -> mockMvc.perform(post("/api/v1/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson))
                .andReturn()
                .getResponse()
                .getStatus());

            List<Integer> statuses = List.of(first.get(), second.get());
            assertThat(statuses).containsExactlyInAnyOrder(200, 401);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void logout_revokesOnlyPresentedRefreshToken_andAccessTokenRemainsUsableUntilExpiry() throws Exception {
        Users user = saveUser("logout-user", true);
        LoginResponseDTO firstLogin = login(user.getUsername());
        LoginResponseDTO secondLogin = login(user.getUsername());

        mockMvc.perform(post("/api/v1/auth/logout")
            .header("Authorization", "Bearer " + firstLogin.getAccessToken())
            .contentType(MediaType.APPLICATION_JSON)
            .content(refreshRequestBody(firstLogin.getRefreshToken())))
            .andExpect(status().isOk());

        RefreshToken firstToken = refreshTokenFor(firstLogin.getRefreshToken());
        RefreshToken secondToken = refreshTokenFor(secondLogin.getRefreshToken());

        assertThat(firstToken.isRevoked()).isTrue();
        assertThat(secondToken.isRevoked()).isFalse();

        mockMvc.perform(get("/api/v1/watchlists")
            .header("Authorization", "Bearer " + firstLogin.getAccessToken()))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .content(refreshRequestBody(firstLogin.getRefreshToken())))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));

        mockMvc.perform(post("/api/v1/auth/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .content(refreshRequestBody(secondLogin.getRefreshToken())))
            .andExpect(status().isOk());
    }

    @Test
    void logout_withAnotherUsersRefreshToken_returnsForbidden_andLeavesTokenUntouched() throws Exception {
        Users owner = saveUser("token-owner", true);
        Users attacker = saveUser("token-attacker", true);

        LoginResponseDTO ownerLogin = login(owner.getUsername());
        LoginResponseDTO attackerLogin = login(attacker.getUsername());

        mockMvc.perform(post("/api/v1/auth/logout")
            .header("Authorization", "Bearer " + attackerLogin.getAccessToken())
            .contentType(MediaType.APPLICATION_JSON)
            .content(refreshRequestBody(ownerLogin.getRefreshToken())))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED_REFRESH_TOKEN_ACCESS"));

        assertThat(refreshTokenFor(ownerLogin.getRefreshToken()).isRevoked()).isFalse();
    }

    @Test
    void login_returnsTokens_forVerifiedUser() throws Exception {
        Users user = saveUser("verified-user", true);

        String responseBody = mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(loginRequestBody(user.getUsername())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isString())
            .andExpect(jsonPath("$.refreshToken").isString())
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andReturn()
            .getResponse()
            .getContentAsString();

        LoginResponseDTO response = objectMapper.readValue(responseBody, LoginResponseDTO.class);

        assertThat(response.getAccessToken()).isNotBlank();
        assertThat(response.getRefreshToken()).isNotBlank();
        RefreshToken persistedRefreshToken = refreshTokenFor(response.getRefreshToken());
        assertThat(persistedRefreshToken.getTokenHash()).isNotEqualTo(response.getRefreshToken());
        assertThat(persistedRefreshToken.getTokenHash()).isEqualTo(hashRefreshToken(response.getRefreshToken()));
    }

    @Test
    void login_rejectsUnverifiedUser() throws Exception {
        Users user = saveUser("unverified-user", false);

        mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(loginRequestBody(user.getUsername())))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("Authentication failed"))
            .andExpect(jsonPath("$.code").value("AUTH_FAILED"));
    }

    private LoginResponseDTO login(String username) throws Exception {
        String responseBody = mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(loginRequestBody(username)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        return objectMapper.readValue(responseBody, LoginResponseDTO.class);
    }

    private LoginResponseDTO refresh(String refreshToken) throws Exception {
        String refreshResponseBody = mockMvc.perform(post("/api/v1/auth/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .content(refreshRequestBody(refreshToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isString())
            .andExpect(jsonPath("$.refreshToken").isString())
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andReturn()
            .getResponse()
            .getContentAsString();
        return objectMapper.readValue(refreshResponseBody, LoginResponseDTO.class);
    }

    private String registerRequestBody(String username, String email) throws Exception {
        return objectMapper.writeValueAsString(new RegisterRequestDTO(username, TEST_PASSWORD, email));
    }

    private String refreshRequestBody(String refreshToken) throws Exception {
        return objectMapper.writeValueAsString(new RefreshTokenRequestDTO(refreshToken));
    }

    private EmailVerificationToken saveVerificationToken(Users user, String token) {
        return emailVerificationTokenRepository.save(EmailVerificationToken.builder()
            .token(token)
            .user(user)
            .expiresAt(LocalDateTime.now().plusMinutes(15))
            .build());
    }

    private List<EmailVerificationToken> verificationTokensFor(Users user) {
        return emailVerificationTokenRepository.findAll().stream()
            .filter(token -> token.getUser().getId().equals(user.getId()))
            .toList();
    }

    private RefreshToken refreshTokenFor(String rawRefreshToken) {
        return refreshTokenRepository.findByTokenHash(hashRefreshToken(rawRefreshToken)).orElseThrow();
    }

    private String hashRefreshToken(String rawRefreshToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(rawRefreshToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}
