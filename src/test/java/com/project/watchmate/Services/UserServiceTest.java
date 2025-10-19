package com.project.watchmate.Services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.project.watchmate.Dto.RegisterRequestDTO;
import com.project.watchmate.Exception.EmailException;
import com.project.watchmate.Exception.UsernameException;
import com.project.watchmate.Models.Users;
import com.project.watchmate.Repositories.UsersRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UsersRepository usersRepository;

    @Mock
    private EmailVerificationTokenService emailService;

    @Mock
    private JwtService jwtService;

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
            when(usersRepository.save(any(Users.class))).thenReturn(new Users());
            when(emailService.createToken(any(Users.class))).thenReturn("verification-token");

            userService.register(validRegisterRequest);

            ArgumentCaptor<Users> userCaptor = ArgumentCaptor.forClass(Users.class);
            verify(usersRepository).save(userCaptor.capture());
            
            Users savedUser = userCaptor.getValue();
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
            when(usersRepository.save(any(Users.class))).thenReturn(new Users());
            when(emailService.createToken(any(Users.class))).thenReturn("verification-token");

            userService.register(validRegisterRequest);

            ArgumentCaptor<Users> userCaptor = ArgumentCaptor.forClass(Users.class);
            verify(usersRepository).save(userCaptor.capture());
            
            Users savedUser = userCaptor.getValue();
            assertEquals(expectedEncodedPassword, savedUser.getPassword());
        }

        @Test
        void register_WithValidData_ShouldSaveUserWithEmailVerifiedToFalse() {
            when(usersRepository.existsByUsername("testuser")).thenReturn(false);
            when(usersRepository.existsByEmail("test@example.com")).thenReturn(false);
            when(usersRepository.save(any(Users.class))).thenReturn(new Users());
            when(emailService.createToken(any(Users.class))).thenReturn("verification-token");

            userService.register(validRegisterRequest);

            ArgumentCaptor<Users> userCaptor = ArgumentCaptor.forClass(Users.class);
            verify(usersRepository).save(userCaptor.capture());
            
            Users savedUser = userCaptor.getValue();
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
    
}
