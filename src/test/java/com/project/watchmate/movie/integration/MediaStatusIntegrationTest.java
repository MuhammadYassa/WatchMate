package com.project.watchmate.movie.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.project.watchmate.common.integration.support.AbstractIntegrationTest;
import com.project.watchmate.media.catalog.domain.Media;
import com.project.watchmate.user.domain.Users;
import com.project.watchmate.media.catalog.domain.WatchStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MediaStatusIntegrationTest extends AbstractIntegrationTest {

	@Test
	void updateWatchStatus_returns200_andPersistsStatus() throws Exception {
		Users user = saveUser("status-user", true);
		Media media = saveMedia(8001L, "Status Movie", com.project.watchmate.media.catalog.domain.MediaType.MOVIE);

		mockMvc.perform(put("/api/v1/movies/{tmdbId}/status", media.getTmdbId())
			.header("Authorization", bearerToken(user))
			.contentType(MediaType.APPLICATION_JSON)
			.content(watchStatusBody("WATCHED")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.tmdbId").value(media.getTmdbId().intValue()))
			.andExpect(jsonPath("$.status").value("WATCHED"));

		assertThat(userMediaStatusRepository.findByUserAndMedia(user, media).orElseThrow().getStatus())
			.isEqualTo(WatchStatus.WATCHED);
	}

	@Test
	void updateWatchStatus_whenSetToNone_deletesExistingRow() throws Exception {
		Users user = saveUser("status-none-user", true);
		Media media = saveMedia(8004L, "Status None Movie", com.project.watchmate.media.catalog.domain.MediaType.MOVIE);
		userMediaStatusRepository.save(com.project.watchmate.movie.tracking.domain.UserMediaStatus.builder()
			.user(user)
			.media(media)
			.status(WatchStatus.WATCHING)
			.build());

		mockMvc.perform(put("/api/v1/movies/{tmdbId}/status", media.getTmdbId())
			.header("Authorization", bearerToken(user))
			.contentType(MediaType.APPLICATION_JSON)
			.content(watchStatusBody("NONE")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.tmdbId").value(media.getTmdbId().intValue()))
			.andExpect(jsonPath("$.status").value("NONE"));

		assertThat(userMediaStatusRepository.findByUserAndMedia(user, media)).isEmpty();
	}

	@Test
	void updateWatchStatus_whenSetToNoneWithoutExistingRow_isIdempotent() throws Exception {
		Users user = saveUser("status-none-idempotent-user", true);
		Media media = saveMedia(8005L, "Status None Idempotent Movie", com.project.watchmate.media.catalog.domain.MediaType.MOVIE);

		mockMvc.perform(put("/api/v1/movies/{tmdbId}/status", media.getTmdbId())
			.header("Authorization", bearerToken(user))
			.contentType(MediaType.APPLICATION_JSON)
			.content(watchStatusBody("NONE")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.tmdbId").value(media.getTmdbId().intValue()))
			.andExpect(jsonPath("$.status").value("NONE"));

		assertThat(userMediaStatusRepository.findByUserAndMedia(user, media)).isEmpty();
	}

	@Test
	void updateWatchStatus_whenSwitchingFromNoneToRealStatus_createsRow() throws Exception {
		Users user = saveUser("status-none-to-real-user", true);
		Media media = saveMedia(8006L, "Status None To Real Movie", com.project.watchmate.media.catalog.domain.MediaType.MOVIE);

		mockMvc.perform(put("/api/v1/movies/{tmdbId}/status", media.getTmdbId())
			.header("Authorization", bearerToken(user))
			.contentType(MediaType.APPLICATION_JSON)
			.content(watchStatusBody("NONE")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("NONE"));

		mockMvc.perform(put("/api/v1/movies/{tmdbId}/status", media.getTmdbId())
			.header("Authorization", bearerToken(user))
			.contentType(MediaType.APPLICATION_JSON)
			.content(watchStatusBody("WATCHED")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.tmdbId").value(media.getTmdbId().intValue()))
			.andExpect(jsonPath("$.status").value("WATCHED"));

		assertThat(userMediaStatusRepository.findByUserAndMedia(user, media).orElseThrow().getStatus())
			.isEqualTo(WatchStatus.WATCHED);
	}

	@Test
	void invalidWatchStatus_returns400() throws Exception {
		Users user = saveUser("invalid-status-user", true);
		Media media = saveMedia(8002L, "Invalid Status Movie", com.project.watchmate.media.catalog.domain.MediaType.MOVIE);

		mockMvc.perform(put("/api/v1/movies/{tmdbId}/status", media.getTmdbId())
			.header("Authorization", bearerToken(user))
			.contentType(MediaType.APPLICATION_JSON)
			.content(watchStatusBody("DONE")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_WATCH_STATUS"));

		assertThat(userMediaStatusRepository.findByUserAndMedia(user, media)).isEmpty();
	}

	@Test
	void movieStatus_upToDate_returns400() throws Exception {
		Users user = saveUser("movie-up-to-date-user", true);
		Media media = saveMedia(8003L, "Movie", com.project.watchmate.media.catalog.domain.MediaType.MOVIE);

		mockMvc.perform(put("/api/v1/movies/{tmdbId}/status", media.getTmdbId())
			.header("Authorization", bearerToken(user))
			.contentType(MediaType.APPLICATION_JSON)
			.content(watchStatusBody("UP_TO_DATE")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_WATCH_STATUS"));

		assertThat(userMediaStatusRepository.findByUserAndMedia(user, media)).isEmpty();
	}

	private String watchStatusBody(String status) {
		return """
			{"status":"%s"}
			""".formatted(status);
	}
}





