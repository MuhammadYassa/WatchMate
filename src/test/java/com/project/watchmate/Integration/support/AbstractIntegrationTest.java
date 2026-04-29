package com.project.watchmate.Integration.support;

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

import com.project.watchmate.Clients.TmdbClient;
import com.project.watchmate.Dto.LoginRequestDTO;
import com.project.watchmate.Dto.TmdbResponseDTO;
import com.project.watchmate.Models.Media;
import com.project.watchmate.Models.Users;
import com.project.watchmate.Models.MediaType;
import com.project.watchmate.Models.Role;
import com.project.watchmate.Repositories.EmailVerificationTokenRepository;
import com.project.watchmate.Repositories.FollowRequestRepository;
import com.project.watchmate.Repositories.ContentSyncStatusRepository;
import com.project.watchmate.Repositories.CuratedContentRepository;
import com.project.watchmate.Repositories.GenreRepository;
import com.project.watchmate.Repositories.GenreLookupRepository;
import com.project.watchmate.Repositories.MediaRepository;
import com.project.watchmate.Repositories.PopularMediaRepository;
import com.project.watchmate.Repositories.RefreshTokenRepository;
import com.project.watchmate.Repositories.ReviewRepository;
import com.project.watchmate.Repositories.UserMediaStatusRepository;
import com.project.watchmate.Repositories.UsersRepository;
import com.project.watchmate.Repositories.WatchListRepository;
import com.project.watchmate.Services.JwtService;

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
	"watchmate.discovery.sync.startup-enabled=false"
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
	protected UserMediaStatusRepository userMediaStatusRepository;

	@Autowired
	protected PopularMediaRepository popularMediaRepository;

	@Autowired
	protected GenreRepository genreRepository;

	@Autowired
	protected GenreLookupRepository genreLookupRepository;

	@Autowired
	protected CuratedContentRepository curatedContentRepository;

	@Autowired
	protected ContentSyncStatusRepository contentSyncStatusRepository;

	@Autowired
	protected BCryptPasswordEncoder passwordEncoder;

	@Autowired
	protected JwtService jwtService;

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
		userMediaStatusRepository.deleteAll();
		curatedContentRepository.deleteAll();
		popularMediaRepository.deleteAll();
		contentSyncStatusRepository.deleteAll();
		genreLookupRepository.deleteAll();
		watchListRepository.deleteAll();
		mediaRepository.deleteAll();
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
				case "searchMulti", "discoverByGenre" -> new TmdbResponseDTO(List.of(), 1, 0, 0);
				default -> null;
			});
		}
	}
}
