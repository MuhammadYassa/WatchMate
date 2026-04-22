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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.project.watchmate.Clients.TmdbClient;
import com.project.watchmate.Dto.LoginRequestDTO;
import com.project.watchmate.Dto.TmdbGenreDTO;
import com.project.watchmate.Dto.TmdbMovieDTO;
import com.project.watchmate.Dto.TmdbResponseDTO;
import com.project.watchmate.Models.Users;
import com.project.watchmate.Models.MediaType;
import com.project.watchmate.Repositories.EmailVerificationTokenRepository;
import com.project.watchmate.Repositories.RefreshTokenRepository;
import com.project.watchmate.Repositories.UsersRepository;
import com.project.watchmate.Repositories.WatchListRepository;
import com.project.watchmate.Services.JwtService;

import tools.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;
import software.amazon.awssdk.services.ses.model.SendEmailResponse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@AutoConfigureMockMvc
@Import(AbstractIntegrationTest.ExternalClientTestConfig.class)
@SpringBootTest(properties = {
	"spring.jpa.hibernate.ddl-auto=create-drop",
	"spring.jpa.show-sql=false",
	"jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
	"tmdb.api.token=test-token",
	"app.domain=http://localhost",
	"verified.sender=test@example.com"
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
	protected BCryptPasswordEncoder passwordEncoder;

	@Autowired
	protected JwtService jwtService;

	@MockitoBean
	protected SesClient sesClient;

	@Autowired
	protected ObjectMapper objectMapper;

	@DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", SharedMySQLContainer.INSTANCE::getJdbcUrl);
        registry.add("spring.datasource.username", SharedMySQLContainer.INSTANCE::getUsername);
        registry.add("spring.datasource.password", SharedMySQLContainer.INSTANCE::getPassword);
    }

	@BeforeEach
	void cleanDatabase() {
		reset(sesClient);
		when(sesClient.sendEmail(any(SendEmailRequest.class)))
			.thenReturn(SendEmailResponse.builder().messageId("test-message-id").build());
		emailVerificationTokenRepository.deleteAll();
		refreshTokenRepository.deleteAll();
		watchListRepository.deleteAll();
		usersRepository.deleteAll();
	}

	protected Users saveUser(String username, boolean emailVerified) {
		return usersRepository.save(Users.builder()
			.username(username)
			.password(passwordEncoder.encode(TEST_PASSWORD))
			.email(username + "@example.com")
			.emailVerified(emailVerified)
			.favorites(new ArrayList<>())
			.build());
	}

	protected String createAccessToken(Users user) {
		return jwtService.generateAccessToken(user.getUsername());
	}

	protected String loginRequestBody(String username) throws Exception {
		return objectMapper.writeValueAsString(new LoginRequestDTO(username, TEST_PASSWORD));
	}

	@TestConfiguration
	public static class ExternalClientTestConfig {
		@Bean
		@Primary
		TmdbClient tmdbClient() {
			return new TmdbClient() {
				@Override
				public List<TmdbGenreDTO> fetchGenres(String type) {
					return List.of();
				}

				@Override
				public List<TmdbMovieDTO> fetchPopular(String type) {
					return List.of();
				}

				@Override
				public TmdbMovieDTO fetchMediaById(Long tmdbId, MediaType type) {
					throw new UnsupportedOperationException("TMDB media fetch is not used by auth integration tests.");
				}

				@Override
				public TmdbResponseDTO searchMulti(String query, int page) {
					throw new UnsupportedOperationException("TMDB search is not used by auth integration tests.");
				}
			};
		}
	}
}
