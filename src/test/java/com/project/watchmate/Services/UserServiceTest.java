package com.project.watchmate.Services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.project.watchmate.Dto.LoginRequestDTO;
import com.project.watchmate.Dto.LoginResponseDTO;
import com.project.watchmate.Dto.RegisterRequestDTO;
import com.project.watchmate.Exception.EmailException;
import com.project.watchmate.Exception.InvalidRefreshTokenException;
import com.project.watchmate.Exception.UsernameException;
import com.project.watchmate.Models.RefreshToken;
import com.project.watchmate.Models.Users;
import com.project.watchmate.Repositories.UsersRepository;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class UserServiceTest {

    @Mock
    private UsersRepository usersRepository;

    @Mock
    private EmailVerificationTokenService emailService;

    @Mock
    private JwtService jwtService;

    @Mock
    private Authentication auth;

    @Mock
    private AuthenticationManager authManager;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private BCryptPasswordEncoder encoder;

    @InjectMocks
    private UserService userService;

    private RegisterRequestDTO validRegisterRequest;

    @BeforeEach
    void setUp() {
        validRegisterRequest = RegisterRequestDTO.builder()
            .username("testuser")
            .email("test@example.com")
            .password("password123")
            .build();
    }
    @Nested
    @DisplayName("Register Method Tests")
    class RegisterTests{
        @Test
        void register_WithValidData_ShouldSaveUserAndSendEmail() {
            when(usersRepository.existsByUsername("testuser")).thenReturn(false);
            when(usersRepository.existsByEmail("test@example.com")).thenReturn(false);
            when(usersRepository.save(any(Users.class))).thenReturn(Objects.requireNonNull(new Users()));
            when(emailService.createToken(any(Users.class))).thenReturn("verification-token");

            userService.register(validRegisterRequest);

            ArgumentCaptor<Users> userCaptor = ArgumentCaptor.forClass(Users.class);
            verify(usersRepository).save(userCaptor.capture());
            
            Users savedUser = Objects.requireNonNull(userCaptor.getValue(), "savedUser");
            assertEquals("testuser", savedUser.getUsername());
            assertEquals("test@example.com", savedUser.getEmail());

            verify(emailService).sendVerificationEmail("test@example.com", "verification-token");
        }

        @Test
        void register_WithValidData_ShouldSaveUserWithEncodedPassword() {
            String expectedEncodedPassword = "encoded_password_123";
            when(usersRepository.existsByUsername("testuser")).thenReturn(false);
            when(usersRepository.existsByEmail("test@example.com")).thenReturn(false);
            when(encoder.encode("password123")).thenReturn(expectedEncodedPassword);
            when(usersRepository.save(any(Users.class))).thenReturn(Objects.requireNonNull(new Users()));
            when(emailService.createToken(any(Users.class))).thenReturn("verification-token");

            userService.register(validRegisterRequest);

            ArgumentCaptor<Users> userCaptor = ArgumentCaptor.forClass(Users.class);
            verify(usersRepository).save(userCaptor.capture());
            
            Users savedUser = Objects.requireNonNull(userCaptor.getValue(), "savedUser");
            assertEquals(expectedEncodedPassword, savedUser.getPassword());
        }

        @Test
        void register_WithValidData_ShouldSaveUserWithEmailVerifiedToFalse() {
            when(usersRepository.existsByUsername("testuser")).thenReturn(false);
            when(usersRepository.existsByEmail("test@example.com")).thenReturn(false);
            when(usersRepository.save(any(Users.class))).thenReturn(Objects.requireNonNull(new Users()));
            when(emailService.createToken(any(Users.class))).thenReturn("verification-token");

            userService.register(validRegisterRequest);

            ArgumentCaptor<Users> userCaptor = ArgumentCaptor.forClass(Users.class);
            verify(usersRepository).save(userCaptor.capture());
            
            Users savedUser = Objects.requireNonNull(userCaptor.getValue(), "savedUser");
            assertEquals(false, savedUser.isEmailVerified());
        }

        @Test
        void register_WithExistingUsername_ShouldThrowUsernameException() {
            when(usersRepository.existsByUsername("testuser")).thenReturn(true);

            UsernameException exception = assertThrows(UsernameException.class, () -> userService.register(validRegisterRequest));

            assertEquals("Username is already taken.", exception.getMessage());
            verify(usersRepository).existsByUsername("testuser");
            verify(usersRepository, never()).save(any(Users.class));
        }

        @Test
        void register_WithExistingEmail_ShouldThrowEmailException() {
            when(usersRepository.existsByUsername("testuser")).thenReturn(false);
            when(usersRepository.existsByEmail("test@example.com")).thenReturn(true);
            
            EmailException exception = assertThrows(EmailException.class, () -> userService.register(validRegisterRequest));

            assertEquals("Email is already in use.", exception.getMessage());
            verify(usersRepository).existsByUsername("testuser");
            verify(usersRepository).existsByEmail("test@example.com");
            verify(usersRepository, never()).save(any(Users.class));
        }

        @Test
        void register_WhenDatabaseFails_ShouldThrowRuntimeException() {
            when(usersRepository.existsByUsername("testuser")).thenReturn(false);
            when(usersRepository.existsByEmail("test@example.com")).thenReturn(false);
            when(usersRepository.save(any(Users.class)))
                .thenThrow(new DataIntegrityViolationException("Database error"));

            RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userService.register(validRegisterRequest)
            );
            
            assertEquals("Account could not be created. Please try again.", exception.getMessage());
            verify(usersRepository).existsByUsername("testuser");
            verify(usersRepository).existsByEmail("test@example.com");
            verify(usersRepository).save(any(Users.class));
        }
    }
    
    @Nested
    @DisplayName("Verify Method Tests")
    class VerifyTests{
        @Test
        void verify_WithValidCredentials_ShouldReturnAccessAndRefreshToken(){
            LoginRequestDTO loginRequest = new LoginRequestDTO("testuser", "password123");
            Users user = Users.builder().username("testuser").build();

            LocalDateTime expectedExpiry = LocalDateTime.of(2030, 1, 1, 0, 0);
            RefreshToken createdRefreshToken = RefreshToken.builder()
                .token("refresh-token-1")
                .user(user)
                .expiryDate(LocalDateTime.of(2030, 1, 8, 0, 0))
                .createdAt(LocalDateTime.of(2030, 1, 1, 0, 0))
                .revoked(false)
                .build();

            when(authManager.authenticate(any())).thenReturn(auth);
            when(auth.isAuthenticated()).thenReturn(true);
            when(usersRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
            when(jwtService.generateAccessToken("testuser")).thenReturn("access-token-1");
            when(refreshTokenService.createRefreshToken(user)).thenReturn(createdRefreshToken);
            when(jwtService.getAccessTokenExpiry()).thenReturn(expectedExpiry);

            LoginResponseDTO response = userService.verify(loginRequest);

            assertEquals("access-token-1", response.getAccessToken());
            assertEquals("refresh-token-1", response.getRefreshToken());
            assertEquals(expectedExpiry, response.getAccessTokenExpiry());
            assertEquals("Bearer", response.getTokenType());

            verify(authManager).authenticate(any());
            verify(usersRepository).findByUsername("testuser");
            verify(jwtService).generateAccessToken("testuser");
            verify(refreshTokenService).createRefreshToken(user);
            verify(jwtService).getAccessTokenExpiry();
        }

        @Test
        void verify_WithInvalidCredentials_ShouldThrowRuntimeException(){
            LoginRequestDTO loginRequest = new LoginRequestDTO("testuser", "wrong_password");

            when(authManager.authenticate(any())).thenReturn(auth);
            when(auth.isAuthenticated()).thenReturn(false);

            RuntimeException exception = assertThrows(RuntimeException.class, () -> userService.verify(loginRequest));
            assertEquals("Authentication Failed", exception.getMessage());

            verify(authManager).authenticate(any());
            verify(usersRepository, never()).findByUsername(anyString());
            verify(jwtService, never()).generateAccessToken(anyString());
            verify(refreshTokenService, never()).createRefreshToken(any(Users.class));
        }
    }

    @Nested
    @DisplayName("Refresh Token Method Tests")
    class RefreshTokenTests{
        @Test
        void refreshToken_WithValidToken_ShouldReturnNewAccessAndRefreshToken(){
            Users user = Users.builder().username("testuser").build();

            RefreshToken existingRefreshToken = RefreshToken.builder()
                .token("refresh-token-old")
                .user(user)
                .expiryDate(LocalDateTime.of(2030, 1, 8, 0, 0))
                .createdAt(LocalDateTime.of(2030, 1, 1, 0, 0))
                .revoked(false)
                .build();

            RefreshToken newRefreshToken = RefreshToken.builder()
                .token("refresh-token-new")
                .user(user)
                .expiryDate(LocalDateTime.of(2030, 1, 9, 0, 0))
                .createdAt(LocalDateTime.of(2030, 1, 2, 0, 0))
                .revoked(false)
                .build();

            LocalDateTime expectedExpiry = LocalDateTime.of(2030, 1, 1, 0, 0);

            when(refreshTokenService.verifyRefreshToken("refresh-token-old")).thenReturn(existingRefreshToken);
            when(jwtService.generateAccessToken("testuser")).thenReturn("access-token-new");
            when(refreshTokenService.createRefreshToken(user)).thenReturn(newRefreshToken);
            when(jwtService.getAccessTokenExpiry()).thenReturn(expectedExpiry);

            LoginResponseDTO response = userService.refreshToken("refresh-token-old");

            assertEquals("access-token-new", response.getAccessToken());
            assertEquals("refresh-token-new", response.getRefreshToken());
            assertEquals(expectedExpiry, response.getAccessTokenExpiry());
            assertEquals("Bearer", response.getTokenType());

            verify(refreshTokenService).verifyRefreshToken("refresh-token-old");
            verify(jwtService).generateAccessToken("testuser");
            verify(refreshTokenService).createRefreshToken(user);
            verify(jwtService).getAccessTokenExpiry();
        }

        @Test
        void refreshToken_WithInvalidToken_ShouldThrowInvalidRefreshTokenException(){
            when(refreshTokenService.verifyRefreshToken("bad-token"))
                .thenThrow(new InvalidRefreshTokenException("Invalid refresh token"));

            InvalidRefreshTokenException exception = assertThrows(
                InvalidRefreshTokenException.class,
                () -> userService.refreshToken("bad-token")
            );

            assertEquals("Invalid refresh token", exception.getMessage());

            verify(refreshTokenService).verifyRefreshToken("bad-token");
            verify(jwtService, never()).generateAccessToken(anyString());
            verify(refreshTokenService, never()).createRefreshToken(any(Users.class));
        }
    }
}
