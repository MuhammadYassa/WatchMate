package com.project.watchmate.Integration.auth;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.project.watchmate.Dto.LoginResponseDTO;
import com.project.watchmate.Dto.RefreshTokenRequestDTO;
import com.project.watchmate.Dto.RegisterRequestDTO;
import com.project.watchmate.Integration.support.AbstractIntegrationTest;
import com.project.watchmate.Models.EmailVerificationToken;
import com.project.watchmate.Models.RefreshToken;
import com.project.watchmate.Models.Users;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
	void refresh_withValidToken_returnsNewTokenPair() throws Exception {
		Users user = saveUser("refresh-user", true);
		String loginResponseBody = mockMvc.perform(post("/api/v1/auth/login")
			.contentType(MediaType.APPLICATION_JSON)
			.content(loginRequestBody(user.getUsername())))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString();
		LoginResponseDTO loginResponse = objectMapper.readValue(loginResponseBody, LoginResponseDTO.class);

		String refreshResponseBody = mockMvc.perform(post("/api/v1/auth/refresh")
			.contentType(MediaType.APPLICATION_JSON)
			.content(refreshRequestBody(loginResponse.getRefreshToken())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.accessToken").isString())
			.andExpect(jsonPath("$.refreshToken").isString())
			.andExpect(jsonPath("$.tokenType").value("Bearer"))
			.andReturn()
			.getResponse()
			.getContentAsString();

		LoginResponseDTO refreshResponse = objectMapper.readValue(refreshResponseBody, LoginResponseDTO.class);
		List<RefreshToken> tokens = refreshTokenRepository.findAll();

		assertThat(refreshResponse.getAccessToken()).isNotBlank();
		assertThat(refreshResponse.getRefreshToken()).isNotBlank();
		assertThat(refreshResponse.getRefreshToken()).isNotEqualTo(loginResponse.getRefreshToken());
		assertThat(refreshTokenRepository.findByToken(loginResponse.getRefreshToken())).isEmpty();
		assertThat(refreshTokenRepository.findByToken(refreshResponse.getRefreshToken())).isPresent();
		assertThat(tokens).hasSize(1);
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
		assertThat(refreshTokenRepository.findByToken(response.getRefreshToken())).isPresent();
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

}
