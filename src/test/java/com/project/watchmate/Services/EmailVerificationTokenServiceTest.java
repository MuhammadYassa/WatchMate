package com.project.watchmate.Services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.project.watchmate.Models.EmailVerificationToken;
import com.project.watchmate.Models.Users;
import com.project.watchmate.Repositories.EmailVerificationTokenRepository;
import com.project.watchmate.Repositories.UsersRepository;

import software.amazon.awssdk.services.ses.SesClient;

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
        void createToken_withValidData_ReturnsEmailToken(){
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
            assertNotNull(saved.getToken());
            assertTrue(saved.getExpiresAt().isBefore(afterTime));
            assertTrue(saved.getExpiresAt().isAfter(beforeTime));
        }
    }
}
