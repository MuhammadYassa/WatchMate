package com.project.watchmate.common.error.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.project.watchmate.common.integration.support.AbstractIntegrationTest;
import com.project.watchmate.user.domain.Users;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ValidationIntegrationTest extends AbstractIntegrationTest {

	@Test
	void register_withInvalidPayload_returns400_withFieldErrors() throws Exception {
		mockMvc.perform(post("/api/v1/auth/register")
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
				{"username":"","password":"weak","email":"not-an-email"}
				"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
			.andExpect(jsonPath("$.fields[*].field", hasItem("username")))
			.andExpect(jsonPath("$.fields[*].field", hasItem("password")))
			.andExpect(jsonPath("$.fields[*].field", hasItem("email")));
	}

	@Test
	void watchlistPathParam_belowOne_returns400() throws Exception {
		Users user = saveUser("validation-watchlist-user", true);

		mockMvc.perform(delete("/api/v1/watchlists/{id}", 0)
			.header("Authorization", bearerToken(user)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}
}




