package com.project.watchmate.Services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.project.watchmate.Models.EmailVerificationToken;
import com.project.watchmate.Models.Users;
import com.project.watchmate.Repositories.EmailVerificationTokenRepository;
import com.project.watchmate.Repositories.UsersRepository;

import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
public class EmailVerificationTokenServiceTest {

    @Mock
    private EmailVerificationTokenRepository tokenRepository;

    @Mock
    private UsersRepository usersRepository;

    @Mock
    private SesClient sesClient;

    @InjectMocks
    private EmailVerificationTokenService emailVerificationTokenService;

    @Nested
    @DisplayName("Create Token Tests")
    class createTokenTests{
        @Test
        void createToken_WithValidData_ReturnsEmailToken(){
            Users user = Users.builder().username("test-user").build();
            LocalDateTime beforeTime = LocalDateTime.now().plusMinutes(15);
            when(tokenRepository.save(any(EmailVerificationToken.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0, EmailVerificationToken.class));

            String token = emailVerificationTokenService.createToken(user);

            LocalDateTime afterTime = LocalDateTime.now().plusMinutes(15);
            assertNotNull(token);

            ArgumentCaptor<EmailVerificationToken> captor = ArgumentCaptor.forClass(EmailVerificationToken.class);
            verify(tokenRepository).save(captor.capture());
            EmailVerificationToken saved = captor.getValue();

            assertNotNull(saved);
            assertEquals(user,saved.getUser());
            assertEquals(token, saved.getToken());
            assertNotNull(saved.getToken());
            assertTrue(saved.getExpiresAt().isBefore(afterTime));
            assertTrue(saved.getExpiresAt().isAfter(beforeTime));
        }
    }

    @Nested
    @DisplayName("Verify Token Tests")
    class verifyTokenTests{
        @Test
        void verifyToken_WithValidData_ReturnsTrueAndSetsEmailVerifiedToTrue(){
            String token = "test-token";
            LocalDateTime expiryDate = LocalDateTime.now().plusDays(4);
            Users user = Users.builder().username("test-user").build();
            when(tokenRepository.getByToken(token))
            .thenReturn(Optional.of(EmailVerificationToken.builder().token(token).expiresAt(expiryDate).user(user).build()));
            when(usersRepository.save(any(Users.class))).thenAnswer(invocation -> invocation.getArgument(0, Users.class));

            Boolean verified = emailVerificationTokenService.verifyToken(token);

            ArgumentCaptor<Users> captor = ArgumentCaptor.forClass(Users.class);
            verify(usersRepository).save(captor.capture());
            Users savedUser = captor.getValue();

            verify(tokenRepository).delete(any(EmailVerificationToken.class));
            assertNotNull(savedUser);
            assertTrue(savedUser.isEmailVerified());
            assertTrue(verified);
        }

        @Test
        void verifyToken_WhenTokenNotFound_ReturnsFalseAndDoesNotSaveOrDelete(){
            String token = "missing-token";
            when(tokenRepository.getByToken(token)).thenReturn(Optional.empty());

            boolean verified = emailVerificationTokenService.verifyToken(token);

            assertFalse(verified);
            verify(usersRepository, never()).save(any(Users.class));
            verify(tokenRepository, never()).delete(any(EmailVerificationToken.class));
        }

        @Test
        void verifyToken_WhenTokenExpired_ReturnsFalseAndDeletesTokenAndDoesNotSaveUser(){
            String token = "expired-token";
            LocalDateTime pastExpiry = LocalDateTime.now().minusMinutes(1);
            Users user = Users.builder().username("test-user").build();
            EmailVerificationToken expiredToken = EmailVerificationToken.builder()
                    .token(token)
                    .expiresAt(pastExpiry)
                    .user(user)
                    .build();
            when(tokenRepository.getByToken(token)).thenReturn(Optional.of(expiredToken));

            boolean verified = emailVerificationTokenService.verifyToken(token);

            assertFalse(verified);
            verify(tokenRepository).delete(expiredToken);
            verify(usersRepository, never()).save(any(Users.class));
        }
    }

    @Nested
    @DisplayName("Send Verification Email Tests")
    class sendVerificationEmailTests{

        @Test
        void sendVerificationEmail_WithValidArgs_CallsSesClientWithCorrectRequest(){
            ReflectionTestUtils.setField(emailVerificationTokenService, "appDomain", "https://app.example.com");
            ReflectionTestUtils.setField(emailVerificationTokenService, "verifiedSender", "noreply@watchmate.com");
            String toEmail = "user@example.com";
            String token = "abc-123-token";

            emailVerificationTokenService.sendVerificationEmail(toEmail, token);

            ArgumentCaptor<SendEmailRequest> captor = ArgumentCaptor.forClass(SendEmailRequest.class);
            verify(sesClient).sendEmail(captor.capture());
            SendEmailRequest request = captor.getValue();
            assertNotNull(request);
            assertEquals("noreply@watchmate.com", request.source());
            assertTrue(request.destination().toAddresses().contains(toEmail));
            assertEquals("Verify Your Email - WatchMate", request.message().subject().data());
            assertTrue(request.message().body().html().data().contains(token));
            assertTrue(request.message().body().html().data().contains("https://app.example.com/verify?token=" + token));
        }
    }

    @Nested
    @DisplayName("Resend Verification Email Tests")
    class resendVerificationEmailTests{

        @Test
        void resendVerificationEmail_WhenUserNotFound_ThrowsIllegalArgumentException(){
            String email = "unknown@example.com";
            when(usersRepository.findByEmail(email)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class,
                    () -> emailVerificationTokenService.resendVerificationEmail(email));

            verify(tokenRepository, never()).deleteByUserId(any());
            verify(sesClient, never()).sendEmail(any(SendEmailRequest.class));
        }

        @Test
        void resendVerificationEmail_WhenEmailAlreadyVerified_ThrowsIllegalStateException(){
            String email = "verified@example.com";
            Users user = Users.builder().id(1L).username("user").emailVerified(true).build();
            when(usersRepository.findByEmail(email)).thenReturn(Optional.of(user));

            assertThrows(IllegalStateException.class,
                    () -> emailVerificationTokenService.resendVerificationEmail(email));

            verify(tokenRepository, never()).deleteByUserId(any());
            verify(sesClient, never()).sendEmail(any(SendEmailRequest.class));
        }

        @Test
        void resendVerificationEmail_WithValidEmail_DeletesOldTokensCreatesNewTokenAndSendsEmail(){
            ReflectionTestUtils.setField(emailVerificationTokenService, "appDomain", "https://app.example.com");
            ReflectionTestUtils.setField(emailVerificationTokenService, "verifiedSender", "noreply@watchmate.com");
            String email = "user@example.com";
            Users user = Users.builder().id(1L).username("user").email(email).emailVerified(false).build();
            when(usersRepository.findByEmail(email)).thenReturn(Optional.of(user));

            emailVerificationTokenService.resendVerificationEmail(email);

            verify(tokenRepository).deleteByUserId(eq(1L));
            verify(tokenRepository).save(any(EmailVerificationToken.class));
            verify(sesClient).sendEmail(any(SendEmailRequest.class));
        }
    }
}
