package com.project.watchmate.common.integration.support;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.project.watchmate.media.tmdb.client.TmdbClient;
import com.project.watchmate.auth.dto.LoginRequestDTO;
import com.project.watchmate.media.tmdb.dto.TmdbResponseDTO;
import com.project.watchmate.media.tmdb.dto.TmdbCreditsDTO;
import com.project.watchmate.media.tmdb.dto.TmdbVideosResponseDTO;
import com.project.watchmate.media.tmdb.dto.TmdbWatchProvidersResponseDTO;
import com.project.watchmate.media.catalog.domain.Media;
import com.project.watchmate.user.domain.Users;
import com.project.watchmate.media.catalog.domain.MediaType;
import com.project.watchmate.user.domain.Role;
import com.project.watchmate.auth.persistence.EmailVerificationTokenRepository;
import com.project.watchmate.social.persistence.FollowRequestRepository;
import com.project.watchmate.discovery.persistence.ContentSyncStatusRepository;
import com.project.watchmate.discovery.persistence.CuratedContentRepository;
import com.project.watchmate.media.catalog.persistence.GenreRepository;
import com.project.watchmate.media.catalog.persistence.MediaRepository;
import com.project.watchmate.auth.persistence.RefreshTokenRepository;
import com.project.watchmate.review.persistence.ReviewRepository;
import com.project.watchmate.media.catalog.persistence.ShowEpisodeRepository;
import com.project.watchmate.media.catalog.persistence.ShowSeasonRepository;
import com.project.watchmate.show.jobs.persistence.ShowTrackingJobRepository;
import com.project.watchmate.show.tracking.persistence.UserEpisodeWatchRepository;
import com.project.watchmate.movie.tracking.persistence.UserMediaStatusRepository;
import com.project.watchmate.show.tracking.persistence.UserShowTrackingRepository;
import com.project.watchmate.user.persistence.UsersRepository;
import com.project.watchmate.watchlist.persistence.WatchListRepository;
import com.project.watchmate.common.security.jwt.JwtService;
import com.project.watchmate.show.jobs.application.ShowTrackingJobService;

import tools.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;
import software.amazon.awssdk.services.ses.model.SendEmailResponse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

@AutoConfigureMockMvc
@Import(AbstractIntegrationTest.ExternalClientTestConfig.class)
@SpringBootTest(properties = {
	"spring.jpa.hibernate.ddl-auto=create-drop",
	"spring.jpa.show-sql=false",
	"jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
	"tmdb.api.token=test-token",
	"app.domain=http://localhost",
	"verified.sender=test@example.com",
	"spring.cache.type=none",
	"watchmate.cache.enabled=false",
	"spring.data.redis.host=localhost",
	"spring.data.redis.port=6379",
	"spring.data.redis.password=",
	"management.health.redis.enabled=false",
	"watchmate.discovery.sync.startup-enabled=false",
	"watchmate.show-jobs.poll-delay-ms=3600000",
	"watchmate.rate-limit.enabled=false"
})
public abstract class AbstractIntegrationTest {

	protected static final String TEST_PASSWORD = "Password1!";

	@Autowired
	protected MockMvc mockMvc;

	@Autowired
	protected UsersRepository usersRepository;

	@Autowired
	protected RefreshTokenRepository refreshTokenRepository;

	@Autowired
	protected EmailVerificationTokenRepository emailVerificationTokenRepository;

	@Autowired
	protected WatchListRepository watchListRepository;

	@Autowired
	protected FollowRequestRepository followRequestRepository;

	@Autowired
	protected MediaRepository mediaRepository;

	@Autowired
	protected ReviewRepository reviewRepository;

	@Autowired
	protected ShowSeasonRepository showSeasonRepository;

	@Autowired
	protected ShowEpisodeRepository showEpisodeRepository;

	@Autowired
	protected UserMediaStatusRepository userMediaStatusRepository;

	@Autowired
	protected UserEpisodeWatchRepository userEpisodeWatchRepository;

	@Autowired
	protected UserShowTrackingRepository userShowTrackingRepository;

	@Autowired
	protected ShowTrackingJobRepository showTrackingJobRepository;

	@Autowired
	protected GenreRepository genreRepository;

	@Autowired
	protected CuratedContentRepository curatedContentRepository;

	@Autowired
	protected ContentSyncStatusRepository contentSyncStatusRepository;

	@Autowired
	protected BCryptPasswordEncoder passwordEncoder;

	@Autowired
	protected JwtService jwtService;

	@Autowired
	protected ShowTrackingJobService showTrackingJobService;

	@MockitoBean
	protected SesClient sesClient;

	@Autowired
	protected TmdbClient tmdbClient;

	@Autowired
	protected ObjectMapper objectMapper;

	@Autowired
	protected JdbcTemplate jdbcTemplate;

