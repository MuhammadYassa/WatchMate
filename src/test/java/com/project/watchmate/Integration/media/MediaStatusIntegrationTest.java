package com.project.watchmate.Integration.media;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.project.watchmate.Integration.support.AbstractIntegrationTest;
import com.project.watchmate.Models.Media;
import com.project.watchmate.Models.Users;
import com.project.watchmate.Models.WatchStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MediaStatusIntegrationTest extends AbstractIntegrationTest {

	@Test
	void updateWatchStatus_returns200_andPersistsStatus() throws Exception {
		Users user = saveUser("status-user", true);
		Media media = saveMedia(8001L, "Status Movie", com.project.watchmate.Models.MediaType.MOVIE);

		mockMvc.perform(post("/api/v1/media/update")
			.header("Authorization", bearerToken(user))
			.contentType(MediaType.APPLICATION_JSON)
			.content(watchStatusBody(media.getTmdbId(), "MOVIE", "WATCHED")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.tmdbId").value(media.getTmdbId().intValue()))
			.andExpect(jsonPath("$.status").value("WATCHED"));

		assertThat(userMediaStatusRepository.findByUserAndMedia(user, media).orElseThrow().getStatus())
			.isEqualTo(WatchStatus.WATCHED);
	}

	@Test
	void invalidWatchStatus_returns400() throws Exception {
		Users user = saveUser("invalid-status-user", true);
		Media media = saveMedia(8002L, "Invalid Status Movie", com.project.watchmate.Models.MediaType.MOVIE);

		mockMvc.perform(post("/api/v1/media/update")
			.header("Authorization", bearerToken(user))
			.contentType(MediaType.APPLICATION_JSON)
			.content(watchStatusBody(media.getTmdbId(), "MOVIE", "DONE")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_WATCH_STATUS"));

		assertThat(userMediaStatusRepository.findByUserAndMedia(user, media)).isEmpty();
	}

	private String watchStatusBody(Long tmdbId, String type, String status) {
		return """
			{"tmdbId":%d,"type":"%s","status":"%s"}
			""".formatted(tmdbId, type, status);
	}
}
