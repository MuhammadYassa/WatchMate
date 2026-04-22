package com.project.watchmate.Integration.security;

import org.junit.jupiter.api.Test;

import com.project.watchmate.Integration.support.AbstractIntegrationTest;
import com.project.watchmate.Models.Users;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class JwtSecurityIntegrationTest extends AbstractIntegrationTest {

	@Test
	void protectedEndpoint_returns401_withoutAuthentication() throws Exception {
		mockMvc.perform(get("/api/v1/watchlists"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.message").value("Authentication failed"))
			.andExpect(jsonPath("$.code").value("AUTH_FAILED"));
	}

	@Test
	void protectedEndpoint_returns200_withValidJwt() throws Exception {
		Users user = saveUser("jwt-user", true);
		String accessToken = createAccessToken(user);

		mockMvc.perform(get("/api/v1/watchlists")
			.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isOk());
	}

	@Test
	void invalidJwt_returns401_onProtectedEndpoint() throws Exception {
		mockMvc.perform(get("/api/v1/watchlists")
			.header("Authorization", "Bearer invalid.jwt.token"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.message").value("Authentication failed"))
			.andExpect(jsonPath("$.code").value("AUTH_FAILED"));
	}
}