	@DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", SharedMySQLContainer.INSTANCE::getJdbcUrl);
        registry.add("spring.datasource.username", SharedMySQLContainer.INSTANCE::getUsername);
        registry.add("spring.datasource.password", SharedMySQLContainer.INSTANCE::getPassword);
    }
	
	@BeforeEach
	void cleanDatabase() {
		reset(sesClient);
		reset(tmdbClient);
		when(sesClient.sendEmail(any(SendEmailRequest.class)))
			.thenReturn(SendEmailResponse.builder().messageId("test-message-id").build());
		when(tmdbClient.fetchGenres(anyString())).thenReturn(List.of());
		when(tmdbClient.fetchPopular(anyString())).thenReturn(List.of());
		when(tmdbClient.fetchTrending(anyString())).thenReturn(List.of());
		when(tmdbClient.fetchUpcomingMovies()).thenReturn(List.of());
		when(tmdbClient.fetchAiringToday()).thenReturn(List.of());
		when(tmdbClient.fetchOnTheAir()).thenReturn(List.of());
		when(tmdbClient.fetchCredits(anyLong(), any())).thenReturn(TmdbCreditsDTO.builder().cast(List.of()).build());
		when(tmdbClient.fetchVideos(anyLong(), any())).thenReturn(TmdbVideosResponseDTO.builder().results(List.of()).build());
		when(tmdbClient.fetchWatchProviders(anyLong(), any())).thenReturn(TmdbWatchProvidersResponseDTO.builder().results(java.util.Map.of()).build());
		when(tmdbClient.searchMulti(anyString(), anyInt())).thenReturn(new TmdbResponseDTO(List.of(), 1, 0, 0));
		when(tmdbClient.discoverByGenre(anyString(), anyLong(), anyInt())).thenReturn(new TmdbResponseDTO(List.of(), 1, 0, 0));
		jdbcTemplate.update("delete from user_following");
		jdbcTemplate.update("delete from blocked_users");
		jdbcTemplate.update("delete from user_favorites");
		jdbcTemplate.update("delete from watchlist_items");
		jdbcTemplate.update("delete from media_genres");
		emailVerificationTokenRepository.deleteAll();
		followRequestRepository.deleteAll();
		refreshTokenRepository.deleteAll();
		reviewRepository.deleteAll();
		showTrackingJobRepository.deleteAll();
		userEpisodeWatchRepository.deleteAll();
		userShowTrackingRepository.deleteAll();
		userMediaStatusRepository.deleteAll();
		showEpisodeRepository.deleteAll();
		showSeasonRepository.deleteAll();
		curatedContentRepository.deleteAll();
		contentSyncStatusRepository.deleteAll();
		watchListRepository.deleteAll();
		mediaRepository.deleteAll();
		genreRepository.deleteAll();
		usersRepository.deleteAll();
	}

	protected Users saveUser(String username, boolean emailVerified) {
		return saveUser(username, emailVerified, Role.USER);
	}

	protected Users saveUser(String username, boolean emailVerified, Role role) {
		return usersRepository.save(Users.builder()
			.username(username)
			.password(passwordEncoder.encode(TEST_PASSWORD))
			.email(username + "@example.com")
			.emailVerified(emailVerified)
			.role(role)
			.favorites(new ArrayList<>())
			.build());
	}

	protected String createAccessToken(Users user) {
		return jwtService.generateAccessToken(user.getUsername());
	}

	protected String loginRequestBody(String username) throws Exception {
		return objectMapper.writeValueAsString(new LoginRequestDTO(username, TEST_PASSWORD));
	}

	protected String bearerToken(Users user) {
		return "Bearer " + createAccessToken(user);
	}

	protected Media saveMedia(Long tmdbId, String title, MediaType type) {
		return mediaRepository.save(Media.builder()
			.tmdbId(tmdbId)
			.title(title)
			.overview(title + " overview")
			.posterPath("/" + tmdbId + ".jpg")
			.type(type)
			.rating(8.0)
			.genres(new ArrayList<>())
			.build());
	}

	@TestConfiguration
	public static class ExternalClientTestConfig {
		@Bean
		@Primary
		TmdbClient tmdbClient() {
			return mock(TmdbClient.class, invocation -> switch (invocation.getMethod().getName()) {
				case "fetchGenres", "fetchPopular", "fetchTrending", "fetchUpcomingMovies", "fetchAiringToday", "fetchOnTheAir" -> List.of();
				case "fetchCredits" -> TmdbCreditsDTO.builder().cast(List.of()).build();
				case "fetchVideos" -> TmdbVideosResponseDTO.builder().results(List.of()).build();
				case "fetchWatchProviders" -> TmdbWatchProvidersResponseDTO.builder().results(java.util.Map.of()).build();
				case "searchMulti", "discoverByGenre" -> new TmdbResponseDTO(List.of(), 1, 0, 0);
				default -> null;
			});
		}
	}
}








