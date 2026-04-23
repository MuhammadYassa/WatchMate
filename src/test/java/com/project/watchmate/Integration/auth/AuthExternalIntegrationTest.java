package com.project.watchmate.Integration.auth;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.project.watchmate.Dto.RegisterRequestDTO;
import com.project.watchmate.Integration.support.AbstractIntegrationTest;

import software.amazon.awssdk.services.ses.model.SendEmailRequest;
import software.amazon.awssdk.services.ses.model.SesException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthExternalIntegrationTest extends AbstractIntegrationTest {

	@Test
	void register_surfacesSesFailure_asMappedError() throws Exception {
		when(sesClient.sendEmail(any(SendEmailRequest.class)))
			.thenThrow(SesException.builder().message("SES unavailable").statusCode(400).build());

		mockMvc.perform(post("/api/v1/auth/register")
			.contentType(MediaType.APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(new RegisterRequestDTO(
				"ses-failure-user",
				TEST_PASSWORD,
				"ses-failure@example.com"))))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("SES_EXCEPTION"));

		assertThat(usersRepository.findByUsername("ses-failure-user")).isPresent();
	}
}
